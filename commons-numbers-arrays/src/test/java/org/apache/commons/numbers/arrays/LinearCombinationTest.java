/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.commons.numbers.arrays;

import org.junit.Assert;
import org.junit.Test;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.numbers.fraction.BigFraction;
    
/**
 * Test cases for the {@link LinearCombination} class.
 */
public class LinearCombinationTest {
    // MATH-1005
    @Test
    public void testSingleElementArray() {
        final double[] a = { 1.23456789 };
        final double[] b = { 98765432.1 };

        Assert.assertEquals(a[0] * b[0], LinearCombination.value(a, b), 0d);
    }

    @Test
    public void testTwoSums() { 
        final BigFraction[] aF = new BigFraction[] {
            new BigFraction(-1321008684645961L, 268435456L),
            new BigFraction(-5774608829631843L, 268435456L),
            new BigFraction(-7645843051051357L, 8589934592L)
        };
        final BigFraction[] bF = new BigFraction[] {
            new BigFraction(-5712344449280879L, 2097152L),
            new BigFraction(-4550117129121957L, 2097152L),
            new BigFraction(8846951984510141L, 131072L)
        };

        final int len = aF.length;
        final double[] a = new double[len];
        final double[] b = new double[len];
        for (int i = 0; i < len; i++) {
            a[i] = aF[i].getNumerator().doubleValue() / aF[i].getDenominator().doubleValue();
            b[i] = bF[i].getNumerator().doubleValue() / bF[i].getDenominator().doubleValue();
        }

        // Ensure "array" and "inline" implementations give the same result.
        final double abSumInline = LinearCombination.value(a[0], b[0],
                                                           a[1], b[1],
                                                           a[2], b[2]);
        final double abSumArray = LinearCombination.value(a, b);
        Assert.assertEquals(abSumInline, abSumArray, 0);

        // Compare with arbitrary precision computation.
        BigFraction result = BigFraction.ZERO;
        for (int i = 0; i < a.length; i++) {
            result = result.add(aF[i].multiply(bF[i]));
        }
        final double expected = result.doubleValue();
        Assert.assertEquals(expected, abSumInline, 1e-15);

        final double naive = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        Assert.assertTrue(Math.abs(naive - abSumInline) > 1.5);
    }

