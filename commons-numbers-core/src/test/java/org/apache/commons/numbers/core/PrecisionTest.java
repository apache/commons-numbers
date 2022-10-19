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

import java.util.Arrays;
import java.util.Collections;
import java.math.RoundingMode;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for the {@link Precision} class.
 *
 */
class PrecisionTest {

    // Interfaces to allow testing variants with the same conditions.
    // All methods that should be tested using these interfaces as the
    // symmetry is asserted.

    @FunctionalInterface
    private interface Equals {
        default boolean equals(double a, double b) {
            final boolean r = equalsImp(a, b);
            Assertions.assertEquals(r, equalsImp(b, a),
                () -> String.format("equals(%s, %s) != equals(%s, %s)", a, b, b, a));
            return r;
        }
        boolean equalsImp(double a, double b);
    }

    @FunctionalInterface
    private interface EqualsWithDelta {
        default boolean equals(double a, double b, double delta) {
            final boolean r = equalsImp(a, b, delta);
            Assertions.assertEquals(r, equalsImp(b, a, delta),
                () -> String.format("equals(%s, %s, %s) != equals(%s, %s, %s)", a, b, delta, b, a, delta));
            return r;
        }
        boolean equalsImp(double a, double b, double delta);
    }

    @FunctionalInterface
    private interface EqualsWithUlps {
        default boolean equals(double a, double b, int ulps) {
            final boolean r = equalsImp(a, b, ulps);
            Assertions.assertEquals(r, equalsImp(b, a, ulps),
                () -> String.format("equals(%s, %s, %d) != equals(%s, %s, %d)", a, b, ulps, b, a, ulps));
            return r;
        }
        boolean equalsImp(double a, double b, int ulps);
    }

    @FunctionalInterface
    private interface FloatEquals {
        default boolean equals(float a, float b) {
            final boolean r = equalsImp(a, b);
            Assertions.assertEquals(r, equalsImp(b, a),
                () -> String.format("equals(%s, %s) != equals(%s, %s)", a, b, b, a));
            return r;
        }
        boolean equalsImp(float a, float b);
    }

    @FunctionalInterface
    private interface FloatEqualsWithDelta {
        default boolean equals(float a, float b, float delta) {
            final boolean r = equalsImp(a, b, delta);
            Assertions.assertEquals(r, equalsImp(b, a, delta),
                () -> String.format("equals(%s, %s, %s) != equals(%s, %s, %s)", a, b, delta, b, a, delta));
            return r;
        }
        boolean equalsImp(float a, float b, float delta);
    }

    @FunctionalInterface
    private interface FloatEqualsWithUlps {
        default boolean equals(float a, float b, int ulps) {
            final boolean r = equalsImp(a, b, ulps);
            Assertions.assertEquals(r, equalsImp(b, a, ulps),
                () -> String.format("equals(%s, %s, %d) != equals(%s, %s, %d)", a, b, ulps, b, a, ulps));
            return r;
        }
        boolean equalsImp(float a, float b, int ulps);
    }

    @FunctionalInterface
    private interface CompareFunction {
        /**
         * Compare two values.
         *
         * @param a First value
         * @param b Second value
         * @return 0 if the value are considered equal, -1 if the first is smaller than
         * the second, 1 if the first is larger than the second.
         */
        int compare(double a, double b);
    }

    @FunctionalInterface
    private interface CompareToWithDelta {
        default int compareTo(double a, double b, double delta) {
            final int r = compareToImp(a, b, delta);
            Assertions.assertEquals(r, -compareToImp(b, a, delta),
                () -> String.format("compareTo(%s, %s, %s) != -compareTo(%s, %s, %s)", a, b, delta, b, a, delta));
            return r;
        }
        int compareToImp(double a, double b, double delta);
    }

