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

import java.math.BigInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for the {@link Factorial} class.
 */
class FactorialTest {
    /** The largest representable factorial using a long (n=20). */
    private static final int MAX_N_LONG = 20;
    /** The largest representable factorial using a double (n=170). */
    private static final int MAX_N_DOUBLE = 170;

    @Test
    void testFactorialZero() {
        Assertions.assertEquals(1, Factorial.value(0), "0!");
        Assertions.assertEquals(1.0, Factorial.doubleValue(0), "0!");
    }

    @Test
    void testFactorialNonPositiveArgument() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> Factorial.value(-1)
        );
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> Factorial.doubleValue(-1)
        );
    }

    @Test
    void testFactorialArgumentTooLarge() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> Factorial.value(MAX_N_LONG + 1)
        );
    }

    @Test
    void testFactorialDoubleValueArgumentTooLarge() {
        // Long avoids overflow to negative
        for (long n = MAX_N_DOUBLE + 1; n < Integer.MAX_VALUE; n *= 2) {
            Assertions.assertEquals(Double.POSITIVE_INFINITY, Factorial.doubleValue((int) n));
        }
    }

    @Test
    void testFactorial() {
        // Start at 0!
        long value = 1;
        for (int n = 1; n <= MAX_N_LONG; n++) {
            // n! = (n-1)! * n
            value *= n;
            Assertions.assertEquals(value, Factorial.value(n));
        }
    }

    @Test
    void testFactorialDoubleValue() {
        // Start at 0!
        BigInteger value = BigInteger.ONE;
        for (int n = 1; n <= MAX_N_DOUBLE; n++) {
            // n! = (n-1)! * n
            value = value.multiply(BigInteger.valueOf(n));
            Assertions.assertEquals(value.doubleValue(), Factorial.doubleValue(n));
        }
    }
}
