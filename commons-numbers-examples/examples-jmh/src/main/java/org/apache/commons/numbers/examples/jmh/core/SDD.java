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
package org.apache.commons.numbers.examples.jmh.core;

/**
 * Computes double-double floating-point operations.
 *
 * <p><b>Note</b>
 *
 * <p>This class has been copied from the original static mutable double-double implementation
 * in Commons Statistics inference module.
 *
 * <p>A double-double is an unevaluated sum of two IEEE double precision numbers capable
 * of representing at least 106 bits of significand.
 *
 * <p>This implementation performs all computations using the explicit high and low parts
 * of the double-double numbers. Results are written to a provided double-double instance
 * which is returned for convenience. This allows the same double-double instance to be an
 * argument and the result of a computation. The double-double class is mutable but can
 * only be modified by methods within this class. All static methods in this class will
 * not allocate objects so minimising memory allocation of intermediate numbers during
 * computation.
 *
 * <p>Note: This is not a full double-double implementation. Only the methods required
 * within this package for extended precision computations have been implemented.
 *
 * <p>References:
 * <ol>
 * <li>
 * Dekker, T.J. (1971)
 * <a href="https://doi.org/10.1007/BF01397083">
 * A floating-point technique for extending the available precision</a>
 * Numerische Mathematik, 18:224–242.
 * <li>
 * Shewchuk, J.R. (1997)
 * <a href="https://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps">
 * Arbitrary Precision Floating-Point Arithmetic</a>.
 * <li>
 * Hide, Y, Li, X.S. and Bailey, D.H. (2008)
 * <a href="https://www.davidhbailey.com/dhbpapers/qd.pdf">
 * Library for Double-Double and Quad-Double Arithmetic</a>.
 * </ol>
 *
 * @since 1.2
 */
final class SDD {
    // Caveat:
    //
    // The code below uses many additions/subtractions that may
    // appear redundant. However, they should NOT be simplified, as they
    // do use IEEE754 floating point arithmetic rounding properties.
    //
    // Algorithms are based on computing the product or sum of two values x and y in
    // extended precision. The standard result is stored using a double (high part z) and
    // the round-off error (or low part zz) is stored in a second double, e.g:
    // x * y = (z, zz); z + zz = x * y
    // x + y = (z, zz); z + zz = x + y
    //
    // The building blocks for double-double arithmetic are:
    //
    // Fast-Two-Sum: Addition of two doubles (ordered |x| > |y|) to a double-double
    // Two-Sum: Addition of two doubles (unordered) to a double-double
    // Two-Prod: Multiplication of two doubles to a double-double
    //
    // These are used to create functions operating on double and double-double numbers.
    //
    // To sum multiple (z, zz) results ideally the parts are sorted in order of
    // non-decreasing magnitude and summed. This is exact if each number's most significant
    // bit is below the least significant bit of the next (i.e. does not
    // overlap). Creating non-overlapping parts requires a rebalancing
    // of adjacent pairs using a summation z + zz = (z1, zz1) iteratively through the parts
    // (see Shewchuk (1997) Grow-Expansion and Expansion-Sum [2]).
    //
    // Accurate summation of an expansion (more than one double value) to a double-double
    // performs a two-sum through the expansion e (length m).
    // The single pass with two-sum ensures that the final term e_m is a good approximation
    // for e: |e - e_m| < ulp(e_m); and the sum of the parts to
    // e_(m-1) is within 1 ULP of the round-off ulp(|e - e_m|).
    // These final two terms create the double-double result using two-sum.

    /**
     * The multiplier used to split the double value into high and low parts. From
     * Dekker (1971): "The constant should be chosen equal to 2^(p - p/2) + 1,
     * where p is the number of binary digits in the mantissa". Here p is 53
     * and the multiplier is {@code 2^27 + 1}.
     */
    private static final double MULTIPLIER = 1.0 + 0x1.0p27;
    /**
     * The upper limit above which a number may overflow during the split into a high part.
     * Assuming the multiplier is above 2^27 and the maximum exponent is 1023 then a safe
     * limit is a value with an exponent of (1023 - 27) = 2^996.
     * 996 is the value obtained from {@code Math.getExponent(Double.MAX_VALUE / MULTIPLIER)}.
     */
    private static final double SAFE_UPPER = 0x1.0p996;
    /** The scale to use when down-scaling during a split into a high part.
     * This must be smaller than the inverse of the multiplier and a power of 2 for exact scaling. */
    private static final double DOWN_SCALE = 0x1.0p-30;
    /** The scale to use when re-scaling during a split into a high part.
     * This is the inverse of {@link #DOWN_SCALE}. */
    private static final double UP_SCALE = 0x1.0p30;
    /** The mask to extract the raw 11-bit exponent.
     * The value must be shifted 52-bits to remove the mantissa bits. */
    private static final int EXP_MASK = 0x7ff;
    /** The value 2046 converted for use if using {@link Integer#compareUnsigned(int, int)}.
     * This requires adding {@link Integer#MIN_VALUE} to 2046. */
    private static final int CMP_UNSIGNED_2046 = Integer.MIN_VALUE + 2046;
    /** The value -1 converted for use if using {@link Integer#compareUnsigned(int, int)}.
     * This requires adding {@link Integer#MIN_VALUE} to -1. */
    private static final int CMP_UNSIGNED_MINUS_1 = Integer.MIN_VALUE - 1;
    /** The value 1022 converted for use if using {@link Integer#compareUnsigned(int, int)}.
     * This requires adding {@link Integer#MIN_VALUE} to 1022. */
    private static final int CMP_UNSIGNED_1022 = Integer.MIN_VALUE + 1022;
    /** 2^512. */
    private static final double TWO_POW_512 = 0x1.0p512;
    /** 2^-512. */
    private static final double TWO_POW_M512 = 0x1.0p-512;
    /** Mask to remove the sign bit from a long. */
    private static final long UNSIGN_MASK = 0x7fff_ffff_ffff_ffffL;
    /** Mask to extract the 52-bit mantissa from a long representation of a double. */
    private static final long MANTISSA_MASK = 0x000f_ffff_ffff_ffffL;
    /** Exponent offset in IEEE754 representation. */
    private static final int EXPONENT_OFFSET = 1023;
    /** 0.5. */
    private static final double HALF = 0.5;
    /** The limit for safe multiplication of {@code x*y}, assuming values above 1.
     * Used to maintain positive values during the power computation. */
    private static final double SAFE_MULTIPLY = 0x1.0p500;
    /** Error message when the input is not a normalized double: {@code x != x + xx}. */
    private static final String NOT_NOMALIZED = "Input is not a normalized double-double";

    /** The high part of the double-double number. */
    private double hi;
    /** The low part of the double-double number. */
    private double lo;

    /**
     * Create a double-double. The value is zero.
     */
    private SDD() {
        // Do nothing
    }

    /**
     * Copy constructor.
     *
     * @param source Source to copy.
     */
    private SDD(SDD source) {
        hi = source.hi;
        lo = source.lo;
    }

    /**
     * Return a copy of this number.
     *
     * @return the copy
     */
    SDD copy() {
        return new SDD(this);
    }

    /**
     * Creates the a new instance of a double-double number. The value is zero.
     *
     * @return the double-double
     */
    static SDD create() {
        return new SDD();
    }

    /**
     * Creates the double-double number as the value {@code (x, 0)}.
     *
     * @param x Value.
     * @return the double-double
     */
    static SDD create(double x) {
        final SDD z = new SDD();
        z.hi = x;
        return z;
    }

    /**
     * Creates the double-double number as the sum of two values {@code (x, xx) = a + b}.
     * The number will be normalized such that {@code |x| > epsilon * |xx|}.
     * The arguments are not required to be ordered by magnitude,
     * i.e. the result is commutative {@code (x, xx) = a + b == b + a}.
     *
     * @param a First part of sum.
     * @param b Second part of sum.
     * @return the double-double
     */
    static SDD create(double a, double b) {
        return twoSum(a, b, new SDD());
    }

    /**
     * Gets the high part of the double-double number.
     *
     * @return the high part
     */
    double hi() {
        return hi;
    }

