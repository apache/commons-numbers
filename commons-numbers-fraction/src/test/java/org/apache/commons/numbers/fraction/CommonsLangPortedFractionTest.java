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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

/**
 * Test cases for the {@link Fraction} class.
 *
 * <p>This class is ported from commons lang to demonstrate interoperability of
 * the Fraction class in numbers.</p>
 */
class CommonsLangPortedFractionTest {

    private static final int SKIP = 500;  //53

    //--------------------------------------------------------------------------
    @Test
    void testConstants() {
        assertEquals(0, Fraction.ZERO.getNumerator());
        assertEquals(1, Fraction.ZERO.getDenominator());

        assertEquals(1, Fraction.ONE.getNumerator());
        assertEquals(1, Fraction.ONE.getDenominator());

        /*
         *  All these constants need not be supported.
         *  Users can create whatever constants they require.
         *  The special constants ZERO and ONE are for the additive and multiplicative identity.
         *
         *  assertEquals(1, Fraction.ONE_HALF.getNumerator());
         *  assertEquals(2, Fraction.ONE_HALF.getDenominator());
         *
         *  assertEquals(1, Fraction.ONE_THIRD.getNumerator());
         *  assertEquals(3, Fraction.ONE_THIRD.getDenominator());
         *
         *  assertEquals(2, Fraction.TWO_THIRDS.getNumerator());
         *  assertEquals(3, Fraction.TWO_THIRDS.getDenominator());
         *
         *  assertEquals(1, Fraction.ONE_QUARTER.getNumerator());
         *  assertEquals(4, Fraction.ONE_QUARTER.getDenominator());
         *
         *  assertEquals(1, Fraction.TWO_QUARTERS.getNumerator());
         *  assertEquals(2, Fraction.TWO_QUARTERS.getDenominator());
         *
         *  assertEquals(3, Fraction.THREE_QUARTERS.getNumerator());
         *  assertEquals(4, Fraction.THREE_QUARTERS.getDenominator());
         *
         *  assertEquals(1, Fraction.ONE_FIFTH.getNumerator());
         *  assertEquals(5, Fraction.ONE_FIFTH.getDenominator());
         *
         *  assertEquals(2, Fraction.TWO_FIFTHS.getNumerator());
         *  assertEquals(5, Fraction.TWO_FIFTHS.getDenominator());
         *
         *  assertEquals(3, Fraction.THREE_FIFTHS.getNumerator());
         *  assertEquals(5, Fraction.THREE_FIFTHS.getDenominator());
         *
         *  assertEquals(4, Fraction.FOUR_FIFTHS.getNumerator());
         *  assertEquals(5, Fraction.FOUR_FIFTHS.getDenominator());
         */
    }

