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
 * A strategy to pick a pivoting index of an array for partitioning.
 *
 * <p>An ideal strategy will pick [1/2, 1/2] across a variety of data.
 *
 * @since 1.2
 */
enum PivotingStrategy {
    /**
     * Pivot around the centre of the range.
     */
    CENTRAL {
        @Override
        int pivotIndex(double[] data, int left, int right, int ignored) {
            return med(left, right);
        }

        @Override
        int[] getSampledIndices(int left, int right, int ignored) {
            return new int[] {med(left, right)};
        }

        @Override
        int samplingEffect() {
            return UNCHANGED;
        }
    },
    /**
     * Pivot around the median of 3 values within the range: the first; the centre; and the last.
     */
    MEDIAN_OF_3 {
        @Override
        int pivotIndex(double[] data, int left, int right, int ignored) {
            return med3(data, left, med(left, right), right);
        }

        @Override
        int[] getSampledIndices(int left, int right, int ignored) {
            return new int[] {left, med(left, right), right};
        }

        @Override
        int samplingEffect() {
            return UNCHANGED;
        }
    },
    /**
     * Pivot around the median of 9 values within the range.
     * Uses the median of 3 medians of 3. The returned value
     * is ranked 4, 5, or 6 out of the 9 values.
     * This is also known in the literature as Tukey’s "ninther" pivot.
     */
    MEDIAN_OF_9 {
        @Override
        int pivotIndex(double[] data, int left, int right, int ignored) {
            final int s = (right - left) >>> 3;
            final int m = med(left, right);
            final int x = med3(data, left, left + s, left + (s << 1));
            final double a = data[x];
            final int y = med3(data, m - s, m, m + s);
            final double b = data[y];
            final int z = med3(data, right - (s << 1), right - s, right);
            return med3(a, b, data[z], x, y, z);
        }

        @Override
        int[] getSampledIndices(int left, int right, int ignored) {
            final int s = (right - left) >>> 3;
            final int m = med(left, right);
            return new int[] {
                left, left + s, left + (s << 1),
                m - s, m, m + s,
                right - (s << 1), right - s, right
            };
        }

        @Override
        int samplingEffect() {
            return UNCHANGED;
        }
    },
    /**
     * Pivot around the median of 3 or 9 values within the range.
     *
     * <p>Note: Bentley & McIlroy (1993) choose a size of 40 to pivot around 9 values;
     * and a lower size of 7 to use the central; otherwise the median of 3.
     * This method does not switch to the central method for small sizes.
     */
    DYNAMIC {
        @Override
        int pivotIndex(double[] data, int left, int right, int ignored) {
            if (right - left >= MED_9) {
                return MEDIAN_OF_9.pivotIndex(data, left, right, ignored);
            }
            return MEDIAN_OF_3.pivotIndex(data, left, right, ignored);
        }

        @Override
        int[] getSampledIndices(int left, int right, int ignored) {
            if (right - left >= MED_9) {
                return MEDIAN_OF_9.getSampledIndices(left, right, ignored);
            }
            return MEDIAN_OF_3.getSampledIndices(left, right, ignored);
        }

        @Override
        int samplingEffect() {
            return UNCHANGED;
        }
    },
    /**
     * Pivot around the median of 5 values within the range.
     * Requires that {@code right - left >= 4}.
     *
     * <p>Warning: This has the side effect that the 5 values are also partially sorted.
     *
     * <p>Uses the same spacing as {@link DualPivotingStrategy#SORT_5}.
     */
    MEDIAN_OF_5 {
        @Override
        int pivotIndex(double[] data, int left, int right, int ignored) {
            // 1/6 = 5/30 ~ 1/8 + 1/32 + 1/64 : 0.1666 ~ 0.1719
            // Ensure the value is above zero to choose different points!
            // This is safe if len >= 4.
            final int len = right - left;
            final int sixth = 1 + (len >>> 3) + (len >>> 5) + (len >>> 6);
            // Note: No use of median(left, right). This is not targeted by median of 3 killer
            // input as it does not use the end points left and right.
            final int p3 = left + (len >>> 1);
            final int p2 = p3 - sixth;
            final int p1 = p2 - sixth;
            final int p4 = p3 + sixth;
            final int p5 = p4 + sixth;
            return Sorting.median5(data, p1, p2, p3, p4, p5);
        }

        @Override
        int[] getSampledIndices(int left, int right, int ignored) {
            final int len = right - left;
            final int sixth = 1 + (len >>> 3) + (len >>> 5) + (len >>> 6);
            final int p3 = left + (len >>> 1);
            final int p2 = p3 - sixth;
            final int p1 = p2 - sixth;
            final int p4 = p3 + sixth;
            final int p5 = p4 + sixth;
            return new int[] {p1, p2, p3, p4, p5};
        }

        @Override
        int samplingEffect() {
            return PARTIAL_SORT;
        }
    },
    /**
     * Pivot around the median of 5 values within the range.
     * Requires that {@code right - left >= 4}.
     *
     * <p>Warning: This has the side effect that the 5 values are also partially sorted.
     *
     * <p>Uses the same spacing as {@link DualPivotingStrategy#SORT_5B}.
     */
    MEDIAN_OF_5B {
        @Override
        int pivotIndex(double[] data, int left, int right, int ignored) {
            // 1/7 = 5/35 ~ 1/8 + 1/64 : 0.1429 ~ 0.1406
            // Ensure the value is above zero to choose different points!
            // This is safe if len >= 4.
            final int len = right - left;
            final int seventh = 1 + (len >>> 3) + (len >>> 6);
            final int p3 = left + (len >>> 1);
            final int p2 = p3 - seventh;
            final int p1 = p2 - seventh;
            final int p4 = p3 + seventh;
            final int p5 = p4 + seventh;
            Sorting.sort4(data, p1, p2, p4, p5);
            // p2 and p4 are sorted: check if p3 is between them
            if (data[p3] < data[p2]) {
                return p2;
            }
            return data[p3] > data[p4] ? p4 : p3;
        }

        @Override
        int[] getSampledIndices(int left, int right, int ignored) {
            final int len = right - left;
            final int seventh = 1 + (len >>> 3) + (len >>> 6);
            final int p3 = left + (len >>> 1);
            final int p2 = p3 - seventh;
            final int p1 = p2 - seventh;
            final int p4 = p3 + seventh;
            final int p5 = p4 + seventh;
            return new int[] {p1, p2, p3, p4, p5};
        }

        @Override
        int samplingEffect() {
            return PARTIAL_SORT;
        }
    },
    /**
     * Pivot around the target index.
     */
    TARGET {
        @Override
        int pivotIndex(double[] data, int left, int right, int k) {
            return k;
        }

        @Override
        int[] getSampledIndices(int left, int right, int k) {
            return new int[] {k};
        }

        @Override
        int samplingEffect() {
            return UNCHANGED;
        }
    };

