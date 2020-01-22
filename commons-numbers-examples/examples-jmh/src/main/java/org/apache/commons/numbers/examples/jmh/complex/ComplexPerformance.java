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

import org.apache.commons.numbers.complex.Complex;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.ZigguratNormalizedGaussianSampler;
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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Executes a benchmark to measure the speed of operations in the {@link Complex} class.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx512M"})
public class ComplexPerformance {
    /**
     * An array of edge numbers that will produce edge case results from functions:
     * {@code +/-inf, +/-max, +/-min, +/-0, nan}.
     */
    private static final double[] EDGE_NUMBERS = {
        Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MAX_VALUE,
        -Double.MAX_VALUE, Double.MIN_VALUE, -Double.MIN_VALUE, 0.0, -0.0, Double.NaN};

    /** The range to use for uniform random numbers. */
    private static final double RANGE = 3.456789;

    /**
     * Contains the size of numbers.
     */
    @State(Scope.Benchmark)
    public static class ComplexNumberSize {
        /**
         * The size of the data.
         */
        @Param({"10000"})
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
     * Contains an array of complex numbers.
     */
    @State(Scope.Benchmark)
    public static class ComplexNumbers extends ComplexNumberSize {
        /** The numbers. */
        protected Complex[] numbers;

        /**
         * The type of the data.
         */
        @Param({"cis", "vector", "log-uniform", "uniform", "edge"})
        private String type;

        /**
         * Gets the numbers.
         *
         * @return the numbers
         */
        public Complex[] getNumbers() {
            return numbers;
        }

        /**
         * Create the complex numbers.
         */
        @Setup
        public void setup() {
            numbers = createNumbers(RandomSource.create(RandomSource.XO_RO_SHI_RO_128_PP));
        }

        /**
         * Creates the numbers.
         *
         * @param rng Random number generator.
         * @return the random complex number
         */
        Complex[] createNumbers(UniformRandomProvider rng) {
            Supplier<Complex> generator;
            if ("cis".equals(type)) {
                generator = () -> Complex.ofCis(rng.nextDouble() * 2 * Math.PI);
            } else if ("vector".equals(type)) {
                // An unnormalised random vector is created using a Gaussian sample
                // for each dimension. Normalisation would create a cis number.
                // This is effectively a polar complex number with random modulus
                // in [-pi, pi] and random magnitude in a range defined by a Chi-squared
                // distribution with 2 degrees of freedom.
                final ZigguratNormalizedGaussianSampler s = ZigguratNormalizedGaussianSampler.of(rng);
                generator = () -> Complex.ofCartesian(s.sample(), s.sample());
            } else if ("log-uniform".equals(type)) {
                generator = () -> Complex.ofCartesian(createLogUniformNumber(rng), createLogUniformNumber(rng));
            } else if ("uniform".equals(type)) {
                generator = () -> Complex.ofCartesian(createUniformNumber(rng), createUniformNumber(rng));
            } else if ("edge".equals(type)) {
                generator = () -> Complex.ofCartesian(createEdgeNumber(rng), createEdgeNumber(rng));
            } else {
                throw new IllegalStateException("Unknown number type: " + type);
            }
            return Stream.generate(generator).limit(getSize()).toArray(Complex[]::new);
        }
    }

    /**
     * Contains two arrays of complex numbers.
     */
    @State(Scope.Benchmark)
    public static class TwoComplexNumbers extends ComplexNumbers {
        /** The numbers. */
        private Complex[] numbers2;

        /**
         * Gets the second set of numbers.
         *
         * @return the numbers
         */
        public Complex[] getNumbers2() {
            return numbers2;
        }

        /**
         * Create the complex numbers.
         */
        @Override
        @Setup
        public void setup() {
            // Do not call super.setup() so we recycle the RNG and avoid duplicates
            final UniformRandomProvider rng = RandomSource.create(RandomSource.XO_RO_SHI_RO_128_PP);
            numbers = createNumbers(rng);
            numbers2 = createNumbers(rng);
        }
    }

    /**
     * Contains an array of complex numbers and an array of real numbers.
     */
    @State(Scope.Benchmark)
    public static class ComplexAndRealNumbers extends ComplexNumbers {
        /** The numbers. */
        private double[] numbers2;

        /**
         * Gets the second set of numbers.
         *
         * @return the numbers
         */
        public double[] getNumbers2() {
            return numbers2;
        }

