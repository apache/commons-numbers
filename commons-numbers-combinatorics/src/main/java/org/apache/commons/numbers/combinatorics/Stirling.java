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

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Computation of <a href="https://en.wikipedia.org/wiki/Stirling_number">Stirling numbers</a>.
 *
 * @since 1.2
 */
public final class Stirling {
    /** Stirling S1 error message. */
    private static final String S1_ERROR_FORMAT = "s(n=%d, k=%d)";
    /** Stirling S2 error message. */
    private static final String S2_ERROR_FORMAT = "S(n=%d, k=%d)";
    /** Overflow threshold for n when computing s(n, 1). */
    private static final int S1_OVERFLOW_K_EQUALS_1 = 21;
    /** Overflow threshold for n when computing s(n, n-2). */
    private static final int S1_OVERFLOW_K_EQUALS_NM2 = 92682;
    /** Overflow threshold for n when computing s(n, n-3). */
    private static final int S1_OVERFLOW_K_EQUALS_NM3 = 2761;
    /** Overflow threshold for n when computing S(n, n-2). */
    private static final int S2_OVERFLOW_K_EQUALS_NM2 = 92683;
    /** Overflow threshold for n when computing S(n, n-3). */
    private static final int S2_OVERFLOW_K_EQUALS_NM3 = 2762;

    /**
     * Precomputed Stirling numbers of the first kind.
     * Provides a thread-safe lazy initialization of the cache.
     */
    private static final class StirlingS1Cache {
        /** Maximum n to compute (exclusive).
         * As s(21,3) = 13803759753640704000 is larger than Long.MAX_VALUE
         * we must stop computation at row 21. */
        static final int MAX_N = 21;
        /** Stirling numbers of the first kind. */
        static final long[][] S1;

        static {
            S1 = new long[MAX_N][];
            // Initialise first two rows to allow s(2, 1) to use s(1, 1)
            S1[0] = new long[] {1};
            S1[1] = new long[] {0, 1};
            for (int n = 2; n < S1.length; n++) {
                S1[n] = new long[n + 1];
                S1[n][0] = 0;
                S1[n][n] = 1;
                for (int k = 1; k < n; k++) {
                    S1[n][k] = S1[n - 1][k - 1] - (n - 1) * S1[n - 1][k];
                }
            }
        }
    }

    /**
     * Precomputed Stirling numbers of the second kind.
     * Provides a thread-safe lazy initialization of the cache.
     */
    private static final class StirlingS2Cache {
        /** Maximum n to compute (exclusive).
         * As S(26,9) = 11201516780955125625 is larger than Long.MAX_VALUE
         * we must stop computation at row 26. */
        static final int MAX_N = 26;
        /** Stirling numbers of the second kind. */
        static final long[][] S2;

        static {
            S2 = new long[MAX_N][];
            S2[0] = new long[] {1};
            for (int n = 1; n < S2.length; n++) {
                S2[n] = new long[n + 1];
                S2[n][0] = 0;
                S2[n][1] = 1;
                S2[n][n] = 1;
                for (int k = 2; k < n; k++) {
                    S2[n][k] = k * S2[n - 1][k] + S2[n - 1][k - 1];
                }
            }
        }
    }

    /** Private constructor. */
    private Stirling() {
        // intentionally empty.
    }

