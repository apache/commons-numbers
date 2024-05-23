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

package org.apache.commons.numbers.arrays;

import java.util.Arrays;
import java.util.BitSet;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.PermutationSampler;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link UpdatingInterval} implementations.
 */
class UpdatingIntervalTest {

    /**
     * Create a KeyUpdatingInterval with the {@code indices}. This will create
     * distinct and sorted indices.
     *
     * @param indices Indices.
     * @param n Number of indices.
     * @return the interval
     * @throws IllegalArgumentException if {@code n == 0}
     */
    private static KeyUpdatingInterval createKeyUpdatingInterval(int[] indices, int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("No indices to define the range");
        }
        // If duplicates are not removed then the test will fail during the split
        // if the splitting index is a duplicate.
        final int[] k = Arrays.stream(indices).distinct().sorted().toArray();
        return new KeyUpdatingInterval(k, k.length);
    }

    /**
     * Create a BitIndexUpdatingInterval with the {@code indices}. The capacity is defined by the
     * range required to store the minimum and maximum index.
     *
     * @param indices Indices.
     * @param n Number of indices.
     * @return the interval
     * @throws IllegalArgumentException if {@code n == 0}
     */
    private static BitIndexUpdatingInterval createBitIndexUpdatingInterval(int[] indices, int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("No indices to define the range");
        }
        int min = indices[0];
        int max = min;
        for (int i = 1; i < n; i++) {
            min = Math.min(min, indices[i]);
            max = Math.max(max, indices[i]);
        }
        final BitIndexUpdatingInterval set = new BitIndexUpdatingInterval(min, max);
        for (int i = -1; ++i < n;) {
            set.set(indices[i]);
        }
        return set;
    }

    @ParameterizedTest
    @MethodSource(value = {"testIndices"})
    void testUpdateKeyInterval(int[] indices, int[] k) {
        assertUpdate(UpdatingIntervalTest::createKeyUpdatingInterval, indices, k);
    }

    @ParameterizedTest
    @MethodSource(value = {"testIndices"})
    void testUpdateBitIndexUpdatingInterval(int[] indices, int[] k) {
        // Skip this due to excess memory consumption
        Assumptions.assumeTrue(k[k.length - 1] < Integer.MAX_VALUE - 1);
        assertUpdate(UpdatingIntervalTest::createBitIndexUpdatingInterval, indices, k);
    }

    @ParameterizedTest
    @MethodSource(value = {"testIndices"})
    void testUpdateIndexSupport(int[] indices, int[] k) {
        final int l = k[0];
        final int r = k[k.length - 1];
        assertUpdate((x, n) -> IndexSupport.createUpdatingInterval(l, r, x, n), indices, k);
    }

    @ParameterizedTest
    @MethodSource(value = {"testIndices"})
    void testSplitKeyInterval(int[] indices, int[] k) {
        assertSplit(UpdatingIntervalTest::createKeyUpdatingInterval, indices, k);
    }

    @ParameterizedTest
    @MethodSource(value = {"testIndices"})
    void testSplitBitIndexUpdatingInterval(int[] indices, int[] k) {
        // Skip this due to excess memory consumption
        Assumptions.assumeTrue(k[k.length - 1] < Integer.MAX_VALUE - 1);
        assertSplit(UpdatingIntervalTest::createBitIndexUpdatingInterval, indices, k);
    }

    @ParameterizedTest
    @MethodSource(value = {"testIndices"})
    void testSplitIndexSupport(int[] indices, int[] k) {
        final int l = k[0];
        final int r = k[k.length - 1];
        assertSplit((x, n) -> IndexSupport.createUpdatingInterval(l, r, x, n), indices, k);
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
            int[] indices, int[] k) {
        UpdatingInterval interval = constructor.apply(indices.clone(), indices.length);
        final int nm1 = k.length - 1;
        Assertions.assertEquals(k[0], interval.left());
        Assertions.assertEquals(k[nm1], interval.right());

        // Use updateLeft to reduce the interval to length 1
        for (int i = 1; i < k.length; i++) {
            // rounded down median between indices
            final int m = (k[i - 1] + k[i]) >>> 1;
            interval.updateLeft(m + 1);
            Assertions.assertEquals(k[i], interval.left());
        }
        Assertions.assertEquals(interval.left(), interval.right());

        // Use updateRight to reduce the interval to length 1
        interval = constructor.apply(indices.clone(), indices.length);
        for (int i = k.length; --i > 0;) {
            // rounded up median between indices
            final int m = 1 + ((k[i - 1] + k[i]) >>> 1);
            interval.updateRight(m - 1);
            Assertions.assertEquals(k[i - 1], interval.right());
        }
        Assertions.assertEquals(interval.left(), interval.right());
    }

    /**
     * Assert the {@link UpdatingInterval#splitLeft(int, int)} method.
     * These are tested by successive calls to split the interval around the mid-point.
     *
     * @param constructor Interval constructor.
     * @param indices Indices.
     * @param k Sorted unique indices.
     */
    private static void assertSplit(BiFunction<int[], Integer, UpdatingInterval> constructor, int[] indices, int[] k) {
        assertSplitMedian(constructor.apply(indices.clone(), indices.length),
            k, 0, k.length - 1);
        assertSplitMiddleIndices(constructor.apply(indices.clone(), indices.length),
            k, 0, k.length - 1);
    }

    /**
     * Assert a split using the median value between the split median.
     *
     * @param interval Interval.
     * @param indices Indices.
     * @param i Low index into the indices (inclusive).
     * @param j High index into the indices (inclusive).
     */
    private static void assertSplitMedian(UpdatingInterval interval, int[] indices, int i, int j) {
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

        final UpdatingInterval leftInterval = interval.splitLeft(m, m);
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
    private static void assertSplitMiddleIndices(UpdatingInterval interval, int[] indices, int i, int j) {
        if (i + 3 >= j) {
            // Cannot split - not two indices between low and high index
            return;
        }
        // Middle two indices
        final int m1 = (i + j) >>> 1;
        final int m2 = m1 + 1;

        final int left = interval.left();
        final int right = interval.right();
        final UpdatingInterval leftInterval = interval.splitLeft(indices[m1], indices[m2]);
        Assertions.assertEquals(left, leftInterval.left());
        Assertions.assertEquals(indices[m1 - 1], leftInterval.right());
        Assertions.assertEquals(indices[m2 + 1], interval.left());
        Assertions.assertEquals(right, interval.right());

        // Recurse
        assertSplitMiddleIndices(leftInterval, indices, i, m1 - 1);
        assertSplitMiddleIndices(interval, indices, m2 + 1, j);
    }

    static Stream<Arguments> testIndices() {
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        final Stream.Builder<int[]> builder = Stream.builder();
        // Create unique sorted indices
        builder.accept(new int[] {4});
        builder.accept(new int[] {4, 78});
        builder.accept(new int[] {4, 78, 999});
        builder.accept(new int[] {4, 78, 79, 999});
        builder.accept(new int[] {4, 5, 6, 7, 8});
        for (final int size : new int[] {25, 100, 400, 800}) {
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
        // Builder contains sorted unique indices.
        // Final required arguments are: [indices, sorted unique indices]
        // Expand to have indices as: sorted, sorted with duplicates, unsorted, unsorted with duplicates
        Stream.Builder<Arguments> out = Stream.builder();
        builder.build().forEach(a -> {
            final int[] c = a.clone();
            out.accept(Arguments.of(a.clone(), c.clone()));
            // Duplicates
            final int[] b = Arrays.copyOf(a, a.length * 2);
            for (int i = a.length; i < b.length; i++) {
                b[i] = a[rng.nextInt(a.length)];
            }
            Arrays.sort(b);
            out.accept(Arguments.of(b.clone(), c.clone()));
            // Unsorted
            PermutationSampler.shuffle(rng, a);
            PermutationSampler.shuffle(rng, b);
            out.accept(Arguments.of(a, c.clone()));
            out.accept(Arguments.of(b, c.clone()));
        });
        return out.build();
    }

    @Test
    void testIndexIntervalCreate() {
        // The above tests verify the UpdatingInterval implementations all work.
        // Hit all paths in the analysis performed to create an interval.

        // 1 key
        Assertions.assertEquals(KeyUpdatingInterval.class,
            IndexSupport.createUpdatingInterval(0, 2, new int[] {1}, 1).getClass());

        // 2 close keys
        Assertions.assertEquals(KeyUpdatingInterval.class,
            IndexSupport.createUpdatingInterval(0, 2, new int[] {2, 1}, 2).getClass());
        Assertions.assertEquals(KeyUpdatingInterval.class,
            IndexSupport.createUpdatingInterval(0, 2, new int[] {1, 2}, 2).getClass());

        // 2 unsorted keys
        Assertions.assertEquals(KeyUpdatingInterval.class,
            IndexSupport.createUpdatingInterval(0, 1000, new int[] {200, 1}, 2).getClass());

        // Sorted number of keys saturating the range
        Assertions.assertEquals(KeyUpdatingInterval.class,
            IndexSupport.createUpdatingInterval(0, 20, new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}, 11).getClass());
        // Sorted keys with duplicates
        Assertions.assertEquals(KeyUpdatingInterval.class,
            IndexSupport.createUpdatingInterval(0, 10, new int[] {1, 1, 2, 2, 3, 3, 4, 4, 5, 5}, 10).getClass());
        Assertions.assertEquals(KeyUpdatingInterval.class,
            IndexSupport.createUpdatingInterval(0, 1000, new int[] {100, 100, 200, 200, 300, 300, 400, 400, 500, 500}, 10).getClass());
        // Small number of keys saturating the range
        Assertions.assertEquals(KeyUpdatingInterval.class,
            IndexSupport.createUpdatingInterval(0, 20, new int[] {11, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1}, 11).getClass());
        // Keys over a huge range
        Assertions.assertEquals(KeyUpdatingInterval.class,
            IndexSupport.createUpdatingInterval(0, Integer.MAX_VALUE - 1,
                new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, Integer.MAX_VALUE - 1}, 11).getClass());

        // Small number of sorted keys over a moderate range
        int[] k = IntStream.range(0, 30).map(i -> i * 64) .toArray();
        Assertions.assertEquals(KeyUpdatingInterval.class,
            IndexSupport.createUpdatingInterval(0, 30 * 64, k.clone(), k.length).getClass());
        // Same keys not sorted
        reverse(k, 0, k.length);
        Assertions.assertEquals(BitIndexUpdatingInterval.class,
            IndexSupport.createUpdatingInterval(0, 30 * 64, k.clone(), k.length).getClass());
        // Same keys over a huge range
        k[k.length - 1] = Integer.MAX_VALUE - 1;
        Assertions.assertEquals(KeyUpdatingInterval.class,
            IndexSupport.createUpdatingInterval(0, Integer.MAX_VALUE - 1, k, k.length).getClass());

        // Moderate number of sorted keys over a moderate range
        k = IntStream.range(0, 3000).map(i -> i * 64) .toArray();
        Assertions.assertEquals(KeyUpdatingInterval.class,
            IndexSupport.createUpdatingInterval(0, 3000 * 64, k.clone(), k.length).getClass());
        // Same keys not sorted
        reverse(k, 0, k.length);
        Assertions.assertEquals(BitIndexUpdatingInterval.class,
            IndexSupport.createUpdatingInterval(0, 3000 * 64, k.clone(), k.length).getClass());
        // Same keys over a huge range
        k[k.length - 1] = Integer.MAX_VALUE - 1;
        Assertions.assertEquals(KeyUpdatingInterval.class,
            IndexSupport.createUpdatingInterval(0, Integer.MAX_VALUE - 1, k.clone(), k.length).getClass());
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
