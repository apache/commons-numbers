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
package org.apache.commons.numbers.angle;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for the {@link PlaneAngle} class.
 */
public class PlaneAngleTest {
    @Test
    public void testConversionTurns() {
        final double value = 12.3456;
        final PlaneAngle a = PlaneAngle.ofTurns(value);
        Assert.assertEquals(value, a.toTurns(), Math.ulp(value));
    }

    @Test
    public void testConversionRadians() {
        final double value = 12.3456;
        final PlaneAngle a = PlaneAngle.ofRadians(value);
        Assert.assertEquals(value, a.toRadians(), Math.ulp(value));
    }

    @Test
    public void testConversionDegrees() {
        final double value = 12.3456;
        final PlaneAngle a = PlaneAngle.ofDegrees(value);
        Assert.assertEquals(value, a.toDegrees(), Math.ulp(value));
    }

    @Test
    public void testNormalizeRadians() {
        for (double a = -15.0; a <= 15.0; a += 0.1) {
            for (double b = -15.0; b <= 15.0; b += 0.2) {
                final PlaneAngle aA = PlaneAngle.ofRadians(a);
                final PlaneAngle aB = PlaneAngle.ofRadians(b);
                final double c = aA.normalize(aB).toRadians();
                Assert.assertTrue((b - Math.PI) <= c);
                Assert.assertTrue(c <= (b + Math.PI));
                double twoK = Math.rint((a - c) / Math.PI);
                Assert.assertEquals(c, a - twoK * Math.PI, 1e-14);
            }
        }
    }

    @Test
    public void testNormalizeMixed() {
        for (double a = -15.0; a <= 15.0; a += 0.1) {
            for (double b = -15.0; b <= 15.0; b += 0.2) {
                final PlaneAngle aA = PlaneAngle.ofDegrees(a);
                final PlaneAngle aB = PlaneAngle.ofRadians(b);
                final double c = aA.normalize(aB).toTurns();
                Assert.assertTrue((aB.toTurns() - 0.5) <= c);
                Assert.assertTrue(c <= (aB.toTurns() + 0.5));
                double twoK = Math.rint((aA.toTurns() - c));
                Assert.assertEquals(c, aA.toTurns() - twoK, 1e-14);
            }
        }
    }

    @Test
    public void testNormalizeAroundZero1() {
        final double value = 1.25;
        final double expected = 0.25;
        final double actual = PlaneAngle.ofTurns(value).normalize(PlaneAngle.ZERO).toTurns();
        final double tol = Math.ulp(expected);
        Assert.assertEquals(expected, actual, tol);
    }
    @Test
    public void testNormalizeAroundZero2() {
        final double value = 0.75;
        final double expected = -0.25;
        final double actual = PlaneAngle.ofTurns(value).normalize(PlaneAngle.ZERO).toTurns();
        final double tol = Math.ulp(expected);
        Assert.assertEquals(expected, actual, tol);
    }
    @Test
    public void testNormalizeAroundZero3() {
        final double value = 0.5 + 1e-10;
        final double expected = -0.5 + 1e-10;
        final double actual = PlaneAngle.ofTurns(value).normalize(PlaneAngle.ZERO).toTurns();
        final double tol = Math.ulp(expected);
        Assert.assertEquals(expected, actual, tol);
    }
    @Test
    public void testNormalizeAroundZero4() {
        final double value = 5 * Math.PI / 4;
        final double expected = Math.PI * (1d / 4 - 1);
        final double actual = PlaneAngle.ofRadians(value).normalize(PlaneAngle.ZERO).toRadians();
        final double tol = Math.ulp(expected);
        Assert.assertEquals(expected, actual, tol);
    }

    @Test
    public void testHashCode() {
        // Test assumes that the internal representation is in "turns".
        final double value = -123.456789;
        final int expected = new Double(value).hashCode();
        final int actual = PlaneAngle.ofTurns(value).hashCode();
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testEquals1() {
        final double value = 12345.6789;
        final PlaneAngle a = PlaneAngle.ofRadians(value);
        final PlaneAngle b = PlaneAngle.ofRadians(value);
        Assert.assertTrue(a.equals(b));
    }
    @Test
    public void testEquals2() {
        final PlaneAngle a = PlaneAngle.ofRadians(153768.373486587);
        final PlaneAngle b = null;
        Assert.assertFalse(a.equals(b));
    }
    @Test
    public void testEquals3() {
        final double value = 0.987654321;
        final PlaneAngle a = PlaneAngle.ofRadians(value);
        final PlaneAngle b = PlaneAngle.ofRadians(value + 1e-16);
        Assert.assertFalse(a.equals(b));
    }

    @Test
    public void testZero() {
        Assert.assertEquals(0, PlaneAngle.ZERO.toRadians(), 0d);
    }
    @Test
    public void testPi() {
        Assert.assertEquals(Math.PI, PlaneAngle.PI.toRadians(), 0d);
    }
}
