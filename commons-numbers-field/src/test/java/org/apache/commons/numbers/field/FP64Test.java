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
package org.apache.commons.numbers.field;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FP64}.
 */
public class FP64Test {
    @Test
    public void testConsistencyWithDouble() {
        final double v = -5.67e89;
        final Double a = Double.valueOf(v);
        final FP64 b = FP64.of(v);

        Assertions.assertEquals(a.doubleValue(), b.doubleValue(), 0d);
        Assertions.assertEquals(a.floatValue(), b.floatValue(), 0f);
        Assertions.assertEquals(a.intValue(), b.intValue());
        Assertions.assertEquals(a.longValue(), b.longValue());
        Assertions.assertEquals(a.byteValue(), b.byteValue());
        Assertions.assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testOne() {
        Assertions.assertEquals(1d, FP64.of(-3.4).one().doubleValue(), 0d);
    }
    @Test
    public void testZero() {
        Assertions.assertEquals(0d, FP64.of(-3.4).zero().doubleValue(), 0d);
    }

    @Test
    public void testSubtract() {
        final double a = 123.4;
        final double b = 5678.9;

        Assertions.assertEquals(a - b, FP64.of(a).subtract(FP64.of(b)).doubleValue(), 0d);
    }
    @Test
    public void testDivide() {
        final double a = 123.4;
        final double b = 5678.9;

        Assertions.assertEquals(a / b, FP64.of(a).divide(FP64.of(b)).doubleValue(), 0d);
    }

    @Test
    public void testMultiplyInt() {
        final double a = 123.4;
        final int n = 3456789;

        Assertions.assertEquals(n * a, FP64.of(a).multiply(n).doubleValue(), 0d);
    }

    @Test
    public void testPowInt() {
        final double a = 123.4;
        final int n = 5;

        Assertions.assertEquals(Math.pow(a, n), FP64.of(a).pow(n).doubleValue(), 0d);
    }
    @Test
    public void testZeroPow() {
        Assertions.assertSame(FP64.of(9876.5).one(), FP64.of(2.3456).pow(0));
    }

    @Test
    public void testCompare() {
        Assertions.assertTrue(FP64.of(0).compareTo(FP64.of(-1)) > 0);
        Assertions.assertTrue(FP64.of(1).compareTo(FP64.of(2)) < 0);

        final double v = 123.45;
        Assertions.assertTrue(FP64.of(v).compareTo(FP64.of(v)) == 0);
    }
}
