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

import java.util.function.DoubleUnaryOperator;

/**
 * Cartesian representation of a complex number. The complex number is expressed
 * in the form \( a + ib \) where \( a \) and \( b \) are real numbers and \( i \)
 * is the imaginary unit which satisfies the equation \( i^2 = -1 \). For the
 * complex number \( a + ib \), \( a \) is called the <em>real part</em> and
 * \( b \) is called the <em>imaginary part</em>.
 *
 * <p>This class is immutable, it is to change from an OO representation of a Complex number
 * to a representation as its real and imaginary parts. All arithmetic will create a new instance for the
 * result.</p>
 *
 * <p>Arithmetic in this class conforms to the C99 standard for complex numbers
 * defined in ISO/IEC 9899, Annex G. Methods have been named using the equivalent
 * method in ISO C99. The behavior for special cases is listed as defined in C99.</p>
 *
 * <p>For functions \( f \) which obey the conjugate equality \( conj(f(z)) = f(conj(z)) \),
 * the specifications for the upper half-plane imply the specifications for the lower
 * half-plane.</p>
 *
 * <p>For functions that are either odd, \( f(z) = -f(-z) \), or even, \( f(z) =  f(-z) \),
 * the specifications for the first quadrant imply the specifications for the other three
 * quadrants.</p>
 *
 * <p>Special cases of <a href="http://mathworld.wolfram.com/BranchCut.html">branch cuts</a>
 * for multivalued functions adopt the principle value convention from C99. Specials cases
 * from C99 that raise the "invalid" or "divide-by-zero"
 * <a href="https://en.cppreference.com/w/c/numeric/fenv/FE_exceptions">floating-point
 * exceptions</a> return the documented value without an explicit mechanism to notify
 * of the exception case, that is no exceptions are thrown during computations in-line with
 * the convention of the corresponding single-valued functions in
 * {@link java.lang.Math java.lang.Math}.
 * These cases are documented in the method special cases as "invalid" or "divide-by-zero"
 * floating-point operation.
 * Note: Invalid floating-point exception cases will result in a complex number where the
 * cardinality of NaN component parts has increased as a real or imaginary part could
 * not be computed and is set to NaN.
 *
 * @see <a href="http://www.open-std.org/JTC1/SC22/WG14/www/standards">
 *    ISO/IEC 9899 - Programming languages - C</a>
 */
public final class ComplexFunctions {

    /** Mask to remove the sign bit from a long. */
    static final long UNSIGN_MASK = 0x7fff_ffff_ffff_ffffL;

    /** Natural logarithm of 2 (ln(2)). */
    private static final double LN_2 = Math.log(2);
    /** {@code 1/2}. */
    private static final double HALF = 0.5;
    /** Base 10 logarithm of 10 divided by 2 (log10(e)/2). */
    private static final double LOG_10E_O_2 = Math.log10(Math.E) / 2;
    /** Base 10 logarithm of 2 (log10(2)). */
    private static final double LOG10_2 = Math.log10(2);
    /** {@code sqrt(2)}. */
    private static final double ROOT2 = 1.4142135623730951;

    /** 54 shifted 20-bits to align with the exponent of the upper 32-bits of a double. */
    private static final int EXP_54 = 0x36_00000;
    /** Represents an exponent of 500 in unbiased form shifted 20-bits to align with the upper 32-bits of a double. */
    private static final int EXP_500 = 0x5f3_00000;
    /** Represents an exponent of 1024 in unbiased form (infinite or nan)
     * shifted 20-bits to align with the upper 32-bits of a double. */
    private static final int EXP_1024 = 0x7ff_00000;
    /** Represents an exponent of -500 in unbiased form shifted 20-bits to align with the upper 32-bits of a double. */
    private static final int EXP_NEG_500 = 0x20b_00000;
    /** 2^600. */
    private static final double TWO_POW_600 = 0x1.0p+600;
    /** 2^-600. */
    private static final double TWO_POW_NEG_600 = 0x1.0p-600;

    /**
     * Private constructor for utility class.
     */
    private ComplexFunctions() {
    }

