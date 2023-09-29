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
package org.apache.commons.numbers.core;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.BinaryOperator;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test cases for {@link DD} arithmetic.
 */
class DDTest {
    /** Down scale factors to apply to argument y of f(x, y). */
    private static final double[] DOWN_SCALES = {
        1.0, 0x1.0p-1, 0x1.0p-2, 0x1.0p-3, 0x1.0p-5, 0x1.0p-10, 0x1.0p-25, 0x1.0p-51, 0x1.0p-52, 0x1.0p-53, 0x1.0p-100
    };
    /** Scale factors to apply to argument y of f(x, y). */
    private static final double[] SCALES = {
        1.0, 0x1.0p-1, 0x1.0p-2, 0x1.0p-3, 0x1.0p-5, 0x1.0p-10, 0x1.0p-25, 0x1.0p-51, 0x1.0p-52, 0x1.0p-53, 0x1.0p-100,
        0x1.0p1, 0x1.0p2, 0x1.0p3, 0x1.0p5, 0x1.0p10, 0x1.0p25, 0x1.0p51, 0x1.0p52, 0x1.0p53, 0x1.0p100
    };
    /** MathContext for division. A double-double has approximately 34 digits of precision so
     * use twice this to allow computation of relative error of the results to a useful precision. */
    private static final MathContext MC_DIVIDE = new MathContext(MathContext.DECIMAL128.getPrecision() * 2);
    /** A BigDecimal for Long.MAX_VALUE. */
    private static final BigDecimal BD_LONG_MAX = BigDecimal.valueOf(Long.MAX_VALUE);
    /** A BigDecimal for Long.MIN_VALUE. */
    private static final BigDecimal BD_LONG_MIN = BigDecimal.valueOf(Long.MIN_VALUE);
    /** Number of random samples for arithmetic data. */
    private static final int SAMPLES = 100;
    /** The epsilon for relative error. Equivalent to 2^-106 for the precision of a double-double
     * 106-bit mantissa. This value is used to report the accuracy of the functions in the DD javadoc. */
    private static final double EPS = 0x1.0p-106;

    @Test
    void testOne() {
        Assertions.assertEquals(1, DD.ONE.hi());
        Assertions.assertEquals(0, DD.ONE.lo());
        Assertions.assertSame(DD.ONE, DD.of(1.23).one());
    }

