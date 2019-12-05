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

import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Tests the standards defined by the C.99 standard for complex numbers
 * defined in ISO/IEC 9899, Annex G.
 *
 * @see <a href="http://www.open-std.org/JTC1/SC22/WG14/www/standards">
 *    ISO/IEC 9899 - Programming languages - C</a>
 */
public class CStandardTest {

    private static final double inf = Double.POSITIVE_INFINITY;
    private static final double negInf = Double.NEGATIVE_INFINITY;
    private static final double nan = Double.NaN;
    private static final double max = Double.MAX_VALUE;
    private static final double piOverFour = Math.PI / 4.0;
    private static final double piOverTwo = Math.PI / 2.0;
    private static final double threePiOverFour = 3.0 * Math.PI / 4.0;
    private static final Complex oneZero = complex(1, 0);
    private static final Complex oneInf = complex(1, inf);
    private static final Complex oneNaN = complex(1, nan);
    private static final Complex oneNegZero = complex(1, -0.0);
    private static final Complex zeroInf = complex(0, inf);
    private static final Complex zeroNegInf = complex(0, negInf);
    private static final Complex zeroNegZero = complex(0, -0.0);
    private static final Complex zeroNaN = complex(0, nan);
    private static final Complex zeroPiTwo = complex(0.0, piOverTwo);
    private static final Complex negZeroPiTwo = complex(-0.0, piOverTwo);
    private static final Complex negZeroZero = complex(-0.0, 0);
    private static final Complex negZeroNegZero = complex(-0.0, -0.0);
    private static final Complex negZeroNaN = complex(-0.0, nan);
    private static final Complex negI = complex(0.0, -1.0);
    private static final Complex infOne = complex(inf, 1);
    private static final Complex infZero = complex(inf, 0);
    private static final Complex infNaN = complex(inf, nan);
    private static final Complex infInf = complex(inf, inf);
    private static final Complex infPiTwo = complex(inf, piOverTwo);
    private static final Complex infThreePiFour = complex(inf, threePiOverFour);
    private static final Complex infPiFour = complex(inf, piOverFour);
    private static final Complex infPi = complex(inf, Math.PI);
    private static final Complex infNegZero = complex(inf, -0.0);
    private static final Complex negOneZero = complex(-1, 0);
    private static final Complex negOneNegZero = complex(-1, -0.0);
    private static final Complex negInfInf = complex(negInf, inf);
    private static final Complex negInfZero = complex(negInf, 0);
    private static final Complex negInfNegZero = complex(negInf, -0.0);
    private static final Complex negInfOne = complex(negInf, 1);
    private static final Complex negInfNaN = complex(negInf, nan);
    private static final Complex negInfPi = complex(negInf, Math.PI);
    private static final Complex negInfPiFour = complex(negInf, piOverFour);
    private static final Complex nanInf = complex(nan, inf);
    private static final Complex nanNegInf = complex(nan, negInf);
    private static final Complex nanZero = complex(nan, 0);
    private static final Complex nanOne = complex(nan, 1);
    private static final Complex piTwoNaN = complex(piOverTwo, nan);
    private static final Complex piNegInf = complex(Math.PI, negInf);
    private static final Complex piTwoNegInf = complex(piOverTwo, negInf);
    private static final Complex piTwoNegZero = complex(piOverTwo, -0.0);
    private static final Complex threePiFourNegInf = complex(threePiOverFour, negInf);
    private static final Complex piFourNegInf = complex(piOverFour, negInf);
    private static final Complex NAN = complex(nan, nan);
    private static final Complex maxMax = complex(max, max);
    private static final Complex maxNan = complex(max, nan);
    private static final Complex nanMax = complex(nan, max);

    /**
     * The type of function.
     */
    private enum FunctionType {
        /** Odd: f(z) = -f(-z). */
        ODD,
        /** Even: f(z) = f(-z). */
        EVEN,
        /** Not Odd or Even. */
        NONE;
    }

    /**
     * Assert the two complex numbers have equivalent real and imaginary components as
     * defined by the {@code ==} operator.
     *
     * @param c1 the first complex (actual)
     * @param c2 the second complex (expected)
     */
    private static void assertComplex(Complex c1, Complex c2) {
        // Use a delta of zero to allow comparison of -0.0 to 0.0
        Assertions.assertEquals(c2.getReal(), c1.getReal(), 0.0, "real");
        Assertions.assertEquals(c2.getImaginary(), c1.getImaginary(), 0.0, "imaginary");
    }

    /**
     * Assert the operation on the two complex numbers.
     *
     * @param c1 the first complex
     * @param c2 the second complex
     * @param operation the operation
     * @param operationName the operation name
     * @param expected the expected
     * @param expectedName the expected name
     */
    private static void assertOperation(Complex c1, Complex c2,
            BiFunction<Complex, Complex, Complex> operation, String operationName,
            Predicate<Complex> expected, String expectedName) {
        final Complex z = operation.apply(c1, c2);
        Assertions.assertTrue(expected.test(z),
            () -> String.format("%s expected: %s %s %s = %s", expectedName, c1, operationName, c2, z));
    }

