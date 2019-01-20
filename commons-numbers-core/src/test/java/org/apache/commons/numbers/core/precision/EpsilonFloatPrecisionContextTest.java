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
package org.apache.commons.numbers.core.precision;

import org.apache.commons.numbers.core.precision.EpsilonFloatPrecisionContext;
import org.junit.Assert;
import org.junit.Test;

public class EpsilonFloatPrecisionContextTest {

    @Test
    public void testGetters() {
        // arrange
        float eps = 1e-6f;

        // act
        EpsilonFloatPrecisionContext ctx = new EpsilonFloatPrecisionContext(eps);

        // assert
        Assert.assertEquals(ctx.getEpsilon(), eps, 0f);
        Assert.assertEquals(ctx.getMaxZero(), eps, 0f);
    }

    @Test
    public void testCompare_compareToZero() {
        // arrange
        float eps = 1e-2f;

        EpsilonFloatPrecisionContext ctx = new EpsilonFloatPrecisionContext(eps);

        // act/assert
        Assert.assertEquals(0, ctx.compare(0f, 0f));
        Assert.assertEquals(0, ctx.compare(+0f, -0f));
        Assert.assertEquals(0, ctx.compare(eps, -0f));
        Assert.assertEquals(0, ctx.compare(+0f, eps));

        Assert.assertEquals(0, ctx.compare(-eps, -0f));
        Assert.assertEquals(0, ctx.compare(+0f, -eps));

        Assert.assertEquals(-1, ctx.compare(0f, 1f));
        Assert.assertEquals(1, ctx.compare(1f, 0f));

        Assert.assertEquals(1, ctx.compare(0f, -1f));
        Assert.assertEquals(-1, ctx.compare(-1f, 0f));
    }

    @Test
    public void testCompare_compareNonZero() {
        // arrange
        float eps = 1e-10f;
        float small = 1e-5f;
        float big = 1e30f;

        EpsilonFloatPrecisionContext ctx = new EpsilonFloatPrecisionContext(eps);

        // act/assert
        Assert.assertEquals(0, ctx.compare(eps, 2 * eps));
        Assert.assertEquals(0, ctx.compare(-2 * eps, -eps));

        Assert.assertEquals(0, ctx.compare(small, small + (0.9f * eps)));
        Assert.assertEquals(0, ctx.compare(-small - (0.9f * eps), -small));

        Assert.assertEquals(0, ctx.compare(big, nextUp(big, 1)));
        Assert.assertEquals(0, ctx.compare(nextDown(-big, 1), -big));

        Assert.assertEquals(-1, ctx.compare(small, small + (1.1f * eps)));
        Assert.assertEquals(1, ctx.compare(-small, -small - (1.1f * eps)));

        Assert.assertEquals(-1, ctx.compare(big, nextUp(big, 2)));
        Assert.assertEquals(1, ctx.compare(-big, nextDown(-big, 2)));
    }

    @Test
    public void testCompare_NaN() {
        // arrange
        EpsilonFloatPrecisionContext ctx = new EpsilonFloatPrecisionContext(1e-6f);

        // act/assert
        Assert.assertEquals(1, ctx.compare(0, Float.NaN));
        Assert.assertEquals(1, ctx.compare(Float.NaN, 0));
        Assert.assertEquals(1, ctx.compare(Float.NaN, Float.NaN));

        Assert.assertEquals(1, ctx.compare(Float.POSITIVE_INFINITY, Float.NaN));
        Assert.assertEquals(1, ctx.compare(Float.NaN, Float.POSITIVE_INFINITY));

        Assert.assertEquals(1, ctx.compare(Float.NEGATIVE_INFINITY, Float.NaN));
        Assert.assertEquals(1, ctx.compare(Float.NaN, Float.NEGATIVE_INFINITY));
    }

    @Test
    public void testCompare_infinity() {
        // arrange
        EpsilonFloatPrecisionContext ctx = new EpsilonFloatPrecisionContext(1e-6f);

        // act/assert
        Assert.assertEquals(-1, ctx.compare(0, Float.POSITIVE_INFINITY));
        Assert.assertEquals(1, ctx.compare(Float.POSITIVE_INFINITY, 0));
        Assert.assertEquals(0, ctx.compare(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));

        Assert.assertEquals(1, ctx.compare(0, Float.NEGATIVE_INFINITY));
        Assert.assertEquals(-1, ctx.compare(Float.NEGATIVE_INFINITY, 0));
        Assert.assertEquals(0, ctx.compare(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY));
    }

    @Test
    public void testGetMaxZero_isZeroEqualityThreshold() {
        // arrange
        float eps = 1e-2f;

        EpsilonFloatPrecisionContext ctx = new EpsilonFloatPrecisionContext(eps);

        float maxZero = ctx.getMaxZero();

        // act/assert
        Assert.assertTrue(ctx.isZero(maxZero));
        Assert.assertTrue(ctx.isZero(nextDown(maxZero, 1)));
        Assert.assertFalse(ctx.isZero(nextUp(maxZero, 1)));

        Assert.assertTrue(ctx.isZero(-maxZero));
        Assert.assertTrue(ctx.isZero(nextUp(-maxZero, 1)));
        Assert.assertFalse(ctx.isZero(nextDown(-maxZero, 1)));
    }

    @Test
    public void testHashCode() {
        // arrange
        EpsilonFloatPrecisionContext a = new EpsilonFloatPrecisionContext(1e-6f);
        EpsilonFloatPrecisionContext b = new EpsilonFloatPrecisionContext(1e-7f);
        EpsilonFloatPrecisionContext c = new EpsilonFloatPrecisionContext(1e-6f);

        // act/assert
        Assert.assertEquals(a.hashCode(), a.hashCode());
        Assert.assertEquals(a.hashCode(), c.hashCode());

        Assert.assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testEquals() {
        // arrange
        EpsilonFloatPrecisionContext a = new EpsilonFloatPrecisionContext(1e-6f);
        EpsilonFloatPrecisionContext b = new EpsilonFloatPrecisionContext(1e-7f);
        EpsilonFloatPrecisionContext c = new EpsilonFloatPrecisionContext(1e-6f);

        // act/assert
        Assert.assertFalse(a.equals(null));
        Assert.assertFalse(a.equals(new Object()));
        Assert.assertFalse(a.equals(b));
        Assert.assertFalse(b.equals(a));

        Assert.assertTrue(a.equals(a));
        Assert.assertTrue(a.equals(c));
    }

    @Test
    public void testToString() {
        // arrange
        EpsilonFloatPrecisionContext a = new EpsilonFloatPrecisionContext(1f);

        // act
        String str = a.toString();

        // assert
        Assert.assertTrue(str.contains("EpsilonFloatPrecisionContext"));
        Assert.assertTrue(str.contains("epsilon= 1"));
    }

    /**
     * Increments the given double value {@code count} number of times
     * using {@link Math#nextUp(float)}.
     * @param n
     * @param count
     * @return
     */
    private static float nextUp(float n, int count) {
        float result = n;
        for (int i=0; i<count; ++i) {
            result = Math.nextUp(result);
        }

        return result;
    }

    /**
     * Decrements the given double value {@code count} number of times
     * using {@link Math#nextDown(float)}.
     * @param n
     * @param count
     * @return
     */
    private static float nextDown(float n, int count) {
        float result = n;
        for (int i=0; i<count; ++i) {
            result = Math.nextDown(result);
        }

        return result;
    }
}
