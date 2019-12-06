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

package org.apache.commons.numbers.complex;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the functions defined by the C.99 standard for complex numbers
 * defined in ISO/IEC 9899, Annex G.
 *
 * <p>The test data is generated from a known implementation of the standard: GNU g++.
 *
 * @see <a href="http://www.open-std.org/JTC1/SC22/WG14/www/standards">
 *    ISO/IEC 9899 - Programming languages - C</a>
 */
public class CReferenceTest {

    /**
     * Assert the two numbers are equal to within floating-point error.
     * Two values are considered equal if there are no floating-point values between them.
     *
     * @param name the name of the number
     * @param expected the expected
     * @param actual the actual
     * @param maxUlps the maximum units of least precision between the two values
     */
    private static void assertEquals(String name, double expected, double actual) {
        assertEquals(name, expected, actual, 1);
    }

    /**
     * Assert the two numbers are equal within the provided units of least precision.
     * The maximum count of numbers allowed between the two values is {@code maxUlps - 1}.
     *
     * @param name the name of the number
     * @param expected the expected
     * @param actual the actual
     * @param maxUlps the maximum units of least precision between the two values
     */
    private static void assertEquals(String name, double expected, double actual, long maxUlps) {
        final long e = Double.doubleToLongBits(expected);
        final long a = Double.doubleToLongBits(actual);
        final long delta = Math.abs(e - a);
        if (delta > maxUlps) {
            Assertions.fail(String.format("%s: %s != %s (ulps=%d)", name, expected, actual, delta));
        }
    }

    @Test
    public void testAcos() {
        final Complex z1 = Complex.ofCartesian(2, 3).acos();
        assertEquals("real", 1.0001435424737972, z1.getReal());
        assertEquals("imaginary", -1.9833870299165355, z1.getImaginary());
        final Complex z2 = Complex.ofCartesian(0, 0.75).acos();
        assertEquals("real", 1.5707963267948966, z2.getReal());
        assertEquals("imaginary", -0.69314718055994529, z2.getImaginary());
        final Complex z3 = Complex.ofCartesian(0.75, 0).acos();
        assertEquals("real", 0.72273424781341566, z3.getReal());
        assertEquals("imaginary", -0, z3.getImaginary());
    }

    @Test
    public void testAcosh() {
        final Complex z1 = Complex.ofCartesian(2, 3).acosh();
        assertEquals("real", 1.9833870299165355, z1.getReal());
        assertEquals("imaginary", 1.0001435424737972, z1.getImaginary());
        final Complex z2 = Complex.ofCartesian(0, 0.75).acosh();
        assertEquals("real", 0.69314718055994529, z2.getReal());
        assertEquals("imaginary", 1.5707963267948966, z2.getImaginary());
        final Complex z3 = Complex.ofCartesian(0.75, 0).acosh();
        assertEquals("real", 0, z3.getReal());
        // TODO: Fix this test. The sign is currently incorrect.
        //assertEquals("imaginary", 0.72273424781341566, z3.getImaginary());
    }

    @Test
    public void testAsinh() {
        final Complex z1 = Complex.ofCartesian(2, 3).asinh();
        assertEquals("real", 1.9686379257930964, z1.getReal());
        assertEquals("imaginary", 0.96465850440760281, z1.getImaginary());
        final Complex z2 = Complex.ofCartesian(0, 0.75).asinh();
        assertEquals("real", 0, z2.getReal());
        assertEquals("imaginary", 0.848062078981481, z2.getImaginary());
        final Complex z3 = Complex.ofCartesian(0.75, 0).asinh();
        assertEquals("real", 0.69314718055994529, z3.getReal());
        assertEquals("imaginary", 0, z3.getImaginary());
    }

