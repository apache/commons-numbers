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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.UnaryOperator;

/**
 * Tests the standards defined by the C.99 standard for complex numbers
 * defined in ISO/IEC 9899, Annex G.
 *
 * @see <a href="http://www.open-std.org/JTC1/SC22/WG14/www/standards">
 *    ISO/IEC 9899 - Programming languages - C</a>
 */
class CStandardTest {

    private static final double inf = Double.POSITIVE_INFINITY;
    private static final double negInf = Double.NEGATIVE_INFINITY;
    private static final double nan = Double.NaN;
    private static final double max = Double.MAX_VALUE;
    private static final double piOverFour = Math.PI / 4.0;
    private static final double piOverTwo = Math.PI / 2.0;
    private static final double threePiOverFour = 3.0 * Math.PI / 4.0;
    private static final Complex oneZero = complex(1, 0);
    private static final Complex zeroInf = complex(0, inf);
    private static final Complex zeroNegInf = complex(0, negInf);
    private static final Complex zeroNaN = complex(0, nan);
    private static final Complex zeroPiTwo = complex(0.0, piOverTwo);
    private static final Complex negZeroZero = complex(-0.0, 0);
    private static final Complex negZeroNaN = complex(-0.0, nan);
    private static final Complex infZero = complex(inf, 0);
    private static final Complex infNaN = complex(inf, nan);
    private static final Complex infInf = complex(inf, inf);
    private static final Complex infPiTwo = complex(inf, piOverTwo);
    private static final Complex infThreePiFour = complex(inf, threePiOverFour);
    private static final Complex infPiFour = complex(inf, piOverFour);
    private static final Complex infPi = complex(inf, Math.PI);
    private static final Complex negInfInf = complex(negInf, inf);
    private static final Complex negInfZero = complex(negInf, 0);
    private static final Complex negInfNaN = complex(negInf, nan);
    private static final Complex negInfPi = complex(negInf, Math.PI);
    private static final Complex nanInf = complex(nan, inf);
    private static final Complex nanNegInf = complex(nan, negInf);
    private static final Complex nanZero = complex(nan, 0);
    private static final Complex nanPiTwo = complex(nan, piOverTwo);
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

    /** Finite numbers (positive and negative with zero). */
    private static final double[] finite;
    /** Positive finite numbers (with zero). */
    private static final double[] positiveFinite;
    /** Non-zero finite numbers (positive and negative). */
    private static final double[] nonZeroFinite;
    /** Non-zero positive finite numbers. */
    private static final double[] nonZeroPositiveFinite;
    /** Non-zero finite and infinite numbers (positive and negative). */
    private static final double[] nonZero;

    static {
        // Choose a range that covers 2 * Math.PI.
        // This is important for the functions that use cis(y).
        final int size = 13;
        finite = new double[size * 2];
        positiveFinite = new double[size];
        for (int i = 0; i < size; i++) {
            final double v = i * 0.5;
            finite[i * 2] = -v;
            finite[i * 2 + 1] = v;
            positiveFinite[i] = v;
        }

        // Copy without zero
        nonZeroFinite = Arrays.copyOfRange(finite, 2, finite.length);
        nonZeroPositiveFinite = Arrays.copyOfRange(positiveFinite, 1, positiveFinite.length);

        nonZero = Arrays.copyOf(nonZeroFinite, nonZeroFinite.length + 2);
        nonZero[nonZeroFinite.length] = Double.POSITIVE_INFINITY;
        nonZero[nonZeroFinite.length + 1] = Double.NEGATIVE_INFINITY;

        // Arrange numerically
        Arrays.sort(finite);
        Arrays.sort(nonZeroFinite);
        Arrays.sort(nonZero);
    }

    /**
     * The function type (e.g. odd or even).
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
     * An enum containing functionality to remove the sign from complex parts.
     */
    private enum UnspecifiedSign {
        /** Remove the sign from the real component. */
        REAL {
            @Override
            Complex removeSign(Complex c) {
                return negative(c.getReal()) ? complex(-c.getReal(), c.getImaginary()) : c;
            }
        },
        /** Remove the sign from the imaginary component. */
        IMAGINARY {
            @Override
            Complex removeSign(Complex c) {
                return negative(c.getImaginary()) ? complex(c.getReal(), -c.getImaginary()) : c;
            }
        },
        /** Remove the sign from the real and imaginary component. */
        REAL_IMAGINARY {
            @Override
            Complex removeSign(Complex c) {
                return IMAGINARY.removeSign(REAL.removeSign(c));
            }
        },
        /** Do not remove the sign. */
        NONE {
            @Override
            Complex removeSign(Complex c) {
                return c;
            }
        };

        /**
         * Removes the sign from the complex real and or imaginary parts.
         *
         * @param c the complex
         * @return the complex
         */
        abstract Complex removeSign(Complex c);

        /**
         * Check that a value is negative. It must meet all the following conditions:
         * <ul>
         *  <li>it is not {@code NaN},</li>
         *  <li>it is negative signed,</li>
         * </ul>
         *
         * <p>Note: This is true for negative zero.</p>
         *
         * @param d Value.
         * @return {@code true} if {@code d} is negative.
         */
        private static boolean negative(double d) {
            return d < 0 || Double.doubleToLongBits(d) == Double.doubleToLongBits(-0.0);
        }
    }

    /**
     * Assert the two complex numbers have equivalent real and imaginary components as
     * defined by the {@code ==} operator.
     *
     * @param c1 the first complex (actual)
     * @param c2 the second complex (expected)
     */
    private static void assertComplex(ComplexDouble c1, ComplexDouble c2) {
        // Use a delta of zero to allow comparison of -0.0 to 0.0
        Assertions.assertEquals(c2.getReal(), c1.getReal(), 0.0, "real");
        Assertions.assertEquals(c2.getImaginary(), c1.getImaginary(), 0.0, "imaginary");
    }

