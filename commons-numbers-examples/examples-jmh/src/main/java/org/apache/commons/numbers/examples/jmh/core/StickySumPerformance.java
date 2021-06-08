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
 * Benchmarks focus on the sticky summation of two double values.
 *
 * <p>Details of the sticky bit can be found in:
 * <blockquote>
 * Coonen, J.T., "An Implementation Guide to a Proposed Standard for Floating Point
 * Arithmetic", Computer, Vol. 13, No. 1, Jan. 1980, pp 68-79.
 * </blockquote>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx512M"})
public class StickySumPerformance {
    /** The mask for the sign bit and the mantissa. */
    private static final long SIGN_MATISSA_MASK = 0x800f_ffff_ffff_ffffL;

    /** Constant for no method. */
    private static final String NONE = "none";
    /** Constant for branched method. */
    private static final String BRANCHED = "branched";
    /** Constant for branchless method. */
    private static final String BRANCHLESS = "branchless";
    /** Constant for single branch method based on the low bit of the high part. */
    private static final String BRANCH_ON_HI = "branch_on_hi";
    /** Constant for single branch method based on the low part. */
    private static final String BRANCH_ON_LO = "branch_on_lo";

    /**
     * The factors to sum.
     */
    @State(Scope.Benchmark)
    public static class BiFactors {
        /** The exponent for small numbers. */
        private static final long EXP = Double.doubleToRawLongBits(1.0);

        /**
         * The count of sums.
         */
        @Param({"10000"})
        private int size;

        /**
         * The fraction of numbers that have a zero round-off.
         */
        @Param({"0", "0.1"})
        private double zeroRoundoff;

        /** Factors. */
        private double[] a;

        /**
         * Gets the factors to be summed as pairs. The array length will be even.
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
            a = new double[size * 2];
            // Report on the dataset
            int nonZero = 0;
            int unsetSticky = 0;
            for (int i = 0; i < a.length; i += 2) {
                // Create data similar to the summation of an expansion.
                // E.g. Generate 2 numbers with large differences in exponents.
                // Their sum should have a round-off term containing many bits.
                final double x = nextDouble(rng);
                // The round-off can conditionally be forced to zero.
                final double y = (rng.nextDouble() < zeroRoundoff) ?
                    0.0 :
                    nextDouble(rng) * 0x1.0p52;

                // Initial expansion
                double e1 = x + y;
                double e0 = DoublePrecision.twoSumLow(x, y, e1);

                // Validate methods
                final double expected = fastSumWithStickyBitBranched(e0, e1);
                assertEqual(expected, fastSumWithStickyBitBranchless(e0, e1), BRANCHLESS);
                assertEqual(expected, fastSumWithStickyBitBranchedOnHigh(e0, e1), BRANCH_ON_HI);
                assertEqual(expected, fastSumWithStickyBitBranchedOnLow(e0, e1), BRANCH_ON_LO);

                // Lower parts of expansion for use in sticky sum
                a[i] = e0;
                a[i + 1] = e1;

                // Check the sum and round-off
                final double sum = e1 + e0;
                final double r = e0 - (sum - e1);
                if (r != 0) {
                    nonZero++;
                }
                if ((Double.doubleToRawLongBits(sum) & 0x1) == 0) {
                    unsetSticky++;
                }
            }
            // CHECKSTYLE: stop Regexp
            System.out.printf("%n%nNon-zero %d/%d (%.3f) : Unset sticky %d/%d (%.3f)%n%n",
                nonZero, size,
                (double) nonZero / size, unsetSticky, size, (double) unsetSticky / size);
            // CHECKSTYLE: resume Regexp
        }

        /**
         * Create the next double in the range [1, 2). All mantissa bits have an equal
         * probability of being set.
         *
         * @param rng Generator of random numbers.
         * @return the double
         */
        private static double nextDouble(UniformRandomProvider rng) {
            final long bits = rng.nextLong() & SIGN_MATISSA_MASK;
            return Double.longBitsToDouble(bits | EXP);
        }

