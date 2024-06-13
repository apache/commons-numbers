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
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Executes a benchmark of sorting array indices to a unique ascending sequence.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx4096M"})
public class IndexSortingPerformance {
    /** Sort using a modified insertion sort that ignores duplicates. */
    private static final String INSERTION = "Insertion";
    /** Sort using a binary search into the unique indices. */
    private static final String BINARY_SEARCH = "BinarySearch";
    /** Sort using a modified heap sort that ignores duplicates. */
    private static final String HEAP = "Heap";
    /** Sort using a full sort and a second pass to ignore duplicates. */
    private static final String SORT_UNIQUE = "SortUnique";
    /** Sort using an {@link IndexSet} to ignore duplicates;
     * sorted array extracted from the {@link IndexSet} storage. */
    private static final String INDEX_SET = "IndexSet";
    /** Sort using an {@link HashIndexSet} to ignore duplicates and full sort the unique values. */
    private static final String HASH_INDEX_SET = "HashIndexSet";
    /** Sort using a hybrid method using heuristics to choose the sort. */
    private static final String HYBRID = "Hybrid";

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

    /**
     * Source of {@code int} index array data.
     */
    @State(Scope.Benchmark)
    public static class IndexDataSource {
        /** Number of indices. */
        @Param({
            "10",
            "100",
            //"1000"
            })
        private int n;
        /** Range factor (spread of indices). */
        @Param({
            //"1",
            "10",
            //"100"
            })
        private double range;
        /** Duplication factor. */
        @Param({
            //"0",
            "1",
            "2"
            })
        private double duplication;
        /** Number of samples. */
        @Param({"100"})
        private int samples;
        /** True if the indices should be sorted into ascending order.
         * This would be the case if multiple quantiles are requested
         * using an ascending sequence of p in [0, 1]. */
        @Param({"false"})
        private boolean ascending;


        /** Data. */
        private int[][] data;

        /**
         * @return the data
         */
        public int[][] getData() {
            return data;
        }

        /**
         * Create the data.
         */
        @Setup(Level.Iteration)
        public void setup() {
            // Data will be randomized per iteration
            final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();

            // length of data: index in [0, length)
            final int length = (int) Math.floor(n * range);
            // extra duplicates
            final int extra = (int) Math.floor(n * duplication);

            data = new int[samples][];
            for (int i = 0; i < samples; i++) {
                final int[] indices = new int[n + extra];
                // Randomly spread indices in the range (this may create duplicates anyway)
                for (int j = 0; j < n; j++) {
                    indices[j] = rng.nextInt(length);
                }
                // Sample from the indices to create duplicates.
                for (int j = 0; j < extra; j++) {
                    indices[j + n] = indices[rng.nextInt(n)];
                }
                // Ensure the full range is present. Otherwise it is hard to fairly assess
                // the performance of the IndexSet when the data is so sparse that
                // the min/max is far from the edge of the range and it can use less memory.
                // Pick a random place to put the min.
                final int i1 = rng.nextInt(indices.length);
                // Put the max somewhere else.
                final int i2 = (i1 + rng.nextInt(indices.length - 1)) % indices.length;
                indices[i1] = 0;
                indices[i2] = length - 1;
                if (ascending) {
                    Arrays.sort(indices);
                }
                data[i] = indices;
            }
        }
    }

    /**
     * Source of a {@link IndexSort}.
     */
    @State(Scope.Benchmark)
    public static class IndexSortSource {
        /** Name of the source. */
        @Param({
            // Fast when size is small (<10)
            INSERTION,
            // Slow (too many System.arraycopy calls)
            //BINARY_SEARCH,
            // Slow ~ n log(n)
            //HEAP,
            // Fast sort but does not scale well with duplicates
            //SORT_UNIQUE,
            // Scale well with duplicates.
            // IndexSet has poor high memory requirements when keys are spread out.
            // HashIndexSet has predictable memory usage.
            INDEX_SET, HASH_INDEX_SET,
            // Should pick the best option most of the time
            HYBRID})
        private String name;

        /** The sort function. */
        private IndexSort function;

        /**
         * @return the function
         */
        public IndexSort getFunction() {
            return function;
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            // Note: Functions defensively copy the data by default
            // Note: KeyStratgey does not matter for single / paired keys but
            // we set it anyway for completeness.
            Objects.requireNonNull(name);
            if (INSERTION.equals(name)) {
                function = Sorting::sortIndicesInsertionSort;
            } else if (BINARY_SEARCH.equals(name)) {
                function = Sorting::sortIndicesBinarySearch;
            } else if (HEAP.equals(name)) {
                function = Sorting::sortIndicesHeapSort;
            } else if (SORT_UNIQUE.equals(name)) {
                function = Sorting::sortIndicesSort;
            } else if (INDEX_SET.equals(name)) {
                function = Sorting::sortIndicesIndexSet;
            } else if ((INDEX_SET + "2").equals(name)) {
                function = Sorting::sortIndicesIndexSet2;
            } else if (HASH_INDEX_SET.equals(name)) {
                function = Sorting::sortIndicesHashIndexSet;
            } else if (HYBRID.equals(name)) {
                function = Sorting::sortIndices;
            } else {
                throw new IllegalStateException("Unknown sort function: " + name);
            }
        }
    }

    /**
     * Sort the unique indices.
     *
     * @param function Source of the function.
     * @param source Source of the data.
     * @param bh Data sink.
     */
    @Benchmark
    public void indexSort(IndexSortSource function, IndexDataSource source, Blackhole bh) {
        for (final int[] a : source.getData()) {
            bh.consume(function.getFunction().sort(a.clone(), a.length));
        }
    }
}
