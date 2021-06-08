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

/**
 * Computes linear combinations as the the sum of the products of two sequences of numbers
 * <code>a<sub>i</sub> b<sub>i</sub></code>.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Dot_product">Dot product</a>
 */
public interface LinearCombination {
    /**
     * Compute the sum of the products of two sequences of 2 factors.
     */
    @FunctionalInterface
    interface TwoD {
        /**
         * Compute the sum of the products of two sequences of factors.
         *
         * @param a1 First factor of the first term.
         * @param b1 Second factor of the first term.
         * @param a2 First factor of the second term.
         * @param b2 Second factor of the second term.
         * @return \( a_1 b_1 + a_2 b_2 \)
         */
        double value(double a1, double b1, double a2, double b2);
    }

    /**
     * Compute the sum of the products of two sequences of 3 factors.
     */
    @FunctionalInterface
    interface ThreeD {
        /**
         * Compute the sum of the products of two sequences of factors.
         *
         * @param a1 First factor of the first term.
         * @param b1 Second factor of the first term.
         * @param a2 First factor of the second term.
         * @param b2 Second factor of the second term.
         * @param a3 First factor of the third term.
         * @param b3 Second factor of the third term.
         * @return \( a_1 b_1 + a_2 b_2 + a_3 b_3 \)
         */
        double value(double a1, double b1, double a2, double b2, double a3, double b3);
    }

    /**
     * Compute the sum of the products of two sequences of 4 factors.
     */
    @FunctionalInterface
    interface FourD {
        /**
         * Compute the sum of the products of two sequences of factors.
         *
         * @param a1 First factor of the first term.
         * @param b1 Second factor of the first term.
         * @param a2 First factor of the second term.
         * @param b2 Second factor of the second term.
         * @param a3 First factor of the third term.
         * @param b3 Second factor of the third term.
         * @param a4 First factor of the fourth term.
         * @param b4 Second factor of the fourth term.
         * @return \( a_1 b_1 + a_2 b_2 + a_3 b_3 + a_4 b_4 \)
         */
        double value(double a1, double b1, double a2, double b2, double a3, double b3, double a4, double b4);
    }

    /**
     * Compute the sum of the products of two sequences of {@code n} factors.
     */
    @FunctionalInterface
    interface ND {
        /**
         * Compute the sum of the products of two sequences of factors.
         *
         * @param a Factors.
         * @param b Factors.
         * @return \( \sum_i a_i b_i \).
         * @throws IllegalArgumentException if the sizes of the arrays are different.
         */
        double value(double[] a, double[] b);
    }
}
