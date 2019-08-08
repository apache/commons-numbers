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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import org.apache.commons.numbers.core.ArithmeticUtils;

/**
 * Representation of a rational number without any overflow. This class is
 * immutable.
 */
public class BigFraction extends Number implements Comparable<BigFraction>, Serializable {
    /** A fraction representing "0". */
    public static final BigFraction ZERO = of(0);

    /** A fraction representing "1". */
    public static final BigFraction ONE = of(1);

    /** Serializable version identifier. */
    private static final long serialVersionUID = 20190701L;

    /** Parameter name for fraction (to satisfy checkstyle). */
    private static final String PARAM_NAME_FRACTION = "fraction";

    /** Parameter name for BigIntegers (to satisfy checkstyle). */
    private static final String PARAM_NAME_BG = "bg";

    /**
     * The numerator of this fraction reduced to lowest terms. Negative if this
     * fraction's value is negative.
     */
    private final BigInteger numerator;

    /** The denominator of this fraction reduced to lowest terms. Always positive. */
    private final BigInteger denominator;

    /**
     * Private constructor: Instances are created using factory methods.
     *
     * @param num Numerator, must not be {@code null}.
     * @param den Denominator, must not be {@code null}.
     * @throws ArithmeticException if the denominator is zero.
     */
    private BigFraction(BigInteger num, BigInteger den) {
        checkNotNull(num, "numerator");
        checkNotNull(den, "denominator");
        if (den.signum() == 0) {
            throw new FractionException(FractionException.ERROR_ZERO_DENOMINATOR);
        }
        if (num.signum() == 0) {
            numerator   = BigInteger.ZERO;
            denominator = BigInteger.ONE;
        } else {

            // reduce numerator and denominator by greatest common denominator
            final BigInteger gcd = num.gcd(den);
            if (BigInteger.ONE.compareTo(gcd) < 0) {
                num = num.divide(gcd);
                den = den.divide(gcd);
            }

            // move sign to numerator
            if (den.signum() == -1) {
                num = num.negate();
                den = den.negate();
            }

            // store the values in the final fields
            numerator   = num;
            denominator = den;
        }
    }

