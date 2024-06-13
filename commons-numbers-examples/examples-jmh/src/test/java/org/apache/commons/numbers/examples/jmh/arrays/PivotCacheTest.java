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
import java.util.BitSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link PivotCache} implementations.
 */
class PivotCacheTest {

    @ParameterizedTest
    @MethodSource(value = {"testSingleRange", "testSinglePoint"})
    void testSingleRangeUsingFilledIndexSetAsScanningPivotCache(int left, int right, int[][] indices) {
        // Collate all indices
        int[] allIndices = Arrays.stream(indices).flatMapToInt(Arrays::stream).toArray();
        // Append left and right
        final int n = allIndices.length;
        allIndices = Arrays.copyOfRange(allIndices, 0, n + 2);
        allIndices[n] = left;
        allIndices[n + 1] = right;
        final IndexSet set = IndexSet.of(allIndices);
        assertSingleRange(left, right, indices, set.asScanningPivotCache(left, right));
    }

    @ParameterizedTest
    @MethodSource(value = {"testSingleRange", "testSinglePoint"})
    void testSingleRangeUsingScanningPivotCache(int left, int right, int[][] indices) {
        assertSingleRange(left, right, indices, IndexSet.createScanningPivotCache(left, right));
    }

    @ParameterizedTest
    @MethodSource(value = {"testSingleRange", "testSinglePoint"})
    void testSingleRangeUsingIndexSet(int left, int right, int[][] indices) {
        assertSingleRange(left, right, indices, IndexSet.ofRange(left, right));
    }

    @ParameterizedTest
    @MethodSource
    void testSinglePoint(int left, int right, int[][] indices) {
        Assumptions.assumeTrue(left == right);
        assertSingleRange(left, right, indices, PivotCaches.ofIndex(left));
    }

    @ParameterizedTest
    @MethodSource
    void testSingleRange(int left, int right, int[][] indices) {
        assertSingleRange(left, right, indices, PivotCaches.ofRange(left, right));
    }

    @ParameterizedTest
    @MethodSource(value = {"testSingleRange", "testSinglePoint"})
    void testPairedIndex(int left, int right, int[][] indices) {
        if (left == right) {
            assertSingleRange(left, right, indices, PivotCaches.ofPairedIndex(left));
        } else if (left + 1 == right) {
            assertSingleRange(left, right, indices,
                PivotCaches.ofPairedIndex(left | Integer.MIN_VALUE));
        } else {
            Assumptions.abort("Not a paired index");
        }
    }

