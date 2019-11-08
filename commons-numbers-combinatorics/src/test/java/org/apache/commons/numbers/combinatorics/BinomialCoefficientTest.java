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

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for the {@link BinomialCoefficient} class.
 */
public class BinomialCoefficientTest {
    /** Cached binomial coefficients. */
    private static final List<Map<Integer, Long>> binomialCache = new ArrayList<>();

    /** Verify that b(0,0) = 1 */
    @Test
    public void test0Choose0() {
        Assertions.assertEquals(1, BinomialCoefficient.value(0, 0));
    }

    @Test
    public void testBinomialCoefficient() {
        final long[] bcoef5 = {1, 5, 10, 10, 5, 1};
        final long[] bcoef6 = {1, 6, 15, 20, 15, 6, 1};

        for (int i = 0; i < 6; i++) {
            Assertions.assertEquals(bcoef5[i], BinomialCoefficient.value(5, i), "5 choose " + i);
        }
        for (int i = 0; i < 7; i++) {
            Assertions.assertEquals(bcoef6[i], BinomialCoefficient.value(6, i), "6 choose " + i);
        }

        for (int n = 1; n < 10; n++) {
            for (int k = 0; k <= n; k++) {
                Assertions.assertEquals(
                        binomialCoefficient(n, k),
                        BinomialCoefficient.value(n, k),
                        n + " choose " + k
                );
            }
        }

        final int[] n = {34, 66, 100, 1500, 1500};
        final int[] k = {17, 33, 10, 1500 - 4, 4};
        for (int i = 0; i < n.length; i++) {
            final long expected = binomialCoefficient(n[i], k[i]);
            Assertions.assertEquals(
                    expected,
                    BinomialCoefficient.value(n[i], k[i]),
                    n[i] + " choose " + k[i]
            );
        }
    }

    @Test
    public void testBinomialCoefficientFail1() {
        Assertions.assertThrows(CombinatoricsException.class,
            () -> BinomialCoefficient.value(4, 5)
        );
    }

    @Test
    public void testBinomialCoefficientFail2() {
        Assertions.assertThrows(CombinatoricsException.class,
            () -> BinomialCoefficient.value(-1, -2)
        );
    }

    @Test
    public void testBinomialCoefficientFail3() {
        Assertions.assertThrows(ArithmeticException.class,
            () -> BinomialCoefficient.value(67, 30)
        );
    }

    @Test
    public void testBinomialCoefficientFail4() {
        Assertions.assertThrows(ArithmeticException.class,
            () -> BinomialCoefficient.value(67, 34)
        );
    }

    @Test
    public void testBinomialCoefficientFail5() {
        Assertions.assertThrows(ArithmeticException.class,
            () -> BinomialCoefficient.value(700, 300)
        );
    }

    /**
     * Tests correctness for large n and sharpness of upper bound in API doc
     * JIRA: MATH-241
     */
    @Test
    public void testBinomialCoefficientLarge() throws Exception {
        // This tests all legal and illegal values for n <= 200.
        for (int n = 0; n <= 200; n++) {
            for (int k = 0; k <= n; k++) {
                long ourResult = -1;
                long exactResult = -1;
                boolean shouldThrow = false;
                boolean didThrow = false;
                try {
                    ourResult = BinomialCoefficient.value(n, k);
                } catch (ArithmeticException ex) {
                    didThrow = true;
                }
                try {
                    exactResult = binomialCoefficient(n, k);
                } catch (ArithmeticException ex) {
                    shouldThrow = true;
                }
                Assertions.assertEquals(exactResult, ourResult, n + " choose " + k);
                Assertions.assertEquals(shouldThrow, didThrow, n + " choose " + k);
                Assertions.assertTrue(n > 66 || !didThrow, n + " choose " + k);
            }
        }

        long ourResult = BinomialCoefficient.value(300, 3);
        long exactResult = binomialCoefficient(300, 3);
        Assertions.assertEquals(exactResult, ourResult);

        ourResult = BinomialCoefficient.value(700, 697);
        exactResult = binomialCoefficient(700, 697);
        Assertions.assertEquals(exactResult, ourResult);

        final int n = 10000;
        ourResult = BinomialCoefficient.value(n, 3);
        exactResult = binomialCoefficient(n, 3);
        Assertions.assertEquals(exactResult, ourResult);

    }

    @Test
    public void checkNLessThanOne() {
        Assertions.assertThrows(CombinatoricsException.class,
            () -> BinomialCoefficient.checkBinomial(-1, -2)
        );
    }

    @Test
    public void checkKGreaterThanN() {
        Assertions.assertThrows(CombinatoricsException.class,
            () -> BinomialCoefficient.checkBinomial(4, 5)
        );
    }

    @Test
    public void testCheckBinomial3() {
        // OK (no exception thrown)
        BinomialCoefficient.checkBinomial(5, 4);
    }

    /**
     * Exact (caching) recursive implementation to test against.
     */
    static long binomialCoefficient(int n, int k) {
        if (binomialCache.size() > n) {
            final Long cachedResult = binomialCache.get(n).get(Integer.valueOf(k));
            if (cachedResult != null) {
                return cachedResult.longValue();
            }
        }
        long result = -1;
        if ((n == k) || (k == 0)) {
            result = 1;
        } else if ((k == 1) || (k == n - 1)) {
            result = n;
        } else {
            // Reduce stack depth for larger values of n.
            if (k < n - 100) {
                binomialCoefficient(n - 100, k);
            }
            if (k > 100) {
                binomialCoefficient(n - 100, k - 100);
            }
            result = Math.addExact(binomialCoefficient(n - 1, k - 1),
                                                 binomialCoefficient(n - 1, k));
        }
        if (result == -1) {
            throw new IllegalArgumentException();
        }
        for (int i = binomialCache.size(); i < n + 1; i++) {
            binomialCache.add(new HashMap<Integer, Long>());
        }
        binomialCache.get(n).put(Integer.valueOf(k), Long.valueOf(result));
        return result;
    }
}
