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
 * An {@link UpdatingInterval} and {@link SplittingInterval} backed by an array of ordered keys.
 *
 * @since 1.2
 */
final class KeyUpdatingInterval implements UpdatingInterval, SplittingInterval {
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
     * Indices must be sorted.
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

    /**
     * Initialise an instance with the {@code indices}. The indices are used in place.
     *
     * @param indices Indices.
     * @param n Number of indices.
     * @return the interval
     * @throws IllegalArgumentException if the indices are not unique and ordered;
     * or {@code n <= 0}
     */
    static KeyUpdatingInterval of(int[] indices, int n) {
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
        return new KeyUpdatingInterval(indices, n);
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
            i = Partition.searchLessOrEqual(keys, l, r, kb);
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

    @Override
    public UpdatingInterval splitRight(int ka, int kb) {
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
            i = Partition.searchLessOrEqual(keys, l, r, kb);
        }
        final int upperLeft = i + 1;

        // Find the new right bound for the lower interval using a scan since a
        // typical use case has ka == kb and this is faster than a second binary search.
        while (keys[i] >= ka) {
            --i;
        }
        final int upperRight = r;
        r = i;
        // return right
        return new KeyUpdatingInterval(keys, upperLeft, upperRight);
    }

    /**
     * Return the current number of indices in the interval.
     * This is undefined when {@link #empty()}.
     *
     * @return the size
     */
    int size() {
        return r - l + 1;
    }

    @Override
    public boolean empty() {
        // Empty when the interval is invalid. Signalled by a negative right index.
        return r < 0;
    }

    @Override
    public SplittingInterval split(int ka, int kb) {
        if (ka <= left()) {
            // No left interval
            if (kb >= right()) {
                // No right interval
                invalidate();
            } else if (kb >= left()) {
                // Update the left bound.
                // Search using a scan on the assumption that kb is close to the end
                // given that ka is less then the end.
                int i = l;
                do {
                    ++i;
                } while (keys[i] < kb);
                l = i;
            }
            return null;
        }
        if (kb >= right()) {
            // No right interval.
            // Find new right bound for the left-side.
            // Search using a scan on the assumption that ka is close to the end
            // given that kb is greater then the end.
            int i = r;
            if (ka <= keys[i]) {
                do {
                    --i;
                } while (keys[i] > ka);
            }
            invalidate();
            return new KeyUpdatingInterval(keys, l, i);
        }
        // Split
        return (SplittingInterval) splitLeft(ka, kb);
    }

    /**
     * Invalidate the interval and mark as empty.
     */
    private void invalidate() {
        r = -1;
    }
}