    @Test
    void testZero() {
        Assertions.assertEquals(0, DD.ZERO.hi());
        Assertions.assertEquals(0, DD.ZERO.lo());
        Assertions.assertSame(DD.ZERO, DD.of(1.23).zero());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, 1, Math.PI, Double.MIN_VALUE, Double.MAX_VALUE, Double.POSITIVE_INFINITY, Double.NaN})
    void testOfDouble(double x) {
        DD dd = DD.of(x);
        Assertions.assertEquals(x, dd.hi(), "x hi");
        Assertions.assertEquals(0, dd.lo(), "x lo");
        dd = DD.of(-x);
        Assertions.assertEquals(-x, dd.hi(), "-x hi");
        Assertions.assertEquals(0, dd.lo(), "-x lo");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 42, 4674567, Integer.MIN_VALUE, Integer.MAX_VALUE - 42, Integer.MAX_VALUE})
    void testOfInt(int x) {
        DD dd = DD.of(x);
        Assertions.assertEquals(x, dd.hi(), "x hi");
        Assertions.assertEquals(0, dd.lo(), "x lo");
        dd = DD.of(-x);
        Assertions.assertEquals(-x, dd.hi(), "-x hi");
        Assertions.assertEquals(0, dd.lo(), "-x lo");
    }

    /**
     * Test conversion of a {@code long}. The upper part should be the value cast as a double.
     * The lower part is any remaining value. If done incorrectly this can lose bits due
     * to rounding to 2^53 so we have extra cases for this.
     */
    @ParameterizedTest
    @ValueSource(longs = {0, 1, 42, 89545664, 8263492364L, Long.MIN_VALUE,
        Long.MAX_VALUE - (1L << 10), Long.MAX_VALUE - 42, Long.MAX_VALUE - 1, Long.MAX_VALUE})
    void testOfLong(long x) {
        DD dd = DD.of(x);
        Assertions.assertEquals(x, dd.hi(), "x hi should be (double) x");
        Assertions.assertEquals(BigDecimal.valueOf(x).subtract(bd(x)).doubleValue(), dd.lo(), "x lo should be remaining bits");
        dd = DD.of(-x);
        Assertions.assertEquals(-x, dd.hi(), "-x hi should be (double) -x");
        Assertions.assertEquals(BigDecimal.valueOf(-x).subtract(bd(-x)).doubleValue(), dd.lo(), "-x lo should be remaining bits");
    }

    @ParameterizedTest
    @MethodSource(value = {"twoSumAddeds"})
    void testFromBigDecimal(double x, double y) {
        final BigDecimal xy = bd(x).add(bd(y));
        final DD z = DD.from(xy);
        Assertions.assertEquals(xy.doubleValue(), z.hi(), "hi should be the double representation");
        Assertions.assertEquals(xy.subtract(bd(z.hi())).doubleValue(), z.lo(), "lo should be the double representation of the round-off");
    }

    @ParameterizedTest
    @CsvSource({
        "1e500, Infinity, 0",
        "-1e600, -Infinity, 0",
    })
    void testFromBigDecimalInfinite(String value, double x, double xx) {
        final DD z = DD.from(new BigDecimal(value));
        Assertions.assertEquals(x, z.hi(), "hi");
        Assertions.assertEquals(xx, z.lo(), "lo");
    }

    @ParameterizedTest
    @MethodSource
    void testIsFinite(double x, double xx) {
        final DD dd = DD.of(x, xx);
        final boolean isFinite = Double.isFinite(x + xx);
        Assertions.assertEquals(isFinite, dd.isFinite(), "finite evaluated sum");
    }

    static Stream<Arguments> testIsFinite() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // Note: (max + max) will overflow but the DD number should be finite.
        final double[] values = {0, 1, Double.MAX_VALUE, Double.MIN_VALUE,
            Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN};
        for (final double x : values) {
            for (final double xx : values) {
                builder.add(Arguments.of(x, xx));
            }
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"twoSumAddeds", "testDoubleFloatValue"})
    void testDoubleFloatValue(double x, double y) {
        // By creating a non-normalized DD this tests the two parts are added
        final DD dd = DD.of(x, y);
        final double sum = x + y;
        Assertions.assertEquals(sum, dd.doubleValue());
        Assertions.assertEquals((float) sum, dd.floatValue());
    }

    static Stream<Arguments> testDoubleFloatValue() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // Note: (max + max) will overflow but the DD number should be finite.
        final double[] values = {0, 1, -42, -0.5, Double.MAX_VALUE, Double.MIN_VALUE,
            -Double.MIN_NORMAL, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN};
        for (final double x : values) {
            for (final double xx : values) {
                builder.add(Arguments.of(x, xx));
            }
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"twoSumAddeds", "testDoubleFloatValue", "testLongIntValue"})
    void testLongIntValue(double x, double y) {
        // Number must be normalized
        final DD dd = DD.ofSum(x, y);
        if (Double.isFinite(x) && Double.isFinite(y)) {
            final BigDecimal bd = bd(x).add(bd(y));
            // The long/int value should act as if a truncating cast towards zero
            long longExpected;
            if (bd.compareTo(BD_LONG_MIN) <= 0) {
                longExpected = Long.MIN_VALUE;
            } else if (bd.compareTo(BD_LONG_MAX) >= 0) {
                longExpected = Long.MAX_VALUE;
            } else {
                longExpected = bd.longValue();
            }
            Assertions.assertEquals(longExpected, dd.longValue(), "finite parts long");
            final int intExpected = (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, longExpected));
            Assertions.assertEquals(intExpected, dd.intValue(), "finite parts int");
        } else {
            // IEEE754 result for casting a non-finite double
            Assertions.assertEquals((long) (x + y), dd.longValue(), "non-finite parts long");
            Assertions.assertEquals((int) (x + y), dd.intValue(), "non-finite parts int");
        }
    }

    static Stream<Arguments> testLongIntValue() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final double[] values = {0, 1, 42,
            Integer.MIN_VALUE, Integer.MAX_VALUE,
            1L << 52, 1L << 53, 1L << 54,
            Long.MIN_VALUE, Long.MAX_VALUE};
        for (final double x : values) {
            for (final double xx : values) {
                if (Math.abs(xx) < Math.abs(x)) {
                    builder.add(Arguments.of(x, xx));
                    builder.add(Arguments.of(x, -xx));
                    builder.add(Arguments.of(-x, xx));
                    builder.add(Arguments.of(-x, -xx));
                }
            }
        }
        return builder.build();
    }

    /**
     * Test the value is exact after a sum of longs.
     * This exercises {@link DD#of(long)}, {@link DD#bigDecimalValue()} and {@link DD#longValue()}.
     * With a 106-bit mantissa a DD should be able to exactly sum up to 2^43 longs.
     */
    @ParameterizedTest
    @MethodSource
    void testLongSum(long[] values) {
        DD dd = DD.ZERO;
        BigDecimal bd = BigDecimal.ZERO;
        for (final long x : values) {
            dd = dd.add(DD.of(x));
            bd = bd.add(BigDecimal.valueOf(x));
        }
        Assertions.assertEquals(0, bd.compareTo(dd.bigDecimalValue()), "BigDecimal value");
        // The long value should act as if a truncating cast towards zero
        long expected;
        if (bd.compareTo(BD_LONG_MIN) <= 0) {
            expected = Long.MIN_VALUE;
        } else if (bd.compareTo(BD_LONG_MAX) >= 0) {
            expected = Long.MAX_VALUE;
        } else {
            expected = bd.longValue();
        }
        Assertions.assertEquals(expected, dd.longValue(), "long value");
    }

    static Stream<long[]> testLongSum() {
        final Stream.Builder<long[]> builder = Stream.builder();
        // Edge cases around min/max long value, including overflow a long then recover
        final long min = Long.MIN_VALUE;
        final long max = Long.MAX_VALUE;
        builder.add(new long[] {min, 1});
        builder.add(new long[] {min, -1, 1});
        builder.add(new long[] {min, -33, 67});
        builder.add(new long[] {min, min, max, 67});
        builder.add(new long[] {max});
        builder.add(new long[] {max, -1});
        builder.add(new long[] {max, 1, -1});
        builder.add(new long[] {max, 13, -14});
        builder.add(new long[] {max, max, min});
        // Random
        final UniformRandomProvider rng = createRNG();
        for (int i = 1; i < 20; i++) {
            // The average sum will be approximately zero, actual cases may overflow.
            builder.add(rng.longs(2 * i).toArray());
            // Here we make the chance of overflow smaller by dropping 4 bits.
            // The long is still greater than the 53-bit precision of a double.
            builder.add(rng.longs(2 * i).map(x -> x >> 4).toArray());
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"twoSumAddeds", "testDoubleFloatValue", "testLongIntValue"})
    void testBigDecimalValue(double x, double y) {
        // By creating a non-normalized DD this tests the two parts are added
        final DD dd = DD.of(x, y);
        if (Double.isFinite(x) && Double.isFinite(y)) {
            Assertions.assertEquals(0, bd(x).add(bd(y)).compareTo(dd.bigDecimalValue()));
            // Note: no test for isFinite==true as a non-normalized number can be infinite
            // if the sum of the parts overflows (isFinite is conditioned on the evaluated sum)
        } else {
            // Cannot create a BigDecimal if not finite
            Assertions.assertThrows(NumberFormatException.class, () -> dd.bigDecimalValue());
            Assertions.assertFalse(dd.isFinite(), "This should be non-finite");
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"twoSumAddeds"})
    void testFastTwoSum(double xa, double ya) {
        // |x| > |y|
        double x;
        double y;
        if (Math.abs(xa) < Math.abs(ya)) {
            x = ya;
            y = xa;
        } else {
            x = xa;
            y = ya;
        }
        DD z;
        final BigDecimal bx = bd(x);
        final Supplier<String> msg = () -> String.format("%s+%s", x, y);
        for (final double scale : DOWN_SCALES) {
            final double sy = scale * y;
            z = DD.fastTwoSum(x, sy);
            final double hi = x + sy;
            final double lo = bx.add(bd(sy)).subtract(bd(hi)).doubleValue();
            // fast-two-sum should be exact
            Assertions.assertEquals(hi, z.hi(), () -> msg.get() + " hi: scale=" + scale);
            Assertions.assertEquals(lo, z.lo(), () -> msg.get() + " lo: scale=" + scale);
            Assertions.assertEquals(hi, hi + lo, () -> msg.get() + " hi+lo: scale=" + scale);
            Assertions.assertEquals(hi, z.doubleValue(), () -> msg.get() + " doubleValue: scale=" + scale);
            // Same as fastTwoDiff
            z = DD.fastTwoDiff(x, -sy);
            Assertions.assertEquals(hi, z.hi(), () -> msg.get() + " fastTwoDiff hi: scale=" + scale);
            Assertions.assertEquals(lo, z.lo(), () -> msg.get() + " fastTwoDiff lo: scale=" + scale);
            z = DD.fastTwoSum(x, -sy);
            final double z0 = z.hi();
            final double z1 = z.lo();
            z = DD.fastTwoDiff(x, sy);
            Assertions.assertEquals(z0, z.hi(), () -> msg.get() + " fastTwoSum hi: scale=" + scale);
            Assertions.assertEquals(z1, z.lo(), () -> msg.get() + " fastTwoSum lo: scale=" + scale);
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleFloatValue", "testLongIntValue"})
    void testNegate(double x, double y) {
        // By creating a non-normalized DD this tests the two parts are negated
        final DD dd = DD.of(x, y).negate();
        Assertions.assertEquals(-x, dd.hi());
        Assertions.assertEquals(-y, dd.lo());
    }

    @ParameterizedTest
    @MethodSource(value = {"testDoubleFloatValue", "testLongIntValue"})
    void testAbs(double x, double y) {
        // By creating a non-normalized DD this tests the parts are returned based only
        // on the high part x
        final DD dd = DD.of(x, y).abs();
        if (x < 0) {
            Assertions.assertEquals(-x, dd.hi());
            Assertions.assertEquals(-y, dd.lo());
        } else if (x == 0) {
            // Canonical absolute of zero, y is ignored
            Assertions.assertEquals(0.0, dd.hi());
            Assertions.assertEquals(0.0, dd.lo());
        } else {
            Assertions.assertEquals(x, dd.hi());
            Assertions.assertEquals(y, dd.lo());
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"testFloorCeil"})
    void testFloor(double x, double xx) {
        assertFloorCeil(x, xx, true);
    }

    @ParameterizedTest
    @MethodSource(value = {"testFloorCeil"})
    void testCeil(double x, double xx) {
        assertFloorCeil(x, xx, false);
    }

    /**
     * Assert the floor or ceil functions. These are tested together as they should match
     * rounding performed by BigDecimal. BigDecimal cannot handle non-finite values; these
     * are tested as mapping to the floor or ceil result with the low part as zero.
     */
    private static void assertFloorCeil(double x, double xx, boolean isFloor) {
        DD dd = DD.of(x, xx);
        dd = isFloor ? dd.floor() : dd.ceil();
        double y = isFloor ? Math.floor(x) : Math.ceil(x);
        double yy;
        // General floor/ceil result changes x (so assume abs(xx) < 1),
        // or special mappings of non-finite/zero.
        // Here the low part is always +0.0.
        // x != op(x) -> (op(x), 0)
        // (+/0.0, xx) -> (x, 0)
        // (NaN, xx) -> (NaN, 0)
        // (+/-infinity, xx) -> (x, 0)
        if (x == 0 || !Double.isFinite(x) || x != y) {
            yy = +0.0;
        } else {
            assertNormalized(x, xx, "x");
            // scale is the number of digits to the right of the decimal point
            final BigDecimal value = bd(x).add(bd(xx)).setScale(0,
                isFloor ? RoundingMode.FLOOR : RoundingMode.CEILING);
            y = value.doubleValue();
            yy = value.subtract(bd(y)).doubleValue();
            // Note: If yy is zero then BigDecimal math will always create +0.0.
            // The DD class is written to match this by never returning a -0.0
            // for the low component of floor or ceiling.
            if (yy == 0) {
                Assertions.assertEquals(+0.0, yy, "BigDecimal should not generate -0.0 values");
            }
        }
        Assertions.assertEquals(y, dd.hi(), "hi");
        Assertions.assertEquals(yy, dd.lo(), "lo");
    }

    static Stream<Arguments> testFloorCeil() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final double inf = Double.POSITIVE_INFINITY;
        final double nan = Double.NaN;
        final double[] lo = {-0.0, 0.0, -1.0, 1.0, -inf, inf, nan};
        for (final double xx : lo) {
            builder.add(Arguments.of(0.0, xx));
            builder.add(Arguments.of(-0.0, xx));
            builder.add(Arguments.of(nan, xx));
            builder.add(Arguments.of(inf, xx));
            builder.add(Arguments.of(-inf, xx));
        }
        // Must be non-zero. Zero has a special mapping to (x, 0.0).
        // Use both representable integers, and fractions for the high part.
        // We require the low part to be zero, an integer or a fraction.
        // When the values are large they are always representable integers
        // and the low part contains the fraction.
        final double[] hi = {1, 1.5, 1234, 1234.5,
            // has a ulp of 2
            1.2345 * 0x1.0p53,
            // has a ulp of 4
            1.2345 * 0x1.0p54,
            // has a ulp of 2048
            1.2345 * 0x1.0p63};
        final double min = Double.MIN_VALUE;
        for (final double x : hi) {
            builder.add(Arguments.of(x, 0));
            builder.add(Arguments.of(-x, 0));
            builder.add(Arguments.of(x, -min));
            builder.add(Arguments.of(x, min));
            builder.add(Arguments.of(-x, min));
            builder.add(Arguments.of(-x, -min));
            // Avoid Math.ulp in case x is an exact power of 2.
            double down = Math.nextDown(x) - x;
            double up = Math.nextUp(x) - x;
            // xx must be less than 0.5 ulp to be a normalised double-double.
            // Create a non-power of 2 low part with a division by 3.
            down /= 3;
            up /= 3;
            builder.add(Arguments.of(x, down));
            builder.add(Arguments.of(x, up));
            builder.add(Arguments.of(-x, -down));
            builder.add(Arguments.of(-x, -up));
            // Large numbers can use an integer low part
            down = Math.ceil(down);
            if (down <= -1) {
                up = Math.floor(up);
                builder.add(Arguments.of(x, down));
                builder.add(Arguments.of(x, up));
                builder.add(Arguments.of(-x, -down));
                builder.add(Arguments.of(-x, -up));
            }
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"twoSumAddeds"})
    void testTwoSum(double x, double y) {
        // This method currently uses DD.add and DD.subtract and not the internally named
        // twoSum and twoDiff.
        DD z;
        final BigDecimal bx = bd(x);
        final Supplier<String> msg = () -> String.format("%s+%s", x, y);
        for (final double scale : DOWN_SCALES) {
            final double sy = scale * y;
            z = DD.ofSum(x, sy);
            final double hi = x + sy;
            final double lo = bx.add(bd(sy)).subtract(bd(hi)).doubleValue();
            // two-sum should be exact
            Assertions.assertEquals(hi, z.hi(), () -> msg.get() + " hi: scale=" + scale);
            Assertions.assertEquals(lo, z.lo(), () -> msg.get() + " lo: scale=" + scale);
            Assertions.assertEquals(hi, hi + lo, () -> msg.get() + " hi+lo: scale=" + scale);
            Assertions.assertEquals(hi, z.doubleValue(), () -> msg.get() + " doubleValue: scale=" + scale);
            z = DD.ofSum(sy, x);
            Assertions.assertEquals(hi, z.hi(), () -> msg.get() + " reversed hi: scale=" + scale);
            Assertions.assertEquals(lo, z.lo(), () -> msg.get() + " reversed lo: scale=" + scale);
            // Same as twoDiff
            z = DD.ofDifference(x, -sy);
            Assertions.assertEquals(hi, z.hi(), () -> msg.get() + " twoDiff hi: scale=" + scale);
            Assertions.assertEquals(lo, z.lo(), () -> msg.get() + " twoDiff lo: scale=" + scale);
            z = DD.ofSum(x, -sy);
            final double z0 = z.hi();
            final double z1 = z.lo();
            z = DD.ofDifference(x, sy);
            Assertions.assertEquals(z0, z.hi(), () -> msg.get() + " twoSum hi: scale=" + scale);
            Assertions.assertEquals(z1, z.lo(), () -> msg.get() + " twoSum lo: scale=" + scale);
        }
    }

    static Stream<Arguments> twoSumAddeds() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = createRNG();
        for (int i = 0; i < SAMPLES; i++) {
            builder.add(Arguments.of(signedNormalDouble(rng), signedNormalDouble(rng)));
        }
        return builder.build();
    }

    /**
     * Test {@link DD#twoSum(double, double)} with cases that have non-normal input
     * or create overflow.
     */
    @Test
    void testTwoSumSpecialCases() {
        // x + y is sub-normal or zero
        assertSum(0.0, Math.nextDown(Double.MIN_NORMAL), -Math.ulp(Double.MIN_NORMAL));
        assertSum(0.0, -1.0, 1.0);
        // x or y is infinite or NaN
        assertSum(Double.NaN, 1.0, Double.POSITIVE_INFINITY);
        assertSum(Double.NaN, 1.0, Double.NEGATIVE_INFINITY);
        assertSum(Double.NaN, 1.0, Double.NaN);
        // x + y is infinite
        assertSum(Double.NaN, 0x1.0p1023, 0x1.0p1023);
        assertSum(Double.NaN, Math.ulp(Double.MAX_VALUE), Double.MAX_VALUE);
    }

    private static void assertSum(double expectedLo, double x, double y) {
        final DD z = DD.ofSum(x, y);
        Assertions.assertEquals(x + y, z.hi(), "hi");
        // Requires a delta of 0.0 to assert -0.0 == 0.0
        Assertions.assertEquals(expectedLo, z.lo(), 0.0, "lo");
        final DD z2 = DD.ofSum(y, x);
        Assertions.assertEquals(z.hi(), z2.hi(), "y+x hi");
        Assertions.assertEquals(z.lo(), z2.lo(), "y+x lo");
    }

    @ParameterizedTest
    @MethodSource
    void testTwoProd(double x, double y) {
        DD z;
        final BigDecimal bx = bd(x);
        final Supplier<String> msg = () -> String.format("%s*%s", x, y);
        for (final double scale : DOWN_SCALES) {
            final double sy = scale * y;
            z = DD.ofProduct(x, sy);
            final double hi = x * sy;
            final double lo = bx.multiply(bd(sy)).subtract(bd(hi)).doubleValue();
            // two-prod should be exact
            Assertions.assertEquals(hi, z.hi(), () -> msg.get() + " hi: scale=" + scale);
            Assertions.assertEquals(lo, z.lo(), () -> msg.get() + " lo: scale=" + scale);
            Assertions.assertEquals(hi, hi + lo, () -> msg.get() + " hi+lo: scale=" + scale);
            Assertions.assertEquals(hi, z.doubleValue(), () -> msg.get() + " doubleValue: scale=" + scale);
            z = DD.ofProduct(sy, x);
            Assertions.assertEquals(hi, z.hi(), () -> msg.get() + " reversed hi: scale=" + scale);
            Assertions.assertEquals(lo, z.lo(), () -> msg.get() + " reversed lo: scale=" + scale);
        }
    }

    static Stream<Arguments> testTwoProd() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = createRNG();
        for (int i = 0; i < SAMPLES; i++) {
            builder.add(Arguments.of(signedNormalDouble(rng), signedNormalDouble(rng)));
        }
        return builder.build();
    }

    /**
     * Test {@link DD#twoProd(double, double)} with cases that have non-normal input
     * or create intermediate overflow when splitting.
     */
    @Test
    void testTwoProdSpecialCases() {
        // x * y is sub-normal or zero
        assertProduct(0.0, 1.0, Math.nextDown(Double.MIN_NORMAL));
        assertProduct(0.0, -1.0, Math.nextDown(Double.MIN_NORMAL));
        assertProduct(0.0, 0x1.0p-512, 0x1.0p-512);
        // x or y is infinite or NaN
        assertProduct(Double.NaN, 1.0, Double.POSITIVE_INFINITY);
        assertProduct(Double.NaN, 1.0, Double.NEGATIVE_INFINITY);
        assertProduct(Double.NaN, 1.0, Double.NaN);
        // x * y is infinite
        assertProduct(Double.NaN, 0x1.0p511, 0x1.0p513);
        // |x| or |y| > ~2^997
        assertProduct(Double.NaN, 0.5, 0x1.0p997);
        assertProduct(Double.NaN, 0.5, Double.MAX_VALUE);
    }

    private static void assertProduct(double expectedLo, double x, double y) {
        final DD z = DD.ofProduct(x, y);
        Assertions.assertEquals(x * y, z.hi(), "hi");
        // Requires a delta of 0.0 to assert -0.0 == 0.0
        Assertions.assertEquals(expectedLo, z.lo(), 0.0, "lo");
        final DD z2 = DD.ofProduct(y, x);
        Assertions.assertEquals(z.hi(), z2.hi(), "y*x hi");
        Assertions.assertEquals(z.lo(), z2.lo(), "y*x lo");
    }

    @ParameterizedTest
    @MethodSource(value = {"testTwoProd"})
    void testTwoSquare(double x) {
        DD z;
        final BigDecimal bx = bd(x);
        final Supplier<String> msg = () -> String.format("%s^2", x);
        z = DD.ofSquare(x);
        final double hi = x * x;
        final double lo = bx.pow(2).subtract(bd(hi)).doubleValue();
        // two-square should be exact
        Assertions.assertEquals(hi, z.hi(), () -> msg.get() + " hi");
        Assertions.assertEquals(lo, z.lo(), () -> msg.get() + " lo");
        Assertions.assertEquals(hi, hi + lo, () -> msg.get() + " hi+lo");
        Assertions.assertEquals(hi, z.doubleValue(), () -> msg.get() + " doubleValue");
        z = DD.ofSquare(-x);
        Assertions.assertEquals(hi, z.hi(), () -> msg.get() + " (-x)^2 hi");
        Assertions.assertEquals(lo, z.lo(), () -> msg.get() + " (-x)^2 lo");
    }

    /**
     * Test {@link DD#twoSquare(double)} with cases that have non-normal input
     * or create intermediate overflow when splitting.
     */
    @Test
    void testTwoSquareSpecialCases() {
        // x * x is sub-normal or zero
        assertSquare(0.0, 0);
        assertSquare(0.0, Double.MIN_NORMAL);
        assertSquare(0.0, 0x1.0p-512);
        // x is infinite or NaN
        assertSquare(Double.NaN, Double.POSITIVE_INFINITY);
        assertSquare(Double.NaN, Double.NaN);
        // x * x is infinite
        assertSquare(Double.NaN, 0x1.0p512);
        // |x| > ~2^997
        assertSquare(Double.NaN, 0x1.0p997);
        assertSquare(Double.NaN, Double.MAX_VALUE);
    }

    private static void assertSquare(double expectedLo, double x) {
        final DD z = DD.ofSquare(x);
        Assertions.assertEquals(x * x, z.hi(), "hi");
        // Requires a delta of 0.0 to assert -0.0 == 0.0
        Assertions.assertEquals(expectedLo, z.lo(), 0.0, "lo");
        final DD z2 = DD.ofSquare(-x);
        Assertions.assertEquals(z.hi(), z2.hi(), "(-x)^2 hi");
        Assertions.assertEquals(z.lo(), z2.lo(), "(-x)^2 lo");
    }

    @ParameterizedTest
    @MethodSource
    void testQuotient(double x, double y) {
        DD s;
        final Supplier<String> msg = () -> String.format("%s/%s", x, y);

        s = DD.fromQuotient(x, y);
        // Check normalized
        final double hi = s.hi();
        final double lo = s.lo();
        Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue");

        final BigDecimal e = bd(x).divide(bd(y), MC_DIVIDE);
        // double-double has 106-bits precision.
        // This passes with a relative error of 2^-107.
        TestUtils.assertEquals(e, s, 0.5 * EPS, () -> msg.get());

        // Same as if low-part of x and y is zero
        s = DD.of(x).divide(DD.of(y));
        Assertions.assertEquals(hi, s.hi(), () -> msg.get() + " xx=yy=0 hi");
        Assertions.assertEquals(lo, s.lo(), () -> msg.get() + " xx=yy=0 lo");
    }

    static Stream<Arguments> testQuotient() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = createRNG();
        for (int i = 0; i < 3 * SAMPLES; i++) {
            builder.add(Arguments.of(signedNormalDouble(rng), signedNormalDouble(rng)));
        }
        return builder.build();
    }

    /**
     * Test {@link DD#fromQuotient(double, double)} with cases that have non-normal input
     * or create intermediate overflow when splitting.
     */
    @Test
    void testQuotientSpecialCases() {
        // x / y is sub-normal or zero
        assertQuotient(0.0, Double.MIN_NORMAL, 3);
        assertQuotient(0.0, Math.nextUp(Double.MIN_NORMAL), 2);
        assertQuotient(0.0, 0, 1);
        // x is infinite or NaN
        assertQuotient(Double.NaN, Double.POSITIVE_INFINITY, 1);
        assertQuotient(Double.NaN, Double.NEGATIVE_INFINITY, 1);
        assertQuotient(Double.NaN, Double.NaN, 1);
        // y is infinite (here low part could be zero if checks were added)
        assertQuotient(Double.NaN, 1.0, Double.POSITIVE_INFINITY);
        assertQuotient(Double.NaN, 1.0, Double.NEGATIVE_INFINITY);
        // y is nan
        assertQuotient(Double.NaN, 1.0, Double.NaN);
        // x / y is infinite
        assertQuotient(Double.NaN, 0x1.0p511, 0x1.0p-513);
        assertQuotient(Double.NaN, 1, Double.MIN_VALUE);
        // |x / y| > ~2^997
        assertQuotient(Double.NaN, 0x1.0p997, 0.5);
        assertQuotient(Double.NaN, Double.MAX_VALUE, 2);
        // |y| > ~2^997
        assertQuotient(Double.NaN, 0.5, 0x1.0p997);
        assertQuotient(Double.NaN, 2, Double.MAX_VALUE);
        // x / y is sub-normal or zero with intermediate overflow
        assertQuotient(Double.NaN, 0.5, Double.MAX_VALUE);
        assertQuotient(Double.NaN, Double.MIN_NORMAL, 0x1.0p997);
    }

    private static void assertQuotient(double expectedLo, double x, double y) {
        final DD z = DD.fromQuotient(x, y);
        Assertions.assertEquals(x / y, z.hi(), "hi");
        // Requires a delta of 0.0 to assert -0.0 == 0.0
        Assertions.assertEquals(expectedLo, z.lo(), 0.0, "lo");
    }

    @ParameterizedTest
    @MethodSource(value = {"addDouble"})
    void testAddDouble(double x, double xx, double y) {
        assertNormalized(x, xx, "x");
        final DD dd = DD.of(x, xx);
        DD s;
        final BigDecimal bx = bd(x).add(bd(xx));
        final Supplier<String> msg = () -> String.format("(%s,%s)+%s", x, xx, y);
        for (final double scale : SCALES) {
            final double sy = scale * y;
            s = dd.add(sy);
            // Check normalized
            final double hi = s.hi();
            final double lo = s.lo();
            Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue: scale=" + scale);

            final BigDecimal e = bx.add(bd(sy));
            // double-double addition should be within 4 eps^2 with eps = 2^-53.
            // A single addition is 2 eps^2.
            TestUtils.assertEquals(e, s, 2.0 * EPS, () -> msg.get() + " scale=" + scale);

            // Same as if low-part of y is zero
            s = dd.add(DD.of(sy));
            Assertions.assertEquals(hi, s.hi(), () -> msg.get() + " yy=0 hi: scale=" + scale);
            Assertions.assertEquals(lo, s.lo(), () -> msg.get() + " yy=0 lo: scale=" + scale);
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"addDouble"})
    void testAccurateAddDouble(double x, double xx, double y) {
        assertNormalized(x, xx, "x");
        final DD dd = DD.of(x, xx);
        DD s;
        final BigDecimal bx = bd(x).add(bd(xx));
        final Supplier<String> msg = () -> String.format("(%s,%s)+%s", x, xx, y);
        for (final double scale : SCALES) {
            final double sy = scale * y;
            s = DDExt.add(dd, sy);
            // Check normalized
            final double hi = s.hi();
            final double lo = s.lo();
            Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue: scale=" + scale);

            final BigDecimal e = bx.add(bd(sy));
            // double-double addition should be within 4 eps^2 with eps = 2^-53
            // Passes at 1 eps^2.
            // Note:
            // It may be sporadically failed by add as data is random. The test could be updated
            // to assert the RMS relative error of accurateAdd is lower than add.
            TestUtils.assertEquals(e, s, EPS, () -> msg.get() + " scale=" + scale);

            // Additional checks for accurateAdd:
            // (Note: These are failed by add for cases of large cancellation, or
            // non-overlapping addends. For reasonable cases the lo-part is within 4 ulp.)
            // e = full expansion series of m numbers, low suffix is smallest
            // |e - e_m| <= ulp(e_m) -> hi is a 1 ULP approximation to the IEEE double result
            TestUtils.assertEquals(e.doubleValue(), hi, 1, () -> msg.get() + " hi: scale=" + scale);
            // |sum_i^{m-1} (e_i)| <= ulp(e - e_m)
            final double esem = e.subtract(bd(hi)).doubleValue();
            TestUtils.assertEquals(esem, lo, 1, () -> msg.get() + " lo: scale=" + scale);

            // Same as if low-part of y is zero
            s = DDExt.add(dd, DD.of(sy));
            Assertions.assertEquals(hi, s.hi(), () -> msg.get() + " yy=0 hi: scale=" + scale);
            Assertions.assertEquals(lo, s.lo(), () -> msg.get() + " yy=0 lo: scale=" + scale);
        }
    }

    static Stream<Arguments> addDouble() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = createRNG();
        DD s;
        for (int i = 0; i < SAMPLES; i++) {
            s = signedNormalDoubleDouble(rng);
            builder.add(Arguments.of(s.hi(), s.lo(), signedNormalDouble(rng)));
        }
        // Cases of large cancellation
        for (int i = 0; i < 10; i++) {
            s = signedNormalDoubleDouble(rng);
            final double x = s.hi();
            final double xx = s.lo();
            final int dir = rng.nextBoolean() ? 1 : -1;
            final double ulp = Math.ulp(x);
            builder.add(Arguments.of(x, xx, -x));
            builder.add(Arguments.of(x, xx, -(x + dir * ulp)));
            builder.add(Arguments.of(x, xx, -(x + dir * ulp * 2)));
            builder.add(Arguments.of(x, xx, -(x + dir * ulp * 3)));
        }
        // Cases requiring correct rounding of low
        for (int i = 0; i < 10; i++) {
            s = signedNormalDoubleDouble(rng);
            final double x = s.hi();
            final double xx = s.lo();
            final double hUlpXX = Math.ulp(xx) / 2;
            final double hUlpXXu = Math.nextUp(hUlpXX);
            final double hUlpXXd = Math.nextDown(hUlpXX);
            builder.add(Arguments.of(x, xx, hUlpXX));
            builder.add(Arguments.of(x, xx, -hUlpXX));
            builder.add(Arguments.of(x, xx, hUlpXXu));
            builder.add(Arguments.of(x, xx, -hUlpXXu));
            builder.add(Arguments.of(x, xx, hUlpXXd));
            builder.add(Arguments.of(x, xx, -hUlpXXd));
        }
        // Create a summation of non-overlapping parts
        for (int i = 0; i < 10; i++) {
            final double x = signedNormalDouble(rng);
            final double xx = Math.ulp(x) / 2;
            final double y = Math.ulp(xx) / 2;
            final double y1 = Math.nextUp(y);
            final double y2 = Math.nextDown(y);
            s = DD.twoSum(x, xx);
            builder.add(Arguments.of(s.hi(), s.lo(), y));
            builder.add(Arguments.of(s.hi(), s.lo(), -y));
            builder.add(Arguments.of(s.hi(), s.lo(), y1));
            builder.add(Arguments.of(s.hi(), s.lo(), -y1));
            builder.add(Arguments.of(s.hi(), s.lo(), y2));
            builder.add(Arguments.of(s.hi(), s.lo(), -y2));
            s = DD.twoSum(x, Math.nextUp(xx));
            builder.add(Arguments.of(s.hi(), s.lo(), y));
            builder.add(Arguments.of(s.hi(), s.lo(), -y));
            builder.add(Arguments.of(s.hi(), s.lo(), y1));
            builder.add(Arguments.of(s.hi(), s.lo(), -y1));
            builder.add(Arguments.of(s.hi(), s.lo(), y2));
            builder.add(Arguments.of(s.hi(), s.lo(), -y2));
            s = DD.twoSum(x, Math.nextDown(xx));
            builder.add(Arguments.of(s.hi(), s.lo(), y));
            builder.add(Arguments.of(s.hi(), s.lo(), -y));
            builder.add(Arguments.of(s.hi(), s.lo(), y1));
            builder.add(Arguments.of(s.hi(), s.lo(), -y1));
            builder.add(Arguments.of(s.hi(), s.lo(), y2));
            builder.add(Arguments.of(s.hi(), s.lo(), -y2));
        }

        // Fails add at 1.35 * eps
        builder.add(Arguments.of(1.2175415583363745, -9.201751961322592E-17, -1.2175415583363745));
        // Fails add at 1.61 * eps
        builder.add(Arguments.of(1.0559874267393727, 7.55723980093395E-17, -1.7469209528242222));
        // Fails add at 1.59 * eps
        builder.add(Arguments.of(1.1187969556288173, -5.666077383672716E-17, -1.9617226734248885));

        // Taken from addDoubleDouble.
        // Cases that fail exact summation to a single double if performed incorrectly.
        // These require correct propagation of the round-off to the high-part.
        // The double-double high-part may not be exact but summed with the low-part
        // it should be <= 0.5 ulp of the IEEE double result.
        builder.add(Arguments.of(-1.8903599998005227, 1.2825149462328469E-17, 1.8903599998005232, 1.2807862928011876E-17));
        builder.add(Arguments.of(1.8709715815417154, 2.542250988259237E-17, -1.8709715815417152, 1.982876215341407E-17));
        builder.add(Arguments.of(-1.8246677074340567, 2.158144877411339E-17, 1.8246677074340565, 2.043199561107511E-17));

        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"addDoubleDouble"})
    void testAddDoubleDouble(double x, double xx, double y, double yy) {
        assertNormalized(x, xx, "x");
        assertNormalized(y, yy, "y");
        final DD dd = DD.of(x, xx);
        DD s;
        final BigDecimal bx = bd(x).add(bd(xx));
        final Supplier<String> msg = () -> String.format("(%s,%s)+(%s,%s)", x, xx, y, yy);
        for (final double scale : SCALES) {
            final DD sy = DD.of(scale * y, scale * yy);
            s = dd.add(sy);
            // Check normalized
            final double hi = s.hi();
            final double lo = s.lo();
            Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue: scale=" + scale);

            final BigDecimal e = bx.add(bd(scale * y)).add(bd(scale * yy));
            // double-double addition should be within 4 eps^2 with eps = 2^-53.
            // Passes at 3 eps^2.
            TestUtils.assertEquals(e, s, 3 * EPS, () -> msg.get() + " scale=" + scale);

            // Same if reversed
            s = sy.add(dd);
            Assertions.assertEquals(hi, s.hi(), () -> msg.get() + "reversed hi: scale=" + scale);
            Assertions.assertEquals(lo, s.lo(), () -> msg.get() + "reversed lo: scale=" + scale);
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"addDoubleDouble"})
    void testAccurateAddDoubleDouble(double x, double xx, double y, double yy) {
        assertNormalized(x, xx, "x");
        assertNormalized(y, yy, "y");
        final DD dd = DD.of(x, xx);
        DD s;
        final BigDecimal bx = bd(x).add(bd(xx));
        final Supplier<String> msg = () -> String.format("(%s,%s)+(%s,%s)", x, xx, y, yy);
        for (final double scale : SCALES) {
            final DD sy = DD.of(scale * y, scale * yy);
            s = DDExt.add(dd, sy);
            // Check normalized
            final double hi = s.hi();
            final double lo = s.lo();
            Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue: scale=" + scale);

            final BigDecimal e = bx.add(bd(scale * y)).add(bd(scale * yy));
            // double-double addition should be within 4 eps^2 with eps = 2^-53.
            // Note that the extra computation in add vs accurateAdd
            // lowers the tolerance to eps^2. This tolerance is consistently failed by add.
            TestUtils.assertEquals(e, s, EPS, () -> msg.get() + " scale=" + scale);

            // Additional checks for accurateAdd.
            // (Note: These are failed by add for cases of large cancellation, or
            // non-overlapping addends. For reasonable cases the lo-part is within 4 ulp.
            // Thus add creates a double-double that is a better estimate of the first two
            // terms of the full expansion of e.)
            // e = full expansion series of m numbers, low suffix is smallest
            // |e - e_m| <= ulp(e_m) -> hi is a 1 ULP approximation to the IEEE double result
            TestUtils.assertEquals(e.doubleValue(), hi, 1, () -> msg.get() + " hi: scale=" + scale);
            // |sum_i^{m-1} (e_i)| <= ulp(e - e_m)
            final double esem = e.subtract(bd(hi)).doubleValue();
            TestUtils.assertEquals(esem, lo, 1, () -> msg.get() + " lo: scale=" + scale);

            // Same if reversed
            s = DDExt.add(sy, dd);
            Assertions.assertEquals(hi, s.hi(), () -> msg.get() + "reversed hi: scale=" + scale);
            Assertions.assertEquals(lo, s.lo(), () -> msg.get() + "reversed lo: scale=" + scale);
        }
    }

    static Stream<Arguments> addDoubleDouble() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = createRNG();
        DD s;
        for (int i = 0; i < SAMPLES; i++) {
            s = signedNormalDoubleDouble(rng);
            final DD t = signedNormalDoubleDouble(rng);
            builder.add(Arguments.of(s.hi(), s.lo(), t.hi(), t.lo()));
        }
        // Cases of large cancellation
        for (int i = 0; i < 10; i++) {
            s = signedNormalDoubleDouble(rng);
            final double x = s.hi();
            final double xx = s.lo();
            final int dir = rng.nextBoolean() ? 1 : -1;
            final double ulp = Math.ulp(x);
            double yy = signedNormalDouble(rng);
            yy = Math.scalb(yy, Math.getExponent(xx));
            add(builder, s, -x, -xx);
            add(builder, s, -x, -(xx + dir * ulp));
            add(builder, s, -x, -(xx + dir * ulp * 2));
            add(builder, s, -x, -(xx + dir * ulp * 3));
            add(builder, s, -x, yy);
            add(builder, s, -x, yy + ulp);
            add(builder, s, -(x + dir * ulp), -xx);
            add(builder, s, -(x + dir * ulp), -(xx + dir * ulp));
            add(builder, s, -(x + dir * ulp), -(xx + dir * ulp * 2));
            add(builder, s, -(x + dir * ulp), -(xx + dir * ulp * 3));
            add(builder, s, -(x + dir * ulp), yy);
            add(builder, s, -(x + dir * ulp), yy + ulp);
        }
        // Cases requiring correct rounding of low
        for (int i = 0; i < 10; i++) {
            s = signedNormalDoubleDouble(rng);
            final double x = s.hi();
            final double xx = s.lo();
            final int dir = rng.nextBoolean() ? 1 : -1;
            final double ulpX = Math.ulp(x);
            final double hUlpXX = Math.ulp(xx) / 2;
            final double hUlpXXu = Math.nextUp(hUlpXX);
            final double hUlpXXd = Math.nextDown(hUlpXX);
            add(builder, s, -x,  hUlpXX);
            add(builder, s, -x, -hUlpXX);
            add(builder, s, -x, hUlpXXu);
            add(builder, s, -x, -hUlpXXu);
            add(builder, s, -x, hUlpXXd);
            add(builder, s, -x, -hUlpXXd);
            add(builder, s, -(x + dir * ulpX),  hUlpXX);
            add(builder, s, -(x + dir * ulpX), -hUlpXX);
            add(builder, s, -(x + dir * ulpX), hUlpXXu);
            add(builder, s, -(x + dir * ulpX), -hUlpXXu);
            add(builder, s, -(x + dir * ulpX), hUlpXXd);
            add(builder, s, -(x + dir * ulpX), -hUlpXXd);
        }
        // Create a summation of non-overlapping parts
        for (int i = 0; i < 10; i++) {
            final double x = signedNormalDouble(rng);
            final double xx = Math.ulp(x) / 2;
            final double y = Math.ulp(xx) / 2;
            final double yy = Math.ulp(y) / 2;
            final double yy1 = Math.nextUp(yy);
            final double yy2 = Math.nextDown(yy);
            s = DD.twoSum(x, xx);
            add(builder, s, y, yy);
            add(builder, s, y, -yy);
            add(builder, s, y, yy1);
            add(builder, s, y, -yy1);
            add(builder, s, y, yy2);
            add(builder, s, y, -yy2);
            add(builder, s, -y, yy);
            add(builder, s, -y, -yy);
            add(builder, s, -y, yy1);
            add(builder, s, -y, -yy1);
            add(builder, s, -y, yy2);
            add(builder, s, -y, -yy2);
            s = DD.twoSum(x, Math.nextUp(xx));
            add(builder, s, y, yy);
            add(builder, s, y, -yy);
            add(builder, s, y, yy1);
            add(builder, s, y, -yy1);
            add(builder, s, y, yy2);
            add(builder, s, y, -yy2);
            add(builder, s, -y, yy);
            add(builder, s, -y, -yy);
            add(builder, s, -y, yy1);
            add(builder, s, -y, -yy1);
            add(builder, s, -y, yy2);
            add(builder, s, -y, -yy2);
            s = DD.twoSum(x, Math.nextDown(xx));
            add(builder, s, y, yy);
            add(builder, s, y, -yy);
            add(builder, s, y, yy1);
            add(builder, s, y, -yy1);
            add(builder, s, y, yy2);
            add(builder, s, y, -yy2);
            add(builder, s, -y, yy);
            add(builder, s, -y, -yy);
            add(builder, s, -y, yy1);
            add(builder, s, -y, -yy1);
            add(builder, s, -y, yy2);
            add(builder, s, -y, -yy2);
        }

        // Fails add at 2.04 * eps
        builder.add(Arguments.of(1.936367217696177, -5.990222602369122E-17, -1.0983273763870391, -8.300320179242541E-17));
        // Fails add at 2.28 * eps
        builder.add(Arguments.of(1.8570239083555447, 9.484019916269656E-17, -1.0125292773654282, 8.247363448814862E-17));

        // Cases that fail exact summation to a single double if performed incorrectly.
        // These require correct propagation of the round-off to the high-part.
        // The double-double high-part may not be exact but summed with the low-part
        // it should be <= 0.5 ulp of the IEEE double result.
        builder.add(Arguments.of(-1.8903599998005227, 1.2825149462328469E-17, 1.8903599998005232, 1.2807862928011876E-17));
        builder.add(Arguments.of(1.8709715815417154, 2.542250988259237E-17, -1.8709715815417152, 1.982876215341407E-17));
        builder.add(Arguments.of(-1.8246677074340567, 2.158144877411339E-17, 1.8246677074340565, 2.043199561107511E-17));
        return builder.build();
    }

    /**
     * Adds the two double-double numbers as arguments. Ensured the (x,yy) values is normalized.
     * The argument {@code t} is used for working.
     */
    private static Stream.Builder<Arguments> add(Stream.Builder<Arguments> builder,
            DD x, double y, double yy) {
        final DD t = DD.fastTwoSum(y, yy);
        builder.add(Arguments.of(x.hi(), x.lo(), t.hi(), t.lo()));
        return builder;
    }

    // Subtraction must be consistent with addition

    @ParameterizedTest
    @MethodSource(value = {"addDouble"})
    void testSubtractDouble(double x, double xx, double y) {
        assertNormalized(x, xx, "x");
        final DD dd = DD.of(x, xx);
        DD s;
        DD t;
        final Supplier<String> msg = () -> String.format("(%s,%s)-%s", x, xx, y);
        for (final double scale : SCALES) {
            final double sy = scale * y;
            s = dd.subtract(sy);
            t = dd.add(-sy);
            Assertions.assertEquals(t.hi(), s.hi(), () -> msg.get() + " hi: scale=" + scale);
            Assertions.assertEquals(t.lo(), s.lo(), () -> msg.get() + " lo: scale=" + scale);
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"addDouble"})
    void testAccurateSubtractDouble(double x, double xx, double y) {
        assertNormalized(x, xx, "x");
        final DD dd = DD.of(x, xx);
        DD s;
        DD t;
        final Supplier<String> msg = () -> String.format("(%s,%s)-%s", x, xx, y);
        for (final double scale : SCALES) {
            final double sy = scale * y;
            s = DDExt.subtract(dd, sy);
            t = DDExt.add(dd, -sy);
            Assertions.assertEquals(t.hi(), s.hi(), () -> msg.get() + " hi: scale=" + scale);
            Assertions.assertEquals(t.lo(), s.lo(), () -> msg.get() + " lo: scale=" + scale);
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"addDoubleDouble"})
    void testSubtractDoubleDouble(double x, double xx, double y, double yy) {
        assertNormalized(x, xx, "x");
        assertNormalized(y, yy, "y");
        final DD dd = DD.of(x, xx);
        DD s;
        DD t;
        final Supplier<String> msg = () -> String.format("(%s,%s)-(%s,%s)", x, xx, y, yy);
        for (final double scale : SCALES) {
            final DD sy = DD.of(scale * y, scale * yy);
            s = dd.subtract(sy);
            t = dd.add(sy.negate());
            Assertions.assertEquals(t.hi(), s.hi(), () -> msg.get() + " hi: scale=" + scale);
            Assertions.assertEquals(t.lo(), s.lo(), () -> msg.get() + " lo: scale=" + scale);
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"addDoubleDouble"})
    void testAccurateSubtractDoubleDouble(double x, double xx, double y, double yy) {
        assertNormalized(x, xx, "x");
        assertNormalized(y, yy, "y");
        final DD dd = DD.of(x, xx);
        DD s;
        DD t;
        final Supplier<String> msg = () -> String.format("(%s,%s)-(%s,%s)", x, xx, y, yy);
        for (final double scale : SCALES) {
            final DD sy = DD.of(scale * y, scale * yy);
            s = DDExt.subtract(dd, sy);
            t = DDExt.add(dd, sy.negate());
            Assertions.assertEquals(t.hi(), s.hi(), () -> msg.get() + " hi: scale=" + scale);
            Assertions.assertEquals(t.lo(), s.lo(), () -> msg.get() + " lo: scale=" + scale);
        }
    }

    @ParameterizedTest
    @MethodSource
    void testMultiplyDouble(double x, double xx, double y) {
        assertNormalized(x, xx, "x");
        final DD dd = DD.of(x, xx);
        DD s;
        final Supplier<String> msg = () -> String.format("(%s,%s)*%s", x, xx, y);

        s = dd.multiply(y);
        // Check normalized
        final double hi = s.hi();
        final double lo = s.lo();
        Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue");

        final BigDecimal e = bd(x).add(bd(xx)).multiply(bd(y));
        // double-double multiplication should be within 16 eps^2 with eps = 2^-53.
        // a single multiply is 4 eps^2
        TestUtils.assertEquals(e, s, 4 * EPS, () -> msg.get());

        // Same as if low-part of y is zero
        s = dd.multiply(DD.of(y));
        // Handle edge case of y==0 where the sign can be different
        if (y == 0) {
            Assertions.assertEquals(hi, s.hi(), 0.0, () -> msg.get() + "y=0 yy=0 hi");
            Assertions.assertEquals(lo, s.lo(), 0.0, () -> msg.get() + "y=0 yy=0 lo");
        } else {
            Assertions.assertEquals(hi, s.hi(), () -> msg.get() + " yy=0 hi");
            Assertions.assertEquals(lo, s.lo(), () -> msg.get() + " yy=0 lo");
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"testMultiplyDouble"})
    void testAccurateMultiplyDouble(double x, double xx, double y) {
        assertNormalized(x, xx, "x");
        DD s;
        final Supplier<String> msg = () -> String.format("(%s,%s)*%s", x, xx, y);

        s = DDExt.multiply(DD.of(x, xx), y);
        // Check normalized
        final double hi = s.hi();
        final double lo = s.lo();
        Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue");

        final BigDecimal e = bd(x).add(bd(xx)).multiply(bd(y));
        // double-double multiplication should be within 16 eps^2 with eps = 2^-53.
        // a single multiply is 0.5 eps^2
        TestUtils.assertEquals(e, s, 0.5 * EPS, () -> msg.get());

        // Same as if low-part of y is zero
        s = DDExt.multiply(DD.of(x, xx), DD.of(y));
        // Handle edge case of y==0 where the sign can be different
        if (y == 0) {
            Assertions.assertEquals(hi, s.hi(), 0.0, () -> msg.get() + "y=0 yy=0 hi");
            Assertions.assertEquals(lo, s.lo(), 0.0, () -> msg.get() + "y=0 yy=0 lo");
        } else {
            Assertions.assertEquals(hi, s.hi(), () -> msg.get() + " yy=0 hi");
            Assertions.assertEquals(lo, s.lo(), () -> msg.get() + " yy=0 lo");
        }
    }

    static Stream<Arguments> testMultiplyDouble() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = createRNG();
        for (int i = 0; i < 3 * SAMPLES; i++) {
            final DD s = signedNormalDoubleDouble(rng);
            builder.add(Arguments.of(s.hi(), s.lo(), signedNormalDouble(rng)));
        }
        // Multiply by zero
        for (int i = 0; i < 3; i++) {
            final DD s = signedNormalDoubleDouble(rng);
            builder.add(Arguments.of(s.hi(), s.lo(), 0.0));
            builder.add(Arguments.of(s.hi(), s.lo(), -0.0));
        }
        return builder.build();
    }

    /**
     * Test multiply by an int.
     * This method exists to support the {@link NativeOperators} interface.
     */
    @ParameterizedTest
    @MethodSource
    void testMultiplyInt(double x, double xx, int y) {
        assertNormalized(x, xx, "x");
        final DD dd = DD.of(x, xx);
        final Supplier<String> msg = () -> String.format("(%s,%s)*%d", x, xx, y);

        // Test this is consistent with multiplication by a double
        final DD s = dd.multiply(y);
        final DD t = dd.multiply((double) y);
        Assertions.assertEquals(t.hi(), s.hi(), () -> msg.get() + " hi");
        Assertions.assertEquals(t.lo(), s.lo(), () -> msg.get() + " lo");
    }

    static Stream<Arguments> testMultiplyInt() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = createRNG();
        for (int i = 0; i < 3 * SAMPLES; i++) {
            final DD s = signedNormalDoubleDouble(rng);
            builder.add(Arguments.of(s.hi(), s.lo(), rng.nextInt()));
        }
        // Multiply by zero
        for (int i = 0; i < 3; i++) {
            final DD s = signedNormalDoubleDouble(rng);
            builder.add(Arguments.of(s.hi(), s.lo(), 0));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testMultiplyDoubleDouble(double x, double xx, double y, double yy) {
        assertNormalized(x, xx, "x");
        assertNormalized(y, yy, "y");
        final DD dx = DD.of(x, xx);
        final DD dy = DD.of(y, yy);
        DD s;
        final Supplier<String> msg = () -> String.format("(%s,%s)*(%s,%s)", x, xx, y, yy);

        s = dx.multiply(dy);
        // Check normalized
        final double hi = s.hi();
        final double lo = s.lo();
        Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue");

        final BigDecimal e = bd(x).add(bd(xx)).multiply(bd(y).add(bd(yy)));
        // double-double multiplication should be within 16 eps^2 with eps = 2^-53.
        // This passes at 4 eps^2
        TestUtils.assertEquals(e, s, 4 * EPS, () -> msg.get());

        // Same if reversed
        s = dy.multiply(dx);
        Assertions.assertEquals(hi, s.hi(), () -> msg.get() + " reversed hi");
        Assertions.assertEquals(lo, s.lo(), () -> msg.get() + " reversed lo");
    }

    @ParameterizedTest
    @MethodSource(value = {"testMultiplyDoubleDouble"})
    void testAccurateMultiplyDoubleDouble(double x, double xx, double y, double yy) {
        assertNormalized(x, xx, "x");
        assertNormalized(y, yy, "y");
        DD s;
        final Supplier<String> msg = () -> String.format("(%s,%s)*(%s,%s)", x, xx, y, yy);

        s = DDExt.multiply(DD.of(x, xx), DD.of(y, yy));
        // Check normalized
        final double hi = s.hi();
        final double lo = s.lo();
        Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue");

        final BigDecimal e = bd(x).add(bd(xx)).multiply(bd(y).add(bd(yy)));
        // This passes at 0.5 eps^2
        TestUtils.assertEquals(e, s, 0.5 * EPS, () -> msg.get());

        // Same if reversed
        s = DDExt.multiply(DD.of(y, yy), DD.of(x, xx));
        Assertions.assertEquals(hi, s.hi(), () -> msg.get() + " reversed hi");
        Assertions.assertEquals(lo, s.lo(), () -> msg.get() + " reversed lo");
    }

    static Stream<Arguments> testMultiplyDoubleDouble() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = createRNG();
        for (int i = 0; i < 3 * SAMPLES; i++) {
            final DD s = signedNormalDoubleDouble(rng);
            final DD t = signedNormalDoubleDouble(rng);
            builder.add(Arguments.of(s.hi(), s.lo(), t.hi(), t.lo()));
        }
        // Multiply by zero
        for (int i = 0; i < 5; i++) {
            final DD s = signedNormalDoubleDouble(rng);
            builder.add(Arguments.of(s.hi(), s.lo(), 0.0, 0.0));
            builder.add(Arguments.of(s.hi(), s.lo(), -0.0, 0.0));
            builder.add(Arguments.of(s.hi(), s.lo(), 0.0, -0.0));
            builder.add(Arguments.of(s.hi(), s.lo(), -0.0, -0.0));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testSquare(double x, double xx) {
        assertNormalized(x, xx, "x");
        final DD dd = DD.of(x, xx);
        DD s;
        final Supplier<String> msg = () -> String.format("(%s,%s)^2", x, xx);

        s = dd.square();
        final DD t = dd.multiply(dd);
        // Consistent with multiply
        final double hi = t.hi();
        final double lo = t.lo();
        Assertions.assertEquals(hi, s.hi(), () -> msg.get() + " hi");
        Assertions.assertEquals(lo, s.lo(), () -> msg.get() + " lo");

        s = dd.negate().square();
        Assertions.assertEquals(hi, s.hi(), () -> "negated " + msg.get() + " hi");
        Assertions.assertEquals(lo, s.lo(), () -> "negated " + msg.get() + " lo");
    }

    @ParameterizedTest
    @MethodSource(value = {"testSquare"})
    void testAccurateSquare(double x, double xx) {
        assertNormalized(x, xx, "x");
        DD s;
        final Supplier<String> msg = () -> String.format("(%s,%s)^2", x, xx);

        s = DDExt.square(DD.of(x, xx));
        final DD t = DDExt.multiply(DD.of(x, xx), DD.of(x, xx));
        // Consistent with multiply
        final double hi = t.hi();
        final double lo = t.lo();
        Assertions.assertEquals(hi, s.hi(), () -> msg.get() + " hi");
        Assertions.assertEquals(lo, s.lo(), () -> msg.get() + " lo");

        s = DDExt.square(DD.of(x, xx).negate());
        Assertions.assertEquals(hi, s.hi(), () -> "negated " + msg.get() + " hi");
        Assertions.assertEquals(lo, s.lo(), () -> "negated " + msg.get() + " lo");
    }

    static Stream<Arguments> testSquare() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = createRNG();
        for (int i = 0; i < 3 * SAMPLES; i++) {
            final DD s = signedNormalDoubleDouble(rng);
            builder.add(Arguments.of(s.hi(), s.lo()));
        }
        // Square zero
        builder.add(Arguments.of(0.0, 0.0));
        builder.add(Arguments.of(0.0, -0.0));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"testDivideDoubleDouble"})
    void testDivideDouble(double x, double xx, double y) {
        assertNormalized(x, xx, "x");
        final Supplier<String> msg = () -> String.format("(%s,%s)/%s", x, xx, y);

        DD s = DD.of(x, xx).divide(y);
        // Check normalized
        final double hi = s.hi();
        final double lo = s.lo();
        Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue");

        final BigDecimal e = bd(x).add(bd(xx)).divide(bd(y), MC_DIVIDE);
        // double-double multiplication should be within 16 eps^2 with eps = 2^-53.
        // This passes with a relative error of 2^-107.
        TestUtils.assertEquals(e, s, 0.5 * EPS, () -> msg.get());

        // Same as if low-part of y is zero
        s = DD.of(x, xx).divide(DD.of(y));
        Assertions.assertEquals(hi, s.hi(), () -> msg.get() + " yy=0 hi");
        Assertions.assertEquals(lo, s.lo(), () -> msg.get() + " yy=0 lo");
    }

    @ParameterizedTest
    @MethodSource
    void testDivideDoubleDouble(double x, double xx, double y, double yy) {
        assertNormalized(x, xx, "x");
        assertNormalized(y, yy, "y");
        final Supplier<String> msg = () -> String.format("(%s,%s)/(%s,%s)", x, xx, y, yy);

        final DD s = DD.of(x, xx).divide(DD.of(y, yy));
        // Check normalized
        final double hi = s.hi();
        Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue");

        final BigDecimal e = bd(x).add(bd(xx)).divide(bd(y).add(bd(yy)), MC_DIVIDE);
        // This passes at 3 eps^2
        TestUtils.assertEquals(e, s, 3 * EPS, () -> msg.get());
    }

    @ParameterizedTest
    @MethodSource(value = {"testDivideDoubleDouble"})
    void testAccurateDivideDoubleDouble(double x, double xx, double y, double yy) {
        assertNormalized(x, xx, "x");
        assertNormalized(y, yy, "y");
        final Supplier<String> msg = () -> String.format("(%s,%s)/(%s,%s)", x, xx, y, yy);

        final DD s = DDExt.divide(DD.of(x, xx), DD.of(y, yy));
        // Check normalized
        final double hi = s.hi();
        Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue");

        final BigDecimal e = bd(x).add(bd(xx)).divide(bd(y).add(bd(yy)), MC_DIVIDE);
        // This passes at 1 eps^2
        TestUtils.assertEquals(e, s, EPS, () -> msg.get());
    }

    static Stream<Arguments> testDivideDoubleDouble() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = createRNG();
        for (int i = 0; i < 3 * SAMPLES; i++) {
            final DD s = signedNormalDoubleDouble(rng);
            final DD t = signedNormalDoubleDouble(rng);
            builder.add(Arguments.of(s.hi(), s.lo(), t.hi(), t.lo()));
        }

        // Fails at 2.233 * eps^2
        builder.add(Arguments.of(-1.4146588987981588, -7.11198841676502E-17, 1.0758443723302153, -1.0505084507177448E-16));
        // Fails at 2.947 * eps^2
        builder.add(Arguments.of(1.1407702754659819, 9.767718050897088E-17, -1.0034167436917367, 1.0394836915953897E-16));

        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testReciprocalDoubleDouble(double y, double yy) {
        assertNormalized(y, yy, "y");
        final Supplier<String> msg = () -> String.format("1/(%s,%s)", y, yy);
        DD s;

        s = DD.of(y, yy).reciprocal();
        // Check normalized
        final double hi = s.hi();
        final double lo = s.lo();
        Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue");

        final BigDecimal e = BigDecimal.ONE.divide(bd(y).add(bd(yy)), MC_DIVIDE);
        // double-double has 106-bits precision.
        // This passes with a relative error of 2^-105.
        TestUtils.assertEquals(e, s, 2 * EPS, () -> msg.get());

        // Same as if using divide
        s = DD.ONE.divide(DD.of(y, yy));
        Assertions.assertEquals(hi, s.hi(), () -> msg.get() + " (x,xx)=(1,0) hi");
        Assertions.assertEquals(lo, s.lo(), () -> msg.get() + " (x,xx)=(1,0) lo");
    }

    @ParameterizedTest
    @MethodSource(value = {"testReciprocalDoubleDouble"})
    void testAccurateReciprocalDoubleDouble(double y, double yy) {
        assertNormalized(y, yy, "y");
        final Supplier<String> msg = () -> String.format("1/(%s,%s)", y, yy);
        DD s;

        s = DDExt.reciprocal(DD.of(y, yy));
        // Check normalized
        final double hi = s.hi();
        final double lo = s.lo();
        Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue");

        final BigDecimal e = BigDecimal.ONE.divide(bd(y).add(bd(yy)), MC_DIVIDE);
        // double-double has 106-bits precision.
        // This passes with a relative error of 2^-106.
        TestUtils.assertEquals(e, s, EPS, () -> msg.get());

        // Same as if using divide
        s = DDExt.divide(DD.ONE, DD.of(y, yy));
        Assertions.assertEquals(hi, s.hi(), () -> msg.get() + " (x,xx)=(1,0) hi");
        Assertions.assertEquals(lo, s.lo(), () -> msg.get() + " (x,xx)=(1,0) lo");
    }

    static Stream<Arguments> testReciprocalDoubleDouble() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = createRNG();
        DD s;
        for (int i = 0; i < SAMPLES; i++) {
            s = signedNormalDoubleDouble(rng);
            builder.add(Arguments.of(s.hi(), s.lo()));
        }
        return builder.build();
    }

    @Test
    void testInfiniteOperationsCreateNaN() {
        // Demonstrate that operations on inf creates NaN.
        // For this reason special handling of single operations to return the IEEE correct result
        // for overflow are not be required. It is possible to include a multiply that is safe
        // against intermediate overflow. But this may never be used. Instead the class is
        // documented as unsuitable for computations that approach +/- inf, and documented
        // that the multiply is safe when the exponent is < 996.
        for (final DD x : new DD[] {DD.of(Double.POSITIVE_INFINITY)}) {
            for (final DD a : new DD[] {DD.ZERO, DD.ONE}) {
                assertNaN(x.add(a), () -> String.format("%s.add(%s)", x, a));
                assertNaN(x.add(a), () -> String.format("%s.add(%s)", x, a));
                assertNaN(x.add(a), () -> String.format("%s.multiply(%s)", x, a));
            }
            for (final double a : new double[] {0, 1}) {
                assertNaN(x.add(a), () -> String.format("%s.add(%s)", x, a));
                assertNaN(x.add(a), () -> String.format("%s.add(%s)", x, a));
                assertNaN(x.add(a), () -> String.format("%s.multiply(%s)", x, a));
            }
        }
    }

    /**
     * Assert both parts of the DD are NaN.
     */
    private static void assertNaN(DD x, Supplier<String> msg) {
        Assertions.assertEquals(Double.NaN, x.hi(), () -> "hi " + msg.get());
        Assertions.assertEquals(Double.NaN, x.lo(), () -> "lo " + msg.get());
    }

    @ParameterizedTest
    @MethodSource
    void testSqrtSpecialCases(double x, double xx, double hi, double lo) {
        final DD z = DD.of(x, xx).sqrt();
        Assertions.assertEquals(hi, z.hi(), "hi");
        Assertions.assertEquals(lo, z.lo(), "lo");
    }

    @ParameterizedTest
    @MethodSource(value = {"testSqrtSpecialCases"})
    void testAccurateSqrtSpecialCases(double x, double xx, double hi, double lo) {
        final DD z = DDExt.sqrt(DD.of(x, xx));
        Assertions.assertEquals(hi, z.hi(), "hi");
        Assertions.assertEquals(lo, z.lo(), "lo");
    }

    static Stream<Arguments> testSqrtSpecialCases() {
        // Note: Cases for non-normalized numbers are not supported
        // (these are commented out using ///).
        // The method assumes |x| > |xx|.

        final Stream.Builder<Arguments> builder = Stream.builder();
        final double inf = Double.POSITIVE_INFINITY;
        ///final double max = Double.MAX_VALUE;
        final double nan = Double.NaN;
        builder.add(Arguments.of(1, 0, 1, 0));
        builder.add(Arguments.of(4, 0, 2, 0));
        ///builder.add(Arguments.of(0, 1, 1, 0));
        // x+xx is NaN
        builder.add(Arguments.of(nan, 3, nan, 0));
        // x+xx is negative
        builder.add(Arguments.of(-1, 0, nan, 0));
        builder.add(Arguments.of(-inf, 3, nan, 0));
        ///builder.add(Arguments.of(1, -3, nan, 0));
        ///builder.add(Arguments.of(42, -inf, nan, 0));
        // x+xx is infinite
        builder.add(Arguments.of(inf, 0, inf, 0));
        builder.add(Arguments.of(inf, 3, inf, 0));
        ///builder.add(Arguments.of(0, inf, inf, 0));
        ///builder.add(Arguments.of(3, inf, inf, 0));
        ///builder.add(Arguments.of(max, max, inf, 0));
        // x+xx is zero
        final double[] zero = {0.0, -0.0};
        for (final double x : zero) {
            for (final double xx : zero) {
                ///builder.add(Arguments.of(x, xx, x + xx, 0));
                builder.add(Arguments.of(x, xx, x, 0));
            }
        }
        // Numbers are normalized before computation
        ///builder.add(Arguments.of(5, -1, 2, 0));
        ///builder.add(Arguments.of(-1, 5, 2, 0));
        return builder.build();
    }

    @ParameterizedTest
    @CsvFileSource(resources = {"sqrt0.csv"})
    void testSqrt(double x, double xx, BigDecimal expected) {
        final DD dd = DD.fastTwoSum(x, xx);
        TestUtils.assertEquals(expected, dd.sqrt(), 2 * EPS, () -> dd.toString());
    }

    @ParameterizedTest
    @CsvFileSource(resources = {"sqrt512.csv"})
    void testSqrtBig(double x, double xx, BigDecimal expected) {
        final DD dd = DD.fastTwoSum(x, xx);
        TestUtils.assertEquals(expected, dd.sqrt(), 2 * EPS, () -> dd.toString());
    }

    @ParameterizedTest
    @CsvFileSource(resources = {"sqrt-512.csv"})
    void testSqrtSmall(double x, double xx, BigDecimal expected) {
        final DD dd = DD.fastTwoSum(x, xx);
        // This test data has a case that fails at 1.01 * 2^-105
        TestUtils.assertEquals(expected, dd.sqrt(), 2.03 * EPS, () -> dd.toString());
    }

    @ParameterizedTest
    @CsvFileSource(resources = {"sqrt0.csv"})
    void testAccurateSqrt(double x, double xx, BigDecimal expected) {
        final DD dd = DD.fastTwoSum(x, xx);
        TestUtils.assertEquals(expected, DDExt.sqrt(dd), EPS, () -> dd.toString());
    }

    @ParameterizedTest
    @CsvFileSource(resources = {"sqrt512.csv"})
    void testAccurateSqrtBig(double x, double xx, BigDecimal expected) {
        final DD dd = DD.fastTwoSum(x, xx);
        TestUtils.assertEquals(expected, DDExt.sqrt(dd), EPS, () -> dd.toString());
    }

    @ParameterizedTest
    @CsvFileSource(resources = {"sqrt-512.csv"})
    void testAccurateSqrtSmall(double x, double xx, BigDecimal expected) {
        final DD dd = DD.fastTwoSum(x, xx);
        TestUtils.assertEquals(expected, DDExt.sqrt(dd), EPS, () -> dd.toString());
    }

    @ParameterizedTest
    @MethodSource
    void testScalb(double x, double xx) {
        final DD dd = DD.of(x, xx);
        DD s;
        final Supplier<String> msg = () -> String.format("(%s,%s)", x, xx);
        // Scales around powers of 2 up to the limit of 2^12 = 4096
        for (int p = 0; p <= 12; p++) {
            final int b = 1 << p;
            for (int i = -1; i <= 1; i++) {
                final int n = b + i;
                s = dd.scalb(n);
                Assertions.assertEquals(Math.scalb(x, n), s.hi(), () -> msg.get() + " hi: scale=" + n);
                Assertions.assertEquals(Math.scalb(xx, n), s.lo(), () -> msg.get() + " lo: scale=" + n);
                s = dd.scalb(-n);
                Assertions.assertEquals(Math.scalb(x, -n), s.hi(), () -> msg.get() + " hi: scale=" + -n);
                Assertions.assertEquals(Math.scalb(xx, -n), s.lo(), () -> msg.get() + " lo: scale=" + -n);
            }
        }
        // Extreme scaling
        for (final int n : new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1, Integer.MAX_VALUE}) {
            s = dd.scalb(n);
            Assertions.assertEquals(Math.scalb(x, n), s.hi(), () -> msg.get() + " hi: scale=" + n);
            Assertions.assertEquals(Math.scalb(xx, n), s.lo(), () -> msg.get() + " lo: scale=" + n);
        }
    }

    static Stream<Arguments> testScalb() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = createRNG();
        DD s;
        for (int i = 0; i < 10; i++) {
            s = signedNormalDoubleDouble(rng);
            builder.add(Arguments.of(s.hi(), s.lo()));
        }
        final double[] v = {1, 0, Double.MAX_VALUE, Double.MIN_NORMAL, Double.MIN_VALUE, Math.PI,
            Math.E, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN};
        for (final double x : v) {
            for (final double xx : v) {
                // Here we do not care if the value is normalized: |x| > |xx|
                // The test is to check scaling is performed on both numbers independently.
                builder.add(Arguments.of(x, xx));
            }
        }
        return builder.build();
    }


    @ParameterizedTest
    @CsvSource({
        // Non-scalable numbers:
        // exponent is always zero, (x,xx) is unchanged
        "0.0, 0.0, 0, 0.0, 0.0",
        "0.0, -0.0, 0, 0.0, -0.0",
        "NaN, 0.0, 0, NaN, 0.0",
        "NaN, NaN, 0, NaN, NaN",
        "Infinity, 0.0, 0, Infinity, 0.0",
        "Infinity, NaN, 0, Infinity, NaN",
        // Normalisation of (1, 0)
        "1.0, 0, 1, 0.5, 0",
        "-1.0, 0, 1, -0.5, 0",
        // Power of 2 with round-off to reduce the magnitude
        "0.5, -5.551115123125783E-17, -1, 1.0, -1.1102230246251565E-16",
        "1.0, -1.1102230246251565E-16, 0, 1.0, -1.1102230246251565E-16",
        "2.0, -2.220446049250313E-16, 1, 1.0, -1.1102230246251565E-16",
        "0.5, 5.551115123125783E-17, 0, 0.5, 5.551115123125783E-17",
        "1.0, 1.1102230246251565E-16, 1, 0.5, 5.551115123125783E-17",
        "2.0, 2.220446049250313E-16, 2, 0.5, 5.551115123125783E-17",
    })
    void testFrexpEdgeCases(double x, double xx, int exp, double fx, double fxx) {
        // Initialize to something so we know it changes
        final int[] e = {62783468};
        DD f = DD.of(x, xx).frexp(e);
        Assertions.assertEquals(exp, e[0], "exp");
        Assertions.assertEquals(fx, f.hi(), "hi");
        Assertions.assertEquals(fxx, f.lo(), "lo");
        // Reset
        e[0] = 126943276;
        f = DD.of(-x, -xx).frexp(e);
        Assertions.assertEquals(exp, e[0], "exp");
        Assertions.assertEquals(-fx, f.hi(), "hi");
        Assertions.assertEquals(-fxx, f.lo(), "lo");
    }

    @ParameterizedTest
    @MethodSource
    void testFrexp(double x, double xx) {
        Assertions.assertTrue(Double.isFinite(x) && x != 0, "Invalid x: " + x);
        assertNormalized(x, xx, "x");

        final int[] e = {627846824};
        final Supplier<String> msg = () -> String.format("(%s,%s)", x, xx);
        DD f = DD.of(x, xx).frexp(e);

        final double hi = f.hi();
        final double lo = f.lo();
        final double ahi = Math.abs(hi);
        Assertions.assertTrue(0.5 <= ahi && ahi <= 1, () -> msg.get() + " hi");

        // Get the exponent handling sub-normal numbers
        int exp = Math.abs(x) < 0x1.0p-900 ?
            Math.getExponent(x * 0x1.0p200) - 200 :
            Math.getExponent(x);

        // Math.getExponent returns the value for a fractional part in [1, 2) not [0.5, 1)
        exp += 1;

        // Edge case where the exponent is smaller
        if (Math.abs(ahi) == 1) {
            if (hi == 1) {
                Assertions.assertTrue(lo < 0, () -> msg.get() + " (f,ff) is not < 1");
            } else {
                Assertions.assertTrue(lo > 0, () -> msg.get() + " (f,ff) is not > -1");
            }
            exp -= 1;
        }

        Assertions.assertEquals(exp, e[0], () -> msg.get() + " exponent");

        // Check the bits are the same.
        Assertions.assertEquals(x, Math.scalb(hi, exp), () -> msg.get() + " scaled f hi");
        Assertions.assertEquals(xx, Math.scalb(lo, exp), () -> msg.get() + " scaled f lo");

        // Check round-trip
        f = f.scalb(exp);
        Assertions.assertEquals(x, f.hi(), () -> msg.get() + " scalb f hi");
        Assertions.assertEquals(xx, f.lo(), () -> msg.get() + " scalb f lo");
    }

    static Stream<Arguments> testFrexp() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = createRNG();
        DD s;
        for (int i = 0; i < 10; i++) {
            s = signedNormalDoubleDouble(rng);
            for (final double scale : SCALES) {
                builder.add(Arguments.of(s.hi() * scale, s.lo() * scale));
            }
        }
        // Sub-normal numbers
        final double[] scales = IntStream.of(-1000, -1022, -1023, -1024, -1050, -1074)
            .mapToDouble(n -> Math.scalb(1.0, n)).toArray();
        for (int i = 0; i < 5; i++) {
            s = signedNormalDoubleDouble(rng);
            for (final double scale : scales) {
                builder.add(Arguments.of(s.hi() * scale, s.lo() * scale));
            }
        }
        // x is power of 2
        for (int i = 0; i < 3; i++) {
            for (final double scale : SCALES) {
                builder.add(Arguments.of(scale, signedNormalDouble(rng) * scale * 0x1.0p-55));
                builder.add(Arguments.of(scale, 0));
            }
        }
        // Extreme case should change x to 1.0 when xx is opposite sign
        builder.add(Arguments.of(0.5, Double.MIN_VALUE));
        builder.add(Arguments.of(0.5, -Double.MIN_VALUE));
        builder.add(Arguments.of(-0.5, Double.MIN_VALUE));
        builder.add(Arguments.of(-0.5, -Double.MIN_VALUE));
        return builder.build();
    }

    @ParameterizedTest
    @CsvSource({
        // Math.pow(x, 0) == 1, even for non-finite values
        "0.0, 0.0, 0, 1.0, 0.0",
        "1.23, 0.0, 0, 1.0, 0.0",
        "1.0, 0.0, 0, 1.0, 0.0",
        "-2.0, 0.0, 0, 1.0, 0.0",
        "Infinity, 0.0, 0, 1.0, 0.0",
        "NaN, 0.0, 0, 1.0, 0.0",
        // Math.pow(0.0, n) == +/- 0.0
        "0.0, 0.0, 1, 0.0, 0.0",
        "0.0, 0.0, 2, 0.0, 0.0",
        "-0.0, 0.0, 1, -0.0, 0.0",
        "-0.0, 0.0, 2, 0.0, 0.0",
        // Math.pow(1, n) == 1
        "1.0, 0.0, 1, 1.0, 0.0",
        "1.0, 0.0, 2, 1.0, 0.0",
        // Math.pow(-1, n) == +/-1 - requires round-off sign propagation
        "-1.0, 0.0, 1, -1.0, 0.0",
        "-1.0, 0.0, 2, 1.0, -0.0",
        "-1.0, -0.0, 1, -1.0, -0.0",
        "-1.0, -0.0, 2, 1.0, 0.0",
        // Math.pow(0.0, -n)
        "0.0, 0.0, -1, Infinity, 0.0",
        "0.0, 0.0, -2, Infinity, 0.0",
        "-0.0, 0.0, -1, -Infinity, 0.0",
        "-0.0, 0.0, -2, Infinity, 0.0",
        // NaN / Infinite is IEEE pow result for x
        "Infinity, 0.0, 1, Infinity, 0.0, 0",
        "-Infinity, 0.0, 1, -Infinity, 0.0, 0",
        "-Infinity, 0.0, 2, Infinity, 0.0, 0",
        "Infinity, 0.0, -1, 0.0, 0.0, 0",
        "-Infinity, 0.0, -1, -0.0, 0.0, 0",
        "-Infinity, 0.0, -2, 0.0, 0.0, 0",
        "NaN, 0.0, 1, NaN, 0.0, 0",
        // Inversion creates infinity (sub-normal x^-n < 2.22e-308)
        // Signed zeros should match inversion when the result is large and finite.
        "1e-312, 0.0, -1, Infinity, -0.0",
        "1e-312, -0.0, -1, Infinity, -0.0",
        "-1e-312, 0.0, -1, -Infinity, 0.0",
        "-1e-312, -0.0, -1, -Infinity, 0.0",
        "1e-156, 0.0, -2, Infinity, -0.0",
        "1e-156, -0.0, -2, Infinity, -0.0",
        "-1e-156, 0.0, -2, Infinity, -0.0",
        "-1e-156, -0.0, -2, Infinity, -0.0",
        "1e-106, 0.0, -3, Infinity, -0.0",
        "1e-106, -0.0, -3, Infinity, -0.0",
        "-1e-106, 0.0, -3, -Infinity, 0.0",
        "-1e-106, -0.0, -3, -Infinity, 0.0",
    })
    void testSimplePowEdgeCases(double x, double xx, int n, double z, double zz) {
        final DD f = DDExt.simplePow(x, xx, n);
        Assertions.assertEquals(z, f.hi(), "hi");
        Assertions.assertEquals(zz, f.lo(), "lo");
    }

    @ParameterizedTest
    @MethodSource
    void testSimplePow(double x, double xx, int n, double eps) {
        assertNormalized(x, xx, "x");
        final Supplier<String> msg = () -> String.format("(%s,%s)^%d", x, xx, n);

        final DD s = DDExt.simplePow(x, xx, n);
        // Check normalized
        final double hi = s.hi();
        Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue");

        final BigDecimal e = bd(x).add(bd(xx)).pow(n, MathContext.DECIMAL128);
        TestUtils.assertEquals(e, s, eps, () -> msg.get());
    }

    static Stream<Arguments> testSimplePow() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = createRNG();
        DD s;

        // Note the negative power is essentially just the same result as x^n combined with
        // the error of the inverse operation. This is far more accurate than simplePow
        // and we can use the same relative error for both.

        // Small powers are around the precision of a double 2^-53
        // No overflow when n < 10
        for (int i = 0; i < SAMPLES; i++) {
            s = signedNormalDoubleDouble(rng);
            final int n = rng.nextInt(2, 10);
            // Some random failures at 2^-53
            builder.add(Arguments.of(s.hi(), s.lo(), n, 1.5 * 0x1.0p-53));
            builder.add(Arguments.of(s.hi(), s.lo(), -n, 1.5 * 0x1.0p-53));
        }

        // Trigger use of the Taylor series (1+z)^n >> 1
        // z = xx/x so is <= 2^-53 for a normalized double-double.
        // n * log1p(z) > log(1 + y)
        // n ~ log1p(y) / log1p(eps)
        // y       n
        // 2^-45   256
        // 2^-44   512
        // 2^-43   1024
        // 2^-40   8192
        // 2^-35   262144
        // 2^-b    2^(53-b)

        // Medium powers where the value of a normalized double will not overflow.
        // Here Math.pow(x, n) alone can be 3 orders of magnitude worse (~10 bits less precision).
        for (int i = 0; i < SAMPLES; i++) {
            s = signedNormalDoubleDouble(rng);
            final int n = rng.nextInt(512, 1024);
            // Some random failures at 2^-53
            builder.add(Arguments.of(s.hi(), s.lo(), n, 0x1.0p-52));
            builder.add(Arguments.of(s.hi(), s.lo(), -n, 0x1.0p-52));
        }

        // Large powers to trigger the case for n > 1e8 (100000000), or n * z > 1e-8.
        // Here Math.pow(x, n) alone can be 9 orders of magnitude worse (~30 bits less precision).
        // Only applicable when the value will not over/underflow.
        // Note that we wish to use n > 1e8 to trigger the condition more frequently.
        // The limit for BigDecimal.pow is 1e9 - 1 so use half of that.
        // Here we use a value in [0.5, 1) and avoid underflow for the double-double
        // which occurs when the high part for the result is close to 2^-958.
        // x^n = 2^-958
        // x ~ exp(log(2^-958) / n) ~ 0.99999867...
        final int n = (int) 5e8;
        final double x = Math.exp(Math.log(0x1.0p-958) / n);
        for (int i = 0; i < SAMPLES; i++) {
            final double hi = rng.nextDouble(x, 1);
            // hi will have an exponent of -1 as it is in [0.5, 1).
            // Scale some random bits to add on to it.
            final double lo = signedNormalDouble(rng) * 0x1.0p-53;
            s = DD.fastTwoSum(hi, lo);
            // Some random failures at 2^-53
            builder.add(Arguments.of(s.hi(), s.lo(), n, 0x1.0p-52));
            builder.add(Arguments.of(s.hi(), s.lo(), -n, 0x1.0p-52));
        }

        // Edge cases
        // Fails at 2^-53
        builder.add(Arguments.of(1.093868041452691, 8.212626726872479E-17, 4, 1.5 * 0x1.0p-53));
        // This creates a result so large it must be safely inverted for the negative power
        builder.add(Arguments.of(1.987660428759235, 3.7909885615002006e-17, -1006, 0x1.0p-52));

        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"testPowScaledEdgeCases"})
    void testSimplePowScaledEdgeCases(double x, double xx, int n, double z, double zz, long exp) {
        final long[] e = {126384};
        final DD f = DDExt.simplePowScaled(x, xx, n, e);
        Assertions.assertEquals(z, f.hi(), "hi");
        Assertions.assertEquals(zz, f.lo(), "lo");
        Assertions.assertEquals(exp, e[0], "exp");
    }

    /**
     * Test cases of {@link DDExt#simplePowScaled(double, double, int, long[])} where no scaling is
     * required. It should be the same as {@link DDExt#simplePow(double, double, int)}.
     */
    @ParameterizedTest
    @CsvSource({
        "1.23, 0.0, 3",
        "1.23, 0.0, -3",
        "1.23, 1e-16, 2",
        "1.23, 1e-16, -2",
        // No underflow - Do not get close to underflowing the low part
        "0.5, 1e-17, 900",
        // x > sqrt(0.5)
        "0.75, 1e-17, 2000",  // 1.33e-250
        "0.9, 1e-17, 5000",   // 1.63e-229
        "0.99, 1e-17, 50000", // 5.75e-219
        "0.75, 1e-17, 100",   // (safe n)
        "0.9999999999999999, 1e-17, 2147483647", // (safe x)
        // No overflow
        "2.0, 1e-16, 1000",
        // 2x < sqrt(0.5)
        "1.5, 1e-16, 1500",   // 1.37e264
        "1.1, 1e-16, 6000",   // 2.27e248
        "1.01, 1e-16, 60000", // 1.92e259
        "2.0, 1e-16, 100",   // (safe n)
        "1.0000000000000002, 1e-17, 2147483647", // (safe x)
    })
    void testSimplePowScaledSafe(double x, double xx, int n) {
        final long[] exp = {61273468};
        final DD f = DDExt.simplePowScaled(x, xx, n, exp);
        // Same
        DD z = DDExt.simplePow(x, xx, n);
        final int[] ez = {168168681};
        z = z.frexp(ez);
        Assertions.assertEquals(z.hi(), f.hi(), "hi");
        Assertions.assertEquals(z.lo(), f.lo(), "lo");
        Assertions.assertEquals(ez[0], exp[0], "exp");
    }

    @ParameterizedTest
    @MethodSource(value = {"testPowScaled"})
    void testSimplePowScaled(double x, double xx, int n, double eps, double ignored) {
        assertNormalized(x, xx, "x");
        final Supplier<String> msg = () -> String.format("(%s,%s)^%d", x, xx, n);

        final long[] exp = {61284638};
        final DD f = DDExt.simplePowScaled(x, xx, n, exp);

        // Check normalized
        final double hi = f.hi();
        Assertions.assertEquals(hi, f.doubleValue(), () -> msg.get() + " doubleValue");

        final BigDecimal e = bd(x).add(bd(xx)).pow(n, MathContext.DECIMAL128);
        TestUtils.assertScaledEquals(e, f, exp[0], eps, () -> msg.get());
    }

    @ParameterizedTest
    @MethodSource(value = {"testPowScaledLargeN"})
    void testSimplePowScaledLargeN(double x, double xx, int n, long e, BigDecimal expected, double eps, double ignored) {
        final long[] exp = {-76967868};
        final DD f = DDExt.simplePowScaled(x, xx, n, exp);
        final Supplier<String> msg = () -> String.format("(%s,%s)^%d", x, xx, n);

        // Check normalized
        final double hi = f.hi();
        Assertions.assertEquals(hi, f.doubleValue(), () -> msg.get() + " doubleValue");

        Assertions.assertEquals(e, exp[0], () -> msg.get() + " exponent");
        TestUtils.assertEquals(expected, f, eps, () -> msg.get());
    }

    @ParameterizedTest
    @MethodSource
    void testPowNotNormalizedFinite(double x, int n) {
        final Supplier<String> msg = () -> String.format("(%s,0)^%d", x, n);
        final DD s = DD.of(x).pow(n);
        Assertions.assertEquals(Math.pow(x, n), s.hi(), () -> msg.get() + " hi");
        Assertions.assertEquals(0, s.lo(), () -> msg.get() + " lo");
    }

    static Stream<Arguments> testPowNotNormalizedFinite() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        for (final int n : new int[] {-2, -1, 0, 1, 2}) {
            for (final double x : new double[] {Double.NaN, Double.POSITIVE_INFINITY,
                    Math.nextDown(Double.MIN_NORMAL), Double.MIN_VALUE, 0}) {
                builder.add(Arguments.of(x, n));
                builder.add(Arguments.of(-x, n));
            }
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testPowEdgeCases(double x, double xx) {
        final Supplier<String> msg = () -> String.format("(%s,%s)", x, xx);
        final DD dd = DD.of(x, xx);

        Assertions.assertSame(dd, dd.pow(1), () -> msg.get() + "^1");

        DD s = dd.pow(0);
        Assertions.assertEquals(1, s.hi(), () -> msg.get() + "^0 hi");
        Assertions.assertEquals(0, s.lo(), () -> msg.get() + "^0 lo");

        s = dd.pow(-1);
        final DD t = dd.reciprocal();
        Assertions.assertEquals(t.hi(), s.hi(), () -> msg.get() + "^-1 hi");
        Assertions.assertEquals(t.lo(), s.lo(), () -> msg.get() + "^-1 lo");
    }

    static Stream<Arguments> testPowEdgeCases() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = createRNG();
        DD s;

        for (int i = 0; i < 10; i++) {
            s = signedNormalDoubleDouble(rng);
            builder.add(Arguments.of(s.hi() * 0.5, s.lo() * 0.5));
            builder.add(Arguments.of(s.hi(), s.lo()));
        }

        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testPow(double x, double xx, int n, double eps) {
        assertNormalized(x, xx, "x");
        final Supplier<String> msg = () -> String.format("(%s,%s)^%d", x, xx, n);

        final DD s = DD.of(x, xx).pow(n);
        // Check normalized
        final double hi = s.hi();
        Assertions.assertEquals(hi, s.doubleValue(), () -> msg.get() + " doubleValue");

        final BigDecimal e = bd(x).add(bd(xx)).pow(n, MathContext.DECIMAL128);
        TestUtils.assertEquals(e, s, eps, () -> msg.get());
    }

    static Stream<Arguments> testPow() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = createRNG();
        DD s;

        // Note the negative power is essentially just the same result as x^n combined with
        // the error of the inverse operation. This is more accurate than pow with large
        // exponents and we can use the same relative error for both.

        // van Mulbregt (2018) pp 22: Error of a compensated pow is ~ 16(n-1) eps^2.
        // The limit is:
        // Math.log(16.0 * (Integer.MAX_VALUE-1) * 0x1.0p-106) / Math.log(2) = -71.0

        // Small powers
        for (int i = 0; i < SAMPLES; i++) {
            s = signedNormalDoubleDouble(rng);
            final int n = rng.nextInt(2, 5);
            builder.add(Arguments.of(s.hi(), s.lo(), n, 0x1.0p-100));
            builder.add(Arguments.of(s.hi(), s.lo(), -n, 0x1.0p-100));
        }

        // Medium powers where the value of a normalized double will not overflow.
        // Limit the upper exponent to 958 so inversion is safe and avoids sub-normals.
        // Here Math.pow(x, n) alone can be 3 orders of magnitude worse (~10 bits less precision).
        for (int i = 0; i < SAMPLES; i++) {
            s = signedNormalDoubleDouble(rng);
            final int n = rng.nextInt(830, 958);
            builder.add(Arguments.of(s.hi(), s.lo(), n, 0x1.0p-92));
            builder.add(Arguments.of(s.hi(), s.lo(), -n, 0x1.0p-92));
        }

        // Large powers only applicable when the value will not over/underflow.
        // The limit for BigDecimal.pow is 1e9 - 1 so use half of that.
        // Here we use a value in [0.5, 1) and avoid underflow for the double-double
        // which occurs when the high part for the result is close to 2^-958.
        // x^n = 2^-958
        // x ~ exp(log(2^-958) / n) ~ 0.99999867...
        // Here Math.pow(x, n) alone can be 9 orders of magnitude worse (~30 bits less precision).
        final int n = (int) 5e8;
        final double x = Math.exp(Math.log(0x1.0p-958) / n);
        for (int i = 0; i < SAMPLES; i++) {
            final double hi = rng.nextDouble(x, 1);
            // hi will have an exponent of -1 as it is in [0.5, 1).
            // Scale some random bits to add on to it.
            final double lo = signedNormalDouble(rng) * 0x1.0p-53;
            s = DD.fastTwoSum(hi, lo);
            builder.add(Arguments.of(s.hi(), s.lo(), n, 0x1.0p-73));
            builder.add(Arguments.of(s.hi(), s.lo(), -n, 0x1.0p-73));
        }

        // Edge cases
        // This creates a result so large it must be safely inverted for the negative power.
        // Currently overflow protection is not supported so this is disabled.
        //builder.add(Arguments.of(1.987660428759235, 3.7909885615002006e-17, -1006, 0x1.0p-52));

        return builder.build();
    }

    /**
     * Test computing the square of a double (no low part).
     * This effectively tests squareLowUnscaled via the public power function.
     */
    @ParameterizedTest
    @MethodSource(value = {"testPowScaledSquare"})
    void testPowScaledSquare(double x) {
        final Supplier<String> msg = () -> String.format("%s^2", x);

        // Two product is exact
        final DD z = DD.twoProd(x, x);

        final long[] e = {67816283};
        final DD x2 = DD.of(x).pow(2, e);
        final double hi = Math.scalb(x2.hi(), (int) e[0]);
        final double lo = Math.scalb(x2.lo(), (int) e[0]);

        // Should be exact
        Assertions.assertEquals(z.hi(), hi, () -> msg.get() + " hi");
        Assertions.assertEquals(z.lo(), lo, () -> msg.get() + " lo");
    }

    @ParameterizedTest
    @MethodSource(value = {"testPowScaledEdgeCases"})
    void testPowScaledEdgeCases(double x, double xx, int n, double z, double zz, long exp) {
        final long[] e = {457578688};
        final DD f = DD.of(x, xx).pow(n, e);
        Assertions.assertEquals(z, f.hi(), "hi");
        Assertions.assertEquals(zz, f.lo(), "lo");
        Assertions.assertEquals(exp, e[0], "exp");
    }

    @ParameterizedTest
    @MethodSource(value = {"testPowScaledSmall", "testPowScaled"})
    void testPowScaled(double x, double xx, int n, double ignored, double eps) {
        assertNormalized(x, xx, "x");
        final Supplier<String> msg = () -> String.format("(%s,%s)^%d", x, xx, n);

        final long[] exp = {67868};
        final DD f = DD.of(x, xx).pow(n, exp);

        // Check normalized
        final double hi = f.hi();
        Assertions.assertEquals(hi, f.doubleValue(), () -> msg.get() + " doubleValue");

        final BigDecimal e = bd(x).add(bd(xx)).pow(n, MathContext.DECIMAL128);
        TestUtils.assertScaledEquals(e, f, exp[0], eps, () -> msg.get());
    }

    @ParameterizedTest
    @MethodSource(value = {"testPowScaledLargeN"})
    void testPowScaledLargeN(double x, double xx, int n, long e, BigDecimal expected, double ignored, double eps) {
        final long[] exp = {6283684};
        final DD f = DD.of(x, xx).pow(n, exp);
        final Supplier<String> msg = () -> String.format("(%s,%s)^%d", x, xx, n);

        // Check normalized
        final double hi = f.hi();
        Assertions.assertEquals(hi, f.doubleValue(), () -> msg.get() + " doubleValue");

        Assertions.assertEquals(e, exp[0], () -> msg.get() + " exponent");
        TestUtils.assertEquals(expected, f, eps, () -> msg.get());
    }

    @ParameterizedTest
    @MethodSource(value = {"testPowScaledEdgeCases"})
    void testAccuratePowScaledEdgeCases(double x, double xx, int n, double z, double zz, long exp) {
        final long[] e = {-9089675};
        final DD f = DDMath.pow(DD.of(x, xx), n, e);
        Assertions.assertEquals(z, f.hi(), "hi");
        Assertions.assertEquals(zz, f.lo(), "lo");
        Assertions.assertEquals(exp, e[0], "exp");
    }

    @ParameterizedTest
    @MethodSource(value = {"testPowScaledSmall", "testPowScaled"})
    void testAccuratePowScaled(double x, double xx, int n, double ignored, double ignored2) {
        assertNormalized(x, xx, "x");
        final Supplier<String> msg = () -> String.format("(%s,%s)^%d", x, xx, n);

        final long[] exp = {-769854635434L};
        final DD f = DDMath.pow(DD.of(x, xx), n, exp);

        // Check normalized
        final double hi = f.hi();
        Assertions.assertEquals(hi, f.doubleValue(), () -> msg.get() + " doubleValue");

        final BigDecimal e = bd(x).add(bd(xx)).pow(n, MathContext.DECIMAL128);
        // Javadoc for the method states accuracy is 1 ULP to be conservative.
        // If correctly performed in triple-double precision it should be exact except
        // for cases of final rounding error when converted to double-double.
        // Test typically passes at: 0.5 * eps with eps = 2^-106.
        // Higher powers may have lower accuracy but are not tested.
        // Update tolerance to 1.0625 * eps as 1 case of rounding error has been observed.
        TestUtils.assertScaledEquals(e, f, exp[0], 1.0625 * EPS, () -> msg.get());
    }

    @ParameterizedTest
    @MethodSource(value = {"testPowScaledLargeN"})
    void testAccuratePowScaledLargeN(double x, double xx, int n, long e, BigDecimal expected, double ignored, double ignored2) {
        final long[] exp = {-657545435};
        final DD f = DDMath.pow(DD.of(x, xx), n, exp);
        final Supplier<String> msg = () -> String.format("(%s,%s)^%d", x, xx, n);

        // Check normalized
        final double hi = f.hi();
        Assertions.assertEquals(hi, f.doubleValue(), () -> msg.get() + " doubleValue");

        Assertions.assertEquals(e, exp[0], () -> msg.get() + " exponent");
        // Accuracy is that of a double-double number: 0.5 * eps with eps = 2^-106
        TestUtils.assertEquals(expected, f, 0.5 * EPS, () -> msg.get());
    }

    static Stream<Arguments> testPowScaledEdgeCases() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final double inf = Double.POSITIVE_INFINITY;
        final double nan = Double.NaN;
        // Math.pow(x, 0) == 1, even for non-finite values (fractional representation)
        builder.add(Arguments.of(0.0, 0.0, 0, 0.5, 0.0, 1));
        builder.add(Arguments.of(1.23, 0.0, 0, 0.5, 0.0, 1));
        builder.add(Arguments.of(1.0, 0.0, 0, 0.5, 0.0, 1));
        builder.add(Arguments.of(inf, 0.0, 0, 0.5, 0.0, 1));
        builder.add(Arguments.of(nan, 0.0, 0, 0.5, 0.0, 1));
        // Math.pow(0.0, n) == +/- 0.0 (no fractional representation)
        builder.add(Arguments.of(0.0, 0.0, 1, 0.0, 0.0, 0));
        builder.add(Arguments.of(0.0, 0.0, 2, 0.0, 0.0, 0));
        builder.add(Arguments.of(-0.0, 0.0, 1, -0.0, 0.0, 0));
        builder.add(Arguments.of(-0.0, 0.0, 2, 0.0, 0.0, 0));
        // Math.pow(1, n) == 1 (fractional representation)
        builder.add(Arguments.of(1.0, 0.0, 1, 0.5, 0.0, 1));
        builder.add(Arguments.of(1.0, 0.0, 2, 0.5, 0.0, 1));
        // Math.pow(-1, n) == +/-1 (fractional representation) - requires round-off sign propagation
        builder.add(Arguments.of(-1.0, 0.0, 1, -0.5, 0.0, 1));
        builder.add(Arguments.of(-1.0, 0.0, 2, 0.5, -0.0, 1));
        builder.add(Arguments.of(-1.0, -0.0, 1, -0.5, -0.0, 1));
        builder.add(Arguments.of(-1.0, -0.0, 2, 0.5, 0.0, 1));
        // Math.pow(0.0, -n) - No fractional representation
        builder.add(Arguments.of(0.0, 0.0, -1, inf, 0.0, 0));
        builder.add(Arguments.of(0.0, 0.0, -2, inf, 0.0, 0));
        builder.add(Arguments.of(-0.0, 0.0, -1, -inf, 0.0, 0));
        builder.add(Arguments.of(-0.0, 0.0, -2, inf, 0.0, 0));
        // NaN / Infinite is IEEE pow result for x
        builder.add(Arguments.of(inf, 0.0, 1, inf, 0.0, 0));
        builder.add(Arguments.of(-inf, 0.0, 1, -inf, 0.0, 0));
        builder.add(Arguments.of(-inf, 0.0, 2, inf, 0.0, 0));
        builder.add(Arguments.of(inf, 0.0, -1, 0.0, 0.0, 0));
        builder.add(Arguments.of(-inf, 0.0, -1, -0.0, 0.0, 0));
        builder.add(Arguments.of(-inf, 0.0, -2, 0.0, 0.0, 0));
        builder.add(Arguments.of(nan, 0.0, 1, nan, 0.0, 0));
        // Hit edge case of zero low part
        builder.add(Arguments.of(0.5, 0.0, -1, 0.5, 0.0, 2));
        builder.add(Arguments.of(1.0, 0.0, -1, 0.5, 0.0, 1));
        builder.add(Arguments.of(2.0, 0.0, -1, 0.5, 0.0, 0));
        builder.add(Arguments.of(4.0, 0.0, -1, 0.5, 0.0, -1));
        builder.add(Arguments.of(0.5, 0.0, 2, 0.5, 0.0, -1));
        builder.add(Arguments.of(1.0, 0.0, 2, 0.5, 0.0, 1));
        builder.add(Arguments.of(2.0, 0.0, 2, 0.5, 0.0, 3));
        builder.add(Arguments.of(4.0, 0.0, 2, 0.5, 0.0, 5));
        // Exact power of two (representable)
        // Math.pow(0.5, 123) == 0.5 * Math.scalb(1.0, -122)
        // Math.pow(2.0, 123) == 0.5 * Math.scalb(1.0, 124)
        builder.add(Arguments.of(0.5, 0.0, 123, 0.5, 0.0, -122));
        builder.add(Arguments.of(1.0, 0.0, 123, 0.5, 0.0, 1));
        builder.add(Arguments.of(2.0, 0.0, 123, 0.5, 0.0, 124));
        builder.add(Arguments.of(0.5, 0.0, -123, 0.5, 0.0, 124));
        builder.add(Arguments.of(1.0, 0.0, -123, 0.5, 0.0, 1));
        builder.add(Arguments.of(2.0, 0.0, -123, 0.5, 0.0, -122));
        // Exact power of two (not representable)
        builder.add(Arguments.of(0.5, 0.0, 12345, 0.5, 0.0, -12344));
        builder.add(Arguments.of(1.0, 0.0, 12345, 0.5, 0.0, 1));
        builder.add(Arguments.of(2.0, 0.0, 12345, 0.5, 0.0, 12346));
        builder.add(Arguments.of(0.5, 0.0, -12345, 0.5, 0.0, 12346));
        builder.add(Arguments.of(1.0, 0.0, -12345, 0.5, 0.0, 1));
        builder.add(Arguments.of(2.0, 0.0, -12345, 0.5, 0.0, -12344));
        return builder.build();
    }

    static Stream<Arguments> testPowScaled() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = createRNG();
        DD s;

        // Note the negative power is essentially just the same result as x^n combined with
        // the error of the inverse operation. This is far more accurate than simplePow
        // and we can use the same relative error for both.

        // This method uses two epsilon values.
        // The first is for simplePowScaled, the second for fastPowScaled.
        // van Mulbregt (2018) pp 22: Error of a compensated pow is ~ 16(n-1) eps^2.
        // The limit is:
        // Math.log(16.0 * (Integer.MAX_VALUE-1) * 0x1.0p-106) / Math.log(2) = -71.0
        // For this test the thresholds are slightly above this limit.
        // Note: powScaled does not require an epsilon as it is ~ eps^2.

        // Powers that approach and do overflow.
        // Here the fractional representation does not overflow.
        // min = 1.5^1700 = 2.26e299
        // max ~ 2^1799
        for (int i = 0; i < SAMPLES; i++) {
            final double x = 1 + rng.nextDouble() / 2;
            final double xx = signedNormalDouble(rng) * 0x1.0p-52;
            s = DD.fastTwoSum(x, xx);
            final int n = rng.nextInt(1700, 1800);
            // Math.log(16.0 * (1799) * 0x1.0p-106) / Math.log(2) = -91.2
            builder.add(Arguments.of(s.hi(), s.lo(), n, 0x1.0p-52, 0x1.0p-94));
            builder.add(Arguments.of(s.hi(), s.lo(), -n, 0x1.0p-52, 0x1.0p-94));
        }

        // Powers that approach and do underflow
        // Here the fractional representation does not overflow.
        // max = 0.75^2400 = 1.41e-300
        // min ~ 2^-2501
        for (int i = 0; i < SAMPLES; i++) {
            final double x = 0.5 + rng.nextDouble() / 4;
            final double xx = signedNormalDouble(rng) * 0x1.0p-53;
            s = DD.fastTwoSum(x, xx);
            final int n = rng.nextInt(2400, 2500);
            // Math.log(16.0 * (2499) * 0x1.0p-106) / Math.log(2) = -90.7
            builder.add(Arguments.of(s.hi(), s.lo(), n, 0x1.0p-51, 0x1.0p-93));
            builder.add(Arguments.of(s.hi(), s.lo(), -n, 0x1.0p-51, 0x1.0p-93));
        }

        // Powers where the fractional representation overflow/underflow
        // x close to sqrt(2) in range [1.4, 1.42). Smallest power:
        // 1.4^5000 = 4.37e730
        // 0.71^5000 = 1.96e-744
        for (int i = 0; i < SAMPLES; i++) {
            final double x = 1.4 + rng.nextDouble() / 50;
            final double xx = signedNormalDouble(rng) * 0x1.0p-52;
            s = DD.fastTwoSum(x, xx);
            final int n = rng.nextInt(5000, 6000);
            // Math.log(16.0 * (5999) * 0x1.0p-106) / Math.log(2) = -89.4
            builder.add(Arguments.of(s.hi(), s.lo(), n, 0x1.0p-50, 0x1.0p-90));
            builder.add(Arguments.of(s.hi(), s.lo(), -n, 0x1.0p-50, 0x1.0p-90));
        }

        // Large powers
        // These lose approximately 10-bits of precision in the double result
        for (int i = 0; i < 20; i++) {
            final double x = 1.4 + rng.nextDouble() / 50;
            final double xx = signedNormalDouble(rng) * 0x1.0p-52;
            s = DD.fastTwoSum(x, xx);
            final int n = rng.nextInt(500000, 600000);
            // Math.log(16.0 * (599999) * 0x1.0p-106) / Math.log(2) = -82.8
            builder.add(Arguments.of(s.hi(), s.lo(), n, 0x1.0p-43, 0x1.0p-85));
            builder.add(Arguments.of(s.hi(), s.lo(), -n, 0x1.0p-43, 0x1.0p-85));
        }

        // Powers approaching the limit of BigDecimal.pow (n=1e9)
        // These lose approximately 15-bits of precision in the double result.
        // Uncomment this to output test cases for testPowScaledLargeN.
        //for (int i = 0; i < 5; i++) {
        //    double x = 1.4 + rng.nextDouble() / 50;
        //    double xx = signedNormalDouble(rng) * 0x1.0p-52;
        //    s = DD.fastTwoSum(x, xx);
        //    int n = rng.nextInt(50000000, 60000000);
        //    builder.add(Arguments.of(s.hi(), s.lo(), n, -0x1.0p-43, -0x1.0p-85));
        //    builder.add(Arguments.of(s.hi(), s.lo(), -n, -0x1.0p-43, -0x1.0p-85));
        //}

        // Spot cases

        // Ensure simplePowScaled coverage with cases where:
        // q = 1
        builder.add(Arguments.of(0.6726201869238487, -1.260696696499313E-17, 2476, 0x1.0p-52, 0x1.0p-94));
        builder.add(Arguments.of(0.7373299007207562, 4.2392599349515834E-17, 2474, 0x1.0p-52, 0x1.0p-94));
        builder.add(Arguments.of(0.7253422761833876, -9.319060725056201E-18, 2477, 0x1.0p-52, 0x1.0p-94));
        // q > 1
        builder.add(Arguments.of(1.4057406814073525, 8.718218123265588E-17, 5172, 0x1.0p-51, 0x1.0p-94));
        builder.add(Arguments.of(1.4123612475347687, -1.8461805152858888E-17, 5318, 0x1.0p-51, 0x1.0p-94));
        // q = 1, r = 0 (i.e. n = m)
        builder.add(Arguments.of(1.4192051440957238, 4.240738704702252E-17, 1935, 0x1.0p-51, 0x1.0p-94));
        builder.add(Arguments.of(1.4146021278694565, 1.7768484484601492E-17, 1917, 0x1.0p-51, 0x1.0p-94));
        // q = 1, r == 1
        builder.add(Arguments.of(1.4192051440957238, 4.240738704702252E-17, 1936, 0x1.0p-51, 0x1.0p-94));
        builder.add(Arguments.of(1.4146021278694565, 1.7768484484601492E-17, 1918, 0x1.0p-51, 0x1.0p-94));
        // q = 2, r = 0
        builder.add(Arguments.of(1.4192051440957238, 4.240738704702252E-17, 3870, 0x1.0p-51, 0x1.0p-94));
        builder.add(Arguments.of(1.4146021278694565, 1.7768484484601492E-17, 3834, 0x1.0p-51, 0x1.0p-94));
        // q = 2, r == 1
        builder.add(Arguments.of(1.4192051440957238, 4.240738704702252E-17, 3871, 0x1.0p-51, 0x1.0p-94));
        builder.add(Arguments.of(1.4146021278694565, 1.7768484484601492E-17, 3835, 0x1.0p-51, 0x1.0p-94));

        // Ensure powScaled coverage with high part a power of 2 and non-zero low part
        builder.add(Arguments.of(0.5, Math.ulp(0.5) / 2, 7, 0x1.0p-52, 0x1.0p-100));
        builder.add(Arguments.of(0.5, -Math.ulp(0.5) / 4, 7, 0x1.0p-52, 0x1.0p-100));
        builder.add(Arguments.of(2, Math.ulp(2.0) / 2, 13, 0x1.0p-52, 0x1.0p-100));
        builder.add(Arguments.of(2, -Math.ulp(2.0) / 4, 13, 0x1.0p-52, 0x1.0p-100));

        // Verify that if at any point the power x^p is down-scaled to ~ 1 then the
        // next squaring will create a value above 1 (hence the very small eps for powScaled).
        // This ensures the value x^e * x^p will always multiply as larger than 1.
        // pow(1.0 + 2^-53, 2) = 1.000000000000000222044604925031320
        // pow(1.0 + 2^-106, 2) = 1.000000000000000000000000000000025
        // pow(Math.nextUp(1.0) - 2^-53 + 2^-54, 2) = 1.000000000000000333066907387546990
        builder.add(Arguments.of(1.0, 0x1.0p-53, 2, 0x1.0p-52, 0x1.0p-106));
        builder.add(Arguments.of(1.0, 0x1.0p-106, 2, 0x1.0p-52, 0x1.0p-106));
        Assertions.assertNotEquals(Math.nextUp(1.0), Math.nextUp(1.0) - 0x1.0p-53, "not normalized double-double");
        builder.add(Arguments.of(Math.nextUp(1.0), -0x1.0p-53 + 0x1.0p-54, 2, 0x1.0p-52, 0x1.0p-106));

        // Misc failure cases
        builder.add(Arguments.of(0.991455078125, 0.0, 64, 0x1.0p-53, 0x1.0p-100));
        builder.add(Arguments.of(0.9530029296875, 0.0, 379, 0x1.0p-53, 0x1.0p-100));
        builder.add(Arguments.of(0.9774169921875, 0.0, 179, 0x1.0p-53, 0x1.0p-100));
        // Fails powScaled at 2^-107 due to a rounding error. Requires eps = 1.0047 * 2^-107.
        // This is a possible error in intermediate triple-double multiplication or
        // rounding of the triple-double to a double-double. A final rounding error could
        // be fixed as the power function norm3 normalises intermediates back to
        // a triple-double from a quad-double result. This discards rounding information
        // that could be used to correctly round the triple-double to a double-double.
        builder.add(Arguments.of(0.5319568842468022, -3.190137112420756E-17, 2473, 0x1.0p-51, 0x1.0p-94));

        // Fails fastPow at 2^-94
        builder.add(Arguments.of(0.5014627401015759, 4.9149107900633496E-17, 2424, 0x1.0p-52, 0x1.0p-93));
        builder.add(Arguments.of(0.5014627401015759, 4.9149107900633496E-17, -2424, 0x1.0p-52, 0x1.0p-93));

        // Observed to fail simplePow at 2^-52 (requires ~ 1.01 * 2^-52)
        // This is platform dependent due to the use of java.lang.Math functions.
        builder.add(Arguments.of(0.7409802960884472, -2.4773863758919158E-17, 2416, 0x1.0p-51, 0x1.0p-93));
        builder.add(Arguments.of(0.7409802960884472, -2.4773863758919158E-17, -2416, 0x1.0p-51, 0x1.0p-93));

        return builder.build();
    }

    static Stream<Arguments> testPowScaledLargeN() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // The scaled BigDecimal power is pre-computed as it takes >10 seconds per case.
        // Results are obtained from the debugging assertion
        // message in TestUtils and thus the BigDecimal is rounded to DECIMAL128 format.
        // simplePowScaled loses ~ 67-bits from a double-double (14-bits from a double).
        // fastPowScaled   loses ~ 26-bits from a double-double.
        // powScaled       loses ~ 1-bit from a double-double.
        builder.add(Arguments.of(1.402774996679172, 4.203934137477261E-17, 58162209, 28399655, "0.5069511623667528687158515355802548", 0x1.0p-39, 0x1.0p-80));
        builder.add(Arguments.of(1.4024304626662112, -1.4084179645855846E-17, 55066019, 26868321, "0.8324073012126417513056910315887745", 0x1.0p-39, 0x1.0p-80));
        builder.add(Arguments.of(1.4125582593027008, -3.545476880711939E-17, 50869441, 25348771, "0.5062665858255789519032946906819150", 0x1.0p-38, 0x1.0p-80));
        builder.add(Arguments.of(1.4119649130236207, -6.64913621578422E-17, 57868054, 28801176, "0.8386830789932243373181320367289536", 0x1.0p-41, 0x1.0p-80));
        builder.add(Arguments.of(1.4138979166089836, 1.9810424188649008E-17, 57796577, 28879676, "0.8521759805456274150644862351758441", 0x1.0p-39, 0x1.0p-80));
        builder.add(Arguments.of(1.4145051107021165, 6.919285583856237E-17, -58047003, -29040764, "0.9609529369187483264098384290609811", 0x1.0p-39, 0x1.0p-80));
        builder.add(Arguments.of(1.4146512942500389, 5.809007274041755E-17, -52177565, -26112078, "0.6333625587966193592039026704846324", 0x1.0p-39, 0x1.0p-80));
        builder.add(Arguments.of(1.4145748596525067, -1.7347735766459908E-17, -58513216, -29278171, "0.6273407549603278011188148414634989", 0x1.0p-39, 0x1.0p-80));
        builder.add(Arguments.of(1.4120799563428865, -5.594285001190042E-17, -52544350, -26157721, "0.5406504832406102336189856859270558", 0x1.0p-38, 0x1.0p-80));
        builder.add(Arguments.of(1.4092258370859025, -8.549761437095368E-17, -51083370, -25281304, "0.7447168954354128135078570760787011", 0x1.0p-39, 0x1.0p-80));
        return builder.build();
    }

    static Stream<Arguments> testPowScaledSquare() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = createRNG();
        for (int i = 0; i < SAMPLES; i++) {
            builder.add(Arguments.of(signedNormalDouble(rng)));
        }
        return builder.build();
    }

    static Stream<Arguments> testPowScaledSmall() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final UniformRandomProvider rng = createRNG();
        final double ignored = Double.NaN;

        // Small powers
        for (int i = 0; i < SAMPLES; i++) {
            final DD s = signedNormalDoubleDouble(rng);
            final int n = rng.nextInt(2, 10);
            builder.add(Arguments.of(s.hi(), s.lo(), n, ignored, 0x1.0p-100));
            builder.add(Arguments.of(s.hi(), s.lo(), -n, ignored, 0x1.0p-100));
        }

        return builder.build();
    }

    // Note: equals and hashcode tests adapted from ComplexTest (since Complex is
    // also an immutable tuple of two doubles)

    @Test
    void testEqualsWithNull() {
        final DD x = DD.of(3.0);
        Assertions.assertNotEquals(x, null);
    }

    @Test
    void testEqualsWithAnotherClass() {
        final DD x = DD.of(3.0);
        Assertions.assertNotEquals(x, new Object());
    }

    @Test
    void testEqualsWithSameObject() {
        final DD x = DD.of(3.0);
        Assertions.assertEquals(x, x);
    }

    @Test
    void testEqualsWithCopyObject() {
        final DD x = DD.of(3.0);
        final DD y = DD.of(3.0);
        Assertions.assertEquals(x, y);
    }

    @Test
    void testEqualsWithHiDifference() {
        final DD x = DD.of(0.0, 0.0);
        final DD y = DD.of(Double.MIN_VALUE, 0.0);
        Assertions.assertNotEquals(x, y);
    }

    @Test
    void testEqualsWithLoDifference() {
        final DD x = DD.of(1.0, 0.0);
        final DD y = DD.of(1.0, Double.MIN_VALUE);
        Assertions.assertNotEquals(x, y);
    }

    /**
     * Test {@link DD#equals(Object)}. It should be consistent with
     * {@link Arrays#equals(double[], double[])} called using the components of two
     * DD numbers.
     */
    @Test
    void testEqualsIsConsistentWithArraysEquals() {
        // Explicit check of the cases documented in the Javadoc:
        assertEqualsIsConsistentWithArraysEquals(DD.of(Double.NaN, 0.0),
            DD.of(Double.NaN, 1.0), "NaN high and different non-NaN low");
        assertEqualsIsConsistentWithArraysEquals(DD.of(0.0, Double.NaN),
            DD.of(1.0, Double.NaN), "Different non-NaN high and NaN low");
        assertEqualsIsConsistentWithArraysEquals(DD.of(0.0, 0.0), DD.of(-0.0, 0.0),
            "Different high zeros");
        assertEqualsIsConsistentWithArraysEquals(DD.of(0.0, 0.0), DD.of(0.0, -0.0),
            "Different low zeros");

        // Test some values of edge cases
        final double[] values = {Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, -1, 0, 1};
        final ArrayList<DD> list = createCombinations(values);

        for (final DD c : list) {
            final double hi = c.hi();
            final double lo = c.lo();

            // Check a copy is equal
            assertEqualsIsConsistentWithArraysEquals(c, DD.of(hi, lo), "Copy DD");

            // Perform the smallest change to the two components
            final double hiDelta = smallestChange(hi);
            final double loDelta = smallestChange(lo);
            Assertions.assertNotEquals(hi, hiDelta, "high was not changed");
            Assertions.assertNotEquals(lo, loDelta, "low was not changed");

            assertEqualsIsConsistentWithArraysEquals(c, DD.of(hiDelta, lo), "Delta high");
            assertEqualsIsConsistentWithArraysEquals(c, DD.of(hi, loDelta), "Delta low");
        }
    }

    /**
     * Specific test to target different representations that contain NaN are {@code false}
     * for {@link DD#equals(Object)}.
     */
    @Test
    void testEqualsWithDifferentNaNs() {
        // Test some NaN combinations
        final ArrayList<DD> list = createCombinations(Double.NaN, 0, 1);

        // Is the all-vs-all comparison only the exact same values should be equal, e.g.
        // (nan,0) not equals (nan,nan)
        // (nan,0) equals (nan,0)
        // (nan,0) not equals (0,nan)
        for (int i = 0; i < list.size(); i++) {
            final DD c1 = list.get(i);
            final DD copy = DD.of(c1.hi(), c1.lo());
            assertEqualsIsConsistentWithArraysEquals(c1, copy, "Copy is not equal");
            for (int j = i + 1; j < list.size(); j++) {
                final DD c2 = list.get(j);
                assertEqualsIsConsistentWithArraysEquals(c1, c2, "Different NaNs should not be equal");
            }
        }
    }

    /**
     * Test the two DD numbers with {@link DD#equals(Object)} and check the
     * result is consistent with {@link Arrays#equals(double[], double[])}.
     *
     * @param c1 the first DD
     * @param c2 the second DD
     * @param msg the message to append to an assertion error
     */
    private static void assertEqualsIsConsistentWithArraysEquals(DD c1, DD c2, String msg) {
        final boolean expected = Arrays.equals(new double[] {c1.hi() + 0.0, c1.lo() + 0.0},
                                               new double[] {c2.hi() + 0.0, c2.lo() + 0.0});
        final boolean actual = c1.equals(c2);
        Assertions.assertEquals(expected, actual,
            () -> String.format("equals(Object) is not consistent with Arrays.equals: %s. %s vs %s", msg, c1, c2));
    }

    /**
     * Test {@link DD#hashCode()}. It should be consistent with
     * {@link Arrays#hashCode(double[])} called using the components of the DD number
     * and fulfil the contract of {@link Object#hashCode()}, i.e. objects with different
     * hash codes are {@code false} for {@link Object#equals(Object)}.
     */
    @Test
    void testHashCode() {
        // Test some values match Arrays.hashCode(double[])
        final double[] values = {Double.NaN, Double.NEGATIVE_INFINITY, -3.45, -1, -0.0, 0.0, Double.MIN_VALUE, 1, 3.45,
            Double.POSITIVE_INFINITY};
        final ArrayList<DD> list = createCombinations(values);

        final String msg = "'equals' not compatible with 'hashCode'";

        for (final DD c : list) {
            final double hi = c.hi();
            final double lo = c.lo();
            final int expected = Arrays.hashCode(new double[] {hi + 0.0, lo + 0.0});
            final int hash = c.hashCode();
            Assertions.assertEquals(expected, hash, "hashCode does not match Arrays.hashCode({re, im})");

            // Test a copy has the same hash code, i.e. is not
            // System.identityHashCode(Object)
            final DD copy = DD.of(hi, lo);
            Assertions.assertEquals(hash, copy.hashCode(), "Copy hash code is not equal");

            // MATH-1118
            // "equals" and "hashCode" must be compatible: if two objects have
            // different hash codes, "equals" must return false.
            // Perform the smallest change to the two components.
            // Note: The hash could actually be the same so we check it changes.
            final double hiDelta = smallestChange(hi);
            final double loDelta = smallestChange(lo);
            Assertions.assertNotEquals(hi, hiDelta, "hi was not changed");
            Assertions.assertNotEquals(lo, loDelta, "lo was not changed");

            final DD cHiDelta = DD.of(hiDelta, lo);
            final DD cLoDelta = DD.of(hi, loDelta);
            if (hash != cHiDelta.hashCode()) {
                Assertions.assertNotEquals(c, cHiDelta, () -> "hi+delta: " + msg);
            }
            if (hash != cLoDelta.hashCode()) {
                Assertions.assertNotEquals(c, cLoDelta, () -> "lo+delta: " + msg);
            }
        }
    }

    /**
     * Specific test that different representations of zero satisfy the contract of
     * {@link Object#hashCode()}: if two objects have different hash codes, "equals" must
     * return false. This is an issue with using {@link Double#hashCode(double)} to create
     * hash codes and {@code ==} for equality when using different representations of
     * zero: Double.hashCode(-0.0) != Double.hashCode(0.0) but -0.0 == 0.0 is
     * {@code true}.
     *
     * @see <a
     * href="https://issues.apache.org/jira/projects/MATH/issues/MATH-1118">MATH-1118</a>
     */
    @Test
    void testHashCodeWithDifferentZeros() {
        final ArrayList<DD> list = createCombinations(-0.0, 0.0);

        // Explicit test for issue MATH-1118
        // "equals" and "hashCode" must be compatible
        for (int i = 0; i < list.size(); i++) {
            final DD c1 = list.get(i);
            for (int j = i; j < list.size(); j++) {
                final DD c2 = list.get(j);
                Assertions.assertEquals(c1.hashCode(), c2.hashCode());
                Assertions.assertEquals(c1, c2);
                Assertions.assertEquals(c2, c1);
            }
        }
    }

    /**
     * Creates a list of DD numbers using an all-vs-all combination of the provided
     * values for both the parts.
     *
     * @param values the values
     * @return the list
     */
    private static ArrayList<DD> createCombinations(double... values) {
        final ArrayList<DD> list = new ArrayList<>(values.length * values.length);
        for (final double x : values) {
            for (final double xx : values) {
                list.add(DD.of(x, xx));
            }
        }
        return list;
    }

    /**
     * Perform the smallest change to the value. This returns the next double value
     * adjacent to d in the direction of infinity. Edge cases: if already infinity then
     * return the next closest in the direction of negative infinity; if nan then return
     * 0.
     *
     * @param x the x
     * @return the new value
     */
    private static double smallestChange(double x) {
        if (Double.isNaN(x)) {
            return 0;
        }
        return x == Double.POSITIVE_INFINITY ? Math.nextDown(x) : Math.nextUp(x);
    }

    @ParameterizedTest
    @MethodSource
    void testToString(DD x) {
        final String s = x.toString();
        Assertions.assertEquals('(', s.charAt(0), "Start");
        Assertions.assertEquals(')', s.charAt(s.length() - 1), "End");
        final int i = s.indexOf(',');
        Assertions.assertEquals(Double.toString(x.hi()), s.substring(1, i));
        Assertions.assertEquals(Double.toString(x.lo()), s.substring(i + 1, s.length() - 1));
    }

    static Stream<DD> testToString() {
        final Stream.Builder<DD> builder = Stream.builder();
        final UniformRandomProvider rng = createRNG();
        for (int i = 0; i < 10; i++) {
            builder.add(signedNormalDoubleDouble(rng));
        }
        final double[] values = {Double.NaN, Double.NEGATIVE_INFINITY, -0.0, 0, 1, Double.MIN_VALUE,
            Double.POSITIVE_INFINITY};
        for (final double x : values) {
            for (final double xx : values) {
                builder.add(DD.of(x, xx));
            }
        }
        return builder.build();
    }

    @Test
    void testIsNotNormal() {
        for (double a : new double[] {Double.MAX_VALUE, 1.0, Double.MIN_NORMAL}) {
            Assertions.assertFalse(DD.isNotNormal(a));
            Assertions.assertFalse(DD.isNotNormal(-a));
        }
        for (double a : new double[] {Double.POSITIVE_INFINITY, 0.0, Double.MIN_VALUE,
                                      Math.nextDown(Double.MIN_NORMAL), Double.NaN}) {
            Assertions.assertTrue(DD.isNotNormal(a));
            Assertions.assertTrue(DD.isNotNormal(-a));
        }
    }

    /**
     * Creates a source of randomness.
     *
     * @return the uniform random provider
     */
    private static UniformRandomProvider createRNG() {
        return RandomSource.SPLIT_MIX_64.create();
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
     * Creates a normalized double in the range {@code [1, 2)} with a random sign. The
     * magnitude is sampled evenly from the 2<sup>52</sup> dyadic rationals in the range.
     *
     * @param rng Source of randomness.
     * @return the double
     */
    private static double signedNormalDouble(UniformRandomProvider rng) {
        return makeSignedNormalDouble(rng.nextLong());
    }

    /**
     * Creates a normalized double-double in the range {@code [1, 2)} with a random sign.
     *
     * @param rng Source of randomness.
     * @return the double-double
     */
    private static DD signedNormalDoubleDouble(UniformRandomProvider rng) {
        final double x = signedNormalDouble(rng);
        // The roundoff must be < 0.5 ULP of the value.
        // Math.ulp(1.0) = 2^-52.
        // Generate using +/- [0.25, 0.5) ULP.
        final double xx = 0x1.0p-54 * signedNormalDouble(rng);
        // Avoid assertNormalized here:
        // Since there is no chance of +/- 0.0 we use the faster assertion with no delta.
        Assertions.assertEquals(x, x + xx, "Failed to generate a random double-double in +/- [1, 2)");
        return DD.of(x, xx);
    }

    /**
     * Create a BigDecimal for the given value.
     *
     * @param v Value
     * @return the BigDecimal
     */
    private static BigDecimal bd(double v) {
        return new BigDecimal(v);
    }

    /**
     * Assert the number is normalized such that {@code |xx| <= eps * |x|}.
     *
     * @param x High part.
     * @param xx Low part.
     * @param name Name of the number.
     */
    private static void assertNormalized(double x, double xx, String name) {
        // Note: Use delta of 0 to allow addition of signed zeros (which may change the sign)
        Assertions.assertEquals(x, x + xx, 0.0, () -> name + " not a normalized double-double");
    }

    /**
     * Compute the max and mean error of operations.
     * Note: This is not a test and is included for reference.
     */
    @Test
    @org.junit.jupiter.api.Disabled("This fixture creates reference data")
    void computeError() {
        final int n = 100_000;
        final int samples = 10_000_000;
        final UniformRandomProvider rng = createRNG();
        final DD[] dd = IntStream.range(0, n).mapToObj(i ->
            signedNormalDoubleDouble(rng).scalb(rng.nextInt(106))
        ).toArray(DD[]::new);
        final BigDecimal[] bd = Arrays.stream(dd).map(DD::bigDecimalValue).toArray(BigDecimal[]::new);
        computeError(rng, dd, bd, samples, "add(DD)", BigDecimal::add, DD::add, DDExt::add);
        computeError(rng, dd, bd, samples, "add(double)",
            (a, b) -> a.add(bd(b.doubleValue())), (a, b) -> a.add(b.hi()), (a, b) -> DDExt.add(a, b.hi()));
        computeError(rng, dd, bd, samples, "add(double) two-sum",
            (a, b) -> a.add(bd(b.doubleValue())),
            (a, b) -> DD.twoSum(a.hi(), a.lo() + b.hi()),
            (a, b) -> DD.fastTwoSum(a.hi(), a.lo() + b.hi()));
        computeError(rng, dd, bd, samples, "multiply(DD)", BigDecimal::multiply, DD::multiply, DDExt::multiply);
        computeError(rng, dd, bd, samples, "multiply(double)",
            (a, b) -> a.multiply(bd(b.doubleValue())), (a, b) -> a.multiply(b.hi()), (a, b) -> DDExt.multiply(a, b.hi()));
        computeError(rng, dd, bd, n, "square()",
            a -> a.multiply(a), DD::square, DDExt::square);
        computeError(rng, dd, bd, samples, "divide(DD)", (a, b) -> a.divide(b, MC_DIVIDE), DD::divide, DDExt::divide);
        computeError(rng, dd, bd, samples, "divide(double)",
            (a, b) -> a.divide(bd(b.doubleValue()), MC_DIVIDE), (a, b) -> a.divide(b.hi()), null);
        computeError(rng, dd, bd, Math.min(n, samples), "reciprocal()",
            a -> BigDecimal.ONE.divide(a, MC_DIVIDE), DD::reciprocal, DDExt::reciprocal);
        // Requires compilation under JDK 9+
        //computeError(rng, dd, bd, Math.min(n, samples), "sqrt()",
        //    a -> a.abs().sqrt(MC_DIVIDE), a -> a.abs().sqrt(), a -> DDExt.sqrt(a.abs()));
    }

    /**
     * Compute the max and mean error of the operators. This method supports two operators
     * so they can be compared to a reference implementation.
     *
     * @param rng Source of randomness
     * @param dd DD data
     * @param bd DD data converted to BigDecimal
     * @param samples Number of samples
     * @param operatorName Operator name
     * @param expected Operator to compute the expected result
     * @param op1 First operator
     * @param op2 Second operator
     */
    private static void computeError(UniformRandomProvider rng,
        DD[] dd, BigDecimal[] bd, int samples,
        String operatorName,
        BinaryOperator<BigDecimal> reference,
        BinaryOperator<DD> op1,
        BinaryOperator<DD> op2) {
        final ErrorStatistics e1 = new ErrorStatistics();
        final ErrorStatistics e2 = new ErrorStatistics();
        final long start = System.nanoTime();
        for (int n = 0; n < samples; n++) {
            final int i = rng.nextInt(dd.length);
            final int j = rng.nextInt(dd.length);
            final BigDecimal expected = reference.apply(bd[i], bd[j]);
            final DD actual1 = op1.apply(dd[i], dd[j]);
            TestUtils.assertEquals(expected, actual1, -1.0, e1::add, () -> operatorName + " op1");
            if (op2 != null) {
                final DD actual2 = op2.apply(dd[i], dd[j]);
                TestUtils.assertEquals(expected, actual2, -1.0, e2::add, () -> operatorName + " op2");
            }
        }
        final long time = System.nanoTime() - start;
        TestUtils.printf("%-20s    %12.6g  %12.6g  %12.6g  %12.6g  %12.6g  %12.6g   (%.3fs)%n",
            operatorName,
            e1.getRMS() / EPS, e1.getMaxAbs() / EPS, e1.getMean() / EPS,
            e2.getRMS() / EPS, e2.getMaxAbs() / EPS, e2.getMean() / EPS,
            1e-9 * time);
    }

    /**
     * Compute the max and mean error of the operators. This method supports two operators
     * so they can be compared to a reference implementation.
     *
     * @param rng Source of randomness
     * @param dd DD data
     * @param bd DD data converted to BigDecimal
     * @param samples Number of samples
     * @param operatorName Operator name
     * @param expected Operator to compute the expected result
     * @param op1 First operator
     * @param op2 Second operator
     */
    private static void computeError(UniformRandomProvider rng,
        DD[] dd, BigDecimal[] bd, int samples,
        String operatorName,
        UnaryOperator<BigDecimal> reference,
        UnaryOperator<DD> op1,
        UnaryOperator<DD> op2) {
        final ErrorStatistics e1 = new ErrorStatistics();
        final ErrorStatistics e2 = new ErrorStatistics();
        final long start = System.nanoTime();
        final int[] count = {0};
        final IntSupplier next = samples == dd.length ? () -> count[0]++ : () -> rng.nextInt(dd.length);
        for (int n = 0; n < samples; n++) {
            final int i = next.getAsInt();
            final BigDecimal expected = reference.apply(bd[i]);
            final DD actual1 = op1.apply(dd[i]);
            TestUtils.assertEquals(expected, actual1, -1.0, e1::add, () -> operatorName + " op1");
            if (op2 != null) {
                final DD actual2 = op2.apply(dd[i]);
                TestUtils.assertEquals(expected, actual2, -1.0, e2::add, () -> operatorName + " op2");
            }
        }
        final long time = System.nanoTime() - start;
        TestUtils.printf("%-20s    %12.6g  %12.6g  %12.6g  %12.6g  %12.6g  %12.6g   (%.3fs)%n",
            operatorName,
            e1.getRMS() / EPS, e1.getMaxAbs() / EPS, e1.getMean() / EPS,
            e2.getRMS() / EPS, e2.getMaxAbs() / EPS, e2.getMean() / EPS,
            1e-9 * time);
    }

    /**
     * Class to compute the error statistics.
     *
     * <p>This class can be used to summarise relative errors if used as the DoubleConsumer
     * argument to {@link TestUtils#assertEquals(BigDecimal, DD, double, java.util.function.DoubleConsumer, Supplier)}.
     * Errors below the precision of a double-double number are treated as zero.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Root_mean_square">Wikipedia: RMS</a>
     */
    static class ErrorStatistics {
        /** Sum of squared error. */
        private DD ss = DD.ZERO;
        /** Maximum absolute error. */
        private double maxAbs;
        /** Number of terms. */
        private int n;
        /** Positive sum. */
        private DD ps = DD.ZERO;
        /** Negative sum. */
        private DD ns = DD.ZERO;

        /**
         * Add the relative error. Values below 2^-107 are ignored.
         *
         * @param x Value
         */
        void add(double x) {
            n++;
            // Ignore errors below 2^-107. This is the effective half ULP limit of DD and
            // it is not possible to get closer.
            if (Math.abs(x) <= 0x1.0p-107) {
                return;
            }
            // Overflow is not supported.
            // Assume the expected and actual are quite close when measuring the RMS.
            // Here we sum the regular square for speed.
            ss = add(ss, x * x);
            // Summing terms of the same sign avoids cancellation in the working sums.
            if (x < 0) {
                ns = add(ns, x);
                maxAbs = maxAbs < -x ? -x : maxAbs;
            } else {
                ps = add(ps, x);
                //ps = ps.add(x);
                maxAbs = maxAbs < x ? x : maxAbs;
            }
        }

        /**
         * Adds the term to the total.
         *
         * @param dd Total
         * @param x Value
         * @return the new total
         */
        private static DD add(DD dd, double x) {
            // We use a fastTwoSum here for speed. This is equivalent to a Kahan summation
            // of the total and is accurate if the total is larger than the terms.
            return DD.fastTwoSum(dd.hi(), dd.lo() + x);
        }

        /**
         * Gets the count of recorded values.
         *
         * @return the size
         */
        int size() {
            return n;
        }

        /**
         * Gets the maximum absolute error.
         *
         * <p>This can be used to set maximum ULP thresholds for test data if the
         * TestUtils.assertEquals method is used with a large maxUlps to measure the ulp
         * (and effectively ignore failures) and the maximum reported as the end of
         * testing.
         *
         * @return maximum absolute error
         */
        double getMaxAbs() {
            return maxAbs;
        }

        /**
         * Gets the root mean squared error (RMS).
         *
         * <p> Note: If no data has been added this will return 0/0 = nan.
         * This prevents using in assertions without adding data.
         *
         * @return root mean squared error (RMS)
         */
        double getRMS() {
            return n == 0 ? 0 : ss.divide(n).sqrt().doubleValue();
        }

        /**
         * Gets the mean error.
         *
         * <p>The mean can be used to determine if the error is consistently above or
         * below zero.
         *
         * @return mean error
         */
        double getMean() {
            return n == 0 ? 0 : ps.add(ns).divide(n).doubleValue();
        }
    }
}
