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

package org.apache.commons.numbers.examples.jmh.gamma;

import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import org.apache.commons.numbers.core.Precision;
import org.apache.commons.numbers.fraction.ContinuedFraction;
import org.apache.commons.numbers.gamma.Erf;
import org.apache.commons.numbers.gamma.Erfc;
import org.apache.commons.numbers.gamma.InverseErf;
import org.apache.commons.numbers.gamma.InverseErfc;
import org.apache.commons.numbers.gamma.LogGamma;
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
 * Executes a benchmark to estimate the speed of error function operations.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx512M"})
public class ErfPerformance {
    /** The threshold value for returning the extreme value. */
    private static final double EXTREME_VALUE_BOUND = 40;
    /** Commons Numbers 1.0 implementation. */
    private static final String IMP_NUMBERS_1_0 = "Numbers 1.0";
    /** Commons Numbers 1.1 implementation. */
    private static final String IMP_NUMBERS_1_1 = "Boost";
    /** Uniform numbers in the appropriate domain of the function. */
    private static final String NUM_UNIFORM = "uniform";
    /** Uniform numbers in the domain of the error function result, [1, 1] or [0, 2]. */
    private static final String NUM_INVERSE_UNIFORM = "inverse uniform";
    /** Message prefix for an unknown parameter. */
    private static final String UNKNOWN = "unknown parameter: ";
    /** Message prefix for a erf domain error. */
    private static final String ERF_DOMAIN_ERROR = "erf domain error: ";
    /** Message prefix for a erf domain error. */
    private static final String ERFC_DOMAIN_ERROR = "erfc domain error: ";

    /**
     * The seed for random number generation. Ensures the same numbers are generated
     * for each implementation of the function.
     *
     * <p>Note: Numbers will not be the same for the error function and complementary error
     * function. The domain is shifted from [-1, 1] to [0, 2]. However number generation
     * may wish to target specific regions of the domain where there is limited precision
     * as {@code |x| -> 1} or high precision as {@code |x| -> 0}.
     */
    private static final long SEED = ThreadLocalRandom.current().nextLong();

    /**
     * Contains an array of numbers.
     */
    public abstract static class NumberData {
        /** The size of the data. */
        @Param({"1000"})
        private int size;

        /** The numbers. */
        private double[] numbers;

        /**
         * Gets the size of the array of numbers.
         *
         * @return the size
         */
        public int getSize() {
            return size;
        }

        /**
         * Gets the numbers.
         *
         * @return the numbers
         */
        public double[] getNumbers() {
            return numbers;
        }

        /**
         * Create the numbers.
         */
        @Setup
        public void setup() {
            numbers = createNumbers(new SplittableRandom(SEED));
        }

        /**
         * Creates the numbers.
         *
         * @param rng Random number generator.
         * @return the random numbers
         * @see #getSize()
         */
        protected abstract double[] createNumbers(SplittableRandom rng);
    }

    /**
     * Contains an array of numbers. This is used to test the JMH overhead for calling
     * a function on each number in the array.
     */
    @State(Scope.Benchmark)
    public static class BaseData extends NumberData {
        /** {@inheritDoc} */
        @Override
        protected double[] createNumbers(SplittableRandom rng) {
            return rng.doubles().limit(getSize()).toArray();
        }
    }

    /**
     * Contains an array of numbers and the method to compute the error function.
     */
    public abstract static class FunctionData extends NumberData {

        /** The function. */
        private DoubleUnaryOperator function;

        /**
         * The implementation of the function.
         */
        @Param({IMP_NUMBERS_1_0, IMP_NUMBERS_1_1})
        private String implementation;

        /**
         * Gets the implementation.
         *
         * @return the implementation
         */
        public String getImplementation() {
            return implementation;
        }

        /**
         * Gets the function.
         *
         * @return the function
         */
        public DoubleUnaryOperator getFunction() {
            return function;
        }

        /**
         * Create the numbers and the function.
         */
        @Override
        @Setup
        public void setup() {
            super.setup();
            function = createFunction();
            verify();
        }