    /**
     * Gets the low part of the double-double number.
     *
     * @return the low part
     */
    double lo() {
        return lo;
    }

    /**
     * Get the value as a double.
     *
     * @return the value converted to a double
     */
    double doubleValue() {
        return hi + lo;
    }

    /**
     * Set the double-double value to {@code (x, xx)}.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @return a reference to {@code this}
     */
    private SDD set(double x, double xx) {
        hi = x;
        lo = xx;
        return this;
    }

    /**
     * Sets the double-double number as the value {@code (x, 0)}.
     *
     * @param x High part.
     * @param z Number (result).
     * @return the double-double
     */
    static SDD set(double x, SDD z) {
        z.set(x, 0);
        return z;
    }

    /**
     * Sets the double-double number as the value {@code (x, xx)}.
     * The number must be normalized such that {@code x == x + xx}.
     *
     * @param x High part.
     * @param xx Low part.
     * @param z Number (result).
     * @return the double-double
     */
    static SDD set(double x, double xx, SDD z) {
        assert x == x + xx : NOT_NOMALIZED;
        z.set(x, xx);
        return z;
    }

    /**
     * Compute the sum of two numbers {@code a} and {@code b} using
     * Dekker's two-sum algorithm. The values are required to be ordered by magnitude:
     * {@code |a| >= |b|}.
     *
     * <p>If {@code a} is zero and {@code b} is non-zero the returned value is {@code (b, 0)}.
     *
     * @param a First part of sum.
     * @param b Second part of sum.
     * @param s Sum (result).
     * @return the sum
     * @see #fastTwoDiff(double, double, SDD)
     * @see <a href="http://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps">
     * Shewchuk (1997) Theorum 6</a>
     */
    static SDD fastTwoSum(double a, double b, SDD s) {
        // (x, xx) = a + b
        // bVirtual = x - a
        // xx = b - bVirtual
        final double x = a + b;
        s.lo = b - (x - a);
        s.hi = x;
        return s;
    }

    /**
     * Compute the round-off of the sum of two numbers {@code a} and {@code b} using
     * Dekker's two-sum algorithm. The values are required to be ordered by magnitude:
     * {@code |a| >= |b|}.
     *
     * <p>If {@code a} is zero and {@code b} is non-zero the returned value is zero.
     *
     * @param a First part of sum.
     * @param b Second part of sum.
     * @param x Sum.
     * @return the sum round-off
     * @see #fastTwoSum(double, double, SDD)
     */
    private static double fastTwoSumLow(double a, double b, double x) {
        return b - (x - a);
    }

    /**
     * Compute the difference of two numbers {@code a} and {@code b} using
     * Dekker's two-sum algorithm. The values are required to be ordered by magnitude:
     * {@code |a| >= |b|}.
     *
     * <p>Computes the same results as {@link #fastTwoSum(double, double, SDD) fastTwoSum(a, -b)}.
     *
     * @param a Minuend.
     * @param b Subtrahend.
     * @param d Difference (result).
     * @return the difference
     * @see #fastTwoSum(double, double, SDD)
     * @see <a href="http://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps">
     * Shewchuk (1997) Theorum 6</a>
     */
    static SDD fastTwoDiff(double a, double b, SDD d) {
        // (x, xx) = a - b
        // bVirtual = a - x
        // xx = bVirtual - b
        final double x = a - b;
        d.lo = (a - x) - b;
        d.hi = x;
        return d;
    }

    /**
     * Compute the sum of two numbers {@code a} and {@code b} using
     * Knuth's two-sum algorithm. The values are not required to be ordered by magnitude,
     * i.e. the result is commutative {@code s = a + b == b + a}.
     *
     * @param a First part of sum.
     * @param b Second part of sum.
     * @param s Sum (result).
     * @return the sum
     * @see #twoDiff(double, double, SDD)
     * @see <a href="http://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps">
     * Shewchuk (1997) Theorum 7</a>
     */
    static SDD twoSum(double a, double b, SDD s) {
        // (x, xx) = a + b
        // bVirtual = x - a
        // aVirtual = x - bVirtual
        // bRoundoff = b - bVirtual
        // aRoundoff = a - aVirtual
        // xx = aRoundoff + bRoundoff
        final double x = a + b;
        final double bVirtual = x - a;
        s.lo = (a - (x - bVirtual)) + (b - bVirtual);
        s.hi = x;
        return s;
    }

    /**
     * Compute the round-off of the sum of two numbers {@code a} and {@code b} using
     * Knuth two-sum algorithm. The values are not required to be ordered by magnitude,
     * i.e. the result is commutative {@code s = a + b == b + a}.
     *
     * @param a First part of sum.
     * @param b Second part of sum.
     * @param x Sum.
     * @return the sum round-off
     * @see #twoSum(double, double, SDD)
     */
    private static double twoSumLow(double a, double b, double x) {
        final double bVirtual = x - a;
        return (a - (x - bVirtual)) + (b - bVirtual);
    }

    /**
     * Compute the difference of two numbers {@code a} and {@code b} using
     * Knuth's two-sum algorithm. The values are not required to be ordered by magnitude
     *
     * <p>Computes the same results as {@link #twoSum(double, double, SDD) twoSum(a, -b)}.
     *
     * @param a Minuend.
     * @param b Subtrahend.
     * @param d Difference (result).
     * @return the difference
     * @see #twoSum(double, double, SDD)
     */
    static SDD twoDiff(double a, double b, SDD d) {
        // (x, xx) = a - b
        // bVirtual = a - x
        // aVirtual = x + bVirtual
        // bRoundoff = b - bVirtual
        // aRoundoff = a - aVirtual
        // xx = aRoundoff - bRoundoff
        final double x = a - b;
        final double bVirtual = a - x;
        d.lo = (a - (x + bVirtual)) - (b - bVirtual);
        d.hi = x;
        return d;
    }

    /**
     * Compute the double-double number {@code (z,zz)} for the exact
     * product of {@code x} and {@code y}.
     *
     * <p>The method is written to be functionally similar to using a fused multiply add (FMA)
     * operation to compute the low part, for example JDK 9's Math.fma function:
     * <pre>
     *  double x = ...;
     *  double y = ...;
     *  double xy = x * y;
     *  double low = Math.fma(x, y, -xy);
     * </pre>
     *
     * <p>This creates the following special cases:
     *
     * <ul>
     *  <li>If {@code x * y} is sub-normal or zero then the low part is 0.0.
     *  <li>If {@code x * y} is infinite or NaN then the low part is NaN.
     * </ul>
     *
     * @param x First factor.
     * @param y Second factor.
     * @param z Product (result).
     * @return the product
     */
    static SDD twoProd(double x, double y, SDD z) {
        final double xy = x * y;
        z.hi = xy;

        // If the number is sub-normal, inf or nan there is no round-off.
        if (isNotNormal(xy)) {
            // Returns 0.0 for sub-normal xy, otherwise NaN for inf/nan:
            z.lo = xy - xy;
            return z;
        }

        // The result xy is finite and normal.
        // Use Dekker's mul12 algorithm that splits the values into high and low parts.
        // Dekker's split using multiplication will overflow if the value is within 2^27
        // of double max value. It can also produce 26-bit approximations that are larger
        // than the input numbers for the high part causing overflow in hx * hy when
        // x * y does not overflow. So we must scale down big numbers.
        // We only have to scale the largest number as we know the product does not overflow
        // (if one is too big then the other cannot be).
        // We also scale if the product is close to overflow to avoid intermediate overflow.
        // This could be done at a higher limit (e.g. Math.abs(xy) > Double.MAX_VALUE / 4)
        // but is included here to have a single low probability branch condition.

        // Add the absolute inputs for a single comparison. The sum will not be more than
        // 3-fold higher than any component.
        final double a = Math.abs(x);
        final double b = Math.abs(y);
        if (a + b + Math.abs(xy) >= SAFE_UPPER) {
            // Only required to scale the largest number as x*y does not overflow.
            if (a > b) {
                z.lo = productLowUnscaled(x * DOWN_SCALE, y, xy * DOWN_SCALE) * UP_SCALE;
            } else {
                z.lo = productLowUnscaled(x, y * DOWN_SCALE, xy * DOWN_SCALE) * UP_SCALE;
            }
        } else {
            // No scaling required. This is the expected branch for a finite product.
            z.lo = productLowUnscaled(x, y, xy);
        }
        return z;
    }

