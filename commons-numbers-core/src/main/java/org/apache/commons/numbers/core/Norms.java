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

/** Class providing methods to compute various norm values.
 *
 * <p>This class uses a variety of techniques to increase numerical accuracy
 * and reduce errors. A primary source for the included algorithms is the
 * 2005 paper <a href="https://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.2.1547">
 * Accurate Sum and Dot Product</a> by Takeshi Ogita, Siegfried M. Rump,
 * and Shin'ichi Oishi published in <em>SIAM J. Sci. Comput</em>.
 * @see <a href="https://en.wikipedia.org/wiki/Norm_(mathematics)">Norm</a>
 */
public final class Norms {

    /** Threshold for scaling small numbers. This value is chosen such that doubles
     * set to this value can be squared without underflow. Values less than this must
     * be scaled up.
     */
    private static final double SMALL_THRESH = 0x1.0p-511;

    /** Threshold for scaling large numbers. This value is chosen such that 2^31 doubles
     * set to this value can be squared and added without overflow. Values greater than
     * this must be scaled down.
     */
    private static final double LARGE_THRESH = 0x1.0p+496;

    /** Threshold for scaling up a single value by {@link #SCALE_UP} without risking overflow
     * when the value is squared.
     */
    private static final double SAFE_SCALE_UP_THRESH = 0x1.0p-100;

    /** Value used to scale down large numbers. */
    private static final double SCALE_DOWN = 0x1.0p-600;

    /** Value used to scale up small numbers. */
    private static final double SCALE_UP = 0x1.0p+600;

    /** Threshold for the difference between the exponents of two Euclidean 2D input values
     * where the larger value dominates the calculation.
     */
    private static final int EXP_DIFF_THRESHOLD_2D = 54;

    /** Utility class; no instantiation. */
    private Norms() {}

    /** Compute the Manhattan norm (also known as the Taxicab norm or L1 norm) of the arguments.
     * The result is equal to \(|x| + |y|\), i.e., the sum of the absolute values of the arguments.
     *
     * <p>Special cases:
     * <ul>
     *  <li>If either value is NaN, then the result is NaN.</li>
     *  <li>If either value is infinite and the other value is not NaN, then the result is positive infinity.</li>
     * </ul>
     * @param x first input value
     * @param y second input value
     * @return Manhattan norm
     * @see <a href="https://en.wikipedia.org/wiki/Norm_(mathematics)#Taxicab_norm_or_Manhattan_norm">Manhattan norm</a>
     */
    public static double manhattan(final double x, final double y) {
        return Math.abs(x) + Math.abs(y);
    }

    /** Compute the Manhattan norm (also known as the Taxicab norm or L1 norm) of the arguments.
     * The result is equal to \(|x| + |y| + |z|\), i.e., the sum of the absolute values of the arguments.
     *
     * <p>Special cases:
     * <ul>
     *  <li>If any value is NaN, then the result is NaN.</li>
     *  <li>If any value is infinite and no value is NaN, then the result is positive infinity.</li>
     * </ul>
     * @param x first input value
     * @param y second input value
     * @param z third input value
     * @return Manhattan norm
     * @see <a href="https://en.wikipedia.org/wiki/Norm_(mathematics)#Taxicab_norm_or_Manhattan_norm">Manhattan norm</a>
     */
    public static double manhattan(final double x, final double y, final double z) {
        return Summation.value(
                Math.abs(x),
                Math.abs(y),
                Math.abs(z));
    }

    /** Compute the Manhattan norm (also known as the Taxicab norm or L1 norm) of the given values.
     * The result is equal to \(|v_0| + ... + |v_i|\), i.e., the sum of the absolute values of the input elements.
     *
     * <p>Special cases:
     * <ul>
     *  <li>If any value is NaN, then the result is NaN.</li>
     *  <li>If any value is infinite and no value is NaN, then the result is positive infinity.</li>
     *  <li>If the array is empty, then the result is 0.</li>
     * </ul>
     * @param v input values
     * @return Manhattan norm
     * @see <a href="https://en.wikipedia.org/wiki/Norm_(mathematics)#Taxicab_norm_or_Manhattan_norm">Manhattan norm</a>
     */
    public static double manhattan(final double[] v) {
        double sum = 0d;
        double comp = 0d;

        for (int i = 0; i < v.length; ++i) {
            final double x = Math.abs(v[i]);
            final double sx = sum + x;
            comp += ExtendedPrecision.twoSumLow(sum, x, sx);
            sum = sx;
        }

        return Summation.summationResult(sum, comp);
    }

