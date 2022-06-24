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

public final class ComplexFunctions {

    /**
     * A complex number representing zero.
     *
     * <p>\( (0 + i 0) \).
     */
    public static final Complex ZERO = Complex.ZERO;

    /** A complex number representing {@code NaN + i NaN}. */
    public static final DComplex NAN = Complex.NAN;


    /** The bit representation of {@code -0.0}. */
    static final long NEGATIVE_ZERO_LONG_BITS = Double.doubleToLongBits(-0.0);
    /** Exponent offset in IEEE754 representation. */
    static final int EXPONENT_OFFSET = 1023;

    /** Mask to remove the sign bit from a long. */
    static final long UNSIGN_MASK = 0x7fff_ffff_ffff_ffffL;
    /** Mask to extract the 52-bit mantissa from a long representation of a double. */
    static final long MANTISSA_MASK = 0x000f_ffff_ffff_ffffL;

    /** &pi;/2. */
    private static final double PI_OVER_2 = 0.5 * Math.PI;
    /** &pi;/4. */
    private static final double PI_OVER_4 = 0.25 * Math.PI;
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
    /** {@code 1.0 / sqrt(2)}.
     * This is pre-computed to the closest double from the exact result.
     * It is 1 ULP different from 1.0 / Math.sqrt(2) but equal to Math.sqrt(2) / 2.
     */
    private static final double ONE_OVER_ROOT2 = 0.7071067811865476;

    /**
     * Largest double-precision floating-point number such that
     * {@code 1 + EPSILON} is numerically equal to 1. This value is an upper
     * bound on the relative error due to rounding real numbers to double
     * precision floating-point numbers.
     *
     * <p>In IEEE 754 arithmetic, this is 2<sup>-53</sup>.
     * Copied from o.a.c.numbers.Precision.
     *
     * @see <a href="http://en.wikipedia.org/wiki/Machine_epsilon">Machine epsilon</a>
     */
    private static final double EPSILON = Double.longBitsToDouble((EXPONENT_OFFSET - 53L) << 52);

    /** The multiplier used to split the double value into hi and low parts. This must be odd
     * and a value of 2^s + 1 in the range {@code p/2 <= s <= p-1} where p is the number of
     * bits of precision of the floating point number. Here {@code s = 27}.*/
    private static final double MULTIPLIER = 1.34217729E8;

    /**
     * Crossover point to switch computation for asin/acos factor A.
     * This has been updated from the 1.5 value used by Hull et al to 10
     * as used in boost::math::complex.
     * @see <a href="https://svn.boost.org/trac/boost/ticket/7290">Boost ticket 7290</a>
     */
    private static final double A_CROSSOVER = 10.0;
    /** Crossover point to switch computation for asin/acos factor B. */
    private static final double B_CROSSOVER = 0.6471;
    /**
     * The safe maximum double value {@code x} to avoid loss of precision in asin/acos.
     * Equal to sqrt(M) / 8 in Hull, et al (1997) with M the largest normalised floating-point value.
     */
    private static final double SAFE_MAX = Math.sqrt(Double.MAX_VALUE) / 8;
    /**
     * The safe minimum double value {@code x} to avoid loss of precision/underflow in asin/acos.
     * Equal to sqrt(u) * 4 in Hull, et al (1997) with u the smallest normalised floating-point value.
     */
    private static final double SAFE_MIN = Math.sqrt(Double.MIN_NORMAL) * 4;
    /**
     * The safe maximum double value {@code x} to avoid loss of precision in atanh.
     * Equal to sqrt(M) / 2 with M the largest normalised floating-point value.
     */
    private static final double SAFE_UPPER = Math.sqrt(Double.MAX_VALUE) / 2;
    /**
     * The safe minimum double value {@code x} to avoid loss of precision/underflow in atanh.
     * Equal to sqrt(u) * 2 with u the smallest normalised floating-point value.
     */
    private static final double SAFE_LOWER = Math.sqrt(Double.MIN_NORMAL) * 2;
    /** The safe maximum double value {@code x} to avoid overflow in sqrt. */
    private static final double SQRT_SAFE_UPPER = Double.MAX_VALUE / 8;
    /**
     * A safe maximum double value {@code m} where {@code e^m} is not infinite.
     * This can be used when functions require approximations of sinh(x) or cosh(x)
     * when x is large using exp(x):
     * <pre>
     * sinh(x) = (e^x - e^-x) / 2 = sign(x) * e^|x| / 2
     * cosh(x) = (e^x + e^-x) / 2 = e^|x| / 2 </pre>
     *
     * <p>This value can be used to approximate e^x using a product:
     *
     * <pre>
     * e^x = product_n (e^m) * e^(x-nm)
     * n = (int) x/m
     * e.g. e^2000 = e^m * e^m * e^(2000 - 2m) </pre>
     *
     * <p>The value should be below ln(max_value) ~ 709.783.
     * The value m is set to an integer for less error when subtracting m and chosen as
     * even (m=708) as it is used as a threshold in tanh with m/2.
     *
     * <p>The value is used to compute e^x multiplied by a small number avoiding
     * overflow (sinh/cosh) or a small number divided by e^x without underflow due to
     * infinite e^x (tanh). The following conditions are used:
     * <pre>
     * 0.5 * e^m * Double.MIN_VALUE * e^m * e^m = Infinity
     * 2.0 / e^m / e^m = 0.0 </pre>
     */
    private static final double SAFE_EXP = 708;
    /**
     * The value of Math.exp(SAFE_EXP): e^708.
     * To be used in overflow/underflow safe products of e^m to approximate e^x where x > m.
     */
    private static final double EXP_M = Math.exp(SAFE_EXP);

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

    private ComplexFunctions() {
    }

    public static DComplex negate(double r, double i, DComplexConstructor<DComplex> result) {
        return result.apply(-r, -i);
    }
    public static DComplex negate(DComplex c, DComplexConstructor<DComplex> result) {
        return negate(c.getReal(), c.getImaginary(), result);
    }
    public static DComplex multiplyImaginary(double r, double i, DComplexConstructor<DComplex> result) {
        return result.apply(-1 * i, r);
    }
    public static DComplex multiplyImaginary(DComplex c, DComplexConstructor<DComplex> result) {
        return multiplyImaginary(c.getReal(), c.getImaginary(), result);
    }
    /**
     * Returns {@code true} if either real or imaginary component of the complex number is infinite.
     *
     * <p>Note: A complex number with at least one infinite part is regarded
     * as an infinity (even if its other part is a NaN).
     * @param real
     * @param imaginary
     * @return {@code true} if this instance contains an infinite value.
     * @see Double#isInfinite(double)
     */
    private static boolean isInfinite(double real, double imaginary) {
        return Double.isInfinite(real) || Double.isInfinite(imaginary);
    }
    public static boolean isInfinite(DComplex c) {
        return isInfinite(c.getReal(), c.getImaginary());
    }

    public static DComplex proj(DComplex c, DComplexConstructor<DComplex> result) {
        if (isInfinite(c)) {
            return result.apply(Double.POSITIVE_INFINITY, Math.copySign(0.0, c.getImaginary()));
        }
        return c;
    }
    public static double abs(double real, double imaginary) {
        // Specialised implementation of hypot.
        // See NUMBERS-143
        return hypot(real, imaginary);
    }
    public static double arg(double r, double i) {
        // Delegate
        return Math.atan2(i, r);
    }

