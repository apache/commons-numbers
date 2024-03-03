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

import org.apache.commons.numbers.core.ArithmeticUtils;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;

/**
 * A benchmark that compares the performance of different GCD implementations.
 */
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx512M"})
public class GcdPerformance {
    /**
     * Provides random ints for benchmarking.
     */
    @State(Scope.Benchmark)
    public static class Ints {
        /**
         * The random seed to use for number generation.
         */
        @Param("42")
        private long seed;
        /**
         * The number of number pairs to generate.
         */
        @Param("100000")
        private int numPairs;

        /**
         * Generated values to be consumed by the benchmark.
         */
        private int[] values;

        /**
         * JMH setup method to generate the data.
         */
        @Setup(Level.Iteration)
        public void setup() {
            values = getRandomProvider(seed).ints()
                    .filter(i -> i != Integer.MIN_VALUE).
                    limit(numPairs * 2)
                    .toArray();

            seed = (((long) values[0]) << Integer.SIZE) | values[1];
        }
    }

    /**
     * Provides random longs for benchmarking.
     */
    @State(Scope.Benchmark)
    public static class Longs {
        /**
         * The random seed to use for number generation.
         */
        @Param("42")
        private long seed;

        /**
         * The number of number pairs to generate.
         */
        @Param("100000")
        private int numPairs;

        /**
         * Generated values to be consumed by the benchmark.
         */
        private long[] values;

        /**
         * JMH setup method to generate the data.
         */
        @Setup(Level.Iteration)
        public void setup() {
            values = getRandomProvider(seed).longs()
                    .filter(i -> i != Long.MIN_VALUE)
                    .limit(numPairs * 2)
                    .toArray();

            seed = values[0];
        }
    }

    /**
     * Returns the random provider used to generate data for the benchmarks.
     *
     * @param seed the seed for the random source.
     * @return a random provider.
     */
    private static RestorableUniformRandomProvider getRandomProvider(long seed) {
        return RandomSource.XO_RO_SHI_RO_128_PP.create(seed);
    }

    /**
     * Benchmarks the current GCD implementation for ints.
     *
     * @param ints data to consume.
     * @param blackhole a data sink to avoid JIT interfering with our benchmark.
     */
    @Benchmark
    public void gcdInt(Ints ints, Blackhole blackhole) {
        calcAndConsumeGcds(ints, blackhole, ArithmeticUtils::gcd);
    }

    /**
     * Benchmarks the old GCD implementation for ints that has been copied into this class.
     *
     * @param ints data to consume.
     * @param blackhole a data sink to avoid JIT interfering with the benchmark.
     */
    @Benchmark
    public void oldGcdInt(Ints ints, Blackhole blackhole) {
        calcAndConsumeGcds(ints, blackhole, GcdPerformance::oldGcdInt);
    }

    /**
     * Benchmarks the current GCD implementation for longs.
     *
     * @param longs data to consume.
     * @param blackhole a data sink to avoid JIT interfering with the benchmark.
     */
    @Benchmark
    public void gcdLong(Longs longs, Blackhole blackhole) {
        calcAndConsumeGcds(longs, blackhole, ArithmeticUtils::gcd);
    }

    /**
     * Benchmarks the old GCD implementation for longs that has been copied into this class.
     *
     * @param longs data to consume.
     * @param blackhole a data sink to avoid JIT interfering with the benchmark.
     */
    @Benchmark
    public void oldGcdLong(Longs longs, Blackhole blackhole) {
        calcAndConsumeGcds(longs, blackhole, GcdPerformance::oldGcdLong);
    }

    /**
     * Benchmarks the old GCD implementation for ints, but adapted for longs, that has been copied into this class.
     *
     * @param longs data to consume.
     * @param blackhole a data sink to avoid JIT interfering with the benchmark.
     */
    @Benchmark
    public void oldGcdIntAdaptedForLong(Longs longs, Blackhole blackhole) {
        calcAndConsumeGcds(longs, blackhole, GcdPerformance::oldGcdIntAdaptedForLong);
    }

    /**
     * Benchmarks the old GCD implementation of {@link BigInteger} to have a baseline.
     *
     * @param longs data to consume.
     * @param blackhole a data sink to avoid JIT interfering with the benchmark.
     */
    @Benchmark
    public void gcdBigInteger(Longs longs, Blackhole blackhole) {
        calcAndConsumeGcds(longs, blackhole, GcdPerformance::gcdBigInteger);
    }

    /**
     * Calculates and consumes GCDs using the given implementation for benchmarking.
     *
     * @param longs the values to consume.
     * @param blackhole a data sink to avoid JIT interfering with the benchmark.
     * @param gcdImpl the GCD implementation to benchmark.
     */
    private void calcAndConsumeGcds(Longs longs, Blackhole blackhole, LongBinaryOperator gcdImpl) {
        long[] values = longs.values;
        for (int i = 0; i < values.length; i += 2) {
            blackhole.consume(gcdImpl.applyAsLong(values[i], values[i + 1]));
        }
    }

    /**
     * Calculates and consumes GCDs using the given implementation for benchmarking.
     *
     * @param ints the values to consume.
     * @param blackhole a data sink to avoid JIT interfering with the benchmark.
     * @param gcdImpl the GCD implementation to benchmark.
     */
    private void calcAndConsumeGcds(Ints ints, Blackhole blackhole, IntBinaryOperator gcdImpl) {
        int[] values = ints.values;
        for (int i = 0; i < values.length; i += 2) {
            blackhole.consume(gcdImpl.applyAsInt(values[i], values[i + 1]));
        }
    }

    /**
     * Calculates the GCD by converting to {@link BigInteger}.
     *
     * @param p a long.
     * @param q a long.
     * @return the GCD of p and q.
     */
    private static long gcdBigInteger(long p, long q) {
        return BigInteger.valueOf(p).gcd(BigInteger.valueOf(q)).longValue();
    }

