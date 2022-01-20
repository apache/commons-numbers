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
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link RegularizedBeta}.
 */
class RegularizedBetaTest {
    private static final double EPS = Policy.getDefault().getEps();
    private static final int MAX_ITER = Policy.getDefault().getMaxIterations();

    @ParameterizedTest
    @CsvSource({
        "0, 1, 2, 0",
        "1, 1, 2, 1",
        "0.5, 1, 2, 0.75",
        // Invalid variants
        "1, 2, NaN, NaN",
        "0.5, 2, NaN, NaN",
        "0.5, 1, NaN, NaN",
        "-0.5, 1, 2, NaN",
        "0.5, -1, 2, NaN",
        "0.5, 1, -2, NaN",
        "0.5, 0, 0, NaN",
        "1.5, 1, 2, NaN",
        // Special case where a xor b is zero
        "0.5, 0, 2, 1",
        "0.5, 1, 0, 0",
    })
    void testRegularizedBetaArguments(double x,
                                      double a,
                                      double b,
                                      double expected) {
        assertRegularizedBeta(x, a, b, expected);
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
        assertRegularizedBeta(0.0, 1e17, 0.5, 0);
        assertRegularizedBeta(1.0, 1e17, 0.5, 1);

        // a and b do not matter
        final double[] v = {0.1, 0.5, 1, 2, 10};
        for (final double a : v) {
            for (final double b : v) {
                assertRegularizedBeta(0, a, b, 0);
                assertRegularizedBeta(1, a, b, 1);
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

    @ParameterizedTest
    @CsvSource({
        // Generated using matlab's betainc function with optional 'upper'
        // argument for the complement
        "0.125, 0.75, 0.25, 0.065817016037708564, 0.93418298396229149",
        "0.125, 0.75, 8.75, 0.78161774393594907, 0.21838225606405093",
        "0.125, 0.75, 13, 0.88429832262475594, 0.11570167737524407",
        "0.125, 5.5, 0.25, 8.8583485327920987e-07, 0.99999911416514675",
        "0.125, 5.5, 8.75, 0.0080298818621658066, 0.9919701181378342",
        "0.125, 5.5, 13, 0.031403972369053644, 0.96859602763094632",
        "0.125, 15.75, 0.25, 2.275822511739568e-16, 0.99999999999999978",
        "0.125, 15.75, 8.75, 1.1608725295814249e-09, 0.99999999883912749",
        "0.125, 15.75, 13, 3.5489227844330215e-08, 0.99999996451077211",
        "0.375, 0.75, 0.25, 0.16604052887639467, 0.83395947112360536",
        "0.375, 0.75, 8.75, 0.9905382314971497, 0.0094617685028503297",
        "0.375, 0.75, 13, 0.99882118884718074, 0.0011788111528192643",
        "0.375, 5.5, 0.25, 0.00045786386155093555, 0.99954213613844911",
        "0.375, 5.5, 8.75, 0.48323155833692188, 0.51676844166307812",
        "0.375, 5.5, 13, 0.77664105547610296, 0.22335894452389699",
        "0.375, 15.75, 0.25, 9.3965393190826217e-09, 0.99999999060346068",
        "0.375, 15.75, 8.75, 0.0035432647390343341, 0.99645673526096568",
        "0.375, 15.75, 13, 0.030538591713165242, 0.9694614082868348",
        "0.8125, 0.75, 0.25, 0.40160289105220459, 0.59839710894779541",
        "0.8125, 0.75, 8.75, 0.99999978600605499, 2.1399394497066033e-07",
        "0.8125, 0.75, 13, 0.99999999984153987, 1.584600819904228e-10",
        "0.8125, 5.5, 0.25, 0.06149698416108057, 0.93850301583891937",
        "0.8125, 5.5, 8.75, 0.99978985623633465, 0.00021014376366534313",
        "0.8125, 5.5, 13, 0.99999931052227398, 6.8947772598025737e-07",
        "0.8125, 15.75, 0.25, 0.003971077054507376, 0.99602892294549261",
        "0.8125, 15.75, 8.75, 0.97219596511873307, 0.027804034881266888",
        "0.8125, 15.75, 13, 0.9993055839090208, 0.00069441609097920377",
    })
    void testRegularizedBeta(double x, double a, double b, double ibeta, double ibetac) {
        final double eps = 1e-13;
        Assertions.assertEquals(ibeta, RegularizedBeta.value(x, a, b), ibeta * eps);
        Assertions.assertEquals(ibetac, RegularizedBeta.complement(x, a, b), ibetac * eps);
    }

    /**
     * Test the incomplete beta function uses the policy containing the epsilon and
     * maximum iterations for series evaluations. The data targets each method computed
     * using a series component to check the policy is not ignored.
     *
     * @see BoostBetaTest#testIBetaPolicy(double, double, double, double, double, double, double)
     */
    @ParameterizedTest
    @CsvSource(value = {
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
        Assertions.assertThrows(ArithmeticException.class, () -> RegularizedBeta.value(x, a, b, EPS, 1), "ibeta");
        Assertions.assertThrows(ArithmeticException.class, () -> RegularizedBeta.complement(x, a, b, EPS, 1), "ibetac");

        // Low epsilon should not be as accurate

        // Ignore 0 or 1
        if ((int) ibeta != ibeta) {
            final double p1 = RegularizedBeta.value(x, a, b);
            final double p2 = RegularizedBeta.value(x, a, b, 1e-3, MAX_ITER);
            assertCloser("ibeta", ibeta, p1, p2);
        }
        if ((int) ibetac != ibetac) {
            final double q1 = RegularizedBeta.complement(x, a, b);
            final double q2 = RegularizedBeta.complement(x, a, b, 1e-3, MAX_ITER);
            assertCloser("ibetac", ibetac, q1, q2);
        }
    }

    @ParameterizedTest
    @CsvFileSource(resources = "ibeta_derivative_med_data.csv")
    void testIBetaDerivative(double a, double b, double x, double expected) {
        final double actual = RegularizedBeta.derivative(x, a, b);
        TestUtils.assertEquals(expected, actual, 150);
    }

    /**
     * Assert the regularized beta exactly matches the expected result.
     * The complement is tested as 1-expected.
     *
     * @param x Argument x
     * @param a Argument a
     * @param b Argument b
     * @param expected Expected result
     */
    private static void assertRegularizedBeta(double x, double a, double b, double expected) {
        Assertions.assertEquals(expected, RegularizedBeta.value(x, a, b), 1e-15);
        Assertions.assertEquals(1 - expected, RegularizedBeta.complement(x, a, b), 1e-15);
        Assertions.assertEquals(expected, RegularizedBeta.value(x, a, b, EPS, MAX_ITER), 1e-15);
        Assertions.assertEquals(1 - expected, RegularizedBeta.complement(x, a, b, EPS, MAX_ITER), 1e-15);
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
}