    /**
     * Create a fraction given the double value and either the maximum
     * error allowed or the maximum number of denominator digits.
     *
     * <p>
     * NOTE: This method is called with
     *  - EITHER a valid epsilon value and the maxDenominator set to
     *    Integer.MAX_VALUE (that way the maxDenominator has no effect)
     *  - OR a valid maxDenominator value and the epsilon value set to
     *    zero (that way epsilon only has effect if there is an exact
     *    match before the maxDenominator value is reached).
     * </p>
     * <p>
     * It has been done this way so that the same code can be reused for
     * both scenarios.  However this could be confusing to users if it
     * were part of the public API and this method should therefore remain
     * PRIVATE.
     * </p>
     *
     * See JIRA issue ticket MATH-181 for more details:
     *   https://issues.apache.org/jira/browse/MATH-181
     *
     * @param value Value to convert to a fraction.
     * @param epsilon Maximum error allowed.
     * The resulting fraction is within {@code epsilon} of {@code value},
     * in absolute terms.
     * @param maxDenominator Maximum denominator value allowed.
     * @param maxIterations Maximum number of convergents.
     * @return a new instance.
     * @throws ArithmeticException if the continued fraction failed to converge.
     */
    private static BigFraction from(final double value,
                                    final double epsilon,
                                    final int maxDenominator,
                                    final int maxIterations) {
        long overflow = Integer.MAX_VALUE;
        double r0 = value;
        long a0 = (long) Math.floor(r0);

        if (Math.abs(a0) > overflow) {
            throw new FractionException(FractionException.ERROR_CONVERSION_OVERFLOW, value, a0, 1l);
        }

        // check for (almost) integer arguments, which should not go
        // to iterations.
        if (Math.abs(a0 - value) < epsilon) {
            return new BigFraction(BigInteger.valueOf(a0),
                                   BigInteger.ONE);
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
            final double r1 = 1d / (r0 - a0);
            final long a1 = (long) Math.floor(r1);
            p2 = (a1 * p1) + p0;
            q2 = (a1 * q1) + q0;
            if (p2 > overflow ||
                q2 > overflow) {
                // in maxDenominator mode, if the last fraction was very close to the actual value
                // q2 may overflow in the next iteration; in this case return the last one.
                if (epsilon == 0 &&
                    Math.abs(q1) < maxDenominator) {
                    break;
                }
                throw new FractionException(FractionException.ERROR_CONVERSION_OVERFLOW, value, p2, q2);
            }

            final double convergent = (double) p2 / (double) q2;
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

        return q2 < maxDenominator ?
            new BigFraction(BigInteger.valueOf(p2),
                            BigInteger.valueOf(q2)) :
            new BigFraction(BigInteger.valueOf(p1),
                            BigInteger.valueOf(q1));
    }

    /**
     * <p>
     * Create a {@link BigFraction} equivalent to the passed {@code BigInteger}, ie
     * "num / 1".
     * </p>
     *
     * @param num the numerator.
     * @return a new instance.
     */
    public static BigFraction of(final BigInteger num) {
        return new BigFraction(num, BigInteger.ONE);
    }

    /**
     * Create a {@link BigFraction} given the numerator and denominator as
     * {@code BigInteger}. The {@link BigFraction} is reduced to lowest terms.
     *
     * @param num the numerator, must not be {@code null}.
     * @param den the denominator, must not be {@code null}.
     * @throws ArithmeticException if the denominator is zero.
     * @return a new instance.
     */
    public static BigFraction of(BigInteger num, BigInteger den) {
        return new BigFraction(num, den);
    }

    /**
     * Create a fraction given the double value.
     * <p>
     * This factory method behaves <em>differently</em> from
     * {@link #from(double, double, int)}. It converts the double value
     * exactly, considering its internal bits representation. This works for all
     * values except NaN and infinities and does not requires any loop or
     * convergence threshold.
     * </p>
     * <p>
     * Since this conversion is exact and since double numbers are sometimes
     * approximated, the fraction created may seem strange in some cases. For example,
     * calling {@code from(1.0 / 3.0)} does <em>not</em> create
     * the fraction \( \frac{1}{3} \), but the fraction \( \frac{6004799503160661}{18014398509481984} \)
     * because the double number passed to the method is not exactly \( \frac{1}{3} \)
     * (which cannot be represented exactly in IEEE754).
     * </p>
     *
     * @param value Value to convert to a fraction.
     * @throws IllegalArgumentException if the given {@code value} is NaN or infinite.
     * @return a new instance.
     *
     * @see #from(double,double,int)
     */
    public static BigFraction from(final double value) {
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException("Cannot convert NaN value");
        }
        if (Double.isInfinite(value)) {
            throw new IllegalArgumentException("Cannot convert infinite value");
        }

        final long bits = Double.doubleToLongBits(value);
        final long sign = bits & 0x8000000000000000L;
        final long exponent = bits & 0x7ff0000000000000L;
        final long mantissa = bits & 0x000fffffffffffffL;

        // Compute m and k such that value = m * 2^k
        long m;
        int k;

        if (exponent != 0) {
            // Normalized number: Add the implicit most significant bit.
            m = mantissa | 0x0010000000000000L;
            k = ((int) (exponent >> 52)) - 1075; // Exponent bias is 1023.
        } else {
            m = mantissa;
            k = 0; // For simplicity, when number is 0.
            if (m != 0) {
                // Subnormal number, the effective exponent bias is 1022, not 1023.
                k = -1074;
            }
        }
        if (sign != 0) {
            m = -m;
        }
        while ((m & 0x001ffffffffffffeL) != 0 &&
               (m & 0x1) == 0) {
            m >>= 1;
            ++k;
        }

        return k < 0 ?
            new BigFraction(BigInteger.valueOf(m),
                            BigInteger.ZERO.flipBit(-k)) :
            new BigFraction(BigInteger.valueOf(m).multiply(BigInteger.ZERO.flipBit(k)),
                            BigInteger.ONE);
    }

    /**
     * Create a fraction given the double value and maximum error allowed.
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
     * @throws ArithmeticException if the continued fraction failed to converge.
     * @return a new instance.
     *
     * @see #from(double,int)
     */
    public static BigFraction from(final double value,
                                   final double epsilon,
                                   final int maxIterations) {
        return from(value, epsilon, Integer.MAX_VALUE, maxIterations);
    }

    /**
     * Create a fraction given the double value and maximum denominator.
     * <p>
     * References:
     * <ul>
     * <li><a href="http://mathworld.wolfram.com/ContinuedFraction.html">
     * Continued Fraction</a> equations (11) and (22)-(26)</li>
     * </ul>
     *
     * @param value Value to convert to a fraction.
     * @param maxDenominator Maximum allowed value for denominator.
     * @throws ArithmeticException if the continued fraction failed to converge.
     * @return a new instance.
     *
     * @see #from(double,double,int)
     */
    public static BigFraction from(final double value,
                                   final int maxDenominator) {
        return from(value, 0, maxDenominator, 100);
    }

    /**
     * <p>
     * Create a {@link BigFraction} equivalent to the passed {@code int}, ie
     * "num / 1".
     * </p>
     *
     * @param num
     *            the numerator.
     * @return a new instance.
     */
    public static BigFraction of(final int num) {
        return new BigFraction(BigInteger.valueOf(num), BigInteger.ONE);
    }

    /**
     * <p>
     * Create a {@link BigFraction} given the numerator and denominator as simple
     * {@code int}. The {@link BigFraction} is reduced to lowest terms.
     * </p>
     *
     * @param num the numerator.
     * @param den the denominator.
     * @return a new instance.
     */
    public static BigFraction of(final int num, final int den) {
        return new BigFraction(BigInteger.valueOf(num), BigInteger.valueOf(den));
    }

