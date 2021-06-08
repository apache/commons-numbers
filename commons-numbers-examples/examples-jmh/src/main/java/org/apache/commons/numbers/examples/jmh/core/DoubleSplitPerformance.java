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
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;

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
 * Benchmarks focus on the split of a double value into high and low parts.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx512M"})
public class DoubleSplitPerformance {
    /** The mask for the sign bit and the mantissa. */
    private static final long SIGN_MATISSA_MASK = 0x800f_ffff_ffff_ffffL;

    /**
     * The multiplier used to split the double value into high and low parts. From
     * Dekker (1971): "The constant should be chosen equal to 2^(p - p/2) + 1,
     * where p is the number of binary digits in the mantissa". Here p is 53
     * and the multiplier is {@code 2^27 + 1}.
     */
    private static final double MULTIPLIER = 1.34217729E8;

    /** The upper limit above which a number may overflow during the split into a high part.
     * Assuming the multiplier is above 2^27 and the maximum exponent is 1023 then a safe
     * limit is a value with an exponent of (1023 - 27) = 2^996. */
    private static final double SAFE_UPPER = 0x1.0p996;

    /** The scale to use when down-scaling during a split into a high part.
     * This must be smaller than the inverse of the multiplier and a power of 2 for exact scaling. */
    private static final double DOWN_SCALE = 0x1.0p-30;

    /** The scale to use when re-scaling during a split into a high part.
     * This is the inverse of {@link #DOWN_SCALE}. */
    private static final double UP_SCALE = 0x1.0p30;

    /** The mask to zero the lower 27-bits of a long . */
    private static final long ZERO_LOWER_27_BITS = 0xffff_ffff_f800_0000L;

    /** Constant to no method. */
    private static final String NONE = "none";

    /**
     * The numbers to split.
     */
    @State(Scope.Benchmark)
    public static class Numbers {
        /** The exponent for small numbers. */
        private static final long EXP_SMALL = Double.doubleToRawLongBits(1.0);
        /** The exponent for big numbers. */
        private static final long EXP_BIG = Double.doubleToRawLongBits(SAFE_UPPER);

        /**
         * The count of numbers.
         */
        @Param({"10000"})
        private int size;

        /**
         * The fraction of small numbers.
         *
         * <p>Note: The split method may employ multiplications.
         * Big numbers are edge cases that would cause overflow in multiplications.
         */
        @Param({"1", "0.999", "0.99", "0.9"})
        private double edge;

        /** Numbers. */
        private double[] a;

        /**
         * Gets the factors.
         *
         * @return Factors.
         */
        public double[] getNumbers() {
            return a;
        }

        /**
         * Create the factors.
         */
        @Setup
        public void setup() {
            final UniformRandomProvider rng = RandomSource.create(RandomSource.XO_RO_SHI_RO_1024_PP);
            a = new double[size];
            for (int i = 0; i < size; i++) {
                long bits = rng.nextLong() & SIGN_MATISSA_MASK;
                // The exponent will either be small or big
                if (rng.nextDouble() < edge) {
                    bits |= EXP_SMALL;
                } else {
                    bits |= EXP_BIG;
                }
                a[i] = Double.longBitsToDouble(bits);
            }
        }
    }

    /**
     * The factors to multiply.
     */
    @State(Scope.Benchmark)
    public static class BiFactors {
        /** The exponent for small numbers. */
        private static final long EXP_SMALL = Double.doubleToRawLongBits(1.0);

        /**
         * The count of products.
         */
        @Param({"5000"})
        private int size;

        /**
         * The exponent for big numbers.
         * Two big numbers multiplied together should overflow.
         *
         * <p>Note: If this is set below the safe upper for Dekker's split then a method
         * that computes the split of the two factors independently and then does the multiply
         * may create split parts that will overflow during multiplication.
         */
        @Param({"600", "1000", "1023"})
        private int exp;

        /**
         * The fraction of small numbers.
         *
         * <p>Note: The split numbers are used in multiplications.
         * It is unlikely that many numbers will be larger than the upper limit.
         * These numbers are edge cases that would cause overflow in multiplications if
         * the other number is anywhere close to the same magnitude.
         */
        @Param({"1", "0.95", "0.9"})
        private double edge;

        /** Factors. */
        private double[] a;

        /**
         * Gets the factors to be multiplied as pairs. The array length will be even.
         *
         * @return Factors.
         */
        public double[] getFactors() {
            return a;
        }

