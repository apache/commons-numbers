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

import java.math.BigInteger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A {@code BigInteger} based, mutable representation of a simple continued
 * fraction, i.e. a continued fraction with partial numerators of 1 and partial
 * denominators, or coefficients, that are positive integers except for the
 * first coefficient, which can be any integer.
 */
class SimpleContinuedFraction {
    /**
     * The error message to display when attempting to perform an operation that
     * requires coefficients to be present in the absence of any coefficients.
     */
    private static final String COEFFICIENTS_EMPTY_ERROR_MESSAGE = "No coefficients present.";

    /**
     * The coefficients of this continued fraction.
     */
    private final List<BigInteger> coefficients;

    /**
     * A 2-element array containing the numerator followed by the denominator of
     * the convergent corresponding to the last coefficient of this continued
     * fraction, or, if no coefficients are currently stored, of the second of
     * the two initial "theoretical" convergents 0/1 and 1/0 that don't
     * correspond to any coefficient but are needed for the recursive formula
     * that calculates the convergents to work.
     */
    private BigInteger[] currentConvergent;

    /**
     * A 2-element array containing the numerator followed by the denominator of
     * the convergent corresponding to the second to last coefficient of this
     * continued fraction, or, if less than two coefficients are currently
     * stored, of one of the two initial "theoretical" convergents 0/1 and 1/0
     * that don't correspond to any coefficient but are needed for the recursive
     * formula that calculates the convergents to work.
     */
    private BigInteger[] previousConvergent;

    /**
     * Creates an instance that does not store any coefficients and hence does
     * not represent a number.
     */
    SimpleContinuedFraction() {
        coefficients = new ArrayList<>();
        previousConvergent = new BigInteger[]{BigInteger.ZERO, BigInteger.ONE};
        currentConvergent = new BigInteger[]{BigInteger.ONE, BigInteger.ZERO};
    }

    /**
     * Adds a coefficient to the end of the simple continued fraction
     * representation of this instance. If this instance does not yet have any
     * coefficients stored, the new coefficient can be any integer, otherwise,
     * it must be positive.
     * @param coefficient the new coefficient
     * @throws NullPointerException if the argument is {@code null}
     * @throws IllegalArgumentException if this instance already has coefficients
     *         stored and the argument is not positive
     */
    void addCoefficient(BigInteger coefficient) {
        if (!coefficients.isEmpty()) {
            requirePositiveCoefficient(coefficient);
        } else {
            Objects.requireNonNull(coefficient);
        }
        coefficients.add(coefficient);
        BigInteger newNumerator = coefficient.multiply(currentConvergent[0]).add(previousConvergent[0]);
        BigInteger newDenominator = coefficient.multiply(currentConvergent[1]).add(previousConvergent[1]);
        previousConvergent = currentConvergent;
        currentConvergent = new BigInteger[]{newNumerator, newDenominator};
    }

    /**
     * Replaces the last coefficient in this instance's simple continued
     * fraction representation with the specified value. If this instance has
     * only one coefficient stored, the new value can be any integer, otherwise,
     * it must be positive.
     * @param coefficient the new coefficient
     * @return the coefficient previously at the last position in this instance's
     *         simple continued fraction representation
     * @throws NullPointerException if the argument is {@code null}
     * @throws IllegalArgumentException if this instance has more than one
     *         coefficient stored and the argument is not positive
     * @throws IllegalStateException if this instance does not have any
     *         coefficients stored
     */
    BigInteger setLastCoefficient(BigInteger coefficient) {
        if (coefficients.size() > 1) {
            requirePositiveCoefficient(coefficient);
        } else {
            Objects.requireNonNull(coefficient);
        }
        BigInteger oldLastCoefficient;
        try {
            oldLastCoefficient = coefficients.set(coefficients.size() - 1, coefficient);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalStateException(COEFFICIENTS_EMPTY_ERROR_MESSAGE);
        }
        BigInteger diff = coefficient.subtract(oldLastCoefficient);
        currentConvergent[0] = currentConvergent[0].add(diff.multiply(previousConvergent[0]));
        currentConvergent[1] = currentConvergent[1].add(diff.multiply(previousConvergent[1]));
        return oldLastCoefficient;
    }

    /**
     * Removes the last coefficient from this instance's simple continued
     * fraction representation.
     * @return the coefficient that was removed
     * @throws IllegalStateException if this instance does not have any
     *         coefficients stored
     */
    BigInteger removeLastCoefficient() {
        BigInteger lastCoefficient;
        try {
            lastCoefficient = coefficients.remove(coefficients.size() - 1);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalStateException(COEFFICIENTS_EMPTY_ERROR_MESSAGE);
        }
        BigInteger newNumerator = currentConvergent[0].subtract(lastCoefficient.multiply(previousConvergent[0]));
        BigInteger newDenominator = currentConvergent[1].subtract(lastCoefficient.multiply(previousConvergent[1]));
        currentConvergent = previousConvergent;
        previousConvergent = new BigInteger[]{newNumerator, newDenominator};
        return lastCoefficient;
    }

    /**
     * Returns a read-only view of the coefficients of this instance's simple
     * continued fraction representation.
     * @return the coefficients of this simple continued fraction
     */
    List<BigInteger> viewCoefficients() {
        return Collections.unmodifiableList(coefficients);
    }

