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
 * <p>The interval provides the following functionality:
 *
 * <ul>
 * <li>Return the supported bounds of the search {@code [left <= right]}.
 * <li>Return the previous index contained in the interval from a search point {@code k}.
 * <li>Return the next index contained in the interval from a search point {@code k}.
 * </ul>
 *
 * <p>Note that the interval provides the supported bounds. If a search begins outside
 * the supported bounds the result is undefined.
 *
 * <p>Implementations may assume indices are positive.
 *
 * @see SearchableInterval2
 * @since 1.2
 */
interface SearchableInterval {
    /**
     * The start (inclusive) of the range of indices supported.
     *
     * @return start of the supported range
     */
    int left();

    /**
     * The end (inclusive) of the range of indices supported.
     *
     * @return end of the supported range
     */
    int right();

    /**
     * Returns the nearest index that occurs on or before the specified starting
     * index.
     *
     * <p>If {@code k < left} or {@code k > right} the result is undefined.
     *
     * @param k Index to start checking from (inclusive).
     * @return the previous index
     */
    int previousIndex(int k);

    /**
     * Returns the nearest index that occurs on or after the specified starting
     * index.
     *
     * <p>If {@code k < left} or {@code k > right} the result is undefined.
     *
     * @param k Index to start checking from (inclusive).
     * @return the next index
     */
    int nextIndex(int k);

    /**
     * Split the interval using two splitting indices. Returns the nearest index that occurs
     * before the specified split index {@code ka}, and the nearest index that occurs after the
     * specified split index {@code kb}.
     *
     * <p>Note: Requires {@code left < ka <= kb < right}, i.e. there exists a valid interval
     * above and below the split indices.
     *
     * <pre>{@code
     * l-----------ka-kb----------r
     *   lower <--|
     *                  |--> upper
     *
     * lower < ka
     * upper > kb
     * }</pre>
     *
     * <p>The default implementation uses:
     *
     * <pre>{@code
     * upper = nextIndex(kb + 1);
     * lower = previousIndex(ka - 1);
     * }</pre>
     *
     * <p>Implementations may override this method if both indices can be obtained together.
     *
     * <p>If {@code ka <= left} or {@code kb >= right} the result is undefined.
     *
     * @param ka Split index.
     * @param kb Split index.
     * @param upper Upper index.
     * @return the lower index
     */
    default int split(int ka, int kb, int[] upper) {
        upper[0] = nextIndex(kb + 1);
        return previousIndex(ka - 1);
    }
}
