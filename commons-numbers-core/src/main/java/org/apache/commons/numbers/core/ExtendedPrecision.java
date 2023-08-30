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
 * <p>Extended precision computation is delegated to the {@link DD} class. The methods here
 * are extensions to prevent overflow or underflow in intermediate computations.
 */
final class ExtendedPrecision {
    /**
     * The upper limit above which a number may overflow during the split into a high part.
     * Assuming the multiplier is above 2^27 and the maximum exponent is 1023 then a safe
     * limit is a value with an exponent of (1023 - 27) = 2^996.
     * 996 is the value obtained from {@code Math.getExponent(Double.MAX_VALUE / MULTIPLIER)}.
     */
    private static final double SAFE_UPPER = 0x1.0p996;
    /**
     * The lower limit for a product {@code x * y} below which the round-off component may be
     * sub-normal. This is set as 2^-1022 * 2^54.
     */
    private static final double SAFE_LOWER = 0x1.0p-968;

    /** The scale to use when down-scaling during a split into a high part.
     * This must be smaller than the inverse of the multiplier and a power of 2 for exact scaling. */
    private static final double DOWN_SCALE = 0x1.0p-30;
    /** The scale to use when up-scaling during a split into a high part.
     * This is the inverse of {@link #DOWN_SCALE}. */
    private static final double UP_SCALE = 0x1.0p30;

    /** The upscale factor squared. */
    private static final double UP_SCALE2 = 0x1.0p60;
    /** The downscale factor squared. */
    private static final double DOWN_SCALE2 = 0x1.0p-60;

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
     * <p>This method delegates to {@link DD#twoProductLow(double, double, double)} but uses
     * scaling to avoid intermediate overflow.
     *
     * @param x First factor.
     * @param y Second factor.
     * @param xy Product of the factors (x * y).
     * @return the low part of the product double length number
     * @see DD#twoProductLow(double, double, double)
     */
    static double productLow(double x, double y, double xy) {
        // Verify the input. This must be NaN safe.
        //assert Double.compare(x * y, xy) == 0

        // If the number is sub-normal, inf or nan there is no round-off.
        if (DD.isNotNormal(xy)) {
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
        final double ab = Math.abs(xy);
        if (a + b + ab >= SAFE_UPPER) {
            // Only required to scale the largest number as x*y does not overflow.
            if (a > b) {
                return DD.twoProductLow(x * DOWN_SCALE, y, xy * DOWN_SCALE) * UP_SCALE;
            }
            return DD.twoProductLow(x, y * DOWN_SCALE, xy * DOWN_SCALE) * UP_SCALE;
        }

        // The result is computed using a product of the low parts.
        // To avoid underflow in the low parts we note that these are approximately a factor
        // of 2^27 smaller than the original inputs so their product will be ~2^54 smaller
        // than the product xy. Ensure the product is at least 2^54 above a sub-normal.
        if (ab <= SAFE_LOWER) {
            // Scaling up here is safe: the largest magnitude cannot be above SAFE_LOWER / MIN_VALUE.
            return DD.twoProductLow(x * UP_SCALE, y * UP_SCALE, xy * UP_SCALE2) * DOWN_SCALE2;
        }

        // No scaling required
        return DD.twoProductLow(x, y, xy);
    }
}
