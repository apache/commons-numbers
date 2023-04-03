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

/**
 * Representation of the <a href="http://mathworld.wolfram.com/BinomialCoefficient.html">
 * binomial coefficient</a>, as a {@code double}.
 * It is "{@code n choose k}", the number of {@code k}-element subsets that
 * can be selected from an {@code n}-element set.
 */
public final class BinomialCoefficientDouble {
    /** The maximum factorial that can be represented as a double. */
    private static final int MAX_FACTORIAL = 170;
    /** The maximum n that can be computed without overflow of a long for any m.
     * {@code C(66, 33) < 2^63}. */
    private static final int LIMIT_N_LONG = 66;
    /** The maximum m that can be computed without overflow of a double.
     * C(1030, 515) ~ 2.85e308. */
    private static final int MAX_M = 514;
    /** The maximum n that can be computed without intermediate overflow for any m.
     * C(1020, 510) * 510 ~ 1.43e308. */
    private static final int SMALL_N = 1020;
    /** The maximum m that can be computed without intermediate overflow for any n.
     * C(2147483647, 37) * 37 ~ 5.13e303. */
    private static final int SMALL_M = 37;

    /** Private constructor. */
    private BinomialCoefficientDouble() {
        // intentionally empty.
    }

    /**
     * Computes the binomial coefficient.
     *
     * <p>The largest value of {@code n} for which <em>all</em> coefficients can
     * fit into a {@code double} is 1029. Larger {@code n} may result in
     * infinity depending on the value of {@code k}.
     *
     * <p>Any {@code min(k, n - k) >= 515} cannot fit into a {@code double}
     * and will result in infinity.
     *
     * @param n Size of the set.
     * @param k Size of the subsets to be counted.
     * @return {@code n choose k}.
     * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0} or {@code k > n}.
     */
    public static double value(int n, int k) {
        if (n <= LIMIT_N_LONG) {
            // Delegate to the exact long result
            return BinomialCoefficient.value(n, k);
        }
        final int m = BinomialCoefficient.checkBinomial(n, k);

        if (m == 0) {
            return 1;
        }
        if (m == 1) {
            return n;
        }

        double result;
        if (n <= MAX_FACTORIAL) {
            // Small factorials are tabulated exactly
            // n! / m! / (n-m)!
            result = Factorial.uncheckedFactorial(n) /
                     Factorial.uncheckedFactorial(m) /
                     Factorial.uncheckedFactorial(n - m);
        } else {
            // Compute recursively using:
            // (n choose m) = (n-1 choose m-1) * n / m

            if (n <= SMALL_N || m <= SMALL_M) {
                // No overflow possible
                result = 1;
                for (int i = 1; i <= m; i++) {
                    result *= n - m + i;
                    result /= i;
                }
            } else {
                if (m > MAX_M) {
                    return Double.POSITIVE_INFINITY;
                }

                // Compute the initial part without overflow checks
                result = 1;
                for (int i = 1; i <= SMALL_M; i++) {
                    result *= n - m + i;
                    result /= i;
                }
                // Careful of overflow
                for (int i = SMALL_M + 1; i <= m; i++) {
                    final double next = result * (n - m + i);
                    if (next > Double.MAX_VALUE) {
                        // Reverse order of terms
                        result /= i;
                        result *= n - m + i;
                        if (result > Double.MAX_VALUE) {
                            // Definite overflow
                            return Double.POSITIVE_INFINITY;
                        }
                    } else {
                        result = next / i;
                    }
                }
            }
        }

        return Math.floor(result + 0.5);
    }
}
