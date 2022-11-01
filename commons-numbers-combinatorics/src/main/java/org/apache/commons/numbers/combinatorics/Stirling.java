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
 * Computation of <a href="https://en.wikipedia.org/wiki/Stirling_number">Stirling numbers</a>.
 *
 * @since 1.2
 */
public final class Stirling {
    /** Stirling S2 error message. */
    private static final String S2_ERROR_FORMAT = "S(n=%d, k=%d)";
    /** Overflow threshold for n when computing S(n, n-2). */
    private static final int S2_OVERFLOW_K_EQUALS_NM2 = 92683;

    /**
     * Precomputed Stirling numbers of the second kind.
     * Provides a thread-safe lazy initialization of the cache.
     */
    private static class StirlingS2Cache {
        /** Stirling numbers of the second kind. */
        static final long[][] STIRLING_S2;
        /** Maximum n to compute (exclusive).
         * As S(26,9) = 11201516780955125625 is larger than Long.MAX_VALUE
         * we must stop computation at row 26. */
        private static final int MAX_N = 26;

        static {
            STIRLING_S2 = new long[MAX_N][];
            STIRLING_S2[0] = new long[] {1};
            for (int n = 1; n < STIRLING_S2.length; n++) {
                STIRLING_S2[n] = new long[n + 1];
                STIRLING_S2[n][0] = 0;
                STIRLING_S2[n][1] = 1;
                STIRLING_S2[n][n] = 1;
                for (int k = 2; k < n; k++) {
                    STIRLING_S2[n][k] = k * STIRLING_S2[n - 1][k] + STIRLING_S2[n - 1][k - 1];
                }
            }
        }
    }

    /** Private constructor. */
    private Stirling() {
        // intentionally empty.
    }

    /**
     * Returns the <a
     * href="http://mathworld.wolfram.com/StirlingNumberoftheSecondKind.html">
     * Stirling number of the second kind</a>, "{@code S(n,k)}", the number of
     * ways of partitioning an {@code n}-element set into {@code k} non-empty
     * subsets.
     *
     * @param n the size of the set
     * @param k the number of non-empty subsets ({@code 0 <= k <= n})
     * @return {@code S(n,k)}
     * @throws IllegalArgumentException if {@code k < 0} or {@code k > n}.
     * @throws ArithmeticException if some overflow happens, typically for n exceeding 25 and
     * k between 20 and n-2 (S(n,n-1) is handled specifically and does not overflow)
     */
    public static long stirlingS2(int n, int k) {
        if (k < 0) {
            throw new CombinatoricsException(CombinatoricsException.NEGATIVE, k);
        }
        if (k > n) {
            throw new CombinatoricsException(CombinatoricsException.OUT_OF_RANGE, k, 0, n);
        }

        if (n < StirlingS2Cache.STIRLING_S2.length) {
            // The number is in the small cache
            return StirlingS2Cache.STIRLING_S2[n][k];
        }

        // Simple cases
        if (k == 0) {
            return 0;
        } else if (k == 1 || k == n) {
            return 1;
        } else if (k == 2) {
            checkN(n, k, 64);
            return (1L << (n - 1)) - 1L;
        } else if (k == n - 1) {
            return BinomialCoefficient.value(n, 2);
        }

        // Compute using: S(n, k) = k * S(n - 1, k) + S(n - 1, k - 1)

        if (k == n - 2) {
            // Given:
            //    k * S(n - 1, k) == (n-2) * S(n-1, n-2)) == (n-2) * binom(n-1, 2))
            // the recursion reduces to a sum of binomial coefficients:
            //   for i in [1, k]:
            //     sum (i * binom(i+1, 2))
            // Avoid overflow checks using the known limit for n when k=n-2
            checkN(n, k, S2_OVERFLOW_K_EQUALS_NM2);
            long binom = BinomialCoefficient.value(k + 1, 2);
            long sum = 0;
            for (int i = k; i > 0; i--) {
                sum += i * binom;
                // update binomial coefficient:
                // binom(i, 2) = binom(i+1, 2) - i
                binom -= i;
            }
            return sum;
        }

        // n >= 26 (MAX_N)
        // 3 <= k <= n-3

        // Start at the largest easily computed value: n < MAX_N or k < 3
        final int reduction = Math.min(n - StirlingS2Cache.MAX_N, k - 3) + 1;
        int n0 = n - reduction;
        int k0 = k - reduction;

        long sum = stirlingS2(n0, k0);
        while (n0 < n) {
            n0++;
            k0++;
            sum = Math.addExact(
                Math.multiplyExact(k0, stirlingS2(n0 - 1, k0)),
                sum
            );
        }

        return sum;
    }

    /**
     * Check {@code n <= threshold}, or else throw an {@link ArithmeticException}.
     *
     * @param n N
     * @param k K
     * @param threshold Threshold for {@code n}
     * @throws ArithmeticException if overflow is expected to happen
     */
    private static void checkN(int n, int k, int threshold) {
        if (n > threshold) {
            throw new ArithmeticException(String.format(S2_ERROR_FORMAT, n, k));
        }
    }
}
