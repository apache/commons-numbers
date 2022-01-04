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
 * <a href="https://mathworld.wolfram.com/IncompleteGammaFunction.html">
 * Incomplete Gamma functions</a>.
 *
 * <p>By definition, the lower and upper incomplete gamma functions satisfy:
 *
 * <p>\[ \Gamma(a) = \gamma(a, x) + \Gamma(a, x) \]
 *
 * <p>This code has been adapted from the <a href="https://www.boost.org/">Boost</a>
 * {@code c++} implementation {@code <boost/math/special_functions/gamma.hpp>}.
 *
 * @see
 * <a href="https://www.boost.org/doc/libs/1_77_0/libs/math/doc/html/math_toolkit/sf_gamma/igamma.html">
 * Boost C++ Incomplete Gamma functions</a>
 * @since 1.1
 */
public final class IncompleteGamma {
    /** No instances. */
    private IncompleteGamma() {}

    /**
     * <a href="http://mathworld.wolfram.com/IncompleteGammaFunction.html">
     * Lower incomplete Gamma function</a> \( \gamma(a, x) \).
     *
     * <p>\[ \gamma(a,x) = \int_0^x t^{a-1}\,e^{-t}\,dt \]
     * @since 1.1
     */
    public static final class Lower {
        /** No instances. */
        private Lower() {}

        /**
         * Computes the lower incomplete gamma function \( \gamma(a, x) \).
         *
         * @param a Argument.
         * @param x Argument.
         * @return \( \gamma(a, x) \).
         * @throws ArithmeticException if the series evaluation fails to converge.
         */
        public static double value(double a,
                                   double x) {
            return BoostGamma.tgammaLower(a, x);
        }

        /**
         * Computes the lower incomplete gamma function \( \gamma(a, x) \).
         *
         * @param a Argument.
         * @param x Argument.
         * @param epsilon Tolerance in series evaluation.
         * @param maxIterations Maximum number of iterations in series evaluation.
         * @return \( \gamma(a, x) \).
         * @throws ArithmeticException if the series evaluation fails to converge.
         */
        public static double value(final double a,
                                   double x,
                                   double epsilon,
                                   int maxIterations) {
            return BoostGamma.tgammaLower(a, x, new Policy(epsilon, maxIterations));
        }
    }

    /**
     * <a href="http://mathworld.wolfram.com/IncompleteGammaFunction.html">
     * Upper incomplete Gamma function</a> \( \Gamma(a, x) \).
     *
     * <p>\[ \Gamma(a,x) = \int_x^{\infty} t^{a-1}\,e^{-t}\,dt \]
     * @since 1.1
     */
    public static final class Upper {
        /** No instances. */
        private Upper() {}

        /**
         * Computes the upper incomplete gamma function \( \Gamma(a, x) \).
         *
         * @param a Argument.
         * @param x Argument.
         * @return \( \Gamma(a, x) \).
         * @throws ArithmeticException if the series evaluation fails to converge.
         */
        public static double value(double a,
                                   double x) {
            return BoostGamma.tgamma(a, x);
        }

        /**
         * Computes the upper incomplete gamma function \( \Gamma(a, x) \).
         *
         * @param a Argument.
         * @param x Argument.
         * @param epsilon Tolerance in series evaluation.
         * @param maxIterations Maximum number of iterations in series evaluation.
         * @return \( \Gamma(a, x) \).
         * @throws ArithmeticException if the series evaluation fails to converge.
         */
        public static double value(double a,
                                   double x,
                                   double epsilon,
                                   int maxIterations) {
            return BoostGamma.tgamma(a, x, new Policy(epsilon, maxIterations));
        }
    }
}
