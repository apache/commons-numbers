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

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.function.ToDoubleFunction;

import org.apache.commons.numbers.core.Sum;

/** Class containing various Euclidean norm computation methods for comparison.
 */
public final class EuclideanNormAlgorithms {

    /** No instantiation. */
    private EuclideanNormAlgorithms() {}

    /** Exact computation method using {@link BigDecimal} and {@link MathContext#DECIMAL128}.
     */
    static final class Exact implements ToDoubleFunction<double[]> {

        /** {@inheritDoc} */
        @Override
        public double applyAsDouble(final double[] v) {
            // compute the sum of squares
            final MathContext ctx = MathContext.DECIMAL128;

            BigDecimal sum = BigDecimal.ZERO;
            BigDecimal n;
            for (int i = 0; i < v.length; ++i) {
                n = Double.isFinite(v[i]) ? new BigDecimal(v[i]) : BigDecimal.ZERO;
                sum = sum.add(n.multiply(n, ctx), ctx);
            }

            return sum.sqrt(ctx).doubleValue();
        }
    }

    /** Direct computation method that simply computes the sums of squares and takes
     * the square root with no special handling of values.
     */
    static final class Direct implements ToDoubleFunction<double[]> {

        /** {@inheritDoc} */
        @Override
        public double applyAsDouble(final double[] v) {
            double n = 0;
            for (int i = 0; i < v.length; i++) {
                n += v[i] * v[i];
            }
            return Math.sqrt(n);
        }
    }

    /** Translation of the <a href="http://www.netlib.org/minpack">minpack</a>
     * "enorm" subroutine. This method handles overflow and underflow.
     */
    static final class Enorm implements ToDoubleFunction<double[]> {

        /** Constant. */
        private static final double R_DWARF = 3.834e-20;
        /** Constant. */
        private static final double R_GIANT = 1.304e+19;

        /** {@inheritDoc} */
        @Override
        public double applyAsDouble(final double[] v) {
            double s1 = 0;
            double s2 = 0;
            double s3 = 0;
            double x1max = 0;
            double x3max = 0;
            final double floatn = v.length;
            final double agiant = R_GIANT / floatn;
            for (int i = 0; i < v.length; i++) {
                final double xabs = Math.abs(v[i]);
                if (xabs < R_DWARF || xabs > agiant) {
                    if (xabs > R_DWARF) {
                        if (xabs > x1max) {
                            final double r = x1max / xabs;
                            s1 = 1 + s1 * r * r;
                            x1max = xabs;
                        } else {
                            final double r = xabs / x1max;
                            s1 += r * r;
                        }
                    } else {
                        if (xabs > x3max) {
                            final double r = x3max / xabs;
                            s3 = 1 + s3 * r * r;
                            x3max = xabs;
                        } else {
                            if (xabs != 0) {
                                final double r = xabs / x3max;
                                s3 += r * r;
                            }
                        }
                    }
                } else {
                    s2 += xabs * xabs;
                }
            }
            double norm;
            if (s1 != 0) {
                norm = x1max * Math.sqrt(s1 + (s2 / x1max) / x1max);
            } else {
                if (s2 == 0) {
                    norm = x3max * Math.sqrt(s3);
                } else {
                    if (s2 >= x3max) {
                        norm = Math.sqrt(s2 * (1 + (x3max / s2) * (x3max * s3)));
                    } else {
                        norm = Math.sqrt(x3max * ((s2 / x3max) + (x3max * s3)));
                    }
                }
            }
            return norm;
        }
    }

    /** Modified version of {@link Enorm} created by Alex Herbert.
     */
    static final class EnormMod implements ToDoubleFunction<double[]> {

