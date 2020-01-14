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
    @State(Scope.Benchmark)
    public static class Numbers extends NumberSize {
        /** The numbers. */
        protected double[] numbers;

        /**
         * The type of the data.
         */
        @Param({"pi", "pi/2", "random", "edge"})
        private String type;

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
        private double[] createNumbers(SplittableRandom rng) {
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

        /**
         * Assert the values are equal to the given ulps, else throw an AssertionError.
         *
         * @param x the x
         * @param y the y
         * @param maxUlps the max ulps for equality
         * @param msg the message upon failure
         */
        private static void assertEquals(double x, double y, int maxUlps, Supplier<String> msg) {
            if (!Precision.equalsIncludingNaN(x, y, maxUlps)) {
                throw new AssertionError(msg.get() + ": " + x + " != " + y);
            }
        }
    }

    /**
     * Creates a random double number with a random sign and mantissa and a large range for
     * the exponent. The numbers will not be uniform over the range.
     *
     * @param rng Random number generator.
     * @return the random number
     */
    private static double createRandomNumber(SplittableRandom rng) {
        // Create random doubles using random bits in the sign bit and the mantissa.
        // Then create an exponent in the range -64 to 64.
        final long mask = ((1L << 52) - 1) | 1L << 63;
        final long bits = rng.nextLong() & mask;
        // The exponent must be unsigned so + 1023 to the signed exponent
        final long exp = rng.nextInt(129) - 64 + 1023;
        return Double.longBitsToDouble(bits | (exp << 52));
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
     * @return the result of the function.
     */
    private static double[] apply(double[] numbers, DoubleUnaryOperator fun) {
        final double[] result = new double[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            result[i] = fun.applyAsDouble(numbers[i]);
        }
        return result;
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
    // The methods are partially documented as the names are self-documenting.
    // CHECKSTYLE: stop JavadocMethod
    // CHECKSTYLE: stop DesignForExtension
    //
    // Benchmarks use function references to perform different operations on the numbers.
    // Tests show that explicit programming of the same benchmarks run in the same time.
    // For reference examples are provided for sin(x).

    /**
     * Explicit benchmark without using a method reference.
     * This is commented out as it exists for reference purposes.
     */
    //@Benchmark
    public double[] mathSin2(Numbers numbers) {
        final double[] x = numbers.getNumbers();
        final double[] result = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            result[i] = Math.sin(x[i]);
        }
        return result;
    }

    /**
     * Baseline the creation of the new array of numbers with the same number (an identity).
     * This contains the baseline JMH overhead for all the benchmarks that create numbers.
     * All other methods are expected to be slower than this.
     */
    @Benchmark
    public double[] baselineIdentity(Numbers numbers) {
        return apply(numbers.getNumbers(), SinCosPerformance::identity);
    }

    @Benchmark
    public double[] mathSin(Numbers numbers) {
        return apply(numbers.getNumbers(), Math::sin);
    }

    @Benchmark
    public double[] mathCos(Numbers numbers) {
        return apply(numbers.getNumbers(), Math::cos);
    }

    @Benchmark
    public double[] fastMathSin(Numbers numbers) {
        return apply(numbers.getNumbers(), FastMath::sin);
    }

    @Benchmark
    public double[] fastMathCos(Numbers numbers) {
        return apply(numbers.getNumbers(), FastMath::cos);
    }
}
