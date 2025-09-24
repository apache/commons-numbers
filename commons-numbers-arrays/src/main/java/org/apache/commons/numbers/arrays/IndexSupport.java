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
package org.apache.commons.numbers.arrays;

/**
 * Support for creating {@link UpdatingInterval} implementations and validating indices.
 *
 * @since 1.2
 */
final class IndexSupport {
    /** The upper threshold to use a modified insertion sort to find unique indices. */
    private static final int INSERTION_SORT_SIZE = 20;

    /** No instances. */
    private IndexSupport() {}

    /**
     * Returns an interval that covers the specified indices {@code k}.
     *
     * @param k Indices.
     * @param n Count of indices (must be strictly positive).
     * @return the interval
     */
    static UpdatingInterval createUpdatingInterval(int[] k, int n) {
        // Note: A typical use case is to have a few indices. Thus the heuristics
        // in this method should be very fast when n is small.
        // We have a choice between a KeyUpdatingInterval which requires
        // sorted keys or a BitIndexUpdatingInterval which handles keys in any order.
        // The purpose of the heuristics is to avoid a very bad choice of data structure,
        // rather than choosing the best data structure in all situations. As long as the
        // choice is reasonable the speed will not impact a partition algorithm.

        // Simple cases
        if (n == 2) {
            if (k[0] == k[1]) {
                return newUpdatingInterval(k, 1);
            }
            if (k[1] < k[0]) {
                final int v = k[0];
                k[0] = k[1];
                k[1] = v;
            }
            return newUpdatingInterval(k, 2);
        }

        // Strategy: Must be fast on already ascending data.
        // Note: The recommended way to generate a lot of partition indices is to
        // generate in sequence.

        // n <= small:
        //   Modified insertion sort (naturally finds ascending data)
        // n > small:
        //   Look for ascending sequence and compact
        // else:
        //   Remove duplicates using an order(1) data structure and sort

        if (n <= INSERTION_SORT_SIZE) {
            final int unique = Sorting.insertionSortIndices(k, n);
            return newUpdatingInterval(k, unique);
        }

        if (isAscending(k, n)) {
            // For sorted keys the KeyUpdatingInterval is fast. It may be slower than the
            // BitIndexUpdatingInterval depending on data length but not significantly
            // slower and the difference is lost in the time taken for partitioning.
            // So always use the keys.
            final int unique = compressDuplicates(k, n);
            return newUpdatingInterval(k, unique);
        }

        // At least 20 indices that are partially unordered.

        // Find min/max to understand the range.
        int min = k[n - 1];
        int max = min;
        for (int i = n - 2; i >= 0; i--) {
            min = Math.min(min, k[i]);
            max = Math.max(max, k[i]);
        }

        // Here we use a simple test based on the number of comparisons required
        // to perform the expected next/previous look-ups after a split.
        // It is expected that we can cut n keys a maximum of n-1 times.
        // Each cut requires a scan next/previous to divide the interval into two intervals:
        //
        //            cut
        //             |
        //        k1--------k2---------k3---- ... ---------kn    initial interval
        //         <--| find previous
        //    find next |-->
        //        k1        k2---------k3---- ... ---------kn    divided intervals
        //
        // An BitSet will scan from the cut location and find a match in time proportional to
        // the index density. Average density is (size / n) and the scanning covers 64
        // indices together: Order(2 * n * (size / n) / 64) = Order(size / 32)

        // Sorted keys: Sort time Order(n log(n)) : Splitting time Order(log(n)) (binary search approx)
        // Bit keys   : Sort time Order(1)        : Splitting time Order(size / 32)

        // Transition when n * n ~ size / 32
        // Benchmarking shows this is a reasonable approximation when size < 2^20.
        // The speed of the bit keys is approximately independent of n and proportional to size.
        // Large size observes degrading performance of the bit keys vs sorted keys.
        // We introduce a penalty for each 4x increase over size = 2^20.
        // n * n = size/32 * 2^log4(size / 2^20)
        // The transition point still favours the bit keys when sorted keys would be faster.
        // However the difference is held within 4x and the BitSet type structure is still fast
        // enough to be negligible against the speed of partitioning.

        // Transition point: n = sqrt(size/32)
        // size n
        // 2^10 5.66
        // 2^15 32.0
        // 2^20 181.0

        // Transition point: n = sqrt(size/32 * 2^(log4(size/2^20))))
        // size n
        // 2^22 512.0
        // 2^24 1448.2
        // 2^28 11585
        // 2^31 55108

        final int size = max - min + 1;

        // Divide by 32 is a shift of 5. This is reduced for each 4-fold size above 2^20.
        // At 2^31 the shift reduces to 0.
        int shift = 5;
        if (size > (1 << 20)) {
            // log4(size/2^20) == (log2(size) - 20) / 2
            shift -= (ceilLog2(size) - 20) >>> 1;
        }

        if ((long) n * n > (size >> shift)) {
            final BitIndexUpdatingInterval interval = new BitIndexUpdatingInterval(min, max);
            for (int i = n; --i >= 0;) {
                interval.set(k[i]);
            }
            return interval;
        }

        // Sort with a hash set to filter indices
        final int unique = Sorting.sortIndices(k, n);
        return new KeyUpdatingInterval(k, unique);
    }

