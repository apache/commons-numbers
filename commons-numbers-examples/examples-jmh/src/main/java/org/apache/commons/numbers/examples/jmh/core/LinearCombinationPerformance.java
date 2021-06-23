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
import java.util.function.IntFunction;

import org.apache.commons.numbers.core.Sum;
import org.apache.commons.numbers.examples.jmh.core.LinearCombination.FourD;
import org.apache.commons.numbers.examples.jmh.core.LinearCombination.ND;
import org.apache.commons.numbers.examples.jmh.core.LinearCombination.ThreeD;
import org.apache.commons.numbers.examples.jmh.core.LinearCombination.TwoD;
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
 * Executes a benchmark to measure the speed of operations in the {@link LinearCombination} class.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx512M"})
public class LinearCombinationPerformance {
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
     * The factors to multiply of a specific length.
     */
    @State(Scope.Benchmark)
    public static class LengthFactors extends Factors {
        /**
         * The length of each factors array.
         */
        @Param({"2", "3", "4", "8", "16", "32", "64"})
        private int length;

        /** {@inheritDoc} */
        @Override
        public int getLength() {
            return length;
        }
    }

    /**
     * The {@link LinearCombination} implementation.
     */
    @State(Scope.Benchmark)
    public static class Calculator {
        /**
         * The implementation name.
         */
        @Param({"standard",
                "current",
                "dekker",
                "dot2s",
                "dot2", "dot3", "dot4", "dot5", "dot6", "dot7",
                "exact",
                "extended", "extended2", "extended_exact", "extended_exact2",
                // Cached working double[] array.
                // Only faster when 'length' is >16. Below this the array
                // is small enough to be allocated locally
                // (Search for Thread Local Allocation Buffer (TLAB))
                "dot3c", "extendedc"})
        private String name;

        /** The 2D implementation. */
        private TwoD twod;
        /** The 3D implementation. */
        private ThreeD threed;
        /** The 4D implementation. */
        private FourD fourd;
        /** The ND implementation. */
        private ND nd;

        /**
         * @return the 2D implementation
         */
        public TwoD getTwoD() {
            return twod;
        }

        /**
         * @return the 3D implementation
         */
        public ThreeD getThreeD() {
            return threed;
        }

        /**
         * @return the 4D implementation
         */
        public FourD getFourD() {
            return fourd;
        }

        /**
         * @return the ND implementation
         */
        public ND getND() {
            return nd;
        }

        /**
         * Setup the implementation.
         */
        @Setup
        public void setup() {
            if ("current".endsWith(name)) {
                twod = (a1, b1, a2, b2) ->
                    Sum.create()
                        .addProduct(a1, b1)
                        .addProduct(a2, b2).getAsDouble();
                threed = (a1, b1, a2, b2, a3, b3) ->
                    Sum.create()
                        .addProduct(a1, b1)
                        .addProduct(a2, b2)
                        .addProduct(a3, b3).getAsDouble();
                fourd = (a1, b1, a2, b2, a3, b3, a4, b4) ->
                    Sum.create()
                        .addProduct(a1, b1)
                        .addProduct(a2, b2)
                        .addProduct(a3, b3)
                        .addProduct(a4, b4).getAsDouble();
                nd = (a, b) -> Sum.ofProducts(a, b).getAsDouble();
                return;
            }
            // All implementations below are expected to implement all the interfaces.
            if ("standard".endsWith(name)) {
                nd = LinearCombinations.StandardPrecision.INSTANCE;
            } else if ("dekker".equals(name)) {
                nd = LinearCombinations.Dekker.INSTANCE;
            } else if ("dot2s".equals(name)) {
                nd = LinearCombinations.Dot2s.INSTANCE;
            } else if ("dot2".equals(name)) {
                nd = new LinearCombinations.DotK(2);
            } else if ("dot3".equals(name)) {
                nd = LinearCombinations.DotK.DOT_3;
            } else if ("dot4".equals(name)) {
                nd = LinearCombinations.DotK.DOT_4;
            } else if ("dot5".equals(name)) {
                nd = LinearCombinations.DotK.DOT_5;
            } else if ("dot6".equals(name)) {
                nd = LinearCombinations.DotK.DOT_6;
            } else if ("dot7".equals(name)) {
                nd = LinearCombinations.DotK.DOT_7;
            } else if ("exact".equals(name)) {
                nd = LinearCombinations.Exact.INSTANCE;
            } else if ("extended".equals(name)) {
                nd = LinearCombinations.ExtendedPrecision.INSTANCE;
            } else if ("extended2".equals(name)) {
                nd = LinearCombinations.ExtendedPrecision.DOUBLE;
            } else if ("extended_exact".equals(name)) {
                nd = LinearCombinations.ExtendedPrecision.EXACT;
            } else if ("extended_exact2".equals(name)) {
                nd = LinearCombinations.ExtendedPrecision.EXACT2;
            } else if ("dot3c".equals(name)) {
                nd = new LinearCombinations.DotK(3, new CachedArrayFactory());
            } else if ("extendedc".equals(name)) {
                nd = LinearCombinations.ExtendedPrecision.of(
                        LinearCombinations.ExtendedPrecision.Summation.STANDARD, new CachedArrayFactory());
            } else {
                throw new IllegalStateException("Unknown implementation: " + name);
            }
            // Possible class-cast exception for partial implementations...
            twod = (TwoD) nd;
            threed = (ThreeD) nd;
            fourd = (FourD) nd;
        }
    }

