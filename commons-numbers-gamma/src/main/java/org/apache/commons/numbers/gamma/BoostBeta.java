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

//  Copyright John Maddock 2006.
//  Use, modification and distribution are subject to the
//  Boost Software License, Version 1.0. (See accompanying file
//  LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)

package org.apache.commons.numbers.gamma;

import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import org.apache.commons.numbers.fraction.GeneralizedContinuedFraction;
import org.apache.commons.numbers.fraction.GeneralizedContinuedFraction.Coefficient;

/**
 * Implementation of the
 * <a href="https://mathworld.wolfram.com/RegularizedBetaFunction.html">regularized beta functions</a> and
 * <a href="https://mathworld.wolfram.com/IncompleteBetaFunction.html">incomplete beta functions</a>.
 *
 * <p>This code has been adapted from the <a href="https://www.boost.org/">Boost</a>
 * {@code c++} implementation {@code <boost/math/special_functions/beta.hpp>}.
 * All work is copyright to the original authors and subject to the Boost Software License.
 *
 * @see
 * <a href="https://www.boost.org/doc/libs/1_77_0/libs/math/doc/html/math_toolkit/sf_beta.html">
 * Boost C++ beta functions</a>
 */
final class BoostBeta {
    //
    // Code ported from Boost 1.77.0
    //
    // boost/math/special_functions/beta.hpp
    //
    // Original code comments are preserved.
    //
    // Changes to the Boost implementation:
    // - Update method names to replace underscores with camel case
    // - Remove checks for under/overflow. In this implementation no error is raised
    //   for overflow (infinity is returned) or underflow (sub-normal or zero is returned).
    //   This follows the conventions in java.lang.Math for the same conditions.
    // - Removed the pointer p_derivative in the betaIncompleteImp. This is used
    //   in the Boost code for the beta_inv functions for a derivative
    //   based inverse function. This is currently not supported.
    // - Altered series generators to use integer counters added to the double term
    //   replacing directly incrementing a double term. When the term is large it cannot
    //   be incremented: 1e16 + 1 == 1e16.
    // - Added the use of the classic continued fraction representation for cases
    //   where the Boost implementation detects sub-normal terms and does not evaluate.
    // - Updated the method used to compute the binomialCoefficient. This can use a
    //   series evaluation when n > max factorial given that n - k < 40.
    // - Changed convergence criteria for betaSmallBLargeASeries to stop when r has
    //   no effect on the sum. The Boost code uses machine epsilon (ignoring the policy eps).

    /** Default epsilon value for relative error.
     * This is equal to the Boost constant {@code boost::math::EPSILON}. */
    private static final double EPSILON = 0x1.0p-52;
    /** Approximate value for ln(Double.MAX_VALUE).
     * This is equal to the Boost constant {@code boost::math::tools::log_max_value<double>()}.
     * No term {@code x} should be used in {@code exp(x)} if {@code x > LOG_MAX_VALUE} to avoid
     * overflow. */
    private static final int LOG_MAX_VALUE = 709;
    /** Approximate value for ln(Double.MIN_VALUE).
     * This is equal to the Boost constant {@code boost::math::tools::log_min_value<double>()}.
     * No term {@code x} should be used in {@code exp(x)} if {@code x < LOG_MIN_VALUE} to avoid
     * underflow to sub-normal or zero. */
    private static final int LOG_MIN_VALUE = -708;
    /** pi/2. */
    private static final double HALF_PI = Math.PI / 2;
    /** The largest factorial that can be represented as a double.
     * This is equal to the Boost constant {@code boost::math::max_factorial<double>::value}. */
    private static final int MAX_FACTORIAL = 170;
    /** Size of the table of Pn's.
     * Equal to {@code Pn_size<double>} suitable for 16-20 digit accuracy. */
    private static final int PN_SIZE = 30;
    /** 2^53. Used to scale sub-normal values. */
    private static final double TWO_POW_53 = 0x1.0p53;
    /** 2^-53. Used to rescale values. */
    private static final double TWO_POW_M53 = 0x1.0p-53;

    /** Private constructor. */
    private BoostBeta() {
        // intentionally empty.
    }

    /**
     * Beta function.
     * <p>\[ B(p, q) = \frac{\Gamma(p) \Gamma(q)}{\Gamma(p+q)} \]
     *
     * @param p Argument p
     * @param q Argument q
     * @return beta value
     */
    static double beta(double p, double q) {
        if (!(p > 0 && q > 0)) {
            // Domain error
            return Double.NaN;
        }

        final double c = p + q;

        // Special cases:
        if (c == p && q < EPSILON) {
            return 1 / q;
        } else if (c == q && p < EPSILON) {
            return 1 / p;
        }
        if (q == 1) {
            return 1 / p;
        } else if (p == 1) {
            return 1 / q;
        } else if (c < EPSILON) {
            return (c / p) / q;
        }

        // Create input a > b
        final double a = p < q ? q : p;
        final double b = p < q ? p : q;

        // Lanczos calculation:
        final double agh = a + BoostGamma.Lanczos.GMH;
        final double bgh = b + BoostGamma.Lanczos.GMH;
        final double cgh = c + BoostGamma.Lanczos.GMH;
        double result = BoostGamma.Lanczos.lanczosSumExpGScaled(a) *
                (BoostGamma.Lanczos.lanczosSumExpGScaled(b) / BoostGamma.Lanczos.lanczosSumExpGScaled(c));
        final double ambh = a - 0.5f - b;
        if (Math.abs(b * ambh) < cgh * 100 && a > 100) {
            // Special case where the base of the power term is close to 1
            // compute (1+x)^y instead:
            result *= Math.exp(ambh * Math.log1p(-b / cgh));
        } else {
            result *= Math.pow(agh / cgh, ambh);
        }

        if (cgh > 1e10f) {
            // this avoids possible overflow, but appears to be marginally less accurate:
            result *= Math.pow((agh / cgh) * (bgh / cgh), b);
        } else {
            result *= Math.pow((agh * bgh) / (cgh * cgh), b);
        }
        result *= Math.sqrt(Math.E / bgh);

        return result;
    }

