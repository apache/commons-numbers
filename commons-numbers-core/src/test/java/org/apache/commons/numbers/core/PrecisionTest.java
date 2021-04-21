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

import java.math.RoundingMode;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for the {@link Precision} class.
 *
 */
class PrecisionTest {

    // Interfaces to allow testing equals variants with the same conditions

    @FunctionalInterface
    private interface EqualsWithDelta {
        boolean equals(double a, double b, double delta);
    }

    @FunctionalInterface
    private interface EqualsWithUlps {
        boolean equals(double a, double b, int ulps);
    }

    @FunctionalInterface
    private interface FloatEqualsWithDelta {
        boolean equals(float a, float b, float delta);
    }

    @FunctionalInterface
    private interface FloatEqualsWithUlps {
        boolean equals(float a, float b, int ulps);
    }

    @Test
    void testEqualsWithRelativeTolerance() {
        Assertions.assertTrue(Precision.equalsWithRelativeTolerance(0d, 0d, 0d));
        Assertions.assertTrue(Precision.equalsWithRelativeTolerance(0d, 1 / Double.NEGATIVE_INFINITY, 0d));

        final double eps = 1e-14;
        Assertions.assertFalse(Precision.equalsWithRelativeTolerance(1.987654687654968, 1.987654687654988, eps));
        Assertions.assertTrue(Precision.equalsWithRelativeTolerance(1.987654687654968, 1.987654687654987, eps));
        Assertions.assertFalse(Precision.equalsWithRelativeTolerance(1.987654687654968, 1.987654687654948, eps));
        Assertions.assertTrue(Precision.equalsWithRelativeTolerance(1.987654687654968, 1.987654687654949, eps));

        Assertions.assertFalse(Precision.equalsWithRelativeTolerance(Precision.SAFE_MIN, 0.0, eps));

        Assertions.assertFalse(Precision.equalsWithRelativeTolerance(1.0000000000001e-300, 1e-300, eps));
        Assertions.assertTrue(Precision.equalsWithRelativeTolerance(1.00000000000001e-300, 1e-300, eps));

        Assertions.assertFalse(Precision.equalsWithRelativeTolerance(Double.NEGATIVE_INFINITY, 1.23, eps));
        Assertions.assertFalse(Precision.equalsWithRelativeTolerance(Double.POSITIVE_INFINITY, 1.23, eps));

        Assertions.assertTrue(Precision.equalsWithRelativeTolerance(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, eps));
        Assertions.assertTrue(Precision.equalsWithRelativeTolerance(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, eps));
        Assertions.assertFalse(Precision.equalsWithRelativeTolerance(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, eps));

        Assertions.assertFalse(Precision.equalsWithRelativeTolerance(Double.NaN, 1.23, eps));
        Assertions.assertFalse(Precision.equalsWithRelativeTolerance(Double.NaN, Double.NaN, eps));
    }

    @Test
    void testEqualsIncludingNaN() {
        double[] testArray = {
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            1d,
            0d };
        for (int i = 0; i < testArray.length; i++) {
            for (int j = 0; j < testArray.length; j++) {
                if (i == j) {
                    Assertions.assertTrue(Precision.equalsIncludingNaN(testArray[i], testArray[j]));
                    Assertions.assertTrue(Precision.equalsIncludingNaN(testArray[j], testArray[i]));
                } else {
                    Assertions.assertFalse(Precision.equalsIncludingNaN(testArray[i], testArray[j]));
                    Assertions.assertFalse(Precision.equalsIncludingNaN(testArray[j], testArray[i]));
                }
            }
        }
    }

    @Test
    void testEqualsWithAllowedDelta() {
        assertEqualsWithAllowedDelta(Precision::equalsIncludingNaN, true);
    }

    @Test
    void testEqualsIncludingNaNWithAllowedDelta() {
        assertEqualsWithAllowedDelta(Precision::equalsIncludingNaN, true);
    }

