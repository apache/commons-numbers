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

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link LogGamma}.
 */
public class LogGammaTest {
    @Test
    public void testLogGammaNan() {
        testLogGamma(Double.NaN, Double.NaN);
    }

    @Test
    public void testLogGammaNegative() {
        testLogGamma(Double.NaN, -1.0);
    }

    @Test
    public void testLogGammaZero() {
        testLogGamma(Double.NaN, 0.0);
    }

    @Test
    public void testLogGammaPositive() {
        testLogGamma(0.6931471805599457, 3.0);
    }

    /**
     * Reference data for the {@link Gamma#logGamma(double)} function. This data
     * was generated with the following <a
     * href="http://maxima.sourceforge.net/">Maxima</a> script.
     *
     * <pre>
     * kill(all);
     *
     * fpprec : 64;
     * gamln(x) := log(gamma(x));
     * x : append(makelist(bfloat(i / 8), i, 1, 80),
     *     [0.8b0, 1b2, 1b3, 1b4, 1b5, 1b6, 1b7, 1b8, 1b9, 1b10]);
     *
     * for i : 1 while i <= length(x) do
     *     print("{", float(x[i]), ",", float(gamln(x[i])), "},");
     * </pre>
     */
    private static final double[][] LOG_GAMMA_REF = {
        { 0.125 , 2.019418357553796 },
        { 0.25 , 1.288022524698077 },
        { 0.375 , .8630739822706475 },
        { 0.5 , .5723649429247001 },
        { 0.625 , .3608294954889402 },
        { 0.75 , .2032809514312954 },
        { 0.875 , .08585870722533433 },
        { 0.890625 , .07353860936979656 },
        { 0.90625 , .06169536624059108 },
        { 0.921875 , .05031670080005688 },
        { 0.9375 , 0.0393909017345823 },
        { 0.953125 , .02890678734595923 },
        { 0.96875 , .01885367233441289 },
        { 0.984375 , .009221337197578781 },
        { 1.0 , 0.0 },
        { 1.015625 , - 0.00881970970573307 },
        { 1.03125 , - .01724677500176807 },
        { 1.046875 , - .02528981394675729 },
        { 1.0625 , - .03295710029357782 },
        { 1.078125 , - .04025658272400143 },
        { 1.09375 , - .04719590272716985 },
        { 1.109375 , - .05378241123619192 },
        { 1.125 , - .06002318412603958 },
        { 1.25 , - .09827183642181316 },
        { 1.375 , - .1177552707410788 },
        { 1.5 , - .1207822376352452 },
        { 1.625 , - .1091741337567954 },
        { 1.75 , - .08440112102048555 },
        { 1.875 , - 0.0476726853991883 },
        { 1.890625 , - .04229320615532515 },
        { 1.90625 , - .03674470657266143 },
        { 1.921875 , - .03102893865389552 },
        { 1.9375 , - .02514761940298887 },
        { 1.953125 , - .01910243184040138 },
        { 1.96875 , - .01289502598016741 },
        { 1.984375 , - .006527019770560387 },
        { 2.0 , 0.0 },
        { 2.015625 , .006684476830232185 },
        { 2.03125 , .01352488366498562 },
        { 2.046875 , .02051972208453692 },
        { 2.0625 , .02766752152285702 },
        { 2.078125 , 0.0349668385135861 },
        { 2.09375 , .04241625596251728 },
        { 2.109375 , .05001438244545164 },
        { 2.125 , .05775985153034387 },
        { 2.25 , .1248717148923966 },
        { 2.375 , .2006984603774558 },
        { 2.5 , .2846828704729192 },
        { 2.625 , .3763336820249054 },
        { 2.75 , .4752146669149371 },
        { 2.875 , .5809359740231859 },
        { 2.890625 , .5946142560817441 },
        { 2.90625 , .6083932548009232 },
        { 2.921875 , .6222723333588501 },
        { 2.9375 , .6362508628423761 },
        { 2.953125 , .6503282221022278 },
        { 2.96875 , .6645037976116387 },
        { 2.984375 , 0.678776983328359 },
        { 3.0 , .6931471805599453 },
        { 3.015625 , .7076137978322324 },
        { 3.03125 , .7221762507608962 },
        { 3.046875 , .7368339619260166 },
        { 3.0625 , 0.751586360749556 },
        { 3.078125 , .7664328833756681 },
        { 3.09375 , .7813729725537568 },
        { 3.109375 , .7964060775242092 },
        { 3.125 , 0.811531653906724 },
        { 3.25 , .9358019311087253 },
        { 3.375 , 1.06569589786406 },
        { 3.5 , 1.200973602347074 },
        { 3.625 , 1.341414578068493 },
        { 3.75 , 1.486815578593417 },
        { 3.875 , 1.6369886482725 },
        { 4.0 , 1.791759469228055 },
        { 4.125 , 1.950965937095089 },
        { 4.25 , 2.114456927450371 },
        { 4.375 , 2.282091222188554 },
        { 4.5 , 2.453736570842442 },
        { 4.625 , 2.62926886637513 },
        { 4.75 , 2.808571418575736 },
        { 4.875 , 2.99153431107781 },
        { 5.0 , 3.178053830347946 },
        { 5.125 , 3.368031956881733 },
        { 5.25 , 3.561375910386697 },
        { 5.375 , 3.757997741998131 },
        { 5.5 , 3.957813967618717 },
        { 5.625 , 4.160745237339519 },
        { 5.75 , 4.366716036622286 },
        { 5.875 , 4.57565441552762 },
        { 6.0 , 4.787491742782046 },
        { 6.125 , 5.002162481906205 },
        { 6.25 , 5.219603986990229 },
        { 6.375 , 5.439756316011858 },
        { 6.5 , 5.662562059857142 },
        { 6.625 , 5.887966185430003 },
        { 6.75 , 6.115915891431546 },
        { 6.875 , 6.346360475557843 },
        { 7.0 , 6.579251212010101 },
        { 7.125 , 6.814541238336996 },
        { 7.25 , 7.05218545073854 },
        { 7.375 , 7.292140407056348 },
        { 7.5 , 7.534364236758733 },
        { 7.625 , 7.778816557302289 },
        { 7.75 , 8.025458396315983 },
        { 7.875 , 8.274252119110479 },
        { 8.0 , 8.525161361065415 },
        { 8.125 , 8.77815096449171 },
        { 8.25 , 9.033186919605123 },
        { 8.375 , 9.290236309282232 },
        { 8.5 , 9.549267257300997 },
        { 8.625 , 9.810248879795765 },
        { 8.75 , 10.07315123968124 },
        { 8.875 , 10.33794530382217 },
        { 9.0 , 10.60460290274525 },
        { 9.125 , 10.87309669270751 },
        { 9.25 , 11.14340011995171 },
        { 9.375 , 11.41548738699336 },
        { 9.5 , 11.68933342079727 },
        { 9.625 , 11.96491384271319 },
        { 9.75 , 12.24220494005076 },
        { 9.875 , 12.52118363918365 },
        { 10.0 , 12.80182748008147 },
        { 0.8 , .1520596783998376 },
        { 100.0 , 359.1342053695754 },
        { 1000.0 , 5905.220423209181 },
        { 10000.0 , 82099.71749644238 },
        { 100000.0 , 1051287.708973657 },
        { 1000000.0 , 1.2815504569147612e+7 },
        { 10000000.0 , 1.511809493694739e+8 },
        { 1.e+8 , 1.7420680661038346e+9 },
        { 1.e+9 , 1.972326582750371e+10 },
        { 1.e+10 , 2.202585092888106e+11 },
    };

    @Test
    public void testLogGamma() {
        final int ulps = 3;
        for (int i = 0; i < LOG_GAMMA_REF.length; i++) {
            final double[] data = LOG_GAMMA_REF[i];
            final double x = data[0];
            final double expected = data[1];
            final double actual = LogGamma.value(x);
            final double tol;
            if (expected == 0.0) {
                tol = 1E-15;
            } else {
                tol = ulps * Math.ulp(expected);
            }
            Assert.assertEquals(Double.toString(x), expected, actual, tol);
        }
    }

    @Test
    public void testLogGammaPrecondition1() {
        Assert.assertTrue(Double.isNaN(LogGamma.value(0.0)));
    }

    @Test
    public void testLogGammaPrecondition2() {
        Assert.assertTrue(Double.isNaN(LogGamma.value(-1.0)));
    }

    private void testLogGamma(double expected, double x) {
        double actual = LogGamma.value(x);
        Assert.assertEquals(expected, actual, 1e-15);
    }
}
