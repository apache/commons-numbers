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

// License for the Boost continued fraction adaptation:

//  (C) Copyright John Maddock 2006.
//  Use, modification and distribution are subject to the
//  Boost Software License, Version 1.0. (See accompanying file
//  LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)

package org.apache.commons.numbers.examples.jmh.core;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.math3.dfp.DfpField;
import org.apache.commons.numbers.core.DD;
import org.apache.commons.numbers.core.DDExt;
import org.apache.commons.numbers.core.DDMath;
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
 * Executes a benchmark to estimate the speed of double-double extended precision number
 * implementations.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx512M"})
public class DDPerformance {

    /** Static mutable DD implementation. */
    static final String IMP_DD_STATIC_MUTABLE = "static mutable";
    /** Static mutable DD implementation. */
    static final String IMP_DD_STATIC_MUTABLE_FULL_POW = "static mutable full-pow";
    /** OO immutable DD implementation. */
    static final String IMP_DD_OO_IMMUTABLE = "OO immutable";
    /** Full accuracy scaled power implementation. */
    static final String IMP_ACCURATE_POW_SCALED = "accuratePowScaled";
    /** Fast scaled power implementation. */
    static final String IMP_POW_SCALED = "powScaled";
    /** Low accuracy scaled power implementation base on {@link Math#pow(double, double)}. */
    static final String IMP_SIMPLE_POW_SCALED = "simplePowScaled";

    /**
     * Interface for an {@code (double, int) -> double} function.
     */
    public interface DoubleIntFunction {
        /**
         * Apply the function.
         *
         * @param x Argument.
         * @param n Argument.
         * @return the result
         */
        double apply(double x, int n);
    }

    /**
     * Interface for an {@code (DD, int) -> Object} function.
     */
    public interface DDIntFunction {
        /**
         * Apply the function.
         *
         * @param x Argument.
         * @param n Argument.
         * @return the result
         */
        Object apply(DD x, int n);
    }

    /**
     * A {@code (double, int)} tuple.
     */
    public static class DoubleInt {
        /** double value. */
        private final double x;
        /** int value. */
        private final int n;

        /**
         * @param x double value
         * @param n int value
         */
        DoubleInt(double x, int n) {
            this.x = x;
            this.n = n;
        }

        /**
         * @return x
         */
        double getX() {
            return x;
        }

        /**
         * @return n
         */
        int getN() {
            return n;
        }
    }

    /**
     * A {@code (DD, int)} tuple.
     */
    public static class DDInt {
        /** double value. */
        private final DD x;
        /** int value. */
        private final int n;

        /**
         * @param x double value
         * @param n int value
         */
        DDInt(DD x, int n) {
            this.x = x;
            this.n = n;
        }

        /**
         * @return x
         */
        DD getX() {
            return x;
        }

        /**
         * @return n
         */
        int getN() {
            return n;
        }
    }

    /**
     * Contains the function to computes the complementary probability {@code P[D_n^+ >= x]}
     * for the one-sided one-sample Kolmogorov-Smirnov distribution.
     */
    @State(Scope.Benchmark)
    public static class KSMethod {
        /** The implementation of the function. */
        @Param({IMP_DD_STATIC_MUTABLE, IMP_DD_STATIC_MUTABLE_FULL_POW, IMP_DD_OO_IMMUTABLE})
        private String implementation;

        /** The function. */
        private DoubleIntFunction function;

        /**
         * Gets the function.
         *
         * @return the function
         */
        public DoubleIntFunction getFunction() {
            return function;
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            function = createFunction(implementation);
        }

        /**
         * Creates the function to evaluate the one-sided one-sample Kolmogorov-Smirnov distribution.
         *
         * @param implementation Function implementation
         * @return the function
         */
        static DoubleIntFunction createFunction(String implementation) {
            if (IMP_DD_STATIC_MUTABLE.equals(implementation)) {
                return KolmogorovSmirnovDistribution.One::sfMutable;
            } else if (IMP_DD_STATIC_MUTABLE_FULL_POW.equals(implementation)) {
                return KolmogorovSmirnovDistribution.One::sfMutableFullPow;
            } else if (IMP_DD_OO_IMMUTABLE.equals(implementation)) {
                return KolmogorovSmirnovDistribution.One::sfOO;
            } else {
                throw new IllegalStateException("unknown KS method: " + implementation);
            }
        }