        /**
         * Assert the two values are equal.
         *
         * @param expected Expected value
         * @param actual Actual value
         * @param msg Error message
         */
        private static void assertEqual(double expected, double actual, String msg) {
            if (Double.compare(expected, actual) != 0) {
                throw new IllegalStateException("Methods do not match: " + msg);
            }
        }
    }

    /**
     * The summation method.
     */
    @State(Scope.Benchmark)
    public static class SumMethod {
        /**
         * The name of the method.
         */
        @Param({NONE, BRANCHED, BRANCHLESS, BRANCH_ON_HI, BRANCH_ON_LO})
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
                fun = (a, b) -> a + b;
            } else if (BRANCHED.equals(name)) {
                fun = StickySumPerformance::fastSumWithStickyBitBranched;
            } else if (BRANCHLESS.equals(name)) {
                fun = StickySumPerformance::fastSumWithStickyBitBranchless;
            } else if (BRANCH_ON_HI.equals(name)) {
                fun = StickySumPerformance::fastSumWithStickyBitBranchedOnHigh;
            } else if (BRANCH_ON_LO.equals(name)) {
                fun = StickySumPerformance::fastSumWithStickyBitBranchedOnLow;
            } else {
                throw new IllegalStateException("Unknown sum method: " + name);
            }
        }
    }

    /**
     * Compute the sum of two numbers {@code a} and {@code b} using
     * Dekker's two-sum algorithm. The values are required to be ordered by magnitude
     * {@code |a| >= |b|}. The result is adjusted to set the lowest bit as a sticky
     * bit that summarises the magnitude of the round-off that were lost. The
     * result is not the correctly rounded result; it is intended the result is to
     * be used in an addition with a value with a greater magnitude exponent. This
     * addition will have exact round-to-nearest, ties-to-even rounding taking account
     * of bits lots in the previous sum.
     *
     * @param a First part of sum.
     * @param b Second part of sum.
     * @return <code>b - (sum - a)</code>
     * @see <a href="http://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps">
     * Shewchuk (1997) Theorum 6</a>
     */
    private static double fastSumWithStickyBitBranched(double a, double b) {
        double sum = a + b;
        // bVitual = sum - a
        // b - bVirtual == b round-off
        final double r = b - (sum - a);

        if (r != 0) {
            // Bits will be lost.
            // In floating-point arithmetic the sticky bit is the bit-wise OR
            // of the rest of the binary bits that cannot be stored in the
            // preliminary representation of the result:
            //
            // sgn | exp | V | N | .............. | L | G | R | S
            //
            // sgn : sign bit
            // exp : exponent
            // V : overflow for significand field
            // N and L : most and least significant bits of the result
            // G and R : the two bits beyond
            // S : sticky bit, bitwise OR of all bits thereafter
            //
            // The sticky bit is a flag indicating if there is more magnitude beyond
            // the last bits. Here the round-off is signed so we have to consider the
            // sign of the sum and round-off together and either add the sticky or
            // remove it. The final bit is thus used to push up the next addition using
            // the sum to a higher value, or down to a lower value, when tie breaking for
            // the correct round-to-nearest, ties-to-even result.
            long hi = Double.doubleToRawLongBits(sum);
            // Can only set a sticky bit if the bit is not set.
            if ((hi & 0x1) == 0) {
                // In a standard extended precision result for (a+b) the bits are extra
                // magnitude lost and the sticky bit is positive.
                // Here the round-off magnitude (r) can be negative so the sticky
                // bit should be added (same sign) or subtracted (different sign).
                if (sum > 0) {
                    hi += (r > 0) ? 1 : -1;
                } else {
                    hi += (r < 0) ? 1 : -1;
                }
                sum = Double.longBitsToDouble(hi);
            }
        }
        return sum;
    }

    /**
     * Compute the sum of two numbers {@code a} and {@code b} using
     * Dekker's two-sum algorithm. The values are required to be ordered by magnitude
     * {@code |a| >= |b|}. The result is adjusted to set the lowest bit as a sticky
     * bit that summarises the magnitude of the round-off that were lost. The
     * result is not the correctly rounded result; it is intended the result is to
     * be used in an addition with a value with a greater magnitude exponent. This
     * addition will have exact round-to-nearest, ties-to-even rounding taking account
     * of bits lots in the previous sum.
     *
     * @param a First part of sum.
     * @param b Second part of sum.
     * @return <code>b - (sum - a)</code>
     * @see <a href="http://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps">
     * Shewchuk (1997) Theorum 6</a>
     */
    private static double fastSumWithStickyBitBranchless(double a, double b) {
        final double sum = a + b;
        // bVitual = sum - a
        // b - bVirtual == b round-off
        final double r = b - (sum - a);

        // In floating-point arithmetic the sticky bit is the bit-wise OR
        // of the rest of the binary bits that cannot be stored in the
        // preliminary representation of the result:
        //
        // sgn | exp | V | N | .............. | L | G | R | S
        //
        // sgn : sign bit
        // exp : exponent
        // V : overflow for significand field
        // N and L : most and least significant bits of the result
        // G and R : the two bits beyond
        // S : sticky bit, bitwise OR of all bits thereafter
        //
        // The sticky bit is a flag indicating if there is more magnitude beyond
        // the last bits.
        //
        // Here the round-off is signed so we have to consider the
        // sign of the sum and round-off together and either add the sticky or
        // remove it. The final bit is thus used to push up the next addition using
        // the sum to a higher value, or down to a lower value, when tie breaking for
        // the correct round-to-nearest, ties-to-even result.
        //
        // One extra consideration: sum is already rounded. Since we are using the
        // last bit to store a sticky bit then if the final bit is 1 then this was
        // not created by a ties-to-even rounding and is already a sticky bit.

        // Compute the sticky bit addition:
        // sign sum   last bit sum    sign r    magnitude r      sticky
        // x          1               x         x                +0
        //
        // 1          0               1         1                +1
        // 1          0               1         0                +0
        // 1          0               0         1                -1
        // 1          0               0         0                +0
        // 0          0               1         1                -1
        // 0          0               1         0                +0
        // 0          0               0         1                +1
        // 0          0               0         0                +0
        //
        // Magnitude of r is computed by bitwise OR of the 63-bits from exponent+mantissa
        // Sign of sum and r is the sign-bit of sum or r

        final long hi = Double.doubleToRawLongBits(sum);

        // Note: >50% of the time all code below here is redundant
        //if ((hi & 0x1) == 0x1) {
        //    // Already sticky
        //    return sum;
        //}

        final long lo = Double.doubleToRawLongBits(r);

        // OR compress least significant 63-bits into lowest bit
        long sticky = lo;
        sticky |= sticky >>> 31; // Discard sign bit
        sticky |= sticky >>> 16;
        sticky |= sticky >>> 8;
        sticky |= sticky >>> 4;
        sticky |= sticky >>> 2;
        sticky |= sticky >>> 1; // final sticky bit is in position 0

        // AND with the inverse of the trailing bit from hi to set it to zero
        // if the last bit in hi is 1 (already sticky).
        sticky = sticky & ~hi;

        // Clear the rest. Sticky is now 0 if r was 0.0; or 1 if r was non-zero.
        sticky = sticky & 0x1;

        // The sign bit is created as + or - using the XOR of hi and lo.
        // Signed shift will create a flag: -1 to negate, else 0.
        final long fNegate = (hi ^ lo) >> 63;

        // Conditionally negate a value without branching:
        // http://graphics.stanford.edu/~seander/bithacks.html#ConditionalNegate
        // (Logic updated since fNegate is already negative.)
        // (1 ^  0) -  0 =  1 -  0 =  1
        // (0 ^  0) -  0 =  0 -  0 =  0
        // (1 ^ -1) - -1 = -2 - -1 = -1
        // (0 ^ -1) - -1 = -1 - -1 =  0
        sticky = (sticky ^ fNegate) - fNegate;

        return Double.longBitsToDouble(hi + sticky);
    }

    /**
     * Compute the sum of two numbers {@code a} and {@code b} using
     * Dekker's two-sum algorithm. The values are required to be ordered by magnitude
     * {@code |a| >= |b|}. The result is adjusted to set the lowest bit as a sticky
     * bit that summarises the magnitude of the round-off that were lost. The
     * result is not the correctly rounded result; it is intended the result is to
     * be used in an addition with a value with a greater magnitude exponent. This
     * addition will have exact round-to-nearest, ties-to-even rounding taking account
     * of bits lots in the previous sum.
     *
     * @param a First part of sum.
     * @param b Second part of sum.
     * @return <code>b - (sum - a)</code>
     * @see <a href="http://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps">
     * Shewchuk (1997) Theorum 6</a>
     */
    private static double fastSumWithStickyBitBranchedOnHigh(double a, double b) {
        final double sum = a + b;
        // bVitual = sum - a
        // b - bVirtual == b round-off
        final double r = b - (sum - a);

        // In floating-point arithmetic the sticky bit is the bit-wise OR
        // of the rest of the binary bits that cannot be stored in the
        // preliminary representation of the result:
        //
        // sgn | exp | V | N | .............. | L | G | R | S
        //
        // sgn : sign bit
        // exp : exponent
        // V : overflow for significand field
        // N and L : most and least significant bits of the result
        // G and R : the two bits beyond
        // S : sticky bit, bitwise OR of all bits thereafter
        //
        // The sticky bit is a flag indicating if there is more magnitude beyond
        // the last bits.
        //
        // Here the round-off is signed so we have to consider the
        // sign of the sum and round-off together and either add the sticky or
        // remove it. The final bit is thus used to push up the next addition using
        // the sum to a higher value, or down to a lower value, when tie breaking for
        // the correct round-to-nearest, ties-to-even result.
        //
        // One extra consideration: sum is already rounded. Since we are using the
        // last bit to store a sticky bit then if the final bit is 1 then this was
        // not created by a ties-to-even rounding and is already a sticky bit.

        // Compute the sticky bit addition:
        // sign sum   last bit sum    sign r    magnitude r      sticky
        // x          1               x         x                +0
        //
        // 1          0               1         1                +1
        // 1          0               1         0                +0
        // 1          0               0         1                -1
        // 1          0               0         0                +0
        // 0          0               1         1                -1
        // 0          0               1         0                +0
        // 0          0               0         1                +1
        // 0          0               0         0                +0
        //
        // Magnitude of r is computed by bitwise OR of the 63-bits from exponent+mantissa
        // Sign of sum and r is the sign-bit of sum or r

        final long hi = Double.doubleToRawLongBits(sum);

        if ((hi & 0x1) == 0x1) {
            // Already sticky
            return sum;
        }

        final long lo = Double.doubleToRawLongBits(r);

        // OR compress least significant 63-bits into lowest bit
        long sticky = lo;
        sticky |= sticky >>> 31; // Discard sign bit
        sticky |= sticky >>> 16;
        sticky |= sticky >>> 8;
        sticky |= sticky >>> 4;
        sticky |= sticky >>> 2;
        sticky |= sticky >>> 1; // final sticky bit is in position 0

        // No requirement for AND with the inverse of the trailing bit from hi as we
        // have eliminated that condition.

        // Clear the rest. Sticky is now 0 if r was 0.0; or 1 if r was non-zero.
        sticky = sticky & 0x1;

        // The sign bit is created as + or - using the XOR of hi and lo.
        // Signed shift will create a flag: -1 to negate, else 0.
        final long fNegate = (hi ^ lo) >> 63;

        // Conditionally negate a value without branching:
        // http://graphics.stanford.edu/~seander/bithacks.html#ConditionalNegate
        // (Logic updated since fNegate is already negative.)
        // (1 ^  0) -  0 =  1 -  0 =  1
        // (0 ^  0) -  0 =  0 -  0 =  0
        // (1 ^ -1) - -1 = -2 - -1 = -1
        // (0 ^ -1) - -1 = -1 - -1 =  0
        sticky = (sticky ^ fNegate) - fNegate;

        return Double.longBitsToDouble(hi + sticky);

    }

    /**
     * Compute the sum of two numbers {@code a} and {@code b} using
     * Dekker's two-sum algorithm. The values are required to be ordered by magnitude
     * {@code |a| >= |b|}. The result is adjusted to set the lowest bit as a sticky
     * bit that summarises the magnitude of the round-off that were lost. The
     * result is not the correctly rounded result; it is intended the result is to
     * be used in an addition with a value with a greater magnitude exponent. This
     * addition will have exact round-to-nearest, ties-to-even rounding taking account
     * of bits lots in the previous sum.
     *
     * @param a First part of sum.
     * @param b Second part of sum.
     * @return <code>b - (sum - a)</code>
     * @see <a href="http://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps">
     * Shewchuk (1997) Theorum 6</a>
     */
    private static double fastSumWithStickyBitBranchedOnLow(double a, double b) {
        final double sum = a + b;
        // bVitual = sum - a
        // b - bVirtual == b round-off
        final double r = b - (sum - a);

        // In floating-point arithmetic the sticky bit is the bit-wise OR
        // of the rest of the binary bits that cannot be stored in the
        // preliminary representation of the result:
        //
        // sgn | exp | V | N | .............. | L | G | R | S
        //
        // sgn : sign bit
        // exp : exponent
        // V : overflow for significand field
        // N and L : most and least significant bits of the result
        // G and R : the two bits beyond
        // S : sticky bit, bitwise OR of all bits thereafter
        //
        // The sticky bit is a flag indicating if there is more magnitude beyond
        // the last bits.
        //
        // Here the round-off is signed so we have to consider the
        // sign of the sum and round-off together and either add the sticky or
        // remove it. The final bit is thus used to push up the next addition using
        // the sum to a higher value, or down to a lower value, when tie breaking for
        // the correct round-to-nearest, ties-to-even result.
        //
        // One extra consideration: sum is already rounded. Since we are using the
        // last bit to store a sticky bit then if the final bit is 1 then this was
        // not created by a ties-to-even rounding and is already a sticky bit.

        // Compute the sticky bit addition.
        // This is only done when r is non-zero:
        // sign sum   last bit sum    sign r    sticky
        // x          1               x         +0
        //
        // 1          0               1         +1
        // 1          0               0         -1
        // 0          0               1         -1
        // 0          0               0         +1
        //
        // Sign of sum and r is the sign-bit of sum or r

        // In the majority of cases there is some round-off.
        // Testing for non-zero allows the branch to assume the sticky bit magnitude
        // is 1 (unless the final bit of hi is already set).
        if (r != 0) {
            // Bits will be lost.
            final long hi = Double.doubleToRawLongBits(sum);
            final long lo = Double.doubleToRawLongBits(r);

            // Can only set a sticky bit if the bit is not already set.
            // Flip the bits and extract the lowest bit. This is 1 if
            // the sticky bit is not currently set. If already set then
            // 'sticky' is zero and the rest of this execution path has
            // no effect but we have eliminated the requirement to check the
            // 50/50 branch:
            // if ((hi & 0x1) == 0) {
            //    // set sticky ...
            // }
            int sticky = ~((int) hi) & 0x1;

            // The sign bit is created as + or - using the XOR of hi and lo.
            // Signed shift will create a flag: -1 to negate, else 0.
            final int fNegate = (int) ((hi ^ lo) >> 63);

            // Conditionally negate a value without branching:
            // http://graphics.stanford.edu/~seander/bithacks.html#ConditionalNegate
            // (Logic updated since fNegate is already negative.)
            // (1 ^  0) -  0 =  1 -  0 =  1
            // (0 ^  0) -  0 =  0 -  0 =  0
            // (1 ^ -1) - -1 = -2 - -1 = -1
            // (0 ^ -1) - -1 = -1 - -1 =  0
            sticky = (sticky ^ fNegate) - fNegate;

            return Double.longBitsToDouble(hi + sticky);
        }

        return sum;
    }

    // Benchmark methods.

    /**
     * Benchmark the sticky summation of two numbers.
     *
     * @param factors Factors.
     * @param bh Data sink.
     * @param method Summation method.
     */
    @Benchmark
    public void stickySum(BiFactors factors, Blackhole bh, SumMethod method) {
        final DoubleBinaryOperator fun = method.getFunction();
        final double[] a = factors.getFactors();
        for (int i = 0; i < a.length; i += 2) {
            bh.consume(fun.applyAsDouble(a[i], a[i + 1]));
        }
    }
}
