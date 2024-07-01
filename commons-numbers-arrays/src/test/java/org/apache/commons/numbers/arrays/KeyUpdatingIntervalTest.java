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
 * Test for {@link KeyUpdatingInterval}.
 */
class KeyUpdatingIntervalTest {

    @ParameterizedTest
    @MethodSource
    void testSearch(int[] keys, int left, int right) {
        // Clip to correct range
        final int l = left < 0 ? 0 : left;
        final int r = right < 0 ? keys.length - 1 : right;
        for (int i = l; i <= r; i++) {
            final int k = keys[i];
            // Unspecified index when key is present
            Assertions.assertEquals(k, keys[KeyUpdatingInterval.searchLessOrEqual(keys, l, r, k)], "leq");
        }
        // Search above/below keys
        Assertions.assertEquals(l - 1, KeyUpdatingInterval.searchLessOrEqual(keys, l, r, keys[l] - 44), "leq below");
        Assertions.assertEquals(r, KeyUpdatingInterval.searchLessOrEqual(keys, l, r, keys[r] + 44), "leq above");
        // Search between neighbour keys
        for (int i = l + 1; i <= r; i++) {
            // Bound: keys[i-1] < k < keys[i]
            final int k1 = keys[i - 1];
            final int k2 = keys[i];
            for (int k = k1 + 1; k < k2; k++) {
                Assertions.assertEquals(i - 1, KeyUpdatingInterval.searchLessOrEqual(keys, l, r, k), "leq between");
            }
        }
    }

    static Stream<Arguments> testSearch() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final int allIndices = -1;
        builder.add(Arguments.of(new int[] {1}, allIndices, allIndices));
        builder.add(Arguments.of(new int[] {1, 2}, allIndices, allIndices));
        builder.add(Arguments.of(new int[] {1, 10}, allIndices, allIndices));
        builder.add(Arguments.of(new int[] {1, 2, 3}, allIndices, allIndices));
        builder.add(Arguments.of(new int[] {1, 4, 7}, allIndices, allIndices));
        builder.add(Arguments.of(new int[] {1, 4, 5, 7}, allIndices, allIndices));
        // Duplicates. These match binary search when found.
        builder.add(Arguments.of(new int[] {1, 1, 1, 1, 1, 1}, allIndices, allIndices));
        builder.add(Arguments.of(new int[] {1, 1, 1, 1, 3, 3, 3, 3, 3, 5, 5, 5, 5}, allIndices, allIndices));
        // Part of the range
        builder.add(Arguments.of(new int[] {1, 4, 5, 7, 13, 15}, 2, 4));
        builder.add(Arguments.of(new int[] {1, 4, 5, 7, 13, 15}, 0, 3));
        builder.add(Arguments.of(new int[] {1, 4, 5, 7, 13, 15}, 3, 5));
        return builder.build();
    }
}