        /** {@inheritDoc} */
        @Override
        public double applyAsDouble(final double[] v) {
            // Sum of big, normal and small numbers with 2-fold extended precision summation
            double s1 = 0;
            double s2 = 0;
            double s3 = 0;
            for (int i = 0; i < v.length; i++) {
                final double x = Math.abs(v[i]);
                if (!(x <= Double.MAX_VALUE)) {
                    return x;
                } else if (x > 0x1.0p500) {
                    // Scale down big numbers
                    s1 += square(x * 0x1.0p-600);
                } else if (x < 0x1.0p-500) {
                    // Scale up small numbers
                    s3 += square(x * 0x1.0p600);
                } else {
                    // Unscaled
                    s2 += square(x);
                }
            }
            // The highest sum is the significant component. Add the next significant.
            if (s1 != 0) {
                return Math.sqrt(s1 + s2 * 0x1.0p-600 * 0x1.0p-600) * 0x1.0p600;
            } else if (s2 != 0) {
                return Math.sqrt(s2 + s3 * 0x1.0p-600 * 0x1.0p-600);
            }
            return Math.sqrt(s3) * 0x1.0p-600;
        }

        /** Compute the square of {@code x}.
         * @param x input value
         * @return square of {@code x}
         */
        private static double square(final double x) {
            return x * x;
        }
    }

    /** Version of {@link EnormMod} using Kahan summation.
     */
    static final class EnormModKahan implements ToDoubleFunction<double[]> {

        /** Threshold for scaling small numbers. */
        private static final double SMALL_THRESH = 0x1.0p-500;

        /** Threshold for scaling large numbers. */
        private static final double LARGE_THRESH = 0x1.0p+500;

        /** Value used to scale down large numbers. */
        private static final double SCALE_DOWN = 0x1.0p-600;

        /** Value used to scale up small numbers. */
        private static final double SCALE_UP = 0x1.0p+600;

        /** {@inheritDoc} */
        @Override
        public double applyAsDouble(final double[] v) {
            // Sum of big, normal and small numbers
            double s1 = 0;
            double s2 = 0;
            double s3 = 0;
            double c1 = 0;
            double c2 = 0;
            double c3 = 0;
            for (int i = 0; i < v.length; i++) {
                final double x = Math.abs(v[i]);
                if (x > LARGE_THRESH) {
                    // Scale down big numbers
                    final double y = square(x * SCALE_DOWN) - c1;
                    final double t = s1 + y;
                    c1 = (t - s1) - y;
                    s1 = t;

                } else if (x < SMALL_THRESH) {
                    // Scale up small numbers
                    final double y = square(x * SCALE_UP) - c3;
                    final double t = s3 + y;
                    c3 = (t - s3) - y;
                    s3 = t;
                } else {
                    // Unscaled
                    final double y = square(x) - c2;
                    final double t = s2 + y;
                    c2 = (t - s2) - y;
                    s2 = t;
                }
            }
            // The highest sum is the significant component. Add the next significant.
            // Add the scaled compensation then the scaled sum.
            if (s1 != 0) {
                double y = c2 * SCALE_DOWN * SCALE_DOWN - c1;
                final double t = s1 + y;
                c1 = (t - s1) - y;
                y = s2 * SCALE_DOWN * SCALE_DOWN - c1;
                return Math.sqrt(t + y) * SCALE_UP;
            } else if (s2 != 0) {
                double y = c3 * SCALE_DOWN * SCALE_DOWN - c2;
                final double t = s2 + y;
                c2 = (t - s2) - y;
                y = s3 * SCALE_DOWN * SCALE_DOWN - c2;
                return Math.sqrt(t + y);
            }
            return Math.sqrt(s3) * SCALE_DOWN;
        }

        /** Compute the square of {@code x}.
         * @param x input value
         * @return square of {@code x}
         */
        private static double square(final double x) {
            return x * x;
        }
    }

    /** Version of {@link EnormMod} using extended precision summation.
     */
    static final class EnormModExt implements ToDoubleFunction<double[]> {

        /** Threshold for scaling small numbers. */
        private static final double SMALL_THRESH = 0x1.0p-500;

        /** Threshold for scaling large numbers. */
        private static final double LARGE_THRESH = 0x1.0p+500;

        /** Value used to scale down large numbers. */
        private static final double SCALE_DOWN = 0x1.0p-600;

        /** Value used to scale up small numbers. */
        private static final double SCALE_UP = 0x1.0p+600;