    /**
     * Derivative of the regularised incomplete beta.
     * <p>\[ \frac{\delta}{\delta x} I_x(a, b) = \frac{(1-x)^{b-1} x^{a-1}}{\B(a, b)} \]
     *
     * <p>Adapted from {@code boost::math::ibeta_derivative}.
     *
     * @param a Argument a
     * @param b Argument b
     * @param x Argument x
     * @return ibeta derivative
     */
    static double ibetaDerivative(double a, double b, double x) {
        //
        // start with the usual error checks:
        //
        if (!(a > 0 && b > 0) || !(x >= 0 && x <= 1)) {
            // Domain error
            return Double.NaN;
        }
        //
        // Now the corner cases:
        //
        if (x == 0) {
            if (a > 1) {
                return 0;
            }
            // a == 1 : return 1 / beta(a, b) == b
            return a == 1 ? b : Double.POSITIVE_INFINITY;
        } else if (x == 1) {
            if (b > 1) {
                return 0;
            }
            // b == 1 : return 1 / beta(a, b) == a
            return b == 1 ? a : Double.POSITIVE_INFINITY;
        }

        // Update with extra edge cases
        if (b == 1) {
            // ibeta = x^a
            return a * Math.pow(x, a - 1);
        }
        if (a == 1) {
            // ibeta = 1 - (1-x)^b
            if (x >= 0.5) {
                return b * Math.pow(1 - x, b - 1);
            }
            return b * Math.exp(Math.log1p(-x) * (b - 1));
        }

        //
        // Now the regular cases:
        //
        final double y = (1 - x) * x;
        return ibetaPowerTerms(a, b, x, 1 - x, true, 1 / y);
    }

    /**
     * Compute the leading power terms in the incomplete Beta.
     *
     * <p>Utility function to call
     * {@link #ibetaPowerTerms(double, double, double, double, boolean, double)}
     * using a multiplication prefix of {@code 1.0}.
     *
     * @param a Argument a
     * @param b Argument b
     * @param x Argument x
     * @param y Argument 1-x
     * @param normalised true to divide by beta(a, b)
     * @return incomplete beta power terms
     */
    private static double ibetaPowerTerms(double a, double b, double x,
            double y, boolean normalised) {
        return ibetaPowerTerms(a, b, x, y, normalised, 1);
    }

