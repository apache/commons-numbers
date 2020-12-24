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
package org.apache.commons.numbers.fraction;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import org.apache.commons.numbers.core.TestUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BigFraction}.
 */
class BigFractionTest {

    /** The zero representation with positive denominator. */
    private static final BigFraction ZERO_P = BigFraction.of(0, 1);
    /** The zero representation with negative denominator. */
    private static final BigFraction ZERO_N = BigFraction.of(0, -1);

    private static void assertFraction(final int expectedNumerator, final int expectedDenominator, final BigFraction actual) {
        Assertions.assertEquals(expectedNumerator, actual.getNumeratorAsInt());
        Assertions.assertEquals(expectedDenominator, actual.getDenominatorAsInt());
        Assertions.assertEquals(
            Integer.signum(expectedNumerator) * Integer.signum(expectedDenominator),
            actual.signum());
    }

    private static void assertFraction(final long expectedNumerator, final long expectedDenominator, final BigFraction actual) {
        Assertions.assertEquals(expectedNumerator, actual.getNumeratorAsLong());
        Assertions.assertEquals(expectedDenominator, actual.getDenominatorAsLong());
        Assertions.assertEquals(
            Long.signum(expectedNumerator) * Long.signum(expectedDenominator),
            actual.signum());
    }

    private static void assertFraction(final BigInteger expectedNumerator, final BigInteger expectedDenominator, final BigFraction actual) {
        Assertions.assertEquals(expectedNumerator, actual.getNumerator());
        Assertions.assertEquals(expectedDenominator, actual.getDenominator());
        Assertions.assertEquals(
            expectedNumerator.signum() * expectedDenominator.signum(),
            actual.signum());
    }

    private static void assertDoubleValue(final double expected, final BigInteger numerator, final BigInteger denominator) {
        final BigFraction f = BigFraction.of(numerator, denominator);
        Assertions.assertEquals(expected, f.doubleValue());
    }

