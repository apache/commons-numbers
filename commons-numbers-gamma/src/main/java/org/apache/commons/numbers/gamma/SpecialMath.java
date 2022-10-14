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
package org.apache.commons.numbers.gamma;

/**
 * Special math functions.
 *
 * @since 1.1
 */
final class SpecialMath {
    /** Minimum x for log1pmx(x). */
    private static final double X_MIN = -1;
    /** Low threshold to use log1p(x) - x. */
    private static final double X_LOW = -0.79149064;
    /** High threshold to use log1p(x) - x. */
    private static final double X_HIGH = 1;
    /** 2^-6. */
    private static final double TWO_POW_M6 = 0x1.0p-6;
    /** 2^-12. */
    private static final double TWO_POW_M12 = 0x1.0p-12;
    /** 2^-20. */
    private static final double TWO_POW_M20 = 0x1.0p-20;
    /** 2^-53. */
    private static final double TWO_POW_M53 = 0x1.0p-53;

    /** Private constructor. */
    private SpecialMath() {
        // intentionally empty.
    }

    /**
     * Returns {@code log(1 + x) - x}. This function is accurate when {@code x -> 0}.
     *
     * <p>This function uses a Taylor series expansion when x is small ({@code |x| < 0.01}):
     *
     * <pre>
     * ln(1 + x) - x = -x^2/2 + x^3/3 - x^4/4 + ...
     * </pre>
     *
     * <p>or around 0 ({@code -0.791 <= x <= 1}):
     *
     * <pre>
     * ln(x + a) = ln(a) + 2 [z + z^3/3 + z^5/5 + z^7/7 + ... ]
     *
     * z = x / (2a + x)
     * </pre>
     *
     * <p>For a = 1:
     *
     * <pre>
     * ln(x + 1) - x = -x + 2 [z + z^3/3 + z^5/5 + z^7/7 + ... ]
     *               = z * (-x + 2z^2 [ 1/3 + z^2/5 + z^4/7 + ... ])
     * </pre>
     *
     * <p>The code is based on the {@code log1pmx} documentation for the <a
     * href="https://rdrr.io/rforge/DPQ/man/log1pmx.html">R DPQ package</a> with addition of the
     * direct Taylor series for tiny x.
     *
     * <p>See Abramowitz, M. and Stegun, I. A. (1972) Handbook of Mathematical Functions. New York:
     * Dover. Formulas 4.1.24 and 4.2.29, p.68. <a
     * href="https://en.wikipedia.org/wiki/Abramowitz_and_Stegun">Wikipedia: Abramowitz_and_Stegun</a>
     * provides links to the full text which is in public domain.
     *
     * @param x Value x
     * @return {@code log(1 + x) - x}
     */
    static double log1pmx(double x) {
        // -1 is the minimum supported value
        if (x <= X_MIN) {
            return x == X_MIN ? Double.NEGATIVE_INFINITY : Double.NaN;
        }
        // Use the thresholds documented in the R implementation
        if (x < X_LOW || x > X_HIGH) {
            return Math.log1p(x) - x;
        }
        final double a = Math.abs(x);

        // Addition to the R version for small x.
        // Use a direct Taylor series:
        // ln(1 + x) = x - x^2/2 + x^3/3 - x^4/4 + ...
        if (a < TWO_POW_M6) {
            return log1pmxSmall(x, a);
        }

        // The use of the following series is fast converging:
        // ln(x + 1) - x = -x + 2 [z + z^3/3 + z^5/5 + z^7/7 + ... ]
        //               = z * (-x + 2z^2 [ 1/3 + z^2/5 + z^4/7 + ... ])
        // z = x / (2 + x)
        //
        // Tests show this is more accurate when |x| > 1e-4 than the direct Taylor series.
        // The direct series can be modified to sum multiple terms together for a small
        // increase in precision to a closer match to this variation but the direct series
        // takes approximately 3x longer to converge.

        final double z = x / (2 + x);
        final double zz = z * z;

        // Series sum
        // sum(k=0,...,Inf; zz^k/(3+k*2)) = 1/3 + zz/5 + zz^2/7 + zz^3/9 + ... )

        double sum = 1.0 / 3;
        double numerator = 1;
        int denominator = 3;
        for (;;) {
            numerator *= zz;
            denominator += 2;
            final double sum2 = sum + numerator / denominator;
            // Since |x| <= 1 the additional terms will reduce in magnitude.
            // Iterate until convergence. Expected iterations:
            // x      iterations
            // -0.79  38
            // -0.5   15
            // -0.1    5
            //  0.1    5
            //  0.5   10
            //  1.0   15
            if (sum2 == sum) {
                break;
            }
            sum = sum2;
        }
        return z * (2 * zz * sum - x);
    }

    /**
     * Returns {@code log(1 + x) - x}. This function is accurate when
     * {@code x -> 0}.
     *
     * <p>This function uses a Taylor series expansion when x is small
     * ({@code |x| < 0.01}):
     *
     * <pre>
     * ln(1 + x) - x = -x^2/2 + x^3/3 - x^4/4 + ...
     * </pre>
     *
     * <p>No loop iterations are used as the series is directly expanded
     * for a set number of terms based on the absolute value of x.
     *
     * @param x Value x (assumed to be small)
     * @param a Absolute value of x
     * @return {@code log(1 + x) - x}
     */
    private static double log1pmxSmall(double x, double a) {
        // Use a direct Taylor series:
        // ln(1 + x) = x - x^2/2 + x^3/3 - x^4/4 + ...
        // Reverse the summation (small to large) for a marginal increase in precision.
        // To stop the Taylor series the next term must be less than 1 ulp from the
        // answer.
        // x^n/n < |log(1+x)-x| * eps
        // eps = machine epsilon = 2^-53
        // x^n < |log(1+x)-x| * eps
        // n < (log(|log(1+x)-x|) + log(eps)) / log(x)
        // In practice this is a conservative limit.

        final double x2 = x * x;

        if (a < TWO_POW_M53) {
            // Below machine epsilon. Addition of x^3/3 is not possible.
            // Subtract from zero to prevent creating -0.0 for x=0.
            return 0 - x2 / 2;
        }

        final double x4 = x2 * x2;

        // +/-9.5367431640625e-07: log1pmx = -4.547470617660916e-13 :
        // -4.5474764000725028e-13
        // n = 4.69
        if (a < TWO_POW_M20) {
            // n=5
            return x * x4 / 5 -
                       x4 / 4 +
                   x * x2 / 3 -
                       x2 / 2;
        }

        // +/-2.44140625E-4: log1pmx = -2.9797472637290841e-08 : -2.9807173914456693e-08
        // n = 6.49
        if (a < TWO_POW_M12) {
            // n=7
            return x * x2 * x4 / 7 -
                       x2 * x4 / 6 +
                        x * x4 / 5 -
                            x4 / 4 +
                        x * x2 / 3 -
                            x2 / 2;
        }

        // Assume |x| < 2^-6
        // +/-0.015625: log1pmx = -0.00012081346403474586 : -0.00012335696813916864
        // n = 10.9974

        // n=11
        final double x8 = x4 * x4;
        return x * x2 * x8 / 11 -
                   x2 * x8 / 10 +
                    x * x8 /  9 -
                        x8 /  8 +
               x * x2 * x4 /  7 -
                   x2 * x4 /  6 +
                    x * x4 /  5 -
                        x4 /  4 +
                    x * x2 /  3 -
                        x2 /  2;
    }
}