    /**
     * Compute the leading power terms in the incomplete Beta.
     *
     * <pre>
     * (x^a)(y^b)/Beta(a,b) when normalised, and
     * (x^a)(y^b) otherwise.
     * </pre>
     *
     * <p>Almost all of the error in the incomplete beta comes from this function:
     * particularly when a and b are large. Computing large powers are *hard*
     * though, and using logarithms just leads to horrendous cancellation errors.
     *
     * @param a Argument a
     * @param b Argument b
     * @param x Argument x
     * @param y Argument 1-x
     * @param normalised true to divide by beta(a, b)
     * @param prefix Prefix to multiply by the result
     * @return incomplete beta power terms
     */
    private static double ibetaPowerTerms(double a, double b, double x,
            double y, boolean normalised, double prefix) {
        if (!normalised) {
            // can we do better here?
            return Math.pow(x, a) * Math.pow(y, b);
        }

        double result;

        final double c = a + b;

        // combine power terms with Lanczos approximation:
        final double agh = a + BoostGamma.Lanczos.GMH;
        final double bgh = b + BoostGamma.Lanczos.GMH;
        final double cgh = c + BoostGamma.Lanczos.GMH;
        result = BoostGamma.Lanczos.lanczosSumExpGScaled(c) /
                (BoostGamma.Lanczos.lanczosSumExpGScaled(a) * BoostGamma.Lanczos.lanczosSumExpGScaled(b));
        result *= prefix;
        // combine with the leftover terms from the Lanczos approximation:
        result *= Math.sqrt(bgh / Math.E);
        result *= Math.sqrt(agh / cgh);

        // l1 and l2 are the base of the exponents minus one:
        double l1 = (x * b - y * agh) / agh;
        double l2 = (y * a - x * bgh) / bgh;
        if (Math.min(Math.abs(l1), Math.abs(l2)) < 0.2) {
            // when the base of the exponent is very near 1 we get really
            // gross errors unless extra care is taken:
            if (l1 * l2 > 0 || Math.min(a, b) < 1) {
                //
                // This first branch handles the simple cases where either:
                //
                // * The two power terms both go in the same direction
                // (towards zero or towards infinity). In this case if either
                // term overflows or underflows, then the product of the two must
                // do so also.
                // * Alternatively if one exponent is less than one, then we
                // can't productively use it to eliminate overflow or underflow
                // from the other term. Problems with spurious overflow/underflow
                // can't be ruled out in this case, but it is *very* unlikely
                // since one of the power terms will evaluate to a number close to 1.
                //
                if (Math.abs(l1) < 0.1) {
                    result *= Math.exp(a * Math.log1p(l1));
                } else {
                    result *= Math.pow((x * cgh) / agh, a);
                }
                if (Math.abs(l2) < 0.1) {
                    result *= Math.exp(b * Math.log1p(l2));
                } else {
                    result *= Math.pow((y * cgh) / bgh, b);
                }
            } else if (Math.max(Math.abs(l1), Math.abs(l2)) < 0.5) {
                //
                // Both exponents are near one and both the exponents are
                // greater than one and further these two
                // power terms tend in opposite directions (one towards zero,
                // the other towards infinity), so we have to combine the terms
                // to avoid any risk of overflow or underflow.
                //
                // We do this by moving one power term inside the other, we have:
                //
                //   (1 + l1)^a * (1 + l2)^b
                // = ((1 + l1)*(1 + l2)^(b/a))^a
                // = (1 + l1 + l3 + l1*l3)^a ; l3 = (1 + l2)^(b/a) - 1
                //                                = exp((b/a) * log(1 + l2)) - 1
                //
                // The tricky bit is deciding which term to move inside :-)
                // By preference we move the larger term inside, so that the
                // size of the largest exponent is reduced. However, that can
                // only be done as long as l3 (see above) is also small.
                //
                final boolean smallA = a < b;
                final double ratio = b / a;
                if ((smallA && ratio * l2 < 0.1) || (!smallA && l1 / ratio > 0.1)) {
                    double l3 = Math.expm1(ratio * Math.log1p(l2));
                    l3 = l1 + l3 + l3 * l1;
                    l3 = a * Math.log1p(l3);
                    result *= Math.exp(l3);
                } else {
                    double l3 = Math.expm1(Math.log1p(l1) / ratio);
                    l3 = l2 + l3 + l3 * l2;
                    l3 = b * Math.log1p(l3);
                    result *= Math.exp(l3);
                }
            } else if (Math.abs(l1) < Math.abs(l2)) {
                // First base near 1 only:
                double l = a * Math.log1p(l1) + b * Math.log((y * cgh) / bgh);
                if (l <= LOG_MIN_VALUE || l >= LOG_MAX_VALUE) {
                    l += Math.log(result);
                    // Update: Allow overflow to infinity
                    result = Math.exp(l);
                } else {
                    result *= Math.exp(l);
                }
            } else {
                // Second base near 1 only:
                double l = b * Math.log1p(l2) + a * Math.log((x * cgh) / agh);
                if (l <= LOG_MIN_VALUE || l >= LOG_MAX_VALUE) {
                    l += Math.log(result);
                    // Update: Allow overflow to infinity
                    result = Math.exp(l);
                } else {
                    result *= Math.exp(l);
                }
            }
        } else {
            // general case:
            final double b1 = (x * cgh) / agh;
            final double b2 = (y * cgh) / bgh;
            l1 = a * Math.log(b1);
            l2 = b * Math.log(b2);
            if (l1 >= LOG_MAX_VALUE || l1 <= LOG_MIN_VALUE || l2 >= LOG_MAX_VALUE || l2 <= LOG_MIN_VALUE) {
                // Oops, under/overflow, sidestep if we can:
                if (a < b) {
                    final double p1 = Math.pow(b2, b / a);
                    final double l3 = a * (Math.log(b1) + Math.log(p1));
                    if (l3 < LOG_MAX_VALUE && l3 > LOG_MIN_VALUE) {
                        result *= Math.pow(p1 * b1, a);
                    } else {
                        l2 += l1 + Math.log(result);
                        // Update: Allow overflow to infinity
                        result = Math.exp(l2);
                    }
                } else {
                    final double p1 = Math.pow(b1, a / b);
                    final double l3 = (Math.log(p1) + Math.log(b2)) * b;
                    if (l3 < LOG_MAX_VALUE && l3 > LOG_MIN_VALUE) {
                        result *= Math.pow(p1 * b2, b);
                    } else {
                        l2 += l1 + Math.log(result);
                        // Update: Allow overflow to infinity
                        result = Math.exp(l2);
                    }
                }
            } else {
                // finally the normal case:
                result *= Math.pow(b1, a) * Math.pow(b2, b);
            }
        }

        return result;
    }

    /**
     * Full incomplete beta.
     * <p>\[ B_x(a,b) = \int_0^x t^{a-1}\,(1-t)^{b-1}\,dt \]
     *
     * @param a Argument a
     * @param b Argument b
     * @param x Argument x
     * @return lower beta value
     */
    static double beta(double a, double b, double x) {
        return betaIncompleteImp(a, b, x, Policy.getDefault(), false, false);
    }

    /**
     * Full incomplete beta.
     * <p>\[ B_x(a,b) = \int_0^x t^{a-1}\,(1-t)^{b-1}\,dt \]
     *
     * @param a Argument a
     * @param b Argument b
     * @param x Argument x
     * @param policy Function evaluation policy
     * @return lower beta value
     */
    static double beta(double a, double b, double x, Policy policy) {
        return betaIncompleteImp(a, b, x, policy, false, false);
    }

    /**
     * Complement of the incomplete beta.
     * <p>\[ betac(a, b, x) = beta(b, a, 1-x) = B_{1-x}(b,a) \]
     *
     * @param a Argument a
     * @param b Argument b
     * @param x Argument x
     * @return upper beta value
     */
    static double betac(double a, double b, double x) {
        return betaIncompleteImp(a, b, x, Policy.getDefault(), false, true);
    }

    /**
     * Complement of the incomplete beta.
     * <p>\[ betac(a, b, x) = beta(b, a, 1-x) = B_{1-x}(b,a) \]
     *
     * @param a Argument a
     * @param b Argument b
     * @param x Argument x
     * @param policy Function evaluation policy
     * @return upper beta value
     */
    static double betac(double a, double b, double x, Policy policy) {
        return betaIncompleteImp(a, b, x, policy, false, true);
    }

    /**
     * Regularised incomplete beta.
     * <p>\[ I_x(a,b) = \frac{1}{B(a, b)} \int_0^x t^{a-1}\,(1-t)^{b-1}\,dt \]
     *
     * @param a Argument a
     * @param b Argument b
     * @param x Argument x
     * @return p
     */
    static double ibeta(double a, double b, double x) {
        return betaIncompleteImp(a, b, x, Policy.getDefault(), true, false);
    }