        /**
         * Create the factors.
         */
        @Setup
        public void setup() {
            // Validate the big exponent
            final double d = Math.scalb(1.0, exp);
            assert Double.isInfinite(d * d) : "Product of big numbers does not overflow";
            final long expBig = Double.doubleToRawLongBits(d);

            final UniformRandomProvider rng = RandomSource.create(RandomSource.XO_RO_SHI_RO_1024_PP);
            a = new double[size * 2];
            for (int i = 0; i < a.length; i++) {
                long bits = rng.nextLong() & SIGN_MATISSA_MASK;
                // The exponent will either be small or big
                if (rng.nextDouble() < edge) {
                    bits |= EXP_SMALL;
                } else {
                    bits |= expBig;
                }
                a[i] = Double.longBitsToDouble(bits);
            }
        }
    }

    /**
     * The numbers to test to determine if they are not normal.
     */
    @State(Scope.Benchmark)
    public static class NonNormalNumbers {
        /** Non-normal positive numbers. */
        private static final double[] NON_NORMAL =
            {Double.POSITIVE_INFINITY, Double.NaN, Double.MIN_NORMAL};

        /**
         * The count of numbers.
         */
        @Param({"10000"})
        private int size;

        /**
         * The fraction of non-normal factors.
         */
        @Param({"1", "0.999", "0.99", "0.9"})
        private double edge;

        /** Numbers. */
        private double[] a;

        /**
         * Gets the factors.
         *
         * @return Factors.
         */
        public double[] getFactors() {
            return a;
        }

        /**
         * Create the factors.
         */
        @Setup
        public void setup() {
            final UniformRandomProvider rng = RandomSource.create(RandomSource.XO_RO_SHI_RO_1024_PP);
            a = new double[size];
            for (int i = 0; i < size; i++) {
                // Value in (-1, 1)
                double value = rng.nextDouble() * (rng.nextBoolean() ? -1 : 1);
                // The number will either be small or non-normal
                if (rng.nextDouble() < edge) {
                    value *= NON_NORMAL[rng.nextInt(NON_NORMAL.length)];
                }
                a[i] = value;
            }
        }
    }

    /**
     * The split method.
     */
    @State(Scope.Benchmark)
    public static class SplitMethod {
        /**
         * The name of the method.
         */
        @Param({NONE, "dekker", "dekkerAbs", "dekkerRaw", "bits"})
        private String name;

        /** The function. */
        private DoubleUnaryOperator fun;

        /**
         * Gets the function.
         *
         * @return the function
         */
        public DoubleUnaryOperator getFunction() {
            return fun;
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            if (NONE.equals(name)) {
                fun = a -> a;
            } else if ("dekker".equals(name)) {
                fun = DoubleSplitPerformance::splitDekker;
            } else if ("dekkerAbs".equals(name)) {
                fun = DoubleSplitPerformance::splitDekkerAbs;
            } else if ("dekkerRaw".equals(name)) {
                fun = DoubleSplitPerformance::splitDekkerRaw;
            } else if ("bits".equals(name)) {
                fun = DoubleSplitPerformance::splitBits;
            } else {
                throw new IllegalStateException("Unknown split method: " + name);
            }
        }
    }

    /**
     * The method to test for a non-normal number.
     */
    @State(Scope.Benchmark)
    public static class NonNormalMethod {
        /**
         * The name of the method.
         */
        @Param({NONE, "if", "exponent", "exponent2"})
        private String name;

        /** The function. */
        private DoublePredicate fun;

        /**
         * Gets the function.
         *
         * @return the function
         */
        public DoublePredicate getFunction() {
            return fun;
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            if (NONE.equals(name)) {
                fun = a -> false;
            } else if ("if".equals(name)) {
                fun = DoubleSplitPerformance::isNotNormalIf;
            } else if ("exponent".equals(name)) {
                fun = DoubleSplitPerformance::isNotNormalExponent;
            } else if ("exponent2".equals(name)) {
                fun = DoubleSplitPerformance::isNotNormalExponent2;
            } else {
                throw new IllegalStateException("Unknown is non-normal method: " + name);
            }
        }
    }

    /**
     * The method to compute the product round-off.
     */
    @State(Scope.Benchmark)
    public static class RoundoffMethod {
        /**
         * The name of the method.
         */
        @Param({NONE, "multiply", "multiplyUnscaled",
            "productLow", "productLow1", "productLow2", "productLow3", "productLowSplit",
            "productLowUnscaled"})
        private String name;

        /** The function. */
        private DoubleBinaryOperator fun;

