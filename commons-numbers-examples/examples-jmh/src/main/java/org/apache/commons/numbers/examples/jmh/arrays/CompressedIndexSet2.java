/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.numbers.examples.jmh.arrays;

/**
 * A fixed size set of indices within an inclusive range {@code [left, right]}.
 *
 * <p>This is a specialised class to implement a data structure similar to a
 * {@link java.util.BitSet}. It supports a fixed size and contains the methods required to
 * store and look-up intervals of indices.
 *
 * <p>An offset is supported to allow the fixed size to cover a range of indices starting
 * above 0 with the most efficient usage of storage.
 *
 * <p>In contrast to a {@link java.util.BitSet}, the data structure does not store all
 * indices in the range. Indices are compressed 2-to-1. The structure can return
 * with 100% accuracy if a query index is not within the range. It cannot return with 100%
 * accuracy if a query index is contained within the range. The presence of a query index
 * is a probabilistic statement that there is an index within 1 of the query index.
 *
 * <p>Indices are stored offset from {@code left} and compressed. A compressed index
 * represents 2 real indices:
 *
 * <pre>
 * Interval:   012345678
 * Compressed: 0-1-2-3-4
 * </pre>
 *
 * <p>When scanning for the next index the identified compressed index is decompressed and
 * the lower bound of the range represented by the index is returned.
 *
 * <p>When scanning for the previous index the identified compressed index is decompressed
 * and the upper bound of the range represented by the index is returned.
 *
 * <p>When scanning in either direction, if the search index is inside a compressed index
 * the search index is returned.
 *
 * <p>Note: Search for the {@link SearchableInterval} interface outside the supported bounds
 * {@code [left, right]} is not supported and will result in an {@link IndexOutOfBoundsException}.
 *
 * <p>See the BloomFilter code in Commons Collections for use of long[] data to store
 * bits.
 *
 * <p>Note: This is a specialised version of {@link CompressedIndexSet} using a fixed
 * compression of 1. This is used for performance testing. This is the most useful
 * compression level for the partition algorithm as any compressed key that lies
 * exactly on a partition index will only require a search for the min/max in the
 * interval immediately below/above the partition index. A pair of indices (k, k+1)
 * that is split into two compressed keys and lies exactly on a partition index will
 * require a search for the min on one side and two maximum values on the other side; or
 * max on one side and two minimum on the other. Each of these cases can be handled by
 * dedicated select routines to find 1 or 2 values at the edge of a range.
 *
 * @since 1.2
 */
final class CompressedIndexSet2 implements SearchableInterval {
    /** All 64-bits bits set. */
    private static final long LONG_MASK = -1L;
    /** A bit shift to apply to an integer to divided by 64 (2^6). */
    private static final int DIVIDE_BY_64 = 6;

    /** Bit indexes. */
    private final long[] data;

    /** Left bound of the support. */
    private final int left;
    /** Right bound of the support. */
    private final int right;

    /**
     * Create an instance to store indices within the range {@code [left, right]}.
     *
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     */
    private CompressedIndexSet2(int l, int r) {
        this.left = l;
        // Note: The functional upper bound may be higher but the next/previous functionality
        // support scanning in the original [left, right] bound.
        this.right = r;
        // Note: This may allow directly writing to index > right if there
        // is extra capacity.
        data = new long[getLongIndex((r - l) >>> 1) + 1];
    }

    /**
     * Create an instance to store indices within the range {@code [left, right]}.
     * The instance is initially empty.
     *
     * <p>Warning: To use this object as an {@link SearchableInterval} the left and right
     * indices should be added to the set.
     *
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @return the index set
     * @throws IllegalArgumentException if {@code compression} is not in {@code [1, 31]};
     * or if {@code right < left}; or if {@code left < 0}
     */
    static CompressedIndexSet2 ofRange(int left, int right) {
        checkLeft(left);
        checkRange(left, right);
        return new CompressedIndexSet2(left, right);
    }

    /**
     * Initialise an instance with the {@code indices}. The capacity is defined by the
     * range required to store the minimum and maximum index at the specified
     * {@code compression} level.
     *
     * <p>This object can be used as an {@link SearchableInterval} as the left and right
     * indices will be set.
     *
     * @param indices Indices.
     * @return the index set
     * @throws IllegalArgumentException if {@code compression} is not in {@code [1, 31]};
     * or if {@code indices.length == 0}; or if {@code left < 0}
     */
    static CompressedIndexSet2 of(int[] indices) {
        return of(indices, indices.length);
    }

