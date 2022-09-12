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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Tests for {@link ComplexList}.
 */
class ComplexListTest {

    private static final int MAX_CAPACITY_INTERLEAVED = (Integer.MAX_VALUE - 9) / 2;
    private static final int MAX_CAPACITY_NON_INTERLEAVED = Integer.MAX_VALUE - 9;

    /**
     * Generate a stream of arguments containing empty {@code Complex<List>} implementations.
     *
     * @return the stream of arguments
     */
    static Stream<Arguments> listImplementations() {
        return Stream.of(
            Arguments.of(ComplexList.interleaved()),
            Arguments.of(ComplexList.nonInterleaved())
        );
    }

    /**
     * Generate a stream of arguments containing {@code Complex<List>} implementations with a set size.
     *
     * @return the stream of arguments
     */
    static Stream<Arguments> listImplementationsWithSize() {
        return Stream.of(
            Arguments.of(ComplexList.interleaved(), 2),
            Arguments.of(ComplexList.nonInterleaved(), 2)
        );
    }

    /**
     * Helper method for testing list capacity exceptions.
     * Generates a stream of arguments containing {@code Complex<List>} constructors and their capacities.
     *
     * @return the stream of arguments
     */
    static Stream<Arguments> listConstructorsWithCapacity() {
        return Stream.of(
            Arguments.of((IntFunction<List<Complex>>) ComplexList::interleaved, MAX_CAPACITY_INTERLEAVED),
            Arguments.of((IntFunction<List<Complex>>) ComplexList::nonInterleaved, MAX_CAPACITY_NON_INTERLEAVED)
        );
    }

    /**
     * Generates a stream of arguments containing populated {@code Complex<List>} implementations of a set size.
     *
     * @return the stream of arguments
     */
    static Stream<Arguments> generateList() {
        return Stream.of(
            Arguments.of(generateComplexInterleavedList(10)),
            Arguments.of(generateComplexNonInterleavedList(10))
        );
    }

    /**
     * Generates a ComplexList in interleaved format of random complex numbers of the given size.
     *
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
     *
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
     *
     * @param size number of complex numbers in the list.
     * @return the list of random complex numbers.
     */
    private static List<Complex> generateList(int size) {
        return ThreadLocalRandom.current().doubles(size, -Math.PI, Math.PI)
            .mapToObj(Complex::ofCis).collect(Collectors.toList());
    }

