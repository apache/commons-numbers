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

import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import java.util.ArrayList;

import org.apache.commons.numbers.fraction.Fraction;
import org.apache.commons.numbers.core.Precision;
import org.apache.commons.numbers.fraction.BigFraction;

/**
 * List of fields.
 */
final class FieldsList {
    /** List of all fields implemented in the library. */
    private static final List<FieldTestData<?>> LIST =
            new ArrayList<>();

    static {
        try {
            // List of fields to test.
            add(FractionField.get(),
                Fraction.of(13, 4),
                Fraction.of(5, 29),
                Fraction.of(-279, 11));
            add(BigFractionField.get(),
                BigFraction.of(13256093L, 43951044L),
                BigFraction.of(543016315L, 29L),
                BigFraction.of(-27930919051L, 11L));
            add(FP64Field.get(),
                FP64.of(23.45678901),
                FP64.of(-543.2109876),
                FP64.of(-234.5678901),
                // double operations are subject to rounding so allow a tolerance
                (x, y) -> Precision.equals(x.doubleValue(), y.doubleValue(), 1));

        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new RuntimeException(e);
        }
    }

    private FieldsList() {}

    /**
     * @param field Field.
     * @param a Field element.
     * @param b Field element.
     * @param c Field element.
     */
    private static <T> void add(Field<T> field,
                                T a,
                                T b,
                                T c) {
        LIST.add(new FieldTestData<>(field, a, b, c));
    }

    /**
     * @param field Field.
     * @param a Field element.
     * @param b Field element.
     * @param c Field element.
     * @param equals Field equality predicate.
     */
    private static <T> void add(Field<T> field,
                                T a,
                                T b,
                                T c,
                                BiPredicate<T, T> equals) {
        LIST.add(new FieldTestData<>(field, a, b, c, equals));
    }

    /**
     * Subclasses that are "parametric" tests can forward the call to
     * the "@Parameters"-annotated method to this method.
     *
     * @return the stream of all fields.
     */
    static Stream<FieldTestData<?>> stream() {
        return LIST.stream();
    }
}
