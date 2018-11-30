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

package org.apache.commons.numbers.quaternion;

import java.util.Arrays;
import java.io.Serializable;
import org.apache.commons.numbers.core.Precision;

/**
 * This class implements <a href="http://mathworld.wolfram.com/Quaternion.html">
 * quaternions</a> (Hamilton's hypercomplex numbers).
 *
 * <p>Instance of this class are guaranteed to be immutable.</p>
 */
public final class Quaternion implements Serializable {
    /** Identity quaternion. */
    public static final Quaternion IDENTITY = new Quaternion(1, 0, 0, 0);
    /** Zero quaternion. */
    public static final Quaternion ZERO = new Quaternion(0, 0, 0, 0);
    /** i */
    public static final Quaternion I = new Quaternion(0, 1, 0, 0);
    /** j */
    public static final Quaternion J = new Quaternion(0, 0, 1, 0);
    /** k */
    public static final Quaternion K = new Quaternion(0, 0, 0, 1);

    /** Serializable version identifier. */
    private static final long serialVersionUID = 20170118L;
    /** Error message. */
    private static final String ZERO_NORM_MSG = "Norm is zero";

    /** {@link #toString() String representation}. */
    private static final String FORMAT_START = "[";
    /** {@link #toString() String representation}. */
    private static final String FORMAT_END = "]";
    /** {@link #toString() String representation}. */
    private static final String FORMAT_SEP = " ";

    /** First component (scalar part). */
    private final double q0;
    /** Second component (first vector part). */
    private final double q1;
    /** Third component (second vector part). */
    private final double q2;
    /** Fourth component (third vector part). */
    private final double q3;

    /**
     * Builds a quaternion from its components.
     *
     * @param a Scalar component.
     * @param b First vector component.
     * @param c Second vector component.
     * @param d Third vector component.
     */
    private Quaternion(final double a,
                      final double b,
                      final double c,
                      final double d) {
        q0 = a;
        q1 = b;
        q2 = c;
        q3 = d;
    }

    /**
     * Builds a quaternion from scalar and vector parts.
     *
     * @param scalar Scalar part of the quaternion.
     * @param v Components of the vector part of the quaternion.
     *
     * @throws IllegalArgumentException if the array length is not 3.
     */
    private Quaternion(final double scalar,
                      final double[] v) {
        if (v.length != 3) {
            throw new IllegalArgumentException("Size of array must be 3");
        }

        q0 = scalar;
        q1 = v[0];
        q2 = v[1];
        q3 = v[2];
    }

    /**
     * Builds a quaternion from its components.
     *
     * @param a Scalar component.
     * @param b First vector component.
     * @param c Second vector component.
     * @param d Third vector component.
     * @return a quaternion instance
     */
    public static Quaternion of(final double a,
                                final double b,
                                final double c,
                                final double d) {
        return new Quaternion(a, b, c, d);
    }

    /**
     * Builds a quaternion from scalar and vector parts.
     *
     * @param scalar Scalar part of the quaternion.
     * @param v Components of the vector part of the quaternion.
     * @return a quaternion instance
     *
     * @throws IllegalArgumentException if the array length is not 3.
     */
    public static Quaternion of(final double scalar,
                                final double[] v) {
        return new Quaternion(scalar, v);
    }

    /**
     * Builds a pure quaternion from a vector (assuming that the scalar
     * part is zero).
     *
     * @param v Components of the vector part of the pure quaternion.
     * @return a quaternion instance
     */
    public static Quaternion of(final double[] v) {
        return new Quaternion(0, v);
    }

    /**
     * Returns the conjugate of this quaternion number.
     * The conjugate of {@code a + bi + cj + dk} is {@code a - bi -cj -dk}.
     *
     * @return the conjugate of this quaternion object.
     */
    public Quaternion conjugate() {
        return new Quaternion(q0, -q1, -q2, -q3);
    }

    /**
     * Returns the Hamilton product of two quaternions.
     *
     * @param q1 First quaternion.
     * @param q2 Second quaternion.
     * @return the product {@code q1} and {@code q2}, in that order.
     */
    public static Quaternion multiply(final Quaternion q1,
                                      final Quaternion q2) {
        // Components of the first quaternion.
        final double q1a = q1.q0;
        final double q1b = q1.q1;
        final double q1c = q1.q2;
        final double q1d = q1.q3;

        // Components of the second quaternion.
        final double q2a = q2.q0;
        final double q2b = q2.q1;
        final double q2c = q2.q2;
        final double q2d = q2.q3;

        // Components of the product.
        final double w = q1a * q2a - q1b * q2b - q1c * q2c - q1d * q2d;
        final double x = q1a * q2b + q1b * q2a + q1c * q2d - q1d * q2c;
        final double y = q1a * q2c - q1b * q2d + q1c * q2a + q1d * q2b;
        final double z = q1a * q2d + q1b * q2c - q1c * q2b + q1d * q2a;

        return new Quaternion(w, x, y, z);
    }

