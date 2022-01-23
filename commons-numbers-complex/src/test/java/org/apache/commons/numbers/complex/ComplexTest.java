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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.DoubleFunction;
import java.util.function.Supplier;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Complex}.
 *
 * <p>Note: The ISO C99 math functions are not fully tested in this class. See also:
 *
 * <ul>
 * <li>{@link CStandardTest} for a test of the ISO C99 standards including special case handling.
 * <li>{@link CReferenceTest} for a test of the output using standard finite value against an
 *     ISO C99 compliant reference implementation.
 * <li>{@link ComplexEdgeCaseTest} for a test of extreme edge case finite values for real and/or
 *     imaginary parts that can create intermediate overflow or underflow.
 * </ul>
 */
class ComplexTest {

    private static final double inf = Double.POSITIVE_INFINITY;
    private static final double neginf = Double.NEGATIVE_INFINITY;
    private static final double nan = Double.NaN;
    private static final double pi = Math.PI;
    private static final Complex oneInf = Complex.ofCartesian(1, inf);
    private static final Complex oneNegInf = Complex.ofCartesian(1, neginf);
    private static final Complex infOne = Complex.ofCartesian(inf, 1);
    private static final Complex infZero = Complex.ofCartesian(inf, 0);
    private static final Complex infNegZero = Complex.ofCartesian(inf, -0.0);
    private static final Complex infNegInf = Complex.ofCartesian(inf, neginf);
    private static final Complex infInf = Complex.ofCartesian(inf, inf);
    private static final Complex negInfInf = Complex.ofCartesian(neginf, inf);
    private static final Complex negInfOne = Complex.ofCartesian(neginf, 1);
    private static final Complex negInfNegInf = Complex.ofCartesian(neginf, neginf);
    private static final Complex oneNan = Complex.ofCartesian(1, nan);
    private static final Complex zeroInf = Complex.ofCartesian(0, inf);
    private static final Complex zeroNan = Complex.ofCartesian(0, nan);
    private static final Complex nanZero = Complex.ofCartesian(nan, 0);
    private static final Complex NAN = Complex.ofCartesian(nan, nan);
    private static final Complex INF = Complex.ofCartesian(inf, inf);

    /**
     * Used to test the number category of a Complex.
     */
    private enum NumberType {
        NAN, INFINITE, FINITE
    }

    /**
     * Create a complex number given the real part.
     *
     * @param real Real part.
     * @return {@code Complex} object
     */
    private static Complex ofReal(double real) {
        return Complex.ofCartesian(real, 0);
    }

    /**
     * Create a complex number given the imaginary part.
     *
     * @param imaginary Imaginary part.
     * @return {@code Complex} object
     */
    private static Complex ofImaginary(double imaginary) {
        return Complex.ofCartesian(0, imaginary);
    }

    @Test
    @Disabled("Used to output the java environment")
    @SuppressWarnings("squid:S2699")
    void testJava() {
        // CHECKSTYLE: stop Regexp
        System.out.println(">>testJava()");
        // MathTest#testExpSpecialCases() checks the following:
        // Assert.assertEquals("exp of -infinity should be 0.0", 0.0,
        // Math.exp(Double.NEGATIVE_INFINITY), Precision.EPSILON);
        // Let's check how well Math works:
        System.out.println("Math.exp=" + Math.exp(Double.NEGATIVE_INFINITY));
        final String[] props = {"java.version", // Java Runtime Environment version
            "java.vendor", // Java Runtime Environment vendor
            "java.vm.specification.version", // Java Virtual Machine specification version
            "java.vm.specification.vendor", // Java Virtual Machine specification vendor
            "java.vm.specification.name", // Java Virtual Machine specification name
            "java.vm.version", // Java Virtual Machine implementation version
            "java.vm.vendor", // Java Virtual Machine implementation vendor
            "java.vm.name", // Java Virtual Machine implementation name
            "java.specification.version", // Java Runtime Environment specification
                                          // version
            "java.specification.vendor", // Java Runtime Environment specification vendor
            "java.specification.name", // Java Runtime Environment specification name
            "java.class.version", // Java class format version number
        };
        for (final String t : props) {
            System.out.println(t + "=" + System.getProperty(t));
        }
        System.out.println("<<testJava()");
        // CHECKSTYLE: resume Regexp
    }

    @Test
    void testCartesianConstructor() {
        final Complex z = Complex.ofCartesian(3.0, 4.0);
        Assertions.assertEquals(3.0, z.getReal());
        Assertions.assertEquals(4.0, z.getImaginary());
    }

    @Test
    void testPolarConstructor() {
        final double r = 98765;
        final double theta = 0.12345;
        final Complex z = Complex.ofPolar(r, theta);
        final Complex y = Complex.ofCis(theta);
        Assertions.assertEquals(r * y.getReal(), z.getReal());
        Assertions.assertEquals(r * y.getImaginary(), z.getImaginary());

        // Edge cases
        // Non-finite theta
        Assertions.assertEquals(NAN, Complex.ofPolar(1, -inf));
        Assertions.assertEquals(NAN, Complex.ofPolar(1, inf));
        Assertions.assertEquals(NAN, Complex.ofPolar(1, nan));
        // Infinite rho is invalid when theta is NaN
        // i.e. do not create an infinite complex such as (inf, nan)
        Assertions.assertEquals(NAN, Complex.ofPolar(inf, nan));
        // negative or NaN rho
        Assertions.assertEquals(NAN, Complex.ofPolar(-inf, 1));
        Assertions.assertEquals(NAN, Complex.ofPolar(-0.0, 1));
        Assertions.assertEquals(NAN, Complex.ofPolar(nan, 1));

        // Construction from infinity has values left to double arithmetic.
        // Test the examples from the javadoc
        Assertions.assertEquals(NAN, Complex.ofPolar(-0.0, 0.0));
        Assertions.assertEquals(Complex.ofCartesian(0.0, 0.0), Complex.ofPolar(0.0, 0.0));
        Assertions.assertEquals(Complex.ofCartesian(1.0, 0.0), Complex.ofPolar(1.0, 0.0));
        Assertions.assertEquals(Complex.ofCartesian(-1.0, Math.sin(pi)), Complex.ofPolar(1.0, pi));
        Assertions.assertEquals(Complex.ofCartesian(-inf, inf), Complex.ofPolar(inf, pi));
        Assertions.assertEquals(Complex.ofCartesian(inf, nan), Complex.ofPolar(inf, 0.0));
        Assertions.assertEquals(Complex.ofCartesian(inf, -inf), Complex.ofPolar(inf, -pi / 4));
        Assertions.assertEquals(Complex.ofCartesian(-inf, -inf), Complex.ofPolar(inf, 5 * pi / 4));
    }

    @Test
    void testPolarConstructorAbsArg() {
        // The test should work with any seed but use a fixed seed to avoid build
        // instability.
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(678678638L);
        for (int i = 0; i < 10; i++) {
            final double rho = rng.nextDouble();
            // Range (pi, pi]: lower exclusive, upper inclusive
            final double theta = pi - rng.nextDouble() * 2 * pi;
            final Complex z = Complex.ofPolar(rho, theta);
            // Match within 1 ULP
            Assertions.assertEquals(rho, z.abs(), Math.ulp(rho));
            Assertions.assertEquals(theta, z.arg(), Math.ulp(theta));
        }
    }

    @Test
    void testCisConstructor() {
        final double x = 0.12345;
        final Complex z = Complex.ofCis(x);
        Assertions.assertEquals(Math.cos(x), z.getReal());
        Assertions.assertEquals(Math.sin(x), z.getImaginary());
    }

    /**
     * Test parse and toString are compatible.
     */
    @Test
    void testParseAndToString() {
        final double[] parts = {Double.NEGATIVE_INFINITY, -1, -0.0, 0.0, 1, Math.PI, Double.POSITIVE_INFINITY,
            Double.NaN};
        for (final double x : parts) {
            for (final double y : parts) {
                final Complex z = Complex.ofCartesian(x, y);
                Assertions.assertEquals(z, Complex.parse(z.toString()));
            }
        }
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        for (int i = 0; i < 10; i++) {
            final double x = -1 + rng.nextDouble() * 2;
            final double y = -1 + rng.nextDouble() * 2;
            final Complex z = Complex.ofCartesian(x, y);
            Assertions.assertEquals(z, Complex.parse(z.toString()));
        }

        // Special values not covered
        Assertions.assertEquals(Complex.ofPolar(2, pi), Complex.parse(Complex.ofPolar(2, pi).toString()));
        Assertions.assertEquals(Complex.ofCis(pi), Complex.parse(Complex.ofCis(pi).toString()));
    }

    @Test
    void testParseNull() {
        Assertions.assertThrows(NullPointerException.class, () -> Complex.parse(null));
    }

    @Test
    void testParseEmpty() {
        Assertions.assertThrows(NumberFormatException.class, () -> Complex.parse(""));
        Assertions.assertThrows(NumberFormatException.class, () -> Complex.parse(" "));
    }

    @Test
    void testParseWrongStart() {
        Assertions.assertThrows(NumberFormatException.class, () -> Complex.parse("1.0,2.0)"));
        Assertions.assertThrows(NumberFormatException.class, () -> Complex.parse("[1.0,2.0)"));
    }

    @Test
    void testParseWrongEnd() {
        Assertions.assertThrows(NumberFormatException.class, () -> Complex.parse("(1.0,2.0"));
        Assertions.assertThrows(NumberFormatException.class, () -> Complex.parse("(1.0,2.0]"));
    }

    @Test
    void testParseWrongSeparator() {
        Assertions.assertThrows(NumberFormatException.class, () -> Complex.parse("(1.0 2.0)"));
        Assertions.assertThrows(NumberFormatException.class, () -> Complex.parse("(1.0:2.0)"));
    }

    @Test
    void testParseSeparatorOutsideStartAndEnd() {
        Assertions.assertThrows(NumberFormatException.class, () -> Complex.parse("(1.0,2.0),"));
        Assertions.assertThrows(NumberFormatException.class, () -> Complex.parse(",(1.0,2.0)"));
    }

    @Test
    void testParseExtraSeparator() {
        Assertions.assertThrows(NumberFormatException.class, () -> Complex.parse("(1.0,,2.0)"));
        Assertions.assertThrows(NumberFormatException.class, () -> Complex.parse("(1.0,2.0,)"));
        Assertions.assertThrows(NumberFormatException.class, () -> Complex.parse("(,1.0,2.0)"));
        Assertions.assertThrows(NumberFormatException.class, () -> Complex.parse("(1.0,2,0)"));
    }

    @Test
    void testParseInvalidRe() {
        Assertions.assertThrows(NumberFormatException.class, () -> Complex.parse("(I.0,2.0)"));
    }

    @Test
    void testParseInvalidIm() {
        Assertions.assertThrows(NumberFormatException.class, () -> Complex.parse("(1.0,2.G)"));
    }

