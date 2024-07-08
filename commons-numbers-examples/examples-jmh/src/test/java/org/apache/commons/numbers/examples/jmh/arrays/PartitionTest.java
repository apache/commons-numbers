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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.numbers.arrays.Selection;
import org.apache.commons.numbers.examples.jmh.arrays.Partition.AdaptMode;
import org.apache.commons.numbers.examples.jmh.arrays.Partition.EdgeSelectStrategy;
import org.apache.commons.numbers.examples.jmh.arrays.Partition.ExpandStrategy;
import org.apache.commons.numbers.examples.jmh.arrays.Partition.KeyStrategy;
import org.apache.commons.numbers.examples.jmh.arrays.Partition.LinearStrategy;
import org.apache.commons.numbers.examples.jmh.arrays.Partition.PairedKeyStrategy;
import org.apache.commons.numbers.examples.jmh.arrays.Partition.SPStrategy;
import org.apache.commons.numbers.examples.jmh.arrays.SelectionPerformance.AbstractDataSource;
import org.apache.commons.numbers.examples.jmh.arrays.SelectionPerformance.AbstractDataSource.Distribution;
import org.apache.commons.numbers.examples.jmh.arrays.SelectionPerformance.AbstractDataSource.Modification;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.ArraySampler;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link Partition}.
 */
class PartitionTest {
    /** Default single pivot strategy. */
    private static final PivotingStrategy SP = PivotingStrategy.MEDIAN_OF_3;
    /** Default single pivot strategy. */
    private static final DualPivotingStrategy DP = DualPivotingStrategy.SORT_5;
    /** Default minimum quick select length. */
    private static final int QS = 3;
    /** Default minimum quick select length for dual pivot. */
    private static final int QS2 = 5;
    /** Default heap select constant. */
    private static final int EC = 2;
    /** Default sub-sampling size. */
    private static final int SU = 600;

    /**
     * Partition function. Used to test different implementations.
     */
    private interface DoubleRangePartitionFunction {
        /**
         * Partition the array such that range of indices {@code [ka, kb]} correspond to
         * their correctly sorted value in the equivalent fully sorted array. For all
         * indices {@code k} and any index {@code i}:
         *
         * <pre>{@code
         * data[i < k] <= data[k] <= data[k < i]
         * }</pre>
         *
         * @param a Data array to use to find out the K<sup>th</sup> value.
         * @param left Lower bound (inclusive).
         * @param right Upper bound (inclusive).
         * @param ka Lower index to select.
         * @param kb Upper index to select.
         */
        void partition(double[] a, int left, int right, int ka, int kb);
    }

    /**
     * Partition function. Used to test different implementations.
     */
    private interface DoublePartitionFunction {
        /**
         * Partition the array such that indices {@code k} correspond to their correctly
         * sorted value in the equivalent fully sorted array. For all indices {@code k}
         * and any index {@code i}:
         *
         * <pre>{@code
         * data[i < k] <= data[k] <= data[k < i]
         * }</pre>
         *
         * <p>This method allows variable length indices using a count of the indices to
         * process.
         *
         * @param a Values.
         * @param k Indices.
         * @param n Count of indices.
         */
        void partition(double[] a, int[] k, int n);
    }

    /**
     * Partition function. Used to test different implementations.
     */
    private interface DoublePartitionFunction2 {
        /**
         * Partition the array such that indices {@code k} correspond to their correctly
         * sorted value in the equivalent fully sorted array. For all indices {@code k}
         * and any index {@code i}:
         *
         * <pre>{@code
         * data[i < k] <= data[k] <= data[k < i]
         * }</pre>
         *
         * @param a Values.
         * @param k Indices.
         */
        void partition(double[] a, int... k);
    }

    @ParameterizedTest
    @MethodSource
    void testSortNaN(double[] values) {
        final double[] sorted = sort(values);
        final int last = Partition.sortNaN(values);
        // index of last non-NaN
        int i = sorted.length;
        while (--i >= 0) {
            if (!Double.isNaN(sorted[i])) {
                break;
            }
        }
        Assertions.assertEquals(i, last);
        // Check the data is the same
        Arrays.sort(values);
        Assertions.assertArrayEquals(sorted, values, "Data destroyed");
    }

    static Stream<double[]> testSortNaN() {
        final Stream.Builder<double[]> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create();
        final double nan = Double.NaN;
        builder.add(new double[0]);
        builder.add(new double[] {1.23});
        builder.add(new double[] {nan});
        builder.add(new double[] {nan, nan});
        builder.add(new double[] {nan, nan, nan});
        for (final int size : new int[] {2, 5}) {
            final double[] values = rng.doubles(size).toArray();
            builder.add(values.clone());
            // Random NaNs
            for (int n = 1; n < size; n++) {
                final double[] x = values.clone();
                Arrays.fill(x, 0, n, nan);
                for (int i = 0; i < 5; i++) {
                    builder.add(ArraySampler.shuffle(rng, x).clone());
                }
            }
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"testSelectMinMax"})
    void testSelectMin(double[] values, int from, int to) {
        assertPartitionRange(sort(values, from, to),
            (a, l, r, ka, kb) -> Partition.selectMin(values, from, to),
            values, from, to, from, from);
    }

    @ParameterizedTest
    @MethodSource(value = {"testSelectMinMax"})
    void testSelectMax(double[] values, int from, int to) {
        assertPartitionRange(sort(values, from, to),
            (a, l, r, ka, kb) -> Partition.selectMax(values, from, to),
            values, from, to, to, to);
    }

