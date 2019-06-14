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

import java.util.stream.Stream;

/**
 * Tests for fields.
 */
public class FieldParametricTest {

    private static Stream<FieldTestData> getList() {
        return FieldsList.list().stream();
    }

    @ParameterizedTest
    @MethodSource("getList")
    public void testAdditionAssociativity(FieldTestData data) {
        Field field = data.getField();
        Object a = data.getA();
        Object b = data.getB();
        Object c = data.getC();
        final Object r1 = field.add(field.add(a, b), c);
        final Object r2 = field.add(a, field.add(b, c));
        assertEquals(r1, r2);
    }

    @ParameterizedTest
    @MethodSource("getList")
    public void testAdditionCommutativity(FieldTestData data) {
        Field field = data.getField();
        Object a = data.getA();
        Object b = data.getB();
        Object c = data.getC();
        final Object r1 = field.add(a, b);
        final Object r2 = field.add(b, a);
        assertEquals(r1, r2);
    }

    @ParameterizedTest
    @MethodSource("getList")
    public void testAdditiveIdentity(FieldTestData data) {
        Field field = data.getField();
        Object a = data.getA();
        Object b = data.getB();
        Object c = data.getC();
        final Object r1 = field.add(a, field.zero());
        final Object r2 = a;
        assertEquals(r1, r2);
    }

    @ParameterizedTest
    @MethodSource("getList")
    public void testAdditiveInverse(FieldTestData data) {
        Field field = data.getField();
        Object a = data.getA();
        Object b = data.getB();
        Object c = data.getC();
        final Object r1 = field.add(a, field.negate(a));
        final Object r2 = field.zero();
        assertEquals(r1, r2);
    }

    @ParameterizedTest
    @MethodSource("getList")
    public void testMultiplicationAssociativity(FieldTestData data) {
        Field field = data.getField();
        Object a = data.getA();
        Object b = data.getB();
        Object c = data.getC();
        final Object r1 = field.multiply(field.multiply(a, b), c);
        final Object r2 = field.multiply(a, field.multiply(b, c));
        assertEquals(r1, r2);
    }

    @ParameterizedTest
    @MethodSource("getList")
    public void testMultiplicationCommutativity(FieldTestData data) {
        Field field = data.getField();
        Object a = data.getA();
        Object b = data.getB();
        Object c = data.getC();
        final Object r1 = field.multiply(a, b);
        final Object r2 = field.multiply(b, a);
        assertEquals(r1, r2);
    }

    @ParameterizedTest
    @MethodSource("getList")
    public void testMultiplicativeIdentity(FieldTestData data) {
        Field field = data.getField();
        Object a = data.getA();
        Object b = data.getB();
        Object c = data.getC();
        final Object r1 = field.multiply(a, field.one());
        final Object r2 = a;
        assertEquals(r1, r2);
    }

    @ParameterizedTest
    @MethodSource("getList")
    public void testMultiplicativeInverse(FieldTestData data) {
        Field field = data.getField();
        Object a = data.getA();
        Object b = data.getB();
        Object c = data.getC();
        final Object r1 = field.multiply(a, field.reciprocal(a));
        final Object r2 = field.one();
        assertEquals(r1, r2);
    }

    @ParameterizedTest
    @MethodSource("getList")
    public void testDistributivity(FieldTestData data) {
        Field field = data.getField();
        Object a = data.getA();
        Object b = data.getB();
        Object c = data.getC();
        final Object r1 = field.multiply(a, field.add(b, c));
        final Object r2 = field.add(field.multiply(a, b), field.multiply(a, c));
        assertEquals(r1, r2);
    }

    /**
     * @param a Instance.
     * @param b Instance.
     */
    private static void assertEquals(Object a,
                                     Object b) {
        Assertions.assertEquals(a, b, a + " != " + b);
    }
}
