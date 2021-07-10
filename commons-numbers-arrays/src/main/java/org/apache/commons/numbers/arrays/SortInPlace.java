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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Sort an array and perform the same reordering of entries on other arrays.
 * For example, if
 * <ul>
 *  <li>{@code x = [3, 1, 2]}</li>
 *  <li>{@code y = [1, 2, 3]}</li>
 *  <li>{@code z = [0, 5, 7]}</li>
 * </ul>
 * then {@code Sort.ASCENDING.apply(x, y, z)} will update those arrays:
 * <ul>
 *  <li>{@code x = [1, 2, 3]}</li>
 *  <li>{@code y = [2, 3, 1]}</li>
 *  <li>{@code z = [5, 7, 0]}</li>
 * </ul>
 */
public enum SortInPlace {
    /** Sort in ascending order. */
    ASCENDING((o1, o2) -> Double.compare(o1.key(), o2.key())),
    /** Sort in descending order. */
    DESCENDING((o1, o2) -> Double.compare(o2.key(), o1.key()));

    /** Comparator. */
    private final Comparator<PairDoubleInteger> comparator;

    /**
     * @param comparator Comparator.
     */
    SortInPlace(Comparator<PairDoubleInteger> comparator) {
        this.comparator = comparator;
    }

    /**
     * Sorts in place.
     *
     * @param x Array to be sorted and used as a pattern for permutation of
     * the other arrays.
     * @param yList Set of arrays whose permutations of entries will follow
     * those performed on {@code x}.
     * @throws IllegalArgumentException if not all arrays have the same size.
     */
    public void apply(double[] x,
                      double[]... yList) {
        final int yListLen = yList.length;
        final int len = x.length;

        for (int j = 0; j < yListLen; j++) {
            final double[] y = yList[j];
            if (y.length != len) {
                throw new IllegalArgumentException("Size mismatch: " +
                                                   y.length + " != " + len);
            }
        }

        // Associate each abscissa "x[i]" with its index "i".
        final List<PairDoubleInteger> list = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            list.add(new PairDoubleInteger(x[i], i));
        }

        // Sort.
        Collections.sort(list, comparator);

        // Modify the original array so that its elements are in the prescribed order.
        // Retrieve indices of original locations.
        final int[] indices = new int[len];
        for (int i = 0; i < len; i++) {
            final PairDoubleInteger e = list.get(i);
            x[i] = e.key();
            indices[i] = e.value();
        }

        // In every associated array, move the elements to their new location.
        for (int j = 0; j < yListLen; j++) {
            // Input array will be modified in place.
            final double[] yInPlace = yList[j];
            final double[] yOrig = Arrays.copyOf(yInPlace, len);

            for (int i = 0; i < len; i++) {
                yInPlace[i] = yOrig[indices[i]];
            }
        }
    }

    /**
     * Helper data structure holding a (double, integer) pair.
     */
    private static class PairDoubleInteger {
        /** Key. */
        private final double key;
        /** Value. */
        private final int value;

        /**
         * @param key Key.
         * @param value Value.
         */
        PairDoubleInteger(double key,
                          int value) {
            this.key = key;
            this.value = value;
        }

        /** @return the key. */
        double key() {
            return key;
        }

        /** @return the value. */
        int value() {
            return value;
        }
    }
}
