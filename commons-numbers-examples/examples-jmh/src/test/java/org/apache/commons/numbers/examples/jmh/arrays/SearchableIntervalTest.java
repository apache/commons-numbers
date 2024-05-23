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

import java.util.BitSet;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link SearchableInterval} implementations.
 */
class SearchableIntervalTest {
    @Test
    void testAnyIndex() {
        final SearchableInterval interval = IndexIntervals.anyIndex();
        // Full range of valid indices.
        // Note Integer.MAX_VALUE is not a valid array index.
        Assertions.assertEquals(0, interval.left());
        Assertions.assertEquals(Integer.MAX_VALUE - 1, interval.right());
        final int[] index = {0};
        for (final int i : new int[] {0, 1, 2, 42, 678268, Integer.MAX_VALUE - 1}) {
            Assertions.assertEquals(i, interval.previousIndex(i));
            Assertions.assertEquals(i, interval.nextIndex(i));
            Assertions.assertEquals(i - 1, interval.split(i, i, index));
            Assertions.assertEquals(i + 1, index[0]);
        }
    }

    @Test
    void testAnyIndex2() {
        final SearchableInterval2 interval = IndexIntervals.anyIndex2();
        // Full range of valid indices.
        // Note Integer.MAX_VALUE is not a valid array index.
        Assertions.assertEquals(0, interval.start());
        Assertions.assertEquals(Integer.MAX_VALUE - 1, interval.end());
        final int[] index = {0};
        for (final int i : new int[] {0, 1, 2, 42, 678268, Integer.MAX_VALUE - 1}) {
            Assertions.assertEquals(i, interval.previous(i, i));
            Assertions.assertEquals(i, interval.next(i, i));
            Assertions.assertEquals(i - 1, interval.split(-1, Integer.MAX_VALUE, i, i, index));
            Assertions.assertEquals(i + 1, index[0]);
        }
    }

