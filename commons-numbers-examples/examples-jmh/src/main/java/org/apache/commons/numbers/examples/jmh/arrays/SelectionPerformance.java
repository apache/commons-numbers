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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.numbers.arrays.Selection;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.ArraySampler;
import org.apache.commons.rng.sampling.PermutationSampler;
import org.apache.commons.rng.sampling.distribution.DiscreteUniformSampler;
import org.apache.commons.rng.sampling.distribution.SharedStateDiscreteSampler;
import org.apache.commons.rng.simple.RandomSource;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Executes a benchmark of the selection of indices from array data.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx8192M"})
public class SelectionPerformance {
    /** Use the JDK sort function. */
    private static final String JDK = "JDK";
    /** Use a sort function. */
    private static final String SORT = "Sort";
    /** Baseline for the benchmark. */
    private static final String BASELINE = "Baseline";
    /** Selection method using a heap. */
    private static final String HEAP_SELECT = "HeapSelect";
    /** Selection method using a sort. */
    private static final String SORT_SELECT = "SortSelect";

    // First generation partition functions.
    // These are based on the KthSelector class used in Commons Math:
    // - Single k or a pair of indices (k,k+1) are selected in a single
    // call; multiple indices cache pivots in a heap structure or use a BitSet.
    // - They dynamically correct signed zeros when they are encountered.

    /** Single-pivot partitioning. This method uses a special comparison of double
     * values similar to {@link Double#compare(double, double)}. This handles
     * NaN and signed zeros. */
    private static final String SP = "SP";
    /** Single-pivot partitioning; uses a BitSet to cache pivots. */
    private static final String SPN = "SPN";
    /** Single-pivot partitioning using a heap to cache pivots.
     * This method is copied from Commons Math. */
    private static final String SPH = "SPH";
    /** Bentley-McIlroy partitioning (Sedgewick); uses a BitSet to cache pivots. */
    private static final String SBM = "SBM";
    /** Bentley-McIlroy partitioning (original); uses a BitSet to cache pivots. */
    private static final String BM = "BM";
    /** Dual-pivot partitioning; uses a BitSet to cache pivots. */
    private static final String DP = "DP";
    /** Dual-pivot partitioning with 5 sorted points to choose pivots; uses a BitSet to cache pivots. */
    private static final String DP5 = "5DP";

    // Second generation partition functions.
    // These pre-process data to sort NaN to the end and count signed zeros;
    // post-processing is performed to restore signed zeros in order.
    // The exception is SBM2 which dynamically corrects signed zeros.

    /** Bentley-McIlroy partitioning (Sedgewick). This second generation function
     * dynamically corrects signed zeros when they are encountered. It is based on
     * the fastest first generation method with changes to allow different pivot
     * store strategies: SEQUENTIAL, INDEX_SET, PIVOT_CACHE. */
    private static final String SBM2 = "2SBM";

    /** Floyd-Rivest partitioning. Only for single k. */
    private static final String FR = "FR";
    /** Floyd-Rivest partitioning (Kiwiel). Only for a single k. */
    private static final String KFR = "KFR";

    // Introselect functions - switch to a stopper function when progress is poor.
    // Allow specification of configuration using the name parameter:
    // single-pivot strategy (Partition.SPStrategy);
    // multiple key strategy (Partition.KeyStrategy);
    // paired (close) keys strategy (Partition.PairedKeyStrategy);
    // edge-selection strategy (Partition.EdgeSelectStrategy);
    // stopper strategy (Partition.StopperStrategy).
    // Parameters to control strategies and introspection are set using the name parameter.
    // See PartitionFactory for details.

    /** Introselect implementation with single-pivot partitioning. */
    private static final String ISP = "ISP";
    /** Introselect implementation with dual-pivot partitioning. */
    private static final String IDP = "IDP";

    // Single k selection using various methods which provide linear runtime (Order(n)).

    /** Linearselect implementation with single pivot partitioning using median-of-medians-of-5
     * for pivot selection. */
    private static final String LSP = "LSP";
    /** Linearselect implementation with single pivot partitioning using optimised
     * median-of-medians. */
    private static final String LINEAR = "Linear";
    /** Quickselect adaptive implementation. Has configuration of the far-step method and some
     * adaption modes. */
    private static final String QA = "QA";
    /** Quickselect adaptive implementation. Uses the best performing far-step method and
     * has configurable adaption control allowing starting at and skipping over adaption modes. */
    private static final String QA2 = "QA2";

    /** Commons Numbers select implementation. This method is built using the best performing
     * select function across a range of input data. This algorithm cannot be configured. */
    private static final String SELECT = "SELECT";

    /** Random source. */
    private static final RandomSource RANDOM_SOURCE = RandomSource.XO_RO_SHI_RO_128_PP;

    /**
     * Source of {@code double} array data.
     *
     * <p>By default this uses the adverse input test suite from figure 1 in Bentley and McIlroy
     * (1993) Engineering a sort function, Software, practice and experience, Vol.23(11),
     * 1249–1265.
     *
     * <p>An alternative set of data is from Valois (2000) Introspective sorting and selection
     * revisited, Software, practice and experience, Vol.30(6), 617-638.
     *
     * <p>Note
     *
     * <p>This class has setter methods to allow re-use in unit testing without requiring
     * use of reflection to set fields. Parameters set by JMH are initialized to their
     * defaults for convenience. Re-use requires:
     *
     * <ol>
     * <li>Creating an instance of the abstract class that provides the data length</li>
     * <li>Calling {@link #setup()} to create the data</li>
     * <li>Iterating over the data</li>
     * </ol>
     *
     * <pre>
     * AbstractDataSource s = new AbstractDataSource() {
     *     protected int getLength() {
     *         return 123;
     *     }
     * };
     * s.setDistribution(Distribution.SAWTOOTH, Distribution.SHUFFLE);
     * s.setModification(Modification.REVERSE_FRONT);
     * s.setRange(2);
     * s.setup();
     * for (int i = 0; i &lt; s.size(); i++) {
     *     s.getData(i);
     * }
     * </pre>
     *
     * <p>Random distribution mode
     *
     * <p>The default BM configuration includes random samples generated as a family of
     * single samples created from ranges that are powers of two [0, 2^i). This small set
     * of samples is only a small representation of randomness. For small lengths this may
     * only be a few random samples.
     *
     * <p>The data source can be changed to generate a fixed number of random samples
     * using a uniform distribution [0, n]. For this purpose the distribution must be set
     * to {@link Distribution#RANDOM} and the {@link #setSamples(int) samples} set above
     * zero. The inclusive upper bound {@code n} is set using the {@link #setSeed(int) seed}.
     * If this is zero then the default is {@link Integer#MAX_VALUE}.
     *
     * <p>Order
     *
     * <p>Data are created in distribution families. If these are passed in order to a
     * partition method the JVM can change behaviour of the algorithm as branch prediction
     * statistics stabilise for the family. To mitigate this effect the order is permuted
     * per invocation of the benchmark (see {@link #createOrder()}. This stabilises the
     * average timing results from JMH. Using per-invocation data generation requires
     * the benchmark execution time is higher than 1 millisecond. Benchmarks that use
     * tiny data (e.g. sort 5 elements) must use several million samples.
     */
    @State(Scope.Benchmark)
    public abstract static class AbstractDataSource {
        /** All distributions / modifications. */
        private static final String ALL = "all";
        /** All distributions / modifications in the Bentley and McIlroy test suite. */
        private static final String BM = "bm";
        /** All distributions in the Valois test suite. These currently ignore the seed.
         * To replicate Valois used a fixed seed and the copy modification. */
        private static final String VALOIS = "valois";
        /** Flag to determine if the data size should be logged. This is useful to be
         * able to determine the execution time per sample when the number of samples
         * is dynamically created based on the data length, range and seed. */
        private static final AtomicInteger LOG_SIZE = new AtomicInteger();

        /**
         * The type of distribution.
         */
        enum Distribution {
            // B&M (1993)

            /** Sawtooth distribution. Ascending data from 0 to m, that repeats. */
            SAWTOOTH,
            /** Random distribution. Uniform random data in [0, m] */
            RANDOM,
            /** Stagger distribution. Multiple interlaced ascending sequences. */
            STAGGER,
            /** Plateau distribution. Ascending data from 0 to m, then constant.  */
            PLATEAU,
            /** Shuffle distribution. Two randomly interlaced ascending sequences of different lengths. */
            SHUFFLE,

            /** Sharktooth distribution. Alternating ascending then descending data from 0
             * to m and back. This is an addition to the original suite of BM
             * and is not included in the test suite by default and must be specified.
             *
             * <p>An ascending then descending sequence is also known as organpipe in
             * Valois (2000). This version allows multiple ascending/descending runs in the
             * same length. */
            SHARKTOOTH,

            // Valois (2000)

            /** Sorted. */
            SORTED,
            /** Permutation of ones and zeros. */
            ONEZERO,
            /** Musser's median-of-3 killer. This elicits worst case performance for a median-of-3
             * pivot selection strategy. */
            M3KILLER,
            /** A sorted sequence rotated left once. */
            ROTATED,
            /** Musser's two-faced sequence (the median-of-3 killer with two random permutations). */
            TWOFACED,
            /** An ascending then descending sequence. */
            ORGANPIPE;
        }

        /**
         * The type of data modification.
         */
        enum Modification {
            /** Copy modification. */
            COPY,
            /** Reverse modification. */
            REVERSE,
            /** Reverse front-half modification. */
            REVERSE_FRONT,
            /** Reverse back-half modification. */
            REVERSE_BACK,
            /** Sort modification. */
            SORT,
            /** Descending modification (this is an addition to the original suite of BM).
             * It is useful for testing worst case performance, e.g. insertion sort performs
             * poorly on descending data. Heapselect using a max heap (to find k minimum elements)
             * would perform poorly if data is processed in the forward direction as all elements
             * must be inserted.
             *
             * <p>This is not included in the test suite by default and must be specified.
             * Note that the Shuffle distribution with a very large seed 'm' is effectively an
             * ascending sequence and will be reversed to descending as part of the original
             * B&M suite of data. */
            DESCENDING,
            /** Dither modification. Add i % 5 to the data element i.  */
            DITHER;
        }

        /**
         * Sample information. Used to obtain information about samples that may be slow
         * for a particular partition method, e.g. they use excessive recursion during quickselect.
         * This is used for testing: each sample from the data source can provide the
         * information to create the sample distribution.
         */
        public static final class SampleInfo {
            /** Distribution. */
            private final Distribution dist;
            /** Modification. */
            private final Modification mod;
            /** Length. */
            private final int n;
            /** Seed. */
            private final int m;
            /** Offset. */
            private final int o;

            /**
             * @param dist Distribution.
             * @param mod Modification.
             * @param n Length.
             * @param m Seed.
             * @param o Offset.
             */
            SampleInfo(Distribution dist, Modification mod, int n, int m, int o) {
                this.dist = dist;
                this.mod = mod;
                this.n = n;
                this.m = m;
                this.o = o;
            }

            /**
             * Create an instance with the specified distribution.
             *
             * @param v Value.
             * @return the instance
             */
            SampleInfo with(Distribution v) {
                return new SampleInfo(v, mod, n, m, o);
            }

            /**
             * Create an instance with the specified modification.
             *
             * @param v Value.
             * @return the instance
             */
            SampleInfo with(Modification v) {
                return new SampleInfo(dist, v, n, m, o);
            }

            /**
             * @return the distribution
             */
            Distribution getDistribution() {
                return dist;
            }

            /**
             * @return the modification
             */
            Modification getModification() {
                return mod;
            }

            /**
             * @return the data length
             */
            int getN() {
                return n;
            }

            /**
             * @return the distribution seed
             */
            int getM() {
                return m;
            }

            /**
             * @return the distribution offset
             */
            int getO() {
                return o;
            }

            @Override
            public String toString() {
                return String.format("%s, %s, n=%d, m=%d, o=%d", dist, mod, n, m, o);
            }
        }

        /** Order. This is randomized to ensure that successive calls do not partition
         * similar distributions. Randomized per invocation to avoid the JVM 'learning'
         * branch decisions on small data sets. */
        protected int[] order;
        /** Cached source of randomness. */
        protected UniformRandomProvider rng;

        /** Type of data. Multiple types can be specified in the same string using
         * lower/upper case, delimited using ':'. */
        @Param({BM})
        private String distribution = BM;

        /** Type of data modification. Multiple types can be specified in the same string using
         * lower/upper case, delimited using ':'. */
        @Param({BM})
        private String modification = BM;

        /** Extra range to add to the data length.
         * E.g. Use 1 to force use of odd and even length samples. */
        @Param({"1"})
        private int range = 1;

        /** Sample 'seed'. This is {@code m} in Bentley and McIlroy's test suite.
         * If set to zero the default is to use powers of 2 based on sample size. */
        @Param({"0"})
        private int seed;

        /** Sample offset. This is used to shift each distribution to create different data.
         * It is advanced on each invocation of {@link #setup()}. */
        @Param({"0"})
        private int offset;