    /** Compute the Euclidean norm (also known as the L2 norm) of the arguments. The result is equal to
     * \(\sqrt{x^2 + y^2}\). This method correctly handles the possibility of overflow or underflow
     * during the computation.
     *
     * <p>Special cases:
     * <ul>
     *  <li>If either value is NaN, then the result is NaN.</li>
     *  <li>If either value is infinite and the other value is not NaN, then the result is positive infinity.</li>
     * </ul>
     *
     * <p><strong>Comparison with Math.hypot()</strong>
     * <p>While not guaranteed to return the same result, this method does provide similar error bounds to
     * the JDK's Math.hypot() method and may run faster on some JVMs.
     * @param x first input
     * @param y second input
     * @return Euclidean norm
     * @see <a href="https://en.wikipedia.org/wiki/Norm_(mathematics)#Euclidean_norm">Euclidean norm</a>
     */
    public static double euclidean(final double x, final double y) {
        final double xabs = Math.abs(x);
        final double yabs = Math.abs(y);

        final double max;
        final double min;
        // the compare method considers NaN greater than other values, meaning that our
        // check for if the max is finite later on will detect NaNs correctly
        if (Double.compare(xabs, yabs) > 0) {
            max = xabs;
            min = yabs;
        } else {
            max = yabs;
            min = xabs;
        }

        // if the max is not finite, then one of the inputs must not have
        // been finite
        if (!Double.isFinite(max)) {
            // let the standard multiply operation determine whether to return NaN or infinite
            return xabs * yabs;
        } else if (Math.getExponent(max) - Math.getExponent(min) > EXP_DIFF_THRESHOLD_2D) {
            // value is completely dominated by max; just return max
            return max;
        }

        // compute the scale and rescale values
        final double scale;
        final double rescale;
        if (max > LARGE_THRESH) {
            scale = SCALE_DOWN;
            rescale = SCALE_UP;
        } else if (max < SAFE_SCALE_UP_THRESH) {
            scale = SCALE_UP;
            rescale = SCALE_DOWN;
        } else {
            scale = 1d;
            rescale = 1d;
        }

        double sum = 0d;
        double comp = 0d;

        // add scaled x
        double sx = xabs * scale;
        final double px = sx * sx;
        comp += ExtendedPrecision.squareLowUnscaled(sx, px);
        final double sumPx = sum + px;
        comp += ExtendedPrecision.twoSumLow(sum, px, sumPx);
        sum = sumPx;

        // add scaled y
        double sy = yabs * scale;
        final double py = sy * sy;
        comp += ExtendedPrecision.squareLowUnscaled(sy, py);
        final double sumPy = sum + py;
        comp += ExtendedPrecision.twoSumLow(sum, py, sumPy);
        sum = sumPy;

        return Math.sqrt(sum + comp) * rescale;
    }