    /**
     * Returns the <em>signed</em> <a
     * href="https://mathworld.wolfram.com/StirlingNumberoftheFirstKind.html">
     * Stirling number of the first kind</a>, "{@code s(n,k)}". The number of permutations of
     * {@code n} elements which contain exactly {@code k} permutation cycles is the
     * nonnegative number: {@code |s(n,k)| = (-1)^(n-k) s(n,k)}
     *
     * @param n Size of the set
     * @param k Number of permutation cycles ({@code 0 <= k <= n})
     * @return {@code s(n,k)}
     * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0} or {@code k > n}.
     * @throws ArithmeticException if some overflow happens, typically for n exceeding 20
     * (s(n,n-1) is handled specifically and does not overflow)
     */
    public static long stirlingS1(int n, int k) {
        checkArguments(n, k);

        if (n < StirlingS1Cache.MAX_N) {
            // The number is in the small cache
            return StirlingS1Cache.S1[n][k];
        }

        // Simple cases
        // https://en.wikipedia.org/wiki/Stirling_numbers_of_the_first_kind#Simple_identities
        if (k == 0) {
            return 0;
        } else if (k == n) {
            return 1;
        } else if (k == 1) {
            checkN(n, k, S1_OVERFLOW_K_EQUALS_1, S1_ERROR_FORMAT);
            // Note: Only occurs for n=21 so avoid computing the sign with pow(-1, n-1) * (n-1)!
            return Factorial.value(n - 1);
        } else if (k == n - 1) {
            return -BinomialCoefficient.value(n, 2);
        } else if (k == n - 2) {
            checkN(n, k, S1_OVERFLOW_K_EQUALS_NM2, S1_ERROR_FORMAT);
            // (3n-1) * binom(n, 3) / 4
            return productOver4(3L * n - 1, BinomialCoefficient.value(n, 3));
        } else if (k == n - 3) {
            checkN(n, k, S1_OVERFLOW_K_EQUALS_NM3, S1_ERROR_FORMAT);
            return -BinomialCoefficient.value(n, 2) * BinomialCoefficient.value(n, 4);
        }

        // Compute using:
        // s(n + 1, k) = s(n, k - 1)     - n       * s(n, k)
        // s(n, k)     = s(n - 1, k - 1) - (n - 1) * s(n - 1, k)

        // n >= 21 (MAX_N)
        // 2 <= k <= n-4

        // Start at the largest easily computed value: n < MAX_N or k < 2
        final int reduction = Math.min(n - StirlingS1Cache.MAX_N, k - 2) + 1;
        int n0 = n - reduction;
        int k0 = k - reduction;

        long sum = stirlingS1(n0, k0);
        while (n0 < n) {
            k0++;
            sum = Math.subtractExact(
                sum,
                Math.multiplyExact(n0, stirlingS1(n0, k0))
            );
            n0++;
        }

        return sum;
    }

    /**
     * Returns the <a
     * href="https://mathworld.wolfram.com/StirlingNumberoftheSecondKind.html">
     * Stirling number of the second kind</a>, "{@code S(n,k)}", the number of
     * ways of partitioning an {@code n}-element set into {@code k} non-empty
     * subsets.
     *
     * @param n Size of the set
     * @param k Number of non-empty subsets ({@code 0 <= k <= n})
     * @return {@code S(n,k)}
     * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0} or {@code k > n}.
     * @throws ArithmeticException if some overflow happens, typically for n exceeding 25 and
     * k between 20 and n-2 (S(n,n-1) is handled specifically and does not overflow)
     */
    public static long stirlingS2(int n, int k) {
        checkArguments(n, k);

        if (n < StirlingS2Cache.MAX_N) {
            // The number is in the small cache
            return StirlingS2Cache.S2[n][k];
        }

        // Simple cases
        if (k == 0) {
            return 0;
        } else if (k == 1 || k == n) {
            return 1;
        } else if (k == 2) {
            checkN(n, k, 64, S2_ERROR_FORMAT);
            return (1L << (n - 1)) - 1L;
        } else if (k == n - 1) {
            return BinomialCoefficient.value(n, 2);
        } else if (k == n - 2) {
            checkN(n, k, S2_OVERFLOW_K_EQUALS_NM2, S2_ERROR_FORMAT);
            // (3n-5) * binom(n, 3) / 4
            return productOver4(3L * n - 5, BinomialCoefficient.value(n, 3));
        } else if (k == n - 3) {
            checkN(n, k, S2_OVERFLOW_K_EQUALS_NM3, S2_ERROR_FORMAT);
            return BinomialCoefficient.value(n - 2, 2) * BinomialCoefficient.value(n, 4);
        }

        // Compute using:
        // S(n, k) = k * S(n - 1, k) + S(n - 1, k - 1)

        // n >= 26 (MAX_N)
        // 3 <= k <= n-3

        // Start at the largest easily computed value: n < MAX_N or k < 3
        final int reduction = Math.min(n - StirlingS2Cache.MAX_N, k - 3) + 1;
        int n0 = n - reduction;
        int k0 = k - reduction;

        long sum = stirlingS2(n0, k0);
        while (n0 < n) {
            k0++;
            sum = Math.addExact(
                Math.multiplyExact(k0, stirlingS2(n0, k0)),
                sum
            );
            n0++;
        }

        return sum;
    }

