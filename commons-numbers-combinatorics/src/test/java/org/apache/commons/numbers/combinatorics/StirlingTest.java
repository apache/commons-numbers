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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test cases for the {@link Stirling} class.
 */
class StirlingTest {

    @ParameterizedTest
    @CsvSource({
        "1, -1",
        "-1, -1",
        "-1, 1",
        "10, 15",
    })
    void testStirlingS2IllegalArgument(int n, int k) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Stirling.stirlingS2(n, k));
    }

    @Test
    void testStirlingS2StandardCases() {
        Assertions.assertEquals(1, Stirling.stirlingS2(0, 0));

        for (int n = 1; n < 64; ++n) {
            Assertions.assertEquals(0, Stirling.stirlingS2(n, 0));
            Assertions.assertEquals(1, Stirling.stirlingS2(n, 1));
            if (n > 2) {
                Assertions.assertEquals((1L << (n - 1)) - 1L, Stirling.stirlingS2(n, 2));
                Assertions.assertEquals(BinomialCoefficient.value(n, 2),
                                        Stirling.stirlingS2(n, n - 1));
            }
            Assertions.assertEquals(1, Stirling.stirlingS2(n, n));
        }
    }

    @ParameterizedTest
    @CsvSource({
        // Data verified using Mathematica StirlingS2[n, k]
        "5, 3, 25",
        "6, 3, 90",
        "6, 4, 65",
        "7, 3, 301",
        "7, 4, 350",
        "7, 5, 140",
        "8, 3, 966",
        "8, 4, 1701",
        "8, 5, 1050",
        "8, 6, 266",
        "9, 3, 3025",
        "9, 4, 7770",
        "9, 5, 6951",
        "9, 6, 2646",
        "9, 7, 462",
        "10, 3, 9330",
        "10, 4, 34105",
        "10, 5, 42525",
        "10, 6, 22827",
        "10, 7, 5880",
        "10, 8, 750",
        // n >= 26 is not cached
        "30, 2, 536870911",
        "37, 3, 75047248929022825",
        "31, 4, 192050639071964750",
        "29, 5, 1540200411172850701",
        "26, 6, 224595186974125331",
        // ... k in [7, 13] require n < 26
        "26, 14, 477898618396288260",
        "26, 15, 90449030191104000",
        "26, 16, 12725877242482560",
        "27, 17, 35569317763922670",
        "27, 18, 3270191625210510",
        "28, 19, 7626292886912700",
        "28, 20, 474194413703010",
        // k in [n-6, n-2]
        "56, 50, 8735311973699025",
        "115, 110, 79593419077014150",
        "204, 200, 7075992116527915",
        "1003, 1000, 20979521187625000",
        "10002, 10000, 1250416704167500",
        // Limits for k in [n-1, n] use n = Integer.MAX_VALUE
        "2147483647, 2147483646, 2305843005992468481",
        "2147483647, 2147483647, 1",
    })
    void testStirlingS2(int n, int k, long expected) {
        Assertions.assertEquals(expected, Stirling.stirlingS2(n, k));
    }

    @ParameterizedTest
    @CsvSource({
        // Upper limits for n with k in [2, 22]
        "64, 2, 9223372036854775807",
        "41, 3, 6078831630016836625",
        "33, 4, 3073530837671316330",
        "30, 5, 7713000216608565075",
        "28, 6, 8220146115188676396",
        "26, 7, 1631853797991016600",
        "26, 8, 5749622251945664950",
        "25, 9, 1167921451092973005",
        "25, 10, 1203163392175387500",
        "25, 11, 802355904438462660",
        "26, 12, 5149507353856958820",
        "26, 13, 1850568574253550060",
        "27, 14, 8541149231801585700",
        "27, 15, 1834634071262848260",
        "28, 16, 6539643128396047620",
        "28, 17, 898741468057510350",
        "29, 18, 2598531274376323650",
        "30, 19, 7145845579888333500",
        "30, 20, 581535955088511150",
        "31, 21, 1359760239259935240",
        "32, 22, 3069483578649883980",
        // Upper limits for n with with k in [n-10, n-2]
        "33, 23, 6708404338089491700",
        "38, 29, 6766081393022256030",
        "47, 39, 8248929419122431611",
        "63, 56, 8426132708490143472",
        "96, 90, 8130394568857873780",
        "183, 178, 9208213764702344301",
        "496, 492, 9161200151995742994",
        "2762, 2759, 9212349555946145400",
        "92683, 92681, 9223345488487980291",
    })
    void testStirlingS2LimitsN(int n, int k, long expected) {
        Assertions.assertEquals(expected, Stirling.stirlingS2(n, k));
        Assertions.assertThrows(ArithmeticException.class, () -> Stirling.stirlingS2(n + 1, k));
        Assertions.assertThrows(ArithmeticException.class, () -> Stirling.stirlingS2(n + 100, k));
        Assertions.assertThrows(ArithmeticException.class, () -> Stirling.stirlingS2(n + 10000, k));
    }

    @ParameterizedTest
    @CsvSource({
        // Large numbers that should easily overflow. Verifies the exception is correct
        // (e.g. no StackOverflowError occurs due to recursion)
        "123, 32",
        "612534, 56123",
        "261388631, 213",
        "678688997, 213879",
        "1000000002, 1000000000",
        "1000000003, 1000000000",
        "1000000004, 1000000000",
        "1000000005, 1000000000",
        "1000000010, 1000000000",
        "1000000100, 1000000000",
    })
    void testStirlingS2Overflow(int n, int k) {
        Assertions.assertThrows(ArithmeticException.class, () -> Stirling.stirlingS2(n, k));
    }
}