        /** {@inheritDoc} */
        @Override
        public double applyAsDouble(final double[] v) {
         // Sum of big, normal and small numbers with 2-fold extended precision summation
            double s1 = 0;
            double s2 = 0;
            double s3 = 0;
            double c1 = 0;
            double c2 = 0;
            double c3 = 0;
            for (int i = 0; i < v.length; i++) {
                final double x = Math.abs(v[i]);
                if (!(x <= Double.MAX_VALUE)) {
                    return x;
                } else if (x > LARGE_THRESH) {
                    // Scale down big numbers
                    final double y = square(x * SCALE_DOWN);
                    final double t = s1 + y;
                    c1 += DoublePrecision.twoSumLow(s1, y, t);
                    s1 = t;
                } else if (x < SMALL_THRESH) {
                    // Scale up small numbers
                    final double y = square(x * SCALE_UP);
                    final double t = s3 + y;
                    c3 += DoublePrecision.twoSumLow(s3, y, t);
                    s3 = t;
                } else {
                    // Unscaled
                    final double y = square(x);
                    final double t = s2 + y;
                    c2 += DoublePrecision.twoSumLow(s2, y, t);
                    s2 = t;
                }
            }
            // The highest sum is the significant component. Add the next significant.
            // Adapted from LinearCombination dot2s summation.
            if (s1 != 0) {
                s2 = s2 * SCALE_DOWN * SCALE_DOWN;
                c2 = c2 * SCALE_DOWN * SCALE_DOWN;
                double sum = s1 + s2;
                c1 += DoublePrecision.twoSumLow(s1, s2, sum) + c2;
                return Math.sqrt(sum + c1) * SCALE_UP;
            } else if (s2 != 0) {
                s3 = s3 * SCALE_DOWN * SCALE_DOWN;
                c3 = c3 * SCALE_DOWN * SCALE_DOWN;
                double sum = s2 + s3;
                c2 += DoublePrecision.twoSumLow(s2, s3, sum) + c3;
                return Math.sqrt(sum + c2);
            }
            return Math.sqrt(s3) * SCALE_DOWN;
        }

        /** Compute the square of {@code x}.
         * @param x input value
         * @return square of {@code x}
         */
        private static double square(final double x) {
            return x * x;
        }
    }

    /** Euclidean norm computation algorithm that uses {@link LinearCombinations} to perform
     * an extended precision summation.
     */
    static final class ExtendedPrecisionLinearCombination implements ToDoubleFunction<double[]> {

        /** {@inheritDoc} */
        @Override
        public double applyAsDouble(final double[] v) {
            // Find the magnitude limits ignoring zero
            double max = 0;
            double min = Double.POSITIVE_INFINITY;
            for (int i = 0; i < v.length; i++) {
                final double x = Math.abs(v[i]);
                if (Double.isNaN(x)) {
                    return x;
                } else if (x > max) {
                    max = x;
                } else if (x < min && x != 0) {
                    min = x;
                }
            }
            // Edge cases
            if (max == 0 || max == Double.POSITIVE_INFINITY) {
                return max;
            }
            // Use scaling if required
            double[] x = v;
            double rescale = 1;
            if (max > 0x1.0p500) {
                // Too big so scale down
                x = x.clone();
                for (int i = 0; i < x.length; i++) {
                    x[i] *= 0x1.0p-600;
                }
                rescale = 0x1.0p600;
            } else if (min < 0x1.0p-500 && max < 0x1.0p100) {
                // Too small so scale up
                x = x.clone();
                for (int i = 0; i < x.length; i++) {
                    x[i] *= 0x1.0p600;
                }
                rescale = 0x1.0p-600;
            }
            return Math.sqrt(Sum.ofProducts(x, x).getAsDouble()) * rescale;
        }
    }

    /** Modification of {@link ExtendedPrecisionLinearCombination} that uses an optimized version of the
     * linear combination computation.
     */
    static final class ExtendedPrecisionLinearCombinationMod implements ToDoubleFunction<double[]> {

        /** Threshold for scaling small numbers. */
        private static final double SMALL_THRESH = 0x1.0p-500;

