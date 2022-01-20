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
 * <a href="https://mathworld.wolfram.com/BetaFunction.html">Beta function</a>.
 *
 * <p>\[ B(a, b) = \frac{\Gamma(a)\ \Gamma(b)}{\Gamma(a+b)} = \frac{(a-1)!\ (b-1)!}{(a+b-1)!} \]
 *
 * <p>where \( \Gamma(z) \) is the gamma function.
 *
 * <p>This code has been adapted from the <a href="https://www.boost.org/">Boost</a>
 * {@code c++} implementation {@code <boost/math/special_functions/beta.hpp>}.
 *
 * @see
 * <a href="https://www.boost.org/doc/libs/1_77_0/libs/math/doc/html/math_toolkit/sf_beta/beta_function.html">
 * Boost C++ Beta function</a>
 * @since 1.1
 */
public final class Beta {

    /** Private constructor. */
    private Beta() {
        // intentionally empty.
    }

    /**
     * Computes the value of the
     * <a href="https://mathworld.wolfram.com/BetaFunction.html">
     * beta function</a> B(a, b).
     *
     * <p>\[ B(a, b) = \frac{\Gamma(a)\ \Gamma(b)}{\Gamma(a+b)} \]
     *
     * <p>where \( \Gamma(z) \) is the gamma function.
     *
     * @param a Parameter {@code a}.
     * @param b Parameter {@code b}.
     * @return the beta function \( B(a, b) \).
     */
    public static double value(double a,
                               double b) {
        return BoostBeta.beta(a, b);
    }
}
