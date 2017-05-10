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
 * Tests for {@link Trigamma}.
 */
public class TrigammaTest {
    @Test
    public void testTrigamma() {
        double eps = 1e-8;
        // computed using webMathematica.  For example, to compute trigamma($i) = Polygamma(1, $i), use
        //
        // http://functions.wolfram.com/webMathematica/Evaluated.jsp?name=PolyGamma2&plottype=0&vars={%221%22,%22$i%22}&digits=20
        double[] data = {
                1e-4, 1.0000000164469368793e8,
                1e-3, 1.0000016425331958690e6,
                1e-2, 10001.621213528313220,
                1e-1, 101.43329915079275882,
                1, 1.6449340668482264365,
                2, 0.64493406684822643647,
                3, 0.39493406684822643647,
                4, 0.28382295573711532536,
                5, 0.22132295573711532536,
                10, 0.10516633568168574612,
                20, 0.051270822935203119832,
                50, 0.020201333226697125806,
                100, 0.010050166663333571395
        };
        for (int i = data.length - 2; i >= 0; i -= 2) {
            Assert.assertEquals(String.format("trigamma %.0f", data[i]), data[i + 1], Trigamma.value(data[i]), eps);
        }
    }

    @Test
    public void testTrigammaNonRealArgs() {
        Assert.assertTrue(Double.isNaN(Trigamma.value(Double.NaN)));
        Assert.assertTrue(Double.isInfinite(Trigamma.value(Double.POSITIVE_INFINITY)));
        Assert.assertTrue(Double.isInfinite(Trigamma.value(Double.NEGATIVE_INFINITY)));
    }
}