    private static void assertEqualsWithAllowedDelta(EqualsWithDelta fun, boolean nanAreEqual) {
        Assertions.assertTrue(fun.equals(153.0000, 153.0000, .0625));
        Assertions.assertTrue(fun.equals(153.0000, 153.0625, .0625));
        Assertions.assertTrue(fun.equals(152.9375, 153.0000, .0625));
        Assertions.assertFalse(fun.equals(153.0000, 153.0625, .0624));
        Assertions.assertFalse(fun.equals(152.9374, 153.0000, .0625));
        Assertions.assertEquals(nanAreEqual, fun.equals(Double.NaN, Double.NaN, 1.0));
        Assertions.assertTrue(fun.equals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
        Assertions.assertTrue(fun.equals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, 1.0));
        Assertions.assertFalse(fun.equals(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
    }

    @Test
    void testEqualsWithAllowedUlps() {
        assertEqualsIncludingNaNWithAllowedUlps(Precision::equals, false, false);
    }

    @Test
    void testEqualsWithImplicitAllowedUlpsOf1() {
        // Use the version without the ulp argument
        assertEqualsIncludingNaNWithAllowedUlps((a, b, ulp) -> Precision.equals(a, b), false, true);
    }

    @Test
    void testEqualsIncludingNaNWithAllowedUlps() {
        assertEqualsIncludingNaNWithAllowedUlps(Precision::equalsIncludingNaN, true, false);
    }

    private static void assertEqualsIncludingNaNWithAllowedUlps(EqualsWithUlps fun,
            boolean nanAreEqual, boolean fixed1Ulp) {
        Assertions.assertTrue(fun.equals(0.0, -0.0, 1));

        Assertions.assertTrue(fun.equals(1.0, 1 + Math.ulp(1d), 1));
        Assertions.assertFalse(fun.equals(1.0, 1 + 2 * Math.ulp(1d), 1));

        for (double value : new double[] {153.0, -128.0, 0.0, 1.0}) {
            Assertions.assertTrue(fun.equals(value, value, 1));
            Assertions.assertTrue(fun.equals(value, Math.nextUp(value), 1));
            Assertions.assertFalse(fun.equals(value, Math.nextUp(Math.nextUp(value)), 1));
            Assertions.assertTrue(fun.equals(value, Math.nextDown(value), 1));
            Assertions.assertFalse(fun.equals(value, Math.nextDown(Math.nextDown(value)), 1));
            // This test is conditional
            if (!fixed1Ulp) {
                Assertions.assertFalse(fun.equals(value, Math.nextUp(value), 0));
                Assertions.assertTrue(fun.equals(value, Math.nextUp(Math.nextUp(value)), 2));
                Assertions.assertTrue(fun.equals(value, Math.nextDown(Math.nextDown(value)), 2));
            }
        }

        Assertions.assertTrue(fun.equals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 1));
        Assertions.assertTrue(fun.equals(Double.MAX_VALUE, Double.POSITIVE_INFINITY, 1));

        Assertions.assertTrue(fun.equals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, 1));
        Assertions.assertTrue(fun.equals(-Double.MAX_VALUE, Double.NEGATIVE_INFINITY, 1));

        Assertions.assertEquals(nanAreEqual, fun.equals(Double.NaN, Double.NaN, 1));
        Assertions.assertEquals(nanAreEqual, fun.equals(Double.NaN, Double.NaN, 0));
        Assertions.assertFalse(fun.equals(Double.NaN, 0, 0));
        Assertions.assertFalse(fun.equals(0, Double.NaN, 0));
        Assertions.assertFalse(fun.equals(Double.NaN, Double.POSITIVE_INFINITY, 0));
        Assertions.assertFalse(fun.equals(Double.NaN, Double.NEGATIVE_INFINITY, 0));

        Assertions.assertFalse(fun.equals(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 100000));
    }

    // Tests for floating point equality match the above tests with arguments
    // converted to float