        /**
         * Creates the function from the implementation name.
         *
         * @return the inverse error function
         * @see #getImplementation()
         */
        protected abstract DoubleUnaryOperator createFunction();

        /**
         * Verify the numbers for the function. This is called after the numbers and function
         * have been created.
         *
         * @see #getNumbers()
         * @see #getFunction()
         */
        protected abstract void verify();
    }

    /**
     * Contains an array of numbers in the range for the error function.
     */
    @State(Scope.Benchmark)
    public static class ErfData extends FunctionData {
        /** The type of the data. */
        @Param({NUM_UNIFORM, NUM_INVERSE_UNIFORM})
        private String type;

        /** {@inheritDoc} */
        @Override
        protected double[] createNumbers(SplittableRandom rng) {
            DoubleSupplier generator;
            if (NUM_INVERSE_UNIFORM.equals(type)) {
                // p range: [-1, 1)
                // The final value is generated using the inverse erf function.
                generator = () -> InverseErf.value(makeSignedDouble(rng));
            } else if (NUM_UNIFORM.equals(type)) {
                // range [-6, 6)
                // Note: Values are not distinguishable from +/-1 when |x| > 6
                generator = () -> makeSignedDouble(rng) * 6;
            } else {
                throw new IllegalStateException(UNKNOWN + type);
            }
            return DoubleStream.generate(generator).limit(getSize()).toArray();
        }

        /** {@inheritDoc} */
        @Override
        protected DoubleUnaryOperator createFunction() {
            final String impl = getImplementation();
            if (IMP_NUMBERS_1_0.equals(impl)) {
                return ErfPerformance::erf;
            } else if (IMP_NUMBERS_1_1.equals(impl)) {
                return Erf::value;
            } else {
                throw new IllegalStateException(UNKNOWN + impl);
            }
        }

        /** {@inheritDoc} */
        @Override
        protected void verify() {
            final DoubleUnaryOperator function = getFunction();
            final double relativeEps = 1e-6;
            for (final double x : getNumbers()) {
                final double p = function.applyAsDouble(x);
                assert -1 <= p & p <= 1 : ERF_DOMAIN_ERROR + p;

                // Implementations may not compute a round-trip
                // to a suitable accuracy as:
                // |p| -> 0 : x -> 0
                // |p| -> 1 : x -> +/-big
                if (p < 1e-10 || Math.abs(p - 1) < 1e-10) {
                    continue;
                }
                assertEquals(x, InverseErf.value(p), Math.abs(x) * relativeEps,
                    () -> getImplementation() + " inverse erf " + p);
            }
        }
    }

    /**
     * Contains an array of numbers in the range for the complementary error function.
     */
    @State(Scope.Benchmark)
    public static class ErfcData extends FunctionData {
        /** The type of the data. */
        @Param({NUM_UNIFORM, NUM_INVERSE_UNIFORM})
        private String type;

        /** {@inheritDoc} */
        @Override
        protected double[] createNumbers(SplittableRandom rng) {
            DoubleSupplier generator;
            if (NUM_INVERSE_UNIFORM.equals(type)) {
                // q range: [0, 2)
                // The final value is generated using the inverse erfc function.
                generator = () -> InverseErfc.value(rng.nextDouble() * 2);
            } else if (NUM_UNIFORM.equals(type)) {
                // range [-6, 28)
                // Note: Values are not distinguishable from 2 when x < -6
                // Shift the range [-17, 17) to [-6, 28)
                generator = () -> makeSignedDouble(rng) * 17 + 11;
            } else {
                throw new IllegalStateException(UNKNOWN + type);
            }
            return DoubleStream.generate(generator).limit(getSize()).toArray();
        }

        /** {@inheritDoc} */
        @Override
        protected DoubleUnaryOperator createFunction() {
            final String impl = getImplementation();
            if (IMP_NUMBERS_1_0.equals(impl)) {
                return ErfPerformance::erfc;
            } else if (IMP_NUMBERS_1_1.equals(impl)) {
                return Erfc::value;
            } else {
                throw new IllegalStateException(UNKNOWN + impl);
            }
        }

