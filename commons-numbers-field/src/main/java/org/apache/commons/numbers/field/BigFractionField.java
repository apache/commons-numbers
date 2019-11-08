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

import org.apache.commons.numbers.fraction.BigFraction;

/**
 * {@link BigFraction} field.
 */
public final class BigFractionField extends AbstractField<BigFraction> {
    /** Singleton. */
    private static final BigFractionField INSTANCE = new BigFractionField();

    /** Singleton. */
    private BigFractionField() {}

    /** @return the field instance. */
    public static BigFractionField get() {
        return INSTANCE;
    }

    /** {@inheritDoc} */
    @Override
    public BigFraction one() {
        return BigFraction.ONE;
    }

    /** {@inheritDoc} */
    @Override
    public BigFraction zero() {
        return BigFraction.ZERO;
    }
}
