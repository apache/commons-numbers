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
 * An {@link UpdatingInterval} and {@link SplittingInterval} backed by a fixed size of bits.
 *
 * <p>This is a specialised class to implement a reduced API similar to a
 * {@link java.util.BitSet}. It uses no bounds range checks and supports only
 * the methods required to implement the {@link UpdatingInterval} API.
 *
 * <p>An offset is supported to allow the fixed size to cover a range of indices starting
 * above 0 with the most efficient usage of storage.
 *
 * <p>See the BloomFilter code in Commons Collections for use of long[] data to store
 * bits.
 *
 * @since 1.2
 */
final class BitIndexUpdatingInterval implements UpdatingInterval, SplittingInterval, IntervalAnalysis {
    /** All 64-bits bits set. */
    private static final long LONG_MASK = -1L;
    /** A bit shift to apply to an integer to divided by 64 (2^6). */
    private static final int DIVIDE_BY_64 = 6;

    /** Bit indexes. */
    private final long[] data;

    /** Index offset. */
    private final int offset;
    /** Left bound of the support. */
    private int left;
    /** Right bound of the support. */
    private int right;

    /**
     * Create an instance to store indices within the range {@code [left, right]}.
     * The range is not validated.
     *
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     */
    BitIndexUpdatingInterval(int left, int right) {
        this.offset = left;
        this.left = left;
        this.right = right;
        // Allocate storage to store index==right
        // Note: This may allow directly writing to index > right if there
        // is extra capacity.
        data = new long[getLongIndex(right - offset) + 1];
    }

    /**
     * Create an instance with the range {@code [left, right]} and reusing the provided
     * index {@code data}.
     *
     * @param data Data.
     * @param offset Index offset.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     */
    private BitIndexUpdatingInterval(long[] data, int offset, int left, int right) {
        this.data = data;
        this.offset = offset;
        this.left = left;
        this.right = right;
    }

    /**
     * Create an instance to store indices within the range {@code [left, right]}.
     *
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @return the index set
     * @throws IllegalArgumentException if {@code right < left}; {@code left < 0} or
     * {@code right == Integer.MAX_VALUE}
     */
    static BitIndexUpdatingInterval ofRange(int left, int right) {
        if (left < 0) {
            throw new IllegalArgumentException("Invalid lower index: " + left);
        }
        if (right == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid upper index: " + right);
        }
        if (right < left) {
            throw new IllegalArgumentException(
                String.format("Invalid range: [%d, %d]", left, right));
        }
        return new BitIndexUpdatingInterval(left, right);
    }

