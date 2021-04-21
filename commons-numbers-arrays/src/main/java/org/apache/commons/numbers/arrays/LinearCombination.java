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
package org.apache.commons.numbers.arrays;

/**
 * Computes linear combinations accurately.
 * This method computes the sum of the products
 * <code>a<sub>i</sub> b<sub>i</sub></code> to high accuracy.
 * It does so by using specific multiplication and addition algorithms to
 * preserve accuracy and reduce cancellation effects.
 *
 * <p>It is based on the 2005 paper
 * <a href="https://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.2.1547">
 * Accurate Sum and Dot Product</a> by Takeshi Ogita, Siegfried M. Rump,
 * and Shin'ichi Oishi published in <em>SIAM J. Sci. Comput</em>.
 */
public final class LinearCombination {
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
     * In this class the sum of the low parts in computed separately from the sum of the
     * high parts for an approximate 2-fold increase in precision in the event of cancellation
     * (sum positives and negatives to a result of much smaller magnitude than the parts).
     * Uses the dot2s algorithm of Ogita to avoid allocation of an array to store intermediates.
     *
     * [1] Shewchuk (1997): Arbitrary Precision Floating-Point Arithmetic
     * http://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps
     */

    /** Private constructor. */
    private LinearCombination() {
        // intentionally empty.
    }

    /**
     * @param a Factors.
     * @param b Factors.
     * @return \( \sum_i a_i b_i \).
     * @throws IllegalArgumentException if the sizes of the arrays are different.
     */
    public static double value(double[] a,
                               double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Dimension mismatch: " + a.length + " != " + b.length);
        }

        // Implement dot2s (Algorithm 5.4) from Ogita et al (2005).
        final int len = a.length;

        // p is the standard scalar product sum.
        // s is the sum of round-off parts.
        double p = a[0] * b[0];
        double s = ExtendedPrecision.productLow(a[0], b[0], p);

        // Remaining split products added to the current sum and round-off sum.
        for (int i = 1; i < len; i++) {
            final double h = a[i] * b[i];
            final double r = ExtendedPrecision.productLow(a[i], b[i], h);

            final double x = p + h;
            // s_i = s_(i-1) + (q_i + r_i)
            s += ExtendedPrecision.twoSumLow(p, h, x) + r;
            p = x;
        }

        return getSum(p, p + s);
    }

    /**
     * @param a1 First factor of the first term.
     * @param b1 Second factor of the first term.
     * @param a2 First factor of the second term.
     * @param b2 Second factor of the second term.
     * @return \( a_1 b_1 + a_2 b_2 \)
     *
     * @see #value(double, double, double, double, double, double)
     * @see #value(double, double, double, double, double, double, double, double)
     * @see #value(double[], double[])
     */
    public static double value(double a1, double b1,
                               double a2, double b2) {
        // p/pn are the standard scalar product old/new sum.
        // s is the sum of round-off parts.
        final double p = a1 * b1;
        double s = ExtendedPrecision.productLow(a1, b1, p);
        final double h = a2 * b2;
        final double r = ExtendedPrecision.productLow(a2, b2, h);
        final double pn = p + h;
        s += ExtendedPrecision.twoSumLow(p, h, pn) + r;

        // Final summation
        return getSum(pn, pn + s);
    }

    /**
     * @param a1 First factor of the first term.
     * @param b1 Second factor of the first term.
     * @param a2 First factor of the second term.
     * @param b2 Second factor of the second term.
     * @param a3 First factor of the third term.
     * @param b3 Second factor of the third term.
     * @return \( a_1 b_1 + a_2 b_2 + a_3 b_3 \)
     *
     * @see #value(double, double, double, double)
     * @see #value(double, double, double, double, double, double, double, double)
     * @see #value(double[], double[])
     */
    public static double value(double a1, double b1,
                               double a2, double b2,
                               double a3, double b3) {
        // p/q are the standard scalar product old/new sum (alternating).
        // s is the sum of round-off parts.
        // pn is the final scalar product sum.
        final double p = a1 * b1;
        double s = ExtendedPrecision.productLow(a1, b1, p);
        double h = a2 * b2;
        double r = ExtendedPrecision.productLow(a2, b2, h);
        final double q = p + h;
        s += r + ExtendedPrecision.twoSumLow(p, h, q);
        h = a3 * b3;
        r = ExtendedPrecision.productLow(a3, b3, h);
        final double pn = q + h;
        s += r + ExtendedPrecision.twoSumLow(q, h, pn);

        // Final summation
        return getSum(pn, pn + s);
    }

    /**
     * @param a1 First factor of the first term.
     * @param b1 Second factor of the first term.
     * @param a2 First factor of the second term.
     * @param b2 Second factor of the second term.
     * @param a3 First factor of the third term.
     * @param b3 Second factor of the third term.
     * @param a4 First factor of the fourth term.
     * @param b4 Second factor of the fourth term.
     * @return \( a_1 b_1 + a_2 b_2 + a_3 b_3 + a_4 b_4 \)
     *
     * @see #value(double, double, double, double)
     * @see #value(double, double, double, double, double, double)
     * @see #value(double[], double[])
     */
    public static double value(double a1, double b1,
                               double a2, double b2,
                               double a3, double b3,
                               double a4, double b4) {
        // p/q are the standard scalar product old/new sum (alternating).
        // s is the sum of round-off parts.
        // pn is the final scalar product sum.
        double p = a1 * b1;
        double s = ExtendedPrecision.productLow(a1, b1, p);
        double h = a2 * b2;
        double r = ExtendedPrecision.productLow(a2, b2, h);
        final double q = p + h;
        s += ExtendedPrecision.twoSumLow(p, h, q) + r;
        h = a3 * b3;
        r = ExtendedPrecision.productLow(a3, b3, h);
        p = q + h;
        s += ExtendedPrecision.twoSumLow(q, h, p) + r;
        h = a4 * b4;
        r = ExtendedPrecision.productLow(a4, b4, h);
        final double pn = p + h;
        s += ExtendedPrecision.twoSumLow(p, h, pn) + r;

        // Final summation
        return getSum(pn, pn + s);
    }

    /**
     * Gets the final sum. This checks the high precision sum is finite, otherwise
     * returns the standard precision sum for the IEEE754 result.
     *
     * <p>The high precision sum may be non-finite due to input infinite
     * or NaN numbers or overflow in the summation. In all cases returning the
     * standard sum ensures the IEEE754 result.
     *
     * @param sum Standard sum.
     * @param hpSum High precision sum.
     * @return the sum
     */
    private static double getSum(double sum, double hpSum) {
        if (!Double.isFinite(hpSum)) {
            // Either we have split infinite numbers, some coefficients were NaNs,
            // or the sum overflowed.
            // Return the naive implementation for the IEEE754 result.
            return sum;
        }
        return hpSum;
    }
}