    /**
     * Assert the operation on the two complex numbers.
     *  @param c1 the first complex
     * @param c2 the second complex
     * @param operation1 the Complex operation
     * @param operation2 ComplexFunctions operation
     * @param operationName the operation name
     * @param expected the expected
     * @param expectedName the expected name
     */
    private static void assertOperation(Complex c1, Complex c2,
                                        BiFunction<Complex, Complex, Complex> operation1,
                                        ComplexBinaryOperator<ComplexDouble> operation2,
                                        String operationName,
                                        Predicate<Complex> expected, String expectedName) {
        final Complex z = operation1.apply(c1, c2);
        final ComplexDouble y = operation2.apply(c1, c2, TestUtils.ComplexDoubleConstructor.of());
        Assertions.assertEquals(z.real(), y.getReal());
        Assertions.assertEquals(z.imag(), y.getImaginary());
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
     * <h2>ISO C99 equalities</h2>
     *
     * <p>Note that this method currently enforces the conjugate equalities for some cases
     * where the sign of the real/imaginary parts are unspecified in ISO C99. This is
     * allowed (since they are unspecified). The sign specification is appropriately
     * handled during testing of odd/even functions. There are some functions where it
     * is not possible to satisfy the conjugate equality and also the odd/even rule.
     * The compromise made here is to satisfy only one and the other is allowed to fail
     * only on the sign of the output result. Known functions where this applies are:
     *
     * <ul>
     *  <li>asinh(NaN, inf)
     *  <li>atanh(NaN, inf)
     *  <li>cosh(NaN, 0.0)
     *  <li>sinh(inf, inf)
     *  <li>sinh(inf, nan)
     * </ul>
     *
     * @param operation the operation
     */
    private static void assertConjugateEquality(UnaryOperator<Complex> operation, ComplexUnaryOperator<ComplexDouble> operation2) {
        // Edge cases. Inf/NaN are specifically handled in the C99 test cases
        // but are repeated here to enforce the conjugate equality even when the C99
        // standard does not specify a sign. This may be revised in the future.
        final double[] parts = {Double.NEGATIVE_INFINITY, -1, -0.0, 0.0, 1,
                                Double.POSITIVE_INFINITY, Double.NaN};
        for (final double x : parts) {
            for (final double y : parts) {
                // No conjugate for imaginary NaN
                if (!Double.isNaN(y)) {
                    assertConjugateEquality(complex(x, y), operation, operation2, UnspecifiedSign.NONE);
                }
            }
        }
        // Random numbers
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        for (int i = 0; i < 100; i++) {
            final double x = next(rng);
            final double y = next(rng);
            assertConjugateEquality(complex(x, y), operation, operation2, UnspecifiedSign.NONE);
        }
    }

    /**
     * Assert the operation on the complex number satisfies the conjugate equality.
     *
     * <pre>
     * op(conj(z)) = conj(op(z))
     * </pre>
     *
     * <p>The results must be binary equal; the sign of the complex number is first processed
     * using the provided sign specification.
     *
     * @param z the complex number
     * @param operation the operation
     * @param sign the sign specification
     */
    private static void assertConjugateEquality(Complex z,
            UnaryOperator<Complex> operation, ComplexUnaryOperator<ComplexDouble> operation2,
                                                UnspecifiedSign sign) {
        final Complex c1 = operation.apply(z.conj());
        final Complex c2 = operation.apply(z).conj();
        final ComplexDouble c3 = operation2.apply(z.conj(), TestUtils.ComplexDoubleConstructor.of());
        final Complex t1 = sign.removeSign(c1);
        final Complex t2 = sign.removeSign(c2);

        // Test for binary equality
        if (!equals(t1.getReal(), t2.getReal()) ||
            !equals(t1.getImaginary(), t2.getImaginary())) {
            Assertions.fail(
                String.format("Conjugate equality failed (z=%s). Expected: %s but was: %s (Unspecified sign = %s)",
                              z, c1, c2, c3, sign));
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
                assertFunctionType(complex(x, y), operation, type, UnspecifiedSign.NONE);
            }
        }
        // Random numbers
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        for (int i = 0; i < 100; i++) {
            final double x = next(rng);
            final double y = next(rng);
            assertFunctionType(complex(x, y), operation, type, UnspecifiedSign.NONE);
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
     * <p>The results must be binary equal; the sign of the complex number is first processed
     * using the provided sign specification.
     *
     * @param z the complex number
     * @param operation the operation
     * @param type the type (assumed to be ODD/EVEN)
     * @param sign the sign specification
     */
    private static void assertFunctionType(Complex z,
            UnaryOperator<Complex> operation, FunctionType type, UnspecifiedSign sign) {
        final Complex c1 = operation.apply(z);
        Complex c2 = operation.apply(z.negate());
        if (type == FunctionType.ODD) {
            c2 = c2.negate();
        }
        final Complex t1 = sign.removeSign(c1);
        final Complex t2 = sign.removeSign(c2);

        // Test for binary equality
        if (!equals(t1.getReal(), t2.getReal()) ||
            !equals(t1.getImaginary(), t2.getImaginary())) {
            Assertions.fail(
                String.format("%s equality failed (z=%s, -z=%s). Expected: %s but was: %s (Unspecified sign = %s)",
                              type, z, z.negate(), c1, c2, sign));
            new Exception().printStackTrace();
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
     *  @param z the complex
     * @param operation the operation
     * @param expected the expected
     * @param operation2
     */
    private static void assertComplex(Complex z,
                                      UnaryOperator<Complex> operation, Complex expected,
                                      ComplexUnaryOperator<ComplexDouble> operation2) {
        assertComplex(z, operation, operation2, expected, FunctionType.NONE, UnspecifiedSign.NONE);
    }

    /**
     * Assert the operation on the complex number is equal to the expected value. If the
     * imaginary part is not NaN the operation must also satisfy the conjugate equality.
     *
     * <pre>
     * op(conj(z)) = conj(op(z))
     * </pre>
     *
     * <p>The results must be binary equal. This includes the sign of zero.
     *  @param z the complex
     * @param operation the operation
     * @param operation2
     * @param expected the expected
     * @param sign the sign specification
     */
    private static void assertComplex(Complex z,
                                      UnaryOperator<Complex> operation,
                                      ComplexUnaryOperator<ComplexDouble> operation2,
                                      Complex expected, UnspecifiedSign sign) {
        assertComplex(z, operation, operation2, expected, FunctionType.NONE, sign);
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
     *  @param z the complex
     * @param operation the operation
     * @param operation2
     * @param expected the expected
     * @param type the type
     */
    private static void assertComplex(Complex z,
                                      UnaryOperator<Complex> operation,
                                      ComplexUnaryOperator<ComplexDouble> operation2,
                                      Complex expected,
                                      FunctionType type) {
        assertComplex(z, operation, operation2, expected, type, UnspecifiedSign.NONE);
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
     * <p>An ODD/EVEN function is also tested that the conjugate equalities hold with {@code -z}.
     * This effectively enumerates testing: (re, im); (re, -im); (-re, -im); and (-re, im).
     *
     * <p>The results must be binary equal; the sign of the complex number is first processed
     * using the provided sign specification.
     *
     * @param z the complex
     * @param operation the operation
     * @param expected the expected
     * @param type the type
     * @param sign the sign specification
     */
    private static void assertComplex(Complex z,
            UnaryOperator<Complex> operation, ComplexUnaryOperator<ComplexDouble> operation2, Complex expected,
            FunctionType type, UnspecifiedSign sign) {
        // Developer note: Set the sign specification to UnspecifiedSign.NONE
        // to see which equalities fail. They should be for input defined
        // in ISO C99 with an unspecified output sign, e.g.
        // sign = UnspecifiedSign.NONE

        // Test the operation
        final Complex c = operation.apply(z);
        final ComplexDouble c1 = operation2.apply(z, TestUtils.ComplexDoubleConstructor.of());
        final Complex t1 = sign.removeSign(c);
        final Complex t2 = sign.removeSign(expected);
        if (!equals(t1.getReal(), t2.getReal()) ||
            !equals(t1.getImaginary(), t2.getImaginary())) {
            Assertions.fail(
                String.format("Operation failed (z=%s). Expected: %s but was: %s (Unspecified sign = %s)",
                              z, expected, c, c1, sign));
        }

        if (!Double.isNaN(z.getImaginary())) {
            assertConjugateEquality(z, operation, operation2, sign);
        }

        if (type != FunctionType.NONE) {
            assertFunctionType(z, operation, type, sign);

            // An odd/even function should satisfy the conjugate equality
            // on the negated complex. This ensures testing the equalities
            // hold for:
            // (re, im) =  (re, -im)
            // (re, im) =  (-re, -im) (even)
            //          = -(-re, -im) (odd)
            // (-re, -im) = (-re, im)
            if (!Double.isNaN(z.getImaginary())) {
                assertConjugateEquality(z.negate(), operation, operation2, sign);
            }
        }
    }

    /**
     * Assert {@link Complex#abs()} is functionally equivalent to using
     * {@link Math#hypot(double, double)}. If the results differ the true result
     * is computed with extended precision. The test fails if the result is further
     * than the provided ULPs from the reference result.
     *
     * <p>This can be used to assert that the custom implementation of abs() is no worse than
     * {@link Math#hypot(double, double)} which aims to be within 1 ULP of the exact result.
     *
     * <p>Note: This method will not handle an input complex that is infinite or nan so should
     * not be used for edge case tests.
     *
     * <p>Note: The true result is the sum {@code x^2 + y^2} computed using BigDecimal,
     * converted to a double and the sqrt computed using standard precision.
     * This is not the exact result as the BigDecimal
     * sqrt() function was added in Java 9 and is unavailable for the current build target.
     * In this case we require a measure of how close you can get to the nearest-double
     * representing the answer, and the not the exact distance from the answer, so this
     * is valid assuming {@link Math#sqrt(double)} has no error. The test then becomes a
     * measure of the accuracy of the high-precision sum {@code x^2 + y^2}.
     *
     * @param z the complex
     * @param ulps the maximum allowed ULPs from the exact result
     */
    private static void assertAbs(Complex z, final double observed, int ulps) {
        double x = z.getReal();
        double y = z.getImaginary();
        // For speed use Math.hypot as the reference, not BigDecimal computation.
        final double expected = Math.hypot(x, y);
        if (expected == observed) {
            // This condition will occur in the majority of cases.
            return;
        }
        // Compute the 'exact' result.
        // JDK 9 BigDecimal.sqrt() is not available so compute the standard sqrt of the
        // high precision sum. Do scaling as the high precision sum may not be in the
        // range of a double.
        int scale = 0;
        x = Math.abs(x);
        y = Math.abs(y);
        if (Math.max(x, y) > 0x1.0p+500) {
            scale = Math.getExponent(Math.max(x, y));
        } else if (Math.min(x, y) < 0x1.0p-500) {
            scale = Math.getExponent(Math.min(x, y));
        }
        if (scale != 0) {
            x = Math.scalb(x, -scale);
            y = Math.scalb(y, -scale);
        }
        // Compute and re-scale. 'exact' must be effectively final for use in the
        // assertion message supplier.
        final double result = Math.sqrt(new BigDecimal(x).pow(2).add(new BigDecimal(y).pow(2)).doubleValue());
        final double exact = scale != 0 ? Math.scalb(result, scale) : result;
        if (exact == observed) {
            // Different from Math.hypot but matches the 'exact' result
            return;
        }
        // Distance from the 'exact' result should be within tolerance.
        final long obsBits = Double.doubleToLongBits(observed);
        final long exactBits = Double.doubleToLongBits(exact);
        final long obsUlp = Math.abs(exactBits - obsBits);
        Assertions.assertTrue(obsUlp <= ulps, () -> {
            // Compute for Math.hypot for reference.
            final long expBits = Double.doubleToLongBits(expected);
            final long expUlp = Math.abs(exactBits - expBits);
            return String.format("%s.abs(). Expected %s, was %s (%d ulps). hypot %s (%d ulps)",
                z, exact, observed, obsUlp, expected, expUlp);
        });
    }

    /**
     * Assert {@link Complex#abs()} functions as per {@link Math#hypot(double, double)}.
     * The two numbers for {@code z = x + iy} are generated from the two function.
     *
     * <p>The functions should not generate numbers that are infinite or nan.
     *
     * @param rng Source of randomness
     * @param fx Function to generate x
     * @param fy Function to generate y
     * @param samples Number of samples
     */
    private static void assertAbs(UniformRandomProvider rng,
                                  ToDoubleFunction<UniformRandomProvider> fx,
                                  ToDoubleFunction<UniformRandomProvider> fy,
                                  int samples) {
        for (int i = 0; i < samples; i++) {
            double x = fx.applyAsDouble(rng);
            double y = fy.applyAsDouble(rng);
            Complex z = Complex.ofCartesian(x, y);
            assertAbs(z, z.abs(), 1);
            assertAbs(z, ComplexFunctions.abs(x, y), 1);
        }
    }

    /**
     * Creates a sub-normal number with up to 52-bits in the mantissa. The number of bits
     * to drop must be in the range [0, 51].
     *
     * @param rng Source of randomness
     * @param drop The number of mantissa bits to drop.
     * @return the number
     */
    private static double createSubNormalNumber(UniformRandomProvider rng, int drop) {
        return Double.longBitsToDouble(rng.nextLong() >>> (12 + drop));
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
     * Checks if the complex is zero. This method uses the {@code ==} operator and allows
     * equality between signed zeros: {@code -0.0 == 0.0}.
     *
     * @param c the complex
     * @return true if zero
     */
    private static boolean isZero(Complex c) {
        return c.getReal() == 0 && c.getImaginary() == 0;
    }

    /**
     * ISO C Standard G.5 (4).
     */
    @Test
    void testMultiply() {
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
                assertOperation(z, w, Complex::multiply, ComplexFunctions::multiply, "*", Complex::isInfinite, "Inf");
                assertOperation(w, z, Complex::multiply, ComplexFunctions::multiply, "*", Complex::isInfinite, "Inf");
            }
            for (final Complex w : nonZeroFinites) {
                assertOperation(z, w, Complex::multiply, ComplexFunctions::multiply, "*", Complex::isInfinite, "Inf");
                assertOperation(w, z, Complex::multiply, ComplexFunctions::multiply, "*", Complex::isInfinite, "Inf");
            }
            // C.99 refers to non-zero finites.
            // Infer that Complex multiplication of zero with infinites is not defined.
            for (final Complex w : zeroFinites) {
                assertOperation(z, w, Complex::multiply, ComplexFunctions::multiply, "*", Complex::isNaN, "NaN");
                assertOperation(w, z, Complex::multiply, ComplexFunctions::multiply, "*", Complex::isNaN, "NaN");
            }
        }

        // ISO C Standard in Annex G is missing an explicit definition of how to handle NaNs.
        // We will assume multiplication by (nan,nan) is not allowed.
        // It is undefined how to multiply when a complex has only one NaN component.
        // The reference implementation in Annex G allows it.

        // The GNU g++ compiler computes:
        // (1e300 + i 1e300) * (1e30 + i NAN) = inf + i inf
        // Thus this is allowing some computations with NaN.

        // Check multiply with (NaN,NaN) is not corrected
        final double[] values = {0, 1, inf, negInf, nan};
        for (final Complex z : createCombinations(values, c -> true)) {
            assertOperation(z, NAN, Complex::multiply, ComplexFunctions::multiply, "*", Complex::isNaN, "NaN");
            assertOperation(NAN, z, Complex::multiply, ComplexFunctions::multiply, "*", Complex::isNaN, "NaN");
        }

        // Test multiply cases which result in overflow are corrected to infinity
        assertOperation(maxMax, maxMax, Complex::multiply, ComplexFunctions::multiply, "*", Complex::isInfinite, "Inf");
        assertOperation(maxNan, maxNan, Complex::multiply, ComplexFunctions::multiply, "*", Complex::isInfinite, "Inf");
        assertOperation(nanMax, maxNan, Complex::multiply, ComplexFunctions::multiply, "*", Complex::isInfinite, "Inf");
        assertOperation(maxNan, nanMax, Complex::multiply, ComplexFunctions::multiply, "*", Complex::isInfinite, "Inf");
        assertOperation(nanMax, nanMax, Complex::multiply, ComplexFunctions::multiply, "*", Complex::isInfinite, "Inf");
    }

    /**
     * ISO C Standard G.5 (4).
     */
    @Test
    void testDivide() {
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
                assertOperation(z, w, Complex::divide, ComplexFunctions::divide, "/", Complex::isInfinite, "Inf");
            }
            for (final Complex w : zeroFinites) {
                assertOperation(z, w, Complex::divide, ComplexFunctions::divide, "/", Complex::isInfinite, "Inf");
            }
            // Check inf/inf cannot be done
            for (final Complex w : infinites) {
                assertOperation(z, w, Complex::divide, ComplexFunctions::divide, "/", Complex::isNaN, "NaN");
            }
        }

        // "if the first operand is a finite number and the second operand is an infinity, then the
        // result of the / operator is a zero;"
        for (final Complex z : finites) {
            for (final Complex w : infinites) {
                assertOperation(z, w, Complex::divide, ComplexFunctions::divide, "/", CStandardTest::isZero, "Zero");
            }
        }

        // "if the first operand is a nonzero finite number or an infinity and the second operand is
        // a zero, then the result of the / operator is an infinity."
        for (final Complex w : zeroFinites) {
            for (final Complex z : nonZeroFinites) {
                assertOperation(z, w, Complex::divide, ComplexFunctions::divide, "/", Complex::isInfinite, "Inf");
            }
            for (final Complex z : infinites) {
                assertOperation(z, w, Complex::divide, ComplexFunctions::divide, "/", Complex::isInfinite, "Inf");
            }
        }

        // ISO C Standard in Annex G is missing an explicit definition of how to handle NaNs.
        // The reference implementation does not correct for divide by NaN components unless
        // infinite.
        for (final Complex w : nans) {
            for (final Complex z : finites) {
                assertOperation(z, w, Complex::divide, ComplexFunctions::divide, "/", c -> NAN.equals(c), "(NaN,NaN)");
            }
            for (final Complex z : infinites) {
                assertOperation(z, w, Complex::divide, ComplexFunctions::divide, "/", c -> NAN.equals(c), "(NaN,NaN)");
            }
        }

        // Check (NaN,NaN) divide is not corrected for the edge case of divide by zero or infinite
        for (final Complex w : zeroFinites) {
            assertOperation(NAN, w, Complex::divide, ComplexFunctions::divide, "/", Complex::isNaN, "NaN");
        }
        for (final Complex w : infinites) {
            assertOperation(NAN, w, Complex::divide, ComplexFunctions::divide, "/", Complex::isNaN, "NaN");
        }
    }

    /**
     * ISO C Standard G.6 (3).
     */
    @Test
    void testSqrt1() {
        Complex c1 = complex(-2.0, 0.0);
        Complex c2 = complex(-2.0, -0.0);
        assertComplex(c1.sqrt(), complex(0.0, Math.sqrt(2)));
        assertComplex(c2.sqrt(), complex(0.0, -Math.sqrt(2)));

        assertComplex(ComplexFunctions.sqrt(c1.getReal(), c1.getImaginary(), TestUtils.ComplexDoubleConstructor.of()), complex(0.0, Math.sqrt(2)));
        assertComplex(ComplexFunctions.sqrt(c2.getReal(), c2.getImaginary(), TestUtils.ComplexDoubleConstructor.of()), complex(0.0, -Math.sqrt(2)));
    }

    /**
     * ISO C Standard G.6 (7).
     */
    @Test
    void testImplicitTrig() {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        for (int i = 0; i < 100; i++) {
            final double re = next(rng);
            final double im = next(rng);
            final Complex z = complex(re, im);
            final Complex iz1 = z.multiplyImaginary(1);
            assertComplex(z.asin(), iz1.asinh().multiplyImaginary(-1));
            assertComplex(z.atan(), iz1.atanh().multiplyImaginary(-1));
            assertComplex(z.cos(), iz1.cosh());
            assertComplex(z.sin(), iz1.sinh().multiplyImaginary(-1));
            assertComplex(z.tan(), iz1.tanh().multiplyImaginary(-1));

            ComplexDouble iz2 = ComplexFunctions.multiplyImaginary(z.getReal(), z.getImaginary(), TestUtils.ComplexDoubleConstructor.of());

            ComplexDouble asinh = ComplexFunctions.asinh(iz2.getReal(), iz2.getImaginary(), TestUtils.ComplexDoubleConstructor.of());
            assertComplex(z.asin(), ComplexFunctions.multiplyImaginary(asinh.getReal(), asinh.getImaginary(), -1.0, TestUtils.ComplexDoubleConstructor.of()));

            ComplexDouble atanh = ComplexFunctions.atanh(iz2.getReal(), iz2.getImaginary(), TestUtils.ComplexDoubleConstructor.of());
            assertComplex(z.atan(), ComplexFunctions.multiplyImaginary(atanh.getReal(), atanh.getImaginary(), -1.0, TestUtils.ComplexDoubleConstructor.of()));

            ComplexDouble cosh = ComplexFunctions.cosh(iz2.getReal(), iz2.getImaginary(), TestUtils.ComplexDoubleConstructor.of());
            assertComplex(z.cos(), cosh);

            ComplexDouble sinh = ComplexFunctions.sinh(iz2.getReal(), iz2.getImaginary(), TestUtils.ComplexDoubleConstructor.of());
            assertComplex(z.sin(), ComplexFunctions.multiplyImaginary(sinh.getReal(), sinh.getImaginary(), -1.0, TestUtils.ComplexDoubleConstructor.of()));

            ComplexDouble tanh = ComplexFunctions.tanh(iz2.getReal(), iz2.getImaginary(), TestUtils.ComplexDoubleConstructor.of());
            assertComplex(z.tan(), ComplexFunctions.multiplyImaginary(tanh.getReal(), tanh.getImaginary(), -1.0, TestUtils.ComplexDoubleConstructor.of()));
        }
    }

    /**
     * Create a number in the range {@code [-5,5)}.
     *
     * @param rng the random generator
     * @return the number
     */
    private static double next(UniformRandomProvider rng) {
        // Note: [0, 1) minus 1 is [-1, 0). This occurs half the time to create [-1, 1).
        return (rng.nextDouble() - rng.nextInt(1)) * 5;
    }

    /**
     * ISO C Standard G.6 (6) for abs().
     * Functionality is defined by ISO C Standard F.9.4.3 hypot function.
     */
    @Test
    void testAbs() {
        Assertions.assertEquals(inf, complex(inf, nan).abs());
        Assertions.assertEquals(inf, complex(negInf, nan).abs());

        Assertions.assertEquals(inf, ComplexFunctions.abs(inf, nan));
        Assertions.assertEquals(inf, ComplexFunctions.abs(negInf, nan));

        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        for (int i = 0; i < 10; i++) {
            final double x = next(rng);
            final double y = next(rng);
            Assertions.assertEquals(complex(x, y).abs(), complex(y, x).abs());
            Assertions.assertEquals(complex(x, y).abs(), complex(x, -y).abs());
            Assertions.assertEquals(Math.abs(x), complex(x, 0.0).abs());
            Assertions.assertEquals(Math.abs(x), complex(x, -0.0).abs());
            Assertions.assertEquals(inf, complex(inf, y).abs());
            Assertions.assertEquals(inf, complex(negInf, y).abs());

            Assertions.assertEquals(ComplexFunctions.abs(x, y), ComplexFunctions.abs(y, x));
            Assertions.assertEquals(ComplexFunctions.abs(x, y), ComplexFunctions.abs(x, -y));
            Assertions.assertEquals(Math.abs(x), ComplexFunctions.abs(x, 0.0));
            Assertions.assertEquals(Math.abs(x), ComplexFunctions.abs(x, -0.0));
            Assertions.assertEquals(inf, ComplexFunctions.abs(inf, y));
            Assertions.assertEquals(inf, ComplexFunctions.abs(negInf, y));
        }

        // Test verses Math.hypot due to the use of a custom implementation.
        // First test edge cases. Use negatives to test the sign is correctly removed.
        final double[] parts = {-0.0, -Double.MIN_VALUE, -Double.MIN_NORMAL, -Double.MAX_VALUE,
            Double.NEGATIVE_INFINITY, Double.NaN};
        for (final double x : parts) {
            for (final double y : parts) {
                Assertions.assertEquals(Math.hypot(x, y), complex(x, y).abs());
                Assertions.assertEquals(Math.hypot(x, y), ComplexFunctions.abs(x, y));
            }
        }

        // The reference fdlibm hypot implementation orders using the upper 32-bits of the double.
        // Tests using random numbers that differ in only the lower 32-bits
        // show a frequency of <1e-9 that the computation is not commutative: f(x, y) != f(y, x)
        // Test known cases where ordering is required on the lower 32-bits to ensure |z| == |iz|.
        // These cases would fail a direct fdlibm conversion of this:
        // https://www.netlib.org/fdlibm/e_hypot.c
        for (final double[] pair : new double[][] {
                {1.3122561682406755, 1.3122565442732959},
                {1.40905821964671, 1.4090583434236112},
                {1.912164268932753, 1.9121638616231227}}) {
            final Complex z = complex(pair[0], pair[1]);
            Assertions.assertEquals(z.abs(), z.multiplyImaginary(1).abs(), "Expected |z| == |iz|");

            ComplexDouble multiplyImaginaryResult  = ComplexFunctions.multiplyImaginary(z.getReal(), z.getImaginary(), TestUtils.ComplexDoubleConstructor.of());
            Assertions.assertEquals(ComplexFunctions.abs(z.getReal(), z.getImaginary()), ComplexFunctions.abs(multiplyImaginaryResult.getReal(), multiplyImaginaryResult.getImaginary()), "Expected |z| == |iz|");
        }

        // Test with a range of numbers.
        // Sub-normals require special handling so we use different variations of these to
        // ensure they are handled correctly.
        // Note:
        // 1 ULP differences with Math.hypot can be observed due to the different implementations.
        // For normal numbers observable differences require billions of numbers to show a
        // few hundred cases of lower ULPs and a magnitude smaller count of higher ULPs.
        // For sub-normal numbers a few thousand examples can demonstrate
        // a larger count of 1 ULP improvements than 1 ULP errors verses Math.hypot.
        // A formal statistical test to demonstrate differences are significant is not implemented.
        // This test simply asserts the answer is either the same as Math.hypot or else is within
        // 1 ULP of a high precision computation.
        final int samples = 100;
        assertAbs(rng, r -> createSubNormalNumber(r, 0), r -> createSubNormalNumber(r, 0), samples);
        assertAbs(rng, r -> createSubNormalNumber(r, 0), r -> createSubNormalNumber(r, 1), samples);
        assertAbs(rng, r -> createSubNormalNumber(r, 0), r -> createSubNormalNumber(r, 2), samples);
        // Numbers on the same scale (fixed exponent)
        assertAbs(rng, r -> createFixedExponentNumber(r, 0), r -> createFixedExponentNumber(r, 0), samples);
        // Numbers on different scales
        assertAbs(rng, r -> createFixedExponentNumber(r, 0), r -> createFixedExponentNumber(r, 1), samples);
        assertAbs(rng, r -> createFixedExponentNumber(r, 0), r -> createFixedExponentNumber(r, 2 + r.nextInt(10)), samples);
        // Intermediate overflow / underflow
        assertAbs(rng, r -> createFixedExponentNumber(r, 1022), r -> createFixedExponentNumber(r, 1022), samples);
        assertAbs(rng, r -> createFixedExponentNumber(r, -1022), r -> createFixedExponentNumber(r, -1022), samples);
        // Complex cis numbers
        final ToDoubleFunction<UniformRandomProvider> cisGenerator = new ToDoubleFunction<UniformRandomProvider>() {
            private double tmp = Double.NaN;
            @Override
            public double applyAsDouble(UniformRandomProvider rng) {
                if (Double.isNaN(tmp)) {
                    double u = rng.nextDouble() * Math.PI;
                    tmp = Math.cos(u);
                    return Math.sin(u);
                }
                final double r = tmp;
                tmp = Double.NaN;
                return r;
            }
        };
        assertAbs(rng, cisGenerator, cisGenerator, samples);
    }

    /**
     * ISO C Standard G.6.1.1.
     */
    @Test
    void testAcos() {
        final UnaryOperator<Complex> operation1 = Complex::acos;
        final ComplexUnaryOperator<ComplexDouble> operation2 = ComplexFunctions::acos;
        assertConjugateEquality(operation1, operation2);
        assertComplex(Complex.ZERO, operation1, piTwoNegZero, operation2);
        assertComplex(negZeroZero, operation1, piTwoNegZero, operation2);
        assertComplex(zeroNaN, operation1, piTwoNaN, operation2);
        assertComplex(negZeroNaN, operation1, piTwoNaN, operation2);
        for (double x : finite) {
            assertComplex(complex(x, inf), operation1, piTwoNegInf, operation2);
        }
        for (double x : nonZeroFinite) {
            assertComplex(complex(x, nan), operation1, NAN, operation2);
        }
        for (double y : positiveFinite) {
            assertComplex(complex(-inf, y), operation1, piNegInf, operation2);
        }
        for (double y : positiveFinite) {
            assertComplex(complex(inf, y), operation1, zeroNegInf, operation2);
        }
        assertComplex(negInfInf, operation1, threePiFourNegInf, operation2);
        assertComplex(infInf, operation1, piFourNegInf, operation2);
        assertComplex(infNaN, operation1, operation2, nanInf, UnspecifiedSign.IMAGINARY);
        assertComplex(negInfNaN, operation1, operation2, nanNegInf, UnspecifiedSign.IMAGINARY);
        for (double y : finite) {
            assertComplex(complex(nan, y), operation1, NAN, operation2);
        }
        assertComplex(nanInf, operation1, nanNegInf, operation2);
        assertComplex(NAN, operation1, NAN, operation2);
    }

    /**
     * ISO C Standard G.6.2.1.
     *
     * @see <a href="http://www.open-std.org/jtc1/sc22/wg14/www/docs/n1892.htm#dr_471">
     *   Complex math functions cacosh and ctanh</a>
     */
    @Test
    void testAcosh() {
        final UnaryOperator<Complex> operation1 = Complex::acosh;
        final ComplexUnaryOperator<ComplexDouble> operation2 = ComplexFunctions::acosh;

        assertConjugateEquality(operation1, operation2);
        assertComplex(Complex.ZERO, operation1, zeroPiTwo, operation2);
        assertComplex(negZeroZero, operation1, zeroPiTwo, operation2);
        for (double x : finite) {
            assertComplex(complex(x, inf), operation1, infPiTwo, operation2);
        }
        assertComplex(zeroNaN, operation1, nanPiTwo, operation2);
        assertComplex(negZeroNaN, operation1, nanPiTwo, operation2);
        for (double x : nonZeroFinite) {
            assertComplex(complex(x, nan), operation1, NAN, operation2);
        }
        for (double y : positiveFinite) {
            assertComplex(complex(-inf, y), operation1, infPi, operation2);
        }
        for (double y : positiveFinite) {
            assertComplex(complex(inf, y), operation1, infZero, operation2);
        }
        assertComplex(negInfInf, operation1, infThreePiFour, operation2);
        assertComplex(infInf, operation1, infPiFour, operation2);
        assertComplex(infNaN, operation1, infNaN, operation2);
        assertComplex(negInfNaN, operation1, infNaN, operation2);
        for (double y : finite) {
            assertComplex(complex(nan, y), operation1, NAN, operation2);
        }
        assertComplex(nanInf, operation1, infNaN, operation2);
        assertComplex(NAN, operation1, NAN, operation2);
    }

    /**
     * ISO C Standard G.6.2.2.
     */
    @Test
    void testAsinh() {
        final UnaryOperator<Complex> operation1 = Complex::asinh;
        final ComplexUnaryOperator<ComplexDouble> operation2 = ComplexFunctions::asinh;

        final FunctionType type = FunctionType.ODD;
        assertConjugateEquality(operation1, operation2);
        assertFunctionType(operation1, type);
        assertComplex(Complex.ZERO, operation1, operation2, Complex.ZERO, type);
        for (double x : positiveFinite) {
            assertComplex(complex(x, inf), operation1, operation2, infPiTwo, type);
        }
        for (double x : finite) {
            assertComplex(complex(x, nan), operation1, operation2, NAN, type);
        }
        for (double y : positiveFinite) {
            assertComplex(complex(inf, y), operation1, operation2, infZero, type);
        }
        assertComplex(infInf, operation1, operation2, infPiFour, type);
        assertComplex(infNaN, operation1, operation2, infNaN, type);
        assertComplex(nanZero, operation1, operation2, nanZero, type);
        for (double y : nonZeroFinite) {
            assertComplex(complex(nan, y), operation1, operation2, NAN, type);
        }
        assertComplex(nanInf, operation1, operation2, infNaN, type, UnspecifiedSign.REAL);
        assertComplex(NAN, operation1, operation2, NAN, type);
    }

    /**
     * ISO C Standard G.6.2.3.
     */
    @Test
    void testAtanh() {
        final UnaryOperator<Complex> operation1 = Complex::atanh;
        final ComplexUnaryOperator<ComplexDouble> operation2 = ComplexFunctions::atanh;
        final FunctionType type = FunctionType.ODD;
        assertConjugateEquality(operation1, operation2);
        assertFunctionType(operation1, type);
        assertComplex(Complex.ZERO, operation1, operation2, Complex.ZERO, type);
        assertComplex(zeroNaN, operation1, operation2, zeroNaN, type);
        assertComplex(oneZero, operation1, operation2, infZero, type);
        for (double x : positiveFinite) {
            assertComplex(complex(x, inf), operation1, operation2, zeroPiTwo, type);
        }
        for (double x : nonZeroFinite) {
            assertComplex(complex(x, nan), operation1, operation2, NAN, type);
        }
        for (double y : positiveFinite) {
            assertComplex(complex(inf, y), operation1, operation2, zeroPiTwo, type);
        }
        assertComplex(infInf, operation1, operation2, zeroPiTwo, type);
        assertComplex(infNaN, operation1, operation2, zeroNaN, type);
        assertComplex(nanZero, operation1, operation2, NAN, type);
        for (double y : finite) {
            assertComplex(complex(nan, y), operation1, operation2, NAN, type);
        }
        assertComplex(nanInf, operation1, operation2, zeroPiTwo, type, UnspecifiedSign.REAL);
        assertComplex(NAN, operation1, operation2, NAN, type);
    }

    /**
     * ISO C Standard G.6.2.4.
     */
    @Test
    void testCosh() {
        final UnaryOperator<Complex> operation1 = Complex::cosh;
        final ComplexUnaryOperator<ComplexDouble> operation2 = ComplexFunctions::cosh;

        final FunctionType type = FunctionType.EVEN;
        assertConjugateEquality(operation1, operation2);
        assertFunctionType(operation1, type);
        assertComplex(Complex.ZERO, operation1, operation2, Complex.ONE, type);
        assertComplex(zeroInf, operation1, operation2, nanZero, type, UnspecifiedSign.IMAGINARY);
        assertComplex(zeroNaN, operation1, operation2, nanZero, type, UnspecifiedSign.IMAGINARY);
        for (double x : nonZeroFinite) {
            assertComplex(complex(x, inf), operation1, operation2, NAN, type);
        }
        for (double x : nonZeroFinite) {
            assertComplex(complex(x, nan), operation1, operation2, NAN, type);
        }
        assertComplex(infZero, operation1, operation2, infZero, type);
        for (double y : nonZeroFinite) {
            assertComplex(complex(inf, y), operation1, operation2, Complex.ofCis(y).multiply(inf), type);
        }
        assertComplex(infInf, operation1, operation2, infNaN, type, UnspecifiedSign.REAL);
        assertComplex(infNaN, operation1, operation2, infNaN, type);
        assertComplex(nanZero, operation1, operation2, nanZero, type, UnspecifiedSign.IMAGINARY);
        for (double y : nonZero) {
            assertComplex(complex(nan, y), operation1, operation2, NAN, type);
        }
        assertComplex(NAN, operation1, operation2, NAN, type);
    }

    /**
     * ISO C Standard G.6.2.5.
     */
    @Test
    void testSinh() {
        final UnaryOperator<Complex> operation1 = Complex::sinh;
        final ComplexUnaryOperator<ComplexDouble> operation2 = ComplexFunctions::sinh;

        final FunctionType type = FunctionType.ODD;
        assertConjugateEquality(operation1, operation2);
        assertFunctionType(operation1, type);
        assertComplex(Complex.ZERO, operation1, operation2, Complex.ZERO, type);
        assertComplex(zeroInf, operation1, operation2, zeroNaN, type, UnspecifiedSign.REAL);
        assertComplex(zeroNaN, operation1, operation2, zeroNaN, type, UnspecifiedSign.REAL);
        for (double x : nonZeroPositiveFinite) {
            assertComplex(complex(x, inf), operation1, operation2, NAN, type);
        }
        for (double x : nonZeroFinite) {
            assertComplex(complex(x, nan), operation1, operation2, NAN, type);
        }
        assertComplex(infZero, operation1, operation2, infZero, type);
        // Note: Error in the ISO C99 reference to use positive finite y but the zero case is different
        for (double y : nonZeroFinite) {
            assertComplex(complex(inf, y), operation1, operation2, Complex.ofCis(y).multiply(inf), type);
        }
        assertComplex(infInf, operation1, operation2, infNaN, type, UnspecifiedSign.REAL);
        assertComplex(infNaN, operation1, operation2, infNaN, type, UnspecifiedSign.REAL);
        assertComplex(nanZero, operation1, operation2, nanZero, type);
        for (double y : nonZero) {
            assertComplex(complex(nan, y), operation1, operation2, NAN, type);
        }
        assertComplex(NAN, operation1, operation2, NAN, type);
    }

    /**
     * ISO C Standard G.6.2.6.
     *
     * @see <a href="http://www.open-std.org/jtc1/sc22/wg14/www/docs/n1892.htm#dr_471">
     *   Complex math functions cacosh and ctanh</a>
     */
    @Test
    void testTanh() {
        final UnaryOperator<Complex> operation1 = Complex::tanh;
        final ComplexUnaryOperator<ComplexDouble> operation2 = ComplexFunctions::tanh;

        final FunctionType type = FunctionType.ODD;
        assertConjugateEquality(operation1, operation2);
        assertFunctionType(operation1, type);
        assertComplex(Complex.ZERO, operation1, operation2, Complex.ZERO, type);
        assertComplex(zeroInf, operation1, operation2, zeroNaN, type);
        for (double x : nonZeroFinite) {
            assertComplex(complex(x, inf), operation1, operation2, NAN, type);
        }
        assertComplex(zeroNaN, operation1, operation2, zeroNaN, type);
        for (double x : nonZeroFinite) {
            assertComplex(complex(x, nan), operation1, operation2, NAN, type);
        }
        for (double y : positiveFinite) {
            assertComplex(complex(inf, y), operation1, operation2, complex(1.0, Math.copySign(0, Math.sin(2 * y))), type);
        }
        assertComplex(infInf, operation1, operation2, oneZero, type, UnspecifiedSign.IMAGINARY);
        assertComplex(infNaN, operation1, operation2, oneZero, type, UnspecifiedSign.IMAGINARY);
        assertComplex(nanZero, operation1, operation2, nanZero, type);
        for (double y : nonZero) {
            assertComplex(complex(nan, y), operation1, operation2, NAN, type);
        }
        assertComplex(NAN, operation1, operation2, NAN, type);
    }

    /**
     * ISO C Standard G.6.3.1.
     */
    @Test
    void testExp() {
        final UnaryOperator<Complex> operation1 = Complex::exp;
        final ComplexUnaryOperator<ComplexDouble> operation2 = ComplexFunctions::exp;

        assertConjugateEquality(operation1, operation2);
        assertComplex(Complex.ZERO, operation1, oneZero, operation2);
        assertComplex(negZeroZero, operation1, oneZero, operation2);
        for (double x : finite) {
            assertComplex(complex(x, inf), operation1, NAN, operation2);
        }
        for (double x : finite) {
            assertComplex(complex(x, nan), operation1, NAN, operation2);
        }
        assertComplex(infZero, operation1, infZero, operation2);
        for (double y : finite) {
            assertComplex(complex(-inf, y), operation1, Complex.ofCis(y).multiply(0.0), operation2);
        }
        for (double y : nonZeroFinite) {
            assertComplex(complex(inf, y), operation1, Complex.ofCis(y).multiply(inf), operation2);
        }
        assertComplex(negInfInf, operation1, operation2, Complex.ZERO, UnspecifiedSign.REAL_IMAGINARY);
        assertComplex(infInf, operation1, operation2, infNaN, UnspecifiedSign.REAL);
        assertComplex(negInfNaN, operation1, operation2, Complex.ZERO, UnspecifiedSign.REAL_IMAGINARY);
        assertComplex(infNaN, operation1, operation2, infNaN, UnspecifiedSign.REAL);
        assertComplex(nanZero, operation1, nanZero, operation2);
        for (double y : nonZero) {
            assertComplex(complex(nan, y), operation1, NAN, operation2);
        }
        assertComplex(NAN, operation1, NAN, operation2);
    }

    /**
     * ISO C Standard G.6.3.2.
     */
    @Test
    void testLog() {
        final UnaryOperator<Complex> operation1 = Complex::log;
        final ComplexUnaryOperator<ComplexDouble> operation2 = ComplexFunctions::log;

        assertConjugateEquality(operation1, operation2);
        assertComplex(negZeroZero, operation1, negInfPi, operation2);
        assertComplex(Complex.ZERO, operation1, negInfZero, operation2);
        for (double x : finite) {
            assertComplex(complex(x, inf), operation1, infPiTwo, operation2);
        }
        for (double x : finite) {
            assertComplex(complex(x, nan), operation1, NAN, operation2);
        }
        for (double y : positiveFinite) {
            assertComplex(complex(-inf, y), operation1, infPi, operation2);
        }
        for (double y : positiveFinite) {
            assertComplex(complex(inf, y), operation1, infZero, operation2);
        }
        assertComplex(negInfInf, operation1, infThreePiFour, operation2);
        assertComplex(infInf, operation1, infPiFour, operation2);
        assertComplex(negInfNaN, operation1, infNaN, operation2);
        assertComplex(infNaN, operation1, infNaN, operation2);
        for (double y : finite) {
            assertComplex(complex(nan, y), operation1, NAN, operation2);
        }
        assertComplex(nanInf, operation1, infNaN, operation2);
        assertComplex(NAN, operation1, NAN, operation2);
    }

    /**
     * Same edge cases as log() since the real component is divided by Math.log(10) which
     * has no effect on infinite or nan.
     */
    @Test
    void testLog10() {
        final UnaryOperator<Complex> operation1 = Complex::log10;
        final ComplexUnaryOperator<ComplexDouble> operation2 = ComplexFunctions::log10;

        assertConjugateEquality(operation1, operation2);
        assertComplex(negZeroZero, operation1, negInfPi, operation2);
        assertComplex(Complex.ZERO, operation1, negInfZero, operation2);
        for (double x : finite) {
            assertComplex(complex(x, inf), operation1, infPiTwo, operation2);
        }
        for (double x : finite) {
            assertComplex(complex(x, nan), operation1, NAN, operation2);
        }
        for (double y : positiveFinite) {
            assertComplex(complex(-inf, y), operation1, infPi, operation2);
        }
        for (double y : positiveFinite) {
            assertComplex(complex(inf, y), operation1, infZero, operation2);
        }
        assertComplex(negInfInf, operation1, infThreePiFour, operation2);
        assertComplex(infInf, operation1, infPiFour, operation2);
        assertComplex(negInfNaN, operation1, infNaN, operation2);
        assertComplex(infNaN, operation1, infNaN, operation2);
        for (double y : finite) {
            assertComplex(complex(nan, y), operation1, NAN, operation2);
        }
        assertComplex(nanInf, operation1, infNaN, operation2);
        assertComplex(NAN, operation1, NAN, operation2);
    }

    /**
     * ISO C Standard G.6.4.2.
     */
    @Test
    void testSqrt() {
        final UnaryOperator<Complex> operation1 = Complex::sqrt;
        final ComplexUnaryOperator<ComplexDouble> operation2 = ComplexFunctions::sqrt;

        assertConjugateEquality(operation1, operation2);
        assertComplex(negZeroZero, operation1, Complex.ZERO, operation2);
        assertComplex(Complex.ZERO, operation1, Complex.ZERO, operation2);
        for (double x : finite) {
            assertComplex(complex(x, inf), operation1, infInf, operation2);
        }
        // Include infinity and nan for (x, inf).
        assertComplex(infInf, operation1, infInf, operation2);
        assertComplex(negInfInf, operation1, infInf, operation2);
        assertComplex(nanInf, operation1, infInf, operation2);
        for (double x : finite) {
            assertComplex(complex(x, nan), operation1, NAN, operation2);
        }
        for (double y : positiveFinite) {
            assertComplex(complex(-inf, y), operation1, zeroInf, operation2);
        }
        for (double y : positiveFinite) {
            assertComplex(complex(inf, y), operation1, infZero, operation2);
        }
        assertComplex(negInfNaN, operation1, operation2, nanInf, UnspecifiedSign.IMAGINARY);
        assertComplex(infNaN, operation1, infNaN, operation2);
        for (double y : finite) {
            assertComplex(complex(nan, y), operation1, NAN, operation2);
        }
        assertComplex(NAN, operation1, NAN, operation2);
    }
}
