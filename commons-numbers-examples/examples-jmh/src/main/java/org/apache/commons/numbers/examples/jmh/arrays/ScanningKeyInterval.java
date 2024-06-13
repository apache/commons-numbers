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
 * An {@link SearchableInterval} backed by an array of ordered keys. The interval is searched using
 * a linear scan of the data. The scan start point is chosen from reference points within the data.
 *
 * <p>The scan is fast when the number of keys is small.
 *
 * @since 1.2
 */
final class ScanningKeyInterval implements SearchableInterval, SearchableInterval2 {
    // Note:
    // Using 4 markers into the data allows this class to return the same
    // performance as using a binary search within the data when n < 1600.
    // Benchmarked by searching once for next and previous from median points between k.

    /** The ordered keys. */
    private final int[] keys;
    /** The original number of keys. */
    private final int n;
    /** Index into the keys (used for fast-forward). */
    private final int i1;
    /** Index into the keys (used for fast-forward). */
    private final int i2;
    /** Index into the keys (used for fast-forward). */
    private final int i3;

    /**
     * Create an instance with the provided keys.
     *
     * @param indices Indices.
     * @param n Number of indices.
     */
    ScanningKeyInterval(int[] indices, int n) {
        keys = indices;
        this.n = n;
        // Divide into quarters for fast-forward
        i1 = n >>> 2;
        i2 = n >>> 1;
        i3 = i1 + i2;
    }

    /**
     * Initialise an instance with {@code n} initial {@code indices}. The indices are used in place.
     *
     * @param indices Indices.
     * @param n Number of indices.
     * @return the interval
     * @throws IllegalArgumentException if the indices are not unique and ordered; or not
     * in the range {@code [0, 2^31-1)}; or {@code n <= 0}
     */
    static ScanningKeyInterval of(int[] indices, int n) {
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
        return new ScanningKeyInterval(indices, n);
    }

    @Override
    public int left() {
        return keys[0];
    }

    @Override
    public int right() {
        return keys[n - 1];
    }

    @Override
    public int previousIndex(int k) {
        return keys[previous(k)];
    }

    @Override
    public int nextIndex(int k) {
        return keys[next(k)];
    }

    @Override
    public int split(int ka, int kb, int[] upper) {
        int i = next(kb + 1);
        upper[0] = keys[i];
        // Find the lower
        do {
            --i;
        } while (keys[i] >= ka);
        return keys[i];
    }

    /**
     * Find the key index {@code i} of {@code keys[i] <= k}.
     *
     * @param k Target key.
     * @return the key index
     */
    private int previous(int k) {
        // Scan the sorted keys from the end.
        // Assume left <= k <= right thus no index checks required.
        // IndexOutOfBoundsException indicates incorrect usage by the caller.

        // Attempt fast-forward
        int i;
        if (keys[i2] > k) {
            i = keys[i1] > k ? i1 : i2;
        } else {
            i = keys[i3] > k ? i3 : n;
        }
        do {
            --i;
        } while (keys[i] > k);
        return i;
    }

    /**
     * Find the key index {@code i} of {@code keys[i] >= k}.
     *
     * @param k Target key.
     * @return the key index
     */
    private int next(int k) {
        // Scan the sorted keys from the start.
        // Assume left <= k <= right thus no index checks required.
        // IndexOutOfBoundsException indicates incorrect usage by the caller.

        // Attempt fast-forward
        int i;
        if (keys[i2] < k) {
            i = keys[i3] < k ? i3 : i2;
        } else {
            i = keys[i1] < k ? i1 : -1;
        }
        do {
            ++i;
        } while (keys[i] < k);
        return i;
    }

    @Override
    public int start() {
        return 0;
    }

    @Override
    public int end() {
        return n - 1;
    }

    @Override
    public int index(int i) {
        return keys[i];
    }

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

        int j = hi;
        do {
            --j;
        } while (keys[j] > kb);
        upper[0] = j + 1;
        // Find the lower
        while (keys[j] >= ka) {
            --j;
        }
        return j;
    }
}
