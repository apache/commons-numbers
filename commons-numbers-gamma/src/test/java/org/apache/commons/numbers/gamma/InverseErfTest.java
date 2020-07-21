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
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link InverseErf}.
 */
class InverseErfTest {
    @Test
    void testErfInvNaN() {
        Assertions.assertTrue(Double.isNaN(InverseErf.value(-1.001)));
        Assertions.assertTrue(Double.isNaN(InverseErf.value(+1.001)));
        Assertions.assertTrue(Double.isNaN(InverseErf.value(Double.NaN)));
    }

    @Test
    void testErfInvInfinite() {
        Assertions.assertTrue(Double.isInfinite(InverseErf.value(-1)));
        Assertions.assertTrue(InverseErf.value(-1) < 0);
        Assertions.assertTrue(Double.isInfinite(InverseErf.value(+1)));
        Assertions.assertTrue(InverseErf.value(+1) > 0);
    }

    @Test
    void testErfInv() {
        for (double x = -5.9; x < 5.9; x += 0.01) {
            final double y = Erf.value(x);
            final double dydx = 2 * Math.exp(-x * x) / Math.sqrt(Math.PI);
            Assertions.assertEquals(x, InverseErf.value(y), 1.0e-15 / dydx);
        }
    }
}
