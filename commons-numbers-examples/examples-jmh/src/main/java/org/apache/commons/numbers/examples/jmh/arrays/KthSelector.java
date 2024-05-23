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

import java.util.Arrays;
import java.util.BitSet;

/**
 * A K<sup>th</sup> selector implementation to pick up the K<sup>th</sup> ordered element
 * from a data array containing the input numbers. Uses a partial sort of the data.
 *
 * <p>Note: The search may use a cache of pivots. A pivot is a K<sup>th</sup> index that
 * corresponds to a value correctly positioned within the equivalent fully sorted data array.
 * Each step of the algorithm partitions the array between a lower and upper bound into
 * values above or below a value chosen from the interval. The index of this value after the
 * partition is stored as a pivot. A subsequent search into the array can use the pivots to
 * quickly bracket the interval for the next target index.
 *
 * <p>The maximum supported cache size is 2^30 - 1. This ensures that any valid index i
 * into the cache can be increased for the next level using 2 * i + 2 without overflow.
 *
 * <p>The method for choosing the value within the interval to pivot around is specified by
 * the {@link PivotingStrategy}. Ideally this should provide a guess of the middle (median)
 * of the interval. The value must be within the interval, thus using for example the
 * mean of the end values is not an option. The default uses a guess for the median from
 * 3 values that are likely to be representative of the range of values.
 *
 * <p>This implementation provides the option to return the (K+1) ordered element along with
 * the K<sup>th</sup>. This uses the position of K and the most recent upper bound on the
 * bracket known to contain values greater than the K<sup>th</sup>. This prevents using a
 * second search into the array for the (K+1) element.
 *
 * @since 1.2
 */
class KthSelector {
    /** Empty pivots array. */
    static final int[] NO_PIVOTS = {};
    /** Minimum selection size for insertion sort rather than selection.
     * Dual-pivot quicksort used 27 in the original paper. */
    private static final int MIN_SELECT_SIZE = 17;

    /** A {@link PivotingStrategy} used for pivoting. */
    private final PivotingStrategy pivotingStrategy;

    /** Minimum selection size for insertion sort rather than selection. */
    private final int minSelectSize;

    /**
     * Constructor with default {@link PivotingStrategy#MEDIAN_OF_3 median of 3} pivoting
     * strategy.
     */
    KthSelector() {
        this(PivotingStrategy.MEDIAN_OF_3);
    }

    /**
     * Constructor with specified pivoting strategy.
     *
     * @param pivotingStrategy Pivoting strategy to use.
     */
    KthSelector(PivotingStrategy pivotingStrategy) {
        this(pivotingStrategy, MIN_SELECT_SIZE);
    }

    /**
     * Constructor with specified pivoting strategy and select size.
     *
     * @param pivotingStrategy Pivoting strategy to use.
     * @param minSelectSize Minimum selection size for insertion sort rather than selection.
     */
    KthSelector(PivotingStrategy pivotingStrategy, int minSelectSize) {
        this.pivotingStrategy = pivotingStrategy;
        this.minSelectSize = minSelectSize;
    }

    /**
     * Select K<sup>th</sup> value in the array. Optionally select the next value after K.
     *
     * <p>Note: If K+1 is requested this method assumes it is a valid index into the array.
     *
     * <p>Uses a single-pivot partition method.
     *
     * @param data Data array to use to find out the K<sup>th</sup> value.
     * @param k Index whose value in the array is of interest.
     * @param kp1 K+1<sup>th</sup> value (if not null)
     * @return K<sup>th</sup> value
     */
    double selectSP(double[] data, int k, double[] kp1) {
        int begin = 0;
        int end = data.length;
        while (end - begin > minSelectSize) {
            // Select a pivot and partition data array around it
            final int pivot = partitionSP(data, begin, end,
                pivotingStrategy.pivotIndex(data, begin, end - 1, k));
            if (k == pivot) {
                // The pivot was exactly the element we wanted
                return finalSelection(data, k, kp1, end);
            } else if (k < pivot) {
                // The element is in the left partition
                end = pivot;
            } else {
                // The element is in the right partition
                begin = pivot + 1;
            }
        }
        sortRange(data, begin, end);
        if (kp1 != null) {
            // Either end == data.length and k+1 is sorted; or
            // end == pivot where data[k] <= data[pivot] <= data[pivot+j] for all j
            kp1[0] = data[k + 1];
        }
        return data[k];
    }

    /**
     * Select K<sup>th</sup> value in the array. Optionally select the next value after K.
     *
     * <p>Note: If K+1 is requested this method assumes it is a valid index into the array.
     *
     * <p>Uses a single-pivot partition method with special handling of NaN and signed zeros.
     * Correction of signed zeros requires a sweep across the entire range.
     *
     * @param data Data array to use to find out the K<sup>th</sup> value.
     * @param k Index whose value in the array is of interest.
     * @param kp1 K+1<sup>th</sup> value (if not null)
     * @return K<sup>th</sup> value
     */
    double selectSPN(double[] data, int k, double[] kp1) {
        // Handle NaN
        final int length = sortNaN(data);
        if (k >= length) {
            if (kp1 != null) {
                kp1[0] = Double.NaN;
            }
            return Double.NaN;
        }

        int begin = 0;
        int end = length;
        while (end - begin > minSelectSize) {
            // Select a pivot and partition data array around it
            final int pivot = partitionSPN(data, begin, end,
                pivotingStrategy.pivotIndex(data, begin, end - 1, k));
            if (k == pivot) {
                // The pivot was exactly the element we wanted
                if (data[k] == 0) {
                    orderSignedZeros(data, 0, length);
                }
                return finalSelection(data, k, kp1, end);
            } else if (k < pivot) {
                // The element is in the left partition
                end = pivot;
            } else {
                // The element is in the right partition
                begin = pivot + 1;
            }
        }
        insertionSort(data, begin, end, begin != 0);
        if (data[k] == 0) {
            orderSignedZeros(data, 0, length);
        }
        if (kp1 != null) {
            // Either end == data.length and k+1 is sorted; or
            // end == pivot where data[k] <= data[pivot] <= data[pivot+j] for all j
            kp1[0] = data[k + 1];
        }
        return data[k];
    }

    /**
     * Select K<sup>th</sup> value in the array. Optionally select the next value after K.
     *
     * <p>Note: If K+1 is requested this method assumes it is a valid index into the array.
     *
     * <p>Uses a single-pivot partition method with a heap cache.
     *
     * <p>This method can be used for repeat calls to identify K<sup>th</sup> values in
     * the same array by caching locations of pivots. Maximum supported heap size is 2^30 - 1.
     *
     * @param data Data array to use to find out the K<sup>th</sup> value.
     * @param pivotsHeap Cached pivots heap that can be used for efficient estimation.
     * @param k Index whose value in the array is of interest.
     * @param kp1 K+1<sup>th</sup> value (if not null)
     * @return K<sup>th</sup> value
     */
    double selectSPH(double[] data, int[] pivotsHeap, int k, double[] kp1) {
        final int heapLength = pivotsHeap.length;
        if (heapLength == 0) {
            // No pivots
            return selectSP(data, k, kp1);
        }
        int begin = 0;
        int end = data.length;
        int node = 0;
        while (end - begin > minSelectSize) {
            int pivot;

            if (node < heapLength && pivotsHeap[node] >= 0) {
                // The pivot has already been found in a previous call
                // and the array has already been partitioned around it
                pivot = pivotsHeap[node];
            } else {
                // Select a pivot and partition data array around it
                pivot = partitionSP(data, begin, end,
                    pivotingStrategy.pivotIndex(data, begin, end - 1, k));
                if (node < heapLength) {
                    pivotsHeap[node] = pivot;
                }
            }

            if (k == pivot) {
                // The pivot was exactly the element we wanted
                return finalSelection(data, k, kp1, end);
            } else if (k < pivot) {
                // The element is in the left partition
                end = pivot;
                if (node < heapLength) {
                    node = Math.min((node << 1) + 1, heapLength);
                }
            } else {
                // The element is in the right partition
                begin = pivot + 1;
                if (node < heapLength) {
                    node = Math.min((node << 1) + 2, heapLength);
                }
            }
        }
        sortRange(data, begin, end);
        if (kp1 != null) {
            // Either end == data.length and k+1 is sorted; or
            // end == pivot where data[k] <= data[pivot] <= data[pivot+j] for all j
            kp1[0] = data[k + 1];
        }
        return data[k];
    }

