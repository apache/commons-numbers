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

import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TestUtils}.
 * This verifies the custom ULP assertions function as expected.
 */
class TestUtilsTest {
    @Test
    void testAssertEquals() {
        final double x = 1.23;
        final double ulp = Math.ulp(x);
        Assertions.assertEquals(0, TestUtils.assertEquals(x, x, 0));
        Assertions.assertEquals(0, TestUtils.assertEquals(x, x, 1));
        Assertions.assertEquals(0, TestUtils.assertEquals(x, x, 30));
        Assertions.assertThrows(AssertionError.class, () -> TestUtils.assertEquals(x, x + ulp, 0));
        Assertions.assertEquals(1, TestUtils.assertEquals(x, x + ulp, 1));
        Assertions.assertEquals(1, TestUtils.assertEquals(x, x - ulp, 1));
        Assertions.assertEquals(1, TestUtils.assertEquals(x, x + ulp, 30));
        Assertions.assertThrows(AssertionError.class, () -> TestUtils.assertEquals(x, x + 30 * ulp, 29));
        Assertions.assertEquals(30, TestUtils.assertEquals(x, x + 30 * ulp, 30));
        Assertions.assertEquals(30, TestUtils.assertEquals(x, x - 30 * ulp, 30));

        // Check order and sign
        Assertions.assertThrows(AssertionError.class, () -> TestUtils.assertEquals(-x - ulp, x, 0));
        Assertions.assertEquals(1, TestUtils.assertEquals(-x - ulp, -x, 1));
        Assertions.assertEquals(1, TestUtils.assertEquals(x - ulp, x, 1));
        Assertions.assertEquals(2, TestUtils.assertEquals(x - ulp, x + ulp, 2));

        // Opposite signs
        Assertions.assertThrows(AssertionError.class, () -> TestUtils.assertEquals(-x, x, 500000));
    }

    @Test
    void testAssertEqualsZero() {
        // These are not binary equal but for numeric purposes they are treated as equal
        final double[] zero = {-0.0, 0.0};
        for (final double a : zero) {
            for (final double b : zero) {
                Assertions.assertEquals(0, TestUtils.assertEquals(a, b, 0));
                Assertions.assertEquals(0, TestUtils.assertEquals(a, b, 1));
                Assertions.assertEquals(0, TestUtils.assertEquals(a, b, 30));
            }
        }

        // Difference from zero
        final double x = Double.MIN_VALUE;
        Assertions.assertThrows(AssertionError.class, () -> TestUtils.assertEquals(0.0, x, 0));
        Assertions.assertEquals(1, TestUtils.assertEquals(0.0, x, 1));
        Assertions.assertEquals(1, TestUtils.assertEquals(-0.0, x, 1));
        Assertions.assertEquals(1, TestUtils.assertEquals(0.0, -x, 1));
        Assertions.assertEquals(1, TestUtils.assertEquals(-0.0, -x, 1));
        Assertions.assertEquals(2, TestUtils.assertEquals(0.0, 2 * x, 2));
        Assertions.assertEquals(2, TestUtils.assertEquals(-0.0, 2 * x, 2));
    }

    @Test
    void testAssertEqualsEdgeCases() {
        final double nan = Double.NaN;
        final double inf = Double.POSITIVE_INFINITY;
        final double max = Double.MAX_VALUE;
        Assertions.assertEquals(0, TestUtils.assertEquals(nan, nan, 0));
        Assertions.assertEquals(0, TestUtils.assertEquals(inf, inf, 0));
        Assertions.assertEquals(0, TestUtils.assertEquals(max, max, 0));

        Assertions.assertThrows(AssertionError.class, () -> TestUtils.assertEquals(nan, inf, 0));
        Assertions.assertThrows(AssertionError.class, () -> TestUtils.assertEquals(nan, max, 0));
        Assertions.assertThrows(AssertionError.class, () -> TestUtils.assertEquals(inf, max, 0));

        Assertions.assertEquals(1, TestUtils.assertEquals(inf, max, 1));
        Assertions.assertThrows(AssertionError.class, () -> TestUtils.assertEquals(inf, -inf, 0));
    }

