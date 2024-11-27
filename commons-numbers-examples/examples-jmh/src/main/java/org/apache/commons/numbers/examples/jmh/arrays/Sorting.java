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

/**
 * Support class for sorting arrays.
 *
 * <p>Optimal sorting networks are used for small fixed size array sorting.
 *
 * <p>Note: Requires that the floating-point data contains no NaN values; sorting
 * does not respect the order of signed zeros imposed by {@link Double#compare(double, double)}.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Sorting_network">Sorting network (Wikipedia)</a>
 * @see <a href="https://bertdobbelaere.github.io/sorting_networks.html">Sorting Networks (Bert Dobbelaere)</a>
 * @since 1.2
 */
final class Sorting {
    /** The upper threshold to use a modified insertion sort to find unique indices. */
    private static final int UNIQUE_INSERTION_SORT = 20;

    /** No instances. */
    private Sorting() {}

    /**
     * Sorts an array using an insertion sort.
     *
     * <p>This method is fast up to approximately 40 - 80 values.
     *
     * <p>The {@code internal} flag indicates that the value at {@code data[begin - 1]}
     * is sorted.
     *
     * @param data Data array.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param internal Internal flag.
     */
    static void sort(double[] data, int left, int right, boolean internal) {
        if (internal) {
            // Assume data[begin - 1] is a pivot and acts as a sentinal on the range.
            // => no requirement to check j >= left.

            // Note:
            // Benchmarking fails to show that this is faster
            // even though it is the same method with fewer instructions.
            // There may be an issue with the benchmarking data, or noise in the timings.

            // There are also paired-insertion sort methods for internal regions
            // which benchmark as slower on random data.
            // On structured data with many ascending runs they are faster.

            for (int i = left; ++i <= right;) {
                final double v = data[i];
                // Move preceding higher elements above (if required)
                if (v < data[i - 1]) {
                    int j = i;
                    while (v < data[--j]) {
                        data[j + 1] = data[j];
                    }
                    data[j + 1] = v;
                }
            }
        } else {
            for (int i = left; ++i <= right;) {
                final double v = data[i];
                // Move preceding higher elements above (if required)
                if (v < data[i - 1]) {
                    int j = i;
                    while (--j >= left && v < data[j]) {
                        data[j + 1] = data[j];
                    }
                    data[j + 1] = v;
                }
            }
        }
    }

    /**
     * Sorts an array using an insertion sort.
     *
     * <p>This method is fast up to approximately 40 - 80 values.
     *
     * @param data Data array.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     */
    static void sort(double[] data, int left, int right) {
        for (int i = left; ++i <= right;) {
            final double v = data[i];
            // Move preceding higher elements above (if required)
            if (v < data[i - 1]) {
                int j = i;
                while (--j >= left && v < data[j]) {
                    data[j + 1] = data[j];
                }
                data[j + 1] = v;
            }
        }
    }

    /**
     * Sorts an array using an insertion sort.
     *
     * <p>This method is fast up to approximately 40 - 80 values.
     *
     * @param data Data array.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     */
    static void sortb(double[] data, int left, int right) {
        for (int i = left; ++i <= right;) {
            final double v = data[i];
            // Move preceding higher elements above.
            // This method always uses a loop. It benchmarks slower than the
            // method that uses an if statement to check the loop is required.
            int j = i;
            while (--j >= left && v < data[j]) {
                data[j + 1] = data[j];
            }
            data[j + 1] = v;
        }
    }

    /**
     * Sorts an array using a paired insertion sort.
     *
     * <p>Warning: It is assumed that the value at {@code data[begin - 1]} is sorted.
     *
     * @param data Data array.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     */
    static void sortPairedInternal1(double[] data, int left, int right) {
        // Assume data[begin - 1] is a pivot and acts as a sentinal on the range.
        // => no requirement to check j >= left.

        // Paired insertion sort. Move largest of two elements down the array.
        // When inserted move the smallest of the two elements down the rest of the array.

        // Pairs require an even length so start at left for even or left + 1 for odd.
        // This will do nothing when right <= left.

        // Using one index which requires i += 2.
        for (int i = left + ((right - left + 1) & 0x1); i < right; i += 2) {
            double v1 = data[i];
            double v2 = data[i + 1];
            // Sort the pair
            if (v2 < v1) {
                v1 = v2;
                v2 = data[i];
            }
            // Move preceding higher elements above the largest value
            int j = i;
            while (v2 < data[--j]) {
                data[j + 2] = data[j];
            }
            // Insert at j + 2. Update j for the next scan down.
            data[++j + 1] = v2;
            // Move preceding higher elements above the smallest value
            while (v1 < data[--j]) {
                data[j + 1] = data[j];
            }
            data[j + 1] = v1;
        }
    }

    /**
     * Sorts an array using a paired insertion sort.
     *
     * <p>Warning: It is assumed that the value at {@code data[begin - 1]} is sorted.
     *
     * @param data Data array.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     */
    static void sortPairedInternal2(double[] data, int left, int right) {
        // Assume data[begin - 1] is a pivot and acts as a sentinal on the range.
        // => no requirement to check j >= left.

        // Paired insertion sort. Move largest of two elements down the array.
        // When inserted move the smallest of the two elements down the rest of the array.

        // Pairs require an even length so start at left for even or left + 1 for odd.
        // This will do nothing when right <= left.

        // Use pair (i, j)
        for (int i = left + ((right - left + 1) & 0x1), j = i; ++j <= right; i = ++j) {
            double v1 = data[i];
            double v2 = data[j];
            // Sort the pair
            if (v2 < v1) {
                v1 = v2;
                v2 = data[i];
            }
            // Move preceding higher elements above the largest value
            while (v2 < data[--i]) {
                data[i + 2] = data[i];
            }
            // Insert at i + 2. Update i for the next scan down.
            data[++i + 1] = v2;
            // Move preceding higher elements above the smallest value
            while (v1 < data[--i]) {
                data[i + 1] = data[i];
            }
            data[i + 1] = v1;
        }
    }

    /**
     * Sorts an array using a paired insertion sort.
     *
     * <p>Warning: It is assumed that the value at {@code data[begin - 1]} is sorted.
     *
     * @param data Data array.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     */
    static void sortPairedInternal3(double[] data, int left, int right) {
        // Assume data[begin - 1] is a pivot and acts as a sentinal on the range.
        // => no requirement to check j >= left.

        // Paired insertion sort. Move largest of two elements down the array.
        // When inserted move the smallest of the two elements down the rest of the array.

        // Pairs require an even length so start at left for even or left + 1 for odd.
        // This will do nothing when right <= left.

        // As above but only move if required
        for (int i = left + ((right - left + 1) & 0x1), j = i; ++j <= right; i = ++j) {
            double v1 = data[i];
            double v2 = data[j];
            // Sort the pair
            if (v2 < v1) {
                v1 = v2;
                v2 = data[i];
                // In the event of no move of v2
                data[j] = v2;
            }
            // Move preceding higher elements above the largest value (if required)
            if (v2 < data[i - 1]) {
                while (v2 < data[--i]) {
                    data[i + 2] = data[i];
                }
                // Insert at i + 2. Update i for the next scan down.
                data[++i + 1] = v2;
            }
            // Move preceding higher elements above the smallest value (if required)
            if (v1 < data[i - 1]) {
                while (v1 < data[--i]) {
                    data[i + 1] = data[i];
                }
                // Insert at i+1
                i++;
            }
            // Always write v1 as v2 may have moved down
            data[i] = v1;
        }
    }

    /**
     * Sorts an array using a paired insertion sort.
     *
     * <p>Warning: It is assumed that the value at {@code data[begin - 1]} is sorted.
     *
     * @param data Data array.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     */
    static void sortPairedInternal4(double[] data, int left, int right) {
        // Assume data[begin - 1] is a pivot and acts as a sentinal on the range.
        // => no requirement to check j >= left.

        // Paired insertion sort. Move largest of two elements down the array.
        // When inserted move the smallest of the two elements down the rest of the array.

        // Pairs require an even length so start at left for even or left + 1 for odd.
        // This will do nothing when right <= left.

        // As above but only move if required
        for (int i = left + ((right - left + 1) & 0x1), j = i; ++j <= right; i = ++j) {
            double v1 = data[i];
            double v2 = data[j];
            // Sort the pair
            if (v2 < v1) {
                v1 = v2;
                v2 = data[i];
                // In the event of no moves
                data[j] = v2;
                data[i] = v1;
            }
            // Move preceding higher elements (if required, only test the smallest)
            if (v1 < data[i - 1]) {
                // Move preceding higher elements above the largest value
                while (v2 < data[--i]) {
                    data[i + 2] = data[i];
                }
                // Insert at i + 2. Update i for the next scan down.
                data[++i + 1] = v2;
                // Move preceding higher elements above the smallest value
                while (v1 < data[--i]) {
                    data[i + 1] = data[i];
                }
                data[i + 1] = v1;
            }
        }
    }

