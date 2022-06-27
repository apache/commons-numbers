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


/**
 * Cartesian representation of a complex number. The complex number is expressed
 * in the form \( a + ib \) where \( a \) and \( b \) are real numbers and \( i \)
 * is the imaginary unit which satisfies the equation \( i^2 = -1 \). For the
 * complex number \( a + ib \), \( a \) is called the <em>real part</em> and
 * \( b \) is called the <em>imaginary part</em>.
 *
 * <p>This class is all the binary arithmetics. All the arithmetics that use two Complex
 * numbers to produce a Complex result.
 * All arithmetic will create a new instance for the result.</p>
 */
public final class ComplexBiFunctions {


    /**
     * Private constructor for utility class.
     */
    private ComplexBiFunctions() {

    }

    /**
     * Box values for the real or imaginary component of an infinite complex number.
     * Any infinite value will be returned as one. Non-infinite values will be returned as zero.
     * The sign is maintained.
     *
     * <pre>
     *  inf  =  1
     * -inf  = -1
     *  x    =  0
     * -x    = -0
     * </pre>
     *
     * @param component the component
     * @return The boxed value
     */
    private static double boxInfinity(double component) {
        return Math.copySign(Double.isInfinite(component) ? 1.0 : 0.0, component);
    }

    /**
     * Checks if the complex number is not zero.
     *
     * @param real the real component
     * @param imaginary the imaginary component
     * @return true if the complex is not zero
     */
    private static boolean isNotZero(double real, double imaginary) {
        // The use of equals is deliberate.
        // This method must distinguish NaN from zero thus ruling out:
        // (real != 0.0 || imaginary != 0.0)
        return !(real == 0.0 && imaginary == 0.0);
    }

    /**
     * Change NaN to zero preserving the sign; otherwise return the value.
     *
     * @param value the value
     * @return The new value
     */
    private static double changeNaNtoZero(double value) {
        return Double.isNaN(value) ? Math.copySign(0.0, value) : value;
    }

    /**
     * Returns a {@code Complex} whose value is obtained using the real and
     * imaginary parts of both Complex numbers.
     * Implements the formula:
     *
     * <p>\[ (a + i b)(c + i d) = (ac - bd) + i (ad + bc) \]
     *
     * <p>Recalculates to recover infinities as specified in C99 standard G.5.1.
     *
     * @param r1 real 1
     * @param i1 imaginary 2
     * @param r2 real 2
     * @param i2 imaginary 2
     * @param result Constructor
     * @return DComplex
     * @see <a href="http://mathworld.wolfram.com/ComplexMultiplication.html">Complex Muliplication</a>
     */
    private static DComplex multiply(double r1, double i1,
                                    double r2, double i2, DComplexConstructor<DComplex> result) {
        double a = r1;
        double b = i1;
        double c = r2;
        double d = i2;
        final double ac = a * c;
        final double bd = b * d;
        final double ad = a * d;
        final double bc = b * c;
        double x = ac - bd;
        double y = ad + bc;

        // --------------
        // NaN can occur if:
        // - any of (a,b,c,d) are NaN (for NaN or Infinite complex numbers)
        // - a multiplication of infinity by zero (ac,bd,ad,bc).
        // - a subtraction of infinity from infinity (e.g. ac - bd)
        //   Note that (ac,bd,ad,bc) can be infinite due to overflow.
        //
        // Detect a NaN result and perform correction.
        //
        // Modification from the listing in ISO C99 G.5.1 (6)
        // Do not correct infinity multiplied by zero. This is left as NaN.
        // --------------

        if (Double.isNaN(x) && Double.isNaN(y)) {
            // Recover infinities that computed as NaN+iNaN ...
            boolean recalc = false;
            if ((Double.isInfinite(a) || Double.isInfinite(b)) &&
                isNotZero(c, d)) {
                // This complex is infinite.
                // "Box" the infinity and change NaNs in the other factor to 0.
                a = boxInfinity(a);
                b = boxInfinity(b);
                c = changeNaNtoZero(c);
                d = changeNaNtoZero(d);
                recalc = true;
            }
            if ((Double.isInfinite(c) || Double.isInfinite(d)) &&
                isNotZero(a, b)) {
                // The other complex is infinite.
                // "Box" the infinity and change NaNs in the other factor to 0.
                c = boxInfinity(c);
                d = boxInfinity(d);
                a = changeNaNtoZero(a);
                b = changeNaNtoZero(b);
                recalc = true;
            }
            if (!recalc && (Double.isInfinite(ac) || Double.isInfinite(bd) ||
                Double.isInfinite(ad) || Double.isInfinite(bc))) {
                // The result overflowed to infinity.
                // Recover infinities from overflow by changing NaNs to 0 ...
                a = changeNaNtoZero(a);
                b = changeNaNtoZero(b);
                c = changeNaNtoZero(c);
                d = changeNaNtoZero(d);
                recalc = true;
            }
            if (recalc) {
                x = Double.POSITIVE_INFINITY * (a * c - b * d);
                y = Double.POSITIVE_INFINITY * (a * d + b * c);
            }
        }
        return result.apply(x, y);
    }

