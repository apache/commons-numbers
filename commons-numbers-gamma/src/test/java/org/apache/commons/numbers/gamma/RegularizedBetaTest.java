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
package org.apache.commons.numbers.gamma;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link RegularizedBeta}.
 */
class RegularizedBetaTest {

    @ParameterizedTest
    @CsvSource({
        "0.0, 0, 1, 2",
        "1.0, 1, 1, 2",
        "0.75, 0.5, 1, 2",
        // Invalid variants
        "NaN, NaN, 1, 2",
        "NaN, 0.5, NaN, 2",
        "NaN, 0.5, 1, NaN",
        "NaN, -0.5, 1, 2",
        "NaN, 0.5, -1, 2",
        "NaN, 0.5, 1, -2",
        "NaN, 0.5, 0, 2",
        "NaN, 0.5, 1, 0",
        "NaN, 1.5, 1, 2",
    })
    void testRegularizedBeta(double expected,
                             double x,
                             double a,
                             double b) {
        final double actual = RegularizedBeta.value(x, a, b);
        Assertions.assertEquals(expected, actual, 1e-15);
    }

    @Test
    void testRegularizedBetaTinyArgument() {
        // Ideally: x < (a + 1) / (2 + b + a)
        double actual = RegularizedBeta.value(1e-17, 2.0, 1e12);
        // This value is from R: pbeta(1e-17,2,1e12)
        double expected = 4.9999666667966403e-11;
        Assertions.assertEquals(expected, actual, expected * 1e-10);
    }

    @Test
    void testRegularizedBetaTinyArgument2() {
        // Ideally: x > (a + 1) / (2 + b + a)
        double actual = RegularizedBeta.value(1e-11, 2.0, 1e12);
        // This value is from R: pbeta(1e-12,2,1e12)
        double expected = 0.99950060077263769;
        Assertions.assertEquals(expected, actual, expected * 1e-8);
    }

    @Test
    void testMath1067() {
        final double x = 0.22580645161290325;
        final double a = 64.33333333333334;
        final double b = 223;

        try {
            RegularizedBeta.value(x, a, b, 1e-14, 10000);
        } catch (StackOverflowError error) {
            Assertions.fail("Infinite recursion");
        }
    }

    @Test
    void testZeroAndOne() {
        // NUMBERS: 170
        Assertions.assertEquals(1.0, RegularizedBeta.value(1.0, 1e17, 0.5));
        Assertions.assertEquals(0.0, RegularizedBeta.value(0.0, 1e17, 0.5));

        // a and b do not matter
        final double[] v = {0.1, 0.5, 1, 2, 10};
        for (final double a : v) {
            for (final double b : v) {
                Assertions.assertEquals(0.0, RegularizedBeta.value(0.0, b, a));
                Assertions.assertEquals(1.0, RegularizedBeta.value(1.0, b, a));
            }
        }
    }

    @Test
    void testBeta1() {
        final double tol = 1e-14;
        final double[] xs = {0.0, Double.MIN_NORMAL, 1e-132, 0.1, 0.5, 0.99, 0.99999, Math.nextDown(1.0), 1};
        final double[] as = {0.1, 0.5, 1, 2, 10};
        for (final double x : xs) {
            for (final double a : as) {
                final double expected = Math.pow(x, a);
                Assertions.assertEquals(expected, RegularizedBeta.value(x, a, 1), expected * tol,
                    () -> String.format("x=%s, a=%s", x, a));
            }
        }
    }

    @Test
    void testAlpha1() {
        final double tol = 1e-14;
        final double[] xs = {0.0, Double.MIN_NORMAL, 1e-132, 0.1, 0.5, 0.99, 0.99999, Math.nextDown(1.0), 1};
        final double[] bs = {0.1, 0.5, 1, 2, 10};
        for (final double x : xs) {
            for (final double b : bs) {
                // 1 - (1-x)^b
                // When x > 0.5 then 1-x is exact and we use the power function.
                // Otherwise use log functions:
                // 1 - exp(b * log(1-x))
                final double expected = x >= 0.5 ?
                        1.0 - Math.pow(1 - x, b) :
                        -Math.expm1(b * Math.log1p(-x));
                Assertions.assertEquals(expected, RegularizedBeta.value(x, 1, b), expected * tol,
                    () -> String.format("x=%s, b=%s", x, b));
            }
        }
    }
}
