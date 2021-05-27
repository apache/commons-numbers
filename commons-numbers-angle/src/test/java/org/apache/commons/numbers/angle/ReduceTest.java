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
package org.apache.commons.numbers.angle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for the {@link Reduce} class.
 */
class ReduceTest {
    @Test
    void testReduce() {
        final double period = 12.222;
        final double offset = 13456.789;

        final double delta = 1.5;

        double orig = offset + 122456789 * period + delta;
        double expected = delta;

        final Reduce r = new Reduce(offset, period);
        Assertions.assertEquals(expected,
                                r.applyAsDouble(orig),
                                1e-7);

        orig = offset - 123356789 * period - delta;
        expected = Math.abs(period) - delta;
        Assertions.assertEquals(expected,
                                r.applyAsDouble(orig),
                                1e-6);

        orig = offset - 123446789 * period + delta;
        expected = delta;
        Assertions.assertEquals(expected,
                                r.applyAsDouble(orig),
                                1e-6);
    }

    @Test
    void testNaN() {
        final double[] values = new double[] {
            12.345, -9876.5, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
        };

        for (double offset : values) {
            for (double period : values) {
                for (double x : values) {
                    final boolean expectedNaN = Double.isNaN(x) || Double.isInfinite(x) ||
                        Double.isNaN(period) || Double.isInfinite(period) ||
                        Double.isNaN(offset) || Double.isInfinite(offset);

                    final double v = new Reduce(offset, period).applyAsDouble(x);
                    if (expectedNaN) {
                        Assertions.assertTrue(Double.isNaN(v));
                    } else {
                        Assertions.assertFalse(Double.isNaN(v));
                    }
                }
            }
        }
    }

    @Test
    void testReduceNegativePeriod() {
        final double period = 12.222;
        final double offset = 13;
        final double delta = 1.5;
        double orig = offset + 122456789 * period + delta;
        double expected = delta;

        final Reduce r1 = new Reduce(offset, period);
        final Reduce r2 = new Reduce(offset, -period);
        Assertions.assertEquals(expected,
                                r1.applyAsDouble(orig),
                                1e-7);
        Assertions.assertEquals(r1.applyAsDouble(orig),
                                r2.applyAsDouble(orig),
                                0d);
    }

    @Test
    void testReduceComparedWithNormalize() {
        final double period = 2 * Math.PI;
        for (double a = -15; a <= 15; a += 0.5) {
            for (double center = -15; center <= 15; center += 1) {
                final double nA = PlaneAngleRadians.normalizer(center).applyAsDouble(a);
                final double offset = center - Math.PI;
                final Reduce reduce = new Reduce(offset, period);
                final double r = reduce.applyAsDouble(a) + offset;
                Assertions.assertEquals(nA, r, 1.1e2 * Math.ulp(nA),
                                        "a=" + a + " center=" + center);
            }
        }
    }
}
