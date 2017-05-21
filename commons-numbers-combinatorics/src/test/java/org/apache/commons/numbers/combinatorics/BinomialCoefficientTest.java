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

import org.junit.Assert;
import org.junit.Test;

import org.apache.commons.numbers.core.ArithmeticUtils;

/**
 * Test cases for the {@link BinomialCoefficient} class.
 */
public class BinomialCoefficientTest {
    /** Cached binomial coefficients. */
    private static final List<Map<Integer, Long>> binomialCache =
        new ArrayList<Map<Integer, Long>>();

    /** Verify that b(0,0) = 1 */
    @Test
    public void test0Choose0() {
        Assert.assertEquals(BinomialCoefficient.value(0, 0), 1);
    }

    @Test
    public void testBinomialCoefficient() {
        final long[] bcoef5 = { 1, 5, 10, 10, 5, 1 };
        final long[] bcoef6 = { 1, 6, 15, 20, 15, 6, 1 };

        for (int i = 0; i < 6; i++) {
            Assert.assertEquals("5 choose " + i, bcoef5[i], BinomialCoefficient.value(5, i));
        }
        for (int i = 0; i < 7; i++) {
            Assert.assertEquals("6 choose " + i, bcoef6[i], BinomialCoefficient.value(6, i));
        }

        for (int n = 1; n < 10; n++) {
            for (int k = 0; k <= n; k++) {
                Assert.assertEquals(n + " choose " + k,
                                    binomialCoefficient(n, k),
                                    BinomialCoefficient.value(n, k));
            }
        }

        final int[] n = { 34, 66, 100, 1500, 1500 };
        final int[] k = { 17, 33, 10, 1500 - 4, 4 };
        for (int i = 0; i < n.length; i++) {
            final long expected = binomialCoefficient(n[i], k[i]);
            Assert.assertEquals(n[i] + " choose " + k[i],
                                expected,
                                BinomialCoefficient.value(n[i], k[i]));
        }
    }

    @Test(expected=CombinatoricsException.class)
    public void testBinomialCoefficientFail1() {
        BinomialCoefficient.value(4, 5);
    }

    @Test(expected=CombinatoricsException.class)
    public void testBinomialCoefficientFail2() {
        BinomialCoefficient.value(-1, -2);
    }

    @Test(expected=ArithmeticException.class)
    public void testBinomialCoefficientFail3() {
        BinomialCoefficient.value(67, 30);
    }

    @Test(expected=ArithmeticException.class)
    public void testBinomialCoefficientFail4() {
        BinomialCoefficient.value(67, 34);
    }

    @Test(expected=ArithmeticException.class)
    public void testBinomialCoefficientFail5() {
        BinomialCoefficient.value(700, 300);
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
                Assert.assertEquals(n + " choose " + k, exactResult, ourResult);
                Assert.assertEquals(n + " choose " + k, shouldThrow, didThrow);
                Assert.assertTrue(n + " choose " + k, (n > 66 || !didThrow));
            }
        }

        long ourResult = BinomialCoefficient.value(300, 3);
        long exactResult = binomialCoefficient(300, 3);
        Assert.assertEquals(exactResult, ourResult);

        ourResult = BinomialCoefficient.value(700, 697);
        exactResult = binomialCoefficient(700, 697);
        Assert.assertEquals(exactResult, ourResult);

        final int n = 10000;
        ourResult = BinomialCoefficient.value(n, 3);
        exactResult = binomialCoefficient(n, 3);
        Assert.assertEquals(exactResult, ourResult);

    }

    @Test(expected=CombinatoricsException.class)
    public void testCheckBinomial1() {
        // n < 0
        BinomialCoefficient.checkBinomial(-1, -2);
    }

    @Test(expected=CombinatoricsException.class)
    public void testCheckBinomial2() {
        // k > n
        BinomialCoefficient.checkBinomial(4, 5);
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
            result = ArithmeticUtils.addAndCheck(binomialCoefficient(n - 1, k - 1),
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