    /**
     * Assert caching pivots around a single range.
     *
     * <p>This test progressively adds more indices to the cache. It then verifies the
     * range is correctly bracketed and any internal pivots can be traversed as sorted
     * (pivot) and unsorted (non-pivot) regions.
     *
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param indices Batches of indices to add.
     * @param cache Pivot cache.
     */
    private static void assertSingleRange(int left, int right, int[][] indices, PivotCache cache) {
        final BitSet ref = new BitSet();
        Assertions.assertEquals(left, cache.left());
        Assertions.assertEquals(right, cache.right());
        Assertions.assertEquals(ref.previousSetBit(left), cache.previousPivot(left));
        Assertions.assertEquals(ref.nextSetBit(left), cache.nextPivot(left));
        Assertions.assertEquals(Integer.MAX_VALUE, cache.nextPivotOrElse(right, Integer.MAX_VALUE));
        // This assumes the cache supports internal scanning
        // With current implementations this is true.
        for (final int[] batch : indices) {
            for (final int i : batch) {
                ref.set(i);
                cache.add(i);
                if (i >= left && i <= right && !cache.sparse()) {
                    Assertions.assertTrue(cache.contains(i));
                }
            }
            Assertions.assertEquals(left, cache.left());
            Assertions.assertEquals(right, cache.right());
            // Flanking pivots. Note: If actually used for partitioning these
            // must be updated from the possible -1 result.
            final int lower = ref.previousSetBit(left);
            final int upper = ref.nextSetBit(right);
            Assertions.assertEquals(lower, cache.previousPivot(left));
            Assertions.assertEquals(upper, cache.nextPivot(right));
            if (upper < 0) {
                Assertions.assertEquals(Integer.MAX_VALUE, cache.nextPivotOrElse(right, Integer.MAX_VALUE));
            }

            // The partition algorithm must run so [left, right] is sorted
            // lower---left--------------------------right----upper

            if (cache.sparse()) {
                continue;
            }

            // Not sparse: can check for indices
            Assertions.assertEquals(ref.get(left), cache.contains(left));
            Assertions.assertEquals(ref.get(right), cache.contains(right));

            if (!(cache instanceof ScanningPivotCache)) {
                continue;
            }
            final ScanningPivotCache scanningCache = (ScanningPivotCache) cache;

            // Test internal scanning from the ends for additional pivots
            // lower---left------p----------p2-------right----upper
            //                    s--------e

            int p = ref.nextSetBit(left);
            Assertions.assertEquals(p, cache.nextPivot(left));

            if (p == upper) {
                // No internal pivots: just run partitioning
                continue;
            }

            int p2 = ref.previousSetBit(right);
            Assertions.assertEquals(p2, cache.previousPivot(right));

            // Must partition: (lower, p) and (p2, upper)
            // Then fully sort all unsorted parts in (p, p2)

            // left to right
            // Check the traversal with a copy
            BitSet copy = (BitSet) ref.clone();
            // partition [left, p) using bracket (lower, p)
            copy.set(left, p);

            int s = ref.nextClearBit(p);
            int e = ref.nextSetBit(s);
            // e can be -1 so check it is within left
            while (left < e && e < upper) {
                // sort [s, e)
                copy.set(s, e);
                Assertions.assertEquals(s, scanningCache.nextNonPivot(p));
                Assertions.assertEquals(e, cache.nextPivot(s));
                p = s;
                s = ref.nextClearBit(p);
                e = ref.nextSetBit(s);
            }
            // partition (p, right] using bracket (p, upper)
            copy.set(p + 1, right + 1);
            final int unsorted = copy.nextClearBit(left);
            Assertions.assertTrue(right < unsorted, () -> "Bad left-to-right traversal: " + unsorted);

            // right to left
            // Check the traversal with a copy
            copy = (BitSet) ref.clone();
            // partition (p2, right] using bracket (p2, upper)
            copy.set(p2 + 1, right + 1);

            e = ref.previousClearBit(p2);
            s = ref.previousSetBit(e);
            while (lower < s) {
                // sort (s, e]
                copy.set(s + 1, e + 1);
                Assertions.assertEquals(e, scanningCache.previousNonPivot(p2));
                Assertions.assertEquals(s, cache.previousPivot(e));
                p2 = s;
                e = ref.previousClearBit(p2);
                s = ref.previousSetBit(e);
            }

            // partition [left, p2) using bracket (lower, p2)
            copy.set(left, p2);
            final int unsorted2 = copy.previousClearBit(right);
            Assertions.assertTrue(unsorted2 < left, () -> "Bad right-to-left traversal: " + unsorted2);
        }
    }

    static Stream<Arguments> testSinglePoint() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // Ranges with no internal pivots
        builder.accept(Arguments.of(5, 5, new int[][] {{5}}));
        builder.accept(Arguments.of(5, 5, new int[][] {{2, 44}, {4, 6}, {5}}));