        /**
         * Create the complex numbers.
         */
        @Override
        @Setup
        public void setup() {
            // Do not call super.setup() so we recycle the RNG and avoid duplicates
            final UniformRandomProvider rng = RandomSource.create(RandomSource.XO_RO_SHI_RO_128_PP);
            numbers = createNumbers(rng);
            numbers2 = Arrays.stream(createNumbers(rng)).mapToDouble(Complex::real).toArray();
        }
    }

    /**
     * Define a function between a complex and real number.
     */
    private interface ComplexRealFunction {
        /**
         * Applies this function to the given arguments.
         *
         * @param z the complex argument
         * @param x the real argument
         * @return the function result
         */
        Complex apply(Complex z, double x);
    }

    /**
     * Creates a random double number with a random sign and mantissa and a large range for
     * the exponent. The numbers will not be uniform over the range. This samples randomly
     * using the components of a double. The limiting distribution is the log-uniform distribution.
     *
     * @param rng Random number generator.
     * @return the random number
     * @see <a href="https://en.wikipedia.org/wiki/Reciprocal_distribution">Reciprocal (log-uniform) distribution</a>
     */
    private static double createLogUniformNumber(UniformRandomProvider rng) {
        // Create random doubles using random bits in the sign bit and the mantissa.
        // Then create an exponent in the range -64 to 64. Thus the sum product
        // of 4 max or min values will not over or underflow.
        final long mask = ((1L << 52) - 1) | 1L << 63;
        final long bits = rng.nextLong() & mask;
        // The exponent must be unsigned so + 1023 to the signed exponent
        final long exp = rng.nextInt(129) - 64 + 1023;
        return Double.longBitsToDouble(bits | (exp << 52));
    }

    /**
     * Creates a random double number with a random sign and uniform range.
     *
     * @param rng Random number generator.
     * @return the random number
     */
    private static double createUniformNumber(UniformRandomProvider rng) {
        // Note: [0, 1) - 1 is [-1, 0).
        // Since the 1 is a 50/50 sample the result is the interval [-1, 1)
        // using the 2^54 dyadic rationals in the interval.
        // The range is not critical. The numbers will have approximately 50%
        // with the same exponent, max, matching that of RANGE and the rest smaller
        // exponents down to (max - 53) since the uniform deviate is limited to 2^-53.
        return (rng.nextDouble() - rng.nextInt(1)) * RANGE;
    }

    /**
     * Creates a random double number that will be an edge case:
     * {@code +/-inf, +/-max, +/-min, +/-0, nan}.
     *
     * @param rng Random number generator.
     * @return the random number
     */
    private static double createEdgeNumber(UniformRandomProvider rng) {
        return EDGE_NUMBERS[rng.nextInt(EDGE_NUMBERS.length)];
    }

    /**
     * Apply the function to all the numbers.
     *
     * @param numbers Numbers.
     * @param fun Function.
     * @return the result of the function.
     */
    private static boolean[] apply(Complex[] numbers, Predicate<Complex> fun) {
        final boolean[] result = new boolean[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            result[i] = fun.test(numbers[i]);
        }
        return result;
    }

    /**
     * Apply the function to all the numbers.
     *
     * @param numbers Numbers.
     * @param fun Function.
     * @return the result of the function.
     */
    private static double[] apply(Complex[] numbers, ToDoubleFunction<Complex> fun) {
        final double[] result = new double[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            result[i] = fun.applyAsDouble(numbers[i]);
        }
        return result;
    }

    /**
     * Apply the function to all the numbers.
     *
     * @param numbers Numbers.
     * @param fun Function.
     * @return the result of the function.
     */
    private static Complex[] apply(Complex[] numbers, UnaryOperator<Complex> fun) {
        final Complex[] result = new Complex[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            result[i] = fun.apply(numbers[i]);
        }
        return result;
    }

    /**
     * Apply the function to the paired numbers.
     *
     * @param numbers First numbers of the pairs.
     * @param numbers2 Second numbers of the pairs.
     * @param fun Function.
     * @return the result of the function.
     */
    private static Complex[] apply(Complex[] numbers, Complex[] numbers2,
            BiFunction<Complex, Complex, Complex> fun) {
        final Complex[] result = new Complex[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            result[i] = fun.apply(numbers[i], numbers2[i]);
        }
        return result;
    }