    /**
     * <p>
     * Create a {@link BigFraction} equivalent to the passed long, ie "num / 1".
     * </p>
     *
     * @param num the numerator.
     * @return a new instance.
     */
    public static BigFraction of(final long num) {
        return new BigFraction(BigInteger.valueOf(num), BigInteger.ONE);
    }

    /**
     * <p>
     * Create a {@link BigFraction} given the numerator and denominator as simple
     * {@code long}. The {@link BigFraction} is reduced to lowest terms.
     * </p>
     *
     * @param num the numerator.
     * @param den the denominator.
     * @return a new instance.
     */
    public static BigFraction of(final long num, final long den) {
        return new BigFraction(BigInteger.valueOf(num), BigInteger.valueOf(den));
    }

    /**
     * <p>
     * Creates a <code>BigFraction</code> instance with the 2 parts of a fraction
     * Y/Z.
     * </p>
     *
     * <p>
     * Any negative signs are resolved to be on the numerator.
     * </p>
     *
     * @param numerator
     *            the numerator, for example the three in 'three sevenths'.
     * @param denominator
     *            the denominator, for example the seven in 'three sevenths'.
     * @return a new fraction instance, with the numerator and denominator
     *         reduced.
     * @throws ArithmeticException
     *             if the denominator is <code>zero</code>.
     */
    public static BigFraction getReducedFraction(final int numerator,
                                                 final int denominator) {
        if (numerator == 0) {
            return ZERO; // normalize zero.
        }

        return of(numerator, denominator);
    }

    /**
     * <p>
     * Returns the absolute value of this {@link BigFraction}.
     * </p>
     *
     * @return the absolute value as a {@link BigFraction}.
     */
    public BigFraction abs() {
        return (numerator.signum() == 1) ? this : negate();
    }

    /**
     * <p>
     * Adds the value of this fraction to the passed {@link BigInteger},
     * returning the result in reduced form.
     * </p>
     *
     * @param bg
     *            the {@link BigInteger} to add, must'nt be <code>null</code>.
     * @return a <code>BigFraction</code> instance with the resulting values.
     */
    public BigFraction add(final BigInteger bg) {
        checkNotNull(bg, PARAM_NAME_BG);

        if (numerator.signum() == 0) {
            return of(bg);
        }
        if (bg.signum() == 0) {
            return this;
        }

        return new BigFraction(numerator.add(denominator.multiply(bg)), denominator);
    }

    /**
     * <p>
     * Adds the value of this fraction to the passed {@code integer}, returning
     * the result in reduced form.
     * </p>
     *
     * @param i
     *            the {@code integer} to add.
     * @return a <code>BigFraction</code> instance with the resulting values.
     */
    public BigFraction add(final int i) {
        return add(BigInteger.valueOf(i));
    }

    /**
     * <p>
     * Adds the value of this fraction to the passed {@code long}, returning
     * the result in reduced form.
     * </p>
     *
     * @param l
     *            the {@code long} to add.
     * @return a <code>BigFraction</code> instance with the resulting values.
     */
    public BigFraction add(final long l) {
        return add(BigInteger.valueOf(l));
    }

    /**
     * <p>
     * Adds the value of this fraction to another, returning the result in
     * reduced form.
     * </p>
     *
     * @param fraction
     *            the {@link BigFraction} to add, must not be <code>null</code>.
     * @return a {@link BigFraction} instance with the resulting values.
     */
    public BigFraction add(final BigFraction fraction) {
        checkNotNull(fraction, PARAM_NAME_FRACTION);
        if (fraction.numerator.signum() == 0) {
            return this;
        }
        if (numerator.signum() == 0) {
            return fraction;
        }

        final BigInteger num;
        final BigInteger den;

        if (denominator.equals(fraction.denominator)) {
            num = numerator.add(fraction.numerator);
            den = denominator;
        } else {
            num = (numerator.multiply(fraction.denominator)).add((fraction.numerator).multiply(denominator));
            den = denominator.multiply(fraction.denominator);
        }

        if (num.signum() == 0) {
            return ZERO;
        }

        return new BigFraction(num, den);

    }

    /**
     * <p>
     * Gets the fraction as a <code>BigDecimal</code>. This calculates the
     * fraction as the numerator divided by denominator.
     * </p>
     *
     * @return the fraction as a <code>BigDecimal</code>.
     * @throws ArithmeticException
     *             if the exact quotient does not have a terminating decimal
     *             expansion.
     * @see BigDecimal
     */
    public BigDecimal bigDecimalValue() {
        return new BigDecimal(numerator).divide(new BigDecimal(denominator));
    }

    /**
     * <p>
     * Gets the fraction as a <code>BigDecimal</code> following the passed
     * rounding mode. This calculates the fraction as the numerator divided by
     * denominator.
     * </p>
     *
     * @param roundingMode Rounding mode to apply.
     * @return the fraction as a <code>BigDecimal</code>.
     * @see BigDecimal
     */
    public BigDecimal bigDecimalValue(RoundingMode roundingMode) {
        return new BigDecimal(numerator).divide(new BigDecimal(denominator), roundingMode);
    }

