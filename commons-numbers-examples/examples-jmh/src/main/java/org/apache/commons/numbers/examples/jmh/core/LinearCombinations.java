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
import java.util.function.IntFunction;

import org.apache.commons.numbers.examples.jmh.core.LinearCombination.FourD;
import org.apache.commons.numbers.examples.jmh.core.LinearCombination.ND;
import org.apache.commons.numbers.examples.jmh.core.LinearCombination.ThreeD;
import org.apache.commons.numbers.examples.jmh.core.LinearCombination.TwoD;

/**
 * Provides implementations to computes linear combinations as the the sum of
 * the products of two sequences of numbers
 * <code>a<sub>i</sub> b<sub>i</sub></code>.
 *
 * @see LinearCombination
 */
public final class LinearCombinations {

    /** No public constructor. */
    private LinearCombinations() {}

    /**
     * Base class to compute a linear combination with high accuracy.
     * Contains common code for computing short combinations and computing
     * the standard precision sum of products.
     */
    public abstract static class BaseLinearCombination implements LinearCombination.ND {
        /** {@inheritDoc} */
        @Override
        public double value(double[] a, double[] b) {
            if (a.length != b.length) {
                throw new IllegalArgumentException("Dimension mismatch: " + a.length + " != " + b.length);
            }

            final int len = a.length;

            if (len == 0) {
                return 0;
            }
            if (len == 1) {
                // Revert to scalar multiplication.
                return a[0] * b[0];
            }

            return computeValue(a, b);
        }

        /**
         * Compute the sum of the products of two sequences of factors with high accuracy.
         * The input arrays will have a length of at least 2; the lengths will
         * be the same.
         *
         * @param a Factors.
         * @param b Factors.
         * @return \( \sum_i a_i b_i \).
         */
        protected abstract double computeValue(double[] a, double[] b);

        /**
         * Compute the sum of the products of two sequences of factors.
         * This is a standard precision summation for the IEEE754 result.
         *
         * @param a Factors.
         * @param b Factors.
         * @return \( \sum_i a_i b_i \).
         */
        static double standardDotProduct(double[] a, double[] b) {
            double result = 0;
            for (int i = 0; i < a.length; ++i) {
                result += a[i] * b[i];
            }
            return result;
        }
    }

    /**
     * Computes linear combinations using standard precision multiplication and summation.
     *
     * <p>This class is used for a baseline and does not provide high precision results.
     */
    static final class StandardPrecision implements TwoD, ThreeD, FourD, ND {
        /** An instance. */
        static final StandardPrecision INSTANCE = new StandardPrecision();

        /** Private constructor. */
        private StandardPrecision() {}

        @Override
        public double value(double[] a, double[] b) {
            double sum = 0;
            for (int i = 0; i < a.length; i++) {
                sum += a[i] * b[i];
            }
            return sum;
        }

        @Override
        public double value(double a1, double b1, double a2, double b2) {
            return a1 * b1 + a2 * b2;
        }

        @Override
        public double value(double a1, double b1, double a2, double b2, double a3, double b3) {
            return a1 * b1 + a2 * b2 + a3 * b3;
        }

        @Override
        public double value(double a1, double b1, double a2, double b2, double a3, double b3, double a4, double b4) {
            return a1 * b1 + a2 * b2 + a3 * b3 + a4 * b4;
        }
    }

    /**
     * Computes linear combinations using the double-length multiplication and summation
     * algorithms of Dekker.
     *
     * @see <a href="https://doi.org/10.1007/BF01397083">
     * Dekker (1971) A floating-point technique for extending the available precision</a>
     */
    public static final class Dekker extends BaseLinearCombination implements TwoD, ThreeD, FourD {
        /** An instance. */
        public static final Dekker INSTANCE = new Dekker();

        /** Private constructor. */
        private Dekker() {}

        @Override
        protected double computeValue(double[] a, double[] b) {
            // Dekker's dot product method:
            // Dekker's multiply and then Dekker's add2 to sum in double length precision.

            double x = a[0] * b[0];
            double xx = DoublePrecision.productLow(a[0], b[0], x);

            // Remaining split products added to the current sum and round-off stored
            for (int i = 1; i < a.length; i++) {
                final double y = a[i] * b[i];
                final double yy = DoublePrecision.productLow(a[i], b[i], y);

                // sum this to the previous number using Dekker's add2 algorithm
                final double r = x + y;
                final double s = DoublePrecision.sumLow(x, xx, y, yy, r);

                x = r + s;
                // final split product does not require the round-off but included here for brevity
                xx = r - x + s;
            }

            if (!Double.isFinite(x)) {
                // Either we have split infinite numbers or some coefficients were NaNs,
                // just rely on the naive implementation and let IEEE754 handle this
                return standardDotProduct(a, b);
            }
            return x;
        }

        @Override
        public double value(double a1, double b1,
                            double a2, double b2) {
            final double x = a1 * b1;
            final double xx = DoublePrecision.productLow(a1, b1, x);
            final double y = a2 * b2;
            final double yy = DoublePrecision.productLow(a2, b2, y);
            double r = x + y;
            final double s = DoublePrecision.sumLow(x, xx, y, yy, r);
            r = r + s;

            if (!Double.isFinite(r)) {
                // Either we have split infinite numbers or some coefficients were NaNs,
                // just rely on the naive implementation and let IEEE754 handle this
                return x + y;
            }
            return r;
        }

        @Override
        public double value(double a1, double b1,
                            double a2, double b2,
                            double a3, double b3) {
            double x = a1 * b1;
            double xx = DoublePrecision.productLow(a1, b1, x);
            double y = a2 * b2;
            double yy = DoublePrecision.productLow(a2, b2, y);
            double r = x + y;
            double s = DoublePrecision.sumLow(x, xx, y, yy, r);
            x = r + s;
            xx = r - x + s;
            y = a3 * b3;
            yy = DoublePrecision.productLow(a3, b3, y);
            r = x + y;
            s = DoublePrecision.sumLow(x, xx, y, yy, r);
            x = r + s;

            if (!Double.isFinite(x)) {
                // Either we have split infinite numbers or some coefficients were NaNs,
                // just rely on the naive implementation and let IEEE754 handle this
                return a1 * b1 + a2 * b2 + y;
            }
            return x;
        }

        @Override
        public double value(double a1, double b1,
                            double a2, double b2,
                            double a3, double b3,
                            double a4, double b4) {
            double x = a1 * b1;
            double xx = DoublePrecision.productLow(a1, b1, x);
            double y = a2 * b2;
            double yy = DoublePrecision.productLow(a2, b2, y);
            double r = x + y;
            double s = DoublePrecision.sumLow(x, xx, y, yy, r);
            x = r + s;
            xx = r - x + s;
            y = a3 * b3;
            yy = DoublePrecision.productLow(a3, b3, y);
            r = x + y;
            s = DoublePrecision.sumLow(x, xx, y, yy, r);
            x = r + s;
            xx = r - x + s;
            y = a4 * b4;
            yy = DoublePrecision.productLow(a4, b4, y);
            r = x + y;
            s = DoublePrecision.sumLow(x, xx, y, yy, r);
            x = r + s;

            if (!Double.isFinite(x)) {
                // Either we have split infinite numbers or some coefficients were NaNs,
                // just rely on the naive implementation and let IEEE754 handle this
                return a1 * b1 + a2 * b2 + a3 * b3 + y;
            }
            return x;
        }
    }