    /** Compute the Euclidean norm (also known as the L2 norm) of the arguments. The result is equal to
     * \(\sqrt{x^2 + y^2 + z^2}\). This method correctly handles the possibility of overflow or underflow
     * during the computation.
     *
     * <p>Special cases:
     * <ul>
     *  <li>If any value is NaN, then the result is NaN.</li>
     *  <li>If any value is infinite and no value is NaN, then the result is positive infinity.</li>
     * </ul>
     * @param x first input
     * @param y second input
     * @param z third input
     * @return Euclidean norm
     * @see <a href="https://en.wikipedia.org/wiki/Norm_(mathematics)#Euclidean_norm">Euclidean norm</a>
     */
    public static double euclidean(final double x, final double y, final double z) {
        final double xabs = Math.abs(x);
        final double yabs = Math.abs(y);
        final double zabs = Math.abs(z);

        final double max = Math.max(Math.max(xabs, yabs), zabs);

        // if the max is not finite, then one of the inputs must not have
        // been finite
        if (!Double.isFinite(max)) {
            // let the standard multiply operation determine whether to return NaN or infinite
            return xabs * yabs * zabs;
        }

        // compute the scale and rescale values
        final double scale;
        final double rescale;
        if (max > LARGE_THRESH) {
            scale = SCALE_DOWN;
            rescale = SCALE_UP;
        } else if (max < SAFE_SCALE_UP_THRESH) {
            scale = SCALE_UP;
            rescale = SCALE_DOWN;
        } else {
            scale = 1d;
            rescale = 1d;
        }

        double sum = 0d;
        double comp = 0d;

        // add scaled x
        double sx = xabs * scale;
        final double px = sx * sx;
        comp += ExtendedPrecision.squareLowUnscaled(sx, px);
        final double sumPx = sum + px;
        comp += ExtendedPrecision.twoSumLow(sum, px, sumPx);
        sum = sumPx;

        // add scaled y
        double sy = yabs * scale;
        final double py = sy * sy;
        comp += ExtendedPrecision.squareLowUnscaled(sy, py);
        final double sumPy = sum + py;
        comp += ExtendedPrecision.twoSumLow(sum, py, sumPy);
        sum = sumPy;

        // add scaled z
        final double sz = zabs * scale;
        final double pz = sz * sz;
        comp += ExtendedPrecision.squareLowUnscaled(sz, pz);
        final double sumPz = sum + pz;
        comp += ExtendedPrecision.twoSumLow(sum, pz, sumPz);
        sum = sumPz;

        return Math.sqrt(sum + comp) * rescale;
    }

    /** Compute the Euclidean norm (also known as the L2 norm) of the given values. The result is equal to
     * \(\sqrt{v_0^2 + ... + v_{n-1}^2}\). This method correctly handles the possibility of overflow or underflow
     * during the computation.
     *
     * <p>Special cases:
     * <ul>
     *  <li>If any value is NaN, then the result is NaN.</li>
     *  <li>If any value is infinite and no value is NaN, then the result is positive infinity.</li>
     *  <li>If the array is empty, then the result is 0.</li>
     * </ul>
     * @param v input values
     * @return Euclidean norm
     * @see <a href="https://en.wikipedia.org/wiki/Norm_(mathematics)#Euclidean_norm">Euclidean norm</a>
     */
    public static double euclidean(final double[] v) {
        // sum of big, normal and small numbers
        double s1 = 0;
        double s2 = 0;
        double s3 = 0;

        // sum compensation values
        double c1 = 0;
        double c2 = 0;
        double c3 = 0;

        for (int i = 0; i < v.length; ++i) {
            final double x = Math.abs(v[i]);
            if (!Double.isFinite(x)) {
                // not finite; determine whether to return NaN or positive infinity
                return euclideanNormSpecial(v, i);
            } else if (x > LARGE_THRESH) {
                // scale down
                final double sx = x * SCALE_DOWN;

                // compute the product and product compensation
                final double p = sx * sx;
                final double cp = ExtendedPrecision.squareLowUnscaled(sx, p);

                // compute the running sum and sum compensation
                final double s = s1 + p;
                final double cs = ExtendedPrecision.twoSumLow(s1, p, s);

                // update running totals
                c1 += cp + cs;
                s1 = s;
            } else if (x < SMALL_THRESH) {
                // scale up
                final double sx = x * SCALE_UP;

                // compute the product and product compensation
                final double p = sx * sx;
                final double cp = ExtendedPrecision.squareLowUnscaled(sx, p);

                // compute the running sum and sum compensation
                final double s = s3 + p;
                final double cs = ExtendedPrecision.twoSumLow(s3, p, s);

                // update running totals
                c3 += cp + cs;
                s3 = s;
            } else {
                // no scaling
                // compute the product and product compensation
                final double p = x * x;
                final double cp = ExtendedPrecision.squareLowUnscaled(x, p);

                // compute the running sum and sum compensation
                final double s = s2 + p;
                final double cs = ExtendedPrecision.twoSumLow(s2, p, s);

                // update running totals
                c2 += cp + cs;
                s2 = s;
            }
        }

        // The highest sum is the significant component. Add the next significant.
        // Note that the "x * SCALE_DOWN * SCALE_DOWN" expressions must be executed
        // in the order given. If the two scale factors are multiplied together first,
        // they will underflow to zero.
        if (s1 != 0) {
            // add s1, s2, c1, c2
            final double s2Adj = s2 * SCALE_DOWN * SCALE_DOWN;
            final double sum = s1 + s2Adj;
            final double comp = ExtendedPrecision.twoSumLow(s1, s2Adj, sum) + c1 + (c2 * SCALE_DOWN * SCALE_DOWN);
            return Math.sqrt(sum + comp) * SCALE_UP;
        } else if (s2 != 0) {
            // add s2, s3, c2, c3
            final double s3Adj = s3 * SCALE_DOWN * SCALE_DOWN;
            final double sum = s2 + s3Adj;
            final double comp = ExtendedPrecision.twoSumLow(s2, s3Adj, sum) + c2 + (c3 * SCALE_DOWN * SCALE_DOWN);
            return Math.sqrt(sum + comp);
        }
        // add s3, c3
        return Math.sqrt(s3 + c3) * SCALE_DOWN;
    }