    private static void assertDoubleValue(final double expected, final long numerator, final long denominator) {
        assertDoubleValue(expected, BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
    }

    @Test
    void testConstructor() {
        for (final CommonTestCases.UnaryOperatorTestCase testCase : CommonTestCases.numDenConstructorTestCases()) {
            assertFraction(
                    testCase.expectedNumerator,
                    testCase.expectedDenominator,
                    BigFraction.of(testCase.operandNumerator, testCase.operandDenominator)
            );
        }

        // Long/BigInteger arguments
        assertFraction(0, 1, BigFraction.of(0L, 2L));
        assertFraction(1L, 1, BigFraction.of(1L));
        assertFraction(11, 1, BigFraction.of(11L));
        assertFraction(11, 1, BigFraction.of(new BigInteger("11")));

        // Divide by zero
        Assertions.assertThrows(ArithmeticException.class, () -> BigFraction.of(BigInteger.ONE, BigInteger.ZERO));

        // Null pointers
        Assertions.assertThrows(NullPointerException.class, () -> BigFraction.of(null, BigInteger.ONE));
        Assertions.assertThrows(NullPointerException.class, () -> BigFraction.of(BigInteger.ONE, null));
        Assertions.assertThrows(NullPointerException.class, () -> BigFraction.of(null));

        Assertions.assertThrows(ArithmeticException.class,
            () -> BigFraction.from(2.0 * Integer.MAX_VALUE, 1.0e-5, 100000));
    }

    @Test
    void testConstructorZero() {
        Assertions.assertSame(BigFraction.ZERO, BigFraction.from(0.0));
        Assertions.assertSame(BigFraction.ZERO, BigFraction.from(0.0, 1e-10, 100));
        Assertions.assertSame(BigFraction.ZERO, BigFraction.from(0.0, 100));
        Assertions.assertSame(BigFraction.ZERO, BigFraction.of(0));
        Assertions.assertSame(BigFraction.ZERO, BigFraction.of(0L));
        Assertions.assertSame(BigFraction.ZERO, BigFraction.of(BigInteger.ZERO));
        Assertions.assertSame(BigFraction.ZERO, BigFraction.of(0, 1));
        Assertions.assertSame(BigFraction.ZERO, BigFraction.of(0, -1));
        Assertions.assertSame(BigFraction.ZERO, BigFraction.of(0L, 1L));
        Assertions.assertSame(BigFraction.ZERO, BigFraction.of(0L, -1L));
        Assertions.assertSame(BigFraction.ZERO, BigFraction.of(BigInteger.ZERO, BigInteger.ONE));
        Assertions.assertSame(BigFraction.ZERO, BigFraction.of(BigInteger.ZERO, BigInteger.ONE.negate()));
    }

    // MATH-179
    @Test
    void testDoubleConstructor() throws Exception {
        for (final CommonTestCases.DoubleToFractionTestCase testCase : CommonTestCases.doubleConstructorTestCases()) {
            assertFraction(
                    testCase.expectedNumerator,
                    testCase.expectedDenominator,
                    BigFraction.from(testCase.operand, 1.0e-5, 100)
            );
        }

        // Cases with different exact results from Fraction
        assertFraction(6004799503160661L, 18014398509481984L, BigFraction.from(1.0 / 3.0));
        assertFraction(6124895493223875L, 36028797018963968L, BigFraction.from(17.0 / 100.0));
        assertFraction(1784551352345559L, 562949953421312L, BigFraction.from(317.0 / 100.0));
        assertFraction(-6004799503160661L, 18014398509481984L, BigFraction.from(-1.0 / 3.0));
        assertFraction(-6124895493223875L, 36028797018963968L, BigFraction.from(17.0 / -100.0));
        assertFraction(-1784551352345559L, 562949953421312L, BigFraction.from(-317.0 / 100.0));

        // Extreme double values
        Assertions.assertEquals(1L, BigFraction.from(Double.MAX_VALUE).getDenominatorAsLong());
        Assertions.assertEquals(1L, BigFraction.from(Double.longBitsToDouble(0x0010000000000000L)).getNumeratorAsLong());
        assertFraction(BigInteger.ONE, BigInteger.ONE.shiftLeft(1074), BigFraction.from(Double.MIN_VALUE));

        // Check exact round-trip of double
        Assertions.assertEquals(0.00000000000001, BigFraction.from(0.00000000000001).doubleValue());
        Assertions.assertEquals(0.40000000000001, BigFraction.from(0.40000000000001).doubleValue());
        Assertions.assertEquals(15.0000000000001, BigFraction.from(15.0000000000001).doubleValue());
        // Check the representation
        assertFraction(3602879701896487L, 9007199254740992L, BigFraction.from(0.40000000000001));
        assertFraction(1055531162664967L, 70368744177664L, BigFraction.from(15.0000000000001));
    }

    // MATH-181
    // NUMBERS-147
    @Test
    void testDoubleConstructorWithMaxDenominator() throws Exception {
        for (final CommonTestCases.DoubleToFractionTestCase testCase : CommonTestCases.doubleMaxDenomConstructorTestCases()) {
            assertFraction(
                    testCase.expectedNumerator,
                    testCase.expectedDenominator,
                    BigFraction.from(testCase.operand, testCase.maxDenominator)
            );
        }

        // Cases with different exact results from Fraction
        final long pow31 = 1L << 31;
        assertFraction(pow31, 1, BigFraction.from(Integer.MIN_VALUE * -1.0, 2));
        assertFraction(pow31, 3, BigFraction.from(Integer.MIN_VALUE / -3.0, 10));
        assertFraction(-1, pow31, BigFraction.from(1.0 / Integer.MIN_VALUE, Integer.MIN_VALUE));
        assertFraction(1, pow31, BigFraction.from(-1.0 / Integer.MIN_VALUE, Integer.MIN_VALUE));

        Assertions.assertThrows(IllegalArgumentException.class, () -> BigFraction.from(1.0, 0));
    }

    @Test
    void testDoubleConstructorThrows() {
        final double eps = 1e-5;
        final int maxIterations = Integer.MAX_VALUE;
        final int maxDenominator = Integer.MAX_VALUE;
        for (final double value : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> BigFraction.from(value));
            Assertions.assertThrows(IllegalArgumentException.class, () -> BigFraction.from(value, eps, maxIterations));
            Assertions.assertThrows(IllegalArgumentException.class, () -> BigFraction.from(value, maxDenominator));
        }
        Assertions.assertThrows(IllegalArgumentException.class, () -> BigFraction.from(1.0, Double.NaN, maxIterations));
        Assertions.assertThrows(IllegalArgumentException.class, () -> BigFraction.from(1.0, -1.0, maxIterations));
        Assertions.assertThrows(IllegalArgumentException.class, () -> BigFraction.from(1.0, eps, 0));
        // Test a zero epsilon is allowed
        assertFraction(1, 1, BigFraction.from(1.0, 0, maxIterations));
    }

    @Test
    void testDoubleConstructorGoldenRatioThrows() {
        // the golden ratio is notoriously a difficult number for continuous fraction
        Assertions.assertThrows(FractionException.class,
            () -> BigFraction.from((1 + Math.sqrt(5)) / 2, 1.0e-12, 25)
        );
    }

