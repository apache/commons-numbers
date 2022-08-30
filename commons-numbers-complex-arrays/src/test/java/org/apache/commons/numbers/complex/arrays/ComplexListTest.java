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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

public class ComplexListTest {

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
        assertListOperation(list -> {
            list.addAll(list);
            return Boolean.TRUE;
        }, l5, l6);
        assertListOperation(list -> {
            list.addAll(list);
            return Boolean.TRUE;
        }, l5, l6);
        assertListOperation(list -> {
            list.addAll(list);
            return Boolean.TRUE;
        }, l5, l6);
        assertListOperation(list -> {
            list.addAll(list1);
            return Boolean.TRUE;
        }, l5, l6);

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
        ComplexList list = new ComplexList();
        list.add(Complex.ofCartesian(42, 13));
        list.addAll(list);
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

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ComplexList(ComplexList.MAX_CAPACITY + 1));

        // Set-up required sizes
        ComplexList list = new ComplexList();
        List<Complex> l = new SizedList(Integer.MAX_VALUE);
        Assertions.assertThrows(OutOfMemoryError.class, () -> list.addAll(l));

        List<Complex> l2 = new SizedList(ComplexList.MAX_CAPACITY + 1);
        Assertions.assertThrows(OutOfMemoryError.class, () -> list.addAll(l2));
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
