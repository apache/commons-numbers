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
@SuppressWarnings("deprecation")
class FactorialDoubleTest {
    /** The largest representable factorial (n=170). */
    private static final int MAX_N = 170;

    @Test
    void testNonPositiveArgument() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> FactorialDouble.create().value(-1)
        );
    }

    @Test
    void testFactorials() {
        final FactorialDouble f = FactorialDouble.create();
        for (int n = 0; n <= MAX_N; n++) {
            Assertions.assertEquals(Factorial.doubleValue(n), f.value(n));
        }
        for (final int n : new int[] {MAX_N + 1, 678, Integer.MAX_VALUE}) {
            Assertions.assertEquals(Factorial.doubleValue(n), f.value(n));
        }
    }

    /**
     * Test the {@link FactorialDouble#withCache(int)} method does nothing.
     * This method was deprecated since 1.1.
     * It now allows calling the method with an invalid cache size.
     */
    @Test
    void testWithCacheReturnsThis() {
        final FactorialDouble f = FactorialDouble.create();
        for (int cacheSize : new int[] {-1, 0, 10000000}) {
            Assertions.assertSame(f, f.withCache(cacheSize));
        }
    }
}
