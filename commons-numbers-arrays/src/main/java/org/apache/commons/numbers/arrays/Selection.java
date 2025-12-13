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
 * Select indices in array data.
 *
 * <p>Arranges elements such that indices {@code k} correspond to their correctly
 * sorted value in the equivalent fully sorted array. For all indices {@code k}
 * and any index {@code i}:
 *
 * <pre>{@code
 * data[i < k] <= data[k] <= data[k < i]
 * }</pre>
 *
 * <p>Examples:
 *
 * <pre>
 * data    [0, 1, 2, 1, 2, 5, 2, 3, 3, 6, 7, 7, 7, 7]
 *
 *
 * k=4   : [0, 1, 2, 1], [2], [5, 2, 3, 3, 6, 7, 7, 7, 7]
 * k=4,8 : [0, 1, 2, 1], [2], [3, 3, 2], [5], [6, 7, 7, 7, 7]
 * </pre>
 *
 * <p>This implementation can select on multiple indices and will handle duplicate and
 * unordered indices. The method detects ordered indices (with or without duplicates) and
 * uses this during processing. Passing ordered indices is recommended if the order is already
 * known; for example using uniform spacing through the array data, or to select the top and
 * bottom {@code n} values from the data.
 *
 * <p>A quickselect adaptive method is used for single indices. This uses analysis of the
 * partition sizes after each division to update the algorithm mode. If the partition
 * containing the target does not sufficiently reduce in size then the algorithm is
 * progressively changed to use partitions with guaranteed margins. This ensures a set fraction
 * of data is eliminated each step and worse-case linear run time performance. This method can
 * handle a range of indices {@code [ka, kb]} with a small separation by targeting the start of
 * the range {@code ka} and then selecting the remaining elements {@code (ka, kb]} that are at
 * the edge of the partition bounded by {@code ka}.
 *
 * <p>Multiple keys are partitioned collectively using an introsort method which only recurses
 * into partitions containing indices. Excess recursion will trigger use of a heapselect
 * on the remaining range of indices ensuring non-quadratic worse case performance. Any
 * partition containing a single index, adjacent pair of indices, or range of indices with a
 * small separation will use the quickselect adaptive method for single keys. Note that the
 * maximum number of times that {@code n} indices can be split is {@code n - 1} before all
 * indices are handled as singles.
 *
 * <p>Floating-point order
 *
 * <p>The {@code <} relation does not impose a total order on all floating-point values.
 * This class respects the ordering imposed by {@link Double#compare(double, double)}.
 * {@code -0.0} is treated as less than value {@code 0.0}; {@code Double.NaN} is
 * considered greater than any other value; and all {@code Double.NaN} values are
 * considered equal.
 *
 * <p>References
 *
 * <p>Quickselect is introduced in Hoare [1]. This selects an element {@code k} from {@code n}
 * using repeat division of the data around a partition element, recursing into the
 * partition that contains {@code k}.
 *
 * <p>Introsort/select is introduced in Musser [2]. This detects excess recursion in
 * quicksort/select and reverts to a heapsort or linear select to achieve an improved worst
 * case bound.
 *
 * <p>Use of sampling to identify a pivot that places {@code k} in the smaller partition is
 * performed in the SELECT algorithm of Floyd and Rivest [3, 4].
 *
 * <p>A worst-case linear time algorithm PICK is described in Blum <i>et al</i> [5]. This uses
 * the median of medians as a partition element for selection which ensures a minimum fraction of
 * the elements are eliminated per iteration. This was extended to use an asymmetric pivot choice
 * with efficient placement of the medians sample location in the QuickselectAdpative algorithm of
 * Alexandrescu [6].
 *
 * <ol>
 * <li>Hoare (1961)
 * Algorithm 65: Find
 * <a href="https://doi.org/10.1145%2F366622.366647">Comm. ACM. 4 (7): 321–322</a></li>
 * <li>Musser (1999)
 * Introspective Sorting and Selection Algorithms
 * <a href="https://doi.org/10.1002/(SICI)1097-024X(199708)27:8%3C983::AID-SPE117%3E3.0.CO;2-%23">
 * Software: Practice and Experience 27, 983-993.</a></li>
 * <li>Floyd and Rivest (1975)
 * Algorithm 489: The Algorithm SELECT—for Finding the ith Smallest of n elements.
 * Comm. ACM. 18 (3): 173.</li>
 * <li>Kiwiel (2005)
 * On Floyd and Rivest's SELECT algorithm.
 * Theoretical Computer Science 347, 214-238.</li>
 * <li>Blum, Floyd, Pratt, Rivest, and Tarjan (1973)
 * Time bounds for selection.
 * <a href="https://doi.org/10.1016%2FS0022-0000%2873%2980033-9">
 * Journal of Computer and System Sciences. 7 (4): 448–461</a>.</li>
 * <li>Alexandrescu (2016)
 * Fast Deterministic Selection
 * <a href="https://arxiv.org/abs/1606.00484">arXiv:1606.00484</a>.</li>
 * <li><a href="https://en.wikipedia.org/wiki/Quickselect">Quickselect (Wikipedia)</a></li>
 * <li><a href="https://en.wikipedia.org/wiki/Introsort">Introsort (Wikipedia)</a></li>
 * <li><a href="https://en.wikipedia.org/wiki/Introselect">Introselect (Wikipedia)</a></li>
 * <li><a href="https://en.wikipedia.org/wiki/Floyd%E2%80%93Rivest_algorithm">Floyd-Rivest algorithm (Wikipedia)</a></li>
 * <li><a href="https://en.wikipedia.org/wiki/Median_of_medians">Median of medians (Wikipedia)</a></li>
 * </ol>
 *
 * @since 1.2
 */
public final class Selection {

    /** No instances. */
    private Selection() {}

    /**
     * Partition the array such that index {@code k} corresponds to its correctly
     * sorted value in the equivalent fully sorted array.
     *
     * @param a Values.
     * @param k Index.
     * @throws IndexOutOfBoundsException if index {@code k} is not within the
     * sub-range {@code [0, a.length)}
     */
    public static void select(double[] a, int k) {
        IndexSupport.checkIndex(0, a.length, k);
        doSelect(a, 0, a.length, k);
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array.
     *
     * @param a Values.
     * @param k Indices (may be destructively modified).
     * @throws IndexOutOfBoundsException if any index {@code k} is not within the
     * sub-range {@code [0, a.length)}
     */
    public static void select(double[] a, int[] k) {
        IndexSupport.checkIndices(0, a.length, k);
        doSelect(a, 0, a.length, k);
    }

    /**
     * Partition the array such that index {@code k} corresponds to its correctly
     * sorted value in the equivalent fully sorted array.
     *
     * @param a Values.
     * @param fromIndex Index of the first element (inclusive).
     * @param toIndex Index of the last element (exclusive).
     * @param k Index.
     * @throws IndexOutOfBoundsException if the sub-range {@code [fromIndex, toIndex)} is out of
     * bounds of range {@code [0, a.length)}; or if index {@code k} is not within the
     * sub-range {@code [fromIndex, toIndex)}
     */
    public static void select(double[] a, int fromIndex, int toIndex, int k) {
        IndexSupport.checkFromToIndex(fromIndex, toIndex, a.length);
        IndexSupport.checkIndex(fromIndex, toIndex, k);
        doSelect(a, fromIndex, toIndex, k);
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array.
     *
     * @param a Values.
     * @param fromIndex Index of the first element (inclusive).
     * @param toIndex Index of the last element (exclusive).
     * @param k Indices (may be destructively modified).
     * @throws IndexOutOfBoundsException if the sub-range {@code [fromIndex, toIndex)} is out of
     * bounds of range {@code [0, a.length)}; or if any index {@code k} is not within the
     * sub-range {@code [fromIndex, toIndex)}
     */
    public static void select(double[] a, int fromIndex, int toIndex, int[] k) {
        IndexSupport.checkFromToIndex(fromIndex, toIndex, a.length);
        IndexSupport.checkIndices(fromIndex, toIndex, k);
        doSelect(a, fromIndex, toIndex, k);
    }

    /**
     * Partition the array such that index {@code k} corresponds to its correctly
     * sorted value in the equivalent fully sorted array.
     *
     * <p>This method pre/post-processes the data and indices to respect the ordering
     * imposed by {@link Double#compare(double, double)}.
     *
     * @param fromIndex Index of the first element (inclusive).
     * @param toIndex Index of the last element (exclusive).
     * @param a Values.
     * @param k Index.
     */
    private static void doSelect(double[] a, int fromIndex, int toIndex, int k) {
        if (toIndex - fromIndex <= 1) {
            return;
        }
        // Sort NaN / count signed zeros.
        // Caution: This loop contributes significantly to the runtime.
        int cn = 0;
        int end = toIndex;
        for (int i = toIndex; --i >= fromIndex;) {
            final double v = a[i];
            // Count negative zeros using a sign bit check
            if (Double.doubleToRawLongBits(v) == Long.MIN_VALUE) {
                cn++;
                // Change to positive zero.
                // Data must be repaired after selection.
                a[i] = 0.0;
            } else if (v != v) {
                // Move NaN to end
                a[i] = a[--end];
                a[end] = v;
            }
        }

        // Partition
        if (end - fromIndex > 1 && k < end) {
            QuickSelect.select(a, fromIndex, end - 1, k);
        }

        // Restore signed zeros
        if (cn != 0) {
            // Use partition index below zero to fast-forward to zero as much as possible
            for (int j = a[k] < 0 ? k : -1;;) {
                if (a[++j] == 0) {
                    a[j] = -0.0;
                    if (--cn == 0) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array.
     *
     * <p>This method pre/post-processes the data and indices to respect the ordering
     * imposed by {@link Double#compare(double, double)}.
     *
     * @param fromIndex Index of the first element (inclusive).
     * @param toIndex Index of the last element (exclusive).
     * @param a Values.
     * @param k Indices (may be destructively modified).
     */
    private static void doSelect(double[] a, int fromIndex, int toIndex, int[] k) {
        if (k.length == 0 || toIndex - fromIndex <= 1) {
            return;
        }
        // Sort NaN / count signed zeros.
        // Caution: This loop contributes significantly to the runtime for single indices.
        int cn = 0;
        int end = toIndex;
        for (int i = toIndex; --i >= fromIndex;) {
            final double v = a[i];
            // Count negative zeros using a sign bit check
            if (Double.doubleToRawLongBits(v) == Long.MIN_VALUE) {
                cn++;
                // Change to positive zero.
                // Data must be repaired after selection.
                a[i] = 0.0;
            } else if (v != v) {
                // Move NaN to end
                a[i] = a[--end];
                a[end] = v;
            }
        }

        // Partition
        int n = 0;
        if (end - fromIndex > 1) {
            n = k.length;
            // Filter indices invalidated by NaN check
            if (end < toIndex) {
                for (int i = n; --i >= 0;) {
                    final int index = k[i];
                    if (index >= end) {
                        // Move to end
                        k[i] = k[--n];
                        k[n] = index;
                    }
                }
            }
            // Return n, the count of used indices in k.
            // Use this to post-process zeros.
            n = QuickSelect.select(a, fromIndex, end - 1, k, n);
        }

        // Restore signed zeros
        if (cn != 0) {
            // Use partition indices below zero to fast-forward to zero as much as possible
            int j = -1;
            if (n < 0) {
                // Binary search on -n sorted indices: hi = (-n) - 1
                int lo = 0;
                int hi = ~n;
                while (lo <= hi) {
                    final int mid = (lo + hi) >>> 1;
                    if (a[k[mid]] < 0) {
                        j = mid;
                        lo = mid + 1;
                    } else {
                        hi = mid - 1;
                    }
                }
            } else {
                // Unsorted, process all indices
                for (int i = n; --i >= 0;) {
                    if (a[k[i]] < 0) {
                        j = k[i];
                    }
                }
            }
            for (;;) {
                if (a[++j] == 0) {
                    a[j] = -0.0;
                    if (--cn == 0) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * Partition the array such that index {@code k} corresponds to its correctly
     * sorted value in the equivalent fully sorted array.
     *
     * @param a Values.
     * @param k Index.
     * @throws IndexOutOfBoundsException if index {@code k} is not within the
     * sub-range {@code [0, a.length)}
     */
    public static void select(int[] a, int k) {
        IndexSupport.checkIndex(0, a.length, k);
        if (a.length <= 1) {
            return;
        }
        QuickSelect.select(a, 0, a.length - 1, k);
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array.
     *
     * @param a Values.
     * @param k Indices (may be destructively modified).
     * @throws IndexOutOfBoundsException if any index {@code k} is not within the
     * sub-range {@code [0, a.length)}
     */
    public static void select(int[] a, int[] k) {
        IndexSupport.checkIndices(0, a.length, k);
        if (k.length == 0 || a.length <= 1) {
            return;
        }
        QuickSelect.select(a, 0, a.length - 1, k, k.length);
    }

    /**
     * Partition the array such that index {@code k} corresponds to its correctly
     * sorted value in the equivalent fully sorted array.
     *
     * @param a Values.
     * @param fromIndex Index of the first element (inclusive).
     * @param toIndex Index of the last element (exclusive).
     * @param k Index.
     * @throws IndexOutOfBoundsException if the sub-range {@code [fromIndex, toIndex)} is out of
     * bounds of range {@code [0, a.length)}; or if index {@code k} is not within the
     * sub-range {@code [fromIndex, toIndex)}
     */
    public static void select(int[] a, int fromIndex, int toIndex, int k) {
        IndexSupport.checkFromToIndex(fromIndex, toIndex, a.length);
        IndexSupport.checkIndex(fromIndex, toIndex, k);
        if (toIndex - fromIndex <= 1) {
            return;
        }
        QuickSelect.select(a, fromIndex, toIndex - 1, k);
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array.
     *
     * @param a Values.
     * @param fromIndex Index of the first element (inclusive).
     * @param toIndex Index of the last element (exclusive).
     * @param k Indices (may be destructively modified).
     * @throws IndexOutOfBoundsException if the sub-range {@code [fromIndex, toIndex)} is out of
     * bounds of range {@code [0, a.length)}; or if any index {@code k} is not within the
     * sub-range {@code [fromIndex, toIndex)}
     */
    public static void select(int[] a, int fromIndex, int toIndex, int[] k) {
        IndexSupport.checkFromToIndex(fromIndex, toIndex, a.length);
        IndexSupport.checkIndices(fromIndex, toIndex, k);
        if (k.length == 0 || toIndex - fromIndex <= 1) {
            return;
        }
        QuickSelect.select(a, fromIndex, toIndex - 1, k, k.length);
    }
}
