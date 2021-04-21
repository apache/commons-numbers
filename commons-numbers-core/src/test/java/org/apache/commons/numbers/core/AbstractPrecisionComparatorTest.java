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

class AbstractPrecisionComparatorTest {

    private final StubComparator cmp = new StubComparator();

    @Test
    void testEq() {
        // act/assert
        Assertions.assertTrue(cmp.eq(0.0, 0.0));
        Assertions.assertTrue(cmp.eq(1.0, 1.0));
        Assertions.assertTrue(cmp.eq(-1.0, -1.0));

        Assertions.assertFalse(cmp.eq(1.0, -1.0));
        Assertions.assertFalse(cmp.eq(1.0, Math.nextUp(1.0)));
        Assertions.assertFalse(cmp.eq(-1.0, Math.nextDown(1.0)));
    }

    @Test
    void testEqZero() {
        // act/assert
        Assertions.assertTrue(cmp.eqZero(0.0));

        Assertions.assertFalse(cmp.eqZero(Math.nextUp(0.0)));
        Assertions.assertFalse(cmp.eqZero(Math.nextDown(-0.0)));
    }

    @Test
    void testLt() {
        // act/assert
        Assertions.assertTrue(cmp.lt(1, 2));
        Assertions.assertTrue(cmp.lt(-2, -1));

        Assertions.assertFalse(cmp.lt(1, 1));
        Assertions.assertFalse(cmp.lt(-1, -1));
        Assertions.assertFalse(cmp.lt(2, 1));
        Assertions.assertFalse(cmp.lt(-1, -2));
    }

    @Test
    void testLte() {
        // act/assert
        Assertions.assertTrue(cmp.lte(1, 2));
        Assertions.assertTrue(cmp.lte(-2, -1));
        Assertions.assertTrue(cmp.lte(1, 1));
        Assertions.assertTrue(cmp.lte(-1, -1));

        Assertions.assertFalse(cmp.lte(2, 1));
        Assertions.assertFalse(cmp.lte(-1, -2));
    }

    @Test
    void testGt() {
        // act/assert
        Assertions.assertTrue(cmp.gt(2, 1));
        Assertions.assertTrue(cmp.gt(-1, -2));

        Assertions.assertFalse(cmp.gt(1, 1));
        Assertions.assertFalse(cmp.gt(-1, -1));
        Assertions.assertFalse(cmp.gt(1, 2));
        Assertions.assertFalse(cmp.gt(-2, -1));
    }

    @Test
    void testGte() {
        // act/assert
        Assertions.assertTrue(cmp.gte(2, 1));
        Assertions.assertTrue(cmp.gte(-1, -2));
        Assertions.assertTrue(cmp.gte(1, 1));
        Assertions.assertTrue(cmp.gte(-1, -1));

        Assertions.assertFalse(cmp.gte(1, 2));
        Assertions.assertFalse(cmp.gte(-2, -1));
    }

    @Test
    void testSign() {
        // act/assert
        Assertions.assertEquals(0, cmp.sign(0.0));

        Assertions.assertEquals(1, cmp.sign(1e-3));
        Assertions.assertEquals(-1, cmp.sign(-1e-3));

        Assertions.assertEquals(1, cmp.sign(Double.NaN));
        Assertions.assertEquals(1, cmp.sign(Double.POSITIVE_INFINITY));
        Assertions.assertEquals(-1, cmp.sign(Double.NEGATIVE_INFINITY));
    }

    @Test
    void testCompare() {
        // act/assert
        Assertions.assertEquals(0, cmp.compare(1, 1));
        Assertions.assertEquals(-1, cmp.compare(1, 2));
        Assertions.assertEquals(1, cmp.compare(2, 1));

        Assertions.assertEquals(0, cmp.compare(-1, -1));
        Assertions.assertEquals(1, cmp.compare(-1, -2));
        Assertions.assertEquals(-1, cmp.compare(-2, -1));
    }

    @Test
    void testCompare_wrapper() {
        // act/assert
        Assertions.assertEquals(0, cmp.compare(Double.valueOf(1), Double.valueOf(1)));
        Assertions.assertEquals(-1, cmp.compare(Double.valueOf(1), Double.valueOf(2)));
        Assertions.assertEquals(1, cmp.compare(Double.valueOf(2), Double.valueOf(1)));

        Assertions.assertEquals(0, cmp.compare(Double.valueOf(-1), Double.valueOf(-1)));
        Assertions.assertEquals(1, cmp.compare(Double.valueOf(-1), Double.valueOf(-2)));
        Assertions.assertEquals(-1, cmp.compare(Double.valueOf(-2), Double.valueOf(-1)));
    }

    private static class StubComparator extends AbstractPrecisionComparator {

        @Override
        public int compare(final double a, final double b) {
            return Double.compare(a, b);
        }
    }
}