    @Test
    void testFloatEqualsIncludingNaN() {
        float[] testArray = {
            Float.NaN,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            1f,
            0f };
        for (int i = 0; i < testArray.length; i++) {
            for (int j = 0; j < testArray.length; j++) {
                if (i == j) {
                    Assertions.assertTrue(Precision.equalsIncludingNaN(testArray[i], testArray[j]));
                    Assertions.assertTrue(Precision.equalsIncludingNaN(testArray[j], testArray[i]));
                } else {
                    Assertions.assertFalse(Precision.equalsIncludingNaN(testArray[i], testArray[j]));
                    Assertions.assertFalse(Precision.equalsIncludingNaN(testArray[j], testArray[i]));
                }
            }
        }
    }

    @Test
    void testFloatEqualsWithAllowedDelta() {
        assertFloatEqualsWithAllowedDelta(Precision::equalsIncludingNaN, true);
    }

    @Test
    void testFloatEqualsIncludingNaNWithAllowedDelta() {
        assertFloatEqualsWithAllowedDelta(Precision::equalsIncludingNaN, true);
    }

    private static void assertFloatEqualsWithAllowedDelta(FloatEqualsWithDelta fun, boolean nanAreEqual) {
        Assertions.assertTrue(fun.equals(153.0000f, 153.0000f, .0625f));
        Assertions.assertTrue(fun.equals(153.0000f, 153.0625f, .0625f));
        Assertions.assertTrue(fun.equals(152.9375f, 153.0000f, .0625f));
        Assertions.assertFalse(fun.equals(153.0000f, 153.0625f, .0624f));
        Assertions.assertFalse(fun.equals(152.9374f, 153.0000f, .0625f));
        Assertions.assertEquals(nanAreEqual, fun.equals(Float.NaN, Float.NaN, 1.0f));
        Assertions.assertTrue(fun.equals(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, 1.0f));
        Assertions.assertTrue(fun.equals(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, 1.0f));
        Assertions.assertFalse(fun.equals(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 1.0f));
    }

    @Test
    void testFloatEqualsWithAllowedUlps() {
        assertFloatEqualsIncludingNaNWithAllowedUlps(Precision::equals, false, false);
    }

    @Test
    void testFloatEqualsWithImplicitAllowedUlpsOf1() {
        // Use the version without the ulp argument
        assertFloatEqualsIncludingNaNWithAllowedUlps((a, b, ulp) -> Precision.equals(a, b), false, true);
    }

    @Test
    void testFloatEqualsIncludingNaNWithAllowedUlps() {
        assertFloatEqualsIncludingNaNWithAllowedUlps(Precision::equalsIncludingNaN, true, false);
    }

    private static void assertFloatEqualsIncludingNaNWithAllowedUlps(FloatEqualsWithUlps fun,
            boolean nanAreEqual, boolean fixed1Ulp) {
        Assertions.assertTrue(fun.equals(0.0f, -0.0f, 1));

        Assertions.assertTrue(fun.equals(1.0f, 1f + Math.ulp(1f), 1));
        Assertions.assertFalse(fun.equals(1.0f, 1f + 2 * Math.ulp(1f), 1));

        for (float value : new float[] {153.0f, -128.0f, 0.0f, 1.0f}) {
            Assertions.assertTrue(fun.equals(value, value, 1));
            Assertions.assertTrue(fun.equals(value, Math.nextUp(value), 1));
            Assertions.assertFalse(fun.equals(value, Math.nextUp(Math.nextUp(value)), 1));
            Assertions.assertTrue(fun.equals(value, Math.nextDown(value), 1));
            Assertions.assertFalse(fun.equals(value, Math.nextDown(Math.nextDown(value)), 1));
            // This test is conditional
            if (!fixed1Ulp) {
                Assertions.assertFalse(fun.equals(value, Math.nextUp(value), 0));
                Assertions.assertTrue(fun.equals(value, Math.nextUp(Math.nextUp(value)), 2));
                Assertions.assertTrue(fun.equals(value, Math.nextDown(Math.nextDown(value)), 2));
            }
        }

        Assertions.assertTrue(fun.equals(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, 1));
        Assertions.assertTrue(fun.equals(Float.MAX_VALUE, Float.POSITIVE_INFINITY, 1));

        Assertions.assertTrue(fun.equals(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, 1));
        Assertions.assertTrue(fun.equals(-Float.MAX_VALUE, Float.NEGATIVE_INFINITY, 1));

        Assertions.assertEquals(nanAreEqual, fun.equals(Float.NaN, Float.NaN, 1));
        Assertions.assertEquals(nanAreEqual, fun.equals(Float.NaN, Float.NaN, 0));
        Assertions.assertFalse(fun.equals(Float.NaN, 0, 0));
        Assertions.assertFalse(fun.equals(0, Float.NaN, 0));
        Assertions.assertFalse(fun.equals(Float.NaN, Float.POSITIVE_INFINITY, 0));
        Assertions.assertFalse(fun.equals(Float.NaN, Float.NEGATIVE_INFINITY, 0));

        Assertions.assertFalse(fun.equals(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 100000));
    }