    /**
     * This is a copy of the original GCD method for ints in {@code o.a.c.numbers.core.ArithmeticUtils} v1.0,
     * but adapted for longs.
     *
     * @param p a long.
     * @param q a long.
     * @return the GCD of p and q.
     */
    private static long oldGcdIntAdaptedForLong(long p, long q) {
        // Perform the gcd algorithm on negative numbers, so that -2^31 does not
        // need to be handled separately
        long a = p > 0 ? -p : p;
        long b = q > 0 ? -q : q;

        long negatedGcd;
        if (a == 0) {
            negatedGcd = b;
        } else if (b == 0) {
            negatedGcd = a;
        } else {
            // Make "a" and "b" odd, keeping track of common power of 2.
            final int aTwos = Long.numberOfTrailingZeros(a);
            final int bTwos = Long.numberOfTrailingZeros(b);
            a >>= aTwos;
            b >>= bTwos;
            final int shift = Math.min(aTwos, bTwos);

            // "a" and "b" are negative and odd.
            // If a < b then "gdc(a, b)" is equal to "gcd(a - b, b)".
            // If a > b then "gcd(a, b)" is equal to "gcd(b - a, a)".
            // Hence, in the successive iterations:
            //  "a" becomes the negative absolute difference of the current values,
            //  "b" becomes that value of the two that is closer to zero.
            while (a != b) {
                final long delta = a - b;
                b = Math.max(a, b);
                a = delta > 0 ? -delta : delta;

                // Remove any power of 2 in "a" ("b" is guaranteed to be odd).
                a >>= Long.numberOfTrailingZeros(a);
            }

            // Recover the common power of 2.
            negatedGcd = a << shift;
        }
        if (negatedGcd == Long.MIN_VALUE) {
            throw new ArithmeticException();
        }

        return -negatedGcd;
    }

    /**
     * This is a copy of the original method in {@code o.a.c.numbers.core.ArithmeticUtils} v1.0.
     *
     * @param p a long.
     * @param q a long.
     * @return the GCD of p and q.
     */
    private static long oldGcdLong(final long p, final long q) {
        long u = p;
        long v = q;
        if (u == 0 || v == 0) {
            if (u == Long.MIN_VALUE || v == Long.MIN_VALUE) {
                throw new ArithmeticException();
            }
            return Math.abs(u) + Math.abs(v);
        }
        // keep u and v negative, as negative integers range down to
        // -2^63, while positive numbers can only be as large as 2^63-1
        // (i.e. we can't necessarily negate a negative number without
        // overflow)
        /* assert u!=0 && v!=0; */
        if (u > 0) {
            u = -u;
        } // make u negative
        if (v > 0) {
            v = -v;
        } // make v negative
        // B1. [Find power of 2]
        int k = 0;
        while ((u & 1) == 0 && (v & 1) == 0 && k < 63) { // while u and v are
            // both even...
            u /= 2;
            v /= 2;
            k++; // cast out twos.
        }
        if (k == 63) {
            throw new ArithmeticException();
        }
        // B2. Initialize: u and v have been divided by 2^k and at least
        // one is odd.
        long t = ((u & 1) == 1) ? v : -(u / 2)/* B3 */;
        // t negative: u was odd, v may be even (t replaces v)
        // t positive: u was even, v is odd (t replaces u)
        do {
            /* assert u<0 && v<0; */
            // B4/B3: cast out twos from t.
            while ((t & 1) == 0) { // while t is even..
                t /= 2; // cast out twos
            }
            // B5 [reset max(u,v)]
            if (t > 0) {
                u = -t;
            } else {
                v = t;
            }
            // B6/B3. at this point both u and v should be odd.
            t = (v - u) / 2;
            // |u| larger: t positive (replace u)
            // |v| larger: t negative (replace v)
        } while (t != 0);
        return -u * (1L << k); // gcd is u*2^k
    }

    /**
     * This is a copy of the original method in {@code o.a.c.numbers.core.ArithmeticUtils} v1.0.
     *
     * @param p a long.
     * @param q a long.
     * @return the GCD of p and q.
     */
    private static int oldGcdInt(int p, int q) {
        // Perform the gcd algorithm on negative numbers, so that -2^31 does not
        // need to be handled separately
        int a = p > 0 ? -p : p;
        int b = q > 0 ? -q : q;

        int negatedGcd;
        if (a == 0) {
            negatedGcd = b;
        } else if (b == 0) {
            negatedGcd = a;
        } else {
            // Make "a" and "b" odd, keeping track of common power of 2.
            final int aTwos = Integer.numberOfTrailingZeros(a);
            final int bTwos = Integer.numberOfTrailingZeros(b);
            a >>= aTwos;
            b >>= bTwos;
            final int shift = Math.min(aTwos, bTwos);

            // "a" and "b" are negative and odd.
            // If a < b then "gdc(a, b)" is equal to "gcd(a - b, b)".
            // If a > b then "gcd(a, b)" is equal to "gcd(b - a, a)".
            // Hence, in the successive iterations:
            //  "a" becomes the negative absolute difference of the current values,
            //  "b" becomes that value of the two that is closer to zero.
            while (a != b) {
                final int delta = a - b;
                b = Math.max(a, b);
                a = delta > 0 ? -delta : delta;

                // Remove any power of 2 in "a" ("b" is guaranteed to be odd).
                a >>= Integer.numberOfTrailingZeros(a);
            }

            // Recover the common power of 2.
            negatedGcd = a << shift;
        }
        if (negatedGcd == Integer.MIN_VALUE) {
            throw new ArithmeticException();
        }
        return -negatedGcd;
    }
}
