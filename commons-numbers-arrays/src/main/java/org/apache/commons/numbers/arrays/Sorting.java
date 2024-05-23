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
 *
 * @since 1.2
 */
final class Sorting {

    /** No instances. */
    private Sorting() {}

    /**
     * Sorts an array using an insertion sort.
     *
     * @param x Data array.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     */
    static void sort(double[] x, int left, int right) {
        for (int i = left; ++i <= right;) {
            final double v = x[i];
            // Move preceding higher elements above (if required)
            if (v < x[i - 1]) {
                int j = i;
                while (--j >= left && v < x[j]) {
                    x[j + 1] = x[j];
                }
                x[j + 1] = v;
            }
        }
    }

    /**
     * Sorts the elements at the given distinct indices in an array.
     *
     * @param x Data array.
     * @param a Index.
     * @param b Index.
     * @param c Index.
     */
    static void sort3(double[] x, int a, int b, int c) {
        // Decision tree avoiding swaps:
        // Order [(0,2)]
        // Move point 1 above point 2 or below point 0
        final double u = x[a];
        final double v = x[b];
        final double w = x[c];
        if (w < u) {
            if (v < w) {
                x[a] = v;
                x[b] = w;
                x[c] = u;
                return;
            }
            if (u < v) {
                x[a] = w;
                x[b] = u;
                x[c] = v;
                return;
            }
            // w < v < u
            x[a] = w;
            x[c] = u;
            return;
        }
        if (v < u) {
            // v < u < w
            x[a] = v;
            x[b] = u;
            return;
        }
        if (w < v) {
            // u < w < v
            x[b] = w;
            x[c] = v;
        }
        // u < v < w
    }

    /**
     * Sorts the elements at the given distinct indices in an array.
     *
     * @param x Data array.
     * @param a Index.
     * @param b Index.
     * @param c Index.
     * @param d Index.
     * @param e Index.
     */
    static void sort5(double[] x, int a, int b, int c, int d, int e) {
        // Uses an optimal sorting network from Knuth's Art of Computer Programming.
        // 9 comparisons.
        // Order pairs:
        // [(0,3),(1,4)]
        // [(0,2),(1,3)]
        // [(0,1),(2,4)]
        // [(1,2),(3,4)]
        // [(2,3)]
        if (x[e] < x[b]) {
            final double u = x[e];
            x[e] = x[b];
            x[b] = u;
        }
        if (x[d] < x[a]) {
            final double v = x[d];
            x[d] = x[a];
            x[a] = v;
        }

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

        if (x[e] < x[c]) {
            final double u = x[e];
            x[e] = x[c];
            x[c] = u;
        }
        if (x[b] < x[a]) {
            final double v = x[b];
            x[b] = x[a];
            x[a] = v;
        }

        if (x[e] < x[d]) {
            final double u = x[e];
            x[e] = x[d];
            x[d] = u;
        }
        if (x[c] < x[b]) {
            final double v = x[c];
            x[c] = x[b];
            x[b] = v;
        }

        if (x[d] < x[c]) {
            final double u = x[d];
            x[d] = x[c];
            x[c] = u;
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
        // 3 to 5 comparisons
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
        if (x[c] < x[b]) {
            final double u = x[c];
            x[c] = x[b];
            x[b] = u;
        } else if (x[b] < x[a]) {
            //    a--c
            // b--d
            final double xb = x[a];
            x[a] = x[b];
            x[b] = xb;
            //    b--c
            // a--d
            if (x[d] < xb) {
                x[b] = x[d];
                // Move a pair to maintain the sorted order
                x[d] = x[c];
                x[c] = xb;
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
    static void upperMedian4(double[] x, int a, int b, int c, int d) {
        // 3 to 5 comparisons
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
                x[a] = x[b];
                x[b] = xc;
            }
        }
    }

    /**
     * Sort the unique indices in-place to the start of the array. The number of
     * unique indices is returned.
     *
     * <p>Uses an insertion sort modified to ignore duplicates. Use on small {@code n}.
     *
     * <p>Warning: Requires {@code n > 0}. The array contents after the count of unique
     * indices {@code c} are unchanged (i.e. {@code [c, n)}. This may change the count of
     * each unique index in the entire array.
     *
     * @param x Indices.
     * @param n Number of indices.
     * @return the number of unique indices
     */
    static int insertionSortIndices(int[] x, int n) {
        // Index of last unique value
        int unique = 0;
        // Do an insertion sort but only compare the current set of unique values.
        for (int i = 0; ++i < n;) {
            final int v = x[i];
            int j = unique;
            if (v > x[j]) {
                // Insert at end
                x[++unique] = v;
            } else if (v < x[j]) {
                // Find insertion point in the unique indices
                do {
                    --j;
                } while (j >= 0 && v < x[j]);
                // Insertion point = j + 1
                // Insert if at start or non-duplicate
                if (j < 0 || v != x[j]) {
                    // Move (j, unique] to (j+1, unique+1]
                    for (int k = unique; k > j; --k) {
                        x[k + 1] = x[k];
                    }
                    x[j + 1] = v;
                    ++unique;
                }
            }
        }
        return unique + 1;
    }

    /**
     * Sort the unique indices in-place to the start of the array. The number of
     * unique indices is returned.
     *
     * <p>Uses an Order(1) data structure to ignore duplicates.
     *
     * <p>Warning: Requires {@code n > 0}. The array contents after the count of unique
     * indices {@code c} are unchanged (i.e. {@code [c, n)}. This may change the count of
     * each unique index in the entire array.
     *
     * @param x Indices.
     * @param n Number of indices.
     * @return the number of unique indices
     */
    static int sortIndices(int[] x, int n) {
        // Duplicates are checked using a primitive hash set.
        // Storage (bytes) = 4 * next-power-of-2(n*2) => 2-4 times n
        final HashIndexSet set = HashIndexSet.create(n);
        int i = 0;
        int last = 0;
        set.add(x[0]);
        while (++i < n) {
            final int v = x[i];
            if (set.add(v)) {
                x[++last] = v;
            }
        }
        Arrays.sort(x, 0, ++last);
        return last;
    }
}
