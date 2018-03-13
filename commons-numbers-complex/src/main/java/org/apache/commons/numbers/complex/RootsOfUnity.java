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
package org.apache.commons.numbers.complex;

/**
 * Computation of the {@code n}-th roots of unity.
 */
public class RootsOfUnity {
    /** 2 * &pi; */
    private static final double TWO_PI = 2 * Math.PI;
    /** Number of roots of unity. */
    private final int omegaCount;
    /** The roots. */
    private final Complex[] omega;
    /**
     * {@code true} if the constructor was called with a positive
     * value of its argument {@code n}.
     */
    private final boolean isCounterClockwise;

    /**
     * Computes the {@code n}-th roots of unity.
     *
     * The roots are stored in an array
     * {@code omega[]}, such that {@code omega[k] = w ^ k}, where
     * {@code k = 0, ..., n - 1}, {@code w = exp(2 * pi * i / n)} and
     * {@code i = sqrt(-1)}.
     *
     * <p>{@code n} can be positive of negative ({@code abs(n)} is always
     * the number of roots of unity):</p>
     * <ul>
     *  <li>If {@code n > 0}, then the roots are stored in counter-clockwise order.</li>
     *  <li>If {@code n < 0}, then the roots are stored in clockwise order.</li>
     * </ul>
     *
     * @param n The (signed) number of roots of unity to be computed.
     * @throws IllegalArgumentException if {@code n == 0}?
     */
    public RootsOfUnity(int n) {
        if (n == 0) {
            throw new IllegalArgumentException("Zero-th root");
        }

        omegaCount = Math.abs(n);
        isCounterClockwise = n > 0;

        omega = new Complex[omegaCount];
        final double t = TWO_PI / omegaCount;
        final double cosT = Math.cos(t);
        final double sinT = Math.sin(t);

        double previousReal = 1;
        double previousImag = 0;
        omega[0] = Complex.ofCartesian(previousReal, previousImag);
        for (int i = 1; i < omegaCount; i++) {
            final double real = previousReal * cosT - previousImag * sinT;
            final double imag = previousReal * sinT + previousImag * cosT;

            omega[i] = isCounterClockwise ?
                Complex.ofCartesian(real, imag) :
            Complex.ofCartesian(real, -imag);

            previousReal = real;
            previousImag = imag;
        }
    }

    /**
     * Returns {@code true} if {@link #RootsOfUnity(int)} was called with a
     * positive value of its argument {@code n}.
     * If {@code true}, then the imaginary parts of the successive roots are
     * {@link #getRoot(int) returned} in counter-clockwise order.
     *
     * @return {@code true} if the roots of unity are stored in
     * counter-clockwise order.
     */
    public boolean isCounterClockwise() {
        return isCounterClockwise;
    }

    /**
     * Gets the {@code k}-th among the computed roots of unity.
     *
     * @param k Index of the requested value.
     * @return the {@code k}-th among the {@code N}-th root of unity
     * where {@code N} is the absolute value of the argument passed
     * to the constructor.
     * @throws IndexOutOfBoundsException if {@code k} is out of range.
     */
    public Complex getRoot(int k) {
        return omega[k];
    }

    /**
     * Gets the number of roots of unity.
     *
     * @return the number of roots of unity.
     */
    public int getNumberOfRoots() {
        return omegaCount;
    }
}