    /**
     * Place the minimum of 3 elements in {@code a}; and the larger
     * two elements in {@code b, c}.
     *
     * @param x Values
     * @param a Index.
     * @param b Index.
     * @param c Index.
     */
    static void min3(double[] x, int a, int b, int c) {
        if (x[b] < x[a]) {
            final double v = x[b];
            x[b] = x[a];
            x[a] = v;
        }
        if (x[c] < x[a]) {
            final double v = x[c];
            x[c] = x[a];
            x[a] = v;
        }
    }

    /**
     * Place the maximum of 3 elements in {@code c}; and the smaller
     * two elements in {@code a, b}.
     *
     * @param x Values
     * @param a Index.
     * @param b Index.
     * @param c Index.
     */
    static void max3(double[] x, int a, int b, int c) {
        if (x[c] < x[b]) {
            final double u = x[c];
            x[c] = x[b];
            x[b] = u;
        }
        if (x[c] < x[a]) {
            final double v = x[c];
            x[c] = x[a];
            x[a] = v;
        }
    }

    /**
     * Sorts the given indices in an array.
     *
     * <p>Assumes all indices are valid and distinct.
     *
     * <p>Data are arranged such that:
     * <pre>{@code
     * i0 != i1 != i2
     * data[i0] < data[i1] < data[i2]
     * }</pre>
     *
     * <p>If indices are duplicated elements will <em>not</em> be correctly ordered.
     * However in this case data will contain the same values and may be partially ordered.
     *
     * @param data Data array.
     * @param i0 Index.
     * @param i1 Index.
     * @param i2 Index.
     */
    static void sort3(double[] data, int i0, int i1, int i2) {
        // Decision tree avoiding swaps:
        // Order [(0,2)]
        // Move point 1 above point 2 or below point 0
        final double x = data[i0];
        final double y = data[i1];
        final double z = data[i2];
        if (z < x) {
            if (y < z) {
                data[i0] = y;
                data[i1] = z;
                data[i2] = x;
                return;
            }
            if (x < y) {
                data[i0] = z;
                data[i1] = x;
                data[i2] = y;
                return;
            }
            // z < y < z
            data[i0] = z;
            data[i2] = x;
            return;
        }
        if (y < x) {
            // y < x < z
            data[i0] = y;
            data[i1] = x;
            return;
        }
        if (z < y) {
            // x < z < y
            data[i1] = z;
            data[i2] = y;
        }
        // x < y < z
    }

    /**
     * Sorts the given indices in an array.
     *
     * <p>Note: Requires that the range contains no NaN values. It does not respect the
     * order of signed zeros.
     *
     * <p>Assumes all indices are valid and distinct.
     *
     * <p>Data are arranged such that:
     * <pre>{@code
     * a != b != c
     * data[a] < data[b] < data[c]
     * }</pre>
     *
     * <p>If indices are duplicated elements will <em>not</em> be correctly ordered.
     * However in this case data will contain the same values and may be partially ordered.
     *
     * @param data Data array.
     * @param i0 Index.
     * @param i1 Index.
     * @param i2 Index.
     */
    static void sort3b(double[] data, int i0, int i1, int i2) {
        // Order pair:
        //[(0,2)]
        // Move point 1 above point 2 or below point 0
        if (data[i2] < data[i0]) {
            final double v = data[i2];
            data[i2] = data[i0];
            data[i0] = v;
        }
        if (data[i2] < data[i1]) {
            final double v = data[i2];
            data[i2] = data[i1];
            data[i1] = v;
        } else if (data[i1] < data[i0]) {
            final double v = data[i1];
            data[i1] = data[i0];
            data[i0] = v;
        }
    }

    /**
     * Sorts the given indices in an array.
     *
     * <p>Note: Requires that the range contains no NaN values. It does not respect the
     * order of signed zeros.
     *
     * <p>Assumes all indices are valid and distinct.
     *
     * <p>Data are arranged such that:
     *
     * <pre>{@code
     * a != b != c
     * data[a] < data[b] < data[c]
     * }</pre>
     *
     * <p>If indices are duplicated elements will <em>not</em> be correctly ordered.
     * However in this case data will contain the same values and may be partially
     * ordered.
     *
     * @param data Data array.
     * @param i0 Index.
     * @param i1 Index.
     * @param i2 Index.
     */
    static void sort3c(double[] data, int i0, int i1, int i2) {
        // Order pairs:
        // [(0,2)]
        // [(0,1)]
        // [(1,2)]
        if (data[i2] < data[i0]) {
            final double v = data[i2];
            data[i2] = data[i0];
            data[i0] = v;
        }
        if (data[i1] < data[i0]) {
            final double v = data[i1];
            data[i1] = data[i0];
            data[i0] = v;
        }
        if (data[i2] < data[i1]) {
            final double v = data[i2];
            data[i2] = data[i1];
            data[i1] = v;
        }
    }

    /**
     * Sorts the given indices in an array using an insertion sort.
     *
     * <p>Assumes all indices are valid and distinct.
     *
     * <p>Data are arranged such that:
     * <pre>{@code
     * i0 != i1 != i2 != i3
     * data[i0] < data[i1] < data[i2] < data[i3]
     * }</pre>
     *
     * <p>If indices are duplicated elements will <em>not</em> be correctly ordered.
     * However in this case data will contain the same values and may be partially ordered.
     *
     * @param data Data array.
     * @param i0 Index.
     * @param i1 Index.
     * @param i2 Index.
     * @param i3 Index.
     */
    static void sort4(double[] data, int i0, int i1, int i2, int i3) {
        // Uses an optimal sorting network from Knuth's Art of Computer Programming.
        // 5 comparisons.
        // Order pairs:
        //[(0,2),(1,3)]
        //[(0,1),(2,3)]
        //[(1,2)]
        if (data[i3] < data[i1]) {
            final double u = data[i3];
            data[i3] = data[i1];
            data[i1] = u;
        }
        if (data[i2] < data[i0]) {
            final double v = data[i2];
            data[i2] = data[i0];
            data[i0] = v;
        }

        if (data[i3] < data[i2]) {
            final double u = data[i3];
            data[i3] = data[i2];
            data[i2] = u;
        }
        if (data[i1] < data[i0]) {
            final double v = data[i1];
            data[i1] = data[i0];
            data[i0] = v;
        }

        if (data[i2] < data[i1]) {
            final double u = data[i2];
            data[i2] = data[i1];
            data[i1] = u;
        }
    }

    /**
     * Sorts the given indices in an array.
     *
     * <p>Assumes all indices are valid and distinct.
     *
     * <p>Data are arranged such that:
     * <pre>{@code
     * i0 != i1 != i2 != i3 != i4
     * data[i0] < data[i1] < data[i2] < data[i3] < data[i4]
     * }</pre>
     *
     * <p>If indices are duplicated elements will <em>not</em> be correctly ordered.
     * However in this case data will contain the same values and may be partially ordered.
     *
     * @param data Data array.
     * @param i0 Index.
     * @param i1 Index.
     * @param i2 Index.
     * @param i3 Index.
     * @param i4 Index.
     */
    static void sort5(double[] data, int i0, int i1, int i2, int i3, int i4) {
        // Uses an optimal sorting network from Knuth's Art of Computer Programming.
        // 9 comparisons.
        // Order pairs:
        // [(0,3),(1,4)]
        // [(0,2),(1,3)]
        // [(0,1),(2,4)]
        // [(1,2),(3,4)]
        // [(2,3)]
        if (data[i4] < data[i1]) {
            final double u = data[i4];
            data[i4] = data[i1];
            data[i1] = u;
        }
        if (data[i3] < data[i0]) {
            final double v = data[i3];
            data[i3] = data[i0];
            data[i0] = v;
        }

        if (data[i3] < data[i1]) {
            final double u = data[i3];
            data[i3] = data[i1];
            data[i1] = u;
        }
        if (data[i2] < data[i0]) {
            final double v = data[i2];
            data[i2] = data[i0];
            data[i0] = v;
        }

        if (data[i4] < data[i2]) {
            final double u = data[i4];
            data[i4] = data[i2];
            data[i2] = u;
        }
        if (data[i1] < data[i0]) {
            final double v = data[i1];
            data[i1] = data[i0];
            data[i0] = v;
        }

        if (data[i4] < data[i3]) {
            final double u = data[i4];
            data[i4] = data[i3];
            data[i3] = u;
        }
        if (data[i2] < data[i1]) {
            final double v = data[i2];
            data[i2] = data[i1];
            data[i1] = v;
        }

        if (data[i3] < data[i2]) {
            final double u = data[i3];
            data[i3] = data[i2];
            data[i2] = u;
        }
    }

