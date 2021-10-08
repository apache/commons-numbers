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
package org.apache.commons.numbers.gamma;

import java.text.MessageFormat;
import java.util.function.DoubleUnaryOperator;
import org.apache.commons.numbers.fraction.ContinuedFraction;

/**
 * <a href="http://mathworld.wolfram.com/RegularizedGammaFunction.html">
 * Regularized Gamma functions</a>.
 */
public final class RegularizedGamma {
    /** Maximum allowed numerical error. */
    private static final double DEFAULT_EPSILON = 1e-15;
    /** Maximum allowed iterations. */
    private static final int DEFAULT_ITERATIONS = Integer.MAX_VALUE;

    /** Private constructor. */
    private RegularizedGamma() {
        // intentionally empty.
    }

    /**
     * Encapsulates values for argument {@code a} of the Regularized Gamma functions.
     *
     * <p>Class is immutable.
     */
    private static final class ArgumentA {
        /** Argument a. */
        private final double a;
        /** logGamma(a). */
        private final double logGammaA;

        /**
         * @param a Argument a
         */
        private ArgumentA(double a) {
            this.a = a;
            this.logGammaA = LogGamma.value(a);
        }

        /**
         * Gets the value of the argument.
         *
         * @return a
         */
        double get() {
            return a;
        }

        /**
         * Gets the value for logGamma(a).
         *
         * <p>Note: This method has an argument to allow it to be used as a method reference.
         * It will not compute the log value on the input argument.
         *
         * @param ignore Value to ignore
         * @return logGamma(a)
         */
        double getLogGamma(double ignore) {
            return logGammaA;
        }

        /**
         * Pre-compute values for argument {@code a} of the Regularized Gamma functions.
         *
         * @param a Argument a
         * @return the argument
         * @throws IllegalArgumentException if {@code a <= 0} or is NaN
         */
        static ArgumentA of(double a) {
            if (invalid(a)) {
                throw new IllegalArgumentException("Value is not strictly positive: " + a);
            }
            return new ArgumentA(a);
        }

        /**
         * Check if argument {@code a} is invalid.
         *
         * @param a Argument a
         * @return true if {@code a <= 0} or is NaN
         */
        static boolean invalid(double a) {
            return Double.isNaN(a) || a <= 0;
        }
    }

    /**
     * Encapsulates values for argument {@code x} of the Regularized Gamma functions.
     *
     * <p>Class is immutable.
     */
    private static final class ArgumentX {
        /** Argument x. */
        private final double x;
        /** log(x). */
        private final double logX;

        /**
         * @param x Argument x
         */
        private ArgumentX(double x) {
            this.x = x;
            this.logX = Math.log(x);
        }

        /**
         * Gets the value of the argument.
         *
         * @return x
         */
        double get() {
            return x;
        }

        /**
         * Gets the value for log(x).
         *
         * <p>Note: This method has an argument to allow it to be used as a method reference.
         * It will not compute the log value on the input argument.
         *
         * @param ignore Value to ignore
         * @return log(x)
         */
        double getLog(double ignore) {
            return logX;
        }

        /**
         * Pre-compute values for argument {@code x} of the Regularized Gamma functions.
         *
         * @param x Argument x
         * @return the argument
         * @throws IllegalArgumentException if {@code x < 0} or is NaN
         */
        static ArgumentX of(double x) {
            if (invalid(x)) {
                throw new IllegalArgumentException("Value is not positive: " + x);
            }
            return new ArgumentX(x);
        }

        /**
         * Check if argument {@code x} is invalid.
         *
         * @param x Argument x
         * @return true if {@code x < 0} or is NaN
         */
        static boolean invalid(double x) {
            return Double.isNaN(x) || x < 0;
        }
    }