    /**
     * Regularised incomplete beta.
     * <p>\[ I_x(a,b) = \frac{1}{B(a, b)} \int_0^x t^{a-1}\,(1-t)^{b-1}\,dt \]
     *
     * @param a Argument a
     * @param b Argument b
     * @param x Argument x
     * @param policy Function evaluation policy
     * @return p
     */
    static double ibeta(double a, double b, double x, Policy policy) {
        return betaIncompleteImp(a, b, x, policy, true, false);
    }

    /**
     * Complement of the regularised incomplete beta.
     * <p>\[ ibetac(a, b, x) = 1 - I_x(a,b) = I_{1-x}(b, a) \]
     *
     * @param a Argument a
     * @param b Argument b
     * @param x Argument x
     * @return q
     */
    static double ibetac(double a, double b, double x) {
        return betaIncompleteImp(a, b, x, Policy.getDefault(), true, true);
    }

    /**
     * Complement of the regularised incomplete beta.
     * <p>\[ ibetac(a, b, x) = 1 - I_x(a,b) = I_{1-x}(b, a) \]
     *
     * @param a Argument a
     * @param b Argument b
     * @param x Argument x
     * @param policy Function evaluation policy
     * @return q
     */
    static double ibetac(double a, double b, double x, Policy policy) {
        return betaIncompleteImp(a, b, x, policy, true, true);
    }

