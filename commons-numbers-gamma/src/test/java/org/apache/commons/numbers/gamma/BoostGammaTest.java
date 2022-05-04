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

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import org.apache.commons.numbers.gamma.BoostGamma.Lanczos;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link BoostGamma}. Special functions from {@link BoostMath} and {@link SpecialMath}
 * are also tested as these are used within the {@link BoostGamma} class.
 *
 * <p>Note: Some resource data files used in these tests have been extracted
 * from the Boost test files for the gamma functions.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BoostGammaTest {
    /** All representable factorials. */
    private static final double[] FACTORIAL = BoostGamma.getFactorials();
    /** The threshold value for choosing the Lanczos approximation. */
    private static final double LANCZOS_THRESHOLD = 20;
    /** Value for the sqrt of the epsilon for relative error.
     * This is equal to the Boost constant {@code boost::math::tools::root_epsilon<double>()}. */
    private static final double ROOT_EPSILON = 1.4901161193847656E-8;
    /** Approximate value for ln(Double.MAX_VALUE).
     * This is equal to the Boost constant {@code boost::math::tools::log_max_value<double>()}. */
    private static final int LOG_MAX_VALUE = 709;
    /** Approximate value for ln(Double.MIN_VALUE).
     * This is equal to the Boost constant {@code boost::math::tools::log_min_value<double>()}.
     * No term {@code x} should be used in {@code exp(x)} if {@code x < LOG_MIN_VALUE} to avoid
     * underflow to sub-normal or zero. */
    private static final int LOG_MIN_VALUE = -708;
    /** The largest factorial that can be represented as a double.
     * This is equal to the Boost constant {@code boost::math::max_factorial<double>::value}. */
    private static final int MAX_FACTORIAL = 170;
    /** Euler's constant. */
    private static final double EULER = 0.5772156649015328606065120900824024310;

    /** The Boost 1_77_0 condition to use the asymptotic approximation. */
    private static final DoubleDoubleBiPredicate USE_ASYM_APPROX =
        (a, x) -> (x > 1000) && ((a < x) || (Math.abs(a - 50) / x < 1));
    /** Predicate to not use the asymptotic approximation. */
    private static final DoubleDoubleBiPredicate NO_ASYM_APPROX = (a, x) -> false;
    /** Predicate to use the asymptotic approximation. */
    private static final DoubleDoubleBiPredicate ASYM_APPROX = (a, x) -> true;

    /** A predicate for two {@code double} arguments. */
    private interface DoubleDoubleBiPredicate {
        /**
         * @param a Argument a
         * @param b Argument b
         * @return true if successful
         */
        boolean test(double a, double b);
    }

    /** Define the expected error for a test. */
    private interface TestError {
        /**
         * @return maximum allowed error
         */
        double getTolerance();

        /**
         * @return maximum allowed RMS error
         */
        double getRmsTolerance();

        /**
         * @param name name of the test error (used to provide a name for {@link #toString()})
         * @param tolerance maximum allowed error
         * @param rmsTolerance maximum allowed RMS error
         * @return the test error
         */
        static TestError of(String name, double tolerance, double rmsTolerance) {
            return new TestError() {
                @Override
                public String toString() {
                    return name;
                }

                @Override
                public double getTolerance() {
                    return tolerance;
                }

                @Override
                public double getRmsTolerance() {
                    return rmsTolerance;
                }
            };
        }
    }

    /**
     * Define the test cases for each resource file.
     * This encapsulates the function to test, the expected maximum and RMS error, and
     * the resource file containing the data.
     *
     * <h2>Note on accuracy</h2>
     *
     * <p>The Boost functions use the default policy of internal promotion
     * of double to long double if it offers more precision. Code comments
     * in the implementations for the maximum error are using the defaults with
     * promotion enabled where the error is 'effectively zero'. Java does not
     * support long double computation. Tolerances have been set to allow tests to
     * pass. Spot checks on larger errors have been verified against the reference
     * implementation compiled with promotion of double <strong>disabled</strong>.
     *
     * @see <a href="https://www.boost.org/doc/libs/1_77_0/libs/math/doc/html/math_toolkit/relative_error.html">Relative error</a>
     * @see <a href="https://www.boost.org/doc/libs/1_77_0/libs/math/doc/html/math_toolkit/pol_tutorial/policy_tut_defaults.html">Policy defaults</a>
     */
    private enum TestCase implements TestError {
        // Note:
        // The original Boost tgamma function is not as accurate as the
        // NSWC Library of Mathematics Subroutines in the range [-20, 20].
        // The default implementation uses long double to compute and
        // performs a narrowing cast to the double result. Without this
        // feature the method accuracy is reduced.
        // The code is here for testing.

        /** gamma Boost near 0 data. */
        TGAMMAO_NEAR_0(BoostGammaTest::tgammaOriginal, "gamma_near_0_data.csv", 3.3, 1.2),
        /** gamma Boost near 1 data. */
        TGAMMAO_NEAR_1(BoostGammaTest::tgammaOriginal, "gamma_near_1_data.csv", 3.3, 1.2),
        /** gamma Boost near 2 data. */
        TGAMMAO_NEAR_2(BoostGammaTest::tgammaOriginal, "gamma_near_2_data.csv", 2.9, 1.2),
        /** gamma Boost near -10 data. */
        TGAMMAO_NEAR_M10(BoostGammaTest::tgammaOriginal, "gamma_near_m10_data.csv", 2.5, 1.2),
        /** gamma -20 to 0 data. */
        TGAMMAO_M20_0(BoostGammaTest::tgammaOriginal, "gamma_m20_0_data.csv", 4.5, 1.4),
        /** gamma 0 to 20 data. */
        TGAMMAO_0_20(BoostGammaTest::tgammaOriginal, "gamma_0_20_data.csv", 3.2, 1.2),
        /** gamma very near 0 data. */
        TGAMMAO_VERY_NEAR_0(BoostGammaTest::tgammaOriginal, "gamma_very_near_0_data.csv", 3.3, 0.75),

        /** gamma Boost factorial data. */
        TGAMMA_FACTORIALS(BoostGamma::tgamma, "gamma_factorials_data.csv", 2.5, 0.8),
        /** gamma Boost near 0 data. */
        TGAMMA_NEAR_0(BoostGamma::tgamma, "gamma_near_0_data.csv", 1.6, 0.7),
        /** gamma Boost near 1 data. */
        TGAMMA_NEAR_1(BoostGamma::tgamma, "gamma_near_1_data.csv", 1.3, 0.7),
        /** gamma Boost near 2 data. */
        TGAMMA_NEAR_2(BoostGamma::tgamma, "gamma_near_2_data.csv", 1.1, 0.6),
        /** gamma Boost near -10 data. */
        TGAMMA_NEAR_M10(BoostGamma::tgamma, "gamma_near_m10_data.csv", 1.8, 0.7),
        /** gamma Boost near -55 data. */
        TGAMMA_NEAR_M55(BoostGamma::tgamma, "gamma_near_m55_data.csv", 2.5, 1.2),
        /** gamma -20 to 0 data. */
        TGAMMA_M20_0(BoostGamma::tgamma, "gamma_m20_0_data.csv", 3, 0.8),
        /** gamma 0 to 20 data. */
        TGAMMA_0_20(BoostGamma::tgamma, "gamma_0_20_data.csv", 2, 0.65),
        /** gamma 20 to 150 data. */
        TGAMMA_20_150(BoostGamma::tgamma, "gamma_20_150_data.csv", 3.8, 1.2),
        /** gamma 150 to 171 data. */
        TGAMMA_150_171(BoostGamma::tgamma, "gamma_150_171_data.csv", 3.2, 1.2),
        /** gamma very near 0 data. */
        TGAMMA_VERY_NEAR_0(BoostGamma::tgamma, "gamma_very_near_0_data.csv", 3.8, 0.7),

        /** gamma Boost factorial data. */
        LGAMMA_FACTORIALS(BoostGamma::lgamma, "gamma_factorials_data.csv", 2, 0.8, 0.1),
        /** gamma Boost near 0 data. */
        LGAMMA_NEAR_0(BoostGamma::lgamma, "gamma_near_0_data.csv", 2, 1.2, 0.5),
        /** gamma Boost near 1 data. */
        LGAMMA_NEAR_1(BoostGamma::lgamma, "gamma_near_1_data.csv", 2, 1.5, 0.7),
        /** gamma Boost near 2 data. */
        LGAMMA_NEAR_2(BoostGamma::lgamma, "gamma_near_2_data.csv", 2, 0.7, 0.2),
        /** gamma Boost near -10 data. */
        LGAMMA_NEAR_M10(BoostGamma::lgamma, "gamma_near_m10_data.csv", 2, 3, 0.99),
        /** gamma Boost near -55 data. */
        LGAMMA_NEAR_M55(BoostGamma::lgamma, "gamma_near_m55_data.csv", 2, 0.9, 0.45),
        /** gamma -20 to 0 data. */
        // The value -2.75 is low precision
        LGAMMA_M20_0(BoostGamma::lgamma, "gamma_m20_0_data.csv", 2, 100, 9),
        /** gamma 0 to 20 data. */
        LGAMMA_0_20(BoostGamma::lgamma, "gamma_0_20_data.csv", 2, 0.95, 0.25),
        /** gamma 20 to 150 data. */
        LGAMMA_20_150(BoostGamma::lgamma, "gamma_20_150_data.csv", 2, 1.5, 0.45),
        /** gamma 150 to 171 data. */
        LGAMMA_150_171(BoostGamma::lgamma, "gamma_150_171_data.csv", 2, 1.6, 0.65),
        /** gamma very near 0 data. */
        LGAMMA_VERY_NEAR_0(BoostGamma::lgamma, "gamma_very_near_0_data.csv", 2, 1.8, 0.4),

        /** gamma(1+x) - 1 Boost data. */
        TGAMMAP1M1(BoostGamma::tgamma1pm1, "gamma1pm1_data.csv", 1.8, 0.6),

        /** log(1+x) - 1  data. */
        LOG1PMX(SpecialMath::log1pmx, "log1pmx_data.csv", -0.9, 0.15);

        /** The function. */
        private final DoubleUnaryOperator fun;

        /** The filename containing the test data. */
        private final String filename;

        /** The field containing the expected value. */
        private final int expected;

        /** The maximum allowed ulp. */
        private final double maxUlp;

        /** The maximum allowed RMS ulp. */
        private final double rmsUlp;

        /**
         * Instantiates a new test case.
         *
         * @param fun function to test
         * @param filename Filename of the test data
         * @param maxUlp maximum allowed ulp
         * @param rmsUlp maximum allowed RMS ulp
         */
        TestCase(DoubleUnaryOperator fun, String filename, double maxUlp, double rmsUlp) {
            this(fun, filename, 1, maxUlp, rmsUlp);
        }

        /**
         * Instantiates a new test case.
         *
         * @param fun function to test
         * @param filename Filename of the test data
         * @param expected Expected result field index
         * @param maxUlp maximum allowed ulp
         * @param rmsUlp maximum allowed RMS ulp
         */
        TestCase(DoubleUnaryOperator fun, String filename, int expected, double maxUlp, double rmsUlp) {
            this.fun = fun;
            this.filename = filename;
            this.expected = expected;
            this.maxUlp = maxUlp;
            this.rmsUlp = rmsUlp;
        }

        /**
         * @return function to test
         */
        DoubleUnaryOperator getFunction() {
            return fun;
        }

        /**
         * @return Filename of the test data
         */
        String getFilename() {
            return filename;
        }

        /**
         * @return Expected result field index
         */
        int getExpectedField() {
            return expected;
        }

        @Override
        public double getTolerance() {
            return maxUlp;
        }

        @Override
        public double getRmsTolerance() {
            return rmsUlp;
        }
    }

    /**
     * Define the test cases for each resource file for two argument functions.
     * This encapsulates the function to test, the expected maximum and RMS error, and
     * the resource file containing the data.
     */
    private enum BiTestCase implements TestError {
        /** pow(x, y) - 1 Boost data. */
        POWM1(BoostMath::powm1, "powm1_data.csv", 2.3, 0.4),
        /** igamma Boost int data. */
        IGAMMA_UPPER_INT(BoostGamma::tgamma, "igamma_int_data.csv", 6, 1.5),
        /** igamma Boost small data. */
        IGAMMA_UPPER_SMALL(BoostGamma::tgamma, "igamma_small_data.csv", 2.5, 0.9),
        /** igamma Boost med data. */
        IGAMMA_UPPER_MED(BoostGamma::tgamma, "igamma_med_data.csv", 9, 1.85),
        /** igamma Boost big data. */
        IGAMMA_UPPER_BIG(BoostGamma::tgamma, "igamma_big_data.csv", 8, 1.3),
        /** igamma extra data containing edge cases. */
        IGAMMA_UPPER_EXTRA(BoostGamma::tgamma, "igamma_extra_data.csv", 13, 4),
        /** igamma Boost int data. */
        IGAMMA_Q_INT(BoostGamma::gammaQ, "igamma_int_data.csv", 3, 5, 1.3),
        /** igamma Boost small data. */
        IGAMMA_Q_SMALL(BoostGamma::gammaQ, "igamma_small_data.csv", 3, 4, 1.1),
        /** igamma Boost med data. */
        IGAMMA_Q_MED(BoostGamma::gammaQ, "igamma_med_data.csv", 3, 6, 0.95),
        /** igamma Boost big data. */
        IGAMMA_Q_BIG(BoostGamma::gammaQ, "igamma_big_data.csv", 3, 550, 62),
        /** igamma extra data containing edge cases. */
        IGAMMA_Q_EXTRA(BoostGamma::gammaQ, "igamma_extra_data.csv", 3, 60, 18),
        /** igamma Boost int data. */
        IGAMMA_LOWER_INT(BoostGamma::tgammaLower, "igamma_int_data.csv", 4, 6, 1.3),
        /** igamma Boost small data. */
        IGAMMA_LOWER_SMALL(BoostGamma::tgammaLower, "igamma_small_data.csv", 4, 1.6, 0.55),
        /** igamma Boost med data. */
        IGAMMA_LOWER_MED(BoostGamma::tgammaLower, "igamma_med_data.csv", 4, 6.5, 1.3),
        /** igamma Boost big data. */
        IGAMMA_LOWER_BIG(BoostGamma::tgammaLower, "igamma_big_data.csv", 4, 8, 1.3),
        /** igamma extra data containing edge cases. */
        IGAMMA_LOWER_EXTRA(BoostGamma::tgammaLower, "igamma_extra_data.csv", 4, 5, 1.4),
        /** igamma Boost int data. */
        IGAMMA_P_INT(BoostGamma::gammaP, "igamma_int_data.csv", 5, 3.5, 0.95),
        /** igamma Boost small data. */
        IGAMMA_P_SMALL(BoostGamma::gammaP, "igamma_small_data.csv", 5, 2.8, 0.9),
        /** igamma Boost med data. */
        IGAMMA_P_MED(BoostGamma::gammaP, "igamma_med_data.csv", 5, 5, 0.9),
        /** igamma Boost big data. */
        IGAMMA_P_BIG(BoostGamma::gammaP, "igamma_big_data.csv", 5, 430, 55),
        /** igamma extra data containing edge cases. */
        IGAMMA_P_EXTRA(BoostGamma::gammaP, "igamma_extra_data.csv", 5, 0.7, 0.2),
        /** gamma p derivative computed for igamma Boost int data. */
        GAMMA_P_DERIV_INT(BoostGamma::gammaPDerivative, "igamma_int_data_p_derivative.csv", 3.5, 1.1),
        /** gamma p derivative computed for igamma Boost small data. */
        GAMMA_P_DERIV_SMALL(BoostGamma::gammaPDerivative, "igamma_small_data_p_derivative.csv", 3.3, 0.99),
        /** gamma p derivative computed for igamma Boost med data. */
        GAMMA_P_DERIV_MED(BoostGamma::gammaPDerivative, "igamma_med_data_p_derivative.csv", 5, 1.25),
        /** gamma p derivative computed for igamma Boost big data. */
        GAMMA_P_DERIV_BIG(BoostGamma::gammaPDerivative, "igamma_big_data_p_derivative.csv", 550, 55),
        /** gamma p derivative computed for igamma Boost int data. */
        LOG_GAMMA_P_DERIV1_INT(BoostGammaTest::logGammaPDerivative1, "igamma_int_data_p_derivative.csv", 3, 50, 10),
        /** gamma p derivative computed for igamma Boost small data. */
        LOG_GAMMA_P_DERIV1_SMALL(BoostGammaTest::logGammaPDerivative1, "igamma_small_data_p_derivative.csv", 3, 8e11, 5e10),
        /** gamma p derivative computed for igamma Boost med data. */
        LOG_GAMMA_P_DERIV1_MED(BoostGammaTest::logGammaPDerivative1, "igamma_med_data_p_derivative.csv", 3, 190, 35),
        /** gamma p derivative computed for igamma Boost big data. */
        LOG_GAMMA_P_DERIV1_BIG(BoostGammaTest::logGammaPDerivative1, "igamma_big_data_p_derivative.csv", 3, 1.2e6, 125000),
        /** gamma p derivative computed for igamma Boost int data. */
        LOG_GAMMA_P_DERIV2_INT(BoostGammaTest::logGammaPDerivative2, "igamma_int_data_p_derivative.csv", 3, 2, 0.5),
        /** gamma p derivative computed for igamma Boost small data. */
        LOG_GAMMA_P_DERIV2_SMALL(BoostGammaTest::logGammaPDerivative2, "igamma_small_data_p_derivative.csv", 3, 1.8e10, 1.4e9),
        /** gamma p derivative computed for igamma Boost med data. */
        LOG_GAMMA_P_DERIV2_MED(BoostGammaTest::logGammaPDerivative2, "igamma_med_data_p_derivative.csv", 3, 6.2, 0.5),
        /** gamma p derivative computed for igamma Boost big data. */
        LOG_GAMMA_P_DERIV2_BIG(BoostGammaTest::logGammaPDerivative2, "igamma_big_data_p_derivative.csv", 3, 40000, 3000),
        /** igamma asymptotic approximation term. */
        IGAMMA_LARGE_X_ASYMP_TERM(BoostGammaTest::incompleteTgammaLargeX, "igamma_asymptotic_data.csv", 300, 110),
        /** gamma Q where the asymptotic approximation applies and <em>is not</em> used. */
        IGAMMA_LARGE_X_Q(BoostGammaTest::igammaQLargeXNoAsym, "igamma_asymptotic_data.csv", 3, 550, 130),
        /** gamma P where the asymptotic approximation applies and <em>is not</em> used. */
        IGAMMA_LARGE_X_P(BoostGammaTest::igammaPLargeXNoAsym, "igamma_asymptotic_data.csv", 4, 1, 0.3),
        /** gamma Q where the asymptotic approximation applies and <em>is</em> used. */
        IGAMMA_LARGE_X_Q_ASYM(BoostGammaTest::igammaQLargeXWithAsym, "igamma_asymptotic_data.csv", 3, 550, 190),
        /** gamma P where the asymptotic approximation applies and <em>is</em> used. */
        IGAMMA_LARGE_X_P_ASYM(BoostGammaTest::igammaPLargeXWithAsym, "igamma_asymptotic_data.csv", 4, 350, 110),
        /** tgamma delta ratio Boost data. */
        TGAMMA_DELTA_RATIO(BoostGamma::tgammaDeltaRatio, "gamma_delta_ratio_data.csv", 2, 9.5, 2.1),
        /** tgamma delta ratio Boost small int data. */
        TGAMMA_DELTA_RATIO_SMALL_INT(BoostGamma::tgammaDeltaRatio, "gamma_delta_ratio_int_data.csv", 2, 4.7, 1.1),
        /** tgamma delta ratio Boost int data. */
        TGAMMA_DELTA_RATIO_INT(BoostGamma::tgammaDeltaRatio, "gamma_delta_ratio_int2_data.csv", 2, 1.4, 0.45),
        /** tgamma delta ratio Boost data. */
        TGAMMA_DELTA_RATIO_NEG(BoostGammaTest::tgammaDeltaRatioNegative, "gamma_delta_ratio_data.csv", 3, 11.5, 1.7),
        /** tgamma delta ratio Boost small int data. */
        TGAMMA_DELTA_RATIO_SMALL_INT_NEG(BoostGammaTest::tgammaDeltaRatioNegative, "gamma_delta_ratio_int_data.csv", 3, 3.6, 1),
        /** tgamma delta ratio Boost int data. */
        TGAMMA_DELTA_RATIO_INT_NEG(BoostGammaTest::tgammaDeltaRatioNegative, "gamma_delta_ratio_int2_data.csv", 3, 1.1, 0.25),
        /** tgamma ratio Boost data. */
        TGAMMA_RATIO(BoostGamma::tgammaDeltaRatio, "gamma_delta_ratio_data.csv", 9.5, 2);

        /** The function. */
        private final DoubleBinaryOperator fun;

        /** The filename containing the test data. */
        private final String filename;

        /** The field containing the expected value. */
        private final int expected;

        /** The maximum allowed ulp. */
        private final double maxUlp;

        /** The maximum allowed RMS ulp. */
        private final double rmsUlp;

        /**
         * Instantiates a new test case.
         *
         * @param fun function to test
         * @param filename Filename of the test data
         * @param maxUlp maximum allowed ulp
         * @param rmsUlp maximum allowed RMS ulp
         */
        BiTestCase(DoubleBinaryOperator fun, String filename, double maxUlp, double rmsUlp) {
            this(fun, filename, 2, maxUlp, rmsUlp);
        }

        /**
         * Instantiates a new test case.
         *
         * @param fun function to test
         * @param filename Filename of the test data
         * @param expected Expected result field index
         * @param maxUlp maximum allowed ulp
         * @param rmsUlp maximum allowed RMS ulp
         */
        BiTestCase(DoubleBinaryOperator fun, String filename, int expected, double maxUlp, double rmsUlp) {
            this.fun = fun;
            this.filename = filename;
            this.expected = expected;
            this.maxUlp = maxUlp;
            this.rmsUlp = rmsUlp;
        }

        /**
         * @return function to test
         */
        DoubleBinaryOperator getFunction() {
            return fun;
        }

        /**
         * @return Filename of the test data
         */
        String getFilename() {
            return filename;
        }

        /**
         * @return Expected result field index
         */
        int getExpectedField() {
            return expected;
        }

        @Override
        public double getTolerance() {
            return maxUlp;
        }

        @Override
        public double getRmsTolerance() {
            return rmsUlp;
        }
    }

    @ParameterizedTest
    @CsvSource({
        // Pole errors
        "0, NaN",
        "-1, NaN",
        "-2, NaN",
        // Factorials: gamma(n+1) = n!
        "1, 1",
        "2, 1",
        "3, 2",
        "4, 6",
        "5, 24",
        "171, 0.7257415615307998967396728211129263114717e307",
        "171.9, Infinity",
        "172, Infinity",
        "172.1, Infinity",
        "500.34, Infinity, 0",
        "1000.123, Infinity, 0",
        "2000.1, Infinity, 0",
        // Overflow close to negative poles creates signed zeros
        "-171.99999999999997, 0.0",
        "-172, NaN",
        "-172.00000000000003, -0.0",
        "-172.99999999999997, -0.0",
        "-173, NaN",
        "-173.00000000000003, 0.0",
    })
    void testTGammaEdgeCases(double z, double expected) {
        Assertions.assertEquals(expected, BoostGamma.tgamma(z));
    }

    /**
     * Test the approach to the overflow limit of gamma(z).
     * This checks that extreme edge case handling is correct.
     */
    @ParameterizedTest
    @CsvSource({
        // Values computed using boost long double implementation.
        "171.5, 9.483367566824799e+307, 1",
        "171.62, 1.7576826789978127e+308, 1",
        "171.624, 1.7942117599248106e+308, 4",
        "171.62437600000001, 1.7976842943982607e+308, 4",
        "171.6244, Infinity, 0",
    })
    void testTGammaLimit(double z, double expected, int ulp) {
        TestUtils.assertEquals(expected, BoostGamma.tgamma(z), ulp);
    }

    /**
     * tgamma spot tests extracted from
     * {@code boost/libs/math/test/test_gamma.hpp}.
     */
    @Test
    void testTGammaSpotTests() {
        final int tolerance = 50;
        assertClose(BoostGamma::tgamma, 3.5, 3.3233509704478425511840640312646472177454052302295, tolerance);
        assertClose(BoostGamma::tgamma, 0.125, 7.5339415987976119046992298412151336246104195881491, tolerance);
        assertClose(BoostGamma::tgamma, -0.125, -8.7172188593831756100190140408231437691829605421405, tolerance);
        assertClose(BoostGamma::tgamma, -3.125, 1.1668538708507675587790157356605097019141636072094, tolerance);
        // Lower tolerance on this one, is only really needed on Linux x86 systems, result is mostly down to std lib accuracy:
        assertClose(BoostGamma::tgamma, -53249.0 / 1024, -1.2646559519067605488251406578743995122462767733517e-65, tolerance * 3);

        // Very small values, from a bug report by Rocco Romeo:
        assertClose(BoostGamma::tgamma, Math.scalb(1.0, -12), 4095.42302574977164107280305038926932586783813167844235368772, tolerance);
        assertClose(BoostGamma::tgamma, Math.scalb(1.0, -14), 16383.4228446989052821887834066513143241996925504706815681204, tolerance * 2);
        assertClose(BoostGamma::tgamma, Math.scalb(1.0, -25), 3.35544314227843645746319656372890833248893111091576093784981e7, tolerance);
        assertClose(BoostGamma::tgamma, Math.scalb(1.0, -27), 1.34217727422784342467508497080056807355928046680073490038257e8, tolerance);
        assertClose(BoostGamma::tgamma, Math.scalb(1.0, -29), 5.36870911422784336940727488260481582524683632281496706906706e8, tolerance);
        assertClose(BoostGamma::tgamma, Math.scalb(1.0, -35), 3.43597383674227843351272524573929605605651956475300480712955e10, tolerance);
        assertClose(BoostGamma::tgamma, Math.scalb(1.0, -54), 1.80143985094819834227843350984671942971248427509141008005685e16, tolerance);
        assertClose(BoostGamma::tgamma, Math.scalb(1.0, -64), 1.84467440737095516154227843350984671394471047428598176073616e19, tolerance);
        assertClose(BoostGamma::tgamma, Math.scalb(1.0, -66), 7.37869762948382064634227843350984671394068921181531525785592922800e19, tolerance);
        assertClose(BoostGamma::tgamma, Math.scalb(1.0, -33), 8.58993459142278433521360841138215453639282914047157884932317481977e9, tolerance);
        assertClose(BoostGamma::tgamma, 4 / Double.MAX_VALUE, Double.MAX_VALUE / 4, tolerance);
        assertClose(BoostGamma::tgamma, -Math.scalb(1.0, -12), -4096.57745718775464971331294488248972086965434176847741450728, tolerance);
        assertClose(BoostGamma::tgamma, -Math.scalb(1.0, -14), -16384.5772760354695939336148831283410381037202353359487504624, tolerance * 2);
        assertClose(BoostGamma::tgamma, -Math.scalb(1.0, -25), -3.35544325772156943776992988569766723938420508937071533029983e7, tolerance);
        assertClose(BoostGamma::tgamma, -Math.scalb(1.0, -27), -1.34217728577215672270574319043497450577151370942651414968627e8, tolerance);
        assertClose(BoostGamma::tgamma, -Math.scalb(1.0, -29), -5.36870912577215666743793215770406791630514293641886249382012e8, tolerance);
        assertClose(BoostGamma::tgamma, -Math.scalb(1.0, -34), -1.71798691845772156649591034966100693794360502123447124928244e10, tolerance);
        assertClose(BoostGamma::tgamma, -Math.scalb(1.0, -54), -1.80143985094819845772156649015329155101490229157245556564920e16, tolerance);
        assertClose(BoostGamma::tgamma, -Math.scalb(1.0, -64), -1.84467440737095516165772156649015328606601289230246224694513e19, tolerance);
        assertClose(BoostGamma::tgamma, -Math.scalb(1.0, -66), -7.37869762948382064645772156649015328606199162983179574406439e19, tolerance);
        assertClose(BoostGamma::tgamma, -Math.scalb(1.0, -33), -8.58993459257721566501667413261977598620193488449233402857632e9, tolerance);
        assertClose(BoostGamma::tgamma, -4 / Double.MAX_VALUE, -Double.MAX_VALUE / 4, tolerance);
        assertClose(BoostGamma::tgamma, -1 + Math.scalb(1.0, -22), -4.19430442278467170746130758391572421252211886167956799318843e6, tolerance);
        assertClose(BoostGamma::tgamma, -1 - Math.scalb(1.0, -22), 4.19430357721600151046968956086404748206205391186399889108944e6, tolerance);
        assertClose(BoostGamma::tgamma, -4 + Math.scalb(1.0, -20), 43690.7294216755534842491085530510391932288379640970386378756, tolerance);
        assertClose(BoostGamma::tgamma, -4 - Math.scalb(1.0, -20), -43690.6039118698506165317137699180871126338425941292693705533, tolerance);
        assertClose(BoostGamma::tgamma, -1 + Math.scalb(1.0, -44), -1.75921860444164227843350985473932247549232492467032584051825e13, tolerance);
        assertClose(BoostGamma::tgamma, -1 - Math.scalb(1.0, -44), 1.75921860444155772156649016131144377791001546933519242218430e13, tolerance);
        assertClose(BoostGamma::tgamma, -4 + Math.scalb(1.0, -44), 7.33007751850729421569517998006564998020333048893618664936994e11, tolerance);
        assertClose(BoostGamma::tgamma, -4 - Math.scalb(1.0, -44), -7.33007751850603911763815347967171096249288790373790093559568e11, tolerance);
        // Test bug fixes in tgamma:
        assertClose(BoostGamma::tgamma, 142.75, 7.8029496083318133344429227511387928576820621466e244, tolerance * 4);
        assertClose(BoostGamma::tgamma, -Double.MIN_VALUE, Double.NEGATIVE_INFINITY, 0);
        assertClose(BoostGamma::tgamma, Double.MIN_VALUE, Double.POSITIVE_INFINITY, 0);
    }

    @Test
    void testLanczosGmh() {
        // These terms are equal. The value (g - 0.5) is provided as a convenience.
        Assertions.assertEquals(Lanczos.G - 0.5, Lanczos.GMH);
        Assertions.assertEquals(0.5 - Lanczos.G, -Lanczos.GMH);
    }

    @ParameterizedTest
    @EnumSource(value = TestCase.class, mode = Mode.MATCH_ANY, names = {"TGAMMAO_.*"})
    void testTGammaOriginal(TestCase tc) {
        assertFunction(tc);
    }

    @ParameterizedTest
    @EnumSource(value = TestCase.class, mode = Mode.MATCH_ANY, names = {"TGAMMA_.*"})
    void testTGamma(TestCase tc) {
        assertFunction(tc);
    }

    /**
     * Read the gamma data and compare the result with the Commons Numbers 1.0 implementation
     * using data that requires the Lanczos support. This test verifies the Boost Lanczos
     * support is an improvement.
     */
    @ParameterizedTest
    @ValueSource(strings = {"gamma_20_150_data.csv", "gamma_factorials_data.csv"})
    @Order(1)
    void testTGammaWithLanczosSupport(String datafile) throws Exception {
        final TestUtils.ErrorStatistics e1 = new TestUtils.ErrorStatistics();
        final TestUtils.ErrorStatistics e2 = new TestUtils.ErrorStatistics();

        // Set this to allow all data to be processed.
        // If set to negative any failures will be output to stdout (useful to debugging).
        final double tolerance = 10;

        try (DataReader in = new DataReader(datafile)) {
            while (in.next()) {
                final double x = in.getDouble(0);

                // The Boost method tabulates the integer factorials so skip these.
                // Also skip those not support by the partial implementation.
                if ((int) x == x || x <= LANCZOS_THRESHOLD) {
                    continue;
                }

                final double v1 = gammaOriginal(x);
                // The original method can overflow even when x <= 170 (the largest factorial).
                // The largest supported value is around 141.5.
                if (Double.isInfinite(v1)) {
                    continue;
                }
                final double v2 = BoostGamma.tgamma(x);

                final BigDecimal expected = in.getBigDecimal(1);

                TestUtils.assertEquals(expected, v1, tolerance, e1::add, () -> "Numbers 1.0 x=" + x);
                TestUtils.assertEquals(expected, v2, tolerance, e2::add, () -> "Boost x=" + x);
            }
        }
        // The gamma functions are accurate to a few ULP.
        // This test is mainly interested in checking the Boost implementation is an improvement.
        final double maxTolerance = 6;
        final double rmsTolerance = 2;
        assertRms(TestError.of(datafile + "  Numbers 1.0", maxTolerance, rmsTolerance), e1);
        assertRms(TestError.of(datafile + "  Boost      ", maxTolerance, rmsTolerance), e2);
        Assertions.assertTrue(e2.getRMS() < e1.getRMS() * 0.8, "Expected better precision");
        Assertions.assertTrue(e2.getMaxAbs() < e1.getMaxAbs() * 0.7, "Expected lower max error");
        Assertions.assertTrue(Math.abs(e2.getMean()) < Math.abs(e1.getMean()) * 0.5, "Expected better accuracy");
    }

    @ParameterizedTest
    @CsvSource({
        // Pole errors
        "0, NaN",
        "-1, NaN",
        "-2, NaN",
        // Factorials: gamma(n+1) = n!
        "1, 0",
        "2, 0",
    })
    void testLGammaEdgeCases(double z, double p) {
        Assertions.assertEquals(p, BoostGamma.lgamma(z));
    }

    /**
     * lgamma spot tests extracted from
     * {@code boost/libs/math/test/test_gamma.hpp}.
     */
    @Test
    void testLGammaSpotTests() {
        final int tolerance = 1;
        final int[] sign = {0};
        final DoubleUnaryOperator fun = z -> BoostGamma.lgamma(z, sign);

        assertClose(fun, 3.5, 1.2009736023470742248160218814507129957702389154682, tolerance);
        Assertions.assertEquals(1, sign[0]);
        assertClose(fun, 0.125, 2.0194183575537963453202905211670995899482809521344, tolerance);
        Assertions.assertEquals(1, sign[0]);
        assertClose(fun, -0.125, 2.1653002489051702517540619481440174064962195287626, tolerance);
        Assertions.assertEquals(-1, sign[0]);
        assertClose(fun, -3.125, 0.1543111276840418242676072830970532952413339012367, tolerance * 2);
        Assertions.assertEquals(1, sign[0]);
        assertClose(fun, -53249.0 / 1024, -149.43323093420259741100038126078721302600128285894, tolerance);
        Assertions.assertEquals(-1, sign[0]);
        // Very small values, from a bug report by Rocco Romeo:
        assertClose(fun, Math.scalb(1.0, -12), Math.log(4095.42302574977164107280305038926932586783813167844235368772), tolerance);
        Assertions.assertEquals(1, sign[0]);
        assertClose(fun, Math.scalb(1.0, -14), Math.log(16383.4228446989052821887834066513143241996925504706815681204), tolerance);
        Assertions.assertEquals(1, sign[0]);
        assertClose(fun, Math.scalb(1.0, -25), Math.log(3.35544314227843645746319656372890833248893111091576093784981e7), tolerance);
        Assertions.assertEquals(1, sign[0]);
        assertClose(fun, Math.scalb(1.0, -27), Math.log(1.34217727422784342467508497080056807355928046680073490038257e8), tolerance);
        Assertions.assertEquals(1, sign[0]);
        assertClose(fun, Math.scalb(1.0, -29), Math.log(5.36870911422784336940727488260481582524683632281496706906706e8), tolerance);
        Assertions.assertEquals(1, sign[0]);
        assertClose(fun, Math.scalb(1.0, -35), Math.log(3.43597383674227843351272524573929605605651956475300480712955e10), tolerance);
        Assertions.assertEquals(1, sign[0]);
        assertClose(fun, Math.scalb(1.0, -54), Math.log(1.80143985094819834227843350984671942971248427509141008005685e16), tolerance);
        Assertions.assertEquals(1, sign[0]);
        assertClose(fun, Math.scalb(1.0, -64), Math.log(1.84467440737095516154227843350984671394471047428598176073616e19), tolerance);
        Assertions.assertEquals(1, sign[0]);
        assertClose(fun, Math.scalb(1.0, -66), Math.log(7.37869762948382064634227843350984671394068921181531525785592922800e19), tolerance);
        Assertions.assertEquals(1, sign[0]);
        assertClose(fun, Math.scalb(1.0, -33), Math.log(8.58993459142278433521360841138215453639282914047157884932317481977e9), tolerance);
        Assertions.assertEquals(1, sign[0]);
        assertClose(fun, 4 / Double.MAX_VALUE, Math.log(Double.MAX_VALUE / 4), tolerance);
        Assertions.assertEquals(1, sign[0]);
        assertClose(fun, -Math.scalb(1.0, -12), Math.log(4096.57745718775464971331294488248972086965434176847741450728), tolerance);
        Assertions.assertEquals(-1, sign[0]);
        assertClose(fun, -Math.scalb(1.0, -14), Math.log(16384.5772760354695939336148831283410381037202353359487504624), tolerance);
        Assertions.assertEquals(-1, sign[0]);
        assertClose(fun, -Math.scalb(1.0, -25), Math.log(3.35544325772156943776992988569766723938420508937071533029983e7), tolerance);
        Assertions.assertEquals(-1, sign[0]);
        assertClose(fun, -Math.scalb(1.0, -27), Math.log(1.34217728577215672270574319043497450577151370942651414968627e8), tolerance);
        Assertions.assertEquals(-1, sign[0]);
        assertClose(fun, -Math.scalb(1.0, -29), Math.log(5.36870912577215666743793215770406791630514293641886249382012e8), tolerance);
        Assertions.assertEquals(-1, sign[0]);
        assertClose(fun, -Math.scalb(1.0, -34), Math.log(1.71798691845772156649591034966100693794360502123447124928244e10), tolerance);
        Assertions.assertEquals(-1, sign[0]);
        assertClose(fun, -Math.scalb(1.0, -54), Math.log(1.80143985094819845772156649015329155101490229157245556564920e16), tolerance);
        Assertions.assertEquals(-1, sign[0]);
        assertClose(fun, -Math.scalb(1.0, -64), Math.log(1.84467440737095516165772156649015328606601289230246224694513e19), tolerance);
        Assertions.assertEquals(-1, sign[0]);
        assertClose(fun, -Math.scalb(1.0, -66), Math.log(7.37869762948382064645772156649015328606199162983179574406439e19), tolerance);
        Assertions.assertEquals(-1, sign[0]);
        assertClose(fun, -Math.scalb(1.0, -33), Math.log(8.58993459257721566501667413261977598620193488449233402857632e9), tolerance);
        Assertions.assertEquals(-1, sign[0]);
        assertClose(fun, -4 / Double.MAX_VALUE, Math.log(Double.MAX_VALUE / 4), tolerance);
        Assertions.assertEquals(-1, sign[0]);
        assertClose(fun, -1 + Math.scalb(1.0, -22), Math.log(4.19430442278467170746130758391572421252211886167956799318843e6), tolerance);
        Assertions.assertEquals(-1, sign[0]);
        assertClose(fun, -1 - Math.scalb(1.0, -22), Math.log(4.19430357721600151046968956086404748206205391186399889108944e6), tolerance);
        Assertions.assertEquals(1, sign[0]);
        assertClose(fun, -4 + Math.scalb(1.0, -20), Math.log(43690.7294216755534842491085530510391932288379640970386378756), tolerance);
        Assertions.assertEquals(1, sign[0]);
        assertClose(fun, -4 - Math.scalb(1.0, -20), Math.log(43690.6039118698506165317137699180871126338425941292693705533), tolerance);
        Assertions.assertEquals(-1, sign[0]);

        assertClose(fun, -1 + Math.scalb(1.0, -44), Math.log(1.75921860444164227843350985473932247549232492467032584051825e13), tolerance);
        Assertions.assertEquals(-1, sign[0]);
        assertClose(fun, -1 - Math.scalb(1.0, -44), Math.log(1.75921860444155772156649016131144377791001546933519242218430e13), tolerance);
        Assertions.assertEquals(1, sign[0]);
        assertClose(fun, -4 + Math.scalb(1.0, -44), Math.log(7.33007751850729421569517998006564998020333048893618664936994e11), tolerance);
        Assertions.assertEquals(1, sign[0]);
        assertClose(fun, -4 - Math.scalb(1.0, -44), Math.log(7.33007751850603911763815347967171096249288790373790093559568e11), tolerance);
        Assertions.assertEquals(-1, sign[0]);
        //
        // Extra large values for lgamma, see https://github.com/boostorg/math/issues/242
        //
        assertClose(fun, Math.scalb(11103367432951928.0, 32), 2.7719825960021351251696385101478518546793793286704974382373670822285114741208958e27, tolerance);
        assertClose(fun, Math.scalb(11103367432951928.0, 62), 4.0411767712186990905102512019058204792570873633363159e36, tolerance);
        assertClose(fun, Math.scalb(11103367432951928.0, 326), 3.9754720509185529233002820161357111676582583112671658e116, tolerance);
        //
        // Super small values may cause spurious overflow:
        //
        double value = Double.MIN_NORMAL;
        while (value != 0) {
            Assertions.assertTrue(Double.isFinite(fun.applyAsDouble(value)));
            value /= 2;
        }

        // Simple check to ensure a zero length array is ignored
        final int[] signEmpty = {};
        for (final double z : new double[] {3.5, 6.76, 8.12}) {
            Assertions.assertEquals(BoostGamma.lgamma(z), BoostGamma.lgamma(z, signEmpty));
        }
    }

    @ParameterizedTest
    @EnumSource(value = TestCase.class, mode = Mode.MATCH_ANY, names = {"LGAMMA_.*"})
    void testLGamma(TestCase tc) {
        assertFunction(tc);
    }

    @ParameterizedTest
    @CsvSource({
        // Pole errors
        "-1, NaN",
        "-2, NaN",
        // Factorials: gamma(n+1)-1 = n! - 1
        "0, 0",
        "1, 0",
        "2, 1",
        "3, 5",
        "4, 23",
        "5, 119",
    })
    void testTGammap1m1EdgeCases(double z, double p) {
        Assertions.assertEquals(p, BoostGamma.tgamma1pm1(z));
    }

    @ParameterizedTest
    @EnumSource(value = TestCase.class, mode = Mode.MATCH_ANY, names = {"TGAMMAP1M1.*"})
    void testTGammap1m1(TestCase tc) {
        assertFunction(tc);
    }

    @ParameterizedTest
    @CsvSource({
        "0, 1, -1",
        "0, 0, 0",
        "0, -1, Infinity",
        "2, -2, -0.75",
        "2, 1024, Infinity",
        "2, -1075, -1",
        "NaN, 1, NaN",
        "1, NaN, NaN",
        // Negative x, even integer y
        "-2, 2, 3",
        // Negative x, non (even integer) y
        "-2, 2.1, NaN",
    })
    void testPowm1EdgeCases(double x, double y, double expected) {
        Assertions.assertEquals(expected, BoostMath.powm1(x, y));
    }

    @ParameterizedTest
    @EnumSource(value = BiTestCase.class, mode = Mode.MATCH_ANY, names = {"POWM1.*"})
    void testPowm1(BiTestCase tc) {
        assertFunction(tc);
    }

    /**
     * Test the log1pmx function with values that do not require high precision.
     *
     * @param x Argument x
     */
    @ParameterizedTest
    @ValueSource(doubles = {-1.1, -1, -0.9, 0, 1, 1.5, 2, 3})
    void testLog1pmxStandard(double x) {
        Assertions.assertEquals(Math.log1p(x) - x, SpecialMath.log1pmx(x));
    }

    /**
     * Test the log1pmx function. The function is not a direct port of the Boost log1pmx
     * function so resides in the {@link SpecialMath} class. It is only used in {@link BoostGamma}
     * so tested here using the same test framework.
     */
    @ParameterizedTest
    @EnumSource(value = TestCase.class, mode = Mode.MATCH_ANY, names = {"LOG1PMX.*"})
    void testLog1pmx(TestCase tc) {
        assertFunction(tc);
    }

    @ParameterizedTest
    @CsvSource({
        // Argument a > 0
        "NaN, 1, NaN",
        "0, 1, NaN",
        "-1, 1, NaN",
        // Argument z >= 0
        "1, NaN, NaN",
        "1, -1, NaN",
    })
    void testIGammaEdgeCases(double a, double z, double expected) {
        // All functions have the same invalid domain for a and z
        Assertions.assertEquals(expected, BoostGamma.tgamma(a, z), "tgamma");
        Assertions.assertEquals(expected, BoostGamma.tgammaLower(a, z), "tgammaLower");
        Assertions.assertEquals(expected, BoostGamma.gammaP(a, z), "gammaP");
        Assertions.assertEquals(expected, BoostGamma.gammaQ(a, z), "gammaQ");
        Assertions.assertEquals(expected, BoostGamma.gammaPDerivative(a, z), "gammaPDerivative");
    }

    @ParameterizedTest
    @CsvSource({
        // z==0
        "2, 0, 0",
        "1, 0, 1",
        "0.5, 0, Infinity",
    })
    void testGammaPDerivativeEdgeCases(double a, double z, double expected) {
        Assertions.assertEquals(expected, BoostGamma.gammaPDerivative(a, z));
    }

    /**
     * tgamma spot tests extracted from
     * {@code boost/libs/math/test/test_igamma.hpp}.
     */
    @Test
    void testIGammaSpotTests() {
        int tolerance = 10;
        assertClose(BoostGamma::tgamma, 5, 1, 23.912163676143750903709045060494956383977723517065, tolerance);
        assertClose(BoostGamma::tgamma, 5, 5, 10.571838841565097874621959975919877646444998907920, tolerance);
        assertClose(BoostGamma::tgamma, 5, 10, 0.70206451384706574414638719662835463671916532623256, tolerance);
        assertClose(BoostGamma::tgamma, 5, 100, 3.8734332808745531496973774140085644548465762343719e-36, tolerance);
        assertClose(BoostGamma::tgamma, 0.5, 0.5, 0.56241823159440712427949495730204306902676756479651, tolerance);
        assertClose(BoostGamma::tgamma, 0.5, 9.0 / 10, 0.31853210360412109873859360390443790076576777747449, tolerance * 10);
        assertClose(BoostGamma::tgamma, 0.5, 5, 0.0027746032604128093194908357272603294120210079791437, tolerance);
        assertClose(BoostGamma::tgamma, 0.5, 100, 3.7017478604082789202535664481339075721362102520338e-45, tolerance);

        assertClose(BoostGamma::tgammaLower, 5, 1, 0.087836323856249096290954939505043616022276482935091, tolerance);
        assertClose(BoostGamma::tgammaLower, 5, 5, 13.428161158434902125378040024080122353555001092080, tolerance);
        assertClose(BoostGamma::tgammaLower, 5, 10, 23.297935486152934255853612803371645363280834673767, tolerance);
        assertClose(BoostGamma::tgammaLower, 5, 100, 23.999999999999999999999999999999999996126566719125, tolerance);

        assertClose(BoostGamma::gammaQ, 5, 1, 0.99634015317265628765454354418728984933240514654437, tolerance);
        assertClose(BoostGamma::gammaQ, 5, 5, 0.44049328506521241144258166566332823526854162116334, tolerance);
        assertClose(BoostGamma::gammaQ, 5, 10, 0.029252688076961072672766133192848109863298555259690, tolerance);
        assertClose(BoostGamma::gammaQ, 5, 100, 1.6139305336977304790405739225035685228527400976549e-37, tolerance);
        assertClose(BoostGamma::gammaQ, 1.5, 2, 0.26146412994911062220282207597592120190281060919079, tolerance);
        assertClose(BoostGamma::gammaQ, 20.5, 22, 0.34575332043467326814971590879658406632570278929072, tolerance);

        assertClose(BoostGamma::gammaP, 5, 1, 0.0036598468273437123454564558127101506675948534556288, tolerance);
        assertClose(BoostGamma::gammaP, 5, 5, 0.55950671493478758855741833433667176473145837883666, tolerance);
        assertClose(BoostGamma::gammaP, 5, 10, 0.97074731192303892732723386680715189013670144474031, tolerance);
        assertClose(BoostGamma::gammaP, 5, 100, 0.9999999999999999999999999999999999998386069466302, tolerance);
        assertClose(BoostGamma::gammaP, 1.5, 2, 0.73853587005088937779717792402407879809718939080921, tolerance);
        assertClose(BoostGamma::gammaP, 20.5, 22, 0.65424667956532673185028409120341593367429721070928, tolerance);

        // naive check on derivative function:
        tolerance = 50;
        assertClose(BoostGamma::gammaPDerivative, 20.5, 22,
            Math.exp(-22) * Math.pow(22, 19.5) / BoostGamma.tgamma(20.5), tolerance);

        // Bug reports from Rocco Romeo:
        assertClose(BoostGamma::tgamma, 20, Math.scalb(1.0, -40), 1.21645100408832000000e17, tolerance);
        assertClose(BoostGamma::tgammaLower, 20, Math.scalb(1.0, -40), 7.498484069471659696438206828760307317022658816757448882e-243, tolerance);
        assertClose(BoostGamma::gammaP, 20, Math.scalb(1.0, -40), 6.164230243774976473534975936127139110276824507876192062e-260, tolerance);

        assertClose(BoostGamma::tgamma, 30, Math.scalb(1.0, -30), 8.841761993739701954543616000000e30, tolerance);
        assertClose(BoostGamma::tgammaLower, 30, Math.scalb(1.0, -30), 3.943507283668378474979245322638092813837393749566146974e-273, tolerance);
        assertClose(BoostGamma::gammaP, 30, Math.scalb(1.0, -30), 4.460092102072560946444018923090222645613009128135650652e-304, tolerance);
        assertClose(BoostGamma::gammaPDerivative, 2, Math.scalb(1.0, -575), 8.08634922390438981326119906687585206568664784377654648227177e-174, tolerance);

        assertEquals(BoostGamma::tgamma, 176, 100, Double.POSITIVE_INFINITY);
        assertEquals(BoostGamma::tgamma, 530, 2000, Double.POSITIVE_INFINITY);
        assertEquals(BoostGamma::tgamma, 740, 2500, Double.POSITIVE_INFINITY);
        assertEquals(BoostGamma::tgamma, 530.5, 2000, Double.POSITIVE_INFINITY);
        assertEquals(BoostGamma::tgamma, 740.5, 2500, Double.POSITIVE_INFINITY);
        assertEquals(BoostGamma::tgammaLower, 10000.0f, 10000.0f / 4, Double.POSITIVE_INFINITY);
        assertClose(BoostGamma::tgamma, 170, 165, 2.737338337642022829223832094019477918166996032112404370e304, tolerance);
        assertClose(BoostGamma::tgammaLower, 170, 165, 1.531729671362682445715419794880088619901822603944331733e304, tolerance);
        // *** Increased from 10 * tolerance ***
        assertClose(BoostGamma::tgamma, 170, 170, 2.090991698081449410761040647015858316167077909285580375e304, 16 * tolerance);
        assertClose(BoostGamma::tgammaLower, 170, 170, 2.178076310923255864178211241883708221901740726771155728e304, 16 * tolerance);
        assertClose(BoostGamma::tgamma, 170, 190, 2.8359275512790301602903689596273175148895758522893941392e303, 10 * tolerance);
        assertClose(BoostGamma::tgammaLower, 170, 190, 3.985475253876802258910214992936834786579861050827796689e304, 10 * tolerance);
        // *** Increased from 10 * tolerance ***
        assertClose(BoostGamma::tgamma, 170, 1000, 6.1067635957780723069200425769800190368662985052038980542e72, 16 * tolerance);

        assertClose(BoostGamma::tgammaLower, 185, 1, 0.001999286058955490074702037576083582139834300307968257924836, tolerance);
        assertClose(BoostGamma::tgamma, 185, 1500, 1.037189524841404054867100938934493979112615962865368623e-67, tolerance * 10);

        assertClose(BoostGamma::tgamma, 36, Math.scalb(1.0, -26), 1.03331479663861449296666513375232000000e40, tolerance * 10);
        assertClose(BoostGamma::tgamma, 50.5, Math.scalb(1.0, -17), 4.2904629123519598109157551960589377e63, tolerance * 10);
        assertClose(BoostGamma::tgamma, 164.5, 0.125, 2.5649307433687542701168405519538910e292, tolerance * 10);
        //
        // Check very large parameters, see: https://github.com/boostorg/math/issues/168
        //
        final double maxVal = Double.MAX_VALUE;
        final double largeVal = maxVal * 0.99f;
        assertEquals(BoostGamma::tgamma, 22.25, maxVal, 0);
        assertEquals(BoostGamma::tgamma, 22.25, largeVal, 0);
        assertEquals(BoostGamma::tgammaLower, 22.25, maxVal, BoostGamma.tgamma(22.25));
        assertEquals(BoostGamma::tgammaLower, 22.25, largeVal, BoostGamma.tgamma(22.25));
        assertEquals(BoostGamma::gammaQ, 22.25, maxVal, 0);
        assertEquals(BoostGamma::gammaQ, 22.25, largeVal, 0);
        assertEquals(BoostGamma::gammaP, 22.25, maxVal, 1);
        assertEquals(BoostGamma::gammaP, 22.25, largeVal, 1);
        assertEquals(BoostGamma::tgamma, 22.25, Double.POSITIVE_INFINITY, 0);
        assertEquals(BoostGamma::tgammaLower, 22.25, Double.POSITIVE_INFINITY, BoostGamma.tgamma(22.25));
        assertEquals(BoostGamma::gammaQ, 22.25, Double.POSITIVE_INFINITY, 0);
        assertEquals(BoostGamma::gammaP, 22.25, Double.POSITIVE_INFINITY, 1);
        //
        // Large arguments and small parameters, see
        // https://github.com/boostorg/math/issues/451:
        //
        assertEquals(BoostGamma::gammaQ, 1770, 1e-12, 1);
        assertEquals(BoostGamma::gammaP, 1770, 1e-12, 0);
    }

    @ParameterizedTest
    @EnumSource(value = BiTestCase.class, mode = Mode.MATCH_ANY, names = {"IGAMMA_U.*"})
    void testIGammaUpper(BiTestCase tc) {
        assertFunction(tc);
    }

    @ParameterizedTest
    @EnumSource(value = BiTestCase.class, mode = Mode.MATCH_ANY, names = {"IGAMMA_L.*"})
    void testIGammaLower(BiTestCase tc) {
        assertFunction(tc);
    }

    @ParameterizedTest
    @EnumSource(value = BiTestCase.class, mode = Mode.MATCH_ANY, names = {"IGAMMA_Q.*"})
    void testIGammaQ(BiTestCase tc) {
        assertFunction(tc);
    }

    @ParameterizedTest
    @EnumSource(value = BiTestCase.class, mode = Mode.MATCH_ANY, names = {"IGAMMA_P.*"})
    void testIGammaP(BiTestCase tc) {
        assertFunction(tc);
    }

    @ParameterizedTest
    @EnumSource(value = BiTestCase.class, mode = Mode.MATCH_ANY, names = {"GAMMA_P_DERIV.*"})
    void testGammaPDerivative(BiTestCase tc) {
        assertFunction(tc);
    }

    @ParameterizedTest
    @EnumSource(value = BiTestCase.class, mode = Mode.MATCH_ANY, names = {"LOG_GAMMA_P_DERIV.*"})
    void testLogGammaPDerivative(BiTestCase tc) {
        assertFunction(tc);
    }

    /**
     * Test the incomplete gamma function uses the policy containing the epsilon and
     * maximum iterations for series evaluations. The data targets each method computed
     * using a series component to check the policy is not ignored.
     *
     * <p>Running the policy tests on their own should hit the code paths
     * using the policy for function evaluation:
     * <pre>
     * mvn clean test -Dtest=BoostGammaTest#testIGammaPolicy* jacoco:report
     * </pre>
     */
    @ParameterizedTest
    @CsvSource(value = {
        // Methods 0, 1, 5, 6 do not use the iteration policy
        // Data extracted from the resource files and formatted to double precision

        // Method 2: x > 1.1, x - (1 / (3 * x)) < a
        "5.0,2.5,21.38827245393963,0.8911780189141513,2.6117275460603704,0.10882198108584876",
        // Method 4: a < 20, x > 1.1, x - (1 / (3 * x)) > a
        "19.24400520324707,21.168405532836914,4.0308280447358675E15,0.3084240508178698,9.038282597080282E15,0.6915759491821302",
        // Method 7: (x > 1000) && (a < x * 0.75f)
        "664.0791015625,1328.158203125,Infinity,4.90100553385586E-91,Infinity,1.0",
        // Method 2: 0.5 < x < 1.1, x * 0.75f < a
        "0.9759566783905029,1.0735523700714111,0.33659577343416824,0.33179703084688433,0.6778671124302277,0.6682029691531157",
        // Method 3: 0.5 < x < 1.1, x * 0.75f > a
        "0.4912221431732178,0.9824442863464355,0.2840949896471149,0.1575143024618326,1.519518937513272,0.8424856975381674",
    })
    void testIGammaPolicy(double a, double x, double upper, double q, double lower, double p) {
        // Low iterations should fail to converge
        final Policy pol1 = new Policy(0x1.0p-52, 1);
        Assertions.assertThrows(ArithmeticException.class, () -> BoostGamma.tgamma(a, x, pol1), "upper");
        Assertions.assertThrows(ArithmeticException.class, () -> BoostGamma.tgammaLower(a, x, pol1), "lower");
        Assertions.assertThrows(ArithmeticException.class, () -> BoostGamma.gammaP(a, x, pol1), "p");
        Assertions.assertThrows(ArithmeticException.class, () -> BoostGamma.gammaQ(a, x, pol1), "q");

        // Low epsilon should not be as accurate
        final Policy pol2 = new Policy(1e-3, Integer.MAX_VALUE);

        // Innore infinite
        if (Double.isFinite(upper)) {
            final double u1 = BoostGamma.tgamma(a, x);
            final double u2 = BoostGamma.tgamma(a, x, pol2);
            assertCloser("upper", upper, u1, u2);
        }
        if (Double.isFinite(lower)) {
            final double l1 = BoostGamma.tgammaLower(a, x);
            final double l2 = BoostGamma.tgammaLower(a, x, pol2);
            assertCloser("lower", lower, l1, l2);
        }

        // Ignore 0 or 1
        if ((int) p != p) {
            final double p1 = BoostGamma.gammaP(a, x);
            final double p2 = BoostGamma.gammaP(a, x, pol2);
            assertCloser("p", p, p1, p2);
        }
        if ((int) q != q) {
            final double q1 = BoostGamma.gammaQ(a, x);
            final double q2 = BoostGamma.gammaQ(a, x, pol2);
            assertCloser("q", q, q1, q2);
        }
    }

    @Test
    void testIGammaPolicy1() {
        // a >= MAX_FACTORIAL && !normalised; invert && (a * 4 < x)
        final double a = 230.1575469970703125;
        final double x = 23015.75390625;
        // expected ~ 0.95e-8996
        final double upper = 0;
        final double u1 = BoostGamma.tgamma(a, x);
        Assertions.assertEquals(upper, u1);
        final Policy pol1 = new Policy(0x1.0p-52, 1);
        Assertions.assertThrows(ArithmeticException.class, () -> BoostGamma.tgamma(a, x, pol1), "upper");
        // Not possible to test the result is not as close to zero with a lower epsilon
    }

    @Test
    void testIGammaPolicy2() {
        // a >= MAX_FACTORIAL && !normalised; !invert && (a > 4 * x)
        final double a = 5823.5341796875;
        final double x = 1.0;
        final double lower = 0.6318201301512319242829e-4;
        final double l1 = BoostGamma.tgammaLower(a, x);
        Assertions.assertEquals(lower, l1, lower * 1e-10);
        final Policy pol1 = new Policy(0x1.0p-52, 1);
        Assertions.assertThrows(ArithmeticException.class, () -> BoostGamma.tgammaLower(a, x, pol1), "lower");
        final Policy pol2 = new Policy(1e-3, Integer.MAX_VALUE);
        assertCloser("lower", lower, l1, BoostGamma.tgammaLower(a, x, pol2));
    }

    @Test
    void testIGammaPolicy3() {
        // a >= MAX_FACTORIAL && !normalised; other
        // In this case the regularized result is first computed then scaled back.
        // The regularized result is typically around 0.5 and is
        // computed without iteration with the Temme algorithm (method 5).
        // No cases exist in the current test data that require iteration.
        // We can check the iteration policy is used for one case.
        final double a = 53731.765625;
        final double x = 26865.8828125;
        final double lower = Double.POSITIVE_INFINITY;
        final double l1 = BoostGamma.tgammaLower(a, x);
        Assertions.assertEquals(lower, l1);
        final Policy pol1 = new Policy(0x1.0p-52, 1);
        Assertions.assertThrows(ArithmeticException.class, () -> BoostGamma.tgammaLower(a, x, pol1), "lower");
    }

    /**
     * Assert x is closer to the expected result than y.
     */
    private static void assertCloser(String msg, double expected, double x, double y) {
        final double dx = Math.abs(expected - x);
        final double dy = Math.abs(expected - y);
        Assertions.assertTrue(dx < dy,
            () -> String.format("%s %s : %s (%s) : %s (%s)", msg, expected, x, dx, y, dy));
    }

    /**
     * Test incomplete gamma function with large X. This is a subset of the test data
     * to isolate the use of the asymptotic approximation for the upper gamma fraction.
     */
    @ParameterizedTest
    @EnumSource(value = BiTestCase.class, mode = Mode.MATCH_ANY, names = {"IGAMMA_LARGE_X.*"})
    void testIGammaLargeX(BiTestCase tc) {
        assertFunction(tc);
    }

    @Test
    void testIGammaAsymptoticApproximationFailsOnVeryLargeX() {
        final double a = 1e30;
        final double x = a + Math.ulp(a);
        // The aymptotic approximation can be used as a < x:
        Assertions.assertTrue(a < x);
        // The term is too big to subtract small integers
        Assertions.assertEquals(a, a - 1);

        // This will not converge as the term will not reduce.
        // Limit the iterations to a million.
        final Policy pol = new Policy(Math.ulp(1.0), 10000000);
        Assertions.assertThrows(ArithmeticException.class,
            () -> BoostGamma.incompleteTgammaLargeX(a, x, pol));
    }

    /**
     * Test the Boost 1_77_0 switch to the asymptotic approximation results in
     * less accurate results for Q.
     *
     * <p>Note: @Order annotations are used on these methods to collect them together
     * if outputting the summary to stdout.
     */
    @ParameterizedTest
    @ValueSource(strings = {"igamma_int_data.csv", "igamma_med_data.csv", "igamma_big_data.csv"})
    @Order(1)
    void testGammaQLargeXOriginal(String datafile) throws Exception {
        assertIgammaLargeX("Boost", datafile, true, USE_ASYM_APPROX, USE_ASYM_APPROX, false);
    }

    /**
     * Test the Boost 1_77_0 switch to the asymptotic approximation results in
     * less accurate results for P.
     */
    @ParameterizedTest
    @ValueSource(strings = {"igamma_int_data.csv", "igamma_med_data.csv", "igamma_big_data.csv"})
    @Order(1)
    void testGammaPLargeXOriginal(String datafile) throws Exception {
        assertIgammaLargeX("Boost", datafile, false, USE_ASYM_APPROX, USE_ASYM_APPROX, false);
    }

    /**
     * Test an updated switch to the asymptotic approximation results in
     * more accurate results for Q.
     */
    @ParameterizedTest
    @ValueSource(strings = {"igamma_int_data.csv", "igamma_med_data.csv", "igamma_big_data.csv"})
    @Order(1)
    void testGammaQLargeX(String datafile) throws Exception {
        assertIgammaLargeX("Commons", datafile, true, getLargeXTarget(), getUseAsymApprox(), true);
    }

    /**
     * Test an updated switch to the asymptotic approximation results in
     * more accurate results for P.
     */
    @ParameterizedTest
    @ValueSource(strings = {"igamma_int_data.csv", "igamma_med_data.csv", "igamma_big_data.csv"})
    @Order(1)
    void testGammaPLargeX(String datafile) throws Exception {
        assertIgammaLargeX("Commons", datafile, false, getLargeXTarget(), getUseAsymApprox(), true);
    }

    /**
     * @return Predicate to identify target data for igamma suitable for the asymptotic approximation.
     */
    private static DoubleDoubleBiPredicate getLargeXTarget() {
        // Target the data that is computed using this method in Boost.
        // It will test that the method switch is an improvement.
        return USE_ASYM_APPROX;

        // Target all data that is computed using this method in Commons numbers.
        // It will test that when the method is switched it
        // will not make the computation worse.
        //return getUseAsymApprox();

        // Allow more data to be tested.
        // This predicate returns all valid data. Use it for testing but note that
        // most data is not suitable for the large x approximation.
        //return (a, x) -> !isIntOrHalfInt(a, x) && x >= 1.1 && (a < x);
    }

    /**
     * @return Predicate to identify when to use the asymptotic approximation.
     */
    private static DoubleDoubleBiPredicate getUseAsymApprox() {
        // The asymptotic approximation is suitable for large x.
        // It sums terms starting from 1 until convergence.
        // term_n = term_(n-1) * (a-n) / z
        // term_0 = 1
        // Terms will get smaller if a < z.
        // The Boost condition is:
        // (x > 1000) && ((a < x) || (Math.abs(a - 50) / x < 1))
        //
        // This is not suitable if a ~ x and x is very large. The series takes
        // too many iterations to converge. With limited precision it may not be
        // possible to reduce the size of a, e.g. 1e16 - 1 == 1e16, thus
        // the terms do not reduce in size if decremented directly and take
        // many iterations to build a counter n that can be subtracted: a - n

        // Experimental:
        // Estimate the number of iterations.
        //
        // Assuming the term is reduced by a/z:
        // term_n = (a/z)^n
        // Setting the limit for term_n == epsilon (2^-52):
        // n = log(epsilon) / log(a/z)
        // Note: log(2^-52) is approximately -36.
        // Estimate of n is high given the factor is (a-i)/z for iteration i.

        // int limit = 1000;
        // return (a, x) -> (x > 1000) && (a < x) && (-36 / Math.log(a / x) < limit);

        // Simple:
        //
        // Target only data that will quickly converge.
        // Not that there is loss of precision in the terms when a ~ z.
        // The closer a/z is to 1 the more bits are lost in the result as the
        // terms are inexact and this error is compounded by a large number of terms
        // in the series.
        // This condition ensures the asymptotic approximation is used when x -> infinity
        // and a << x.
        // Using the logic from above an overestimate of the maximum iterations is:
        // threshold   value      iterations
        // 1 - 2^-1    0.5        52
        // 1 - 2^-2    0.75       125
        // 1 - 2^-3    0.875      270
        // 1 - 2^-4    0.9375     558
        // 1 - 2^-5    0.96875    1135
        // 1 - 2^-6    0.984375   2289
        return (a, x) -> (x > 1000) && (a < x * 0.75);
    }

    /**
     * Read the regularized incomplete gamma data and compare the result with or
     * without the asymptotic approximation for large X. This test verifies the
     * conditions used for the asymptotic approximation.
     *
     * <p>All applicable target data is evaluated with the incomplete gamma function,
     * either without the asymptotic approximation or using it when allowed to
     * by the specified predicate. The final RMS error for the two methods
     * with or without the asymptotic approximation applied are compared. The test
     * asserts if the change is an improvement or not.
     *
     * <p>Prior to Boost 1_68_0 the asymptotic approximation was not used. It was
     * added to support large x. Using the conditions from Boost 1_77_0 to switch to
     * the asymptotic approximation results is worse accuracy.
     *
     * <p>This test can be revisited if the Boost reference code is updated for the
     * conditions under which the asymptotic approximation is used. It is possible
     * to adjust the condition directly in this test and determine if the RMS error
     * improves using the asymptotic approximation.
     *
     * @param name Test name
     * @param datafile Test data file
     * @param invert true to compute the upper value Q (default is lower P)
     * @param targetData Condition to identify the target data
     * @param useAsymp Condition to use the asymptotic approximation
     * @param expectImprovement true if the RMS error is expected to improve
     * @throws Exception If an error occurs reading the test files
     */
    private static void assertIgammaLargeX(String name, String datafile, boolean invert,
            DoubleDoubleBiPredicate targetData,
            DoubleDoubleBiPredicate useAsymp,
            boolean expectImprovement) throws Exception {
        final TestUtils.ErrorStatistics e1 = new TestUtils.ErrorStatistics();
        final TestUtils.ErrorStatistics e2 = new TestUtils.ErrorStatistics();

        final Policy pol = Policy.getDefault();
        final DoubleBinaryOperator without = (a, x) -> gammaIncompleteImp(a, x, invert, pol, NO_ASYM_APPROX);
        final DoubleBinaryOperator with = (a, x) -> gammaIncompleteImp(a, x, invert, pol, useAsymp);
        final int expectedField = invert ? 3 : 5;

        // Count how many times the asymptotic approximation was used
        int count = 0;

        final String functionName = datafile + " " + name + (invert ? " Q" : " P");

        // Set this to allow all data to be processed.
        // If set to negative any failures will be output to stdout (useful to debugging).
        final double tolerance = 1000;

        try (DataReader in = new DataReader(datafile)) {
            while (in.next()) {
                final double a = in.getDouble(0);
                final double x = in.getDouble(1);

                // Test if this is target data
                if (targetData.test(a, x)) {
                    final BigDecimal expected = in.getBigDecimal(expectedField);

                    // Ignore 0 or 1 results.
                    // This test is interested in values that can be computed.
                    final double value = expected.doubleValue();
                    if ((int) value != value) {
                        final double v1 = without.applyAsDouble(a, x);
                        final double v2 = with.applyAsDouble(a, x);
                        // Check if the asymptoptic approximation is used
                        if (useAsymp.test(a, x)) {
                            count++;
                        }

                        TestUtils.assertEquals(expected, v1, tolerance, e1::add,
                            () -> functionName + " " + a + ", x=" + x);
                        TestUtils.assertEquals(expected, v2, tolerance, e2::add,
                            () -> functionName + " asymp " + a + ", x=" + x);
                    }
                }
            }
        }
        // Use relaxed tolerances to allow the big data to pass.
        // This test is mainly interested in checking a switch to the asymptotic approximation
        // does not make the computation worse.
        final double maxTolerance = 600;
        final double rmsTolerance = 600;
        if (e1.size() != 0) {
            assertRms(TestError.of(functionName + "           ", maxTolerance, rmsTolerance), e1);
            assertRms(TestError.of(functionName + " asymp " +
                    String.format("%4d", count), maxTolerance, rmsTolerance), e2);
            if (expectImprovement) {
                // Better or equal. Equal applies if the asymptotic approximation was not used
                // or computed the same result.
                Assertions.assertTrue(e2.getRMS() <= e1.getRMS());
            } else {
                // Worse
                Assertions.assertTrue(e2.getRMS() > e1.getRMS());
            }
        }
    }

    /**
     * Test gamma Q with a value {@code a} so small that {@code tgamma(a) == inf}.
     * A direct port of the Boost code without changes will fail this test.
     */
    @ParameterizedTest
    @CsvSource({
        // This data needs verifying. Matlab, maxima and Boost all compute different values.
        // The values are the exponent of 2 to ensure tiny values are machine representable.
        // The following are from Boost using long double. This at least verifies that
        // the updated code computes close to the long double equivalent in the source
        // implementation.
        "-1074,-1074,3.67517082493672000135e-321",
        "-1074,-1073,3.67174622284245611609e-321",
        "-1074,-1072,3.66832162074819223109e-321",
        "-1074,-1000,3.4217502699611925034e-321",
        "-1074,-100,3.3960838512369590694e-322",
        "-1074,-10,3.13990203220802065461e-323",
        "-1074,-5,1.44243837956526236902e-323",
        "-1030,-1074,6.46542888972966038541e-308",
        "-1030,-1073,6.45940426601262169248e-308",
        "-1030,-1072,6.45337964229558300003e-308",
        "-1030,-1000,6.01960673466879712923e-308",
        "-1030,-100,5.97445389333973743599e-309",
        "-1030,-10,5.52377407118433787103e-310",
        "-1030,-5,2.53756443309180378013e-310",
    })
    void testGammaQTinyA(int ba, int bx, double q) {
        final double a = Math.scalb(1.0, ba);
        final double x = Math.scalb(1.0, bx);
        final double actualQ = BoostGamma.gammaQ(a, x);

        // Without changes to the Boost code that normalises by tgamma(a)
        // the result is zero. Check this does not occur.
        Assertions.assertNotEquals(0.0, actualQ);

        // Change tolerance for very small sub-normal result
        if (q < 1e-320) {
            // Within 1 ULP
            TestUtils.assertEquals(q, actualQ, 1);
        } else {
            // Sub-normal argument a.
            // The worst case here is 260 ULP. The relative error is OK.
            final double relError = 1e-13;
            Assertions.assertEquals(q, actualQ, q * relError);
        }
    }

    /**
     * Gamma function with Lanczos support.
     *
     * <p>This is the original Boost implementation here for reference. For {@code z}
     * in the range [-20, 20] the function is not as accurate as the NSWC
     * Library of Mathematics Subroutines.
     *
     * <p>This function and the tests that exercise it can be revisited if
     * improvements are made to the reference implementation.
     *
     * @param z Argument z
     * @return gamma value
     */
    static double tgammaOriginal(double z) {
        double result = 1;

        if (z <= 0) {
            if (Math.rint(z) == z) {
                // Pole error
                return Double.NaN;
            }
            if (z <= -20) {
                result = BoostGamma.tgamma(-z) * BoostGamma.sinpx(z);
                // Checks for overflow, sub-normal or underflow have been removed.
                return -Math.PI / result;
            }

            // shift z to > 1:
            // Q. Is this comment old? The shift is to > 0.
            // Spot tests in the Boost resources test z -> 0 due to a bug report.
            while (z < 0) {
                result /= z;
                z += 1;
            }
        }
        //
        // z is > 0
        //

        // Updated condition from z < MAX_FACTORIAL
        if ((Math.rint(z) == z) && (z <= MAX_FACTORIAL + 1)) {
            // Gamma(n) = (n-1)!
            result *= FACTORIAL[(int) z - 1];
        } else if (z < ROOT_EPSILON) {
            result *= 1 / z - EULER;
        } else {
            result *= Lanczos.lanczosSum(z);
            final double zgh = z + Lanczos.G - 0.5;
            final double lzgh = Math.log(zgh);
            if (z * lzgh > LOG_MAX_VALUE) {
                // we're going to overflow unless this is done with care:
                if (lzgh * z / 2 > LOG_MAX_VALUE) {
                    return Double.POSITIVE_INFINITY;
                }
                final double hp = Math.pow(zgh, (z / 2) - 0.25);
                result *= hp / Math.exp(zgh);
                // Check for overflow has been removed:
                // if (Double.MAX_VALUE / hp < result) ... overflow
                result *= hp;
            } else {
                result *= Math.pow(zgh, z - 0.5) / Math.exp(zgh);
            }
        }
        return result;
    }

    /**
     * Computes the value of \( \Gamma(x) \).
     *
     * <p>Based on the <em>NSWC Library of Mathematics Subroutines</em> double
     * precision implementation, {@code DGAMMA}.
     *
     * <p>This is a partial implementation of the Commons Numbers 1.0 implementation
     * for testing the {@link LanczosApproximation}.
     * The Lanczos support is valid for {@code x > 20}.
     *
     * @param x Argument ({@code x > 20})
     * @return \( \Gamma(x) \)
     */
    static double gammaOriginal(double x) {
        Assertions.assertTrue(x > LANCZOS_THRESHOLD, "Unsupported x: " + x);

        final double y = x + LanczosApproximation.g() + 0.5;
        // Constant 2.506... is sqrt(2 pi)
        return 2.506628274631000502 / x *
            Math.pow(y, x + 0.5) *
            Math.exp(-y) * LanczosApproximation.value(x);
    }

    /**
     * Compute incomplete gamma Q assuming large X without the asymptotic approximation.
     *
     * @param a Argument a
     * @param x Argument x
     * @return incomplete gamma Q
     */
    private static double igammaQLargeXNoAsym(double a, double x) {
        return gammaIncompleteImp(a, x, true, Policy.getDefault(), NO_ASYM_APPROX);
    }

    /**
     * Compute incomplete gamma Q assuming large X without the asymptotic approximation.
     *
     * @param a Argument a
     * @param x Argument x
     * @return incomplete gamma P
     */
    private static double igammaPLargeXNoAsym(double a, double x) {
        return gammaIncompleteImp(a, x, false, Policy.getDefault(), NO_ASYM_APPROX);
    }

    /**
     * Compute incomplete gamma Q assuming large X.
     * Uses the asymptotic approximation for large argument, see: https://dlmf.nist.gov/8.11#E2.
     *
     * @param a Argument a
     * @param x Argument x
     * @return incomplete gamma Q
     */
    private static double igammaQLargeXWithAsym(double a, double x) {
        // Force use of the asymptotic approximation
        return gammaIncompleteImp(a, x, true, Policy.getDefault(), ASYM_APPROX);
    }

    /**
     * Compute incomplete gamma P assuming large X.
     * Uses the asymptotic approximation for large argument, see: https://dlmf.nist.gov/8.11#E2.
     *
     * @param a Argument a
     * @param x Argument x
     * @return incomplete gamma P
     */
    private static double igammaPLargeXWithAsym(double a, double x) {
        // Force use of the asymptotic approximation
        return gammaIncompleteImp(a, x, false, Policy.getDefault(), ASYM_APPROX);
    }

    /**
     * Partial implementation of igamma for normalised and x > small threshold. This
     * is used to test the switch to the asymptotic approximation for large x. The
     * predicate to determine the switch is passed as an argument. Adapted from
     * {@code boost::math::detail::gamma_incomplete_imp}.
     *
     * @param a Argument a
     * @param x Argument x
     * @param invert true to compute the upper value Q (default is lower value P)
     * @param pol Function evaluation policy
     * @param useAsymApprox Predicate to determine when to use the asymptotic approximation
     * @return gamma value
     */
    private static double gammaIncompleteImp(double a, double x,
            boolean invert, Policy pol, DoubleDoubleBiPredicate useAsymApprox) {
        if (Double.isNaN(a) || Double.isNaN(x) || a <= 0 || x < 0) {
            return Double.NaN;
        }

        // Assumption for testing.
        // Do not support int or half-int a.
        // Do not support small x.
        // These evaluations methods have been removed.
        Assertions.assertFalse(isIntOrHalfInt(a, x), () -> "Invalid a : " + a);
        Assertions.assertFalse(x < 1.1, () -> "Invalid x : " + x);

        double result = 0;
        int evalMethod;

        // Configurable switch to the asymptotic approximation.
        // The branch is used in Boost when:
        // ((x > 1000) && ((a < x) || (Math.abs(a - 50) / x < 1)))
        if (useAsymApprox.test(a, x)) {
            // This case was added after Boost 1_68_0.
            // See: https://github.com/boostorg/math/issues/168
            // It is a source of error when a ~ z as the asymptotic approximation
            // sums terms t_n+1 = t_n * (a - n - 1) / z starting from t_0 = 1.
            // These terms are close to 1 when a ~ z and the sum has many terms
            // with reduced precision.

            // calculate Q via asymptotic approximation:
            invert = !invert;
            evalMethod = 7;
        } else {
            //
            // Begin by testing whether we're in the "bad" zone
            // where the result will be near 0.5 and the usual
            // series and continued fractions are slow to converge:
            //
            boolean useTemme = false;
            if (a > 20) {
                final double sigma = Math.abs((x - a) / a);
                if (a > 200) {
                    //
                    // This limit is chosen so that we use Temme's expansion
                    // only if the result would be larger than about 10^-6.
                    // Below that the regular series and continued fractions
                    // converge OK, and if we use Temme's method we get increasing
                    // errors from the dominant erfc term as it's (inexact) argument
                    // increases in magnitude.
                    //
                    if (20 / a > sigma * sigma) {
                        useTemme = true;
                    }
                } else {
                    // Note in this zone we can't use Temme's expansion for
                    // types longer than an 80-bit real:
                    // it would require too many terms in the polynomials.
                    if (sigma < 0.4) {
                        useTemme = true;
                    }
                }
            }
            if (useTemme) {
                evalMethod = 5;
            } else {
                //
                // Regular case where the result will not be too close to 0.5.
                //
                // Changeover here occurs at P ~ Q ~ 0.5
                // Note that series computation of P is about x2 faster than continued fraction
                // calculation of Q, so try and use the CF only when really necessary,
                // especially for small x.
                //
                if (x - (1 / (3 * x)) < a) {
                    evalMethod = 2;
                } else {
                    evalMethod = 4;
                    invert = !invert;
                }
            }
        }

        switch (evalMethod) {
        case 2:
            // Compute P:
            result = BoostGamma.regularisedGammaPrefix(a, x);
            if (result != 0) {
                //
                // If we're going to be inverting the result then we can
                // reduce the number of series evaluations by quite
                // a few iterations if we set an initial value for the
                // series sum based on what we'll end up subtracting it from
                // at the end.
                // Have to be careful though that this optimization doesn't
                // lead to spurious numeric overflow. Note that the
                // scary/expensive overflow checks below are more often
                // than not bypassed in practice for "sensible" input
                // values:
                //

                double initValue = 0;
                boolean optimisedInvert = false;
                if (invert) {
                    // Assume normalised=true
                    initValue = 1;
                    initValue /= result;
                    initValue *= -a;
                    optimisedInvert = true;
                }
                result *= BoostGamma.lowerGammaSeries(a, x, initValue, pol) / a;
                if (optimisedInvert) {
                    invert = false;
                    result = -result;
                }
            }
            break;
        case 4:
            // Compute Q:
            result = BoostGamma.regularisedGammaPrefix(a, x);
            if (result != 0) {
                result *= BoostGamma.upperGammaFraction(a, x, pol);
            }
            break;
        case 5:
            // Call 53-bit version
            result = BoostGamma.igammaTemmeLarge(a, x);
            if (x >= a) {
                invert = !invert;
            }
            break;
        case 7:
            // x is large,
            // Compute Q:
            result = BoostGamma.regularisedGammaPrefix(a, x);
            result /= x;
            if (result != 0) {
                result *= BoostGamma.incompleteTgammaLargeX(a, x, pol);
            }
            break;
        default:
            Assertions.fail(String.format("Unknown evaluation method: %s %s", a, x));
        }

        if (result > 1) {
            result = 1;
        }
        if (invert) {
            result = 1 - result;
        }

        return result;
    }

    /**
     * Test if a is small and an integer or half-integer.
     *
     * @param a Argument a
     * @param x Argument x
     * @return true if an integer or half integer
     */
    private static boolean isIntOrHalfInt(double a, double x) {
        boolean isInt;
        boolean isHalfInt;
        final boolean isSmallA = (a < 30) && (a <= x + 1) && (-x < LOG_MIN_VALUE);
        if (isSmallA) {
            final double fa = Math.floor(a);
            isInt = fa == a;
            isHalfInt = !isInt && (Math.abs(fa - a) == 0.5f);
        } else {
            isInt = isHalfInt = false;
        }
        return isInt || isHalfInt;
    }

    /**
     * Incomplete tgamma for large X.
     *
     * <p>Uses the default epsilon and iterations.
     *
     * @param a Argument a
     * @param x Argument x
     * @return incomplete tgamma
     */
    private static double incompleteTgammaLargeX(double a, double x) {
        return BoostGamma.incompleteTgammaLargeX(a, x, Policy.getDefault());
    }

    /**
     * Natural logarithm of the derivative of the regularised lower incomplete gamma.
     *
     * <pre>
     * a * log(x) - log(x) - x - lgamma(a)
     * </pre>
     *
     * <p>Always computes using logs.
     *
     * @param a Argument a
     * @param x Argument x
     * @return log p derivative
     */
    private static double logGammaPDerivative1(double a, double x) {
        if (a == 1) {
            // Special case
            return -x;
        }
        // No argument checks
        return a * Math.log(x) - x - BoostGamma.lgamma(a) - Math.log(x);

        //// Note:
        //// High precision. Only marginally lowers the error.
        //return new BigDecimal(a).subtract(BigDecimal.ONE)
        //    .multiply(new BigDecimal(Math.log(x)))
        //    .subtract(new BigDecimal(x))
        //    .subtract(new BigDecimal(BoostGamma.lgamma(a))).doubleValue();
    }

    /**
     * Natural logarithm of the derivative of the regularised lower incomplete gamma.
     *
     * <pre>
     * a * log(x) - log(x) - x - lgamma(a)
     * </pre>
     *
     * <p>Computed using the logarithm of the gamma p derivative if this is finite; else
     * uses logs.
     *
     * @param a Argument a
     * @param x Argument x
     * @return log p derivative
     */
    private static double logGammaPDerivative2(double a, double x) {
        //
        // Usual error checks first:
        //
        if (Double.isNaN(a) || Double.isNaN(x) || a <= 0 || x < 0) {
            return Double.NaN;
        }
        //
        // Now special cases:
        //
        if (x == 0) {
            return (a > 1) ? Double.NEGATIVE_INFINITY : (a == 1) ? 0 : Double.POSITIVE_INFINITY;
        }
        if (a == 1) {
            return -x;
        }
        //
        // Normal case:
        //

        // Note: This may be computing log(exp(result)) for a round-trip of 'result'.
        // Variations were created that:
        // 1. Called BoostGamma.regularisedGammaPrefix(a, x) and processed the result.
        //    This avoids a log(exp(result)) round-trip.
        // 2. Repeated the logic of BoostGamma.regularisedGammaPrefix(a, x) and directly
        //    returned the log result.
        // The variations may be faster but are no more accurate on the current test data.

        final double v = BoostGamma.gammaPDerivative(a, x);
        return Double.isFinite(v) && v != 0 ?
            Math.log(v) :
            logGammaPDerivative1(a, x);
    }

    @ParameterizedTest
    @CsvSource({
        "NaN, 1, NaN",
        "1, NaN, NaN",
        // Allow calling with infinity
        "Infinity, 0, 1",
        "Infinity, 1, 0",
        "Infinity, -1, Infinity",
        // z <= 0 || z+delta <= 0
        // Pole errors
        "-1, 0.5, NaN",
        "-1.5, 0.5, NaN",
        "0.5, -1.5, NaN",
    })
    void testGammaDeltaRatioEdgeCases(double a, double delta, double expected) {
        Assertions.assertEquals(expected, BoostGamma.tgammaDeltaRatio(a, delta));
    }

    @ParameterizedTest
    @CsvSource({
        "0, 1, NaN",
        "-1, 1, NaN",
        "Infinity, 1, NaN",
        "NaN, 1, NaN",
        "1, 0, NaN",
        "1, -1, NaN",
        "1, Infinity, NaN",
        "1, NaN, NaN",
        // underflow
        "0.5, 500, 0",
        // overflow
        "500, 0.5, Infinity",
    })
    void testGammaRatioEdgeCases(double a, double b, double expected) {
        Assertions.assertEquals(expected, BoostGamma.tgammaRatio(a, b));
    }

    /**
     * tgamma ratio spot tests extracted from
     * {@code boost/libs/math/test/test_tgamma_ratio.hpp}.
     */
    @Test
    void testGammaRatioSpotTests() {
        final int tol = 20;
        assertClose(BoostGamma::tgammaRatio, Math.scalb(1.0, -500), 180.25, 8.0113754557649679470816892372669519037339812035512e-178, 3 * tol);
        assertClose(BoostGamma::tgammaRatio, Math.scalb(1.0, -525), 192.25, 1.5966560279353205461166489184101261541784867035063e-197, 3 * tol);
        assertClose(BoostGamma::tgammaRatio, 182.25, Math.scalb(1.0, -500), 4.077990437521002194346763299159975185747917450788e+181, 3 * tol);
        assertClose(BoostGamma::tgammaRatio, 193.25, Math.scalb(1.0, -525), 1.2040790040958522422697601672703926839178050326148e+199, 3 * tol);
        assertClose(BoostGamma::tgammaRatio, 193.25, 194.75, 0.00037151765099653237632823607820104961270831942138159, 3 * tol);
        //
        // Some bug cases from Rocco Romeo:
        //
        assertClose(BoostGamma::tgammaRatio, Math.scalb(1.0, -1020), 100, 1.20390418056093374068585549133304106854441830616070800417660e151, tol);
        assertClose(BoostGamma::tgammaRatio, Math.scalb(1.0, -1020), 150, 2.94980580122226729924781231239336413648584663386992050529324e46, tol);
        assertClose(BoostGamma::tgammaRatio, Math.scalb(1.0, -1020), 180, 1.00669209319561468911303652019446665496398881230516805140750e-20, tol);
        assertClose(BoostGamma::tgammaRatio, Math.scalb(1.0, -1020), 220, 1.08230263539550701700187215488533416834407799907721731317227e-112, tol);
        assertClose(BoostGamma::tgammaRatio, Math.scalb(1.0, -1020), 260, 7.62689807594728483940172477902929825624752380292252137809206e-208, tol);
        assertClose(BoostGamma::tgammaRatio, Math.scalb(1.0, -1020), 290, 5.40206998243175672775582485422795773284966068149812072521290e-281, tol);
        assertClose(BoostGamma::tgammaDeltaRatio, Math.scalb(1.0, -1020), Math.scalb(1.0, -1020), 2, tol);
        // This is denorm_min at double precision:
        assertClose(BoostGamma::tgammaRatio, Math.scalb(1.0, -1074), 200, 5.13282785052571536804189023927976812551830809667482691717029e-50, tol * 50);
        assertClose(BoostGamma::tgammaRatio, 200, Math.scalb(1.0, -1074), 1.94824379293682687942882944294875087145333536754699303593931e49, tol * 10);
        assertClose(BoostGamma::tgammaDeltaRatio, Math.scalb(1.0, -1074), 200, 5.13282785052571536804189023927976812551830809667482691717029e-50, tol * 10);
        assertClose(BoostGamma::tgammaDeltaRatio, 200, Math.scalb(1.0, -1074), 1, tol);

        // Test simple negative handling
        for (final double z : new double[] {-0.5, -15.5, -25.5}) {
            for (final double delta : new double[] {0.25, -0.25}) {
                Assertions.assertEquals(BoostGamma.tgamma(z) / BoostGamma.tgamma(z + delta), BoostGamma.tgammaDeltaRatio(z, delta));
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = BiTestCase.class, mode = Mode.MATCH_ANY, names = {"TGAMMA_DELTA_RATIO.*"})
    void testGammaDeltaRatio(BiTestCase tc) {
        assertFunction(tc);
    }

    @ParameterizedTest
    @EnumSource(value = BiTestCase.class, mode = Mode.MATCH_ANY, names = {"TGAMMA_RATIO.*"})
    void testGammaRatio(BiTestCase tc) {
        assertFunction(tc);
    }

    /**
     * Helper function for ratio of gamma functions to negate the delta argument.
     *
     * <p>\[ tgamma_ratio(z, delta) = \frac{\Gamma(z)}{\Gamma(z - delta)} \]
     *
     * @param z Argument z
     * @param delta The difference
     * @return gamma ratio
     */
    private static double tgammaDeltaRatioNegative(double z, double delta) {
        return BoostGamma.tgammaDeltaRatio(z, -delta);
    }

    /**
     * Assert the function is close to the expected value.
     *
     * @param fun Function
     * @param x Input value
     * @param expected Expected value
     * @param tolerance the tolerance
     */
    private static void assertClose(DoubleUnaryOperator fun, double x, double expected, int tolerance) {
        final double actual = fun.applyAsDouble(x);
        TestUtils.assertEquals(expected, actual, tolerance, null, () -> Double.toString(x));
    }

    /**
     * Assert the function is close to the expected value.
     *
     * @param fun Function
     * @param x Input value
     * @param y Input value
     * @param expected Expected value
     * @param tolerance the tolerance
     */
    private static void assertClose(DoubleBinaryOperator fun, double x, double y, double expected, int tolerance) {
        final double actual = fun.applyAsDouble(x, y);
        TestUtils.assertEquals(expected, actual, tolerance, null, () -> x + ", " + y);
    }

    /**
     * Assert the function is equal to the expected value.
     *
     * @param fun Function
     * @param x Input value
     * @param y Input value
     * @param expected Expected value
     */
    private static void assertEquals(DoubleBinaryOperator fun, double x, double y, double expected) {
        final double actual = fun.applyAsDouble(x, y);
        Assertions.assertEquals(expected, actual, () -> x + ", " + y);
    }

    /**
     * Assert the function using extended precision.
     *
     * @param tc Test case
     */
    private static void assertFunction(TestCase tc) {
        final TestUtils.ErrorStatistics stats = new TestUtils.ErrorStatistics();
        try (DataReader in = new DataReader(tc.getFilename())) {
            while (in.next()) {
                try {
                    final double x = in.getDouble(0);
                    final BigDecimal expected = in.getBigDecimal(tc.getExpectedField());
                    final double actual = tc.getFunction().applyAsDouble(x);
                    TestUtils.assertEquals(expected, actual, tc.getTolerance(), stats::add,
                        () -> tc + " x=" + x);
                } catch (final NumberFormatException ex) {
                    Assertions.fail("Failed to load data: " + Arrays.toString(in.getFields()), ex);
                }
            }
        } catch (final IOException ex) {
            Assertions.fail("Failed to load data: " + tc.getFilename(), ex);
        }

        assertRms(tc, stats);
    }

    /**
     * Assert the function using extended precision.
     *
     * @param tc Test case
     */
    private static void assertFunction(BiTestCase tc) {
        final TestUtils.ErrorStatistics stats = new TestUtils.ErrorStatistics();
        try (DataReader in = new DataReader(tc.getFilename())) {
            while (in.next()) {
                try {
                    final double x = in.getDouble(0);
                    final double y = in.getDouble(1);
                    final BigDecimal expected = in.getBigDecimal(tc.getExpectedField());
                    final double actual = tc.getFunction().applyAsDouble(x, y);
                    TestUtils.assertEquals(expected, actual, tc.getTolerance(), stats::add,
                        () -> tc + " x=" + x + ", y=" + y);
                } catch (final NumberFormatException ex) {
                    Assertions.fail("Failed to load data: " + Arrays.toString(in.getFields()), ex);
                }
            }
        } catch (final IOException ex) {
            Assertions.fail("Failed to load data: " + tc.getFilename(), ex);
        }

        assertRms(tc, stats);
    }

    /**
     * Assert the Root Mean Square (RMS) error of the function is below the allowed
     * maximum for the specified TestError.
     *
     * @param te Test error
     * @param stats Error statistics
     */
    private static void assertRms(TestError te, TestUtils.ErrorStatistics stats) {
        final double rms = stats.getRMS();
        //debugRms(te.toString(), stats.getMaxAbs(), rms, stats.getMean(), stats.size());
        Assertions.assertTrue(rms <= te.getRmsTolerance(),
            () -> String.format("%s RMS %s < %s", te, rms, te.getRmsTolerance()));
    }

    /**
     * Output the maximum and RMS ulp for the named test. Used for reporting the
     * errors and setting appropriate test tolerances. This is relevant across
     * different JDK implementations where the java.util.Math functions used in
     * BoostGamma may compute to different accuracy.
     *
     * @param name Test name
     * @param maxAbsUlp Maximum |ulp|
     * @param rmsUlp RMS ulp
     * @param meanUlp Mean ulp
     * @param size Number of measurements
     */
    private static void debugRms(String name, double maxAbsUlp, double rmsUlp, double meanUlp, int size) {
        // CHECKSTYLE: stop regexp
        System.out.printf("%-35s   max %10.6g   RMS %10.6g   mean %14.6g  n %4d%n",
            name, maxAbsUlp, rmsUlp, meanUlp, size);
    }
}
