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
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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
        assertListOperation(list -> {
            list.add(Complex.ofCartesian(42, 13));
            list.add(1, Complex.ofCartesian(11, 12));
            list.add(Complex.ofCartesian(13, 14));
            list.add(Complex.ofCartesian(15, 16));
            list.add(Complex.ofCartesian(17, 18));
            list.addAll(1, list);
            list.add(Complex.ofCartesian(18, 19));
            list.add(Complex.ofCartesian(11, 12));
            list.add(Complex.ofCartesian(13, 14));
            list.add(Complex.ofCartesian(15, 16));
            list.add(Complex.ofCartesian(17, 18));
            return list.add(Complex.ofCartesian(11, 12));
        });

        assertListOperation(list -> {
            list.add(Complex.ofCartesian(42, 13));
            list.add(1, Complex.ofCartesian(11, 12));
            list.add(Complex.ofCartesian(13, 14));
            list.add(Complex.ofCartesian(15, 16));
            list.add(Complex.ofCartesian(17, 18));
            list.addAll(1, list);
            list.add(Complex.ofCartesian(18, 19));
            list.add(Complex.ofCartesian(11, 12));
            list.add(Complex.ofCartesian(13, 14));
            list.add(Complex.ofCartesian(15, 16));
            list.add(Complex.ofCartesian(17, 18));
            list.add(1, Complex.ofCartesian(11, 12));
        });

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
        list.add(Complex.ofCartesian(42, 13));
        list.addAll(1, list);
        list.addAll(list);
        list.set(2, Complex.ofCartesian(200, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.get(4));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.get(5));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.set(-2, Complex.ofCartesian(200, 1)));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.set(4, Complex.ofCartesian(200, 1)));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.set(5, Complex.ofCartesian(200, 1)));
    }

    @Test
    void testAddIndexOutOfBoundExceptions() {
        ComplexList list = new ComplexList();
        list.add(Complex.ofCartesian(42, 13));
        list.addAll(1, list);
        list.addAll(list);
        list.set(2, Complex.ofCartesian(200, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () ->
            list.add(-1, Complex.ofCartesian(42, 13)));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () ->
            list.add(5, Complex.ofCartesian(42, 13)));
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
        ComplexList list = new ComplexList(size);
        list.add(Complex.ofCartesian(20, 12));
        list.addAll(list);
        Assertions.assertEquals(Complex.ofCartesian(20, 12), list.get(0));
        Assertions.assertEquals(Complex.ofCartesian(20, 12), list.get(1));
    }

    @Test
    void testCapacityExceptions() {
        Assertions.assertDoesNotThrow(() -> new ComplexList().ensureCapacity(64), () -> "unexpected exception for ensureCapacity(64)");

        assertOutOfMemoryErrorOnConstructor(() -> new ComplexList(ComplexList.MAX_CAPACITY + 1));

        assertOutOfMemoryErrorOnEnsureCapacity(() -> new ComplexList().ensureCapacity(ComplexList.MAX_CAPACITY + 1));

        int oldCapacity = ComplexList.MAX_CAPACITY * 2 / 3;
        assertOutOfMemoryErrorOnArrayCopy(() ->  new ComplexList().ensureCapacity((int) (oldCapacity * 1.4)), () -> "unexpected exception for ensureCapacity(" + (oldCapacity * 1.4) + ")");
        assertOutOfMemoryErrorOnArrayCopy(() ->  new ComplexList().ensureCapacity((int) (oldCapacity * 1.5)), () -> "unexpected exception for ensureCapacity(" + (oldCapacity * 1.5) + ")");

        assertOutOfMemoryErrorOnEnsureCapacity(() ->  new ComplexList().ensureCapacity((int) (oldCapacity * 1.6)));

    }

    private static void assertOutOfMemoryErrorOnArrayCopy(Executable executable, Supplier<String> msgSupplier) {
        try {
            executable.execute();
        } catch (Throwable oom) {
            Assertions.assertSame(oom.getClass(), OutOfMemoryError.class, msgSupplier);
            Assertions.assertTrue(oom.getStackTrace().length > 1, msgSupplier);
            StackTraceElement firstStackElement = oom.getStackTrace()[0];
            Assertions.assertEquals("copyOf", firstStackElement.getMethodName(), msgSupplier);
            Assertions.assertEquals(Arrays.class.getName(), firstStackElement.getClassName(), msgSupplier);
        }
    }

    private static void assertOutOfMemoryErrorOnEnsureCapacity(Executable executable) {
        try {
            executable.execute();
            Assertions.assertTrue(false, "failed to throw OutOfMemory Error");
        } catch (Throwable oom) {
            Assertions.assertSame(oom.getClass(), OutOfMemoryError.class);
            Assertions.assertTrue(oom.getStackTrace().length > 1);
            StackTraceElement firstStackElement = oom.getStackTrace()[0];
            Assertions.assertEquals(ComplexList.class.getName(), firstStackElement.getClassName());
            Assertions.assertEquals("ensureCapacityInternal", firstStackElement.getMethodName());
        }
    }

    private static void assertOutOfMemoryErrorOnConstructor(Executable executable) {
        try {
            executable.execute();
            Assertions.assertTrue(false, "failed to throw OutOfMemory Error");
        } catch (Throwable oom) {
            Assertions.assertSame(oom.getClass(), OutOfMemoryError.class);
            Assertions.assertTrue(oom.getStackTrace().length > 1);
            StackTraceElement firstStackElement = oom.getStackTrace()[0];
            Assertions.assertEquals(ComplexList.class.getName(), firstStackElement.getClassName());
            Assertions.assertEquals("<init>", firstStackElement.getMethodName());
        }
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

    private static void assertListOperation(Consumer<List<Complex>> operation) {
        assertListOperation(operation, new ArrayList<>(), new ComplexList());
    }

    private static void assertListOperation(Consumer<List<Complex>> operation,
                                            List<Complex> l1, List<Complex> l2) {
        operation.accept(l1);
        operation.accept(l2);
        Assertions.assertEquals(l1, l2);
    }
}
