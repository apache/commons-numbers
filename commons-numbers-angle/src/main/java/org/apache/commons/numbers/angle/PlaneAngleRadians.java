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

import java.util.function.DoubleUnaryOperator;

/**
 * Utility class where all {@code double} values are assumed to be in
 * radians.
 *
 * @see PlaneAngle
 */
public final class PlaneAngleRadians {
    /** Value of \( \pi \): {@value}. */
    public static final double PI = Math.PI;
    /** Value of \( 2\pi \): {@value}. */
    public static final double TWO_PI = 2 * PI;
    /** Value of \( \pi/2 \): {@value}. */
    public static final double PI_OVER_TWO = 0.5 * PI;
    /** Value of \( 3\pi/2 \): {@value}. */
    public static final double THREE_PI_OVER_TWO = 3 * PI_OVER_TWO;
    /** Normalizes an angle to be in the range [-&pi;, &pi;). */
    public static final Normalizer WITHIN_MINUS_PI_AND_PI = new Normalizer(PlaneAngle.ZERO);
    /** Normalize an angle to be in the range [0, 2&pi;). */
    public static final Normalizer WITHIN_0_AND_2PI = new Normalizer(PlaneAngle.PI);

    /** Utility class. */
    private PlaneAngleRadians() {}

    /**
     * Normalizes an angle in an interval of size 2&pi; around a center value.
     */
    public static final class Normalizer implements DoubleUnaryOperator {
        /** Underlying normalizer. */
        private final PlaneAngle.Normalizer normalizer;

        /**
         * @param center Center (in radians) of the desired interval.
         */
        private Normalizer(PlaneAngle center) {
            normalizer = PlaneAngle.normalizer(center);
        }

        /**
         * @param a Angle (in radians).
         * @return {@code a - 2 * k} with integer {@code k} such that
         * {@code center - pi <= a - 2 * k * pi < center + pi} (in radians).
         */
        @Override
        public double applyAsDouble(double a) {
            return normalizer.apply(PlaneAngle.ofRadians(a)).toRadians();
        }
    }

    /**
     * Factory method.
     *
     * @param center Center (in radians) of the desired interval.
     * @return a {@link Normalizer} instance.
     */
    public static Normalizer normalizer(double center) {
        return new Normalizer(PlaneAngle.ofRadians(center));
    }
}
