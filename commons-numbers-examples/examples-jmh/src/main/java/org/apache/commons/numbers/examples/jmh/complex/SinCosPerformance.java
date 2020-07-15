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

package org.apache.commons.numbers.examples.jmh.complex;

import org.apache.commons.math3.util.FastMath;
import org.apache.commons.numbers.core.Precision;
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
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;

/**
 * Executes a benchmark to estimate the speed of sin/cos operations.
 * This compares the Math implementation to FastMath. It would be possible
 * to adapt FastMath to compute sin/cos together as they both use a common
 * initial stage to map the value to the domain [0, pi/2).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx512M"})
public class SinCosPerformance {
    /**
     * An array of edge numbers that will produce edge case results from sin/cos functions:
     * {@code +/-inf, +/-0, nan}.
     */
    private static final double[] EDGE_NUMBERS = {
        Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0, -0.0, Double.NaN};

    /**
     * Contains the size of numbers.
     */
    @State(Scope.Benchmark)
    public static class NumberSize {
        /**
         * The size of the data.
         */
        @Param({"1000"})
        private int size;

        /**
         * Gets the size.
         *
         * @return the size
         */
        public int getSize() {
            return size;
        }
    }

    /**
     * Contains an array of numbers.
     */
    public abstract static class BaseNumbers extends NumberSize {
        /** The numbers. */
        protected double[] numbers;

        /**
         * Gets the numbers.
         *
         * @return the numbers
         */
        public double[] getNumbers() {
            return numbers;
        }

        /**
         * Create the complex numbers.
         */
        @Setup
        public void setup() {
            numbers = createNumbers(new SplittableRandom());
            // Verify functions
            for (final double x : numbers) {
                final double sin = Math.sin(x);
                assertEquals(sin, FastMath.sin(x), 1, () -> "sin " + x);
                final double cos = Math.cos(x);
                assertEquals(cos, FastMath.cos(x), 1, () -> "cos " + x);
            }
        }

        /**
         * Creates the numbers.
         *
         * @param rng Random number generator.
         * @return the random number
         */
        protected abstract double[] createNumbers(SplittableRandom rng);
    }

    /**
     * Contains an array of numbers.
     */
    @State(Scope.Benchmark)
    public static class Numbers extends BaseNumbers {
        /**
         * The type of the data.
         */
        @Param({"pi", "pi/2", "random", "edge"})
        private String type;

        /** {@inheritDoc} */
        @Override
        protected double[] createNumbers(SplittableRandom rng) {
            DoubleSupplier generator;
            if ("pi".equals(type)) {
                generator = () -> rng.nextDouble() * 2 * Math.PI - Math.PI;
            } else if ("pi/2".equals(type)) {
                generator = () -> rng.nextDouble() * Math.PI - Math.PI / 2;
            } else if ("random".equals(type)) {
                generator = () -> createRandomNumber(rng);
            } else if ("edge".equals(type)) {
                generator = () -> createEdgeNumber(rng);
            } else {
                throw new IllegalStateException("Unknown number type: " + type);
            }
            return DoubleStream.generate(generator).limit(getSize()).toArray();
        }
    }

    /**
     * Contains an array of uniform numbers.
     */
    @State(Scope.Benchmark)
    public static class UniformNumbers extends BaseNumbers {
        /**
         * The range of the data.
         *
         * <p>Note: Representations of half-pi and pi are rounded down
         * to ensure the value is less than the exact representation.
         */
        @Param({"1.57079", "3.14159", "10", "100", "1e4", "1e8", "1e16", "1e32"})
        private double range;

        /** {@inheritDoc} */
        @Override
        protected double[] createNumbers(SplittableRandom rng) {
            return rng.doubles(getSize(), -range, range).toArray();
        }
    }

    /**
     * Assert the values are equal to the given ulps, else throw an AssertionError.
     *
     * @param x the x
     * @param y the y
     * @param maxUlps the max ulps for equality
     * @param msg the message upon failure
     */
    static void assertEquals(double x, double y, int maxUlps, Supplier<String> msg) {
        if (!Precision.equalsIncludingNaN(x, y, maxUlps)) {
            throw new AssertionError(msg.get() + ": " + x + " != " + y);
        }
    }

    /**
     * Creates a random double number uniformly distributed over a range much larger than [-pi, pi].
     * The data is a test of the reduction operation to convert a large number to the domain
     * [0, pi/2).
     *
     * @param rng Random number generator.
     * @return the random number
     */
    private static double createRandomNumber(SplittableRandom rng) {
        return rng.nextDouble(-1e200, 1e200);
    }

    /**
     * Creates a random double number that will be an edge case:
     * {@code +/-inf, +/-0, nan}.
     *
     * @param rng Random number generator.
     * @return the random number
     */
    private static double createEdgeNumber(SplittableRandom rng) {
        return EDGE_NUMBERS[rng.nextInt(EDGE_NUMBERS.length)];
    }

    /**
     * Apply the function to all the numbers.
     *
     * @param numbers Numbers.
     * @param fun Function.
     * @param bh Data sink.
     */
    private static void apply(double[] numbers, DoubleUnaryOperator fun, Blackhole bh) {
        for (int i = 0; i < numbers.length; i++) {
            bh.consume(fun.applyAsDouble(numbers[i]));
        }
    }

    /**
     * Identity function. This can be used to measure overhead of copy array creation.
     *
     * @param z Complex number.
     * @return the number
     */
    private static double identity(double z) {
        return z;
    }

    // Benchmark methods.
    //
    // Benchmarks use function references to perform different operations on the numbers.
    // Tests show that explicit programming of the same benchmarks run in the same time.
    // For reference examples are provided for sin(x).

    /**
     * Explicit benchmark without using a method reference.
     * This is commented out as it exists for reference purposes.
     *
     * @param numbers Numbers.
     * @param bh Data sink.
     */
    //@Benchmark
    public void mathSin2(Numbers numbers, Blackhole bh) {
        final double[] x = numbers.getNumbers();
        for (int i = 0; i < x.length; i++) {
            bh.consume(Math.sin(x[i]));
        }
    }

    /**
     * Baseline the JMH overhead for all the benchmarks that create numbers. All other
     * methods are expected to be slower than this.
     *
     * @param numbers Numbers.
     * @param bh Data sink.
     */
    @Benchmark
    public void baselineIdentity(Numbers numbers, Blackhole bh) {
        apply(numbers.getNumbers(), SinCosPerformance::identity, bh);
    }

    /**
     * Benchmark {@link Math#sin(double)}.
     *
     * @param numbers Numbers.
     * @param bh Data sink.
     */
    @Benchmark
    public void mathSin(Numbers numbers, Blackhole bh) {
        apply(numbers.getNumbers(), Math::sin, bh);
    }

    /**
     * Benchmark {@link Math#cos(double)}.
     *
     * @param numbers Numbers.
     * @param bh Data sink.
     */
    @Benchmark
    public void mathCos(Numbers numbers, Blackhole bh) {
        apply(numbers.getNumbers(), Math::cos, bh);
    }

    /**
     * Benchmark {@link FastMath#sin(double)}.
     *
     * @param numbers Numbers.
     * @param bh Data sink.
     */
    @Benchmark
    public void fastMathSin(Numbers numbers, Blackhole bh) {
        apply(numbers.getNumbers(), FastMath::sin, bh);
    }

    /**
     * Benchmark {@link FastMath#cos(double)}.
     *
     * @param numbers Numbers.
     * @param bh Data sink.
     */
    @Benchmark
    public void fastMathCos(Numbers numbers, Blackhole bh) {
        apply(numbers.getNumbers(), FastMath::cos, bh);
    }

    /**
     * Benchmark {@link Math#sin(double)} using a uniform range of numbers.
     *
     * @param numbers Numbers.
     * @param bh Data sink.
     */
    @Benchmark
    public void rangeMathSin(UniformNumbers numbers, Blackhole bh) {
        apply(numbers.getNumbers(), Math::sin, bh);
    }

    /**
     * Benchmark {@link FastMath#sin(double)} using a uniform range of numbers.
     *
     * @param numbers Numbers.
     * @param bh Data sink.
     */
    @Benchmark
    public void rangeFastMathSin(UniformNumbers numbers, Blackhole bh) {
        apply(numbers.getNumbers(), FastMath::sin, bh);
    }
}
