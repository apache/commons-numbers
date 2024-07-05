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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.ArraySampler;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link KthSelector}.
 */
class KthSelectorTest {
    @ParameterizedTest
    @MethodSource
    void testPartitionMin(double[] values, int from, int to) {
        final double[] sorted = values.clone();
        Arrays.sort(sorted, from, to);
        KthSelector.partitionMin(values, from, to);
        Assertions.assertEquals(sorted[from], values[from]);
        // Check the data is the same
        Arrays.sort(values, from, to);
        Assertions.assertArrayEquals(sorted, values, "Data destroyed");
    }

    static Stream<Arguments> testPartitionMin() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create();
        for (final int size : new int[] {5, 10}) {
            final double[] values = rng.doubles(size).toArray();
            builder.add(Arguments.of(values.clone(), 0, size));
            builder.add(Arguments.of(values.clone(), size >>> 1, size));
            builder.add(Arguments.of(values.clone(), 1, size >>> 1));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testPartitionMax(double[] values, int from, int to) {
        final double[] sorted = values.clone();
        Arrays.sort(sorted, from, to);
        KthSelector.partitionMax(values, from, to);
        Assertions.assertEquals(sorted[to - 1], values[to - 1]);
        // Check the data is the same
        Arrays.sort(values, from, to);
        Assertions.assertArrayEquals(sorted, values, "Data destroyed");
    }

    static Stream<Arguments> testPartitionMax() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create();
        for (final int size : new int[] {5, 10}) {
            final double[] values = rng.doubles(size).toArray();
            builder.add(Arguments.of(values.clone(), 0, size));
            builder.add(Arguments.of(values.clone(), size >>> 1, size));
            builder.add(Arguments.of(values.clone(), 1, size >>> 1));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testSelect(double[] values) {
        final double[] sorted = values.clone();
        Arrays.sort(sorted);
        final KthSelector selector = new KthSelector();
        final double[] kp1 = new double[1];
        for (int i = 0; i < sorted.length; i++) {
            final int k = i;
            double[] x = values.clone();
            Assertions.assertEquals(sorted[k], selector.selectSP(x, k, null), () -> "k[" + k + "]");
            Arrays.sort(x);
            Assertions.assertArrayEquals(sorted, x, () -> "Data destroyed: k[" + k + "]");
            if (k + 1 < sorted.length) {
                x = values.clone();
                Assertions.assertEquals(sorted[k], selector.selectSP(x, k, kp1), () -> "k[" + k + "] with k+1");
                Assertions.assertEquals(sorted[k + 1], kp1[0], () -> "k+1[" + (k + 1) + "]");
                Arrays.sort(x);
                Assertions.assertArrayEquals(sorted, x, () -> "Data destroyed: k[" + k + "] with k+1");
            }
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"testSelect"})
    void testSelectSPN(double[] values) {
        final double[] sorted = values.clone();
        Arrays.sort(sorted);
        final KthSelector selector = new KthSelector();
        final double[] kp1 = new double[1];
        for (int i = 0; i < sorted.length; i++) {
            final int k = i;
            double[] x = values.clone();
            Assertions.assertEquals(sorted[k], selector.selectSPN(x, k, null), () -> "k[" + k + "]");
            Arrays.sort(x);
            Assertions.assertArrayEquals(sorted, x, () -> "Data destroyed: k[" + k + "]");
            if (k + 1 < sorted.length) {
                x = values.clone();
                Assertions.assertEquals(sorted[k], selector.selectSPN(x, k, kp1), () -> "k[" + k + "] with k+1");
                Assertions.assertEquals(sorted[k + 1], kp1[0], () -> "k+1[" + (k + 1) + "]");
                Arrays.sort(x);
                Assertions.assertArrayEquals(sorted, x, () -> "Data destroyed: k[" + k + "] with k+1");
            }
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"testSelect"})
    void testSelectSPWithHeap(double[] values) {
        final double[] sorted = values.clone();
        Arrays.sort(sorted);
        final KthSelector selector = new KthSelector();
        final double[] kp1 = new double[1];
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create();
        final int[] indices = IntStream.range(0, sorted.length).toArray();
        for (int n = 0; n < 3; n++) {
            ArraySampler.shuffle(rng, indices);
            final double[] x = values.clone();
            final int[] pivotsHeap = KthSelector.createPivotsHeap(sorted.length);
            for (int i = 0; i < sorted.length; i++) {
                final int k = indices[i];
                Assertions.assertEquals(sorted[k], selector.selectSPH(x, pivotsHeap, k, null), () -> "k[" + k + "]");
                if (k + 1 < sorted.length) {
                    Assertions.assertEquals(sorted[k], selector.selectSPH(x, pivotsHeap, k, kp1), () -> "k[" + k + "] with k+1");
                    Assertions.assertEquals(sorted[k + 1], kp1[0], () -> "k+1[" + (k + 1) + "]");
                }
            }
            Arrays.sort(x);
            Assertions.assertArrayEquals(sorted, x, "Data destroyed");
        }
    }

    static Stream<double[]> testSelect() {
        final Stream.Builder<double[]> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create();
        // Sizes above and below the threshold for partitioning
        for (final int size : new int[] {5, 50}) {
            final double[] values = IntStream.range(0, size).asDoubleStream().toArray();
            final double[] zeros = values.clone();
            final double[] nans = values.clone();
            Arrays.fill(zeros, 0, size >>> 2, -0.0);
            Arrays.fill(zeros, size >>> 2, size >>> 1, 0.0);
            Arrays.fill(nans, 0, 2, Double.NaN);
            for (int i = 0; i < 25; i++) {
                builder.add(ArraySampler.shuffle(rng, values.clone()));
                builder.add(ArraySampler.shuffle(rng, zeros.clone()));
            }
            for (int i = 0; i < 5; i++) {
                builder.add(ArraySampler.shuffle(rng, nans.clone()));
            }
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionSP(double[] values, int[] indices) {
        assertPartition(values, indices, new KthSelector()::partitionSP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionSPN(double[] values, int[] indices) {
        assertPartition(values, indices, new KthSelector()::partitionSPN);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionSBM(double[] values, int[] indices) {
        assertPartition(values, indices, new KthSelector()::partitionSBM);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionBM(double[] values, int[] indices) {
        assertPartition(values, indices, new KthSelector()::partitionBM);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionDP(double[] values, int[] indices) {
        assertPartition(values, indices, new KthSelector()::partitionDP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPartition"})
    void testPartitionDP5(double[] values, int[] indices) {
        assertPartition(values, indices, new KthSelector()::partitionDP5);
    }

    static void assertPartition(double[] values, int[] indices, BiConsumer<double[], int[]> function) {
        final double[] data = values.clone();
        final double[] sorted = values.clone();
        Arrays.sort(sorted);
        function.accept(data, indices);
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
                    () -> j + " < " + k);
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
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create(123);
        // Sizes above and below the threshold for partitioning
        for (final int size : new int[] {5, 50}) {
            final double[] values = IntStream.range(0, size).asDoubleStream().toArray();
            final double[] zeros = values.clone();
            Arrays.fill(zeros, 0, size >>> 2, -0.0);
            Arrays.fill(zeros, size >>> 2, size >>> 1, 0.0);
            for (final int k : new int[] {1, 2, 3, size}) {
                for (int i = 0; i < 25; i++) {
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
            // min; max; min/max
            builder.add(Arguments.of(values.clone(), new int[] {0}));
            builder.add(Arguments.of(values.clone(), new int[] {size - 1}));
            builder.add(Arguments.of(values.clone(), new int[] {0, size - 1}));
            builder.add(Arguments.of(zeros.clone(), new int[] {0}));
            builder.add(Arguments.of(zeros.clone(), new int[] {size - 1}));
            builder.add(Arguments.of(zeros.clone(), new int[] {0, size - 1}));
        }
        builder.add(Arguments.of(new double[] {}, new int[0]));
        builder.add(Arguments.of(new double[] {Double.NaN}, new int[] {0}));
        builder.add(Arguments.of(new double[] {Double.NaN, Double.NaN, Double.NaN}, new int[] {2}));
        builder.add(Arguments.of(new double[] {Double.NaN, 0.0, -0.0, Double.NaN}, new int[] {3}));
        builder.add(Arguments.of(new double[] {Double.NaN, 0.0, -0.0}, new int[] {0, 2}));
        builder.add(Arguments.of(new double[] {Double.NaN, 1.23, 0.0, -4.56, -0.0, Double.NaN}, new int[] {0, 1, 3}));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"testSort"})
    void testSortSP(double[] values) {
        assertSort(values, new KthSelector()::sortSP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testSort"})
    void testSortBM(double[] values) {
        assertSort(values, new KthSelector()::sortBM);
    }

    @ParameterizedTest
    @MethodSource(value = {"testSort"})
    void testSortSBM(double[] values) {
        assertSort(values, new KthSelector(PivotingStrategy.DYNAMIC, 3)::sortSBM);
    }

    @ParameterizedTest
    @MethodSource(value = {"testSort"})
    void testSortDP(double[] values) {
        assertSort(values, new KthSelector(PivotingStrategy.DYNAMIC, 3)::sortDP);
    }

    @ParameterizedTest
    @MethodSource(value = {"testSort"})
    void testSortDP5(double[] values) {
        // Requires at least 5 points
        assertSort(values, new KthSelector(PivotingStrategy.DYNAMIC, 5)::sortDP5);
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
            assertSort(v, x -> KthSelector.sortZero(x, 0, x.length));
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
        builder.add(new double[] {});
        builder.add(new double[] {Double.NaN});
        builder.add(new double[] {Double.NaN, Double.NaN, Double.NaN});
        builder.add(new double[] {Double.NaN, 0.0, -0.0, Double.NaN});
        builder.add(new double[] {Double.NaN, 0.0, -0.0});
        builder.add(new double[] {Double.NaN, 1.23, 0.0, -4.56, -0.0, Double.NaN});
        return builder.build();
    }
}
