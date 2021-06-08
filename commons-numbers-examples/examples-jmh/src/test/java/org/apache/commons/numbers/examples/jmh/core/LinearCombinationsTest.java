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
package org.apache.commons.numbers.examples.jmh.core;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.SplittableRandom;
import java.util.stream.Stream;

import org.apache.commons.numbers.examples.jmh.core.LinearCombination.FourD;
import org.apache.commons.numbers.examples.jmh.core.LinearCombination.ND;
import org.apache.commons.numbers.examples.jmh.core.LinearCombination.ThreeD;
import org.apache.commons.numbers.examples.jmh.core.LinearCombination.TwoD;
import org.apache.commons.numbers.fraction.BigFraction;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test each implementation of the LinearCombination interface.
 */
class LinearCombinationsTest {
    /** Double.MIN_VALUE as a BigDecimal. Use string constructor to truncate precision to 4.9e-324. */
    private static final BigDecimal MIN = BigDecimal.valueOf(Double.MIN_VALUE);

    /**
     * Provide instances of the LinearCombination interface as arguments.
     *
     * @return the stream
     */
    static Stream<Arguments> provideLinearCombination() {
        return Stream.of(
            Arguments.of(LinearCombinations.Dekker.INSTANCE),
            Arguments.of(LinearCombinations.Dot2s.INSTANCE),
            Arguments.of(LinearCombinations.DotK.DOT_3),
            Arguments.of(LinearCombinations.DotK.DOT_4),
            Arguments.of(LinearCombinations.DotK.DOT_5),
            Arguments.of(LinearCombinations.DotK.DOT_6),
            Arguments.of(LinearCombinations.DotK.DOT_7),
            Arguments.of(LinearCombinations.ExtendedPrecision.INSTANCE),
            Arguments.of(LinearCombinations.ExtendedPrecision.DOUBLE),
            Arguments.of(LinearCombinations.ExtendedPrecision.EXACT),
            Arguments.of(LinearCombinations.ExtendedPrecision.EXACT2),
            Arguments.of(LinearCombinations.Exact.INSTANCE)
        );
    }

    @ParameterizedTest
    @MethodSource("provideLinearCombination")
    void testSingleElementArray(ND fun) {
        final double[] a = {1.23456789};
        final double[] b = {98765432.1};

        Assertions.assertEquals(a[0] * b[0], fun.value(a, b));
    }

    @ParameterizedTest
    @MethodSource("provideLinearCombination")
    void testTwoSums(ND fun) {
        final BigFraction[] aF = new BigFraction[] {
            BigFraction.of(-1321008684645961L, 268435456L),
            BigFraction.of(-5774608829631843L, 268435456L),
            BigFraction.of(-7645843051051357L, 8589934592L)
        };
        final BigFraction[] bF = new BigFraction[] {
            BigFraction.of(-5712344449280879L, 2097152L),
            BigFraction.of(-4550117129121957L, 2097152L),
            BigFraction.of(8846951984510141L, 131072L)
        };
        final ThreeD fun3 = (ThreeD) fun;

        final int len = aF.length;
        final double[] a = new double[len];
        final double[] b = new double[len];
        for (int i = 0; i < len; i++) {
            a[i] = aF[i].getNumerator().doubleValue() / aF[i].getDenominator().doubleValue();
            b[i] = bF[i].getNumerator().doubleValue() / bF[i].getDenominator().doubleValue();
        }

        // Ensure "array" and "inline" implementations give the same result.
        final double abSumInline = fun3.value(a[0], b[0],
                                              a[1], b[1],
                                              a[2], b[2]);
        final double abSumArray = fun.value(a, b);
        Assertions.assertEquals(abSumInline, abSumArray);

        // Compare with arbitrary precision computation.
        BigFraction result = BigFraction.ZERO;
        for (int i = 0; i < a.length; i++) {
            result = result.add(aF[i].multiply(bF[i]));
        }
        final double expected = result.doubleValue();
        Assertions.assertEquals(expected, abSumInline, "Expecting exact result");

        final double naive = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        Assertions.assertTrue(Math.abs(naive - abSumInline) > 1.5);
    }

