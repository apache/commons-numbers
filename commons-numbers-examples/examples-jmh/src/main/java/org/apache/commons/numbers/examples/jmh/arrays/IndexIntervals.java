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
 * Support for creating {@link SearchableInterval}, {@link SearchableInterval2} and
 * {@link UpdatingInterval} implementations.
 *
 * @since 1.2
 */
final class IndexIntervals {
    /** Size to perform key analysis. This avoids key analysis for a small number of keys. */
    private static final int KEY_ANALYSIS_SIZE = 10;
    /** The upper threshold to use a modified insertion sort to find unique indices. */
    private static final int INDICES_INSERTION_SORT_SIZE = 20;

    /** Size to use a {@link BinarySearchKeyInterval}. Note that the
     * {@link ScanningKeyInterval} uses points within the range to fast-forward
     * scanning which improves performance significantly for a few hundred indices.
     * Performance is similar when indices are in the thousands. Binary search is
     * much faster when there are multiple thousands of indices. */
    private static final int BINARY_SEARCH_SIZE = 2048;

    /** No instances. */
    private IndexIntervals() {}

    /**
     * Returns an interval that covers all indices ({@code [0, MAX_VALUE)}).
     *
     * <p>When used with a partition algorithm will cause a full sort
     * of the range between the bounds {@code [ka, kb]}.
     *
     * @return the interval
     */
    static SearchableInterval anyIndex() {
        return AnyIndex.INSTANCE;
    }

    /**
     * Returns an interval that covers all indices ({@code [0, MAX_VALUE)}).
     *
     * <p>When used with a partition algorithm will cause a full sort
     * of the range between the bounds {@code [ka, kb]}.
     *
     * @return the interval
     */
    static SearchableInterval2 anyIndex2() {
        return AnyIndex.INSTANCE;
    }

    /**
     * Returns an interval that covers a single index {@code k}. The interval cannot
     * be split or the bounds updated.
     *
     * @param k Index.
     * @return the interval
     */
    static UpdatingInterval interval(int k) {
        return new PointInterval(k);
    }

    /**
     * Returns an interval that covers all indices {@code [left, right]}.
     * This method will sort the input bound to ensure {@code left <= right}.
     *
     * <p>When used with a partition algorithm will cause a full sort
     * of the range between the bounds {@code [left, right]}.
     *
     * @param left Left bound (inclusive).
     * @param right Right bound (inclusive).
     * @return the interval
     */
    static UpdatingInterval interval(int left, int right) {
        // Sort the bound
        final int l = left < right ? left : right;
        final int r = left < right ? right : left;
        return new RangeInterval(l, r);
    }

    /**
     * Returns an interval that covers the specified indices {@code k}.
     *
     * @param k Indices.
     * @param n Count of indices (must be strictly positive).
     * @return the interval
     */
    static SearchableInterval createSearchableInterval(int[] k, int n) {
        // Note: A typical use case is to have a few indices. Thus the heuristics
        // in this method should be very fast when n is small. Here we skip them
        // completely when the number of keys is tiny.

        if (n > KEY_ANALYSIS_SIZE) {
            // Here we use a simple test based on the number of comparisons required
            // to perform the expected next/previous look-ups.
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
            // A ScanningKeyIndexInterval will scan n keys in both directions using n comparisons
            // (if next takes m comparisons then previous will take n - m comparisons): Order(n^2)
            // An IndexSet will scan from the cut location and find a match in time proportional to
            // the index density. Average density is (size / n) and the scanning covers 64
            // indices together: Order(2 * n * (size / n) / 64) = Order(size / 32)

            // Get the range. This will throw an exception if there are no indices.
            int min = k[n - 1];
            int max = min;
            for (int i = n - 1; --i >= 0;) {
                min = Math.min(min, k[i]);
                max = Math.max(max, k[i]);
            }

            // Transition when n * n ~ size / 32
            // Benchmarking shows this is a reasonable approximation when size is small.
            // Speed of the IndexSet is approximately independent of n and proportional to size.
            // Large size observes degrading performance more than expected from a linear relationship.
            // Note the memory required is approximately (size / 8) bytes.
            // We introduce a penalty for each 4x increase over size = 2^20 (== 128KiB).
            // n * n = size/32 * 2^log4(size / 2^20)

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
                // Do not call IndexSet.of(k, n) which repeats the min/max search
                // (especially given n is likely to be large).
                final IndexSet interval = IndexSet.ofRange(min, max);
                for (int i = n; --i >= 0;) {
                    interval.set(k[i]);
                }
                return interval;
            }

            // Switch to binary search above a threshold.
            // Note this invalidates the speed assumptions based on the number of comparisons.
            // Benchmarking shows this is useful when the keys are in the thousands so this
            // would be used when data size is in the millions.
            if (n > BINARY_SEARCH_SIZE) {
                final int unique = Sorting.sortIndices2(k, n);
                return BinarySearchKeyInterval.of(k, unique);
            }

            // Fall-though to the ScanningKeyIndexInterval...
        }

        // This is the typical use case.
        // Here n is small, or small compared to the min/max range of indices.
        // Use a special method to sort unique indices (detects already sorted indices).
        final int unique = Sorting.sortIndices2(k, n);

