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
package org.apache.commons.numbers.core;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.function.ToDoubleFunction;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NormTest {

    private static final int SMALL_THRESH_EXP = -511;

    private static final int LARGE_THRESH_EXP = +496;

    private static final int RAND_VECTOR_CNT = 1_000;

    private static final int MAX_ULP_ERR = 1;

    private static final double HYPOT_COMPARE_EPS = 1e-2;

    private static final BigDecimal BD_MAX_VALUE = new BigDecimal(Double.MAX_VALUE);
    private static final BigDecimal BD_MIN_NORMAL = new BigDecimal(Double.MIN_NORMAL);

    /** The scale, used to scale the sqrt of the sum of squares. */
    private static final double SCALE = 0x1.0p200;

    /** The scale squared, used to scale the sum of squares. */
    private static final BigDecimal SCALE2 = new BigDecimal(SCALE * SCALE);

    @Test
    void testManhattan_2d() {
        // act/assert
        Assertions.assertEquals(0d, Norm.L1.of(0d, -0d));
        Assertions.assertEquals(3d, Norm.L1.of(-1d, 2d));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, Norm.L1.of(Double.MAX_VALUE, Double.MAX_VALUE));

        Assertions.assertEquals(Double.NaN, Norm.L1.of(Double.NaN, 1d));
        Assertions.assertEquals(Double.NaN, Norm.L1.of(1d, Double.NaN));
        Assertions.assertEquals(Double.NaN, Norm.L1.of(Double.POSITIVE_INFINITY, Double.NaN));

        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Norm.L1.of(Double.POSITIVE_INFINITY, 0d));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Norm.L1.of(0d, Double.POSITIVE_INFINITY));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Norm.L1.of(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
    }

    @Test
    void testManhattan_3d() {
        // act/assert
        Assertions.assertEquals(0d, Norm.L1.of(0d, -0d, 0d));
        Assertions.assertEquals(6d, Norm.L1.of(-1d, 2d, -3d));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, Norm.L1.of(Double.MAX_VALUE, Double.MAX_VALUE, 0d));

        Assertions.assertEquals(Double.NaN, Norm.L1.of(Double.NaN, -2d, 1d));
        Assertions.assertEquals(Double.NaN, Norm.L1.of(-2d, Double.NaN, 1d));
        Assertions.assertEquals(Double.NaN, Norm.L1.of(-2d, 1d, Double.NaN));
        Assertions.assertEquals(Double.NaN, Norm.L1.of(-2d, Double.POSITIVE_INFINITY, Double.NaN));

        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Norm.L1.of(Double.POSITIVE_INFINITY, 2d, -4d));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Norm.L1.of(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -4d));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Norm.L1.of(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
    }

    @Test
    void testManhattan_array() {
        // act/assert
        Assertions.assertThrows(IllegalArgumentException.class, () -> Norm.L1.of(new double[0]));

        Assertions.assertEquals(0d, Norm.L1.of(new double[] {0d, -0d}));
        Assertions.assertEquals(6d, Norm.L1.of(new double[] {-1d, 2d, -3d}));
        Assertions.assertEquals(10d, Norm.L1.of(new double[] {-1d, 2d, -3d, 4d}));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Norm.L1.of(new double[] {Double.MAX_VALUE, Double.MAX_VALUE}));

        Assertions.assertEquals(Double.NaN, Norm.L1.of(new double[] {-2d, Double.NaN, 1d}));
        Assertions.assertEquals(Double.NaN, Norm.L1.of(new double[] {Double.POSITIVE_INFINITY, Double.NaN, 1d}));

        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Norm.L1.of(new double[] {Double.POSITIVE_INFINITY, 0d}));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Norm.L1.of(new double[] {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY}));
    }

    @Test
    void testEuclidean_2d_simple() {
        // act/assert
        Assertions.assertEquals(0d, Norm.L2.of(0d, 0d));
        Assertions.assertEquals(1d, Norm.L2.of(1d, 0d));
        Assertions.assertEquals(1d, Norm.L2.of(0d, 1d));
        Assertions.assertEquals(5d, Norm.L2.of(-3d, 4d));
        Assertions.assertEquals(Double.MIN_VALUE, Norm.L2.of(0d, Double.MIN_VALUE));
        Assertions.assertEquals(Double.MAX_VALUE, Norm.L2.of(Double.MAX_VALUE, 0d));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, Norm.L2.of(Double.MAX_VALUE, Double.MAX_VALUE));

        Assertions.assertEquals(Math.sqrt(2), Norm.L2.of(1d, -1d));

        Assertions.assertEquals(Double.NaN, Norm.L2.of(Double.NaN, -2d));
        Assertions.assertEquals(Double.NaN, Norm.L2.of(Double.NaN, Double.POSITIVE_INFINITY));
        Assertions.assertEquals(Double.NaN, Norm.L2.of(-2d, Double.NaN));
        Assertions.assertEquals(Double.NaN,
                Norm.L2.of(Double.NaN, Double.NEGATIVE_INFINITY));

        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Norm.L2.of(1d, Double.NEGATIVE_INFINITY));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Norm.L2.of(Double.POSITIVE_INFINITY, -1d));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Norm.L2.of(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
    }

    @Test
    void testEuclidean_2d_scaled() {
        // arrange
        final double[] ones = new double[] {1, 1};
        final double[] multiplesOfTen = new double[] {1, 10};

        // act/assert
        checkScaledEuclideanNorm(ones, 0);
        checkScaledEuclideanNorm(ones, LARGE_THRESH_EXP);
        checkScaledEuclideanNorm(ones, LARGE_THRESH_EXP + 1);
        checkScaledEuclideanNorm(ones, -100);
        checkScaledEuclideanNorm(ones, -101);
        checkScaledEuclideanNorm(ones, SMALL_THRESH_EXP);
        checkScaledEuclideanNorm(ones, SMALL_THRESH_EXP - 1);

        checkScaledEuclideanNorm(multiplesOfTen, 0);
        checkScaledEuclideanNorm(multiplesOfTen, -100);
        checkScaledEuclideanNorm(multiplesOfTen, -101);
        checkScaledEuclideanNorm(multiplesOfTen, LARGE_THRESH_EXP - 1);
        checkScaledEuclideanNorm(multiplesOfTen, SMALL_THRESH_EXP);
    }

    @Test
    void testEuclidean_2d_dominantValue() {
        // act/assert
        Assertions.assertEquals(Math.PI, Norm.L2.of(-Math.PI, 0x1.0p-55));
        Assertions.assertEquals(Math.PI, Norm.L2.of(0x1.0p-55, -Math.PI));
    }

    @Test
    void testEuclidean_2d_random() {
        // arrange
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_1024_PP.create(1L);

        // act/assert
        checkEuclideanRandom(2, rng);
    }

    @Test
    void testEuclidean_2d_vsArray() {
        // arrange
        final double[][] inputs = {
            {-4.074598908124454E-9, 9.897869969944898E-28},
            {1.3472131556526359E-27, -9.064577177323565E9},
            {-3.9219339341360245E149, -7.132522817112096E148},
            {-1.4888098520466735E153, -2.9099184907796666E150},
            {-8.659395144898396E-152, -1.123275532302136E-150},
            {-3.660198254902351E-152, -6.656524053354807E-153}
        };

        // act/assert
        for (final double[] input : inputs) {
            Assertions.assertEquals(Norm.L2.of(input), Norm.L2.of(input[0], input[1]),
                () -> "Expected inline method result to equal array result for input " + Arrays.toString(input));
        }
    }

    @Test
    void testEuclidean_2d_vsHypot() {
        // arrange
        final int samples = 1000;
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_1024_PP.create(3L);

        // act/assert
        assertEuclidean2dVersusHypot(-10, +10, samples, rng);
        assertEuclidean2dVersusHypot(0, +20, samples, rng);
        assertEuclidean2dVersusHypot(-20, 0, samples, rng);
        assertEuclidean2dVersusHypot(-20, +20, samples, rng);
        assertEuclidean2dVersusHypot(-100, +100, samples, rng);
        assertEuclidean2dVersusHypot(LARGE_THRESH_EXP - 10, LARGE_THRESH_EXP + 10, samples, rng);
        assertEuclidean2dVersusHypot(SMALL_THRESH_EXP - 10, SMALL_THRESH_EXP + 10, samples, rng);
        assertEuclidean2dVersusHypot(-600, +600, samples, rng);
    }

    /** Assert that the Norms euclidean 2D computation produces similar error behavior to Math.hypot().
     * @param minExp minimum exponent for random inputs
     * @param maxExp maximum exponent for random inputs
     * @param samples sample count
     * @param rng random number generator
     */
    private static void assertEuclidean2dVersusHypot(final int minExp,
                                                     final int maxExp,
                                                     final int samples,
                                                     final UniformRandomProvider rng) {
        // generate random inputs
        final double[][] inputs = new double[samples][];
        for (int i = 0; i < samples; ++i) {
            inputs[i] = DoubleTestUtils.randomArray(2, minExp, maxExp, rng);
        }

        // compute exact results
        final double[] exactResults = new double[samples];
        for (int i = 0; i < samples; ++i) {
            exactResults[i] = exactEuclideanNorm(inputs[i]);
        }

        // compute the std devs
        final UlpErrorStats hypotStats = computeUlpErrorStats(inputs, exactResults, v -> Math.hypot(v[0], v[1]));
        final UlpErrorStats normStats = computeUlpErrorStats(inputs, exactResults, v -> Norm.L2.of(v[0], v[1]));

        // ensure that we are within the ballpark of Math.hypot
        Assertions.assertTrue(normStats.getMean() <= (hypotStats.getMean() + HYPOT_COMPARE_EPS),
            () -> "Expected 2D norm result to have similar error mean to Math.hypot(): hypot error mean= " +
                    hypotStats.getMean() + ", norm error mean= " + normStats.getMean());

        Assertions.assertTrue(normStats.getStdDev() <= (hypotStats.getStdDev() + HYPOT_COMPARE_EPS),
            () -> "Expected 2D norm result to have similar std deviation to Math.hypot(): hypot std dev= " +
                    hypotStats.getStdDev() + ", norm std dev= " + normStats.getStdDev());
    }

    @Test
    void testEuclidean_3d_simple() {
        // act/assert
        Assertions.assertEquals(0d, Norm.L2.of(0d, 0d, 0d));
        Assertions.assertEquals(1d, Norm.L2.of(1d, 0d, 0d));
        Assertions.assertEquals(1d, Norm.L2.of(0d, 1d, 0d));
        Assertions.assertEquals(1d, Norm.L2.of(0d, 0d, 1d));
        Assertions.assertEquals(5 * Math.sqrt(2), Norm.L2.of(-3d, -4d, 5d));
        Assertions.assertEquals(Double.MIN_VALUE, Norm.L2.of(0d, 0d, Double.MIN_VALUE));
        Assertions.assertEquals(Double.MAX_VALUE, Norm.L2.of(Double.MAX_VALUE, 0d, 0d));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Norm.L2.of(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE));

        Assertions.assertEquals(Math.sqrt(3), Norm.L2.of(1d, -1d, 1d));

        Assertions.assertEquals(Double.NaN, Norm.L2.of(Double.NaN, -2d, 0d));
        Assertions.assertEquals(Double.NaN, Norm.L2.of(-2d, Double.NaN, 0d));
        Assertions.assertEquals(Double.NaN, Norm.L2.of(-2d, 0d, Double.NaN));
        Assertions.assertEquals(Double.NaN,
                Norm.L2.of(Double.POSITIVE_INFINITY, Double.NaN, 1d));

        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Norm.L2.of(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 1d));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Norm.L2.of(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
    }

    @Test
    void testEuclidean_3d_scaled() {
        // arrange
        final double[] ones = new double[] {1, 1, 1};
        final double[] multiplesOfTen = new double[] {1, 10, 100};

        // act/assert
        checkScaledEuclideanNorm(ones, 0);
        checkScaledEuclideanNorm(ones, LARGE_THRESH_EXP);
        checkScaledEuclideanNorm(ones, LARGE_THRESH_EXP + 1);
        checkScaledEuclideanNorm(ones, -100);
        checkScaledEuclideanNorm(ones, -101);
        checkScaledEuclideanNorm(ones, SMALL_THRESH_EXP);
        checkScaledEuclideanNorm(ones, SMALL_THRESH_EXP - 1);

        checkScaledEuclideanNorm(multiplesOfTen, 0);
        checkScaledEuclideanNorm(multiplesOfTen, -100);
        checkScaledEuclideanNorm(multiplesOfTen, -101);
        checkScaledEuclideanNorm(multiplesOfTen, LARGE_THRESH_EXP - 1);
        checkScaledEuclideanNorm(multiplesOfTen, SMALL_THRESH_EXP - 1);
    }

    @Test
    void testEuclidean_3d_random() {
        // arrange
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_1024_PP.create(1L);

        // act/assert
        checkEuclideanRandom(3, rng);
    }

    @Test
    void testEuclidean_3d_vsArray() {
        // arrange
        final double[][] inputs = {
            {-4.074598908124454E-9, 9.897869969944898E-28, 7.849935157082846E-14},
            {1.3472131556526359E-27, -9.064577177323565E9, 323771.526282239},
            {-3.9219339341360245E149, -7.132522817112096E148, -3.427334456813165E147},
            {-1.4888098520466735E153, -2.9099184907796666E150, 1.0144962310234785E152},
            {-8.659395144898396E-152, -1.123275532302136E-150, -2.151505326692001E-152},
            {-3.660198254902351E-152, -6.656524053354807E-153, -3.198606556986218E-154}
        };

        // act/assert
        for (final double[] input : inputs) {
            Assertions.assertEquals(Norm.L2.of(input), Norm.L2.of(input[0], input[1], input[2]),
                () -> "Expected inline method result to equal array result for input " + Arrays.toString(input));
        }
    }

    @Test
    void testEuclidean_array_simple() {
        // act/assert
        Assertions.assertThrows(IllegalArgumentException.class, () -> Norm.L2.of(new double[0]));

        Assertions.assertEquals(5d, Norm.L2.of(new double[] {-3d, 4d}));

        Assertions.assertEquals(Math.sqrt(2), Norm.L2.of(new double[] {1d, -1d}));
        Assertions.assertEquals(Math.sqrt(3), Norm.L2.of(new double[] {1d, -1d, 1d}));
        Assertions.assertEquals(2, Norm.L2.of(new double[] {1d, -1d, 1d, -1d}));

        final double[] longVec = new double[] {-0.9, 8.7, -6.5, -4.3, -2.1, 0, 1.2, 3.4, -5.6, 7.8, 9.0};
        Assertions.assertEquals(directEuclideanNorm(longVec), Norm.L2.of(longVec));

        Assertions.assertEquals(Double.MIN_VALUE, Norm.L2.of(new double[] {0d, Double.MIN_VALUE}));
        Assertions.assertEquals(Double.MAX_VALUE, Norm.L2.of(new double[] {Double.MAX_VALUE, 0d}));

        final double[] maxVec = new double[1000];
        Arrays.fill(maxVec, Double.MAX_VALUE);
        Assertions.assertEquals(Double.POSITIVE_INFINITY, Norm.L2.of(maxVec));

        final double[] largeThreshVec = new double[1000];
        Arrays.fill(largeThreshVec, 0x1.0p496);
        Assertions.assertEquals(Math.sqrt(largeThreshVec.length) * largeThreshVec[0], Norm.L2.of(largeThreshVec));

        Assertions.assertEquals(Double.NaN, Norm.L2.of(new double[] {-2d, Double.NaN, 1d}));
        Assertions.assertEquals(Double.NaN,
                Norm.L2.of(new double[] {Double.POSITIVE_INFINITY, Double.NaN}));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Norm.L2.of(new double[] {Double.POSITIVE_INFINITY, 1, 0}));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Norm.L2.of(new double[] {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Norm.L2.of(new double[] {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY}));
    }

    @Test
    void testEuclidean_array_scaled() {
        // arrange
        final double[] ones = new double[] {1, 1, 1, 1};
        final double[] multiplesOfTen = new double[] {1, 10, 100, 1000};

        // act/assert
        checkScaledEuclideanNorm(ones, 0);
        checkScaledEuclideanNorm(ones, LARGE_THRESH_EXP);
        checkScaledEuclideanNorm(ones, LARGE_THRESH_EXP + 1);
        checkScaledEuclideanNorm(ones, SMALL_THRESH_EXP);
        checkScaledEuclideanNorm(ones, SMALL_THRESH_EXP - 1);

        checkScaledEuclideanNorm(multiplesOfTen, 1);
        checkScaledEuclideanNorm(multiplesOfTen, LARGE_THRESH_EXP - 1);
        checkScaledEuclideanNorm(multiplesOfTen, SMALL_THRESH_EXP - 1);
    }

    @Test
    void testEuclidean_array_random() {
        // arrange
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_1024_PP.create(1L);

        // act/assert
        checkEuclideanRandom(2, rng);
        checkEuclideanRandom(3, rng);
        checkEuclideanRandom(4, rng);
        checkEuclideanRandom(10, rng);
        checkEuclideanRandom(100, rng);
    }

    @Test
    void testMaximum_2d() {
        // act/assert
        Assertions.assertEquals(0d, Norm.LINF.of(0d, -0d));
        Assertions.assertEquals(2d, Norm.LINF.of(1d, -2d));
        Assertions.assertEquals(3d, Norm.LINF.of(3d, 1d));
        Assertions.assertEquals(Double.MAX_VALUE, Norm.LINF.of(Double.MAX_VALUE, Double.MAX_VALUE));

        Assertions.assertEquals(Double.NaN, Norm.LINF.of(Double.NaN, 0d));
        Assertions.assertEquals(Double.NaN, Norm.LINF.of(0d, Double.NaN));
        Assertions.assertEquals(Double.NaN, Norm.LINF.of(Double.POSITIVE_INFINITY, Double.NaN));

        Assertions.assertEquals(Double.POSITIVE_INFINITY, Norm.LINF.of(Double.POSITIVE_INFINITY, 0d));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, Norm.LINF.of(Double.NEGATIVE_INFINITY, 0d));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, Norm.LINF.of(0d, Double.NEGATIVE_INFINITY));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Norm.LINF.of(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
    }

    @Test
    void testMaximum_3d() {
        // act/assert
        Assertions.assertEquals(0d, Norm.LINF.of(0d, -0d, 0d));
        Assertions.assertEquals(3d, Norm.LINF.of(1d, -2d, 3d));
        Assertions.assertEquals(4d, Norm.LINF.of(-4d, -2d, 3d));
        Assertions.assertEquals(Double.MAX_VALUE, Norm.LINF.of(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE));

        Assertions.assertEquals(Double.NaN, Norm.LINF.of(Double.NaN, 3d, 0d));
        Assertions.assertEquals(Double.NaN, Norm.LINF.of(3d, Double.NaN, 0d));
        Assertions.assertEquals(Double.NaN, Norm.LINF.of(3d, 0d, Double.NaN));
        Assertions.assertEquals(Double.NaN, Norm.LINF.of(Double.POSITIVE_INFINITY, 0d, Double.NaN));

        Assertions.assertEquals(Double.POSITIVE_INFINITY, Norm.LINF.of(Double.POSITIVE_INFINITY, 0d, 1d));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, Norm.LINF.of(0d, Double.POSITIVE_INFINITY, 1d));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, Norm.LINF.of(0d, 1d, Double.NEGATIVE_INFINITY));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Norm.LINF.of(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
    }

    @Test
    void testMaximum_array() {
        // act/assert
        Assertions.assertThrows(IllegalArgumentException.class, () -> Norm.LINF.of(new double[0]));

        Assertions.assertEquals(0d, Norm.LINF.of(new double[] {0d, -0d}));
        Assertions.assertEquals(3d, Norm.LINF.of(new double[] {-1d, 2d, -3d}));
        Assertions.assertEquals(4d, Norm.LINF.of(new double[] {-1d, 2d, -3d, 4d}));
        Assertions.assertEquals(Double.MAX_VALUE, Norm.LINF.of(new double[] {Double.MAX_VALUE, Double.MAX_VALUE}));

        Assertions.assertEquals(Double.NaN, Norm.LINF.of(new double[] {-2d, Double.NaN, 1d}));
        Assertions.assertEquals(Double.NaN, Norm.LINF.of(new double[] {Double.POSITIVE_INFINITY, Double.NaN}));

        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Norm.LINF.of(new double[] {0d, Double.POSITIVE_INFINITY}));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Norm.LINF.of(new double[] {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY}));
    }

    /** Check a number of random vectors of length {@code len} with various exponent
     * ranges.
     * @param len vector array length
     * @param rng random number generator
     * @param fn euclidean norm test function
     */
    private static void checkEuclideanRandom(final int len,
                                             final UniformRandomProvider rng) {
        checkEuclideanRandom(len, +600, +620, rng);
        checkEuclideanRandom(len, LARGE_THRESH_EXP - 10, LARGE_THRESH_EXP + 10, rng);
        checkEuclideanRandom(len, +400, +420, rng);
        checkEuclideanRandom(len, +100, +120, rng);
        checkEuclideanRandom(len, -10, +10, rng);
        checkEuclideanRandom(len, -120, -100, rng);
        checkEuclideanRandom(len, -420, -400, rng);
        checkEuclideanRandom(len, SMALL_THRESH_EXP - 10, SMALL_THRESH_EXP + 10, rng);
        checkEuclideanRandom(len, -620, -600, rng);

        checkEuclideanRandom(len, -600, +600, rng);
    }

    /** Check a number of random vectors of length {@code len} with elements containing
     * exponents in the range {@code [minExp, maxExp]}.
     * @param len vector array length
     * @param minExp min exponent
     * @param maxExp max exponent
     * @param rng random number generator
     * @param fn euclidean norm test function
     */
    private static void checkEuclideanRandom(final int len,
                                             final int minExp,
                                             final int maxExp,
                                             final UniformRandomProvider rng) {
        for (int i = 0; i < RAND_VECTOR_CNT; ++i) {
            // arrange
            final double[] v = DoubleTestUtils.randomArray(len, minExp, maxExp, rng);

            final double exact = exactEuclideanNorm(v);
            final double direct = directEuclideanNorm(v);

            // act
            final double actual = Norm.L2.of(v);

            // assert
            Assertions.assertTrue(Double.isFinite(actual), () ->
                "Computed norm was not finite; vector= " + Arrays.toString(v) + ", exact= " + exact +
                ", direct= " + direct + ", actual= " + actual);

            final int ulpError = Math.abs(DoubleTestUtils.computeUlpDifference(exact, actual));

            Assertions.assertTrue(ulpError <= MAX_ULP_ERR, () ->
                "Computed norm ulp error exceeds bounds; vector= " + Arrays.toString(v) +
                ", exact= " + exact + ", actual= " + actual + ", ulpError= " + ulpError);
        }
    }

    /** Assert that {@code directNorm(v) * 2^scaleExp = fn(v * 2^scaleExp)}.
     * @param v unscaled vector
     * @param scaleExp scale factor exponent
     */
    private static void checkScaledEuclideanNorm(final double[] v,
                                                 final int scaleExp) {

        final double scale = Math.scalb(1d, scaleExp);
        final double[] scaledV = new double[v.length];
        for (int i = 0; i < v.length; ++i) {
            scaledV[i] = v[i] * scale;
        }

        final double norm = directEuclideanNorm(v);
        final double scaledNorm = Norm.L2.of(scaledV);

        Assertions.assertEquals(norm * scale, scaledNorm);
    }

    /** Direct euclidean norm computation.
     * @param v array
     * @return euclidean norm using direct summation.
     */
    private static double directEuclideanNorm(final double[] v) {
        double n = 0;
        for (int i = 0; i < v.length; i++) {
            n += v[i] * v[i];
        }
        return Math.sqrt(n);
    }

    /** Compute the exact double value of the vector norm using BigDecimals
     * with a math context of {@link MathContext#DECIMAL128}.
     * @param v array
     * @return euclidean norm using BigDecimal with MathContext.DECIMAL128
     */
    private static double exactEuclideanNorm(final double[] v) {
        final MathContext ctx = MathContext.DECIMAL128;

        BigDecimal sum = BigDecimal.ZERO;
        for (final double d : v) {
            sum = sum.add(new BigDecimal(d).pow(2), ctx);
        }
        if (sum.equals(BigDecimal.ZERO)) {
            return 0;
        }

        // Java 9+:
        // sum.sqrt(ctx).doubleValue()

        // Require the sum to be in the range of a double for conversion before sqrt().
        // We scale by a power of 2. Rescaling uses the square root of this which is also
        // a power of 2 and can be accumulated for exact rescaling.
        double rescale = 1.0;
        if (sum.compareTo(BD_MIN_NORMAL) < 0) {
            while (sum.compareTo(BD_MIN_NORMAL) < 0) {
                sum = sum.multiply(SCALE2);
                rescale /= SCALE;
            }
        } else if (sum.compareTo(BD_MAX_VALUE) > 0) {
            while (sum.compareTo(BD_MAX_VALUE) > 0) {
                sum = sum.divide(SCALE2);
                rescale *= SCALE;
            }
        }

        return Math.sqrt(sum.doubleValue()) * rescale;
    }

    /** Compute statistics for the ulp error of {@code fn} for the given inputs and
     * array of exact results.
     * @param inputs sample inputs
     * @param exactResults array containing the exact expected results
     * @param fn function to perform the computation
     * @return ulp error statistics
     */
    private static UlpErrorStats computeUlpErrorStats(final double[][] inputs,
                                                      final double[] exactResults,
                                                      ToDoubleFunction<double[]> fn) {

        // compute the ulp errors for each input
        final int[] ulpErrors = new int[inputs.length];
        int sum = 0;
        for (int i = 0; i < inputs.length; ++i) {
            final double exact = exactResults[i];
            final double actual = fn.applyAsDouble(inputs[i]);

            final int error = DoubleTestUtils.computeUlpDifference(exact, actual);
            ulpErrors[i] = error;
            sum += error;
        }

        // compute the mean
        final double mean = sum / (double) ulpErrors.length;

        // compute the std dev
        double diffSumSq = 0d;
        double diff;
        for (int ulpError : ulpErrors) {
            diff = ulpError - mean;
            diffSumSq += diff * diff;
        }

        final double stdDev = Math.sqrt(diffSumSq / (inputs.length - 1));

        return new UlpErrorStats(mean, stdDev);
    }

    /** Class containing ULP error statistics. */
    private static final class UlpErrorStats {

        private final double mean;

        private final double stdDev;

        UlpErrorStats(final double mean, final double stdDev) {
            this.mean = mean;
            this.stdDev = stdDev;
        }

        public double getMean() {
            return mean;
        }

        public double getStdDev() {
            return stdDev;
        }
    }
}
