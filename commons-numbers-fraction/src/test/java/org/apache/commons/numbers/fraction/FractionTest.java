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

import java.util.Arrays;
import org.apache.commons.numbers.core.TestUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 */
public class FractionTest {

    private void assertFraction(int expectedNumerator, int expectedDenominator, Fraction actual) {
        Assertions.assertEquals(expectedNumerator, actual.getNumerator());
        Assertions.assertEquals(expectedDenominator, actual.getDenominator());
        Assertions.assertEquals(
            Integer.signum(expectedNumerator) * Integer.signum(expectedDenominator),
            actual.signum());
    }

    @Test
    public void testConstructor() {
        for (CommonTestCases.UnaryOperatorTestCase testCase : CommonTestCases.numDenConstructorTestCases()) {
            assertFraction(
                    testCase.expectedNumerator,
                    testCase.expectedDenominator,
                    Fraction.of(testCase.operandNumerator, testCase.operandDenominator)
            );
        }

        // Special cases.
        assertFraction(Integer.MIN_VALUE, -1, Fraction.of(Integer.MIN_VALUE, -1));
        assertFraction(1, Integer.MIN_VALUE, Fraction.of(1, Integer.MIN_VALUE));
        assertFraction(-1, Integer.MIN_VALUE, Fraction.of(-1, Integer.MIN_VALUE));
        assertFraction(1, 1, Fraction.of(Integer.MIN_VALUE, Integer.MIN_VALUE));
    }

    @Test
    public void testGoldenRatio() {
        // the golden ratio is notoriously a difficult number for continuous fraction
        Assertions.assertThrows(ArithmeticException.class,
            () -> Fraction.from((1 + Math.sqrt(5)) / 2, 1.0e-12, 25)
        );
    }

    // MATH-179
    @Test
    public void testDoubleConstructor() throws Exception  {
        for (CommonTestCases.DoubleToFractionTestCase testCase : CommonTestCases.doubleConstructorTestCases()) {
            assertFraction(
                    testCase.expectedNumerator,
                    testCase.expectedDenominator,
                    Fraction.from(testCase.operand)
            );
        }
    }