    @Test
    void testCompareToEpsilon() {
        Assertions.assertEquals(0, Precision.compareTo(152.33, 152.32, .011));
        Assertions.assertTrue(Precision.compareTo(152.308, 152.32, .011) < 0);
        Assertions.assertTrue(Precision.compareTo(152.33, 152.318, .011) > 0);
        Assertions.assertEquals(0, Precision.compareTo(Double.MIN_VALUE, +0.0, Double.MIN_VALUE));
        Assertions.assertEquals(0, Precision.compareTo(Double.MIN_VALUE, -0.0, Double.MIN_VALUE));
    }

    @Test
    void testCompareToMaxUlps() {
        double a = 152.32;
        double delta = Math.ulp(a);
        for (int i = 0; i <= 10; ++i) {
            if (i <= 5) {
                Assertions.assertEquals(+0, Precision.compareTo(a, a + i * delta, 5));
                Assertions.assertEquals(+0, Precision.compareTo(a, a - i * delta, 5));
            } else {
                Assertions.assertEquals(-1, Precision.compareTo(a, a + i * delta, 5));
                Assertions.assertEquals(+1, Precision.compareTo(a, a - i * delta, 5));
            }
        }

        Assertions.assertEquals(+0, Precision.compareTo(-0.0, 0.0, 0));

        Assertions.assertEquals(-1, Precision.compareTo(-Double.MIN_VALUE, -0.0, 0));
        Assertions.assertEquals(+0, Precision.compareTo(-Double.MIN_VALUE, -0.0, 1));
        Assertions.assertEquals(-1, Precision.compareTo(-Double.MIN_VALUE, +0.0, 0));
        Assertions.assertEquals(+0, Precision.compareTo(-Double.MIN_VALUE, +0.0, 1));

        Assertions.assertEquals(+1, Precision.compareTo(+Double.MIN_VALUE, -0.0, 0));
        Assertions.assertEquals(+0, Precision.compareTo(+Double.MIN_VALUE, -0.0, 1));
        Assertions.assertEquals(+1, Precision.compareTo(+Double.MIN_VALUE, +0.0, 0));
        Assertions.assertEquals(+0, Precision.compareTo(+Double.MIN_VALUE, +0.0, 1));

        Assertions.assertEquals(-1, Precision.compareTo(-Double.MIN_VALUE, Double.MIN_VALUE, 0));
        Assertions.assertEquals(-1, Precision.compareTo(-Double.MIN_VALUE, Double.MIN_VALUE, 1));
        Assertions.assertEquals(+0, Precision.compareTo(-Double.MIN_VALUE, Double.MIN_VALUE, 2));

        Assertions.assertEquals(+0, Precision.compareTo(Double.MAX_VALUE, Double.POSITIVE_INFINITY, 1));
        Assertions.assertEquals(-1, Precision.compareTo(Double.MAX_VALUE, Double.POSITIVE_INFINITY, 0));

        Assertions.assertEquals(+1, Precision.compareTo(Double.MAX_VALUE, Double.NaN, Integer.MAX_VALUE));
        Assertions.assertEquals(+1, Precision.compareTo(Double.NaN, Double.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    void testRoundDouble() {
        double x = 1.234567890;
        Assertions.assertEquals(1.23, Precision.round(x, 2));
        Assertions.assertEquals(1.235, Precision.round(x, 3));
        Assertions.assertEquals(1.2346, Precision.round(x, 4));

        // JIRA MATH-151
        Assertions.assertEquals(39.25, Precision.round(39.245, 2));
        Assertions.assertEquals(39.24, Precision.round(39.245, 2, RoundingMode.DOWN));
        double xx = 39.0;
        xx += 245d / 1000d;
        Assertions.assertEquals(39.25, Precision.round(xx, 2));

        // BZ 35904
        Assertions.assertEquals(30.1d, Precision.round(30.095d, 2));
        Assertions.assertEquals(30.1d, Precision.round(30.095d, 1));
        Assertions.assertEquals(33.1d, Precision.round(33.095d, 1));
        Assertions.assertEquals(33.1d, Precision.round(33.095d, 2));
        Assertions.assertEquals(50.09d, Precision.round(50.085d, 2));
        Assertions.assertEquals(50.19d, Precision.round(50.185d, 2));
        Assertions.assertEquals(50.01d, Precision.round(50.005d, 2));
        Assertions.assertEquals(30.01d, Precision.round(30.005d, 2));
        Assertions.assertEquals(30.65d, Precision.round(30.645d, 2));

        Assertions.assertEquals(1.24, Precision.round(x, 2, RoundingMode.CEILING));
        Assertions.assertEquals(1.235, Precision.round(x, 3, RoundingMode.CEILING));
        Assertions.assertEquals(1.2346, Precision.round(x, 4, RoundingMode.CEILING));
        Assertions.assertEquals(-1.23, Precision.round(-x, 2, RoundingMode.CEILING));
        Assertions.assertEquals(-1.234, Precision.round(-x, 3, RoundingMode.CEILING));
        Assertions.assertEquals(-1.2345, Precision.round(-x, 4, RoundingMode.CEILING));

        Assertions.assertEquals(1.23, Precision.round(x, 2, RoundingMode.DOWN));
        Assertions.assertEquals(1.234, Precision.round(x, 3, RoundingMode.DOWN));
        Assertions.assertEquals(1.2345, Precision.round(x, 4, RoundingMode.DOWN));
        Assertions.assertEquals(-1.23, Precision.round(-x, 2, RoundingMode.DOWN));
        Assertions.assertEquals(-1.234, Precision.round(-x, 3, RoundingMode.DOWN));
        Assertions.assertEquals(-1.2345, Precision.round(-x, 4, RoundingMode.DOWN));

        Assertions.assertEquals(1.23, Precision.round(x, 2, RoundingMode.FLOOR));
        Assertions.assertEquals(1.234, Precision.round(x, 3, RoundingMode.FLOOR));
        Assertions.assertEquals(1.2345, Precision.round(x, 4, RoundingMode.FLOOR));
        Assertions.assertEquals(-1.24, Precision.round(-x, 2, RoundingMode.FLOOR));
        Assertions.assertEquals(-1.235, Precision.round(-x, 3, RoundingMode.FLOOR));
        Assertions.assertEquals(-1.2346, Precision.round(-x, 4, RoundingMode.FLOOR));

        Assertions.assertEquals(1.23, Precision.round(x, 2, RoundingMode.HALF_DOWN));
        Assertions.assertEquals(1.235, Precision.round(x, 3, RoundingMode.HALF_DOWN));
        Assertions.assertEquals(1.2346, Precision.round(x, 4, RoundingMode.HALF_DOWN));
        Assertions.assertEquals(-1.23, Precision.round(-x, 2, RoundingMode.HALF_DOWN));
        Assertions.assertEquals(-1.235, Precision.round(-x, 3, RoundingMode.HALF_DOWN));
        Assertions.assertEquals(-1.2346, Precision.round(-x, 4, RoundingMode.HALF_DOWN));
        Assertions.assertEquals(1.234, Precision.round(1.2345, 3, RoundingMode.HALF_DOWN));
        Assertions.assertEquals(-1.234, Precision.round(-1.2345, 3, RoundingMode.HALF_DOWN));

        Assertions.assertEquals(1.23, Precision.round(x, 2, RoundingMode.HALF_EVEN));
        Assertions.assertEquals(1.235, Precision.round(x, 3, RoundingMode.HALF_EVEN));
        Assertions.assertEquals(1.2346, Precision.round(x, 4, RoundingMode.HALF_EVEN));
        Assertions.assertEquals(-1.23, Precision.round(-x, 2, RoundingMode.HALF_EVEN));
        Assertions.assertEquals(-1.235, Precision.round(-x, 3, RoundingMode.HALF_EVEN));
        Assertions.assertEquals(-1.2346, Precision.round(-x, 4, RoundingMode.HALF_EVEN));
        Assertions.assertEquals(1.234, Precision.round(1.2345, 3, RoundingMode.HALF_EVEN));
        Assertions.assertEquals(-1.234, Precision.round(-1.2345, 3, RoundingMode.HALF_EVEN));
        Assertions.assertEquals(1.236, Precision.round(1.2355, 3, RoundingMode.HALF_EVEN));
        Assertions.assertEquals(-1.236, Precision.round(-1.2355, 3, RoundingMode.HALF_EVEN));

        Assertions.assertEquals(1.23, Precision.round(x, 2, RoundingMode.HALF_UP));
        Assertions.assertEquals(1.235, Precision.round(x, 3, RoundingMode.HALF_UP));
        Assertions.assertEquals(1.2346, Precision.round(x, 4, RoundingMode.HALF_UP));
        Assertions.assertEquals(-1.23, Precision.round(-x, 2, RoundingMode.HALF_UP));
        Assertions.assertEquals(-1.235, Precision.round(-x, 3, RoundingMode.HALF_UP));
        Assertions.assertEquals(-1.2346, Precision.round(-x, 4, RoundingMode.HALF_UP));
        Assertions.assertEquals(1.235, Precision.round(1.2345, 3, RoundingMode.HALF_UP));
        Assertions.assertEquals(-1.235, Precision.round(-1.2345, 3, RoundingMode.HALF_UP));

        Assertions.assertEquals(-1.23, Precision.round(-1.23, 2, RoundingMode.UNNECESSARY));
        Assertions.assertEquals(1.23, Precision.round(1.23, 2, RoundingMode.UNNECESSARY));

        try {
            Precision.round(1.234, 2, RoundingMode.UNNECESSARY);
            Assertions.fail();
        } catch (ArithmeticException ex) {
            // expected
        }

        Assertions.assertEquals(1.24, Precision.round(x, 2, RoundingMode.UP));
        Assertions.assertEquals(1.235, Precision.round(x, 3, RoundingMode.UP));
        Assertions.assertEquals(1.2346, Precision.round(x, 4, RoundingMode.UP));
        Assertions.assertEquals(-1.24, Precision.round(-x, 2, RoundingMode.UP));
        Assertions.assertEquals(-1.235, Precision.round(-x, 3, RoundingMode.UP));
        Assertions.assertEquals(-1.2346, Precision.round(-x, 4, RoundingMode.UP));

        // MATH-151
        Assertions.assertEquals(39.25, Precision.round(39.245, 2, RoundingMode.HALF_UP));

        // special values
        Assertions.assertEquals(Double.NaN, Precision.round(Double.NaN, 2));
        Assertions.assertEquals(0.0, Precision.round(0.0, 2));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, Precision.round(Double.POSITIVE_INFINITY, 2));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, Precision.round(Double.NEGATIVE_INFINITY, 2));
        // comparison of positive and negative zero is not possible -> always equal thus do string comparison
        Assertions.assertEquals("-0.0", Double.toString(Precision.round(-0.0, 0)));
        Assertions.assertEquals("-0.0", Double.toString(Precision.round(-1e-10, 0)));
    }

    @Test
    void testRepresentableDelta() {
        int nonRepresentableCount = 0;
        final double x = 100;
        final int numTrials = 10000;
        for (int i = 0; i < numTrials; i++) {
            final double originalDelta = Math.random();
            final double delta = Precision.representableDelta(x, originalDelta);
            if (delta != originalDelta) {
                ++nonRepresentableCount;
            }
        }

        Assertions.assertTrue(nonRepresentableCount / (double) numTrials > 0.9);
    }

    @Test
    void testIssue721() {
        Assertions.assertEquals(-53, Math.getExponent(Precision.EPSILON));
        Assertions.assertEquals(-1022, Math.getExponent(Precision.SAFE_MIN));
    }

    @Test
    void testMath475() {
        final double a = 1.7976931348623182E16;
        final double b = Math.nextUp(a);

        double diff = Math.abs(a - b);
        // Because they are adjacent floating point numbers, "a" and "b" are
        // considered equal even though the allowed error is smaller than
        // their difference.
        Assertions.assertTrue(Precision.equals(a, b, 0.5 * diff));

        final double c = Math.nextUp(b);
        diff = Math.abs(a - c);
        // Because "a" and "c" are not adjacent, the tolerance is taken into
        // account for assessing equality.
        Assertions.assertTrue(Precision.equals(a, c, diff));
        Assertions.assertFalse(Precision.equals(a, c, Math.nextDown(1.0) * diff));
    }

    @Test
    void testMath475Float() {
        final float a = 1.7976931348623182E16f;
        final float b = Math.nextUp(a);

        float diff = Math.abs(a - b);
        // Because they are adjacent floating point numbers, "a" and "b" are
        // considered equal even though the allowed error is smaller than
        // their difference.
        Assertions.assertTrue(Precision.equals(a, b, 0.5f * diff));

        final float c = Math.nextUp(b);
        diff = Math.abs(a - c);
        // Because "a" and "c" are not adjacent, the tolerance is taken into
        // account for assessing equality.
        Assertions.assertTrue(Precision.equals(a, c, diff));
        Assertions.assertFalse(Precision.equals(a, c, Math.nextDown(1.0f) * diff));
    }

    @Test
    void testMath843() {
        final double afterEpsilon = Math.nextAfter(Precision.EPSILON,
                                                   Double.POSITIVE_INFINITY);

        // a) 1 + EPSILON is equal to 1.
        Assertions.assertEquals(1, 1 + Precision.EPSILON);

        // b) 1 + "the number after EPSILON" is not equal to 1.
        Assertions.assertNotEquals(1, 1 + afterEpsilon, 0.0);
    }

    @Test
    void testMath1127() {
        Assertions.assertFalse(Precision.equals(2.0, -2.0, 1));
        Assertions.assertTrue(Precision.equals(0.0, -0.0, 0));
        Assertions.assertFalse(Precision.equals(2.0f, -2.0f, 1));
        Assertions.assertTrue(Precision.equals(0.0f, -0.0f, 0));
    }

    @Test
    void testCreateEpsilonComparator() {
        final PrecisionComparator context = Precision.createEpsilonComparator(1e-3);

        Assertions.assertEquals(EpsilonPrecisionComparator.class, context.getClass());
        Assertions.assertEquals(1e-3, ((EpsilonPrecisionComparator) context).getEpsilon(), 0);
    }
}
