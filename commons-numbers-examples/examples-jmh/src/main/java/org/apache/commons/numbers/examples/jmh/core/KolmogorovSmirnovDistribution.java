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

import org.apache.commons.numbers.core.DD;
import org.apache.commons.numbers.core.DDMath;

/**
 * Computes the complementary probability for the one-sample Kolmogorov-Smirnov distribution.
 *
 * <p>This class has been extracted from {@code o.a.c.statistics.inference}. It is a subset of
 * the computations required for the KS distribution.
 *
 * @since 1.2
 */
final class KolmogorovSmirnovDistribution {
    /** No instances. */
    private KolmogorovSmirnovDistribution() {}

    /**
     * Computes the complementary probability {@code P[D_n^+ >= x]} for the one-sided
     * one-sample Kolmogorov-Smirnov distribution.
     *
     * <pre>
     * D_n^+ = sup_x {CDF_n(x) - F(x)}
     * </pre>
     *
     * <p>where {@code n} is the sample size; {@code CDF_n(x)} is an empirical
     * cumulative distribution function; and {@code F(x)} is the expected
     * distribution. The computation uses Smirnov's stable formula:
     *
     * <pre>
     *                   floor(n(1-x)) (n) ( j     ) (j-1)  (         j ) (n-j)
     * P[D_n^+ >= x] = x     Sum       ( ) ( - + x )        ( 1 - x - - )
     *                       j=0       (j) ( n     )        (         n )
     * </pre>
     *
     * <p>Computing using logs is not as accurate as direct multiplication when n is large.
     * However the terms are very large and small. Multiplication uses a scaled representation
     * with a separate exponent term to support the extreme range. Extended precision
     * representation of the numbers reduces the error in the power terms. Details in
     * van Mulbregt (2018).
     *
     * <p>
     * References:
     * <ol>
     * <li>
     * van Mulbregt, P. (2018).
     * <a href="https://doi.org/10.48550/arxiv.1802.06966">Computing the Cumulative Distribution Function and Quantiles of the One-sided Kolmogorov-Smirnov Statistic</a>
     * arxiv:1802.06966.
     * <li>Magg &amp; Dicaire (1971).
     * <a href="https://doi.org/10.1093/biomet/58.3.653">On Kolmogorov-Smirnov Type One-Sample Statistics</a>
     * Biometrika 58.3 pp. 653–656.
     * </ol>
     *
     * @since 1.1
     */
    static final class One {
        /** "Very large" n to use a asymptotic limiting form.
         * [1] suggests 1e12 but this is reduced to avoid excess
         * computation time. */
        private static final int VERY_LARGE_N = 1000000;
        /** Maximum number of term for the Smirnov-Dwass algorithm. */
        private static final int SD_MAX_TERMS = 3;
        /** Minimum sample size for the Smirnov-Dwass algorithm. */
        private static final int SD_MIN_N = 8;
        /** Number of bits of precision in the sum of terms Aj.
         * This does not have to be the full 106 bits of a double-double as the final result
         * is used as a double. The terms are represented as fractions with an exponent:
         * <pre>
         *  Aj = 2^b * f
         *  f of sum(A) in [0.5, 1)
         *  f of Aj in [0.25, 2]
         * </pre>
         * <p>The terms can be added if their exponents overlap. The bits of precision must
         * account for the extra range of the fractional part of Aj by 1 bit. Note that
         * additional bits are added to this dynamically based on the number of terms. */
        private static final int SUM_PRECISION_BITS = 53;
        /** Number of bits of precision in the sum of terms Aj.
         * For Smirnov-Dwass we use the full 106 bits of a double-double due to the summation
         * of terms that cancel. Account for the extra range of the fractional part of Aj by 1 bit. */
        private static final int SD_SUM_PRECISION_BITS = 107;

        /**
         * Defines a scaled power function.
         */
        private interface StaticScaledPower {
            /**
             * Compute the number {@code x} raised to the power {@code n}.
             *
             * <p>The value is returned as fractional {@code f} and integral
             * {@code 2^exp} components.
             * <pre>
             * (x+xx)^n = (f+ff) * 2^exp
             * </pre>
             *
             * @param x High part of x.
             * @param xx Low part of x.
             * @param n Power.
             * @param f Fraction part.
             * @return Power of two scale factor (integral exponent).
             * @see SDD#frexp(double, double, SDD)
             * @see SDD#fastPowScaled(double, double, int, SDD)
             * @see SDD#powScaled(double, double, int, SDD)
             */
            long pow(double x, double xx, int n, SDD f);
        }

