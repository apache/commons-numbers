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
import java.util.function.DoubleUnaryOperator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link BoostErf}.
 *
 * <p>Note: Some resource data files used in these tests have been extracted
 * from the Boost test files for the Erf functions.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BoostErfTest {
    // Test order:
    // Any methods not annotated with @Order will be executed as if using int max value.
    // This class uses the @Order annotation to build the RMS values before asserting
    // the max RMS. For convenience when using reporting the ulp errors this is done
    // in the order of the class methods.

    /**
     * Define the function to test.
     * This is a utility to identify if the function is an odd function: f(x) = -f(-x).
     */
    private enum TestFunction {
        ERF_INV(BoostErf::erfInv, true),
        ERFC_INV(BoostErf::erfcInv, false);

        /** The function. */
        private final DoubleUnaryOperator fun;

        /** Odd function flag: f(x) = -f(-x). */
        private final boolean odd;

        /**
         * @param fun function to test
         * @param odd true if an odd function
         */
        TestFunction(DoubleUnaryOperator fun, boolean odd) {
            this.fun = fun;
            this.odd = odd;
        }

        /**
         * @return the function
         */
        DoubleUnaryOperator getFunction() {
            return fun;
        }

        /**
         * @return true if an odd function: f(x) = -f(-x)
         */
        boolean isOdd() {
            return odd;
        }
    }

    /**
     * Define the test cases for each resource file.
     * This encapsulates the function to test, the expected maximum and RMS error, and
     * methods to accumulate error and compute RMS error.
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
    private enum TestCase {
        /** Inverse Erf Boost data. */
        ERF_INV(TestFunction.ERF_INV, 1.8, 0.65),
        /** Inverse Erfc Boost data. */
        ERFC_INV(TestFunction.ERFC_INV, 1.8, 0.65),
        /** Inverse Erfc Boost big data. */
        ERFC_INV_BIG(TestFunction.ERFC_INV, 1.8, 0.5),
        /** Inverse Erf limit data. */
        ERF_INV_LIMIT(TestFunction.ERF_INV, 1.4, 0.5),
        /** Inverse Erfc limit data. */
        ERFC_INV_LIMIT(TestFunction.ERFC_INV, 1.2, 0.5);

        /** Sum of the squared ULP error and count n. */
        private final RMS rms = new RMS();

        /** The function. */
        private final TestFunction fun;

        /** The maximum allowed ulp. */
        private final double maxUlp;

        /** The maximum allowed RMS ulp. */
        private final double rmsUlp;

        /**
         * @param fun function to test
         * @param maxUlp maximum allowed ulp
         * @param rmsUlp maximum allowed RMS ulp
         */
        TestCase(TestFunction fun, double maxUlp, double rmsUlp) {
            this.fun = fun;
            this.maxUlp = maxUlp;
            this.rmsUlp = rmsUlp;
        }

        /**
         * @return function to test
         */
        DoubleUnaryOperator getFunction() {
            return fun.getFunction();
        }

        /**
         * @return true if an odd function
         */
        boolean isOdd() {
            return fun.isOdd();
        }

        /**
         * @param ulp error in ulp
         */
        void addError(double ulp) {
            rms.add(ulp);
        }

        /**
         * @return Root Mean Squared measured error
         */
        double getRMSError() {
            return rms.getRMS();
        }

        /**
         * @return maximum measured error
         */
        double getMaxError() {
            return rms.getMax();
        }

        /**
         * @return maximum allowed error
         */
        double getTolerance() {
            return maxUlp;
        }

        /**
         * @return maximum allowed RMS error
         */
        double getRmsTolerance() {
            return rmsUlp;
        }
    }

    /**
     * Class to compute the root mean squared error (RMS).
     * @see <a href="https://en.wikipedia.org/wiki/Root_mean_square">Wikipedia: RMS</a>
     */
    private static class RMS {
        private double ss;
        private double max;
        private int n;

        /**
         * @param x Value (assumed to be positive)
         */
        void add(double x) {
            // Overflow is not supported.
            // Assume the expected and actual are quite close when measuring the RMS.
            ss += x * x;
            n++;
            // Assume absolute error when detecting the maximum
            max = max < x ? x : max;
        }

        /**
         * Gets the maximum error.
         *
         * <p>This is not used for assertions. It can be used to set maximum ULP thresholds
         * for test data if the TestUtils.assertEquals method is used with a large maxUlps
         * to measure the ulp (and effectively ignore failures) and the maximum reported
         * as the end of testing.
         *
         * @return maximum error
         */
        double getMax() {
            return max;
        }

        /**
         * Gets the root mean squared error (RMS).
         *
         * <p> Note: If no data has been added this will return 0/0 = nan.
         * This prevents using in assertions without adding data.
         *
         * @return root mean squared error (RMS)
         */
        double getRMS() {
            return Math.sqrt(ss / n);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0",
        // Odd function
        "-0.0, -0.0",
        "1, Infinity",
        "-1, -Infinity",
        "NaN, NaN",
        // Domain errors do not throw
        "-1.1, NaN",
        "1.1, NaN",
    })
    void testInverseErfEdgeCases(double p, double z) {
        Assertions.assertEquals(z, BoostErf.erfInv(p));
    }

    @ParameterizedTest
    @CsvSource({
        "1, 0",
        "0, Infinity",
        "2, -Infinity",
        "NaN, NaN",
        // Domain errors do not throw
        "-0.1, NaN",
        "2.1, NaN",
    })
    void testInverseErfcEdgeCases(double p, double z) {
        Assertions.assertEquals(z, BoostErf.erfcInv(p));
    }

    @ParameterizedTest
    @Order(1)
    @CsvFileSource(resources = "erf_inv_data.csv")
    void testInverseErf(double p, BigDecimal z) {
        assertErf(TestCase.ERF_INV, p, z);
    }

    @Test
    @Order(2010)
    void testInverseErfRMS() {
        assertRms(TestCase.ERF_INV);
    }

    @ParameterizedTest
    @Order(1)
    @CsvFileSource(resources = "erfc_inv_data.csv")
    void testInverseErfc(double q, BigDecimal z) {
        assertErf(TestCase.ERFC_INV, q, z);
    }

    @Test
    @Order(2020)
    void testInverseErfcRMS() {
        assertRms(TestCase.ERFC_INV);
    }

    @ParameterizedTest
    @Order(1)
    @CsvFileSource(resources = "erfc_inv_big_data.csv")
    void testInverseErfcBig(double q, BigDecimal z) {
        assertErf(TestCase.ERFC_INV_BIG, q, z);
    }

    @Test
    @Order(2030)
    void testInverseErfcBigRMS() {
        assertRms(TestCase.ERFC_INV_BIG);
    }

    @ParameterizedTest
    @Order(1)
    @CsvFileSource(resources = "erf_inv_limit_data.csv")
    void testInverseErfLimit(double p, BigDecimal z) {
        assertErf(TestCase.ERF_INV_LIMIT, p, z);
    }

    @Test
    @Order(2040)
    void testInverseErfLimitRMS() {
        assertRms(TestCase.ERF_INV_LIMIT);
    }

    @ParameterizedTest
    @Order(1)
    @CsvFileSource(resources = "erfc_inv_limit_data.csv")
    void testInverseErfcLimit(double q, BigDecimal z) {
        assertErf(TestCase.ERFC_INV_LIMIT, q, z);
    }

    @Test
    @Order(2050)
    void testInverseErfcLimitRMS() {
        assertRms(TestCase.ERFC_INV_LIMIT);
    }

    /**
     * Round-trip test of the inverse erf and then the erf to return to the original value.
     */
    @Test
    void testErfRoundTrip() {
        assertRoundTrip("erf(erfInv(x))",
            x -> Erf.value(BoostErf.erfInv(x)),
            // Inverse Erf domain: [-1, 1]
            -0.95, 1, 0.125,
            5L, 2.99);
    }

    /**
     * Round-trip test of the inverse erfc and then the erfc to return to the original value.
     */
    @Test
    void testErfcRoundTrip() {
        assertRoundTrip("erfc(erfcInv(x))",
            x -> Erfc.value(BoostErf.erfcInv(x)),
            // Inverse Erfc domain: [0, 2]
            0.125, 2, 0.125,
            15L, 3.99);
    }

    /**
     * Test a round-trip function (foward and reverse) returns to the original value.
     *
     * @param name Test name
     * @param fun Round-trip function
     * @param low Low bound to test
     * @param high Upper bound to test
     * @param increment Increment between bounds
     * @param tolerance Maximum ULP tolerance
     * @param rmsUlp Maximum RMS ULP
     */
    private static void assertRoundTrip(String name,
            DoubleUnaryOperator fun,
            double low, double high, double increment,
            long tolerance, double rmsUlp) {
        final RMS data = new RMS();
        for (double p = low; p <= high; p += increment) {
            final double pp = fun.applyAsDouble(p);
            TestUtils.assertEquals(p, pp, tolerance, ulp -> data.add(ulp), () -> name);
        }
        assertRms(name, data, rmsUlp);
    }

    /**
     * Assert the function using extended precision.
     *
     * @param tc Test case
     * @param x Input value
     * @param expected Expected value
     */
    private static void assertErf(TestCase tc, double x, BigDecimal expected) {
        final double actual = tc.getFunction().applyAsDouble(x);
        TestUtils.assertEquals(expected, actual, tc.getTolerance(), tc::addError,
            () -> tc + " x=" + x);
        if (tc.isOdd()) {
            // Use a delta of 0.0 to allow -0.0 == 0.0
            Assertions.assertEquals(actual, -tc.getFunction().applyAsDouble(-x), 0.0, "odd function: f(x) = -f(-x)");
        }
    }

    /**
     * Assert the Root Mean Square (RMS) error of the function is below the allowed maximum.
     *
     * @param tc Test case
     */
    private static void assertRms(TestCase tc) {
        final double rms = tc.getRMSError();
        debugRms(tc.toString(), tc.getMaxError(), rms);
        Assertions.assertTrue(rms <= tc.getRmsTolerance(),
            () -> String.format("%s RMS %s < %s", tc, rms, tc.getRmsTolerance()));
    }

    /**
     * Assert the Root Mean Square (RMS) error of the function is below the allowed maximum.
     *
     * @param name Test name
     * @param data Test data
     * @param rmsTolerance RMS tolerance
     */
    private static void assertRms(String name, RMS data, double rmsTolerance) {
        final double rms = data.getRMS();
        debugRms(name, data.getMax(), rms);
        Assertions.assertTrue(rms <= rmsTolerance,
            () -> String.format("%s RMS %s < %s", name, rms, rmsTolerance));
    }

    /**
     * Output the maximum and RMS ulp for the named test.
     * Used for reporting the errors and setting appropriate test tolerances.
     * This is relevant across different JDK implementations where the java.util.Math
     * functions used in BoostErf may compute to different accuracy.
     *
     * @param name Test name
     * @param maxUlp Maximum ulp
     * @param rmsUlp RMS ulp
     */
    private static void debugRms(String name, double maxUlp, double rmsUlp) {
        // CHECKSTYLE: stop regexp
        // Debug output of max and RMS error.
        //System.out.printf("%-25s   max %10.6g   RMS %10.6g%n", name, maxUlp, rmsUlp);
    }
}