    /**
     * If this instance does not have any coefficients stored, this method
     * returns the array {@code {1, 0}}. Otherwise, it returns a 2-element array
     * containing the numerator followed by the denominator of the convergent
     * corresponding to the last coefficient of this continued fraction. The
     * convergent's denominator will always be positive.
     * @return a representation of a convergent as described above
     */
    BigInteger[] getCurrentConvergent() {
        return Arrays.copyOf(currentConvergent, 2);
    }

    /**
     * If this instance does not have any coefficients stored, this method
     * returns the array {@code {0, 1}}. If exactly one coefficient is stored,
     * it returns the array {@code {1, 0}}. Otherwise, it returns a 2-element
     * array containing the numerator followed by the denominator of the
     * convergent corresponding to the second to last coefficient of this
     * continued fraction. The convergent's denominator will always be positive.
     * @return a representation of a convergent as described above
     */
    BigInteger[] getPreviousConvergent() {
        return Arrays.copyOf(previousConvergent, 2);
    }

    /**
     * Converts this continued fraction to a {@code BigFraction}.
     * @return a {@code BigFraction} instance equivalent to this continued
     *         fraction, reduced to lowest terms
     * @throws IllegalStateException if this instance does not have any
     *         coefficients stored
     */
    BigFraction toBigFraction() {
        if (coefficients.isEmpty()) {
            throw new IllegalStateException(COEFFICIENTS_EMPTY_ERROR_MESSAGE);
        }
        return BigFraction.of(currentConvergent[0], currentConvergent[1]);
    }

    /**
     * Ascertains that an integer that is intended to be used as a coefficient
     * other than the first one is positive, and throws an
     * {@code IllegalArgumentException} with a corresponding error message if it
     * is not.
     * @param coefficient the prospective non-initial coefficient
     * @throws NullPointerException if the argument is {@code null}
     * @throws IllegalArgumentException if the argument is not positive
     */
    private static void requirePositiveCoefficient(BigInteger coefficient) {
        if (coefficient.signum() != 1) {
            throw new IllegalArgumentException("Only the initial coefficient may be non-positive: " + coefficient);
        }
    }

    /**
     * <p>Generates an iterator that iterates over the coefficients of the
     * simple continued fraction representation of the passed
     * {@code BigFraction} object.</p>
     *
     * <p>If <code>[a<sub>0</sub>; a<sub>1</sub>, a<sub>2</sub>, …]</code> is
     * the simple continued fraction representation of the argument, then the
     * ({@code i+1})th invocation of the returned iterator's {@link Iterator#next()
     * next()} method returns a 3-element {@code BigInteger} array
     * <code>&#x7b;a<sub>i</sub>, p, q&#x7d;</code>, where {@code p} and {@code q}
     * are integers such that {@code q > 0} and <code>p/q = [a<sub>i</sub>;
     * a<sub>i+1</sub>, a<sub>i+2</sub>, …]</code>.</p>
     *
     * <p>Note that the iterator returned by this method always generates the
     * shorter of the two equivalent simple continued fraction representations
     * of a rational number, i.e. the one where the last coefficient is not 1
     * unless the represented number itself is 1 (in which case the generated
     * sequence of coefficients will simply be [1;] rather than [0; 1]). For
     * example, when passed 31/24, the iterator will generate the sequence
     * [1; 3, 2, 3], and not the equivalent sequence [1; 3, 2, 2, 1].</p>
     * @param fraction the fraction the coefficients of whose simple continued
     *        fraction representation should be iterated over
     * @return an {@code Iterator<BigInteger[]>} as described above
     * @throws NullPointerException if the argument is {@code null}
     */
    static Iterator<BigInteger[]> coefficientsOf(BigFraction fraction) {
        final BigInteger initialDividend;
        final BigInteger initialDivisor;
        if (fraction.getDenominator().signum() == -1) {
            initialDividend = fraction.getNumerator().negate();
            initialDivisor = fraction.getDenominator().negate();
        } else {
            initialDividend = fraction.getNumerator();
            initialDivisor = fraction.getDenominator();
        }

        return new Iterator<BigInteger[]>() {
            private BigInteger dividend = initialDividend;
            private BigInteger divisor = initialDivisor;

            @Override
            public boolean hasNext() {
                return !divisor.equals(BigInteger.ZERO);
            }

            @Override
            public BigInteger[] next() {
                BigInteger[] quotientAndRemainder;
                try {
                    quotientAndRemainder = dividend.divideAndRemainder(divisor);
                } catch (ArithmeticException e) {
                    throw new NoSuchElementException();
                }
                //simulate floor function for negative quotients to ensure non-negative remainder
                if (quotientAndRemainder[1].signum() == -1) {
                    quotientAndRemainder[0] = quotientAndRemainder[0].subtract(BigInteger.ONE);
                    quotientAndRemainder[1] = quotientAndRemainder[1].add(divisor);
                }
                BigInteger[] result = new BigInteger[]{quotientAndRemainder[0], dividend, divisor};
                dividend = divisor;
                divisor = quotientAndRemainder[1];
                return result;
            }
        };
    }
}