    /** Return a Euclidean norm value for special cases of non-finite input.
     * @param v input vector
     * @param start index to start examining the input vector from
     * @return Euclidean norm special value
     */
    private static double euclideanNormSpecial(final double[] v, final int start) {
        for (int i = start; i < v.length; ++i) {
            if (Double.isNaN(v[i])) {
                return Double.NaN;
            }
        }
        return Double.POSITIVE_INFINITY;
    }

    /** Compute the maximum norm (also known as the infinity norm or L<sub>inf</sub> norm) of the arguments.
     * The result is equal to \(\max{(|x|, |y|)}\), i.e., the maximum of the absolute values of the arguments.
     *
     * <p>Special cases:
     * <ul>
     *  <li>If either value is NaN, then the result is NaN.</li>
     *  <li>If either value is infinite and the other value is not NaN, then the result is positive infinity.</li>
     * </ul>
     * @param x first input
     * @param y second input
     * @return maximum norm
     * @see <a href="https://en.wikipedia.org/wiki/Norm_(mathematics)#Maximum_norm_(special_case_of:_infinity_norm,_uniform_norm,_or_supremum_norm)">Maximum norm</a>
     */
    public static double maximum(final double x, final double y) {
        return Math.max(Math.abs(x), Math.abs(y));
    }

    /** Compute the maximum norm (also known as the infinity norm or L<sub>inf</sub> norm) of the arguments.
     * The result is equal to \(\max{(|x|, |y|, |z|)}\), i.e., the maximum of the absolute values of the arguments.
     *
     * <p>Special cases:
     * <ul>
     *  <li>If any value is NaN, then the result is NaN.</li>
     *  <li>If any value is infinite and no value is NaN, then the result is positive infinity.</li>
     * </ul>
     * @param x first input
     * @param y second input
     * @param z third input
     * @return maximum norm
     * @see <a href="https://en.wikipedia.org/wiki/Norm_(mathematics)#Maximum_norm_(special_case_of:_infinity_norm,_uniform_norm,_or_supremum_norm)">Maximum norm</a>
     */
    public static double maximum(final double x, final double y, final double z) {
        return Math.max(
                Math.abs(x),
                Math.max(Math.abs(y), Math.abs(z)));
    }

    /** Compute the maximum norm (also known as the infinity norm or L<sub>inf</sub> norm) of the given values.
     * The result is equal to \(\max{(|v_0|, \ldots, |v_{n-1}|)}\), i.e., the maximum of the absolute values of the
     * input elements.
     *
     * <p>Special cases:
     * <ul>
     *  <li>If any value is NaN, then the result is NaN.</li>
     *  <li>If any value is infinite and no value is NaN, then the result is positive infinity.</li>
     *  <li>If the array is empty, then the result is 0.</li>
     * </ul>
     * @param v input values
     * @return maximum norm
     * @see <a href="https://en.wikipedia.org/wiki/Norm_(mathematics)#Maximum_norm_(special_case_of:_infinity_norm,_uniform_norm,_or_supremum_norm)">Maximum norm</a>
     */
    public static double maximum(final double[] v) {
        double max = 0d;
        for (int i = 0; i < v.length; ++i) {
            max = Math.max(max, Math.abs(v[i]));
        }
        return max;
    }
}