    /**
     * Apply the function to the paired numbers.
     *
     * @param numbers First numbers of the pairs.
     * @param numbers2 Second numbers of the pairs.
     * @param fun Function.
     * @return the result of the function.
     */
    private static Complex[] apply(Complex[] numbers, double[] numbers2,
            ComplexRealFunction fun) {
        final Complex[] result = new Complex[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            result[i] = fun.apply(numbers[i], numbers2[i]);
        }
        return result;
    }

    /**
     * Identity function. This can be used to measure overhead of object array creation.
     *
     * @param z Complex number.
     * @return the complex number
     */
    private static Complex identity(Complex z) {
        return z;
    }

    /**
     * Copy function. This can be used to measure overhead of object array creation plus
     * new Complex creation.
     *
     * @param z Complex number.
     * @return a copy of the complex number
     */
    private static Complex copy(Complex z) {
        return Complex.ofCartesian(z.real(), z.imag());
    }

    // Benchmark methods.
    //
    // The methods are partially documented as the names are self-documenting.
    // CHECKSTYLE: stop JavadocMethod
    // CHECKSTYLE: stop DesignForExtension
    //
    // Benchmarks use function references to perform different operations on the complex numbers.
    // Tests show that explicit programming of the same benchmarks run in the same time.
    // For reference examples are provided for the fastest operations: real() and conj().

    /**
     * Explicit benchmark without using a method reference.
     * This should run in the same time as {@link #real(ComplexNumbers)}.
     * This is commented out as it exists for reference purposes.
     */
    //@Benchmark
    public double[] real2(ComplexNumbers numbers) {
        final Complex[] z = numbers.getNumbers();
        final double[] result = new double[z.length];
        for (int i = 0; i < z.length; i++) {
            result[i] = z[i].real();
        }
        return result;
    }

    /**
     * Explicit benchmark without using a method reference.
     * This should run in the same time as {@link #conj(ComplexNumbers)}.
     * This is commented out as it exists for reference purposes.
     */
    //@Benchmark
    public Complex[] conj2(ComplexNumbers numbers) {
        final Complex[] z = numbers.getNumbers();
        final Complex[] result = new Complex[z.length];
        for (int i = 0; i < z.length; i++) {
            result[i] = z[i].conj();
        }
        return result;
    }

    /**
     * Baseline the creation of the new array of numbers.
     * This contains the baseline JMH overhead for all the benchmarks that create complex numbers.
     * All other methods are expected to be slower than this.
     */
    @Benchmark
    public Complex[] baselineNewArray(ComplexNumberSize numberSize) {
        return new Complex[numberSize.getSize()];
    }

    /**
     * Baseline the creation of a copy array of numbers.
     * This is commented out as it provides no information other than to demonstrate that
     * {@link #baselineCopy(ComplexNumbers)} is not being optimised to a single array copy
     * operation.
     */
    //@Benchmark
    public Complex[] baselineCopyArray(ComplexNumbers numbers) {
        return Arrays.copyOf(numbers.getNumbers(), numbers.getNumbers().length);
    }

