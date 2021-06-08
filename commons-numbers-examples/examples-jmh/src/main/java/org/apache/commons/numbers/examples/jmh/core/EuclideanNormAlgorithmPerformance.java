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
 * Execute benchmarks for the algorithms in the {@link EuclideanNormAlgorithms} class.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx512M"})
public class EuclideanNormAlgorithmPerformance {

    /** String indicating double exponents with very low negative values, likely to underflow. */
    private static final String LOW = "low";

    /** String indicating double exponents with mid-level values which will not overflow or underflow. */
    private static final String MID = "mid";

    /** String indicating double exponents with very high positive values, likely to overflow. */
    private static final String HIGH = "high";

    /** String indicating double exponents over a very wide range of values. */
    private static final String FULL = "full";

    /** Class providing input vectors for benchmarks.
     */
    @State(Scope.Benchmark)
    public static class VectorArrayInput {

        /** The number of samples. */
        @Param("100000")
        private int samples;

        /** The length of each vector. */
        @Param("100")
        private int vectorLength;

        /** The type of double values placed in the vector arrays. */
        @Param({LOW, MID, HIGH, FULL})
        private String type;

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

            int minExp;
            int maxExp;

            switch (type) {
            case LOW:
                minExp = -530;
                maxExp = -510;
                break;
            case MID:
                minExp = -10;
                maxExp = +10;
                break;
            case HIGH:
                minExp = +510;
                maxExp = +530;
                break;
            default:
                throw new IllegalArgumentException("Invalid vector type: " + type);
            }

            vectors = new double[samples][];
            for (int i = 0; i < vectors.length; ++i) {
                vectors[i] = DoubleUtils.randomArray(vectorLength, minExp, maxExp, rng);
            }
        }
    }

    /** Evaluate a norm computation method with the given input.
     * @param fn function to evaluate
     * @param input computation input
     * @param bh blackhole
     */
    private static void eval(final ToDoubleFunction<double[]> fn, final VectorArrayInput input,
            final Blackhole bh) {
        final double[][] vectors = input.getVectors();
        for (int i = 0; i < vectors.length; ++i) {
            bh.consume(fn.applyAsDouble(vectors[i]));
        }
    }

    /** Compute the performance of the {@link EuclideanNormAlgorithms.Exact} class.
     * @param input benchmark input
     * @param bh blackhole
     */
    @Benchmark
    public void exact(final VectorArrayInput input, final Blackhole bh) {
        eval(new EuclideanNormAlgorithms.Exact(), input, bh);
    }

    /** Compute the performance of the {@link EuclideanNormAlgorithms.Direct} class.
     * @param input benchmark input
     * @param bh blackhole
     */
    @Benchmark
    public void direct(final VectorArrayInput input, final Blackhole bh) {
        eval(new EuclideanNormAlgorithms.Direct(), input, bh);
    }

    /** Compute the performance of the {@link EuclideanNormAlgorithms.Enorm} class.
     * @param input benchmark input
     * @param bh blackhole
     */
    @Benchmark
    public void enorm(final VectorArrayInput input, final Blackhole bh) {
        eval(new EuclideanNormAlgorithms.Enorm(), input, bh);
    }

    /** Compute the performance of the {@link EuclideanNormAlgorithms.EnormMod} class.
     * @param input benchmark input
     * @param bh blackhole
     */
    @Benchmark
    public void enormMod(final VectorArrayInput input, final Blackhole bh) {
        eval(new EuclideanNormAlgorithms.EnormMod(), input, bh);
    }

    /** Compute the performance of the {@link EuclideanNormAlgorithms.EnormModKahan} class.
     * @param input benchmark input
     * @param bh blackhole
     */
    @Benchmark
    public void enormModKahan(final VectorArrayInput input, final Blackhole bh) {
        eval(new EuclideanNormAlgorithms.EnormModKahan(), input, bh);
    }

    /** Compute the performance of the {@link EuclideanNormAlgorithms.EnormModExt} class.
     * @param input benchmark input
     * @param bh blackhole
     */
    @Benchmark
    public void enormModExt(final VectorArrayInput input, final Blackhole bh) {
        eval(new EuclideanNormAlgorithms.EnormModExt(), input, bh);
    }

    /** Compute the performance of the {@link EuclideanNormAlgorithms.ExtendedPrecisionLinearCombination} class.
     * @param input benchmark input
     * @param bh blackhole
     */
    @Benchmark
    public void extLinear(final VectorArrayInput input, final Blackhole bh) {
        eval(new EuclideanNormAlgorithms.ExtendedPrecisionLinearCombination(), input, bh);
    }


    /** Compute the performance of the {@link EuclideanNormAlgorithms.ExtendedPrecisionLinearCombinationMod} class.
     * @param input benchmark input
     * @param bh blackhole
     */
    @Benchmark
    public void extLinearMod(final VectorArrayInput input, final Blackhole bh) {
        eval(new EuclideanNormAlgorithms.ExtendedPrecisionLinearCombinationMod(), input, bh);
    }

    /** Compute the performance of the {@link EuclideanNormAlgorithms.ExtendedPrecisionLinearCombinationSinglePass}
     * class.
     * @param input benchmark input
     * @param bh blackhole
     */
    @Benchmark
    public void extLinearSinglePass(final VectorArrayInput input, final Blackhole bh) {
        eval(new EuclideanNormAlgorithms.ExtendedPrecisionLinearCombinationSinglePass(), input, bh);
    }

    /** Compute the performance of the {@link EuclideanNormAlgorithms.ExtendedPrecisionLinearCombinationSqrt2}
     * class.
     * @param input benchmark input
     * @param bh blackhole
     */
    @Benchmark
    public void extLinearSqrt2(final VectorArrayInput input, final Blackhole bh) {
        eval(new EuclideanNormAlgorithms.ExtendedPrecisionLinearCombinationSqrt2(), input, bh);
    }
}
