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

class EpsilonPrecisionComparatorTest {

    @Test
    void testGetters() {
        // arrange
        final double eps = 1e-6;

        // act
        final EpsilonPrecisionComparator cmp = new EpsilonPrecisionComparator(eps);

        // assert
        Assertions.assertEquals(eps, cmp.getEpsilon(), 0.0);
    }

    @Test
    void testInvalidEpsilonValues() {
        // act/assert
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EpsilonPrecisionComparator(-1.0));

        String msg;

        msg = Assertions.assertThrows(IllegalArgumentException.class,
            () -> new EpsilonPrecisionComparator(Double.NaN)).getMessage();
        Assertions.assertEquals("Invalid epsilon value: NaN", msg);

        msg = Assertions.assertThrows(IllegalArgumentException.class,
            () -> new EpsilonPrecisionComparator(Double.POSITIVE_INFINITY)).getMessage();
        Assertions.assertEquals("Invalid epsilon value: Infinity", msg);

        msg = Assertions.assertThrows(IllegalArgumentException.class,
            () -> new EpsilonPrecisionComparator(Double.NEGATIVE_INFINITY)).getMessage();
        Assertions.assertEquals("Invalid epsilon value: -Infinity", msg);
    }

    @Test
    void testSign() {
        // arrange
        final double eps = 1e-2;

        final EpsilonPrecisionComparator cmp = new EpsilonPrecisionComparator(eps);

        // act/assert
        Assertions.assertEquals(0, cmp.sign(0.0));
        Assertions.assertEquals(0, cmp.sign(-0.0));

        Assertions.assertEquals(0, cmp.sign(1e-2));
        Assertions.assertEquals(0, cmp.sign(-1e-2));

        Assertions.assertEquals(1, cmp.sign(1e-1));
        Assertions.assertEquals(-1, cmp.sign(-1e-1));

        Assertions.assertEquals(1, cmp.sign(Double.NaN));
        Assertions.assertEquals(1, cmp.sign(Double.POSITIVE_INFINITY));
        Assertions.assertEquals(-1, cmp.sign(Double.NEGATIVE_INFINITY));
    }

    @Test
    void testCompare_compareToZero() {
        // arrange
        final double eps = 1e-2;

        final EpsilonPrecisionComparator cmp = new EpsilonPrecisionComparator(eps);

        // act/assert
        Assertions.assertEquals(0, cmp.compare(0.0, 0.0));
        Assertions.assertEquals(0, cmp.compare(+0.0, -0.0));
        Assertions.assertEquals(0, cmp.compare(eps, -0.0));
        Assertions.assertEquals(0, cmp.compare(+0.0, eps));

        Assertions.assertEquals(0, cmp.compare(-eps, -0.0));
        Assertions.assertEquals(0, cmp.compare(+0.0, -eps));

        Assertions.assertEquals(-1, cmp.compare(0.0, 1.0));
        Assertions.assertEquals(1, cmp.compare(1.0, 0.0));

        Assertions.assertEquals(1, cmp.compare(0.0, -1.0));
        Assertions.assertEquals(-1, cmp.compare(-1.0, 0.0));
    }

    @Test
    void testCompare_compareNonZero() {
        // arrange
        final double eps = 1e-5;
        final double small = 1e-3;
        final double big = 1e100;

        final EpsilonPrecisionComparator cmp = new EpsilonPrecisionComparator(eps);

        // act/assert
        Assertions.assertEquals(0, cmp.compare(eps, 2 * eps));
        Assertions.assertEquals(0, cmp.compare(-2 * eps, -eps));

        Assertions.assertEquals(0, cmp.compare(small, small + (0.9 * eps)));
        Assertions.assertEquals(0, cmp.compare(-small - (0.9 * eps), -small));

        Assertions.assertEquals(0, cmp.compare(big, nextUp(big, 1)));
        Assertions.assertEquals(0, cmp.compare(nextDown(-big, 1), -big));

        Assertions.assertEquals(-1, cmp.compare(small, small + (1.1 * eps)));
        Assertions.assertEquals(1, cmp.compare(-small, -small - (1.1 * eps)));

        Assertions.assertEquals(-1, cmp.compare(big, nextUp(big, 2)));
        Assertions.assertEquals(1, cmp.compare(-big, nextDown(-big, 2)));
    }

    @Test
    void testCompare_NaN() {
        // arrange
        final EpsilonPrecisionComparator cmp = new EpsilonPrecisionComparator(1e-6);

        // act/assert
        Assertions.assertEquals(1, cmp.compare(0, Double.NaN));
        Assertions.assertEquals(1, cmp.compare(Double.NaN, 0));
        Assertions.assertEquals(1, cmp.compare(Double.NaN, Double.NaN));

        Assertions.assertEquals(1, cmp.compare(Double.POSITIVE_INFINITY, Double.NaN));
        Assertions.assertEquals(1, cmp.compare(Double.NaN, Double.POSITIVE_INFINITY));

        Assertions.assertEquals(1, cmp.compare(Double.NEGATIVE_INFINITY, Double.NaN));
        Assertions.assertEquals(1, cmp.compare(Double.NaN, Double.NEGATIVE_INFINITY));
    }

    @Test
    void testCompare_infinity() {
        // arrange
        final EpsilonPrecisionComparator cmp = new EpsilonPrecisionComparator(1e-6);

        // act/assert
        Assertions.assertEquals(-1, cmp.compare(0, Double.POSITIVE_INFINITY));
        Assertions.assertEquals(1, cmp.compare(Double.POSITIVE_INFINITY, 0));
        Assertions.assertEquals(0, cmp.compare(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));

        Assertions.assertEquals(1, cmp.compare(0, Double.NEGATIVE_INFINITY));
        Assertions.assertEquals(-1, cmp.compare(Double.NEGATIVE_INFINITY, 0));
        Assertions.assertEquals(0, cmp.compare(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
    }

    @Test
    void testHashCode() {
        // arrange
        final EpsilonPrecisionComparator a = new EpsilonPrecisionComparator(1e-6);
        final EpsilonPrecisionComparator b = new EpsilonPrecisionComparator(1e-7);
        final EpsilonPrecisionComparator c = new EpsilonPrecisionComparator(1e-6);

        // act/assert
        Assertions.assertEquals(a.hashCode(), a.hashCode());
        Assertions.assertEquals(a.hashCode(), c.hashCode());

        Assertions.assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void testEquals() {
        // arrange
        final EpsilonPrecisionComparator a = new EpsilonPrecisionComparator(1e-6);
        final EpsilonPrecisionComparator b = new EpsilonPrecisionComparator(1e-7);
        final EpsilonPrecisionComparator c = new EpsilonPrecisionComparator(1e-6);

        // act/assert
        Assertions.assertFalse(a.equals(null));
        Assertions.assertFalse(a.equals(new Object()));

        Assertions.assertTrue(a.equals(a));

        Assertions.assertFalse(a.equals(b));
        Assertions.assertFalse(b.equals(a));

        Assertions.assertTrue(a.equals(c));
    }

    @Test
    void testEqualsAndHashCode_signedZeroConsistency() {
        // arrange
        final EpsilonPrecisionComparator a = new EpsilonPrecisionComparator(0.0);
        final EpsilonPrecisionComparator b = new EpsilonPrecisionComparator(-0.0);
        final EpsilonPrecisionComparator c = new EpsilonPrecisionComparator(0.0);
        final EpsilonPrecisionComparator d = new EpsilonPrecisionComparator(-0.0);

        // act/assert
        Assertions.assertFalse(a.equals(b));
        Assertions.assertNotEquals(a.hashCode(), b.hashCode());

        Assertions.assertTrue(a.equals(c));
        Assertions.assertEquals(a.hashCode(), c.hashCode());

        Assertions.assertTrue(b.equals(d));
        Assertions.assertEquals(b.hashCode(), d.hashCode());
    }

    @Test
    void testToString() {
        // arrange
        final EpsilonPrecisionComparator a = new EpsilonPrecisionComparator(1d);

        // act
        final String str = a.toString();

        // assert
        Assertions.assertTrue(str.contains("EpsilonPrecisionComparator"));
        Assertions.assertTrue(str.contains("epsilon= 1"));
    }

    @Test
    void testSerializable() {
        // arrange
        final EpsilonPrecisionComparator in = new EpsilonPrecisionComparator(1d);

        // act
        final EpsilonPrecisionComparator out = (EpsilonPrecisionComparator) TestUtils.serializeAndRecover(in);

        // assert
        Assertions.assertEquals(in, out);
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