    /**
     * Create or return a cached array.
     */
    static final class CachedArrayFactory implements IntFunction<double[]> {
        /** An empty double array. */
        private static final double[] EMPTY = new double[0];

        /** The cached array. */
        private double[] array = EMPTY;

        @Override
        public double[] apply(int value) {
            double[] a = array;
            if (a.length < value) {
                array = a = new double[value];
            }
            return a;
        }
    }

    /**
     * Compute the 2D scalar product for all the factors.
     *
     * @param factors Factors.
     * @param bh Data sink.
     * @param calc Scalar product calculator.
     */
    @Benchmark
    public void twoD(Factors factors, Blackhole bh, Calculator calc) {
        final TwoD fun = calc.getTwoD();
        for (int i = 0; i < factors.getSize(); i++) {
            final double[] a = factors.getA(i);
            final double[] b = factors.getB(i);
            bh.consume(fun.value(a[0], b[0], a[1], b[1]));
        }
    }

    /**
     * Compute the 3D scalar product for all the factors.
     *
     * @param factors Factors.
     * @param bh Data sink.
     * @param calc Scalar product calculator.
     */
    @Benchmark
    public void threeD(Factors factors, Blackhole bh, Calculator calc) {
        final ThreeD fun = calc.getThreeD();
        for (int i = 0; i < factors.getSize(); i++) {
            final double[] a = factors.getA(i);
            final double[] b = factors.getB(i);
            bh.consume(fun.value(a[0], b[0], a[1], b[1], a[2], b[2]));
        }
    }

    /**
     * Compute the 4D scalar product for all the factors.
     *
     * @param factors Factors.
     * @param bh Data sink.
     * @param calc Scalar product calculator.
     */
    @Benchmark
    public void fourD(Factors factors, Blackhole bh, Calculator calc) {
        final FourD fun = calc.getFourD();
        for (int i = 0; i < factors.getSize(); i++) {
            final double[] a = factors.getA(i);
            final double[] b = factors.getB(i);
            bh.consume(fun.value(a[0], b[0], a[1], b[1], a[2], b[2], a[3], b[3]));
        }
    }

    /**
     * Compute the ND scalar product for all the factors.
     *
     * @param factors Factors.
     * @param bh Data sink.
     * @param calc Scalar product calculator.
     */
    @Benchmark
    public void nD(LengthFactors factors, Blackhole bh, Calculator calc) {
        final ND fun = calc.getND();
        for (int i = 0; i < factors.getSize(); i++) {
            // These should be pre-computed to the correct length
            final double[] a = factors.getA(i);
            final double[] b = factors.getB(i);
            bh.consume(fun.value(a, b));
        }
    }
}