    /**
     * Returns a {@code Complex} whose value is obtained using the real
     * and imaginary part of the Complex number, with {@code factor} interpreted as a real number.
     * Implements the formula:
     *
     * <p>\[ (a + i b) c =  (ac) + i (bc) \]
     *
     * <p>This method is included for compatibility with ISO C99 which defines arithmetic between
     * real-only and complex numbers.</p>
     *
     * <p>Note: This method should be preferred over using
     * {@link #multiply(DComplex, DComplex, DComplexConstructor) multiply(Complex.ofCartesian(factor, 0))}. Multiplication
     * can generate signed zeros if either {@code this} complex has zeros for the real
     * and/or imaginary component, or if the factor is zero. The summation of signed zeros
     * in {@link #multiply(DComplex, DComplex, DComplexConstructor)} may create zeros in the result that differ in sign
     * from the equivalent call to multiply by a real-only number.
     *
     * @param r real
     * @param i imaginary
     * @param f Value this complex number is to being multiplied with.
     * @param result Constructor
     * @return DComplex
     * @see #multiply(DComplex, DComplex, DComplexConstructor)
     */
    private static DComplex multiply(double r, double i, double f, DComplexConstructor<DComplex> result) {
        return result.apply(r * f, i * f);
    }

    /**
     * Returns a {@code Complex} whose value is {@code c * factor}, with {@code factor}
     * interpreted as a real number.
     * Implements the formula:
     *
     * <p>\[ (a + i b) c =  (ac) + i (bc) \]
     *
     * <p>This method is included for compatibility with ISO C99 which defines arithmetic between
     * real-only and complex numbers.</p>
     *
     * <p>Note: This method should be preferred over using
     * {@link #multiply(DComplex, DComplex, DComplexConstructor) multiply(Complex.ofCartesian(factor, 0))}. Multiplication
     * can generate signed zeros if either {@code this} complex has zeros for the real
     * and/or imaginary component, or if the factor is zero. The summation of signed zeros
     * in {@link #multiply(DComplex, DComplex, DComplexConstructor)} may create zeros in the result that differ in sign
     * from the equivalent call to multiply by a real-only number.
     *
     * @param c Complex number
     * @param f Value this complex number is to being multiplied with.
     * @param result Constructor
     * @return {@code c * factor}.
     * @see #multiply(DComplex, DComplex, DComplexConstructor)
     */
    public static DComplex multiply(DComplex c, double f, DComplexConstructor<DComplex> result) {
        return multiply(c.real(), c.imag(), f, result);
    }