    /**
     * Baseline the creation of the new array of numbers with the same complex number (an identity).
     *
     * <p>Note: This runs much faster than {@link #baselineCopy(ComplexNumbers)}. This is
     * attributed to the identity function not requiring that the fields of the
     * complex are accessed unlike all other methods that do computations on the real and/or
     * imaginary parts. The method is slower than a creation of a new empty array or a
     * copy array thus contains the loop overhead of the benchmarks that create new numbers.
     *
     * @see #baselineNewArray(ComplexNumberSize)
     * @see #baselineCopyArray(ComplexNumbers)
     */
    @Benchmark
    public Complex[] baselineIdentity(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), ComplexPerformance::identity);
    }

    /**
     * Baseline the creation of the new array of numbers with a copy complex number. This
     * measures the overhead of creation of new complex numbers including field access
     * to the real and imaginary parts.
     */
    @Benchmark
    public Complex[] baselineCopy(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), ComplexPerformance::copy);
    }

    // Unary operations that return a boolean

    @Benchmark
    public boolean[] isNaN(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::isNaN);
    }

    @Benchmark
    public boolean[] isInfinite(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::isInfinite);
    }

    @Benchmark
    public boolean[] isFinite(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::isFinite);
    }

    // Unary operations that return a double

    @Benchmark
    public double[] real(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::real);
    }

    @Benchmark
    public double[] imag(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::imag);
    }

    @Benchmark
    public double[] abs(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::abs);
    }

    @Benchmark
    public double[] arg(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::arg);
    }

    @Benchmark
    public double[] norm(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::norm);
    }

    /**
     * This test demonstrates that the method used in abs() is not as fast as using square
     * root of the norm. The C99 standard for the abs() function requires over/underflow
     * protection in the intermediate computation and infinity edge case handling. This
     * has a performance overhead.
     */
    @Benchmark
    public double[] sqrtNorm(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), (ToDoubleFunction<Complex>) z -> Math.sqrt(z.norm()));
    }

    /**
     * This test demonstrates that the {@link Math#hypot(double, double)} method
     * is not as fast as the custom implementation in abs().
     */
    @Benchmark
    public double[] absMathHypot(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), (ToDoubleFunction<Complex>) z -> Math.hypot(z.real(), z.imag()));
    }

    // Unary operations that return a complex number

    @Benchmark
    public Complex[] conj(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::conj);
    }

    @Benchmark
    public Complex[] negate(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::negate);
    }

    @Benchmark
    public Complex[] proj(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::proj);
    }

    @Benchmark
    public Complex[] cos(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::cos);
    }

    @Benchmark
    public Complex[] cosh(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::cosh);
    }

    @Benchmark
    public Complex[] exp(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::exp);
    }

    @Benchmark
    public Complex[] log(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::log);
    }

    @Benchmark
    public Complex[] log10(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::log10);
    }

    @Benchmark
    public Complex[] sin(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::sin);
    }

    @Benchmark
    public Complex[] sinh(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::sinh);
    }

    @Benchmark
    public Complex[] sqrt(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::sqrt);
    }

    @Benchmark
    public Complex[] tan(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::tan);
    }

    @Benchmark
    public Complex[] tanh(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::tanh);
    }

    @Benchmark
    public Complex[] acos(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::acos);
    }

    @Benchmark
    public Complex[] acosh(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::acosh);
    }

    @Benchmark
    public Complex[] asin(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::asin);
    }

    @Benchmark
    public Complex[] asinh(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::asinh);
    }

    @Benchmark
    public Complex[] atan(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::atan);
    }

    @Benchmark
    public Complex[] atanh(ComplexNumbers numbers) {
        return apply(numbers.getNumbers(), Complex::atanh);
    }

    // Binary operations on two complex numbers.

    @Benchmark
    public Complex[] pow(TwoComplexNumbers numbers) {
        return apply(numbers.getNumbers(), numbers.getNumbers2(), Complex::pow);
    }

    @Benchmark
    public Complex[] multiply(TwoComplexNumbers numbers) {
        return apply(numbers.getNumbers(), numbers.getNumbers2(), Complex::multiply);
    }

    @Benchmark
    public Complex[] divide(TwoComplexNumbers numbers) {
        return apply(numbers.getNumbers(), numbers.getNumbers2(), Complex::divide);
    }

    @Benchmark
    public Complex[] add(TwoComplexNumbers numbers) {
        return apply(numbers.getNumbers(), numbers.getNumbers2(), Complex::add);
    }

    @Benchmark
    public Complex[] subtract(TwoComplexNumbers numbers) {
        return apply(numbers.getNumbers(), numbers.getNumbers2(), Complex::subtract);
    }

    // Binary operations on a complex and a real number.
    // These only benchmark methods on the real component as the
    // following are expected to be the same speed as the real-only operations
    // given the equivalent primitive operations:
    // - multiplyImaginary
    // - divideImaginary
    // - addImaginary
    // - subtractImaginary
    // - subtractFrom
    // - subtractFromImaginary

    @Benchmark
    public Complex[] powReal(ComplexAndRealNumbers numbers) {
        return apply(numbers.getNumbers(), numbers.getNumbers2(), Complex::pow);
    }

    @Benchmark
    public Complex[] multiplyReal(ComplexAndRealNumbers numbers) {
        return apply(numbers.getNumbers(), numbers.getNumbers2(), Complex::multiply);
    }

    @Benchmark
    public Complex[] divideReal(ComplexAndRealNumbers numbers) {
        return apply(numbers.getNumbers(), numbers.getNumbers2(), Complex::divide);
    }

    @Benchmark
    public Complex[] addReal(ComplexAndRealNumbers numbers) {
        return apply(numbers.getNumbers(), numbers.getNumbers2(), Complex::add);
    }

    @Benchmark
    public Complex[] subtractReal(ComplexAndRealNumbers numbers) {
        return apply(numbers.getNumbers(), numbers.getNumbers2(), Complex::subtract);
    }
}
