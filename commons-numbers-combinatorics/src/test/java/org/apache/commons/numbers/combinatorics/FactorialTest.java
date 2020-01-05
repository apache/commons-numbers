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
 * Test cases for the {@link Factorial} class.
 */
public class FactorialTest {
    @Test
    public void testFactorialZero() {
        Assertions.assertEquals(1, Factorial.value(0), "0!");
    }

    @Test
    public void testFactorial() {
        for (int i = 1; i < 21; i++) {
            Assertions.assertEquals(factorial(i), Factorial.value(i), i + "!");
        }
    }

    @Test
    public void testPrecondition1() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> Factorial.value(-1)
        );
    }

    @Test
    public void testPrecondition2() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> Factorial.value(21)
        );
    }

    /**
     * Direct multiplication implementation.
     */
    private long factorial(int n) {
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }
}