    @Test
    public void testDoubleConstructorThrowsWithNonFinite() {
        final double eps = 1e-5;
        final int maxIterations = Integer.MAX_VALUE;
        final int maxDenominator = Integer.MAX_VALUE;
        for (final double value : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> Fraction.from(value));
            Assertions.assertThrows(IllegalArgumentException.class, () -> Fraction.from(value, eps, maxIterations));
            Assertions.assertThrows(IllegalArgumentException.class, () -> Fraction.from(value, maxDenominator));
        }
    }

    // MATH-181
    @Test
    public void testDigitLimitConstructor() throws Exception  {
        assertFraction(2, 5, Fraction.from(0.4,   9));
        assertFraction(2, 5, Fraction.from(0.4,  99));
        assertFraction(2, 5, Fraction.from(0.4, 999));

        assertFraction(3, 5,      Fraction.from(0.6152,    9));
        assertFraction(8, 13,     Fraction.from(0.6152,   99));
        assertFraction(510, 829,  Fraction.from(0.6152,  999));
        assertFraction(769, 1250, Fraction.from(0.6152, 9999));

        // MATH-996
        assertFraction(1, 2, Fraction.from(0.5000000001, 10));
    }

    @Test
    public void testIntegerOverflow() {
        checkIntegerOverflow(0.75000000001455192);
        checkIntegerOverflow(1.0e10);
        checkIntegerOverflow(-1.0e10);
        checkIntegerOverflow(-43979.60679604749);
    }

    private void checkIntegerOverflow(final double a) {
        Assertions.assertThrows(ArithmeticException.class,
            () -> Fraction.from(a, 1.0e-12, 1000)
        );
    }

    @Test
    public void testEpsilonLimitConstructor() throws Exception  {
        assertFraction(2, 5, Fraction.from(0.4, 1.0e-5, 100));

        assertFraction(3, 5,      Fraction.from(0.6152, 0.02, 100));
        assertFraction(8, 13,     Fraction.from(0.6152, 1.0e-3, 100));
        assertFraction(251, 408,  Fraction.from(0.6152, 1.0e-4, 100));
        assertFraction(251, 408,  Fraction.from(0.6152, 1.0e-5, 100));
        assertFraction(510, 829,  Fraction.from(0.6152, 1.0e-6, 100));
        assertFraction(769, 1250, Fraction.from(0.6152, 1.0e-7, 100));
    }

    @Test
    public void testCompareTo() {
        Fraction first = Fraction.of(1, 2);
        Fraction second = Fraction.of(1, 3);
        Fraction third = Fraction.of(1, 2);

        Assertions.assertEquals(0, first.compareTo(first));
        Assertions.assertEquals(0, first.compareTo(third));
        Assertions.assertEquals(1, first.compareTo(second));
        Assertions.assertEquals(-1, second.compareTo(first));

        // these two values are different approximations of PI
        // the first  one is approximately PI - 3.07e-18
        // the second one is approximately PI + 1.936e-17
        Fraction pi1 = Fraction.of(1068966896, 340262731);
        Fraction pi2 = Fraction.of(411557987, 131002976);
        Assertions.assertEquals(-1, pi1.compareTo(pi2));
        Assertions.assertEquals(1, pi2.compareTo(pi1));
        Assertions.assertEquals(0.0, pi1.doubleValue() - pi2.doubleValue(), 1.0e-20);
    }

    @Test
    public void testDoubleValue() {
        Fraction first = Fraction.of(1, 2);
        Fraction second = Fraction.of(1, 3);

        Assertions.assertEquals(0.5, first.doubleValue(), 0.0);
        Assertions.assertEquals(1.0 / 3.0, second.doubleValue(), 0.0);
    }

    @Test
    public void testFloatValue() {
        Fraction first = Fraction.of(1, 2);
        Fraction second = Fraction.of(1, 3);

        Assertions.assertEquals(0.5f, first.floatValue(), 0.0f);
        Assertions.assertEquals((float)(1.0 / 3.0), second.floatValue(), 0.0f);
    }

    @Test
    public void testIntValue() {
        Fraction first = Fraction.of(1, 2);
        Fraction second = Fraction.of(3, 2);

        Assertions.assertEquals(0, first.intValue());
        Assertions.assertEquals(1, second.intValue());
    }

    @Test
    public void testLongValue() {
        Fraction first = Fraction.of(1, 2);
        Fraction second = Fraction.of(3, 2);

        Assertions.assertEquals(0L, first.longValue());
        Assertions.assertEquals(1L, second.longValue());
    }

    @Test
    public void testConstructorDouble() {
        assertFraction(1, 2, Fraction.from(0.5));
        assertFraction(1, 3, Fraction.from(1.0 / 3.0));
        assertFraction(17, 100, Fraction.from(17.0 / 100.0));
        assertFraction(317, 100, Fraction.from(317.0 / 100.0));
        assertFraction(-1, 2, Fraction.from(-0.5));
        assertFraction(-1, 3, Fraction.from(-1.0 / 3.0));
        assertFraction(-17, 100, Fraction.from(17.0 / -100.0));
        assertFraction(-317, 100, Fraction.from(-317.0 / 100.0));
    }

    @Test
    public void testAbs() {
        for (CommonTestCases.UnaryOperatorTestCase testCase : CommonTestCases.absTestCases()) {
            Fraction f = Fraction.of(testCase.operandNumerator, testCase.operandDenominator);
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f.abs());
        }
    }

    @Test
    public void testMath1261() {
        final Fraction a = Fraction.of(Integer.MAX_VALUE, 2);
        final Fraction b = a.multiply(2);
        Assertions.assertTrue(b.equals(Fraction.of(Integer.MAX_VALUE)));

        final Fraction c = Fraction.of(2, Integer.MAX_VALUE);
        final Fraction d = c.divide(2);
        Assertions.assertTrue(d.equals(Fraction.of(1, Integer.MAX_VALUE)));
    }

    @Test
    public void testReciprocal() {
        for (CommonTestCases.UnaryOperatorTestCase testCase : CommonTestCases.reciprocalTestCases()) {
            Fraction f = Fraction.of(testCase.operandNumerator, testCase.operandDenominator);
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f.reciprocal());
        }

        final Fraction f = Fraction.of(0, 3);
        Assertions.assertThrows(ArithmeticException.class,
                f::reciprocal
        );
    }

    @Test
    public void testNegate() {
        for (CommonTestCases.UnaryOperatorTestCase testCase : CommonTestCases.negateTestCases()) {
            Fraction f = Fraction.of(testCase.operandNumerator, testCase.operandDenominator);
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f.negate());
        }
    }

    /**
     * Test special cases of negation that differ from BigFraction.
     */
    @Test
    public void testNegateMinValue() {
        final Fraction one = Fraction.of(Integer.MIN_VALUE, Integer.MIN_VALUE);
        assertFraction(-1, 1, one.negate());
        // Special case where the negation of the numerator is not possible.
        final Fraction minValue = Fraction.of(Integer.MIN_VALUE, 1);
        assertFraction(Integer.MIN_VALUE, -1, minValue.negate());
    }

    @Test
    public void testAdd() {
        for (CommonTestCases.BinaryOperatorTestCase testCase : CommonTestCases.addFractionTestCases()) {
            Fraction f1 = Fraction.of(testCase.firstOperandNumerator, testCase.firstOperandDenominator);
            Fraction f2 = Fraction.of(testCase.secondOperandNumerator, testCase.secondOperandDenominator);
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f1.add(f2));
        }

        Fraction f1 = Fraction.of(Integer.MAX_VALUE - 1, 1);
        Fraction f2 = f1.add(1);
        assertFraction(Integer.MAX_VALUE, 1, f2);

        final Fraction f3 = Fraction.of(-17 - 2 * 13 * 2, 13 * 13 * 17 * 2 * 2);
        Assertions.assertThrows(NullPointerException.class,
            () -> f3.add(null)
        );

        final Fraction f4 = Fraction.of(Integer.MAX_VALUE, 1);
        Assertions.assertThrows(ArithmeticException.class,
            () -> {
                Fraction f = f4.add(Fraction.ONE); // should overflow
                Assertions.fail("expecting ArithmeticException but got: " + f.toString());
            }
        );

        // denominator should not be a multiple of 2 or 3 to trigger overflow
        final Fraction f5 = Fraction.of(Integer.MIN_VALUE, 5);
        final Fraction f6 = Fraction.of(-1, 5);
        Assertions.assertThrows(ArithmeticException.class,
            () -> {
                Fraction f = f5.add(f6); // should overflow
                Assertions.fail("expecting ArithmeticException but got: " + f.toString());
            }
        );

        final Fraction f7 = Fraction.of(-Integer.MAX_VALUE, 1);
        Assertions.assertThrows(ArithmeticException.class,
            () -> f7.add(f7)
        );

        final Fraction f8 = Fraction.of(3, 327680);
        final Fraction f9 = Fraction.of(2, 59049);
        Assertions.assertThrows(ArithmeticException.class,
            () -> {
                Fraction f = f8.add(f9); // should overflow
                Assertions.fail("expecting ArithmeticException but got: " + f.toString());
            }
        );
    }

    @Test
    public void testDivide() {
        for (CommonTestCases.BinaryOperatorTestCase testCase : CommonTestCases.divideByFractionTestCases()) {
            Fraction f1 = Fraction.of(testCase.firstOperandNumerator, testCase.firstOperandDenominator);
            Fraction f2 = Fraction.of(testCase.secondOperandNumerator, testCase.secondOperandDenominator);
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f1.divide(f2));
        }

        final Fraction f1 = Fraction.of(3, 5);
        final Fraction f2 = Fraction.ZERO;
        Assertions.assertThrows(FractionException.class,
            () -> f1.divide(f2)
        );

        Fraction f3 = Fraction.of(0, 5);
        Fraction f4 = Fraction.of(2, 7);
        Fraction f = f3.divide(f4);
        Assertions.assertSame(Fraction.ZERO, f);

        final Fraction f5 = Fraction.of(Integer.MIN_VALUE, 1);
        Assertions.assertThrows(NullPointerException.class,
            () -> f5.divide(null)
        );

        final Fraction f6 = Fraction.of(1, Integer.MAX_VALUE);
        Assertions.assertThrows(ArithmeticException.class,
            () -> f6.divide(f6.reciprocal())  // should overflow
        );

        final Fraction f7 = Fraction.of(1, -Integer.MAX_VALUE);
        Assertions.assertThrows(ArithmeticException.class,
            () -> f7.divide(f7.reciprocal())  // should overflow
        );

        Fraction f8 = Fraction.of(6, 35);
        Fraction f9 = f8.divide(15);
        assertFraction(2, 175, f9);
    }

    @Test
    public void testMultiply() {
        for (CommonTestCases.BinaryOperatorTestCase testCase : CommonTestCases.multiplyByFractionTestCases()) {
            Fraction f1 = Fraction.of(testCase.firstOperandNumerator, testCase.firstOperandDenominator);
            Fraction f2 = Fraction.of(testCase.secondOperandNumerator, testCase.secondOperandDenominator);
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f1.multiply(f2));
        }

        final Fraction f0 = Fraction.of(Integer.MIN_VALUE, 1);
        Assertions.assertThrows(NullPointerException.class,
            () -> f0.multiply(null)
        );

        Fraction f1 = Fraction.of(6, 35);
        Fraction f = f1.multiply(15);
        assertFraction(18, 7, f);
    }

    @Test
    public void testPow() {
        Fraction a = Fraction.of(3, 7);
        assertFraction(1, 1, a.pow(0));
        assertFraction(3, 7, a.pow(1));
        assertFraction(7, 3, a.pow(-1));
        assertFraction(9, 49, a.pow(2));
        assertFraction(49, 9, a.pow(-2));

        Fraction b = Fraction.of(3, -7);
        assertFraction(1, 1, b.pow(0));
        assertFraction(3, -7, b.pow(1));
        assertFraction(-7, 3, b.pow(-1));
        assertFraction(9, 49, b.pow(2));
        assertFraction(49, 9, b.pow(-2));

        Fraction c = Fraction.of(0, -11);
        assertFraction(0, -1, c.pow(Integer.MAX_VALUE));
    }

    @Test
    public void testSubtract() {
        for (CommonTestCases.BinaryOperatorTestCase testCase : CommonTestCases.subtractFractionTestCases()) {
            Fraction f1 = Fraction.of(testCase.firstOperandNumerator, testCase.firstOperandDenominator);
            Fraction f2 = Fraction.of(testCase.secondOperandNumerator, testCase.secondOperandDenominator);
            assertFraction(testCase.expectedNumerator, testCase.expectedDenominator, f1.subtract(f2));
        }

        final Fraction f0 = Fraction.of(1, 1);
        Assertions.assertThrows(NullPointerException.class,
            () -> f0.subtract(null)
        );

        Fraction f1 = Fraction.of(Integer.MAX_VALUE, 1);
        Fraction f2 = f1.subtract(1);
        assertFraction(Integer.MAX_VALUE - 1, 1, f2);

        final Fraction f3 = Fraction.of(1, Integer.MAX_VALUE);
        final Fraction f4 = Fraction.of(1, Integer.MAX_VALUE - 1);
        Assertions.assertThrows(ArithmeticException.class,
            () -> f3.subtract(f4)  //should overflow
        );

        // denominator should not be a multiple of 2 or 3 to trigger overflow
        final Fraction f5 = Fraction.of(Integer.MIN_VALUE, 5);
        final Fraction f6 = Fraction.of(1, 5);
        Assertions.assertThrows(ArithmeticException.class,
            () -> {
                Fraction f = f5.subtract(f6); // should overflow
                Assertions.fail("expecting ArithmeticException but got: " + f.toString());
            }
        );

        final Fraction f7 = Fraction.of(Integer.MIN_VALUE, 1);
        Assertions.assertThrows(ArithmeticException.class,
            () -> f7.subtract(Fraction.ONE)
        );

        final Fraction f8 = Fraction.of(Integer.MAX_VALUE, 1);
        Assertions.assertThrows(ArithmeticException.class,
            () -> f8.subtract(Fraction.ONE.negate())
        );

        final Fraction f9 = Fraction.of(3, 327680);
        final Fraction f10 = Fraction.of(2, 59049);
        Assertions.assertThrows(ArithmeticException.class,
            () -> {
                Fraction f = f9.subtract(f10); // should overflow
                Assertions.fail("expecting ArithmeticException but got: " + f.toString());
            }
        );
    }

    @Test
    public void testEqualsAndHashCode() {
        Fraction zero = Fraction.of(0, 1);
        Assertions.assertEquals(zero, zero);
        Assertions.assertNotEquals(zero, null);
        Assertions.assertFalse(zero.equals(new Object()));
        Assertions.assertFalse(zero.equals(Double.valueOf(0)));

        // Equal to same rational number
        Fraction zero2 = Fraction.of(0, 2);
        assertEqualAndHashCodeEqual(zero, zero2);

        // Not equal to different rational number
        Fraction one = Fraction.of(1, 1);
        Assertions.assertNotEquals(zero, one);
        Assertions.assertNotEquals(one, zero);

        // Test using different representations of the same fraction
        // (Denominators are primes)
        for (int[] f : new int[][] {{1, 1}, {2, 3}, {6826, 15373}, {1373, 103813}}) {
            final int num = f[0];
            final int den = f[1];
            Fraction f1 = Fraction.of(-num, den);
            Fraction f2 = Fraction.of(num, -den);
            assertEqualAndHashCodeEqual(f1, f2);
            assertEqualAndHashCodeEqual(f2, f1);
            f1 = Fraction.of(num, den);
            f2 = Fraction.of(-num, -den);
            assertEqualAndHashCodeEqual(f1, f2);
            assertEqualAndHashCodeEqual(f2, f1);
        }

        // Same numerator or denominator as 1/1
        Fraction half = Fraction.of(1, 2);
        Fraction two = Fraction.of(2, 1);
        Assertions.assertNotEquals(one, half);
        Assertions.assertNotEquals(one, two);

        // Check worst case fractions which will have a component using MIN_VALUE.
        // Note: abs(MIN_VALUE) is negative but this should not effect the equals result.
        Fraction almostOne = Fraction.of(Integer.MIN_VALUE, Integer.MAX_VALUE);
        Fraction almostOne2 = Fraction.of(Integer.MIN_VALUE, -Integer.MAX_VALUE);
        Assertions.assertEquals(almostOne, almostOne);
        Assertions.assertNotEquals(almostOne, almostOne2);
        Fraction almostZero = Fraction.of(-1, Integer.MIN_VALUE);
        Fraction almostZero2 = Fraction.of(1, Integer.MIN_VALUE);
        Assertions.assertEquals(almostZero, almostZero);
        Assertions.assertNotEquals(almostZero, almostZero2);
    }

    /**
     * Assert the two fractions are equal. The contract of {@link Object#hashCode()} requires
     * that the hash code must also be equal.
     *
     * <p>This method must not be called with the same instance for both arguments. It is
     * intended to be used to test different objects that are equal have the same hash code.
     *
     * @param f1 Fraction 1.
     * @param f2 Fraction 2.
     */
    private static void assertEqualAndHashCodeEqual(Fraction f1, Fraction f2) {
        Assertions.assertNotSame(f1, f2, "Do not call this assertion with the same object");
        Assertions.assertEquals(f1, f2);
        Assertions.assertEquals(f1.hashCode(), f2.hashCode(), "Equal fractions have different hashCode");
        // Check the hashcode computation.
        // This is not mandated but is a recommendation.
        final int expected = Arrays.hashCode(new int[] {f1.signum(),
                                                        Math.abs(f1.getNumerator()),
                                                        Math.abs(f1.getDenominator())});
        Assertions.assertEquals(expected, f1.hashCode(), "Hashcode not equal to using Arrays.hashCode");
    }

    @Test
    public void testAdditiveNeutral() {
        Assertions.assertEquals(Fraction.ZERO, Fraction.ONE.zero());
    }
    @Test
    public void testMultiplicativeNeutral() {
        Assertions.assertEquals(Fraction.ONE, Fraction.ZERO.one());
    }

    @Test
    public void testToString() {
        Assertions.assertEquals("0", Fraction.of(0, 3).toString());
        Assertions.assertEquals("0", Fraction.of(0, -3).toString());
        Assertions.assertEquals("3", Fraction.of(6, 2).toString());
        Assertions.assertEquals("2 / 3", Fraction.of(18, 27).toString());
        Assertions.assertEquals("-10 / 11", Fraction.of(-10, 11).toString());
        Assertions.assertEquals("10 / -11", Fraction.of(10, -11).toString());
    }

    @Test
    public void testSerial() {
        Fraction[] fractions = {
            Fraction.of(3, 4), Fraction.ONE, Fraction.ZERO,
            Fraction.of(17), Fraction.from(Math.PI, 1000),
            Fraction.of(-5, 2)
        };
        for (Fraction fraction : fractions) {
            Assertions.assertEquals(fraction,
                                    TestUtils.serializeAndRecover(fraction));
        }
    }

    @Test
    public void testParse() {
        String[] validExpressions = new String[] {
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
            "-3"
        };
        Fraction[] fractions = {
            Fraction.of(1, 2),
            Fraction.of(-1, 2),
            Fraction.of(1, -2),
            Fraction.of(-1, -2),
            Fraction.of(1, 2),
            Fraction.of(1, 2),
            Fraction.of(-1, 2),
            Fraction.of(1, -2),
            Fraction.of(15, 16),
            Fraction.of(-2, 3),
            Fraction.of(8, 7),
            Fraction.of(5, 1),
            Fraction.of(-3, 1),
            Fraction.of(3, -1),
        };
        int inc = 0;
        for (Fraction fraction : fractions) {
            Assertions.assertEquals(fraction,
                                    Fraction.parse(validExpressions[inc]));
            inc++;
        }

        Assertions.assertThrows(NumberFormatException.class, () -> Fraction.parse("1 // 2"));
        Assertions.assertThrows(NumberFormatException.class, () -> Fraction.parse("1 / z"));
        Assertions.assertThrows(NumberFormatException.class, () -> Fraction.parse("1 / --2"));
        Assertions.assertThrows(NumberFormatException.class, () -> Fraction.parse("x"));
    }
}