    /**
     * Main incomplete beta entry point, handles all four incomplete betas.
     * Adapted from {@code boost::math::detail::ibeta_imp}.
     *
     * <p>The Boost code has a pointer {@code p_derivative} that can be set to the
     * value of the derivative. This is used for the inverse incomplete
     * beta functions. It is not required for the forward evaluation functions.
     *
     * @param a Argument a
     * @param b Argument b
     * @param x Argument x
     * @param pol Function evaluation policy
     * @param normalised true to compute the regularised value
     * @param inv true to compute the complement value
     * @return incomplete beta value
     */
    private static double betaIncompleteImp(double a, double b, double x,
            Policy pol, boolean normalised, boolean inv) {
        //
        // The incomplete beta function implementation:
        // This is just a big bunch of spaghetti code to divide up the
        // input range and select the right implementation method for
        // each domain:
        //

        if (!(x >= 0 && x <= 1)) {
            // Domain error
            return Double.NaN;
        }

        if (normalised) {
            if (!(a >= 0 && b >= 0)) {
                // Domain error
                return Double.NaN;
            }
            // extend to a few very special cases:
            if (a == 0) {
                if (b == 0) {
                    // a and b cannot both be zero
                    return Double.NaN;
                }
                // Assume b > 0
                return inv ? 0 : 1;
            } else if (b == 0) {
                // assume a > 0
                return inv ? 1 : 0;
            }
        } else {
            if (!(a > 0 && b > 0)) {
                // Domain error
                return Double.NaN;
            }
        }

        if (x == 0) {
            if (inv) {
                return normalised ? 1 : beta(a, b);
            }
            return 0;
        }
        if (x == 1) {
            if (!inv) {
                return normalised ? 1 : beta(a, b);
            }
            return 0;
        }

        if (a == 0.5f && b == 0.5f) {
            // We have an arcsine distribution:
            final double z = inv ? 1 - x : x;
            final double asin = Math.asin(Math.sqrt(z));
            return normalised ? asin / HALF_PI : 2 * asin;
        }

        boolean invert = inv;
        double y = 1 - x;
        if (a == 1) {
            // swap(a, b)
            double tmp = a;
            a = b;
            b = tmp;
            // swap(x, y)
            tmp = x;
            x = y;
            y = tmp;
            invert = !invert;
        }
        if (b == 1) {
            //
            // Special case see:
            // http://functions.wolfram.com/GammaBetaErf/BetaRegularized/03/01/01/
            //
            if (a == 1) {
                return invert ? y : x;
            }

            double p;
            if (y < 0.5) {
                p = invert ? -Math.expm1(a * Math.log1p(-y)) : Math.exp(a * Math.log1p(-y));
            } else {
                p = invert ? -BoostMath.powm1(x, a) : Math.pow(x, a);
            }
            if (!normalised) {
                p /= a;
            }
            return p;
        }

        double fract;
        if (Math.min(a, b) <= 1) {
            if (x > 0.5) {
                // swap(a, b)
                double tmp = a;
                a = b;
                b = tmp;
                // swap(x, y)
                tmp = x;
                x = y;
                y = tmp;
                invert = !invert;
            }
            if (Math.max(a, b) <= 1) {
                // Both a,b < 1:
                if (a >= Math.min(0.2, b) || Math.pow(x, a) <= 0.9) {
                    if (invert) {
                        fract = -(normalised ? 1 : beta(a, b));
                        invert = false;
                        fract = -ibetaSeries(a, b, x, fract, normalised, pol);
                    } else {
                        fract = ibetaSeries(a, b, x, 0, normalised, pol);
                    }
                } else {
                    // swap(a, b)
                    double tmp = a;
                    a = b;
                    b = tmp;
                    // swap(x, y)
                    tmp = x;
                    x = y;
                    y = tmp;
                    invert = !invert;
                    if (y >= 0.3) {
                        if (invert) {
                            fract = -(normalised ? 1 : beta(a, b));
                            invert = false;
                            fract = -ibetaSeries(a, b, x, fract, normalised, pol);
                        } else {
                            fract = ibetaSeries(a, b, x, 0, normalised, pol);
                        }
                    } else {
                        // Sidestep on a, and then use the series representation:
                        double prefix;
                        if (normalised) {
                            prefix = 1;
                        } else {
                            prefix = risingFactorialRatio(a + b, a, 20);
                        }
                        fract = ibetaAStep(a, b, x, y, 20, normalised);
                        if (invert) {
                            fract -= normalised ? 1 : beta(a, b);
                            invert = false;
                            fract = -betaSmallBLargeASeries(a + 20, b, x, y, fract, prefix, pol, normalised);
                        } else {
                            fract = betaSmallBLargeASeries(a + 20, b, x, y, fract, prefix, pol, normalised);
                        }
                    }
                }
            } else {
                // One of a, b < 1 only:
                if (b <= 1 || (x < 0.1 && Math.pow(b * x, a) <= 0.7)) {
                    if (invert) {
                        fract = -(normalised ? 1 : beta(a, b));
                        invert = false;
                        fract = -ibetaSeries(a, b, x, fract, normalised, pol);
                    } else {
                        fract = ibetaSeries(a, b, x, 0, normalised, pol);
                    }
                } else {
                    // swap(a, b)
                    double tmp = a;
                    a = b;
                    b = tmp;
                    // swap(x, y)
                    tmp = x;
                    x = y;
                    y = tmp;
                    invert = !invert;

                    if (y >= 0.3) {
                        if (invert) {
                            fract = -(normalised ? 1 : beta(a, b));
                            invert = false;
                            fract = -ibetaSeries(a, b, x, fract, normalised, pol);
                        } else {
                            fract = ibetaSeries(a, b, x, 0, normalised, pol);
                        }
                    } else if (a >= 15) {
                        if (invert) {
                            fract = -(normalised ? 1 : beta(a, b));
                            invert = false;
                            fract = -betaSmallBLargeASeries(a, b, x, y, fract, 1, pol, normalised);
                        } else {
                            fract = betaSmallBLargeASeries(a, b, x, y, 0, 1, pol, normalised);
                        }
                    } else {
                        // Sidestep to improve errors:
                        double prefix;
                        if (normalised) {
                            prefix = 1;
                        } else {
                            prefix = risingFactorialRatio(a + b, a, 20);
                        }
                        fract = ibetaAStep(a, b, x, y, 20, normalised);
                        if (invert) {
                            fract -= normalised ? 1 : beta(a, b);
                            invert = false;
                            fract = -betaSmallBLargeASeries(a + 20, b, x, y, fract, prefix, pol, normalised);
                        } else {
                            fract = betaSmallBLargeASeries(a + 20, b, x, y, fract, prefix, pol, normalised);
                        }
                    }
                }
            }
        } else {
            // Both a,b >= 1:
            // Note:
            // median ~ (a - 1/3) / (a + b - 2/3) ~ a / (a + b)
            // if x > a / (a + b) => a - (a + b) * x < 0
            double lambda;
            if (a < b) {
                lambda = a - (a + b) * x;
            } else {
                lambda = (a + b) * y - b;
            }
            if (lambda < 0) {
                // swap(a, b)
                double tmp = a;
                a = b;
                b = tmp;
                // swap(x, y)
                tmp = x;
                x = y;
                y = tmp;
                invert = !invert;
            }

            if (b < 40) {
                // Note: y != 1 check is required for non-zero x < epsilon
                if (Math.rint(a) == a && Math.rint(b) == b && a < (Integer.MAX_VALUE - 100) && y != 1) {
                    // Here: a in [2, 2^31 - 102] && b in [2, 39]

                    // relate to the binomial distribution and use a finite sum:
                    final int k = (int) (a - 1);
                    final int n = (int) (b + k);
                    fract = binomialCCdf(n, k, x, y);
                    if (!normalised) {
                        fract *= beta(a, b);
                    }
                } else if (b * x <= 0.7) {
                    if (invert) {
                        fract = -(normalised ? 1 : beta(a, b));
                        invert = false;
                        fract = -ibetaSeries(a, b, x, fract, normalised, pol);
                    } else {
                        fract = ibetaSeries(a, b, x, 0, normalised, pol);
                    }
                } else if (a > 15) {
                    // sidestep so we can use the series representation:
                    int n = (int) b;
                    if (n == b) {
                        --n;
                    }
                    final double bbar = b - n;
                    double prefix;
                    if (normalised) {
                        prefix = 1;
                    } else {
                        prefix = risingFactorialRatio(a + bbar, bbar, n);
                    }
                    fract = ibetaAStep(bbar, a, y, x, n, normalised);
                    fract = betaSmallBLargeASeries(a, bbar, x, y, fract, 1, pol, normalised);
                    fract /= prefix;
                } else if (normalised) {
                    // The formula here for the non-normalised case is tricky to figure
                    // out (for me!!), and requires two pochhammer calculations rather
                    // than one, so leave it for now and only use this in the normalized case....
                    int n = (int) Math.floor(b);
                    double bbar = b - n;
                    if (bbar <= 0) {
                        --n;
                        bbar += 1;
                    }
                    fract = ibetaAStep(bbar, a, y, x, n, normalised);
                    fract += ibetaAStep(a, bbar, x, y, 20, normalised);
                    if (invert) {
                        // Note this line would need changing if we ever enable this branch in
                        // non-normalized case
                        fract -= 1;
                    }
                    fract = betaSmallBLargeASeries(a + 20, bbar, x, y, fract, 1, pol, normalised);
                    if (invert) {
                        fract = -fract;
                        invert = false;
                    }
                } else {
                    fract = ibetaFraction2(a, b, x, y, pol, normalised);
                }
            } else {
                fract = ibetaFraction2(a, b, x, y, pol, normalised);
            }
        }
        if (invert) {
            return (normalised ? 1 : beta(a, b)) - fract;
        }
        return fract;
    }