        /** {@inheritDoc} */
        @Override
        protected void verify() {
            final DoubleUnaryOperator function = getFunction();
            final double relativeEps = 1e-6;
            for (final double x : getNumbers()) {
                final double q = function.applyAsDouble(x);
                assert 0 <= q && q <= 2 : ERFC_DOMAIN_ERROR + q;

                // Implementations may not compute a round-trip
                // to a suitable accuracy as:
                // q -> 0 : x -> big
                // |q| -> 1 : x -> 0
                // q -> 2 : x -> -big
                if (q < 1e-10 || Math.abs(q - 1) < 1e-10 || q > 2 - 1e-10) {
                    continue;
                }
                assertEquals(x, InverseErfc.value(q), Math.abs(x) * relativeEps,
                    () -> getImplementation() + " inverse erfc " + q);
            }
        }
    }

    /**
     * Contains an array of numbers in the range [-1, 1] for the inverse error function.
     */
    @State(Scope.Benchmark)
    public static class InverseErfData extends FunctionData {
        /**
         * The type of the data.
         */
        @Param({NUM_UNIFORM})
        private String type;

        /** {@inheritDoc} */
        @Override
        protected double[] createNumbers(SplittableRandom rng) {
            DoubleSupplier generator;
            if (NUM_UNIFORM.equals(type)) {
                // range [-1, 1)
                generator = () -> makeSignedDouble(rng);
            } else {
                throw new IllegalStateException(UNKNOWN + type);
            }
            return DoubleStream.generate(generator).limit(getSize()).toArray();
        }

        /** {@inheritDoc} */
        @Override
        protected DoubleUnaryOperator createFunction() {
            final String impl = getImplementation();
            if (IMP_NUMBERS_1_0.equals(impl)) {
                return ErfPerformance::inverseErf;
            } else if (IMP_NUMBERS_1_1.equals(impl)) {
                return InverseErf::value;
            } else {
                throw new IllegalStateException(UNKNOWN + impl);
            }
        }

        /** {@inheritDoc} */
        @Override
        protected void verify() {
            final DoubleUnaryOperator function = getFunction();
            final double relativeEps = 1e-12;
            for (final double x : getNumbers()) {
                assert -1 <= x && x <= 1 : ERF_DOMAIN_ERROR + x;

                // Implementations may not compute a round-trip
                // to a suitable accuracy as:
                // |x| -> 0 : t -> 0
                // |x| -> 1 : t -> +/-big
                if (x < 1e-10 || Math.abs(x - 1) < 1e-10) {
                    continue;
                }
                final double t = function.applyAsDouble(x);
                assertEquals(x, Erf.value(t), Math.abs(x) * relativeEps,
                    () -> getImplementation() + " erf " + t);
            }
        }
    }

    /**
     * Contains an array of numbers in the range [0, 2] for the inverse complementary error function.
     */
    @State(Scope.Benchmark)
    public static class InverseErfcData extends FunctionData {
        /**
         * The type of the data.
         */
        @Param({NUM_UNIFORM})
        private String type;

        /** {@inheritDoc} */
        @Override
        protected double[] createNumbers(SplittableRandom rng) {
            DoubleSupplier generator;
            if (NUM_UNIFORM.equals(type)) {
                // range [0, 2)
                generator = () -> rng.nextDouble() * 2;
            } else {
                throw new IllegalStateException(UNKNOWN + type);
            }
            return DoubleStream.generate(generator).limit(getSize()).toArray();
        }

        /** {@inheritDoc} */
        @Override
        protected DoubleUnaryOperator createFunction() {
            final String impl = getImplementation();
            if (IMP_NUMBERS_1_0.equals(impl)) {
                return ErfPerformance::inverseErfc;
            } else if (IMP_NUMBERS_1_1.equals(impl)) {
                return InverseErfc::value;
            } else {
                throw new IllegalStateException(UNKNOWN + impl);
            }
        }