    /**
     * <p>
     * Gets the fraction as a <code>BigDecimal</code> following the passed scale
     * and rounding mode. This calculates the fraction as the numerator divided
     * by denominator.
     * </p>
     *
     * @param scale
     *            scale of the <code>BigDecimal</code> quotient to be returned.
     *            see {@link BigDecimal} for more information.
     * @param roundingMode Rounding mode to apply.
     * @return the fraction as a <code>BigDecimal</code>.
     * @see BigDecimal
     */
    public BigDecimal bigDecimalValue(final int scale, RoundingMode roundingMode) {
        return new BigDecimal(numerator).divide(new BigDecimal(denominator), scale, roundingMode);
    }

    /**
     * <p>
     * Compares this object to another based on size.
     * </p>
     *
     * @param object
     *            the object to compare to, must not be <code>null</code>.
     * @return -1 if this is less than {@code object}, +1 if this is greater
     *         than {@code object}, 0 if they are equal.
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final BigFraction object) {
        int lhsSigNum = numerator.signum();
        int rhsSigNum = object.numerator.signum();

        if (lhsSigNum != rhsSigNum) {
            return (lhsSigNum > rhsSigNum) ? 1 : -1;
        }
        if (lhsSigNum == 0) {
            return 0;
        }

        BigInteger nOd = numerator.multiply(object.denominator);
        BigInteger dOn = denominator.multiply(object.numerator);
        return nOd.compareTo(dOn);
    }

    /**
     * <p>
     * Divide the value of this fraction by the passed {@code BigInteger},
     * ie {@code this * 1 / bg}, returning the result in reduced form.
     * </p>
     *
     * @param bg the {@code BigInteger} to divide by, must not be {@code null}
     * @return a {@link BigFraction} instance with the resulting values
     * @throws ArithmeticException if the fraction to divide by is zero
     */
    public BigFraction divide(final BigInteger bg) {
        checkNotNull(bg, PARAM_NAME_BG);
        if (bg.signum() == 0) {
            throw new FractionException(FractionException.ERROR_ZERO_DENOMINATOR);
        }
        if (numerator.signum() == 0) {
            return ZERO;
        }
        return new BigFraction(numerator, denominator.multiply(bg));
    }

    /**
     * <p>
     * Divide the value of this fraction by the passed {@code int}, ie
     * {@code this * 1 / i}, returning the result in reduced form.
     * </p>
     *
     * @param i the {@code int} to divide by
     * @return a {@link BigFraction} instance with the resulting values
     * @throws ArithmeticException if the fraction to divide by is zero
     */
    public BigFraction divide(final int i) {
        return divide(BigInteger.valueOf(i));
    }

    /**
     * <p>
     * Divide the value of this fraction by the passed {@code long}, ie
     * {@code this * 1 / l}, returning the result in reduced form.
     * </p>
     *
     * @param l the {@code long} to divide by
     * @return a {@link BigFraction} instance with the resulting values
     * @throws ArithmeticException if the fraction to divide by is zero
     */
    public BigFraction divide(final long l) {
        return divide(BigInteger.valueOf(l));
    }

    /**
     * <p>
     * Divide the value of this fraction by another, returning the result in
     * reduced form.
     * </p>
     *
     * @param fraction Fraction to divide by, must not be {@code null}.
     * @return a {@link BigFraction} instance with the resulting values.
     * @throws ArithmeticException if the fraction to divide by is zero
     */
    public BigFraction divide(final BigFraction fraction) {
        checkNotNull(fraction, PARAM_NAME_FRACTION);
        if (fraction.numerator.signum() == 0) {
            throw new FractionException(FractionException.ERROR_ZERO_DENOMINATOR);
        }
        if (numerator.signum() == 0) {
            return ZERO;
        }

        return multiply(fraction.reciprocal());
    }

