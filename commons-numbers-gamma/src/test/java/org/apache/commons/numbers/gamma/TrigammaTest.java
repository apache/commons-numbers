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

/**
 * Tests for {@link Trigamma}.
 */
class TrigammaTest {
    @Test
    void testTrigamma() {
        final double eps = 1e-9; // Allowed relative error.
        // computed using webMathematica.  For example, to compute trigamma($i) = Polygamma(1, $i), use
        //
        // http://functions.wolfram.com/webMathematica/Evaluated.jsp?name=PolyGamma2&plottype=0&vars={%221%22,%22$i%22}&digits=20
        final double[] data = {
            -98765.4321, 10.332673372988805424,
            -100.5, 9.8597034918700861520,
            -50.5, 9.8499971860824842274,
            -20.5, 9.8219943446498794821,
            -10.5, 9.7787577398148123845,
            -5.5, 9.7033198653394003812,
            -2.5, 9.5392466449891237539,
            -0.5, 8.9348022005446793094,
            -1e-1, 101.92253995947720352,
            -1e-2, 10001.669304101071825,
            -1e-3, 1.0000016473414317771e6,
            -1e-4, 1.0000000164517451070e8,
            -1e-5, 1.0000000001644958108e10,
            1e-11, 1e22,
            1e-10, 1e20,
            1e-9, 1.0000000000000000016e18,
            1e-8, 1.0000000000000001645e16,
            1e-7, 1.0000000000000164493e14,
            1e-6, 1.0000000000016449317e12,
            1e-5, 1.0000000001644910026e10,
            1e-4, 1.0000000164469368793e8,
            1e-3, 1.0000016425331958690e6,
            1e-2, 10001.621213528313220,
            1e-1, 101.43329915079275882,
            1, 1.6449340668482264365,
            1.5, 0.93480220054467930942,
            2, 0.64493406684822643647,
            2.5, 0.49035775610023486497,
            3, 0.39493406684822643647,
            3.5, 0.33035775610023486497,
            4, 0.28382295573711532536,
            4.5, 0.24872510303901037518,
            5, 0.22132295573711532536,
            7.5, 0.14261589669670379977,
            10, 0.10516633568168574612,
            20, 0.051270822935203119832,
            50, 0.020201333226697125806,
            100, 0.010050166663333571395,
            12345.6789, 0.000081003281325733214110
        };
        for (int i = data.length - 2; i >= 0; i -= 2) {
            final double value = data[i];
            final double expected = data[i + 1];
            Assertions.assertEquals(1, Trigamma.value(value) / expected, eps, () -> "trigamma " + value);
        }
    }

    @Test
    void testTrigammaZero() {
        Assertions.assertEquals(Double.POSITIVE_INFINITY, Trigamma.value(0), 0d);
    }

    @Test
    void testTrigammaNonRealArgs() {
        Assertions.assertTrue(Double.isNaN(Trigamma.value(Double.NaN)));
        Assertions.assertTrue(Double.isInfinite(Trigamma.value(Double.POSITIVE_INFINITY)));
        Assertions.assertTrue(Double.isInfinite(Trigamma.value(Double.NEGATIVE_INFINITY)));
    }
}