    /**
     * Assert the operation on the complex number satisfies the conjugate equality.
     *
     * <pre>
     * op(conj(z)) = conj(op(z))
     * </pre>
     *
     * <p>The results must be binary equal. This includes the sign of zero.
     *
     * @param operation the operation
     */
    private static void assertConjugateEquality(UnaryOperator<Complex> operation) {
        // Edge cases
        final double[] parts = {Double.NEGATIVE_INFINITY, -1, -0.0, 0.0, 1,
                                Double.POSITIVE_INFINITY, Double.NaN};
        for (final double x : parts) {
            for (final double y : parts) {
                // No conjugate for imaginary NaN
                if (!Double.isNaN(y)) {
                    assertConjugateEquality(complex(x, y), operation);
                }
            }
        }
        // Random numbers
        final UniformRandomProvider rng = RandomSource.create(RandomSource.SPLIT_MIX_64);
        for (int i = 0; i < 100; i++) {
            final double x = next(rng);
            final double y = next(rng);
            assertConjugateEquality(complex(x, y), operation);
        }
    }

    /**
     * Assert the operation on the complex number satisfies the conjugate equality.
     *
     * <pre>
     * op(conj(z)) = conj(op(z))
     * </pre>
     *
     * <p>The results must be binary equal. This includes the sign of zero.
     *
     * @param z the complex number
     * @param operation the operation
     */
    private static void assertConjugateEquality(Complex z,
            UnaryOperator<Complex> operation) {
        final Complex c1 = operation.apply(z.conjugate());
        final Complex c2 = operation.apply(z).conjugate();

        // Test for binary equality
        if (!equals(c1.getReal(), c2.getReal()) ||
            !equals(c1.getImaginary(), c2.getImaginary())) {
            Assertions.fail(String.format("Conjugate equality failed (z=%s). Expected: %s but was: %s",
                                          z, c1, c2));
        }
    }

    /**
     * Assert the operation on the complex number is odd or even.
     *
     * <pre>
     * Odd : f(z) = -f(-z)
     * Even: f(z) =  f(-z)
     * </pre>
     *
     * <p>The results must be binary equal. This includes the sign of zero.
     *
     * @param operation the operation
     * @param type the type
     */
    private static void assertFunctionType(UnaryOperator<Complex> operation, FunctionType type) {
        // Note: It may not be possible to satisfy the conjugate equality
        // and be an odd/even function with regard to zero.
        // The C99 standard allows for these cases to have unspecified sign.
        // This test ignores parts that can result in unspecified signed results.
        // The valid edge cases should be tested for each function separately.
        if (type == FunctionType.NONE) {
            return;
        }

        // Edge cases around zero.
        final double[] parts = {-2, -1, -0.0, 0.0, 1, 2};
        for (final double x : parts) {
            for (final double y : parts) {
                assertFunctionType(complex(x, y), operation, type);
            }
        }
        // Random numbers
        final UniformRandomProvider rng = RandomSource.create(RandomSource.SPLIT_MIX_64);
        for (int i = 0; i < 100; i++) {
            final double x = next(rng);
            final double y = next(rng);
            assertFunctionType(complex(x, y), operation, type);
        }
    }

    /**
     * Assert the operation on the complex number is odd or even.
     *
     * <pre>
     * Odd : f(z) = -f(-z)
     * Even: f(z) =  f(-z)
     * </pre>
     *
     * <p>The results must be binary equal. This includes the sign of zero.
     *
     * @param z the complex number
     * @param operation the operation
     * @param type the type (assumed to be ODD/EVEN)
     */
    private static void assertFunctionType(Complex z,
            UnaryOperator<Complex> operation, FunctionType type) {
        final Complex c1 = operation.apply(z);
        Complex c2 = operation.apply(z.negate());
        if (type == FunctionType.ODD) {
            c2 = c2.negate();
        }

        // Test for binary equality
        if (!equals(c1.getReal(), c2.getReal()) ||
            !equals(c1.getImaginary(), c2.getImaginary())) {
            Assertions.fail(String.format("%s equality failed (z=%s, -z=%s). Expected: %s but was: %s",
                                          type, z, z.negate(), c1, c2));
        }
    }

    /**
     * Assert the operation on the complex number is equal to the expected value.
     * If the imaginary part is not NaN the operation must also satisfy the conjugate equality.
     *
     * <pre>
     * op(conj(z)) = conj(op(z))
     * </pre>
     *
     * <p>The results must be binary equal. This includes the sign of zero.
     *
     * @param z the complex
     * @param operation the operation
     * @param expected the expected
     */
    private static void assertComplex(Complex z,
            UnaryOperator<Complex> operation, Complex expected) {
        assertComplex(z, operation, expected, FunctionType.NONE);
    }

    /**
     * Assert the operation on the complex number is equal to the expected value. If the
     * imaginary part is not NaN the operation must also satisfy the conjugate equality.
     *
     * <pre>
     * op(conj(z)) = conj(op(z))
     * </pre>
     *
     * <p>If the function type is ODD/EVEN the operation must satisfy the function type equality.
     *
     * <pre>
     * Odd : f(z) = -f(-z)
     * Even: f(z) =  f(-z)
     * </pre>
     *
     * <p>The results must be binary equal. This includes the sign of zero.
     *
     * @param z the complex
     * @param operation the operation
     * @param expected the expected
     * @param type the type
     */
    private static void assertComplex(Complex z,
            UnaryOperator<Complex> operation, Complex expected, FunctionType type) {
        // Test the operation
        final Complex c1 = operation.apply(z);
        if (!equals(c1.getReal(), expected.getReal()) ||
            !equals(c1.getImaginary(), expected.getImaginary())) {
            Assertions.fail(String.format("Operation failed (z=%s). Expected: %s but was: %s",
                                          z, expected, c1));
        }

        if (!Double.isNaN(z.getImaginary())) {
            assertConjugateEquality(z, operation);
        }

        if (type != FunctionType.NONE) {
            assertFunctionType(z, operation, type);
        }
    }

