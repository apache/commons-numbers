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

import org.apache.commons.numbers.gamma.RegularizedGamma.ArgumentA;
import org.apache.commons.numbers.gamma.RegularizedGamma.ArgumentX;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link RegularizedGamma}.
 */
class RegularizedGammaTest {
    /**
     * Test argument X cannot be NaN, negative or zero.
     *
     * @param a Argument a
     */
    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, 0, -1})
    void testInvalidArgumentA(double a) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ArgumentA.of(a));
        // No exception thrown. The result is NaN.
        testRegularizedGamma(Double.NaN, Double.NaN, a, 1.0);
    }

    /**
     * Test argument X cannot be NaN or negative.
     *
     * @param x Argument x
     */
    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, -1})
    void testInvalidArgumentX(double x) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ArgumentX.of(x));
        // No exception thrown. The result is NaN.
        testRegularizedGamma(Double.NaN, Double.NaN, 1.0, x);
    }

    @Test
    void testRegularizedGammaPWithACloseToZero() {
        // Creates a case where the regularized gamma P series is evaluated and the
        // result is outside the expected bounds of [0, 1]. This should be clipped to 1.0.
        final double a = 1e-18;
        // x must force use of the series in regularized gamma P using x < a + 1
        final double x = 0.5;
        testRegularizedGamma(1.0, 0.0, a, x);
    }

    @Test
    void testRegularizedGammaPWithAVeryCloseToZero() {
        // Creates a case where the partial sum is infinite due to inclusion of 1 / a
        final double a = Double.MIN_VALUE;
        // x must force use of the series in regularized gamma P using x < a + 1
        final double x = 0.5;
        testRegularizedGamma(1.0, 0.0, a, x);
    }

    /**
     * Test the regularized gamma P and Q functions.
     *
     * <p>Note that the identity P + Q = 1 is tested. It should not be used to generate
     * data for P from Q (or vice versa) when the values approach 0 or 1 due to floating
     * point error.
     *
     * <p>Tests the methods with double arguments and then the methods with pre-computed
     * arguments which must be an exact match.
     *
     * @param expectedP Expected P(a, x)
     * @param expectedQ Expected Q(a, x)
     * @param a Argument a
     * @param x Argument x
     */
    @ParameterizedTest
    @CsvSource(value = {
        "0.0, 1.0, 1.0, 0.0",
        // Values computed using Wolfram Mathematica
        "0.63212055882855767840, 0.36787944117144232160, 1.0, 1.0",
        "0.080301397071394196011, 0.91969860292860580399, 3, 1",
        "0.877050191685244, 0.1229498083147559, 0.52, 1.23",
        "0.01101451006216559, 0.988985489937834, 46.34, 32.18",
        "1.0, 1.0922956375456871032e-43, 10, 130",
        "7.6002090267819442301e-95, 1.0, 130, 10",
    })
    void testRegularizedGamma(double expectedP, double expectedQ, double a, double x) {
        double actualP = RegularizedGamma.P.value(a, x);
        double actualQ = RegularizedGamma.Q.value(a, x);
        Assertions.assertEquals(expectedP, actualP, 1e-15, "p");
        Assertions.assertEquals(expectedQ, actualQ, 1e-15, "q");

        // Note: If the expected values are NaN then assume this is due to invalid parameters.
        if (Double.isNaN(expectedP)) {
            // Try to construct the arguments.
            // If one is valid then the function should compute the same result (NaN)
            try {
                final ArgumentA argA = ArgumentA.of(a);
                Assertions.assertEquals(actualP, RegularizedGamma.P.value(argA, x));
                Assertions.assertEquals(actualQ, RegularizedGamma.Q.value(argA, x));
            } catch (IllegalArgumentException ex) {
                // argument a is invalid
            }
            try {
                final ArgumentX argX = ArgumentX.of(x);
                Assertions.assertEquals(actualP, RegularizedGamma.P.value(a, argX));
                Assertions.assertEquals(actualQ, RegularizedGamma.Q.value(a, argX));
            } catch (IllegalArgumentException ex) {
                // argument x is invalid
            }
            return;
        }

        // Test the identity P + Q = 1
        Assertions.assertEquals(1.0, actualP + actualQ, 1e-15, "p+q");

        // Verify the versions with pre-computed arguments.
        // The results must be binary equal so do not use a tolerance.

        final ArgumentA argA = ArgumentA.of(a);
        Assertions.assertEquals(actualP, RegularizedGamma.P.value(argA, x));
        Assertions.assertEquals(actualQ, RegularizedGamma.Q.value(argA, x));

        final ArgumentX argX = ArgumentX.of(x);
        Assertions.assertEquals(actualP, RegularizedGamma.P.value(a, argX));
        Assertions.assertEquals(actualQ, RegularizedGamma.Q.value(a, argX));
    }

    @Test
    void testRegularizedGammaPMaxIterationsExceededThrows() {
        // x < a + 1
        final double a = 13.0;
        final double x = 10.0;
        // OK without
        final double actual = RegularizedGamma.P.value(a, x);
        // mathematica: N[GammaRegularized[13, 0, 10], 20]
        Assertions.assertEquals(0.20844352360512566106, actual, 1e-15);
        final ArgumentA argA = ArgumentA.of(a);
        final ArgumentX argX = ArgumentX.of(x);
        Assertions.assertEquals(actual, RegularizedGamma.P.value(argA, x));
        Assertions.assertEquals(actual, RegularizedGamma.P.value(a, argX));

        final int maxIterations = 3;
        Assertions.assertThrows(ArithmeticException.class, () ->
            RegularizedGamma.P.value(a, x, 1e-15, maxIterations));
        Assertions.assertThrows(ArithmeticException.class, () ->
            RegularizedGamma.P.value(argA, x, 1e-15, maxIterations));
        Assertions.assertThrows(ArithmeticException.class, () ->
            RegularizedGamma.P.value(a, argX, 1e-15, maxIterations));
    }

    @Test
    void testRegularizedGammaQMaxIterationsExceededThrows() {
        // x >= a + 1
        final double a = 10.0;
        final double x = 13.0;
        // OK without
        final double actual = RegularizedGamma.Q.value(a, x);
        // mathematica: N[GammaRegularized[10, 13], 20]
        Assertions.assertEquals(0.16581187661729210469, actual, 1e-15);
        final ArgumentA argA = ArgumentA.of(a);
        final ArgumentX argX = ArgumentX.of(x);
        Assertions.assertEquals(actual, RegularizedGamma.Q.value(argA, x));
        Assertions.assertEquals(actual, RegularizedGamma.Q.value(a, argX));

        final int maxIterations = 3;
        Assertions.assertThrows(ArithmeticException.class, () ->
            RegularizedGamma.Q.value(a, x, 1e-15, maxIterations));
        Assertions.assertThrows(ArithmeticException.class, () ->
            RegularizedGamma.Q.value(argA, x, 1e-15, maxIterations));
        Assertions.assertThrows(ArithmeticException.class, () ->
            RegularizedGamma.Q.value(a, argX, 1e-15, maxIterations));
    }
}