    /**
     * \( P(a, x) \) <a href="http://mathworld.wolfram.com/RegularizedGammaFunction.html">
     * regularized Gamma function</a>.
     *
     * <p>The implementation of this method is based on:
     * <ul>
     *  <li>
     *   <a href="http://mathworld.wolfram.com/RegularizedGammaFunction.html">
     *   Regularized Gamma Function</a>, equation (1)
     *  </li>
     *  <li>
     *   <a href="http://mathworld.wolfram.com/IncompleteGammaFunction.html">
     *   Incomplete Gamma Function</a>, equation (4).
     *  </li>
     *  <li>
     *   <a href="http://mathworld.wolfram.com/ConfluentHypergeometricFunctionoftheFirstKind.html">
     *   Confluent Hypergeometric Function of the First Kind</a>, equation (1).
     *  </li>
     * </ul>
     */
    public static final class P {
        /** Prevent instantiation. */
        private P() {}

        /**
         * Create a function to computes the regularized gamma function \( P(a, x) \)
         * with argument {@code a} fixed.
         *
         * <p>The returned function will behave as if calling
         * {@link #value(double, double)} with argument {@code a} constant;
         * the error conditions for the function are identical.
         *
         * @param a Argument.
         * @return Function for \( P(a, x) \).
         * @throws IllegalArgumentException if {@code a <= 0} or is NaN
         */
        public static DoubleUnaryOperator withArgumentA(double a) {
            return withArgumentA(a, DEFAULT_EPSILON, DEFAULT_ITERATIONS);
        }

        /**
         * Create a function to computes the regularized gamma function \( P(a, x) \)
         * with argument {@code x} fixed.
         *
         * <p>The returned function will behave as if calling
         * {@link #value(double, double)} with argument {@code x} constant;
         * the error conditions for the function are identical.
         *
         * @param x Argument.
         * @return Function for \( P(a, x) \).
         * @throws IllegalArgumentException if {@code x < 0} or is NaN
         */
        public static DoubleUnaryOperator withArgumentX(double x) {
            return withArgumentX(x, DEFAULT_EPSILON, DEFAULT_ITERATIONS);
        }

        /**
         * Create a function to computes the regularized gamma function \( P(a, x) \)
         * with argument {@code a} fixed.
         *
         * <p>The returned function will behave as if calling
         * {@link #value(double, double, double, int)} with argument {@code a} constant;
         * the error conditions for the function are identical.
         *
         * @param a Argument.
         * @param epsilon Tolerance in continued fraction evaluation.
         * @param maxIterations Maximum number of iterations in continued fraction evaluation.
         * @return Function for \( P(a, x) \).
         * @throws IllegalArgumentException if {@code a <= 0} or is NaN
         */
        public static DoubleUnaryOperator withArgumentA(double a,
                                                        final double epsilon,
                                                        final int maxIterations) {
            final ArgumentA argA = ArgumentA.of(a);
            return x -> value(argA, x, epsilon, maxIterations);
        }

        /**
         * Create a function to computes the regularized gamma function \( P(a, x) \)
         * with argument {@code x} fixed.
         *
         * <p>The returned function will behave as if calling
         * {@link #value(double, double, double, int)} with argument {@code x} constant;
         * the error conditions for the function are identical.
         *
         * @param x Argument.
         * @param epsilon Tolerance in continued fraction evaluation.
         * @param maxIterations Maximum number of iterations in continued fraction evaluation.
         * @return Function for \( P(a, x) \).
         * @throws IllegalArgumentException if {@code x < 0} or is NaN
         */
        public static DoubleUnaryOperator withArgumentX(double x,
                                                        final double epsilon,
                                                        final int maxIterations) {
            final ArgumentX argX = ArgumentX.of(x);
            return a -> value(a, argX, epsilon, maxIterations);
        }

        /**
         * Computes the regularized gamma function \( P(a, x) \).
         *
         * @param a Argument.
         * @param x Argument.
         * @return \( P(a, x) \).
         * @throws ArithmeticException if the continued fraction fails to converge.
         */
        public static double value(double a,
                                   double x) {
            return value(a, x, DEFAULT_EPSILON, DEFAULT_ITERATIONS);
        }

        /**
         * Computes the regularized gamma function \( P(a, x) \).
         *
         * @param a Argument.
         * @param x Argument.
         * @param epsilon Tolerance in continued fraction evaluation.
         * @param maxIterations Maximum number of iterations in continued fraction evaluation.
         * @return \( P(a, x) \).
         * @throws ArithmeticException if the continued fraction fails to converge.
         */
        public static double value(double a,
                                   double x,
                                   double epsilon,
                                   int maxIterations) {
            if (ArgumentA.invalid(a) || ArgumentX.invalid(x)) {
                return Double.NaN;
            }
            return compute(a, x, epsilon, maxIterations, Math::log, LogGamma::value);
        }

