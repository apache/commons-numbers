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
 * Partition array data.
 *
 * <p>Note: Requires that the floating-point data contains no NaN values; sorting does not
 * respect the order of signed zeros imposed by {@link Double#compare(double, double)};
 * mixed signed zeros may be destroyed (the mixture updated during partitioning). The
 * caller is responsible for counting a mixture of signed zeros and restoring them if
 * required.
 *
 * @see Selection
 * @since 1.2
 */
final class QuickSelect {
    // Implementation Notes
    //
    // Selection is performed using a quickselect variant to recursively divide the range
    // to select the target index, or indices. Partition sizes or recursion are monitored
    // will fall-backs on poor convergence of a linearselect (single index) or heapselect.
    //
    // Many implementations were tested, each with strengths and weaknesses on different
    // input data containing random elements, repeat elements, elements with repeat
    // patterns, and constant elements. The final implementation performs well across data
    // types for single and multiple indices with no obvious weakness.
    // See: o.a.c.numbers.examples.jmh.arrays for benchmarking implementations.
    //
    // Single indices are selected using a quickselect adaptive method based on Alexandrescu.
    // The algorithm is a quickselect around a pivot identified using a
    // sample-of-sample-of-samples created from the entire range data. This pivot will
    // have known lower and upper margins and ensures elimination of a minimum fraction of
    // data at each step. To increase speed the pivot can be identified using less of the data
    // but without margin guarantees (sampling mode). The algorithm monitors partition sizes
    // against the known margins. If the reduction in the partition size is not large enough
    // then the algorithm can disable sampling mode and ensure linear performance by removing
    // a set fraction of the data each iteration.
    //
    // Modifications from Alexandrescu are:
    // 1. Initialise sampling mode using the Floyd-Rivest (FR) SELECT algorithm.
    // 2. Adaption is adjusted to force use of the lower margin in the far-step method when
    //    sampling is disabled.
    // 3. Change the far-step method to a min-of-4 then median-of-3 into the 2nd 12th-tile.
    //    The original method uses a lower-median-of-4, min-of-3 into the 4th 12th-tile.
    // 4. Position the sample around the target k when in sampling mode for the non-far-step
    //    methods.
    //
    // The far step method is used when target k is within 1/12 of the end of the data A.
    // The differences in the far-step method are:
    // - The upper margin when not sampling is 8/24 vs. 9/24; the lower margin remains at 1/12.
    // - The position of the sample is closer to the expected location of k < |A|/12.
    // - Sampling mode uses a median-of-3 with adaptive k, matching the other step methods.
    //   Note the original min-of-3 sample is more likely to create a pivot too small if used
    //   with adaption leaving k in the larger partition and a wasted iteration.
    //
    // The Floyd-Rivest (FR) SELECT algorithm is preferred for sampling over using quickselect
    // adaptive sampling. It uses a smaller sample and has improved heuristics to place the sample
    // pivot. However the FR sample is a small range of the data and pivot selection can be poor
    // if the sample is not representative. This can be mitigated by creating a random sample
    // of the entire range for the pivot selection. This implementation does not use random
    // sampling for the FR mode. Performance is identical on random data (randomisation is a
    // negligible overhead) and faster on sorted data. Any data not suitable for the FR algorithm
    // are immediately switched to the quickselect adaptive algorithm with sampling. Performance
    // across a range of data shows this strategy is approximately mid-way in performance between
    // FR with random sampling, and quickselect adaptive in sampling mode. The benefit is that
    // sorted or partially partitioned data are not intentionally unordered as the method will
    // only move elements known to be incorrectly placed in the array.
    //
    // Multiple indices are selected using a dual-pivot partition method by
    // Yaroslavskiy to divide the interval containing the indices. Recursion is performed for
    // regions containing target indices. The method is thus a partial quicksort into regions of
    // interest. Excess recursion triggers use of a heapselect. When indices are effectively
    // a single index the method can switch to the single index selection to use the FR algorithm.
    //
    // Alternative schemes to partition multiple indices are to repeat call single index select
    // with cached pivots, or without cached pivots if processing indices in order as the previous
    // index brackets the range for the next search. Caching pivots is the most effective
    // alternative. It requires storing all pivots during select, and using the cache to look-up
    // the search bounds (sub-range) for each target index. This requires 2n searches for n indices.
    // All pivots must be stored to avoid destroying previously partitioned data on repeat entry
    // to the array. The current scheme inverts this by requiring at most n-1 divides of the
    // indices during recursion and has the advantage of tracking recursion depth during selection
    // for each sub-range. Division of indices is a small overhead for the common case where
    // the number of indices is far smaller than the size of the data.
    //
    // Dual-pivot partitioning adapted from Yaroslavskiy
    // http://codeblab.com/wp-content/uploads/2009/09/DualPivotQuicksort.pdf
    //
    // Modified to allow partial sorting (partitioning):
    // - Ignore insertion sort for tiny array (handled by calling code).
    // - Ignore recursive calls for a full sort (handled by calling code).
    // - Change to fast-forward over initial ascending / descending runs.
    // - Change to fast-forward great when v > v2 and either break the sorting
    //   loop, or move a[great] direct to the correct location.
    // - Change to use the 2nd and 4th of 5 elements for the pivots.
    // - Identify a large central region using ~5/8 of the length to trigger search for
    //   equal values.
    //
    // For some indices and data a full sort of the data will be faster; this is impossible to
    // predict on unknown data input and attempts to analyse the indices and data impact
    // performance for the majority of use cases where sorting is not a suitable choice.
    // Use of the sortselect finisher allows the current multiple indices method to degrade
    // to a (non-optimised) dual-pivot quicksort (see below).
    //
    // heapselect vs sortselect
    //
    // Quickselect can switch to an alternative when: the range is very small
    // (e.g. insertion sort); or the target index is close to the end (e.g. heapselect).
    // Small ranges and a target index close to the end are handled using a hybrid of insertion
    // sort and selection (sortselect). This is faster than heapselect for small distance from
    // the edge (m) for a single index and has the advantage of sorting all upstream values from
    // the target index (heapselect requires push-down of each successive value to sort). This
    // allows the dual-pivot quickselect on multiple indices that saturate the range to degrade
    // to a (non-optimised) dual-pivot quicksort. However sortselect is worst case Order(m * (r-l))
    // for range [l, r] so cannot be used when quickselect fails to converge as m may be very large.
    // Thus heapselect is used as the stopper algorithm when quickselect progress is slow on
    // multiple indices. If heapselect is used for small range handling the performance on
    // saturated indices is significantly slower. Hence the presence of two final selection
    // methods for different purposes.

    /** Sampling mode using Floyd-Rivest sampling. */
    static final int MODE_FR_SAMPLING = -1;
    /** Sampling mode. */
    static final int MODE_SAMPLING = 0;
    /** No sampling but use adaption of the target k. */
    static final int MODE_ADAPTION = 1;
    /** No sampling and no adaption of target k (strict margins). */
    static final int MODE_STRICT = 2;

    /** Minimum size for sortselect.
     * Below this perform a sort rather than selection. This is used to avoid
     * sort select on tiny data. */
    private static final int MIN_SORTSELECT_SIZE = 4;
    /** Single-pivot sortselect size for quickselect adaptive. Note that quickselect adaptive
     * recursively calls quickselect so very small lengths are included with an initial medium
     * length. Using lengths of 1023-5 and 2043-53 indicate optimum performance around 20-30.
     * Note: The expand partition function assumes a sample of at least length 2 as each end
     * of the sample is used as a sentinel; this imposes a minimum length of 24 on the range
     * to ensure it contains a 12-th tile of length 2. Thus the absolute minimum for the
     * distance from the edge is 12. */
    private static final int LINEAR_SORTSELECT_SIZE = 24;
    /** Dual-pivot sortselect size for the distance of a single k from the edge of the
     * range length n. Benchmarking in range [81+81, 243+243] suggests a value of ~20 (or
     * higher on some hardware). Ranges are chosen based on third interval spacing between
     * powers of 3.
     *
     * <p>Sortselect is faster at this small size than heapselect. A second advantage is
     * that all indices closer to the edge than the target index are also sorted. This
     * allows selection of multiple close indices to be performed with effectively the
     * same speed. High density indices will result in recursion to very short fragments
     * which also trigger use of sort select. The threshold for sorting short lengths is
     * configured in {@link #dualPivotSortSelectSize(int, int)}. */
    private static final int DP_SORTSELECT_SIZE = 20;
    /** Threshold to use Floyd-Rivest sub-sampling. This partitions a sample of the data to
     * identify a pivot so that the target element is in the smaller set after partitioning.
     * The original FR paper used 600 otherwise reverted to the target index as the pivot.
     * This implementation reverts to quickselect adaptive which increases robustness
     * at small size on a variety of data and allows raising the original FR threshold. */
    private static final int FR_SAMPLING_SIZE = 1200;

    /** Increment used for the recursion counter. The counter will overflow to negative when
     * recursion has exceeded the maximum level. The counter is maintained in the upper bits
     * of the dual-pivot control flags. */
    private static final int RECURSION_INCREMENT = 1 << 20;
    /** Mask to extract the sort select size from the dual-pivot control flags. Currently
     * the bits below those used for the recursion counter are only used for the sort select size
     * so this can use a mask with all bits below the increment. */
    private static final int SORTSELECT_MASK = RECURSION_INCREMENT - 1;

    /** Threshold to use repeated step left: 7 / 16. */
    private static final double STEP_LEFT = 0.4375;
    /** Threshold to use repeated step right: 9 / 16. */
    private static final double STEP_RIGHT = 0.5625;
    /** Threshold to use repeated step far-left: 1 / 12. */
    private static final double STEP_FAR_LEFT = 0.08333333333333333;
    /** Threshold to use repeated step far-right: 11 / 12. */
    private static final double STEP_FAR_RIGHT = 0.9166666666666666;

    /** No instances. */
    private QuickSelect() {}

    /**
     * Partition the elements between {@code ka} and {@code kb} using a heap select
     * algorithm. It is assumed {@code left <= ka <= kb <= right}.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param ka Lower index to select.
     * @param kb Upper index to select.
     */
    static void heapSelect(double[] a, int left, int right, int ka, int kb) {
        if (right <= left) {
            return;
        }
        // Use the smallest heap
        if (kb - left < right - ka) {
            heapSelectLeft(a, left, right, ka, kb);
        } else {
            heapSelectRight(a, left, right, ka, kb);
        }
    }

    /**
     * Partition the elements between {@code ka} and {@code kb} using a heap select
     * algorithm. It is assumed {@code left <= ka <= kb <= right}.
     *
     * <p>For best performance this should be called with {@code k} in the lower
     * half of the range.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param ka Lower index to select.
     * @param kb Upper index to select.
     */
    static void heapSelectLeft(double[] a, int left, int right, int ka, int kb) {
        // Create a max heap in-place in [left, k], rooted at a[left] = max
        // |l|-max-heap-|k|--------------|
        // Build the heap using Floyd's heap-construction algorithm for heap size n.
        // Start at parent of the last element in the heap (k),
        // i.e. start = parent(n-1) : parent(c) = floor((c - 1) / 2) : c = k - left
        int end = kb + 1;
        for (int p = left + ((kb - left - 1) >> 1); p >= left; p--) {
            maxHeapSiftDown(a, a[p], p, left, end);
        }
        // Scan the remaining data and insert
        // Mitigate worst case performance on descending data by backward sweep
        double max = a[left];
        for (int i = right; i > kb; i--) {
            final double v = a[i];
            if (v < max) {
                a[i] = max;
                maxHeapSiftDown(a, v, left, left, end);
                max = a[left];
            }
        }
        // Partition [ka, kb]
        // |l|-max-heap-|k|--------------|
        //  |  <-swap->  |   then sift down reduced size heap
        // Avoid sifting heap of size 1
        final int last = Math.max(left, ka - 1);
        while (--end > last) {
            maxHeapSiftDown(a, a[end], left, left, end);
            a[end] = max;
            max = a[left];
        }
    }

    /**
     * Sift the element down the max heap.
     *
     * <p>Assumes {@code root <= p < end}, i.e. the max heap is above root.
     *
     * @param a Heap data.
     * @param v Value to sift.
     * @param p Start position.
     * @param root Root of the heap.
     * @param end End of the heap (exclusive).
     */
    private static void maxHeapSiftDown(double[] a, double v, int p, int root, int end) {
        // child2 = root + 2 * (parent - root) + 2
        //        = 2 * parent - root + 2
        while (true) {
            // Right child
            int c = (p << 1) - root + 2;
            if (c > end) {
                // No left child
                break;
            }
            // Use the left child if right doesn't exist, or it is greater
            if (c == end || a[c] < a[c - 1]) {
                --c;
            }
            if (v >= a[c]) {
                // Parent greater than largest child - done
                break;
            }
            // Swap and descend
            a[p] = a[c];
            p = c;
        }
        a[p] = v;
    }

    /**
     * Partition the elements between {@code ka} and {@code kb} using a heap select
     * algorithm. It is assumed {@code left <= ka <= kb <= right}.
     *
     * <p>For best performance this should be called with {@code k} in the upper
     * half of the range.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param ka Lower index to select.
     * @param kb Upper index to select.
     */
    static void heapSelectRight(double[] a, int left, int right, int ka, int kb) {
        // Create a min heap in-place in [k, right], rooted at a[right] = min
        // |--------------|k|-min-heap-|r|
        // Build the heap using Floyd's heap-construction algorithm for heap size n.
        // Start at parent of the last element in the heap (k),
        // i.e. start = parent(n-1) : parent(c) = floor((c - 1) / 2) : c = right - k
        int end = ka - 1;
        for (int p = right - ((right - ka - 1) >> 1); p <= right; p++) {
            minHeapSiftDown(a, a[p], p, right, end);
        }
        // Scan the remaining data and insert
        // Mitigate worst case performance on descending data by backward sweep
        double min = a[right];
        for (int i = left; i < ka; i++) {
            final double v = a[i];
            if (v > min) {
                a[i] = min;
                minHeapSiftDown(a, v, right, right, end);
                min = a[right];
            }
        }
        // Partition [ka, kb]
        // |--------------|k|-min-heap-|r|
        //                 |  <-swap->  |   then sift down reduced size heap
        // Avoid sifting heap of size 1
        final int last = Math.min(right, kb + 1);
        while (++end < last) {
            minHeapSiftDown(a, a[end], right, right, end);
            a[end] = min;
            min = a[right];
        }
    }

    /**
     * Sift the element down the min heap.
     *
     * <p>Assumes {@code root >= p > end}, i.e. the max heap is below root.
     *
     * @param a Heap data.
     * @param v Value to sift.
     * @param p Start position.
     * @param root Root of the heap.
     * @param end End of the heap (exclusive).
     */
    private static void minHeapSiftDown(double[] a, double v, int p, int root, int end) {
        // child2 = root - 2 * (root - parent) - 2
        //        = 2 * parent - root - 2
        while (true) {
            // Right child
            int c = (p << 1) - root - 2;
            if (c < end) {
                // No left child
                break;
            }
            // Use the left child if right doesn't exist, or it is less
            if (c == end || a[c] > a[c + 1]) {
                ++c;
            }
            if (v <= a[c]) {
                // Parent less than smallest child - done
                break;
            }
            // Swap and descend
            a[p] = a[c];
            p = c;
        }
        a[p] = v;
    }

    /**
     * Partition the elements between {@code ka} and {@code kb} using a sort select
     * algorithm. It is assumed {@code left <= ka <= kb <= right}.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param ka Lower index to select.
     * @param kb Upper index to select.
     */
    static void sortSelect(double[] a, int left, int right, int ka, int kb) {
        // Combine the test for right <= left with
        // avoiding the overhead of sort select on tiny data.
        if (right - left <= MIN_SORTSELECT_SIZE) {
            Sorting.sort(a, left, right);
            return;
        }
        // Sort the smallest side
        if (kb - left < right - ka) {
            sortSelectLeft(a, left, right, kb);
        } else {
            sortSelectRight(a, left, right, ka);
        }
    }

    /**
     * Partition the minimum {@code n} elements below {@code k} where
     * {@code n = k - left + 1}. Uses an insertion sort algorithm.
     *
     * <p>Works with any {@code k} in the range {@code left <= k <= right}
     * and performs a full sort of the range below {@code k}.
     *
     * <p>For best performance this should be called with
     * {@code k - left < right - k}, i.e.
     * to partition a value in the lower half of the range.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param k Index to select.
     */
    static void sortSelectLeft(double[] a, int left, int right, int k) {
        // Sort
        for (int i = left; ++i <= k;) {
            final double v = a[i];
            // Move preceding higher elements above (if required)
            if (v < a[i - 1]) {
                int j = i;
                while (--j >= left && v < a[j]) {
                    a[j + 1] = a[j];
                }
                a[j + 1] = v;
            }
        }
        // Scan the remaining data and insert
        // Mitigate worst case performance on descending data by backward sweep
        double m = a[k];
        for (int i = right; i > k; i--) {
            final double v = a[i];
            if (v < m) {
                a[i] = m;
                int j = k;
                while (--j >= left && v < a[j]) {
                    a[j + 1] = a[j];
                }
                a[j + 1] = v;
                m = a[k];
            }
        }
    }

    /**
     * Partition the maximum {@code n} elements above {@code k} where
     * {@code n = right - k + 1}. Uses an insertion sort algorithm.
     *
     * <p>Works with any {@code k} in the range {@code left <= k <= right}
     * and can be used to perform a full sort of the range above {@code k}.
     *
     * <p>For best performance this should be called with
     * {@code k - left > right - k}, i.e.
     * to partition a value in the upper half of the range.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param k Index to select.
     */
    static void sortSelectRight(double[] a, int left, int right, int k) {
        // Sort
        for (int i = right; --i >= k;) {
            final double v = a[i];
            // Move succeeding lower elements below (if required)
            if (v > a[i + 1]) {
                int j = i;
                while (++j <= right && v > a[j]) {
                    a[j - 1] = a[j];
                }
                a[j - 1] = v;
            }
        }
        // Scan the remaining data and insert
        // Mitigate worst case performance on descending data by backward sweep
        double m = a[k];
        for (int i = left; i < k; i++) {
            final double v = a[i];
            if (v > m) {
                a[i] = m;
                int j = k;
                while (++j <= right && v > a[j]) {
                    a[j - 1] = a[j];
                }
                a[j - 1] = v;
                m = a[k];
            }
        }
    }

    /**
     * Partition the array such that index {@code k} corresponds to its correctly
     * sorted value in the equivalent fully sorted array.
     *
     * <p>Assumes {@code k} is a valid index into [left, right].
     *
     * @param a Values.
     * @param left Lower bound of data (inclusive).
     * @param right Upper bound of data (inclusive).
     * @param k Index.
     */
    static void select(double[] a, int left, int right, int k) {
        quickSelectAdaptive(a, left, right, k, k, new int[1], MODE_FR_SAMPLING);
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array.
     *
     * <p>The count of the number of used indices is returned. If the keys are sorted in-place,
     * the count is returned as a negative.
     *
     * @param a Values.
     * @param left Lower bound of data (inclusive).
     * @param right Upper bound of data (inclusive).
     * @param k Indices (may be destructively modified).
     * @param n Count of indices.
     * @return the count of used indices
     */
    static int select(double[] a, int left, int right, int[] k, int n) {
        if (n < 1) {
            return 0;
        }
        if (n == 1) {
            quickSelectAdaptive(a, left, right, k[0], k[0], new int[1], MODE_FR_SAMPLING);
            return -1;
        }

        // Interval creation validates the indices are in [left, right]
        final UpdatingInterval keys = IndexSupport.createUpdatingInterval(k, n);

        // Save number of used indices
        final int count = IndexSupport.countIndices(keys, n);

        // Note: If the keys are not separated then they are effectively a single key.
        // Any split of keys separated by the sort select size
        // will be finished on the next iteration.
        final int k1 = keys.left();
        final int kn = keys.right();
        if (kn - k1 < DP_SORTSELECT_SIZE) {
            quickSelectAdaptive(a, left, right, k1, kn, new int[1], MODE_FR_SAMPLING);
        } else {
            // Dual-pivot mode with small range sort length configured using index density
            dualPivotQuickSelect(a, left, right, keys, dualPivotFlags(left, right, k1, kn));
        }
        return count;
    }

    /**
     * Partition the array such that indices {@code k} correspond to their
     * correctly sorted value in the equivalent fully sorted array.
     *
     * <p>For all indices {@code [ka, kb]} and any index {@code i}:
     *
     * <pre>{@code
     * data[i < ka] <= data[ka] <= data[kb] <= data[kb < i]
     * }</pre>
     *
     * <p>This function accepts indices {@code [ka, kb]} that define the
     * range of indices to partition. It is expected that the range is small.
     *
     * <p>The {@code flags} are used to control the sampling mode and adaption of
     * the index within the sample.
     *
     * <p>Returns the bounds containing {@code [ka, kb]}. These may be lower/higher
     * than the keys if equal values are present in the data.
     *
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param ka First key of interest.
     * @param kb Last key of interest.
     * @param bounds Upper bound of the range containing {@code [ka, kb]} (inclusive).
     * @param flags Adaption flags.
     * @return Lower bound of the range containing {@code [ka, kb]} (inclusive).
     */
    static int quickSelectAdaptive(double[] a, int left, int right, int ka, int kb,
            int[] bounds, int flags) {
        int l = left;
        int r = right;
        int m = flags;
        while (true) {
            // Select when ka and kb are close to the same end
            // |l|-----|ka|kkkkkkkk|kb|------|r|
            if (Math.min(kb - l, r - ka) < LINEAR_SORTSELECT_SIZE) {
                sortSelect(a, l, r, ka, kb);
                bounds[0] = kb;
                return ka;
            }

            // Only target ka; kb is assumed to be close
            int p0;
            final int n = r - l;
            // f in [0, 1]
            final double f = (double) (ka - l) / n;
            // Record the larger margin (start at 1/4) to create the estimated size.
            // step        L     R
            // far left    1/12  1/3   (use 1/4 + 1/32 + 1/64 ~ 0.328)
            // left        1/6   1/4
            // middle      2/9   2/9   (use 1/4 - 1/32 ~ 0.219)
            int margin = n >> 2;
            if (m < MODE_SAMPLING && r - l > FR_SAMPLING_SIZE) {
                // Floyd-Rivest sample step uses the same margins
                p0 = sampleStep(a, l, r, ka, bounds);
                if (f <= STEP_FAR_LEFT || f >= STEP_FAR_RIGHT) {
                    margin += (n >> 5) + (n >> 6);
                } else if (f > STEP_LEFT && f < STEP_RIGHT) {
                    margin -= n >> 5;
                }
            } else if (f <= STEP_LEFT) {
                if (f <= STEP_FAR_LEFT) {
                    margin += (n >> 5) + (n >> 6);
                    p0 = repeatedStepFarLeft(a, l, r, ka, bounds, m);
                } else {
                    p0 = repeatedStepLeft(a, l, r, ka, bounds, m);
                }
            } else if (f >= STEP_RIGHT) {
                if (f >= STEP_FAR_RIGHT) {
                    margin += (n >> 5) + (n >> 6);
                    p0 = repeatedStepFarRight(a, l, r, ka, bounds, m);
                } else {
                    p0 = repeatedStepRight(a, l, r, ka, bounds, m);
                }
            } else {
                margin -= n >> 5;
                p0 = repeatedStep(a, l, r, ka, bounds, m);
            }

            // Note: Here we expect [ka, kb] to be small and splitting is unlikely.
            //                   p0 p1
            // |l|--|ka|kkkk|kb|--|P|-------------------|r|
            // |l|----------------|P|--|ka|kkk|kb|------|r|
            // |l|-----------|ka|k|P|k|kb|--------------|r|
            final int p1 = bounds[0];
            if (kb < p0) {
                // Entirely on left side
                r = p0 - 1;
            } else if (ka > p1) {
                // Entirely on right side
                l = p1 + 1;
            } else {
                // Pivot splits [ka, kb]. Expect ends to be close to the pivot and finish.
                // Here we set the bounds for use after median-of-medians pivot selection.
                // In the event there are many equal values this allows collecting those
                // known to be equal together when moving around the medians sample.
                if (kb > p1) {
                    sortSelectLeft(a, p1 + 1, r, kb);
                    bounds[0] = kb;
                }
                if (ka < p0) {
                    sortSelectRight(a, l, p0 - 1, ka);
                    p0 = ka;
                }
                return p0;
            }
            // Update mode based on target partition size
            if (r - l > n - margin) {
                m++;
            }
        }
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Partitions a Floyd-Rivest sample around a pivot offset so that the input {@code k} will
     * fall in the smaller partition when the entire range is partitioned.
     *
     * <p>Assumes the range {@code r - l} is large.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private static int sampleStep(double[] a, int l, int r, int k, int[] upper) {
        // Floyd-Rivest: use SELECT recursively on a sample of size S to get an estimate
        // for the (k-l+1)-th smallest element into a[k], biased slightly so that the
        // (k-l+1)-th element is expected to lie in the smaller set after partitioning.
        final int n = r - l + 1;
        final int ith = k - l + 1;
        final double z = Math.log(n);
        // sample size = 0.5 * n^(2/3)
        final double s = 0.5 * Math.exp(0.6666666666666666 * z);
        final double sd = 0.5 * Math.sqrt(z * s * (n - s) / n) * Integer.signum(ith - (n >> 1));
        final int ll = Math.max(l, (int) (k - ith * s / n + sd));
        final int rr = Math.min(r, (int) (k + (n - ith) * s / n + sd));
        // Sample recursion restarts from [ll, rr]
        final int p = quickSelectAdaptive(a, ll, rr, k, k, upper, MODE_FR_SAMPLING);
        return expandPartition(a, l, r, ll, rr, p, upper[0], upper);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Assumes the range {@code r - l >= 8}; the caller is responsible for selection on a smaller
     * range. If using a 12th-tile for sampling then assumes {@code r - l >= 11}.
     *
     * <p>Uses the Chen and Dumitrescu repeated step median-of-medians-of-medians algorithm
     * with the median of 3 then median of 3; the final sample is placed in the
     * 5th 9th-tile; the pivot chosen from the sample is adaptive using the input {@code k}.
     *
     * <p>Given a pivot in the middle of the sample this has margins of 2/9 and 2/9.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @param flags Control flags.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private static int repeatedStep(double[] a, int l, int r, int k, int[] upper, int flags) {
        // Adapted from Alexandrescu (2016), algorithm 8.
        final int fp;
        final int s;
        int p;
        if (flags <= MODE_SAMPLING) {
            // Median into a 12th-tile
            fp = (r - l + 1) / 12;
            // Position the sample around the target k
            s = k - mapDistance(k - l, l, r, fp);
            p = k;
        } else {
            // i in tertile [3f':6f')
            fp = (r - l + 1) / 9;
            final int f3 = 3 * fp;
            final int end = l + (f3 << 1);
            for (int i = l + f3; i < end; i++) {
                Sorting.sort3(a, i - f3, i, i + f3);
            }
            // 5th 9th-tile: [4f':5f')
            s = l + (fp << 2);
            // No adaption uses the middle to enforce strict margins
            p = s + (flags == MODE_ADAPTION ? mapDistance(k - l, l, r, fp) : (fp >>> 1));
        }
        final int e = s + fp - 1;
        for (int i = s; i <= e; i++) {
            Sorting.sort3(a, i - fp, i, i + fp);
        }
        p = quickSelectAdaptive(a, s, e, p, p, upper, MODE_FR_SAMPLING);
        return expandPartition(a, l, r, s, e, p, upper[0], upper);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Assumes the range {@code r - l >= 11}; the caller is responsible for selection on a smaller
     * range.
     *
     * <p>Uses the Chen and Dumitrescu repeated step median-of-medians-of-medians algorithm
     * with the lower median of 4 then either median of 3 with the final sample placed in the
     * 5th 12th-tile, or min of 3 with the final sample in the 4th 12th-tile;
     * the pivot chosen from the sample is adaptive using the input {@code k}.
     *
     * <p>Given a pivot in the middle of the sample this has margins of 1/6 and 1/4.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @param flags Control flags.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private static int repeatedStepLeft(double[] a, int l, int r, int k, int[] upper, int flags) {
        // Adapted from Alexandrescu (2016), algorithm 9.
        final int fp;
        final int s;
        int p;
        if (flags <= MODE_SAMPLING) {
            // Median into a 12th-tile
            fp = (r - l + 1) / 12;
            // Position the sample around the target k
            // Avoid bounds error due to rounding as (k-l)/(r-l) -> 1/12
            s = Math.max(k - mapDistance(k - l, l, r, fp), l + fp);
            p = k;
        } else {
            // i in 2nd quartile
            final int f = (r - l + 1) >> 2;
            final int f2 = f + f;
            final int end = l + f2;
            for (int i = l + f; i < end; i++) {
                Sorting.lowerMedian4(a, i - f, i, i + f, i + f2);
            }
            // i in 5th 12th-tile
            fp = f / 3;
            s = l + f + fp;
            // No adaption uses the middle to enforce strict margins
            p = s + (flags == MODE_ADAPTION ? mapDistance(k - l, l, r, fp) : (fp >>> 1));
        }
        final int e = s + fp - 1;
        for (int i = s; i <= e; i++) {
            Sorting.sort3(a, i - fp, i, i + fp);
        }
        p = quickSelectAdaptive(a, s, e, p, p, upper, MODE_FR_SAMPLING);
        return expandPartition(a, l, r, s, e, p, upper[0], upper);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Assumes the range {@code r - l >= 11}; the caller is responsible for selection on a smaller
     * range.
     *
     * <p>Uses the Chen and Dumitrescu repeated step median-of-medians-of-medians algorithm
     * with the upper median of 4 then either median of 3 with the final sample placed in the
     * 8th 12th-tile, or max of 3 with the final sample in the 9th 12th-tile;
     * the pivot chosen from the sample is adaptive using the input {@code k}.
     *
     * <p>Given a pivot in the middle of the sample this has margins of 1/4 and 1/6.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @param flags Control flags.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private static int repeatedStepRight(double[] a, int l, int r, int k, int[] upper, int flags) {
        // Mirror image repeatedStepLeft using upper median into 3rd quartile
        final int fp;
        final int e;
        int p;
        if (flags <= MODE_SAMPLING) {
            // Median into a 12th-tile
            fp = (r - l + 1) / 12;
            // Position the sample around the target k
            // Avoid bounds error due to rounding as (r-k)/(r-l) -> 11/12
            e = Math.min(k + mapDistance(r - k, l, r, fp), r - fp);
            p = k;
        } else {
            // i in 3rd quartile
            final int f = (r - l + 1) >> 2;
            final int f2 = f + f;
            final int end = r - f2;
            for (int i = r - f; i > end; i--) {
                Sorting.upperMedian4(a, i - f2, i - f, i, i + f);
            }
            // i in 8th 12th-tile
            fp = f / 3;
            e = r - f - fp;
            // No adaption uses the middle to enforce strict margins
            p = e - (flags == MODE_ADAPTION ? mapDistance(r - k, l, r, fp) : (fp >>> 1));
        }
        final int s = e - fp + 1;
        for (int i = s; i <= e; i++) {
            Sorting.sort3(a, i - fp, i, i + fp);
        }
        p = quickSelectAdaptive(a, s, e, p, p, upper, MODE_FR_SAMPLING);
        return expandPartition(a, l, r, s, e, p, upper[0], upper);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Assumes the range {@code r - l >= 11}; the caller is responsible for selection on a smaller
     * range.
     *
     * <p>Uses the Chen and Dumitrescu repeated step median-of-medians-of-medians algorithm
     * with the minimum of 4 then median of 3; the final sample is placed in the
     * 2nd 12th-tile; the pivot chosen from the sample is adaptive using the input {@code k}.
     *
     * <p>Given a pivot in the middle of the sample this has margins of 1/12 and 1/3.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @param flags Control flags.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private static int repeatedStepFarLeft(double[] a, int l, int r, int k, int[] upper, int flags) {
        // Far step has been changed from the Alexandrescu (2016) step of lower-median-of-4, min-of-3
        // into the 4th 12th-tile to a min-of-4, median-of-3 into the 2nd 12th-tile.
        // The differences are:
        // - The upper margin when not sampling is 8/24 vs. 9/24; the lower margin remains at 1/12.
        // - The position of the sample is closer to the expected location of k < |A| / 12.
        // - Sampling mode uses a median-of-3 with adaptive k, matching the other step methods.
        //   A min-of-3 sample can create a pivot too small if used with adaption of k leaving
        //   k in the larger parition and a wasted iteration.
        // - Adaption is adjusted to force use of the lower margin when not sampling.
        final int fp;
        final int s;
        int p;
        if (flags <= MODE_SAMPLING) {
            // 2nd 12th-tile
            fp = (r - l + 1) / 12;
            s = l + fp;
            // Use adaption
            p = s + mapDistance(k - l, l, r, fp);
        } else {
            // i in 2nd quartile; min into i-f (1st quartile)
            final int f = (r - l + 1) >> 2;
            final int f2 = f + f;
            final int end = l + f2;
            for (int i = l + f; i < end; i++) {
                if (a[i + f] < a[i - f]) {
                    final double u = a[i + f];
                    a[i + f] = a[i - f];
                    a[i - f] = u;
                }
                if (a[i + f2] < a[i]) {
                    final double v = a[i + f2];
                    a[i + f2] = a[i];
                    a[i] = v;
                }
                if (a[i] < a[i - f]) {
                    final double u = a[i];
                    a[i] = a[i - f];
                    a[i - f] = u;
                }
            }
            // 2nd 12th-tile
            fp = f / 3;
            s = l + fp;
            // Lower margin has 2(d+1) elements; d == (position in sample) - s
            // Force k into the lower margin
            p = s + ((k - l) >>> 1);
        }
        final int e = s + fp - 1;
        for (int i = s; i <= e; i++) {
            Sorting.sort3(a, i - fp, i, i + fp);
        }
        p = quickSelectAdaptive(a, s, e, p, p, upper, MODE_FR_SAMPLING);
        return expandPartition(a, l, r, s, e, p, upper[0], upper);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Assumes the range {@code r - l >= 11}; the caller is responsible for selection on a smaller
     * range.
     *
     * <p>Uses the Chen and Dumitrescu repeated step median-of-medians-of-medians algorithm
     * with the maximum of 4 then median of 3; the final sample is placed in the
     * 11th 12th-tile; the pivot chosen from the sample is adaptive using the input {@code k}.
     *
     * <p>Given a pivot in the middle of the sample this has margins of 1/3 and 1/12.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @param flags Control flags.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private static int repeatedStepFarRight(double[] a, int l, int r, int k, int[] upper, int flags) {
        // Mirror image repeatedStepFarLeft
        final int fp;
        final int e;
        int p;
        if (flags <= MODE_SAMPLING) {
            // 11th 12th-tile
            fp = (r - l + 1) / 12;
            e = r - fp;
            // Use adaption
            p = e - mapDistance(r - k, l, r, fp);
        } else {
            // i in 3rd quartile; max into i+f (4th quartile)
            final int f = (r - l + 1) >> 2;
            final int f2 = f + f;
            final int end = r - f2;
            for (int i = r - f; i > end; i--) {
                if (a[i - f] > a[i + f]) {
                    final double u = a[i - f];
                    a[i - f] = a[i + f];
                    a[i + f] = u;
                }
                if (a[i - f2] > a[i]) {
                    final double v = a[i - f2];
                    a[i - f2] = a[i];
                    a[i] = v;
                }
                if (a[i] > a[i + f]) {
                    final double u = a[i];
                    a[i] = a[i + f];
                    a[i + f] = u;
                }
            }
            // 11th 12th-tile
            fp = f / 3;
            e = r - fp;
            // Upper margin has 2(d+1) elements; d == e - (position in sample)
            // Force k into the upper margin
            p = e - ((r - k) >>> 1);
        }
        final int s = e - fp + 1;
        for (int i = s; i <= e; i++) {
            Sorting.sort3(a, i - fp, i, i + fp);
        }
        p = quickSelectAdaptive(a, s, e, p, p, upper, MODE_FR_SAMPLING);
        return expandPartition(a, l, r, s, e, p, upper[0], upper);
    }

    /**
     * Expand a partition around a single pivot. Partitioning exchanges array
     * elements such that all elements smaller than pivot are before it and all
     * elements larger than pivot are after it. The central region is already
     * partitioned.
     *
     * <pre>{@code
     * |l             |s   |p0 p1|   e|                r|
     * |    ???       | <P | ==P | >P |        ???      |
     * }</pre>
     *
     * <p>This requires that {@code start != end}. However it handles
     * {@code left == start} and/or {@code end == right}.
     *
     * @param a Data array.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param start Start of the partition range (inclusive).
     * @param end End of the partitioned range (inclusive).
     * @param pivot0 Lower pivot location (inclusive).
     * @param pivot1 Upper pivot location (inclusive).
     * @param upper Upper bound (inclusive) of the pivot range [k1].
     * @return Lower bound (inclusive) of the pivot range [k0].
     */
    // package-private for testing
    static int expandPartition(double[] a, int left, int right, int start, int end,
        int pivot0, int pivot1, int[] upper) {
        // 3-way partition of the data using a pivot value into
        // less-than, equal or greater-than.
        // Based on Sedgewick's Bentley-McIroy partitioning: always swap i<->j then
        // check for equal to the pivot and move again.
        //
        // Move sentinels from start and end to left and right. Scan towards the
        // sentinels until >=,<=. Swap then move == to the pivot region.
        //           <-i                           j->
        // |l |        |            |p0  p1|       |             | r|
        // |>=|   ???  |     <      |  ==  |   >   |     ???     |<=|
        //
        // When either i or j reach the edge perform finishing loop.
        // Finish loop for a[j] <= v replaces j with p1+1, optionally moves value
        // to p0 for < and updates the pivot range p1 (and optionally p0):
        //                                             j->
        // |l                       |p0  p1|           |         | r|
        // |         <              |  ==  |       >   |   ???   |<=|

        final double v = a[pivot0];
        // Use start/end as sentinels (requires start != end)
        double vi = a[start];
        double vj = a[end];
        a[start] = a[left];
        a[end] = a[right];
        a[left] = vj;
        a[right] = vi;

        int i = start + 1;
        int j = end - 1;

        // Positioned for pre-in/decrement to write to pivot region
        int p0 = pivot0 == start ? i : pivot0;
        int p1 = pivot1 == end ? j : pivot1;

        while (true) {
            do {
                --i;
            } while (a[i] < v);
            do {
                ++j;
            } while (a[j] > v);
            vj = a[i];
            vi = a[j];
            a[i] = vi;
            a[j] = vj;
            // Move the equal values to pivot region
            if (vi == v) {
                a[i] = a[--p0];
                a[p0] = v;
            }
            if (vj == v) {
                a[j] = a[++p1];
                a[p1] = v;
            }
            // Termination check and finishing loops.
            // Note: This works even if pivot region is zero length (p1 == p0-1 due to
            // length 1 pivot region at either start/end) because we pre-inc/decrement
            // one side and post-inc/decrement the other side.
            if (i == left) {
                while (j < right) {
                    do {
                        ++j;
                    } while (a[j] > v);
                    final double w = a[j];
                    // Move upper bound of pivot region
                    a[j] = a[++p1];
                    a[p1] = v;
                    // Move lower bound of pivot region
                    if (w != v) {
                        a[p0] = w;
                        p0++;
                    }
                }
                break;
            }
            if (j == right) {
                while (i > left) {
                    do {
                        --i;
                    } while (a[i] < v);
                    final double w = a[i];
                    // Move lower bound of pivot region
                    a[i] = a[--p0];
                    a[p0] = v;
                    // Move upper bound of pivot region
                    if (w != v) {
                        a[p1] = w;
                        p1--;
                    }
                }
                break;
            }
        }

        upper[0] = p1;
        return p0;
    }

    /**
     * Partition the array such that indices {@code k} correspond to their
     * correctly sorted value in the equivalent fully sorted array.
     *
     * <p>For all indices {@code k} and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>This function accepts a {@link UpdatingInterval} of indices {@code k} that define the
     * range of indices to partition. The {@link UpdatingInterval} can be narrowed or split as
     * partitioning divides the range.
     *
     * <p>Uses an introselect variant. The quickselect is a dual-pivot quicksort
     * partition method by Vladimir Yaroslavskiy; the fall-back on poor convergence of
     * the quickselect is a heapselect.
     *
     * <p>The {@code flags} contain the the current recursion count and the configured
     * length threshold for {@code r - l} to perform sort select. The count is in the upper
     * bits and the threshold is in the lower bits.
     *
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param k Interval of indices to partition (ordered).
     * @param flags Control flags.
     */
    // package-private for testing
    static void dualPivotQuickSelect(double[] a, int left, int right, UpdatingInterval k, int flags) {
        // If partitioning splits the interval then recursion is used for the left-most side(s)
        // and the right-most side remains within this function. If partitioning does
        // not split the interval then it remains within this function.
        int l = left;
        int r = right;
        int f = flags;
        int ka = k.left();
        int kb = k.right();
        final int[] upper = {0, 0, 0};
        while (true) {
            // Select when ka and kb are close to the same end,
            // or the entire range is small
            // |l|-----|ka|--------|kb|------|r|
            final int n = r - l;
            if (Math.min(kb - l, r - ka) < DP_SORTSELECT_SIZE ||
                n < (f & SORTSELECT_MASK)) {
                sortSelect(a, l, r, ka, kb);
                return;
            }
            if (kb - ka < DP_SORTSELECT_SIZE) {
                // Switch to single-pivot mode with Floyd-Rivest sub-sampling
                quickSelectAdaptive(a, l, r, ka, kb, upper, MODE_FR_SAMPLING);
                return;
            }
            if (f < 0) {
                // Excess recursion, switch to heap select
                heapSelect(a, l, r, ka, kb);
                return;
            }

            // Dual-pivot partitioning
            final int p0 = partition(a, l, r, upper);
            final int p1 = upper[0];

            // Recursion to max depth
            // Note: Here we possibly branch left, middle and right with multiple keys.
            // It is possible that the partition has split the keys
            // and the recursion proceeds with a reduced set in each region.
            //                   p0 p1               p2 p3
            // |l|--|ka|--k----k--|P|------k--|kb|----|P|----|r|
            //                 kb  |      ka
            f += RECURSION_INCREMENT;
            // Recurse left side if required
            if (ka < p0) {
                if (kb <= p1) {
                    // Entirely on left side
                    r = p0 - 1;
                    if (r < kb) {
                        kb = k.updateRight(r);
                    }
                    continue;
                }
                dualPivotQuickSelect(a, l, p0 - 1, k.splitLeft(p0, p1), f);
                // Here we must process middle and/or right
                ka = k.left();
            } else if (kb <= p1) {
                // No middle/right side
                return;
            } else if (ka <= p1) {
                // Advance lower bound
                ka = k.updateLeft(p1 + 1);
            }
            // Recurse middle if required
            final int p2 = upper[1];
            final int p3 = upper[2];
            if (ka < p2) {
                l = p1 + 1;
                if (kb <= p3) {
                    // Entirely in middle
                    r = p2 - 1;
                    if (r < kb) {
                        kb = k.updateRight(r);
                    }
                    continue;
                }
                dualPivotQuickSelect(a, l, p2 - 1, k.splitLeft(p2, p3), f);
                ka = k.left();
            } else if (kb <= p3) {
                // No right side
                return;
            } else if (ka <= p3) {
                ka = k.updateLeft(p3 + 1);
            }
            // Continue right
            l = p3 + 1;
        }
    }

    /**
     * Partition an array slice around 2 pivots. Partitioning exchanges array elements
     * such that all elements smaller than pivot are before it and all elements larger
     * than pivot are after it.
     *
     * <p>This method returns 4 points describing the pivot ranges of equal values.
     *
     * <pre>{@code
     *         |k0  k1|                |k2  k3|
     * |   <P  | ==P1 |  <P1 && <P2    | ==P2 |   >P   |
     * }</pre>
     *
     * <ul>
     * <li>k0: lower pivot1 point</li>
     * <li>k1: upper pivot1 point (inclusive)</li>
     * <li>k2: lower pivot2 point</li>
     * <li>k3: upper pivot2 point (inclusive)</li>
     * </ul>
     *
     * <p>Bounds are set so {@code i < k0}, {@code i > k3} and {@code k1 < i < k2} are
     * unsorted. When the range {@code [k0, k3]} contains fully sorted elements the result
     * is set to {@code k1 = k3; k2 == k0}. This can occur if
     * {@code P1 == P2} or there are zero or one value between the pivots
     * {@code P1 < v < P2}. Any sort/partition of ranges [left, k0-1], [k1+1, k2-1] and
     * [k3+1, right] must check the length is {@code > 1}.
     *
     * @param a Data array.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param bounds Points [k1, k2, k3].
     * @return Lower bound (inclusive) of the pivot range [k0].
     */
    private static int partition(double[] a, int left, int right, int[] bounds) {
        // Pick 2 pivots from 5 approximately uniform through the range.
        // Spacing is ~ 1/7 made using shifts. Other strategies are equal or much
        // worse. 1/7 = 5/35 ~ 1/8 + 1/64 : 0.1429 ~ 0.1406
        // Ensure the value is above zero to choose different points!
        final int n = right - left;
        final int step = 1 + (n >>> 3) + (n >>> 6);
        final int i3 = left + (n >>> 1);
        final int i2 = i3 - step;
        final int i1 = i2 - step;
        final int i4 = i3 + step;
        final int i5 = i4 + step;
        Sorting.sort5(a, i1, i2, i3, i4, i5);

        // Partition data using pivots P1 and P2 into less-than, greater-than or between.
        // Pivot values P1 & P2 are placed at the end. If P1 < P2, P2 acts as a sentinel.
        // k traverses the unknown region ??? and values moved if less-than or
        // greater-than:
        //
        // left        less              k       great         right
        // |P1|  <P1   |   P1 <= & <= P2 |    ???    |    >P2   |P2|
        //
        // <P1            (left, lt)
        // P1 <= & <= P2  [lt, k)
        // >P2            (gt, right)
        //
        // At the end pivots are swapped back to behind the less and great pointers.
        //
        // |  <P1        |P1|     P1<= & <= P2    |P2|      >P2    |

        // Swap ends to the pivot locations.
        final double v1 = a[i2];
        a[i2] = a[left];
        a[left] = v1;
        final double v2 = a[i4];
        a[i4] = a[right];
        a[right] = v2;

        // pointers
        int less = left;
        int great = right;

        // Fast-forward ascending / descending runs to reduce swaps.
        // Cannot overrun as end pivots (v1 <= v2) act as sentinels.
        do {
            ++less;
        } while (a[less] < v1);
        do {
            --great;
        } while (a[great] > v2);

        // a[less - 1] < P1 : a[great + 1] > P2
        // unvisited in [less, great]
        SORTING:
        for (int k = less; k <= great; k++) {
            final double v = a[k];
            if (v < v1) {
                // swap(a, k, less++)
                a[k] = a[less];
                a[less] = v;
                less++;
            } else if (v > v2) {
                // while k < great and a[great] > v2:
                //   great--
                while (a[great] > v2) {
                    if (great-- == k) {
                        // Done
                        break SORTING;
                    }
                }
                // swap(a, k, great--)
                // if a[k] < v1:
                //   swap(a, k, less++)
                final double w = a[great];
                a[great] = v;
                great--;
                // delay a[k] = w
                if (w < v1) {
                    a[k] = a[less];
                    a[less] = w;
                    less++;
                } else {
                    a[k] = w;
                }
            }
        }

        // Change to inclusive ends : a[less] < P1 : a[great] > P2
        less--;
        great++;
        // Move the pivots to correct locations
        a[left] = a[less];
        a[less] = v1;
        a[right] = a[great];
        a[great] = v2;

        // Record the pivot locations
        final int lower = less;
        bounds[2] = great;

        // equal elements
        // Original paper: If middle partition is bigger than a threshold
        // then check for equal elements.

        // Note: This is extra work. When performing partitioning the region of interest
        // may be entirely above or below the central region and this can be skipped.

        // Here we look for equal elements if the centre is more than 5/8 the length.
        // 5/8 = 1/2 + 1/8. Pivots must be different.
        if ((great - less) > (n >>> 1) + (n >>> 3) && v1 != v2) {

            // Fast-forward to reduce swaps. Changes inclusive ends to exclusive ends.
            // Since v1 != v2 these act as sentinels to prevent overrun.
            do {
                ++less;
            } while (a[less] == v1);
            do {
                --great;
            } while (a[great] == v2);

            // This copies the logic in the sorting loop using == comparisons
            EQUAL:
            for (int k = less; k <= great; k++) {
                final double v = a[k];
                if (v == v1) {
                    a[k] = a[less];
                    a[less] = v;
                    less++;
                } else if (v == v2) {
                    while (a[great] == v2) {
                        if (great-- == k) {
                            // Done
                            break EQUAL;
                        }
                    }
                    final double w = a[great];
                    a[great] = v;
                    great--;
                    if (w == v1) {
                        a[k] = a[less];
                        a[less] = w;
                        less++;
                    } else {
                        a[k] = w;
                    }
                }
            }

            // Change to inclusive ends
            less--;
            great++;
        }

        // Between pivots in (less, great)
        if (v1 != v2 && less < great - 1) {
            // Record the pivot end points
            bounds[0] = less;
            bounds[1] = great;
        } else {
            // No unsorted internal region (set k1 = k3; k2 = k0)
            bounds[0] = bounds[2];
            bounds[1] = lower;
        }

        return lower;
    }

    /**
     * Partition the elements between {@code ka} and {@code kb} using a heap select
     * algorithm. It is assumed {@code left <= ka <= kb <= right}.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param ka Lower index to select.
     * @param kb Upper index to select.
     */
    static void heapSelect(int[] a, int left, int right, int ka, int kb) {
        if (right <= left) {
            return;
        }
        // Use the smallest heap
        if (kb - left < right - ka) {
            heapSelectLeft(a, left, right, ka, kb);
        } else {
            heapSelectRight(a, left, right, ka, kb);
        }
    }

    /**
     * Partition the elements between {@code ka} and {@code kb} using a heap select
     * algorithm. It is assumed {@code left <= ka <= kb <= right}.
     *
     * <p>For best performance this should be called with {@code k} in the lower
     * half of the range.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param ka Lower index to select.
     * @param kb Upper index to select.
     */
    static void heapSelectLeft(int[] a, int left, int right, int ka, int kb) {
        // Create a max heap in-place in [left, k], rooted at a[left] = max
        // |l|-max-heap-|k|--------------|
        // Build the heap using Floyd's heap-construction algorithm for heap size n.
        // Start at parent of the last element in the heap (k),
        // i.e. start = parent(n-1) : parent(c) = floor((c - 1) / 2) : c = k - left
        int end = kb + 1;
        for (int p = left + ((kb - left - 1) >> 1); p >= left; p--) {
            maxHeapSiftDown(a, a[p], p, left, end);
        }
        // Scan the remaining data and insert
        // Mitigate worst case performance on descending data by backward sweep
        int max = a[left];
        for (int i = right; i > kb; i--) {
            final int v = a[i];
            if (v < max) {
                a[i] = max;
                maxHeapSiftDown(a, v, left, left, end);
                max = a[left];
            }
        }
        // Partition [ka, kb]
        // |l|-max-heap-|k|--------------|
        //  |  <-swap->  |   then sift down reduced size heap
        // Avoid sifting heap of size 1
        final int last = Math.max(left, ka - 1);
        while (--end > last) {
            maxHeapSiftDown(a, a[end], left, left, end);
            a[end] = max;
            max = a[left];
        }
    }

    /**
     * Sift the element down the max heap.
     *
     * <p>Assumes {@code root <= p < end}, i.e. the max heap is above root.
     *
     * @param a Heap data.
     * @param v Value to sift.
     * @param p Start position.
     * @param root Root of the heap.
     * @param end End of the heap (exclusive).
     */
    private static void maxHeapSiftDown(int[] a, int v, int p, int root, int end) {
        // child2 = root + 2 * (parent - root) + 2
        //        = 2 * parent - root + 2
        while (true) {
            // Right child
            int c = (p << 1) - root + 2;
            if (c > end) {
                // No left child
                break;
            }
            // Use the left child if right doesn't exist, or it is greater
            if (c == end || a[c] < a[c - 1]) {
                --c;
            }
            if (v >= a[c]) {
                // Parent greater than largest child - done
                break;
            }
            // Swap and descend
            a[p] = a[c];
            p = c;
        }
        a[p] = v;
    }

    /**
     * Partition the elements between {@code ka} and {@code kb} using a heap select
     * algorithm. It is assumed {@code left <= ka <= kb <= right}.
     *
     * <p>For best performance this should be called with {@code k} in the upper
     * half of the range.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param ka Lower index to select.
     * @param kb Upper index to select.
     */
    static void heapSelectRight(int[] a, int left, int right, int ka, int kb) {
        // Create a min heap in-place in [k, right], rooted at a[right] = min
        // |--------------|k|-min-heap-|r|
        // Build the heap using Floyd's heap-construction algorithm for heap size n.
        // Start at parent of the last element in the heap (k),
        // i.e. start = parent(n-1) : parent(c) = floor((c - 1) / 2) : c = right - k
        int end = ka - 1;
        for (int p = right - ((right - ka - 1) >> 1); p <= right; p++) {
            minHeapSiftDown(a, a[p], p, right, end);
        }
        // Scan the remaining data and insert
        // Mitigate worst case performance on descending data by backward sweep
        int min = a[right];
        for (int i = left; i < ka; i++) {
            final int v = a[i];
            if (v > min) {
                a[i] = min;
                minHeapSiftDown(a, v, right, right, end);
                min = a[right];
            }
        }
        // Partition [ka, kb]
        // |--------------|k|-min-heap-|r|
        //                 |  <-swap->  |   then sift down reduced size heap
        // Avoid sifting heap of size 1
        final int last = Math.min(right, kb + 1);
        while (++end < last) {
            minHeapSiftDown(a, a[end], right, right, end);
            a[end] = min;
            min = a[right];
        }
    }

    /**
     * Sift the element down the min heap.
     *
     * <p>Assumes {@code root >= p > end}, i.e. the max heap is below root.
     *
     * @param a Heap data.
     * @param v Value to sift.
     * @param p Start position.
     * @param root Root of the heap.
     * @param end End of the heap (exclusive).
     */
    private static void minHeapSiftDown(int[] a, int v, int p, int root, int end) {
        // child2 = root - 2 * (root - parent) - 2
        //        = 2 * parent - root - 2
        while (true) {
            // Right child
            int c = (p << 1) - root - 2;
            if (c < end) {
                // No left child
                break;
            }
            // Use the left child if right doesn't exist, or it is less
            if (c == end || a[c] > a[c + 1]) {
                ++c;
            }
            if (v <= a[c]) {
                // Parent less than smallest child - done
                break;
            }
            // Swap and descend
            a[p] = a[c];
            p = c;
        }
        a[p] = v;
    }

    /**
     * Partition the elements between {@code ka} and {@code kb} using a sort select
     * algorithm. It is assumed {@code left <= ka <= kb <= right}.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param ka Lower index to select.
     * @param kb Upper index to select.
     */
    static void sortSelect(int[] a, int left, int right, int ka, int kb) {
        // Combine the test for right <= left with
        // avoiding the overhead of sort select on tiny data.
        if (right - left <= MIN_SORTSELECT_SIZE) {
            Sorting.sort(a, left, right);
            return;
        }
        // Sort the smallest side
        if (kb - left < right - ka) {
            sortSelectLeft(a, left, right, kb);
        } else {
            sortSelectRight(a, left, right, ka);
        }
    }

    /**
     * Partition the minimum {@code n} elements below {@code k} where
     * {@code n = k - left + 1}. Uses an insertion sort algorithm.
     *
     * <p>Works with any {@code k} in the range {@code left <= k <= right}
     * and performs a full sort of the range below {@code k}.
     *
     * <p>For best performance this should be called with
     * {@code k - left < right - k}, i.e.
     * to partition a value in the lower half of the range.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param k Index to select.
     */
    static void sortSelectLeft(int[] a, int left, int right, int k) {
        // Sort
        for (int i = left; ++i <= k;) {
            final int v = a[i];
            // Move preceding higher elements above (if required)
            if (v < a[i - 1]) {
                int j = i;
                while (--j >= left && v < a[j]) {
                    a[j + 1] = a[j];
                }
                a[j + 1] = v;
            }
        }
        // Scan the remaining data and insert
        // Mitigate worst case performance on descending data by backward sweep
        int m = a[k];
        for (int i = right; i > k; i--) {
            final int v = a[i];
            if (v < m) {
                a[i] = m;
                int j = k;
                while (--j >= left && v < a[j]) {
                    a[j + 1] = a[j];
                }
                a[j + 1] = v;
                m = a[k];
            }
        }
    }

    /**
     * Partition the maximum {@code n} elements above {@code k} where
     * {@code n = right - k + 1}. Uses an insertion sort algorithm.
     *
     * <p>Works with any {@code k} in the range {@code left <= k <= right}
     * and can be used to perform a full sort of the range above {@code k}.
     *
     * <p>For best performance this should be called with
     * {@code k - left > right - k}, i.e.
     * to partition a value in the upper half of the range.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param k Index to select.
     */
    static void sortSelectRight(int[] a, int left, int right, int k) {
        // Sort
        for (int i = right; --i >= k;) {
            final int v = a[i];
            // Move succeeding lower elements below (if required)
            if (v > a[i + 1]) {
                int j = i;
                while (++j <= right && v > a[j]) {
                    a[j - 1] = a[j];
                }
                a[j - 1] = v;
            }
        }
        // Scan the remaining data and insert
        // Mitigate worst case performance on descending data by backward sweep
        int m = a[k];
        for (int i = left; i < k; i++) {
            final int v = a[i];
            if (v > m) {
                a[i] = m;
                int j = k;
                while (++j <= right && v > a[j]) {
                    a[j - 1] = a[j];
                }
                a[j - 1] = v;
                m = a[k];
            }
        }
    }

    /**
     * Partition the array such that index {@code k} corresponds to its correctly
     * sorted value in the equivalent fully sorted array.
     *
     * <p>Assumes {@code k} is a valid index into [left, right].
     *
     * @param a Values.
     * @param left Lower bound of data (inclusive).
     * @param right Upper bound of data (inclusive).
     * @param k Index.
     */
    static void select(int[] a, int left, int right, int k) {
        quickSelectAdaptive(a, left, right, k, k, new int[1], MODE_FR_SAMPLING);
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array.
     *
     * <p>The count of the number of used indices is returned. If the keys are sorted in-place,
     * the count is returned as a negative.
     *
     * @param a Values.
     * @param left Lower bound of data (inclusive).
     * @param right Upper bound of data (inclusive).
     * @param k Indices (may be destructively modified).
     * @param n Count of indices.
     */
    static void select(int[] a, int left, int right, int[] k, int n) {
        if (n == 1) {
            quickSelectAdaptive(a, left, right, k[0], k[0], new int[1], MODE_FR_SAMPLING);
            return;
        }

        // Interval creation validates the indices are in [left, right]
        final UpdatingInterval keys = IndexSupport.createUpdatingInterval(k, n);

        // Note: If the keys are not separated then they are effectively a single key.
        // Any split of keys separated by the sort select size
        // will be finished on the next iteration.
        final int k1 = keys.left();
        final int kn = keys.right();
        if (kn - k1 < DP_SORTSELECT_SIZE) {
            quickSelectAdaptive(a, left, right, k1, kn, new int[1], MODE_FR_SAMPLING);
        } else {
            // Dual-pivot mode with small range sort length configured using index density
            dualPivotQuickSelect(a, left, right, keys, dualPivotFlags(left, right, k1, kn));
        }
    }

    /**
     * Partition the array such that indices {@code k} correspond to their
     * correctly sorted value in the equivalent fully sorted array.
     *
     * <p>For all indices {@code [ka, kb]} and any index {@code i}:
     *
     * <pre>{@code
     * data[i < ka] <= data[ka] <= data[kb] <= data[kb < i]
     * }</pre>
     *
     * <p>This function accepts indices {@code [ka, kb]} that define the
     * range of indices to partition. It is expected that the range is small.
     *
     * <p>The {@code flags} are used to control the sampling mode and adaption of
     * the index within the sample.
     *
     * <p>Returns the bounds containing {@code [ka, kb]}. These may be lower/higher
     * than the keys if equal values are present in the data.
     *
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param ka First key of interest.
     * @param kb Last key of interest.
     * @param bounds Upper bound of the range containing {@code [ka, kb]} (inclusive).
     * @param flags Adaption flags.
     * @return Lower bound of the range containing {@code [ka, kb]} (inclusive).
     */
    static int quickSelectAdaptive(int[] a, int left, int right, int ka, int kb,
            int[] bounds, int flags) {
        int l = left;
        int r = right;
        int m = flags;
        while (true) {
            // Select when ka and kb are close to the same end
            // |l|-----|ka|kkkkkkkk|kb|------|r|
            if (Math.min(kb - l, r - ka) < LINEAR_SORTSELECT_SIZE) {
                sortSelect(a, l, r, ka, kb);
                bounds[0] = kb;
                return ka;
            }

            // Only target ka; kb is assumed to be close
            int p0;
            final int n = r - l;
            // f in [0, 1]
            final double f = (double) (ka - l) / n;
            // Record the larger margin (start at 1/4) to create the estimated size.
            // step        L     R
            // far left    1/12  1/3   (use 1/4 + 1/32 + 1/64 ~ 0.328)
            // left        1/6   1/4
            // middle      2/9   2/9   (use 1/4 - 1/32 ~ 0.219)
            int margin = n >> 2;
            if (m < MODE_SAMPLING && r - l > FR_SAMPLING_SIZE) {
                // Floyd-Rivest sample step uses the same margins
                p0 = sampleStep(a, l, r, ka, bounds);
                if (f <= STEP_FAR_LEFT || f >= STEP_FAR_RIGHT) {
                    margin += (n >> 5) + (n >> 6);
                } else if (f > STEP_LEFT && f < STEP_RIGHT) {
                    margin -= n >> 5;
                }
            } else if (f <= STEP_LEFT) {
                if (f <= STEP_FAR_LEFT) {
                    margin += (n >> 5) + (n >> 6);
                    p0 = repeatedStepFarLeft(a, l, r, ka, bounds, m);
                } else {
                    p0 = repeatedStepLeft(a, l, r, ka, bounds, m);
                }
            } else if (f >= STEP_RIGHT) {
                if (f >= STEP_FAR_RIGHT) {
                    margin += (n >> 5) + (n >> 6);
                    p0 = repeatedStepFarRight(a, l, r, ka, bounds, m);
                } else {
                    p0 = repeatedStepRight(a, l, r, ka, bounds, m);
                }
            } else {
                margin -= n >> 5;
                p0 = repeatedStep(a, l, r, ka, bounds, m);
            }

            // Note: Here we expect [ka, kb] to be small and splitting is unlikely.
            //                   p0 p1
            // |l|--|ka|kkkk|kb|--|P|-------------------|r|
            // |l|----------------|P|--|ka|kkk|kb|------|r|
            // |l|-----------|ka|k|P|k|kb|--------------|r|
            final int p1 = bounds[0];
            if (kb < p0) {
                // Entirely on left side
                r = p0 - 1;
            } else if (ka > p1) {
                // Entirely on right side
                l = p1 + 1;
            } else {
                // Pivot splits [ka, kb]. Expect ends to be close to the pivot and finish.
                // Here we set the bounds for use after median-of-medians pivot selection.
                // In the event there are many equal values this allows collecting those
                // known to be equal together when moving around the medians sample.
                if (kb > p1) {
                    sortSelectLeft(a, p1 + 1, r, kb);
                    bounds[0] = kb;
                }
                if (ka < p0) {
                    sortSelectRight(a, l, p0 - 1, ka);
                    p0 = ka;
                }
                return p0;
            }
            // Update mode based on target partition size
            if (r - l > n - margin) {
                m++;
            }
        }
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Partitions a Floyd-Rivest sample around a pivot offset so that the input {@code k} will
     * fall in the smaller partition when the entire range is partitioned.
     *
     * <p>Assumes the range {@code r - l} is large.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private static int sampleStep(int[] a, int l, int r, int k, int[] upper) {
        // Floyd-Rivest: use SELECT recursively on a sample of size S to get an estimate
        // for the (k-l+1)-th smallest element into a[k], biased slightly so that the
        // (k-l+1)-th element is expected to lie in the smaller set after partitioning.
        final int n = r - l + 1;
        final int ith = k - l + 1;
        final double z = Math.log(n);
        // sample size = 0.5 * n^(2/3)
        final double s = 0.5 * Math.exp(0.6666666666666666 * z);
        final double sd = 0.5 * Math.sqrt(z * s * (n - s) / n) * Integer.signum(ith - (n >> 1));
        final int ll = Math.max(l, (int) (k - ith * s / n + sd));
        final int rr = Math.min(r, (int) (k + (n - ith) * s / n + sd));
        // Sample recursion restarts from [ll, rr]
        final int p = quickSelectAdaptive(a, ll, rr, k, k, upper, MODE_FR_SAMPLING);
        return expandPartition(a, l, r, ll, rr, p, upper[0], upper);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Assumes the range {@code r - l >= 8}; the caller is responsible for selection on a smaller
     * range. If using a 12th-tile for sampling then assumes {@code r - l >= 11}.
     *
     * <p>Uses the Chen and Dumitrescu repeated step median-of-medians-of-medians algorithm
     * with the median of 3 then median of 3; the final sample is placed in the
     * 5th 9th-tile; the pivot chosen from the sample is adaptive using the input {@code k}.
     *
     * <p>Given a pivot in the middle of the sample this has margins of 2/9 and 2/9.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @param flags Control flags.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private static int repeatedStep(int[] a, int l, int r, int k, int[] upper, int flags) {
        // Adapted from Alexandrescu (2016), algorithm 8.
        final int fp;
        final int s;
        int p;
        if (flags <= MODE_SAMPLING) {
            // Median into a 12th-tile
            fp = (r - l + 1) / 12;
            // Position the sample around the target k
            s = k - mapDistance(k - l, l, r, fp);
            p = k;
        } else {
            // i in tertile [3f':6f')
            fp = (r - l + 1) / 9;
            final int f3 = 3 * fp;
            final int end = l + (f3 << 1);
            for (int i = l + f3; i < end; i++) {
                Sorting.sort3(a, i - f3, i, i + f3);
            }
            // 5th 9th-tile: [4f':5f')
            s = l + (fp << 2);
            // No adaption uses the middle to enforce strict margins
            p = s + (flags == MODE_ADAPTION ? mapDistance(k - l, l, r, fp) : (fp >>> 1));
        }
        final int e = s + fp - 1;
        for (int i = s; i <= e; i++) {
            Sorting.sort3(a, i - fp, i, i + fp);
        }
        p = quickSelectAdaptive(a, s, e, p, p, upper, MODE_FR_SAMPLING);
        return expandPartition(a, l, r, s, e, p, upper[0], upper);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Assumes the range {@code r - l >= 11}; the caller is responsible for selection on a smaller
     * range.
     *
     * <p>Uses the Chen and Dumitrescu repeated step median-of-medians-of-medians algorithm
     * with the lower median of 4 then either median of 3 with the final sample placed in the
     * 5th 12th-tile, or min of 3 with the final sample in the 4th 12th-tile;
     * the pivot chosen from the sample is adaptive using the input {@code k}.
     *
     * <p>Given a pivot in the middle of the sample this has margins of 1/6 and 1/4.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @param flags Control flags.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private static int repeatedStepLeft(int[] a, int l, int r, int k, int[] upper, int flags) {
        // Adapted from Alexandrescu (2016), algorithm 9.
        final int fp;
        final int s;
        int p;
        if (flags <= MODE_SAMPLING) {
            // Median into a 12th-tile
            fp = (r - l + 1) / 12;
            // Position the sample around the target k
            // Avoid bounds error due to rounding as (k-l)/(r-l) -> 1/12
            s = Math.max(k - mapDistance(k - l, l, r, fp), l + fp);
            p = k;
        } else {
            // i in 2nd quartile
            final int f = (r - l + 1) >> 2;
            final int f2 = f + f;
            final int end = l + f2;
            for (int i = l + f; i < end; i++) {
                Sorting.lowerMedian4(a, i - f, i, i + f, i + f2);
            }
            // i in 5th 12th-tile
            fp = f / 3;
            s = l + f + fp;
            // No adaption uses the middle to enforce strict margins
            p = s + (flags == MODE_ADAPTION ? mapDistance(k - l, l, r, fp) : (fp >>> 1));
        }
        final int e = s + fp - 1;
        for (int i = s; i <= e; i++) {
            Sorting.sort3(a, i - fp, i, i + fp);
        }
        p = quickSelectAdaptive(a, s, e, p, p, upper, MODE_FR_SAMPLING);
        return expandPartition(a, l, r, s, e, p, upper[0], upper);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Assumes the range {@code r - l >= 11}; the caller is responsible for selection on a smaller
     * range.
     *
     * <p>Uses the Chen and Dumitrescu repeated step median-of-medians-of-medians algorithm
     * with the upper median of 4 then either median of 3 with the final sample placed in the
     * 8th 12th-tile, or max of 3 with the final sample in the 9th 12th-tile;
     * the pivot chosen from the sample is adaptive using the input {@code k}.
     *
     * <p>Given a pivot in the middle of the sample this has margins of 1/4 and 1/6.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @param flags Control flags.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private static int repeatedStepRight(int[] a, int l, int r, int k, int[] upper, int flags) {
        // Mirror image repeatedStepLeft using upper median into 3rd quartile
        final int fp;
        final int e;
        int p;
        if (flags <= MODE_SAMPLING) {
            // Median into a 12th-tile
            fp = (r - l + 1) / 12;
            // Position the sample around the target k
            // Avoid bounds error due to rounding as (r-k)/(r-l) -> 11/12
            e = Math.min(k + mapDistance(r - k, l, r, fp), r - fp);
            p = k;
        } else {
            // i in 3rd quartile
            final int f = (r - l + 1) >> 2;
            final int f2 = f + f;
            final int end = r - f2;
            for (int i = r - f; i > end; i--) {
                Sorting.upperMedian4(a, i - f2, i - f, i, i + f);
            }
            // i in 8th 12th-tile
            fp = f / 3;
            e = r - f - fp;
            // No adaption uses the middle to enforce strict margins
            p = e - (flags == MODE_ADAPTION ? mapDistance(r - k, l, r, fp) : (fp >>> 1));
        }
        final int s = e - fp + 1;
        for (int i = s; i <= e; i++) {
            Sorting.sort3(a, i - fp, i, i + fp);
        }
        p = quickSelectAdaptive(a, s, e, p, p, upper, MODE_FR_SAMPLING);
        return expandPartition(a, l, r, s, e, p, upper[0], upper);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Assumes the range {@code r - l >= 11}; the caller is responsible for selection on a smaller
     * range.
     *
     * <p>Uses the Chen and Dumitrescu repeated step median-of-medians-of-medians algorithm
     * with the minimum of 4 then median of 3; the final sample is placed in the
     * 2nd 12th-tile; the pivot chosen from the sample is adaptive using the input {@code k}.
     *
     * <p>Given a pivot in the middle of the sample this has margins of 1/12 and 1/3.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @param flags Control flags.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private static int repeatedStepFarLeft(int[] a, int l, int r, int k, int[] upper, int flags) {
        // Far step has been changed from the Alexandrescu (2016) step of lower-median-of-4, min-of-3
        // into the 4th 12th-tile to a min-of-4, median-of-3 into the 2nd 12th-tile.
        // The differences are:
        // - The upper margin when not sampling is 8/24 vs. 9/24; the lower margin remains at 1/12.
        // - The position of the sample is closer to the expected location of k < |A| / 12.
        // - Sampling mode uses a median-of-3 with adaptive k, matching the other step methods.
        //   A min-of-3 sample can create a pivot too small if used with adaption of k leaving
        //   k in the larger parition and a wasted iteration.
        // - Adaption is adjusted to force use of the lower margin when not sampling.
        final int fp;
        final int s;
        int p;
        if (flags <= MODE_SAMPLING) {
            // 2nd 12th-tile
            fp = (r - l + 1) / 12;
            s = l + fp;
            // Use adaption
            p = s + mapDistance(k - l, l, r, fp);
        } else {
            // i in 2nd quartile; min into i-f (1st quartile)
            final int f = (r - l + 1) >> 2;
            final int f2 = f + f;
            final int end = l + f2;
            for (int i = l + f; i < end; i++) {
                if (a[i + f] < a[i - f]) {
                    final int u = a[i + f];
                    a[i + f] = a[i - f];
                    a[i - f] = u;
                }
                if (a[i + f2] < a[i]) {
                    final int v = a[i + f2];
                    a[i + f2] = a[i];
                    a[i] = v;
                }
                if (a[i] < a[i - f]) {
                    final int u = a[i];
                    a[i] = a[i - f];
                    a[i - f] = u;
                }
            }
            // 2nd 12th-tile
            fp = f / 3;
            s = l + fp;
            // Lower margin has 2(d+1) elements; d == (position in sample) - s
            // Force k into the lower margin
            p = s + ((k - l) >>> 1);
        }
        final int e = s + fp - 1;
        for (int i = s; i <= e; i++) {
            Sorting.sort3(a, i - fp, i, i + fp);
        }
        p = quickSelectAdaptive(a, s, e, p, p, upper, MODE_FR_SAMPLING);
        return expandPartition(a, l, r, s, e, p, upper[0], upper);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Assumes the range {@code r - l >= 11}; the caller is responsible for selection on a smaller
     * range.
     *
     * <p>Uses the Chen and Dumitrescu repeated step median-of-medians-of-medians algorithm
     * with the maximum of 4 then median of 3; the final sample is placed in the
     * 11th 12th-tile; the pivot chosen from the sample is adaptive using the input {@code k}.
     *
     * <p>Given a pivot in the middle of the sample this has margins of 1/3 and 1/12.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @param flags Control flags.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private static int repeatedStepFarRight(int[] a, int l, int r, int k, int[] upper, int flags) {
        // Mirror image repeatedStepFarLeft
        final int fp;
        final int e;
        int p;
        if (flags <= MODE_SAMPLING) {
            // 11th 12th-tile
            fp = (r - l + 1) / 12;
            e = r - fp;
            // Use adaption
            p = e - mapDistance(r - k, l, r, fp);
        } else {
            // i in 3rd quartile; max into i+f (4th quartile)
            final int f = (r - l + 1) >> 2;
            final int f2 = f + f;
            final int end = r - f2;
            for (int i = r - f; i > end; i--) {
                if (a[i - f] > a[i + f]) {
                    final int u = a[i - f];
                    a[i - f] = a[i + f];
                    a[i + f] = u;
                }
                if (a[i - f2] > a[i]) {
                    final int v = a[i - f2];
                    a[i - f2] = a[i];
                    a[i] = v;
                }
                if (a[i] > a[i + f]) {
                    final int u = a[i];
                    a[i] = a[i + f];
                    a[i + f] = u;
                }
            }
            // 11th 12th-tile
            fp = f / 3;
            e = r - fp;
            // Upper margin has 2(d+1) elements; d == e - (position in sample)
            // Force k into the upper margin
            p = e - ((r - k) >>> 1);
        }
        final int s = e - fp + 1;
        for (int i = s; i <= e; i++) {
            Sorting.sort3(a, i - fp, i, i + fp);
        }
        p = quickSelectAdaptive(a, s, e, p, p, upper, MODE_FR_SAMPLING);
        return expandPartition(a, l, r, s, e, p, upper[0], upper);
    }

    /**
     * Expand a partition around a single pivot. Partitioning exchanges array
     * elements such that all elements smaller than pivot are before it and all
     * elements larger than pivot are after it. The central region is already
     * partitioned.
     *
     * <pre>{@code
     * |l             |s   |p0 p1|   e|                r|
     * |    ???       | <P | ==P | >P |        ???      |
     * }</pre>
     *
     * <p>This requires that {@code start != end}. However it handles
     * {@code left == start} and/or {@code end == right}.
     *
     * @param a Data array.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param start Start of the partition range (inclusive).
     * @param end End of the partitioned range (inclusive).
     * @param pivot0 Lower pivot location (inclusive).
     * @param pivot1 Upper pivot location (inclusive).
     * @param upper Upper bound (inclusive) of the pivot range [k1].
     * @return Lower bound (inclusive) of the pivot range [k0].
     */
    // package-private for testing
    static int expandPartition(int[] a, int left, int right, int start, int end,
        int pivot0, int pivot1, int[] upper) {
        // 3-way partition of the data using a pivot value into
        // less-than, equal or greater-than.
        // Based on Sedgewick's Bentley-McIroy partitioning: always swap i<->j then
        // check for equal to the pivot and move again.
        //
        // Move sentinels from start and end to left and right. Scan towards the
        // sentinels until >=,<=. Swap then move == to the pivot region.
        //           <-i                           j->
        // |l |        |            |p0  p1|       |             | r|
        // |>=|   ???  |     <      |  ==  |   >   |     ???     |<=|
        //
        // When either i or j reach the edge perform finishing loop.
        // Finish loop for a[j] <= v replaces j with p1+1, optionally moves value
        // to p0 for < and updates the pivot range p1 (and optionally p0):
        //                                             j->
        // |l                       |p0  p1|           |         | r|
        // |         <              |  ==  |       >   |   ???   |<=|

        final int v = a[pivot0];
        // Use start/end as sentinels (requires start != end)
        int vi = a[start];
        int vj = a[end];
        a[start] = a[left];
        a[end] = a[right];
        a[left] = vj;
        a[right] = vi;

        int i = start + 1;
        int j = end - 1;

        // Positioned for pre-in/decrement to write to pivot region
        int p0 = pivot0 == start ? i : pivot0;
        int p1 = pivot1 == end ? j : pivot1;

        while (true) {
            do {
                --i;
            } while (a[i] < v);
            do {
                ++j;
            } while (a[j] > v);
            vj = a[i];
            vi = a[j];
            a[i] = vi;
            a[j] = vj;
            // Move the equal values to pivot region
            if (vi == v) {
                a[i] = a[--p0];
                a[p0] = v;
            }
            if (vj == v) {
                a[j] = a[++p1];
                a[p1] = v;
            }
            // Termination check and finishing loops.
            // Note: This works even if pivot region is zero length (p1 == p0-1 due to
            // length 1 pivot region at either start/end) because we pre-inc/decrement
            // one side and post-inc/decrement the other side.
            if (i == left) {
                while (j < right) {
                    do {
                        ++j;
                    } while (a[j] > v);
                    final int w = a[j];
                    // Move upper bound of pivot region
                    a[j] = a[++p1];
                    a[p1] = v;
                    // Move lower bound of pivot region
                    if (w != v) {
                        a[p0] = w;
                        p0++;
                    }
                }
                break;
            }
            if (j == right) {
                while (i > left) {
                    do {
                        --i;
                    } while (a[i] < v);
                    final int w = a[i];
                    // Move lower bound of pivot region
                    a[i] = a[--p0];
                    a[p0] = v;
                    // Move upper bound of pivot region
                    if (w != v) {
                        a[p1] = w;
                        p1--;
                    }
                }
                break;
            }
        }

        upper[0] = p1;
        return p0;
    }

    /**
     * Partition the array such that indices {@code k} correspond to their
     * correctly sorted value in the equivalent fully sorted array.
     *
     * <p>For all indices {@code k} and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>This function accepts a {@link UpdatingInterval} of indices {@code k} that define the
     * range of indices to partition. The {@link UpdatingInterval} can be narrowed or split as
     * partitioning divides the range.
     *
     * <p>Uses an introselect variant. The quickselect is a dual-pivot quicksort
     * partition method by Vladimir Yaroslavskiy; the fall-back on poor convergence of
     * the quickselect is a heapselect.
     *
     * <p>The {@code flags} contain the the current recursion count and the configured
     * length threshold for {@code r - l} to perform sort select. The count is in the upper
     * bits and the threshold is in the lower bits.
     *
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param k Interval of indices to partition (ordered).
     * @param flags Control flags.
     */
    // package-private for testing
    static void dualPivotQuickSelect(int[] a, int left, int right, UpdatingInterval k, int flags) {
        // If partitioning splits the interval then recursion is used for the left-most side(s)
        // and the right-most side remains within this function. If partitioning does
        // not split the interval then it remains within this function.
        int l = left;
        int r = right;
        int f = flags;
        int ka = k.left();
        int kb = k.right();
        final int[] upper = {0, 0, 0};
        while (true) {
            // Select when ka and kb are close to the same end,
            // or the entire range is small
            // |l|-----|ka|--------|kb|------|r|
            final int n = r - l;
            if (Math.min(kb - l, r - ka) < DP_SORTSELECT_SIZE ||
                n < (f & SORTSELECT_MASK)) {
                sortSelect(a, l, r, ka, kb);
                return;
            }
            if (kb - ka < DP_SORTSELECT_SIZE) {
                // Switch to single-pivot mode with Floyd-Rivest sub-sampling
                quickSelectAdaptive(a, l, r, ka, kb, upper, MODE_FR_SAMPLING);
                return;
            }
            if (f < 0) {
                // Excess recursion, switch to heap select
                heapSelect(a, l, r, ka, kb);
                return;
            }

            // Dual-pivot partitioning
            final int p0 = partition(a, l, r, upper);
            final int p1 = upper[0];

            // Recursion to max depth
            // Note: Here we possibly branch left, middle and right with multiple keys.
            // It is possible that the partition has split the keys
            // and the recursion proceeds with a reduced set in each region.
            //                   p0 p1               p2 p3
            // |l|--|ka|--k----k--|P|------k--|kb|----|P|----|r|
            //                 kb  |      ka
            f += RECURSION_INCREMENT;
            // Recurse left side if required
            if (ka < p0) {
                if (kb <= p1) {
                    // Entirely on left side
                    r = p0 - 1;
                    if (r < kb) {
                        kb = k.updateRight(r);
                    }
                    continue;
                }
                dualPivotQuickSelect(a, l, p0 - 1, k.splitLeft(p0, p1), f);
                // Here we must process middle and/or right
                ka = k.left();
            } else if (kb <= p1) {
                // No middle/right side
                return;
            } else if (ka <= p1) {
                // Advance lower bound
                ka = k.updateLeft(p1 + 1);
            }
            // Recurse middle if required
            final int p2 = upper[1];
            final int p3 = upper[2];
            if (ka < p2) {
                l = p1 + 1;
                if (kb <= p3) {
                    // Entirely in middle
                    r = p2 - 1;
                    if (r < kb) {
                        kb = k.updateRight(r);
                    }
                    continue;
                }
                dualPivotQuickSelect(a, l, p2 - 1, k.splitLeft(p2, p3), f);
                ka = k.left();
            } else if (kb <= p3) {
                // No right side
                return;
            } else if (ka <= p3) {
                ka = k.updateLeft(p3 + 1);
            }
            // Continue right
            l = p3 + 1;
        }
    }

    /**
     * Partition an array slice around 2 pivots. Partitioning exchanges array elements
     * such that all elements smaller than pivot are before it and all elements larger
     * than pivot are after it.
     *
     * <p>This method returns 4 points describing the pivot ranges of equal values.
     *
     * <pre>{@code
     *         |k0  k1|                |k2  k3|
     * |   <P  | ==P1 |  <P1 && <P2    | ==P2 |   >P   |
     * }</pre>
     *
     * <ul>
     * <li>k0: lower pivot1 point</li>
     * <li>k1: upper pivot1 point (inclusive)</li>
     * <li>k2: lower pivot2 point</li>
     * <li>k3: upper pivot2 point (inclusive)</li>
     * </ul>
     *
     * <p>Bounds are set so {@code i < k0}, {@code i > k3} and {@code k1 < i < k2} are
     * unsorted. When the range {@code [k0, k3]} contains fully sorted elements the result
     * is set to {@code k1 = k3; k2 == k0}. This can occur if
     * {@code P1 == P2} or there are zero or one value between the pivots
     * {@code P1 < v < P2}. Any sort/partition of ranges [left, k0-1], [k1+1, k2-1] and
     * [k3+1, right] must check the length is {@code > 1}.
     *
     * @param a Data array.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param bounds Points [k1, k2, k3].
     * @return Lower bound (inclusive) of the pivot range [k0].
     */
    private static int partition(int[] a, int left, int right, int[] bounds) {
        // Pick 2 pivots from 5 approximately uniform through the range.
        // Spacing is ~ 1/7 made using shifts. Other strategies are equal or much
        // worse. 1/7 = 5/35 ~ 1/8 + 1/64 : 0.1429 ~ 0.1406
        // Ensure the value is above zero to choose different points!
        final int n = right - left;
        final int step = 1 + (n >>> 3) + (n >>> 6);
        final int i3 = left + (n >>> 1);
        final int i2 = i3 - step;
        final int i1 = i2 - step;
        final int i4 = i3 + step;
        final int i5 = i4 + step;
        Sorting.sort5(a, i1, i2, i3, i4, i5);

        // Partition data using pivots P1 and P2 into less-than, greater-than or between.
        // Pivot values P1 & P2 are placed at the end. If P1 < P2, P2 acts as a sentinel.
        // k traverses the unknown region ??? and values moved if less-than or
        // greater-than:
        //
        // left        less              k       great         right
        // |P1|  <P1   |   P1 <= & <= P2 |    ???    |    >P2   |P2|
        //
        // <P1            (left, lt)
        // P1 <= & <= P2  [lt, k)
        // >P2            (gt, right)
        //
        // At the end pivots are swapped back to behind the less and great pointers.
        //
        // |  <P1        |P1|     P1<= & <= P2    |P2|      >P2    |

        // Swap ends to the pivot locations.
        final int v1 = a[i2];
        a[i2] = a[left];
        a[left] = v1;
        final int v2 = a[i4];
        a[i4] = a[right];
        a[right] = v2;

        // pointers
        int less = left;
        int great = right;

        // Fast-forward ascending / descending runs to reduce swaps.
        // Cannot overrun as end pivots (v1 <= v2) act as sentinels.
        do {
            ++less;
        } while (a[less] < v1);
        do {
            --great;
        } while (a[great] > v2);

        // a[less - 1] < P1 : a[great + 1] > P2
        // unvisited in [less, great]
        SORTING:
        for (int k = less; k <= great; k++) {
            final int v = a[k];
            if (v < v1) {
                // swap(a, k, less++)
                a[k] = a[less];
                a[less] = v;
                less++;
            } else if (v > v2) {
                // while k < great and a[great] > v2:
                //   great--
                while (a[great] > v2) {
                    if (great-- == k) {
                        // Done
                        break SORTING;
                    }
                }
                // swap(a, k, great--)
                // if a[k] < v1:
                //   swap(a, k, less++)
                final int w = a[great];
                a[great] = v;
                great--;
                // delay a[k] = w
                if (w < v1) {
                    a[k] = a[less];
                    a[less] = w;
                    less++;
                } else {
                    a[k] = w;
                }
            }
        }

        // Change to inclusive ends : a[less] < P1 : a[great] > P2
        less--;
        great++;
        // Move the pivots to correct locations
        a[left] = a[less];
        a[less] = v1;
        a[right] = a[great];
        a[great] = v2;

        // Record the pivot locations
        final int lower = less;
        bounds[2] = great;

        // equal elements
        // Original paper: If middle partition is bigger than a threshold
        // then check for equal elements.

        // Note: This is extra work. When performing partitioning the region of interest
        // may be entirely above or below the central region and this can be skipped.

        // Here we look for equal elements if the centre is more than 5/8 the length.
        // 5/8 = 1/2 + 1/8. Pivots must be different.
        if ((great - less) > (n >>> 1) + (n >>> 3) && v1 != v2) {

            // Fast-forward to reduce swaps. Changes inclusive ends to exclusive ends.
            // Since v1 != v2 these act as sentinels to prevent overrun.
            do {
                ++less;
            } while (a[less] == v1);
            do {
                --great;
            } while (a[great] == v2);

            // This copies the logic in the sorting loop using == comparisons
            EQUAL:
            for (int k = less; k <= great; k++) {
                final int v = a[k];
                if (v == v1) {
                    a[k] = a[less];
                    a[less] = v;
                    less++;
                } else if (v == v2) {
                    while (a[great] == v2) {
                        if (great-- == k) {
                            // Done
                            break EQUAL;
                        }
                    }
                    final int w = a[great];
                    a[great] = v;
                    great--;
                    if (w == v1) {
                        a[k] = a[less];
                        a[less] = w;
                        less++;
                    } else {
                        a[k] = w;
                    }
                }
            }

            // Change to inclusive ends
            less--;
            great++;
        }

        // Between pivots in (less, great)
        if (v1 != v2 && less < great - 1) {
            // Record the pivot end points
            bounds[0] = less;
            bounds[1] = great;
        } else {
            // No unsorted internal region (set k1 = k3; k2 = k0)
            bounds[0] = bounds[2];
            bounds[1] = lower;
        }

        return lower;
    }

    /**
     * Map the distance from the edge of {@code [l, r]} to a new distance in {@code [0, n)}.
     *
     * <p>The provides the adaption {@code kf'/|A|} from Alexandrescu (2016) where
     * {@code k == d}, {@code f' == n} and {@code |A| == r-l+1}.
     *
     * <p>For convenience this accepts the input range {@code [l, r]}.
     *
     * @param d Distance from the edge in {@code [0, r - l]}.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param n Size of the new range.
     * @return the mapped distance in [0, n)
     */
    private static int mapDistance(int d, int l, int r, int n) {
        return (int) (d * (n - 1.0) / (r - l));
    }

    /**
     * Configure the dual-pivot control flags. This packs the maximum recursion depth and
     * sort select size into a single integer.
     *
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param k1 First key of interest.
     * @param kn Last key of interest.
     * @return the flags
     */
    private static int dualPivotFlags(int left, int right, int k1, int kn) {
        final int maxDepth = dualPivotMaxDepth(right - left);
        final int ss = dualPivotSortSelectSize(k1, kn);
        return dualPivotFlags(maxDepth, ss);
    }

    /**
     * Configure the dual-pivot control flags. This packs the maximum recursion depth and
     * sort select size into a single integer.
     *
     * @param maxDepth Maximum recursion depth.
     * @param ss Sort select size.
     * @return the flags
     */
    static int dualPivotFlags(int maxDepth, int ss) {
        // The flags are packed using the upper bits to count back from -1 in
        // step sizes. The lower bits pack the sort select size.
        int flags = Integer.MIN_VALUE - maxDepth * RECURSION_INCREMENT;
        flags &= ~SORTSELECT_MASK;
        return flags | ss;
    }

    /**
     * Compute the maximum recursion depth for dual pivot recursion.
     * This is an approximation to {@code 2 * log3 (x)}.
     *
     * <p>The result is between {@code 2*floor(log3(x))} and {@code 2*ceil(log3(x))}.
     * The result is correctly rounded when {@code x +/- 1} is a power of 3.
     *
     * @param x Value.
     * @return maximum recursion depth
     */
    static int dualPivotMaxDepth(int x) {
        // log3(2) ~ 1.5849625
        // log3(x) ~ log2(x) * 0.630929753... ~ log2(x) * 323 / 512 (0.630859375)
        // Use (floor(log2(x))+1) * 323 / 256
        return ((32 - Integer.numberOfLeadingZeros(x)) * 323) >>> 8;
    }

    /**
     * Configure the sort select size for dual pivot partitioning.
     *
     * @param k1 First key of interest.
     * @param kn Last key of interest.
     * @return the sort select size.
     */
    private static int dualPivotSortSelectSize(int k1, int kn) {
        // Configure the sort select size based on the index density
        // l---k1---k---k-----k--k------kn----r
        //
        // For a full sort the dual-pivot quicksort can switch to insertion sort
        // when the length is small. The optimum value is dependent on the
        // hardware and the insertion sort implementation. Benchmarks show that
        // insertion sort can be used at length 80-120.
        //
        // During selection the SORTSELECT_SIZE specifies the distance from the edge
        // to use sort select. When keys are not dense there may be a small length
        // that is ignored by sort select due to the presence of another key.
        // Diagram of k-l = SORTSELECT_SIZE and r-k < SORTSELECT_SIZE where a second
        // key b is blocking the use of sort select. The key b is closest it can be to the right
        // key to enable blocking; it could be further away (up to k = left).
        //
        // |--SORTSELECT_SIZE--|
        //    |--SORTSELECT_SIZE--|
        // l--b----------------k--r
        // l----b--------------k----r
        // l------b------------k------r
        // l--------b----------k--------r
        // l----------b--------k----------r
        // l------------b------k------------r
        // l--------------b----k--------------r
        // l----------------b--k----------------r
        // l------------------bk------------------r
        //                    |--SORTSELECT_SIZE--|
        //
        // For all these cases the partitioning method would have to run. Assuming ideal
        // dual-pivot partitioning into thirds, and that the left key is randomly positioned
        // in [left, k) it is more likely that after partitioning 2 partitions will have to
        // be processed rather than 1 partition. In this case the options are:
        // - split the range using partitioning; sort select next iteration
        // - use sort select with a edge distance above the optimum length for single k selection
        //
        // Contrast with a longer length:
        // |--SORTSELECT_SIZE--|
        // l-------------------k-----k-------k-------------------r
        //                                   |--SORTSELECT_SIZE--|
        // Here partitioning has to run and 1, 2, or 3 partitions processed. But all k can
        // be found with a sort. In this case sort select could be used with a much higher
        // length (e.g. 80 - 120).
        //
        // When keys are extremely sparse (never within SORTSELECT_SIZE) then no switch
        // to sort select based on length is *required*. It may still be beneficial to avoid
        // partitioning if the length is very small due to the overhead of partitioning.
        //
        // Benchmarking with different lengths for a switch to sort select show inconsistent
        // behaviour across platforms due to the variable speed of insertion sort at longer
        // lengths. Attempts to transition the length based on various ramps schemes can
        // be incorrect and result is a slowdown rather than speed-up (if the correct threshold
        // is not chosen).
        //
        // Here we use a much simpler scheme based on these observations:
        // - If the average separation is very high then no length will collect extra indices
        // from a sort select over the current trigger of using the distance from the end. But
        // using a length dependence will not effect the work done by sort select as it only
        // performs the minimum sorting required.
        // - If the average separation is within the SORTSELECT_SIZE then a round of
        // partitioning will create multiple regions that all require a sort selection.
        // Thus a partitioning round can be avoided if the length is small.
        // - If the keys are at the end with nothing in between then partitioning will be able
        // to split them but a sort will have to sort the entire range:
        // lk-------------------------------kr
        // After partitioning starts the chance of keys being at the ends is low as keys
        // should be random within the divided range.
        // - Extremely high density keys is rare. It is only expected to saturate the range
        // with short lengths, e.g. 100 quantiles for length 1000 = separation 10 (high density)
        // but for length 10000 = separation 100 (low density).
        // - The density of (non-uniform) keys is hard to predict without complex analysis.
        //
        // Benchmarking using random keys at various density show no performance loss from
        // using a fixed size for the length dependence of sort select, if the size is small.
        // A large length can impact performance with low density keys, and on machines
        // where insertion sort is slower. Extreme performance gains occur when the average
        // separation of random keys is below 8-16, or of uniform keys around 32, by using a
        // sort at lengths up to 90. But this threshold shows performance loss greater than
        // the gains with separation of 64-128 on random keys, and on machines with slow
        // insertion sort. The transition to using an insertion sort of a longer length
        // is difficult to predict for all situations.

        // Let partitioning run if the initial length is small.
        // Use kn - k1 as a proxy for the length. If length is actually very large then
        // the final selection is insignificant. This avoids slowdown for small lengths
        // where the keys may only be at the ends. Note ideal dual-pivot partitioning
        // creates thirds so 1 iteration on SORTSELECT_SIZE * 3 should create
        // SORTSELECT_SIZE partitions.
        if (kn - k1 < DP_SORTSELECT_SIZE * 3) {
            return 0;
        }
        // Here partitioning will run at least once.
        // Stable performance across platforms using a modest length dependence.
        return DP_SORTSELECT_SIZE * 2;
    }
}