    @Test
    void testParseSpaceAllowedAroundNumbers() {
        final double re = 1.234;
        final double im = 5.678;
        final Complex z = Complex.ofCartesian(re, im);
        Assertions.assertEquals(z, Complex.parse("(" + re + "," + im + ")"));
        Assertions.assertEquals(z, Complex.parse("( " + re + "," + im + ")"));
        Assertions.assertEquals(z, Complex.parse("(" + re + " ," + im + ")"));
        Assertions.assertEquals(z, Complex.parse("(" + re + ", " + im + ")"));
        Assertions.assertEquals(z, Complex.parse("(" + re + "," + im + " )"));
        Assertions.assertEquals(z, Complex.parse("(  " + re + "  , " + im + "     )"));
    }

    @Test
    void testCGrammar() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        for (int i = 0; i < 10; i++) {
            final Complex z = Complex.ofCartesian(rng.nextDouble(), rng.nextDouble());
            Assertions.assertEquals(z.getReal(), z.real(), "real");
            Assertions.assertEquals(z.getImaginary(), z.imag(), "imag");
        }
    }

    @Test
    void testAbs() {
        final Complex z = Complex.ofCartesian(3.0, 4.0);
        Assertions.assertEquals(5.0, z.abs());
    }

    @Test
    void testAbsNaN() {
        // The result is NaN if either argument is NaN and the other is not infinite
        Assertions.assertEquals(nan, NAN.abs());
        Assertions.assertEquals(nan, Complex.ofCartesian(3.0, nan).abs());
        Assertions.assertEquals(nan, Complex.ofCartesian(nan, 3.0).abs());
        // The result is positive infinite if either argument is infinite
        Assertions.assertEquals(inf, Complex.ofCartesian(inf, nan).abs());
        Assertions.assertEquals(inf, Complex.ofCartesian(-inf, nan).abs());
        Assertions.assertEquals(inf, Complex.ofCartesian(nan, inf).abs());
        Assertions.assertEquals(inf, Complex.ofCartesian(nan, -inf).abs());
        Assertions.assertEquals(inf, Complex.ofCartesian(inf, 3.0).abs());
        Assertions.assertEquals(inf, Complex.ofCartesian(-inf, 3.0).abs());
        Assertions.assertEquals(inf, Complex.ofCartesian(3.0, inf).abs());
        Assertions.assertEquals(inf, Complex.ofCartesian(3.0, -inf).abs());
    }

    /**
     * Test standard values
     */
    @Test
    void testArg() {
        Complex z = Complex.ofCartesian(1, 0);
        assertArgument(0.0, z, 1.0e-12);

        z = Complex.ofCartesian(1, 1);
        assertArgument(Math.PI / 4, z, 1.0e-12);

        z = Complex.ofCartesian(0, 1);
        assertArgument(Math.PI / 2, z, 1.0e-12);

        z = Complex.ofCartesian(-1, 1);
        assertArgument(3 * Math.PI / 4, z, 1.0e-12);

        z = Complex.ofCartesian(-1, 0);
        assertArgument(Math.PI, z, 1.0e-12);

        z = Complex.ofCartesian(-1, -1);
        assertArgument(-3 * Math.PI / 4, z, 1.0e-12);

        z = Complex.ofCartesian(0, -1);
        assertArgument(-Math.PI / 2, z, 1.0e-12);

        z = Complex.ofCartesian(1, -1);
        assertArgument(-Math.PI / 4, z, 1.0e-12);
    }

    /**
     * Verify atan2-style handling of infinite parts
     */
    @Test
    void testArgInf() {
        assertArgument(Math.PI / 4, infInf, 1.0e-12);
        assertArgument(Math.PI / 2, oneInf, 1.0e-12);
        assertArgument(0.0, infOne, 1.0e-12);
        assertArgument(Math.PI / 2, zeroInf, 1.0e-12);
        assertArgument(0.0, infZero, 1.0e-12);
        assertArgument(Math.PI, negInfOne, 1.0e-12);
        assertArgument(-3.0 * Math.PI / 4, negInfNegInf, 1.0e-12);
        assertArgument(-Math.PI / 2, oneNegInf, 1.0e-12);
    }

    /**
     * Verify that either part NaN results in NaN
     */
    @Test
    void testArgNaN() {
        assertArgument(Double.NaN, nanZero, 0);
        assertArgument(Double.NaN, zeroNan, 0);
        assertArgument(Double.NaN, NAN, 0);
    }

    private static void assertArgument(double expected, Complex complex, double delta) {
        final double actual = complex.arg();
        Assertions.assertEquals(expected, actual, delta);
        Assertions.assertEquals(actual, complex.arg(), delta);
    }

    @Test
    void testNorm() {
        final Complex z = Complex.ofCartesian(3.0, 4.0);
        Assertions.assertEquals(25.0, z.norm());
    }

    @Test
    void testNormNaN() {
        // The result is NaN if either argument is NaN and the other is not infinite
        Assertions.assertEquals(nan, NAN.norm());
        Assertions.assertEquals(nan, Complex.ofCartesian(3.0, nan).norm());
        Assertions.assertEquals(nan, Complex.ofCartesian(nan, 3.0).norm());
        // The result is positive infinite if either argument is infinite
        Assertions.assertEquals(inf, Complex.ofCartesian(inf, nan).norm());
        Assertions.assertEquals(inf, Complex.ofCartesian(-inf, nan).norm());
        Assertions.assertEquals(inf, Complex.ofCartesian(nan, inf).norm());
        Assertions.assertEquals(inf, Complex.ofCartesian(nan, -inf).norm());
    }

    /**
     * Test all number types: isNaN, isInfinite, isFinite.
     */
    @Test
    void testNumberType() {
        assertNumberType(0, 0, NumberType.FINITE);
        assertNumberType(1, 0, NumberType.FINITE);
        assertNumberType(0, 1, NumberType.FINITE);

        assertNumberType(inf, 0, NumberType.INFINITE);
        assertNumberType(-inf, 0, NumberType.INFINITE);
        assertNumberType(0, inf, NumberType.INFINITE);
        assertNumberType(0, -inf, NumberType.INFINITE);
        // A complex or imaginary value with at least one infinite part is regarded as an
        // infinity
        // (even if its other part is a NaN).
        assertNumberType(inf, nan, NumberType.INFINITE);
        assertNumberType(-inf, nan, NumberType.INFINITE);
        assertNumberType(nan, inf, NumberType.INFINITE);
        assertNumberType(nan, -inf, NumberType.INFINITE);

        assertNumberType(nan, 0, NumberType.NAN);
        assertNumberType(0, nan, NumberType.NAN);
        assertNumberType(nan, nan, NumberType.NAN);
    }

    /**
     * Assert the number type of the Complex created from the real and imaginary
     * components.
     *
     * @param real the real component
     * @param imaginary the imaginary component
     * @param type the type
     */
    private static void assertNumberType(double real, double imaginary, NumberType type) {
        final Complex z = Complex.ofCartesian(real, imaginary);
        final boolean isNaN = z.isNaN();
        final boolean isInfinite = z.isInfinite();
        final boolean isFinite = z.isFinite();
        // A number can be only one
        int count = isNaN ? 1 : 0;
        count += isInfinite ? 1 : 0;
        count += isFinite ? 1 : 0;
        Assertions.assertEquals(1, count,
            () -> String.format("Complex can be only one type: isNaN=%s, isInfinite=%s, isFinite=%s: %s", isNaN,
                isInfinite, isFinite, z));
        switch (type) {
        case FINITE:
            Assertions.assertTrue(isFinite, () -> "not finite: " + z);
            break;
        case INFINITE:
            Assertions.assertTrue(isInfinite, () -> "not infinite: " + z);
            break;
        case NAN:
            Assertions.assertTrue(isNaN, () -> "not nan: " + z);
            break;
        default:
            Assertions.fail("Unknown number type");
        }
    }

    @Test
    void testConjugate() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final Complex z = x.conj();
        Assertions.assertEquals(3.0, z.getReal());
        Assertions.assertEquals(-4.0, z.getImaginary());
    }

    @Test
    void testConjugateNaN() {
        final Complex z = NAN.conj();
        Assertions.assertTrue(z.isNaN());
    }

    @Test
    void testConjugateInfinite() {
        Complex z = Complex.ofCartesian(0, inf);
        Assertions.assertEquals(neginf, z.conj().getImaginary());
        z = Complex.ofCartesian(0, neginf);
        Assertions.assertEquals(inf, z.conj().getImaginary());
    }

    @Test
    void testNegate() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final Complex z = x.negate();
        Assertions.assertEquals(-3.0, z.getReal());
        Assertions.assertEquals(-4.0, z.getImaginary());
    }

    @Test
    void testNegateNaN() {
        final Complex z = NAN.negate();
        Assertions.assertTrue(z.isNaN());
    }

    @Test
    void testProj() {
        final Complex z = Complex.ofCartesian(3.0, 4.0);
        Assertions.assertSame(z, z.proj());
        // Sign must be the same for projection
        TestUtils.assertSame(infZero, Complex.ofCartesian(inf, 4.0).proj());
        TestUtils.assertSame(infZero, Complex.ofCartesian(inf, inf).proj());
        TestUtils.assertSame(infZero, Complex.ofCartesian(inf, nan).proj());
        TestUtils.assertSame(infZero, Complex.ofCartesian(3.0, inf).proj());
        TestUtils.assertSame(infZero, Complex.ofCartesian(nan, inf).proj());
        TestUtils.assertSame(infNegZero, Complex.ofCartesian(inf, -4.0).proj());
        TestUtils.assertSame(infNegZero, Complex.ofCartesian(inf, -inf).proj());
        TestUtils.assertSame(infNegZero, Complex.ofCartesian(3.0, -inf).proj());
        TestUtils.assertSame(infNegZero, Complex.ofCartesian(nan, -inf).proj());
    }

    @Test
    void testAdd() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final Complex y = Complex.ofCartesian(5.0, 6.0);
        final Complex z = x.add(y);
        Assertions.assertEquals(8.0, z.getReal());
        Assertions.assertEquals(10.0, z.getImaginary());
    }

    @Test
    void testAddInf() {
        Complex x = Complex.ofCartesian(1, 1);
        final Complex z = Complex.ofCartesian(inf, 0);
        final Complex w = x.add(z);
        Assertions.assertEquals(1, w.getImaginary());
        Assertions.assertEquals(inf, w.getReal());

        x = Complex.ofCartesian(neginf, 0);
        Assertions.assertTrue(Double.isNaN(x.add(z).getReal()));
    }

    @Test
    void testAddReal() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = 5.0;
        final Complex z = x.add(y);
        Assertions.assertEquals(8.0, z.getReal());
        Assertions.assertEquals(4.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.add(ofReal(y)));
    }

    @Test
    void testAddRealNaN() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = nan;
        final Complex z = x.add(y);
        Assertions.assertEquals(nan, z.getReal());
        Assertions.assertEquals(4.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.add(ofReal(y)));
    }

    @Test
    void testAddRealInf() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = inf;
        final Complex z = x.add(y);
        Assertions.assertEquals(inf, z.getReal());
        Assertions.assertEquals(4.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.add(ofReal(y)));
    }

    @Test
    void testAddRealWithNegZeroImaginary() {
        final Complex x = Complex.ofCartesian(3.0, -0.0);
        final double y = 5.0;
        final Complex z = x.add(y);
        Assertions.assertEquals(8.0, z.getReal());
        Assertions.assertEquals(-0.0, z.getImaginary(), "Expected sign preservation");
        // Sign-preservation is a problem: -0.0 + 0.0 == 0.0
        final Complex z2 = x.add(ofReal(y));
        Assertions.assertEquals(8.0, z2.getReal());
        Assertions.assertEquals(0.0, z2.getImaginary(), "Expected no-sign preservation");
    }

    @Test
    void testAddImaginary() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = 5.0;
        final Complex z = x.addImaginary(y);
        Assertions.assertEquals(3.0, z.getReal());
        Assertions.assertEquals(9.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.add(ofImaginary(y)));
    }

    @Test
    void testAddImaginaryNaN() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = nan;
        final Complex z = x.addImaginary(y);
        Assertions.assertEquals(3.0, z.getReal());
        Assertions.assertEquals(nan, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.add(ofImaginary(y)));
    }

    @Test
    void testAddImaginaryInf() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = inf;
        final Complex z = x.addImaginary(y);
        Assertions.assertEquals(3.0, z.getReal());
        Assertions.assertEquals(inf, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.add(ofImaginary(y)));
    }

    @Test
    void testAddImaginaryWithNegZeroReal() {
        final Complex x = Complex.ofCartesian(-0.0, 4.0);
        final double y = 5.0;
        final Complex z = x.addImaginary(y);
        Assertions.assertEquals(-0.0, z.getReal(), "Expected sign preservation");
        Assertions.assertEquals(9.0, z.getImaginary());
        // Sign-preservation is a problem: -0.0 + 0.0 == 0.0
        final Complex z2 = x.add(ofImaginary(y));
        Assertions.assertEquals(0.0, z2.getReal(), "Expected no-sign preservation");
        Assertions.assertEquals(9.0, z2.getImaginary());
    }

    @Test
    void testSubtract() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final Complex y = Complex.ofCartesian(5.0, 7.0);
        final Complex z = x.subtract(y);
        Assertions.assertEquals(-2.0, z.getReal());
        Assertions.assertEquals(-3.0, z.getImaginary());
    }

    @Test
    void testSubtractInf() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final Complex y = Complex.ofCartesian(inf, 7.0);
        Complex z = x.subtract(y);
        Assertions.assertEquals(neginf, z.getReal());
        Assertions.assertEquals(-3.0, z.getImaginary());

        z = y.subtract(y);
        Assertions.assertEquals(nan, z.getReal());
        Assertions.assertEquals(0.0, z.getImaginary());
    }

    @Test
    void testSubtractReal() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = 5.0;
        final Complex z = x.subtract(y);
        Assertions.assertEquals(-2.0, z.getReal());
        Assertions.assertEquals(4.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.subtract(ofReal(y)));
    }

    @Test
    void testSubtractRealNaN() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = nan;
        final Complex z = x.subtract(y);
        Assertions.assertEquals(nan, z.getReal());
        Assertions.assertEquals(4.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.subtract(ofReal(y)));
    }

    @Test
    void testSubtractRealInf() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = inf;
        final Complex z = x.subtract(y);
        Assertions.assertEquals(-inf, z.getReal());
        Assertions.assertEquals(4.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.subtract(ofReal(y)));
    }

    @Test
    void testSubtractRealWithNegZeroImaginary() {
        final Complex x = Complex.ofCartesian(3.0, -0.0);
        final double y = 5.0;
        final Complex z = x.subtract(y);
        Assertions.assertEquals(-2.0, z.getReal());
        Assertions.assertEquals(-0.0, z.getImaginary());
        // Equivalent
        // Sign-preservation is not a problem: -0.0 - 0.0 == -0.0
        Assertions.assertEquals(z, x.subtract(ofReal(y)));
    }

    @Test
    void testSubtractImaginary() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = 5.0;
        final Complex z = x.subtractImaginary(y);
        Assertions.assertEquals(3.0, z.getReal());
        Assertions.assertEquals(-1.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.subtract(ofImaginary(y)));
    }

    @Test
    void testSubtractImaginaryNaN() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = nan;
        final Complex z = x.subtractImaginary(y);
        Assertions.assertEquals(3.0, z.getReal());
        Assertions.assertEquals(nan, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.subtract(ofImaginary(y)));
    }

    @Test
    void testSubtractImaginaryInf() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = inf;
        final Complex z = x.subtractImaginary(y);
        Assertions.assertEquals(3.0, z.getReal());
        Assertions.assertEquals(-inf, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.subtract(ofImaginary(y)));
    }

    @Test
    void testSubtractImaginaryWithNegZeroReal() {
        final Complex x = Complex.ofCartesian(-0.0, 4.0);
        final double y = 5.0;
        final Complex z = x.subtractImaginary(y);
        Assertions.assertEquals(-0.0, z.getReal());
        Assertions.assertEquals(-1.0, z.getImaginary());
        // Equivalent
        // Sign-preservation is not a problem: -0.0 - 0.0 == -0.0
        Assertions.assertEquals(z, x.subtract(ofImaginary(y)));
    }

    @Test
    void testSubtractFromReal() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = 5.0;
        final Complex z = x.subtractFrom(y);
        Assertions.assertEquals(2.0, z.getReal());
        Assertions.assertEquals(-4.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, ofReal(y).subtract(x));
    }

    @Test
    void testSubtractFromRealNaN() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = nan;
        final Complex z = x.subtractFrom(y);
        Assertions.assertEquals(nan, z.getReal());
        Assertions.assertEquals(-4.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, ofReal(y).subtract(x));
    }

    @Test
    void testSubtractFromRealInf() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = inf;
        final Complex z = x.subtractFrom(y);
        Assertions.assertEquals(inf, z.getReal());
        Assertions.assertEquals(-4.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, ofReal(y).subtract(x));
    }

    @Test
    void testSubtractFromRealWithPosZeroImaginary() {
        final Complex x = Complex.ofCartesian(3.0, 0.0);
        final double y = 5.0;
        final Complex z = x.subtractFrom(y);
        Assertions.assertEquals(2.0, z.getReal());
        Assertions.assertEquals(-0.0, z.getImaginary(), "Expected sign inversion");
        // Sign-inversion is a problem: 0.0 - 0.0 == 0.0
        Assertions.assertNotEquals(z, ofReal(y).subtract(x));
    }

    @Test
    void testSubtractFromImaginary() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = 5.0;
        final Complex z = x.subtractFromImaginary(y);
        Assertions.assertEquals(-3.0, z.getReal());
        Assertions.assertEquals(1.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, ofImaginary(y).subtract(x));
    }

    @Test
    void testSubtractFromImaginaryNaN() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = nan;
        final Complex z = x.subtractFromImaginary(y);
        Assertions.assertEquals(-3.0, z.getReal());
        Assertions.assertEquals(nan, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, ofImaginary(y).subtract(x));
    }

    @Test
    void testSubtractFromImaginaryInf() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = inf;
        final Complex z = x.subtractFromImaginary(y);
        Assertions.assertEquals(-3.0, z.getReal());
        Assertions.assertEquals(inf, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, ofImaginary(y).subtract(x));
    }

    @Test
    void testSubtractFromImaginaryWithPosZeroReal() {
        final Complex x = Complex.ofCartesian(0.0, 4.0);
        final double y = 5.0;
        final Complex z = x.subtractFromImaginary(y);
        Assertions.assertEquals(-0.0, z.getReal(), "Expected sign inversion");
        Assertions.assertEquals(1.0, z.getImaginary());
        // Sign-inversion is a problem: 0.0 - 0.0 == 0.0
        Assertions.assertNotEquals(z, ofImaginary(y).subtract(x));
    }

    @Test
    void testMultiply() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final Complex y = Complex.ofCartesian(5.0, 6.0);
        final Complex z = x.multiply(y);
        Assertions.assertEquals(-9.0, z.getReal());
        Assertions.assertEquals(38.0, z.getImaginary());
    }

    @Test
    void testMultiplyInfInf() {
        final Complex z = infInf.multiply(infInf);
        // Assert.assertTrue(z.isNaN()); // MATH-620
        Assertions.assertTrue(z.isInfinite());

        // Expected results from g++:
        Assertions.assertEquals(Complex.ofCartesian(nan, inf), infInf.multiply(infInf));
        Assertions.assertEquals(Complex.ofCartesian(inf, nan), infInf.multiply(infNegInf));
        Assertions.assertEquals(Complex.ofCartesian(-inf, nan), infInf.multiply(negInfInf));
        Assertions.assertEquals(Complex.ofCartesian(nan, -inf), infInf.multiply(negInfNegInf));
    }

    @Test
    void testMultiplyReal() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = 2.0;
        Complex z = x.multiply(y);
        Assertions.assertEquals(6.0, z.getReal());
        Assertions.assertEquals(8.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.multiply(ofReal(y)));

        z = x.multiply(-y);
        Assertions.assertEquals(-6.0, z.getReal());
        Assertions.assertEquals(-8.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.multiply(ofReal(-y)));
    }

    @Test
    void testMultiplyRealNaN() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = nan;
        final Complex z = x.multiply(y);
        Assertions.assertEquals(nan, z.getReal());
        Assertions.assertEquals(nan, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.multiply(ofReal(y)));
    }

    @Test
    void testMultiplyRealInf() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = inf;
        Complex z = x.multiply(y);
        Assertions.assertEquals(inf, z.getReal());
        Assertions.assertEquals(inf, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.multiply(ofReal(y)));

        z = x.multiply(-y);
        Assertions.assertEquals(-inf, z.getReal());
        Assertions.assertEquals(-inf, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.multiply(ofReal(-y)));
    }

    @Test
    void testMultiplyRealZero() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = 0.0;
        Complex z = x.multiply(y);
        Assertions.assertEquals(0.0, z.getReal());
        Assertions.assertEquals(0.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.multiply(ofReal(y)));

        z = x.multiply(-y);
        Assertions.assertEquals(-0.0, z.getReal());
        Assertions.assertEquals(-0.0, z.getImaginary());
        // Sign-preservation is a problem for imaginary: 0.0 - -0.0 == 0.0
        final Complex z2 = x.multiply(ofReal(-y));
        Assertions.assertEquals(-0.0, z2.getReal());
        Assertions.assertEquals(0.0, z2.getImaginary(), "Expected no sign preservation");
    }

    @Test
    void testMultiplyImaginary() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = 2.0;
        Complex z = x.multiplyImaginary(y);
        Assertions.assertEquals(-8.0, z.getReal());
        Assertions.assertEquals(6.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.multiply(ofImaginary(y)));

        z = x.multiplyImaginary(-y);
        Assertions.assertEquals(8.0, z.getReal());
        Assertions.assertEquals(-6.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.multiply(ofImaginary(-y)));
    }

    @Test
    void testMultiplyImaginaryNaN() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = nan;
        final Complex z = x.multiplyImaginary(y);
        Assertions.assertEquals(nan, z.getReal());
        Assertions.assertEquals(nan, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.multiply(ofImaginary(y)));
    }

    @Test
    void testMultiplyImaginaryInf() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = inf;
        Complex z = x.multiplyImaginary(y);
        Assertions.assertEquals(-inf, z.getReal());
        Assertions.assertEquals(inf, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.multiply(ofImaginary(y)));

        z = x.multiplyImaginary(-y);
        Assertions.assertEquals(inf, z.getReal());
        Assertions.assertEquals(-inf, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.multiply(ofImaginary(-y)));
    }

    @Test
    void testMultiplyImaginaryZero() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = 0.0;
        Complex z = x.multiplyImaginary(y);
        Assertions.assertEquals(-0.0, z.getReal());
        Assertions.assertEquals(0.0, z.getImaginary());
        // Sign-preservation is a problem for real: 0.0 - -0.0 == 0.0
        Complex z2 = x.multiply(ofImaginary(y));
        Assertions.assertEquals(0.0, z2.getReal(), "Expected no sign preservation");
        Assertions.assertEquals(0.0, z2.getImaginary());

        z = x.multiplyImaginary(-y);
        Assertions.assertEquals(0.0, z.getReal());
        Assertions.assertEquals(-0.0, z.getImaginary());
        // Sign-preservation is a problem for imaginary: -0.0 - 0.0 == 0.0
        z2 = x.multiply(ofImaginary(-y));
        Assertions.assertEquals(0.0, z2.getReal());
        Assertions.assertEquals(0.0, z2.getImaginary(), "Expected no sign preservation");
    }

    @Test
    void testNonZeroMultiplyI() {
        final double[] parts = {3.0, 4.0};
        for (final double a : parts) {
            for (final double b : parts) {
                final Complex c = Complex.ofCartesian(a, b);
                final Complex x = c.multiplyImaginary(1.0);
                // Check verses algebra solution
                Assertions.assertEquals(-b, x.getReal());
                Assertions.assertEquals(a, x.getImaginary());
                final Complex z = c.multiply(Complex.I);
                Assertions.assertEquals(x, z);
            }
        }
    }

    @Test
    void testNonZeroMultiplyNegativeI() {
        // This works no matter how you represent -I as a Complex
        final double[] parts = {3.0, 4.0};
        final Complex[] negIs = {Complex.ofCartesian(-0.0, -1), Complex.ofCartesian(0.0, -1)};
        for (final double a : parts) {
            for (final double b : parts) {
                final Complex c = Complex.ofCartesian(a, b);
                final Complex x = c.multiplyImaginary(-1.0);
                // Check verses algebra solution
                Assertions.assertEquals(b, x.getReal());
                Assertions.assertEquals(-a, x.getImaginary());
                for (final Complex negI : negIs) {
                    final Complex z = c.multiply(negI);
                    Assertions.assertEquals(x, z);
                }
            }
        }
    }

    @Test
    void testMultiplyZeroByI() {
        final double[] zeros = {-0.0, 0.0};
        for (final double a : zeros) {
            for (final double b : zeros) {
                final Complex c = Complex.ofCartesian(a, b);
                final Complex x = c.multiplyImaginary(1.0);
                // Check verses algebra solution
                Assertions.assertEquals(-b, x.getReal());
                Assertions.assertEquals(a, x.getImaginary());
                final Complex z = c.multiply(Complex.I);
                // Does not work when imaginary part is +0.0.
                if (Double.compare(b, 0.0) == 0) {
                    // (-0.0, 0.0).multiply( (0,1) ) => (-0.0, 0.0) expected (-0.0,-0.0)
                    // ( 0.0, 0.0).multiply( (0,1) ) => ( 0.0, 0.0) expected (-0.0, 0.0)
                    // Sign is allowed to be different for zero.
                    Assertions.assertEquals(0, z.getReal(), 0.0);
                    Assertions.assertEquals(0, z.getImaginary(), 0.0);
                    Assertions.assertNotEquals(x, z);
                } else {
                    Assertions.assertEquals(x, z);
                }
            }
        }
    }

    @Test
    void testMultiplyZeroByNegativeI() {
        // Depending on how we represent -I this does not work for 2/4 cases
        // but the cases are different. Here we test the negation of I.
        final Complex negI = Complex.I.negate();
        final double[] zeros = {-0.0, 0.0};
        for (final double a : zeros) {
            for (final double b : zeros) {
                final Complex c = Complex.ofCartesian(a, b);
                final Complex x = c.multiplyImaginary(-1.0);
                // Check verses algebra solution
                Assertions.assertEquals(b, x.getReal());
                Assertions.assertEquals(-a, x.getImaginary());
                final Complex z = c.multiply(negI);
                final Complex z2 = c.multiply(Complex.I).negate();
                // Does not work when imaginary part is -0.0.
                if (Double.compare(b, -0.0) == 0) {
                    // (-0.0,-0.0).multiply( (-0.0,-1) ) => ( 0.0, 0.0) expected (-0.0, 0.0)
                    // ( 0.0,-0.0).multiply( (-0.0,-1) ) => (-0.0, 0.0) expected (-0.0,-0.0)
                    // Sign is allowed to be different for zero.
                    Assertions.assertEquals(0, z.getReal(), 0.0);
                    Assertions.assertEquals(0, z.getImaginary(), 0.0);
                    Assertions.assertNotEquals(x, z);
                    // When multiply by I.negate() fails multiply by I then negate()
                    // works!
                    Assertions.assertEquals(x, z2);
                } else {
                    Assertions.assertEquals(x, z);
                    // When multiply by I.negate() works multiply by I then negate()
                    // fails!
                    Assertions.assertNotEquals(x, z2);
                }
            }
        }
    }

    @Test
    void testDivide() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final Complex y = Complex.ofCartesian(5.0, 6.0);
        final Complex z = x.divide(y);
        Assertions.assertEquals(39.0 / 61.0, z.getReal());
        Assertions.assertEquals(2.0 / 61.0, z.getImaginary());
    }

    @Test
    void testDivideZero() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final Complex z = x.divide(Complex.ZERO);
        Assertions.assertEquals(INF, z);
    }

    @Test
    void testDivideZeroZero() {
        final Complex x = Complex.ofCartesian(0.0, 0.0);
        final Complex z = x.divide(Complex.ZERO);
        Assertions.assertEquals(NAN, z);
    }

    @Test
    void testDivideNaN() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final Complex z = x.divide(NAN);
        Assertions.assertTrue(z.isNaN());
    }

    @Test
    void testDivideNanInf() {
        Complex z = oneInf.divide(Complex.ONE);
        Assertions.assertTrue(Double.isNaN(z.getReal()));
        Assertions.assertEquals(inf, z.getImaginary());

        z = negInfNegInf.divide(oneNan);
        Assertions.assertTrue(Double.isNaN(z.getReal()));
        Assertions.assertTrue(Double.isNaN(z.getImaginary()));

        z = negInfInf.divide(Complex.ONE);
        Assertions.assertTrue(Double.isInfinite(z.getReal()));
        Assertions.assertTrue(Double.isInfinite(z.getImaginary()));
    }

    @Test
    void testDivideReal() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = 2.0;
        Complex z = x.divide(y);
        Assertions.assertEquals(1.5, z.getReal());
        Assertions.assertEquals(2.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.divide(ofReal(y)));

        z = x.divide(-y);
        Assertions.assertEquals(-1.5, z.getReal());
        Assertions.assertEquals(-2.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.divide(ofReal(-y)));
    }

    @Test
    void testDivideRealNaN() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = nan;
        final Complex z = x.divide(y);
        Assertions.assertEquals(nan, z.getReal());
        Assertions.assertEquals(nan, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.divide(ofReal(y)));
    }

    @Test
    void testDivideRealInf() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = inf;
        Complex z = x.divide(y);
        Assertions.assertEquals(0.0, z.getReal());
        Assertions.assertEquals(0.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.divide(ofReal(y)));

        z = x.divide(-y);
        Assertions.assertEquals(-0.0, z.getReal());
        Assertions.assertEquals(-0.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.divide(ofReal(-y)));
    }

    @Test
    void testDivideRealZero() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = 0.0;
        Complex z = x.divide(y);
        Assertions.assertEquals(inf, z.getReal());
        Assertions.assertEquals(inf, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.divide(ofReal(y)));

        z = x.divide(-y);
        Assertions.assertEquals(-inf, z.getReal());
        Assertions.assertEquals(-inf, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.divide(ofReal(-y)));
    }

    @Test
    void testDivideImaginary() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = 2.0;
        Complex z = x.divideImaginary(y);
        Assertions.assertEquals(2.0, z.getReal());
        Assertions.assertEquals(-1.5, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.divide(ofImaginary(y)));

        z = x.divideImaginary(-y);
        Assertions.assertEquals(-2.0, z.getReal());
        Assertions.assertEquals(1.5, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.divide(ofImaginary(-y)));
    }

    @Test
    void testDivideImaginaryNaN() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = nan;
        final Complex z = x.divideImaginary(y);
        Assertions.assertEquals(nan, z.getReal());
        Assertions.assertEquals(nan, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.divide(ofImaginary(y)));
    }

    @Test
    void testDivideImaginaryInf() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = inf;
        Complex z = x.divideImaginary(y);
        Assertions.assertEquals(0.0, z.getReal());
        Assertions.assertEquals(-0.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.divide(ofImaginary(y)));

        z = x.divideImaginary(-y);
        Assertions.assertEquals(-0.0, z.getReal());
        Assertions.assertEquals(0.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.divide(ofImaginary(-y)));
    }

    @Test
    void testDivideImaginaryZero() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double y = 0.0;
        Complex z = x.divideImaginary(y);
        Assertions.assertEquals(inf, z.getReal());
        Assertions.assertEquals(-inf, z.getImaginary());
        // Sign-preservation is a problem for imaginary: 0.0 - -0.0 == 0.0
        Complex z2 = x.divide(ofImaginary(y));
        Assertions.assertEquals(inf, z2.getReal());
        Assertions.assertEquals(inf, z2.getImaginary(), "Expected no sign preservation");

        z = x.divideImaginary(-y);
        Assertions.assertEquals(-inf, z.getReal());
        Assertions.assertEquals(inf, z.getImaginary());
        // Sign-preservation is a problem for real: 0.0 + -0.0 == 0.0
        z2 = x.divide(ofImaginary(-y));
        Assertions.assertEquals(inf, z2.getReal(), "Expected no sign preservation");
        Assertions.assertEquals(inf, z2.getImaginary());
    }

    /**
     * Arithmetic test using combinations of +/- x for real, imaginary and the double
     * argument for add, subtract, subtractFrom, multiply and divide, where x is zero or
     * non-zero.
     *
     * <p>The differences to the same argument as a Complex are tested. The only
     * differences should be the sign of zero in certain cases.
     */
    @Test
    void testSignedArithmetic() {
        // The following lists the conditions for the double primitive operation where
        // the Complex operation is different. Here the double argument can be:
        // x : any value
        // +x : positive
        // +0.0: positive zero
        // -x : negative
        // -0.0: negative zero
        // 0 : any zero
        // use y for any non-zero value

        // Check the known fail cases using an integer as a bit set.
        // If a bit is 1 then the case is known to fail.
        // The 64 cases are enumerated as:
        // 4 cases: (a,-0.0) operation on -0.0, 0.0, -2, 3
        // 4 cases: (a, 0.0) operation on -0.0, 0.0, -2, 3
        // 4 cases: (a,-2.0) operation on -0.0, 0.0, -2, 3
        // 4 cases: (a, 3.0) operation on -0.0, 0.0, -2, 3
        // with a in [-0.0, 0.0, -2, 3]
        // The least significant bit is for the first case.

        // The bit set was generated for this test. The summary below demonstrates
        // documenting the sign change cases for multiply and divide is non-trivial
        // and the javadoc in Complex does not break down the actual cases.

        // 16: (x,-0.0) + x
        assertSignedZeroArithmetic("addReal", Complex::add, ComplexTest::ofReal, Complex::add,
            0b1111000000000000111100000000000011110000000000001111L);
        // 16: (-0.0,x) + x
        assertSignedZeroArithmetic("addImaginary", Complex::addImaginary, ComplexTest::ofImaginary, Complex::add,
            0b1111111111111111L);
        // 0:
        assertSignedZeroArithmetic("subtractReal", Complex::subtract, ComplexTest::ofReal, Complex::subtract, 0);
        // 0:
        assertSignedZeroArithmetic("subtractImaginary", Complex::subtractImaginary, ComplexTest::ofImaginary,
            Complex::subtract, 0);
        // 16: x - (x,+0.0)
        assertSignedZeroArithmetic("subtractFromReal", Complex::subtractFrom, ComplexTest::ofReal,
            (y, z) -> z.subtract(y), 0b11110000000000001111000000000000111100000000000011110000L);
        // 16: x - (+0.0,x)
        assertSignedZeroArithmetic("subtractFromImaginary", Complex::subtractFromImaginary, ComplexTest::ofImaginary,
            (y, z) -> z.subtract(y), 0b11111111111111110000000000000000L);
        // 4: (-0.0,-x) * +x
        // 4: (+0.0,-0.0) * x
        // 4: (+0.0,x) * -x
        // 2: (-y,-x) * +0.0
        // 2: (+y,+0.0) * -x
        // 2: (+y,-0.0) * +x
        // 2: (+y,-x) * -0.0
        // 2: (+x,-y) * +0.0
        // 2: (+x,+y) * -0.0
        assertSignedZeroArithmetic("multiplyReal", Complex::multiply, ComplexTest::ofReal, Complex::multiply,
            0b1001101011011000000100000001000010111010111110000101000001010L);
        // 4: (-0.0,+x) * +x
        // 2: (+0.0,-0.0) * -x
        // 4: (+0.0,+0.0) * x
        // 2: (+0.0,+y) * -x
        // 2: (-y,+x) * +0.0
        // 4: (+y,x) * -0.0
        // 2: (+0.0,+/-y) * -/+0
        // 2: (+y,+/-0.0) * +/-y (sign 0.0 matches sign y)
        // 2: (+y,+x) * +0.0
        assertSignedZeroArithmetic("multiplyImaginary", Complex::multiplyImaginary, ComplexTest::ofImaginary,
            Complex::multiply, 0b11000110110101001000000010000001110001111101011010000010100000L);
        // 2: (-0.0,0) / +y
        // 2: (+0.0,+x) / -y
        // 2: (-x,0) / -y
        // 1: (-0.0,+y) / +y
        // 1: (-y,+0.0) / -y
        assertSignedZeroArithmetic("divideReal", Complex::divide, ComplexTest::ofReal, Complex::divide,
            0b100100001000000010000001000000011001000L);

        // DivideImaginary has its own test as the result is not always equal ignoring the
        // sign.
    }

    private static void assertSignedZeroArithmetic(String name, BiFunction<Complex, Double, Complex> doubleOperation,
        DoubleFunction<Complex> doubleToComplex, BiFunction<Complex, Complex, Complex> complexOperation,
        long expectedFailures) {
        // With an operation on zero or non-zero arguments
        final double[] arguments = {-0.0, 0.0, -2, 3};
        for (final double a : arguments) {
            for (final double b : arguments) {
                final Complex c = Complex.ofCartesian(a, b);
                for (final double arg : arguments) {
                    final Complex y = doubleOperation.apply(c, arg);
                    final Complex z = complexOperation.apply(c, doubleToComplex.apply(arg));
                    final boolean expectedFailure = (expectedFailures & 0x1) == 1;
                    expectedFailures >>>= 1;
                    // Check the same answer. Sign is allowed to be different for zero.
                    Assertions.assertEquals(y.getReal(), z.getReal(), 0, () -> c + " " + name + " " + arg + ": real");
                    Assertions.assertEquals(y.getImaginary(), z.getImaginary(), 0,
                        () -> c + " " + name + " " + arg + ": imaginary");
                    Assertions.assertEquals(expectedFailure, !y.equals(z),
                        () -> c + " " + name + " " + arg + ": sign-difference");
                }
            }
        }
    }

    /**
     * Arithmetic test using combinations of +/- x for real, imaginary and and the double
     * argument for divideImaginary, where x is zero or non-zero.
     *
     * <p>The differences to the same argument as a Complex are tested. This checks for
     * sign differences of zero or, if divide by zero, that the result is equal to divide
     * by zero using a Complex then multiplied by I.
     */
    @Test
    void testSignedDivideImaginaryArithmetic() {
        // Cases for divide by non-zero:
        // 2: (-0.0,+x) / -y
        // 4: (+x,+/-0.0) / -/+y
        // 2: (+0.0,+x) / +y
        // Cases for divide by zero after multiplication of the Complex result by I:
        // 2: (-0.0,+/-y) / +0.0
        // 2: (+0.0,+/-y) / +0.0
        // 4: (-y,x) / +0.0
        // 4: (y,x) / +0.0
        // If multiplied by -I all the divide by -0.0 cases have sign errors and / +0.0 is
        // OK.
        long expectedFailures = 0b11001101111011001100110011001110110011110010000111001101000000L;
        // With an operation on zero or non-zero arguments
        final double[] arguments = {-0.0, 0.0, -2, 3};
        for (final double a : arguments) {
            for (final double b : arguments) {
                final Complex c = Complex.ofCartesian(a, b);
                for (final double arg : arguments) {
                    final Complex y = c.divideImaginary(arg);
                    Complex z = c.divide(ofImaginary(arg));
                    final boolean expectedFailure = (expectedFailures & 0x1) == 1;
                    expectedFailures >>>= 1;
                    // If divide by zero then the divide(Complex) method matches divide by real.
                    // To match divide by imaginary requires multiplication by I.
                    if (arg == 0) {
                        // Same result if multiplied by I. The sign may not match so
                        // optionally ignore the sign of the infinity.
                        z = z.multiplyImaginary(1);
                        final double ya = expectedFailure ? Math.abs(y.getReal()) : y.getReal();
                        final double yb = expectedFailure ? Math.abs(y.getImaginary()) : y.getImaginary();
                        final double za = expectedFailure ? Math.abs(z.getReal()) : z.getReal();
                        final double zb = expectedFailure ? Math.abs(z.getImaginary()) : z.getImaginary();
                        Assertions.assertEquals(ya, za, () -> c + " divideImaginary " + arg + ": real");
                        Assertions.assertEquals(yb, zb, () -> c + " divideImaginary " + arg + ": imaginary");
                    } else {
                        // Check the same answer. Sign is allowed to be different for zero.
                        Assertions.assertEquals(y.getReal(), z.getReal(), 0,
                            () -> c + " divideImaginary " + arg + ": real");
                        Assertions.assertEquals(y.getImaginary(), z.getImaginary(), 0,
                            () -> c + " divideImaginary " + arg + ": imaginary");
                        Assertions.assertEquals(expectedFailure, !y.equals(z),
                            () -> c + " divideImaginary " + arg + ": sign-difference");
                    }
                }
            }
        }
    }

    @Test
    void testLog10() {
        final double ln10 = Math.log(10);
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        for (int i = 0; i < 10; i++) {
            final Complex z = Complex.ofCartesian(rng.nextDouble() * 2, rng.nextDouble() * 2);
            final Complex lnz = z.log();
            final Complex log10z = z.log10();
            // This is prone to floating-point error so use a delta
            Assertions.assertEquals(lnz.getReal() / ln10, log10z.getReal(), 1e-12, "real");
            // This test should be exact
            Assertions.assertEquals(lnz.getImaginary(), log10z.getImaginary(), "imag");
        }
    }

    @Test
    void testPow() {
        final Complex x = Complex.ofCartesian(3, 4);
        final double yDouble = 5.0;
        final Complex yComplex = ofReal(yDouble);
        Assertions.assertEquals(x.pow(yComplex), x.pow(yDouble));
    }

    @Test
    void testPowComplexRealZero() {
        // Hits the edge case when real == 0 but imaginary != 0
        final Complex x = Complex.ofCartesian(0, 1);
        final Complex z = Complex.ofCartesian(2, 3);
        final Complex c = x.pow(z);
        // Answer from g++
        Assertions.assertEquals(-0.008983291021129429, c.getReal());
        Assertions.assertEquals(1.1001358594835313e-18, c.getImaginary());
    }

    @Test
    void testPowComplexZeroBase() {
        final double x = Double.MIN_VALUE;
        assertPowComplexZeroBase(0, 0, NAN);
        assertPowComplexZeroBase(0, x, NAN);
        assertPowComplexZeroBase(x, x, NAN);
        assertPowComplexZeroBase(x, 0, Complex.ZERO);
    }

    private static void assertPowComplexZeroBase(double re, double im, Complex expected) {
        final Complex z = Complex.ofCartesian(re, im);
        final Complex c = Complex.ZERO.pow(z);
        Assertions.assertEquals(expected, c);
    }

    @Test
    void testPowScalerRealZero() {
        // Hits the edge case when real == 0 but imaginary != 0
        final Complex x = Complex.ofCartesian(0, 1);
        final Complex c = x.pow(2);
        // Answer from g++
        Assertions.assertEquals(-1, c.getReal());
        Assertions.assertEquals(1.2246467991473532e-16, c.getImaginary());
    }

    @Test
    void testPowScalarZeroBase() {
        final double x = Double.MIN_VALUE;
        assertPowScalarZeroBase(0, NAN);
        assertPowScalarZeroBase(x, Complex.ZERO);
    }

    private static void assertPowScalarZeroBase(double exp, Complex expected) {
        final Complex c = Complex.ZERO.pow(exp);
        Assertions.assertEquals(expected, c);
    }

    @Test
    void testPowNanBase() {
        final Complex x = NAN;
        final double yDouble = 5.0;
        final Complex yComplex = ofReal(yDouble);
        Assertions.assertEquals(x.pow(yComplex), x.pow(yDouble));
    }

    @Test
    void testPowNanExponent() {
        final Complex x = Complex.ofCartesian(3, 4);
        final double yDouble = Double.NaN;
        final Complex yComplex = ofReal(yDouble);
        Assertions.assertEquals(x.pow(yComplex), x.pow(yDouble));
    }

    @Test
    void testSqrtPolar() {
        final double tol = 1e-12;
        double r = 1;
        for (int i = 0; i < 5; i++) {
            r += i;
            double theta = 0;
            for (int j = 0; j < 11; j++) {
                theta += pi / 12;
                final Complex z = Complex.ofPolar(r, theta);
                final Complex sqrtz = Complex.ofPolar(Math.sqrt(r), theta / 2);
                TestUtils.assertEquals(sqrtz, z.sqrt(), tol);
            }
        }
    }

    @Test
    void testZerothRootThrows() {
        final Complex c = Complex.ofCartesian(1, 1);
        Assertions.assertThrows(IllegalArgumentException.class, () -> c.nthRoot(0),
            "zeroth root should not be allowed");
    }

    /**
     * Test: computing <b>third roots</b> of z.
     *
     * <pre>
     * <code>
     * <b>z = -2 + 2 * i</b>
     *   => z_0 =  1      +          i
     *   => z_1 = -1.3660 + 0.3660 * i
     *   => z_2 =  0.3660 - 1.3660 * i
     * </code>
     * </pre>
     */
    @Test
    void testNthRootNormalThirdRoot() {
        // The complex number we want to compute all third-roots for.
        final Complex z = Complex.ofCartesian(-2, 2);
        // The List holding all third roots
        final Complex[] thirdRootsOfZ = z.nthRoot(3).toArray(new Complex[0]);
        // Returned Collection must not be empty!
        Assertions.assertEquals(3, thirdRootsOfZ.length);
        // test z_0
        Assertions.assertEquals(1.0, thirdRootsOfZ[0].getReal(), 1.0e-5);
        Assertions.assertEquals(1.0, thirdRootsOfZ[0].getImaginary(), 1.0e-5);
        // test z_1
        Assertions.assertEquals(-1.3660254037844386, thirdRootsOfZ[1].getReal(), 1.0e-5);
        Assertions.assertEquals(0.36602540378443843, thirdRootsOfZ[1].getImaginary(), 1.0e-5);
        // test z_2
        Assertions.assertEquals(0.366025403784439, thirdRootsOfZ[2].getReal(), 1.0e-5);
        Assertions.assertEquals(-1.3660254037844384, thirdRootsOfZ[2].getImaginary(), 1.0e-5);
    }

    /**
     * Test: computing <b>fourth roots</b> of z.
     *
     * <pre>
     * <code>
     * <b>z = 5 - 2 * i</b>
     *   => z_0 =  1.5164 - 0.1446 * i
     *   => z_1 =  0.1446 + 1.5164 * i
     *   => z_2 = -1.5164 + 0.1446 * i
     *   => z_3 = -1.5164 - 0.1446 * i
     * </code>
     * </pre>
     */
    @Test
    void testNthRootNormalFourthRoot() {
        // The complex number we want to compute all third-roots for.
        final Complex z = Complex.ofCartesian(5, -2);
        // The List holding all fourth roots
        final Complex[] fourthRootsOfZ = z.nthRoot(4).toArray(new Complex[0]);
        // Returned Collection must not be empty!
        Assertions.assertEquals(4, fourthRootsOfZ.length);
        // test z_0
        Assertions.assertEquals(1.5164629308487783, fourthRootsOfZ[0].getReal(), 1.0e-5);
        Assertions.assertEquals(-0.14469266210702247, fourthRootsOfZ[0].getImaginary(), 1.0e-5);
        // test z_1
        Assertions.assertEquals(0.14469266210702256, fourthRootsOfZ[1].getReal(), 1.0e-5);
        Assertions.assertEquals(1.5164629308487783, fourthRootsOfZ[1].getImaginary(), 1.0e-5);
        // test z_2
        Assertions.assertEquals(-1.5164629308487783, fourthRootsOfZ[2].getReal(), 1.0e-5);
        Assertions.assertEquals(0.14469266210702267, fourthRootsOfZ[2].getImaginary(), 1.0e-5);
        // test z_3
        Assertions.assertEquals(-0.14469266210702275, fourthRootsOfZ[3].getReal(), 1.0e-5);
        Assertions.assertEquals(-1.5164629308487783, fourthRootsOfZ[3].getImaginary(), 1.0e-5);
    }

    /**
     * Test: computing <b>third roots</b> of z.
     *
     * <pre>
     * <code>
     * <b>z = 8</b>
     *   => z_0 =  2
     *   => z_1 = -1 + 1.73205 * i
     *   => z_2 = -1 - 1.73205 * i
     * </code>
     * </pre>
     */
    @Test
    void testNthRootCornercaseThirdRootImaginaryPartEmpty() {
        // The number 8 has three third roots. One we all already know is the number 2.
        // But there are two more complex roots.
        final Complex z = Complex.ofCartesian(8, 0);
        // The List holding all third roots
        final Complex[] thirdRootsOfZ = z.nthRoot(3).toArray(new Complex[0]);
        // Returned Collection must not be empty!
        Assertions.assertEquals(3, thirdRootsOfZ.length);
        // test z_0
        Assertions.assertEquals(2.0, thirdRootsOfZ[0].getReal(), 1.0e-5);
        Assertions.assertEquals(0.0, thirdRootsOfZ[0].getImaginary(), 1.0e-5);
        // test z_1
        Assertions.assertEquals(-1.0, thirdRootsOfZ[1].getReal(), 1.0e-5);
        Assertions.assertEquals(1.7320508075688774, thirdRootsOfZ[1].getImaginary(), 1.0e-5);
        // test z_2
        Assertions.assertEquals(-1.0, thirdRootsOfZ[2].getReal(), 1.0e-5);
        Assertions.assertEquals(-1.732050807568877, thirdRootsOfZ[2].getImaginary(), 1.0e-5);
    }

    /**
     * Test: computing <b>third roots</b> of z with real part 0.
     *
     * <pre>
     * <code>
     * <b>z = 2 * i</b>
     *   => z_0 =  1.0911 + 0.6299 * i
     *   => z_1 = -1.0911 + 0.6299 * i
     *   => z_2 = -2.3144 - 1.2599 * i
     * </code>
     * </pre>
     */
    @Test
    void testNthRootCornercaseThirdRootRealPartZero() {
        // complex number with only imaginary part
        final Complex z = Complex.ofCartesian(0, 2);
        // The List holding all third roots
        final Complex[] thirdRootsOfZ = z.nthRoot(3).toArray(new Complex[0]);
        // Returned Collection must not be empty!
        Assertions.assertEquals(3, thirdRootsOfZ.length);
        // test z_0
        Assertions.assertEquals(1.0911236359717216, thirdRootsOfZ[0].getReal(), 1.0e-5);
        Assertions.assertEquals(0.6299605249474365, thirdRootsOfZ[0].getImaginary(), 1.0e-5);
        // test z_1
        Assertions.assertEquals(-1.0911236359717216, thirdRootsOfZ[1].getReal(), 1.0e-5);
        Assertions.assertEquals(0.6299605249474365, thirdRootsOfZ[1].getImaginary(), 1.0e-5);
        // test z_2
        Assertions.assertEquals(-2.3144374213981936E-16, thirdRootsOfZ[2].getReal(), 1.0e-5);
        Assertions.assertEquals(-1.2599210498948732, thirdRootsOfZ[2].getImaginary(), 1.0e-5);
    }

    /**
     * Test: compute <b>third roots</b> using a negative argument to go clockwise around
     * the unit circle. Fourth roots of one are taken in both directions around the circle
     * using positive and negative arguments.
     *
     * <pre>
     * <code>
     * <b>z = 1</b>
     *   => z_0 = Positive: 1,0 ; Negative: 1,0
     *   => z_1 = Positive: 0,1 ; Negative: 0,-1
     *   => z_2 = Positive: -1,0 ; Negative: -1,0
     *   => z_3 = Positive: 0,-1 ; Negative: 0,1
     * </code>
     * </pre>
     */
    @Test
    void testNthRootNegativeArg() {
        // The complex number we want to compute all third-roots for.
        final Complex z = Complex.ofCartesian(1, 0);
        // The List holding all fourth roots
        Complex[] fourthRootsOfZ = z.nthRoot(4).toArray(new Complex[0]);
        // test z_0
        Assertions.assertEquals(1, fourthRootsOfZ[0].getReal(), 1.0e-5);
        Assertions.assertEquals(0, fourthRootsOfZ[0].getImaginary(), 1.0e-5);
        // test z_1
        Assertions.assertEquals(0, fourthRootsOfZ[1].getReal(), 1.0e-5);
        Assertions.assertEquals(1, fourthRootsOfZ[1].getImaginary(), 1.0e-5);
        // test z_2
        Assertions.assertEquals(-1, fourthRootsOfZ[2].getReal(), 1.0e-5);
        Assertions.assertEquals(0, fourthRootsOfZ[2].getImaginary(), 1.0e-5);
        // test z_3
        Assertions.assertEquals(0, fourthRootsOfZ[3].getReal(), 1.0e-5);
        Assertions.assertEquals(-1, fourthRootsOfZ[3].getImaginary(), 1.0e-5);
        // go clockwise around the unit circle using negative argument
        fourthRootsOfZ = z.nthRoot(-4).toArray(new Complex[0]);
        // test z_0
        Assertions.assertEquals(1, fourthRootsOfZ[0].getReal(), 1.0e-5);
        Assertions.assertEquals(0, fourthRootsOfZ[0].getImaginary(), 1.0e-5);
        // test z_1
        Assertions.assertEquals(0, fourthRootsOfZ[1].getReal(), 1.0e-5);
        Assertions.assertEquals(-1, fourthRootsOfZ[1].getImaginary(), 1.0e-5);
        // test z_2
        Assertions.assertEquals(-1, fourthRootsOfZ[2].getReal(), 1.0e-5);
        Assertions.assertEquals(0, fourthRootsOfZ[2].getImaginary(), 1.0e-5);
        // test z_3
        Assertions.assertEquals(0, fourthRootsOfZ[3].getReal(), 1.0e-5);
        Assertions.assertEquals(1, fourthRootsOfZ[3].getImaginary(), 1.0e-5);
    }

    @Test
    void testNthRootNan() {
        final int n = 3;
        final Complex z = ofReal(Double.NaN);
        final List<Complex> r = z.nthRoot(n);
        Assertions.assertEquals(n, r.size());
        for (final Complex c : r) {
            Assertions.assertTrue(Double.isNaN(c.getReal()));
            Assertions.assertTrue(Double.isNaN(c.getImaginary()));
        }
    }

    @Test
    void testNthRootInf() {
        final int n = 3;
        final Complex z = ofReal(Double.NEGATIVE_INFINITY);
        final List<Complex> r = z.nthRoot(n);
        Assertions.assertEquals(n, r.size());
    }

    @Test
    void testEqualsWithNull() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        Assertions.assertNotEquals(x, null);
    }

    @Test
    void testEqualsWithAnotherClass() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        Assertions.assertNotEquals(x, new Object());
    }

    @Test
    void testEqualsWithSameObject() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        Assertions.assertEquals(x, x);
    }

    @Test
    void testEqualsWithCopyObject() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final Complex y = Complex.ofCartesian(3.0, 4.0);
        Assertions.assertEquals(x, y);
    }

    @Test
    void testEqualsWithRealDifference() {
        final Complex x = Complex.ofCartesian(0.0, 0.0);
        final Complex y = Complex.ofCartesian(0.0 + Double.MIN_VALUE, 0.0);
        Assertions.assertNotEquals(x, y);
    }

    @Test
    void testEqualsWithImaginaryDifference() {
        final Complex x = Complex.ofCartesian(0.0, 0.0);
        final Complex y = Complex.ofCartesian(0.0, 0.0 + Double.MIN_VALUE);
        Assertions.assertNotEquals(x, y);
    }

    /**
     * Test {@link Complex#equals(Object)}. It should be consistent with
     * {@link Arrays#equals(double[], double[])} called using the components of two
     * complex numbers.
     */
    @Test
    void testEqualsIsConsistentWithArraysEquals() {
        // Explicit check of the cases documented in the Javadoc:
        assertEqualsIsConsistentWithArraysEquals(Complex.ofCartesian(Double.NaN, 0.0),
            Complex.ofCartesian(Double.NaN, 1.0), "NaN real and different non-NaN imaginary");
        assertEqualsIsConsistentWithArraysEquals(Complex.ofCartesian(0.0, Double.NaN),
            Complex.ofCartesian(1.0, Double.NaN), "Different non-NaN real and NaN imaginary");
        assertEqualsIsConsistentWithArraysEquals(Complex.ofCartesian(0.0, 0.0), Complex.ofCartesian(-0.0, 0.0),
            "Different real zeros");
        assertEqualsIsConsistentWithArraysEquals(Complex.ofCartesian(0.0, 0.0), Complex.ofCartesian(0.0, -0.0),
            "Different imaginary zeros");

        // Test some values of edge cases
        final double[] values = {Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, -1, 0, 1};
        final ArrayList<Complex> list = createCombinations(values);

        for (final Complex c : list) {
            final double real = c.getReal();
            final double imag = c.getImaginary();

            // Check a copy is equal
            assertEqualsIsConsistentWithArraysEquals(c, Complex.ofCartesian(real, imag), "Copy complex");

            // Perform the smallest change to the two components
            final double realDelta = smallestChange(real);
            final double imagDelta = smallestChange(imag);
            Assertions.assertNotEquals(real, realDelta, "Real was not changed");
            Assertions.assertNotEquals(imag, imagDelta, "Imaginary was not changed");

            assertEqualsIsConsistentWithArraysEquals(c, Complex.ofCartesian(realDelta, imag), "Delta real");
            assertEqualsIsConsistentWithArraysEquals(c, Complex.ofCartesian(real, imagDelta), "Delta imaginary");
        }
    }

    /**
     * Specific test to target different representations that return {@code true} for
     * {@link Complex#isNaN()} are {@code false} for {@link Complex#equals(Object)}.
     */
    @Test
    void testEqualsWithDifferentNaNs() {
        // Test some NaN combinations
        final double[] values = {Double.NaN, 0, 1};
        final ArrayList<Complex> list = createCombinations(values);

        // Is the all-vs-all comparison only the exact same values should be equal, e.g.
        // (nan,0) not equals (nan,nan)
        // (nan,0) equals (nan,0)
        // (nan,0) not equals (0,nan)
        for (int i = 0; i < list.size(); i++) {
            final Complex c1 = list.get(i);
            final Complex copy = Complex.ofCartesian(c1.getReal(), c1.getImaginary());
            assertEqualsIsConsistentWithArraysEquals(c1, copy, "Copy is not equal");
            for (int j = i + 1; j < list.size(); j++) {
                final Complex c2 = list.get(j);
                assertEqualsIsConsistentWithArraysEquals(c1, c2, "Different NaNs should not be equal");
            }
        }
    }

    /**
     * Test the two complex numbers with {@link Complex#equals(Object)} and check the
     * result is consistent with {@link Arrays#equals(double[], double[])}.
     *
     * @param c1 the first complex
     * @param c2 the second complex
     * @param msg the message to append to an assertion error
     */
    private static void assertEqualsIsConsistentWithArraysEquals(Complex c1, Complex c2, String msg) {
        final boolean expected = Arrays.equals(new double[] {c1.getReal(), c1.getImaginary()},
            new double[] {c2.getReal(), c2.getImaginary()});
        final boolean actual = c1.equals(c2);
        Assertions.assertEquals(expected, actual,
            () -> String.format("equals(Object) is not consistent with Arrays.equals: %s. %s vs %s", msg, c1, c2));
    }

    /**
     * Test {@link Complex#hashCode()}. It should be consistent with
     * {@link Arrays#hashCode(double[])} called using the components of the complex number
     * and fulfil the contract of {@link Object#hashCode()}, i.e. objects with different
     * hash codes are {@code false} for {@link Object#equals(Object)}.
     */
    @Test
    void testHashCode() {
        // Test some values match Arrays.hashCode(double[])
        final double[] values = {Double.NaN, Double.NEGATIVE_INFINITY, -3.45, -1, -0.0, 0.0, Double.MIN_VALUE, 1, 3.45,
            Double.POSITIVE_INFINITY};
        final ArrayList<Complex> list = createCombinations(values);

        final String msg = "'equals' not compatible with 'hashCode'";

        for (final Complex c : list) {
            final double real = c.getReal();
            final double imag = c.getImaginary();
            final int expected = Arrays.hashCode(new double[] {real, imag});
            final int hash = c.hashCode();
            Assertions.assertEquals(expected, hash, "hashCode does not match Arrays.hashCode({re, im})");

            // Test a copy has the same hash code, i.e. is not
            // System.identityHashCode(Object)
            final Complex copy = Complex.ofCartesian(real, imag);
            Assertions.assertEquals(hash, copy.hashCode(), "Copy hash code is not equal");

            // MATH-1118
            // "equals" and "hashCode" must be compatible: if two objects have
            // different hash codes, "equals" must return false.
            // Perform the smallest change to the two components.
            // Note: The hash could actually be the same so we check it changes.
            final double realDelta = smallestChange(real);
            final double imagDelta = smallestChange(imag);
            Assertions.assertNotEquals(real, realDelta, "Real was not changed");
            Assertions.assertNotEquals(imag, imagDelta, "Imaginary was not changed");

            final Complex cRealDelta = Complex.ofCartesian(realDelta, imag);
            final Complex cImagDelta = Complex.ofCartesian(real, imagDelta);
            if (hash != cRealDelta.hashCode()) {
                Assertions.assertNotEquals(c, cRealDelta, () -> "real+delta: " + msg);
            }
            if (hash != cImagDelta.hashCode()) {
                Assertions.assertNotEquals(c, cImagDelta, () -> "imaginary+delta: " + msg);
            }
        }
    }

    /**
     * Specific test that different representations of zero satisfy the contract of
     * {@link Object#hashCode()}: if two objects have different hash codes, "equals" must
     * return false. This is an issue with using {@link Double#hashCode(double)} to create
     * hash codes and {@code ==} for equality when using different representations of
     * zero: Double.hashCode(-0.0) != Double.hashCode(0.0) but -0.0 == 0.0 is
     * {@code true}.
     *
     * @see <a
     * href="https://issues.apache.org/jira/projects/MATH/issues/MATH-1118">MATH-1118</a>
     */
    @Test
    void testHashCodeWithDifferentZeros() {
        final double[] values = {-0.0, 0.0};
        final ArrayList<Complex> list = createCombinations(values);

        // Explicit test for issue MATH-1118
        // "equals" and "hashCode" must be compatible
        for (int i = 0; i < list.size(); i++) {
            final Complex c1 = list.get(i);
            for (int j = i + 1; j < list.size(); j++) {
                final Complex c2 = list.get(j);
                if (c1.hashCode() != c2.hashCode()) {
                    Assertions.assertNotEquals(c1, c2, "'equals' not compatible with 'hashCode'");
                }
            }
        }
    }

    /**
     * Creates a list of Complex numbers using an all-vs-all combination of the provided
     * values for both the real and imaginary parts.
     *
     * @param values the values
     * @return the list
     */
    private static ArrayList<Complex> createCombinations(final double[] values) {
        final ArrayList<Complex> list = new ArrayList<>(values.length * values.length);
        for (final double re : values) {
            for (final double im : values) {
                list.add(Complex.ofCartesian(re, im));
            }
        }
        return list;
    }

    /**
     * Perform the smallest change to the value. This returns the next double value
     * adjacent to d in the direction of infinity. Edge cases: if already infinity then
     * return the next closest in the direction of negative infinity; if nan then return
     * 0.
     *
     * @param x the x
     * @return the new value
     */
    private static double smallestChange(double x) {
        if (Double.isNaN(x)) {
            return 0;
        }
        return x == Double.POSITIVE_INFINITY ? Math.nextDown(x) : Math.nextUp(x);
    }

    @Test
    void testAtanhEdgeConditions() {
        // Hits the edge case when imaginary == 0 but real != 0 or 1
        final Complex c = Complex.ofCartesian(2, 0).atanh();
        // Answer from g++
        Assertions.assertEquals(0.54930614433405489, c.getReal());
        Assertions.assertEquals(1.5707963267948966, c.getImaginary());
    }

    @Test
    void testAtanhAssumptions() {
        // Compute the same constants used by atanh
        final double safeUpper = Math.sqrt(Double.MAX_VALUE) / 2;
        final double safeLower = Math.sqrt(Double.MIN_NORMAL) * 2;

        // Can we assume (1+x) = x when x is large
        Assertions.assertEquals(safeUpper, 1 + safeUpper);
        // Can we assume (1-x) = -x when x is large
        Assertions.assertEquals(-safeUpper, 1 - safeUpper);
        // Can we assume (y^2/x) = 0 when y is small and x is large
        Assertions.assertEquals(0, safeLower * safeLower / safeUpper);
        // Can we assume (1-x)^2/y + y = y when x <= 1. Try with x = 0.
        Assertions.assertEquals(safeUpper, 1 / safeUpper + safeUpper);
        // Can we assume (4+y^2) = 4 when y is small
        Assertions.assertEquals(4, 4 + safeLower * safeLower);
        // Can we assume (1-x)^2 = 1 when x is small
        Assertions.assertEquals(1, (1 - safeLower) * (1 - safeLower));
        // Can we assume 1 - y^2 = 1 when y is small
        Assertions.assertEquals(1, 1 - safeLower * safeLower);
        // Can we assume Math.log1p(4 * x / y / y) = (4 * x / y / y) when big y and small
        // x
        final double result = 4 * safeLower / safeUpper / safeUpper;
        Assertions.assertEquals(result, Math.log1p(result));
        Assertions.assertEquals(result, result - result * result / 2, "Expected log1p Taylor series to be redundant");
        // Can we assume if x != 1 then (x-1) is valid for multiplications.
        Assertions.assertNotEquals(0, 1 - Math.nextUp(1));
        Assertions.assertNotEquals(0, 1 - Math.nextDown(1));
    }

    @Test
    void testCoshSinhTanhAssumptions() {
        // Use the same constants used to approximate cosh/sinh with e^|x| / 2
        final double safeExpMax = 708;

        final double big = Math.exp(safeExpMax);
        final double small = Math.exp(-safeExpMax);

        // Overflow assumptions
        Assertions.assertTrue(Double.isFinite(big));
        Assertions.assertTrue(Double.isInfinite(Math.exp(safeExpMax + 2)));

        // Can we assume cosh(x) = (e^x + e^-x) / 2 = e^|x| / 2
        Assertions.assertEquals(big + small, big);
        Assertions.assertEquals(Math.cosh(safeExpMax), big / 2);
        Assertions.assertEquals(Math.cosh(-safeExpMax), big / 2);

        // Can we assume sinh(x) = (e^x - e^-x) / 2 = sign(x) * e^|x| / 2
        Assertions.assertEquals(big - small, big);
        Assertions.assertEquals(small - big, -big);
        Assertions.assertEquals(Math.sinh(safeExpMax), big / 2);
        Assertions.assertEquals(Math.sinh(-safeExpMax), -big / 2);

        // Can we assume sinh(x/2) * cosh(x/2) is finite
        // Can we assume sinh(x/2)^2 is finite
        Assertions.assertTrue(Double.isFinite(Math.sinh(safeExpMax / 2) * Math.cosh(safeExpMax / 2)));
        Assertions.assertTrue(Double.isFinite(Math.sinh(safeExpMax / 2) * Math.sinh(safeExpMax / 2)));

        // Will 2.0 / e^|x| / e^|x| underflow
        Assertions.assertNotEquals(0.0, 2.0 / big);
        Assertions.assertEquals(0.0, 2.0 / big / big);

        // This is an assumption used in sinh/cosh.
        // Will 3 * (e^|x|/2) * y overflow for any positive y
        Assertions.assertTrue(Double.isFinite(0.5 * big * Double.MIN_VALUE * big));
        Assertions.assertTrue(Double.isInfinite(0.5 * big * Double.MIN_VALUE * big * big));

        // Assume the sign of sin(2y) = sin(y) * cos(y) when |y| < pi/2
        for (final double y : new double[] {Math.PI / 2, Math.PI / 4, 1.0, 0.5, 0.0}) {
            Assertions.assertEquals(Math.signum(Math.sin(2 * y)), Math.signum(Math.sin(y) * Math.cos(y)));
            Assertions.assertEquals(Math.signum(Math.sin(2 * -y)), Math.signum(Math.sin(-y) * Math.cos(-y)));
        }

        // tanh: 2.0 / Double.MAX_VALUE does not underflow.
        // Thus 2 sin(2y) / e^2|x| can be computed when e^2|x| only just overflows
        Assertions.assertTrue(2.0 / Double.MAX_VALUE > 0);
    }

    /**
     * Test that sin and cos are linear around zero. This can be used for fast computation
     * of sin and cos together when |x| is small.
     */
    @Test
    void testSinCosLinearAssumptions() {
        // Are cos and sin linear around zero?
        // If cos is still 1 then since d(sin) dx = cos then sin is linear.
        Assertions.assertEquals(1.0, Math.cos(Double.MIN_NORMAL));
        Assertions.assertEquals(Double.MIN_NORMAL, Math.sin(Double.MIN_NORMAL));

        // Are cosh and sinh linear around zero?
        // If cosh is still 1 then since d(sinh) dx = cosh then sinh is linear.
        Assertions.assertEquals(1.0, Math.cosh(Double.MIN_NORMAL));
        Assertions.assertEquals(Double.MIN_NORMAL, Math.sinh(Double.MIN_NORMAL));
    }

    /**
     * Test the abs and sqrt functions are consistent. The definition of sqrt uses abs and
     * the result should be computed using the same representation of the complex number's
     * magnitude (abs). If the sqrt function uses a simple representation
     * {@code sqrt(x^2 + y^2)} then this may have a 1 ulp or more difference from the high
     * accuracy result computed by abs. This will propagate to create differences in sqrt.
     *
     * <p>Note: This test is separated from the similar test for log to allow testing
     * different numbers.
     */
    @Test
    void testAbsVsSqrt() {
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        // Note: All methods implement scaling to ensure the magnitude can be computed.
        // Try very large or small numbers that will over/underflow to test that the
        // scaling
        // is consistent. Note that:
        // - sqrt will reduce the size of the real and imaginary
        // components when |z|>1 and increase them when |z|<1.

        // Each sample fails approximately 3% of the time if using a standard x^2+y^2 in
        // sqrt()
        // and high accuracy representation in abs().
        // Use 1000 samples to ensure the behavior is OK.
        // Do not use data which will over/underflow so we can use a simple computation in
        // the test
        assertAbsVsSqrt(1000,
            () -> Complex.ofCartesian(createFixedExponentNumber(rng, 1000), createFixedExponentNumber(rng, 1000)));
        assertAbsVsSqrt(1000,
            () -> Complex.ofCartesian(createFixedExponentNumber(rng, -1000), createFixedExponentNumber(rng, -1000)));
    }

    private static void assertAbsVsSqrt(int samples, Supplier<Complex> supplier) {
        // Note: All methods implement scaling to ensure the magnitude can be computed.
        // Try very large or small numbers that will over/underflow to test that the
        // scaling
        // is consistent.
        for (int i = 0; i < samples; i++) {
            final Complex z = supplier.get();
            final double abs = z.abs();
            final double x = Math.abs(z.getReal());
            final double y = Math.abs(z.getImaginary());

            // Target the formula provided in the documentation for sqrt:
            // sqrt(x + iy)
            // t = sqrt( 2 (|x| + |x + iy|) )
            // if x >= 0: (t/2, y/t)
            // else : (|y| / t, t/2 * sgn(y))
            // Note this is not the definitional polar computation using absolute and
            // argument:
            // real = sqrt(|z|) * cos(0.5 * arg(z))
            // imag = sqrt(|z|) * sin(0.5 * arg(z))
            final Complex c = z.sqrt();
            final double t = Math.sqrt(2 * (x + abs));
            if (z.getReal() >= 0) {
                Assertions.assertEquals(t / 2, c.getReal());
                Assertions.assertEquals(z.getImaginary() / t, c.getImaginary());
            } else {
                Assertions.assertEquals(y / t, c.getReal());
                Assertions.assertEquals(Math.copySign(t / 2, z.getImaginary()), c.getImaginary());
            }
        }
    }

    /**
     * Test the abs and log functions are consistent. The definition of log uses abs and
     * the result should be computed using the same representation of the complex number's
     * magnitude (abs). If the log function uses a simple representation
     * {@code sqrt(x^2 + y^2)} then this may have a 1 ulp or more difference from the high
     * accuracy result computed by abs. This will propagate to create differences in log.
     *
     * <p>Note: This test is separated from the similar test for sqrt to allow testing
     * different numbers.
     */
    @Test
    void testAbsVsLog() {
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        // Note: All methods implement scaling to ensure the magnitude can be computed.
        // Try very large or small numbers that will over/underflow to test that the
        // scaling
        // is consistent. Note that:
        // - log will set the real component using log(|z|). This will massively reduce
        // the magnitude when |z| >> 1. Highest accuracy will be when |z| is as large
        // as possible before computing the log.

        // No test around |z| == 1 as a high accuracy computation is required:
        // Math.log1p(x*x+y*y-1)

        // Each sample fails approximately 25% of the time if using a standard x^2+y^2 in
        // log()
        // and high accuracy representation in abs(). Use 100 samples to ensure the
        // behavior is OK.
        assertAbsVsLog(100,
            () -> Complex.ofCartesian(createFixedExponentNumber(rng, 1022), createFixedExponentNumber(rng, 1022)));
        assertAbsVsLog(100,
            () -> Complex.ofCartesian(createFixedExponentNumber(rng, -1022), createFixedExponentNumber(rng, -1022)));
    }

    private static void assertAbsVsLog(int samples, Supplier<Complex> supplier) {
        // Note: All methods implement scaling to ensure the magnitude can be computed.
        // Try very large or small numbers that will over/underflow to test that the
        // scaling
        // is consistent.
        for (int i = 0; i < samples; i++) {
            final Complex z = supplier.get();
            final double abs = z.abs();
            final double x = Math.abs(z.getReal());
            final double y = Math.abs(z.getImaginary());

            // log(x + iy) = log(|x + i y|) + i arg(x + i y)
            // Only test the real component
            final Complex c = z.log();
            Assertions.assertEquals(Math.log(abs), c.getReal());
        }
    }

    /**
     * Creates a number in the range {@code [1, 2)} with up to 52-bits in the mantissa.
     * Then modifies the exponent by the given amount.
     *
     * @param rng Source of randomness
     * @param exponent Amount to change the exponent (in range [-1023, 1023])
     * @return the number
     */
    private static double createFixedExponentNumber(UniformRandomProvider rng, int exponent) {
        return Double.longBitsToDouble((rng.nextLong() >>> 12) | ((1023L + exponent) << 52));
    }
}
