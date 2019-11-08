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
package org.apache.commons.numbers.field;

/**
 * {@link Double} field.
 */
public final class FP64Field extends AbstractField<FP64> {
    /** 0d. */
    private static final FP64 ZERO = FP64.of(0d);
    /** 1d. */
    private static final FP64 ONE = FP64.of(1d);
    /** Singleton. */
    private static final FP64Field INSTANCE = new FP64Field();

    /** Singleton. */
    private FP64Field() {}

    /** @return the field instance. */
    public static FP64Field get() {
        return INSTANCE;
    }

    /** {@inheritDoc} */
    @Override
    public FP64 one() {
        return ONE;
    }

    /** {@inheritDoc} */
    @Override
    public FP64 zero() {
        return ZERO;
    }
}