    /**
     * Returns a {@code Complex} whose value is {@code c1 * c2}.
     * Implements the formula:
     *
     * <p>\[ (a + i b)(c + i d) = (ac - bd) + i (ad + bc) \]
     *
     * <p>Recalculates to recover infinities as specified in C99 standard G.5.1.
     *
     * @param c1 Complex number 1
     * @param c2 Complex number 2
     * @param result Constructor
     * @return {@code c1 * c2}.
     * @see <a href="http://mathworld.wolfram.com/ComplexMultiplication.html">Complex Muliplication</a>
     */
    public static DComplex multiply(DComplex c1, DComplex c2,
                                    DComplexConstructor<DComplex> result) {
        return multiply(c1.real(), c1.imag(), c2.real(), c2.imag(), result);
    }

    /**
     * Returns a {@code Complex} whose value is obtained using the real and
     * imaginary parts of both Complex numbers.
     * Implements the formula:
     *
     * <p>\[ \frac{a + i b}{c + i d} = \frac{(ac + bd) + i (bc - ad)}{c^2+d^2} \]
     *
     * <p>Re-calculates NaN result values to recover infinities as specified in C99 standard G.5.1.
     *
     * @param re1 real 1
     * @param im1 imaginary 2
     * @param re2 real 2
     * @param im2 imaginary 2
     * @param result Constructor
     * @return DComplex
     * @see <a href="http://mathworld.wolfram.com/ComplexDivision.html">Complex Division</a>
     */
    private static DComplex divide(double re1, double im1,
                                  double re2, double im2, DComplexConstructor<DComplex> result) {
        double a = re1;
        double b = im1;
        double c = re2;
        double d = im2;
        int ilogbw = 0;
        // Get the exponent to scale the divisor parts to the range [1, 2).
        final int exponent = getScale(c, d);
        if (exponent <= Double.MAX_EXPONENT) {
            ilogbw = exponent;
            c = Math.scalb(c, -ilogbw);
            d = Math.scalb(d, -ilogbw);
        }
        final double denom = c * c + d * d;

        // Note: Modification from the listing in ISO C99 G.5.1 (8):
        // Avoid overflow if a or b are very big.
        // Since (c, d) in the range [1, 2) the sum (ac + bd) could overflow
        // when (a, b) are both above (Double.MAX_VALUE / 4). The same applies to
        // (bc - ad) with large negative values.
        // Use the maximum exponent as an approximation to the magnitude.
        if (getMaxExponent(a, b) > Double.MAX_EXPONENT - 2) {
            ilogbw -= 2;
            a /= 4;
            b /= 4;
        }

        double x = Math.scalb((a * c + b * d) / denom, -ilogbw);
        double y = Math.scalb((b * c - a * d) / denom, -ilogbw);
        // Recover infinities and zeros that computed as NaN+iNaN
        // the only cases are nonzero/zero, infinite/finite, and finite/infinite, ...
        if (Double.isNaN(x) && Double.isNaN(y)) {
            if (denom == 0.0 &&
                (!Double.isNaN(a) || !Double.isNaN(b))) {
                // nonzero/zero
                // This case produces the same result as divide by a real-only zero
                // using Complex.divide(+/-0.0)
                x = Math.copySign(Double.POSITIVE_INFINITY, c) * a;
                y = Math.copySign(Double.POSITIVE_INFINITY, c) * b;
            } else if ((Double.isInfinite(a) || Double.isInfinite(b)) &&
                Double.isFinite(c) && Double.isFinite(d)) {
                // infinite/finite
                a = boxInfinity(a);
                b = boxInfinity(b);
                x = Double.POSITIVE_INFINITY * (a * c + b * d);
                y = Double.POSITIVE_INFINITY * (b * c - a * d);
            } else if ((Double.isInfinite(c) || Double.isInfinite(d)) &&
                Double.isFinite(a) && Double.isFinite(b)) {
                // finite/infinite
                c = boxInfinity(c);
                d = boxInfinity(d);
                x = 0.0 * (a * c + b * d);
                y = 0.0 * (b * c - a * d);
            }
        }
        return result.apply(x, y);
    }