    /**
     * Calculates the sign bit, the biased exponent and the significand for a
     * binary floating-point representation of this {@code BigFraction}
     * according to the IEEE 754 standard, and encodes these values into a {@code long}
     * variable. The representative bits are arranged adjacent to each other and
     * placed at the low-order end of the returned {@code long} value, with the
     * least significant bits used for the significand, the next more
     * significant bits for the exponent, and next more significant bit for the
     * sign.
     * @param exponentLength the number of bits allowed for the exponent; must be
     *                       between 1 and 32 (inclusive), and must not be greater
     *                       than {@code 63 - significandLength}
     * @param significandLength the number of bits allowed for the significand
     *                          (excluding the implicit leading 1-bit in
     *                          normalized numbers, e.g. 52 for a double-precision
     *                          floating-point number); must be between 1 and
     *                          {@code 63 - exponentLength} (inclusive)
     * @return the bits of an IEEE 754 binary floating-point representation of
     * this fraction encoded in a {@code long}, as described above.
     * @throws IllegalArgumentException if the arguments do not meet the
     *         criteria described above
     */
    private long toFloatingPointBits(int exponentLength, int significandLength) {
        if (exponentLength < 1 ||
            significandLength < 1 ||
            exponentLength > Math.min(32, 63 - significandLength)) {
            throw new IllegalArgumentException("exponent length: " + exponentLength +
                                               "; significand length: " + significandLength);
        }
        if (numerator.signum() == 0) {
            return 0L;
        }

        final long sign = numerator.signum() == -1 ? 1L : 0L;
        final BigInteger positiveNumerator = numerator.abs();

        /*
         * The most significant 1-bit of a non-zero number is not explicitly
         * stored in the significand of an IEEE 754 normalized binary
         * floating-point number, so we need to round the value of this fraction
         * to (significandLength + 1) bits. In order to do this, we calculate
         * the most significant (significandLength + 2) bits, and then, based on
         * the least significant of those bits, find out whether we need to
         * round up or down.
         *
         * First, we'll remove all powers of 2 from the denominator because they
         * are not relevant for the significand of the prospective binary
         * floating-point value.
         */
        final int denRightShift = denominator.getLowestSetBit();
        final BigInteger divisor = denominator.shiftRight(denRightShift);

        /*
         * Now, we're going to calculate the (significandLength + 2) most
         * significant bits of the fraction's value using integer division. To
         * guarantee that the quotient of the division has at least
         * (significandLength + 2) bits, the bit length of the dividend must
         * exceed that of the divisor by at least that amount.
         *
         * If the denominator has prime factors other than 2, i.e. if the
         * divisor was not reduced to 1, an excess of exactly
         * (significandLength + 2) bits is sufficient, because the knowledge
         * that the fractional part of the precise quotient's binary
         * representation does not terminate is enough information to resolve
         * cases where the most significant (significandLength + 2) bits alone
         * are not conclusive.
         *
         * Otherwise, the quotient must be calculated exactly and the bit length
         * of the numerator can only be reduced as long as no precision is lost
         * in the process (meaning it can have powers of 2 removed, like the
         * denominator).
         */
        int numRightShift = positiveNumerator.bitLength() - divisor.bitLength() - (significandLength + 2);
        if (numRightShift > 0 &&
            divisor.equals(BigInteger.ONE)) {
            numRightShift = Math.min(numRightShift, positiveNumerator.getLowestSetBit());
        }
        final BigInteger dividend = positiveNumerator.shiftRight(numRightShift);

        final BigInteger quotient = dividend.divide(divisor);

        int quotRightShift = quotient.bitLength() - (significandLength + 1);
        long significand = roundAndRightShift(
                quotient,
                quotRightShift,
                !divisor.equals(BigInteger.ONE)
        ).longValue();

        /*
         * If the significand had to be rounded up, this could have caused the
         * bit length of the significand to increase by one.
         */
        if ((significand & (1L << (significandLength + 1))) != 0) {
            significand >>= 1;
            quotRightShift++;
        }

        /*
         * Now comes the exponent. The absolute value of this fraction based on
         * the current local variables is:
         *
         * significand * 2^(numRightShift - denRightShift + quotRightShift)
         *
         * To get the unbiased exponent for the floating-point value, we need to
         * add (significandLength) to the above exponent, because all but the
         * most significant bit of the significand will be treated as a
         * fractional part. To convert the unbiased exponent to a biased
         * exponent, we also need to add the exponent bias.
         */
        final int exponentBias = (1 << (exponentLength - 1)) - 1;
        long exponent = numRightShift - denRightShift + quotRightShift + significandLength + exponentBias;
        final long maxExponent = (1L << exponentLength) - 1L; //special exponent for infinities and NaN

        if (exponent >= maxExponent) { //infinity
            exponent = maxExponent;
            significand = 0L;
        } else if (exponent > 0) { //normalized number
            significand &= -1L >>> (64 - significandLength); //remove implicit leading 1-bit
        } else { //smaller than the smallest normalized number
            /*
             * We need to round the quotient to fewer than
             * (significandLength + 1) bits. This must be done with the original
             * quotient and not with the current significand, because the loss
             * of precision in the previous rounding might cause a rounding of
             * the current significand's value to produce a different result
             * than a rounding of the original quotient.
             *
             * So we find out how many high-order bits from the quotient we can
             * transfer into the significand. The absolute value of the fraction
             * is:
             *
             * quotient * 2^(numRightShift - denRightShift)
             *
             * To get the significand, we need to right shift the quotient so
             * that the above exponent becomes (1 - exponentBias - significandLength)
             * (the unbiased exponent of a subnormal floating-point number is
             * defined as equivalent to the minimum unbiased exponent of a
             * normalized floating-point number, and (- significandLength)
             * because the significand will be treated as the fractional part).
             */
            significand = roundAndRightShift(quotient,
                                             (1 - exponentBias - significandLength) - (numRightShift - denRightShift),
                                             !divisor.equals(BigInteger.ONE)).longValue();
            exponent = 0L;

            /*
             * Note: It is possible that an otherwise subnormal number will
             * round up to the smallest normal number. However, this special
             * case does not need to be treated separately, because the
             * overflowing highest-order bit of the significand will then simply
             * become the lowest-order bit of the exponent, increasing the
             * exponent from 0 to 1 and thus establishing the implicity of the
             * leading 1-bit.
             */
        }

        return (sign << (significandLength + exponentLength)) |
            (exponent << significandLength) |
            significand;
    }

