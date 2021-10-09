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

import java.util.function.DoubleUnaryOperator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link RegularizedGamma}.
 */
class RegularizedGammaTest {
    private static final double EPSILON = 1e-15;
    private static final int MAX_ITER = Integer.MAX_VALUE;

    /**
     * Test argument X cannot be NaN, negative or zero.
     *
     * @param a Argument a
     */
    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, 0, -1})
    void testInvalidArgumentA(double a) {
        // Test variations of constructing the unary operator
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> RegularizedGamma.P.withArgumentA(a));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> RegularizedGamma.P.withArgumentA(a, EPSILON, MAX_ITER));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> RegularizedGamma.Q.withArgumentA(a));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> RegularizedGamma.Q.withArgumentA(a, EPSILON, MAX_ITER));
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
        // Test variations of constructing the unary operator
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> RegularizedGamma.P.withArgumentA(x));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> RegularizedGamma.P.withArgumentA(x, EPSILON, MAX_ITER));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> RegularizedGamma.Q.withArgumentA(x));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> RegularizedGamma.Q.withArgumentA(x, EPSILON, MAX_ITER));
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
                final DoubleUnaryOperator p = RegularizedGamma.P.withArgumentA(a);
                final DoubleUnaryOperator q = RegularizedGamma.Q.withArgumentA(a);
                Assertions.assertEquals(Double.NaN, p.applyAsDouble(x));
                Assertions.assertEquals(Double.NaN, q.applyAsDouble(x));
            } catch (IllegalArgumentException ex) {
                // argument a is invalid
            }
            try {
                final DoubleUnaryOperator p = RegularizedGamma.P.withArgumentX(x);
                final DoubleUnaryOperator q = RegularizedGamma.Q.withArgumentX(x);
                Assertions.assertEquals(Double.NaN, p.applyAsDouble(a));
                Assertions.assertEquals(Double.NaN, q.applyAsDouble(a));
            } catch (IllegalArgumentException ex) {
                // argument x is invalid
            }
            return;
        }

        // Test the identity P + Q = 1
        Assertions.assertEquals(1.0, actualP + actualQ, 1e-15, "p+q");

        // Verify the versions with pre-computed arguments.
        // The results must be binary equal so do not use a tolerance.

        final DoubleUnaryOperator pa = RegularizedGamma.P.withArgumentA(a);
        final DoubleUnaryOperator qa = RegularizedGamma.Q.withArgumentA(a);
        Assertions.assertEquals(actualP, pa.applyAsDouble(x));
        Assertions.assertEquals(actualQ, qa.applyAsDouble(x));

        final DoubleUnaryOperator px = RegularizedGamma.P.withArgumentX(x);
        final DoubleUnaryOperator qx = RegularizedGamma.Q.withArgumentX(x);
        Assertions.assertEquals(actualP, px.applyAsDouble(a));
        Assertions.assertEquals(actualQ, qx.applyAsDouble(a));
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

        final DoubleUnaryOperator pa = RegularizedGamma.P.withArgumentA(a);
        final DoubleUnaryOperator px = RegularizedGamma.P.withArgumentX(x);

        Assertions.assertEquals(actual, pa.applyAsDouble(x));
        Assertions.assertEquals(actual, px.applyAsDouble(a));

        final int maxIterations = 3;
        final DoubleUnaryOperator pa1 = RegularizedGamma.P.withArgumentA(a, EPSILON, maxIterations);
        final DoubleUnaryOperator px1 = RegularizedGamma.P.withArgumentX(x, EPSILON, maxIterations);
        Assertions.assertThrows(ArithmeticException.class, () ->
            RegularizedGamma.P.value(a, x, 1e-15, maxIterations));
        Assertions.assertThrows(ArithmeticException.class, () ->
            pa1.applyAsDouble(x));
        Assertions.assertThrows(ArithmeticException.class, () ->
            px1.applyAsDouble(a));
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

        final DoubleUnaryOperator qa = RegularizedGamma.Q.withArgumentA(a);
        final DoubleUnaryOperator qx = RegularizedGamma.Q.withArgumentX(x);

        Assertions.assertEquals(actual, qa.applyAsDouble(x));
        Assertions.assertEquals(actual, qx.applyAsDouble(a));

        final int maxIterations = 3;
        final DoubleUnaryOperator qa1 = RegularizedGamma.Q.withArgumentA(a, EPSILON, maxIterations);
        final DoubleUnaryOperator qx1 = RegularizedGamma.Q.withArgumentX(x, EPSILON, maxIterations);
        Assertions.assertThrows(ArithmeticException.class, () ->
            RegularizedGamma.Q.value(a, x, 1e-15, maxIterations));
        Assertions.assertThrows(ArithmeticException.class, () ->
            qa1.applyAsDouble(x));
        Assertions.assertThrows(ArithmeticException.class, () ->
            qx1.applyAsDouble(a));
    }
}
