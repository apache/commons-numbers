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
 * <p>This class supplements the arithmetic operations in the {@link DD} class providing
 * greater accuracy at the cost of performance.
 *
 * @since 1.2
 */
public final class DDMath {
    /** 0.5. */
    private static final double HALF = 0.5;
    /** The limit for safe multiplication of {@code x*y}, assuming values above 1.
     * Used to maintain positive values during the power computation. */
    private static final double SAFE_MULTIPLY = 0x1.0p500;

    /**
     * Mutable double-double number used for working.
     * This structure is used for the output argument during triple-double computations.
     */
    private static final class MDD {
        /** The high part of the double-double number. */
        private double x;
        /** The low part of the double-double number. */
        private double xx;

        /** Package-private constructor. */
        MDD() {}
    }

    /** No instances. */
    private DDMath() {}

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
     *  <li>If {@code (x, xx)} is zero the high part of the fractional part is
     *      computed using {@link Math#pow(double, double) Math.pow(x, n)} and the exponent is 0.</li>
     *  <li>If {@code n = 0} the fractional part is 0.5 and the exponent is 1.</li>
     *  <li>If {@code (x, xx)} is an exact power of 2 the fractional part is 0.5 and the exponent
     *      is the power of 2 minus 1.</li>
     *  <li>If the result high-part is an exact power of 2 and the low-part has an opposite
     *      signed non-zero magnitude then the fraction high-part {@code f} will be {@code +/-1} such that
     *      the double-double number is in the range {@code [0.5, 1)}.</li>
     *  <li>If the argument is not finite then a fractional representation is not possible.
     *      In this case the fraction and the scale factor is undefined.</li>
     * </ul>
     *
     * <p>The computed result is within 1 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * <p>The performance is approximately 4-fold slower than {@link DD#pow(int, long[])}.
     *
     * @param x Number.
     * @param n Power.
     * @param exp Result power of two scale factor (integral exponent).
     * @return Fraction part.
     * @see DD#frexp(int[])
     */
    public static DD pow(DD x, int n, long[] exp) {
        // Edge cases.
        if (n == 0) {
            exp[0] = 1;
            return DD.of(0.5);
        }
        // IEEE result for non-finite or zero
        if (!Double.isFinite(x.hi()) || x.hi() == 0) {
            exp[0] = 0;
            return DD.of(Math.pow(x.hi(), n));
        }
        // Here the number is non-zero finite
        final int[] ie = {0};
        final DD f = x.frexp(ie);
        final long b = ie[0];
        // Handle exact powers of 2
        if (Math.abs(f.hi()) == HALF && f.lo() == 0) {
            // (f * 2^b)^n = (2f)^n * 2^(b-1)^n
            // Use Math.pow to create the sign.
            // Note the result must be scaled to the fractional representation
            // by multiplication by 0.5 and addition of 1 to the exponent.
            final double y0 = 0.5 * Math.pow(2 * f.hi(), n);
            // Propagate sign change (y0*f.x) to the original zero (this.xx)
            final double y1 = Math.copySign(0.0, y0 * f.hi() * x.lo());
            exp[0] = 1 + (b - 1) * n;
            return DD.of(y0, y1);
        }
        return computePowScaled(b, f.hi(), f.lo(), n, exp);
    }

    /**
     * Compute the number {@code x} (non-zero finite) raised to the power {@code n}.
     *
     * <p>Performs the computation using triple-double precision. If the input power is
     * negative the result is computed using the absolute value of {@code n} and then
     * inverted by dividing into 1.
     *
     * @param b Integral component 2^b of x.
     * @param x Fractional high part of x.
     * @param xx Fractional low part of x.
     * @param n Power (in [2, 2^31]).
     * @param exp Result power of two scale factor (integral exponent).
     * @return Fraction part.
     */
    private static DD computePowScaled(long b, double x, double xx, int n, long[] exp) {
        // Same as DD.computePowScaled using a triple-double intermediate.

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
        final long be = b - 1;
        final double b0 = x * 2;
        final double b1 = xx * 2;
        // Split b
        final double b0h = DD.highPart(b0);
        final double b0l = b0 - b0h;
        final double b1h = DD.highPart(b1);
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
        DD t;
        final MDD m = new MDD();

        // Multiplication is done inline with some triple precision helper routines.
        // Process remaining bits below highest set bit.
        for (int i = 32 - shift; i != 0; i--, bits <<= 1) {
            // Square the result
            fe <<= 1;
            double a0h = DD.highPart(f0);
            double a0l = f0 - a0h;
            double a1h = DD.highPart(f1);
            double a1l = f1 - a1h;
            double a2h = DD.highPart(f2);
            double a2l = f2 - a2h;
            double p00 = f0 * f0;
            double q00 = DD.twoSquareLow(a0h, a0l, p00);
            double p01 = f0 * f1;
            double q01 = DD.twoProductLow(a0h, a0l, a1h, a1l, p01);
            final double p02 = f0 * f2;
            final double q02 = DD.twoProductLow(a0h, a0l, a2h, a2l, p02);
            double p11 = f1 * f1;
            double q11 = DD.twoSquareLow(a1h, a1l, p11);
            final double p12 = f1 * f2;
            double s0 = p00;
            // Sum (2 * p01, q00) -> (s1, r2)                  Order(eps)
            double s1 = 2 * p01 + q00;
            double r2 = DD.twoSumLow(2 * p01, q00, s1);
            // Sum (2 * p02, 2 * q01, p11, r2) -> (s2, r3b)    Order(eps^2)
            double s2 = p02 + q01;
            double r3b = DD.twoSumLow(p02, q01, s2);
            double u = p11 + r2;
            double v = DD.twoSumLow(p11, r2, u);
            t = DD.add(2 * s2, 2 * r3b, u, v);
            s2 = t.hi();
            r3b = t.lo();
            // Sum (2 * p12, 2 * q02, q11, r3b) -> s3          Order(eps^3)
            double s3 = 2 * (p12 + q02) + q11 + r3b;
            f0 = norm3(s0, s1, s2, s3, m);
            f1 = m.x;
            f2 = m.xx;

            // Rescale
            if (Math.abs(f0) > SAFE_MULTIPLY) {
                // Scale back to the [1, 2) range. As safe multiply is 2^500
                // the exponent should be < 1001 so the twoPow scaling factor is supported.
                final int e = Math.getExponent(f0);
                final double s = DD.twoPow(-e);
                fe += e;
                f0 *= s;
                f1 *= s;
                f2 *= s;
            }

            if (bits < 0) {
                // Multiply by b
                fe += be;
                a0h = DD.highPart(f0);
                a0l = f0 - a0h;
                a1h = DD.highPart(f1);
                a1l = f1 - a1h;
                a2h = DD.highPart(f2);
                a2l = f2 - a2h;
                p00 = f0 * b0;
                q00 = DD.twoProductLow(a0h, a0l, b0h, b0l, p00);
                p01 = f0 * b1;
                q01 = DD.twoProductLow(a0h, a0l, b1h, b1l, p01);
                final double p10 = f1 * b0;
                final double q10 = DD.twoProductLow(a1h, a1l, b0h, b0l, p10);
                p11 = f1 * b1;
                q11 = DD.twoProductLow(a1h, a1l, b1h, b1l, p11);
                final double p20 = f2 * b0;
                final double q20 = DD.twoProductLow(a2h, a2l, b0h, b0l, p20);
                final double p21 = f2 * b1;
                s0 = p00;
                // Sum (p01, p10, q00) -> (s1, r2, r3a)            Order(eps)
                u = p01 + p10;
                v = DD.twoSumLow(p01, p10, u);
                s1 = q00 + u;
                final double w = DD.twoSumLow(q00, u, s1);
                r2 = v + w;
                final double r3a = DD.twoSumLow(v, w, r2);
                // Sum (p11, p20, q01, q10, r2) -> (s2, r3b)       Order(eps^2)
                s2 = p11 + p20;
                r3b = DD.twoSumLow(p11, p20, s2);
                u = q01 + q10;
                v = DD.twoSumLow(q01, q10, u);
                t = DD.add(s2, r3b, u, v);
                s2 = t.hi() + r2;
                r3b = DD.twoSumLow(t.hi(), r2, s2);
                // Sum (p21, q11, q20, r3a, r3b) -> s3             Order(eps^3)
                s3 = p21 + q11 + q20 + r3a + r3b;
                f0 = norm3(s0, s1, s2, s3, m);
                f1 = m.x;
                f2 = m.xx;
                // Avoid rescale as x2 is in [1, 2)
            }
        }

        // Ensure (f0, f1) are 1 ulp exact
        final double u = f1 + f2;
        t = DD.fastTwoSum(f0, u);
        final int[] e = {0};

        // If the power is negative, invert in triple precision
        if (n < 0) {
            // Require the round-off
            final double v = DD.fastTwoSumLow(f1, f2, u);
            // Result is in approximately [1, 2^501] so inversion is safe.
            t = inverse3(t.hi(), t.lo(), v);
            // Rescale to [0.5, 1.0]
            t = t.frexp(e);
            exp[0] = e[0] - fe;
            return t;
        }

        t = t.frexp(e);
        exp[0] = fe + e[0];
        return t;
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
    private static double norm3(double s0, double s1, double s2, double s3, MDD s12) {
        double q;
        // Compress (Schewchuk Fig. 15) (s0, s1, s2, s3) -> (g0, g1, g2, g3)
        final double g0 = s0 + s1;
        q = DD.fastTwoSumLow(s0, s1, g0);
        final double g1 = q + s2;
        q = DD.fastTwoSumLow(q, s2, g1);
        final double g2 = q + s3;
        final double g3 = DD.fastTwoSumLow(q, s3, g2);
        // (g0, g1, g2, g3) -> (h0, h1, h2, h3), returned as (h0, h1, h2 + h3)
        q = g1 + g2;
        s12.xx = DD.fastTwoSumLow(g1, g2, q) + g3;
        final double h0 = g0 + q;
        s12.x = DD.fastTwoSumLow(g0, q, h0);
        return h0;
    }

    /**
     * Compute the inverse of {@code (y, yy, yyy)}.
     * If {@code y = 0} the result is undefined.
     *
     * <p>This is special routine used in {@link #pow(int, long[])}
     * to invert the triple precision result.
     *
     * @param y First part of y.
     * @param yy Second part of y.
     * @param yyy Third part of y.
     * @return the inverse
     */
    private static DD inverse3(double y, double yy, double yyy) {
        // Long division (1, 0, 0) / (y, yy, yyy)
        double r;
        double rr;
        double rrr;
        double t;
        // quotient q0 = x / y
        final double q0 = 1 / y;
        // remainder r0 = x - q0 * y
        final MDD q = new MDD();
        t = multiply3(y, yy, yyy, q0, q);
        r = add3(-t, -q.x, -q.xx, 1, q);
        rr = q.x;
        rrr = q.xx;
        // next quotient q1 = r0 / y
        final double q1 = r / y;
        // remainder r1 = r0 - q1 * y
        t = multiply3(y, yy, yyy, q1, q);
        r = add3(-t, -q.x, -q.xx, r, rr, rrr, q);
        rr = q.x;
        rrr = q.xx;
        // next quotient q2 = r1 / y
        final double q2 = r / y;
        // remainder r2 = r1 - q2 * y
        t = multiply3(y, yy, yyy, q2, q);
        r = add3(-t, -q.x, -q.xx, r, rr, rrr, q);
        // next quotient q3 = r2 / y
        final double q3 = r / y;
        // Collect (q0, q1, q2, q3) to (s0, s1, s2)
        t = norm3(q0, q1, q2, q3, q);
        // Reduce to (s0, s1)
        return DD.fastTwoSum(t, q.x + q.xx);
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
    private static double multiply3(double a0, double a1, double a2, double b, MDD s12) {
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
        final double a0h = DD.highPart(a0);
        final double a0l = a0 - a0h;
        final double a1h = DD.highPart(a1);
        final double a1l = a1 - a1h;
        final double b0h = DD.highPart(b);
        final double b0l = b - b0h;
        final double p00 = a0 * b;
        final double q00 = DD.twoProductLow(a0h, a0l, b0h, b0l, p00);
        final double p10 = a1 * b;
        final double q10 = DD.twoProductLow(a1h, a1l, b0h, b0l, p10);
        final double p20 = a2 * b;
        // Sum (p10, q00) -> (s1, r1)            Order(eps)
        final double s1 = p10 + q00;
        final double r1 = DD.twoSumLow(p10, q00, s1);
        // Sum (p20, q10, r1) -> (s2, s3)        Order(eps^2)
        double u = p20 + q10;
        final double v = DD.twoSumLow(p20, q10, u);
        final double s2 = u + r1;
        u = DD.twoSumLow(u, r1, s2);
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
    private static double add3(double a0, double a1, double a2, double b, MDD s12) {
        // Hide et al (2008) Fig.5: Quad-Double + Double without final a3.
        double u;
        final double v;
        final double s0 = a0 + b;
        u = DD.twoSumLow(a0, b, s0);
        final double s1 = a1 + u;
        v = DD.twoSumLow(a1, u, s1);
        final double s2 = a2 + v;
        u = DD.twoSumLow(a2, v, s2);
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
    private static double add3(double a0, double a1, double a2, double b0, double b1, double b2, MDD s12) {
        // Hide et al (2008) Fig.6: Quad-Double + Quad-Double without final a3, b3.
        double u;
        double v;
        // a0 + b0 -> (s0, r1)
        final double s0 = a0 + b0;
        final double r1 = DD.twoSumLow(a0, b0, s0);
        // a1 + b1 + r1 -> (s1, r2, r3)
        u = a1 + b1;
        v = DD.twoSumLow(a1, b1, u);
        final double s1 = r1 + u;
        u = DD.twoSumLow(r1, u, s1);
        final double r2 = v + u;
        final double r3 = DD.twoSumLow(v, u, r2);
        // (a2 + b2 + r2) + r3 -> (s2, s3)
        u = a2 + b2;
        v = DD.twoSumLow(a2, b2, u);
        final double s2 = r2 + u;
        u = DD.twoSumLow(r2, u, s2);
        final double s3 = v + u + r3;
        return norm3(s0, s1, s2, s3, s12);
    }
}
