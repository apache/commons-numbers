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
import java.math.MathContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SummationTest {

    @Test
    void test3n_simple() {
        // act/assert
        Assertions.assertEquals(0, Summation.value(0, 0, 0));
        Assertions.assertEquals(6, Summation.value(1, 2, 3));
        Assertions.assertEquals(2, Summation.value(1, -2, 3));

        Assertions.assertEquals(Double.NaN, Summation.value(Double.NaN, 0, 0));
        Assertions.assertEquals(Double.NaN, Summation.value(0, Double.NaN, 0));
        Assertions.assertEquals(Double.NaN, Summation.value(0, 0, Double.NaN));

        Assertions.assertEquals(Double.NaN, Summation.value(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0));

        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Summation.value(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE));

        Assertions.assertEquals(Double.POSITIVE_INFINITY, Summation.value(Double.POSITIVE_INFINITY, 1, 1));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, Summation.value(1, Double.NEGATIVE_INFINITY, 1));
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
        Assertions.assertEquals(0, Summation.value(0, 0, 0, 0));
        Assertions.assertEquals(10, Summation.value(1, 2, 3, 4));
        Assertions.assertEquals(-2, Summation.value(1, -2, 3, -4));

        Assertions.assertEquals(Double.NaN, Summation.value(Double.NaN, 0, 0, 0));
        Assertions.assertEquals(Double.NaN, Summation.value(0, Double.NaN, 0, 0));
        Assertions.assertEquals(Double.NaN, Summation.value(0, 0, Double.NaN, 0));
        Assertions.assertEquals(Double.NaN, Summation.value(0, 0, 0, Double.NaN));

        Assertions.assertEquals(Double.NaN, Summation.value(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0, 0));

        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Summation.value(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE));

        Assertions.assertEquals(Double.POSITIVE_INFINITY, Summation.value(Double.POSITIVE_INFINITY, 1, 1, 1));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, Summation.value(1, Double.NEGATIVE_INFINITY, 1, 1));
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
    void test5n_simple() {
        // act/assert
        Assertions.assertEquals(0, Summation.value(0, 0, 0, 0, 0));
        Assertions.assertEquals(15, Summation.value(1, 2, 3, 4, 5));
        Assertions.assertEquals(3, Summation.value(1, -2, 3, -4, 5));

        Assertions.assertEquals(Double.NaN, Summation.value(Double.NaN, 0, 0, 0, 0));
        Assertions.assertEquals(Double.NaN, Summation.value(0, Double.NaN, 0, 0, 0));
        Assertions.assertEquals(Double.NaN, Summation.value(0, 0, Double.NaN, 0, 0));
        Assertions.assertEquals(Double.NaN, Summation.value(0, 0, 0, Double.NaN, 0));
        Assertions.assertEquals(Double.NaN, Summation.value(0, 0, 0, 0, Double.NaN));

        Assertions.assertEquals(Double.NaN, Summation.value(
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0, 0, 0));

        Assertions.assertEquals(Double.POSITIVE_INFINITY, Summation.value(
                Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE));

        Assertions.assertEquals(Double.POSITIVE_INFINITY, Summation.value(Double.POSITIVE_INFINITY, 1, 1, 1, 1));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, Summation.value(1, Double.NEGATIVE_INFINITY, 1, 1, 1));
    }

    @Test
    void test5n_accuracy() {
        // arrange
        final double a = 9.999999999;
        final double b = Math.scalb(a, -53);
        final double c = Math.scalb(a, -53);
        final double d = Math.scalb(a, -27);
        final double e = Math.scalb(a, -27);

        // act/assert
        assertValue(a, b, c, d, e);
        assertValue(e, d, c, b, a);

        assertValue(a, -b, c, -d, e);
        assertValue(-e, d, -c, b, -a);
    }

    @Test
    void test6n_simple() {
        // act/assert
        Assertions.assertEquals(0, Summation.value(0, 0, 0, 0, 0, 0));
        Assertions.assertEquals(21, Summation.value(1, 2, 3, 4, 5, 6));
        Assertions.assertEquals(-3, Summation.value(1, -2, 3, -4, 5, -6));

        Assertions.assertEquals(Double.NaN, Summation.value(Double.NaN, 0, 0, 0, 0, 0));
        Assertions.assertEquals(Double.NaN, Summation.value(0, Double.NaN, 0, 0, 0, 0));
        Assertions.assertEquals(Double.NaN, Summation.value(0, 0, Double.NaN, 0, 0, 0));
        Assertions.assertEquals(Double.NaN, Summation.value(0, 0, 0, Double.NaN, 0, 0));
        Assertions.assertEquals(Double.NaN, Summation.value(0, 0, 0, 0, Double.NaN, 0));
        Assertions.assertEquals(Double.NaN, Summation.value(0, 0, 0, 0, 0, Double.NaN));

        Assertions.assertEquals(Double.NaN, Summation.value(
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0, 0, 0, 0));

        Assertions.assertEquals(Double.POSITIVE_INFINITY, Summation.value(
                Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE,
                Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE));

        Assertions.assertEquals(Double.POSITIVE_INFINITY, Summation.value(Double.POSITIVE_INFINITY, 1, 1, 1, 1, 1));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, Summation.value(1, Double.NEGATIVE_INFINITY, 1, 1, 1, 1));
    }

    @Test
    void test6n_accuracy() {
        // arrange
        final double a = 9.999999999;
        final double b = Math.scalb(a, -53);
        final double c = Math.scalb(a, -53);
        final double d = Math.scalb(a, -27);
        final double e = Math.scalb(a, -27);
        final double f = Math.scalb(a, -50);

        // act/assert
        assertValue(a, b, c, d, e, f);
        assertValue(f, e, d, c, b, a);

        assertValue(a, -b, c, -d, e, f);
        assertValue(f, -e, d, -c, b, -a);
    }

    @Test
    void testArray_simple() {
        // act/assert
        Assertions.assertEquals(0, Summation.value(new double[0]));
        Assertions.assertEquals(-1, Summation.value(new double[] {-1}));
        Assertions.assertEquals(0, Summation.value(new double[] {0, 0, 0}));
        Assertions.assertEquals(6, Summation.value(new double[] {1, 2, 3}));
        Assertions.assertEquals(2, Summation.value(new double[] {1, -2, 3}));

        Assertions.assertEquals(Double.MAX_VALUE, Summation.value(new double[] {Double.MAX_VALUE}));
        Assertions.assertEquals(Double.MIN_VALUE, Summation.value(new double[] {Double.MIN_VALUE}));

        Assertions.assertEquals(Double.NaN, Summation.value(new double[] {0d, Double.NaN}));
        Assertions.assertEquals(Double.NaN, Summation.value(new double[] {Double.POSITIVE_INFINITY, Double.NaN}));
        Assertions.assertEquals(Double.NaN,
                Summation.value(new double[] {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}));

        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Summation.value(new double[] {Double.MAX_VALUE, Double.MAX_VALUE}));
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                Summation.value(new double[] {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY}));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY,
                Summation.value(new double[] {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY}));
    }

    @Test
    void testArray_accuracy() {
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
        final double computed = Summation.value(a, b, c);
        assertComputedValue(computed, a, b, c);
    }

    private static void assertValue(final double a, final double b, final double c, final double d) {
        final double computed = Summation.value(a, b, c, d);
        assertComputedValue(computed, a, b, c, d);
    }

    private static void assertValue(final double a, final double b, final double c, final double d,
            final double e) {
        final double computed = Summation.value(a, b, c, d, e);
        assertComputedValue(computed, a, b, c, d, e);
    }

    private static void assertValue(final double a, final double b, final double c, final double d,
            final double e, final double f) {
        final double computed = Summation.value(a, b, c, d, e, f);
        assertComputedValue(computed, a, b, c, d, e, f);
    }

    private static void assertArrayValue(final double... values) {
        final double computed = Summation.value(values);
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
            sum = sum.add(BigDecimal.valueOf(value), MathContext.UNLIMITED);
        }

        return sum.doubleValue();
    }
}
