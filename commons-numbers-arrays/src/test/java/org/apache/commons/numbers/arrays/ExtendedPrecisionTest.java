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
package org.apache.commons.numbers.arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for the {@link ExtendedPrecision} class.
 */
class ExtendedPrecisionTest {
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
    void testHighPartUnscaled() {
        Assertions.assertEquals(Double.NaN, ExtendedPrecision.highPartUnscaled(Double.POSITIVE_INFINITY));
        Assertions.assertEquals(Double.NaN, ExtendedPrecision.highPartUnscaled(Double.NEGATIVE_INFINITY));
        Assertions.assertEquals(Double.NaN, ExtendedPrecision.highPartUnscaled(Double.NaN));
        // Large finite numbers will overflow during the split
        Assertions.assertEquals(Double.NaN, ExtendedPrecision.highPartUnscaled(Double.MAX_VALUE));
        Assertions.assertEquals(Double.NaN, ExtendedPrecision.highPartUnscaled(-Double.MAX_VALUE));
    }

    /**
     * Test {@link ExtendedPrecision#productLow(double, double, double)} computes the same
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
        Assertions.assertEquals(expected, ExtendedPrecision.productLow(x, y, x * y), 0.0);
    }

    @Test
    void testIsNotNormal() {
        for (double a : new double[] {Double.MAX_VALUE, 1.0, Double.MIN_NORMAL}) {
            Assertions.assertFalse(ExtendedPrecision.isNotNormal(a));
            Assertions.assertFalse(ExtendedPrecision.isNotNormal(-a));
        }
        for (double a : new double[] {Double.POSITIVE_INFINITY, 0.0,
                                      Math.nextDown(Double.MIN_NORMAL), Double.NaN}) {
            Assertions.assertTrue(ExtendedPrecision.isNotNormal(a));
            Assertions.assertTrue(ExtendedPrecision.isNotNormal(-a));
        }
    }

    /**
     * This demonstrates splitting a sub normal number with no information in the upper 26 bits
     * of the mantissa.
     */
    @Test
    void testSubNormalSplit() {
        final double a = Double.longBitsToDouble(1L << 25);

        // A split using masking of the mantissa bits computes the high part incorrectly
        final double hi1 = Double.longBitsToDouble(Double.doubleToRawLongBits(a) & ((-1L) << 27));
        final double lo1 = a - hi1;
        Assertions.assertEquals(0, hi1);
        Assertions.assertEquals(a, lo1);
        Assertions.assertFalse(Math.abs(hi1) > Math.abs(lo1));

        // Dekker's split
        final double hi2 = ExtendedPrecision.highPartUnscaled(a);
        final double lo2 = a - hi2;
        Assertions.assertEquals(a, hi2);
        Assertions.assertEquals(0, lo2);
        Assertions.assertTrue(Math.abs(hi2) > Math.abs(lo2));
    }
}