    /**
     * Returns the Hamilton product of the instance by a quaternion.
     *
     * @param q Quaternion.
     * @return the product of this instance with {@code q}, in that order.
     */
    public Quaternion multiply(final Quaternion q) {
        return multiply(this, q);
    }

    /**
     * Computes the sum of two quaternions.
     *
     * @param q1 Quaternion.
     * @param q2 Quaternion.
     * @return the sum of {@code q1} and {@code q2}.
     */
    public static Quaternion add(final Quaternion q1,
                                 final Quaternion q2) {
        return new Quaternion(q1.q0 + q2.q0,
                              q1.q1 + q2.q1,
                              q1.q2 + q2.q2,
                              q1.q3 + q2.q3);
    }

    /**
     * Computes the sum of the instance and another quaternion.
     *
     * @param q Quaternion.
     * @return the sum of this instance and {@code q}
     */
    public Quaternion add(final Quaternion q) {
        return add(this, q);
    }

    /**
     * Subtracts two quaternions.
     *
     * @param q1 First Quaternion.
     * @param q2 Second quaternion.
     * @return the difference between {@code q1} and {@code q2}.
     */
    public static Quaternion subtract(final Quaternion q1,
                                      final Quaternion q2) {
        return new Quaternion(q1.q0 - q2.q0,
                              q1.q1 - q2.q1,
                              q1.q2 - q2.q2,
                              q1.q3 - q2.q3);
    }

    /**
     * Subtracts a quaternion from the instance.
     *
     * @param q Quaternion.
     * @return the difference between this instance and {@code q}.
     */
    public Quaternion subtract(final Quaternion q) {
        return subtract(this, q);
    }

    /**
     * Computes the dot-product of two quaternions.
     *
     * @param q1 Quaternion.
     * @param q2 Quaternion.
     * @return the dot product of {@code q1} and {@code q2}.
     */
    public static double dotProduct(final Quaternion q1,
                                    final Quaternion q2) {
        return q1.q0 * q2.q0 +
            q1.q1 * q2.q1 +
            q1.q2 * q2.q2 +
            q1.q3 * q2.q3;
    }

    /**
     * Computes the dot-product of the instance by a quaternion.
     *
     * @param q Quaternion.
     * @return the dot product of this instance and {@code q}.
     */
    public double dotProduct(final Quaternion q) {
        return dotProduct(this, q);
    }

    /**
     * Computes the norm of the quaternion.
     *
     * @return the norm.
     */
    public double norm() {
        return Math.sqrt(norm2());
    }

    /**
     * Computes the square of the norm of the quaternion.
     *
     * @return the square of the norm.
     */
    public double norm2() {
        return q0 * q0 +
                q1 * q1 +
                q2 * q2 +
                q3 * q3;
    }

    /**
     * Computes the normalized quaternion (the versor of the instance).
     * The norm of the quaternion must not be near zero.
     *
     * @return a normalized quaternion.
     * @throws IllegalStateException if the norm of the quaternion is near zero.
     */
    public Quaternion normalize() {
        final double norm = norm();

        if (norm < Precision.SAFE_MIN) {
            throw new IllegalStateException(ZERO_NORM_MSG);
        }

        return this.divide(norm);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Quaternion) {
            final Quaternion q = (Quaternion) other;
            return ((Double)q0).equals(q.q0) &&
                    ((Double)q1).equals(q.q1) &&
                    ((Double)q2).equals(q.q2) &&
                    ((Double)q3).equals(q.q3);
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(new double[] { q0, q1, q2, q3 });
    }

    /**
     * Checks whether this instance is equal to another quaternion
     * within a given tolerance.
     *
     * @param q Quaternion with which to compare the current quaternion.
     * @param eps Tolerance.
     * @return {@code true} if the each of the components are equal
     * within the allowed absolute error.
     */
    public boolean equals(final Quaternion q,
                          final double eps) {
        return Precision.equals(q0, q.q0, eps) &&
            Precision.equals(q1, q.q1, eps) &&
            Precision.equals(q2, q.q2, eps) &&
            Precision.equals(q3, q.q3, eps);
    }

    /**
     * Checks whether the instance is a unit quaternion within a given
     * tolerance.
     *
     * @param eps Tolerance (absolute error).
     * @return {@code true} if the norm is 1 within the given tolerance,
     * {@code false} otherwise
     */
    public boolean isUnitQuaternion(double eps) {
        return Precision.equals(norm(), 1d, eps);
    }

    /**
     * Checks whether the instance is a pure quaternion within a given
     * tolerance.
     *
     * @param eps Tolerance (absolute error).
     * @return {@code true} if the scalar part of the quaternion is zero.
     */
    public boolean isPureQuaternion(double eps) {
        return Math.abs(q0) <= eps;
    }

    /**
     * Returns the polar form of the quaternion.
     *
     * @return the unit quaternion with positive scalar part.
     */
    public Quaternion positivePolarForm() {
        if (q0 < 0) {
            final Quaternion unitQ = normalize();
            // The quaternion of rotation (normalized quaternion) q and -q
            // are equivalent (i.e. represent the same rotation).
            return new Quaternion(-unitQ.q0,
                                  -unitQ.q1,
                                  -unitQ.q2,
                                  -unitQ.q3);
        } else {
            return this.normalize();
        }
    }