    /**
     * Returns a {@code Complex} whose value is {@code (c1 / c2)}.
     * Implements the formula:
     *
     * <p>\[ \frac{a + i b}{c + i d} = \frac{(ac + bd) + i (bc - ad)}{c^2+d^2} \]
     *
     * <p>Re-calculates NaN result values to recover infinities as specified in C99 standard G.5.1.
     *
     * @param c1 Complex number 1
     * @param c2 Complex number 2
     * @param result Constructor
     * @return {@code c1 / c2}.
     * @see <a href="http://mathworld.wolfram.com/ComplexDivision.html">Complex Division</a>
     */
    public static DComplex divide(DComplex c1, DComplex c2,
                                  DComplexConstructor<DComplex> result) {
        return divide(c1.real(), c1.imag(), c2.real(), c2.imag(), result);
    }

    /**
     * Returns a scale suitable for use with {@link Math#scalb(double, int)} to normalise
     * the number to the interval {@code [1, 2)}.
     *
     * <p>The scale is typically the largest unbiased exponent used in the representation of the
     * two numbers. In contrast to {@link Math#getExponent(double)} this handles
     * sub-normal numbers by computing the number of leading zeros in the mantissa
     * and shifting the unbiased exponent. The result is that for all finite, non-zero,
     * numbers {@code a, b}, the magnitude of {@code scalb(x, -getScale(a, b))} is
     * always in the range {@code [1, 2)}, where {@code x = max(|a|, |b|)}.
     *
     * <p>This method is a functional equivalent of the c function ilogb(double) adapted for
     * two input arguments.
     *
     * <p>The result is to be used to scale a complex number using {@link Math#scalb(double, int)}.
     * Hence the special case of both zero arguments is handled using the return value for NaN
     * as zero cannot be scaled. This is different from {@link Math#getExponent(double)}
     * or {@link #getMaxExponent(double, double)}.
     *
     * <p>Special cases:
     *
     * <ul>
     * <li>If either argument is NaN or infinite, then the result is
     * {@link Double#MAX_EXPONENT} + 1.
     * <li>If both arguments are zero, then the result is
     * {@link Double#MAX_EXPONENT} + 1.
     * </ul>
     *
     * @param a the first value
     * @param b the second value
     * @return The maximum unbiased exponent of the values to be used for scaling
     * @see Math#getExponent(double)
     * @see Math#scalb(double, int)
     * @see <a href="http://www.cplusplus.com/reference/cmath/ilogb/">ilogb</a>
     */
    private static int getScale(double a, double b) {
        // Only interested in the exponent and mantissa so remove the sign bit
        final long x = Double.doubleToRawLongBits(a) & ComplexFunctions.UNSIGN_MASK;
        final long y = Double.doubleToRawLongBits(b) & ComplexFunctions.UNSIGN_MASK;
        // Only interested in the maximum
        final long bits = Math.max(x, y);
        // Get the unbiased exponent
        int exp = ((int) (bits >>> 52)) - ComplexFunctions.EXPONENT_OFFSET;

        // No case to distinguish nan/inf
        // Handle sub-normal numbers
        if (exp == Double.MIN_EXPONENT - 1) {
            // Special case for zero, return as nan/inf to indicate scaling is not possible
            if (bits == 0) {
                return Double.MAX_EXPONENT + 1;
            }
            // A sub-normal number has an exponent below -1022. The amount below
            // is defined by the number of shifts of the most significant bit in
            // the mantissa that is required to get a 1 at position 53 (i.e. as
            // if it were a normal number with assumed leading bit)
            final long mantissa = bits & ComplexFunctions.MANTISSA_MASK;
            exp -= Long.numberOfLeadingZeros(mantissa << 12);
        }
        return exp;
    }