    /**
     * Check {@code 0 <= k <= n}.
     *
     * @param n N
     * @param k K
     * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0} or {@code k > n}.
     */
    private static void checkArguments(int n, int k) {
        // Combine all checks with a single branch:
        // 0 <= n; 0 <= k <= n
        // Note: If n >= 0 && k >= 0 && n - k < 0 then k > n.
        // Bitwise or will detect a negative sign bit in any of the numbers
        if ((n | k | (n - k)) < 0) {
            // Raise the correct exception
            if (n < 0) {
                throw new CombinatoricsException(CombinatoricsException.NEGATIVE, n);
            }
            throw new CombinatoricsException(CombinatoricsException.OUT_OF_RANGE, k, 0, n);
        }
    }

    /**
     * Check {@code n <= threshold}, or else throw an {@link ArithmeticException}.
     *
     * @param n N
     * @param k K
     * @param threshold Threshold for {@code n}
     * @param msgFormat Error message format
     * @throws ArithmeticException if overflow is expected to happen
     */
    private static void checkN(int n, int k, int threshold, String msgFormat) {
        if (n > threshold) {
            throw new ArithmeticException(String.format(msgFormat, n, k));
        }
    }

    /**
     * Return {@code a*b/4} without intermediate overflow.
     * It is assumed that:
     * <ul>
     * <li>The coefficients a and b are positive</li>
     * <li>The product (a*b) is an exact multiple of 4</li>
     * <li>The result (a*b/4) is an exact integer that does not overflow a {@code long}</li>
     * </ul>
     *
     * <p>A conditional branch is performed on the odd/even property of {@code b}.
     * The branch is predictable if {@code b} is typically the same parity.
     *
     * @param a Coefficient a
     * @param b Coefficient b
     * @return {@code a*b/4}
     */
    private static long productOver4(long a, long b) {
        // Compute (a*b/4) without intermediate overflow.
        // The product (a*b) must be an exact multiple of 4.
        // If b is even: ((b/2) * a) / 2
        // If b is odd then a must be even to make a*b even: ((a/2) * b) / 2
        return (b & 1) == 0 ?
            ((b >>> 1) * a) >>> 1 :
            ((a >>> 1) * b) >>> 1;
    }

    /**
     * From a collection of {@code n} items, generates all partitions that contains {@code k} subsets.
     * For example:
     * <pre>{@code
     * Stirling.S2.of(4, 2)
     *     .stream()
     *     .forEach(p -> System.out.println(java.util.Arrays.deepToString(p)));
     * }</pre>
     * will output
     * <pre>
     * [[0, 1, 2], [3]]
     * [[0, 1, 3], [2]]
     * [[0, 1], [2, 3]]
     * [[0, 2, 3], [1]]
     * [[0, 2], [1, 3]]
     * [[0, 3], [1, 2]]
     * [[0], [1, 2, 3]]
     * </pre>
     *
     * <p>
     * Method {@link #get() S2.of(n, k).get()} returns the number of partitions.
     * </p>
     *
     * <p>
     * A <a href="https://mathworld.wolfram.com/RestrictedGrowthString.html">restrictive growth string
     * (RGS)</a> is used internally.  RGS uses integers to represent items:  Position (index) in the
     * RGS array is the same as in the original list to be partitioned, value is the "group" to which
     * this element belongs in a given partition.
     * </p>
     */
    public static final class S2 {
        /** Number of sublists in every partition (aka "k"). */
        private final int numberOfSubsets;
        /** Number of partitions. */
        private final long stirlingS2;
        /** Number of elements in the original list (aka "n"). */
        private final int numberOfElements;
        /** Difference between the number of items and the required number of subsets. */
        private final int nMinusK;
        /** Helper .*/
        private final int nMinusOne;
        /** Helper .*/
        private final int kMinusOne;

        /**
         * Constructor.
         *
         * @param n Number of elements.
         * @param k Number of sublists in each partition.
         * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0} or {@code k > n}.
         */
        private S2(int n,
                   int k) {
            stirlingS2 = stirlingS2(n, k);
            numberOfElements = n;
            numberOfSubsets = k;
            nMinusK = n - k;
            nMinusOne = n - 1;
            kMinusOne = k - 1;
        }

