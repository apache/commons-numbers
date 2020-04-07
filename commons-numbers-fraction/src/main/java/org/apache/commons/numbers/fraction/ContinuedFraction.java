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
package org.apache.commons.numbers.fraction;

import org.apache.commons.numbers.core.Precision;

/**
 * Provides a generic means to evaluate
 * <a href="https://mathworld.wolfram.com/ContinuedFraction.html">continued fractions</a>.
 *
 * <p>The continued fraction uses the following form for the numerator ({@code a}) and
 * denominator ({@code b}) coefficients:
 * <pre>
 *              a1
 * b0 + ------------------
 *      b1 +      a2
 *           -------------
 *           b2 +    a3
 *                --------
 *                b3 + ...
 * </pre>
 *
 * <p>Subclasses must provide the {@link #getA(int,double) a} and {@link #getB(int,double) b}
 * coefficients to evaluate the continued fraction.
 */
public abstract class ContinuedFraction {
    /**
     * The value for any number close to zero.
     *
     * <p>"The parameter small should be some non-zero number less than typical values of
     * eps * |b_n|, e.g., 1e-50".
     */
    private static final double SMALL = 1e-50;

    /**
     * Defines the <a href="https://mathworld.wolfram.com/ContinuedFraction.html">
     * {@code n}-th "a" coefficient</a> of the continued fraction.
     *
     * @param n Index of the coefficient to retrieve.
     * @param x Evaluation point.
     * @return the coefficient <code>a<sub>n</sub></code>.
     */
    protected abstract double getA(int n, double x);

    /**
     * Defines the <a href="https://mathworld.wolfram.com/ContinuedFraction.html">
     * {@code n}-th "b" coefficient</a> of the continued fraction.
     *
     * @param n Index of the coefficient to retrieve.
     * @param x Evaluation point.
     * @return the coefficient <code>b<sub>n</sub></code>.
     */
    protected abstract double getB(int n, double x);

    /**
     * Evaluates the continued fraction.
     *
     * @param x the evaluation point.
     * @param epsilon Maximum error allowed.
     * @return the value of the continued fraction evaluated at {@code x}.
     * @throws ArithmeticException if the algorithm fails to converge.
     * @throws ArithmeticException if the maximal number of iterations is reached
     * before the expected convergence is achieved.
     *
     * @see #evaluate(double,double,int)
     */
    public double evaluate(double x, double epsilon) {
        return evaluate(x, epsilon, Integer.MAX_VALUE);
    }

    /**
     * Evaluates the continued fraction.
     * <p>
     * The implementation of this method is based on the modified Lentz algorithm as described
     * on page 508 in:
     * </p>
     *
     * <ul>
     *   <li>
     *   I. J. Thompson,  A. R. Barnett (1986).
     *   "Coulomb and Bessel Functions of Complex Arguments and Order."
     *   Journal of Computational Physics 64, 490-509.
     *   <a target="_blank" href="https://www.fresco.org.uk/papers/Thompson-JCP64p490.pdf">
     *   https://www.fresco.org.uk/papers/Thompson-JCP64p490.pdf</a>
     *   </li>
     * </ul>
     *
     * @param x Point at which to evaluate the continued fraction.
     * @param epsilon Maximum error allowed.
     * @param maxIterations Maximum number of iterations.
     * @return the value of the continued fraction evaluated at {@code x}.
     * @throws ArithmeticException if the algorithm fails to converge.
     * @throws ArithmeticException if the maximal number of iterations is reached
     * before the expected convergence is achieved.
     */
    public double evaluate(double x, double epsilon, int maxIterations) {
        double hPrev = updateIfCloseToZero(getB(0, x));

        int n = 1;
        double dPrev = 0.0;
        double cPrev = hPrev;
        double hN;

        while (n <= maxIterations) {
            final double a = getA(n, x);
            final double b = getB(n, x);

            double dN = updateIfCloseToZero(b + a * dPrev);
            final double cN = updateIfCloseToZero(b + a / cPrev);

            dN = 1 / dN;
            final double deltaN = cN * dN;
            hN = hPrev * deltaN;

            if (Double.isInfinite(hN)) {
                throw new FractionException(
                    "Continued fraction convergents diverged to +/- infinity for value {0}", x);
            }
            if (Double.isNaN(hN)) {
                throw new FractionException(
                    "Continued fraction diverged to NaN for value {0}", x);
            }

            if (Math.abs(deltaN - 1) < epsilon) {
                return hN;
            }

            dPrev = dN;
            cPrev = cN;
            hPrev = hN;
            ++n;
        }

        throw new FractionException("maximal count ({0}) exceeded", maxIterations);
    }

    /**
     * Returns the value, or if close to zero returns a small epsilon.
     *
     * <p>This method is used in Thompson & Barnett to monitor both the numerator and denominator
     * ratios for approaches to zero.
     *
     * @param value the value
     * @return the value (or small epsilon)
     */
    private static double updateIfCloseToZero(double value) {
        return Precision.equals(value, 0.0, SMALL) ? SMALL : value;
    }
}