        /** {@inheritDoc} */
        @Override
        protected void verify() {
            final DoubleUnaryOperator function = getFunction();
            final double relativeEps = 1e-12;
            for (final double x : getNumbers()) {
                assert 0 <= x && x <= 2 : ERFC_DOMAIN_ERROR + x;

                // Implementations may not compute a round-trip
                // to a suitable accuracy as:
                // x -> 0 : t -> big
                // |x| -> 1 : t -> 0
                // x -> 2 : t -> -big
                if (x < 1e-10 || Math.abs(x - 1) < 1e-10 || x > 2 - 1e-10) {
                    continue;
                }
                final double t = function.applyAsDouble(x);
                assertEquals(x, Erfc.value(t), Math.abs(x) * relativeEps,
                    () -> getImplementation() + " erfc " + t);
            }
        }
    }

    /**
     * Make a signed double in the range [-1, 1).
     *
     * @param rng Random generator
     * @return u in [-1, 1)
     */
    private static double makeSignedDouble(SplittableRandom rng) {
        // As per o.a.c.rng.core.utils.NumberFactory.makeDouble(long) but using a signed
        // shift of 10 in place of an unsigned shift of 11.
        // Use the upper 54 bits on the assumption they are more random.
        // The sign bit is maintained by the signed shift.
        // The next 53 bits generates a magnitude in the range [0, 2^53) or [-2^53, 0).
        return (rng.nextLong() >> 10) * 0x1.0p-53;
    }

    /**
     * Returns the inverse complementary error function.
     *
     * <p>This is the implementation in Commons Numbers 1.0.
     *
     * @param x Value in [0, 2].
     * @return t such that {@code x = erfc(t)}
     */
    private static double inverseErfc(final double x) {
        return inverseErf(1 - x);
    }