    /**
     * Test the data is in ascending order: {@code data[i] <= data[i+1]} for all {@code i}.
     * Data is assumed to be at least length 1.
     *
     * @param data Data.
     * @param n Length of data.
     * @return true if ascending
     */
    private static boolean isAscending(int[] data, int n) {
        for (int i = 1; i < n; i++) {
            if (data[i] < data[i - 1]) {
                // descending
                return false;
            }
        }
        return true;
    }

    /**
     * Compress duplicates in the ascending data.
     *
     * <p>Warning: Requires {@code n > 0}.
     *
     * @param data Indices.
     * @param n Number of indices.
     * @return the number of unique indices
     */
    private static int compressDuplicates(int[] data, int n) {
        // Compress to remove duplicates
        int last = 0;
        int top = data[0];
        for (int i = 1; i < n; i++) {
            final int v = data[i];
            if (v == top) {
                continue;
            }
            top = v;
            data[++last] = v;
        }
        return last + 1;
    }

    /**
     * Compute {@code ceil(log2(x))}. This is valid for all strictly positive {@code x}.
     *
     * <p>Returns -1 for {@code x = 0} in place of -infinity.
     *
     * @param x Value.
     * @return {@code ceil(log2(x))}
     */
    private static int ceilLog2(int x) {
        return 32 - Integer.numberOfLeadingZeros(x - 1);
    }

    /**
     * Returns an interval that covers the specified indices {@code k}.
     * The indices must be sorted.
     *
     * @param k Indices.
     * @param n Count of indices (must be strictly positive).
     * @throws IndexOutOfBoundsException if any index {@code k} is not within the
     * sub-range {@code [left, right]}
     * @return the interval
     */
    private static UpdatingInterval newUpdatingInterval(int[] k, int n) {
        return new KeyUpdatingInterval(k, n);
    }

    /**
     * Count the number of indices. Returns a negative value if the indices are sorted.
     *
     * @param keys Keys.
     * @param n Count of indices.
     * @return the count of (sorted) indices
     */
    static int countIndices(UpdatingInterval keys, int n) {
        if (keys instanceof KeyUpdatingInterval) {
            return -((KeyUpdatingInterval) keys).size();
        }
        return n;
    }

    /**
     * Checks if the sub-range from fromIndex (inclusive) to toIndex (exclusive) is
     * within the bounds of range from 0 (inclusive) to length (exclusive).
     *
     * <p>This function provides the functionality of
     * {@code java.utils.Objects.checkFromToIndex} introduced in JDK 9. The <a
     * href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Objects.html#checkFromToIndex(int,int,int)">Objects</a>
     * javadoc has been reproduced for reference. The return value has been changed
     * to void.
     *
     * <p>The sub-range is defined to be out of bounds if any of the following
     * inequalities is true:
     * <ul>
     * <li>{@code fromIndex < 0}
     * <li>{@code fromIndex > toIndex}
     * <li>{@code toIndex > length}
     * <li>{@code length < 0}, which is implied from the former inequalities
     * </ul>
     *
     * @param fromIndex Lower-bound (inclusive) of the sub-range.
     * @param toIndex Upper-bound (exclusive) of the sub-range.
     * @param length Upper-bound (exclusive) of the range.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    static void checkFromToIndex(int fromIndex, int toIndex, int length) {
        // Checks as documented above
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length) {
            throw new IndexOutOfBoundsException(
                msgRangeOutOfBounds(fromIndex, toIndex, length));
        }
    }

    /**
     * Checks if the {@code index} is within the half-open interval {@code [fromIndex, toIndex)}.
     *
     * @param fromIndex Lower-bound (inclusive) of the sub-range.
     * @param toIndex Upper-bound (exclusive) of the sub-range.
     * @param k Indices.
     * @throws IndexOutOfBoundsException if any index is out of bounds
     */
    static void checkIndices(int fromIndex, int toIndex, int[] k) {
        for (final int i : k) {
            checkIndex(fromIndex, toIndex, i);
        }
    }

    /**
     * Checks if the {@code index} is within the half-open interval {@code [fromIndex, toIndex)}.
     *
     * @param fromIndex Lower-bound (inclusive) of the sub-range.
     * @param toIndex Upper-bound (exclusive) of the sub-range.
     * @param index Index.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    static void checkIndex(int fromIndex, int toIndex, int index) {
        if (index < fromIndex || index >= toIndex) {
            throw new IndexOutOfBoundsException(
                msgIndexOutOfBounds(fromIndex, toIndex, index));
        }
    }

    // Message formatting moved to separate methods to assist inlining of the validation methods.

    /**
     * Format a message when range [from, to) is not entirely within the length.
     *
     * @param fromIndex Lower-bound (inclusive) of the sub-range.
     * @param toIndex Upper-bound (exclusive) of the sub-range.
     * @param length Upper-bound (exclusive) of the range.
     * @return the message
     */
    private static String msgRangeOutOfBounds(int fromIndex, int toIndex, int length) {
        return String.format("Range [%d, %d) out of bounds for length %d", fromIndex, toIndex, length);
    }

    /**
     * Format a message when index is not within range [from, to).
     *
     * @param fromIndex Lower-bound (inclusive) of the sub-range.
     * @param toIndex Upper-bound (exclusive) of the sub-range.
     * @param index Index.
     * @return the message
     */
    private static String msgIndexOutOfBounds(int fromIndex, int toIndex, int index) {
        return String.format("Index %d out of bounds for range [%d, %d)", index, fromIndex, toIndex);
    }
}
