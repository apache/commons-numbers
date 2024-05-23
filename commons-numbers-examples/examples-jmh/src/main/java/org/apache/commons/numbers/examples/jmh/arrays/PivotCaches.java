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
 * Support for creating {@link PivotCache} implementations.
 *
 * @since 1.2
 */
final class PivotCaches {
    /** Default value for an unset upper floating pivot.
     * Set as a value higher than any valid array index. */
    private static final int UPPER_DEFAULT = Integer.MAX_VALUE;

    /** No instances. */
    private PivotCaches() {}

    /**
     * Return a {@link PivotCache} for a single {@code k}.
     *
     * @param k Index.
     * @return the pivot cache
     */
    static PivotCache ofIndex(int k) {
        return new PointPivotCache(k);
    }

    /**
     * Return a {@link PivotCache} for a single {@code k},
     * or a pair of indices {@code (k, k+1)}. A pair is
     * signalled using the sign bit.
     *
     * @param k Paired index.
     * @return the pivot cache
     */
    static PivotCache ofPairedIndex(int k) {
        if (k >= 0) {
            return new PointPivotCache(k);
        }
        // Remove sign bit
        final int ka = k & Integer.MAX_VALUE;
        return new RangePivotCache(ka, ka + 1);
    }

    /**
     * Return a {@link PivotCache} for the range {@code [left, right]}.
     *
     * <p>If the range contains internal indices, the {@link PivotCache} will not
     * store them and will be {@link PivotCache#sparse() sparse}.
     *
     * <p>The range returned instance may implement {@link ScanningPivotCache}.
     * It should only be cast to a {@link ScanningPivotCache} and used for scanning
     * if it reports itself as non-{@link PivotCache#sparse() sparse}.
     *
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @return the pivot cache
     * @see #ofFullRange(int, int)
     */
    static PivotCache ofRange(int left, int right) {
        validateRange(left, right);
        return left == right ?
            new PointPivotCache(left) :
            new RangePivotCache(left, right);
    }

    /**
     * Return a {@link PivotCache} for the full-range {@code [left, right]}.
     * The returned implementation will be non-{@link PivotCache#sparse() sparse}.
     *
     * <p>The range returned instance may implement {@link ScanningPivotCache}.
     *
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @return the pivot cache
     */
    static PivotCache ofFullRange(int left, int right) {
        validateRange(left, right);
        if (right - left <= 1) {
            return left == right ?
                new PointPivotCache(left) :
                new RangePivotCache(left, right);
        }
        return IndexSet.ofRange(left, right);
    }

    /**
     * Validate the range {@code left <= right}.
     *
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     */
    private static void validateRange(int left, int right) {
        if (right < left) {
            throw new IllegalArgumentException("Invalid range");
        }
    }

    /**
     * PivotCache for range {@code [left, right]} consisting of a single point.
     */
    private static class PointPivotCache implements ScanningPivotCache {
        /** The target point. */
        private final int target;
        /** The upstream pivot closest to the left bound of the support.
         * Provides a lower search bound for the range [left, right]. */
        private int lowerPivot = -1;
        /** The downstream pivot closest to the right bound of the support.
         * Provides an upper search bound for the range [left, right]. */
        private int upperPivot = UPPER_DEFAULT;

        /**
         * @param index Index defining {@code [left, right]}.
         */
        PointPivotCache(int index) {
            this.target = index;
        }

        @Override
        public void add(int index) {
            // Update the floating pivots
            if (index <= target) {
                // This does not update upperPivot if index == target.
                // This case is checked in nextPivot(int).
                lowerPivot = Math.max(index, lowerPivot);
            } else {
                upperPivot = Math.min(index, upperPivot);
            }
        }

        @Override
        public void add(int fromIndex, int toIndex) {
            // Update the floating pivots
            if (toIndex <= target) {
                // This does not update upperPivot if toIndex == target.
                // This case is checked in nextPivot(int).
                lowerPivot = Math.max(toIndex, lowerPivot);
            } else if (fromIndex > target) {
                upperPivot = Math.min(fromIndex, upperPivot);
            } else {
                // Range brackets the target
                lowerPivot = upperPivot = target;
            }
        }

        @Override
        public int left() {
            return target;
        }

        @Override
        public int right() {
            return target;
        }

        @Override
        public boolean sparse() {
            // Not sparse between [left, right]
            return false;
        }

        @Override
        public boolean moveLeft(int newLeft) {
            // Unsupported
            return false;
        }

        @Override
        public boolean contains(int k) {
            return lowerPivot == k;
        }

