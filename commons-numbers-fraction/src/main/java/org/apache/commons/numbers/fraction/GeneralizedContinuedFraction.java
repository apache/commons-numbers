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

import java.util.function.Supplier;

/**
 * Provides a means to evaluate
 * <a href="https://mathworld.wolfram.com/GeneralizedContinuedFraction.html">generalized continued fractions</a>.
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
 * <p>A generator of the coefficients must be provided to evaluate the continued fraction.
 *
 * <p>The implementation of the fraction evaluation is based on the modified Lentz algorithm
 * as described on page 508 in:
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
 * @see <a href="https://mathworld.wolfram.com/GeneralizedContinuedFraction.html">Wikipedia: Generalized continued fraction</a>
 * @see <a href="https://en.wikipedia.org/wiki/Generalized_continued_fraction">MathWorld: Generalized continued fraction</a>
 * @since 1.1
 */
public final class GeneralizedContinuedFraction {
    /**
     * The value for any number close to zero.
     *
     * <p>"The parameter small should be some non-zero number less than typical values of
     * eps * |b_n|, e.g., 1e-50".
     */
    static final double SMALL = 1e-50;
    /** Default maximum number of iterations. */
    static final int DEFAULT_ITERATIONS = Integer.MAX_VALUE;
    /**
     * Minimum relative error epsilon. Equal to 1 - Math.nextDown(1.0), or 2^-53.
     *
     * <p>The epsilon is used to compare the change in the magnitude of the fraction
     * convergent to 1.0. In theory eps can be 2^-53 reflecting the smallest reduction in
     * magnitude possible i.e. {@code next = previous * Math.nextDown(1.0)}, or zero
     * reflecting exact convergence.
     *
     * <p>If set to zero then the algorithm requires exact convergence which may not be possible
     * due to floating point error in the algorithm. For example the golden ratio will not
     * converge.
     *
     * <p>The minimum value will stop the recursive evaluation at the smallest possible
     * increase or decrease in the convergent.
     */
    private static final double MIN_EPSILON = 0x1.0p-53;
    /** Maximum relative error epsilon. This is configured to prevent incorrect usage. Values
     * higher than 1.0 invalidate the relative error lower bound of {@code (1 - eps) / 1}.
     * Set to 0.5 which is a very weak relative error tolerance. */
    private static final double MAX_EPSILON = 0.5;
    /** Default low threshold for change in magnitude. Precomputed using MIN_EPSILON.
     * Equal to 1 - 2^-53. */
    private static final double DEFAULT_LOW = 1 - MIN_EPSILON;
    /** Default absolute difference threshold for change in magnitude. Precomputed using MIN_EPSILON.
     * Equal to {@code 1 / (1 - 2^-53) = 2^-52}. */
    private static final double DEFAULT_EPS = 0x1.0p-52;

    /**
     * Defines the <a href="https://mathworld.wolfram.com/GeneralizedContinuedFraction.html">
     * {@code n}-th "a" and "b" coefficients</a> of the continued fraction.
     *
     * @since 1.1
     */
    public static final class Coefficient {
        /** "a" coefficient. */
        private final double a;
        /** "b" coefficient. */
        private final double b;

        /**
         * @param a "a" coefficient
         * @param b "b" coefficient
         */
        private Coefficient(double a, double b) {
            this.a = a;
            this.b = b;
        }

        /**
         * Returns the {@code n}-th "a" coefficient of the continued fraction.
         *
         * @return the coefficient <code>a<sub>n</sub></code>.
         */
        public double getA() {
            return a;
        }

        /**
         * Returns the {@code n}-th "b" coefficient of the continued fraction.
         *
         * @return the coefficient <code>b<sub>n</sub></code>.
         */
        public double getB() {
            return b;
        }

        /**
         * Create a new coefficient.
         *
         * @param a "a" coefficient
         * @param b "b" coefficient
         * @return the coefficient
         */
        public static Coefficient of(double a, double b) {
            return new Coefficient(a, b);
        }
    }