        /** Threshold for scaling large numbers. */
        private static final double LARGE_THRESH = 0x1.0p+500;

        /** Value used to scale down large numbers. */
        private static final double SCALE_DOWN = 0x1.0p-600;

        /** Value used to scale up small numbers. */
        private static final double SCALE_UP = 0x1.0p+600;

        /** {@inheritDoc} */
        @Override
        public double applyAsDouble(final double[] v) {
            // Find the magnitude limits ignoring zero
            double max = 0;
            double min = Double.POSITIVE_INFINITY;
            for (int i = 0; i < v.length; i++) {
                final double x = Math.abs(v[i]);
                if (Double.isNaN(x)) {
                    return x;
                } else if (x > max) {
                    max = x;
                } else if (x < min && x != 0) {
                    min = x;
                }
            }
            // Edge cases
            if (max == 0 || max == Double.POSITIVE_INFINITY) {
                return max;
            }

            // Below here no value is infinite or NaN.

            // Use scaling if required
            double scale = 1;
            double rescale = 1;
            if (max > LARGE_THRESH) {
                // Too big so scale down
                scale = SCALE_DOWN;
                rescale = SCALE_UP;
            } else if (min < SMALL_THRESH && max < 0x1.0p100) {
                // Too small so scale up
                scale = SCALE_UP;
                rescale = SCALE_DOWN;
            }

            // Same as LinearCombination but with scaling.
            // Splitting is safe due to scaling and only one term requires splitting.

            // Implement dot2s (Algorithm 5.4) from Ogita et al (2005).
            final int len = v.length;

            // p is the standard scalar product sum.
            // s is the sum of round-off parts.
            double a = v[0] * scale;
            double p = a * a;
            double s = productLowUnscaled(a, p);

            // Remaining split products added to the current sum and round-off sum.
            for (int i = 1; i < len; i++) {
                a = v[i] * scale;
                final double h = a * a;
                final double r = productLowUnscaled(a, h);

                final double x = p + h;
                // s_i = s_(i-1) + (q_i + r_i)
                s += DoublePrecision.twoSumLow(p, h, x) + r;
                p = x;
            }
            p += s;

            return Math.sqrt(p) * rescale;
        }

        /**
         * Compute the low part of the double length number {@code (z,zz)} for the exact
         * square of {@code x} using Dekker's mult12 algorithm. The standard
         * precision product {@code x*x} must be provided. The number {@code x}
         * is split into high and low parts using Dekker's algorithm.
         *
         * <p>Warning: This method does not perform scaling in Dekker's split and large
         * finite numbers can create NaN results.
         *
         * @param x The factor.
         * @param xx Square of the factor (x * x).
         * @return the low part of the product double length number
         */
        private static double productLowUnscaled(double x, double xx) {
            // Split the numbers using Dekker's algorithm without scaling
            final double hx = DoublePrecision.highPartUnscaled(x);
            final double lx = x - hx;

            return DoublePrecision.productLow(hx, lx, hx, lx, xx);
        }
    }

    /** Modification of {@link ExtendedPrecisionLinearCombination} that only uses a single pass through
     * the input array.
     */
    static final class ExtendedPrecisionLinearCombinationSinglePass implements ToDoubleFunction<double[]> {

        /** Threshold for scaling small numbers. */
        private static final double SMALL_THRESH = 0x1.0p-500;

        /** Threshold for scaling large numbers. */
        private static final double LARGE_THRESH = 0x1.0p+500;

        /** Value used to scale down large numbers. */
        private static final double SCALE_DOWN = 0x1.0p-600;

        /** Value used to scale up small numbers. */
        private static final double SCALE_UP = 0x1.0p+600;

