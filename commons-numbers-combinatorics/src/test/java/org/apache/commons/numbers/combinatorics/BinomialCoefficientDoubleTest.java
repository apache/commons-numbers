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
import org.apache.commons.numbers.core.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test cases for the {@link BinomialCoefficientDouble} class.
 */
class BinomialCoefficientDoubleTest {
    @ParameterizedTest
    @CsvSource({
        "4, 5",
        "-1, 1",
        "10, -1",
        "-1, -1",
        "-1, -2",
    })
    void testBinomialCoefficientIllegalArguments(int n, int k) {
        Assertions.assertThrows(CombinatoricsException.class, () -> BinomialCoefficientDouble.value(n, k),
            () -> n + " choose " + k);
    }

    @ParameterizedTest
    @CsvSource({
        // Data verified using maxima: bfloat(binomial(n, k)) using 30 digits of precision.
        // Note: This test will correctly assert infinite expected values.
        "0, 0, 1, 0",
        "5, 0, 1, 0",
        "5, 1, 5, 0",
        "5, 2, 10, 0",
        "6, 0, 1, 0",
        "6, 1, 6, 0",
        "6, 2, 15, 0",
        "6, 3, 20, 0",
        "34, 17, 2333606220, 0",
        "66, 33, 7219428434016265740, 0",
        "100, 10, 17310309456440, 0",
        "1500, 4, 210094780875, 0",
        "300, 3, 4455100, 0",
        "700, 697, 56921900, 0",
        "10000, 3, 166616670000, 0",
        "412, 9, 863177604710689620, 0",
        "678, 7, 12667255449994080, 0",
        "66, 33, 7219428434016265740, 0",
        // Overflow as a long
        "67, 30, 9989690752182277136, 1",
        "67, 33, 14226520737620288370, 0",
        "68, 34, 28453041475240576740, 0",
        // Overflow without special handling
        // See NUMBERS-183
        "1040, 450, 2.3101613255412135615e307, 11",
        "1029, 514, 1.4298206864989040819e308, 5",
        "1786388282, 38, 7.187239013254065384599502085053593e306, 0",
        "1914878305, 38, 100.6570419073661447979173868523364e306, 1",
        "1179067476, 39, 30.22890249420109200962786203300876e306, 2",
        "2147483647, 37, 1.388890512412231479281222156415993e302, 4",
        "20000, 116, 1.75293130532995289393810309132e308, 8",
        "20000, 117, 2.97908427992998148231326853571e310, 0",
        "1028, 514, 7.156051054877897008430135897e307, 8",
        "1030, 496, 1.41941785031194251722295917039e308, 0",
        "1030, 497, 1.52508879691464246317315935007e308, 32",
        "1030, 498, 1.63227375252109323869737737668e308, 0",
        "1030, 499, 1.74021971210665651901203359598e308, 12",
        "1030, 500, 1.84811333425726922319077967894e308, 0",
        "1020, 510, 2.80626776829962271039414307883e305, 8",
        "1022, 511, 1.12140876377061244121816833013e306, 14",
        "1024, 512, 4.48125455209897081002416485048e306, 3",
    })
    void testBinomialCoefficient(int n, int k, double nCk, int ulp) {
        assertBinomial(n, k, nCk, ulp);
    }

    @ParameterizedTest
    @CsvSource({
        "1030, 515",
        "10000000, 10000",
    })
    void testBinomialCoefficientOverflow(int n, int k) {
        Assertions.assertEquals(Double.POSITIVE_INFINITY, BinomialCoefficientDouble.value(n, k),
            () -> n + " choose " + k);
    }

    /**
     * Tests correctness for large n and sharpness of upper bound in API doc.
     * JIRA: MATH-241
     */
    @Test
    void testBinomialCoefficientLarge() {
        // This tests all values for n <= 200.
        final int size = 200;
        final BigInteger[] factorials = new BigInteger[size + 1];
        factorials[0] = BigInteger.ONE;
        for (int n = 1; n <= size; n++) {
            factorials[n] = factorials[n - 1].multiply(BigInteger.valueOf(n));
        }

        for (int n = 0; n <= size; n++) {
            int ulp;
            if (n <= 66) {
                ulp = 0;
            } else if (n <= 100) {
                ulp = 5;
            } else if (n <= 150) {
                ulp = 10;
            } else {
                ulp = 15;
            }
            for (int k = 0; k <= n / 2; k++) {
                final BigInteger nCk = factorials[n].divide(factorials[n - k]).divide(factorials[k]);
                final double expected = nCk.doubleValue();
                assertBinomial(n, k, expected, ulp);
            }
        }
    }

    private static void assertBinomial(int n, int k, double expected, int ulp) {
        final double actual = BinomialCoefficientDouble.value(n, k);
        if (expected == Double.POSITIVE_INFINITY) {
            Assertions.assertEquals(expected, actual, () -> n + " choose " + k);
        } else {
            Assertions.assertTrue(Precision.equals(expected, actual, ulp),
                () -> String.format("C(%d, %d) = %s : actual %s : ULP error = %d", n, k,
                    expected, actual,
                    Double.doubleToRawLongBits(actual) - Double.doubleToRawLongBits(expected)));
        }
        // Test symmetry
        Assertions.assertEquals(actual, BinomialCoefficientDouble.value(n, n - k), () -> n + " choose " + k);
    }
}