    /**
     * Series approximation to the incomplete beta.
     *
     * @param a Argument a
     * @param b Argument b
     * @param x Argument x
     * @param s0 Initial sum for the series
     * @param normalised true to compute the regularised value
     * @param pol Function evaluation policy
     * @return incomplete beta series
     */
    private static double ibetaSeries(double a, double b, double x, double s0, boolean normalised, Policy pol) {
        double result;

        if (normalised) {
            final double c = a + b;

            // incomplete beta power term, combined with the Lanczos approximation:
            final double agh = a + BoostGamma.Lanczos.GMH;
            final double bgh = b + BoostGamma.Lanczos.GMH;
            final double cgh = c + BoostGamma.Lanczos.GMH;
            result = BoostGamma.Lanczos.lanczosSumExpGScaled(c) /
                    (BoostGamma.Lanczos.lanczosSumExpGScaled(a) * BoostGamma.Lanczos.lanczosSumExpGScaled(b));

            final double l1 = Math.log(cgh / bgh) * (b - 0.5f);
            final double l2 = Math.log(x * cgh / agh) * a;
            //
            // Check for over/underflow in the power terms:
            //
            if (l1 > LOG_MIN_VALUE && l1 < LOG_MAX_VALUE && l2 > LOG_MIN_VALUE && l2 < LOG_MAX_VALUE) {
                if (a * b < bgh * 10) {
                    result *= Math.exp((b - 0.5f) * Math.log1p(a / bgh));
                } else {
                    result *= Math.pow(cgh / bgh, b - 0.5f);
                }
                result *= Math.pow(x * cgh / agh, a);
                result *= Math.sqrt(agh / Math.E);
            } else {
                //
                // Oh dear, we need logs, and this *will* cancel:
                //
                result = Math.log(result) + l1 + l2 + (Math.log(agh) - 1) / 2;
                result = Math.exp(result);
            }
        } else {
            // Non-normalised, just compute the power:
            result = Math.pow(x, a);
        }
        double rescale = 1.0;
        if (result < Double.MIN_NORMAL) {
            // Safeguard: series can't cope with denorms.

            // Update:
            // The entire series is only based on the magnitude of 'result'.
            // If the first term can be added to s0 (e.g. if s0 == 0) then
            // scale s0 and result, compute the series and then rescale.

            // Intentional floating-point equality check.
            if (s0 + result / a == s0) {
                return s0;
            }
            s0 *= TWO_POW_53;
            result *= TWO_POW_53;
            rescale = TWO_POW_M53;
        }

        final double eps = pol.getEps();
        final int maxIterations = pol.getMaxIterations();

        // Create effectively final 'result' for initialisation
        final double result1 = result;
        final DoubleSupplier gen = new DoubleSupplier() {
            /** Next result term. */
            private double result = result1;
            /** Pochhammer term. */
            private final double poch = -b;
            /** Iteration. */
            private int n;

            @Override
            public double getAsDouble() {
                final double r = result / (a + n);
                n++;
                result *= (n + poch) * x / n;
                return r;
            }
        };

        return BoostTools.sumSeries(gen, eps, maxIterations, s0) * rescale;
    }

    /**
     * Rising factorial ratio.
     * This function is only needed for the non-regular incomplete beta,
     * it computes the delta in:
     * <pre>
     * beta(a,b,x) = prefix + delta * beta(a+k,b,x)
     * </pre>
     * <p>It is currently only called for small k.
     *
     * @param a Argument a
     * @param b Argument b
     * @param k Argument k
     * @return the rising factorial ratio
     */
    private static double risingFactorialRatio(double a, double b, int k) {
        // calculate:
        // (a)(a+1)(a+2)...(a+k-1)
        // _______________________
        // (b)(b+1)(b+2)...(b+k-1)

        // This is only called with small k, for large k
        // it is grossly inefficient, do not use outside it's
        // intended purpose!!!
        double result = 1;
        for (int i = 0; i < k; ++i) {
            result *= (a + i) / (b + i);
        }
        return result;
    }

    /**
     * Binomial complementary cdf.
     * For integer arguments we can relate the incomplete beta to the
     * complement of the binomial distribution cdf and use this finite sum.
     *
     * @param n Argument n (called with {@code [2, 39] + k})
     * @param k Argument k (called with {@code k in [1, Integer.MAX_VALUE - 102]})
     * @param x Argument x
     * @param y Argument 1-x
     * @return Binomial complementary cdf
     */
    private static double binomialCCdf(int n, int k, double x, double y) {
        double result = Math.pow(x, n);

        if (result > Double.MIN_NORMAL) {
            double term = result;
            for (int i = n - 1; i > k; --i) {
                term *= ((i + 1) * y) / ((n - i) * x);
                result += term;
            }
        } else {
            // First term underflows so we need to start at the mode of the
            // distribution and work outwards:
            int start = (int) (n * x);
            if (start <= k + 1) {
                start = k + 2;
            }
            // Update:
            // Carefully compute this term to guard against extreme parameterisation
            result = binomialTerm(n, start, x, y);
            if (result == 0) {
                // OK, starting slightly above the mode didn't work,
                // we'll have to sum the terms the old fashioned way:
                for (int i = start - 1; i > k; --i) {
                    result += binomialTerm(n, i, x, y);
                }
            } else {
                double term = result;
                final double startTerm = result;
                for (int i = start - 1; i > k; --i) {
                    term *= ((i + 1) * y) / ((n - i) * x);
                    result += term;
                }
                term = startTerm;
                for (int i = start + 1; i <= n; ++i) {
                    term *= (n - i + 1) * x / (i * y);
                    result += term;
                }
            }
        }

        return result;
    }

