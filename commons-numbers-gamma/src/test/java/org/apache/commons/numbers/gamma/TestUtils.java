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

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.function.DoubleConsumer;
import java.util.function.LongConsumer;
import java.util.function.Supplier;
import org.junit.jupiter.api.Assertions;

/**
 * Test utilities.
 */
final class TestUtils {
    /** Positive zero bits. */
    private static final long POSITIVE_ZERO_DOUBLE_BITS = Double.doubleToRawLongBits(+0.0);
    /** Negative zero bits. */
    private static final long NEGATIVE_ZERO_DOUBLE_BITS = Double.doubleToRawLongBits(-0.0);
    /** Set this to true to report all deviations to System out when the maximum ULPs is negative. */
    private static boolean reportAllDeviations = false;

    /**
     * Class to compute the error statistics.
     *
     * <p>This class can be used to summary errors if used as the DoubleConsumer
     * argument to {@link TestUtils#assertEquals(BigDecimal, double, double)}.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Root_mean_square">Wikipedia: RMS</a>
     */
    static class ErrorStatistics {
        /** Sum of squared error. */
        private double ss;
        /** Maximum absolute error. */
        private double maxAbs;
        /** Number of terms. */
        private int n;
        /** Positive sum. */
        private double ps;
        /** Positive sum round-off compensation. */
        private double psc;
        /** Negative sum. */
        private double ns;
        /** Negative sum round-off compensation. */
        private double nsc;

        /**
         * @param x Value
         */
        void add(double x) {
            // Overflow is not supported.
            // Assume the expected and actual are quite close when measuring the RMS.
            ss += x * x;
            n++;
            // Summing terms of the same sign avoids cancellation in the working sums.
            // There may be cancellation in the final sum so the sums are totalled
            // to 106-bit precision. This is done by adding the term through the lower
            // then higher bits of the split quad length number.
            if (x < 0) {
                final double s = nsc + x;
                nsc = twoSumLow(nsc, x, s);
                final double t = ns + s;
                nsc += twoSumLow(ns, s, t);
                ns = t;

                maxAbs = maxAbs < -x ? -x : maxAbs;
            } else {
                final double s = psc + x;
                psc = twoSumLow(psc, x, s);
                final double t = ps + s;
                psc += twoSumLow(ps, s, t);
                ps = t;

                maxAbs = maxAbs < x ? x : maxAbs;
            }
        }

        /**
         * Gets the count of recorded values.
         *
         * @return the size
         */
        int size() {
            return n;
        }

        /**
         * Gets the maximum absolute error.
         *
         * <p>This can be used to set maximum ULP thresholds for test data if the
         * TestUtils.assertEquals method is used with a large maxUlps to measure the ulp
         * (and effectively ignore failures) and the maximum reported as the end of
         * testing.
         *
         * @return maximum absolute error
         */
        double getMaxAbs() {
            return maxAbs;
        }

        /**
         * Gets the root mean squared error (RMS).
         *
         * <p> Note: If no data has been added this will return 0/0 = nan.
         * This prevents using in assertions without adding data.
         *
         * @return root mean squared error (RMS)
         */
        double getRMS() {
            return Math.sqrt(ss / n);
        }

        /**
         * Gets the mean error.
         *
         * <p>The mean can be used to determine if the error is consistently above or
         * below zero.
         *
         * @return mean error
         */
        double getMean() {
            // Sum the negative parts into the positive.
            // (see Shewchuk (1997) Grow-Expansion and Expansion-Sum [1]).
            // [1] Shewchuk (1997): Arbitrary Precision Floating-Point Arithmetic
            //  http://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps

            // This creates a 3 part number (c,b,a) in descending order of magnitude.
            double s;
            double t;
            s = nsc + psc;
            double a = twoSumLow(nsc, psc, s);
            t = s + ps;
            double b = twoSumLow(s, ps, t);
            double c = t;

            // Sum the remaining part to create a 4 part number (d,c,b,a).
            // Note: If ns+nsc is a non-overlapping 2 part number we can skip
            // adding the round-off from nsc and psc (a) as it would be smaller
            // in magnitude than nsc and hence ns. But nsc is a compensation
            // term that may overlap ns.
            s = ns + a;
            a = twoSumLow(ns, a, s);
            t = s + b;
            b = twoSumLow(s, b, t);
            s = t + c;
            c = twoSumLow(t, c, s);
            final double d = s;

            // Sum the parts in order of magnitude for 1 ULP error.
            // Reducing the error requires a two-sum rebalancing of the terms
            // iterated through the parts.
            return (a + b + c + d) / n;
        }