    /**
     * Returns the absolute value of the complex number.
     * <pre>abs(x + i y) = sqrt(x^2 + y^2)</pre>
     *
     * <p>This should satisfy the special cases of the hypot function in ISO C99 F.9.4.3:
     * "The hypot functions compute the square root of the sum of the squares of x and y,
     * without undue overflow or underflow."
     *
     * <ul>
     * <li>hypot(x, y), hypot(y, x), and hypot(x, −y) are equivalent.
     * <li>hypot(x, ±0) is equivalent to |x|.
     * <li>hypot(±∞, y) returns +∞, even if y is a NaN.
     * </ul>
     *
     * <p>This method is called by all methods that require the absolute value of the complex
     * number, e.g. abs(), sqrt() and log().
     *
     * @param real Real part \( a \) of the complex number \(a +ib \).
     * @param imaginary Imaginary part \( b \) of the complex number \(a +ib \).
     * @return The absolute value.
     */
    public static double abs(double real, double imaginary) {
        // Specialised implementation of hypot.
        // See NUMBERS-143
        return hypot(real, imaginary);
    }

    /**
     * Returns the argument of the complex number.
     *
     * <p>The argument is the angle phi between the positive real axis and
     * the point representing this number in the complex plane.
     * The value returned is between \( -\pi \) (not inclusive)
     * and \( \pi \) (inclusive), with negative values returned for numbers with
     * negative imaginary parts.
     *
     * <p>If either real or imaginary part (or both) is NaN, then the result is NaN.
     * Infinite parts are handled as {@linkplain Math#atan2} handles them,
     * essentially treating finite parts as zero in the presence of an
     * infinite coordinate and returning a multiple of \( \frac{\pi}{4} \) depending on
     * the signs of the infinite parts.
     *
     * <p>This code follows the
     * <a href="http://www.iso-9899.info/wiki/The_Standard">ISO C Standard</a>, Annex G,
     * in calculating the returned value using the {@code atan2(y, x)} method for complex
     * \( x + iy \).
     *
     * @param r Real part \( a \) of the complex number \(a +ib \).
     * @param i Imaginary part \( b \) of the complex number \(a +ib \).
     * @return The argument of the complex number.
     * @see Math#atan2(double, double)
     */
    public static double arg(double r, double i) {
        // Delegate
        return Math.atan2(i, r);
    }

    /**
     * Returns {@code true} if either real or imaginary component of the complex number is infinite.
     *
     * <p>Note: A complex number with at least one infinite part is regarded
     * as an infinity (even if its other part is a NaN).
     * @param real Real part \( a \) of the complex number \(a +ib \).
     * @param imaginary Imaginary part \( b \) of the complex number \(a +ib \).
     * @return {@code true} if the complex number contains an infinite value.
     * @see Double#isInfinite(double)
     */
    public static boolean isInfinite(double real, double imaginary) {
        return Double.isInfinite(real) || Double.isInfinite(imaginary);
    }

    /**
     * Returns the
     * <a href="http://mathworld.wolfram.com/NaturalLogarithm.html">
     * natural logarithm</a> of the complex number using its real and imaginary parts.
     *
     * <p>The natural logarithm of \( z \) is unbounded along the real axis and
     * in the range \( [-\pi, \pi] \) along the imaginary axis. The imaginary part of the
     * natural logarithm has a branch cut along the negative real axis \( (-infty,0] \).
     * Special cases:
     *
     * <ul>
     * <li>{@code z.conj().log() == z.log().conj()}.
     * <li>If {@code z} is −0 + i0, returns −∞ + iπ ("divide-by-zero" floating-point operation).
     * <li>If {@code z} is +0 + i0, returns −∞ + i0 ("divide-by-zero" floating-point operation).
     * <li>If {@code z} is x + i∞ for finite x, returns +∞ + iπ/2.
     * <li>If {@code z} is x + iNaN for finite x, returns NaN + iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is −∞ + iy for finite positive-signed y, returns +∞ + iπ.
     * <li>If {@code z} is +∞ + iy for finite positive-signed y, returns +∞ + i0.
     * <li>If {@code z} is −∞ + i∞, returns +∞ + i3π/4.
     * <li>If {@code z} is +∞ + i∞, returns +∞ + iπ/4.
     * <li>If {@code z} is ±∞ + iNaN, returns +∞ + iNaN.
     * <li>If {@code z} is NaN + iy for finite y, returns NaN + iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is NaN + i∞, returns +∞ + iNaN.
     * <li>If {@code z} is NaN + iNaN, returns NaN + iNaN.
     * </ul>
     *
     * <p>Implements the formula:
     *
     * <p>\[ \ln(z) = \ln |z| + i \arg(z) \]
     *
     * <p>where \( |z| \) is the absolute and \( \arg(z) \) is the argument.
     *
     * <p>The implementation is based on the method described in:</p>
     * <blockquote>
     * T E Hull, Thomas F Fairgrieve and Ping Tak Peter Tang (1994)
     * Implementing complex elementary functions using exception handling.
     * ACM Transactions on Mathematical Software, Vol 20, No 2, pp 215-244.
     * </blockquote>
     *
     * @param real Real part \( a \) of the complex number \(a +ib \).
     * @param imaginary Imaginary part \( b \) of the complex number \(a +ib \).
     * @param constructor Terminating consumer for the complex result, used to construct a result of type {@code R}.
     * @param <R> the object type produced by the supplied constructor.
     * @return The natural logarithm of the complex number.
     * @see Math#log(double)
     * @see <a href="http://functions.wolfram.com/ElementaryFunctions/Log/">Log</a>
     */
    public static <R> R log(double real, double imaginary, ComplexResult<R> constructor) {
        return log(Math::log, HALF, LN_2, real, imaginary, constructor);
    }

