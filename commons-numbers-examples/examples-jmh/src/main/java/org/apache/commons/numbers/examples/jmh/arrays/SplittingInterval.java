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
 * <li>Split the interval around two indices {@code k1} and {@code k2}.</li>
 * </ul>
 *
 * <p>Note that the interval provides the supported bounds. If a split invalidates an interval
 * the bounds are undefined and the interval is marked as {@link #empty()}.
 *
 * <p>Implementations may assume indices are positive.
 *
 * @since 1.2
 */
interface SplittingInterval {
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
     * Signal this interval is empty. The left and right bounds are undefined. This results
     * from a split where there is no right side.
     *
     * @return {@code true} if empty
     */
    boolean empty();

    /**
     * Split the interval using two splitting indices. Returns the left interval that occurs
     * before the specified split index {@code ka}, and updates the current interval left bound
     * to after the specified split index {@code kb}.
     *
     * <pre>{@code
     * l-----------ka-kb----------r
     *      ra <--|     |--> lb
     *
     * ra < ka
     * lb > kb
     * }</pre>
     *
     * <p>If {@code ka <= left} the returned left interval is {@code null}.
     *
     * <p>If {@code kb >= right} the current interval is invalidated and marked as empty.
     *
     * @param ka Split index.
     * @param kb Split index.
     * @return the left interval
     * @see #empty()
     */
    SplittingInterval split(int ka, int kb);
}
