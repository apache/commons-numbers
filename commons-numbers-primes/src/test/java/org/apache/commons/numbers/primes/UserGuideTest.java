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
package org.apache.commons.numbers.primes;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for examples contained in the user guide.
 */
class UserGuideTest {

    @Test
    void testPrimes1() {
        int n = 51237173;
        boolean prime = Primes.isPrime(n);
        int     m     = Primes.nextPrime(n);
        Assertions.assertFalse(prime);
        Assertions.assertEquals(51237233, m);

        List<Integer> f1 = Primes.primeFactors(n);
        List<Integer> f2 = Primes.primeFactors(m);
        Assertions.assertEquals(Arrays.asList(13, 863, 4567), f1);
        Assertions.assertEquals(Arrays.asList(m), f2);
        // n == 13 * 863 * 4567
        Assertions.assertEquals(n, f1.stream().mapToInt(i -> i).reduce((i, j) -> i * j).getAsInt());
    }
}