    /**
     * Returns the base 10
     * <a href="http://mathworld.wolfram.com/CommonLogarithm.html">
     * common logarithm</a> of the complex number using its real and imaginary parts.
     *
     * <p>The common logarithm of \( z \) is unbounded along the real axis and
     * in the range \( [-\pi, \pi] \) along the imaginary axis. The imaginary part of the
     * common logarithm has a branch cut along the negative real axis \( (-infty,0] \).
     * Special cases are as defined in the log:
     *
     * <p>Implements the formula:
     *
     * <p>\[ \log_{10}(z) = \log_{10} |z| + i \arg(z) \]
     *
     * <p>where \( |z| \) is the absolute and \( \arg(z) \) is the argument.
     *
     * @param real Real part \( a \) of the complex number \(a +ib \).
     * @param imaginary Imaginary part \( b \) of the complex number \(a +ib \).
     * @param constructor Terminating consumer for the complex result, used to construct a result of type {@code R}.
     * @param <R> the object type produced by the supplied constructor.
     * @return The base 10 logarithm of the complex number.
     * @see Math#log10(double)
     */
    public static <R> R log10(double real, double imaginary, ComplexResult<R> constructor) {
        return log(Math::log10, LOG_10E_O_2, LOG10_2, real, imaginary, constructor);
    }

