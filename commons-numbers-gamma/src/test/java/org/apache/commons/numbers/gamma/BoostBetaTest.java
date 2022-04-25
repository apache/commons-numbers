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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

/**
 * Tests for {@link BoostBeta}.
 *
 * <p>Note: Some resource data files used in these tests have been extracted
 * from the Boost test files for the beta functions.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BoostBetaTest {
    /**
     * Represents an operation upon three {@code double}-valued operands and producing a
     * {@code double}-valued result.
     */
    interface DoubleTernaryOperator {
        /**
         * Applies this operator to the given operands.
         *
         * @param x the first operand
         * @param y the second operand
         * @param z the third operand
         * @return the operator result
         */
        double applyAsDouble(double x, double y, double z);
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
    }

    /**
     * Define the test cases for each resource file for two argument functions.
     * This encapsulates the function to test, the expected maximum and RMS error, and
     * the resource file containing the data.
     *
     * <h2>Note on accuracy</h2>
     *
     * <p>The Boost functions use the default policy of internal promotion
     * of double to long double if it offers more precision. Java does not
     * support long double computation. Tolerances have been set to allow tests to
     * pass. Spot checks on larger errors have been verified against the reference
     * implementation compiled with promotion of double <strong>disabled</strong>.
     *
     * @see <a href="https://www.boost.org/doc/libs/1_77_0/libs/math/doc/html/math_toolkit/relative_error.html">Relative error</a>
     * @see <a href="https://www.boost.org/doc/libs/1_77_0/libs/math/doc/html/math_toolkit/pol_tutorial/policy_tut_defaults.html">Policy defaults</a>
     */
    private enum BiTestCase implements TestError {
        // beta(a, b)
        // Note that the worst errors occur when a or b are large, and that
        // when this is the case the result is very close to zero, so absolute
        // errors will be very small.
        /** beta Boost small data. */
        BETA_SMALL(BoostBeta::beta, "beta_small_data.csv", 4, 1.7),
        /** beta Boost medium data. */
        BETA_MED(BoostBeta::beta, "beta_med_data.csv", 200, 35),
        /** beta Boost divergent data. */
        BETA_EXP(BoostBeta::beta, "beta_exp_data.csv", 17, 3.6),
        // LogBeta based implementation is worse
        /** beta Boost small data. */
        BETA1_SMALL(BoostBetaTest::beta, "beta_small_data.csv", 110, 28),
        /** beta Boost medium data. */
        BETA1_MED(BoostBetaTest::beta, "beta_med_data.csv", 280, 46),
        /** beta Boost divergent data. */
        BETA1_EXP(BoostBetaTest::beta, "beta_exp_data.csv", 28, 4.5),
        /** binomial coefficient Boost small argument data. */
        BINOMIAL_SMALL(BoostBetaTest::binomialCoefficient, "binomial_small_data.csv", -2, 0.5),
        /** binomial coefficient Boost large argument data. */
        BINOMIAL_LARGE(BoostBetaTest::binomialCoefficient, "binomial_large_data.csv", 5, 1.1),
        /** binomial coefficient extra large argument data. */
        BINOMIAL_XLARGE(BoostBetaTest::binomialCoefficient, "binomial_extra_large_data.csv", 9, 2),
        /** binomial coefficient huge argument data. */
        BINOMIAL_HUGE(BoostBetaTest::binomialCoefficient, "binomial_huge_data.csv", 9, 2),
        // Using the beta function is worse
        /** binomial coefficient Boost large argument data computed using the beta function. */
        BINOMIAL1_LARGE(BoostBetaTest::binomialCoefficient1, "binomial_large_data.csv", 31, 9),
        /** binomial coefficient huge argument data computed using the beta function. */
        BINOMIAL1_HUGE(BoostBetaTest::binomialCoefficient1, "binomial_huge_data.csv", 70, 19);

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

    /**
     * Define the test cases for each resource file for three argument functions.
     * This encapsulates the function to test, the expected maximum and RMS error, and
     * the resource file containing the data.
     */
    private enum TriTestCase implements TestError {
        /** ibeta derivative Boost small integer data. */
        IBETA_DERIV_SMALL_INT(BoostBeta::ibetaDerivative, "ibeta_derivative_small_int_data.csv", 60, 13),
        /** ibeta derivative Boost small data. */
        IBETA_DERIV_SMALL(BoostBeta::ibetaDerivative, "ibeta_derivative_small_data.csv", 22, 4),
        /** ibeta derivative Boost medium data. */
        IBETA_DERIV_MED(BoostBeta::ibetaDerivative, "ibeta_derivative_med_data.csv", 150, 33),
        /** ibeta derivative Boost large and diverse data. */
        IBETA_DERIV_LARGE(BoostBeta::ibetaDerivative, "ibeta_derivative_large_data.csv", 3900, 260),
        // LogGamma based implementation is worse
        /** ibeta derivative Boost small integer data. */
        IBETA_DERIV1_SMALL_INT(BoostBetaTest::ibetaDerivative1, "ibeta_derivative_small_int_data.csv", 220, 55),
        /** ibeta derivative Boost small data. */
        IBETA_DERIV1_SMALL(BoostBetaTest::ibetaDerivative1, "ibeta_derivative_small_data.csv", 75, 10.5),
        /** ibeta derivative Boost medium data. */
        IBETA_DERIV1_MED(BoostBetaTest::ibetaDerivative1, "ibeta_derivative_med_data.csv", 1500, 300),
        /** ibeta derivative Boost large and diverse data. */
        IBETA_DERIV1_LARGE(BoostBetaTest::ibetaDerivative1, "ibeta_derivative_large_data.csv", 9e7, 250000),
        // LogBeta based implementation is worse
        /** ibeta derivative Boost small integer data. */
        IBETA_DERIV2_SMALL_INT(BoostBetaTest::ibetaDerivative2, "ibeta_derivative_small_int_data.csv", 180, 31),
        /** ibeta derivative Boost small data. */
        IBETA_DERIV2_SMALL(BoostBetaTest::ibetaDerivative2, "ibeta_derivative_small_data.csv", 75, 8.5),
        /** ibeta derivative Boost medium data. */
        IBETA_DERIV2_MED(BoostBetaTest::ibetaDerivative2, "ibeta_derivative_med_data.csv", 500, 85),
        /** ibeta derivative Boost large and diverse data. */
        IBETA_DERIV2_LARGE(BoostBetaTest::ibetaDerivative2, "ibeta_derivative_large_data.csv", 28000, 1200),

        /** ibeta Boost small integer data. */
        IBETA_SMALL_INT(BoostBeta::beta, "ibeta_small_int_data.csv", 48, 11),
        /** ibeta Boost small data. */
        IBETA_SMALL(BoostBeta::beta, "ibeta_small_data.csv", 17, 3.3),
        /** ibeta Boost medium data. */
        IBETA_MED(BoostBeta::beta, "ibeta_med_data.csv", 190, 20),
        /** ibeta Boost large and diverse data. */
        IBETA_LARGE(BoostBeta::beta, "ibeta_large_data.csv", 1300, 50),
        /** ibetac Boost small integer data. */
        IBETAC_SMALL_INT(BoostBeta::betac, "ibeta_small_int_data.csv", 4, 57, 11),
        /** ibetac Boost small data. */
        IBETAC_SMALL(BoostBeta::betac, "ibeta_small_data.csv", 4, 14, 3.2),
        /** ibetac Boost medium data. */
        IBETAC_MED(BoostBeta::betac, "ibeta_med_data.csv", 4, 130, 24),
        /** ibetac Boost large and diverse data. */
        IBETAC_LARGE(BoostBeta::betac, "ibeta_large_data.csv", 4, 7000, 220),
        /** regularised ibeta Boost small integer data. */
        RBETA_SMALL_INT(BoostBeta::ibeta, "ibeta_small_int_data.csv", 5, 7.5, 1.2),
        /** regularised ibeta Boost small data. */
        RBETA_SMALL(BoostBeta::ibeta, "ibeta_small_data.csv", 5, 14, 3.3),
        /** regularised ibeta Boost medium data. */
        RBETA_MED(BoostBeta::ibeta, "ibeta_med_data.csv", 5, 200, 28),
        /** regularised ibeta Boost large and diverse data. */
        RBETA_LARGE(BoostBeta::ibeta, "ibeta_large_data.csv", 5, 2400, 100),
        /** regularised ibetac Boost small integer data. */
        RBETAC_SMALL_INT(BoostBeta::ibetac, "ibeta_small_int_data.csv", 6, 8, 1.6),
        /** regularised ibetac Boost small data. */
        RBETAC_SMALL(BoostBeta::ibetac, "ibeta_small_data.csv", 6, 11, 2.75),
        /** regularised ibetac Boost medium data. */
        RBETAC_MED(BoostBeta::ibetac, "ibeta_med_data.csv", 6, 105, 23),
        /** regularised ibetac Boost large and diverse data. */
        RBETAC_LARGE(BoostBeta::ibetac, "ibeta_large_data.csv", 6, 4000, 180),

        // Classic continued fraction representation is:
        // - worse on small data
        // - comparable (or better) on medium data
        // - much worse on large data
        /** regularised ibeta Boost small data using the classic continued fraction evaluation. */
        RBETA1_SMALL(BoostBetaTest::ibeta, "ibeta_small_data.csv", 5, 45, 5),
        /** regularised ibeta Boost small data using the classic continued fraction evaluation. */
        RBETA1_MED(BoostBetaTest::ibeta, "ibeta_med_data.csv", 5, 200, 26),
        /** regularised ibeta Boost large and diverse data. */
        RBETA1_LARGE(BoostBetaTest::ibeta, "ibeta_large_data.csv", 5, 150000, 7500),
        /** regularised ibeta Boost small data using the classic continued fraction evaluation. */
        RBETAC1_SMALL(BoostBetaTest::ibetac, "ibeta_small_data.csv", 6, 30, 4.5),
        /** regularised ibeta Boost small data using the classic continued fraction evaluation. */
        RBETAC1_MED(BoostBetaTest::ibetac, "ibeta_med_data.csv", 6, 100, 22),
        /** regularised ibetac Boost large and diverse data. */
        RBETAC1_LARGE(BoostBetaTest::ibetac, "ibeta_large_data.csv", 6, 370000, 11000);

        /** The function. */
        private final DoubleTernaryOperator fun;

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
        TriTestCase(DoubleTernaryOperator fun, String filename, double maxUlp, double rmsUlp) {
            this(fun, filename, 3, maxUlp, rmsUlp);
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
        TriTestCase(DoubleTernaryOperator fun, String filename, int expected, double maxUlp, double rmsUlp) {
            this.fun = fun;
            this.filename = filename;
            this.expected = expected;
            this.maxUlp = maxUlp;
            this.rmsUlp = rmsUlp;
        }

        /**
         * @return function to test
         */
        DoubleTernaryOperator getFunction() {
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
        // Argument a > 0
        "NaN, 1, NaN",
        "0, 1, NaN",
        "-1, 1, NaN",
        // Argument b > 0
        "1, NaN, NaN",
        "1, 0, NaN",
        "1, -1, NaN",
    })
    void testBetaEdgeCases(double a, double b, double expected) {
        Assertions.assertEquals(expected, BoostBeta.beta(a, b), "beta");
    }

    /**
     * beta spot tests extracted from
     * {@code boost/libs/math/test/test_beta.hpp}.
     */
    @Test
    void testBetaSpotTests() {
        final int tolerance = 20;
        // small = epsilon / 1024
        final double small = Math.ulp(1.0) / 1024;
        assertClose(BoostBeta::beta, 1, 1, 1, 0);
        assertClose(BoostBeta::beta, 1, 4, 0.25, 0);
        assertClose(BoostBeta::beta, 4, 1, 0.25, 0);
        assertClose(BoostBeta::beta, small, 4, 1 / small, 0);
        assertClose(BoostBeta::beta, 4, small, 1 / small, 0);
        assertClose(BoostBeta::beta, 4, 20, 0.00002823263692828910220214568040654997176736, tolerance);
        assertClose(BoostBeta::beta, 0.0125, 0.000023, 43558.24045647538375006349016083320744662, tolerance * 2);

        // Additional tests

        // a + b > 1e10
        assertClose(BoostBeta::beta, 1e6, 1e6, 0, 0);
        assertClose(BoostBeta::beta, 1e10, 100, 0, 0);
        assertClose(BoostBeta::beta, 1e11, 1, 1e-11, 0);
        assertClose(BoostBeta::beta, 1e11, 2, 1 / (1e11 * (1e11 + 1)), 5);

        // a + b == a; b > epsilon
        assertClose(BoostBeta::beta, 5, 0x1.0p-51, 2.2517998136852459167e15, 5);
        // a + b == b; a > epsilon
        assertClose(BoostBeta::beta, 0x1.0p-50, 11, 1.125899906842621071e15, 5);
    }

    @ParameterizedTest
    @EnumSource(value = BiTestCase.class, mode = Mode.MATCH_ANY, names = {"BETA_.*", "BETA1_.*"})
    void testBeta(BiTestCase tc) {
        assertFunction(tc);
    }

    @ParameterizedTest
    @CsvSource({
        // k=0 or k=n
        "1, 0, 1",
        "42, 0, 1",
        "1, 1, 1",
        "42, 42, 1",
        // k=1 or k=n-1
        "6, 1, 6",
        "42, 1, 42",
        "6, 5, 6",
        "42, 41, 42",
    })
    void testBinomialCoefficientEdgeCases(int n, int k, double expected) {
        Assertions.assertEquals(expected, BoostBeta.binomialCoefficient(n, k));
    }

    /**
     * Binomial coefficient spot tests extracted from
     * {@code boost/libs/math/test/test_binomial_coeff.cpp}.
     */
    @Test
    void testBinomialCoefficientSpotTests() {
        Assertions.assertEquals(1, BoostBeta.binomialCoefficient(20, 0));
        Assertions.assertEquals(20, BoostBeta.binomialCoefficient(20, 1));
        Assertions.assertEquals(190, BoostBeta.binomialCoefficient(20, 2));
        Assertions.assertEquals(1140, BoostBeta.binomialCoefficient(20, 3));
        Assertions.assertEquals(1, BoostBeta.binomialCoefficient(20, 20));
        Assertions.assertEquals(20, BoostBeta.binomialCoefficient(20, 19));
        Assertions.assertEquals(190, BoostBeta.binomialCoefficient(20, 18));
        Assertions.assertEquals(1140, BoostBeta.binomialCoefficient(20, 17));
        Assertions.assertEquals(184756, BoostBeta.binomialCoefficient(20, 10));

        // Requires tolerance 50 if using the beta function approximation
        // in <boost/math/special_functions/binomial.hpp>.
        // Lower if using BinomialCoefficientDouble (see below).
        int tolerance = 2;

        // 0 ulp
        assertClose(BoostBetaTest::binomialCoefficient, 100, 5, 7.528752e7, tolerance);
        // 1 ulp
        assertClose(BoostBetaTest::binomialCoefficient, 100, 81, 1.323415729392122674e20, tolerance);

        // 0 ulp
        assertClose(BoostBetaTest::binomialCoefficient, 300, 3, 4.45510e6, tolerance);
        // 0 ulp
        assertClose(BoostBetaTest::binomialCoefficient, 300, 7, 4.04385595614e13, tolerance);
        // -3 ulp / 1 ulp
        assertClose(BoostBetaTest::binomialCoefficient, 300, 290, 1.39832023324170177e18, tolerance);
        // 36 ulp / 2 ulp
        assertClose(BoostBetaTest::binomialCoefficient, 300, 275, 1.953265141442868389822364184842211512e36, tolerance);

        // Additional tests

        // Overflow if computed using
        // result = 1
        // for i in [1, m]:
        //   result *= n - m + i
        //   result /= i
        // The final term must use divide by i first.
        tolerance = 5;
        assertClose(BoostBetaTest::binomialCoefficient, 1786388282, 38, 7.187239013254065384599502085053593e306, tolerance);
        assertClose(BoostBetaTest::binomialCoefficient, 1914878305, 38, 100.6570419073661447979173868523364e306, tolerance);
        assertClose(BoostBetaTest::binomialCoefficient, 1179067476, 39, 30.22890249420109200962786203300876e306, tolerance);
        // n == 2^31 - 1
        assertClose(BoostBetaTest::binomialCoefficient, 2147483647, 37, 1.388890512412231479281222156415993e302, tolerance);
    }

    @ParameterizedTest
    @EnumSource(value = BiTestCase.class, mode = Mode.MATCH_ANY, names = {"BINOM.*"})
    void testBinomialCoefficient(BiTestCase tc) {
        assertFunction(tc);
    }

    /**
     * Demonstrate the binomial coefficient is better without the beta function at large k.
     */
    @ParameterizedTest
    @CsvSource({
        "500, 225, 9.5909622148251792594e147",
        "500, 250, 1.1674431578827768292e149",
        "600, 300, 1.3510794199619426851e179",
        "700, 350, 1.5857433585316795488e209",
        "800, 400, 1.8804244186835312701e239",
        "1000, 500, 2.7028824094543656952e299",
        "100000, 50, 3.2479111644852887358e185",
        "100000, 70, 8.1490000781382598363e249",
        "100000, 80, 1.3537701492763430639e281",
        "100000, 85, 3.4252195975122556458e296",
        "1030, 515, 2.8596413729978081638e308",
    })
    void testBinomialCoefficientLargeK(int n, int k, double nCk) {
        final double bc1 = BoostBeta.binomialCoefficient(n, k);
        final double bc2 = BoostBetaTest.binomialCoefficient1(n, k);
        assertCloser("nCk", nCk, bc1, bc2);
    }

    @ParameterizedTest
    @CsvSource({
        // No protection against overflow when k ~ n/2, only for k << n
        "1040, 450, 2.3101613255412135615e307",
        "1029, 514, 1.4298206864989040819e308",
    })
    void testBinomialCoefficientOverflowAtLargeK(int n, int k, double nCk) {
        // Note: This could be made to work correctly but is not required for the
        // current use cases.
        Assertions.assertEquals(Double.POSITIVE_INFINITY, BoostBeta.binomialCoefficient(n, k));

        // Using the beta function is not very accurate but it does work
        Assertions.assertEquals(nCk, BoostBetaTest.binomialCoefficient1(n, k), nCk * 1e-12);
    }

    @ParameterizedTest
    @CsvSource({
        // Argument a > 0
        "NaN, 1, 0.5, NaN",
        "0, 1, 0.5, NaN",
        "-1, 1, 0.5, NaN",
        // Argument b > 0
        "1, NaN, 0.5, NaN",
        "1, 0, 0.5, NaN",
        "1, -1, 0.5, NaN",
        // Argument x in [0, 1]
        "1, 1, NaN, NaN",
        "1, 1, -1, NaN",
        "1, 1, 2, NaN",
    })
    void testIBetaEdgeCases(double a, double b, double x, double expected) {
        // Note: Despite being named 'beta' this function is the full incomplete beta
        Assertions.assertEquals(expected, BoostBeta.beta(a, b, x), "beta");
        Assertions.assertEquals(expected, BoostBeta.betac(a, b, x), "betac");
        final Policy pol = Policy.getDefault();
        Assertions.assertEquals(expected, BoostBeta.beta(a, b, x, pol), "beta");
        Assertions.assertEquals(expected, BoostBeta.betac(a, b, x, pol), "betac");

        // The ibetaDerivative has the same invalid domain for a, b and x
        // as beta. This is despite the ibeta being defined when a or b is 0.
        // In that case the value x is ignored in ibeta (it is a constant
        // function) and there is no derivative.
        Assertions.assertEquals(expected, BoostBeta.ibetaDerivative(a, b, x), "ibetaDerivative");
    }

    @ParameterizedTest
    @CsvSource({
        // Argument a >= 0
        "NaN, 1, 0.5, NaN",
        "-1, 1, 0.5, NaN",
        // Argument b >= 0
        "1, NaN, 0.5, NaN",
        "1, -1, 0.5, NaN",
        // a and b cannot both be 0
        "0, 0, 0.5, NaN",
        // a or b are allowed to be zero, x is ignored
        "0, 1, 0.25, 1",
        "0, 1, 0.5, 1",
        "0, 2, 0.5, 1",
        "1, 0, 0.25, 0",
        "1, 0, 0.5, 0",
        "2, 0, 0.5, 0",
        // Argument x in [0, 1]
        "1, 1, NaN, NaN",
        "1, 1, -1, NaN",
        "1, 1, 2, NaN",
    })
    void testRegularisedIBetaEdgeCases(double a, double b, double x, double expected) {
        Assertions.assertEquals(expected, BoostBeta.ibeta(a, b, x), "ibeta");
        Assertions.assertEquals(1 - expected, BoostBeta.ibetac(a, b, x), "ibetac");
        final Policy pol = Policy.getDefault();
        Assertions.assertEquals(expected, BoostBeta.ibeta(a, b, x, pol), "ibeta");
        Assertions.assertEquals(1 - expected, BoostBeta.ibetac(a, b, x, pol), "ibetac");
    }

    /**
     * ibeta derivative spot tests extracted from
     * {@code boost/libs/math/test/test_ibeta_derivative.hpp}.
     */
    @Test
    void testIBetaDerivativeSpotTests() {
        final int tolerance = 400;
        assertClose(BoostBeta::ibetaDerivative, 2, 4, Math.scalb(1.0, -557), 4.23957586190238472641508753637420672781472122471791800210e-167, tolerance * 4);
        assertClose(BoostBeta::ibetaDerivative, 2, 4.5, Math.scalb(1.0, -557), 5.24647512910420109893867082626308082567071751558842352760e-167, tolerance * 4);

        // Additions
        for (final double p : new double[] {3, 13, 42}) {
            // x==0
            assertClose(BoostBeta::ibetaDerivative, 2, p, 0, 0.0, 0);
            assertClose(BoostBeta::ibetaDerivative, 1, p, 0, p, 0);
            assertClose(BoostBeta::ibetaDerivative, 0.5, p, 0, Double.POSITIVE_INFINITY, 0);
            // x==1
            assertClose(BoostBeta::ibetaDerivative, p, 2, 1, 0.0, 0);
            assertClose(BoostBeta::ibetaDerivative, p, 1, 1, p, 0);
            assertClose(BoostBeta::ibetaDerivative, p, 0.5, 1, Double.POSITIVE_INFINITY, 0);
        }
    }

    @ParameterizedTest
    @EnumSource(value = TriTestCase.class, mode = Mode.MATCH_ANY, names = {"IBETA_DERIV.*"})
    void testIBetaDerivative(TriTestCase tc) {
        assertFunction(tc);
    }

    /**
     * ibeta spot tests extracted from
     * {@code boost/libs/math/test/test_ibeta.hpp}.
     */
    @Test
    void testIBetaSpotTests() {
        int tolerance = 30;

        // Spot values are from http://functions.wolfram.com/webMathematica/FunctionEvaluation.jsp?name=BetaRegularized
        // using precision of 50 decimal digits.
        assertClose(BoostBeta::ibeta, 159.0 / 10000, 1184.0 / 1000000000L, 6917.0 / 10000,
            0.000075393541456247525676062058821484095548666733251733, tolerance);
        assertClose(BoostBeta::ibeta, 4243.0 / 100, 3001.0 / 10000, 9157.0 / 10000,
            0.0028387319012616013434124297160711532419664289474798, tolerance);
        assertClose(BoostBeta::ibeta, 9713.0 / 1000, 9940.0 / 100, 8391.0 / 100000,
            0.46116895440368248909937863372410093344466819447476, tolerance * 2);
        // extra tolerance needed on linux X86EM64
        assertClose(BoostBeta::ibeta, 72.5, 1.125, 0.75, 1.3423066982487051710597194786268004978931316494920e-9,
            tolerance * 3);
        assertClose(BoostBeta::ibeta, 4985.0 / 1000, 1066.0 / 1000, 7599.0 / 10000,
            0.27533431334486812211032939156910472371928659321347, tolerance);
        assertClose(BoostBeta::ibeta, 6813.0 / 1000, 1056.0 / 1000, 1741.0 / 10000,
            7.6736128722762245852815040810349072461658078840945e-6, tolerance);
        assertClose(BoostBeta::ibeta, 4898.0 / 10000, 2251.0 / 10000, 2003.0 / 10000,
            0.17089223868046209692215231702890838878342349377008, tolerance);
        assertClose(BoostBeta::ibeta, 4049.0 / 1000, 1540.0 / 10000, 6537.0 / 10000,
            0.017273988301528087878279199511703371301647583919670, tolerance);
        assertClose(BoostBeta::ibeta, 7269.0 / 1000, 1190.0 / 10000, 8003.0 / 10000,
            0.013334694467796052900138431733772122625376753696347, tolerance);
        assertClose(BoostBeta::ibeta, 2726.0 / 1000, 1151.0 / 100000, 8665.0 / 100000,
            5.8218877068298586420691288375690562915515260230173e-6, tolerance);
        assertClose(BoostBeta::ibeta, 3431.0 / 10000, 4634.0 / 100000, 7582.0 / 10000,
            0.15132819929418661038699397753916091907278005695387, tolerance);

        assertClose(BoostBeta::ibeta, 0.34317314624786377, 0.046342257410287857, 0, 0, tolerance);
        assertClose(BoostBeta::ibetac, 0.34317314624786377, 0.046342257410287857, 0, 1, tolerance);
        assertClose(BoostBeta::ibeta, 0.34317314624786377, 0.046342257410287857, 1, 1, tolerance);
        assertClose(BoostBeta::ibetac, 0.34317314624786377, 0.046342257410287857, 1, 0, tolerance);
        assertClose(BoostBeta::ibeta, 1, 4634.0 / 100000,
            32.0 / 100, 0.017712849440718489999419956301675684844663359595318, tolerance);
        assertClose(BoostBeta::ibeta, 4634.0 / 100000,
            1, 32.0 / 100, 0.94856839398626914764591440181367780660208493234722, tolerance);

        // try with some integer arguments:
        assertClose(BoostBeta::ibeta, 3, 8, 0.25, 0.474407196044921875, tolerance);
        assertClose(BoostBeta::ibeta, 6, 8, 0.25, 0.08021259307861328125, tolerance);
        assertClose(BoostBeta::ibeta, 12, 1, 0.25, 5.9604644775390625e-8, tolerance);
        assertClose(BoostBeta::ibeta, 1, 8, 0.25, 0.8998870849609375, tolerance);

        // very naive check on derivative:
        tolerance = 100;
        assertClose(BoostBeta::ibetaDerivative, 2, 3, 0.5,
            Math.pow(0.5, 2) * Math.pow(0.5, 1) / BoostBeta.beta(2, 3), tolerance);

        //
        // Special cases and error handling:
        //
        Assertions.assertEquals(1, BoostBeta.ibeta(0, 2, 0.5));
        Assertions.assertEquals(0, BoostBeta.ibeta(3, 0, 0.5));
        Assertions.assertEquals(0, BoostBeta.ibetac(0, 2, 0.5));
        Assertions.assertEquals(1, BoostBeta.ibetac(4, 0, 0.5));

        // Domain errors
        Assertions.assertEquals(Double.NaN, BoostBeta.beta(0, 2, 0.5));
        Assertions.assertEquals(Double.NaN, BoostBeta.beta(3, 0, 0.5));
        Assertions.assertEquals(Double.NaN, BoostBeta.betac(0, 2, 0.5));
        Assertions.assertEquals(Double.NaN, BoostBeta.betac(4, 0, 0.5));

        Assertions.assertEquals(Double.NaN, BoostBeta.ibetac(0, 0, 0.5));
        Assertions.assertEquals(Double.NaN, BoostBeta.ibetac(-1, 2, 0.5));
        Assertions.assertEquals(Double.NaN, BoostBeta.ibetac(2, -2, 0.5));
        Assertions.assertEquals(Double.NaN, BoostBeta.ibetac(2, 2, -0.5));
        Assertions.assertEquals(Double.NaN, BoostBeta.ibetac(2, 2, 1.5));

        //
        // a = b = 0.5 is a special case:
        //
        assertClose(BoostBeta::ibeta, 0.5, 0.5, 0.25, 1.0 / 3, tolerance);
        assertClose(BoostBeta::ibetac, 0.5, 0.5, 0.25, 2.0 / 3, tolerance);
        assertClose(BoostBeta::ibeta, 0.5, 0.5, 0.125, 0.230053456162615885213780567705142893009911395270714102055874, tolerance);
        assertClose(BoostBeta::ibetac, 0.5, 0.5, 0.125, 0.769946543837384114786219432294857106990088604729285897944125, tolerance);
        assertClose(BoostBeta::ibeta, 0.5, 0.5, 0.825, 0.725231121519469565327291851560156562956885802608457839260161, tolerance);
        assertClose(BoostBeta::ibetac, 0.5, 0.5, 0.825, 0.274768878480530434672708148439843437043114197391542160739838, tolerance);
        // beta(0.5, 0.5) = pi. Multiply by pi for full incomplete beta
        assertClose(BoostBeta::beta, 0.5, 0.5, 0.25, Math.PI / 3, tolerance);
        assertClose(BoostBeta::betac, 0.5, 0.5, 0.25, Math.PI * 2 / 3, tolerance);
        //
        // Second argument is 1 is a special case, see http://functions.wolfram.com/GammaBetaErf/BetaRegularized/03/01/01/
        //
        assertClose(BoostBeta::ibeta, 0.5, 1, 0.825, 0.908295106229247499626759842915458109758420750043003849691665, tolerance);
        assertClose(BoostBeta::ibetac, 0.5, 1, 0.825, 0.091704893770752500373240157084541890241579249956996150308334, tolerance);
        assertClose(BoostBeta::ibeta, 30, 1, 0.825, 0.003116150729395132012981654047222541793435357905008020740211, tolerance);
        assertClose(BoostBeta::ibetac, 30, 1, 0.825, 0.996883849270604867987018345952777458206564642094991979259788, tolerance);

        //
        // Bug cases from Rocco Romeo:
        //
        assertClose(BoostBeta::beta, 2, 24, Math.scalb(1.0, -52),
            2.46519032881565349871772482100516780410072110983579277754743e-32, tolerance);
        assertClose(BoostBeta::ibeta, 2, 24, Math.scalb(1.0, -52),
            1.47911419728939209923063489260310068246043266590147566652846e-29, tolerance);
        assertClose(BoostBeta::beta, 3, 2, Math.scalb(1.0, -270),
            4.88182556606650701438035298707052523938789614661168065734809e-245, tolerance);
        assertClose(BoostBeta::beta, 2, 31, Math.scalb(1.0, -373),
            1.35080680244581673116149460571129957689952846520037541640260e-225, tolerance);
        assertClose(BoostBeta::ibeta, 3, 2, Math.scalb(1.0, -270),
            5.85819067927980841725642358448463028726547537593401678881771e-244, tolerance);
        assertClose(BoostBeta::ibeta, 2, 31, Math.scalb(1.0, -373),
            1.34000034802625019731220264886560918028433223747877241307138e-222, tolerance);
        //
        // Bug cases from Rocco Romeo:
        //
        assertClose(BoostBeta::beta, 2, 4, Math.scalb(1 + 1.0 / 1024, -351),
            2.381008060978474962211278613067275529112106932635520021e-212, tolerance);
        assertClose(BoostBeta::beta, 2, 4, Math.scalb(1 + 1.0 / 2048, -351),
            2.378685692854274898232669682422430136513931911501225435e-212, tolerance);
        assertClose(BoostBeta::ibeta, 3, 5, Math.scalb(1 + 15.0 / 16, -268),
            2.386034198603463687323052353589201848077110231388968865e-240, tolerance);
        assertClose(BoostBeta::ibetaDerivative, 2, 4, Math.scalb(1.0, -557),
            4.23957586190238472641508753637420672781472122471791800210e-167, tolerance * 4);
        assertClose(BoostBeta::ibetaDerivative, 2, 4.5, Math.scalb(1.0, -557),
            5.24647512910420109893867082626308082567071751558842352760e-167, tolerance * 20);

        // Additional tests

        // Underflow of binomialTerm(n, n, x, y) but binomialTerm(n, n - 1, x, y) is non-zero.
        // Result computed using Maxima: beta_incomplete_regularized with 64 digits precision.
        // Note this is moderately accurate until the missing first term is a significant.
        tolerance = 5;
        assertClose(BoostBeta::ibeta, 20, 2, Math.scalb(1.0, -52),
            1.78247646441082836775138741451452643650923455144048771440215391e-312, tolerance);
        assertClose(BoostBeta::ibeta, 33, 2, Math.scalb(1.0, -32),
            4.403555717560735620179800985566548469434721409459606451475482401e-317, tolerance);
        assertClose(BoostBeta::ibeta, 759, 2, 0.375,
            2.327049191912271223071639014409322663839431670313469640444508565e-321, tolerance);

        // With integer a and/or b just above 1
        // ibeta_small_int_data uses 5 as the smallest parameter above 1
        tolerance = 1;
        assertClose(BoostBeta::ibeta, 1, 2, 0.125, 0.234375, tolerance);
        assertClose(BoostBeta::ibeta, 1, 2, 0.5, 0.75, tolerance);
        assertClose(BoostBeta::ibeta, 1, 2, 0.75, 0.9375, tolerance);
        assertClose(BoostBeta::ibeta, 2, 1, 0.125, 0.015625, tolerance);
        assertClose(BoostBeta::ibeta, 2, 1, 0.5, 0.25, tolerance);
        assertClose(BoostBeta::ibeta, 2, 1, 0.75, 0.5625, tolerance);
        assertClose(BoostBeta::ibeta, 2, 2, 0.125, 0.04296875, tolerance);
        assertClose(BoostBeta::ibeta, 2, 2, 0.5, 0.5, tolerance);
        assertClose(BoostBeta::ibeta, 2, 2, 0.75, 0.84375, tolerance);
        assertClose(BoostBeta::ibeta, 2, 3, 0.125, 0.078857421875, tolerance);
        assertClose(BoostBeta::ibeta, 2, 3, 0.5, 0.6875, tolerance);
        assertClose(BoostBeta::ibeta, 2, 3, 0.75, 0.94921875, tolerance);

        // Extreme integer arguments. Creates an overflow in the binomial coefficient
        // binomial(2147483545 + 39, 38) ~ 7.84899e309
        Assertions.assertEquals(1, BoostBeta.ibeta(39, 2147483546, Math.nextDown(1.0)));
        Assertions.assertEquals(0, BoostBeta.ibetac(39, 2147483546, Math.nextDown(1.0)));
        // Executes a different code path not using the binomial
        Assertions.assertEquals(1, BoostBeta.ibeta(39, Integer.MAX_VALUE, Math.nextDown(1.0)));
        Assertions.assertEquals(0, BoostBeta.ibetac(39, Integer.MAX_VALUE, Math.nextDown(1.0)));

        // x==0 or 1
        Assertions.assertEquals(0, BoostBeta.ibeta(2, 4, 0));
        Assertions.assertEquals(1, BoostBeta.ibeta(2, 4, 1));
        Assertions.assertEquals(1, BoostBeta.ibetac(2, 4, 0));
        Assertions.assertEquals(0, BoostBeta.ibetac(2, 4, 1));
        Assertions.assertEquals(0, BoostBeta.beta(2, 4, 0));
        TestUtils.assertEquals(0.05, BoostBeta.beta(2, 4, 1), 1);
        TestUtils.assertEquals(0.05, BoostBeta.betac(2, 4, 0), 1);
        Assertions.assertEquals(0, BoostBeta.betac(2, 4, 1));

        tolerance = 30;

        // a > 15, integer b < 40, b * x >= 0.7
        assertClose(BoostBeta::ibeta, 72.5, 2, 0.75, 1.673181444858556263e-8, tolerance * 2);

        // a < 15, integer b < 40, b * x >= 0.7, regularized
        assertClose(BoostBeta::ibeta, 14.5, 2, 0.75, 7.1367429756558048437e-2, tolerance);
    }

    @ParameterizedTest
    @EnumSource(value = TriTestCase.class, mode = Mode.MATCH_ANY,
                names = {"IBETA_[SML].*", "IBETAC_[SML].*", "RBETA.*"})
    void testIBeta(TriTestCase tc) {
        assertFunction(tc);
    }

    /**
     * Test the incomplete beta function uses the policy containing the epsilon and
     * maximum iterations for series evaluations. The data targets methods computed
     * using a series component to check the policy is not ignored.
     *
     * <p>This does not target
     * {@link BoostBeta#ibetaFraction(double, double, double, double, Policy, boolean)}.
     * This is used when the result is sub-normal and the continued
     * fraction converges without iteration. The policy has no effect.
     *
     * <p>Running the policy tests on their own should hit the code paths
     * using the policy for function evaluation:
     * <pre>
     * mvn clean test -Dtest=BoostBetaTest#testIBetaPolicy* jacoco:report
     * </pre>
     */
    @ParameterizedTest
    @CsvSource(value = {
        // Data extracted from the resource files and formatted to double precision

        // Target ibetaSeries
        "4.201121919322759E-5,2.1881177963223308E-4,0.6323960423469543,23803.707603529016,4569.595256369948,0.838947362634037,0.16105263736596298",
        "0.22512593865394592,0.4898320138454437,0.7996731996536255,4.764013990849158,0.9820281524501243,0.8290948573018541,0.17090514269814597",
        "4.623167842510156E-5,4.340034502092749E-5,0.135563462972641,21628.337679706918,23043.143905699715,0.48416432390665337,0.5158356760933467",
        "2.9415024982881732E-5,4.1924233664758503E-4,0.3082362115383148,33995.42298781772,2386.0630988783787,0.9344154580933705,0.0655845419066295",
        "1.184685606858693E-5,0.015964560210704803,0.3082362115383148,84409.7658651171,63.42758275377908,0.9992491395179357,7.508604820642986E-4",
        "3.529437162796967E-5,2.2326681573758833E-5,0.6323960423469543,28333.671885777032,44788.91695876111,0.3874817937042091,0.6125182062957909",
        "0.06715317070484161,2.306319236755371,0.9084427952766418,13.787272071732604,0.001859217475218142,0.999865167903893,1.3483209610697333E-4",
        "0.3183284401893616,3.165504217147827,0.07764927297830582,1.3374998709679642,0.6794195418585712,0.6631399660602084,0.3368600339397915",
        "0.15403440594673157,4.049813747406006,0.34629878401756287,4.872033861103044,0.08561968850485947,0.9827297959310547,0.01727020406894529",
        "1.3317101001739502,0.7650398015975952,0.6445860862731934,0.47144799136487586,0.5594135526519237,0.4573339592510717,0.5426660407489283",
        "0.11902070045471191,7.269547462463379,0.19963125884532928,6.225047194692518,0.08420413075357451,0.9866538632858122,0.013346136714187858",
        "2.664715051651001,0.6914005279541016,0.8443243503570557,0.3338388990912521,0.3587830340198169,0.4819929648946269,0.518007035105373",
        "1.0562920570373535,6.812713623046875,0.8258343935012817,0.12732498812245932,9.807107058749213E-7,0.9999922976379849,7.702362015088557E-6",
        "1.7118667364120483,3.0191311836242676,0.07594671100378036,0.0064151684204504875,0.10850933283432233,0.05582072012850319,0.9441792798714969",
        // Target betaSmallBLargeASeries
        "0.04634225741028786,0.34317314624786377,0.24176712334156036,20.363670894714268,3.6307737402387885,0.8486827348798152,0.15131726512018484",
        "2.113992923113983E-5,1.7535277947899885E-5,0.8350250720977783,47305.46963235176,57026.27394012006,0.4534139659948463,0.5465860340051537",
        "4.005068331025541E-5,42.84983825683594,0.12707412242889404,24964.03974453176,4.764518849491958E-4,0.9999999809144722,1.9085527852527327E-8",
        "67.90167999267578,0.8324270844459534,0.9676981568336487,0.002669338211636283,0.031098353139101195,0.0790500654578503,0.9209499345421497",
        "0.395370751619339,0.004023698624223471,0.9058013558387756,4.307485901473997,246.2348442100959,0.017192647244702382,0.9828073527552976",
        "3.444607973098755,66.36054992675781,0.09654488414525986,1.4741027361579568E-6,8.307589573110104E-8,0.9466497330301025,0.05335026696989754",
        "1.0665277242660522,4.985442161560059,0.2400285303592682,0.12523918055824373,0.0476465037156954,0.7244045745268357,0.2755954254731643",
        // Target ibetaFraction2
        "1.319732904434204,4.903014659881592,0.33251503109931946,0.0837704419861451,0.021604794441302123,0.7949727547593459,0.20502724524065405",
        "485.7690734863281,190.16734313964844,0.6323960423469543,7.8023253024461E-182,7.885435919806278E-176,9.894592590329194E-7,0.9999990105407409",
    })
    void testIBetaPolicy(double a, double b, double x, double beta, double betac, double ibeta, double ibetac) {
        // Low iterations should fail to converge
        final Policy pol1 = new Policy(0x1.0p-52, 1);
        Assertions.assertThrows(ArithmeticException.class, () -> BoostBeta.beta(a, b, x, pol1), "beta");
        Assertions.assertThrows(ArithmeticException.class, () -> BoostBeta.betac(a, b, x, pol1), "betac");
        Assertions.assertThrows(ArithmeticException.class, () -> BoostBeta.ibeta(a, b, x, pol1), "ibeta");
        Assertions.assertThrows(ArithmeticException.class, () -> BoostBeta.ibetac(a, b, x, pol1), "ibetac");

        // Low epsilon should not be as accurate
        final Policy pol2 = new Policy(1e-3, Integer.MAX_VALUE);

        // Ignore infinite
        if (Double.isFinite(beta)) {
            final double u1 = BoostBeta.beta(a, b, x);
            final double u2 = BoostBeta.beta(a, b, x, pol2);
            assertCloser("beta", beta, u1, u2);
        }
        if (Double.isFinite(betac)) {
            final double l1 = BoostBeta.betac(a, b, x);
            final double l2 = BoostBeta.betac(a, b, x, pol2);
            assertCloser("betac", betac, l1, l2);
        }

        // Ignore 0 or 1
        if ((int) ibeta != ibeta) {
            final double p1 = BoostBeta.ibeta(a, b, x);
            final double p2 = BoostBeta.ibeta(a, b, x, pol2);
            assertCloser("ibeta", ibeta, p1, p2);
        }
        if ((int) ibetac != ibetac) {
            final double q1 = BoostBeta.ibetac(a, b, x);
            final double q2 = BoostBeta.ibetac(a, b, x, pol2);
            assertCloser("ibetac", ibetac, q1, q2);
        }
    }

    /**
     * Assert x is closer to the expected result than y.
     */
    private static void assertCloser(String msg, double expected, double x, double y) {
        if (!Double.isFinite(expected)) {
            // Test both answers are correct
            Assertions.assertEquals(expected, x);
            Assertions.assertEquals(expected, y);
        } else {
            final double dx = Math.abs(expected - x);
            final double dy = Math.abs(expected - y);
            Assertions.assertTrue(dx < dy,
                () -> String.format("%s %s : %s (%s) : %s (%s)", msg, expected,
                    x, dx / Math.ulp(expected), y, dy / Math.ulp(expected)));
        }
    }

    /**
     * Computes the binomial coefficient.
     *
     * <p>Wrapper to convert double arguments to integer and call the
     * {@link BoostBeta#binomialCoefficient(int, int)} method.
     *
     * @param n Size of the set.
     * @param k Size of the subsets to be counted.
     * @return {@code n choose k}.
     */
    private static double binomialCoefficient(double n, double k) {
        return BoostBeta.binomialCoefficient((int) n, (int) k);
    }

    /**
     * Computes the binomial coefficient using the beta function.
     *
     * <p>Adapted from {@code <boost/math/special_functions/binomial.hpp>}.
     *
     * @param n1 Size of the set.
     * @param k1 Size of the subsets to be counted.
     * @return {@code n choose k}.
     */
    private static double binomialCoefficient1(double n1, double k1) {
        // Use symmetry
        int n = (int) n1;
        int k = (int) k1;
        final int m = Math.min(k, n - k);
        if (m == 0) {
            return 1;
        }
        if (m == 1) {
            return n;
        }
        if (m == 2) {
            // Cannot overflow a long
            return (n - 1L) * n / 2;
        }

        double result;
        if (n <= 170) {
            // Use fast table lookup:
            // Note: This has lower error on test data than calling BinomialCoefficientDouble.
            result = BoostGamma.uncheckedFactorial(n);
            // Smaller m will have a more accurate factorial
            result /= BoostGamma.uncheckedFactorial(m);
            result /= BoostGamma.uncheckedFactorial(n - m);
        } else {
            if (k < n - k) {
                result = k * BoostBeta.beta(k, n - k + 1);
            } else {
                result = (n - k) * BoostBeta.beta(k + 1, n - k);
            }
            result = 1 / result;
        }
        return Math.ceil(result - 0.5f);
    }

    /**
     * Beta function.
     * <p>\[ B(p, q) = \frac{\Gamma(p) \Gamma(q)}}{\Gamma(p+q)} \]
     *
     * <p>Computed using {@link LogBeta}.
     *
     * @param p Argument p
     * @param q Argument q
     * @return beta value
     */
    private static double beta(double p, double q) {
        return Math.exp(LogBeta.value(p, q));
    }

    /**
     * Derivative of the regularised incomplete beta.
     * <p>\[ \frac{\delta}{\delta x} I_x(a, b) = \frac{(1-x)^{b-1} x^{a-1}}{\B(a, b)} \]
     *
     * <p>Computed using {@link LogGamma}.
     *
     * @param a Argument a
     * @param b Argument b
     * @param x Argument x
     * @return ibeta derivative
     */
    private static double ibetaDerivative1(double a, double b, double x) {
        if (x < 0 || x > 1) {
            return 0;
        } else if (x == 0) {
            if (a < 1) {
                // Distribution is not valid when x=0, alpha<1
                // due to a divide by zero error.
                // Do not raise an exception and return the limit.
                return Double.POSITIVE_INFINITY;
            }
            // Special case of cancellation: x^(a-1) (1-x)^(b-1) / B(a, b)
            if (a == 1) {
                return b;
            }
            return 0;
        } else if (x == 1) {
            if (b < 1) {
                // Distribution is not valid when x=1, beta<1
                // due to a divide by zero error.
                // Do not raise an exception and return the limit.
                return Double.POSITIVE_INFINITY;
            }
            // Special case of cancellation: x^(a-1) (1-x)^(b-1) / B(a, b)
            if (b == 1) {
                return a;
            }
            return 0;
        } else {
            final double z = LogGamma.value(a) + LogGamma.value(b) - LogGamma.value(a + b);
            final double logX = Math.log(x);
            final double log1mX = Math.log1p(-x);
            return Math.exp((a - 1) * logX + (b - 1) * log1mX - z);
        }
    }

    /**
     * Derivative of the regularised incomplete beta.
     * <p>\[ \frac{\delta}{\delta x} I_x(a, b) = \frac{(1-x)^{b-1} x^{a-1}}{\B(a, b)} \]
     *
     * <p>Computed using {@link LogBeta}.
     *
     * @param a Argument a
     * @param b Argument b
     * @param x Argument x
     * @return ibeta derivative
     */
    private static double ibetaDerivative2(double a, double b, double x) {
        if (x < 0 || x > 1) {
            return 0;
        } else if (x == 0) {
            if (a < 1) {
                // Distribution is not valid when x=0, alpha<1
                // due to a divide by zero error.
                // Do not raise an exception and return the limit.
                return Double.POSITIVE_INFINITY;
            }
            // Special case of cancellation: x^(a-1) (1-x)^(b-1) / B(a, b)
            if (a == 1) {
                return b;
            }
            return 0;
        } else if (x == 1) {
            if (b < 1) {
                // Distribution is not valid when x=1, beta<1
                // due to a divide by zero error.
                // Do not raise an exception and return the limit.
                return Double.POSITIVE_INFINITY;
            }
            // Special case of cancellation: x^(a-1) (1-x)^(b-1) / B(a, b)
            if (b == 1) {
                return a;
            }
            return 0;
        } else {
            final double z = LogBeta.value(a, b);
            final double logX = Math.log(x);
            final double log1mX = Math.log1p(-x);
            return Math.exp((a - 1) * logX + (b - 1) * log1mX - z);
        }
    }

    /**
     * Regularised incomplete beta.
     * Helper method to call the classic continued fraction representation.
     *
     * @param a Argument a
     * @param b Argument b
     * @param x Argument x
     * @return p
     */
    private static double ibeta(double a, double b, double x) {
        return ibetaImp(a, b, x, false);
    }

    /**
     * Complement of the regularised incomplete beta.
     * Helper method to call the classic continued fraction representation.
     *
     * @param a Argument a
     * @param b Argument b
     * @param x Argument x
     * @return q
     */
    private static double ibetac(double a, double b, double x) {
        return ibetaImp(a, b, x, true);
    }

    /**
     * Regularised incomplete beta implementation using
     * the classic continued fraction representation.
     *
     * <p>This is a partial implementation with no special case handling.
     *
     * @param a Argument a
     * @param b Argument b
     * @param x Argument x
     * @param inv true to compute the complement value
     * @return p
     */
    private static double ibetaImp(double a, double b, double x, boolean inv) {
        // This logic is from Commons Numbers 1.0 for use of the
        // complement of the regularised beta function
        double result;
        if (x > (a + 1) / (2 + b + a)) {
            result = BoostBeta.ibetaFraction(b, a, 1 - x, x, Policy.getDefault(), true);
            inv = !inv;
        } else {
            result = BoostBeta.ibetaFraction(a, b, x, 1 - x, Policy.getDefault(), true);
        }
        return inv ? 1 - result : result;
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
     * Assert the function is close to the expected value.
     *
     * @param fun Function
     * @param x Input value
     * @param y Input value
     * @param expected Expected value
     * @param eps Relative tolerance
     */
    private static void assertWithinEps(DoubleBinaryOperator fun, double x, double y, double expected, double eps) {
        final double actual = fun.applyAsDouble(x, y);
        Assertions.assertEquals(expected, actual, Math.abs(expected) * eps, () -> x + ", " + y);
    }

    /**
     * Assert the function is close to the expected value.
     *
     * @param fun Function
     * @param x Input value
     * @param y Input value
     * @param z Input value
     * @param expected Expected value
     * @param tolerance the tolerance
     */
    private static void assertClose(DoubleTernaryOperator fun, double x, double y, double z, double expected, int tolerance) {
        final double actual = fun.applyAsDouble(x, y, z);
        TestUtils.assertEquals(expected, actual, tolerance, null, () -> x + ", " + y + ", " + z);
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
     * Assert the function using extended precision.
     *
     * @param tc Test case
     */
    private static void assertFunction(TriTestCase tc) {
        final TestUtils.ErrorStatistics stats = new TestUtils.ErrorStatistics();
        try (DataReader in = new DataReader(tc.getFilename())) {
            while (in.next()) {
                try {
                    final double x = in.getDouble(0);
                    final double y = in.getDouble(1);
                    final double z = in.getDouble(2);
                    final BigDecimal expected = in.getBigDecimal(tc.getExpectedField());
                    final double actual = tc.getFunction().applyAsDouble(x, y, z);
                    TestUtils.assertEquals(expected, actual, tc.getTolerance(), stats::add,
                        () -> tc + " x=" + x + ", y=" + y + ", z=" + z);
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
