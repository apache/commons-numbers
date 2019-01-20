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

import org.junit.Assert;
import org.junit.Test;

public class DoublePrecisionContextTest {

    private StubContext ctx = new StubContext();

    @Test
    public void testAreEqual() {
        // act/assert
        Assert.assertTrue(ctx.areEqual(0.0, 0.0));
        Assert.assertTrue(ctx.areEqual(1.0, 1.0));
        Assert.assertTrue(ctx.areEqual(-1.0, -1.0));

        Assert.assertFalse(ctx.areEqual(1.0, -1.0));
        Assert.assertFalse(ctx.areEqual(1.0, Math.nextUp(1.0)));
        Assert.assertFalse(ctx.areEqual(-1.0, Math.nextDown(1.0)));
    }

    @Test
    public void testIsZero() {
        // act/assert
        Assert.assertTrue(ctx.isZero(0.0));

        Assert.assertFalse(ctx.isZero(Math.nextUp(0.0)));
        Assert.assertFalse(ctx.isZero(Math.nextDown(-0.0)));
    }

    @Test
    public void testIsLessThan() {
        // act/assert
        Assert.assertTrue(ctx.isLessThan(1, 2));
        Assert.assertTrue(ctx.isLessThan(-2, -1));

        Assert.assertFalse(ctx.isLessThan(1, 1));
        Assert.assertFalse(ctx.isLessThan(-1, -1));
        Assert.assertFalse(ctx.isLessThan(2, 1));
        Assert.assertFalse(ctx.isLessThan(-1, -2));
    }

    @Test
    public void testIsLessThanOrEqual() {
        // act/assert
        Assert.assertTrue(ctx.isLessThanOrEqual(1, 2));
        Assert.assertTrue(ctx.isLessThanOrEqual(-2, -1));
        Assert.assertTrue(ctx.isLessThanOrEqual(1, 1));
        Assert.assertTrue(ctx.isLessThanOrEqual(-1, -1));

        Assert.assertFalse(ctx.isLessThanOrEqual(2, 1));
        Assert.assertFalse(ctx.isLessThanOrEqual(-1, -2));
    }

    @Test
    public void testIsGreaterThan() {
        // act/assert
        Assert.assertTrue(ctx.isGreaterThan(2, 1));
        Assert.assertTrue(ctx.isGreaterThan(-1, -2));

        Assert.assertFalse(ctx.isGreaterThan(1, 1));
        Assert.assertFalse(ctx.isGreaterThan(-1, -1));
        Assert.assertFalse(ctx.isGreaterThan(1, 2));
        Assert.assertFalse(ctx.isGreaterThan(-2, -1));
    }

    @Test
    public void testIsGreaterThanOrEqual() {
        // act/assert
        Assert.assertTrue(ctx.isGreaterThanOrEqual(2, 1));
        Assert.assertTrue(ctx.isGreaterThanOrEqual(-1, -2));
        Assert.assertTrue(ctx.isGreaterThanOrEqual(1, 1));
        Assert.assertTrue(ctx.isGreaterThanOrEqual(-1, -1));

        Assert.assertFalse(ctx.isGreaterThanOrEqual(1, 2));
        Assert.assertFalse(ctx.isGreaterThanOrEqual(-2, -1));
    }

    @Test
    public void testCompare() {
        // act/assert
        Assert.assertEquals(0, ctx.compare(1, 1));
        Assert.assertEquals(-1, ctx.compare(1, 2));
        Assert.assertEquals(1, ctx.compare(2, 1));

        Assert.assertEquals(0, ctx.compare(-1, -1));
        Assert.assertEquals(1, ctx.compare(-1, -2));
        Assert.assertEquals(-1, ctx.compare(-2, -1));
    }

    @Test
    public void testCompare_wrapper() {
        // act/assert
        Assert.assertEquals(0, ctx.compare(new Double(1), new Double(1)));
        Assert.assertEquals(-1, ctx.compare(new Double(1), new Double(2)));
        Assert.assertEquals(1, ctx.compare(new Double(2), new Double(1)));

        Assert.assertEquals(0, ctx.compare(new Double(-1), new Double(-1)));
        Assert.assertEquals(1, ctx.compare(new Double(-1), new Double(-2)));
        Assert.assertEquals(-1, ctx.compare(new Double(-2), new Double(-1)));
    }

    private static class StubContext extends DoublePrecisionContext {

        @Override
        public double getMaxZero() {
            return 0.0;
        }

        @Override
        public int compare(double a, double b) {
            return Double.compare(a, b);
        }
    }
}
