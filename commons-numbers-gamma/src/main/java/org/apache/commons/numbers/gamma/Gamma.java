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
 * <a href="https://mathworld.wolfram.com/GammaFunction.html">Gamma
 * function</a> \( \Gamma(x) \).
 *
 * <p>The <a href="https://mathworld.wolfram.com/GammaFunction.html">gamma
 * function</a> can be seen to extend the factorial function to cover real and
 * complex numbers, but with its argument shifted by {@code -1}. This
 * implementation supports real numbers.
 *
 * <p>This code has been adapted from:
 * <ul>
 *  <li>The <a href="https://www.boost.org/">Boost</a>
 *      {@code c++} implementation {@code <boost/math/special_functions/gamma.hpp>}.
 *  <li>The <em>NSWC Library of Mathematics Subroutines</em> double
 *      precision implementation, {@code DGAMMA}.
 * </ul>
 *
 * @see
 * <a href="https://www.boost.org/doc/libs/1_77_0/libs/math/doc/html/math_toolkit/sf_gamma/tgamma.html">
 * Boost C++ Gamma functions</a>
 */
public final class Gamma {
    /** Private constructor. */
    private Gamma() {
        // intentionally empty.
    }

    /**
     * Computes the value of \( \Gamma(x) \).
     *
     * @param x Argument.
     * @return \( \Gamma(x) \)
     */
    public static double value(final double x) {
        return BoostGamma.tgamma(x);
    }
}