    @FunctionalInterface
    private interface CompareToWithUlps {
        default int compareTo(double a, double b, int ulps) {
            final int r = compareToImp(a, b, ulps);
            Assertions.assertEquals(r, -compareToImp(b, a, ulps),
                () -> String.format("compareTo(%s, %s, %d) != -compareTo(%s, %s, %d)", a, b, ulps, b, a, ulps));
            return r;
        }
        int compareToImp(double a, double b, int ulps);
    }

    @Test
    void testEqualsWithRelativeTolerance() {
        final EqualsWithDelta fun = Precision::equalsWithRelativeTolerance;

        Assertions.assertTrue(fun.equals(0d, 0d, 0d));
        Assertions.assertTrue(fun.equals(0d, 1 / Double.NEGATIVE_INFINITY, 0d));

        final double eps = 1e-14;
        Assertions.assertFalse(fun.equals(1.987654687654968, 1.987654687654988, eps));
        Assertions.assertTrue(fun.equals(1.987654687654968, 1.987654687654987, eps));
        Assertions.assertFalse(fun.equals(1.987654687654968, 1.987654687654948, eps));
        Assertions.assertTrue(fun.equals(1.987654687654968, 1.987654687654949, eps));

        Assertions.assertFalse(fun.equals(Precision.SAFE_MIN, 0.0, eps));

        Assertions.assertFalse(fun.equals(1.0000000000001e-300, 1e-300, eps));
        Assertions.assertTrue(fun.equals(1.00000000000001e-300, 1e-300, eps));

        Assertions.assertFalse(fun.equals(Double.NEGATIVE_INFINITY, 1.23, eps));
        Assertions.assertFalse(fun.equals(Double.POSITIVE_INFINITY, 1.23, eps));

        Assertions.assertTrue(fun.equals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, eps));
        Assertions.assertTrue(fun.equals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, eps));
        Assertions.assertFalse(fun.equals(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, eps));

        Assertions.assertFalse(fun.equals(Double.NaN, 1.23, eps));
        Assertions.assertFalse(fun.equals(Double.NaN, Double.NaN, eps));
    }

    @Test
    void testEqualsIncludingNaN() {
        final Equals fun = Precision::equalsIncludingNaN;

        double[] testArray = {
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            1d,
            0d };
        for (int i = 0; i < testArray.length; i++) {
            for (int j = 0; j < testArray.length; j++) {
                if (i == j) {
                    Assertions.assertTrue(fun.equals(testArray[i], testArray[j]));
                    Assertions.assertTrue(fun.equals(testArray[j], testArray[i]));
                } else {
                    Assertions.assertFalse(fun.equals(testArray[i], testArray[j]));
                    Assertions.assertFalse(fun.equals(testArray[j], testArray[i]));
                }
            }
        }
    }

    @Test
    void testEqualsWithAllowedDelta() {
        assertEqualsWithAllowedDelta(Precision::equals, false);
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
        assertEqualsWithAllowedUlps(Precision::equals, false, false);
    }

    @Test
    void testEqualsWithImplicitAllowedUlpsOf1() {
        // Use the version without the ulp argument
        assertEqualsWithAllowedUlps((a, b, ulp) -> Precision.equals(a, b), false, true);
    }

    @Test
    void testEqualsIncludingNaNWithAllowedUlps() {
        assertEqualsWithAllowedUlps(Precision::equalsIncludingNaN, true, false);
    }

    private static void assertEqualsWithAllowedUlps(EqualsWithUlps fun,
            boolean nanAreEqual, boolean fixed1Ulp) {
        Assertions.assertTrue(fun.equals(0.0, -0.0, 1));
        Assertions.assertTrue(fun.equals(Double.MIN_VALUE, -0.0, 1));
        Assertions.assertFalse(fun.equals(Double.MIN_VALUE, -Double.MIN_VALUE, 1));

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
                Assertions.assertFalse(fun.equals(value, value, -1), "Negative ULP should be supported");
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

        if (!fixed1Ulp) {
            Assertions.assertFalse(fun.equals(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Integer.MAX_VALUE));
            Assertions.assertFalse(fun.equals(0, Double.MAX_VALUE, Integer.MAX_VALUE));
            // Here: f == 5.304989477E-315;
            // it is used to test the maximum ULP distance between two opposite sign numbers.
            final double f = Double.longBitsToDouble(1L << 30);
            Assertions.assertFalse(fun.equals(-f, f, Integer.MAX_VALUE));
            Assertions.assertTrue(fun.equals(-f, Math.nextDown(f), Integer.MAX_VALUE));
            Assertions.assertTrue(fun.equals(Math.nextUp(-f), f, Integer.MAX_VALUE));
            // Maximum distance between same sign numbers.
            final double f2 = Double.longBitsToDouble((1L << 30) + Integer.MAX_VALUE);
            Assertions.assertTrue(fun.equals(f, f2, Integer.MAX_VALUE));
            Assertions.assertFalse(fun.equals(f, Math.nextUp(f2), Integer.MAX_VALUE));
            Assertions.assertFalse(fun.equals(Math.nextDown(f), f2, Integer.MAX_VALUE));
        }
    }

    // Tests for floating point equality match the above tests with arguments
    // converted to float

    @Test
    void testFloatEqualsIncludingNaN() {
        final FloatEquals fun = Precision::equalsIncludingNaN;

        float[] testArray = {
            Float.NaN,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            1f,
            0f };
        for (int i = 0; i < testArray.length; i++) {
            for (int j = 0; j < testArray.length; j++) {
                if (i == j) {
                    Assertions.assertTrue(fun.equals(testArray[i], testArray[j]));
                    Assertions.assertTrue(fun.equals(testArray[j], testArray[i]));
                } else {
                    Assertions.assertFalse(fun.equals(testArray[i], testArray[j]));
                    Assertions.assertFalse(fun.equals(testArray[j], testArray[i]));
                }
            }
        }
    }

    @Test
    void testFloatEqualsWithAllowedDelta() {
        assertFloatEqualsWithAllowedDelta(Precision::equals, false);
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
        assertFloatEqualsWithAllowedUlps(Precision::equals, false, false);
    }

    @Test
    void testFloatEqualsWithImplicitAllowedUlpsOf1() {
        // Use the version without the ulp argument
        assertFloatEqualsWithAllowedUlps((a, b, ulp) -> Precision.equals(a, b), false, true);
    }

    @Test
    void testFloatEqualsIncludingNaNWithAllowedUlps() {
        assertFloatEqualsWithAllowedUlps(Precision::equalsIncludingNaN, true, false);
    }

    private static void assertFloatEqualsWithAllowedUlps(FloatEqualsWithUlps fun,
            boolean nanAreEqual, boolean fixed1Ulp) {
        Assertions.assertTrue(fun.equals(0.0f, -0.0f, 1));
        Assertions.assertTrue(fun.equals(Float.MIN_VALUE, -0.0f, 1));
        Assertions.assertFalse(fun.equals(Float.MIN_VALUE, -Float.MIN_VALUE, 1));

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
                Assertions.assertFalse(fun.equals(value, value, -1), "Negative ULP should be supported");
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

        if (!fixed1Ulp) {
            Assertions.assertFalse(fun.equals(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Integer.MAX_VALUE));
            // The 31-bit integer specification of the max positive ULP allows an extremely
            // large range of a 23-bit mantissa and 8-bit exponent
            Assertions.assertTrue(fun.equals(0, Float.MAX_VALUE, Integer.MAX_VALUE));
            // Here: f == 2;
            // it is used to test the maximum ULP distance between two opposite sign numbers.
            final float f = Float.intBitsToFloat(1 << 30);
            Assertions.assertFalse(fun.equals(-f, f, Integer.MAX_VALUE));
            Assertions.assertTrue(fun.equals(-f, Math.nextDown(f), Integer.MAX_VALUE));
            Assertions.assertTrue(fun.equals(Math.nextUp(-f), f, Integer.MAX_VALUE));
            // Maximum distance between same sign finite numbers is not possible as the upper
            // limit is NaN. Check that it is not equal to anything.
            final float f2 = Float.intBitsToFloat(Integer.MAX_VALUE);
            Assertions.assertEquals(Double.NaN, f2);
            Assertions.assertFalse(fun.equals(f2, Float.MAX_VALUE, Integer.MAX_VALUE));
            Assertions.assertFalse(fun.equals(f2, 0, Integer.MAX_VALUE));
        }
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
    void testSortWithCompareTo() {
        final double eps = 0.1;
        final double v1 = 0.02;
        final double v2 = v1 + 0.5 * eps;
        final CompareToWithDelta fun = Precision::compareTo;
        assertSortWithCompareTo((a, b) -> fun.compareTo(a, b, eps), v1, v2, 13, 42);
    }

    @Test
    void testSortWithCompareToMaxUlps() {
        final int ulps = 1000;
        final double v1 = 0.02;
        final double v2 = v1 + 0.5 * ulps * Math.ulp(v1);
        final CompareToWithUlps fun = Precision::compareTo;
        assertSortWithCompareTo((a, b) -> fun.compareTo(a, b, ulps), v1, v2, 0.75, 2.23);
    }

    /**
     * Assert sorting with the provided compare function.
     *
     * <p>The test makes assumptions on the input data using the compare function:
     * <pre>
     * v1 == v2; (v1,v2) < v3 < v4
     * </pre>
     *
     * <p>The data will be supplemented with NaN and infinite values to ensure sorting is correct.
     */
    private static void assertSortWithCompareTo(CompareFunction compare,
                                                double v1, double v2, double v3, double v4) {
        Assertions.assertEquals(0, compare.compare(v1, v2));
        Assertions.assertEquals(-1, compare.compare(v1, v3));
        Assertions.assertEquals(-1, compare.compare(v2, v3));
        Assertions.assertEquals(-1, compare.compare(v3, v4));
        final Double[] array = {Double.NaN, v1, v2, Double.NaN, v3, v4, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (int i = 0; i < 10; i++) {
            Collections.shuffle(Arrays.asList(array));
            Arrays.sort(array, compare::compare);

            for (int j = 0; j < array.length - 1; j++) {
                final int c = compare.compare(array[j],
                                            array[j + 1]);
                // Check that order is consistent with the comparison function.
                Assertions.assertNotEquals(1, c);
            }
            Assertions.assertEquals(Double.NEGATIVE_INFINITY, array[0]);
            Assertions.assertTrue(array[1] == v1 || array[1] == v2);
            Assertions.assertTrue(array[2] == v1 || array[2] == v2);
            Assertions.assertEquals(v3, array[3]);
            Assertions.assertEquals(v4, array[4]);
            Assertions.assertEquals(Double.POSITIVE_INFINITY, array[5]);
            Assertions.assertEquals(Double.NaN, array[6]);
            Assertions.assertEquals(Double.NaN, array[7]);
        }
    }

    @Test
    void testCompareToMaxUlps() {
        final CompareToWithUlps fun = Precision::compareTo;

        double a = 152.32;
        double delta = Math.ulp(a);
        for (int i = 0; i <= 10; ++i) {
            if (i <= 5) {
                Assertions.assertEquals(+0, fun.compareTo(a, a + i * delta, 5));
                Assertions.assertEquals(+0, fun.compareTo(a, a - i * delta, 5));
            } else {
                Assertions.assertEquals(-1, fun.compareTo(a, a + i * delta, 5));
                Assertions.assertEquals(+1, fun.compareTo(a, a - i * delta, 5));
            }
        }

        Assertions.assertEquals(+0, fun.compareTo(-0.0, 0.0, 0));

        Assertions.assertEquals(-1, fun.compareTo(-Double.MIN_VALUE, -0.0, 0));
        Assertions.assertEquals(+0, fun.compareTo(-Double.MIN_VALUE, -0.0, 1));
        Assertions.assertEquals(-1, fun.compareTo(-Double.MIN_VALUE, +0.0, 0));
        Assertions.assertEquals(+0, fun.compareTo(-Double.MIN_VALUE, +0.0, 1));

        Assertions.assertEquals(+1, fun.compareTo(+Double.MIN_VALUE, -0.0, 0));
        Assertions.assertEquals(+0, fun.compareTo(+Double.MIN_VALUE, -0.0, 1));
        Assertions.assertEquals(+1, fun.compareTo(+Double.MIN_VALUE, +0.0, 0));
        Assertions.assertEquals(+0, fun.compareTo(+Double.MIN_VALUE, +0.0, 1));

        Assertions.assertEquals(-1, fun.compareTo(-Double.MIN_VALUE, Double.MIN_VALUE, 0));
        Assertions.assertEquals(-1, fun.compareTo(-Double.MIN_VALUE, Double.MIN_VALUE, 1));
        Assertions.assertEquals(+0, fun.compareTo(-Double.MIN_VALUE, Double.MIN_VALUE, 2));

        Assertions.assertEquals(+0, fun.compareTo(Double.MAX_VALUE, Double.POSITIVE_INFINITY, 1));
        Assertions.assertEquals(-1, fun.compareTo(Double.MAX_VALUE, Double.POSITIVE_INFINITY, 0));

        // NaN should be after all non-NaN numbers
        Assertions.assertEquals(+1, fun.compareTo(Double.NaN, Double.MAX_VALUE, Integer.MAX_VALUE));
        Assertions.assertEquals(+1, fun.compareTo(Double.NaN, Double.POSITIVE_INFINITY, Integer.MAX_VALUE));
        Assertions.assertEquals(+1, fun.compareTo(Double.NaN, 42, Integer.MAX_VALUE));
        Assertions.assertEquals(0, fun.compareTo(Double.NaN, Double.NaN, Integer.MAX_VALUE));
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
        final EqualsWithDelta fun = Precision::equals;

        final double a = 1.7976931348623182E16;
        final double b = Math.nextUp(a);

        double diff = Math.abs(a - b);
        // Because they are adjacent floating point numbers, "a" and "b" are
        // considered equal even though the allowed error is smaller than
        // their difference.
        Assertions.assertTrue(fun.equals(a, b, 0.5 * diff));

        final double c = Math.nextUp(b);
        diff = Math.abs(a - c);
        // Because "a" and "c" are not adjacent, the tolerance is taken into
        // account for assessing equality.
        Assertions.assertTrue(fun.equals(a, c, diff));
        Assertions.assertFalse(fun.equals(a, c, Math.nextDown(1.0) * diff));
    }

    @Test
    void testMath475Float() {
        final FloatEqualsWithDelta fun = Precision::equals;

        final float a = 1.7976931348623182E16f;
        final float b = Math.nextUp(a);

        float diff = Math.abs(a - b);
        // Because they are adjacent floating point numbers, "a" and "b" are
        // considered equal even though the allowed error is smaller than
        // their difference.
        Assertions.assertTrue(fun.equals(a, b, 0.5f * diff));

        final float c = Math.nextUp(b);
        diff = Math.abs(a - c);
        // Because "a" and "c" are not adjacent, the tolerance is taken into
        // account for assessing equality.
        Assertions.assertTrue(fun.equals(a, c, diff));
        Assertions.assertFalse(fun.equals(a, c, Math.nextDown(1.0f) * diff));
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
        final EqualsWithUlps fun = Precision::equals;
        Assertions.assertFalse(fun.equals(2.0, -2.0, 1));
        Assertions.assertTrue(fun.equals(0.0, -0.0, 0));
        final FloatEqualsWithUlps fun2 = Precision::equals;
        Assertions.assertFalse(fun2.equals(2.0f, -2.0f, 1));
        Assertions.assertTrue(fun2.equals(0.0f, -0.0f, 0));
    }
}