    // MATH-1029
    @Test
    void testDoubleConstructorWithMaxDenominatorOverFlow() {
        Assertions.assertThrows(ArithmeticException.class,
            () -> BigFraction.from(1e10, 1000)
        );
        Assertions.assertThrows(ArithmeticException.class,
            () -> BigFraction.from(-1e10, 1000)
        );
    }

    @Test
    void testDoubleConstructorOverflow() {
        assertDoubleConstructorOverflow(0.75000000001455192);
        assertDoubleConstructorOverflow(1.0e10);
        assertDoubleConstructorOverflow(-1.0e10);
        assertDoubleConstructorOverflow(-43979.60679604749);
    }

    private void assertDoubleConstructorOverflow(final double a) {
        Assertions.assertThrows(ArithmeticException.class,
            () -> BigFraction.from(a, 1.0e-12, 1000)
        );
    }

    @Test
    void testDoubleConstructorWithEpsilonLimit() throws Exception {
        assertFraction(2, 5, BigFraction.from(0.4, 1.0e-5, 100));

        assertFraction(3, 5,      BigFraction.from(0.6152, 0.02, 100));
        assertFraction(8, 13,     BigFraction.from(0.6152, 1.0e-3, 100));
        assertFraction(251, 408,  BigFraction.from(0.6152, 1.0e-4, 100));
        assertFraction(251, 408,  BigFraction.from(0.6152, 1.0e-5, 100));
        assertFraction(510, 829,  BigFraction.from(0.6152, 1.0e-6, 100));
        assertFraction(769, 1250, BigFraction.from(0.6152, 1.0e-7, 100));
    }

    @Test
    void testCompareTo() {
        final BigFraction a = BigFraction.of(1, 2);
        final BigFraction b = BigFraction.of(1, 3);
        final BigFraction c = BigFraction.of(1, 2);
        final BigFraction d = BigFraction.of(-1, 2);
        final BigFraction e = BigFraction.of(1, -2);
        final BigFraction f = BigFraction.of(-1, -2);
        final BigFraction g = BigFraction.of(-1, Integer.MIN_VALUE);

        Assertions.assertEquals(0, a.compareTo(a));
        Assertions.assertEquals(0, a.compareTo(c));
        Assertions.assertEquals(1, a.compareTo(b));
        Assertions.assertEquals(-1, b.compareTo(a));
        Assertions.assertEquals(-1, d.compareTo(a));
        Assertions.assertEquals(1, a.compareTo(d));
        Assertions.assertEquals(-1, e.compareTo(a));
        Assertions.assertEquals(1, a.compareTo(e));
        Assertions.assertEquals(0, d.compareTo(e));
        Assertions.assertEquals(0, a.compareTo(f));
        Assertions.assertEquals(0, f.compareTo(a));
        Assertions.assertEquals(1, f.compareTo(e));
        Assertions.assertEquals(-1, e.compareTo(f));
        Assertions.assertEquals(-1, g.compareTo(a));
        Assertions.assertEquals(-1, g.compareTo(f));
        Assertions.assertEquals(1, a.compareTo(g));
        Assertions.assertEquals(-1, d.compareTo(g));

        Assertions.assertEquals(0, BigFraction.of(0, 3).compareTo(BigFraction.of(0, -2)));

        // these two values are different approximations of PI
        // the first  one is approximately PI - 3.07e-18
        // the second one is approximately PI + 1.936e-17
        final BigFraction pi1 = BigFraction.of(1068966896, 340262731);
        final BigFraction pi2 = BigFraction.of(411557987, 131002976);
        Assertions.assertEquals(-1, pi1.compareTo(pi2));
        Assertions.assertEquals(1, pi2.compareTo(pi1));
        Assertions.assertEquals(0.0, pi1.doubleValue() - pi2.doubleValue(), 1.0e-20);

        Assertions.assertEquals(0, ZERO_P.compareTo(ZERO_N));
    }

    @Test
    void testDoubleValue() {
        assertDoubleValue(0.5, 1, 2);
        assertDoubleValue(-0.5, -1, 2);
        assertDoubleValue(-0.5, 1, -2);
        assertDoubleValue(0.5, -1, -2);
        assertDoubleValue(1.0 / 3.0, 1, 3);

        Assertions.assertEquals(0.0, BigFraction.ZERO.doubleValue());
        Assertions.assertEquals(0.0, ZERO_P.doubleValue());
        Assertions.assertEquals(0.0, ZERO_N.doubleValue());

        // NUMBERS-120
        assertDoubleValue(
                2d - 0x1P-52,
                1L << 54,
                (1L << 53) + 1L
        );

        assertDoubleValue(
                2d,
                (1L << 54) - 1L,
                1L << 53
        );
        assertDoubleValue(
                1d,
                (1L << 53) + 1L,
                1L << 53
        );
    }

