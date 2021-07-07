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

import java.util.function.DoubleUnaryOperator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for the {@link Angle} class.
 */
class AngleTest {
    @Test
    void testConstants() {
        Assertions.assertEquals(0d, Angle.Turn.ZERO.getAsDouble());
        Assertions.assertEquals(0d, Angle.Rad.ZERO.getAsDouble());
        Assertions.assertEquals(0d, Angle.Deg.ZERO.getAsDouble());
        Assertions.assertEquals(Math.PI, Angle.Rad.PI.getAsDouble());
        Assertions.assertEquals(2 * Math.PI, Angle.Rad.TWO_PI.getAsDouble());
        Assertions.assertEquals(2 * Math.PI, Angle.TWO_PI);
        Assertions.assertEquals(Math.PI / 2, Angle.PI_OVER_TWO);
    }

    @Test
    void testConversions() {
        final double a = 12.3456;
        final double tol = 1e-14;
        Angle.Turn t = Angle.Turn.of(a);
        Angle.Rad r = Angle.Rad.of(a);
        Angle.Deg d = Angle.Deg.of(a);
        Assertions.assertSame(t, t.toTurn());
        Assertions.assertSame(r, r.toRad());
        Assertions.assertSame(d, d.toDeg());
        Assertions.assertEquals(a, t.toRad().toDeg().toTurn().getAsDouble(), tol);
        Assertions.assertEquals(a, r.toTurn().toDeg().toRad().getAsDouble(), tol);
        Assertions.assertEquals(a, d.toTurn().toRad().toDeg().getAsDouble(), tol);
    }

    @Test
    void testEquals() {
        final double value = -12.3456789;
        final double nextValue = Math.nextUp(value);

        final Angle.Turn asTurn = Angle.Turn.of(value);
        Assertions.assertEquals(Angle.Turn.of(value), asTurn);
        Assertions.assertEquals(asTurn, asTurn);
        Assertions.assertNotEquals(asTurn, Angle.Turn.of(nextValue));
        Assertions.assertNotEquals(asTurn, null);

        final Angle.Rad asRad = Angle.Rad.of(value);
        Assertions.assertEquals(Angle.Rad.of(value), asRad);
        Assertions.assertEquals(asRad, asRad);
        Assertions.assertNotEquals(asRad, Angle.Rad.of(nextValue));
        Assertions.assertNotEquals(asRad, null);

        final Angle.Deg asDeg = Angle.Deg.of(value);
        Assertions.assertEquals(Angle.Deg.of(value), asDeg);
        Assertions.assertEquals(asDeg, asDeg);
        Assertions.assertNotEquals(asDeg, Angle.Deg.of(nextValue));
        Assertions.assertNotEquals(asDeg, null);

        Assertions.assertNotEquals(asDeg, asTurn);
        Assertions.assertNotEquals(asTurn, asRad);
        Assertions.assertNotEquals(asRad, asDeg);
    }

    @Test
    void testNormalizeRadians() {
        final double twopi = 2 * Math.PI;
        for (double a = -15.0; a <= 15.0; a += 0.1) {
            for (double b = -15.0; b <= 15.0; b += 0.2) {
                final double c = Angle.Rad.normalizer(b).applyAsDouble(a);
                Assertions.assertTrue(b <= c);
                Assertions.assertTrue(c <= b + twopi);
                double twoK = Math.rint((a - c) / Math.PI);
                Assertions.assertEquals(c, a - twoK * Math.PI, 1e-14);
            }
        }
    }