        /** Number of samples. Applies only to the random distribution. In this case
         * the length of the data is randomly chosen in {@code [length, length + range)}. */
        @Param({"0"})
        private int samples;

        /** RNG seed. Created using ThreadLocalRandom.current().nextLong(). This is advanced
         * for the random distribution mode per iteration. Each benchmark executed by
         * JMH will use the same random data, even across JVMs.
         *
         * <p>If this is zero then a random seed is chosen. */
        @Param({"-7450238124206088695"})
        private long rngSeed = -7450238124206088695L;

        /** Data. This is stored as integer data which saves memory. Note that when ranking
         * data it is not necessary to have the full range of the double data type; the same
         * number of unique values can be recorded in an array using an integer type.
         * Returning a double[] forces a copy to be generated for destructive sorting /
         * partitioning methods. */
        private int[][] data;

        /** Sample information. */
        private List<SampleInfo> sampleInfo;

        /**
         * Gets the sample for the given {@code index}.
         *
         * <p>This is returned in a randomized order per iteration.
         *
         * @param index Index.
         * @return the data sample
         */
        public double[] getData(int index) {
            return getDataSample(order[index]);
        }

        /**
         * Gets the sample for the given {@code index}.
         *
         * <p>This is returned in a randomized order per iteration.
         *
         * @param index Index.
         * @return the data sample
         */
        public int[] getIntData(int index) {
            return getIntDataSample(order[index]);
        }

        /**
         * Gets the sample for the given {@code index}.
         *
         * @param index Index.
         * @return the data sample
         */
        protected double[] getDataSample(int index) {
            final int[] a = data[index];
            final double[] x = new double[a.length];
            for (int i = -1; ++i < a.length;) {
                x[i] = a[i];
            }
            return x;
        }

        /**
         * Gets the sample for the given {@code index}.
         *
         * @param index Index.
         * @return the data sample
         */
        protected int[] getIntDataSample(int index) {
            // For parity with other methods do not use data.clone()
            final int[] a = data[index];
            final int[] x = new int[a.length];
            for (int i = -1; ++i < a.length;) {
                x[i] = a[i];
            }
            return x;
        }

        /**
         * Gets the sample size for the given {@code index}.
         *
         * @param index Index.
         * @return the data sample size
         */
        public int getDataSize(int index) {
            return data[index].length;
        }

        /**
         * Gets the sample information for the given {@code index}.
         * Matches the (native) order returned by {@link #getDataSample(int)}.
         *
         * @param index Index.
         * @return the data sample information
         */
        SampleInfo getDataSampleInfo(int index) {
            return sampleInfo.get(index);
        }

        /**
         * Get the number of data samples.
         *
         * <p>Note: This data source will create a permutation order per invocation based on
         * this size. Per-invocation control in JMH is recommended for methods that take
         * more than 1 millisecond to execute. For very small data and/or fast methods
         * this may not be achievable. Child classes may override this value to create
         * a large number of repeats of the same data per invocation. Any class performing
         * this should also override {@link #getData(int)} to prevent index out of bound errors.
         * This can be done by mapping the index to the original index using the number of repeats
         * e.g. {@code original index = index / repeats}.
         *
         * @return the number of samples
         */
        public int size() {
            return data.length;
        }

        /**
         * Create the data.
         */
        @Setup(Level.Iteration)
        public void setup() {
            Objects.requireNonNull(distribution);
            Objects.requireNonNull(modification);

            // Set-up using parameters (may throw)
            final EnumSet<Distribution> dist = getDistributions();
            final int length = getLength();
            if (length < 1) {
                throw new IllegalStateException("Unsupported length: " + length);
            }
            // Note: Bentley-McIlroy use n in {100, 1023, 1024, 1025}.
            // Here we only support a continuous range.
            final int r = range > 0 ? range : 0;
            if (length + (long) r > Integer.MAX_VALUE) {
                throw new IllegalStateException("Unsupported upper length: " + length);
            }
            final int length2 = length + r;

            // Allow pseudorandom seeding
            if (rngSeed == 0) {
                rngSeed = RandomSource.createLong();
            }
            if (rng == null) {
                // First call, create objects
                rng = RANDOM_SOURCE.create(rngSeed);
            }

            // Special case for random distribution mode
            if (dist.contains(Distribution.RANDOM) && dist.size() == 1 && samples > 0) {
                data = new int[samples][];
                sampleInfo = new ArrayList<>(samples);
                final int upper = seed > 0 ? seed : Integer.MAX_VALUE;
                final SharedStateDiscreteSampler s1 = DiscreteUniformSampler.of(rng, 0, upper);
                final SharedStateDiscreteSampler s2 = DiscreteUniformSampler.of(rng, length, length2);
                for (int i = 0; i < data.length; i++) {
                    final int[] a = new int[s2.sample()];
                    for (int j = a.length; --j >= 0;) {
                        a[j] = s1.sample();
                    }
                    data[i] = a;
                    sampleInfo.add(new SampleInfo(Distribution.RANDOM, Modification.COPY, a.length, 0, 0));
                }
                return;
            }

            // New data per iteration
            data = null;
            final int o = offset;
            offset = rng.nextInt();

            final EnumSet<Modification> mod = getModifications();

            // Data using the RNG will be randomized only once.
            // Here we use the same seed for parity across benchmark methods.
            // Note that most distributions do not use the source of randomness.
            final ArrayList<int[]> sampleData = new ArrayList<>();
            sampleInfo = new ArrayList<>();
            final List<SampleInfo> info = new ArrayList<>();
            for (int n = length; n <= length2; n++) {
                // Note: Large lengths may wish to limit the range of m to limit
                // the memory required to store the samples. Currently a single
                // m is supported via the seed parameter.
                // Default seed will create ceil(log2(2*n)) * 5 dist * 6 mods samples:
                // MAX  = 32 * 5 * 7 * (2^31-1) * 4 bytes == 7679 GiB
                // HUGE = 31 * 5 * 7 * 2^30 * 4 bytes == 3719 GiB
                // BIG  = 21 * 5 * 7 * 2^20 * 4 bytes == 2519 MiB  <-- within configured JVM -Xmx
                // MED  = 11 * 5 * 7 * 2^10 * 4 bytes == 1318 KiB
                // (This is for the B&M data.)
                // It is possible to create lengths above 2^30 using a single distribution,
                // modification, and seed:
                // MAX1 = 1 * 1 * 1 * (2^31-1) * 4 bytes == 8191 MiB
                // However this is then used to create double[] data thus requiring an extra
                // ~16GiB memory for the sample to partition.
                for (final int m : createSeeds(seed, n)) {
                    final List<int[]> d = createDistributions(dist, rng, n, m, o, info);
                    for (int i = 0; i < d.size(); i++) {
                        final int[] x = d.get(i);
                        final SampleInfo si = info.get(i);
                        if (mod.contains(Modification.COPY)) {
                            // Don't copy! All other methods generate copies
                            // so we can use this in-place.
                            sampleData.add(x);
                            sampleInfo.add(si.with(Modification.COPY));
                        }
                        if (mod.contains(Modification.REVERSE)) {
                            sampleData.add(reverse(x, 0, n));
                            sampleInfo.add(si.with(Modification.REVERSE));
                        }
                        if (mod.contains(Modification.REVERSE_FRONT)) {
                            sampleData.add(reverse(x, 0, n >>> 1));
                            sampleInfo.add(si.with(Modification.REVERSE_FRONT));
                        }
                        if (mod.contains(Modification.REVERSE_BACK)) {
                            sampleData.add(reverse(x, n >>> 1, n));
                            sampleInfo.add(si.with(Modification.REVERSE_BACK));
                        }
                        // Only sort once
                        if (mod.contains(Modification.SORT) ||
                            mod.contains(Modification.DESCENDING)) {
                            final int[] y = x.clone();
                            Arrays.sort(y);
                            if (mod.contains(Modification.DESCENDING)) {
                                sampleData.add(reverse(y, 0, n));
                                sampleInfo.add(si.with(Modification.DESCENDING));
                            }
                            if (mod.contains(Modification.SORT)) {
                                sampleData.add(y);
                                sampleInfo.add(si.with(Modification.SORT));
                            }
                        }
                        if (mod.contains(Modification.DITHER)) {
                            sampleData.add(dither(x));
                            sampleInfo.add(si.with(Modification.DITHER));
                        }
                    }
                }
            }
            data = sampleData.toArray(new int[0][]);
            if (LOG_SIZE.getAndSet(length) != length) {
                Logger.getLogger(getClass().getName()).info(
                    () -> String.format("Data length: [%d, %d] n=%d", length, length2, data.length));
            }
        }

        /**
         * Create the order to process the indices.
         *
         * <p>JMH recommends that invocations should take at
         * least 1 millisecond for timings to be usable. In practice there should be
         * enough data that processing takes much longer than a few milliseconds.
         */
        @Setup(Level.Invocation)
        public void createOrder() {
            if (order == null) {
                // First call, create objects
                order = PermutationSampler.natural(size());
            }
            ArraySampler.shuffle(rng, order);
        }

        /**
         * @return the distributions
         */
        private EnumSet<Distribution> getDistributions() {
            EnumSet<Distribution> dist;
            if (BM.equals(distribution)) {
                dist = EnumSet.of(
                    Distribution.SAWTOOTH,
                    Distribution.RANDOM,
                    Distribution.STAGGER,
                    Distribution.PLATEAU,
                    Distribution.SHUFFLE);
            } else if (VALOIS.equals(distribution)) {
                dist = EnumSet.of(
                    Distribution.RANDOM,
                    Distribution.SORTED,
                    Distribution.ONEZERO,
                    Distribution.M3KILLER,
                    Distribution.ROTATED,
                    Distribution.TWOFACED,
                    Distribution.ORGANPIPE);
            } else {
                dist = getEnumFromParam(Distribution.class, distribution);
            }
            return dist;
        }

        /**
         * @return the modifications
         */
        private EnumSet<Modification> getModifications() {
            EnumSet<Modification> mod;
            if (BM.equals(modification)) {
                // Modifications are from Bentley and McIlroy
                mod = EnumSet.allOf(Modification.class);
                // ... except descending
                mod.remove(Modification.DESCENDING);
            } else if (VALOIS.equals(modification)) {
                // For convenience alias Valois to copy
                mod = EnumSet.of(Modification.COPY);
            } else {
                mod = getEnumFromParam(Modification.class, modification);
            }
            return mod;
        }

        /**
         * Gets all the enum values of the given class from the parameters.
         *
         * @param <E> Enum type.
         * @param cls Class of the enum.
         * @param parameters Parameters (multiple values delimited by ':')
         * @return the enum values
         */
        static <E extends Enum<E>> EnumSet<E> getEnumFromParam(Class<E> cls, String parameters) {
            if (ALL.equals(parameters)) {
                return EnumSet.allOf(cls);
            }
            final EnumSet<E> set = EnumSet.noneOf(cls);
            final String s = parameters.toUpperCase(Locale.ROOT);
            for (final E e : cls.getEnumConstants()) {
                // Scan for the name
                for (int i = s.indexOf(e.name(), 0); i >= 0; i = s.indexOf(e.name(), i)) {
                    // Ensure a full match to the name:
                    // either at the end of the string, or followed by the delimiter
                    i += e.name().length();
                    if (i == s.length() || s.charAt(i) == ':') {
                        set.add(e);
                        break;
                    }
                }
            }
            if (set.isEmpty()) {
                throw new IllegalStateException("Unknown parameters: " + parameters);
            }
            return set;
        }

        /**
         * Creates the seeds.
         *
         * <p>This can be pasted into a JShell terminal to verify it works for any size
         * {@code 1 <= n < 2^31}. With the default behaviour all seeds {@code m} are unsigned
         * strictly positive powers of 2 and the highest seed should be below {@code 2*n}.
         *
         * @param seed Seed (use 0 for default; or provide a strictly positive {@code 1 <= m <= 2^31}).
         * @param n Sample size.
         * @return the seeds
         */
        private static int[] createSeeds(int seed, int n) {
            // Allow [1, 2^31] (note 2^31 is negative but handled as a power of 2)
            if (seed - 1 >= 0) {
                return new int[] {seed};
            }
            // Bentley-McIlroy use:
            // for: m = 1; m < 2 * n; m *= 2
            // This has been modified here to handle n up to MAX_VALUE
            // by knowing the count of m to generate as the power of 2 >= n.

            // ceil(log2(n)) + 1 == ceil(log2(2*n)) but handles MAX_VALUE
            int c = 33 - Integer.numberOfLeadingZeros(n - 1);
            final int[] seeds = new int[c];
            c = 0;
            for (int m = 1; c != seeds.length; m *= 2) {
                seeds[c++] = m;
            }
            return seeds;
        }

