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
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.PermutationSampler;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link Sorting}.
 *
 * <p>Sorting tests for floating point values do not include NaN or signed zero (-0.0).
 */
class SortingTest {

    /**
     * Interface to test sorting of indices.
§     */
    interface IndexSort {
        /**
         * Sort the indices into unique ascending order.
         *
         * @param a Indices.
         * @param n Number of indices.
         * @return number of unique indices.
         */
        int insertionSortIndices(int[] a, int n);
    }

    // double[]

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort"})
    void testDoubleInsertionSort(double[] values) {
        assertSort(values, x -> Sorting.sort(x, 0, x.length - 1));
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort", "testDoubleSort3"})
    void testDoubleSort3(double[] values) {
        final double[] data = Arrays.copyOf(values, 3);
        assertSort(data, x -> Sorting.sort3(x, 0, 1, 2));
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort", "testDoubleSort5"})
    void testDoubleSort5(double[] values) {
        final double[] data = Arrays.copyOf(values, 5);
        assertSort(data, x -> Sorting.sort5(x, 0, 1, 2, 3, 4));
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort3Internal"})
    void testDoubleSort3Internal(double[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        assertSortInternal(values, x -> Sorting.sort3(x, a, b, c), indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort5Internal"})
    void testDoubleSort5Internal(double[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        final int d = indices[3];
        final int e = indices[4];
        assertSortInternal(values, x -> Sorting.sort5(x, a, b, c, d, e), indices);
    }

    /**
     * Assert that the sort {@code function} computes the same result as
     * {@link Arrays#sort(double[])}.
     *
     * @param values Data.
     * @param function Sort function.
     */
    private static void assertSort(double[] values, Consumer<double[]> function) {
        final double[] expected = values.clone();
        Arrays.sort(expected);
        final double[] actual = values.clone();
        function.accept(actual);
        Assertions.assertArrayEquals(expected, actual, "Invalid sort");
    }

    /**
     * Assert that the sort {@code function} computes the same result as
     * {@link Arrays#sort(double[])} run on the provided {@code indices}.
     *
     * @param values Data.
     * @param function Sort function.
     * @param indices Indices.
     */
    private static void assertSortInternal(double[] values, Consumer<double[]> function, int... indices) {
        Assertions.assertFalse(containsDuplicates(indices), () -> "Duplicate indices: " + Arrays.toString(indices));
        // Pick out the data to sort
        final double[] expected = extractIndices(values, indices);
        Arrays.sort(expected);
        final double[] data = values.clone();
        function.accept(data);
        // Pick out the data that was sorted
        final double[] actual = extractIndices(data, indices);
        Assertions.assertArrayEquals(expected, actual, "Invalid sort");
        // Check outside the sorted indices
        OUTSIDE: for (int i = 0; i < values.length; i++) {
            for (final int ignore : indices) {
                if (i == ignore) {
                    continue OUTSIDE;
                }
            }
            Assertions.assertEquals(values[i], data[i]);
        }
    }

    static Stream<double[]> testDoubleSort() {
        final Stream.Builder<double[]> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create();
        for (final int size : new int[] {10, 15}) {
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
            builder.add(a);
            for (int i = 0; i < 5; i++) {
                builder.add(rng.doubles(size).toArray());
            }
        }
        return builder.build();
    }

    static Stream<double[]> testDoubleSort3() {
        // Permutations is 3! = 6
        final double x = 3.35;
        final double y = 12.3;
        final double z = -9.99;
        final double[][] a = {
            {x, y, z},
            {x, z, y},
            {z, x, y},
            {y, x, z},
            {y, z, x},
            {z, y, x},
        };
        return Arrays.stream(a);
    }

    static Stream<double[]> testDoubleSort5() {
        final Stream.Builder<double[]> builder = Stream.builder();
        final double[] a = new double[5];
        // Permutations is 5! = 120
        final int shift = 42;
        for (int i = 0; i < 5; i++) {
            a[0] = i + shift;
            for (int j = 0; j < 5; j++) {
                if (j == i) {
                    continue;
                }
                a[1] = j + shift;
                for (int k = 0; k < 5; k++) {
                    if (k == j || k == i) {
                        continue;
                    }
                    a[2] = k + shift;
                    for (int l = 0; l < 5; l++) {
                        if (l == k || l == j || l == i) {
                            continue;
                        }
                        a[3] = l + shift;
                        for (int m = 0; m < 5; m++) {
                            if (m == l || m == k || m == j || m == i) {
                                continue;
                            }
                            a[3] = m + shift;
                            builder.add(a.clone());
                        }
                    }
                }
            }
        }
        return builder.build();
    }

    static Stream<Arguments> testDoubleSort3Internal() {
        return testDoubleSortInternal(3);
    }

    static Stream<Arguments> testDoubleSort5Internal() {
        return testDoubleSortInternal(5);
    }

    static Stream<Arguments> testDoubleSortInternal(int k) {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create();
        for (final int size : new int[] {k, 2 * k, 4 * k}) {
            final PermutationSampler s = new PermutationSampler(rng, size, k);
            for (int i = k * k; i-- >= 0;) {
                final double[] a = rng.doubles(size).toArray();
                final int[] indices = s.sample();
                builder.add(Arguments.of(a, indices));
            }
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort4Internal"})
    void testDoubleLowerMedian4Internal(double[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        final int d = indices[3];
        assertMedian(values, x -> {
            Sorting.lowerMedian4(x, a, b, c, d);
            return b;
        }, true, false, indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort4Internal"})
    void testDoubleUpperMedian4Internal(double[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        final int d = indices[3];
        assertMedian(values, x -> {
            Sorting.upperMedian4(x, a, b, c, d);
            return c;
        }, false, false, indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort4"})
    void testDoubleLowerMedian4(double[] a) {
        // This method computes in place
        assertMedian(a, x -> {
            Sorting.lowerMedian4(x, 0, 1, 2, 3);
            return 1;
        }, true, true, 0, 1, 2, 3);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort4"})
    void testDoubleUpperMedian4(double[] a) {
        // This method computes in place
        assertMedian(a, x -> {
            Sorting.upperMedian4(x, 0, 1, 2, 3);
            return 2;
        }, false, true, 0, 1, 2, 3);
    }

    /**
     * Assert that the median {@code function} computes the same result as
     * {@link Arrays#sort(double[])} run on the provided {@code indices}.
     *
     * @param values Data.
     * @param function Sort function.
     * @param lower For even lengths use the lower median; else the upper median.
     * @param stable If true then no swaps should be made on the second pass.
     * @param indices Indices.
     */
    private static void assertMedian(double[] values, ToIntFunction<double[]> function,
        boolean lower, boolean stable, int... indices) {
        Assertions.assertFalse(containsDuplicates(indices), () -> "Duplicate indices: " + Arrays.toString(indices));
        // Pick out the data to sort
        final double[] expected = extractIndices(values, indices);
        Arrays.sort(expected);
        final double[] data = values.clone();
        final int m = function.applyAsInt(data);
        Assertions.assertEquals(expected[(lower ? -1 : 0) + (expected.length >>> 1)], data[m]);
        // Check outside the sorted indices
        OUTSIDE: for (int i = 0; i < values.length; i++) {
            for (final int ignore : indices) {
                if (i == ignore) {
                    continue OUTSIDE;
                }
            }
            Assertions.assertEquals(values[i], data[i]);
        }
        // This is not a strict requirement but check that no swaps occur on a second pass
        if (stable) {
            final double[] x = data.clone();
            final int m2 = function.applyAsInt(data);
            Assertions.assertEquals(m, m2);
            Assertions.assertArrayEquals(x, data);
        }
    }


    static Stream<double[]> testDoubleSort4() {
        final Stream.Builder<double[]> builder = Stream.builder();
        final double[] a = new double[4];
        // Permutations is 4! = 24
        final int shift = 42;
        for (int i = 0; i < 4; i++) {
            a[0] = i + shift;
            for (int j = 0; j < 4; j++) {
                if (j == i) {
                    continue;
                }
                a[1] = j + shift;
                for (int k = 0; k < 4; k++) {
                    if (k == j || k == i) {
                        continue;
                    }
                    a[2] = k + shift;
                    for (int l = 0; l < 4; l++) {
                        if (l == k || l == j || l == i) {
                            continue;
                        }
                        a[3] = l + shift;
                        builder.add(a.clone());
                    }
                }
            }
        }
        return builder.build();
    }

    static Stream<Arguments> testDoubleSort4Internal() {
        final int k = 4;
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create();
        for (final int size : new int[] {k, 2 * k, 4 * k}) {
            double[] a = rng.doubles(size).toArray();
            final PermutationSampler s = new PermutationSampler(rng, size, k);
            for (int i = k * k; i-- >= 0;) {
                a = rng.doubles(size).toArray();
                final int[] indices = s.sample();
                builder.add(Arguments.of(a, indices));
            }
        }
        return builder.build();
    }

    private static double[] extractIndices(double[] values, int[] indices) {
        final double[] data = new double[indices.length];
        for (int i = 0; i < indices.length; i++) {
            data[i] = values[indices[i]];
        }
        return data;
    }

    // int[]

    @ParameterizedTest
    @MethodSource(value = {"testIntSort"})
    void testIntInsertionSort(int[] values) {
        assertSort(values, x -> Sorting.sort(x, 0, x.length - 1));
    }

    @ParameterizedTest
    @MethodSource(value = {"testIntSort", "testIntSort3"})
    void testIntSort3(int[] values) {
        final int[] data = Arrays.copyOf(values, 3);
        assertSort(data, x -> Sorting.sort3(x, 0, 1, 2));
    }

    @ParameterizedTest
    @MethodSource(value = {"testIntSort", "testIntSort5"})
    void testIntSort5(int[] values) {
        final int[] data = Arrays.copyOf(values, 5);
        assertSort(data, x -> Sorting.sort5(x, 0, 1, 2, 3, 4));
    }

    @ParameterizedTest
    @MethodSource(value = {"testIntSort3Internal"})
    void testIntSort3Internal(int[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        assertSortInternal(values, x -> Sorting.sort3(x, a, b, c), indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testIntSort5Internal"})
    void testIntSort5Internal(int[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        final int d = indices[3];
        final int e = indices[4];
        assertSortInternal(values, x -> Sorting.sort5(x, a, b, c, d, e), indices);
    }

    /**
     * Assert that the sort {@code function} computes the same result as
     * {@link Arrays#sort(int[])}.
     *
     * @param values Data.
     * @param function Sort function.
     */
    private static void assertSort(int[] values, Consumer<int[]> function) {
        final int[] expected = values.clone();
        Arrays.sort(expected);
        final int[] actual = values.clone();
        function.accept(actual);
        Assertions.assertArrayEquals(expected, actual, "Invalid sort");
    }

    /**
     * Assert that the sort {@code function} computes the same result as
     * {@link Arrays#sort(int[])} run on the provided {@code indices}.
     *
     * @param values Data.
     * @param function Sort function.
     * @param indices Indices.
     */
    private static void assertSortInternal(int[] values, Consumer<int[]> function, int... indices) {
        Assertions.assertFalse(containsDuplicates(indices), () -> "Duplicate indices: " + Arrays.toString(indices));
        // Pick out the data to sort
        final int[] expected = extractIndices(values, indices);
        Arrays.sort(expected);
        final int[] data = values.clone();
        function.accept(data);
        // Pick out the data that was sorted
        final int[] actual = extractIndices(data, indices);
        Assertions.assertArrayEquals(expected, actual, "Invalid sort");
        // Check outside the sorted indices
        OUTSIDE: for (int i = 0; i < values.length; i++) {
            for (final int ignore : indices) {
                if (i == ignore) {
                    continue OUTSIDE;
                }
            }
            Assertions.assertEquals(values[i], data[i]);
        }
    }

    static Stream<int[]> testIntSort() {
        final Stream.Builder<int[]> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create();
        for (final int size : new int[] {10, 15}) {
            int[] a = new int[size];
            Arrays.fill(a, 123);
            builder.add(a.clone());
            for (int ii = 0; ii < size; ii++) {
                a[ii] = ii;
            }
            builder.add(a.clone());
            for (int ii = 0; ii < size; ii++) {
                a[ii] = size - ii;
            }
            builder.add(a);
            for (int i = 0; i < 5; i++) {
                builder.add(rng.ints(size).toArray());
            }
        }
        return builder.build();
    }

    static Stream<int[]> testIntSort3() {
        // Permutations is 3! = 6
        final int x = 335;
        final int y = 123;
        final int z = -999;
        final int[][] a = {
            {x, y, z},
            {x, z, y},
            {z, x, y},
            {y, x, z},
            {y, z, x},
            {z, y, x},
        };
        return Arrays.stream(a);
    }

    static Stream<int[]> testIntSort5() {
        final Stream.Builder<int[]> builder = Stream.builder();
        final int[] a = new int[5];
        // Permutations is 5! = 120
        final int shift = 42;
        for (int i = 0; i < 5; i++) {
            a[0] = i + shift;
            for (int j = 0; j < 5; j++) {
                if (j == i) {
                    continue;
                }
                a[1] = j + shift;
                for (int k = 0; k < 5; k++) {
                    if (k == j || k == i) {
                        continue;
                    }
                    a[2] = k + shift;
                    for (int l = 0; l < 5; l++) {
                        if (l == k || l == j || l == i) {
                            continue;
                        }
                        a[3] = l + shift;
                        for (int m = 0; m < 5; m++) {
                            if (m == l || m == k || m == j || m == i) {
                                continue;
                            }
                            a[3] = m + shift;
                            builder.add(a.clone());
                        }
                    }
                }
            }
        }
        return builder.build();
    }

    static Stream<Arguments> testIntSort3Internal() {
        return testIntSortInternal(3);
    }

    static Stream<Arguments> testIntSort5Internal() {
        return testIntSortInternal(5);
    }

    static Stream<Arguments> testIntSortInternal(int k) {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create();
        for (final int size : new int[] {k, 2 * k, 4 * k}) {
            final PermutationSampler s = new PermutationSampler(rng, size, k);
            for (int i = k * k; i-- >= 0;) {
                final int[] a = rng.ints(size).toArray();
                final int[] indices = s.sample();
                builder.add(Arguments.of(a, indices));
            }
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"testIntSort4Internal"})
    void testIntLowerMedian4Internal(int[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        final int d = indices[3];
        assertMedian(values, x -> {
            Sorting.lowerMedian4(x, a, b, c, d);
            return b;
        }, true, false, indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testIntSort4Internal"})
    void testIntUpperMedian4Internal(int[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        final int d = indices[3];
        assertMedian(values, x -> {
            Sorting.upperMedian4(x, a, b, c, d);
            return c;
        }, false, false, indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testIntSort4"})
    void testIntLowerMedian4(int[] a) {
        // This method computes in place
        assertMedian(a, x -> {
            Sorting.lowerMedian4(x, 0, 1, 2, 3);
            return 1;
        }, true, true, 0, 1, 2, 3);
    }

    @ParameterizedTest
    @MethodSource(value = {"testIntSort4"})
    void testIntUpperMedian4(int[] a) {
        // This method computes in place
        assertMedian(a, x -> {
            Sorting.upperMedian4(x, 0, 1, 2, 3);
            return 2;
        }, false, true, 0, 1, 2, 3);
    }

    /**
     * Assert that the median {@code function} computes the same result as
     * {@link Arrays#sort(int[])} run on the provided {@code indices}.
     *
     * @param values Data.
     * @param function Sort function.
     * @param lower For even lengths use the lower median; else the upper median.
     * @param stable If true then no swaps should be made on the second pass.
     * @param indices Indices.
     */
    private static void assertMedian(int[] values, ToIntFunction<int[]> function,
        boolean lower, boolean stable, int... indices) {
        Assertions.assertFalse(containsDuplicates(indices), () -> "Duplicate indices: " + Arrays.toString(indices));
        // Pick out the data to sort
        final int[] expected = extractIndices(values, indices);
        Arrays.sort(expected);
        final int[] data = values.clone();
        final int m = function.applyAsInt(data);
        Assertions.assertEquals(expected[(lower ? -1 : 0) + (expected.length >>> 1)], data[m]);
        // Check outside the sorted indices
        OUTSIDE: for (int i = 0; i < values.length; i++) {
            for (final int ignore : indices) {
                if (i == ignore) {
                    continue OUTSIDE;
                }
            }
            Assertions.assertEquals(values[i], data[i]);
        }
        // This is not a strict requirement but check that no swaps occur on a second pass
        if (stable) {
            final int[] x = data.clone();
            final int m2 = function.applyAsInt(data);
            Assertions.assertEquals(m, m2);
            Assertions.assertArrayEquals(x, data);
        }
    }


    static Stream<int[]> testIntSort4() {
        final Stream.Builder<int[]> builder = Stream.builder();
        final int[] a = new int[4];
        // Permutations is 4! = 24
        final int shift = 42;
        for (int i = 0; i < 4; i++) {
            a[0] = i + shift;
            for (int j = 0; j < 4; j++) {
                if (j == i) {
                    continue;
                }
                a[1] = j + shift;
                for (int k = 0; k < 4; k++) {
                    if (k == j || k == i) {
                        continue;
                    }
                    a[2] = k + shift;
                    for (int l = 0; l < 4; l++) {
                        if (l == k || l == j || l == i) {
                            continue;
                        }
                        a[3] = l + shift;
                        builder.add(a.clone());
                    }
                }
            }
        }
        return builder.build();
    }

    static Stream<Arguments> testIntSort4Internal() {
        final int k = 4;
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create();
        for (final int size : new int[] {k, 2 * k, 4 * k}) {
            int[] a = rng.ints(size).toArray();
            final PermutationSampler s = new PermutationSampler(rng, size, k);
            for (int i = k * k; i-- >= 0;) {
                a = rng.ints(size).toArray();
                final int[] indices = s.sample();
                builder.add(Arguments.of(a, indices));
            }
        }
        return builder.build();
    }

    private static int[] extractIndices(int[] values, int[] indices) {
        final int[] data = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            data[i] = values[indices[i]];
        }
        return data;
    }

    // Sorting unique indices

    @ParameterizedTest
    @MethodSource(value = {"testSortIndices"})
    void testInsertionSortIndices(int[] values, int n) {
        assertSortIndices(Sorting::insertionSortIndices, values, n, 1);
    }

    @ParameterizedTest
    @MethodSource(value = {"testSortIndices"})
    void testSortIndices(int[] values, int n) {
        assertSortIndices(Sorting::sortIndices, values, n, 1);
    }

    private static void assertSortIndices(IndexSort fun, int[] values, int n, int minSupportedLength) {
        // Negative n is a signal to use the full length
        n = n < 0 ? values.length : n;
        Assumptions.assumeTrue(n >= minSupportedLength);
        final int[] x = values.clone();
        final int[] expected = Arrays.stream(values).limit(n)
            .distinct().sorted().toArray();
        final int unique = fun.insertionSortIndices(x, n);
        Assertions.assertEquals(expected.length, unique, "Incorrect unique length");
        for (int i = 0; i < expected.length; i++) {
            final int index = i;
            Assertions.assertEquals(expected[i], x[i], () -> "Error @ " + index);
        }
        // Test values after unique should be in the entire original data
        final BitSet set = new BitSet();
        Arrays.stream(values).limit(n).forEach(set::set);
        for (int i = expected.length; i < n; i++) {
            Assertions.assertTrue(set.get(x[i]), "Data up to n destroyed");
        }
        // Data after n should be untouched
        for (int i = n; i < values.length; i++) {
            Assertions.assertEquals(values[i], x[i], "Data after n destroyed");
        }
    }

    static Stream<Arguments> testSortIndices() {
        // Create data that should exercise all strategies in the heuristics in
        // Sorting::sortIndices used to choose a sorting method
        final Stream.Builder<Arguments> builder = Stream.builder();
        // Use length -1 to use the array length
        builder.add(Arguments.of(new int[0], -1));
        builder.add(Arguments.of(new int[3], -1));
        builder.add(Arguments.of(new int[3], -1));
        builder.add(Arguments.of(new int[] {42}, -1));
        builder.add(Arguments.of(new int[] {1, 2, 3}, -1));
        builder.add(Arguments.of(new int[] {3, 2, 1}, -1));
        builder.add(Arguments.of(new int[] {42, 5, 7}, -1));
        // Duplicates
        builder.add(Arguments.of(new int[] {1, 1}, -1));
        builder.add(Arguments.of(new int[] {1, 1, 1}, -1));
        builder.add(Arguments.of(new int[] {42, 5, 2, 9, 2, 9, 7, 7, 4}, -1));
        // Truncated indices
        builder.add(Arguments.of(new int[] {3, 2, 1}, 1));
        builder.add(Arguments.of(new int[] {3, 2, 1}, 2));
        builder.add(Arguments.of(new int[] {2, 2, 1}, 2));
        builder.add(Arguments.of(new int[] {42, 5, 7, 7, 4}, 3));
        builder.add(Arguments.of(new int[] {5, 4, 3, 2, 1}, 3));
        builder.add(Arguments.of(new int[] {1, 2, 3, 4, 5}, 3));
        builder.add(Arguments.of(new int[] {5, 3, 1, 2, 4}, 3));
        // Some random indices with duplicates
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create();
        for (final int size : new int[] {5, 10, 30}) {
            final int maxIndex = size >>> 1;
            for (int i = 0; i < 5; i++) {
                builder.add(Arguments.of(rng.ints(size, 0, maxIndex).toArray(), -1));
            }
        }
        // A lot of duplicates
        builder.add(Arguments.of(rng.ints(50, 0, 3).toArray(), -1));
        builder.add(Arguments.of(rng.ints(50, 0, 5).toArray(), -1));
        builder.add(Arguments.of(rng.ints(50, 0, 10).toArray(), -1));
        // Bug where the first index was ignored when using an IndexSet
        builder.add(Arguments.of(IntStream.range(0, 50).map(x -> 50 - x).toArray(), -1));
        // Sparse
        builder.add(Arguments.of(rng.ints(25, 0, 100000).toArray(), -1));
        // Ascending
        builder.add(Arguments.of(IntStream.range(99, 134).toArray(), -1));
        builder.add(Arguments.of(IntStream.range(99, 134).map(x -> x * 2).toArray(), -1));
        builder.add(Arguments.of(IntStream.range(99, 134).map(x -> x * 3).toArray(), -1));
        return builder.build();
    }

    private static boolean containsDuplicates(int[] indices) {
        for (int i = 0; i < indices.length; i++) {
            for (int j = 0; j < i; j++) {
                if (indices[i] == indices[j]) {
                    return true;
                }
            }
        }
        return false;
    }
}