    /**
     * Initialise an instance with the {@code indices}. The capacity is defined by the
     * range required to store the minimum and maximum index at the specified
     * {@code compression} level.
     *
     * @param indices Indices.
     * @param n Number of indices.
     * @return the index set
     * @throws IllegalArgumentException if {@code compression} is not in {@code [1, 31]};
     * or if {@code n == 0}; or if {@code left < 0}
     */
    static CompressedIndexSet2 of(int[] indices, int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("No indices to define the range");
        }
        int min = indices[0];
        int max = min;
        for (int i = 0; ++i < n;) {
            min = Math.min(min, indices[i]);
            max = Math.max(max, indices[i]);
        }
        checkLeft(min);
        final CompressedIndexSet2 set = new CompressedIndexSet2(min, max);
        for (int i = -1; ++i < n;) {
            set.set(indices[i]);
        }
        return set;
    }

    /**
     * Gets the compressed index for this instance using the left bound and the
     * compression level.
     *
     * @param index Index.
     * @return the compressed index
     */
    private int compressIndex(int index) {
        return (index - left) >>> 1;
    }

    /**
     * Gets the filter index for the specified bit index assuming the filter is using
     * 64-bit longs to store bits starting at index 0.
     *
     * <p>The index is assumed to be positive. For a positive index the result will match
     * {@code bitIndex / 64}.</p>
     *
     * <p><em>The divide is performed using bit shifts. If the input is negative the
     * behavior is not defined.</em></p>
     *
     * @param bitIndex the bit index (assumed to be positive)
     * @return the index of the bit map in an array of bit maps.
     */
    private static int getLongIndex(final int bitIndex) {
        // An integer divide by 64 is equivalent to a shift of 6 bits if the integer is
        // positive.
        // We do not explicitly check for a negative here. Instead we use a
        // signed shift. Any negative index will produce a negative value
        // by sign-extension and if used as an index into an array it will throw an
        // exception.
        return bitIndex >> DIVIDE_BY_64;
    }

    /**
     * Gets the filter bit mask for the specified bit index assuming the filter is using
     * 64-bit longs to store bits starting at index 0. The returned value is a
     * {@code long} with only 1 bit set.
     *
     * <p>The index is assumed to be positive. For a positive index the result will match
     * {@code 1L << (bitIndex % 64)}.</p>
     *
     * <p><em>If the input is negative the behavior is not defined.</em></p>
     *
     * @param bitIndex the bit index (assumed to be positive)
     * @return the filter bit
     */
    private static long getLongBit(final int bitIndex) {
        // Bit shifts only use the first 6 bits. Thus it is not necessary to mask this
        // using 0x3f (63) or compute bitIndex % 64.
        // Note: If the index is negative the shift will be (64 - (bitIndex & 0x3f)) and
        // this will identify an incorrect bit.
        return 1L << bitIndex;
    }

    /**
     * Returns the value of the bit with the specified index.
     *
     * <p>Warning: This has no range checks.
     *
     * @param bitIndex the bit index (assumed to be positive)
     * @return the value of the bit with the specified index
     */
    boolean get(int bitIndex) {
        // WARNING: No range checks !!!
        final int index = compressIndex(bitIndex);
        final int i = getLongIndex(index);
        final long m = getLongBit(index);
        return (data[i] & m) != 0;
    }

    /**
     * Sets the bit at the specified index to {@code true}.
     *
     * <p>Warning: This has no range checks.
     *
     * @param bitIndex the bit index (assumed to be positive)
     */
    void set(int bitIndex) {
        // WARNING: No range checks !!!
        final int index = compressIndex(bitIndex);
        final int i = getLongIndex(index);
        final long m = getLongBit(index);
        data[i] |= m;
    }


    @Override
    public int left() {
        return left;
    }

    @Override
    public int right() {
        return right;
    }

    @Override
    public int previousIndex(int k) {
        // WARNING: No range checks !!!
        // Assume left <= k <= right and that left and right are set bits acting as sentinels.
        final int index = compressIndex(k);

        int i = getLongIndex(index);
        long bits = data[i];

        // Check if this is within a compressed index. If so return the exact result.
        if ((bits & getLongBit(index)) != 0) {
            return k;
        }

        // Mask bits before the bit index
        // mask = 00011111 = -1L >>> (64 - ((index + 1) % 64))
        bits &= LONG_MASK >>> -(index + 1);
        for (;;) {
            if (bits != 0) {
                //(i+1)       i
                // |   c      |
                // |   |      |
                // 0  001010000
                final int c = (i + 1) * Long.SIZE - Long.numberOfLeadingZeros(bits);
                // Decompress the prior unset bit to an index. When inflated this is the
                // next index above the upper bound of the compressed range so subtract 1.
                return (c << 1) - 1 + left;
            }
            // Unsupported: the interval should contain k
            //if (i == 0) {
            //    return left - 1;
            //}
            bits = data[--i];
        }
    }

    @Override
    public int nextIndex(int k) {
        // WARNING: No range checks !!!
        // Assume left <= k <= right and that left and right are set bits acting as sentinels.
        final int index = compressIndex(k);

        int i = getLongIndex(index);
        long bits = data[i];

        // Check if this is within a compressed index. If so return the exact result.
        if ((bits & getLongBit(index)) != 0) {
            return k;
        }

        // Mask bits after the bit index
        // mask = 11111000 = -1L << (index % 64)
        bits &= LONG_MASK << index;
        for (;;) {
            if (bits != 0) {
                //(i+1)       i
                // |      c   |
                // |      |   |
                // 0  001010000
                final int c = i * Long.SIZE + Long.numberOfTrailingZeros(bits);
                // Decompress the set bit to an index. When inflated this is the lower bound of
                // the compressed range and is OK for next scanning.
                return (c << 1) + left;
            }
            // Unsupported: the interval should contain k
            //if (++i == data.length) {
            //    return right + 1;
            //}
            bits = data[++i];
        }
    }

    /**
     * Check the lower bound to the range is valid.
     *
     * @param left Lower bound (inclusive).
     * @throws IllegalArgumentException if {@code left < 0}
     */
    private static void checkLeft(int left) {
        if (left < 0) {
            throw new IllegalArgumentException("Invalid lower index: " + left);
        }
    }

    /**
     * Check the range is valid.
     *
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @throws IllegalArgumentException if {@code right < left}
     */
    private static void checkRange(int left, int right) {
        if (right < left) {
            throw new IllegalArgumentException(
                String.format("Invalid range: [%d, %d]", left, right));
        }
    }
}
