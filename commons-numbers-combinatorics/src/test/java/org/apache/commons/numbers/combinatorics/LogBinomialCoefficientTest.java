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
 * Test cases for the {@link LogBinomialCoefficient} class.
 */
class LogBinomialCoefficientTest {
    @ParameterizedTest
    @CsvSource({
        "4, 5",
        "-1, 1",
        "10, -1",
        "-1, -1",
        "-1, -2",
    })
    void testBinomialCoefficientIllegalArguments(int n, int k) {
        Assertions.assertThrows(CombinatoricsException.class, () -> LogBinomialCoefficient.value(n, k),
            () -> n + " choose " + k);
    }

    @ParameterizedTest
    @CsvSource({
        // Data verified using maxima: bfloat(binomial(n, k)) using 30 digits of precision.
        // Note: This test avoids infinite expected values.
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
        "67, 30, 9989690752182277136, 0",
        "67, 33, 14226520737620288370, 0",
        "68, 34, 28453041475240576740, 0",
        // Overflow a double without special handling
        // See NUMBERS-183
        "1040, 450, 2.3101613255412135615e307, 1",
        "1029, 514, 1.4298206864989040819e308, 0",
        "1786388282, 38, 7.187239013254065384599502085053593e306, 1",
        "1914878305, 38, 100.6570419073661447979173868523364e306, 1",
        "1179067476, 39, 30.22890249420109200962786203300876e306, 0",
        "2147483647, 37, 1.388890512412231479281222156415993e302, 0",
        "20000, 116, 1.75293130532995289393810309132e308, 0",
        "1028, 514, 7.156051054877897008430135897e307, 0",
        "1030, 496, 1.41941785031194251722295917039e308, 0",
        "1030, 497, 1.52508879691464246317315935007e308, 1",
        "1030, 498, 1.63227375252109323869737737668e308, 1",
        "1030, 499, 1.74021971210665651901203359598e308, 1",
        "1020, 510, 2.80626776829962271039414307883e305, 0",
        "1022, 511, 1.12140876377061244121816833013e306, 0",
        "1024, 512, 4.48125455209897081002416485048e306, 0",
    })
    void testBinomialCoefficient(int n, int k, double nCk, int ulp) {
        Assertions.assertTrue(Double.isFinite(nCk));
        assertBinomial(n, k, Math.log(nCk), ulp);
    }

    @ParameterizedTest
    @CsvSource({
        // Data verified using maxima: bfloat(log(binomial(n, k))) using 30 digits of precision.
        "20000, 117, 7.14892994792834554505294427064e2, 0",
        "1030, 500, 7.09810373941566367297112517919e2, 0",
        "1030, 515, 7.10246904865078629457587850298e2, 1",
        "10000000, 10000, 7.90670275055185025423945062124e4, 0",
        "10000000, 10000, 7.90670275055185025423945062124e4, 0",
        "152635712, 789, 1.03890814359013076045677773736e4, 1",
        "152635712, 4546, 5.19172217038600425710151646693e4, 1",
        "152635712, 125636, 1.01789719965975898402939070835e6, 1",
        "2147483647, 107, 1.90292037817495610257804518397e3, 0",
        "2147483647, 207, 3.54746695691646741842751657371e3, 1",
        "2147483647, 407, 6.70292742211067648162528876066e3, 0",
        "2147483647, 807, 1.27416849603252171413378310472e4, 0",
        "2147483647, 1607, 2.42698285839945068392543390151e4, 0",

        // Maxima cannot handle very large arguments to binomial or beta.
        // fpprec : 64;
        // a(n, k) := bfloat(log(gamma(n+1)) - log(gamma(k+1)) - log(gamma(n-k+1)));
        // a(2147483647, 1073741824);
        "2147483647, 1073741824, 1.488522224247066203747566030677421662382623181089980272736272874e9, 1",
    })
    void testLogBinomialCoefficient(int n, int k, double lognCk, int ulp) {
        assertBinomial(n, k, lognCk, ulp);
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
            } else {
                ulp = 1;
            }
            for (int k = 0; k <= n / 2; k++) {
                final BigInteger nCk = factorials[n].divide(factorials[n - k]).divide(factorials[k]);
                final double expected = nCk.doubleValue();
                // Cannot log infinite result
                if (expected == Double.POSITIVE_INFINITY) {
                    Assertions.fail("Incorrect limit for n: " + size);
                }
                assertBinomial(n, k, Math.log(expected), ulp);
            }
        }
    }

    private static void assertBinomial(int n, int k, double expected, int ulp) {
        final double actual = LogBinomialCoefficient.value(n, k);
        Assertions.assertTrue(Precision.equals(expected, actual, ulp),
            () -> String.format("Log C(%d, %d) = %s : actual %s : ULP error = %d", n, k,
                expected, actual,
                Double.doubleToRawLongBits(actual) - Double.doubleToRawLongBits(expected)));
        // Test symmetry
        Assertions.assertEquals(actual, LogBinomialCoefficient.value(n, n - k), () -> n + " choose " + k);
    }
}