    /**
     * Computes linear combinations accurately using the DotK algorithm of Ogita et al
     * for K-fold precision of the sum.
     *
     * <p>It is based on the 2005 paper
     * <a href="http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.2.1547">
     * Accurate Sum and Dot Product</a> by Takeshi Ogita, Siegfried M. Rump,
     * and Shin'ichi Oishi published in <em>SIAM J. Sci. Comput</em>.
     *
     * <p>Note: It is possible to use this class to compute 2-fold precision. In this case
     * the round-off parts of the dot-product are stored in an array and summed. The
     * {@link Dot2s} class provides an alternative faster implementation that sums the
     * round-off parts during processing to avoid array allocation overhead. The results will
     * not be identical due to a different order for the summation of the round-off parts.
     *
     * <p>The number of operations will scale with {@code k}. When computing the small linear
     * combinations {@link TwoD}, {@link ThreeD} and {@link FourD} using a {@code k} value too
     * large will result in more operations than the number used by {@link ExtendedPrecision}.
     * For a small {@code n}-dimension linear combinations the value {@code k = n+1} will have the
     * same number of operations as {@link ExtendedPrecision} and users should switch to the
     * {@link ExtendedPrecision} class. For example 3D combinations should use {@code k} up to
     * 3 or else switch to using {@link ExtendedPrecision}. This rule does not apply to the
     * {@link org.apache.commons.numbers.examples.jmh.core.LinearCombination.ND ND}
     * implementations as {@link ExtendedPrecision} eliminates redundant operations
     * on zeros. In this case equivalent performance will be observed when {@code k <= n+1}.
     * Choice of implementation must consider performance and accuracy on real-world data.
     */
    public static final class DotK extends BaseLinearCombination implements TwoD, ThreeD, FourD {
        /** An instance computing 3-fold precision. */
        public static final DotK DOT_3 = new DotK(3);
        /** An instance computing 4-fold precision. */
        public static final DotK DOT_4 = new DotK(4);
        /** An instance computing 5-fold precision. */
        public static final DotK DOT_5 = new DotK(5);
        /** An instance computing 6-fold precision. */
        public static final DotK DOT_6 = new DotK(6);
        /** An instance computing 7-fold precision. */
        public static final DotK DOT_7 = new DotK(7);

        /** The k-fold precision to compute. */
        private final int k;
        /** The factory to create a working array. */
        private final IntFunction<double[]> arrayFactory;

        /**
         * Instantiates a new dot K.
         *
         * @param k K-fold precision.
         */
        public DotK(int k) {
            this(k, double[]::new);
        }

        /**
         * Instantiates a new dot K.
         *
         * @param k K-fold precision.
         * @param arrayFactory The factory to create a working array.
         */
        DotK(int k, IntFunction<double[]> arrayFactory) {
            this.k = k;
            this.arrayFactory = arrayFactory;
        }

        /** {@inheritDoc} */
        @Override
        protected double computeValue(double[] a, double[] b) {
            // Implement dotK (Algorithm 5.10) from Ogita et al (2005).
            // Store all round-off parts.
            // Round-off parts of each product are r[0 to (n-1)].
            // Round-off parts of each sum are r[n to (2n-2)].
            // The standard precision scalar product is term p which becomes r[2n-1].
            final int len = a.length;
            final double[] r = arrayFactory.apply(len * 2);

            // p is the standard scalar product sum initialized with the first product
            double p = a[0] * b[0];
            r[0] = DoublePrecision.productLow(a[0], b[0], p);

            // Remaining split products added to the current sum and round-off stored
            for (int i = 1; i < len; i++) {
                final double h = a[i] * b[i];
                r[i] = DoublePrecision.productLow(a[i], b[i], h);

                final double x = p + h;
                r[i + len - 1] = DoublePrecision.twoSumLow(p, h, x);
                p = x;
            }

            // Sum the round-off with the standard sum as the final component.
            // Here the value passed to sumK is (K-1) for K-fold precision of the sum (dotK).
            // Increasing K may be of benefit for longer arrays.
            // The iteration is an error-free transform and higher K never loses precision.
            r[r.length - 1] = p;
            return getSum(p, sumK(r, k - 1));
        }

        /** {@inheritDoc} */
        @Override
        public double value(double a1, double b1,
                            double a2, double b2) {
            // Round-off parts of each product are r[0-1].
            // Round-off parts of each sum are r[2].
            // Working variables p/q (new/old sum).
            // The standard precision scalar product is stored in s.

            double p = a1 * b1;
            double r0 = DoublePrecision.productLow(a1, b1, p);
            double q = a2 * b2;
            double r1 = DoublePrecision.productLow(a2, b2, q);
            final double s = p + q;
            double r2 = DoublePrecision.twoSumLow(p, q, s);
            double r3 = s;

            // In-line k-2 rounds of vector sum for k-fold precision
            for (int i = 2; i < k; i++) {
                q = r1 + r0;
                r0 = DoublePrecision.twoSumLow(r1, r0, q);
                p = r2 + q;
                r1 = DoublePrecision.twoSumLow(r2, q, p);
                q = r3 + p;
                r2 = DoublePrecision.twoSumLow(r3, p, q);
                r3 = q;
            }

            // Final summation
            return getSum(s, r0 + r1 + r2 + r3);
        }

        /** {@inheritDoc} */
        @Override
        public double value(double a1, double b1,
                            double a2, double b2,
                            double a3, double b3) {
            // Round-off parts of each product are r[0-2].
            // Round-off parts of each sum are r[3-4].
            // Working variables p/q (new/old sum) and h (current product high part).
            // The standard precision scalar product is stored in s.

            double p = a1 * b1;
            double r0 = DoublePrecision.productLow(a1, b1, p);
            double h = a2 * b2;
            double r1 = DoublePrecision.productLow(a2, b2, h);
            double q = p + h;
            double r3 = DoublePrecision.twoSumLow(p, h, q);
            h = a3 * b3;
            double r2 = DoublePrecision.productLow(a3, b3, h);
            final double s = q + h;
            double r4 = DoublePrecision.twoSumLow(q, h, s);
            double r5 = s;

            // In-line k-2 rounds of vector sum for k-fold precision
            for (int i = 2; i < k; i++) {
                q = r1 + r0;
                r0 = DoublePrecision.twoSumLow(r1, r0, q);
                p = r2 + q;
                r1 = DoublePrecision.twoSumLow(r2, q, p);
                q = r3 + p;
                r2 = DoublePrecision.twoSumLow(r3, p, q);
                p = r4 + q;
                r3 = DoublePrecision.twoSumLow(r4, q, p);
                q = r5 + p;
                r4 = DoublePrecision.twoSumLow(r5, p, q);
                r5 = q;
            }

            // Final summation
            return getSum(s, r0 + r1 + r2 + r3 + r4 + r5);
        }

        /** {@inheritDoc} */
        @Override
        public double value(double a1, double b1,
                            double a2, double b2,
                            double a3, double b3,
                            double a4, double b4) {
            // Round-off parts of each product are r[0-3].
            // Round-off parts of each sum are r[4-6].
            // Working variables p/q (new/old sum) and h (current product high part).
            // The standard precision scalar product is stored in s.

            double p = a1 * b1;
            double r0 = DoublePrecision.productLow(a1, b1, p);
            double h = a2 * b2;
            double r1 = DoublePrecision.productLow(a2, b2, h);
            double q = p + h;
            double r4 = DoublePrecision.twoSumLow(p, h, q);
            h = a3 * b3;
            double r2 = DoublePrecision.productLow(a3, b3, h);
            p = q + h;
            double r5 = DoublePrecision.twoSumLow(q, h, p);
            h = a4 * b4;
            double r3 = DoublePrecision.productLow(a4, b4, h);
            final double s = p + h;
            double r6 = DoublePrecision.twoSumLow(p, h, s);
            double r7 = s;

            // In-line k-2 rounds of vector sum for k-fold precision
            for (int i = 2; i < k; i++) {
                q = r1 + r0;
                r0 = DoublePrecision.twoSumLow(r1, r0, q);
                p = r2 + q;
                r1 = DoublePrecision.twoSumLow(r2, q, p);
                q = r3 + p;
                r2 = DoublePrecision.twoSumLow(r3, p, q);
                p = r4 + q;
                r3 = DoublePrecision.twoSumLow(r4, q, p);
                q = r5 + p;
                r4 = DoublePrecision.twoSumLow(r5, p, q);
                p = r6 + q;
                r5 = DoublePrecision.twoSumLow(r6, q, p);
                q = r7 + p;
                r6 = DoublePrecision.twoSumLow(r7, p, q);
                r7 = q;
            }

            // Final summation
            return getSum(s, r0 + r1 + r2 + r3 + r4 + r5 + r6 + r7);
        }

        /**
         * Sum to K-fold precision.
         *
         * @param p Data to sum.
         * @param km1 The precision (k-1).
         * @return the sum
         */
        private static double sumK(double[] p, int km1) {
            // (k-1)=1 will skip the vector transformation and sum in standard precision.
            for (int i = 1; i < km1; i++) {
                vectorSum(p);
            }
            double sum = 0;
            for (final double pi : p) {
                sum += pi;
            }
            return sum;
        }