        /**
         * Defines a scaled power function.
         */
        private interface ScaledPower {
            /**
             * Compute the number {@code x} raised to the power {@code n}.
             *
             * <p>The value is returned as fractional {@code f} and integral
             * {@code 2^exp} components.
             * <pre>
             * (x+xx)^n = (f+ff) * 2^exp
             * </pre>
             *
             * @param x x.
             * @param n Power.
             * @param exp Power of two scale factor (integral exponent).
             * @return Fraction part.
             * @see DD#frexp(int[])
             * @see DD#pow(int, long[])
             */
            DD pow(DD x, int n, long[] exp);
        }

        /**
         * Defines an addition of two double-double numbers.
         */
        private interface StatisDDAdd {
            /**
             * Compute the sum of {@code (x,xx)} and {@code (y,yy)}.
             *
             * @param x High part of x.
             * @param xx Low part of x.
             * @param y High part of y.
             * @param yy Low part of y.
             * @param s Sum.
             * @return the sum
             * @see SDD#add(double, double, double, double, SDD)
             */
            SDD add(double x, double xx, double y, double yy, SDD s);
        }

        /** No instances. */
        private One() {}

        /**
         * Calculates complementary probability {@code P[D_n^+ >= x]}, or survival
         * function (SF), for the one-sided one-sample Kolmogorov-Smirnov distribution.
         *
         * @param x Statistic.
         * @param n Sample size (assumed to be positive).
         * @return \(P(D_n^+ &ge; x)\)
         */
        static double sfMutable(double x, int n) {
            final double p = sfExact(x, n);
            if (p >= 0) {
                return p;
            }
            // Note: This is not referring to N = floor(n*x).
            // Here n is the sample size and a suggested limit 10^12 is noted on pp.15 in [1].
            // This uses a lower threshold where the full computation takes ~ 1 second.
            if (n > VERY_LARGE_N) {
                return sfAsymptotic(x, n);
            }
            return sfSDD(x, n, false);
        }

        /**
         * Calculates complementary probability {@code P[D_n^+ >= x]}, or survival
         * function (SF), for the one-sided one-sample Kolmogorov-Smirnov distribution.
         *
         * @param x Statistic.
         * @param n Sample size (assumed to be positive).
         * @return \(P(D_n^+ &ge; x)\)
         */
        static double sfMutableFullPow(double x, int n) {
            final double p = sfExact(x, n);
            if (p >= 0) {
                return p;
            }
            // Note: This is not referring to N = floor(n*x).
            // Here n is the sample size and a suggested limit 10^12 is noted on pp.15 in [1].
            // This uses a lower threshold where the full computation takes ~ 1 second.
            if (n > VERY_LARGE_N) {
                return sfAsymptotic(x, n);
            }
            return sfSDD(x, n, true);
        }

        /**
         * Calculates exact cases for the complementary probability
         * {@code P[D_n^+ >= x]} the one-sided one-sample Kolmogorov-Smirnov distribution.
         *
         * <p>Exact cases handle x not in [0, 1]. It is assumed n is positive.
         *
         * @param x Statistic.
         * @param n Sample size (assumed to be positive).
         * @return \(P(D_n^+ &ge; x)\)
         */
        private static double sfExact(double x, int n) {
            if (n * x * x >= 372.5 || x >= 1) {
                // p would underflow, or x is out of the domain
                return 0;
            }
            if (x <= 0) {
                // edge-of, or out-of, the domain
                return 1;
            }
            if (n == 1) {
                return x;
            }
            // x <= 1/n
            // [1] Equation (33)
            final double nx = n * x;
            if (nx <= 1) {
                // 1 - x (1+x)^(n-1): here x may be small so use log1p
                return 1 - x * Math.exp((n - 1) * Math.log1p(x));
            }
            // 1 - 1/n <= x < 1
            // [1] Equation (16)
            if (n - 1 <= nx) {
                // (1-x)^n: here x > 0.5 and 1-x is exact
                return Math.pow(1 - x, n);
            }
            return -1;
        }

