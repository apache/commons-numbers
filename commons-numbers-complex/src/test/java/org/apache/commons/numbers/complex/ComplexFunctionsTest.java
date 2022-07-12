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

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;
import java.util.function.DoubleFunction;
import java.util.function.Supplier;

/**
 * Tests for {@link Complex} and {@link ComplexFunctions}.
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
class ComplexFunctionsTest {

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
    void testAbs() {
        final Complex z = Complex.ofCartesian(3.0, 4.0);
        Assertions.assertEquals(5.0, z.abs());
        TestUtils.assertDouble(z, ComplexFunctions::abs, 5.0, "abs");
    }

    @Test
    void testAbsNaN() {
        // The result is NaN if either argument is NaN and the other is not infinite
        Assertions.assertEquals(nan, NAN.abs());
        TestUtils.assertDouble(NAN, ComplexFunctions::abs, nan, "abs");
        Assertions.assertEquals(nan, Complex.ofCartesian(3.0, nan).abs());
        TestUtils.assertDouble(Complex.ofCartesian(3.0, nan), ComplexFunctions::abs, nan, "abs");
        Assertions.assertEquals(nan, Complex.ofCartesian(nan, 3.0).abs());
        TestUtils.assertDouble(Complex.ofCartesian(nan, 3.0), ComplexFunctions::abs, nan, "abs");
        // The result is positive infinite if either argument is infinite
        Assertions.assertEquals(inf, Complex.ofCartesian(inf, nan).abs());
        TestUtils.assertDouble(Complex.ofCartesian(inf, nan), ComplexFunctions::abs, inf, "abs");
        Assertions.assertEquals(inf, Complex.ofCartesian(-inf, nan).abs());
        TestUtils.assertDouble(Complex.ofCartesian(-inf, nan), ComplexFunctions::abs, inf, "abs");
        Assertions.assertEquals(inf, Complex.ofCartesian(nan, inf).abs());
        TestUtils.assertDouble(Complex.ofCartesian(nan, inf), ComplexFunctions::abs, inf, "abs");
        Assertions.assertEquals(inf, Complex.ofCartesian(nan, -inf).abs());
        TestUtils.assertDouble(Complex.ofCartesian(nan, -inf), ComplexFunctions::abs, inf, "abs");
        Assertions.assertEquals(inf, Complex.ofCartesian(inf, 3.0).abs());
        TestUtils.assertDouble(Complex.ofCartesian(inf, 3.0), ComplexFunctions::abs, inf, "abs");
        Assertions.assertEquals(inf, Complex.ofCartesian(-inf, 3.0).abs());
        TestUtils.assertDouble(Complex.ofCartesian(-inf, 3.0), ComplexFunctions::abs, inf, "abs");
        Assertions.assertEquals(inf, Complex.ofCartesian(3.0, inf).abs());
        TestUtils.assertDouble(Complex.ofCartesian(3.0, inf), ComplexFunctions::abs, inf, "abs");
        Assertions.assertEquals(inf, Complex.ofCartesian(3.0, -inf).abs());
        TestUtils.assertDouble(Complex.ofCartesian(3.0, -inf), ComplexFunctions::abs, inf, "abs");
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
        TestUtils.assertDouble(complex.getReal(), complex.getImaginary(), ComplexFunctions::arg, expected, "arg", delta);
        Assertions.assertEquals(actual, complex.arg(), delta);
        TestUtils.assertDouble(complex.getReal(), complex.getImaginary(), ComplexFunctions::arg, actual, "arg", delta);
    }

    @Test
    void testNorm() {
        final Complex z = Complex.ofCartesian(3.0, 4.0);
        Assertions.assertEquals(25.0, z.norm());
        TestUtils.assertDouble(z, ComplexFunctions::norm, 25.0, "norm");

    }

    @Test
    void testNormNaN() {
        // The result is NaN if either argument is NaN and the other is not infinite
        assertNorm(nan, NAN);
        assertNorm(nan, Complex.ofCartesian(3.0, nan));
        assertNorm(nan, Complex.ofCartesian(nan, 3.0));
        // The result is positive infinite if either argument is infinite
        assertNorm(inf, Complex.ofCartesian(inf, nan));
        assertNorm(inf, Complex.ofCartesian(-inf, nan));
        assertNorm(inf, Complex.ofCartesian(nan, inf));
        assertNorm(inf, Complex.ofCartesian(nan, -inf));
    }

    private static void assertNorm(double expected, Complex complex) {
        final double actual = complex.norm();
        Assertions.assertEquals(expected, actual);
        TestUtils.assertDouble(complex, ComplexFunctions::norm, expected, "norm");
    }

    @Test
    void testConjugate() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final Complex z = x.conj();
        Assertions.assertEquals(3.0, z.getReal());
        Assertions.assertEquals(-4.0, z.getImaginary());
        Complex expected = Complex.ofCartesian(3, -4);
        TestUtils.assertComplexUnary(x, Complex::conj, ComplexFunctions::conj, expected, "conj");
    }

    @Test
    void testNegate() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final Complex z = x.negate();
        Assertions.assertEquals(-3.0, z.getReal());
        Assertions.assertEquals(-4.0, z.getImaginary());
        Complex expected = Complex.ofCartesian(-3, -4);
        TestUtils.assertComplexUnary(x, Complex::negate, ComplexFunctions::negate, expected, "negate");
    }

    @Test
    void testMultiply() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final Complex y = Complex.ofCartesian(5.0, 6.0);
        final Complex z = x.multiply(y);
        Assertions.assertEquals(-9.0, z.getReal());
        Assertions.assertEquals(38.0, z.getImaginary());

        Complex expected = Complex.ofCartesian(-9.0, 38.0);
        TestUtils.assertComplexBinary(x, y, Complex::multiply, ComplexFunctions::multiply, expected, "multiply");
    }

    @Test
    void testMultiplyInfInf() {
        final Complex z = infInf.multiply(infInf);
        // Assert.assertTrue(z.isNaN()); // MATH-620
        Assertions.assertTrue(z.isInfinite());

        // Expected results from g++:
        Assertions.assertEquals(Complex.ofCartesian(nan, inf), infInf.multiply(infInf));
        Complex expected = Complex.ofCartesian(nan, inf);
        TestUtils.assertComplexBinary(infInf, infInf, Complex::multiply, ComplexFunctions::multiply, expected, "multiply");
        Assertions.assertEquals(Complex.ofCartesian(inf, nan), infInf.multiply(infNegInf));
        expected = Complex.ofCartesian(inf, nan);
        TestUtils.assertComplexBinary(infInf, infNegInf, Complex::multiply, ComplexFunctions::multiply, expected, "multiply");
        Assertions.assertEquals(Complex.ofCartesian(-inf, nan), infInf.multiply(negInfInf));
        expected = Complex.ofCartesian(-inf, nan);
        TestUtils.assertComplexBinary(infInf, negInfInf, Complex::multiply, ComplexFunctions::multiply, expected, "multiply");
        Assertions.assertEquals(Complex.ofCartesian(nan, -inf), infInf.multiply(negInfNegInf));
        expected = Complex.ofCartesian(nan, -inf);
        TestUtils.assertComplexBinary(infInf, negInfNegInf, Complex::multiply, ComplexFunctions::multiply, expected, "multiply");
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
        Complex expected = Complex.ofCartesian(6.0, 8.0);
        TestUtils.assertComplexBinary(x, ofReal(y), Complex::multiply, ComplexFunctions::multiply, expected, "multiply");
        z = x.multiply(-y);
        Assertions.assertEquals(-6.0, z.getReal());
        Assertions.assertEquals(-8.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.multiply(ofReal(-y)));
        expected = Complex.ofCartesian(-6.0, -8.0);
        TestUtils.assertComplexBinary(x, ofReal(-y), Complex::multiply, ComplexFunctions::multiply, expected, "multiply");
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
        Complex expected = Complex.ofCartesian(nan, nan);
        TestUtils.assertComplexBinary(x, ofReal(y), Complex::multiply, ComplexFunctions::multiply, expected, "multiply");
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
        Complex expected = Complex.ofCartesian(inf, inf);
        TestUtils.assertComplexBinary(x, ofReal(y), Complex::multiply, ComplexFunctions::multiply, expected, "multiply");
        z = x.multiply(-y);
        Assertions.assertEquals(-inf, z.getReal());
        Assertions.assertEquals(-inf, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.multiply(ofReal(-y)));
        expected = Complex.ofCartesian(-inf, -inf);
        TestUtils.assertComplexBinary(x, ofReal(-y), Complex::multiply, ComplexFunctions::multiply, expected, "multiply");
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
        Complex expected = Complex.ofCartesian(0.0, 0.0);
        TestUtils.assertComplexBinary(x, ofReal(y), Complex::multiply, ComplexFunctions::multiply, expected, "multiply");
        z = x.multiply(-y);
        Assertions.assertEquals(-0.0, z.getReal());
        Assertions.assertEquals(-0.0, z.getImaginary());
        // Sign-preservation is a problem for imaginary: 0.0 - -0.0 == 0.0
        final Complex z2 = x.multiply(ofReal(-y));
        Assertions.assertEquals(-0.0, z2.getReal());
        Assertions.assertEquals(0.0, z2.getImaginary(), "Expected no sign preservation");
        expected = Complex.ofCartesian(-0.0, 0.0);
        TestUtils.assertComplexBinary(x, ofReal(-y), Complex::multiply, ComplexFunctions::multiply, expected, "multiply");
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
        Complex expected = Complex.ofCartesian(-8.0, 6.0);
        TestUtils.assertComplexBinary(x, ofImaginary(y), Complex::multiply, ComplexFunctions::multiply, expected, "multiply");
        z = x.multiplyImaginary(-y);
        Assertions.assertEquals(8.0, z.getReal());
        Assertions.assertEquals(-6.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.multiply(ofImaginary(-y)));
        expected = Complex.ofCartesian(8.0, -6.0);
        TestUtils.assertComplexBinary(x, ofImaginary(-y), Complex::multiply, ComplexFunctions::multiply, expected, "multiply");
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
        Complex expected = Complex.ofCartesian(nan, nan);
        TestUtils.assertComplexBinary(x, ofImaginary(y), Complex::multiply, ComplexFunctions::multiply, expected, "multiply");
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
        Complex expected = Complex.ofCartesian(-inf, inf);
        TestUtils.assertComplexBinary(x, ofImaginary(y), Complex::multiply, ComplexFunctions::multiply, expected, "multiply");
        z = x.multiplyImaginary(-y);
        Assertions.assertEquals(inf, z.getReal());
        Assertions.assertEquals(-inf, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.multiply(ofImaginary(-y)));
        expected = Complex.ofCartesian(inf, -inf);
        TestUtils.assertComplexBinary(x, ofImaginary(-y), Complex::multiply, ComplexFunctions::multiply, expected, "multiply");
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
        Complex expected = Complex.ofCartesian(0.0, 0.0);
        TestUtils.assertComplexBinary(x, ofImaginary(y), Complex::multiply, ComplexFunctions::multiply, expected, "multiply");

        z = x.multiplyImaginary(-y);
        Assertions.assertEquals(0.0, z.getReal());
        Assertions.assertEquals(-0.0, z.getImaginary());
        // Sign-preservation is a problem for imaginary: -0.0 - 0.0 == 0.0
        z2 = x.multiply(ofImaginary(-y));
        Assertions.assertEquals(0.0, z2.getReal());
        Assertions.assertEquals(0.0, z2.getImaginary(), "Expected no sign preservation");
        expected = Complex.ofCartesian(0.0, 0.0);
        TestUtils.assertComplexBinary(x, ofImaginary(-y), Complex::multiply, ComplexFunctions::multiply, expected, "multiply");
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
                Complex expected = Complex.ofCartesian(-b, a);
                TestUtils.assertComplexBinary(c, Complex.I, Complex::multiply, ComplexFunctions::multiply, expected, "multiply");
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
                Complex expected = Complex.ofCartesian(b, -a);
                for (final Complex negI : negIs) {
                    final Complex z = c.multiply(negI);
                    Assertions.assertEquals(x, z);
                    TestUtils.assertComplexBinary(c, negI, Complex::multiply, ComplexFunctions::multiply, expected, "multiply");
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
                    Complex expected = Complex.ofCartesian(0, 0);
                    TestUtils.assertComplexBinary(c, Complex.I, Complex::multiply, ComplexFunctions::multiply, expected, "multiply");
                    Assertions.assertNotEquals(x, z);
                } else {
                    Assertions.assertEquals(x, z);
                    Complex expected = Complex.ofCartesian(-b, a);
                    TestUtils.assertComplexBinary(c, Complex.I, Complex::multiply, ComplexFunctions::multiply, expected, "multiply");
                }
            }
        }
    }

    @Test
    void testMultiplyZeroByNegativeI() {
        // Depending on how we represent -I this does not work for 2/4 cases
        // but the cases are different. Here we test the negation of I.
        final Complex negI1 = Complex.I.negate();
        final ComplexDouble negI2 = ComplexFunctions.negate(Complex.I.getReal(), Complex.I.getImaginary(), TestUtils.ComplexDoubleConstructor.of());
        final double[] zeros = {-0.0, 0.0};
        for (final double a : zeros) {
            for (final double b : zeros) {
                final Complex c = Complex.ofCartesian(a, b);
                final Complex x1 = c.multiplyImaginary(-1.0);
                final ComplexDouble x2 = ComplexFunctions.multiplyImaginary(c.getReal(), c.getImaginary(), -1.0, TestUtils.ComplexDoubleConstructor.of());
                // Check verses algebra solution
                Assertions.assertEquals(b, x1.getReal());
                Assertions.assertEquals(-a, x1.getImaginary());

                Assertions.assertEquals(b, x2.getReal());
                Assertions.assertEquals(-a, x2.getImaginary());

                final Complex z1 = c.multiply(negI1);
                final Complex z2 = c.multiply(Complex.I).negate();

                ComplexBinaryOperator<ComplexDouble> multiplyNegate = ComplexFunctions::multiply;
                multiplyNegate = multiplyNegate.andThen(ComplexFunctions::negate);

                final ComplexDouble z3 = ComplexFunctions.multiply(c.getReal(), c.getImaginary(), negI2.getReal(), negI2.getImaginary(), TestUtils.ComplexDoubleConstructor.of());
                final ComplexDouble z4 = multiplyNegate.apply(c.getReal(), c.getImaginary(), Complex.I.getReal(), Complex.I.getImaginary(), TestUtils.ComplexDoubleConstructor.of());

                // Does not work when imaginary part is -0.0.
                if (Double.compare(b, -0.0) == 0) {
                    // (-0.0,-0.0).multiply( (-0.0,-1) ) => ( 0.0, 0.0) expected (-0.0, 0.0)
                    // ( 0.0,-0.0).multiply( (-0.0,-1) ) => (-0.0, 0.0) expected (-0.0,-0.0)
                    // Sign is allowed to be different for zero.
                    Assertions.assertEquals(0, z1.getReal(), 0.0);
                    Assertions.assertEquals(0, z1.getImaginary(), 0.0);
                    Assertions.assertNotEquals(x1, z1);

                    Assertions.assertEquals(0, z3.getReal(), 0.0);
                    Assertions.assertEquals(0, z3.getImaginary(), 0.0);
                    Assertions.assertNotEquals(x2, z3);
                    // When multiply by I.negate() fails multiply by I then negate()
                    // works!
                    Assertions.assertEquals(x1, z2);
                    Assertions.assertEquals(x2, z4);
                } else {
                    Assertions.assertEquals(x1, z1);
                    Assertions.assertEquals(x2, z3);
                    // When multiply by I.negate() works multiply by I then negate()
                    // fails!
                    Assertions.assertNotEquals(x1, z2);
                    Assertions.assertNotEquals(x2, z4);
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
        Complex expected = Complex.ofCartesian(39.0 / 61.0, 2.0 / 61.0);
        TestUtils.assertComplexBinary(x, y, Complex::divide, ComplexFunctions::divide, expected, "divide");
    }

    @Test
    void testDivideZero() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final Complex z = x.divide(Complex.ZERO);
        Assertions.assertEquals(INF, z);
        TestUtils.assertComplexBinary(x, Complex.ZERO, Complex::divide, ComplexFunctions::divide, INF, "divide");
    }

    @Test
    void testDivideZeroZero() {
        final Complex x = Complex.ofCartesian(0.0, 0.0);
        final Complex z = x.divide(Complex.ZERO);
        Assertions.assertEquals(NAN, z);
        TestUtils.assertComplexBinary(x, Complex.ZERO, Complex::divide, ComplexFunctions::divide, NAN, "divide");
    }

    @Test
    void testDivideNaN() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final Complex z1 = x.divide(NAN);
        Assertions.assertTrue(z1.isNaN());
        ComplexDouble z2 = ComplexFunctions.divide(x.getReal(), x.getImaginary(), NAN.getReal(), NAN.getImaginary(), TestUtils.ComplexDoubleConstructor.of());
        Assertions.assertTrue(ComplexFunctions.isNaN(z2));
    }

    @Test
    void testDivideNanInf() {
        Complex z = oneInf.divide(Complex.ONE);
        Assertions.assertTrue(Double.isNaN(z.getReal()));
        Assertions.assertEquals(inf, z.getImaginary());

        TestUtils.assertComplexBinary(oneInf, Complex.ONE, Complex::divide, ComplexFunctions::divide, Complex.ofCartesian(nan, inf), "divide");

        z = negInfNegInf.divide(oneNan);
        Assertions.assertTrue(Double.isNaN(z.getReal()));
        Assertions.assertTrue(Double.isNaN(z.getImaginary()));

        TestUtils.assertComplexBinary(negInfNegInf, oneNan, Complex::divide, ComplexFunctions::divide, Complex.ofCartesian(nan, nan), "divide");

        z = negInfInf.divide(Complex.ONE);
        Assertions.assertTrue(Double.isInfinite(z.getReal()));
        Assertions.assertTrue(Double.isInfinite(z.getImaginary()));

        TestUtils.assertComplexBinary(negInfInf, Complex.ONE, Complex::divide, ComplexFunctions::divide, (r, i) -> Double.isInfinite(r) && Double.isInfinite(i), "divide");

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
        Complex expected = Complex.ofCartesian(1.5, 2.0);
        TestUtils.assertComplexBinary(x, ofReal(y), Complex::divide, ComplexFunctions::divide, expected, "divide");
        z = x.divide(-y);
        Assertions.assertEquals(-1.5, z.getReal());
        Assertions.assertEquals(-2.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.divide(ofReal(-y)));
        expected = Complex.ofCartesian(-1.5, -2.0);
        TestUtils.assertComplexBinary(x, ofReal(-y), Complex::divide, ComplexFunctions::divide, expected, "divide");
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
        Complex expected = Complex.ofCartesian(nan, nan);
        TestUtils.assertComplexBinary(x, ofReal(y), Complex::divide, ComplexFunctions::divide, expected, "divide");
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
        Complex expected = Complex.ofCartesian(0.0, 0.0);
        TestUtils.assertComplexBinary(x, ofReal(y), Complex::divide, ComplexFunctions::divide, expected, "divide");

        z = x.divide(-y);
        Assertions.assertEquals(-0.0, z.getReal());
        Assertions.assertEquals(-0.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.divide(ofReal(-y)));
        expected = Complex.ofCartesian(-0.0, -0.0);
        TestUtils.assertComplexBinary(x, ofReal(-y), Complex::divide, ComplexFunctions::divide, expected, "divide");
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
        Complex expected = Complex.ofCartesian(inf, inf);
        TestUtils.assertComplexBinary(x, ofReal(y), Complex::divide, ComplexFunctions::divide, expected, "divide");

        z = x.divide(-y);
        Assertions.assertEquals(-inf, z.getReal());
        Assertions.assertEquals(-inf, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.divide(ofReal(-y)));
        expected = Complex.ofCartesian(-inf, -inf);
        TestUtils.assertComplexBinary(x, ofReal(-y), Complex::divide, ComplexFunctions::divide, expected, "divide");
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
        Complex expected = Complex.ofCartesian(2.0, -1.5);
        TestUtils.assertComplexBinary(x, ofImaginary(y), Complex::divide, ComplexFunctions::divide, expected, "divide");

        z = x.divideImaginary(-y);
        Assertions.assertEquals(-2.0, z.getReal());
        Assertions.assertEquals(1.5, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.divide(ofImaginary(-y)));
        expected = Complex.ofCartesian(-2.0, 1.5);
        TestUtils.assertComplexBinary(x, ofImaginary(-y), Complex::divide, ComplexFunctions::divide, expected, "divide");
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
        Complex expected = Complex.ofCartesian(nan, nan);
        TestUtils.assertComplexBinary(x, ofImaginary(y), Complex::divide, ComplexFunctions::divide, expected, "divide");
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
        Complex expected = Complex.ofCartesian(0.0, -0.0);
        TestUtils.assertComplexBinary(x, ofImaginary(y), Complex::divide, ComplexFunctions::divide, expected, "divide");

        z = x.divideImaginary(-y);
        Assertions.assertEquals(-0.0, z.getReal());
        Assertions.assertEquals(0.0, z.getImaginary());
        // Equivalent
        Assertions.assertEquals(z, x.divide(ofImaginary(-y)));
        expected = Complex.ofCartesian(-0.0, -0.0);
        TestUtils.assertComplexBinary(x, ofImaginary(-y), Complex::divide, ComplexFunctions::divide, expected, "divide");
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

        TestUtils.assertComplexBinary(x, ofImaginary(y), Complex::divide, ComplexFunctions::divide, Complex.ofCartesian(inf, inf), "Expected no sign preservation for imaginary divide");

        z = x.divideImaginary(-y);
        Assertions.assertEquals(-inf, z.getReal());
        Assertions.assertEquals(inf, z.getImaginary());
        // Sign-preservation is a problem for real: 0.0 + -0.0 == 0.0
        z2 = x.divide(ofImaginary(-y));
        Assertions.assertEquals(inf, z2.getReal(), "Expected no sign preservation");
        Assertions.assertEquals(inf, z2.getImaginary());

        TestUtils.assertComplexBinary(x, ofImaginary(-y), Complex::divide, ComplexFunctions::divide, Complex.ofCartesian(inf, inf), "Expected no sign preservation for real divide");

    }

    /**
     * Arithmetic test using combinations of +/- x for real, imaginary and the double
     * argument for multiply and divide, where x is zero or
     * non-zero.
     *
     * <p>The differences to the same argument as a Complex are tested. The only
     * differences should be the sign of zero in certain cases.
     */
    @Test
    void testSignedArithmetic() {

        // 4: (-0.0,-x) * +x
        // 4: (+0.0,-0.0) * x
        // 4: (+0.0,x) * -x
        // 2: (-y,-x) * +0.0
        // 2: (+y,+0.0) * -x
        // 2: (+y,-0.0) * +x
        // 2: (+y,-x) * -0.0
        // 2: (+x,-y) * +0.0
        // 2: (+x,+y) * -0.0
        assertSignedZeroArithmetic("multiplyReal", Complex::multiply, ComplexFunctions::multiply, ComplexFunctionsTest::ofReal,
            Complex::multiply, ComplexFunctions::multiply,
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
        assertSignedZeroArithmetic("multiplyImaginary", Complex::multiplyImaginary, ComplexFunctions::multiplyImaginary, ComplexFunctionsTest::ofImaginary,
            Complex::multiply, ComplexFunctions::multiply, 0b11000110110101001000000010000001110001111101011010000010100000L);
        // 2: (-0.0,0) / +y
        // 2: (+0.0,+x) / -y
        // 2: (-x,0) / -y
        // 1: (-0.0,+y) / +y
        // 1: (-y,+0.0) / -y
        assertSignedZeroArithmetic("divideReal", Complex::divide, (r, i, d, c) -> c.apply(r / d, i / d), ComplexFunctionsTest::ofReal, Complex::divide, ComplexFunctions::divide,
            0b100100001000000010000001000000011001000L);

        // DivideImaginary has its own test as the result is not always equal ignoring the
        // sign.
    }

    private static void assertSignedZeroArithmetic(String name, BiFunction<Complex, Double, Complex> doubleOperation1, ComplexScalarFunction<ComplexDouble> doubleOperation2,
        DoubleFunction<Complex> doubleToComplex, BiFunction<Complex, Complex, Complex> complexOperation1, ComplexBinaryOperator<ComplexDouble> complexOperation2,
        long expectedFailures) {
        // With an operation on zero or non-zero arguments
        final double[] arguments = {-0.0, 0.0, -2, 3};
        for (final double a : arguments) {
            for (final double b : arguments) {
                final Complex c = Complex.ofCartesian(a, b);
                for (final double arg : arguments) {
                    final Complex y1 = doubleOperation1.apply(c, arg);
                    final Complex z1 = complexOperation1.apply(c, doubleToComplex.apply(arg));

                    final ComplexDouble y2 = doubleOperation2.apply(c.getReal(), c.getImaginary(), arg, TestUtils.ComplexDoubleConstructor.of());
                    final ComplexDouble operand = doubleToComplex.apply(arg);
                    final ComplexDouble z2 = complexOperation2.apply(c.getReal(), c.getImaginary(), operand.getReal(), operand.getImaginary(), TestUtils.ComplexDoubleConstructor.of());

                    final boolean expectedFailure = (expectedFailures & 0x1) == 1;
                    expectedFailures >>>= 1;
                    // Check the same answer. Sign is allowed to be different for zero.
                    Assertions.assertEquals(y1.getReal(), z1.getReal(), 0, () -> c + " " + name + " " + arg + ": real");
                    Assertions.assertEquals(y1.getImaginary(), z1.getImaginary(), 0,
                        () -> c + " " + name + " " + arg + ": imaginary");
                    Assertions.assertEquals(expectedFailure, !y1.equals(z1),
                        () -> c + " " + name + " " + arg + ": sign-difference");

                    Assertions.assertEquals(y2.getReal(), z2.getReal(), 0, () -> c + " " + name + " " + arg + ": real");
                    Assertions.assertEquals(y2.getImaginary(), z2.getImaginary(), 0,
                        () -> c + " " + name + " " + arg + ": imaginary");
                    Assertions.assertEquals(expectedFailure, !y2.equals(z2),
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
                    final ComplexDouble y1 = TestUtils.ComplexDoubleConstructor.of().apply(c.getImaginary() / arg, -c.getReal() / arg);

                    Complex operand = ofImaginary(arg);
                    Complex z = c.divide(ofImaginary(arg));
                    ComplexDouble z2 = ComplexFunctions.divide(c.getReal(), c.getImaginary(), operand.getReal(), operand.getImaginary(), TestUtils.ComplexDoubleConstructor.of());
                    final boolean expectedFailure = (expectedFailures & 0x1) == 1;
                    expectedFailures >>>= 1;
                    // If divide by zero then the divide(Complex) method matches divide by real.
                    // To match divide by imaginary requires multiplication by I.
                    if (arg == 0) {
                        // Same result if multiplied by I. The sign may not match so
                        // optionally ignore the sign of the infinity.
                        z = z.multiplyImaginary(1);
                        z2 = ComplexFunctions.multiplyImaginary(z2.getReal(), z2.getImaginary(), TestUtils.ComplexDoubleConstructor.of());
                        final double ya = expectedFailure ? Math.abs(y.getReal()) : y.getReal();
                        final double yb = expectedFailure ? Math.abs(y.getImaginary()) : y.getImaginary();

                        final double za = expectedFailure ? Math.abs(z.getReal()) : z.getReal();
                        final double zb = expectedFailure ? Math.abs(z.getImaginary()) : z.getImaginary();

                        final double za2 = expectedFailure ? Math.abs(z2.getReal()) : z2.getReal();
                        final double zb2 = expectedFailure ? Math.abs(z2.getImaginary()) : z2.getImaginary();
                        Assertions.assertEquals(ya, za, () -> c + " divideImaginary " + arg + ": real");
                        Assertions.assertEquals(yb, zb, () -> c + " divideImaginary " + arg + ": imaginary");

                        Assertions.assertEquals(ya, za2, () -> c + " ComplexFunctions.divideImaginary " + arg + ": real");
                        Assertions.assertEquals(yb, zb2, () -> c + " ComplexFunctions.divideImaginary " + arg + ": imaginary");
                    } else {
                        // Check the same answer. Sign is allowed to be different for zero.
                        Assertions.assertEquals(y.getReal(), z.getReal(), 0,
                            () -> c + " divideImaginary " + arg + ": real");
                        Assertions.assertEquals(y.getImaginary(), z.getImaginary(), 0,
                            () -> c + " divideImaginary " + arg + ": imaginary");
                        Assertions.assertEquals(expectedFailure, !y.equals(z),
                            () -> c + " divideImaginary " + arg + ": sign-difference");

                        Assertions.assertEquals(y1.getReal(), z2.getReal(), 0,
                            () -> c + " ComplexFunctions.divideImaginary " + arg + ": real");
                        Assertions.assertEquals(y1.getImaginary(), z2.getImaginary(), 0,
                            () -> c + " ComplexFunctions.divideImaginary " + arg + ": imaginary");
                        Assertions.assertEquals(expectedFailure, !y1.equals(z2),
                            () -> c + " ComplexFunctions.divideImaginary " + arg + ": sign-difference");
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
            // real part is prone to floating-point error so use a delta
            // imaginary part should be exact
            Complex expected = Complex.ofCartesian(lnz.getReal() / ln10, lnz.getImaginary());
            TestUtils.assertComplexUnary(z, Complex::log10, ComplexFunctions::log10, expected, "log10", 1e-12, 0.0D);

        }
    }

    @Test
    void testPow() {
        final Complex x = Complex.ofCartesian(3, 4);
        final double yDouble = 5.0;
        final Complex yComplex = ofReal(yDouble);
        Assertions.assertEquals(x.pow(yComplex), x.pow(yDouble));
        TestUtils.assertComplexScalar(x, yDouble, ComplexFunctions::pow, x.pow(yComplex), x.pow(yDouble), "pow");

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
        Complex expected = Complex.ofCartesian(-0.008983291021129429, 1.1001358594835313e-18);
        TestUtils.assertComplexBinary(x, z, Complex::pow, ComplexFunctions::pow, expected, "pow");
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
        TestUtils.assertComplexBinary(Complex.ZERO, z, Complex::pow, ComplexFunctions::pow, expected, "pow");
    }

    @Test
    void testPowScalerRealZero() {
        // Hits the edge case when real == 0 but imaginary != 0
        final Complex x = Complex.ofCartesian(0, 1);
        final Complex c = x.pow(2);
        // Answer from g++
        Assertions.assertEquals(-1, c.getReal());
        Assertions.assertEquals(1.2246467991473532e-16, c.getImaginary());
        Complex expected = Complex.ofCartesian(-1, 1.2246467991473532e-16);
        TestUtils.assertComplexScalar(x, 2, ComplexFunctions::pow, expected, x.pow(2), "pow");
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
        TestUtils.assertComplexScalar(Complex.ZERO, exp, ComplexFunctions::pow, expected, Complex.ZERO.pow(exp), "pow");
    }

    @Test
    void testPowNanBase() {
        final Complex x = NAN;
        final double yDouble = 5.0;
        final Complex yComplex = ofReal(yDouble);
        Assertions.assertEquals(x.pow(yComplex), x.pow(yDouble));
        TestUtils.assertComplexScalar(x, yDouble, ComplexFunctions::pow, x.pow(yComplex), x.pow(yDouble), "pow");
    }

    @Test
    void testPowNanExponent() {
        final Complex x = Complex.ofCartesian(3, 4);
        final double yDouble = Double.NaN;
        final Complex yComplex = ofReal(yDouble);
        Assertions.assertEquals(x.pow(yComplex), x.pow(yDouble));
        TestUtils.assertComplexScalar(x, yDouble, ComplexFunctions::pow, x.pow(yComplex), x.pow(yDouble), "pow");
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
                TestUtils.assertComplexUnary(z, Complex::sqrt, ComplexFunctions::sqrt, sqrtz, "sqrt", tol);
                TestUtils.assertEquals(sqrtz, z.sqrt(), tol);
            }
        }
    }

    @Test
    void testAtanhEdgeConditions() {
        // Hits the edge case when imaginary == 0 but real != 0 or 1
        final Complex c = Complex.ofCartesian(2, 0);
        Complex r = c.atanh();
        // Answer from g++
        Assertions.assertEquals(0.54930614433405489, r.getReal());
        Assertions.assertEquals(1.5707963267948966, r.getImaginary());
        Complex expected = Complex.ofCartesian(0.54930614433405489, 1.5707963267948966);
        TestUtils.assertComplexUnary(c, Complex::atanh, ComplexFunctions::atanh, expected, "atanh");
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
