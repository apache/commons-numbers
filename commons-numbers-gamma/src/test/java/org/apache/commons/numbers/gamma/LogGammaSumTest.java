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
 * Tests for {@link LogGammaSum}.
 */
public class LogGammaSumTest {
    /**
     * Reference data for the {@link LogGammaSum#value(double, double)}
     * function. This data was generated with the following
     * <a href="http://maxima.sourceforge.net/">Maxima</a> script.
     *
     * <pre>
     * kill(all);
     *
     * fpprec : 64;
     * gsumln(a, b) := log(gamma(a + b));
     *
     * x : [1.0b0, 1.125b0, 1.25b0, 1.375b0, 1.5b0, 1.625b0, 1.75b0, 1.875b0, 2.0b0];
     *
     * for i : 1 while i <= length(x) do
     *   for j : 1 while j <= length(x) do block(
     *     a : x[i],
     *     b : x[j],
     *     print("{", float(a), ",", float(b), ",", float(gsumln(a, b)), "},")
     *   );
     * </pre>
     */
    private static final double[][] LOG_GAMMA_SUM_REF = {
        { 1.0 , 1.0 , 0.0 },
        { 1.0 , 1.125 , .05775985153034387 },
        { 1.0 , 1.25 , .1248717148923966 },
        { 1.0 , 1.375 , .2006984603774558 },
        { 1.0 , 1.5 , .2846828704729192 },
        { 1.0 , 1.625 , .3763336820249054 },
        { 1.0 , 1.75 , .4752146669149371 },
        { 1.0 , 1.875 , .5809359740231859 },
        { 1.0 , 2.0 , .6931471805599453 },
        { 1.125 , 1.0 , .05775985153034387 },
        { 1.125 , 1.125 , .1248717148923966 },
        { 1.125 , 1.25 , .2006984603774558 },
        { 1.125 , 1.375 , .2846828704729192 },
        { 1.125 , 1.5 , .3763336820249054 },
        { 1.125 , 1.625 , .4752146669149371 },
        { 1.125 , 1.75 , .5809359740231859 },
        { 1.125 , 1.875 , .6931471805599453 },
        { 1.125 , 2.0 , 0.811531653906724 },
        { 1.25 , 1.0 , .1248717148923966 },
        { 1.25 , 1.125 , .2006984603774558 },
        { 1.25 , 1.25 , .2846828704729192 },
        { 1.25 , 1.375 , .3763336820249054 },
        { 1.25 , 1.5 , .4752146669149371 },
        { 1.25 , 1.625 , .5809359740231859 },
        { 1.25 , 1.75 , .6931471805599453 },
        { 1.25 , 1.875 , 0.811531653906724 },
        { 1.25 , 2.0 , .9358019311087253 },
        { 1.375 , 1.0 , .2006984603774558 },
        { 1.375 , 1.125 , .2846828704729192 },
        { 1.375 , 1.25 , .3763336820249054 },
        { 1.375 , 1.375 , .4752146669149371 },
        { 1.375 , 1.5 , .5809359740231859 },
        { 1.375 , 1.625 , .6931471805599453 },
        { 1.375 , 1.75 , 0.811531653906724 },
        { 1.375 , 1.875 , .9358019311087253 },
        { 1.375 , 2.0 , 1.06569589786406 },
        { 1.5 , 1.0 , .2846828704729192 },
        { 1.5 , 1.125 , .3763336820249054 },
        { 1.5 , 1.25 , .4752146669149371 },
        { 1.5 , 1.375 , .5809359740231859 },
        { 1.5 , 1.5 , .6931471805599453 },
        { 1.5 , 1.625 , 0.811531653906724 },
        { 1.5 , 1.75 , .9358019311087253 },
        { 1.5 , 1.875 , 1.06569589786406 },
        { 1.5 , 2.0 , 1.200973602347074 },
        { 1.625 , 1.0 , .3763336820249054 },
        { 1.625 , 1.125 , .4752146669149371 },
        { 1.625 , 1.25 , .5809359740231859 },
        { 1.625 , 1.375 , .6931471805599453 },
        { 1.625 , 1.5 , 0.811531653906724 },
        { 1.625 , 1.625 , .9358019311087253 },
        { 1.625 , 1.75 , 1.06569589786406 },
        { 1.625 , 1.875 , 1.200973602347074 },
        { 1.625 , 2.0 , 1.341414578068493 },
        { 1.75 , 1.0 , .4752146669149371 },
        { 1.75 , 1.125 , .5809359740231859 },
        { 1.75 , 1.25 , .6931471805599453 },
        { 1.75 , 1.375 , 0.811531653906724 },
        { 1.75 , 1.5 , .9358019311087253 },
        { 1.75 , 1.625 , 1.06569589786406 },
        { 1.75 , 1.75 , 1.200973602347074 },
        { 1.75 , 1.875 , 1.341414578068493 },
        { 1.75 , 2.0 , 1.486815578593417 },
        { 1.875 , 1.0 , .5809359740231859 },
        { 1.875 , 1.125 , .6931471805599453 },
        { 1.875 , 1.25 , 0.811531653906724 },
        { 1.875 , 1.375 , .9358019311087253 },
        { 1.875 , 1.5 , 1.06569589786406 },
        { 1.875 , 1.625 , 1.200973602347074 },
        { 1.875 , 1.75 , 1.341414578068493 },
        { 1.875 , 1.875 , 1.486815578593417 },
        { 1.875 , 2.0 , 1.6369886482725 },
        { 2.0 , 1.0 , .6931471805599453 },
        { 2.0 , 1.125 , 0.811531653906724 },
        { 2.0 , 1.25 , .9358019311087253 },
        { 2.0 , 1.375 , 1.06569589786406 },
        { 2.0 , 1.5 , 1.200973602347074 },
        { 2.0 , 1.625 , 1.341414578068493 },
        { 2.0 , 1.75 , 1.486815578593417 },
        { 2.0 , 1.875 , 1.6369886482725 },
        { 2.0 , 2.0 , 1.791759469228055 },
    };

    @Test
    public void testLogGammaSum() {
        final int ulps = 2;
        for (int i = 0; i < LOG_GAMMA_SUM_REF.length; i++) {
            final double[] ref = LOG_GAMMA_SUM_REF[i];
            final double a = ref[0];
            final double b = ref[1];
            final double expected = ref[2];
            final double actual = LogGammaSum.value(a, b);
            final double tol = ulps * Math.ulp(expected);
            final StringBuilder builder = new StringBuilder();
            builder.append(a).append(", ").append(b);
            Assert.assertEquals(builder.toString(), expected, actual, tol);
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void testLogGammaSumPrecondition1() {
        LogGammaSum.value(0, 1);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testLogGammaSumPrecondition2() {
        LogGammaSum.value(3, 1);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testLogGammaSumPrecondition3() {
        LogGammaSum.value(1, 0);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testLogGammaSumPrecondition4() {
        LogGammaSum.value(1, 3);
    }
}