        /** {@inheritDoc} */
        @Override
        public double applyAsDouble(final double[] v) {
            double s1 = 0;
            double s2 = 0;
            double s3 = 0;
            double c1 = 0;
            double c2 = 0;
            double c3 = 0;
            for (int i = 0; i < v.length; ++i) {
                final double x = Math.abs(v[i]);
                if (Double.isNaN(x)) {
                    // found an NaN; no use in continuing
                    return x;
                } else if (x > LARGE_THRESH) {
                    // scale down
                    final double sx = x * SCALE_DOWN;

                    // compute the product and product correction
                    final double p = sx * sx;
                    final double cp = productLowUnscaled(sx, p);

                    // compute the running sum and sum correction
                    final double s = s1 + p;
                    final double cs = DoublePrecision.twoSumLow(s1, p, s);

                    // update running totals
                    c1 += cp + cs;
                    s1 = s;
                } else if (x < SMALL_THRESH) {
                    // scale up
                    final double sx = x * SCALE_UP;

                    // compute the product and product correction
                    final double p = sx * sx;
                    final double cp = productLowUnscaled(sx, p);

                    // compute the running sum and sum correction
                    final double s = s3 + p;
                    final double cs = DoublePrecision.twoSumLow(s3, p, s);

                    // update running totals
                    c3 += cp + cs;
                    s3 = s;
                } else {
                    // no scaling
                    // compute the product and product correction
                    final double p = x * x;
                    final double cp = productLowUnscaled(x, p);

                    // compute the running sum and sum correction
                    final double s = s2 + p;
                    final double cs = DoublePrecision.twoSumLow(s2, p, s);

                    // update running totals
                    c2 += cp + cs;
                    s2 = s;
                }
            }

            if (s1 != 0) {
                // add s1, s2, c1, c2
                s2 = s2 * SCALE_DOWN * SCALE_DOWN;
                c2 = c2 * SCALE_DOWN * SCALE_DOWN;
                final double sum = s1 + s2;
                c1 += DoublePrecision.twoSumLow(s1, s2, sum) + c2;
                return Math.sqrt(sum + c1) * SCALE_UP;
            } else if (s2 != 0) {
                // add s2, s3, c2, c3
                s3 = s3 * SCALE_DOWN * SCALE_DOWN;
                c3 = c3 * SCALE_DOWN * SCALE_DOWN;
                final double sum = s2 + s3;
                c2 += DoublePrecision.twoSumLow(s2, s3, sum) + c3;
                return Math.sqrt(sum + c2);
            }
            // add s3, c3
            return Math.sqrt(s3 + c3) * SCALE_DOWN;
        }

        /**
         * Compute the low part of the double length number {@code (z,zz)} for the exact
         * square of {@code x} using Dekker's mult12 algorithm. The standard
         * precision product {@code x*x} must be provided. The number {@code x}
         * is split into high and low parts using Dekker's algorithm.
         *
         * <p>Warning: This method does not perform scaling in Dekker's split and large
         * finite numbers can create NaN results.
         *
         * @param x The factor.
         * @param xx Square of the factor (x * x).
         * @return the low part of the product double length number
         */
        private static double productLowUnscaled(double x, double xx) {
            // Split the numbers using Dekker's algorithm without scaling
            final double hx = DoublePrecision.highPartUnscaled(x);
            final double lx = x - hx;

            return DoublePrecision.productLow(hx, lx, hx, lx, xx);
        }
    }

    /** Modification of {@link ExtendedPrecisionLinearCombination} that only uses a single pass through
     * the input array as well as an extended precision sqrt computation.
     */
    static final class ExtendedPrecisionLinearCombinationSqrt2 implements ToDoubleFunction<double[]> {

        /** Threshold for scaling small numbers. */
        private static final double SMALL_THRESH = 0x1.0p-500;

        /** Threshold for scaling large numbers. */
        private static final double LARGE_THRESH = 0x1.0p+500;

        /** Value used to scale down large numbers. */
        private static final double SCALE_DOWN = 0x1.0p-600;

        /** Value used to scale up small numbers. */
        private static final double SCALE_UP = 0x1.0p+600;

