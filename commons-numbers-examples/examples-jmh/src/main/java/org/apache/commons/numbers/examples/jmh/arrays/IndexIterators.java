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
 * Support for creating {@link IndexIterator} implementations.
 *
 * @since 1.2
 */
final class IndexIterators {

    /** No instances. */
    private IndexIterators() {}

    /**
     * Creates an iterator for index {@code k}.
     *
     * @param k Index.
     * @return the iterator
     */
    static IndexIterator ofIndex(int k) {
        return new SingleIndex(k);
    }

    /**
     * Creates an iterator for the closed interval {@code [k1, k2]}.
     *
     * <p>This method handles duplicate indices; indices can be in any order.
     *
     * @param k1 Index.
     * @param k2 Index.
     * @return the iterator
     */
    static IndexIterator ofInterval(int k1, int k2) {
        // Eliminate duplicates
        if (k1 == k2) {
            return new SingleIndex(k1);
        }
        // Sort
        final int i1 = k1 < k2 ? k1 : k2;
        final int i2 = k1 < k2 ? k2 : k1;
        return new SingleInterval(i1, i2);
    }

    /**
     * {@link IndexIterator} for a single index.
     */
    private static final class SingleIndex implements IndexIterator {
        /** Index. */
        private final int k;

        /**
         * @param k Index.
         */
        SingleIndex(int k) {
            this.k = k;
        }

        @Override
        public int left() {
            return k;
        }

        @Override
        public int right() {
            return k;
        }

        @Override
        public int end() {
            return k;
        }

        @Override
        public boolean next() {
            return false;
        }

        @Override
        public boolean positionAfter(int index) {
            return k > index;
        }

        @Override
        public boolean nextAfter(int index) {
            // right >= end : no next index
            return true;
        }
    }

    /**
     * {@link IndexIterator} for a single closed interval {@code [left, right]}.
     */
    private static final class SingleInterval implements IndexIterator {
        /** Left index. */
        private final int l;
        /** Right index. */
        private final int r;

        /**
         * @param l Left index.
         * @param r Right index.
         */
        SingleInterval(int l, int r) {
            this.l = l;
            this.r = r;
        }

        @Override
        public int left() {
            return l;
        }

        @Override
        public int right() {
            return r;
        }

        @Override
        public int end() {
            return r;
        }

        @Override
        public boolean next() {
            return false;
        }

        @Override
        public boolean positionAfter(int index) {
            return r > index;
        }

        @Override
        public boolean nextAfter(int index) {
            // right >= end : no next index
            return true;
        }
    }
}
