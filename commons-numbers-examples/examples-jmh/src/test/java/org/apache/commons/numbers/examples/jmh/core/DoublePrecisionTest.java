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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DoublePrecision}.
 */
class DoublePrecisionTest {
    @Test
    void testSplitAssumptions() {
        // The multiplier used to split the double value into high and low parts.
        final double scale = (1 << 27) + 1;
        // The upper limit above which a number may overflow during the split into a high part.
        final double limit = 0x1.0p996;
        Assertions.assertTrue(Double.isFinite(limit * scale));
        Assertions.assertTrue(Double.isFinite(-limit * scale));
        // Cannot make the limit the next power up
        Assertions.assertEquals(Double.POSITIVE_INFINITY, limit * 2 * scale);
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, -limit * 2 * scale);
        // Check the level for the safe upper limit of the exponent of the sum of the absolute
        // components of the product
        Assertions.assertTrue(Math.getExponent(2 * Math.sqrt(Double.MAX_VALUE)) - 2 > 508);
    }

    @Test
    void testHighPart() {
        Assertions.assertEquals(Double.NaN, DoublePrecision.highPart(Double.POSITIVE_INFINITY));
        Assertions.assertEquals(Double.NaN, DoublePrecision.highPart(Double.NEGATIVE_INFINITY));
        Assertions.assertEquals(Double.NaN, DoublePrecision.highPart(Double.NaN));
        // Any finite number should be split to a finite number
        Assertions.assertTrue(Double.isFinite(DoublePrecision.highPart(Double.MAX_VALUE)));
        Assertions.assertTrue(Double.isFinite(DoublePrecision.highPart(-Double.MAX_VALUE)));
    }

    @Test
    void testHighPartUnscaled() {
        Assertions.assertEquals(Double.NaN, DoublePrecision.highPartUnscaled(Double.POSITIVE_INFINITY));
        Assertions.assertEquals(Double.NaN, DoublePrecision.highPartUnscaled(Double.NEGATIVE_INFINITY));
        Assertions.assertEquals(Double.NaN, DoublePrecision.highPartUnscaled(Double.NaN));
        // Large finite numbers will overflow during the split
        Assertions.assertEquals(Double.NaN, DoublePrecision.highPartUnscaled(Double.MAX_VALUE));
        Assertions.assertEquals(Double.NaN, DoublePrecision.highPartUnscaled(-Double.MAX_VALUE));
    }

    /**
     * Test {@link DoublePrecision#productLow(double, double, double)} computes the same
     * result as JDK 9 Math.fma(x, y, -x * y) for edge cases.
     */
    @Test
    void testProductLow() {
        assertProductLow(0.0, 1.0, Math.nextDown(Double.MIN_NORMAL));
        assertProductLow(0.0, -1.0, Math.nextDown(Double.MIN_NORMAL));
        assertProductLow(Double.NaN, 1.0, Double.POSITIVE_INFINITY);
        assertProductLow(Double.NaN, 1.0, Double.NEGATIVE_INFINITY);
        assertProductLow(Double.NaN, 1.0, Double.NaN);
        assertProductLow(0.0, 1.0, Double.MAX_VALUE);
        assertProductLow(Double.NaN, 2.0, Double.MAX_VALUE);
    }

    private static void assertProductLow(double expected, double x, double y) {
        // Requires a delta of 0.0 to assert -0.0 == 0.0
        Assertions.assertEquals(expected, DoublePrecision.productLow(x, y, x * y), 0.0);
    }

    @Test
    void testIsNotNormal() {
        for (double a : new double[] {Double.MAX_VALUE, 1.0, Double.MIN_NORMAL}) {
            Assertions.assertFalse(DoublePrecision.isNotNormal(a));
            Assertions.assertFalse(DoublePrecision.isNotNormal(-a));
        }
        for (double a : new double[] {Double.POSITIVE_INFINITY, 0.0,
                                      Math.nextDown(Double.MIN_NORMAL), Double.NaN}) {
            Assertions.assertTrue(DoublePrecision.isNotNormal(a));
            Assertions.assertTrue(DoublePrecision.isNotNormal(-a));
        }
    }
}
