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

//  (C) Copyright John Maddock 2006.
//  Use, modification and distribution are subject to the
//  Boost Software License, Version 1.0. (See accompanying file
//  LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)

package org.apache.commons.numbers.gamma;

import java.util.function.DoubleSupplier;

/**
 * Utility tools used by the Boost functions.
 *
 * <p>This code has been adapted from the <a href="https://www.boost.org/">Boost</a>
 * {@code c++} implementations in {@code <boost/math/tools/>}.
 * All work is copyright John Maddock 2006 and subject to the Boost Software License.
 */
final class BoostTools {
    /**
     * The minimum epsilon value for relative error in the summation.
     * Equal to Math.ulp(1.0) or 2^-52.
     *
     * <h2>Note</h2>
     *
     * <p>The summation will terminate when any additional terms are too small to
     * change the sum. Assuming additional terms are reducing in magnitude this
     * occurs when the term is 1 ULP from the sum:
     * <pre>{@code
     * ulp(sum) >= term
     * }</pre>
     *
     * <p>The epsilon is used to set a configurable threshold using:
     * <pre>{@code
     * sum * eps >= term
     * }</pre>
     * <p>The minimum epsilon is the smallest value that will create a value
     * {@code >= 1} ULP and {@code < 2} ULP of the sum. For any normal number the ULP
     * of all values with the same exponent b is scalb(1.0, b - 52). This can
     * be achieved by multiplication by 2^-52.
     */
    private static final double EPSILON = 0x1.0p-52;
    /**
     * The minimum epsilon value for relative error in the Kahan summation.
     * This can be lower than {@link #EPSILON}. Set to 2^-62.
     *
     * <h2>Note</h2>
     *
     * <p>The Kahan summation uses a carry term to extend the precision of the sum.
     * This extends the 53-bit mantissa by adding more bits to hold round-off error.
     * Thus the term may effect the sum when it has a magnitude smaller than 1 ULP
     * of the current sum. The epsilon can be lowered from 2^-52 to include the
     * extra bits in the convergence criteria. The lower limit for the epsilon is
     * 2^-104. Boost uses an epsilon specified as a number of bits of accuracy. Each
     * lowering of the epsilon by a factor of 2 adds a guard digit to the sum.
     * Slower converging series will benefit from a lower epsilon. This uses 2^-62
     * to add 10 guard digits and allow testing of different thresholds when the
     * Kahan summation is used, for example in the gamma function. Lowering the
     * epsilon further is only of benefit if the terms can be computed exactly.
     * Otherwise the rounding errors of the early terms affect the final result as
     * much as the inclusion of extra guard digits.
     */
    private static final double KAHAN_EPSILON = 0x1.0p-62;
    /** Message for failure to converge. */
    private static final String MSG_FAILED_TO_CONVERGE = "Failed to converge within %d iterations";

    /** Private constructor. */
    private BoostTools() {
        // intentionally empty.
    }

    /**
     * Sum the series.
     *
     * <p>Adapted from {@code boost/math/tools/series.hpp}.
     *
     * @param func Series generator
     * @param epsilon Maximum relative error allowed
     * @param maxTerms Maximum number of terms
     * @return result
     */
    static double sumSeries(DoubleSupplier func, double epsilon, int maxTerms) {
        return sumSeries(func, epsilon, maxTerms, 0);
    }

