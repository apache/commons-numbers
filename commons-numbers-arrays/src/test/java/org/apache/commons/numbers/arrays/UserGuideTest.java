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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for examples contained in the user guide.
 */
class UserGuideTest {

    @Test
    void testSortInPlace1() {
        double[] x = {3, 1, 2};
        double[] y = {1, 2, 3};
        double[] z = {0, 5, 7};
        SortInPlace.ASCENDING.apply(x, y, z);
        Assertions.assertArrayEquals(new double[] {1, 2, 3}, x);
        Assertions.assertArrayEquals(new double[] {2, 3, 1}, y);
        Assertions.assertArrayEquals(new double[] {5, 7, 0}, z);
    }

    @Test
    void testMultidimensionalCounter1() {
        MultidimensionalCounter c = MultidimensionalCounter.of(100, 50);
        int size = c.getSize();
        Assertions.assertEquals(5000, size);

        int index = 233;
        int[] indices1 = c.toMulti(index);
        int[] indices2 = c.toMulti(index + 1);
        Assertions.assertArrayEquals(new int[] {4, 33}, indices1);
        Assertions.assertArrayEquals(new int[] {4, 34}, indices2);

        int index1 = c.toUni(4, 33); // varargs
        int index2 = c.toUni(indices1);
        Assertions.assertEquals(index, index1);
        Assertions.assertEquals(index, index2);

        Assertions.assertArrayEquals(new int[] {0, 49}, c.toMulti(49));
        Assertions.assertArrayEquals(new int[] {1, 0}, c.toMulti(50));
        Assertions.assertArrayEquals(new int[] {99, 49}, c.toMulti(4999));
    }
}