    @Test
    public void testAtanh() {
        final Complex z1 = Complex.ofCartesian(2, 3).atanh();
        assertEquals("real", 0.14694666622552977, z1.getReal());
        assertEquals("imaginary", 1.3389725222944935, z1.getImaginary());
        final Complex z2 = Complex.ofCartesian(0, 0.75).atanh();
        assertEquals("real", 0, z2.getReal());
        assertEquals("imaginary", 0.64350110879328437, z2.getImaginary());
        final Complex z3 = Complex.ofCartesian(0.75, 0).atanh();
        assertEquals("real", 0.97295507452765662, z3.getReal());
        assertEquals("imaginary", 0, z3.getImaginary());
    }

    @Test
    public void testCosh() {
        final Complex z1 = Complex.ofCartesian(2, 3).cosh();
        assertEquals("real", -3.7245455049153224, z1.getReal());
        assertEquals("imaginary", 0.51182256998738462, z1.getImaginary());
        final Complex z2 = Complex.ofCartesian(0, 0.75).cosh();
        assertEquals("real", 0.7316888688738209, z2.getReal());
        assertEquals("imaginary", 0, z2.getImaginary());
        final Complex z3 = Complex.ofCartesian(0.75, 0).cosh();
        assertEquals("real", 1.2946832846768448, z3.getReal());
        assertEquals("imaginary", 0, z3.getImaginary());
    }

    @Test
    public void testSinh() {
        final Complex z1 = Complex.ofCartesian(2, 3).sinh();
        assertEquals("real", -3.5905645899857799, z1.getReal());
        assertEquals("imaginary", 0.53092108624851975, z1.getImaginary());
        final Complex z2 = Complex.ofCartesian(0, 0.75).sinh();
        assertEquals("real", 0, z2.getReal());
        assertEquals("imaginary", 0.68163876002333412, z2.getImaginary());
        final Complex z3 = Complex.ofCartesian(0.75, 0).sinh();
        assertEquals("real", 0.82231673193582999, z3.getReal());
        assertEquals("imaginary", 0, z3.getImaginary());
    }

    @Test
    public void testTanh() {
        final Complex z1 = Complex.ofCartesian(2, 3).tanh();
        assertEquals("real", 0.96538587902213302, z1.getReal());
        assertEquals("imaginary", -0.0098843750383224918, z1.getImaginary());
        final Complex z2 = Complex.ofCartesian(0, 0.75).tanh();
        assertEquals("real", 0, z2.getReal());
        assertEquals("imaginary", 0.93159645994407225, z2.getImaginary(), 3);
        final Complex z3 = Complex.ofCartesian(0.75, 0).tanh();
        assertEquals("real", 0.63514895238728741, z3.getReal());
        assertEquals("imaginary", 0, z3.getImaginary());
    }

    @Test
    public void testExp() {
        final Complex z1 = Complex.ofCartesian(2, 3).exp();
        assertEquals("real", -7.3151100949011028, z1.getReal());
        assertEquals("imaginary", 1.0427436562359045, z1.getImaginary());
        final Complex z2 = Complex.ofCartesian(0, 0.75).exp();
        assertEquals("real", 0.7316888688738209, z2.getReal());
        assertEquals("imaginary", 0.68163876002333412, z2.getImaginary());
        final Complex z3 = Complex.ofCartesian(0.75, 0).exp();
        assertEquals("real", 2.1170000166126748, z3.getReal());
        assertEquals("imaginary", 0, z3.getImaginary());
    }

    @Test
    public void testLog() {
        final Complex z1 = Complex.ofCartesian(2, 3).log();
        assertEquals("real", 1.2824746787307684, z1.getReal());
        assertEquals("imaginary", 0.98279372324732905, z1.getImaginary());
        final Complex z2 = Complex.ofCartesian(0, 0.75).log();
        assertEquals("real", -0.2876820724517809, z2.getReal());
        assertEquals("imaginary", 1.5707963267948966, z2.getImaginary());
        final Complex z3 = Complex.ofCartesian(0.75, 0).log();
        assertEquals("real", -0.2876820724517809, z3.getReal());
        assertEquals("imaginary", 0, z3.getImaginary());
    }

