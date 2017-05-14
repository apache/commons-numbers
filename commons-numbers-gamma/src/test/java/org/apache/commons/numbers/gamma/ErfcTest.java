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
 * Tests for {@link Erfc}.
 */
public class ErfcTest {
    /**
     * Compare erfc against reference values computed using GCC 4.2.1
     * (Apple OSX packaged version) erfcl (extended precision erfc).
     */
    @Test
    public void testErfcGnu() {
        final double tol = 1e-15;
        final double[] gnuValues = new double[] {
            2,  2,  2,  2,  2,
            2,  2,  2, 1.9999999999999999785,
            1.9999999999999926422, 1.9999999999984625402, 1.9999999998033839558, 1.9999999845827420998,
            1.9999992569016276586, 1.9999779095030014146, 1.9995930479825550411, 1.9953222650189527342,
            1.9661051464753107271, 1.8427007929497148695, 1.5204998778130465381,  1,
            0.47950012218695346194, 0.15729920705028513051, 0.033894853524689272893, 0.0046777349810472658333,
            0.00040695201744495893941, 2.2090496998585441366E-05, 7.4309837234141274516E-07, 1.5417257900280018858E-08,
            1.966160441542887477E-10, 1.5374597944280348501E-12, 7.3578479179743980661E-15, 2.1519736712498913103E-17,
            3.8421483271206474691E-20, 4.1838256077794144006E-23, 2.7766493860305691016E-26, 1.1224297172982927079E-29,
            2.7623240713337714448E-33, 4.1370317465138102353E-37, 3.7692144856548799402E-41, 2.0884875837625447567E-45
        };

        double x = -10;
        for (int i = 0; i < 41; i++) {
            Assert.assertEquals(gnuValues[i], Erfc.value(x), tol);
            x += 0.5d;
        }
    }

    /**
     * Tests erfc against reference data computed using Maple reported in Marsaglia, G,,
     * "Evaluating the Normal Distribution," Journal of Statistical Software, July, 2004.
     * http//www.jstatsoft.org/v11/a05/paper
     */
    @Test
    public void testErfcMaple() {
        double[][] ref = new double[][] {
            {0.1, 4.60172162722971e-01},
            {1.2, 1.15069670221708e-01},
            {2.3, 1.07241100216758e-02},
            {3.4, 3.36929265676881e-04},
            {4.5, 3.39767312473006e-06},
            {5.6, 1.07175902583109e-08},
            {6.7, 1.04209769879652e-11},
            {7.8, 3.09535877195870e-15},
            {8.9, 2.79233437493966e-19},
            {10.0, 7.61985302416053e-24},
            {11.1, 6.27219439321703e-29},
            {12.2, 1.55411978638959e-34},
            {13.3, 1.15734162836904e-40},
            {14.4, 2.58717592540226e-47},
            {15.5, 1.73446079179387e-54},
            {16.6, 3.48454651995041e-62}
        };

        for (int i = 0; i < 15; i++) {
            final double result = 0.5 * Erfc.value(ref[i][0] / Math.sqrt(2));
            Assert.assertEquals(ref[i][1], result, 1e-15);
            Assert.assertEquals(1, ref[i][1] / result, 1e-13);
        }
    }
}
