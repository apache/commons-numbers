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
package org.apache.commons.numbers.field;

import org.apache.commons.numbers.core.NativeOperators;
import org.apache.commons.numbers.core.Precision;

/**
 * Wraps a {@code double} value in order to be used as a field
 * element.
 */
public final class FP64 extends Number
    implements NativeOperators<FP64>,
               Comparable<FP64> {
    private static final long serialVersionUID = 1L;

    /** Additive neutral. */
    private static final FP64 ZERO = new FP64(0);
    /** Multiplicative neutral. */
    private static final FP64 ONE = new FP64(1);
    /** Value. */
    private final double value;

    /**
     * @param value Value.
     */
    private FP64(double value) {
        this.value = value;
    }

    /**
     * Factory.
     *
     * @param value Value.
     * @return a new instance.
     */
    public static FP64 of(double value) {
        return new FP64(value);
    }

    /** {@inheritDoc} */
    @Override
    public FP64 add(FP64 a) {
        return new FP64(value + a.value);
    }

    /** {@inheritDoc} */
    @Override
    public FP64 negate() {
        return new FP64(-value);
    }

    /** {@inheritDoc} */
    @Override
    public FP64 multiply(FP64 a) {
        return new FP64(value * a.value);
    }

    /** {@inheritDoc} */
    @Override
    public FP64 reciprocal() {
        return new FP64(1 / value);
    }

    /** {@inheritDoc} */
    @Override
    public FP64 subtract(FP64 a) {
        return new FP64(value - a.value);
    }

    /** {@inheritDoc} */
    @Override
    public FP64 divide(FP64 a) {
        return new FP64(value / a.value);
    }

    /** {@inheritDoc} */
    @Override
    public FP64 multiply(int n) {
        return new FP64(value * n);
    }

    /** {@inheritDoc} */
    @Override
    public FP64 pow(int n) {
        if (n == 0) {
            return ONE;
        }

        return new FP64(Math.pow(value, n));
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (other instanceof FP64) {
            final FP64 o = (FP64) other;
            return Precision.equals(value, o.value, 1);
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Double.hashCode(value);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Double.toString(value);
    }

    /** {@inheritDoc} */
    @Override
    public double doubleValue() {
        return value;
    }
    /** {@inheritDoc} */
    @Override
    public float floatValue() {
        return (float) value;
    }
    /** {@inheritDoc} */
    @Override
    public int intValue() {
        return (int) value;
    }
    /** {@inheritDoc} */
    @Override
    public long longValue() {
        return (long) value;
    }
    /** {@inheritDoc} */
    @Override
    public byte byteValue() {
        return (byte) value;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(FP64 other) {
        return Double.compare(value, other.value);
    }

    /** {@inheritDoc} */
    @Override
    public FP64 zero() {
        return ZERO;
    }

    /** {@inheritDoc} */
    @Override
    public FP64 one() {
        return ONE;
    }
}
