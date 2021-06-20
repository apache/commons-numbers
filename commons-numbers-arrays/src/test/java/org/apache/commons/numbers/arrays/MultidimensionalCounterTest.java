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

import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for the {@link MultidimensionalCounter} class.
 *
 */
class MultidimensionalCounterTest {
    @Test
    void testPreconditions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> MultidimensionalCounter.of(0, 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> MultidimensionalCounter.of(2, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> MultidimensionalCounter.of(-1, 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> MultidimensionalCounter.of(-1, -1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> MultidimensionalCounter.of(Integer.MAX_VALUE, 2, Integer.MAX_VALUE));

        final MultidimensionalCounter c = MultidimensionalCounter.of(2, 3);
        Assertions.assertThrows(IllegalArgumentException.class, () -> c.toUni(1, 1, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> c.toUni(3, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> c.toUni(0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> c.toMulti(-1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> c.toMulti(6));
    }

    @Test
    void testMulti2UniConversion() {
        final MultidimensionalCounter c = MultidimensionalCounter.of(2, 4, 5);
        Assertions.assertEquals(33, c.toUni(1, 2, 3));

        for (int i = 0, max = c.getSize(); i < max; i++) {
            Assertions.assertEquals(i, c.toUni(c.toMulti(i)));
        }
    }

    @Test
    void testAccessors() {
        final int[] originalSize = new int[] {2, 6, 5};
        final MultidimensionalCounter c = MultidimensionalCounter.of(originalSize);
        final int nDim = c.getDimension();
        Assertions.assertEquals(nDim, originalSize.length);

        final int[] size = c.getSizes();
        for (int i = 0; i < nDim; i++) {
            Assertions.assertEquals(originalSize[i], size[i]);
        }
    }

    @Test
    void testIterationConsistency() {
        final MultidimensionalCounter c = MultidimensionalCounter.of(2, 3, 4);
        final int[][] expected = new int[][] {
            {0, 0, 0},
            {0, 0, 1},
            {0, 0, 2},
            {0, 0, 3},
            {0, 1, 0},
            {0, 1, 1},
            {0, 1, 2},
            {0, 1, 3},
            {0, 2, 0},
            {0, 2, 1},
            {0, 2, 2},
            {0, 2, 3},
            {1, 0, 0},
            {1, 0, 1},
            {1, 0, 2},
            {1, 0, 3},
            {1, 1, 0},
            {1, 1, 1},
            {1, 1, 2},
            {1, 1, 3},
            {1, 2, 0},
            {1, 2, 1},
            {1, 2, 2},
            {1, 2, 3}
        };

        final int totalSize = c.getSize();
        Assertions.assertEquals(expected.length, totalSize);

        final int nDim = c.getDimension();
        for (int i = 0; i < totalSize; i++) {
            Assertions.assertEquals(i, c.toUni(expected[i]),
                                    "Wrong unidimensional index for [" + i + "]");

            final int[] indices = c.toMulti(i);
            for (int dimIndex = 0; dimIndex < nDim; dimIndex++) {
                Assertions.assertEquals(expected[i][dimIndex], indices[dimIndex],
                                        "Wrong multidimensional index for [" + i + "][" + dimIndex + "]");
            }
        }
    }

    @Test
    void testToString() {
        final int[] sizes = new int[] {7, 5, 3, 1};
        final MultidimensionalCounter c = MultidimensionalCounter.of(sizes);
        Assertions.assertEquals(Arrays.toString(sizes), c.toString());
    }

    // Illustrates how to recover the iterator functionality that existed
    // in Commons Math (v3.6.1) but was not ported to "Commons Numbers".
    @Test
    void testCommonsMathIterator() {
        final int[] sizes = new int[] {3, 2, 5};
        final org.apache.commons.math3.util.MultidimensionalCounter.Iterator cmIter =
            new org.apache.commons.math3.util.MultidimensionalCounter(sizes).iterator();

        final MultidimensionalCounter counter = MultidimensionalCounter.of(sizes);

        Assertions.assertTrue(cmIter.hasNext());
        Assertions.assertTrue(counter.getSize() > 0);

        for (int i = 0; i < counter.getSize(); i++) {
            cmIter.next();
            Assertions.assertArrayEquals(cmIter.getCounts(), counter.toMulti(i));
        }

        Assertions.assertFalse(cmIter.hasNext());
    }
}