    /** No instances. */
    private GeneralizedContinuedFraction() {}

    /**
     * Evaluates the continued fraction.
     *
     * <p>Note: The first generated partial numerator a<sub>0</sub> is discarded.
     *
     * @param gen Generator of coefficients.
     * @return the value of the continued fraction.
     * @throws ArithmeticException if the algorithm fails to converge or if the maximal number of
     * iterations is reached before the expected convergence is achieved.
     * @see #value(Supplier,double,int)
     */
    public static double value(Supplier<Coefficient> gen) {
        return value(gen, MIN_EPSILON, DEFAULT_ITERATIONS);
    }

    /**
     * Evaluates the continued fraction.
     *
     * <p>Note: The first generated partial numerator a<sub>0</sub> is discarded.
     *
     * @param gen Generator of coefficients.
     * @param epsilon Maximum relative error allowed.
     * @return the value of the continued fraction.
     * @throws ArithmeticException if the algorithm fails to converge or if the maximal number of
     * iterations is reached before the expected convergence is achieved.
     * @see #value(Supplier,double,int)
     */
    public static double value(Supplier<Coefficient> gen, double epsilon) {
        return value(gen, epsilon, DEFAULT_ITERATIONS);
    }

    /**
     * Evaluates the continued fraction.
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
     * <p>Setting coefficient a<sub>n</sub> to zero will signal the end of the recursive evaluation.
     *
     * <p>Note: The first generated partial numerator a<sub>0</sub> is discarded.
     *
     * <p><b>Usage Note</b>
     *
     * <p>This method is not functionally identical to calling
     * {@link #value(double, Supplier, double, int)} with the generator configured to
     * provide coefficients from n=1 and supplying b<sub>0</sub> separately. In some cases
     * the computed result from the two variations may be different by more than the
     * provided epsilon. The other method should be used if b<sub>0</sub> is zero or very
     * small. See the corresponding javadoc for details.
     *
     * @param gen Generator of coefficients.
     * @param epsilon Maximum relative error allowed.
     * @param maxIterations Maximum number of iterations.
     * @return the value of the continued fraction.
     * @throws ArithmeticException if the algorithm fails to converge or if the maximal number of
     * iterations is reached before the expected convergence is achieved.
     * @see #value(double, Supplier, double, int)
     */
    public static double value(Supplier<Coefficient> gen, double epsilon, int maxIterations) {
        // Use the first b coefficient to seed the evaluation of the fraction.
        // Coefficient a is discarded.
        final Coefficient c = gen.get();
        return evaluate(c.getB(), gen, epsilon, maxIterations);
    }

    /**
     * Evaluates the continued fraction.
     *
     * <p>Note: The initial term b<sub>0</sub> is supplied as an argument.
     * Both of the first generated terms a and b are used. This fraction evaluation
     * can be used when:
     * <ul>
     *  <li>b<sub>0</sub> is not part of a regular series
     *  <li>b<sub>0</sub> is zero and the result will evaluate only the continued fraction component
     *  <li>b<sub>0</sub> is very small and the result is expected to approach zero
     * </ul>
     *
     * @param b0 Coefficient b<sub>0</sub>.
     * @param gen Generator of coefficients.
     * @return the value of the continued fraction.
     * @throws ArithmeticException if the algorithm fails to converge or if the maximal number
     * of iterations is reached before the expected convergence is achieved.
     * @see #value(double,Supplier,double,int)
     */
    public static double value(double b0, Supplier<Coefficient> gen) {
        return value(b0, gen, MIN_EPSILON, DEFAULT_ITERATIONS);
    }

