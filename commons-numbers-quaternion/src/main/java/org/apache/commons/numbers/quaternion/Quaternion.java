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
    private final double w;
    /** Second component (first vector part). */
    private final double x;
    /** Third component (second vector part). */
    private final double y;
    /** Fourth component (third vector part). */
    private final double z;

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
        w = a;
        x = b;
        y = c;
        z = d;
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

        w = scalar;
        x = v[0];
        y = v[1];
        z = v[2];
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
        return new Quaternion(w, -x, -y, -z);
    }

    /**
     * Returns the Hamilton product of two quaternions.
     *
     * @param x First quaternion.
     * @param q2 Second quaternion.
     * @return the product {@code x} and {@code q2}, in that order.
     */
    public static Quaternion multiply(final Quaternion q1,
                                      final Quaternion q2) {
        // Components of the first quaternion.
        final double q1a = q1.w;
        final double q1b = q1.x;
        final double q1c = q1.y;
        final double q1d = q1.z;

        // Components of the second quaternion.
        final double q2a = q2.w;
        final double q2b = q2.x;
        final double q2c = q2.y;
        final double q2d = q2.z;

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
        return new Quaternion(q1.w + q2.w,
                              q1.x + q2.x,
                              q1.y + q2.y,
                              q1.z + q2.z);
    }

    /**
     * Computes the sum of the instance and another quaternion.
     *
     * @param q Quaternion.
     * @return the sum of this instance and {@code q}.
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
        return new Quaternion(q1.w - q2.w,
                              q1.x - q2.x,
                              q1.y - q2.y,
                              q1.z - q2.z);
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
        return q1.w * q2.w +
            q1.x * q2.x +
            q1.y * q2.y +
            q1.z * q2.z;
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
        return w * w +
            x * x +
            y * y +
            z * z;
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
            return ((Double) w).equals(q.w) &&
                ((Double) x).equals(q.x) &&
                ((Double) y).equals(q.y) &&
                ((Double) z).equals(q.z);
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(new double[] { w, x, y, z });
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
        return Precision.equals(w, q.w, eps) &&
            Precision.equals(x, q.x, eps) &&
            Precision.equals(y, q.y, eps) &&
            Precision.equals(z, q.z, eps);
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
        return Math.abs(w) <= eps;
    }

    /**
     * Returns the polar form of the quaternion.
     *
     * @return the unit quaternion with positive scalar part.
     */
    public Quaternion positivePolarForm() {
        if (w < 0) {
            final Quaternion unitQ = normalize();
            // The quaternion of rotation (normalized quaternion) q and -q
            // are equivalent (i.e. represent the same rotation).
            return new Quaternion(-unitQ.w,
                                  -unitQ.x,
                                  -unitQ.y,
                                  -unitQ.z);
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

        return new Quaternion(w / squareNorm,
                              -x / squareNorm,
                              -y / squareNorm,
                              -z / squareNorm);
    }

    /**
     * Gets the first component of the quaternion (scalar part).
     *
     * @return the scalar part.
     */
    public double getW() {
        return w;
    }

    /**
     * Gets the second component of the quaternion (first component
     * of the vector part).
     *
     * @return the first component of the vector part.
     */
    public double getX() {
        return x;
    }

    /**
     * Gets the third component of the quaternion (second component
     * of the vector part).
     *
     * @return the second component of the vector part.
     */
    public double getY() {
        return y;
    }

    /**
     * Gets the fourth component of the quaternion (third component
     * of the vector part).
     *
     * @return the third component of the vector part.
     */
    public double getZ() {
        return z;
    }

    /**
     * Gets the scalar part of the quaternion.
     *
     * @return the scalar part.
     * @see #getW()
     */
    public double getScalarPart() {
        return w;
    }

    /**
     * Gets the three components of the vector part of the quaternion.
     *
     * @return the vector part.
     * @see #getX()
     * @see #getY()
     * @see #getZ()
     */
    public double[] getVectorPart() {
        return new double[] { x, y, z };
    }

    /**
     * Multiplies the instance by a scalar.
     *
     * @param alpha Scalar factor.
     * @return a scaled quaternion.
     */
    public Quaternion multiply(final double alpha) {
        return new Quaternion(alpha * w,
                              alpha * x,
                              alpha * y,
                              alpha * z);
    }

    /**
     * Divides the instance by a scalar.
     *
     * @param alpha Scalar factor.
     * @return a scaled quaternion.
     */
    public Quaternion divide(final double alpha) {
        return new Quaternion(w / alpha,
                              x / alpha,
                              y / alpha,
                              z / alpha);
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

        final double a;
        try {
            a = Double.parseDouble(elements[0]);
        } catch (NumberFormatException ex) {
            throw new QuaternionParsingException("Could not parse scalar part" + elements[0]);
        }
        final double b;
        try {
            b = Double.parseDouble(elements[1]);
        } catch (NumberFormatException ex) {
            throw new QuaternionParsingException("Could not parse i part" + elements[1]);
        }
        final double c;
        try {
            c = Double.parseDouble(elements[2]);
        } catch (NumberFormatException ex) {
            throw new QuaternionParsingException("Could not parse j part" + elements[2]);
        }
        final double d;
        try {
            d = Double.parseDouble(elements[3]);
        } catch (NumberFormatException ex) {
            throw new QuaternionParsingException("Could not parse k part" + elements[3]);
        }

        return of(a, b, c, d);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder();
        s.append(FORMAT_START)
            .append(w).append(FORMAT_SEP)
            .append(x).append(FORMAT_SEP)
            .append(y).append(FORMAT_SEP)
            .append(z)
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