        /**
         * Gets the double-double implementations to compute the
         * one-sided one-sample Kolmogorov-Smirnov distribution.
         *
         * @return the implementations
         */
        static Stream<String> getImplementations() {
            return Stream.of(IMP_DD_STATIC_MUTABLE, IMP_DD_STATIC_MUTABLE_FULL_POW, IMP_DD_OO_IMMUTABLE);
        }
    }

    /**
     * Contains the data to computes the complementary probability {@code P[D_n^+ >= x]}
     * for the one-sided one-sample Kolmogorov-Smirnov distribution.
     */
    @State(Scope.Benchmark)
    public static class KSData {
        // The parameters should be chosen such that the computation takes less than 1 second
        // thus allowing for repeats in the iteration.
        // Maintain n*x*x < 372.5 and n*x > 3 (see KSSample for details).
        // n=10000, values=50, ux=0.15
        // n=100000, values=5, ux=0.05

        /** The sample size for the KS distribution. This should be below the large N limit of 1000000. */
        @Param({"1000"})
        private int n;
        /** The number of values. */
        @Param({"500"})
        private int values;
        /** The lower limit on x. */
        @Param({"0.001"})
        private double lx;
        /** The upper limit on x. */
        @Param({"0.5"})
        private double ux;

        /** The data. */
        private DoubleInt[] data;

        /**
         * Gets the data.
         *
         * @return the data
         */
        public DoubleInt[] getData() {
            return data;
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            data = createData(n, values, lx, ux);
        }

        /**
         * Creates the data for the one-sided one-sample Kolmogorov-Smirnov distribution.
         * The value {@code x} should by in the range [0, 1].
         *
         * @param n KS sample size
         * @param values Number of values
         * @param lx Lower limit on x
         * @param ux Upper limit on x
         * @return the data
         */
        static DoubleInt[] createData(int n, int values, double lx, double ux) {
            assert n > 0 : "Invalid n";
            assert lx <= ux : "Invalid range";
            if (values <= 1) {
                // Single value
                return new DoubleInt[] {new DoubleInt((lx + ux) * 0.5, n)};
            }
            // Create values between the lower and upper range
            final double inc = (ux - lx) / (values - 1);
            return IntStream.range(0, values)
                            .mapToObj(i -> new DoubleInt(lx + inc * i, n))
                            .toArray(DoubleInt[]::new);
        }
    }

    /**
     * Contains the data to computes the complementary probability {@code P[D_n^+ >= x]}
     * for the one-sided one-sample Kolmogorov-Smirnov distribution.
     */
    @State(Scope.Benchmark)
    public static class KSSample {
        /** The sample size for the KS distribution. This should be below the large N limit of 1000000. */
        @Param({"10000", "100000"})
        private int n;
        /**
         * The KS value (in the range [0, 1].
         *
         * <p>Note that the use of the full computation depends on the parameters.
         * If {@code n*x*x >= 372.5} then the p-value underflows. So large n limits the
         * usable upper range of x. If {@code n*x <= 3} then a faster computation can be performed
         * (either Smirnov-Dwass or exact when {@code nx <= 1}).
         */
        @Param({"0.01", "0.05"})
        private double x;

        /**
         * @return x
         */
        double getX() {
            return x;
        }

        /**
         * @return n
         */
        int getN() {
            return n;
        }
    }

    /**
     * Contains the data to compute the double-double operations.
     */
    @State(Scope.Benchmark)
    public static class OperatorData {
        /** The sample size. */
        @Param({"1000"})
        private int n;

        /** The data. */
        private DD[] data;
        /** The second data. */
        private DD[] data2;

        /**
         * Gets the data.
         *
         * @return the data
         */
        public DD[] getData() {
            return data;
        }

        /**
         * Gets the second data.
         *
         * @return the second data
         */
        public DD[] getData2() {
            return data2;
        }

        /**
         * Create the data.
         */
        @Setup(Level.Iteration)
        public void setup() {
            data = createData(n);
            data2 = createData(n);
        }

