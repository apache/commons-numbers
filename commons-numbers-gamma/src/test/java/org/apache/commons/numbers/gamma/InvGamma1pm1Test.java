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
 * Tests for {@link InvGamma1pm1}.
 */
public class InvGamma1pm1Test {
    /**
     * <p>
     * Reference values for the {@link Gamma#invGamma1pm1(double)} method.
     * These values were generated with the following <a
     * href="http://maxima.sourceforge.net/">Maxima</a> script
     * </p>
     *
     * <pre>
     * kill(all);
     *
     * fpprec : 64;
     * gam1(x) := 1 / gamma(1 + x) - 1;
     * x : makelist(bfloat(i / 8), i, -4, 12);
     *
     * for i : 1 while i <= length(x) do print("{",
     *                                         float(x[i]),
     *                                         ",",
     *                                         float(gam1(x[i])),
     *                                         "},");
     * </pre>
     */
    private static final double[][] INV_GAMMA1P_M1_REF = {
        { -0.5 , -.4358104164522437 },
        { -0.375 , -.3029021533379859 },
        { -0.25 , -0.183951060901737 },
        { -0.125 , -.08227611018520711 },
        { 0.0 , 0.0 },
        { 0.125 , .06186116458306091 },
        { 0.25 , .1032626513208373 },
        { 0.375 , .1249687649039041 },
        { 0.5 , .1283791670955126 },
        { 0.625 , .1153565546592225 },
        { 0.75 , 0.0880652521310173 },
        { 0.875 , .04882730264547758 },
        { 1.0 , 0.0 },
        { 1.125 , -.05612340925950141 },
        { 1.25 , -.1173898789433302 },
        { 1.375 , -.1818408982517061 },
        { 1.5 , -0.247747221936325 },
    };

    @Test
    public void testInvGamma1pm1() {
        final int ulps = 3;
        for (int i = 0; i < INV_GAMMA1P_M1_REF.length; i++) {
            final double[] ref = INV_GAMMA1P_M1_REF[i];
            final double x = ref[0];
            final double expected = ref[1];
            final double actual = InvGamma1pm1.value(x);
            final double tol = ulps * Math.ulp(expected);
            Assert.assertEquals(Double.toString(x), expected, actual, tol);
        }
    }

    @Test(expected=GammaException.class)
    public void testInvGamma1pm1Precondition1() {
        InvGamma1pm1.value(-0.51);
    }

    @Test(expected=GammaException.class)
    public void testInvGamma1pm1Precondition2() {
        InvGamma1pm1.value(1.51);
    }
}
