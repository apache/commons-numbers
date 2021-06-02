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

import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for the {@link Angle} class.
 */
class AngleTest {
    @Test
    void testConstants() {
        Assertions.assertEquals(0d, Angle.Turn.ZERO.getAsDouble(), 0d);
        Assertions.assertEquals(0d, Angle.Rad.ZERO.getAsDouble(), 0d);
        Assertions.assertEquals(0d, Angle.Deg.ZERO.getAsDouble(), 0d);
        Assertions.assertEquals(Math.PI, Angle.Rad.PI.getAsDouble(), 0d);
        Assertions.assertEquals(2 * Math.PI, Angle.Rad.TWO_PI.getAsDouble(), 0d);
    }

    @Test
    void testConversionTurns() {
        final double value = 12.3456;
        final Angle a = Angle.Turn.of(value);
        Assertions.assertEquals(value, a.getAsDouble());
    }

    @Test
    void testConversionRadians() {
        final double one = 2 * Math.PI;
        final double value = 12.3456 * one;
        final Angle a = Angle.Rad.of(value);
        Assertions.assertEquals(value, a.toRad().getAsDouble());
    }

    @Test
    void testConversionDegrees() {
        final double one = 360;
        final double value = 12.3456 * one;
        final Angle a = Angle.Deg.of(value);
        Assertions.assertEquals(value, a.toDeg().getAsDouble());
    }

    @Test
    void testNormalizeRadians() {
        for (double a = -15.0; a <= 15.0; a += 0.1) {
            for (double b = -15.0; b <= 15.0; b += 0.2) {
                final Angle.Rad aA = Angle.Rad.of(a);
                final Angle.Rad aB = Angle.Rad.of(b);
                final double c = Angle.Rad.normalizer(aB).apply(aA).getAsDouble();
                Assertions.assertTrue((b - Math.PI) <= c);
                Assertions.assertTrue(c <= (b + Math.PI));
                double twoK = Math.rint((a - c) / Math.PI);
                Assertions.assertEquals(c, a - twoK * Math.PI, 1e-14);
            }
        }
    }

    @Test
    void testNormalizeAroundZero1() {
        final double value = 1.25;
        final double expected = 0.25;
        final double actual = Angle.Turn.normalizer(Angle.Turn.ZERO).apply(Angle.Turn.of(value)).getAsDouble();
        final double tol = Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol);
    }
    @Test
    void testNormalizeAroundZero2() {
        final double value = 0.75;
        final double expected = -0.25;
        final double actual = Angle.Turn.normalizer(Angle.Turn.ZERO).apply(Angle.Turn.of(value)).getAsDouble();
        final double tol = Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol);
    }
    @Test
    void testNormalizeAroundZero3() {
        final double value = 0.5 + 1e-10;
        final double expected = -0.5 + 1e-10;
        final double actual = Angle.Turn.normalizer(Angle.Turn.ZERO).apply(Angle.Turn.of(value)).getAsDouble();
        final double tol = Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol);
    }
    @Test
    void testNormalizeAroundZero4() {
        final double value = 5 * Math.PI / 4;
        final double expected = Math.PI * (1d / 4 - 1);
        final double actual = Angle.Rad.normalizer(Angle.Rad.ZERO).apply(Angle.Rad.of(value)).getAsDouble();
        final double tol = Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol);
    }

    @Test
    void testNormalizeUpperAndLowerBounds() {
        final UnaryOperator<Angle.Rad> nZero = Angle.Rad.normalizer(Angle.Rad.ZERO);
        final UnaryOperator<Angle.Rad> nPi = Angle.Rad.normalizer(Angle.Rad.PI);

        // act/assert
        Assertions.assertEquals(-0.5, nZero.apply(Angle.Turn.of(-0.5).toRad()).toTurn().getAsDouble(), 0d);
        Assertions.assertEquals(-0.5, nZero.apply(Angle.Turn.of(0.5).toRad()).toTurn().getAsDouble(), 0d);

        Assertions.assertEquals(-0.5, nZero.apply(Angle.Turn.of(-1.5).toRad()).toTurn().getAsDouble(), 0d);
        Assertions.assertEquals(-0.5, nZero.apply(Angle.Turn.of(1.5).toRad()).toTurn().getAsDouble(), 0d);

        Assertions.assertEquals(0.0, nPi.apply(Angle.Turn.of(0).toRad()).toTurn().getAsDouble(), 0d);
        Assertions.assertEquals(0.0, nPi.apply(Angle.Turn.of(1).toRad()).toTurn().getAsDouble(), 0d);

        Assertions.assertEquals(0.0, nPi.apply(Angle.Turn.of(-1).toRad()).toTurn().getAsDouble(), 0d);
        Assertions.assertEquals(0.0, nPi.apply(Angle.Turn.of(2).toRad()).toTurn().getAsDouble(), 0d);
    }

    @Test
    void testNormalizeVeryCloseToBounds() {
        final UnaryOperator<Angle.Rad> nZero = Angle.Rad.normalizer(Angle.Rad.ZERO);
        final UnaryOperator<Angle.Rad> nPi = Angle.Rad.normalizer(Angle.Rad.PI);

        // arrange
        final double pi = Math.PI;
        final double twopi = 2 * pi;
        double small = Math.ulp(twopi);
        double tiny = 5e-17; // pi + tiny = pi (the value is too small to add to pi)

        // act/assert
        Assertions.assertEquals(twopi - small, nPi.apply(Angle.Rad.of(-small)).getAsDouble(), 0d);
        Assertions.assertEquals(small, nPi.apply(Angle.Rad.of(small)).getAsDouble(), 0d);

        Assertions.assertEquals(pi - small, nZero.apply(Angle.Rad.of(-pi - small)).getAsDouble(), 0d);
        Assertions.assertEquals(-pi + small, nZero.apply(Angle.Rad.of(pi + small)).getAsDouble(), 0d);

        Assertions.assertEquals(0d, nPi.apply(Angle.Rad.of(-tiny)).getAsDouble(), 0d);
        Assertions.assertEquals(tiny, nPi.apply(Angle.Rad.of(tiny)).getAsDouble(), 0d);

        Assertions.assertEquals(-pi, nZero.apply(Angle.Rad.of(-pi - tiny)).getAsDouble(), 0d);
        Assertions.assertEquals(-pi, nZero.apply(Angle.Rad.of(pi + tiny)).getAsDouble(), 0d);
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
    void testEquals() {
        final double value = 12345.6789;
        final Angle a = Angle.Rad.of(value);
        Assertions.assertTrue(a.equals(a));
        Assertions.assertTrue(a.equals(Angle.Rad.of(value)));
        Assertions.assertFalse(a.equals(Angle.Rad.of(Math.nextUp(value))));
        Assertions.assertFalse(a.equals(new Object()));
        Assertions.assertFalse(a.equals(null));
    }

    @Test
    void testZero() {
        Assertions.assertEquals(0, Angle.Rad.ZERO.getAsDouble());
    }
    @Test
    void testPi() {
        Assertions.assertEquals(Math.PI, Angle.Rad.PI.getAsDouble());
    }
}
