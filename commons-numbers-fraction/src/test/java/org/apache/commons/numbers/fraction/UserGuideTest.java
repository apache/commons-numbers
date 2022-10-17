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
package org.apache.commons.numbers.fraction;

import java.util.function.Supplier;
import org.apache.commons.numbers.fraction.GeneralizedContinuedFraction.Coefficient;
import org.apache.commons.numbers.fraction.GeneralizedContinuedFractionTest.Tan;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for examples contained in the user guide.
 */
class UserGuideTest {

    @Test
    void testFraction1() {
        int maxIterations = 100;

        // tolerance
        Fraction a = Fraction.from(0.6152, 0.02, maxIterations);
        Fraction b = Fraction.from(0.6152, 1.0e-7, maxIterations);
        Assertions.assertEquals(Fraction.of(3, 5), a);
        Assertions.assertEquals(Fraction.of(769, 1250), b);
        Assertions.assertEquals(0.6152, b.doubleValue(), "exact");

        // max denominator
        Fraction c = Fraction.from(0.6152, 9);
        Fraction d = Fraction.from(0.6152, 9999);
        Assertions.assertEquals(Fraction.of(3, 5), c);
        Assertions.assertEquals(Fraction.of(769, 1250), d);

        // largest possible value
        Fraction e = Fraction.from(Math.pow(2, 31));
        Assertions.assertEquals(Fraction.of(Integer.MIN_VALUE, -1), e);

        Assertions.assertThrows(ArithmeticException.class, () -> Fraction.from(1e10));
    }

    @Test
    void testFraction2() {
        Fraction f1 = Fraction.of(-240, 256);
        Fraction f2 = Fraction.of(15, -16);

        Assertions.assertEquals(-1, f1.signum());
        Assertions.assertTrue(f1.equals(f2));
        Assertions.assertEquals(0, f1.compareTo(f2));
        Assertions.assertEquals(-1, f1.compareTo(Fraction.ZERO));

        Assertions.assertEquals(0, Fraction.of(Integer.MIN_VALUE, Integer.MIN_VALUE).compareTo(Fraction.ONE));
    }

    @Test
    void testFraction3() {
        Fraction result = Fraction.of(1, 2).add(Fraction.of(1, 3))
                                           .add(Fraction.of(1, 6));
        Assertions.assertEquals(Fraction.ONE, result);
        Assertions.assertEquals(1.0, result.doubleValue(), "exact");
        Assertions.assertEquals(0.9999999999999999, 1.0 / 2 + 1.0 / 3 + 1.0 / 6, "inexact");
    }

    @Test
    void testFraction4() {
        double a = Fraction.of(1, 8).doubleValue();
        double b = Fraction.of(1, 3).doubleValue();
        int c = Fraction.of(8, 3).intValue();
        Assertions.assertEquals(0.125, a, "exact");
        Assertions.assertEquals(1.0 / 3, b, "inexact");
        Assertions.assertEquals(2, c, "inexact, whole number part of 2 2/3");
    }

    @Test
    void testBigFraction1() {
        BigFraction a = BigFraction.from(1.0 / 3);
        Assertions.assertEquals(BigFraction.of(6004799503160661L, 18014398509481984L), a);

        BigFraction b = BigFraction.from(1.0 / 3, 3);
        Assertions.assertEquals(BigFraction.of(1, 3), b);
    }

    /**
     * Test tan(z). See:
     * https://en.wikipedia.org/wiki/Trigonometric_functions#Continued_fraction_expansion
     */
    @Test
    void testContinuedFraction1() {
        final double eps = 1e-8;
        final ContinuedFraction cf = new ContinuedFraction() {
            @Override
            public double getA(int n, double x) {
                return n < 2 ? x : -x * x;
            }

            @Override
            public double getB(int n, double x) {
                return n == 0 ? 0 : 2 * n - 1;
            }
        };
        double z = 0.125;
        double result = cf.evaluate(z, eps);
        double expected = Math.tan(z);
        Assertions.assertEquals(expected, result, expected * eps * 10);
    }

    @Test
    void testGeneralizedContinuedFraction1() {
        double eps = 1e-10;
        // Golden ratio
        Supplier<Coefficient> gen = () -> Coefficient.of(1, 1);
        double gr1 = GeneralizedContinuedFraction.value(gen, eps);
        double gr2 = GeneralizedContinuedFraction.value(1, gen, eps);
        final double expected = 1.6180339887498948;
        Assertions.assertEquals(expected, gr1, expected * eps * 10);
        Assertions.assertEquals(expected, gr2, expected * eps * 10);
    }

    @Test
    void testGeneralizedContinuedFraction2() {
        double z = 0.125;
        double tan1 = GeneralizedContinuedFraction.value(0, new Tan(z));

        // Advance 1 term
        Tan t = new Tan(z);
        Coefficient c = t.get();
        double tan2 = c.getA() / GeneralizedContinuedFraction.value(c.getB(), t);

        // Avoid JDK variations in Math.tan(z)
        // https://keisan.casio.com/calculator
        double expected = 0.1256551365751309677927;
        Assertions.assertEquals(expected, tan1, 2 * Math.ulp(expected));
        Assertions.assertEquals(expected, tan2);
    }

    @Test
    void testGeneralizedContinuedFraction3() {
        double result1 = GeneralizedContinuedFraction.value(GeneralizedContinuedFractionTest.simpleContinuedFraction(4, 2, 6, 7));
        double result2 = GeneralizedContinuedFraction.value(4, GeneralizedContinuedFractionTest.simpleContinuedFraction(2, 6, 7));
        double expected = 415.0 / 93.0;
        Assertions.assertEquals(expected, result1, Math.ulp(expected));
        Assertions.assertEquals(expected, result2, Math.ulp(expected));
    }
}
