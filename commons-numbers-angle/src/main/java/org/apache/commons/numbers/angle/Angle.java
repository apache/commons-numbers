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
import java.util.function.DoubleUnaryOperator;
import java.util.function.DoubleSupplier;

/**
 * Represents the <a href="https://en.wikipedia.org/wiki/Angle">angle</a> concept.
 */
public abstract class Angle implements DoubleSupplier {
    /** Conversion factor. */
    private static final double TURN_TO_RAD = 2 * Math.PI;
    /** Conversion factor. */
    private static final double TURN_TO_DEG = 360d;

    /** Value (unit depends on concrete instance). */
    protected final double value;

    /**
     * @param value Value in turns.
     */
    private Angle(double value) {
        this.value = value;
    }

    /** @return the value. */
    @Override
    public double getAsDouble() {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Double.hashCode(value);
    }

    /**
     * @return the angle in <a href="https://en.wikipedia.org/wiki/Turn_%28geometry%29">turns</a>.
     */
    public abstract Turn toTurn();

    /**
     * @return the angle in <a href="https://en.wikipedia.org/wiki/Radian">radians</a>.
     */
    public abstract Rad toRad();

    /**
     * @return the angle in <a href="https://en.wikipedia.org/wiki/Degree_%28angle%29">degrees</a>.
     */
    public abstract Deg toDeg();

    /**
     * Objects are considered to be equal if their values are exactly
     * the same, or both are {@code Double.NaN}.
     * Caveat: Method should be called only on instances of the same
     * concrete type in order to avoid that angles with the same value
     * but different units are be considered equal.
     *
     * @param other Angle.
     * @return {@code true} if the two instances have the same {@link #value}.
     */
    protected boolean isSame(Angle other) {
        return this == other ||
            Double.doubleToLongBits(value) == Double.doubleToLongBits(other.value);
    }

    /**
     * Unit: <a href="https://en.wikipedia.org/wiki/Turn_%28geometry%29">turns</a>.
     */
    public static final class Turn extends Angle {
        /** Zero. */
        public static final Turn ZERO = Turn.of(0d);

        /**
         * @param angle (in turns).
         */
        private Turn(double angle) {
            super(angle);
        }

        /**
         * @param angle (in turns).
         * @return a new intance.
         */
        public static Turn of(double angle) {
            return new Turn(angle);
        }

        /**
         * Test for equality with another object.
         * Objects are considered to be equal if their values are exactly
         * the same, or both are {@code Double.NaN}.
         *
         * @param other Object to test for equality with this instance.
         * @return {@code true} if the objects are equal, {@code false} if
         * {@code other} is {@code null}, not an instance of {@code Turn},
         * or not equal to this instance.
         */
        @Override
        public boolean equals(Object other) {
            return other instanceof Turn ?
                isSame((Turn) other) :
                false;
        }

        /** {@inheritDoc} */
        @Override
        public Turn toTurn() {
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public Rad toRad() {
            return Rad.of(value * TURN_TO_RAD);
        }

        /** {@inheritDoc} */
        @Override
        public Deg toDeg() {
            return Deg.of(value * TURN_TO_DEG);
        }

        /**
         * Creates an operator for normalizing/reducing an angle.
         * The output will be within the {@code [c - 0.5, c + 0.5[} interval.
         *
         * @param c Center.
         * @return the normalization operator.
         */
        public static UnaryOperator<Turn> normalizer(Turn c) {
            final Normalizer n = new Normalizer(c.value, 1d);
            return (Turn a) -> Turn.of(n.applyAsDouble(a.value));
        }
    }

    /**
     * Unit: <a href="https://en.wikipedia.org/wiki/Radian">radians</a>.
     */
    public static final class Rad extends Angle {
        /** Zero. */
        public static final Rad ZERO = Rad.of(0d);
        /** &pi;. */
        public static final Rad PI = Rad.of(Math.PI);
        /** 2&pi;. */
        public static final Rad TWO_PI = Rad.of(2 * Math.PI);

        /**
         * @param angle (in radians).
         */
        private Rad(double angle) {
            super(angle);
        }

        /**
         * @param angle (in radians).
         * @return a new intance.
         */
        public static Rad of(double angle) {
            return new Rad(angle);
        }