        /**
         * Computes the regularized gamma function \( P(a, x) \).
         *
         * <p>This is a specialization of the function \( P(a, x) \) that allows pre-computation
         * of values required for argument \( a \).
         *
         * @param a Argument.
         * @param x Argument.
         * @param epsilon Tolerance in continued fraction evaluation.
         * @param maxIterations Maximum number of iterations in continued fraction evaluation.
         * @return \( P(a, x) \).
         * @throws ArithmeticException if the continued fraction fails to converge.
         * @see ArgumentA
         */
        private static double value(ArgumentA a,
                                    double x,
                                    double epsilon,
                                    int maxIterations) {
            if (ArgumentX.invalid(x)) {
                return Double.NaN;
            }
            return compute(a.get(), x, epsilon, maxIterations, Math::log, a::getLogGamma);
        }

        /**
         * Computes the regularized gamma function \( P(a, x) \).
         *
         * <p>This is a specialization of the function \( P(a, x) \) that allows pre-computation
         * of values required for argument \( x \).
         *
         * @param a Argument.
         * @param x Argument.
         * @param epsilon Tolerance in continued fraction evaluation.
         * @param maxIterations Maximum number of iterations in continued fraction evaluation.
         * @return \( P(a, x) \).
         * @throws ArithmeticException if the continued fraction fails to converge.
         * @see ArgumentX
         */
        private static double value(double a,
                                    ArgumentX x,
                                    double epsilon,
                                    int maxIterations) {
            if (ArgumentA.invalid(a)) {
                return Double.NaN;
            }
            return compute(a, x.get(), epsilon, maxIterations, x::getLog, LogGamma::value);
        }

        /**
         * Computes the regularized gamma function \( P(a, x) \).
         *
         * <p>Note: Assumes argument validation has been performed.
         *
         * @param a Argument ({@code a > 0}).
         * @param x Argument ({@code a >= 0}).
         * @param epsilon Tolerance in continued fraction evaluation.
         * @param maxIterations Maximum number of iterations in continued fraction evaluation.
         * @param fLogX Function to compute the log of x
         * @param fLogGammaA Function to compute logGamma(a)
         * @return \( P(a, x) \).
         * @throws ArithmeticException if the continued fraction fails to converge.
         */
        static double compute(double a,
                              double x,
                              double epsilon,
                              int maxIterations,
                              DoubleUnaryOperator fLogX,
                              DoubleUnaryOperator fLogGammaA) {
            // Assume validation has been performed:
            // a > 0
            // x >= 0
            // NaN is not allowed

            if (x == 0) {
                return 0;
            } else if (x >= a + 1) {
                // Q should converge faster in this case.
                return 1 - RegularizedGamma.Q.compute(a, x, epsilon, maxIterations, fLogX, fLogGammaA);
            } else {
                // Series.
                double n = 0; // current element index
                double an = 1 / a; // n-th element in the series
                double sum = an; // partial sum
                while (Math.abs(an / sum) > epsilon &&
                       n < maxIterations &&
                       sum < Double.POSITIVE_INFINITY) {
                    // compute next element in the series
                    n += 1;
                    an *= x / (a + n);

                    // update partial sum
                    sum += an;
                }
                if (n >= maxIterations) {
                    throw new ArithmeticException(
                            MessageFormat.format("Failed to converge within {0} iterations", maxIterations));
                } else if (Double.isInfinite(sum)) {
                    return 1;
                } else {
                    final double logX = fLogX.applyAsDouble(x);
                    final double logGammaA = fLogGammaA.applyAsDouble(a);
                    final double result = Math.exp(-x + (a * logX) - logGammaA) * sum;
                    // Ensure result is in the range [0, 1]
                    return result > 1.0 ? 1.0 : result;
                }
            }
        }
    }

