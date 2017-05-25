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

/**
 * Represents the <a href="https://en.wikipedia.org/wiki/Angle">angle</a> concept.
 */
public class PlaneAngle {
    /** Conversion factor. */
    private static final double HALF_TURN = 0.5;
    /** Conversion factor. */
    private static final double TO_RADIANS = 2 * Math.PI;
    /** Conversion factor. */
    private static final double FROM_RADIANS = 1d / TO_RADIANS;
    /** Conversion factor. */
    private static final double TO_DEGREES = 360;
    /** Conversion factor. */
    private static final double FROM_DEGREES = 1d / TO_DEGREES;
    /** Value (in turns). */
    private final double value;
    /** Zero. */
    public static final PlaneAngle ZERO = new PlaneAngle(0);
    /** &pi; radians. */
    public static final PlaneAngle PI = new PlaneAngle(HALF_TURN);

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
     * Normalize an angle in an interval of size 1 turn around a
     * center value.
     *
     * @param center Center of the desired interval for the result.
     * @return {@code a - 2 * k} with integer {@code k} such that
     * {@code center - 0.5 <= a - 2 * k <= center + 0.5} (in turns).
     */
    public PlaneAngle normalize(PlaneAngle center) {
        return new PlaneAngle(value - Math.floor(value + HALF_TURN - center.value));
    }

    /**
     * Normalize within the interval centered at 0.
     *
     * @return {@code a - 2 * k} with integer {@code k} such that
     * {@code -0.5 <= a - 2 * k <= 0.5} (in turns).
     */
    public PlaneAngle normalize() {
        return normalize(ZERO);
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
        if (other instanceof PlaneAngle){
            return new Double(value).equals(new Double(((PlaneAngle) other).value));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new Double(value).hashCode();
    }
}