    /**
     * Returns {@code true} if the values are equal according to semantics of
     * {@link Double#equals(Object)}.
     *
     * @param x Value
     * @param y Value
     * @return {@code Double.valueof(x).equals(Double.valueOf(y))}
     */
    private static boolean equals(double x, double y) {
        return Double.doubleToLongBits(x) == Double.doubleToLongBits(y);
    }

    /**
     * Utility to create a Complex.
     *
     * @param real the real
     * @param imaginary the imaginary
     * @return the complex
     */
    private static Complex complex(double real, double imaginary) {
        return Complex.ofCartesian(real, imaginary);
    }

    /**
     * Creates a list of Complex infinites.
     *
     * @return the list
     */
    private static ArrayList<Complex> createInfinites() {
        final double[] values = {0, 1, inf, negInf, nan};
        return createCombinations(values, Complex::isInfinite);
    }

    /**
     * Creates a list of Complex finites that are not zero.
     *
     * @return the list
     */
    private static ArrayList<Complex> createNonZeroFinites() {
        final double[] values = {-1, -0, 0, 1, Double.MAX_VALUE};
        return createCombinations(values, c -> !CStandardTest.isZero(c));
    }

    /**
     * Creates a list of Complex finites that are zero: [0,0], [-0,0], [0,-0], [-0,-0].
     *
     * @return the list
     */
    private static ArrayList<Complex> createZeroFinites() {
        final double[] values = {-0, 0};
        return createCombinations(values, c -> true);
    }

    /**
     * Creates a list of Complex NaNs.
     *
     * @return the list
     */
    private static ArrayList<Complex> createNaNs() {
        final double[] values = {0, 1, nan};
        return createCombinations(values, Complex::isNaN);
    }

    /**
     * Creates a list of Complex numbers as an all-vs-all combinations that pass the
     * condition.
     *
     * @param values the values
     * @param condition the condition
     * @return the list
     */
    private static ArrayList<Complex> createCombinations(final double[] values, Predicate<Complex> condition) {
        final ArrayList<Complex> list = new ArrayList<>();
        for (final double re : values) {
            for (final double im : values) {
                final Complex z = complex(re, im);
                if (condition.test(z)) {
                    list.add(z);
                }
            }
        }
        return list;
    }

    /**
     * Checks if the complex is zero.
     *
     * @param c the complex
     * @return true if zero
     */
    private static boolean isZero(Complex c) {
        return Complex.equals(c, Complex.ZERO, 0);
    }

    /**
     * ISO C Standard G.5 (4).
     */
    @Test
    public void testMultiply() {
        final ArrayList<Complex> infinites = createInfinites();
        final ArrayList<Complex> nonZeroFinites = createNonZeroFinites();
        final ArrayList<Complex> zeroFinites = createZeroFinites();

        // C.99 refers to non-zero finites.
        // Standard multiplication of zero with infinites is not defined.
        Assertions.assertEquals(nan, 0.0 * inf, "0 * inf");
        Assertions.assertEquals(nan, 0.0 * negInf, "0 * -inf");
        Assertions.assertEquals(nan, -0.0 * inf, "-0 * inf");
        Assertions.assertEquals(nan, -0.0 * negInf, "-0 * -inf");

        // "if one operand is an infinity and the other operand is a nonzero finite number or an
        // infinity, then the result of the * operator is an infinity;"
        for (final Complex z : infinites) {
            for (final Complex w : infinites) {
                assertOperation(z, w, Complex::multiply, "*", Complex::isInfinite, "Inf");
                assertOperation(w, z, Complex::multiply, "*", Complex::isInfinite, "Inf");
            }
            for (final Complex w : nonZeroFinites) {
                assertOperation(z, w, Complex::multiply, "*", Complex::isInfinite, "Inf");
                assertOperation(w, z, Complex::multiply, "*", Complex::isInfinite, "Inf");
            }
            // C.99 refers to non-zero finites.
            // Infer that Complex multiplication of zero with infinites is not defined.
            for (final Complex w : zeroFinites) {
                assertOperation(z, w, Complex::multiply, "*", Complex::isNaN, "NaN");
                assertOperation(w, z, Complex::multiply, "*", Complex::isNaN, "NaN");
            }
        }

        // ISO C Standard in Annex G is missing an explicit definition of how to handle NaNs.
        // We will assume multiplication by (nan,nan) is not allowed.
        // It is undefined how to multiply when a complex has only one NaN component.
        // The reference implementation allows it.

        // The GNU g++ compiler computes:
        // (1e300 + i 1e300) * (1e30 + i NAN) = inf + i inf
        // Thus this is allowing some computations with NaN.

        // Check multiply with (NaN,NaN) is not corrected
        final double[] values = {0, 1, inf, negInf, nan};
        for (final Complex z : createCombinations(values, c -> true)) {
            assertOperation(z, NAN, Complex::multiply, "*", Complex::isNaN, "NaN");
            assertOperation(NAN, z, Complex::multiply, "*", Complex::isNaN, "NaN");
        }

        // Test multiply cases which result in overflow are corrected to infinity
        assertOperation(maxMax, maxMax, Complex::multiply, "*", Complex::isInfinite, "Inf");
        assertOperation(maxNan, maxNan, Complex::multiply, "*", Complex::isInfinite, "Inf");
        assertOperation(nanMax, maxNan, Complex::multiply, "*", Complex::isInfinite, "Inf");
        assertOperation(maxNan, nanMax, Complex::multiply, "*", Complex::isInfinite, "Inf");
        assertOperation(nanMax, nanMax, Complex::multiply, "*", Complex::isInfinite, "Inf");
    }

