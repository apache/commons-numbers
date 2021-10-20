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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link RegularizedGamma}.
 */
class RegularizedGammaTest {
    /**
     * Test argument A cannot be NaN, negative or zero.
     *
     * @param a Argument a
     */
    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, 0, -1})
    void testInvalidArgumentA(double a) {
        // No exception thrown. The result is NaN.
        assertRegularizedGamma(a, 1.0, Double.NaN);
    }

    /**
     * Test argument X cannot be NaN or negative.
     *
     * @param x Argument x
     */
    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, -1})
    void testInvalidArgumentX(double x) {
        // No exception thrown. The result is NaN.
        assertRegularizedGamma(1.0, x, Double.NaN);
    }

    @Test
    void testRegularizedGammaPWithACloseToZero() {
        // Creates a case where the regularized gamma P series is evaluated and the
        // result is outside the expected bounds of [0, 1]. This should be clipped to 1.0.
        final double a = 1e-18;
        // x must force use of the series in regularized gamma P using x < a + 1
        final double x = 0.5;
        assertRegularizedGamma(a, x, 1.0);
    }

    @Test
    void testRegularizedGammaPWithAVeryCloseToZero() {
        // Creates a case where the partial sum is infinite due to inclusion of 1 / a
        final double a = Double.MIN_VALUE;
        // x must force use of the series in regularized gamma P using x < a + 1
        final double x = 0.5;
        assertRegularizedGamma(a, x, 1.0);
    }

    @ParameterizedTest
    @CsvSource({
        // Generated using matlab's gammainc function
        "0.250, 0.0, 0, 1",
        "0.250, 0.250, 0.74367794473146098, 0.25632205526853896",
        "0.250, 0.50, 0.84648640419167753, 0.15351359580832244",
        "0.250, 1.0, 0.93207886798989104, 0.067921132010108964",
        "0.250, 1.50, 0.96658355584102096, 0.033416444158979083",
        "0.250, 2.0, 0.98271398814048316, 0.017286011859516792",
        "0.250, 4.0, 0.99845731780336078, 0.0015426821966391837",
        "0.250, 10.0, 0.99999791696959139, 2.0830304086494328e-06",
        "0.50, 0.0, 0, 1",
        "0.50, 0.250, 0.52049987781304641, 0.47950012218695348",
        "0.50, 0.50, 0.68268949213708585, 0.31731050786291409",
        "0.50, 1.0, 0.84270079294971489, 0.15729920705028508",
        "0.50, 1.50, 0.91673548333644961, 0.083264516663550392",
        "0.50, 2.0, 0.95449973610364158, 0.045500263896358438",
        "0.50, 4.0, 0.99532226501895271, 0.0046777349810472628",
        "0.50, 10.0, 0.99999225578356898, 7.7442164310441028e-06",
        "1.0, 0.0, 0, 1",
        "1.0, 0.250, 0.22119921692859515, 0.77880078307140488",
        "1.0, 0.50, 0.39346934028736663, 0.60653065971263342",
        "1.0, 1.0, 0.63212055882855767, 0.36787944117144233",
        "1.0, 1.50, 0.77686983985157021, 0.22313016014842982",
        "1.0, 2.0, 0.8646647167633873, 0.1353352832366127",
        "1.0, 4.0, 0.98168436111126578, 0.018315638888734182",
        "1.0, 10.0, 0.99995460007023751, 4.5399929762484827e-05",
        "1.50, 0.0, 0, 1",
        "1.50, 0.250, 0.081108588345324126, 0.91889141165467592",
        "1.50, 0.50, 0.19874804309879915, 0.80125195690120088",
        "1.50, 1.0, 0.42759329552912018, 0.57240670447087982",
        "1.50, 1.50, 0.60837482372891105, 0.39162517627108895",
        "1.50, 2.0, 0.73853587005088939, 0.26146412994911061",
        "1.50, 4.0, 0.95398829431076859, 0.046011705689231366",
        "1.50, 10.0, 0.99983025756444721, 0.00016974243555282646",
        "2.0, 0.0, 0, 1",
        "2.0, 0.250, 0.026499021160743926, 0.97350097883925613",
        "2.0, 0.50, 0.090204010431049877, 0.90979598956895014",
        "2.0, 1.0, 0.26424111765711533, 0.73575888234288467",
        "2.0, 1.50, 0.44217459962892558, 0.55782540037107442",
        "2.0, 2.0, 0.59399415029016189, 0.40600584970983811",
        "2.0, 4.0, 0.90842180555632912, 0.091578194443670935",
        "2.0, 10.0, 0.99950060077261271, 0.00049939922738733366",
        "4.0, 0.0, 0, 1",
        "4.0, 0.250, 0.00013336965051406234, 0.99986663034948597",
        "4.0, 0.50, 0.001751622556290825, 0.9982483774437092",
        "4.0, 1.0, 0.018988156876153815, 0.98101184312384615",
        "4.0, 1.50, 0.065642454378450107, 0.93435754562154993",
        "4.0, 2.0, 0.1428765395014529, 0.85712346049854715",
        "4.0, 4.0, 0.56652987963329116, 0.4334701203667089",
        "4.0, 10.0, 0.98966394932407431, 0.010336050675925723",
        "10.0, 0.0, 0, 1",
        "10.0, 0.250, 2.0942485399973598e-13, 0.99999999999979061",
        "10.0, 0.50, 1.7096700293489097e-10, 0.99999999982903298",
        "10.0, 1.0, 1.1142547833872003e-07, 0.99999988857452171",
        "10.0, 1.50, 4.0975009763948422e-06, 0.99999590249902359",
        "10.0, 2.0, 4.6498075017263825e-05, 0.99995350192498278",
        "10.0, 4.0, 0.0081322427969338588, 0.99186775720306619",
        "10.0, 10.0, 0.54207028552814784, 0.4579297144718521",
    })
    void testRegularizedGamma(double a, double x, double p, double q) {
        final double actualP = RegularizedGamma.P.value(a, x);
        final double actualQ = RegularizedGamma.Q.value(a, x);
        final double eps = 1e-14;
        Assertions.assertEquals(p, actualP, p * eps);
        Assertions.assertEquals(q, actualQ, q * eps);
    }

    @Test
    void testRegularizedGammaMaxIterationsExceededThrows() {
        final double a = 1.0;
        final double x = 1.0;
        // OK without
        Assertions.assertEquals(0.632120558828558, RegularizedGamma.P.value(a, x), 1e-15);

        final int maxIterations = 3;
        Assertions.assertThrows(ArithmeticException.class, () ->
            RegularizedGamma.P.value(a, x, 1e-15, maxIterations));
    }

    /**
     * Assert the regularized gamma exactly matches value p. q is tested as 1-p.
     *
     * @param a Argument a
     * @param x Argument x
     * @param p Expected p
     */
    private static void assertRegularizedGamma(double a, double x, double p) {
        final double actualP = RegularizedGamma.P.value(a, x);
        final double actualQ = RegularizedGamma.Q.value(a, x);
        Assertions.assertEquals(p, actualP);
        Assertions.assertEquals(1 - p, actualQ);
    }
}