        /**
         * Creates data where the high part is approximately uniform in the range [-1, 1).
         *
         * @param n sample size
         * @return the data
         */
        static DD[] createData(int n) {
            UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
            return IntStream.range(0, n)
                            .mapToObj(i -> makeSignedDoubleDouble(rng))
                            .toArray(DD[]::new);
        }
    }

    /**
     * Contains the data to compute the double-double operations.
     *
     * <p>Note: This targets the package-private SDD implementation which contains variants
     * of binary operators.
     */
    @State(Scope.Benchmark)
    public static class BinaryOperatorMethod {
        /** The implementation of the function. */
        @Param({"baseline",
                // Summation of double-double values: (x, xx) + (y, yy)
                "add", "accurateAdd",
                // Summation of double values:  (x, xx) + y
                // Add y to the high part:
                // (x + y) => (z, zz) => (z, zz + xx)
                "addDouble", "accurateAddDouble",
                // Add y to the low part an ignore round-off
                // (x, xx + y)
                "twoSum", "fastTwoSum",
                // Multiplication: (x, xx) * (y, yy)
                "multiply", "accurateMultiply", "checkedMultiply",
                "square", "accurateSquare",
                // Multiplication: (x, xx) * y
                "multiplyDouble", "accurateMultiplyDouble", "checkedMultiplyDouble",
                // Division: (x, xx) / (y, yy)
                "divide", "accurateDivide",
                // Division: (x, xx) / y
                "divideDouble",
                "reciprocal", "accurateReciprocal",
                "sqrt", "accurateSqrt"})
        private String implementation;

        /** The function. */
        private BiFunction<DD, DD, Object> function;

        /**
         * Gets the function.
         *
         * @return the function
         */
        public BiFunction<DD, DD, Object> getFunction() {
            return function;
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            function = createFunction(implementation);
        }

        /**
         * Creates the function to compute the double-double operation.
         *
         * @param implementation Function implementation
         * @return the function
         */
        static BiFunction<DD, DD, Object> createFunction(String implementation) {
            if ("baseline".equals(implementation)) {
                return (a, b) -> DD.of(a.hi());
            } else if ("add".equals(implementation)) {
                return DD::add;
            } else if ("accurateAdd".equals(implementation)) {
                return DDExt::add;
            } else if ("addDouble".equals(implementation)) {
                return (a, b) -> a.add(b.hi());
            } else if ("accurateAddDouble".equals(implementation)) {
                return (a, b) -> DDExt.add(a, b.hi());
            } else if ("twoSum".equals(implementation)) {
                // twoSum is not public in DD, use SDD
                return (a, b) -> SDD.twoSum(a.hi(), a.lo() + b.hi(), SDD.create());
            } else if ("fastTwoSum".equals(implementation)) {
                // fastTwoSum is not public in DD, use SDD
                return (a, b) -> SDD.fastTwoSum(a.hi(), a.lo() + b.hi(), SDD.create());
            } else if ("multiply".equals(implementation)) {
                return DD::multiply;
            } else if ("accurateMultiply".equals(implementation)) {
                return DDExt::multiply;
            } else if ("checkedMultiply".equals(implementation)) {
                // DD is always unchecked, use SDD
                return (a, b) -> SDD.multiply(a.hi(), a.lo(), b.hi(), b.lo(), SDD.create());
            } else if ("square".equals(implementation)) {
                return (a, b) -> a.square();
            } else if ("accurateSquare".equals(implementation)) {
                return (a, b) -> DDExt.square(a);
            } else if ("multiplyDouble".equals(implementation)) {
                return (a, b) -> a.multiply(b.hi());
            } else if ("accurateMultiplyDouble".equals(implementation)) {
                return (a, b) -> DDExt.multiply(a, b.hi());
            } else if ("checkedMultiplyDouble".equals(implementation)) {
                // DD is always unchecked, use SDD
                return (a, b) -> SDD.multiply(a.hi(), a.lo(), b.hi(), SDD.create());
            } else if ("divide".equals(implementation)) {
                return DD::divide;
            } else if ("accurateDivide".equals(implementation)) {
                return DDExt::divide;
            } else if ("divideDouble".equals(implementation)) {
                return (a, b) -> a.divide(b.hi());
            } else if ("reciprocal".equals(implementation)) {
                return (a, b) -> a.reciprocal();
            } else if ("accurateReciprocal".equals(implementation)) {
                return (a, b) -> DDExt.reciprocal(a);
            } else if ("sqrt".equals(implementation)) {
                return (a, b) -> a.sqrt();
            } else if ("accurateSqrt".equals(implementation)) {
                return (a, b) -> DDExt.sqrt(a);
            } else {
                throw new IllegalStateException("unknown binary operator: " + implementation);
            }
        }
    }

