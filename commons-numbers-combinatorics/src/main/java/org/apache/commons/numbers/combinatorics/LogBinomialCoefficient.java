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

package org.apache.commons.numbers.combinatorics;

import org.apache.commons.numbers.gamma.LogBeta;

/**
 * Natural logarithm of the <a href="http://mathworld.wolfram.com/BinomialCoefficient.html">
 * binomial coefficient</a>.
 * It is "{@code n choose k}", the number of {@code k}-element subsets that
 * can be selected from an {@code n}-element set.
 */
public final class LogBinomialCoefficient {
    /** The maximum n that can be computed without overflow of a long for any m.
     * {@code C(66, 33) < 2^63}. */
    private static final int LIMIT_N_LONG = 66;
    /** The maximum n that can be computed without overflow of a double for an m.
     * C(1029, 514) ~ 1.43e308. */
    private static final int LIMIT_N_DOUBLE = 1029;
    /** The maximum m that can be computed without overflow of a double for any n.
     * C(2147483647, 37) ~ 1.39e302. */
    private static final int LIMIT_M_DOUBLE = 37;

    /** Private constructor. */
    private LogBinomialCoefficient() {
        // intentionally empty.
    }

    /**
     * Computes the logarithm of the binomial coefficient.
     *
     * <p>This returns a finite result for any valid {@code n choose k}.
     *
     * @param n Size of the set.
     * @param k Size of the subsets to be counted.
     * @return {@code log(n choose k)}.
     * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0} or {@code k > n}.
     */
    public static double value(int n, int k) {
        final int m = BinomialCoefficient.checkBinomial(n, k);

        if (m == 0) {
            return 0;
        }
        if (m == 1) {
            return Math.log(n);
        }

        if (n <= LIMIT_N_LONG) {
            // Delegate to the exact long result
            return Math.log(BinomialCoefficient.value(n, k));
        }
        if (n <= LIMIT_N_DOUBLE || m <= LIMIT_M_DOUBLE) {
            // Delegate to the double result
            return Math.log(BinomialCoefficientDouble.value(n, k));
        }

        //    n!                gamma(n+1)               gamma(k+1) * gamma(n-k+1)
        // ---------   = ------------------------- = 1 / -------------------------
        // k! (n-k)!     gamma(k+1) * gamma(n-k+1)              gamma(n+1)
        //
        //
        //             = 1 / (k * beta(k, n-k+1))
        //
        // where: beta(a, b) = gamma(a) * gamma(b) / gamma(a+b)

        // Delegate to LogBeta
        return -Math.log(m) - LogBeta.value(m, n - m + 1);
    }
}
