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

package org.apache.commons.numbers.examples.jmh.arrays;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link UpdatingInterval} implementations.
 */
class UpdatingIntervalTest {
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 42, Integer.MAX_VALUE - 1})
    void testPointInterval(int k) {
        final UpdatingInterval interval = IndexIntervals.interval(k);
        Assertions.assertEquals(k, interval.left());
        Assertions.assertEquals(k, interval.right());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> interval.updateLeft(k));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> interval.updateRight(k));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> interval.splitLeft(k, k));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> interval.splitRight(k, k));
    }

    @ParameterizedTest
    @CsvSource({
        "1, 1",
        "1, 2",
        "1, 3",
        "10, 42",
    })
    void testRangeInterval(int lo, int hi) {
        UpdatingInterval interval = IndexIntervals.interval(lo, hi);
        Assertions.assertEquals(lo, interval.left());
        Assertions.assertEquals(hi, interval.right());
        if (interval.left() < interval.right()) {
            Assertions.assertEquals(lo + 1, interval.updateLeft(lo + 1));
        }
        interval = IndexIntervals.interval(lo, hi);
        if (interval.left() < interval.right()) {
            Assertions.assertEquals(hi - 1, interval.updateRight(hi - 1));
        }
        interval = IndexIntervals.interval(lo, hi);
        if (interval.left() + 2 < interval.right()) {
            final int left = interval.left();
            final int right = interval.right();
            final int m1 = (interval.left() + interval.right()) >>> 1;
            final int m2 = m1 + 1;
            final UpdatingInterval leftInterval = interval.splitLeft(m1, m2);
            Assertions.assertEquals(left, leftInterval.left());
            Assertions.assertEquals(m1 - 1, leftInterval.right());
            Assertions.assertEquals(m2 + 1, interval.left());
            Assertions.assertEquals(right, interval.right());

            interval = IndexIntervals.interval(lo, hi);
            final UpdatingInterval rightInterval = interval.splitRight(m1, m2);
            Assertions.assertEquals(left, interval.left());
            Assertions.assertEquals(m1 - 1, interval.right());
            Assertions.assertEquals(m2 + 1, rightInterval.left());
            Assertions.assertEquals(right, rightInterval.right());
        }
    }

    @Test
    void testKeyIntervalInvalidIndicesThrows() {
        // Size zero
        Assertions.assertThrows(IllegalArgumentException.class, () -> KeyUpdatingInterval.of(new int[0], 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> KeyUpdatingInterval.of(new int[10], 0));
        // Not sorted
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> KeyUpdatingInterval.of(new int[] {3, 2, 1}, 3));
        // Not unique
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> KeyUpdatingInterval.of(new int[] {1, 2, 2, 3}, 4));
        // Invalid indices: not in [0, Integer.MAX_VALUE)
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> KeyUpdatingInterval.of(new int[] {-1, 2, 3}, 3));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> KeyUpdatingInterval.of(new int[] {1, 2, Integer.MAX_VALUE}, 3));
    }

    @Test
    void testBitIndexUpdatingIntervalInvalidIndicesThrows() {
        // Size zero
        Assertions.assertThrows(IllegalArgumentException.class, () -> BitIndexUpdatingInterval.of(new int[0], 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> BitIndexUpdatingInterval.of(new int[10], 0));
        // Invalid indices: not in [0, Integer.MAX_VALUE)
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> BitIndexUpdatingInterval.of(new int[] {-1, 2, 3}, 3));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> BitIndexUpdatingInterval.of(new int[] {1, 2, Integer.MAX_VALUE}, 3));
    }

    @ParameterizedTest
    @MethodSource(value = {"testIndices"})
    void testUpdateKeyInterval(int[] indices) {
        assertUpdate(KeyUpdatingInterval::of, indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testIndices"})
    void testUpdateIndexSetInterval(int[] indices) {
        // Skip this due to excess memory consumption
        Assumptions.assumeTrue(indices[indices.length - 1] < Integer.MAX_VALUE - 1);
        assertUpdate((k, n) -> IndexSet.of(k, n).interval(), indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testIndices"})
    void testUpdateBitIndexUpdatingInterval(int[] indices) {
        // Skip this due to excess memory consumption
        Assumptions.assumeTrue(indices[indices.length - 1] < Integer.MAX_VALUE - 1);
        assertUpdate(BitIndexUpdatingInterval::of, indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testIndices"})
    void testUpdateIndexInterval(int[] indices) {
        assertUpdate(IndexIntervals::createUpdatingInterval, indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testIndices"})
    void testSplitKeyInterval(int[] indices) {
        assertSplit(KeyUpdatingInterval::of, indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testIndices"})
    void testSplitIndexSetInterval(int[] indices) {
        // Skip this due to excess memory consumption
        Assumptions.assumeTrue(indices[indices.length - 1] < Integer.MAX_VALUE - 1);
        assertSplit((k, n) -> IndexSet.of(k, n).interval(), indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testIndices"})
    void testSplitBitIndexUpdatingInterval(int[] indices) {
        // Skip this due to excess memory consumption
        Assumptions.assumeTrue(indices[indices.length - 1] < Integer.MAX_VALUE - 1);
        assertSplit(BitIndexUpdatingInterval::of, indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testIndices"})
    void testSplitIndexInterval(int[] indices) {
        assertSplit(IndexIntervals::createUpdatingInterval, indices);
    }

    /**
     * Assert the {@link UpdatingInterval#updateLeft(int)} and {@link UpdatingInterval#updateRight(int)} methods.
     * These are tested by successive calls to reduce the interval by 1 index until it
     * has only 1 index remaining.
     *
     * @param constructor Interval constructor.
     * @param indices Indices.
     */
    private static void assertUpdate(BiFunction<int[], Integer, UpdatingInterval> constructor,
            int[] indices) {
        UpdatingInterval interval = constructor.apply(indices, indices.length);
        final int nm1 = indices.length - 1;
        Assertions.assertEquals(indices[0], interval.left());
        Assertions.assertEquals(indices[nm1], interval.right());

        // Use updateLeft to reduce the interval to length 1
        for (int i = 1; i < indices.length; i++) {
            // rounded down median between indices
            final int k = (indices[i - 1] + indices[i]) >>> 1;
            interval.updateLeft(k + 1);
            Assertions.assertEquals(indices[i], interval.left());
        }
        Assertions.assertEquals(interval.left(), interval.right());

        // Use updateRight to reduce the interval to length 1
        interval = constructor.apply(indices, indices.length);
        for (int i = indices.length; --i > 0;) {
            // rounded up median between indices
            final int k = 1 + ((indices[i - 1] + indices[i]) >>> 1);
            interval.updateRight(k - 1);
            Assertions.assertEquals(indices[i - 1], interval.right());
        }
        Assertions.assertEquals(interval.left(), interval.right());
    }

    /**
     * Assert the {@link UpdatingInterval#splitLeft(int, int)} method.
     * These are tested by successive calls to split the interval around the mid-point.
     *
     * @param constructor Interval constructor.
     * @param indices Indices.
     */
    private static void assertSplit(BiFunction<int[], Integer, UpdatingInterval> constructor, int[] indices) {
        assertSplitMedian(constructor.apply(indices, indices.length),
            indices, 0, indices.length - 1, true);
        assertSplitMedian(constructor.apply(indices, indices.length),
            indices, 0, indices.length - 1, false);
        assertSplitMiddleIndices(constructor.apply(indices, indices.length),
            indices, 0, indices.length - 1, true);
        assertSplitMiddleIndices(constructor.apply(indices, indices.length),
            indices, 0, indices.length - 1, false);
    }

    /**
     * Assert a split using the median value between the split median.
     *
     * @param interval Interval.
     * @param indices Indices.
     * @param i Low index into the indices (inclusive).
     * @param j High index into the indices (inclusive).
     * @param splitLeft Use split left, else split right
     */
    private static void assertSplitMedian(UpdatingInterval interval, int[] indices, int i, int j,
            boolean splitLeft) {
        if (indices[i] + 1 >= indices[j]) {
            // Cannot split - no value between the low and high points
            return;
        }
        // Find the expected split about the median
        final int m = (indices[i] + indices[j]) >>> 1;
        // Binary search finds the value or the insertion index of the value
        int hi = Arrays.binarySearch(indices, i, j + 1, m + 1);
        if (hi < 0) {
            // Use the insertion index
            hi = ~hi;
        }
        // Scan for the lower index
        int lo = hi;
        do {
            --lo;
        } while (indices[lo] >= m);

        final int left = interval.left();
        final int right = interval.right();

        UpdatingInterval leftInterval;
        if (splitLeft) {
            leftInterval = interval.splitLeft(m, m);
        } else {
            UpdatingInterval rightInterval = interval.splitRight(m, m);
            leftInterval = interval;
            interval = rightInterval;
        }
        Assertions.assertEquals(left, leftInterval.left());
        Assertions.assertEquals(indices[lo], leftInterval.right());
        Assertions.assertEquals(indices[hi], interval.left());
        Assertions.assertEquals(right, interval.right());

        // Recurse
        assertSplitMedian(leftInterval, indices, i, lo, splitLeft);
        assertSplitMedian(interval, indices, hi, j, splitLeft);
    }

    /**
     * Assert a split using the two middle indices.
     *
     * @param interval Interval.
     * @param indices Indices.
     * @param i Low index into the indices (inclusive).
     * @param j High index into the indices (inclusive).
     * @param splitLeft Use split left, else split right
     */
    private static void assertSplitMiddleIndices(UpdatingInterval interval, int[] indices, int i, int j,
            boolean splitLeft) {
        if (i + 3 >= j) {
            // Cannot split - not two indices between low and high index
            return;
        }
        // Middle two indices
        final int m1 = (i + j) >>> 1;
        final int m2 = m1 + 1;

        final int left = interval.left();
        final int right = interval.right();
        UpdatingInterval leftInterval;
        if (splitLeft) {
            leftInterval = interval.splitLeft(indices[m1], indices[m2]);
        } else {
            UpdatingInterval rightInterval = interval.splitRight(indices[m1], indices[m2]);
            leftInterval = interval;
            interval = rightInterval;
        }
        Assertions.assertEquals(left, leftInterval.left());
        Assertions.assertEquals(indices[m1 - 1], leftInterval.right());
        Assertions.assertEquals(indices[m2 + 1], interval.left());
        Assertions.assertEquals(right, interval.right());

        // Recurse
        assertSplitMiddleIndices(leftInterval, indices, i, m1 - 1, splitLeft);
        assertSplitMiddleIndices(interval, indices, m2 + 1, j, splitLeft);
    }

    static Stream<int[]> testIndices() {
        return SearchableIntervalTest.testPreviousNextIndex();
    }

    @Test
    void testIndexIntervalCreate() {
        // The above tests verify the UpdatingInterval implementations all work.
        // Hit all paths in the analysis performed to create an interval.

        // 1 key
        Assertions.assertEquals(IndexIntervals.PointInterval.class,
            IndexIntervals.createUpdatingInterval(new int[] {1}, 1).getClass());

        // 2 close keys
        Assertions.assertEquals(IndexIntervals.RangeInterval.class,
            IndexIntervals.createUpdatingInterval(new int[] {2, 1}, 2).getClass());
        Assertions.assertEquals(IndexIntervals.RangeInterval.class,
            IndexIntervals.createUpdatingInterval(new int[] {1, 2}, 2).getClass());

        // 2 unsorted keys
        Assertions.assertEquals(KeyUpdatingInterval.class,
            IndexIntervals.createUpdatingInterval(new int[] {200, 1}, 2).getClass());

        // Sorted number of keys saturating the range
        Assertions.assertEquals(KeyUpdatingInterval.class,
            IndexIntervals.createUpdatingInterval(new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}, 11).getClass());
        // Small number of keys saturating the range
        Assertions.assertEquals(KeyUpdatingInterval.class,
            IndexIntervals.createUpdatingInterval(new int[] {11, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1}, 11).getClass());
        // Keys over a huge range
        Assertions.assertEquals(KeyUpdatingInterval.class,
            IndexIntervals.createUpdatingInterval(new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, Integer.MAX_VALUE - 1}, 11).getClass());

        // Small number of sorted keys over a moderate range
        int[] k = IntStream.range(0, 30).map(i -> i * 64) .toArray();
        Assertions.assertEquals(KeyUpdatingInterval.class,
            IndexIntervals.createUpdatingInterval(k.clone(), k.length).getClass());
        // Same keys not sorted
        reverse(k, 0, k.length);
        Assertions.assertEquals(BitIndexUpdatingInterval.class,
            IndexIntervals.createUpdatingInterval(k.clone(), k.length).getClass());
        // Same keys over a huge range
        k[k.length - 1] = Integer.MAX_VALUE - 1;
        Assertions.assertEquals(KeyUpdatingInterval.class,
            IndexIntervals.createUpdatingInterval(k, k.length).getClass());

        // Moderate number of sorted keys over a moderate range
        k = IntStream.range(0, 3000).map(i -> i * 64) .toArray();
        Assertions.assertEquals(KeyUpdatingInterval.class,
            IndexIntervals.createUpdatingInterval(k.clone(), k.length).getClass());
        // Same keys not sorted
        reverse(k, 0, k.length);
        Assertions.assertEquals(BitIndexUpdatingInterval.class,
            IndexIntervals.createUpdatingInterval(k.clone(), k.length).getClass());
        // Same keys over a huge range - switch to binary search on the keys
        k[k.length - 1] = Integer.MAX_VALUE - 1;
        Assertions.assertEquals(KeyUpdatingInterval.class,
            IndexIntervals.createUpdatingInterval(k, k.length).getClass());
    }

    /**
     * Reverse (part of) the data.
     *
     * @param a Data.
     * @param from Start index to reverse (inclusive).
     * @param to End index to reverse (exclusive).
     */
    private static void reverse(int[] a, int from, int to) {
        for (int i = from - 1, j = to; ++i < --j;) {
            final int v = a[i];
            a[i] = a[j];
            a[j] = v;
        }
    }
}