        /**
         * Creates the distribution samples. Handles {@code m = 2^31} using
         * {@link Integer#MIN_VALUE}.
         *
         * <p>The offset is used to adjust each distribution to generate a different
         * output. Only applies to distributions that do not use the source of randomness.
         *
         * <p>Distributions are generated in enum order and recorded in the output {@code info}.
         * Distributions that are a constant value at {@code m == 1} are not generated.
         * This case is handled by the plateau distribution which will be a constant value
         * except one occurrence of zero.
         *
         * @param dist Distributions.
         * @param rng Source of randomness.
         * @param n Length of the sample.
         * @param m Sample seed (in [1, 2^31])
         * @param o Offset.
         * @param info Sample information.
         * @return the samples
         */
        private static List<int[]> createDistributions(EnumSet<Distribution> dist,
                UniformRandomProvider rng, int n, int m, int o, List<SampleInfo> info) {
            final ArrayList<int[]> distData = new ArrayList<>(6);
            int[] x;
            info.clear();
            SampleInfo si = new SampleInfo(null, null, n, m, o);
            // B&M (1993)
            if (dist.contains(Distribution.SAWTOOTH) && m != 1) {
                x = createSample(distData, info, si.with(Distribution.SAWTOOTH));
                // i % m
                // Typical case m is a power of 2 so we can use a mask
                // Use the offset.
                final int mask = m - 1;
                if ((m & mask) == 0) {
                    for (int i = -1; ++i < n;) {
                        x[i] = (i + o) & mask;
                    }
                } else {
                    // User input seed. Start at the offset.
                    int j = Integer.remainderUnsigned(o, m);
                    for (int i = -1; ++i < n;) {
                        j = j % m;
                        x[i] = j++;
                    }
                }
            }
            if (dist.contains(Distribution.RANDOM) && m != 1) {
                x = createSample(distData, info, si.with(Distribution.RANDOM));
                // rand() % m
                // A sampler is faster than rng.nextInt(m); the sampler has an inclusive upper.
                final SharedStateDiscreteSampler s = DiscreteUniformSampler.of(rng, 0, m - 1);
                for (int i = -1; ++i < n;) {
                    x[i] = s.sample();
                }
            }
            if (dist.contains(Distribution.STAGGER)) {
                x = createSample(distData, info, si.with(Distribution.STAGGER));
                // Overflow safe: (i * m + i) % n
                final long nn = n;
                final long oo = Integer.toUnsignedLong(o);
                for (int i = -1; ++i < n;) {
                    final long j = i + oo;
                    x[i] = (int) ((j * m + j) % nn);
                }
            }
            if (dist.contains(Distribution.PLATEAU)) {
                x = createSample(distData, info, si.with(Distribution.PLATEAU));
                // min(i, m)
                for (int i = Math.min(n, m); --i >= 0;) {
                    x[i] = i;
                }
                for (int i = m - 1; ++i < n;) {
                    x[i] = m;
                }
                // Rotate
                final int n1 = Integer.remainderUnsigned(o, n);
                if (n1 != 0) {
                    final int[] a = x.clone();
                    final int n2 = n - n1;
                    System.arraycopy(a, 0, x, n1, n2);
                    System.arraycopy(a, n2, x, 0, n1);
                }
            }
            if (dist.contains(Distribution.SHUFFLE) && m != 1) {
                x = createSample(distData, info, si.with(Distribution.SHUFFLE));
                // rand() % m ? (j += 2) : (k += 2)
                final SharedStateDiscreteSampler s = DiscreteUniformSampler.of(rng, 0, m - 1);
                for (int i = -1, j = 0, k = 1; ++i < n;) {
                    x[i] = s.sample() != 0 ? (j += 2) : (k += 2);
                }
            }
            // Extra - based on organpipe with a variable ascending/descending length
            if (dist.contains(Distribution.SHARKTOOTH) && m != 1) {
                x = createSample(distData, info, si.with(Distribution.SHARKTOOTH));
                // ascending-descending runs
                int i = -1;
                int j = (o & Integer.MAX_VALUE) % m - 1;
                OUTER:
                for (;;) {
                    while (++j < m) {
                        if (++i == n) {
                            break OUTER;
                        }
                        x[i] = j;
                    }
                    while (--j >= 0) {
                        if (++i == n) {
                            break OUTER;
                        }
                        x[i] = j;
                    }
                }
            }
            // Valois (2000)
            if (dist.contains(Distribution.SORTED)) {
                x = createSample(distData, info, si.with(Distribution.SORTED));
                for (int i = -1; ++i < n;) {
                    x[i] = i;
                }
            }
            if (dist.contains(Distribution.ONEZERO)) {
                x = createSample(distData, info, si.with(Distribution.ONEZERO));
                // permutation of floor(n/2) ones and ceil(n/2) zeroes.
                // For convenience this uses random ones and zeros to avoid a shuffle
                // and simply reads bits from integers. The distribution will not
                // be exactly 50:50.
                final int end = n & ~31;
                for (int i = 0; i < end; i += 32) {
                    int z = rng.nextInt();
                    for (int j = -1; ++j < 32;) {
                        x[i + j] = z & 1;
                        z >>>= 1;
                    }
                }
                for (int i = end; ++i < n;) {
                    x[i] = rng.nextBoolean() ? 1 : 0;
                }
            }
            if (dist.contains(Distribution.M3KILLER)) {
                x = createSample(distData, info, si.with(Distribution.M3KILLER));
                medianOf3Killer(x);
            }
            if (dist.contains(Distribution.ROTATED)) {
                x = createSample(distData, info, si.with(Distribution.ROTATED));
                // sorted sequence rotated left once
                // 1, 2, 3, ..., n-1, 0
                for (int i = 1; i < n; i++) {
                    x[i - 1] = i;
                }
            }
            if (dist.contains(Distribution.TWOFACED)) {
                x = createSample(distData, info, si.with(Distribution.TWOFACED));
                // Musser's two faced randomly permutes a median-of-3 killer in
                // 4 floor(log2(n)) through n/2 and n/2 + 4 floor(log2(n)) through n
                medianOf3Killer(x);
                final int j = 4 * (31 - Integer.numberOfLeadingZeros(n));
                final int n2 = n >>> 1;
                ArraySampler.shuffle(rng, x, j, n2);
                ArraySampler.shuffle(rng, x, n2 + j, n);
            }
            if (dist.contains(Distribution.ORGANPIPE)) {
                x = createSample(distData, info, si.with(Distribution.ORGANPIPE));
                // 0, 1, 2, 3, ..., 3, 2, 1, 0
                // n should be even to leave two equal values in the middle, otherwise a single
                for (int i = -1, j = n; ++i <= --j;) {
                    x[i] = i;
                    x[j] = i;
                }
            }
            return distData;
        }

        /**
         * Create the sample array and add it to the {@code data}; add the information to the {@code info}.
         *
         * @param data Data samples.
         * @param info Sample information.
         * @param s Sample information.
         * @return the new sample array
         */
        private static int[] createSample(ArrayList<int[]> data, List<SampleInfo> info,
            SampleInfo s) {
            final int[] x = new int[s.getN()];
            data.add(x);
            info.add(s);
            return x;
        }

        /**
         * Create Musser's median-of-3 killer sequence (in-place).
         *
         * @param x Data.
         */
        private static void medianOf3Killer(int[] x) {
            // This uses the original K_2k sequence from Musser (1997)
            // Introspective sorting and selection algorithms,
            // Software—Practice and Experience, 27(8), 983–993.
            // A true median-of-3 killer requires n to be an even integer divisible by 4,
            // i.e. k is an even positive integer. This causes a median-of-3 partition
            // strategy to produce a sequence of n/4 partitions into sub-sequences of
            // length 2 and n-2, 2 and n-4, ..., 2 and n/2.
            // 1   2   3   4   5       k-2   k-1  k   k+1 k+2 k+3     2k-1  2k
            // 1, k+1, 3, k+3, 5, ..., 2k-3, k-1 2k-1  2   4   6  ... 2k-2  2k
            final int n = x.length;
            final int k = n >>> 1;
            for (int i = 0; i < k; i++) {
                x[i] = ++i;
                x[i] = k + i;
            }
            for (int i = k - 1, j = 2; ++i < n; j += 2) {
                x[i] = j;
            }
        }

        /**
         * Return a (part) reversed copy of the data.
         *
         * @param x Data.
         * @param from Start index to reverse (inclusive).
         * @param to End index to reverse (exclusive).
         * @return the copy
         */
        private static int[] reverse(int[] x, int from, int to) {
            final int[] a = x.clone();
            for (int i = from - 1, j = to; ++i < --j;) {
                final int v = a[i];
                a[i] = a[j];
                a[j] = v;
            }
            return a;
        }

        /**
         * Return a dithered copy of the data.
         *
         * @param x Data.
         * @return the copy
         */
        private static int[] dither(int[] x) {
            final int[] a = x.clone();
            for (int i = a.length; --i >= 0;) {
                // Bentley-McIlroy use i % 5.
                // It is important this is not a power of 2 so it will not coincide
                // with patterns created in the data using the default m powers-of-2.
                a[i] += i % 5;
            }
            return a;
        }

        /**
         * Gets the minimum length of the data.
         * The actual length is enumerated in {@code [length, length + range]}.
         *
         * @return the length
         */
        protected abstract int getLength();

        /**
         * Gets the range.
         *
         * @return the range
         */
        final int getRange() {
            return range;
        }

        /**
         * Sets the distribution(s) of the data.
         * If the input is an empty array or the first enum value is null,
         * then all distributions are used.
         *
         * @param v Values.
         */
        void setDistribution(Distribution... v) {
            if (v.length == 0 || v[0] == null) {
                distribution = ALL;
            } else {
                final EnumSet<Distribution> s = EnumSet.of(v[0], v);
                distribution = s.stream().map(Enum::name).collect(Collectors.joining(":"));
            }
        }

        /**
         * Sets the modification of the data.
         * If the input is an empty array or the first enum value is null,
         * then all distributions are used.
         *
         * @param v Value.
         */
        void setModification(Modification... v) {
            if (v.length == 0 || v[0] == null) {
                modification = ALL;
            } else {
                final EnumSet<Modification> s = EnumSet.of(v[0], v);
                modification = s.stream().map(Enum::name).collect(Collectors.joining(":"));
            }
        }

        /**
         * Sets the maximum addition to extend the length of each sample of data.
         * The actual length is enumerated in {@code [length, length + range]}.
         *
         * @param v Value.
         */
        void setRange(int v) {
            range = v;
        }

        /**
         * Sets the sample 'seed' used to generate distributions.
         * If set to zero the default is to use powers of 2 based on sample size.
         *
         * <p>Supports positive values and the edge case of {@link Integer#MIN_VALUE}
         * which is treated as an unsigned power of 2.
         *
         * @param v Value (ignored if not within {@code [1, 2^31]}).
         */
        void setSeed(int v) {
            seed = v;
        }

        /**
         * Sets the sample 'offset' used to generate distributions. Advanced to a new
         * random integer on each invocation of {@link #setup()}.
         *
         * @param v Value.
         */
        void setOffset(int v) {
            offset = v;
        }

        /**
         * Sets the number of samples to use for the random distribution mode.
         * See {@link AbstractDataSource} for details.
         *
         * @param v Value.
         */
        void setSamples(int v) {
            samples = v;
        }

        /**
         * Sets the seed for the random number generator.
         *
         * @param v Value.
         */
        void setRngSeed(long v) {
            this.rngSeed = v;
        }
    }

    /**
     * Source of {@code double} array data to sort.
     */
    @State(Scope.Benchmark)
    public static class SortSource extends AbstractDataSource {
        /** Data length. */
        @Param({"1023"})
        private int length;
        /** Number of repeats. This is used to control the number of times the data is processed
         * per invocation. Note that each invocation randomises the order. For very small data
         * and/or fast methods there may not be enough data to achieve the target of 1
         * millisecond per invocation. Use this value to increase the length of each invocation.
         * For example the insertion sort on tiny data, or the sort5 methods, may require this
         * to be 1,000,000 or higher. */
        @Param({"1"})
        private int repeats;

        /** {@inheritDoc} */
        @Override
        protected int getLength() {
            return length;
        }

        /** {@inheritDoc} */
        @Override
        public int size() {
            return super.size() * repeats;
        }

        /** {@inheritDoc} */
        @Override
        public double[] getData(int index) {
            // order = (data index) * repeats + repeat
            // data index = order / repeats
            return super.getDataSample(order[index] / repeats);
        }
    }

