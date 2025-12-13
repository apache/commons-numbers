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

package org.apache.commons.numbers.arrays;

/**
 * An {@link UpdatingInterval} backed by an array of ordered keys.
 *
 * @since 1.2
 */
final class KeyUpdatingInterval implements UpdatingInterval {
    /** Size to use a scan of the keys when splitting instead of binary search.
     * Note binary search has an overhead on small size due to the random left/right
     * branching per iteration. It is much faster on very large sizes. */
    private static final int SCAN_SIZE = 256;

    /** The ordered keys. */
    private final int[] keys;
    /** Index of the left key. */
    private int l;
    /** Index of the right key. */
    private int r;

    /**
     * Create an instance with the provided {@code indices}.
     *
     * <p><strong>Warning:</strong> Indices must be sorted and distinct.
     *
     * @param indices Indices.
     * @param n Number of indices.
     */
    KeyUpdatingInterval(int[] indices, int n) {
        this(indices, 0, n - 1);
    }

    /**
     * @param indices Indices.
     * @param l Index of left key.
     * @param r Index of right key.
     */
    private KeyUpdatingInterval(int[] indices, int l, int r) {
        keys = indices;
        this.l = l;
        this.r = r;
    }

    @Override
    public int left() {
        return keys[l];
    }

    @Override
    public int right() {
        return keys[r];
    }

    @Override
    public int updateLeft(int k) {
        // Assume left < k <= right (i.e. we must move left at least 1)
        // Search using a scan on the assumption that k is close to the end
        int i = l;
        do {
            ++i;
        } while (keys[i] < k);
        l = i;
        return keys[i];
    }

    @Override
    public int updateRight(int k) {
        // Assume left <= k < right (i.e. we must move right at least 1)
        // Search using a scan on the assumption that k is close to the end
        int i = r;
        do {
            --i;
        } while (keys[i] > k);
        r = i;
        return keys[i];
    }

    @Override
    public UpdatingInterval splitLeft(int ka, int kb) {
        // left < ka <= kb < right

        // Find the new left bound for the upper interval.
        // Switch to a linear scan if length is small.
        int i;
        if (r - l < SCAN_SIZE) {
            i = r;
            do {
                --i;
            } while (keys[i] > kb);
        } else {
            // Binary search
            i = searchLessOrEqual(keys, l, r, kb);
        }
        final int lowerLeft = l;
        l = i + 1;

        // Find the new right bound for the lower interval using a scan since a
        // typical use case has ka == kb and this is faster than a second binary search.
        while (keys[i] >= ka) {
            --i;
        }
        // return left
        return new KeyUpdatingInterval(keys, lowerLeft, i);
    }

    /**
     * Return the current number of indices in the interval.
     *
     * @return the size
     */
    int size() {
        return r - l + 1;
    }

    /**
     * Search the data for the largest index {@code i} where {@code a[i]} is
     * less-than-or-equal to the {@code key}; else return {@code left - 1}.
     * <pre>
     * a[i] <= k    :   left <= i <= right, or (left - 1)
     * </pre>
     *
     * <p>The data is assumed to be in ascending order, otherwise the behaviour is undefined.
     * If the range contains multiple elements with the {@code key} value, the result index
     * may be any that match.
     *
     * <p>This is similar to using {@link java.util.Arrays#binarySearch(int[], int, int, int)
     * Arrays.binarySearch}. The method differs in:
     * <ul>
     * <li>use of an inclusive upper bound;</li>
     * <li>returning the closest index with a value below {@code key} if no match was not found;</li>
     * <li>performing no range checks: it is assumed {@code left <= right} and they are valid
     * indices into the array.</li>
     * </ul>
     *
     * <p>An equivalent use of binary search is:
     * <pre>{@code
     * int i = Arrays.binarySearch(a, left, right + 1, k);
     * if (i < 0) {
     *     i = ~i - 1;
     * }
     * }</pre>
     *
     * <p>This specialisation avoids the caller checking the binary search result for the use
     * case when the presence or absence of a key is not important; only that the returned
     * index for an absence of a key is the largest index. When used on unique keys this
     * method can be used to update an upper index so all keys are known to be below a key:
     *
     * <pre>{@code
     * int[] keys = ...
     * // [i0, i1] contains all keys
     * int i0 = 0;
     * int i1 = keys.length - 1;
     * // Update: [i0, i1] contains all keys <= k
     * i1 = searchLessOrEqual(keys, i0, i1, k);
     * }</pre>
     *
     * @param a Data.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param k Key.
     * @return largest index {@code i} such that {@code a[i] <= k}, or {@code left - 1} if no
     * such index exists
     */
    static int searchLessOrEqual(int[] a, int left, int right, int k) {
        int l = left;
        int r = right;
        while (l <= r) {
            // Middle value
            final int m = (l + r) >>> 1;
            final int v = a[m];
            // Test:
            // l------m------r
            //        v  k      update left
            //     k  v         update right
            if (v < k) {
                l = m + 1;
            } else if (v > k) {
                r = m - 1;
            } else {
                // Equal
                return m;
            }
        }
        // Return largest known value below:
        // r is always moved downward when a middle index value is too high
        return r;
    }
}