        /**
         * Error free vector transformation for summation.
         *
         * @param p Data.
         */
        private static void vectorSum(double[] p) {
            for (int i = 1; i < p.length; i++) {
                final double x = p[i] + p[i - 1];
                p[i - 1] = DoublePrecision.twoSumLow(p[i], p[i - 1], x);
                p[i] = x;
            }
        }
    }

    /**
     * Computes linear combinations accurately using the Dot2s algorithm of Ogita et al
     * for 2-fold precision of the sum.
     *
     * <p>It is based on the 2005 paper
     * <a href="http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.2.1547">
     * Accurate Sum and Dot Product</a> by Takeshi Ogita, Siegfried M. Rump,
     * and Shin'ichi Oishi published in <em>SIAM J. Sci. Comput</em>.
     *
     * <p>This is faster than using {@link DotK} with a {@code k} of 2. The results will
     * not be identical due to a different summation order of the round-off parts.
     */
    public static final class Dot2s extends BaseLinearCombination implements TwoD, ThreeD, FourD {
        /** An instance computing 2-fold precision. */
        public static final Dot2s INSTANCE = new Dot2s();

        /** Private constructor. */
        private Dot2s() {}

        /** {@inheritDoc} */
        @Override
        protected double computeValue(double[] a, double[] b) {
            // Implement dot2s (Algorithm 5.4) from Ogita et al (2005).
            final int len = a.length;

            // p is the standard scalar product sum.
            // s is the sum of round-off parts.
            double p = a[0] * b[0];
            double s = DoublePrecision.productLow(a[0], b[0], p);

            // Remaining split products added to the current sum and round-off sum.
            for (int i = 1; i < len; i++) {
                final double h = a[i] * b[i];
                final double r = DoublePrecision.productLow(a[i], b[i], h);

                final double x = p + h;
                // s_i = s_(i-1) + (q_i + r_i)
                s += DoublePrecision.twoSumLow(p, h, x) + r;
                p = x;
            }

            return getSum(p, p + s);
        }

        /** {@inheritDoc} */
        @Override
        public double value(double a1, double b1,
                            double a2, double b2) {
            // p/pn are the standard scalar product old/new sum.
            // s is the sum of round-off parts.
            final double p = a1 * b1;
            double s = DoublePrecision.productLow(a1, b1, p);
            final double h = a2 * b2;
            final double r = DoublePrecision.productLow(a2, b2, h);
            final double pn = p + h;
            s += DoublePrecision.twoSumLow(p, h, pn) + r;

            // Final summation
            return getSum(pn, pn + s);
        }

        /** {@inheritDoc} */
        @Override
        public double value(double a1, double b1,
                            double a2, double b2,
                            double a3, double b3) {
            // Sum round-off parts in s: s_i = s_(i-1) + (q_i + r_i)
            // The standard precision scalar product is stored in p_n.
            final double p = a1 * b1;
            double s = DoublePrecision.productLow(a1, b1, p);
            double h = a2 * b2;
            double r = DoublePrecision.productLow(a2, b2, h);
            final double q = p + h;
            s += r + DoublePrecision.twoSumLow(p, h, q);
            h = a3 * b3;
            r = DoublePrecision.productLow(a3, b3, h);
            final double pn = q + h;
            s += r + DoublePrecision.twoSumLow(q, h, pn);

            // Final summation
            return getSum(pn, pn + s);
        }

        /** {@inheritDoc} */
        @Override
        public double value(double a1, double b1,
                            double a2, double b2,
                            double a3, double b3,
                            double a4, double b4) {
            // p/q are the standard scalar product old/new sum (alternating).
            // s is the sum of round-off parts.
            // pn is the final scalar product sum.
            double p = a1 * b1;
            double s = DoublePrecision.productLow(a1, b1, p);
            double h = a2 * b2;
            double r = DoublePrecision.productLow(a2, b2, h);
            final double q = p + h;
            s += DoublePrecision.twoSumLow(p, h, q) + r;
            h = a3 * b3;
            r = DoublePrecision.productLow(a3, b3, h);
            p = q + h;
            s += DoublePrecision.twoSumLow(q, h, p) + r;
            h = a4 * b4;
            r = DoublePrecision.productLow(a4, b4, h);
            final double pn = p + h;
            s += DoublePrecision.twoSumLow(p, h, pn) + r;

            // Final summation
            return getSum(pn, pn + s);
        }
    }

    /**
     * Computes linear combinations exactly using BigDecimal.
     * This computation may be prohibitively slow on large combination sums.
     */
    public static final class Exact extends BaseLinearCombination implements TwoD, ThreeD, FourD {
        /** An instance. */
        public static final Exact INSTANCE = new Exact();

        /** Private constructor. */
        private Exact() {}

        @Override
        protected double computeValue(double[] a, double[] b) {
            // BigDecimal cannot handle inf/nan so check the arguments and return the IEEE754 result
            if (!areFinite(a) || !areFinite(b)) {
                return standardDotProduct(a, b);
            }
            BigDecimal sum = multiply(a[0], b[0]);
            for (int i = 1; i < a.length; i++) {
                sum = sum.add(multiply(a[i], b[i]));
            }
            return sum.doubleValue();
        }

        @Override
        public double value(double a1, double b1,
                            double a2, double b2) {
            // BigDecimal cannot handle inf/nan so check the arguments and return the IEEE754 result
            if (!areFinite(a1, b1, a2, b2)) {
                return a1 * b1 + a2 * b2;
            }
            return multiply(a1, b1)
                    .add(multiply(a2, b2))
                    .doubleValue();
        }

        @Override
        public double value(double a1, double b1,
                            double a2, double b2,
                            double a3, double b3) {
            // BigDecimal cannot handle inf/nan so check the arguments and return the IEEE754 result
            if (!areFinite(a1, b1, a2, b2, a3, b3)) {
                return a1 * b1 + a2 * b2 + a3 * b3;
            }
            return multiply(a1, b1)
                    .add(multiply(a2, b2))
                    .add(multiply(a3, b3))
                    .doubleValue();
        }

        @Override
        public double value(double a1, double b1,
                            double a2, double b2,
                            double a3, double b3,
                            double a4, double b4) {
            // BigDecimal cannot handle inf/nan so check the arguments and return the IEEE754 result
            if (!areFinite(a1, b1, a2, b2, a3, b3, a4, b4)) {
                return a1 * b1 + a2 * b2 + a3 * b3 + a4 * b4;
            }
            return multiply(a1, b1)
                    .add(multiply(a2, b2))
                    .add(multiply(a3, b3))
                    .add(multiply(a4, b4))
                    .doubleValue();
        }

