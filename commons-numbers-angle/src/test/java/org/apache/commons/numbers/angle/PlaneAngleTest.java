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
package org.apache.commons.numbers.angle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for the {@link PlaneAngle} class.
 */
class PlaneAngleTest {
    @Test
    void testConversionTurns() {
        final double value = 12.3456;
        final PlaneAngle a = PlaneAngle.ofTurns(value);
        Assertions.assertEquals(value, a.toTurns(), 0d);
    }

    @Test
    void testConversionRadians() {
        final double one = 2 * Math.PI;
        final double value = 12.3456 * one;
        final PlaneAngle a = PlaneAngle.ofRadians(value);
        Assertions.assertEquals(value, a.toRadians(), 0d);
    }

    @Test
    void testConversionDegrees() {
        final double one = 360;
        final double value = 12.3456 * one;
        final PlaneAngle a = PlaneAngle.ofDegrees(value);
        Assertions.assertEquals(value, a.toDegrees(), 0d);
    }

    @Test
    void testNormalizeRadians() {
        for (double a = -15.0; a <= 15.0; a += 0.1) {
            for (double b = -15.0; b <= 15.0; b += 0.2) {
                final PlaneAngle aA = PlaneAngle.ofRadians(a);
                final PlaneAngle aB = PlaneAngle.ofRadians(b);
                final double c = aA.normalize(aB).toRadians();
                Assertions.assertTrue((b - Math.PI) <= c);
                Assertions.assertTrue(c <= (b + Math.PI));
                double twoK = Math.rint((a - c) / Math.PI);
                Assertions.assertEquals(c, a - twoK * Math.PI, 1e-14);
            }
        }
    }

    @Test
    void testNormalizeMixed() {
        for (double a = -15.0; a <= 15.0; a += 0.1) {
            for (double b = -15.0; b <= 15.0; b += 0.2) {
                final PlaneAngle aA = PlaneAngle.ofDegrees(a);
                final PlaneAngle aB = PlaneAngle.ofRadians(b);
                final double c = aA.normalize(aB).toTurns();
                Assertions.assertTrue((aB.toTurns() - 0.5) <= c);
                Assertions.assertTrue(c <= (aB.toTurns() + 0.5));
                double twoK = Math.rint(aA.toTurns() - c);
                Assertions.assertEquals(c, aA.toTurns() - twoK, 1e-14);
            }
        }
    }

    @Test
    void testNormalizeAroundZero1() {
        final double value = 1.25;
        final double expected = 0.25;
        final double actual = PlaneAngle.ofTurns(value).normalize(PlaneAngle.ZERO).toTurns();
        final double tol = Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol);
    }
    @Test
    void testNormalizeAroundZero2() {
        final double value = 0.75;
        final double expected = -0.25;
        final double actual = PlaneAngle.ofTurns(value).normalize(PlaneAngle.ZERO).toTurns();
        final double tol = Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol);
    }
    @Test
    void testNormalizeAroundZero3() {
        final double value = 0.5 + 1e-10;
        final double expected = -0.5 + 1e-10;
        final double actual = PlaneAngle.ofTurns(value).normalize(PlaneAngle.ZERO).toTurns();
        final double tol = Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol);
    }
    @Test
    void testNormalizeAroundZero4() {
        final double value = 5 * Math.PI / 4;
        final double expected = Math.PI * (1d / 4 - 1);
        final double actual = PlaneAngle.ofRadians(value).normalize(PlaneAngle.ZERO).toRadians();
        final double tol = Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol);
    }

    @Test
    void testNormalizeUpperAndLowerBounds() {
        // arrange
        double eps = 1e-15;

        // act/assert
        Assertions.assertEquals(-0.5, PlaneAngle.ofTurns(-0.5).normalize(PlaneAngle.ZERO).toTurns(), eps);
        Assertions.assertEquals(-0.5, PlaneAngle.ofTurns(0.5).normalize(PlaneAngle.ZERO).toTurns(), eps);

        Assertions.assertEquals(-0.5, PlaneAngle.ofTurns(-1.5).normalize(PlaneAngle.ZERO).toTurns(), eps);
        Assertions.assertEquals(-0.5, PlaneAngle.ofTurns(1.5).normalize(PlaneAngle.ZERO).toTurns(), eps);

        Assertions.assertEquals(0.0, PlaneAngle.ofTurns(0).normalize(PlaneAngle.PI).toTurns(), eps);
        Assertions.assertEquals(0.0, PlaneAngle.ofTurns(1).normalize(PlaneAngle.PI).toTurns(), eps);

        Assertions.assertEquals(0.0, PlaneAngle.ofTurns(-1).normalize(PlaneAngle.PI).toTurns(), eps);
        Assertions.assertEquals(0.0, PlaneAngle.ofTurns(2).normalize(PlaneAngle.PI).toTurns(), eps);
    }

    @Test
    void testNormalizeVeryCloseToBounds() {
        // arrange
        double eps = 1e-22;

        double small = 1e-16;
        double tiny = 1e-18; // 0.5 + tiny = 0.5 (the value is too small to add to 0.5)

        // act/assert
        Assertions.assertEquals(1.0 - small, PlaneAngle.ofTurns(-small).normalize(PlaneAngle.PI).toTurns(), eps);
        Assertions.assertEquals(small, PlaneAngle.ofTurns(small).normalize(PlaneAngle.PI).toTurns(), eps);

        Assertions.assertEquals(0.5 - small, PlaneAngle.ofTurns(-0.5 - small).normalize(PlaneAngle.ZERO).toTurns(), eps);
        Assertions.assertEquals(-0.5 + small, PlaneAngle.ofTurns(0.5 + small).normalize(PlaneAngle.ZERO).toTurns(), eps);

        Assertions.assertEquals(0.0, PlaneAngle.ofTurns(-tiny).normalize(PlaneAngle.PI).toTurns(), eps);
        Assertions.assertEquals(tiny, PlaneAngle.ofTurns(tiny).normalize(PlaneAngle.PI).toTurns(), eps);

        Assertions.assertEquals(-0.5, PlaneAngle.ofTurns(-0.5 - tiny).normalize(PlaneAngle.ZERO).toTurns(), eps);
        Assertions.assertEquals(-0.5, PlaneAngle.ofTurns(0.5 + tiny).normalize(PlaneAngle.ZERO).toTurns(), eps);
    }

    @Test
    void testHashCode() {
        // Test assumes that the internal representation is in "turns".
        final double value = -123.456789;
        final int expected = Double.valueOf(value).hashCode();
        final int actual = PlaneAngle.ofTurns(value).hashCode();
        Assertions.assertEquals(actual, expected);
    }

    @Test
    void testEquals() {
        final double value = 12345.6789;
        final PlaneAngle a = PlaneAngle.ofRadians(value);
        Assertions.assertTrue(a.equals(a));
        Assertions.assertTrue(a.equals(PlaneAngle.ofRadians(value)));
        Assertions.assertFalse(a.equals(PlaneAngle.ofRadians(Math.nextUp(value))));
        Assertions.assertFalse(a.equals(new Object()));
        Assertions.assertFalse(a.equals(null));
    }

    @Test
    void testZero() {
        Assertions.assertEquals(0, PlaneAngle.ZERO.toRadians(), 0d);
    }
    @Test
    void testPi() {
        Assertions.assertEquals(Math.PI, PlaneAngle.PI.toRadians(), 0d);
    }
}