    /** Sampled points are unchanged. */
    static final int UNCHANGED = 0;
    /** Sampled points are partially sorted. */
    static final int PARTIAL_SORT = 0x1;
    /** Sampled points are sorted. */
    static final int SORT = 0x2;
    /** Size to pivot around the median of 9. */
    private static final int MED_9 = 40;

    /**
     * Compute the median index.
     *
     * <p>Note: This intentionally uses the median as {@code left + (right - left + 1) / 2}.
     * If the median is {@code left + (right - left) / 2} then the median is 1 position lower
     * for even length due to using an inclusive right bound. This median is not as affected
     * by median-of-3 killer sequences. For benchmarking it is useful to maintain the classic
     * median-of-3 behaviour to be able to trigger worst case performance on input
     * used in the literature.
     *
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @return the median index
     */
    private static int med(int left, int right) {
        return (left + right + 1) >>> 1;
    }

    /**
     * Find the median index of 3.
     *
     * @param data Values.
     * @param i Index.
     * @param j Index.
     * @param k Index.
     * @return the median index
     */
    private static int med3(double[] data, int i, int j, int k) {
        return med3(data[i], data[j], data[k], i, j, k);
    }

    /**
     * Find the median index of 3 values.
     *
     * @param a Value.
     * @param b Value.
     * @param c Value.
     * @param ia Index of a.
     * @param ib Index of b.
     * @param ic Index of c.
     * @return the median index
     */
    private static int med3(double a, double b, double c, int ia, int ib, int ic) {
        if (a < b) {
            if (b < c) {
                return ib;
            }
            return a < c ? ic : ia;
        }
        if (b > c) {
            return ib;
        }
        return a > c ? ic : ia;
    }

    /**
     * Find a pivot index of the array so that partitioning into 2-regions can be made.
     *
     * <pre>{@code
     * left <= p <= right
     * }</pre>
     *
     * <p>The argument {@code k} is the target index in {@code [left, right]}. Strategies
     * may use this to help select the pivot index. If not available (e.g. selecting a pivot
     * for quicksort) then choose a value in {@code [left, right]} to be safe.
     *
     * @param data Array.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param k Target index.
     * @return pivot
     */
    abstract int pivotIndex(double[] data, int left, int right, int k);

    // The following methods allow the strategy and side effects to be tested

    /**
     * Get the indices of points that will be sampled.
     *
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param k Target index.
     * @return the indices
     */
    abstract int[] getSampledIndices(int left, int right, int k);

    /**
     * Get the effect on the sampled points.
     * <ul>
     * <li>0 - Unchanged
     * <li>1 - Partially sorted
     * <li>2 - Sorted
     * </ul>
     *
     * @return the effect
     */
    abstract int samplingEffect();
}
