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
import java.util.function.ToDoubleFunction;

import org.apache.commons.numbers.core.Norm;
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
 * Execute benchmarks for the methods in the {@link Norm} class.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx512M"})
public class NormPerformance {

    /** Class providing input vectors for benchmarks.
     */
    @State(Scope.Benchmark)
    public static class VectorArrayInput {

        /** Number of vector samples. */
        @Param("100000")
        private int samples;

        /** Minimum possible double exponent. */
        @Param("-550")
        private int minExp;

        /** Maximum possible double exponent. */
        @Param("+550")
        private int maxExp;

        /** Range of exponents within a single vector. */
        @Param("26")
        private int vectorExpRange;

        /** Array of input vectors. */
        private double[][] vectors;

        /** Get the input vectors.
         * @return input vectors
         */
        public double[][] getVectors() {
            return vectors;
        }

        /** Create the input vectors for the instance.
         */
        @Setup
        public void createVectors() {
            final UniformRandomProvider rng = RandomSource.create(RandomSource.XO_RO_SHI_RO_1024_PP);

            vectors = new double[samples][];
            for (int i = 0; i < vectors.length; ++i) {
                // pick a general range for the vector element exponents and then
                // create values within that range
                final int vMidExp = rng.nextInt(maxExp - minExp + 1) + minExp;
                final int vExpRadius = vectorExpRange / 2;
                final int vMinExp = vMidExp - vExpRadius;
                final int vMaxExp = vMidExp + vExpRadius;

                vectors[i] = DoubleUtils.randomArray(getLength(), vMinExp, vMaxExp, rng);
            }
        }

        /** Get the length of the input vectors.
         * @return input vector length
         */
        protected int getLength() {
            return 3;
        }
    }

    /** Class providing 2D input vectors for benchmarks.
     */
    @State(Scope.Benchmark)
    public static class VectorArrayInput2D extends VectorArrayInput {

        /** {@inheritDoc} */
        @Override
        protected int getLength() {
            return 2;
        }
    }

    /** Evaluate a norm computation method with the given input.
     * @param fn function to evaluate
     * @param input computation input
     * @param bh blackhole
     */
    private static void eval(final ToDoubleFunction<double[]> fn,
                             final VectorArrayInput input,
                             final Blackhole bh) {
        final double[][] vectors = input.getVectors();
        for (int i = 0; i < vectors.length; ++i) {
            bh.consume(fn.applyAsDouble(vectors[i]));
        }
    }

    /** Evaluate a norm computation method with the given input.
     * @param fn function to evaluate
     * @param input computation input
     * @param bh blackhole
     */
    private static void eval(final Norm fn,
                             final VectorArrayInput input,
                             final Blackhole bh) {
        final double[][] vectors = input.getVectors();
        for (int i = 0; i < vectors.length; ++i) {
            bh.consume(fn.of(vectors[i]));
        }
    }

    /** Compute the Euclidean norm directly with no checks for overflow or underflow.
     * @param v input vector
     * @return Euclidean norm
     */
    private static double directEuclideanNorm(final double[] v) {
        double n = 0;
        for (int i = 0; i < v.length; i++) {
            n += v[i] * v[i];
        }
        return Math.sqrt(n);
    }

    /** Compute a baseline performance metric with a method that does nothing.
     * @param input benchmark input
     * @param bh blackhole
     */
    @Benchmark
    public void baseline(final VectorArrayInput input, final Blackhole bh) {
        eval(v -> 0d, input, bh);
    }

    /** Compute a baseline performance metric using direct computation of the
     * Euclidean norm.
     * @param input benchmark input
     * @param bh blackhole
     */
    @Benchmark
    public void directEuclideanArray(final VectorArrayInput input, final Blackhole bh) {
        eval(NormPerformance::directEuclideanNorm, input, bh);
    }

    /** Compute a baseline performance metric using {@link Math#hypot(double, double)}.
     * @param input benchmark input
     * @param bh blackhole
     */
    @Benchmark
    public void hypot(final VectorArrayInput2D input, final Blackhole bh) {
        eval(v -> Math.hypot(v[0], v[1]), input, bh);
    }

    /** Compute the performance of the {@link Norm#L2} 2D method.
     * @param input benchmark input
     * @param bh blackhole
     */
    @Benchmark
    public void euclidean2d(final VectorArrayInput2D input, final Blackhole bh) {
        eval(v -> Norm.L2.of(v[0], v[1]), input, bh);
    }

    /** Compute the performance of the {@link Norm#L2} 3D norm computation.
     * @param input benchmark input
     * @param bh blackhole
     */
    @Benchmark
    public void euclidean3d(final VectorArrayInput input, final Blackhole bh) {
        eval(v -> Norm.L2.of(v[0], v[1], v[2]), input, bh);
    }

    /** Compute the performance of the {@link Norm#L2} array norm method.
     * @param input benchmark input
     * @param bh blackhole
     */
    @Benchmark
    public void euclideanArray(final VectorArrayInput input, final Blackhole bh) {
        eval(Norm.L2, input, bh);
    }
}
