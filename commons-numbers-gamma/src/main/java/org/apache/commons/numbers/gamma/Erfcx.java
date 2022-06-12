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
 * Scaled complementary error function.
 *
 * <p>\[ \operatorname{erfcx}(z) = \operatorname{erfc}(z)\ e^{z^2} \]
 *
 * <p>For large z the value is approximately:
 *
 * <p>\[ \operatorname{erfcx}(z) = \frac{1}{z \sqrt{\pi}} \]
 *
 * @see Erfc
 * @since 1.1
 */
public final class Erfcx {
    /** Private constructor. */
    private Erfcx() {
        // intentionally empty.
    }

    /**
     * Returns the scaled complementary error function.
     *
     * <p>Special cases:
     * <ul>
     * <li>If the argument is 0, then the result is 1.
     * <li>If the argument is +infinity, then the result is 0.
     * <li>If the argument is negative and {@code exp(x*x)} is infinite, then the result is +infinity.
     * <li>If the argument is nan, then the result is nan.
     * </ul>
     *
     * @param x Value.
     * @return the scaled complementary error function.
     */
    public static double value(double x) {
        return BoostErf.erfcx(x);
    }
}
