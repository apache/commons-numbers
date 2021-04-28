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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Precision#DoubleEquivalence} instances created with
 * {@link Precision#doubleEquivalenceOfEpsilon(double)}.
 */
class EpsilonDoubleEquivalenceTest {
    @Test
    void testInvalidEpsilonValues() {
        // act/assert
        Assertions.assertThrows(IllegalArgumentException.class, () -> Precision.doubleEquivalenceOfEpsilon(-1d));

        String msg;

        msg = Assertions.assertThrows(IllegalArgumentException.class,
            () -> Precision.doubleEquivalenceOfEpsilon(Double.NaN)).getMessage();
        Assertions.assertEquals("Invalid epsilon value: NaN", msg);

        msg = Assertions.assertThrows(IllegalArgumentException.class,
            () -> Precision.doubleEquivalenceOfEpsilon(Double.POSITIVE_INFINITY)).getMessage();
        Assertions.assertEquals("Invalid epsilon value: Infinity", msg);

        msg = Assertions.assertThrows(IllegalArgumentException.class,
            () -> Precision.doubleEquivalenceOfEpsilon(Double.NEGATIVE_INFINITY)).getMessage();
        Assertions.assertEquals("Invalid epsilon value: -Infinity", msg);
    }

    @Test
    void testSignum() {
        // arrange
        final double eps = 1e-2;

        final Precision.DoubleEquivalence cmp = Precision.doubleEquivalenceOfEpsilon(eps);

        // act/assert
        Assertions.assertEquals(Double.POSITIVE_INFINITY, 1 / cmp.signum(0.0), 0d);
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, 1 / cmp.signum(-0.0), 0d);

        Assertions.assertEquals(Double.POSITIVE_INFINITY, 1 / cmp.signum(eps), 0d);
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, 1 / cmp.signum(-eps), 0d);

        Assertions.assertEquals(1, cmp.signum(Math.nextUp(eps)), 0d);
        Assertions.assertEquals(-1, cmp.signum(Math.nextDown(-eps)), 0d);

        Assertions.assertTrue(Double.isNaN(cmp.signum(Double.NaN)));
        Assertions.assertEquals(1, cmp.signum(Double.POSITIVE_INFINITY), 0d);
        Assertions.assertEquals(-1, cmp.signum(Double.NEGATIVE_INFINITY), 0d);
    }

    @Test
    void testCompare_simple() {
        // arrange
        final Precision.DoubleEquivalence cmp = Precision.doubleEquivalenceOfEpsilon(1e-10);

        // act/assert
        Assertions.assertEquals(0, cmp.compare(1, 1));
        Assertions.assertEquals(-1, cmp.compare(1, 2));
        Assertions.assertEquals(1, cmp.compare(2, 1));

        Assertions.assertEquals(0, cmp.compare(-1, -1));
        Assertions.assertEquals(1, cmp.compare(-1, -2));
        Assertions.assertEquals(-1, cmp.compare(-2, -1));
    }

    @Test
    void testCompare_compareToZero() {
        // arrange
        final double eps = 1e-2;

        final Precision.DoubleEquivalence cmp = Precision.doubleEquivalenceOfEpsilon(eps);

        // act/assert
        Assertions.assertEquals(0, cmp.compare(0.0, 0.0));
        Assertions.assertEquals(0, cmp.compare(+0.0, -0.0));
        Assertions.assertEquals(0, cmp.compare(eps, -0.0));
        Assertions.assertEquals(0, cmp.compare(+0.0, eps));

        Assertions.assertEquals(0, cmp.compare(-eps, -0.0));
        Assertions.assertEquals(0, cmp.compare(+0.0, -eps));

        Assertions.assertEquals(-1, cmp.compare(0.0, 1.0));
        Assertions.assertEquals(1, cmp.compare(1.0, 0.0));

        Assertions.assertEquals(1, cmp.compare(0.0, -1.0));
        Assertions.assertEquals(-1, cmp.compare(-1.0, 0.0));
    }

    @Test
    void testCompare_compareNonZero() {
        // arrange
        final double eps = 1e-5;
        final double small = 1e-3;
        final double big = 1e100;

        final Precision.DoubleEquivalence cmp = Precision.doubleEquivalenceOfEpsilon(eps);

        // act/assert
        Assertions.assertEquals(0, cmp.compare(eps, 2 * eps));
        Assertions.assertEquals(0, cmp.compare(-2 * eps, -eps));

        Assertions.assertEquals(0, cmp.compare(small, small + (0.9 * eps)));
        Assertions.assertEquals(0, cmp.compare(-small - (0.9 * eps), -small));

        Assertions.assertEquals(0, cmp.compare(big, nextUp(big, 1)));
        Assertions.assertEquals(0, cmp.compare(nextDown(-big, 1), -big));

        Assertions.assertEquals(-1, cmp.compare(small, small + (1.1 * eps)));
        Assertions.assertEquals(1, cmp.compare(-small, -small - (1.1 * eps)));

        Assertions.assertEquals(-1, cmp.compare(big, nextUp(big, 2)));
        Assertions.assertEquals(1, cmp.compare(-big, nextDown(-big, 2)));
    }

    @Test
    void testCompare_NaN() {
        // arrange
        final Precision.DoubleEquivalence cmp = Precision.doubleEquivalenceOfEpsilon(1e-6);

        // act/assert
        Assertions.assertEquals(-1, cmp.compare(0, Double.NaN));
        Assertions.assertEquals(1, cmp.compare(Double.NaN, 0));
        Assertions.assertEquals(0, cmp.compare(Double.NaN, Double.NaN));

        Assertions.assertEquals(-1, cmp.compare(Double.POSITIVE_INFINITY, Double.NaN));
        Assertions.assertEquals(1, cmp.compare(Double.NaN, Double.POSITIVE_INFINITY));

        Assertions.assertEquals(-1, cmp.compare(Double.NEGATIVE_INFINITY, Double.NaN));
        Assertions.assertEquals(1, cmp.compare(Double.NaN, Double.NEGATIVE_INFINITY));
    }

    @Test
    void testCompare_infinity() {
        // arrange
        final Precision.DoubleEquivalence cmp = Precision.doubleEquivalenceOfEpsilon(1e-6);

        // act/assert
        Assertions.assertEquals(-1, cmp.compare(0, Double.POSITIVE_INFINITY));
        Assertions.assertEquals(1, cmp.compare(Double.POSITIVE_INFINITY, 0));
        Assertions.assertEquals(0, cmp.compare(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));

        Assertions.assertEquals(1, cmp.compare(0, Double.NEGATIVE_INFINITY));
        Assertions.assertEquals(-1, cmp.compare(Double.NEGATIVE_INFINITY, 0));
        Assertions.assertEquals(0, cmp.compare(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
    }

    @Test
    void testEq() {
        // arrange
        final double eps = Math.ulp(1.0);
        final double twoEps = 2 * eps;
        final Precision.DoubleEquivalence cmp = Precision.doubleEquivalenceOfEpsilon(eps);

        // act/assert
        Assertions.assertTrue(cmp.eq(0.0, 0.0));

        Assertions.assertTrue(cmp.eq(1.0, 1.0));
        Assertions.assertTrue(cmp.eq(1.0, 1.0 + eps));
        Assertions.assertTrue(cmp.eq(1.0, 1.0 - eps));
        Assertions.assertFalse(cmp.eq(1.0, 1.0 + twoEps));
        Assertions.assertFalse(cmp.eq(1.0, 1.0 - twoEps));

        Assertions.assertTrue(cmp.eq(-1.0, -1.0));
        Assertions.assertTrue(cmp.eq(-1.0, -1.0 + eps));
        Assertions.assertTrue(cmp.eq(-1.0, -1.0 - eps));
        Assertions.assertFalse(cmp.eq(-1.0, -1.0 + twoEps));
        Assertions.assertFalse(cmp.eq(-1.0, -1.0 - twoEps));
    }

    @Test
    void testEqZero() {
        // arrange
        final double eps = 1e-6;
        final Precision.DoubleEquivalence cmp = Precision.doubleEquivalenceOfEpsilon(eps);

        // act/assert
        Assertions.assertTrue(cmp.eqZero(0.0));

        Assertions.assertFalse(cmp.eqZero(Math.nextUp(eps)));
        Assertions.assertFalse(cmp.eqZero(Math.nextDown(-eps)));
    }

    @Test
    void testLt() {
        // arrange
        final Precision.DoubleEquivalence cmp = Precision.doubleEquivalenceOfEpsilon(1e-6);

        // act/assert
        Assertions.assertTrue(cmp.lt(1, 2));
        Assertions.assertTrue(cmp.lt(-2, -1));

        Assertions.assertFalse(cmp.lt(1, 1));
        Assertions.assertFalse(cmp.lt(-1, -1));
        Assertions.assertFalse(cmp.lt(2, 1));
        Assertions.assertFalse(cmp.lt(-1, -2));
    }

    @Test
    void testLte() {
        // arrange
        final Precision.DoubleEquivalence cmp = Precision.doubleEquivalenceOfEpsilon(1e-6);

        // act/assert
        Assertions.assertTrue(cmp.lte(1, 2));
        Assertions.assertTrue(cmp.lte(-2, -1));
        Assertions.assertTrue(cmp.lte(1, 1));
        Assertions.assertTrue(cmp.lte(-1, -1));

        Assertions.assertFalse(cmp.lte(2, 1));
        Assertions.assertFalse(cmp.lte(-1, -2));
    }

    @Test
    void testGt() {
        // arrange
        final Precision.DoubleEquivalence cmp = Precision.doubleEquivalenceOfEpsilon(1e-6);

        // act/assert
        Assertions.assertTrue(cmp.gt(2, 1));
        Assertions.assertTrue(cmp.gt(-1, -2));

        Assertions.assertFalse(cmp.gt(1, 1));
        Assertions.assertFalse(cmp.gt(-1, -1));
        Assertions.assertFalse(cmp.gt(1, 2));
        Assertions.assertFalse(cmp.gt(-2, -1));
    }

    @Test
    void testGte() {
        // arrange
        final Precision.DoubleEquivalence cmp = Precision.doubleEquivalenceOfEpsilon(1e-6);

        // act/assert
        Assertions.assertTrue(cmp.gte(2, 1));
        Assertions.assertTrue(cmp.gte(-1, -2));
        Assertions.assertTrue(cmp.gte(1, 1));
        Assertions.assertTrue(cmp.gte(-1, -1));

        Assertions.assertFalse(cmp.gte(1, 2));
        Assertions.assertFalse(cmp.gte(-2, -1));
    }

    /**
     * Increments the given double value {@code count} number of times
     * using {@link Math#nextUp(double)}.
     * @param n
     * @param count
     * @return
     */
    private static double nextUp(final double n, final int count) {
        double result = n;
        for (int i = 0; i < count; ++i) {
            result = Math.nextUp(result);
        }

        return result;
    }

    /**
     * Decrements the given double value {@code count} number of times
     * using {@link Math#nextDown(double)}.
     * @param n
     * @param count
     * @return
     */
    private static double nextDown(final double n, final int count) {
        double result = n;
        for (int i = 0; i < count; ++i) {
            result = Math.nextDown(result);
        }

        return result;
    }
}