        @Override
        public int previousPivot(int k) {
            // Only support scanning within [left, right] => assume k == target
            return lowerPivot;
        }

        // Do not override: int nextPivot(int k)

        @Override
        public int nextPivotOrElse(int k, int other) {
            // Only support scanning within [left, right]
            // assume lowerPivot <= left <= k <= right <= upperPivot
            if (lowerPivot == target) {
                return target;
            }
            return upperPivot == UPPER_DEFAULT ? other : upperPivot;
        }

        @Override
        public int nextNonPivot(int k) {
            // Only support scanning within [left, right] => assume k == target
            return lowerPivot == target ? target + 1 : target;
        }

        @Override
        public int previousNonPivot(int k) {
            // Only support scanning within [left, right] => assume k == target
            return lowerPivot == target ? target - 1 : target;
        }
    }

    /**
     * PivotCache for range {@code [left, right]} consisting of a bracketing range
     * {@code lower <= left < right <= upper}.
     *
     * <p>Behaviour is undefined if {@code left == right}. This cache is intended to
     * bracket a range [left, right] that can be entirely sorted, e.g. if the separation
     * between left and right is small.
     */
    private static class RangePivotCache implements ScanningPivotCache {
        /** Left bound of the support. */
        private final int left;
        /** Right bound of the support. */
        private final int right;
        /** The upstream pivot closest to the left bound of the support.
         * Provides a lower search bound for the range [left, right]. */
        private int lowerPivot = -1;
        /** The downstream pivot closest to the right bound of the support.
         * Provides an upper search bound for the range [left, right]. */
        private int upperPivot = UPPER_DEFAULT;

        /**
         * @param left Lower bound (inclusive).
         * @param right Upper bound (inclusive).
         */
        RangePivotCache(int left, int right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public void add(int index) {
            // Update the floating pivots
            if (index <= left) {
                lowerPivot = Math.max(index, lowerPivot);
            } else if (index >= right) {
                upperPivot = Math.min(index, upperPivot);
            }
        }

        @Override
        public void add(int fromIndex, int toIndex) {
            // Update the floating pivots
            if (toIndex <= left) {
                //     l-------------r
                // f---t
                lowerPivot = Math.max(toIndex, lowerPivot);
            } else if (fromIndex >= right) {
                //   l-------------r
                //                 f---t
                upperPivot = Math.min(fromIndex, upperPivot);
            } else {
                // Range [left, right] overlaps [from, to]
                // toIndex > left && fromIndex < right
                //   l-------------r
                // f---t
                //        f----t
                //               f----t
                if (fromIndex <= left) {
                    lowerPivot = left;
                }
                if (toIndex >= right) {
                    upperPivot = right;
                }
            }
        }

        @Override
        public int left() {
            return left;
        }

        @Override
        public int right() {
            return right;
        }

        @Override
        public boolean sparse() {
            // Sparse if there are internal points between [left, right]
            return right - left > 1;
        }

        @Override
        public boolean moveLeft(int newLeft) {
            // Unsupported
            return false;
        }

        @Override
        public boolean contains(int k) {
            return lowerPivot == k || upperPivot == k;
        }

        @Override
        public int previousPivot(int k) {
            // Only support scanning within [left, right]
            // assume lowerPivot <= left <= k <= right <= upperPivot
            return k == upperPivot ? k : lowerPivot;
        }

        // Do not override: int nextPivot(int k)

        @Override
        public int nextPivotOrElse(int k, int other) {
            // Only support scanning within [left, right]
            // assume lowerPivot <= left <= k <= right <= upperPivot
            if (k == lowerPivot) {
                return k;
            }
            return upperPivot == UPPER_DEFAULT ? other : upperPivot;
        }

        @Override
        public int nextNonPivot(int k) {
            // Only support scanning within [left, right]
            // assume lowerPivot <= left <= k <= right <= upperPivot
            if (sparse()) {
                throw new UnsupportedOperationException();
            }
            // range of size 2
            // scan right
            int i = k;
            if (i == left) {
                if (lowerPivot != left) {
                    return left;
                }
                i++;
            }
            if (i == right && upperPivot == right) {
                i++;
            }
            return i;
        }

        @Override
        public int previousNonPivot(int k) {
            // Only support scanning within [left, right]
            // assume lowerPivot <= left <= k <= right <= upperPivot
            if (sparse()) {
                throw new UnsupportedOperationException();
            }
            // range of size 2
            // scan left
            int i = k;
            if (i == right) {
                if (upperPivot != right) {
                    return right;
                }
                i--;
            }
            if (i == left && lowerPivot == left) {
                i--;
            }
            return i;
        }
    }
}