    /**
     * Source of k-th indices to partition.
     *
     * <p>This class provides both data to partition and the indices to partition.
     * The indices and data are created per iteration. The order to process them
     * is created per invocation.
     */
    @State(Scope.Benchmark)
    public static class KSource extends AbstractDataSource {
        /** Data length. */
        @Param({"1023"})
        private int length;
        /** Number of indices to select. */
        @Param({"1", "2", "3", "5", "10"})
        private int k;
        /** Number of repeats. */
        @Param({"10"})
        private int repeats;
        /** Distribution mode. K indices can be distributed randomly or uniformly.
         * <ul>
         * <li>"random": distribute k indices randomly</li>
         * <li>"uniform": distribute k indices uniformly but with a random start point</li>
         * <li>"index": Use a single index at k</li>
         * <li>"single": Use a single index at k uniformly spaced points. This mode
         * first generates the spacing for the indices. Then samples from that spacing
         * using the configured repeats. Common usage of k=10 will have 10 samples with a
         * single index, each in a different position.</li>
         * </ul>
         * <p>If the mode ends with a "s" then the indices are sorted. For example "randoms"
         * will sort the random indices.
         */
        @Param({"random"})
        private String mode;
        /** Separation. K can be single indices (s=0) or paired (s!=0). Paired indices are
         * separated using the specified separation. When running in paired mode the
         * number of k is doubled and duplicates may occur. This method is used for
         * testing sparse or uniform distributions of paired indices that may occur when
         * interpolating quantiles. Since the separation is allowed to be above 1 it also
         * allows testing configurations for close indices. */
        @Param({"0"})
        private int s;

        /** Indices. */
        private int[][] indices;
        /** Cache permutation samplers. */
        private PermutationSampler[] samplers;

        /** {@inheritDoc} */
        @Override
        protected int getLength() {
            return length;
        }

        /** {@inheritDoc} */
        @Override
        public int size() {
            return super.size() * repeats;
        }

        /** {@inheritDoc} */
        @Override
        public double[] getData(int index) {
            // order = (data index) * repeats + repeat
            // data index = order / repeats
            return super.getDataSample(order[index] / repeats);
        }

        /** {@inheritDoc} */
        @Override
        public int[] getIntData(int index) {
            return super.getIntDataSample(order[index] / repeats);
        }

        /**
         * Gets the indices for the given {@code index}.
         *
         * @param index Index.
         * @return the data indices
         */
        public int[] getIndices(int index) {
            // order = (data index) * repeats + repeat
            // Directly look-up the indices for this repeat.
            return indices[order[index]];
        }

        /**
         * Create the indices.
         */
        @Override
        @Setup(Level.Iteration)
        public void setup() {
            if (s < 0 || s >= getLength()) {
                throw new IllegalStateException("Invalid separation: " + s);
            }
            super.setup();

            // Data will be randomized per iteration
            if (indices == null) {
                // First call, create objects
                indices = new int[size()][];
                // Cache samplers. These hold an array which is randomized
                // per call to obtain a permutation.
                if (k > 1) {
                    samplers = new PermutationSampler[getRange() + 1];
                }
            }

            // Create indices in the data sample length.
            // If a separation is provided then the length is reduced by the separation
            // to make space for a second index.

            int index = 0;
            final int noOfSamples = super.size();
            if (mode.startsWith("random")) {
                // random mode creates a permutation of k indices in the length
                if (k > 1) {
                    final int baseLength = getLength();
                    for (int i = 0; i < noOfSamples; i++) {
                        final int len = getDataSize(i);
                        // Create permutation sampler for the length
                        PermutationSampler sampler = samplers[len - baseLength];
                        if (sampler == null) {
                            // Reduce length by the separation
                            final int n = len - s;
                            samplers[len - baseLength] = sampler = new PermutationSampler(rng, n, k);
                        }
                        for (int j = repeats; --j >= 0;) {
                            indices[index++] = sampler.sample();
                        }
                    }
                } else {
                    // k=1: No requirement for a permutation
                    for (int i = 0; i < noOfSamples; i++) {
                        // Reduce length by the separation
                        final int n = getDataSize(i) - s;
                        for (int j = repeats; --j >= 0;) {
                            indices[index++] = new int[] {rng.nextInt(n)};
                        }
                    }
                }
            } else if (mode.startsWith("uniform")) {
                // uniform indices with a random start
                for (int i = 0; i < noOfSamples; i++) {
                    // Reduce length by the separation
                    final int n = getDataSize(i) - s;
                    final int step = Math.max(1, (int) Math.round((double) n / k));
                    for (int j = repeats; --j >= 0;) {
                        final int[] k1 = new int[k];
                        int p = rng.nextInt(n);
                        for (int m = 0; m < k; m++) {
                            p = (p + step) % n;
                            k1[m] = p;
                        }
                        indices[index++] = k1;
                    }
                }
            } else if (mode.startsWith("single")) {
                // uniform indices with a random start
                for (int i = 0; i < noOfSamples; i++) {
                    // Reduce length by the separation
                    final int n = getDataSize(i) - s;
                    int[] samples;
                    // When k approaches n then a linear spacing covers every part
                    // of the array and we sample. Do this when n < k/4. This handles
                    // k > n (saturation).
                    if (n < (k >> 2)) {
                        samples = rng.ints(k, 0, n).toArray();
                    } else {
                        // Linear spacing
                        final int step = n / k;
                        samples = new int[k];
                        for (int j = 0, x = step >> 1; j < k; j++, x += step) {
                            samples[j] = x;
                        }
                    }
                    for (int j = 0; j < repeats; j++) {
                        final int ii = j % k;
                        if (ii == 0) {
                            ArraySampler.shuffle(rng, samples);
                        }
                        indices[index++] = new int[] {samples[ii]};
                    }
                }
            } else if ("index".equals(mode)) {
                // Same single or paired indices for all samples.
                // Check the index is valid.
                for (int i = 0; i < noOfSamples; i++) {
                    // Reduce length by the separation
                    final int n = getDataSize(i) - s;
                    if (k >= n) {
                        throw new IllegalStateException("Invalid k: " + k + " >= " + n);
                    }
                }
                final int[] kk = s > 0 ? new int[] {k, k + s} : new int[] {k};
                Arrays.fill(indices, kk);
                return;
            } else {
                throw new IllegalStateException("Unknown index mode: " + mode);
            }
            // Add paired indices
            if (s > 0) {
                for (int i = 0; i < indices.length; i++) {
                    final int[] k1 = indices[i];
                    final int[] k2 = new int[k1.length << 1];
                    for (int j = 0; j < k1.length; j++) {
                        k2[j << 1] = k1[j];
                        k2[(j << 1) + 1] = k1[j] + s;
                    }
                    indices[i] = k2;
                }
            }
            // Optionally sort
            if (mode.endsWith("s")) {
                for (int i = 0; i < indices.length; i++) {
                    Arrays.sort(indices[i]);
                }
            }
        }
    }

    /**
     * Source of k-th indices. This does not extend the {@link AbstractDataSource} to provide
     * data to partition. It is to be used to test processing of indices without partition
     * overhead.
     */
    @State(Scope.Benchmark)
    public static class IndexSource {
        /** Indices. */
        protected int[][] indices;
        /** Upper bound (exclusive) on the indices. */
        @Param({"1000", "1000000", "1000000000"})
        private int length;
        /** Number of indices to select. */
        @Param({"10", "20", "40", "80", "160"})
        private int k;
        /** Number of repeats. */
        @Param({"1000"})
        private int repeats;
        /** RNG seed. Created using ThreadLocalRandom.current().nextLong(). Each benchmark
         * executed by JMH will use the same random data, even across JVMs.
         *
         * <p>If this is zero then a random seed is chosen. */
        @Param({"-7450238124206088695"})
        private long rngSeed;
        /** Ordered keys. */
        @Param({"false"})
        private boolean ordered;
        /** Minimum separation between keys. */
        @Param({"32"})
        private int separation;

        /**
         * @return the indices
         */
        public int[][] getIndices() {
            return indices;
        }

        /**
         * Gets the minimum separation between keys. This is used by benchmarks
         * to ignore splitting/search keys below a threshold.
         *
         * @return the minimum separation
         */
        public int getMinSeparation() {
            return separation;
        }

        /**
         * Create the indices and search points.
         */
        @Setup(Level.Iteration)
        public void setup() {
            if (k < 2) {
                throw new IllegalStateException("Require multiple indices");
            }
            // Data will be randomized per iteration. It is the same sequence across
            // benchmarks and JVM instances and allows benchmarking across JVM platforms
            // with the same data.
            // Allow pseudorandom seeding
            if (rngSeed == 0) {
                rngSeed = RandomSource.createLong();
            }
            final UniformRandomProvider rng = RANDOM_SOURCE.create(rngSeed);
            // Advance the seed for the next iteration.
            rngSeed = rng.nextLong();

            final SharedStateDiscreteSampler s = DiscreteUniformSampler.of(rng, 0, length - 1);

            indices = new int[repeats][];

            for (int i = repeats; --i >= 0;) {
                // Indices with possible repeats
                final int[] x = new int[k];
                for (int j = k; --j >= 0;) {
                    x[j] = s.sample();
                }
                indices[i] = x;
                if (ordered) {
                    Sorting.sortIndices(x, x.length);
                }
            }
        }

        /**
         * @return the RNG seed
         */
        long getRngSeed() {
            return rngSeed;
        }
    }

    /**
     * Source of k-th indices to be searched/split.
     * Can be used to split the same indices multiple times, or split a set of indices
     * a single time, e.g. split indices k at point p.
     */
    @State(Scope.Benchmark)
    public static class SplitIndexSource extends IndexSource {
        /** Division mode. */
        @Param({"RANDOM", "BINARY"})
        private DivisionMode mode;

        /** Search points. */
        private int[][] points;
        /** The look-up samples. These are used to identify a set of indices, and a single point to
         * find in the range of the indices, e.g. split indices k at point p. The long packs
         * two integers: the index of the indices k; and the search point p. These are packed
         * as a long to enable easy shuffling of samples and access to the two indices. */
        private long[] samples;

        /** Options for the division mode. */
        public enum DivisionMode {
            /** Randomly divide. */
            RANDOM,
            /** Divide using binary division with recursion left then right. */
            BINARY;
        }

        /**
         * Return the search points. They are the median index points between adjacent
         * indices. These are in the order specified by the division mode.
         *
         * @return the search points
         */
        public int[][] getPoints() {
            return points;
        }

        /**
         * @return the sample size
         */
        int samples() {
            return samples.length;
        }

        /**
         * Gets the indices for the random sample.
         *
         * @param index the index
         * @return the indices
         */
        int[] getIndices(int index) {
            return indices[(int) (samples[index] >>> Integer.SIZE)];
        }

        /**
         * Gets the search point for the random sample.
         *
         * @param index the index
         * @return the search point
         */
        int getPoint(int index) {
            return (int) samples[index];
        }

        /**
         * Create the indices and search points.
         */
        @Override
        @Setup(Level.Iteration)
        public void setup() {
            super.setup();

            final UniformRandomProvider rng = RANDOM_SOURCE.create(getRngSeed());

            final int[][] indices = getIndices();
            points = new int[indices.length][];

            final int s = getMinSeparation();

            // Set the division mode
            final boolean random = Objects.requireNonNull(mode) == DivisionMode.RANDOM;

            int size = 0;

            for (int i = points.length; --i >= 0;) {
                // Get the sorted unique indices
                final int[] y = indices[i].clone();
                final int unique = Sorting.sortIndices(y, y.length);

                // Create the cut points between each unique index
                int[] p = new int[unique - 1];
                if (random) {
                    int c = 0;
                    for (int j = 0; j < p.length; j++) {
                        // Ignore dense keys
                        if (y[j] + s < y[j + 1]) {
                            p[c++] = (y[j] + y[j + 1]) >>> 1;
                        }
                    }
                    p = Arrays.copyOf(p, c);
                    ArraySampler.shuffle(rng, p);
                    points[i] = p;
                } else {
                    // binary division
                    final int c = divide(y, 0, unique - 1, p, 0, s);
                    points[i] = Arrays.copyOf(p, c);
                }
                size += points[i].length;
            }

            // Create the samples: pack indices index+point into a long
            samples = new long[size];
            for (int i = points.length; --i >= 0;) {
                final long l = ((long) i) << Integer.SIZE;
                for (final int p : points[i]) {
                    samples[--size] = l | p;
                }
            }
            ArraySampler.shuffle(rng, samples);
        }

        /**
         * Divide the indices using binary division with recursion left then right.
         * If a division is possible store the division point and update the count.
         *
         * @param indices Indices to divide
         * @param lo Lower index in indices (inclusive).
         * @param hi Upper index in indices (inclusive).
         * @param p Division points.
         * @param c Count of division points.
         * @param s Minimum separation between indices.
         * @return the updated count of division points.
         */
        private static int divide(int[] indices, int lo, int hi, int[] p, int c, int s) {
            if (lo < hi) {
                // Divide the interval in half
                final int m = (lo + hi) >>> 1;
                // Create a division point at approximately the midpoint
                final int m1 = m + 1;
                // Ignore dense keys
                if (indices[m] + s < indices[m1]) {
                    final int k = (indices[m] + indices[m1]) >>> 1;
                    p[c++] = k;
                }
                // Recurse left then right.
                // Does nothing if lo + 1 == hi as m == lo and m1 == hi.
                c = divide(indices, lo, m, p, c, s);
                c = divide(indices, m1, hi, p, c, s);
            }
            return c;
        }
    }