    /**
     * Compute the low part of the double length number {@code (z,zz)} for the exact
     * product of {@code x} and {@code y} using Dekker's mult12 algorithm. The standard
     * precision product {@code x*y} must be provided. The numbers {@code x} and {@code y}
     * are split into high and low parts using Dekker's algorithm.
     *
     * <p>Warning: This method does not perform scaling in Dekker's split and large
     * finite numbers can create NaN results.
     *
     * @param x First factor.
     * @param y Second factor.
     * @param xy Product of the factors (x * y).
     * @return the low part of the product double length number
     * @see #highPartUnscaled(double)
     */
    private static double productLowUnscaled(double x, double y, double xy) {
        // Split the numbers using Dekker's algorithm without scaling
        final double hx = highPartUnscaled(x);
        final double lx = x - hx;

        final double hy = highPartUnscaled(y);
        final double ly = y - hy;

        // Compute the multiply low part:
        // err1 = xy - hx * hy
        // err2 = err1 - lx * hy
        // err3 = err2 - hx * ly
        // low = lx * ly - err3
        return lx * ly - (((xy - hx * hy) - lx * hy) - hx * ly);
    }

    /**
     * Compute the low part of the double length number {@code (z,zz)} for the exact
     * product of {@code x} and {@code y} using Dekker's mult12 algorithm. The standard
     * precision product {@code x*y}, and the high and low parts of the factors must be
     * provided.
     *
     * @param hx High-part of first factor.
     * @param lx Low-part of first factor.
     * @param hy High-part of second factor.
     * @param ly Low-part of second factor.
     * @param xy Product of the factors (x * y).
     * @return the low part of the product double length number
     * @see #productLowUnscaled(double, double, double)
     */
    private static double productLowUnscaled(double hx, double lx, double hy, double ly, double xy) {
        return lx * ly - (((xy - hx * hy) - lx * hy) - hx * ly);
    }

    /**
     * Compute the low part of the double length number {@code (z,zz)} for the exact
     * square of {@code x} using Dekker's mult12 algorithm. The standard
     * precision square {@code x*x} must be provided. The number {@code x}
     * is split into high and low parts using Dekker's algorithm.
     *
     * <p>Warning: This method does not perform scaling in Dekker's split and large
     * finite numbers can create NaN results.
     *
     * @param x Factor.
     * @param x2 Square of the factor (x * x).
     * @return the low part of the square double length number
     * @see #highPartUnscaled(double)
     * @see #productLowUnscaled(double, double, double)
     */
    private static double squareLowUnscaled(double x, double x2) {
        // See productLowUnscaled
        final double hx = highPartUnscaled(x);
        final double lx = x - hx;
        return lx * lx - ((x2 - hx * hx) - 2 * lx * hx);
    }

    /**
     * Compute the low part of the double length number {@code (z,zz)} for the exact
     * square of {@code x} using Dekker's mult12 algorithm. The standard
     * precision square {@code x*x}, and the high and low parts of the factors must be
     * provided.
     *
     * @param hx High-part of factor.
     * @param lx Low-part of factor.
     * @param x2 Square of the factor (x * x).
     * @return the low part of the square double length number
     * @see #squareLowUnscaled(double, double)
     */
    private static double squareLowUnscaled(double hx, double lx, double x2) {
        return lx * lx - ((x2 - hx * hx) - 2 * lx * hx);
    }

    /**
     * Implement Dekker's method to split a value into two parts. Multiplying by (2^s + 1) creates
     * a big value from which to derive the two split parts.
     * <pre>
     * c = (2^s + 1) * a
     * a_big = c - a
     * a_hi = c - a_big
     * a_lo = a - a_hi
     * a = a_hi + a_lo
     * </pre>
     *
     * <p>The multiplicand allows a p-bit value to be split into
     * (p-s)-bit value {@code a_hi} and a non-overlapping (s-1)-bit value {@code a_lo}.
     * Combined they have (p-1) bits of significand but the sign bit of {@code a_lo}
     * contains a bit of information. The constant is chosen so that s is ceil(p/2) where
     * the precision p for a double is 53-bits (1-bit of the mantissa is assumed to be
     * 1 for a non sub-normal number) and s is 27.
     *
     * <p>This conversion does not use scaling and the result of overflow is NaN. Overflow
     * may occur when the exponent of the input value is above 996.
     *
     * <p>Splitting a NaN or infinite value will return NaN.
     *
     * @param value Value.
     * @return the high part of the value.
     * @see Math#getExponent(double)
     */
    private static double highPartUnscaled(double value) {
        final double c = MULTIPLIER * value;
        return c - (c - value);
    }

    /**
     * Checks if the number is not normal. This is functionally equivalent to:
     * <pre>{@code
     * final double abs = Math.abs(a);
     * return (abs <= Double.MIN_NORMAL || !(abs <= Double.MAX_VALUE));
     * }</pre>
     *
     * @param a The value.
     * @return true if the value is not normal
     */
    private static boolean isNotNormal(double a) {
        // Sub-normal numbers have a biased exponent of 0.
        // Inf/NaN numbers have a biased exponent of 2047.
        // Catch both cases by extracting the raw exponent, subtracting 1
        // and compare unsigned (so 0 underflows to a unsigned large value).
        final int baisedExponent = ((int) (Double.doubleToRawLongBits(a) >>> 52)) & EXP_MASK;
        // Pre-compute the additions used by Integer.compareUnsigned
        return baisedExponent + CMP_UNSIGNED_MINUS_1 >= CMP_UNSIGNED_2046;
    }

    /**
     * Compute the sum of {@code (x, xx)} and {@code y}.
     *
     * <p>This computes the same result as
     * {@link #fastAdd(double, double, double, double, SDD) add(x, xx, y, 0, s)}.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param y y.
     * @param s Sum (result).
     * @return the sum
     * @see #fastAdd(double, double, double, double, SDD)
     */
    static SDD fastAdd(double x, double xx, double y, SDD s) {
        // (s0, s1) = x + y
        twoSum(x, y, s);
        // Note: if x + y cancel to a non-zero result then s.hi is >= 1 ulp of x.
        // This is larger than xx so fast-two-sum can be used.
        return fastTwoSum(s.hi, s.lo + xx, s);
    }

    /**
     * Compute the sum of {@code (x, xx)} and {@code y}.
     *
     * <p>This computes the same result as
     * {@link #add(double, double, double, double, SDD) add(x, xx, y, 0, s)}.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param y y.
     * @param s Sum (result).
     * @return the sum
     * @see #add(double, double, double, double, SDD)
     */
    static SDD add(double x, double xx, double y, SDD s) {
        // Grow expansion (Schewchuk): (x, xx) + y -> (s0, s1, s2)
        twoSum(xx, y, s);
        double s2 = s.lo;
        twoSum(x, s.hi, s);
        final double s1 = s.lo;
        final double s0 = s.hi;
        // Compress (Schewchuk Fig. 15): (s0, s1, s2) -> (s0, s1)
        fastTwoSum(s1, s2, s);
        s2 = s.lo;
        fastTwoSum(s0, s.hi, s);
        // Here (s0, s1) = s
        // e = exact 159-bit result
        // |e - s0| <= ulp(s0)
        // |s1 + s2| <= ulp(e - s0)
        return fastTwoSum(s.hi, s2 + s.lo, s);
    }

    /**
     * Compute the sum of {@code (x, xx)} and {@code (y, yy)}.
     *
     * <p>The result is within the error bound {@code 4 eps^2} with {@code eps = 2^-53}.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param y High part of y.
     * @param yy Low part of y.
     * @param s Sum (result).
     * @return the sum
     * @see #add(double, double, double, double, SDD)
     */
    static SDD fastAdd(double x, double xx, double y, double yy, SDD s) {
        // Sum parts and save
        // (p, pp) = x + y
        twoSum(x, y, s);
        final double p = s.hi;
        final double pp = s.lo;
        // (q, qq) = xx + yy
        twoSum(xx, yy, s);
        final double q = s.hi;
        final double qq = s.lo;
        // result = p + q
        // |pp| is >= 1 ulp of max(|x|, |y|)
        // |q| is >= 1 ulp of max(|xx|, |yy|)
        fastTwoSum(p, pp + q, s);
        return fastTwoSum(s.hi, s.lo + qq, s);
    }

