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

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;

/**
 * Test cases for the {@link LinearCombination} class.
 */
class LinearCombinationTest {
    @Test
    void testDimensionMismatch() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> LinearCombination.value(new double[1], new double[2]));
    }

    // MATH-1005
    @Test
    void testSingleElementArray() {
        final double[] a = {1.23456789};
        final double[] b = {98765432.1};

        Assertions.assertEquals(a[0] * b[0], LinearCombination.value(a, b));
    }

    @Test
    void testTwoSums() {
        final BigDecimal[] aFN = new BigDecimal[] {
            BigDecimal.valueOf(-1321008684645961L),
            BigDecimal.valueOf(-5774608829631843L),
            BigDecimal.valueOf(-7645843051051357L),
        };
        final BigDecimal[] aFD = new BigDecimal[] {
            BigDecimal.valueOf(268435456L),
            BigDecimal.valueOf(268435456L),
            BigDecimal.valueOf(8589934592L)
        };
        final BigDecimal[] bFN = new BigDecimal[] {
            BigDecimal.valueOf(-5712344449280879L),
            BigDecimal.valueOf(-4550117129121957L),
            BigDecimal.valueOf(8846951984510141L)
        };
        final BigDecimal[] bFD = new BigDecimal[] {
            BigDecimal.valueOf(2097152L),
            BigDecimal.valueOf(2097152L),
            BigDecimal.valueOf(131072L)
        };

        final int len = aFN.length;
        final double[] a = new double[len];
        final double[] b = new double[len];
        for (int i = 0; i < len; i++) {
            a[i] = aFN[i].doubleValue() / aFD[i].doubleValue();
            b[i] = bFN[i].doubleValue() / bFD[i].doubleValue();
        }

        // Ensure "array" and "inline" implementations give the same result.
        final double abSumInline = LinearCombination.value(a[0], b[0],
                                                           a[1], b[1],
                                                           a[2], b[2]);
        final double abSumArray = LinearCombination.value(a, b);
        Assertions.assertEquals(abSumInline, abSumArray);

        // Compare with arbitrary precision computation.
        BigDecimal result = BigDecimal.ZERO;
        for (int i = 0; i < a.length; i++) {
            result = result.add(aFN[i].divide(aFD[i]).multiply(bFN[i].divide(bFD[i])));
        }
        final double expected = result.doubleValue();
        Assertions.assertEquals(expected, abSumInline, 1e-15);

        final double naive = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        Assertions.assertTrue(Math.abs(naive - abSumInline) > 1.5);
    }

    @Test
    void testArrayVsInline() {
        final UniformRandomProvider rng = RandomSource.create(RandomSource.XO_SHI_RO_256_PP);

        double sInline;
        double sArray;
        final double scale = 1e17;
        for (int i = 0; i < 10000; ++i) {
            final double u1 = scale * rng.nextDouble();
            final double u2 = scale * rng.nextDouble();
            final double u3 = scale * rng.nextDouble();
            final double u4 = scale * rng.nextDouble();
            final double v1 = scale * rng.nextDouble();
            final double v2 = scale * rng.nextDouble();
            final double v3 = scale * rng.nextDouble();
            final double v4 = scale * rng.nextDouble();

            // One sum.
            sInline = LinearCombination.value(u1, v1, u2, v2);
            sArray = LinearCombination.value(new double[] {u1, u2},
                                             new double[] {v1, v2});
            Assertions.assertEquals(sInline, sArray);

            // Two sums.
            sInline = LinearCombination.value(u1, v1, u2, v2, u3, v3);
            sArray = LinearCombination.value(new double[] {u1, u2, u3},
                                             new double[] {v1, v2, v3});
            Assertions.assertEquals(sInline, sArray);

            // Three sums.
            sInline = LinearCombination.value(u1, v1, u2, v2, u3, v3, u4, v4);
            sArray = LinearCombination.value(new double[] {u1, u2, u3, u4},
                                             new double[] {v1, v2, v3, v4});
            Assertions.assertEquals(sInline, sArray);
        }
    }

    @Test
    void testHuge() {
        int scale = 971;
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

        final int len = a.length;
        final double[] scaledA = new double[len];
        final double[] scaledB = new double[len];
        for (int i = 0; i < len; ++i) {
            scaledA[i] = Math.scalb(a[i], -scale);
            scaledB[i] = Math.scalb(b[i], scale);
        }
        final double abSumInline = LinearCombination.value(scaledA[0], scaledB[0],
                                                           scaledA[1], scaledB[1],
                                                           scaledA[2], scaledB[2]);
        final double abSumArray = LinearCombination.value(scaledA, scaledB);

        Assertions.assertEquals(abSumInline, abSumArray);
        Assertions.assertEquals(-1.8551294182586248737720779899, abSumInline, 1e-15);

        final double naive = scaledA[0] * scaledB[0] + scaledA[1] * scaledB[1] + scaledA[2] * scaledB[2];
        Assertions.assertTrue(Math.abs(naive - abSumInline) > 1.5);
    }

    @Test
    void testNonFinite() {
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

        Assertions.assertEquals(-3,
                            LinearCombination.value(a[0][0], b[0][0],
                                                    a[0][1], b[0][1]));
        Assertions.assertEquals(6,
                            LinearCombination.value(a[0][0], b[0][0],
                                                    a[0][1], b[0][1],
                                                    a[0][2], b[0][2]));
        Assertions.assertEquals(22,
                            LinearCombination.value(a[0][0], b[0][0],
                                                    a[0][1], b[0][1],
                                                    a[0][2], b[0][2],
                                                    a[0][3], b[0][3]));
        Assertions.assertEquals(22, LinearCombination.value(a[0], b[0]));

        Assertions.assertEquals(Double.NEGATIVE_INFINITY,
                            LinearCombination.value(a[1][0], b[1][0],
                                                    a[1][1], b[1][1]));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY,
                            LinearCombination.value(a[1][0], b[1][0],
                                                    a[1][1], b[1][1],
                                                    a[1][2], b[1][2]));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY,
                            LinearCombination.value(a[1][0], b[1][0],
                                                    a[1][1], b[1][1],
                                                    a[1][2], b[1][2],
                                                    a[1][3], b[1][3]));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, LinearCombination.value(a[1], b[1]));

        Assertions.assertEquals(-3,
                            LinearCombination.value(a[2][0], b[2][0],
                                                    a[2][1], b[2][1]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                            LinearCombination.value(a[2][0], b[2][0],
                                                    a[2][1], b[2][1],
                                                    a[2][2], b[2][2]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                            LinearCombination.value(a[2][0], b[2][0],
                                                    a[2][1], b[2][1],
                                                    a[2][2], b[2][2],
                                                    a[2][3], b[2][3]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, LinearCombination.value(a[2], b[2]));

        Assertions.assertEquals(Double.NEGATIVE_INFINITY,
                            LinearCombination.value(a[3][0], b[3][0],
                                                    a[3][1], b[3][1]));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY,
                            LinearCombination.value(a[3][0], b[3][0],
                                                    a[3][1], b[3][1],
                                                    a[3][2], b[3][2]));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY,
                            LinearCombination.value(a[3][0], b[3][0],
                                                    a[3][1], b[3][1],
                                                    a[3][2], b[3][2],
                                                    a[3][3], b[3][3]));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, LinearCombination.value(a[3], b[3]));

        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                            LinearCombination.value(a[4][0], b[4][0],
                                                    a[4][1], b[4][1]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                            LinearCombination.value(a[4][0], b[4][0],
                                                    a[4][1], b[4][1],
                                                    a[4][2], b[4][2]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                            LinearCombination.value(a[4][0], b[4][0],
                                                    a[4][1], b[4][1],
                                                    a[4][2], b[4][2],
                                                    a[4][3], b[4][3]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, LinearCombination.value(a[4], b[4]));

        Assertions.assertEquals(-3,
                            LinearCombination.value(a[5][0], b[5][0],
                                                    a[5][1], b[5][1]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                            LinearCombination.value(a[5][0], b[5][0],
                                                    a[5][1], b[5][1],
                                                    a[5][2], b[5][2]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                            LinearCombination.value(a[5][0], b[5][0],
                                                    a[5][1], b[5][1],
                                                    a[5][2], b[5][2],
                                                    a[5][3], b[5][3]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, LinearCombination.value(a[5], b[5]));

        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                            LinearCombination.value(a[6][0], b[6][0],
                                                    a[6][1], b[6][1]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                            LinearCombination.value(a[6][0], b[6][0],
                                                    a[6][1], b[6][1],
                                                    a[6][2], b[6][2]));
        Assertions.assertEquals(Double.NaN,
                            LinearCombination.value(a[6][0], b[6][0],
                                                    a[6][1], b[6][1],
                                                    a[6][2], b[6][2],
                                                    a[6][3], b[6][3]));
        Assertions.assertEquals(Double.NaN, LinearCombination.value(a[6], b[6]));

        Assertions.assertEquals(Double.NaN,
                            LinearCombination.value(a[7][0], b[7][0],
                                                    a[7][1], b[7][1]));
        Assertions.assertEquals(Double.NaN,
                            LinearCombination.value(a[7][0], b[7][0],
                                                    a[7][1], b[7][1],
                                                    a[7][2], b[7][2]));
        Assertions.assertEquals(Double.NaN,
                            LinearCombination.value(a[7][0], b[7][0],
                                                    a[7][1], b[7][1],
                                                    a[7][2], b[7][2],
                                                    a[7][3], b[7][3]));
        Assertions.assertEquals(Double.NaN, LinearCombination.value(a[7], b[7]));

        Assertions.assertEquals(Double.NEGATIVE_INFINITY,
                            LinearCombination.value(a[8][0], b[8][0],
                                                    a[8][1], b[8][1]));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY,
                            LinearCombination.value(a[8][0], b[8][0],
                                                    a[8][1], b[8][1],
                                                    a[8][2], b[8][2]));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY,
                            LinearCombination.value(a[8][0], b[8][0],
                                                    a[8][1], b[8][1],
                                                    a[8][2], b[8][2],
                                                    a[8][3], b[8][3]));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, LinearCombination.value(a[8], b[8]));

        Assertions.assertEquals(-3,
                            LinearCombination.value(a[9][0], b[9][0],
                                                    a[9][1], b[9][1]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                            LinearCombination.value(a[9][0], b[9][0],
                                                    a[9][1], b[9][1],
                                                    a[9][2], b[9][2]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                            LinearCombination.value(a[9][0], b[9][0],
                                                    a[9][1], b[9][1],
                                                    a[9][2], b[9][2],
                                                    a[9][3], b[9][3]));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, LinearCombination.value(a[9], b[9]));

        Assertions.assertEquals(-Double.MAX_VALUE,
                            LinearCombination.value(a[10][0], b[10][0],
                                                    a[10][1], b[10][1]));
        Assertions.assertEquals(-Double.MAX_VALUE,
                            LinearCombination.value(a[10][0], b[10][0],
                                                    a[10][1], b[10][1],
                                                    a[10][2], b[10][2]));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY,
                            LinearCombination.value(a[10][0], b[10][0],
                                                    a[10][1], b[10][1],
                                                    a[10][2], b[10][2],
                                                    a[10][3], b[10][3]));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, LinearCombination.value(a[10], b[10]));
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
        final double xxMxyHighPrecision = LinearCombination.value(x, x, x, y);
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

        // High precision result using Dekker's multiplier.
        final double m = (1 << 27) + 1;
        // First demonstrate that Dekker's split will create overflow in the high part.
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
        Assertions.assertEquals(expected, LinearCombination.value(a1, b1, a2, b2));
        Assertions.assertEquals(expected, LinearCombination.value(a1, b1, a2, b2, 0, 0));
        Assertions.assertEquals(expected, LinearCombination.value(a1, b1, a2, b2, 0, 0, 0, 0));
        Assertions.assertEquals(expected, LinearCombination.value(new double[] {a1, a2}, new double[] {b1, b2}));
    }
}