    /**
     * ISO C Standard G.5 (4).
     */
    @Test
    public void testDivide() {
        final ArrayList<Complex> infinites = createInfinites();
        final ArrayList<Complex> nonZeroFinites = createNonZeroFinites();
        final ArrayList<Complex> zeroFinites = createZeroFinites();
        final ArrayList<Complex> nans = createNaNs();
        final ArrayList<Complex> finites = new ArrayList<>(nonZeroFinites);
        finites.addAll(zeroFinites);

        // "if the first operand is an infinity and the second operand is a finite number, then the
        // result of the / operator is an infinity;"
        for (final Complex z : infinites) {
            for (final Complex w : nonZeroFinites) {
                assertOperation(z, w, Complex::divide, "/", Complex::isInfinite, "Inf");
            }
            for (final Complex w : zeroFinites) {
                assertOperation(z, w, Complex::divide, "/", Complex::isInfinite, "Inf");
            }
            // Check inf/inf cannot be done
            for (final Complex w : infinites) {
                assertOperation(z, w, Complex::divide, "/", Complex::isNaN, "NaN");
            }
        }

        // "if the first operand is a finite number and the second operand is an infinity, then the
        // result of the / operator is a zero;"
        for (final Complex z : finites) {
            for (final Complex w : infinites) {
                assertOperation(z, w, Complex::divide, "/", CStandardTest::isZero, "Zero");
            }
        }

        // "if the first operand is a nonzero finite number or an infinity and the second operand is
        // a zero, then the result of the / operator is an infinity."
        for (final Complex w : zeroFinites) {
            for (final Complex z : nonZeroFinites) {
                assertOperation(z, w, Complex::divide, "/", Complex::isInfinite, "Inf");
            }
            for (final Complex z : infinites) {
                assertOperation(z, w, Complex::divide, "/", Complex::isInfinite, "Inf");
            }
        }

        // ISO C Standard in Annex G is missing an explicit definition of how to handle NaNs.
        // The reference implementation does not correct for divide by NaN components unless
        // infinite.
        for (final Complex w : nans) {
            for (final Complex z : finites) {
                assertOperation(z, w, Complex::divide, "/", c -> NAN.equals(c), "(NaN,NaN)");
            }
            for (final Complex z : infinites) {
                assertOperation(z, w, Complex::divide, "/", c -> NAN.equals(c), "(NaN,NaN)");
            }
        }

        // Check (NaN,NaN) divide is not corrected for the edge case of divide by zero or infinite
        for (final Complex w : zeroFinites) {
            assertOperation(NAN, w, Complex::divide, "/", Complex::isNaN, "NaN");
        }
        for (final Complex w : infinites) {
            assertOperation(NAN, w, Complex::divide, "/", Complex::isNaN, "NaN");
        }
    }

    /**
     * ISO C Standard G.6 (3).
     */
    @Test
    public void testSqrt1() {
        assertComplex(complex(-2.0, 0.0).sqrt(), complex(0.0, Math.sqrt(2)));
        assertComplex(complex(-2.0, -0.0).sqrt(), complex(0.0, -Math.sqrt(2)));
    }

    /**
     * ISO C Standard G.6 (7).
     */
    @Test
    public void testImplicitTrig() {
        final UniformRandomProvider rng = RandomSource.create(RandomSource.SPLIT_MIX_64);
        for (int i = 0; i < 100; i++) {
            final double re = next(rng);
            final double im = next(rng);
            final Complex z = complex(re, im);
            final Complex iz = Complex.I.multiply(z);
            assertComplex(z.asin(), negI.multiply(iz.asinh()));
            assertComplex(z.atan(), negI.multiply(iz.atanh()));
            assertComplex(z.cos(), iz.cosh());
            assertComplex(z.sin(), negI.multiply(iz.sinh()));
            assertComplex(z.tan(), negI.multiply(iz.tanh()));
        }
    }

    /**
     * Create a number in the range {@code (-5,5)}.
     *
     * @param rng the random generator
     * @return the number
     */
    private static double next(UniformRandomProvider rng) {
        return rng.nextDouble() * (rng.nextBoolean() ? -5 : 5);
    }

    /**
     * ISO C Standard G.6 (6) for abs().
     * Defined by ISO C Standard F.9.4.3 hypot function.
     */
    @Test
    public void testAbs() {
        Assertions.assertEquals(inf, complex(inf, nan).abs());
        Assertions.assertEquals(inf, complex(negInf, nan).abs());
        final UniformRandomProvider rng = RandomSource.create(RandomSource.SPLIT_MIX_64);
        for (int i = 0; i < 100; i++) {
            final double x = next(rng);
            final double y = next(rng);
            Assertions.assertEquals(complex(x, y).abs(), complex(y, x).abs());
            Assertions.assertEquals(complex(x, y).abs(), complex(x, -y).abs());
            Assertions.assertEquals(Math.abs(x), complex(x, 0.0).abs());
            Assertions.assertEquals(Math.abs(x), complex(x, -0.0).abs());
            Assertions.assertEquals(inf, complex(inf, y).abs());
            Assertions.assertEquals(inf, complex(negInf, y).abs());
        }
    }