    /**
     * Source of an {@link SearchableInterval}.
     */
    @State(Scope.Benchmark)
    public static class SearchableIntervalSource {
        /** Name of the source. */
        @Param({"ScanningKeyInterval",
            "BinarySearchKeyInterval",
            "IndexSetInterval",
            "CompressedIndexSet",
            // Same speed as the CompressedIndexSet_2
            //"CompressedIndexSet2",
            })
        private String name;

        /** The factory. */
        private Function<int[], SearchableInterval> factory;

        /**
         * @param indices Indices.
         * @return {@link SearchableInterval}
         */
        public SearchableInterval create(int[] indices) {
            return factory.apply(indices);
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            Objects.requireNonNull(name);
            if ("ScanningKeyInterval".equals(name)) {
                factory = k -> {
                    k = k.clone();
                    final int unique = Sorting.sortIndices(k, k.length);
                    return ScanningKeyInterval.of(k, unique);
                };
            } else if ("BinarySearchKeyInterval".equals(name)) {
                factory = k -> {
                    k = k.clone();
                    final int unique = Sorting.sortIndices(k, k.length);
                    return BinarySearchKeyInterval.of(k, unique);
                };
            } else if ("IndexSetInterval".equals(name)) {
                factory = IndexSet::of;
            } else if (name.equals("CompressedIndexSet2")) {
                factory = CompressedIndexSet2::of;
            } else if (name.startsWith("CompressedIndexSet")) {
                // To use compression 2 requires CompressedIndexSet_2 otherwise
                // a fixed compression set will be returned
                final int c = getCompression(name);
                factory = k -> CompressedIndexSet.of(c, k);
            } else {
                throw new IllegalStateException("Unknown SearchableInterval: " + name);
            }
        }

        /**
         * Gets the compression from the last character of the name.
         *
         * @param name Name.
         * @return the compression
         */
        private static int getCompression(String name) {
            final char c = name.charAt(name.length() - 1);
            if (Character.isDigit(c)) {
                return Character.digit(c, 10);
            }
            return 1;
        }
    }

    /**
     * Source of an {@link UpdatingInterval}.
     */
    @State(Scope.Benchmark)
    public static class UpdatingIntervalSource {
        /** Name of the source. */
        @Param({"KeyUpdatingInterval",
            // Same speed as BitIndexUpdatingInterval
            //"IndexSet",
            "BitIndexUpdatingInterval",
            })
        private String name;

        /** The factory. */
        private Function<int[], UpdatingInterval> factory;

        /**
         * @param indices Indices.
         * @return {@link UpdatingInterval}
         */
        public UpdatingInterval create(int[] indices) {
            return factory.apply(indices);
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            Objects.requireNonNull(name);
            if ("KeyUpdatingInterval".equals(name)) {
                factory = k -> {
                    k = k.clone();
                    final int unique = Sorting.sortIndices(k, k.length);
                    return KeyUpdatingInterval.of(k, unique);
                };
            } else if ("IndexSet".equals(name)) {
                factory = k -> IndexSet.of(k).interval();
            } else if (name.equals("BitIndexUpdatingInterval")) {
                factory = k -> BitIndexUpdatingInterval.of(k, k.length);
            } else {
                throw new IllegalStateException("Unknown UpdatingInterval: " + name);
            }
        }
    }

    /**
     * Source of a range of positions to partition. These are positioned away from the edge
     * using a power of 2 shift.
     *
     * <p>This is a specialised class to allow benchmarking the switch from using
     * quickselect partitioning to using an edge selection.
     *
     * <p>This class provides both data to partition and the indices to partition.
     * The indices and data are created per iteration. The order to process them
     * is created per invocation.
     */
    @State(Scope.Benchmark)
    public static class EdgeSource extends AbstractDataSource {
        /** Data length. */
        @Param({"1023"})
        private int length;
        /** Mode. */
        @Param({"SHIFT"})
        private Mode mode;
        /** Parameter to find k. Configured for 'shift' of the length. */
        @Param({"1", "2", "3", "4", "5", "6", "7", "8", "9"})
        private int p;
        /** Target indices (as pairs of {@code [ka, kb]} defining a range to select). */
        private int[][] indices;

        /** Define the method used to generated the edge k. */
        public enum Mode {
            /** Create {@code k} using a right-shift {@code >>>} applied to the length. */
            SHIFT,
            /** Use the parameter {@code p} as an index. */
            INDEX;
        }

        /** {@inheritDoc} */
        @Override
        public int size() {
            return super.size() * 2;
        }

        /** {@inheritDoc} */
        @Override
        public double[] getData(int index) {
            // order = (data index) * repeats + repeat
            // data index = order / repeats; repeats=2 divide by using a shift
            return super.getDataSample(order[index] >> 1);
        }

        /**
         * Gets the sample indices for the given {@code index}.
         * Returns a range to partition {@code [k1, kn]}.
         *
         * @param index Index.
         * @return the target indices
         */
        public int[] getIndices(int index) {
            // order = (data index) * repeats + repeat
            // Directly look-up the indices for this repeat.
            return indices[order[index]];
        }

        /** {@inheritDoc} */
        @Override
        protected int getLength() {
            return length;
        }

        /**
         * Create the data and check the indices are not at the end.
         */
        @Override
        @Setup(Level.Iteration)
        public void setup() {
            // Data will be randomized per iteration
            super.setup();
            // Error for a bad configuration. Allow k=0 but not smaller.
            // Uses the lower bound on the length.
            int k;
            if (mode == Mode.SHIFT) {
                k = length >>> p;
                if (k == 0 && length >>> (p - 1) == 0) {
                    throw new IllegalStateException(length + " >>> (" + p + " - 1) == 0");
                }
            } else if (mode == Mode.INDEX) {
                k = p;
                if (k < 0 || k >= length) {
                    throw new IllegalStateException("Invalid index [0, " + length + "): " + p);
                }
            } else {
                throw new IllegalStateException("Unknown mode: " + mode);
            }

            if (indices == null) {
                // First call, create objects
                indices = new int[size()][];
            }

            // Create a single index at both ends.
            // Note: Data has variable length so we have to compute the upper end for each sample.
            // Re-use the constant lower but we do not bother to cache repeats of the upper.
            final int[] lower = {k, k};
            final int noOfSamples = super.size();
            for (int i = 0; i < noOfSamples; i++) {
                final int len = getDataSize(i);
                final int k1 = len - 1 - k;
                indices[i << 1] = lower;
                indices[(i << 1) + 1] = new int[] {k1, k1};
            }
        }
    }

    /**
     * Source of a sort function.
     */
    @State(Scope.Benchmark)
    public static class SortFunctionSource {
        /** Name of the source. */
        @Param({JDK, SP, BM, SBM, DP, DP5,
            SBM2,
            // Not run by default as it is slow on large data
            //"InsertionSortIF", "InsertionSortIT", "InsertionSort", "InsertionSortB"
            // Introsort methods with defaults, can configure using the name
            // e.g. ISP_SBM_QS50.
            ISP, IDP,
            })
        private String name;

        /** Override of minimum quickselect size. */
        @Param({"0"})
        private int qs;

        /** The action. */
        private Consumer<double[]> function;

        /**
         * @return the function
         */
        public Consumer<double[]> getFunction() {
            return function;
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            Objects.requireNonNull(name);
            if (JDK.equals(name)) {
                function = Arrays::sort;
            // First generation kth-selector functions (not configurable)
            } else if (name.startsWith(SP)) {
                function = PartitionFactory.createKthSelector(name, SP, qs)::sortSP;
            } else if (name.startsWith(SBM)) {
                function = PartitionFactory.createKthSelector(name, SBM, qs)::sortSBM;
            } else if (name.startsWith(BM)) {
                function = PartitionFactory.createKthSelector(name, BM, qs)::sortBM;
            } else if (name.startsWith(DP)) {
                function = PartitionFactory.createKthSelector(name, DP, qs)::sortDP;
            } else if (name.startsWith(DP5)) {
                function = PartitionFactory.createKthSelector(name, DP5, qs)::sortDP5;
            // 2nd generation partition function
            } else if (name.startsWith(SBM2)) {
                function = PartitionFactory.createPartition(name, SBM2, qs, 0)::sortSBM;
            // Introsort
            } else if (name.startsWith(ISP)) {
                function = PartitionFactory.createPartition(name, ISP, qs, 0)::sortISP;
            } else if (name.startsWith(IDP)) {
                function = PartitionFactory.createPartition(name, IDP, qs, 0)::sortIDP;
            // Insertion sort variations.
            // For parity with the internal version these all use the same (shorter) data
            } else if ("InsertionSortIF".equals(name)) {
                function = x -> {
                    // Ignored sentinal
                    x[0] = Double.NEGATIVE_INFINITY;
                    Sorting.sort(x, 1, x.length - 1, false);
                };
            } else if ("InsertionSortIT".equals(name)) {
                // Internal version
                function = x -> {
                    // Add a sentinal
                    x[0] = Double.NEGATIVE_INFINITY;
                    Sorting.sort(x, 1, x.length - 1, true);
                };
            } else if ("InsertionSort".equals(name)) {
                function = x -> {
                    x[0] = Double.NEGATIVE_INFINITY;
                    Sorting.sort(x, 1, x.length - 1);
                };
            } else if (name.startsWith("PairedInsertionSort")) {
                if (name.endsWith("1")) {
                    function = x -> {
                        x[0] = Double.NEGATIVE_INFINITY;
                        Sorting.sortPairedInternal1(x, 1, x.length - 1);
                    };
                } else if (name.endsWith("2")) {
                    function = x -> {
                        x[0] = Double.NEGATIVE_INFINITY;
                        Sorting.sortPairedInternal2(x, 1, x.length - 1);
                    };
                } else if (name.endsWith("3")) {
                    function = x -> {
                        x[0] = Double.NEGATIVE_INFINITY;
                        Sorting.sortPairedInternal3(x, 1, x.length - 1);
                    };
                } else if (name.endsWith("4")) {
                    function = x -> {
                        x[0] = Double.NEGATIVE_INFINITY;
                        Sorting.sortPairedInternal4(x, 1, x.length - 1);
                    };
                }
            } else if ("InsertionSortB".equals(name)) {
                function = x -> {
                    x[0] = Double.NEGATIVE_INFINITY;
                    Sorting.sortb(x, 1, x.length - 1);
                };
            // Not actually a sort. This is used to benchmark the speed of heapselect
            // for a single k as a stopper function against a full sort of small data.
            } else if (name.startsWith(HEAP_SELECT)) {
                final char c = name.charAt(name.length() - 1);
                // This offsets the start by 1 for comparison with insertion sort
                final int k = Character.isDigit(c) ? Character.digit(c, 10) + 1 : 1;
                function = x -> Partition.heapSelectLeft(x, 1, x.length - 1, k, 0);
            }
            if (function == null) {
                throw new IllegalStateException("Unknown sort function: " + name);
            }
        }
    }

    /**
     * Source of a sort function to sort 5 points.
     */
    @State(Scope.Benchmark)
    public static class Sort5FunctionSource {
        /** Name of the source. */
        @Param({"sort5", "sort5b", "sort5c",
            //"sort", "sort5head"
            })
        private String name;

        /** The action. */
        private Consumer<double[]> function;

        /**
         * @return the function
         */
        public Consumer<double[]> getFunction() {
            return function;
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            Objects.requireNonNull(name);
            // Note: We do not run this on input of length 5. We can run it on input of
            // any length above 5. So we choose indices using a spacing of 1/4 of the range.
            // Since we do this for all methods it is a fixed overhead. This allows use
            // of a variety of data sizes.
            if ("sort5".equals(name)) {
                function = x -> {
                    final int s = x.length >> 2;
                    Sorting.sort5(x, 0, s, s << 1, x.length - 1 - s, x.length - 1);
                };
            } else if ("sort5b".equals(name)) {
                function = x -> {
                    final int s = x.length >> 2;
                    Sorting.sort5b(x, 0, s, s << 1, x.length - 1 - s, x.length - 1);
                };
            } else if ("sort5c".equals(name)) {
                function = x -> {
                    final int s = x.length >> 2;
                    Sorting.sort5c(x, 0, s, s << 1, x.length - 1 - s, x.length - 1);
                };
            } else if ("sort".equals(name)) {
                function = x -> Sorting.sort(x, 0, 4);
            } else if ("sort5head".equals(name)) {
                function = x -> Sorting.sort5(x, 0, 1, 2, 3, 4);
            // Median of 5. Ensure the median index is computed by storing it in x
            } else if ("median5".equals(name)) {
                function = x -> {
                    final int s = x.length >> 2;
                    x[0] = Sorting.median5(x, 0, s, s << 1, x.length - 1 - s, x.length - 1);
                };
            } else if ("median5head".equals(name)) {
                function = x -> x[0] = Sorting.median5(x, 0, 1, 2, 3, 4);
            // median of 5 continuous elements
            } else if ("med5".equals(name)) {
                function = x -> x[0] = Sorting.median5(x, 0);
            } else if ("med5b".equals(name)) {
                function = x -> x[0] = Sorting.median5b(x, 0);
            } else if ("med5c".equals(name)) {
                function = x -> x[0] = Sorting.median5c(x, 0);
            } else if ("med5d".equals(name)) {
                function = x -> Sorting.median5d(x, 0, 1, 2, 3, 4);
            } else {
                throw new IllegalStateException("Unknown sort5 function: " + name);
            }
        }
    }

