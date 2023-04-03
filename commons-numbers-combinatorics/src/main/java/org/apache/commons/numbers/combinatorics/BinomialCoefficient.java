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

import org.apache.commons.numbers.core.ArithmeticUtils;

/**
 * Representation of the <a href="http://mathworld.wolfram.com/BinomialCoefficient.html">
 * binomial coefficient</a>.
 * It is "{@code n choose k}", the number of {@code k}-element subsets that
 * can be selected from an {@code n}-element set.
 */
public final class BinomialCoefficient {
    /** The maximum m that can be computed without overflow of a long.
     * {@code C(68, 34) > 2^63}. */
    private static final int MAX_M = 33;
    /** The maximum n that can be computed without intermediate overflow for any m.
     * {@code C(61, 30) * 30 < 2^63}. */
    private static final int SMALL_N = 61;
    /** The maximum n that can be computed without overflow of a long for any m.
     * {@code C(66, 33) < 2^63}. */
    private static final int LIMIT_N = 66;

    /** Private constructor. */
    private BinomialCoefficient() {
        // intentionally empty.
    }

    /**
     * Computes the binomial coefficient.
     *
     * <p>The largest value of {@code n} for which <em>all</em> coefficients can
     * fit into a {@code long} is 66. Larger {@code n} may result in an
     * {@link ArithmeticException} depending on the value of {@code k}.
     *
     * <p>Any {@code min(k, n - k) >= 34} cannot fit into a {@code long}
     * and will result in an {@link ArithmeticException}.
     *
     * @param n Size of the set.
     * @param k Size of the subsets to be counted.
     * @return {@code n choose k}.
     * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0} or {@code k > n}.
     * @throws ArithmeticException if the result is too large to be
     * represented by a {@code long}.
     */
    public static long value(int n, int k) {
        final int m = checkBinomial(n, k);

        if (m == 0) {
            return 1;
        }
        if (m == 1) {
            return n;
        }

        // We use the formulae:
        // (n choose m) = n! / (n-m)! / m!
        // (n choose m) = ((n-m+1)*...*n) / (1*...*m)
        // which can be written
        // (n choose m) = (n-1 choose m-1) * n / m
        long result = 1;
        if (n <= SMALL_N) {
            // For n <= 61, the naive implementation cannot overflow.
            int i = n - m + 1;
            for (int j = 1; j <= m; j++) {
                result = result * i / j;
                i++;
            }
        } else if (n <= LIMIT_N) {
            // For n > 61 but n <= 66, the result cannot overflow,
            // but we must take care not to overflow intermediate values.
            int i = n - m + 1;
            for (int j = 1; j <= m; j++) {
                // We know that (result * i) is divisible by j,
                // but (result * i) may overflow, so we split j:
                // Filter out the gcd, d, so j/d and i/d are integer.
                // result is divisible by (j/d) because (j/d)
                // is relative prime to (i/d) and is a divisor of
                // result * (i/d).
                final long d = ArithmeticUtils.gcd(i, j);
                result = (result / (j / d)) * (i / d);
                ++i;
            }
        } else {
            if (m > MAX_M) {
                throw new ArithmeticException(n + " choose " + k);
            }

            // For n > 66, a result overflow might occur, so we check
            // the multiplication, taking care to not overflow
            // unnecessary.
            int i = n - m + 1;
            for (int j = 1; j <= m; j++) {
                final long d = ArithmeticUtils.gcd(i, j);
                result = Math.multiplyExact(result / (j / d), i / d);
                ++i;
            }
        }

        return result;
    }

    /**
     * Check binomial preconditions.
     *
     * <p>For convenience in implementations this returns the smaller of
     * {@code k} or {@code n - k} allowing symmetry to be exploited in
     * computing the binomial coefficient.
     *
     * @param n Size of the set.
     * @param k Size of the subsets to be counted.
     * @return min(k, n - k)
     * @throws IllegalArgumentException if {@code n < 0}.
     * @throws IllegalArgumentException if {@code k > n} or {@code k < 0}.
     */
    static int checkBinomial(int n,
                             int k) {
        // Combine all checks with a single branch:
        // 0 <= n; 0 <= k <= n
        // Note: If n >= 0 && k >= 0 && n - k < 0 then k > n.
        final int m = n - k;
        // Bitwise or will detect a negative sign bit in any of the numbers
        if ((n | k | m) < 0) {
            // Raise the correct exception
            if (n < 0) {
                throw new CombinatoricsException(CombinatoricsException.NEGATIVE, n);
            }
            throw new CombinatoricsException(CombinatoricsException.OUT_OF_RANGE, k, 0, n);
        }
        return m < k ? m : k;
    }
}