    /**
     * Compute the sum of {@code (x, xx)} and {@code (y, yy)}.
     *
     * <p>The high-part of the result is within 1 ulp of the true sum {@code e}.
     * The low-part of the result is within 1 ulp of the result of the high-part
     * subtracted from the true sum {@code e - hi}.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param y High part of y.
     * @param yy Low part of y.
     * @param s Sum (result).
     * @return the sum
     * @see #fastAdd(double, double, double, double, SDD)
     */
    static SDD add(double x, double xx, double y, double yy, SDD s) {
        // Expansion sum (Schewchuk Fig 7): (x, xx) + (x, yy) -> (s0, s1, s2, s3)
        twoSum(xx, yy, s);
        double s3 = s.lo;
        twoSum(x, s.hi, s);
        // (s0, s1, s2) == (s.hi, s.lo, s3)
        double s0 = s.hi;
        twoSum(s.lo, y, s);
        double s2 = s.lo;
        twoSum(s0, s.hi, s);
        // s1 = s.lo
        s0 = s.hi;
        // Compress (Schewchuk Fig. 15) (s0, s1, s2, s3) -> (s0, s1)
        fastTwoSum(s.lo, s2, s);
        final double s1 = s.hi;
        fastTwoSum(s.lo, s3, s);
        // s2 = s.hi
        s3 = s.lo;
        fastTwoSum(s1, s.hi, s);
        s2 = s.lo;
        fastTwoSum(s0, s.hi, s);
        // Here (s0, s1) = s
        // e = exact 212-bit result
        // |e - s0| <= ulp(s0)
        // |s1 + s2 + s3| <= ulp(e - s0)   (Sum magnitudes small to high)
        return fastTwoSum(s.hi, s3 + s2 + s.lo, s);
    }

    /**
     * Compute the multiplication product of {@code (x, xx)} and {@code y}.
     *
     * <p>This computes the same result as
     * {@link #multiply(double, double, double, double, SDD) multiply(x, xx, y, 0, s)}.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param y High part of y.
     * @param p Product (result).
     * @return the product
     * @see #multiply(double, double, double, double, SDD)
     */
    static SDD multiply(double x, double xx, double y, SDD p) {
        // Dekker mul2 with yy=0
        // (Alternative: Scale expansion (Schewchuk Fig 13))
        twoProd(x, y, p);
        // Save 2 FLOPS compared to multiply(x, xx, y, 0).
        // This is reused in divide to save more FLOPS so worth the optimisation.
        return fastTwoSum(p.hi, p.lo + xx * y, p);
    }

    /**
     * Compute the multiplication product of {@code (x, xx)} and {@code (y, yy)}.
     *
     * <p>The result is within the error bound {@code 16 eps^2} with {@code eps = 2^-53}.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param y High part of y.
     * @param yy Low part of y.
     * @param p Product (result).
     * @return the product
     */
    static SDD multiply(double x, double xx, double y, double yy, SDD p) {
        // Dekker mul2
        // (Alternative: Scale expansion (Schewchuk Fig 13))
        twoProd(x, y, p);
        return fastTwoSum(p.hi, p.lo + (x * yy + xx * y), p);
    }

    /**
     * Compute the multiplication product of {@code (x, xx)} and {@code (y, yy)}.
     *
     * <p>This computes the same result as
     * {@link #multiply(double, double, double, double, SDD) multiply(x, xx, y, yy,
     * p)} without checks for overflow of intermediates. An exception is if either
     * argument is a signed zero; in this case the sign of the zero result may be
     * different.
     *
     * <p>Use when the magnitude of both factors is below {@code 2^511} and the
     * product will be finite.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param y High part of y.
     * @param yy Low part of y.
     * @param p Product (result).
     * @return the product
     * @see #multiply(double, double, double, double, SDD)
     */
    static SDD uncheckedMultiply(double x, double xx, double y, double yy, SDD p) {
        // Dekker mul2
        final double hi = x * y;
        final double lo = productLowUnscaled(x, y, hi);
        return fastTwoSum(hi, lo + (x * yy + xx * y), p);
    }

    /**
     * Compute the division of {@code x} by {@code y}.
     * If {@code y = 0} the result is undefined.
     *
     * @param x x.
     * @param y y.
     * @param q Quotient (result).
     * @return the quotient
     */
    static SDD divide(double x, double y, SDD q) {
        // Long division
        // quotient q0 = x / y
        final double q0 = x / y;
        // remainder r = x - q0 * y
        twoProd(q0, y, q);
        final double p1 = q.lo;
        twoDiff(x, q.hi, q);
        q.lo -= p1;
        // correction term q1 = r0 / y
        final double q1 = q.doubleValue() / y;
        return fastTwoSum(q0, q1, q);
    }

    /**
     * Compute the division of {@code x} by {@code y}.
     * If {@code y = 0} the result is undefined.
     *
     * <p>This computes the same result as {@link #divide(double, double, SDD) divide(x, y, q)}
     * without checks for intermediate overflow. Use when the magnitude of
     * both {@code x/y} and {@code y} are below {@code 2^996}.
     *
     * @param x x.
     * @param y y.
     * @param q Quotient (result).
     * @return the quotient
     */
    static SDD uncheckedDivide(double x, double y, SDD q) {
        // Long division
        // quotient q0 = x / y
        final double q0 = x / y;
        // remainder r = x - q0 * y
        final double p0 = q0 * y;
        final double p1 = productLowUnscaled(q0, y, p0);
        twoDiff(x, p0, q);
        q.lo -= p1;
        // correction term q1 = r0 / y
        final double q1 = q.doubleValue() / y;
        return fastTwoSum(q0, q1, q);
    }

    /**
     * Compute the division of {@code (x, xx)} by {@code y}.
     * If {@code y = 0} the result is undefined.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param y High part of y.
     * @param yy Low part of y.
     * @param q Quotient (result).
     * @return the quotient
     */
    static SDD divide(double x, double xx, double y, double yy, SDD q) {
        // Long division
        // quotient q0 = x / y
        final double q0 = x / y;
        // remainder r0 = x - q0 * y
        multiply(y, yy, q0, q);
        add(-q.hi, -q.lo, x, xx, q);
        final double r = q.hi;
        final double rr = q.lo;
        // next quotient q1 = r0 / y
        final double q1 = r / y;
        // remainder r1 = r0 - q1 * y
        multiply(y, yy, q1, q);
        add(-q.hi, -q.lo, r, rr, q);
        // next quotient q2 = r1 / y
        final double q2 = q.hi / y;
        // Collect (q0, q1, q2)
        fastTwoSum(q0, q1, q);
        return twoSum(q.hi, q.lo + q2, q);
    }

    /**
     * Compute the inverse of {@code (y, yy)}.
     * If {@code y = 0} the result is undefined.
     *
     * @param y High part of y.
     * @param yy Low part of y.
     * @param q Quotient 1 / y (result).
     * @return the inverse
     */
    static SDD inverse(double y, double yy, SDD q) {
        // As per divide using (x, xx) = (1, 0)
        // quotient q0 = x / y
        final double q0 = 1 / y;
        // remainder r0 = x - q0 * y
        multiply(y, yy, q0, q);
        // This add saves 2 twoSum and 2 fastTwoSum (18 FLOPS)
        add(-q.hi, -q.lo, 1, q);
        final double r = q.hi;
        final double rr = q.lo;
        // next quotient q1 = r0 / y
        final double q1 = r / y;
        // remainder r1 = r0 - q1 * y
        multiply(y, yy, q1, q);
        add(-q.hi, -q.lo, r, rr, q);
        // next quotient q2 = r1 / y
        final double q2 = q.hi / y;
        // Collect (q0, q1, q2)
        fastTwoSum(q0, q1, q);
        return twoSum(q.hi, q.lo + q2, q);
    }