        /**
         * Calculates complementary probability {@code P[D_n^+ >= x]}, or survival
         * function (SF), for the one-sided one-sample Kolmogorov-Smirnov distribution.
         *
         * <p>Computes the result using the asymptotic formula Eq 5 in [1].
         *
         * @param x Statistic.
         * @param n Sample size (assumed to be positive).
         * @return \(P(D_n^+ &ge; x)\)
         */
        private static double sfAsymptotic(double x, int n) {
            // Magg & Dicaire (1971) limiting form
            return Math.exp(-Math.pow(6.0 * n * x + 1, 2) / (18.0 * n));
        }

        /**
         * Compute exactly {@code x = (k + alpha) / n} with {@code k} an integer and
         * {@code alpha in [0, 1)}. Note that {@code k ~ floor(nx)} but may be rounded up
         * if {@code alpha -> 1} within working precision.
         *
         * <p>This computation is a significant source of increased error if performed in
         * 64-bit arithmetic. Although the value alpha is only used for the PDF computation
         * a value of {@code alpha == 0} indicates the final term of the SF summation can be
         * dropped due to the cancellation of a power term {@code (x + j/n)} to zero with
         * {@code x = (n-j)/n}. That is if {@code alpha == 0} then x is the fraction {@code k/n}
         * and one Aj term is zero.
         *
         * @param n Sample size.
         * @param x Statistic.
         * @param z Used for computation. Return {@code alpha} in the high part.
         * @return k
         */
        private static int splitX(int n, double x, SDD z) {
            // Described on page 14 in van Mulbregt [1].
            // nx = U+V (exact)
            SDD.twoProd(n, x, z);
            final double u = z.hi();
            final double v = z.lo();
            // Integer part of nx is *almost* the integer part of U.
            // Compute k = floor((U,V)) (changed from the listing of floor(U)).
            int k = (int) Math.floor(u);
            // Incorporate the round-off of u in the floor
            if (k == u) {
                // u is an integer. If v < 0 then the floor is 1 lower.
                k += v < 0 ? -1 : 0;
            }
            // nx = k + ((U - k) + V) = k + (U1 + V1)
            SDD.fastAdd(u, v, -k, z);
            // alpha = (U1, V1) = z
            // alpha is in [0, 1) in double-double precision.
            // Ensure the high part is in [0, 1) (i.e. in double precision).
            if (z.hi() == 1) {
                // Here alpha is ~ 1.0-eps.
                // This occurs when x ~ j/n and n is large.
                k += 1;
                SDD.set(0, z);
            }
            return k;
        }

        /**
         * Returns {@code floor(log2(n))}.
         *
         * @param n Value.
         * @return approximate log2(n)
         */
        private static int log2(int n) {
            return 31 - Integer.numberOfLeadingZeros(n);
        }

        /**
         * Calculates complementary probability {@code P[D_n^+ >= x]}, or survival
         * function (SF), for the one-sided one-sample Kolmogorov-Smirnov distribution.
         *
         * @param x Statistic.
         * @param n Sample size (assumed to be positive).
         * @return \(P(D_n^+ &ge; x)\)
         */
        static double sfOO(double x, int n) {
            final double p = sfExact(x, n);
            if (p >= 0) {
                return p;
            }
            // Note: This is not referring to N = floor(n*x).
            // Here n is the sample size and a suggested limit 10^12 is noted on pp.15 in [1].
            // This uses a lower threshold where the full computation takes ~ 1 second.
            if (n > VERY_LARGE_N) {
                return sfAsymptotic(x, n);
            }
            return sfDD(x, n);
        }

        // @CHECKSTYLE: stop MethodLength

