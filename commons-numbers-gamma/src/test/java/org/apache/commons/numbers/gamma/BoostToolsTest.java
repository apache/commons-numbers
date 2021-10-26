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

import java.math.BigDecimal;
import java.util.function.DoubleSupplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link BoostTools}.
 */
class BoostToolsTest {

    @ParameterizedTest
    @ValueSource(doubles = {0.5, 0.2})
    void testSumSeries(double x) {
        final double expected = Math.log1p(x);

        final int maxTerms = 1000;
        for (final double eps : new double[] {1e-6, 1e-10, Math.ulp(1.0)}) {
            final DoubleSupplier fun = new LogApXSeries(1, x);
            final double actual = BoostTools.sumSeries(fun, eps, maxTerms);
            Assertions.assertEquals(expected, actual, expected * eps, () -> "eps: " + eps);
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.5, 0.2})
    void testSumSeriesWithInitialValue(double x) {
        final double a = 2.5;
        final double expected = Math.log(a + x);

        final int maxTerms = 1000;
        for (final double eps : new double[] {1e-6, 1e-10, Math.ulp(1.0)}) {
            final DoubleSupplier fun = new LogApXSeries(a, x);
            final double actual = BoostTools.sumSeries(fun, eps, maxTerms, Math.log(a));
            Assertions.assertEquals(expected, actual, expected * eps, () -> "eps: " + eps);
        }
    }

    @Test
    void testSumSeriesThrows() {
        final double eps = Math.ulp(1.0);
        final double x = 0.01;

        // This works when enough terms are used
        final double expected = Math.log1p(x);
        Assertions.assertEquals(expected,
            BoostTools.sumSeries(new LogApXSeries(1, x), eps, 50, 0), expected * eps);

        // 3 terms are not enough to converge
        final DoubleSupplier fun = new LogApXSeries(1, x);
        Assertions.assertThrows(ArithmeticException.class, () -> BoostTools.sumSeries(fun, eps, 3));
    }

