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
     * @param expected expected value
     * @param actual actual value
     * @param maxUlps maximum units of least precision between the two values
     * @return ulp difference between the values (always positive; may be truncated to Long.MAX_VALUE)
     */
    static long assertEquals(double expected, double actual, long maxUlps) {
        return assertEquals(expected, actual, maxUlps, null, (Supplier<String>) null);
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
     * @param expected expected value
     * @param actual actual value
     * @param maxUlps maximum units of least precision between the two values
     * @param error Consumer for the ulp difference between the values (always positive)
     * @param msg failure message
     * @return ulp difference between the values (always positive; may be truncated to Long.MAX_VALUE)
     */
    static long assertEquals(double expected, double actual, long maxUlps,
            LongConsumer error, Supplier<String> msg) {
        final long e = Double.doubleToLongBits(expected);
        final long a = Double.doubleToLongBits(actual);

        // Code adapted from Precision#equals(double, double, int) so we maintain the delta
        // for the message and return it for reporting.

        long delta;
        boolean equal;
        if (e == a) {
            // Binary equal
            equal = true;
            delta = 0;
        } else if ((a ^ e) < 0L) {
            // The difference is the count of numbers between each and zero.
            // This makes -0.0 and 0.0 equal.
            long d1;
            long d2;
            if (a < e) {
                d1 = e - POSITIVE_ZERO_DOUBLE_BITS;
                d2 = a - NEGATIVE_ZERO_DOUBLE_BITS;
            } else {
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
            delta = Math.abs(e - a);
            // Allow input of a negative maximum ULPs
            equal = delta <= ((maxUlps < 0) ? (-maxUlps - 1) : maxUlps);
        }

        // DEBUG:
        if (maxUlps < 0) {
            // CHECKSTYLE: stop Regex
            if (!equal || reportAllDeviations) {
                System.out.printf("%sexpected <%s> != actual <%s> (ulps=%s)%n",
                    prefix(msg), expected, actual, Long.toUnsignedString(delta));
            }
            // CHECKSTYLE: resume Regex
        } else if (!equal) {
            Assertions.fail(String.format("%sexpected <%s> != actual <%s> (ulps=%s)",
                prefix(msg), expected, actual, Long.toUnsignedString(delta)));
        }

        // This may have overflowed.
        delta = delta < 0 ? Long.MAX_VALUE : delta;
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
     * @param expected expected value
     * @param actual actual value
     * @param maxUlps maximum units of least precision between the two values
     * @return ulp difference between the values (always positive)
     */
    static double assertEquals(BigDecimal expected, double actual, double maxUlps) {
        return assertEquals(expected, actual, maxUlps, null, (Supplier<String>) null);
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
     * value for the error is 0.5. A ulp of < 1.0 indicates the value is the closest
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
     * a = 0.75 ulp
     * b = 0    ulp
     * c = 1.25 ulp
     * </pre>
     *
     * <p>Set {@code maxUlps} to negative to report the ulps to the stdout and
     * ignore failures.
     *
     * @param expected expected value
     * @param actual actual value
     * @param maxUlps maximum units of least precision between the two values
     * @param error Consumer for the ulp difference between the values (always positive)
     * @param msg failure message
     * @return ulp difference between the values (always positive)
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
            delta = Double.POSITIVE_INFINITY;
        } else {
            // Two finite numbers
            delta = expected.subtract(new BigDecimal(actual)).abs()
                        .divide(new BigDecimal(Math.ulp(e)), MathContext.DECIMAL64).doubleValue();
            // Allow input of a negative maximum ULPs
            equal = delta <= Math.abs(maxUlps);
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
