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

package org.apache.commons.numbers.combinatorics;

import java.io.Serializable;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Iterator;
import java.util.Comparator;

import org.apache.commons.numbers.core.ArithmeticUtils;

/**
 * Utility to create <a href="http://en.wikipedia.org/wiki/Combination">
 * combinations</a> {@code (n, k)} of {@code k} elements in a set of
 * {@code n} elements.
 */
public final class Combinations implements Iterable<int[]> {
    /** Size of the set from which combinations are drawn. */
    private final int n;
    /** Number of elements in each combination. */
    private final int k;

    /**
     * @param n Size of the set from which subsets are selected.
     * @param k Size of the subsets to be enumerated.
     * @throws IllegalArgumentException if {@code n < 0}.
     * @throws IllegalArgumentException if {@code k > n} or {@code k < 0}.
     */
    private Combinations(int n,
                         int k) {
        BinomialCoefficient.checkBinomial(n, k);
        this.n = n;
        this.k = k;
    }

    /**
     * Create an instance.
     *
     * @param n Size of the set from which subsets are selected.
     * @param k Size of the subsets to be enumerated.
     * @throws IllegalArgumentException if {@code n < 0}.
     * @throws IllegalArgumentException if {@code k > n} or {@code k < 0}.
     * @return a new instance.
     */
    public static Combinations of(int n,
                                  int k) {
        return new Combinations(n, k);
    }

    /**
     * Gets the size of the set from which combinations are drawn.
     *
     * @return the size of the universe.
     */
    public int getN() {
        return n;
    }

    /**
     * Gets the number of elements in each combination.
     *
     * @return the size of the subsets to be enumerated.
     */
    public int getK() {
        return k;
    }

    /**
     * Creates an iterator whose range is the k-element subsets of
     * {0, ..., n - 1} represented as {@code int[]} arrays.
     * <p>
     * The iteration order is lexicographic: the arrays returned by the
     * {@link #iterator() iterator} are sorted in descending order and
     * they are visited in lexicographic order with significance from
     * right to left.
     * For example, {@code new Combinations(4, 2).iterator()} returns
     * an iterator that will generate the following sequence of arrays
     * on successive calls to
     * {@code next()}:<br>
     * {@code [0, 1], [0, 2], [1, 2], [0, 3], [1, 3], [2, 3]}
     * </p>
     * If {@code k == 0} an iterator containing an empty array is returned;
     * if {@code k == n} an iterator containing [0, ..., n - 1] is returned.
     */
    @Override
    public Iterator<int[]> iterator() {
        return k == 0 || k == n ?
            new SingletonIterator(k) :
            new LexicographicIterator(n, k);
    }

    /**
     * Creates a comparator.
     * When performing a comparison, if an element of the array is not
     * within the interval [0, {@code n}), an {@code IllegalArgumentException}
     * will be thrown.
     *
     * @return a comparator.
     */
    public Comparator<int[]> comparator() {
        return new LexicographicComparator(n, k);
    }

    /**
     * Lexicographic combinations iterator.
     * <p>
     * Implementation follows Algorithm T in <i>The Art of Computer Programming</i>
     * Internet Draft (PRE-FASCICLE 3A), "A Draft of Section 7.2.1.3 Generating All
     * Combinations, D. Knuth, 2004.</p>
     * <p>
     * The degenerate cases {@code k == 0} and {@code k == n} are NOT handled by this
     * implementation. It is assumed that {@code n > k > 0}.
     * </p>
     */
    private static class LexicographicIterator implements Iterator<int[]> {
        /** Size of subsets returned by the iterator. */
        private final int k;

        /**
         * c[1], ..., c[k] stores the next combination; c[k + 1], c[k + 2] are
         * sentinels.
         * <p>
         * Note that c[0] is "wasted" but this makes it a little easier to
         * follow the code.
         * </p>
         */
        private final int[] c;

        /** Return value for {@link #hasNext()}. */
        private boolean more = true;

        /** Marker: smallest index such that {@code c[j + 1] > j}. */
        private int j;