    /**
     * Test sum series with an invalid epsilon. For convergence to work the relative error
     * epsilon should be above zero. Invalid values are set to the minimum epsilon
     * and the result computed without an exception.
     */
    @ParameterizedTest
    @ValueSource(doubles = {0, Double.NaN, -0.123})
    void testSumSeriesWithEps(double eps) {
        final double x = 0.01;

        final double expected = Math.log1p(x);
        Assertions.assertEquals(expected,
            BoostTools.sumSeries(new LogApXSeries(1, x), eps, 50), Math.ulp(expected));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.5, 0.2})
    void testKahanSumSeries(double x) {
        final double expected = Math.log1p(x);

        final int maxTerms = 1000;
        for (final double eps : new double[] {1e-6, 1e-10, Math.ulp(1.0)}) {
            final DoubleSupplier fun = new LogApXSeries(1, x);
            final double actual = BoostTools.kahanSumSeries(fun, eps, maxTerms);
            Assertions.assertEquals(expected, actual, expected * eps, () -> "eps: " + eps);
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.5, 0.2})
    void testKahanSumSeriesWithInitialValue(double x) {
        final double a = 2.5;
        final double expected = Math.log(a + x);

        final int maxTerms = 1000;
        for (final double eps : new double[] {1e-6, 1e-10, Math.ulp(1.0)}) {
            final DoubleSupplier fun = new LogApXSeries(a, x);
            final double actual = BoostTools.kahanSumSeries(fun, eps, maxTerms, Math.log(a));
            Assertions.assertEquals(expected, actual, expected * eps, () -> "eps: " + eps);
        }
    }

    @Test
    void testKahanSumSeriesThrows() {
        final double eps = Math.ulp(1.0);
        final double x = 0.01;

        // This works when enough terms are used
        final double expected = Math.log1p(x);
        Assertions.assertEquals(expected,
            BoostTools.sumSeries(new LogApXSeries(1, x), eps, 50, 0), expected * eps);

        // 3 terms are not enough to converge
        final DoubleSupplier fun = new LogApXSeries(1, x);
        Assertions.assertThrows(ArithmeticException.class, () -> BoostTools.kahanSumSeries(fun, eps, 3));
    }

    /**
     * Test sum series with an invalid epsilon. For convergence to work the relative error
     * epsilon should be above zero (unless a generated term is zero).
     * Invalid values are set to the minimum epsilon
     * and the result computed without an exception.
     */
    @ParameterizedTest
    @ValueSource(doubles = {0, Double.NaN, -0.123})
    void testKahanSumSeriesWithEps(double eps) {
        final double x = 0.01;

        final double expected = Math.log1p(x);
        Assertions.assertEquals(expected,
            BoostTools.kahanSumSeries(new LogApXSeries(1, x), eps, 50), Math.ulp(expected));
    }

    /**
     * Test the Kahan sum is robust to cancellation using a series with terms
     * that alternate in sign. Uses the series for log(1+x) - x.
     */
    @ParameterizedTest
    @CsvSource({
        // Results computed using maxima to 64 digits.
        // x is machine representable as a double.
        "0, 0, 0, 0",
        "9.765625E-4,      -4.765269445411040391750919828133273881656662154637622414017262915e-7, 1.3, 0",
        "-0.5,             -1.931471805599453094172321214581765680755001343602552541206800095e-1, 3,   0",
        "-0.84130859375,   -9.994852100796609183345472280222483649740308246655652350831388108e-1, 14,  0",
        "-0.8414306640625, -1.000132666546189484308487768061225710806999885159575693903185135,    5.5, 0.51",
    })
    void testKahanSumSeriesLog1pmx(double x, BigDecimal expected, double maxUlps1, double maxUlps2) {
        final double eps = Math.ulp(1.0);
        final int maxTerms = Integer.MAX_VALUE;
        final double s1 = BoostTools.sumSeries(new Log1pmxSeries(x), eps, maxTerms);
        final double s2 = BoostTools.kahanSumSeries(new Log1pmxSeries(x), eps, maxTerms);

        // Standard sum is close
        final double e1 = TestUtils.assertEquals(expected, s1, maxUlps1, "sum series");
        // Kaham sum is close
        final double e2 = TestUtils.assertEquals(expected, s2, maxUlps1, "Kaham sum series");
        // Kaham sum is closer even with the same epsilon
        Assertions.assertTrue(e1 == 0 || Math.abs(e2) < Math.abs(e1), () -> e2 + " < " + e1);

        // Use 0 eps to use the minimum allowed by the function.
        final double s3 = BoostTools.kahanSumSeries(new Log1pmxSeries(x), 0.0, maxTerms);
        final double e3 = TestUtils.assertEquals(expected, s3, maxUlps2, "Kaham sum series with extra guard digits");
        Assertions.assertTrue(e2 == 0 || Math.abs(e3) < Math.abs(e2), () -> e3 + " < " + e2);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -1, 2, -3, 4})
    void testEvaluatePolynomial(double x) {
        final double[] c1 = {7, -5, 2, 4};
        final double expected1 = c1[3] * x * x * x + c1[2] * x * x + c1[1] * x + c1[0];
        Assertions.assertEquals(expected1, BoostTools.evaluatePolynomial(c1, x), Math.ulp(expected1));

        final double[] c2 = {-9, 3, -6};
        final double expected2 = c2[2] * x * x + c2[1] * x + c2[0];
        Assertions.assertEquals(expected2, BoostTools.evaluatePolynomial(c2, x), Math.ulp(expected2));

        final double[] c3 = {-13, 2};
        final double expected3 = c3[1] * x + c3[0];
        Assertions.assertEquals(expected3, BoostTools.evaluatePolynomial(c3, x), Math.ulp(expected3));

        final double[] c4 = {-0.12345};
        final double expected4 = c4[0];
        Assertions.assertEquals(expected4, BoostTools.evaluatePolynomial(c4, x));

        // Zero length coefficients not supported
        final double[] c5 = {};
        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> BoostTools.evaluatePolynomial(c5, x));
    }

    /**
     * Series to compute log(a+x) using:
     * <pre>
     * log(a+x) = log(a) + 2 [z + z^3/3 + z^5/5 + ...] ; z = x / (2a + x)
     * </pre>
     * <p>The term log(a) must be added to the generated series.
     */
    private static class LogApXSeries implements DoubleSupplier {
        private double next;
        private final double z2;
        private int n = 1;

        /**
         * @param a Argument a
         * @param x Argument x
         */
        LogApXSeries(double a, double x) {
            final double z = x / (2 * a + x);
            next = z;
            z2 = z * z;
        }

        @Override
        public double getAsDouble() {
            final double r = next / n;
            next *= z2;
            n += 2;
            return 2 * r;
        }
    }

    /**
     * Series to compute log(1+x) - x using:
     * <pre>
     * log(1+x) = -x^2/2 + x^3/3 - x^4/4 + ...
     * </pre>
     * <p>This series is hard to compute as the result {@code -> -1}
     * (x approximately -0.8414 and 2.146) due to limited precision in the summation.
     */
    private static class Log1pmxSeries implements DoubleSupplier {
        private double next;
        private final double x;
        private int n = 1;

        /**
         * @param x Argument x
         */
        Log1pmxSeries(double x) {
            this.x = x;
            next = x;
        }

        @Override
        public double getAsDouble() {
            next *= -x;
            n++;
            return next / n;
        }
    }
}
