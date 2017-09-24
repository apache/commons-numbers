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

import org.apache.commons.numbers.gamma.LogGamma;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for the {@link LogFactorial} class.
 */
public class LogFactorialTest {
    @Test(expected=IllegalArgumentException.class)
    public void testNonPositiveArgument() {
        LogFactorial.create().withCache(-1);
    }

    @Test
    public void testDelegation() {
        final LogFactorial f = LogFactorial.create();

        // Starting at 21 because for smaller arguments, there is no delegation to the
        // "LogGamma" class.
        for (int i = 21; i < 10000; i++) {
            final double expected = LogGamma.value(i + 1);
            Assert.assertEquals(i + "! ",
                                expected, f.value(i), 0d);
        }
    }

    @Test
    public void testCompareDirectWithoutCache() {
        // This test shows that delegating to the "Gamma" class leads to difference
        // wrt the "direct" computation.

        final int max = 100;
        final LogFactorial f = LogFactorial.create();

        for (int i = 0; i < max; i++) {
            final double expected = logFactorial(i);
            Assert.assertEquals(i + "! ",
                                expected, f.value(i), 2 * Math.ulp(expected));
        }
    }

    @Test
    public void testCompareDirectWithCache() {
        final int max = 1000;
        final LogFactorial f = LogFactorial.create().withCache(max);

        for (int i = 0; i < max; i++) {
            final double expected = logFactorial(i);
            Assert.assertEquals(i + "! ",
                                expected, f.value(i), 0d);
        }
    }

    @Test
    public void testZeroCache() {
        // Ensure that no exception is thrown.
        final LogFactorial f = LogFactorial.create().withCache(0);
        Assert.assertEquals(0, f.value(0), 0d);
        Assert.assertEquals(0, f.value(1), 0d);
    }

    @Test
    public void testUselessCache() {
        // Ensure that no exception is thrown.
        LogFactorial.create().withCache(1);
        LogFactorial.create().withCache(2);
    }

    @Test
    public void testCacheIncrease() {
        final int max = 100;
        final LogFactorial f1 = LogFactorial.create().withCache(max);
        final LogFactorial f2 = f1.withCache(2 * max);

        final int val = max + max / 2;
        final double expected = logFactorial(val);
        Assert.assertEquals(expected, f2.value(val), 0d);
    }

    @Test
    public void testCacheDecrease() {
        final int max = 100;
        final LogFactorial f1 = LogFactorial.create().withCache(max);
        final LogFactorial f2 = f1.withCache(max / 2);

        final int val = max / 4;
        final double expected = logFactorial(val);
        Assert.assertEquals(expected, f2.value(val), 0d);
    }

    // Direct implementation.
    private double logFactorial(final int n) {
        double logSum = 0;
        for (int i = 2; i <= n; i++) {
            logSum += Math.log(i);
        }
        return logSum;
    }
}
