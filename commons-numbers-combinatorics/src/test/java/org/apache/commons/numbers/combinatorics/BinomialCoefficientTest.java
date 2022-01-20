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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test cases for the {@link BinomialCoefficient} class.
 */
class BinomialCoefficientTest {
    @ParameterizedTest
    @CsvSource({
        "4, 5",
        "-1, 1",
        "10, -1",
        "-1, -1",
        "-1, -2",
    })
    void testBinomialCoefficientIllegalArguments(int n, int k) {
        Assertions.assertThrows(CombinatoricsException.class, () -> BinomialCoefficient.value(n, k),
            () -> n + " choose " + k);
    }

    @ParameterizedTest
    @CsvSource({
        // Data verified using maxima: binomial(n, k)
        "0, 0, 1",
        "5, 0, 1",
        "5, 1, 5",
        "5, 2, 10",
        "6, 0, 1",
        "6, 1, 6",
        "6, 2, 15",
        "6, 3, 20",
        "34, 17, 2333606220",
        "66, 33, 7219428434016265740",
        "100, 10, 17310309456440",
        "1500, 4, 210094780875",
        "300, 3, 4455100",
        "700, 697, 56921900",
        "10000, 3, 166616670000",
        "412, 9, 863177604710689620",
        "678, 7, 12667255449994080",
        "66, 33, 7219428434016265740",
    })
    void testBinomialCoefficient(int n, int k, long nCk) {
        Assertions.assertEquals(nCk, BinomialCoefficient.value(n, k), () -> n + " choose " + k);
        final int m = n - k;
        Assertions.assertEquals(nCk, BinomialCoefficient.value(n, m), () -> n + " choose " + m);
    }

    @ParameterizedTest
    @CsvSource({
        "67, 30",
        "67, 33",
        "68, 34",
    })
    void testBinomialCoefficientOverflow(int n, int k) {
        Assertions.assertThrows(ArithmeticException.class, () -> BinomialCoefficient.value(n, k),
            () -> n + " choose " + k);
    }

    /**
     * Tests correctness for large n and sharpness of upper bound in API doc.
     * JIRA: MATH-241
     */
    @Test
    void testBinomialCoefficientLarge() {
        // This tests all legal and illegal values for n <= 200.
        final int size = 200;
        final BigInteger[] factorials = new BigInteger[size + 1];
        factorials[0] = BigInteger.ONE;
        for (int n = 1; n <= size; n++) {
            factorials[n] = factorials[n - 1].multiply(BigInteger.valueOf(n));
        }

        for (int i = 0; i <= size; i++) {
            final int n = i;
            for (int j = 0; j <= n; j++) {
                final int k = j;
                final BigInteger nCk = factorials[n].divide(factorials[n - k]).divide(factorials[k]);
                // Exceptions are ignored. If both throw then the results will match as -1.
                long actual = -1;
                long expected = -1;
                try {
                    actual = BinomialCoefficient.value(n, k);
                } catch (final ArithmeticException ex) {
                    // Ignore
                }
                try {
                    expected = nCk.longValueExact();
                } catch (final ArithmeticException ex) {
                    // Ignore
                }
                Assertions.assertEquals(expected, actual, () -> n + " choose " + k);
            }
        }
    }
}
