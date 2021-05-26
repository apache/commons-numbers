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
 * Reduces {@code |a - offset|} to the primary interval {@code [0, |period|)}.
 *
 * Specifically, the {@link #applyAsDouble(double) computed value} is:
 * {@code a - |period| * floor((a - offset) / |period|) - offset}.
 */
public class Reduce implements DoubleUnaryOperator {
    /** Offset. */
    private final double offset;
    /** Period. */
    private final double period;

    /**
     * @param offset Value that will be mapped to {@code 0}.
     * @param period Period.
     */
    public Reduce(double offset,
                  double period) {
        this.offset = offset;
        this.period = Math.abs(period);
    }

    /** {@inheritDoc} */
    @Override
    public double applyAsDouble(double x) {
        final double xMo = x - offset;
        return xMo - period * Math.floor(xMo / period);
    }
}