    /**
     * Returns the largest unbiased exponent used in the representation of the
     * two numbers. Special cases:
     *
     * <ul>
     * <li>If either argument is NaN or infinite, then the result is
     * {@link Double#MAX_EXPONENT} + 1.
     * <li>If both arguments are zero or subnormal, then the result is
     * {@link Double#MIN_EXPONENT} -1.
     * </ul>
     *
     * <p>This is used by {@link #} as
     * a simple detection that a number may overflow if multiplied
     * by a value in the interval [1, 2).
     *
     * @param a the first value
     * @param b the second value
     * @return The maximum unbiased exponent of the values.
     * @see Math#getExponent(double)
     * @see #(double, double, double, double)
     */
    static int getMaxExponent(double a, double b) {
        // This could return:
        // Math.getExponent(Math.max(Math.abs(a), Math.abs(b)))
        // A speed test is required to determine performance.
        return Math.max(Math.getExponent(a), Math.getExponent(b));
    }

    /**
     * Returns the complex power of this complex number raised to the power of {@code x}.
     * Implements the formula:
     *
     * <p>\[ z^x = e^{x \ln(z)} \]
     *
     * <p>If this complex number is zero then this method returns zero if {@code x} is positive
     * in the real component and zero in the imaginary component;
     * otherwise it returns NaN + iNaN.
     *
     * @param  base the complex number that is to be raised.
     * @param  exp The exponent to which {@code base} is to be raised.
     * @param  constructor constructor
     * @return {@code base} raised to the power of {@code exp}.
     * @see <a href="http://mathworld.wolfram.com/ComplexExponentiation.html">Complex exponentiation</a>
     * @see <a href="http://functions.wolfram.com/ElementaryFunctions/Power/">Power</a>
     */
    public static DComplex pow(DComplex base, DComplex exp, DComplexConstructor<DComplex> constructor) {
        if (base.real() == 0 &&
            base.imag() == 0) {
            // This value is zero. Test the other.
            if (exp.real() > 0 &&
                exp.imag() == 0) {
                // 0 raised to positive number is 0
                return Complex.ZERO;
            }
            // 0 raised to anything else is NaN
            return Complex.NAN;
        }

        final DComplexUnaryOperator log = ComplexFunctions::log;
        final DComplexBinaryOperator logMultiply = log.thenApplyBinaryOperator(ComplexBiFunctions::multiply);
        final DComplexBinaryOperator logMultiplyExp = logMultiply.thenApply(ComplexFunctions::exp);
        return logMultiplyExp.apply(base, exp, constructor);
    }

    /**
     * Returns the complex power of this complex number raised to the power of {@code x},
     * with {@code x} interpreted as a real number.
     * Implements the formula:
     *
     * <p>\[ z^x = e^{x \ln(z)} \]
     *
     * <p>If this complex number is zero then this method returns zero if {@code x} is positive;
     * otherwise it returns NaN + iNaN.
     * @param  base The complex number that is to be raised.
     * @param  exp The exponent to which this complex number is to be raised.
     * @param  constructor Constructor
     * @return {@code base} raised to the power of {@code exp}.
     * @see <a href="http://functions.wolfram.com/ElementaryFunctions/Power/">Power</a>
     */
    public static DComplex pow(DComplex base, double exp, DComplexConstructor<DComplex> constructor) {
        if (base.real() == 0 &&
            base.imag() == 0) {
            // This value is zero. Test the other.
            if (exp > 0) {
                // 0 raised to positive number is 0
                return Complex.ZERO;
            }
            // 0 raised to anything else is NaN
            return Complex.NAN;
        }
        final DComplexUnaryOperator log = ComplexFunctions::log;
        final DComplexScalarFunction logMultiply = log.thenApplyScalarFunction(ComplexBiFunctions::multiply);
        final DComplexScalarFunction logMultiplyExp = logMultiply.thenApply(ComplexFunctions::exp);
        return logMultiplyExp.apply(base, exp, constructor);
    }
}
