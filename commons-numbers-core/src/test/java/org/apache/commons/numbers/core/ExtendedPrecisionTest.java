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
package org.apache.commons.numbers.core;

import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
        Assertions.assertEquals(Double.NaN, DD.highPart(Double.POSITIVE_INFINITY));
        Assertions.assertEquals(Double.NaN, DD.highPart(Double.NEGATIVE_INFINITY));
        Assertions.assertEquals(Double.NaN, DD.highPart(Double.NaN));
        // Large finite numbers will overflow during the split
        Assertions.assertEquals(Double.NaN, DD.highPart(Double.MAX_VALUE));
        Assertions.assertEquals(Double.NaN, DD.highPart(-Double.MAX_VALUE));
    }

    /**
     * Test {@link ExtendedPrecision#productLow(double, double, double)} computes the same
     * result as JDK 9 Math.fma(x, y, -x * y) for edge cases, e.g.
     * <pre>
     * jshell> static double f(double a, double b) { return Math.fma(x, y, -x * y); }
     * jshell> f(5.6266120027810604E-148, 2.9150607442566245E-154);
     * </pre>
     */
    @Test
    void testProductLowEdgeCases() {
        assertProductLow(0.0, 1.0, Math.nextDown(Double.MIN_NORMAL));
        assertProductLow(0.0, -1.0, Math.nextDown(Double.MIN_NORMAL));
        assertProductLow(Double.NaN, 1.0, Double.POSITIVE_INFINITY);
        assertProductLow(Double.NaN, 1.0, Double.NEGATIVE_INFINITY);
        assertProductLow(Double.NaN, 1.0, Double.NaN);
        assertProductLow(0.0, 1.0, Double.MAX_VALUE);
        assertProductLow(Double.NaN, 2.0, Double.MAX_VALUE);
        // Product is normal, round-off is sub-normal
        // Ignoring sub-normals during computation results in a rounding error
        assertProductLow(-0.0, -2.73551683292218E-154, -1.0861547555023299E-154);
        assertProductLow(9.023244E-318, 5.6266120027810604E-148, 2.9150607442566245E-154);
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
        final double hi2 = DD.highPart(a);
        final double lo2 = a - hi2;
        Assertions.assertEquals(a, hi2);
        Assertions.assertEquals(0, lo2);

        Assertions.assertTrue(Math.abs(hi2) > Math.abs(lo2));
    }

    @ParameterizedTest
    @MethodSource
    void testProductLow(double x, double y) {
        // Assumes the arguments are in [1, 2) so scaling factors hit target cases
        Assertions.assertTrue(Math.abs(x) >= 1 && Math.abs(x) < 2, () -> "Invalid x: " + x);
        Assertions.assertTrue(Math.abs(y) >= 1 && Math.abs(y) < 2, () -> "Invalid y: " + y);

        final double low = DD.ofProduct(x, y).lo();
        assertProductLow(low, x, y);

        // Product approaching and into sub-normal
        final int[] scaleDown = {-490, -510, -511, -512, -513};
        for (final int e1 : scaleDown) {
            final double a = Math.scalb(x, e1);
            for (final int e2 : scaleDown) {
                final double b = Math.scalb(y, e2);
                final double expected = Math.scalb(low, e1 + e2);
                assertProductLow(expected, a, b, () -> "Product towards sub-normal");
            }
        }

        // Product approaching overflow
        final int[] scaleUp = {509, 510, 511, 512};
        for (final int e1 : scaleUp) {
            final double a = Math.scalb(x, e1);
            for (final int e2 : scaleUp) {
                final double b = Math.scalb(y, e2);
                if (Double.isFinite(a * b)) {
                    final double expected = Math.scalb(low, e1 + e2);
                    assertProductLow(expected, a, b, () -> "Product towards overflow");
                }
            }
        }

        // Split of an argument approaching overflow
        final int[] scaleUp2 = {990, 1000, 1010};
        for (final int e : scaleUp2) {
            final double a = Math.scalb(x, e);
            final double expected = Math.scalb(low, e);
            assertProductLow(expected, a, y, () -> "Argument x split towards overflow");
            assertProductLow(expected, y, a, () -> "Argument y split towards overflow");
        }
    }

    static Stream<Arguments> testProductLow() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        final int n = 50;
        // Generate arguments in [1, 2)
        for (int i = 0; i < n; i++) {
            builder.add(Arguments.of(DoubleTestUtils.randomDouble(rng),
                                     DoubleTestUtils.randomDouble(rng)));
        }
        return builder.build();
    }

    private static void assertProductLow(double expected, double x, double y) {
        assertProductLow(expected, x, y, null);
    }

    private static void assertProductLow(double expected, double x, double y, Supplier<String> msg) {
        // Allowed ULP=0. This allows -0.0 and 0.0 to be equal.
        TestUtils.assertEquals(expected, ExtendedPrecision.productLow(x, y, x * y), 0,
            () -> TestUtils.prefix(msg) + "low(" + x + " * " + y  + ")");
    }
}
