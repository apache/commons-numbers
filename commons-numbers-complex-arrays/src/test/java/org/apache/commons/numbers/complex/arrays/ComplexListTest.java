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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ComplexListTest {

    private static final int MAX_CAPACITY_INTERLEAVED = (Integer.MAX_VALUE - 9) / 2;
    private static final int MAX_CAPACITY_NON_INTERLEAVED = Integer.MAX_VALUE - 9;

    /**
     * Generate a stream of arguments containing empty {@code Complex<List>} implementations.
     *
     * @return the stream of arguments
     */
    static Stream<Arguments> listImplementations() {
        return Stream.of(Arguments.of(ComplexList.interleaved()),
            Arguments.of(ComplexList.nonInterleaved()));
    }

    /**
     * Generate a stream of arguments containing {@code Complex<List>} implementations with a set size.
     *
     * @return the stream of arguments
     */
    static Stream<Arguments> listImplementationsWithSize() {
        return Stream.of(Arguments.of(ComplexList.interleaved(), 2),
            Arguments.of(ComplexList.nonInterleaved(), 2));
    }

    /**
     * Generates a ComplexList in interleaved format of random complex numbers of the given size using the.
     * @param size number of complex numbers in the list.
     * @return the ComplexList of random complex numbers.
     */
    private static ComplexList generateComplexInterleavedList(int size) {
        List<Complex> objectList = generateList(size);
        ComplexList list = ComplexList.interleaved();
        list.addAll(objectList);
        Assertions.assertEquals(objectList, list);
        return list;
    }

    /**
     * Generates a ComplexList in non-interleaved format of random complex numbers of the given size.
     * @param size number of complex numbers in the list.
     * @return the ComplexList of random complex numbers.
     */
    private static ComplexList generateComplexNonInterleavedList(int size) {
        List<Complex> objectList = generateList(size);
        ComplexList list = ComplexList.nonInterleaved();
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

    @Test
    void testFromArray() {
        int size = 3;
        double[] fromArray1 = ThreadLocalRandom.current().doubles(size * 2, -Math.PI, Math.PI).toArray();
        ComplexList list = ComplexList.from(fromArray1);
        for (int i = 0; i < size; i++) {
            Assertions.assertEquals(Complex.ofCartesian(fromArray1[i * 2], fromArray1[(i * 2) + 1]), list.get(i));
        }
        double[] fromArray2 = ThreadLocalRandom.current().doubles(5, -Math.PI, Math.PI).toArray();
        Assertions.assertThrows(IllegalArgumentException.class, () -> ComplexList.from(fromArray2));

        //testing NullPointerException
        Assertions.assertThrows(NullPointerException.class, () -> ComplexList.from(null));
    }

    @Test
    void testFromRealAndImaginaryArray() {
        int size = 3;
        double[] fromRealArray1 = ThreadLocalRandom.current().doubles(size, -Math.PI, Math.PI).toArray();
        double[] fromImaginaryArray1 = ThreadLocalRandom.current().doubles(size, -Math.PI, Math.PI).toArray();
        ComplexList list = ComplexList.from(fromRealArray1, fromImaginaryArray1);
        for (int i = 0; i < size; i++) {
            Assertions.assertEquals(Complex.ofCartesian(fromRealArray1[i], fromImaginaryArray1[i]), list.get(i));
        }

        double[] fromRealArray2 = ThreadLocalRandom.current().doubles(5, -Math.PI, Math.PI).toArray();
        double[] fromImaginaryArray2 = ThreadLocalRandom.current().doubles(4, -Math.PI, Math.PI).toArray();
        Assertions.assertThrows(IllegalArgumentException.class, () -> ComplexList.from(fromRealArray2, fromImaginaryArray2));

        //testing NullPointerException
        Assertions.assertThrows(NullPointerException.class, () -> ComplexList.from(null, null));
        Assertions.assertThrows(NullPointerException.class, () -> ComplexList.from(fromRealArray2, null));
        Assertions.assertThrows(NullPointerException.class, () -> ComplexList.from(null, fromImaginaryArray2));
    }

    @Test
    void testFromEmptyInterleaved() {
        final List<Complex> l = ComplexList.from(new double[0]);
        final Complex c = Complex.ofCartesian(1, 2);
        l.add(c);
        Assertions.assertEquals(Arrays.asList(c), l);
    }

    @Test
    void testFromEmptyNonInterleaved() {
        final List<Complex> l = ComplexList.from(new double[0], new double[0]);
        final Complex c = Complex.ofCartesian(1, 2);
        l.add(c);
        Assertions.assertEquals(Arrays.asList(c), l);
    }

    @ParameterizedTest
    @MethodSource({"listImplementations"})
    void testGetAndSetMethod(List<Complex> l2) {
        List<Complex> l1 = new ArrayList<>();
        assertListOperation(list -> list.add(Complex.ofCartesian(42, 13)), l1, l2);
        assertListOperation(list -> list.addAll(1, list), l1, l2);
        assertListOperation(list -> list.addAll(list), l1, l2);
        assertListOperation(list -> list.set(2, Complex.ofCartesian(200, 1)), l1, l2);
        assertListOperation(list -> list.get(2), l1, l2);
    }

    @ParameterizedTest
    @MethodSource({"listImplementations"})
    void testAddAndAddAllList(List<Complex> l2) {
        List<Complex> l1 = new ArrayList<>();
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
    }

    @ParameterizedTest
    @MethodSource({"listImplementations"})
    void testAddAndAddAtIndexBranchCondition(List<Complex> l4) {
        //Testing add at an index for branch condition (size == realAndImagParts.length >>> 1) and (size == real.length)
        List<Complex> l3 = new ArrayList<>();
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
    }

    @ParameterizedTest
    @MethodSource({"listImplementations"})
    void testAddAndAddAllEnsureCapacityBranchConditions(List<Complex> l6) {
        //Testing branch condition (newArrayCapacity < minArrayCapacity) in ensureCapacity
        int size = 5;
        List<Complex> list1 = new ArrayList<>(size);
        IntStream.range(0, size).mapToObj(i -> Complex.ofCartesian(i, -i)).forEach(list1::add);

        List<Complex> l5 = new ArrayList<>();
        assertListOperation(list -> list.add(Complex.ofCartesian(1, 2)), l5, l6);
        // Expand the list by doubling in size until at the known minArrayCapacity
        while (l5.size() < 8) {
            assertListOperation(list -> list.addAll(list), l5, l6);
        }
        assertListOperation(list -> list.addAll(list1), l5, l6);
    }

    @ParameterizedTest
    @MethodSource({"listImplementations"})
    void testAddingEmptyListToEmptyList(List<Complex> l6) {
        //Test for adding an empty list to an empty list
        List<Complex> l5 = new ArrayList<>();
        List<Complex> list = new ArrayList<>();
        assertListOperation(l -> l.addAll(list), l5, l6);
        assertListOperation(l -> l.addAll(0, list), l5, l6);
    }

    @ParameterizedTest
    @MethodSource({"listImplementationsWithSize"})
    void testSetAddAndAddAllNullPointerException(List<Complex> list1, int size) {
        // Reference copy. This could be populated in a method.
        // It should not be a ComplexList to avoid testing
        // an implementation against itself.
        List<Complex> copy = new ArrayList<>();
        IntStream.range(0, size).forEach(i -> copy.add(Complex.ofCartesian(i, -i)));

        // Populate the list under test
        list1.addAll(copy);

        // Check it worked
        Assertions.assertEquals(copy, list1);

        Assertions.assertThrows(NullPointerException.class, () -> list1.add(null));
        Assertions.assertThrows(NullPointerException.class, () -> list1.add(0, null));
        Assertions.assertThrows(NullPointerException.class, () -> list1.set(1, null));

        List<Complex> list2 = generateList(3);
        list2.set(1, null);
        Assertions.assertThrows(NullPointerException.class, () -> list1.addAll(list2));
        Assertions.assertThrows(NullPointerException.class, () -> list1.addAll(0, list2));

        // Check no modifications were made
        Assertions.assertEquals(copy, list1);
    }

    @ParameterizedTest
    @MethodSource({"listImplementations"})
    void testRemove(List<Complex> l2) {
        List<Complex> l1 = new ArrayList<>();
        assertListOperation(list -> list.add(Complex.ofCartesian(42, 13)), l1, l2);
        assertListOperation(list -> list.addAll(list), l1, l2);
        assertListOperation(list -> list.remove(0), l1, l2);
        assertListOperation(list -> list.remove(0), l1, l2);
    }

    @ParameterizedTest
    @MethodSource({"listImplementations"})
    void testGetAndSetIndexOutOfBoundExceptions(List<Complex> l) {
        // Empty list throws
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> l.get(0));
        int size = 5;
        IntStream.range(0, size).mapToObj(i -> Complex.ofCartesian(i, -i)).forEach(l::add);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> l.get(-1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> l.get(size));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> l.get(size + 1));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> l.set(-2, Complex.ofCartesian(200, 1)));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> l.set(size, Complex.ofCartesian(200, 1)));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> l.set(size + 1, Complex.ofCartesian(200, 1)));
    }

    @ParameterizedTest
    @MethodSource({"listImplementations"})
    void testAddIndexOutOfBoundExceptions(List<Complex> l) {
        int size = 5;
        IntStream.range(0, size).mapToObj(i -> Complex.ofCartesian(i, -i)).forEach(l::add);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () ->
            l.add(-1, Complex.ofCartesian(42, 13)));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () ->
            l.add(size + 1, Complex.ofCartesian(42, 13)));
    }

    @ParameterizedTest
    @MethodSource({"listImplementationsWithSize"})
    void testRemoveIndexOutOfBoundExceptions(List<Complex> l, int size) {
        IntStream.range(0, size).forEach(i -> l.add(Complex.ofCartesian(i, -i)));
        l.remove(0);
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> l.remove(1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> l.remove(-1));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 10})
    void testConstructor(int size) {
        List<Complex> l1 = new ArrayList<>(size);
        List<Complex> l2 = ComplexList.interleaved(size);
        List<Complex> l3 = new ArrayList<>(size);
        List<Complex> l4 = ComplexList.nonInterleaved(size);

        Assertions.assertEquals(l1, l2);
        Assertions.assertEquals(l3, l4);

        assertListOperation(l -> l.add(Complex.ofCartesian(10, 20)), l1, l2);
        assertListOperation(l -> {
            l.add(1, Complex.ofCartesian(10, 20));
            return Boolean.TRUE;
        }, l1, l2);
        assertListOperation(l -> l.addAll(1, l), l1, l2);

        assertListOperation(l -> l.add(Complex.ofCartesian(10, 20)), l3, l4);
        assertListOperation(l -> {
            l.add(1, Complex.ofCartesian(10, 20));
            return Boolean.TRUE;
        }, l3, l4);
        assertListOperation(l -> l.addAll(1, l), l3, l4);
    }

    @Test
    void testCapacityExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ComplexList.interleaved(
            MAX_CAPACITY_INTERLEAVED + 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ComplexList.nonInterleaved(
            MAX_CAPACITY_NON_INTERLEAVED + 1));

        // Set-up required sizes
        ComplexList list1 = ComplexList.interleaved();
        ComplexList list2 = ComplexList.nonInterleaved();
        List<Complex> l1 = new SizedList(Integer.MAX_VALUE);
        Assertions.assertThrows(OutOfMemoryError.class, () -> list1.addAll(l1));
        Assertions.assertThrows(OutOfMemoryError.class, () -> list2.addAll(l1));

        List<Complex> l2 = new SizedList(MAX_CAPACITY_INTERLEAVED + 1);
        List<Complex> l3 = new SizedList(MAX_CAPACITY_NON_INTERLEAVED + 1);
        Assertions.assertThrows(OutOfMemoryError.class, () -> list1.addAll(l2));
        Assertions.assertThrows(OutOfMemoryError.class, () -> list2.addAll(l3));
    }

    @Test
    void testReplaceAllComplexUnaryOperator() {
        List<Complex> objectList = generateList(10);
        ComplexList actualList1 = ComplexList.interleaved();
        ComplexList actualList2 = ComplexList.nonInterleaved();
        actualList1.addAll(objectList);
        actualList2.addAll(objectList);
        Assertions.assertThrows(NullPointerException.class, () -> actualList1.replaceAll((ComplexUnaryOperator<Void>) null));
        Assertions.assertThrows(NullPointerException.class, () -> actualList2.replaceAll((ComplexUnaryOperator<Void>) null));
        objectList.replaceAll(Complex::conj);
        actualList1.replaceAll(ComplexFunctions::conj);
        actualList2.replaceAll(ComplexFunctions::conj);
        Assertions.assertEquals(objectList, actualList1);
        Assertions.assertEquals(objectList, actualList2);
    }

    @Test
    void testReplaceAllComplexBinaryOperator() {
        List<Complex> objectList = generateList(10);
        double r = 2;
        double i = 3;
        Complex multiplier = Complex.ofCartesian(r, i);
        ComplexList actualList1 = ComplexList.interleaved();
        ComplexList actualList2 = ComplexList.nonInterleaved();
        actualList1.addAll(objectList);
        actualList2.addAll(objectList);
        objectList.replaceAll(c -> c.multiply(multiplier));
        actualList1.replaceAll((x, y, action) -> ComplexFunctions.multiply(x, y, r, i, action));
        actualList2.replaceAll((x, y, action) -> ComplexFunctions.multiply(x, y, r, i, action));
        Assertions.assertEquals(objectList, actualList1);
        Assertions.assertEquals(objectList, actualList2);
    }

    @Test
    void testReplaceAllComplexScalarFunction() {
        List<Complex> objectList = generateList(10);
        double factor = 2;
        ComplexList actualList1 = ComplexList.interleaved();
        ComplexList actualList2 = ComplexList.nonInterleaved();
        actualList1.addAll(objectList);
        actualList2.addAll(objectList);
        objectList.replaceAll(c -> c.pow(factor));
        actualList1.replaceAll((x, y, action) -> ComplexFunctions.pow(x, y, factor, action));
        actualList2.replaceAll((x, y, action) -> ComplexFunctions.pow(x, y, factor, action));
        Assertions.assertEquals(objectList, actualList1);
        Assertions.assertEquals(objectList, actualList2);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 10})
    void testForEachComplexConsumer(int size) {
        ComplexList expected1 = generateComplexInterleavedList(size);
        ComplexList expected2 = generateComplexNonInterleavedList(size);
        ArrayList<Complex> actual1 = new ArrayList<>();
        ArrayList<Complex> actual2 = new ArrayList<>();
        Assertions.assertThrows(NullPointerException.class, () -> expected1.forEach((ComplexConsumer) null));
        Assertions.assertThrows(NullPointerException.class, () -> expected2.forEach((ComplexConsumer) null));
        expected1.forEach((real, imaginary) -> actual1.add(Complex.ofCartesian(real, imaginary)));
        expected2.forEach((real, imaginary) -> actual2.add(Complex.ofCartesian(real, imaginary)));
        Assertions.assertEquals(expected1, actual1);
        Assertions.assertEquals(expected2, actual2);
    }

    @Test
    void testGetRealAndImaginary() {
        int size = 10;
        ComplexList list1 = generateComplexInterleavedList(size);
        ComplexList list2 = generateComplexNonInterleavedList(size);
        for (int i = 0; i < size; i++) {
            Assertions.assertEquals(list1.get(i).getReal(), list1.getReal(i), "real");
            Assertions.assertEquals(list1.get(i).getImaginary(), list1.getImaginary(i), "imaginary");
            Assertions.assertEquals(list2.get(i).getReal(), list2.getReal(i), "real");
            Assertions.assertEquals(list2.get(i).getImaginary(), list2.getImaginary(i), "imaginary");
        }
    }

    @Test
    void testSetRealAndImaginary() {
        int size = 10;
        ComplexList list1 = generateComplexInterleavedList(size);
        ComplexList list2 = generateComplexNonInterleavedList(size);
        for (int i = 0; i < size; i++) {
            final double value = Math.PI * i;
            list1.setReal(i, value);
            list1.setImaginary(i, value);
            list2.setReal(i, value);
            list2.setImaginary(i, value);
            Assertions.assertEquals(value, list1.get(i).getReal());
            Assertions.assertEquals(value, list1.get(i).getImaginary());
            Assertions.assertEquals(value, list2.get(i).getReal());
            Assertions.assertEquals(value, list2.get(i).getImaginary());
        }
    }

    @Test
    void testGetAndSetRealAndImaginaryIndexOutOfBoundsException() {
        ComplexList list1 = ComplexList.interleaved();
        ComplexList list2 = ComplexList.nonInterleaved();
        // Empty list throws
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list1.getReal(0));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list1.getImaginary(0));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list2.getReal(0));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list2.getImaginary(0));

        int size = 5;
        IntStream.range(0, size).mapToObj(i -> Complex.ofCartesian(i, -i)).forEach(list1::add);
        IntStream.range(0, size).mapToObj(i -> Complex.ofCartesian(i, -i)).forEach(list2::add);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list1.getReal(-1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list1.getReal(size));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list1.getReal(size + 1));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list2.getReal(-1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list2.getReal(size));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list2.getReal(size + 1));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list1.getImaginary(-1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list1.getImaginary(size));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list1.getImaginary(size + 1));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list2.getImaginary(-1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list2.getImaginary(size));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list2.getImaginary(size + 1));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list1.setReal(-2, 200));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list1.setReal(size, 200));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list1.setReal(size + 1, 200));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list2.setReal(-2, 200));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list2.setReal(size, 200));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list2.setReal(size + 1, 200));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list1.setImaginary(-2, 200));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list1.setImaginary(size, 200));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list1.setImaginary(size + 1, 200));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list2.setImaginary(-2, 200));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list2.setImaginary(size, 200));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list2.setImaginary(size + 1, 200));
    }

    @Test
    void testToArrayRealAndImaginary() {
        ComplexList list1 = generateComplexInterleavedList(10);
        ComplexList list2 = generateComplexNonInterleavedList(10);
        double[] expectedReal1 = list1.stream().mapToDouble(Complex::getReal).toArray();
        double[] actualReal1 = list1.toArrayReal();
        double[] expectedReal2 = list2.stream().mapToDouble(Complex::getReal).toArray();
        double[] actualReal2 = list2.toArrayReal();
        Assertions.assertArrayEquals(expectedReal1, actualReal1);
        Assertions.assertArrayEquals(expectedReal2, actualReal2);
        double[] expectedImaginary1 = list1.stream().mapToDouble(Complex::getImaginary).toArray();
        double[] actualImaginary1 = list1.toArrayImaginary();
        double[] expectedImaginary2 = list2.stream().mapToDouble(Complex::getImaginary).toArray();
        double[] actualImaginary2 = list2.toArrayImaginary();
        Assertions.assertArrayEquals(expectedImaginary1, actualImaginary1);
        Assertions.assertArrayEquals(expectedImaginary2, actualImaginary2);

    }

    private static <T> void assertListOperation(Function<List<Complex>, T> operation,
                                                List<Complex> l1, List<Complex> l2) {
        T t1 = operation.apply(l1);
        T t2 = operation.apply(l2);
        Assertions.assertEquals(t1, t2);
        Assertions.assertEquals(l1, l2);
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
