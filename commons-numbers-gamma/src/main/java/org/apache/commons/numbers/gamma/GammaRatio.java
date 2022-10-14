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
 * Ratio of <a href="https://mathworld.wolfram.com/GammaFunction.html">Gamma
 * functions</a>.
 *
 * <p>\[ \frac{\Gamma(a)}{\Gamma(b)} \]
 *
 * <p>This code has been adapted from:
 * <ul>
 *  <li>The <a href="https://www.boost.org/">Boost</a>
 *      {@code c++} implementation {@code <boost/math/special_functions/gamma.hpp>}.
 * </ul>
 *
 * @see
 * <a href="https://www.boost.org/doc/libs/1_77_0/libs/math/doc/html/math_toolkit/sf_gamma/gamma_ratios.html">
 * Boost C++ Ratios of Gamma Functions</a>
 * @since 1.1
 */
public final class GammaRatio {
    /** Private constructor. */
    private GammaRatio() {
        // intentionally empty.
    }

    /**
     * Computes the ratio of gamma functions of two values.
     *
     * <p>\[ \frac{\Gamma(a)}{\Gamma(b)} \]
     *
     * <p>If either argument is {@code <= 0} or infinite then the result is NaN.
     *
     * @param a Argument a (must be positive finite).
     * @param b Argument b (must be positive finite).
     * @return \( \Gamma(a) / \Gamma(b) \)
     */
    public static double value(double a, double b) {
        return BoostGamma.tgammaRatio(a, b);
    }

    /**
     * Computes the ratio of gamma functions of a value and an offset value.
     *
     * <p>\[ \frac{\Gamma(a)}{\Gamma(a + delta)} \]
     *
     * <p>Note that the result is calculated accurately even when {@code delta} is
     * small compared to {@code a}: indeed even if {@code a+delta ~ a}. The function
     * is typically used when {@code a} is large and {@code delta} is very small.
     *
     * @param a Argument.
     * @param delta Argument.
     * @return \( \Gamma(a) / \Gamma(a + delta) \)
     */
    public static double delta(double a, double delta) {
        return BoostGamma.tgammaDeltaRatio(a, delta);
    }
}
