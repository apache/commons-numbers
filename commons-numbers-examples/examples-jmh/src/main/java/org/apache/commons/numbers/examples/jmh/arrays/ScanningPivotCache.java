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
 * A cache of pivot indices used for partitioning an array into multiple regions.
 *
 * <p>This extends the {@link PivotCache} interface to add support to traverse
 * a region between pivot and non-pivot indices. It is intended to be used to
 * allow unsorted gaps between pivots to be targeted using a full sort when
 * the partition function is configured to partition several regions [ka, kb].
 *
 * <p>In the following example the partition algorithm must fully sort [k1, k2]
 * and [k3, k4]. The first iteration detects pivots downstream from [k1, k2].
 * The second iteration can fill in the gaps when processing [k3, k4]:
 *
 * <pre>
 * Partition:
 * 0------k1---k2-------k3--------------k4-N
 *
 * Iteration 1:
 * 0------ppppppp----p------p--p-------p---N
 *
 * Iteration 2:
 *                   -------               Partition (p, k3, p)
 *                           ss sssssss    Sort these regions
 *                                      -- Partition (p, k4, N)
 * </pre>
 *
 * @since 1.2
 */
interface ScanningPivotCache extends PivotCache {
    /**
     * Move the start (inclusive) of the range of indices supported.
     *
     * <p>Implementations may discard previously stored indices during this operation, for
     * example all indices below {@code newLeft}.
     *
     * <p>This method can be used when partitioning keys {@code k1, k2, ...} in ascending
     * order to indicate the next {@code k} that will be processed. The cache can optimise
     * pivot storage to help partition downstream keys.
     *
     * <p>Note: If {@code newLeft < left} then the updated range is outside the current
     * support. Implementations may choose to: move {@code left} so the new support is
     * {@code [newLeft, right]}; return {@code false} to indicate the support was not changed;
     * or to throw an exception (which should be documented).
     *
     * <p>Note: If {@code newLeft > right} then the updated range is outside the current
     * support. Implementations may choose to: move {@code right} so the new support is
     * {@code [newLeft, newleft]}; return {@code false} to indicate the support was not changed;
     * or to throw an exception (which should be documented).
     *
     * @param newLeft Start of the supported range.
     * @return true if the support was successfully modified
     */
    boolean moveLeft(int newLeft);

    /**
     * Returns the nearest non-pivot index that occurs on or after the specified starting
     * index <em>within the supported range</em>. If no such
     * indices exists then {@code right + n} is returned, where {@code n} is strictly positive.
     *
     * <p>If the starting index is less than the supported range {@code left}
     * the result is undefined.
     *
     * <p>This method is intended to allow traversing the unsorted ranges between sorted
     * pivot regions within the range {@code [left, right]}.
     *
     * @param k Index to start checking from (inclusive).
     * @return the index of the next non-pivot, or {@code right + n} if there is no index
     */
    int nextNonPivot(int k);

    /**
     * Returns the nearest non-pivot index that occurs on or before the specified starting
     * index <em>within the supported range</em>. If no such
     * indices exists then {@code left - n} is returned, where {@code n} is strictly positive.
     *
     * <p>If the starting index is greater than the supported range {@code right}
     * the result is undefined.
     *
     * <p>This method is intended to allow traversing the unsorted ranges between sorted
     * pivot regions within the range {@code [left, right]}.
     *
     * @param k Index to start checking from (inclusive).
     * @return the index of the next non-pivot, or {@code left - n} if there is no index
     */
    int previousNonPivot(int k);
}