        /**
         * Factory.
         *
         * @param n Number of items to partition.
         * @param k Number of sublists in each partition.
         * @return a new instance.
         */
        public static S2 of(int n,
                            int k) {
            return new S2(n, k);
        }

        /**
         * Get the number of partitions (aka "Stirling number of the second kind").
         *
         * @return the number of partitions.
         */
        public long get() {
            return stirlingS2;
        }

        /**
         * Iteration wrapped in a stream.
         * This method must be kept "internal" to ensure consistency: Argument
         * {@code gen} must be a {@link PartitionGenerator} instance tied to
         * {@code this} instance.
         *
         * @param gen Partition generator.
         * @return a stream (without duplicate or "null" elements).
         *
         * @param <T> Partition representation.
         */
        private <T> Stream<T> streamInternal(Iterable<T> gen) {
            final int characteristics = Spliterator.DISTINCT | Spliterator.NONNULL;
            return StreamSupport.stream(Spliterators.spliterator(gen.iterator(),
                                                                 stirlingS2,
                                                                 characteristics),
                                        false);
        }

        /**
         * Iteration wrapped in a stream, where each element is a partition,
         * into {@code k} subsets of a set of {@code n} elements.
         *
         * @return a stream (without duplicate or "null" elements).
         */
        public Stream<int[][]> stream() {
            return streamInternal(partitionGenerator());
        }

        /**
         * Factory method for iterating on the partitions of the given list
         * of {@code items}.
         *
         * @param k Number of sublists in each partition.
         * @param items Items to be partitioned.
         * @return a stream (without duplicate or "null" elements).
         *
         * @param <T> Item type.
         */
        public static <T> Stream<List<List<T>>> stream(List<T> items,
                                                       int k) {
            return of(items.size(), k).stream().map(o -> mapPartition(o, items, k));
        }

        /**
         * Iteration wrapped in a stream.
         *
         * @param items Items to be partitioned.
         * @return a stream (without duplicate or "null" elements).
         * @throws IllegalArgumentException if the number of {@code items} does
         * not match the {@link #of(int,int) first argument of the factory method}.
         *
         * @param <T> Item type.
         */
        public <T> Stream<List<List<T>>> stream(T... items) {
            if (items.length != numberOfElements) {
                throw new CombinatoricsException(CombinatoricsException.MISMATCH,
                                                 numberOfElements, items.length);
            }

            final List<T> list = Arrays.asList(items);
            return stream().map(o -> mapPartition(o, list, numberOfSubsets));
        }

        /**
         * Creates a partition generator that returns each partition as
         * indices between {@code 0} (included) and {@code n} (excluded).
         *
         * @return a new instance.
         */
        public Iterable<int[][]> partitionGenerator() {
            return new Iterable<int[][]>() {
                /** {@inheritDoc} */
                @Override
                public Iterator<int[][]> iterator() {
                    return new PartitionIterator();
                }
            };
        }

        // Commented out: Should RGS functionality be implemented in a dedicated class?
        // /**
        //  * Creates a partition generator that returns each partition as a list
        //  * of {@code n} elements whose value, between {@code 0} (included) and
        //  * {@code k} (excluded), indicates to which subsets that element belongs.
        //  *
        //  * @return a new instance.
        //  */
        // public Iterable<int[]> restrictedGrowthStringGenerator() {
        //     return new Iterable<int[]>() {
        //         /** {@inheritDoc} */
        //         @Override
        //         public Iterator<int[]> iterator() {
        //             return new RestrictedGrowthStringIterator();
        //         }
        //     };
        // }

        /**
         * Maps a given partition to a user-defined list of objects.
         *
         * @param p Partition.
         * @param items List of objects.
         * @param k Number of sublists.
         * @return the mapped partition.
         *
         * @param <T> Item type.
         */
        private static <T> List<List<T>> mapPartition(int[][] p,
                                                      List<T> items,
                                                      int k) {
            final List<List<T>> out = new ArrayList<>(k);

            for (int[] subset : p) {
                final List<T> customSubset = new ArrayList<>(subset.length);

                for (int i : subset) {
                    customSubset.add(items.get(i));
                }

                out.add(customSubset);
            }

            return out;
        }

