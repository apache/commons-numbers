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
    /** Stirling S1 error message. */
    private static final String S1_ERROR_FORMAT = "s(n=%d, k=%d)";
    /** Stirling S2 error message. */
    private static final String S2_ERROR_FORMAT = "S(n=%d, k=%d)";
    /** Overflow threshold for n when computing s(n, 1). */
    private static final int S1_OVERFLOW_K_EQUALS_1 = 21;
    /** Overflow threshold for n when computing s(n, n-2). */
    private static final int S1_OVERFLOW_K_EQUALS_NM2 = 92682;
    /** Overflow threshold for n when computing s(n, n-3). */
    private static final int S1_OVERFLOW_K_EQUALS_NM3 = 2761;
    /** Overflow threshold for n when computing S(n, n-2). */
    private static final int S2_OVERFLOW_K_EQUALS_NM2 = 92683;
    /** Overflow threshold for n when computing S(n, n-3). */
    private static final int S2_OVERFLOW_K_EQUALS_NM3 = 2762;

    /**
     * Precomputed Stirling numbers of the first kind.
     * Provides a thread-safe lazy initialization of the cache.
     */
    private static class StirlingS1Cache {
        /** Maximum n to compute (exclusive).
         * As s(21,3) = 13803759753640704000 is larger than Long.MAX_VALUE
         * we must stop computation at row 21. */
        static final int MAX_N = 21;
        /** Stirling numbers of the first kind. */
        static final long[][] S1;

        static {
            S1 = new long[MAX_N][];
            // Initialise first two rows to allow s(2, 1) to use s(1, 1)
            S1[0] = new long[] {1};
            S1[1] = new long[] {0, 1};
            for (int n = 2; n < S1.length; n++) {
                S1[n] = new long[n + 1];
                S1[n][0] = 0;
                S1[n][n] = 1;
                for (int k = 1; k < n; k++) {
                    S1[n][k] = S1[n - 1][k - 1] - (n - 1) * S1[n - 1][k];
                }
            }
        }
    }

    /**
     * Precomputed Stirling numbers of the second kind.
     * Provides a thread-safe lazy initialization of the cache.
     */
    private static class StirlingS2Cache {
        /** Maximum n to compute (exclusive).
         * As S(26,9) = 11201516780955125625 is larger than Long.MAX_VALUE
         * we must stop computation at row 26. */
        static final int MAX_N = 26;
        /** Stirling numbers of the second kind. */
        static final long[][] S2;

        static {
            S2 = new long[MAX_N][];
            S2[0] = new long[] {1};
            for (int n = 1; n < S2.length; n++) {
                S2[n] = new long[n + 1];
                S2[n][0] = 0;
                S2[n][1] = 1;
                S2[n][n] = 1;
                for (int k = 2; k < n; k++) {
                    S2[n][k] = k * S2[n - 1][k] + S2[n - 1][k - 1];
                }
            }
        }
    }

    /** Private constructor. */
    private Stirling() {
        // intentionally empty.
    }

    /**
     * Returns the <em>signed</em> <a
     * href="https://mathworld.wolfram.com/StirlingNumberoftheFirstKind.html">
     * Stirling number of the first kind</a>, "{@code s(n,k)}". The number of permutations of
     * {@code n} elements which contain exactly {@code k} permutation cycles is the
     * nonnegative number: {@code |s(n,k)| = (-1)^(n-k) s(n,k)}
     *
     * @param n Size of the set
     * @param k Number of permutation cycles ({@code 0 <= k <= n})
     * @return {@code s(n,k)}
     * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0} or {@code k > n}.
     * @throws ArithmeticException if some overflow happens, typically for n exceeding 20
     * (s(n,n-1) is handled specifically and does not overflow)
     */
    public static long stirlingS1(int n, int k) {
        checkArguments(n, k);

        if (n < StirlingS1Cache.MAX_N) {
            // The number is in the small cache
            return StirlingS1Cache.S1[n][k];
        }

        // Simple cases
        // https://en.wikipedia.org/wiki/Stirling_numbers_of_the_first_kind#Simple_identities
        if (k == 0) {
            return 0;
        } else if (k == n) {
            return 1;
        } else if (k == 1) {
            checkN(n, k, S1_OVERFLOW_K_EQUALS_1, S1_ERROR_FORMAT);
            // Note: Only occurs for n=21 so avoid computing the sign with pow(-1, n-1) * (n-1)!
            return Factorial.value(n - 1);
        } else if (k == n - 1) {
            return -BinomialCoefficient.value(n, 2);
        } else if (k == n - 2) {
            checkN(n, k, S1_OVERFLOW_K_EQUALS_NM2, S1_ERROR_FORMAT);
            // (3n-1) * binom(n, 3) / 4
            return productOver4(3L * n - 1, BinomialCoefficient.value(n, 3));
        } else if (k == n - 3) {
            checkN(n, k, S1_OVERFLOW_K_EQUALS_NM3, S1_ERROR_FORMAT);
            return -BinomialCoefficient.value(n, 2) * BinomialCoefficient.value(n, 4);
        }

        // Compute using:
        // s(n + 1, k) = s(n, k - 1)     - n       * s(n, k)
        // s(n, k)     = s(n - 1, k - 1) - (n - 1) * s(n - 1, k)

        // n >= 21 (MAX_N)
        // 2 <= k <= n-4

        // Start at the largest easily computed value: n < MAX_N or k < 2
        final int reduction = Math.min(n - StirlingS1Cache.MAX_N, k - 2) + 1;
        int n0 = n - reduction;
        int k0 = k - reduction;

        long sum = stirlingS1(n0, k0);
        while (n0 < n) {
            k0++;
            sum = Math.subtractExact(
                sum,
                Math.multiplyExact(n0, stirlingS1(n0, k0))
            );
            n0++;
        }

        return sum;
    }

    /**
     * Returns the <a
     * href="https://mathworld.wolfram.com/StirlingNumberoftheSecondKind.html">
     * Stirling number of the second kind</a>, "{@code S(n,k)}", the number of
     * ways of partitioning an {@code n}-element set into {@code k} non-empty
     * subsets.
     *
     * @param n Size of the set
     * @param k Number of non-empty subsets ({@code 0 <= k <= n})
     * @return {@code S(n,k)}
     * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0} or {@code k > n}.
     * @throws ArithmeticException if some overflow happens, typically for n exceeding 25 and
     * k between 20 and n-2 (S(n,n-1) is handled specifically and does not overflow)
     */
    public static long stirlingS2(int n, int k) {
        checkArguments(n, k);

        if (n < StirlingS2Cache.MAX_N) {
            // The number is in the small cache
            return StirlingS2Cache.S2[n][k];
        }

        // Simple cases
        if (k == 0) {
            return 0;
        } else if (k == 1 || k == n) {
            return 1;
        } else if (k == 2) {
            checkN(n, k, 64, S2_ERROR_FORMAT);
            return (1L << (n - 1)) - 1L;
        } else if (k == n - 1) {
            return BinomialCoefficient.value(n, 2);
        } else if (k == n - 2) {
            checkN(n, k, S2_OVERFLOW_K_EQUALS_NM2, S2_ERROR_FORMAT);
            // (3n-5) * binom(n, 3) / 4
            return productOver4(3L * n - 5, BinomialCoefficient.value(n, 3));
        } else if (k == n - 3) {
            checkN(n, k, S2_OVERFLOW_K_EQUALS_NM3, S2_ERROR_FORMAT);
            return BinomialCoefficient.value(n - 2, 2) * BinomialCoefficient.value(n, 4);
        }

        // Compute using:
        // S(n, k) = k * S(n - 1, k) + S(n - 1, k - 1)

        // n >= 26 (MAX_N)
        // 3 <= k <= n-3

        // Start at the largest easily computed value: n < MAX_N or k < 3
        final int reduction = Math.min(n - StirlingS2Cache.MAX_N, k - 3) + 1;
        int n0 = n - reduction;
        int k0 = k - reduction;

        long sum = stirlingS2(n0, k0);
        while (n0 < n) {
            k0++;
            sum = Math.addExact(
                Math.multiplyExact(k0, stirlingS2(n0, k0)),
                sum
            );
            n0++;
        }

        return sum;
    }

    /**
     * Check {@code 0 <= k <= n}.
     *
     * @param n N
     * @param k K
     * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0} or {@code k > n}.
     */
    private static void checkArguments(int n, int k) {
        // Combine all checks with a single branch:
        // 0 <= n; 0 <= k <= n
        // Note: If n >= 0 && k >= 0 && n - k < 0 then k > n.
        // Bitwise or will detect a negative sign bit in any of the numbers
        if ((n | k | (n - k)) < 0) {
            // Raise the correct exception
            if (n < 0) {
                throw new CombinatoricsException(CombinatoricsException.NEGATIVE, n);
            }
            throw new CombinatoricsException(CombinatoricsException.OUT_OF_RANGE, k, 0, n);
        }
    }

    /**
     * Check {@code n <= threshold}, or else throw an {@link ArithmeticException}.
     *
     * @param n N
     * @param k K
     * @param threshold Threshold for {@code n}
     * @param msgFormat Error message format
     * @throws ArithmeticException if overflow is expected to happen
     */
    private static void checkN(int n, int k, int threshold, String msgFormat) {
        if (n > threshold) {
            throw new ArithmeticException(String.format(msgFormat, n, k));
        }
    }

    /**
     * Return {@code a*b/4} without intermediate overflow.
     * It is assumed that:
     * <ul>
     * <li>The coefficients a and b are positive
     * <li>The product (a*b) is an exact multiple of 4
     * <li>The result (a*b/4) is an exact integer that does not overflow a {@code long}
     * </ul>
     *
     * <p>A conditional branch is performed on the odd/even property of {@code b}.
     * The branch is predictable if {@code b} is typically the same parity.
     *
     * @param a Coefficient a
     * @param b Coefficient b
     * @return {@code a*b/4}
     */
    private static long productOver4(long a, long b) {
        // Compute (a*b/4) without intermediate overflow.
        // The product (a*b) must be an exact multiple of 4.
        // If b is even: ((b/2) * a) / 2
        // If b is odd then a must be even to make a*b even: ((a/2) * b) / 2
        return (b & 1) == 0 ?
            ((b >>> 1) * a) >>> 1 :
            ((a >>> 1) * b) >>> 1;
    }
}