    /**
     * Compute the binomial term.
     * <pre>
     * x^k * (1-x)^(n-k) * binomial(n, k)
     * </pre>
     * <p>This is a helper function used to guard against extreme values generated
     * in the term which can produce NaN from zero multiplied by infinity.
     *
     * @param n Argument n
     * @param k Argument k
     * @param x Argument x
     * @param y Argument 1-x
     * @return Binomial term
     */
    private static double binomialTerm(int n, int k, double x, double y) {
        // This function can be called with the following extreme that will overflow:
        // binomial(2147483545 + 39, 38) ~ 7.84899e309
        // Guard this as the power functions are very likely to be zero with large n and k.

        final double binom = binomialCoefficient(n, k);
        if (!Double.isFinite(binom)) {
            // Product of the power functions will be zero with large n and k
            return 0;
        }

        // The power terms are below 1.
        // Multiply by the largest so that any sub-normal term is used last
        // This method is called where x^n is sub-normal so assume the other term is larger.
        return binom * Math.pow(y, n - k) * Math.pow(x, k);
    }

    /**
     * Computes the binomial coefficient.
     *
     * <p>Adapted from
     * {@code org.apache.commons.numbers.combinatorics.BinomialCoefficientDouble}.
     *
     * <p>Note: This does not use {@code BinomialCoefficientDouble}
     * to avoid a circular dependency as the combinatorics depends on the gamma package.
     * No checks are made on the arguments.
     *
     * @param n Size of the set (must be positive).
     * @param k Size of the subsets to be counted (must be in [0, n]).
     * @return {@code n choose k}.
     */
    static double binomialCoefficient(int n, int k) {
        // Assume: n >= 0; 0 <= k <= n
        // Use symmetry
        final int m = Math.min(k, n - k);

        // This function is typically called with m <= 3 so handle these special cases
        if (m == 0) {
            return 1;
        }
        if (m == 1) {
            return n;
        }
        if (m == 2) {
            return 0.5 * n * (n - 1);
        }
        if (m == 3) {
            // Divide by 3 at the end to avoid using an 1/6 inexact initial multiplier.
            return 0.5 * n * (n - 1) * (n - 2) / 3;
        }

        double result;
        if (n <= MAX_FACTORIAL) {
            // Use fast table lookup:
            result = BoostGamma.uncheckedFactorial(n);
            // Smaller m will have a more accurate factorial
            result /= BoostGamma.uncheckedFactorial(m);
            result /= BoostGamma.uncheckedFactorial(n - m);
        } else {
            // Updated:
            // Do not use the beta function as per <boost/math/special_functions/binomial.hpp>,
            // e.g. result = 1 / (m * beta(m, n - m + 1)) == gamma(n+1) / (m * gamma(m) * gamma(n-m+1))

            // Use a summation as per BinomialCoefficientDouble which is more accurate
            // than using the beta function. This is only used up to m = 39 so
            // may overflow *only* on the final term (i.e. m << n when overflow can occur).

            result = 1;
            for (int i = 1; i < m; i++) {
                result *= n - m + i;
                result /= i;
            }
            // Final term may cause overflow.
            if (result * n > Double.MAX_VALUE) {
                result /= m;
                result *= n;
            } else {
                result *= n;
                result /= m;
            }
        }
        // convert to nearest integer:
        return Math.ceil(result - 0.5f);
    }

    /**
     * Computes the difference between ibeta(a,b,x) and ibeta(a+k,b,x).
     *
     * @param a Argument a
     * @param b Argument b
     * @param x Argument x
     * @param y Argument 1-x
     * @param k Argument k
     * @param normalised true to compute the regularised value
     * @return ibeta difference
     */
    private static double ibetaAStep(double a, double b, double x, double y, int k, boolean normalised) {
        double prefix = ibetaPowerTerms(a, b, x, y, normalised);
        prefix /= a;
        if (prefix == 0) {
            return prefix;
        }
        double sum = 1;
        double term = 1;
        // series summation from 0 to k-1:
        for (int i = 0; i < k - 1; ++i) {
            term *= (a + b + i) * x / (a + i + 1);
            sum += term;
        }
        prefix *= sum;

        return prefix;
    }

    /**
     * Computes beta(a, b, x) using small b and large a.
     *
     * @param a Argument a
     * @param b Argument b
     * @param x Argument x
     * @param y Argument 1-x
     * @param s0 Initial sum for the series
     * @param mult Multiplication prefix factor
     * @param pol Function evaluation policy
     * @param normalised true to compute the regularised value
     * @return beta series
     */
    private static double betaSmallBLargeASeries(double a, double b, double x, double y, double s0, double mult,
            Policy pol, boolean normalised) {
        //
        // This is DiDonato and Morris's BGRAT routine, see Eq's 9 through 9.6.
        //
        // Some values we'll need later, these are Eq 9.1:
        //
        final double bm1 = b - 1;
        final double t = a + bm1 / 2;
        double lx;
        double u;
        if (y < 0.35) {
            lx = Math.log1p(-y);
        } else {
            lx = Math.log(x);
        }
        u = -t * lx;
        // and from from 9.2:
        final double h = BoostGamma.regularisedGammaPrefix(b, u);
        if (h <= Double.MIN_NORMAL) {
            // Update:
            // Boost returns s0.
            // If this is zero then compute an expected sub-normal value
            // using the classic continued fraction representation.
            if (s0 == 0) {
                return ibetaFraction(a, b, x, y, pol, normalised);
            }
            return s0;
        }
        double prefix;
        if (normalised) {
            prefix = h / GammaRatio.delta(a, b);
            prefix /= Math.pow(t, b);
        } else {
            prefix = BoostGamma.fullIgammaPrefix(b, u) / Math.pow(t, b);
        }
        prefix *= mult;
        //
        // now we need the quantity Pn, unfortunately this is computed
        // recursively, and requires a full history of all the previous values
        // so no choice but to declare a big table and hope it's big enough...
        //
        final double[] p = new double[PN_SIZE];
        // see 9.3.
        p[0] = 1;
        //
        // Now an initial value for J, see 9.6:
        //
        double j = BoostGamma.gammaQ(b, u, pol) / h;
        //
        // Now we can start to pull things together and evaluate the sum in Eq 9:
        //
        // Value at N = 0
        double sum = s0 + prefix * j;
        // some variables we'll need:
        // 2*N+1
        int tnp1 = 1;
        double lx2 = lx / 2;
        lx2 *= lx2;
        double lxp = 1;
        final double t4 = 4 * t * t;
        double b2n = b;

        for (int n = 1; n < PN_SIZE; ++n) {
            //
            // begin by evaluating the next Pn from Eq 9.4:
            //
            tnp1 += 2;
            p[n] = 0;
            int tmp1 = 3;
            for (int m = 1; m < n; ++m) {
                final double mbn = m * b - n;
                p[n] += mbn * p[n - m] / BoostGamma.uncheckedFactorial(tmp1);
                tmp1 += 2;
            }
            p[n] /= n;
            p[n] += bm1 / BoostGamma.uncheckedFactorial(tnp1);
            //
            // Now we want Jn from Jn-1 using Eq 9.6:
            //
            j = (b2n * (b2n + 1) * j + (u + b2n + 1) * lxp) / t4;
            lxp *= lx2;
            b2n += 2;
            //
            // pull it together with Eq 9:
            //
            final double r = prefix * p[n] * j;
            final double previous = sum;
            sum += r;
            // Update:
            // This continues until convergence at machine epsilon
            // |r| < eps * |sum|; r < 1
            // |r| / eps < |sum|; r > 1
            if (sum == previous) {
                break;
            }
        }
        return sum;
    }

