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
package org.apache.commons.numbers.examples.jmh.core;

import org.apache.commons.rng.UniformRandomProvider;

/** Class containing utility methods for working with doubles.
 */
final class DoubleUtils {

    /** No instantiation. */
    private DoubleUtils() {}

    /** Create a random double value with exponent in the range {@code [minExp, maxExp]}.
     * @param minExp minimum exponent; must be less than {@code maxExp}
     * @param maxExp maximum exponent; must be greater than {@code minExp}
     * @param rng random number generator
     * @return random double
     */
    static double random(final int minExp, final int maxExp, final UniformRandomProvider rng) {
        // Create random doubles using random bits in the sign bit and the mantissa.
        final long mask = ((1L << 52) - 1) | 1L << 63;
        final long bits = rng.nextLong() & mask;
        // The exponent must be unsigned so + 1023 to the signed exponent
        final long exp = rng.nextInt(maxExp - minExp + 1) + minExp + 1023;
        return Double.longBitsToDouble(bits | (exp << 52));
    }

    /** Create an array with the given length containing random doubles with exponents in the range
     * {@code [minExp, maxExp]}.
     * @param len array length
     * @param minExp minimum exponent; must be less than {@code maxExp}
     * @param maxExp maximum exponent; must be greater than {@code minExp}
     * @param rng random number generator
     * @return array of random doubles
     */
    static double[] randomArray(final int len, final int minExp, final int maxExp,
            final UniformRandomProvider rng) {
        final double[] arr = new double[len];
        for (int i = 0; i < arr.length; ++i) {
            arr[i] = random(minExp, maxExp, rng);
        }
        return arr;
    }

    /** Return a new array containing each element of {@code v} multiplied by {@code s}.
     * @param v input array
     * @param s scale factor
     * @return new array containing each element of {@code v} multiplied by {@code s}
     */
    static double[] scalarMultiply(final double[] v, final double s) {
        final double[] sv = new double[v.length];
        for (int i = 0; i < v.length; ++i) {
            sv[i] = s * v[i];
        }
        return sv;
    }
}
