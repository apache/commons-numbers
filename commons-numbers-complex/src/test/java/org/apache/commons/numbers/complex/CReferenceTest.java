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

import org.apache.commons.numbers.complex.TestUtils.TestDataFlagOption;
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
class CReferenceTest {
    /** Positive zero bits. */
    private static final long POSITIVE_ZERO_DOUBLE_BITS = Double.doubleToRawLongBits(+0.0);
    /** Negative zero bits. */
    private static final long NEGATIVE_ZERO_DOUBLE_BITS = Double.doubleToRawLongBits(-0.0);

    /**
     * The maximum units of least precision (ULPs) allowed between values.
     * This is a global setting used to override individual test settings for ULPs as follows:
     *
     * <ul>
     * <li>In the normal use case this is set to zero and ignored.
     * <li>If the sign matches the setting of the test then the larger magnitude is used.
     * <li>If the global setting is negative and the test setting is positive then it overrides
     * the individual test setting for reporting purposes.
     * <li>If the global setting is positive and the test setting is negative then the test
     * setting takes precedence.
     * </ul>
     *
     * <p>During testing the difference between an expected and actual result is specified by the
     * count of numbers in the range between them lower end exclusive and upper end inclusive.
     * Two equal numbers have a count of 0. The next adjacent value has a count of 1.
     *
     * <p>If the configured ULPs is positive then the test is
     * asserted using: {@code delta <= maxUlps}.
     *
     * <p>If the configured ULPs is negative the test is asserted using:
     * {@code delta <= (|maxUlps|-1)}. This allows setting a ULPs of -1 to indicate
     * maximum ULPs = 0 but flagging the assertion for special processing.
     *
     * <p>If the maximum ULPs is positive then an assertion error is raised.
     * If negative then the error is printed to System out. This allows reporting of large
     * deviations between the library and the reference data.
     *
     * <p>In a standard use-case all tests will have a configured positive maximum ULPs to
     * pass the current test data. The global setting can be set to a negative value to allow
     * reporting of errors larger in magnitude to the console. Setting -1 will output all
     * differences. Setting -2 will output only those with a value between the two numbers,
     * i.e. the numbers are not the same to floating point roundoff.
     *
     * <p>Setting the global maximum ULPs to negative has the second effect of loading all
     * data that has been flagged in data files using the {@code ;} character.
     * Otherwise this data is ignored by testing and printed to System out.
     */
    private static long globalMaxUlps = 0;

    /** Set this to true to report all deviations to System out when the maximum ULPs is negative. */
    private static boolean reportAllDeviations = false;

    /**
     * Assert the two numbers are equal within the provided units of least precision.
     * The maximum count of numbers allowed between the two values is {@code maxUlps - 1}.
     *
     * <p>Numbers must have the same sign. Thus -0.0 and 0.0 are never equal.
     *
     * @param msg the failure message
     * @param expected the expected
     * @param actual the actual
     * @param maxUlps the maximum units of least precision between the two values
     */
    static void assertEquals(Supplier<String> msg, double expected, double actual, long maxUlps) {
        final long e = Double.doubleToLongBits(expected);
        final long a = Double.doubleToLongBits(actual);

        // Code adapted from Precision#equals(double, double, int) so we maintain the delta
        // for the message.

        long delta;
        boolean equal;
        if (e == a) {
            // Binary equal
            equal = true;
            delta = 0;
        } else if ((a ^ e) < 0L) {
            // Opposite signs are never equal.
            equal = false;
            // The difference is the count of numbers between each and zero.
            // This may overflow but we report it using an unsigned formatter.
            if (a < e) {
                delta = (e - POSITIVE_ZERO_DOUBLE_BITS) + (a - NEGATIVE_ZERO_DOUBLE_BITS) + 1;
            } else {
                delta = (a - POSITIVE_ZERO_DOUBLE_BITS) + (e - NEGATIVE_ZERO_DOUBLE_BITS) + 1;
            }
        } else {
            delta = Math.abs(e - a);
            // Allow input of a negative maximum ULPs
            equal = delta <= ((maxUlps < 0) ? (-maxUlps - 1) : maxUlps);
        }

        // DEBUG:
        if (maxUlps < 0) {
            // CHECKSTYLE: stop Regex
            if (!equal) {
                System.out.printf("%s: expected <%s> != actual <%s> (ulps=%s)%n",
                        msg.get(), expected, actual, Long.toUnsignedString(delta));
            } else if (reportAllDeviations) {
                System.out.printf("%s: expected <%s> == actual <%s> (ulps=0)%n",
                    msg.get(), expected, actual);
            }
            // CHECKSTYLE: resume Regex
            return;
        }

        if (!equal) {
            Assertions.fail(String.format("%s: expected <%s> != actual <%s> (ulps=%s)",
                    msg.get(), expected, actual, Long.toUnsignedString(delta)));
        }
    }

    /**
     * Assert the operation on the complex number is equal to the expected value.
     *
     * <p>The results are considered equal within the provided units of least
     * precision. The maximum count of numbers allowed between the two values is
     * {@code maxUlps - 1}.
     *
     * <p>Numbers must have the same sign. Thus -0.0 and 0.0 are never equal.
     *
     * @param c Input number.
     * @param name the operation name
     * @param operation the operation
     * @param expected Expected result.
     * @param maxUlps the maximum units of least precision between the two values
     */
    static void assertComplex(Complex c,
            String name, UnaryOperator<Complex> operation,
            Complex expected, long maxUlps) {
        final Complex z = operation.apply(c);
        assertEquals(() -> c + "." + name + "(): real", expected.real(), z.real(), maxUlps);
        assertEquals(() -> c + "." + name + "(): imaginary", expected.imag(), z.imag(), maxUlps);
    }

