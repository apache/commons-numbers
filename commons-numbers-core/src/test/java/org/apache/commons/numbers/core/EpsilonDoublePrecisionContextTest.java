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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EpsilonDoublePrecisionContextTest {

    @Test
    public void testGetters() {
        // arrange
        final double eps = 1e-6;

        // act
        final EpsilonDoublePrecisionContext ctx = new EpsilonDoublePrecisionContext(eps);

        // assert
        Assertions.assertEquals(eps, ctx.getEpsilon(), 0.0);
        Assertions.assertEquals(eps, ctx.getMaxZero(), 0.0);
    }

    @Test
    public void testInvalidEpsilonValues() {
        // act/assert
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EpsilonDoublePrecisionContext(-1.0));

        String msg;

        msg = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new EpsilonDoublePrecisionContext(Double.NaN)).getMessage();
        Assertions.assertEquals(msg, "Invalid epsilon value: NaN");

        msg = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new EpsilonDoublePrecisionContext(Double.POSITIVE_INFINITY)).getMessage();
        Assertions.assertEquals(msg, "Invalid epsilon value: Infinity");

        msg = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new EpsilonDoublePrecisionContext(Double.NEGATIVE_INFINITY)).getMessage();
        Assertions.assertEquals(msg, "Invalid epsilon value: -Infinity");
    }

    @Test
    public void testSign() {
        // arrange
        final double eps = 1e-2;

        final EpsilonDoublePrecisionContext ctx = new EpsilonDoublePrecisionContext(eps);

        // act/assert
        Assertions.assertEquals(0, ctx.sign(0.0));
        Assertions.assertEquals(0, ctx.sign(-0.0));

        Assertions.assertEquals(0, ctx.sign(1e-2));
        Assertions.assertEquals(0, ctx.sign(-1e-2));

        Assertions.assertEquals(1, ctx.sign(1e-1));
        Assertions.assertEquals(-1, ctx.sign(-1e-1));

        Assertions.assertEquals(1, ctx.sign(Double.NaN));
        Assertions.assertEquals(1, ctx.sign(Double.POSITIVE_INFINITY));
        Assertions.assertEquals(-1, ctx.sign(Double.NEGATIVE_INFINITY));
    }

    @Test
    public void testCompare_compareToZero() {
        // arrange
        final double eps = 1e-2;

        final EpsilonDoublePrecisionContext ctx = new EpsilonDoublePrecisionContext(eps);

        // act/assert
        Assertions.assertEquals(0, ctx.compare(0.0, 0.0));
        Assertions.assertEquals(0, ctx.compare(+0.0, -0.0));
        Assertions.assertEquals(0, ctx.compare(eps, -0.0));
        Assertions.assertEquals(0, ctx.compare(+0.0, eps));

        Assertions.assertEquals(0, ctx.compare(-eps, -0.0));
        Assertions.assertEquals(0, ctx.compare(+0.0, -eps));

        Assertions.assertEquals(-1, ctx.compare(0.0, 1.0));
        Assertions.assertEquals(1, ctx.compare(1.0, 0.0));

        Assertions.assertEquals(1, ctx.compare(0.0, -1.0));
        Assertions.assertEquals(-1, ctx.compare(-1.0, 0.0));
    }

    @Test
    public void testCompare_compareNonZero() {
        // arrange
        final double eps = 1e-5;
        final double small = 1e-3;
        final double big = 1e100;

        final EpsilonDoublePrecisionContext ctx = new EpsilonDoublePrecisionContext(eps);

        // act/assert
        Assertions.assertEquals(0, ctx.compare(eps, 2 * eps));
        Assertions.assertEquals(0, ctx.compare(-2 * eps, -eps));

        Assertions.assertEquals(0, ctx.compare(small, small + (0.9 * eps)));
        Assertions.assertEquals(0, ctx.compare(-small - (0.9 * eps), -small));

        Assertions.assertEquals(0, ctx.compare(big, nextUp(big, 1)));
        Assertions.assertEquals(0, ctx.compare(nextDown(-big, 1), -big));

        Assertions.assertEquals(-1, ctx.compare(small, small + (1.1 * eps)));
        Assertions.assertEquals(1, ctx.compare(-small, -small - (1.1 * eps)));

        Assertions.assertEquals(-1, ctx.compare(big, nextUp(big, 2)));
        Assertions.assertEquals(1, ctx.compare(-big, nextDown(-big, 2)));
    }

    @Test
    public void testCompare_NaN() {
        // arrange
        final EpsilonDoublePrecisionContext ctx = new EpsilonDoublePrecisionContext(1e-6);

        // act/assert
        Assertions.assertEquals(1, ctx.compare(0, Double.NaN));
        Assertions.assertEquals(1, ctx.compare(Double.NaN, 0));
        Assertions.assertEquals(1, ctx.compare(Double.NaN, Double.NaN));

        Assertions.assertEquals(1, ctx.compare(Double.POSITIVE_INFINITY, Double.NaN));
        Assertions.assertEquals(1, ctx.compare(Double.NaN, Double.POSITIVE_INFINITY));

        Assertions.assertEquals(1, ctx.compare(Double.NEGATIVE_INFINITY, Double.NaN));
        Assertions.assertEquals(1, ctx.compare(Double.NaN, Double.NEGATIVE_INFINITY));
    }

    @Test
    public void testCompare_infinity() {
        // arrange
        final EpsilonDoublePrecisionContext ctx = new EpsilonDoublePrecisionContext(1e-6);

        // act/assert
        Assertions.assertEquals(-1, ctx.compare(0, Double.POSITIVE_INFINITY));
        Assertions.assertEquals(1, ctx.compare(Double.POSITIVE_INFINITY, 0));
        Assertions.assertEquals(0, ctx.compare(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));

        Assertions.assertEquals(1, ctx.compare(0, Double.NEGATIVE_INFINITY));
        Assertions.assertEquals(-1, ctx.compare(Double.NEGATIVE_INFINITY, 0));
        Assertions.assertEquals(0, ctx.compare(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
    }

    @Test
    public void testGetMaxZero_isZeroEqualityThreshold() {
        // arrange
        final double eps = 1e-2;

        final EpsilonDoublePrecisionContext ctx = new EpsilonDoublePrecisionContext(eps);

        final double maxZero = ctx.getMaxZero();

        // act/assert
        Assertions.assertTrue(ctx.eqZero(maxZero));
        Assertions.assertTrue(ctx.eqZero(nextDown(maxZero, 1)));
        Assertions.assertFalse(ctx.eqZero(nextUp(maxZero, 1)));

        Assertions.assertTrue(ctx.eqZero(-maxZero));
        Assertions.assertTrue(ctx.eqZero(nextUp(-maxZero, 1)));
        Assertions.assertFalse(ctx.eqZero(nextDown(-maxZero, 1)));
    }

    @Test
    public void testHashCode() {
        // arrange
        final EpsilonDoublePrecisionContext a = new EpsilonDoublePrecisionContext(1e-6);
        final EpsilonDoublePrecisionContext b = new EpsilonDoublePrecisionContext(1e-7);
        final EpsilonDoublePrecisionContext c = new EpsilonDoublePrecisionContext(1e-6);

        // act/assert
        Assertions.assertEquals(a.hashCode(), a.hashCode());
        Assertions.assertEquals(a.hashCode(), c.hashCode());

        Assertions.assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testEquals() {
        // arrange
        final EpsilonDoublePrecisionContext a = new EpsilonDoublePrecisionContext(1e-6);
        final EpsilonDoublePrecisionContext b = new EpsilonDoublePrecisionContext(1e-7);
        final EpsilonDoublePrecisionContext c = new EpsilonDoublePrecisionContext(1e-6);

        // act/assert
        Assertions.assertFalse(a.equals(null));
        Assertions.assertFalse(a.equals(new Object()));

        Assertions.assertTrue(a.equals(a));

        Assertions.assertFalse(a.equals(b));
        Assertions.assertFalse(b.equals(a));

        Assertions.assertTrue(a.equals(c));
    }

    @Test
    public void testEqualsAndHashCode_signedZeroConsistency() {
        // arrange
        final EpsilonDoublePrecisionContext a = new EpsilonDoublePrecisionContext(0.0);
        final EpsilonDoublePrecisionContext b = new EpsilonDoublePrecisionContext(-0.0);
        final EpsilonDoublePrecisionContext c = new EpsilonDoublePrecisionContext(0.0);
        final EpsilonDoublePrecisionContext d = new EpsilonDoublePrecisionContext(-0.0);

        // act/assert
        Assertions.assertFalse(a.equals(b));
        Assertions.assertNotEquals(a.hashCode(), b.hashCode());

        Assertions.assertTrue(a.equals(c));
        Assertions.assertEquals(a.hashCode(), c.hashCode());

        Assertions.assertTrue(b.equals(d));
        Assertions.assertEquals(b.hashCode(), d.hashCode());
    }

    @Test
    public void testToString() {
        // arrange
        final EpsilonDoublePrecisionContext a = new EpsilonDoublePrecisionContext(1d);

        // act
        final String str = a.toString();

        // assert
        Assertions.assertTrue(str.contains("EpsilonDoublePrecisionContext"));
        Assertions.assertTrue(str.contains("epsilon= 1"));
    }

    /**
     * Increments the given double value {@code count} number of times
     * using {@link Math#nextUp(double)}.
     * @param n
     * @param count
     * @return
     */
    private static double nextUp(final double n, final int count) {
        double result = n;
        for (int i = 0; i < count; ++i) {
            result = Math.nextUp(result);
        }

        return result;
    }

    /**
     * Decrements the given double value {@code count} number of times
     * using {@link Math#nextDown(double)}.
     * @param n
     * @param count
     * @return
     */
    private static double nextDown(final double n, final int count) {
        double result = n;
        for (int i = 0; i < count; ++i) {
            result = Math.nextDown(result);
        }

        return result;
    }
}
