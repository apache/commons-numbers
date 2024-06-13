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
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link Selection} and {@link QuickSelect}.
 */
class SelectionTest {
    /** Default sub-sampling size for the Floyd-Rivest algorithm. */
    private static final int SU = 1200;
    /** Signal to ignore the range of [from, to). */
    private static final int IGNORE_FROM = -1236481268;

    /**
     * {@link UpdatingInterval} for range {@code [left, right]}.
     */
    static final class RangeInterval implements UpdatingInterval {
        /** Left bound of the interval. */
        private int left;
        /** Right bound of the interval. */
        private int right;

        /**
         * @param left Left bound.
         * @param right Right bound.
         */
        RangeInterval(int left, int right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public int left() {
            return left;
        }

        @Override
        public int right() {
            return right;
        }

        @Override
        public int updateLeft(int k) {
            // Assume left < k <= right
            left = k;
            return k;
        }

        @Override
        public int updateRight(int k) {
            // Assume left <= k < right
            right = k;
            return k;
        }

        @Override
        public UpdatingInterval splitLeft(int ka, int kb) {
            // Assume left < ka <= kb < right
            final int lower = left;
            left = kb + 1;
            return new RangeInterval(lower, ka - 1);
        }
    }

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
     * Return a sorted copy of the {@code values}.
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
     * Move NaN values to the end of the array.
     * This allows all other values to be compared using {@code <, ==, >} operators (with
     * the exception of signed zeros).
     *
     * @param data Values.
     * @return index of last non-NaN value (or -1)
     */
    private static int sortNaN(double[] data) {
        int end = data.length;
        // Find first non-NaN
        while (--end >= 0) {
            if (!Double.isNaN(data[end])) {
                break;
            }
        }
        for (int i = end; --i >= 0;) {
            final double v = data[i];
            if (Double.isNaN(v)) {
                // swap(data, i, end--)
                data[i] = data[end];
                data[end] = v;
                end--;
            }
        }
        return end;
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

    /**
     * Shuffles the entries of the given array.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @return Shuffled input array.
     */
    // TODO - replace with Commons RNG 1.6: o.a.c.rng.sampling.ArraySampler
    private static double[] shuffle(UniformRandomProvider rng, double[] array) {
        for (int i = array.length; i > 1; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
        return array;
    }

    /**
     * Swaps the two specified elements in the array.
     *
     * @param array Array.
     * @param i First index.
     * @param j Second index.
     */
    private static void swap(double[] array, int i, int j) {
        final double tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleHeapSelect", "testDoubleSelectMinMax", "testDoubleSelectMinMax2"})
    void testDoubleHeapSelectLeft(double[] values, int from, int to) {
        final double[] sorted = sort(values, from, to);

        final double[] x = values.clone();
        final DoubleRangePartitionFunction fun = QuickSelect::heapSelectLeft;

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
    @MethodSource(value = {"testDoubleHeapSelect", "testDoubleSelectMinMax", "testDoubleSelectMinMax2"})
    void testDoubleHeapSelectRight(double[] values, int from, int to) {
        final double[] sorted = sort(values, from, to);

        final double[] x = values.clone();
        final DoubleRangePartitionFunction fun = QuickSelect::heapSelectRight;

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

    static Stream<Arguments> testDoubleHeapSelect() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of(new double[] {1}, 0, 0));
        builder.add(Arguments.of(new double[] {3, 2, 1}, 1, 1));
        builder.add(Arguments.of(new double[] {2, 1}, 0, 1));
        builder.add(Arguments.of(new double[] {4, 3, 2, 1}, 1, 2));
        builder.add(Arguments.of(new double[] {-1, 0.0, -0.5, -0.5, 1}, 0, 4));
        builder.add(Arguments.of(new double[] {-1, 0.0, -0.5, -0.5, 1}, 0, 2));
        builder.add(Arguments.of(new double[] {1, 0.0, -0.5, -0.5, -1}, 0, 4));
        builder.add(Arguments.of(new double[] {-1, 2, -3, 4, -4, 3, -2, 1}, 1, 6));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleHeapSelectRange"})
    void testDoubleHeapSelectRange(double[] values, int from, int to, int k1, int k2) {
        assertPartitionRange(sort(values, from, to),
            QuickSelect::heapSelect, values, from, to, k1, k2);
    }

    static Stream<Arguments> testDoubleHeapSelectRange() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of(new double[] {-1, 2, -3, 4, -4, 3, -2, 1}, 0, 7, 1, 2));
        builder.add(Arguments.of(new double[] {-1, 2, -3, 4, -4, 3, -2, 1}, 0, 7, 2, 2));
        builder.add(Arguments.of(new double[] {-1, 2, -3, 4, -4, 3, -2, 1}, 0, 7, 5, 7));
        builder.add(Arguments.of(new double[] {-1, 2, -3, 4, -4, 3, -2, 1}, 0, 7, 1, 6));
        builder.add(Arguments.of(new double[] {-1, 2, -3, 4, -4, 3, -2, 1}, 0, 7, 0, 3));
        builder.add(Arguments.of(new double[] {-1, 2, -3, 4, -4, 3, -2, 1}, 0, 7, 4, 7));
        return builder.build();
    }

    static Stream<Arguments> testDoubleSelectMinMax() {
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
        builder.add(Arguments.of(new double[] {-0.5, 0.0}, 0, 1));
        builder.add(Arguments.of(new double[] {0.0, -0.5}, 0, 1));
        builder.add(Arguments.of(new double[] {-0.5, -0.5}, 0, 1));
        builder.add(Arguments.of(new double[] {0.0, 0.0}, 0, 1));
        builder.add(Arguments.of(new double[] {0.0, -0.5, 0.0, -0.5}, 0, 3));
        builder.add(Arguments.of(new double[] {-0.5, 0.0, -0.5, 0.0}, 0, 3));
        builder.add(Arguments.of(new double[] {0.0, -0.5, -0.5, 0.0}, 0, 3));
        builder.add(Arguments.of(new double[] {-0.5, 0.0, 0.0, -0.5}, 0, 3));
        return builder.build();
    }

    static Stream<Arguments> testDoubleSelectMinMax2() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final double[] values = {-0.5, 0.0, 1};
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
        builder.add(Arguments.of(new double[] {-0.5, 0.0}, 0, 1));
        builder.add(Arguments.of(new double[] {0.0, -0.5}, 0, 1));
        builder.add(Arguments.of(new double[] {-0.5, -0.5}, 0, 1));
        builder.add(Arguments.of(new double[] {0.0, 0.0}, 0, 1));
        builder.add(Arguments.of(new double[] {0.0, -0.5, 0.0, -0.5}, 0, 3));
        builder.add(Arguments.of(new double[] {-0.5, 0.0, -0.5, 0.0}, 0, 3));
        builder.add(Arguments.of(new double[] {0.0, -0.5, -0.5, 0.0}, 0, 3));
        builder.add(Arguments.of(new double[] {-0.5, 0.0, 0.0, -0.5}, 0, 3));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleHeapSelect", "testDoubleSelectMinMax", "testDoubleSelectMinMax2"})
    void testDoubleSortSelectLeft(double[] values, int from, int to) {
        final double[] sorted = sort(values, from, to);

        final double[] x = values.clone();
        final DoubleRangePartitionFunction fun = (a, l, r, ka, kb) ->
            QuickSelect.sortSelectLeft(a, l, r, kb);

        for (int k = from; k <= to; k++) {
            assertPartitionRange(sorted, fun, x.clone(), from, to, from, k);
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleHeapSelect", "testDoubleSelectMinMax", "testDoubleSelectMinMax2"})
    void testDoubleSortSelectRight(double[] values, int from, int to) {
        final double[] sorted = sort(values, from, to);

        final double[] x = values.clone();
        final DoubleRangePartitionFunction fun = (a, l, r, ka, kb) ->
            QuickSelect.sortSelectRight(a, l, r, ka);

        for (int k = from; k <= to; k++) {
            assertPartitionRange(sorted, fun, x.clone(), from, to, k, to);
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleHeapSelectRange"})
    void testDoubleSortSelectRange(double[] values, int from, int to, int k1, int k2) {
        assertPartitionRange(sort(values, from, to),
            QuickSelect::sortSelect, values, from, to, k1, k2);
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

    @ParameterizedTest
    @MethodSource
    void testDoubleSelectThrows(double[] values, int[] indices, int from, int to) {
        final double[] x = values.clone();
        final int[] k = indices.clone();
        if (from == IGNORE_FROM) {
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Selection.select(values, indices));
        } else {
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Selection.select(values, from, to, indices));
        }
        Assertions.assertArrayEquals(x, values, "Data modified");
        Assertions.assertArrayEquals(k, indices, "Indices modified");
        if (k.length != 1) {
            return;
        }
        if (from == IGNORE_FROM) {
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Selection.select(values, k[0]));
        } else {
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Selection.select(values, from, to, k[0]));
        }
        Assertions.assertArrayEquals(x, values, "Data modified for single k");
    }

    static Stream<Arguments> testDoubleSelectThrows() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final double[] a = {1, 2, 3, Double.NaN, 0.0, -0.0};
        // Invalid range
        builder.add(Arguments.of(a.clone(), new int[] {0}, 0, a.length + 1));
        builder.add(Arguments.of(a.clone(), new int[] {0}, -1, a.length));
        builder.add(Arguments.of(a.clone(), new int[] {0}, 0, 0));
        builder.add(Arguments.of(a.clone(), new int[] {0}, a.length, 0));
        builder.add(Arguments.of(a.clone(), new int[] {1}, 3, 1));
        // Single k
        // Full length
        builder.add(Arguments.of(a.clone(), new int[] {-1}, IGNORE_FROM, 0));
        builder.add(Arguments.of(a.clone(), new int[] {10}, IGNORE_FROM, 0));
        // Range
        builder.add(Arguments.of(a.clone(), new int[] {-1}, 0, 5));
        builder.add(Arguments.of(a.clone(), new int[] {1}, 2, 5));
        builder.add(Arguments.of(a.clone(), new int[] {10}, 2, 5));
        // Multiple k, some invalid
        // Full length
        builder.add(Arguments.of(a.clone(), new int[] {0, -1, 1, 2}, IGNORE_FROM, 0));
        builder.add(Arguments.of(a.clone(), new int[] {0, 2, 3, 10}, IGNORE_FROM, 0));
        // Range
        builder.add(Arguments.of(a.clone(), new int[] {0, -1, 1, 2}, 0, 5));
        builder.add(Arguments.of(a.clone(), new int[] {2, 3, 1}, 2, 5));
        builder.add(Arguments.of(a.clone(), new int[] {2, 10, 3}, 2, 5));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoublePartition", "testDoublePartitionBigData"})
    void testDoubleQuickSelectAdaptiveFRSampling(double[] values, int[] indices) {
        assertQuickSelectAdaptive(values, indices, QuickSelect.MODE_FR_SAMPLING);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoublePartition", "testDoublePartitionBigData"})
    void testDoubleQuickSelectAdaptiveSampling(double[] values, int[] indices) {
        assertQuickSelectAdaptive(values, indices, QuickSelect.MODE_SAMPLING);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoublePartition", "testDoublePartitionBigData"})
    void testDoubleQuickSelectAdaptiveAdaption(double[] values, int[] indices) {
        assertQuickSelectAdaptive(values, indices, QuickSelect.MODE_ADAPTION);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoublePartition", "testDoublePartitionBigData"})
    void testDoubleQuickSelectAdaptiveStrict(double[] values, int[] indices) {
        assertQuickSelectAdaptive(values, indices, QuickSelect.MODE_STRICT);
    }

    private static void assertQuickSelectAdaptive(double[] values, int[] indices, int mode) {
        Assumptions.assumeTrue(indices.length == 1 ||
            (indices.length == 2 && Math.abs(indices[1] - indices[0]) < 10));
        final int k1 = Math.min(indices[0], indices[indices.length - 1]);
        final int kn = Math.max(indices[0], indices[indices.length - 1]);
        assertPartition(values, indices, (a, k, n) -> {
            final int right = sortNaN(a);
            if (right < 1) {
                return;
            }
            replaceNegativeZeros(a, 0, right);
            QuickSelect.quickSelectAdaptive(a, 0, right, k1, kn, new int[1], mode);
            restoreNegativeZeros(a, 0, right);
        }, true);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoublePartition", "testDoublePartitionBigData"})
    void testDoubleDualPivotQuickSelectMaxRecursion(double[] values, int[] indices) {
        assertPartition(values, indices, (a, k, n) -> {
            final int right = sortNaN(a);
            // Sanitise indices
            k = Arrays.stream(k).filter(i -> i <= right).toArray();
            if (right < 1 || k.length == 0) {
                return;
            }
            replaceNegativeZeros(a, 0, right);
            QuickSelect.dualPivotQuickSelect(a, 0, right,
                IndexSupport.createUpdatingInterval(0, right, k, k.length),
                QuickSelect.dualPivotFlags(2, 5));
            restoreNegativeZeros(a, 0, right);
        }, false);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoublePartition", "testDoublePartitionBigData"})
    void testDoubleSelect(double[] values, int[] indices) {
        assertPartition(values, indices, (a, k, n) -> {
            double[] b = a;
            if (n == 1) {
                b = a.clone();
                Selection.select(b, k[0]);
            }
            Selection.select(a, Arrays.copyOf(k, n));
            if (n == 1) {
                Assertions.assertArrayEquals(a, b, "single k mismatch");
            }
        }, false);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoublePartition", "testDoublePartitionBigData"})
    void testDoubleSelectRange(double[] values, int[] indices) {
        assertPartition(values, indices, (a, k, n) -> {
            double[] b = a;
            if (n == 1) {
                b = a.clone();
                Selection.select(b, 0, b.length, k[0]);
            }
            Selection.select(a, 0, a.length, Arrays.copyOf(k, n));
            if (n == 1) {
                Assertions.assertArrayEquals(a, b, "single k mismatch");
            }
        }, false);
    }

    static void assertPartition(double[] values, int[] indices, DoublePartitionFunction function,
        boolean sortedRange) {
        final double[] data = values.clone();
        final double[] sorted = sort(values);
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
        if (sortedRange) {
            final double[] a = Arrays.copyOfRange(sorted, indices[0], k + 1);
            final double[] b = Arrays.copyOfRange(data, indices[0], k + 1);
            Assertions.assertArrayEquals(a, b, "Entire range of indices is not sorted");
        }
        Arrays.sort(data);
        Assertions.assertArrayEquals(sorted, data, "Data destroyed");
    }

    static Stream<Arguments> testDoublePartition() {
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
                        shuffle(rng, values.clone()),
                        indices.clone()));
                    builder.add(Arguments.of(
                        shuffle(rng, zeros.clone()),
                        indices.clone()));
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
                        shuffle(rng, values.clone()),
                        indices.clone()));
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
        // Also partition with the pivots in P1 and P2 using thirds.
        final int third = (int) (n / 3.0);
        // Use a fix seed to ensure we hit coverage with only 5 loops.
        rng = RandomSource.XO_SHI_RO_128_PP.create(-8111061151820577011L);
        for (int i = 0; i < 5; i++) {
            builder.add(Arguments.of(shuffle(rng, x.clone()), new int[] {n >> 1}));
            builder.add(Arguments.of(shuffle(rng, x.clone()),
                new int[] {third, 2 * third}));
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
        // Reverse data. Makes it simple to detect failed range selection.
        final double[] a = IntStream.range(0, 50).asDoubleStream().toArray();
        for (int i = -1, j = a.length; ++i < --j;) {
            final double v = a[i];
            a[i] = a[j];
            a[j] = v;
        }
        builder.add(Arguments.of(a, new int[] {1, 1}));
        builder.add(Arguments.of(a, new int[] {1, 2}));
        builder.add(Arguments.of(a, new int[] {10, 12}));
        builder.add(Arguments.of(a, new int[] {10, 42}));
        builder.add(Arguments.of(a, new int[] {1, 48}));
        builder.add(Arguments.of(a, new int[] {48, 49}));
        return builder.build();
    }

    static Stream<Arguments> testDoublePartitionBigData() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create(123);
        // Sizes above the threshold (1200) for recursive partitioning
        for (final int size : new int[] {1000, 5000, 10000}) {
            final double[] a = IntStream.range(0, size).asDoubleStream().toArray();
            // With repeat elements
            final double[] b = rng.ints(size, 0, size >> 3).asDoubleStream().toArray();
            for (int i = 0; i < 15; i++) {
                builder.add(Arguments.of(
                    shuffle(rng, a.clone()),
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
        builder.add(Arguments.of(x.clone(), new int[] {n - 1 - 20}));
        // Constant value when using FR partitioning
        Arrays.fill(x, 1.23);
        builder.add(Arguments.of(x, new int[] {x.length >>> 1}));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testDoubleExpandPartition(double[] values, int start, int end, int pivot0, int pivot1) {
        final int[] upper = new int[1];
        final double[] sorted = sort(values);
        final double v = values[pivot0];
        final int p0 = QuickSelect.expandPartition(values, 0, values.length - 1, start, end, pivot0, pivot1, upper);
        final int p1 = upper[0];
        for (int i = 0; i < p0; i++) {
            final int index = i;
            Assertions.assertTrue(values[i] < v,
                () -> String.format("[%d] : %s < %s", index, values[index], v));
        }
        for (int i = p0; i <= p1; i++) {
            final int index = i;
            Assertions.assertEquals(v, values[i],
                () -> String.format("[%d] : %s == %s", index, values[index], v));
        }
        for (int i = p1 + 1; i < values.length; i++) {
            final int index = i;
            Assertions.assertTrue(values[i] > v,
                () -> String.format("[%d] : %s > %s", index, values[index], v));
        }
        Arrays.sort(values);
        Assertions.assertArrayEquals(sorted, values, "Data destroyed");
    }

    static Stream<Arguments> testDoubleExpandPartition() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // Create data:
        // |l          |start       |p0  p1|    end|            r|
        // |     ???   |     <      |  ==  |   >   |     ???     |
        // Arguments: data, start, end, pivot0, pivot1

        // Create the data with unique values 42 and 0 either side of
        // [start, end] (e.g. region ???). These are permuted for -1 and 10
        // to create cases that may or not have to swap elements.

        // Single pivot
        addExpandPartitionArguments(builder, new double[] {42, 1, 2, 3, 4, 0}, 1, 4, 2, 2);
        // Pivot range
        addExpandPartitionArguments(builder, new double[] {42, 1, 2, 2, 3, 0}, 1, 4, 2, 3);
        // Single pivot at start/end
        addExpandPartitionArguments(builder, new double[] {42, 1, 2, 3, 4, 0}, 1, 4, 1, 1);
        addExpandPartitionArguments(builder, new double[] {42, 1, 2, 3, 4, 0}, 1, 4, 4, 4);
        // Pivot range at start/end
        addExpandPartitionArguments(builder, new double[] {42, 1, 1, 2, 3, 0}, 1, 4, 1, 2);
        addExpandPartitionArguments(builder, new double[] {42, 1, 2, 3, 3, 0}, 1, 4, 3, 4);
        addExpandPartitionArguments(builder, new double[] {42, 1, 2, 2, 2, 0}, 1, 4, 2, 4);
        addExpandPartitionArguments(builder, new double[] {42, 1, 1, 1, 2, 0}, 1, 4, 1, 3);
        addExpandPartitionArguments(builder, new double[] {42, 1, 1, 1, 1, 0}, 1, 4, 1, 4);

        // Single pivot at left/right
        addExpandPartitionArguments(builder, new double[] {1, 2, 3, 4, 0}, 0, 3, 0, 0);
        addExpandPartitionArguments(builder, new double[] {42, 1, 2, 3, 4}, 1, 4, 4, 4);
        // Pivot range at left/right
        addExpandPartitionArguments(builder, new double[] {1, 1, 2, 3, 4, 0}, 0, 4, 0, 1);
        addExpandPartitionArguments(builder, new double[] {42, 1, 2, 3, 4, 4}, 1, 5, 4, 5);
        addExpandPartitionArguments(builder, new double[] {1, 1, 1, 1, 2, 0}, 0, 4, 0, 3);
        addExpandPartitionArguments(builder, new double[] {42, 3, 4, 4, 4, 4}, 1, 5, 2, 5);
        addExpandPartitionArguments(builder, new double[] {1, 1, 1, 1, 1, 0}, 0, 4, 0, 4);
        addExpandPartitionArguments(builder, new double[] {42, 4, 4, 4, 4, 4}, 1, 5, 1, 5);

        // Minimum range: [start, end] == length 2
        addExpandPartitionArguments(builder, new double[] {42, 1, 2, 0}, 1, 2, 1, 1);
        addExpandPartitionArguments(builder, new double[] {42, 1, 2, 0}, 1, 2, 2, 2);
        addExpandPartitionArguments(builder, new double[] {42, 1, 1, 0}, 1, 2, 1, 2);
        addExpandPartitionArguments(builder, new double[] {42, 1, 2}, 1, 2, 1, 1);
        addExpandPartitionArguments(builder, new double[] {42, 1, 2}, 1, 2, 2, 2);
        addExpandPartitionArguments(builder, new double[] {42, 1, 1}, 1, 2, 1, 2);
        addExpandPartitionArguments(builder, new double[] {1, 2, 0}, 0, 1, 0, 0);
        addExpandPartitionArguments(builder, new double[] {1, 2, 0}, 0, 1, 1, 1);
        addExpandPartitionArguments(builder, new double[] {1, 1, 0}, 0, 1, 0, 1);
        addExpandPartitionArguments(builder, new double[] {1, 2}, 0, 1, 0, 0);
        addExpandPartitionArguments(builder, new double[] {1, 2}, 0, 1, 1, 1);
        addExpandPartitionArguments(builder, new double[] {1, 1}, 0, 1, 0, 1);

        return builder.build();
    }

    private static void addExpandPartitionArguments(Stream.Builder<Arguments> builder,
        double[] a, int start, int end, int pivot0, int pivot1) {
        builder.add(Arguments.of(a.clone(), start, end, pivot0, pivot1));
        final double[] b = a.clone();
        if (replace(a, 42, -1)) {
            builder.add(Arguments.of(a.clone(), start, end, pivot0, pivot1));
            if (replace(a, 0, 10)) {
                builder.add(Arguments.of(a, start, end, pivot0, pivot1));
            }
        }
        if (replace(b, 0, 10)) {
            builder.add(Arguments.of(b, start, end, pivot0, pivot1));
        }
    }

    private static boolean replace(double[] a, int x, int y) {
        boolean updated = false;
        for (int i = 0; i < a.length; i++) {
            if (a[i] == x) {
                a[i] = y;
                updated = true;
            }
        }
        return updated;
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort"})
    void testDoubleSortUsingHeapSelect(double[] values) {
        Assumptions.assumeTrue(values.length > 0);
        assertSort(values, x -> {
            final int right = sortNaN(x);
            // heapSelect is robust to right <= left
            replaceNegativeZeros(x, 0, right);
            QuickSelect.heapSelect(x, 0, right, 0, right);
            restoreNegativeZeros(x, 0, right);
        });
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort"})
    void testDoubleSortUsingHeapSelectLeft(double[] values) {
        Assumptions.assumeTrue(values.length > 0);
        assertSort(values, x -> {
            final int right = sortNaN(x);
            if (right < 1) {
                return;
            }
            replaceNegativeZeros(x, 0, right);
            QuickSelect.heapSelectLeft(x, 0, right, 0, right);
            restoreNegativeZeros(x, 0, right);
        });
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort"})
    void testDoubleSortUsingHeapSelectRight(double[] values) {
        Assumptions.assumeTrue(values.length > 0);
        assertSort(values, x -> {
            final int right = sortNaN(x);
            if (right < 1) {
                return;
            }
            replaceNegativeZeros(x, 0, right);
            QuickSelect.heapSelectRight(x, 0, right, 0, right);
            restoreNegativeZeros(x, 0, right);
        });
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort"})
    void testDoubleSortUsingSelection(double[] values) {
        // This tests that the select function performs
        // a full sort when the interval is saturated
        assertSort(values, a -> {
            final int right = sortNaN(a);
            if (right < 1) {
                return;
            }
            replaceNegativeZeros(a, 0, right);
            QuickSelect.dualPivotQuickSelect(a, 0, right, new RangeInterval(0, right),
                QuickSelect.dualPivotFlags(QuickSelect.dualPivotMaxDepth(right), 20));
            restoreNegativeZeros(a, 0, right);
        });
    }

    private static void assertSort(double[] values, Consumer<double[]> function) {
        final double[] data = values.clone();
        final double[] sorted = sort(values);
        function.accept(data);
        Assertions.assertArrayEquals(sorted, data);
    }

    static Stream<double[]> testDoubleSort() {
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

    @Test
    void testDualPivotMaxDepth() {
        // Reasonable behaviour at small x
        Assertions.assertEquals(0, log3(0));
        Assertions.assertEquals(0, log3(1));
        Assertions.assertEquals(1, log3(2));
        Assertions.assertEquals(1, log3(3));
        Assertions.assertEquals(1, log3(4));
        Assertions.assertEquals(1, log3(5));
        Assertions.assertEquals(1, log3(6));
        Assertions.assertEquals(1, log3(7));
        Assertions.assertEquals(2, log3(8));
        // log3(2^31-1) = 19.5588223...
        Assertions.assertEquals(19, log3(Integer.MAX_VALUE));
        // Create a series of powers of 3, start at 3^2
        long p = 3;
        for (int i = 2;; i++) {
            p *= 3;
            if (p > Integer.MAX_VALUE) {
                break;
            }
            final int x = (int) p;
            // Computes round(log3(x)) when x is close to a power of 3
            Assertions.assertEquals(i, log3(x - 1));
            Assertions.assertEquals(i, log3(x));
            Assertions.assertEquals(i, log3(x + 1));
            // Half-way point is within the bracket [i, i+1]
            final int y = (int) Math.floor(Math.pow(3, i + 0.5));
            Assertions.assertTrue(log3(y) >= i);
            Assertions.assertTrue(log3(y + 1) <= i + 1);
        }
    }

    /**
     * Compute an approximation to log3(x).
     *
     * @param x Value
     * @return log3(x)
     */
    private static int log3(int x) {
        // Use half of the dual-pivot max recursion depth
        return QuickSelect.dualPivotMaxDepth(x) >>> 1;
    }
}
