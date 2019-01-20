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
package org.apache.commons.numbers.core.precision;

import java.util.Comparator;

/** Class encapsulating the concept of comparison operations for doubles.
 */
public abstract class DoublePrecisionContext implements Comparator<Double> {

    /** Return true if the given values are considered equal to each other.
     * @param a first value
     * @param b second value
     * @return true if the given values are considered equal
     */
    public boolean areEqual(final double a, final double b) {
        return compare(a, b) == 0;
    }

    /** Return true if the given value is considered equal to zero. This is
     * equivalent {@code context.areEqual(n, 0.0)} but with a more explicit
     * method name.
     * @param n the number to compare to zero
     * @return true if the argument is considered equal to zero.
     */
    public boolean isZero(final double n) {
        return areEqual(n, 0.0);
    }

    /**
     * Return true if the first argument is strictly less than the second.
     * @param a first value
     * @param b second value
     * @return true if {@code a < b}
     */
    public boolean isLessThan(final double a, final double b) {
        return compare(a, b) < 0;
    }

    /**
     * Return true if the first argument is less than or equal to the second.
     * @param a first value
     * @param b second value
     * @return true if {@code a <= b}
     */
    public boolean isLessThanOrEqual(final double a, final double b) {
        return compare(a, b) <= 0;
    }

    /**
     * Return true if the first argument is strictly greater than the second.
     * @param a first value
     * @param b second value
     * @return true if {@code a > b}
     */
    public boolean isGreaterThan(final double a, final double b) {
        return compare(a, b) > 0;
    }

    /**
     * Return true if the first argument is greater than or equal to the second.
     * @param a first value
     * @param b second value
     * @return true if {@code a >= b}
     */
    public boolean isGreaterThanOrEqual(final double a, final double b) {
        return compare(a, b) >= 0;
    }

    /** {@inheritDoc} */
    @Override
    public int compare(final Double a, final Double b) {
        return compare(a.doubleValue(), b.doubleValue());
    }

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
    public abstract int compare(final double a, final double b);

    /** Get the largest positive double value that is still considered equal
     * to zero by this instance.
     * @return the largest positive double value still considered equal to zero
     */
    public abstract double getMaxZero();
}