    /**
     * Evaluates the continued fraction.
     *
     * <p>Note: The initial term b<sub>0</sub> is supplied as an argument.
     * Both of the first generated terms a and b are used. This fraction evaluation
     * can be used when:
     * <ul>
     *  <li>b<sub>0</sub> is not part of a regular series
     *  <li>b<sub>0</sub> is zero and the result will evaluate only the continued fraction component
     *  <li>b<sub>0</sub> is very small and the result is expected to approach zero
     * </ul>
     *
     * @param b0 Coefficient b<sub>0</sub>.
     * @param gen Generator of coefficients.
     * @param epsilon Maximum relative error allowed.
     * @return the value of the continued fraction.
     * @throws ArithmeticException if the algorithm fails to converge or if the maximal number
     * of iterations is reached before the expected convergence is achieved.
     * @see #value(double,Supplier,double,int)
     */
    public static double value(double b0, Supplier<Coefficient> gen, double epsilon) {
        return value(b0, gen, epsilon, DEFAULT_ITERATIONS);
    }

    /**
     * Evaluates the continued fraction.
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
     * <p>Setting coefficient a<sub>n</sub> to zero will signal the end of the recursive evaluation.
     *
     * <p>Note: The initial term b<sub>0</sub> is supplied as an argument.
     * Both of the first generated terms a and b are used. This fraction evaluation
     * can be used when:
     * <ul>
     *  <li>b<sub>0</sub> is not part of a regular series
     *  <li>b<sub>0</sub> is zero and the result will evaluate only the continued fraction component
     *  <li>b<sub>0</sub> is very small and the result is expected to approach zero
     * </ul>
     *
     * <p><b>Usage Note</b>
     *
     * <p>This method is not functionally identical to calling
     * {@link #value(Supplier, double, int)} with the generator configured to provide term
     * "b<sub>0</sub>" in the first coefficient. In some cases the computed result from
     * the two variations may be different by more than the provided epsilon. The
     * convergence of the continued fraction algorithm relies on computing an update
     * multiplier applied to the current value. Convergence is faster if the initial value
     * is close to the final value. The {@link #value(Supplier, double, int)} method will
     * initialise the current value using b<sub>0</sub> and evaluate the continued
     * fraction using updates computed from the generated coefficients. This method
     * initialises the algorithm using b1 to evaluate part of the continued fraction and
     * computes the result as:
     *
     * <pre>
     *        a1
     * b0 + ------
     *       part
     * </pre>
     *
     * <p>This is preferred if b<sub>0</sub> is smaller in magnitude than the continued
     * fraction component. In particular the evaluation algorithm sets a bound on the
     * minimum initial value as {@code 1e-50}. If b<sub>0</sub> is smaller than this value
     * then using this method is the preferred evaluation.
     *
     * @param b0 Coefficient b<sub>0</sub>.
     * @param gen Generator of coefficients.
     * @param epsilon Maximum relative error allowed.
     * @param maxIterations Maximum number of iterations.
     * @return the value of the continued fraction.
     * @throws ArithmeticException if the algorithm fails to converge or if the maximal number
     * of iterations is reached before the expected convergence is achieved.
     * @see #value(Supplier,double,int)
     */
    public static double value(double b0, Supplier<Coefficient> gen, double epsilon, int maxIterations) {
        // Use the first b coefficient to seed the evaluation of the fraction.
        // Coefficient a is used to compute the final result as the numerator term a1.
        // The supplied b0 is added to the result.
        final Coefficient c = gen.get();
        return b0 + c.getA() / evaluate(c.getB(), gen, epsilon, maxIterations);
    }