    /**
     * Sum the series.
     *
     * <p>Adapted from {@code boost/math/tools/series.hpp}.
     *
     * @param func Series generator
     * @param epsilon Maximum relative error allowed
     * @param maxTerms Maximum number of terms
     * @param initValue Initial value
     * @return result
     */
    static double sumSeries(DoubleSupplier func, double epsilon, int maxTerms, double initValue) {
        // Note:
        // The Boost code requires eps to be non-zero. It is created in the
        // <boost/math/policies/policy.hpp> as a non-zero relative error term.
        // An alternative termination condition with a divide is:
        // (eps < Math.abs(nextTerm / result))
        //
        // Here the argument is checked against the minimum epsilon for a double
        // to provide functional equivalence with the Boost policy.
        // In the min eps case the loop terminates if the most recently added term is
        // 0 or 1 ulp of the result. This condition is acceptable if the next
        // computed term will be at most half of the most recent term (thus
        // cannot be added to the current result).

        final double eps = getEpsilon(epsilon, EPSILON);

        int counter = maxTerms;

        double result = initValue;
        double nextTerm;
        do {
            nextTerm = func.getAsDouble();
            result += nextTerm;
        } while (Math.abs(eps * result) < Math.abs(nextTerm) && --counter > 0);

        if (counter <= 0) {
            throw new ArithmeticException(
               String.format(MSG_FAILED_TO_CONVERGE, maxTerms));
        }

        return result;
    }

    /**
     * Sum the series using Kahan summation.
     *
     * <p>Adapted from {@code boost/math/tools/series.hpp}.
     *
     * @param func Series generator
     * @param epsilon Maximum relative error allowed
     * @param maxTerms Maximum number of terms
     * @return result
     */
    static double kahanSumSeries(DoubleSupplier func, double epsilon, int maxTerms) {
        return kahanSumSeries(func, epsilon, maxTerms, 0);
    }

    /**
     * Sum the series using Kahan summation.
     *
     * <p>Adapted from {@code boost/math/tools/series.hpp}.
     *
     * @param func Series generator
     * @param epsilon Maximum relative error allowed
     * @param maxTerms Maximum number of terms
     * @param initValue Initial value
     * @return result
     */
    static double kahanSumSeries(DoubleSupplier func, double epsilon, int maxTerms, double initValue) {
        final double eps = getEpsilon(epsilon, KAHAN_EPSILON);

        int counter = maxTerms;

        // Kahan summation:
        // https://en.wikipedia.org/wiki/Kahan_summation_algorithm
        // This summation is accurate if the term is smaller in magnitude
        // than the current sum. This is a condition required for the
        // series termination thus the extended precision sum need not
        // check magnitudes of terms to compute the carry.

        double result = initValue;
        double carry = 0;
        double nextTerm;
        do {
            nextTerm = func.getAsDouble();
            final double y = nextTerm - carry;
            final double t = result + y;
            carry = t - result;
            carry -= y;
            result = t;
        } while (Math.abs(eps * result) < Math.abs(nextTerm) && --counter > 0);

        if (counter <= 0) {
            throw new ArithmeticException(
               String.format(MSG_FAILED_TO_CONVERGE, maxTerms));
        }

        return result;
    }

    /**
     * Gets the epsilon ensuring it satisfies the minimum allowed value.
     *
     * <p>This is returning the maximum of the two arguments.
     * Do not use Math.max as it returns NaN if either value is NaN.
     * In this case the desired result in the default minEpsilon, not NaN.
     * Math.max will also check ordering when terms are equal to support
     * -0.0 and 0.0. This does not apply here and a single conditional
     * returns the desired result.
     *
     * @param epsilon Configured epsilon
     * @param minEpsilon Minimum allowed epsilon
     * @return the epsilon
     */
    private static double getEpsilon(double epsilon, double minEpsilon) {
        return epsilon > minEpsilon ? epsilon : minEpsilon;
    }

    /**
     * Evaluate the polynomial using Horner's method.
     * The coefficients are used in descending order, for example a polynomial of order
     * 3 requires 4 coefficients:
     * <pre>
     * f(x) = c[3] * x^3 + c[2] * x^2 + c[1] * x + c[0]
     * </pre>
     *
     * @param c Polynomial coefficients (must have {@code length > 0})
     * @param x Argument x
     * @return polynomial value
     */
    static double evaluatePolynomial(double[] c, double x) {
        final int count = c.length;
        double sum = c[count - 1];
        for (int i = count - 2; i >= 0; --i) {
            sum *= x;
            sum += c[i];
        }
        return sum;
    }
}