    /**
     * Sorts the given indices in an array.
     *
     * <p>Assumes all indices are valid and distinct.
     *
     * <p>Data are arranged such that:
     * <pre>{@code
     * i0 != i1 != i2 != i3 != i4
     * data[i0] < data[i1] < data[i2] < data[i3] < data[i4]
     * }</pre>
     *
     * <p>If indices are duplicated elements will <em>not</em> be correctly ordered.
     * However in this case data will contain the same values and may be partially ordered.
     *
     * @param data Data array.
     * @param i0 Index.
     * @param i1 Index.
     * @param i2 Index.
     * @param i3 Index.
     * @param i4 Index.
     */
    static void sort5b(double[] data, int i0, int i1, int i2, int i3, int i4) {
        // Sorting network for size 5 is 9 comparisons (see sort5).
        // Sorting network for size 4 is 5 comparisons + 2 or 3 extra.
        // This method benchmarks marginally faster (~1%) than the sorting network of size 5
        // on length 5 data. When the data is larger and the indices are uniformly
        // spread across the range, the sorting network is faster.

        // Order quadruple:
        //[(0,1,3,4)]
        // Move point 2 above points 3,4 or below points 0,1
        sort4(data, i0, i1, i3, i4);
        final double u = data[i2];
        if (u > data[i3]) {
            data[i2] = data[i3];
            data[i3] = u;
            if (u > data[i4]) {
                data[i3] = data[i4];
                data[i4] = u;
            }
        } else if (u < data[i1]) {
            data[i2] = data[i1];
            data[i1] = u;
            if (u < data[i0]) {
                data[i1] = data[i0];
                data[i0] = u;
            }
        }
    }

    /**
     * Sorts the given indices in an array.
     *
     * <p>Assumes all indices are valid and distinct.
     *
     * <p>Data are arranged such that:
     * <pre>{@code
     * i0 != i1 != i2 != i3 != i4
     * data[i0] < data[i1] < data[i2] < data[i3] < data[i4]
     * }</pre>
     *
     * <p>If indices are duplicated elements will <em>not</em> be correctly ordered.
     * However in this case data will contain the same values and may be partially ordered.
     *
     * @param data Data array.
     * @param i0 Index.
     * @param i1 Index.
     * @param i2 Index.
     * @param i3 Index.
     * @param i4 Index.
     */
    static void sort5c(double[] data, int i0, int i1, int i2, int i3, int i4) {
        // Sorting of 5 elements in optimum 7 comparisons.
        // Code adapted from Raphael, Computer Science Stack Exchange.
        // https://cs.stackexchange.com/a/44982
        // https://gist.github.com/akerbos/5acb345ff3d41bc888c4

        // 1. Sort the first two pairs.
        if (data[i1] < data[i0]) {
            final double u = data[i1];
            data[i1] = data[i0];
            data[i0] = u;
        }
        if (data[i3] < data[i2]) {
            final double v = data[i3];
            data[i3] = data[i2];
            data[i2] = v;
        }

        // 2. Order the pairs w.r.t. their respective larger element.
        // Call the result [a,b,c,d,e]; we know a<b<d and c<d.
        if (data[i3] < data[i1]) {
            final double u = data[i0];
            final double v = data[i1];
            data[i0] = data[i2];
            data[i1] = data[i3];
            data[i2] = u;
            data[i3] = v;
        }

        // 3. Insert e into [a,b,d]
        final double e = data[i4];
        if (e < data[i1]) {
            if (e < data[i0]) {
                // e,a,b,d
                data[i4] = data[i3];
                data[i3] = data[i1];
                data[i1] = data[i0];
                data[i0] = e;
            } else {
                // a,e,b,d
                data[i4] = data[i3];
                data[i3] = data[i1];
                data[i1] = e;
            }
        } else {
            if (data[i4] < data[i3]) {
                // a,b,e,d
                data[i4] = data[i3];
                data[i3] = e;
            }
            // else a,b,d,e (already sorted)
        }

        // 4. Insert c into the first 3 elements of result of step 3.
        final double c = data[i2];
        if (c < data[i1]) {
            if (c < data[i0]) {
                data[i2] = data[i1];
                data[i1] = data[i0];
                data[i0] = c;
            } else {
                data[i2] = data[i1];
                data[i1] = c;
            }
        } else {
            if (data[i3] < c) {
                data[i2] = data[i3];
                data[i3] = c;
            }
            // else already sorted
        }
    }

    /**
     * Place the lower median of 4 elements in {@code b}; the smaller element in
     * {@code a}; and the larger two elements in {@code c, d}.
     *
     * @param x Values
     * @param a Index.
     * @param b Index.
     * @param c Index.
     * @param d Index.
     */
    static void lowerMedian4(double[] x, int a, int b, int c, int d) {
        // 3, 4, 5 comparisons
        // ----
        // Note: Performance testing shows this is faster than methods
        // that only require exactly 4 comparisons on both random data and the
        // Bentley & McIlroy (1993) data. It may be that the branch order
        // is such that it is more predictable. Branch frequency statistics
        // are shown for:
        // - all possible permutations of unique values (4! = 24);
        // - B&M data of length 1023-1025;
        // - Uniform random data of length 10000.
        // Note that on random data this completes in 3 comparisons with frequency ~1/6.
        // and hits the branch requiring 5 comparisons with frequency 0.498360. Thus
        // it requires 4 comparisons 1/3 of the time. On the B&M test data the
        // frequency for the branch with 5 comparisons is lower at 0.291995 and 3
        // comparisons much lower at 0.047813. Combined this leaves approximately 2/3
        // of the time taking 4 comparisons.
        // ----
        if (x[d] < x[b]) {
            // 0.25 : 0.263033 : 0.500960
            final double u = x[d];
            x[d] = x[b];
            x[b] = u;
        }
        if (x[c] < x[a]) {
            // 0.25 : 0.253122 : 0.494720
            final double v = x[c];
            x[c] = x[a];
            x[a] = v;
        }
        // a--c
        // b--d
        if (x[c] < x[b]) {
            // 0.083333 : 0.047813 : 0.165960
            final double u = x[c];
            x[c] = x[b];
            x[b] = u;
        } else if (x[b] < x[a]) {
            // 0.25 : 0.291995 : 0.498360
            //    a--c
            // b--d
            final double xb = x[a];
            x[a] = x[b];
            x[b] = xb;
            //    b--c
            // a--d
            if (x[d] < xb) {
                // 0.25 : 0.038541 : 0.164240
                x[b] = x[d];
                // Move a pair to maintain the sorted order
                //x[d] = xb;
                x[d] = x[c];
                x[c] = xb;
            }
        }
    }

    /**
     * Place the lower median of 4 elements in {@code b}; the smaller element in
     * {@code a}; and the larger two elements in {@code c, d}.
     *
     * @param x Values
     * @param a Index.
     * @param b Index.
     * @param c Index.
     * @param d Index.
     */
    static void lowerMedian4b(double[] x, int a, int b, int c, int d) {
        // 3 or 5 comparisons
        // Set min of [b, c, d]
        if (x[d] < x[b]) {
            final double u = x[d];
            x[d] = x[b];
            x[b] = u;
        }
        if (x[c] < x[b]) {
            final double v = x[c];
            x[c] = x[b];
            x[b] = v;
        }
        // b < {c,d}
        if (x[b] < x[a]) {
            final double w = x[b];
            x[b] = x[a];
            x[a] = w;
            // a < {b,c,d}
            // Set min of [b, c, d]
            if (x[d] < x[b]) {
                final double u = x[d];
                x[d] = x[b];
                x[b] = u;
            }
            if (x[c] < x[b]) {
                final double v = x[c];
                x[c] = x[b];
                x[b] = v;
            }
        }
    }

    /**
     * Place the lower median of 4 elements in {@code b}; the smaller element in
     * {@code a}; and the larger two elements in {@code c, d}.
     *
     * @param x Values
     * @param a Index.
     * @param b Index.
     * @param c Index.
     * @param d Index.
     */
    static void lowerMedian4c(double[] x, int a, int b, int c, int d) {
        // 4 comparisons
        if (x[d] < x[b]) {
            final double u = x[d];
            x[d] = x[b];
            x[b] = u;
        }
        if (x[c] < x[a]) {
            final double v = x[c];
            x[c] = x[a];
            x[a] = v;
        }
        // a--c
        // b--d
        if (x[b] < x[a]) {
            final double v = x[a];
            final double u = x[c];
            x[a] = x[b];
            x[c] = x[d];
            x[b] = v;
            x[d] = u;
        }
        // a--c
        //    b--d
        if (x[c] < x[b]) {
            final double u = x[c];
            x[c] = x[b];
            x[b] = u;
        }
    }

