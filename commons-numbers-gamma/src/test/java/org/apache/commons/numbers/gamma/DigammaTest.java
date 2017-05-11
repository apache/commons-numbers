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
 * Tests for {@link Digamma}.
 */
public class DigammaTest {
    @Test
    public void testDigammaLargeArgs() {
        double eps = 1e-8;
        Assert.assertEquals(4.6001618527380874002, Digamma.value(100), eps);
        Assert.assertEquals(3.9019896734278921970, Digamma.value(50), eps);
        Assert.assertEquals(2.9705239922421490509, Digamma.value(20), eps);
        Assert.assertEquals(2.9958363947076465821, Digamma.value(20.5), eps);
        Assert.assertEquals(2.2622143570941481605, Digamma.value(10.1), eps);
        Assert.assertEquals(2.1168588189004379233, Digamma.value(8.8), eps);
        Assert.assertEquals(1.8727843350984671394, Digamma.value(7), eps);
        Assert.assertEquals(0.42278433509846713939, Digamma.value(2), eps);
        Assert.assertEquals(-100.56088545786867450, Digamma.value(0.01), eps);
        Assert.assertEquals(-4.0390398965921882955, Digamma.value(-0.8), eps);
        Assert.assertEquals(4.2003210041401844726, Digamma.value(-6.3), eps);
        Assert.assertEquals(-3.110625123035E-5, Digamma.value(1.4616), eps);
    }

    @Test
    public void testDigammaSmallArgs() {
        // values for negative powers of 10 from 1 to 30 as computed by webMathematica with 20 digits
        // see functions.wolfram.com
        double[] expected = {-10.423754940411076795, -100.56088545786867450, -1000.5755719318103005,
                -10000.577051183514335, -100000.57719921568107, -1.0000005772140199687e6, -1.0000000577215500408e7,
                -1.0000000057721564845e8, -1.0000000005772156633e9, -1.0000000000577215665e10, -1.0000000000057721566e11,
                -1.0000000000005772157e12, -1.0000000000000577216e13, -1.0000000000000057722e14, -1.0000000000000005772e15, -1e+16,
                -1e+17, -1e+18, -1e+19, -1e+20, -1e+21, -1e+22, -1e+23, -1e+24, -1e+25, -1e+26,
                -1e+27, -1e+28, -1e+29, -1e+30};
        for (double n = 1; n < 30; n++) {
            checkRelativeError(String.format("Test %.0f: ", n), expected[(int) (n - 1)], Digamma.value(Math.pow(10.0, -n)), 1e-8);
        }
    }

    @Test
    public void testDigammaNonRealArgs() {
        Assert.assertTrue(Double.isNaN(Digamma.value(Double.NaN)));
        Assert.assertTrue(Double.isInfinite(Digamma.value(Double.POSITIVE_INFINITY)));
        Assert.assertTrue(Double.isInfinite(Digamma.value(Double.NEGATIVE_INFINITY)));
    }

    private void checkRelativeError(String msg,
                                    double expected,
                                    double actual,
                                    double tolerance) {
        Assert.assertEquals(msg, expected, actual, Math.abs(tolerance * actual));
    }
}

