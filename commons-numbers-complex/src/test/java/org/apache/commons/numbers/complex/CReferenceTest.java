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

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Tests the functions defined by the C.99 standard for complex numbers
 * defined in ISO/IEC 9899, Annex G.
 *
 * <p>The test data is generated from a known implementation of the standard
 * and saved to the test resources data files.
 *
 * @see <a href="http://www.open-std.org/JTC1/SC22/WG14/www/standards">
 *    ISO/IEC 9899 - Programming languages - C</a>
 */
public class CReferenceTest {
    /**
     * The maximum units of least precision allowed between values.
     *
     * <p>In the normal use case this is set to zero and ignored.
     * It can be set to a non-zero value and it overrides the ulps values used in
     * each test if greater in magnitude. This can be used to output a report
     * of the ULPS between Complex and the reference data.
     */
    private static final long MAX_ULPS = 0;

    /**
     * Assert the two numbers are equal within the provided units of least precision.
     * The maximum count of numbers allowed between the two values is {@code maxUlps - 1}.
     *
     * @param msg the failure message
     * @param expected the expected
     * @param actual the actual
     * @param maxUlps the maximum units of least precision between the two values
     */
    private static void assertEquals(Supplier<String> msg, double expected, double actual, long maxUlps) {
        final long e = Double.doubleToLongBits(expected);
        final long a = Double.doubleToLongBits(actual);
        final long delta = Math.abs(e - a);
        if (delta > Math.abs(maxUlps)) {
            // DEBUG:
            if (maxUlps < 0) {
                // CHECKSTYLE: stop Regex
                System.out.printf("%s: expected <%s> != actual <%s> (ulps=%d)%n",
                        msg.get(), expected, actual, delta);
                // CHECKSTYLE: resume Regex
            } else {
                Assertions.fail(String.format("%s: expected <%s> != actual <%s> (ulps=%d)",
                        msg.get(), expected, actual, delta));
            }
        }
    }

    /**
     * Assert the operation on the complex number is equal to the expected value.
     *
     * <p>The results are considered equal within the provided units of least
     * precision. The maximum count of numbers allowed between the two values is
     * {@code maxUlps - 1}.
     *
     * @param c Input number.
     * @param operation the operation
     * @param expected Expected result.
     * @param maxUlps the maximum units of least precision between the two values
     */
    private static void assertComplex(Complex c,
            UnaryOperator<Complex> operation,
            Complex expected, long maxUlps) {
        final Complex z = operation.apply(c);
        assertEquals(() -> c + ": real", expected.real(), z.real(), maxUlps);
        assertEquals(() -> c + ": imaginary", expected.imag(), z.imag(), maxUlps);
    }

    /**
     * Assert the operation on the complex numbers is equal to the expected value.
     *
     * <p>The results are considered equal within the provided units of least
     * precision. The maximum count of numbers allowed between the two values is
     * {@code maxUlps - 1}.
     *
     * @param c1 First number.
     * @param c2 Second number.
     * @param operation the operation
     * @param expected Expected real part.
     * @param maxUlps the maximum units of least precision between the two values
     */
    private static void assertComplex(Complex c1, Complex c2,
            BiFunction<Complex, Complex, Complex> operation,
            Complex expected, long maxUlps) {
        final Complex z = operation.apply(c1, c2);
        assertEquals(() -> c1 + " op " + c2 + ": real", expected.real(), z.real(), maxUlps);
        assertEquals(() -> c1 + " op " + c2 + ": imaginary", expected.imag(), z.imag(), maxUlps);
    }

    /**
     * Assert the operation using the data loaded from test resources.
     *
     * @param testData Test data resource name.
     * @param operation the operation
     * @param maxUlps the maximum units of least precision between the two values
     */
    private static void assertOperation(String testData,
            UnaryOperator<Complex> operation, long maxUlps) {
        final List<Complex[]> data = TestUtils.loadTestData(testData);
        final long ulps = getTestUlps(maxUlps);
        for (final Complex[] pair : data) {
            assertComplex(pair[0], operation, pair[1], ulps);
        }
    }

    /**
     * Assert the operation using the data loaded from test resources.
     *
     * @param testData Test data resource name.
     * @param operation the operation
     * @param maxUlps the maximum units of least precision between the two values
     */
    private static void assertBiOperation(String testData,
            BiFunction<Complex, Complex, Complex> operation, long maxUlps) {
        final List<Complex[]> data = TestUtils.loadTestData(testData);
        final long ulps = getTestUlps(maxUlps);
        for (final Complex[] triple : data) {
            assertComplex(triple[0], triple[1], operation, triple[2], ulps);
        }
    }

    /**
     * Gets the test ulps. This uses the input value of the global setting if that is greater
     * in magnitude.
     *
     * @param ulps the ulps
     * @return the test ulps
     */
    private static long getTestUlps(long ulps) {
        final long max = Math.max(Math.abs(ulps), Math.abs(MAX_ULPS));
        // If either are negative then choose negative for debugging output
        return (ulps | MAX_ULPS) < 0 ? -max : max;
    }

    @Test
    public void testAcos() {
        assertOperation("data/acos.txt", Complex::acos, 36);
    }

    @Test
    public void testAcosh() {
        assertOperation("data/acosh.txt", Complex::acosh, 36);
    }

    @Test
    public void testAsinh() {
        // Odd function: negative real cases defined by positive real cases
        assertOperation("data/asinh.txt", Complex::asinh, 2);
    }

    @Test
    public void testAtanh() {
        // Odd function: negative real cases defined by positive real cases
        assertOperation("data/atanh.txt", Complex::atanh, 26);
    }

    @Test
    public void testCosh() {
        // Even function: negative real cases defined by positive real cases
        assertOperation("data/cosh.txt", Complex::cosh, 2);
    }

    @Test
    public void testSinh() {
        // Odd function: negative real cases defined by positive real cases
        assertOperation("data/sinh.txt", Complex::sinh, 2);
    }

    @Test
    public void testTanh() {
        // Odd function: negative real cases defined by positive real cases
        assertOperation("data/tanh.txt", Complex::tanh, 34);
    }

    @Test
    public void testExp() {
        assertOperation("data/exp.txt", Complex::exp, 2);
    }

    @Test
    public void testLog() {
        assertOperation("data/log.txt", Complex::log, 3);
    }

    @Test
    public void testSqrt() {
        assertOperation("data/sqrt.txt", Complex::sqrt, 1);
    }

    @Test
    public void testMultiply() {
        assertBiOperation("data/multiply.txt", Complex::multiply, 0);
    }

    @Test
    public void testDivide() {
        assertBiOperation("data/divide.txt", Complex::divide, 0);
    }

    @Test
    public void testPowComplex() {
        assertBiOperation("data/pow.txt", Complex::pow, 17);
    }
}
