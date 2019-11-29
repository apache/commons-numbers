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
import java.util.function.ToDoubleFunction;
import java.util.function.BiPredicate;
import java.io.Serializable;
import org.apache.commons.numbers.core.Precision;

/**
 * This class implements <a href="http://mathworld.wolfram.com/Quaternion.html">
 * quaternions</a> (Hamilton's hypercomplex numbers).
 *
 * <p>Wherever quaternion components are listed in sequence, this class follows the
 * convention of placing the scalar ({@code w}) component first, e.g. [{@code w, x, y, z}].
 * Other libraries and textbooks may place the {@code w} component last.</p>
 *
 * <p>Instances of this class are guaranteed to be immutable.</p>
 */
public final class Quaternion implements Serializable {
    /** Zero quaternion. */
    public static final Quaternion ZERO = of(0, 0, 0, 0);
    /** Identity quaternion. */
    public static final Quaternion ONE = new Quaternion(Type.POSITIVE_POLAR_FORM, 1, 0, 0, 0);
    /** i. */
    public static final Quaternion I = new Quaternion(Type.POSITIVE_POLAR_FORM, 0, 1, 0, 0);
    /** j. */
    public static final Quaternion J = new Quaternion(Type.POSITIVE_POLAR_FORM, 0, 0, 1, 0);
    /** k. */
    public static final Quaternion K = new Quaternion(Type.POSITIVE_POLAR_FORM, 0, 0, 0, 1);

    /** Serializable version identifier. */
    private static final long serialVersionUID = 20170118L;
    /** Error message. */
    private static final String ILLEGAL_NORM_MSG = "Illegal norm: ";

    /** {@link #toString() String representation}. */
    private static final String FORMAT_START = "[";
    /** {@link #toString() String representation}. */
    private static final String FORMAT_END = "]";
    /** {@link #toString() String representation}. */
    private static final String FORMAT_SEP = " ";

    /** The number of dimensions for the vector part of the quaternion. */
    private static final int VECTOR_DIMENSIONS = 3;
    /** The number of parts when parsing a text representation of the quaternion. */
    private static final int NUMBER_OF_PARTS = 4;

    /** For enabling specialized method implementations. */
    private final Type type;
    /** First component (scalar part). */
    private final double w;
    /** Second component (first vector part). */
    private final double x;
    /** Third component (second vector part). */
    private final double y;
    /** Fourth component (third vector part). */
    private final double z;

    /**
     * For enabling optimized implementations.
     */
    private enum Type {
        /** Default implementation. */
        DEFAULT(Default.NORMSQ,
                Default.NORM,
                Default.IS_UNIT),
        /** Quaternion has unit norm. */
        NORMALIZED(Normalized.NORM,
                   Normalized.NORM,
                   Normalized.IS_UNIT),
        /** Quaternion has positive scalar part. */
        POSITIVE_POLAR_FORM(Normalized.NORM,
                            Normalized.NORM,
                            Normalized.IS_UNIT);

        /** {@link Quaternion#normSq()}. */
        private final ToDoubleFunction<Quaternion> normSq;
        /** {@link Quaternion#norm()}. */
        private final ToDoubleFunction<Quaternion> norm;
        /** {@link Quaternion#isUnit()}. */
        private final BiPredicate<Quaternion, Double> testIsUnit;

        /** Default implementations. */
        private static final class Default {
            /** {@link Quaternion#normSq()}. */
            static final ToDoubleFunction<Quaternion> NORMSQ = q ->
                q.w * q.w + q.x * q.x + q.y * q.y + q.z * q.z;

            /** {@link Quaternion#norm()}. */
            private static final ToDoubleFunction<Quaternion> NORM = q ->
                Math.sqrt(NORMSQ.applyAsDouble(q));

            /** {@link Quaternion#isUnit()}. */
            private static final BiPredicate<Quaternion, Double> IS_UNIT = (q, eps) ->
                Precision.equals(NORM.applyAsDouble(q), 1d, eps);
        }

        /** Implementations for normalized quaternions. */
        private static final class Normalized {
            /** {@link Quaternion#norm()} returns 1. */
            static final ToDoubleFunction<Quaternion> NORM = q -> 1;
            /** {@link Quaternion#isUnit(double)} returns 1. */
            static final BiPredicate<Quaternion, Double> IS_UNIT = (q, eps) -> true;
        }

        /**
         * @param normSq {@code normSq} method.
         * @param norm {@code norm} method.
         * @param isUnit {@code isUnit} method.
         */
        Type(ToDoubleFunction<Quaternion> normSq,
             ToDoubleFunction<Quaternion> norm,
             BiPredicate<Quaternion, Double> isUnit)  {
            this.normSq = normSq;
            this.norm = norm;
            this.testIsUnit = isUnit;
        }

