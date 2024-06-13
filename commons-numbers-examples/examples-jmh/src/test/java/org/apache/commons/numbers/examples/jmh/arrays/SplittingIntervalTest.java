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
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link SplittingInterval} implementations.
 */
class SplittingIntervalTest {

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
    void testSplitKeyInterval(int[] indices) {
        assertSplit(KeyUpdatingInterval::of, indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testIndices"})
    void testSplitBitIndexUpdatingInterval(int[] indices) {
        // Skip this due to excess memory consumption
        Assumptions.assumeTrue(indices[indices.length - 1] < Integer.MAX_VALUE - 1);
        assertSplit(BitIndexUpdatingInterval::of, indices);
    }

    /**
     * Assert the {@link SplittingInterval#splitLeft(int, int)} method.
     * These are tested by successive calls to split the interval around the mid-point.
     *
     * @param constructor Interval constructor.
     * @param indices Indices.
     */
    private static void assertSplit(BiFunction<int[], Integer, SplittingInterval> constructor, int[] indices) {
        assertSplitMedian(constructor.apply(indices, indices.length),
            indices, 0, indices.length - 1);
        assertSplitMiddleIndices(constructor.apply(indices, indices.length),
            indices, 0, indices.length - 1);
    }

    /**
     * Assert a split using the median value between the split median.
     *
     * @param interval Interval.
     * @param indices Indices.
     * @param i Low index into the indices (inclusive).
     * @param j High index into the indices (inclusive).
     */
    private static void assertSplitMedian(SplittingInterval interval, int[] indices, int i, int j) {
        if (indices[i] + 1 >= indices[j]) {
            // Cannot split - no value between the low and high points
            // Split on low should return null left, right may not be empty
            final SplittingInterval leftInterval = interval.split(indices[i], indices[i]);
            Assertions.assertNull(leftInterval, "left should be empty");
            if (indices[i] == indices[j]) {
                Assertions.assertTrue(interval.empty(), "right should be empty");
            } else {
                Assertions.assertFalse(interval.empty(), "right should not be empty");
                Assertions.assertEquals(indices[i + 1], interval.left());
                Assertions.assertEquals(indices[j], interval.right());
            }
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

        final SplittingInterval leftInterval = interval.split(m, m);
        Assertions.assertEquals(left, leftInterval.left());
        Assertions.assertEquals(indices[lo], leftInterval.right());
        Assertions.assertEquals(indices[hi], interval.left());
        Assertions.assertEquals(right, interval.right());

        // Recurse
        assertSplitMedian(leftInterval, indices, i, lo);
        assertSplitMedian(interval, indices, hi, j);
    }

    /**
     * Assert a split using the two middle indices.
     *
     * @param interval Interval.
     * @param indices Indices.
     * @param i Low index into the indices (inclusive).
     * @param j High index into the indices (inclusive).
     */
    private static void assertSplitMiddleIndices(SplittingInterval interval, int[] indices, int i, int j) {
        if (i + 3 >= j) {
            // Cannot split - not two indices between low and high index
            // Split on high may return left, right should be empty
            final SplittingInterval leftInterval = interval.split(indices[j], indices[j]);
            Assertions.assertTrue(interval.empty(), "right should be empty");
            if (indices[i] == indices[j]) {
                Assertions.assertNull(leftInterval, "left should be empty");
            } else {
                Assertions.assertEquals(indices[i], leftInterval.left());
                Assertions.assertEquals(indices[j - 1], leftInterval.right());
            }
            return;
        }
        // Middle two indices
        final int m1 = (i + j) >>> 1;
        final int m2 = m1 + 1;

        final int left = interval.left();
        final int right = interval.right();
        final SplittingInterval leftInterval = interval.split(indices[m1], indices[m2]);
        Assertions.assertEquals(left, leftInterval.left());
        Assertions.assertEquals(indices[m1 - 1], leftInterval.right());
        Assertions.assertEquals(indices[m2 + 1], interval.left());
        Assertions.assertEquals(right, interval.right());

        // Recurse
        assertSplitMiddleIndices(leftInterval, indices, i, m1 - 1);
        assertSplitMiddleIndices(interval, indices, m2 + 1, j);
    }

    static Stream<int[]> testIndices() {
        return SearchableIntervalTest.testPreviousNextIndex();
    }
}