        /**
         * Calculates complementary probability {@code P[D_n^+ >= x]}, or survival
         * function (SF), for the one-sided one-sample Kolmogorov-Smirnov distribution.
         *
         * <p>Computes the result using double-double arithmetic. The power function can
         * use a fast approximation or a full power computation.
         *
         * <p>This function is safe for {@code x > 1/n}. When {@code x} approaches
         * sub-normal then division or multiplication by x can under/overflow. The case of
         * {@code x < 1/n} can be computed in {@code sfExact}.
         *
         * @param x Statistic (typically in (1/n, 1 - 1/n)).
         * @param n Sample size (assumed to be positive).
         * @param fullPow If {@code true} compute using a full accuracy pow function
         * @return \(P(D_n^+ &ge; x)\)
         * @see SDD#fastPowScaled(double, double, int, SDD)
         * @see SDD#powScaled(double, double, int, SDD)
         */
        private static double sfSDD(double x, int n, boolean fullPow) {
            // Compute only the SF using Algorithm 1 pp 12.
            // Only require 1 double-double for all intermediate computations.
            final SDD z = SDD.create();

            // Compute: k = floor(n*x), alpha = nx - k; x = (k+alpha)/n with 0 <= alpha < 1
            final int k = splitX(n, x, z);
            final double alpha = z.hi();

            // Choose the algorithm:
            // Eq (13) Smirnov/Birnbaum-Tingey; or Smirnov/Dwass Eq (31)
            // Eq. 13 sums j = 0 : floor( n(1-x) )  = n - 1 - floor(nx) iff alpha != 0; else n - floor(nx)
            // Eq. 31 sums j = ceil( n(1-x) ) : n   = n - floor(nx)
            // Drop a term term if x = (n-j)/n. Equates to shifting the floor* down and ceil* up:
            // Eq. 13 N = floor*( n(1-x) ) = n - k - ((alpha!=0) ? 1 : 0) - ((alpha==0) ? 1 : 0)
            // Eq. 31 N = n - ceil*( n(1-x) ) = k - ((alpha==0) ? 1 : 0)
            // Where N is the number of terms - 1. This differs from Algorithm 1 by dropping
            // a SD term when it should be zero (to working precision).
            final int regN = n - k - 1;
            final int sdN = k - ((alpha == 0) ? 1 : 0);

            // SD : Figure 3 (c) (pp. 6)
            // Terms Aj (j = n -> 0) have alternating signs through the range and may involve
            // numbers much bigger than 1 causing cancellation; magnitudes increase then decrease.
            // Section 3.3: Extra digits of precision required
            // grows like Order(sqrt(n)). E.g. sf=0.7 (x ~ 0.4/sqrt(n)) loses 8 digits.
            //
            // Regular : Figure 3 (a, b)
            // Terms Aj can have similar magnitude through the range; when x >= 1/sqrt(n)
            // the final few terms can be magnitudes smaller and could be ignored.
            // Section 3.4: As x increases the magnitude of terms becomes more peaked,
            // centred at j = (n-nx)/2, i.e. 50% of the terms.
            //
            // As n -> inf the sf for x = k/n agrees with the asymptote Eq 5 in log2(n) bits.
            //
            // Figure 4 has lines at x = 1/n and x = 3/sqrt(n).
            // Point between is approximately x = 4/n, i.e. nx < 4 : k <= 3.
            // If faster when x < 0.5 and requiring nx ~ 4 then requires n >= 8.
            //
            // Note: If SD accuracy scales with sqrt(n) then we could use 1 / sqrt(n).
            // That threshold is always above 4 / n when n is 16 (4/n = 1/sqrt(n) : n = 4^2).
            // So the current thresholds are conservative.
            boolean sd = false;
            if (sdN < regN) {
                // Here x < 0.5 and SD has fewer terms
                // Always choose when we only have one additional term (i.e x < 2/n)
                sd = sdN <= 1;
                // Otherwise when x < 4 / n
                sd |= sdN <= SD_MAX_TERMS && n >= SD_MIN_N;
            }

            final int maxN = sd ? sdN : regN;

            // Note: if N > "very large" use the asymptotic approximation.
            // Currently this check is done on n (sample size) in the calling function.
            // This provides a monotonic p-value for all x with the same n.

            // Configure the algorithm.
            // The error of double-double addition and multiplication is low (< 2^-102).
            // The error in Aj is mainly from the power function.
            // fastPow error is around 2^-52, pow error is ~ 2^-70 or lower.
            // Smirnoff-Dwass has a sum of terms that cancel and requires higher precision.
            // SD has only a few terms. Use a high accuracy power.
            StaticScaledPower fpow = sd || fullPow ? SDD::powScaled : SDD::fastPowScaled;
            // SD requires a more precise summation using all the terms that can be added.
            // For the regular summation we must sum at least 50% of the terms. The number
            // of bits required to sum remaining terms of the same magnitude is log2(N/2).
            // These guards bits are conservative and > ~99% of terms are typically used.
            final StatisDDAdd fadd = sd ? SDD::add : SDD::fastAdd;
            final int sumBits = sd ? SD_SUM_PRECISION_BITS : SUM_PRECISION_BITS + log2(maxN >> 1);

            // Working variable for the exponent of scaled values
            long e;

            // Compute A0. The terms Aj may over/underflow.
            // This is handled by maintaining the sum(Aj) using a fractional representation.
            if (sd) {
                // A0 = (1+x)^(n-1)
                SDD.fastTwoSum(1, x, z);
                e = fpow.pow(z.hi(), z.lo(), n - 1, z);
            } else {
                // A0 = (1-x)^n / x
                SDD.fastTwoSum(1, -x, z);
                e = fpow.pow(z.hi(), z.lo(), n, z);
                // x in (1/n, 1 - 1/n) so the divide of the fraction is safe
                SDD.divide(z.hi(), z.lo(), x, 0, z);
                e += SDD.frexp(z.hi(), z.lo(), z);
            }

            // sum(Aj) maintained as 2^e * f with f in [0.5, 1)
            final SDD sum = z.copy();
            long esum = e;
            // Binomial coefficient c(n, j) maintained as 2^e * f with f in [1, 2)
            // This value is integral but maintained to limited precision
            final SDD c = SDD.create(1);
            long ec = 0;
            for (int i = 1; i <= maxN; i++) {
                // c(n, j) = c(n, j-1) * (n-j+1) / j
                SDD.uncheckedDivide(n - i + 1, i, z);
                SDD.uncheckedMultiply(c.hi(), c.lo(), z.hi(), z.lo(), c);
                // Here we maintain c in [1, 2) to restrict the scaled Aj term to [0.25, 2].
                final int b = Math.getExponent(c.hi());
                if (b != 0) {
                    SDD.ldexp(c.hi(), c.lo(), -b, c);
                    ec += b;
                }
                // Compute Aj
                final int j = sd ? n - i : i;
                // Algorithm 4 pp. 27
                // S = ((j/n) + x)^(j-1)
                // T = ((n-j)/n - x)^(n-j)
                SDD.uncheckedDivide(j, n, z);
                SDD.fastAdd(z.hi(), z.lo(), x, z);
                final long es = fpow.pow(z.hi(), z.lo(), j - 1, z);
                final double s = z.hi();
                final double ss = z.lo();
                SDD.uncheckedDivide(n - j, n, z);
                SDD.fastAdd(z.hi(), z.lo(), -x, z);
                final long et = fpow.pow(z.hi(), z.lo(), n - j, z);
                // Aj = C(n, j) * T * S
                //    = 2^e * [1, 2] * [0.5, 1] * [0.5, 1]
                //    = 2^e * [0.25, 2]
                e = ec + es + et;
                // Only compute and add to the sum when the exponents overlap by n-bits.
                if (e > esum - sumBits) {
                    SDD.uncheckedMultiply(c.hi(), c.lo(), z.hi(), z.lo(), z);
                    SDD.uncheckedMultiply(z.hi(), z.lo(), s, ss, z);
                    // Scaling must offset by the scale of the sum
                    SDD.ldexp(z.hi(), z.lo(), (int) (e - esum), z);
                    fadd.add(sum.hi(), sum.lo(), z.hi(), z.lo(), sum);
                } else {
                    // Terms are expected to increase in magnitude then reduce.
                    // Here the terms are insignificant and we can stop.
                    // Effectively Aj -> eps * sum, and most of the computation is done.
                    break;
                }

                // Re-scale the sum
                esum += SDD.frexp(sum.hi(), sum.lo(), sum);
            }

            // p = x * sum(Ai). Since the sum is normalized
            // this is safe as long as x does not approach a sub-normal.
            // Typically x in (1/n, 1 - 1/n).
            SDD.multiply(sum.hi(), sum.lo(), x, sum);
            // Rescale the result
            SDD.ldexp(sum.hi(), sum.lo(), (int) esum, sum);
            if (sd) {
                // SF = 1 - CDF
                SDD.add(-sum.hi(), -sum.lo(), 1, sum);
            }
            return clipProbability(sum.doubleValue());
        }

