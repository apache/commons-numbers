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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link IncompleteBeta}.
 *
 * <p>The class directly calls the methods in {@link BoostBeta}. This test ensures
 * the arguments are passed through correctly. Accuracy of the function is tested
 * in {@link BoostBetaTest}.
 */
class IncompleteBetaTest {
    private static final double EPS = Policy.getDefault().getEps();
    private static final int MAX_ITER = Policy.getDefault().getMaxIterations();

    @ParameterizedTest
    @CsvFileSource(resources = "ibeta_med_data.csv")
    void testIBeta(double a, double b, double x, double beta) {
        TestUtils.assertEquals(beta, IncompleteBeta.value(x, a, b), 190);
    }

    @ParameterizedTest
    @CsvFileSource(resources = "ibeta_med_data.csv")
    void testIBetaComplement(double a, double b, double x, double beta, double betac) {
        TestUtils.assertEquals(betac, IncompleteBeta.complement(x, a, b), 130);
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
        Assertions.assertThrows(ArithmeticException.class, () -> IncompleteBeta.value(x, a, b, EPS, 1), "ibeta");
        Assertions.assertThrows(ArithmeticException.class, () -> IncompleteBeta.complement(x, a, b, EPS, 1), "ibetac");

        // Low epsilon should not be as accurate

        // Ignore infinite
        if (Double.isFinite(beta)) {
            final double u1 = IncompleteBeta.value(x, a, b);
            final double u2 = IncompleteBeta.value(x, a, b, 1e-3, MAX_ITER);
            assertCloser("beta", beta, u1, u2);
        }
        if (Double.isFinite(betac)) {
            final double l1 = IncompleteBeta.complement(x, a, b);
            final double l2 = IncompleteBeta.complement(x, a, b, 1e-3, MAX_ITER);
            assertCloser("betac", betac, l1, l2);
        }
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