    /**
     * ISO C Standard G.6.1.1.
     */
    @Test
    public void testAcos() {
        assertConjugateEquality(Complex::acos);
        assertComplex(Complex.ZERO, Complex::acos, piTwoNegZero);
        assertComplex(negZeroZero, Complex::acos, piTwoNegZero);
        assertComplex(zeroNaN, Complex::acos, piTwoNaN);
        assertComplex(oneNaN, Complex::acos, NAN);
        assertComplex(oneInf, Complex::acos, piTwoNegInf);
        assertComplex(negInfZero, Complex::acos, piNegInf);
        assertComplex(negInfOne, Complex::acos, piNegInf);
        assertComplex(infZero, Complex::acos, zeroNegInf);
        assertComplex(infOne, Complex::acos, zeroNegInf);
        assertComplex(negInfInf, Complex::acos, threePiFourNegInf);
        assertComplex(infInf, Complex::acos, piFourNegInf);
        assertComplex(infNaN, Complex::acos, nanInf);
        assertComplex(negInfNaN, Complex::acos, nanNegInf); // Unspecified imaginary sign
        assertComplex(nanOne, Complex::acos, NAN);
        assertComplex(nanInf, Complex::acos, nanNegInf);
        assertComplex(NAN, Complex::acos, NAN);
    }

    /**
     * ISO C Standard G.6.2.1.
     */
    @Test
    public void testAcosh() {
        assertConjugateEquality(Complex::acosh);
        assertComplex(Complex.ZERO, Complex::acosh, zeroPiTwo);
        assertComplex(negZeroZero, Complex::acosh, negZeroPiTwo);
        assertComplex(zeroNaN, Complex::acosh, NAN);
        assertComplex(oneNaN, Complex::acosh, NAN);
        assertComplex(oneInf, Complex::acosh, infPiTwo);
        assertComplex(negInfZero, Complex::acosh, infPi);
        assertComplex(negInfOne, Complex::acosh, infPi);
        assertComplex(infZero, Complex::acosh, infZero);
        assertComplex(infOne, Complex::acosh, infZero);
        assertComplex(negInfInf, Complex::acosh, infThreePiFour);
        assertComplex(infInf, Complex::acosh, infPiFour);
        assertComplex(infNaN, Complex::acosh, infNaN);
        assertComplex(negInfNaN, Complex::acosh, infNaN);
        assertComplex(nanOne, Complex::acosh, NAN);
        assertComplex(nanInf, Complex::acosh, infNaN);
        assertComplex(NAN, Complex::acosh, NAN);
    }

    /**
     * ISO C Standard G.6.2.2.
     */
    @Test
    public void testAsinh() {
        assertConjugateEquality(Complex::asinh);
        assertFunctionType(Complex::asinh, FunctionType.ODD);
        assertComplex(Complex.ZERO, Complex::asinh, Complex.ZERO, FunctionType.ODD);
        assertComplex(negZeroZero, Complex::asinh, negZeroZero, FunctionType.ODD);
        assertComplex(zeroNaN, Complex::asinh, NAN, FunctionType.ODD);
        assertComplex(oneNaN, Complex::asinh, NAN, FunctionType.ODD);
        assertComplex(oneInf, Complex::asinh, infPiTwo, FunctionType.ODD);
        assertComplex(negInfZero, Complex::asinh, negInfZero, FunctionType.ODD);
        assertComplex(negInfOne, Complex::asinh, negInfZero, FunctionType.ODD);
        assertComplex(infZero, Complex::asinh, infZero, FunctionType.ODD);
        assertComplex(infOne, Complex::asinh, infZero, FunctionType.ODD);
        assertComplex(negInfInf, Complex::asinh, negInfPiFour, FunctionType.ODD);
        assertComplex(infInf, Complex::asinh, infPiFour, FunctionType.ODD);
        assertComplex(infNaN, Complex::asinh, infNaN, FunctionType.ODD);
        assertComplex(negInfNaN, Complex::asinh, negInfNaN, FunctionType.ODD);
        assertComplex(nanZero, Complex::asinh, nanZero, FunctionType.ODD);
        assertComplex(nanOne, Complex::asinh, NAN, FunctionType.ODD);
        assertComplex(nanInf, Complex::asinh, infNaN); // Unspecified real sign
        assertComplex(NAN, Complex::asinh, NAN, FunctionType.ODD);
    }

