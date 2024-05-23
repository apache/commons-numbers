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
 * A strategy to pick two pivot indices of an array for partitioning.
 *
 * <p>An ideal strategy will pick the tertiles across a variety of data so
 * to divide the data into [1/3, 1/3, 1/3].
 *
 * @see <a href="https://en.wiktionary.org/wiki/tertile">Tertile (Wiktionary)</a>
 * @since 1.2
 */
enum DualPivotingStrategy {
    /**
     * Pivot around the medians at 1/3 and 2/3 of the range.
     *
     * <p>Requires {@code right - left >= 2}.
     *
     * <p>On sorted data the tertiles are: 0.3340 0.6670
     * <p>On random data the tertiles are:
     * <pre>
     *         min      max     mean       sd   median     skew
     * [1]  0.0000   0.9970   0.3327   0.2357   0.2920   0.5654
     * [2]  0.0020   1.0000   0.3346   0.2356   0.2940   0.5675
     * [3]  0.0000   0.9970   0.3328   0.2356   0.2920   0.5656
     * </pre>
     */
    MEDIANS {
        @Override
        int pivotIndex(double[] data, int left, int right, int[] pivot2) {
            // Original 'medians' method from the dual-pivot quicksort paper by Vladimir Yaroslavskiy
            final int len = right - left;
            // Do not pivot at the ends by setting 1/3 to at least 1.
            // This is safe if len >= 2.
            final int third = Math.max(1, len / 3);
            final int m1 = left + third;
            final int m2 = right - third;
            // Ensure p1 is lower
            if (data[m1] < data[m2]) {
                pivot2[0] = m2;
                return m1;
            }
            pivot2[0] = m1;
            return m2;
        }

        @Override
        int[] getSampledIndices(int left, int right) {
            final int len = right - left;
            final int third = Math.max(1, len / 3);
            final int m1 = left + third;
            final int m2 = right - third;
            return new int[] {m1, m2};
        }

        @Override
        int samplingEffect() {
            return UNCHANGED;
        }
    },
    /**
     * Pivot around the 2nd and 4th values from 5 approximately uniformly spaced within the range.
     * Uses points +/- sixths from the median: 1/6, 1/3, 1/2, 2/3, 5/6.
     *
     * <p>Requires {@code right - left >= 4}.
     *
     * <p>Warning: This has the side effect that the 5 values are also sorted.
     *
     * <p>On sorted data the tertiles are: 0.3290 0.6710
     * <p>On random data the tertiles are:
     * <pre>
     *         min      max     mean       sd   median     skew
     * [1]  0.0010   0.9820   0.3327   0.1778   0.3130   0.4650
     * [2]  0.0030   0.9760   0.3348   0.1778   0.3150   0.4665
     * [3]  0.0010   0.9870   0.3325   0.1779   0.3130   0.4698
     * </pre>
     */
    SORT_5 {
        @Override
        int pivotIndex(double[] data, int left, int right, int[] pivot2) {
            // 1/6 = 5/30 ~ 1/8 + 1/32 + 1/64 : 0.1666 ~ 0.1719
            // Ensure the value is above zero to choose different points!
            // This is safe if len >= 4.
            final int len = right - left;
            final int sixth = 1 + (len >>> 3) + (len >>> 5) + (len >>> 6);
            final int p3 = left + (len >>> 1);
            final int p2 = p3 - sixth;
            final int p1 = p2 - sixth;
            final int p4 = p3 + sixth;
            final int p5 = p4 + sixth;
            Sorting.sort5(data, p1, p2, p3, p4, p5);
            pivot2[0] = p4;
            return p2;
        }

        @Override
        int[] getSampledIndices(int left, int right) {
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
            return SORT;
        }
    },
    /**
     * Pivot around the 2nd and 4th values from 5 approximately uniformly spaced within the range.
     * Uses points +/- sevenths from the median: 3/14, 5/14, 1/2, 9/14, 11/14.
     *
     * <p>Requires {@code right - left >= 4}.
     *
     * <p>Warning: This has the side effect that the 5 values are also sorted.
     *
     * <p>On sorted data the tertiles are: 0.3600 0.6400
     * <p>On random data the tertiles are:
     * <pre>
     *         min      max     mean       sd   median     skew
     * [1]  0.0010   0.9790   0.3330   0.1780   0.3140   0.4665
     * [2]  0.0030   0.9800   0.3348   0.1778   0.3150   0.4681
     * [3]  0.0010   0.9770   0.3322   0.1777   0.3130   0.4677
     * </pre>
     */
    SORT_5B {
        @Override
        int pivotIndex(double[] data, int left, int right, int[] pivot2) {
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
            Sorting.sort5(data, p1, p2, p3, p4, p5);
            pivot2[0] = p4;
            return p2;
        }

        @Override
        int[] getSampledIndices(int left, int right) {
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
            return SORT;
        }
    },
    /**
     * This strategy is the same as {@link #SORT_5B} with the exception that it
     * returns identical pivots if the data at the chosen pivots is equal.
     *
     * <p>This allows testing switching to a single pivot strategy against using
     * a dual pivot partitioning with effectively only 1 pivot. This requires
     * the dual pivot partition function to check pivot1 == pivot2. If the
     * dual pivot partition function checks data[pivot1] == data[pivot2] then
     * the switching choice cannot be enabled/disabled by changing pivoting strategy
     * and must use another mechanism.
     *
     * <p>This specific strategy has been selected for single-pivot switching as
     * {@link #SORT_5B} benchmarks as consistently fast across all data input.
     */
    SORT_5B_SP {
        @Override
        int pivotIndex(double[] data, int left, int right, int[] pivot2) {
            final int pivot1 = SORT_5B.pivotIndex(data, left, right, pivot2);
            if (data[pivot1] == data[pivot2[0]]) {
                // Here 3 of 5 middle values are the same.
                // Present single-pivot pivot methods would not
                // have an advantage pivoting on p2, p3, or p4; just use 'p2'
                pivot2[0] = pivot1;
            }
            return pivot1;
        }

        @Override
        int[] getSampledIndices(int left, int right) {
            return SORT_5B.getSampledIndices(left, right);
        }

        @Override
        int samplingEffect() {
            return SORT;
        }
    },
    /**
     * Pivot around the 2nd and 4th values from 5 approximately uniformly spaced within the range.
     * Uses points +/- eights from the median: 1/4, 3/8, 1/2, 5/8, 3/4.
     *
     * <p>Requires {@code right - left >= 4}.
     *
     * <p>Warning: This has the side effect that the 5 values are also sorted.
     *
     * <p>On sorted data the tertiles are: 0.3750 0.6250
     * <p>On random data the tertiles are:
     * <pre>
     *         min      max     mean       sd   median     skew
     * [1]  0.0010   0.9790   0.3324   0.1779   0.3130   0.4666
     * [2]  0.0030   0.9850   0.3348   0.1778   0.3150   0.4686
     * [3]  0.0010   0.9720   0.3327   0.1779   0.3130   0.4666
     * </pre>
     */
    SORT_5C {
        @Override
        int pivotIndex(double[] data, int left, int right, int[] pivot2) {
            // 1/8 = 0.125
            // Ensure the value is above zero to choose different points!
            // This is safe if len >= 4.
            final int len = right - left;
            final int eighth = 1 + (len >>> 3);
            final int p3 = left + (len >>> 1);
            final int p2 = p3 - eighth;
            final int p1 = p2 - eighth;
            final int p4 = p3 + eighth;
            final int p5 = p4 + eighth;
            Sorting.sort5(data, p1, p2, p3, p4, p5);
            pivot2[0] = p4;
            return p2;
        }

        @Override
        int[] getSampledIndices(int left, int right) {
            final int len = right - left;
            final int eighth = 1 + (len >>> 3);
            final int p3 = left + (len >>> 1);
            final int p2 = p3 - eighth;
            final int p1 = p2 - eighth;
            final int p4 = p3 + eighth;
            final int p5 = p4 + eighth;
            return new int[] {p1, p2, p3, p4, p5};
        }

        @Override
        int samplingEffect() {
            return SORT;
        }
    },
    /**
     * Pivot around the 2nd and 4th values from 5 medians approximately uniformly spaced within
     * the range. The medians are from 3 samples. The 5 samples of 3 do not overlap thus this
     * method requires {@code right - left >= 14}. The samples can be visualised as 5 sorted
     * columns:
     *
     * <pre>
     * v w x y z
     * 1 2 3 4 5
     * a b c d e
     * </pre>
     *
     * <p>The pivots are points 2 and 4. The other points are either known to be below or
     * above the pivots; or potentially below or above the pivots.
     *
     * <p>Pivot 1: below {@code 1,a,b}; potentially below {@code v,c,d,e}. This ranks
     * pivot 1 from 4/15 to 8/15 and exactly 5/15 if the input data is sorted/reverse sorted.
     *
     * <p>Pivot 2: above {@code 5,y,z}; potentially above {@code e,v,w,x}. This ranks
     * pivot 2 from 7/15 to 11/15 and exactly 10/15 if the input data is sorted/reverse sorted.
     *
     * <p>Warning: This has the side effect that the 15 samples values are partially sorted.
     *
     * <p>On sorted data the tertiles are: 0.3140 0.6860
     * <p>On random data the tertiles are:
     * <pre>
     *         min      max     mean       sd   median     skew
     * [1]  0.0090   0.9170   0.3783   0.1320   0.3730   0.2107
     * [2]  0.0030   0.8950   0.2438   0.1328   0.2270   0.6150
     * [3]  0.0110   0.9140   0.3779   0.1319   0.3730   0.2114
     * </pre>
     * <p>Note the bias towards the outer regions.
     */
    SORT_5_OF_3 {
        @Override
        int pivotIndex(double[] data, int left, int right, int[] pivot2) {
            // Step size of 1/16 of the length
            final int len = right - left;
            final int step = Math.max(1, len >>> 4);
            final int step3 = step * 3;
            final int p3 = left + (len >>> 1);
            final int p2 = p3 - step3;
            final int p1 = p2 - step3;
            final int p4 = p3 + step3;
            final int p5 = p4 + step3;
            // 5 medians of 3
            Sorting.sort3(data, p1 - step, p1, p1 + step);
            Sorting.sort3(data, p2 - step, p2, p2 + step);
            Sorting.sort3(data, p3 - step, p3, p3 + step);
            Sorting.sort3(data, p4 - step, p4, p4 + step);
            Sorting.sort3(data, p5 - step, p5, p5 + step);
            // Sort the medians
            Sorting.sort5(data, p1, p2, p3, p4, p5);
            pivot2[0] = p4;
            return p2;
        }

        @Override
        int[] getSampledIndices(int left, int right) {
            final int len = right - left;
            final int step = Math.max(1, len >>> 4);
            final int step3 = step * 3;
            final int p3 = left + (len >>> 1);
            final int p2 = p3 - step3;
            final int p1 = p2 - step3;
            final int p4 = p3 + step3;
            final int p5 = p4 + step3;
            return new int[] {
                p1 - step, p1, p1 + step,
                p2 - step, p2, p2 + step,
                p3 - step, p3, p3 + step,
                p4 - step, p4, p4 + step,
                p5 - step, p5, p5 + step,
            };
        }

        @Override
        int samplingEffect() {
            return PARTIAL_SORT;
        }
    },
    /**
     * Pivot around the 2nd and 3rd values from 4 medians approximately uniformly spaced within
     * the range. The medians are from 3 samples. The 4 samples of 3 do not overlap thus this
     * method requires {@code right - left >= 11}. The samples can be visualised as 4 sorted
     * columns:
     *
     * <pre>
     * w x y z
     * 1 2 3 4
     * a b c d
     * </pre>
     *
     * <p>The pivots are points 2 and 3. The other points are either known to be below or
     * above the pivots; or potentially below or above the pivots.
     *
     * <p>Pivot 1: below {@code 1,a,b}; potentially below {@code w,c,d}. This ranks
     * pivot 1 from 4/12 to 7/12 and exactly 5/12 if the input data is sorted/reverse sorted.
     *
     * <p>Pivot 2: above {@code 4,y,z}; potentially above {@code d,w,x}. This ranks
     * pivot 2 from 5/15 to 8/12 and exactly 7/12 if the input data is sorted/reverse sorted.
     *
     * <p>Warning: This has the side effect that the 12 samples values are partially sorted.
     *
     * <p>On sorted data the tertiles are: 0.3850 0.6160
     * <p>On random data the tertiles are:
     * <pre>
     *         min      max     mean       sd   median     skew
     * [1]  0.0160   0.9580   0.4269   0.1454   0.4230   0.1366
     * [2]  0.0020   0.8270   0.1467   0.1193   0.1170   1.1417
     * [3]  0.0140   0.9560   0.4264   0.1453   0.4230   0.1352
     * </pre>
     * <p>Note the large bias towards the outer regions.
     */
    SORT_4_OF_3 {
        @Override
        int pivotIndex(double[] data, int left, int right, int[] pivot2) {
            // Step size of 1/13 of the length: 1/13 ~ 1/16 + 1/64 : 0.0769 ~ 0.0781
            final int len = right - left;
            final int step = Math.max(1, (len >>> 4) + (len >>> 6));
            final int step3 = step * 3;
            final int p1 = left + (step << 1) - 1;
            final int p2 = p1 + step3;
            final int p3 = p2 + step3;
            final int p4 = p3 + step3;
            // 5 medians of 3
            Sorting.sort3(data, p1 - step, p1, p1 + step);
            Sorting.sort3(data, p2 - step, p2, p2 + step);
            Sorting.sort3(data, p3 - step, p3, p3 + step);
            Sorting.sort3(data, p4 - step, p4, p4 + step);
            // Sort the medians
            Sorting.sort4(data, p1, p2, p3, p4);
            pivot2[0] = p3;
            return p2;
        }

        @Override
        int[] getSampledIndices(int left, int right) {
            final int len = right - left;
            final int step = Math.max(1, (len >>> 4) + (len >>> 6));
            final int step3 = step * 3;
            final int p1 = left + (step << 1) - 1;
            final int p2 = p1 + step3;
            final int p3 = p2 + step3;
            final int p4 = p3 + step3;
            return new int[] {
                p1 - step, p1, p1 + step,
                p2 - step, p2, p2 + step,
                p3 - step, p3, p3 + step,
                p4 - step, p4, p4 + step,
            };
        }

        @Override
        int samplingEffect() {
            return PARTIAL_SORT;
        }
    },
    /**
     * Pivot around the 1st and 3rd values from 3 medians approximately uniformly spaced within
     * the range. The medians are from 3 samples. The 3 samples of 3 do not overlap thus this
     * method requires {@code right - left >= 8}. The samples can be visualised as 3 sorted
     * columns:
     *
     * <pre>
     * x y z
     * 1 2 3
     * a b c
     * </pre>
     *
     * <p>The pivots are points 1 and 3. The other points are either known to be below or
     * above the pivots; or potentially below or above the pivots.
     *
     * <p>Pivot 1: below {@code a}; potentially below {@code b, c}. This ranks
     * pivot 1 from 2/9 to 4/9 and exactly 2/9 if the input data is sorted/reverse sorted.
     *
     * <p>Pivot 2: above {@code z}; potentially above {@code x,y}. This ranks
     * pivot 2 from 6/9 to 8/9 and exactly 8/9 if the input data is sorted/reverse sorted.
     *
     * <p>Warning: This has the side effect that the 9 samples values are partially sorted.
     *
     * <p>On sorted data the tertiles are: 0.1280 0.8720
     * <p>On random data the tertiles are:
     * <pre>
     *         min      max     mean       sd   median     skew
     * [1]  0.0010   0.9460   0.3062   0.1560   0.2910   0.4455
     * [2]  0.0030   0.9820   0.3875   0.1813   0.3780   0.2512
     * [3]  0.0010   0.9400   0.3063   0.1558   0.2910   0.4453
     * </pre>
     * <p>Note the bias towards the central region.
     */
    SORT_3_OF_3 {
        @Override
        int pivotIndex(double[] data, int left, int right, int[] pivot2) {
            // Step size of 1/8 of the length
            final int len = right - left;
            final int step = Math.max(1, len >>> 3);
            final int step3 = step * 3;
            final int p2 = left + (len >>> 1);
            final int p1 = p2 - step3;
            final int p3 = p2 + step3;
            // 3 medians of 3
            Sorting.sort3(data, p1 - step, p1, p1 + step);
            Sorting.sort3(data, p2 - step, p2, p2 + step);
            Sorting.sort3(data, p3 - step, p3, p3 + step);
            // Sort the medians
            Sorting.sort3(data, p1, p2, p3);
            pivot2[0] = p3;
            return p1;
        }

        @Override
        int[] getSampledIndices(int left, int right) {
            final int len = right - left;
            final int step = Math.max(1, len >>> 3);
            final int step3 = step * 3;
            final int p2 = left + (len >>> 1);
            final int p1 = p2 - step3;
            final int p3 = p2 + step3;
            return new int[] {
                p1 - step, p1, p1 + step,
                p2 - step, p2, p2 + step,
                p3 - step, p3, p3 + step,
            };
        }

        @Override
        int samplingEffect() {
            return PARTIAL_SORT;
        }
    },
    /**
     * Pivot around the 2nd and 4th values from 5 medians approximately uniformly spaced within
     * the range. The medians are from 5 samples. The 5 samples of 5 do not overlap thus this
     * method requires {@code right - left >= 24}. The samples can be visualised as 5 sorted
     * columns:
     *
     * <pre>
     * v w x y z
     * q r s t u
     * 1 2 3 4 5
     * f g h i j
     * a b c d e
     * </pre>
     *
     * <p>The pivots are points 2 and 4. The other points are either known to be below or
     * above the pivots; or potentially below or above the pivots.
     *
     * <p>Pivot 1: below {@code 1,a,b,f,g}; potentially below {@code q,v,c,d,e,h,i,j}. This ranks
     * pivot 1 from 6/25 to 14/25 and exactly 8/25 if the input data is sorted/reverse sorted.
     *
     * <p>Pivot 2 by symmetry from 12/25 to 20/25 and exactly 18/25 for sorted data.
     *
     * <p>Warning: This has the side effect that the 25 samples values are partially sorted.
     *
     * <p>On sorted data the tertiles are: 0.3050 0.6950
     * <p>On random data the tertiles are:
     * <pre>
     *         min      max     mean       sd   median     skew
     * [1]  0.0270   0.8620   0.3996   0.1093   0.3970   0.1130
     * [2]  0.0030   0.8100   0.2010   0.1106   0.1860   0.6691
     * [3]  0.0270   0.8970   0.3994   0.1093   0.3970   0.1147
     * </pre>
     * <p>Note the bias towards the outer regions on random data but the inner region on
     * sorted data.
     */
    SORT_5_OF_5 {
        @Override
        int pivotIndex(double[] data, int left, int right, int[] pivot2) {
            // Step size of 1/25 of the length
            final int len = right - left;
            final int step = Math.max(1, len / 25);
            final int step2 = step << 1;
            final int step5 = step * 5;
            final int p3 = left + (len >>> 1);
            final int p2 = p3 - step5;
            final int p1 = p2 - step5;
            final int p4 = p3 + step5;
            final int p5 = p4 + step5;
            // 5 medians of 3
            Sorting.sort5(data, p1 - step2, p1 - step, p1, p1 + step, p1 + step2);
            Sorting.sort5(data, p2 - step2, p2 - step, p2, p2 + step, p2 + step2);
            Sorting.sort5(data, p3 - step2, p3 - step, p3, p3 + step, p3 + step2);
            Sorting.sort5(data, p4 - step2, p4 - step, p4, p4 + step, p4 + step2);
            Sorting.sort5(data, p5 - step2, p5 - step, p5, p5 + step, p5 + step2);
            // Sort the medians
            Sorting.sort5(data, p1, p2, p3, p4, p5);
            pivot2[0] = p4;
            return p2;
        }

        @Override
        int[] getSampledIndices(int left, int right) {
            // Step size of 1/25 of the length
            final int len = right - left;
            final int step = Math.max(1, len / 25);
            final int step2 = step << 1;
            final int step5 = step * 5;
            final int p3 = left + (len >>> 1);
            final int p2 = p3 - step5;
            final int p1 = p2 - step5;
            final int p4 = p3 + step5;
            final int p5 = p4 + step5;
            return new int[] {
                p1 - step2, p1 - step, p1, p1 + step, p1 + step2,
                p2 - step2, p2 - step, p2, p2 + step, p2 + step2,
                p3 - step2, p3 - step, p3, p3 + step, p3 + step2,
                p4 - step2, p4 - step, p4, p4 + step, p4 + step2,
                p5 - step2, p5 - step, p5, p5 + step, p5 + step2,
            };
        }

        @Override
        int samplingEffect() {
            return PARTIAL_SORT;
        }
    },
    /**
     * Pivot around the 3rd and 5th values from 7 approximately uniformly spaced within the range.
     * Uses points +/- eights from the median: 1/8, 1/4, 3/8, 1/2, 5/8, 3/4, 7/8.
     *
     * <p>Requires {@code right - left >= 6}.
     *
     * <p>Warning: This has the side effect that the 7 values are also sorted.
     *
     * <p>On sorted data the tertiles are: 0.3760 0.6240
     * <p>On random data the tertiles are:
     * <pre>
     *         min      max     mean       sd   median     skew
     * [1]  0.0020   0.9600   0.3745   0.1609   0.3640   0.3092
     * [2]  0.0030   0.9490   0.2512   0.1440   0.2300   0.6920
     * [3]  0.0030   0.9620   0.3743   0.1609   0.3640   0.3100
     * </pre>
     * <p>Note the bias towards the outer regions.
     */
    SORT_7 {
        @Override
        int pivotIndex(double[] data, int left, int right, int[] pivot2) {
            // Ensure the value is above zero to choose different points!
            // This is safe if len >= 4.
            final int len = right - left;
            final int eighth = Math.max(1, len >>> 3);
            final int p4 = left + (len >>> 1);
            final int p3 = p4 - eighth;
            final int p2 = p3 - eighth;
            final int p1 = p2 - eighth;
            final int p5 = p4 + eighth;
            final int p6 = p5 + eighth;
            final int p7 = p6 + eighth;
            Sorting.sort7(data, p1, p2, p3, p4, p5, p6, p7);
            pivot2[0] = p5;
            return p3;
        }

        @Override
        int[] getSampledIndices(int left, int right) {
            final int len = right - left;
            final int eighth = Math.max(1, len >>> 3);
            final int p4 = left + (len >>> 1);
            final int p3 = p4 - eighth;
            final int p2 = p3 - eighth;
            final int p1 = p2 - eighth;
            final int p5 = p4 + eighth;
            final int p6 = p5 + eighth;
            final int p7 = p6 + eighth;
            return new int[] {p1, p2, p3, p4, p5, p6, p7};
        }

        @Override
        int samplingEffect() {
            return SORT;
        }
    },
    /**
     * Pivot around the 3rd and 6th values from 8 approximately uniformly spaced within the range.
     * Uses points +/- ninths from the median: m - 4/9, m - 3/9, m - 2/9, m - 1/9; m + 1 + 1/9,
     * m + 1 + 2/9, m + 1 + 3/9, m + 1 + 4/9.
     *
     * <p>Requires {@code right - left >= 7}.
     *
     * <p>Warning: This has the side effect that the 8 values are also sorted.
     *
     * <p>On sorted data the tertiles are: 0.3380 0.6630
     * <p>On random data the tertiles are:
     * <pre>
     *         min      max     mean       sd   median     skew
     * [1]  0.0030   0.9480   0.3327   0.1485   0.3200   0.4044
     * [2]  0.0050   0.9350   0.3345   0.1485   0.3220   0.4056
     * [3]  0.0020   0.9320   0.3328   0.1485   0.3200   0.4063
     * </pre>
     */
    SORT_8 {
        @Override
        int pivotIndex(double[] data, int left, int right, int[] pivot2) {
            // 1/9 = 4/36 = 8/72 ~ 7/64 ~ 1/16 + 1/32 + 1/64 : 0.11111 ~ 0.1094
            // Ensure the value is above zero to choose different points!
            // This is safe if len >= 7.
            final int len = right - left;
            final int ninth = Math.max(1, (len >>> 4) + (len >>> 5) + (len >>> 6));
            // Work from middle outward. This is deliberate to ensure data.length==7
            // throws an index out-of-bound exception.
            final int m = left + (len >>> 1);
            final int p4 = m - (ninth >> 1);
            final int p3 = p4 - ninth;
            final int p2 = p3 - ninth;
            final int p1 = p2 - ninth;
            final int p5 = m + (ninth >> 1) + 1;
            final int p6 = p5 + ninth;
            final int p7 = p6 + ninth;
            final int p8 = p7 + ninth;
            Sorting.sort8(data, p1, p2, p3, p4, p5, p6, p7, p8);
            pivot2[0] = p6;
            return p3;
        }

        @Override
        int[] getSampledIndices(int left, int right) {
            final int len = right - left;
            final int ninth = Math.max(1, (len >>> 4) + (len >>> 5) + (len >>> 6));
            final int m = left + (len >>> 1);
            final int p4 = m - (ninth >> 1);
            final int p3 = p4 - ninth;
            final int p2 = p3 - ninth;
            final int p1 = p2 - ninth;
            final int p5 = m + (ninth >> 1) + 1;
            final int p6 = p5 + ninth;
            final int p7 = p6 + ninth;
            final int p8 = p7 + ninth;
            return new int[] {p1, p2, p3, p4, p5, p6, p7, p8};
        }

        @Override
        int samplingEffect() {
            return SORT;
        }
    },
    /**
     * Pivot around the 4th and 8th values from 11 approximately uniformly spaced within the range.
     * Uses points +/- twelfths from the median: ..., m - 1/12, m, m + 1/12, ... .
     *
     * <p>Requires {@code right - left >= 10}.
     *
     * <p>Warning: This has the side effect that the 11 values are also sorted.
     *
     * <p>On sorted data the tertiles are: 0.3460 0.6540
     * <p>On random data the tertiles are:
     * <pre>
     *         min      max     mean       sd   median     skew
     * [1]  0.0060   0.9000   0.3328   0.1301   0.3230   0.3624
     * [2]  0.0100   0.9190   0.3345   0.1299   0.3250   0.3643
     * [3]  0.0060   0.8970   0.3327   0.1302   0.3230   0.3653
     * </pre>
     */
    SORT_11 {
        @Override
        int pivotIndex(double[] data, int left, int right, int[] pivot2) {
            // 1/12 = 8/96 ~ 1/16 + 1/32 ~ 9/96 : 0.8333 ~ 0.09375
            // Ensure the value is above zero to choose different points!
            // This is safe if len >= 10.
            final int len = right - left;
            final int twelfth = Math.max(1, (len >>> 4) + (len >>> 6));
            final int p6 = left + (len >>> 1);
            final int p5 = p6 - twelfth;
            final int p4 = p5 - twelfth;
            final int p3 = p4 - twelfth;
            final int p2 = p3 - twelfth;
            final int p1 = p2 - twelfth;
            final int p7 = p6 + twelfth;
            final int p8 = p7 + twelfth;
            final int p9 = p8 + twelfth;
            final int p10 = p9 + twelfth;
            final int p11 = p10 + twelfth;
            Sorting.sort11(data, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11);
            pivot2[0] = p8;
            return p4;
        }

        @Override
        int[] getSampledIndices(int left, int right) {
            final int len = right - left;
            final int twelfth = Math.max(1, (len >>> 4) + (len >>> 6));
            final int p6 = left + (len >>> 1);
            final int p5 = p6 - twelfth;
            final int p4 = p5 - twelfth;
            final int p3 = p4 - twelfth;
            final int p2 = p3 - twelfth;
            final int p1 = p2 - twelfth;
            final int p7 = p6 + twelfth;
            final int p8 = p7 + twelfth;
            final int p9 = p8 + twelfth;
            final int p10 = p9 + twelfth;
            final int p11 = p10 + twelfth;
            return new int[] {p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11};
        }

        @Override
        int samplingEffect() {
            return SORT;
        }
    };

    /** Sampled points are unchanged. */
    static final int UNCHANGED = 0;
    /** Sampled points are partially sorted. */
    static final int PARTIAL_SORT = 0x1;
    /** Sampled points are sorted. */
    static final int SORT = 0x2;

    /**
     * Find two pivot indices of the array so that partitioning into 3-regions can be made.
     *
     * <pre>{@code
     * left <= p1 <= p2 <= right
     * }</pre>
     *
     * <p>Returns two pivots so that {@code data[p1] <= data[p2]}.
     *
     * @param data Array.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param pivot2 Second pivot.
     * @return first pivot
     */
    abstract int pivotIndex(double[] data, int left, int right, int[] pivot2);

    // The following methods allow the strategy and side effects to be tested

    /**
     * Get the indices of points that will be sampled.
     *
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @return the indices
     */
    abstract int[] getSampledIndices(int left, int right);

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
