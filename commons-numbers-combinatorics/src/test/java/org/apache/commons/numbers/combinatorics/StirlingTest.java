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

import java.util.stream.Stream;
import org.apache.commons.numbers.core.ArithmeticUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test cases for the {@link Stirling} class.
 */
class StirlingTest {

    /**
     * Arguments that are illegal for the Stirling number computations.
     *
     * @return the arguments
     */
    static Stream<Arguments> stirlingIllegalArguments() {
        return Stream.of(
            Arguments.of(1, -1),
            Arguments.of(-1, -1),
            Arguments.of(-1, 1),
            Arguments.of(10, 15),
            Arguments.of(Integer.MIN_VALUE, 1),
            Arguments.of(1, Integer.MIN_VALUE),
            Arguments.of(Integer.MIN_VALUE, Integer.MIN_VALUE),
            Arguments.of(Integer.MAX_VALUE - 1, Integer.MAX_VALUE)
        );
    }

    /**
     * Arguments that should easily overflow the Stirling number computations.
     * Used to verify the exception is correct
     * (e.g. no StackOverflowError occurs due to recursion).
     *
     * @return the arguments
     */
    static Stream<Arguments> stirlingOverflowArguments() {
        return Stream.of(
            Arguments.of(123, 32),
            Arguments.of(612534, 56123),
            Arguments.of(261388631, 213),
            Arguments.of(678688997, 213879),
            Arguments.of(1000000002, 1000000000),
            Arguments.of(1000000003, 1000000000),
            Arguments.of(1000000004, 1000000000),
            Arguments.of(1000000005, 1000000000),
            Arguments.of(1000000010, 1000000000),
            Arguments.of(1000000100, 1000000000)
        );
    }