    /**
     * Source of a function to compute the median of 4 points.
     */
    @State(Scope.Benchmark)
    public static class Median4FunctionSource {
        /** Name of the source. */
        @Param({"lower4", "lower4b", "lower4c", "lower4d", "lower4e",
            "upper4", "upper4c", "upper4d",
            // Full sort is slower
            //"sort4"
            })
        private String name;

        /** The action. */
        private Consumer<double[]> function;

        /**
         * @return the function
         */
        public Consumer<double[]> getFunction() {
            return function;
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            Objects.requireNonNull(name);
            // Note: We run this across the entire input array to simulate a pass
            // of the quickselect adaptive algorithm.
            if ("lower4".equals(name)) {
                function = x -> {
                    final int f = x.length >>> 2;
                    final int f2 = f + f;
                    for (int i = f; i < f2; i++) {
                        Sorting.lowerMedian4(x, i - f, i, i + f, i + f2);
                    }
                };
            } else if ("lower4b".equals(name)) {
                function = x -> {
                    final int f = x.length >>> 2;
                    final int f2 = f + f;
                    for (int i = f; i < f2; i++) {
                        Sorting.lowerMedian4b(x, i - f, i, i + f, i + f2);
                    }
                };
            } else if ("lower4c".equals(name)) {
                function = x -> {
                    final int f = x.length >>> 2;
                    final int f2 = f + f;
                    for (int i = f; i < f2; i++) {
                        Sorting.lowerMedian4c(x, i - f, i, i + f, i + f2);
                    }
                };
            } else if ("lower4d".equals(name)) {
                function = x -> {
                    final int f = x.length >>> 2;
                    final int f2 = f + f;
                    for (int i = f; i < f2; i++) {
                        Sorting.lowerMedian4d(x, i - f, i, i + f, i + f2);
                    }
                };
            } else if ("lower4e".equals(name)) {
                function = x -> {
                    final int f = x.length >>> 2;
                    final int f2 = f + f;
                    for (int i = f; i < f2; i++) {
                        Sorting.lowerMedian4e(x, i - f, i, i + f, i + f2);
                    }
                };
            } else if ("upper4".equals(name)) {
                function = x -> {
                    final int f = x.length >>> 2;
                    final int f2 = f + f;
                    for (int i = f; i < f2; i++) {
                        Sorting.upperMedian4(x, i - f, i, i + f, i + f2);
                    }
                };
            } else if ("upper4c".equals(name)) {
                function = x -> {
                    final int f = x.length >>> 2;
                    final int f2 = f + f;
                    for (int i = f; i < f2; i++) {
                        Sorting.upperMedian4c(x, i - f, i, i + f, i + f2);
                    }
                };
            } else if ("upper4d".equals(name)) {
                function = x -> {
                    final int f = x.length >>> 2;
                    final int f2 = f + f;
                    for (int i = f; i < f2; i++) {
                        Sorting.upperMedian4d(x, i - f, i, i + f, i + f2);
                    }
                };
            } else if ("sort4".equals(name)) {
                function = x -> {
                    final int f = x.length >>> 2;
                    final int f2 = f + f;
                    for (int i = f; i < f2; i++) {
                        Sorting.sort4(x, i - f, i, i + f, i + f2);
                    }
                };
            } else {
                throw new IllegalStateException("Unknown median4 function: " + name);
            }
        }
    }

    /**
     * Source of a function to compute the median of 3 points.
     */
    @State(Scope.Benchmark)
    public static class Median3FunctionSource {
        /** Name of the source. */
        @Param({"sort3", "sort3b", "sort3c"})
        private String name;

        /** The action. */
        private Consumer<double[]> function;

        /**
         * @return the function
         */
        public Consumer<double[]> getFunction() {
            return function;
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            Objects.requireNonNull(name);
            // Note: We run this across the entire input array to simulate a pass
            // of the quickselect adaptive algorithm.
            if ("sort3".equals(name)) {
                function = x -> {
                    final int f = x.length / 3;
                    final int f2 = f + f;
                    for (int i = f; i < f2; i++) {
                        Sorting.sort3(x, i - f, i, i + f);
                    }
                };
            } else if ("sort3b".equals(name)) {
                function = x -> {
                    final int f = x.length / 3;
                    final int f2 = f + f;
                    for (int i = f; i < f2; i++) {
                        Sorting.sort3b(x, i - f, i, i + f);
                    }
                };
            } else if ("sort3c".equals(name)) {
                function = x -> {
                    final int f = x.length / 3;
                    final int f2 = f + f;
                    for (int i = f; i < f2; i++) {
                        Sorting.sort3c(x, i - f, i, i + f);
                    }
                };
            } else {
                throw new IllegalStateException("Unknown sort3 function: " + name);
            }
        }
    }

    /**
     * Source of a k-th selector function for {@code double} data.
     */
    @State(Scope.Benchmark)
    public static class DoubleKFunctionSource {
        /** Name of the source. */
        @Param({SORT + JDK, SPH,
            SP, BM, SBM,
            DP, DP5,
            SBM2,
            ISP, IDP,
            LSP, LINEAR, SELECT})
        private String name;

        /** Override of minimum quickselect size. */
        @Param({"0"})
        private int qs;

        /** Override of minimum edgeselect constant. */
        @Param({"0"})
        private int ec;

        /** The action. */
        private BiFunction<double[], int[], double[]> function;

        /**
         * @return the function
         */
        public BiFunction<double[], int[], double[]> getFunction() {
            return function;
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            Objects.requireNonNull(name);
            // Note: For parity in the test, each partition method that accepts the keys as any array
            // receives a clone of the indices.
            if (name.equals(BASELINE)) {
                function = (data, indices) -> extractIndices(data, indices.clone());
            } else  if (name.startsWith(SORT)) {
                // Sort variants (do not clone the keys)
                if (name.contains(ISP)) {
                    final Partition part = PartitionFactory.createPartition(name.substring(SORT.length()), ISP, qs, ec);
                    function = (data, indices) -> {
                        part.sortISP(data);
                        return extractIndices(data, indices);
                    };
                } else if (name.contains(IDP)) {
                    final Partition part = PartitionFactory.createPartition(name.substring(SORT.length()), IDP, qs, ec);
                    function = (data, indices) -> {
                        part.sortIDP(data);
                        return extractIndices(data, indices);
                    };
                } else if (name.contains(JDK)) {
                    function = (data, indices) -> {
                        Arrays.sort(data);
                        return extractIndices(data, indices);
                    };
                }
            // First generation kth-selector functions
            } else if (name.startsWith(SPH)) {
                // Ported CM implementation with a heap
                final KthSelector selector = PartitionFactory.createKthSelector(name, SPH, qs);
                function = (data, indices) -> {
                    final int n = indices.length;
                    // Note: Pivots heap is not optimal here but should be enough for most cases.
                    // The size matches that in the Commons Math Percentile class
                    final int[] pivots = n <= 1 ?
                        KthSelector.NO_PIVOTS :
                        new int[1023];
                    final double[] x = new double[indices.length];
                    for (int i = 0; i < indices.length; i++) {
                        x[i] = selector.selectSPH(data, pivots, indices[i], null);
                    }
                    return x;
                };
            // The following methods clone the indices to avoid destructive modification
            } else if (name.startsWith(SPN)) {
                final KthSelector selector = PartitionFactory.createKthSelector(name, SPN, qs);
                function = (data, indices) -> {
                    selector.partitionSPN(data, indices.clone());
                    return extractIndices(data, indices);
                };
            } else if (name.startsWith(SP)) {
                final KthSelector selector = PartitionFactory.createKthSelector(name, SP, qs);
                function = (data, indices) -> {
                    selector.partitionSP(data, indices.clone());
                    return extractIndices(data, indices);
                };
            } else if (name.startsWith(BM)) {
                final KthSelector selector = PartitionFactory.createKthSelector(name, BM, qs);
                function = (data, indices) -> {
                    selector.partitionBM(data, indices.clone());
                    return extractIndices(data, indices);
                };
            } else if (name.startsWith(SBM)) {
                final KthSelector selector = PartitionFactory.createKthSelector(name, SBM, qs);
                function = (data, indices) -> {
                    selector.partitionSBM(data, indices.clone());
                    return extractIndices(data, indices);
                };
            } else if (name.startsWith(DP)) {
                final KthSelector selector = PartitionFactory.createKthSelector(name, DP, qs);
                function = (data, indices) -> {
                    selector.partitionDP(data, indices.clone());
                    return extractIndices(data, indices);
                };
            } else if (name.startsWith(DP5)) {
                final KthSelector selector = PartitionFactory.createKthSelector(name, DP5, qs);
                function = (data, indices) -> {
                    selector.partitionDP5(data, indices.clone());
                    return extractIndices(data, indices);
                };
            // Second generation partition function with configurable key strategy
            } else if (name.startsWith(SBM2)) {
                final Partition part = PartitionFactory.createPartition(name, SBM2, qs, ec);
                function = (data, indices) -> {
                    part.partitionSBM(data, indices.clone(), indices.length);
                    return extractIndices(data, indices);
                };
            // Floyd-Rivest partition functions
            } else if (name.startsWith(FR)) {
                final Partition part = PartitionFactory.createPartition(name, FR, qs, ec);
                function = (data, indices) -> {
                    part.partitionFR(data, indices.clone(), indices.length);
                    return extractIndices(data, indices);
                };
            } else if (name.startsWith(KFR)) {
                final Partition part = PartitionFactory.createPartition(name, KFR, qs, ec);
                function = (data, indices) -> {
                    part.partitionKFR(data, indices.clone(), indices.length);
                    return extractIndices(data, indices);
                };
            // Introselect implementations which are highly configurable
            } else if (name.startsWith(ISP)) {
                final Partition part = PartitionFactory.createPartition(name, ISP, qs, ec);
                function = (data, indices) -> {
                    part.partitionISP(data, indices.clone(), indices.length);
                    return extractIndices(data, indices);
                };
            } else if (name.startsWith(IDP)) {
                final Partition part = PartitionFactory.createPartition(name, IDP, qs, ec);
                function = (data, indices) -> {
                    part.partitionIDP(data, indices.clone(), indices.length);
                    return extractIndices(data, indices);
                };
            } else if (name.startsWith(SELECT)) {
                // Not configurable
                function = (data, indices) -> {
                    Selection.select(data, indices.clone());
                    return extractIndices(data, indices);
                };
            // Linearselect (median-of-medians) implementation (stopper for quickselect)
            } else if (name.startsWith(LSP)) {
                final Partition part = PartitionFactory.createPartition(name, LSP, qs, ec);
                function = (data, indices) -> {
                    part.partitionLSP(data, indices.clone(), indices.length);
                    return extractIndices(data, indices);
                };
            // Linearselect (optimised median-of-medians) implementation (stopper for quickselect)
            } else if (name.startsWith(LINEAR)) {
                final Partition part = PartitionFactory.createPartition(name, LINEAR, qs, ec);
                function = (data, indices) -> {
                    part.partitionLinear(data, indices.clone(), indices.length);
                    return extractIndices(data, indices);
                };
            } else if (name.startsWith(QA2)) {
                // Configurable only by static properties.
                // Default to FR sampling for the initial mode.
                final int mode = PartitionFactory.getControlFlags(new String[] {name}, -1);
                final int inc = PartitionFactory.getOptionFlags(new String[] {name}, 1);
                Partition.configureQaAdaptive(mode, inc);
                function = (data, indices) -> {
                    Partition.partitionQA2(data, indices.clone(), indices.length);
                    return extractIndices(data, indices);
                };
            } else if (name.startsWith(QA)) {
                final Partition part = PartitionFactory.createPartition(name, QA, qs, ec);
                function = (data, indices) -> {
                    part.partitionQA(data, indices.clone(), indices.length);
                    return extractIndices(data, indices);
                };
            // Heapselect implementation (stopper for quickselect)
            } else if (name.startsWith(HEAP_SELECT)) {
                function = (data, indices) -> {
                    int min = indices[indices.length - 1];
                    int max = min;
                    for (int i = indices.length - 1; --i >= 0;) {
                        min = Math.min(min, indices[i]);
                        max = Math.max(max, indices[i]);
                    }
                    Partition.heapSelectRange(data, 0, data.length - 1, min, max);
                    return extractIndices(data, indices);
                };
            }
            if (function == null) {
                throw new IllegalStateException("Unknown selector function: " + name);
            }
        }

        /**
         * Extract the data at the specified indices.
         *
         * @param data Data.
         * @param indices Indices.
         * @return the data
         */
        private static double[] extractIndices(double[] data, int[] indices) {
            final double[] x = new double[indices.length];
            for (int i = 0; i < indices.length; i++) {
                x[i] = data[indices[i]];
            }
            return x;
        }
    }

