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

import org.apache.commons.numbers.core.NativeOperators;

/**
 * Boiler-plate code for concrete implementations of {@link Field}.
 *
 * @param <T> Type of the field elements.
 */
public abstract class AbstractField<T extends NativeOperators<T>>
    implements Field<T> {
    /** {@inheritDoc} */
    @Override
    public T add(T a, T b) {
        return a.add(b);
    }

    /** {@inheritDoc} */
    @Override
    public T subtract(T a, T b) {
        return a.subtract(b);
    }

    /** {@inheritDoc} */
    @Override
    public T negate(T a) {
        return a.negate();
    }

    /** {@inheritDoc} */
    @Override
    public T multiply(int n, T a) {
        return a.multiply(n);
    }

    /** {@inheritDoc} */
    @Override
    public T multiply(T a, T b) {
        return a.multiply(b);
    }

    /** {@inheritDoc} */
    @Override
    public T divide(T a, T b) {
        return a.divide(b);
    }

    /** {@inheritDoc} */
    @Override
    public T reciprocal(T a) {
        return a.reciprocal();
    }
}