    /**
     * Place the lower median of 4 elements in {@code b}; the smaller element in
     * {@code a}; and the larger two elements in {@code c, d}.
     *
     * @param x Values
     * @param a Index.
     * @param b Index.
     * @param c Index.
     * @param d Index.
     */
    static void lowerMedian4d(double[] x, int a, int b, int c, int d) {
        // 4 comparisons
        if (x[d] < x[a]) {
            final double u = x[d];
            x[d] = x[a];
            x[a] = u;
        }
        if (x[c] < x[b]) {
            final double v = x[c];
            x[c] = x[b];
            x[b] = v;
        }
        // a--d
        // b--c
        if (x[b] < x[a]) {
            final double xb = x[a];
            x[a] = x[b];
            x[b] = xb;
            //    b--d
            // a--c
            if (x[c] < xb) {
                x[b] = x[c];
                x[c] = xb;
                // fully sorted here
            }
            // else full sort requires c:d ordering
            // Not fully sorted for 6 of 24 permutations
        } else if (x[d] < x[b]) {
            // a--d
            //       b--c
            final double v = x[d];
            // Do a full sort for 1 additional swap
            x[d] = x[c];
            x[c] = x[b];
            x[b] = v;
            // minimum swaps to put the lower median at b
            //x[d] = x[b];
            //x[b] = v;
        }
    }

    /**
     * Place the lower median of 4 elements in {@code b}; the smaller element in
     * {@code a}; and the larger two elements in {@code c, d}.
     *
     * @param x Values
     * @param a Index.
     * @param b Index.
     * @param c Index.
     * @param d Index.
     */
    static void lowerMedian4e(double[] x, int a, int b, int c, int d) {
        // 4 comparisons
        if (x[d] < x[b]) {
            final double u = x[d];
            x[d] = x[b];
            x[b] = u;
        }
        if (x[c] < x[a]) {
            final double v = x[c];
            x[c] = x[a];
            x[a] = v;
        }
        // a--c
        // b--d
        if (x[b] < x[a]) {
            final double xb = x[a];
            x[a] = x[b];
            x[b] = xb;
            // a--b--c
            //    d
            if (x[d] < xb) {
                x[b] = x[d];
                // Move a pair to maintain the sorted order
                //x[d] = xb;
                x[d] = x[c];
                x[c] = xb;
            }
        // a--c
        //    b--d
        } else if (x[c] < x[b]) {
            final double u = x[c];
            x[c] = x[b];
            x[b] = u;
        }
    }

    /**
     * Place the upper median of 4 elements in {@code c}; the smaller two elements in
     * {@code a,b}; and the larger element in {@code d}.
     *
     * @param x Values
     * @param a Index.
     * @param b Index.
     * @param c Index.
     * @param d Index.
     */
    static void upperMedian4(double[] x, int a, int b, int c, int d) {
        // 3, 4, 5 comparisons
        if (x[d] < x[b]) {
            final double u = x[d];
            x[d] = x[b];
            x[b] = u;
        }
        if (x[c] < x[a]) {
            final double v = x[c];
            x[c] = x[a];
            x[a] = v;
        }
        // a--c
        // b--d
        if (x[b] > x[c]) {
            final double u = x[c];
            x[c] = x[b];
            x[b] = u;
        } else if (x[c] > x[d]) {
            //    a--c
            // b--d
            final double xc = x[d];
            x[d] = x[c];
            x[c] = xc;
            //    a--d
            // b--c
            if (x[a] > xc) {
                x[c] = x[a];
                // Move a pair to maintain the sorted order
                //x[a] = xc;
                x[a] = x[b];
                x[b] = xc;
            }
        }
    }

    /**
     * Place the upper median of 4 elements in {@code c}; the smaller two elements in
     * {@code a,b}; and the larger element in {@code d}.
     *
     * @param x Values
     * @param a Index.
     * @param b Index.
     * @param c Index.
     * @param d Index.
     */
    static void upperMedian4c(double[] x, int a, int b, int c, int d) {
        // 4 comparisons
        if (x[d] < x[b]) {
            final double u = x[d];
            x[d] = x[b];
            x[b] = u;
        }
        if (x[c] < x[a]) {
            final double v = x[c];
            x[c] = x[a];
            x[a] = v;
        }
        // a--c
        // b--d
        if (x[d] < x[c]) {
            final double v = x[a];
            final double u = x[c];
            x[a] = x[b];
            x[c] = x[d];
            x[b] = v;
            x[d] = u;
        }
        // a--c
        //    b--d
        if (x[c] < x[b]) {
            final double u = x[c];
            x[c] = x[b];
            x[b] = u;
        }
    }

    /**
     * Place the upper median of 4 elements in {@code c}; the smaller two elements in
     * {@code a,b}; and the larger element in {@code d}.
     *
     * @param x Values
     * @param a Index.
     * @param b Index.
     * @param c Index.
     * @param d Index.
     */
    static void upperMedian4d(double[] x, int a, int b, int c, int d) {
        // 4 comparisons
        if (x[d] < x[a]) {
            final double u = x[d];
            x[d] = x[a];
            x[a] = u;
        }
        if (x[c] < x[b]) {
            final double v = x[c];
            x[c] = x[b];
            x[b] = v;
        }
        // a--d
        // b--c
        if (x[d] < x[c]) {
            final double xc = x[d];
            x[d] = x[c];
            x[c] = xc;
            // a--c
            //    b--d
            if (xc < x[b]) {
                x[c] = x[b];
                x[b] = xc;
                // fully sorted here
            }
            // else full sort requires a:b ordering
            // Not fully sorted for 6 of 24 permutations
        } else if (x[c] < x[a]) {
            //       a--d
            // b--c
            final double v = x[a];
            // Do a full sort for 1 additional swap
            x[a] = x[b];
            x[b] = x[c];
            x[c] = v;
            // minimum swaps to put the lower median at b
            //x[d] = x[b];
            //x[b] = v;
        }
    }

    /**
     * Return the median of a continuous block of 5 elements.
     * Data may be partially reordered.
     *
     * @param a Values
     * @param i1 First index.
     * @return the median index
     */
    static int median5(double[] a, int i1) {
        final int i2 = i1 + 1;
        final int i3 = i1 + 2;
        final int i4 = i1 + 3;
        final int i5 = i1 + 4;
        // 6 comparison decision tree
        // Possible median in parentheses
        // (12345)
        if (a[i2] < a[i1]) {
            final double v = a[i2];
            a[i2] = a[i1];
            a[i1] = v;
        }
        if (a[i4] < a[i3]) {
            final double v = a[i4];
            a[i4] = a[i3];
            a[i3] = v;
        }
        // (1<2 3<4 5)
        if (a[i1] < a[i3]) {
            // 1(2 3<4 5)
            if (a[i5] < a[i2]) {
                final double v = a[i5];
                a[i5] = a[i2];
                a[i2] = v;
            }
            // 1(2<5 3<4)
            if (a[i2] < a[i3]) {
                // 1,2(5 3<4)
                return a[i5] < a[i3] ? i5 : i3;
            }
            // 1,3(2<5 4)
            return a[i2] < a[i4] ? i2 : i4;
        }
        // 3(1<2 4 5)
        if (a[i5] < a[i4]) {
            final double v = a[i5];
            a[i5] = a[i4];
            a[i4] = v;
        }
        // 3(1<2 4<5)
        if (a[i1] < a[i4]) {
            // 3,1(2 4<5)
            return a[i2] < a[i4] ? i2 : i4;
        }
        // 3,4(1<2 5)
        return a[i1] < a[i5] ? i1 : i5;
    }

    /**
     * Return the median of 5 elements. Data may be partially reordered.
     *
     * @param a Values
     * @param i1 Index.
     * @param i2 Index.
     * @param i3 Index.
     * @param i4 Index.
     * @param i5 Index.
     * @return the median index
     */
    static int median5(double[] a, int i1, int i2, int i3, int i4, int i5) {
        // 6 comparison decision tree
        // Possible median in parentheses
        // (12345)
        if (a[i2] < a[i1]) {
            final double v = a[i2];
            a[i2] = a[i1];
            a[i1] = v;
        }
        if (a[i4] < a[i3]) {
            final double v = a[i4];
            a[i4] = a[i3];
            a[i3] = v;
        }
        // (1<2 3<4 5)
        if (a[i1] < a[i3]) {
            // 1(2 3<4 5)
            if (a[i5] < a[i2]) {
                final double v = a[i5];
                a[i5] = a[i2];
                a[i2] = v;
            }
            // 1(2<5 3<4)
            if (a[i2] < a[i3]) {
                // 1,2(5 3<4)
                return a[i5] < a[i3] ? i5 : i3;
            }
            // 1,3(2<5 4)
            return a[i2] < a[i4] ? i2 : i4;
        }
        // 3(1<2 4 5)
        if (a[i5] < a[i4]) {
            final double v = a[i5];
            a[i5] = a[i4];
            a[i4] = v;
        }
        // 3(1<2 4<5)
        if (a[i1] < a[i4]) {
            // 3,1(2 4<5)
            return a[i2] < a[i4] ? i2 : i4;
        }
        // 3,4(1<2 5)
        return a[i1] < a[i5] ? i1 : i5;
    }