    /**
     * <p>
     * Gets the fraction as a {@code double}. This calculates the fraction as
     * the numerator divided by denominator.
     * </p>
     *
     * @return the fraction as a {@code double}
     * @see java.lang.Number#doubleValue()
     */
    @Override
    public double doubleValue() {
        return Double.longBitsToDouble(toFloatingPointBits(11, 52));
    }

    /**
     * Rounds an integer to the specified power of two (i.e. the minimum number of
     * low-order bits that must be zero) and performs a right-shift by this
     * amount. The rounding mode applied is round to nearest, with ties rounding
     * to even (meaning the prospective least significant bit must be zero). The
     * number can optionally be treated as though it contained at
     * least one 0-bit and one 1-bit in its fractional part, to influence the result in cases
     * that would otherwise be a tie.
     * @param value the number to round and right-shift
     * @param bits the power of two to which to round; must be positive
     * @param hasFractionalBits whether the number should be treated as though
     *                          it contained a non-zero fractional part
     * @return a {@code BigInteger} as described above
     * @throws IllegalArgumentException if {@code bits <= 0}
     */
    private static BigInteger roundAndRightShift(BigInteger value, int bits, boolean hasFractionalBits) {
        if (bits <= 0) {
            throw new IllegalArgumentException("bits: " + bits);
        }

        BigInteger result = value.shiftRight(bits);
        if (value.testBit(bits - 1) &&
            (hasFractionalBits ||
             (value.getLowestSetBit() < bits - 1) ||
             value.testBit(bits))) {
            result = result.add(BigInteger.ONE); //round up
        }

        return result;
    }

    /**
     * <p>
     * Test for the equality of two fractions. If the lowest term numerator and
     * denominators are the same for both fractions, the two fractions are
     * considered to be equal.
     * </p>
     *
     * @param other
     *            fraction to test for equality to this fraction, can be
     *            <code>null</code>.
     * @return true if two fractions are equal, false if object is
     *         <code>null</code>, not an instance of {@link BigFraction}, or not
     *         equal to this fraction instance.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object other) {
        boolean ret = false;

        if (this == other) {
            ret = true;
        } else if (other instanceof BigFraction) {
            BigFraction rhs = (BigFraction) other;
            ret = numerator.equals(rhs.numerator) && denominator.equals(rhs.denominator);
        }

        return ret;
    }

    /**
     * <p>
     * Gets the fraction as a {@code float}. This calculates the fraction as
     * the numerator divided by denominator.
     * </p>
     *
     * @return the fraction as a {@code float}.
     * @see java.lang.Number#floatValue()
     */
    @Override
    public float floatValue() {
        return Float.intBitsToFloat((int) toFloatingPointBits(8, 23));
    }

    /**
     * <p>
     * Access the denominator as a <code>BigInteger</code>.
     * </p>
     *
     * @return the denominator as a <code>BigInteger</code>.
     */
    public BigInteger getDenominator() {
        return denominator;
    }

    /**
     * <p>
     * Access the denominator as a {@code int}.
     * </p>
     *
     * @return the denominator as a {@code int}.
     */
    public int getDenominatorAsInt() {
        return denominator.intValue();
    }

    /**
     * <p>
     * Access the denominator as a {@code long}.
     * </p>
     *
     * @return the denominator as a {@code long}.
     */
    public long getDenominatorAsLong() {
        return denominator.longValue();
    }

    /**
     * <p>
     * Access the numerator as a <code>BigInteger</code>.
     * </p>
     *
     * @return the numerator as a <code>BigInteger</code>.
     */
    public BigInteger getNumerator() {
        return numerator;
    }

    /**
     * <p>
     * Access the numerator as a {@code int}.
     * </p>
     *
     * @return the numerator as a {@code int}.
     */
    public int getNumeratorAsInt() {
        return numerator.intValue();
    }

    /**
     * <p>
     * Access the numerator as a {@code long}.
     * </p>
     *
     * @return the numerator as a {@code long}.
     */
    public long getNumeratorAsLong() {
        return numerator.longValue();
    }

    /**
     * <p>
     * Gets a hashCode for the fraction.
     * </p>
     *
     * @return a hash code value for this object.
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return 37 * (37 * 17 + numerator.hashCode()) + denominator.hashCode();
    }

    /**
     * <p>
     * Gets the fraction as an {@code int}. This returns the whole number part
     * of the fraction.
     * </p>
     *
     * @return the whole number fraction part.
     * @see java.lang.Number#intValue()
     */
    @Override
    public int intValue() {
        return numerator.divide(denominator).intValue();
    }