    @Test
    public void testSqrt() {
        final Complex z1 = Complex.ofCartesian(2, 3).sqrt();
        assertEquals("real", 1.6741492280355401, z1.getReal());
        assertEquals("imaginary", 0.89597747612983814, z1.getImaginary());
        final Complex z2 = Complex.ofCartesian(0, 0.75).sqrt();
        assertEquals("real", 0.61237243569579447, z2.getReal());
        assertEquals("imaginary", 0.61237243569579447, z2.getImaginary());
        final Complex z3 = Complex.ofCartesian(0.75, 0).sqrt();
        assertEquals("real", 0.8660254037844386, z3.getReal());
        assertEquals("imaginary", 0, z3.getImaginary());
    }


    @Test
    public void testMultiply() {
        final Complex c1 = Complex.ofCartesian(2, 3);
        final Complex c2 = Complex.ofCartesian(5, 4);
        final Complex z1 = c1.multiply(c2);
        assertEquals("real", -2, z1.getReal());
        assertEquals("imaginary", 23, z1.getImaginary());
        final Complex z2 = c1.conjugate().multiply(c2);
        assertEquals("real", 22, z2.getReal());
        assertEquals("imaginary", -7, z2.getImaginary());
        final Complex z3 = c2.multiply(c1);
        assertEquals("real", -2, z3.getReal());
        assertEquals("imaginary", 23, z3.getImaginary());
        final Complex z4 = c2.conjugate().multiply(c1);
        assertEquals("real", 22, z4.getReal());
        assertEquals("imaginary", 7, z4.getImaginary());
    }

    @Test
    public void testDivide() {
        final Complex c1 = Complex.ofCartesian(2, 3);
        final Complex c2 = Complex.ofCartesian(5, 4);
        final Complex z1 = c1.divide(c2);
        assertEquals("real", 0.53658536585365868, z1.getReal());
        assertEquals("imaginary", 0.17073170731707318, z1.getImaginary());
        final Complex z2 = c1.conjugate().divide(c2);
        assertEquals("real", -0.048780487804878099, z2.getReal(), 7);
        assertEquals("imaginary", -0.56097560975609762, z2.getImaginary());
        final Complex z3 = c2.divide(c1);
        assertEquals("real", 1.6923076923076923, z3.getReal());
        assertEquals("imaginary", -0.53846153846153855, z3.getImaginary());
        final Complex z4 = c2.conjugate().divide(c1);
        assertEquals("real", -0.15384615384615394, z4.getReal(), 3);
        assertEquals("imaginary", -1.7692307692307692, z4.getImaginary());
    }

    @Test
    public void testPowComplex() {
        final Complex c1 = Complex.ofCartesian(2, 3);
        final Complex c2 = Complex.ofCartesian(5, 4);
        final Complex z1 = c1.pow(c2);
        assertEquals("real", -9.7367145095888414, z1.getReal());
        assertEquals("imaginary", -6.9377513609299868, z1.getImaginary());
        final Complex z2 = c1.conjugate().pow(c2);
        assertEquals("real", 30334.832969842264, z2.getReal());
        assertEquals("imaginary", 6653.9414970320349, z2.getImaginary());
        final Complex z3 = c2.pow(c1);
        assertEquals("real", 4.3549103166315382, z3.getReal(), 2);
        assertEquals("imaginary", 3.2198331430252156, z3.getImaginary(), 8);
        final Complex z4 = c2.conjugate().pow(c1);
        assertEquals("real", -146.48661898442663, z4.getReal(), 9);
        assertEquals("imaginary", -273.63651239033993, z4.getImaginary(), 2);
    }

    @Test
    public void testPowScalar() {
        final Complex c1 = Complex.ofCartesian(2, 3);
        final double d = 5;
        final Complex z1 = c1.pow(d);
        assertEquals("real", 122, z1.getReal());
        assertEquals("imaginary", -597, z1.getImaginary());
        final Complex z2 = c1.conjugate().pow(d);
        assertEquals("real", 122, z2.getReal());
        assertEquals("imaginary", 597, z2.getImaginary());
    }
}
