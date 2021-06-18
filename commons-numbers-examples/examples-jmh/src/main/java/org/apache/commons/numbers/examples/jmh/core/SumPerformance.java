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

import java.math.MathContext;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

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
     * The seed to use to create the factors.
     * Using a fixed seed ensures the same factors are created for the variable
     * length arrays as for the small fixed size arrays.
     */
    private static final long SEED = System.currentTimeMillis();

    /**
     * The factors to multiply.
     */
    @State(Scope.Benchmark)
    public static class Factors {
        /**
         * The condition number of the generated data.
         */
        @Param({"1e20"})
        private double c;

        /**
         * The number of factors.
         */
        @Param({"1000"})
        private int size;

        /** Factors a. */
        private double[][] a;

        /** Factors b. */
        private double[][] b;

        /**
         * Gets the length of the array of factors.
         * This exists to be overridden by factors of a specific length.
         * The default is to create factors of length 4 for use in the inlined
         * scalar product methods.
         *
         * @return the length
         */
        public int getLength() {
            return 4;
        }

        /**
         * Gets the number of scalar products to compute.
         *
         * @return the size
         */
        public int getSize() {
            return size;
        }

        /**
         * Gets the a factors.
         *
         * @param index the index
         * @return Factors b.
         */
        public double[] getA(int index) {
            return a[index];
        }

        /**
         * Gets the b factors.
         *
         * @param index the index
         * @return Factors b.
         */
        public double[]  getB(int index) {
            return b[index];
        }

        /**
         * Create the factors.
         */
        @Setup
        public void setup() {
            final UniformRandomProvider rng =
                    RandomSource.create(RandomSource.XO_RO_SHI_RO_1024_PP, SEED);
            // Use the ill conditioned data generation method.
            // This requires an array of at least 6.
            final int n = Math.max(6, getLength());
            final double[] x = new double[n];
            final double[] y = new double[n];
            a = new double[size][];
            b = new double[size][];
            // Limit precision to allow large array lengths to be generated.
            final MathContext mathContext = new MathContext(100);
            for (int i = 0; i < size; i++) {
                LinearCombinationUtils.genDot(c, rng, x, y, null, mathContext);
                a[i] = Arrays.copyOf(x, getLength());
                b[i] = Arrays.copyOf(y, getLength());
            }
        }
    }

    /**
     * Compute the sum of two arrays.
     * @param factors benchmark input
     * @param bh data sink
     */
    @Benchmark
    public void sum(final Factors factors, final Blackhole bh) {
        for (int i = 0; i < factors.getSize(); i++) {
            final double[] a = factors.getA(i);
            final double[] b = factors.getB(i);
            bh.consume(Sum.of(a).add(b).getAsDouble());
        }
    }

    /**
     * Compute the sum of products of two arrays.
     * @param factors benchmark input
     * @param bh data sink
     */
    @Benchmark
    public void sumOfProducts(final Factors factors, final Blackhole bh) {
        for (int i = 0; i < factors.getSize(); i++) {
            final double[] a = factors.getA(i);
            final double[] b = factors.getB(i);
            bh.consume(Sum.ofProducts(a, b).getAsDouble());
        }
    }


}