    @Test
    public void testArrayVsInline() {
        final UniformRandomProvider rng = RandomSource.create(RandomSource.XOR_SHIFT_1024_S);

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
            sArray = LinearCombination.value(new double[] { u1, u2 },
                                             new double[] { v1, v2 });
            Assert.assertEquals(sInline, sArray, 0);

            // Two sums.
            sInline = LinearCombination.value(u1, v1, u2, v2, u3, v3);
            sArray = LinearCombination.value(new double[] { u1, u2, u3 },
                                             new double[] { v1, v2, v3 });
            Assert.assertEquals(sInline, sArray, 0);

            // Three sums.
            sInline = LinearCombination.value(u1, v1, u2, v2, u3, v3, u4, v4);
            sArray = LinearCombination.value(new double[] { u1, u2, u3, u4 },
                                             new double[] { v1, v2, v3, v4 });
            Assert.assertEquals(sInline, sArray, 0);
        }
    }

    @Test
    public void testHuge() {
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

        Assert.assertEquals(abSumInline, abSumArray, 0);
        Assert.assertEquals(-1.8551294182586248737720779899, abSumInline, 1e-15);

        final double naive = scaledA[0] * scaledB[0] + scaledA[1] * scaledB[1] + scaledA[2] * scaledB[2];
        Assert.assertTrue(Math.abs(naive - abSumInline) > 1.5);
    }

    @Test
    public void testInfinite() {
        final double[][] a = new double[][] {
            { 1, 2, 3, 4 },
            { 1, Double.POSITIVE_INFINITY, 3, 4 },
            { 1, 2, Double.POSITIVE_INFINITY, 4 },
            { 1, Double.POSITIVE_INFINITY, 3, Double.NEGATIVE_INFINITY },
            { 1, 2, 3, 4 },
            { 1, 2, 3, 4 },
            { 1, 2, 3, 4 },
            { 1, 2, 3, 4 }
        };
        final double[][] b = new double[][] {
            { 1, -2, 3, 4 },
            { 1, -2, 3, 4 },
            { 1, -2, 3, 4 },
            { 1, -2, 3, 4 },
            { 1, Double.POSITIVE_INFINITY, 3, 4 },
            { 1, -2, Double.POSITIVE_INFINITY, 4 },
            { 1, Double.POSITIVE_INFINITY, 3, Double.NEGATIVE_INFINITY },
            { Double.NaN, -2, 3, 4 }
        };

        Assert.assertEquals(-3,
                            LinearCombination.value(a[0][0], b[0][0],
                                                    a[0][1], b[0][1]),
                            1e-10);
        Assert.assertEquals(6,
                            LinearCombination.value(a[0][0], b[0][0],
                                                    a[0][1], b[0][1],
                                                    a[0][2], b[0][2]),
                            1e-10);
        Assert.assertEquals(22,
                            LinearCombination.value(a[0][0], b[0][0],
                                                    a[0][1], b[0][1],
                                                    a[0][2], b[0][2],
                                                    a[0][3], b[0][3]),
                            1e-10);
        Assert.assertEquals(22, LinearCombination.value(a[0], b[0]), 1e-10);

        Assert.assertEquals(Double.NEGATIVE_INFINITY,
                            LinearCombination.value(a[1][0], b[1][0],
                                                    a[1][1], b[1][1]),
                            1e-10);
        Assert.assertEquals(Double.NEGATIVE_INFINITY,
                            LinearCombination.value(a[1][0], b[1][0],
                                                    a[1][1], b[1][1],
                                                    a[1][2], b[1][2]),
                            1e-10);
        Assert.assertEquals(Double.NEGATIVE_INFINITY,
                            LinearCombination.value(a[1][0], b[1][0],
                                                    a[1][1], b[1][1],
                                                    a[1][2], b[1][2],
                                                    a[1][3], b[1][3]),
                            1e-10);
        Assert.assertEquals(Double.NEGATIVE_INFINITY, LinearCombination.value(a[1], b[1]), 1e-10);

        Assert.assertEquals(-3,
                            LinearCombination.value(a[2][0], b[2][0],
                                                    a[2][1], b[2][1]),
                            1e-10);
        Assert.assertEquals(Double.POSITIVE_INFINITY,
                            LinearCombination.value(a[2][0], b[2][0],
                                                    a[2][1], b[2][1],
                                                    a[2][2], b[2][2]),
                            1e-10);
        Assert.assertEquals(Double.POSITIVE_INFINITY,
                            LinearCombination.value(a[2][0], b[2][0],
                                                    a[2][1], b[2][1],
                                                    a[2][2], b[2][2],
                                                    a[2][3], b[2][3]),
                            1e-10);
        Assert.assertEquals(Double.POSITIVE_INFINITY, LinearCombination.value(a[2], b[2]), 1e-10);

        Assert.assertEquals(Double.NEGATIVE_INFINITY,
                            LinearCombination.value(a[3][0], b[3][0],
                                                    a[3][1], b[3][1]),
                            1e-10);
        Assert.assertEquals(Double.NEGATIVE_INFINITY,
                            LinearCombination.value(a[3][0], b[3][0],
                                                    a[3][1], b[3][1],
                                                    a[3][2], b[3][2]),
                            1e-10);
        Assert.assertEquals(Double.NEGATIVE_INFINITY,
                            LinearCombination.value(a[3][0], b[3][0],
                                                    a[3][1], b[3][1],
                                                    a[3][2], b[3][2],
                                                    a[3][3], b[3][3]),
                            1e-10);
        Assert.assertEquals(Double.NEGATIVE_INFINITY, LinearCombination.value(a[3], b[3]), 1e-10);

        Assert.assertEquals(Double.POSITIVE_INFINITY,
                            LinearCombination.value(a[4][0], b[4][0],
                                                    a[4][1], b[4][1]),
                            1e-10);
        Assert.assertEquals(Double.POSITIVE_INFINITY,
                            LinearCombination.value(a[4][0], b[4][0],
                                                    a[4][1], b[4][1],
                                                    a[4][2], b[4][2]),
                            1e-10);
        Assert.assertEquals(Double.POSITIVE_INFINITY,
                            LinearCombination.value(a[4][0], b[4][0],
                                                    a[4][1], b[4][1],
                                                    a[4][2], b[4][2],
                                                    a[4][3], b[4][3]),
                            1e-10);
        Assert.assertEquals(Double.POSITIVE_INFINITY, LinearCombination.value(a[4], b[4]), 1e-10);

        Assert.assertEquals(-3,
                            LinearCombination.value(a[5][0], b[5][0],
                                                    a[5][1], b[5][1]),
                            1e-10);
        Assert.assertEquals(Double.POSITIVE_INFINITY,
                            LinearCombination.value(a[5][0], b[5][0],
                                                    a[5][1], b[5][1],
                                                    a[5][2], b[5][2]),
                            1e-10);
        Assert.assertEquals(Double.POSITIVE_INFINITY,
                            LinearCombination.value(a[5][0], b[5][0],
                                                    a[5][1], b[5][1],
                                                    a[5][2], b[5][2],
                                                    a[5][3], b[5][3]),
                            1e-10);
        Assert.assertEquals(Double.POSITIVE_INFINITY, LinearCombination.value(a[5], b[5]), 1e-10);

        Assert.assertEquals(Double.POSITIVE_INFINITY,
                            LinearCombination.value(a[6][0], b[6][0],
                                                    a[6][1], b[6][1]),
                            1e-10);
        Assert.assertEquals(Double.POSITIVE_INFINITY,
                            LinearCombination.value(a[6][0], b[6][0],
                                                    a[6][1], b[6][1],
                                                    a[6][2], b[6][2]),
                            1e-10);
        Assert.assertTrue(Double.isNaN(LinearCombination.value(a[6][0], b[6][0],
                                                               a[6][1], b[6][1],
                                                               a[6][2], b[6][2],
                                                               a[6][3], b[6][3])));
        Assert.assertTrue(Double.isNaN(LinearCombination.value(a[6], b[6])));

        Assert.assertTrue(Double.isNaN(LinearCombination.value(a[7][0], b[7][0],
                                                               a[7][1], b[7][1])));
        Assert.assertTrue(Double.isNaN(LinearCombination.value(a[7][0], b[7][0],
                                                               a[7][1], b[7][1],
                                                               a[7][2], b[7][2])));
        Assert.assertTrue(Double.isNaN(LinearCombination.value(a[7][0], b[7][0],
                                                               a[7][1], b[7][1],
                                                               a[7][2], b[7][2],
                                                               a[7][3], b[7][3])));
        Assert.assertTrue(Double.isNaN(LinearCombination.value(a[7], b[7])));
    }
}