    /**
     * Return the median of a continuous block of 5 elements.
     * Data may be partially reordered.
     *
     * @param a Values
     * @param i1 First index.
     * @return the median index
     */
    static int median5b(double[] a, int i1) {
        final int i2 = i1 + 1;
        final int i3 = i1 + 2;
        final int i4 = i1 + 3;
        final int i5 = i1 + 4;
        // 6 comparison decision tree
        // Possible median in parentheses
        // (12345)
        if (a[i2] < a[i1]) {
            final double v = a[i2];
            a[i2] = a[i1];
            a[i1] = v;
        }
        if (a[i4] < a[i3]) {
            final double v = a[i4];
            a[i4] = a[i3];
            a[i3] = v;
        }
        // (1<2 3<4 5)
        if (a[i1] < a[i3]) {
            // 1(2 3<4 5)
            if (a[i5] < a[i2]) {
                // 1(5<2 3<4)
                if (a[i5] < a[i3]) {
                    // 1,5(2 3<4)
                    return a[i2] < a[i3] ? i2 : i3;
                }
                // 1,3(2<5 4)
                return a[i5] < a[i4] ? i5 : i4;
            }
            // 1(2<5 3<4)
            if (a[i2] < a[i3]) {
                // 1,2(5 3<4)
                return a[i5] < a[i3] ? i5 : i3;
            }
            // 1,3(2<5 4)
            return a[i2] < a[i4] ? i2 : i4;
        }
        // 3(1<2 4 5)
        if (a[i5] < a[i4]) {
            // 3(1<2 5<4)
            if (a[i1] < a[i5]) {
                // 3,1(2 5<4)
                return a[i2] < a[i5] ? i2 : i5;
            }
            // 3,5(1<2 4)
            return a[i1] < a[i4] ? i1 : i4;
        }
        // 3(1<2 4<5)
        if (a[i1] < a[i4]) {
            // 3,1(2 4<5)
            return a[i2] < a[i4] ? i2 : i4;
        }
        // 3,4(1<2 5)
        return a[i1] < a[i5] ? i1 : i5;
    }

    /**
     * Return the median of a continuous block of 5 elements.
     * Data may be partially reordered.
     *
     * @param a Values
     * @param i1 First index.
     * @return the median index
     */
    static int median5c(double[] a, int i1) {
        // Sort 4
        Sorting.sort4(a, i1, i1 + 1, i1 + 3, i1 + 4);
        // median of [e-4, e-3, e-2]
        int m = i1 + 2;
        if (a[m] < a[m - 1]) {
            --m;
        } else if (a[m] > a[m + 1]) {
            ++m;
        }
        return m;
    }

    /**
     * Place the median of 5 elements in {@code c}; the smaller 2 elements in
     * {@code a, b}; and the larger two elements in {@code d, e}.
     *
     * @param x Values
     * @param a Index.
     * @param b Index.
     * @param c Index.
     * @param d Index.
     * @param e Index.
     */
    static void median5d(double[] x, int a, int b, int c, int d, int e) {
        // 6 comparison decision tree from:
        // Alexandrescu (2016) Fast Deterministic Selection, arXiv:1606.00484, Algorithm 4
        // https://arxiv.org/abs/1606.00484
        if (x[c] < x[a]) {
            final double v = x[c];
            x[c] = x[a];
            x[a] = v;
        }
        if (x[d] < x[b]) {
            final double u = x[d];
            x[d] = x[b];
            x[b] = u;
        }
        if (x[d] < x[c]) {
            final double v = x[d];
            x[d] = x[c];
            x[c] = v;
            final double u = x[b];
            x[b] = x[a];
            x[a] = u;
        }
        if (x[e] < x[b]) {
            final double v = x[e];
            x[e] = x[b];
            x[b] = v;
        }
        if (x[e] < x[c]) {
            final double u = x[e];
            x[e] = x[c];
            x[c] = u;
            if (u < x[a]) {
                x[c] = x[a];
                x[a] = u;
            }
        } else {
            if (x[c] < x[b]) {
                final double u = x[c];
                x[c] = x[b];
                x[b] = u;
            }
        }
    }

    /**
     * Sorts the given indices in an array.
     *
     * <p>Assumes all indices are valid and distinct.
     *
     * <p>Data are arranged such that:
     * <pre>{@code
     * i0 != i1 != i2 != i3 != i4 != i5 != i6
     * data[i0] < data[i1] < data[i2] < data[i3] < data[i4] < data[i5] < data[i6]
     * }</pre>
     *
     * <p>If indices are duplicated elements will <em>not</em> be correctly ordered.
     * However in this case data will contain the same values and may be partially ordered.
     *
     * @param data Data array.
     * @param i0 Index.
     * @param i1 Index.
     * @param i2 Index.
     * @param i3 Index.
     * @param i4 Index.
     * @param i5 Index.
     * @param i6 Index.
     */
    static void sort7(double[] data, int i0, int i1, int i2, int i3, int i4, int i5, int i6) {
        // Uses an optimal sorting network from Knuth's Art of Computer Programming.
        // 16 comparisons.
        // Order pairs:
        //[(0,6),(2,3),(4,5)]
        //[(0,2),(1,4),(3,6)]
        //[(0,1),(2,5),(3,4)]
        //[(1,2),(4,6)]
        //[(2,3),(4,5)]
        //[(1,2),(3,4),(5,6)]
        if (data[i5] < data[i4]) {
            final double u = data[i5];
            data[i5] = data[i4];
            data[i4] = u;
        }
        if (data[i3] < data[i2]) {
            final double v = data[i3];
            data[i3] = data[i2];
            data[i2] = v;
        }
        if (data[i6] < data[i0]) {
            final double w = data[i6];
            data[i6] = data[i0];
            data[i0] = w;
        }

        if (data[i6] < data[i3]) {
            final double u = data[i6];
            data[i6] = data[i3];
            data[i3] = u;
        }
        if (data[i4] < data[i1]) {
            final double v = data[i4];
            data[i4] = data[i1];
            data[i1] = v;
        }
        if (data[i2] < data[i0]) {
            final double w = data[i2];
            data[i2] = data[i0];
            data[i0] = w;
        }

        if (data[i4] < data[i3]) {
            final double u = data[i4];
            data[i4] = data[i3];
            data[i3] = u;
        }
        if (data[i5] < data[i2]) {
            final double v = data[i5];
            data[i5] = data[i2];
            data[i2] = v;
        }
        if (data[i1] < data[i0]) {
            final double w = data[i1];
            data[i1] = data[i0];
            data[i0] = w;
        }

        if (data[i6] < data[i4]) {
            final double u = data[i6];
            data[i6] = data[i4];
            data[i4] = u;
        }
        if (data[i2] < data[i1]) {
            final double v = data[i2];
            data[i2] = data[i1];
            data[i1] = v;
        }

        if (data[i5] < data[i4]) {
            final double u = data[i5];
            data[i5] = data[i4];
            data[i4] = u;
        }
        if (data[i3] < data[i2]) {
            final double v = data[i3];
            data[i3] = data[i2];
            data[i2] = v;
        }

        if (data[i6] < data[i5]) {
            final double u = data[i6];
            data[i6] = data[i5];
            data[i5] = u;
        }
        if (data[i4] < data[i3]) {
            final double v = data[i4];
            data[i4] = data[i3];
            data[i3] = v;
        }
        if (data[i2] < data[i1]) {
            final double w = data[i2];
            data[i2] = data[i1];
            data[i1] = w;
        }
    }

