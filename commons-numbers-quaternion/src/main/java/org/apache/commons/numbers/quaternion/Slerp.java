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

import java.util.function.DoubleFunction;

/**
 * Perform spherical linear interpolation (<a href="https://en.wikipedia.org/wiki/Slerp">Slerp</a>).
 *
 * The <em>Slerp</em> algorithm is designed to interpolate smoothly between
 * two rotations/orientations, producing a constant-speed motion along an arc.
 * The original purpose of this algorithm was to animate 3D rotations. All output
 * quaternions are in positive polar form, meaning a unit quaternion with a positive
 * scalar component.
 */
public class Slerp implements DoubleFunction<Quaternion> {
    /**
     * Threshold max value for the dot product.
     * If the quaternion dot product is greater than this value (i.e. the
     * quaternions are very close to each other), then the quaternions are
     * linearly interpolated instead of spherically interpolated.
     */
    private static final double MAX_DOT_THRESHOLD = 0.9995;
    /** Start of the interpolation. */
    private final Quaternion start;
    /** End of the interpolation. */
    private final Quaternion end;
    /** Linear or spherical interpolation algorithm. */
    private final DoubleFunction<Quaternion> algo;

    /**
     * @param start Start of the interpolation.
     * @param end End of the interpolation.
     */
    public Slerp(Quaternion start,
                 Quaternion end) {
        this.start = start.positivePolarForm();

        final Quaternion e = end.positivePolarForm();
        double dot = this.start.dot(e);

        // If the dot product is negative, then the interpolation won't follow the shortest
        // angular path between the two quaterions. In this case, invert the end quaternion
        // to produce an equivalent rotation that will give us the path we want.
        if (dot < 0) {
            dot = -dot;
            this.end = e.negate();
        } else {
            this.end = e;
        }

        algo = dot > MAX_DOT_THRESHOLD ?
            new Linear() :
            new Spherical(dot);
    }

    /**
     * Performs the interpolation.
     * The rotation returned by this method is controlled by the interpolation parameter, {@code t}.
     * All other values are interpolated (or extrapolated if {@code t} is outside of the
     * {@code [0, 1]} range). The returned quaternion is in positive polar form, meaning that it
     * is a unit quaternion with a positive scalar component.
     *
     * @param t Interpolation control parameter.
     * When {@code t = 0}, a rotation equal to the start instance is returned.
     * When {@code t = 1}, a rotation equal to the end instance is returned.
     * @return an interpolated quaternion in positive polar form.
     */
    @Override
    public Quaternion apply(double t) {
        // Handle no-op cases.
        if (t == 0) {
            return start;
        } else if (t == 1) {
            // Call to "positivePolarForm()" is required because "end" might
            // not be in positive polar form.
            return end.positivePolarForm();
        }

        return algo.apply(t);
    }

    /**
     * Linear interpolation, used when the quaternions are too closely aligned.
     */
    private class Linear implements DoubleFunction<Quaternion> {
        /** {@inheritDoc} */
        @Override
        public Quaternion apply(double t) {
            final double f = 1 - t;
            return Quaternion.of(f * start.getW() + t * end.getW(),
                                 f * start.getX() + t * end.getX(),
                                 f * start.getY() + t * end.getY(),
                                 f * start.getZ() + t * end.getZ()).positivePolarForm();
        }
    }

    /**
     * Spherical interpolation, used when the quaternions are too closely aligned.
     * When we may end up dividing by zero (cf. 1/sin(theta) term below).
     * {@link Linear} interpolation must be used.
     */
    private class Spherical implements DoubleFunction<Quaternion> {
        /** Angle of rotation. */
        private final double theta;
        /** Sine of {@link #theta}. */
        private final double sinTheta;

        /**
         * @param dot Dot product of the start and end quaternions.
         */
        Spherical(double dot) {
            theta = Math.acos(dot);
            sinTheta = Math.sin(theta);
        }

        /** {@inheritDoc} */
        @Override
        public Quaternion apply(double t) {
            final double f1 = Math.sin((1 - t) * theta) / sinTheta;
            final double f2 = Math.sin(t * theta) / sinTheta;

            return Quaternion.of(f1 * start.getW() + f2 * end.getW(),
                                 f1 * start.getX() + f2 * end.getX(),
                                 f1 * start.getY() + f2 * end.getY(),
                                 f1 * start.getZ() + f2 * end.getZ()).positivePolarForm();
        }
    }
}