    /**
     * Returns the logarithm of complex number using its real and
     * imaginary parts and the provided function.
     * Implements the formula:
     *
     * <pre>
     *   log(x + i y) = log(|x + i y|) + i arg(x + i y)</pre>
     *
     * <p>Warning: The argument {@code logOf2} must be equal to {@code log(2)} using the
     * provided log function otherwise scaling using powers of 2 in the case of overflow
     * will be incorrect. This is provided as an internal optimisation.
     *
     * @param log Log function.
     * @param logOfeOver2 The log function applied to e, then divided by 2.
     * @param logOf2 The log function applied to 2.
     * @param real Real part \( a \) of the complex number \(a +ib \).
     * @param imaginary Imaginary part \( b \) of the complex number \(a +ib \).
     * @param constructor Terminating consumer for the complex result, used to construct a result of type {@code R}.
     * @param <R> the object type produced by the supplied constructor.
     * @return The logarithm of the complex number.
     */
    private static <R> R log(DoubleUnaryOperator log,
        double logOfeOver2,
        double logOf2,
        double real,
        double imaginary,
        ComplexResult<R> constructor) {

        // Handle NaN
        if (Double.isNaN(real) || Double.isNaN(imaginary)) {
            // Return NaN unless infinite
            if (isInfinite(real, imaginary)) {
                return constructor.apply(Double.POSITIVE_INFINITY, Double.NaN);
            }
            return constructor.apply(Double.NaN, Double.NaN);
        }

        // Returns the real part:
        // log(sqrt(x^2 + y^2))
        // log(x^2 + y^2) / 2

        // Compute with positive values
        double x = Math.abs(real);
        double y = Math.abs(imaginary);

        // Find the larger magnitude.
        if (x < y) {
            final double tmp = x;
            x = y;
            y = tmp;
        }

        if (x == 0) {
            // Handle zero: raises the ‘‘divide-by-zero’’ floating-point exception.
            return constructor.apply(Double.NEGATIVE_INFINITY,
                Complex.negative(real) ? Math.copySign(Math.PI, imaginary) : imaginary);
        }

        double re;

        // This alters the implementation of Hull et al (1994) which used a standard
        // precision representation of |z|: sqrt(x*x + y*y).
        // This formula should use the same definition of the magnitude returned
        // by Complex.abs() which is a high precision computation with scaling.
        // The checks for overflow thus only require ensuring the output of |z|
        // will not overflow or underflow.

        if (x > HALF && x < ROOT2) {
            // x^2+y^2 close to 1. Use log1p(x^2+y^2 - 1) / 2.
            re = Math.log1p(Complex.x2y2m1(x, y)) * logOfeOver2;
        } else {
            // Check for over/underflow in |z|
            // When scaling:
            // log(a / b) = log(a) - log(b)
            // So initialize the result with the log of the scale factor.
            re = 0;
            if (x > Double.MAX_VALUE / 2) {
                // Potential overflow.
                if (Complex.isPosInfinite(x)) {
                    // Handle infinity
                    return constructor.apply(x, arg(real, imaginary));
                }
                // Scale down.
                x /= 2;
                y /= 2;
                // log(2)
                re = logOf2;
            } else if (y < Double.MIN_NORMAL) {
                // Potential underflow.
                if (y == 0) {
                    // Handle real only number
                    return constructor.apply(log.applyAsDouble(x), arg(real, imaginary));
                }
                // Scale up sub-normal numbers to make them normal by scaling by 2^54,
                // i.e. more than the mantissa digits.
                x *= 0x1.0p54;
                y *= 0x1.0p54;
                // log(2^-54) = -54 * log(2)
                re = -54 * logOf2;
            }
            re += log.applyAsDouble(abs(x, y));
        }

        // All ISO C99 edge cases for the imaginary are satisfied by the Math library.
        return constructor.apply(re, arg(real, imaginary));
    }