    /**
     * Contains the data to computes the power function {@code (x, xx)^n}.
     */
    @State(Scope.Benchmark)
    public static class PowSample {
        // The parameters should be chosen such that the pow computation does not overflow

        /** The number of values. */
        @Param({"500"})
        private int values;
        /** The lower limit on n. */
        @Param({"-1022"})
        private int lower;
        /** The upper limit on n. */
        @Param({"1022"})
        private int upper;

        /** The data. */
        private DDInt[] data;

        /**
         * Gets the data.
         *
         * @return the data
         */
        public DDInt[] getData() {
            return data;
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            if (lower > upper) {
                throw new IllegalStateException(String.format("invalid range: %s to %s", lower, upper));
            }
            if (!Double.isFinite(Math.pow(0.5, lower))) {
                throw new IllegalStateException("lower overflow 0.5^" + lower);
            }
            if (!Double.isFinite(Math.pow(0.5, upper))) {
                throw new IllegalStateException("upper overflow 0.5^" + upper);
            }
            if (!Double.isFinite(Math.pow(2, lower))) {
                throw new IllegalStateException("lower overflow 2^" + lower);
            }
            if (!Double.isFinite(Math.pow(2, upper))) {
                throw new IllegalStateException("upper overflow 2^" + upper);
            }
            UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
            data = IntStream.range(0, values)
                            .mapToObj(i -> {
                                // DD in +/- [1, 2)
                                DD dd = makeSignedNormalDoubleDouble(rng);
                                if (rng.nextBoolean()) {
                                    // Change to +/- [0.5, 2)
                                    dd = dd.scalb(-1);
                                }
                                return new DDInt(dd, rng.nextInt(lower, upper));
                            }).toArray(DDInt[]::new);
        }
    }

    /**
     * Contains the scaled power operation.
     */
    @State(Scope.Benchmark)
    public static class PowMethod {
        /** The implementation of the function. */
        @Param({"pow", IMP_POW_SCALED, IMP_ACCURATE_POW_SCALED, IMP_SIMPLE_POW_SCALED})
        private String implementation;

        /** The function. */
        private DDIntFunction function;

        /**
         * Gets the function.
         *
         * @return the function
         */
        public DDIntFunction getFunction() {
            return function;
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            function = createFunction(implementation);
        }

        /**
         * Creates the function to evaluate the one-sided one-sample Kolmogorov-Smirnov distribution.
         *
         * @param implementation Function implementation
         * @return the function
         */
        static DDIntFunction createFunction(String implementation) {
            if ("pow".equals(implementation)) {
                return DD::pow;
            } else if (IMP_POW_SCALED.equals(implementation)) {
                final long[] exp = {0};
                return (x, n) -> x.pow(n, exp);
            } else if (IMP_ACCURATE_POW_SCALED.equals(implementation)) {
                final long[] exp = {0};
                return (x, n) -> DDMath.pow(x, n, exp);
            } else if (IMP_SIMPLE_POW_SCALED.equals(implementation)) {
                final long[] exp = {0};
                return (x, n) -> DDExt.simplePowScaled(x.hi(), x.lo(), n, exp);
            } else {
                throw new IllegalStateException("unknown pow: " + implementation);
            }
        }
    }