    /**
     * Creates the \( Q(a, x) \equiv 1 - P(a, x) \) <a href="http://mathworld.wolfram.com/RegularizedGammaFunction.html">
     * regularized Gamma function</a>.
     *
     * <p>The implementation of this method is based on:
     * <ul>
     *  <li>
     *   <a href="http://mathworld.wolfram.com/RegularizedGammaFunction.html">
     *   Regularized Gamma Function</a>, equation (1).
     *  </li>
     *  <li>
     *   <a href="http://functions.wolfram.com/GammaBetaErf/GammaRegularized/10/0003/">
     *   Regularized incomplete gamma function: Continued fraction representations
     *   (formula 06.08.10.0003)</a>
     *  </li>
     * </ul>
     */
    public static final class Q {
        /** Prevent instantiation. */
        private Q() {}

        /**
         * Create a function to computes the regularized gamma function \( Q(a, x) \)
         * with argument {@code a} fixed.
         *
         * <p>The returned function will behave as if calling
         * {@link #value(double, double)} with argument {@code a} constant;
         * the error conditions for the function are identical.
         *
         * @param a Argument.
         * @return Function for \( Q(a, x) \).
         * @throws IllegalArgumentException if {@code a <= 0} or is NaN
         */
        public static DoubleUnaryOperator withArgumentA(double a) {
            return withArgumentA(a, DEFAULT_EPSILON, DEFAULT_ITERATIONS);
        }

        /**
         * Create a function to computes the regularized gamma function \( Q(a, x) \)
         * with argument {@code x} fixed.
         *
         * <p>The returned function will behave as if calling
         * {@link #value(double, double)} with argument {@code x} constant;
         * the error conditions for the function are identical.
         *
         * @param x Argument.
         * @return Function for \( Q(a, x) \).
         * @throws IllegalArgumentException if {@code x < 0} or is NaN
         */
        public static DoubleUnaryOperator withArgumentX(double x) {
            return withArgumentX(x, DEFAULT_EPSILON, DEFAULT_ITERATIONS);
        }

        /**
         * Create a function to computes the regularized gamma function \( Q(a, x) \)
         * with argument {@code a} fixed.
         *
         * <p>The returned function will behave as if calling
         * {@link #value(double, double, double, int)} with argument {@code a} constant;
         * the error conditions for the function are identical.
         *
         * @param a Argument.
         * @param epsilon Tolerance in continued fraction evaluation.
         * @param maxIterations Maximum number of iterations in continued fraction evaluation.
         * @return Function for \( Q(a, x) \).
         * @throws IllegalArgumentException if {@code a <= 0} or is NaN
         */
        public static DoubleUnaryOperator withArgumentA(double a,
                                                        final double epsilon,
                                                        final int maxIterations) {
            final ArgumentA argA = ArgumentA.of(a);
            return x -> value(argA, x, epsilon, maxIterations);
        }

        /**
         * Create a function to computes the regularized gamma function \( Q(a, x) \)
         * with argument {@code x} fixed.
         *
         * <p>The returned function will behave as if calling
         * {@link #value(double, double, double, int)} with argument {@code x} constant;
         * the error conditions for the function are identical.
         *
         * @param x Argument.
         * @param epsilon Tolerance in continued fraction evaluation.
         * @param maxIterations Maximum number of iterations in continued fraction evaluation.
         * @return Function for \( Q(a, x) \).
         * @throws IllegalArgumentException if {@code x < 0} or is NaN
         */
        public static DoubleUnaryOperator withArgumentX(double x,
                                                        final double epsilon,
                                                        final int maxIterations) {
            final ArgumentX argX = ArgumentX.of(x);
            return a -> value(a, argX, epsilon, maxIterations);
        }

        /**
         * Computes the regularized gamma function \( Q(a, x) = 1 - P(a, x) \).
         *
         * @param a Argument.
         * @param x Argument.
         * @return \( Q(a, x) \).
         * @throws ArithmeticException if the continued fraction fails to converge.
         */
        public static double value(double a,
                                   double x) {
            return value(a, x, DEFAULT_EPSILON, DEFAULT_ITERATIONS);
        }

