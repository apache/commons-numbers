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
        ERF(BoostErf::erf, true),
        ERFC(BoostErf::erfc, false),
        ERF_INV(BoostErf::erfInv, true),
        ERFC_INV(BoostErf::erfcInv, false),
        ERFCX(BoostErf::erfcx, false);

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
     * <p>For the erf and erfc functions the Boost error is due to the accuracy of
     * exponentiation. From the Boost test_erf.cpp:
     * <pre>
     * "On MacOS X erfc has much higher error levels than
     * expected: given that the implementation is basically
     * just a rational function evaluation combined with
     * exponentiation, we conclude that exp and pow are less
     * accurate on this platform, especially when the result
     * is outside the range of a double."
     * </pre>
     *
     * @see <a href="https://www.boost.org/doc/libs/1_77_0/libs/math/doc/html/math_toolkit/relative_error.html">Relative error</a>
     * @see <a href="https://www.boost.org/doc/libs/1_77_0/libs/math/doc/html/math_toolkit/pol_tutorial/policy_tut_defaults.html">Policy defaults</a>
     */
    private enum TestCase {
        /** Erf Boost data. */
        ERF(TestFunction.ERF, 1.3, 0.2),
        /** Erfc Boost data. */
        ERFC(TestFunction.ERFC, 2.2, 0.5),
        /** Erf Boost large data (simple case where all z>8, p=1.0). */
        ERF_LARGE(TestFunction.ERF, 0, 0.0),
        /** Erfc Boost large data. */
        ERFC_LARGE(TestFunction.ERFC, 1.75, 0.7),
        /** Erf Boost small data (no exponentiation required). */
        ERF_SMALL(TestFunction.ERF, 1.2, 0.25),
        /** Erfc Boost small data (no exponentiation required: ulp=0). */
        ERFC_SMALL(TestFunction.ERFC, 0, 0.0),
        /** Inverse Erf Boost data. */
        ERF_INV(TestFunction.ERF_INV, 1.8, 0.65),
        /** Inverse Erfc Boost data. */
        ERFC_INV(TestFunction.ERFC_INV, 1.8, 0.65),
        /** Inverse Erfc Boost big data. */
        ERFC_INV_BIG(TestFunction.ERFC_INV, 1.8, 0.5),
        /** Inverse Erf limit data. */
        ERF_INV_LIMIT(TestFunction.ERF_INV, 1.4, 0.5),
        /** Inverse Erfc limit data. */
        ERFC_INV_LIMIT(TestFunction.ERFC_INV, 1.2, 0.5),
        /** Erfcx negative medium data. */
        ERFCX_NEG_MEDIUM(TestFunction.ERFCX, 1.7, 0.55),
        /** Erfcx negative small data. */
        ERFCX_NEG_SMALL(TestFunction.ERFCX, 0.7, 0.061),
        /** Erfcx small data. */
        ERFCX_SMALL(TestFunction.ERFCX, 0.6, 0.073),
        /** Erfcx medium data. */
        ERFCX_MED(TestFunction.ERFCX, 1.2, 0.5),
        /** Erfcx large data. */
        ERFCX_LARGE(TestFunction.ERFCX, 1.1, 0.45),
        /** Erfcx huge data. */
        ERFCX_HUGE(TestFunction.ERFCX, 1.25, 0.45);

        /** Sum of the squared ULP error and count n. */
        private final TestUtils.ErrorStatistics stats = new TestUtils.ErrorStatistics();

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
            stats.add(ulp);
        }

        /**
         * @return Root Mean Squared measured error
         */
        double getRMSError() {
            return stats.getRMS();
        }

        /**
         * @return maximum absolute measured error
         */
        double getMaxAbsError() {
            return stats.getMaxAbs();
        }

        /**
         * @return maximum allowed absolute error
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

    @ParameterizedTest
    @CsvSource({
        "0, 1",
        // Reference data from GCC libquadmath expq(z*z)
        // Cases from known max ulp errors for:
        // a = z*z; b = z*z round-off
        // exp(a+b) = exp(a) * exp(b) = exp(a) * expm1(b) + exp(a)
        "0.042876182518572857, 1.00184005785999452616485960307584103",
        "0.12332761808971203,  1.01532595755115384513226892929687518",
        "0.23492402237866017,  1.05674063282543772769083554593703693",
        "0.5005007866910639,   1.28466892274154983265904587935363446",
        "0.6079329058333609,   1.44713019294681595655835657289687865",
        "0.70256943723915932,  1.6382093968128949474570450121807659",
        "0.82241395271131823,  1.96671514042639717092581737392007089",
        "1.1676941591314409,   3.90989159781783456283136476225348036",
        "1.2872328693421708,   5.24339117555116565213868516013500793",
        "1.763493621168525,    22.4190210354395450203564424019491017",
        "2.2911777554159483,   190.470153340296021320752950876036293",
        "2.6219566826446719,   967.443326246348231629933262201707317",
        "3.0037511703473512,   8287.64469163961117832508854092931197",
        "4.9612493700241904,   48946578790.7631709834358118976268355",
        "9.3076562180806182,   4.20727779143532487527563444483221012e+37",
        "14.323050579907544,   1.24570874136900582743589942668130243e+89",
        "19.789244604728378,   1.19093202729437857229577485425178318e+170",
        "25.264725343479071,   1.63276667418921707391087152900497326e+277",
        "26.471453083170552,   2.12115354204783587764203142231215649e+304",
        "26.628235718509234,   8.7522773171090885282464768100291983e+307",
        // Limit.
        // The next double up (26.64174755704633) has
        // exp(x*x) = 1.79769313486244732925408679719502907e+308 = Infinity
        // This is computed as NaN due to lack of overflow checks.
        "26.641747557046327,   1.79769313486210702414246567679413232e+308",
    })
    void testExpxx(double z, BigDecimal expected) {
        final double actual = BoostErf.expxx(z);
        TestUtils.assertEquals(expected, actual, 1.0);
    }

    @ParameterizedTest
    @CsvSource({
        // Known cases where exp(x*x) is infinite and the round-off is zero or negative
        "27,                 3.98728526204259656354686104733435016e+316",
        "27.45162436,        1.79769313486244732925408679719502907e+308",
        // Next double after 26.641747557046327
        "26.64174755704633,  1.79769313486244732925408679719502907e+308",
    })
    void testExpxxOverflow(double z, double expected) {
        final double e = Math.exp(z * z);
        Assertions.assertEquals(Double.POSITIVE_INFINITY, e);
        Assertions.assertEquals(Double.POSITIVE_INFINITY, expected);
        Assertions.assertEquals(Double.NaN, BoostErf.expxx(z), "No overflow checks");
    }

    @ParameterizedTest
    @CsvSource({
        "0, 1",
        // Reference data from GCC libquadmath expq(-z*z)
        // Cases from known max ulp errors for:
        // a = z*z; b = z*z round-off
        // exp(a+b) = exp(a) * exp(b) = exp(a) * expm1(b) + exp(a)
        "0.018888546570790723, 0.999643286445856926713199201695735708",
        "0.044040879540028978, 0.998062280736064566626960146268218869",
        "0.14252706610502067,  0.979890973955195922461144945194213951",
        "0.21644880196960639,  0.954230441398827896658935167789523746",
        "0.38611178653477424,  0.861498200598095608584361373395644347",
        "0.48918821106359167,  0.78717467413707227187574762653633039",
        "0.69762286071744906,  0.614665134758267412066486574992456239",
        "0.74212965000887676,  0.576513560506426682539710563714000483",
        "0.83549504832885912,  0.497553606822144444881962542058093636",
        "0.93551572137564831,  0.416782963065511995785537520046193978",
        "1.1557980830683314,   0.262929535430060880774643925549236367",
        "1.4043799106718902,   0.139138848619574074162424456941838826",
        "1.76928217755289,     0.0437020868591733321664690441540917611",
        "3.030043930000375,    0.000102960388874918024753918581478636755",
        "6.8941333932059292,   2.28236391243884653067874490191517352e-21",
        "12.150966601450184,   7.55373161692552909052687379129491314e-65",
        "18.145618649215113,   1.00621134630738394627962504745954853e-143",
        "22.58677686154909,    2.74945208846365015258084878726170577e-222",
        "25.307715003447811,   6.96433586576952601048539830338695099e-279",
        "26.314662520770881,   1.85270995749302588464727454552962812e-301",
        "26.550566379026709,   7.1067746279803211201902681713374704e-307",
        // Limit
        "27.297128403953796,   2.47032822920651974943004172315065178e-324",
        // exp(-z*z) = 0
        "27.2971284039538,     2.47032822920604061009296400001395168e-324",
    })
    void testExpmxx(double z, BigDecimal expected) {
        final double actual = BoostErf.expmxx(z);
        TestUtils.assertEquals(expected, actual, 1.0);
    }

    @ParameterizedTest
    @CsvSource({
        // Reference data from GCC libquadmath expq(z)
        // exp(z) is nearly sub-normal
        "26.589967471163597, 8.75686990774305433076998380040492953e-308",
        "26.592726991055095, 7.56157741629803260538530412232841961e-308",
        "26.593224983876986, 7.36392883240998076191982506525092463e-308",
        "26.596608821043414, 6.15095812619805721349676447899566968e-308",
    })
    void testExpmxxCloseToSubNormal(double z, BigDecimal expected) {
        final double actual = BoostErf.expmxx(z);
        TestUtils.assertEquals(expected, actual, 1.3);
    }

    @ParameterizedTest
    @CsvSource({
        "-Infinity, -1",
        "Infinity, 1",
        "0, 0",
        // Odd function
        "-0.0, -0.0",
        "NaN, NaN",
        // Realistic limits
        "-6, -1",
        "6, 1",
    })
    void testErfEdgeCases(double z, double p) {
        Assertions.assertEquals(p, BoostErf.erf(z));
    }

    @ParameterizedTest
    @CsvSource({
        "-Infinity, 2",
        "Infinity, 0",
        "0, 1",
        "NaN, NaN",
        // Realistic limits
        "-6, 2",
        "28, 0",
    })
    void testErfcEdgeCases(double z, double q) {
        Assertions.assertEquals(q, BoostErf.erfc(z));
    }

    @ParameterizedTest
    @Order(1)
    @CsvFileSource(resources = "erf_data.csv")
    void testErf(double z, BigDecimal p, double q) {
        assertErf(TestCase.ERF, z, p);
    }

    @Test
    @Order(1010)
    void testErfRMS() {
        assertRms(TestCase.ERF);
    }

    @ParameterizedTest
    @Order(1)
    @CsvFileSource(resources = "erf_data.csv")
    void testErfc(double z, double p, BigDecimal q) {
        assertErf(TestCase.ERFC, z, q);
    }

    @Test
    @Order(1020)
    void testErfcRMS() {
        assertRms(TestCase.ERFC);
    }

    @ParameterizedTest
    @Order(1)
    @CsvFileSource(resources = "erf_large_data.csv")
    void testErfLarge(double z, BigDecimal p, double q) {
        assertErf(TestCase.ERF_LARGE, z, p);
    }

    @Test
    @Order(1030)
    void testErfLargeRMS() {
        assertRms(TestCase.ERF_LARGE);
    }

    @ParameterizedTest
    @Order(1)
    @CsvFileSource(resources = "erf_large_data.csv")
    void testErfcLarge(double z, double p, BigDecimal q) {
        assertErf(TestCase.ERFC_LARGE, z, q);
    }

    @Test
    @Order(1040)
    void testErfcLargeRMS() {
        assertRms(TestCase.ERFC_LARGE);
    }

    @ParameterizedTest
    @Order(1)
    @CsvFileSource(resources = "erf_small_data.csv")
    void testErfSmall(double z, BigDecimal p, double q) {
        assertErf(TestCase.ERF_SMALL, z, p);
    }

    @Test
    @Order(1050)
    void testErfSmallRMS() {
        assertRms(TestCase.ERF_SMALL);
    }

    @ParameterizedTest
    @Order(1)
    @CsvFileSource(resources = "erf_small_data.csv")
    void testErfcSmall(double z, double p, BigDecimal q) {
        assertErf(TestCase.ERFC_SMALL, z, q);
    }

    @Test
    @Order(1060)
    void testErfcSmallRMS() {
        assertRms(TestCase.ERFC_SMALL);
    }

    /**
     * This tests the erf function as the result approaches 1.0. The Boost threshold
     * for large z is 5.8f. This is too low and results in 2 ulp errors when z is
     * just above 5.8 and the result is 0.9999999999999998.
     *
     * @param z Value to test
     * @param p Expected p
     */
    @ParameterizedTest
    @CsvFileSource(resources = "erf_close_to_1_data.csv")
    void testErfCloseTo1(double z, double p) {
        Assertions.assertTrue(5.8 < z, () -> "z not above Boost threshold: " + z);
        Assertions.assertTrue(z < 5.95, () -> "z not close to Boost threhsold: " + z);
        Assertions.assertTrue(p <= 1.0, () -> "p not <= 1: " + p);
        Assertions.assertEquals(1.0, p, 0x1.0p-52, "Value not with 2 ulp of 1.0");
        Assertions.assertEquals(p, BoostErf.erf(z));
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
            x -> BoostErf.erf(BoostErf.erfInv(x)),
            // Inverse Erf domain: [-1, 1]
            -0.95, 1, 0.125,
            2L, 0.99);
    }

    /**
     * Round-trip test of the inverse erfc and then the erfc to return to the original value.
     */
    @Test
    void testErfcRoundTrip() {
        assertRoundTrip("erfc(erfcInv(x))",
            x -> BoostErf.erfc(BoostErf.erfcInv(x)),
            // Inverse Erfc domain: [0, 2]
            0.125, 2, 0.125,
            2L, 0.99);
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
        final TestUtils.ErrorStatistics data = new TestUtils.ErrorStatistics();
        for (double p = low; p <= high; p += increment) {
            final double pp = fun.applyAsDouble(p);
            TestUtils.assertEquals(p, pp, tolerance, ulp -> data.add(ulp), () -> name);
        }
        assertRms(name, data, rmsUlp);
    }

    @ParameterizedTest
    @CsvSource({
        "NaN, NaN",
        "0, 1",
        "-0.0, 1",
        "1e-100, 1",
        // expected 1.0000000000000002
        "-2.220446049250313E-16, 1.0000000000000002505505",
        // expected 1.0
        "-1.1102230246251565E-16, 1.000000000000000125275",
        "1.734723475976807e-17, 0.99999999999999998042574169",
        // expected 0.9999999999999999
        "5.551115123125783e-17, 0.99999999999999993736237340916",
        // max value
        "1.7976931348623157e308, 3.13840873398544321279297017922274491e-309",
        "Infinity, 0",
        "-27, Infinity",
        "-26.62873571375149, 1.7976931348622485388617592502115433e+308",
        // This is infinite as a double: matlab erfcx = Inf
        "-26.628735713751492, 1.79769313486258867776818776259144527e+308",
    })
    void testErfcxEdgeCases(double z, double expected) {
        final double actual = BoostErf.erfcx(z);
        // Workaround for NaN not having a valid ulp
        if (Double.isFinite(expected)) {
            Assertions.assertEquals(expected, actual, Math.ulp(expected));
        } else {
            Assertions.assertEquals(expected, actual);
        }
    }

    @ParameterizedTest
    @CsvSource({
        // matlab: fprintf("%.17g\n, erfcx(z))
        "-24.356, 8.5293595881160216e+257",
        "-12.34, 2.7132062210034015e+66",
        "-6.89, 8.2775358436372447e+20",
        "-1.11134, 6.4783098090861095",
        "-0.67868, 2.6356650381821858",
        "-0.1234, 1.156008270595601",
        "0.1234, 0.87467990946395457",
        "0.2836, 0.74601996202714793",
        "0.7521364, 0.5061525663103037",
        "1.678, 0.2947001506216585",
        "2.67868, 0.19827490572168827",
        "5.6788, 0.09787639472753934",
        "10.67182, 0.052638121464397732",
        "15.678, 0.035913308816213706",
        "23.975, 0.023511995366729203",
        "26.8989, 0.020959983993738648",
        "27.1678, 0.020752808901245822",
        "27.789, 0.020289502724156101",
        "28.4567, 0.019814028635674531",
        "33.67868, 0.016744753846685077",
        "56.567, 0.0099722712078122132",
        "101.101, 0.0055801820870247853",
        "234.7, 0.0024038536962965006",
        "678658.678, 8.3133039013043281e-07",
    })
    void testErfcxSpotTests(double z, double expected) {
        final double actual = BoostErf.erfcx(z);
        Assertions.assertEquals(expected, actual, Math.ulp(expected));
    }

    @ParameterizedTest
    @Order(1)
    @CsvFileSource(resources = "erfcx_neg_medium_data.csv")
    void testErfcxNegMedium(double z, BigDecimal expected) {
        assertErf(TestCase.ERFCX_NEG_MEDIUM, z, expected);
    }

    @Test
    @Order(3010)
    void testErfcxNegMediumRMS() {
        assertRms(TestCase.ERFCX_NEG_MEDIUM);
    }

    @ParameterizedTest
    @Order(1)
    @CsvFileSource(resources = "erfcx_neg_small_data.csv")
    void testErfcxNegSmall(double z, BigDecimal expected) {
        assertErf(TestCase.ERFCX_NEG_SMALL, z, expected);
    }

    @Test
    @Order(3020)
    void testErfcxNegSmallRMS() {
        assertRms(TestCase.ERFCX_NEG_SMALL);
    }

    @ParameterizedTest
    @Order(1)
    @CsvFileSource(resources = "erfcx_small_data.csv")
    void testErfcxSmall(double z, BigDecimal expected) {
        assertErf(TestCase.ERFCX_SMALL, z, expected);
    }

    @Test
    @Order(3030)
    void testErfcxSmallRMS() {
        assertRms(TestCase.ERFCX_SMALL);
    }

    @ParameterizedTest
    @Order(1)
    @CsvFileSource(resources = "erfcx_medium_data.csv")
    void testErfcxMedium(double z, BigDecimal expected) {
        assertErf(TestCase.ERFCX_MED, z, expected);
    }

    @Test
    @Order(3040)
    void testErfcxMediumRMS() {
        assertRms(TestCase.ERFCX_MED);
    }

    @ParameterizedTest
    @Order(1)
    @CsvFileSource(resources = "erfcx_large_data.csv")
    void testErfcxLarge(double z, BigDecimal expected) {
        assertErf(TestCase.ERFCX_LARGE, z, expected);
    }

    @Test
    @Order(3050)
    void testErfcxLargeRMS() {
        assertRms(TestCase.ERFCX_LARGE);
    }

    @ParameterizedTest
    @Order(1)
    @CsvFileSource(resources = "erfcx_huge_data.csv")
    void testErfcxHuge(double z, BigDecimal expected) {
        assertErf(TestCase.ERFCX_HUGE, z, expected);
    }

    @Test
    @Order(3060)
    void testErfcxHugeRMS() {
        assertRms(TestCase.ERFCX_HUGE);
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
        debugRms(tc.toString(), tc.getMaxAbsError(), rms);
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
    private static void assertRms(String name, TestUtils.ErrorStatistics data, double rmsTolerance) {
        final double rms = data.getRMS();
        debugRms(name, data.getMaxAbs(), rms);
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
