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

/**
 * Converter between unidimensional storage structure and multidimensional
 * conceptual structure.
 * This utility will convert from indices in a multidimensional structure
 * to the corresponding index in a one-dimensional array. For example,
 * assuming that the ranges (in 3 dimensions) of indices are 2, 4 and 3,
 * the following correspondences, between 3-tuples indices and unidimensional
 * indices, will hold:
 * <ul>
 *  <li>(0, 0, 0) corresponds to 0</li>
 *  <li>(0, 0, 1) corresponds to 1</li>
 *  <li>(0, 0, 2) corresponds to 2</li>
 *  <li>(0, 1, 0) corresponds to 3</li>
 *  <li>...</li>
 *  <li>(1, 0, 0) corresponds to 12</li>
 *  <li>...</li>
 *  <li>(1, 3, 2) corresponds to 23</li>
 * </ul>
 */
public final class MultidimensionalCounter {
    /**
     * Number of dimensions.
     */
    private final int dimension;
    /**
     * Offset for each dimension.
     */
    private final int[] uniCounterOffset;
    /**
     * Counter sizes.
     */
    private final int[] size;
    /**
     * Total number of (one-dimensional) slots.
     */
    private final int totalSize;
    /**
     * Index of last dimension.
     */
    private final int last;

    /**
     * Creates a counter.
     *
     * @param size Counter sizes (number of slots in each dimension).
     * @throws IllegalArgumentException if one of the sizes is negative
     * or zero.
     */
    private MultidimensionalCounter(int... size) {
        dimension = size.length;
        this.size = Arrays.copyOf(size, size.length);

        uniCounterOffset = new int[dimension];

        last = dimension - 1;
        uniCounterOffset[last] = 1;

        int tS = 1;
        for (int i = last - 1; i >= 0; i--) {
            final int index = i + 1;
            checkStrictlyPositive("index size", size[index]);
            tS *= size[index];
            checkStrictlyPositive("cumulative size", tS);
            uniCounterOffset[i] = tS;
        }

        totalSize = tS * size[0];
        checkStrictlyPositive("total size", totalSize);
    }

    /**
     * Creates a counter.
     *
     * @param size Counter sizes (number of slots in each dimension).
     * @return a new instance.
     * @throws IllegalArgumentException if one of the sizes is negative
     * or zero.
     */
    public static MultidimensionalCounter of(int... size) {
        return new MultidimensionalCounter(size);
    }

    /**
     * Gets the number of dimensions of the multidimensional counter.
     *
     * @return the number of dimensions.
     */
    public int getDimension() {
        return dimension;
    }

    /**
     * Converts to a multidimensional counter.
     *
     * @param index Index in unidimensional counter.
     * @return the multidimensional counts.
     * @throws IndexOutOfBoundsException if {@code index} is not between
     * {@code 0} and the value returned by {@link #getSize()} (excluded).
     */
    public int[] toMulti(int index) {
        if (index < 0 ||
            index >= totalSize) {
            throw new IndexOutOfBoundsException(createIndexOutOfBoundsMessage(totalSize, index));
        }

        final int[] indices = new int[dimension];

        for (int i = 0; i < last; i++) {
            indices[i] = index / uniCounterOffset[i];
            // index = index % uniCounterOffset[i]
            index = index - indices[i] * uniCounterOffset[i];
        }

        indices[last] = index;

        return indices;
    }

    /**
     * Converts to a unidimensional counter.
     *
     * @param c Indices in multidimensional counter.
     * @return the index within the unidimensionl counter.
     * @throws IllegalArgumentException if the size of {@code c}
     * does not match the size of the array given in the constructor.
     * @throws IndexOutOfBoundsException if a value of {@code c} is not in
     * the range of the corresponding dimension, as defined in the
     * {@link MultidimensionalCounter#of(int...) constructor}.
     */
    public int toUni(int... c) {
        if (c.length != dimension) {
            throw new IllegalArgumentException("Wrong number of arguments: " + c.length +
                                               "(expected: " + dimension + ")");
        }
        int count = 0;
        for (int i = 0; i < dimension; i++) {
            final int index = c[i];
            if (index < 0 ||
                index >= size[i]) {
                throw new IndexOutOfBoundsException(createIndexOutOfBoundsMessage(size[i], index));
            }
            count += uniCounterOffset[i] * index;
        }
        return count;
    }

    /**
     * Gets the total number of elements.
     *
     * @return the total size of the unidimensional counter.
     */
    public int getSize() {
        return totalSize;
    }

    /**
     * Gets the number of multidimensional counter slots in each dimension.
     *
     * @return the number of slots in each dimension.
     */
    public int[] getSizes() {
        return Arrays.copyOf(size, size.length);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Arrays.toString(size);
    }

    /**
     * Check the size is strictly positive: {@code size > 0}.
     *
     * @param name the name of the size
     * @param size the size
     */
    private static void checkStrictlyPositive(String name, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Not positive " + name + ": " + size);
        }
    }

    /**
     * Creates the message for the index out of bounds exception.
     *
     * @param size the size
     * @param index the index
     * @return the message
     */
    private static String createIndexOutOfBoundsMessage(int size, int index) {
        return "Index out of bounds [0, " + (size - 1) + "]: " + index;
    }
}
