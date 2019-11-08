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
package org.apache.commons.numbers.combinatorics;

import java.util.Iterator;
import java.util.Comparator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link Combinations} class.
 */
public class CombinationsTest {
    @Test
    public void testGetN() {
        final int n = 5;
        final int k = 3;
        Assertions.assertEquals(n, Combinations.of(n, k).getN());
    }

    @Test
    public void testGetK() {
        final int n = 5;
        final int k = 3;
        Assertions.assertEquals(k, Combinations.of(n, k).getK());
    }

    @Test
    public void testLexicographicIterator() {
        checkLexicographicIterator(5, 3);
        checkLexicographicIterator(6, 4);
        checkLexicographicIterator(8, 2);
        checkLexicographicIterator(6, 1);
        checkLexicographicIterator(3, 3);
        checkLexicographicIterator(1, 1);
        checkLexicographicIterator(1, 0);
        checkLexicographicIterator(0, 0);
        checkLexicographicIterator(4, 2);
        checkLexicographicIterator(123, 2);
    }

    @Test
    public void testLexicographicComparatorWrongIterate1() {
        final int n = 5;
        final int k = 3;
        final Comparator<int[]> comp = Combinations.of(n, k).comparator();
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> comp.compare(new int[] {1},
                               new int[] {0, 1, 2}));
    }

    @Test
    public void testLexicographicComparatorWrongIterate2() {
        final int n = 5;
        final int k = 3;
        final Comparator<int[]> comp = Combinations.of(n, k).comparator();
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> comp.compare(new int[] {0, 1, 2},
                               new int[] {0, 1, 2, 3}));
    }

    @Test
    public void testLexicographicComparatorWrongIterate3() {
        final int n = 5;
        final int k = 3;
        final Comparator<int[]> comp = Combinations.of(n, k).comparator();
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> comp.compare(new int[] {1, 2, 5},
                               new int[] {0, 1, 2}));
    }

    @Test
    public void testLexicographicComparatorWrongIterate4() {
        final int n = 5;
        final int k = 3;
        final Comparator<int[]> comp = Combinations.of(n, k).comparator();
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> comp.compare(new int[] {1, 2, 4},
                               new int[] {-1, 1, 2}));
    }

    @Test
    public void testLexicographicComparator() {
        final int n = 5;
        final int k = 3;
        final Comparator<int[]> comp = Combinations.of(n, k).comparator();
        Assertions.assertEquals(1, comp.compare(new int[] {1, 2, 4},
                                                new int[] {1, 2, 3}));
        Assertions.assertEquals(-1, comp.compare(new int[] {0, 1, 4},
                                                 new int[] {0, 2, 4}));
        Assertions.assertEquals(0, comp.compare(new int[] {1, 3, 4},
                                                new int[] {1, 3, 4}));
    }

    /**
     * Check that iterates can be passed unsorted.
     */
    @Test
    public void testLexicographicComparatorUnsorted() {
        final int n = 5;
        final int k = 3;
        final Comparator<int[]> comp = Combinations.of(n, k).comparator();
        Assertions.assertEquals(1, comp.compare(new int[] {1, 4, 2},
                                                new int[] {1, 3, 2}));
        Assertions.assertEquals(-1, comp.compare(new int[] {0, 4, 1},
                                                 new int[] {0, 4, 2}));
        Assertions.assertEquals(0, comp.compare(new int[] {1, 4, 3},
                                                new int[] {1, 3, 4}));
    }

    @Test
    public void testEmptyCombination() {
        final Iterator<int[]> iter = Combinations.of(12345, 0).iterator();
        Assertions.assertTrue(iter.hasNext());
        final int[] c = iter.next();
        Assertions.assertEquals(0, c.length);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testFullSetCombination() {
        final int n = 67;
        final Iterator<int[]> iter = Combinations.of(n, n).iterator();
        Assertions.assertTrue(iter.hasNext());
        final int[] c = iter.next();
        Assertions.assertEquals(n, c.length);

        for (int i = 0; i < n; i++) {
            Assertions.assertEquals(i, c[i]);
        }

        Assertions.assertFalse(iter.hasNext());
    }

    /**
     * Verifies that the iterator generates a lexicographically
     * increasing sequence of b(n,k) arrays, each having length k
     * and each array itself increasing.
     *
     * @param n Size of the set from which subsets are selected.
     * @param k Size of the subsets to be enumerated.
     */
    private static void checkLexicographicIterator(int n,
                                                   int k) {
        int[] lastIterate = null;

        long numIterates = 0;
        final Comparator<int[]> comp = Combinations.of(n, k).comparator();
        for (int[] iterate : Combinations.of(n, k)) {
            Assertions.assertEquals(k, iterate.length);

            // Check that the sequence of iterates is ordered.
            if (lastIterate != null) {
                Assertions.assertEquals(1, comp.compare(iterate, lastIterate));
            }

            // Check that each iterate is ordered.
            for (int i = 1; i < iterate.length; i++) {
                Assertions.assertTrue(iterate[i] > iterate[i - 1]);
            }

            lastIterate = iterate;
            ++numIterates;
        }

        // Check the number of iterates.
        Assertions.assertEquals(BinomialCoefficient.value(n, k), numIterates);
    }

    @Test
    public void testCombinationsPrecondition1() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> Combinations.of(4, 5));
    }
    @Test
    public void testCombinationsPrecondition2() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> Combinations.of(-1, -2));
    }
}
