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
 * An interval that provides analysis of indices within the range.
 *
 * @since 1.2
 */
interface IntervalAnalysis {
    /**
     * Test if the interval is saturated at the specified {@code separation}. The
     * separation distance is provided as a power of 2.
     *
     * <pre>{@code distance = 1 << separation}</pre>
     *
     * <p>A saturated interval will have all neighbouring indices separated
     * <em>approximately</em> within the maximum separation distance.
     *
     * <p>Implementations may:
     * <ol>
     * <li>Use approximations for performance, for example
     * compressing indices into blocks of the defined separation.
     * <pre>{@code c = (i - left) >> separation}</pre>
     * <li>Support only a range of the possible
     * {@code separation} values in {@code [0, 30]}. Unsupported {@code separation}
     * values should return {@code false}.
     * </ol>
     *
     * @param separation Log2 of the maximum separation between indices.
     * @return true if saturated
     */
    boolean saturated(int separation);
}
