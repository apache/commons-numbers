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

/** Class providing accurate floating-point summations. The methods provided
 * use a compensated summation technique to reduce numerical errors.
 * The approach is based on the <em>Sum2S</em> algorithm described in the
 * 2005 paper <a href="https://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.2.1547">
 * Accurate Sum and Dot Product</a> by Takeshi Ogita, Siegfried M. Rump,
 * and Shin'ichi Oishi published in <em>SIAM J. Sci. Comput</em>.
 *
 * <p>Method results follow the standard rules for IEEE 754 addition. For example,
 * if any input value is NaN, the result is NaN.
 */
public final class Summation {

    /** Utility class; no instantiation. */
    private Summation() {}

    /** Compute the sum of the input values.
     * @param a first value
     * @param b second value
     * @param c third value
     * @return sum of the input values
     */
    public static double value(final double a, final double b, final double c) {
        double sum = a;
        double comp = 0d;

        final double sb = sum + b;
        comp += ExtendedPrecision.twoSumLow(sum, b, sb);
        sum = sb;

        final double sc = sum + c;
        comp += ExtendedPrecision.twoSumLow(sum, c, sc);
        sum = sc;

        return summationResult(sum, comp);
    }

    /** Compute the sum of the input values.
     * @param a first value
     * @param b second value
     * @param c third value
     * @param d fourth value
     * @return sum of the input values
     */
    public static double value(final double a, final double b, final double c, final double d) {
        double sum = a;
        double comp = 0d;

        final double sb = sum + b;
        comp += ExtendedPrecision.twoSumLow(sum, b, sb);
        sum = sb;

        final double sc = sum + c;
        comp += ExtendedPrecision.twoSumLow(sum, c, sc);
        sum = sc;

        final double sd = sum + d;
        comp += ExtendedPrecision.twoSumLow(sum, d, sd);
        sum = sd;

        return summationResult(sum, comp);
    }

    /** Compute the sum of the input values.
     * @param a first value
     * @param b second value
     * @param c third value
     * @param d fourth value
     * @param e fifth value
     * @return sum of the input values
     */
    public static double value(final double a, final double b, final double c, final double d,
            final double e) {
        double sum = a;
        double comp = 0d;

        final double sb = sum + b;
        comp += ExtendedPrecision.twoSumLow(sum, b, sb);
        sum = sb;

        final double sc = sum + c;
        comp += ExtendedPrecision.twoSumLow(sum, c, sc);
        sum = sc;

        final double sd = sum + d;
        comp += ExtendedPrecision.twoSumLow(sum, d, sd);
        sum = sd;

        final double se = sum + e;
        comp += ExtendedPrecision.twoSumLow(sum, e, se);
        sum = se;

        return summationResult(sum, comp);
    }

    /** Compute the sum of the input values.
     * @param a first value
     * @param b second value
     * @param c third value
     * @param d fourth value
     * @param e fifth value
     * @param f sixth value
     * @return sum of the input values
     */
    public static double value(final double a, final double b, final double c, final double d,
            final double e, final double f) {
        double sum = a;
        double comp = 0d;

        final double sb = sum + b;
        comp += ExtendedPrecision.twoSumLow(sum, b, sb);
        sum = sb;

        final double sc = sum + c;
        comp += ExtendedPrecision.twoSumLow(sum, c, sc);
        sum = sc;

        final double sd = sum + d;
        comp += ExtendedPrecision.twoSumLow(sum, d, sd);
        sum = sd;

        final double se = sum + e;
        comp += ExtendedPrecision.twoSumLow(sum, e, se);
        sum = se;

        final double sf = sum + f;
        comp += ExtendedPrecision.twoSumLow(sum, f, sf);
        sum = sf;

        return summationResult(sum, comp);
    }

    /** Compute the sum of the input values.
     * @param a array containing values to sum
     * @return sum of the input values
     */
    public static double value(final double[] a) {
        double sum = 0d;
        double comp = 0d;

        for (final double x : a) {
            final double s = sum + x;
            comp += ExtendedPrecision.twoSumLow(sum, x, s);
            sum = s;
        }

        return summationResult(sum, comp);
    }

    /** Return the final result from a summation operation.
     * @param sum standard sum value
     * @param comp compensation value
     * @return final summation result
     */
    static double summationResult(final double sum, final double comp) {
        // only add comp if finite; otherwise, return the raw sum
        // to comply with standard double addition rules
        return Double.isFinite(comp) ?
                sum + comp :
                sum;
    }
}