    @Test
    void testNormalizeAboveZero1() {
        final double value = 1.25;
        final double expected = 0.25;
        final double actual = Angle.Turn.WITHIN_0_AND_1.applyAsDouble(value);
        final double tol = Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol);
    }
    @Test
    void testNormalizeAboveZero2() {
        final double value = -0.75;
        final double expected = 0.25;
        final double actual = Angle.Turn.WITHIN_0_AND_1.applyAsDouble(value);
        final double tol = Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol);
    }
    @Test
    void testNormalizeAboveZero3() {
        final double value = -0.5 + 1e-10;
        final double expected = 0.5 + 1e-10;
        final double actual = Angle.Turn.WITHIN_0_AND_1.applyAsDouble(value);
        final double tol = Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol);
    }
    @Test
    void testNormalizeAroundZero() {
        final double value = 5 * Math.PI / 4;
        final double expected = Math.PI * (1d / 4 - 1);
        final double actual = Angle.Rad.WITHIN_MINUS_PI_AND_PI.applyAsDouble(value);
        final double tol = Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol);
    }

    @Test
    void testNormalizeVeryCloseToBounds() {
        final DoubleUnaryOperator nZero = Angle.Rad.WITHIN_MINUS_PI_AND_PI;
        final DoubleUnaryOperator nPi = Angle.Rad.WITHIN_0_AND_2PI;

        // arrange
        final double pi = Math.PI;
        double small = Math.ulp(Angle.TWO_PI);
        double tiny = 5e-17; // pi + tiny = pi (the value is too small to add to pi)

        // act/assert
        Assertions.assertEquals(Angle.TWO_PI - small, nPi.applyAsDouble(-small));
        Assertions.assertEquals(small, nPi.applyAsDouble(small));

        Assertions.assertEquals(pi - small, nZero.applyAsDouble(-pi - small));
        Assertions.assertEquals(-pi + small, nZero.applyAsDouble(pi + small));

        Assertions.assertEquals(0d, nPi.applyAsDouble(-tiny));
        Assertions.assertEquals(tiny, nPi.applyAsDouble(tiny));

        Assertions.assertEquals(-pi, nZero.applyAsDouble(-pi - tiny));
        Assertions.assertEquals(-pi, nZero.applyAsDouble(pi + tiny));
    }

    @Test
    void testHashCode() {
        // Test assumes that the internal representation is in "turns".
        final double value = -123.456789;
        final int expected = Double.valueOf(value).hashCode();
        final int actual = Angle.Turn.of(value).hashCode();
        Assertions.assertEquals(actual, expected);
    }

    @Test
    void testZero() {
        Assertions.assertEquals(0, Angle.Rad.ZERO.getAsDouble());
    }
    @Test
    void testPi() {
        Assertions.assertEquals(Math.PI, Angle.Rad.PI.getAsDouble());
    }

    @Test
    void testNormalizeRetainsInputPrecision() {
        final double aboveZero = Math.nextUp(0);
        final double belowZero = Math.nextDown(0);

        Assertions.assertEquals(aboveZero,
                                Angle.Rad.WITHIN_MINUS_PI_AND_PI.applyAsDouble(aboveZero));
        Assertions.assertEquals(aboveZero,
                                Angle.Rad.WITHIN_0_AND_2PI.applyAsDouble(aboveZero));

        Assertions.assertEquals(belowZero,
                                Angle.Rad.WITHIN_MINUS_PI_AND_PI.applyAsDouble(belowZero));
        Assertions.assertEquals(0,
                                Angle.Rad.WITHIN_0_AND_2PI.applyAsDouble(belowZero));
    }

    @Test
    void testNormalizePreciseLowerBound() {
        final double x = Math.PI / 3;
        final double above = Math.nextUp(x);
        final double below = Math.nextDown(x);

        final DoubleUnaryOperator normalizer = Angle.Rad.normalizer(x);

        Assertions.assertEquals(x, normalizer.applyAsDouble(x));
        Assertions.assertEquals(above, normalizer.applyAsDouble(above));

        // "below" is so close to "x" that below + Math.PI = x + Math.PI
        // In this case, we can't return below + Math.PI because that is exactly equal to the
        // upper bound of the range. Instead, we must return the lower bound of x.
        Assertions.assertEquals(x, normalizer.applyAsDouble(below));
    }
}
