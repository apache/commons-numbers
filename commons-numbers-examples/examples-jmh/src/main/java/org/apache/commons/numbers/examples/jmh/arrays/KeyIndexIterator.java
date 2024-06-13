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
 * An {@link IndexIterator} backed by an array of ordered keys.
 *
 * @since 1.2
 */
final class KeyIndexIterator implements IndexIterator {
    /** The ordered keys. */
    private final int[] keys;
    /** The original number of keys minus 1. */
    private final int nm1;

    /** Iterator left. */
    private int left;
    /** Iterator right position. Never advanced beyond {@code n - 1}. */
    private int hi = -1;

    /**
     * Create an instance with the provided keys.
     *
     * @param indices Indices.
     * @param n Number of indices.
     */
    KeyIndexIterator(int[] indices, int n) {
        keys = indices;
        this.nm1 = n - 1;
        next();
    }

    /**
     * Initialise an instance with {@code n} initial {@code indices}. The indices are used in place.
     *
     * @param indices Indices.
     * @param n Number of indices.
     * @return the iterator
     * @throws IllegalArgumentException if the indices are not unique and ordered; or not
     * in the range {@code [0, 2^31-1)}; or {@code n <= 0}
     */
    static KeyIndexIterator of(int[] indices, int n) {
        // Check the indices are uniquely ordered
        if (n <= 0) {
            throw new IllegalArgumentException("No indices to define the range");
        }
        int p = indices[0];
        for (int i = 0; ++i < n;) {
            final int c = indices[i];
            if (c <= p) {
                throw new IllegalArgumentException("Indices are not unique and ordered");
            }
            p = c;
        }
        if (indices[0] < 0) {
            throw new IllegalArgumentException("Unsupported min value: " + indices[0]);
        }
        if (indices[n - 1] == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Unsupported max value: " + Integer.MAX_VALUE);
        }
        return new KeyIndexIterator(indices, n);
    }

    @Override
    public int left() {
        return left;
    }

    @Override
    public int right() {
        return keys[hi];
    }

    @Override
    public int end() {
        return keys[nm1];
    }

    @Override
    public boolean next() {
        int i = hi;
        if (i < nm1) {
            // Blocks [left, right] use a maximum separation of 2 between indices
            int k = keys[++i];
            left = k;
            while (++i <= nm1 && k + 2 >= keys[i]) {
                k = keys[i];
            }
            hi = i - 1;
            return true;
        }
        return false;
    }

    @Override
    public boolean positionAfter(int index) {
        int i = hi;
        int r = keys[i];
        if (r <= index && i < nm1) {
            // fast-forward right
            while (++i <= nm1) {
                r = keys[i];
                if (r > index) {
                    // Advance to match the output of next()
                    while (++i <= nm1 && r + 2 >= keys[i]) {
                        r = keys[i];
                    }
                    break;
                }
            }
            hi = --i;
            // calculate left
            // Blocks [left, right] use a maximum separation of 2 between indices
            int k = r;
            while (--i >= 0 && keys[i] + 2 >= k) {
                k = keys[i];
                // indices <= index are not required
                if (k <= index) {
                    left = index + 1;
                    return r > index;
                }
            }
            left = k;
        }
        return r > index;
    }

    @Override
    public boolean nextAfter(int index) {
        if (hi < nm1) {
            // test if the next left is after the index
            return keys[hi + 1] > index;
        }
        // no more indices
        return true;
    }
}