        /**
         * Construct a CombinationIterator to enumerate {@code k}-sets from a set
         * of size {@code n}.
         * <p>
         * NOTE: It is assumed that {@code n > k > 0}.
         * </p>
         *
         * @param n Size of the set from which subsets are enumerated.
         * @param k Size of the subsets to enumerate.
         */
        LexicographicIterator(int n, int k) {
            this.k = k;
            c = new int[k + 3];
            // Initialize c to start with lexicographically first k-set
            for (int i = 1; i <= k; i++) {
                c[i] = i - 1;
            }
            // Initialize sentinels
            c[k + 1] = n;
            c[k + 2] = 0;
            j = k; // Set up invariant: j is smallest index such that c[j + 1] > j
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return more;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int[] next() {
            if (!more) {
                throw new NoSuchElementException();
            }
            // Copy return value (prepared by last activation)
            final int[] ret = new int[k];
            System.arraycopy(c, 1, ret, 0, k);

            // Prepare next iteration
            // T2 and T6 loop
            int x = 0;
            if (j > 0) {
                x = j;
                c[j] = x;
                j--;
                return ret;
            }
            // T3
            if (c[1] + 1 < c[2]) {
                c[1]++;
                return ret;
            } else {
                j = 2;
            }
            // T4
            boolean stepDone = false;
            while (!stepDone) {
                c[j - 1] = j - 2;
                x = c[j] + 1;
                if (x == c[j + 1]) {
                    j++;
                } else {
                    stepDone = true;
                }
            }
            // T5
            if (j > k) {
                more = false;
                return ret;
            }
            // T6
            c[j] = x;
            j--;
            return ret;
        }

        /**
         * Not supported.
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Iterator with just one element to handle degenerate cases (full array,
     * empty array) for combination iterator.
     */
    private static class SingletonIterator implements Iterator<int[]> {
        /** Number of elements of the singleton array. */
        private final int n;
        /** True on initialization, false after first call to next. */
        private boolean more = true;
        /**
         * Create a singleton iterator providing the given array.
         *
         * @param n Size of the singleton array returned by the iterator.
         */
        SingletonIterator(final int n) {
            this.n = n;
        }
        /**
         * @return {@code true} until next is called the first time,
         * then {@code false}.
         **/
        @Override
        public boolean hasNext() {
            return more;
        }
        /**
         * @return the singleton at the first activation.
         * @throws NoSuchElementException after the first activation.
         */
        @Override
        public int[] next() {
            if (more) {
                more = false;
                // Create singleton.
                final int[] s = new int[n];
                for (int i = 0; i < n; i++) {
                    s[i] = i;
                }
                return s;
            } else {
                throw new NoSuchElementException();
            }
        }
        /**
         * Unsupported.
         *
         * @throws UnsupportedOperationException Remove is not supported.
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Defines a lexicographic ordering of the combinations.
     *
     * The comparison is based on the value (in base 10) represented
     * by the digit (interpreted in base {@code n}) in the input array,
     * in reverse order.
     */
    private static class LexicographicComparator
        implements Comparator<int[]>,
                   Serializable {
        /** Serializable version identifier. */
        private static final long serialVersionUID = 20170520L;
        /** Size of the set from which combinations are drawn. */
        private final int n;
        /** Number of elements in each combination. */
        private final int k;

        /**
         * @param n Size of the set from which subsets are selected.
         * @param k Size of the subsets to be enumerated.
         */
        LexicographicComparator(int n,
                                int k) {
            this.n = n;
            this.k = k;
        }

        /**
         * {@inheritDoc}
         *
         * @throws IllegalArgumentException if the array lengths are not
         * equal to {@code k}.
         * @throws IllegalArgumentException if an element of the array is not
         * within the interval [0, {@code n}).
         */
        @Override
        public int compare(int[] c1,
                           int[] c2) {
            if (c1.length != k) {
                throw new CombinatoricsException(CombinatoricsException.MISMATCH, k, c1.length);
            }
            if (c2.length != k) {
                throw new CombinatoricsException(CombinatoricsException.MISMATCH, k, c2.length);
            }

            // Method "lexNorm" works with ordered arrays.
            final int[] c1s = Arrays.copyOf(c1, k);
            final int[] c2s = Arrays.copyOf(c2, k);
            Arrays.sort(c1s);
            Arrays.sort(c2s);

            final long v1 = lexNorm(c1s);
            final long v2 = lexNorm(c2s);

            return Long.compare(v1, v2);
        }

        /**
         * Computes the value (in base 10) represented by the digit
         * (interpreted in base {@code n}) in the input array in reverse
         * order.
         * For example if {@code c} is {@code {3, 2, 1}}, and {@code n}
         * is 3, the method will return 18.
         *
         * @param c Input array.
         * @return the lexicographic norm.
         * @throws IllegalArgumentException if an element of the array is not
         * within the interval [0, {@code n}).
         */
        private long lexNorm(int[] c) {
            long ret = 0;
            for (int i = 0; i < c.length; i++) {
                final int digit = c[i];
                if (digit < 0 ||
                    digit >= n) {
                    throw new CombinatoricsException(CombinatoricsException.OUT_OF_RANGE, digit, 0, n - 1);
                }

                ret += c[i] * ArithmeticUtils.pow((long) n, i);
            }
            return ret;
        }
    }
}
