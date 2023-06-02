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
package org.apache.commons.numbers.examples.jmh.core;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.numbers.examples.jmh.core.DDPerformance.DoubleInt;
import org.apache.commons.numbers.examples.jmh.core.DDPerformance.DoubleIntFunction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests the implementations in {@link DDPerformance}.
 */
class DDPerformanceTest {
    @ParameterizedTest
    @CsvSource({
        "300, 300, 0, 1",
        "1000, 500, 0.001, 0.5",
        //"10000, 50, 0.001, 0.15",
        //"100000, 5, 0.001, 0.05",
    })
    void testKSFunction(int samples, int values, double lx, double ux) {
        final List<DoubleIntFunction> funs = DDPerformance.KSMethod.getImplementations()
            .map(DDPerformance.KSMethod::createFunction)
            .collect(Collectors.toList());
        final DoubleInt[] data = DDPerformance.KSData.createData(samples, values, lx, ux);
        final DoubleIntFunction ref = funs.get(0);
        for (final DoubleInt d : data) {
            final double x = d.getX();
            final int n = d.getN();
            final double expected = ref.apply(x, n);
            for (int i = 1; i < funs.size(); i++) {
                final double actual = funs.get(i).apply(x, n);
                Assertions.assertEquals(expected, actual, () -> String.format("(%s,%s)", x, n));
            }
        }
    }
}
