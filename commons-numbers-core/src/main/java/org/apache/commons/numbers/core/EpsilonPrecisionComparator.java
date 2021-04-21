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

import java.io.Serializable;

/** Simple {@link PrecisionComparator} subclass that uses an absolute epsilon value to
 * determine equality between doubles.
 *
 * <p>This class uses the {@link Precision#compareTo(double, double, double)} method to compare
 * numbers. Two values are considered equal if there is no floating point
 * value strictly between them or if their numerical difference is less than or equal
 * to the configured epsilon value.</p>
 *
 * @see Precision#compareTo(double, double, double)
 */
public class EpsilonPrecisionComparator extends AbstractPrecisionComparator implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20210421L;

    /** Epsilon value. */
    private final double epsilon;

    /** Simple constructor.
     * @param eps Epsilon value. Numbers are considered equal if there is no
     *      floating point value strictly between them or if their difference is less
     *      than or equal to this value.
     * @throws IllegalArgumentException if the given epsilon value is infinite, NaN, or negative
     */
    public EpsilonPrecisionComparator(final double eps) {
        if (!Double.isFinite(eps) || eps < 0.0) {
            throw new IllegalArgumentException("Invalid epsilon value: " + eps);
        }
        this.epsilon = eps;
    }

    /** Get the epsilon value for the instance. Numbers are considered equal if there
     * is no floating point value strictly between them or if their difference is less
     * than or equal to this value.
     * @return the epsilon value for the instance
     */
    public double getEpsilon() {
        return epsilon;
    }

    /** {@inheritDoc} **/
    @Override
    public int compare(final double a, final double b) {
        return Precision.compareTo(a, b, epsilon);
    }

    /** {@inheritDoc} **/
    @Override
    public int hashCode() {
        int result = 31;
        result += 17 * Double.hashCode(epsilon);

        return result;
    }

    /** {@inheritDoc} **/
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EpsilonPrecisionComparator)) {
            return false;
        }

        final EpsilonPrecisionComparator other = (EpsilonPrecisionComparator) obj;

        return Double.compare(this.epsilon, other.epsilon) == 0;
    }

    /** {@inheritDoc} **/
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName())
            .append("[")
            .append("epsilon= ")
            .append(epsilon)
            .append("]");

        return sb.toString();
    }
}
