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

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/** Class providing accurate floating-point sums and linear combinations. The methods
 * provided use compensated summation and multiplication techniques to reduce numerical errors.
 * The approach is based on the <em>Sum2S</em> and <em>Dot2S</em> algorithms described in the
 * 2005 paper <a href="https://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.2.1547">
 * Accurate Sum and Dot Product</a> by Takeshi Ogita, Siegfried M. Rump,
 * and Shin'ichi Oishi published in <em>SIAM J. Sci. Comput</em>.
 *
 * <p>Method results follow the standard rules for IEEE 754 addition. For example,
 * if any input value is NaN, the result is NaN.
 */
public final class Sum implements DoubleSupplier, DoubleConsumer {

    /** Standard sum. */
    private double sum;

    /** Compensation value. */
    private double comp;

    /** Construct a new instance with an initial value of zero.
     */
    private Sum() {
        this(0d);
    }

    /** Construct a new instance with the given initial value.
     * @param initialValue initial value
     */
    private Sum(final double initialValue) {
        this.sum = initialValue;
    }

    /** Add a single term to this sum.
     * @param t value to add
     * @return this instance
     */
    public Sum add(final double t) {
        final double newSum = sum + t;
        comp += ExtendedPrecision.twoSumLow(sum, t, newSum);
        sum = newSum;

        return this;
    }

    /** Add an array of value to the sum.
     * @param terms terms to add
     * @return this instance
     */
    public Sum add(final double[] terms) {
        for (double t : terms) {
            add(t);
        }

        return this;
    }

    /** Add the product \(a b\) to this sum.
     * @param a first factor
     * @param b second factor
     * @return this instance
     */
    public Sum addProduct(final double a, final double b) {
        final double ab = a * b;
        final double pLow = ExtendedPrecision.productLow(a, b, ab);

        final double newSum = sum + ab;
        comp += ExtendedPrecision.twoSumLow(sum, ab, newSum) + pLow;
        sum = newSum;

        return this;
    }

    /** Add \( \sum_i a_i b_i \). In other words, multiply each element
     * in {@code a} with its corresponding element in {@code b} and add the product
     * to the sum.
     * @param a factors
     * @param b factors
     * @return this instance
     * @throws IllegalArgumentException if the arrays do not have the same length
     */
    public Sum addProducts(final double[] a, final double[] b) {
        final int len = a.length;
        if (len != b.length) {
            throw new IllegalArgumentException("Dimension mismatch: " + a.length + " != " + b.length);
        }

        for (int i = 0; i < len; ++i) {
            addProduct(a[i], b[i]);
        }

        return this;
    }

    /** Add another sum to this sum.
     * @param other sum to add
     * @return this instance
     */
    public Sum add(final Sum other) {
        // pull both values first in order to support
        // adding a sum to itself
        final double s = other.sum;
        final double c = other.comp;

        return add(s)
                .add(c);
    }

    /** Add a single term to this sum. This is equivalent to {@link #add(double)}.
     * @param value value to add
     * @see #add(double)
     */
    @Override
    public void accept(final double value) {
        add(value);
    }

    /** Get the sum value.
     * @return sum value as a double
     */
    @Override
    public double getAsDouble() {
        // compute and return the high precision sum if it is finite; otherwise,
        // return the standard IEEE754 result
        final double hpsum = sum + comp;
        return Double.isFinite(hpsum) ?
                hpsum :
                sum;
    }

    /** Create a new sum instance with an initial value of zero.
     * @return new sum instance
     */
    public static Sum create() {
        return new Sum();
    }

    /** Return a new sum instance containing a single value.
     * @param a value
     * @return new sum instance
     * @see #add(double)
     */
    public static Sum of(final double a) {
        return new Sum(a);
    }

    /** Return a new sum instance containing the value \(a + b\).
     * @param a first term
     * @param b second term
     * @return new sum instance
     * @see #add(double)
     */
    public static Sum of(final double a, final double b) {
        return new Sum(a).add(b);
    }

    /** Return a new sum instance containing the value \(a + b + c\).
     * @param a first term
     * @param b second term
     * @param c third term
     * @return new sum instance
     * @see #add(double)
     */
    public static Sum of(final double a, final double b, final double c) {
        return new Sum(a)
                .add(b)
                .add(c);
    }

    /** Return a new sum instance containing the value \(a + b + c + d\).
     * @param a first term
     * @param b second term
     * @param c third term
     * @param d fourth term
     * @return new sum instance
     * @see #add(double)
     */
    public static Sum of(final double a, final double b, final double c, final double d) {
        return new Sum(a)
                .add(b)
                .add(c)
                .add(d);
    }

    /** Return a new sum instance containing the sum of the given values.
     * @param values input values
     * @return new sum instance
     * @see #add(double[])
     */
    public static Sum of(final double[] values) {
        return new Sum().add(values);
    }

    /** Return a new sum instance containing the linear combination
     * \(a_1 b_1 + a_2 b_2\).
     * @param a1 first factor of first term
     * @param b1 second factor of first term
     * @param a2 first factor of second term
     * @param b2 second factor of second term
     * @return new sum instance
     * @see #addProduct(double, double)
     */
    public static Sum ofProducts(final double a1, final double b1,
                                 final double a2, final double b2) {
        return new Sum()
                .addProduct(a1, b1)
                .addProduct(a2, b2);
    }

    /** Return a new sum instance containing the linear combination
     * \(a_1 b_1 + a_2 b_2 + a_3 b_3\).
     * @param a1 first factor of first term
     * @param b1 second factor of first term
     * @param a2 first factor of second term
     * @param b2 second factor of second term
     * @param a3 first factor of third term
     * @param b3 second factor of third term
     * @return new sum instance
     * @see #addProduct(double, double)
     */
    public static Sum ofProducts(final double a1, final double b1,
                                 final double a2, final double b2,
                                 final double a3, final double b3) {
        return new Sum()
                .addProduct(a1, b1)
                .addProduct(a2, b2)
                .addProduct(a3, b3);
    }

    /** Return a new sum instance containing \( \sum_i a_i b_i \).
     * @param a first set of factors
     * @param b second set of factors
     * @return new sum instance
     * @see #addProducts(double[], double[])
     */
    public static Sum ofProducts(final double[] a, final double[] b) {
        return new Sum().addProducts(a, b);
    }
}
