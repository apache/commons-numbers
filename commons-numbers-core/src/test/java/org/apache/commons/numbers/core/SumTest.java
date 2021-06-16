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
    void test3n_simple() {
        // act/assert
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
    void test3n_accuracy() {
        // arrange
        final double a = 9.999999999;
        final double b = Math.scalb(a, -53);
        final double c = Math.scalb(a, -53);

        // act/assert
        assertValue(a, b, c);
        assertValue(c, b, a);

        assertValue(a, -b, c);
        assertValue(-c, b, -a);
    }

    @Test
    void test4n_simple() {
        // act/assert
        Assertions.assertEquals(0, Sum.of(0, 0, 0, 0).getAsDouble());
        Assertions.assertEquals(10, Sum.of(1, 2, 3, 4).getAsDouble());
        Assertions.assertEquals(-2, Sum.of(1, -2, 3, -4).getAsDouble());

        Assertions.assertEquals(Double.NaN, Sum.of(Double.NaN, 0, 0, 0).getAsDouble());
        Assertions.assertEquals(Double.NaN, Sum.of(0, Double.NaN, 0, 0).getAsDouble());
        Assertions.assertEquals(Double.NaN, Sum.of(0, 0, Double.NaN, 0).getAsDouble());
        Assertions.assertEquals(Double.NaN, Sum.of(0, 0, 0, Double.NaN).getAsDouble());

        Assertions.assertEquals(Double.NaN, Sum.of(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0, 0)
                .getAsDouble());

        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Sum.of(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE).getAsDouble());

        Assertions.assertEquals(Double.POSITIVE_INFINITY, Sum.of(Double.POSITIVE_INFINITY, 1, 1, 1).getAsDouble());
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, Sum.of(1, Double.NEGATIVE_INFINITY, 1, 1).getAsDouble());
    }

    @Test
    void test4n_accuracy() {
        // arrange
        final double a = 9.999999999;
        final double b = Math.scalb(a, -53);
        final double c = Math.scalb(a, -53);
        final double d = Math.scalb(a, -27);

        // act/assert
        assertValue(a, b, c, d);
        assertValue(d, c, b, a);

        assertValue(a, -b, c, -d);
        assertValue(d, -c, b, -a);
    }

    @Test
    void testSimpleSum_array() {
        // act/assert
        Assertions.assertEquals(0, Sum.of(new double[0]).getAsDouble());
        Assertions.assertEquals(-1, Sum.of(new double[] {-1}).getAsDouble());
        Assertions.assertEquals(0, Sum.of(new double[] {0, 0, 0}).getAsDouble());
        Assertions.assertEquals(6, Sum.of(new double[] {1, 2, 3}).getAsDouble());
        Assertions.assertEquals(2, Sum.of(new double[] {1, -2, 3}).getAsDouble());

        Assertions.assertEquals(Double.MAX_VALUE, Sum.of(new double[] {Double.MAX_VALUE}).getAsDouble());
        Assertions.assertEquals(Double.MIN_VALUE, Sum.of(new double[] {Double.MIN_VALUE}).getAsDouble());

        Assertions.assertEquals(Double.NaN, Sum.of(new double[] {0d, Double.NaN}).getAsDouble());
        Assertions.assertEquals(Double.NaN, Sum.of(new double[] {Double.POSITIVE_INFINITY, Double.NaN}).getAsDouble());
        Assertions.assertEquals(Double.NaN,
                Sum.of(new double[] {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}).getAsDouble());

        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Sum.of(new double[] {Double.MAX_VALUE, Double.MAX_VALUE}).getAsDouble());
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Sum.of(new double[] {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY}).getAsDouble());
        Assertions.assertEquals(Double.NEGATIVE_INFINITY,
                Sum.of(new double[] {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY}).getAsDouble());
    }

    @Test
    void testSimpleSum_accuracy() {
        // arrange
        final double a = 9.999999999;
        final double b = Math.scalb(a, -53);
        final double c = Math.scalb(a, -53);
        final double d = Math.scalb(a, -27);
        final double e = Math.scalb(a, -27);
        final double f = Math.scalb(a, -50);

        // act/assert
        assertArrayValue(a);

        assertArrayValue(a, b);
        assertArrayValue(b, a);

        assertArrayValue(a, b, c);
        assertArrayValue(c, b, a);

        assertArrayValue(a, b, c, d);
        assertArrayValue(d, c, b, a);

        assertArrayValue(a, -b, c, -d);
        assertArrayValue(d, -c, b, -a);

        assertArrayValue(a, b, c, d, e, f);
        assertArrayValue(f, e, d, c, b, a);

        assertArrayValue(a, -b, c, -d, e, f);
        assertArrayValue(f, -e, d, -c, b, -a);
    }

    private static void assertValue(final double a, final double b, final double c) {
        final double computed = Sum.of(a, b, c).getAsDouble();
        assertComputedValue(computed, a, b, c);
    }

    private static void assertValue(final double a, final double b, final double c, final double d) {
        final double computed = Sum.of(a, b, c, d).getAsDouble();
        assertComputedValue(computed, a, b, c, d);
    }

    private static void assertArrayValue(final double... values) {
        final double computed = Sum.of(values).getAsDouble();
        assertComputedValue(computed, values);
    }

    private static void assertComputedValue(final double computed, final double... values) {
        final double exact = computeExact(values);
        Assertions.assertEquals(exact, computed);
    }

    /** Return the double estimation of the exact summation result computed with unlimited precision.
     * @param values values to add
     * @return double value closest to the exact result
     */
    private static double computeExact(final double... values) {
        BigDecimal sum = BigDecimal.ZERO;
        for (double value : values) {
            sum = sum.add(new BigDecimal(value), MathContext.UNLIMITED);
        }

        return sum.doubleValue();
    }
}
