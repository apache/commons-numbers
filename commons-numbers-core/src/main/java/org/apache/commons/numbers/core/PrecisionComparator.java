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

/** Interface containing comparison operations for doubles that allow values
 * to be <em>considered</em> equal even if they are not <em>exactly</em> equal.
 * This is especially useful when comparing outputs of a computation where floating
 * point errors may have occurred.
 */
public interface PrecisionComparator {

    /** Return true if the given values are considered equal to each other.
     * @param a first value
     * @param b second value
     * @return true if the given values are considered equal
     */
    boolean eq(double a, double b);

    /** Return true if the given value is considered equal to zero. This is
     * equivalent to {@code context.eq(n, 0.0)} but with a more explicit
     * method name.
     * @param n the number to compare to zero
     * @return true if the argument is considered equal to zero.
     */
    boolean eqZero(double n);

    /**
     * Return true if the first argument is strictly less than the second.
     * @param a first value
     * @param b second value
     * @return true if {@code a < b}
     */
    boolean lt(double a, double b);

    /**
     * Return true if the first argument is less than or considered equal to the second.
     * @param a first value
     * @param b second value
     * @return true if {@code a <= b}
     */
    boolean lte(double a, double b);

    /**
     * Return true if the first argument is strictly greater than the second.
     * @param a first value
     * @param b second value
     * @return true if {@code a > b}
     */
    boolean gt(double a, double b);

    /**
     * Return true if the first argument is greater than or considered equal to the second.
     * @param a first value
     * @param b second value
     * @return true if {@code a >= b}
     */
    boolean gte(double a, double b);

    /** Return the sign of the argument: 0 if the value is considered equal to
     * zero, -1 if less than 0, and +1 if greater than 0.
     * @param a number to determine the sign of
     * @return 0 if the number is considered equal to 0, -1 if less than
     *      0, and +1 if greater than 0
     */
    int sign(double a);

    /** Compare two double values. The returned value is
     * <ul>
     *  <li>
     *   {@code 0} if the arguments are considered equal,
     *  </li>
     *  <li>
     *   {@code -1} if {@code a < b},
     *  </li>
     *  <li>
     *   {@code +1} if {@code a > b} or if either value is NaN.
     *  </li>
     * </ul>
     *
     * @param a first value
     * @param b second value
     * @return {@code 0} if the values are considered equal, {@code -1} if the
     *      first is smaller than the second, {@code 1} is the first is larger
     *      than the second or either value is NaN.
     */
    int compare(double a, double b);
}
