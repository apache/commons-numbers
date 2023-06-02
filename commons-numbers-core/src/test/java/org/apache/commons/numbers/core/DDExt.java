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
 * Computes double-double floating-point operations.
 *
 * <p>This class contains extension methods to supplement the functionality in {@link DD}.
 * The methods are tested in {@link DDTest}. These include:
 * <ul>
 *  <li>Arithmetic operations that have 105+ bits of precision and are typically 2-3 bits more
 *      accurate than the versions in {@link DD}.
 *  <li>A power function based on {@link Math#pow(double, double)} with approximately 50+ bits of
 *      precision.
 * </ul>
 *
 * <p><b>Note</b>
 *
 * <p>This class is public and has public methods to allow testing within the examples JMH module.
 *
 * @since 1.2
 */
public final class DDExt {
    /** Threshold for large n where the Taylor series for (1+z)^n is not applicable. */
    private static final int LARGE_N = 100000000;
    /** Threshold for (x, xx)^n where x=0.5 and low-part will not be sub-normal.
     * Note x has an exponent of -1; xx of -54 (if normalized); the min normal exponent is -1022;
     * add 10-bits headroom in case xx is below epsilon * x: 1022 - 54 - 10. 0.5^958 = 4.1e-289. */
    private static final int SAFE_EXPONENT_F = 958;
    /** Threshold for (x, xx)^n where n ~ 2^31 and low-part will not be sub-normal.
     * x ~ exp(log(2^-958) / 2^31).
     * Note: floor(-958 * ln(2) / ln(nextDown(SAFE_F))) < 2^31. */
    private static final double SAFE_F = 0.9999996907846553;
    /** Threshold for (x, xx)^n where x=2 and high-part is finite.
     * For consistency we use 10-bits headroom down from max exponent 1023. 0.5^1013 = 8.78e304. */
    private static final int SAFE_EXPONENT_2F = 1013;
    /** Threshold for (x, xx)^n where n ~ 2^31 and high-part is finite.
     * x ~ exp(log(2^1013) / 2^31)
     * Note: floor(1013 * ln(2) / ln(nextUp(SAFE_2F))) < 2^31. */
    private static final double SAFE_2F = 1.0000003269678954;
    /** log(2) (20-digits). */
    private static final double LN2 = 0.69314718055994530941;
    /** sqrt(0.5) == 1 / sqrt(2). */
    private static final double ROOT_HALF = 0.707106781186547524400;
    /** The limit for safe multiplication of {@code x*y}, assuming values above 1.
     * Used to maintain positive values during the power computation. */
    private static final double SAFE_MULTIPLY = 0x1.0p500;
    /** Used to downscale values before multiplication. Downscaling of any value
     * strictly above SAFE_MULTIPLY will be above 1 even including a double-double
     * roundoff that lowers the magnitude. */
    private static final double SAFE_MULTIPLY_DOWNSCALE = 0x1.0p-500;

    /**
     * No instances.
     */
    private DDExt() {}

    /**
     * Compute the sum of {@code x} and {@code y}.
     *
     * <p>This computes the same result as
     * {@link #add(DD, DD) add(x, DD.of(y))}.
     *
     * <p>The performance is approximately 1.5-fold slower than {@link DD#add(double)}.
     *
     * @param x x.
     * @param y y.
     * @return the sum
     */
    public static DD add(DD x, double y) {
        return DD.accurateAdd(x.hi(), x.lo(), y);
    }

    /**
     * Compute the sum of {@code x} and {@code y}.
     *
     * <p>The high-part of the result is within 1 ulp of the true sum {@code e}.
     * The low-part of the result is within 1 ulp of the result of the high-part
     * subtracted from the true sum {@code e - hi}.
     *
     * <p>The performance is approximately 2-fold slower than {@link DD#add(DD)}.
     *
     * @param x x.
     * @param y y.
     * @return the sum
     */
    public static DD add(DD x, DD y) {
        return DD.accurateAdd(x.hi(), x.lo(), y.hi(), y.lo());
    }

    /**
     * Compute the subtraction of {@code y} from {@code x}.
     *
     * <p>This computes the same result as
     * {@link #add(DD, double) add(x, -y)}.
     *
     * @param x x.
     * @param y y.
     * @return the difference
     */
    public static DD subtract(DD x, double y) {
        return DD.accurateAdd(x.hi(), x.lo(), -y);
    }

    /**
     * Compute the subtraction of {@code y} from {@code x}.
     *
     * <p>This computes the same result as
     * {@link #add(DD, DD) add(x, y.negate())}.
     *
     * @param x x.
     * @param y y.
     * @return the difference
     */
    public static DD subtract(DD x, DD y) {
        return DD.accurateAdd(x.hi(), x.lo(), -y.hi(), -y.lo());
    }

    /**
     * Compute the multiplication product of {@code x} and {@code y}.
     *
     * <p>The computed result is within 0.5 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * <p>The performance is approximately 2.5-fold slower than {@link DD#multiply(double)}.
     *
     * @param x x.
     * @param y y.
     * @return the product
     */
    public static DD multiply(DD x, double y) {
        return accurateMultiply(x.hi(), x.lo(), y);
    }

    /**
     * Compute the multiplication product of {@code (x, xx)} and {@code y}.
     *
     * <p>The computed result is within 0.5 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * <p>The performance is approximately 2.5-fold slower than {@link DD#multiply(double)}.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param y y.
     * @return the product
     */
    private static DD accurateMultiply(double x, double xx, double y) {
        // For working see accurateMultiply(double x, double xx, double y, double yy)

        final double xh = DD.highPart(x);
        final double xl = x - xh;
        final double xxh = DD.highPart(xx);
        final double xxl = xx - xxh;
        final double yh = DD.highPart(y);
        final double yl = y - yh;

        final double p00 = x * y;
        final double q00 = DD.twoProductLow(xh, xl, yh, yl, p00);
        final double p10 = xx * y;
        final double q10 = DD.twoProductLow(xxh, xxl, yh, yl, p10);

        // The code below collates the O(eps) terms with a round-off
        // so O(eps^2) terms can be added to it.

        final double s0 = p00;
        // Sum (p10, q00) -> (s1, r2)       Order(eps)
        final double s1 = p10 + q00;
        final double r2 = DD.twoSumLow(p10, q00, s1);

        // Collect (s0, s1, r2 + q10)
        final double u = s0 + s1;
        final double v = DD.fastTwoSumLow(s0, s1, u);
        return DD.fastTwoSum(u, r2 + q10 + v);
    }

    /**
     * Compute the multiplication product of {@code x} and {@code y}.
     *
     * <p>The computed result is within 0.5 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * <p>The performance is approximately 4.5-fold slower than {@link DD#multiply(DD)}.
     *
     * @param x x.
     * @param y y.
     * @return the product
     */
    public static DD multiply(DD x, DD y) {
        return accurateMultiply(x.hi(), x.lo(), y.hi(), y.lo());
    }

    /**
     * Compute the multiplication product of {@code (x, xx)} and {@code (y, yy)}.
     *
     * <p>The computed result is within 0.5 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * <p>The performance is approximately 4.5-fold slower than {@link DD#multiply(DD)}.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param y High part of y.
     * @param yy Low part of y.
     * @return the product
     */
    private static DD accurateMultiply(double x, double xx, double y, double yy) {
        // double-double multiplication:
        // (a0, a1) * (b0, b1)
        // a x b ~ a0b0                 O(1) term
        //       + a0b1 + a1b0          O(eps) terms
        //       + a1b1                 O(eps^2) term
        // Higher terms require two-prod if the round-off is <= O(eps^2).
        // (pij,qij) = two-prod(ai, bj); pij = O(eps^i+j); qij = O(eps^i+j+1)
        // p00               O(1)
        // p01, p10, q00     O(eps)
        // p11, q01, q10     O(eps^2)
        // q11               O(eps^3)   (not required for the first 106 bits)
        // Sum terms of the same order. Carry round-off to lower order:
        // s0 = p00                              Order(1)
        // Sum (p01, p10, q00) -> (s1, r2)       Order(eps)
        // Sum (p11, q01, q10, r2) -> s2         Order(eps^2)

        final double xh = DD.highPart(x);
        final double xl = x - xh;
        final double xxh = DD.highPart(xx);
        final double xxl = xx - xxh;
        final double yh = DD.highPart(y);
        final double yl = y - yh;
        final double yyh = DD.highPart(yy);
        final double yyl = yy - yyh;

        final double p00 = x * y;
        final double q00 = DD.twoProductLow(xh, xl, yh, yl, p00);
        final double p01 = x * yy;
        final double q01 = DD.twoProductLow(xh, xl, yyh, yyl, p01);
        final double p10 = xx * y;
        final double q10 = DD.twoProductLow(xxh, xxl, yh, yl, p10);
        final double p11 = xx * yy;

        // Note: Directly adding same order terms (error = 2 eps^2):
        // DD.fastTwoSum(p00, (p11 + q01 + q10) + (p01 + p10 + q00))

        // The code below collates the O(eps) terms with a round-off
        // so O(eps^2) terms can be added to it.

        final double s0 = p00;
        // Sum (p01, p10, q00) -> (s1, r2)       Order(eps)
        double u = p01 + p10;
        double v = DD.twoSumLow(p01, p10, u);
        final double s1 = q00 + u;
        final double w = DD.twoSumLow(q00, u, s1);
        final double r2 = v + w;

        // Collect (s0, s1, r2 + p11 + q01 + q10)
        u = s0 + s1;
        v = DD.fastTwoSumLow(s0, s1, u);
        return DD.fastTwoSum(u, r2 + p11 + q01 + q10 + v);
    }

    /**
     * Compute the square of {@code x}.
     *
     * <p>The computed result is within 0.5 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * <p>The performance is approximately 4.5-fold slower than {@link DD#square()}.
     *
     * @param x x.
     * @return the square
     */
    public static DD square(DD x) {
        return accurateSquare(x.hi(), x.lo());
    }

    /**
     * Compute the square of {@code (x, xx)}.
     *
     * <p>The computed result is within 0.5 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * <p>The performance is approximately 4.5-fold slower than {@link DD#square()}.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @return the square
     */
    private static DD accurateSquare(double x, double xx) {
        // For working see accurateMultiply(double x, double xx, double y, double yy)

        final double xh = DD.highPart(x);
        final double xl = x - xh;
        final double xxh = DD.highPart(xx);
        final double xxl = xx - xxh;

        final double p00 = x * x;
        final double q00 = DD.twoSquareLow(xh, xl, p00);
        final double p01 = x * xx;
        final double q01 = DD.twoProductLow(xh, xl, xxh, xxl, p01);
        final double p11 = xx * xx;

        // The code below collates the O(eps) terms with a round-off
        // so O(eps^2) terms can be added to it.

        final double s0 = p00;
        // Sum (p01, p10, q00) -> (s1, r2)       Order(eps)
        final double s1 = q00 + 2 * p01;
        final double r2 = DD.twoSumLow(q00, 2 * p01, s1);

        // Collect (s0, s1, r2 + p11 + q01 + q10)
        final double u = s0 + s1;
        final double v = DD.fastTwoSumLow(s0, s1, u);
        return DD.fastTwoSum(u, r2 + p11 + 2 * q01 + v);
    }

    /**
     * Compute the division of {@code x} by {@code y}.
     * If {@code y = 0} the result is undefined.
     *
     * <p>The computed result is within 1 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * <p>The performance is approximately 1.25-fold slower than {@link DD#divide(DD)}.
     * Note that division is an order of magnitude slower than multiplication and the
     * absolute performance difference is significant.
     *
     * @param x x.
     * @param y y.
     * @return the quotient
     */
    public static DD divide(DD x, DD y) {
        return accurateDivide(x.hi(), x.lo(), y.hi(), y.lo());
    }

    /**
     * Compute the division of {@code (x, xx)} by {@code y}.
     * If {@code y = 0} the result is undefined.
     *
     * <p>The computed result is within 1 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * <p>The performance is approximately 1.25-fold slower than {@link DD#divide(DD)}.
     * Note that division is an order of magnitude slower than multiplication and the
     * absolute performance difference is significant.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param y High part of y.
     * @param yy Low part of y.
     * @return the quotient
     */
    private static DD accurateDivide(double x, double xx, double y, double yy) {
        // Long division
        // quotient q0 = x / y
        final double q0 = x / y;
        // remainder r0 = x - q0 * y
        DD p = accurateMultiply(y, yy, q0);
        DD r = DD.accurateAdd(x, xx, -p.hi(), -p.lo());
        // next quotient q1 = r0 / y
        final double q1 = r.hi() / y;
        // remainder r1 = r0 - q1 * y
        p = accurateMultiply(y, yy, q1);
        r = DD.accurateAdd(r.hi(), r.lo(), -p.hi(), -p.lo());
        // next quotient q2 = r1 / y
        final double q2 = r.hi() / y;
        // Collect (q0, q1, q2)
        final DD q = DD.fastTwoSum(q0, q1);
        return DD.twoSum(q.hi(), q.lo() + q2);
    }

    /**
     * Compute the inverse of {@code y}.
     * If {@code y = 0} the result is undefined.
     *
     * <p>The computed result is within 1 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * <p>The performance is approximately 2-fold slower than {@link DD#reciprocal()}.
     *
     * @param y y.
     * @return the inverse
     */
    public static DD reciprocal(DD y) {
        return accurateReciprocal(y.hi(), y.lo());
    }

    /**
     * Compute the inverse of {@code (y, yy)}.
     * If {@code y = 0} the result is undefined.
     *
     * <p>The computed result is within 1 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * <p>The performance is approximately 2-fold slower than {@link DD#reciprocal()}.
     *
     * @param y High part of y.
     * @param yy Low part of y.
     * @return the inverse
     */
    private static DD accurateReciprocal(double y, double yy) {
        // As per divide using (x, xx) = (1, 0)
        // quotient q0 = x / y
        final double q0 = 1 / y;
        // remainder r0 = x - q0 * y
        DD p = accurateMultiply(y, yy, q0);
        // High accuracy add required
        // This add saves 2 twoSum and 3 fastTwoSum (24 FLOPS) by ignoring the zero low part
        DD r = DD.accurateAdd(-p.hi(), -p.lo(), 1);
        // next quotient q1 = r0 / y
        final double q1 = r.hi() / y;
        // remainder r1 = r0 - q1 * y
        p = accurateMultiply(y, yy, q1);
        // accurateAdd not used as we do not need r1.xx()
        r = DD.accurateAdd(r.hi(), r.lo(), -p.hi(), -p.lo());
        // next quotient q2 = r1 / y
        final double q2 = r.hi() / y;
        // Collect (q0, q1, q2)
        final DD q = DD.fastTwoSum(q0, q1);
        return DD.twoSum(q.hi(), q.lo() + q2);
    }

    /**
     * Compute the square root of {@code x}.
     *
     * <p>Uses the result {@code Math.sqrt(x)}
     * if that result is not a finite normalized {@code double}.
     *
     * <p>Special cases:
     * <ul>
     *  <li>If {@code x} is NaN or less than zero, then the result is {@code (NaN, 0)}.
     *  <li>If {@code x} is positive infinity, then the result is {@code (+infinity, 0)}.
     *  <li>If {@code x} is positive zero or negative zero, then the result is {@code (x, 0)}.
     * </ul>
     *
     * <p>The computed result is within 1 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * <p>The performance is approximately 5.5-fold slower than {@link DD#sqrt()}.
     *
     * @param x x.
     * @return {@code sqrt(x)}
     * @see Math#sqrt(double)
     * @see Double#MIN_NORMAL
     */
    public static DD sqrt(DD x) {
        // Standard sqrt
        final DD c = x.sqrt();

        // Here we support {negative, +infinity, nan and zero} edge cases.
        // (This is the same condition as in DD.sqrt)
        if (DD.isNotNormal(c.hi())) {
            return c;
        }

        // Repeat Dekker's iteration from DD.sqrt with an accurate DD square.
        // Using an accurate sum for cc does not improve accuracy.
        final DD u = square(c);
        final double cc = (x.hi() - u.hi() - u.lo() + x.lo()) * 0.5 / c.hi();
        return DD.fastTwoSum(c.hi(), c.lo() + cc);
    }

    /**
     * Compute the number {@code x} raised to the power {@code n}.
     *
     * <p>This uses the powDSimple algorithm of van Mulbregt [1] which applies a Taylor series
     * adjustment to the result of {@code x^n}:
     * <pre>
     * (x+xx)^n = x^n * (1 + xx/x)^n
     *          = x^n + x^n * (exp(n log(1 + xx/x)) - 1)
     * </pre>
     *
     * <ol>
     * <li>
     * van Mulbregt, P. (2018).
     * <a href="https://doi.org/10.48550/arxiv.1802.06966">Computing the Cumulative Distribution
     * Function and Quantiles of the One-sided Kolmogorov-Smirnov Statistic</a>
     * arxiv:1802.06966.
     * </ol>
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param n Power.
     * @return the result
     * @see Math#pow(double, double)
     */
    public static DD simplePow(double x, double xx, int n) {
        // Edge cases. These ignore (x, xx) = (+/-1, 0). The result is Math.pow(x, n).
        if (n == 0) {
            return DD.ONE;
        }
        // IEEE result for non-finite or zero
        if (!Double.isFinite(x) || x == 0) {
            return DD.of(Math.pow(x, n));
        }
        // Here the number is non-zero finite
        if (n < 0) {
            DD r = computeSimplePow(x, xx, -1L * n);
            // Safe inversion of small/large values. Reuse the existing multiply scaling factors.
            // 1 / x = b * 1 / bx
            if (Math.abs(r.hi()) < SAFE_MULTIPLY_DOWNSCALE) {
                r = DD.of(r.hi() * SAFE_MULTIPLY, r.lo() * SAFE_MULTIPLY).reciprocal();
                final double hi = r.hi() * SAFE_MULTIPLY;
                // Return signed zero by multiplication for infinite
                final double lo = r.lo() * (Double.isInfinite(hi) ? 0 : SAFE_MULTIPLY);
                return DD.of(hi, lo);
            }
            if (Math.abs(r.hi()) > SAFE_MULTIPLY) {
                r = DD.of(r.hi() * SAFE_MULTIPLY_DOWNSCALE, r.lo() * SAFE_MULTIPLY_DOWNSCALE).reciprocal();
                final double hi = r.hi() * SAFE_MULTIPLY_DOWNSCALE;
                final double lo = r.lo() * SAFE_MULTIPLY_DOWNSCALE;
                return DD.of(hi, lo);
            }
            return r.reciprocal();
        }
        return computeSimplePow(x, xx, n);
    }

    /**
     * Compute the number {@code x} raised to the power {@code n} (must be strictly positive).
     *
     * <p>This method exists to allow negation of the power when it is {@link Integer#MIN_VALUE}
     * by casting to a long. It is called directly by simplePow and computeSimplePowScaled
     * when the arguments have already been validated.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param n Power (must be positive).
     * @return the result
     */
    private static DD computeSimplePow(double x, double xx, long n) {
        final double y = Math.pow(x, n);
        final double z = xx / x;
        // Taylor series: (1 + z)^n = n*z * (1 + ((n-1)*z/2))
        // Applicable when n*z is small.
        // Assume xx < epsilon * x.
        // n > 1e8 => n * xx/x > 1e8 * xx/x == n*z > 1e8 * 1e-16 > 1e-8
        double w;
        if (n > LARGE_N) {
            w = Math.expm1(n * Math.log1p(z));
        } else {
            w = n * z * (1 + (n - 1) * z * 0.5);
        }
        // w ~ (1+z)^n : z ~ 2^-53
        // Math.pow(1 + 2 * Math.ulp(1.0), 2^31) ~ 1.0000000000000129
        // Math.pow(1 - 2 * Math.ulp(1.0), 2^31) ~ 0.9999999999999871
        // If (x, xx) is normalized a fast-two-sum can be used.
        // fast-two-sum propagates sign changes for input of (+/-1.0, +/-0.0) (two-sum does not).
        return DD.fastTwoSum(y, y * w);
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
     *
     * <ul>
     *  <li>If {@code (x, xx)} is zero the high part of the fractional part is
     *      computed using {@link Math#pow(double, double) Math.pow(x, n)} and the exponent is 0.
     *  <li>If {@code n = 0} the fractional part is 0.5 and the exponent is 1.
     *  <li>If {@code (x, xx)} is an exact power of 2 the fractional part is 0.5 and the exponent
     *      is the power of 2 minus 1.
     *  <li>If the result high-part is an exact power of 2 and the low-part has an opposite
     *      signed non-zero magnitude then the fraction high-part {@code f} will be {@code +/-1} such that
     *      the double-double number is in the range {@code [0.5, 1)}.
     *  <li>If the argument is not finite then a fractional representation is not possible.
     *      In this case the fraction and the scale factor is undefined.
     * </ul>
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param n Power.
     * @param exp Power of two scale factor (integral exponent).
     * @return Fraction part.
     * @see #simplePow(double, double, int)
     * @see DD#frexp(int[])
     */
    public static DD simplePowScaled(double x, double xx, int n, long[] exp) {
        // Edge cases.
        if (n == 0) {
            exp[0] = 1;
            return DD.of(0.5);
        }
        // IEEE result for non-finite or zero
        if (!Double.isFinite(x) || x == 0) {
            exp[0] = 0;
            return DD.of(Math.pow(x, n));
        }
        // Here the number is non-zero finite
        final int[] ie = {0};
        DD f = DD.of(x, xx).frexp(ie);
        final long b = ie[0];
        if (n < 0) {
            f = computeSimplePowScaled(b, f.hi(), f.lo(), -1L * n, exp);
            // Result is a non-zero fraction part so inversion is safe
            f = f.reciprocal();
            // Rescale to [0.5, 1.0)
            f = f.frexp(ie);
            exp[0] = ie[0] - exp[0];
            return f;
        }
        return computeSimplePowScaled(b, f.hi(), f.lo(), n, exp);
    }

    /**
     * Compute the number {@code x} (non-zero finite) raised to the power {@code n} (must be strictly positive).
     *
     * <p>This method exists to allow negation of the power when it is {@link Integer#MIN_VALUE}
     * by casting to a long. By using a fractional representation for the argument
     * the recursive calls avoid a step to normalise the input.
     *
     * @param bx Integral component 2^bx of x.
     * @param x Fractional high part of x.
     * @param xx Fractional low part of x.
     * @param n Power (in [1, 2^31]).
     * @param exp Power of two scale factor (integral exponent).
     * @return Fraction part.
     */
    private static DD computeSimplePowScaled(long bx, double x, double xx, long n, long[] exp) {
        // By normalising x we can break apart the power to avoid over/underflow:
        // x^n = (f * 2^b)^n = 2^bn * f^n
        long b = bx;
        double f0 = x;
        double f1 = xx;

        // Minimise the amount we have to decompose the power. This is done
        // using either f (<=1) or 2f (>=1) as the fractional representation,
        // based on which can use a larger exponent without over/underflow.
        // We approximate the power as 2^b and require a result with the
        // smallest absolute b. An additional consideration is the low-part ff
        // which sets a more conservative underflow limit:
        // f^n              = 2^(-b+53)  => b = -n log2(f) - 53
        // (2f)^n = 2^n*f^n = 2^b        => b =  n log2(f) + n
        // Switch-over point for f is at:
        // -n log2(f) - 53 = n log2(f) + n
        // 2n log2(f) = -53 - n
        // f = 2^(-53/2n) * 2^(-1/2)
        // Avoid a power computation to find the threshold by dropping the first term:
        // f = 2^(-1/2) = 1/sqrt(2) = sqrt(0.5) = 0.707
        // This will bias towards choosing f even when (2f)^n would not overflow.
        // It allows the same safe exponent to be used for both cases.

        // Safe maximum for exponentiation.
        long m;
        double af = Math.abs(f0);
        if (af < ROOT_HALF) {
            // Choose 2f.
            // This case will handle (x, xx) = (1, 0) in a single power operation
            f0 *= 2;
            f1 *= 2;
            af *= 2;
            b -= 1;
            if (n <= SAFE_EXPONENT_2F || af <= SAFE_2F) {
                m = n;
            } else {
                // f^m < 2^1013
                // m ~ 1013 / log2(f)
                m = Math.max(SAFE_EXPONENT_2F, (long) (SAFE_EXPONENT_2F * LN2 / Math.log(af)));
            }
        } else {
            // Choose f
            if (n <= SAFE_EXPONENT_F || af >= SAFE_F) {
                m = n;
            } else {
                // f^m > 2^-958
                // m ~ -958 / log2(f)
                m = Math.max(SAFE_EXPONENT_F, (long) (-SAFE_EXPONENT_F * LN2 / Math.log(af)));
            }
        }

        DD f;
        final int[] expi = {0};

        if (n <= m) {
            f = computeSimplePow(f0, f1, n);
            f = f.frexp(expi);
            exp[0] = b * n + expi[0];
            return f;
        }

        // Decompose the power function.
        // quotient q = n / m
        // remainder r = n % m
        // f^n = (f^m)^(n/m) * f^(n%m)

        final long q = n / m;
        final long r = n % m;
        // (f^m)
        // m is safe and > 1
        f = computeSimplePow(f0, f1, m);
        f = f.frexp(expi);
        long qb = expi[0];
        // (f^m)^(n/m)
        // q is non-zero but may be 1
        if (q > 1) {
            // full simple-pow to ensure safe exponentiation
            f = computeSimplePowScaled(qb, f.hi(), f.lo(), q, exp);
            qb = exp[0];
        }
        // f^(n%m)
        // r may be zero or one which do not require another power
        if (r == 0) {
            f = f.frexp(expi);
            exp[0] = b * n + qb + expi[0];
            return f;
        }
        if (r == 1) {
            f = f.multiply(DD.of(f0, f1));
            f = f.frexp(expi);
            exp[0] = b * n + qb + expi[0];
            return f;
        }
        // Here r is safe
        final DD t = f;
        f = computeSimplePow(f0, f1, r);
        f = f.frexp(expi);
        final long rb = expi[0];
        // (f^m)^(n/m) * f^(n%m)
        f = f.multiply(t);
        // 2^bn * (f^m)^(n/m) * f^(n%m)
        f = f.frexp(expi);
        exp[0] = b * n + qb + rb + expi[0];
        return f;
    }
}