    /**
     * Returns {@code sqrt(x^2 + y^2)} without intermediate overflow or underflow.
     *
     * <p>Special cases:
     * <ul>
     * <li>If either argument is infinite, then the result is positive infinity.
     * <li>If either argument is NaN and neither argument is infinite, then the result is NaN.
     * </ul>
     *
     * <p>The computed result is expected to be within 1 ulp of the exact result.
     *
     * <p>This method is a replacement for {@link Math#hypot(double, double)}. There
     * will be differences between this method and {@code Math.hypot(double, double)} due
     * to the use of a different algorithm to compute the high precision sum of
     * {@code x^2 + y^2}. This method has been tested to have a lower maximum error from
     * the exact result; any differences are expected to be 1 ULP indicating a rounding
     * change in the sum.
     *
     * <p>JDK9 ported the hypot function to Java for bug JDK-7130085 due to the slow performance
     * of the method as a native function. Benchmarks of the Complex class for functions that
     * use hypot confirm this is slow pre-Java 9. This implementation outperforms the new faster
     * {@code Math.hypot(double, double)} on JDK 11 (LTS). See the Commons numbers examples JMH
     * module for benchmarks. Comparisons with alternative implementations indicate
     * performance gains are related to edge case handling and elimination of an unpredictable
     * branch in the computation of {@code x^2 + y^2}.
     *
     * <p>This port was adapted from the "Freely Distributable Math Library" hypot function.
     * This method only (and not invoked methods within) is distributed under the terms of the
     * original notice as shown below:
     * <pre>
     * ====================================================
     * Copyright (C) 1993 by Sun Microsystems, Inc. All rights reserved.
     *
     * Developed at SunSoft, a Sun Microsystems, Inc. business.
     * Permission to use, copy, modify, and distribute this
     * software is freely granted, provided that this notice
     * is preserved.
     * ====================================================
     * </pre>
     *
     * <p>Note: The fdlibm c code makes use of the language ability to read and write directly
     * to the upper and lower 32-bits of the 64-double. The function performs
     * checking on the upper 32-bits for the magnitude of the two numbers by accessing
     * the exponent and 20 most significant bits of the mantissa. These upper bits
     * are manipulated during scaling and then used to perform extended precision
     * computation of the sum {@code x^2 + y^2} where the high part of the number has 20-bit
     * precision. Manipulation of direct bits has no equivalent in Java
     * other than use of {@link Double#doubleToLongBits(double)} and
     * {@link Double#longBitsToDouble(long)}. To avoid conversion to and from long and double
     * representations this implementation only scales the double representation. The high
     * and low parts of a double for the extended precision computation are extracted
     * using the method of Dekker (1971) to create two 26-bit numbers. This works for sub-normal
     * numbers and reduces the maximum error in comparison to fdlibm hypot which does not
     * use a split number algorithm for sub-normal numbers.
     *
     * @param x Value x
     * @param y Value y
     * @return sqrt(x^2 + y^2)
     * @see Math#hypot(double, double)
     * @see <a href="https://www.netlib.org/fdlibm/e_hypot.c">fdlibm e_hypot.c</a>
     * @see <a href="https://bugs.java.com/bugdatabase/view_bug.do?bug_id=7130085">JDK-7130085 : Port fdlibm hypot to Java</a>
     */
    private static double hypot(double x, double y) {
        // Differences to the fdlibm reference:
        //
        // 1. fdlibm orders the two parts using the magnitude of the upper 32-bits.
        // This incorrectly orders numbers which differ only in the lower 32-bits.
        // This invalidates hypot(x, y) = hypot(y, x) for small sub-normal numbers and a minority
        // of cases of normal numbers. This implementation forces the |x| >= |y| order
        // using the entire 63-bits of the unsigned doubles to ensure the function
        // is commutative.
        //
        // 2. fdlibm computed scaling by directly writing changes to the exponent bits
        // and maintained the high part (ha) during scaling for use in the high
        // precision sum x^2 + y^2. Since exponent scaling cannot be applied to sub-normals
        // the original version dropped the split number representation for sub-normals
        // and can produce maximum errors above 1 ULP for sub-normal numbers.
        // This version uses Dekker's method to split the number. This can be applied to
        // sub-normals and allows dropping the condition to check for sub-normal numbers
        // since all small numbers are handled with a single scaling factor.
        // The effect is increased precision for the majority of sub-normal cases where
        // the implementations compute a different result.
        //
        // 3. An alteration is done here to add an 'else if' instead of a second
        // 'if' statement. Thus you cannot scale down and up at the same time.
        //
        // 4. There is no use of the absolute double value. The magnitude comparison is
        // performed using the long bit representation. The computation x^2+y^2 is
        // insensitive to the sign bit. Thus use of Math.abs(double) is only in edge-case
        // branches.
        //
        // 5. The exponent different to ignore the smaller component has changed from 60 to 54.
        //
        // Original comments from fdlibm are in c style: /* */
        // Extra comments added for reference.
        //
        // Note that the high 32-bits are compared to constants.
        // The lowest 20-bits are the upper bits of the 52-bit mantissa.
        // The next 11-bits are the biased exponent. The sign bit has been cleared.
        // Scaling factors are powers of two for exact scaling.
        // For clarity the values have been refactored to named constants.

        // The mask is used to remove the sign bit.
        final long xbits = Double.doubleToRawLongBits(x) & UNSIGN_MASK;
        final long ybits = Double.doubleToRawLongBits(y) & UNSIGN_MASK;

        // Order by magnitude: |a| >= |b|
        double a;
        double b;
        /* High word of x & y */
        int ha;
        int hb;
        if (ybits > xbits) {
            a = y;
            b = x;
            ha = (int) (ybits >>> 32);
            hb = (int) (xbits >>> 32);
        } else {
            a = x;
            b = y;
            ha = (int) (xbits >>> 32);
            hb = (int) (ybits >>> 32);
        }

        // Check if the smaller part is significant.
        // a^2 is computed in extended precision for an effective mantissa of 106-bits.
        // An exponent difference of 54 is where b^2 will not overlap a^2.
        if ((ha - hb) > EXP_54) {
            /* a/b > 2**54 */
            // or a is Inf or NaN.
            // No addition of a + b for sNaN.
            return Math.abs(a);
        }

        double rescale = 1.0;
        if (ha > EXP_500) {
            /* a > 2^500 */
            if (ha >= EXP_1024) {
                /* Inf or NaN */
                // Check b is infinite for the IEEE754 result.
                // No addition of a + b for sNaN.
                return Math.abs(b) == Double.POSITIVE_INFINITY ?
                    Double.POSITIVE_INFINITY :
                    Math.abs(a);
            }
            /* scale a and b by 2^-600 */
            // Before scaling: a in [2^500, 2^1023].
            // After scaling: a in [2^-100, 2^423].
            // After scaling: b in [2^-154, 2^423].
            a *= TWO_POW_NEG_600;
            b *= TWO_POW_NEG_600;
            rescale = TWO_POW_600;
        } else if (hb < EXP_NEG_500) {
            // No special handling of sub-normals.
            // These do not matter when we do not manipulate the exponent bits
            // for scaling the split representation.

            // Intentional comparison with zero.
            if (b == 0) {
                return Math.abs(a);
            }

            /* scale a and b by 2^600 */
            // Effective min exponent of a sub-normal = -1022 - 52 = -1074.
            // Before scaling: b in [2^-1074, 2^-501].
            // After scaling: b in [2^-474, 2^99].
            // After scaling: a in [2^-474, 2^153].
            a *= TWO_POW_600;
            b *= TWO_POW_600;
            rescale = TWO_POW_NEG_600;
        }

        // High precision x^2 + y^2
        return Math.sqrt(x2y2(a, b)) * rescale;
    }