    @Test
    void testDoubleValueForSubnormalNumbers() {
        assertDoubleValue(
                //Double.MIN_VALUE * 2/3
                Double.MIN_VALUE,
                BigInteger.ONE,
                BigInteger.ONE.shiftLeft(1073).multiply(BigInteger.valueOf(3L))
        );

        assertDoubleValue(
                Double.MIN_VALUE,
                BigInteger.ONE,
                BigInteger.ONE.shiftLeft(1074)
        );
        assertDoubleValue(
                Double.MIN_VALUE * 2,
                BigInteger.valueOf(2),
                BigInteger.ONE.shiftLeft(1074)
        );
        assertDoubleValue(
                Double.MIN_VALUE * 3,
                BigInteger.valueOf(3),
                BigInteger.ONE.shiftLeft(1074)
        );

        assertDoubleValue(
                Double.MIN_NORMAL - Double.MIN_VALUE,
                BigInteger.ONE.shiftLeft(52).subtract(BigInteger.ONE),
                BigInteger.ONE.shiftLeft(1074)
        );
        assertDoubleValue(
                Double.MIN_NORMAL - 2 * Double.MIN_VALUE,
                BigInteger.ONE.shiftLeft(52).subtract(BigInteger.valueOf(2)),
                BigInteger.ONE.shiftLeft(1074)
        );

        //this number is smaller than Double.MIN_NORMAL, but should round up to it
        assertDoubleValue(
                Double.MIN_NORMAL,
                BigInteger.ONE.shiftLeft(53).subtract(BigInteger.ONE),
                BigInteger.ONE.shiftLeft(1075)
        );
    }

    @Test
    void testDoubleValueForInfinities() {
        //the smallest integer that rounds up to Double.POSITIVE_INFINITY
        final BigInteger minInf = BigInteger.ONE
                .shiftLeft(1024)
                .subtract(BigInteger.ONE.shiftLeft(970));

        assertDoubleValue(
                Double.NEGATIVE_INFINITY,
                minInf.negate(),
                BigInteger.ONE
        );
        assertDoubleValue(
                Double.POSITIVE_INFINITY,
                minInf,
                BigInteger.ONE
        );
    }

    // MATH-744
    @Test
    void testDoubleValueForLargeNumeratorAndDenominator() {
        final BigInteger pow400 = BigInteger.TEN.pow(400);
        final BigInteger pow401 = BigInteger.TEN.pow(401);
        final BigInteger two = new BigInteger("2");
        final BigFraction large = BigFraction.of(pow401.add(BigInteger.ONE),
                                                 pow400.multiply(two));

        Assertions.assertEquals(5, large.doubleValue(), 1e-15);
    }

    // MATH-744
    @Test
    void testFloatValueForLargeNumeratorAndDenominator() {
        final BigInteger pow400 = BigInteger.TEN.pow(400);
        final BigInteger pow401 = BigInteger.TEN.pow(401);
        final BigInteger two = new BigInteger("2");
        final BigFraction large = BigFraction.of(pow401.add(BigInteger.ONE),
                                                 pow400.multiply(two));

        Assertions.assertEquals(5, large.floatValue(), 1e-15);
    }

    @Test
    void testDoubleValueForLargeNumeratorAndSmallDenominator() {
        // NUMBERS-15
        final BigInteger pow300 = BigInteger.TEN.pow(300);
        final BigInteger pow330 = BigInteger.TEN.pow(330);
        final BigFraction large = BigFraction.of(pow330.add(BigInteger.ONE),
                                                 pow300);

        Assertions.assertEquals(1e30, large.doubleValue(), 1e-15);

        // NUMBERS-120
        assertDoubleValue(
                5.992310449541053E307,
                BigInteger.ONE
                        .shiftLeft(1024)
                        .subtract(BigInteger.ONE.shiftLeft(970))
                        .add(BigInteger.ONE),
                BigInteger.valueOf(3)
        );

        assertDoubleValue(
                Double.MAX_VALUE,
                BigInteger.ONE
                        .shiftLeft(1025)
                        .subtract(BigInteger.ONE.shiftLeft(972))
                        .subtract(BigInteger.ONE),
                BigInteger.valueOf(2)
        );
    }