    /**
     * Select K<sup>th</sup> value in the array. Optionally select the next value after K.
     *
     * <p>Note: If K+1 is requested this method assumes it is a valid index into the array
     * (i.e. K is not the last index in the array).
     *
     * @param data Data array to use to find out the K<sup>th</sup> value.
     * @param k Index whose value in the array is of interest.
     * @param kp1 K+1<sup>th</sup> value (if not null)
     * @param end Upper bound (exclusive) of the interval containing K.
     * This should be either a pivot point {@code data[k] <= data[end]} or the length
     * of the data array.
     * @return K<sup>th</sup> value
     */
    private static double finalSelection(double[] data, int k, double[] kp1, int end) {
        if (kp1 != null) {
            // After partitioning all elements above k are greater than or equal to k.
            // Find the minimum of the elements above.
            // Set the k+1 limit as either a pivot or the end of the data.
            final int limit = Math.min(end, data.length - 1);
            double min = data[k + 1];
            for (int i = k + 2; i <= limit; i++) {
                if (DoubleMath.lessThan(data[i], min)) {
                    min = data[i];
                }
            }
            kp1[0] = min;
        }
        return data[k];
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array. For all indices {@code k}
     * and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>Uses a single-pivot partition method.
     *
     * @param data Values.
     * @param k Indices.
     */
    void partitionSP(double[] data, int... k) {
        final int n = k.length;
        if (n <= 1) {
            if (n == 1) {
                selectSP(data, k[0], null);
            }
            return;
        }
        // Multiple pivots
        final int length = data.length;
        final BitSet pivots = new BitSet(length);

        for (int i = 0; i < n; i++) {
            final int kk = k[i];
            if (pivots.get(kk)) {
                // Already sorted
                continue;
            }
            int begin;
            int end;
            if (i == 0) {
                begin = 0;
                end = length;
            } else {
                // Start inclusive
                begin = pivots.previousSetBit(kk) + 1;
                end = pivots.nextSetBit(kk + 1);
                if (end < 0) {
                    end = length;
                }
            }
            partitionSP(data, begin, end, pivots, kk);
        }
    }

    /**
     * Partition around the K<sup>th</sup> value in the array.
     *
     * <p>This method can be used for repeat calls to identify K<sup>th</sup> values in
     * the same array by caching locations of pivots (correctly sorted indices).
     *
     * <p>Uses a single-pivot partition method.
     *
     * @param data Data array to use to find out the K<sup>th</sup> value.
     * @param begin Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     * @param pivots Cache of pivot points.
     * @param k Index whose value in the array is of interest.
     */
    private void partitionSP(double[] data, int begin, int end, BitSet pivots, int k) {
        // Find the unsorted range containing k
        while (end - begin > minSelectSize) {
            // Select a value and partition data array around it
            final int pivot = partitionSP(data, begin, end,
                pivotingStrategy.pivotIndex(data, begin, end - 1, k));
            pivots.set(pivot);
            if (k == pivot) {
                // The pivot was exactly the element we wanted
                return;
            } else if (k < pivot) {
                // The element is in the left partition
                end = pivot;
            } else {
                // The element is in the right partition
                begin = pivot + 1;
            }
        }
        setPivots(begin, end, pivots);
        sortRange(data, begin, end);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Uses a single-pivot partition method.
     *
     * @param data Data array.
     * @param begin Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     * @param pivot Initial index of the pivot.
     * @return index of the pivot after partition
     */
    private static int partitionSP(double[] data, int begin, int end, int pivot) {
        final double value = data[pivot];
        data[pivot] = data[begin];

        int i = begin + 1;
        int j = end - 1;
        while (i < j) {
            while (i < j && DoubleMath.greaterThan(data[j], value)) {
                --j;
            }
            while (i < j && DoubleMath.lessThan(data[i], value)) {
                ++i;
            }
            if (i < j) {
                final double tmp = data[i];
                data[i++] = data[j];
                data[j--] = tmp;
            }
        }

        if (i >= end || DoubleMath.greaterThan(data[i], value)) {
            --i;
        }
        data[begin] = data[i];
        data[i] = value;
        return i;
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array. For all indices {@code k}
     * and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>Uses a single-pivot partition method.
     *
     * @param data Values.
     * @param k Indices.
     */
    void partitionSPN(double[] data, int... k) {
        final int n = k.length;
        if (n <= 1) {
            if (n == 1) {
                selectSPN(data, k[0], null);
            }
            return;
        }
        // Multiple pivots

        // Handle NaN
        final int length = sortNaN(data);
        if (length < 1) {
            return;
        }

        final BitSet pivots = new BitSet(length);

        // Flag any pivots that are zero
        boolean zeros = false;
        for (int i = 0; i < n; i++) {
            final int kk = k[i];
            if (kk >= length || pivots.get(kk)) {
                // Already sorted
                continue;
            }
            int begin;
            int end;
            if (i == 0) {
                begin = 0;
                end = length;
            } else {
                // Start inclusive
                begin = pivots.previousSetBit(kk) + 1;
                end = pivots.nextSetBit(kk + 1);
                if (end < 0) {
                    end = length;
                }
            }
            partitionSPN(data, begin, end, pivots, kk);
            zeros = zeros || data[kk] == 0;
        }

        // Handle signed zeros
        if (zeros) {
            orderSignedZeros(data, 0, length);
        }
    }

    /**
     * Partition around the K<sup>th</sup> value in the array.
     *
     * <p>This method can be used for repeat calls to identify K<sup>th</sup> values in
     * the same array by caching locations of pivots (correctly sorted indices).
     *
     * <p>Note: Requires that the range contains no NaN values. Does not partition
     * around signed zeros.
     *
     * <p>Uses a single-pivot partition method.
     *
     * @param data Data array to use to find out the K<sup>th</sup> value.
     * @param begin Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     * @param pivots Cache of pivot points.
     * @param k Index whose value in the array is of interest.
     */
    private void partitionSPN(double[] data, int begin, int end, BitSet pivots, int k) {
        // Find the unsorted range containing k
        while (end - begin > minSelectSize) {
            // Select a value and partition data array around it
            final int pivot = partitionSPN(data, begin, end,
                pivotingStrategy.pivotIndex(data, begin, end - 1, k));
            pivots.set(pivot);
            if (k == pivot) {
                // The pivot was exactly the element we wanted
                return;
            } else if (k < pivot) {
                // The element is in the left partition
                end = pivot;
            } else {
                // The element is in the right partition
                begin = pivot + 1;
            }
        }
        setPivots(begin, end, pivots);
        sortRange(data, begin, end);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Note: Requires that the range contains no NaN values. Does not partition
     * around signed zeros.
     *
     * <p>Uses a single-pivot partition method.
     *
     * @param data Data array.
     * @param begin Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     * @param pivot Initial index of the pivot.
     * @return index of the pivot after partition
     */
    private static int partitionSPN(double[] data, int begin, int end, int pivot) {
        final double value = data[pivot];
        data[pivot] = data[begin];

        int i = begin + 1;
        int j = end - 1;
        while (i < j) {
            while (i < j && data[j] > value) {
                --j;
            }
            while (i < j && data[i] < value) {
                ++i;
            }
            if (i < j) {
                final double tmp = data[i];
                data[i++] = data[j];
                data[j--] = tmp;
            }
        }

        if (i >= end || data[i] > value) {
            --i;
        }
        data[begin] = data[i];
        data[i] = value;
        return i;
    }

    /**
     * Sort an array range.
     *
     * @param data Data array.
     * @param begin Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     */
    private static void sortRange(double[] data, int begin, int end) {
        Arrays.sort(data, begin, end);
    }

    /**
     * Sorts an array using an insertion sort.
     *
     * <p>Note: Requires that the range contains no NaN values. It does not respect the
     * order of signed zeros.
     *
     * <p>This method is fast up to approximately 40 - 80 values.
     *
     * <p>The {@code internal} flag indicates that the value at {@code data[begin - 1]}
     * is sorted.
     *
     * @param data Data array.
     * @param begin Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     * @param internal Internal flag.
     */
    private static void insertionSort(double[] data, int begin, int end, boolean internal) {
        Sorting.sort(data, begin, end - 1, internal);
    }

    /**
     * Move NaN values to the end of the array.
     * This allows all other values to be compared using {@code <, ==, >} operators (with
     * the exception of signed zeros).
     *
     * @param data Values.
     * @return end of non-NaN values
     */
    private static int sortNaN(double[] data) {
        int end = data.length;
        // Avoid unnecessary moves
        while (--end > 0) {
            if (!Double.isNaN(data[end])) {
                break;
            }
        }
        end++;
        for (int i = end; i > 0;) {
            final double v = data[--i];
            if (Double.isNaN(v)) {
                data[i] = data[--end];
                data[end] = v;
            }
        }
        return end;
    }

    /**
     * Detect and fix the sort order of signed zeros. Assumes the data may have been
     * partially ordered around zero.
     *
     * <p>Searches for zeros if {@code data[begin] <= 0} and {@code data[end - 1] >= 0}.
     * If zeros are discovered in the range then they are assumed to be continuous.
     *
     * @param data Values.
     * @param begin Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     */
    private static void fixSignedZeros(double[] data, int begin, int end) {
        int j;
        if (data[begin] <= 0 && data[end - 1] >= 0) {
            int i = begin;
            while (data[i] < 0) {
                i++;
            }
            j = end - 1;
            while (data[j] > 0) {
                j--;
            }
            sortZero(data, i, j + 1);
        }
    }

    /**
     * Count the number of signed zeros in the range and order them to be correctly
     * sorted. This checks all values in the range. It does not assume zeros are continuous.
     *
     * @param data Values.
     * @param begin Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     */
    private static void orderSignedZeros(double[] data, int begin, int end) {
        int c = countSignedZeros(data, begin, end);
        if (c != 0) {
            int i = begin - 1;
            while (++i < end) {
                if (data[i] == 0) {
                    data[i] = -0.0;
                    if (--c == 0) {
                        break;
                    }
                }
            }
            while (++i < end) {
                if (data[i] == 0) {
                    data[i] = 0.0;
                }
            }
        }
    }

    /**
     * Count the number of signed zeros (-0.0).
     *
     * @param data Values.
     * @param begin Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     * @return the count
     */
    static int countSignedZeros(double[] data, int begin, int end) {
        // Count negative zeros
        int c = 0;
        for (int i = begin; i < end; i++) {
            if (data[i] == 0 && Double.doubleToRawLongBits(data[i]) < 0) {
                c++;
            }
        }
        return c;
    }

    /**
     * Sort a range of all zero values.
     * This orders -0.0 before 0.0.
     *
     * @param data Values.
     * @param begin Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     */
    static void sortZero(double[] data, int begin, int end) {
        // Count negative zeros
        int c = 0;
        for (int i = begin; i < end; i++) {
            if (Double.doubleToRawLongBits(data[i]) < 0) {
                c++;
            }
        }
        // Replace
        if (c != 0) {
            int i = begin;
            while (c-- > 0) {
                data[i++] = -0.0;
            }
            while (i < end) {
                data[i++] = 0.0;
            }
        }
    }

    /**
     * Sets the pivots.
     *
     * @param from Start (inclusive)
     * @param to End (exclusive)
     * @param pivots the pivots
     */
    private static void setPivots(int from, int to, BitSet pivots) {
        if (from + 1 == to) {
            pivots.set(from);
        } else {
            pivots.set(from, to);
        }
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array. For all indices {@code k}
     * and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>Uses a Bentley-McIlroy quicksort partition method by Sedgewick.
     *
     * @param data Values.
     * @param k Indices.
     */
    void partitionSBM(double[] data, int... k) {
        final int n = k.length;
        if (n < 1) {
            return;
        }

        // Handle NaN
        final int length = sortNaN(data);
        if (length < 1) {
            return;
        }

        if (n == 1) {
            if (k[0] < length) {
                partitionSBM(data, 0, length, k[0]);
            }
            return;
        }
        // Special case for partition around adjacent indices (for interpolation)
        if (n == 2 && k[0] + 1 == k[1]) {
            if (k[0] < length) {
                final int p = partitionSBM(data, 0, length, k[0]);
                if (p > k[1]) {
                    partitionMin(data, k[1], p);
                }
            }
            return;
        }

        // To partition all k requires not moving any pivot k after it has been
        // processed. This is supported using two strategies:
        //
        // 1. Processing k in sorted order:
        // (k1, end), (k2, end), (k3, end), ... , k1 <= k2 <= k3
        // This can reorder each region during processing without destroying sorted k.
        //
        // 2. Processing unique k and visiting array regions only once:
        // Pre-process the pivots to make them unique and store the entire sorted
        // region between the end pivots (k1, kn) in a BitSet type structure:
        // |k1|......|k2|....|p|k3|k4|pppp|......|kn|
        // k can be processed in any order, e.g. k3. We use already sorted regions
        // |p| to bracket the search for each k, and skip k that are already sorted (k4).
        // Worst case storage cost is Order(N / 64).
        // The advantage is never visiting any part of the array twice. If the pivots
        // saturate the entire range then performance degrades to the speed of
        // the sort of the entire array.

        // Multiple pivots
        final BitSet pivots = new BitSet(length);

        for (int i = 0; i < n; i++) {
            final int kk = k[i];
            if (kk >= length || pivots.get(kk)) {
                // Already sorted
                continue;
            }
            int begin;
            int end;
            if (i == 0) {
                begin = 0;
                end = length;
            } else {
                // Start inclusive
                begin = pivots.previousSetBit(kk) + 1;
                end = pivots.nextSetBit(kk + 1);
                if (end < 0) {
                    end = length;
                }
            }
            partitionSBM(data, begin, end, pivots, kk);
        }
    }

    /**
     * Move the minimum value to the start of the range.
     *
     * <p>Note: Requires that the range contains no NaN values.
     * Does not respect the ordering of signed zeros.
     *
     * @param data Data array to use to find out the K<sup>th</sup> value.
     * @param begin Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     */
    static void partitionMin(double[] data, int begin, int end) {
        int i = begin;
        double min = data[i];
        int j = i;
        while (++i < end) {
            if (data[i] < min) {
                min = data[i];
                j = i;
            }
        }
        //swap(data, begin, j)
        data[j] = data[begin];
        data[begin] = min;
    }

    /**
     * Move the maximum value to the end of the range.
     *
     * <p>Note: Requires that the range contains no NaN values.
     * Does not respect the ordering of signed zeros.
     *
     * @param data Data array to use to find out the K<sup>th</sup> value.
     * @param begin Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     */
    static void partitionMax(double[] data, int begin, int end) {
        int i = end - 1;
        double max = data[i];
        int j = i;
        while (--i >= begin) {
            if (data[i] > max) {
                max = data[i];
                j = i;
            }
        }
        //swap(data, end - 1, j)
        data[j] = data[end - 1];
        data[end - 1] = max;
    }

    /**
     * Partition around the K<sup>th</sup> value in the array.
     *
     * <p>This method can be used for repeat calls to identify K<sup>th</sup> values in
     * the same array by caching locations of pivots (correctly sorted indices).
     *
     * <p>Note: Requires that the range contains no NaN values.
     *
     * <p>Uses a Bentley-McIlroy quicksort partition method by Sedgewick.
     *
     * @param data Data array to use to find out the K<sup>th</sup> value.
     * @param begin Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     * @param pivots Cache of pivot points.
     * @param k Index whose value in the array is of interest.
     */
    private void partitionSBM(double[] data, int begin, int end, BitSet pivots, int k) {
        // Find the unsorted range containing k
        final int[] upper = {0};
        while (end - begin > minSelectSize) {
            // Select a value and partition data array around it
            final int from = partitionSBM(data, begin, end,
                pivotingStrategy.pivotIndex(data, begin, end - 1, k), upper);
            final int to = upper[0];
            setPivots(from, to, pivots);
            if (k >= to) {
                // The element is in the right partition
                begin = to;
            } else if (k < from) {
                // The element is in the left partition
                end = from;
            } else {
                // The range contains the element we wanted
                return;
            }
        }
        setPivots(begin, end, pivots);
        insertionSort(data, begin, end, begin != 0);
        fixSignedZeros(data, begin, end);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Note: Requires that the range contains no NaN values.
     *
     * <p>Uses a Bentley-McIlroy quicksort partition method by Sedgewick.
     *
     * @param data Data array.
     * @param begin Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     * @param pivot Initial index of the pivot.
     * @param upper Upper bound (exclusive) of the pivot range.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private static int partitionSBM(double[] data, int begin, int end, int pivot, int[] upper) {
        // Single-pivot Bentley-McIlroy quicksort handling equal keys (Sedgewick's algorithm).
        //
        // Partition data using pivot P into less-than, greater-than or equal.
        // P is placed at the end to act as a sentinal.
        // k traverses the unknown region ??? and values moved if equal (l) or greater (g):
        //
        // left    p       i            j         q    right
        // |  ==P  |  <P   |     ???    |   >P    | ==P  |P|
        //
        // At the end P and additional equal values are swapped back to the centre.
        //
        // |         <P        | ==P |            >P        |
        //
        // Adapted from Sedgewick "Quicksort is optimal"
        // https://sedgewick.io/wp-content/themes/sedgewick/talks/2002QuicksortIsOptimal.pdf
        //
        // The algorithm has been changed so that:
        // - A pivot point must be provided.
        // - An edge case where the search meets in the middle is handled.
        // - Equal value data is not swapped to the end. Since the value is fixed then
        //   only the less than / greater than value must be moved from the end inwards.
        //   The end is then assumed to be the equal value. This would not work with
        //   object references. Equivalent swap calls are commented.
        // - Added a fast-forward over initial range containing the pivot.

        final int l = begin;
        final int r = end - 1;

        int p = l;
        int q = r;

        // Use the pivot index to set the upper sentinal value
        final double v = data[pivot];
        data[pivot] = data[r];
        data[r] = v;

        // Special case: count signed zeros
        int c = 0;
        if (v == 0) {
            c = countSignedZeros(data, begin, end);
        }

        // Fast-forward over equal regions to reduce swaps
        while (data[p] == v) {
            if (++p == q) {
                // Edge-case: constant value
                if (c != 0) {
                    sortZero(data, begin, end);
                }
                upper[0] = end;
                return begin;
            }
        }
        // Cannot overrun as the prior scan using p stopped before the end
        while (data[q - 1] == v) {
            q--;
        }

        int i = p - 1;
        int j = q;

        for (;;) {
            do {
                ++i;
            } while (data[i] < v);
            while (v < data[--j]) {
                // Stop at l (not i) allows scan loops to be independent
                if (j == l) {
                    break;
                }
            }
            if (i >= j) {
                // Edge-case if search met on an internal pivot value
                // (not at the greater equal region, i.e. i < q).
                // Move this to the lower-equal region.
                if (i == j && v == data[i]) {
                    //swap(data, i++, p++)
                    //data[i++] = data[p++];
                    data[i++] = data[p];
                    data[p++] = v;
                }
                break;
            }
            //swap(data, i, j)
            final double vj = data[i];
            final double vi = data[j];
            data[i] = vi;
            data[j] = vj;
            if (vi == v) {
                //swap(data, i, p++)
                //data[i] = data[p++];
                data[i] = data[p];
                data[p++] = v;
            }
            if (vj == v) {
                //swap(data, j, --q)
                data[j] = data[--q];
                data[q] = v;
            }
        }
        // i is at the end (exclusive) of the less-than region

        // Place pivot value in centre
        //swap(data, r, i)
        data[r] = data[i];
        data[i] = v;

        // Move equal regions to the centre.
        // Set the pivot range [j, i) and move this outward for equal values.
        j = i++;

        // less-equal:
        //   for (int k = l; k < p; k++):
        //     swap(data, k, --j)
        // greater-equal:
        //   for (int k = r; k-- > q; i++) {
        //     swap(data, k, i)

        // Move the minimum of less-equal or less-than
        int move = Math.min(p - l, j - p);
        final int lower = j - (p - l);
        for (int k = l; move-- > 0; k++) {
            data[k] = data[--j];
            data[j] = v;
        }
        // Move the minimum of greater-equal or greater-than
        move = Math.min(r - q, q - i);
        upper[0] = i + (r - q);
        for (int k = r; move-- > 0; i++) {
            data[--k] = data[i];
            data[i] = v;
        }

        // Special case: fixed signed zeros
        if (c != 0) {
            p = lower;
            while (c-- > 0) {
                data[p++] = -0.0;
            }
            while (p < upper[0]) {
                data[p++] = 0.0;
            }
        }

        return lower;
    }

    /**
     * Partition around the K<sup>th</sup> value in the array.
     *
     * <p>This method can be used for repeat calls to identify K<sup>th</sup> values in
     * the same array by caching locations of pivots (correctly sorted indices).
     *
     * <p>Note: Requires that the range contains no NaN values.
     *
     * <p>Uses a Bentley-McIlroy quicksort partition method by Sedgewick.
     *
     * <p>Returns the last known pivot location adjacent to K.
     * If {@code p <= k} the range [p, min{k+2, data.length}) is sorted.
     * If {@code p > k} then p is a pivot.
     *
     * @param data Data array to use to find out the K<sup>th</sup> value.
     * @param begin Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     * @param k Index whose value in the array is of interest.
     * @return the bound index
     */
    private int partitionSBM(double[] data, int begin, int end, int k) {
        // Find the unsorted range containing k
        final int[] upper = {0};
        while (end - begin > minSelectSize) {
            // Select a value and partition data array around it
            final int from = partitionSBM(data, begin, end,
                pivotingStrategy.pivotIndex(data, begin, end - 1, k), upper);
            final int to = upper[0];
            if (k >= to) {
                // The element is in the right partition
                begin = to;
            } else if (k < from) {
                // The element is in the left partition
                end = from;
            } else {
                // The range contains the element we wanted
                return end;
            }
        }
        insertionSort(data, begin, end, begin != 0);
        fixSignedZeros(data, begin, end);
        // Either end == data.length and k+1 is sorted; or
        // end == pivot and k+1 is sorted
        return begin;
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array. For all indices {@code k}
     * and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>Uses a Bentley-McIlroy quicksort partition method.
     *
     * @param data Values.
     * @param k Indices.
     */
    void partitionBM(double[] data, int... k) {
        final int n = k.length;
        if (n < 1) {
            return;
        }

        // Handle NaN
        final int length = sortNaN(data);
        if (length < 1) {
            return;
        }

        if (n == 1) {
            partitionBM(data, 0, length, k[0]);
            return;
        }
        // Special case for partition around adjacent indices (for interpolation)
        if (n == 2 && k[0] + 1 == k[1]) {
            final int p = partitionBM(data, 0, length, k[0]);
            if (p > k[1]) {
                partitionMin(data, k[1], p);
            }
            return;
        }

        // Multiple pivots
        final BitSet pivots = new BitSet(length);

        for (int i = 0; i < n; i++) {
            final int kk = k[i];
            if (kk >= length || pivots.get(kk)) {
                // Already sorted
                continue;
            }
            int begin;
            int end;
            if (i == 0) {
                begin = 0;
                end = length;
            } else {
                // Start inclusive
                begin = pivots.previousSetBit(kk) + 1;
                end = pivots.nextSetBit(kk + 1);
                if (end < 0) {
                    end = length;
                }
            }
            partitionBM(data, begin, end, pivots, kk);
        }
    }

    /**
     * Partition around the K<sup>th</sup> value in the array.
     *
     * <p>This method can be used for repeat calls to identify K<sup>th</sup> values in
     * the same array by caching locations of pivots (correctly sorted indices).
     *
     * <p>Note: Requires that the range contains no NaN values.
     *
     * <p>Uses a Bentley-McIlroy quicksort partition method.
     *
     * @param data Data array to use to find out the K<sup>th</sup> value.
     * @param begin Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     * @param pivots Cache of pivot points.
     * @param k Index whose value in the array is of interest.
     */
    private void partitionBM(double[] data, int begin, int end, BitSet pivots, int k) {
        // Find the unsorted range containing k
        final int[] upper = {0};
        while (end - begin > minSelectSize) {
            // Select a value and partition data array around it
            final int from = partitionBM(data, begin, end,
                pivotingStrategy.pivotIndex(data, begin, end - 1, k), upper);
            final int to = upper[0];
            setPivots(from, to, pivots);
            if (k >= to) {
                // The element is in the right partition
                begin = to;
            } else if (k < from) {
                // The element is in the left partition
                end = from;
            } else {
                // The range contains the element we wanted
                return;
            }
        }
        setPivots(begin, end, pivots);
        insertionSort(data, begin, end, begin != 0);
        fixSignedZeros(data, begin, end);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Note: Requires that the range contains no NaN values.
     *
     * <p>Uses a Bentley-McIlroy quicksort partition method.
     *
     * @param data Data array.
     * @param begin Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     * @param pivot Initial index of the pivot.
     * @param upper Upper bound (exclusive) of the pivot range.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private static int partitionBM(double[] data, int begin, int end, int pivot, int[] upper) {
        // Partition method handling equal keys, Bentley-McIlroy quicksort.
        //
        // Adapted from program 7 in Bentley-McIlroy (1993)
        // Engineering a sort function
        // SOFTWARE—PRACTICE AND EXPERIENCE, VOL.23(11), 1249–1265
        //
        // 3-way partition of the data using a pivot value into
        // less-than, equal or greater-than.
        //
        // First partition data into 4 reqions by scanning the unknown region from
        // left (i) and right (j) and moving equal values to the ends:
        //                  i->       <-j                  end
        // l        p       |           |         q       r|
        // | equal  | less  |  unknown  | greater | equal ||
        //
        //                    <-j                          end
        // l        p             i               q       r|
        // | equal  | less        |       greater | equal ||
        //
        // Then the equal values are copied from the ends to the centre:
        // | less        |        equal      |     greater |

        final int l = begin;
        final int r = end - 1;

        int i = l;
        int j = r;
        int p = l;
        int q = r;

        final double v = data[pivot];

        // Special case: count signed zeros
        int c = 0;
        if (v == 0) {
            c = countSignedZeros(data, begin, end);
        }

        for (;;) {
            while (i <= j && data[i] <= v) {
                if (data[i] == v) {
                    //swap(data, i, p++)
                    data[i] = data[p];
                    data[p++] = v;
                }
                i++;
            }
            while (j >= i && data[j] >= v) {
                if (v == data[j]) {
                    //swap(data, j, q--)
                    data[j] = data[q];
                    data[q--] = v;
                }
                j--;
            }
            if (i > j) {
                break;
            }
            swap(data, i++, j--);
        }

        // Move equal regions to the centre.
        int s = Math.min(p - l, i - p);
        for (int k = l; s > 0; k++, s--) {
            //swap(data, k, i - s)
            data[k] = data[i - s];
            data[i - s] = v;
        }
        s = Math.min(q - j, r - q);
        for (int k = i; s > 0; k++, s--) {
            //swap(data, end - s, k)
            data[end - s] = data[k];
            data[k] = v;
        }

        // Set output range
        i = i - p + l;
        j = j - q + end;
        upper[0] = j;

        // Special case: fixed signed zeros
        if (c != 0) {
            p = i;
            while (c-- > 0) {
                data[p++] = -0.0;
            }
            while (p < j) {
                data[p++] = 0.0;
            }
        }

        return i;
    }

    /**
     * Partition around the K<sup>th</sup> value in the array.
     *
     * <p>This method can be used for repeat calls to identify K<sup>th</sup> values in
     * the same array by caching locations of pivots (correctly sorted indices).
     *
     * <p>Note: Requires that the range contains no NaN values.
     *
     * <p>Uses a Bentley-McIlroy quicksort partition method.
     *
     * <p>Returns the last known pivot location adjacent to K.
     * If {@code p <= k} the range [p, min{k+2, data.length}) is sorted.
     * If {@code p > k} then p is a pivot.
     *
     * @param data Data array to use to find out the K<sup>th</sup> value.
     * @param begin Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     * @param k Index whose value in the array is of interest.
     * @return the bound index
     */
    private int partitionBM(double[] data, int begin, int end, int k) {
        // Find the unsorted range containing k
        final int[] upper = {0};
        while (end - begin > minSelectSize) {
            // Select a value and partition data array around it
            final int from = partitionBM(data, begin, end,
                pivotingStrategy.pivotIndex(data, begin, end - 1, k), upper);
            final int to = upper[0];
            if (k >= to) {
                // The element is in the right partition
                begin = to;
            } else if (k < from) {
                // The element is in the left partition
                end = from;
            } else {
                // The range contains the element we wanted
                return end;
            }
        }
        insertionSort(data, begin, end, begin != 0);
        fixSignedZeros(data, begin, end);
        // Either end == data.length and k+1 is sorted; or
        // end == pivot and k+1 is sorted
        return begin;
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array. For all indices {@code k}
     * and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>Uses a dual-pivot quicksort method by Vladimir Yaroslavskiy.
     *
     * @param data Values.
     * @param k Indices.
     */
    void partitionDP(double[] data, int... k) {
        final int n = k.length;
        if (n < 1) {
            return;
        }

        // Handle NaN
        final int length = sortNaN(data);
        if (length < 1) {
            return;
        }

        if (n == 1) {
            partitionDP(data, 0, length, (BitSet) null, k[0]);
            return;
        }
        // Special case for partition around adjacent indices (for interpolation)
        if (n == 2 && k[0] + 1 == k[1]) {
            final int p = partitionDP(data, 0, length, (BitSet) null, k[0]);
            if (p > k[1]) {
                partitionMin(data, k[1], p);
            }
            return;
        }

        // Multiple pivots
        final BitSet pivots = new BitSet(length);

        for (int i = 0; i < n; i++) {
            final int kk = k[i];
            if (kk >= length || pivots.get(kk)) {
                // Already sorted
                continue;
            }
            int begin;
            int end;
            if (i == 0) {
                begin = 0;
                end = length;
            } else {
                // Start inclusive
                begin = pivots.previousSetBit(kk) + 1;
                end = pivots.nextSetBit(kk + 1);
                if (end < 0) {
                    end = length;
                }
            }
            partitionDP(data, begin, end, pivots, kk);
        }
    }

    /**
     * Partition around the K<sup>th</sup> value in the array.
     *
     * <p>This method can be used for repeat calls to identify K<sup>th</sup> values in
     * the same array by caching locations of pivots (correctly sorted indices).
     *
     * <p>Note: Requires that the range contains no NaN values.
     *
     * <p>Uses a dual-pivot quicksort method by Vladimir Yaroslavskiy.
     *
     * <p>Returns the pivot location adjacent to K to signal if K+1 is sorted.
     * If {@code p <= k} the range [p, min{k+2, data.length}) is sorted.
     * If {@code p > k} then p is a pivot.
     *
     * @param data Data array to use to find out the K<sup>th</sup> value.
     * @param begin Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     * @param pivots Cache of pivot points.
     * @param k Index whose value in the array is of interest.
     * @return the bound index
     */
    private int partitionDP(double[] data, int begin, int end, BitSet pivots, int k) {
        // Find the unsorted range containing k
        final int[] bounds = new int[4];
        int div = 3;
        while (end - begin > minSelectSize) {
            div = partitionDP(data, begin, end, bounds, div);
            final int k0 = bounds[0];
            final int k1 = bounds[1];
            final int k2 = bounds[2];
            final int k3 = bounds[3];
            // sorted in [k0, k1) and (k2, k3]
            if (pivots != null) {
                setPivots(k0, k1, pivots);
                setPivots(k2 + 1, k3 + 1, pivots);
            }
            if (k > k3) {
                // The element is in the right partition
                begin = k3 + 1;
            } else if (k < k0) {
                // The element is in the left partition
                end = k0;
            } else if (k >= k1 && k <= k2) {
                // Internal unsorted region
                begin = k1;
                end = k2 + 1;
            } else {
                // The sorted ranges contain the element we wanted.
                // Return a pivot (k0; k2+1) to signal if k+1 is sorted.
                if (k + 1 < k1) {
                    return k0;
                }
                if (k + 1 < k3) {
                    return k2 + 1;
                }
                return end;
            }
        }
        if (pivots != null) {
            setPivots(begin, end, pivots);
        }
        insertionSort(data, begin, end, begin != 0);
        fixSignedZeros(data, begin, end);
        // Either end == data.length and k+1 is sorted; or
        // end == pivot and k+1 is sorted
        return begin;
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Note: Requires that the range contains no NaN values. If the range contains
     * signed zeros and one is chosen as a pivot point the sort order of zeros is correct.
     *
     * <p>This method returns 4 points: the lower and upper pivots and bounds for
     * the internal range of unsorted values.
     * <ul>
     * <li>k0: lower pivot point: {@code a[k] < a[k0]} for {@code k < k0}.
     * <li>k1: the start (inclusive) of the unsorted range: {@code k0 < k1}.
     * <li>k2: the end (inclusive) of the unsorted range: {@code k2 <= k3}.
     * <li>k3: upper pivot point: {@code a[k3] < a[k]} for {@code k3 < k}.
     * </ul>
     *
     * <p>Bounds are set so {@code [k0, k1)} and {@code (k2, k3]} are fully sorted.
     * When the range {@code [k0, k3]} contains fully sorted elements
     * the result is set to {@code k1 = k3+1} and {@code k2 = k3}.
     *
     * <p>Uses a dual-pivot quicksort method by Vladimir Yaroslavskiy.
     *
     * @param a Data array.
     * @param left Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     * @param bounds Points [k0, k1, k2, k3].
     * @param div Divisor for the range to pick medians.
     * @return div
     */
    private int partitionDP(double[] a, int left, int end, int[] bounds, int div) {
        // Dual-pivot quicksort method by Vladimir Yaroslavskiy.
        //
        // Partition data using pivots P1 and P2 into less-than, greater-than or between.
        // Pivot values P1 & P2 are placed at the end. If P1 < P2, P2 acts as a sentinal.
        // k traverses the unknown region ??? and values moved if less (l) or greater (g):
        //
        // left        l                k           g         right
        // |P1|  <P1   |   P1<= & <= P2 |    ???    |    >P2   |P2|
        //
        // At the end pivots are swapped back to behind the l and g pointers.
        //
        // |  <P1        |P1|     P1<= & <= P2    |P2|      >P2   |
        //
        // Adapted from Yaroslavskiy
        // http://codeblab.com/wp-content/uploads/2009/09/DualPivotQuicksort.pdf
        //
        // Modified to allow partial sorting (partitioning):
        // - Ignore insertion sort for tiny array (handled by calling code)
        // - Ignore recursive calls for a full sort (handled by calling code)
        // - Check for equal elements if a pivot is a signed zero
        // - Fix signed zeros within the region between pivots
        // - Change to fast-forward over initial ascending / descending runs
        // - Change to a single-pivot partition method if the pivots are equal

        final int right = end - 1;
        final int len = right - left;

        // Find pivots:

        // Original method: Guess medians using 1/3 and 2/3 of range
        final int third = len / div;
        int m1 = left + third;
        int m2 = right - third;
        if (m1 <= left) {
            m1 = left + 1;
        }
        if (m2 >= right) {
            m2 = right - 1;
        }
        if (a[m1] < a[m2]) {
            swap(a, m1, left);
            swap(a, m2, right);
        } else {
            swap(a, m1, right);
            swap(a, m2, left);
        }
        // pivots
        final double pivot1 = a[left];
        final double pivot2 = a[right];

        // Single pivot sort
        if (pivot1 == pivot2) {
            final int lower = partitionSBM(a, left, end, m1, bounds);
            final int upper = bounds[0];
            // Set dual pivot range
            bounds[0] = lower;
            bounds[3] = upper - 1;
            // Fully sorted internally
            bounds[1] = upper;
            bounds[2] = upper - 1;
            return div;
        }

        // Special case: Handle signed zeros
        int c = 0;
        if (pivot1 == 0 || pivot2 == 0) {
            c = countSignedZeros(a, left, end);
        }

        // pointers
        int less = left + 1;
        int great = right - 1;

        // Fast-forward ascending / descending runs to reduce swaps
        while (a[less] < pivot1) {
            less++;
        }
        while (a[great] > pivot2) {
            great--;
        }

        // sorting
        SORTING:
        for (int k = less; k <= great; k++) {
            final double v = a[k];
            if (v < pivot1) {
                //swap(a, k, less++)
                a[k] = a[less];
                a[less] = v;
                less++;
            } else if (v > pivot2) {
                // Original
                //while (k < great && a[great] > pivot2) {
                //    great--;
                //}
                while (a[great] > pivot2) {
                    if (great-- == k) {
                        // Done
                        break SORTING;
                    }
                }
                // swap(a, k, great--)
                // if (a[k] < pivot1)
                //    swap(a, k, less++)
                final double w = a[great];
                a[great] = v;
                great--;
                // a[k] = w
                if (w < pivot1) {
                    a[k] = a[less];
                    a[less] = w;
                    less++;
                } else {
                    a[k] = w;
                }
            }
        }
        // swaps
        final int dist = great - less;
        // Original paper: If middle partition (dist) is less than 13
        // then increase 'div' by 1. This means that the two outer partitions
        // contained most of the data and choosing medians should take
        // values closer to the edge. The middle will be sorted by quicksort.
        // 13 = 27 / 2 where 27 is the threshold for quicksort.
        if (dist < (minSelectSize >>> 1)) {
            // TODO: Determine if this is needed? The original paper
            // does not comment its purpose.
            div++;
        }
        //swap(a, less - 1, left)
        //swap(a, great + 1, right)
        a[left] = a[less - 1];
        a[less - 1] = pivot1;
        a[right] = a[great + 1];
        a[great + 1] = pivot2;

        // unsorted in [less, great]

        // Set the pivots
        bounds[0] = less - 1;
        bounds[3] = great + 1;
        //partitionDP(a, left, less - 2, div)
        //partitionDP(a, great + 2, right, div)

        // equal elements
        // Original paper: If middle partition (dist) is bigger
        // than (length - 13) then check for equal elements, i.e.
        // if the middle was very large there may be many repeated elements.
        // 13 = 27 / 2 where 27 is the threshold for quicksort.
        // We always do this if the pivots are signed zeros.
        if ((dist > len - (minSelectSize >>> 1) || c != 0) && pivot1 != pivot2) {
            // Fast-forward to reduce swaps
            while (a[less] == pivot1) {
                less++;
            }
            while (a[great] == pivot2) {
                great--;
            }
            // This copies the logic in the sorting loop using == comparisons
            EQUAL:
            for (int k = less; k <= great; k++) {
                final double v = a[k];
                if (v == pivot1) {
                    //swap(a, k, less++)
                    a[k] = a[less];
                    a[less] = v;
                    less++;
                } else if (v == pivot2) {
                    while (a[great] == pivot2) {
                        if (great-- == k) {
                            // Done
                            break EQUAL;
                        }
                    }
                    final double w = a[great];
                    a[great] = v;
                    great--;
                    if (w == pivot1) {
                        a[k] = a[less];
                        a[less] = w;
                        less++;
                    } else {
                        a[k] = w;
                    }
                }
            }
        }
        // unsorted in [less, great]
        if (pivot1 < pivot2 && less < great) {
            //partitionDP(a, less, great, div)
            bounds[1] = less;
            bounds[2] = great;
        } else {
            // Fully sorted
            bounds[1] = bounds[3] + 1;
            bounds[2] = bounds[3];
        }

        // Fix signed zeros
        if (c != 0) {
            int i;
            if (pivot1 == 0) {
                i = bounds[0];
                while (c-- > 0) {
                    a[i++] = -0.0;
                }
                while (i < end && a[i] == 0) {
                    a[i++] = 0.0;
                }
            } else {
                i = bounds[3];
                while (a[i] == 0) {
                    a[i--] = 0.0;
                    if (i == left) {
                        break;
                    }
                }
                while (c-- > 0) {
                    a[++i] = -0.0;
                }
            }
        }

        return div;
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array. For all indices {@code k}
     * and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>Uses a dual-pivot quicksort method by Vladimir Yaroslavskiy.
     *
     * @param data Values.
     * @param k Indices.
     */
    void partitionDP5(double[] data, int... k) {
        final int n = k.length;
        if (n < 1) {
            return;
        }

        // Handle NaN
        final int length = sortNaN(data);
        if (length < 1) {
            return;
        }

        if (n == 1) {
            partitionDP5(data, 0, length, (BitSet) null, k[0]);
            return;
        }
        // Special case for partition around adjacent indices (for interpolation)
        if (n == 2 && k[0] + 1 == k[1]) {
            final int p = partitionDP5(data, 0, length, (BitSet) null, k[0]);
            if (p > k[1]) {
                partitionMin(data, k[1], p);
            }
            return;
        }

        // Multiple pivots
        final BitSet pivots = new BitSet(length);

        for (int i = 0; i < n; i++) {
            final int kk = k[i];
            if (kk >= length || pivots.get(kk)) {
                // Already sorted
                continue;
            }
            int begin;
            int end;
            if (i == 0) {
                begin = 0;
                end = length;
            } else {
                // Start inclusive
                begin = pivots.previousSetBit(kk) + 1;
                end = pivots.nextSetBit(kk + 1);
                if (end < 0) {
                    end = length;
                }
            }
            partitionDP5(data, begin, end, pivots, kk);
        }
    }

    /**
     * Partition around the K<sup>th</sup> value in the array.
     *
     * <p>This method can be used for repeat calls to identify K<sup>th</sup> values in
     * the same array by caching locations of pivots (correctly sorted indices).
     *
     * <p>Note: Requires that the range contains no NaN values.
     *
     * <p>Uses a dual-pivot quicksort method by Vladimir Yaroslavskiy.
     *
     * <p>Returns the pivot location adjacent to K to signal if K+1 is sorted.
     * If {@code p <= k} the range [p, min{k+2, data.length}) is sorted.
     * If {@code p > k} then p is a pivot.
     *
     * @param data Data array to use to find out the K<sup>th</sup> value.
     * @param begin Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     * @param pivots Cache of pivot points.
     * @param k Index whose value in the array is of interest.
     * @return the bound index
     */
    private int partitionDP5(double[] data, int begin, int end, BitSet pivots, int k) {
        // Find the unsorted range containing k
        final int[] bounds = new int[4];
        while (end - begin > minSelectSize) {
            partitionDP5(data, begin, end, bounds);
            final int k0 = bounds[0];
            final int k1 = bounds[1];
            final int k2 = bounds[2];
            final int k3 = bounds[3];
            // sorted in [k0, k1) and (k2, k3]
            if (pivots != null) {
                setPivots(k0, k1, pivots);
                setPivots(k2 + 1, k3 + 1, pivots);
            }
            if (k > k3) {
                // The element is in the right partition
                begin = k3 + 1;
            } else if (k < k0) {
                // The element is in the left partition
                end = k0;
            } else if (k >= k1 && k <= k2) {
                // Internal unsorted region
                begin = k1;
                end = k2 + 1;
            } else {
                // The sorted ranges contain the element we wanted.
                // Return a pivot (k0; k2+1) to signal if k+1 is sorted.
                if (k + 1 < k1) {
                    return k0;
                }
                if (k + 1 < k3) {
                    return k2 + 1;
                }
                return end;
            }
        }
        if (pivots != null) {
            setPivots(begin, end, pivots);
        }
        insertionSort(data, begin, end, begin != 0);
        fixSignedZeros(data, begin, end);
        // Either end == data.length and k+1 is sorted; or
        // end == pivot and k+1 is sorted
        return begin;
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Note: Requires that the range contains no NaN values. If the range contains
     * signed zeros and one is chosen as a pivot point the sort order of zeros is correct.
     *
     * <p>This method returns 4 points: the lower and upper pivots and bounds for
     * the internal range of unsorted values.
     * <ul>
     * <li>k0: lower pivot point: {@code a[k] < a[k0]} for {@code k < k0}.
     * <li>k1: the start (inclusive) of the unsorted range: {@code k0 < k1}.
     * <li>k2: the end (inclusive) of the unsorted range: {@code k2 <= k3}.
     * <li>k3: upper pivot point: {@code a[k3] < a[k]} for {@code k3 < k}.
     * </ul>
     *
     * <p>Bounds are set so {@code [k0, k1)} and {@code (k2, k3]} are fully sorted.
     * When the range {@code [k0, k3]} contains fully sorted elements
     * the result is set to {@code k1 = k3+1} and {@code k2 = k3}.
     *
     * <p>Uses a dual-pivot quicksort method by Vladimir Yaroslavskiy.
     *
     * @param a Data array.
     * @param left Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     * @param bounds Points [k0, k1, k2, k3].
     */
    private static void partitionDP5(double[] a, int left, int end, int[] bounds) {
        // Dual-pivot quicksort method by Vladimir Yaroslavskiy.
        //
        // Adapted from:
        //
        // http://codeblab.com/wp-content/uploads/2009/09/DualPivotQuicksort.pdf
        //
        // Modified to allow partial sorting (partitioning):
        // - Choose a pivot using 5 sorted points from the range.
        // - Ignore insertion sort for tiny array (handled by calling code)
        // - Ignore recursive calls for a full sort (handled by calling code)
        // - Check for equal elements if a pivot is a signed zero
        // - Fix signed zeros within the region between pivots
        // - Change to fast-forward over initial ascending / descending runs
        // - Change to a single-pivot partition method if the pivots are equal

        final int right = end - 1;
        final int len = right - left;

        // Find pivots:

        // Original method: Guess medians using 1/3 and 2/3 of range.
        // Here we sort 5 points and choose 2 and 4 as the pivots: 1/6, 1/3, 1/2, 2/3, 5/6
        // 1/6 ~ 1/8 + 1/32. Ensure the value is above zero to choose different points!
        // This is safe is len >= 4.
        final int sixth = 1 + (len >>> 3) + (len >>> 5);
        final int p3 = left + (len >>> 1);
        final int p2 = p3 - sixth;
        final int p1 = p2 - sixth;
        final int p4 = p3 + sixth;
        final int p5 = p4 + sixth;
        Sorting.sort5(a, p1, p2, p3, p4, p5);

        // For testing
        //p2 = DualPivotingStrategy.SORT_5.pivotIndex(a, left, end - 1, bounds);
        //p4 = bounds[0];

        final double pivot1 = a[p2];
        final double pivot2 = a[p4];

        // Add property to control this switch so we can benchmark not using it.

        if (pivot1 == pivot2) {
            // pivots == median !
            // Switch to a single pivot sort around the estimated median
            final int lower = partitionSBM(a, left, end, p3, bounds);
            final int upper = bounds[0];
            // Set dual pivot range
            bounds[0] = lower;
            bounds[3] = upper - 1;
            // No unsorted internal region
            bounds[1] = upper;
            bounds[2] = upper - 1;
            return;
        }

        // Special case: Handle signed zeros
        int c = 0;
        if (pivot1 == 0 || pivot2 == 0) {
            c = countSignedZeros(a, left, end);
        }

        // Move ends to the pivot locations.
        // After sorting the final pivot locations are overwritten.
        a[p2] = a[left];
        a[p4] = a[right];
        // It is assumed
        //a[left] = pivot1
        //a[right] = pivot2

        // pointers
        int less = left + 1;
        int great = right - 1;

        // Fast-forward ascending / descending runs to reduce swaps
        while (a[less] < pivot1) {
            less++;
        }
        while (a[great] > pivot2) {
            great--;
        }

        // sorting
        SORTING:
        for (int k = less; k <= great; k++) {
            final double v = a[k];
            if (v < pivot1) {
                //swap(a, k, less++)
                a[k] = a[less];
                a[less] = v;
                less++;
            } else if (v > pivot2) {
                // Original
                //while (k < great && a[great] > pivot2) {
                //    great--;
                //}
                while (a[great] > pivot2) {
                    if (great-- == k) {
                        // Done
                        break SORTING;
                    }
                }
                // swap(a, k, great--)
                // if (a[k] < pivot1)
                //    swap(a, k, less++)
                final double w = a[great];
                a[great] = v;
                great--;
                // a[k] = w
                if (w < pivot1) {
                    a[k] = a[less];
                    a[less] = w;
                    less++;
                } else {
                    a[k] = w;
                }
            }
        }
        // swaps
        //swap(a, less - 1, left)
        //swap(a, great + 1, right)
        a[left] = a[less - 1];
        a[less - 1] = pivot1;
        a[right] = a[great + 1];
        a[great + 1] = pivot2;

        // unsorted in [less, great]

        // Set the pivots
        bounds[0] = less - 1;
        bounds[3] = great + 1;
        //partitionDP5(a, left, less - 2)
        //partitionDP5(a, great + 2, right)

        // equal elements
        // Original paper: If middle partition (dist) is bigger
        // than (length - 13) then check for equal elements, i.e.
        // if the middle was very large there may be many repeated elements.
        // 13 = 27 / 2 where 27 is the threshold for quicksort.

        // Look for equal elements if the centre is more than 2/3 the length
        // We always do this if the pivots are signed zeros.
        if ((less < p1 && great > p5 || c != 0) && pivot1 != pivot2) {

            // Fast-forward to reduce swaps
            while (a[less] == pivot1) {
                less++;
            }
            while (a[great] == pivot2) {
                great--;
            }

            // This copies the logic in the sorting loop using == comparisons
            EQUAL:
            for (int k = less; k <= great; k++) {
                final double v = a[k];
                if (v == pivot1) {
                    //swap(a, k, less++)
                    a[k] = a[less];
                    a[less] = v;
                    less++;
                } else if (v == pivot2) {
                    while (a[great] == pivot2) {
                        if (great-- == k) {
                            // Done
                            break EQUAL;
                        }
                    }
                    final double w = a[great];
                    a[great] = v;
                    great--;
                    if (w == pivot1) {
                        a[k] = a[less];
                        a[less] = w;
                        less++;
                    } else {
                        a[k] = w;
                    }
                }
            }
        }
        // unsorted in [less, great]
        if (pivot1 < pivot2 && less < great) {
            //partitionDP5(a, less, great)
            bounds[1] = less;
            bounds[2] = great;
        } else {
            // Fully sorted
            bounds[1] = bounds[3] + 1;
            bounds[2] = bounds[3];
        }

        // Fix signed zeros
        if (c != 0) {
            int i;
            if (pivot1 == 0) {
                i = bounds[0];
                while (c-- > 0) {
                    a[i++] = -0.0;
                }
                while (i < end && a[i] == 0) {
                    a[i++] = 0.0;
                }
            } else {
                i = bounds[3];
                while (a[i] == 0) {
                    a[i--] = 0.0;
                    if (i == left) {
                        break;
                    }
                }
                while (c-- > 0) {
                    a[++i] = -0.0;
                }
            }
        }
    }

    /**
     * Swaps the two specified elements in the array.
     *
     * @param array Array.
     * @param i First index.
     * @param j Second index.
     */
    private static void swap(double[] array, int i, int j) {
        final double tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    /**
     * Sort the data.
     *
     * <p>Uses a single-pivot quicksort partition method.
     *
     * @param data Values.
     */
    void sortSP(double[] data) {
        sortSP(data, 0, data.length);
    }

    /**
     * Sort the data.
     *
     * @param data Values.
     * @param begin Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     */
    private void sortSP(double[] data, int begin, int end) {
        if (end - begin <= 1) {
            return;
        }
        final int i = partitionSP(data, begin, end,
            pivotingStrategy.pivotIndex(data, begin, end - 1, begin));
        sortSP(data, begin, i);
        sortSP(data, i + 1, end);
    }

    /**
     * Sort the data.
     *
     * <p>Uses a Bentley-McIlroy quicksort partition method.
     *
     * @param data Values.
     */
    void sortSBM(double[] data) {
        sortSBM(data, 0, sortNaN(data));
    }

    /**
     * Sort the data. Requires no NaN values in the range.
     *
     * @param data Values.
     * @param begin Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     */
    private void sortSBM(double[] data, int begin, int end) {
        if (end - begin <= minSelectSize) {
            insertionSort(data, begin, end, begin != 0);
            if (begin < end) {
                fixSignedZeros(data, begin, end);
            }
            return;
        }
        final int[] to = {0};
        final int from = partitionSBM(data, begin, end,
            pivotingStrategy.pivotIndex(data, begin, end - 1, begin), to);
        sortSBM(data, begin, from);
        sortSBM(data, to[0], end);
    }

    /**
     * Sort the data.
     *
     * <p>Uses a Bentley-McIlroy quicksort partition method.
     *
     * @param data Values.
     */
    void sortBM(double[] data) {
        sortBM(data, 0, sortNaN(data));
    }

    /**
     * Sort the data. Requires no NaN values in the range.
     *
     * @param data Values.
     * @param begin Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     */
    private void sortBM(double[] data, int begin, int end) {
        if (end - begin <= minSelectSize) {
            insertionSort(data, begin, end, begin != 0);
            if (begin < end) {
                fixSignedZeros(data, begin, end);
            }
            return;
        }
        final int[] to = {0};
        final int from = partitionBM(data, begin, end,
            pivotingStrategy.pivotIndex(data, begin, end - 1, begin), to);
        sortBM(data, begin, from);
        sortBM(data, to[0], end);
    }

    /**
     * Sort the data.
     *
     * <p>Uses a dual-pivot quicksort method by Vladimir Yaroslavskiy.
     *
     * @param data Values.
     */
    void sortDP(double[] data) {
        sortDP(data, 0, sortNaN(data), 3);
    }

    /**
     * Sort the data. Requires no NaN values in the range.
     *
     * @param data Values.
     * @param begin Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     * @param div Divisor for the range to pick medians.
     */
    private void sortDP(double[] data, int begin, int end, int div) {
        if (end - begin <= minSelectSize) {
            insertionSort(data, begin, end, begin != 0);
            if (begin < end) {
                fixSignedZeros(data, begin, end);
            }
            return;
        }
        final int[] bounds = new int[4];
        div = partitionDP(data, begin, end, bounds, div);
        final int k0 = bounds[0];
        final int k1 = bounds[1];
        final int k2 = bounds[2];
        final int k3 = bounds[3];
        sortDP(data, begin, k0, div);
        sortDP(data, k3 + 1, end, div);
        sortDP(data, k1, k2 + 1, div);
    }

    /**
     * Sort the data.
     *
     * <p>Uses a dual-pivot quicksort method by Vladimir Yaroslavskiy optimised
     * to choose the pivots using 5 sorted points.
     *
     * @param data Values.
     */
    void sortDP5(double[] data) {
        sortDP5(data, 0, sortNaN(data));
    }

    /**
     * Sort the data. Requires no NaN values in the range.
     *
     * @param data Values.
     * @param begin Lower bound (inclusive).
     * @param end Upper bound (exclusive).
     */
    private void sortDP5(double[] data, int begin, int end) {
        if (end - begin <= minSelectSize) {
            insertionSort(data, begin, end, begin != 0);
            if (begin < end) {
                fixSignedZeros(data, begin, end);
            }
            return;
        }
        final int[] bounds = new int[4];
        partitionDP5(data, begin, end, bounds);
        final int k0 = bounds[0];
        final int k1 = bounds[1];
        final int k2 = bounds[2];
        final int k3 = bounds[3];
        sortDP5(data, begin, k0);
        sortDP5(data, k3 + 1, end);
        sortDP5(data, k1, k2 + 1);
    }

    /**
     * Creates the pivots heap for a data array of the specified {@code length}.
     * If the array is too small to use the pivots heap then an empty array is returned.
     *
     * @param length Length.
     * @return the pivots heap
     */
    static int[] createPivotsHeap(int length) {
        if (length <= MIN_SELECT_SIZE) {
            return NO_PIVOTS;
        }
        // Size should be x^2 - 1, where x is the layers in the heap.
        // Do not create more pivots than the array length. When partitions are small
        // the pivots are no longer used so this does not have to contain all indices.
        // Default size in Commons Math Percentile class was 1023 (10 layers).
        final int n = nextPow2(length >>> 1);
        final int[] pivotsHeap = new int[Math.min(n, 1 << 10) - 1];
        Arrays.fill(pivotsHeap, -1);
        return pivotsHeap;
    }

    /**
     * Returns the closest power-of-two number greater than or equal to value.
     *
     * <p>Warning: This will return {@link Integer#MIN_VALUE} for any value above
     * {@code 1 << 30}. This is the next power of 2 as an unsigned integer.
     *
     * @param value the value (must be positive)
     * @return the closest power-of-two number greater than or equal to value
     */
    private static int nextPow2(int value) {
        // shift by -x is equal to shift by (32 - x) as only the low 5-bits are used.
        return 1 << -Integer.numberOfLeadingZeros(value - 1);
    }
}
