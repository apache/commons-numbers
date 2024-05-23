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
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link IndexIterator} implementations.
 */
class IndexIteratorTest {
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 42, Integer.MAX_VALUE - 1})
    void testSingleIndex(int k) {
        final IndexIterator iterator = IndexIterators.ofIndex(k);
        Assertions.assertEquals(k, iterator.left());
        Assertions.assertEquals(k, iterator.right());
        Assertions.assertEquals(k, iterator.end());
        Assertions.assertFalse(iterator.next());
        Assertions.assertFalse(iterator.positionAfter(k + 1));
        Assertions.assertFalse(iterator.positionAfter(k));
        Assertions.assertTrue(iterator.positionAfter(k - 1));
        Assertions.assertEquals(k, iterator.left());
        Assertions.assertEquals(k, iterator.right());
        Assertions.assertEquals(k, iterator.end());
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0",
        "0, 10",
        "5615236, 1263818376",
    })
    void testSingleInterval(int l, int r) {
        final IndexIterator iterator = IndexIterators.ofInterval(l, r);
        Assertions.assertEquals(l, iterator.left());
        Assertions.assertEquals(r, iterator.right());
        Assertions.assertEquals(r, iterator.end());
        Assertions.assertFalse(iterator.next());
        Assertions.assertFalse(iterator.positionAfter(r + 1));
        Assertions.assertFalse(iterator.positionAfter(r));
        Assertions.assertTrue(iterator.positionAfter(r - 1));
        Assertions.assertEquals(r > l, iterator.positionAfter(l));
        Assertions.assertEquals(r > l, iterator.positionAfter((l + r) >>> 1));
        Assertions.assertEquals(l, iterator.left());
        Assertions.assertEquals(r, iterator.right());
        Assertions.assertEquals(r, iterator.end());
    }

    @Test
    void testKeyIndexIteratorInvalidIndicesThrows() {
        assertInvalidIndicesThrows(KeyIndexIterator::of);
        // Invalid indices: not in [0, Integer.MAX_VALUE)
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> KeyIndexIterator.of(new int[] {-1, 2, 3}, 3));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> KeyIndexIterator.of(new int[] {1, 2, Integer.MAX_VALUE}, 3));
    }

    private static void assertInvalidIndicesThrows(BiFunction<int[], Integer, IndexIterator> constructor) {
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
    @MethodSource(value = {"testIterator"})
    void testKeyIndexIterator(int[] indices) {
        // This defaults to joining keys with a minimum separation of 2
        assertIterator(KeyIndexIterator::of, indices, 2, 0);
    }

    @ParameterizedTest
    @MethodSource(value = {"testIterator"})
    void testCompressedIndexIterator1(int[] indices) {
        assertIterator((k, n) -> CompressedIndexSet.iterator(1, k, k.length), indices, 0, 1);
    }

    @ParameterizedTest
    @MethodSource(value = {"testIterator"})
    void testCompressedIndexIterator2(int[] indices) {
        assertIterator((k, n) -> CompressedIndexSet.iterator(2, k, k.length), indices, 0, 2);
    }

    @ParameterizedTest
    @MethodSource(value = {"testIterator"})
    void testCompressedIndexIterator3(int[] indices) {
        assertIterator((k, n) -> CompressedIndexSet.iterator(3, k, k.length), indices, 0, 3);
    }

    @ParameterizedTest
    @MethodSource(value = {"testIterator"})
    void testCompressedIndexIterator5(int[] indices) {
        assertIterator((k, n) -> CompressedIndexSet.iterator(5, k, k.length), indices, 0, 5);
    }

    /**
     * Assert iterating along the indices.
     *
     * <p>Supported compressed indices. Each index is compressed by a power of 2, then
     * decompressed to create a range of indices {@code [from, to)}. See
     * {@link #createBitSet(int[], int)} for details of the indices that must be iterated
     * over.
     *
     * @param constructor Iterator constructor.
     * @param indices Indices.
     * @param separation Minimum separation between uncompressed indices.
     * @param compression Compression level (for compressed indices).
     */
    private static void assertIterator(BiFunction<int[], Integer, IndexIterator> constructor,
        int[] indices, int separation, int compression) {

        // Reference
        final BitSet set = createBitSet(indices, compression);
        final int first = indices[0];
        final int last = indices[indices.length - 1];

        IndexIterator iterator = constructor.apply(indices, indices.length);
        Assertions.assertEquals(last, iterator.end());
        // Check invariants
        Assertions.assertTrue(iterator.left() <= iterator.right());
        Assertions.assertTrue(iterator.right() <= iterator.end());

        // Expected
        int l = first;
        int r;
        if (compression == 0) {
            r = l;
            while (true) {
                final int n = set.nextSetBit(r + 1);
                if (n < 0 || r + separation < n) {
                    break;
                }
                r = n;
            }
        } else {
            r = Math.min(last, set.nextClearBit(l) - 1);
        }
        Assertions.assertEquals(l, iterator.left(), "left");
        Assertions.assertEquals(r, iterator.right(), "right");

        // Iterate
        while (iterator.right() < iterator.end()) {
            final int previous = iterator.right();

            Assertions.assertTrue(iterator.next());
            Assertions.assertTrue(previous < iterator.left(), "Did not advance");
            // Check invariants
            Assertions.assertTrue(iterator.left() <= iterator.right());
            Assertions.assertTrue(iterator.right() <= iterator.end());

            // Expected
            l = set.nextSetBit(previous + 1);
            if (compression == 0) {
                r = l;
                while (true) {
                    final int n = set.nextSetBit(r + 1);
                    if (n < 0 || r + separation < n) {
                        break;
                    }
                    r = n;
                }
            } else {
                r = Math.min(last, set.nextClearBit(l) - 1);
            }
            Assertions.assertEquals(l, iterator.left(), "left");
            Assertions.assertEquals(r, iterator.right(), "right");
        }
        Assertions.assertEquals(last, iterator.right());
        Assertions.assertFalse(iterator.next());
        Assertions.assertEquals(last, iterator.right());
        Assertions.assertFalse(iterator.next());

        // Test position after
        iterator = constructor.apply(indices, indices.length);
        Assertions.assertFalse(iterator.positionAfter(last + 1));
        Assertions.assertEquals(last, iterator.right());

        for (final int jump : new int[] {1, 2, 3}) {
            iterator = constructor.apply(indices, indices.length);
            final IndexIterator iterator2 = constructor.apply(indices, indices.length);

            for (int i = jump; i < indices.length; i += jump) {
                final int k = indices[i];
                if (k == last) {
                    Assertions.assertFalse(iterator.positionAfter(k));
                    Assertions.assertEquals(k, iterator.right());
                } else {
                    Assertions.assertTrue(iterator.positionAfter(k));
                    Assertions.assertTrue(k < iterator.right());
                }
                // Iterate using next. Ensures the sequence output is the same.
                boolean result = true;
                while (result && iterator2.right() <= k) {
                    result = iterator2.next();
                }
                // Allowed to be clipped to k+1
                if (iterator2.left() > k) {
                    Assertions.assertEquals(iterator2.left(), iterator.left(), () -> "left after " + k);
                } else {
                    Assertions.assertTrue(iterator.left() <= k + 1, () -> "left after " + k);
                }
                Assertions.assertEquals(iterator2.right(), iterator.right(), () -> "right after " + k);

                // Expected
                if (compression == 0) {
                    r = set.nextSetBit(Math.min(k + 1, last));
                    while (true) {
                        final int n = set.nextSetBit(r + 1);
                        if (n < 0 || r + separation < n) {
                            break;
                        }
                        r = n;
                    }
                    l = r;
                    while (true) {
                        final int n = set.previousSetBit(l - 1);
                        if (n < 0 || l - separation > n) {
                            break;
                        }
                        l = n;
                    }
                } else {
                    if (set.get(k + 1)) {
                        l = set.previousClearBit(k + 1) + 1;
                        r = Math.min(last, set.nextClearBit(k + 1) - 1);
                    } else {
                        if (k == last) {
                            r = last;
                            l = set.previousClearBit(last) + 1;
                        } else {
                            l = set.nextSetBit(k + 1);
                            r = Math.min(last, set.nextClearBit(l + 1) - 1);
                        }
                    }
                }
                // Allowed to be clipped to k+1
                if (l > k) {
                    Assertions.assertEquals(l, iterator.left(), "left");
                } else {
                    Assertions.assertTrue(iterator.left() <= k + 1, "left");
                }
                Assertions.assertEquals(r, iterator.right(), "right");
            }
            Assertions.assertFalse(iterator.positionAfter(last));
            Assertions.assertEquals(last, iterator.right());
            Assertions.assertFalse(iterator.positionAfter(last));
            Assertions.assertEquals(last, iterator.right());
        }
    }

    static Stream<int[]> testIterator() {
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        final Stream.Builder<int[]> builder = Stream.builder();
        builder.accept(new int[] {4});
        builder.accept(new int[] {4, 78});
        builder.accept(new int[] {4, 78, 999});
        builder.accept(new int[] {4, 78, 79, 999});
        builder.accept(new int[] {4, 5, 6, 7, 8});
        for (final int size : new int[] {10, 50, 500}) {
            for (final int n : new int[] {2, 5, 10}) {
                final int[] a = rng.ints(n, 0, size).distinct().sorted().toArray();
                builder.accept(a.clone());
                // Force use of index 0
                a[0] = 0;
                builder.accept(a);
            }
        }
        return builder.build();
    }

    /**
     * Creates the BitSet using the indices.
     *
     * <p>Compressed indices are created using {@code c = (i - min) >>> compression}.
     * This is then decompressed {@code from = (c << compression) + min}. The BitSet
     * has all bits set in {@code [from, from + (1 << compression))}.
     *
     * @param indices Indices.
     * @param compression Compression level.
     * @return the set
     */
    private static BitSet createBitSet(int[] indices, int compression) {
        final int max = indices[indices.length - 1] + 1 + (1 << compression);
        final BitSet set = new BitSet(max);
        if (compression == 0) {
            Arrays.stream(indices).forEach(set::set);
        } else {
            final int min = indices[0];
            final int width = 1 << compression;
            Arrays.stream(indices).forEach(i -> {
                i = (((i - min) >>> compression) << compression) + min;
                set.set(i, i + width);
            });
        }
        return set;
    }

    /**
     * Output the iterator intervals for the indices.
     */
    @ParameterizedTest
    @MethodSource
    @Disabled("This is not a test")
    void testIntervals(int compression, int[] indices) {
        IndexIterator iterator;
        if (compression == 0) {
            final int unique = Sorting.sortIndices(indices, indices.length);
            iterator = KeyIndexIterator.of(indices, unique);
        } else {
            iterator = CompressedIndexSet.iterator(compression, indices, indices.length);
        }
        TestUtils.printf("Compression %d%n", compression);
        do {
            final int l = iterator.left();
            final int r = iterator.right();
            TestUtils.printf("%d %d : %d%n", l, r, r - l + 1);
        } while (iterator.next());
    }

    static Stream<Arguments> testIntervals() {
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        final Stream.Builder<Arguments> builder = Stream.builder();
        int[] a;
        // Unsaturated: mean spacing = 200
        a = rng.ints(5, 0, 1000).toArray();
        for (final int c : new int[] {0, 2, 5}) {
            builder.accept(Arguments.of(c, a));
        }
        // Saturated: mean spacing = 10
        a = rng.ints(100, 0, 1000).toArray();
        for (final int c : new int[] {0, 2, 5}) {
            builder.accept(Arguments.of(c, a));
        }
        // Big data: mean spacing = 100
        a = rng.ints(1000, 0, 100000).toArray();
        for (final int c : new int[] {5, 8}) {
            builder.accept(Arguments.of(c, a));
        }
        return builder.build();
    }
}
