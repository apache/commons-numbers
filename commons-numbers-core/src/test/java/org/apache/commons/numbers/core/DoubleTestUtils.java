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
package org.apache.commons.numbers.core;

import org.apache.commons.rng.UniformRandomProvider;

/** Class providing test utilities related to doubles.
 */
final class DoubleTestUtils {

    /** Utility class; no instantiation. */
    private DoubleTestUtils() {}

    /** Compute the difference in ULP between two arguments of the same sign.
     * @param a first argument
     * @param b second argument
     * @return ULP difference between the arguments
     */
    public static int computeUlpDifference(final double a, final double b) {
        return (int) (Double.doubleToLongBits(a) - Double.doubleToLongBits(b));
    }

    /** Construct an array of length {@code len} containing random double values with exponents between
     * {@code minExp} and {@code maxExp}.
     * @param len vector length
     * @param minExp minimum element exponent value
     * @param maxExp maximum element exponent value
     * @param rng random number generator
     * @return random vector array
     */
    public static double[] randomArray(final int len, final int minExp, final int maxExp,
            final UniformRandomProvider rng) {
        final double[] v = new double[len];
        for (int i = 0; i < v.length; ++i) {
            v[i] = randomDouble(minExp, maxExp, rng);
        }
        return v;
    }

    /** Construct a random double with an exponent in the range {@code [minExp, maxExp]}.
     * @param minExp minimum exponent; must be less than {@code maxExp}
     * @param maxExp maximum exponent; must be greater than {@code minExp}
     * @param rng random number generator
     * @return random double value with an exponent in the specified range
     */
    public static double randomDouble(final int minExp, final int maxExp, final UniformRandomProvider rng) {
        // Create random doubles using random bits in the sign bit and the mantissa.
        final long mask = ((1L << 52) - 1) | 1L << 63;
        final long bits = rng.nextLong() & mask;
        // The exponent must be unsigned so + 1023 to the signed exponent
        final int expRange = maxExp - minExp + 1;
        final long exp = rng.nextInt(expRange) + minExp + 1023;
        return Double.longBitsToDouble(bits | (exp << 52));
    }
}
