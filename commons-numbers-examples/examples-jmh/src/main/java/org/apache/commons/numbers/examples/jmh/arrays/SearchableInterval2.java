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
 * A searchable interval that contains indices used for partitioning an array into multiple regions.
 *
 * <p>The interval provides pointers to indices in the interval. These pointers are used
 * to assist in searching the interval and can be used to access the index value.
 *
 * <p>The interval provides the following functionality:
 *
 * <ul>
 * <li>Return the supported bounds of the search pointers {@code [start <= end]}.</li>
 * <li>Return a pointer {@code j} to the previous index contained in the interval from a search point {@code i}.</li>
 * <li>Return a pointer {@code j} to the next index contained in the interval from a search point {@code i}.</li>
 * <li>Split the interval around two indices given search points {@code i1} and {@code i2}.</li>
 * </ul>
 *
 * <p>Note that the interval provides the supported bounds. If a search begins outside
 * the supported bounds the result is undefined.
 *
 * <p>Implementations may assume indices are positive.
 *
 * <p>This differs from {@link SearchableInterval} by providing pointers into the interval to
 * assist the search.
 *
 * @see SearchableInterval
 * @since 1.2
 */
interface SearchableInterval2 {
    /**
     * Start pointer of the interval.
     *
     * @return the start pointer
     */
    int start();

    /**
     * End pointer of the interval.
     *
     * @return the end pointer
     */
    int end();

    /**
     * Return the index value {@code k} for the pointer.
     *
     * <p>If {@code i < start} or {@code i > end} the result is undefined.
     *
     * @param i Pointer.
     * @return the index value of {@code i}
     */
    int index(int i);

    /**
     * Returns a pointer to the nearest index that occurs on or before the specified starting index.
     *
     * <p>Assumes {@code index(start) <= k < index(i)}.
     *
     * @param i Pointer.
     * @param k Index to start checking from (inclusive).
     * @return the previous pointer
     */
    int previous(int i, int k);

    /**
     * Returns a pointer to the nearest index that occurs on or after the specified starting
     * index.
     *
     * <p>Assumes {@code index(i) < k <= index(end)}.
     *
     * @param i Pointer.
     * @param k Index to start checking from (inclusive).
     * @return the next index
     */
    int next(int i, int k);

    /**
     * Split the interval using two splitting indices. Returns a pointer to the the
     * nearest index that occurs before the specified split index {@code ka}, and a
     * pointer to the nearest index that occurs after the specified split index
     * {@code kb}.
     *
     * <p>Requires {@code index(lo) < ka <= kb < index(hi)}, i.e. there exists a
     * valid interval above and below the split indices.
     *
     * <pre>{@code
     * lo-----------ka-kb----------hi
     *      lower <--|
     *                 |--> upper
     *
     * index(lower) < ka
     * index(upper) > kb
     * }</pre>
     *
     * <p>The default implementation uses:
     *
     * <pre>{@code
     * upper = next(hi, kb + 1);
     * lower = previous(lo, ka - 1);
     * }</pre>
     *
     * <p>Implementations may override this method if both pointers can be obtained
     * together.
     *
     * <p>If {@code lo < start} or {@code hi > end} the result is undefined.
     *
     * @param lo Lower pointer.
     * @param hi Upper pointer.
     * @param ka Split index.
     * @param kb Split index.
     * @param upper Upper pointer.
     * @return the lower pointer
     */
    default int split(int lo, int hi, int ka, int kb, int[] upper) {
        upper[0] = next(hi, kb + 1);
        return previous(lo, ka - 1);
    }
}
