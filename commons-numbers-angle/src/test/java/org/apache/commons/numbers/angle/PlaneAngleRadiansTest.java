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
 * Test cases for the {@link PlaneAngleRadians} class.
 */
class PlaneAngleRadiansTest {
    @Test
    @SuppressWarnings("squid:S3415")
    void testConstants() {
        final double eps = 0;

        Assertions.assertEquals(Math.PI, PlaneAngleRadians.PI, eps);
        Assertions.assertEquals(2 * Math.PI, PlaneAngleRadians.TWO_PI, eps);
        Assertions.assertEquals(Math.PI / 2, PlaneAngleRadians.PI_OVER_TWO, eps);
        Assertions.assertEquals(3 * Math.PI / 2, PlaneAngleRadians.THREE_PI_OVER_TWO, eps);
    }

    // Test constants using "sin" and "cos".
    @Test
    void testConstants2() {
        final double eps = Math.ulp(1d);

        Assertions.assertEquals(0d, Math.sin(PlaneAngleRadians.PI), eps);
        Assertions.assertEquals(-1d, Math.cos(PlaneAngleRadians.PI), eps);

        Assertions.assertEquals(0d, Math.sin(PlaneAngleRadians.TWO_PI), 2 * eps);
        Assertions.assertEquals(1d, Math.cos(PlaneAngleRadians.TWO_PI), eps);

        Assertions.assertEquals(1d, Math.sin(PlaneAngleRadians.PI_OVER_TWO), eps);
        Assertions.assertEquals(0d, Math.cos(PlaneAngleRadians.PI_OVER_TWO), eps);

        Assertions.assertEquals(-1d, Math.sin(PlaneAngleRadians.THREE_PI_OVER_TWO), eps);
        Assertions.assertEquals(0d, Math.cos(PlaneAngleRadians.THREE_PI_OVER_TWO), eps);
    }

    @Test
    void testNormalize() {
        for (double a = -15.0; a <= 15.0; a += 0.1) {
            for (double b = -15.0; b <= 15.0; b += 0.2) {
                final double c = PlaneAngleRadians.normalizer(b).applyAsDouble(a);
                Assertions.assertTrue((b - PlaneAngleRadians.PI) <= c);
                Assertions.assertTrue(c <= (b + PlaneAngleRadians.PI));
                double twoK = Math.rint((a - c) / PlaneAngleRadians.PI);
                Assertions.assertEquals(c, a - twoK * PlaneAngleRadians.PI, 1e-14);
            }
        }
    }

    @Test
    void testNormalizeBetweenMinusPiAndPi1() {
        final double value = 1.25 * PlaneAngleRadians.TWO_PI;
        final double expected = PlaneAngleRadians.PI_OVER_TWO;
        final double actual = PlaneAngleRadians.WITHIN_MINUS_PI_AND_PI.applyAsDouble(value);
        final double tol = Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol);
    }
    @Test
    void testNormalizeBetweenMinusPiAndPi2() {
        final double value = 0.75 * PlaneAngleRadians.TWO_PI;
        final double expected = -PlaneAngleRadians.PI_OVER_TWO;
        final double actual = PlaneAngleRadians.WITHIN_MINUS_PI_AND_PI.applyAsDouble(value);
        final double tol = Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol);
    }
    @Test
    void testNormalizeBetweenMinusPiAndPi3() {
        final double value = PlaneAngleRadians.PI + 1e-10;
        final double expected = -PlaneAngleRadians.PI + 1e-10;
        final double actual = PlaneAngleRadians.WITHIN_MINUS_PI_AND_PI.applyAsDouble(value);
        final double tol = Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol);
    }
    @Test
    void testNormalizeBetweenMinusPiAndPi4() {
        final double value = 5 * PlaneAngleRadians.PI / 4;
        final double expected = PlaneAngleRadians.PI * (1d / 4 - 1);
        final double actual = PlaneAngleRadians.WITHIN_MINUS_PI_AND_PI.applyAsDouble(value);
        final double tol = Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol);
    }

    @Test
    void testNormalizeBetweenMinusPiAndPi_lowerBound() {
        final double value = PlaneAngleRadians.PI;
        final double expected = -PlaneAngleRadians.PI;
        final double actual = PlaneAngleRadians.WITHIN_MINUS_PI_AND_PI.applyAsDouble(value);
        final double tol = Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol);
    }
    @Test
    void testNormalizeBetweenMinusPiAndPi_upperBound() {
        final double value = PlaneAngleRadians.PI;
        final double expected = -PlaneAngleRadians.PI;
        final double actual = PlaneAngleRadians.WITHIN_MINUS_PI_AND_PI.applyAsDouble(value);
        final double tol = Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol);
    }

    @Test
    void testNormalizeBetweenZeroAndTwoPi1() {
        final double value = 1.25 * PlaneAngleRadians.TWO_PI;
        final double expected = PlaneAngleRadians.PI_OVER_TWO;
        final double actual = PlaneAngleRadians.WITHIN_0_AND_2PI.applyAsDouble(value);
        final double tol = Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol);
    }
    @Test
    void testNormalizeBetweenZeroAndTwoPi2() {
        final double value = 1.75 * PlaneAngleRadians.TWO_PI;
        final double expected = PlaneAngleRadians.THREE_PI_OVER_TWO;
        final double actual = PlaneAngleRadians.WITHIN_0_AND_2PI.applyAsDouble(value);
        final double tol = Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol);
    }
    @Test
    void testNormalizeBetweenZeroAndTwoPi3() {
        final double value = -PlaneAngleRadians.PI + 1e-10;
        final double expected = PlaneAngleRadians.PI + 1e-10;
        final double actual = PlaneAngleRadians.WITHIN_0_AND_2PI.applyAsDouble(value);
        final double tol = Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol);
    }
    @Test
    void testNormalizeBetweenZeroAndTwoPi4() {
        final double value = 9 * PlaneAngleRadians.PI / 4;
        final double expected = PlaneAngleRadians.PI / 4;
        final double actual = PlaneAngleRadians.WITHIN_0_AND_2PI.applyAsDouble(value);
        final double tol = Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol);
    }

    @Test
    void testNormalizeBetweenZeroAndTwoPi_lowerBound() {
        final double value = 0.0;
        final double expected = 0.0;
        final double actual = PlaneAngleRadians.WITHIN_0_AND_2PI.applyAsDouble(value);
        final double tol = Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol);
    }
    @Test
    void testNormalizeBetweenZeroAndTwoPi_upperBound() {
        final double value = PlaneAngleRadians.TWO_PI;
        final double expected = 0.0;
        final double actual = PlaneAngleRadians.WITHIN_0_AND_2PI.applyAsDouble(value);
        final double tol = Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol);
    }
}