    /**
     * Multiply the floating-point number {@code x} by an integral power of two.
     *
     * <p>This performs the same result as:
     * <pre>
     * hi = Math.scalb(x, exp);
     * lo = Math.scalb(x, exp);
     * </pre>
     *
     * <p>The implementation computes this with a single multiplication if {@code exp}
     * is in {@code [-1022, 1023]}. Otherwise the pair {@code (x, xx)} are scaled by
     * repeated multiplication by power-of-two factors without any loss of precision.
     *
     * <p>This is named using the equivalent function in the standard C math.h library.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param exp Power of two scale factor.
     * @param r Result.
     * @return the result
     * @see Math#scalb(double, int)
     * @see #frexp(double, double, SDD)
     * @see <a href="https://www.cplusplus.com/reference/cmath/ldexp/">C math.h ldexp</a>
     */
    static SDD ldexp(double x, double xx, int exp, SDD r) {
        // Handle scaling when 2^n can be represented with a single normal number
        // n >= -1022 && n <= 1023
        // Using unsigned compare => n + 1022 <= 1023 + 1022
        if (exp + CMP_UNSIGNED_1022 < CMP_UNSIGNED_2046) {
            final double s = twoPow(exp);
            r.hi = x * s;
            r.lo = xx * s;
            return r;
        }

        // Scale by multiples of 2^512 (largest representable power of 2).
        // Scaling requires max 5 multiplications to under/overflow any normal value.
        // Break this down into e.g.: 2^512^(exp / 512) * 2^(exp % 512)
        // Number of multiples n = exp / 512   : exp >>> 9
        // Remainder           m = exp % 512   : exp & 511  (exp must be positive)
        int n;
        int m;
        double p;
        if (exp < 0) {
            // Downscaling
            // (Note: Using an unsigned shift handles negation of min value: -2^31)
            n = -exp >>> 9;
            // m = exp % 512
            m = -(-exp & 511);
            p = TWO_POW_M512;
        } else {
            // Upscaling
            n = exp >>> 9;
            m = exp & 511;
            p = TWO_POW_512;
        }

        // Multiply by the remainder scaling factor first. The remaining multiplications
        // are either 2^512 or 2^-512.
        // Down-scaling to sub-normal will use the final multiplication into a sub-normal result.
        // Note here that n >= 1 as the n in [-1022, 1023] case has been handled.

        // Handle n : 1, 2, 3, 4, 5
        if (n >= 5) {
            // n >= 5 will be over/underflow. Use an extreme scale factor.
            // Do not use +/- infinity as this creates NaN if x = 0.
            // p -> 2^1023 or 2^-1025
            p *= p * 0.5;
            r.hi = x * p * p * p;
            r.lo = xx * p * p * p;
            return r;
        }

        final double s = twoPow(m);
        if (n == 4) {
            r.hi = x * s * p * p * p * p;
            r.lo = xx * s * p * p * p * p;
        } else if (n == 3) {
            r.hi = x * s * p * p * p;
            r.lo = xx * s * p * p * p;
        } else if (n == 2) {
            r.hi = x * s * p * p;
            r.lo = xx * s * p * p;
        } else {
            // n = 1. Occurs only if exp = -1023.
            r.hi = x * s * p;
            r.lo = xx * s * p;
        }
        return r;
    }

    /**
     * Create a double with the value {@code 2^n}.
     *
     * <p>Warning: Do not call with {@code n = -1023}. This will create zero.
     *
     * @param n Exponent (in the range [-1022, 1023]).
     * @return the double
     */
    private static double twoPow(int n) {
        return Double.longBitsToDouble(((long) (n + 1023)) << 52);
    }

    /**
     * Convert floating-point number {@code x} to fractional {@code f} and integral
     * {@code 2^exp} components.
     * <pre>
     * x = f * 2^exp
     * </pre>
     *
     * <p>The combined fractional part (f, ff) is in the range {@code [0.5, 1)}.
     *
     * <p>Special cases:
     * <ul>
     * <li>If {@code x} is zero, then the normalized fraction is zero and the exponent is zero.
     * <li>If {@code x} is NaN, then the normalized fraction is NaN and the exponent is unspecified.
     * <li>If {@code x} is infinite, then the normalized fraction is infinite and the exponent is unspecified.
     * <li>If high-part {@code x} is an exact power of 2 and the low-part {@code xx} has an opposite
     * signed non-zero magnitude then fraction high-part {@code f} will be {@code +/-1} such that
     * the double-double number is in the range {@code [0.5, 1)}.
     * </ul>
     *
     * <p>This is named using the equivalent function in the standard C math.h library.
     *
     * <p>Note: This method returns the exponent to avoid using an {@code int[] exp} argument
     * to save the result.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param f Fraction part.
     * @return Power of two scale factor (integral exponent).
     * @see Math#getExponent(double)
     * @see #ldexp(double, double, int, SDD)
     * @see <a href="https://www.cplusplus.com/reference/cmath/frexp/">C math.h frexp</a>
     */
    static int frexp(double x, double xx, SDD f) {
        int exp = getScale(x);
        // Handle non-scalable numbers
        if (exp == Double.MAX_EXPONENT + 1) {
            // Returns +/-0.0, inf or nan
            f.hi = x;
            // Maintain the fractional part unchanged.
            // Do not change the fractional part of inf/nan, and assume
            // |xx| < |x| thus if x == 0 then xx == 0 (otherwise the double-double is invalid)
            f.lo = xx;
            // Unspecified for NaN/inf so just return zero
            return 0;
        }
        // The scale will create the fraction in [1, 2) so increase by 1 for [0.5, 1)
        exp += 1;
        ldexp(x, xx, -exp, f);
        // Return |(hi, lo)| = (1, -eps) if required.
        // f.hi * f.lo < 0 detects sign change unless the product underflows.
        // Handle extreme case of |f.lo| being min value by doubling f.hi to 1.
        if (Math.abs(f.hi) == HALF && 2 * f.hi * f.lo < 0) {
            f.hi *= 2;
            f.lo *= 2;
            exp -= 1;
        }
        return exp;
    }