    @ParameterizedTest
    @MethodSource(value = {"stirlingIllegalArguments"})
    void testStirlingS1IllegalArgument(int n, int k) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Stirling.stirlingS1(n, k));
    }

    @Test
    void testStirlingS1StandardCases() {
        Assertions.assertEquals(1, Stirling.stirlingS1(0, 0));

        for (int n = 1; n < 64; ++n) {
            Assertions.assertEquals(0, Stirling.stirlingS1(n, 0));
            if (n < 21) {
                Assertions.assertEquals(ArithmeticUtils.pow(-1, n - 1) * Factorial.value(n - 1),
                                        Stirling.stirlingS1(n, 1));
                if (n > 2) {
                    Assertions.assertEquals(-BinomialCoefficient.value(n, 2),
                                            Stirling.stirlingS1(n, n - 1));
                }
            }
            Assertions.assertEquals(1, Stirling.stirlingS1(n, n));
        }
    }

    @ParameterizedTest
    @CsvSource({
        // Data verified using Mathematica StirlingS1[n, k]
        "5, 3, 35",
        "6, 3, -225",
        "6, 4, 85",
        "7, 3, 1624",
        "7, 4, -735",
        "7, 5, 175",
        "8, 3, -13132",
        "8, 4, 6769",
        "8, 5, -1960",
        "8, 6, 322",
        "9, 3, 118124",
        "9, 4, -67284",
        "9, 5, 22449",
        "9, 6, -4536",
        "9, 7, 546",
        "10, 3, -1172700",
        "10, 4, 723680",
        "10, 5, -269325",
        "10, 6, 63273",
        "10, 7, -9450",
        "10, 8, 870",
        // n >= 21 is not cached
        // ... k in [1, 7] require n <= 21
        "21, 8, -311333643161390640",
        "21, 9, 63030812099294896",
        "22, 10, 276019109275035346",
        "22, 11, -37600535086859745",
        "23, 12, -129006659818331295",
        "23, 13, 12363045847086207",
        "24, 14, 34701806448704206",
        "25, 15, 92446911376173550",
        "25, 16, -5700586321864500",
        "26, 17, -12972753318542875",
        "27, 18, -28460103232088385",
        "28, 19, -60383004803151030",
        "29, 20, -124243455209483610",
        // k in [n-8, n-2]
        "33, 25, 42669229615802790",
        "40, 33, -16386027912368400",
        "66, 60, 98715435586436240",
        "155, 150, -1849441185054164625",
        "404, 400, 1793805203416799170",
        "1003, 1000, -21063481189500750",
        "10002, 10000, 1250583420837500",
        // Limits for k in [n-1, n] use n = Integer.MAX_VALUE
        "2147483647, 2147483646, -2305843005992468481",
        "2147483647, 2147483647, 1",
        // Data for s(n, n-2)
        "21, 19, 20615",
        "22, 20, 25025",
        "23, 21, 30107",
        "24, 22, 35926",
        "25, 23, 42550",
        "26, 24, 50050",
        "27, 25, 58500",
        "92679, 92677, 9221886003909976111",
        "92680, 92678, 9222284027979459010",
        "92681, 92679, 9222682064933083810",
        // Data for s(n, n-3)
        "21, 18, -1256850",
        "22, 19, -1689765",
        "23, 20, -2240315",
        "24, 21, -2932776",
        "25, 22, -3795000",
        "26, 23, -4858750",
        "27, 24, -6160050",
        "2758, 2755, -9145798629595485585",
        "2759, 2756, -9165721700732052911",
        "2760, 2757, -9185680925511388200",
    })
    void testStirlingS1(int n, int k, long expected) {
        Assertions.assertEquals(expected, Stirling.stirlingS1(n, k));
    }

    @ParameterizedTest
    @CsvSource({
        // Upper limits for n with k in [1, 20]
        "21, 1, 2432902008176640000",
        "21, 2, -8752948036761600000",
        "20, 3, -668609730341153280",
        "20, 4, 610116075740491776",
        "21, 5, 8037811822645051776",
        "21, 6, -3599979517947607200",
        "21, 7, 1206647803780373360",
        "22, 8, 7744654310169576800",
        "22, 9, -1634980697246583456",
        "23, 10, -7707401101297361068",
        "23, 11, 1103230881185949736",
        "24, 12, 4070384057007569521",
        "24, 13, -413356714301314056",
        "25, 14, -1246200069070215000",
        "26, 15, -3557372853474553750",
        "26, 16, 234961569422786050",
        "27, 17, 572253155704900800",
        "28, 18, 1340675942971287195",
        "29, 19, 3031400077459516035",
        "30, 20, 6634460278534540725",
        // Upper limits for n with k in [n-9, n-2]
        "35, 26, -5576855646887454930",
        "44, 36, 6364808704290634598",
        "61, 54, -8424028440309413250",
        "95, 89, 8864929183170733205",
        "181, 176, -8872439767850041020",
        "495, 491, 9161199664152744351",
        "2761, 2758, -9205676356399769400",
        "92682, 92680, 9223080114771128550",
    })
    void testStirlingS1LimitsN(int n, int k, long expected) {
        Assertions.assertEquals(expected, Stirling.stirlingS1(n, k));
        Assertions.assertThrows(ArithmeticException.class, () -> Stirling.stirlingS1(n + 1, k));
        Assertions.assertThrows(ArithmeticException.class, () -> Stirling.stirlingS1(n + 100, k));
        Assertions.assertThrows(ArithmeticException.class, () -> Stirling.stirlingS1(n + 10000, k));
    }

    @ParameterizedTest
    @MethodSource(value = {"stirlingOverflowArguments"})
    void testStirlingS1Overflow(int n, int k) {
        Assertions.assertThrows(ArithmeticException.class, () -> Stirling.stirlingS1(n, k));
    }

    @ParameterizedTest
    @MethodSource(value = {"stirlingIllegalArguments"})
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
        // Data for S(n, n-2)
        "26, 24, 47450",
        "27, 25, 55575",
        "28, 26, 64701",
        "29, 27, 74907",
        "30, 28, 86275",
        "31, 29, 98890",
        "32, 30, 112840",
        "92680, 92678, 9222151351858080650",
        "92681, 92679, 9222549384516960590",
        "92682, 92680, 9222947430060167790",
        // Data for S(n, n-3)
        "26, 23, 4126200",
        "27, 24, 5265000",
        "28, 25, 6654375",
        "29, 26, 8336601",
        "30, 27, 10359090",
        "31, 28, 12774790",
        "32, 29, 15642600",
        "2759, 2756, 9152435640507623646",
        "2760, 2757, 9172370757033509130",
        "2761, 2758, 9192342044684582630",
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
        // Upper limits for n with k in [n-10, n-2]
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
    @MethodSource(value = {"stirlingOverflowArguments"})
    void testStirlingS2Overflow(int n, int k) {
        Assertions.assertThrows(ArithmeticException.class, () -> Stirling.stirlingS2(n, k));
    }
}
