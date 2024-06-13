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

package org.apache.commons.numbers.arrays;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link IndexSupport}.
 */
class IndexSupportTest {

    @ParameterizedTest
    @MethodSource
    void testCheckIndex(int from, int to, int index) {
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> IndexSupport.checkIndex(from, to, index));
        final int[] k1 = new int[] {index};
        final int[] k2 = new int[] {from, to - 1, index};
        final int[] k3 = new int[] {index, from, to - 1};
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> IndexSupport.checkIndices(from, to, k1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> IndexSupport.checkIndices(from, to, k2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> IndexSupport.checkIndices(from, to, k3));
    }

    static Stream<Arguments> testCheckIndex() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of(0, 10, -1));
        builder.add(Arguments.of(0, 10, Integer.MIN_VALUE));
        builder.add(Arguments.of(0, 10, 10));
        builder.add(Arguments.of(5, 10, 0));
        builder.add(Arguments.of(5, Integer.MAX_VALUE, Integer.MAX_VALUE));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testCheckFromToIndex(int from, int to, int length) {
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> IndexSupport.checkFromToIndex(from, to, length));
    }

    static Stream<Arguments> testCheckFromToIndex() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // fromIndex < 0
        builder.add(Arguments.of(-1, 10, 10));
        builder.add(Arguments.of(Integer.MIN_VALUE, 10, 10));
        builder.add(Arguments.of(Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
        // fromIndex > toIndex
        builder.add(Arguments.of(2, 1, 10));
        builder.add(Arguments.of(20, 10, 10));
        builder.add(Arguments.of(0, -1, 10));
        // toIndex > length
        builder.add(Arguments.of(0, 11, 10));
        builder.add(Arguments.of(0, Integer.MAX_VALUE, Integer.MAX_VALUE - 1));
        // length < 0
        builder.add(Arguments.of(0, 1, -1));
        builder.add(Arguments.of(0, 1, Integer.MIN_VALUE));
        builder.add(Arguments.of(0, Integer.MAX_VALUE, Integer.MIN_VALUE));
        return builder.build();
    }
}