        return builder.build();
    }

    static Stream<Arguments> testSingleRange() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // Ranges with no internal pivots
        builder.accept(Arguments.of(5, 5, new int[][] {{5}}));
        builder.accept(Arguments.of(5, 5, new int[][] {{2, 44}, {4, 6}, {5}}));

        builder.accept(Arguments.of(5, 6, new int[][] {{5, 6}}));
        builder.accept(Arguments.of(5, 6, new int[][] {{2, 44}, {4, 6}, {5}}));
        builder.accept(Arguments.of(5, 6, new int[][] {{2, 44}, {5, 7}, {6}}));

        builder.accept(Arguments.of(5, 80, new int[][] {{5, 80}}));
        builder.accept(Arguments.of(5, 80, new int[][] {{2, 100}, {3, 90}, {4, 80}}));
        builder.accept(Arguments.of(5, 80, new int[][] {{2, 100}, {5, 90}}));

        // 1 internal pivot
        builder.accept(Arguments.of(5, 80, new int[][] {{63}}));
        builder.accept(Arguments.of(5, 80, new int[][] {{63, 64, 65, 66}}));
        builder.accept(Arguments.of(5, 80, new int[][] {{2, 63, 101}}));

        // multiple internal pivots
        builder.accept(Arguments.of(5, 80, new int[][] {{31, 63}}));
        builder.accept(Arguments.of(5, 80, new int[][] {{31, 44, 63}}));
        builder.accept(Arguments.of(5, 80, new int[][] {{31, 32, 33, 44, 63}}));

        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"testSetRange", "testSetRangeSinglePoint"})
    void testSetRangeUsingFilledIndexSetAsScanningPivotCache(int left, int right, int[][] indices) {
        // Collate all indices within [left, right]
        int[] allIndices = Arrays.stream(indices).flatMapToInt(Arrays::stream)
            .filter(i -> i >= left && i <= right).toArray();
        // Append left and right to ensure the range is supported
        final int n = allIndices.length;
        allIndices = Arrays.copyOfRange(allIndices, 0, n + 2);
        allIndices[n] = left;
        allIndices[n + 1] = right;
        final IndexSet set = IndexSet.of(allIndices);
        assertSetRange(left, right, indices, set.asScanningPivotCache(left, right));
    }

    @ParameterizedTest
    @MethodSource(value = {"testSetRange", "testSetRangeSinglePoint"})
    void testSetRangeUsingScanningPivotCache(int left, int right, int[][] indices) {
        assertSetRange(left, right, indices, IndexSet.createScanningPivotCache(left, right));
    }

    @ParameterizedTest
    @MethodSource(value = {"testSetRange", "testSetRangeSinglePoint"})
    void testSetRangeUsingIndexSet(int left, int right, int[][] indices) {
        assertSetRange(left, right, indices, IndexSet.ofRange(left, right));
    }

    @ParameterizedTest
    @MethodSource
    void testSetRangeSinglePoint(int left, int right, int[][] indices) {
        Assumptions.assumeTrue(left == right);
        assertSetRange(left, right, indices, PivotCaches.ofIndex(left));
    }

    @ParameterizedTest
    @MethodSource(value = {"testSetRange"})
    void testSetRangeUsingRange(int left, int right, int[][] indices) {
        Assumptions.assumeTrue(left < right);
        assertSetRange(left, right, indices, PivotCaches.ofRange(left, right));
    }

    @ParameterizedTest
    @MethodSource(value = {"testSingleRange", "testSinglePoint"})
    void testSetRangeUsingPairedIndex(int left, int right, int[][] indices) {
        if (left == right) {
            assertSingleRange(left, right, indices, PivotCaches.ofPairedIndex(left));
        } else if (left + 1 == right) {
            assertSingleRange(left, right, indices,
                PivotCaches.ofPairedIndex(left | Integer.MIN_VALUE));
        } else {
            Assumptions.abort("Not a paired index");
        }
    }

    /**
     * Assert caching pivots around a single range.
     *
     * <p>This test progressively adds more indices to the cache. It then verifies the
     * range is correctly bracketed and any internal pivots can be traversed as sorted
     * (pivot) and unsorted (non-pivot) regions.
     *
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param indices Batches of indices to add.
     * @param cache Pivot cache.
     */
    private static void assertSetRange(int left, int right, int[][] indices, PivotCache cache) {
        final BitSet ref = new BitSet();
        Assertions.assertEquals(left, cache.left());
        Assertions.assertEquals(right, cache.right());
        Assertions.assertEquals(ref.previousSetBit(left), cache.previousPivot(left));
        Assertions.assertEquals(ref.nextSetBit(left), cache.nextPivot(left));
        Assertions.assertEquals(Integer.MAX_VALUE, cache.nextPivotOrElse(right, Integer.MAX_VALUE));
        // This assumes the cache supports internal scanning
        // With current implementations this is true.
        for (final int[] batch : indices) {
            // BitSet uses an exclusive end
            ref.set(batch[0], batch[1] + 1);
            cache.add(batch[0], batch[1]);
            Assertions.assertEquals(left, cache.left());
            Assertions.assertEquals(right, cache.right());
            // Flanking pivots. Note: If actually used for partitioning these
            // must be updated from the possible -1 result.
            final int lower = ref.previousSetBit(left);
            final int upper = ref.nextSetBit(right);
            Assertions.assertEquals(lower, cache.previousPivot(left), "left flanking pivot");
            Assertions.assertEquals(upper, cache.nextPivot(right), "right flanking pivot");
            if (upper < 0) {
                Assertions.assertEquals(Integer.MAX_VALUE, cache.nextPivotOrElse(right, Integer.MAX_VALUE));
            }
            if (cache.sparse()) {
                continue;
            }
            // Simple test within the range
            for (int i = left; i <= right; i++) {
                Assertions.assertEquals(ref.get(i), cache.contains(i));
            }
        }
    }

    static Stream<Arguments> testSetRangeSinglePoint() {
        final Stream.Builder<Arguments> builder = Stream.builder();

        // Highest value for an index. Use sparingly as the test will create a BitSet
        // large enough to hold this value.
        final int max = Integer.MAX_VALUE - 1;

        builder.accept(Arguments.of(5, 5, new int[][] {{5, 5}}));
        builder.accept(Arguments.of(5, 5, new int[][] {{2, 44}, {4, 6}, {5, 5}}));
        builder.accept(Arguments.of(5, 5, new int[][] {{2, 44}, {5, 6}}));
        builder.accept(Arguments.of(5, 5, new int[][] {{2, 44}, {4, 5}}));
        builder.accept(Arguments.of(5, 5, new int[][] {{2, 44}, {6, 7}, {5, 6}}));
        builder.accept(Arguments.of(5, 5, new int[][] {{2, 44}, {3, 4}, {4, 5}}));
        builder.accept(Arguments.of(5, 5, new int[][] {{1, 2}, {7, 7}}));
        builder.accept(Arguments.of(5, 5, new int[][] {{0, 0}, {max, max}}));

        return builder.build();
    }

    static Stream<Arguments> testSetRange() {
        final Stream.Builder<Arguments> builder = Stream.builder();

        // Highest value for an index. Use sparingly as the test will create a BitSet
        // large enough to hold this value.
        final int max = Integer.MAX_VALUE - 1;

        builder.accept(Arguments.of(5, 6, new int[][] {{5, 6}}));
        builder.accept(Arguments.of(5, 6, new int[][] {{2, 3}, {44, 49}, {7, 8}, {5, 5}}));
        builder.accept(Arguments.of(5, 6, new int[][] {{0, 1}, {max - 1, max}}));

        // Bits span 2 longs
        builder.accept(Arguments.of(5, 80, new int[][] {{42, 42}, {1, 1}, {2, 2}, {90, 95}, {67, 68}}));
        builder.accept(Arguments.of(5, 80, new int[][] {{0, 1}, {max - 1, max}}));

        // Bits span 3 longs
        builder.accept(Arguments.of(5, 140, new int[][] {{42, 42}, {1, 1}, {2, 2}, {90, 95}, {187, 190}, {67, 68}}));
        builder.accept(Arguments.of(5, 140, new int[][] {{0, 0}, {max, max}}));

        return builder.build();
    }

    // TODO:
    // Test moveLeft()
}