        /**
         * Computes the regularized gamma function \( Q(a, x) = 1 - P(a, x) \).
         *
         * @param a Argument.
         * @param x Argument.
         * @param epsilon Tolerance in continued fraction evaluation.
         * @param maxIterations Maximum number of iterations in continued fraction evaluation.
         * @throws ArithmeticException if the continued fraction fails to converge.
         * @return \( Q(a, x) \).
         */
        public static double value(final double a,
                                   double x,
                                   double epsilon,
                                   int maxIterations) {
            if (ArgumentA.invalid(a) || ArgumentX.invalid(x)) {
                return Double.NaN;
            }
            return compute(a, x, epsilon, maxIterations, Math::log, LogGamma::value);
        }

        /**
         * Computes the regularized gamma function \( Q(a, x) = 1 - P(a, x) \).
         *
         * <p>This is a specialization of the function \( Q(a, x) \) that allows pre-computation
         * of values required for argument \( a \).
         *
         * @param a Argument.
         * @param x Argument.
         * @param epsilon Tolerance in continued fraction evaluation.
         * @param maxIterations Maximum number of iterations in continued fraction evaluation.
         * @return \( Q(a, x) \).
         * @throws ArithmeticException if the continued fraction fails to converge.
         * @see ArgumentA
         */
        private static double value(ArgumentA a,
                                    double x,
                                    double epsilon,
                                    int maxIterations) {
            if (ArgumentX.invalid(x)) {
                return Double.NaN;
            }
            return compute(a.get(), x, epsilon, maxIterations, Math::log, a::getLogGamma);
        }

        /**
         * Computes the regularized gamma function \( Q(a, x) = 1 - P(a, x) \).
         *
         * <p>This is a specialization of the function \( Q(a, x) \) that allows pre-computation
         * of values required for argument \( x \).
         *
         * @param a Argument.
         * @param x Argument.
         * @param epsilon Tolerance in continued fraction evaluation.
         * @param maxIterations Maximum number of iterations in continued fraction evaluation.
         * @return \( Q(a, x) \).
         * @throws ArithmeticException if the continued fraction fails to converge.
         * @see ArgumentX
         */
        private static double value(double a,
                                    ArgumentX x,
                                    double epsilon,
                                    int maxIterations) {
            if (ArgumentA.invalid(a)) {
                return Double.NaN;
            }
            return compute(a, x.get(), epsilon, maxIterations, x::getLog, LogGamma::value);
        }

        /**
         * Computes the regularized gamma function \( Q(a, x) = 1 - P(a, x) \).
         *
         * <p>Note: Assumes argument validation has been performed.
         *
         * @param a Argument ({@code a > 0}).
         * @param x Argument ({@code a >= 0}).
         * @param epsilon Tolerance in continued fraction evaluation.
         * @param fLogX Function to compute the log of x
         * @param fLogGammaA Function to compute logGamma(a)
         * @param maxIterations Maximum number of iterations in continued fraction evaluation.
         * @throws ArithmeticException if the continued fraction fails to converge.
         * @return \( Q(a, x) \).
         */
        static double compute(final double a,
                              double x,
                              double epsilon,
                              int maxIterations,
                              DoubleUnaryOperator fLogX,
                              DoubleUnaryOperator fLogGammaA) {
            // Assume validation has been performed:
            // a > 0
            // x >= 0
            // NaN is not allowed

            if (x == 0) {
                return 1;
            } else if (x < a + 1) {
                // P should converge faster in this case.
                return 1 - RegularizedGamma.P.compute(a, x, epsilon, maxIterations, fLogX, fLogGammaA);
            } else {
                final ContinuedFraction cf = new ContinuedFraction() {
                        /** {@inheritDoc} */
                        @Override
                        protected double getA(int n, double x) {
                            return n * (a - n);
                        }

                        /** {@inheritDoc} */
                        @Override
                        protected double getB(int n, double x) {
                            return ((2 * n) + 1) - a + x;
                        }
                    };

                final double logX = fLogX.applyAsDouble(x);
                final double logGammaA = fLogGammaA.applyAsDouble(a);
                return Math.exp(-x + (a * logX) - logGammaA) /
                    cf.evaluate(x, epsilon, maxIterations);
            }
        }
    }
}
