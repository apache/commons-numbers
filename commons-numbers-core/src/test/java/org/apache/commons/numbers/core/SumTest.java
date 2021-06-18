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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SumTest {
    @Test
    void testSum_simple() {
        // act/assert
        Assertions.assertEquals(0d, Sum.create().getAsDouble());
        Assertions.assertEquals(Math.PI, Sum.of(Math.PI).getAsDouble());
        Assertions.assertEquals(Math.PI + Math.E, Sum.of(Math.PI, Math.E).getAsDouble());

        Assertions.assertEquals(0, Sum.of(0, 0, 0).getAsDouble());
        Assertions.assertEquals(6, Sum.of(1, 2, 3).getAsDouble());
        Assertions.assertEquals(2, Sum.of(1, -2, 3).getAsDouble());

        Assertions.assertEquals(Double.NaN, Sum.of(Double.NaN, 0, 0).getAsDouble());
        Assertions.assertEquals(Double.NaN, Sum.of(0, Double.NaN, 0).getAsDouble());
        Assertions.assertEquals(Double.NaN, Sum.of(0, 0, Double.NaN).getAsDouble());

        Assertions.assertEquals(Double.NaN, Sum.of(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0)
                .getAsDouble());

        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Sum.of(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE).getAsDouble());

        Assertions.assertEquals(Double.POSITIVE_INFINITY, Sum.of(Double.POSITIVE_INFINITY, 1, 1).getAsDouble());
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, Sum.of(1, Double.NEGATIVE_INFINITY, 1).getAsDouble());
    }

    @Test
    void testSumAccuracy() {
        // arrange
        final double a = 9.999999999;
        final double b = Math.scalb(a, -53);
        final double c = Math.scalb(a, -53);
        final double d = Math.scalb(a, -27);

        // act/assert
        assertSum(a, b, c);
        assertSum(c, b, a);

        assertSum(a, -b, c);
        assertSum(-c, b, -a);

        assertSum(a, b, c, d);
        assertSum(d, c, b, a);

        assertSum(a, -b, c, -d);
        assertSum(d, -c, b, -a);
    }

    @Test
    void testSumAccuracy_array() {
        // arrange
        final double a = 9.999999999;
        final double b = Math.scalb(a, -53);
        final double c = Math.scalb(a, -53);
        final double d = Math.scalb(a, -27);
        final double e = Math.scalb(a, -27);
        final double f = Math.scalb(a, -50);

        // act/assert
        assertArraysum(a);

        assertArraysum(a, b);
        assertArraysum(b, a);

        assertArraysum(a, b, c);
        assertArraysum(c, b, a);

        assertArraysum(a, b, c, d);
        assertArraysum(d, c, b, a);

        assertArraysum(a, -b, c, -d);
        assertArraysum(d, -c, b, -a);

        assertArraysum(a, b, c, d, e, f);
        assertArraysum(f, e, d, c, b, a);

        assertArraysum(a, -b, c, -d, e, f);
        assertArraysum(f, -e, d, -c, b, -a);
    }

    @Test
    void testSumOfProducts_dimensionMismatch() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> Sum.ofProducts(new double[1], new double[2]));
    }

    @Test
    void testSumOfProducts_singleElement() {
        final double[] a = {1.23456789};
        final double[] b = {98765432.1};

        Assertions.assertEquals(a[0] * b[0], Sum.ofProducts(a, b).getAsDouble());
    }

    @Test
    void testSumOfProducts_accuracy() {
        // arrange
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

        // act
        final double sum = Sum.ofProducts(a, b).getAsDouble();

        // assert
        // Compare with arbitrary precision computation.
        BigDecimal result = BigDecimal.ZERO;
        for (int i = 0; i < a.length; i++) {
            result = result.add(aFN[i].divide(aFD[i]).multiply(bFN[i].divide(bFD[i])));
        }
        final double expected = result.doubleValue();
        Assertions.assertEquals(expected, sum, 1e-15);

        final double naive = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        Assertions.assertTrue(Math.abs(naive - sum) > 1.5);
    }

    @Test
    void testSumOfProducts_huge() {
        // arrange
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

    private static void assertSum(final double a, final double b, final double c) {
        final double computedShort = Sum.of(a, b, c).getAsDouble();
        final double computedLong = Sum.create().add(a).add(b).add(c).getAsDouble();

        assertComputedSum(computedShort, a, b, c);
        assertComputedSum(computedLong, a, b, c);
    }

    private static void assertSum(final double a, final double b, final double c, final double d) {
        final double computedShort = Sum.of(a, b, c, d).getAsDouble();
        final double computedLong = Sum.create().add(a).add(b).add(c).add(d).getAsDouble();

        assertComputedSum(computedShort, a, b, c, d);
        assertComputedSum(computedLong, a, b, c, d);
    }

    private static void assertArraysum(final double... values) {
        final double computedShort = Sum.of(values).getAsDouble();
        final double computedLong = Sum.create().add(values).getAsDouble();

        assertComputedSum(computedShort, values);
        assertComputedSum(computedLong, values);
    }

    private static void assertComputedSum(final double computed, final double... values) {
        final double exact = exactSum(values);
        Assertions.assertEquals(exact, computed);
    }

    /** Return the double estimation of the exact summation result computed with unlimited precision.
     * @param values values to add
     * @return double value closest to the exact result
     */
    private static double exactSum(final double... values) {
        BigDecimal sum = BigDecimal.ZERO;
        for (double value : values) {
            sum = sum.add(new BigDecimal(value), MathContext.UNLIMITED);
        }

        return sum.doubleValue();
    }
}