    /**
     * <p>
     * Gets the fraction as a {@code long}. This returns the whole number part
     * of the fraction.
     * </p>
     *
     * @return the whole number fraction part.
     * @see java.lang.Number#longValue()
     */
    @Override
    public long longValue() {
        return numerator.divide(denominator).longValue();
    }

    /**
     * <p>
     * Multiplies the value of this fraction by the passed
     * <code>BigInteger</code>, returning the result in reduced form.
     * </p>
     *
     * @param bg the {@code BigInteger} to multiply by.
     * @return a {@code BigFraction} instance with the resulting values.
     */
    public BigFraction multiply(final BigInteger bg) {
        checkNotNull(bg, PARAM_NAME_BG);
        if (numerator.signum() == 0 || bg.signum() == 0) {
            return ZERO;
        }
        return new BigFraction(bg.multiply(numerator), denominator);
    }

    /**
     * <p>
     * Multiply the value of this fraction by the passed {@code int}, returning
     * the result in reduced form.
     * </p>
     *
     * @param i
     *            the {@code int} to multiply by.
     * @return a {@link BigFraction} instance with the resulting values.
     */
    public BigFraction multiply(final int i) {
        if (i == 0 || numerator.signum() == 0) {
            return ZERO;
        }

        return multiply(BigInteger.valueOf(i));
    }

    /**
     * <p>
     * Multiply the value of this fraction by the passed {@code long},
     * returning the result in reduced form.
     * </p>
     *
     * @param l
     *            the {@code long} to multiply by.
     * @return a {@link BigFraction} instance with the resulting values.
     */
    public BigFraction multiply(final long l) {
        if (l == 0 || numerator.signum() == 0) {
            return ZERO;
        }

        return multiply(BigInteger.valueOf(l));
    }

    /**
     * <p>
     * Multiplies the value of this fraction by another, returning the result in
     * reduced form.
     * </p>
     *
     * @param fraction Fraction to multiply by, must not be {@code null}.
     * @return a {@link BigFraction} instance with the resulting values.
     */
    public BigFraction multiply(final BigFraction fraction) {
        checkNotNull(fraction, PARAM_NAME_FRACTION);
        if (numerator.signum() == 0 ||
            fraction.numerator.signum() == 0) {
            return ZERO;
        }
        return new BigFraction(numerator.multiply(fraction.numerator),
                               denominator.multiply(fraction.denominator));
    }

    /**
     * <p>
     * Return the additive inverse of this fraction, returning the result in
     * reduced form.
     * </p>
     *
     * @return the negation of this fraction.
     */
    public BigFraction negate() {
        return new BigFraction(numerator.negate(), denominator);
    }

    /**
     * <p>
     * Returns a {@code BigFraction} whose value is
     * {@code (this<sup>exponent</sup>)}, returning the result in reduced form.
     * </p>
     *
     * @param exponent
     *            exponent to which this {@code BigFraction} is to be
     *            raised.
     * @return \(\mathit{this}^{\mathit{exponent}}\).
     */
    public BigFraction pow(final int exponent) {
        if (exponent == 0) {
            return ONE;
        }
        if (numerator.signum() == 0) {
            return this;
        }

        if (exponent < 0) {
            return new BigFraction(denominator.pow(-exponent), numerator.pow(-exponent));
        }
        return new BigFraction(numerator.pow(exponent), denominator.pow(exponent));
    }

    /**
     * <p>
     * Returns a <code>BigFraction</code> whose value is
     * \(\mathit{this}^{\mathit{exponent}}\), returning the result in reduced form.
     * </p>
     *
     * @param exponent
     *            exponent to which this <code>BigFraction</code> is to be raised.
     * @return \(\mathit{this}^{\mathit{exponent}}\) as a <code>BigFraction</code>.
     */
    public BigFraction pow(final long exponent) {
        if (exponent == 0) {
            return ONE;
        }
        if (numerator.signum() == 0) {
            return this;
        }

        if (exponent < 0) {
            return new BigFraction(ArithmeticUtils.pow(denominator, -exponent),
                                   ArithmeticUtils.pow(numerator,   -exponent));
        }
        return new BigFraction(ArithmeticUtils.pow(numerator,   exponent),
                               ArithmeticUtils.pow(denominator, exponent));
    }

    /**
     * <p>
     * Returns a <code>BigFraction</code> whose value is
     * \(\mathit{this}^{\mathit{exponent}}\), returning the result in reduced form.
     * </p>
     *
     * @param exponent
     *            exponent to which this <code>BigFraction</code> is to be raised.
     * @return \(\mathit{this}^{\mathit{exponent}}\) as a <code>BigFraction</code>.
     */
    public BigFraction pow(final BigInteger exponent) {
        if (exponent.signum() == 0) {
            return ONE;
        }
        if (numerator.signum() == 0) {
            return this;
        }

        if (exponent.signum() == -1) {
            final BigInteger eNeg = exponent.negate();
            return new BigFraction(ArithmeticUtils.pow(denominator, eNeg),
                                   ArithmeticUtils.pow(numerator,   eNeg));
        }
        return new BigFraction(ArithmeticUtils.pow(numerator,   exponent),
                               ArithmeticUtils.pow(denominator, exponent));
    }