        /**
         * Compute the round-off from the sum of two numbers {@code a} and {@code b} using
         * Knuth's two-sum algorithm. The values are not required to be ordered by magnitude.
         * The standard precision sum must be provided.
         *
         * @param a First part of sum.
         * @param b Second part of sum.
         * @param sum Sum of the parts (a + b).
         * @return <code>(b - (sum - (sum - b))) + (a - (sum - b))</code>
         * @see <a href="http://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps">
         * Shewchuk (1997) Theorum 7</a>
         */
        static double twoSumLow(double a, double b, double sum) {
            final double bVirtual = sum - a;
            // sum - bVirtual == aVirtual.
            // a - aVirtual == a round-off
            // b - bVirtual == b round-off
            return (a - (sum - bVirtual)) + (b - bVirtual);
        }
    }

    /** Private constructor. */
    private TestUtils() {
        // intentionally empty.
    }

    /**
     * Assert the two numbers are equal within the provided units of least precision.
     * The maximum count of numbers allowed between the two values is {@code maxUlps - 1}.
     *
     * <p>The values -0.0 and 0.0 are considered equal.
     *
     * <p>Set {@code maxUlps} to negative to report the ulps to the stdout and ignore
     * failures.
     *
     * <p>The ulp difference is signed. It may be truncated to +/-Long.MAX_VALUE. Use of
     * {@link Math#abs(long)} on the value will always be positive. The sign of the error
     * is the same as that returned from Double.compare(actual, expected).
     *
     * @param expected expected value
     * @param actual actual value
     * @param maxUlps maximum units of least precision between the two values
     * @return ulp difference between the values (signed; may be truncated to +/-Long.MAX_VALUE)
     */
    static long assertEquals(double expected, double actual, long maxUlps) {
        return assertEquals(expected, actual, maxUlps, null, (Supplier<String>) null);
    }

    /**
     * Assert the two numbers are equal within the provided units of least precision.
     * The maximum count of numbers allowed between the two values is {@code maxUlps - 1}.
     *
     * <p>The values -0.0 and 0.0 are considered equal.
     *
     * <p>Set {@code maxUlps} to negative to report the ulps to the stdout and ignore
     * failures.
     *
     * <p>The ulp difference is signed. It may be truncated to +/-Long.MAX_VALUE. Use of
     * {@link Math#abs(long)} on the value will always be positive. The sign of the error
     * is the same as that returned from Double.compare(actual, expected).
     *
     * @param expected expected value
     * @param actual actual value
     * @param maxUlps maximum units of least precision between the two values
     * @param msg failure message
     * @return ulp difference between the values (signed; may be truncated to +/-Long.MAX_VALUE)
     */
    static long assertEquals(double expected, double actual, long maxUlps, String msg) {
        return assertEquals(expected, actual, maxUlps, null, () -> msg);
    }