    /**
     * Returns the inverse error function.
     *
     * <p>This implementation is described in the paper:
     * <a href="http://people.maths.ox.ac.uk/gilesm/files/gems_erfinv.pdf">Approximating
     * the erfinv function</a> by Mike Giles, Oxford-Man Institute of Quantitative Finance,
     * which was published in GPU Computing Gems, volume 2, 2010.
     * The source code is available <a href="http://gpucomputing.net/?q=node/1828">here</a>.
     * </p>
     *
     * <p>This is the implementation in Commons Numbers 1.0.
     *
     * @param x Value in [-1, 1].
     * @return t such that {@code x = erf(t)}
     */
    private static double inverseErf(final double x) {
        // Beware that the logarithm argument must be
        // computed as (1 - x) * (1 + x),
        // it must NOT be simplified as 1 - x * x as this
        // would induce rounding errors near the boundaries +/-1
        double w = -Math.log((1 - x) * (1 + x));
        double p;

        if (w < 6.25) {
            w -= 3.125;
            p =  -3.6444120640178196996e-21;
            p =   -1.685059138182016589e-19 + p * w;
            p =   1.2858480715256400167e-18 + p * w;
            p =    1.115787767802518096e-17 + p * w;
            p =   -1.333171662854620906e-16 + p * w;
            p =   2.0972767875968561637e-17 + p * w;
            p =   6.6376381343583238325e-15 + p * w;
            p =  -4.0545662729752068639e-14 + p * w;
            p =  -8.1519341976054721522e-14 + p * w;
            p =   2.6335093153082322977e-12 + p * w;
            p =  -1.2975133253453532498e-11 + p * w;
            p =  -5.4154120542946279317e-11 + p * w;
            p =    1.051212273321532285e-09 + p * w;
            p =  -4.1126339803469836976e-09 + p * w;
            p =  -2.9070369957882005086e-08 + p * w;
            p =   4.2347877827932403518e-07 + p * w;
            p =  -1.3654692000834678645e-06 + p * w;
            p =  -1.3882523362786468719e-05 + p * w;
            p =    0.0001867342080340571352 + p * w;
            p =  -0.00074070253416626697512 + p * w;
            p =   -0.0060336708714301490533 + p * w;
            p =      0.24015818242558961693 + p * w;
            p =       1.6536545626831027356 + p * w;
        } else if (w < 16.0) {
            w = Math.sqrt(w) - 3.25;
            p =   2.2137376921775787049e-09;
            p =   9.0756561938885390979e-08 + p * w;
            p =  -2.7517406297064545428e-07 + p * w;
            p =   1.8239629214389227755e-08 + p * w;
            p =   1.5027403968909827627e-06 + p * w;
            p =   -4.013867526981545969e-06 + p * w;
            p =   2.9234449089955446044e-06 + p * w;
            p =   1.2475304481671778723e-05 + p * w;
            p =  -4.7318229009055733981e-05 + p * w;
            p =   6.8284851459573175448e-05 + p * w;
            p =   2.4031110387097893999e-05 + p * w;
            p =   -0.0003550375203628474796 + p * w;
            p =   0.00095328937973738049703 + p * w;
            p =   -0.0016882755560235047313 + p * w;
            p =    0.0024914420961078508066 + p * w;
            p =   -0.0037512085075692412107 + p * w;
            p =     0.005370914553590063617 + p * w;
            p =       1.0052589676941592334 + p * w;
            p =       3.0838856104922207635 + p * w;
        } else if (w < Double.POSITIVE_INFINITY) {
            w = Math.sqrt(w) - 5;
            p =  -2.7109920616438573243e-11;
            p =  -2.5556418169965252055e-10 + p * w;
            p =   1.5076572693500548083e-09 + p * w;
            p =  -3.7894654401267369937e-09 + p * w;
            p =   7.6157012080783393804e-09 + p * w;
            p =  -1.4960026627149240478e-08 + p * w;
            p =   2.9147953450901080826e-08 + p * w;
            p =  -6.7711997758452339498e-08 + p * w;
            p =   2.2900482228026654717e-07 + p * w;
            p =  -9.9298272942317002539e-07 + p * w;
            p =   4.5260625972231537039e-06 + p * w;
            p =  -1.9681778105531670567e-05 + p * w;
            p =   7.5995277030017761139e-05 + p * w;
            p =  -0.00021503011930044477347 + p * w;
            p =  -0.00013871931833623122026 + p * w;
            p =       1.0103004648645343977 + p * w;
            p =       4.8499064014085844221 + p * w;
        } else if (w == Double.POSITIVE_INFINITY) {
            // this branch does not appears in the original code, it
            // was added because the previous branch does not handle
            // x = +/-1 correctly. In this case, w is positive infinity
            // and as the first coefficient (-2.71e-11) is negative.
            // Once the first multiplication is done, p becomes negative
            // infinity and remains so throughout the polynomial evaluation.
            // So the branch above incorrectly returns negative infinity
            // instead of the correct positive infinity.
            p = Double.POSITIVE_INFINITY;
        } else {
            // this branch does not appears in the original code, it
            // occurs when the input is NaN or not in the range [-1, 1].
            return Double.NaN;
        }

        return p * x;
    }

    /**
     * Returns the complementary error function.
     *
     * <p>This implementation computes erfc(x) using the
     * {@link RegularizedGamma.Q#value(double, double, double, int) regularized gamma function},
     * following <a href="http://mathworld.wolfram.com/Erf.html">Erf</a>, equation (3).
     *
     * <p>This is the implementation in Commons Numbers 1.0.
     *
     * @param x Value in [0, 2].
     * @return t such that {@code x = erfc(t)}
     */
    private static double erfc(final double x) {
        if (Math.abs(x) > EXTREME_VALUE_BOUND) {
            return x > 0 ? 0 : 2;
        }
        final double ret = RegularizedGamma.Q.value(0.5, x * x, 1e-15, 10000);
        return x < 0 ? 2 - ret : ret;
    }

    /**
     * Returns the error function.
     *
     * <p>This implementation computes erf(x) using the
     * {@link RegularizedGamma.P#value(double, double, double, int) regularized gamma function},
     * following <a href="http://mathworld.wolfram.com/Erf.html"> Erf</a>, equation (3)
     *
     * <p>This is the implementation in Commons Numbers 1.0.
     *
     * @param x Value in [-1, 1].
     * @return t such that {@code x = erf(t)}
     */
    private static double erf(final double x) {
        if (Math.abs(x) > EXTREME_VALUE_BOUND) {
            return x > 0 ? 1 : -1;
        }
        final double ret = RegularizedGamma.P.value(0.5, x * x, 1e-15, 10000);
        return x < 0 ? -ret : ret;
    }