    public static double norm(double real, double imaginary) {
        if (isInfinite(real, imaginary)) {
            return Double.POSITIVE_INFINITY;
        }
        return real * real + imaginary * imaginary;
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
    public static double hypot(double x, double y) {
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
        final double xHigh = splitHigh(x);
        final double xLow = x - xHigh;
        final double xxLow = squareLow(xLow, xHigh, xx);
        // Dekker mul12
        final double yHigh = splitHigh(y);
        final double yLow = y - yHigh;
        final double yyLow = squareLow(yLow, yHigh, yy);
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

    public static DComplex exp(double r, double i, DComplexConstructor<DComplex> result) {
        if (Double.isInfinite(r)) {
            // Set the scale factor applied to cis(y)
            double zeroOrInf;
            if (r < 0) {
                if (!Double.isFinite(i)) {
                    // (−∞ + i∞) or (−∞ + iNaN) returns (±0 ± i0) (where the signs of the
                    // real and imaginary parts of the result are unspecified).
                    // Here we preserve the conjugate equality.
                    return result.apply(0, Math.copySign(0, i));
                }
                // (−∞ + iy) returns +0 cis(y), for finite y
                zeroOrInf = 0;
            } else {
                // (+∞ + i0) returns +∞ + i0.
                if (i == 0) {
                    return result.apply(r, i);
                }
                // (+∞ + i∞) or (+∞ + iNaN) returns (±∞ + iNaN) and raises the invalid
                // floating-point exception (where the sign of the real part of the
                // result is unspecified).
                if (!Double.isFinite(i)) {
                    return result.apply(r, Double.NaN);
                }
                // (+∞ + iy) returns (+∞ cis(y)), for finite nonzero y.
                zeroOrInf = r;
            }
            return result.apply(zeroOrInf * Math.cos(i),
                zeroOrInf * Math.sin(i));

        } else if (Double.isNaN(r)) {
            // (NaN + i0) returns (NaN + i0)
            // (NaN + iy) returns (NaN + iNaN) and optionally raises the invalid floating-point exception
            // (NaN + iNaN) returns (NaN + iNaN)
            if (i == 0) {
                return result.apply(r, i);
            } else {
                return result.apply(Double.NaN, Double.NaN);
            }
        } else if (!Double.isFinite(i)) {
            // (x + i∞) or (x + iNaN) returns (NaN + iNaN) and raises the invalid
            // floating-point exception, for finite x.
            return result.apply(Double.NaN, Double.NaN);
        }
        // real and imaginary are finite.
        // Compute e^a * (cos(b) + i sin(b)).

        // Special case:
        // (±0 + i0) returns (1 + i0)
        final double exp = Math.exp(r);
        if (i == 0) {
            return result.apply(exp, i);
        }
        return result.apply(exp * Math.cos(i),
            exp * Math.sin(i));

    }
    public static DComplex exp(DComplex c, DComplexConstructor<DComplex> result) {
        return exp(c.getReal(), c.getImaginary(), result);
    }

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
    static boolean negative(double d) {
        return d < 0 || Double.doubleToLongBits(d) == NEGATIVE_ZERO_LONG_BITS;
    }

    /**
     * Change the sign of the magnitude based on the signed value.
     *
     * <p>If the signed value is negative then the result is {@code -magnitude}; otherwise
     * return {@code magnitude}.
     *
     * <p>A signed value of {@code -0.0} is treated as negative. A signed value of {@code NaN}
     * is treated as positive.
     *
     * <p>This is not the same as {@link Math#copySign(double, double)} as this method
     * will change the sign based on the signed value rather than copy the sign.
     *
     * @param magnitude the magnitude
     * @param signedValue the signed value
     * @return magnitude or -magnitude.
     * @see #negative(double)
     */
    private static double changeSign(double magnitude, double signedValue) {
        return negative(signedValue) ? -magnitude : magnitude;
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
     * Checks if both x and y are in the region defined by the minimum and maximum.
     *
     * @param x x value.
     * @param y y value.
     * @param min the minimum (exclusive).
     * @param max the maximum (exclusive).
     * @return true if inside the region.
     */
    private static boolean inRegion(double x, double y, double min, double max) {
        return (x < max) && (x > min) && (y < max) && (y > min);
    }

    /**
     * Check that a value is positive infinity. Used to replace {@link Double#isInfinite()}
     * when the input value is known to be positive (i.e. in the case where it has been
     * set using {@link Math#abs(double)}).
     *
     * @param d Value.
     * @return {@code true} if {@code d} is +inf.
     */
    private static boolean isPosInfinite(double d) {
        return d == Double.POSITIVE_INFINITY;
    }

    public static DComplex asin(final double real, final double imaginary,
                                DComplexConstructor<DComplex> result) {
        // Compute with positive values and determine sign at the end
        final double x = Math.abs(real);
        final double y = Math.abs(imaginary);
        // The result (without sign correction)
        double re;
        double im;

        // Handle C99 special cases
        if (Double.isNaN(x)) {
            if (isPosInfinite(y)) {
                re = x;
                im = y;
            } else {
                // No-use of the input constructor
                return NAN;
            }
        } else if (Double.isNaN(y)) {
            if (x == 0) {
                re = 0;
                im = y;
            } else if (isPosInfinite(x)) {
                re = y;
                im = x;
            } else {
                // No-use of the input constructor
                return NAN;
            }
        } else if (isPosInfinite(x)) {
            re = isPosInfinite(y) ? PI_OVER_4 : PI_OVER_2;
            im = x;
        } else if (isPosInfinite(y)) {
            re = 0;
            im = y;
        } else {
            // Special case for real numbers:
            if (y == 0 && x <= 1) {
                return result.apply(Math.asin(real), imaginary);
            }

            final double xp1 = x + 1;
            final double xm1 = x - 1;

            if (inRegion(x, y, SAFE_MIN, SAFE_MAX)) {
                final double yy = y * y;
                final double r = Math.sqrt(xp1 * xp1 + yy);
                final double s = Math.sqrt(xm1 * xm1 + yy);
                final double a = 0.5 * (r + s);
                final double b = x / a;

                if (b <= B_CROSSOVER) {
                    re = Math.asin(b);
                } else {
                    final double apx = a + x;
                    if (x <= 1) {
                        re = Math.atan(x / Math.sqrt(0.5 * apx * (yy / (r + xp1) + (s - xm1))));
                    } else {
                        re = Math.atan(x / (y * Math.sqrt(0.5 * (apx / (r + xp1) + apx / (s + xm1)))));
                    }
                }

                if (a <= A_CROSSOVER) {
                    double am1;
                    if (x < 1) {
                        am1 = 0.5 * (yy / (r + xp1) + yy / (s - xm1));
                    } else {
                        am1 = 0.5 * (yy / (r + xp1) + (s + xm1));
                    }
                    im = Math.log1p(am1 + Math.sqrt(am1 * (a + 1)));
                } else {
                    im = Math.log(a + Math.sqrt(a * a - 1));
                }
            } else {
                // Hull et al: Exception handling code from figure 4
                if (y <= (EPSILON * Math.abs(xm1))) {
                    if (x < 1) {
                        re = Math.asin(x);
                        im = y / Math.sqrt(xp1 * (1 - x));
                    } else {
                        re = PI_OVER_2;
                        if ((Double.MAX_VALUE / xp1) > xm1) {
                            // xp1 * xm1 won't overflow:
                            im = Math.log1p(xm1 + Math.sqrt(xp1 * xm1));
                        } else {
                            im = LN_2 + Math.log(x);
                        }
                    }
                } else if (y <= SAFE_MIN) {
                    // Hull et al: Assume x == 1.
                    // True if:
                    // E^2 > 8*sqrt(u)
                    //
                    // E = Machine epsilon: (1 + epsilon) = 1
                    // u = Double.MIN_NORMAL
                    re = PI_OVER_2 - Math.sqrt(y);
                    im = Math.sqrt(y);
                } else if (EPSILON * y - 1 >= x) {
                    // Possible underflow:
                    re = x / y;
                    im = LN_2 + Math.log(y);
                } else if (x > 1) {
                    re = Math.atan(x / y);
                    final double xoy = x / y;
                    im = LN_2 + Math.log(y) + 0.5 * Math.log1p(xoy * xoy);
                } else {
                    final double a = Math.sqrt(1 + y * y);
                    // Possible underflow:
                    re = x / a;
                    im = 0.5 * Math.log1p(2 * y * (y + a));
                }
            }
        }

        return result.apply(changeSign(re, real),
            changeSign(im, imaginary));
    }
    public static DComplex asin(final DComplex c, DComplexConstructor<DComplex> result) {
        return asin(c.getReal(), c.getImaginary(), result);
    }

    public static DComplex acos(final double real, final double imaginary,
                                final DComplexConstructor<DComplex> result) {
        // Compute with positive values and determine sign at the end
        final double x = Math.abs(real);
        final double y = Math.abs(imaginary);
        // The result (without sign correction)
        double re;
        double im;

        // Handle C99 special cases
        if (isPosInfinite(x)) {
            if (isPosInfinite(y)) {
                re = PI_OVER_4;
                im = y;
            } else if (Double.isNaN(y)) {
                // sign of the imaginary part of the result is unspecified
                return result.apply(imaginary, real);
            } else {
                re = 0;
                im = Double.POSITIVE_INFINITY;
            }
        } else if (Double.isNaN(x)) {
            if (isPosInfinite(y)) {
                return result.apply(x, -imaginary);
            }
            // No-use of the input constructor
            return NAN;
        } else if (isPosInfinite(y)) {
            re = PI_OVER_2;
            im = y;
        } else if (Double.isNaN(y)) {
            return result.apply(x == 0 ? PI_OVER_2 : y, y);
        } else {
            // Special case for real numbers:
            if (y == 0 && x <= 1) {
                return result.apply(x == 0 ? PI_OVER_2 : Math.acos(real), -imaginary);
            }

            final double xp1 = x + 1;
            final double xm1 = x - 1;

            if (inRegion(x, y, SAFE_MIN, SAFE_MAX)) {
                final double yy = y * y;
                final double r = Math.sqrt(xp1 * xp1 + yy);
                final double s = Math.sqrt(xm1 * xm1 + yy);
                final double a = 0.5 * (r + s);
                final double b = x / a;

                if (b <= B_CROSSOVER) {
                    re = Math.acos(b);
                } else {
                    final double apx = a + x;
                    if (x <= 1) {
                        re = Math.atan(Math.sqrt(0.5 * apx * (yy / (r + xp1) + (s - xm1))) / x);
                    } else {
                        re = Math.atan((y * Math.sqrt(0.5 * (apx / (r + xp1) + apx / (s + xm1)))) / x);
                    }
                }

                if (a <= A_CROSSOVER) {
                    double am1;
                    if (x < 1) {
                        am1 = 0.5 * (yy / (r + xp1) + yy / (s - xm1));
                    } else {
                        am1 = 0.5 * (yy / (r + xp1) + (s + xm1));
                    }
                    im = Math.log1p(am1 + Math.sqrt(am1 * (a + 1)));
                } else {
                    im = Math.log(a + Math.sqrt(a * a - 1));
                }
            } else {
                // Hull et al: Exception handling code from figure 6
                if (y <= (EPSILON * Math.abs(xm1))) {
                    if (x < 1) {
                        re = Math.acos(x);
                        im = y / Math.sqrt(xp1 * (1 - x));
                    } else {
                        // This deviates from Hull et al's paper as per
                        // https://svn.boost.org/trac/boost/ticket/7290
                        if ((Double.MAX_VALUE / xp1) > xm1) {
                            // xp1 * xm1 won't overflow:
                            re = y / Math.sqrt(xm1 * xp1);
                            im = Math.log1p(xm1 + Math.sqrt(xp1 * xm1));
                        } else {
                            re = y / x;
                            im = LN_2 + Math.log(x);
                        }
                    }
                } else if (y <= SAFE_MIN) {
                    // Hull et al: Assume x == 1.
                    // True if:
                    // E^2 > 8*sqrt(u)
                    //
                    // E = Machine epsilon: (1 + epsilon) = 1
                    // u = Double.MIN_NORMAL
                    re = Math.sqrt(y);
                    im = Math.sqrt(y);
                } else if (EPSILON * y - 1 >= x) {
                    re = PI_OVER_2;
                    im = LN_2 + Math.log(y);
                } else if (x > 1) {
                    re = Math.atan(y / x);
                    final double xoy = x / y;
                    im = LN_2 + Math.log(y) + 0.5 * Math.log1p(xoy * xoy);
                } else {
                    re = PI_OVER_2;
                    final double a = Math.sqrt(1 + y * y);
                    im = 0.5 * Math.log1p(2 * y * (y + a));
                }
            }
        }

        return result.apply(negative(real) ? Math.PI - re : re,
            negative(imaginary) ? im : -im);
    }
    public static DComplex acos(final DComplex c,
                                final DComplexConstructor<DComplex> result) {
        return acos(c.getReal(), c.getImaginary(), result);
    }
    public static DComplex acosh(double r, double i, DComplexConstructor<DComplex> result) {
        // Define in terms of acos
        // acosh(z) = +-i acos(z)
        // Note the special case:
        // acos(+-0 + iNaN) = π/2 + iNaN
        // acosh(0 + iNaN) = NaN + iπ/2
        // will not appropriately multiply by I to maintain positive imaginary if
        // acos() imaginary computes as NaN. So do this explicitly.
        if (Double.isNaN(i) && r == 0) {
            return result.apply(Double.NaN, PI_OVER_2);
        }
        return acos(r, i, (re, im) ->
            // Set the sign appropriately for real >= 0
            (negative(im)) ?
                // Multiply by I
                result.apply(-im, re) :
                // Multiply by -I
                result.apply(im, -re)
        );
    }
    public static DComplex acosh(DComplex c, DComplexConstructor<DComplex> result) {
        return acosh(c.getReal(), c.getImaginary(), result);
    }

    public static DComplex atanh(final double r, final double i,
                                 final DComplexConstructor<DComplex> result) {
        // Compute with positive values and determine sign at the end
        double x = Math.abs(r);
        double y = Math.abs(i);
        // The result (without sign correction)
        double re;
        double im;
        // Handle C99 special cases
        if (Double.isNaN(x)) {
            if (isPosInfinite(y)) {
                // The sign of the real part of the result is unspecified
                return result.apply(0, Math.copySign(PI_OVER_2, i));
            }
            // No-use of the input constructor.
            // Optionally raises the ‘‘invalid’’ floating-point exception, for finite y.
            return NAN;
        } else if (Double.isNaN(y)) {
            if (isPosInfinite(x)) {
                return result.apply(Math.copySign(0, r), Double.NaN);
            }
            if (x == 0) {
                return result.apply(r, Double.NaN);
            }
            // No-use of the input constructor
            return NAN;
        } else {
            // x && y are finite or infinite. Check the safe region.
            // The lower and upper bounds have been copied from boost::math::atanh.
            // They are different from the safe region for asin and acos.
            // x >= SAFE_UPPER: (1-x) == -x
            // x <= SAFE_LOWER: 1 - x^2 = 1
            if (inRegion(x, y, SAFE_LOWER, SAFE_UPPER)) {
                // Normal computation within a safe region.
                // minus x plus 1: (-x+1)
                final double mxp1 = 1 - x;
                final double yy = y * y;
                // The definition of real component is:
                // real = log( ((x+1)^2+y^2) / ((1-x)^2+y^2) ) / 4
                // This simplifies by adding 1 and subtracting 1 as a fraction:
                //      = log(1 + ((x+1)^2+y^2) / ((1-x)^2+y^2) - ((1-x)^2+y^2)/((1-x)^2+y^2) ) / 4
                // real(atanh(z)) == log(1 + 4*x / ((1-x)^2+y^2)) / 4
                // imag(atanh(z)) == tan^-1 (2y, (1-x)(1+x) - y^2) / 2
                // imag(atanh(z)) == tan^-1 (2y, (1 - x^2 - y^2) / 2
                // The division is done at the end of the function.
                re = Math.log1p(4 * x / (mxp1 * mxp1 + yy));
                // Modified from boost which does not switch the magnitude of x and y.
                // The denominator for atan2 is 1 - x^2 - y^2. This can be made more precise if |x| > |y|.
                final double numerator = 2 * y;
                double denominator;
                if (x < y) {
                    final double tmp = x;
                    x = y;
                    y = tmp;
                }
                // 1 - x is precise if |x| >= 1
                if (x >= 1) {
                    denominator = (1 - x) * (1 + x) - y * y;
                } else {
                    // |x| < 1: Use high precision if possible: 1 - x^2 - y^2 = -(x^2 + y^2 - 1)
                    // Modified from boost to use the custom high precision method.
                    denominator = -x2y2m1(x, y);
                }
                im = Math.atan2(numerator, denominator);
            } else {
                // This section handles exception cases that would normally cause
                // underflow or overflow in the main formulas.
                // C99. G.7: Special case for imaginary only numbers
                if (x == 0) {
                    if (i == 0) {
                        return result.apply(r, i);
                    }
                    // atanh(iy) = i atan(y)
                    return result.apply(r, Math.atan(i));
                }
                // Real part:
                // real = Math.log1p(4x / ((1-x)^2 + y^2))
                // real = Math.log1p(4x / (1 - 2x + x^2 + y^2))
                // real = Math.log1p(4x / (1 + x(x-2) + y^2))
                // without either overflow or underflow in the squared terms.
                if (x >= SAFE_UPPER) {
                    // (1-x) = -x to machine precision:
                    // log1p(4x / (x^2 + y^2))
                    if (isPosInfinite(x) || isPosInfinite(y)) {
                        re = 0;
                    } else if (y >= SAFE_UPPER) {
                        // Big x and y: divide by x*y
                        re = Math.log1p((4 / y) / (x / y + y / x));
                    } else if (y > 1) {
                        // Big x: divide through by x:
                        re = Math.log1p(4 / (x + y * y / x));
                    } else {
                        // Big x small y, as above but neglect y^2/x:
                        re = Math.log1p(4 / x);
                    }
                } else if (y >= SAFE_UPPER) {
                    if (x > 1) {
                        // Big y, medium x, divide through by y:
                        final double mxp1 = 1 - x;
                        re = Math.log1p((4 * x / y) / (mxp1 * mxp1 / y + y));
                    } else {
                        // Big y, small x, as above but neglect (1-x)^2/y:
                        // Note: log1p(v) == v - v^2/2 + v^3/3 ... Taylor series when v is small.
                        // Here v is so small only the first term matters.
                        re = 4 * x / y / y;
                    }
                } else if (x == 1) {
                    // x = 1, small y: Special case when x == 1 as (1-x) is invalid.
                    // Simplify the following formula:
                    // real = log( sqrt((x+1)^2+y^2) ) / 2 - log( sqrt((1-x)^2+y^2) ) / 2
                    //      = log( sqrt(4+y^2) ) / 2 - log(y) / 2
                    // if: 4+y^2 -> 4
                    //      = log( 2 ) / 2 - log(y) / 2
                    //      = (log(2) - log(y)) / 2
                    // Multiply by 2 as it will be divided by 4 at the end.
                    // C99: if y=0 raises the ‘‘divide-by-zero’’ floating-point exception.
                    re = 2 * (LN_2 - Math.log(y));
                } else {
                    // Modified from boost which checks y > SAFE_LOWER.
                    // if y*y -> 0 it will be ignored so always include it.
                    final double mxp1 = 1 - x;
                    re = Math.log1p((4 * x) / (mxp1 * mxp1 + y * y));
                }
                // Imaginary part:
                // imag = atan2(2y, (1-x)(1+x) - y^2)
                // if x or y are large, then the formula: atan2(2y, (1-x)(1+x) - y^2)
                // evaluates to +(PI - theta) where theta is negligible compared to PI.
                if ((x >= SAFE_UPPER) || (y >= SAFE_UPPER)) {
                    im = Math.PI;
                } else if (x <= SAFE_LOWER) {
                    // (1-x)^2 -> 1
                    if (y <= SAFE_LOWER) {
                        // 1 - y^2 -> 1
                        im = Math.atan2(2 * y, 1);
                    } else {
                        im = Math.atan2(2 * y, 1 - y * y);
                    }
                } else {
                    // Medium x, small y.
                    // Modified from boost which checks (y == 0) && (x == 1) and sets re = 0.
                    // This is same as the result from calling atan2(0, 0) so exclude this case.
                    // 1 - y^2 = 1 so ignore subtracting y^2
                    im = Math.atan2(2 * y, (1 - x) * (1 + x));
                }
            }
        }
        re /= 4;
        im /= 2;
        return result.apply(changeSign(re, r),
            changeSign(im, i));
    }
    public static DComplex atanh(final DComplex c,
                                 final DComplexConstructor<DComplex> result) {
        return atanh(c.getReal(), c.getImaginary(), result);
    }
    public static DComplex conj(double r, double i, DComplexConstructor<DComplex> result) {
        return result.apply(r, -i);
    }
    public static DComplex conjComplex(DComplex c, DComplexConstructor<DComplex> result) {
        return conj(c.getReal(), c.getImaginary(), result);
    }


    /**
     * Returns the square root of the complex number {@code sqrt(x + i y)}.
     *
     * @param real Real component.
     * @param imaginary Imaginary component.
     * @param result ComplexConstructor
     * @return The square root of the complex number.
     */
    public static DComplex sqrt(double real, double imaginary, DComplexConstructor<DComplex> result) {
        // Handle NaN
        if (Double.isNaN(real) || Double.isNaN(imaginary)) {
            // Check for infinite
            if (Double.isInfinite(imaginary)) {
                return result.apply(Double.POSITIVE_INFINITY, imaginary);
            }
            if (Double.isInfinite(real)) {
                if (real == Double.NEGATIVE_INFINITY) {
                    return result.apply(Double.NaN, Math.copySign(Double.POSITIVE_INFINITY, imaginary));
                }
                return result.apply(Double.POSITIVE_INFINITY, Double.NaN);
            }
            return result.apply(Double.NaN, Double.NaN);
        }

        // Compute with positive values and determine sign at the end
        final double x = Math.abs(real);
        final double y = Math.abs(imaginary);

        // Compute
        double t;

        // This alters the implementation of Hull et al (1994) which used a standard
        // precision representation of |z|: sqrt(x*x + y*y).
        // This formula should use the same definition of the magnitude returned
        // by Complex.abs() which is a high precision computation with scaling.
        // Worry about overflow if 2 * (|z| + |x|) will overflow.
        // Worry about underflow if |z| or |x| are sub-normal components.

        if (inRegion(x, y, Double.MIN_NORMAL, SQRT_SAFE_UPPER)) {
            // No over/underflow
            t = Math.sqrt(2 * (abs(x, y) + x));
        } else {
            // Potential over/underflow. First check infinites and real/imaginary only.

            // Check for infinite
            if (isPosInfinite(y)) {
                return result.apply(Double.POSITIVE_INFINITY, imaginary);
            } else if (isPosInfinite(x)) {
                if (real == Double.NEGATIVE_INFINITY) {
                    return result.apply(0, Math.copySign(Double.POSITIVE_INFINITY, imaginary));
                }
                return result.apply(Double.POSITIVE_INFINITY, Math.copySign(0, imaginary));
            } else if (y == 0) {
                // Real only
                final double sqrtAbs = Math.sqrt(x);
                if (real < 0) {
                    return result.apply(0, Math.copySign(sqrtAbs, imaginary));
                }
                return result.apply(sqrtAbs, imaginary);
            } else if (x == 0) {
                // Imaginary only. This sets the two components to the same magnitude.
                // Note: In polar coordinates this does not happen:
                // real = sqrt(abs()) * Math.cos(arg() / 2)
                // imag = sqrt(abs()) * Math.sin(arg() / 2)
                // arg() / 2 = pi/4 and cos and sin should both return sqrt(2)/2 but
                // are different by 1 ULP.
                final double sqrtAbs = Math.sqrt(y) * ONE_OVER_ROOT2;
                return result.apply(sqrtAbs, Math.copySign(sqrtAbs, imaginary));
            } else {
                // Over/underflow.
                // Full scaling is not required as this is done in the hypotenuse function.
                // Keep the number as big as possible for maximum precision in the second sqrt.
                // Note if we scale by an even power of 2, we can re-scale by sqrt of the number.
                // a = sqrt(b)
                // a = sqrt(b/4) * sqrt(4)

                double rescale;
                double sx;
                double sy;
                if (Math.max(x, y) > SQRT_SAFE_UPPER) {
                    // Overflow. Scale down by 16 and rescale by sqrt(16).
                    sx = x / 16;
                    sy = y / 16;
                    rescale = 4;
                } else {
                    // Sub-normal numbers. Make them normal by scaling by 2^54,
                    // i.e. more than the mantissa digits, and rescale by sqrt(2^54) = 2^27.
                    sx = x * 0x1.0p54;
                    sy = y * 0x1.0p54;
                    rescale = 0x1.0p-27;
                }
                t = rescale * Math.sqrt(2 * (abs(sx, sy) + sx));
            }
        }

        if (real >= 0) {
            return result.apply(t / 2, imaginary / t);
        }
        return result.apply(y / t, Math.copySign(t / 2, imaginary));
    }
    public static DComplex sqrt(DComplex c, DComplexConstructor<DComplex> result) {
        return sqrt(c.getReal(), c.getImaginary(), result);
    }

    public static DComplex sinh(double real, double imaginary, DComplexConstructor<DComplex> result) {
        if (Double.isInfinite(real) && !Double.isFinite(imaginary)) {
            return result.apply(real, Double.NaN);
        }
        if (real == 0) {
            // Imaginary-only sinh(iy) = i sin(y).
            if (Double.isFinite(imaginary)) {
                // Maintain periodic property with respect to the imaginary component.
                // sinh(+/-0.0) * cos(+/-x) = +/-0 * cos(x)
                return result.apply(changeSign(real, Math.cos(imaginary)),
                    Math.sin(imaginary));
            }
            // If imaginary is inf/NaN the sign of the real part is unspecified.
            // Returning the same real value maintains the conjugate equality.
            // It is not possible to also maintain the odd function (hence the unspecified sign).
            return result.apply(real, Double.NaN);
        }
        if (imaginary == 0) {
            // Real-only sinh(x).
            return result.apply(Math.sinh(real), imaginary);
        }
        final double x = Math.abs(real);
        if (x > SAFE_EXP) {
            // Approximate sinh/cosh(x) using exp^|x| / 2
            return coshsinh(x, real, imaginary, true, result);
        }
        // No overflow of sinh/cosh
        return result.apply(Math.sinh(real) * Math.cos(imaginary),
            Math.cosh(real) * Math.sin(imaginary));
    }
    public static DComplex sinh(DComplex c, DComplexConstructor<DComplex> result) {
        return sinh(c.getReal(), c.getImaginary(), result);
    }
        /**
         * Returns the hyperbolic cosine of the complex number.
         *
         * <p>This function exists to allow implementation of the identity
         * {@code cos(z) = cosh(iz)}.<p>
         *
         * @param real Real part.
         * @param imaginary Imaginary part.
         * @param result Constructor.
         * @return The hyperbolic cosine of the complex number.
         */
    public static DComplex cosh(double real, double imaginary, DComplexConstructor<DComplex> result) {
        // ISO C99: Preserve the even function by mapping to positive
        // f(z) = f(-z)
        if (Double.isInfinite(real) && !Double.isFinite(imaginary)) {
            return result.apply(Math.abs(real), Double.NaN);
        }
        if (real == 0) {
            // Imaginary-only cosh(iy) = cos(y).
            if (Double.isFinite(imaginary)) {
                // Maintain periodic property with respect to the imaginary component.
                // sinh(+/-0.0) * sin(+/-x) = +/-0 * sin(x)
                return result.apply(Math.cos(imaginary),
                    changeSign(real, Math.sin(imaginary)));
            }
            // If imaginary is inf/NaN the sign of the imaginary part is unspecified.
            // Although not required by C99 changing the sign maintains the conjugate equality.
            // It is not possible to also maintain the even function (hence the unspecified sign).
            return result.apply(Double.NaN, changeSign(real, imaginary));
        }
        if (imaginary == 0) {
            // Real-only cosh(x).
            // Change sign to preserve conjugate equality and even function.
            // sin(+/-0) * sinh(+/-x) = +/-0 * +/-a (sinh is monotonic and same sign)
            // => change the sign of imaginary using real. Handles special case of infinite real.
            // If real is NaN the sign of the imaginary part is unspecified.
            return result.apply(Math.cosh(real), changeSign(imaginary, real));
        }
        final double x = Math.abs(real);
        if (x > SAFE_EXP) {
            // Approximate sinh/cosh(x) using exp^|x| / 2
            return coshsinh(x, real, imaginary, false, result);
        }
        // No overflow of sinh/cosh
        return result.apply(Math.cosh(real) * Math.cos(imaginary),
            Math.sinh(real) * Math.sin(imaginary));
    }
    public static DComplex cosh(DComplex c, DComplexConstructor<DComplex> result) {
        return cosh(c.getReal(), c.getImaginary(), result);
    }
    /**
     * Compute cosh or sinh when the absolute real component |x| is large. In this case
     * cosh(x) and sinh(x) can be approximated by exp(|x|) / 2:
     *
     * <pre>
     * cosh(x+iy) real = (e^|x| / 2) * cos(y)
     * cosh(x+iy) imag = (e^|x| / 2) * sin(y) * sign(x)
     * sinh(x+iy) real = (e^|x| / 2) * cos(y) * sign(x)
     * sinh(x+iy) imag = (e^|x| / 2) * sin(y)
     * </pre>
     *
     * @param x Absolute real component |x|.
     * @param real Real part (x).
     * @param imaginary Imaginary part (y).
     * @param sinh Set to true to compute sinh, otherwise cosh.
     * @param result Constructor.
     * @return The hyperbolic sine/cosine of the complex number.
     */
    private static DComplex coshsinh(double x, double real, double imaginary, boolean sinh,
                                     DComplexConstructor<DComplex> result) {
        // Always require the cos and sin.
        double re = Math.cos(imaginary);
        double im = Math.sin(imaginary);
        // Compute the correct function
        if (sinh) {
            re = changeSign(re, real);
        } else {
            im = changeSign(im, real);
        }
        // Multiply by (e^|x| / 2).
        // Overflow safe computation since sin/cos can be very small allowing a result
        // when e^x overflows: e^x / 2 = (e^m / 2) * e^m * e^(x-2m)
        if (x > SAFE_EXP * 3) {
            // e^x > e^m * e^m * e^m
            // y * (e^m / 2) * e^m * e^m will overflow when starting with Double.MIN_VALUE.
            // Note: Do not multiply by +inf to safeguard against sin(y)=0.0 which
            // will create 0 * inf = nan.
            re *= Double.MAX_VALUE * Double.MAX_VALUE * Double.MAX_VALUE;
            im *= Double.MAX_VALUE * Double.MAX_VALUE * Double.MAX_VALUE;
        } else {
            // Initial part of (e^x / 2) using (e^m / 2)
            re *= EXP_M / 2;
            im *= EXP_M / 2;
            double xm;
            if (x > SAFE_EXP * 2) {
                // e^x = e^m * e^m * e^(x-2m)
                re *= EXP_M;
                im *= EXP_M;
                xm = x - SAFE_EXP * 2;
            } else {
                // e^x = e^m * e^(x-m)
                xm = x - SAFE_EXP;
            }
            final double exp = Math.exp(xm);
            re *= exp;
            im *= exp;
        }
        return result.apply(re, im);
    }

    public static DComplex tanh(double real, double imaginary, DComplexConstructor<DComplex> result) {
        // Cache the absolute real value
        final double x = Math.abs(real);

        // Handle inf or nan.
        if (!isPosFinite(x) || !Double.isFinite(imaginary)) {
            if (isPosInfinite(x)) {
                if (Double.isFinite(imaginary)) {
                    // The sign is copied from sin(2y)
                    // The identity sin(2a) = 2 sin(a) cos(a) is used for consistency
                    // with the computation below. Only the magnitude is important
                    // so drop the 2. When |y| is small sign(sin(2y)) = sign(y).
                    final double sign = Math.abs(imaginary) < PI_OVER_2 ?
                        imaginary :
                        Math.sin(imaginary) * Math.cos(imaginary);
                    return result.apply(Math.copySign(1, real),
                        Math.copySign(0, sign));
                }
                // imaginary is infinite or NaN
                return result.apply(Math.copySign(1, real), Math.copySign(0, imaginary));
            }
            // Remaining cases:
            // (0 + i inf), returns (0 + i NaN)
            // (0 + i NaN), returns (0 + i NaN)
            // (x + i inf), returns (NaN + i NaN) for non-zero x (including infinite)
            // (x + i NaN), returns (NaN + i NaN) for non-zero x (including infinite)
            // (NaN + i 0), returns (NaN + i 0)
            // (NaN + i y), returns (NaN + i NaN) for non-zero y (including infinite)
            // (NaN + i NaN), returns (NaN + i NaN)
            return result.apply(real == 0 ? real : Double.NaN,
                imaginary == 0 ? imaginary : Double.NaN);
        }

        // Finite components
        // tanh(x+iy) = (sinh(2x) + i sin(2y)) / (cosh(2x) + cos(2y))

        if (real == 0) {
            // Imaginary-only tanh(iy) = i tan(y)
            // Identity: sin 2y / (1 + cos 2y) = tan(y)
            return result.apply(real, Math.tan(imaginary));
        }
        if (imaginary == 0) {
            // Identity: sinh 2x / (1 + cosh 2x) = tanh(x)
            return result.apply(Math.tanh(real), imaginary);
        }

        // The double angles can be avoided using the identities:
        // sinh(2x) = 2 sinh(x) cosh(x)
        // sin(2y) = 2 sin(y) cos(y)
        // cosh(2x) = 2 sinh^2(x) + 1
        // cos(2y) = 2 cos^2(y) - 1
        // tanh(x+iy) = (sinh(x)cosh(x) + i sin(y)cos(y)) / (sinh^2(x) + cos^2(y))
        // To avoid a junction when swapping between the double angles and the identities
        // the identities are used in all cases.

        if (x > SAFE_EXP / 2) {
            // Potential overflow in sinh/cosh(2x).
            // Approximate sinh/cosh using exp^x.
            // Ignore cos^2(y) in the divisor as it is insignificant.
            // real = sinh(x)cosh(x) / sinh^2(x) = +/-1
            final double re = Math.copySign(1, real);
            // imag = sin(2y) / 2 sinh^2(x)
            // sinh(x) -> sign(x) * e^|x| / 2 when x is large.
            // sinh^2(x) -> e^2|x| / 4 when x is large.
            // imag = sin(2y) / 2 (e^2|x| / 4) = 2 sin(2y) / e^2|x|
            //      = 4 * sin(y) cos(y) / e^2|x|
            // Underflow safe divide as e^2|x| may overflow:
            // imag = 4 * sin(y) cos(y) / e^m / e^(2|x| - m)
            // (|im| is a maximum of 2)
            double im = Math.sin(imaginary) * Math.cos(imaginary);
            if (x > SAFE_EXP) {
                // e^2|x| > e^m * e^m
                // This will underflow 2.0 / e^m / e^m
                im = Math.copySign(0.0, im);
            } else {
                // e^2|x| = e^m * e^(2|x| - m)
                im = 4 * im / EXP_M / Math.exp(2 * x - SAFE_EXP);
            }
            return result.apply(re, im);
        }

        // No overflow of sinh(2x) and cosh(2x)

        // Note: This does not use the definitional formula but uses the identity:
        // tanh(x+iy) = (sinh(x)cosh(x) + i sin(y)cos(y)) / (sinh^2(x) + cos^2(y))

        final double sinhx = Math.sinh(real);
        final double coshx = Math.cosh(real);
        final double siny = Math.sin(imaginary);
        final double cosy = Math.cos(imaginary);
        final double divisor = sinhx * sinhx + cosy * cosy;
        return result.apply(sinhx * coshx / divisor,
            siny * cosy / divisor);
    }
    public static DComplex tanh(DComplex c, DComplexConstructor<DComplex> result) {
        return tanh(c.getReal(), c.getImaginary(), result);
    }
        /**
         * Check that an absolute value is finite. Used to replace {@link Double#isFinite(double)}
         * when the input value is known to be positive (i.e. in the case where it has been
         * set using {@link Math#abs(double)}).
         *
         * @param d Value.
         * @return {@code true} if {@code d} is +finite.
         */
    private static boolean isPosFinite(double d) {
        return d <= Double.MAX_VALUE;
    }
    /**
     * Compute {@code x^2 + y^2 - 1} in high precision.
     * Assumes that the values x and y can be multiplied without overflow; that
     * {@code x >= y}; and both values are positive.
     *
     * @param x the x value
     * @param y the y value
     * @return {@code x^2 + y^2 - 1}.
     */
    private static double x2y2m1(double x, double y) {
        // Hull et al used (x-1)*(x+1)+y*y.
        // From the paper on page 236:

        // If x == 1 there is no cancellation.

        // If x > 1, there is also no cancellation, but the argument is now accurate
        // only to within a factor of 1 + 3 EPSILSON (note that x – 1 is exact),
        // so that error = 3 EPSILON.

        // If x < 1, there can be serious cancellation:

        // If 4 y^2 < |x^2 – 1| the cancellation is not serious ... the argument is accurate
        // only to within a factor of 1 + 4 EPSILSON so that error = 4 EPSILON.

        // Otherwise there can be serious cancellation and the relative error in the real part
        // could be enormous.

        final double xx = x * x;
        final double yy = y * y;
        // Modify to use high precision before the threshold set by Hull et al.
        // This is to preserve the monotonic output of the computation at the switch.
        // Set the threshold when x^2 + y^2 is above 0.5 thus subtracting 1 results in a number
        // that can be expressed with a higher precision than any number in the range 0.5-1.0
        // due to the variable exponent used below 0.5.
        if (x < 1 && xx + yy > 0.5) {
            // Large relative error.
            // This does not use o.a.c.numbers.LinearCombination.value(x, x, y, y, 1, -1).
            // It is optimised knowing that:
            // - the products are squares
            // - the final term is -1 (which does not require split multiplication and addition)
            // - The answer will not be NaN as the terms are not NaN components
            // - The order is known to be 1 > |x| >= |y|
            // The squares are computed using a split multiply algorithm and
            // the summation using an extended precision summation algorithm.

            // Split x and y as one 26 bits number and one 27 bits number
            final double xHigh = splitHigh(x);
            final double xLow  = x - xHigh;
            final double yHigh = splitHigh(y);
            final double yLow  = y - yHigh;

            // Accurate split multiplication x * x and y * y
            final double x2Low = squareLow(xLow, xHigh, xx);
            final double y2Low = squareLow(yLow, yHigh, yy);

            return sumx2y2m1(xx, x2Low, yy, y2Low);
        }
        return (x - 1) * (x + 1) + yy;
    }

    /**
     * Implement Dekker's method to split a value into two parts. Multiplying by (2^s + 1) create
     * a big value from which to derive the two split parts.
     * <pre>
     * c = (2^s + 1) * a
     * a_big = c - a
     * a_hi = c - a_big
     * a_lo = a - a_hi
     * a = a_hi + a_lo
     * </pre>
     *
     * <p>The multiplicand must be odd allowing a p-bit value to be split into
     * (p-s)-bit value {@code a_hi} and a non-overlapping (s-1)-bit value {@code a_lo}.
     * Combined they have (p􏰔-1) bits of significand but the sign bit of {@code a_lo}
     * contains a bit of information.
     *
     * @param a Value.
     * @return the high part of the value.
     * @see <a href="https://doi.org/10.1007/BF01397083">
     * Dekker (1971) A floating-point technique for extending the available precision</a>
     */
    private static double splitHigh(double a) {
        final double c = MULTIPLIER * a;
        return c - (c - a);
    }

    /**
     * Compute the round-off from the square of a split number with {@code low} and {@code high}
     * components. Uses Dekker's algorithm for split multiplication modified for a square product.
     *
     * <p>Note: This is candidate to be replaced with {@code Math.fma(x, x, -x * x)} to compute
     * the round-off from the square product {@code x * x}. This would remove the requirement
     * to compute the split number and make this method redundant. {@code Math.fma} requires
     * JDK 9 and FMA hardware support.
     *
     * @param low Low part of number.
     * @param high High part of number.
     * @param square Square of the number.
     * @return <code>low * low - (((product - high * high) - low * high) - high * low)</code>
     * @see <a href="http://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps">
     * Shewchuk (1997) Theorum 18</a>
     */
    private static double squareLow(double low, double high, double square) {
        final double lh = low * high;
        return low * low - (((square - high * high) - lh) - lh);
    }

    /**
     * Compute the round-off from the sum of two numbers {@code a} and {@code b} using
     * Dekker's two-sum algorithm. The values are required to be ordered by magnitude:
     * {@code |a| >= |b|}.
     *
     * @param a First part of sum.
     * @param b Second part of sum.
     * @param x Sum.
     * @return <code>b - (x - a)</code>
     * @see <a href="http://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps">
     * Shewchuk (1997) Theorum 6</a>
     */
    private static double fastSumLow(double a, double b, double x) {
        // x = a + b
        // bVirtual = x - a
        // y = b - bVirtual
        return b - (x - a);
    }

    /**
     * Compute the round-off from the sum of two numbers {@code a} and {@code b} using
     * Knuth's two-sum algorithm. The values are not required to be ordered by magnitude.
     *
     * @param a First part of sum.
     * @param b Second part of sum.
     * @param x Sum.
     * @return <code>(a - (x - (x - a))) + (b - (x - a))</code>
     * @see <a href="http://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps">
     * Shewchuk (1997) Theorum 7</a>
     */
    private static double sumLow(double a, double b, double x) {
        // x = a + b
        // bVirtual = x - a
        // aVirtual = x - bVirtual
        // bRoundoff = b - bVirtual
        // aRoundoff = a - aVirtual
        // y = aRoundoff + bRoundoff
        final double bVirtual = x - a;
        return (a - (x - bVirtual)) + (b - bVirtual);
    }

    /**
     * Sum x^2 + y^2 - 1. It is assumed that {@code y <= x < 1}.
     *
     * <p>Implement Shewchuk's expansion-sum algorithm: [x2Low, x2High] + [-1] + [y2Low, y2High].
     *
     * @param x2High High part of x^2.
     * @param x2Low Low part of x^2.
     * @param y2High High part of y^2.
     * @param y2Low Low part of y^2.
     * @return x^2 + y^2 - 1
     * @see <a href="http://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps">
     * Shewchuk (1997) Theorum 12</a>
     */
    private static double sumx2y2m1(double x2High, double x2Low, double y2High, double y2Low) {
        // Let e and f be non-overlapping expansions of components of length m and n.
        // The following algorithm will produce a non-overlapping expansion h where the
        // sum h_i = e + f and components of h are in increasing order of magnitude.
        // Expansion-sum proceeds by a grow-expansion of the first part from one expansion
        // into the other, extending its length by 1. The process repeats for the next part
        // but the grow-expansion starts at the previous merge position + 1.
        // Thus expansion-sum requires mn two-sum operations to merge length m into length n
        // resulting in length m+n-1.
        // Variables numbered from 1 as per Figure 7 (p.12). The output expansion h is placed
        // into e increasing its length for each grow expansion.
        // We have two expansions for x^2 and y^2 and the whole number -1.
        // Expecting (x^2 + y^2) close to 1 we generate first the intermediate expansion
        // (x^2 - 1) moving the result away from 1 where there are sparse floating point
        // representations. This is then added to a similar magnitude y^2. Leaving the -1
        // until last suffers from 1 ulp rounding errors more often and the requirement
        // for a distillation sum to reduce rounding error frequency.

        // Note: Do not use the alternative fast-expansion-sum of the parts sorted by magnitude.
        // The parts can be ordered with a single comparison into:
        // [y2Low, (y2High|x2Low), x2High, -1]
        // The fast-two-sum saves 1 fast-two-sum and 3 two-sum operations (21 additions) and
        // adds a penalty of a single branch condition.
        // However the order in not "strongly non-overlapping" and the fast-expansion-sum
        // output will not be strongly non-overlapping. The sum of the output has 1 ulp error
        // on random cis numbers approximately 1 in 160 events. This can be removed by a
        // distillation two-sum pass over the final expansion as a cost of 1 fast-two-sum and
        // 3 two-sum operations! So we use the expansion sum with the same operations and
        // no branches.
        // q=running sum
        double q = x2Low - 1;
        double e1 = fastSumLow(-1, x2Low, q);
        double e3 = q + x2High;
        double e2 = sumLow(q, x2High, e3);

        final double f1 = y2Low;
        final double f2 = y2High;

        // Grow expansion of f1 into e
        q = f1 + e1;
        e1 = sumLow(f1, e1, q);
        double p = q + e2;
        e2 = sumLow(q, e2, p);
        double e4 = p + e3;
        e3 = sumLow(p, e3, e4);

        // Grow expansion of f2 into e (only required to start at e2)
        q = f2 + e2;
        e2 = sumLow(f2, e2, q);
        p = q + e3;
        e3 = sumLow(q, e3, p);
        final double e5 = p + e4;
        e4 = sumLow(p, e4, e5);

        // Final summation:
        // The sum of the parts is within 1 ulp of the true expansion value e:
        // |e - sum| < ulp(sum).
        // To achieve the exact result requires iteration of a distillation two-sum through
        // the expansion until convergence, i.e. no smaller term changes higher terms.
        // This requires (n-1) iterations for length n. Here we neglect this as
        // although the method is not ensured to be exact is it robust on random
        // cis numbers.
        return e1 + e2 + e3 + e4 + e5;
    }

    /**
     * Returns the logarithm of this complex number using the provided function.
     * Implements the formula:
     *
     * <pre>
     *   log(x + i y) = log(|x + i y|) + i arg(x + i y)</pre>
     *
     * <p>Warning: The argument {@code logOf2} must be equal to {@code log(2)} using the
     * provided log function otherwise scaling using powers of 2 in the case of overflow
     * will be incorrect. This is provided as an internal optimisation.
     *
     * @param c input complex number
     * @param constructor Constructor for the returned complex.
     * @return The logarithm of this complex number.
     */
    public static DComplex log(DComplex c, DComplexConstructor constructor) {
        return log(c.getReal(), c.getImaginary(), constructor);
    }

    /**
     * Returns the logarithm of this complex number using the provided function.
     * Implements the formula:
     *
     * <pre>
     *   log(x + i y) = log(|x + i y|) + i arg(x + i y)</pre>
     *
     * <p>Warning: The argument {@code logOf2} must be equal to {@code log(2)} using the
     * provided log function otherwise scaling using powers of 2 in the case of overflow
     * will be incorrect. This is provided as an internal optimisation.
     *
     * @param real real part of input complex number
     * @param imaginary imaginary part of input complex number
     * @param constructor Constructor for the returned complex.
     * @return The logarithm of this complex number.
     */
    private static DComplex log(double real, double imaginary, DComplexConstructor<DComplex> constructor) {
        return log(Math::log, HALF, LN_2, real, imaginary, constructor);
    }

    /**
     * Returns the logarithm of this complex number using the provided function.
     * Implements the formula:
     *
     * <pre>
     *   log10(x + i y) = log10(|x + i y|) + i arg(x + i y)</pre>
     *
     * <p>Warning: The argument {@code logOf2} must be equal to {@code log(2)} using the
     * provided log function otherwise scaling using powers of 2 in the case of overflow
     * will be incorrect. This is provided as an internal optimisation.
     *
     * @param c input complex number
     * @param constructor Constructor for the returned complex.
     * @return The logarithm of this complex number.
     */
    public static DComplex log10(DComplex c, DComplexConstructor constructor) {
        return log10(c.getReal(), c.getImaginary(), constructor);
    }

    /**
     * Returns the logarithm of this complex number using the provided function.
     * Implements the formula:
     *
     * <pre>
     *   log10(x + i y) = log10(|x + i y|) + i arg(x + i y)</pre>
     *
     * <p>Warning: The argument {@code logOf2} must be equal to {@code log(2)} using the
     * provided log function otherwise scaling using powers of 2 in the case of overflow
     * will be incorrect. This is provided as an internal optimisation.
     *
     * @param real real part of input complex number
     * @param imaginary imaginary part of input complex number
     * @param constructor Constructor for the returned complex.
     * @return The logarithm of this complex number.
     */
    private static DComplex log10(double real, double imaginary, DComplexConstructor<DComplex> constructor) {
        return log(Math::log10, LOG_10E_O_2, LOG10_2, real, imaginary, constructor);
    }


    static DComplex log(DoubleUnaryOperator log, double logOfeOver2, double logOf2,
                                double real, double imaginary, DComplexConstructor<DComplex> constructor) {
        // Handle NaN
        if (Double.isNaN(real) || Double.isNaN(imaginary)) {
            // Return NaN unless infinite
            if (isInfinite(real, imaginary)) {
                return constructor.apply(Double.POSITIVE_INFINITY, Double.NaN);
            }
            // No-use of the input constructor
            return NAN;
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
                negative(real) ? Math.copySign(Math.PI, imaginary) : imaginary);
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
            re = Math.log1p(x2y2m1(x, y)) * logOfeOver2;
        } else {
            // Check for over/underflow in |z|
            // When scaling:
            // log(a / b) = log(a) - log(b)
            // So initialize the result with the log of the scale factor.
            re = 0;
            if (x > Double.MAX_VALUE / 2) {
                // Potential overflow.
                if (isPosInfinite(x)) {
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

}