    // NUMBERS-15
    @Test
    void testFloatValueForLargeNumeratorAndSmallDenominator() {
        final BigInteger pow30 = BigInteger.TEN.pow(30);
        final BigInteger pow40 = BigInteger.TEN.pow(40);
        final BigFraction large = BigFraction.of(pow40.add(BigInteger.ONE),
                                                 pow30);

        Assertions.assertEquals(1e10f, large.floatValue(), 1e-15);
    }

    @Test
    void testFloatValue() {
        Assertions.assertEquals(0.5f, BigFraction.of(1, 2).floatValue());
        Assertions.assertEquals(0.5f, BigFraction.of(-1, -2).floatValue());
        Assertions.assertEquals(-0.5f, BigFraction.of(-1, 2).floatValue());
        Assertions.assertEquals(-0.5f, BigFraction.of(1, -2).floatValue());

        final float e = 1f / 3f;
        Assertions.assertEquals(e, BigFraction.of(1, 3).floatValue());
        Assertions.assertEquals(e, BigFraction.of(-1, -3).floatValue());
        Assertions.assertEquals(-e, BigFraction.of(-1, 3).floatValue());
        Assertions.assertEquals(-e, BigFraction.of(1, -3).floatValue());

        Assertions.assertEquals(0.0f, ZERO_P.floatValue());
        Assertions.assertEquals(0.0f, ZERO_N.floatValue());
    }

    @Test
    void testIntValue() {
        Assertions.assertEquals(0, BigFraction.of(1, 2).intValue());
        Assertions.assertEquals(0, BigFraction.of(-1, -2).intValue());
        Assertions.assertEquals(0, BigFraction.of(-1, 2).intValue());
        Assertions.assertEquals(0, BigFraction.of(1, -2).intValue());

        Assertions.assertEquals(1, BigFraction.of(3, 2).intValue());
        Assertions.assertEquals(1, BigFraction.of(-3, -2).intValue());
        Assertions.assertEquals(-1, BigFraction.of(-3, 2).intValue());
        Assertions.assertEquals(-1, BigFraction.of(3, -2).intValue());

        Assertions.assertEquals(0, ZERO_P.intValue());
        Assertions.assertEquals(0, ZERO_N.intValue());
    }

    @Test
    void testLongValue() {
        Assertions.assertEquals(0L, BigFraction.of(1, 2).longValue());
        Assertions.assertEquals(0L, BigFraction.of(-1, -2).longValue());
        Assertions.assertEquals(0L, BigFraction.of(-1, 2).longValue());
        Assertions.assertEquals(0L, BigFraction.of(1, -2).longValue());

        Assertions.assertEquals(1L, BigFraction.of(3, 2).longValue());
        Assertions.assertEquals(1L, BigFraction.of(-3, -2).longValue());
        Assertions.assertEquals(-1L, BigFraction.of(-3, 2).longValue());
        Assertions.assertEquals(-1L, BigFraction.of(3, -2).longValue());

        Assertions.assertEquals(0, ZERO_P.longValue());
        Assertions.assertEquals(0, ZERO_N.longValue());
    }

    @Test
    void testBigDecimalValue() {
        Assertions.assertEquals(new BigDecimal(0.5), BigFraction.of(1, 2).bigDecimalValue());
        Assertions.assertEquals(new BigDecimal("0.0003"), BigFraction.of(3, 10000).bigDecimalValue());
        Assertions.assertEquals(new BigDecimal("0"), BigFraction.of(1, 3).bigDecimalValue(RoundingMode.DOWN));
        Assertions.assertEquals(new BigDecimal("0.333"), BigFraction.of(1, 3).bigDecimalValue(3, RoundingMode.DOWN));
    }

