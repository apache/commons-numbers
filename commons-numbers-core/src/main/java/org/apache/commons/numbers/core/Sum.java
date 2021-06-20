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

/**
 * Class providing accurate floating-point sums and linear combinations.
 *
 * In order to reduce errors, compensated summations and multiplications
 * are performed according to the <em>Sum2S</em> and <em>Dot2S</em>
 * algorithms described in
 * <a href="https://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.2.1547">
 * Accurate Sum and Dot Product</a> by Takeshi Ogita, Siegfried M. Rump,
 * and Shin'ichi Oishi (<em>SIAM J. Sci. Comput</em>, 2005).
 *
 * <p>Results follow the IEEE 754 rules for addition: For example, if any
 * input value is {@link Double#NaN}, the result is {@link Double#NaN}.
 */
public final class Sum
    implements DoubleSupplier,
               DoubleConsumer {
    /** Standard sum. */
    private double sum;
    /** Compensation value. */
    private double comp;

    /**
     * Constructs a new instance with the given initial value.
     *
     * @param initialValue Initial value.
     */
    private Sum(final double initialValue) {
        sum = initialValue;
        comp = 0d;
    }

    /**
     * Adds a single term to this sum.
     *
     * @param t Value to add.
     * @return this instance.
     */
    public Sum add(final double t) {
        final double newSum = sum + t;
        comp += ExtendedPrecision.twoSumLow(sum, t, newSum);
        sum = newSum;

        return this;
    }

    /**
     * Adds values from the given array to the sum.
     *
     * @param terms Terms to add.
     * @return this instance.
     */
    public Sum add(final double... terms) {
        for (double t : terms) {
            add(t);
        }

        return this;
    }

    /**
     * Adds the high-accuracy product \( a b \) to this sum.
     *
     * @param a Factor
     * @param b Factor.
     * @return this instance
     */
    public Sum addProduct(final double a,
                          final double b) {
        final double ab = a * b;
        final double pLow = ExtendedPrecision.productLow(a, b, ab);

        final double newSum = sum + ab;
        comp += ExtendedPrecision.twoSumLow(sum, ab, newSum) + pLow;
        sum = newSum;

        return this;
    }

    /**
     * Adds \( \sum_i a_i b_i \) to this sum.
     *
     * @param a Factors.
     * @param b Factors.
     * @return this instance.
     * @throws IllegalArgumentException if the arrays do not have the same length.
     */
    public Sum addProducts(final double[] a,
                           final double[] b) {
        final int len = a.length;
        if (len != b.length) {
            throw new IllegalArgumentException("Dimension mismatch: " +
                                               a.length + " != " + b.length);
        }

        for (int i = 0; i < len; ++i) {
            addProduct(a[i], b[i]);
        }

        return this;
    }

    /**
     * Adds another sum to this sum.
     *
     * @param other Sum to add.
     * @return this instance.
     */
    public Sum add(final Sum other) {
        // Pull both values first to ensure there are
        // no issues when adding a sum to itself.
        final double s = other.sum;
        final double c = other.comp;

        return add(s).add(c);
    }

    /**
     * Adds a single term to this sum.
     * This is equivalent to {@link #add(double)}.
     *
     * @param value Value to add.
     *
     * @see #add(double)
     */
    @Override
    public void accept(final double value) {
        add(value);
    }

    /**
     * Gets the sum value.
     *
     * @return the sum value.
     */
    @Override
    public double getAsDouble() {
        // High-precision value if it is finite, standard IEEE754 result otherwise.
        final double hpsum = sum + comp;
        return Double.isFinite(hpsum) ?
                hpsum :
                sum;
    }

    /**
     * Creates a new instance with an initial value of zero.
     *
     * @return a new instance.
     */
    public static Sum create() {
        return new Sum(0d);
    }

    /**
     * Creates an instance initialized to the given value.
     *
     * @param a Initial value.
     * @return a new instance.
     */
    public static Sum of(final double a) {
        return new Sum(a);
    }

    /**
     * Creates an instance containing the sum of the given values.
     *
     * @param values Values to add.
     * @return a new instance.
     */
    public static Sum of(final double... values) {
        return create().add(values);
    }

    /**
     * Creates a new instance containing \( \sum_i a_i b_i \).
     *
     * @param a Factors.
     * @param b Factors.
     * @return a new instance.
     */
    public static Sum ofProducts(final double[] a,
                                 final double[] b) {
        return create().addProducts(a, b);
    }
}
