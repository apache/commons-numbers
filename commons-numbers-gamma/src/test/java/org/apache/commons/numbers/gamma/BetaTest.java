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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

/**
 * Tests for {@link Beta}.
 *
 * <p>The class directly calls the methods in {@link BoostBeta}. This test ensures
 * the arguments are passed through correctly. Accuracy of the function is tested
 * in {@link BoostBetaTest}.
 */
class BetaTest {
    @ParameterizedTest
    @CsvFileSource(resources = "beta_med_data.csv")
    void testBeta(double a, double b, double beta) {
        TestUtils.assertEquals(beta, Beta.value(a, b), 200);
    }
}