    /**
     * <a href="http://mathworld.wolfram.com/RegularizedGammaFunction.html">
     * Regularized Gamma functions</a>.
     *
     * <p>This is the Commons Numbers 1.0 implementation. Later versions of
     * RegularizedGamma changed to compute using more than the continued fraction
     * representation (Q) or lower gamma series representation (P).
     *
     * <p>The ContinuedFraction and LogGamma class use the current version
     * and are not preserved from Commons Numbers 1.0.
     */
    private static final class RegularizedGamma {
        /** Private constructor. */
        private RegularizedGamma() {
            // intentionally empty.
        }

        /**
         * \( P(a, x) \) <a href="http://mathworld.wolfram.com/RegularizedGammaFunction.html">
         * regularized Gamma function</a>.
         *
         * Class is immutable.
         */
        static final class P {
            /** Prevent instantiation. */
            private P() {}

            /**
             * Computes the regularized gamma function \( P(a, x) \).
             *
             * The implementation of this method is based on:
             * <ul>
             *  <li>
             *   <a href="http://mathworld.wolfram.com/RegularizedGammaFunction.html">
             *   Regularized Gamma Function</a>, equation (1)
             *  </li>
             *  <li>
             *   <a href="http://mathworld.wolfram.com/IncompleteGammaFunction.html">
             *   Incomplete Gamma Function</a>, equation (4).
             *  </li>
             *  <li>
             *   <a href="http://mathworld.wolfram.com/ConfluentHypergeometricFunctionoftheFirstKind.html">
             *   Confluent Hypergeometric Function of the First Kind</a>, equation (1).
             *  </li>
             * </ul>
             *
             * @param a Argument.
             * @param x Argument.
             * @param epsilon Tolerance in continued fraction evaluation.
             * @param maxIterations Maximum number of iterations in continued fraction evaluation.
             * @return \( P(a, x) \).
             * @throws ArithmeticException if the continued fraction fails to converge.
             */
            static double value(double a,
                                double x,
                                double epsilon,
                                int maxIterations) {
                if (Double.isNaN(a) ||
                    Double.isNaN(x) ||
                    a <= 0 ||
                    x < 0) {
                    return Double.NaN;
                } else if (x == 0) {
                    return 0;
                } else if (x >= a + 1) {
                    // Q should converge faster in this case.
                    return 1 - RegularizedGamma.Q.value(a, x, epsilon, maxIterations);
                } else {
                    // Series.
                    double n = 0; // current element index
                    double an = 1 / a; // n-th element in the series
                    double sum = an; // partial sum
                    while (Math.abs(an / sum) > epsilon &&
                           n < maxIterations &&
                           sum < Double.POSITIVE_INFINITY) {
                        // compute next element in the series
                        n += 1;
                        an *= x / (a + n);

                        // update partial sum
                        sum += an;
                    }
                    if (n >= maxIterations) {
                        throw new ArithmeticException("Max iterations exceeded: " + maxIterations);
                    } else if (Double.isInfinite(sum)) {
                        return 1;
                    } else {
                        // Ensure result is in the range [0, 1]
                        final double result = Math.exp(-x + (a * Math.log(x)) - LogGamma.value(a)) * sum;
                        return result > 1.0 ? 1.0 : result;
                    }
                }
            }
        }

        /**
         * Creates the \( Q(a, x) \equiv 1 - P(a, x) \) <a href="http://mathworld.wolfram.com/RegularizedGammaFunction.html">
         * regularized Gamma function</a>.
         *
         * Class is immutable.
         */
        static final class Q {
            /** Prevent instantiation. */
            private Q() {}