    /**
     * Contains the data to computes the scaled power function {@code (x, xx)^n}.
     * This method returns result with a separate scale and supports powers beyond the range
     * of a {@code double}.
     *
     * <p>Data have been taken from the test cases in the {@code o.a.c.statistics.ext} package
     * as these do not overflow BigDecimal using exponents in the range [50000000, 60000000).
     */
    @State(Scope.Benchmark)
    public static class PowScaledSample {
        /** The power exponent. This should be below the limit of BigDecimal (0 through 999999999).
         * Note that BigDecimal is too slow for larger powers for micro-benchmarking as run times
         * can be in seconds. */
        @Param({"1000", "10000"})
        private int n;
        /** The high part of the value. */
        @Param({"1.4146512942500389",
            //"1.4092258370859025"
            })
        private double x;

        /** The value. */
        private DD dd;

        /**
         * @return (x, xx)
         */
        DD getDD() {
            return dd;
        }

        /**
         * @return n
         */
        int getN() {
            return n;
        }

        /**
         * Create the data.
         */
        @Setup(Level.Iteration)
        public void setup() {
            // Create a random set of round-off bits.
            // The roundoff must be < 0.5 ULP of the value.
            // Generate using +/- [0.25, 0.5) ULP.
            final double xx = 0.25 * Math.ulp(x) * makeSignedNormalDouble(ThreadLocalRandom.current().nextLong());
            dd = DD.ofSum(x, xx);
        }
    }

    /**
     * Contains the scaled power operation.
     */
    @State(Scope.Benchmark)
    public static class PowScaledMethod {
        /** The implementation of the function. */
        @Param({IMP_POW_SCALED, IMP_ACCURATE_POW_SCALED, IMP_SIMPLE_POW_SCALED, "BigDecimal", "Dfp"})
        private String implementation;

        /** The function. */
        private DDIntFunction function;

        /**
         * Gets the function.
         *
         * @return the function
         */
        public DDIntFunction getFunction() {
            return function;
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            function = createFunction(implementation);
        }

        /**
         * Creates the function to evaluate the one-sided one-sample Kolmogorov-Smirnov distribution.
         *
         * @param implementation Function implementation
         * @return the function
         */
        static DDIntFunction createFunction(String implementation) {
            if (IMP_POW_SCALED.equals(implementation)) {
                final long[] exp = {0};
                return (x, n) -> x.pow(n, exp);
            } else if (IMP_ACCURATE_POW_SCALED.equals(implementation)) {
                final long[] exp = {0};
                return (x, n) -> DDMath.pow(x, n, exp);
            } else if (IMP_SIMPLE_POW_SCALED.equals(implementation)) {
                final long[] exp = {0};
                return (x, n) -> DDExt.simplePowScaled(x.hi(), x.lo(), n, exp);
            } else if ("BigDecimal".equals(implementation)) {
                return (x, n) -> new BigDecimal(x.hi()).add(new BigDecimal(x.lo())).pow(n, MathContext.DECIMAL128);
            } else if ("Dfp".equals(implementation)) {
                final DfpField df = new DfpField(MathContext.DECIMAL128.getPrecision());
                return (x, n) -> df.newDfp(x.hi()).add(x.lo()).pow(n);
            } else {
                throw new IllegalStateException("unknown pow scaled: " + implementation);
            }
        }
    }

    /**
     * Creates a signed double in the range {@code [-1, 1)}. The magnitude is sampled evenly from the
     * 2<sup>54</sup> dyadic rationals in the range.
     *
     * <p>Note: This method will not return samples for both -0.0 and 0.0.
     *
     * @param bits the bits
     * @return the double
     */
    private static double makeSignedDouble(long bits) {
        // Use the upper 54 bits on the assumption they are more random.
        // The sign bit is maintained by the signed shift.
        // The next 53 bits generates a magnitude in the range [0, 2^53) or [-2^53, 0).
        return (bits >> 10) * 0x1.0p-53d;
    }

    /**
     * Creates a normalized double in the range {@code [1, 2)} with a random sign. The
     * magnitude is sampled evenly from the 2<sup>52</sup> dyadic rationals in the range.
     *
     * @param bits Random bits.
     * @return the double
     */
    private static double makeSignedNormalDouble(long bits) {
        // Combine an unbiased exponent of 0 with the 52 bit mantissa and a random sign
        // bit
        return Double.longBitsToDouble((1023L << 52) | (bits >>> 12) | (bits << 63));
    }

