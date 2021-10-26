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
package org.apache.commons.numbers.examples.jmh.gamma;

import java.util.function.DoubleBinaryOperator;
import org.apache.commons.numbers.examples.jmh.gamma.GammaContinuedFractionPerformance.BaseData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests the continued fraction implementations in {@link GammaContinuedFractionPerformance}.
 */
class GammaContinuedFractionTest {

    @ParameterizedTest
    @ValueSource(strings = {
        GammaContinuedFractionPerformance.IMP_NUMBERS_1_0,
        GammaContinuedFractionPerformance.IMP_NUMBERS_EXT_A,
        GammaContinuedFractionPerformance.IMP_NUMBERS_EXT_A1,
        GammaContinuedFractionPerformance.IMP_ITERATOR,
        GammaContinuedFractionPerformance.IMP_NUMBERS_1_1_INC,
    })
    void testContinuedFraction(String implementation) {
        // In the absence of expected results test that all implementations
        // return the same value. Use the Numbers 1.1 version as the reference.
        final DoubleBinaryOperator f1 = BaseData.createFunction(GammaContinuedFractionPerformance.IMP_NUMBERS_1_1);
        final DoubleBinaryOperator f2 = BaseData.createFunction(implementation);
        for (final double[] pair : GammaContinuedFractionPerformance.getData()) {
            final double a = pair[0];
            final double z = pair[1];
            final double expected = f1.applyAsDouble(a, z);
            final double actual = f2.applyAsDouble(a, z);
            Assertions.assertEquals(expected, actual, Math.abs(1e-14 * expected),
                () -> String.format("(%s,%s)", a, z));
        }
    }
}
