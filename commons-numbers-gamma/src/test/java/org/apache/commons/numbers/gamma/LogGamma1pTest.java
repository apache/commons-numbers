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
 * Tests for {@link LogGamma1p}.
 */
public class LogGamma1pTest {
    private static final double[][] LOG_GAMMA1P_REF = {
        { - 0.5 , .5723649429247001 },
        { - 0.375 , .3608294954889402 },
        { - 0.25 , .2032809514312954 },
        { - 0.125 , .08585870722533433 },
        { 0.0 , 0.0 },
        { 0.125 , - .06002318412603958 },
        { 0.25 , - .09827183642181316 },
        { 0.375 , - .1177552707410788 },
        { 0.5 , - .1207822376352452 },
        { 0.625 , - .1091741337567954 },
        { 0.75 , - .08440112102048555 },
        { 0.875 , - 0.0476726853991883 },
        { 1.0 , 0.0 },
        { 1.125 , .05775985153034387 },
        { 1.25 , .1248717148923966 },
        { 1.375 , .2006984603774558 },
        { 1.5 , .2846828704729192 },
    };

    @Test
    public void testLogGamma1p() {
        final int ulps = 3;
        for (int i = 0; i < LOG_GAMMA1P_REF.length; i++) {
            final double[] ref = LOG_GAMMA1P_REF[i];
            final double x = ref[0];
            final double expected = ref[1];
            final double actual = LogGamma1p.value(x);
            final double tol = ulps * Math.ulp(expected);
            Assert.assertEquals(Double.toString(x), expected, actual, tol);
        }
    }

    @Test(expected=GammaException.class)
    public void testLogGamma1pPrecondition1() {
        LogGamma1p.value(-0.51);
    }

    @Test(expected=GammaException.class)
    public void testLogGamma1pPrecondition2() {
        LogGamma1p.value(1.51);
    }
}