    /**
     * Initialise an instance with the {@code indices}. The capacity is defined by the
     * range required to store the minimum and maximum index.
     *
     * @param indices Indices.
     * @param n Number of indices.
     * @return the index set
     * @throws IllegalArgumentException if {@code n == 0}
     */
    static BitIndexUpdatingInterval of(int[] indices, int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("No indices to define the range");
        }
        int min = indices[0];
        int max = min;
        for (int i = 1; i < n; i++) {
            min = Math.min(min, indices[i]);
            max = Math.max(max, indices[i]);
        }
        final BitIndexUpdatingInterval set = BitIndexUpdatingInterval.ofRange(min, max);
        for (int i = -1; ++i < n;) {
            set.set(indices[i]);
        }
        return set;
    }

    /**
     * Return the memory footprint in bytes. This is always a multiple of 64.
     *
     * <p>The result is {@code 64 * ceil((right - offset + 1) / 64)}.
     *
     * <p>This method is intended to provided information to choose if the data structure
     * is memory efficient.
     *
     * <p>Warning: It is assumed {@code 0 <= left <= right}. Use with the min/max index
     * that is to be stored.
     *
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @return the memory footprint
     */
    static long memoryFootprint(int left, int right) {
        return (getLongIndex(right - left) + 1L) * Long.BYTES;
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
     * Sets the bit at the specified index to {@code true}.
     *
     * <p>Warning: This has no range checks.
     *
     * @param bitIndex the bit index (assumed to be positive)
     */
    void set(int bitIndex) {
        // WARNING: No range checks !!!
        final int index = bitIndex - offset;
        final int i = getLongIndex(index);
        final long m = getLongBit(index);
        data[i] |= m;
    }

    /**
     * Returns the index of the first bit that is set to {@code true} that occurs on or
     * after the specified starting index.
     *
     * <p>Warning: This has no range checks. It is assumed that {@code left <= k <= right},
     * that is there is a set bit on or after {@code k}.
     *
     * @param k Index to start checking from (inclusive).
     * @return the index of the next set bit
     */
    private int nextIndex(int k) {
        // left <= k <= right

        final int index = k - offset;
        int i = getLongIndex(index);

        // Mask bits after the bit index
        // mask = 11111000 = -1L << (index % 64)
        long bits = data[i] & (LONG_MASK << index);
        for (;;) {
            if (bits != 0) {
                //(i+1)       i
                // |    index |
                // |      |   |
                // 0  001010000
                return i * Long.SIZE + Long.numberOfTrailingZeros(bits) + offset;
            }
            // Unsupported: the interval should contain k
            //if (++i == data.length)
            //    return right + 1
            bits = data[++i];
        }
    }

    /**
     * Returns the index of the first bit that is set to {@code true} that occurs on or
     * before the specified starting index.
     *
     * <p>Warning: This has no range checks. It is assumed that {@code left <= k <= right},
     * that is there is a set bit on or before {@code k}.
     *
     * @param k Index to start checking from (inclusive).
     * @return the index of the previous set bit
     */
    private int previousIndex(int k) {
        // left <= k <= right

        final int index = k - offset;
        int i = getLongIndex(index);

        // Mask bits before the bit index
        // mask = 00011111 = -1L >>> (64 - ((index + 1) % 64))
        long bits = data[i] & (LONG_MASK >>> -(index + 1));
        for (;;) {
            if (bits != 0) {
                //(i+1)       i
                // |  index   |
                // |    |     |
                // 0  001010000
                return (i + 1) * Long.SIZE - Long.numberOfLeadingZeros(bits) - 1 + offset;
            }
            // Unsupported: the interval should contain k
            //if (i == 0)
            //    return left - 1
            bits = data[--i];
        }
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
    public int updateLeft(int k) {
        // Assume left < k= < right
        return left = nextIndex(k);
    }

    @Override
    public int updateRight(int k) {
        // Assume left <= k < right
        return right = previousIndex(k);
    }

    @Override
    public UpdatingInterval splitLeft(int ka, int kb) {
        // Assume left < ka <= kb < right
        final int lower = left;
        left = nextIndex(kb + 1);
        return new BitIndexUpdatingInterval(data, offset, lower, previousIndex(ka - 1));
    }

    @Override
    public UpdatingInterval splitRight(int ka, int kb) {
        // Assume left < ka <= kb < right
        final int upper = right;
        right = previousIndex(ka - 1);
        return new BitIndexUpdatingInterval(data, offset, nextIndex(kb + 1), upper);
    }

    @Override
    public boolean empty() {
        // Empty when the interval is invalid. Signalled by a negative right bound.
        return right < 0;
    }

    @Override
    public SplittingInterval split(int ka, int kb) {
        if (ka <= left) {
            // No left interval
            if (kb >= right) {
                // No right interval
                invalidate();
            } else if (kb >= left) {
                // Update the left bound
                left = nextIndex(kb + 1);
            }
            return null;
        }
        if (kb >= right) {
            // No right interval.
            // Find new right bound for the left-side.
            final int r = ka <= right ? previousIndex(ka - 1) : right;
            invalidate();
            return new BitIndexUpdatingInterval(data, offset, left, r);
        }
        // Split
        return (SplittingInterval) splitLeft(ka, kb);
    }

    /**
     * Invalidate the interval and mark as empty.
     */
    private void invalidate() {
        right = -1;
    }

    @Override
    public boolean saturated(int separation) {
        // Support saturation analysis at separation relevant to the
        // quickselect implementations
        if (separation == 3) {
            return saturated3();
        }
        if (separation == 4) {
            return saturated4();
        }
        return false;
    }

    /**
     * Test if saturated as a separation of {@code 2^3}.
     *
     * @return true if saturated
     */
    private boolean saturated3() {
        int c = 0;
        for (long x : data) {
            // Shift powers of 2 and mask out the bits that were shifted
            x = x | (x >>> 1);
            x = x | (x >>> 2);
            x = (x | (x >>> 4)) & 0b0000000100000001000000010000000100000001000000010000000100000001L;
            // Expect a population count intrinsic method
            // Add [0, 8]
            c += Long.bitCount(x);
        }
        // Multiply by 8
        return c << 3 >= right - left;
    }

    /**
     * Test if saturated as a separation of {@code 2^4}.
     *
     * @return true if saturated
     */
    private boolean saturated4() {
        int c = 0;
        for (long x : data) {
            // Shift powers of 2 and mask out the bits that were shifted
            x = x | (x >>> 1);
            x = x | (x >>> 2);
            x = x | (x >>> 4);
            x = (x | (x >>> 8)) & 0b0000000000000001000000000000000100000000000000010000000000000001L;
            // Count the bits using folding
            // x = mask:
            // 0000000000000001000000000000001000000000000000100000000000000010  (x += (x >>> 16))
            // 0000000100000001000000100000001000000011000000110000010000000100  (x += (x >>> 32))
            x = x + (x >>> 16); // put count of each 32 bits into their lowest 2 bits
            x = x + (x >>> 32); // put count of each 64 bits into their lowest 3 bits
            // Add [0, 4]
            c += (int) x & 0b111;
        }
        // Multiply by 16
        return c << 4 >= right - left;
    }
}