    /**
     * Creates a double-double approximately uniformly distributed in the range {@code [-1, 1)}.
     *
     * @param rng Source of randomness.
     * @return the double-double
     */
    private static DD makeSignedDoubleDouble(UniformRandomProvider rng) {
        // Uniform in [-1, 1) using increments of 2^-53
        double x = makeSignedDouble(rng.nextLong());
        // The roundoff must be < 0.5 ULP of the value.
        // Generate using +/- [0.25, 0.5) ULP.
        final double xx = 0.25 * Math.ulp(x) * makeSignedNormalDouble(rng.nextLong());
        return DD.ofSum(x, xx);
    }

    /**
     * Creates a normalized double-double in the range {@code [1, 2)} with a random sign.
     *
     * @param rng Source of randomness.
     * @return the double-double
     */
    private static DD makeSignedNormalDoubleDouble(UniformRandomProvider rng) {
        final double x = makeSignedNormalDouble(rng.nextLong());
        // The roundoff must be < 0.5 ULP of the value.
        // Math.ulp(1.0) = 2^-52.
        // Generate using +/- [0.25, 0.5) ULP.
        final double xx = 0x1.0p-54 * makeSignedNormalDouble(rng.nextLong());
        return DD.ofSum(x, xx);
    }

    /**
     * Apply the function to all the numbers.
     *
     * @param fun Function.
     * @param data Data.
     * @param bh Data sink.
     */
    private static void apply(DoubleIntFunction fun, DoubleInt[] data, Blackhole bh) {
        for (final DoubleInt d : data) {
            bh.consume(fun.apply(d.getX(), d.getN()));
        }
    }

    /**
     * Apply the function to all the numbers.
     *
     * @param fun Function.
     * @param data Data.
     * @param data2 Second data.
     * @param bh Data sink.
     */
    private static void apply(BiFunction<DD, DD, Object> fun, DD[] data, DD[] data2, Blackhole bh) {
        for (int i = 0; i < data.length; i++) {
            bh.consume(fun.apply(data[i], data2[i]));
        }
    }

    /**
     * Apply the function to all the numbers.
     *
     * @param fun Function.
     * @param data Data.
     * @param bh Data sink.
     */
    private static void apply(DDIntFunction fun, DDInt[] data, Blackhole bh) {
        for (int i = 0; i < data.length; i++) {
            bh.consume(fun.apply(data[i].getX(), data[i].getN()));
        }
    }

    // Benchmark methods.
    // Benchmarks use function references to perform different operations on the numbers.

    /**
     * Benchmark a range of the KS function.
     *
     * @param method Test method.
     * @param data Test data.
     * @param bh Data sink.
     */
    @Benchmark
    public void ksRange(KSMethod method, KSData data, Blackhole bh) {
        apply(method.getFunction(), data.getData(), bh);
    }

    /**
     * Benchmark a sample of the KS function.
     *
     * @param method Test method.
     * @param data Test data.
     * @return the sample value
     */
    @Benchmark
    public double ksSample(KSMethod method, KSSample data) {
        return method.getFunction().apply(data.getX(), data.getN());
    }

    /**
     * Benchmark a sample of the KS function.
     *
     * @param method Test method.
     * @param data Test data.
     * @param bh Data sink.
     */
    @Benchmark
    public void binaryOperator(BinaryOperatorMethod method, OperatorData data, Blackhole bh) {
        apply(method.getFunction(), data.getData(), data.getData2(), bh);
    }

    /**
     * Benchmark a sample of the KS function.
     *
     * @param method Test method.
     * @param data Test data.
     * @param bh Data sink.
     */
    @Benchmark
    public void pow(PowMethod method, PowSample data, Blackhole bh) {
        apply(method.getFunction(), data.getData(), bh);
    }

    /**
     * Benchmark a sample of the KS function.
     *
     * @param method Test method.
     * @param data Test data.
     * @return the result
     */
    @Benchmark
    public Object powScaled(PowScaledMethod method, PowScaledSample data) {
        return method.getFunction().apply(data.getDD(), data.getN());
    }
}
