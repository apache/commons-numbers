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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RegularizedGamma}.
 */
public class RegularizedGammaTest {
    @Test
    public void testRegularizedGammaNanPositive() {
        testRegularizedGamma(Double.NaN, Double.NaN, 1.0);
    }

    @Test
    public void testRegularizedGammaPositiveNan() {
        testRegularizedGamma(Double.NaN, 1.0, Double.NaN);
    }

    @Test
    public void testRegularizedGammaNegativePositive() {
        testRegularizedGamma(Double.NaN, -1.5, 1.0);
    }

    @Test
    public void testRegularizedGammaPositiveNegative() {
        testRegularizedGamma(Double.NaN, 1.0, -1.0);
    }

    @Test
    public void testRegularizedGammaZeroPositive() {
        testRegularizedGamma(Double.NaN, 0.0, 1.0);
    }

    @Test
    public void testRegularizedGammaPositiveZero() {
        testRegularizedGamma(0.0, 1.0, 0.0);
    }

    @Test
    public void testRegularizedGammaPositivePositive() {
        testRegularizedGamma(0.632120558828558, 1.0, 1.0);
    }

    @Disabled
    @Test
    public void testRegularizedGammaPWithACloseToZero() {
        // Creates a case where the regularized gamma P series is evaluated and the
        // result is outside the expected bounds of [0, 1]. This should be clipped to 1.0.
        final double a = 1e-18;
        // x must force use of the series in regularized gamma P using x < a + 1
        final double x = 0.5;
        testRegularizedGamma(1.0, a, x);
    }

    @Test
    public void testRegularizedGammaPWithAVeryCloseToZero() {
        // Creates a case where the partial sum is infinite due to inclusion of 1 / a
        final double a = Double.MIN_VALUE;
        // x must force use of the series in regularized gamma P using x < a + 1
        final double x = 0.5;
        testRegularizedGamma(1.0, a, x);
    }

    private void testRegularizedGamma(double expected, double a, double x) {
        double actualP = RegularizedGamma.P.value(a, x);
        double actualQ = RegularizedGamma.Q.value(a, x);
        Assertions.assertEquals(expected, actualP, 1e-15);
        Assertions.assertEquals(actualP, 1 - actualQ, 1e-15);
    }

    @Test
    public void testRegularizedGammaMaxIterationsExceededThrows() {
        final double a = 1.0;
        final double x = 1.0;
        // OK without
        Assertions.assertEquals(0.632120558828558, RegularizedGamma.P.value(a, x), 1e-15);

        final int maxIterations = 3;
        Assertions.assertThrows(ArithmeticException.class, () ->
            RegularizedGamma.P.value(a, x, 1e-15, maxIterations));
    }
}
