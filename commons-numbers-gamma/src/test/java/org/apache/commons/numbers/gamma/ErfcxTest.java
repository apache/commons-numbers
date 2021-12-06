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
package org.apache.commons.numbers.gamma;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link Erfcx}.
 */
class ErfcxTest {
    @ParameterizedTest
    @CsvSource({
        "NaN, NaN",
        "0, 1",
        "Infinity, 0",
        "-27, Infinity",
    })
    void testErfcxEdgeCases(double z, double expected) {
        Assertions.assertEquals(expected, Erfcx.value(z));
    }

    /**
     * Compare erfcx against reference values computed using matlab.
     */
    @ParameterizedTest
    @CsvSource({
        // Positives
        "0.123, 0.87504503469728478",
        "0.356, 0.69768949677109249",
        "0.598, 0.56869994421241077",
        "1.12, 0.39689157691768223",
        "2.54, 0.20787186512579933",
        "4.78, 0.11560177456745414",
        "8.89, 0.06306928075747556",
        "16.133, 0.034904353289554449",
        "25.677, 0.021955940669412959",
        "32.123, 0.017554917112870515",
        "64.156, 0.0087929589162697615",
        "128.1256, 0.00440327616930610247",
        "256.245, 0.0022017416435368476",
        "512.123, 0.0011016660215700644",
        "1024.1211, 0.0005509009770282079",
        "1.87e5, 3.0170565964689267e-06",
        "1.23e6, 4.5869071833135764e-07",
        "6.23e7, 9.0560125770105324e-09",
        "2.34e8, 2.4110665963579326e-09",
        "1.7976931348623157E308, 3.1384087339854447e-309",
        // Negatives require accurate computation of exp(z^2)
        "-0.123, 1.1554430105980469",
        "-0.356, 1.5725451177335525",
        "-0.598, 2.2910985922662546",
        "-1.12, 6.6145770282875613",
        "-2.54, 1267.2229816784452",
        "-4.78, 16748041793.844347",
        "-8.89, 4.2095426970353675e+34",
        "-16.133, 2.1699859052022138e+113",
        "-25.677, 4.3151823545771249e+286",
    })
    void testErfcx(double z, double expected) {
        Assertions.assertEquals(expected, Erfcx.value(z), Math.ulp(expected));
    }
}
