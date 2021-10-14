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
 * <a href="https://mathworld.wolfram.com/Erf.html">Error function</a>.
 *
 * <p>\[ \operatorname{erf}(z) = \frac{2}{\sqrt\pi}\int_0^z e^{-t^2}\,dt \]
 */
public final class Erf {
    /** Private constructor. */
    private Erf() {
        // intentionally empty.
    }

    /**
     * Returns the error function.
     *
     * <p>The returned value is always between -1 and 1 (inclusive).
     * The appropriate extreme is returned when {@code erf(x)} is
     * indistinguishable from either -1 or 1 at {@code double} precision.
     *
     * <p>Special cases:
     * <ul>
     * <li>If the argument is 0, then the result is 0.
     * <li>If the argument is {@code > 6}, then the result is 1.
     * <li>If the argument is {@code < 6}, then the result is -1.
     * <li>If the argument is nan, then the result is nan.
     * </ul>
     *
     * @param x the value.
     * @return the error function.
     */
    public static double value(double x) {
        return BoostErf.erf(x);
    }
}