            /**
             * Computes the regularized gamma function \( Q(a, x) = 1 - P(a, x) \).
             *
             * The implementation of this method is based on:
             * <ul>
             *  <li>
             *   <a href="http://mathworld.wolfram.com/RegularizedGammaFunction.html">
             *   Regularized Gamma Function</a>, equation (1).
             *  </li>
             *  <li>
             *   <a href="http://functions.wolfram.com/GammaBetaErf/GammaRegularized/10/0003/">
             *   Regularized incomplete gamma function: Continued fraction representations
             *   (formula 06.08.10.0003)</a>
             *  </li>
             * </ul>
             *
             * @param a Argument.
             * @param x Argument.
             * @param epsilon Tolerance in continued fraction evaluation.
             * @param maxIterations Maximum number of iterations in continued fraction evaluation.
             * @throws ArithmeticException if the continued fraction fails to converge.
             * @return \( Q(a, x) \).
             */
            static double value(final double a,
                                double x,
                                double epsilon,
                                int maxIterations) {
                if (Double.isNaN(a) ||
                    Double.isNaN(x) ||
                    a <= 0 ||
                    x < 0) {
                    return Double.NaN;
                } else if (x == 0) {
                    return 1;
                } else if (x < a + 1) {
                    // P should converge faster in this case.
                    return 1 - RegularizedGamma.P.value(a, x, epsilon, maxIterations);
                } else {
                    final ContinuedFraction cf = new ContinuedFraction() {
                            /** {@inheritDoc} */
                            @Override
                            protected double getA(int n, double x) {
                                return n * (a - n);
                            }

                            /** {@inheritDoc} */
                            @Override
                            protected double getB(int n, double x) {
                                return ((2 * n) + 1) - a + x;
                            }
                        };

                    return Math.exp(-x + (a * Math.log(x)) - LogGamma.value(a)) /
                        cf.evaluate(x, epsilon, maxIterations);
                }
            }
        }
    }

    /**
     * Assert the values are equal to the given epsilon, else throw an AssertionError.
     *
     * @param x the x
     * @param y the y
     * @param eps the max epsilon for equality
     * @param msg the message upon failure
     */
    static void assertEquals(double x, double y, double eps, Supplier<String> msg) {
        if (!Precision.equalsIncludingNaN(x, y, eps)) {
            throw new AssertionError(msg.get() + ": " + x + " != " + y);
        }
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
     * Identity function. This can be used to measure the JMH overhead of calling a function
     * on an array of numbers.
     *
     * @param z Number.
     * @return the number
     */
    private static double identity(double z) {
        return z;
    }

    // Benchmark methods.
    // Benchmarks use function references to perform different operations on the numbers.

    /**
     * Baseline the JMH overhead for all the benchmarks that evaluate a function of
     * an array of numbers. All other methods are expected to be slower than this.
     *
     * @param numbers Numbers.
     * @param bh Data sink.
     */
    @Benchmark
    public void baseline(BaseData numbers, Blackhole bh) {
        apply(numbers.getNumbers(), ErfPerformance::identity, bh);
    }

    /**
     * Benchmark the error function.
     *
     * @param data Test data.
     * @param bh Data sink.
     */
    @Benchmark
    public void erf(ErfData data, Blackhole bh) {
        apply(data.getNumbers(), data.getFunction(), bh);
    }

    /**
     * Benchmark the complementary error function.
     *
     * @param data Test data.
     * @param bh Data sink.
     */
    @Benchmark
    public void erfc(ErfcData data, Blackhole bh) {
        apply(data.getNumbers(), data.getFunction(), bh);
    }

    /**
     * Benchmark the inverse error function.
     *
     * @param data Test data.
     * @param bh Data sink.
     */
    @Benchmark
    public void inverseErf(InverseErfData data, Blackhole bh) {
        apply(data.getNumbers(), data.getFunction(), bh);
    }

    /**
     * Benchmark the inverse complementary error function.
     *
     * @param data Test data.
     * @param bh Data sink.
     */
    @Benchmark
    public void inverseErfc(InverseErfcData data, Blackhole bh) {
        apply(data.getNumbers(), data.getFunction(), bh);
    }
}
