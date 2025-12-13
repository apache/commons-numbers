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
 * An interval that contains indices used for partitioning an array into multiple regions.
 *
 * <p>The interval provides the following functionality:
 *
 * <ul>
 * <li>Return the supported bounds of the interval {@code [left <= right]}.</li>
 * <li>Update the left or right bound of the interval using an index {@code k} inside the interval.</li>
 * <li>Split the interval around two indices {@code k1} and {@code k2}.</li>
 * </ul>
 *
 * <p>Note that the interval provides the supported bounds. If an index {@code k} is
 * outside the supported bounds the result is undefined.
 *
 * <p>Implementations may assume indices are positive.
 *
 * @since 1.2
 */
interface UpdatingInterval {
    /**
     * The start (inclusive) of the interval.
     *
     * @return start of the interval
     */
    int left();

    /**
     * The end (inclusive) of the interval.
     *
     * @return end of the interval
     */
    int right();

    /**
     * Update the interval so {@code k <= left}.
     *
     * <p>Note: Requires {@code left < k <= right}, i.e. there exists a valid interval
     * above the index.
     *
     * <pre>{@code
     * l-----------k----------r
     *             |--> l
     * }</pre>
     *
     * @param k Index to start checking from (inclusive).
     * @return the new left
     */
    int updateLeft(int k);

    /**
     * Update the interval so {@code right <= k}.
     *
     * <p>Note: Requires {@code left <= k < right}, i.e. there exists a valid interval
     * below the index.
     *
     * <pre>{@code
     * l-----------k----------r
     *        r <--|
     * }</pre>
     *
     * @param k Index to start checking from (inclusive).
     * @return the new right
     */
    int updateRight(int k);

    /**
     * Split the interval using two splitting indices. Returns the left interval that occurs
     * before the specified split index {@code ka}, and updates the current interval left bound
     * to after the specified split index {@code kb}.
     *
     * <p>Note: Requires {@code left < ka <= kb < right}, i.e. there exists a valid interval
     * above and below the split indices.
     *
     * <pre>{@code
     * l-----------ka-kb----------r
     *      r1 <--|     |--> l1
     *
     * r1 < ka
     * l1 > kb
     * }</pre>
     *
     * <p>If {@code ka <= left} or {@code kb >= right} the result is undefined.
     *
     * @param ka Split index.
     * @param kb Split index.
     * @return the left interval
     */
    UpdatingInterval splitLeft(int ka, int kb);

    /**
     * Split the interval using two splitting indices. Returns the right interval that occurs
     * after the specified split index {@code kb}, and updates the current interval right bound
     * to before the specified split index {@code ka}.
     *
     * <p>Note: Requires {@code left < ka <= kb < right}, i.e. there exists a valid interval
     * above and below the split indices.
     *
     * <pre>{@code
     * l-----------ka-kb----------r
     *      r1 <--|     |--> l1
     *
     * r1 < ka
     * l1 > kb
     * }</pre>
     *
     * <p>If {@code ka <= left} or {@code kb >= right} the result is undefined.
     *
     * @param ka Split index.
     * @param kb Split index.
     * @return the right interval
     */
    UpdatingInterval splitRight(int ka, int kb);
}