        /**
         * Gets the function.
         *
         * @return the function
         */
        public DoubleBinaryOperator getFunction() {
            return fun;
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            if (NONE.equals(name)) {
                // No actually the round-off but x*y - x*y will be optimised away so this
                // captures the multiply overhead.
                fun = (x, y) -> x * y;
            } else if ("multiply".equals(name)) {
                final DoublePrecision.Quad result = new DoublePrecision.Quad();
                fun = (x, y) -> {
                    DoublePrecision.multiply(x, y, result);
                    return result.lo;
                };
            } else if ("productLow".equals(name)) {
                fun = (x, y) -> DoublePrecision.productLow(x, y, x * y);
            } else if ("productLow1".equals(name)) {
                fun = (x, y) -> DoublePrecision.productLow1(x, y, x * y);
            } else if ("productLow2".equals(name)) {
                fun = (x, y) -> DoublePrecision.productLow2(x, y, x * y);
            } else if ("productLow3".equals(name)) {
                fun = (x, y) -> DoublePrecision.productLow3(x, y, x * y);
            } else if ("productLowSplit".equals(name)) {
                fun = (x, y) -> DoublePrecision.productLowSplit(x, y, x * y);
            } else if ("multiplyUnscaled".equals(name)) {
                final DoublePrecision.Quad result = new DoublePrecision.Quad();
                fun = (x, y) -> {
                    DoublePrecision.multiplyUnscaled(x, y, result);
                    return result.lo;
                };
            } else if ("productLowUnscaled".equals(name)) {
                fun = (x, y) -> DoublePrecision.productLowUnscaled(x, y, x * y);
            } else {
                throw new IllegalStateException("Unknown round-off method: " + name);
            }
        }
    }

    /**
     * Implement Dekker's method to split a value into two parts. Multiplying by (2^s + 1) creates
     * a big value from which to derive the two split parts.
     * <pre>
     * c = (2^s + 1) * a
     * a_big = c - a
     * a_hi = c - a_big
     * a_lo = a - a_hi
     * a = a_hi + a_lo
     * </pre>
     *
     * <p>The multiplicand allows a p-bit value to be split into
     * (p-s)-bit value {@code a_hi} and a non-overlapping (s-1)-bit value {@code a_lo}.
     * Combined they have (p􏰔-1) bits of significand but the sign bit of {@code a_lo}
     * contains a bit of information. The constant is chosen so that s is ceil(p/2) where
     * the precision p for a double is 53-bits (1-bit of the mantissa is assumed to be
     * 1 for a non sub-normal number) and s is 27.
     *
     * @param value Value.
     * @return the high part of the value.
     */
    private static double splitDekker(double value) {
        // Avoid overflow
        if (value >= SAFE_UPPER || value <= -SAFE_UPPER) {
            // Do scaling.
            final double x = value * DOWN_SCALE;
            final double c = MULTIPLIER * x;
            final double hi = (c - (c - x)) * UP_SCALE;
            if (Double.isInfinite(hi)) {
                // Number is too large.
                // This occurs if value is infinite or close to Double.MAX_VALUE.
                // Note that Dekker's split creates an approximating 26-bit number which may
                // have an exponent 1 greater than the input value. This will overflow if the
                // exponent is already +1023. Revert to the raw upper 26 bits of the 53-bit
                // mantissa (including the assumed leading 1 bit). This conversion will result in
                // the low part being a 27-bit significand and the potential loss of bits during
                // addition and multiplication. (Contrast to the Dekker split which creates two
                // 26-bit numbers with a bit of information moved to the sign of low.)
                // The conversion will maintain Infinite in the high part where the resulting
                // low part a_lo = a - a_hi = inf - inf = NaN.
                return Double.longBitsToDouble(Double.doubleToRawLongBits(value) & ZERO_LOWER_27_BITS);
            }
            return hi;
        }
        // normal conversion
        final double c = MULTIPLIER * value;
        return c - (c - value);
    }

    /**
     * Implement Dekker's method to split a value into two parts.
     * This is as per {@link #splitDekker(double)} but uses a {@link Math#abs(double)} in the
     * condition statement.
     *
     * @param value Value.
     * @return the high part of the value.
     */
    private static double splitDekkerAbs(double value) {
        if (Math.abs(value) >= SAFE_UPPER) {
            final double x = value * DOWN_SCALE;
            final double c = MULTIPLIER * x;
            final double hi = (c - (c - x)) * UP_SCALE;
            if (Double.isInfinite(hi)) {
                return Double.longBitsToDouble(Double.doubleToRawLongBits(value) & ZERO_LOWER_27_BITS);
            }
            return hi;
        }
        final double c = MULTIPLIER * value;
        return c - (c - value);
    }