    /**
     * Assert the two numbers are equal within the provided units of least
     * precision. The maximum count of numbers allowed between the two values is
     * {@code maxUlps - 1}.
     *
     * <p>The values -0.0 and 0.0 are considered equal.
     *
     * <p>Set {@code maxUlps} to negative to report the ulps to the stdout and
     * ignore failures.
     *
     * <p>The ulp difference is signed. It may be truncated to +/-Long.MAX_VALUE. Use of
     * {@link Math#abs(long)} on the value will always be positive. The sign of the error
     * is the same as that returned from Double.compare(actual, expected).
     *
     * @param expected expected value
     * @param actual actual value
     * @param maxUlps maximum units of least precision between the two values
     * @param error Consumer for the ulp difference between the values (signed)
     * @param msg failure message
     * @return ulp difference between the values (signed; may be truncated to +/-Long.MAX_VALUE)
     */
    static long assertEquals(double expected, double actual, long maxUlps,
            LongConsumer error, Supplier<String> msg) {
        final long e = Double.doubleToLongBits(expected);
        final long a = Double.doubleToLongBits(actual);

        // Code adapted from Precision#equals(double, double, int) so we maintain the delta
        // for the message and return it for reporting. The sign is maintained separately
        // to allow reporting errors above Long.MAX_VALUE.

        int sign;
        long delta;
        boolean equal;
        if (e == a) {
            // Binary equal
            equal = true;
            sign = 0;
            delta = 0;
        } else if ((a ^ e) < 0L) {
            // The difference is the count of numbers between each and zero.
            // This makes -0.0 and 0.0 equal.
            long d1;
            long d2;
            if (a < e) {
                sign = -1;
                d1 = e - POSITIVE_ZERO_DOUBLE_BITS;
                d2 = a - NEGATIVE_ZERO_DOUBLE_BITS;
            } else {
                sign = 1;
                d1 = a - POSITIVE_ZERO_DOUBLE_BITS;
                d2 = e - NEGATIVE_ZERO_DOUBLE_BITS;
            }
            // This may overflow but we report it using an unsigned formatter.
            delta = d1 + d2;
            if (delta < 0) {
                // Overflow
                equal = false;
            } else {
                // Allow input of a negative maximum ULPs
                equal = delta <= ((maxUlps < 0) ? (-maxUlps - 1) : maxUlps);
            }
        } else {
            if (a < e) {
                sign = -1;
                delta = e - a;
            } else {
                sign = 1;
                delta = a - e;
            }
            // The sign must be negated for negative doubles since the magnitude
            // comparison (a < e) included the sign bit.
            sign = a < 0 ? -sign : sign;

            // Allow input of a negative maximum ULPs
            equal = delta <= ((maxUlps < 0) ? (-maxUlps - 1) : maxUlps);
        }

        assert sign == Double.compare(actual, expected);

        // DEBUG:
        if (maxUlps < 0) {
            // CHECKSTYLE: stop Regex
            if (!equal || reportAllDeviations) {
                System.out.printf("%sexpected <%s> != actual <%s> (ulps=%c%s)%n",
                    prefix(msg), expected, actual, sign < 0 ? '-' : ' ', Long.toUnsignedString(delta));
            }
            // CHECKSTYLE: resume Regex
        } else if (!equal) {
            Assertions.fail(String.format("%sexpected <%s> != actual <%s> (ulps=%c%s)",
                prefix(msg), expected, actual, sign < 0 ? '-' : ' ', Long.toUnsignedString(delta)));
        }

        // This may have overflowed.
        delta = delta < 0 ? Long.MAX_VALUE : delta;
        delta *= sign;
        if (error != null) {
            error.accept(delta);
        }
        return delta;
    }

    /**
     * Assert the two numbers are equal within the provided units of least precision.
     *
     * <p>This method is for values that can be computed to arbitrary precision.
     * It raises an exception when the actual value is not finite and the expected value
     * has a non-infinite representation; or the actual value is finite and the expected
     * value has a infinite representation. In this case the computed ulp difference
     * is infinite.
     *
     * <p>This method expresses the error relative the units in the last place of the
     * expected value when converted to a {@code double} type
     * (see {@link #assertEquals(BigDecimal, double, double, DoubleConsumer, Supplier)} for details).
     *
     * <p>Set {@code maxUlps} to negative to report the ulps to the stdout and ignore
     * failures.
     *
     * <p>The ulp difference is signed. The sign of the error
     * is the same as that returned from Double.compare(actual, expected); it is
     * computed using {@code actual - expected}.
     *
     * @param expected expected value
     * @param actual actual value
     * @param maxUlps maximum units of least precision between the two values
     * @return ulp difference between the values (signed)
     */
    static double assertEquals(BigDecimal expected, double actual, double maxUlps) {
        return assertEquals(expected, actual, maxUlps, null, (Supplier<String>) null);
    }

