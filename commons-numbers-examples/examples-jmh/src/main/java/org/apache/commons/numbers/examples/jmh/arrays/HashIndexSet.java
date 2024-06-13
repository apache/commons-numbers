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
 * An index set backed by a open-addressed hash table using linear hashing. Table size is a power
 * of 2 and has a maximum capacity of 2^29 with a fixed load factor of 0.5. If the functional
 * capacity is exceeded then the set raises an {@link IllegalStateException}.
 *
 * <p>Values are stored using bit inversion. Any positive index will have a negative
 * representation when stored. An empty slot is indicated by a zero.
 *
 * <p>This class has a minimal API. It can be used to ensure a collection of indices of
 * a known size are unique:
 *
 * <pre>{@code
 * int[] keys = ...
 * HashIndexSet set = new HashIndexSet(keys.length);
 * for (int k : keys) {
 *   if (set.add(k)) {
 *     // first occurrence of k in keys
 *   }
 * }
 * }</pre>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Open_addressing">Open addressing (Wikipedia)</a>
 * @since 1.2
 */
final class HashIndexSet {
    /** Message for an invalid index. */
    private static final String INVALID_INDEX = "Invalid index: ";
    /** The maximum capacity of the set. */
    private static final int MAX_CAPACITY = 1 << 29;
    /** The minimum size of the backing array. */
    private static final int MIN_SIZE = 16;
    /**
     * Unsigned 32-bit integer numerator of the golden ratio (0.618) with an assumed
     * denominator of 2^32.
     *
     * <pre>
     * 2654435769 = round(2^32 * (sqrt(5) - 1) / 2)
     * Long.toHexString((long)(0x1p32 * (Math.sqrt(5.0) - 1) / 2))
     * </pre>
     */
    private static final int PHI = 0x9e3779b9;

    /** The set. */
    private final int[] set;
    /** The size. */
    private int size;

    /**
     * Create an instance with size to store up to the specified {@code capacity}.
     *
     * <p>The functional capacity (number of indices that can be stored) is the next power
     * of 2 above {@code capacity}; or a minimum size if the requested {@code capacity} is
     * small.
     *
     * @param capacity Capacity (assumed to be positive).
     */
    HashIndexSet(int capacity) {
        if (capacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("Unsupported capacity: " + capacity);
        }
        // This will generate a load factor at capacity in the range (0.25, 0.5]
        // The use of Math.max will ignore zero/negative capacity requests.
        set = new int[nextPow2(Math.max(MIN_SIZE, capacity * 2))];
    }

    /**
     * Return the memory footprint in bytes. This is always a power of 2.
     *
     * <p>This will return the size as if not limited to a capacity of 2<sup>29</sup>.
     * In this case the size will exceed the maximum size of an {@code int[]} array.
     *
     * <p>This method is intended to provide information to choose if the data structure
     * is memory efficient.
     *
     * @param capacity Capacity.
     * @return the memory footprint
     */
    static long memoryFootprint(int capacity) {
        if (capacity <= (MIN_SIZE >> 1)) {
            // 4 bytes/int
            return MIN_SIZE << 2;
        }
        // Double the next power of 2, then convert integer count to bytes (4 bytes/int)
        // * 2 * 4 == * 2^3
        return Integer.toUnsignedLong(nextPow2(capacity)) << 3;
    }

    /**
     * Returns the closest power-of-two number greater than or equal to {@code value}.
     *
     * <p>Warning: This will return {@link Integer#MIN_VALUE} for any {@code value} above
     * {@code 1 << 30}. This is the next power of 2 as an unsigned integer.
     *
     * <p>See <a
     * href="https://graphics.stanford.edu/~seander/bithacks.html#RoundUpPowerOf2">Bit
     * Hacks: Rounding up to a power of 2</a>
     *
     * @param value Value.
     * @return the closest power-of-two number greater than or equal to value
     */
    private static int nextPow2(int value) {
        int result = value - 1;
        result |= result >>> 1;
        result |= result >>> 2;
        result |= result >>> 4;
        result |= result >>> 8;
        return (result | (result >>> 16)) + 1;
    }

    /**
     * Adds the {@code index} to the set.
     *
     * @param index Index.
     * @return true if the set was modified by the operation
     * @throws IndexOutOfBoundsException if the index is negative
     */
    boolean add(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException(INVALID_INDEX + index);
        }
        final int[] keys = set;
        final int key = ~index;
        final int mask = keys.length - 1;
        int pos = mix(index) & mask;
        int curr = keys[pos];
        if (curr < 0) {
            if (curr == key) {
                // Already present
                return false;
            }
            // Probe
            while ((curr = keys[pos = (pos + 1) & mask]) < 0) {
                if (curr == key) {
                    // Already present
                    return false;
                }
            }
        }
        // Insert
        keys[pos] = key;
        // Here the load factor is 0.5: Test if size > keys.length * 0.5
        if (++size > (mask + 1) >>> 1) {
            // This is where we should grow the size of the set and re-insert
            // all current keys into the new key storage. Here we are using a
            // fixed capacity so raise an exception.
            throw new IllegalStateException("Functional capacity exceeded: " + (keys.length >>> 1));
        }
        return true;
    }

    /**
     * Test if the {@code index} is in the set.
     *
     * <p>This method is present for testing. It is not required when filtering a collection
     * of indices with duplicates to a unique set of indices.
     *
     * @param index Index.
     * @return true if the set contains the index
     * @throws IndexOutOfBoundsException if the index is negative
     */
    boolean contains(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException(INVALID_INDEX + index);
        }
        final int[] keys = set;
        final int mask = keys.length - 1;
        int pos = mix(index) & mask;
        int curr = keys[pos];
        if (curr == 0) {
            return false;
        }
        final int key = ~index;
        if (curr == key) {
            return true;
        }
        // Probe
        while (true) {
            pos = (pos + 1) & mask;
            curr = keys[pos];
            if (curr == 0) {
                // No more entries
                return false;
            }
            if (curr == key) {
                return true;
            }
        }
    }

    /**
     * Mix the bits of an integer.
     *
     * <p>This is the fast hash function used in the linear hash implementation in the <a
     * href="https://github.com/leventov/Koloboke">Koloboke Collections</a>.
     *
     * @param x Bits.
     * @return the mixed bits
     */
    private static int mix(int x) {
        final int h = x * PHI;
        return h ^ (h >>> 16);
    }

    /**
     * Returns the number of distinct indices in the set.
     *
     * @return the size
     */
    int size() {
        return size;
    }

    /**
     * Write each index in the set into the provided array. Returns the number of indices.
     *
     * <p>The caller must ensure the output array has sufficient capacity.
     *
     * <p>Warning: The indices are not ordered.
     *
     * <p>This method is present for testing. It is not required when filtering a collection
     * of indices with duplicates to a unique set of indices. It can be used to write
     * out the unique indices, but they are not in the encounter order of indices
     * {@link #add(int) added} to the set.
     *
     * @param a Output array.
     * @return count of indices
     * @see #size()
     */
    int toArray(int[] a) {
        final int[] keys = set;
        int c = 0;
        for (final int key : keys) {
            if (key < 0) {
                a[c++] = ~key;
            }
        }
        // assert c == size
        return c;
    }
}
