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
 */
public final class Fraction
    extends Number
    implements Comparable<Fraction>,
               NativeOperators<Fraction>,
               Serializable {
    /** A fraction representing "1". */
    public static final Fraction ONE = new Fraction(1, 1);
    /** A fraction representing "0". */
    public static final Fraction ZERO = new Fraction(0, 1);
    /** Serializable version identifier. */
    private static final long serialVersionUID = 20190701L;
    /** The default epsilon used for convergence. */
    private static final double DEFAULT_EPSILON = 1e-5;
    /** The denominator of this fraction reduced to lowest terms. */
    private final int denominator;
    /** The numerator of this fraction reduced to lowest terms. */
    private final int numerator;

    /**
     * Create a fraction given the double value and either the maximum error
     * allowed or the maximum number of denominator digits.
     * <p>
     *
     * NOTE: This constructor is called with EITHER
     *   - a valid epsilon value and the maxDenominator set to Integer.MAX_VALUE
     *     (that way the maxDenominator has no effect).
     * OR
     *   - a valid maxDenominator value and the epsilon value set to zero
     *     (that way epsilon only has effect if there is an exact match before
     *     the maxDenominator value is reached).
     * </p><p>
     *
     * It has been done this way so that the same code can be (re)used for both
     * scenarios. However this could be confusing to users if it were part of
     * the public API and this constructor should therefore remain PRIVATE.
     * </p>
     *
     * See JIRA issue ticket MATH-181 for more details:
     *     https://issues.apache.org/jira/browse/MATH-181
     *
     * @param value the double value to convert to a fraction.
     * @param epsilon maximum error allowed.  The resulting fraction is
     * within {@code epsilon} of {@code value}, in absolute terms.
     * @param maxDenominator maximum denominator value allowed.
     * @param maxIterations maximum number of convergents
     * @throws ArithmeticException if the continued fraction failed
     * to converge.
     */
    private Fraction(double value, double epsilon, int maxDenominator, int maxIterations) {
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
     * Constructs an instance.
     *
     * @param num Numerator.
     * @param den Nenominator.
     * @throws ArithmeticException if the denominator is {@code zero}
     * or if integer overflow occurs.
     */
    private Fraction(int num, int den) {
        if (den == 0) {
            throw new ArithmeticException("division by zero");
        }

        if (num == den) {
            numerator = 1;
            denominator = 1;
        } else {
            // If num and den are both 2^-31, or if one is 0 and the other is 2^-31,
            // the calculation of the gcd below will fail. Ensure that this does not
            // happen by dividing both by 2 in case both are even.
            if (((num | den) & 1) == 0) {
                num >>= 1;
                den >>= 1;
            }

            // Reduce numerator and denominator by greatest common divisor.
            final int d = ArithmeticUtils.gcd(num, den);
            if (d > 1) {
                num /= d;
                den /= d;
            }

            numerator = num;
            denominator = den;
        }
    }

    /**
     * Creates an instance.
     *
     * @param value Value to convert to a fraction.
     * @throws ArithmeticException if the continued fraction failed to
     * converge.
     * @return a new instance.
     */
    public static Fraction from(double value) {
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
     * @param value the double value to convert to a fraction.
     * @param epsilon maximum error allowed.  The resulting fraction is within
     * {@code epsilon} of {@code value}, in absolute terms.
     * @param maxIterations maximum number of convergents
     * @throws ArithmeticException if the continued fraction failed to
     * converge.
     * @return a new instance.
     */
    public static Fraction from(double value, double epsilon, int maxIterations) {
        return new Fraction(value, epsilon, Integer.MAX_VALUE, maxIterations);
    }

    /**
     * Creates an instance.
     *
     * <p>
     * References:
     * <ul>
     * <li><a href="http://mathworld.wolfram.com/ContinuedFraction.html">
     * Continued Fraction</a> equations (11) and (22)-(26)</li>
     * </ul>
     *
     * @param value the double value to convert to a fraction.
     * @param maxDenominator The maximum allowed value for denominator
     * @throws ArithmeticException if the continued fraction failed to
     * converge.
     * @return a new instance.
     */
    public static Fraction from(double value, int maxDenominator) {
        return new Fraction(value, 0, maxDenominator, 100);
    }

    /**
     * Creates an instance.
     * The fraction is {@code num / 1}.
     *
     * @param num Numerator.
     * @return a new instance.
     */
    public static Fraction of(int num) {
        return of(num, 1);
    }

    /**
     * Return a fraction given the numerator and denominator.
     * The fraction is reduced to lowest terms.
     *
     * @param num Numerator.
     * @param den Denominator.
     * @throws ArithmeticException if the denominator is {@code zero}
     * or if integer overflow occurs.
     * @return a new instance.
     */
    public static Fraction of(int num, int den) {
        return new Fraction(num, den);
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

    /**
     * Compares this object to another based on size.
     *
     * @param other Object to compare to.
     * @return -1 if this is less than {@code object}, +1 if this is greater
     * than {@code object}, 0 if they are equal.
     */
    @Override
    public int compareTo(Fraction other) {
        return Long.compare(((long) numerator) * other.denominator,
                            ((long) denominator) * other.numerator);
    }

    /**
     * Retrieves the {@code double} value closest to this fraction.
     * This calculates the fraction as numerator divided by denominator.
     *
     * @return the fraction as a {@code double}.
     */
    @Override
    public double doubleValue() {
        return (double) numerator / (double) denominator;
    }

    /**
     * Test for the equality of two fractions.
     * If the lowest term numerator and denominators are the same for
     * both fractions, the two fractions are considered to be equal.
     * @param other Fraction to test for equality with.
     * @return {@code true} if the two fractions are equal, {@code false}
     * otherwise.
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
            } else {
                return false;
            }
        }

        return false;
    }

    /**
     * Retrieves the {@code float} value closest to this fraction.
     * This calculates the fraction as numerator divided by denominator.
     *
     * @return the fraction as {@code float}.
     */
    @Override
    public float floatValue() {
        return (float) doubleValue();
    }

    /**
     * @return the denominator.
     */
    public int getDenominator() {
        return denominator;
    }

    /**
     * @return the numerator.
     */
    public int getNumerator() {
        return numerator;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return 37 * (37 * 17 + numerator) + denominator;
    }

    /**
     * Retrieves the whole number part of the fraction.
     *
     * @return the largest {@code int} value that is not larger than
     * this fraction.
     */
    @Override
    public int intValue() {
        return (int) doubleValue();
    }

    /**
     * Retrieves the whole number part of the fraction.
     *
     * @return the largest {@code long} value that is not larger than
     * this fraction.
     */
    @Override
    public long longValue() {
        return (long) doubleValue();
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
        } else if (numerator == 0) {
            return 0;
        } else {
            return -1;
        }
    }

    /**
     * Computes the additive inverse of this fraction.
     *
     * @return the opposite.
     */
    @Override
    public Fraction negate() {
        return numerator == Integer.MIN_VALUE ?
            new Fraction(numerator, -denominator) :
            new Fraction(-numerator, denominator);
    }

    /**
     * Computes the multiplicative inverse of this fraction.
     *
     * @return the reciprocal.
     */
    @Override
    public Fraction reciprocal() {
        return new Fraction(denominator, numerator);
    }

    /**
     * Adds the value of this fraction to another, returning the result
     * in reduced form.
     * The algorithm follows Knuth, 4.5.1.
     *
     * @param fraction Fraction to add.
     * @return a new instance.
     * @throws ArithmeticException if the resulting numerator or denominator
     * cannot be represented in an {@code int}.
     */
    @Override
    public Fraction add(Fraction fraction) {
        return addSub(fraction, true /* add */);
    }

    /**
     * Adds an integer to the fraction.
     *
     * @param i Value to add.
     * @return {@code this + i}.
     */
    public Fraction add(final int i) {
        return new Fraction(numerator + i * denominator, denominator);
    }

    /**
     * Subtracts the value of another fraction from the value of this one,
     * returning the result in reduced form.
     *
     * @param fraction Fraction to subtract.
     * @return a new instance.
     * @throws ArithmeticException if the resulting numerator or denominator
     * cannot be represented in an {@code int}.
     */
    @Override
    public Fraction subtract(Fraction fraction) {
        return addSub(fraction, false /* subtract */);
    }

    /**
     * Subtracts an integer from this fraction.
     *
     * @param i Value to subtract.
     * @return {@code this - i}.
     */
    public Fraction subtract(final int i) {
        return new Fraction(numerator - i * denominator, denominator);
    }

    /**
     * Implements add and subtract using algorithm described in Knuth 4.5.1.
     *
     * @param fraction Fraction to add or subtract.
     * @param isAdd Whether the operation is "add" or "subtract".
     * @return a new instance.
     * @throws ArithmeticException if the resulting numerator or denominator
     * cannot be represented in an {@code int}.
     */
    private Fraction addSub(Fraction fraction, boolean isAdd) {
        // Zero is identity for addition.
        if (numerator == 0) {
            return isAdd ? fraction : fraction.negate();
        }

        if (fraction.numerator == 0) {
            return this;
        }

        /*
         * Let the two fractions be u/u' and v/v', and d1 = gcd(u', v').
         * First, compute t, defined as:
         *
         * t = u(v'/d1) +/- v(u'/d1)
         */
        final int d1 = ArithmeticUtils.gcd(denominator, fraction.denominator);
        final long uvp = (long) numerator * (long) (fraction.denominator / d1);
        final long upv = (long) fraction.numerator * (long) (denominator / d1);

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
                                     fraction.denominator / (int) d2));
    }

    /**
     * Multiplies the value of this fraction by another, returning the
     * result in reduced form.
     *
     * @param fraction Fraction to multiply by.
     * @return a new instance.
     * @throws ArithmeticException if the resulting numerator or denominator
     * cannot be represented in an {@code int}.
     */
    @Override
    public Fraction multiply(Fraction fraction) {
        if (numerator == 0 ||
            fraction.numerator == 0) {
            return ZERO;
        }

        // knuth 4.5.1
        // Make sure we don't overflow unless the result *must* overflow.
        final int d1 = ArithmeticUtils.gcd(numerator, fraction.denominator);
        final int d2 = ArithmeticUtils.gcd(fraction.numerator, denominator);
        return of(Math.multiplyExact(numerator / d1, fraction.numerator / d2),
                  Math.multiplyExact(denominator / d2, fraction.denominator / d1));
    }

    /**
     * Multiplies the fraction by an integer.
     *
     * @param i Value to multiply by.
     * @return {@code this * i}.
     */
    @Override
    public Fraction multiply(final int i) {
        return multiply(of(i));
    }

    /**
     * Divides the value of this fraction by another.
     *
     * @param fraction Fraction to divide by.
     * @return a new instance.
     * @throws ArithmeticException if the fraction to divide by is zero
     * or if the resulting numerator or denominator cannot be represented
     * by an {@code int}.
     */
    @Override
    public Fraction divide(Fraction fraction) {
        if (fraction.numerator == 0) {
            throw new FractionException("the fraction to divide by must not be zero: {0}/{1}",
                                        fraction.numerator, fraction.denominator);
        }

        return multiply(fraction.reciprocal());
    }

    /**
     * Divides the fraction by an integer.
     *
     * @param i Value to divide by.
     * @return {@code this * i}.
     */
    public Fraction divide(final int i) {
        return divide(of(i));
    }

    /**
     * @param n Power.
     * @return <code>this<sup>n</sup></code>.
     */
    @Override
    public Fraction pow(final int n) {
        if (n == 0) {
            return ONE;
        }
        if (numerator == 0) {
            return this;
        }

        return n < 0 ?
            new Fraction(ArithmeticUtils.pow(denominator, -n),
                         ArithmeticUtils.pow(numerator, -n)) :
            new Fraction(ArithmeticUtils.pow(numerator, n),
                         ArithmeticUtils.pow(denominator, n));
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final String str;
        if (denominator == 1) {
            str = Integer.toString(numerator);
        } else if (numerator == 0) {
            str = "0";
        } else {
            str = numerator + " / " + denominator;
        }
        return str;
    }

    /** {@inheritDoc} */
    @Override
    public Fraction zero() {
        return ZERO;
    }

    /** {@inheritDoc} */
    @Override
    public Fraction one() {
        return ONE;
    }

    /**
     * Parses a string that would be produced by {@link #toString()}
     * and instantiates the corresponding object.
     *
     * @param s String representation.
     * @return an instance.
     * @throws NumberFormatException if the string does not conform to the
     * specification.
     */
    public static Fraction parse(String s) {
        final int slashLoc = s.indexOf('/');
        // if no slash, parse as single number
        if (slashLoc == -1) {
            return Fraction.of(Integer.parseInt(s.trim()));
        } else {
            final int num = Integer.parseInt(s.substring(0, slashLoc).trim());
            final int denom = Integer.parseInt(s.substring(slashLoc + 1).trim());
            return of(num, denom);
        }
    }
}