    /**
     * Assert the two numbers are equal within the provided units of least precision.
     *
     * <p>This method is for values that can be computed to arbitrary precision.
     * It raises an exception when the actual value is not finite and the expected value
     * has a non-infinite representation; or the actual value is finite and the expected
     * value has a infinite representation. In this case the computed ulp difference
     * is infinite.
     *
     * <p>This method expresses the error relative the units in the last place of the
     * expected value when converted to a {@code double} type
     * (see {@link #assertEquals(BigDecimal, double, double, DoubleConsumer, Supplier)} for details).
     *
     * <p>Set {@code maxUlps} to negative to report the ulps to the stdout and ignore
     * failures.
     *
     * <p>The ulp difference is signed. The sign of the error
     * is the same as that returned from Double.compare(actual, expected); it is
     * computed using {@code actual - expected}.
     *
     * @param expected expected value
     * @param actual actual value
     * @param maxUlps maximum units of least precision between the two values
     * @param msg failure message
     * @return ulp difference between the values (signed)
     */
    static double assertEquals(BigDecimal expected, double actual, double maxUlps, String msg) {
        return assertEquals(expected, actual, maxUlps, null, () -> msg);
    }

    /**
     * Assert the two numbers are equal within the provided units of least
     * precision.
     *
     * <p>This method is for values that can be computed to arbitrary precision. It
     * raises an exception when the actual value is not finite and the expected
     * value has a non-infinite representation; or the actual value is finite and
     * the expected value has a infinite representation. In this case the computed
     * ulp difference is infinite.
     *
     * <p>This method expresses the error relative the units in the last place (ulp)
     * of the expected value when converted to a {@code double} type. If the actual
     * value equals the expected value the error is 0. Otherwise the error is
     * computed relative to the ulp of the expected value. The minimum non-zero
     * value for the error is 0.5. A |ulp| of < 1.0 indicates the value is the closest
     * value to the result that is not exact.
     *
     * <pre>
     * ulp          -1               0               1               2
     * --------------|---------------|---------------|---------------|--------
     *                           ^
     *                           |
     *                        expected
     *
     *               a               b               c
     *
     * a = -0.75 ulp
     * b =  0    ulp
     * c =  1.25 ulp
     * </pre>
     *
     * <p>Set {@code maxUlps} to negative to report the ulps to the stdout and
     * ignore failures.
     *
     * <p>The ulp difference is signed. The sign of the error
     * is the same as that returned from Double.compare(actual, expected); it is
     * computed using {@code actual - expected}.
     *
     * @param expected expected value
     * @param actual actual value
     * @param maxUlps maximum units of least precision between the two values
     * @param error Consumer for the ulp difference between the values (always positive)
     * @param msg failure message
     * @return ulp difference between the values (signed)
     */
    static double assertEquals(BigDecimal expected, double actual, double maxUlps,
            DoubleConsumer error, Supplier<String> msg) {
        final double e = expected.doubleValue();

        double delta;
        boolean equal;
        if (e == actual) {
            // Binary equal. This will match infinity if expected is very large.
            equal = true;
            delta = 0;
        } else if (!Double.isFinite(e) || !Double.isFinite(actual)) {
            // No representable delta between infinite and non-infinite values
            equal = false;
            delta = Double.compare(actual, e) * Double.POSITIVE_INFINITY;
        } else {
            // Two finite numbers
            delta = new BigDecimal(actual).subtract(expected)
                        .divide(new BigDecimal(Math.ulp(e)), MathContext.DECIMAL64).doubleValue();
            // Allow input of a negative maximum ULPs
            equal = Math.abs(delta) <= Math.abs(maxUlps);
        }

        if (error != null) {
            error.accept(delta);
        }

        // DEBUG:
        if (maxUlps < 0) {
            // CHECKSTYLE: stop Regex
            if (!equal || reportAllDeviations) {
                System.out.printf("%sexpected <%s> != actual <%s> (ulps=%s)%n",
                    prefix(msg), expected, actual, delta);
            }
            // CHECKSTYLE: resume Regex
        } else if (!equal) {
            Assertions.fail(String.format("%sexpected <%s> != actual <%s> (ulps=%s)",
                prefix(msg), expected, actual, delta));
        }

        return delta;
    }

    /**
     * Get the prefix for the message.
     *
     * @param msg Message supplier
     * @return the prefix
     */
    private static String prefix(Supplier<String> msg) {
        return msg == null ? "" : msg.get() + ": ";
    }
}
