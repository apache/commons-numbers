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
package org.apache.commons.numbers.angle;

import java.util.function.UnaryOperator;

/**
 * Represents the <a href="https://en.wikipedia.org/wiki/Angle">angle</a> concept.
 */
public final class PlaneAngle {
    /** Zero. */
    public static final PlaneAngle ZERO = new PlaneAngle(0);
    /** Half-turn (aka &pi; radians). */
    public static final PlaneAngle PI = new PlaneAngle(0.5);
    /** Conversion factor. */
    private static final double HALF_TURN = 0.5;
    /** Conversion factor. */
    private static final double TO_RADIANS = PlaneAngleRadians.TWO_PI;
    /** Conversion factor. */
    private static final double FROM_RADIANS = 1d / TO_RADIANS;
    /** Conversion factor. */
    private static final double TO_DEGREES = 360;
    /** Conversion factor. */
    private static final double FROM_DEGREES = 1d / TO_DEGREES;
    /** Value (in turns). */
    private final double value;

    /**
     * @param value Value in turns.
     */
    private PlaneAngle(double value) {
        this.value = value;
    }

    /**
     * @param angle (in <a href="https://en.wikipedia.org/wiki/Turn_%28geometry%29">turns</a>).
     * @return a new intance.
     */
    public static PlaneAngle ofTurns(double angle) {
        return new PlaneAngle(angle);
    }

    /**
     * @param angle (in <a href="https://en.wikipedia.org/wiki/Radian">radians</a>).
     * @return a new intance.
     */
    public static PlaneAngle ofRadians(double angle) {
        return new PlaneAngle(angle * FROM_RADIANS);
    }

    /**
     * @param angle (in <a href="https://en.wikipedia.org/wiki/Degree_%28angle%29">degrees</a>).
     * @return a new intance.
     */
    public static PlaneAngle ofDegrees(double angle) {
        return new PlaneAngle(angle * FROM_DEGREES);
    }

    /**
     * @return the value in <a href="https://en.wikipedia.org/wiki/Turn_%28geometry%29">turns</a>.
     */
    public double toTurns() {
        return value;
    }

    /**
     * @return the value in <a href="https://en.wikipedia.org/wiki/Radian">radians</a>.
     */
    public double toRadians() {
        return value * TO_RADIANS;
    }

    /**
     * @return the value in <a href="https://en.wikipedia.org/wiki/Degree_%28angle%29">degrees</a>.
     */
    public double toDegrees() {
        return value * TO_DEGREES;
    }

    /**
     * Test for equality with another object.
     * Objects are considered to be equal if the two values are exactly the
     * same, or both are {@code Double.NaN}.
     *
     * @param other Object to test for equality with this instance.
     * @return {@code true} if the objects are equal, {@code false} if
     * {@code other} is {@code null}, not an instance of {@code PlaneAngle},
     * or not equal to this instance.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof PlaneAngle) {
            return Double.doubleToLongBits(value) ==
                    Double.doubleToLongBits(((PlaneAngle) other).value);
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Double.hashCode(value);
    }

    /**
     * Normalizes an angle in an interval of size 1 turn around a center value.
     */
    public static final class Normalizer implements UnaryOperator<PlaneAngle> {
        /** Lower bound. */
        private final double lowerBound;
        /** Upper bound. */
        private final double upperBound;
        /** Normalizer. */
        private final Reduce reduce;

        /**
         * @param center Center of the desired interval.
         */
        private Normalizer(PlaneAngle center) {
            lowerBound = center.value - HALF_TURN;
            upperBound = center.value + HALF_TURN;
            reduce = new Reduce(lowerBound, 1d);
        }

        /**
         * @param a Angle.
         * @return {@code = a - k} where {@code k} is an integer that satisfies
         * {@code center - 0.5 <= a - k < center + 0.5} (in turns).
         */
        @Override
        public PlaneAngle apply(PlaneAngle a) {
            final double normalized = reduce.applyAsDouble(a.value) + lowerBound;
            return normalized < upperBound ?
                new PlaneAngle(normalized) :
                // If value is too small to be representable compared to the
                // floor expression above (ie, if value + x = x), then we may
                // end up with a number exactly equal to the upper bound here.
                // In that case, subtract one from the normalized value so that
                // we can fulfill the contract of only returning results strictly
                // less than the upper bound.
                new PlaneAngle(normalized - 1);
        }
    }

    /**
     * Factory method.
     *
     * @param center Center of the desired interval.
     * @return a {@link Normalizer} instance.
     */
    public static Normalizer normalizer(PlaneAngle center) {
        return new Normalizer(center);
    }
}