    @Test
    void testAbs() {
        for (final CommonTestCases.UnaryOperatorTestCase testCase : CommonTestCases.absTestCases()) {
            final BigFraction f = BigFraction.of(testCase.operandNumerator, testCase.operandDenominator);
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f.abs());
        }
    }

    @Test
    void testReciprocal() {
        for (final CommonTestCases.UnaryOperatorTestCase testCase : CommonTestCases.reciprocalTestCases()) {
            final BigFraction f = BigFraction.of(testCase.operandNumerator, testCase.operandDenominator);
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f.reciprocal());
        }

        final BigFraction f = BigFraction.of(0, 3);
        Assertions.assertThrows(ArithmeticException.class, f::reciprocal);
    }

    @Test
    void testNegate() {
        for (final CommonTestCases.UnaryOperatorTestCase testCase : CommonTestCases.negateTestCases()) {
            final BigFraction f = BigFraction.of(testCase.operandNumerator, testCase.operandDenominator);
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f.negate());
        }
    }

    @Test
    void testAdd() {
        for (final CommonTestCases.BinaryOperatorTestCase testCase : CommonTestCases.addFractionTestCases()) {
            final BigFraction f1 = BigFraction.of(testCase.firstOperandNumerator, testCase.firstOperandDenominator);
            final BigFraction f2 = BigFraction.of(testCase.secondOperandNumerator, testCase.secondOperandDenominator);
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f1.add(f2));
        }
        for (final CommonTestCases.BinaryIntOperatorTestCase testCase : CommonTestCases.addIntTestCases()) {
            final BigFraction f1 = BigFraction.of(testCase.firstOperandNumerator, testCase.firstOperandDenominator);
            final int i2 = testCase.secondOperand;
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f1.add(i2));
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f1.add((long) i2));
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f1.add(BigInteger.valueOf(i2)));
        }

        Assertions.assertThrows(NullPointerException.class, () -> BigFraction.ONE.add((BigFraction) null));
        Assertions.assertThrows(NullPointerException.class, () -> BigFraction.ONE.add((BigInteger) null));

        // Special cases
        final BigFraction f2 = BigFraction.of(1, 2);
        assertFraction(1, 2, f2.add(BigInteger.ZERO));
        assertFraction(12, 1, BigFraction.ZERO.add(BigInteger.valueOf(12)));
    }

    @Test
    void testDivide() {
        for (final CommonTestCases.BinaryOperatorTestCase testCase : CommonTestCases.divideByFractionTestCases()) {
            final BigFraction f1 = BigFraction.of(testCase.firstOperandNumerator, testCase.firstOperandDenominator);
            final BigFraction f2 = BigFraction.of(testCase.secondOperandNumerator, testCase.secondOperandDenominator);
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f1.divide(f2));
        }
        for (final CommonTestCases.BinaryIntOperatorTestCase testCase : CommonTestCases.divideByIntTestCases()) {
            final BigFraction f1 = BigFraction.of(testCase.firstOperandNumerator, testCase.firstOperandDenominator);
            final int i2 = testCase.secondOperand;
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f1.divide(i2));
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f1.divide((long) i2));
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f1.divide(BigInteger.valueOf(i2)));
        }

        Assertions.assertThrows(NullPointerException.class, () -> BigFraction.ONE.divide((BigFraction) null));
        Assertions.assertThrows(NullPointerException.class, () -> BigFraction.ONE.divide((BigInteger) null));

        Assertions.assertThrows(FractionException.class, () -> BigFraction.of(1, 2).divide(BigFraction.ZERO));
        Assertions.assertThrows(FractionException.class, () -> BigFraction.of(1, 2).divide(0));
        Assertions.assertThrows(FractionException.class, () -> BigFraction.of(1, 2).divide(0L));
        Assertions.assertThrows(FractionException.class, () -> BigFraction.of(1, 2).divide(BigInteger.ZERO));

        // Special cases
        final BigFraction f1 = BigFraction.of(Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertFraction(-1, -Integer.MAX_VALUE, f1.divide(Integer.MIN_VALUE));
        assertFraction(-1, -Integer.MAX_VALUE, f1.divide((long) Integer.MIN_VALUE));
        assertFraction(-1, -Integer.MAX_VALUE, f1.divide(BigInteger.valueOf(Integer.MIN_VALUE)));
    }

    @Test
    void testMultiply() {
        for (final CommonTestCases.BinaryOperatorTestCase testCase : CommonTestCases.multiplyByFractionTestCases()) {
            final BigFraction f1 = BigFraction.of(testCase.firstOperandNumerator, testCase.firstOperandDenominator);
            final BigFraction f2 = BigFraction.of(testCase.secondOperandNumerator, testCase.secondOperandDenominator);
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f1.multiply(f2));
        }
        for (final CommonTestCases.BinaryIntOperatorTestCase testCase : CommonTestCases.multiplyByIntTestCases()) {
            final BigFraction f1 = BigFraction.of(testCase.firstOperandNumerator, testCase.firstOperandDenominator);
            final int i2 = testCase.secondOperand;
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f1.multiply(i2));
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f1.multiply((long) i2));
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f1.multiply(BigInteger.valueOf(i2)));
        }

        Assertions.assertThrows(NullPointerException.class, () -> BigFraction.ONE.multiply((BigFraction) null));
        Assertions.assertThrows(NullPointerException.class, () -> BigFraction.ONE.multiply((BigInteger) null));
    }

    @Test
    void testPow() {
        for (final CommonTestCases.BinaryIntOperatorTestCase testCase : CommonTestCases.powTestCases()) {
            final BigFraction f1 = BigFraction.of(testCase.firstOperandNumerator, testCase.firstOperandDenominator);
            final int exponent = testCase.secondOperand;
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f1.pow(exponent));
        }

        // Note: BigInteger magnitude is limited to 2^Integer.MAX_VALUE exclusive
        // in the reference implementation (up to at least JDK 14).
        Assertions.assertThrows(ArithmeticException.class, () -> BigFraction.of(2).pow(Integer.MAX_VALUE));
        Assertions.assertThrows(ArithmeticException.class, () -> BigFraction.of(1, 2).pow(Integer.MAX_VALUE));
        Assertions.assertThrows(ArithmeticException.class, () -> BigFraction.of(2).pow(-Integer.MAX_VALUE));
        Assertions.assertThrows(ArithmeticException.class, () -> BigFraction.of(1, 2).pow(-Integer.MAX_VALUE));
    }

    @Test
    void testSubtract() {
        for (final CommonTestCases.BinaryOperatorTestCase testCase : CommonTestCases.subtractFractionTestCases()) {
            final BigFraction f1 = BigFraction.of(testCase.firstOperandNumerator, testCase.firstOperandDenominator);
            final BigFraction f2 = BigFraction.of(testCase.secondOperandNumerator, testCase.secondOperandDenominator);
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f1.subtract(f2));
        }
        for (final CommonTestCases.BinaryIntOperatorTestCase testCase : CommonTestCases.subtractIntTestCases()) {
            final BigFraction f1 = BigFraction.of(testCase.firstOperandNumerator, testCase.firstOperandDenominator);
            final int i2 = testCase.secondOperand;
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f1.subtract(i2));
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f1.subtract((long) i2));
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f1.subtract(BigInteger.valueOf(i2)));
        }

        Assertions.assertThrows(NullPointerException.class, () -> BigFraction.ONE.subtract((BigFraction) null));
        Assertions.assertThrows(NullPointerException.class, () -> BigFraction.ONE.subtract((BigInteger) null));
    }

    @Test
    void testEqualsAndHashCode() {
        final BigFraction zero = BigFraction.of(0, 1);
        Assertions.assertTrue(zero.equals(zero));
        Assertions.assertFalse(zero.equals(null));
        Assertions.assertFalse(zero.equals(new Object()));
        Assertions.assertFalse(zero.equals(Double.valueOf(0)));

        // Equal to same rational number
        final BigFraction zero2 = BigFraction.of(0, 2);
        assertEqualAndHashCodeEqual(zero, zero2);

        // Not equal to different rational number
        final BigFraction one = BigFraction.of(1, 1);
        Assertions.assertNotEquals(zero, one);
        Assertions.assertNotEquals(one, zero);

        // Test using different representations of the same fraction
        // (Denominators are primes)
        for (final int[] f : new int[][] {{1, 1}, {2, 3}, {6826, 15373}, {1373, 103813}, {0, 3}}) {
            final int num = f[0];
            final int den = f[1];
            BigFraction f1 = BigFraction.of(-num, den);
            BigFraction f2 = BigFraction.of(num, -den);
            assertEqualAndHashCodeEqual(f1, f2);
            assertEqualAndHashCodeEqual(f2, f1);
            f1 = BigFraction.of(num, den);
            f2 = BigFraction.of(-num, -den);
            assertEqualAndHashCodeEqual(f1, f2);
            assertEqualAndHashCodeEqual(f2, f1);
        }

        // Same numerator or denominator as 1/1
        final BigFraction half = BigFraction.of(1, 2);
        final BigFraction two = BigFraction.of(2, 1);
        Assertions.assertNotEquals(one, half);
        Assertions.assertNotEquals(one, two);
    }

    /**
     * Assert the two fractions are equal. The contract of {@link Object#hashCode()} requires
     * that the hash code must also be equal.
     *
     * <p>Ideally this method should not be called with the same instance for both arguments.
     * It is intended to be used to test different objects that are equal have the same hash code.
     * However the same object may be constructed for different arguments using factory
     * constructors, e.g. zero.
     *
     * @param f1 Fraction 1.
     * @param f2 Fraction 2.
     */
    private static void assertEqualAndHashCodeEqual(final BigFraction f1, final BigFraction f2) {
        Assertions.assertEquals(f1, f2);
        Assertions.assertEquals(f1.hashCode(), f2.hashCode(), "Equal fractions have different hashCode");
        // Check the computation matches the result of Arrays.hashCode and the signum.
        // This is not mandated but is a recommendation.
        final int expected = f1.signum() *
                             Arrays.hashCode(new Object[] {f1.getNumerator().abs(),
                                                           f1.getDenominator().abs()});
        Assertions.assertEquals(expected, f1.hashCode(), "Hashcode not equal to using Arrays.hashCode");
    }

    @Test
    void testAdditiveNeutral() {
        Assertions.assertEquals(BigFraction.ZERO, BigFraction.ONE.zero());
    }

    @Test
    void testMultiplicativeNeutral() {
        Assertions.assertEquals(BigFraction.ONE, BigFraction.ZERO.one());
    }

    @Test
    void testSerial() {
        final BigFraction[] fractions = {
            BigFraction.of(3, 4), BigFraction.ONE, BigFraction.ZERO,
            BigFraction.of(17), BigFraction.from(Math.PI, 1000),
            BigFraction.of(-5, 2)
        };
        for (final BigFraction fraction : fractions) {
            Assertions.assertEquals(fraction,
                                    TestUtils.serializeAndRecover(fraction));
        }
    }

    @Test
    void testToString() {
        Assertions.assertEquals("0", BigFraction.of(0, 3).toString());
        Assertions.assertEquals("0", BigFraction.of(0, -3).toString());
        Assertions.assertEquals("3", BigFraction.of(6, 2).toString());
        Assertions.assertEquals("2 / 3", BigFraction.of(18, 27).toString());
        Assertions.assertEquals("-10 / 11", BigFraction.of(-10, 11).toString());
        Assertions.assertEquals("10 / -11", BigFraction.of(10, -11).toString());
    }

    @Test
    void testParse() {
        final String[] validExpressions = new String[] {
            "1 / 2",
            "-1 / 2",
            "1 / -2",
            "-1 / -2",
            "01 / 2",
            "01 / 02",
            "-01 / 02",
            "01 / -02",
            "15 / 16",
            "-2 / 3",
            "8 / 7",
            "5",
            "-3",
            "-3",
            "2147,483,647 / 2,147,483,648", //over largest int value
            "9,223,372,036,854,775,807 / 9,223,372,036,854,775,808" //over largest long value
        };
        final BigFraction[] fractions = {
                BigFraction.of(1, 2),
                BigFraction.of(-1, 2),
                BigFraction.of(1, -2),
                BigFraction.of(-1, -2),
                BigFraction.of(1, 2),
                BigFraction.of(1, 2),
                BigFraction.of(-1, 2),
                BigFraction.of(1, -2),
                BigFraction.of(15, 16),
                BigFraction.of(-2, 3),
                BigFraction.of(8, 7),
                BigFraction.of(5, 1),
                BigFraction.of(-3, 1),
                BigFraction.of(3, -1),
                BigFraction.of(2147483647, 2147483648L),
                BigFraction.of(new BigInteger("9223372036854775807"),
                               new BigInteger("9223372036854775808"))
        };
        int inc = 0;
        for (final BigFraction fraction: fractions) {
            Assertions.assertEquals(fraction,
                                    BigFraction.parse(validExpressions[inc]));
            inc++;
        }

        Assertions.assertThrows(NumberFormatException.class, () -> BigFraction.parse("1 // 2"));
        Assertions.assertThrows(NumberFormatException.class, () -> BigFraction.parse("1 / z"));
        Assertions.assertThrows(NumberFormatException.class, () -> BigFraction.parse("1 / --2"));
        Assertions.assertThrows(NumberFormatException.class, () -> BigFraction.parse("x"));
    }

    @Test
    void testMath340() {
        final BigFraction fractionA = BigFraction.from(0.00131);
        final BigFraction fractionB = BigFraction.from(.37).reciprocal();
        final BigFraction errorResult = fractionA.multiply(fractionB);
        final BigFraction correctResult = BigFraction.of(fractionA.getNumerator().multiply(fractionB.getNumerator()),
                                                   fractionA.getDenominator().multiply(fractionB.getDenominator()));
        Assertions.assertEquals(correctResult, errorResult);
    }

    @Test
    void testNumbers150() {
        // zero to negative powers should throw an exception
        Assertions.assertThrows(ArithmeticException.class, () -> BigFraction.ZERO.pow(-1));
        Assertions.assertThrows(ArithmeticException.class, () -> BigFraction.ZERO.pow(Integer.MIN_VALUE));

        // shall overflow
        Assertions.assertThrows(ArithmeticException.class, () -> BigFraction.of(2).pow(Integer.MIN_VALUE));
        Assertions.assertThrows(ArithmeticException.class, () -> BigFraction.of(1, 2).pow(Integer.MIN_VALUE));
    }
}
