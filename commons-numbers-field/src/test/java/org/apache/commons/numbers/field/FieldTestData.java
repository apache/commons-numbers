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

import java.util.function.BiPredicate;

/**
 * Data store for {@link FieldParametricTest}.
 */
class FieldTestData<T> {
    private final Field<T> field;
    private final T a;
    private final T b;
    private final T c;
    private final BiPredicate<T, T> equal;

    FieldTestData(Field<T> field,
        T a,
        T b,
        T c) {
        this(field, a, b, c, Object::equals);
    }

    FieldTestData(Field<T> field,
                  T a,
                  T b,
                  T c,
                  BiPredicate<T, T> equal) {
        this.field = field;
        this.a = a;
        this.b = b;
        this.c = c;
        this.equal = equal;
    }

    public Field<T> getField() {
        return field;
    }

    public T getA() {
        return a;
    }

    public T getB() {
        return b;
    }

    public T getC() {
        return c;
    }

    @Override
    public String toString() {
        return field.toString() + " [a=" + a + ", b=" + b + ", c=" + c + "]";
    }

    public boolean equals(T x, T y) {
        return equal.test(x, y);
    }
}