    @Test
    void testFromInterleaved() {
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
    void testFromNonInterleaved() {
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
        final List<Complex> list = ComplexList.from(new double[0]);
        final Complex c = Complex.ofCartesian(1, 2);
        list.add(c);
        Assertions.assertEquals(Arrays.asList(c), list);
    }

    @Test
    void testFromEmptyNonInterleaved() {
        final List<Complex> list = ComplexList.from(new double[0], new double[0]);
        final Complex c = Complex.ofCartesian(1, 2);
        list.add(c);
        Assertions.assertEquals(Arrays.asList(c), list);
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
    void testAddAndAddAtIndexBranchCondition(List<Complex> l2) {
        //Testing add at an index for branch condition (size == realAndImagParts.length >>> 1) and (size == real.length)
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
        assertListOperation(list -> {
            list.add(1, Complex.ofCartesian(10, 20));
            return Boolean.TRUE;
        }, l1, l2);
    }

    @ParameterizedTest
    @MethodSource({"listImplementations"})
    void testAddAndAddAllEnsureCapacityBranchConditions(List<Complex> l2) {
        //Testing branch condition (newArrayCapacity < minArrayCapacity) in ensureCapacity
        int size = 5;
        List<Complex> list1 = new ArrayList<>(size);
        IntStream.range(0, size).mapToObj(i -> Complex.ofCartesian(i, -i)).forEach(list1::add);

        List<Complex> l1 = new ArrayList<>();
        assertListOperation(list -> list.add(Complex.ofCartesian(1, 2)), l1, l2);
        // Expand the list by doubling in size until at the known minArrayCapacity
        while (l1.size() < 8) {
            assertListOperation(list -> list.addAll(list), l1, l2);
        }
        assertListOperation(list -> list.addAll(list1), l1, l2);
    }

    @ParameterizedTest
    @MethodSource({"listImplementations"})
    void testAddingEmptyListToEmptyList(List<Complex> l2) {
        //Test for adding an empty list to an empty list
        List<Complex> l1 = new ArrayList<>();
        List<Complex> list2 = new ArrayList<>();
        assertListOperation(list -> list.addAll(list2), l1, l2);
        assertListOperation(list -> list.addAll(0, list2), l1, l2);
    }

    @ParameterizedTest
    @MethodSource({"listImplementationsWithSize"})
    void testSetAddAndAddAllNullPointerException(List<Complex> list, int size) {
        List<Complex> expected = new ArrayList<>();
        IntStream.range(0, size).forEach(i -> expected.add(Complex.ofCartesian(i, -i)));

        list.addAll(expected);

        Assertions.assertEquals(expected, list);

        Assertions.assertThrows(NullPointerException.class, () -> list.add(null));
        Assertions.assertThrows(NullPointerException.class, () -> list.add(0, null));
        Assertions.assertThrows(NullPointerException.class, () -> list.set(1, null));

        List<Complex> list2 = generateList(3);
        list2.set(1, null);
        Assertions.assertThrows(NullPointerException.class, () -> list.addAll(list2));
        Assertions.assertThrows(NullPointerException.class, () -> list.addAll(0, list2));

        // Check no modifications were made
        Assertions.assertEquals(expected, list);
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
    void testGetAndSetIndexOutOfBoundExceptions(List<Complex> list) {
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

    @ParameterizedTest
    @MethodSource({"listImplementations"})
    void testAddIndexOutOfBoundExceptions(List<Complex> list) {
        int size = 5;
        IntStream.range(0, size).mapToObj(i -> Complex.ofCartesian(i, -i)).forEach(list::add);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () ->
            list.add(-1, Complex.ofCartesian(42, 13)));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () ->
            list.add(size + 1, Complex.ofCartesian(42, 13)));
    }

    @ParameterizedTest
    @MethodSource({"listImplementationsWithSize"})
    void testRemoveIndexOutOfBoundExceptions(List<Complex> list, int size) {
        IntStream.range(0, size).forEach(i -> list.add(Complex.ofCartesian(i, -i)));
        list.remove(0);
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.remove(1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.remove(-1));
    }

    /**
     * Helper method for testInitialSize.
     * Generates a stream of arguments containing {@code Complex<List>} constructors with different initial sizes.
     *
     * @return the stream of arguments
     */
    static Stream<Arguments> testInitialSize() {
        return Stream.of(
            Arguments.of((IntFunction<List<Complex>>) ComplexList::interleaved, 0),
            Arguments.of((IntFunction<List<Complex>>) ComplexList::interleaved, 1),
            Arguments.of((IntFunction<List<Complex>>) ComplexList::interleaved, 10),
            Arguments.of((IntFunction<List<Complex>>) ComplexList::nonInterleaved, 0),
            Arguments.of((IntFunction<List<Complex>>) ComplexList::nonInterleaved, 1),
            Arguments.of((IntFunction<List<Complex>>) ComplexList::nonInterleaved, 10)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInitialSize(IntFunction<List<Complex>> constructor, int size) {
        List<Complex> l1 = new ArrayList<>(size);
        List<Complex> l2 = constructor.apply(size);

        Assertions.assertEquals(l1, l2);

        assertListOperation(list -> list.add(Complex.ofCartesian(10, 20)), l1, l2);
        assertListOperation(list -> {
            list.add(1, Complex.ofCartesian(10, 20));
            return Boolean.TRUE;
        }, l1, l2);
        assertListOperation(list -> list.addAll(1, list), l1, l2);
    }

    @ParameterizedTest
    @MethodSource({"listConstructorsWithCapacity"})
    void testCapacityIllegalArgumentException(IntFunction<List<Complex>> constructor, int maxCapacity) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> constructor.apply(-1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> constructor.apply(maxCapacity + 1));
    }

    @ParameterizedTest
    @MethodSource({"listImplementations"})
    void testCapacityOutOfMemoryException(List<Complex> list) {
        // Set-up required sizes
        List<Complex> tooLarge = new SizedList(Integer.MAX_VALUE);
        Assertions.assertThrows(OutOfMemoryError.class, () -> list.addAll(tooLarge));
    }

    @ParameterizedTest
    @MethodSource({"listConstructorsWithCapacity"})
    void testCapacityOutOfMemoryExceptions(IntFunction<List<Complex>> constructor, int maxCapacity) {
        // Set-up required sizes
        List<Complex> list = constructor.apply(0);
        List<Complex> tooLarge = new SizedList(maxCapacity + 1);
        Assertions.assertThrows(OutOfMemoryError.class, () -> list.addAll(tooLarge));
    }

    @ParameterizedTest
    @MethodSource({"listImplementations"})
    void testReplaceAllComplexUnaryOperator(ComplexList actualList) {
        List<Complex> objectList = generateList(10);
        actualList.addAll(objectList);
        Assertions.assertThrows(NullPointerException.class, () -> actualList.replaceAll((ComplexUnaryOperator<Void>) null));
        objectList.replaceAll(Complex::conj);
        actualList.replaceAll(ComplexFunctions::conj);
        Assertions.assertEquals(objectList, actualList);
    }

    @ParameterizedTest
    @MethodSource({"listImplementations"})
    void testReplaceAllComplexBinaryOperator(ComplexList actualList) {
        List<Complex> objectList = generateList(10);
        double r = 2;
        double i = 3;
        Complex multiplier = Complex.ofCartesian(r, i);
        actualList.addAll(objectList);
        objectList.replaceAll(c -> c.multiply(multiplier));
        actualList.replaceAll((x, y, action) -> ComplexFunctions.multiply(x, y, r, i, action));
        Assertions.assertEquals(objectList, actualList);

    }

    @ParameterizedTest
    @MethodSource({"listImplementations"})
    void testReplaceAllComplexScalarFunction(ComplexList actualList) {
        List<Complex> objectList = generateList(10);
        double factor = 2;
        actualList.addAll(objectList);
        objectList.replaceAll(c -> c.pow(factor));
        actualList.replaceAll((x, y, action) -> ComplexFunctions.pow(x, y, factor, action));
        Assertions.assertEquals(objectList, actualList);
    }

    /**
     * Helper method for testForEachComplexConsumer.
     * Generates a stream of arguments containing populated {@code Complex<List>} implementations of different set sizes.
     *
     * @return the stream of arguments
     */
    static Stream<Arguments> testForEachComplexConsumer() {
        return Stream.of(
            Arguments.of(generateComplexInterleavedList(0)),
            Arguments.of(generateComplexInterleavedList(10)),
            Arguments.of(generateComplexNonInterleavedList(0)),
            Arguments.of(generateComplexNonInterleavedList(10))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testForEachComplexConsumer(ComplexList expected) {
        ArrayList<Complex> actual = new ArrayList<>();
        Assertions.assertThrows(NullPointerException.class, () -> expected.forEach((ComplexConsumer) null));
        expected.forEach((real, imaginary) -> actual.add(Complex.ofCartesian(real, imaginary)));
        Assertions.assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource({"generateList"})
    void testGetRealAndImaginary(ComplexList expected) {
        for (int i = 0; i < expected.size(); i++) {
            Assertions.assertEquals(expected.get(i).getReal(), expected.getReal(i), "real");
            Assertions.assertEquals(expected.get(i).getImaginary(), expected.getImaginary(i), "imaginary");
        }
    }

    @ParameterizedTest
    @MethodSource({"generateList"})
    void testSetRealAndImaginary(ComplexList expected) {
        for (int i = 0; i < expected.size(); i++) {
            final double real = Math.PI * (i + 1);
            final double imag = Math.E * (i + 1);
            expected.setReal(i, real);
            expected.setImaginary(i, imag);
            Assertions.assertEquals(real, expected.get(i).getReal());
            Assertions.assertEquals(imag, expected.get(i).getImaginary());
        }
    }

    @ParameterizedTest
    @MethodSource({"listImplementations"})
    void testGetAndSetRealAndImaginaryIndexOutOfBoundsException(ComplexList list) {
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

    @ParameterizedTest
    @MethodSource({"generateList"})
    void testToArrayRealAndImaginary(ComplexList list) {
        double[] expectedReal = list.stream().mapToDouble(Complex::getReal).toArray();
        double[] actualReal = list.toArrayReal();
        Assertions.assertArrayEquals(expectedReal, actualReal);
        double[] expectedImaginary = list.stream().mapToDouble(Complex::getImaginary).toArray();
        double[] actualImaginary = list.toArrayImaginary();
        Assertions.assertArrayEquals(expectedImaginary, actualImaginary);
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