    /**
     * Returns the inverse of this instance.
     * The norm of the quaternion must not be zero.
     *
     * @return the inverse.
     * @throws IllegalArgumentException if the norm (squared) of the quaternion is zero.
     */
    public Quaternion inverse() {
        final double squareNorm = norm2();
        if (squareNorm < Precision.SAFE_MIN) {
            throw new IllegalStateException(ZERO_NORM_MSG);
        }

        return new Quaternion(q0 / squareNorm,
                              -q1 / squareNorm,
                              -q2 / squareNorm,
                              -q3 / squareNorm);
    }

    /**
     * Gets the first component of the quaternion (scalar part).
     *
     * @return the scalar part.
     */
    public double getQ0() {
        return q0;
    }

    /**
     * Gets the second component of the quaternion (first component
     * of the vector part).
     *
     * @return the first component of the vector part.
     */
    public double getQ1() {
        return q1;
    }

    /**
     * Gets the third component of the quaternion (second component
     * of the vector part).
     *
     * @return the second component of the vector part.
     */
    public double getQ2() {
        return q2;
    }

    /**
     * Gets the fourth component of the quaternion (third component
     * of the vector part).
     *
     * @return the third component of the vector part.
     */
    public double getQ3() {
        return q3;
    }

    /**
     * Gets the scalar part of the quaternion.
     *
     * @return the scalar part.
     * @see #getQ0()
     */
    public double getScalarPart() {
        return q0;
    }

    /**
     * Gets the three components of the vector part of the quaternion.
     *
     * @return the vector part.
     * @see #getQ1()
     * @see #getQ2()
     * @see #getQ3()
     */
    public double[] getVectorPart() {
        return new double[] { q1, q2, q3 };
    }

    /**
     * Multiplies the instance by a scalar.
     *
     * @param alpha Scalar factor.
     * @return a scaled quaternion.
     */
    public Quaternion multiply(final double alpha) {
        return new Quaternion(alpha * q0,
                              alpha * q1,
                              alpha * q2,
                              alpha * q3);
    }

    /**
     * Divides the instance by a scalar.
     *
     * @param alpha Scalar factor.
     * @return a scaled quaternion.
     */
    public Quaternion divide(final double alpha) {
        return new Quaternion(q0 / alpha,
                              q1 / alpha,
                              q2 / alpha,
                              q3 / alpha);
    }

    /**
     * Parses a string that would be produced by {@link #toString()}
     * and instantiates the corresponding object.
     *
     * @param s String representation.
     * @return an instance.
     * @throws IllegalArgumentException if the string does not
     * conform to the specification.
     */
    public static Quaternion parse(String s) {
        final int len = s.length();
        final int startBracket = s.indexOf(FORMAT_START);
        if (startBracket != 0) {
            throw new QuaternionParsingException("Expected start string: " + FORMAT_START);
        }
        final int endBracket = s.indexOf(FORMAT_END);
        if (endBracket != len - 1) {
            throw new QuaternionParsingException("Expected end string: " + FORMAT_END);
        }
        final String[] elements = s.substring(1, s.length() - 1).split(FORMAT_SEP);
        if (elements.length != 4) {
            throw new QuaternionParsingException("Incorrect number of parts: Expected 4 but was " +
                                                 elements.length +
                                                 " (separator is '" + FORMAT_SEP + "')");
        }

        final double q1;
        try {
            q1 = Double.parseDouble(elements[0]);
        } catch (NumberFormatException ex) {
            throw new QuaternionParsingException("Could not parse scalar part" + elements[0]);
        }
        final double q2;
        try {
            q2 = Double.parseDouble(elements[1]);
        } catch (NumberFormatException ex) {
            throw new QuaternionParsingException("Could not parse i part" + elements[1]);
        }
        final double q3;
        try {
            q3 = Double.parseDouble(elements[2]);
        } catch (NumberFormatException ex) {
            throw new QuaternionParsingException("Could not parse j part" + elements[2]);
        }
        final double q4;
        try {
            q4 = Double.parseDouble(elements[3]);
        } catch (NumberFormatException ex) {
            throw new QuaternionParsingException("Could not parse k part" + elements[3]);
        }

        return of(q1, q2, q3, q4);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder();
        s.append(FORMAT_START)
            .append(q0).append(FORMAT_SEP)
            .append(q1).append(FORMAT_SEP)
            .append(q2).append(FORMAT_SEP)
            .append(q3)
            .append(FORMAT_END);

        return s.toString();
    }

    /** See {@link #parse(String)}. */
    private static class QuaternionParsingException extends IllegalArgumentException {
        /** Serializable version identifier. */
        private static final long serialVersionUID = 20181128L;

        /**
         * @param msg Error message.
         */
        QuaternionParsingException(String msg) {
            super(msg);
        }
    }
}
