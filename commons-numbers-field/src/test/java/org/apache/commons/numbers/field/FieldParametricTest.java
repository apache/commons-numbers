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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import org.apache.commons.numbers.core.Addition;
import org.apache.commons.numbers.core.Multiplication;

/**
 * Tests for fields.
 */
class FieldParametricTest {

    private static Stream<FieldTestData<?>> getList() {
        return FieldsList.stream();
    }

    @ParameterizedTest
    @MethodSource("getList")
    <T> void testAdditionAssociativity(FieldTestData<T> data) {
        Field<T> field = data.getField();
        T a = data.getA();
        T b = data.getB();
        T c = data.getC();
        final T r1 = field.add(field.add(a, b), c);
        final T r2 = field.add(a, field.add(b, c));
        assertEquals(r1, r2, data::equals);
    }

    @ParameterizedTest
    @MethodSource("getList")
    <T> void testAdditionCommutativity(FieldTestData<T> data) {
        Field<T> field = data.getField();
        T a = data.getA();
        T b = data.getB();
        final T r1 = field.add(a, b);
        final T r2 = field.add(b, a);
        assertEquals(r1, r2, data::equals);
    }

    @ParameterizedTest
    @MethodSource("getList")
    <T> void testAdditiveIdentity(FieldTestData<T> data) {
        Field<T> field = data.getField();
        T a = data.getA();
        final T r1 = field.add(a, field.zero());
        final T r2 = a;
        assertEquals(r1, r2, data::equals);
    }

    @ParameterizedTest
    @MethodSource("getList")
    <T> void testAdditiveInverse(FieldTestData<T> data) {
        Field<T> field = data.getField();
        T a = data.getA();
        final T r1 = field.add(a, field.negate(a));
        final T r2 = field.zero();
        assertEquals(r1, r2, data::equals);
    }

    @ParameterizedTest
    @MethodSource("getList")
    <T> void testMultiplicationAssociativity(FieldTestData<T> data) {
        Field<T> field = data.getField();
        T a = data.getA();
        T b = data.getB();
        T c = data.getC();
        final T r1 = field.multiply(field.multiply(a, b), c);
        final T r2 = field.multiply(a, field.multiply(b, c));
        assertEquals(r1, r2, data::equals);
    }

    @ParameterizedTest
    @MethodSource("getList")
    <T> void testMultiplicationCommutativity(FieldTestData<T> data) {
        Field<T> field = data.getField();
        T a = data.getA();
        T b = data.getB();
        final T r1 = field.multiply(a, b);
        final T r2 = field.multiply(b, a);
        assertEquals(r1, r2, data::equals);
    }

    @ParameterizedTest
    @MethodSource("getList")
    <T> void testMultiplicativeIdentity(FieldTestData<T> data) {
        Field<T> field = data.getField();
        T a = data.getA();
        final T r1 = field.multiply(a, field.one());
        final T r2 = a;
        assertEquals(r1, r2, data::equals);
    }

    @ParameterizedTest
    @MethodSource("getList")
    <T> void testMultiplicativeInverse(FieldTestData<T> data) {
        Field<T> field = data.getField();
        T a = data.getA();
        final T r1 = field.multiply(a, field.reciprocal(a));
        final T r2 = field.one();
        assertEquals(r1, r2, data::equals);
    }

    @ParameterizedTest
    @MethodSource("getList")
    <T> void testDistributivity(FieldTestData<T> data) {
        Field<T> field = data.getField();
        T a = data.getA();
        T b = data.getB();
        T c = data.getC();
        final T r1 = field.multiply(a, field.add(b, c));
        final T r2 = field.add(field.multiply(a, b), field.multiply(a, c));
        assertEquals(r1, r2, data::equals);
    }

    @ParameterizedTest
    @MethodSource("getList")
    <T extends Addition<T>> void testAdd(FieldTestData<T> data) {
        Field<T> field = data.getField();
        T a = data.getA();
        T b = data.getB();

        final T r1 = field.add(a, b);
        final T r2 = a.add(b);
        assertEquals(r1, r2, data::equals);
    }

    @ParameterizedTest
    @MethodSource("getList")
    <T extends Addition<T>> void testSubtract(FieldTestData<T> data) {
        Field<T> field = data.getField();
        T a = data.getA();
        T b = data.getB();

        final T r1 = field.subtract(a, b);
        final T r2 = a.add(b.negate());
        assertEquals(r1, r2, data::equals);
    }

    @ParameterizedTest
    @MethodSource("getList")
    <T extends Addition<T>> void testMultiplyInt(FieldTestData<T> data) {
        Field<T> field = data.getField();
        T a = data.getA();
        final int n = 5;

        final T r1 = field.multiply(n, a);

        T r2 = field.zero();
        for (int i = 0; i < n; i++) {
            r2 = r2.add(a);
        }

        assertEquals(r1, r2, data::equals);
    }

    @ParameterizedTest
    @MethodSource("getList")
    <T extends Addition<T>> void testZero(FieldTestData<T> data) {
        Field<T> field = data.getField();
        T a = data.getA();

        final T r1 = field.zero();
        final T r2 = a.zero();
        assertEquals(r1, r2, data::equals);
    }

    @ParameterizedTest
    @MethodSource("getList")
    <T extends Multiplication<T>> void testMultiply(FieldTestData<T> data) {
        Field<T> field = data.getField();
        T a = data.getA();
        T b = data.getB();

        final T r1 = field.multiply(a, b);
        final T r2 = a.multiply(b);
        assertEquals(r1, r2, data::equals);
    }

    @ParameterizedTest
    @MethodSource("getList")
    <T extends Multiplication<T>> void testDivide(FieldTestData<T> data) {
        Field<T> field = data.getField();
        T a = data.getA();
        T b = data.getB();

        final T r1 = field.divide(a, b);
        final T r2 = a.multiply(b.reciprocal());
        assertEquals(r1, r2, data::equals);
    }

    @ParameterizedTest
    @MethodSource("getList")
    <T extends Multiplication<T>> void testOne(FieldTestData<T> data) {
        Field<T> field = data.getField();
        T a = data.getA();

        final T r1 = field.one();
        final T r2 = a.one();
        assertEquals(r1, r2, data::equals);
    }

    /**
     * @param a Instance.
     * @param b Instance.
     */
    private static <T> void assertEquals(T a,
                                         T b,
                                         BiPredicate<T, T> equals) {
        Assertions.assertTrue(equals.test(a, b), () -> a + " != " + b);
    }
}
