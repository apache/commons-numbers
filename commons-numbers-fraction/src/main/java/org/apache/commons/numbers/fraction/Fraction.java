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

import java.io.Serializable;
import org.apache.commons.numbers.core.ArithmeticUtils;
import org.apache.commons.numbers.core.NativeOperators;

/**
 * Representation of a rational number.
 *
 * <p>The number is expressed as the quotient {@code p/q} of two 32-bit integers,
 * a numerator {@code p} and a non-zero denominator {@code q}.
 *
 * <p>This class is immutable.
 *
 * <a href="https://en.wikipedia.org/wiki/Rational_number">Rational number</a>
 */
public final class Fraction
    extends Number
    implements Comparable<Fraction>,
               NativeOperators<Fraction>,
               Serializable {
    /** A fraction representing "0". */
    public static final Fraction ZERO = new Fraction(0, 1);

    /** A fraction representing "1". */
    public static final Fraction ONE = new Fraction(1, 1);

    /** Serializable version identifier. */
    private static final long serialVersionUID = 20190701L;

    /** The default epsilon used for convergence. */
    private static final double DEFAULT_EPSILON = 1e-5;

    /** Message for non-finite input double argument to factory constructors. */
    private static final String NOT_FINITE = "Not finite: ";

    /** The numerator of this fraction reduced to lowest terms. */
    private final int numerator;

    /** The denominator of this fraction reduced to lowest terms. */
    private final int denominator;

    /**
     * Private constructor: Instances are created using factory methods.
     *
     * @param num Numerator.
     * @param den Denominator.
     * @throws ArithmeticException if the denominator is {@code zero}
     * or if integer overflow occurs.
     */
    private Fraction(int num, int den) {
        if (den == 0) {
            throw new FractionException(FractionException.ERROR_ZERO_DENOMINATOR);
        }

        if (num == den) {
            numerator = 1;
            denominator = 1;
        } else {
            // Reduce numerator (p) and denominator (q) by greatest common divisor.
            int p;
            int q;

            // If num and den are both 2^-31, or if one is 0 and the other is 2^-31,
            // the calculation of the gcd below will fail. Ensure that this does not
            // happen by dividing both by 2 in case both are even.
            if (((num | den) & 1) == 0) {
                p = num >> 1;
                q = den >> 1;
            } else {
                p = num;
                q = den;
            }

            final int d = ArithmeticUtils.gcd(p, q);
            if (d > 1) {
                p /= d;
                q /= d;
            }

            numerator = p;
            denominator = q;
        }
    }

    /**
     * Create a fraction given the double value and either the maximum error
     * allowed or the maximum number of denominator digits.
     *
     * <p>
     * NOTE: This constructor is called with:
     * <ul>
     *  <li>EITHER a valid epsilon value and the maxDenominator set to
     *      Integer.MAX_VALUE (that way the maxDenominator has no effect)
     *  <li>OR a valid maxDenominator value and the epsilon value set to
     *      zero (that way epsilon only has effect if there is an exact
     *      match before the maxDenominator value is reached).
     * </ul>
     * <p>
     * It has been done this way so that the same code can be reused for
     * both scenarios. However this could be confusing to users if it
     * were part of the public API and this method should therefore remain
     * PRIVATE.
     * </p>
     *
     * <p>
     * See JIRA issue ticket MATH-181 for more details:
     *     https://issues.apache.org/jira/browse/MATH-181
     * </p>
     *
     * @param value Value to convert to a fraction.
     * @param epsilon Maximum error allowed.
     * The resulting fraction is within {@code epsilon} of {@code value},
     * in absolute terms.
     * @param maxDenominator Maximum denominator value allowed.
     * @param maxIterations Maximum number of convergents.
     * @throws IllegalArgumentException if the given {@code value} is NaN or infinite.
     * @throws ArithmeticException if the continued fraction failed to converge.
     */
    private Fraction(final double value,
                     final double epsilon,
                     final int maxDenominator,
                     final int maxIterations) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(NOT_FINITE + value);
        }

        final long overflow = Integer.MAX_VALUE;
        double r0 = value;
        long a0 = (long)Math.floor(r0);
        if (Math.abs(a0) > overflow) {
            throw new FractionException(FractionException.ERROR_CONVERSION, value, a0, 1L);
        }

        // check for (almost) integer arguments, which should not go to iterations.
        if (Math.abs(a0 - value) < epsilon) {
            this.numerator = (int) a0;
            this.denominator = 1;
            return;
        }

        long p0 = 1;
        long q0 = 0;
        long p1 = a0;
        long q1 = 1;

        long p2 = 0;
        long q2 = 1;

        int n = 0;
        boolean stop = false;
        do {
            ++n;
            final double r1 = 1.0 / (r0 - a0);
            final long a1 = (long)Math.floor(r1);
            p2 = (a1 * p1) + p0;
            q2 = (a1 * q1) + q0;

            if (Math.abs(p2) > overflow ||
                Math.abs(q2) > overflow) {
                // in maxDenominator mode, if the last fraction was very close to the actual value
                // q2 may overflow in the next iteration; in this case return the last one.
                if (epsilon == 0.0 &&
                    Math.abs(q1) < maxDenominator) {
                    break;
                }
                throw new FractionException(FractionException.ERROR_CONVERSION, value, p2, q2);
            }

            final double convergent = (double)p2 / (double)q2;
            if (n < maxIterations &&
                Math.abs(convergent - value) > epsilon &&
                q2 < maxDenominator) {
                p0 = p1;
                p1 = p2;
                q0 = q1;
                q1 = q2;
                a0 = a1;
                r0 = r1;
            } else {
                stop = true;
            }
        } while (!stop);

        if (n >= maxIterations) {
            throw new FractionException(FractionException.ERROR_CONVERSION, value, maxIterations);
        }

        if (q2 < maxDenominator) {
            this.numerator = (int) p2;
            this.denominator = (int) q2;
        } else {
            this.numerator = (int) p1;
            this.denominator = (int) q1;
        }
    }

    /**
     * Create a fraction given the double value.
     *
     * @param value Value to convert to a fraction.
     * @throws IllegalArgumentException if the given {@code value} is NaN or infinite.
     * @throws ArithmeticException if the continued fraction failed to
     * converge.
     * @return a new instance.
     */
    public static Fraction from(final double value) {
        return from(value, DEFAULT_EPSILON, 100);
    }

    /**
     * Create a fraction given the double value and maximum error allowed.
     *
     * <p>
     * References:
     * <ul>
     * <li><a href="http://mathworld.wolfram.com/ContinuedFraction.html">
     * Continued Fraction</a> equations (11) and (22)-(26)</li>
     * </ul>
     *
     * @param value Value to convert to a fraction.
     * @param epsilon Maximum error allowed. The resulting fraction is within
     * {@code epsilon} of {@code value}, in absolute terms.
     * @param maxIterations Maximum number of convergents.
     * @throws IllegalArgumentException if the given {@code value} is NaN or infinite.
     * @throws ArithmeticException if the continued fraction failed to converge.
     * @return a new instance.
     */
    public static Fraction from(final double value,
                                final double epsilon,
                                final int maxIterations) {
        return new Fraction(value, epsilon, Integer.MAX_VALUE, maxIterations);
    }

    /**
     * Create a fraction given the double value and maximum denominator.
     *
     * <p>
     * References:
     * <ul>
     * <li><a href="http://mathworld.wolfram.com/ContinuedFraction.html">
     * Continued Fraction</a> equations (11) and (22)-(26)</li>
     * </ul>
     *
     * @param value Value to convert to a fraction.
     * @param maxDenominator Maximum allowed value for denominator.
     * @throws IllegalArgumentException if the given {@code value} is NaN or infinite.
     * @throws ArithmeticException if the continued fraction failed to converge.
     * @return a new instance.
     */
    public static Fraction from(final double value,
                                final int maxDenominator) {
        return new Fraction(value, 0, maxDenominator, 100);
    }

    /**
     * Create a fraction given the numerator. The denominator is {@code 1}.
     *
     * @param num Numerator.
     * @return a new instance.
     */
    public static Fraction of(final int num) {
        return of(num, 1);
    }

    /**
     * Create a fraction given the numerator and denominator.
     * The fraction is reduced to lowest terms.
     *
     * @param num Numerator.
     * @param den Denominator.
     * @throws ArithmeticException if the denominator is {@code zero}
     * or if integer overflow occurs.
     * @return a new instance.
     */
    public static Fraction of(final int num, final int den) {
        return new Fraction(num, den);
    }

    /**
     * Returns a {@code Fraction} instance representing the specified string {@code s}.
     *
     * <p>If {@code s} is {@code null}, then a {@code NullPointerException} is thrown.
     *
     * <p>The string must be in a format compatible with that produced by
     * {@link #toString() Fraction.toString()}.
     * The format expects an integer optionally followed by a {@code '/'} character and
     * and second integer. Leading and trailing spaces are allowed around each numeric part.
     * Each numeric part is parsed using {@link Integer#parseInt(String)}. The parts
     * are interpreted as the numerator and optional denominator of the fraction. If absent
     * the denominator is assumed to be "1".
     *
     * <p>Examples of valid strings and the equivalent {@code Fraction} are shown below:
     *
     * <pre>
     * "0"                 = Fraction.of(0)
     * "42"                = Fraction.of(42)
     * "0 / 1"             = Fraction.of(0, 1)
     * "1 / 3"             = Fraction.of(1, 3)
     * "-4 / 13"           = Fraction.of(-4, 13)</pre>
     *
     * <p>Note: The fraction is returned in reduced form and the numerator and denominator
     * may not match the values in the input string. For this reason the result of
     * {@code Fraction.parse(s).toString().equals(s)} may not be {@code true}.
     *
     * @param s String representation.
     * @return an instance.
     * @throws NullPointerException if the string is null.
     * @throws NumberFormatException if the string does not contain a parsable fraction.
     * @see Integer#parseInt(String)
     * @see #toString()
     */
    public static Fraction parse(String s) {
        final String stripped = s.replace(",", "");
        final int slashLoc = stripped.indexOf('/');
        // if no slash, parse as single number
        if (slashLoc == -1) {
            return Fraction.of(Integer.parseInt(stripped.trim()));
        }
        final int num = Integer.parseInt(stripped.substring(0, slashLoc).trim());
        final int denom = Integer.parseInt(stripped.substring(slashLoc + 1).trim());
        return of(num, denom);
    }

    @Override
    public Fraction zero() {
        return ZERO;
    }

    @Override
    public Fraction one() {
        return ONE;
    }

    /**
     * Access the numerator as an {@code int}.
     *
     * @return the numerator as an {@code int}.
     */
    public int getNumerator() {
        return numerator;
    }

    /**
     * Access the denominator as an {@code int}.
     *
     * @return the denominator as an {@code int}.
     */
    public int getDenominator() {
        return denominator;
    }

    /**
     * Retrieves the sign of this fraction.
     *
     * @return -1 if the value is strictly negative, 1 if it is strictly
     * positive, 0 if it is 0.
     */
    public int signum() {
        if ((numerator > 0 && denominator > 0) ||
            (numerator < 0 && denominator < 0)) {
            return 1;
        } else if (isZero()) {
            return 0;
        } else {
            return -1;
        }
    }

    /**
     * Returns the absolute value of this fraction.
     *
     * @return the absolute value.
     */
    public Fraction abs() {
        return signum() >= 0 ?
            this :
            negate();
    }

    @Override
    public Fraction negate() {
        return numerator == Integer.MIN_VALUE ?
            new Fraction(numerator, -denominator) :
            new Fraction(-numerator, denominator);
    }

    @Override
    public Fraction reciprocal() {
        return new Fraction(denominator, numerator);
    }

    /**
     * Returns the {@code double} value closest to this fraction.
     * This calculates the fraction as numerator divided by denominator.
     *
     * @return the fraction as a {@code double}.
     */
    @Override
    public double doubleValue() {
        return (double) numerator / (double) denominator;
    }

    /**
     * Returns the {@code float} value closest to this fraction.
     * This calculates the fraction as numerator divided by denominator.
     *
     * @return the fraction as {@code float}.
     */
    @Override
    public float floatValue() {
        return (float) doubleValue();
    }

    /**
     * Returns the whole number part of the fraction.
     *
     * @return the largest {@code int} value that is not larger than this fraction.
     */
    @Override
    public int intValue() {
        return (int) doubleValue();
    }

    /**
     * Returns the whole number part of the fraction.
     *
     * @return the largest {@code long} value that is not larger than this fraction.
     */
    @Override
    public long longValue() {
        return (long) doubleValue();
    }

    /**
     * Adds the specified {@code value} to this fraction, returning
     * the result in reduced form.
     *
     * @param value Value to add.
     * @return {@code this + value}.
     * @throws ArithmeticException if the resulting numerator or denominator
     * cannot be represented in an {@code int}.
     */
    public Fraction add(final int value) {
        return new Fraction(numerator + value * denominator, denominator);
    }

    /**
     * Adds the specified {@code value} to this fraction, returning
     * the result in reduced form.
     *
     * @param value Value to add.
     * @return {@code this + value}.
     * @throws ArithmeticException if the resulting numerator or denominator
     * cannot be represented in an {@code int}.
     */
    @Override
    public Fraction add(Fraction value) {
        return addSub(value, true /* add */);
    }

    /**
     * Subtracts the specified {@code value} from this fraction, returning
     * the result in reduced form.
     *
     * @param value Value to subtract.
     * @return {@code this - value}.
     * @throws ArithmeticException if the resulting numerator or denominator
     * cannot be represented in an {@code int}.
     */
    public Fraction subtract(final int value) {
        return new Fraction(numerator - value * denominator, denominator);
    }

    /**
     * Subtracts the specified {@code value} from this fraction, returning
     * the result in reduced form.
     *
     * @param value Value to subtract.
     * @return {@code this - value}.
     * @throws ArithmeticException if the resulting numerator or denominator
     * cannot be represented in an {@code int}.
     */
    @Override
    public Fraction subtract(Fraction value) {
        return addSub(value, false /* subtract */);
    }

    /**
     * Implements add and subtract using algorithm described in Knuth 4.5.1.
     *
     * @param value Fraction to add or subtract.
     * @param isAdd Whether the operation is "add" or "subtract".
     * @return a new instance.
     * @throws ArithmeticException if the resulting numerator or denominator
     * cannot be represented in an {@code int}.
     */
    private Fraction addSub(Fraction value, boolean isAdd) {
        if (value.isZero()) {
            return this;
        }
        // Zero is identity for addition.
        if (isZero()) {
            return isAdd ? value : value.negate();
        }

        /*
         * Let the two fractions be u/u' and v/v', and d1 = gcd(u', v').
         * First, compute t, defined as:
         *
         * t = u(v'/d1) +/- v(u'/d1)
         */
        final int d1 = ArithmeticUtils.gcd(denominator, value.denominator);
        final long uvp = (long) numerator * (long) (value.denominator / d1);
        final long upv = (long) value.numerator * (long) (denominator / d1);

        /*
         * The largest possible absolute value of a product of two ints is 2^62,
         * which can only happen as a result of -2^31 * -2^31 = 2^62, so a
         * product of -2^62 is not possible. It follows that (uvp - upv) cannot
         * overflow, and (uvp + upv) could only overflow if uvp = upv = 2^62.
         * But for this to happen, the terms u, v, v'/d1 and u'/d1 would all
         * have to be -2^31, which is not possible because v'/d1 and u'/d1
         * are necessarily coprime.
         */
        final long t = isAdd ? uvp + upv : uvp - upv;

        /*
         * Because u is coprime to u' and v is coprime to v', t is necessarily
         * coprime to both v'/d1 and u'/d1. However, it might have a common
         * factor with d1.
         */
        final long d2 = ArithmeticUtils.gcd(t, d1);
        // result is (t/d2) / (u'/d1)(v'/d2)
        return of(Math.toIntExact(t / d2),
                  Math.multiplyExact(denominator / d1,
                                     value.denominator / (int) d2));
    }

    /**
     * Multiply this fraction by the passed {@code value}, returning
     * the result in reduced form.
     *
     * @param value Value to multiply by.
     * @return {@code this * value}.
     * @throws ArithmeticException if the resulting numerator or denominator
     * cannot be represented in an {@code int}.
     */
    @Override
    public Fraction multiply(final int value) {
        if (value == 0 || isZero()) {
            return ZERO;
        }
        return multiply(of(value));
    }

    /**
     * Multiply this fraction by the passed {@code value}, returning
     * the result in reduced form.
     *
     * @param value Value to multiply by.
     * @return {@code this * value}.
     * @throws ArithmeticException if the resulting numerator or denominator
     * cannot be represented in an {@code int}.
     */
    @Override
    public Fraction multiply(Fraction value) {
        if (value.isZero() || isZero()) {
            return ZERO;
        }

        // knuth 4.5.1
        // Make sure we don't overflow unless the result *must* overflow.
        final int d1 = ArithmeticUtils.gcd(numerator, value.denominator);
        final int d2 = ArithmeticUtils.gcd(value.numerator, denominator);
        return of(Math.multiplyExact(numerator / d1, value.numerator / d2),
                  Math.multiplyExact(denominator / d2, value.denominator / d1));
    }

    /**
     * Divide this fraction by the passed {@code value}, returning
     * the result in reduced form.
     *
     * @param value Value to divide by
     * @return {@code this / value}.
     * @throws ArithmeticException if the value to divide by is zero
     * or if the resulting numerator or denominator cannot be represented
     * by an {@code int}.
     */
    public Fraction divide(final int value) {
        return divide(of(value));
    }

    /**
     * Divide this fraction by the passed {@code value}, returning
     * the result in reduced form.
     *
     * @param value Value to divide by
     * @return {@code this / value}.
     * @throws ArithmeticException if the value to divide by is zero
     * or if the resulting numerator or denominator cannot be represented
     * by an {@code int}.
     */
    @Override
    public Fraction divide(Fraction value) {
        if (value.isZero()) {
            throw new FractionException(FractionException.ERROR_DIVIDE_BY_ZERO);
        }
        return multiply(value.reciprocal());
    }

    /**
     * Returns a {@code Fraction} whose value is
     * <code>this<sup>exponent</sup></code>, returning the result in reduced form.
     *
     * @param exponent exponent to which this {@code Fraction} is to be raised.
     * @return <code>this<sup>exponent</sup></code>.
     */
    @Override
    public Fraction pow(final int exponent) {
        if (exponent == 0) {
            return ONE;
        }
        if (isZero()) {
            return this;
        }

        if (exponent < 0) {
            return new Fraction(ArithmeticUtils.pow(denominator, -exponent),
                                ArithmeticUtils.pow(numerator,   -exponent));
        }
        return new Fraction(ArithmeticUtils.pow(numerator,   exponent),
                            ArithmeticUtils.pow(denominator, exponent));
    }

    /**
     * Returns the {@code String} representing this fraction.
     * Uses:
     * <ul>
     *  <li>{@code "0"} if {@code numerator} is zero.
     *  <li>{@code "numerator"} if {@code denominator} is one.
     *  <li>{@code "numerator / denominator"} for all other cases.
     * </ul>
     *
     * @return a string representation of the fraction.
     */
    @Override
    public String toString() {
        final String str;
        if (denominator == 1) {
            str = Integer.toString(numerator);
        } else if (isZero()) {
            str = "0";
        } else {
            str = numerator + " / " + denominator;
        }
        return str;
    }

    /**
     * Compares this object with the specified object for order using the signed magnitude.
     *
     * @param other {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public int compareTo(Fraction other) {
        return Long.compare(((long) numerator) * other.denominator,
                            ((long) denominator) * other.numerator);
    }

    /**
     * Test for equality with another object. If the other object is a {@code Fraction} then a
     * comparison is made of the sign and magnitude; otherwise {@code false} is returned.
     *
     * @param other {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof Fraction) {
            // Since fractions are always in lowest terms, numerators and
            // denominators can be compared directly for equality.
            final Fraction rhs = (Fraction) other;
            if (signum() == rhs.signum()) {
                return Math.abs(numerator) == Math.abs(rhs.numerator) &&
                       Math.abs(denominator) == Math.abs(rhs.denominator);
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return 37 * (37 * 17 + numerator) + denominator;
    }

    /**
     * Returns true if this fraction is zero.
     *
     * @return true if zero
     */
    private boolean isZero() {
        return numerator == 0;
    }
}