    /**
     * Source of a k-th selector function for {@code int} data.
     */
    @State(Scope.Benchmark)
    public static class IntKFunctionSource {
        /** Name of the source. */
        @Param({SORT + JDK, SELECT})
        private String name;

        /** The action. */
        private BiFunction<int[], int[], int[]> function;

        /**
         * @return the function
         */
        public BiFunction<int[], int[], int[]> getFunction() {
            return function;
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            Objects.requireNonNull(name);
            // Note: Always clone the indices
            if (name.equals(BASELINE)) {
                function = (data, indices) -> extractIndices(data, indices.clone());
            } else  if (name.startsWith(SORT)) {
                function = (data, indices) -> {
                    Arrays.sort(data);
                    return extractIndices(data, indices.clone());
                };
            } else if (name.startsWith(SELECT)) {
                function = (data, indices) -> {
                    Selection.select(data, indices.clone());
                    return extractIndices(data, indices);
                };
            }
            if (function == null) {
                throw new IllegalStateException("Unknown int selector function: " + name);
            }
        }

        /**
         * Extract the data at the specified indices.
         *
         * @param data Data.
         * @param indices Indices.
         * @return the data
         */
        private static int[] extractIndices(int[] data, int[] indices) {
            final int[] x = new int[indices.length];
            for (int i = 0; i < indices.length; i++) {
                x[i] = data[indices[i]];
            }
            return x;
        }
    }

    /**
     * Source of a function that pre-processes NaN and signed zeros (-0.0).
     *
     * <p>Detection of signed zero using direct conversion of raw bits and
     * comparison with the bit representation is noticeably faster than comparison
     * using {@code == 0.0}.
     */
    @State(Scope.Benchmark)
    public static class SortNaNFunctionSource {
        /** Name of the source. */
        @Param({"RawZeroNaN", "ZeroSignNaN", "NaNRawZero", "NaNZeroSign"})
        private String name;

        /** The action. */
        private BiConsumer<double[], Blackhole> function;

        /**
         * @return the function
         */
        public BiConsumer<double[], Blackhole> getFunction() {
            return function;
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            // Functions sort NaN and detect signed zeros.
            // For convenience the function accepts the blackhole to handle any
            // output from the processing.
            if ("RawZeroNaN".equals(name)) {
                function = (a, bh) -> {
                    int cn = 0;
                    int end = a.length;
                    for (int i = end; --i >= 0;) {
                        final double v = a[i];
                        if (Double.doubleToRawLongBits(v) == Long.MIN_VALUE) {
                            cn++;
                            a[i] = 0.0;
                        } else if (v != v) {
                            a[i] = a[--end];
                            a[end] = v;
                        }
                    }
                    bh.consume(cn);
                    bh.consume(end);
                };
            } else if ("ZeroSignNaN".equals(name)) {
                function = (a, bh) -> {
                    int cn = 0;
                    int end = a.length;
                    for (int i = end; --i >= 0;) {
                        final double v = a[i];
                        if (v == 0.0 && Double.doubleToRawLongBits(v) < 0) {
                            cn++;
                            a[i] = 0.0;
                        } else if (v != v) {
                            a[i] = a[--end];
                            a[end] = v;
                        }
                    }
                    bh.consume(cn);
                    bh.consume(end);
                };
            } else if ("NaNRawZero".equals(name)) {
                function = (a, bh) -> {
                    int cn = 0;
                    int end = a.length;
                    for (int i = end; --i >= 0;) {
                        final double v = a[i];
                        if (v != v) {
                            a[i] = a[--end];
                            a[end] = v;
                        } else if (Double.doubleToRawLongBits(v) == Long.MIN_VALUE) {
                            cn++;
                            a[i] = 0.0;
                        }
                    }
                    bh.consume(cn);
                    bh.consume(end);
                };
            } else if ("NaNZeroSign".equals(name)) {
                function = (a, bh) -> {
                    int cn = 0;
                    int end = a.length;
                    for (int i = end; --i >= 0;) {
                        final double v = a[i];
                        if (v != v) {
                            a[i] = a[--end];
                            a[end] = v;
                        } else if (v == 0.0 && Double.doubleToRawLongBits(v) < 0) {
                            cn++;
                            a[i] = 0.0;
                        }
                    }
                    bh.consume(cn);
                    bh.consume(end);
                };
            } else {
                throw new IllegalStateException("Unknown sort NaN function: " + name);
            }
        }
    }

    /**
     * Source of a edge selector function. This is a function that selects indices
     * that are clustered close to the edge of the data.
     *
     * <p>This is a specialised class to allow benchmarking the switch from using
     * quickselect partitioning to using edgeselect.
     */
    @State(Scope.Benchmark)
    public static class EdgeFunctionSource {
        /** Name of the source.
         * For introselect methods this should effectively turn-off edgeselect. */
        @Param({HEAP_SELECT, ISP + "_EC0", IDP + "_EC0",
            // Only use for small length as sort insertion is worst case Order(k * (right - left))
            // vs heap select() is O(k - left) + O((right - k) * log(k - left))
            //SORT_SELECT
            })
        private String name;

        /** The action. */
        private BiFunction<double[], int[], double[]> function;

        /**
         * @return the function
         */
        public BiFunction<double[], int[], double[]> getFunction() {
            return function;
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            Objects.requireNonNull(name);
            // Direct use of heapselect. This has variations which use different
            // optimisations for small heaps.
            // Note: Optimisation for small heap size (n=1,2) is not observable on large data.
            // It requires the use of small data (e.g. len=[16, 32)) to observe differences.
            // The main overhead is the test for insertion against the current top of the
            // heap which grows increasingly unlikely as the range is scanned.
            // Optimisation for n=1 is negligible; for n=2 it is up to 10%. However using only
            // heapSelectRange2 is not as fast as the non-optimised heapSelectRange0
            // when the heap is size 1. For n=1 the heap insertion branch prediction
            // can learn the heap has no children and skip descending the heap, whereas
            // heap size n=2 can descend 1 level if the child is smaller/bigger. This is not
            // as fast as dedicated code for the single child case.
            // This benchmark requires repeating with variable heap size to avoid branch
            // prediction learning what to do, i.e. use with an index source that has variable
            // distance from the edge.
            if (HEAP_SELECT.equals(name)) {
                function = (data, indices) -> {
                    heapSelectRange0(data, 0, data.length - 1, indices[0], indices[1]);
                    return extractIndices(data, indices[0], indices[1]);
                };
            } else if ((HEAP_SELECT + "1").equals(name)) {
                function = (data, indices) -> {
                    heapSelectRange1(data, 0, data.length - 1, indices[0], indices[1]);
                    return extractIndices(data, indices[0], indices[1]);
                };
            } else if ((HEAP_SELECT + "2").equals(name)) {
                function = (data, indices) -> {
                    heapSelectRange2(data, 0, data.length - 1, indices[0], indices[1]);
                    return extractIndices(data, indices[0], indices[1]);
                };
            } else if ((HEAP_SELECT + "12").equals(name)) {
                function = (data, indices) -> {
                    heapSelectRange12(data, 0, data.length - 1, indices[0], indices[1]);
                    return extractIndices(data, indices[0], indices[1]);
                };
            // Only use on small edge as insertion is Order(k)
            } else if (SORT_SELECT.equals(name)) {
                function = (data, indices) -> {
                    Partition.sortSelectRange(data, 0, data.length - 1, indices[0], indices[1]);
                    return extractIndices(data, indices[0], indices[1]);
                };
            // Introselect methods - these should be configured to not use edgeselect.
            // These directly call the introselect method to skip NaN/signed zero processing.
            } else if (name.startsWith(ISP)) {
                final Partition part = PartitionFactory.createPartition(name, ISP);
                function = (data, indices) -> {
                    part.introselect(part.getSPFunction(), data,
                        0, data.length - 1, IndexIntervals.interval(indices[0], indices[1]), 10000);
                    return extractIndices(data, indices[0], indices[1]);
                };
            } else if (name.startsWith(IDP)) {
                final Partition part = PartitionFactory.createPartition(name, IDP);
                function = (data, indices) -> {
                    part.introselect(Partition::partitionDP, data,
                        0, data.length - 1, IndexIntervals.interval(indices[0], indices[1]), 10000);
                    return extractIndices(data, indices[0], indices[1]);
                };
            } else {
                throw new IllegalStateException("Unknown edge selector function: " + name);
            }
        }

        /**
         * Partition the elements between {@code ka} and {@code kb} using a heap select
         * algorithm. It is assumed {@code left <= ka <= kb <= right}.
         *
         * <p>Note:
         *
         * <p>This is a copy of {@link Partition#heapSelectRange(double[], int, int, int, int)}.
         * It uses no optimised versions for small heaps.
         *
         * @param a Data array to use to find out the K<sup>th</sup> value.
         * @param left Lower bound (inclusive).
         * @param right Upper bound (inclusive).
         * @param ka Lower index to select.
         * @param kb Upper index to select.
         */
        static void heapSelectRange0(double[] a, int left, int right, int ka, int kb) {
            if (right - left < Partition.MIN_HEAPSELECT_SIZE) {
                Sorting.sort(a, left, right);
                return;
            }
            if (kb - left < right - ka) {
                Partition.heapSelectLeft(a, left, right, kb, kb - ka);
            } else {
                Partition.heapSelectRight(a, left, right, ka, kb - ka);
            }
        }

        /**
         * Partition the elements between {@code ka} and {@code kb} using a heap select
         * algorithm. It is assumed {@code left <= ka <= kb <= right}.
         *
         * <p>Note:
         *
         * <p>This is a copy of {@link Partition#heapSelectRange(double[], int, int, int, int)}.
         * It uses no optimised versions for small heap of size 1.
         *
         * @param a Data array to use to find out the K<sup>th</sup> value.
         * @param left Lower bound (inclusive).
         * @param right Upper bound (inclusive).
         * @param ka Lower index to select.
         * @param kb Upper index to select.
         */
        static void heapSelectRange1(double[] a, int left, int right, int ka, int kb) {
            if (right - left < Partition.MIN_HEAPSELECT_SIZE) {
                Sorting.sort(a, left, right);
                return;
            }
            if (kb - left < right - ka) {
                // Optimise
                if (kb == left) {
                    Partition.selectMinIgnoreZeros(a, left, right);
                } else {
                    Partition.heapSelectLeft(a, left, right, kb, kb - ka);
                }
            } else {
                // Optimise
                if (ka == right) {
                    Partition.selectMaxIgnoreZeros(a, left, right);
                } else {
                    Partition.heapSelectRight(a, left, right, ka, kb - ka);
                }
            }
        }

        /**
         * Partition the elements between {@code ka} and {@code kb} using a heap select
         * algorithm. It is assumed {@code left <= ka <= kb <= right}.
         *
         * <p>Note:
         *
         * <p>This is a copy of {@link Partition#heapSelectRange(double[], int, int, int, int)}.
         * It uses optimised versions for small heap of size 2.
         *
         * @param a Data array to use to find out the K<sup>th</sup> value.
         * @param left Lower bound (inclusive).
         * @param right Upper bound (inclusive).
         * @param ka Lower index to select.
         * @param kb Upper index to select.
         */
        static void heapSelectRange2(double[] a, int left, int right, int ka, int kb) {
            if (right - left < Partition.MIN_HEAPSELECT_SIZE) {
                Sorting.sort(a, left, right);
                return;
            }
            if (kb - left < right - ka) {
                // Optimise
                if (kb - 1 <= left) {
                    Partition.selectMin2IgnoreZeros(a, left, right);
                } else {
                    Partition.heapSelectLeft(a, left, right, kb, kb - ka);
                }
            } else {
                // Optimise
                if (ka + 1 >= right) {
                    Partition.selectMax2IgnoreZeros(a, left, right);
                } else {
                    Partition.heapSelectRight(a, left, right, ka, kb - ka);
                }
            }
        }

        /**
         * Partition the elements between {@code ka} and {@code kb} using a heap select
         * algorithm. It is assumed {@code left <= ka <= kb <= right}.
         *
         * <p>Note:
         *
         * <p>This is a copy of {@link Partition#heapSelectRange(double[], int, int, int, int)}.
         * It uses optimised versions for small heap of size 1 and 2.
         *
         * @param a Data array to use to find out the K<sup>th</sup> value.
         * @param left Lower bound (inclusive).
         * @param right Upper bound (inclusive).
         * @param ka Lower index to select.
         * @param kb Upper index to select.
         */
        static void heapSelectRange12(double[] a, int left, int right, int ka, int kb) {
            if (right - left < Partition.MIN_HEAPSELECT_SIZE) {
                Sorting.sort(a, left, right);
                return;
            }
            if (kb - left < right - ka) {
                // Optimise
                if (kb - 1 <= left) {
                    if (kb == left) {
                        Partition.selectMinIgnoreZeros(a, left, right);
                    } else {
                        Partition.selectMin2IgnoreZeros(a, left, right);
                    }
                } else {
                    Partition.heapSelectLeft(a, left, right, kb, kb - ka);
                }
            } else {
                // Optimise
                if (ka + 1 >= right) {
                    if (ka == right) {
                        Partition.selectMaxIgnoreZeros(a, left, right);
                    } else {
                        Partition.selectMax2IgnoreZeros(a, left, right);
                    }
                } else {
                    Partition.heapSelectRight(a, left, right, ka, kb - ka);
                }
            }
        }