        /**
         * Test for equality with another object.
         * Objects are considered to be equal if their values are exactly
         * the same, or both are {@code Double.NaN}.
         *
         * @param other Object to test for equality with this instance.
         * @return {@code true} if the objects are equal, {@code false} if
         * {@code other} is {@code null}, not an instance of {@code Rad},
         * or not equal to this instance.
         */
        @Override
        public boolean equals(Object other) {
            return other instanceof Rad ?
                isSame((Rad) other) :
                false;
        }

        /** {@inheritDoc} */
        @Override
        public Turn toTurn() {
            return Turn.of(value / TURN_TO_RAD);
        }

        /** {@inheritDoc} */
        @Override
        public Rad toRad() {
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public Deg toDeg() {
            return Deg.of(toTurn().getAsDouble() * TURN_TO_DEG);
        }

        /**
         * Creates an operator for normalizing/reducing an angle.
         * The output will be within the <code> [c - &pi;, c + &pi;[</code> interval.
         *
         * @param c Center.
         * @return the normalization operator.
         */
        public static UnaryOperator<Rad> normalizer(Rad c) {
            final Normalizer n = new Normalizer(c.value, TURN_TO_RAD);
            return (Rad a) -> Rad.of(n.applyAsDouble(a.value));
        }
    }

    /**
     * Unit: <a href="https://en.wikipedia.org/wiki/Degree_%28angle%29">degrees</a>.
     */
    public static final class Deg extends Angle {
        /** Zero. */
        public static final Deg ZERO = Deg.of(0d);

        /**
         * @param angle (in degrees).
         */
        private Deg(double angle) {
            super(angle);
        }

        /**
         * @param angle (in degrees).
         * @return a new intance.
         */
        public static Deg of(double angle) {
            return new Deg(angle);
        }

        /**
         * Test for equality with another object.
         * Objects are considered to be equal if their values are exactly
         * the same, or both are {@code Double.NaN}.
         *
         * @param other Object to test for equality with this instance.
         * @return {@code true} if the objects are equal, {@code false} if
         * {@code other} is {@code null}, not an instance of {@code Deg},
         * or not equal to this instance.
         */
        @Override
        public boolean equals(Object other) {
            return other instanceof Deg ?
                isSame((Deg) other) :
                false;
        }

        /** {@inheritDoc} */
        @Override
        public Turn toTurn() {
            return Turn.of(value / TURN_TO_DEG);
        }

        /** {@inheritDoc} */
        @Override
        public Rad toRad() {
            return Rad.of(toTurn().getAsDouble() * TURN_TO_RAD);
        }

        /** {@inheritDoc} */
        @Override
        public Deg toDeg() {
            return this;
        }

        /**
         * Creates an operator for normalizing/reducing an angle.
         * The output will be within the {@code [c - 180, c + 180[} interval.
         *
         * @param c Center.
         * @return the normalization operator.
         */
        public static UnaryOperator<Deg> normalizer(Deg c) {
            final Normalizer n = new Normalizer(c.value, TURN_TO_DEG);
            return (Deg a) -> Deg.of(n.applyAsDouble(a.value));
        }
    }

    /**
     * Normalizes an angle around a center value.
     */
    private static final class Normalizer implements DoubleUnaryOperator {
        /** Lower bound. */
        private final double lowerBound;
        /** Upper bound. */
        private final double upperBound;
        /** Period. */
        private final double period;
        /** Normalizer. */
        private final Reduce reduce;

        /**
         * Note: It is assumed that both arguments have the same unit.
         *
         * @param center Center of the desired interval.
         * @param period Circonference of the circle.
         */
        Normalizer(double center,
                   double period) {
            final double halfPeriod = 0.5 * period;
            this.period = period;
            lowerBound = center - halfPeriod;
            upperBound = center + halfPeriod;
            reduce = new Reduce(lowerBound, period);
        }

        /**
         * @param a Angle.
         * @return {@code = a - k} where {@code k} is an integer that satisfies
         * {@code center - 0.5 <= a - k < center + 0.5} (in turns).
         */
        @Override
        public double applyAsDouble(double a) {
            final double normalized = reduce.applyAsDouble(a) + lowerBound;
            return normalized < upperBound ?
                normalized :
                // If value is too small to be representable compared to the
                // floor expression above (ie, if value + x = x), then we may
                // end up with a number exactly equal to the upper bound here.
                // In that case, subtract one from the normalized value so that
                // we can fulfill the contract of only returning results strictly
                // less than the upper bound.
                normalized - period;
        }
    }
}
