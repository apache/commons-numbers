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
 * Tests for {@link Erf}.
 */
public class ErfTest {
    @Test
    public void testErf0() {
        double actual = Erf.value(0);
        double expected = 0;
        Assert.assertEquals(expected, actual, 1e-15);
        Assert.assertEquals(1 - expected, Erfc.value(0), 1e-15);
    }

    @Test
    public void testErf1960() {
        double x = 1.960 / Math.sqrt(2);
        double actual = Erf.value(x);
        double expected = 0.95;
        Assert.assertEquals(expected, actual, 1e-5);
        Assert.assertEquals(1 - actual, Erfc.value(x), 1e-15);

        actual = Erf.value(-x);
        expected = -expected;
        Assert.assertEquals(expected, actual, 1e-5);
        Assert.assertEquals(1 - actual, Erfc.value(-x), 1e-15);
    }

    @Test
    public void testErf2576() {
        double x = 2.576 / Math.sqrt(2);
        double actual = Erf.value(x);
        double expected = 0.99;
        Assert.assertEquals(expected, actual, 1e-5);
        Assert.assertEquals(1 - actual, Erfc.value(x), 1e-15);

        actual = Erf.value(-x);
        expected = -expected;
        Assert.assertEquals(expected, actual, 1e-5);
        Assert.assertEquals(1 - actual, Erfc.value(-x), 1e-15);
    }

    @Test
    public void testErf2807() {
        double x = 2.807 / Math.sqrt(2);
        double actual = Erf.value(x);
        double expected = 0.995;
        Assert.assertEquals(expected, actual, 1e-5);
        Assert.assertEquals(1 - actual, Erfc.value(x), 1e-15);

        actual = Erf.value(-x);
        expected = -expected;
        Assert.assertEquals(expected, actual, 1e-5);
        Assert.assertEquals(1 - actual, Erfc.value(-x), 1e-15);
    }

    @Test
    public void testErf3291() {
        double x = 3.291 / Math.sqrt(2);
        double actual = Erf.value(x);
        double expected = 0.999;
        Assert.assertEquals(expected, actual, 1e-5);
        Assert.assertEquals(1 - expected, Erfc.value(x), 1e-5);

        actual = Erf.value(-x);
        expected = -expected;
        Assert.assertEquals(expected, actual, 1e-5);
        Assert.assertEquals(1 - expected, Erfc.value(-x), 1e-5);
    }

    /**
     * MATH-301, MATH-456
     */
    @Test
    public void testLargeValues() {
        for (int i = 1; i < 200; i *= 10) {
            double result = Erf.value(i);
            Assert.assertFalse(Double.isNaN(result));
            Assert.assertTrue(result > 0 && result <= 1);
            result = Erf.value(-i);
            Assert.assertFalse(Double.isNaN(result));
            Assert.assertTrue(result >= -1 && result < 0);
            result = Erfc.value(i);
            Assert.assertFalse(Double.isNaN(result));
            Assert.assertTrue(result >= 0 && result < 1);
            result = Erfc.value(-i);
            Assert.assertFalse(Double.isNaN(result));
            Assert.assertTrue(result >= 1 && result <= 2);
        }
        Assert.assertEquals(-1, Erf.value(Double.NEGATIVE_INFINITY), 0);
        Assert.assertEquals(1, Erf.value(Double.POSITIVE_INFINITY), 0);
        Assert.assertEquals(2, Erfc.value(Double.NEGATIVE_INFINITY), 0);
        Assert.assertEquals(0, Erfc.value(Double.POSITIVE_INFINITY), 0);
    }

    /**
     * Compare Erf.value against reference values computed using GCC 4.2.1
     * (Apple OSX packaged version) erfl (extended precision erf).
     */
    @Test
    public void testErfGnu() {
        final double tol = 1E-15;
        final double[] gnuValues = new double[] {
            -1, -1, -1, -1, -1,
            -1, -1, -1, -0.99999999999999997848,
            -0.99999999999999264217, -0.99999999999846254017, -0.99999999980338395581, -0.99999998458274209971,
            -0.9999992569016276586, -0.99997790950300141459, -0.99959304798255504108, -0.99532226501895273415,
            -0.96610514647531072711, -0.84270079294971486948, -0.52049987781304653809,  0,
            0.52049987781304653809, 0.84270079294971486948, 0.96610514647531072711, 0.99532226501895273415,
            0.99959304798255504108, 0.99997790950300141459, 0.9999992569016276586, 0.99999998458274209971,
            0.99999999980338395581, 0.99999999999846254017, 0.99999999999999264217, 0.99999999999999997848,
            1,  1,  1,  1,
            1,  1,  1,  1};
        
        double x = -10;
        for (int i = 0; i < 41; i++) {
            Assert.assertEquals(gnuValues[i], Erf.value(x), tol);
            x += 0.5d;
        }
    }
}
