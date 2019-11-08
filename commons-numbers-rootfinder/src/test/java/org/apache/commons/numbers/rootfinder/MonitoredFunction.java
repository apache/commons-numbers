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
package org.apache.commons.numbers.rootfinder;

import java.util.function.DoubleUnaryOperator;

/**
 * Wrapper class for counting functions calls.
 */
class MonitoredFunction implements DoubleUnaryOperator {
    private final int maxCount;
    private int callsCount;
    private final DoubleUnaryOperator f;

    MonitoredFunction(DoubleUnaryOperator f) {
        this(f, Integer.MAX_VALUE);
    }

    MonitoredFunction(DoubleUnaryOperator f,
                      int maxCount) {
        callsCount = 0;
        this.f = f;
        this.maxCount = maxCount;
    }

    int getCallsCount() {
        return callsCount;
    }

    @Override
    public double applyAsDouble(double x) {
        if (++callsCount > maxCount) {
            throw new IllegalStateException(callsCount + " > " + maxCount +
                                            ": too many calls");
        }
        return f.applyAsDouble(x);
    }
}
