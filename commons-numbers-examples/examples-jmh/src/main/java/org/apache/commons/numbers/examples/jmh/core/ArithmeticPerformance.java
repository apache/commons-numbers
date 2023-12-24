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
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;
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

/**
 * Executes a benchmark to measure the speed of arithmetic operations.
 *
 * <p>Contains copy implementations of {@code o.a.c.numbers.core.ArithmeticUtils}.
 * Do not call {@code ArithmeticUtils} directly to ensure we are benchmarking the
 * original method and not an updated version that may for example delegate to JDK functions.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx512M"})
public class ArithmeticPerformance {
    /** Method to compute the divide using unsigned arithmetic. */
    private static final String DIVIDE_UNSIGNED = "divideUnsigned";
    /** Method to compute the remainder using unsigned arithmetic. */
    private static final String REMAINDER_UNSIGNED = "remainderUnsigned";

    /**
     * Source of {@code long} array data.
     */
    @State(Scope.Benchmark)
    public static class LongDataSource {
        /** Data length. */
        @Param({"1024"})
        private int length;

        /** Data. */
        private long[] data;

        /**
         * @return the data
         */
        public long[] getData() {
            return data;
        }

        /**
         * Create the data.
         * Data will be randomized per iteration.
         */
        @Setup(Level.Iteration)
        public void setup() {
            data = RandomSource.XO_RO_SHI_RO_128_PP.create().longs(length).toArray();
        }
    }

    /**
     * Source of {@code int} array data.
     */
    @State(Scope.Benchmark)
    public static class IntDataSource {
        /** Data length. */
        @Param({"1024"})
        private int length;

        /** Data. */
        private int[] data;

        /**
         * @return the data
         */
        public int[] getData() {
            return data;
        }

        /**
         * Create the data.
         * Data will be randomized per iteration.
         */
        @Setup(Level.Iteration)
        public void setup() {
            data = RandomSource.XO_RO_SHI_RO_128_PP.create().ints(length).toArray();
        }
    }

    /**
     * Source of a {@link LongBinaryOperator}.
     */
    @State(Scope.Benchmark)
    public static class LongFunctionSource {
        /** Name of the source. */
        @Param({"Long.divideUnsigned", DIVIDE_UNSIGNED,
                "Long.remainderUnsigned", REMAINDER_UNSIGNED})
        private String name;

        /** The action. */
        private LongBinaryOperator function;

        /**
         * @return the function
         */
        public LongBinaryOperator getFunction() {
            return function;
        }

        /**
         * Create the function.
         */
        @Setup(Level.Iteration)
        public void setup() {
            if ("Long.divideUnsigned".equals(name)) {
                function = Long::divideUnsigned;
            } else if (DIVIDE_UNSIGNED.equals(name)) {
                function = ArithmeticPerformance::divideUnsigned;
            } else if ("Long.remainderUnsigned".equals(name)) {
                function = Long::remainderUnsigned;
            } else if (REMAINDER_UNSIGNED.equals(name)) {
                function = ArithmeticPerformance::remainderUnsigned;
            } else {
                throw new IllegalStateException("Unknown long function: " + name);
            }
        }
    }

    /**
     * Source of an {@link IntBinaryOperator}.
     */
    @State(Scope.Benchmark)
    public static class IntFunctionSource {
        /** Name of the source. */
        @Param({"Integer.divideUnsigned", DIVIDE_UNSIGNED,
                "Integer.remainderUnsigned", REMAINDER_UNSIGNED})
        private String name;

        /** The action. */
        private IntBinaryOperator function;

        /**
         * @return the function
         */
        public IntBinaryOperator getFunction() {
            return function;
        }

        /**
         * Create the function.
         */
        @Setup(Level.Iteration)
        public void setup() {
            if ("Integer.divideUnsigned".equals(name)) {
                function = Integer::divideUnsigned;
            } else if (DIVIDE_UNSIGNED.equals(name)) {
                function = ArithmeticPerformance::divideUnsigned;
            } else if ("Integer.remainderUnsigned".equals(name)) {
                function = Integer::remainderUnsigned;
            } else if (REMAINDER_UNSIGNED.equals(name)) {
                function = ArithmeticPerformance::remainderUnsigned;
            } else {
                throw new IllegalStateException("Unknown int function: " + name);
            }
        }
    }

    /**
     * Returns the unsigned remainder from dividing the first argument
     * by the second where each argument and the result is interpreted
     * as an unsigned value.
     * <p>This method does not use the {@code long} datatype.</p>
     *
     * <p>This is a copy of the original method in {@code o.a.c.numbers.core.ArithmeticUtils}.
     *
     * @param dividend the value to be divided
     * @param divisor the value doing the dividing
     * @return the unsigned remainder of the first argument divided by
     * the second argument.
     */
    public static int remainderUnsigned(int dividend, int divisor) {
        if (divisor >= 0) {
            if (dividend >= 0) {
                return dividend % divisor;
            }
            // The implementation is a Java port of algorithm described in the book
            // "Hacker's Delight" (section "Unsigned short division from signed division").
            final int q = ((dividend >>> 1) / divisor) << 1;
            dividend -= q * divisor;
            if (dividend < 0 || dividend >= divisor) {
                dividend -= divisor;
            }
            return dividend;
        }
        return dividend >= 0 || dividend < divisor ? dividend : dividend - divisor;
    }

    /**
     * Returns the unsigned remainder from dividing the first argument
     * by the second where each argument and the result is interpreted
     * as an unsigned value.
     * <p>This method does not use the {@code BigInteger} datatype.</p>
     *
     * <p>This is a copy of the original method in {@code o.a.c.numbers.core.ArithmeticUtils}.
     *
     * @param dividend the value to be divided
     * @param divisor the value doing the dividing
     * @return the unsigned remainder of the first argument divided by
     * the second argument.
     */
    public static long remainderUnsigned(long dividend, long divisor) {
        if (divisor >= 0L) {
            if (dividend >= 0L) {
                return dividend % divisor;
            }
            // The implementation is a Java port of algorithm described in the book
            // "Hacker's Delight" (section "Unsigned short division from signed division").
            final long q = ((dividend >>> 1) / divisor) << 1;
            dividend -= q * divisor;
            if (dividend < 0L || dividend >= divisor) {
                dividend -= divisor;
            }
            return dividend;
        }
        return dividend >= 0L || dividend < divisor ? dividend : dividend - divisor;
    }

    /**
     * Returns the unsigned quotient of dividing the first argument by
     * the second where each argument and the result is interpreted as
     * an unsigned value.
     * <p>Note that in two's complement arithmetic, the three other
     * basic arithmetic operations of add, subtract, and multiply are
     * bit-wise identical if the two operands are regarded as both
     * being signed or both being unsigned. Therefore separate {@code
     * addUnsigned}, etc. methods are not provided.</p>
     * <p>This method does not use the {@code long} datatype.</p>
     *
     * <p>This is a copy of the original method in {@code o.a.c.numbers.core.ArithmeticUtils}.
     *
     * @param dividend the value to be divided
     * @param divisor the value doing the dividing
     * @return the unsigned quotient of the first argument divided by
     * the second argument
     */
    public static int divideUnsigned(int dividend, int divisor) {
        if (divisor >= 0) {
            if (dividend >= 0) {
                return dividend / divisor;
            }
            // The implementation is a Java port of algorithm described in the book
            // "Hacker's Delight" (section "Unsigned short division from signed division").
            int q = ((dividend >>> 1) / divisor) << 1;
            dividend -= q * divisor;
            if (dividend < 0L || dividend >= divisor) {
                q++;
            }
            return q;
        }
        return dividend >= 0 || dividend < divisor ? 0 : 1;
    }

    /**
     * Returns the unsigned quotient of dividing the first argument by
     * the second where each argument and the result is interpreted as
     * an unsigned value.
     * <p>Note that in two's complement arithmetic, the three other
     * basic arithmetic operations of add, subtract, and multiply are
     * bit-wise identical if the two operands are regarded as both
     * being signed or both being unsigned. Therefore separate {@code
     * addUnsigned}, etc. methods are not provided.</p>
     * <p>This method does not use the {@code BigInteger} datatype.</p>
     *
     * <p>This is a copy of the original method in {@code o.a.c.numbers.core.ArithmeticUtils}.
     *
     * @param dividend the value to be divided
     * @param divisor the value doing the dividing
     * @return the unsigned quotient of the first argument divided by
     * the second argument.
     */
    public static long divideUnsigned(long dividend, long divisor) {
        if (divisor >= 0L) {
            if (dividend >= 0L) {
                return dividend / divisor;
            }
            // The implementation is a Java port of algorithm described in the book
            // "Hacker's Delight" (section "Unsigned short division from signed division").
            long q = ((dividend >>> 1) / divisor) << 1;
            dividend -= q * divisor;
            if (dividend < 0L || dividend >= divisor) {
                q++;
            }
            return q;
        }
        return dividend >= 0L || dividend < divisor ? 0L : 1L;
    }

    // Benchmark methods.

    /**
     * Benchmark a {@link LongBinaryOperator}.
     *
     * @param data Data source.
     * @param function Function source.
     * @return the long
     */
    @Benchmark
    public long longOp(LongDataSource data, LongFunctionSource function) {
        final LongBinaryOperator fun = function.getFunction();
        final long[] a = data.getData();
        long s = 0;
        for (int i = 0; i < a.length; i += 2) {
            s += fun.applyAsLong(a[i], a[i + 1]);
        }
        return s;
    }

    /**
     * Benchmark a {@link IntBinaryOperator}.
     *
     * @param data Data source.
     * @param function Function source.
     * @return the int
     */
    @Benchmark
    public int intOp(IntDataSource data, IntFunctionSource function) {
        final IntBinaryOperator fun = function.getFunction();
        final int[] a = data.getData();
        int s = 0;
        for (int i = 0; i < a.length; i += 2) {
            s += fun.applyAsInt(a[i], a[i + 1]);
        }
        return s;
    }
}