        return ScanningKeyInterval.of(k, unique);
    }

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
        if (n < 3) {
            if (n == 1 || k[0] == k[1]) {
                // 1 unique value
                return IndexIntervals.interval(k[0]);
            }
            // 2 unique values
            if (Math.abs(k[0] - k[1]) == 1) {
                // Small range
                return IndexIntervals.interval(k[0], k[1]);
            }
            // 2 well separated values
            if (k[1] < k[0]) {
                final int v = k[0];
                k[0] = k[1];
                k[1] = v;
            }
            return KeyUpdatingInterval.of(k, 2);
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

        if (n <= INDICES_INSERTION_SORT_SIZE) {
            final int unique = Sorting.sortIndicesInsertionSort(k, n);
            return KeyUpdatingInterval.of(k, unique);
        }

        if (isAscending(k, n)) {
            // For sorted keys the KeyUpdatingInterval is fast. It may be slower than the
            // BitIndexUpdatingInterval depending on data length but not significantly
            // slower and the difference is lost in the time taken for partitioning.
            // So always use the keys.
            final int unique = compressDuplicates(k, n);
            return KeyUpdatingInterval.of(k, unique);
        }

        // At least 20 indices that are partially unordered.

        // Find min/max to understand the range.
        int min = k[n - 1];
        int max = min;
        for (int i = n - 1; --i >= 0;) {
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
            // Do not call BitIndexUpdatingInterval.of(k, n) which repeats the min/max search
            // (especially given n is likely to be large).
            final BitIndexUpdatingInterval interval = BitIndexUpdatingInterval.ofRange(min, max);
            for (int i = n; --i >= 0;) {
                interval.set(k[i]);
            }
            return interval;
        }

        // Sort with a hash set to filter indices
        final int unique = Sorting.sortIndicesHashIndexSet(k, n);
        return KeyUpdatingInterval.of(k, unique);
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
        for (int i = 0; ++i < n;) {
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
        for (int i = 0; ++i < n;) {
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
     * {@link SearchableInterval} for range {@code [0, MAX_VALUE)}.
     */
    private static final class AnyIndex implements SearchableInterval, SearchableInterval2 {
        /** Singleton instance. */
        static final AnyIndex INSTANCE = new AnyIndex();

        @Override
        public int left() {
            return 0;
        }

        @Override
        public int right() {
            return Integer.MAX_VALUE - 1;
        }

        @Override
        public int previousIndex(int k) {
            return k;
        }

        @Override
        public int nextIndex(int k) {
            return k;
        }

        @Override
        public int split(int ka, int kb, int[] upper) {
            upper[0] = kb + 1;
            return ka - 1;
        }

        // IndexInterval2
        // This is exactly the same as IndexInterval as the pointers i are the same as the keys k

        @Override
        public int start() {
            return 0;
        }

        @Override
        public int end() {
            return Integer.MAX_VALUE - 1;
        }

        @Override
        public int index(int i) {
            return i;
        }

        @Override
        public int previous(int i, int k) {
            return k;
        }

        @Override
        public int next(int i, int k) {
            return k;
        }

        @Override
        public int split(int i1, int i2, int ka, int kb, int[] upper) {
            upper[0] = kb + 1;
            return ka - 1;
        }
    }

    /**
     * {@link UpdatingInterval} for a single {@code index}.
     */
    static final class PointInterval implements UpdatingInterval {
        /** Left/right bound of the interval. */
        private final int index;

        /**
         * @param k Left/right bound.
         */
        PointInterval(int k) {
            this.index = k;
        }

        @Override
        public int left() {
            return index;
        }

        @Override
        public int right() {
            return index;
        }

        // Note: An UpdatingInterval is only required to update when a target index
        // is within [left, right]. This is not possible for a single point.

        @Override
        public int updateLeft(int k) {
            throw new UnsupportedOperationException("updateLeft should not be called");
        }

        @Override
        public int updateRight(int k) {
            throw new UnsupportedOperationException("updateRight should not be called");
        }

        @Override
        public UpdatingInterval splitLeft(int ka, int kb) {
            throw new UnsupportedOperationException("splitLeft should not be called");
        }

        @Override
        public UpdatingInterval splitRight(int ka, int kb) {
            throw new UnsupportedOperationException("splitRight should not be called");
        }
    }

    /**
     * {@link UpdatingInterval} for range {@code [left, right]}.
     */
    static final class RangeInterval implements UpdatingInterval {
        /** Left bound of the interval. */
        private int left;
        /** Right bound of the interval. */
        private int right;

        /**
         * @param left Left bound.
         * @param right Right bound.
         */
        RangeInterval(int left, int right) {
            this.left = left;
            this.right = right;
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
            // Assume left < k <= right
            left = k;
            return k;
        }

        @Override
        public int updateRight(int k) {
            // Assume left <= k < right
            right = k;
            return k;
        }

        @Override
        public UpdatingInterval splitLeft(int ka, int kb) {
            // Assume left < ka <= kb < right
            final int lower = left;
            left = kb + 1;
            return new RangeInterval(lower, ka - 1);
        }

        @Override
        public UpdatingInterval splitRight(int ka, int kb) {
            // Assume left < ka <= kb < right
            final int upper = right;
            right = ka - 1;
            return new RangeInterval(kb + 1, upper);
        }
    }
}
