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
 * <a href="https://mathworld.wolfram.com/IncompleteBetaFunction.html">
 * Incomplete Beta function</a>.
 *
 * <p>\[ B_x(a,b) = \int_0^x t^{a-1}\,(1-t)^{b-1}\,dt \]
 *
 * <p>This code has been adapted from the <a href="https://www.boost.org/">Boost</a>
 * {@code c++} implementation {@code <boost/math/special_functions/beta.hpp>}.
 *
 * @see
 * <a href="https://www.boost.org/doc/libs/1_77_0/libs/math/doc/html/math_toolkit/sf_beta/ibeta_function.html">
 * Boost C++ Incomplete Beta functions</a>
 * @since 1.1
 */
public final class IncompleteBeta {

    /** Private constructor. */
    private IncompleteBeta() {
        // intentionally empty.
    }

    /**
     * Computes the value of the
     * <a href="https://mathworld.wolfram.com/IncompleteBetaFunction.html">
     * incomplete beta function</a> B(x, a, b).
     *
     * <p>\[ B_x(a,b) = \int_0^x t^{a-1}\,(1-t)^{b-1}\,dt \]
     *
     * @param x Value.
     * @param a Parameter {@code a}.
     * @param b Parameter {@code b}.
     * @return the incomplete beta function \( B_x(a, b) \).
     * @throws ArithmeticException if the series evaluation fails to converge.
     */
    public static double value(double x,
                               double a,
                               double b) {
        return BoostBeta.beta(a, b, x);
    }

    /**
     * Computes the value of the
     * <a href="https://mathworld.wolfram.com/IncompleteBetaFunction.html">
     * incomplete beta function</a> B(x, a, b).
     *
     * <p>\[ B_x(a,b) = \int_0^x t^{a-1}\,(1-t)^{b-1}\,dt \]
     *
     * @param x the value.
     * @param a Parameter {@code a}.
     * @param b Parameter {@code b}.
     * @param epsilon Tolerance in series evaluation.
     * @param maxIterations Maximum number of iterations in series evaluation.
     * @return the incomplete beta function \( B_x(a, b) \).
     * @throws ArithmeticException if the series evaluation fails to converge.
     */
    public static double value(double x,
                               final double a,
                               final double b,
                               double epsilon,
                               int maxIterations) {
        return BoostBeta.beta(a, b, x, new Policy(epsilon, maxIterations));
    }

    /**
     * Computes the complement of the
     * <a href="https://mathworld.wolfram.com/IncompleteBetaFunction.html">
     * incomplete beta function</a> B(x, a, b).
     *
     * <p>\[ B(a, b) - B_x(a,b) = B_{1-x}(b, a) \]
     *
     * <p>where \( B(a, b) \) is the beta function.
     *
     * @param x Value.
     * @param a Parameter {@code a}.
     * @param b Parameter {@code b}.
     * @return the complement of the incomplete beta function \( B(a, b) - B_x(a, b) \).
     * @throws ArithmeticException if the series evaluation fails to converge.
     */
    public static double complement(double x,
                                    double a,
                                    double b) {
        return BoostBeta.betac(a, b, x);
    }

    /**
     * Computes the complement of the
     * <a href="https://mathworld.wolfram.com/IncompleteBetaFunction.html">
     * incomplete beta function</a> B(x, a, b).
     *
     * <p>\[ B(a, b) - B_x(a,b) = B_{1-x}(b, a) \]
     *
     * <p>where \( B(a, b) \) is the beta function.
     *
     * @param x the value.
     * @param a Parameter {@code a}.
     * @param b Parameter {@code b}.
     * @param epsilon Tolerance in series evaluation.
     * @param maxIterations Maximum number of iterations in series evaluation.
     * @return the complement of the incomplete beta function \( B(a, b) - B_x(a, b) \).
     * @throws ArithmeticException if the series evaluation fails to converge.
     */
    public static double complement(double x,
                                    final double a,
                                    final double b,
                                    double epsilon,
                                    int maxIterations) {
        return BoostBeta.betac(a, b, x, new Policy(epsilon, maxIterations));
    }
}