    @ParameterizedTest
    @MethodSource("provideLinearCombination")
    void testHuge(ND fun) {
        final int scale = 971;
        final double[] a = new double[] {
            -1321008684645961.0 / 268435456.0,
            -5774608829631843.0 / 268435456.0,
            -7645843051051357.0 / 8589934592.0
        };
        final double[] b = new double[] {
            -5712344449280879.0 / 2097152.0,
            -4550117129121957.0 / 2097152.0,
            8846951984510141.0 / 131072.0
        };
        final ThreeD fun3 = (ThreeD) fun;

        final int len = a.length;
        final double[] scaledA = new double[len];
        final double[] scaledB = new double[len];
        for (int i = 0; i < len; ++i) {
            scaledA[i] = Math.scalb(a[i], -scale);
            scaledB[i] = Math.scalb(b[i], scale);
        }
        final double abSumInline = fun3.value(scaledA[0], scaledB[0],
                                              scaledA[1], scaledB[1],
                                              scaledA[2], scaledB[2]);
        final double abSumArray = fun.value(scaledA, scaledB);

        Assertions.assertEquals(abSumInline, abSumArray);
        Assertions.assertEquals(-1.8551294182586248737720779899, abSumInline, "Expecting exact result");

        final double naive = scaledA[0] * scaledB[0] + scaledA[1] * scaledB[1] + scaledA[2] * scaledB[2];
        Assertions.assertTrue(Math.abs(naive - abSumInline) > 1.5);
    }

    @ParameterizedTest
    @MethodSource("provideLinearCombination")
    void testArrayVsInline(ND fun) {
        // Assume the instance implements the inline functions
        final TwoD fun2 = (TwoD) fun;
        final ThreeD fun3 = (ThreeD) fun;
        final FourD fun4 = (FourD) fun;

        final SplittableRandom rng = new SplittableRandom();

        double sInline;
        double sArray;
        final double scale = 1e17;
        for (int i = 0; i < 1000; ++i) {
            final double u1 = scale * rng.nextDouble();
            final double u2 = scale * rng.nextDouble();
            final double u3 = scale * rng.nextDouble();
            final double u4 = scale * rng.nextDouble();
            final double v1 = scale * rng.nextDouble();
            final double v2 = scale * rng.nextDouble();
            final double v3 = scale * rng.nextDouble();
            final double v4 = scale * rng.nextDouble();

            // One sum.
            sInline = fun2.value(u1, v1, u2, v2);
            sArray = fun.value(new double[] {u1, u2},
                               new double[] {v1, v2});
            Assertions.assertEquals(sInline, sArray);

            // Two sums.
            sInline = fun3.value(u1, v1, u2, v2, u3, v3);
            sArray = fun.value(new double[] {u1, u2, u3},
                               new double[] {v1, v2, v3});
            Assertions.assertEquals(sInline, sArray);

            // Three sums.
            sInline = fun4.value(u1, v1, u2, v2, u3, v3, u4, v4);
            sArray = fun.value(new double[] {u1, u2, u3, u4},
                               new double[] {v1, v2, v3, v4});
            Assertions.assertEquals(sInline, sArray);
        }
    }

