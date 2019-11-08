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
 * Test cases for the {@link FactorialDouble} class.
 */
public class FactorialDoubleTest {
    @Test
    public void testFactorialZero() {
        Assertions.assertEquals(1, FactorialDouble.create().value(0), 0d, "0!");
    }

    @Test
    public void testFactorialDirect() {
        for (int i = 1; i < 21; i++) {
            Assertions.assertEquals(
                    factorialDirect(i), FactorialDouble.create().value(i), 0d, i + "!");
        }
    }

    @Test
    public void testLargestFactorialDouble() {
        final int n = 170;
        Assertions.assertTrue(
                Double.POSITIVE_INFINITY != FactorialDouble.create().value(n), n + "!");
    }

    @Test
    public void testFactorialDoubleTooLarge() {
        final int n = 171;
        Assertions.assertEquals(
                Double.POSITIVE_INFINITY, FactorialDouble.create().value(n), 0d, n + "!");
    }

    @Test
    public void testNonPositiveArgument() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> FactorialDouble.create().value(-1)
        );
    }

    @Test
    public void testCompareDirectWithoutCache() {
        // This test shows that delegating to the "Gamma" class will also lead to a
        // less accurate result.

        final int max = 100;
        final FactorialDouble f = FactorialDouble.create();

        for (int i = 0; i < max; i++) {
            final double expected = factorialDirect(i);
            Assertions.assertEquals(
                    expected, f.value(i), 100 * Math.ulp(expected), i + "! ");
        }
    }

    @Test
    public void testCompareDirectWithCache() {
        final int max = 100;
        final FactorialDouble f = FactorialDouble.create().withCache(max);

        for (int i = 0; i < max; i++) {
            final double expected = factorialDirect(i);
            Assertions.assertEquals(
                    expected, f.value(i), 100 * Math.ulp(expected), i + "! ");
        }
    }

    @Test
    public void testCacheIncrease() {
        final int max = 100;
        final FactorialDouble f1 = FactorialDouble.create().withCache(max);
        final FactorialDouble f2 = f1.withCache(2 * max);

        final int val = max + max / 2;
        Assertions.assertEquals(f1.value(val), f2.value(val), 0d);
    }

    @Test
    public void testZeroCache() {
        // Ensure that no exception is thrown.
        final FactorialDouble f = FactorialDouble.create().withCache(0);
        Assertions.assertEquals(1, f.value(0), 0d);
        Assertions.assertEquals(1, f.value(1), 0d);
    }

    @Test
    public void testUselessCache() {
        Assertions.assertDoesNotThrow(() -> {
            LogFactorial.create().withCache(1);
            LogFactorial.create().withCache(2);
        });
    }

    @Test
    public void testCacheDecrease() {
        final int max = 100;
        final FactorialDouble f1 = FactorialDouble.create().withCache(max);
        final FactorialDouble f2 = f1.withCache(max / 2);

        final int val = max / 4;
        Assertions.assertEquals(f1.value(val), f2.value(val), 0d);
    }

    /**
     * Direct multiplication implementation.
     */
    private double factorialDirect(int n) {
        double result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }
}