    /**
     * Sorts the given indices in an array.
     *
     * <p>Assumes all indices are valid and distinct.
     *
     * <p>Data are arranged such that:
     * <pre>{@code
     * i0 != i1 != i2 != i3 != i4 != i5 != i6 != i7
     * data[i0] < data[i1] < data[i2] < data[i3] < data[i4] < data[i5] < data[i6] < data[i7]
     * }</pre>
     *
     * <p>If indices are duplicated elements will <em>not</em> be correctly ordered.
     * However in this case data will contain the same values and may be partially ordered.
     *
     * @param data Data array.
     * @param i0 Index.
     * @param i1 Index.
     * @param i2 Index.
     * @param i3 Index.
     * @param i4 Index.
     * @param i5 Index.
     * @param i6 Index.
     * @param i7 Index.
     */
    static void sort8(double[] data, int i0, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
        // Uses an optimal sorting network from Knuth's Art of Computer Programming.
        // 19 comparisons.
        // Order pairs:
        //[(0,2),(1,3),(4,6),(5,7)]
        //[(0,4),(1,5),(2,6),(3,7)]
        //[(0,1),(2,3),(4,5),(6,7)]
        //[(2,4),(3,5)]
        //[(1,4),(3,6)]
        //[(1,2),(3,4),(5,6)]
        if (data[i7] < data[i5]) {
            final double u = data[i7];
            data[i7] = data[i5];
            data[i5] = u;
        }
        if (data[i6] < data[i4]) {
            final double v = data[i6];
            data[i6] = data[i4];
            data[i4] = v;
        }
        if (data[i3] < data[i1]) {
            final double w = data[i3];
            data[i3] = data[i1];
            data[i1] = w;
        }
        if (data[i2] < data[i0]) {
            final double x = data[i2];
            data[i2] = data[i0];
            data[i0] = x;
        }

        if (data[i7] < data[i3]) {
            final double u = data[i7];
            data[i7] = data[i3];
            data[i3] = u;
        }
        if (data[i6] < data[i2]) {
            final double v = data[i6];
            data[i6] = data[i2];
            data[i2] = v;
        }
        if (data[i5] < data[i1]) {
            final double w = data[i5];
            data[i5] = data[i1];
            data[i1] = w;
        }
        if (data[i4] < data[i0]) {
            final double x = data[i4];
            data[i4] = data[i0];
            data[i0] = x;
        }

        if (data[i7] < data[i6]) {
            final double u = data[i7];
            data[i7] = data[i6];
            data[i6] = u;
        }
        if (data[i5] < data[i4]) {
            final double v = data[i5];
            data[i5] = data[i4];
            data[i4] = v;
        }
        if (data[i3] < data[i2]) {
            final double w = data[i3];
            data[i3] = data[i2];
            data[i2] = w;
        }
        if (data[i1] < data[i0]) {
            final double x = data[i1];
            data[i1] = data[i0];
            data[i0] = x;
        }

        if (data[i5] < data[i3]) {
            final double u = data[i5];
            data[i5] = data[i3];
            data[i3] = u;
        }
        if (data[i4] < data[i2]) {
            final double v = data[i4];
            data[i4] = data[i2];
            data[i2] = v;
        }

        if (data[i6] < data[i3]) {
            final double u = data[i6];
            data[i6] = data[i3];
            data[i3] = u;
        }
        if (data[i4] < data[i1]) {
            final double v = data[i4];
            data[i4] = data[i1];
            data[i1] = v;
        }

        if (data[i6] < data[i5]) {
            final double u = data[i6];
            data[i6] = data[i5];
            data[i5] = u;
        }
        if (data[i4] < data[i3]) {
            final double v = data[i4];
            data[i4] = data[i3];
            data[i3] = v;
        }
        if (data[i2] < data[i1]) {
            final double w = data[i2];
            data[i2] = data[i1];
            data[i1] = w;
        }
    }


    /**
     * Sorts the given indices in an array.
     *
     * <p>Assumes all indices are valid and distinct.
     *
     * <p>Data are arranged such that:
     * <pre>{@code
     * data[i] <= data[i + i] <= data[i + 2] ...
     * }</pre>
     *
     * <p>If indices are duplicated elements will <em>not</em> be correctly ordered.
     * However in this case data will contain the same values and may be partially ordered.
     *
     * @param data Data array.
     * @param i0 Index.
     * @param i1 Index.
     * @param i2 Index.
     * @param i3 Index.
     * @param i4 Index.
     * @param i5 Index.
     * @param i6 Index.
     * @param i7 Index.
     * @param i8 Index.
     * @param i9 Index.
     * @param i10 Index.
     */
    static void sort11(double[] data, int i0, int i1, int i2, int i3, int i4, int i5, int i6, int i7,
        int i8, int i9, int i10) {
        // Uses an optimal sorting network from Knuth's Art of Computer Programming.
        // 35 comparisons.
        // Order pairs:
        //[(0,9),(1,6),(2,4),(3,7),(5,8)]
        //[(0,1),(3,5),(4,10),(6,9),(7,8)]
        //[(1,3),(2,5),(4,7),(8,10)]
        //[(0,4),(1,2),(3,7),(5,9),(6,8)]
        //[(0,1),(2,6),(4,5),(7,8),(9,10)]
        //[(2,4),(3,6),(5,7),(8,9)]
        //[(1,2),(3,4),(5,6),(7,8)]
        //[(2,3),(4,5),(6,7)]
        if (data[i8] < data[i5]) {
            final double u = data[i8];
            data[i8] = data[i5];
            data[i5] = u;
        }
        if (data[i7] < data[i3]) {
            final double v = data[i7];
            data[i7] = data[i3];
            data[i3] = v;
        }
        if (data[i4] < data[i2]) {
            final double w = data[i4];
            data[i4] = data[i2];
            data[i2] = w;
        }
        if (data[i6] < data[i1]) {
            final double x = data[i6];
            data[i6] = data[i1];
            data[i1] = x;
        }
        if (data[i9] < data[i0]) {
            final double y = data[i9];
            data[i9] = data[i0];
            data[i0] = y;
        }

        if (data[i8] < data[i7]) {
            final double u = data[i8];
            data[i8] = data[i7];
            data[i7] = u;
        }
        if (data[i9] < data[i6]) {
            final double v = data[i9];
            data[i9] = data[i6];
            data[i6] = v;
        }
        if (data[i10] < data[i4]) {
            final double w = data[i10];
            data[i10] = data[i4];
            data[i4] = w;
        }
        if (data[i5] < data[i3]) {
            final double x = data[i5];
            data[i5] = data[i3];
            data[i3] = x;
        }
        if (data[i1] < data[i0]) {
            final double y = data[i1];
            data[i1] = data[i0];
            data[i0] = y;
        }

        if (data[i10] < data[i8]) {
            final double u = data[i10];
            data[i10] = data[i8];
            data[i8] = u;
        }
        if (data[i7] < data[i4]) {
            final double v = data[i7];
            data[i7] = data[i4];
            data[i4] = v;
        }
        if (data[i5] < data[i2]) {
            final double w = data[i5];
            data[i5] = data[i2];
            data[i2] = w;
        }
        if (data[i3] < data[i1]) {
            final double x = data[i3];
            data[i3] = data[i1];
            data[i1] = x;
        }

        if (data[i8] < data[i6]) {
            final double u = data[i8];
            data[i8] = data[i6];
            data[i6] = u;
        }
        if (data[i9] < data[i5]) {
            final double v = data[i9];
            data[i9] = data[i5];
            data[i5] = v;
        }
        if (data[i7] < data[i3]) {
            final double w = data[i7];
            data[i7] = data[i3];
            data[i3] = w;
        }
        if (data[i2] < data[i1]) {
            final double x = data[i2];
            data[i2] = data[i1];
            data[i1] = x;
        }
        if (data[i4] < data[i0]) {
            final double y = data[i4];
            data[i4] = data[i0];
            data[i0] = y;
        }

        if (data[i10] < data[i9]) {
            final double u = data[i10];
            data[i10] = data[i9];
            data[i9] = u;
        }
        if (data[i8] < data[i7]) {
            final double v = data[i8];
            data[i8] = data[i7];
            data[i7] = v;
        }
        if (data[i5] < data[i4]) {
            final double w = data[i5];
            data[i5] = data[i4];
            data[i4] = w;
        }
        if (data[i6] < data[i2]) {
            final double x = data[i6];
            data[i6] = data[i2];
            data[i2] = x;
        }
        if (data[i1] < data[i0]) {
            final double y = data[i1];
            data[i1] = data[i0];
            data[i0] = y;
        }

        if (data[i9] < data[i8]) {
            final double u = data[i9];
            data[i9] = data[i8];
            data[i8] = u;
        }
        if (data[i7] < data[i5]) {
            final double v = data[i7];
            data[i7] = data[i5];
            data[i5] = v;
        }
        if (data[i6] < data[i3]) {
            final double w = data[i6];
            data[i6] = data[i3];
            data[i3] = w;
        }
        if (data[i4] < data[i2]) {
            final double x = data[i4];
            data[i4] = data[i2];
            data[i2] = x;
        }

        if (data[i8] < data[i7]) {
            final double u = data[i8];
            data[i8] = data[i7];
            data[i7] = u;
        }
        if (data[i6] < data[i5]) {
            final double v = data[i6];
            data[i6] = data[i5];
            data[i5] = v;
        }
        if (data[i4] < data[i3]) {
            final double w = data[i4];
            data[i4] = data[i3];
            data[i3] = w;
        }
        if (data[i2] < data[i1]) {
            final double x = data[i2];
            data[i2] = data[i1];
            data[i1] = x;
        }

        if (data[i7] < data[i6]) {
            final double u = data[i7];
            data[i7] = data[i6];
            data[i6] = u;
        }
        if (data[i5] < data[i4]) {
            final double v = data[i5];
            data[i5] = data[i4];
            data[i4] = v;
        }
        if (data[i3] < data[i2]) {
            final double w = data[i3];
            data[i3] = data[i2];
            data[i2] = w;
        }
    }