    static Stream<Arguments> testSelectMinMax() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of(new double[] {1, 2, 3, 4, 5}, 0, 4));
        builder.add(Arguments.of(new double[] {5, 4, 3, 2, 1}, 0, 4));
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create();
        for (final int size : new int[] {5, 10}) {
            final double[] values = rng.doubles(size).toArray();
            builder.add(Arguments.of(values.clone(), 0, size - 1));
            builder.add(Arguments.of(values.clone(), size >>> 1, size - 1));
            builder.add(Arguments.of(values.clone(), 1, size >>> 1));
        }
        builder.add(Arguments.of(new double[] {-0.0, 0.0}, 0, 1));
        builder.add(Arguments.of(new double[] {0.0, -0.0}, 0, 1));
        builder.add(Arguments.of(new double[] {-0.0, -0.0}, 0, 1));
        builder.add(Arguments.of(new double[] {0.0, 0.0}, 0, 1));
        builder.add(Arguments.of(new double[] {0.0, -0.0, 0.0, -0.0}, 0, 3));
        builder.add(Arguments.of(new double[] {-0.0, 0.0, -0.0, 0.0}, 0, 3));
        builder.add(Arguments.of(new double[] {0.0, -0.0, -0.0, 0.0}, 0, 3));
        builder.add(Arguments.of(new double[] {-0.0, 0.0, 0.0, -0.0}, 0, 3));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = "testSelectMinMax2")
    void testSelectMin2IgnoreZeros(double[] values, int from, int to) {
        assertPartitionRange(sort(values, from, to),
            (a, l, r, ka, kb) -> {
                replaceNegativeZeros(values, from, to);
                Partition.selectMin2IgnoreZeros(values, from, to);
                restoreNegativeZeros(values, from, to);
            },
            values, from, to, from, from + 1);
    }

    @ParameterizedTest
    @MethodSource(value = "testSelectMinMax2")
    void testSelectMax2IgnoreZeros(double[] values, int from, int to) {
        assertPartitionRange(sort(values, from, to),
            (a, l, r, ka, kb) -> {
                replaceNegativeZeros(values, from, to);
                Partition.selectMax2IgnoreZeros(values, from, to);
                restoreNegativeZeros(values, from, to);
            },
            values, from, to, to - 1, to);
    }

    static Stream<Arguments> testSelectMinMax2() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final double[] values = {-0.0, 0.0, 1};
        final double x = Double.NaN;
        final double y = 42;
        for (final double a : values) {
            for (final double b : values) {
                builder.add(Arguments.of(new double[] {a, b}, 0, 1));
                builder.add(Arguments.of(new double[] {x, a, b, y}, 1, 2));
                for (final double c : values) {
                    builder.add(Arguments.of(new double[] {a, b, c}, 0, 2));
                    builder.add(Arguments.of(new double[] {x, a, b, c, y}, 1, 3));
                    for (final double d : values) {
                        builder.add(Arguments.of(new double[] {a, b, c, d}, 0, 3));
                        builder.add(Arguments.of(new double[] {x, a, b, c, d, y}, 1, 4));
                    }
                }
            }
        }
        builder.add(Arguments.of(new double[] {-1, -1, -1, 4, 3, 2, 1, y}, 3, 6));
        builder.add(Arguments.of(new double[] {1, 2, 3, 4, 5}, 0, 4));
        builder.add(Arguments.of(new double[] {5, 4, 3, 2, 1}, 0, 4));
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create();
        for (final int size : new int[] {5, 10}) {
            final double[] a = rng.doubles(size).toArray();
            builder.add(Arguments.of(a.clone(), 0, size - 1));
            builder.add(Arguments.of(a.clone(), size >>> 1, size - 1));
            builder.add(Arguments.of(a.clone(), 1, size >>> 1));
        }
        builder.add(Arguments.of(new double[] {-0.0, 0.0}, 0, 1));
        builder.add(Arguments.of(new double[] {0.0, -0.0}, 0, 1));
        builder.add(Arguments.of(new double[] {-0.0, -0.0}, 0, 1));
        builder.add(Arguments.of(new double[] {0.0, 0.0}, 0, 1));
        builder.add(Arguments.of(new double[] {0.0, -0.0, 0.0, -0.0}, 0, 3));
        builder.add(Arguments.of(new double[] {-0.0, 0.0, -0.0, 0.0}, 0, 3));
        builder.add(Arguments.of(new double[] {0.0, -0.0, -0.0, 0.0}, 0, 3));
        builder.add(Arguments.of(new double[] {-0.0, 0.0, 0.0, -0.0}, 0, 3));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"testHeapSelect", "testSelectMinMax", "testSelectMinMax2"})
    void testHeapSelectLeft(double[] values, int from, int to) {
        final double[] sorted = sort(values, from, to);

        final double[] x = values.clone();
        replaceNegativeZeros(x, from, to);
        final DoubleRangePartitionFunction fun = (a, l, r, ka, kb) -> {
            Partition.heapSelectLeft(a, l, r, kb, kb - ka);
            restoreNegativeZeros(a, l, r);
        };

        for (int k = from; k <= to; k++) {
            assertPartitionRange(sorted, fun, x.clone(), from, to, k, k);
            if (k > from) {
                // Sort an extra 1
                assertPartitionRange(sorted, fun, x.clone(), from, to, k - 1, k);
                if (k > from + 1) {
                    // Sort all
                    // Test clipping with k < from
                    assertPartitionRange(sorted, fun, x.clone(), from, to, from - 23, k);
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"testHeapSelect", "testSelectMinMax", "testSelectMinMax2"})
    void testHeapSelectRight(double[] values, int from, int to) {
        final double[] sorted = sort(values, from, to);

        final double[] x = values.clone();
        replaceNegativeZeros(x, from, to);
        final DoubleRangePartitionFunction fun = (a, l, r, ka, kb) -> {
            Partition.heapSelectRight(a, l, r, ka, kb - ka);
            restoreNegativeZeros(a, l, r);
        };

        for (int k = from; k <= to; k++) {
            assertPartitionRange(sorted, fun, x.clone(), from, to, k, k);
            if (k < to) {
                // Sort an extra 1
                assertPartitionRange(sorted, fun, x.clone(), from, to, k, k + 1);
                if (k < to - 1) {
                    // Sort all
                    // Test clipping with k > to
                    assertPartitionRange(sorted, fun, x.clone(), from, to, k, to + 23);
                }
            }
        }
    }

    static Stream<Arguments> testHeapSelect() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of(new double[] {1}, 0, 0));
        builder.add(Arguments.of(new double[] {3, 2, 1}, 1, 1));
        builder.add(Arguments.of(new double[] {2, 1}, 0, 1));
        builder.add(Arguments.of(new double[] {4, 3, 2, 1}, 1, 2));
        builder.add(Arguments.of(new double[] {-1, 0.0, -0.0, -0.0, 1}, 0, 4));
        builder.add(Arguments.of(new double[] {-1, 0.0, -0.0, -0.0, 1}, 0, 2));
        builder.add(Arguments.of(new double[] {1, 0.0, -0.0, -0.0, -1}, 0, 4));
        builder.add(Arguments.of(new double[] {-1, 2, -3, 4, -4, 3, -2, 1}, 1, 6));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"testHeapSelect", "testSelectMinMax", "testSelectMinMax2"})
    void testHeapSelectLeft2(double[] values, int from, int to) {
        final double[] sorted = sort(values, from, to);

        final double[] x = values.clone();
        replaceNegativeZeros(x, from, to);
        final DoubleRangePartitionFunction fun = (a, l, r, ka, kb) -> {
            Partition.heapSelectLeft2(a, l, r, ka, kb);
            restoreNegativeZeros(a, l, r);
        };

        for (int k = from; k <= to; k++) {
            assertPartitionRange(sorted, fun, x.clone(), from, to, k, k);
            if (k > from) {
                // Sort an extra 1
                assertPartitionRange(sorted, fun, x.clone(), from, to, k - 1, k);
                if (k > from + 1) {
                    // Sort all
                    // Test clipping with k < from
                    assertPartitionRange(sorted, fun, x.clone(), from, to, from - 23, k);
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"testHeapSelect", "testSelectMinMax", "testSelectMinMax2"})
    void testHeapSelectRight2(double[] values, int from, int to) {
        final double[] sorted = sort(values, from, to);

        final double[] x = values.clone();
        replaceNegativeZeros(x, from, to);
        final DoubleRangePartitionFunction fun = (a, l, r, ka, kb) -> {
            Partition.heapSelectRight2(a, l, r, ka, kb);
            restoreNegativeZeros(a, l, r);
        };

        for (int k = from; k <= to; k++) {
            assertPartitionRange(sorted, fun, x.clone(), from, to, k, k);
            if (k < to) {
                // Sort an extra 1
                assertPartitionRange(sorted, fun, x.clone(), from, to, k, k + 1);
                if (k < to - 1) {
                    // Sort all
                    // Test clipping with k > to
                    assertPartitionRange(sorted, fun, x.clone(), from, to, k, to + 23);
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource
    void testHeapSelectPair(double[] values, int from, int to, int k1, int k2) {
        final double[] sorted = sort(values, from, to);
        Partition.heapSelectPair(values, from, to, k1, k2);
        Assertions.assertEquals(sorted[k1], values[k1]);
        Assertions.assertEquals(sorted[k2], values[k2]);
        // Check the data is the same
        Arrays.sort(values, from, to + 1);
        Assertions.assertArrayEquals(sorted, values, "Data destroyed");
    }

    static Stream<Arguments> testHeapSelectPair() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of(new double[] {-1, 2, -3, 4, -4, 3, -2, 1}, 0, 7, 1, 2));
        builder.add(Arguments.of(new double[] {-1, 2, -3, 4, -4, 3, -2, 1}, 0, 7, 2, 2));
        builder.add(Arguments.of(new double[] {-1, 2, -3, 4, -4, 3, -2, 1}, 0, 7, 5, 7));
        builder.add(Arguments.of(new double[] {-1, 2, -3, 4, -4, 3, -2, 1}, 0, 7, 1, 6));
        builder.add(Arguments.of(new double[] {-1, 2, -3, 4, -4, 3, -2, 1}, 0, 7, 4, 4));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"testHeapSelectRange"})
    void testHeapSelectRange(double[] values, int from, int to, int k1, int k2) {
        assertPartitionRange(sort(values, from, to),
            Partition::heapSelectRange, values, from, to, k1, k2);
    }

    @ParameterizedTest
    @MethodSource(value = {"testHeapSelectRange"})
    void testHeapSelectRange2(double[] values, int from, int to, int k1, int k2) {
        assertPartitionRange(sort(values, from, to),
            Partition::heapSelectRange2, values, from, to, k1, k2);
    }

    static Stream<Arguments> testHeapSelectRange() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of(new double[] {-1, 2, -3, 4, -4, 3, -2, 1}, 0, 7, 1, 2));
        builder.add(Arguments.of(new double[] {-1, 2, -3, 4, -4, 3, -2, 1}, 0, 7, 2, 2));
        builder.add(Arguments.of(new double[] {-1, 2, -3, 4, -4, 3, -2, 1}, 0, 7, 5, 7));
        builder.add(Arguments.of(new double[] {-1, 2, -3, 4, -4, 3, -2, 1}, 0, 7, 1, 6));
        builder.add(Arguments.of(new double[] {-1, 2, -3, 4, -4, 3, -2, 1}, 0, 7, 0, 3));
        builder.add(Arguments.of(new double[] {-1, 2, -3, 4, -4, 3, -2, 1}, 0, 7, 4, 7));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"testHeapSelect", "testSelectMinMax", "testSelectMinMax2"})
    void testSortSelectLeft(double[] values, int from, int to) {
        final double[] sorted = sort(values, from, to);

        final double[] x = values.clone();
        replaceNegativeZeros(x, from, to);
        final DoubleRangePartitionFunction fun = (a, l, r, ka, kb) -> {
            Partition.sortSelectLeft(a, l, r, kb);
            restoreNegativeZeros(a, l, r);
        };

        for (int k = from; k <= to; k++) {
            assertPartitionRange(sorted, fun, x.clone(), from, to, from, k);
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"testHeapSelect", "testSelectMinMax", "testSelectMinMax2"})
    void testSortSelectRight(double[] values, int from, int to) {
        final double[] sorted = sort(values, from, to);

        final double[] x = values.clone();
        replaceNegativeZeros(x, from, to);
        final DoubleRangePartitionFunction fun = (a, l, r, ka, kb) -> {
            Partition.sortSelectRight(a, l, r, ka);
            restoreNegativeZeros(a, l, r);
        };

        for (int k = from; k <= to; k++) {
            assertPartitionRange(sorted, fun, x.clone(), from, to, k, to);
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"testHeapSelectRange"})
    void testSortSelectRange(double[] values, int from, int to, int k1, int k2) {
        assertPartitionRange(sort(values, from, to),
            Partition::sortSelectRange, values, from, to, k1, k2);
    }

    @ParameterizedTest
    @MethodSource(value = {"testHeapSelectRange"})
    void testSortSelectRange2(double[] values, int from, int to, int k1, int k2) {
        assertPartitionRange(sort(values, from, to),
            Partition::sortSelectRange2, values, from, to, k1, k2);
    }

    /**
     * Return a copy of the {@code values} sorted.
     *
     * @param values Values.
     * @return the copy
     */
    private static double[] sort(double[] values) {
        final double[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted;
    }

    /**
     * Return a copy of the {@code values} sorted in the range {@code [from, to]}.
     *
     * @param values Values.
     * @param from From (inclusive).
     * @param to To (inclusive).
     * @return the copy
     */
    private static double[] sort(double[] values, int from, int to) {
        final double[] sorted = values.clone();
        Arrays.sort(sorted, from, to + 1);
        return sorted;
    }

    /**
     * Assert the function correctly partitions the range.
     *
     * @param sorted Expected sort result.
     * @param fun Partition function.
     * @param values Values.
     * @param from From (inclusive).
     * @param to To (inclusive).
     * @param ka Lower index to select.
     * @param kb Upper index to select.
     */
    private static void assertPartitionRange(double[] sorted,
            DoubleRangePartitionFunction fun,
            double[] values, int from, int to, int ka, int kb) {
        Arrays.sort(sorted, from, to + 1);
        fun.partition(values, from, to, ka, kb);
        // Clip
        ka = ka < from ? from : ka;
        kb = kb > to ? to : kb;
        for (int i = ka; i <= kb; i++) {
            final int index = i;
            Assertions.assertEquals(sorted[i], values[i], () -> "index: " + index);
        }
        // Check the data is the same
        Arrays.sort(values, from, to + 1);
        Assertions.assertArrayEquals(sorted, values, "Data destroyed");
    }

    @Test
    void testFloorLog2() {
        // Here expected = -Infinity; actual = -1
        Assertions.assertEquals(-1, Partition.floorLog2(0));
        Assertions.assertEquals(0, Partition.floorLog2(1));
        // Create a series of powers of 2, start at 2^1
        long p = 1;
        for (int i = 1;; i++) {
            p *= 2;
            if (p > Integer.MAX_VALUE) {
                break;
            }
            final int x = (int) p;
            Assertions.assertEquals(i - 1, Partition.floorLog2(x - 1));
            Assertions.assertEquals(i, Partition.floorLog2(x));
            Assertions.assertEquals(i, Partition.floorLog2(x + 1));
        }
    }

    @Test
    void testLog3() {
        // Reasonable behaviour at small x
        Assertions.assertEquals(0, Partition.log3(0));
        Assertions.assertEquals(0, Partition.log3(1));
        Assertions.assertEquals(1, Partition.log3(2));
        Assertions.assertEquals(1, Partition.log3(3));
        Assertions.assertEquals(1, Partition.log3(4));
        Assertions.assertEquals(1, Partition.log3(5));
        Assertions.assertEquals(1, Partition.log3(6));
        Assertions.assertEquals(1, Partition.log3(7));
        Assertions.assertEquals(2, Partition.log3(8));
        // log3(2^31-1) = 19.5588223...
        Assertions.assertEquals(19, Partition.log3(Integer.MAX_VALUE));
        // Create a series of powers of 3, start at 2^3
        long p = 3;
        for (int i = 2;; i++) {
            p *= 3;
            if (p > Integer.MAX_VALUE) {
                break;
            }
            final int x = (int) p;
            // Computes round(log3(x)) when x is close to a power of 3
            Assertions.assertEquals(i, Partition.log3(x - 1));
            Assertions.assertEquals(i, Partition.log3(x));
            Assertions.assertEquals(i, Partition.log3(x + 1));
            // Half-way point is within the bracket [i, i+1]
            final int y = (int) Math.floor(Math.pow(3, i + 0.5));
            Assertions.assertTrue(Partition.log3(y) >= i);
            Assertions.assertTrue(Partition.log3(y + 1) <= i + 1);
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionSBMIndexSet(double[] values, int[] indices) {
        assertPartition(values, indices,
            new Partition(SP, QS).setKeyStrategy(KeyStrategy.INDEX_SET)::partitionSBM);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionSBMPivotCache(double[] values, int[] indices) {
        assertPartition(values, indices,
            new Partition(SP, QS).setKeyStrategy(KeyStrategy.PIVOT_CACHE)::partitionSBM);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionSBMSequential(double[] values, int[] indices) {
        assertPartition(values, indices,
            new Partition(SP, QS).setKeyStrategy(KeyStrategy.SEQUENTIAL)::partitionSBM);
    }

    // Introselect versions use standard select configuration.
    // We test the different PairedKeyStrategy/EdgeSelectStrategy options alongside KeyStrategy options.

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionISP(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setKeyStrategy(KeyStrategy.ORDERED_KEYS)
            .setPairedKeyStrategy(PairedKeyStrategy.PAIRED_KEYS)
            .setEdgeSelectStrategy(EdgeSelectStrategy.ESH)
            .setControlFlags(Partition.FLAG_RANDOM_SAMPLING)
            .setSPStrategy(SPStrategy.SP)
            ::partitionISP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionIBM(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setKeyStrategy(KeyStrategy.ORDERED_KEYS)
            .setPairedKeyStrategy(PairedKeyStrategy.PAIRED_KEYS_2)
            .setEdgeSelectStrategy(EdgeSelectStrategy.ESH2)
            .setRecursionMultiple(5)
            .setRecursionConstant(1)
            .setSPStrategy(SPStrategy.BM)
            ::partitionISP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionISBM(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setKeyStrategy(KeyStrategy.ORDERED_KEYS)
            .setPairedKeyStrategy(PairedKeyStrategy.PAIRED_KEYS_LEN)
            .setEdgeSelectStrategy(EdgeSelectStrategy.ESS)
            .setRecursionMultiple(2)
            .setSPStrategy(SPStrategy.SBM)
            ::partitionISP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionISBMScanningKey(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setKeyStrategy(KeyStrategy.SCANNING_KEY_SEARCHABLE_INTERVAL)
            .setPairedKeyStrategy(PairedKeyStrategy.PAIRED_KEYS)
            .setEdgeSelectStrategy(EdgeSelectStrategy.ESS2)
            .setSPStrategy(SPStrategy.SBM)
            ::partitionISP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionISBMSearchKey(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setKeyStrategy(KeyStrategy.SEARCH_KEY_SEARCHABLE_INTERVAL)
            .setPairedKeyStrategy(PairedKeyStrategy.TWO_KEYS)
            .setEdgeSelectStrategy(EdgeSelectStrategy.ESH)
            .setSPStrategy(SPStrategy.SBM)
            ::partitionISP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionISBMIndexSet(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setKeyStrategy(KeyStrategy.INDEX_SET)
            .setPairedKeyStrategy(PairedKeyStrategy.KEY_RANGE)
            .setEdgeSelectStrategy(EdgeSelectStrategy.ESH2)
            .setRecursionMultiple(5)
            .setRecursionConstant(1)
            .setControlFlags(Partition.FLAG_RANDOM_SAMPLING)
            .setSPStrategy(SPStrategy.SBM)
            ::partitionISP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionISBMKeyUpdating(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setKeyStrategy(KeyStrategy.KEY_UPDATING_INTERVAL)
            .setPairedKeyStrategy(PairedKeyStrategy.SEARCHABLE_INTERVAL)
            .setEdgeSelectStrategy(EdgeSelectStrategy.ESS)
            .setSPStrategy(SPStrategy.SBM)
            ::partitionISP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionISBMIndexSetUpdating(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setKeyStrategy(KeyStrategy.INDEX_SET_UPDATING_INTERVAL)
            .setPairedKeyStrategy(PairedKeyStrategy.UPDATING_INTERVAL)
            .setEdgeSelectStrategy(EdgeSelectStrategy.ESS2)
            .setSPStrategy(SPStrategy.SBM)
            ::partitionISP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionISBMKeySplitting(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setKeyStrategy(KeyStrategy.KEY_SPLITTING_INTERVAL)
            .setPairedKeyStrategy(PairedKeyStrategy.SEARCHABLE_INTERVAL)
            .setEdgeSelectStrategy(EdgeSelectStrategy.ESH)
            .setSPStrategy(SPStrategy.SBM)
            ::partitionISP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionISBMIndexSetSplitting(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setKeyStrategy(KeyStrategy.INDEX_SET_SPLITTING_INTERVAL)
            .setPairedKeyStrategy(PairedKeyStrategy.UPDATING_INTERVAL)
            .setEdgeSelectStrategy(EdgeSelectStrategy.ESH)
            .setSPStrategy(SPStrategy.SBM)
            ::partitionISP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionISBMCompressedIndexSet(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setKeyStrategy(KeyStrategy.COMPRESSED_INDEX_SET)
            .setCompression(1)
            .setPairedKeyStrategy(PairedKeyStrategy.KEY_RANGE)
            .setEdgeSelectStrategy(EdgeSelectStrategy.ESH2)
            .setRecursionMultiple(5)
            .setRecursionConstant(1)
            .setSPStrategy(SPStrategy.SBM)
            ::partitionISP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionISBMCompressedIndexSet2(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setKeyStrategy(KeyStrategy.COMPRESSED_INDEX_SET)
            .setCompression(2)
            .setSPStrategy(SPStrategy.SBM)
            ::partitionISP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionISBMIndexIterator(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setKeyStrategy(KeyStrategy.INDEX_ITERATOR)
            .setSPStrategy(SPStrategy.SBM)
            ::partitionISP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionISBMCompressedIndexIterator(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setKeyStrategy(KeyStrategy.COMPRESSED_INDEX_ITERATOR)
            .setCompression(1)
            .setSPStrategy(SPStrategy.SBM)
            ::partitionISP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionISBMCompressedIndexIterator4(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setKeyStrategy(KeyStrategy.COMPRESSED_INDEX_ITERATOR)
            .setCompression(4)
            .setSPStrategy(SPStrategy.SBM)
            ::partitionISP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionIKBM(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setKeyStrategy(KeyStrategy.ORDERED_KEYS)
            .setPairedKeyStrategy(PairedKeyStrategy.PAIRED_KEYS)
            .setSPStrategy(SPStrategy.KBM)
            ::partitionISP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionIDNF1(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setSPStrategy(SPStrategy.DNF1)
            ::partitionISP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionIDPScanningKey(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(DP, QS2, EC)
            .setKeyStrategy(KeyStrategy.SCANNING_KEY_SEARCHABLE_INTERVAL)
            .setPairedKeyStrategy(PairedKeyStrategy.PAIRED_KEYS)
            .setEdgeSelectStrategy(EdgeSelectStrategy.ESH)
            ::partitionIDP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionIDPSearchKey(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(DP, QS2, EC)
            .setKeyStrategy(KeyStrategy.SEARCH_KEY_SEARCHABLE_INTERVAL)
            .setPairedKeyStrategy(PairedKeyStrategy.TWO_KEYS)
            .setEdgeSelectStrategy(EdgeSelectStrategy.ESH2)
            ::partitionIDP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionIDPIndexSet(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(DP, QS2, EC)
            .setKeyStrategy(KeyStrategy.INDEX_SET)
            .setPairedKeyStrategy(PairedKeyStrategy.SEARCHABLE_INTERVAL)
            .setEdgeSelectStrategy(EdgeSelectStrategy.ESS)
            ::partitionIDP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionIDPKeyUpdating(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(DP, QS2, EC)
            .setKeyStrategy(KeyStrategy.KEY_UPDATING_INTERVAL)
            .setPairedKeyStrategy(PairedKeyStrategy.SEARCHABLE_INTERVAL)
            .setEdgeSelectStrategy(EdgeSelectStrategy.ESS2)
            ::partitionIDP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionIDPIndexSetUpdating(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(DP, QS2, EC)
            .setKeyStrategy(KeyStrategy.INDEX_SET_UPDATING_INTERVAL)
            .setPairedKeyStrategy(PairedKeyStrategy.SEARCHABLE_INTERVAL)
            .setEdgeSelectStrategy(EdgeSelectStrategy.ESH)
            ::partitionIDP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionIDPKeySplitting(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(DP, QS2, EC)
            .setKeyStrategy(KeyStrategy.KEY_SPLITTING_INTERVAL)
            .setPairedKeyStrategy(PairedKeyStrategy.SEARCHABLE_INTERVAL)
            .setEdgeSelectStrategy(EdgeSelectStrategy.ESH2)
            ::partitionIDP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionIDPIndexSetSplitting(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(DP, QS2, EC)
            .setKeyStrategy(KeyStrategy.INDEX_SET_SPLITTING_INTERVAL)
            .setPairedKeyStrategy(PairedKeyStrategy.SEARCHABLE_INTERVAL)
            .setEdgeSelectStrategy(EdgeSelectStrategy.ESS)
            ::partitionIDP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionIDPCompressedIndexSet(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(DP, QS2, EC)
            .setKeyStrategy(KeyStrategy.COMPRESSED_INDEX_SET)
            .setCompression(1)::partitionIDP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionIDPCompressedIndexSet2(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(DP, QS2, EC)
            .setKeyStrategy(KeyStrategy.COMPRESSED_INDEX_SET)
            .setCompression(2)::partitionIDP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionIDPIndexIterator(double[] values, int[] indices) {
        assertPartition(values, indices,
            new Partition(DP, QS2, EC).setKeyStrategy(KeyStrategy.INDEX_ITERATOR)::partitionIDP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionIDPCompressedIndexIterator(double[] values, int[] indices) {
        assertPartition(values, indices, new Partition(DP, QS2, EC)
            .setKeyStrategy(KeyStrategy.COMPRESSED_INDEX_ITERATOR)
            .setCompression(1)::partitionIDP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition", "testPartitionBigData"})
    void testPartitionFR(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1);
        assertPartition(values, indices,
            new Partition(PivotingStrategy.TARGET, QS, EC, SU)::partitionFR);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition", "testPartitionBigData"})
    void testPartitionFRPivotingStrategy(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1);
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)::partitionFR);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition", "testPartitionBigData"})
    void testPartitionFRRandomSampling(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1);
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setControlFlags(Partition.FLAG_RANDOM_SAMPLING)::partitionFR);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition", "testPartitionBigData"})
    void testPartitionFRMoveSample(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1);
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setControlFlags(Partition.FLAG_MOVE_SAMPLE)::partitionFR);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition", "testPartitionBigData"})
    void testPartitionFRSubsetSampling(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1);
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setControlFlags(Partition.FLAG_SUBSET_SAMPLING)::partitionFR);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition", "testPartitionBigData"})
    void testPartitionKFR(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1);
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)::partitionKFR);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionLSP(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1 ||
            (indices.length == 2 && Math.abs(indices[1] - indices[0]) < 10));
        // Uses a special sortselect size so ensure this is set
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setLinearSortSelectSize(3)
            ::partitionLSP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionLSPMoveSample(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1 ||
            (indices.length == 2 && Math.abs(indices[1] - indices[0]) < 10));
        // Uses a special sortselect size so ensure this is set
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setLinearSortSelectSize(3)
            .setControlFlags(Partition.FLAG_MOVE_SAMPLE)
            ::partitionLSP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionLinearBFPRTPER(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1 ||
            (indices.length == 2 && Math.abs(indices[1] - indices[0]) < 10));
        // Require the range >= 5: uses a special sortselect size
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setLinearSortSelectSize(indices.length == 1 ? 3 : 5)
            .setLinearStrategy(LinearStrategy.BFPRT)
            .setExpandStrategy(ExpandStrategy.PER)
            ::partitionLinear);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionLinearBFPRTT1(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1 ||
            (indices.length == 2 && Math.abs(indices[1] - indices[0]) < 10));
        // Require the range >= 5: uses a special sortselect size
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setLinearSortSelectSize(indices.length == 1 ? 3 : 5)
            .setLinearStrategy(LinearStrategy.BFPRT)
            .setExpandStrategy(ExpandStrategy.T1)
            ::partitionLinear);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionLinearBFPRTB1(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1 ||
            (indices.length == 2 && Math.abs(indices[1] - indices[0]) < 10));
        // Require the range >= 5: uses a special sortselect size
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setLinearSortSelectSize(indices.length == 1 ? 3 : 5)
            .setLinearStrategy(LinearStrategy.BFPRT)
            .setExpandStrategy(ExpandStrategy.B1)
            ::partitionLinear);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionLinearBFPRTT2(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1 ||
            (indices.length == 2 && Math.abs(indices[1] - indices[0]) < 10));
        // Require the range >= 2*5: uses a special sortselect size
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setLinearSortSelectSize(indices.length == 1 ? 6 : 10)
            .setLinearStrategy(LinearStrategy.BFPRT)
            .setExpandStrategy(ExpandStrategy.T2)
            ::partitionLinear);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionLinearBFPRTB2(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1 ||
            (indices.length == 2 && Math.abs(indices[1] - indices[0]) < 10));
        // Require the range >= 2*5: uses a special sortselect size
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setLinearSortSelectSize(indices.length == 1 ? 5 : 10)
            .setLinearStrategy(LinearStrategy.BFPRT)
            .setExpandStrategy(ExpandStrategy.B2)
            ::partitionLinear);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionLinearRS(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1 ||
            (indices.length == 2 && Math.abs(indices[1] - indices[0]) < 10));
        // Require the range >= 9: uses a special sortselect size
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setLinearSortSelectSize(indices.length == 1 ? 5 : 9)
            .setLinearStrategy(LinearStrategy.RS)
            .setExpandStrategy(ExpandStrategy.T1)
            ::partitionLinear);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionLinearBFPRTImprovedT2(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1 ||
            (indices.length == 2 && Math.abs(indices[1] - indices[0]) < 10));
        // Require the range >= 2*5: uses a special sortselect size
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setLinearSortSelectSize(indices.length == 1 ? 5 : 10)
            .setLinearStrategy(LinearStrategy.BFPRT_IM)
            .setExpandStrategy(ExpandStrategy.T2)
            ::partitionLinear);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionLinearRSImproved(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1 ||
            (indices.length == 2 && Math.abs(indices[1] - indices[0]) < 10));
        // Require the range >= 2*9: uses a special sortselect size
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setLinearSortSelectSize(indices.length == 1 ? 9 : 18)
            .setLinearStrategy(LinearStrategy.RS_IM)
            .setExpandStrategy(ExpandStrategy.B2)
            ::partitionLinear);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionLinearRSAdaptive(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1 ||
            (indices.length == 2 && Math.abs(indices[1] - indices[0]) < 10));
        // Require the range >= 9: uses a special sortselect size
        assertPartition(values, indices, new Partition(SP, QS, EC, SU)
            .setLinearSortSelectSize(indices.length == 1 ? 5 : 9)
            .setLinearStrategy(LinearStrategy.RSA)
            .setExpandStrategy(ExpandStrategy.T1)
            ::partitionLinear);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionQA(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1 ||
            (indices.length == 2 && Math.abs(indices[1] - indices[0]) < 10));
        // Require the range >= 12: uses a special sortselect size
        assertPartition(values, indices, new Partition(SP, QS, EC, Partition.SUBSAMPLING_SIZE)
            .setLinearSortSelectSize(indices.length == 1 ? 6 : 12)
            .setExpandStrategy(ExpandStrategy.T1)
            ::partitionQA);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionQAAlwaysAdapt(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1 ||
            (indices.length == 2 && Math.abs(indices[1] - indices[0]) < 10));
        // Require the range >= 12: uses a special sortselect size
        assertPartition(values, indices, new Partition(SP, QS, EC, Partition.SUBSAMPLING_SIZE)
            .setLinearSortSelectSize(indices.length == 1 ? 6 : 12)
            .setExpandStrategy(ExpandStrategy.T1)
            .setAdaptMode(AdaptMode.ADAPT1)
            ::partitionQA);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionQANoSampling(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1 ||
            (indices.length == 2 && Math.abs(indices[1] - indices[0]) < 10));
        // Require the range >= 12: uses a special sortselect size
        assertPartition(values, indices, new Partition(SP, QS, EC, Partition.SUBSAMPLING_SIZE)
            .setLinearSortSelectSize(indices.length == 1 ? 6 : 12)
            .setExpandStrategy(ExpandStrategy.T1)
            // No sampling but always use variable margins
            .setAdaptMode(AdaptMode.ADAPT1B)
            ::partitionQA);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionQAFarStepAndMiddle12(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1 ||
            (indices.length == 2 && Math.abs(indices[1] - indices[0]) < 10));
        // Require the range >= 12: uses a special sortselect size
        assertPartition(values, indices, new Partition(SP, QS, EC, Partition.SUBSAMPLING_SIZE)
            .setLinearSortSelectSize(indices.length == 1 ? 6 : 12)
            .setExpandStrategy(ExpandStrategy.T1)
            .setControlFlags(Partition.FLAG_QA_FAR_STEP | Partition.FLAG_QA_MIDDLE_12)
            ::partitionQA);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionQAFarStepAdaptOriginal(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1 ||
            (indices.length == 2 && Math.abs(indices[1] - indices[0]) < 10));
        // Require the range >= 12: uses a special sortselect size
        assertPartition(values, indices, new Partition(SP, QS, EC, Partition.SUBSAMPLING_SIZE)
            .setLinearSortSelectSize(indices.length == 1 ? 6 : 12)
            .setExpandStrategy(ExpandStrategy.T1)
            .setControlFlags(Partition.FLAG_QA_FAR_STEP_ADAPT_ORIGINAL)
            ::partitionQA);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionQASampleK(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1 ||
            (indices.length == 2 && Math.abs(indices[1] - indices[0]) < 10));
        // Require the range >= 12: uses a special sortselect size
        assertPartition(values, indices, new Partition(SP, QS, EC, Partition.SUBSAMPLING_SIZE)
            .setLinearSortSelectSize(indices.length == 1 ? 6 : 12)
            .setExpandStrategy(ExpandStrategy.T1)
            .setControlFlags(Partition.FLAG_QA_SAMPLE_K)
            ::partitionQA);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionQASampleStep(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1 ||
            (indices.length == 2 && Math.abs(indices[1] - indices[0]) < 10));
        // Require the range >= 2*12: uses a special sortselect size
        assertPartition(values, indices, new Partition(SP, QS, EC, 200)
            .setLinearSortSelectSize(indices.length == 1 ? 12 : 24)
            .setExpandStrategy(ExpandStrategy.T2)
            ::partitionQA);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionQASampleStepRandom1(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1 ||
            (indices.length == 2 && Math.abs(indices[1] - indices[0]) < 10));
        // Require the range >= 2*12: uses a special sortselect size
        assertPartition(values, indices, new Partition(SP, QS, EC, 200)
            .setLinearSortSelectSize(indices.length == 1 ? 12 : 24)
            .setExpandStrategy(ExpandStrategy.T2)
            .setControlFlags(Partition.FLAG_RANDOM_SAMPLING)
            ::partitionQA);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionQASampleStepRandom2(double[] values, int[] indices) {
        Assumptions.assumeTrue(indices.length == 1 ||
            (indices.length == 2 && Math.abs(indices[1] - indices[0]) < 10));
        // Require the range >= 2*12: uses a special sortselect size
        assertPartition(values, indices, new Partition(SP, QS, EC, 200)
            .setLinearSortSelectSize(indices.length == 1 ? 12 : 24)
            .setExpandStrategy(ExpandStrategy.T2)
            .setControlFlags(Partition.FLAG_QA_RANDOM_SAMPLING)
            ::partitionQA);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition", "testPartitionBigData"})
    void testPartitionQA2FRSampling(double[] values, int[] indices) {
        assertPartitionQA2(values, indices, Partition.MODE_FR_SAMPLING);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition", "testPartitionBigData"})
    void testPartitionQA2Sampling(double[] values, int[] indices) {
        assertPartitionQA2(values, indices, Partition.MODE_SAMPLING);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition", "testPartitionBigData"})
    void testPartitionQA2Adaption(double[] values, int[] indices) {
        assertPartitionQA2(values, indices, Partition.MODE_ADAPTION);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition", "testPartitionBigData"})
    void testPartitionQA2Strict(double[] values, int[] indices) {
        assertPartitionQA2(values, indices, Partition.MODE_STRICT);
    }

    private static void assertPartitionQA2(double[] values, int[] indices, int mode) {
        Assumptions.assumeTrue(indices.length == 1 ||
            (indices.length == 2 && Math.abs(indices[1] - indices[0]) < 10));
        Partition.configureQaAdaptive(mode, 1);
        try {
            assertPartition(values, indices, Partition::partitionQA2);
        } finally {
            Partition.configureQaAdaptive(Partition.MODE_FR_SAMPLING, 1);
        }
    }

    static void assertPartitionPaired(double[] values, int[] indices, DoublePartitionFunction2 function) {
        // Create a paired version of the indices.
        // We apply the partition function to this and test the result as if values
        // had been partitioned using indices.
        final BitSet bs = new BitSet();
        for (final int i : indices) {
            bs.set(i);
        }
        final int[] unique = bs.stream().toArray();
        // compress pairs
        int n = 1;
        for (int i = 1; i < unique.length; i++) {
            final int k = unique[i];
            if (k - 1 == unique[n - 1]) {
                // Mark as pair with sign bit
                unique[n - 1] |= Integer.MIN_VALUE;
                continue;
            }
            unique[n++] = k;
        }
        final int[] k = Arrays.copyOf(unique, n);
        ArraySampler.shuffle(RandomSource.XO_RO_SHI_RO_128_PP.create(0xdeadbeef), k);
        assertPartition(values, indices, (a, ignoredIndices, ignoredN) -> function.partition(a, k));
    }

    static void assertPartition(double[] values, int[] indices, DoublePartitionFunction function) {
        final double[] data = values.clone();
        final double[] sorted = values.clone();
        Arrays.sort(sorted);
        // Indices may be destructively modified
        function.partition(data, indices.clone(), indices.length);
        if (indices.length == 0) {
            return;
        }
        for (final int k : indices) {
            Assertions.assertEquals(sorted[k], data[k], () -> "k[" + k + "]");
        }
        // Check partial ordering
        Arrays.sort(indices);
        int i = 0;
        for (final int k : indices) {
            final double value = sorted[k];
            while (i < k) {
                final int j = i;
                Assertions.assertTrue(Double.compare(data[i], value) <= 0,
                    () -> j + " < " + k + " : " + data[j] + " < " + value);
                i++;
            }
        }
        final int k = indices[indices.length - 1];
        final double value = sorted[k];
        while (i < data.length) {
            final int j = i;
            Assertions.assertTrue(Double.compare(data[i], value) >= 0,
                () -> k + " < " + j);
            i++;
        }
        Arrays.sort(data);
        Assertions.assertArrayEquals(sorted, data, "Data destroyed");
    }

    static Stream<Arguments> testPartition() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create(123);
        // Sizes above and below the threshold for partitioning.
        // The largest size should trigger single-pivot sub-sampling for pivot selection.
        for (final int size : new int[] {5, 47, SU + 10}) {
            final int halfSize = size >>> 1;
            final int from = -halfSize;
            final int to = -halfSize + size;
            final double[] values = IntStream.range(from, to).asDoubleStream().toArray();
            final double[] zeros = values.clone();
            final int quarterSize = size >>> 2;
            Arrays.fill(zeros, quarterSize, halfSize, -0.0);
            Arrays.fill(zeros, halfSize, halfSize + quarterSize, 0.0);
            for (final int k : new int[] {1, 2, 3, size}) {
                for (int i = 0; i < 15; i++) {
                    // Note: Duplicate indices do not matter
                    final int[] indices = rng.ints(k, 0, size).toArray();
                    builder.add(Arguments.of(
                        ArraySampler.shuffle(rng, values.clone()),
                        indices));
                    builder.add(Arguments.of(
                        ArraySampler.shuffle(rng, zeros.clone()),
                        indices));
                }
            }
            // Test sequential processing by creating potential ranges
            // after an initial low point. This should be high enough
            // so any range analysis that joins indices will leave the initial
            // index as a single point.
            final int limit = 50;
            if (size > limit) {
                for (int i = 0; i < 10; i++) {
                    final int[] indices = rng.ints(size - limit, limit, size).toArray();
                    // This sets a low index
                    indices[rng.nextInt(indices.length)] = rng.nextInt(0, limit >>> 1);
                    builder.add(Arguments.of(
                        ArraySampler.shuffle(rng, values.clone()),
                        indices));
                }
            }
            // min; max; min/max
            builder.add(Arguments.of(values.clone(), new int[] {0}));
            builder.add(Arguments.of(values.clone(), new int[] {size - 1}));
            builder.add(Arguments.of(values.clone(), new int[] {0, size - 1}));
            builder.add(Arguments.of(zeros.clone(), new int[] {0}));
            builder.add(Arguments.of(zeros.clone(), new int[] {size - 1}));
            builder.add(Arguments.of(zeros.clone(), new int[] {0, size - 1}));
        }
        final double nan = Double.NaN;
        builder.add(Arguments.of(new double[] {}, new int[0]));
        builder.add(Arguments.of(new double[] {nan}, new int[] {0}));
        builder.add(Arguments.of(new double[] {-0.0, nan}, new int[] {1}));
        builder.add(Arguments.of(new double[] {nan, nan, nan}, new int[] {2}));
        builder.add(Arguments.of(new double[] {nan, 0.0, -0.0, nan}, new int[] {3}));
        builder.add(Arguments.of(new double[] {nan, 0.0, -0.0, nan}, new int[] {1, 2}));
        builder.add(Arguments.of(new double[] {nan, 0.0, 1, -0.0, nan}, new int[] {1, 3}));
        builder.add(Arguments.of(new double[] {nan, 0.0, -0.0}, new int[] {0, 2}));
        builder.add(Arguments.of(new double[] {nan, 1.23, 0.0, -4.56, -0.0, nan}, new int[] {0, 1, 3}));
        // Dual-pivot with a large middle region (> 5 / 8) requires equal elements loop
        final int n = 128;
        final double[] x = IntStream.range(0, n).asDoubleStream().toArray();
        // Put equal elements in the central region:
        //          2/16      6/16             10/16      14/16
        // |  <P1    |    P1   |   P1< & < P2    |    P2    |    >P2    |
        final int sixteenth = n / 16;
        final int i2 = 2 * sixteenth;
        final int i6 = 6 * sixteenth;
        final double p1 = x[i2];
        final double p2 = x[n - i2];
        // Lots of values equal to the pivots
        Arrays.fill(x, i2, i6, p1);
        Arrays.fill(x, n - i6, n - i2, p2);
        // Equal value in between the pivots
        Arrays.fill(x, i6, n - i6, (p1 + p2) / 2);
        // Shuffle this and partition in the middle.
        // Use a fix seed to ensure we hit coverage with only 5 loops.
        rng = RandomSource.XO_SHI_RO_128_PP.create(-8111061151820577011L);
        for (int i = 0; i < 5; i++) {
            builder.add(Arguments.of(ArraySampler.shuffle(rng, x.clone()), new int[] {50}));
        }
        // A single value smaller/greater than the pivot at the left/right/both ends
        Arrays.fill(x, 1);
        for (int i = 0; i <= 2; i++) {
            for (int j = 0; j <= 2; j++) {
                x[n - 1] = i;
                x[0] = j;
                builder.add(Arguments.of(x.clone(), new int[] {50}));
            }
        }
        return builder.build();
    }

    static Stream<Arguments> testPartitionBigData() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create(123);
        // Sizes above the threshold (600) for recursive partitioning
        for (final int size : new int[] {1000, 5000, 10000}) {
            final double[] a = IntStream.range(0, size).asDoubleStream().toArray();
            // With repeat elements
            final double[] b = rng.ints(size, 0, size >> 3).asDoubleStream().toArray();
            for (int i = 0; i < 15; i++) {
                builder.add(Arguments.of(
                    ArraySampler.shuffle(rng, a.clone()),
                    new int[] {rng.nextInt(size)}));
                builder.add(Arguments.of(b.clone(),
                    new int[] {rng.nextInt(size)}));
            }
        }
        // Hit Floyd-Rivest sub-sampling conditions.
        // Close to edge but outside edge select size.
        final int n = 7000;
        final double[] x = IntStream.range(0, n).asDoubleStream().toArray();
        builder.add(Arguments.of(x.clone(), new int[] {20}));
        builder.add(Arguments.of(x, new int[] {n - 1 - 20}));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"testSort"})
    void testSortSBM(double[] values) {
        assertSort(values,
            new Partition(SP, 3)::sortSBM);
    }

    @ParameterizedTest
    @MethodSource(value = {"testSort"})
    void testHeapSortUsingHeapSelectRange(double[] values) {
        assumeNonNaN(values);
        Assumptions.assumeTrue(values.length > 0);
        assertSort(values, x -> {
            replaceNegativeZeros(x, 0, x.length - 1);
            Partition.heapSelectRange(x, 0, x.length - 1, 0, x.length - 1);
            restoreNegativeZeros(x, 0, x.length - 1);
        });
    }

    @ParameterizedTest
    @MethodSource(value = {"testSort"})
    void testHeapSort(double[] values) {
        assumeNonNaN(values);
        Assumptions.assumeTrue(values.length > 0);
        assertSort(values, x -> {
            replaceNegativeZeros(x, 0, x.length - 1);
            Partition.heapSort(x, 0, x.length - 1);
            restoreNegativeZeros(x, 0, x.length - 1);
        });
    }

    @ParameterizedTest
    @MethodSource(value = {"testSort"})
    void testSortISP(double[] values) {
        assertSort(values, new Partition(SP, QS)
            .setSPStrategy(SPStrategy.SP)
            ::sortISP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testSort"})
    void testSortIBM(double[] values) {
        assertSort(values, new Partition(SP, QS)
            .setSPStrategy(SPStrategy.BM)
            ::sortISP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testSort"})
    void testSortISBM(double[] values) {
        assertSort(values, new Partition(SP, QS)
            .setSPStrategy(SPStrategy.SBM)
            ::sortISP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testSort"})
    void testSortIKBM(double[] values) {
        assertSort(values, new Partition(SP, QS)
            .setSPStrategy(SPStrategy.KBM)
            ::sortISP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testSort"})
    void testSortIDNF1(double[] values) {
        assertSort(values, new Partition(SP, QS)
            .setSPStrategy(SPStrategy.DNF1)
            ::sortISP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testSort"})
    void testSortIDNF2(double[] values) {
        assertSort(values, new Partition(SP, QS)
            .setSPStrategy(SPStrategy.DNF2)
            ::sortISP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testSort"})
    void testSortIDNF3(double[] values) {
        assertSort(values, new Partition(SP, QS)
            .setSPStrategy(SPStrategy.DNF3)
            ::sortISP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testSort"})
    void testSortIDP(double[] values) {
        assertSort(values,
            new Partition(DP, QS2)::sortIDP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testSort"})
    void testInsertionSort(double[] values) {
        assumeNonNaN(values);
        assertSort(values, x -> {
            replaceNegativeZeros(x, 0, x.length - 1);
            Sorting.sort(x, 0, x.length - 1, false);
            restoreNegativeZeros(x, 0, x.length - 1);
        });
        if (values.length < 2) {
            return;
        }
        // Check internal sort
        // Set pivot at lower end
        values[0] = Arrays.stream(values).min().getAsDouble();
        // check internal sort
        assertSort(values, x -> {
            replaceNegativeZeros(x, 1, x.length - 1);
            Sorting.sort(x, 1, x.length - 1, false);
            restoreNegativeZeros(x, 1, x.length - 1);
        });
    }

    @ParameterizedTest
    @MethodSource(value = {"testSort"})
    void testInsertionSort5(double[] values) {
        // Cannot handle NaN or -0.0
        // Negative zeros are swapped for a proxy
        assumeNonNaN(values);
        final double[] data = Arrays.copyOf(values, 5);
        assertSort(data, x -> {
            replaceNegativeZeros(x, 0, x.length - 1);
            Sorting.sort5(x, 0, 1, 2, 3, 4);
            restoreNegativeZeros(x, 0, x.length - 1);
        });
    }

    @Test
    void testSortZero() {
        final double a = -0.0;
        final double b = 0.0;
        final double[][] values = new double[][] {
            {a, a},
            {a, b},
            {b, a},
            {b, b},
            {a, a, a},
            {a, a, b},
            {a, b, a},
            {a, b, b},
            {b, a, a},
            {b, a, b},
            {b, b, a},
            {b, b, b},
            {a, a, a, a},
            {a, a, a, b},
            {a, a, b, a},
            {a, a, b, b},
            {a, b, a, a},
            {a, b, a, b},
            {a, b, b, a},
            {a, b, b, b},
            {b, a, a, a},
            {b, a, a, b},
            {b, a, b, a},
            {b, a, b, b},
            {b, b, a, a},
            {b, b, a, b},
            {b, b, b, a},
            {b, b, b, b},
        };
        for (final double[] v : values) {
            assertSort(v, x -> Partition.sortZero(x, 0, x.length - 1));
        }
    }

    private static void assertSort(double[] values, Consumer<double[]> function) {
        final double[] data = values.clone();
        final double[] sorted = values.clone();
        Arrays.sort(sorted);
        function.accept(data);
        Assertions.assertArrayEquals(sorted, data);
    }

    static Stream<double[]> testSort() {
        final Stream.Builder<double[]> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create(123);
        // Sizes above and below the threshold for partitioning
        for (final int size : new int[] {5, 50}) {
            double[] a = new double[size];
            Arrays.fill(a, 1.23);
            builder.add(a.clone());
            for (int ii = 0; ii < size; ii++) {
                a[ii] = ii;
            }
            builder.add(a.clone());
            for (int ii = 0; ii < size; ii++) {
                a[ii] = size - ii;
            }
            builder.add(a.clone());
            for (int i = 0; i < 5; i++) {
                a = rng.doubles(size).toArray();
                builder.add(a.clone());
                final int j = rng.nextInt(size);
                final int k = rng.nextInt(size);
                a[j] = Double.NaN;
                a[k] = Double.NaN;
                builder.add(a.clone());
                a[j] = -0.0;
                a[k] = 0.0;
                builder.add(a.clone());
                for (int z = 0; z < size; z++) {
                    a[z] = rng.nextBoolean() ? -0.0 : 0.0;
                }
                builder.add(a.clone());
                a[j] = -rng.nextDouble();
                a[k] = rng.nextDouble();
                builder.add(a.clone());
            }
        }
        final double nan = Double.NaN;
        builder.add(new double[] {});
        builder.add(new double[] {nan});
        builder.add(new double[] {-0.0, nan});
        builder.add(new double[] {nan, nan, nan});
        builder.add(new double[] {nan, 0.0, -0.0, nan});
        builder.add(new double[] {nan, 0.0, -0.0});
        builder.add(new double[] {nan, 0.0, 1, -0.0});
        builder.add(new double[] {nan, 1.23, 0.0, -4.56, -0.0, nan});
        return builder.build();
    }

    /**
     * Test key analysis.
     * The key analysis code decides the partition strategy. Currently this
     * supports recommendations for processing keys or ranges of keys in ascending
     * order based on separation between points, and the point when data partitioning
     * switches to a full sort.
     *
     * @param size Length of the data to partition.
     * @param k Indices (non-zero length).
     * @param n Count of indices (either {@code 1 <= n <= k.length} or -1).
     * @param minSeparation Minimum separation between points.
     * @param minSelectSize Minimum selection size for insertion sort rather than selection.
     * @param expected Expected keys (up to the end of the indices or the marker {@link Integer#MIN_VALUE})
     * @param cacheRange {@code [L, R]} bounds of returned {@link PivotCache}, or null.
     */
    @ParameterizedTest
    @MethodSource
    void testKeyAnalysis(int size, int[] k, int n, int minSeparation, int minSelectSize,
            int[] expected, int[] cacheRange) {
        // Set the number of keys
        n = n < 0 ? k.length : n;
        final PivotCache pivotCache = new Partition(SP, minSelectSize)
            .keyAnalysis(size, k, n, minSeparation);
        // Truncate to the marker
        int m = 0;
        while (m < n && k[m] != Integer.MIN_VALUE) {
            m++;
        }
        if (m == 0) {
            // Full sort recommendation
            Assertions.assertArrayEquals(expected, new int[] {Integer.MIN_VALUE});
        } else {
            final int[] actual = Arrays.copyOf(k, m);
            Assertions.assertArrayEquals(expected, actual,
                () -> Arrays.toString(actual));
        }
        if (cacheRange == null) {
            Assertions.assertNull(pivotCache);
        } else {
            Assertions.assertEquals(cacheRange[0], pivotCache.left());
            Assertions.assertEquals(cacheRange[1], pivotCache.right());
        }
    }

    static Stream<Arguments> testKeyAnalysis() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final int allK = -1;
        final int[] noCache = null;
        final int[] fullSort = new int[] {Integer.MIN_VALUE};
        builder.add(Arguments.of(100, new int[] {3}, allK, 2, 0,
            new int[] {3}, noCache));
        builder.add(Arguments.of(100, new int[] {3, 4, 5}, allK, 1, 0,
            new int[] {3, 5}, noCache));
        builder.add(Arguments.of(100, new int[] {3, 4, 5, 8}, allK, 2, 0,
            new int[] {3, 5, ~8}, new int[] {8, 8}));
        builder.add(Arguments.of(100, new int[] {3, 4, 5, 6, 7, 8}, allK, 1, 0,
            new int[] {3, 8}, noCache));
        builder.add(Arguments.of(100, new int[] {3, 4, 7, 8}, allK, 1, 0,
            new int[] {3, 4, 7, 8}, new int[] {7, 8}));
        builder.add(Arguments.of(100, new int[] {3, 4, 7, 8, 99}, allK, 1, 0,
            new int[] {3, 4, 7, 8, ~99}, new int[] {7, 99}));
        // Full sort recommendation: cases not large enough
        builder.add(Arguments.of(20, new int[] {3, 5, 8, 17}, allK, 3, 0,
            new int[] {3, 8, ~17}, new int[] {17, 17}));
        builder.add(Arguments.of(20, new int[] {3, 5, 8, 17}, allK, 3, 10,
            new int[] {3, 8, ~17}, new int[] {17, 17}));
        builder.add(Arguments.of(20, new int[] {3, 5, 8, 17}, allK, 9, 0,
            new int[] {3, 17}, noCache));
        // Full sort based on a single range to the end (due to high min separation)
        builder.add(Arguments.of(20, new int[] {3, 5, 8, 17}, allK, 10, 10,
            fullSort, noCache));
        // Full sort based on min select size
        builder.add(Arguments.of(20, new int[] {10, 11}, allK, 1, 20,
            fullSort, noCache));
        // No min separation - process each index
        builder.add(Arguments.of(100, new int[] {3, 4, 5}, allK, 0, 0,
            new int[] {~3, ~4, ~5}, new int[] {4, 5}));
        builder.add(Arguments.of(100, new int[] {3, 4, 7, 8}, allK, 0, 0,
            new int[] {~3, ~4, ~7, ~8}, new int[] {4, 8}));
        // Duplicate keys
        builder.add(Arguments.of(100, new int[] {0, 1, 2, 2, 3, 3}, allK, 0, 0,
            new int[] {~0, ~1, ~2, ~3}, new int[] {1, 3}));
        builder.add(Arguments.of(100, new int[] {0, 1, 2, 2, 3, 3}, allK, 1, 0,
            new int[] {0, 3}, noCache));
        builder.add(Arguments.of(100, new int[] {0, 1, 2, 2, 3, 3, 8, 8, 8}, allK, 2, 0,
            new int[] {0, 3, ~8}, new int[] {8, 8}));
        builder.add(Arguments.of(100, new int[] {9, 6, 7, 8, 2, 1, 1, 3}, allK, 2, 0,
            new int[] {1, 3, 6, 9}, new int[] {6, 9}));

        // Repeat the contents of the stream with any case not using the full length of the data.
        // by padding with random indices (these should be ignored)
        final Stream.Builder<Arguments> builder2 = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create();
        builder.build().forEach(arg -> {
            builder2.add(arg);
            // unpack
            final Object[] o = arg.get();
            final int size = (int) o[0];
            final int[] k = (int[]) o[1];
            final int n = (int) o[2];
            if (n < 0) {
                final Object[] o2 = o.clone();
                // Add extra
                final int extra = rng.nextInt(3, 10);
                final int len = k.length;
                // Extra are zeros
                final int[] k2 = Arrays.copyOf(k, len + extra);
                o2[1] = k2.clone();
                o2[2] = len;
                builder2.add(Arguments.of(o2));
                // Deliberately add indices not in the original
                final Object[] o3 = o2.clone();
                final int max = Arrays.stream(k).max().getAsInt();
                for (int i = len; i < k2.length; i++) {
                    k2[i] = rng.nextInt(max, size);
                }
                o3[1] = k2;
                builder2.add(Arguments.of(o3));
            }
        });
        return builder2.build();
    }

    /**
     * Assume the data are non-NaN, otherwise skip the test.
     *
     * @param a Data.
     */
    private void assumeNonNaN(double[] a) {
        for (int i = 0; i < a.length; i++) {
            Assumptions.assumeFalse(Double.isNaN(a[i]));
        }
    }

    /**
     * Replace negative zeros with a proxy. Uses -{@link Double#MIN_VALUE} as the proxy.
     *
     * @param a Data.
     * @param from Lower bound (inclusive).
     * @param to Upper bound (inclusive).
     */
    private static void replaceNegativeZeros(double[] a, int from, int to) {
        for (int i = from; i <= to; i++) {
            if (Double.doubleToRawLongBits(a[i]) == Long.MIN_VALUE) {
                a[i] = -Double.MIN_VALUE;
            }
        }
    }
    /**
     * Restore proxy negative zeros.
     *
     * @param a Data.
     * @param from Lower bound (inclusive).
     * @param to Upper bound (inclusive).
     */
    private static void restoreNegativeZeros(double[] a, int from, int to) {
        for (int i = from; i <= to; i++) {
            if (a[i] == -Double.MIN_VALUE) {
                a[i] = -0.0;
            }
        }
    }

    @ParameterizedTest
    @MethodSource
    void testSearch(int[] keys, int left, int right) {
        // Clip to correct range
        final int l = left < 0 ? 0 : left;
        final int r = right < 0 ? keys.length - 1 : right;
        for (int i = l; i <= r; i++) {
            final int k = keys[i];
            // Unspecified index when key is present
            Assertions.assertEquals(k, keys[Partition.searchLessOrEqual(keys, l, r, k)], "leq");
            Assertions.assertEquals(k, keys[Partition.searchGreaterOrEqual(keys, l, r, k)], "geq");
        }
        // Search above/below keys
        Assertions.assertEquals(l - 1, Partition.searchLessOrEqual(keys, l, r, keys[l] - 44), "leq below");
        Assertions.assertEquals(r, Partition.searchLessOrEqual(keys, l, r, keys[r] + 44), "leq above");
        Assertions.assertEquals(l, Partition.searchGreaterOrEqual(keys, l, r, keys[l] - 44), "geq below");
        Assertions.assertEquals(r + 1, Partition.searchGreaterOrEqual(keys, l, r, keys[r] + 44), "geq above");
        // Search between neighbour keys
        for (int i = l + 1; i <= r; i++) {
            // Bound: keys[i-1] < k < keys[i]
            final int k1 = keys[i - 1];
            final int k2 = keys[i];
            for (int k = k1 + 1; k < k2; k++) {
                Assertions.assertEquals(i - 1, Partition.searchLessOrEqual(keys, l, r, k), "leq between");
                Assertions.assertEquals(i, Partition.searchGreaterOrEqual(keys, l, r, k), "geq between");
            }
        }
    }

    static Stream<Arguments> testSearch() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final int allIndices = -1;
        builder.add(Arguments.of(new int[] {1}, allIndices, allIndices));
        builder.add(Arguments.of(new int[] {1, 2}, allIndices, allIndices));
        builder.add(Arguments.of(new int[] {1, 10}, allIndices, allIndices));
        builder.add(Arguments.of(new int[] {1, 2, 3}, allIndices, allIndices));
        builder.add(Arguments.of(new int[] {1, 4, 7}, allIndices, allIndices));
        builder.add(Arguments.of(new int[] {1, 4, 5, 7}, allIndices, allIndices));
        // Duplicates. These match binary search when found.
        builder.add(Arguments.of(new int[] {1, 1, 1, 1, 1, 1}, allIndices, allIndices));
        builder.add(Arguments.of(new int[] {1, 1, 1, 1, 3, 3, 3, 3, 3, 5, 5, 5, 5}, allIndices, allIndices));
        // Part of the range
        builder.add(Arguments.of(new int[] {1, 4, 5, 7, 13, 15}, 2, 4));
        builder.add(Arguments.of(new int[] {1, 4, 5, 7, 13, 15}, 0, 3));
        builder.add(Arguments.of(new int[] {1, 4, 5, 7, 13, 15}, 3, 5));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testPartitionDP(double[] a, int pivot1, int pivot2, int k0, int[] bounds) {
        final int r = a.length - 1;
        final int[] b = new int[3];
        Assertions.assertEquals(k0, Partition.partitionDP(a, 0, r, pivot1, pivot2, b));
        Assertions.assertArrayEquals(bounds, b);
    }

    static Stream<Arguments> testPartitionDP() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // Test less-than fast-forward bounds check - all values are < the pivots
        //builder.add(Arguments.of(new double[] {3, 4, 10, 12, 5, 6}, 2, 3, 4, new int[] {5, 5, 5}));
        builder.add(Arguments.of(new double[] {3, 4, 10, 12, 5, 6}, 2, 3, 4, new int[] {5, 4, 5}));
        // Test greater-than fast-forward bounds check - all values are > the pivots
        //builder.add(Arguments.of(new double[] {3, 4, 1, 2, 5, 6}, 2, 3, 0, new int[] {1, 1, 1}));
        builder.add(Arguments.of(new double[] {3, 4, 1, 2, 5, 6}, 2, 3, 0, new int[] {1, 0, 1}));
        return builder.build();
    }

    /**
     * Test the RNG used for shuffling. This test creates a RNG seeded using
     * {@code n} and {@code k}. Then an ascending array of size = 2^power is created
     * and shuffled to create the given number of {@code samples}. Each sample
     * is used to add counts to a histogram. Actual values {@code v} are mapped to buckets
     * using {@code v >> shift}. The number of buckets is 2^(power - shift) and the
     * width of the bucket is 2^shift. The histogram counts should be uniform.
     *
     * @param n Seed.
     * @param k Seed.
     * @param samples Number of samples.
     * @param power Defines the length of the data.
     * @param shift Defines the bucket size for histogram counts.
     */
    @ParameterizedTest
    @MethodSource
    void testRNG(int n, int k, int samples, int power, int shift) {
        final int size = 1 << power;
        final int[] a = IntStream.range(0, size).toArray();
        // histogram of index block count for each position
        final long[][] h = new long[size][size >>> shift];
        final IntUnaryOperator rng = Partition.createFastRNG(n, k);
        for (int s = samples; --s >= 0;) {
            // Shuffle the data
            for (int i = a.length; i > 1;) {
                final int j = rng.applyAsInt(i);
                final int t = a[--i];
                a[i] = a[j];
                a[j] = t;
            }
            // Add to histogram
            for (int i = 0; i < size; i++) {
                h[i][a[i] >>> shift]++;
            }
        }
        // Chi-square test the histogram is uniform
        final double p = org.apache.commons.math3.stat.inference.TestUtils.chiSquareTest(h);
        Assertions.assertFalse(p < 1e-3, () -> "Not uniform: " + p);
    }

    static Stream<Arguments> testRNG() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // Smallest sample size for n=600 ~ 37 ~ 2^32
        builder.add(Arguments.of(600, 52, 100, 5, 1));
        // Smallest sample size for n=6000 ~ 167 ~ 2^8
        builder.add(Arguments.of(6000, 789, 100, 8, 3));
        // Largest sample size for n=2^31 ~ 830192 ~ 2^20.
        // This works but is slow
        //builder.add(Arguments.of(830192, 678, 100, 20, 12));
        return builder.build();
    }

    /**
     * This is not a test. It runs the introselect algorithm as a full sort on the specified
     * data. A histogram of the level of recursion required to visit all regions is recorded
     * to file.
     */
    @ParameterizedTest
    @MethodSource
    @Disabled("Used for testing")
    void testRecursion(Distribution dist, Modification mod, int length, int range, boolean dualPivot) {
        final int maxDepth = 2048;
        final int[] h = new int[maxDepth + 1];
        // Use the defaults.
        // If the single pivot strategy is changed from MEDIAN_OF_3 to DYNAMIC
        // this avoid excess recursion.
        final Partition p = new Partition(
            Partition.PIVOTING_STRATEGY,
            //PivotingStrategy.MEDIAN_OF_3, // Use this to see excess recursion
            Partition.DUAL_PIVOTING_STRATEGY,
            Partition.MIN_QUICKSELECT_SIZE,
            Partition.EDGESELECT_CONSTANT,
            Partition.SUBSAMPLING_SIZE);
        p.setRecursionConsumer(i -> h[maxDepth - i]++);
        final AbstractDataSource source = new AbstractDataSource() {
            @Override
            protected int getLength() {
                return length;
            }
        };
        source.setDistribution(dist);
        source.setModification(mod);
        source.setRange(range);
        source.setup();

        // Sort the data. This will record the recursion depth when a region is complete.
        for (int i = 0; i < source.size(); i++) {
            final double[] x = source.getDataSample(i);
            if (dualPivot) {
                p.introselect(Partition::partitionDP, x, 0, x.length - 1,
                    IndexIntervals.anyIndex(), 0, x.length - 1, maxDepth);
            } else {
                p.introselect(Partition::partitionSBM, x, 0, x.length - 1,
                    IndexIntervals.anyIndex(), 0, x.length - 1, maxDepth);
            }
        }

        // Bracket the histogram. Assume at least 1 non-zero value.
        int hi = h.length;
        do {
            --hi;
        } while (h[hi] == 0);

        // Summary statistics
        long s = 0;
        long ss = 0;
        long n = 0;
        for (int i = 0; i < h.length; i++) {
            final int c = h[i];
            if (c != 0) {
                n += c;
                s += (long) i * c;
                ss += (long) i * i * c;
            }
        }
        final double mean = s / (double) n;
        double variance = ss - ((double) s * s) / n;
        if (variance > 0) {
            variance = variance / (n - 1);
        } else {
            variance = Double.isFinite(variance) ? 0.0 : Double.NaN;
        }
        final String name = dualPivot ? "DP" : "SP";
        final String distName = dist == null ? "ALL" : dist.name();
        final String modName = mod == null ? "ALL" : mod.name();
        // Flag when the method used excessive recursion.
        // Note that recursion only occurs down to a small length which is finished with a sort.
        final double expected = Math.log((length + range * 0.5) / Partition.MIN_QUICKSELECT_SIZE) /
            Math.log(dualPivot ? 3 : 2);
        String excess = "";
        for (double m = mean; m > expected && m > mean - 10; m -= 1) {
            excess += "*";
        }
        TestUtils.printf("%s %10s %15s %d-%d : n=%11d  mean=%10.6f  std=%10.6f   max=%4d : expected=%10.6f %s%n",
            name, distName, modName, length, length + range,
            n, mean, Math.sqrt(variance), hi, expected, excess);

        // Record the histogram
        final String dir = System.getProperty("java.io.tmpdir");
        final Path path = Paths.get(dir, String.format("%s_%s_%s_%d-%d.txt",
            name, distName, modName, length, length + range));
        try (BufferedWriter bw = Files.newBufferedWriter(path);
            Formatter f = new Formatter(bw)) {
            for (int i = 0; i <= hi; i++) {
                f.format("%d %d%n", i, h[i]);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Stream<Arguments> testRecursion() {
        TestUtils.printf("Save directory: %s%n", System.getProperty("java.io.tmpdir"));

        final Stream.Builder<Arguments> builder = Stream.builder();
        //final int length = 10000023;
        final int length = 1023;
        final int range = 2;

        // All
        //builder.add(Arguments.of(null, null, length, range, true));
        //builder.add(Arguments.of(null, null, length, range, false));

        // Individual distribution / modification
        // Both single-pivot method (using DYNAMIC pivoting strategy) and dual-pivot
        // method have a mean recursion just above the theoretical max recursion depth.
        for (final Boolean dp : new Boolean[] {Boolean.TRUE, Boolean.FALSE}) {
            for (final Distribution dist : Distribution.values()) {
                for (final Modification mod : Modification.values()) {
                    builder.add(Arguments.of(dist, mod, length, range, dp));
                }
            }
        }

        return builder.build();
    }

    /**
     * This is not a test. It outputs the Bentley and McIlroy test data, optionally
     * with a single round of partitioning performed. This allows visualising the
     * change to the data made by the partition algorithm.
     */
    @ParameterizedTest
    @MethodSource
    @Disabled("Used for testing")
    void testData(int length, int seed, int partition) {
        final AbstractDataSource source = new AbstractDataSource() {
            @Override
            protected int getLength() {
                return length;
            }
        };
        source.setRange(0);
        source.setModification(Modification.COPY);
        source.setSeed(seed);
        source.setRngSeed(0xdeadbeef);
        source.setup();

        // Get the data
        final double[][] data = IntStream.range(0, source.size())
            .mapToObj(source::getDataSample).toArray(double[][]::new);

        // Optional: Run a single round of partitioning on the data.
        final LinkedList<String> pivots = new LinkedList<>();
        final int left = 0;
        final int right = length - 1;
        final int[] bounds = new int[3];
        if (partition == 1) {
            for (final double[] d : data) {
                int p = Partition.PIVOTING_STRATEGY.pivotIndex(d, left, right, (left + right) >>> 1);
                p = Partition.partitionSBM(d, left, right, p, bounds);
                pivots.add(formatPivotRange(p, bounds[0]));
            }
        } else if (partition == 2) {
            for (final double[] d : data) {
                int p = Partition.DUAL_PIVOTING_STRATEGY.pivotIndex(d, left, right, bounds);
                p = Partition.partitionDP(d, left, right, p, bounds[0], bounds);
                pivots.add(formatPivotRange(p, bounds[0]) + ":" + formatPivotRange(bounds[1], bounds[2]));
            }
        }

        // Print header (distributions are in enum order)
        TestUtils.printf("m %d%n", seed);
        TestUtils.printf("i");
        for (final Distribution d : Distribution.values()) {
            TestUtils.printf(" %s", d);
            if (!pivots.isEmpty()) {
                TestUtils.printf(pivots.pop());
            }
        }
        TestUtils.printf("%n");

        // Sort the data. This will record the recursion depth when a region is complete.
        for (int j = 0; j < length; j++) {
            TestUtils.printf("%d", j);
            for (int i = 0; i < data.length; i++) {
                TestUtils.printf(" %s", data[i][j]);
            }
            TestUtils.printf("%n");
        }
    }

    private static String formatPivotRange(int lo, int hi) {
        return lo == hi ? Integer.toString(lo) : lo + "-" + hi;
    }

    static Stream<Arguments> testData() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of(128, 32, 0));
        return builder.build();
    }

    /**
     * Prints the size of the Floyd-Rivest recursive subset samples.
     * This method is for information purposes. It is intended to be pasted into JShell
     * and called with various parameters. Example:
     *
     * <pre>{@code
     * jshell> printSubSamplingSize(0, 1000000, 5000)
     * 5000 [0, 1000000] (k=0.005 * 1000001) -> [4843, 9843] (k=0.032 * 5001)
     * 5000 [4843, 9843] (k=0.032 * 5001) -> [4977, 5124] (k=0.162 * 148)
     *
     * jshell> printSubSamplingSize(0, 1000000, 500000)
     * 500000 [0, 1000000] (k=0.500 * 1000001) -> [497631, 502631] (k=0.474 * 5001)
     * 500000 [497631, 502631] (k=0.474 * 5001) -> [499913, 500059] (k=0.599 * 147)
     * }</pre>
     *
     * @param l Left bound (inclusive).
     * @param r Right bound (inclusive).
     * @param k Target index.
     */
    static void printSubSamplingSize(int l, int r, int k) {
        int n = r - l;
        if (n > 600) {
            // Floyd-Rivest: use SELECT recursively on a sample of size S to get an estimate
            // for the (k-l+1)-th smallest element into a[k], biased slightly so that the
            // (k-l+1)-th element is expected to lie in the smaller set after partitioning.
            ++n;
            final int i = k - l + 1;
            final double z = Math.log(n);
            // s ~ sub-sample size ~ 0.5 * n^2/3
            final double s = 0.5 * Math.exp(0.6666666666666666 * z);
            final double sd = 0.5 * Math.sqrt(z * s * (n - s) / n) * Integer.signum(i - (n >> 1));
            final int ll = Math.max(l, (int) (k - i * s / n + sd));
            final int rr = Math.min(r, (int) (k + (n - i) * s / n + sd));
            // CHECKSTYLE: stop regex
            System.out.printf("%d [%d, %d] (k=%.3f * %d) -> [%d, %d] (k=%.3f * %d)%n",
                k, l, r, (double) i / n, n, ll, rr, (double) (k - ll + 1) / (rr - ll + 1), rr - ll + 1);
            // CHECKSTYLE: start regex
            printSubSamplingSize(ll, rr, k);
        }
    }

    /**
     * Prints the size of the Kiwiel Floyd-Rivest recursive subset samples.
     * This method is for information purposes. It is intended to be pasted into JShell
     * and called with various parameters. Example:
     *
     * <pre>{@code
     * jshell> printSubSamplingSize(0, 1000000, 5000)
     * 5000 [0, 1000000] (k=0.005 * 1000001) -> [4843, 9843] (k=0.032 * 5001)
     * 5000 [4843, 9843] (k=0.032 * 5001) -> [4977, 5124] (k=0.162 * 148)
     *
     * jshell> printSubSamplingSize(0, 1000000, 500000)
     * 500000 [0, 1000000] (k=0.500 * 1000001) -> [497631, 502631] (k=0.474 * 5001)
     * 500000 [497631, 502631] (k=0.474 * 5001) -> [499913, 500059] (k=0.599 * 147)
     * }</pre>
     *
     * @param l Left bound (inclusive).
     * @param r Right bound (inclusive).
     * @param k Target index.
     */
    static void printKSubSamplingSize(int l, int r, int k) {
        int n = r - l;
        if (n > 600) {
            // Floyd-Rivest sub-sampling
            ++n;
            // Step 1: Choose sample size s <= n-1 and gap g > 0
            final double z = Math.log(n);
            // sample size = alpha * n^(2/3) * ln(n)^1/3  (4.1)
            // sample size = alpha * n^(2/3)              (4.17; original Floyd-Rivest size)
            final double s = 0.5 * Math.exp(0.6666666666666666 * z) * Math.cbrt(z);
            //final double s = 0.5 * Math.exp(0.6666666666666666 * z);
            // gap = sqrt(beta * s * ln(n))
            final double g = Math.sqrt(0.25 * s * z);
            final int rs = (int) (l + s - 1);
            // Step 3: pivot selection
            final double isn = (k - l + 1) * s / n;
            final int ku = (int) Math.max(Math.floor(l - 1 + isn - g), l);
            final int kv = (int) Math.min(Math.ceil(l - 1 + isn + g), rs);
            // CHECKSTYLE: stop regex
            System.out.printf("%d [%d, %d] (k=%.3f * %d) -> [0, %d, %d, %d] (ku=%.3f; kv=%.3f)%n",
                k, l, r, (double) (k - l + 1) / n, n, ku, kv, rs, (double) (ku + 1) / (rs + 1), (double) (kv + 1) / (rs + 1));
            // CHECKSTYLE: start regex
            printKSubSamplingSize(0, rs, ku);
            printKSubSamplingSize(0, rs, kv);
        }
    }

    /**
     * This is not a test. It runs the introselect algorithm with data that may trigger excess
     * recursion when using the Floyd-Rivest algorithm. Use of a random sample can avoid
     * excess recursion when the local data is non-representative of the range to partition.
     */
    @ParameterizedTest
    @MethodSource
    @Disabled("Used for testing")
    void testFloydRivestRecursion(int n, int subSamplingSize, PivotingStrategy sp, int controlFlags,
        PairedKeyStrategy pk, double recursionMultiple, int recursionConstant) {
        final AbstractDataSource source = new AbstractDataSource() {
            @Override
            protected int getLength() {
                return n;
            }
        };
        source.setRange(0);
        source.setup();
        // Target the "median"
        final int[] k = {source.getLength() >> 1};
        final Partition p = new Partition(sp, QS, EC, subSamplingSize)
            .setPairedKeyStrategy(pk)
            .setRecursionMultiple(recursionMultiple)
            .setRecursionConstant(recursionConstant);
        p.setControlFlags(controlFlags);
        final ArrayList<String> excess = new ArrayList<>();
        for (int i = 0; i < source.size(); i++) {
            final int index = i;
            p.setRecursionConsumer(v -> excess.add(String.format(
                "%d: %s%n", index, source.getDataSampleInfo(index))));
            p.partitionISP(source.getDataSample(i), k, 1);
        }
        TestUtils.printf("n=%d, su=%d, %s, flags=%d : stopped=%d%n",
            n, subSamplingSize, sp, controlFlags, excess.size());
        //excess.forEach(TestUtils::printf);
    }

    static Stream<Arguments> testFloydRivestRecursion() {
        final Stream.Builder<Arguments> builder = Stream.builder();

        // The following test cases show that using FR is better than median of 3 at avoiding
        // excess recursion; but not as good as median of 9.
        // However FR is faster than median of 9 on many datasets (over two-fold on large data).
        // To mitigate worst-case recursion when using FR we can use a random sub-sample
        // allowing the speed of FR without the weakness of excess recursion on patterned data.

        // n=5000 : # samples = 402
        // These use the original FR size of 600.

        // Recursion stopper using max depth
        //n=5000, su=2147483647, MEDIAN_OF_3, flags=0 : stopped=25
        //n=5000, su=600, MEDIAN_OF_3, flags=0 : stopped=8
        //n=5000, su=600, MEDIAN_OF_9, flags=0 : stopped=3
        //n=5000, su=600, MEDIAN_OF_3, flags=2 : stopped=2
        //n=5000, su=2147483647, MEDIAN_OF_9, flags=0 : stopped=0
        //n=5000, su=600, MEDIAN_OF_9, flags=2 : stopped=0

        builder.add(Arguments.of(5000, Integer.MAX_VALUE, PivotingStrategy.MEDIAN_OF_3, 0, PairedKeyStrategy.PAIRED_KEYS, 2, 0));
        builder.add(Arguments.of(5000, 600, PivotingStrategy.MEDIAN_OF_3, 0, PairedKeyStrategy.PAIRED_KEYS, 2, 0));
        builder.add(Arguments.of(5000, 600, PivotingStrategy.MEDIAN_OF_9, 0, PairedKeyStrategy.PAIRED_KEYS, 2, 0));
        builder.add(Arguments.of(5000, 600, PivotingStrategy.MEDIAN_OF_3, Partition.FLAG_RANDOM_SAMPLING, PairedKeyStrategy.PAIRED_KEYS, 2, 0));
        builder.add(Arguments.of(5000, Integer.MAX_VALUE, PivotingStrategy.MEDIAN_OF_9, 0, PairedKeyStrategy.PAIRED_KEYS, 2, 0));
        builder.add(Arguments.of(5000, 600, PivotingStrategy.MEDIAN_OF_9, Partition.FLAG_RANDOM_SAMPLING, PairedKeyStrategy.PAIRED_KEYS, 2, 0));

        // At the threshold for random sub-sampling (see below where the constant
        // has been copied from an early version which used FR random sub-sampling)
        // n=25000 : # samples = 462

        // Threshold to use a random sub-sample for the Floyd-Rivest algorithm. Note:
        // Random sampling is a redundant overhead on fully random data and will part
        // destroy sorted data. On data that is structured with repeat patterns, the
        // shuffle removes side-effects of patterns and stabilises performance where the
        // standard Floyd-Rivest algorithm (with a non-random local sample) will recurse
        // excessively and trigger a switch to the stopper function. The threshold has
        // been chosen at a level where average performance over a variety of data
        // distributions shows no performance loss. Individual distributions may be better
        // or worse at different thresholds. On random data the impact is minimal; on
        // sorted data the impact is approximately 10%. On data with patterns that trigger
        // excess recursion this can increase performance by an order of magnitude. Note
        // that the stopper will still be used to avoid worst-case quickselect performance
        // if this threshold is not appropriate for the input data.
        final int randomSubSamplingSize = 25000;

        //n=25000, su=2147483647, MEDIAN_OF_9, flags=0 : stopped=0
        //n=25000, su=1200, MEDIAN_OF_9, flags=0 : stopped=3
        //n=25000, su=1200, MEDIAN_OF_9, flags=2 : stopped=0
        builder.add(Arguments.of(randomSubSamplingSize, Integer.MAX_VALUE,
            PivotingStrategy.MEDIAN_OF_9, 0, PairedKeyStrategy.PAIRED_KEYS, 2, 0));
        builder.add(Arguments.of(randomSubSamplingSize, Partition.SELECT_SUB_SAMPLING_SIZE,
            PivotingStrategy.MEDIAN_OF_9, 0, PairedKeyStrategy.PAIRED_KEYS, 2, 0));
        builder.add(Arguments.of(randomSubSamplingSize, Partition.SELECT_SUB_SAMPLING_SIZE,
            PivotingStrategy.MEDIAN_OF_9, Partition.FLAG_RANDOM_SAMPLING, PairedKeyStrategy.PAIRED_KEYS, 2, 0));

        // Recursion stopper using halving after c iterations.
        // Use c=5 for 98% confidence the length will half.

        //n=5000, su=2147483647, MEDIAN_OF_3, flags=0 : stopped=47
        //n=5000, su=600, MEDIAN_OF_3, flags=0 : stopped=69
        //n=5000, su=600, MEDIAN_OF_9, flags=0 : stopped=25
        //n=5000, su=600, MEDIAN_OF_3, flags=2 : stopped=22
        //n=5000, su=2147483647, MEDIAN_OF_9, flags=0 : stopped=0
        //n=5000, su=600, MEDIAN_OF_9, flags=2 : stopped=0

        builder.add(Arguments.of(5000, Integer.MAX_VALUE, PivotingStrategy.MEDIAN_OF_3, 0, PairedKeyStrategy.PAIRED_KEYS_2, 5, 1));
        builder.add(Arguments.of(5000, 600, PivotingStrategy.MEDIAN_OF_3, 0, PairedKeyStrategy.PAIRED_KEYS_2, 5, 1));
        builder.add(Arguments.of(5000, 600, PivotingStrategy.MEDIAN_OF_9, 0, PairedKeyStrategy.PAIRED_KEYS_2, 5, 1));
        builder.add(Arguments.of(5000, 600, PivotingStrategy.MEDIAN_OF_3, Partition.FLAG_RANDOM_SAMPLING, PairedKeyStrategy.PAIRED_KEYS_2, 5, 1));
        builder.add(Arguments.of(5000, Integer.MAX_VALUE, PivotingStrategy.MEDIAN_OF_9, 0, PairedKeyStrategy.PAIRED_KEYS_2, 5, 1));
        builder.add(Arguments.of(5000, 600, PivotingStrategy.MEDIAN_OF_9, Partition.FLAG_RANDOM_SAMPLING, PairedKeyStrategy.PAIRED_KEYS_2, 5, 1));

        // At the threshold for random sub-sampling

        //n=25000, su=2147483647, MEDIAN_OF_9, flags=0 : stopped=0
        //n=25000, su=1200, MEDIAN_OF_9, flags=0 : stopped=42
        //n=25000, su=1200, MEDIAN_OF_9, flags=2 : stopped=0

        builder.add(Arguments.of(randomSubSamplingSize, Integer.MAX_VALUE,
            PivotingStrategy.MEDIAN_OF_9, 0, PairedKeyStrategy.PAIRED_KEYS_2, 5, 1));
        builder.add(Arguments.of(randomSubSamplingSize, Partition.SELECT_SUB_SAMPLING_SIZE,
            PivotingStrategy.MEDIAN_OF_9, 0, PairedKeyStrategy.PAIRED_KEYS_2, 5, 1));
        builder.add(Arguments.of(randomSubSamplingSize, Partition.SELECT_SUB_SAMPLING_SIZE,
            PivotingStrategy.MEDIAN_OF_9, Partition.FLAG_RANDOM_SAMPLING, PairedKeyStrategy.PAIRED_KEYS_2, 5, 1));

        // Recursion stopper using partition length sum as twice the length

        //n=5000, su=2147483647, MEDIAN_OF_3, flags=0 : stopped=66
        //n=5000, su=600, MEDIAN_OF_3, flags=0 : stopped=160
        //n=5000, su=600, MEDIAN_OF_9, flags=0 : stopped=41
        //n=5000, su=600, MEDIAN_OF_3, flags=2 : stopped=84
        //n=5000, su=2147483647, MEDIAN_OF_9, flags=0 : stopped=10
        //n=5000, su=600, MEDIAN_OF_9, flags=2 : stopped=7

        builder.add(Arguments.of(5000, Integer.MAX_VALUE, PivotingStrategy.MEDIAN_OF_3, 0, PairedKeyStrategy.PAIRED_KEYS_LEN, 2, 0));
        builder.add(Arguments.of(5000, 600, PivotingStrategy.MEDIAN_OF_3, 0, PairedKeyStrategy.PAIRED_KEYS_LEN, 2, 0));
        builder.add(Arguments.of(5000, 600, PivotingStrategy.MEDIAN_OF_9, 0, PairedKeyStrategy.PAIRED_KEYS_LEN, 2, 0));
        builder.add(Arguments.of(5000, 600, PivotingStrategy.MEDIAN_OF_3, Partition.FLAG_RANDOM_SAMPLING, PairedKeyStrategy.PAIRED_KEYS_LEN, 2, 0));
        builder.add(Arguments.of(5000, Integer.MAX_VALUE, PivotingStrategy.MEDIAN_OF_9, 0, PairedKeyStrategy.PAIRED_KEYS_LEN, 2, 0));
        builder.add(Arguments.of(5000, 600, PivotingStrategy.MEDIAN_OF_9, Partition.FLAG_RANDOM_SAMPLING, PairedKeyStrategy.PAIRED_KEYS_LEN, 2, 0));

        // At the threshold for random sub-sampling

        //n=25000, su=2147483647, MEDIAN_OF_9, flags=0 : stopped=11
        //n=25000, su=1200, MEDIAN_OF_9, flags=0 : stopped=70
        //n=25000, su=1200, MEDIAN_OF_9, flags=2 : stopped=14

        builder.add(Arguments.of(randomSubSamplingSize, Integer.MAX_VALUE,
            PivotingStrategy.MEDIAN_OF_9, 0, PairedKeyStrategy.PAIRED_KEYS_LEN, 2, 0));
        builder.add(Arguments.of(randomSubSamplingSize, Partition.SELECT_SUB_SAMPLING_SIZE,
            PivotingStrategy.MEDIAN_OF_9, 0, PairedKeyStrategy.PAIRED_KEYS_LEN, 2, 0));
        builder.add(Arguments.of(randomSubSamplingSize, Partition.SELECT_SUB_SAMPLING_SIZE,
            PivotingStrategy.MEDIAN_OF_9, Partition.FLAG_RANDOM_SAMPLING, PairedKeyStrategy.PAIRED_KEYS_LEN, 2, 0));

        return builder.build();
    }

    @Test
    @Disabled("Used for testing")
    void testConfiguredPartition() {
        final AbstractDataSource source = new AbstractDataSource() {
            @Override
            protected int getLength() {
                //return 1 << 16;
                return 1 << 20;
            }
        };
        source.setRange(0);
        source.setRngSeed(0xdeadbeef);

        // Uncomment for Valois
        source.setDistribution(
            Distribution.RANDOM
        //    Distribution.SORTED,
        //    Distribution.ONEZERO,
        //    Distribution.M3KILLER,
        //    Distribution.ROTATED,
        //    Distribution.TWOFACED,
        //    Distribution.ORGANPIPE
        );
        source.setModification(Modification.COPY);
        source.setSeed(Integer.MAX_VALUE);
        source.setup();
        // Target the "median"
        //final int[] k = {source.getLength() >> 1};
        // Target the "edge"
        final int[] k = {source.getLength() / 20};

        // Stop based on recursion depth
        //final Partition p = new Partition(PivotingStrategy.MEDIAN_OF_3, QS, EC, Integer.MAX_VALUE)
        //    .setPairedKeyStrategy(PairedKeyStrategy.PAIRED_KEYS)
        //    .setRecursionMultiple(2);

        // Stop based on steps to half the initial length
        final int su = 1200;
        final Partition p = new Partition(PivotingStrategy.MEDIAN_OF_5, QS, EC, su)
            .setPairedKeyStrategy(PairedKeyStrategy.KEY_RANGE)
            .setControlFlags(Partition.FLAG_RANDOM_SAMPLING |
                Partition.FLAG_QA_FAR_STEP | Partition.FLAG_QA_SAMPLE_K)
            // 92.9% confidence
            .setRecursionMultiple(4)
            .setRecursionConstant(1);

        for (int i = 0; i < source.size(); i++) {
            final int index = i;
            p.setRecursionConsumer(v -> TestUtils.printf("%d: %s (%d)%n", index, source.getDataSampleInfo(index), v));
            //p.setSPStrategy(SPStrategy.SP);
            //p.partitionISP(source.getDataSample(i), k, 1);
            p.partitionQA(source.getDataSample(i), k, 1);
        }
    }

    @Test
    @Disabled("Used for testing the QA implementation against select")
    void testQAAndSelect() {
        final AbstractDataSource source = new AbstractDataSource() {
            @Override
            protected int getLength() {
                return 15000;
            }
        };

        source.setup();
        // Target the "median"
        final int[] k = {source.getLength() >> 1};

        // QA2 algorithm should be configured the same as SELECT

        for (int i = 0; i < source.size(); i++) {
            final double[] a = source.getDataSample(i);
            Partition.partitionQA2(a, k, 1);
            final double[] b = source.getDataSample(i);
            Selection.select(b, k);
            if (!Arrays.equals(a, b)) {
                TestUtils.printf("%d: %s%n", i, source.getDataSampleInfo(i));
            }
        }
    }
}
