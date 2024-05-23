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

package org.apache.commons.numbers.examples.jmh.arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link DoubleMath}.
 */
class DoubleMathTest {
    @Test
    void testGreaterThanLessThan() {
        final double[] values = {0.0, 1.0, Double.POSITIVE_INFINITY, Double.NaN};
        final int[] sign = {-1, 1};
        for (final double a : values) {
            for (final double b : values) {
                for (final int i : sign) {
                    final double x = i * a;
                    for (final int j : sign) {
                        final double y = j * b;
                        Assertions.assertEquals(Double.compare(x, y) > 0, DoubleMath.greaterThan(x, y),
                            () -> x + " > " + y);
                        Assertions.assertEquals(Double.compare(x, y) < 0, DoubleMath.lessThan(x, y),
                            () -> x + " < " + y);
                    }
                }
            }
        }
    }
}
