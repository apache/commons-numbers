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
package org.apache.commons.numbers.examples.jmh.arrays;

/**
 * Defines a transformer for {@code double[]} arrays.
 *
 * <p>This interface is not intended for a public API. It provides a consistent method
 * to handle partial sorting of {@code double[]} data.
 *
 * <p>The transformer allows pre-processing data before applying a sort algorithm.
 * This is required to handle {@code NaN} and signed-zeros {@code -0.0}.
 *
 * <p>Note: The {@code <} relation does not provide a total order on all double
 * values: {@code -0.0 == 0.0} is {@code true} and a {@code NaN}
 * value compares neither less than, greater than, nor equal to any value,
 * even itself.
 *
 * <p>The {@link java.util.Arrays#sort(double[])} method respects the order imposed by
 * {@link Double#compare(double, double)}: {@code -0.0} is treated as less than value
 * {@code 0.0} and {@code Double.NaN} is considered greater than any
 * other value and all {@code Double.NaN} values are considered equal.
 *
 * <p>This interface allows implementations to respect the behaviour
 * {@link Double#compare(double, double)}, or implement different behaviour.
 *
 * @see java.util.Arrays#sort(double[])
 * @since 1.2
 */
interface DoubleDataTransformer {
    /**
     * Pre-process the data for partitioning.
     *
     * <p>This method will scan all the data and apply
     * processing to {@code NaN} and signed-zeros {@code -0.0}.
     *
     * <p>A method matching {@link java.util.Arrays#sort(double[])} would move
     * all {@code NaN} to the end of the array and order zeros. However ordering
     * zeros is not useful if the data is to be fully or partially reordered
     * by the caller. Possible solutions are to count signed zeros, or ignore them since
     * they will not interfere with comparison operators {@code <, ==, >}.
     *
     * <p>The length of the data that must be processed by partitioning can be
     * accessed using {@link #length()}. For example if {@code NaN} values are moved
     * to the end of the data they are already partitioned. A partition algorithm
     * can then avoid processing {@code NaN} during partitioning.
     *
     * @param data Data.
     * @return pre-processed data (may be a copy)
     */
    double[] preProcess(double[] data);

    /**
     * Get the size of the data.
     *
     * <p>Note: Although the pre-processed data array may be longer than this length some
     * values may have been excluded from the data (e.g. removal of NaNs). This is the
     * effective size of the data.
     *
     * @return the size
     */
    int size();

    /**
     * Get the length of the pre-processed data that must be partitioned.
     *
     * <p>Note: Although the pre-processed data array may be longer than this length it is
     * only required to partition indices below this length. For example the end of the
     * array may contain values to ignore from partitioning such as {@code NaN}.
     *
     * @return the length
     */
    int length();

    /**
     * Post-process the data after partitioning. This method can restore values that
     * may have been removed from the pre-processed data, for example signed zeros
     * or revert any special {@code NaN} value processing.
     *
     * <p>If no partition indices are available use {@code null} and {@code n = 0}.
     *
     * @param data Data.
     * @param k Partition indices.
     * @param n Count of partition indices.
     */
    void postProcess(double[] data, int[] k, int n);

    /**
     * Post-process the data after sorting. This method can restore values that
     * may have been removed from the pre-processed data, for example signed zeros
     * or revert any special {@code NaN} value processing.
     *
     * <p>Warning: Assumes data is fully sorted in {@code [0, length)} (see {@link #length()}).
     *
     * @param data Data.
     */
    void postProcess(double[] data);
}