        /**
         * @param q Quaternion.
         * @return the norm squared.
         */
        double normSq(Quaternion q) {
            return normSq.applyAsDouble(q);
        }
        /**
         * @param q Quaternion.
         * @return the norm.
         */
        double norm(Quaternion q) {
            return norm.applyAsDouble(q);
        }
        /**
         * @param q Quaternion.
         * @param eps Tolerance.
         * @return whether {@code q} has unit norm within the allowed tolerance.
         */
        boolean isUnit(Quaternion q,
                       double eps) {
            return testIsUnit.test(q, eps);
        }
    }

    /**
     * Builds a quaternion from its components.
     *
     * @param type Quaternion type.
     * @param w Scalar component.
     * @param x First vector component.
     * @param y Second vector component.
     * @param z Third vector component.
     */
    private Quaternion(Type type,
                       final double w,
                       final double x,
                       final double y,
                       final double z) {
        this.type = type;
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Copies the given quaternion, but change its {@link Type}.
     *
     * @param type Quaternion type.
     * @param q Quaternion whose components will be copied.
     */
    private Quaternion(Type type,
                       Quaternion q) {
        this.type = type;
        w = q.w;
        x = q.x;
        y = q.y;
        z = q.z;
    }

    /**
     * Builds a quaternion from its components.
     *
     * @param w Scalar component.
     * @param x First vector component.
     * @param y Second vector component.
     * @param z Third vector component.
     * @return a quaternion instance.
     */
    public static Quaternion of(final double w,
                                final double x,
                                final double y,
                                final double z) {
        return new Quaternion(Type.DEFAULT,
                              w, x, y, z);
    }

    /**
     * Builds a quaternion from scalar and vector parts.
     *
     * @param scalar Scalar part of the quaternion.
     * @param v Components of the vector part of the quaternion.
     * @return a quaternion instance.
     *
     * @throws IllegalArgumentException if the array length is not 3.
     */
    public static Quaternion of(final double scalar,
                                final double[] v) {
        if (v.length != VECTOR_DIMENSIONS) {
            throw new IllegalArgumentException("Size of array must be 3");
        }

        return of(scalar, v[0], v[1], v[2]);
    }

    /**
     * Builds a pure quaternion from a vector (assuming that the scalar
     * part is zero).
     *
     * @param v Components of the vector part of the pure quaternion.
     * @return a quaternion instance.
     */
    public static Quaternion of(final double[] v) {
        return of(0, v);
    }

    /**
     * Returns the conjugate of this quaternion number.
     * The conjugate of {@code a + bi + cj + dk} is {@code a - bi -cj -dk}.
     *
     * @return the conjugate of this quaternion object.
     */
    public Quaternion conjugate() {
        return of(w, -x, -y, -z);
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

        return of(w, x, y, z);
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
        return of(q1.w + q2.w,
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
        return of(q1.w - q2.w,
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
    public static double dot(final Quaternion q1,
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
    public double dot(final Quaternion q) {
        return dot(this, q);
    }

    /**
     * Computes the norm of the quaternion.
     *
     * @return the norm.
     */
    public double norm() {
        return type.norm(this);
    }

    /**
     * Computes the square of the norm of the quaternion.
     *
     * @return the square of the norm.
     */
    public double normSq() {
        return type.normSq(this);
    }

    /**
     * Computes the normalized quaternion (the versor of the instance).
     * The norm of the quaternion must not be near zero.
     *
     * @return a normalized quaternion.
     * @throws IllegalStateException if the norm of the quaternion is NaN, infinite,
     *      or near zero.
     */
    public Quaternion normalize() {
        switch (type) {
        case NORMALIZED:
        case POSITIVE_POLAR_FORM:
            return this;
        case DEFAULT:
            final double norm = norm();

            if (norm < Precision.SAFE_MIN ||
                !Double.isFinite(norm)) {
                throw new IllegalStateException(ILLEGAL_NORM_MSG + norm);
            }

            final Quaternion unit = divide(norm);

            return w >= 0 ?
                new Quaternion(Type.POSITIVE_POLAR_FORM, unit) :
                new Quaternion(Type.NORMALIZED, unit);
        default:
            throw new IllegalStateException(); // Should never happen.
        }
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
        return Arrays.hashCode(new double[] {w, x, y, z});
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
    public boolean isUnit(double eps) {
        return type.isUnit(this, eps);
    }

    /**
     * Checks whether the instance is a pure quaternion within a given
     * tolerance.
     *
     * @param eps Tolerance (absolute error).
     * @return {@code true} if the scalar part of the quaternion is zero.
     */
    public boolean isPure(double eps) {
        return Math.abs(w) <= eps;
    }

    /**
     * Returns the polar form of the quaternion.
     *
     * @return the unit quaternion with positive scalar part.
     */
    public Quaternion positivePolarForm() {
        switch (type) {
        case POSITIVE_POLAR_FORM:
            return this;
        case NORMALIZED:
            return w >= 0 ?
                new Quaternion(Type.POSITIVE_POLAR_FORM, this) :
                new Quaternion(Type.POSITIVE_POLAR_FORM, negate());
        case DEFAULT:
            return w >= 0 ?
                normalize() :
                // The quaternion of rotation (normalized quaternion) q and -q
                // are equivalent (i.e. represent the same rotation).
                negate().normalize();
        default:
            throw new IllegalStateException(); // Should never happen.
        }
    }

    /**
     * Returns the opposite of this instance.
     *
     * @return the quaternion for which all components have an opposite
     * sign to this one.
     */
    public Quaternion negate() {
        switch (type) {
        case POSITIVE_POLAR_FORM:
        case NORMALIZED:
            return new Quaternion(Type.NORMALIZED, -w, -x, -y, -z);
        case DEFAULT:
            return new Quaternion(Type.DEFAULT, -w, -x, -y, -z);
        default:
            throw new IllegalStateException(); // Should never happen.
        }
    }

    /**
     * Returns the inverse of this instance.
     * The norm of the quaternion must not be zero.
     *
     * @return the inverse.
     * @throws IllegalStateException if the norm (squared) of the quaternion is NaN,
     *      infinite, or near zero.
     */
    public Quaternion inverse() {
        switch (type) {
        case POSITIVE_POLAR_FORM:
        case NORMALIZED:
            return new Quaternion(type, w, -x, -y, -z);
        case DEFAULT:
            final double squareNorm = normSq();
            if (squareNorm < Precision.SAFE_MIN ||
                !Double.isFinite(squareNorm)) {
                throw new IllegalStateException(ILLEGAL_NORM_MSG + Math.sqrt(squareNorm));
            }

            return of(w / squareNorm,
                      -x / squareNorm,
                      -y / squareNorm,
                      -z / squareNorm);
        default:
            throw new IllegalStateException(); // Should never happen.
        }
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
        return getW();
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
        return new double[] {x, y, z};
    }

    /**
     * Multiplies the instance by a scalar.
     *
     * @param alpha Scalar factor.
     * @return a scaled quaternion.
     */
    public Quaternion multiply(final double alpha) {
        return of(alpha * w,
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
        return of(w / alpha,
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
     * @throws NumberFormatException if the string does not conform
     * to the specification.
     */
    public static Quaternion parse(String s) {
        final int startBracket = s.indexOf(FORMAT_START);
        if (startBracket != 0) {
            throw new QuaternionParsingException("Expected start string: " + FORMAT_START);
        }
        final int len = s.length();
        final int endBracket = s.indexOf(FORMAT_END);
        if (endBracket != len - 1) {
            throw new QuaternionParsingException("Expected end string: " + FORMAT_END);
        }
        final String[] elements = s.substring(1, s.length() - 1).split(FORMAT_SEP);
        if (elements.length != NUMBER_OF_PARTS) {
            throw new QuaternionParsingException("Incorrect number of parts: Expected 4 but was " +
                                                 elements.length +
                                                 " (separator is '" + FORMAT_SEP + "')");
        }

        final double a;
        try {
            a = Double.parseDouble(elements[0]);
        } catch (NumberFormatException ex) {
            throw new QuaternionParsingException("Could not parse scalar part" + elements[0], ex);
        }
        final double b;
        try {
            b = Double.parseDouble(elements[1]);
        } catch (NumberFormatException ex) {
            throw new QuaternionParsingException("Could not parse i part" + elements[1], ex);
        }
        final double c;
        try {
            c = Double.parseDouble(elements[2]);
        } catch (NumberFormatException ex) {
            throw new QuaternionParsingException("Could not parse j part" + elements[2], ex);
        }
        final double d;
        try {
            d = Double.parseDouble(elements[3]);
        } catch (NumberFormatException ex) {
            throw new QuaternionParsingException("Could not parse k part" + elements[3], ex);
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
    private static class QuaternionParsingException extends NumberFormatException {
        /** Serializable version identifier. */
        private static final long serialVersionUID = 20181128L;

        /**
         * @param msg Error message.
         */
        QuaternionParsingException(String msg) {
            super(msg);
        }

        /**
         * @param msg Error message.
         * @param cause Cause of the exception.
         */
        QuaternionParsingException(String msg, Throwable cause) {
            super(msg);
            initCause(cause);
        }
    }
}
