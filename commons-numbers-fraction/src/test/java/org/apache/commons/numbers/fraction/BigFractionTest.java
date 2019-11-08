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
import org.apache.commons.numbers.core.TestUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class BigFractionTest {

    private static void assertFraction(long expectedNumerator, long expectedDenominator, BigFraction actual) {
        Assertions.assertEquals(BigInteger.valueOf(expectedNumerator), actual.getNumerator());
        Assertions.assertEquals(BigInteger.valueOf(expectedDenominator), actual.getDenominator());
    }

    private static void assertFraction(BigInteger expectedNumerator, BigInteger expectedDenominator, BigFraction actual) {
        Assertions.assertEquals(expectedNumerator, actual.getNumerator());
        Assertions.assertEquals(expectedDenominator, actual.getDenominator());
    }

    private static void assertDoubleValue(double expected, BigInteger numerator, BigInteger denominator) {
        BigFraction f = BigFraction.of(numerator, denominator);
        Assertions.assertEquals(expected, f.doubleValue());
    }

    private static void assertDoubleValue(double expected, long numerator, long denominator) {
        assertDoubleValue(expected, BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
    }

    @Test
    public void testConstructor() {
        for (CommonTestCases.UnaryOperatorTestCase testCase : CommonTestCases.numDenConstructorTestCases()) {
            assertFraction(
                    testCase.expectedNumerator,
                    testCase.expectedDenominator,
                    BigFraction.of(testCase.operandNumerator, testCase.operandDenominator)
            );
        }

        assertFraction(0, 1, BigFraction.of(0L, 2L));
        assertFraction(1L, 1, BigFraction.of(1L));
        assertFraction(11, 1, BigFraction.of(11L));
        assertFraction(11, 1, BigFraction.of(new BigInteger("11")));

        Assertions.assertEquals(0.00000000000001, BigFraction.from(0.00000000000001).doubleValue(), 0.0);
        Assertions.assertEquals(0.40000000000001, BigFraction.from(0.40000000000001).doubleValue(), 0.0);
        Assertions.assertEquals(15.0000000000001, BigFraction.from(15.0000000000001).doubleValue(), 0.0);
        assertFraction(3602879701896487L, 9007199254740992L, BigFraction.from(0.40000000000001));
        assertFraction(1055531162664967L, 70368744177664L, BigFraction.from(15.0000000000001));
        try {
            BigFraction.of(null, BigInteger.ONE);
            Assertions.fail("Expecting NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            BigFraction.of(BigInteger.ONE, null);
            Assertions.fail("Expecting NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            BigFraction.of(BigInteger.ONE, BigInteger.ZERO);
            Assertions.fail("Expecting ArithmeticException");
        } catch (ArithmeticException ignored) {
            // expected
        }
        try {
            BigFraction.from(2.0 * Integer.MAX_VALUE, 1.0e-5, 100000);
            Assertions.fail("Expecting ArithmeticException");
        } catch (ArithmeticException ignored) {
            // expected
        }
    }

    @Test
    public void testGoldenRatio() {
        // the golden ratio is notoriously a difficult number for continuous fraction
        Assertions.assertThrows(FractionException.class,
            () -> BigFraction.from((1 + Math.sqrt(5)) / 2, 1.0e-12, 25)
        );
    }

    // MATH-179
    @Test
    public void testDoubleConstructor() throws Exception {
        for (CommonTestCases.DoubleToFractionTestCase testCase : CommonTestCases.doubleConstructorTestCases()) {
            assertFraction(
                    testCase.expectedNumerator,
                    testCase.expectedDenominator,
                    BigFraction.from(testCase.operand, 1.0e-5, 100)
            );
        }
    }

    // MATH-181
    @Test
    public void testDigitLimitConstructor() throws Exception {
        assertFraction(2, 5, BigFraction.from(0.4, 9));
        assertFraction(2, 5, BigFraction.from(0.4, 99));
        assertFraction(2, 5, BigFraction.from(0.4, 999));

        assertFraction(3, 5, BigFraction.from(0.6152, 9));
        assertFraction(8, 13, BigFraction.from(0.6152, 99));
        assertFraction(510, 829, BigFraction.from(0.6152, 999));
        assertFraction(769, 1250, BigFraction.from(0.6152, 9999));

        // MATH-996
        assertFraction(1, 2, BigFraction.from(0.5000000001, 10));
    }

    // MATH-1029
    @Test
    public void testPositiveValueOverflow() {
        Assertions.assertThrows(ArithmeticException.class,
            () -> assertFraction((long) 1e10, 1, BigFraction.from(1e10, 1000))
        );
    }

    // MATH-1029
    @Test
    public void testNegativeValueOverflow() {
        Assertions.assertThrows(ArithmeticException.class,
            () -> assertFraction((long) -1e10, 1, BigFraction.from(-1e10, 1000))
        );
    }

    @Test
    public void testEpsilonLimitConstructor() throws Exception {
        assertFraction(2, 5, BigFraction.from(0.4, 1.0e-5, 100));

        assertFraction(3, 5, BigFraction.from(0.6152, 0.02, 100));
        assertFraction(8, 13, BigFraction.from(0.6152, 1.0e-3, 100));
        assertFraction(251, 408, BigFraction.from(0.6152, 1.0e-4, 100));
        assertFraction(251, 408, BigFraction.from(0.6152, 1.0e-5, 100));
        assertFraction(510, 829, BigFraction.from(0.6152, 1.0e-6, 100));
        assertFraction(769, 1250, BigFraction.from(0.6152, 1.0e-7, 100));
    }

    @Test
    public void testCompareTo() {
        final BigFraction a = BigFraction.of(1, 2);
        final BigFraction b = BigFraction.of(1, 3);
        final BigFraction c = BigFraction.of(1, 2);
        final BigFraction d = BigFraction.of(-1, 2);
        final BigFraction e = BigFraction.of(1, -2);

        Assertions.assertEquals(0, a.compareTo(a));
        Assertions.assertEquals(0, a.compareTo(c));
        Assertions.assertEquals(1, a.compareTo(b));
        Assertions.assertEquals(-1, b.compareTo(a));
        Assertions.assertEquals(-1, d.compareTo(a));
        Assertions.assertEquals(1, a.compareTo(d));
        Assertions.assertEquals(-1, e.compareTo(a));
        Assertions.assertEquals(1, a.compareTo(e));
        Assertions.assertEquals(0, d.compareTo(e));

        Assertions.assertEquals(0, BigFraction.of(0, 3).compareTo(BigFraction.of(0, -2)));

        // these two values are different approximations of PI
        // the first  one is approximately PI - 3.07e-18
        // the second one is approximately PI + 1.936e-17
        final BigFraction pi1 = BigFraction.of(1068966896, 340262731);
        final BigFraction pi2 = BigFraction.of(411557987, 131002976);
        Assertions.assertEquals(-1, pi1.compareTo(pi2));
        Assertions.assertEquals(1, pi2.compareTo(pi1));
        Assertions.assertEquals(0.0, pi1.doubleValue() - pi2.doubleValue(), 1.0e-20);
    }

    @Test
    public void testDoubleValue() {
        Assertions.assertEquals(0d, BigFraction.ZERO.doubleValue(), 0d);

        assertDoubleValue(0.5, 1, 2);
        assertDoubleValue(-0.5, -1, 2);
        assertDoubleValue(-0.5, 1, -2);
        assertDoubleValue(0.5, -1, -2);
        assertDoubleValue(1.0 / 3.0, 1, 3);

        //NUMBERS-120
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
    public void testDoubleValueForSubnormalNumbers() {
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
    public void testDoubleValueForInfinities() {
        //the smallest integer that rounds up to Double.POSITIVE_INFINITY
        BigInteger minInf = BigInteger.ONE
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
    public void testDoubleValueForLargeNumeratorAndDenominator() {
        final BigInteger pow400 = BigInteger.TEN.pow(400);
        final BigInteger pow401 = BigInteger.TEN.pow(401);
        final BigInteger two = new BigInteger("2");
        final BigFraction large = BigFraction.of(pow401.add(BigInteger.ONE),
                                                  pow400.multiply(two));

        Assertions.assertEquals(5, large.doubleValue(), 1e-15);
    }

    // MATH-744
    @Test
    public void testFloatValueForLargeNumeratorAndDenominator() {
        final BigInteger pow400 = BigInteger.TEN.pow(400);
        final BigInteger pow401 = BigInteger.TEN.pow(401);
        final BigInteger two = new BigInteger("2");
        final BigFraction large = BigFraction.of(pow401.add(BigInteger.ONE),
                                                  pow400.multiply(two));

        Assertions.assertEquals(5, large.floatValue(), 1e-15);
    }

    @Test
    public void testDoubleValueForLargeNumeratorAndSmallDenominator() {
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
    public void testFloatValueForLargeNumeratorAndSmallDenominator() {
        final BigInteger pow30 = BigInteger.TEN.pow(30);
        final BigInteger pow40 = BigInteger.TEN.pow(40);
        final BigFraction large = BigFraction.of(pow40.add(BigInteger.ONE),
                pow30);

        Assertions.assertEquals(1e10f, large.floatValue(), 1e-15);
    }

    @Test
    public void testFloatValue() {
        Assertions.assertEquals(0.5f, BigFraction.of(1, 2).floatValue(), 0.0f);
        Assertions.assertEquals(0.5f, BigFraction.of(-1, -2).floatValue(), 0.0f);
        Assertions.assertEquals(-0.5f, BigFraction.of(-1, 2).floatValue(), 0.0f);
        Assertions.assertEquals(-0.5f, BigFraction.of(1, -2).floatValue(), 0.0f);

        final float e = 1f / 3f;
        Assertions.assertEquals(e, BigFraction.of(1, 3).floatValue(), 0.0f);
        Assertions.assertEquals(e, BigFraction.of(-1, -3).floatValue(), 0.0f);
        Assertions.assertEquals(-e, BigFraction.of(-1, 3).floatValue(), 0.0f);
        Assertions.assertEquals(-e, BigFraction.of(1, -3).floatValue(), 0.0f);
    }

    @Test
    public void testIntValue() {
        Assertions.assertEquals(0, BigFraction.of(1, 2).intValue());
        Assertions.assertEquals(0, BigFraction.of(-1, -2).intValue());
        Assertions.assertEquals(0, BigFraction.of(-1, 2).intValue());
        Assertions.assertEquals(0, BigFraction.of(1, -2).intValue());

        Assertions.assertEquals(1, BigFraction.of(3, 2).intValue());
        Assertions.assertEquals(1, BigFraction.of(-3, -2).intValue());
        Assertions.assertEquals(-1, BigFraction.of(-3, 2).intValue());
        Assertions.assertEquals(-1, BigFraction.of(3, -2).intValue());
    }

    @Test
    public void testLongValue() {
        Assertions.assertEquals(0L, BigFraction.of(1, 2).longValue());
        Assertions.assertEquals(0L, BigFraction.of(-1, -2).longValue());
        Assertions.assertEquals(0L, BigFraction.of(-1, 2).longValue());
        Assertions.assertEquals(0L, BigFraction.of(1, -2).longValue());

        Assertions.assertEquals(1L, BigFraction.of(3, 2).longValue());
        Assertions.assertEquals(1L, BigFraction.of(-3, -2).longValue());
        Assertions.assertEquals(-1L, BigFraction.of(-3, 2).longValue());
        Assertions.assertEquals(-1L, BigFraction.of(3, -2).longValue());
    }

    @Test
    public void testConstructorDouble() {
        assertFraction(1, 2, BigFraction.from(0.5));
        assertFraction(6004799503160661L, 18014398509481984L, BigFraction.from(1.0 / 3.0));
        assertFraction(6124895493223875L, 36028797018963968L, BigFraction.from(17.0 / 100.0));
        assertFraction(1784551352345559L, 562949953421312L, BigFraction.from(317.0 / 100.0));
        assertFraction(-1, 2, BigFraction.from(-0.5));
        assertFraction(-6004799503160661L, 18014398509481984L, BigFraction.from(-1.0 / 3.0));
        assertFraction(-6124895493223875L, 36028797018963968L, BigFraction.from(17.0 / -100.0));
        assertFraction(-1784551352345559L, 562949953421312L, BigFraction.from(-317.0 / 100.0));
        for (double v : new double[] {Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}) {
            try {
                BigFraction.from(v);
                Assertions.fail("Expecting IllegalArgumentException");
            } catch (IllegalArgumentException iae) {
                // expected
            }
        }
        Assertions.assertEquals(1L, BigFraction.from(Double.MAX_VALUE).getDenominatorAsLong());
        Assertions.assertEquals(1L, BigFraction.from(Double.longBitsToDouble(0x0010000000000000L)).getNumeratorAsLong());
        assertFraction(BigInteger.ONE, BigInteger.ONE.shiftLeft(1074), BigFraction.from(Double.MIN_VALUE));
    }

    @Test
    public void testAbs() {
        for (CommonTestCases.UnaryOperatorTestCase testCase : CommonTestCases.absTestCases()) {
            BigFraction f = BigFraction.of(testCase.operandNumerator, testCase.operandDenominator);
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f.abs());
        }
    }

    @Test
    public void testReciprocal() {
        for (CommonTestCases.UnaryOperatorTestCase testCase : CommonTestCases.reciprocalTestCases()) {
            BigFraction f = BigFraction.of(testCase.operandNumerator, testCase.operandDenominator);
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f.reciprocal());
        }

        BigFraction f = BigFraction.of(0, 3);
        try {
            f = f.reciprocal();
            Assertions.fail("expecting ArithmeticException");
        } catch (ArithmeticException ignored) {
        }
    }

    @Test
    public void testNegate() {
        for (CommonTestCases.UnaryOperatorTestCase testCase : CommonTestCases.negateTestCases()) {
            BigFraction f = BigFraction.of(testCase.operandNumerator, testCase.operandDenominator);
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f.negate());
        }
    }

    @Test
    public void testAdd() {
        for (CommonTestCases.BinaryOperatorTestCase testCase : CommonTestCases.addFractionTestCases()) {
            BigFraction f1 = BigFraction.of(testCase.firstOperandNumerator, testCase.firstOperandDenominator);
            BigFraction f2 = BigFraction.of(testCase.secondOperandNumerator, testCase.secondOperandDenominator);
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f1.add(f2));
        }

        final BigFraction f0 = BigFraction.of(-17 - 2 * 13 * 2, 13 * 13 * 17 * 2 * 2);
        Assertions.assertThrows(NullPointerException.class,
            () -> f0.add((BigFraction) null)
        );

        BigFraction f1 = BigFraction.of(Integer.MAX_VALUE - 1, 1);
        BigFraction f = f1.add(BigInteger.ONE);
        Assertions.assertEquals(Integer.MAX_VALUE, f.getNumeratorAsInt());
        Assertions.assertEquals(1, f.getDenominatorAsInt());

        f = f.add(BigInteger.ZERO);
        Assertions.assertEquals(Integer.MAX_VALUE, f.getNumeratorAsInt());
        Assertions.assertEquals(1, f.getDenominatorAsInt());

        f1 = BigFraction.of(Integer.MAX_VALUE - 1, 1);
        f = f1.add(1);
        Assertions.assertEquals(Integer.MAX_VALUE, f.getNumeratorAsInt());
        Assertions.assertEquals(1, f.getDenominatorAsInt());

        f = f.add(0);
        Assertions.assertEquals(Integer.MAX_VALUE, f.getNumeratorAsInt());
        Assertions.assertEquals(1, f.getDenominatorAsInt());

        f1 = BigFraction.of(Integer.MAX_VALUE - 1, 1);
        f = f1.add(1L);
        Assertions.assertEquals(Integer.MAX_VALUE, f.getNumeratorAsInt());
        Assertions.assertEquals(1, f.getDenominatorAsInt());

        f = f.add(0L);
        Assertions.assertEquals(Integer.MAX_VALUE, f.getNumeratorAsInt());
        Assertions.assertEquals(1, f.getDenominatorAsInt());

    }

    @Test
    public void testDivide() {
        for (CommonTestCases.BinaryOperatorTestCase testCase : CommonTestCases.divideByFractionTestCases()) {
            BigFraction f1 = BigFraction.of(testCase.firstOperandNumerator, testCase.firstOperandDenominator);
            BigFraction f2 = BigFraction.of(testCase.secondOperandNumerator, testCase.secondOperandDenominator);
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f1.divide(f2));
        }

        Assertions.assertThrows(FractionException.class, () -> BigFraction.of(1, 2).divide(BigInteger.ZERO));

        BigFraction f1;
        BigFraction f2;

        f1 = BigFraction.of(0, 5);
        f2 = BigFraction.of(2, 7);
        BigFraction f = f1.divide(f2);
        Assertions.assertSame(BigFraction.ZERO, f);

        final BigFraction f3 = BigFraction.of(Integer.MIN_VALUE, 1);
        Assertions.assertThrows(NullPointerException.class,
            () -> f3.divide((BigFraction) null)
        );

        f1 = BigFraction.of(Integer.MIN_VALUE, Integer.MAX_VALUE);
        f = f1.divide(BigInteger.valueOf(Integer.MIN_VALUE));
        Assertions.assertEquals(-Integer.MAX_VALUE, f.getDenominatorAsInt());
        Assertions.assertEquals(-1, f.getNumeratorAsInt());

        f1 = BigFraction.of(Integer.MIN_VALUE, Integer.MAX_VALUE);
        f = f1.divide(Integer.MIN_VALUE);
        Assertions.assertEquals(-Integer.MAX_VALUE, f.getDenominatorAsInt());
        Assertions.assertEquals(-1, f.getNumeratorAsInt());

        f1 = BigFraction.of(Integer.MIN_VALUE, Integer.MAX_VALUE);
        f = f1.divide((long) Integer.MIN_VALUE);
        Assertions.assertEquals(-Integer.MAX_VALUE, f.getDenominatorAsInt());
        Assertions.assertEquals(-1, f.getNumeratorAsInt());

        Assertions.assertEquals(BigFraction.ZERO, BigFraction.of(0, 3).divide(BigInteger.valueOf(11)));
    }

    @Test
    public void testMultiply() {
        for (CommonTestCases.BinaryOperatorTestCase testCase : CommonTestCases.multiplyByFractionTestCases()) {
            BigFraction f1 = BigFraction.of(testCase.firstOperandNumerator, testCase.firstOperandDenominator);
            BigFraction f2 = BigFraction.of(testCase.secondOperandNumerator, testCase.secondOperandDenominator);
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f1.multiply(f2));
        }

        BigFraction f2 = BigFraction.of(Integer.MIN_VALUE, Integer.MAX_VALUE);
        BigFraction f = f2.multiply(Integer.MAX_VALUE);
        Assertions.assertEquals(Integer.MIN_VALUE, f.getNumeratorAsInt());
        Assertions.assertEquals(1, f.getDenominatorAsInt());

        f = f2.multiply((long) Integer.MAX_VALUE);
        Assertions.assertEquals(Integer.MIN_VALUE, f.getNumeratorAsInt());
        Assertions.assertEquals(1, f.getDenominatorAsInt());

        Assertions.assertThrows(NullPointerException.class, () -> BigFraction.ONE.multiply((BigFraction) null));

        Assertions.assertEquals(BigFraction.ZERO, BigFraction.ZERO.multiply(BigInteger.ONE));
        Assertions.assertEquals(BigFraction.ZERO, BigFraction.ONE.multiply(BigInteger.ZERO));
        Assertions.assertEquals(BigFraction.ZERO, BigFraction.ZERO.multiply(1));
        Assertions.assertEquals(BigFraction.ZERO, BigFraction.ONE.multiply(0));
        Assertions.assertEquals(BigFraction.ZERO, BigFraction.ZERO.multiply(1L));
        Assertions.assertEquals(BigFraction.ZERO, BigFraction.ONE.multiply(0L));
    }

    @Test
    public void testSubtract() {
        for (CommonTestCases.BinaryOperatorTestCase testCase : CommonTestCases.subtractFractionTestCases()) {
            BigFraction f1 = BigFraction.of(testCase.firstOperandNumerator, testCase.firstOperandDenominator);
            BigFraction f2 = BigFraction.of(testCase.secondOperandNumerator, testCase.secondOperandDenominator);
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f1.subtract(f2));
        }

        BigFraction f = BigFraction.of(1, 1);
        Assertions.assertThrows(NullPointerException.class, () -> f.subtract((BigFraction) null));

        Assertions.assertEquals(BigFraction.ONE, BigFraction.ONE.subtract(BigInteger.ZERO));
        Assertions.assertEquals(BigFraction.of(-1), BigFraction.ZERO.subtract(BigInteger.ONE));
        Assertions.assertEquals(BigFraction.of(-2), BigFraction.ZERO.subtract(2));
        Assertions.assertEquals(BigFraction.of(-123), BigFraction.ZERO.subtract(123L));
        Assertions.assertEquals(BigFraction.of(-7, 3), BigFraction.of(2, 3).subtract(BigInteger.valueOf(3)));
    }

    @Test
    public void testBigDecimalValue() {
        Assertions.assertEquals(new BigDecimal(0.5), BigFraction.of(1, 2).bigDecimalValue());
        Assertions.assertEquals(new BigDecimal("0.0003"), BigFraction.of(3, 10000).bigDecimalValue());
        Assertions.assertEquals(new BigDecimal("0"), BigFraction.of(1, 3).bigDecimalValue(RoundingMode.DOWN));
        Assertions.assertEquals(new BigDecimal("0.333"), BigFraction.of(1, 3).bigDecimalValue(3, RoundingMode.DOWN));
    }

    @Test
    public void testEqualsAndHashCode() {
        BigFraction zero = BigFraction.of(0, 1);
        BigFraction nullFraction = null;
        Assertions.assertEquals(zero, zero);
        Assertions.assertFalse(zero.equals(nullFraction));
        Assertions.assertFalse(zero.equals(Double.valueOf(0)));
        BigFraction zero2 = BigFraction.of(0, 2);
        Assertions.assertEquals(zero, zero2);
        Assertions.assertEquals(zero.hashCode(), zero2.hashCode());
        BigFraction one = BigFraction.of(1, 1);
        Assertions.assertNotEquals(zero, one);
        Assertions.assertNotEquals(one, zero);
        Assertions.assertEquals(BigFraction.ONE, one);
        BigFraction one2 = BigFraction.of(-1, -1);
        Assertions.assertEquals(one2, one);
        Assertions.assertEquals(one, one2);

        BigFraction minusOne = BigFraction.of(-1, 1);
        BigFraction minusOne2 = BigFraction.of(1, -1);
        Assertions.assertEquals(minusOne2, minusOne);
        Assertions.assertEquals(minusOne, minusOne2);
    }

    @Test
    public void testPow() {
        Assertions.assertEquals(BigFraction.of(8192, 1594323), BigFraction.of(2, 3).pow(13));
        Assertions.assertEquals(BigFraction.of(8192, 1594323), BigFraction.of(2, 3).pow(13L));
        Assertions.assertEquals(BigFraction.of(8192, 1594323), BigFraction.of(2, 3).pow(BigInteger.valueOf(13L)));
        Assertions.assertEquals(BigFraction.ONE, BigFraction.of(2, 3).pow(0));
        Assertions.assertEquals(BigFraction.ONE, BigFraction.of(2, 3).pow(0L));
        Assertions.assertEquals(BigFraction.ONE, BigFraction.of(2, 3).pow(BigInteger.valueOf(0L)));
        Assertions.assertEquals(BigFraction.of(1594323, 8192), BigFraction.of(2, 3).pow(-13));
        Assertions.assertEquals(BigFraction.of(1594323, 8192), BigFraction.of(2, 3).pow(-13L));
        Assertions.assertEquals(BigFraction.of(1594323, 8192), BigFraction.of(2, 3).pow(BigInteger.valueOf(-13L)));
        Assertions.assertEquals(BigFraction.ZERO, BigFraction.of(0, 5).pow(123));
        Assertions.assertEquals(BigFraction.ZERO, BigFraction.of(0, 5).pow(123L));
        Assertions.assertEquals(BigFraction.ZERO, BigFraction.of(0, 5).pow(new BigInteger("112233445566778899")));
        Assertions.assertEquals(Math.sqrt(2d / 3), BigFraction.of(2, 3).pow(0.5), 1e-15);
    }

    @Test
    public void testMath340() {
        BigFraction fractionA = BigFraction.from(0.00131);
        BigFraction fractionB = BigFraction.from(.37).reciprocal();
        BigFraction errorResult = fractionA.multiply(fractionB);
        BigFraction correctResult = BigFraction.of(fractionA.getNumerator().multiply(fractionB.getNumerator()),
                                                    fractionA.getDenominator().multiply(fractionB.getDenominator()));
        Assertions.assertEquals(correctResult, errorResult);
    }

    @Test
    public void testSerial() {
        BigFraction[] fractions = {
            BigFraction.of(3, 4), BigFraction.ONE, BigFraction.ZERO,
            BigFraction.of(17), BigFraction.from(Math.PI, 1000),
            BigFraction.of(-5, 2)
        };
        for (BigFraction fraction : fractions) {
            Assertions.assertEquals(fraction, TestUtils.serializeAndRecover(fraction));
        }
    }

    @Test
    public void testAdditiveNeutral() {
        Assertions.assertEquals(BigFraction.ZERO, BigFraction.ONE.zero());
    }
    @Test
    public void testMultiplicativeNeutral() {
        Assertions.assertEquals(BigFraction.ONE, BigFraction.ZERO.one());
    }

    @Test
    public void testToString() {
        Assertions.assertEquals("0", BigFraction.of(0, 3).toString());
        Assertions.assertEquals("3", BigFraction.of(6, 2).toString());
        Assertions.assertEquals("2 / 3", BigFraction.of(18, 27).toString());
        Assertions.assertEquals("-10 / 11", BigFraction.of(-10, 11).toString());
        Assertions.assertEquals("10 / -11", BigFraction.of(10, -11).toString());
    }

    @Test
    public void testParse() {
        String[] validExpressions = new String[] {
            "3",
            "1 / 2",
            "-1 / 2",
            "1 / -2",
            "-1 / -2",
            "2147,483,647 / 2,147,483,648", //over largest int value
            "9,223,372,036,854,775,807 / 9,223,372,036,854,775,808" //over largest long value
        };
        BigFraction[] fractions = {
                BigFraction.of(3),
                BigFraction.of(1, 2),
                BigFraction.of(-1, 2),
                BigFraction.of(1, -2),
                BigFraction.of(-1, -2),
                BigFraction.of(2147483647, 2147483648L),
                BigFraction.of(new BigInteger("9223372036854775807"),
                               new BigInteger("9223372036854775808"))
        };
        int inc = 0;
        for (BigFraction fraction: fractions) {
            Assertions.assertEquals(fraction,
                                    BigFraction.parse(validExpressions[inc]));
            inc++;
        }

        Assertions.assertThrows(NumberFormatException.class, () -> BigFraction.parse("1 // 2"));
        Assertions.assertThrows(NumberFormatException.class, () -> BigFraction.parse("1 / z"));
        Assertions.assertThrows(NumberFormatException.class, () -> BigFraction.parse("1 / --2"));
        Assertions.assertThrows(NumberFormatException.class, () -> BigFraction.parse("x"));
    }
}