    /**
     * Sort the unique indices in-place to the start of the array. Duplicates are moved
     * to the end of the array and set to negative. For convenience the maximum
     * index is set into the final position in the array. If this is a duplicate it is
     * set to negative using the twos complement representation:
     *
     * <pre>{@code
     * int[] indices = ...
     * IndexSet sortUnique(indices);
     * int min = indices[0];
     * int max = indices[indices.length - 1]
     * if (max < 0) {
     *     max = ~max;
     * }
     * }</pre>
     *
     * <p>A small number of indices is sorted in place. A large number will use an
     * IndexSet which is returned for reuse by the caller. The threshold for this
     * switch is provided by the caller. An index set is used when
     * {@code indices.length > countThreshold} and there is more than 1 index.
     *
     * <p>This method assumes the {@code data} contains only positive integers.
     *
     * @param countThreshold Threshold to use an IndexSet.
     * @param data Indices.
     * @param n Number of indices.
     * @return the index set (or null if not used)
     */
    static IndexSet sortUnique(int countThreshold, int[] data, int n) {
        if (n <= 1) {
            return null;
        }
        if (n > countThreshold) {
            return sortUnique(data, n);
        }
        int unique = 1;
        int j;
        // Do an insertion sort but only compare the current set of unique values.
        for (int i = 0; ++i < n;) {
            final int v = data[i];
            // Erase data
            data[i] = -1;
            j = unique;
            if (v > data[j - 1]) {
                // Insert at end
                data[j] = v;
                unique++;
            } else if (v < data[j - 1]) {
                // Find insertion point in the unique indices
                do {
                    --j;
                } while (j >= 0 && v < data[j]);
                // Either insert at the start, or insert non-duplicate
                if (j < 0 || v != data[j]) {
                    // Update j so it is the insertion position
                    j++;
                    // Process the delayed moves
                    // Move from [j, unique) to [j+1, unique+1)
                    // System.arraycopy(data, j, data, j + 1, unique - j)
                    for (int k = unique; k-- > j;) {
                        data[k + 1] = data[k];
                    }
                    data[j] = v;
                    unique++;
                }
            }
        }
        // Set the max value at the end, bit-flipped
        if (unique < n) {
            data[n - 1] = ~data[unique - 1];
        }
        return null;
    }

    /**
     * Sort the unique indices in-place to the start of the array. Duplicates are moved
     * to the end of the array and set to negative. For convenience the maximum
     * index is set into the final position in the array. If this is a duplicate it is
     * set to negative using the twos complement representation:
     *
     * <pre>{@code
     * int[] indices = ...
     * IndexSet sortUnique(indices);
     * int min = indices[0];
     * int max = indices[indices.length - 1]
     * if (max < 0) {
     *     max = ~max;
     * }
     * }</pre>
     *
     * <p>Uses an IndexSet which is returned to the caller. Assumes the indices
     * are non-zero in length.
     *
     * @param data Indices.
     * @param n Number of indices.
     * @return the index set
     */
    private static IndexSet sortUnique(int[] data, int n) {
        final IndexSet set = IndexSet.of(data, n);
        // Iterate
        final int[] unique = {0};
        set.forEach(i -> data[unique[0]++] = i);
        if (unique[0] < n) {
            for (int i = unique[0]; i < n; i++) {
                data[i] = -1;
            }
            // Set the max value at the end, bit flipped
            data[n - 1] = ~data[unique[0] - 1];
        }
        return set;
    }

    /**
     * Sort the unique indices in-place to the start of the array. The number of
     * indices is returned.
     *
     * <pre>{@code
     * int[] indices = ...
     * int n sortIndices(indices, indices.length);
     * int min = indices[0];
     * int max = indices[n - 1]
     * }</pre>
     *
     * <p>This method assumes the {@code data} contains only positive integers.
     *
     * @param data Indices.
     * @param n Number of indices.
     * @return the number of indices
     */
    static int sortIndices(int[] data, int n) {
        // Simple cases
        if (n < 3) {
            if (n == 2) {
                final int i0 = data[0];
                final int i1 = data[1];
                if (i0 > i1) {
                    data[0] = i1;
                    data[1] = i0;
                } else if (i0 == i1) {
                    return 1;
                }
            }
            // n=0,1,2 unique values
            return n;
        }

        // Strategy: Must be fast on already ascending data.
        // Note: The recommended way to generate a lot of partition indices from
        // many quantiles for interpolation is to generate in sequence.

        // n <= small:
        //   Modified insertion sort (naturally finds ascending data)
        // n > small:
        //   Look for ascending sequence and compact
        // else:
        //   Remove duplicates using an order(1) data structure and sort

        if (n <= UNIQUE_INSERTION_SORT) {
            return sortIndicesInsertionSort(data, n);
        }

        if (isAscending(data, n)) {
            return compressDuplicates(data, n);
        }

        // At least 20 indices that are partially unordered.
        // Find min/max
        int min = data[0];
        int max = min;
        for (int i = 0; ++i < n;) {
            min = Math.min(min, data[i]);
            max = Math.max(max, data[i]);
        }

        // Benchmarking shows the IndexSet is very fast when the long[] efficiently
        // resides in cache memory. If the indices are very well separated the
        // distribution is sparse and it is faster to use a HashIndexSet despite
        // having to perform a sort after making keys unique.
        // Both structures have Order(1) detection of unique keys (the HashIndexSet
        // is configured with a load factor that should see low collision rates).
        // IndexSet     sort Order(n)        (data is stored sorted and must be read)
        // HashIndexSet sort Order(n log n)  (unique data is sorted separately)

        // For now base the choice on memory consumption alone which is a fair
        // approximation when n < 1000.
        // Above 1000 indices we assume that sorting the indices is a small cost
        // compared to sorting/partitioning the data that requires so many indices.
        // If the input data is small upstream code could detect this, e.g.
        // indices.length >> data.length, and choose to sort the data rather than
        // partitioning so many indices.

        // If the HashIndexSet uses < 8x memory of IndexSet then prefer that.
        // This detects obvious cases of sparse keys where the IndexSet is
        // outperformed by the HashIndexSet. Otherwise we can assume the
        // memory consumption of the IndexSet is small compared to the data to be
        // partitioned at these target indices (max 1/64 for double[] data); any
        // time taken here for sorting indices should be less than partitioning time.

        // This requires more analysis of performance crossover.
        // Note: Expected behaviour under extreme use-cases should be documented.

        if (HashIndexSet.memoryFootprint(n) < (IndexSet.memoryFootprint(min, max) >>> 3)) {
            return sortIndicesHashIndexSet(data, n);
        }

        // Repeat code from IndexSet as we have the min/max
        final IndexSet set = IndexSet.ofRange(min, max);
        for (int i = -1; ++i < n;) {
            set.set(data[i]);
        }
        return set.toArray(data);
    }