        /**
         * Extract the data at the specified indices.
         *
         * @param data Data.
         * @param l Lower bound (inclusive).
         * @param r Upper bound (inclusive).
         * @return the data
         */
        private static double[] extractIndices(double[] data, int l, int r) {
            final double[] x = new double[r - l + 1];
            for (int i = l; i <= r; i++) {
                x[i - l] = data[i];
            }
            return x;
        }
    }

    /**
     * Source of an search function. This is a function that find an index
     * in a sorted list of indices, e.g. a binary search.
     */
    @State(Scope.Benchmark)
    public static class IndexSearchFunctionSource {
        /** Name of the source. */
        @Param({"Binary",
            //"binarySearch",
            "Scan"})
        private String name;

        /** The action. */
        private SearchFunction function;

        /**
         * Define a search function.
         */
        public interface SearchFunction {
            /**
             * Find the index of the element {@code k}, or the closest index
             * to the element (implementation definitions may vary).
             *
             * @param a Data.
             * @param k Element.
             * @return the index
             */
            int find(int[] a, int k);
        }

        /**
         * @return the function
         */
        public SearchFunction getFunction() {
            return function;
        }

        /**
         * Create the function.
         */
        @Setup
        public void setup() {
            Objects.requireNonNull(name);
            if ("Binary".equals(name)) {
                function = (keys, k) -> Partition.searchLessOrEqual(keys, 0, keys.length - 1, k);
            } else if ("binarySearch".equals(name)) {
                function = (keys, k) -> Arrays.binarySearch(keys, 0, keys.length, k);
            } else if ("Scan".equals(name)) {
                function = (keys, k) -> {
                    // Assume that k >= keys[0]
                    int i = keys.length;
                    do {
                        --i;
                    } while (keys[i] > k);
                    return i;
                };
            } else {
                throw new IllegalStateException("Unknown index search function: " + name);
            }
        }
    }

    /**
     * Benchmark a sort on the data.
     *
     * @param function Source of the function.
     * @param source Source of the data.
     * @param bh Data sink.
     */
    @Benchmark
    public void sort(SortFunctionSource function, SortSource source, Blackhole bh) {
        final int size = source.size();
        final Consumer<double[]> fun = function.getFunction();
        for (int j = -1; ++j < size;) {
            final double[] y = source.getData(j);
            fun.accept(y);
            bh.consume(y);
        }
    }

    /**
     * Benchmark a sort of 5 data values.
     * This tests the pivot selection from 5 values used in dual-pivot partitioning.
     *
     * @param function Source of the function.
     * @param source Source of the data.
     * @param bh Data sink.
     */
    @Benchmark
    public void fiveSort(Sort5FunctionSource function, SortSource source, Blackhole bh) {
        final int size = source.size();
        final Consumer<double[]> fun = function.getFunction();
        for (int j = -1; ++j < size;) {
            final double[] y = source.getData(j);
            fun.accept(y);
            bh.consume(y);
        }
    }

    /**
     * Benchmark a pass over the entire data computing the medians of 4 data values.
     * This simulates a QuickselectAdaptive step.
     *
     * @param function Source of the function.
     * @param source Source of the data.
     * @param bh Data sink.
     */
    @Benchmark
    public void fourMedian(Median4FunctionSource function, SortSource source, Blackhole bh) {
        final int size = source.size();
        final Consumer<double[]> fun = function.getFunction();
        for (int j = -1; ++j < size;) {
            final double[] y = source.getData(j);
            fun.accept(y);
            bh.consume(y);
        }
    }

    /**
     * Benchmark a pass over the entire data computing the medians of 3 data values.
     * This simulates a QuickselectAdaptive step.
     *
     * @param function Source of the function.
     * @param source Source of the data.
     * @param bh Data sink.
     */
    @Benchmark
    public void threeMedian(Median3FunctionSource function, SortSource source, Blackhole bh) {
        final int size = source.size();
        final Consumer<double[]> fun = function.getFunction();
        for (int j = -1; ++j < size;) {
            final double[] y = source.getData(j);
            fun.accept(y);
            bh.consume(y);
        }
    }

    /**
     * Benchmark partitioning using k partition indices.
     *
     * @param function Source of the function.
     * @param source Source of the data.
     * @param bh Data sink.
     */
    @Benchmark
    public void doublePartition(DoubleKFunctionSource function, KSource source, Blackhole bh) {
        final int size = source.size();
        final BiFunction<double[], int[], double[]> fun = function.getFunction();
        for (int j = -1; ++j < size;) {
            // Note: This uses the indices without cloning. This is because some
            // functions do not destructively modify the data.
            bh.consume(fun.apply(source.getData(j), source.getIndices(j)));
        }
    }

    /**
     * Benchmark partitioning using k partition indices on {@code int} data.
     *
     * @param function Source of the function.
     * @param source Source of the data.
     * @param bh Data sink.
     */
    @Benchmark
    public void intPartition(IntKFunctionSource function, KSource source, Blackhole bh) {
        final int size = source.size();
        final BiFunction<int[], int[], int[]> fun = function.getFunction();
        for (int j = -1; ++j < size;) {
            // Note: This uses the indices without cloning. This is because some
            // functions do not destructively modify the data.
            bh.consume(fun.apply(source.getIntData(j), source.getIndices(j)));
        }
    }

    /**
     * Benchmark partitioning of an interval of indices a set distance from the edge.
     * This is used to benchmark the switch from quickselect partitioning to edgeselect.
     *
     * @param function Source of the function.
     * @param source Source of the data.
     * @param bh Data sink.
     */
    @Benchmark
    public void edgeSelect(EdgeFunctionSource function, EdgeSource source, Blackhole bh) {
        final int size = source.size();
        final BiFunction<double[], int[], double[]> fun = function.getFunction();
        for (int j = -1; ++j < size;) {
            bh.consume(fun.apply(source.getData(j), source.getIndices(j)));
        }
    }

    /**
     * Benchmark pre-processing of NaN and signed zeros (-0.0).
     *
     * @param function Source of the function.
     * @param source Source of the data.
     * @param bh Data sink.
     */
    @Benchmark
    public void nanZero(SortNaNFunctionSource function, SortSource source, Blackhole bh) {
        final int size = source.size();
        final BiConsumer<double[], Blackhole> fun = function.getFunction();
        for (int j = -1; ++j < size;) {
            fun.accept(source.getData(j), bh);
        }
    }

    /**
     * Benchmark the search of an ordered set of indices.
     *
     * @param function Source of the search.
     * @param source Source of the data.
     * @return value to consume
     */
    @Benchmark
    public long indexSearch(IndexSearchFunctionSource function, SplitIndexSource source) {
        final IndexSearchFunctionSource.SearchFunction fun = function.getFunction();
        // Ensure we have something to consume during the benchmark
        long sum = 0;
        for (int i = source.samples(); --i >= 0;) {
            // Single point in the range
            sum += fun.find(source.getIndices(i), source.getPoint(i));
        }
        return sum;
    }

    /**
     * Benchmark the tracking of an interval of indices during a partition algorithm.
     *
     * <p>The {@link SearchableInterval} is created for the indices of interest. These are then
     * cut at all points in the interval between indices to simulate a partition algorithm
     * dividing the data and requiring a new interval to use in each part:
     * <pre>{@code
     *            cut
     *             |
     * -------k1--------k2---------k3---- ... ---------kn--------
     *          <-- scan previous
     *    scan next -->
     * }</pre>
     *
     * <p>Note: If a cut is made in the interval then the smallest region of data
     * that was most recently partitioned was the length between the two flanking k.
     * This involves a full scan (and partitioning) over the data of length (k2 - k1).
     * A BitSet-type structure will require a scan over 1/64 of this length of data
     * to find the next and previous index from a cut point. In practice
     * the interval may be partitioned over a much larger length, e.g. (kn - k1).
     * Thus the length of time for the partition algorithm is expected to be at least
     * 64x the length of time for the BitSet-type scan. The disadvantage of the
     * BitSet-type structure is memory consumption. For a small number of keys the
     * structures that search the entire set of keys are fast enough. At very high
     * density the BitSet-type structures are preferred.
     *
     * @param function Source of the interval.
     * @param source Source of the data.
     * @return value to consume
     */
    @Benchmark
    public long searchableIntervalNextPrevious(SearchableIntervalSource function, SplitIndexSource source) {
        final int[][] indices = source.getIndices();
        final int[][] points = source.getPoints();
        // Ensure we have something to consume during the benchmark
        long sum = 0;
        for (int i = 0; i < indices.length; i++) {
            final int[] x = indices[i];
            final int[] p = points[i];
            final SearchableInterval interval = function.create(x);
            for (final int k : p) {
                sum += interval.nextIndex(k);
                sum += interval.previousIndex(k);
            }
        }
        return sum;
    }

    /**
     * Benchmark the tracking of an interval of indices during a partition algorithm.
     *
     * <p>This is similar to
     * {@link #searchableIntervalNextPrevious(SearchableIntervalSource, SplitIndexSource)}.
     * It uses the {@link SearchableInterval#split(int, int, int[])} method. This requires
     * {@code k} to be in an open interval. Some modes of the {@link IndexSource} do not
     * ensure that {@code left < k < right} for all split points so we have to check this
     * before calling the split method (it is a fixed overhead for the benchmark).
     *
     * @param function Source of the interval.
     * @param source Source of the data.
     * @return value to consume
     */
    @Benchmark
    public long searchableIntervalSplit(SearchableIntervalSource function, SplitIndexSource source) {
        final int[][] indices = source.getIndices();
        final int[][] points = source.getPoints();
        // Ensure we have something to consume during the benchmark
        long sum = 0;
        final int[] bound = {0};
        for (int i = 0; i < indices.length; i++) {
            final int[] x = indices[i];
            final int[] p = points[i];
            // Note: A partition algorithm would only call split if there are indices
            // above and below the split point.
            final SearchableInterval interval = function.create(x);
            final int left = interval.left();
            final int right = interval.right();
            for (final int k : p) {
                // Check k is in the open interval (left, right)
                if (left < k && k < right) {
                    sum += interval.split(k, k, bound);
                    sum += bound[0];
                }
            }
        }
        return sum;
    }

    /**
     * Benchmark the creation of an interval of indices for controlling a partition
     * algorithm.
     *
     * <p>This baselines the
     * {@link #searchableIntervalNextPrevious(SearchableIntervalSource, SplitIndexSource)}
     * benchmark. For the BitSet-type structures a large overhead is the memory allocation
     * to create the {@link SearchableInterval}. Note that this will be at most 1/64 the
     * size of the array that is being partitioned and in practice this overhead is not
     * significant.
     *
     * @param function Source of the interval.
     * @param source Source of the data.
     * @param bh Data sink.
     */
    @Benchmark
    public void createSearchableInterval(SearchableIntervalSource function, IndexSource source, Blackhole bh) {
        final int[][] indices = source.getIndices();
        for (final int[] x : indices) {
            bh.consume(function.create(x));
        }
    }

    /**
     * Benchmark the splitting of an interval of indices during a partition algorithm.
     *
     * <p>This is similar to
     * {@link #searchableIntervalSplit(SearchableIntervalSource, SplitIndexSource)}. It
     * uses the {@link UpdatingInterval#splitLeft(int, int)} method by recursive division
     * of the indices.
     *
     * @param function Source of the interval.
     * @param source Source of the data.
     * @param bh Data sink.
     */
    @Benchmark
    public void updatingIntervalSplit(UpdatingIntervalSource function, IndexSource source, Blackhole bh) {
        final int[][] indices = source.getIndices();
        final int s = source.getMinSeparation();
        for (int i = 0; i < indices.length; i++) {
            split(function.create(indices[i]), s, bh);
        }
    }

    /**
     * Recursively split the interval until the length is below the provided separation.
     * Consume the interval when no more divides can occur. Simulates a single-pivot
     * partition algorithm.
     *
     * @param interval Interval.
     * @param s Minimum separation between left and right.
     * @param bh Data sink.
     */
    private static void split(UpdatingInterval interval, int s, Blackhole bh) {
        int l = interval.left();
        final int r = interval.right();
        // Note: A partition algorithm would only call split if there are indices
        // above and below the split point.
        if (r - l > s) {
            final int middle = (l + r) >>> 1;
            // recurse left
            split(interval.splitLeft(middle, middle), s, bh);
            // continue on right side
            l = interval.left();
        }
        bh.consume(interval);
    }
}