    @Test
    void testBigDecimalAssertEquals() {
        final double x = 1.23;
        final double ulp = Math.ulp(x);

        final BigDecimal bdx = new BigDecimal(x);

        Assertions.assertEquals(0, TestUtils.assertEquals(bdx, x, 0));
        Assertions.assertEquals(0, TestUtils.assertEquals(bdx, x, 1));
        Assertions.assertEquals(0, TestUtils.assertEquals(bdx, x, 30));
        Assertions.assertThrows(AssertionError.class, () -> TestUtils.assertEquals(bdx, x + ulp, 0));
        Assertions.assertEquals(1, TestUtils.assertEquals(bdx, x + ulp, 1));
        Assertions.assertEquals(1, TestUtils.assertEquals(bdx, x - ulp, 1));
        Assertions.assertEquals(1, TestUtils.assertEquals(bdx, x + ulp, 30));
        Assertions.assertThrows(AssertionError.class, () -> TestUtils.assertEquals(bdx, x + 30 * ulp, 29));
        Assertions.assertEquals(30, TestUtils.assertEquals(bdx, x + 30 * ulp, 30));
        Assertions.assertEquals(30, TestUtils.assertEquals(bdx, x - 30 * ulp, 30));

        // Opposite signs
        Assertions.assertThrows(AssertionError.class, () -> TestUtils.assertEquals(bdx.negate(), x, 500000));

        // High precision: Add 1/4 ulp to the value:
        //    ---|----------|----------|-----
        //       -1         x  |       +1
        //                     y
        final BigDecimal bdy = bdx.add(new BigDecimal(ulp).divide(BigDecimal.valueOf(4)));
        Assertions.assertEquals(0, TestUtils.assertEquals(bdy, x, 0));
        Assertions.assertThrows(AssertionError.class, () -> TestUtils.assertEquals(bdy, x + ulp, 0));
        Assertions.assertEquals(0.75, TestUtils.assertEquals(bdy, x + ulp, 1));
        Assertions.assertThrows(AssertionError.class, () -> TestUtils.assertEquals(bdy, x - ulp, 0));
        Assertions.assertThrows(AssertionError.class, () -> TestUtils.assertEquals(bdy, x - ulp, 1));
        Assertions.assertEquals(1.25, TestUtils.assertEquals(bdy, x - ulp, 1.5), 1e-16);
    }

    @Test
    void testBigDecimalAssertEqualsZero() {
        // These are not binary equal but for numeric purposes they are treated as equal
        final double[] zero = {-0.0, 0.0};
        final BigDecimal a = BigDecimal.ZERO;
        for (final double b : zero) {
            Assertions.assertEquals(0, TestUtils.assertEquals(a, b, 0));
            Assertions.assertEquals(0, TestUtils.assertEquals(a, b, 1));
            Assertions.assertEquals(0, TestUtils.assertEquals(a, b, 30));
        }

        // Difference from zero
        final double x = Double.MIN_VALUE;
        Assertions.assertThrows(AssertionError.class, () -> TestUtils.assertEquals(a, x, 0));
        Assertions.assertEquals(1, TestUtils.assertEquals(a, x, 1));
        Assertions.assertEquals(1, TestUtils.assertEquals(a, -x, 1));
        Assertions.assertEquals(2, TestUtils.assertEquals(a, 2 * x, 2));
        Assertions.assertEquals(2, TestUtils.assertEquals(a, -2 * x, 2));
    }

    @Test
    void testBigDecimalAssertEqualsEdgeCases() {
        final double nan = Double.NaN;
        final double inf = Double.POSITIVE_INFINITY;
        final double max = Double.MAX_VALUE;

        final BigDecimal bdinf = new BigDecimal("1e310");
        final BigDecimal bdmax = new BigDecimal(max);

        Assertions.assertEquals(0, TestUtils.assertEquals(bdinf, inf, 0));
        Assertions.assertEquals(0, TestUtils.assertEquals(bdmax, max, 0));

        Assertions.assertThrows(AssertionError.class, () -> TestUtils.assertEquals(bdinf, max, 0));
        Assertions.assertThrows(AssertionError.class, () -> TestUtils.assertEquals(bdinf, nan, 0));
        Assertions.assertThrows(AssertionError.class, () -> TestUtils.assertEquals(bdmax, inf, 0));
        Assertions.assertThrows(AssertionError.class, () -> TestUtils.assertEquals(bdmax, nan, 0));

        final double x = Math.nextDown(max);
        Assertions.assertThrows(AssertionError.class, () -> TestUtils.assertEquals(bdmax, x, 0));
        Assertions.assertEquals(1, TestUtils.assertEquals(bdmax, x, 1));
    }
}
