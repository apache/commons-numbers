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
 * A {@link SearchableInterval} backed by an array of ordered keys. The interval is searched using
 * a binary search.
 *
 * @since 1.2
 */
final class BinarySearchKeyInterval implements SearchableInterval, SearchableInterval2 {
    /** The ordered keys for descending search. */
    private final int[] keys;
    /** The original number of keys - 1. This is more convenient to store for the use cases. */
    private final int nm1;

    /**
     * Create an instance with the provided keys.
     *
     * @param indices Indices.
     * @param n Number of indices.
     */
    BinarySearchKeyInterval(int[] indices, int n) {
        nm1 = n - 1;
        keys = indices;
    }

    /**
     * Initialise an instance with the {@code indices}. The indices are used in place.
     *
     * @param indices Indices.
     * @param n Number of indices.
     * @return the interval
     * @throws IllegalArgumentException if the indices are not unique and ordered;
     * or {@code n <= 0}
     */
    static BinarySearchKeyInterval of(int[] indices, int n) {
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
        return new BinarySearchKeyInterval(indices, n);
    }

    @Override
    public int left() {
        return keys[0];
    }

    @Override
    public int right() {
        return keys[nm1];
    }

    @Override
    public int previousIndex(int k) {
        // Assume left <= k <= right thus no index checks required.
        // IndexOutOfBoundsException indicates incorrect usage by the caller.
        return keys[Partition.searchLessOrEqual(keys, 0, nm1, k)];
    }

    @Override
    public int nextIndex(int k) {
        // Assume left <= k <= right thus no index checks required.
        // IndexOutOfBoundsException indicates incorrect usage by the caller.
        return keys[Partition.searchGreaterOrEqual(keys, 0, nm1, k)];
    }

    @Override
    public int split(int ka, int kb, int[] upper) {
        int i = Partition.searchGreaterOrEqual(keys, 0, nm1, kb + 1);
        upper[0] = keys[i];
        // Find the lower using a scan since a typical use case has ka == kb
        // and a scan is faster than a second binary search.
        do {
            --i;
        } while (keys[i] >= ka);
        return keys[i];
    }

    @Override
    public int start() {
        return 0;
    }

    @Override
    public int end() {
        return nm1;
    }

    @Override
    public int index(int i) {
        return keys[i];
    }

    // Use case for previous/next is when left/right is within
    // a partition pivot [p0, p1]. Most likely case is p0 == p1
    // and a scan is faster.

    @Override
    public int previous(int i, int k) {
        // index(start) <= k < index(i)
        int j = i;
        do {
            --j;
        } while (keys[j] > k);
        return j;
    }

    @Override
    public int next(int i, int k) {
        // index(i) < k <= index(end)
        int j = i;
        do {
            ++j;
        } while (keys[j] < k);
        return j;
    }

    @Override
    public int split(int lo, int hi, int ka, int kb, int[] upper) {
        // index(lo) < ka <= kb < index(hi)

        // We could test if ka/kb is above or below the
        // median (keys[lo] + keys[hi]) >>> 1 to pick the side to search

        int j = Partition.searchGreaterOrEqual(keys, lo, hi, kb + 1);
        upper[0] = j;
        // Find the lower using a scan since a typical use case has ka == kb
        // and a scan is faster than a second binary search.
        do {
            --j;
        } while (keys[j] >= ka);
        return j;
    }
}