    /**
     * Evaluate the incomplete beta via a continued fraction representation.
     *
     * <p>Note: This is not a generic continued fraction. The formula is from <a
     * href="https://dl.acm.org/doi/10.1145/131766.131776">Didonato and Morris</a> and is
     * only used when a and b are above 1. See <a
     * href="https://www.boost.org/doc/libs/1_77_0/libs/math/doc/html/math_toolkit/sf_beta/ibeta_function.html">Incomplete
     * Beta Function: Implementation</a>.
     *
     * @param a Argument a
     * @param b Argument b
     * @param x Argument x
     * @param y Argument 1-x
     * @param pol Function evaluation policy
     * @param normalised true to compute the regularised value
     * @return incomplete beta
     */
    static double ibetaFraction2(double a, double b, double x, double y, Policy pol, boolean normalised) {
        final double result = ibetaPowerTerms(a, b, x, y, normalised);
        if (result == 0) {
            return result;
        }

        final double eps = pol.getEps();
        final int maxIterations = pol.getMaxIterations();

        final Supplier<Coefficient> gen = new Supplier<Coefficient>() {
            /** Iteration. */
            private int m;

            @Override
            public Coefficient get() {
                double aN = (a + m - 1) * (a + b + m - 1) * m * (b - m) * x * x;
                final double denom = a + 2 * m - 1;
                aN /= denom * denom;

                double bN = m;
                bN += (m * (b - m) * x) / (a + 2 * m - 1);
                bN += ((a + m) * (a * y - b * x + 1 + m * (2 - x))) / (a + 2 * m + 1);

                ++m;
                return Coefficient.of(aN, bN);
            }
        };

        // Note: The first generated term a0 is discarded
        final double fract = GeneralizedContinuedFraction.value(gen, eps, maxIterations);
        return result / fract;
    }

    /**
     * Evaluate the incomplete beta via the classic continued fraction representation.
     *
     * <p>Note: This is not part of the Boost C++ implementation.
     * This is a generic continued fraction applicable to all arguments.
     * It is used when alternative methods are unsuitable due to the presence of sub-normal
     * terms and the result is expected to be sub-normal. In this case the original Boost
     * implementation would return zero.
     *
     * <p>This continued fraction was the evaluation method used in Commons Numbers 1.0.
     * This method has been updated to use the {@code ibetaPowerTerms} function to compute
     * the power terms. Reversal of the arguments to call {@code 1 - ibetaFraction(b, a, 1 - x)}
     * is not performed as the result is expected to be very small and this optimisation
     * for accuracy is not required.
     *
     * @param a Argument a
     * @param b Argument b
     * @param x Argument x
     * @param y Argument 1-x
     * @param pol Function evaluation policy
     * @param normalised true to compute the regularised value
     * @return incomplete beta
     */
    static double ibetaFraction(double a, double b, double x, double y, Policy pol, boolean normalised) {
        final double result = ibetaPowerTerms(a, b, x, y, normalised);
        if (result == 0) {
            return result;
        }

        final double eps = pol.getEps();
        final int maxIterations = pol.getMaxIterations();

        final Supplier<Coefficient> gen = new Supplier<Coefficient>() {
            /** Iteration. */
            private int n;

            @Override
            public Coefficient get() {
                // https://functions.wolfram.com/GammaBetaErf/Beta3/10/0001/

                final int m = n;
                final int k = m / 2;
                double aN;
                if ((m & 0x1) == 0) {
                    // even
                    // m = 2k
                    aN = (k * (b - k) * x) /
                        ((a + m - 1) * (a + m));
                } else {
                    // odd
                    // k = (m - 1) / 2 due to integer truncation
                    // m - 1 = 2k
                    aN = -((a + k) * (a + b + k) * x) /
                        ((a + m - 1) * (a + m));
                }

                n = m + 1;
                return Coefficient.of(aN, 1);
            }
        };

        // Note: The first generated term a0 is discarded
        final double fract = GeneralizedContinuedFraction.value(gen, eps, maxIterations);
        return (result / a) / fract;
    }
}
