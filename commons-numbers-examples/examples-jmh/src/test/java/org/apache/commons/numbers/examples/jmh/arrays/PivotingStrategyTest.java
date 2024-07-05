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
 * Test for {@link PivotingStrategy}.
 */
class PivotingStrategyTest {
    @ParameterizedTest
    @MethodSource(value = {"testPivot"})
    void testCentral(double[] a) {
        assertPivot(a, 0, PivotingStrategy.CENTRAL);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPivot"})
    void testMedianOf3(double[] a) {
        assertPivot(a, 0, PivotingStrategy.MEDIAN_OF_3);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPivot"})
    void testMedianOf9(double[] a) {
        // Sometimes this is off by an index of 1
        assertPivot(a, 0, PivotingStrategy.MEDIAN_OF_9, -1, 1);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPivot", "testMedianOf5"})
    void testMedianOf5(double[] a) {
        assertPivot(a, 0, PivotingStrategy.MEDIAN_OF_5);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPivot", "testMedianOf5"})
    void testMedianOf5B(double[] a) {
        assertPivot(a, 0, PivotingStrategy.MEDIAN_OF_5B);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPivot"})
    void testDynamic(double[] a) {
        final int index = PivotingStrategy.DYNAMIC.pivotIndex(a, 0, a.length - 1, 0);
        PivotingStrategy s;
        if (PivotingStrategy.DYNAMIC.getSampledIndices(0, a.length - 1, 0).length == 3) {
            s = PivotingStrategy.MEDIAN_OF_3;
        } else {
            s = PivotingStrategy.MEDIAN_OF_9;
        }
        Assertions.assertEquals(s.pivotIndex(a, 0, a.length - 1, 0), index);
    }

    @ParameterizedTest
    @MethodSource(value = {"testPivot"})
    void testTarget(double[] a) {
        assertPivot(a, 0, PivotingStrategy.TARGET);
        assertPivot(a, 13, PivotingStrategy.TARGET);
    }

    private static void assertPivot(double[] a, int target, PivotingStrategy s, int... offset) {
        final double[] copy = a.clone();
        final int[] k = s.getSampledIndices(0, a.length - 1, target);
        // Extract data
        final double[] x = new double[k.length];
        for (int i = 0; i < k.length; i++) {
            x[i] = a[k[i]];
        }
        final int p1 = s.pivotIndex(a, 0, a.length - 1, target);
        // Extract data after
        final double[] y = new double[k.length];
        for (int i = 0; i < k.length; i++) {
            y[i] = a[k[i]];
        }
        // Test the effect on the data
        final int effect = s.samplingEffect();
        if (effect == PivotingStrategy.SORT) {
            Arrays.sort(x);
            Assertions.assertArrayEquals(x, y, "Data at indices not sorted");
        } else if (effect == PivotingStrategy.UNCHANGED) {
            Assertions.assertArrayEquals(x, y, "Data at indices changed");
            // Sort the data to obtain the expected pivot
            Arrays.sort(x);
        } else if (effect == PivotingStrategy.PARTIAL_SORT) {
            Arrays.sort(x);
            Arrays.sort(y);
            Assertions.assertArrayEquals(x, y, "Data destroyed");
        }
        // Pivot should be the centre of the sorted sample
        final int m = k.length >>> 1;
        // Allowed to be offset
        if (offset.length != 0) {
            boolean ok = x[m] == a[p1];
            for (final int o : offset) {
                if (ok) {
                    break;
                }
                ok = x[m + o] == a[p1];
            }
            Assertions.assertTrue(ok, () -> "Unexpected pivot: " + p1);
        } else {
            Assertions.assertEquals(x[m], a[p1], () -> "Unexpected pivot: " + p1);
        }
        // Flip data, pivot value should be the same
        for (int i = 0, j = k.length - 1; i < j; i++, j--) {
            final double v = copy[k[i]];
            copy[k[i]] = copy[k[j]];
            copy[k[j]] = v;
        }
        final int p1a = s.pivotIndex(copy, 0, a.length - 1, target);
        Assertions.assertEquals(a[p1], copy[p1a], "Pivot changed");
    }

    @Test
    void testMedianOf5Indexing() {
        assertIndexing(PivotingStrategy.MEDIAN_OF_5, 5, 0);
    }

    @Test
    void testMedianOf5BIndexing() {
        assertIndexing(PivotingStrategy.MEDIAN_OF_5B, 5, 0);
    }

    private static void assertIndexing(PivotingStrategy s, int safeLength, int target) {
        final double[] a = new double[safeLength - 1];
        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class,
            () -> s.pivotIndex(a, 0, a.length - 1, target),
            () -> "Length: " + (safeLength - 1));
        for (int i = safeLength; i < 50; i++) {
            final int n = i;
            final double[] b = new double[i];
            Assertions.assertDoesNotThrow(() -> s.pivotIndex(b, 0, b.length - 1, target), () -> "Length: " + n);
        }
    }

    static Stream<double[]> testPivot() {
        final Stream.Builder<double[]> builder = Stream.builder();
        final UniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create(123);
        // Big enough to use median of 9
        final double[] a = rng.doubles(50).toArray();
        for (int i = 0; i < 10; i++) {
            ArraySampler.shuffle(rng, a);
            builder.add(a.clone());
        }
        return builder.build();
    }

    static Stream<Arguments> testMedianOf5() {
        final Stream.Builder<Arguments> builder = Stream.builder();
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
                            builder.add(Arguments.of(a.clone(), 2));
                        }
                    }
                }
            }
        }
        return builder.build();
    }
}