    /**
     * Evaluates the continued fraction using the modified Lentz algorithm described in
     * Thompson and Barnett (1986) Journal of Computational Physics 64, 490-509.
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
     * <p>Note: The initial term b<sub>0</sub> is supplied as an argument.
     * Both of the first generated terms a and b are used.
     *
     * <p><b>Implementation Note</b>
     *
     * <p>This method is private and functionally different from
     * {@link #value(double, Supplier, double, int)}. The convergence of the algorithm relies on
     * computing an update multiplier applied to the current value, initialised as b0. Accuracy
     * of the evaluation can be effected if the magnitude of b0 is very different from later
     * terms. In particular if initialised as 0 the algorithm will not function and so must
     * set b0 to a small non-zero number. The public methods with the leading b0 term
     * provide evaluation of the fraction if the term b0 is zero.
     *
     * @param b0 Coefficient b<sub>0</sub>.
     * @param gen Generator of coefficients.
     * @param epsilon Maximum relative error allowed.
     * @param maxIterations Maximum number of iterations.
     * @return the value of the continued fraction.
     * @throws ArithmeticException if the algorithm fails to converge or if the maximal number
     * of iterations is reached before the expected convergence is achieved.
     */
    static double evaluate(double b0, Supplier<Coefficient> gen, double epsilon, int maxIterations) {
        // Relative error epsilon should not be zero to prevent drift in the event
        // that the update ratio never achieves 1.0.

        // Epsilon is the relative change allowed from 1. Configure the absolute limits so
        // convergence requires: low <= deltaN <= high
        // low = 1 - eps
        // high = 1 / (1 - eps)
        // High is always further from 1 than low in absolute distance. Do not store high
        // but store the maximum absolute deviation from 1 for convergence = high - 1.
        // If this is achieved a second check is made against low.
        double low;
        double eps;
        if (epsilon > MIN_EPSILON && epsilon <= MAX_EPSILON) {
            low = 1 - epsilon;
            eps = 1 / low - 1;
        } else {
            // Precomputed defaults. Used when epsilon <= MIN_EPSILON
            low = DEFAULT_LOW;
            eps = DEFAULT_EPS;
        }

        double hPrev = updateIfCloseToZero(b0);

        // Notes from Thompson and Barnett:
        //
        // Fraction convergent: hn = An / Bn
        // A(-1) = 1, A0 = b0, B(-1) = 0, B0 = 1

        // Compute the ratios:
        // Dn = B(n-1) / Bn  = 1 / (an * D(n-1) + bn)
        // Cn = An / A(n-1)  = an / C(n-1) + bn
        //
        // Ratio of successive convergents:
        // delta n = hn / h(n-1)
        //         = Cn / Dn

        // Avoid divisors being zero (less than machine precision) by shifting them to e.g. 1e-50.

        double dPrev = 0.0;
        double cPrev = hPrev;

        for (int n = maxIterations; n > 0; n--) {
            final Coefficient c = gen.get();
            final double a = c.getA();
            final double b = c.getB();

            double dN = updateIfCloseToZero(b + a * dPrev);
            final double cN = updateIfCloseToZero(b + a / cPrev);

            dN = 1 / dN;
            final double deltaN = cN * dN;
            final double hN = hPrev * deltaN;

            // If the fraction is convergent then deltaN -> 1.
            // Computation of deltaN = 0 or deltaN = big will result in zero or overflow.
            // Directly check for overflow on hN (this ensures the result is finite).

            if (!Double.isFinite(hN)) {
                throw new FractionException("Continued fraction diverged to " + hN);
            }

            // Check for underflow on deltaN. This allows fractions to compute zero
            // if this is the convergent limit.
            // Note: deltaN is only zero if dN > 1e-50 / min_value, or 2.02e273.
            // Since dN is the ratio of convergent denominators this magnitude of
            // ratio is a presumed to be an error.
            if (deltaN == 0) {
                throw new FractionException("Ratio of successive convergents is zero");
            }

            // Update from Thompson and Barnett to use <= eps in place of < eps.
            // eps = high - 1
            // A second check is made to ensure:
            // low <= deltaN <= high
            if (Math.abs(deltaN - 1) <= eps && deltaN >= low) {
                return hN;
            }

            dPrev = dN;
            cPrev = cN;
            hPrev = hN;
        }

        throw new FractionException("Maximum iterations (%d) exceeded", maxIterations);
    }

    /**
     * Returns the value, or if close to zero returns a small epsilon of the same sign.
     *
     * <p>This method is used in Thompson &amp; Barnett to monitor both the numerator and denominator
     * ratios for approaches to zero.
     *
     * @param value the value
     * @return the value (or small epsilon)
     */
    private static double updateIfCloseToZero(double value) {
        return Math.abs(value) < SMALL ? Math.copySign(SMALL, value) : value;
    }
}