    @Test
    void testScanningKeySearchableIntervalInvalidIndicesThrows() {
        assertInvalidIndicesThrows(ScanningKeyInterval::of);
        // Invalid indices: not in [0, Integer.MAX_VALUE)
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ScanningKeyInterval.of(new int[] {-1, 2, 3}, 3));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ScanningKeyInterval.of(new int[] {1, 2, Integer.MAX_VALUE}, 3));
    }

    @Test
    void testBinarySearchKeySearchableIntervalInvalidIndicesThrows() {
        assertInvalidIndicesThrows(BinarySearchKeyInterval::of);
    }

    private static void assertInvalidIndicesThrows(BiFunction<int[], Integer, SearchableInterval> constructor) {
        // Size zero
        Assertions.assertThrows(IllegalArgumentException.class, () -> constructor.apply(new int[0], 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> constructor.apply(new int[10], 0));
        // Not sorted
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> constructor.apply(new int[] {3, 2, 1}, 3));
        // Not unique
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> constructor.apply(new int[] {1, 2, 2, 3}, 4));
    }

    @ParameterizedTest
    @MethodSource(value = {"testPreviousNextIndex"})
    void testPreviousNextScanningKeySearchableInterval(int[] indices) {
        assertPreviousNextIndex(ScanningKeyInterval.of(indices, indices.length), indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPreviousNextIndex"})
    void testPreviousNextBinarySearchKeySearchableInterval(int[] indices) {
        assertPreviousNextIndex(BinarySearchKeyInterval.of(indices, indices.length), indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPreviousNextIndex"})
    void testPreviousNextIndexSet(int[] indices) {
        // Skip this due to excess memory consumption
        Assumptions.assumeTrue(indices[indices.length - 1] < Integer.MAX_VALUE - 1);
        assertPreviousNextIndex(IndexSet.of(indices, indices.length), indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPreviousNextIndex"})
    void testPreviousNextSearchableInterval(int[] indices) {
        assertPreviousNextIndex(IndexIntervals.createSearchableInterval(indices, indices.length), indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPreviousNextIndex"})
    void testPreviousNextCompressedIndexSet(int[] indices) {
        // Skip this due to excess memory consumption
        Assumptions.assumeTrue(indices[indices.length - 1] < Integer.MAX_VALUE - 1);
        final int[] index = {0};
        // The test is adjusted as the compressed index set does not store all indices.
        // So we scan previous and next instead and check we do not miss the index.
        for (final int c : new int[] {1, 2, 3}) {
            final SearchableInterval interval = CompressedIndexSet.of(c, indices, indices.length);
            final int nm1 = indices.length - 1;
            Assertions.assertEquals(indices[0], interval.left());
            Assertions.assertEquals(indices[nm1], interval.right());
            // Number of steps between indices should be less twice the
            // compression level minus 1. Max steps required @ compression 2:
            // -------i---------n-----
            // -------cccc---cccc----  Compressed indices cover 4 real indices
            final int maxSteps = (1 << (c + 1)) - 1;
            for (int i = 0; i < indices.length; i++) {
                if (i > 0) {
                    final int prev = indices[i - 1];
                    int steps = 1;
                    int j = interval.previousIndex(indices[i] - 1);
                    // Splitting is tested against previous and next
                    if (i < nm1) {
                        Assertions.assertEquals(j, interval.split(indices[i], indices[i], index));
                        Assertions.assertEquals(interval.nextIndex(indices[i] + 1), index[0]);
                        if (i > 1) {
                            final int upper = index[0];
                            Assertions.assertEquals(interval.previousIndex(indices[i - 1] - 1),
                                interval.split(indices[i - 1], indices[i], index));
                            Assertions.assertEquals(upper, index[0]);
                        }
                    }
                    // Scan previous
                    while (j > prev) {
                        steps++;
                        j = interval.previousIndex(j - 1);
                    }
                    Assertions.assertEquals(prev, j);
                    Assertions.assertTrue(steps <= maxSteps);
                }
                Assertions.assertEquals(indices[i], interval.previousIndex(indices[i]));
                Assertions.assertEquals(indices[i], interval.nextIndex(indices[i]));
                if (i < nm1) {
                    final int next = indices[i + 1];
                    int steps = 1;
                    int j = interval.nextIndex(indices[i] + 1);
                    while (j < next) {
                        steps++;
                        j = interval.nextIndex(j + 1);
                    }
                    Assertions.assertEquals(next, j);
                    Assertions.assertTrue(steps <= maxSteps);
                }
            }
        }
    }

    private static void assertPreviousNextIndex(SearchableInterval interval, int[] indices) {
        final int nm1 = indices.length - 1;
        Assertions.assertEquals(indices[0], interval.left());
        Assertions.assertEquals(indices[nm1], interval.right());
        final int[] index = {0};
        // Note: For performance scanning is not supported outside the range
        for (int i = 0; i < indices.length; i++) {
            if (i > 0) {
                Assertions.assertEquals(indices[i - 1], interval.previousIndex(indices[i] - 1));
                // Split on an index: Cannot call when k == left or k == right
                if (i < nm1) {
                    Assertions.assertEquals(indices[i - 1], interval.split(indices[i], indices[i], index));
                    Assertions.assertEquals(indices[i + 1], index[0]);
                    if (indices[i - 1] < indices[i] - 1 && indices[i] < indices[i + 1] - 1) {
                        // Split between indices
                        final int middle1 = (indices[i - 1] + indices[i]) >>> 1;
                        final int middle2 = (indices[i] + indices[i + 1]) >>> 1;
                        Assertions.assertEquals(indices[i - 1], interval.split(middle1, middle2, index));
                        Assertions.assertEquals(indices[i + 1], index[0]);
                    }
                }
            }
            Assertions.assertEquals(indices[i], interval.previousIndex(indices[i]));
            Assertions.assertEquals(indices[i], interval.nextIndex(indices[i]));
            if (i < nm1) {
                Assertions.assertEquals(indices[i + 1], interval.nextIndex(indices[i] + 1));
            }
        }
    }

    static Stream<int[]> testPreviousNextIndex() {
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        final Stream.Builder<int[]> builder = Stream.builder();
        builder.accept(new int[] {4});
        builder.accept(new int[] {4, 78});
        builder.accept(new int[] {4, 78, 999});
        builder.accept(new int[] {4, 78, 79, 999});
        builder.accept(new int[] {4, 5, 6, 7, 8});
        for (final int size : new int[] {25, 100, 400}) {
            final BitSet set = new BitSet(size);
            for (final int n : new int[] {2, size / 8, size / 4, size / 2}) {
                set.clear();
                rng.ints(n, 0, size).forEach(set::set);
                final int[] a = set.stream().toArray();
                builder.accept(a.clone());
                // Force use of index 0 and max index
                a[0] = 0;
                a[a.length - 1] = Integer.MAX_VALUE - 1;
                builder.accept(a);
            }
        }
        return builder.build();
    }

    @Test
    void testSearchableIntervalCreate() {
        // The above tests verify the SearchableInterval implementations all work.
        // Hit all paths in the key analysis performed to create an interval.

        // Small number of keys; no analysis
        Assertions.assertEquals(ScanningKeyInterval.class,
            IndexIntervals.createSearchableInterval(new int[] {1}, 1).getClass());

        // >10 keys for key analysis

        // Small number of keys saturating the range
        Assertions.assertEquals(IndexSet.class,
            IndexIntervals.createSearchableInterval(new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}, 11).getClass());
        // Keys over a huge range
        Assertions.assertEquals(ScanningKeyInterval.class,
            IndexIntervals.createSearchableInterval(new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, Integer.MAX_VALUE - 1}, 11).getClass());

        // Small number of keys over a moderate range
        int[] k = IntStream.range(0, 30).map(i -> i * 64) .toArray();
        Assertions.assertEquals(IndexSet.class,
            IndexIntervals.createSearchableInterval(k.clone(), k.length).getClass());
        // Same keys over a huge range
        k[k.length - 1] = Integer.MAX_VALUE - 1;
        Assertions.assertEquals(ScanningKeyInterval.class,
            IndexIntervals.createSearchableInterval(k, k.length).getClass());

        // Moderate number of keys over a moderate range
        k = IntStream.range(0, 3000).map(i -> i * 64) .toArray();
        Assertions.assertEquals(IndexSet.class,
            IndexIntervals.createSearchableInterval(k.clone(), k.length).getClass());
        // Same keys over a huge range - switch to binary search on the keys
        k[k.length - 1] = Integer.MAX_VALUE - 1;
        Assertions.assertEquals(BinarySearchKeyInterval.class,
            IndexIntervals.createSearchableInterval(k, k.length).getClass());
    }
}