    @Test
    void testFactory_int_int() {
        Fraction f = null;

        // zero
        f = Fraction.of(0, 1);
        assertEquals(0, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of(0, 2);
        assertEquals(0, f.getNumerator());
        assertEquals(1, f.getDenominator());

        // normal
        f = Fraction.of(1, 1);
        assertEquals(1, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of(2, 1);
        assertEquals(2, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of(23, 345);
        assertEquals(1, f.getNumerator());
        assertEquals(15, f.getDenominator());

        // improper
        f = Fraction.of(22, 7);
        assertEquals(22, f.getNumerator());
        assertEquals(7, f.getDenominator());

        // negatives
        f = Fraction.of(-6, 10);
        assertEquals(-3, f.getNumerator());
        assertEquals(5, f.getDenominator());

        f = Fraction.of(6, -10);
        assertEquals(3, f.getNumerator());
        assertEquals(-5, f.getDenominator());

        f = Fraction.of(-6, -10);
        assertEquals(-3, f.getNumerator());
        assertEquals(-5, f.getDenominator());

        // zero denominator
        assertThrows(ArithmeticException.class, () -> Fraction.of(1, 0));
        assertThrows(ArithmeticException.class, () -> Fraction.of(2, 0));
        assertThrows(ArithmeticException.class, () -> Fraction.of(-3, 0));

        // lang cannot represent the unsimplified fraction with MIN_VALUE as the denominator
        // assertThrows(ArithmeticException.class, () -> Fraction.getFraction(4, Integer.MIN_VALUE));
        // assertThrows(ArithmeticException.class, () -> Fraction.getFraction(1, Integer.MIN_VALUE));
        // numbers will always simplify the fraction
        f = Fraction.of(4, Integer.MIN_VALUE);
        assertEquals(-1, f.signum());
        assertEquals(1, f.getNumerator());
        assertEquals(Integer.MIN_VALUE / 4, f.getDenominator());
        // numbers can use MIN_VALUE as the denominator
        f = Fraction.of(1, Integer.MIN_VALUE);
        assertEquals(-1, f.signum());
        assertEquals(1, f.getNumerator());
        assertEquals(Integer.MIN_VALUE, f.getDenominator());
    }

/*
 *  Removed as not supported in numbers.
 *
 *  @Test
 *  void testFactory_int_int_int() {
 *      Fraction f = null;
 *
 *      // zero
 *      f = Fraction.of(0, 0, 2);
 *      assertEquals(0, f.getNumerator());
 *      assertEquals(1, f.getDenominator());
 *
 *      f = Fraction.of(2, 0, 2);
 *      assertEquals(2, f.getNumerator());
 *      assertEquals(1, f.getDenominator());
 *
 *      f = Fraction.of(0, 1, 2);
 *      assertEquals(1, f.getNumerator());
 *      assertEquals(2, f.getDenominator());
 *
 *      // normal
 *      f = Fraction.of(1, 1, 2);
 *      assertEquals(3, f.getNumerator());
 *      assertEquals(2, f.getDenominator());
 *
 *      // negatives
 *      assertThrows(ArithmeticException.class, () -> Fraction.of(1, -6, -10));
 *      assertThrows(ArithmeticException.class, () -> Fraction.of(1, -6, -10));
 *      assertThrows(ArithmeticException.class, () -> Fraction.of(1, -6, -10));
 *
 *      // negative whole
 *      f = Fraction.of(-1, 6, 10);
 *      assertEquals(-8, f.getNumerator());
 *      assertEquals(5, f.getDenominator());
 *
 *      assertThrows(ArithmeticException.class, () -> Fraction.of(-1, -6, 10));
 *      assertThrows(ArithmeticException.class, () -> Fraction.of(-1, 6, -10));
 *      assertThrows(ArithmeticException.class, () -> Fraction.of(-1, -6, -10));
 *
 *      // zero denominator
 *      assertThrows(ArithmeticException.class, () -> Fraction.of(0, 1, 0));
 *      assertThrows(ArithmeticException.class, () -> Fraction.of(1, 2, 0));
 *      assertThrows(ArithmeticException.class, () -> Fraction.of(-1, -3, 0));
 *      assertThrows(ArithmeticException.class, () -> Fraction.of(Integer.MAX_VALUE, 1, 2));
 *      assertThrows(ArithmeticException.class, () -> Fraction.of(-Integer.MAX_VALUE, 1, 2));
 *
 *      // very large
 *      f = Fraction.of(-1, 0, Integer.MAX_VALUE);
 *      assertEquals(-1, f.getNumerator());
 *      assertEquals(1, f.getDenominator());
 *
 *      // negative denominators not allowed in this constructor.
 *      assertThrows(ArithmeticException.class, () -> Fraction.of(0, 4, Integer.MIN_VALUE));
 *      assertThrows(ArithmeticException.class, () -> Fraction.of(1, 1, Integer.MAX_VALUE));
 *      assertThrows(ArithmeticException.class, () -> Fraction.of(-1, 2, Integer.MAX_VALUE));
 *  }
 */

    @Test
    void testReducedFactory_int_int() {
        Fraction f = null;

        // zero
        f = Fraction.of(0, 1);
        assertEquals(0, f.getNumerator());
        assertEquals(1, f.getDenominator());

        // normal
        f = Fraction.of(1, 1);
        assertEquals(1, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of(2, 1);
        assertEquals(2, f.getNumerator());
        assertEquals(1, f.getDenominator());

        // improper
        f = Fraction.of(22, 7);
        assertEquals(22, f.getNumerator());
        assertEquals(7, f.getDenominator());

        // negatives
        f = Fraction.of(-6, 10);
        assertEquals(-3, f.getNumerator());
        assertEquals(5, f.getDenominator());

        f = Fraction.of(6, -10);
        assertEquals(3, f.getNumerator());
        assertEquals(-5, f.getDenominator());

        f = Fraction.of(-6, -10);
        assertEquals(-3, f.getNumerator());
        assertEquals(-5, f.getDenominator());

        // zero denominator
        assertThrows(ArithmeticException.class, () -> Fraction.of(1, 0));
        assertThrows(ArithmeticException.class, () -> Fraction.of(2, 0));
        assertThrows(ArithmeticException.class, () -> Fraction.of(-3, 0));

        // reduced
        f = Fraction.of(0, 2);
        assertEquals(0, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of(2, 2);
        assertEquals(1, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of(2, 4);
        assertEquals(1, f.getNumerator());
        assertEquals(2, f.getDenominator());

        f = Fraction.of(15, 10);
        assertEquals(3, f.getNumerator());
        assertEquals(2, f.getDenominator());

        f = Fraction.of(121, 22);
        assertEquals(11, f.getNumerator());
        assertEquals(2, f.getDenominator());

        // Extreme values
        // OK, can reduce before negating
        f = Fraction.of(-2, Integer.MIN_VALUE);
        assertEquals(-1, f.getNumerator());
        assertEquals(Integer.MIN_VALUE / 2, f.getDenominator());

        // lang requires the sign to be in the numerator so this would throw.
        // assertThrows(ArithmeticException.class, () -> Fraction.getReducedFraction(-7, Integer.MIN_VALUE));
        // numbers allows the sign to be in the denominator so this does not throw.
        f = Fraction.of(-7, Integer.MIN_VALUE);
        assertEquals(1, f.signum());
        assertEquals(-7, f.getNumerator());
        assertEquals(Integer.MIN_VALUE, f.getDenominator());

        // LANG-662
        f = Fraction.of(Integer.MIN_VALUE, 2);
        assertEquals(Integer.MIN_VALUE / 2, f.getNumerator());
        assertEquals(1, f.getDenominator());
    }

    @Test
    void testFactory_double() {
        assertThrows(IllegalArgumentException.class, () -> Fraction.from(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> Fraction.from(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> Fraction.from(Double.NEGATIVE_INFINITY));
        Fraction.from((double) Integer.MAX_VALUE + 1);

        // zero
        Fraction f = Fraction.from(0.0d);
        assertEquals(0, f.getNumerator());
        assertEquals(1, f.getDenominator());

        // one
        f = Fraction.from(1.0d);
        assertEquals(1, f.getNumerator());
        assertEquals(1, f.getDenominator());

        // one half
        f = Fraction.from(0.5d);
        assertEquals(1, f.getNumerator());
        assertEquals(2, f.getDenominator());

        // negative
        f = Fraction.from(-0.875d);
        assertEquals(-7, f.getNumerator());
        assertEquals(8, f.getDenominator());

        // over 1
        f = Fraction.from(1.25d);
        assertEquals(5, f.getNumerator());
        assertEquals(4, f.getDenominator());

        // two thirds
        f = Fraction.from(0.66666d);
        assertEquals(2, f.getNumerator());
        assertEquals(3, f.getDenominator());

        // small
        f = Fraction.from(1.0d / 10001d);
        assertEquals(1, f.getNumerator());
        assertEquals(10001, f.getDenominator());

        // normal
        Fraction f2 = null;
        for (int i = 1; i <= 100; i++) {  // denominator
            for (int j = 1; j <= i; j++) {  // numerator
                f = Fraction.from((double) j / (double) i);

                f2 = Fraction.of(j, i);
                assertEquals(f2.getNumerator(), f.getNumerator());
                assertEquals(f2.getDenominator(), f.getDenominator());
            }
        }
        // save time by skipping some tests!  (
        for (int i = 1001; i <= 10000; i += SKIP) {  // denominator
            for (int j = 1; j <= i; j++) {  // numerator
                f = Fraction.from((double) j / (double) i, 1e-8, 100);
                f2 = Fraction.of(j, i);
                assertEquals(f2.getNumerator(), f.getNumerator());
                assertEquals(f2.getDenominator(), f.getDenominator());
            }
        }
    }

/*
 *  Removed as not supported in numbers.
 *
 *  @Test
 *  void testFactory_String() {
 *      assertThrows(NullPointerException.class, () -> Fraction.from(null));
 *  }
 *
 *
 *  @Test
 *  void testFactory_String_double() {
 *      Fraction f = null;
 *
 *      f = Fraction.from("0.0");
 *      assertEquals(0, f.getNumerator());
 *      assertEquals(1, f.getDenominator());
 *
 *      f = Fraction.from("0.2");
 *      assertEquals(1, f.getNumerator());
 *      assertEquals(5, f.getDenominator());
 *
 *      f = Fraction.from("0.5");
 *      assertEquals(1, f.getNumerator());
 *      assertEquals(2, f.getDenominator());
 *
 *      f = Fraction.from("0.66666");
 *      assertEquals(2, f.getNumerator());
 *      assertEquals(3, f.getDenominator());
 *
 *      assertThrows(NumberFormatException.class, () -> Fraction.from("2.3R"));
 *      assertThrows(NumberFormatException.class, () -> Fraction.from("2147483648")); // too big
 *      assertThrows(NumberFormatException.class, () -> Fraction.from("."));
 *  }
 *
 *  @Test
 *  void testFactory_String_proper() {
 *      Fraction f = null;
 *
 *      f = Fraction.from("0 0/1");
 *      assertEquals(0, f.getNumerator());
 *      assertEquals(1, f.getDenominator());
 *
 *      f = Fraction.from("1 1/5");
 *      assertEquals(6, f.getNumerator());
 *      assertEquals(5, f.getDenominator());
 *
 *      f = Fraction.from("7 1/2");
 *      assertEquals(15, f.getNumerator());
 *      assertEquals(2, f.getDenominator());
 *
 *      f = Fraction.from("1 2/4");
 *      assertEquals(3, f.getNumerator());
 *      assertEquals(2, f.getDenominator());
 *
 *      f = Fraction.from("-7 1/2");
 *      assertEquals(-15, f.getNumerator());
 *      assertEquals(2, f.getDenominator());
 *
 *      f = Fraction.from("-1 2/4");
 *      assertEquals(-3, f.getNumerator());
 *      assertEquals(2, f.getDenominator());
 *
 *      assertThrows(NumberFormatException.class, () -> Fraction.from("2 3"));
 *      assertThrows(NumberFormatException.class, () -> Fraction.from("a 3"));
 *      assertThrows(NumberFormatException.class, () -> Fraction.from("2 b/4"));
 *      assertThrows(NumberFormatException.class, () -> Fraction.from("2 "));
 *      assertThrows(NumberFormatException.class, () -> Fraction.from(" 3"));
 *      assertThrows(NumberFormatException.class, () -> Fraction.from(" "));
 *  }
 *
 *  @Test
 *  void testFactory_String_improper() {
 *      Fraction f = null;
 *
 *      f = Fraction.from("0/1");
 *      assertEquals(0, f.getNumerator());
 *      assertEquals(1, f.getDenominator());
 *
 *      f = Fraction.from("1/5");
 *      assertEquals(1, f.getNumerator());
 *      assertEquals(5, f.getDenominator());
 *
 *      f = Fraction.from("1/2");
 *      assertEquals(1, f.getNumerator());
 *      assertEquals(2, f.getDenominator());
 *
 *      f = Fraction.from("2/3");
 *      assertEquals(2, f.getNumerator());
 *      assertEquals(3, f.getDenominator());
 *
 *      f = Fraction.from("7/3");
 *      assertEquals(7, f.getNumerator());
 *      assertEquals(3, f.getDenominator());
 *
 *      f = Fraction.from("2/4");
 *      assertEquals(1, f.getNumerator());
 *      assertEquals(2, f.getDenominator());
 *
 *      assertThrows(NumberFormatException.class, () -> Fraction.from("2/d"));
 *      assertThrows(NumberFormatException.class, () -> Fraction.from("2e/3"));
 *      assertThrows(NumberFormatException.class, () -> Fraction.from("2/"));
 *      assertThrows(NumberFormatException.class, () -> Fraction.from("/"));
 *  }
 *
 *  @Test
 *  void testGets() {
 *      Fraction f = null;
 *
 *      f = Fraction.of(3, 5, 6);
 *      assertEquals(23, f.getNumerator());
 *      assertEquals(3, f.getProperWhole());
 *      assertEquals(5, f.getProperNumerator());
 *      assertEquals(6, f.getDenominator());
 *
 *      f = Fraction.of(-3, 5, 6);
 *      assertEquals(-23, f.getNumerator());
 *      assertEquals(-3, f.getProperWhole());
 *      assertEquals(5, f.getProperNumerator());
 *      assertEquals(6, f.getDenominator());
 *
 *      f = Fraction.of(Integer.MIN_VALUE, 0, 1);
 *      assertEquals(Integer.MIN_VALUE, f.getNumerator());
 *      assertEquals(Integer.MIN_VALUE, f.getProperWhole());
 *      assertEquals(0, f.getProperNumerator());
 *      assertEquals(1, f.getDenominator());
 *  }
 *
 *  @Test
 *  void testConversions() {
 *      Fraction f = null;
 *
 *      f = Fraction.of(3, 7, 8);
 *      assertEquals(3, f.intValue());
 *      assertEquals(3L, f.longValue());
 *      assertEquals(3.875f, f.floatValue(), 0.00001f);
 *      assertEquals(3.875d, f.doubleValue(), 0.00001d);
 *  }
 *
 *  @Test
 *  void testReduce() {
 *      Fraction f = null;
 *
 *      f = Fraction.of(50, 75);
 *      Fraction result = f.reduce();
 *      assertEquals(2, result.getNumerator());
 *      assertEquals(3, result.getDenominator());
 *
 *      f = Fraction.of(-2, -3);
 *      result = f.reduce();
 *      assertEquals(2, result.getNumerator());
 *      assertEquals(3, result.getDenominator());
 *
 *      f = Fraction.of(2, -3);
 *      result = f.reduce();
 *      assertEquals(-2, result.getNumerator());
 *      assertEquals(3, result.getDenominator());
 *
 *      f = Fraction.of(-2, 3);
 *      result = f.reduce();
 *      assertEquals(-2, result.getNumerator());
 *      assertEquals(3, result.getDenominator());
 *      assertSame(f, result);
 *
 *      f = Fraction.of(2, 3);
 *      result = f.reduce();
 *      assertEquals(2, result.getNumerator());
 *      assertEquals(3, result.getDenominator());
 *      assertSame(f, result);
 *
 *      f = Fraction.of(0, 1);
 *      result = f.reduce();
 *      assertEquals(0, result.getNumerator());
 *      assertEquals(1, result.getDenominator());
 *      assertSame(f, result);
 *
 *      f = Fraction.of(0, 100);
 *      result = f.reduce();
 *      assertEquals(0, result.getNumerator());
 *      assertEquals(1, result.getDenominator());
 *      assertSame(result, Fraction.ZERO);
 *
 *      f = Fraction.of(Integer.MIN_VALUE, 2);
 *      result = f.reduce();
 *      assertEquals(Integer.MIN_VALUE / 2, result.getNumerator());
 *      assertEquals(1, result.getDenominator());
 *  }
 *
 *  @Test
 *  void testreciprocal() {
 *      Fraction f = null;
 *
 *      f = Fraction.of(50, 75);
 *      f = f.reciprocal();
 *      assertEquals(3, f.getNumerator());
 *      assertEquals(2, f.getDenominator());
 *
 *      f = Fraction.of(4, 3);
 *      f = f.reciprocal();
 *      assertEquals(3, f.getNumerator());
 *      assertEquals(4, f.getDenominator());
 *
 *      f = Fraction.of(-15, 47);
 *      f = f.reciprocal();
 *      assertEquals(47, f.getNumerator());
 *      assertEquals(-15, f.getDenominator());
 *
 *      assertThrows(ArithmeticException.class, () -> Fraction.of(0, 3).reciprocal());
 *      Fraction.of(Integer.MIN_VALUE, 1).reciprocal();
 *
 *      f = Fraction.of(Integer.MAX_VALUE, 1);
 *      f = f.reciprocal();
 *      assertEquals(1, f.getNumerator());
 *      assertEquals(Integer.MAX_VALUE, f.getDenominator());
 *  }
 */

    @Test
    void testNegate() {
        Fraction f = null;

        f = Fraction.of(50, 75);
        f = f.negate();
        assertEquals(-2, f.getNumerator());
        assertEquals(3, f.getDenominator());

        f = Fraction.of(-50, 75);
        f = f.negate();
        assertEquals(2, f.getNumerator());
        assertEquals(3, f.getDenominator());

        // large values
        f = Fraction.of(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        f = f.negate();
        assertEquals(Integer.MIN_VALUE + 2, f.getNumerator());
        assertEquals(Integer.MAX_VALUE, f.getDenominator());

        // lang requires the sign in the numerator and so cannot negate MIN_VALUE as the numerator
        // assertThrows(ArithmeticException.class, () -> Fraction.getFraction(Integer.MIN_VALUE, 1).negate());
        // numbers allows the sign in the numerator or denominator
        f = Fraction.of(Integer.MIN_VALUE, 1).negate();
        assertEquals(1, f.signum());
        assertEquals(Integer.MIN_VALUE, f.getNumerator());
        assertEquals(-1, f.getDenominator());
    }

    @Test
    void testAbs() {
        Fraction f = null;

        f = Fraction.of(50, 75);
        f = f.abs();
        assertEquals(2, f.getNumerator());
        assertEquals(3, f.getDenominator());

        f = Fraction.of(-50, 75);
        f = f.abs();
        assertEquals(2, f.getNumerator());
        assertEquals(3, f.getDenominator());

        f = Fraction.of(Integer.MAX_VALUE, 1);
        f = f.abs();
        assertEquals(Integer.MAX_VALUE, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of(Integer.MAX_VALUE, -1);
        f = f.abs();
        assertEquals(-Integer.MAX_VALUE, f.getNumerator());
        assertEquals(-1, f.getDenominator());

        // lang requires the sign in the numerator and so cannot compute the absolute with MIN_VALUE as the numerator
        // assertThrows(ArithmeticException.class, () -> Fraction.getFraction(Integer.MIN_VALUE, 1).abs());
        // numbers allows the sign in the numerator or denominator
        f = Fraction.of(Integer.MIN_VALUE, 1).abs();
        assertEquals(1, f.signum());
        assertEquals(Integer.MIN_VALUE, f.getNumerator());
        assertEquals(-1, f.getDenominator());
    }

    @Test
    void testPow() {
        Fraction f = null;

        f = Fraction.of(3, 5);
        assertEquals(Fraction.ONE, f.pow(0));

        f = Fraction.of(3, 5);
        assertEquals(f, f.pow(1));
        assertEquals(f, f.pow(1));

        f = Fraction.of(3, 5);
        f = f.pow(2);
        assertEquals(9, f.getNumerator());
        assertEquals(25, f.getDenominator());

        f = Fraction.of(3, 5);
        f = f.pow(3);
        assertEquals(27, f.getNumerator());
        assertEquals(125, f.getDenominator());

        f = Fraction.of(3, 5);
        f = f.pow(-1);
        assertEquals(5, f.getNumerator());
        assertEquals(3, f.getDenominator());

        f = Fraction.of(3, 5);
        f = f.pow(-2);
        assertEquals(25, f.getNumerator());
        assertEquals(9, f.getDenominator());

        // check unreduced fractions stay that way.
        f = Fraction.of(6, 10);
        assertEquals(Fraction.ONE, f.pow(0));

        f = Fraction.of(6, 10);
        assertEquals(f, f.pow(1));
        assertEquals(f.pow(1), Fraction.of(3, 5));

        f = Fraction.of(6, 10);
        f = f.pow(2);
        assertEquals(9, f.getNumerator());
        assertEquals(25, f.getDenominator());

        f = Fraction.of(6, 10);
        f = f.pow(3);
        assertEquals(27, f.getNumerator());
        assertEquals(125, f.getDenominator());

        f = Fraction.of(6, 10);
        f = f.pow(-1);
        assertEquals(5, f.getNumerator());
        assertEquals(3, f.getDenominator());

        f = Fraction.of(6, 10);
        f = f.pow(-2);
        assertEquals(25, f.getNumerator());
        assertEquals(9, f.getDenominator());

        // zero to any positive power is still zero.
        f = Fraction.of(0, 1231);
        f = f.pow(1);
        assertEquals(0, f.compareTo(Fraction.ZERO));
        assertEquals(0, f.getNumerator());
        assertEquals(1, f.getDenominator());
        f = f.pow(2);
        assertEquals(0, f.compareTo(Fraction.ZERO));
        assertEquals(0, f.getNumerator());
        assertEquals(1, f.getDenominator());

        // zero to negative powers should throw an exception
        final Fraction fr = f;
        assertThrows(ArithmeticException.class, () -> fr.pow(-1));
        assertThrows(ArithmeticException.class, () -> fr.pow(Integer.MIN_VALUE));

        // one to any power is still one.
        f = Fraction.of(1, 1);
        f = f.pow(0);
        assertEquals(Fraction.ONE, f);
        f = f.pow(1);
        assertEquals(Fraction.ONE, f);
        f = f.pow(-1);
        assertEquals(Fraction.ONE, f);
        f = f.pow(Integer.MAX_VALUE);
        assertEquals(Fraction.ONE, f);
        f = f.pow(Integer.MIN_VALUE);
        assertEquals(Fraction.ONE, f);

        assertOperationThrows(ArithmeticException.class, Fraction.of(Integer.MAX_VALUE, 1), a -> a.pow(2));

        // Numerator growing too negative during the pow operation.
        assertOperationThrows(ArithmeticException.class, Fraction.of(Integer.MIN_VALUE, 1), a -> a.pow(3));

        assertOperationThrows(ArithmeticException.class, Fraction.of(65536, 1), a -> a.pow(2));
    }

    @Test
    void testAdd() {
        Fraction f = null;
        Fraction f1 = null;
        Fraction f2 = null;

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(1, 5);
        f = f1.add(f2);
        assertEquals(4, f.getNumerator());
        assertEquals(5, f.getDenominator());

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(2, 5);
        f = f1.add(f2);
        assertEquals(1, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(3, 5);
        f = f1.add(f2);
        assertEquals(6, f.getNumerator());
        assertEquals(5, f.getDenominator());

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(-4, 5);
        f = f1.add(f2);
        assertEquals(-1, f.getNumerator());
        assertEquals(5, f.getDenominator());

        f1 = Fraction.of(Integer.MAX_VALUE - 1, 1);
        f2 = Fraction.ONE;
        f = f1.add(f2);
        assertEquals(Integer.MAX_VALUE, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(1, 2);
        f = f1.add(f2);
        assertEquals(11, f.getNumerator());
        assertEquals(10, f.getDenominator());

        f1 = Fraction.of(3, 8);
        f2 = Fraction.of(1, 6);
        f = f1.add(f2);
        assertEquals(13, f.getNumerator());
        assertEquals(24, f.getDenominator());

        f1 = Fraction.of(0, 5);
        f2 = Fraction.of(1, 5);
        f = f1.add(f2);
        assertSame(f2, f);
        f = f2.add(f1);
        assertSame(f2, f);

        f1 = Fraction.of(-1, 13 * 13 * 2 * 2);
        f2 = Fraction.of(-2, 13 * 17 * 2);
        final Fraction fr = f1.add(f2);
        assertEquals(13 * 13 * 17 * 2 * 2, fr.getDenominator());
        assertEquals(-17 - 2 * 13 * 2, fr.getNumerator());

        assertThrows(NullPointerException.class, () -> fr.add(null));

        // if this fraction is added naively, it will overflow.
        // check that it doesn't.
        f1 = Fraction.of(1, 32768 * 3);
        f2 = Fraction.of(1, 59049);
        f = f1.add(f2);
        assertEquals(52451, f.getNumerator());
        assertEquals(1934917632, f.getDenominator());

        f1 = Fraction.of(Integer.MIN_VALUE, 3);
        f2 = Fraction.of(1, 3);
        f = f1.add(f2);
        assertEquals(Integer.MIN_VALUE + 1, f.getNumerator());
        assertEquals(3, f.getDenominator());

        f1 = Fraction.of(Integer.MAX_VALUE - 1, 1);
        f2 = Fraction.ONE;
        f = f1.add(f2);
        assertEquals(Integer.MAX_VALUE, f.getNumerator());
        assertEquals(1, f.getDenominator());

        final Fraction overflower = f;
        assertThrows(ArithmeticException.class, () -> overflower.add(Fraction.ONE)); // should overflow

        // denominator should not be a multiple of 2 or 3 to trigger overflow
        assertOperationThrows(ArithmeticException.class, Fraction.of(Integer.MIN_VALUE, 5), Fraction.of(-1, 5), Fraction::add);

        final Fraction maxValue = Fraction.of(-Integer.MAX_VALUE, 1);
        assertThrows(ArithmeticException.class, () -> maxValue.add(maxValue));

        final Fraction negativeMaxValue = Fraction.of(-Integer.MAX_VALUE, 1);
        assertThrows(ArithmeticException.class, () -> negativeMaxValue.add(negativeMaxValue));

        final Fraction f3 = Fraction.of(3, 327680);
        final Fraction f4 = Fraction.of(2, 59049);
        assertThrows(ArithmeticException.class, () -> f3.add(f4)); // should overflow
    }

    @Test
    void testSubtract() {
        Fraction f = null;
        Fraction f1 = null;
        Fraction f2 = null;

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(1, 5);
        f = f1.subtract(f2);
        assertEquals(2, f.getNumerator());
        assertEquals(5, f.getDenominator());

        f1 = Fraction.of(7, 5);
        f2 = Fraction.of(2, 5);
        f = f1.subtract(f2);
        assertEquals(1, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(3, 5);
        f = f1.subtract(f2);
        assertEquals(0, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(-4, 5);
        f = f1.subtract(f2);
        assertEquals(7, f.getNumerator());
        assertEquals(5, f.getDenominator());

        f1 = Fraction.of(0, 5);
        f2 = Fraction.of(4, 5);
        f = f1.subtract(f2);
        assertEquals(-4, f.getNumerator());
        assertEquals(5, f.getDenominator());

        f1 = Fraction.of(0, 5);
        f2 = Fraction.of(-4, 5);
        f = f1.subtract(f2);
        assertEquals(4, f.getNumerator());
        assertEquals(5, f.getDenominator());

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(1, 2);
        f = f1.subtract(f2);
        assertEquals(1, f.getNumerator());
        assertEquals(10, f.getDenominator());

        f1 = Fraction.of(0, 5);
        f2 = Fraction.of(1, 5);
        f = f2.subtract(f1);
        assertSame(f2, f);

        final Fraction fr = f;
        assertThrows(NullPointerException.class, () -> fr.subtract(null));

        // if this fraction is subtracted naively, it will overflow.
        // check that it doesn't.
        f1 = Fraction.of(1, 32768 * 3);
        f2 = Fraction.of(1, 59049);
        f = f1.subtract(f2);
        assertEquals(-13085, f.getNumerator());
        assertEquals(1934917632, f.getDenominator());

        f1 = Fraction.of(Integer.MIN_VALUE, 3);
        f2 = Fraction.of(1, 3).negate();
        f = f1.subtract(f2);
        assertEquals(Integer.MIN_VALUE + 1, f.getNumerator());
        assertEquals(3, f.getDenominator());

        f1 = Fraction.of(Integer.MAX_VALUE, 1);
        f2 = Fraction.ONE;
        f = f1.subtract(f2);
        assertEquals(Integer.MAX_VALUE - 1, f.getNumerator());
        assertEquals(1, f.getDenominator());

        // Should overflow
        assertOperationThrows(ArithmeticException.class, Fraction.of(1, Integer.MAX_VALUE), Fraction.of(1, Integer.MAX_VALUE - 1), Fraction::subtract);
        f = f1.subtract(f2);

        // denominator should not be a multiple of 2 or 3 to trigger overflow
        assertOperationThrows(ArithmeticException.class, Fraction.of(Integer.MIN_VALUE, 5), Fraction.of(1, 5), Fraction::subtract);

        assertOperationThrows(ArithmeticException.class, Fraction.of(Integer.MIN_VALUE, 1), Fraction.ONE, Fraction::subtract);

        assertOperationThrows(ArithmeticException.class, Fraction.of(Integer.MAX_VALUE, 1), Fraction.ONE.negate(), Fraction::subtract);

        // Should overflow
        assertOperationThrows(ArithmeticException.class, Fraction.of(3, 327680), Fraction.of(2, 59049), Fraction::subtract);
    }

    @Test
    void testMultiply() {
        Fraction f = null;
        Fraction f1 = null;
        Fraction f2 = null;

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(2, 5);
        f = f1.multiply(f2);
        assertEquals(6, f.getNumerator());
        assertEquals(25, f.getDenominator());

        f1 = Fraction.of(6, 10);
        f2 = Fraction.of(6, 10);
        f = f1.multiply(f2);
        assertEquals(9, f.getNumerator());
        assertEquals(25, f.getDenominator());
        f = f.multiply(f2);
        assertEquals(27, f.getNumerator());
        assertEquals(125, f.getDenominator());

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(-2, 5);
        f = f1.multiply(f2);
        assertEquals(-6, f.getNumerator());
        assertEquals(25, f.getDenominator());

        f1 = Fraction.of(-3, 5);
        f2 = Fraction.of(-2, 5);
        f = f1.multiply(f2);
        assertEquals(6, f.getNumerator());
        assertEquals(25, f.getDenominator());

        f1 = Fraction.of(0, 5);
        f2 = Fraction.of(2, 7);
        f = f1.multiply(f2);
        assertSame(Fraction.ZERO, f);

        f1 = Fraction.of(2, 7);
        f2 = Fraction.ONE;
        f = f1.multiply(f2);
        assertEquals(2, f.getNumerator());
        assertEquals(7, f.getDenominator());

        f1 = Fraction.of(Integer.MAX_VALUE, 1);
        f2 = Fraction.of(Integer.MIN_VALUE, Integer.MAX_VALUE);
        f = f1.multiply(f2);
        assertEquals(Integer.MIN_VALUE, f.getNumerator());
        assertEquals(1, f.getDenominator());

        final Fraction fr = f;
        assertThrows(NullPointerException.class, () -> fr.multiply(null));

        final Fraction fr1 = Fraction.of(1, Integer.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> fr1.multiply(fr1));

        final Fraction fr2 = Fraction.of(1, -Integer.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> fr2.multiply(fr2));
    }

    @Test
    void testDivide() {
        Fraction f = null;
        Fraction f1 = null;
        Fraction f2 = null;

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(2, 5);
        f = f1.divide(f2);
        assertEquals(3, f.getNumerator());
        assertEquals(2, f.getDenominator());

        assertOperationThrows(ArithmeticException.class, Fraction.of(3, 5), Fraction.ZERO, Fraction::divide);

        f1 = Fraction.of(0, 5);
        f2 = Fraction.of(2, 7);
        f = f1.divide(f2);
        assertSame(Fraction.ZERO, f);

        f1 = Fraction.of(2, 7);
        f2 = Fraction.ONE;
        f = f1.divide(f2);
        assertEquals(2, f.getNumerator());
        assertEquals(7, f.getDenominator());

        f1 = Fraction.of(1, Integer.MAX_VALUE);
        f = f1.divide(f1);
        assertEquals(1, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f1 = Fraction.of(Integer.MIN_VALUE, Integer.MAX_VALUE);
        f2 = Fraction.of(1, Integer.MAX_VALUE);
        final Fraction fr = f1.divide(f2);
        assertEquals(Integer.MIN_VALUE, fr.getNumerator());
        assertEquals(1, fr.getDenominator());

        assertThrows(NullPointerException.class, () -> fr.divide(null));

        final Fraction smallest = Fraction.of(1, Integer.MAX_VALUE);
        final Fraction smallestReciprocal = smallest.reciprocal();
        assertThrows(ArithmeticException.class, () -> smallest.divide(smallestReciprocal)); // Should overflow

        final Fraction negative = Fraction.of(1, -Integer.MAX_VALUE);
        final Fraction negativeReciprocal = negative.reciprocal();
        assertThrows(ArithmeticException.class, () -> negative.divide(negativeReciprocal)); // Should overflow
    }

    @Test
    void testEquals() {
        Fraction f1 = null;
        Fraction f2 = null;

        f1 = Fraction.of(3, 5);
        assertNotEquals(f1, null);
        assertNotEquals(f1, new Object());
        assertNotEquals(f1, Integer.valueOf(6));

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(2, 5);
        assertNotEquals(f1, f2);
        assertEquals(f1, f1);
        assertEquals(f2, f2);

        f2 = Fraction.of(3, 5);
        assertEquals(f1, f2);

        f2 = Fraction.of(6, 10);
        assertEquals(f1, f2);
    }

    @Test
    void testHashCode() {
        final Fraction f1 = Fraction.of(3, 5);
        Fraction f2 = Fraction.of(3, 5);

        assertEquals(f1.hashCode(), f2.hashCode());

        f2 = Fraction.of(2, 5);
        assertNotEquals(f1.hashCode(), f2.hashCode());

        f2 = Fraction.of(6, 10);
        assertEquals(f1.hashCode(), f2.hashCode());
    }

    @Test
    void testCompareTo() {
        Fraction f1 = null;
        Fraction f2 = null;

        f1 = Fraction.of(3, 5);
        assertEquals(0, f1.compareTo(f1));

        final Fraction fr = f1;
        assertThrows(NullPointerException.class, () -> fr.compareTo(null));

        f2 = Fraction.of(2, 5);
        assertTrue(f1.compareTo(f2) > 0);
        assertEquals(0, f2.compareTo(f2));

        f2 = Fraction.of(4, 5);
        assertTrue(f1.compareTo(f2) < 0);
        assertEquals(0, f2.compareTo(f2));

        f2 = Fraction.of(3, 5);
        assertEquals(0, f1.compareTo(f2));
        assertEquals(0, f2.compareTo(f2));

        f2 = Fraction.of(6, 10);
        assertEquals(0, f1.compareTo(f2));
        assertEquals(0, f2.compareTo(f2));

        /*
         *  Removed as not supported in numbers.
         *
         *  f2 = Fraction.of(-1, 1, Integer.MAX_VALUE);
         *  assertTrue(f1.compareTo(f2) > 0);
         *  assertEquals(0, f2.compareTo(f2));
         */
    }

    @Test
    void testToString() {
        Fraction f = null;

        f = Fraction.of(3, 5);
        final String str = f.toString();
        assertEquals("3 / 5", str);
        assertEquals(str, f.toString());

        f = Fraction.of(7, 5);
        assertEquals("7 / 5", f.toString());

        f = Fraction.of(4, 2);
        assertEquals("2", f.toString());

        f = Fraction.of(0, 2);
        assertEquals("0", f.toString());

        f = Fraction.of(2, 2);
        assertEquals("1", f.toString());

        f = Fraction.of(Integer.MIN_VALUE);
        assertEquals("-2147483648", f.toString());

        f = Fraction.of(-1).add(Fraction.of(-1, Integer.MAX_VALUE));
        assertEquals("-2147483648 / 2147483647", f.toString());
    }

/*
 *  Removed as not supported in numbers.
 *
 *  @Test
 *  void testToProperString() {
 *      Fraction f = null;
 *
 *      f = Fraction.of(3, 5);
 *      final String str = f.toProperString();
 *      assertEquals("3/5", str);
 *      assertSame(str, f.toProperString());
 *
 *      f = Fraction.of(7, 5);
 *      assertEquals("1 2/5", f.toProperString());
 *
 *      f = Fraction.of(14, 10);
 *      assertEquals("1 2/5", f.toProperString());
 *
 *      f = Fraction.of(4, 2);
 *      assertEquals("2", f.toProperString());
 *
 *      f = Fraction.of(0, 2);
 *      assertEquals("0", f.toProperString());
 *
 *      f = Fraction.of(2, 2);
 *      assertEquals("1", f.toProperString());
 *
 *      f = Fraction.of(-7, 5);
 *      assertEquals("-1 2/5", f.toProperString());
 *
 *      f = Fraction.of(Integer.MIN_VALUE, 0, 1);
 *      assertEquals("-2147483648", f.toProperString());
 *
 *      f = Fraction.of(-1, 1, Integer.MAX_VALUE);
 *      assertEquals("-1 1/2147483647", f.toProperString());
 *
 *      assertEquals("-1", Fraction.of(-1).toProperString());
 *  }
 */

    /**
     * Assert the specified operation on the fraction throws the expected type.
     * This method exists to ensure the fractions are constructed without an exception
     * and the operation is tested to throw the exception.
     *
     * @param <T> the generic type
     * @param expectedType the expected type
     * @param f the fraction
     * @param operation the operation
     * @return the throwable
     */
    private static <T extends Throwable> T assertOperationThrows(Class<T> expectedType,
            Fraction f, UnaryOperator<Fraction> operation) {
        return assertThrows(expectedType, () -> operation.apply(f));
    }

    /**
     * Assert the specified operation on two fractions throws the expected type.
     * This method exists to ensure the fractions are constructed without an exception
     * and the operation is tested to throw the exception.
     *
     * @param <T> the generic type
     * @param expectedType the expected type
     * @param f1 the first fraction
     * @param f2 the second fraction
     * @param operation the operation
     * @return the throwable
     */
    private static <T extends Throwable> T assertOperationThrows(Class<T> expectedType,
            Fraction f1, Fraction f2, BiFunction<Fraction, Fraction, Fraction> operation) {
        return assertThrows(expectedType, () -> operation.apply(f1, f2));
    }
}
