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

package org.apache.commons.numbers.complex.arrays;

import org.apache.commons.numbers.complex.Complex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ComplexListFunctionsTest {


    @Test
    void testListConj() {
        List<Complex> l5 = new ArrayList<>();
        List<Complex> l6 = new ComplexList();
        createList(l5, l6);
        assertListOperation(list -> list.replaceAll(Complex::conj), l5, l6);

    }

    @Test
    void testListNegate() {
        List<Complex> l5 = new ArrayList<>();
        List<Complex> l6 = new ComplexList();
        createList(l5, l6);
        assertListOperation(list -> list.replaceAll(Complex::negate), l5, l6);
    }

    @Test
    void testListProj() {
        List<Complex> l5 = new ArrayList<>();
        List<Complex> l6 = new ComplexList();
        createList(l5, l6);
        assertListOperation(list -> list.replaceAll(Complex::proj), l5, l6);
    }

    @Test
    void testListExp() {
        List<Complex> l5 = new ArrayList<>();
        List<Complex> l6 = new ComplexList();
        createList(l5, l6);
        assertListOperation(list -> list.replaceAll(Complex::exp), l5, l6);
    }

    @Test
    void testListLog() {
        List<Complex> l5 = new ArrayList<>();
        List<Complex> l6 = new ComplexList();
        createList(l5, l6);
        assertListOperation(list -> list.replaceAll(Complex::log), l5, l6);
    }

    @Test
    void testListLog10() {
        List<Complex> l5 = new ArrayList<>();
        List<Complex> l6 = new ComplexList();
        createList(l5, l6);
        assertListOperation(list -> list.replaceAll(Complex::log10), l5, l6);
    }

    @Test
    void testListSqrt() {
        List<Complex> l5 = new ArrayList<>();
        List<Complex> l6 = new ComplexList();
        createList(l5, l6);
        assertListOperation(list -> list.replaceAll(Complex::sqrt), l5, l6);
    }

    @Test
    void testListSin() {
        List<Complex> l5 = new ArrayList<>();
        List<Complex> l6 = new ComplexList();
        createList(l5, l6);
        assertListOperation(list -> list.replaceAll(Complex::sin), l5, l6);
    }

    @Test
    void testListCos() {
        List<Complex> l5 = new ArrayList<>();
        List<Complex> l6 = new ComplexList();
        createList(l5, l6);
        assertListOperation(list -> list.replaceAll(Complex::cos), l5, l6);
    }

    @Test
    void testListTan() {
        List<Complex> l5 = new ArrayList<>();
        List<Complex> l6 = new ComplexList();
        createList(l5, l6);
        assertListOperation(list -> list.replaceAll(Complex::tan), l5, l6);
    }

    @Test
    void testListAsin() {
        List<Complex> l5 = new ArrayList<>();
        List<Complex> l6 = new ComplexList();
        createList(l5, l6);
        assertListOperation(list -> list.replaceAll(Complex::asin), l5, l6);
    }

    @Test
    void testListAcos() {
        List<Complex> l5 = new ArrayList<>();
        List<Complex> l6 = new ComplexList();
        createList(l5, l6);
        assertListOperation(list -> list.replaceAll(Complex::acos), l5, l6);
    }

    @Test
    void testListAtan() {
        List<Complex> l5 = new ArrayList<>();
        List<Complex> l6 = new ComplexList();
        createList(l5, l6);
        assertListOperation(list -> list.replaceAll(Complex::atan), l5, l6);
    }

    @Test
    void testListSinh() {
        List<Complex> l5 = new ArrayList<>();
        List<Complex> l6 = new ComplexList();
        createList(l5, l6);
        assertListOperation(list -> list.replaceAll(Complex::sinh), l5, l6);
    }

    @Test
    void testListCosh() {
        List<Complex> l5 = new ArrayList<>();
        List<Complex> l6 = new ComplexList();
        createList(l5, l6);
        assertListOperation(list -> list.replaceAll(Complex::cosh), l5, l6);
    }

    @Test
    void testListTanh() {
        List<Complex> l5 = new ArrayList<>();
        List<Complex> l6 = new ComplexList();
        createList(l5, l6);
        assertListOperation(list -> list.replaceAll(Complex::tanh), l5, l6);
    }

    @Test
    void testListAsinh() {
        List<Complex> l5 = new ArrayList<>();
        List<Complex> l6 = new ComplexList();
        createList(l5, l6);
        assertListOperation(list -> list.replaceAll(Complex::asinh), l5, l6);
    }

    @Test
    void testListAcosh() {
        List<Complex> l5 = new ArrayList<>();
        List<Complex> l6 = new ComplexList();
        createList(l5, l6);
        assertListOperation(list -> list.replaceAll(Complex::acosh), l5, l6);
    }

    @Test
    void testListAtanh() {
        List<Complex> l5 = new ArrayList<>();
        List<Complex> l6 = new ComplexList();
        createList(l5, l6);
        assertListOperation(list -> list.replaceAll(Complex::atanh), l5, l6);
    }

    private void createList(List<Complex> l1, List<Complex> l2) {
        assertListOperation(list -> list.add(Complex.ofCartesian(1, 2)), l1, l2);
        while (l1.size() < 8) {
            assertListOperation(list -> list.addAll(list), l1, l2);
        }
    }

    private static void assertListOperation(Consumer<List<Complex>> operation,
                                            List<Complex> l1, List<Complex> l2) {
        operation.accept(l1);
        operation.accept(l2);
        Assertions.assertEquals(l1, l2);
    }
}