        /**
         * Calculates complementary probability {@code P[D_n^+ >= x]}, or survival
         * function (SF), for the one-sided one-sample Kolmogorov-Smirnov distribution.
         *
         * <p>Computes the result using double-double arithmetic. The power function
         * can use a fast approximation or a full power computation.
         *
         * <p>This function is safe for {@code x > 1/n}. When {@code x} approaches
         * sub-normal then division or multiplication by x can under/overflow. The
         * case of {@code x < 1/n} can be computed in {@code sfExact}.
         *
         * @param x Statistic (typically in (1/n, 1 - 1/n)).
         * @param n Sample size (assumed to be positive).
         * @return \(P(D_n^+ &ge; x)\)
         * @see SDD#fastPowScaled(double, double, int, SDD)
         * @see SDD#powScaled(double, double, int, SDD)
         */
        private static double sfDD(double x, int n) {
            // Same initialisation as: double sf(double x, int n, ScaledPower power)

            // Compute only the SF using Algorithm 1 pp 12.
            // Only require 1 double-double for all intermediate computations.
            final SDD zz = SDD.create();

            // Compute: k = floor(n*x), alpha = nx - k; x = (k+alpha)/n with 0 <= alpha < 1
            final int k = splitX(n, x, zz);
            final double alpha = zz.hi();

            // Choose the algorithm:
            // Eq (13) Smirnov/Birnbaum-Tingey; or Smirnov/Dwass Eq (31)
            // Eq. 13 sums j = 0 : floor( n(1-x) )  = n - 1 - floor(nx) iff alpha != 0; else n - floor(nx)
            // Eq. 31 sums j = ceil( n(1-x) ) : n   = n - floor(nx)
            // Drop a term term if x = (n-j)/n. Equates to shifting the floor* down and ceil* up:
            // Eq. 13 N = floor*( n(1-x) ) = n - k - ((alpha!=0) ? 1 : 0) - ((alpha==0) ? 1 : 0)
            // Eq. 31 N = n - ceil*( n(1-x) ) = k - ((alpha==0) ? 1 : 0)
            // Where N is the number of terms - 1. This differs from Algorithm 1 by dropping
            // a SD term when it should be zero (to working precision).
            final int regN = n - k - 1;
            final int sdN = k - ((alpha == 0) ? 1 : 0);

            // SD : Figure 3 (c) (pp. 6)
            // Terms Aj (j = n -> 0) have alternating signs through the range and may involve
            // numbers much bigger than 1 causing cancellation; magnitudes increase then decrease.
            // Section 3.3: Extra digits of precision required
            // grows like Order(sqrt(n)). E.g. sf=0.7 (x ~ 0.4/sqrt(n)) loses 8 digits.
            //
            // Regular : Figure 3 (a, b)
            // Terms Aj can have similar magnitude through the range; when x >= 1/sqrt(n)
            // the final few terms can be magnitudes smaller and could be ignored.
            // Section 3.4: As x increases the magnitude of terms becomes more peaked,
            // centred at j = (n-nx)/2, i.e. 50% of the terms.
            //
            // As n -> inf the sf for x = k/n agrees with the asymptote Eq 5 in log2(n) bits.
            //
            // Figure 4 has lines at x = 1/n and x = 3/sqrt(n).
            // Point between is approximately x = 4/n, i.e. nx < 4 : k <= 3.
            // If faster when x < 0.5 and requiring nx ~ 4 then requires n >= 8.
            //
            // Note: If SD accuracy scales with sqrt(n) then we could use 1 / sqrt(n).
            // That threshold is always above 4 / n when n is 16 (4/n = 1/sqrt(n) : n = 4^2).
            // So the current thresholds are conservative.
            boolean sd = false;
            if (sdN < regN) {
                // Here x < 0.5 and SD has fewer terms
                // Always choose when we only have one additional term (i.e x < 2/n)
                sd = sdN <= 1;
                // Otherwise when x < 4 / n
                sd |= sdN <= SD_MAX_TERMS && n >= SD_MIN_N;
            }

            final int maxN = sd ? sdN : regN;

            // Note: if N > "very large" use the asymptotic approximation.
            // Currently this check is done on n (sample size) in the calling function.
            // This provides a monotonic p-value for all x with the same n.

            // Configure the algorithm.
            // The error of double-double addition and multiplication is low (< 2^-102).
            // The error in Aj is mainly from the power function.
            // fastPow error is around 2^-52, pow error is ~ 2^-70 or lower.
            // Smirnoff-Dwass has a sum of terms that cancel and requires higher precision.
            // SD has only a few terms. Use a high accuracy power.
            ScaledPower fpow = sd ? DDMath::pow : DD::pow;
            // SD requires a more precise summation using all the terms that can be added.
            // For the regular summation we must sum at least 50% of the terms. The number
            // of required bits to sum remaining terms of the same magnitude is log2(N/2).
            // These guards bits are conservative and > ~99% of terms are typically used.
            final int sumBits = sd ? SD_SUM_PRECISION_BITS : SUM_PRECISION_BITS + log2(maxN >> 1);

            // Working variable for the exponent of scaled values
            long e;
            int[] ie = {0};
            long[] le = {0};
            DD z;

            // Compute A0. The terms Aj may over/underflow.
            // This is handled by maintaining the sum(Aj) using a fractional representation.
            if (sd) {
                // A0 = (1+x)^(n-1)
                // Could use fastTwoSum
                z = DD.ofSum(1, x);
                z = fpow.pow(z, n - 1, le);
                e = le[0];
            } else {
                // A0 = (1-x)^n / x
                // Could use fastTwoDiff
                z = DD.ofDifference(1, x);
                z = fpow.pow(z, n, le);
                e = le[0];
                // x in (1/n, 1 - 1/n) so the divide of the fraction is safe
                z = z.divide(x);
                z = z.frexp(ie);
                e += ie[0];
            }

            // sum(Aj) maintained as 2^e * f with f in [0.5, 1)
            DD sum = z;
            long esum = e;
            // Binomial coefficient c(n, j) maintained as 2^e * f with f in [1, 2)
            // This value is integral but maintained to limited precision
            DD c = DD.ONE;
            long ec = 0;
            for (int i = 1; i <= maxN; i++) {
                // c(n, j) = c(n, j-1) * (n-j+1) / j
                z = DD.fromQuotient(n - i + 1, i);
                c = c.multiply(z);
                // Here we maintain c in [1, 2) to restrict the scaled Aj term to [0.25, 2].
                final int b = Math.getExponent(c.hi());
                if (b != 0) {
                    c = c.scalb(-b);
                    ec += b;
                }
                // Compute Aj
                final int j = sd ? n - i : i;
                // Algorithm 4 pp. 27
                // S = ((j/n) + x)^(j-1)
                // T = ((n-j)/n - x)^(n-j)
                z = DD.fromQuotient(j, n);
                z = z.add(x);
                DD s = fpow.pow(z, j - 1, le);
                final long es = le[0];
                z = DD.fromQuotient(n - j, n);
                z = z.add(-x);
                z = fpow.pow(z, n - j, le);
                final long et = le[0];
                // Aj = C(n, j) * T * S
                //    = 2^e * [1, 2] * [0.5, 1] * [0.5, 1]
                //    = 2^e * [0.25, 2]
                e = ec + es + et;
                // Only compute and add to the sum when the exponents overlap by n-bits.
                if (e > esum - sumBits) {
                    z = c.multiply(z);
                    z = z.multiply(s);
                    // Scaling must offset by the scale of the sum
                    z = z.scalb((int) (e - esum));
                    sum = sum.add(z);
                } else {
                    // Terms are expected to increase in magnitude then reduce.
                    // Here the terms are insignificant and we can stop.
                    // Effectively Aj -> eps * sum, and most of the computation is done.
                    break;
                }

                // Re-scale the sum
                sum = sum.frexp(ie);
                esum += ie[0];
            }

            // p = x * sum(Ai). Since the sum is normalized
            // this is safe as long as x does not approach a sub-normal.
            // Typically x in (1/n, 1 - 1/n).
            sum = sum.multiply(x);
            // Rescale the result
            sum = sum.scalb((int) esum);
            if (sd) {
                // SF = 1 - CDF
                sum = sum.negate().add(1);
            }
            return clipProbability(sum.doubleValue());
        }
    }

    /**
     * Clip the probability to the range [0, 1].
     *
     * @param p Probability.
     * @return p in [0, 1]
     */
    static double clipProbability(double p) {
        return Math.min(1, Math.max(0, p));
    }
}
