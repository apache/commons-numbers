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

import java.util.BitSet;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link HashIndexSet}.
 */
class HashIndexSetTest {

    @Test
    void testInvalidCapacityThrows() {
        final int maxCapacity = 1 << 29;
        Assertions.assertThrows(IllegalArgumentException.class, () -> HashIndexSet.create(maxCapacity + 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> HashIndexSet.create(Integer.MAX_VALUE));
    }

    @Test
    void testInvalidIndexThrows() {
        final HashIndexSet set = HashIndexSet.create(16);
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> set.add(-1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> set.add(Integer.MIN_VALUE));
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 32})
    void testCapacityExceededThrows(int capacity) {
        final HashIndexSet set = HashIndexSet.create(capacity);
        IntStream.range(0, capacity).forEach(set::add);
        // Now add more and expect an exception.
        // With a load factor of 0.5 we can only add twice the requested capacity
        // before an exception occurs.
        Assertions.assertThrows(IllegalStateException.class, () -> {
            for (int i = capacity; i < capacity * 2; i++) {
                set.add(i);
            }
        });
    }

    @ParameterizedTest
    @MethodSource(value = {"testIndices"})
    void testAdd(int[] indices, int capacity) {
        final HashIndexSet set = HashIndexSet.create(capacity);
        final BitSet ref = new BitSet(capacity);
        for (final int i : indices) {
            final boolean observed = ref.get(i);
            // Add returns true if not already present
            Assertions.assertEquals(!observed, set.add(i), () -> String.valueOf(i));
            ref.set(i);
        }
    }

    static Stream<Arguments> testIndices() {
        final Stream.Builder<Arguments> builder = Stream.builder();

        builder.accept(Arguments.of(new int[] {1, 2}, 10));
        builder.accept(Arguments.of(new int[] {1, 2, 3, 4, 5}, 10));

        // Add duplicates
        builder.accept(Arguments.of(new int[] {1, 1, 1, 2, 3, 4, 5, 6, 7}, 10));
        builder.accept(Arguments.of(new int[] {5, 6, 2, 2, 3, 8, 1, 1, 4, 3}, 10));
        builder.accept(Arguments.of(new int[] {2, 2, 2, 2, 2}, 10));
        builder.accept(Arguments.of(new int[] {2000, 2001, 2000, 2001}, 2010));

        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create();
        for (final int size : new int[] {5, 500}) {
            // Sparse
            builder.accept(Arguments.of(rng.ints(10, 0, size).toArray(), size));
            // With duplicates
            builder.accept(Arguments.of(rng.ints(size, 0, size).toArray(), size));
            builder.accept(Arguments.of(rng.ints(size, 0, size >> 1).toArray(), size));
            builder.accept(Arguments.of(rng.ints(size, 0, size >> 2).toArray(), size));
        }

        return builder.build();
    }
}