        /**
         * Test that all the values are finite.
         *
         * @param values the values
         * @return true if finite
         */
        private static boolean areFinite(double... values) {
            for (final double value : values) {
                if (!Double.isFinite(value)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Multiply the factors to a BigDecimal.
         *
         * @param a First factor.
         * @param b Second factor.
         * @return the product
         */
        private static BigDecimal multiply(double a, double b) {
            return new BigDecimal(a).multiply(new BigDecimal(b));
        }
    }

    /**
     * Computes linear combinations accurately using extended precision representations of
     * floating point numbers.
     *
     * <p>It is based on the paper by
     * <a href="http://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps">
     * Shewchuk (1997): Arbitrary Precision Floating-Point Arithmetic</a>.
     */
    public static final class ExtendedPrecision extends BaseLinearCombination implements TwoD, ThreeD, FourD {
        /*
         * Note:
         *
         * An expansion representation of a number is a series of non-overlapping floating-point
         * values where the most significant bit of each value is less than the least significant
         * bit of the next value. The summation of the expansion is exact (without round-off error)
         * and is equal to the original number. The largest magnitude value in the expansion is
         * an approximation of the number.
         *
         * Expansions are created from normal numbers by multiplications or additions that
         * represent the result exactly with extended precision. Addition of expansions f and g
         * of length m and n creates an expansion of length (m+n). Some parts of the expansion
         * may be zero and can be eliminated. The size of the expansion is constrained by the
         * maximum and minimum exponents required to represent the original results of
         * multiplication and addition. If all products in the linear combination have
         * approximately the same magnitude the expansion size will be small and the sum
         * is efficient. If the products have a very large range of magnitudes then the expansion
         * will require a large size and the sum is computationally more expensive.
         */

        /**
         * An instance computing the final summation of the extended precision value
         * using standard precision. The sum will be within 1 ULP of the exact result.
         */
        public static final ExtendedPrecision INSTANCE = new ExtendedPrecision(ApproximationSum.INSTANCE);

        /**
         * An instance computing the final summation of the extended precision value
         * using two-fold precision. The sum will be within 1 ULP of the exact result.
         */
        public static final ExtendedPrecision DOUBLE = new ExtendedPrecision(TwoSum.INSTANCE);

        /**
         * An instance computing the final summation of the extended precision value exactly.
         */
        public static final ExtendedPrecision EXACT = new ExtendedPrecision(ExactSum.INSTANCE);

        /**
         * An instance computing the final summation of the extended precision value exactly.
         */
        public static final ExtendedPrecision EXACT2 = new ExtendedPrecision(ExactSum2.INSTANCE);

        /**
         * Define methods to sum an expansion.
         */
        private interface ExpansionSum {
            /**
             * Sum the expansion of size m. It can be assumed that the size is non-zero.
             *
             * @param e Expansion.
             * @param m Size.
             * @return the sum
             */
            double sum(double[] e, int m);

            /**
             * Sum the expansion; low parts have smaller magnitudes.
             *
             * @param e0 Part 0.
             * @param e1 Part 1.
             * @param e2 Part 2.
             * @param e3 Part 3.
             * @return the sum
             */
            double sum(double e0, double e1, double e2, double e3);

            /**
             * Sum the expansion; low parts have smaller magnitudes.
             *
             * @param e0 Part 0.
             * @param e1 Part 1.
             * @param e2 Part 2.
             * @param e3 Part 3.
             * @param e4 Part 4.
             * @param e5 Part 5.
             * @return the sum
             */
            double sum(double e0, double e1, double e2, double e3, double e4, double e5);

            /**
             * Sum the expansion; low parts have smaller magnitudes.
             *
             * @param e0 Part 0.
             * @param e1 Part 1.
             * @param e2 Part 2.
             * @param e3 Part 3.
             * @param e4 Part 4.
             * @param e5 Part 5.
             * @param e6 Part 6.
             * @param e7 Part 7.
             * @return the sum
             */
            double sum(double e0, double e1, double e2, double e3, double e4, double e5, double e6, double e7);
        }

        /**
         * Shewchuk's APPROXIMATION algorithm sums the parts in order of increasing magnitude.
         * The sum is within 1 ulp of the true expansion value.
         */
        private static final class ApproximationSum implements ExpansionSum {
            /** An instance. */
            static final ApproximationSum INSTANCE = new ApproximationSum();

            /** No construction. */
            private ApproximationSum() {}

            @Override
            public double sum(double[] e, int m) {
                double sum = 0;
                for (int i = 0; i < m; i++) {
                    sum += e[i];
                }
                return sum;
            }

            @Override
            public double sum(double e0, double e1, double e2, double e3) {
                return e0 + e1 + e2 + e3;
            }

            @Override
            public double sum(double e0, double e1, double e2, double e3, double e4, double e5) {
                return e0 + e1 + e2 + e3 + e4 + e5;
            }

            @Override
            public double sum(double e0, double e1, double e2, double e3, double e4, double e5, double e6, double e7) {
                return e0 + e1 + e2 + e3 + e4 + e5 + e6 + e7;
            }
        }

        /**
         * Perform a two-sum through the expansion.
         * The single pass with two-sum ensures that the final term e_m is a good approximation
         * for e: |e - e_m| < ulp(e_m); and the sum of the parts to
         * e_(m-1) is within 1 ULP of the round-off ulp(|e - e_m|).
         */
        private static final class TwoSum implements ExpansionSum {
            /** An instance. */
            static final TwoSum INSTANCE = new TwoSum();

            /** No construction. */
            private TwoSum() {}

            @Override
            public double sum(double[] e, int m) {
                double q = e[0];
                double qq = 0.0;
                for (int i = 1; i < m; i++) {
                    final double p = q + e[i];
                    qq += DoublePrecision.twoSumLow(q, e[i], p);
                    q = p;
                }
                return q + qq;
            }

            @Override
            public double sum(double e0, double e1, double e2, double e3) {
                double q = e0 + e1;
                double qq = DoublePrecision.fastTwoSumLow(e0, e1, q);
                final double p = q + e2;
                qq += DoublePrecision.twoSumLow(q, e2, p);
                q = p + e3;
                qq += DoublePrecision.twoSumLow(p, e3, q);
                return q + qq;
            }

            @Override
            public double sum(double e0, double e1, double e2, double e3, double e4, double e5) {
                double q = e0 + e1;
                double qq = DoublePrecision.fastTwoSumLow(e0, e1, q);
                double p = q + e2;
                qq += DoublePrecision.twoSumLow(q, e2, p);
                q = p + e3;
                qq += DoublePrecision.twoSumLow(p, e3, q);
                p = q + e4;
                qq += DoublePrecision.twoSumLow(q, e4, p);
                q = p + e5;
                qq += DoublePrecision.twoSumLow(p, e5, q);
                return q + qq;
            }

            @Override
            public double sum(double e0, double e1, double e2, double e3, double e4, double e5, double e6, double e7) {
                double q = e0 + e1;
                double qq = DoublePrecision.fastTwoSumLow(e0, e1, q);
                double p = q + e2;
                qq += DoublePrecision.twoSumLow(q, e2, p);
                q = p + e3;
                qq += DoublePrecision.twoSumLow(p, e3, q);
                p = q + e4;
                qq += DoublePrecision.twoSumLow(q, e4, p);
                q = p + e5;
                qq += DoublePrecision.twoSumLow(p, e5, q);
                p = q + e6;
                qq += DoublePrecision.twoSumLow(q, e6, p);
                q = p + e7;
                qq += DoublePrecision.twoSumLow(p, e7, q);
                return q + qq;
            }
        }

        /**
         * Shewchuk's COMPRESS(e) algorithm uses fast-two-sum with a double pass
         * (big->small then small->big) and zero elimination for up to 6m additions (zero
         * elimination reduces the size of the second pass). This ensure e_m is within 1
         * ULP of the expansion value: |e - e_m| < ulp(e_m); and the sum to e_(m-1) is
         * within 1 ULP of the round-off ulp(|e - e_m|). Summation of the expansion should
         * then have low error.
         *
         * <p>Here we modify the second pass to set a sticky bit on the carried sum
         * that allows the rounding of the next addition to compute round-to-nearest,
         * ties-to-even rounding. This simulates the floating point addition of a+b into
         * an extended register where some extra digits are held beyond the precision of
         * the result for use in rounding. The final digit is the sticky bit, a bit wise OR
         * of all the bits from the exact result that are discarded. This works if the value
         * with the sticky bit is added to a value with a higher exponent. The use of the
         * COMPRESS algorithm ensures the next addition is to a number larger in magnitude.
         *
         * <p>Note: The final addition of the sticky carry must be to a non-zero value.
         * Otherwise the sticky bit will be left in the result potentially causing a 1
         * ULP error.
         *
         * <p>Details of the sticky bit can be found in:
         * <blockquote>
         * Coonen, J.T., "An Implementation Guide to a Proposed Standard for Floating Point
         * Arithmetic", Computer, Vol. 13, No. 1, Jan. 1980, pp 68-79.
         * </blockquote>
         */
        private static class ExactSum implements ExpansionSum {
            /** An instance. */
            static final ExactSum INSTANCE = new ExactSum();

            /** Package-private construction to allow class extension. */
            ExactSum() {}

            @Override
            public double sum(double[] e, int size) {
                if (size == 1) {
                    return e[0];
                }

                // First traversal (from big to small)
                // Shewchuk uses (Q,q) for (big,small) from fast-two-sum and carries Q.
                // Here use (q,qq); p is used for intermediates.
                final int m = size - 1;
                double q = e[m];
                int bottom = m;
                for (int i = m - 1; i >= 0; i--) {
                    final double p = q + e[i];
                    final double qq = DoublePrecision.fastTwoSumLow(q, e[i], p);
                    if (qq != 0) {
                        // Store larger component in e and carry the smaller
                        e[bottom] = p;
                        bottom--;
                        q = qq;
                    } else {
                        // Compression.
                        q = p;
                    }
                }
                // Redundant store
                // e[bottom] = q

                if (bottom == m) {
                    // Complete compression to size 1
                    return q;
                }

                // Second traversal (from small to big)
                // Here we do not store the round-off parts back into the expansion
                // but summarise them into the carry using a sticky bit.
                for (int i = bottom + 1; i < m; i++) {
                    q = fastSumWithStickyBit(e[i], q);
                }
                // Final sum. No requirement to compute round-off.
                return e[m] + q;
            }

            @Override
            public double sum(double e0, double e1, double e2, double e3) {
                // compress(e) into g.
                double g3 = 0.0;
                double g2 = 0.0;
                double g1 = 0.0;

                // First traversal (from big to small).
                double q = e3 + e2;
                double qq = DoublePrecision.fastTwoSumLow(e3, e2, q);
                if (qq != 0) {
                    g3 = q;
                    q = qq;
                }
                double p = q + e1;
                qq = DoublePrecision.fastTwoSumLow(q, e1, p);
                if (qq != 0) {
                    g2 = p;
                    p = qq;
                }
                q = p + e0;
                qq = DoublePrecision.fastTwoSumLow(p, e0, q);
                if (qq != 0) {
                    g1 = p;
                    q = qq;
                }
                // g0 = q

                // Second traversal (from small to big)
                // Here we do not store the round-off parts back into the expansion
                // but summarise them into the carry using a sticky bit sum.
                return stickySum(q, g1, g2, g3);
            }

            @Override
            public double sum(double e0, double e1, double e2, double e3, double e4, double e5) {
                // compress(e) into g.
                double g5 = 0.0;
                double g4 = 0.0;
                double g3 = 0.0;
                double g2 = 0.0;
                double g1 = 0.0;

                // First traversal (from big to small).
                double q = e5 + e4;
                double qq = DoublePrecision.fastTwoSumLow(e5, e4, q);
                if (qq != 0) {
                    g5 = q;
                    q = qq;
                }
                double p = q + e3;
                qq = DoublePrecision.fastTwoSumLow(q, e3, p);
                if (qq != 0) {
                    g4 = p;
                    p = qq;
                }
                q = p + e2;
                qq = DoublePrecision.fastTwoSumLow(p, e2, q);
                if (qq != 0) {
                    g3 = q;
                    q = qq;
                }
                p = q + e1;
                qq = DoublePrecision.fastTwoSumLow(q, e1, p);
                if (qq != 0) {
                    g2 = p;
                    p = qq;
                }
                q = p + e0;
                qq = DoublePrecision.fastTwoSumLow(p, e0, q);
                if (qq != 0) {
                    g1 = q;
                    q = qq;
                }
                // g0 = q

                // Second traversal (from small to big)
                // Here we do not store the round-off parts back into the expansion
                // but summarise them into the carry using a sticky bit sum.
                return stickySum(q, g1, g2, g3, g4, g5);
            }

            @Override
            public double sum(double e0, double e1, double e2, double e3, double e4, double e5, double e6, double e7) {
                // compress(e) into g.
                double g7 = 0.0;
                double g6 = 0.0;
                double g5 = 0.0;
                double g4 = 0.0;
                double g3 = 0.0;
                double g2 = 0.0;
                double g1 = 0.0;

                // First traversal (from big to small).
                // If round-off is non-zero it is carried down and the high part deposited in g.
                // If round-off is zero compression has occurred. The high part is carried and
                // gi remains zero. This does not eliminate zeros from g but spurious zeros are
                // left to avoid use of array indexing. These are ignored from the sum operations.
                double q = e7 + e6;
                double qq = DoublePrecision.fastTwoSumLow(e7, e6, q);
                if (qq != 0) {
                    g7 = q;
                    q = qq;
                }
                double p = q + e5;
                qq = DoublePrecision.fastTwoSumLow(q, e5, p);
                if (qq != 0) {
                    g6 = p;
                    p = qq;
                }
                q = p + e4;
                qq = DoublePrecision.fastTwoSumLow(p, e4, q);
                if (qq != 0) {
                    g5 = q;
                    q = qq;
                }
                p = q + e3;
                qq = DoublePrecision.fastTwoSumLow(q, e3, p);
                if (qq != 0) {
                    g4 = p;
                    p = qq;
                }
                q = p + e2;
                qq = DoublePrecision.fastTwoSumLow(p, e2, q);
                if (qq != 0) {
                    g3 = q;
                    q = qq;
                }
                p = q + e1;
                qq = DoublePrecision.fastTwoSumLow(q, e1, p);
                if (qq != 0) {
                    g2 = p;
                    p = qq;
                }
                q = p + e0;
                qq = DoublePrecision.fastTwoSumLow(p, e0, q);
                if (qq != 0) {
                    g1 = q;
                    q = qq;
                }
                // g0 = q

                // Second traversal (from small to big)
                // Here we do not store the round-off parts back into the expansion
                // but summarise them into the carry using a sticky bit sum.
                return stickySum(q, g1, g2, g3, g4, g5, g6, g7);
            }

            /**
             * Compute the summation of the parts using a fast-two-sum. The remainder is
             * carried into the next part using a sticky bit for the final correctly
             * rounded result.
             *
             * @param g0 Part 0
             * @param g1 Part 1
             * @param g2 Part 2
             * @param g3 Part 3
             * @param g4 Part 4
             * @param g5 Part 5
             * @param g6 Part 6
             * @param g7 Part 7
             * @return the sum
             */
            private static double stickySum(double g0, double g1, double g2, double g3,
                                            double g4, double g5, double g6, double g7) {
                if (g7 == 0) {
                    return stickySum(g0, g1, g2, g3, g4, g5, g6);
                }
                double q = fastSumWithStickyBit(g1, g0);
                q = fastSumWithStickyBit(g2, q);
                q = fastSumWithStickyBit(g3, q);
                q = fastSumWithStickyBit(g4, q);
                q = fastSumWithStickyBit(g5, q);
                return g7 + fastSumWithStickyBit(g6, q);
            }

            /**
             * Compute the summation of the parts using a fast-two-sum. The remainder is
             * carried into the next part using a sticky bit for the final correctly
             * rounded result.
             *
             * @param g0 Part 0
             * @param g1 Part 1
             * @param g2 Part 2
             * @param g3 Part 3
             * @param g4 Part 4
             * @param g5 Part 5
             * @param g6 Part 6
             * @return the sum
             */
            private static double stickySum(double g0, double g1, double g2, double g3,
                                            double g4, double g5, double g6) {
                if (g6 == 0) {
                    return stickySum(g0, g1, g2, g3, g4, g5);
                }
                double q = fastSumWithStickyBit(g1, g0);
                q = fastSumWithStickyBit(g2, q);
                q = fastSumWithStickyBit(g3, q);
                q = fastSumWithStickyBit(g4, q);
                return g6 + fastSumWithStickyBit(g5, q);
            }

            /**
             * Compute the summation of the parts using a fast-two-sum. The remainder is
             * carried into the next part using a sticky bit for the final correctly
             * rounded result.
             *
             * @param g0 Part 0
             * @param g1 Part 1
             * @param g2 Part 2
             * @param g3 Part 3
             * @param g4 Part 4
             * @param g5 Part 5
             * @return the sum
             */
            private static double stickySum(double g0, double g1, double g2, double g3,
                                            double g4, double g5) {
                if (g5 == 0) {
                    return stickySum(g0, g1, g2, g3, g4);
                }
                double q = fastSumWithStickyBit(g1, g0);
                q = fastSumWithStickyBit(g2, q);
                q = fastSumWithStickyBit(g3, q);
                return g5 + fastSumWithStickyBit(g4, q);
            }

            /**
             * Compute the summation of the parts using a fast-two-sum. The remainder is
             * carried into the next part using a sticky bit for the final correctly
             * rounded result.
             *
             * @param g0 Part 0
             * @param g1 Part 1
             * @param g2 Part 2
             * @param g3 Part 3
             * @param g4 Part 4
             * @return the sum
             */
            private static double stickySum(double g0, double g1, double g2, double g3,
                                            double g4) {
                if (g4 == 0) {
                    return stickySum(g0, g1, g2, g3);
                }
                double q = fastSumWithStickyBit(g1, g0);
                q = fastSumWithStickyBit(g2, q);
                return g4 + fastSumWithStickyBit(g3, q);
            }

            /**
             * Compute the summation of the parts using a fast-two-sum. The remainder is
             * carried into the next part using a sticky bit for the final correctly
             * rounded result.
             *
             * @param g0 Part 0
             * @param g1 Part 1
             * @param g2 Part 2
             * @param g3 Part 3
             * @return the sum
             */
            private static double stickySum(double g0, double g1, double g2, double g3) {
                if (g3 == 0) {
                    return stickySum(g0, g1, g2);
                }
                final double q = fastSumWithStickyBit(g1, g0);
                return g3 + fastSumWithStickyBit(g2, q);
            }

            /**
             * Compute the summation of the parts using a fast-two-sum. The remainder is
             * carried into the next part using a sticky bit for the final correctly
             * rounded result.
             *
             * @param g0 Part 0
             * @param g1 Part 1
             * @param g2 Part 2
             * @return the sum
             */
            private static double stickySum(double g0, double g1, double g2) {
                if (g2 == 0) {
                    // No sticky bit needed
                    return g1 + g0;
                }
                return g2 + fastSumWithStickyBit(g1, g0);
            }

            /**
             * Compute the sum of two numbers {@code a} and {@code b} using
             * Dekker's two-sum algorithm. The values are required to be ordered by magnitude
             * {@code |a| >= |b|}. The result is adjusted to set the lowest bit as a sticky
             * bit that summarises the magnitude of the round-off that were lost. The
             * result is not the correctly rounded result; it is intended the result is to
             * be used in an addition with a value with a greater magnitude exponent. This
             * addition will have exact round-to-nearest, ties-to-even rounding taking account
             * of bits lots in the previous sum.
             *
             * <p>Details of the sticky bit can be found in:
             * <blockquote>
             * Coonen, J.T., "An Implementation Guide to a Proposed Standard for Floating Point
             * Arithmetic", Computer, Vol. 13, No. 1, Jan. 1980, pp 68-79.
             * </blockquote>
             *
             * @param a First part of sum.
             * @param b Second part of sum.
             * @return <code>b - (sum - a)</code>
             * @see <a href="http://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps">
             * Shewchuk (1997) Theorum 6</a>
             */
            static double fastSumWithStickyBit(double a, double b) {
                double sum = a + b;
                // bVitual = sum - a
                // b - bVirtual == b round-off
                final double r = b - (sum - a);

                if (r != 0) {
                    // Bits will be lost.
                    // In floating-point arithmetic the sticky bit is the bit-wise OR
                    // of the rest of the binary bits that cannot be stored in the
                    // preliminary representation of the result:
                    //
                    // sgn | exp | V | N | .............. | L | G | R | S
                    //
                    // sgn : sign bit
                    // exp : exponent
                    // V : overflow for significand field
                    // N and L : most and least significant bits of the result
                    // G and R : the two bits beyond
                    // S : sticky bit, bitwise OR of all bits thereafter
                    //
                    // The sticky bit is a flag indicating if there is more magnitude beyond
                    // the last bits. Here the round-off is signed so we have to consider the
                    // sign of the sum and round-off together and either add the sticky or
                    // remove it. The final bit is thus used to push up the next addition using
                    // the sum to a higher value, or down to a lower value, when tie breaking for
                    // the correct round-to-nearest, ties-to-even result.
                    long hi = Double.doubleToRawLongBits(sum);
                    // Can only set a sticky bit if the bit is not set.
                    if ((hi & 0x1) == 0) {
                        // In a standard extended precision result for (a+b) the bits are extra
                        // magnitude lost and the sticky bit is positive.
                        // Here the round-off magnitude (r) can be negative so the sticky
                        // bit should be added (same sign) or subtracted (different sign).
                        if (sum > 0) {
                            hi += (r > 0) ? 1 : -1;
                        } else {
                            hi += (r < 0) ? 1 : -1;
                        }
                        sum = Double.longBitsToDouble(hi);
                    }
                }
                return sum;
            }
        }

        /**
         * Shewchuk's COMPRESS(e) algorithm uses fast-two-sum with a double pass
         * (big->small then small->big) and zero elimination. This is a variant
         * of {@link ExactSum}. Methods differ for the inlined versions which use an array
         * to store non-zero values for an efficient second pass summation. This avoids
         * the cascading function calls used in {@link ExactSum}. This variant exists for
         * benchmarking.
         */
        private static final class ExactSum2 extends ExactSum {
            /** An instance. */
            static final ExactSum2 INSTANCE = new ExactSum2();

            /** No construction. */
            private ExactSum2() {}

            @Override
            public double sum(double[] e, int size) {
                if (size == 1) {
                    return e[0];
                }

                // First traversal (from big to small)
                // Shewchuk uses (Q,q) for (big,small) from fast-two-sum and carries Q.
                // Here use (q,qq); p is used for intermediates.
                final int m = size - 1;
                double q = e[m];
                int bottom = m;
                for (int i = m - 1; i >= 0; i--) {
                    final double p = q + e[i];
                    final double qq = DoublePrecision.fastTwoSumLow(q, e[i], p);
                    if (qq != 0) {
                        // Store larger component in e and carry the smaller
                        e[bottom] = p;
                        bottom--;
                        q = qq;
                    } else {
                        // Compression.
                        q = p;
                    }
                }
                // Redundant store
                // e[bottom] = q

                return stickySum(q, e, bottom, m);
            }

            /**
             * Compute the summation of the parts using a fast-two-sum. The remainder is
             * carried into the next part using a sticky bit for the final correctly
             * rounded result.
             *
             * @param q the lowest term
             * @param e the remaining terms from in the range (bottom, m]
             * @param bottom the bottom of the range (exclusive)
             * @param m the upper of the range (inclusive)
             * @return the sum
             */
            private static double stickySum(double q, double[] e, int bottom, int m) {
                if (bottom == m) {
                    // Complete compression to size 1
                    return q;
                }

                // Second traversal (from small to big)
                // Here we do not store the round-off parts back into the expansion
                // but summarise them into the carry using a sticky bit.
                for (int i = bottom + 1; i < m; i++) {
                    q = fastSumWithStickyBit(e[i], q);
                }
                // Final sum. No requirement to compute round-off.
                return e[m] + q;
            }

            @Override
            public double sum(double e0, double e1, double e2, double e3) {
                int bottom = 2;
                final double[] e = new double[3];

                // First traversal (from big to small).
                double q = e3 + e2;
                double qq = DoublePrecision.fastTwoSumLow(e3, e2, q);
                if (qq != 0) {
                    e[bottom--] = q;
                    q = qq;
                }
                double p = q + e1;
                qq = DoublePrecision.fastTwoSumLow(q, e1, p);
                if (qq != 0) {
                    e[bottom--] = p;
                    p = qq;
                }
                q = p + e0;
                qq = DoublePrecision.fastTwoSumLow(p, e0, q);
                if (qq != 0) {
                    e[bottom--] = q;
                    q = qq;
                }

                return stickySum(q, e, bottom, 2);
            }

            @Override
            public double sum(double e0, double e1, double e2, double e3, double e4, double e5) {
                int bottom = 4;
                final double[] e = new double[5];

                // First traversal (from big to small).
                double q = e5 + e4;
                double qq = DoublePrecision.fastTwoSumLow(e5, e4, q);
                if (qq != 0) {
                    e[bottom--] = q;
                    q = qq;
                }
                double p = q + e3;
                qq = DoublePrecision.fastTwoSumLow(q, e3, p);
                if (qq != 0) {
                    e[bottom--] = p;
                    p = qq;
                }
                q = p + e2;
                qq = DoublePrecision.fastTwoSumLow(p, e2, q);
                if (qq != 0) {
                    e[bottom--] = q;
                    q = qq;
                }
                p = q + e1;
                qq = DoublePrecision.fastTwoSumLow(q, e1, p);
                if (qq != 0) {
                    e[bottom--] = p;
                    p = qq;
                }
                q = p + e0;
                qq = DoublePrecision.fastTwoSumLow(p, e0, q);
                if (qq != 0) {
                    e[bottom--] = q;
                    q = qq;
                }

                return stickySum(q, e, bottom, 4);
            }

            @Override
            public double sum(double e0, double e1, double e2, double e3, double e4, double e5, double e6, double e7) {
                int bottom = 6;
                final double[] e = new double[7];

                // First traversal (from big to small).
                // If round-off is non-zero it is carried down and the high part deposited in g.
                // If round-off is zero compression has occurred. The high part is carried and
                // gi remains zero. This does not eliminate zeros from g but spurious zeros are
                // left to avoid use of array indexing. These are ignored from the sum operations.
                double q = e7 + e6;
                double qq = DoublePrecision.fastTwoSumLow(e7, e6, q);
                if (qq != 0) {
                    e[bottom--] = q;
                    q = qq;
                }
                double p = q + e5;
                qq = DoublePrecision.fastTwoSumLow(q, e5, p);
                if (qq != 0) {
                    e[bottom--] = p;
                    p = qq;
                }
                q = p + e4;
                qq = DoublePrecision.fastTwoSumLow(p, e4, q);
                if (qq != 0) {
                    e[bottom--] = q;
                    q = qq;
                }
                p = q + e3;
                qq = DoublePrecision.fastTwoSumLow(q, e3, p);
                if (qq != 0) {
                    e[bottom--] = p;
                    p = qq;
                }
                q = p + e2;
                qq = DoublePrecision.fastTwoSumLow(p, e2, q);
                if (qq != 0) {
                    e[bottom--] = q;
                    q = qq;
                }
                p = q + e1;
                qq = DoublePrecision.fastTwoSumLow(q, e1, p);
                if (qq != 0) {
                    e[bottom--] = p;
                    p = qq;
                }
                q = p + e0;
                qq = DoublePrecision.fastTwoSumLow(p, e0, q);
                if (qq != 0) {
                    e[bottom--] = q;
                    q = qq;
                }

                return stickySum(q, e, bottom, 6);
            }
        }

        /**
         * Define the summation algorithm for the expansion.
         */
        enum Summation {
            /** Standard precision sum of the parts. */
            STANDARD,
            /** Double-length precision sum of the parts. */
            DOUBLE,
            /** Exact sum of the parts. */
            EXACT
        }

        /** The summation algorithm. */
        private final ExpansionSum summation;
        /** The factory to create a working array. */
        private final IntFunction<double[]> arrayFactory;

        /**
         * Private constructor.
         *
         * @param summation The summation algorithm.
         */
        private ExtendedPrecision(ExpansionSum summation) {
            this(summation, double[]::new);
        }

        /**
         * Private constructor.
         *
         * @param summation The summation algorithm.
         * @param arrayFactory The factory to create a working array.
         */
        private ExtendedPrecision(ExpansionSum summation, IntFunction<double[]> arrayFactory) {
            this.summation = summation;
            this.arrayFactory = arrayFactory;
        }

        /**
         * Create an instance with the specified summation algorithm and array factory.
         * Thread-safety is dependent on the array factory.
         *
         * @param type Summation algorithm
         * @param arrayFactory The factory to create a working array.
         * @return the instance
         */
        static ExtendedPrecision of(Summation type, IntFunction<double[]> arrayFactory) {
            switch (type) {
            case DOUBLE:
                return new ExtendedPrecision(TwoSum.INSTANCE);
            case EXACT:
                return new ExtendedPrecision(ExactSum.INSTANCE);
            case STANDARD:
            default:
                return new ExtendedPrecision(ApproximationSum.INSTANCE);
            }
        }

        @Override
        protected double computeValue(double[] a, double[] b) {
            // This method uses an optimised two-product to create an expansion for a1*b1 + a2*b2.
            // This is added to the current sum using an expansion sum.
            // The initial two-product creates the expansion e.
            // This may grow to contain 'length' split numbers.
            // The size is kept smaller using zero elimination.
            final int len = a.length;
            final double[] e = arrayFactory.apply(len * 2);
            int size = sumProduct(a[0], b[0], a[1], b[1], e);

            // Remaining split products added to the current sum.
            // Index i is the second of the pair
            for (int i = 3; i < len; i += 2) {
                // Create the expansion to add inline.
                // Do not re-use sumProduct to limit array read/writes.

                final double a1 = a[i - 1];
                final double b1 = b[i - 1];
                final double a2 = a[i];
                final double b2 = b[i];

                // Expansion e
                double e1 = a1 * b1;
                double e0 = DoublePrecision.productLow(a1, b1, e1);

                // Expansion f
                final double f1 = a2 * b2;
                final double f0 = DoublePrecision.productLow(a2, b2, f1);

                // Inline an expansion sum to avoid sorting e and f into a sequence g.
                // f0 into e
                double q = e0 + f0;
                e0 = DoublePrecision.twoSumLow(e0, f0, q);
                double e2 = e1 + q;
                e1 = DoublePrecision.twoSumLow(e1, q, e2);
                // f1 into e
                q = e1 + f1;
                e1 = DoublePrecision.twoSumLow(e1, f1, q);
                final double e3 = e2 + q;
                e2 = DoublePrecision.twoSumLow(e2, q, e3);

                // Add the round-off parts if non-zero.
                int n = 0;
                if (e0 != 0) {
                    growExpansion(e, size++, n++, e0);
                }
                if (e1 != 0) {
                    growExpansion(e, size++, n++, e1);
                }
                if (e2 != 0) {
                    growExpansion(e, size++, n++, e2);
                }
                // Unlikely that the overall representation of the two-product is zero
                // so no check for non-zero here.
                growExpansion(e, size++, n, e3);

                size = zeroElimination(e, size);
            }
            // Add a trailing final product
            if ((len & 0x1) == 0x1) {
                // Create the expansion f.
                final int i = len - 1;
                final double a1 = a[i];
                final double b1 = b[i];
                final double f1 = a1 * b1;
                final double f0 = DoublePrecision.productLow(a1, b1, f1);
                if (f0 != 0) {
                    growExpansion(e, size++, 0, f0);
                    growExpansion(e, size++, 1, f1);
                } else {
                    growExpansion(e, size++, 0, f1);
                }
                // Ignore zero elimination as the result is now summed.
            }

            // Final summation.
            if (size == 0) {
                return 0.0;
            }

            final double result = summation.sum(e, size);
            if (!Double.isFinite(result)) {
                // Either we have split infinite numbers or some coefficients were NaNs,
                // just rely on the naive implementation and let IEEE754 handle this
                return standardDotProduct(a, b);
            }
            return result;
        }

        @Override
        public double value(double a1, double b1,
                            double a2, double b2) {
            // s is the running scalar product used for a final edge-case check
            double s;

            // Initial product creates the expansion e[0-1]
            double e1 = a1 * b1;
            double e0 = DoublePrecision.productLow(a1, b1, e1);
            s = e1;

            // Second product creates expansion f[0-1]
            final double f1 = a2 * b2;
            final double f0 = DoublePrecision.productLow(a2, b2, f1);
            s += f1;
            // Expansion sum f into e to create e[0-3]
            // f0 into e
            double q = e0 + f0;
            e0 = DoublePrecision.twoSumLow(e0, f0, q);
            double e2 = e1 + q;
            e1 = DoublePrecision.twoSumLow(e1, q, e2);
            // f1 into e
            q = e1 + f1;
            e1 = DoublePrecision.twoSumLow(e1, f1, q);
            final double e3 = e2 + q;
            e2 = DoublePrecision.twoSumLow(e2, q, e3);

            // Final summation
            return getSum(s, summation.sum(e0, e1, e2, e3));
        }

        @Override
        public double value(double a1, double b1,
                            double a2, double b2,
                            double a3, double b3) {
            // s is the running scalar product used for a final edge-case check
            double s;

            // Initial product creates the expansion e[0-1]
            double e1 = a1 * b1;
            double e0 = DoublePrecision.productLow(a1, b1, e1);
            s = e1;

            // Second product creates expansion f[0-1]
            double f1 = a2 * b2;
            double f0 = DoublePrecision.productLow(a2, b2, f1);
            s += f1;
            // Expansion sum f into e to create e[0-3]
            // f0 into e
            double q = e0 + f0;
            e0 = DoublePrecision.twoSumLow(e0, f0, q);
            double e2 = e1 + q;
            e1 = DoublePrecision.twoSumLow(e1, q, e2);
            // f1 into e
            q = e1 + f1;
            e1 = DoublePrecision.twoSumLow(e1, f1, q);
            double e3 = e2 + q;
            e2 = DoublePrecision.twoSumLow(e2, q, e3);

            // Third product creates the expansion f[0-1]
            f1 = a3 * b3;
            f0 = DoublePrecision.productLow(a3, b3, f1);
            s += f1;

            // Expansion sum f into e.
            // f0 into e
            q = e0 + f0;
            e0 = DoublePrecision.twoSumLow(e0, f0, q);
            double p = e1 + q;
            e1 = DoublePrecision.twoSumLow(e1, q, p);
            q = e2 + p;
            e2 = DoublePrecision.twoSumLow(e2, p, q);
            double e4 = e3 + q;
            e3 = DoublePrecision.twoSumLow(e3, q, e4);

            // f1 into e
            q = e1 + f1;
            e1 = DoublePrecision.twoSumLow(e1, f1, q);
            p = e2 + q;
            e2 = DoublePrecision.twoSumLow(e2, q, p);
            q = e3 + p;
            e3 = DoublePrecision.twoSumLow(e3, p, q);
            final double e5 = e4 + q;
            e4 = DoublePrecision.twoSumLow(e4, q, e5);

            // Final summation
            return getSum(s, summation.sum(e0, e1, e2, e3, e4, e5));
        }

        @Override
        public double value(double a1, double b1,
                            double a2, double b2,
                            double a3, double b3,
                            double a4, double b4) {
            // s is the running scalar product used for a final edge-case check
            double s;

            // Initial product creates the expansion e[0-1]
            double e1 = a1 * b1;
            double e0 = DoublePrecision.productLow(a1, b1, e1);
            s = e1;

            // Second product creates expansion f[0-1]
            double f1 = a2 * b2;
            double f0 = DoublePrecision.productLow(a2, b2, f1);
            s += f1;
            // Expansion sum f into e to create e[0-3]
            // f0 into e
            double q = e0 + f0;
            e0 = DoublePrecision.twoSumLow(e0, f0, q);
            double e2 = e1 + q;
            e1 = DoublePrecision.twoSumLow(e1, q, e2);
            // f1 into e
            q = e1 + f1;
            e1 = DoublePrecision.twoSumLow(e1, f1, q);
            double e3 = e2 + q;
            e2 = DoublePrecision.twoSumLow(e2, q, e3);

            // Third product creates the expansion f[0-1]
            f1 = a3 * b3;
            f0 = DoublePrecision.productLow(a3, b3, f1);
            s += f1;

            // Second product creates expansion g[0-1]
            final double g1 = a4 * b4;
            final double g0 = DoublePrecision.productLow(a4, b4, g1);
            s += g1;
            // Expansion sum g into f to create f[0-3]
            // g0 into f
            q = f0 + g0;
            f0 = DoublePrecision.twoSumLow(f0, g0, q);
            double f2 = f1 + q;
            f1 = DoublePrecision.twoSumLow(f1, q, f2);
            // g1 into f
            q = f1 + g1;
            f1 = DoublePrecision.twoSumLow(f1, g1, q);
            final double f3 = f2 + q;
            f2 = DoublePrecision.twoSumLow(f2, q, f3);

            // Expansion sum f into e.
            // f0 into e
            q = e0 + f0;
            e0 = DoublePrecision.twoSumLow(e0, f0, q);
            double p = e1 + q;
            e1 = DoublePrecision.twoSumLow(e1, q, p);
            q = e2 + p;
            e2 = DoublePrecision.twoSumLow(e2, p, q);
            double e4 = e3 + q;
            e3 = DoublePrecision.twoSumLow(e3, q, e4);

            // f1 into e
            q = e1 + f1;
            e1 = DoublePrecision.twoSumLow(e1, f1, q);
            p = e2 + q;
            e2 = DoublePrecision.twoSumLow(e2, q, p);
            q = e3 + p;
            e3 = DoublePrecision.twoSumLow(e3, p, q);
            double e5 = e4 + q;
            e4 = DoublePrecision.twoSumLow(e4, q, e5);

            // f2 into e
            q = e2 + f2;
            e2 = DoublePrecision.twoSumLow(e2, f2, q);
            p = e3 + q;
            e3 = DoublePrecision.twoSumLow(e3, q, p);
            q = e4 + p;
            e4 = DoublePrecision.twoSumLow(e4, p, q);
            double e6 = e5 + q;
            e5 = DoublePrecision.twoSumLow(e5, q, e6);

            // f3 into e
            q = e3 + f3;
            e3 = DoublePrecision.twoSumLow(e3, f3, q);
            p = e4 + q;
            e4 = DoublePrecision.twoSumLow(e4, q, p);
            q = e5 + p;
            e5 = DoublePrecision.twoSumLow(e5, p, q);
            final double e7 = e6 + q;
            e6 = DoublePrecision.twoSumLow(e6, q, e7);

            // Final summation
            return getSum(s, summation.sum(e0, e1, e2, e3, e4, e5, e6, e7));
        }

        /**
         * Compute the sum of the two products and store the result in the expansion.
         * Interspersed zeros are removed and the length returned.
         *
         * @param a1 First factor of the first term.
         * @param b1 Second factor of the first term.
         * @param a2 First factor of the second term.
         * @param b2 Second factor of the second term.
         * @param e Expansion
         * @return the length of the new expansion (can be zero)
         */
        private static int sumProduct(double a1, double b1, double a2, double b2, double[] e) {
            // Expansion e
            double e1 = a1 * b1;
            double e0 = DoublePrecision.productLow(a1, b1, e1);

            // Expansion f
            final double f1 = a2 * b2;
            final double f0 = DoublePrecision.productLow(a2, b2, f1);

            // Inline an expansion sum to avoid sorting e and f into a sequence g.
            // f0 into e
            double q = e0 + f0;
            e0 = DoublePrecision.twoSumLow(e0, f0, q);
            double e2 = e1 + q;
            e1 = DoublePrecision.twoSumLow(e1, q, e2);
            // f1 into e
            q = e1 + f1;
            e1 = DoublePrecision.twoSumLow(e1, f1, q);
            final double e3 = e2 + q;
            e2 = DoublePrecision.twoSumLow(e2, q, e3);

            // Store but remove interspersed zeros
            int ei = 0;
            if (e0 != 0) {
                e[ei++] = e0;
            }
            if (e1 != 0) {
                e[ei++] = e1;
            }
            if (e2 != 0) {
                e[ei++] = e2;
            }
            // Unlikely that the overall representation of the two-product is zero
            // so no check for non-zero here.
            e[ei++] = e3;
            return ei;
        }

        /**
         * Grow the expansion. This maintains the increasing non-overlapping expansion
         * by two-summing the new value through the entire expansion from the given
         * start.
         *
         * @param expansion Expansion.
         * @param length Expansion size.
         * @param start Start point to begin the merge. To be used for optimised
         * expansion sum.
         * @param value Value to add.
         */
        private static void growExpansion(double[] expansion, int length, int start, double value) {
            double p = value;
            for (int i = start; i < length; i++) {
                final double ei = expansion[i];
                final double q = ei + p;
                expansion[i] = DoublePrecision.twoSumLow(ei, p, q);
                // Carry the larger magnitude up to the next iteration.
                p = q;
            }
            expansion[length] = p;
        }

        /**
         * Perform zero elimination on the expansion.
         * The new size can be zero.
         *
         * @param e Expansion.
         * @param size Expansion size.
         * @return the new size
         */
        private static int zeroElimination(double[] e, int size) {
            int newSize = 0;
            // Skip to the first zero
            while (newSize < size && e[newSize] != 0) {
                newSize++;
            }
            if (newSize != size) {
                // Skip the zero and copy remaining non-zeros.
                // This avoids building blocks of non-zeros to bulk-copy using
                // System.arraycopy. This extra complexity may be useful
                // if the number of zeros is small compared to the
                // length of the expansion.
                for (int i = newSize + 1; i < size; i++) {
                    if (e[i] != 0) {
                        e[newSize++] = e[i];
                    }
                }
            }
            return newSize;
        }
    }

    /**
     * Gets the final sum. This checks the high precision sum is finite, otherwise
     * returns the standard precision sum for the IEEE754 result.
     *
     * <p>The high precision sum may be non-finite due to input infinite
     * or NaN numbers or overflow in the summation. However the high precision sum
     * can also be non-finite when the standard sum is finite. This occurs when
     * the split product had a component high-part that overflowed during
     * computation of the hx * hy partial result. In all cases returning the
     * standard sum ensures the IEEE754 result.
     *
     * @param sum Standard sum.
     * @param hpSum High precision sum.
     * @return the sum
     */
    static double getSum(double sum, double hpSum) {
        if (!Double.isFinite(hpSum)) {
            // Either we have split infinite numbers, some coefficients were NaNs,
            // or the split product overflowed.
            // Return the naive implementation for the IEEE754 result.
            return sum;
        }
        return hpSum;
    }
}
