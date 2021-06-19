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
package org.apache.commons.numbers.examples.jmh.core;

import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;

import org.apache.commons.numbers.core.Sum;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
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
 * Executes a benchmark to measure the speed of operations in the {@link Sum} class.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx512M"})
public class SumPerformance {
    /**
     * The seed to use to create the random benchmark input.
     * Using a fixed seed ensures the same values are created across benchmarks.
     */
    private static final long SEED = System.currentTimeMillis();

    /** Class providing double arrays for benchmarks.
     */
    @State(Scope.Benchmark)
    public static class ArrayInput {

        /** Number of array samples. */
        @Param("100000")
        private int samples;

        /** Number of values in each input array. */
        @Param("50")
        private int len;

        /** Minimum possible double exponent. */
        @Param("-550")
        private int minExp;

        /** Maximum possible double exponent. */
        @Param("+550")
        private int maxExp;

        /** Range of exponents within a single array. */
        @Param("26")
        private int expRange;

        /** First set of input arrays. */
        private double[][] a;

        /** Second set of input arrays. */
        private double[][] b;

        /** Get the first set of input arrays.
         * @return first set of input arrays
         */
        public double[][] getA() {
            return a;
        }

        /** Get the second set of input arrays.
         * @return second set of input arrays
         */
        public double[][] getB() {
            return b;
        }

        /** Create the input arrays for the instance. */
        @Setup
        public void createArrays() {
            final UniformRandomProvider rng = RandomSource.create(RandomSource.XO_RO_SHI_RO_1024_PP, SEED);

            a = new double[samples][];
            b = new double[samples][];
            for (int i = 0; i < samples; ++i) {
                // pick a general range for the array element exponents and then
                // create values within that range
                final int vMidExp = rng.nextInt(maxExp - minExp + 1) + minExp;
                final int vExpRadius = expRange / 2;
                final int vMinExp = vMidExp - vExpRadius;
                final int vMaxExp = vMidExp + vExpRadius;

                a[i] = DoubleUtils.randomArray(len, vMinExp, vMaxExp, rng);
                b[i] = DoubleUtils.randomArray(len, vMinExp, vMaxExp, rng);
            }
        }
    }

    /** Run a benchmark for a function that accepts a single array and produces a double result.
     * @param input benchmark input
     * @param bh data sink
     * @param fn function to benchmark
     */
    private static void runSingle(final ArrayInput input, final Blackhole bh,
            final ToDoubleFunction<double[]> fn) {
        final double[][] a = input.getA();
        for (int i = 0; i < a.length; ++i) {
            bh.consume(fn.applyAsDouble(a[i]));
        }
    }

    /** Run a benchmark for a function that accepts a two arrays and produces a double result.
     * @param input benchmark input
     * @param bh data sink
     * @param fn function to benchmark
     */
    private static void runDouble(final ArrayInput input, final Blackhole bh,
            final ToDoubleBiFunction<double[], double[]> fn) {
        final double[][] a = input.getA();
        final double[][] b = input.getB();
        for (int i = 0; i < a.length; ++i) {
            bh.consume(fn.applyAsDouble(a[i], b[i]));
        }
    }

    /** Benchmark baseline for functions that use a single input array.
     * @param input benchmark input
     * @param bh data sink
     */
    @Benchmark
    public void baselineSingle(final ArrayInput input, final Blackhole bh) {
        runSingle(input, bh, a -> 0d);
    }

    /** Benchmark baseline for functions that use two input arrays.
     * @param input benchmark input
     * @param bh data sink
     */
    @Benchmark
    public void baselineDouble(final ArrayInput input, final Blackhole bh) {
        runDouble(input, bh, (a, b) -> 0d);
    }

    /** Benchmark testing {@link Sum} addition performance.
     * @param input benchmark input
     * @param bh data sink
     */
    @Benchmark
    public void sum(final ArrayInput input, final Blackhole bh) {
        runSingle(input, bh, a -> Sum.of(a).getAsDouble());
    }

    /** Benchmark testing {@link Sum} linear combination performance.
     * @param input benchmark input
     * @param bh data sink
     */
    @Benchmark
    public void sumOfProducts(final ArrayInput input, final Blackhole bh) {
        runDouble(input, bh, (a, b) -> Sum.ofProducts(a, b).getAsDouble());
    }
}
