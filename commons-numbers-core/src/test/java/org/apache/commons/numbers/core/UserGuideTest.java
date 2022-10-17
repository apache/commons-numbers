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

/**
 * Tests for examples contained in the user guide.
 */
class UserGuideTest {

    @Test
    void testNorm1() {
        double x = Norm.EUCLIDEAN.of(3, -4);
        double y = Norm.MANHATTAN.of(3, -4, 5);
        double z = Norm.MAXIMUM.of(new double[] {3, -4, 5, -6, -7, -8});
        Assertions.assertEquals(5, x);
        Assertions.assertEquals(12, y);
        Assertions.assertEquals(8, z);

        double big = Double.MAX_VALUE * 0.5;
        double length = Norm.EUCLIDEAN.of(big, big, big);
        double expected = Math.sqrt(0.5 * 0.5 * 3) * Double.MAX_VALUE;
        Assertions.assertEquals(expected, length, Math.ulp(expected));
    }

    @Test
    void testSum1() {
        double sum1 = Sum.create().add(1)
                                  .addProduct(3, 4)
                                  .getAsDouble();
        double sum2 = Sum.of(1).addProduct(3, 4)
                               .getAsDouble();
        double sum3 = Sum.ofProducts(new double[] {3, 4}, new double[] {5, 6})
                         .getAsDouble();
        Assertions.assertEquals(13, sum1);
        Assertions.assertEquals(13, sum2);
        Assertions.assertEquals(3 * 5 + 4 * 6, sum3);

        Assertions.assertEquals(Double.NaN, Sum.of(1, 2, Double.NaN).getAsDouble());
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, Sum.of(1, 2, Double.NEGATIVE_INFINITY).getAsDouble());
    }

    @Test
    void testSum2() {
        double x1 = 1e100 + 1 - 2 - 1e100;
        double x2 = Sum.of(1e100, 1, -2, -1e100).getAsDouble();
        Assertions.assertEquals(0.0, x1);
        Assertions.assertEquals(-1.0, x2);
    }

    @Test
    void testPrecision1() {
        // Default allows no numbers between
        Assertions.assertTrue(Precision.equals(1000.0, 1000.0));
        Assertions.assertTrue(Precision.equals(1000.0, 1000.0 + Math.ulp(1000.0)));
        Assertions.assertFalse(Precision.equals(1000.0, 1000.0 + 2 * Math.ulp(1000.0)));

        // Absolute - tolerance is floating-point
        Assertions.assertFalse(Precision.equals(1000.0, 1001.0));
        Assertions.assertTrue(Precision.equals(1000.0, 1001.0, 1.0));
        Assertions.assertTrue(Precision.equals(1000.0, 1000.0 + Math.ulp(1000.0), 0.0));

        // ULP - tolerance is integer
        Assertions.assertFalse(Precision.equals(1000.0, 1001.0));
        Assertions.assertFalse(Precision.equals(1000.0, 1001.0, 1));
        Assertions.assertFalse(Precision.equals(1000.0, 1000.0 + 2 * Math.ulp(1000.0), 1));
        Assertions.assertTrue(Precision.equals(1000.0, 1000.0 + 2 * Math.ulp(1000.0), 2));
        Assertions.assertFalse(Precision.equals(1000.0, 1000.0 + 3 * Math.ulp(1000.0), 2));

        // Relative
        Assertions.assertFalse(Precision.equalsWithRelativeTolerance(1000.0, 1001.0, 1e-6));
        Assertions.assertTrue(Precision.equalsWithRelativeTolerance(1000.0, 1001.0, 1e-3));
    }

    @Test
    void testPrecision2() {
        Assertions.assertFalse(Precision.equals(Double.NaN, 1000.0));
        Assertions.assertFalse(Precision.equals(Double.NaN, Double.NaN));
        Assertions.assertTrue(Precision.equalsIncludingNaN(Double.NaN, Double.NaN));

        Assertions.assertTrue(Precision.equals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        Assertions.assertTrue(Precision.equals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
        Assertions.assertFalse(Precision.equals(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY));
    }

    @Test
    void testPrecision3() {
        Assertions.assertEquals(0, Precision.compareTo(100, 100, 0.0));
        Assertions.assertEquals(0, Precision.compareTo(100, 101, 1.0));
        Assertions.assertEquals(-1, Precision.compareTo(100, 102, 1.0));
        Assertions.assertEquals(1, Precision.compareTo(102, 100, 1.0));
    }

    @Test
    void testPrecision4() {
        Precision.DoubleEquivalence eq = Precision.doubleEquivalenceOfEpsilon(1.0);
        Assertions.assertFalse(eq.lt(100, 100));
        Assertions.assertTrue(eq.lte(100, 100));
        Assertions.assertTrue(eq.eq(100, 100));
        Assertions.assertTrue(eq.gte(100, 100));
        Assertions.assertFalse(eq.gt(100, 100));
    }

    @Test
    void testPrecision5() {
        Assertions.assertEquals(678.125, Precision.round(678.125, 4));
        Assertions.assertEquals(678.125, Precision.round(678.125, 3));
        Assertions.assertEquals(678.13, Precision.round(678.125, 2));
        Assertions.assertEquals(678.1, Precision.round(678.125, 1));
        Assertions.assertEquals(678.0, Precision.round(678.125, 0));
        Assertions.assertEquals(680.0, Precision.round(678.125, -1));
        Assertions.assertEquals(700.0, Precision.round(678.125, -2));

        Assertions.assertEquals(0.10000000000000009, Precision.representableDelta(1.0, 0.1));
    }
}
