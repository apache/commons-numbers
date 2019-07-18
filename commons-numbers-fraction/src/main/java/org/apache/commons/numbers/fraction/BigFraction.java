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
import java.util.Iterator;
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
     * This factory method approximates the given {@code double} value with a
     * fraction such that no other fraction within the given interval will have
     * a smaller or equal denominator, unless {@code |epsilon| > 0.5}, in which
     * case the integer closest to the value to be approximated will be returned
     * (if there are two equally distant integers within the specified interval,
     * either of them will be returned).
     * </p>
     * <p>
     * References:
     * <ul>
     * <li><a href="http://mathworld.wolfram.com/ContinuedFraction.html">
     * Continued Fraction</a> equations (11) and (22)-(26)</li>
     * </ul>
     *
     * @param value Value to convert to a fraction.
     * @param epsilon Maximum error allowed. The resulting fraction is within
     *                {@code epsilon} of {@code value}, in absolute terms.
     * @param maxIterations Maximum number of convergents. If this parameter is
     *                      negative, no limit will be imposed.
     * @throws ArithmeticException if {@code maxIterations >= 0} and the
     *         continued fraction failed to converge after this number of
     *         iterations
     * @throws IllegalArgumentException if {@code value} is NaN or infinite, or
     *         if {@code epsilon} is NaN.
     * @return a new instance.
     *
     * @see #from(double,BigInteger)
     */
    public static BigFraction from(final double value,
                                   double epsilon,
                                   final int maxIterations) {
        BigFraction valueAsFraction = from(value);
        /*
         * For every rational number outside the interval [α - 0.5, α + 0.5],
         * there will be a rational number with the same denominator that lies
         * within that interval (because repeatedly adding or subtracting 1 from
         * any number is bound to hit the interval eventually). Limiting epsilon
         * to ±0.5 thus ensures that, if a number with an absolute value greater
         * than 0.5 is passed as epsilon, the integer closest to α is returned,
         * and it also eliminates the need to make a special case for epsilon
         * being infinite.
         */
        epsilon = Math.min(Math.abs(epsilon), 0.5);
        BigFraction epsilonAsFraction = from(epsilon);

        BigFraction lowerBound = valueAsFraction.subtract(epsilonAsFraction);
        BigFraction upperBound = valueAsFraction.add(epsilonAsFraction);

        /*
         * If [a_0; a_1, a_2, …] and [b_0; b_1, b_2, …] are the simple continued
         * fraction expansions of the specified interval's boundaries, and n is
         * an integer such that a_k = b_k for all k <= n, every real number
         * within this interval will have a simple continued fraction expansion
         * of the form
         *
         * [a_0; a_1, …, a_n, c_0, c_1, …]
         *
         * where [c_0; c_1, c_2 …] lies between [a_{n+1}; a_{n+2}, …] and
         * [b_{n+1}; b_{n+2}, …]
         *
         * The objective is therefore to calculate a value for c_0 so that the
         * denominator will grow by the smallest amount possible with the
         * resulting number still being within the given interval.
         */
        Iterator<BigInteger[]> coefficientsOfLower = SimpleContinuedFraction.coefficientsOf(lowerBound);
        Iterator<BigInteger[]> coefficientsOfUpper = SimpleContinuedFraction.coefficientsOf(upperBound);
        BigInteger lastCoefficientOfLower;
        BigInteger lastCoefficientOfUpper;

        SimpleContinuedFraction approximation = new SimpleContinuedFraction();

        boolean stop = false;
        int iterationCount = 0;
        do {
            if (maxIterations >= 0 && iterationCount == maxIterations) {
                throw new FractionException(FractionException.ERROR_CONVERSION, value, maxIterations);
            }
            if (coefficientsOfLower.hasNext()) {
                lastCoefficientOfLower = coefficientsOfLower.next()[0];
            } else {
                lastCoefficientOfLower = null;
            }
            if (coefficientsOfUpper.hasNext()) {
                lastCoefficientOfUpper = coefficientsOfUpper.next()[0];
            } else {
                lastCoefficientOfUpper = null;
            }
            if (lastCoefficientOfLower == null ||
                    !lastCoefficientOfLower.equals(lastCoefficientOfUpper)) {
                stop = true;
            } else {
                approximation.addCoefficient(lastCoefficientOfLower);
            }
            iterationCount++;
        } while (!stop);

        if (lastCoefficientOfLower != null && lastCoefficientOfUpper != null) {
            BigInteger finalCoefficient;
            if (lastCoefficientOfLower.compareTo(lastCoefficientOfUpper) < 0) {
                finalCoefficient = lastCoefficientOfLower;
                if (coefficientsOfLower.hasNext()) {
                    finalCoefficient = finalCoefficient.add(BigInteger.ONE);
                }
            } else {
                finalCoefficient = lastCoefficientOfUpper;
                if (coefficientsOfUpper.hasNext()) {
                    finalCoefficient = finalCoefficient.add(BigInteger.ONE);
                }
            }
            approximation.addCoefficient(finalCoefficient);
        }
        return approximation.toBigFraction();
    }

    /**
     * Create a fraction given the double value and maximum denominator.
     * <p>
     * This factory method approximates the given {@code double} value with a
     * fraction such that no other fraction with a denominator smaller than or
     * equal to the passed upper bound for the denominator will be closer to the
     * {@code double} value. Furthermore, no other fraction with the same or a
     * smaller denominator will be equally close to the {@code double} value
     * unless the denominator limit is set to {@code 1} and the value to be
     * approximated is an odd multiple of {@code 0.5}, in which case there will
     * necessarily be two equally distant integers surrounding the {@code double}
     * value, one of which will then be returned by this method.
     * </p>
     * <p>
     * References:
     * <ul>
     * <li><a href="http://mathworld.wolfram.com/ContinuedFraction.html">
     * Continued Fraction</a> equations (11) and (22)-(26)</li>
     * </ul>
     *
     * @param value Value to convert to a fraction.
     * @param maxDenominator Maximum allowed value for denominator.
     * @throws IllegalArgumentException if the given {@code value} is NaN or
     *         infinite, or if {@code maxDenominator < 1}
     * @return a new instance.
     *
     * @see #from(double,double,int)
     */
    public static BigFraction from(final double value,
                                   final BigInteger maxDenominator) {
        if (maxDenominator.signum() != 1) {
            throw new IllegalArgumentException("Upper bound for denominator must be positive: " + maxDenominator);
        }

        /*
         * Required facts:
         *
         * 1. Every best rational approximation p/q of α (with q > 0), in the
         * sense that |α - p/q| < |α - a/b| for all a/b ≠ p/q and 0 < b <= q, is
         * a convergent or a semiconvergent of α's simple continued fraction
         * expansion (Continued Fractions by A. Khinchin, theorem 15, p. 22)
         *
         * 2. Every convergent p/q from the second convergent onwards is a best
         * rational approximation (even in the stronger sense that
         * |qα - p| < |bα - a|), provided that the last coefficient is greater
         * than 1 (theorem 17, p. 26, which ignores the fact that, if a_1 = 1,
         * both the first and the second convergent will have a denominator of
         * 1 and, should the variable k chosen to be 0, s = k + 1 will therefore
         * also be conceivable as the order of the convergent equivalent to the
         * expression x_0 / y_0 in addition to s <= k).
         *
         * 3. It follows that the first convergent is only a best rational
         * approximation if a_1 >= 2, i.e. if the second convergent does not
         * also have a denominator of 1, and if α ≠ [a_0; 2] (the exceptional
         * case mentioned in theorem 17, where the first convergent a_0 will tie
         * with the semiconvergent a_0 + 1 as a best rational approximation of α).
         *
         * 4. A semiconvergent [a_0; a_1, …, a_{n-1}, x], with n >= 1 and
         * 0 < x <= a_n, is consequently only a best rational approximation if it
         * is closer to α than the (n-1)th-order convergent [a_0; a_1, …, a_{n-1}],
         * since the latter is definitely a best rational approximation (unless
         * n = 1 and a_1 = 1, but then, the only possible integer value for x is
         * a_1 = 1, which will yield the second convergent, which is always a
         * best approximation). This is the case if and only if
         *
         * [a_n; a_{n+1}, a_{n+2}, …] - 2x < q_{n-2} / q_{n-1}
         *
         * where q_k is the denominator of the k-th-order convergent
         * (https://math.stackexchange.com/questions/856861/semi-convergent-of-continued-fractions)
         */
        BigFraction valueAsFraction = from(value);

        SimpleContinuedFraction approximation = new SimpleContinuedFraction();
        BigInteger[] currentConvergent;
        BigInteger[] convergentBeforePrevious;

        Iterator<BigInteger[]> coefficientsIterator = SimpleContinuedFraction.coefficientsOf(valueAsFraction);
        BigInteger[] lastIterationResult;

        do {
            convergentBeforePrevious = approximation.getPreviousConvergent();
            lastIterationResult = coefficientsIterator.next();
            approximation.addCoefficient(lastIterationResult[0]);
            currentConvergent = approximation.getCurrentConvergent();
        } while (coefficientsIterator.hasNext() && currentConvergent[1].compareTo(maxDenominator) <= 0);

        if (currentConvergent[1].compareTo(maxDenominator) <= 0) {
            return valueAsFraction;
        } else {
            // Calculate the largest possible value for the last coefficient so
            // that the denominator will be within the given bounds
            BigInteger[] previousConvergent = approximation.getPreviousConvergent();
            BigInteger lastCoefficientMax = maxDenominator.subtract(convergentBeforePrevious[1]).divide(previousConvergent[1]);

            /*
             * Determine if the semiconvergent generated with this coefficient
             * is a closer approximation than the previous convergent with the
             * formula described in point 4. n >= 1 is guaranteed because the
             * first convergent's denominator is 1 and thus cannot exceed the
             * limit, and if x = 0 is inserted into the inequation, it will
             * always be false because a_n >= 1 and k_{n-2} / k_{n-1} <= 1, so
             * no need to make a special case for lastCoefficientMax = 0.
             */
            boolean semiConvergentIsCloser = lastIterationResult[1]
                    .subtract(BigInteger.valueOf(2)
                            .multiply(lastCoefficientMax)
                            .multiply(lastIterationResult[2]))
                    .multiply(previousConvergent[1])
                    .compareTo(convergentBeforePrevious[1]
                            .multiply(lastIterationResult[2])) < 0;

            if (semiConvergentIsCloser) {
                return of(previousConvergent[0].multiply(lastCoefficientMax).add(convergentBeforePrevious[0]),
                          previousConvergent[1].multiply(lastCoefficientMax).add(convergentBeforePrevious[1]));
            } else {
                return of(previousConvergent[0], previousConvergent[1]);
            }
        }
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
        double doubleNum = numerator.doubleValue();
        double doubleDen = denominator.doubleValue();
        double result = doubleNum / doubleDen;
        if (Double.isInfinite(doubleNum) ||
            Double.isInfinite(doubleDen) ||
            Double.isNaN(result)) {
            // Numerator and/or denominator must be out of range:
            // Calculate how far to shift them to put them in range.
            int shift = Math.max(numerator.bitLength(),
                                 denominator.bitLength()) - Math.getExponent(Double.MAX_VALUE);
            result = numerator.shiftRight(shift).doubleValue() /
                denominator.shiftRight(shift).doubleValue();
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
        float floatNum = numerator.floatValue();
        float floatDen = denominator.floatValue();
        float result = floatNum / floatDen;
        if (Float.isInfinite(floatNum) ||
            Float.isInfinite(floatDen) ||
            Float.isNaN(result)) {
            // Numerator and/or denominator must be out of range:
            // Calculate how far to shift them to put them in range.
            int shift = Math.max(numerator.bitLength(),
                                 denominator.bitLength()) - Math.getExponent(Float.MAX_VALUE);
            result = numerator.shiftRight(shift).floatValue() /
                denominator.shiftRight(shift).floatValue();
        }
        return result;
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