        /** {@inheritDoc} */
        @Override
        public double applyAsDouble(final double[] v) {
            double s1 = 0;
            double s2 = 0;
            double s3 = 0;
            double c1 = 0;
            double c2 = 0;
            double c3 = 0;
            for (int i = 0; i < v.length; ++i) {
                final double x = Math.abs(v[i]);
                if (Double.isNaN(x)) {
                    // found an NaN; no use in continuing
                    return x;
                } else if (x > LARGE_THRESH) {
                    // scale down
                    final double sx = x * SCALE_DOWN;

                    // compute the product and product correction
                    final double p = sx * sx;
                    final double cp = productLowUnscaled(sx, p);

                    // compute the running sum and sum correction
                    final double s = s1 + p;
                    final double cs = DoublePrecision.twoSumLow(s1, p, s);

                    // update running totals
                    c1 += cp + cs;
                    s1 = s;
                } else if (x < SMALL_THRESH) {
                    // scale up
                    final double sx = x * SCALE_UP;

                    // compute the product and product correction
                    final double p = sx * sx;
                    final double cp = productLowUnscaled(sx, p);

                    // compute the running sum and sum correction
                    final double s = s3 + p;
                    final double cs = DoublePrecision.twoSumLow(s3, p, s);

                    // update running totals
                    c3 += cp + cs;
                    s3 = s;
                } else {
                    // no scaling
                    // compute the product and product correction
                    final double p = x * x;
                    final double cp = productLowUnscaled(x, p);

                    // compute the running sum and sum correction
                    final double s = s2 + p;
                    final double cs = DoublePrecision.twoSumLow(s2, p, s);

                    // update running totals
                    c2 += cp + cs;
                    s2 = s;
                }
            }

            if (s1 != 0) {
                // add s1, s2, c1, c2
                s2 = s2 * SCALE_DOWN * SCALE_DOWN;
                c2 = c2 * SCALE_DOWN * SCALE_DOWN;
                final double sum = s1 + s2;
                c1 += DoublePrecision.twoSumLow(s1, s2, sum) + c2;

                final double f = sum + c1;
                final double ff = DoublePrecision.twoSumLow(sum, c1, f);
                return sqrt2(f, ff) * SCALE_UP;
            } else if (s2 != 0) {
                // add s2, s3, c2, c3
                s3 = s3 * SCALE_DOWN * SCALE_DOWN;
                c3 = c3 * SCALE_DOWN * SCALE_DOWN;
                final double sum = s2 + s3;
                c2 += DoublePrecision.twoSumLow(s2, s3, sum) + c3;

                final double f = sum + c2;
                final double ff = DoublePrecision.twoSumLow(sum, c2, f);
                return sqrt2(f, ff);
            }
            // add s3, c3
            final double f = s3 + c3;
            final double ff = DoublePrecision.twoSumLow(s3, c3, f);
            return sqrt2(f, ff) * SCALE_DOWN;
        }

        /**
         * Compute the low part of the double length number {@code (z,zz)} for the exact
         * square of {@code x} using Dekker's mult12 algorithm. The standard
         * precision product {@code x*x} must be provided. The number {@code x}
         * is split into high and low parts using Dekker's algorithm.
         *
         * <p>Warning: This method does not perform scaling in Dekker's split and large
         * finite numbers can create NaN results.
         *
         * @param x The factor.
         * @param xx Square of the factor (x * x).
         * @return the low part of the product double length number
         */
        private static double productLowUnscaled(double x, double xx) {
            // Split the numbers using Dekker's algorithm without scaling
            final double hx = DoublePrecision.highPartUnscaled(x);
            final double lx = x - hx;

            return DoublePrecision.productLow(hx, lx, hx, lx, xx);
        }

       /**
        * Compute the extended precision square root from the split number
        *  {@code x, xx}.
        * This is a modification of Dekker's sqrt2 algorithm to ignore the
        * roundoff of the square root.
        *
        * @param x the high part
        * @param xx the low part
        * @return the double
        */
        private static double sqrt2(final double x, final double xx) {
            if (x > 0) {
                double c = Math.sqrt(x);
                double u = c * c;
                //double uu = ExtendedPrecision.productLow(c, c, u);
                // Here we use the optimised version:
                double uu = productLowUnscaled(c, u);
                double cc = (x - u - uu + xx) * 0.5 / c;
                // Extended precision sqrt (y, yy)
                // y = c + cc
                // yy = c - y + cc (ignored)
                return c + cc;
            }
            return x;
        }
    }
}