    /**
     * Return {@code x^2 + y^2} with high accuracy.
     *
     * <p>It is assumed that {@code 2^500 > |x| >= |y| > 2^-500}. Thus there will be no
     * overflow or underflow of the result. The inputs are not assumed to be unsigned.
     *
     * <p>The computation is performed using Dekker's method for extended precision
     * multiplication of x and y and then summation of the extended precision squares.
     *
     * @param x Value x.
     * @param y Value y
     * @return x^2 + y^2
     * @see <a href="https://doi.org/10.1007/BF01397083">
     * Dekker (1971) A floating-point technique for extending the available precision</a>
     */
    private static double x2y2(double x, double y) {
        // Note:
        // This method is different from the high-accuracy summation used in fdlibm for hypot.
        // The summation could be any valid computation of x^2+y^2. However since this follows
        // the re-scaling logic in hypot(x, y) the use of high precision has relatively
        // less performance overhead than if used without scaling.
        // The Dekker algorithm is branchless for better performance
        // than the fdlibm method with a maximum ULP error of approximately 0.86.
        //
        // See NUMBERS-143 for analysis.

        // Do a Dekker summation of double length products x*x and y*y
        // (10 multiply and 20 additions).
        final double xx = x * x;
        final double yy = y * y;
        // Compute the round-off from the products.
        // With FMA hardware support in JDK 9+ this can be replaced with the much faster:
        // xxLow = Math.fma(x, x, -xx)
        // yyLow = Math.fma(y, y, -yy)
        // Dekker mul12
        final double xHigh = Complex.splitHigh(x);
        final double xLow = x - xHigh;
        final double xxLow = Complex.squareLow(xLow, xHigh, xx);
        // Dekker mul12
        final double yHigh = Complex.splitHigh(y);
        final double yLow = y - yHigh;
        final double yyLow = Complex.squareLow(yLow, yHigh, yy);
        // Dekker add2
        final double r = xx + yy;
        // Note: The order is important. Assume xx > yy and drop Dekker's conditional
        // check for which is the greater magnitude.
        // s = xx - r + yy + yyLow + xxLow
        // z = r + s
        // zz = r - z + s
        // Here we compute z inline and ignore computing the round-off zz.
        // Note: The round-off could be used with Dekker's sqrt2 method.
        // That adds 7 multiply, 1 division and 19 additions doubling the cost
        // and reducing error to < 0.5 ulp for the final sqrt.
        return xx - r + yy + yyLow + xxLow + r;
    }
}