    /**
     * Sort the unique indices in-place to the start of the array. The number of
     * indices is returned.
     *
     * <pre>{@code
     * int[] indices = ...
     * int n sortIndices(indices, indices.length);
     * int min = indices[0];
     * int max = indices[n - 1]
     * }</pre>
     *
     * <p>This method assumes the {@code data} contains only positive integers;
     * and that {@code n} is small relative to the range of indices {@code [min, max]} such
     * that storing all indices in an {@link IndexSet} is not memory efficient.
     *
     * @param data Indices.
     * @param n Number of indices.
     * @return the number of indices
     */
    static int sortIndices2(int[] data, int n) {
        // Simple cases
        if (n < 3) {
            if (n == 2) {
                final int i0 = data[0];
                final int i1 = data[1];
                if (i0 > i1) {
                    data[0] = i1;
                    data[1] = i0;
                } else if (i0 == i1) {
                    return 1;
                }
            }
            // n=0,1,2 unique values
            return n;
        }

        // Strategy: Must be fast on already ascending data.
        // Note: The recommended way to generate a lot of partition indices from
        // many quantiles for interpolation is to generate in sequence.

        // n <= small:
        //   Modified insertion sort (naturally finds ascending data)
        // n > small:
        //   Look for ascending sequence and compact
        // else:
        //   Remove duplicates using an order(1) data structure and sort

        if (n <= UNIQUE_INSERTION_SORT) {
            return sortIndicesInsertionSort(data, n);
        }

        if (isAscending(data, n)) {
            return compressDuplicates(data, n);
        }

        return sortIndicesHashIndexSet(data, n);
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
     * Test the data is in ascending order: {@code data[i] <= data[i+1]} for all {@code i}.
     * Data is assumed to be at least length 1.
     *
     * @param data Data.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @return true if ascending
     */
    static boolean isAscending(double[] data, int left, int right) {
        for (int i = left; ++i <= right;) {
            if (data[i] < data[i - 1]) {
                // descending
                return false;
            }
        }
        return true;
    }

    // The following methods all perform the same function and are present
    // for performance testing.

    /**
     * Sort the unique indices in-place to the start of the array. The number of
     * indices is returned.
     *
     * <p>Uses an insertion sort modified to ignore duplicates.
     *
     * <p>Warning: Requires {@code n > 0}.
     *
     * @param data Indices.
     * @param n Number of indices.
     * @return the number of indices
     */
    static int sortIndicesInsertionSort(int[] data, int n) {
        int unique = 1;
        // Do an insertion sort but only compare the current set of unique values.
        for (int i = 0; ++i < n;) {
            final int v = data[i];
            int j = unique - 1;
            if (v > data[j]) {
                // Insert at end
                data[unique] = v;
                unique++;
            } else if (v < data[j]) {
                // Find insertion point in the unique indices
                do {
                    --j;
                } while (j >= 0 && v < data[j]);
                // Insertion point = j + 1
                // Insert if at start or non-duplicate
                if (j < 0 || v != data[j]) {
                    // Move (j, unique) to (j+1, unique+1)
                    for (int k = unique; --k > j;) {
                        data[k + 1] = data[k];
                    }
                    data[j + 1] = v;
                    unique++;
                }
            }
        }
        return unique;
    }

    /**
     * Sort the unique indices in-place to the start of the array. The number of
     * indices is returned.
     *
     * <p>Uses a binary search to find the insert point.
     *
     * <p>Warning: Requires {@code n > 1}.
     *
     * @param data Indices.
     * @param n Number of indices.
     * @return the number of indices
     */
    static int sortIndicesBinarySearch(int[] data, int n) {
        // Sort first 2
        if (data[1] < data[0]) {
            final int v = data[0];
            data[0] = data[1];
            data[1] = v;
        }
        int unique = data[0] != data[1] ? 2 : 1;
        // Insert the remaining indices if unique
        OUTER:
        for (int i = 1; ++i < n;) {
            // Binary search with fast exit on match
            int l = 0;
            int r = unique - 1;
            final int k = data[i];
            while (l <= r) {
                // Middle value
                final int m = (l + r) >>> 1;
                final int v = data[m];
                // Test:
                // l------m------r
                //        v  k      update left
                //     k  v         update right
                if (v < k) {
                    l = m + 1;
                } else if (v > k) {
                    r = m - 1;
                } else {
                    // Equal
                    continue OUTER;
                }
            }
            // key not found: insert at l
            System.arraycopy(data, l, data, l + 1, unique - l);
            data[l] = k;
            unique++;
        }
        return unique;
    }

    /**
     * Sort the unique indices in-place to the start of the array. The number of
     * indices is returned.
     *
     * <p>Uses a heap sort modified to ignore duplicates.
     *
     * <p>Warning: Requires {@code n > 0}.
     *
     * @param data Indices.
     * @param n Number of indices.
     * @return the number of indices
     */
    static int sortIndicesHeapSort(int[] data, int n) {
        // Build the min heap using Floyd's heap-construction algorithm
        // Start at parent of the last element in the heap (n-1)
        final int offset = n - 1;
        for (int start = offset >> 1; start >= 0; start--) {
            minHeapSiftDown(data, offset, start, n);
        }

        // The min heap has been constructed in-place so a[n-1] is the min.
        // To sort we have to move elements from the top of the
        // heap to the position immediately before the end of the heap
        // (which is below right), reducing the heap size each step:
        //                             root
        // |--------------|k|-min-heap-|r|
        //                 |  <-swap->  |

        // Move top of heap to the sorted end and move the end
        // to the top.
        int previous = data[offset];
        data[offset] = data[0];
        data[0] = previous;
        int s = n - 1;
        minHeapSiftDown(data, offset, 0, s);

        // Min heap is now 1 smaller
        // Proceed with the remaining elements but do not write them
        // to the sorted data unless different from the previous value.
        int last = 0;
        for (;;) {
            s--;
            // Move top of heap to the sorted end
            final int v = data[offset];
            data[offset] = data[offset - s];
            if (previous != v) {
                data[++last] = v;
                previous = v;
            }
            if (s == 1) {
                // end of heap
                break;
            }
            minHeapSiftDown(data, offset, 0, s);
        }
        // Stopped sifting when the heap was size 1.
        // Move the last (max) value to the sorted data.
        if (previous != data[offset]) {
            data[++last] = data[offset];
        }
        return last + 1;
    }

    /**
     * Sift the top element down the min heap.
     *
     * <p>Note this creates the min heap in descending sequence so the
     * heap is positioned below the root.
     *
     * @param a Heap data.
     * @param offset Offset of the heap in the data.
     * @param root Root of the heap.
     * @param n Size of the heap.
     */
    private static void minHeapSiftDown(int[] a, int offset, int root, int n) {
        // For node i:
        // left child: 2i + 1
        // right child: 2i + 2
        // parent: floor((i-1) / 2)

        // Value to sift
        int p = root;
        final int v = a[offset - p];
        // Left child of root: p * 2 + 1
        int c = (p << 1) + 1;
        while (c < n) {
            // Left child value
            int cv = a[offset - c];
            // Use the right child if less
            if (c + 1 < n && cv > a[offset - c - 1]) {
                cv = a[offset - c - 1];
                c++;
            }
            // Min heap requires parent <= child
            if (v <= cv) {
                // Less than smallest child - done
                break;
            }
            // Swap and descend
            a[offset - p] = cv;
            p = c;
            c = (p << 1) + 1;
        }
        a[offset - p] = v;
    }

    /**
     * Sort the unique indices in-place to the start of the array. The number of
     * indices is returned.
     *
     * <p>Uses a full sort and a second-pass to ignore duplicates.
     *
     * <p>Warning: Requires {@code n > 0}.
     *
     * @param data Indices.
     * @param n Number of indices.
     * @return the number of indices
     */
    static int sortIndicesSort(int[] data, int n) {
        java.util.Arrays.sort(data, 0, n);
        return compressDuplicates(data, n);
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
     * Sort the unique indices in-place to the start of the array. The number of
     * indices is returned.
     *
     * <p>Uses an {@link IndexSet} to ignore duplicates. The sorted array is
     * extracted from the {@link IndexSet} storage in order.
     *
     * <p>Warning: Requires {@code n > 0}.
     *
     * @param data Indices.
     * @param n Number of indices.
     * @return the number of indices
     * @see IndexSet#toArray(int[])
     */
    static int sortIndicesIndexSet(int[] data, int n) {
        // Delegate to IndexSet
        // Storage (bytes) = 8 * ceil((max - min) / 64), irrespective of n.
        // This can be use a lot of memory when the indices are spread out.
        return IndexSet.of(data, n).toArray(data);
    }

    /**
     * Sort the unique indices in-place to the start of the array. The number of
     * indices is returned.
     *
     * <p>Uses an {@link IndexSet} to ignore duplicates. The sorted array is
     * extracted from the {@link IndexSet} storage in order.
     *
     * <p>Warning: Requires {@code n > 0}.
     *
     * @param data Indices.
     * @param n Number of indices.
     * @return the number of indices
     * @see IndexSet#toArray2(int[])
     */
    static int sortIndicesIndexSet2(int[] data, int n) {
        // Delegate to IndexSet
        // Storage (bytes) = 8 * ceil((max - min) / 64), irrespective of n.
        // This can be use a lot of memory when the indices are spread out.
        return IndexSet.of(data, n).toArray2(data);
    }

    /**
     * Sort the unique indices in-place to the start of the array. The number of
     * indices is returned.
     *
     * <p>Uses a {@link HashIndexSet} to ignore duplicates and then performs
     * a full sort of the unique values.
     *
     * <p>Warning: Requires {@code n > 0}.
     *
     * @param data Indices.
     * @param n Number of indices.
     * @return the number of indices
     */
    static int sortIndicesHashIndexSet(int[] data, int n) {
        // Compress to remove duplicates.
        // Duplicates are checked using a HashIndexSet.
        // Storage (bytes) = 4 * next-power-of-2(n*2) => 2-4 times n
        final HashIndexSet set = new HashIndexSet(n);
        int i = 0;
        int last = 0;
        set.add(data[0]);
        while (++i < n) {
            final int v = data[i];
            if (set.add(v)) {
                data[++last] = v;
            }
        }
        // Sort unique data.
        // This can exploit the input already being sorted.
        Arrays.sort(data, 0, ++last);
        return last;
    }
}
