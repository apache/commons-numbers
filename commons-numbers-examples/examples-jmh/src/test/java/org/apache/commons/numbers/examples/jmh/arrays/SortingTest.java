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
 */
class SortingTest {

    /**
     * Interface to test sorting unique indices.
     */
    interface IndexSort {
        /**
         * Sort the indices into unique ascending order.
         *
         * @param a Indices.
         * @param n Number of indices.
         * @return number of unique indices.
         */
        int sort(int[] a, int n);
    }

    // double[]

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort", "testDoubleInsertionSortInternal"})
    void testDoubleInsertionSortInternal(double[] values) {
        assertDoubleSort(values, x -> Sorting.sort(x, 0, x.length - 1, false));
        if (values.length < 2) {
            return;
        }
        // Check internal sort
        // Set pivot at lower end
        values[0] = Arrays.stream(values).min().getAsDouble();
        // check internal sort
        assertDoubleSort(values, x -> Sorting.sort(x, 1, x.length - 1, true));
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort", "testDoubleInsertionSortInternal"})
    void testDoublePairedInsertionSortInternal(double[] values) {
        if (values.length < 2) {
            return;
        }
        // Set pivot at lower end
        values[0] = Arrays.stream(values).min().getAsDouble();
        assertDoubleSort(values.clone(), x -> Sorting.sortPairedInternal1(x, 1, x.length - 1));
        assertDoubleSort(values.clone(), x -> Sorting.sortPairedInternal2(x, 1, x.length - 1));
        assertDoubleSort(values.clone(), x -> Sorting.sortPairedInternal3(x, 1, x.length - 1));
        assertDoubleSort(values.clone(), x -> Sorting.sortPairedInternal4(x, 1, x.length - 1));
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort"})
    void testDoubleInsertionSort(double[] values) {
        assertDoubleSort(values, x -> Sorting.sort(x, 0, x.length - 1));
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort"})
    void testDoubleInsertionSortB(double[] values) {
        assertDoubleSort(values, x -> Sorting.sortb(x, 0, x.length - 1));
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort", "testDoubleSort3"})
    void testDoubleSort3(double[] values) {
        final double[] data = Arrays.copyOf(values, 3);
        assertDoubleSort(data, x -> Sorting.sort3(x, 0, 1, 2));
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort", "testDoubleSort3"})
    void testDoubleSort3b(double[] values) {
        final double[] data = Arrays.copyOf(values, 3);
        assertDoubleSort(data, x -> Sorting.sort3b(x, 0, 1, 2));
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort", "testDoubleSort3"})
    void testDoubleSort3c(double[] values) {
        final double[] data = Arrays.copyOf(values, 3);
        assertDoubleSort(data, x -> Sorting.sort3c(x, 0, 1, 2));
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort", "testDoubleSort4"})
    void testDoubleSort4(double[] values) {
        final double[] data = Arrays.copyOf(values, 4);
        assertDoubleSort(data, x -> Sorting.sort4(x, 0, 1, 2, 3));
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort", "testDoubleSort5"})
    void testDoubleSort5(double[] values) {
        final double[] data = Arrays.copyOf(values, 5);
        assertDoubleSort(data, x -> Sorting.sort5(x, 0, 1, 2, 3, 4));
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort", "testDoubleSort5"})
    void testDoubleSort5b(double[] values) {
        final double[] data = Arrays.copyOf(values, 5);
        assertDoubleSort(data, x -> Sorting.sort5b(x, 0, 1, 2, 3, 4));
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort", "testDoubleSort5"})
    void testDoubleSort5c(double[] values) {
        final double[] data = Arrays.copyOf(values, 5);
        assertDoubleSort(data, x -> Sorting.sort5c(x, 0, 1, 2, 3, 4));
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort"})
    void testDoubleSort7(double[] values) {
        final double[] data = Arrays.copyOf(values, 7);
        assertDoubleSort(data, x -> Sorting.sort7(x, 0, 1, 2, 3, 4, 5, 6));
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort"})
    void testDoubleSort8(double[] values) {
        final double[] data = Arrays.copyOf(values, 8);
        assertDoubleSort(data, x -> Sorting.sort8(x, 0, 1, 2, 3, 4, 5, 6, 7));
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort"})
    void testDoubleSort11(double[] values) {
        final double[] data = Arrays.copyOf(values, 11);
        assertDoubleSort(data, x -> Sorting.sort11(x, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort3Internal"})
    void testDoubleSort3Internal(double[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        assertDoubleSortInternal(values, x -> Sorting.sort3(x, a, b, c), indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort3Internal"})
    void testDoubleSort3bInternal(double[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        assertDoubleSortInternal(values, x -> Sorting.sort3b(x, a, b, c), indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort3Internal"})
    void testDoubleSort3cInternal(double[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        assertDoubleSortInternal(values, x -> Sorting.sort3c(x, a, b, c), indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort4Internal"})
    void testDoubleSort4Internal(double[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        final int d = indices[3];
        assertDoubleSortInternal(values, x -> Sorting.sort4(x, a, b, c, d), indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort5Internal"})
    void testDoubleSort5Internal(double[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        final int d = indices[3];
        final int e = indices[4];
        assertDoubleSortInternal(values, x -> Sorting.sort5(x, a, b, c, d, e), indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort5Internal"})
    void testDoubleSort5bInternal(double[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        final int d = indices[3];
        final int e = indices[4];
        assertDoubleSortInternal(values, x -> Sorting.sort5b(x, a, b, c, d, e), indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort5Internal"})
    void testDoubleSort5cInternal(double[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        final int d = indices[3];
        final int e = indices[4];
        assertDoubleSortInternal(values, x -> Sorting.sort5c(x, a, b, c, d, e), indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort7Internal"})
    void testDoubleSort7Internal(double[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        final int d = indices[3];
        final int e = indices[4];
        final int f = indices[5];
        final int g = indices[6];
        assertDoubleSortInternal(values, x -> Sorting.sort7(x, a, b, c, d, e, f, g), indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort8Internal"})
    void testDoubleSort8Internal(double[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        final int d = indices[3];
        final int e = indices[4];
        final int f = indices[5];
        final int g = indices[6];
        final int h = indices[7];
        assertDoubleSortInternal(values, x -> Sorting.sort8(x, a, b, c, d, e, f, g, h), indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort11Internal"})
    void testDoubleSort11Internal(double[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        final int d = indices[3];
        final int e = indices[4];
        final int f = indices[5];
        final int g = indices[6];
        final int h = indices[7];
        final int i = indices[8];
        final int j = indices[9];
        final int k = indices[10];
        assertDoubleSortInternal(values, x -> Sorting.sort11(x, a, b, c, d, e, f, g, h, i, j, k), indices);
    }

    /**
     * Assert that the sort {@code function} computes the same result as
     * {@link Arrays#sort(double[])}. Ignores signed zeros.
     *
     * @param values Data.
     * @param function Sort function.
     */
    private static void assertDoubleSort(double[] values, Consumer<double[]> function) {
        final double[] expected = values.clone();
        Arrays.sort(expected);
        final double[] actual = values.clone();
        function.accept(actual);
        assertDoubleSort(expected, actual);
    }

    /**
     * Assert that the {@code expected} and {@code actual} sort are the same. Ignores
     * signed zeros.
     *
     * @param expected Expected sort.
     * @param actual Actual sort.
     */
    private static void assertDoubleSort(double[] expected, double[] actual) {
        // Detect signed zeros
        int c = 0;
        for (int i = 0; i < expected.length; i++) {
            if (Double.compare(-0.0, expected[i]) == 0) {
                c++;
            }
        }
        // Check
        if (c != 0) {
            // Replace signed zeros
            final double[] e = replaceSignedZeros(expected.clone());
            final double[] a = replaceSignedZeros(actual.clone());
            Assertions.assertArrayEquals(e, a, "Sort with +0.0");
            // Sort the signed zeros correctly
            Arrays.sort(actual);
            // Check the same number of signed zeros are present
            Assertions.assertArrayEquals(expected, actual, "Signed zeros destroyed");
        } else {
            Assertions.assertArrayEquals(expected, actual, "Invalid sort");
        }
    }

    /**
     * Assert that the sort {@code function} computes the same result as
     * {@link Arrays#sort(double[])} run on the provided {@code indices}. Ignores signed
     * zeros.
     *
     * @param values Data.
     * @param function Sort function.
     * @param indices Indices.
     */
    private static void assertDoubleSortInternal(double[] values, Consumer<double[]> function, int... indices) {
        Assertions.assertFalse(containsDuplicates(indices), () -> "Duplicate indices: " + Arrays.toString(indices));
        // Pick out the data to sort
        final double[] expected = extractIndices(values, indices);
        Arrays.sort(expected);
        final double[] data = values.clone();
        function.accept(data);
        // Pick out the data that was sorted
        final double[] actual = extractIndices(data, indices);
        assertDoubleSort(expected, actual);
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
            builder.add(a.clone());
            for (int i = 0; i < 5; i++) {
                a = rng.doubles(size).toArray();
                builder.add(a.clone());
                final int j = rng.nextInt(size);
                // Pick a different index
                final int k = (j + rng.nextInt(size - 1)) % size;
                a[j] = -0.0;
                a[k] = 0.0;
                builder.add(a.clone());
                for (int z = 0; z < size; z++) {
                    a[z] = rng.nextBoolean() ? -0.0 : 0.0;
                }
            }
        }
        return builder.build();
    }

    static Stream<double[]> testDoubleInsertionSortInternal() {
        final Stream.Builder<double[]> builder = Stream.builder();
        builder.add(new double[] {});
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create();
        // Use small sizes to test the pair
        for (final int size : new int[] {1, 2, 3, 4, 5}) {
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
            if (size == 1) {
                continue;
            }
            for (int i = 0; i < 5; i++) {
                a = rng.doubles(size).toArray();
                builder.add(a.clone());
                final int j = rng.nextInt(size);
                // Pick a different index
                final int k = (j + rng.nextInt(size - 1)) % size;
                a[j] = -0.0;
                a[k] = 0.0;
                builder.add(a.clone());
                for (int z = 0; z < size; z++) {
                    a[z] = rng.nextBoolean() ? -0.0 : 0.0;
                }
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

    static Stream<Arguments> testDoubleSort4Internal() {
        return testDoubleSortInternal(4);
    }

    static Stream<Arguments> testDoubleSort5Internal() {
        return testDoubleSortInternal(5);
    }

    static Stream<Arguments> testDoubleSort7Internal() {
        return testDoubleSortInternal(7);
    }

    static Stream<Arguments> testDoubleSort8Internal() {
        return testDoubleSortInternal(8);
    }

    static Stream<Arguments> testDoubleSort11Internal() {
        return testDoubleSortInternal(11);
    }

    static Stream<Arguments> testDoubleSortInternal(int k) {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create();
        for (final int size : new int[] {k, 2 * k, 4 * k}) {
            double[] a = rng.doubles(size).toArray();
            final PermutationSampler s = new PermutationSampler(rng, size, k);
            for (int i = k * k; i-- >= 0;) {
                a = rng.doubles(size).toArray();
                final int[] indices = s.sample();
                builder.add(Arguments.of(a.clone(), indices));
                a[indices[0]] = -0.0;
                a[indices[1]] = 0.0;
                builder.add(Arguments.of(a.clone(), indices));
                for (final int z : indices) {
                    a[z] = rng.nextBoolean() ? -0.0 : 0.0;
                }
                builder.add(Arguments.of(a.clone(), indices));
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

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort4Internal"})
    void testLowerMedian4Internal(double[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        final int d = indices[3];
        assertDoubleMedian(values, x -> {
            Sorting.lowerMedian4(x, a, b, c, d);
            return b;
        }, true, false, indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort4Internal"})
    void testLowerMedian4bInternal(double[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        final int d = indices[3];
        assertDoubleMedian(values, x -> {
            Sorting.lowerMedian4b(x, a, b, c, d);
            return b;
        }, true, true, indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort4Internal"})
    void testLowerMedian4cInternal(double[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        final int d = indices[3];
        assertDoubleMedian(values, x -> {
            Sorting.lowerMedian4c(x, a, b, c, d);
            return b;
        }, true, true, indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort4Internal"})
    void testLowerMedian4dInternal(double[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        final int d = indices[3];
        assertDoubleMedian(values, x -> {
            Sorting.lowerMedian4d(x, a, b, c, d);
            return b;
        }, true, true, indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort4Internal"})
    void testLowerMedian4eInternal(double[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        final int d = indices[3];
        assertDoubleMedian(values, x -> {
            Sorting.lowerMedian4e(x, a, b, c, d);
            return b;
        }, true, true, indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort4Internal"})
    void testUpperMedian4Internal(double[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        final int d = indices[3];
        assertDoubleMedian(values, x -> {
            Sorting.upperMedian4(x, a, b, c, d);
            return c;
        }, false, true, indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort4Internal"})
    void testUpperMedian4cInternal(double[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        final int d = indices[3];
        assertDoubleMedian(values, x -> {
            Sorting.upperMedian4c(x, a, b, c, d);
            return c;
        }, false, true, indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort4Internal"})
    void testUpperMedian4dInternal(double[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        final int d = indices[3];
        assertDoubleMedian(values, x -> {
            Sorting.upperMedian4d(x, a, b, c, d);
            return c;
        }, false, true, indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort4"})
    void testLowerMedian4(double[] a) {
        // This method computes in place
        assertDoubleMedian(a, x -> {
            Sorting.lowerMedian4(x, 0, 1, 2, 3);
            return 1;
        }, true, true, 0, 1, 2, 3);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort4"})
    void testLowerMedian4b(double[] a) {
        // This method computes in place
        assertDoubleMedian(a, x -> {
            Sorting.lowerMedian4b(x, 0, 1, 2, 3);
            return 1;
        }, true, true, 0, 1, 2, 3);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort4"})
    void testLowerMedian4c(double[] a) {
        // This method computes in place
        assertDoubleMedian(a, x -> {
            Sorting.lowerMedian4c(x, 0, 1, 2, 3);
            return 1;
        }, true, true, 0, 1, 2, 3);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort4"})
    void testLowerMedian4d(double[] a) {
        // This method computes in place
        assertDoubleMedian(a, x -> {
            Sorting.lowerMedian4d(x, 0, 1, 2, 3);
            return 1;
        }, true, true, 0, 1, 2, 3);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort4"})
    void testLowerMedian4e(double[] a) {
        // This method computes in place
        assertDoubleMedian(a, x -> {
            Sorting.lowerMedian4e(x, 0, 1, 2, 3);
            return 1;
        }, true, true, 0, 1, 2, 3);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort4"})
    void testUpperMedian4(double[] a) {
        // This method computes in place
        assertDoubleMedian(a, x -> {
            Sorting.upperMedian4(x, 0, 1, 2, 3);
            return 2;
        }, false, true, 0, 1, 2, 3);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort4"})
    void testUpperMedian4c(double[] a) {
        // This method computes in place
        assertDoubleMedian(a, x -> {
            Sorting.upperMedian4c(x, 0, 1, 2, 3);
            return 2;
        }, false, true, 0, 1, 2, 3);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort4"})
    void testUpperMedian4d(double[] a) {
        // This method computes in place
        assertDoubleMedian(a, x -> {
            Sorting.upperMedian4d(x, 0, 1, 2, 3);
            return 2;
        }, false, true, 0, 1, 2, 3);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort5Internal"})
    void testMedian5Internal(double[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        final int d = indices[3];
        final int e = indices[4];
        assertDoubleMedian5(values, x -> Sorting.median5(x, a, b, c, d, e), false, indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort5"})
    void testMedian5(double[] a) {
        assertDoubleMedian5(a, x -> Sorting.median5(x, 0), false, 0, 1, 2, 3, 4);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort5"})
    void testMedian5b(double[] a) {
        assertDoubleMedian5(a, x -> Sorting.median5b(x, 0), true, 0, 1, 2, 3, 4);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort5"})
    void testMedian5c(double[] a) {
        assertDoubleMedian5(a, x -> Sorting.median5c(x, 0), true, 0, 1, 2, 3, 4);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort5"})
    void testMedian5d(double[] a) {
        // This method computes in place
        assertDoubleMedian5(a, x -> {
            Sorting.median5d(x, 0, 1, 2, 3, 4);
            return 2;
        }, true, 0, 1, 2, 3, 4);
    }

    /**
     * Assert that the median {@code function} computes the same result as
     * {@link Arrays#sort(double[])} run on the provided {@code indices}. Ignores signed
     * zeros.
     *
     * @param values Data.
     * @param function Sort function.
     * @param lower For even lengths use the lower median; else the upper median.
     * @param stable If true then no swaps should be made on the second pass.
     * @param indices Indices.
     */
    private static void assertDoubleMedian(double[] values, ToIntFunction<double[]> function,
        boolean lower, boolean stable, int... indices) {
        Assertions.assertFalse(containsDuplicates(indices), () -> "Duplicate indices: " + Arrays.toString(indices));
        // Pick out the data to sort
        final double[] expected = extractIndices(values, indices);
        Arrays.sort(expected);
        final double[] data = values.clone();
        final int m = function.applyAsInt(data);
        // Only the magnitude matters so use a delta of 0 to allow -0.0 == 0.0
        Assertions.assertEquals(expected[(lower ? -1 : 0) + (expected.length >>> 1)], data[m], 0.0);
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

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort3"})
    void testMin3(double[] a) {
        assertDoubleMinMax(a, x -> Sorting.min3(x, 0, 1, 2), true, 0, 1, 2);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort3"})
    void testMax3(double[] a) {
        assertDoubleMinMax(a, x -> Sorting.max3(x, 0, 1, 2), false, 0, 1, 2);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort3Internal"})
    void testMin3Internal(double[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        assertDoubleMinMax(values, x -> Sorting.min3(x, a, b, c), true, indices);
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleSort3Internal"})
    void testMax3Internal(double[] values, int[] indices) {
        final int a = indices[0];
        final int b = indices[1];
        final int c = indices[2];
        assertDoubleMinMax(values, x -> Sorting.max3(x, a, b, c), false, indices);
    }

    /**
     * Assert that the median {@code function} computes the same result as
     * {@link Arrays#sort(double[])} run on the provided {@code indices}. Ignores signed
     * zeros.
     *
     * @param values Data.
     * @param function Min/Max function.
     * @param min Compute min; else max.
     * @param indices Indices.
     */
    private static void assertDoubleMinMax(double[] values, Consumer<double[]> function,
        boolean min, int... indices) {
        Assertions.assertFalse(containsDuplicates(indices), () -> "Duplicate indices: " + Arrays.toString(indices));
        // Pick out the data to sort
        final double[] expected = extractIndices(values, indices);
        Arrays.sort(expected);
        final double[] data = values.clone();
        function.accept(data);
        final int m = min ? indices[0] : indices[indices.length - 1];
        // Only the magnitude matters so use a delta of 0 to allow -0.0 == 0.0
        Assertions.assertEquals(expected[min ? 0 : indices.length - 1], data[m], 0.0);
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

    /**
     * Assert that the median {@code function} computes the same result as
     * {@link Arrays#sort(double[])} run on the provided {@code indices}. Ignores signed
     * zeros.
     *
     * @param values Data.
     * @param function Sort function.
     * @param stable If true then no swaps should be made on the second pass.
     * @param indices Indices.
     */
    private static void assertDoubleMedian5(double[] values, ToIntFunction<double[]> function,
        boolean stable, int... indices) {
        assertDoubleMedian(values, function, false, stable, indices);
    }

    // Sorting unique indices

    @ParameterizedTest
    @MethodSource(value = {"testSortUnique"})
    void testSortUniqueArray(int[] values, int n) {
        assertSortUnique(values.length, values, n < 0 ? values.length : n);
    }

    @ParameterizedTest
    @MethodSource(value = {"testSortUnique"})
    void testSortUniqueIndexSet(int[] values, int n) {
        assertSortUnique(0, values, n < 0 ? values.length : n);
    }

    private static void assertSortUnique(int threshold, int[] values, int n) {
        final int[] x = values.clone();
        final int[] expected = Arrays.stream(values).limit(n)
            .distinct().sorted().toArray();
        final IndexSet set = Sorting.sortUnique(threshold, x, n);
        for (int i = 0; i < expected.length; i++) {
            Assertions.assertEquals(expected[i], x[i]);
        }
        if (n > 0) {
            final int end = expected.length - 1;
            final int max = x[n - 1];
            if (expected.length < n) {
                Assertions.assertEquals(expected[end], ~max, "twos-complement max value");
            } else {
                Assertions.assertEquals(expected[end], max, "max value");
            }
        }
        for (int i = expected.length; i < n; i++) {
            Assertions.assertTrue(x[i] < 0, "Duplicate not set to negative");
        }

        if (x.length <= threshold) {
            Assertions.assertNull(set);
        } else if (n > 1) {
            // Check the IndexSet contains all the indices
            final int[] a = new int[expected.length];
            final int[] c = {0};
            set.forEach(i -> a[c[0]++] = i);
            Assertions.assertArrayEquals(expected, a);
        }
    }

    static Stream<Arguments> testSortUnique() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // Use length -1 to use the array length
        builder.add(Arguments.of(new int[0], -1));
        builder.add(Arguments.of(new int[3], -1));
        builder.add(Arguments.of(new int[3], -1));
        builder.add(Arguments.of(new int[] {1, 2, 3}, -1));
        builder.add(Arguments.of(new int[] {1, 1, 1}, -1));
        builder.add(Arguments.of(new int[] {42}, -1));
        builder.add(Arguments.of(new int[] {42, 5, 7}, -1));
        builder.add(Arguments.of(new int[] {42, 5, 7, 7, 4}, -1));
        // Truncated indices
        builder.add(Arguments.of(new int[] {42, 5, 7, 7, 4}, 3));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"testSortIndices"})
    void testSortIndicesInsertionSort(int[] values, int n) {
        assertSortIndices(Sorting::sortIndicesInsertionSort, values, n, 1);
    }

    @ParameterizedTest
    @MethodSource(value = {"testSortIndices"})
    void testSortIndicesBinarySearch(int[] values, int n) {
        assertSortIndices(Sorting::sortIndicesBinarySearch, values, n, 2);
    }

    @ParameterizedTest
    @MethodSource(value = {"testSortIndices"})
    void testSortIndicesHeapSort(int[] values, int n) {
        assertSortIndices(Sorting::sortIndicesHeapSort, values, n, 3);
    }

    @ParameterizedTest
    @MethodSource(value = {"testSortIndices"})
    void testSortIndicesSort(int[] values, int n) {
        assertSortIndices(Sorting::sortIndicesSort, values, n, 1);
    }

    @ParameterizedTest
    @MethodSource(value = {"testSortIndices"})
    void testSortIndicesIndexSet(int[] values, int n) {
        assertSortIndices(Sorting::sortIndicesIndexSet, values, n, 1);
    }

    @ParameterizedTest
    @MethodSource(value = {"testSortIndices"})
    void testSortIndicesHashIndexSet(int[] values, int n) {
        assertSortIndices(Sorting::sortIndicesHashIndexSet, values, n, 1);
    }

    @ParameterizedTest
    @MethodSource(value = {"testSortIndices"})
    void testSortIndices(int[] values, int n) {
        assertSortIndices(Sorting::sortIndices, values, n, 0);
    }

    private static void assertSortIndices(IndexSort fun, int[] values, int n, int minSupportedLength) {
        // Negative n is a signal to use the full length
        n = n < 0 ? values.length : n;
        Assumptions.assumeTrue(n >= minSupportedLength);
        final int[] x = values.clone();
        final int[] expected = Arrays.stream(values).limit(n)
            .distinct().sorted().toArray();
        final int unique = fun.sort(x, n);
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

    // Helper methods

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

    private static double[] replaceSignedZeros(double[] values) {
        for (int i = 0; i < values.length; i++) {
            if (Double.compare(-0.0, values[i]) == 0) {
                values[i] = 0;
            }
        }
        return values;
    }
}
