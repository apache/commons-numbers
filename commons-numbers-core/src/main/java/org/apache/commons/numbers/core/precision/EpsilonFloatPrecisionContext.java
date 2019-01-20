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

import java.io.Serializable;

/** Simple {@link FloatPrecisionContext} subclass that uses an absolute epsilon value to
 * determine equality between float.
 *
 * <p>This class uses the {@link Precision#compareTo(float, float, float)} method to compare
 * numbers. Two values are considered equal if there is no floating point
 * value strictly between them or if their numerical difference is less than or equal
 * to the configured epsilon value.</p>
 *
 * @see Precision#compareTo(float, float, float)
 */
public class EpsilonFloatPrecisionContext extends FloatPrecisionContext implements Serializable {

    /** Serializable identifer */
    private static final long serialVersionUID = 20190119L;

    /** Epsilon value. */
    private final float epsilon;

    /** Simple constructor.
     * @param eps Epsilon value. Numbers are considered equal if there is no
     *      floating point value strictly between them or if their difference is less
     *      than or equal to this value.
     */
    public EpsilonFloatPrecisionContext(final float eps) {
        this.epsilon = eps;
    }

    /** Get the epsilon value for the instance. Numbers are considered equal if there
     * is no floating point value strictly between them or if their difference is less
     * than or equal to this value.
     * @return the epsilon value for the instance
     */
    public float getEpsilon() {
        return epsilon;
    }

    /** {@inheritDoc}
     * This value is equal to the epsilon value for the instance.
     * @see #getEpsilon()
     */
    @Override
    public float getMaxZero() {
        return epsilon;
    }

    /** {@inheritDoc} **/
    @Override
    public int compare(float a, float b) {
        if (Precision.equals(a, b, epsilon)) {
            return 0;
        }
        else if (a < b) {
            return -1;
        }
        return 1;
    }

    /** {@inheritDoc} **/
    @Override
    public int hashCode() {
        int result = 31;
        result += 17 * Float.hashCode(epsilon);

        return result;
    }

    /** {@inheritDoc} **/
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EpsilonFloatPrecisionContext)) {
            return false;
        }

        EpsilonFloatPrecisionContext other = (EpsilonFloatPrecisionContext) obj;

        return this.epsilon == other.epsilon;
    }

    /** {@inheritDoc} **/
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName())
            .append("[")
            .append("epsilon= ")
            .append(epsilon)
            .append("]");

        return sb.toString();
    }
}
