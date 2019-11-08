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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for the {@link BinomialCoefficient} class.
 */
public class BinomialCoefficientDoubleTest {
    /** Verify that b(0,0) = 1 */
    @Test
    public void test0Choose0() {
        Assertions.assertEquals(1d, BinomialCoefficientDouble.value(0, 0), 0);
    }

    @Test
    public void testBinomialCoefficient() {
        final long[] bcoef5 = {1, 5, 10, 10, 5, 1};
        final long[] bcoef6 = {1, 6, 15, 20, 15, 6, 1};

        for (int n = 1; n < 10; n++) {
            for (int k = 0; k <= n; k++) {
                Assertions.assertEquals(
                        BinomialCoefficientTest.binomialCoefficient(n, k),
                        BinomialCoefficientDouble.value(n, k),
                        Double.MIN_VALUE,
                        n + " choose " + k
                );
            }
        }

        final int[] n = {34, 66, 100, 1500, 1500};
        final int[] k = {17, 33, 10, 1500 - 4, 4};
        for (int i = 0; i < n.length; i++) {
            final long expected = BinomialCoefficientTest.binomialCoefficient(n[i], k[i]);
            Assertions.assertEquals(
                    expected,
                    BinomialCoefficientDouble.value(n[i], k[i]),
                    0.0,
                    n[i] + " choose " + k[i]
            );
        }
    }

    @Test
    public void testBinomialCoefficientFail1() {
        Assertions.assertThrows(CombinatoricsException.class,
            () -> BinomialCoefficientDouble.value(4, 5)
        );
    }

    @Test
    public void testBinomialCoefficientFail2() {
        Assertions.assertThrows(CombinatoricsException.class,
            () -> BinomialCoefficientDouble.value(-1, -2)
        );
    }

    @Test
    public void testBinomialCoefficientFail3() {
        final double x = BinomialCoefficientDouble.value(1030, 515);
        Assertions.assertTrue(Double.isInfinite(x), "expecting infinite binomial coefficient");
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
                long exactResult = -1;
                boolean shouldThrow = false;
                boolean didThrow = false;
                try {
                    BinomialCoefficient.value(n, k);
                } catch (ArithmeticException ex) {
                    didThrow = true;
                }
                try {
                    exactResult = BinomialCoefficientTest.binomialCoefficient(n, k);
                } catch (ArithmeticException ex) {
                    shouldThrow = true;
                }

                if (!shouldThrow && exactResult > 1) {
                    Assertions.assertEquals(
                            1.,
                            BinomialCoefficientDouble.value(n, k) / exactResult,
                            1e-10,
                            n + " choose " + k
                    );
                }
            }
        }

        final int n = 10000;
        final double actualOverExpected = BinomialCoefficientDouble.value(n, 3) /
            BinomialCoefficientTest.binomialCoefficient(n, 3);
        Assertions.assertEquals(1, actualOverExpected, 1e-10);
    }
}
