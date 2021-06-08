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
package org.apache.commons.numbers.core;

/**
 * Computes extended precision floating-point operations.
 *
 * <p>It is based on the 1971 paper
 * <a href="https://doi.org/10.1007/BF01397083">
 * Dekker (1971) A floating-point technique for extending the available precision</a>.
 */
final class ExtendedPrecision {
    /*
     * Caveat:
     *
     * The code below uses many additions/subtractions that may
     * appear redundant. However, they should NOT be simplified, as they
     * do use IEEE754 floating point arithmetic rounding properties.
     *
     * Algorithms are based on computing the product or sum of two values x and y in
     * extended precision. The standard result is stored using a double (high part z) and
     * the round-off error (or low part zz) is stored in a second double, e.g:
     * x * y = (z, zz); z + zz = x * y
     * x + y = (z, zz); z + zz = x + y
     *
     * To sum multiple (z, zz) results ideally the parts are sorted in order of
     * non-decreasing magnitude and summed. This is exact if each number's most significant
     * bit is below the least significant bit of the next (i.e. does not
     * overlap). Creating non-overlapping parts requires a rebalancing
     * of adjacent pairs using a summation z + zz = (z1, zz1) iteratively through the parts
     * (see Shewchuk (1997) Grow-Expansion and Expansion-Sum [1]).
     *
     * [1] Shewchuk (1997): Arbitrary Precision Floating-Point Arithmetic
     * http://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps
     */

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

    /** Private constructor. */
    private ExtendedPrecision() {
        // intentionally empty.
    }

    /**
     * Compute the low part of the double length number {@code (z,zz)} for the exact
     * product of {@code x} and {@code y}. This is equivalent to computing a {@code double}
     * containing the magnitude of the rounding error when converting the exact 106-bit
     * significand of the multiplication result to a 53-bit significand.
     *
     * <p>The method is written to be functionally similar to using a fused multiply add (FMA)
     * operation to compute the low part, for example JDK 9's Math.fma function (note the sign
     * change in the input argument for the product):
     * <pre>
     *  double x = ...;
     *  double y = ...;
     *  double xy = x * y;
     *  double low1 = Math.fma(x, y, -xy);
     *  double low2 = productLow(x, y, xy);
     * </pre>
     *
     * <p>Special cases:
     *
     * <ul>
     *  <li>If {@code x * y} is sub-normal or zero then the result is 0.0.
     *  <li>If {@code x * y} is infinite or NaN then the result is NaN.
     * </ul>
     *
     * @param x First factor.
     * @param y Second factor.
     * @param xy Product of the factors (x * y).
     * @return the low part of the product double length number
     * @see #highPartUnscaled(double)
     */
    static double productLow(double x, double y, double xy) {
        // Verify the input. This must be NaN safe.
        //assert Double.compare(x * y, xy) == 0

        // If the number is sub-normal, inf or nan there is no round-off.
        if (isNotNormal(xy)) {
            // Returns 0.0 for sub-normal xy, otherwise NaN for inf/nan:
            return xy - xy;
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
                return productLowUnscaled(x * DOWN_SCALE, y, xy * DOWN_SCALE) * UP_SCALE;
            }
            return productLowUnscaled(x, y * DOWN_SCALE, xy * DOWN_SCALE) * UP_SCALE;
        }

        // No scaling required
        return productLowUnscaled(x, y, xy);
    }

    /**
     * Checks if the number is not normal. This is functionally equivalent to:
     * <pre>
     * final double abs = Math.abs(a);
     * return (abs <= Double.MIN_NORMAL || !(abs <= Double.MAX_VALUE));
     * </pre>
     *
     * @param a The value.
     * @return true if the value is not normal
     */
    static boolean isNotNormal(double a) {
        // Sub-normal numbers have a biased exponent of 0.
        // Inf/NaN numbers have a biased exponent of 2047.
        // Catch both cases by extracting the raw exponent, subtracting 1
        // and compare unsigned (so 0 underflows to a unsigned large value).
        final int baisedExponent = ((int) (Double.doubleToRawLongBits(a) >>> 52)) & EXP_MASK;
        // Pre-compute the additions used by Integer.compareUnsigned
        return baisedExponent + CMP_UNSIGNED_MINUS_1 >= CMP_UNSIGNED_2046;
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
     * @see #productLow(double, double, double, double, double)
     */
    private static double productLowUnscaled(double x, double y, double xy) {
        // Split the numbers using Dekker's algorithm without scaling
        final double hx = highPartUnscaled(x);
        final double lx = x - hx;

        final double hy = highPartUnscaled(y);
        final double ly = y - hy;

        return productLow(hx, lx, hy, ly, xy);
    }

    /**
     * Compute the low part of the double length number {@code (z,zz)} for the exact
     * square of {@code x} using Dekker's mult12 algorithm. The standard precision product
     * {@code x*x} must be provided. The number {@code x} is split into high and low parts
     * using Dekker's algorithm.
     *
     * <p>Warning: This method does not perform scaling in Dekker's split and large
     * finite numbers can create NaN results.
     *
     * @param x Number to square
     * @param xx Standard precision product {@code x*x}
     * @return the low part of the square double length number
     */
    static double squareLowUnscaled(double x, double xx) {
        // Split the numbers using Dekker's algorithm without scaling
        final double hx = highPartUnscaled(x);
        final double lx = x - hx;

        return productLow(hx, lx, hx, lx, xx);
    }

    /**
     * Compute the low part of the double length number {@code (z,zz)} for the exact
     * product of {@code x} and {@code y} using Dekker's mult12 algorithm. The standard
     * precision product {@code x*y} must be provided. The numbers {@code x} and {@code y}
     * should already be split into low and high parts.
     *
     * <p>Note: This uses the high part of the result {@code (z,zz)} as {@code x * y} and not
     * {@code hx * hy + hx * ty + tx * hy} as specified in Dekker's original paper.
     * See Shewchuk (1997) for working examples.
     *
     * @param hx High part of first factor.
     * @param lx Low part of first factor.
     * @param hy High part of second factor.
     * @param ly Low part of second factor.
     * @param xy Product of the factors.
     * @return <code>lx * ly - (((xy - hx * hy) - lx * hy) - hx * ly)</code>
     * @see <a href="http://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps">
     * Shewchuk (1997) Theorum 18</a>
     */
    private static double productLow(double hx, double lx, double hy, double ly, double xy) {
        // Compute the multiply low part:
        // err1 = xy - hx * hy
        // err2 = err1 - lx * hy
        // err3 = err2 - hx * ly
        // low = lx * ly - err3
        return lx * ly - (((xy - hx * hy) - lx * hy) - hx * ly);
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
    static double highPartUnscaled(double value) {
        final double c = MULTIPLIER * value;
        return c - (c - value);
    }

    /**
     * Compute the round-off from the sum of two numbers {@code a} and {@code b} using
     * Knuth's two-sum algorithm. The values are not required to be ordered by magnitude.
     * The standard precision sum must be provided.
     *
     * @param a First part of sum.
     * @param b Second part of sum.
     * @param sum Sum of the parts (a + b).
     * @return <code>(b - (sum - (sum - b))) + (a - (sum - b))</code>
     * @see <a href="http://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps">
     * Shewchuk (1997) Theorum 7</a>
     */
    static double twoSumLow(double a, double b, double sum) {
        final double bVirtual = sum - a;
        // sum - bVirtual == aVirtual.
        // a - aVirtual == a round-off
        // b - bVirtual == b round-off
        return (a - (sum - bVirtual)) + (b - bVirtual);
    }
}