        /**
         * Iterator.
         */
        private final class PartitionIterator implements Iterator<int[][]> {
            /** Delegate to RGS implementation. */
            private final RestrictedGrowthStringIterator delegate = new RestrictedGrowthStringIterator();

            /** {@inheritDoc} */
            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            /** {@inheritDoc} */
            @Override
            public int[][] next() {
                return rgs2partition(delegate.next());
            }

            /**
             * Maps a given RGS to a list of indices.
             *
             * @param rgs RGS.
             * @return the partition.
             */
            private int[][] rgs2partition(int[] rgs) {
                // Size of every subsets of the partition described by "rgs".
                final int[] sizes = new int[numberOfSubsets];
                for (int i = 0; i < numberOfElements; i++) {
                    ++sizes[rgs[i]];
                }

                final int[][] out = new int[numberOfSubsets][];
                for (int i = 0; i < numberOfSubsets; i++) {
                    out[i] = new int[sizes[i]];
                }

                final int[] counts = new int[numberOfSubsets];
                for (int i = 0; i < numberOfElements; i++) {
                    final int groupIdx = rgs[i];
                    out[groupIdx][counts[groupIdx]++] = i;
                }

                return out;
            }
        }

        /**
         * Iterator.
         */
        private final class RestrictedGrowthStringIterator implements Iterator<int[]> {
            /** RGS array (current state). */
            private final int[] rgs = new int[numberOfElements];
            /** RGS array (current state). */
            private final int[] maxRgs = new int[numberOfElements];
            /** Current partition. */
            private final int[] currentRGS = new int[numberOfElements];
            /** Number of generated partitions. */
            private long partitionCount = 0;
            /** Whether there is another partition. */
            private boolean hasNext = true;

            /**
             * Constructor.
             */
            /* package-private */ RestrictedGrowthStringIterator() {
                // Initialize the first valid lexicographical RGS matching k-subsets.
                for (int i = nMinusK + 1; i < numberOfElements; i++) {
                    rgs[i] = i - nMinusK;
                }
                for (int i = 1; i < numberOfElements; i++) {
                    final int iMinusOne = i - 1;
                    maxRgs[i] = Math.max(maxRgs[iMinusOne],
                                         rgs[iMinusOne]);
                }

                calculateNext();
            }

            /** {@inheritDoc} */
            @Override
            public boolean hasNext() {
                return hasNext;
            }

            /** {@inheritDoc} */
            @Override
            public int[] next() {
                if (!hasNext) {
                    throw new NoSuchElementException();
                }

                // Copy to prevent exposing internal data.
                final int[] c = Arrays.copyOf(currentRGS, numberOfElements);

                calculateNext();

                return c;
            }

            /** Generates next partition. */
            private void calculateNext() {
                hasNext = partitionCount < stirlingS2;

                if (numberOfElements > 0) {
                    while (true) {
                        if (maxRgs[nMinusOne] == kMinusOne ||
                            rgs[nMinusOne] == kMinusOne) { // Partition contains "k" blocks.
                            build();
                            updateRgs();
                            return;
                        }

                        updateRgs();
                    }
                } else {
                    build();
                }
            }

            /** Updates RGS. */
            private void updateRgs() {
                int i = nMinusOne;
                while (i > 0) {
                    if (rgs[i] < kMinusOne &&
                        rgs[i] <= maxRgs[i]) {
                        ++rgs[i];
                        break;
                    }
                    --i;
                }

                if (i == 0) {
                    return;
                }

                Arrays.fill(rgs, i + 1, numberOfElements, 0);

                for (int j = i; j < numberOfElements; j++) {
                    final int jMinusOne = j - 1;
                    maxRgs[j] = Math.max(maxRgs[jMinusOne],
                                         rgs[jMinusOne]);
                }
            }

            /**
             * Generates state to be returned by {@link #next()}.
             */
            private void build() {
                System.arraycopy(rgs, 0, currentRGS, 0, numberOfElements);

                // Keep count to prevent infinite loop in "calculateNext()".
                ++partitionCount;
            }
        }
    }
}