    /**
     * ISO C Standard G.6.2.3.
     */
    @Test
    public void testAtanh() {
        assertConjugateEquality(Complex::atanh);
        assertFunctionType(Complex::atanh, FunctionType.ODD);
        assertComplex(Complex.ZERO, Complex::atanh, Complex.ZERO, FunctionType.ODD);
        assertComplex(negZeroZero, Complex::atanh, negZeroZero, FunctionType.ODD);
        assertComplex(zeroNaN, Complex::atanh, zeroNaN, FunctionType.ODD);
        assertComplex(oneNaN, Complex::atanh, NAN, FunctionType.ODD);
        assertComplex(oneZero, Complex::atanh, infZero, FunctionType.ODD);
        assertComplex(oneInf, Complex::atanh, zeroPiTwo, FunctionType.ODD);
        assertComplex(negInfZero, Complex::atanh, negZeroPiTwo, FunctionType.ODD);
        assertComplex(negInfOne, Complex::atanh, negZeroPiTwo, FunctionType.ODD);
        assertComplex(infZero, Complex::atanh, zeroPiTwo, FunctionType.ODD);
        assertComplex(infOne, Complex::atanh, zeroPiTwo, FunctionType.ODD);
        assertComplex(negInfInf, Complex::atanh, negZeroPiTwo, FunctionType.ODD);
        assertComplex(infInf, Complex::atanh, zeroPiTwo, FunctionType.ODD);
        assertComplex(infNaN, Complex::atanh, zeroNaN, FunctionType.ODD);
        assertComplex(negInfNaN, Complex::atanh, negZeroNaN, FunctionType.ODD);
        assertComplex(nanZero, Complex::atanh, NAN, FunctionType.ODD);
        assertComplex(nanOne, Complex::atanh, NAN, FunctionType.ODD);
        assertComplex(nanInf, Complex::atanh, zeroPiTwo); // Unspecified real sign
        assertComplex(NAN, Complex::atanh, NAN, FunctionType.ODD);
    }

    /**
     * ISO C Standard G.6.2.4.
     */
    @Test
    public void testCosh() {
        assertConjugateEquality(Complex::cosh);
        assertFunctionType(Complex::cosh, FunctionType.EVEN);
        assertComplex(Complex.ZERO, Complex::cosh, Complex.ONE);
        assertComplex(negZeroZero, Complex::cosh, oneNegZero, FunctionType.EVEN);
        assertComplex(zeroInf, Complex::cosh, nanZero); // Unspecified imaginary sign
        assertComplex(oneInf, Complex::cosh, NAN, FunctionType.EVEN);
        assertComplex(zeroNaN, Complex::cosh, nanZero); // Unspecified imaginary sign
        assertComplex(oneNaN, Complex::cosh, NAN, FunctionType.EVEN);
        // (inf + iy) = inf * cis(y)
        // where cis(y) = cos(y) + i sin(y), and y is finite non-zero
        //
        // Note that y == 0: complex(1, 0).multiply(inf) = (inf, NaN)
        // But the cosh is (inf, 0). This result is computed by g++ and we test it separately.
        assertComplex(negInfNegZero, Complex::cosh, infZero, FunctionType.EVEN);
        assertComplex(negInfZero, Complex::cosh, infNegZero, FunctionType.EVEN);
        assertComplex(infNegZero, Complex::cosh, infNegZero, FunctionType.EVEN);
        assertComplex(infZero, Complex::cosh, infZero, FunctionType.EVEN);
        for (int i = 1; i < 10; i++) {
            final double y = i * 0.5;
            assertComplex(complex(inf, y), Complex::cosh, Complex.ofCis(y).multiply(inf), FunctionType.EVEN);
            assertComplex(complex(-inf, -y), Complex::cosh, Complex.ofCis(y).multiply(inf), FunctionType.EVEN);
            assertComplex(complex(inf, -y), Complex::cosh, Complex.ofCis(-y).multiply(inf), FunctionType.EVEN);
            assertComplex(complex(-inf, y), Complex::cosh, Complex.ofCis(-y).multiply(inf), FunctionType.EVEN);
        }
        assertComplex(negInfInf, Complex::cosh, infNaN); // Unspecified real sign
        assertComplex(infInf, Complex::cosh, infNaN); // Unspecified real sign
        assertComplex(infNaN, Complex::cosh, infNaN, FunctionType.EVEN);
        assertComplex(negInfNaN, Complex::cosh, infNaN, FunctionType.EVEN);
        assertComplex(nanZero, Complex::cosh, nanZero); // Unspecified imaginary sign
        assertComplex(nanOne, Complex::cosh, NAN, FunctionType.EVEN);
        assertComplex(nanInf, Complex::cosh, NAN, FunctionType.EVEN);
        assertComplex(NAN, Complex::cosh, NAN, FunctionType.EVEN);
    }