    @ParameterizedTest
    @MethodSource("provideLinearCombination")
    void testNonFinite(ND fun) {
        final double[][] a = new double[][] {
            {1, 2, 3, 4},
            {1, Double.POSITIVE_INFINITY, 3, 4},
            {1, 2, Double.POSITIVE_INFINITY, 4},
            {1, Double.POSITIVE_INFINITY, 3, Double.NEGATIVE_INFINITY},
            {1, 2, 3, 4},
            {1, 2, 3, 4},
            {1, 2, 3, 4},
            {1, 2, 3, 4},
            {1, Double.MAX_VALUE, 3, 4},
            {1, 2, Double.MAX_VALUE, 4},
            {1, Double.MAX_VALUE / 2, 3, -Double.MAX_VALUE / 4},
        };
        final double[][] b = new double[][] {
            {1, -2, 3, 4},
            {1, -2, 3, 4},
            {1, -2, 3, 4},
            {1, -2, 3, 4},
            {1, Double.POSITIVE_INFINITY, 3, 4},
            {1, -2, Double.POSITIVE_INFINITY, 4},
            {1, Double.POSITIVE_INFINITY, 3, Double.NEGATIVE_INFINITY},
            {Double.NaN, -2, 3, 4},
            {1, -2, 3, 4},
            {1, -2, 3, 4},
            {1, -2, 3, 4},
        };

        final TwoD fun2 = (TwoD) fun;
        final ThreeD fun3 = (ThreeD) fun;
        final FourD fun4 = (FourD) fun;

        Assertions.assertEquals(-3, fun2.value(a[0][0], b[0][0],
                                               a[0][1], b[0][1]));
        Assertions.assertEquals(6, fun3.value(a[0][0], b[0][0],
                                              a[0][1], b[0][1],
                                              a[0][2], b[0][2]));
        Assertions.assertEquals(22, fun4.value(a[0][0], b[0][0],
                                               a[0][1], b[0][1],
                                               a[0][2], b[0][2],
                                               a[0][3], b[0][3]));
        Assertions.assertEquals(22, fun.value(a[0], b[0]));

        Assertions.assertEquals(Double.NEGATIVE_INFINITY, fun2.value(a[1][0], b[1][0],
                                                                     a[1][1], b[1][1]));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, fun3.value(a[1][0], b[1][0],
                                                                     a[1][1], b[1][1],
                                                                     a[1][2], b[1][2]));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, fun4.value(a[1][0], b[1][0],
                                                                     a[1][1], b[1][1],
                                                                     a[1][2], b[1][2],
                                                                     a[1][3], b[1][3]));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, fun.value(a[1], b[1]));

        Assertions.assertEquals(-3, fun2.value(a[2][0], b[2][0],
                                               a[2][1], b[2][1]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, fun3.value(a[2][0], b[2][0],
                                                                     a[2][1], b[2][1],
                                                                     a[2][2], b[2][2]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, fun4.value(a[2][0], b[2][0],
                                                                     a[2][1], b[2][1],
                                                                     a[2][2], b[2][2],
                                                                     a[2][3], b[2][3]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, fun.value(a[2], b[2]));

        Assertions.assertEquals(Double.NEGATIVE_INFINITY, fun2.value(a[3][0], b[3][0],
                                                                     a[3][1], b[3][1]));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, fun3.value(a[3][0], b[3][0],
                                                                     a[3][1], b[3][1],
                                                                     a[3][2], b[3][2]));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, fun4.value(a[3][0], b[3][0],
                                                                     a[3][1], b[3][1],
                                                                     a[3][2], b[3][2],
                                                                     a[3][3], b[3][3]));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, fun.value(a[3], b[3]));

        Assertions.assertEquals(Double.POSITIVE_INFINITY, fun2.value(a[4][0], b[4][0],
                                                                     a[4][1], b[4][1]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, fun3.value(a[4][0], b[4][0],
                                                                     a[4][1], b[4][1],
                                                                     a[4][2], b[4][2]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, fun4.value(a[4][0], b[4][0],
                                                                     a[4][1], b[4][1],
                                                                     a[4][2], b[4][2],
                                                                     a[4][3], b[4][3]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, fun.value(a[4], b[4]));

        Assertions.assertEquals(-3, fun2.value(a[5][0], b[5][0],
                                               a[5][1], b[5][1]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, fun3.value(a[5][0], b[5][0],
                                                                     a[5][1], b[5][1],
                                                                     a[5][2], b[5][2]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, fun4.value(a[5][0], b[5][0],
                                                                     a[5][1], b[5][1],
                                                                     a[5][2], b[5][2],
                                                                     a[5][3], b[5][3]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, fun.value(a[5], b[5]));

        Assertions.assertEquals(Double.POSITIVE_INFINITY, fun2.value(a[6][0], b[6][0],
                                                                     a[6][1], b[6][1]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, fun3.value(a[6][0], b[6][0],
                                                                     a[6][1], b[6][1],
                                                                     a[6][2], b[6][2]));
        Assertions.assertEquals(Double.NaN, fun4.value(a[6][0], b[6][0],
                                                       a[6][1], b[6][1],
                                                       a[6][2], b[6][2],
                                                       a[6][3], b[6][3]));
        Assertions.assertEquals(Double.NaN, fun.value(a[6], b[6]));

        Assertions.assertEquals(Double.NaN, fun2.value(a[7][0], b[7][0],
                                                       a[7][1], b[7][1]));
        Assertions.assertEquals(Double.NaN, fun3.value(a[7][0], b[7][0],
                                                       a[7][1], b[7][1],
                                                       a[7][2], b[7][2]));
        Assertions.assertEquals(Double.NaN, fun4.value(a[7][0], b[7][0],
                                                       a[7][1], b[7][1],
                                                       a[7][2], b[7][2],
                                                       a[7][3], b[7][3]));
        Assertions.assertEquals(Double.NaN, fun.value(a[7], b[7]));

        Assertions.assertEquals(Double.NEGATIVE_INFINITY, fun2.value(a[8][0], b[8][0],
                                                                     a[8][1], b[8][1]));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, fun3.value(a[8][0], b[8][0],
                                                                     a[8][1], b[8][1],
                                                                     a[8][2], b[8][2]));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, fun4.value(a[8][0], b[8][0],
                                                                     a[8][1], b[8][1],
                                                                     a[8][2], b[8][2],
                                                                     a[8][3], b[8][3]));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, fun.value(a[8], b[8]));

        Assertions.assertEquals(-3, fun2.value(a[9][0], b[9][0],
                                               a[9][1], b[9][1]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, fun3.value(a[9][0], b[9][0],
                                                                     a[9][1], b[9][1],
                                                                     a[9][2], b[9][2]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, fun4.value(a[9][0], b[9][0],
                                                                     a[9][1], b[9][1],
                                                                     a[9][2], b[9][2],
                                                                     a[9][3], b[9][3]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, fun.value(a[9], b[9]));

        Assertions.assertEquals(-Double.MAX_VALUE, fun2.value(a[10][0], b[10][0],
                                                              a[10][1], b[10][1]));
        Assertions.assertEquals(-Double.MAX_VALUE, fun3.value(a[10][0], b[10][0],
                                                              a[10][1], b[10][1],
                                                              a[10][2], b[10][2]));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, fun4.value(a[10][0], b[10][0],
                                                                     a[10][1], b[10][1],
                                                                     a[10][2], b[10][2],
                                                                     a[10][3], b[10][3]));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, fun.value(a[10], b[10]));
    }

    /**
     * This creates a scenario where the split product will overflow but the standard
     * precision computation will not. The result is expected to be in extended precision,
     * i.e. the method correctly detects and handles intermediate overflow.
     *
     * <p>Note: This test assumes that LinearCombination computes a split number
     * using Dekker's method. This can result in the high part of the number being
     * greater in magnitude than the the original number due to round-off in the split.
     */
    @Test
    void testOverflow() {
        // Create a simple dot product that is different in high precision and has
        // values that create a high part above the original number. This can be done using
        // a mantissa with almost all bits set to 1.
        final double x = Math.nextDown(2.0);
        final double y = -Math.nextDown(x);
        final double xxMxy = x * x + x * y;
        final double xxMxyHighPrecision = LinearCombinations.DotK.DOT_3.value(x, x, x, y);
        Assertions.assertNotEquals(xxMxy, xxMxyHighPrecision, "High precision result should be different");

        // Scale it close to max value.
        // The current exponent is 0 so the combined scale must be 1023-1 as the
        // partial product x*x and x*y have an exponent 1 higher
        Assertions.assertEquals(0, Math.getExponent(x));
        Assertions.assertEquals(0, Math.getExponent(y));

        final double a1 = Math.scalb(x, 1022 - 30);
        final double b1 = Math.scalb(x, 30);
        final double a2 = a1;
        final double b2 = Math.scalb(y, 30);
        // Verify low precision result is scaled and finite
        final double sxxMxy = Math.scalb(xxMxy, 1022);
        Assertions.assertEquals(sxxMxy, a1 * b1 + a2 * b2);
        Assertions.assertTrue(Double.isFinite(sxxMxy));

        // High precision result.
        // First demonstrate that Dekker's split will create overflow in the high part.
        final double m = (1 << 27) + 1;
        double c;
        c = a1 * m;
        final double ha1 = c - (c - a1);
        c = b1 * m;
        final double hb1 = c - (c - b1);
        c = a2 * m;
        final double ha2 = c - (c - a2);
        c = b2 * m;
        final double hb2 = c - (c - b2);
        Assertions.assertTrue(Double.isFinite(ha1));
        Assertions.assertTrue(Double.isFinite(hb1));
        Assertions.assertTrue(Double.isFinite(ha2));
        Assertions.assertTrue(Double.isFinite(hb2));
        // High part should be bigger in magnitude
        Assertions.assertTrue(Math.abs(ha1) > Math.abs(a1));
        Assertions.assertTrue(Math.abs(hb1) > Math.abs(b1));
        Assertions.assertTrue(Math.abs(ha2) > Math.abs(a2));
        Assertions.assertTrue(Math.abs(hb2) > Math.abs(b2));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, ha1 * hb1, "Expected split high part to overflow");
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, ha2 * hb2, "Expected split high part to overflow");

        // LinearCombination should detect and handle intermediate overflow and return the
        // high precision result.
        final double expected = Math.scalb(xxMxyHighPrecision, 1022);
        Assertions.assertEquals(expected, LinearCombinations.DotK.DOT_3.value(a1, b1, a2, b2));
        Assertions.assertEquals(expected, LinearCombinations.DotK.DOT_3.value(a1, b1, a2, b2, 0, 0));
        Assertions.assertEquals(expected, LinearCombinations.DotK.DOT_3.value(a1, b1, a2, b2, 0, 0, 0, 0));
        Assertions.assertEquals(expected, LinearCombinations.DotK.DOT_3.value(new double[] {a1, a2}, new double[] {b1, b2}));
    }

    /**
     * This is an extreme case of the sum x^2 + y^2 - 1 when x^2 + y^2 are 1.0 within
     * floating-point error but if performed using high precision subtracting 1.0 is not 0.0.
     * This case is derived from computations on a complex cis number.
     */
    @Test
    void testCisNumber() {
        final double theta = 5.992112452678286E-7;
        final double x = Math.cos(theta);
        final double y = Math.sin(theta);
        assertValue(LinearCombinations.DotK.DOT_3.value(x, x, y, y, 1, -1),
                new double[] {x, y, 1},
                new double[] {x, y, -1});
    }

    /**
     * Test the sum of vectors composed of sub-vectors of [a1, a2, a3, a4] * [a1, a2, -a3, -a4]
     * where a1^2 + a2^2 = 1 and a3^2 + a4^2 = 1 such that the sum is approximately 0 every
     * 4 products. This is a test that is failed by various implementations that accumulate the
     * round-off sum in single or 2-fold precision.
     */
    @Test
    void testSumZero() {
        // Fixed seed for stability
        final UniformRandomProvider rng = RandomSource.create(RandomSource.XO_RO_SHI_RO_128_PP, 876543L);
        final int size = 10;
        // Create random doublets of pairs of numbers that sum to 1 or -1.
        for (int length = 4; length <= 12; length += 4) {
            final double[] a = new double[length];
            final double[] b = new double[length];
            for (int i = 0; i < size; i++) {
                // Flip-flop above and below zero
                double sign = 1;
                for (int k = 0; k < length; k += 4) {
                    // Create 2 complex cis numbers
                    final double theta1 = rng.nextDouble() * Math.PI / 2;
                    final double theta2 = rng.nextDouble() * Math.PI / 2;
                    a[k + 0] = b[k + 0] = Math.cos(theta1);
                    a[k + 1] = b[k + 1] = Math.sin(theta1);
                    a[k + 2] = b[k + 2] = Math.cos(theta2);
                    a[k + 3] = b[k + 3] = Math.sin(theta2);
                    a[k + 0] *= sign;
                    a[k + 1] *= sign;
                    a[k + 2] *= sign;
                    a[k + 3] *= sign;
                    // Invert second pair.
                    // The sum of the pairs should be zero +/- floating point error.
                    a[k + 2] = -a[k + 2];
                    a[k + 3] = -a[k + 3];
                    sign = -sign;
                }
                assertValue(LinearCombinations.DotK.DOT_3.value(a, b), a, b);
            }
        }
    }

    /**
     * Compute the sum of the product of factors in arbitrary precision and compare it to the
     * given value.
     *
     * @param value the value
     * @param a factors
     * @param b factors
     */
    private static void assertValue(double value, double[] a, double[] b) {
        final double expected = computeValue(a, b);
        Assertions.assertEquals(expected, value, Math.ulp(expected),
            () -> "Difference in Ulps = " + ulps(expected, value));
    }

    /**
     * Compute the sum of the product of pairs of input data using BigDecimal.
     * The BigDecimal is not allowed to underflow Double.MIN_VALUE.
     *
     * @param data the data
     * @return the sum of products
     */
    private static double computeValue(double[] a, double[] b) {
        BigDecimal sum = new BigDecimal(a[0]).multiply(new BigDecimal(b[0]));
        for (int i = 1; i < a.length; i++) {
            sum = clip(sum.add(clip(new BigDecimal(a[i]).multiply(new BigDecimal(b[i])))));
        }
        return sum.doubleValue();
    }

    /**
     * Compute the units of least precision (ulps) between the two numbers.
     *
     * @param a first number
     * @param b second number
     * @return the ulps
     */
    private static long ulps(double a, double b) {
        long x = Double.doubleToLongBits(a);
        long y = Double.doubleToLongBits(b);
        if (x != y) {
            if ((x ^ y) < 0L) {
                // Opposite signs. Measure the combined distance to zero.
                if (x < 0) {
                    final long tmp = x;
                    x = y;
                    y = tmp;
                }
                return (x - Double.doubleToLongBits(0.0)) + (y - Double.doubleToLongBits(-0.0)) + 1;
            }
            return Math.abs(x - y);
        }
        return 0;
    }

    /**
     * Clip the value to the minimum value that can be stored by a double.
     * Ideally this should round BigDecimal to values occupied by sub-normal numbers.
     * That is non-trivial so this just removes excess precision in the significand and
     * clips it to Double.MIN_VALUE or zero if the value is very small. The ultimate use for
     * the BigDecimal is rounded to the closest double so this method is adequate. It would
     * take many summations of extended precision sub-normal numbers to create more
     * than a few ULP difference to the final double value
     *
     * <p>In data output by the various tests the values have never been known to require
     * clipping so this is just a safety threshold.
     *
     * @param a the value
     * @return the clipped value
     */
    private static BigDecimal clip(BigDecimal a) {
        // Min value is approx 4.9e-324. Anything with fewer decimal digits to the right of the
        // decimal point is OK.
        if (a.scale() < 324) {
            return a;
        }
        // Reduce the scale
        final BigDecimal b = a.setScale(MIN.scale(), RoundingMode.HALF_UP);
        // Clip to min value
        final BigDecimal bb = b.abs();
        if (bb.compareTo(MIN) < 0) {
            // Note the number may be closer to MIN than zero so do rounding
            if (MIN.subtract(bb).compareTo(bb) < 0) {
                // Closer to MIN
                return a.signum() == -1 ? MIN.negate() : MIN;
            }
            // Closer to zero
            return BigDecimal.ZERO;
        }
        // Anything above min is allowed.
        return b;
    }

    /**
     * Test the clip method does what it specifies.
     */
    @Test
    void testClip() {
        // min value is not affected
        Assertions.assertEquals(Double.MIN_VALUE, clip(MIN).doubleValue());
        Assertions.assertEquals(-Double.MIN_VALUE, clip(MIN.negate()).doubleValue());
        // Round-up to min
        Assertions.assertEquals(Double.MIN_VALUE, clip(MIN.divide(new BigDecimal(2))).doubleValue());
        Assertions.assertEquals(-Double.MIN_VALUE, clip(MIN.negate().divide(new BigDecimal(2))).doubleValue());
        // Round down to zero
        Assertions.assertEquals(0, clip(MIN.divide(new BigDecimal(2.1), MathContext.DECIMAL64)).doubleValue());
        Assertions.assertEquals(0, clip(MIN.negate().divide(new BigDecimal(2.1), MathContext.DECIMAL64)).doubleValue());
        // It does not matter if BigDecimal is more precise than a sub-normal number
        // when the output is ultimately rounded to a double.
        Assertions.assertEquals(Double.MIN_VALUE, clip(MIN.multiply(new BigDecimal(1.1))).doubleValue());
        Assertions.assertEquals(Double.MIN_VALUE, clip(MIN.multiply(new BigDecimal(1.5))).doubleValue());
        Assertions.assertEquals(Double.MIN_VALUE * 2, clip(MIN.multiply(new BigDecimal(1.6))).doubleValue());
    }
}