    /**
     * Implement Dekker's method to split a value into two parts.
     * This is as per {@link #splitDekker(double)} but has no overflow protection.
     *
     * @param value Value.
     * @return the high part of the value.
     */
    private static double splitDekkerRaw(double value) {
        final double c = MULTIPLIER * value;
        return c - (c - value);
    }

    /**
     * Implement a split using the upper and lower raw bits from the value.
     *
     * @param value Value.
     * @return the high part of the value.
     */
    private static double splitBits(double value) {
        return Double.longBitsToDouble(Double.doubleToRawLongBits(value) & ZERO_LOWER_27_BITS);
    }

    /**
     * Checks if the number is not normal. This is functionally equivalent to:
     * <pre>
     * </pre>
     *
     * @param a The value.
     * @return true if the value is not normal
     */
    private static boolean isNotNormalIf(double a) {
        final double abs = Math.abs(a);
        return abs <= Double.MIN_NORMAL || !(abs <= Double.MAX_VALUE);
    }

    /**
     * Checks if the number is not normal.
     *
     * @param a The value.
     * @return true if the value is not normal
     */
    private static boolean isNotNormalExponent(double a) {
        // Sub-normal numbers have a biased exponent of 0.
        // Inf/NaN numbers have a biased exponent of 2047.
        // Catch both cases by extracting the raw exponent, subtracting 1
        // and make unsigned. 0 will underflow to a large value.
        final int baisedExponent = ((int) (Double.doubleToRawLongBits(a) >>> 52)) & 0x7ff;
        return ((baisedExponent - 1) & 0xffff) >= 2046;
    }

    /**
     * Checks if the number is not normal.
     *
     * @param a The value.
     * @return true if the value is not normal
     */
    private static boolean isNotNormalExponent2(double a) {
        // Sub-normal numbers have a biased exponent of 0.
        // Inf/NaN numbers have a biased exponent of 2047.
        // Catch both cases by extracting the raw exponent, subtracting 1
        // and compare unsigned (so 0 underflows to a large value).
        final int baisedExponent = ((int) (Double.doubleToRawLongBits(a) >>> 52)) & 0x7ff;
        // Adding int min value is equal to compare unsigned
        return baisedExponent + Integer.MIN_VALUE - 1 >= 2046 + Integer.MIN_VALUE;
    }

    // Benchmark methods.

    /**
     * Benchmark extracting the high part of the split number.
     *
     * @param numbers Factors.
     * @param bh Data sink.
     * @param method Split method
     */
    @Benchmark
    public void high(Numbers numbers, Blackhole bh, SplitMethod method) {
        final DoubleUnaryOperator fun = method.getFunction();
        final double[] a = numbers.getNumbers();
        for (int i = 0; i < a.length; i++) {
            bh.consume(fun.applyAsDouble(a[i]));
        }
    }

    /**
     * Benchmark extracting the low part of the split number.
     *
     * @param numbers Factors.
     * @param bh Data sink.
     * @param method Split method.
     */
    @Benchmark
    public void low(Numbers numbers, Blackhole bh, SplitMethod method) {
        final DoubleUnaryOperator fun = method.getFunction();
        final double[] a = numbers.getNumbers();
        for (int i = 0; i < a.length; i++) {
            bh.consume(a[i] - fun.applyAsDouble(a[i]));
        }
    }

    /**
     * Benchmark testing if a number is non-normal.
     *
     * @param numbers Factors.
     * @param bh Data sink.
     * @param method Split method.
     */
    @Benchmark
    public void nonNormal(NonNormalNumbers numbers, Blackhole bh, NonNormalMethod method) {
        final DoublePredicate fun = method.getFunction();
        final double[] a = numbers.getFactors();
        for (int i = 0; i < a.length; i++) {
            bh.consume(fun.test(a[i]));
        }
    }

    /**
     * Benchmark extracting the round-off from the product of two numbers.
     *
     * @param factors Factors.
     * @param bh Data sink.
     * @param method Round-off method.
     */
    @Benchmark
    public void productLow(BiFactors factors, Blackhole bh, RoundoffMethod method) {
        final DoubleBinaryOperator fun = method.getFunction();
        final double[] a = factors.getFactors();
        for (int i = 0; i < a.length; i += 2) {
            bh.consume(fun.applyAsDouble(a[i], a[i + 1]));
        }
    }
}