    /**
     * ISO C Standard G.6.2.5.
     */
    @Test
    public void testSinh() {
        assertConjugateEquality(Complex::sinh);
        assertFunctionType(Complex::sinh, FunctionType.ODD);
        assertComplex(Complex.ZERO, Complex::sinh, Complex.ZERO);
        assertComplex(negZeroZero, Complex::sinh, negZeroZero, FunctionType.ODD);
        assertComplex(zeroInf, Complex::sinh, zeroNaN); // Unspecified real sign
        assertComplex(oneInf, Complex::sinh, NAN, FunctionType.ODD);
        assertComplex(zeroNaN, Complex::sinh, zeroNaN); // Unspecified real sign
        assertComplex(oneNaN, Complex::sinh, NAN, FunctionType.ODD);
        // (inf + iy) = inf * cis(y)
        // where cis(y) = cos(y) + i sin(y), and y is finite non-zero
        //
        // Note that y == 0: complex(1, 0).multiply(inf) = (inf, NaN)
        // But the sinh is (inf, 0). This result is computed by g++ and we test it separately.
        assertComplex(negInfNegZero, Complex::sinh, negInfNegZero, FunctionType.ODD);
        assertComplex(negInfZero, Complex::sinh, negInfZero, FunctionType.ODD);
        assertComplex(infNegZero, Complex::sinh, infNegZero, FunctionType.ODD);
        assertComplex(infZero, Complex::sinh, infZero, FunctionType.ODD);
        for (int i = 1; i < 10; i++) {
            final double y = i * 0.5;
            assertComplex(complex(inf, y), Complex::sinh, Complex.ofCis(y).multiply(inf), FunctionType.ODD);
            assertComplex(complex(inf, -y), Complex::sinh, Complex.ofCis(-y).multiply(inf), FunctionType.ODD);
            assertComplex(complex(-inf, y), Complex::sinh, Complex.ofCis(y).multiply(-inf), FunctionType.ODD);
            assertComplex(complex(-inf, -y), Complex::sinh, Complex.ofCis(-y).multiply(-inf), FunctionType.ODD);
        }
        assertComplex(negInfInf, Complex::sinh, infNaN); // Unspecified real sign
        assertComplex(infInf, Complex::sinh, infNaN); // Unspecified real sign
        assertComplex(infNaN, Complex::sinh, infNaN); // Unspecified real sign
        assertComplex(negInfNaN, Complex::sinh, infNaN); // Unspecified real sign
        assertComplex(nanZero, Complex::sinh, nanZero, FunctionType.ODD);
        assertComplex(nanOne, Complex::sinh, NAN, FunctionType.ODD);
        assertComplex(nanInf, Complex::sinh, NAN, FunctionType.ODD);
        assertComplex(NAN, Complex::sinh, NAN, FunctionType.ODD);
    }

    /**
     * ISO C Standard G.6.2.6.
     */
    @Test
    public void testTanh() {
        assertConjugateEquality(Complex::tanh);
        assertFunctionType(Complex::tanh, FunctionType.ODD);
        assertComplex(Complex.ZERO, Complex::tanh, Complex.ZERO, FunctionType.ODD);
        assertComplex(negZeroZero, Complex::tanh, negZeroZero, FunctionType.ODD);
        assertComplex(zeroInf, Complex::tanh, NAN, FunctionType.ODD);
        assertComplex(oneInf, Complex::tanh, NAN, FunctionType.ODD);
        assertComplex(zeroNaN, Complex::tanh, NAN, FunctionType.ODD);
        assertComplex(oneNaN, Complex::tanh, NAN, FunctionType.ODD);
        // (inf + iy) = 1 + i0 sin(2y), and y is positive-signed finite
        // Note: no specification for other -inf and/or negative y.
        // g++ returns the result using (+/-1, i0 sin(2y)) where the sign of the 1 is from the inf.
        assertComplex(negInfNegZero, Complex::tanh, negOneNegZero, FunctionType.ODD);
        assertComplex(negInfZero, Complex::tanh, negOneZero, FunctionType.ODD);
        assertComplex(infNegZero, Complex::tanh, oneNegZero, FunctionType.ODD);
        assertComplex(infZero, Complex::tanh, oneZero, FunctionType.ODD);
        for (int i = 1; i < 10; i++) {
            final double y = i * 0.5;
            assertComplex(complex(inf, y), Complex::tanh, complex(1.0, Math.copySign(0, Math.sin(2 * y))), FunctionType.ODD);
            assertComplex(complex(inf, -y), Complex::tanh, complex(1.0, Math.copySign(0, Math.sin(2 * -y))), FunctionType.ODD);
            assertComplex(complex(-inf, y), Complex::tanh, complex(-1.0, Math.copySign(0, Math.sin(2 * y))), FunctionType.ODD);
            assertComplex(complex(-inf, -y), Complex::tanh, complex(-1.0, Math.copySign(0, Math.sin(2 * -y))), FunctionType.ODD);
        }
        assertComplex(negInfInf, Complex::tanh, negOneZero); // Unspecified imaginary sign
        assertComplex(infInf, Complex::tanh, oneZero); // Unspecified imaginary sign
        assertComplex(infNaN, Complex::tanh, oneZero, FunctionType.ODD);
        assertComplex(negInfNaN, Complex::tanh, negOneZero, FunctionType.ODD);
        assertComplex(nanZero, Complex::tanh, nanZero, FunctionType.ODD);
        assertComplex(nanOne, Complex::tanh, NAN, FunctionType.ODD);
        assertComplex(nanInf, Complex::tanh, NAN, FunctionType.ODD);
        assertComplex(NAN, Complex::tanh, NAN, FunctionType.ODD);
    }