    /**
     * Returns a scale suitable for use with {@link Math#scalb(double, int)} to normalise
     * the number to the interval {@code [1, 2)}.
     *
     * <p>In contrast to {@link Math#getExponent(double)} this handles
     * sub-normal numbers by computing the number of leading zeros in the mantissa
     * and shifting the unbiased exponent. The result is that for all finite, non-zero,
     * numbers, the magnitude of {@code scalb(x, -getScale(x))} is
     * always in the range {@code [1, 2)}.
     *
     * <p>This method is a functional equivalent of the c function ilogb(double).
     *
     * <p>The result is to be used to scale a number using {@link Math#scalb(double, int)}.
     * Hence the special case of a zero argument is handled using the return value for NaN
     * as zero cannot be scaled. This is different from {@link Math#getExponent(double)}.
     *
     * <p>Special cases:
     * <ul>
     * <li>If the argument is NaN or infinite, then the result is {@link Double#MAX_EXPONENT} + 1.
     * <li>If the argument is zero, then the result is {@link Double#MAX_EXPONENT} + 1.
     * </ul>
     *
     * @param a Value.
     * @return The unbiased exponent of the value to be used for scaling, or 1024 for 0, NaN or Inf
     * @see Math#getExponent(double)
     * @see Math#scalb(double, int)
     * @see <a href="https://www.cplusplus.com/reference/cmath/ilogb/">ilogb</a>
     */
    private static int getScale(double a) {
        // Only interested in the exponent and mantissa so remove the sign bit
        final long bits = Double.doubleToRawLongBits(a) & UNSIGN_MASK;
        // Get the unbiased exponent
        int exp = ((int) (bits >>> 52)) - EXPONENT_OFFSET;

        // No case to distinguish nan/inf (exp == 1024).
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
            final long mantissa = bits & MANTISSA_MASK;
            exp -= Long.numberOfLeadingZeros(mantissa << 12);
        }
        return exp;
    }

    /**
     * Compute the number {@code x} raised to the power {@code n}.
     *
     * <p>The value is returned as fractional {@code f} and integral
     * {@code 2^exp} components.
     * <pre>
     * (x+xx)^n = (f+ff) * 2^exp
     * </pre>
     *
     * <p>The combined fractional part (f, ff) is in the range {@code [0.5, 1)}.
     *
     * <p>Special cases:
     * <ul>
     * <li>If {@code (x, xx)} is zero the high part of the fractional part is
     * computed using {@link Math#pow(double, double) Math.pow(x, n)} and the exponent is 0.
     * <li>If {@code n = 0} the fractional part is 0.5 and the exponent is 1.
     * <li>If {@code (x, xx)} is an exact power of 2 the fractional part is 0.5 and the exponent
     * is the power of 2 minus 1.
     * <li>If the result high-part is an exact power of 2 and the low-part has an opposite
     * signed non-zero magnitude then the fraction high-part {@code f} will be {@code +/-1} such that
     * the double-double number is in the range {@code [0.5, 1)}.
     * <p>If the argument is not finite then a fractional representation is not possible.
     * In this case the fraction and the scale factor is undefined.
     * </ul>
     *
     * <p>Note: This method returns the exponent to avoid using an {@code long[] exp} argument
     * to save the result.
     *
     * <p>The computed result is approximately {@code 16 * (n - 1) * eps} of the exact result
     * where eps is {@code 2^-106}.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param n Power.
     * @param f Fraction part.
     * @return Power of two scale factor (integral exponent).
     * @see #frexp(double, double, SDD)
     */
    static long fastPowScaled(double x, double xx, int n, SDD f) {
        // Edge cases.
        if (n == 0) {
            f.set(0.5, 0);
            return 1;
        }
        // IEEE result for non-finite or zero
        if (!Double.isFinite(x) || x == 0) {
            f.set(Math.pow(x, n), 0);
            return 0;
        }
        // Here the number is non-zero finite
        assert x == x + xx : NOT_NOMALIZED;
        long b = frexp(x, xx, f);
        // Handle exact powers of 2
        if (Math.abs(f.hi) == HALF && f.lo == 0) {
            // (f * 2^b)^n = (2f)^n * 2^(b-1)^n
            // Use Math.pow to create the sign.
            // Note the result must be scaled to the fractional representation
            // by multiplication by 0.5 and addition of 1 to the exponent.
            f.hi = 0.5 * Math.pow(2 * f.hi, n);
            // Propagate sign change (x*f.hi) to the zero
            f.lo = Math.copySign(0.0, x * f.hi * xx);
            return 1 + (b - 1) * n;
        }
        if (n < 0) {
            b = computeFastPowScaled(b, f.hi, f.lo, -n, f);
            // Result is a non-zero fraction part so inversion is safe
            inverse(f.hi, f.lo, f);
            // Rescale to [0.5, 1.0]
            return -b + frexp(f.hi, f.lo, f);
        }
        return computeFastPowScaled(b, f.hi, f.lo, n, f);
    }

    /**
     * Compute the number {@code x} (non-zero finite) raised to the power {@code n}.
     *
     * <p>The input power is treated as an unsigned integer. Thus the negative value
     * {@link Integer#MIN_VALUE} is 2^31.
     *
     * @param exp Integral component 2^exp of x.
     * @param x Fractional high part of x.
     * @param xx Fractional low part of x.
     * @param n Power (in [2, 2^31]).
     * @param f Fraction part.
     * @return Power of two scale factor (integral exponent).
     */
    private static long computeFastPowScaled(long exp, double x, double xx, int n, SDD f) {
        // Compute the power by multiplication (keeping track of the scale):
        // 13 = 1101
        // x^13 = x^8 * x^4 * x^1
        //      = ((x^2 * x)^2)^2 * x
        // 21 = 10101
        // x^21 = x^16 * x^4 * x^1
        //      = (((x^2)^2 * x)^2)^2 * x
        // 1. Find highest set bit in n (assume n != 0)
        // 2. Initialise result as x
        // 3. For remaining bits (0 or 1) below the highest set bit:
        //    - square the current result
        //    - if the current bit is 1 then multiply by x
        // In this scheme the factors to multiply by x can be pre-computed.

        // Scale the input in [0.5, 1) to be above 1. Represented as 2^be * b.
        final long be = exp - 1;
        final double b0 = x * 2;
        final double b1 = xx * 2;
        // Split b
        final double b0h = highPartUnscaled(b0);
        final double b0l = b0 - b0h;

        // Initialise the result as x^1. Represented as 2^fe * f.
        long fe = be;
        double f0 = b0;
        double f1 = b1;

        double u;
        double v;
        double w;

        // Shift the highest set bit off the top.
        // Any remaining bits are detected in the sign bit.
        final int shift = Integer.numberOfLeadingZeros(n) + 1;
        int bits = n << shift;

        // Multiplication is done without using SDD.multiply as the arguments
        // are always finite and the product will not overflow. The square can be optimised.
        // Process remaining bits below highest set bit.
        for (int i = 32 - shift; i != 0; i--, bits <<= 1) {
            // Square the result
            // Inline multiply(f0, f1, f0, f1, f), adapted for squaring
            fe <<= 1;
            u = f0 * f0;
            v = squareLowUnscaled(f0, u);
            // Inline fastTwoSum(hi, lo + (2 * f0 * f1), f)
            w = v + (2 * f0 * f1);
            f0 = u + w;
            f1 = fastTwoSumLow(u, w, f0);
            // Rescale
            if (Math.abs(f0) > SAFE_MULTIPLY) {
                // Scale back to the [1, 2) range. As safe multiply is 2^500
                // the exponent should be < 1001 so the twoPow scaling factor is supported.
                final int e = Math.getExponent(f0);
                final double s = twoPow(-e);
                fe += e;
                f0 *= s;
                f1 *= s;
            }
            if (bits < 0) {
                // Multiply by b
                fe += be;
                // Inline multiply(f0, f1, b0, b1, f)
                u = highPartUnscaled(f0);
                v = f0 - u;
                w = f0 * b0;
                v = productLowUnscaled(u, v, b0h, b0l, w);
                // Inline fastTwoSum(w, v + (f0 * b1 + f1 * b0), f)
                u = v + (f0 * b1 + f1 * b0);
                f0 = w + u;
                f1 = fastTwoSumLow(w, u, f0);
                // Avoid rescale as x2 is in [1, 2)
            }
        }

        return fe + frexp(f0, f1, f);
    }

    /**
     * Compute the number {@code x} raised to the power {@code n}.
     *
     * <p>The value is returned as fractional {@code f} and integral
     * {@code 2^exp} components.
     * <pre>
     * (x+xx)^n = (f+ff) * 2^exp
     * </pre>
     *
     * <p>The combined fractional part (f, ff) is in the range {@code [0.5, 1)}.
     *
     * <p>Special cases:
     * <ul>
     * <li>If {@code (x, xx)} is zero the high part of the fractional part is
     * computed using {@link Math#pow(double, double) Math.pow(x, n)} and the exponent is 0.
     * <li>If {@code n = 0} the fractional part is 0.5 and the exponent is 1.
     * <li>If {@code (x, xx)} is an exact power of 2 the fractional part is 0.5 and the exponent
     * is the power of 2 minus 1.
     * <li>If the result high-part is an exact power of 2 and the low-part has an opposite
     * signed non-zero magnitude then the fraction high-part {@code f} will be {@code +/-1} such that
     * the double-double number is in the range {@code [0.5, 1)}.
     * <p>If the argument is not finite then a fractional representation is not possible.
     * In this case the fraction and the scale factor is undefined.
     * </ul>
     *
     * <p>Note: This method returns the exponent to avoid using an {@code long[] exp} argument
     * to save the result.
     *
     * <p>The computed result is within 1 ULP of the exact result where ULP is {@code 2^-106}.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param n Power.
     * @param f Fraction part.
     * @return Power of two scale factor (integral exponent).
     * @see #frexp(double, double, SDD)
     */
    static long powScaled(double x, double xx, int n, SDD f) {
        // Edge cases.
        if (n == 0) {
            f.set(0.5, 0);
            return 1;
        }
        // IEEE result for non-finite or zero
        if (!Double.isFinite(x) || x == 0) {
            f.set(Math.pow(x, n), 0);
            return 0;
        }
        // Here the number is non-zero finite
        assert x == x + xx : NOT_NOMALIZED;
        final long b = frexp(x, xx, f);
        // Handle exact powers of 2
        if (Math.abs(f.hi) == HALF && f.lo == 0) {
            // (f * 2^b)^n = (2f)^n * 2^(b-1)^n
            // Use Math.pow to create the sign.
            // Note the result must be scaled to the fractional representation
            // by multiplication by 0.5 and addition of 1 to the exponent.
            f.hi = 0.5 * Math.pow(2 * f.hi, n);
            // Propagate sign change (x*f.hi) to the zero
            f.lo = Math.copySign(0.0, x * f.hi * xx);
            return 1 + (b - 1) * n;
        }
        return computePowScaled(b, f.hi, f.lo, n, f);
    }

    /**
     * Compute the number {@code x} (non-zero finite) raised to the power {@code n}.
     *
     * <p>Performs the computation in triple-length precision. If the input power is
     * negative the result is computed using the absolute value of {@code n} and then
     * inverted by dividing into 1.
     *
     * @param exp Integral component 2^exp of x.
     * @param x Fractional high part of x.
     * @param xx Fractional low part of x.
     * @param n Power (in [2, 2^31]).
     * @param f Fraction part.
     * @return Power of two scale factor (integral exponent).
     */
    private static long computePowScaled(long exp, double x, double xx, int n, SDD f) {
        // Same as computePowScaled using a triple-double intermediate.

        // triple-double multiplication:
        // (a0, a1, a2) * (b0, b1, b2)
        // a x b ~ a0b0                 O(1) term
        //       + a0b1 + a1b0          O(eps) terms
        //       + a0b2 + a1b1 + a2b0   O(eps^2) terms
        //       + a1b2 + a2b1          O(eps^3) terms
        //       + a2b2                 O(eps^4) term  (not required for the first 159 bits)
        // Higher terms require two-prod if the round-off is <= O(eps^3).
        // (pij,qij) = two-prod(ai, bj); pij = O(eps^i+j); qij = O(eps^i+j+1)
        // p00                      O(1)
        // p01, p10, q00            O(eps)
        // p02, p11, p20, q01, q10  O(eps^2)
        // p12, p21, q02, q11, q20  O(eps^3)
        // Sum terms of the same order. Carry round-off to lower order:
        // s0 = p00                                        Order(1)
        // Sum (p01, p10, q00) -> (s1, r2, r3a)            Order(eps)
        // Sum (p02, p11, p20, q01, q10, r2) -> (s2, r3b)  Order(eps^2)
        // Sum (p12, p21, q02, q11, q20, r3a, r3b) -> s3   Order(eps^3)
        //
        // Simplifies for (b0, b1):
        // Sum (p01, p10, q00) -> (s1, r2, r3a)            Order(eps)
        // Sum (p11, p20, q01, q10, r2) -> (s2, r3b)       Order(eps^2)
        // Sum (p21, q11, q20, r3a, r3b) -> s3             Order(eps^3)
        //
        // Simplifies for the square:
        // Sum (2 * p01, q00) -> (s1, r2)                  Order(eps)
        // Sum (2 * p02, 2 * q01, p11, r2) -> (s2, r3b)    Order(eps^2)
        // Sum (2 * p12, 2 * q02, q11, r3b) -> s3          Order(eps^3)

        // Scale the input in [0.5, 1) to be above 1. Represented as 2^be * b.
        final long be = exp - 1;
        final double b0 = x * 2;
        final double b1 = xx * 2;
        // Split b
        final double b0h = highPartUnscaled(b0);
        final double b0l = b0 - b0h;
        final double b1h = highPartUnscaled(b1);
        final double b1l = b1 - b1h;

        // Initialise the result as x^1. Represented as 2^fe * f.
        long fe = be;
        double f0 = b0;
        double f1 = b1;
        double f2 = 0;

        // Shift the highest set bit off the top.
        // Any remaining bits are detected in the sign bit.
        final int an = Math.abs(n);
        final int shift = Integer.numberOfLeadingZeros(an) + 1;
        int bits = an << shift;

        // Multiplication is done inline with some triple precision helper routines.
        // Process remaining bits below highest set bit.
        for (int i = 32 - shift; i != 0; i--, bits <<= 1) {
            // Square the result
            fe <<= 1;
            double a0h = highPartUnscaled(f0);
            double a0l = f0 - a0h;
            double a1h = highPartUnscaled(f1);
            double a1l = f1 - a1h;
            double a2h = highPartUnscaled(f2);
            double a2l = f2 - a2h;
            double p00 = f0 * f0;
            double q00 = squareLowUnscaled(a0h, a0l, p00);
            double p01 = f0 * f1;
            double q01 = productLowUnscaled(a0h, a0l, a1h, a1l, p01);
            final double p02 = f0 * f2;
            final double q02 = productLowUnscaled(a0h, a0l, a2h, a2l, p02);
            double p11 = f1 * f1;
            double q11 = squareLowUnscaled(a1h, a1l, p11);
            final double p12 = f1 * f2;
            double s0 = p00;
            // Sum (2 * p01, q00) -> (s1, r2)                  Order(eps)
            double s1 = 2 * p01 + q00;
            double r2 = twoSumLow(2 * p01, q00, s1);
            // Sum (2 * p02, 2 * q01, p11, r2) -> (s2, r3b)    Order(eps^2)
            double s2 = p02 + q01;
            double r3b = twoSumLow(p02, q01, s2);
            double u = p11 + r2;
            double v = twoSumLow(p11, r2, u);
            fastAdd(2 * s2, 2 * r3b, u, v, f);
            s2 = f.hi;
            r3b = f.lo;
            // Sum (2 * p12, 2 * q02, q11, r3b) -> s3          Order(eps^3)
            double s3 = 2 * (p12 + q02) + q11 + r3b;
            f0 = norm3(s0, s1, s2, s3, f);
            f1 = f.hi;
            f2 = f.lo;

            // Rescale
            if (Math.abs(f0) > SAFE_MULTIPLY) {
                // Scale back to the [1, 2) range. As safe multiply is 2^500
                // the exponent should be < 1001 so the twoPow scaling factor is supported.
                final int e = Math.getExponent(f0);
                final double s = twoPow(-e);
                fe += e;
                f0 *= s;
                f1 *= s;
                f2 *= s;
            }

            if (bits < 0) {
                // Multiply by b
                fe += be;
                a0h = highPartUnscaled(f0);
                a0l = f0 - a0h;
                a1h = highPartUnscaled(f1);
                a1l = f1 - a1h;
                a2h = highPartUnscaled(f2);
                a2l = f2 - a2h;
                p00 = f0 * b0;
                q00 = productLowUnscaled(a0h, a0l, b0h, b0l, p00);
                p01 = f0 * b1;
                q01 = productLowUnscaled(a0h, a0l, b1h, b1l, p01);
                final double p10 = f1 * b0;
                final double q10 = productLowUnscaled(a1h, a1l, b0h, b0l, p10);
                p11 = f1 * b1;
                q11 = productLowUnscaled(a1h, a1l, b1h, b1l, p11);
                final double p20 = f2 * b0;
                final double q20 = productLowUnscaled(a2h, a2l, b0h, b0l, p20);
                final double p21 = f2 * b1;
                s0 = p00;
                // Sum (p01, p10, q00) -> (s1, r2, r3a)            Order(eps)
                u = p01 + p10;
                v = twoSumLow(p01, p10, u);
                s1 = q00 + u;
                final double w = twoSumLow(q00, u, s1);
                r2 = v + w;
                final double r3a = twoSumLow(v, w, r2);
                // Sum (p11, p20, q01, q10, r2) -> (s2, r3b)       Order(eps^2)
                s2 = p11 + p20;
                r3b = twoSumLow(p11, p20, s2);
                u = q01 + q10;
                v = twoSumLow(q01, q10, u);
                fastAdd(s2, r3b, u, v, f);
                s2 = f.hi + r2;
                r3b = twoSumLow(f.hi, r2, s2);
                // Sum (p21, q11, q20, r3a, r3b) -> s3             Order(eps^3)
                s3 = p21 + q11 + q20 + r3a + r3b;
                f0 = norm3(s0, s1, s2, s3, f);
                f1 = f.hi;
                f2 = f.lo;
                // Avoid rescale as x2 is in [1, 2)
            }
        }

        // Ensure (f0, f1) are 1 ulp exact
        final double u = f1 + f2;
        SDD.fastTwoSum(f0, u, f);

        // If the power is negative, invert in triple precision
        if (n < 0) {
            // Require the round-off
            final double v = fastTwoSumLow(f1, f2, u);
            // Result is in approximately [1, 2^501] so inversion is safe.
            inverse3(f.hi, f.lo, v, f);
            // Rescale to [0.5, 1.0]
            return -fe + frexp(f.hi, f.lo, f);
        }

        return fe + frexp(f.hi, f.lo, f);
    }

    /**
     * Normalize (s0, s1, s2, s3) to (s0, s1, s2).
     *
     * @param s0 High part of s.
     * @param s1 Second part of s.
     * @param s2 Third part of s.
     * @param s3 Fourth part of s.
     * @param s12 Output parts (s1, s2)
     * @return s0
     */
    private static double norm3(double s0, double s1, double s2, double s3, SDD s12) {
        double q;
        // Compress (Schewchuk Fig. 15) (s0, s1, s2, s3) -> (g0, g1, g2, g3)
        final double g0 = s0 + s1;
        q = fastTwoSumLow(s0, s1, g0);
        final double g1 = q + s2;
        q = fastTwoSumLow(q, s2, g1);
        final double g2 = q + s3;
        final double g3 = fastTwoSumLow(q, s3, g2);
        // (g0, g1, g2, g3) -> (h0, h1, h2, h3), returned as (h0, h1, h2 + h3)
        q = g1 + g2;
        s12.lo = fastTwoSumLow(g1, g2, q) + g3;
        final double h0 = g0 + q;
        s12.hi = fastTwoSumLow(g0, q, h0);
        return h0;
    }

    /**
     * Compute the inverse of {@code (y, yy, yyy)}.
     * If {@code y = 0} the result is undefined.
     *
     * <p>This is special routine used in {@link #powScaled(double, double, int, SDD)}
     * to invert the triple precision result.
     *
     * @param y First part of y.
     * @param yy Second part of y.
     * @param yyy Third part of y.
     * @param q Quotient 1 / y (result).
     * @return the inverse
     */
    private static SDD inverse3(double y, double yy, double yyy, SDD q) {
        // Long division (1, 0, 0) / (y, yy, yyy)
        double r;
        double rr;
        double rrr;
        double t;
        // quotient q0 = x / y
        final double q0 = 1 / y;
        // remainder r0 = x - q0 * y
        t = multiply3(y, yy, yyy, q0, q);
        r = add3(-t, -q.hi, -q.lo, 1, q);
        rr = q.hi;
        rrr = q.lo;
        // next quotient q1 = r0 / y
        final double q1 = r / y;
        // remainder r1 = r0 - q1 * y
        t = multiply3(y, yy, yyy, q1, q);
        r = add3(-t, -q.hi, -q.lo, r, rr, rrr, q);
        rr = q.hi;
        rrr = q.lo;
        // next quotient q2 = r1 / y
        final double q2 = r / y;
        // remainder r2 = r1 - q2 * y
        t = multiply3(y, yy, yyy, q2, q);
        r = add3(-t, -q.hi, -q.lo, r, rr, rrr, q);
        // next quotient q3 = r2 / y
        final double q3 = r / y;
        // Collect (q0, q1, q2, q3) to (s0, s1, s2)
        t = norm3(q0, q1, q2, q3, q);
        // Reduce to (s0, s1)
        return fastTwoSum(t, q.hi + q.lo, q);
    }

    /**
     * Compute the multiplication product of {@code (a0,a1,a2)} and {@code b}.
     *
     * @param a0 High part of a.
     * @param a1 Second part of a.
     * @param a2 Third part of a.
     * @param b Factor.
     * @param s12 Output parts (s1, s2)
     * @return s0
     */
    private static double multiply3(double a0, double a1, double a2, double b, SDD s12) {
        // Triple-Double x Double
        // a x b ~ a0b                 O(1) term
        //       + a1b                 O(eps) terms
        //       + a2b                 O(eps^2) terms
        // Higher terms require two-prod if the round-off is <= O(eps^2).
        // (pij,qij) = two-prod(ai, bj); pij = O(eps^i+j); qij = O(eps^i+j+1)
        // p00           O(1)
        // p10, q00      O(eps)
        // p20, q10      O(eps^2)
        // |a2| < |eps^2 a0| => |a2 * b| < eps^2 |a0 * b| and q20 < eps^3 |a0 * b|
        //
        // Sum terms of the same order. Carry round-off to lower order:
        // s0 = p00                              Order(1)
        // Sum (p10, q00) -> (s1, r1)            Order(eps)
        // Sum (p20, q10, r1) -> (s2, s3)        Order(eps^2)
        final double a0h = highPartUnscaled(a0);
        final double a0l = a0 - a0h;
        final double a1h = highPartUnscaled(a1);
        final double a1l = a1 - a1h;
        final double b0h = highPartUnscaled(b);
        final double b0l = b - b0h;
        final double p00 = a0 * b;
        final double q00 = productLowUnscaled(a0h, a0l, b0h, b0l, p00);
        final double p10 = a1 * b;
        final double q10 = productLowUnscaled(a1h, a1l, b0h, b0l, p10);
        final double p20 = a2 * b;
        // Sum (p10, q00) -> (s1, r1)            Order(eps)
        final double s1 = p10 + q00;
        final double r1 = twoSumLow(p10, q00, s1);
        // Sum (p20, q10, r1) -> (s2, s3)        Order(eps^2)
        double u = p20 + q10;
        final double v = twoSumLow(p20, q10, u);
        final double s2 = u + r1;
        u = twoSumLow(u, r1, s2);
        return norm3(p00, s1, s2, v + u, s12);
    }

    /**
     * Compute the sum of {@code (a0,a1,a2)} and {@code b}.
     *
     * @param a0 High part of a.
     * @param a1 Second part of a.
     * @param a2 Third part of a.
     * @param b Addend.
     * @param s12 Output parts (s1, s2)
     * @return s0
     */
    private static double add3(double a0, double a1, double a2, double b, SDD s12) {
        // Hide et al (2008) Fig.5: Quad-Double + Double without final a3.
        double u;
        double v;
        final double s0 = a0 + b;
        u = twoSumLow(a0, b, s0);
        final double s1 = a1 + u;
        v = twoSumLow(a1, u, s1);
        final double s2 = a2 + v;
        u = twoSumLow(a2, v, s2);
        return norm3(s0, s1, s2, u, s12);
    }

    /**
     * Compute the sum of {@code (a0,a1,a2)} and {@code (b0,b1,b2))}.
     * It is assumed the absolute magnitudes of a and b are equal and the sign
     * of a and b are opposite.
     *
     * @param a0 High part of a.
     * @param a1 Second part of a.
     * @param a2 Third part of a.
     * @param b0 High part of b.
     * @param b1 Second part of b.
     * @param b2 Third part of b.
     * @param s12 Output parts (s1, s2)
     * @return s0
     */
    private static double add3(double a0, double a1, double a2, double b0, double b1, double b2, SDD s12) {
        // Hide et al (2008) Fig.6: Quad-Double + Quad-Double without final a3, b3.
        double u;
        double v;
        // a0 + b0 -> (s0, r1)
        final double s0 = a0 + b0;
        final double r1 = twoSumLow(a0, b0, s0);
        // a1 + b1 + r1 -> (s1, r2, r3)
        u = a1 + b1;
        v = twoSumLow(a1, b1, u);
        final double s1 = r1 + u;
        u = twoSumLow(r1, u, s1);
        final double r2 = v + u;
        final double r3 = twoSumLow(v, u, r2);
        // (a2 + b2 + r2) + r3 -> (s2, s3)
        u = a2 + b2;
        v = twoSumLow(a2, b2, u);
        final double s2 = r2 + u;
        u = twoSumLow(r2, u, s2);
        final double s3 = v + u + r3;
        return norm3(s0, s1, s2, s3, s12);
    }
}
