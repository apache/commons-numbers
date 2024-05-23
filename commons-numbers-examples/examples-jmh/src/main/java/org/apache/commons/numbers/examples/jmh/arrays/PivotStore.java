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
 * Storage for pivot indices used for partitioning an array into multiple regions.
 *
 * <p>A pivot is an index position that contains a value equal to the value in a fully
 * sorted array.
 *
 * <p>For a pivot {@code p}:
 *
 * <pre>{@code
 * i < p < j
 * data[i] <= data[p] <= data[j]
 * }</pre>
 *
 * <p>Implementations may assume indices are positive. Implementations are not required to
 * store all indices, and may discard previously stored indices during operation. Behaviour
 * should be documented.
 *
 * <p>This interface is used by methods that create pivots. Methods that use pivots should
 * use the {@link PivotCache} interface.
 *
 * @since 1.2
 */
interface PivotStore {
    /**
     * Add the pivot index to the store.
     *
     * @param index Index.
     */
    void add(int index);

    /**
     * Add a range of pivot indices to the store.
     *
     * <p>If {@code fromIndex == toIndex} this is equivalent to {@link #add(int)}.
     *
     * <p><em>If {@code fromIndex > toIndex} the behavior is not defined.</em></p>
     *
     * @param fromIndex Start index of the range (inclusive).
     * @param toIndex End index of the range (inclusive).
     */
    void add(int fromIndex, int toIndex);
}
