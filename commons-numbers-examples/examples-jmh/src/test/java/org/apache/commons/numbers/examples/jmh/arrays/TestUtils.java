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

package org.apache.commons.numbers.examples.jmh.arrays;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.function.Supplier;
import org.apache.commons.numbers.core.DD;
import org.apache.commons.rng.UniformRandomProvider;
import org.junit.jupiter.api.Assertions;

/**
 * Test utilities.
 */
final class TestUtils {
    /** No instances. */
    private TestUtils() {}

    // DD equality checks adapted from o.a.c.numbers.core.TestUtils

    /**
     * Assert the two numbers are equal within the provided relative error.
     *
     * <p>The provided error is relative to the exact result in expected: (e - a) / e.
     * If expected is zero this division is undefined. In this case the actual must be zero
     * (no absolute tolerance is supported). The reporting of the error uses the absolute
     * error and the return value of the relative error is 0. Cases of complete cancellation
     * should be avoided for benchmarking relative accuracy.
     *
     * <p>Note that the actual double-double result is not validated using the high and low
     * parts individually. These are summed and compared to the expected.
     *
     * <p>Set {@code eps} to negative to report the relative error to the stdout and
     * ignore failures.
     *
     * <p>The relative error is signed. The sign of the error
     * is the same as that returned from Double.compare(actual, expected); it is
     * computed using {@code actual - expected}.
     *
     * @param expected expected value
     * @param actual actual value
     * @param eps maximum relative error between the two values
     * @param msg failure message
     * @return relative error difference between the values (signed)
     * @throws NumberFormatException if {@code actual} contains non-finite values
     */
    static double assertEquals(BigDecimal expected, DD actual, double eps, String msg) {
        return assertEquals(expected, actual, eps, () -> msg);
    }

    /**
     * Assert the two numbers are equal within the provided relative error.
     *
     * <p>The provided error is relative to the exact result in expected: (e - a) / e.
     * If expected is zero this division is undefined. In this case the actual must be zero
     * (no absolute tolerance is supported). The reporting of the error uses the absolute
     * error and the return value of the relative error is 0. Cases of complete cancellation
     * should be avoided for benchmarking relative accuracy.
     *
     * <p>Note that the actual double-double result is not validated using the high and low
     * parts individually. These are summed and compared to the expected.
     *
     * <p>Set {@code eps} to negative to report the relative error to the stdout and
     * ignore failures.
     *
     * <p>The relative error is signed. The sign of the error
     * is the same as that returned from Double.compare(actual, expected); it is
     * computed using {@code actual - expected}.
     *
     * @param expected expected value
     * @param actual actual value
     * @param eps maximum relative error between the two values
     * @param msg failure message
     * @return relative error difference between the values (signed)
     * @throws NumberFormatException if {@code actual} contains non-finite values
     */
    static double assertEquals(BigDecimal expected, DD actual, double eps, Supplier<String> msg) {
        // actual - expected
        final BigDecimal delta = new BigDecimal(actual.hi())
            .add(new BigDecimal(actual.lo()))
            .subtract(expected);
        boolean equal;
        if (expected.compareTo(BigDecimal.ZERO) == 0) {
            // Edge case. Currently an absolute tolerance is not supported as summation
            // to zero cases generated in testing all pass.
            equal = actual.doubleValue() == 0;

            // DEBUG:
            if (eps < 0) {
                if (!equal) {
                    printf("%sexpected 0 != actual <%s + %s> (abs.error=%s)%n",
                        prefix(msg), actual.hi(), actual.lo(), delta.doubleValue());
                }
            } else if (!equal) {
                Assertions.fail(String.format("%sexpected 0 != actual <%s + %s> (abs.error=%s)",
                    prefix(msg), actual.hi(), actual.lo(), delta.doubleValue()));
            }

            return 0;
        }

        final double rel = delta.divide(expected, MathContext.DECIMAL128).doubleValue();
        // Allow input of a negative maximum ULPs
        equal = Math.abs(rel) <= Math.abs(eps);

        // DEBUG:
        if (eps < 0) {
            if (!equal) {
                printf("%sexpected <%s> != actual <%s + %s> (rel.error=%s (%.3f x tol))%n",
                    prefix(msg), expected.round(MathContext.DECIMAL128), actual.hi(), actual.lo(),
                    rel, Math.abs(rel) / eps);
            }
        } else if (!equal) {
            Assertions.fail(String.format("%sexpected <%s> != actual <%s + %s> (rel.error=%s (%.3f x tol))",
                prefix(msg), expected.round(MathContext.DECIMAL128), actual.hi(), actual.lo(),
                rel, Math.abs(rel) / eps));
        }

        return rel;
    }

    /**
     * Print a formatted message to stdout.
     * Provides a single point to disable checkstyle warnings on print statements and
     * enable/disable all print debugging.
     *
     * @param format Format string.
     * @param args Arguments.
     */
    static void printf(String format, Object... args) {
        // CHECKSTYLE: stop regex
        System.out.printf(format, args);
        // CHECKSTYLE: resume regex
    }

    /**
     * Get the prefix for the message.
     *
     * @param msg Message supplier
     * @return the prefix
     */
    static String prefix(Supplier<String> msg) {
        return msg == null ? "" : msg.get() + ": ";
    }

    // Uses Fisher-Yates shuffle copied from o.a.c.rng.sampling.ArraySampler
    // TODO: This can be removed when {@code commons-rng-sampling 1.6} is released.

    /**
     * Shuffles the entries of the given array.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @return Shuffled input array.
     */
    static double[] shuffle(UniformRandomProvider rng, double[] array) {
        for (int i = array.length; i > 1; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
        return array;
    }

    /**
     * Shuffles the entries of the given array.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @return Shuffled input array.
     */
    static int[] shuffle(UniformRandomProvider rng, int[] array) {
        for (int i = array.length; i > 1; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
        return array;
    }

    /**
     * Swaps the two specified elements in the array.
     *
     * @param array Array.
     * @param i First index.
     * @param j Second index.
     */
    private static void swap(double[] array, int i, int j) {
        final double tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    /**
     * Swaps the two specified elements in the array.
     *
     * @param array Array.
     * @param i First index.
     * @param j Second index.
     */
    private static void swap(int[] array, int i, int j) {
        final int tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }
}