    /**
     * Assert the operation on the complex numbers is equal to the expected value.
     *
     * <p>The results are considered equal within the provided units of least
     * precision. The maximum count of numbers allowed between the two values is
     * {@code maxUlps - 1}.
     *
     * <p>Numbers must have the same sign. Thus -0.0 and 0.0 are never equal.
     *
     * @param c1 First number.
     * @param c2 Second number.
     * @param name the operation name
     * @param operation the operation
     * @param expected Expected real part.
     * @param maxUlps the maximum units of least precision between the two values
     */
    static void assertComplex(Complex c1, Complex c2,
            String name, BiFunction<Complex, Complex, Complex> operation,
            Complex expected, long maxUlps) {
        final Complex z = operation.apply(c1, c2);
        assertEquals(() -> c1 + "." + name + c2 + ": real", expected.real(), z.real(), maxUlps);
        assertEquals(() -> c1 + "." + name + c2 + ": imaginary", expected.imag(), z.imag(), maxUlps);
    }

    /**
     * Assert the operation using the data loaded from test resources.
     *
     * @param name the operation name
     * @param operation the operation
     * @param maxUlps the maximum units of least precision between the two values
     */
    private static void assertOperation(String name,
            UnaryOperator<Complex> operation, long maxUlps) {
        final List<Complex[]> data = loadTestData(name);
        final long ulps = getTestUlps(maxUlps);
        for (final Complex[] pair : data) {
            assertComplex(pair[0], name, operation, pair[1], ulps);
        }
    }

    /**
     * Assert the operation using the data loaded from test resources.
     *
     * @param name the operation name
     * @param operation the operation
     * @param maxUlps the maximum units of least precision between the two values
     */
    private static void assertBiOperation(String name,
            BiFunction<Complex, Complex, Complex> operation, long maxUlps) {
        final List<Complex[]> data = loadTestData(name);
        final long ulps = getTestUlps(maxUlps);
        for (final Complex[] triple : data) {
            assertComplex(triple[0], triple[1], name, operation, triple[2], ulps);
        }
    }

    /**
     * Assert the operation using the data loaded from test resources.
     *
     * @param testData Test data resource name.
     * @return the list
     */
    private static List<Complex[]> loadTestData(String name) {
        final String testData = "data/" + name + ".txt";
        final TestDataFlagOption option = globalMaxUlps < 1 ?
            TestDataFlagOption.LOAD : TestDataFlagOption.IGNORE;
        return TestUtils.loadTestData(testData, option,
            // CHECKSTYLE: stop Regex
            s -> System.out.println(name + " IGNORED: " + s));
            // CHECKSTYLE: resume Regex
    }

    /**
     * Gets the test ulps. This uses the input value of the global setting if that is greater
     * in magnitude.
     *
     * @param ulps the ulps
     * @return the test ulps
     */
    private static long getTestUlps(long ulps) {
        // If sign matches use the larger magnitude.
        // xor the sign bytes will be negative if the sign does not match
        if ((globalMaxUlps ^ ulps) >= 0) {
            final long max = Math.max(Math.abs(globalMaxUlps), Math.abs(ulps));
            // restore sign
            return ulps < 0 ? -max : max;
        }
        // If the global setting is negative and the test setting is positive then it overrides
        // the individual test setting for reporting purposes.
        if (globalMaxUlps < 0) {
            return globalMaxUlps;
        }
        // If the global setting is positive and the test setting is negative then the test
        // setting takes precedence.
        return ulps;
    }

    @Test
    void testAcos() {
        assertOperation("acos", Complex::acos, 2);
    }

    @Test
    void testAcosh() {
        assertOperation("acosh", Complex::acosh, 2);
    }

    @Test
    void testAsinh() {
        // Odd function: negative real cases defined by positive real cases
        assertOperation("asinh", Complex::asinh, 3);
    }

    @Test
    void testAtanh() {
        // Odd function: negative real cases defined by positive real cases
        assertOperation("atanh", Complex::atanh, 1);
    }

    @Test
    void testCosh() {
        // Even function: negative real cases defined by positive real cases
        assertOperation("cosh", Complex::cosh, 2);
    }

    @Test
    void testSinh() {
        // Odd function: negative real cases defined by positive real cases
        assertOperation("sinh", Complex::sinh, 2);
    }

    @Test
    void testTanh() {
        // Odd function: negative real cases defined by positive real cases
        assertOperation("tanh", Complex::tanh, 2);
    }

    @Test
    void testExp() {
        assertOperation("exp", Complex::exp, 2);
    }

    @Test
    void testLog() {
        assertOperation("log", Complex::log, 1);
    }

    @Test
    void testSqrt() {
        assertOperation("sqrt", Complex::sqrt, 1);
    }

    @Test
    void testMultiply() {
        assertBiOperation("multiply", Complex::multiply, 0);
    }

    @Test
    void testDivide() {
        assertBiOperation("divide", Complex::divide, 7);
    }

    @Test
    void testPowComplex() {
        assertBiOperation("pow", Complex::pow, 9);
    }
}