    /**
     * ISO C Standard G.6.3.1.
     */
    @Test
    public void testExp() {
        assertConjugateEquality(Complex::exp);
        assertComplex(Complex.ZERO, Complex::exp, oneZero);
        assertComplex(negZeroZero, Complex::exp, oneZero);
        assertComplex(zeroInf, Complex::exp, NAN);
        assertComplex(oneInf, Complex::exp, NAN);
        assertComplex(zeroNaN, Complex::exp, NAN);
        assertComplex(oneNaN, Complex::exp, NAN);
        assertComplex(infNegZero, Complex::exp, infNegZero);
        assertComplex(infZero, Complex::exp, infZero);
        // (-inf + iy) = +0 cis(y)
        // where cis(y) = cos(y) + i sin(y), and y is finite
        for (int i = 0; i < 10; i++) {
            final double y = i * 0.5;
            assertComplex(complex(-inf, y), Complex::exp, Complex.ofCis(y).multiply(0.0));
            assertComplex(complex(-inf, -y), Complex::exp, Complex.ofCis(-y).multiply(0.0));
        }
        // (inf + iy) = +inf cis(y)
        // where cis(y) = cos(y) + i sin(y), and y is non-zero finite
        for (int i = 1; i < 10; i++) {
            final double y = i * 0.5;
            assertComplex(complex(inf, y), Complex::exp, Complex.ofCis(y).multiply(inf));
            assertComplex(complex(inf, -y), Complex::exp, Complex.ofCis(-y).multiply(inf));
        }
        assertComplex(negInfInf, Complex::exp, Complex.ZERO); // Unspecified real/imaginary sign
        assertComplex(infInf, Complex::exp, infNaN);
        assertComplex(negInfNaN, Complex::exp, Complex.ZERO); // Unspecified real/imaginary sign
        assertComplex(infNaN, Complex::exp, infNaN); // Unspecified real/imaginary sign
        assertComplex(nanZero, Complex::exp, nanZero);
        assertComplex(nanOne, Complex::exp, NAN);
        assertComplex(nanInf, Complex::exp, NAN);
        assertComplex(NAN, Complex::exp, NAN);
    }

    /**
     * ISO C Standard G.6.3.2.
     */
    @Test
    public void testLog() {
        assertConjugateEquality(Complex::log);
        assertComplex(negZeroZero, Complex::log, negInfPi);
        assertComplex(Complex.ZERO, Complex::log, negInfZero);
        assertComplex(zeroInf, Complex::log, infPiTwo);
        assertComplex(oneInf, Complex::log, infPiTwo);
        assertComplex(zeroNaN, Complex::log, NAN);
        assertComplex(oneNaN, Complex::log, NAN);
        assertComplex(negInfZero, Complex::log, infPi);
        assertComplex(negInfOne, Complex::log, infPi);
        assertComplex(infZero, Complex::log, infZero);
        assertComplex(infOne, Complex::log, infZero);
        assertComplex(negInfInf, Complex::log, infThreePiFour);
        assertComplex(infInf, Complex::log, infPiFour);
        assertComplex(negInfNaN, Complex::log, infNaN);
        assertComplex(infNaN, Complex::log, infNaN);
        assertComplex(nanZero, Complex::log, NAN);
        assertComplex(nanOne, Complex::log, NAN);
        assertComplex(nanInf, Complex::log, infNaN);
        assertComplex(NAN, Complex::log, NAN);
    }

    /**
     * Same edge cases as log() since the real component is divided by Math.log(10) which
     * has no effect on infinite or nan.
     */
    @Test
    public void testLog10() {
        assertConjugateEquality(Complex::log10);
        assertComplex(negZeroZero, Complex::log10, negInfPi);
        assertComplex(Complex.ZERO, Complex::log10, negInfZero);
        assertComplex(zeroInf, Complex::log10, infPiTwo);
        assertComplex(oneInf, Complex::log10, infPiTwo);
        assertComplex(zeroNaN, Complex::log10, NAN);
        assertComplex(oneNaN, Complex::log10, NAN);
        assertComplex(negInfZero, Complex::log10, infPi);
        assertComplex(negInfOne, Complex::log10, infPi);
        assertComplex(infZero, Complex::log10, infZero);
        assertComplex(infOne, Complex::log10, infZero);
        assertComplex(negInfInf, Complex::log10, infThreePiFour);
        assertComplex(infInf, Complex::log10, infPiFour);
        assertComplex(negInfNaN, Complex::log10, infNaN);
        assertComplex(infNaN, Complex::log10, infNaN);
        assertComplex(nanZero, Complex::log10, NAN);
        assertComplex(nanOne, Complex::log10, NAN);
        assertComplex(nanInf, Complex::log10, infNaN);
        assertComplex(NAN, Complex::log10, NAN);
    }

    /**
     * ISO C Standard G.6.4.2.
     */
    @Test
    public void testSqrt2() {
        assertConjugateEquality(Complex::sqrt);
        assertComplex(negZeroZero, Complex::sqrt, Complex.ZERO);
        assertComplex(Complex.ZERO, Complex::sqrt, Complex.ZERO);
        assertComplex(zeroNegZero, Complex::sqrt, zeroNegZero);
        assertComplex(negZeroNegZero, Complex::sqrt, zeroNegZero);
        assertComplex(zeroInf, Complex::sqrt, infInf);
        assertComplex(oneInf, Complex::sqrt, infInf);
        assertComplex(infInf, Complex::sqrt, infInf);
        assertComplex(nanInf, Complex::sqrt, infInf);
        assertComplex(zeroNaN, Complex::sqrt, NAN);
        assertComplex(oneNaN, Complex::sqrt, NAN);
        assertComplex(negInfZero, Complex::sqrt, zeroInf);
        assertComplex(negInfOne, Complex::sqrt, zeroInf);
        assertComplex(infZero, Complex::sqrt, infZero);
        assertComplex(infOne, Complex::sqrt, infZero);
        assertComplex(negInfNaN, Complex::sqrt, nanInf); // Unspecified imaginary sign
        assertComplex(infNaN, Complex::sqrt, infNaN);
        assertComplex(nanZero, Complex::sqrt, NAN);
        assertComplex(nanOne, Complex::sqrt, NAN);
        assertComplex(NAN, Complex::sqrt, NAN);
    }
}
