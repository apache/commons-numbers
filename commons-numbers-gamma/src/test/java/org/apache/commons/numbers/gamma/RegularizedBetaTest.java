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

import org.apache.commons.numbers.fraction.ContinuedFraction;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests for {@link RegularizedBeta}.
 */
public class RegularizedBetaTest {
    @Test
    public void testRegularizedBetaNanPositivePositive() {
        testRegularizedBeta(Double.NaN, Double.NaN, 1.0, 1.0);
    }

    @Test
    public void testRegularizedBetaPositiveNanPositive() {
        testRegularizedBeta(Double.NaN, 0.5, Double.NaN, 1.0);
    }

    @Test
    public void testRegularizedBetaPositivePositiveNan() {
        testRegularizedBeta(Double.NaN, 0.5, 1.0, Double.NaN);
    }

    @Test
    public void testRegularizedBetaNegativePositivePositive() {
        testRegularizedBeta(Double.NaN, -0.5, 1.0, 2.0);
    }

    @Test
    public void testRegularizedBetaPositiveNegativePositive() {
        testRegularizedBeta(Double.NaN, 0.5, -1.0, 2.0);
    }

    @Test
    public void testRegularizedBetaPositivePositiveNegative() {
        testRegularizedBeta(Double.NaN, 0.5, 1.0, -2.0);
    }

    @Test
    public void testRegularizedBetaZeroPositivePositive() {
        testRegularizedBeta(0.0, 0.0, 1.0, 2.0);
    }

    @Test
    public void testRegularizedBetaPositiveZeroPositive() {
        testRegularizedBeta(Double.NaN, 0.5, 0.0, 2.0);
    }

    @Test
    public void testRegularizedBetaPositivePositiveZero() {
        testRegularizedBeta(Double.NaN, 0.5, 1.0, 0.0);
    }

    @Test
    public void testRegularizedBetaPositivePositivePositive() {
        testRegularizedBeta(0.75, 0.5, 1.0, 2.0);
    }

    @Test
    public void testRegularizedBetaTinyArgument() {
        double actual = RegularizedBeta.value(1e-17, 1.0, 1e12);
        // This value is from R: pbeta(1e-17,1,1e12)
        assertEquals(9.999950000166648e-6, actual, 1e-16);
    }

    @Test
    public void testMath1067() {
        final double x = 0.22580645161290325;
        final double a = 64.33333333333334;
        final double b = 223;

        try {
            RegularizedBeta.value(x, a, b, 1e-14, 10000);
        } catch (StackOverflowError error) {
            fail("Infinite recursion");
        }
    }

    private void testRegularizedBeta(double expected,
                                     double x,
                                     double a,
                                     double b) {
        final double actual = RegularizedBeta.value(x, a, b);
        assertEquals(expected, actual, 1e-15);
    }

    @Test
    public void testValueTaking5ArgumentsThrowsArithmeticException() {

        try {
            RegularizedBeta.value(0.9972492333291466, 3668.4488, 9.795436556336103, Double.NaN, 387);
            fail("Expecting exception: ArithmeticException");
        } catch (ArithmeticException e) {
            assertEquals("maximal count (387) exceeded", e.getMessage());
            assertEquals(ContinuedFraction.class.getName(), e.getStackTrace()[0].getClassName());
        }

    }


    @Test
    public void testValueTakingFourArgumentsWithPositiveAndPositive() {

        assertEquals(Double.NaN, RegularizedBeta.value(2.0, 2.0, 1280.57004088342),0.01);

    }


    @Test
    public void testCreatesRegularizedBeta() {

        RegularizedBeta regularizedBeta = new RegularizedBeta();

    }


    @Test
    public void testValueTakingFourArgumentsThrowsArithmeticException() {

        try {
            RegularizedBeta.value(0.6946630178187274, 1582.7220438354161, Double.POSITIVE_INFINITY);
            fail("Expecting exception: ArithmeticException");
        } catch (ArithmeticException e) {
            assertEquals("Continued fraction diverged to NaN for value 0.695", e.getMessage());
            assertEquals(ContinuedFraction.class.getName(), e.getStackTrace()[0].getClassName());
        }

    }

}