    /**
     * <p>
     * Returns a <code>double</code> whose value is
     * \(\mathit{this}^{\mathit{exponent}}\), returning the result in reduced form.
     * </p>
     *
     * @param exponent
     *            exponent to which this <code>BigFraction</code> is to be raised.
     * @return \(\mathit{this}^{\mathit{exponent}}\).
     */
    public double pow(final double exponent) {
        return Math.pow(numerator.doubleValue(),   exponent) /
               Math.pow(denominator.doubleValue(), exponent);
    }

    /**
     * <p>
     * Return the multiplicative inverse of this fraction.
     * </p>
     *
     * @return the reciprocal fraction.
     */
    public BigFraction reciprocal() {
        return new BigFraction(denominator, numerator);
    }

    /**
     * <p>
     * Subtracts the value of an {@link BigInteger} from the value of this
     * {@code BigFraction}, returning the result in reduced form.
     * </p>
     *
     * @param bg the {@link BigInteger} to subtract, cannot be {@code null}.
     * @return a {@code BigFraction} instance with the resulting values.
     */
    public BigFraction subtract(final BigInteger bg) {
        checkNotNull(bg, PARAM_NAME_BG);
        if (bg.signum() == 0) {
            return this;
        }
        if (numerator.signum() == 0) {
            return of(bg.negate());
        }

        return new BigFraction(numerator.subtract(denominator.multiply(bg)), denominator);
    }

    /**
     * <p>
     * Subtracts the value of an {@code integer} from the value of this
     * {@code BigFraction}, returning the result in reduced form.
     * </p>
     *
     * @param i the {@code integer} to subtract.
     * @return a {@code BigFraction} instance with the resulting values.
     */
    public BigFraction subtract(final int i) {
        return subtract(BigInteger.valueOf(i));
    }

    /**
     * <p>
     * Subtracts the value of a {@code long} from the value of this
     * {@code BigFraction}, returning the result in reduced form.
     * </p>
     *
     * @param l the {@code long} to subtract.
     * @return a {@code BigFraction} instance with the resulting values.
     */
    public BigFraction subtract(final long l) {
        return subtract(BigInteger.valueOf(l));
    }

    /**
     * <p>
     * Subtracts the value of another fraction from the value of this one,
     * returning the result in reduced form.
     * </p>
     *
     * @param fraction {@link BigFraction} to subtract, must not be {@code null}.
     * @return a {@link BigFraction} instance with the resulting values
     */
    public BigFraction subtract(final BigFraction fraction) {
        checkNotNull(fraction, PARAM_NAME_FRACTION);
        if (fraction.numerator.signum() == 0) {
            return this;
        }
        if (numerator.signum() == 0) {
            return fraction.negate();
        }

        final BigInteger num;
        final BigInteger den;
        if (denominator.equals(fraction.denominator)) {
            num = numerator.subtract(fraction.numerator);
            den = denominator;
        } else {
            num = (numerator.multiply(fraction.denominator)).subtract((fraction.numerator).multiply(denominator));
            den = denominator.multiply(fraction.denominator);
        }
        return new BigFraction(num, den);
    }

    /**
     * <p>
     * Returns the <code>String</code> representing this fraction, ie
     * "num / dem" or just "num" if the denominator is one.
     * </p>
     *
     * @return a string representation of the fraction.
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final String str;
        if (BigInteger.ONE.equals(denominator)) {
            str = numerator.toString();
        } else if (BigInteger.ZERO.equals(numerator)) {
            str = "0";
        } else {
            str = numerator + " / " + denominator;
        }
        return str;
    }

    /**
     * Parses a string that would be produced by {@link #toString()}
     * and instantiates the corresponding object.
     *
     * @param s String representation.
     * @return an instance.
     * @throws FractionException if the string does not
     * conform to the specification.
     */
    public static BigFraction parse(String s) {
        s = s.replace(",", "");
        final int slashLoc = s.indexOf("/");
        // if no slash, parse as single number
        if (slashLoc == -1) {
            return BigFraction.of(new BigInteger(s.trim()));
        } else {
            final BigInteger num = new BigInteger(
                    s.substring(0, slashLoc).trim());
            final BigInteger denom = new BigInteger(s.substring(slashLoc + 1).trim());
            return of(num, denom);
        }
    }


    /**
     * Check that the argument is not null and throw a NullPointerException
     * if it is.
     * @param arg     the argument to check
     * @param argName the name of the argument
     */
    private static void checkNotNull(Object arg, String argName) {
        if (arg == null) {
            throw new NullPointerException(argName);
        }
    }
}
