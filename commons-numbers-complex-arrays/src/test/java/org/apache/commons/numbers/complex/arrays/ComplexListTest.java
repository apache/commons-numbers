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
import org.apache.commons.numbers.complex.ComplexConsumer;
import org.apache.commons.numbers.complex.ComplexFunctions;
import org.apache.commons.numbers.complex.ComplexUnaryOperator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ComplexListTest {

    private static final int MAX_CAPACITY = (Integer.MAX_VALUE - 9) / 2;

    @Test
    void testGetAndSetMethod() {
        assertListOperation(list -> {
            list.add(Complex.ofCartesian(42, 13));
            list.addAll(1, list);
            list.addAll(list);
            list.set(2, Complex.ofCartesian(200, 1));
            return list.get(2);
        });
    }

    @Test
    void testAddAndAddAll() {
        List<Complex> l1 = new ArrayList<>();
        List<Complex> l2 = new ComplexList();
        assertListOperation(list -> list.add(Complex.ofCartesian(1, 2)), l1, l2);
        assertListOperation(list -> {
            list.add(1, Complex.ofCartesian(10, 20));
            return Boolean.TRUE;
        }, l1, l2);
        assertListOperation(list -> list.add(Complex.ofCartesian(13, 14)), l1, l2);
        assertListOperation(list -> list.add(Complex.ofCartesian(15, 16)), l1, l2);
        assertListOperation(list -> list.add(Complex.ofCartesian(17, 18)), l1, l2);
        assertListOperation(list -> {
            list.addAll(1, list);
            return Boolean.TRUE;
        }, l1, l2);
        assertListOperation(list -> list.add(Complex.ofCartesian(19, 20)), l1, l2);
        assertListOperation(list -> list.add(Complex.ofCartesian(21, 22)), l1, l2);
        assertListOperation(list -> list.add(Complex.ofCartesian(23, 24)), l1, l2);

        //Testing add at an index for branch condition (size == realAndImagParts.length >>> 1)
        List<Complex> l3 = new ArrayList<>();
        List<Complex> l4 = new ComplexList();
        assertListOperation(list -> list.add(Complex.ofCartesian(1, 2)), l3, l4);
        assertListOperation(list -> {
            list.add(1, Complex.ofCartesian(10, 20));
            return Boolean.TRUE;
        }, l3, l4);
        assertListOperation(list -> list.add(Complex.ofCartesian(13, 14)), l3, l4);
        assertListOperation(list -> list.add(Complex.ofCartesian(15, 16)), l3, l4);
        assertListOperation(list -> list.add(Complex.ofCartesian(17, 18)), l3, l4);
        assertListOperation(list -> {
            list.addAll(1, list);
            return Boolean.TRUE;
        }, l3, l4);
        assertListOperation(list -> list.add(Complex.ofCartesian(19, 20)), l3, l4);
        assertListOperation(list -> list.add(Complex.ofCartesian(21, 22)), l3, l4);
        assertListOperation(list -> {
            list.add(1, Complex.ofCartesian(10, 20));
            return Boolean.TRUE;
        }, l3, l4);

        //Testing branch condition (newArrayCapacity < minArrayCapacity) in ensureCapacity
        ComplexList list1 = new ComplexList();
        int size = 5;
        IntStream.range(0, size).mapToObj(i -> Complex.ofCartesian(i, -i)).forEach(list1::add);

        List<Complex> l5 = new ArrayList<>();
        List<Complex> l6 = new ComplexList();
        assertListOperation(list -> list.add(Complex.ofCartesian(1, 2)), l5, l6);
        // Expand the list by doubling in size until at the known minArrayCapacity
        while (l5.size() < 8) {
            assertListOperation(list -> list.addAll(list), l5, l6);
        }
        assertListOperation(list -> list.addAll(list1), l5, l6);

        //Test for adding an empty list to an empty list
        ComplexList list = new ComplexList();
        assertListOperation(l -> {
            l.addAll(list);
            return l.addAll(0, list);
        });
    }

    @Test
    void testRemove() {
        assertListOperation(list -> {
            list.add(Complex.ofCartesian(42, 13));
            list.addAll(list);
            list.remove(0);
            return list.remove(0);
        });
    }

    @Test
    void testGetAndSetIndexOutOfBoundExceptions() {
        ComplexList list = new ComplexList();
        // Empty list throws
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));
        int size = 5;
        IntStream.range(0, size).mapToObj(i -> Complex.ofCartesian(i, -i)).forEach(list::add);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.get(size));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.get(size + 1));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.set(-2, Complex.ofCartesian(200, 1)));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.set(size, Complex.ofCartesian(200, 1)));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.set(size + 1, Complex.ofCartesian(200, 1)));
    }

    @Test
    void testAddIndexOutOfBoundExceptions() {
        ComplexList list = new ComplexList();
        int size = 5;
        IntStream.range(0, size).mapToObj(i -> Complex.ofCartesian(i, -i)).forEach(list::add);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () ->
            list.add(-1, Complex.ofCartesian(42, 13)));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () ->
            list.add(size + 1, Complex.ofCartesian(42, 13)));
    }

    @Test
    void testRemoveIndexOutOfBoundExceptions() {
        ComplexList list = generateComplexList(2);
        list.remove(0);
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.remove(1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.remove(-1));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 10})
    void testConstructor(int size) {
        List<Complex> l1 = new ArrayList<>(size);
        List<Complex> l2 = new ComplexList(size);
        Assertions.assertEquals(l1, l2);
        assertListOperation(l -> l.add(Complex.ofCartesian(10, 20)), l1, l2);
        assertListOperation(l -> {
            l.add(1, Complex.ofCartesian(10, 20));
            return Boolean.TRUE;
        }, l1, l2);
        assertListOperation(l -> l.addAll(1, l), l1, l2);
    }

    @Test
    void testCapacityExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ComplexList(MAX_CAPACITY + 1));

        // Set-up required sizes
        ComplexList list = new ComplexList();
        List<Complex> l = new SizedList(Integer.MAX_VALUE);
        Assertions.assertThrows(OutOfMemoryError.class, () -> list.addAll(l));

        List<Complex> l2 = new SizedList(MAX_CAPACITY + 1);
        Assertions.assertThrows(OutOfMemoryError.class, () -> list.addAll(l2));
    }

    @Test
    void testReplaceAllComplexUnaryOperator() {
        List<Complex> objectList = generateList(10);
        ComplexList actualList = new ComplexList();
        actualList.addAll(objectList);
        Assertions.assertThrows(NullPointerException.class, () -> actualList.replaceAll((ComplexUnaryOperator<Void>) null));
        objectList.replaceAll(Complex::conj);
        actualList.replaceAll(ComplexFunctions::conj);
        Assertions.assertEquals(objectList, actualList);
    }

    @Test
    void testReplaceAllComplexBinaryOperator() {
        List<Complex> objectList = generateList(10);
        double r = 2;
        double i = 3;
        Complex multiplier = Complex.ofCartesian(r, i);
        ComplexList actualList = new ComplexList();
        actualList.addAll(objectList);
        objectList.replaceAll(c -> c.multiply(multiplier));
        actualList.replaceAll((x, y, action) -> ComplexFunctions.multiply(x, y, r, i, action));
        Assertions.assertEquals(objectList, actualList);
    }

    @Test
    void testReplaceAllComplexScalarFunction() {
        List<Complex> objectList = generateList(10);
        double factor = 2;
        ComplexList actualList = new ComplexList();
        actualList.addAll(objectList);
        objectList.replaceAll(c -> c.pow(factor));
        actualList.replaceAll((x, y, action) -> ComplexFunctions.pow(x, y, factor, action));
        Assertions.assertEquals(objectList, actualList);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 10})
    void testForEachComplexConsumer(int size) {
        ComplexList expected = generateComplexList(size);
        ArrayList<Complex> actual = new ArrayList<>();
        Assertions.assertThrows(NullPointerException.class, () -> expected.forEach((ComplexConsumer) null));
        expected.forEach((real, imaginary) -> actual.add(Complex.ofCartesian(real, imaginary)));
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testGetRealAndImaginary() {
        ComplexList list = generateComplexList(10);
        for (int i = 0; i < list.size(); i++) {
            Assertions.assertEquals(list.get(i).getReal(), list.getReal(i), "real");
            Assertions.assertEquals(list.get(i).getImaginary(), list.getImaginary(i), "imaginary");
        }
    }

    @Test
    void testSetRealAndImaginary() {
        ComplexList list = generateComplexList(10);
        for (int i = 0; i < list.size(); i++) {
            final double value = Math.PI * i;
            list.setReal(i, value);
            list.setImaginary(i, value);
            Assertions.assertEquals(value, list.get(i).getReal());
            Assertions.assertEquals(value, list.get(i).getImaginary());
        }
    }

    @Test
    void testGetAndSetRealAndImaginaryIndexOutOfBoundsException() {
        ComplexList list = new ComplexList();
        // Empty list throws
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.getReal(0));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.getImaginary(0));

        int size = 5;
        IntStream.range(0, size).mapToObj(i -> Complex.ofCartesian(i, -i)).forEach(list::add);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.getReal(-1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.getReal(size));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.getReal(size + 1));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.getImaginary(-1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.getImaginary(size));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.getImaginary(size + 1));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.setReal(-2, 200));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.setReal(size, 200));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.setReal(size + 1, 200));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.setImaginary(-2, 200));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.setImaginary(size, 200));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.setImaginary(size + 1, 200));
    }

    @Test
    void testToArrayRealAndImaginary() {
        ComplexList list = generateComplexList(10);
        double[] expectedReal = list.stream().mapToDouble(Complex::getReal).toArray();
        double[] actualReal = list.toArrayReal();
        Assertions.assertArrayEquals(expectedReal, actualReal);
        double[] expectedImaginary = list.stream().mapToDouble(Complex::getImaginary).toArray();
        double[] actualImaginary = list.toArrayImaginary();
        Assertions.assertArrayEquals(expectedImaginary, actualImaginary);
    }

    /**
     * Generates a ComplexList of random complex numbers of the given size.
     * @param size number of complex numbers in the list.
     * @return the ComplexList of random complex numbers.
     */
    private static ComplexList generateComplexList(int size) {
        List<Complex> objectList = generateList(size);
        ComplexList list = new ComplexList();
        list.addAll(objectList);
        Assertions.assertEquals(objectList, list);
        return list;
    }

    /**
     * Generates a list of random complex numbers of the given size.
     * @param size number of complex numbers in the list.
     * @return the list of random complex numbers.
     */
    private static List<Complex> generateList(int size) {
        return ThreadLocalRandom.current().doubles(size, -Math.PI, Math.PI)
            .mapToObj(Complex::ofCis).collect(Collectors.toList());
    }

    private static <T> void assertListOperation(Function<List<Complex>, T> operation,
                                                List<Complex> l1, List<Complex> l2) {
        T t1 = operation.apply(l1);
        T t2 = operation.apply(l2);
        Assertions.assertEquals(t1, t2);
        Assertions.assertEquals(l1, l2);
    }

    private static <T> void assertListOperation(Function<List<Complex>, T> operation) {
        assertListOperation(operation, new ArrayList<>(), new ComplexList());
    }

    /**
     * This class purposely gives a fixed size and so is a non-functional list.
     * It is used to trigger capacity exceptions when adding a collection to ComplexList.
     */
    private static class SizedList extends ArrayList<Complex> {
        private final int fixedSize;

        SizedList(int fixedSize) {
            super();
            this.fixedSize = fixedSize;
        }

        @Override
        public int size() {
            return fixedSize;
        }
    }
}
