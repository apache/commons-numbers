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

/**
 * <a href="https://mathworld.wolfram.com/RegularizedGammaFunction.html">
 * Regularized Gamma functions</a>.
 *
 * <p>By definition, the lower and upper regularized gamma functions satisfy:
 *
 * <p>\[ 1 = P(a, x) + Q(a, x) \]
 *
 * <p>This code has been adapted from the <a href="https://www.boost.org/">Boost</a>
 * {@code c++} implementation {@code <boost/math/special_functions/gamma.hpp>}.
 *
 * @see
 * <a href="https://www.boost.org/doc/libs/1_77_0/libs/math/doc/html/math_toolkit/sf_gamma/igamma.html">
 * Boost C++ Incomplete Gamma functions</a>
 */
public final class RegularizedGamma {
    /** Private constructor. */
    private RegularizedGamma() {
        // intentionally empty.
    }

    /**
     * <a href="http://mathworld.wolfram.com/RegularizedGammaFunction.html">
     * Lower regularized Gamma function</a> \( P(a, x) \).
     *
     * <p>\[ P(a,x) = 1 - Q(a,x) = \frac{\gamma(a,x)}{\Gamma(a)} = \frac{1}{\Gamma(a)} \int_0^x t^{a-1}\,e^{-t}\,dt \]
     */
    public static final class P {
        /** Prevent instantiation. */
        private P() {}

        /**
         * Computes the lower regularized gamma function \( P(a, x) \).
         *
         * @param a Argument.
         * @param x Argument.
         * @return \( P(a, x) \).
         * @throws ArithmeticException if the continued fraction fails to converge.
         */
        public static double value(double a,
                                   double x) {
            return BoostGamma.gammaP(a, x);
        }

        /**
         * Computes the lower regularized gamma function \( P(a, x) \).
         *
         * @param a Argument.
         * @param x Argument.
         * @param epsilon Tolerance in series evaluation.
         * @param maxIterations Maximum number of iterations in series evaluation.
         * @return \( P(a, x) \).
         * @throws ArithmeticException if the series evaluation fails to converge.
         */
        public static double value(double a,
                                   double x,
                                   double epsilon,
                                   int maxIterations) {
            return BoostGamma.gammaP(a, x, new Policy(epsilon, maxIterations));
        }

        /**
         * Computes the derivative of the lower regularized gamma function \( P(a, x) \).
         *
         * <p>\[ \frac{\delta}{\delta x} P(a,x) = \frac{e^{-x} x^{a-1}}{\Gamma(a)} \]
         *
         * <p>This function has uses in some statistical distributions.
         *
         * @param a Argument.
         * @param x Argument.
         * @return derivative of \( P(a,x) \) with respect to x.
         * @since 1.1
         */
        public static double derivative(double a,
                                        double x) {
            return BoostGamma.gammaPDerivative(a, x);
        }
    }

    /**
     * <a href="http://mathworld.wolfram.com/RegularizedGammaFunction.html">
     * Upper regularized Gamma function</a> \( Q(a, x) \).
     *
     * <p>\[ Q(a,x) = 1 - P(a,x) = \frac{\Gamma(a,x)}{\Gamma(a)} = \frac{1}{\Gamma(a)} \int_x^{\infty} t^{a-1}\,e^{-t}\,dt \]
     */
    public static final class Q {
        /** Prevent instantiation. */
        private Q() {}

        /**
         * Computes the upper regularized gamma function \( Q(a, x) \).
         *
         * @param a Argument.
         * @param x Argument.
         * @return \( Q(a, x) \).
         * @throws ArithmeticException if the series evaluation fails to converge.
         */
        public static double value(double a,
                                   double x) {
            return BoostGamma.gammaQ(a, x);
        }

        /**
         * Computes the upper regularized gamma function \( Q(a, x) \).
         *
         * @param a Argument.
         * @param x Argument.
         * @param epsilon Tolerance in series evaluation.
         * @param maxIterations Maximum number of iterations in series evaluation.
         * @return \( Q(a, x) \).
         * @throws ArithmeticException if the series evaluation fails to converge.
         */
        public static double value(final double a,
                                   double x,
                                   double epsilon,
                                   int maxIterations) {
            return BoostGamma.gammaQ(a, x, new Policy(epsilon, maxIterations));
        }

        /**
         * Computes the derivative of the upper regularized gamma function \( Q(a, x) \).
         *
         * <p>\[ \frac{\delta}{\delta x} Q(a,x) = -\frac{e^{-x} x^{a-1}}{\Gamma(a)} \]
         *
         * <p>This function has uses in some statistical distributions.
         *
         * @param a Argument.
         * @param x Argument.
         * @return derivative of \( Q(a,x) \) with respect to x.
         * @since 1.1
         */
        public static double derivative(double a,
                                        double x) {
            return -BoostGamma.gammaPDerivative(a, x);
        }
    }
}
