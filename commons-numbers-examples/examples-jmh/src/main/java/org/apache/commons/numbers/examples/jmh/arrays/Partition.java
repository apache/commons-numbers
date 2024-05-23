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

import java.util.Arrays;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;

/**
 * Partition array data.
 *
 * <p>Arranges elements such that indices {@code k} correspond to their correctly
 * sorted value in the equivalent fully sorted array. For all indices {@code k}
 * and any index {@code i}:
 *
 * <pre>{@code
 * data[i < k] <= data[k] <= data[k < i]
 * }</pre>
 *
 * <p>Examples:
 *
 * <pre>
 * data    [0, 1, 2, 1, 2, 5, 2, 3, 3, 6, 7, 7, 7, 7]
 *
 *
 * k=4   : [0, 1, 2, 1], [2], [5, 2, 3, 3, 6, 7, 7, 7, 7]
 * k=4,8 : [0, 1, 2, 1], [2], [3, 3, 2], [5], [6, 7, 7, 7, 7]
 * </pre>
 *
 * <p>Note: Unless otherwise stated, methods in this class require that the floating-point data
 * contains no NaN values; ordering does not respect the order of signed zeros imposed by
 * {@link Double#compare(double, double)}.
 *
 * <p>References
 *
 * <p>Quickselect is introduced in Hoare [1]. This selects an element {@code k} from {@code n}
 * using repeat division of the data around a partition element, recursing into the
 * partition that contains {@code k}.
 *
 * <p>Introselect is introduced in Musser [2]. This detects excess recursion in quickselect
 * and reverts to heapselect to achieve an improved worst case bound on selection.
 *
 * <p>Use of dual-pivot quickselect is analysed in Wild et al [3] and shown to require
 * marginally more comparisons than single-pivot quickselect on a uniformly chosen order
 * statistic {@code k} and extremal order statistic (see table 1, page 19). This analysis
 * is reflected in the current implementation where dual-pivot quickselect is marginally
 * slower when {@code k} is close to the end of the data. However the dual-pivot quickselect
 * outperforms single-pivot quickselect when using multiple {@code k}; often significantly
 * when {@code k} or {@code n} are large.
 *
 * <p>Use of sampling to identify a pivot that places {@code k} in the smaller partition is
 * performed in the SELECT algorithm of Floyd and Rivest [4]. The original algorithm partitions
 * on a single pivot. This was extended by Kiwiel to partition using two pivots either side
 * of {@code k} with high probability [5].
 *
 * <p>Confidence bounds for the number of iterations to reduce a partition length by 2<sup>-x</sup>
 * are provided in Valois [6].
 *
 * <p>A worst-case linear time algorithm PICK is described in Blum <i>et al</i> [7]. This uses the
 * median-of-medians as a partition element for selection which ensures a minimum fraction of the
 * elements are eliminated per iteration. This was extended to use an asymmetric pivot choice
 * with efficient reuse of the medians sample in the QuickselectAdpative algorithm of
 * Alexandrescu [8].
 *
 * <ol>
 * <li>
 * Hoare (1961)
 * Algorithm 65: Find
 * <a href="https://doi.org/10.1145%2F366622.366647">Comm. ACM. 4 (7): 321–322</a>
 * <li>
 * Musser (1999)
 * Introspective Sorting and Selection Algorithms
 * <a href="https://doi.org/10.1002/(SICI)1097-024X(199708)27:8%3C983::AID-SPE117%3E3.0.CO;2-%23">
 * Software: Practice and Experience 27, 983-993.</a>
 * <li>
 * Wild, Nebel and Mahmoud (2013)
 * Analysis of Quickselect under Yaroslavskiy's Dual-Pivoting Algorithm
 * <a href="https://doi.org/10.48550/arXiv.1306.3819">arXiv:1306.3819</a>
 * <li>Floyd and Rivest (1975)
 * Algorithm 489: The Algorithm SELECT - for Finding the ith Smallest of n elements.
 * Comm. ACM. 18 (3): 173.
 * <li>Kiwiel (2005)
 * On Floyd and Rivest's SELECT algorithm.
 * <a href="https://doi.org/10.1016/j.tcs.2005.06.032">
 * Theoretical Computer Science 347, 214-238</a>.
 * <li>Valois (2000)
 * Introspective sorting and selection revisited
 * <a href="https://doi.org/10.1002/(SICI)1097-024X(200005)30:6%3C617::AID-SPE311%3E3.0.CO;2-A">
 * Software: Practice and Experience 30, 617-638.</a>
 * <li>Blum, Floyd, Pratt, Rivest, and Tarjan (1973)
 * Time bounds for selection.
 * <a href="https://doi.org/10.1016%2FS0022-0000%2873%2980033-9">
 * Journal of Computer and System Sciences. 7 (4): 448–461</a>.
 * <li>Alexandrescu (2016)
 * Fast Deterministic Selection
 * <a href="https://arxiv.org/abs/1606.00484">arXiv:1606.00484</a>.
 * <li><a href="https://en.wikipedia.org/wiki/Quickselect">Quickselect (Wikipedia)</a>
 * <li><a href="https://en.wikipedia.org/wiki/Introsort">Introsort (Wikipedia)</a>
 * <li><a href="https://en.wikipedia.org/wiki/Introselect">Introselect (Wikipedia)</a>
 * <li><a href="https://en.wikipedia.org/wiki/Median_of_medians">Median of medians (Wikipedia)</a>
 * </ol>
 *
 * @since 1.2
 */
final class Partition {
    // This class contains implementations for use in benchmarking.

    /** Default pivoting strategy. Note: Using the dynamic strategy avoids excess recursion
     * on the Bentley and McIlroy test data vs the MEDIAN_OF_3 strategy. */
    static final PivotingStrategy PIVOTING_STRATEGY = PivotingStrategy.DYNAMIC;
    /**
     * Default pivoting strategy. Choosing from 5 points is unbiased on random data and
     * has a lower standard deviation around the thirds than choosing 2 points
     * (Yaroslavskiy's original method, see {@link DualPivotingStrategy#MEDIANS}). It
     * performs well across various test data.
     *
     * <p>There are 3 variants using spacings of approximately 1/6, 1/7, and 1/8 computed
     * using shifts to create 0.1719, 0.1406, and 0.125; with middle thirds on large
     * lengths of 0.342, 0.28 and 0.25. The spacing using 1/7 is marginally faster when
     * performing a full sort than the others; thus favouring a smaller middle third, but
     * not too small, appears to be most performant.
     */
    static final DualPivotingStrategy DUAL_PIVOTING_STRATEGY = DualPivotingStrategy.SORT_5B;
    /** Minimum selection size for quickselect/quicksort.
     * Below this switch to sortselect/insertion sort rather than selection.
     * Dual-pivot quicksort used 27 in Yaroslavskiy's original paper.
     * Changes to this value are only noticeable when the input array is small.
     *
     * <p>This is a legacy setting from when insertion sort was used as the stopper.
     * This has been replaced by edge selection functions. Using insertion sort
     * is slower as n must be sorted compared to an edge select that only sorts
     * up to n/2 from the edge. It is disabled by default but can be used for
     * benchmarking.
     *
     * <p>If using insertion sort as the stopper for quickselect:
     * <ul>
     * <li>Single-pivot: Benchmarking random data in range [96, 192] suggests a value of ~16 for n=1.
     * <li>Dual-pivot: Benchmarking random data in range [162, 486] suggests a value of ~27 for n=1
     * and increasing with higher n in the same range.
     * Dual-pivot sorting requires a value of ~120. If keys are saturated between k1 and kn
     * an increase to this threshold will gain full sort performance.
     * </ul> */
    static final int MIN_QUICKSELECT_SIZE = 0;
    /** Minimum size for heapselect.
     * Below this switch to insertion sort rather than selection. This is used to avoid
     * heap select on tiny data. */
    static final int MIN_HEAPSELECT_SIZE = 5;
    /** Minimum size for sortselect.
     * Below this switch to insertion sort rather than selection. This is used to avoid
     * sort select on tiny data. */
    static final int MIN_SORTSELECT_SIZE = 4;
    /** Default selection constant for edgeselect. Any k closer than this to the left/right
     * bound will be selected using the configured edge selection function. */
    static final int EDGESELECT_CONSTANT = 20;
    /** Default sort selection constant for linearselect. Note that linear select variants
     * recursively call quickselect so very small lengths are included with an initial
     * medium length. Using lengths of 1023-5 and 2043-53 indicate optimum performance around
     * 80 for median-of-medians when placing the sample on the left. Adaptive linear methods
     * are faster and so this value is reduced. Quickselect adaptive has a value around 20-30.
     * Note: When using {@link ExpandStrategy#T2} the input length must create a sample of at
     * least length 2 as each end of the sample is used as a sentinel. With a sample length of
     * 1/12 of the data this requires edge select of at least 12. */
    static final int LINEAR_SORTSELECT_SIZE = 24;
    /** Default sub-sampling size to identify a single pivot. Sub-sampling is performed if the
     * length is above this value thus using MAX_VALUE sets it off by default.
     * The SELECT algorithm of Floyd-Rivest uses 600. */
    static final int SUBSAMPLING_SIZE = Integer.MAX_VALUE;
    /** Default key strategy. */
    static final KeyStrategy KEY_STRATEGY = KeyStrategy.INDEX_SET;
    /** Default 1 or 2 key strategy. */
    static final PairedKeyStrategy PAIRED_KEY_STRATEGY = PairedKeyStrategy.SEARCHABLE_INTERVAL;
    /** Default recursion multiple. */
    static final int RECURSION_MULTIPLE = 2;
    /** Default recursion constant. */
    static final int RECURSION_CONSTANT = 0;
    /** Default compression. */
    static final int COMPRESSION_LEVEL = 1;
    /** Default control flags. */
    static final int CONTROL_FLAGS = 0;
    /** Default option flags. */
    static final int OPTION_FLAGS = 0;
    /** Default single-pivot partition strategy. */
    static final SPStrategy SP_STRATEGY = SPStrategy.KBM;
    /** Default expand partition strategy. A ternary method is faster on equal elements and no
     * slower on unique elements. */
    static final ExpandStrategy EXPAND_STRATEGY = ExpandStrategy.T2;
    /** Default single-pivot linear select strategy. */
    static final LinearStrategy LINEAR_STRATEGY = LinearStrategy.RSA;
    /** Default edge select strategy. */
    static final EdgeSelectStrategy EDGE_STRATEGY = EdgeSelectStrategy.ESS;
    /** Default single-pivot stopper strategy. */
    static final StopperStrategy STOPPER_STRATEGY = StopperStrategy.SQA;
    /** Default quickselect adaptive mode. */
    static final AdaptMode ADAPT_MODE = AdaptMode.ADAPT3;

    /** Sampling mode using Floyd-Rivest sampling. */
    static final int MODE_FR_SAMPLING = -1;
    /** Sampling mode. */
    static final int MODE_SAMPLING = 0;
    /** No sampling but use adaption of the target k. */
    static final int MODE_ADAPTION = 1;
    /** No sampling and no adaption of target k (strict margins). */
    static final int MODE_STRICT = 2;

    // Floyd-Rivest flags

    /** Control flag for random sampling. */
    static final int FLAG_RANDOM_SAMPLING = 0x2;
    /** Control flag for vector swap of the sample. */
    static final int FLAG_MOVE_SAMPLE = 0x4;
    /** Control flag for random subset sampling. This creates the sample at the end
     * of the data and requires moving regions to reposition around the target k. */
    static final int FLAG_SUBSET_SAMPLING = 0x8;

    // RNG flags

    /** Control flag for biased nextInt(n) RNG. */
    static final int FLAG_BIASED_RANDOM = 0x1000;
    /** Control flag for SplittableRandom RNG. */
    static final int FLAG_SPLITTABLE_RANDOM = 0x2000;
    /** Control flag for MSWS RNG. */
    static final int FLAG_MSWS = 0x4000;

    // Quickselect adaptive flags. Must not clash with the Floyd-Rivest/RNG flags
    // that are supported for sample mode.

    /** Control flag for quickselect adaptive to propagate the no sampling mode recursively. */
    static final int FLAG_QA_PROPAGATE = 0x1;
    /** Control flag for quickselect adaptive variant of Floyd-Rivest random sampling. */
    static final int FLAG_QA_RANDOM_SAMPLING = 0x4;
    /** Control flag for quickselect adaptive to use a different far left/right step
     * using min of 4; then median of 3 into the 2nd 12th-tile. The default (original) uses
     * lower median of 4; then min of 3 into 4th 12th-tile). The default has a larger
     * upper margin of 3/8 vs 1/3 for the new method. The new method is better
     * with the original k mapping for far left/right and similar speed to the original
     * far left/right step using the new k mapping. When sampling is off it is marginally
     * faster, may be due to improved layout of the sample closer to the strict 1/12 lower margin.
     * There is no compelling evidence to indicate is it better so the default uses
     * the original far step method. */
    static final int FLAG_QA_FAR_STEP = 0x8;
    /** Control flag for quickselect adaptive to map k using the same k mapping for all
     * repeated steps. This enables the original algorithm behaviour.
     *
     * <p>Note that the original mapping can create a lower margin that
     * does not contain k. This makes it possible to put k into the larger partition.
     * For the middle and step left methods this heuristic is acceptable as the bias in
     * margins is shifted but the smaller margin is at least 1/12 of the data and a choice
     * of this side is not a severe penalty. For the far step left the original mapping
     * will always create a smaller margin that does not contain k. Removing this
     * adaptive k and using the median of the 12th-tile shows a measurable speed-up
     * as the smaller margin always contains k. This result has been extended to change
     * the mapping for the far step to ensure the smaller
     * margin always contains at least k elements. This is faster and so enabled by default. */
    static final int FLAG_QA_FAR_STEP_ADAPT_ORIGINAL = 0x10;
    /** Use a 12th-tile for the sampling mode in the middle repeated step method.
     * The default uses a 9th-tile which is a larger sample than the 12th-tile used in
     * the step left/far left methods. */
    static final int FLAG_QA_MIDDLE_12 = 0x20;
    /** Position the sample for quickselect adaptive to place the mapped k' at the target index k.
     * This is not possible for the far step methods as it can generated a bounds error as
     * k approaches the edge. */
    static final int FLAG_QA_SAMPLE_K = 0x40;

    /** Threshold to use sub-sampling of the range to identify the single pivot.
     * Sub-sampling uses the Floyd-Rivest algorithm to partition a sample of the data to
     * identify a pivot so that the target element is in the smaller set after partitioning.
     * The original FR paper used 600 otherwise reverted to the target index as the pivot.
     * This implementation uses a sample to identify a median pivot which increases robustness
     * at small size on a variety of data and allows raising the original FR threshold.
     * At 600, FR has no speed-up; at double this the speed-up can be measured. */
    static final int SELECT_SUB_SAMPLING_SIZE = 1200;

    /** Message for an unsupported introselect configuration. */
    private static final String UNSUPPORTED_INTROSELECT = "Unsupported introselect: ";

    /** Transformer factory for double data with the behaviour of a JDK sort.
     * Moves NaN to the end of the data and handles signed zeros. Works on the data in-place. */
    private static final Supplier<DoubleDataTransformer> SORT_TRANSFORMER =
        DoubleDataTransformers.createFactory(NaNPolicy.INCLUDE, false);

    /** Minimum length between 2 pivots {@code p2 - p1} that requires a full sort. */
    private static final int SORT_BETWEEN_SIZE = 2;
    /** log2(e). Used for conversions: log2(x) = ln(x) * log2(e) */
    private static final double LOG2_E = 1.4426950408889634;

    /** Threshold to use repeated step left: 7 / 16. */
    private static final double STEP_LEFT = 0.4375;
    /** Threshold to use repeated step right: 9 / 16. */
    private static final double STEP_RIGHT = 0.5625;
    /** Threshold to use repeated step far-left: 1 / 12. */
    private static final double STEP_FAR_LEFT = 0.08333333333333333;
    /** Threshold to use repeated step far-right: 11 / 12. */
    private static final double STEP_FAR_RIGHT = 0.9166666666666666;

    /** Default quickselect adaptive mode. Start with FR sampling. */
    private static int qaMode = MODE_FR_SAMPLING;
    /** Default quickselect adaptive mode increment. */
    private static int qaIncrement = 1;

    // Use final for settings/objects used within partitioning functions

    /** A {@link PivotingStrategy} used for pivoting. */
    private final PivotingStrategy pivotingStrategy;
    /** A {@link DualPivotingStrategy} used for pivoting. */
    private final DualPivotingStrategy dualPivotingStrategy;

    /** Minimum size for quickselect when partitioning multiple keys.
     * Below this threshold partitioning using quickselect is stopped and a sort selection
     * is performed.
     *
     * <p>This threshold is also used in the sort methods to switch to insertion sort;
     * and in legacy partition methods which do not use edge selection. These may perform
     * key analysis using this value to determine saturation. */
    private final int minQuickSelectSize;
    /** Constant for edgeselect. */
    private final int edgeSelectConstant;
    /** Size for sortselect in the linearselect function. Optimal value for this is higher
     * than for regular quickselect as the median-of-medians pivot strategy is expensive.
     * For convenience (limit overrides for the constructor) this is not final. */
    private int linearSortSelectSize = LINEAR_SORTSELECT_SIZE;
    /** Threshold to use sub-sampling of the range to identify the single pivot.
     * Sub-sampling uses the Floyd-Rivest algorithm to partition a sample of the data. This
     * identifies a pivot so that the target element is in the smaller set after partitioning.
     * The algorithm applies to searching for a single k.
     * Not all single-pivot {@link PairedKeyStrategy} methods support sub-sampling. It is
     * available to test in {@link #introselect(SPEPartition, double[], int, int, int, int)}.
     *
     * <p>Sub-sampling can provide up to a 2-fold performance gain on large random data.
     * It can have a 2-fold slowdown on some structured data (e.g. large shuffle data from
     * the Bentley and McIlroy test data). Large shuffle data also observes a larger performance
     * drop when using the SBM/BM/DNF partition methods (collect equal values) verses a
     * simple SP method ignoring equal values. Here large ~500,000; the behaviour
     * is observed at smaller sizes and becomes increasingly obvious at larger sizes.
     *
     * <p>The algorithm relies on partitioning of a subset to be representative of partitioning
     * of the entire data. Values in a small range partitioned around a pivot P
     * should create P in a similar location to its position in the entire fully sorted array,
     * ideally closer to the middle so ensuring elimination of the larger side.
     * E.g. ordering around P in [ll, rr] will be similar to P's order in [l, r]:
     * <pre>
     * target:                       k
     * subset:                  ll---P-------rr
     * sorted: l----------------------P-------------------------------------------r
     *                                Good pivot
     * </pre>
     *
     * <p>If the data in [ll, rr] is not representative then pivot selection based on a
     * subset creates bad pivot choices and performance is worse than using a
     * {@link PivotingStrategy}.
     * <pre>
     * target:                       k
     * subset:                 ll----P-------rr
     * sorted: l------------------------------------------P----------------------r
     *                                                    Bad pivot
     * </pre>
     *
     * <p>Use of the Floyd-Rivest subset sampling is not always an improvement and is data
     * dependent. The type of data cannot be known by the partition algorithm before processing.
     * Thus the Floyd-Rivest subset sampling is more suitable as an option to be enabled by
     * user settings.
     *
     * <p>A random sub-sample can mitigate issues with non-representative data. This can
     * be done by sampling with/without replacement into a new array; or shuffling in-place
     * to part of the array. This implementation supports the later option.
     *
     * <p>See <a href="https://en.wikipedia.org/wiki/Floyd%E2%80%93Rivest_algorithm">
     * Floyd-Rivest Algorithm (Wikipedia)</a>.
     *
     * <pre>
     * Floyd and Rivest (1975)
     * Algorithm 489: The Algorithm SELECT—for Finding the ith Smallest of n elements.
     * Comm. ACM. 18 (3): 173.
     * </pre> */
    private final int subSamplingSize;

    // Use non-final members for settings used to configure partitioning functions

    /** Setting to indicate strategy for processing of multiple keys. */
    private KeyStrategy keyStrategy = KEY_STRATEGY;
    /** Setting to indicate strategy for processing of 1 or 2 keys. */
    private PairedKeyStrategy pairedKeyStrategy = PAIRED_KEY_STRATEGY;

    /** Multiplication factor {@code m} applied to the length based recursion factor {@code x}.
     * The recursion is set using {@code m * x + c}.
     * <p>Also used for the multiple of the original length to check the sum of the partition length
     * for poor quickselect partitions.
     * <p>Also used for the number of iterations before checking the partition length has been
     * reduced by a given factor of 2 (in iselect). */
    private double recursionMultiple = RECURSION_MULTIPLE;
    /** Constant {@code c} added to the length based recursion factor {@code x}.
     * The recursion is set using {@code m * x + c}.
     * <p>Also used to specify the factor of two to reduce the partition length after a set
     * number of iterations (in iselect). */
    private int recursionConstant = RECURSION_CONSTANT;
    /** Compression level for a {@link CompressedIndexSet} (in [1, 31]). */
    private int compression = COMPRESSION_LEVEL;
    /** Control flags level for Floyd-Rivest sub-sampling. */
    private int controlFlags = CONTROL_FLAGS;
    /** Consumer for the recursion level reached during partitioning. Used to analyse
     * the distribution of the recursion for different input data. */
    private IntConsumer recursionConsumer = i -> { /* no-op */ };

    /** The single-pivot partition function. */
    private SPEPartition spFunction;
    /** The expand partition function. */
    private ExpandPartition expandFunction;
    /** The single-pivot linear partition function. */
    private SPEPartition linearSpFunction;
    /** Selection function used when {@code k} is close to the edge of the range. */
    private SelectFunction edgeSelection;
    /** Selection function used when quickselect progress is poor. */
    private SelectFunction stopperSelection;
    /** Quickselect adaptive mode. */
    private AdaptMode adaptMode = ADAPT_MODE;

    /** Quickselect adaptive mapping function applied when sampling-mode is on. */
    private MapDistance samplingAdapt;
    /** Quickselect adaptive mapping function applied when sampling-mode is on for
     * distances close to the edge (i.e. the far-step functions). */
    private MapDistance samplingEdgeAdapt;
    /** Quickselect adaptive mapping function applied when sampling-mode is off. */
    private MapDistance noSamplingAdapt;
    /** Quickselect adaptive mapping function applied when sampling-mode is off for
     * distances close to the edge (i.e. the far-step functions). */
    private MapDistance noSamplingEdgeAdapt;

    /**
     * Define the strategy for processing multiple keys.
     */
    enum KeyStrategy {
        /** Sort unique keys, collate ranges and process in ascending order. */
        SEQUENTIAL,
        /** Process in input order using an {@link IndexSet} to cover the entire range.
         * Introselect implementations will use a {@link SearchableInterval}. */
        INDEX_SET,
        /** Process in input order using a {@link CompressedIndexSet} to cover the entire range.
         * Introselect implementations will use a {@link SearchableInterval}. */
        COMPRESSED_INDEX_SET,
        /** Process in input order using a {@link PivotCache} to cover the minimum range. */
        PIVOT_CACHE,
        /** Sort unique keys and process using recursion with division of the keys
         * for each sub-partition. */
        ORDERED_KEYS,
        /** Sort unique keys and process using recursion with a {@link ScanningKeyInterval}. */
        SCANNING_KEY_SEARCHABLE_INTERVAL,
        /** Sort unique keys and process using recursion with a {@link BinarySearchKeyInterval}. */
        SEARCH_KEY_SEARCHABLE_INTERVAL,
        /** Sort unique keys and process using recursion with a {@link KeyIndexIterator}. */
        INDEX_ITERATOR,
        /** Process in input order using an {@link IndexIterator} of a {@link CompressedIndexSet}. */
        COMPRESSED_INDEX_ITERATOR,
        /** Process using recursion with an {@link IndexSet}-based {@link UpdatingInterval}. */
        INDEX_SET_UPDATING_INTERVAL,
        /** Sort unique keys and process using recursion with an {@link UpdatingInterval}. */
        KEY_UPDATING_INTERVAL,
        /** Process using recursion with an {@link IndexSet}-based {@link SplittingInterval}. */
        INDEX_SET_SPLITTING_INTERVAL,
        /** Sort unique keys and process using recursion with a {@link SplittingInterval}. */
        KEY_SPLITTING_INTERVAL;
    }

    /**
     * Define the strategy for processing 1 key or 2 keys: (k, k+1).
     */
    enum PairedKeyStrategy {
        /** Use a dedicated single key method that returns information about (k+1).
         * Use recursion depth to trigger the stopper select. */
        PAIRED_KEYS,
        /** Use a dedicated single key method that returns information about (k+1).
         * Recursion is monitored by checking the partition is reduced by 2<sup>-x</sup> after
         * {@code c} iterations where {@code x} is the
         * {@link #setRecursionConstant(int) recursion constant} and {@code c} is the
         * {@link #setRecursionMultiple(double) recursion multiple} */
        PAIRED_KEYS_2,
        /** Use a dedicated single key method that returns information about (k+1).
         * Use a multiple of the sum of the length of all partitions to trigger the stopper select. */
        PAIRED_KEYS_LEN,
        /** Use a method that accepts two separate keys. The keys do not define a range
         * and are independent. */
        TWO_KEYS,
        /** Use a method that accepts two keys to define a range.
         * Recursion is monitored by checking the partition is reduced by 2<sup>-x</sup> after
         * {@code c} iterations where {@code x} is the
         * {@link #setRecursionConstant(int) recursion constant} and {@code c} is the
         * {@link #setRecursionMultiple(double) recursion multiple} */
        KEY_RANGE,
        /** Use an {@link SearchableInterval} covering the keys. This will reuse a multi-key
         * strategy with keys that are a very small range. */
        SEARCHABLE_INTERVAL,
        /** Use an {@link UpdatingInterval} covering the keys. This will reuse a multi-key
         * strategy with keys that are a very small range. */
        UPDATING_INTERVAL;
    }

    /**
     * Define the strategy for single-pivot partitioning. Partitioning may be binary
     * ({@code <, >}), or ternary ({@code <, ==, >}) by collecting values equal to the
     * pivot value. Typically partitioning will use two pointers i and j to traverse the
     * sequence from either end; or a single pointer i for a single pass.
     *
     * <p>Binary partitioning will be faster for quickselect when no equal elements are
     * present. As duplicates become increasingly likely a ternary partition will be
     * faster for quickselect to avoid repeat processing of values (that matched the
     * previous pivot) on the next iteration. The type of ternary partition with the best
     * performance depends on the number of duplicates. In the extreme case of 1 or 2
     * unique elements it is more likely to match the {@code ==, !=} comparison to the
     * pivot than {@code <, >} (see {@link #DNF3}). An ideal ternary scheme should have
     * little impact on data with no repeats, and significantly improve performance as the
     * number of repeat elements increases.
     *
     * <p>Binary partitioning will skip over values already {@code <, >}, or
     * {@code <=, =>} to the pivot value; otherwise values at the pointers i and j are
     * swapped. If using {@code <, >} then values can be placed at either end of the
     * sequence that are {@code >=, <=} respectively to act as sentinels during the scan.
     * This is always possible in binary partitioning as the pivot can be one sentinel;
     * any other value will be either {@code <=, =>} to the pivot and so can be used at
     * one or the other end as appropriate. Note: Many schemes omit using sentinels. Modern
     * processor branch prediction nullifies the cost of checking indices remain within
     * the {@code [left, right]} bounds. However placing sentinels is a negligible cost
     * and at least simplifies the code for the region traversal.
     *
     * <p>Bentley-McIlroy ternary partitioning schemes move equal values to the ends
     * during the traversal, these are moved to the centre after the pass. This may use
     * minimal swaps based on region sizes. Note that values already {@code <, >} are not
     * moved during traversal allowing moves to be minimised.
     *
     * <p>Dutch National Flag schemes move non-equal values to either end and finish with
     * the equal value region in the middle. This requires that every element is moved
     * during traversal, even if already {@code <, >}. This can be mitigated by fast-forward
     * of pointers at the current {@code <, >} end points until the condition is not true.
     *
     * @see SPEPartition
     */
    enum SPStrategy {
        /**
         * Single-pivot partitioning. Uses a method adapted from Floyd and Rivest (1975)
         * which uses sentinels to avoid bounds checks on the i and j pointers.
         * This is a baseline for the maximum speed when no equal elements are present.
         */
        SP,
        /**
         * Bentley-McIlroy ternary partitioning. Requires bounds checks on the i and j
         * pointers during traversal. Comparisons to the pivot use {@code <=, =>} and a
         * second check for {@code ==} if the first is true.
         */
        BM,
        /**
         * Sedgewick's Bentley-McIlroy ternary partitioning. Requires bounds checks on the
         * j pointer during traversal. Comparisons to the pivot use {@code <, >} and a
         * second check for {@code ==} when both i and j have stopped.
         */
        SBM,
        /**
         * Kiwiel's Bentley-McIlroy ternary partitioning. Similar to Sedgewick's BM but
         * avoids bounds checks on both pointers during traversal using sentinels.
         * Comparisons to the pivot use {@code <, >} and a second check for {@code ==}
         * when both i and j have stopped. Handles i and j meeting at the pivot without a
         * swap.
         */
        KBM,
        /**
         * Dutch National Flag partitioning. Single pointer iteration using {@code <, >}
         * comparisons to move elements to the edges. Fast-forwards any initial {@code <}
         * region. The {@code ==} region is filled with the pivot after region traversal.
         */
        DNF1,
        /**
         * Dutch National Flag partitioning. Single pointer iteration using {@code <, >}
         * comparisons to move elements to the edges. Fast-forwards any initial {@code <}
         * region. The {@code >} region uses fast-forward to reduce swaps. The {@code ==}
         * region is filled with the pivot after region traversal.
         */
        DNF2,
        /**
         * Dutch National Flag partitioning. Single pointer iteration using {@code !=}
         * comparison to identify elements to move to the edges, then {@code <, >}
         * comparisons. Fast-forwards any initial {@code <} region. The {@code >} region
         * uses fast-forward to reduce swaps. The {@code ==} region is filled during
         * traversal.
         */
        DNF3;
    }

    /**
     * Define the strategy for expanding a partition. This function is used when
     * partitioning has used a sample located within the range to find the pivot.
     * The remaining range below and above the sample can be partitioned without
     * re-processing the sample.
     *
     * <p>Schemes may be binary ({@code <, >}), or ternary ({@code <, ==, >}) by
     * collecting values equal to the pivot value. Schemes may process the
     * unpartitioned range below and above the partitioned middle using a sweep
     * outwards towards the ends; or start at the ends and sweep inwards towards
     * the partitioned middle.
     *
     * @see ExpandPartition
     */
    enum ExpandStrategy {
        /** Use the current {@link SPStrategy} partition method. This will not expand
         * the partition but will Partition the Entire Range (PER). This can be used
         * to test if the implementations of expand are efficient. */
        PER,
        /** Ternary partition method 1. Sweeps outwards and uses sentinels at the ends
         * to avoid pointer range checks. Equal values are moved directly into the
         * central pivot range. */
        T1,
        /** Ternary partition method 2. Similar to {@link #T1} with different method
         * to set the sentinels. */
        T2,
        /** Binary partition method 1. Sweeps outwards and uses sentinels at the ends
         * to avoid pointer range checks. */
        B1,
        /** Binary partition method 2. Similar to {@link #B1} with different method
         * to set the sentinels. */
        B2,
    }

    /**
     * Define the strategy for the linear select single-pivot partition function. Linear
     * select functions use a deterministic sample to find a pivot value that will
     * eliminate at least a set fraction of the range (fixed borders/margins). After the
     * sample has been processed to find a pivot the entire range is partitioned. This can
     * be done by re-processing the entire range, or expanding the partition.
     *
     * <p>Adaption (selecting a non-central index in the median-of-medians sample) creates
     * asymmetric borders; in practice the larger border is typically eliminated per
     * iteration which improves performance.
     *
     * @see SPStrategy
     * @see ExpandStrategy
     * @see SPEPartition
     * @see ExpandPartition
     */
    enum LinearStrategy {
        /** Uses the Blum, Floyd, Pratt, Rivest, and Tarjan (BFPRT) median-of-medians algorithm
         * with medians of 5. This is the baseline version that creates the median sample
         * at the left end and repartitions the entire range using the pivot.
         * Fixed borders of 3/10. */
        BFPRT,
        /** Uses the Chen and Dumitrescu repeated step median-of-medians-of-medians algorithm
         * with medians of 3. This is the baseline version that creates the median sample
         * at the left end and repartitions the entire range using the pivot.
         * Fixed borders of 2/9. */
        RS,
        /** Uses the Blum, Floyd, Pratt, Rivest, and Tarjan (BFPRT) median-of-medians algorithm
         * with medians of 5. This is the improved version that creates the median sample
         * in the centre and expands the partition around the pivot sample.
         * Fixed borders of 3/10. */
        BFPRT_IM,
        /** Uses the Chen and Dumitrescu repeated step median-of-medians-of-medians algorithm
         * with medians of 3. This is the improved version that creates the median sample
         * in the centre and expands the partition around the pivot sample.
         * Fixed borders of 2/9. */
        RS_IM,
        /** Uses the Blum, Floyd, Pratt, Rivest, and Tarjan (BFPRT) median-of-medians algorithm
         * with medians of 5. This is the improved version that creates the median sample
         * in the centre and expands the partition around the pivot sample; the adaption
         * is to use k to define the pivot in the sample instead of using the median.
         * This will not have fixed borders. */
        BFPRTA,
        /** Uses the Chen and Dumitrescu repeated step median-of-medians-of-medians algorithm
         * with medians of 3. This is the adaptive version that creates the median sample
         * in the centre and expands the partition around the pivot sample; the adaption
         * is to use k to define the pivot in the sample instead of using the median.
         * This will not have fixed borders. */
        RSA;
    }

    /**
     * Define the strategy for selecting {@code k} close to the edge.
     * <p>These are named to allow regex identification for dynamic configuration
     * in benchmarking using the name; this uses the E (Edge) prefix.
     */
    enum EdgeSelectStrategy {
        /** Use heapselect version 1. Selects {@code k} and an additional
         * {@code c} elements closer to the edge than {@code k} using a heap
         * structure. */
        ESH,
        /** Use heapselect version 2. Differs from {@link #ESH} in the
         * final unwinding of the heap to sort the range {@code [ka, kb]};
         * the heap construction is identical. */
        ESH2,
        /** Use sortselect version 1. Uses an insertion sort to maintain {@code k}
         * and all elements closer to the edge as sorted. */
        ESS,
        /** Use sortselect version 2. Differs from {@link #ESS} by a using pointer
         * into the sorted range to improve insertion speed. In practice the more
         * complex code is not more performant. */
        ESS2;
    }

    /**
     * Define the strategy for selecting {@code k} when quickselect progress is poor
     * (worst case is quadratic). This should be a method providing good worst-case
     * performance.
     * <p>These are named to allow regex identification for dynamic configuration
     * in benchmarking using the name; this uses the S (Stopper) prefix.
     */
    enum StopperStrategy {
        /** Use heapselect version 1. Selects {@code k} and an additional
         * {@code c} elements closer to the edge than {@code k}. Heapselect
         * provides increasingly slower performance with distance from the edge.
         * It has better worst-case performance than quickselect. */
        SSH,
        /** Use heapselect version 2. Differs from {@link #SSH} in the
         * final unwinding of the heap to sort the range {@code [ka, kb]};
         * the heap construction is identical. */
        SSH2,
        /** Use a linear selection algorithm with Order(n) worst-case performance.
         * This is a median-of-medians using medians of size 5. This is the base
         * implementation using a median sample into the first 20% of the data
         * and not the improved version (with sample in the centre). */
        SLS,
        /** Use the quickselect adaptive algorithm with Order(n) worst-case performance. */
        SQA;
    }

    /**
     * Partition function. Used to benchmark different implementations.
     *
     * <p>Note: The function is applied within a {@code [left, right]} bound. This bound
     * is set using the entire range of the data to process, or it may be a sub-range
     * due to previous partitioning. In this case the value at {@code left - 1} and/or
     * {@code right + 1} can be a pivot. The value at these pivot points will be {@code <=} or
     * {@code >=} respectively to all values within the range. This information is valuable
     * during recursive partitioning and is passed as flags to the partition method.
     */
    private interface PartitionFunction {

        /**
         * Partition (partially sort) the array in the range {@code [left, right]} around
         * a central region {@code [ka, kb]}. The central region should be entirely
         * sorted.
         *
         * <pre>{@code
         * data[i < ka] <= data[ka] <= data[kb] <= data[kb < i]
         * }</pre>
         *
         * @param a Data array.
         * @param left Lower bound (inclusive).
         * @param right Upper bound (inclusive).
         * @param ka Lower bound (inclusive) of the central region.
         * @param kb Upper bound (inclusive) of the central region.
         * @param leftInner Flag to indicate {@code left - 1} is a pivot.
         * @param rightInner Flag to indicate {@code right + 1} is a pivot.
         */
        void partition(double[] a, int left, int right, int ka, int kb,
            boolean leftInner, boolean rightInner);

        /**
         * Partition (partially sort) the array in the range {@code [left, right]} around
         * a central region {@code [ka, kb]}. The central region should be entirely
         * sorted.
         *
         * <pre>{@code
         * data[i < ka] <= data[ka] <= data[kb] <= data[kb < i]
         * }</pre>
         *
         * <p>The {@link PivotStore} is only required to record pivots after {@code kb}.
         * This is to support sequential ascending order processing of regions to partition.
         *
         * @param a Data array.
         * @param left Lower bound (inclusive).
         * @param right Upper bound (inclusive).
         * @param ka Lower bound (inclusive) of the central region.
         * @param kb Upper bound (inclusive) of the central region.
         * @param leftInner Flag to indicate {@code left - 1} is a pivot.
         * @param rightInner Flag to indicate {@code right + 1} is a pivot.
         * @param pivots Used to store sorted regions.
         */
        void partitionSequential(double[] a, int left, int right, int ka, int kb,
            boolean leftInner, boolean rightInner, PivotStore pivots);

        /**
         * Partition (partially sort) the array in the range {@code [left, right]} around
         * a central region {@code [ka, kb]}. The central region should be entirely
         * sorted.
         *
         * <pre>{@code
         * data[i < ka] <= data[ka] <= data[kb] <= data[kb < i]
         * }</pre>
         *
         * <p>The {@link PivotStore} records all pivots and sorted regions.
         *
         * @param a Data array.
         * @param left Lower bound (inclusive).
         * @param right Upper bound (inclusive).
         * @param ka Lower bound (inclusive) of the central region.
         * @param kb Upper bound (inclusive) of the central region.
         * @param leftInner Flag to indicate {@code left - 1} is a pivot.
         * @param rightInner Flag to indicate {@code right + 1} is a pivot.
         * @param pivots Used to store sorted regions.
         */
        void partition(double[] a, int left, int right, int ka, int kb,
            boolean leftInner, boolean rightInner, PivotStore pivots);

        /**
         * Sort the array in the range {@code [left, right]}.
         *
         * @param a Data array.
         * @param left Lower bound (inclusive).
         * @param right Upper bound (inclusive).
         * @param leftInner Flag to indicate {@code left - 1} is a pivot.
         * @param rightInner Flag to indicate {@code right + 1} is a pivot.
         */
        void sort(double[] a, int left, int right, boolean leftInner, boolean rightInner);
    }

    /**
     * Single-pivot partition method handling equal values.
     */
    @FunctionalInterface
    private interface SPEPartitionFunction extends PartitionFunction {
        /**
         * Partition an array slice around a single pivot. Partitioning exchanges array
         * elements such that all elements smaller than pivot are before it and all
         * elements larger than pivot are after it.
         *
         * <p>This method returns 2 points describing the pivot range of equal values.
         * <pre>{@code
         *                     |k0 k1|
         * |         <P        | ==P |            >P        |
         * }</pre>
         * <ul>
         * <li>k0: lower pivot point
         * <li>k1: upper pivot point
         * </ul>
         *
         * @param a Data array.
         * @param left Lower bound (inclusive).
         * @param right Upper bound (inclusive).
         * @param upper Upper bound (inclusive) of the pivot range [k1].
         * @param leftInner Flag to indicate {@code left - 1} is a pivot.
         * @param rightInner Flag to indicate {@code right + 1} is a pivot.
         * @return Lower bound (inclusive) of the pivot range [k0].
         */
        int partition(double[] a, int left, int right, int[] upper,
            boolean leftInner, boolean rightInner);

        // Add support to have a pivot cache. Assume it is to store pivots after kb.
        // Switch to not using it when right < kb, or doing a full sort between
        // left and right (pivots are irrelevant).

        @Override
        default void partition(double[] a, int left, int right, int ka, int kb,
            boolean leftInner, boolean rightInner) {
            // Skip when [left, right] does not overlap [ka, kb]
            if (right - left < 1) {
                return;
            }
            // Assume: left <= right && ka <= kb
            // Ranges may overlap either way:
            // left ---------------------- right
            //        ka --- kb
            //
            // Requires full sort:
            // ka ------------------------- kb
            //        left ---- right
            //
            // This will naturally perform a full sort when ka < left and kb > right

            // Edge case for a single point
            if (ka == right) {
                selectMax(a, left, ka);
            } else if (kb == left) {
                selectMin(a, kb, right);
            } else {
                final int[] upper = {0};
                final int k0 = partition(a, left, right, upper, leftInner, rightInner);
                final int k1 = upper[0];
                // Sorted in [k0, k1]
                // Unsorted in [left, k0) and (k1, right]
                if (ka < k0) {
                    partition(a, left, k0 - 1, ka, kb, leftInner, true);
                }
                if (kb > k1) {
                    partition(a, k1 + 1, right, ka, kb, true, rightInner);
                }
            }
        }

        @Override
        default void partitionSequential(double[] a, int left, int right, int ka, int kb,
            boolean leftInner, boolean rightInner, PivotStore pivots) {
            // This method is a copy of the above method except:
            // - It records all sorted ranges to the cache
            // - It switches to the above method when the cache is not required
            if (right - left < 1) {
                return;
            }
            if (ka == right) {
                selectMax(a, left, ka);
                pivots.add(ka);
            } else if (kb == left) {
                selectMin(a, kb, right);
                pivots.add(kb);
            } else {
                final int[] upper = {0};
                final int k0 = partition(a, left, right, upper, leftInner, rightInner);
                final int k1 = upper[0];
                // Sorted in [k0, k1]
                // Unsorted in [left, k0) and (k1, right]
                pivots.add(k0, k1);

                if (ka < k0) {
                    if (k0 - 1 < kb) {
                        // Left branch entirely below kb - no cache required
                        partition(a, left, k0 - 1, ka, kb, leftInner, true);
                    } else {
                        partitionSequential(a, left, k0 - 1, ka, kb, leftInner, true, pivots);
                    }
                }
                if (kb > k1) {
                    partitionSequential(a, k1 + 1, right, ka, kb, true, rightInner, pivots);
                }
            }
        }

        @Override
        default void partition(double[] a, int left, int right, int ka, int kb,
            boolean leftInner, boolean rightInner, PivotStore pivots) {
            // This method is a copy of the above method except:
            // - It records all sorted ranges to the cache
            // - It switches to the above method when the cache is not required
            if (right - left < 1) {
                return;
            }
            if (ka == right) {
                selectMax(a, left, ka);
                pivots.add(ka);
            } else if (kb == left) {
                selectMin(a, kb, right);
                pivots.add(kb);
            } else {
                final int[] upper = {0};
                final int k0 = partition(a, left, right, upper, leftInner, rightInner);
                final int k1 = upper[0];
                // Sorted in [k0, k1]
                // Unsorted in [left, k0) and (k1, right]
                pivots.add(k0, k1);

                if (ka < k0) {
                    partition(a, left, k0 - 1, ka, kb, leftInner, true, pivots);
                }
                if (kb > k1) {
                    partition(a, k1 + 1, right, ka, kb, true, rightInner, pivots);
                }
            }
        }

        @Override
        default void sort(double[] a, int left, int right, boolean leftInner, boolean rightInner) {
            // Skip when [left, right] is sorted
            // Note: This has no insertion sort for small lengths (so is less performant).
            // It can be used to test the partition algorithm across the entire data.
            if (right - left < 1) {
                return;
            }
            final int[] upper = {0};
            final int k0 = partition(a, left, right, upper, leftInner, rightInner);
            final int k1 = upper[0];
            // Sorted in [k0, k1]
            // Unsorted in [left, k0) and (k1, right]
            sort(a, left, k0 - 1, leftInner, true);
            sort(a, k1 + 1, right, true, rightInner);
        }
    }

    /**
     * Single-pivot partition method handling equal values.
     */
    @FunctionalInterface
    interface SPEPartition {
        /**
         * Partition an array slice around a single pivot. Partitioning exchanges array
         * elements such that all elements smaller than pivot are before it and all
         * elements larger than pivot are after it.
         *
         * <p>This method returns 2 points describing the pivot range of equal values.
         * <pre>{@code
         *                     |k0 k1|
         * |         <P        | ==P |            >P        |
         * }</pre>
         * <ul>
         * <li>k0: lower pivot point
         * <li>k1: upper pivot point (inclusive)
         * </ul>
         *
         * @param a Data array.
         * @param left Lower bound (inclusive).
         * @param right Upper bound (inclusive).
         * @param upper Upper bound (inclusive) of the pivot range [k1].
         * @param pivot Pivot location.
         * @return Lower bound (inclusive) of the pivot range [k0].
         */
        int partition(double[] a, int left, int right, int pivot, int[] upper);
    }

    /**
     * Dual-pivot partition method handling equal values.
     */
    @FunctionalInterface
    interface DPPartition {
        /**
         * Partition an array slice around two pivots. Partitioning exchanges array
         * elements such that all elements smaller than pivot are before it and all
         * elements larger than pivot are after it.
         *
         * <p>This method returns 4 points describing the pivot ranges of equal values.
         * <pre>{@code
         *         |k0  k1|                |k2  k3|
         * |   <P  | ==P1 |  <P1 && <P2    | ==P2 |   >P   |
         * }</pre>
         * <ul>
         * <li>k0: lower pivot1 point
         * <li>k1: upper pivot1 point (inclusive)
         * <li>k2: lower pivot2 point
         * <li>k3: upper pivot2 point (inclusive)
         * </ul>
         *
         * <p>Bounds are set so {@code i < k0}, {@code i > k3} and {@code k1 < i < k2} are
         * unsorted. When the range {@code [k0, k3]} contains fully sorted elements the result
         * is set to {@code k1 = k3; k2 == k0}. This can occur if
         * {@code P1 == P2} or there are zero or 1 value between the pivots
         * {@code P1 < v < P2}. Any sort/partition of ranges [left, k0-1], [k1+1, k2-1] and
         * [k3+1, right] must check the length is {@code > 1}.
         *
         * @param a Data array.
         * @param left Lower bound (inclusive).
         * @param right Upper bound (inclusive).
         * @param bounds Points [k1, k2, k3].
         * @param pivot1 Pivot1 location.
         * @param pivot2 Pivot2 location.
         * @return Lower bound (inclusive) of the pivot range [k0].
         */
        int partition(double[] a, int left, int right, int pivot1, int pivot2, int[] bounds);
    }

    /**
     * Select function.
     *
     * <p>Used to define the function to call when {@code k} is close
     * to the edge; or when quickselect progress is poor. This allows
     * the edge-select or stopper-function to be configured using parameters.
     */
    @FunctionalInterface
    interface SelectFunction {
        /**
         * Partition the elements between {@code ka} and {@code kb}.
         * It is assumed {@code left <= ka <= kb <= right}.
         *
         * @param a Data array to use to find out the K<sup>th</sup> value.
         * @param left Lower bound (inclusive).
         * @param right Upper bound (inclusive).
         * @param ka Lower index to select.
         * @param kb Upper index to select.
         */
        void partition(double[] a, int left, int right, int ka, int kb);
    }

    /**
     * Single-pivot partition method handling a pre-partitioned range in the centre.
     */
    @FunctionalInterface
    interface ExpandPartition {
        /**
         * Expand a partition around a single pivot. Partitioning exchanges array
         * elements such that all elements smaller than pivot are before it and all
         * elements larger than pivot are after it. The central region is already
         * partitioned.
         *
         * <pre>{@code
         * |l             |s   |p0 p1|   e|                r|
         * |    ???       | <P | ==P | >P |        ???      |
         * }</pre>
         *
         * <p>This method returns 2 points describing the pivot range of equal values.
         * <pre>{@code
         * |l                  |k0 k1|                     r|
         * |         <P        | ==P |            >P        |
         * }</pre>
         * <ul>
         * <li>k0: lower pivot point
         * <li>k1: upper pivot point (inclusive)
         * </ul>
         *
         * @param a Data array.
         * @param left Lower bound (inclusive).
         * @param right Upper bound (inclusive).
         * @param start Start of the partition range (inclusive).
         * @param end End of the partitioned range (inclusive).
         * @param pivot0 Lower pivot location (inclusive).
         * @param pivot1 Upper pivot location (inclusive).
         * @param upper Upper bound (inclusive) of the pivot range [k1].
         * @return Lower bound (inclusive) of the pivot range [k0].
         */
        int partition(double[] a, int left, int right, int start, int end,
            int pivot0, int pivot1, int[] upper);
    }

    /**
     * Function to map the distance from the edge of a range {@code [l, r]} to a smaller
     * range. The mapping is used in the quickselect adaptive method to adapt {@code k}
     * based on the position in the sample: {@code kf'/|A|}; k is the index to partition;
     * |A| is the size of the data to partition; f' is the size of the sample.
     */
    enum MapDistance {
        /** Use the median of the new range. */
        MEDIAN {
            @Override
            int mapDistance(int d, int l, int r, int n) {
                return n >>> 1;
            }
        },
        /** Map the distance using a fraction of the original range: {@code d / (r - l)}. */
        ADAPT {
            @Override
            int mapDistance(int d, int l, int r, int n) {
                // If distance==r-l this returns n-1
                return (int) (d * (n - 1.0) / (r - l));
            }
        },
        /** Use the midpoint between the adaption computed by the {@link #MEDIAN} and {@link #ADAPT} methods. */
        HALF_ADAPT {
            @Override
            int mapDistance(int d, int l, int r, int n) {
                // Half-adaption: compute the full adaption
                final int x = ADAPT.mapDistance(d, l, r, n);
                // Compute the median between the x and the middle
                final int m = n >>> 1;
                return (m + x) >>> 1;
            }
        },
        /**
         * Map the distance assuming the distance to the edge is small. This method is
         * used when the sample has a lower margin (minimum number of elements) in the
         * original range of {@code 2(d+1)}. That is each element in the sample has at
         * least 1 element below it in the original range. This occurs for example when
         * the sample is built using a median-of-3. In this case setting the mapped
         * distance as {@code d/2} will ensure that the lower margin in the original data
         * is at least {@code d} and consequently {@code d} is inside the lower margin. It
         * will generate a bounds error if called with {@code d > 2(r - l)}.
         */
        EDGE_ADAPT {
            @Override
            int mapDistance(int d, int l, int r, int n) {
                return d >>> 1;
            }
        };

        /**
         * Map the distance from the edge of {@code [l, r]} to a new distance in {@code [0, n)}.
         *
         * <p>For convenience this accepts the input range {@code [l, r]} instead of the length
         * of the original range. The implementation may use the range or ignore it and only
         * use the new range size {@code n}.
         *
         * @param d Distance from the edge in {@code [0, r - l]}.
         * @param l Lower bound (inclusive).
         * @param r Upper bound (inclusive).
         * @param n Size of the new range.
         * @return the mapped distance in [0, n)
         */
        abstract int mapDistance(int d, int l, int r, int n);
    }

    /**
     * Encapsulate the state of adaption in the quickselect adaptive algorithm.
     *
     * <p>To ensure linear runtime performance a fixed size of data must be eliminated at each
     * step. This requires a median-of-median-of-medians pivot sample generated from all the data
     * with the target {@code k} mapped to the middle of the sample so that the margins of the
     * possible partitions are a minimum size.
     * Note that random selection of a pivot will achieve the same margins with some probability
     * and is less expensive to compute; runtime performance may be better or worse due to average
     * quality of the pivot. The adaption in quickselect adaptive is two-fold:
     * <ul>
     * <li>Sample mode: Do not use all the data to create the pivot sample. This is less expensive
     * to compute but invalidates strict margins. The margin quality is better than a random pivot
     * due to sampling a reasonable range of the data and using medians to create the sample.
     * <li>Adaption mode: Map the target {@code k} from the current range to the size of the pivot
     * sample. This create asymmetric margins and adapts the larger margin to the position of
     * {@code k} thus increasing the chance of eliminating a large amount of data. However data
     * randomness can create a larger margin so large it includes {@code k} and partitioning
     * must eliminate a possibly very small other side.
     * </ul>
     *
     * <p>The quickselect adaptive paper suggests sampling mode is turned off when margins are not
     * achieved. That is the size after partitioning is not as small as expected.
     * However there is no detail on whether to turn off adaption, and the margins that are
     * targeted. This provides the following possible state transitions:
     * <pre>{@code
     * 1: sampling + adaption    --> no-sampling + adaption
     * 2: sampling + adaption    --> no-sampling + no-adaption
     * 3: sampling + adaption    --> no-sampling + adaption     --> no-sampling + no-adaption
     * 4: sampling + no-adaption --> no-sampling + no-adaption
     * }</pre>
     *
     * <p>The behaviour is captured in this enum as a state-machine. The finite state
     * is dependent on the start state. The transition from one state to the next may require a
     * count of failures to achieve; this is not captured in this state machine.
     *
     * <p>Note that use of no-adaption when sampling (case 4) is unlikely to work unless the sample
     * median is representative of the location of the pivot sample. This is true for
     * median-of-median-of-medians but not the offset pivot samples used in quickselect adaptive;
     * this is supported for completeness and can be used to demonstrate its inefficiency.
     */
    enum AdaptMode {
        /** No sampling and no adaption (fixed margins) for worst-case linear runtime performance.
         * This is a terminal state. */
        FIXED {
            @Override
            boolean isSampleMode() {
                return false;
            }
            @Override
            boolean isAdapt() {
                return false;
            }
            @Override
            AdaptMode update(int size, int l, int r) {
                // No further states
                return this;
            }
        },
        /** Sampling and adaption. Failure to achieve the expected partition size
         * will revert to no sampling but retain adaption. */
        ADAPT1 {
            @Override
            boolean isSampleMode() {
                return true;
            }
            @Override
            boolean isAdapt() {
                return true;
            }
            @Override
            AdaptMode update(int size, int l, int r) {
                return r - l <= size ? this : ADAPT1B;
            }
        },
        /** No sampling and use adaption. This is a terminal state. */
        ADAPT1B {
            @Override
            boolean isSampleMode() {
                return false;
            }
            @Override
            boolean isAdapt() {
                return true;
            }
            @Override
            AdaptMode update(int size, int l, int r) {
                // No further states
                return this;
            }
        },
        /** Sampling and adaption. Failure to achieve the expected partition size
         * will revert to no sampling and no adaption. */
        ADAPT2 {
            @Override
            boolean isSampleMode() {
                return true;
            }
            @Override
            boolean isAdapt() {
                return true;
            }
            @Override
            AdaptMode update(int size, int l, int r) {
                return r - l <= size ? this : FIXED;
            }
        },
        /** Sampling and adaption. Failure to achieve the expected partition size
         * will revert to no sampling but retain adaption. */
        ADAPT3 {
            @Override
            boolean isSampleMode() {
                return true;
            }
            @Override
            boolean isAdapt() {
                return true;
            }
            @Override
            AdaptMode update(int size, int l, int r) {
                return r - l <= size ? this : ADAPT3B;
            }
        },
        /** No sampling and use adaption. Failure to achieve the expected partition size
         * will disable adaption (revert to fixed margins). */
        ADAPT3B {
            @Override
            boolean isSampleMode() {
                return false;
            }
            @Override
            boolean isAdapt() {
                return true;
            }
            @Override
            AdaptMode update(int size, int l, int r) {
                return r - l <= size ? this : FIXED;
            }
        },
        /** Sampling and no adaption. Failure to achieve the expected partition size
         * will disabled sampling (revert to fixed margins). */
        ADAPT4 {
            @Override
            boolean isSampleMode() {
                return true;
            }
            @Override
            boolean isAdapt() {
                return false;
            }
            @Override
            AdaptMode update(int size, int l, int r) {
                return r - l <= size ? this : FIXED;
            }
        };

        /**
         * Checks if sample-mode is enabled.
         *
         * @return true if sample mode is enabled
         */
        abstract boolean isSampleMode();

        /**
         * Checks if adaption is enabled.
         *
         * @return true if adaption is enabled
         */
        abstract boolean isAdapt();

        /**
         * Update the state using the expected {@code size} of the partition and the actual size.
         *
         * <p>For convenience this accepts the range {@code [l, r]} instead of the actual size.
         * The implementation may use the range or ignore it.
         *
         * @param size Expected size of the partition.
         * @param l Lower bound (inclusive).
         * @param r Upper bound (inclusive).
         * @return the new state
         */
        abstract AdaptMode update(int size, int l, int r);
    }

    /**
     * Constructor with defaults.
     */
    Partition() {
        this(PIVOTING_STRATEGY, DUAL_PIVOTING_STRATEGY, MIN_QUICKSELECT_SIZE,
            EDGESELECT_CONSTANT, SUBSAMPLING_SIZE);
    }

    /**
     * Constructor with specified pivoting strategy and quickselect size.
     *
     * <p>Used to test single-pivot quicksort.
     *
     * @param pivotingStrategy Pivoting strategy to use.
     * @param minQuickSelectSize Minimum size for quickselect.
     */
    Partition(PivotingStrategy pivotingStrategy, int minQuickSelectSize) {
        this(pivotingStrategy, DUAL_PIVOTING_STRATEGY, minQuickSelectSize,
            EDGESELECT_CONSTANT, SUBSAMPLING_SIZE);
    }

    /**
     * Constructor with specified pivoting strategy and quickselect size.
     *
     * <p>Used to test dual-pivot quicksort.
     *
     * @param dualPivotingStrategy Dual pivoting strategy to use.
     * @param minQuickSelectSize Minimum size for quickselect.
     */
    Partition(DualPivotingStrategy dualPivotingStrategy, int minQuickSelectSize) {
        this(PIVOTING_STRATEGY, dualPivotingStrategy, minQuickSelectSize,
            EDGESELECT_CONSTANT, SUBSAMPLING_SIZE);
    }

    /**
     * Constructor with specified pivoting strategy; quickselect size; and edgeselect configuration.
     *
     * <p>Used to test single-pivot quickselect.
     *
     * @param pivotingStrategy Pivoting strategy to use.
     * @param minQuickSelectSize Minimum size for quickselect.
     * @param edgeSelectConstant Length constant used for edge select distance from end threshold.
     * @param subSamplingSize Size threshold to use sub-sampling for single-pivot selection.
     */
    Partition(PivotingStrategy pivotingStrategy,
        int minQuickSelectSize, int edgeSelectConstant, int subSamplingSize) {
        this(pivotingStrategy, DUAL_PIVOTING_STRATEGY, minQuickSelectSize, edgeSelectConstant,
            subSamplingSize);
    }

    /**
     * Constructor with specified dual-pivoting strategy; quickselect size; and edgeselect configuration.
     *
     * <p>Used to test dual-pivot quickselect.
     *
     * @param dualPivotingStrategy Dual pivoting strategy to use.
     * @param minQuickSelectSize Minimum size for quickselect.
     * @param edgeSelectConstant Length constant used for edge select distance from end threshold.
     */
    Partition(DualPivotingStrategy dualPivotingStrategy,
        int minQuickSelectSize, int edgeSelectConstant) {
        this(PIVOTING_STRATEGY, dualPivotingStrategy, minQuickSelectSize,
            edgeSelectConstant, SUBSAMPLING_SIZE);
    }

    /**
     * Constructor with specified pivoting strategy; quickselect size; and edgeselect configuration.
     *
     * @param pivotingStrategy Pivoting strategy to use.
     * @param dualPivotingStrategy Dual pivoting strategy to use.
     * @param minQuickSelectSize Minimum size for quickselect.
     * @param edgeSelectConstant Length constant used for distance from end threshold.
     * @param subSamplingSize Size threshold to use sub-sampling for single-pivot selection.
     */
    Partition(PivotingStrategy pivotingStrategy, DualPivotingStrategy dualPivotingStrategy,
        int minQuickSelectSize, int edgeSelectConstant, int subSamplingSize) {
        this.pivotingStrategy = pivotingStrategy;
        this.dualPivotingStrategy = dualPivotingStrategy;
        this.minQuickSelectSize = minQuickSelectSize;
        this.edgeSelectConstant = edgeSelectConstant;
        this.subSamplingSize = subSamplingSize;
        // Default strategies
        setSPStrategy(SP_STRATEGY);
        setEdgeSelectStrategy(EDGE_STRATEGY);
        setStopperStrategy(STOPPER_STRATEGY);
        setExpandStrategy(EXPAND_STRATEGY);
        setLinearStrategy(LINEAR_STRATEGY);
        // Called to initialise state
        setControlFlags(0);
    }

    /**
     * Sets the single-pivot partition strategy.
     *
     * @param v Value.
     * @return {@code this} for chaining
     */
    Partition setSPStrategy(SPStrategy v) {
        switch (v) {
        case BM:
            spFunction = Partition::partitionBM;
            break;
        case DNF1:
            spFunction = Partition::partitionDNF1;
            break;
        case DNF2:
            spFunction = Partition::partitionDNF2;
            break;
        case DNF3:
            spFunction = Partition::partitionDNF3;
            break;
        case KBM:
            spFunction = Partition::partitionKBM;
            break;
        case SBM:
            spFunction = Partition::partitionSBM;
            break;
        case SP:
            spFunction = Partition::partitionSP;
            break;
        default:
            throw new IllegalArgumentException("Unknown single-pivot strategy: " + v);
        }
        return this;
    }

    /**
     * Sets the single-pivot partition expansion strategy.
     *
     * @param v Value.
     * @return {@code this} for chaining
     */
    Partition setExpandStrategy(ExpandStrategy v) {
        switch (v) {
        case PER:
            // Partition the entire range using the single-pivot partition strategy
            expandFunction = (a, left, right, start, end, pivot0, pivot1, upper) ->
                spFunction.partition(a, left, right, (pivot0 + pivot1) >>> 1, upper);
            break;
        case T1:
            expandFunction = Partition::expandPartitionT1;
            break;
        case B1:
            expandFunction = Partition::expandPartitionB1;
            break;
        case T2:
            expandFunction = Partition::expandPartitionT2;
            break;
        case B2:
            expandFunction = Partition::expandPartitionB2;
            break;
        default:
            throw new IllegalArgumentException("Unknown expand strategy: " + v);
        }
        return this;
    }

    /**
     * Sets the single-pivot linear select strategy.
     *
     * <p>Note: The linear select strategy will partition remaining range after computing
     * a pivot from a sample by single-pivot partitioning or by expanding the partition
     * (see {@link #setExpandStrategy(ExpandStrategy)}).
     *
     * @param v Value.
     * @return {@code this} for chaining
     * @see #setExpandStrategy(ExpandStrategy)
     */
    Partition setLinearStrategy(LinearStrategy v) {
        switch (v) {
        case BFPRT:
            linearSpFunction = this::linearBFPRTBaseline;
            break;
        case RS:
            linearSpFunction = this::linearRepeatedStepBaseline;
            break;
        case BFPRT_IM:
            noSamplingAdapt = MapDistance.MEDIAN;
            linearSpFunction = this::linearBFPRTImproved;
            break;
        case BFPRTA:
            // Here we re-use the same method as the only difference is adaption of k
            noSamplingAdapt = MapDistance.ADAPT;
            linearSpFunction = this::linearBFPRTImproved;
            break;
        case RS_IM:
            noSamplingAdapt = MapDistance.MEDIAN;
            linearSpFunction = this::linearRepeatedStepImproved;
            break;
        case RSA:
            // Here we re-use the same method as the only difference is adaption of k
            noSamplingAdapt = MapDistance.ADAPT;
            linearSpFunction = this::linearRepeatedStepImproved;
            break;
        default:
            throw new IllegalArgumentException("Unknown linear strategy: " + v);
        }
        return this;
    }

    /**
     * Sets the edge-select strategy.
     *
     * @param v Value.
     * @return {@code this} for chaining
     */
    Partition setEdgeSelectStrategy(EdgeSelectStrategy v) {
        switch (v) {
        case ESH:
            edgeSelection = Partition::heapSelectRange;
            break;
        case ESH2:
            edgeSelection = Partition::heapSelectRange2;
            break;
        case ESS:
            edgeSelection = Partition::sortSelectRange;
            break;
        case ESS2:
            edgeSelection = Partition::sortSelectRange2;
            break;
        default:
            throw new IllegalArgumentException("Unknown edge select: " + v);
        }
        return this;
    }

    /**
     * Sets the stopper strategy (when quickselect progress is poor).
     *
     * @param v Value.
     * @return {@code this} for chaining
     */
    Partition setStopperStrategy(StopperStrategy v) {
        switch (v) {
        case SSH:
            stopperSelection = Partition::heapSelectRange;
            break;
        case SSH2:
            stopperSelection = Partition::heapSelectRange2;
            break;
        case SLS:
            // Linear select does not match the interface as it:
            // - requires the single-pivot partition function
            // - uses a bounds array to allow minimising the partition region size after pivot selection
            stopperSelection = (a, l, r, ka, kb) -> linearSelect(getSPFunction(),
                a, l, r, ka, kb, new int[2]);
            break;
        case SQA:
            // Linear select does not match the interface as it:
            // - uses a bounds array to allow minimising the partition region size after pivot selection
            // - uses control flags to set sampling mode on/off
            stopperSelection = (a, l, r, ka, kb) -> quickSelectAdaptive(a, l, r, ka, kb, new int[1],
                adaptMode);
            break;
        default:
            throw new IllegalArgumentException("Unknown stopper: " + v);
        }
        return this;
    }

    /**
     * Sets the key strategy.
     *
     * @param v Value.
     * @return {@code this} for chaining
     */
    Partition setKeyStrategy(KeyStrategy v) {
        this.keyStrategy = v;
        return this;
    }

    /**
     * Sets the paired key strategy.
     *
     * @param v Value.
     * @return {@code this} for chaining
     */
    Partition setPairedKeyStrategy(PairedKeyStrategy v) {
        this.pairedKeyStrategy = v;
        return this;
    }

    /**
     * Sets the recursion multiple.
     *
     * @param v Value.
     * @return {@code this} for chaining
     */
    Partition setRecursionMultiple(double v) {
        this.recursionMultiple = v;
        return this;
    }

    /**
     * Sets the recursion constant.
     *
     * @param v Value.
     * @return {@code this} for chaining
     */
    Partition setRecursionConstant(int v) {
        this.recursionConstant = v;
        return this;
    }

    /**
     * Sets the compression for a {@link CompressedIndexSet}.
     *
     * @param v Value.
     * @return {@code this} for chaining
     */
    Partition setCompression(int v) {
        if (v < 1 || v > Integer.SIZE - 1) {
            throw new IllegalArgumentException("Bad compression: " + v);
        }
        this.compression = v;
        return this;
    }

    /**
     * Sets the control flags for Floyd-Rivest sub-sampling.
     *
     * @param v Value.
     * @return {@code this} for chaining
     */
    Partition setControlFlags(int v) {
        this.controlFlags = v;
        // Quickselect adaptive requires functions to map k to the sample.
        // These functions must be set based on the margins in the repeated step method.
        // These will differ due to the implementation and whether the first step is
        // skipped (sampling mode on).
        if ((v & FLAG_QA_FAR_STEP_ADAPT_ORIGINAL) != 0) {
            // Use the same mapping for all repeated step functions.
            // This is the original behaviour from Alexandrescu (2016).
            samplingAdapt = samplingEdgeAdapt = noSamplingAdapt = noSamplingEdgeAdapt = MapDistance.ADAPT;
        } else {
            // Default behaviour. This optimises the adaption for the algorithm.
            samplingAdapt = MapDistance.ADAPT;
            if ((v & FLAG_QA_FAR_STEP) != 0) {
                // Switches the far-step to minimum-of-4, median-of-3.
                // When sampling mode is on all samples are from median-of-3 and we
                // use the same adaption.
                samplingEdgeAdapt = MapDistance.ADAPT;
            } else {
                // Original far-step of lower-median-of-4, minimum-of-3
                // When sampling mode is on the sample is a minimum-of-3. This halves the
                // lower margin from median-of-3. Change the adaption to avoid
                // a tiny lower margin (and possibility of k falling in a very large partition).
                // Note: The only way we can ensure that k is inside the lower margin is by using
                // (r-l) as the sample k. Compromise by using the midpoint for a 50% chance that
                // k is inside the lower margin.
                samplingEdgeAdapt = MapDistance.MEDIAN;
            }
            noSamplingAdapt = MapDistance.ADAPT;
            // Force edge margin to contain the target index
            noSamplingEdgeAdapt = MapDistance.EDGE_ADAPT;
        }
        return this;
    }

    /**
     * Sets the size for sortselect for the linearselect algorithm.
     * Must be above 0 for the algorithm to return (else an infinite loop occurs).
     * The minimum size required depends on the expand partition function, and the
     * same size relative to the range (e.g. 1/5, 1/9 or 1/12).
     *
     * @param v Value.
     * @return {@code this} for chaining
     */
    Partition setLinearSortSelectSize(int v) {
        if (v < 1) {
            throw new IllegalArgumentException("Bad linear sortselect size: " + v);
        }
        this.linearSortSelectSize = v;
        return this;
    }

    /**
     * Sets the quickselect adaptive mode.
     *
     * @param v Value.
     * @return {@code this} for chaining
     */
    Partition setAdaptMode(AdaptMode v) {
        this.adaptMode = v;
        return this;
    }

    /**
     * Sets the recursion consumer. This is called with the value of the recursion
     * counter immediately before the introselect routine returns. It is used to
     * analyse recursion depth on various input data.
     *
     * @param v Value.
     */
    void setRecursionConsumer(IntConsumer v) {
        this.recursionConsumer = Objects.requireNonNull(v);
    }

    /**
     * Gets the single-pivot partition function.
     *
     * @return the single-pivot partition function
     */
    SPEPartition getSPFunction() {
        return spFunction;
    }

    /**
     * Configure the properties used by the static quickselect adaptive algorithm.
     * The increment is used to update the current mode when the margins are not achieved.
     *
     * @param mode Initial mode.
     * @param increment Flag increment
     */
    static void configureQaAdaptive(int mode, int increment) {
        qaMode = mode;
        qaIncrement = increment;
    }

    /**
     * Move the minimum value to the start of the range.
     *
     * <p>Note: Respects the ordering of signed zeros.
     *
     * <p>Assumes {@code left <= right}.
     *
     * @param data Data.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     */
    static void selectMin(double[] data, int left, int right) {
        selectMinIgnoreZeros(data, left, right);
        // Edge-case: if min was 0.0, check for a -0.0 above and swap.
        if (data[left] == 0) {
            minZero(data, left, right);
        }
    }

    /**
     * Move the maximum value to the end of the range.
     *
     * <p>Note: Respects the ordering of signed zeros.
     *
     * <p>Assumes {@code left <= right}.
     *
     * @param data Data.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     */
    static void selectMax(double[] data, int left, int right) {
        selectMaxIgnoreZeros(data, left, right);
        // Edge-case: if max was -0.0, check for a 0.0 below and swap.
        if (data[right] == 0) {
            maxZero(data, left, right);
        }
    }

    /**
     * Place a negative signed zero at {@code left} before any positive signed zero in the range,
     * {@code -0.0 < 0.0}.
     *
     * <p>Warning: Only call when {@code data[left]} is zero.
     *
     * @param data Data.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     */
    private static void minZero(double[] data, int left, int right) {
        // Assume data[left] is zero and check the sign bit
        if (Double.doubleToRawLongBits(data[left]) >= 0) {
            // Check for a -0.0 above and swap.
            // We only require 1 swap as this is not a full sort of zeros.
            for (int k = left; ++k <= right;) {
                if (data[k] == 0 && Double.doubleToRawLongBits(data[k]) < 0) {
                    data[k] = 0.0;
                    data[left] = -0.0;
                    break;
                }
            }
        }
    }

    /**
     * Place a positive signed zero at {@code right} after any negative signed zero in the range,
     * {@code -0.0 < 0.0}.
     *
     * <p>Warning: Only call when {@code data[right]} is zero.
     *
     * @param data Data.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     */
    private static void maxZero(double[] data, int left, int right) {
        // Assume data[right] is zero and check the sign bit
        if (Double.doubleToRawLongBits(data[right]) < 0) {
            // Check for a 0.0 below and swap.
            // We only require 1 swap as this is not a full sort of zeros.
            for (int k = right; --k >= left;) {
                if (data[k] == 0 && Double.doubleToRawLongBits(data[k]) >= 0) {
                    data[k] = -0.0;
                    data[right] = 0.0;
                    break;
                }
            }
        }
    }

    /**
     * Move the minimum value to the start of the range.
     *
     * <p>Assumes {@code left <= right}.
     *
     * @param data Data.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     */
    static void selectMinIgnoreZeros(double[] data, int left, int right) {
        // Mitigate worst case performance on descending data by backward sweep
        double min = data[left];
        for (int i = right + 1; --i > left;) {
            final double v = data[i];
            if (v < min) {
                data[i] = min;
                min = v;
            }
        }
        data[left] = min;
    }

    /**
     * Move the two smallest values to the start of the range.
     *
     * <p>Assumes {@code left < right}.
     *
     * @param data Data.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     */
    static void selectMin2IgnoreZeros(double[] data, int left, int right) {
        double min1 = data[left + 1];
        if (min1 < data[left]) {
            min1 = data[left];
            data[left] = data[left + 1];
        }
        // Mitigate worst case performance on descending data by backward sweep
        for (int i = right + 1, end = left + 1; --i > end;) {
            final double v = data[i];
            if (v < min1) {
                data[i] = min1;
                if (v < data[left]) {
                    min1 = data[left];
                    data[left] = v;
                } else {
                    min1 = v;
                }
            }
        }
        data[left + 1] = min1;
    }

    /**
     * Move the maximum value to the end of the range.
     *
     * <p>Assumes {@code left <= right}.
     *
     * @param data Data.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     */
    static void selectMaxIgnoreZeros(double[] data, int left, int right) {
        // Mitigate worst case performance on descending data by backward sweep
        double max = data[right];
        for (int i = left - 1; ++i < right;) {
            final double v = data[i];
            if (v > max) {
                data[i] = max;
                max = v;
            }
        }
        data[right] = max;
    }

    /**
     * Move the two largest values to the end of the range.
     *
     * <p>Assumes {@code left < right}.
     *
     * @param data Data.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     */
    static void selectMax2IgnoreZeros(double[] data, int left, int right) {
        double max1 = data[right - 1];
        if (max1 > data[right]) {
            max1 = data[right];
            data[right] = data[right - 1];
        }
        // Mitigate worst case performance on descending data by backward sweep
        for (int i = left - 1, end = right - 1; ++i < end;) {
            final double v = data[i];
            if (v > max1) {
                data[i] = max1;
                if (v > data[right]) {
                    max1 = data[right];
                    data[right] = v;
                } else {
                    max1 = v;
                }
            }
        }
        data[right - 1] = max1;
    }

    /**
     * Sort the elements using a heap sort algorithm.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     */
    static void heapSort(double[] a, int left, int right) {
        // We could make a choice here between select left or right
        heapSelectLeft(a, left, right, right, right - left);
    }

    /**
     * Partition the elements {@code ka} and {@code kb} using a heap select algorithm. It
     * is assumed {@code left <= ka <= kb <= right}. Any range between the two elements is
     * not ensured to be sorted.
     *
     * <p>If there is no range between the two point, i.e. {@code ka == kb} or
     * {@code ka + 1 == kb}, it is preferred to use
     * {@link #heapSelectRange(double[], int, int, int, int)}. The result is the same but
     * the decision choice is simpler for the range function.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param ka Lower index to select.
     * @param kb Upper index to select.
     * @see #heapSelectRange(double[], int, int, int, int)
     */
    static void heapSelectPair(double[] a, int left, int right, int ka, int kb) {
        // Avoid the overhead of heap select on tiny data (supports right <= left).
        if (right - left < MIN_HEAPSELECT_SIZE) {
            Sorting.sort(a, left, right);
            return;
        }
        // Call the appropriate heap partition function based on
        // building a heap up to 50% of the length
        // |l|-----|ka|--------|kb|------|r|
        //  ---d1----
        //                      -----d3----
        //  ---------d2----------
        //          ----------d4-----------
        final int d1 = ka - left;
        final int d2 = kb - left;
        final int d3 = right - kb;
        final int d4 = right - ka;
        if (d1 + d3 < Math.min(d2, d4)) {
            // Partition both ends.
            // Note: Not possible if ka == kb.
            // s1 + s3 == r - l and >= than the smallest
            // distance to one of the ends
            heapSelectLeft(a, left, right, ka, 0);
            // Repeat for the other side above ka
            heapSelectRight(a, ka + 1, right, kb, 0);
        } else if (d2 < d4) {
            heapSelectLeft(a, left, right, kb, kb - ka);
        } else {
            // s4
            heapSelectRight(a, left, right, ka, kb - ka);
        }
    }

    /**
     * Partition the elements between {@code ka} and {@code kb} using a heap select
     * algorithm. It is assumed {@code left <= ka <= kb <= right}.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param ka Lower index to select.
     * @param kb Upper index to select.
     * @see #heapSelectPair(double[], int, int, int, int)
     */
    static void heapSelectRange(double[] a, int left, int right, int ka, int kb) {
        // Combine the test for right <= left with
        // avoiding the overhead of heap select on tiny data.
        if (right - left < MIN_HEAPSELECT_SIZE) {
            Sorting.sort(a, left, right);
            return;
        }
        // Call the appropriate heap partition function based on
        // building a heap up to 50% of the length
        // |l|-----|ka|--------|kb|------|r|
        // |---------d1-----------|
        //         |----------d2-----------|
        // Note: Optimisation for small heap size (n=1,2) is negligible.
        // The main overhead is the test for insertion against the current top of the heap
        // which grows increasingly unlikely as the range is scanned.
        if (kb - left < right - ka) {
            heapSelectLeft(a, left, right, kb, kb - ka);
        } else {
            heapSelectRight(a, left, right, ka, kb - ka);
        }
    }

    /**
     * Partition the minimum {@code n} elements below {@code k} where
     * {@code n = k - left + 1}. Uses a heap select algorithm.
     *
     * <p>Works with any {@code k} in the range {@code left <= k <= right}
     * and can be used to perform a full sort of the range below {@code k}
     * using the {@code count} parameter.
     *
     * <p>For best performance this should be called with
     * {@code k - left < right - k}, i.e.
     * to partition a value in the lower half of the range.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param k Index to select.
     * @param count Size of range to sort below k.
     */
    static void heapSelectLeft(double[] a, int left, int right, int k, int count) {
        // Create a max heap in-place in [left, k], rooted at a[left] = max
        // |l|-max-heap-|k|--------------|
        // Build the heap using Floyd's heap-construction algorithm for heap size n.
        // Start at parent of the last element in the heap (k),
        // i.e. start = parent(n-1) : parent(c) = floor((c - 1) / 2) : c = k - left
        int end = k + 1;
        for (int p = left + ((k - left - 1) >> 1); p >= left; p--) {
            maxHeapSiftDown(a, a[p], p, left, end);
        }
        // Scan the remaining data and insert
        // Mitigate worst case performance on descending data by backward sweep
        double max = a[left];
        for (int i = right + 1; --i > k;) {
            final double v = a[i];
            if (v < max) {
                a[i] = max;
                maxHeapSiftDown(a, v, left, left, end);
                max = a[left];
            }
        }

        // To partition elements k (and below) move the top of the heap to the position
        // immediately after the end of the reduced size heap; the previous end
        // of the heap [k] is placed at the top
        // |l|-max-heap-|k|--------------|
        //  |  <-swap->  |
        // The heap can be restored by sifting down the new top.

        // Always require the top 1
        a[left] = a[k];
        a[k] = max;

        if (count > 0) {
            --end;
            // Sifting limited to heap size of 2 (i.e. don't sift heap n==1)
            for (int c = Math.min(count, end - left - 1); --c >= 0;) {
                maxHeapSiftDown(a, a[left], left, left, end--);
                // Move top of heap to the sorted end
                max = a[left];
                a[left] = a[end];
                a[end] = max;
            }
        }
    }

    /**
     * Sift the element down the max heap.
     *
     * <p>Assumes {@code root <= p < end}, i.e. the max heap is above root.
     *
     * @param a Heap data.
     * @param v Value to sift.
     * @param p Start position.
     * @param root Root of the heap.
     * @param end End of the heap (exclusive).
     */
    private static void maxHeapSiftDown(double[] a, double v, int p, int root, int end) {
        // child2 = root + 2 * (parent - root) + 2
        //        = 2 * parent - root + 2
        while (true) {
            // Right child
            int c = (p << 1) - root + 2;
            if (c > end) {
                // No left child
                break;
            }
            // Use the left child if right doesn't exist, or it is greater
            if (c == end || a[c] < a[c - 1]) {
                --c;
            }
            if (v >= a[c]) {
                // Parent greater than largest child - done
                break;
            }
            // Swap and descend
            a[p] = a[c];
            p = c;
        }
        a[p] = v;
    }

    /**
     * Partition the maximum {@code n} elements above {@code k} where
     * {@code n = right - k + 1}. Uses a heap select algorithm.
     *
     * <p>Works with any {@code k} in the range {@code left <= k <= right}
     * and can be used to perform a full sort of the range above {@code k}
     * using the {@code count} parameter.
     *
     * <p>For best performance this should be called with
     * {@code k - left > right - k}, i.e.
     * to partition a value in the upper half of the range.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param k Index to select.
     * @param count Size of range to sort below k.
     */
    static void heapSelectRight(double[] a, int left, int right, int k, int count) {
        // Create a min heap in-place in [k, right], rooted at a[right] = min
        // |--------------|k|-min-heap-|r|
        // Build the heap using Floyd's heap-construction algorithm for heap size n.
        // Start at parent of the last element in the heap (k),
        // i.e. start = parent(n-1) : parent(c) = floor((c - 1) / 2) : c = right - k
        int end = k - 1;
        for (int p = right - ((right - k - 1) >> 1); p <= right; p++) {
            minHeapSiftDown(a, a[p], p, right, end);
        }
        // Scan the remaining data and insert
        // Mitigate worst case performance on descending data by backward sweep
        double min = a[right];
        for (int i = left - 1; ++i < k;) {
            final double v = a[i];
            if (v > min) {
                a[i] = min;
                minHeapSiftDown(a, v, right, right, end);
                min = a[right];
            }
        }

        // To partition elements k (and above) move the top of the heap to the position
        // immediately before the end of the reduced size heap; the previous end
        // of the heap [k] is placed at the top.
        // |--------------|k|-min-heap-|r|
        //                 |  <-swap->  |
        // The heap can be restored by sifting down the new top.

        // Always require the top 1
        a[right] = a[k];
        a[k] = min;

        if (count > 0) {
            ++end;
            // Sifting limited to heap size of 2 (i.e. don't sift heap n==1)
            for (int c = Math.min(count, right - end - 1); --c >= 0;) {
                minHeapSiftDown(a, a[right], right, right, end++);
                // Move top of heap to the sorted end
                min = a[right];
                a[right] = a[end];
                a[end] = min;
            }
        }
    }

    /**
     * Sift the element down the min heap.
     *
     * <p>Assumes {@code root >= p > end}, i.e. the max heap is below root.
     *
     * @param a Heap data.
     * @param v Value to sift.
     * @param p Start position.
     * @param root Root of the heap.
     * @param end End of the heap (exclusive).
     */
    private static void minHeapSiftDown(double[] a, double v, int p, int root, int end) {
        // child2 = root - 2 * (root - parent) - 2
        //        = 2 * parent - root - 2
        while (true) {
            // Right child
            int c = (p << 1) - root - 2;
            if (c < end) {
                // No left child
                break;
            }
            // Use the left child if right doesn't exist, or it is less
            if (c == end || a[c] > a[c + 1]) {
                ++c;
            }
            if (v <= a[c]) {
                // Parent less than smallest child - done
                break;
            }
            // Swap and descend
            a[p] = a[c];
            p = c;
        }
        a[p] = v;
    }

    /**
     * Partition the elements between {@code ka} and {@code kb} using a heap select
     * algorithm. It is assumed {@code left <= ka <= kb <= right}.
     *
     * <p>Differs from {@link #heapSelectRange(double[], int, int, int, int)} by using
     * a different extraction of the sorted elements from the heap.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param ka Lower index to select.
     * @param kb Upper index to select.
     * @see #heapSelectPair(double[], int, int, int, int)
     */
    static void heapSelectRange2(double[] a, int left, int right, int ka, int kb) {
        // Combine the test for right <= left with
        // avoiding the overhead of heap select on tiny data.
        if (right - left < MIN_HEAPSELECT_SIZE) {
            Sorting.sort(a, left, right);
            return;
        }
        // Use the smallest heap
        if (kb - left < right - ka) {
            heapSelectLeft2(a, left, right, ka, kb);
        } else {
            heapSelectRight2(a, left, right, ka, kb);
        }
    }

    /**
     * Partition the elements between {@code ka} and {@code kb} using a heap select
     * algorithm. It is assumed {@code left <= ka <= kb <= right}.
     *
     * <p>For best performance this should be called with {@code k} in the lower
     * half of the range.
     *
     * <p>Differs from {@link #heapSelectLeft(double[], int, int, int, int)} by using
     * a different extraction of the sorted elements from the heap.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param ka Lower index to select.
     * @param kb Upper index to select.
     */
    static void heapSelectLeft2(double[] a, int left, int right, int ka, int kb) {
        // Create a max heap in-place in [left, k], rooted at a[left] = max
        // |l|-max-heap-|k|--------------|
        // Build the heap using Floyd's heap-construction algorithm for heap size n.
        // Start at parent of the last element in the heap (k),
        // i.e. start = parent(n-1) : parent(c) = floor((c - 1) / 2) : c = k - left
        int end = kb + 1;
        for (int p = left + ((kb - left - 1) >> 1); p >= left; p--) {
            maxHeapSiftDown(a, a[p], p, left, end);
        }
        // Scan the remaining data and insert
        // Mitigate worst case performance on descending data by backward sweep
        double max = a[left];
        for (int i = right + 1; --i > kb;) {
            final double v = a[i];
            if (v < max) {
                a[i] = max;
                maxHeapSiftDown(a, v, left, left, end);
                max = a[left];
            }
        }
        // Partition [ka, kb]
        // |l|-max-heap-|k|--------------|
        //  |  <-swap->  |   then sift down reduced size heap
        // Avoid sifting heap of size 1
        final int last = Math.max(left, ka - 1);
        while (--end > last) {
            maxHeapSiftDown(a, a[end], left, left, end);
            a[end] = max;
            max = a[left];
        }
    }

    /**
     * Partition the elements between {@code ka} and {@code kb} using a heap select
     * algorithm. It is assumed {@code left <= ka <= kb <= right}.
     *
     * <p>For best performance this should be called with {@code k} in the upper
     * half of the range.
     *
     * <p>Differs from {@link #heapSelectRight(double[], int, int, int, int)} by using
     * a different extraction of the sorted elements from the heap.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param ka Lower index to select.
     * @param kb Upper index to select.
     */
    static void heapSelectRight2(double[] a, int left, int right, int ka, int kb) {
        // Create a min heap in-place in [k, right], rooted at a[right] = min
        // |--------------|k|-min-heap-|r|
        // Build the heap using Floyd's heap-construction algorithm for heap size n.
        // Start at parent of the last element in the heap (k),
        // i.e. start = parent(n-1) : parent(c) = floor((c - 1) / 2) : c = right - k
        int end = ka - 1;
        for (int p = right - ((right - ka - 1) >> 1); p <= right; p++) {
            minHeapSiftDown(a, a[p], p, right, end);
        }
        // Scan the remaining data and insert
        // Mitigate worst case performance on descending data by backward sweep
        double min = a[right];
        for (int i = left - 1; ++i < ka;) {
            final double v = a[i];
            if (v > min) {
                a[i] = min;
                minHeapSiftDown(a, v, right, right, end);
                min = a[right];
            }
        }
        // Partition [ka, kb]
        // |--------------|k|-min-heap-|r|
        //                 |  <-swap->  |   then sift down reduced size heap
        // Avoid sifting heap of size 1
        final int last = Math.min(right, kb + 1);
        while (++end < last) {
            minHeapSiftDown(a, a[end], right, right, end);
            a[end] = min;
            min = a[right];
        }
    }

    /**
     * Partition the elements between {@code ka} and {@code kb} using a sort select
     * algorithm. It is assumed {@code left <= ka <= kb <= right}.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param ka Lower index to select.
     * @param kb Upper index to select.
     */
    static void sortSelectRange(double[] a, int left, int right, int ka, int kb) {
        // Combine the test for right <= left with
        // avoiding the overhead of sort select on tiny data.
        if (right - left <= MIN_SORTSELECT_SIZE) {
            Sorting.sort(a, left, right);
            return;
        }
        // Sort the smallest side
        if (kb - left < right - ka) {
            sortSelectLeft(a, left, right, kb);
        } else {
            sortSelectRight(a, left, right, ka);
        }
    }

    /**
     * Partition the minimum {@code n} elements below {@code k} where
     * {@code n = k - left + 1}. Uses an insertion sort algorithm.
     *
     * <p>Works with any {@code k} in the range {@code left <= k <= right}
     * and performs a full sort of the range below {@code k}.
     *
     * <p>For best performance this should be called with
     * {@code k - left < right - k}, i.e.
     * to partition a value in the lower half of the range.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param k Index to select.
     */
    static void sortSelectLeft(double[] a, int left, int right, int k) {
        // Sort
        for (int i = left; ++i <= k;) {
            final double v = a[i];
            // Move preceding higher elements above (if required)
            if (v < a[i - 1]) {
                int j = i;
                while (--j >= left && v < a[j]) {
                    a[j + 1] = a[j];
                }
                a[j + 1] = v;
            }
        }
        // Scan the remaining data and insert
        // Mitigate worst case performance on descending data by backward sweep
        double m = a[k];
        for (int i = right + 1; --i > k;) {
            final double v = a[i];
            if (v < m) {
                a[i] = m;
                int j = k;
                while (--j >= left && v < a[j]) {
                    a[j + 1] = a[j];
                }
                a[j + 1] = v;
                m = a[k];
            }
        }
    }

    /**
     * Partition the maximum {@code n} elements above {@code k} where
     * {@code n = right - k + 1}. Uses an insertion sort algorithm.
     *
     * <p>Works with any {@code k} in the range {@code left <= k <= right}
     * and can be used to perform a full sort of the range above {@code k}.
     *
     * <p>For best performance this should be called with
     * {@code k - left > right - k}, i.e.
     * to partition a value in the upper half of the range.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param k Index to select.
     */
    static void sortSelectRight(double[] a, int left, int right, int k) {
        // Sort
        for (int i = right; --i >= k;) {
            final double v = a[i];
            // Move succeeding lower elements below (if required)
            if (v > a[i + 1]) {
                int j = i;
                while (++j <= right && v > a[j]) {
                    a[j - 1] = a[j];
                }
                a[j - 1] = v;
            }
        }
        // Scan the remaining data and insert
        // Mitigate worst case performance on descending data by backward sweep
        double m = a[k];
        for (int i = left - 1; ++i < k;) {
            final double v = a[i];
            if (v > m) {
                a[i] = m;
                int j = k;
                while (++j <= right && v > a[j]) {
                    a[j - 1] = a[j];
                }
                a[j - 1] = v;
                m = a[k];
            }
        }
    }

    /**
     * Partition the elements between {@code ka} and {@code kb} using a sort select
     * algorithm. It is assumed {@code left <= ka <= kb <= right}.
     *
     * <p>Differs from {@link #sortSelectRange(double[], int, int, int, int)} by using
     * a pointer to a position in the sorted array to skip ahead during insertion.
     * This extra complexity does not improve performance.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param ka Lower index to select.
     * @param kb Upper index to select.
     */
    static void sortSelectRange2(double[] a, int left, int right, int ka, int kb) {
        // Combine the test for right <= left with
        // avoiding the overhead of sort select on tiny data.
        if (right - left <= MIN_SORTSELECT_SIZE) {
            Sorting.sort(a, left, right);
            return;
        }
        // Sort the smallest side
        if (kb - left < right - ka) {
            sortSelectLeft2(a, left, right, kb);
        } else {
            sortSelectRight2(a, left, right, ka);
        }
    }

    /**
     * Partition the minimum {@code n} elements below {@code k} where
     * {@code n = k - left + 1}. Uses an insertion sort algorithm.
     *
     * <p>Works with any {@code k} in the range {@code left <= k <= right}
     * and performs a full sort of the range below {@code k}.
     *
     * <p>For best performance this should be called with
     * {@code k - left < right - k}, i.e.
     * to partition a value in the lower half of the range.
     *
     * <p>Differs from {@link #sortSelectLeft(double[], int, int, int)} by using
     * a pointer to a position in the sorted array to skip ahead during insertion.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param k Index to select.
     */
    static void sortSelectLeft2(double[] a, int left, int right, int k) {
        // Sort
        for (int i = left; ++i <= k;) {
            final double v = a[i];
            // Move preceding higher elements above (if required)
            if (v < a[i - 1]) {
                int j = i;
                while (--j >= left && v < a[j]) {
                    a[j + 1] = a[j];
                }
                a[j + 1] = v;
            }
        }
        // Scan the remaining data and insert
        // Mitigate worst case performance on descending data by backward sweep
        double m = a[k];
        // Pointer to a position in the sorted array
        final int p = (left + k) >>> 1;
        for (int i = right + 1; --i > k;) {
            final double v = a[i];
            if (v < m) {
                a[i] = m;
                int j = k;
                if (v < a[p]) {
                    // Skip ahead
                    //System.arraycopy(a, p, a, p + 1, k - p);
                    while (j > p) {
                        // left index is evaluated before right decrement
                        a[j] = a[--j];
                    }
                    // j == p
                    while (--j >= left && v < a[j]) {
                        a[j + 1] = a[j];
                    }
                } else {
                    // No bounds check on left: a[p] <= v < a[k]
                    while (v < a[--j]) {
                        a[j + 1] = a[j];
                    }
                }
                a[j + 1] = v;
                m = a[k];
            }
        }
    }

    /**
     * Partition the maximum {@code n} elements above {@code k} where
     * {@code n = right - k + 1}. Uses an insertion sort algorithm.
     *
     * <p>Works with any {@code k} in the range {@code left <= k <= right}
     * and can be used to perform a full sort of the range above {@code k}.
     *
     * <p>For best performance this should be called with
     * {@code k - left > right - k}, i.e.
     * to partition a value in the upper half of the range.
     *
     * <p>Differs from {@link #sortSelectRight(double[], int, int, int)} by using
     * a pointer to a position in the sorted array to skip ahead during insertion.
     *
     * @param a Data array to use to find out the K<sup>th</sup> value.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param k Index to select.
     */
    static void sortSelectRight2(double[] a, int left, int right, int k) {
        // Sort
        for (int i = right; --i >= k;) {
            final double v = a[i];
            // Move succeeding lower elements below (if required)
            if (v > a[i + 1]) {
                int j = i;
                while (++j <= right && v > a[j]) {
                    a[j - 1] = a[j];
                }
                a[j - 1] = v;
            }
        }
        // Scan the remaining data and insert
        // Mitigate worst case performance on descending data by backward sweep
        double m = a[k];
        // Pointer to a position in the sorted array
        final int p = (right + k) >>> 1;
        for (int i = left - 1; ++i < k;) {
            final double v = a[i];
            if (v > m) {
                a[i] = m;
                int j = k;
                if (v > a[p]) {
                    // Skip ahead
                    //System.arraycopy(a, p, a, p - 1, p - k);
                    while (j < p) {
                        // left index is evaluated before right increment
                        a[j] = a[++j];
                    }
                    // j == p
                    while (++j <= right && v > a[j]) {
                        a[j - 1] = a[j];
                    }
                } else {
                    // No bounds check on right: a[k] < v <= a[p]
                    while (v > a[++j]) {
                        a[j - 1] = a[j];
                    }
                }
                a[j - 1] = v;
                m = a[k];
            }
        }
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array. For all indices {@code k}
     * and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>Note: This is the only quickselect method in this class not based on introselect.
     * This is a legacy method containing alternatives for iterating over
     * multiple keys that are not supported by introselect, namely:
     *
     * <ul>
     * <li>{@link KeyStrategy#SEQUENTIAL}: This method concatenates close indices into
     * ranges and processes them together. It should be able to identify ranges that
     * require a full sort. Start-up cost is higher. In practice the indices do not saturate
     * the range if the length is reasonable and it is typically possible to cut between indices
     * during partitioning to create regions that do not require visiting. Thus trying to identify
     * regions for a full sort is a waste of resources.
     * <li>{@link KeyStrategy#INDEX_SET}: Uses a {@code BitSet}-type structure to store
     * pivots during a call to partition. These can be used to bracket the search for the next index.
     * Storage of sparse indices is inefficient as it will require up to length bits of the memory
     * of the input array length. Sparse ranges cannot be efficiently searched.
     * <li>{@link KeyStrategy#PIVOT_CACHE}: The {@link PivotCache} interface abstracts
     * methods from a {@code BitSet}. Indices can be stored and searched. The abstraction allows
     * the pivots to be stored efficiently. However there are no sparse implementations
     * of the interface other than 1 or 2 points. So performance is similar to the INDEX_SET
     * method. One difference is the method finds the outer indices first and then
     * only searches the internal region for the rest of the indices. This makes no difference
     * to performance.
     * </ul>
     *
     * <p>Note: In each method indices are processed independently. Thus each bracket around an
     * index to partition does not know the number of recursion steps used to obtain the start
     * pivots defining the bracket. Excess recursion cannot be efficiently tracked for each
     * partition. This is unlike introselect which tracks recursion and can switch to algorithm
     * if quickselect convergence is slow.
     *
     * <p>Benchmarking can be used to show these alternatives are slower.
     *
     * @param part Partition function.
     * @param data Values.
     * @param right Upper bound of data (inclusive).
     * @param k Indices (may be destructively modified).
     * @param count Count of indices.
     */
    private void partition(PartitionFunction part, double[] data, int right, int[] k, int count) {
        if (count < 1 || right < 1) {
            return;
        }
        // Validate indices. Excludes indices > right.
        final int n = countIndices(k, count, right);
        if (n < 1) {
            return;
        }
        if (n == 1) {
            part.partition(data, 0, right, k[0], k[0], false, false);
        } else if (n == 2 && Math.abs(k[0] - k[1]) <= (minQuickSelectSize >>> 1)) {
            final int ka = Math.min(k[0], k[1]);
            final int kb = Math.max(k[0], k[1]);
            part.partition(data, 0, right, ka, kb, false, false);
        } else {
            // Allow non-sequential / sequential processing to be selected
            if (keyStrategy == KeyStrategy.SEQUENTIAL) {
                // Sequential processing
                final ScanningPivotCache pivots = keyAnalysis(right + 1, k, n, minQuickSelectSize >>> 1);
                if (k[0] == Integer.MIN_VALUE) {
                    // Full-sort recommended. Assume the partition function
                    // can choose to switch to using Arrays.sort.
                    part.sort(data, 0, right, false, false);
                } else {
                    partitionSequential(part, data, k, n, right, pivots);
                }
            } else if (keyStrategy == KeyStrategy.INDEX_SET) {
                // Non-sequential processing using non-optimised storage
                final IndexSet pivots = IndexSet.ofRange(0, right);
                // First index must partition the entire range
                part.partition(data, 0, right, k[0], k[0], false, false, pivots);
                for (int i = 1; i < n; i++) {
                    final int ki = k[i];
                    if (pivots.get(ki)) {
                        continue;
                    }
                    final int l = pivots.previousSetBit(ki);
                    int r = pivots.nextSetBit(ki);
                    if (r < 0) {
                        r = right + 1;
                    }
                    part.partition(data, l + 1, r - 1, ki, ki, l >= 0, r <= right, pivots);
                }
            } else if (keyStrategy == KeyStrategy.PIVOT_CACHE) {
                // Non-sequential processing using a pivot cache to optimise storage
                final PivotCache pivots = createPivotCacheForIndices(k, n);

                // Handle single-point or tiny range
                if ((pivots.right() - pivots.left()) <= (minQuickSelectSize >>> 1)) {
                    part.partition(data, 0, right, pivots.left(), pivots.right(), false, false);
                    return;
                }

                // Bracket the range so the rest is internal.
                // Note: Partition function handles min/max searching if ka/kb are
                // at the end of the range.
                final int ka = pivots.left();
                part.partition(data, 0, right, ka, ka, false, false, pivots);
                final int kb = pivots.right();
                int l = pivots.previousPivot(kb);
                int r = pivots.nextPivot(kb);
                if (r < 0) {
                    // Partition did not visit downstream
                    r = right + 1;
                }
                part.partition(data, l + 1, r - 1, kb, kb, true, r <= right, pivots);
                for (int i = 0; i < n; i++) {
                    final int ki = k[i];
                    if (pivots.contains(ki)) {
                        continue;
                    }
                    l = pivots.previousPivot(ki);
                    r = pivots.nextPivot(ki);
                    part.partition(data, l + 1, r - 1, ki, ki, true, true, pivots);
                }
            } else {
                throw new IllegalStateException("Unsupported: " + keyStrategy);
            }
        }
    }

    /**
     * Return a {@link PivotCache} implementation to support the range
     * {@code [left, right]} as defined by minimum and maximum index.
     *
     * @param indices Indices.
     * @param n Count of indices (must be strictly positive).
     * @return the pivot cache
     */
    private static PivotCache createPivotCacheForIndices(int[] indices, int n) {
        int min = indices[0];
        int max = min;
        for (int i = 1; i < n; i++) {
            final int k = indices[i];
            min = Math.min(min, k);
            max = Math.max(max, k);
        }
        return PivotCaches.ofFullRange(min, max);
    }

    /**
     * Analysis of keys to partition. The indices k are updated in-place. The keys are
     * processed to eliminate duplicates and sorted in ascending order. Close points are
     * joined into ranges using the minimum separation. A zero or negative separation
     * prevents creating ranges.
     *
     * <p>On output the indices contain ranges or single points to partition in ascending
     * order. Single points are identified as negative values and should be bit-flipped
     * to the index value.
     *
     * <p>If compression occurs the result will contain fewer indices than {@code n}.
     * The end of the compressed range is marked using {@link Integer#MIN_VALUE}. This
     * is outside the valid range for any single index and signals to stop processing
     * the ordered indices.
     *
     * <p>A {@link PivotCache} implementation is returned for optimal bracketing
     * of indices in the range after the first target range / point.
     *
     * <p>Examples:
     *
     * <pre>{@code
     *                                                 [L, R] PivotCache
     * [3]                -> [3]                       -
     *
     * // min separation 0
     * [3, 4, 5]          -> [~3, ~4, ~5]              [4, 5]
     * [3, 4, 7, 8]       -> [~3, ~4, ~7, ~8]          [4, 8]
     *
     * // min separation 1
     * [3, 4, 5]          -> [3, 5, MIN_VALUE]         -
     * [3, 4, 5, 8]       -> [3, 5, ~8, MIN_VALUE]     [8]
     * [3, 4, 5, 6, 7, 8] -> [3, 8, MIN_VALUE, ...]    -
     * [3, 4, 7, 8]       -> [3, 4, 7, 8]              [7, 8]
     * [3, 4, 7, 8, 99]   -> [3, 4, 7, 8, ~99]         [7, 99]
     * }</pre>
     *
     * <p>The length of data to partition can be used to determine if processing is
     * required. A full sort of the data is recommended by returning
     * {@code k[0] == Integer.MIN_VALUE}. This occurs if the length is sufficiently small
     * or the first range to partition covers the entire data.
     *
     * <p>Note: The signal marker {@code Integer.MIN_VALUE} is {@code Integer.MAX_VALUE}
     * bit flipped. It this is outside the range of any valid index into an array.
     *
     * @param size Length of the data to partition.
     * @param k Indices.
     * @param n Count of indices (must be strictly positive).
     * @param minSeparation Minimum separation between points (set to zero to disable ranges).
     * @return the pivot cache
     */
    // package-private for testing
    ScanningPivotCache keyAnalysis(int size, int[] k, int n, int minSeparation) {
        // Tiny data, signal to sort it
        if (size < minQuickSelectSize) {
            k[0] = Integer.MIN_VALUE;
            return null;
        }
        // Sort the keys
        final IndexSet indices = Sorting.sortUnique(Math.max(6, minQuickSelectSize), k, n);
        // Find the max index
        int right = k[n - 1];
        if (right < 0) {
            right = ~right;
        }
        // Join up close keys using the min separation distance.
        final int left = compressRange(k, n, minSeparation);
        if (left < 0) {
            // Nothing to partition after the first target.
            // Recommend full sort if the range is effectively complete.
            // A range requires n > 1 and positive indices.
            if (n != 1 && k[0] >= 0 && size - (k[1] - k[0]) < minQuickSelectSize) {
                k[0] = Integer.MIN_VALUE;
            }
            return null;
        }
        // Return an optimal PivotCache to process keys in sorted order
        if (indices != null) {
            // Reuse storage from sorting large number of indices
            return indices.asScanningPivotCache(left, right);
        }
        return IndexSet.createScanningPivotCache(left, right);
    }

    /**
     * Compress sorted indices into ranges using the minimum separation.
     * Single points are identified by bit flipping to negative. The
     * first unused position after compression is set to {@link Integer#MIN_VALUE},
     * unless this is outside the array length (i.e. no compression).
     *
     * @param k Unique indices (sorted).
     * @param n Count of indices (must be strictly positive).
     * @param minSeparation Minimum separation between points.
     * @return the first index after the initial pair / point (or -1)
     */
    private static int compressRange(int[] k, int n, int minSeparation) {
        if (n == 1) {
            // Single point, mark the first unused position
            if (k.length > 1) {
                k[1] = Integer.MIN_VALUE;
            }
            return -1;
        }
        // Start of range is in k[j]; end in p2
        int j = 0;
        int p2 = k[0];
        int secondTarget = -1;
        for (int i = 0; ++i < n;) {
            if (k[i] < 0) {
                // Start of duplicate indices
                break;
            }
            if (k[i] <= p2 + minSeparation) {
                // Extend range
                p2 = k[i];
            } else {
                // Store range or point (bit flipped)
                if (k[j] == p2) {
                    k[j] = ~p2;
                } else {
                    k[++j] = p2;
                }
                j++;
                // Next range is k[j] to p2
                k[j] = p2 = k[i];
                // Set the position of the second target
                if (secondTarget < 0) {
                    secondTarget = p2;
                }
            }
        }
        // Store range or point (bit flipped)
        // Note: If there is only 1 range then the second target is -1
        if (k[j] == p2) {
            k[j] = ~p2;
        } else {
            k[++j] = p2;
        }
        j++;
        // Add a marker at the end of the compressed indices
        if (k.length > j) {
            k[j] = Integer.MIN_VALUE;
        }
        return secondTarget;
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array. For all indices {@code k}
     * and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>The keys must have been pre-processed by {@link #keyAnalysis(int, int[], int, int)}
     * to structure them for sequential processing.
     *
     * @param part Partition function.
     * @param data Values.
     * @param k Indices (created by key analysis).
     * @param n Count of indices.
     * @param right Upper bound (inclusive).
     * @param pivots Cache of pivots (created by key analysis).
     */
    private static void partitionSequential(PartitionFunction part, double[] data, int[] k, int n,
        int right, ScanningPivotCache pivots) {
        // Sequential processing of [s, s] single points / [s, e] pairs (regions).
        // Single-points are identified as negative indices.
        // The partition algorithm must run so each [s, e] is sorted:
        // lower---se----------------s---e---------upper
        // Pivots are stored to allow lower / upper to be set for the next region:
        // lower---se-------p--------s-p-e-----p---upper
        int i = 1;
        int s = k[0];
        int e;
        if (s < 0) {
            e = s = ~s;
        } else {
            e = k[i++];
        }

        // Key analysis has configured the pivot cache correctly for the first region.
        // If there is no cache, there is only 1 region.
        if (pivots == null) {
            part.partition(data, 0, right, s, e, false, false);
            return;
        }

        part.partitionSequential(data, 0, right, s, e, false, false, pivots);

        // Process remaining regions
        while (i < n) {
            s = k[i++];
            if (s < 0) {
                e = s = ~s;
            } else {
                e = k[i++];
            }
            if (s > right) {
                // End of indices
                break;
            }
            // Cases:
            // 1. l------s-----------r  Single point (s==e)
            // 2. l------se----------r  An adjacent pair of points
            // 3. l------s------e----r  A range of points (may contain internal pivots)
            // Find bounding region of range: [l, r)
            // Left (inclusive) is always above 0 as we have partitioned upstream already.
            // Right (exclusive) may not have been searched yet so we check right bounds.
            final int l = pivots.previousPivot(s);
            final int r = pivots.nextPivotOrElse(e, right + 1);

            // Create regions:
            // Partition: l------s--p1
            // Sort:                p1-----p2
            // Partition:                  p2-----e-----r
            // Look for internal pivots.
            int p1 = -1;
            int p2 = -1;
            if (e - s > 1) {
                final int p = pivots.nextPivot(s + 1);
                if (p > s && p < e) {
                    p1 = p;
                    p2 = pivots.previousPivot(e - 1);
                    if (p2 - p1 > SORT_BETWEEN_SIZE) {
                        // Special-case: multiple internal pivots
                        // Full-sort of (p1, p2). Walk the unsorted regions:
                        // l------s--p1                               p2----e-----r
                        //             ppppp-----pppp----pppp---------
                        //                  s1-e1    s1e1    s1-----e1
                        int e1 = pivots.previousNonPivot(p2);
                        while (p1 < e1) {
                            final int s1 = pivots.previousPivot(e1);
                            part.sort(data, s1 + 1, e1, true, true);
                            e1 = pivots.previousNonPivot(s1);
                        }
                    }
                }
            }

            // Pivots are only required for the next downstream region
            int sn = right + 1;
            if (i < n) {
                sn = k[i];
                if (sn < 0) {
                    sn = ~sn;
                }
            }
            // Current implementations will signal if this is outside the support.
            // Occurs on the last region the cache was created to support (i.e. sn > right).
            final boolean unsupportedCacheRange = !pivots.moveLeft(sn);

            // Note: The partition function uses inclusive left and right bounds
            // so use +/- 1 from pivot values. If r is not a pivot it is right + 1
            // which is a valid exclusive upper bound.

            if (p1 > s) {
                // At least 1 internal pivot:
                // l <= s < p1 and p2 < e <= r
                // If l == s or r == e these calls should fully sort the respective range
                part.partition(data, l + 1, p1 - 1, s, p1 - 1, true, p1 <= right);
                if (unsupportedCacheRange) {
                    part.partition(data, p2 + 1, r - 1, p2 + 1, e, true, r <= right);
                } else {
                    part.partitionSequential(data, p2 + 1, r - 1, p2 + 1, e, true, r <= right, pivots);
                }
            } else {
                // Single range
                if (unsupportedCacheRange) {
                    part.partition(data, l + 1, r - 1, s, e, true, r <= right);
                } else {
                    part.partitionSequential(data, l + 1, r - 1, s, e, true, r <= right, pivots);
                }
            }
        }
    }

    /**
     * Sort the data.
     *
     * <p>Uses a Bentley-McIlroy quicksort partition method. Signed zeros
     * are corrected when encountered during processing.
     *
     * @param data Values.
     */
    void sortSBM(double[] data) {
        // Handle NaN
        final int right = sortNaN(data);
        sort((SPEPartitionFunction) this::partitionSBMWithZeros, data, right);
    }

    /**
     * Sort the data by recursive partitioning (quicksort).
     *
     * @param part Partition function.
     * @param data Values.
     * @param right Upper bound (inclusive).
     */
    private static void sort(PartitionFunction part, double[] data, int right) {
        if (right < 1) {
            return;
        }
        // Signal entire range
        part.sort(data, 0, right, false, false);
    }

    /**
     * Sort the data using an introsort.
     *
     * <p>Uses the configured single-pivot partition method; falling back
     * to heapsort when quicksort recursion is slow.
     *
     * @param data Values.
     */
    void sortISP(double[] data) {
        // NaN processing is done in the introsort method
        introsort(getSPFunction(), data);
    }

    /**
     * Sort the array using an introsort. The single-pivot partition method is provided as an argument.
     * Switches to heapsort when recursive partitioning reaches a maximum depth.
     *
     * <p>The partition method is not required to handle signed zeros.
     *
     * @param part Partition function.
     * @param a Values.
     * @see <a href="https://en.wikipedia.org/wiki/Introsort">Introsort (Wikipedia)</a>
     */
    private void introsort(SPEPartition part, double[] a) {
        // Handle NaN / signed zeros
        final DoubleDataTransformer t = SORT_TRANSFORMER.get();
        // Assume this is in-place
        t.preProcess(a);
        final int end = t.length();
        if (end > 1) {
            introsort(part, a, 0, end - 1, createMaxDepthSinglePivot(end));
        }
        // Restore signed zeros
        t.postProcess(a);
    }

    /**
     * Sort the array.
     *
     * <p>Uses an introsort. The single-pivot partition method is provided as an argument.
     *
     * @param part Partition function.
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param maxDepth Maximum depth for recursion.
     * @see <a href="https://en.wikipedia.org/wiki/Introsort">Introsort (Wikipedia)</a>
     */
    private void introsort(SPEPartition part, double[] a, int left, int right, int maxDepth) {
        // Only one side requires recursion. The other side
        // can remain within this function call.
        final int l = left;
        int r = right;
        final int[] upper = {0};
        while (true) {
            // Full sort of small data
            if (r - l < minQuickSelectSize) {
                Sorting.sort(a, l, r);
                return;
            }
            if (maxDepth == 0) {
                // Too much recursion
                heapSort(a, l, r);
                return;
            }

            // Pick a pivot and partition
            final int p0 = part.partition(a, l, r,
                pivotingStrategy.pivotIndex(a, l, r, l),
                upper);
            final int p1 = upper[0];

            // Recurse right side
            introsort(part, a, p1 + 1, r, --maxDepth);
            // Continue on the left side
            r = p0 - 1;
        }
    }

    /**
     * Sort the data using an introsort.
     *
     * <p>Uses a dual-pivot quicksort method; falling back
     * to heapsort when quicksort recursion is slow.
     *
     * @param data Values.
     */
    void sortIDP(double[] data) {
        // NaN processing is done in the introsort method
        introsort((DPPartition) Partition::partitionDP, data);
    }

    /**
     * Sort the array using an introsort. The dual-pivot partition method is provided as an argument.
     * Switches to heapsort when recursive partitioning reaches a maximum depth.
     *
     * <p>The partition method is not required to handle signed zeros.
     *
     * @param part Partition function.
     * @param a Values.
     * @see <a href="https://en.wikipedia.org/wiki/Introsort">Introsort (Wikipedia)</a>
     */
    private void introsort(DPPartition part, double[] a) {
        // Handle NaN / signed zeros
        final DoubleDataTransformer t = SORT_TRANSFORMER.get();
        // Assume this is in-place
        t.preProcess(a);
        final int end = t.length();
        if (end > 1) {
            introsort(part, a, 0, end - 1, createMaxDepthDualPivot(end));
        }
        // Restore signed zeros
        t.postProcess(a);
    }

    /**
     * Sort the array.
     *
     * <p>Uses an introsort. The dual-pivot partition method is provided as an argument.
     *
     * @param part Partition function.
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param maxDepth Maximum depth for recursion.
     * @see <a href="https://en.wikipedia.org/wiki/Introsort">Introsort (Wikipedia)</a>
     */
    private void introsort(DPPartition part, double[] a, int left, int right, int maxDepth) {
        // Only two regions require recursion. The third region
        // can remain within this function call.
        final int l = left;
        int r = right;
        final int[] upper = {0, 0, 0};
        while (true) {
            // Full sort of small data
            if (r - l < minQuickSelectSize) {
                Sorting.sort(a, l, r);
                return;
            }
            if (maxDepth == 0) {
                // Too much recursion
                heapSort(a, l, r);
                return;
            }

            // Pick 2 pivots and partition
            int p0 = dualPivotingStrategy.pivotIndex(a, l, r, upper);
            p0 = part.partition(a, l, r, p0, upper[0], upper);
            final int p1 = upper[0];
            final int p2 = upper[1];
            final int p3 = upper[2];

            // Recurse middle and right sides
            --maxDepth;
            introsort(part, a, p3 + 1, r, maxDepth);
            introsort(part, a, p1 + 1, p2 - 1, maxDepth);
            // Continue on the left side
            r = p0 - 1;
        }
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array. For all indices {@code k}
     * and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>All indices are assumed to be within {@code [0, right]}.
     *
     * <p>Uses an introselect variant. The single-pivot quickselect is provided as an argument;
     * the fall-back on poor convergence of the quickselect is controlled by
     * current configuration.
     *
     * <p>The partition method is not required to handle signed zeros.
     *
     * @param part Partition function.
     * @param a Values.
     * @param k Indices (may be destructively modified).
     * @param count Count of indices (assumed to be strictly positive).
     */
    private void introselect(SPEPartition part, double[] a, int[] k, int count) {
        // Handle NaN / signed zeros
        final DoubleDataTransformer t = SORT_TRANSFORMER.get();
        // Assume this is in-place
        t.preProcess(a);
        final int end = t.length();
        int n = count;
        if (end > 1) {
            // Filter indices invalidated by NaN check
            if (end < a.length) {
                for (int i = n; --i >= 0;) {
                    final int v = k[i];
                    if (v >= end) {
                        // swap(k, i, --n)
                        k[i] = k[--n];
                        k[n] = v;
                    }
                }
            }
            introselect(part, a, end - 1, k, n);
        }
        // Restore signed zeros
        t.postProcess(a, k, n);
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array. For all indices {@code k}
     * and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>All indices are assumed to be within {@code [0, right]}.
     *
     * <p>Uses an introselect variant. The quickselect is provided as an argument;
     * the fall-back on poor convergence of the quickselect is controlled by
     * current configuration.
     *
     * @param part Partition function.
     * @param a Values.
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param k Indices (may be destructively modified).
     * @param n Count of indices (assumed to be strictly positive).
     */
    private void introselect(SPEPartition part, double[] a, int right, int[] k, int n) {
        if (n < 1) {
            return;
        }
        final int maxDepth = createMaxDepthSinglePivot(right + 1);
        // Handle cases without multiple keys
        if (n == 1) {
            // Dedicated methods for a single key. These use different strategies
            // to trigger the stopper on quickselect recursion
            if (pairedKeyStrategy == PairedKeyStrategy.PAIRED_KEYS) {
                introselect(part, a, 0, right, k[0], maxDepth);
            } else if (pairedKeyStrategy == PairedKeyStrategy.PAIRED_KEYS_2) {
                // This uses the configured recursion constant c.
                // The length must halve every c iterations.
                introselect2(part, a, 0, right, k[0]);
            } else if (pairedKeyStrategy == PairedKeyStrategy.PAIRED_KEYS_LEN) {
                introselect(part, a, 0, right, k[0]);
            } else if (pairedKeyStrategy == PairedKeyStrategy.TWO_KEYS) {
                // Dedicated method for two separate keys using the same key
                introselect(part, a, 0, right, k[0], k[0], maxDepth);
            } else if (pairedKeyStrategy == PairedKeyStrategy.KEY_RANGE) {
                // Dedicated method for a range of keys using the same key
                introselect2(part, a, 0, right, k[0], k[0]);
            } else if (pairedKeyStrategy == PairedKeyStrategy.SEARCHABLE_INTERVAL) {
                // Reuse the SearchableInterval method using the same key
                introselect(part, a, 0, right, IndexIntervals.anyIndex(), k[0], k[0], maxDepth);
            } else if (pairedKeyStrategy == PairedKeyStrategy.UPDATING_INTERVAL) {
                // Reuse the UpdatingInterval method using a single key
                introselect(part, a, 0, right, IndexIntervals.interval(k[0]), maxDepth);
            } else {
                throw new IllegalStateException(UNSUPPORTED_INTROSELECT + pairedKeyStrategy);
            }
            return;
        }
        // Special case for partition around adjacent indices (for interpolation)
        if (n == 2 && k[0] + 1 == k[1]) {
            // Dedicated method for a single key, returns information about k+1
            if (pairedKeyStrategy == PairedKeyStrategy.PAIRED_KEYS) {
                final int p = introselect(part, a, 0, right, k[0], maxDepth);
                // p <= k to signal k+1 is unsorted, or p+1 is a pivot.
                // if k is sorted, and p+1 is sorted, k+1 is sorted if k+1 == p.
                if (p > k[1]) {
                    selectMinIgnoreZeros(a, k[1], p);
                }
            } else if (pairedKeyStrategy == PairedKeyStrategy.PAIRED_KEYS_2) {
                final int p = introselect2(part, a, 0, right, k[0]);
                if (p > k[1]) {
                    selectMinIgnoreZeros(a, k[1], p);
                }
            } else if (pairedKeyStrategy == PairedKeyStrategy.PAIRED_KEYS_LEN) {
                final int p = introselect(part, a, 0, right, k[0]);
                if (p > k[1]) {
                    selectMinIgnoreZeros(a, k[1], p);
                }
            } else if (pairedKeyStrategy == PairedKeyStrategy.TWO_KEYS) {
                // Dedicated method for two separate keys
                // Note: This can handle keys that are not adjacent
                // e.g. keys near opposite ends without a partition step.
                final int ka = Math.min(k[0], k[1]);
                final int kb = Math.max(k[0], k[1]);
                introselect(part, a, 0, right, ka, kb, maxDepth);
            } else if (pairedKeyStrategy == PairedKeyStrategy.KEY_RANGE) {
                // Dedicated method for a range of keys using the same key
                final int ka = Math.min(k[0], k[1]);
                final int kb = Math.max(k[0], k[1]);
                introselect2(part, a, 0, right, ka, kb);
            } else if (pairedKeyStrategy == PairedKeyStrategy.SEARCHABLE_INTERVAL) {
                // Reuse the SearchableInterval method using a range of two keys
                introselect(part, a, 0, right, IndexIntervals.anyIndex(), k[0], k[1], maxDepth);
            } else if (pairedKeyStrategy == PairedKeyStrategy.UPDATING_INTERVAL) {
                // Reuse the UpdatingInterval method using a range of two keys
                introselect(part, a, 0, right, IndexIntervals.interval(k[0], k[1]), maxDepth);
            } else {
                throw new IllegalStateException(UNSUPPORTED_INTROSELECT + pairedKeyStrategy);
            }
            return;
        }

        // Note: Sorting to unique keys is an overhead. This can be eliminated
        // by requesting the caller passes sorted keys.

        // Note: Attempts to perform key analysis here to detect a full sort
        // add an overhead for sparse keys and do not increase performance
        // for saturated keys unless data is structured with ascending/descending
        // runs so that it is fast with JDK's merge sort algorithm in Arrays.sort.

        if (keyStrategy == KeyStrategy.ORDERED_KEYS) {
            final int unique = Sorting.sortIndices(k, n);
            introselect(part, a, 0, right, k, 0, unique - 1, maxDepth);
        } else if (keyStrategy == KeyStrategy.SCANNING_KEY_SEARCHABLE_INTERVAL) {
            final int unique = Sorting.sortIndices(k, n);
            final SearchableInterval keys = ScanningKeyInterval.of(k, unique);
            introselect(part, a, 0, right, keys, keys.left(), keys.right(), maxDepth);
        } else if (keyStrategy == KeyStrategy.SEARCH_KEY_SEARCHABLE_INTERVAL) {
            final int unique = Sorting.sortIndices(k, n);
            final SearchableInterval keys = BinarySearchKeyInterval.of(k, unique);
            introselect(part, a, 0, right, keys, keys.left(), keys.right(), maxDepth);
        } else if (keyStrategy == KeyStrategy.COMPRESSED_INDEX_SET) {
            final SearchableInterval keys = CompressedIndexSet.of(compression, k, n);
            introselect(part, a, 0, right, keys, keys.left(), keys.right(), maxDepth);
        } else if (keyStrategy == KeyStrategy.INDEX_SET) {
            final SearchableInterval keys = IndexSet.of(k, n);
            introselect(part, a, 0, right, keys, keys.left(), keys.right(), maxDepth);
        } else if (keyStrategy == KeyStrategy.KEY_UPDATING_INTERVAL) {
            final int unique = Sorting.sortIndices(k, n);
            final UpdatingInterval keys = KeyUpdatingInterval.of(k, unique);
            introselect(part, a, 0, right, keys, maxDepth);
        } else if (keyStrategy == KeyStrategy.INDEX_SET_UPDATING_INTERVAL) {
            final UpdatingInterval keys = BitIndexUpdatingInterval.of(k, n);
            introselect(part, a, 0, right, keys, maxDepth);
        } else if (keyStrategy == KeyStrategy.KEY_SPLITTING_INTERVAL) {
            final int unique = Sorting.sortIndices(k, n);
            final SplittingInterval keys = KeyUpdatingInterval.of(k, unique);
            introselect(part, a, 0, right, keys, maxDepth);
        } else if (keyStrategy == KeyStrategy.INDEX_SET_SPLITTING_INTERVAL) {
            final SplittingInterval keys = BitIndexUpdatingInterval.of(k, n);
            introselect(part, a, 0, right, keys, maxDepth);
        } else if (keyStrategy == KeyStrategy.INDEX_ITERATOR) {
            final int unique = Sorting.sortIndices(k, n);
            final IndexIterator keys = KeyIndexIterator.of(k, unique);
            introselect(part, a, 0, right, keys, keys.left(), keys.right(), maxDepth);
        } else if (keyStrategy == KeyStrategy.COMPRESSED_INDEX_ITERATOR) {
            final IndexIterator keys = CompressedIndexSet.iterator(compression, k, n);
            introselect(part, a, 0, right, keys, keys.left(), keys.right(), maxDepth);
        } else {
            throw new IllegalStateException(UNSUPPORTED_INTROSELECT + keyStrategy);
        }
    }

    /**
     * Partition the array such that index {@code k} corresponds to its
     * correctly sorted value in the equivalent fully sorted array.
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>Uses an introselect variant. The quickselect is provided as an argument;
     * the fall-back on poor convergence of the quickselect is controlled by
     * current configuration.
     *
     * <p>Returns information {@code p} on whether {@code k+1} is sorted.
     * If {@code p <= k} then {@code k+1} is sorted.
     * If {@code p > k} then {@code p+1} is a pivot.
     *
     * @param part Partition function.
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param k Index.
     * @param maxDepth Maximum depth for recursion.
     * @return the index {@code p}
     */
    private int introselect(SPEPartition part, double[] a, int left, int right,
        int k, int maxDepth) {
        int l = left;
        int r = right;
        final int[] upper = {0};
        while (true) {
            // It is possible to use edgeselect when k is close to the end
            // |l|-----|k|---------|k|--------|r|
            //  ---d1----
            //                      -----d2----
            final int d1 = k - l;
            final int d2 = r - k;
            if (Math.min(d1, d2) < edgeSelectConstant) {
                edgeSelection.partition(a, l, r, k, k);
                // Last known unsorted value >= k
                return r;
            }

            if (maxDepth == 0) {
                // Too much recursion
                // Note: For testing the Floyd-Rivest algorithm we trigger the recursion
                // consumer as a signal that FR failed due to a non-representative sample.
                recursionConsumer.accept(maxDepth);
                stopperSelection.partition(a, l, r, k, k);
                // Last known unsorted value >= k
                return r;
            }

            // Pick a pivot and partition
            int pivot;
            // length - 1
            int n = r - l;
            if (n > subSamplingSize) {
                // Floyd-Rivest: use SELECT recursively on a sample of size S to get an estimate
                // for the (k-l+1)-th smallest element into a[k], biased slightly so that the
                // (k-l+1)-th element is expected to lie in the smaller set after partitioning.
                ++n;
                final int ith = k - l + 1;
                final double z = Math.log(n);
                final double s = 0.5 * Math.exp(0.6666666666666666 * z);
                final double sd = 0.5 * Math.sqrt(z * s * (n - s) / n) * Integer.signum(ith - (n >> 1));
                final int ll = Math.max(l, (int) (k - ith * s / n + sd));
                final int rr = Math.min(r, (int) (k + (n - ith) * s / n + sd));
                // Optional random sampling
                if ((controlFlags & FLAG_RANDOM_SAMPLING) != 0) {
                    final IntUnaryOperator rng = createRNG(n, k);
                    // Shuffle [ll, k) from [l, k)
                    if (ll > l) {
                        for (int i = k; i > ll;) {
                            // l + rand [0, i - l + 1) : i is currently i+1
                            final int j = l + rng.applyAsInt(i - l);
                            final double t = a[--i];
                            a[i] = a[j];
                            a[j] = t;
                        }
                    }
                    // Shuffle (k, rr] from (k, r]
                    if (rr < r) {
                        for (int i = k; i < rr;) {
                            // r - rand [0, r - i + 1) : i is currently i-1
                            final int j = r - rng.applyAsInt(r - i);
                            final double t = a[++i];
                            a[i] = a[j];
                            a[j] = t;
                        }
                    }
                }
                introselect(part, a, ll, rr, k, lnNtoMaxDepthSinglePivot(z));
                pivot = k;
            } else {
                // default pivot strategy
                pivot = pivotingStrategy.pivotIndex(a, l, r, k);
            }

            final int p0 = part.partition(a, l, r, pivot, upper);
            final int p1 = upper[0];

            maxDepth--;
            if (k < p0) {
                // The element is in the left partition
                r = p0 - 1;
            } else if (k > p1) {
                // The element is in the right partition
                l = p1 + 1;
            } else {
                // The range contains the element we wanted.
                // Signal if k+1 is sorted.
                // This can be true if the pivot was a range [p0, p1]
                return k < p1 ? k : r;
            }
        }
    }

    /**
     * Partition the array such that index {@code k} corresponds to its
     * correctly sorted value in the equivalent fully sorted array.
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>Uses an introselect variant. The quickselect is provided as an argument;
     * the fall-back on poor convergence of the quickselect is controlled by
     * current configuration.
     *
     * <p>Returns information {@code p} on whether {@code k+1} is sorted.
     * If {@code p <= k} then {@code k+1} is sorted.
     * If {@code p > k} then {@code p+1} is a pivot.
     *
     * <p>Recursion is monitored by checking the partition is reduced by 2<sup>-x</sup> after
     * {@code c} iterations where {@code x} is the
     * {@link #setRecursionConstant(int) recursion constant} and {@code c} is the
     * {@link #setRecursionMultiple(double) recursion multiple} (variables reused for convenience).
     * Confidence bounds for dividing a length by 2<sup>-x</sup> are provided in Valois (2000)
     * as {@code c = floor((6/5)x) + b}:
     * <pre>
     * b  confidence (%)
     * 2  76.56
     * 3  92.92
     * 4  97.83
     * 5  99.33
     * 6  99.79
     * </pre>
     * <p>Ideally {@code c >= 3} using {@code x = 1}. E.g. We can use 3 iterations to be 76%
     * confident the sequence will divide in half; or 7 iterations to be 99% confident the
     * sequence will divide into a quarter. A larger factor {@code b} reduces the sensitivity
     * of introspection.
     *
     * @param part Partition function.
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param k Index.
     * @return the index {@code p}
     */
    private int introselect2(SPEPartition part, double[] a, int left, int right, int k) {
        int l = left;
        int r = right;
        final int[] upper = {0};
        int counter = (int) recursionMultiple;
        int threshold = (right - left) >>> recursionConstant;
        int depth = singlePivotMaxDepth(right - left);
        while (true) {
            // It is possible to use edgeselect when k is close to the end
            // |l|-----|k|---------|k|--------|r|
            //  ---d1----
            //                      -----d2----
            final int d1 = k - l;
            final int d2 = r - k;
            if (Math.min(d1, d2) < edgeSelectConstant) {
                edgeSelection.partition(a, l, r, k, k);
                // Last known unsorted value >= k
                return r;
            }

            // length - 1
            int n = r - l;
            depth--;
            if (--counter < 0) {
                if (n > threshold) {
                    // Did not reduce the length after set number of iterations.
                    // Here riselect (Valois (2000)) would use random points to choose the pivot
                    // to inject entropy and restart. This continues until the sum of the partition
                    // lengths is too high (twice the original length). Here we just switch.

                    // Note: For testing we trigger the recursion consumer
                    recursionConsumer.accept(depth);
                    stopperSelection.partition(a, l, r, k, k);
                    // Last known unsorted value >= k
                    return r;
                }
                // Once the confidence has been achieved we use (6/5)x with x=1.
                // So check every 5/6 iterations that the length is halving.
                if (counter == -5) {
                    counter = 1;
                }
                threshold >>>= 1;
            }

            // Pick a pivot and partition
            int pivot;
            if (n > subSamplingSize) {
                // Floyd-Rivest: use SELECT recursively on a sample of size S to get an estimate
                // for the (k-l+1)-th smallest element into a[k], biased slightly so that the
                // (k-l+1)-th element is expected to lie in the smaller set after partitioning.
                ++n;
                final int ith = k - l + 1;
                final double z = Math.log(n);
                final double s = 0.5 * Math.exp(0.6666666666666666 * z);
                final double sd = 0.5 * Math.sqrt(z * s * (n - s) / n) * Integer.signum(ith - (n >> 1));
                final int ll = Math.max(l, (int) (k - ith * s / n + sd));
                final int rr = Math.min(r, (int) (k + (n - ith) * s / n + sd));
                // Optional random sampling
                if ((controlFlags & FLAG_RANDOM_SAMPLING) != 0) {
                    final IntUnaryOperator rng = createRNG(n, k);
                    // Shuffle [ll, k) from [l, k)
                    if (ll > l) {
                        for (int i = k; i > ll;) {
                            // l + rand [0, i - l + 1) : i is currently i+1
                            final int j = l + rng.applyAsInt(i - l);
                            final double t = a[--i];
                            a[i] = a[j];
                            a[j] = t;
                        }
                    }
                    // Shuffle (k, rr] from (k, r]
                    if (rr < r) {
                        for (int i = k; i < rr;) {
                            // r - rand [0, r - i + 1) : i is currently i-1
                            final int j = r - rng.applyAsInt(r - i);
                            final double t = a[++i];
                            a[i] = a[j];
                            a[j] = t;
                        }
                    }
                }
                // Sample recursion restarts from [ll, rr]
                introselect2(part, a, ll, rr, k);
                pivot = k;
            } else {
                // default pivot strategy
                pivot = pivotingStrategy.pivotIndex(a, l, r, k);
            }

            final int p0 = part.partition(a, l, r, pivot, upper);
            final int p1 = upper[0];

            if (k < p0) {
                // The element is in the left partition
                r = p0 - 1;
            } else if (k > p1) {
                // The element is in the right partition
                l = p1 + 1;
            } else {
                // The range contains the element we wanted.
                // Signal if k+1 is sorted.
                // This can be true if the pivot was a range [p0, p1]
                return k < p1 ? k : r;
            }
        }
    }

    /**
     * Partition the array such that index {@code k} corresponds to its
     * correctly sorted value in the equivalent fully sorted array.
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>Uses an introselect variant. The quickselect is provided as an argument;
     * the fall-back on poor convergence of the quickselect is controlled by
     * current configuration.
     *
     * <p>Returns information {@code p} on whether {@code k+1} is sorted.
     * If {@code p <= k} then {@code k+1} is sorted.
     * If {@code p > k} then {@code p+1} is a pivot.
     *
     * <p>Recursion is monitored by checking the sum of partition lengths is less than
     * {@code m * (r - l)} where {@code m} is the
     * {@link #setRecursionMultiple(double) recursion multiple}.
     * Ideally {@code c} should be a value above 1.
     *
     * @param part Partition function.
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param k Index.
     * @return the index {@code p}
     */
    private int introselect(SPEPartition part, double[] a, int left, int right, int k) {
        int l = left;
        int r = right;
        final int[] upper = {0};
        // Set the limit on the sum of the length. Since the length is subtracted at the start
        // of the loop use (1 + recursionMultiple).
        long limit = (long) ((1 + recursionMultiple) * (right - left));
        int depth = singlePivotMaxDepth(right - left);
        while (true) {
            // It is possible to use edgeselect when k is close to the end
            // |l|-----|k|---------|k|--------|r|
            //  ---d1----
            //                      -----d2----
            final int d1 = k - l;
            final int d2 = r - k;
            if (Math.min(d1, d2) < edgeSelectConstant) {
                edgeSelection.partition(a, l, r, k, k);
                // Last known unsorted value >= k
                return r;
            }

            // length - 1
            int n = r - l;
            limit -= n;
            depth--;

            if (limit < 0) {
                // Excess total partition length
                // Note: For testing we trigger the recursion consumer
                recursionConsumer.accept(depth);
                stopperSelection.partition(a, l, r, k, k);
                // Last known unsorted value >= k
                return r;
            }

            // Pick a pivot and partition
            int pivot;
            if (n > subSamplingSize) {
                // Floyd-Rivest: use SELECT recursively on a sample of size S to get an estimate
                // for the (k-l+1)-th smallest element into a[k], biased slightly so that the
                // (k-l+1)-th element is expected to lie in the smaller set after partitioning.
                ++n;
                final int ith = k - l + 1;
                final double z = Math.log(n);
                final double s = 0.5 * Math.exp(0.6666666666666666 * z);
                final double sd = 0.5 * Math.sqrt(z * s * (n - s) / n) * Integer.signum(ith - (n >> 1));
                final int ll = Math.max(l, (int) (k - ith * s / n + sd));
                final int rr = Math.min(r, (int) (k + (n - ith) * s / n + sd));
                // Optional random sampling
                if ((controlFlags & FLAG_RANDOM_SAMPLING) != 0) {
                    final IntUnaryOperator rng = createRNG(n, k);
                    // Shuffle [ll, k) from [l, k)
                    if (ll > l) {
                        for (int i = k; i > ll;) {
                            // l + rand [0, i - l + 1) : i is currently i+1
                            final int j = l + rng.applyAsInt(i - l);
                            final double t = a[--i];
                            a[i] = a[j];
                            a[j] = t;
                        }
                    }
                    // Shuffle (k, rr] from (k, r]
                    if (rr < r) {
                        for (int i = k; i < rr;) {
                            // r - rand [0, r - i + 1) : i is currently i-1
                            final int j = r - rng.applyAsInt(r - i);
                            final double t = a[++i];
                            a[i] = a[j];
                            a[j] = t;
                        }
                    }
                }
                // Sample recursion restarts from [ll, rr]
                introselect(part, a, ll, rr, k);
                pivot = k;
            } else {
                // default pivot strategy
                pivot = pivotingStrategy.pivotIndex(a, l, r, k);
            }

            final int p0 = part.partition(a, l, r, pivot, upper);
            final int p1 = upper[0];

            if (k < p0) {
                // The element is in the left partition
                r = p0 - 1;
            } else if (k > p1) {
                // The element is in the right partition
                l = p1 + 1;
            } else {
                // The range contains the element we wanted.
                // Signal if k+1 is sorted.
                // This can be true if the pivot was a range [p0, p1]
                return k < p1 ? k : r;
            }
        }
    }

    /**
     * Partition the array such that indices {@code ka} and {@code kb} correspond to their
     * correctly sorted value in the equivalent fully sorted array.
     *
     * <p>For all indices {@code k} and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>Note: Requires {@code ka <= kb}. The use of two indices is to support processing
     * of pairs of indices {@code (k, k+1)}. However the indices are treated independently
     * and partitioned by recursion. They may be equal, neighbours or well separated.
     *
     * <p>Uses an introselect variant. The quickselect is provided as an argument; the
     * fall-back on poor convergence of the quickselect is a heapselect.
     *
     * @param part Partition function.
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param ka Index.
     * @param kb Index.
     * @param maxDepth Maximum depth for recursion.
     */
    private void introselect(SPEPartition part, double[] a, int left, int right,
        int ka, int kb, int maxDepth) {
        // Only one side requires recursion. The other side
        // can remain within this function call.
        int l = left;
        int r = right;
        int ka1 = ka;
        int kb1 = kb;
        final int[] upper = {0};
        while (true) {
            // length - 1
            final int n = r - l;

            if (n < minQuickSelectSize) {
                // Sort selection on small data
                sortSelectRange(a, l, r, ka1, kb1);
                return;
            }

            // It is possible to use heapselect when ka1 and kb1 are close to the ends
            // |l|-----|ka1|--------|kb1|------|r|
            //  ---d1----
            //                       -----d3----
            //  ---------d2-----------
            //          ----------d4-----------
            final int d1 = ka1 - l;
            final int d2 = kb1 - l;
            final int d3 = r - kb1;
            final int d4 = r - ka1;
            if (maxDepth == 0 ||
                Math.min(d1 + d3, Math.min(d2, d4)) < edgeSelectConstant) {
                // Too much recursion, or ka1 and kb1 are both close to the ends
                // Note: Does not use the edgeSelection function as the indices are not a range
                heapSelectPair(a, l, r, ka1, kb1);
                return;
            }

            // Pick a pivot and partition
            final int p0 = part.partition(a, l, r,
                pivotingStrategy.pivotIndex(a, l, r, ka),
                upper);
            final int p1 = upper[0];

            // Recursion to max depth
            // Note: Here we possibly branch left and right with multiple keys.
            // It is possible that the partition has split the pair
            // and the recursion proceeds with a single point.
            maxDepth--;
            // Recurse left side if required
            if (ka1 < p0) {
                if (kb1 <= p1) {
                    // Entirely on left side
                    r = p0 - 1;
                    kb1 = r < kb1 ? ka1 : kb1;
                    continue;
                }
                introselect(part, a, l, p0 - 1, ka1, ka1, maxDepth);
                ka1 = kb1;
            }
            if (kb1 <= p1) {
                // No right side
                return;
            }
            // Continue on the right side
            l = p1 + 1;
            ka1 = ka1 < l ? kb1 : ka1;
        }
    }

    /**
     * Partition the array such that index {@code k} corresponds to its
     * correctly sorted value in the equivalent fully sorted array.
     *
     * <p>For all indices {@code [ka, kb]} and any index {@code i}:
     *
     * <pre>{@code
     * data[i < ka] <= data[ka] <= data[kb] <= data[kb < i]
     * }</pre>
     *
     * <p>This function accepts indices {@code [ka, kb]} that define the
     * range of indices to partition. It is expected that the range is small.
     *
     * <p>Uses an introselect variant. The quickselect is provided as an argument;
     * the fall-back on poor convergence of the quickselect is controlled by
     * current configuration.
     *
     * <p>Recursion is monitored by checking the partition is reduced by 2<sup>-x</sup> after
     * {@code c} iterations where {@code x} is the
     * {@link #setRecursionConstant(int) recursion constant} and {@code c} is the
     * {@link #setRecursionMultiple(double) recursion multiple} (variables reused for convenience).
     * Confidence bounds for dividing a length by 2<sup>-x</sup> are provided in Valois (2000)
     * as {@code c = floor((6/5)x) + b}:
     * <pre>
     * b  confidence (%)
     * 2  76.56
     * 3  92.92
     * 4  97.83
     * 5  99.33
     * 6  99.79
     * </pre>
     * <p>Ideally {@code c >= 3} using {@code x = 1}. E.g. We can use 3 iterations to be 76%
     * confident the sequence will divide in half; or 7 iterations to be 99% confident the
     * sequence will divide into a quarter. A larger factor {@code b} reduces the sensitivity
     * of introspection.
     *
     * @param part Partition function.
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param ka First key of interest.
     * @param kb Last key of interest.
     */
    private void introselect2(SPEPartition part, double[] a, int left, int right, int ka, int kb) {
        int l = left;
        int r = right;
        final int[] upper = {0};
        int counter = (int) recursionMultiple;
        int threshold = (right - left) >>> recursionConstant;
        while (true) {
            // It is possible to use edgeselect when k is close to the end
            // |l|-----|ka|kkkkkkkk|kb|------|r|
            if (Math.min(kb - l, r - ka) < edgeSelectConstant) {
                edgeSelection.partition(a, l, r, ka, kb);
                return;
            }

            // length - 1
            int n = r - l;
            if (--counter < 0) {
                if (n > threshold) {
                    // Did not reduce the length after set number of iterations.
                    // Here riselect (Valois (2000)) would use random points to choose the pivot
                    // to inject entropy and restart. This continues until the sum of the partition
                    // lengths is too high (twice the original length). Here we just switch.

                    // Note: For testing we trigger the recursion consumer with the remaining length
                    recursionConsumer.accept(r - l);
                    stopperSelection.partition(a, l, r, ka, kb);
                    return;
                }
                // Once the confidence has been achieved we use (6/5)x with x=1.
                // So check every 5/6 iterations that the length is halving.
                if (counter == -5) {
                    counter = 1;
                }
                threshold >>>= 1;
            }

            // Pick a pivot and partition
            int pivot;
            if (n > subSamplingSize) {
                // Floyd-Rivest: use SELECT recursively on a sample of size S to get an estimate
                // for the (k-l+1)-th smallest element into a[k], biased slightly so that the
                // (k-l+1)-th element is expected to lie in the smaller set after partitioning.
                ++n;
                final int ith = ka - l + 1;
                final double z = Math.log(n);
                final double s = 0.5 * Math.exp(0.6666666666666666 * z);
                final double sd = 0.5 * Math.sqrt(z * s * (n - s) / n) * Integer.signum(ith - (n >> 1));
                final int ll = Math.max(l, (int) (ka - ith * s / n + sd));
                final int rr = Math.min(r, (int) (ka + (n - ith) * s / n + sd));
                // Optional random sampling
                if ((controlFlags & FLAG_RANDOM_SAMPLING) != 0) {
                    final IntUnaryOperator rng = createRNG(n, ka);
                    // Shuffle [ll, k) from [l, k)
                    if (ll > l) {
                        for (int i = ka; i > ll;) {
                            // l + rand [0, i - l + 1) : i is currently i+1
                            final int j = l + rng.applyAsInt(i - l);
                            final double t = a[--i];
                            a[i] = a[j];
                            a[j] = t;
                        }
                    }
                    // Shuffle (k, rr] from (k, r]
                    if (rr < r) {
                        for (int i = ka; i < rr;) {
                            // r - rand [0, r - i + 1) : i is currently i-1
                            final int j = r - rng.applyAsInt(r - i);
                            final double t = a[++i];
                            a[i] = a[j];
                            a[j] = t;
                        }
                    }
                }
                // Sample recursion restarts from [ll, rr]
                introselect2(part, a, ll, rr, ka, ka);
                pivot = ka;
            } else {
                // default pivot strategy
                pivot = pivotingStrategy.pivotIndex(a, l, r, ka);
            }

            final int p0 = part.partition(a, l, r, pivot, upper);
            final int p1 = upper[0];

            // Note: Here we expect [ka, kb] to be small and splitting is unlikely.
            //                   p0 p1
            // |l|--|ka|kkkk|kb|--|P|-------------------|r|
            // |l|----------------|P|--|ka|kkk|kb|------|r|
            // |l|-----------|ka|k|P|k|kb|--------------|r|
            if (kb < p0) {
                // The element is in the left partition
                r = p0 - 1;
            } else if (ka > p1) {
                // The element is in the right partition
                l = p1 + 1;
            } else {
                // Pivot splits [ka, kb]. Expect ends to be close to the pivot and finish.
                if (ka < p0) {
                    sortSelectRight(a, l, p0 - 1, ka);
                }
                if (kb > p1) {
                    sortSelectLeft(a, p1 + 1, r, kb);
                }
                return;
            }
        }
    }

    /**
     * Partition the array such that indices {@code k} correspond to their
     * correctly sorted value in the equivalent fully sorted array.
     *
     * <p>For all indices {@code k} and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>This function accepts an ordered array of indices {@code k} and pointers
     * to the first and last positions in {@code k} that define the range indices
     * to partition.
     *
     * <pre>{@code
     * left <= k[ia] <= k[ib] <= right  : ia <= ib
     * }</pre>
     *
     * <p>A binary search is used to search for keys in {@code [ia, ib]}
     * to create {@code [ia, ib1]} and {@code [ia1, ib]} if partitioning splits the range.
     *
     * <p>Uses an introselect variant. The quickselect is provided as an argument;
     * the fall-back on poor convergence of the quickselect is a heapselect.
     *
     * @param part Partition function.
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param k Indices to partition (ordered).
     * @param ia Index of first key.
     * @param ib Index of last key.
     * @param maxDepth Maximum depth for recursion.
     */
    private void introselect(SPEPartition part, double[] a, int left, int right,
        int[] k, int ia, int ib, int maxDepth) {
        // Only one side requires recursion. The other side
        // can remain within this function call.
        int l = left;
        int r = right;
        int ia1 = ia;
        int ib1 = ib;
        final int[] upper = {0};
        while (true) {
            // Switch to paired key implementation if possible.
            // Note: adjacent indices can refer to well separated keys.
            // This is the major difference between this implementation
            // and an implementation using an IndexInterval (which does not
            // have a fast way to determine if there are any keys within the range).
            if (ib1 - ia1 <= 1) {
                introselect(part, a, l, r, k[ia1], k[ib1], maxDepth);
                return;
            }

            // length - 1
            final int n = r - l;
            int ka = k[ia1];
            final int kb = k[ib1];

            if (n < minQuickSelectSize) {
                // Sort selection on small data
                sortSelectRange(a, l, r, ka, kb);
                return;
            }

            // It is possible to use heapselect when ka and kb are close to the same end
            // |l|-----|ka|--------|kb|------|r|
            //  ---------s2----------
            //          ----------s4-----------
            if (Math.min(kb - l, r - ka) < edgeSelectConstant) {
                edgeSelection.partition(a, l, r, ka, kb);
                return;
            }

            if (maxDepth == 0) {
                // Too much recursion
                heapSelectRange(a, l, r, ka, kb);
                return;
            }

            // Pick a pivot and partition
            final int p0 = part.partition(a, l, r,
                pivotingStrategy.pivotIndex(a, l, r, ka),
                upper);
            final int p1 = upper[0];

            // Recursion to max depth
            // Note: Here we possibly branch left and right with multiple keys.
            // It is possible that the partition has split the keys
            // and the recursion proceeds with a reduced set on either side.
            //                   p0 p1
            // |l|--|ka|--k----k--|P|------k--|kb|------|r|
            //       ia1       iba  |      ia1  ib1
            // Search less/greater is bounded at ia1/ib1
            maxDepth--;
            // Recurse left side if required
            if (ka < p0) {
                if (kb <= p1) {
                    // Entirely on left side
                    r = p0 - 1;
                    if (r < kb) {
                        ib1 = searchLessOrEqual(k, ia1, ib1, r);
                    }
                    continue;
                }
                // Require a split here
                introselect(part, a, l, p0 - 1, k, ia1, searchLessOrEqual(k, ia1, ib1, p0 - 1), maxDepth);
                ia1 = searchGreaterOrEqual(k, ia1, ib1, l);
                ka = k[ia1];
            }
            if (kb <= p1) {
                // No right side
                recursionConsumer.accept(maxDepth);
                return;
            }
            // Continue on the right side
            l = p1 + 1;
            if (ka < l) {
                ia1 = searchGreaterOrEqual(k, ia1, ib1, l);
            }
        }
    }

    /**
     * Partition the array such that indices {@code k} correspond to their
     * correctly sorted value in the equivalent fully sorted array.
     *
     * <p>For all indices {@code k} and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>This function accepts a {@link SearchableInterval} of indices {@code k} and the
     * first index {@code ka} and last index {@code kb} that define the range of indices
     * to partition. The {@link SearchableInterval} is used to search for keys in {@code [ka, kb]}
     * to create {@code [ka, kb1]} and {@code [ka1, kb]} if partitioning splits the range.
     *
     * <pre>{@code
     * left <= ka <= kb <= right
     * }</pre>
     *
     * <p>Uses an introselect variant. The quickselect is provided as an argument;
     * the fall-back on poor convergence of the quickselect is a heapselect.
     *
     * @param part Partition function.
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param k Interval of indices to partition (ordered).
     * @param ka First key.
     * @param kb Last key.
     * @param maxDepth Maximum depth for recursion.
     */
    // package-private for benchmarking
    void introselect(SPEPartition part, double[] a, int left, int right,
        SearchableInterval k, int ka, int kb, int maxDepth) {
        // Only one side requires recursion. The other side
        // can remain within this function call.
        int l = left;
        int r = right;
        int ka1 = ka;
        int kb1 = kb;
        final int[] upper = {0};
        while (true) {
            // length - 1
            int n = r - l;

            if (n < minQuickSelectSize) {
                // Sort selection on small data
                sortSelectRange(a, l, r, ka1, kb1);
                recursionConsumer.accept(maxDepth);
                return;
            }

            // It is possible to use heapselect when kaa and kb1 are close to the same end
            // |l|-----|ka1|--------|kb1|------|r|
            //  ---------s2----------
            //          ----------s4-----------
            if (Math.min(kb1 - l, r - ka1) < edgeSelectConstant) {
                edgeSelection.partition(a, l, r, ka1, kb1);
                recursionConsumer.accept(maxDepth);
                return;
            }

            if (maxDepth == 0) {
                // Too much recursion
                heapSelectRange(a, l, r, ka1, kb1);
                recursionConsumer.accept(maxDepth);
                return;
            }

            // Pick a pivot and partition
            int pivot;
            if (n > subSamplingSize) {
                // Floyd-Rivest: use SELECT recursively on a sample of size S to get an estimate
                // for the (k-l+1)-th smallest element into a[k], biased slightly so that the
                // (k-l+1)-th element is expected to lie in the smaller set after partitioning.
                // Note: This targets ka1 and ignores kb1 for pivot selection.
                ++n;
                final int ith = ka1 - l + 1;
                final double z = Math.log(n);
                final double s = 0.5 * Math.exp(0.6666666666666666 * z);
                final double sd = 0.5 * Math.sqrt(z * s * (n - s) / n) * Integer.signum(ith - (n >> 1));
                final int ll = Math.max(l, (int) (ka1 - ith * s / n + sd));
                final int rr = Math.min(r, (int) (ka1 + (n - ith) * s / n + sd));
                // Optional random sampling
                if ((controlFlags & FLAG_RANDOM_SAMPLING) != 0) {
                    final IntUnaryOperator rng = createRNG(n, ka1);
                    // Shuffle [ll, k) from [l, k)
                    if (ll > l) {
                        for (int i = ka1; i > ll;) {
                            // l + rand [0, i - l + 1) : i is currently i+1
                            final int j = l + rng.applyAsInt(i - l);
                            final double t = a[--i];
                            a[i] = a[j];
                            a[j] = t;
                        }
                    }
                    // Shuffle (k, rr] from (k, r]
                    if (rr < r) {
                        for (int i = ka1; i < rr;) {
                            // r - rand [0, r - i + 1) : i is currently i-1
                            final int j = r - rng.applyAsInt(r - i);
                            final double t = a[++i];
                            a[i] = a[j];
                            a[j] = t;
                        }
                    }
                }
                introselect(part, a, ll, rr, k, ka1, ka1, lnNtoMaxDepthSinglePivot(z));
                pivot = ka1;
            } else {
                // default pivot strategy
                pivot = pivotingStrategy.pivotIndex(a, l, r, ka1);
            }

            final int p0 = part.partition(a, l, r, pivot, upper);
            final int p1 = upper[0];

            // Recursion to max depth
            // Note: Here we possibly branch left and right with multiple keys.
            // It is possible that the partition has split the keys
            // and the recursion proceeds with a reduced set on either side.
            //                    p0 p1
            // |l|--|ka1|--k----k--|P|------k--|kb1|------|r|
            //                 kb1  |      ka1
            // Search previous/next is bounded at ka1/kb1
            maxDepth--;
            // Recurse left side if required
            if (ka1 < p0) {
                if (kb1 <= p1) {
                    // Entirely on left side
                    r = p0 - 1;
                    if (r < kb1) {
                        kb1 = k.previousIndex(r);
                    }
                    continue;
                }
                introselect(part, a, l, p0 - 1, k, ka1, k.split(p0, p1, upper), maxDepth);
                ka1 = upper[0];
            }
            if (kb1 <= p1) {
                // No right side
                recursionConsumer.accept(maxDepth);
                return;
            }
            // Continue on the right side
            l = p1 + 1;
            if (ka1 < l) {
                ka1 = k.nextIndex(l);
            }
        }
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array.
     *
     * <p>For all indices {@code k} and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>This function accepts a {@link UpdatingInterval} of indices {@code k} that define the
     * range of indices to partition. The {@link UpdatingInterval} can be narrowed or split as
     * partitioning divides the range.
     *
     * <p>Uses an introselect variant. The quickselect is provided as an argument;
     * the fall-back on poor convergence of the quickselect is controlled by
     * current configuration.
     *
     * @param part Partition function.
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param k Interval of indices to partition (ordered).
     * @param maxDepth Maximum depth for recursion.
     */
    // package-private for benchmarking
    void introselect(SPEPartition part, double[] a, int left, int right,
        UpdatingInterval k, int maxDepth) {
        // Only one side requires recursion. The other side
        // can remain within this function call.
        int l = left;
        int r = right;
        int ka = k.left();
        int kb = k.right();
        final int[] upper = {0};
        while (true) {
            // length - 1
            final int n = r - l;

            if (n < minQuickSelectSize) {
                // Sort selection on small data
                sortSelectRange(a, l, r, ka, kb);
                recursionConsumer.accept(maxDepth);
                return;
            }

            // It is possible to use heapselect when ka and kb are close to the same end
            // |l|-----|ka|--------|kb|------|r|
            //  ---------s2----------
            //          ----------s4-----------
            if (Math.min(kb - l, r - ka) < edgeSelectConstant) {
                edgeSelection.partition(a, l, r, ka, kb);
                recursionConsumer.accept(maxDepth);
                return;
            }

            if (maxDepth == 0) {
                // Too much recursion
                heapSelectRange(a, l, r, ka, kb);
                recursionConsumer.accept(maxDepth);
                return;
            }

            // Pick a pivot and partition
            final int p0 = part.partition(a, l, r,
                pivotingStrategy.pivotIndex(a, l, r, ka),
                upper);
            final int p1 = upper[0];

            // Recursion to max depth
            // Note: Here we possibly branch left and right with multiple keys.
            // It is possible that the partition has split the keys
            // and the recursion proceeds with a reduced set on either side.
            //                   p0 p1
            // |l|--|ka|--k----k--|P|------k--|kb|------|r|
            //                 kb  |       ka
            maxDepth--;
            // Recurse left side if required
            if (ka < p0) {
                if (kb <= p1) {
                    // Entirely on left side
                    r = p0 - 1;
                    if (r < kb) {
                        kb = k.updateRight(r);
                    }
                    continue;
                }
                introselect(part, a, l, p0 - 1, k.splitLeft(p0, p1), maxDepth);
                ka = k.left();
            } else if (kb <= p1) {
                // No right side
                recursionConsumer.accept(maxDepth);
                return;
            } else if (ka <= p1) {
                ka = k.updateLeft(p1 + 1);
            }
            // Continue on the right side
            l = p1 + 1;
        }
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array.
     *
     * <p>For all indices {@code k} and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>This function accepts a {@link SplittingInterval} of indices {@code k} that define the
     * range of indices to partition. The {@link SplittingInterval} is split as
     * partitioning divides the range.
     *
     * <p>Uses an introselect variant. The quickselect is provided as an argument;
     * the fall-back on poor convergence of the quickselect is a heapselect.
     *
     * @param part Partition function.
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param keys Interval of indices to partition (ordered).
     * @param maxDepth Maximum depth for recursion.
     */
    // package-private for benchmarking
    void introselect(SPEPartition part, double[] a, int left, int right,
        SplittingInterval keys, int maxDepth) {
        // Only one side requires recursion. The other side
        // can remain within this function call.
        int l = left;
        int r = right;
        SplittingInterval k = keys;
        int ka = k.left();
        int kb = k.right();
        final int[] upper = {0};
        while (true) {
            // length - 1
            final int n = r - l;

            if (n < minQuickSelectSize) {
                // Sort selection on small data
                sortSelectRange(a, l, r, ka, kb);
                recursionConsumer.accept(maxDepth);
                return;
            }

            // It is possible to use heapselect when ka and kb are close to the same end
            // |l|-----|ka|--------|kb|------|r|
            //  ---------s2----------
            //          ----------s4-----------
            if (Math.min(kb - l, r - ka) < edgeSelectConstant) {
                edgeSelection.partition(a, l, r, ka, kb);
                recursionConsumer.accept(maxDepth);
                return;
            }

            if (maxDepth == 0) {
                // Too much recursion
                heapSelectRange(a, l, r, ka, kb);
                recursionConsumer.accept(maxDepth);
                return;
            }

            // Pick a pivot and partition
            final int p0 = part.partition(a, l, r,
                pivotingStrategy.pivotIndex(a, l, r, ka),
                upper);
            final int p1 = upper[0];

            // Recursion to max depth
            // Note: Here we possibly branch left and right with multiple keys.
            // It is possible that the partition has split the keys
            // and the recursion proceeds with a reduced set on either side.
            //                   p0 p1
            // |l|--|ka|--k----k--|P|------k--|kb|------|r|
            //                 kb  |       ka
            maxDepth--;
            final SplittingInterval lk = k.split(p0, p1);
            // Recurse left side if required
            if (lk != null) {
                // Avoid recursive method calls
                if (k.empty()) {
                    // Entirely on left side
                    r = p0 - 1;
                    kb = lk.right();
                    k = lk;
                    continue;
                }
                introselect(part, a, l, p0 - 1, lk, maxDepth);
            }
            if (k.empty()) {
                // No right side
                recursionConsumer.accept(maxDepth);
                return;
            }
            // Continue on the right side
            l = p1 + 1;
            ka = k.left();
        }
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array.
     *
     * <p>For all indices {@code k} and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>This function accepts an {@link IndexIterator} of indices {@code k}; for
     * convenience the lower and upper indices of the current interval are passed as the
     * first index {@code ka} and last index {@code kb} of the closed interval of indices
     * to partition. These may be within the lower and upper indices if the interval was
     * split during recursion: {@code lower <= ka <= kb <= upper}.
     *
     * <p>The data is recursively partitioned using left-most ordering. When the current
     * interval has been partitioned the {@link IndexIterator} is used to advance to the
     * next interval to partition.
     *
     * <p>Uses an introselect variant. The quickselect is provided as an argument; the
     * fall-back on poor convergence of the quickselect is a heapselect.
     *
     * @param part Partition function.
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param k Interval of indices to partition (ordered).
     * @param ka First key.
     * @param kb Last key.
     * @param maxDepth Maximum depth for recursion.
     */
    // package-private for benchmarking
    void introselect(SPEPartition part, double[] a, int left, int right,
        IndexIterator k, int ka, int kb, int maxDepth) {
        // Left side requires recursion; right side remains within this function
        // When this function returns all indices in [left, right] must be processed.
        int l = left;
        int lo = ka;
        int hi = kb;
        final int[] upper = {0};
        while (true) {
            if (maxDepth == 0) {
                // Too much recursion.
                // Advance the iterator to the end of the current range.
                // Note: heapSelectRange handles hi > right.
                // Single API method: advanceBeyond(right): return hi <= right
                while (hi < right && k.next()) {
                    hi = k.right();
                }
                heapSelectRange(a, l, right, lo, hi);
                recursionConsumer.accept(maxDepth);
                return;
            }

            // length - 1
            final int n = right - l;

            // If interval is close to one end then edgeselect.
            // Only elect left if there are no further indices in the range.
            // |l|-----|lo|--------|hi|------|right|
            //  ---------d1----------
            //          --------------d2-----------
            if (Math.min(hi - l, right - lo) < edgeSelectConstant) {
                if (hi - l > right - lo) {
                    // Right end. Do not check above hi, just select to the end
                    edgeSelection.partition(a, l, right, lo, right);
                    recursionConsumer.accept(maxDepth);
                    return;
                } else if (k.nextAfter(right)) {
                    // Left end
                    // Only if no further indices in the range.
                    // If false this branch will continue to be triggered until
                    // a partition is made to separate the next indices.
                    edgeSelection.partition(a, l, right, lo, hi);
                    recursionConsumer.accept(maxDepth);
                    // Advance iterator
                    l = hi + 1;
                    if (!k.positionAfter(hi) || Math.max(k.left(), l) > right) {
                        // No more keys, or keys beyond the current bounds
                        return;
                    }
                    lo = Math.max(k.left(), l);
                    hi = Math.min(right, k.right());
                    // Continue right (allows a second heap select for the right side)
                    continue;
                }
            }

            // If interval is close to both ends then full sort
            // |l|-----|lo|--------|hi|------|right|
            //  ---d1----
            //                       ----d2--------
            // (lo - l) + (right - hi) == (right - l) - (hi - lo)
            if (n - (hi - lo) < minQuickSelectSize) {
                // Handle small data. This is done as the JDK sort will
                // use insertion sort for small data. For double data it
                // will also pre-process the data for NaN and signed
                // zeros which is an overhead to avoid.
                if (n < minQuickSelectSize) {
                    // Must not use sortSelectRange in [lo, hi] as the iterator
                    // has not been advanced to check after hi
                    sortSelectRight(a, l, right, lo);
                } else {
                    // Note: This disregards the current level of recursion
                    // but can exploit the JDK's more advanced sort algorithm.
                    Arrays.sort(a, l, right + 1);
                }
                recursionConsumer.accept(maxDepth);
                return;
            }

            // Here: l <= lo <= hi <= right
            // Pick a pivot and partition
            final int p0 = part.partition(a, l, right,
                pivotingStrategy.pivotIndex(a, l, right, ka),
                upper);
            final int p1 = upper[0];

            maxDepth--;
            // Recursion left
            if (lo < p0) {
                introselect(part, a, l, p0 - 1, k, lo, Math.min(hi, p0 - 1), maxDepth);
                // Advance iterator
                // Single API method: fastForwardAndLeftWithin(p1, right)
                if (!k.positionAfter(p1) || k.left() > right) {
                    // No more keys, or keys beyond the current bounds
                    return;
                }
                lo = k.left();
                hi = Math.min(right, k.right());
            }
            if (hi <= p1) {
                // Advance iterator
                if (!k.positionAfter(p1) || k.left() > right) {
                    // No more keys, or keys beyond the current bounds
                    return;
                }
                lo = k.left();
                hi = Math.min(right, k.right());
            }
            // Continue right
            l = p1 + 1;
            lo = Math.max(lo, l);
        }
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array. For all indices {@code k}
     * and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>All indices are assumed to be within {@code [0, right]}.
     *
     * <p>Uses an introselect variant. The dual pivot quickselect is provided as an argument;
     * the fall-back on poor convergence of the quickselect is controlled by
     * current configuration.
     *
     * <p>The partition method is not required to handle signed zeros.
     *
     * @param part Partition function.
     * @param a Values.
     * @param k Indices (may be destructively modified).
     * @param count Count of indices (assumed to be strictly positive).
     */
    void introselect(DPPartition part, double[] a, int[] k, int count) {
        // Handle NaN / signed zeros
        final DoubleDataTransformer t = SORT_TRANSFORMER.get();
        // Assume this is in-place
        t.preProcess(a);
        final int end = t.length();
        int n = count;
        if (end > 1) {
            // Filter indices invalidated by NaN check
            if (end < a.length) {
                for (int i = n; --i >= 0;) {
                    final int v = k[i];
                    if (v >= end) {
                        // swap(k, i, --n)
                        k[i] = k[--n];
                        k[n] = v;
                    }
                }
            }
            introselect(part, a, end - 1, k, n);
        }
        // Restore signed zeros
        t.postProcess(a, k, n);
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array. For all indices {@code k}
     * and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>All indices are assumed to be within {@code [0, right]}.
     *
     * <p>Uses an introselect variant. The dual pivot quickselect is provided as an argument;
     * the fall-back on poor convergence of the quickselect is controlled by
     * current configuration.
     *
     * <p>This function assumes {@code n > 0} and {@code right > 0}; otherwise
     * there is nothing to do.
     *
     * @param part Partition function.
     * @param a Values.
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param k Indices (may be destructively modified).
     * @param n Count of indices (assumed to be strictly positive).
     */
    private void introselect(DPPartition part, double[] a, int right, int[] k, int n) {
        if (n < 1) {
            return;
        }
        final int maxDepth = createMaxDepthDualPivot(right + 1);
        // Handle cases without multiple keys
        if (n == 1) {
            if (pairedKeyStrategy == PairedKeyStrategy.PAIRED_KEYS) {
                // Dedicated method for a single key
                introselect(part, a, 0, right, k[0], maxDepth);
            } else if (pairedKeyStrategy == PairedKeyStrategy.TWO_KEYS) {
                // Dedicated method for two keys using the same key
                introselect(part, a, 0, right, k[0], k[0], maxDepth);
            } else if (pairedKeyStrategy == PairedKeyStrategy.SEARCHABLE_INTERVAL) {
                // Reuse the IndexInterval method using the same key
                introselect(part, a, 0, right, IndexIntervals.anyIndex(), k[0], k[0], maxDepth);
            } else if (pairedKeyStrategy == PairedKeyStrategy.UPDATING_INTERVAL) {
                // Reuse the Interval method using a single key
                introselect(part, a, 0, right, IndexIntervals.interval(k[0]), maxDepth);
            } else {
                throw new IllegalStateException(UNSUPPORTED_INTROSELECT + pairedKeyStrategy);
            }
            return;
        }
        // Special case for partition around adjacent indices (for interpolation)
        if (n == 2 && k[0] + 1 == k[1]) {
            if (pairedKeyStrategy == PairedKeyStrategy.PAIRED_KEYS) {
                // Dedicated method for a single key, returns information about k+1
                final int p = introselect(part, a, 0, right, k[0], maxDepth);
                // p <= k to signal k+1 is unsorted, or p+1 is a pivot.
                // if k is sorted, and p+1 is sorted, k+1 is sorted if k+1 == p.
                if (p > k[1]) {
                    selectMinIgnoreZeros(a, k[1], p);
                }
            } else if (pairedKeyStrategy == PairedKeyStrategy.TWO_KEYS) {
                // Dedicated method for two keys
                // Note: This can handle keys that are not adjacent
                // e.g. keys near opposite ends without a partition step.
                final int ka = Math.min(k[0], k[1]);
                final int kb = Math.max(k[0], k[1]);
                introselect(part, a, 0, right, ka, kb, maxDepth);
            } else if (pairedKeyStrategy == PairedKeyStrategy.SEARCHABLE_INTERVAL) {
                // Reuse the IndexInterval method using a range of two keys
                introselect(part, a, 0, right, IndexIntervals.anyIndex(), k[0], k[1], maxDepth);
            } else if (pairedKeyStrategy == PairedKeyStrategy.UPDATING_INTERVAL) {
                // Reuse the Interval method using a range of two keys
                introselect(part, a, 0, right, IndexIntervals.interval(k[0], k[1]), maxDepth);
            } else {
                throw new IllegalStateException(UNSUPPORTED_INTROSELECT + pairedKeyStrategy);
            }
            return;
        }

        // Detect possible saturated range.
        // minimum keys = 10
        // min separation = 2^3  (could use log2(minQuickSelectSize) here)
        // saturation = 0.95
        //if (keysAreSaturated(right + 1, k, n, 10, 3, 0.95)) {
        //    Arrays.sort(a, 0, right + 1);
        //    return;
        //}

        // Note: Sorting to unique keys is an overhead. This can be eliminated
        // by requesting the caller passes sorted keys (or quantiles in order).

        if (keyStrategy == KeyStrategy.ORDERED_KEYS) {
            // DP does not offer ORDERED_KEYS implementation but we include the branch
            // for completeness.
            throw new IllegalStateException(UNSUPPORTED_INTROSELECT + keyStrategy);
        } else if (keyStrategy == KeyStrategy.SCANNING_KEY_SEARCHABLE_INTERVAL) {
            final int unique = Sorting.sortIndices(k, n);
            final SearchableInterval keys = ScanningKeyInterval.of(k, unique);
            introselect(part, a, 0, right, keys, keys.left(), keys.right(), maxDepth);
        } else if (keyStrategy == KeyStrategy.SEARCH_KEY_SEARCHABLE_INTERVAL) {
            final int unique = Sorting.sortIndices(k, n);
            final SearchableInterval keys = BinarySearchKeyInterval.of(k, unique);
            introselect(part, a, 0, right, keys, keys.left(), keys.right(), maxDepth);
        } else if (keyStrategy == KeyStrategy.COMPRESSED_INDEX_SET) {
            final SearchableInterval keys = CompressedIndexSet.of(compression, k, n);
            introselect(part, a, 0, right, keys, keys.left(), keys.right(), maxDepth);
        } else if (keyStrategy == KeyStrategy.INDEX_SET) {
            final SearchableInterval keys = IndexSet.of(k, n);
            introselect(part, a, 0, right, keys, keys.left(), keys.right(), maxDepth);
        } else if (keyStrategy == KeyStrategy.KEY_UPDATING_INTERVAL) {
            final int unique = Sorting.sortIndices(k, n);
            final UpdatingInterval keys = KeyUpdatingInterval.of(k, unique);
            introselect(part, a, 0, right, keys, maxDepth);
        } else if (keyStrategy == KeyStrategy.INDEX_SET_UPDATING_INTERVAL) {
            final UpdatingInterval keys = BitIndexUpdatingInterval.of(k, n);
            introselect(part, a, 0, right, keys, maxDepth);
        } else if (keyStrategy == KeyStrategy.KEY_SPLITTING_INTERVAL) {
            final int unique = Sorting.sortIndices(k, n);
            final SplittingInterval keys = KeyUpdatingInterval.of(k, unique);
            introselect(part, a, 0, right, keys, maxDepth);
        } else if (keyStrategy == KeyStrategy.INDEX_SET_SPLITTING_INTERVAL) {
            final SplittingInterval keys = BitIndexUpdatingInterval.of(k, n);
            introselect(part, a, 0, right, keys, maxDepth);
        } else if (keyStrategy == KeyStrategy.INDEX_ITERATOR) {
            final int unique = Sorting.sortIndices(k, n);
            final IndexIterator keys = KeyIndexIterator.of(k, unique);
            introselect(part, a, 0, right, keys, keys.left(), keys.right(), maxDepth);
        } else if (keyStrategy == KeyStrategy.COMPRESSED_INDEX_ITERATOR) {
            final IndexIterator keys = CompressedIndexSet.iterator(compression, k, n);
            introselect(part, a, 0, right, keys, keys.left(), keys.right(), maxDepth);
        } else {
            throw new IllegalStateException(UNSUPPORTED_INTROSELECT + keyStrategy);
        }
    }

    /**
     * Partition the array such that index {@code k} corresponds to its
     * correctly sorted value in the equivalent fully sorted array.
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>Uses an introselect variant. The quickselect is provided as an argument;
     * the fall-back on poor convergence of the quickselect is controlled by
     * current configuration.
     *
     * <p>Returns information {@code p} on whether {@code k+1} is sorted.
     * If {@code p <= k} then {@code k+1} is sorted.
     * If {@code p > k} then {@code p+1} is a pivot.
     *
     * @param part Partition function.
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param k Index.
     * @param maxDepth Maximum depth for recursion.
     * @return the index {@code p}
     */
    private int introselect(DPPartition part, double[] a, int left, int right,
        int k, int maxDepth) {
        int l = left;
        int r = right;
        final int[] upper = {0, 0, 0};
        while (true) {
            // It is possible to use edgeselect when k is close to the end
            // |l|-----|k|---------|k|--------|r|
            //  ---d1----
            //                      -----d3----
            final int d1 = k - l;
            final int d3 = r - k;
            if (Math.min(d1, d3) < edgeSelectConstant) {
                edgeSelection.partition(a, l, r, k, k);
                // Last known unsorted value >= k
                return r;
            }

            if (maxDepth == 0) {
                // Too much recursion
                stopperSelection.partition(a, l, r, k, k);
                // Last known unsorted value >= k
                return r;
            }

            // Pick 2 pivots and partition
            int p0 = dualPivotingStrategy.pivotIndex(a, l, r, upper);
            p0 = part.partition(a, l, r, p0, upper[0], upper);
            final int p1 = upper[0];
            final int p2 = upper[1];
            final int p3 = upper[2];

            maxDepth--;
            if (k < p0) {
                // The element is in the left partition
                r = p0 - 1;
                continue;
            } else if (k > p3) {
                // The element is in the right partition
                l = p3 + 1;
                continue;
            }
            // Check the interval overlaps the middle; and the middle exists.
            //                    p0 p1                p2 p3
            // |l|-----------------|P|------------------|P|----|r|
            // Eliminate:     ----kb1                    ka1----
            if (k <= p1 || p2 <= k || p2 - p1 <= 2) {
                // Signal if k+1 is sorted.
                // This can be true if the pivots were ranges [p0, p1] or [p2, p3]
                // This check will match *most* sorted k for the 3 eliminated cases.
                // It will not identify p2 - p1 <= 2 when k == p1. In this case
                // k+1 is sorted and a min-select for k+1 is a fast scan up to r.
                return k != p1 && k < p3 ? k : r;
            }
            // Continue in the middle partition
            l = p1 + 1;
            r = p2 - 1;
        }
    }

    /**
     * Partition the array such that indices {@code ka} and {@code kb} correspond to their
     * correctly sorted value in the equivalent fully sorted array.
     *
     * <p>For all indices {@code k} and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>Note: Requires {@code ka <= kb}. The use of two indices is to support processing
     * of pairs of indices {@code (k, k+1)}. However the indices are treated independently
     * and partitioned by recursion. They may be equal, neighbours or well separated.
     *
     * <p>Uses an introselect variant. The quickselect is provided as an argument; the
     * fall-back on poor convergence of the quickselect is a heapselect.
     *
     * @param part Partition function.
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param ka Index.
     * @param kb Index.
     * @param maxDepth Maximum depth for recursion.
     */
    private void introselect(DPPartition part, double[] a, int left, int right,
        int ka, int kb, int maxDepth) {
        // Only one side requires recursion. The other side
        // can remain within this function call.
        int l = left;
        int r = right;
        int ka1 = ka;
        int kb1 = kb;
        final int[] upper = {0, 0, 0};
        while (true) {
            // length - 1
            final int n = r - l;

            if (n < minQuickSelectSize) {
                // Sort selection on small data
                sortSelectRange(a, l, r, ka1, kb1);
                return;
            }

            // It is possible to use heapselect when ka1 and kb1 are close to the ends
            // |l|-----|ka1|--------|kb1|------|r|
            //  ---s1----
            //                       -----s3----
            //  ---------s2-----------
            //          ----------s4-----------
            final int s1 = ka1 - l;
            final int s2 = kb1 - l;
            final int s3 = r - kb1;
            final int s4 = r - ka1;
            if (maxDepth == 0 ||
                Math.min(s1 + s3, Math.min(s2, s4)) < edgeSelectConstant) {
                // Too much recursion, or ka1 and kb1 are both close to the ends
                // Note: Does not use the edgeSelection function as the indices are not a range
                heapSelectPair(a, l, r, ka1, kb1);
                return;
            }

            // Pick 2 pivots and partition
            int p0 = dualPivotingStrategy.pivotIndex(a, l, r, upper);
            p0 = part.partition(a, l, r, p0, upper[0], upper);
            final int p1 = upper[0];
            final int p2 = upper[1];
            final int p3 = upper[2];

            // Recursion to max depth
            // Note: Here we possibly branch left and right with multiple keys.
            // It is possible that the partition has split the pair
            // and the recursion proceeds with a single point.
            maxDepth--;
            // Recurse left side if required
            if (ka1 < p0) {
                if (kb1 <= p1) {
                    // Entirely on left side
                    r = p0 - 1;
                    kb1 = r < kb1 ? ka1 : kb1;
                    continue;
                }
                introselect(part, a, l, p0 - 1, ka1, ka1, maxDepth);
                // Here we must process middle and possibly right
                ka1 = kb1;
            }
            // Recurse middle if required
            // Check the either k is in the range (p1, p2)
            //                    p0 p1                p2 p3
            // |l|-----------------|P|------------------|P|----|r|
            if (ka1 < p2 && ka1 > p1 || kb1 < p2 && kb1 > p1) {
                // Advance lower bound
                l = p1 + 1;
                ka1 = ka1 < l ? kb1 : ka1;
                if (kb1 <= p3) {
                    // Entirely in middle
                    r = p2 - 1;
                    kb1 = r < kb1 ? ka1 : kb1;
                    continue;
                }
                introselect(part, a, l, p2 - 1, ka1, ka1, maxDepth);
                // Here we must process right
                ka1 = kb1;
            }
            if (kb1 <= p3) {
                // No right side
                return;
            }
            // Continue right
            l = p3 + 1;
            ka1 = ka1 < l ? kb1 : ka1;
        }
    }

    /**
     * Partition the array such that indices {@code k} correspond to their
     * correctly sorted value in the equivalent fully sorted array.
     *
     * <p>For all indices {@code k} and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>This function accepts a {@link SearchableInterval} of indices {@code k} and the
     * first index {@code ka} and last index {@code kb} that define the range of indices
     * to partition. The {@link SearchableInterval} is used to search for keys in {@code [ka, kb]}
     * to create {@code [ka, kb1]} and {@code [ka1, kb]} if partitioning splits the range.
     *
     * <pre>{@code
     * left <= ka <= kb <= right
     * }</pre>
     *
     * <p>Uses an introselect variant. The dual pivot quickselect is provided as an argument;
     * the fall-back on poor convergence of the quickselect is a heapselect.
     *
     * @param part Partition function.
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param k Interval of indices to partition (ordered).
     * @param ka First key.
     * @param kb Last key.
     * @param maxDepth Maximum depth for recursion.
     */
    // package-private for benchmarking
    void introselect(DPPartition part, double[] a, int left, int right,
        SearchableInterval k, int ka, int kb, int maxDepth) {
        // If partitioning splits the interval then recursion is used for left and/or
        // right sides and the middle remains within this function. If partitioning does
        // not split the interval then it remains within this function.
        int l = left;
        int r = right;
        int ka1 = ka;
        int kb1 = kb;
        final int[] upper = {0, 0, 0};
        while (true) {
            // length - 1
            final int n = r - l;

            if (n < minQuickSelectSize) {
                // Sort selection on small data
                sortSelectRange(a, l, r, ka1, kb1);
                recursionConsumer.accept(maxDepth);
                return;
            }

            // It is possible to use heapselect when ka1 and kb1 are close to the same end
            // |l|-----|ka1|--------|kb1|------|r|
            //  ---------s2-----------
            //          ----------s4-----------
            if (Math.min(kb1 - l, r - ka1) < edgeSelectConstant) {
                edgeSelection.partition(a, l, r, ka1, kb1);
                recursionConsumer.accept(maxDepth);
                return;
            }

            if (maxDepth == 0) {
                // Too much recursion
                heapSelectRange(a, l, r, ka1, kb1);
                recursionConsumer.accept(maxDepth);
                return;
            }

            // Pick 2 pivots and partition
            int p0 = dualPivotingStrategy.pivotIndex(a, l, r, upper);
            p0 = part.partition(a, l, r, p0, upper[0], upper);
            final int p1 = upper[0];
            final int p2 = upper[1];
            final int p3 = upper[2];

            // Recursion to max depth
            // Note: Here we possibly branch left, middle and right with multiple keys.
            // It is possible that the partition has split the keys
            // and the recursion proceeds with a reduced set in each region.
            //                    p0 p1                p2 p3
            // |l|--|ka1|--k----k--|P|------k--|kb1|----|P|----|r|
            //                 kb1  |      ka1
            // Search previous/next is bounded at ka1/kb1
            maxDepth--;
            // Recurse left side if required
            if (ka1 < p0) {
                if (kb1 <= p1) {
                    // Entirely on left side
                    r = p0 - 1;
                    if (r < kb1) {
                        kb1 = k.previousIndex(r);
                    }
                    continue;
                }
                introselect(part, a, l, p0 - 1, k, ka1, k.split(p0, p1, upper), maxDepth);
                ka1 = upper[0];
            }
            // Recurse right side if required
            if (kb1 > p3) {
                if (ka1 >= p2) {
                    // Entirely on right-side
                    l = p3 + 1;
                    if (ka1 < l) {
                        ka1 = k.nextIndex(l);
                    }
                    continue;
                }
                final int lo = k.split(p2, p3, upper);
                introselect(part, a, p3 + 1, r, k, upper[0], kb1, maxDepth);
                kb1 = lo;
            }
            // Check the interval overlaps the middle; and the middle exists.
            //                    p0 p1                p2 p3
            // |l|-----------------|P|------------------|P|----|r|
            // Eliminate:     ----kb1                    ka1----
            if (kb1 <= p1 || p2 <= ka1 || p2 - p1 <= 2) {
                // No middle
                recursionConsumer.accept(maxDepth);
                return;
            }
            l = p1 + 1;
            r = p2 - 1;
            // Interval [ka1, kb1] overlaps the middle but there may be nothing in the interval.
            // |l|-----------------|P|------------------|P|----|r|
            // Eliminate:          ka1                  kb1
            // Detect this if ka1 is advanced too far.
            if (ka1 < l) {
                ka1 = k.nextIndex(l);
                if (ka1 > r) {
                    // No middle
                    recursionConsumer.accept(maxDepth);
                    return;
                }
            }
            if (r < kb1) {
                kb1 = k.previousIndex(r);
            }
        }
    }

    /**
     * Partition the array such that indices {@code k} correspond to their
     * correctly sorted value in the equivalent fully sorted array.
     *
     * <p>For all indices {@code k} and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>This function accepts a {@link UpdatingInterval} of indices {@code k} that define the
     * range of indices to partition. The {@link UpdatingInterval} can be narrowed or split as
     * partitioning divides the range.
     *
     * <p>Uses an introselect variant. The dual pivot quickselect is provided as an argument;
     * the fall-back on poor convergence of the quickselect is a heapselect.
     *
     * @param part Partition function.
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param k Interval of indices to partition (ordered).
     * @param maxDepth Maximum depth for recursion.
     */
    // package-private for benchmarking
    void introselect(DPPartition part, double[] a, int left, int right,
        UpdatingInterval k, int maxDepth) {
        // If partitioning splits the interval then recursion is used for left and/or
        // right sides and the middle remains within this function. If partitioning does
        // not split the interval then it remains within this function.
        int l = left;
        int r = right;
        int ka = k.left();
        int kb = k.right();
        final int[] upper = {0, 0, 0};
        while (true) {
            // length - 1
            final int n = r - l;

            if (n < minQuickSelectSize) {
                // Sort selection on small data
                sortSelectRange(a, l, r, ka, kb);
                recursionConsumer.accept(maxDepth);
                return;
            }

            // It is possible to use heapselect when ka and kb are close to the same end
            // |l|-----|ka|--------|kb|------|r|
            //  ---------s2-----------
            //          ----------s4-----------
            if (Math.min(kb - l, r - ka) < edgeSelectConstant) {
                edgeSelection.partition(a, l, r, ka, kb);
                recursionConsumer.accept(maxDepth);
                return;
            }

            if (maxDepth == 0) {
                // Too much recursion
                heapSelectRange(a, l, r, ka, kb);
                recursionConsumer.accept(maxDepth);
                return;
            }

            // Pick 2 pivots and partition
            int p0 = dualPivotingStrategy.pivotIndex(a, l, r, upper);
            p0 = part.partition(a, l, r, p0, upper[0], upper);
            final int p1 = upper[0];
            final int p2 = upper[1];
            final int p3 = upper[2];

            // Recursion to max depth
            // Note: Here we possibly branch left, middle and right with multiple keys.
            // It is possible that the partition has split the keys
            // and the recursion proceeds with a reduced set in each region.
            //                   p0 p1               p2 p3
            // |l|--|ka|--k----k--|P|------k--|kb|----|P|----|r|
            //                 kb  |      ka
            // Search previous/next is bounded at ka/kb
            maxDepth--;
            // Recurse left side if required
            if (ka < p0) {
                if (kb <= p1) {
                    // Entirely on left side
                    r = p0 - 1;
                    if (r < kb) {
                        kb = k.updateRight(r);
                    }
                    continue;
                }
                introselect(part, a, l, p0 - 1, k.splitLeft(p0, p1), maxDepth);
                ka = k.left();
            } else if (kb <= p1) {
                // No middle/right side
                return;
            } else if (ka <= p1) {
                // Advance lower bound
                ka = k.updateLeft(p1 + 1);
            }
            // Recurse middle if required
            if (ka < p2) {
                l = p1 + 1;
                if (kb <= p3) {
                    // Entirely in middle
                    r = p2 - 1;
                    if (r < kb) {
                        kb = k.updateRight(r);
                    }
                    continue;
                }
                introselect(part, a, l, p2 - 1, k.splitLeft(p2, p3), maxDepth);
                ka = k.left();
            } else if (kb <= p3) {
                // No right side
                return;
            } else if (ka <= p3) {
                ka = k.updateLeft(p3 + 1);
            }
            // Continue right
            l = p3 + 1;
        }
    }

    /**
     * Partition the array such that indices {@code k} correspond to their
     * correctly sorted value in the equivalent fully sorted array.
     *
     * <p>For all indices {@code k} and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>This function accepts a {@link SplittingInterval} of indices {@code k} that define the
     * range of indices to partition. The {@link SplittingInterval} is split as
     * partitioning divides the range.
     *
     * <p>Uses an introselect variant. The dual pivot quickselect is provided as an argument;
     * the fall-back on poor convergence of the quickselect is a heapselect.
     *
     * @param part Partition function.
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param k Interval of indices to partition (ordered).
     * @param maxDepth Maximum depth for recursion.
     */
    // package-private for benchmarking
    void introselect(DPPartition part, double[] a, int left, int right,
        SplittingInterval k, int maxDepth) {
        // If partitioning splits the interval then recursion is used for left and/or
        // right sides and the middle remains within this function. If partitioning does
        // not split the interval then it remains within this function.
        int l = left;
        int r = right;
        int ka = k.left();
        int kb = k.right();
        final int[] upper = {0, 0, 0};
        while (true) {
            // length - 1
            final int n = r - l;

            if (n < minQuickSelectSize) {
                // Sort selection on small data
                sortSelectRange(a, l, r, ka, kb);
                recursionConsumer.accept(maxDepth);
                return;
            }

            // It is possible to use heapselect when ka and kb are close to the same end
            // |l|-----|ka|--------|kb|------|r|
            //  ---------s2-----------
            //          ----------s4-----------
            if (Math.min(kb - l, r - ka) < edgeSelectConstant) {
                edgeSelection.partition(a, l, r, ka, kb);
                recursionConsumer.accept(maxDepth);
                return;
            }

            if (maxDepth == 0) {
                // Too much recursion
                heapSelectRange(a, l, r, ka, kb);
                recursionConsumer.accept(maxDepth);
                return;
            }

            // Pick 2 pivots and partition
            int p0 = dualPivotingStrategy.pivotIndex(a, l, r, upper);
            p0 = part.partition(a, l, r, p0, upper[0], upper);
            final int p1 = upper[0];
            final int p2 = upper[1];
            final int p3 = upper[2];

            // Recursion to max depth
            // Note: Here we possibly branch left, middle and right with multiple keys.
            // It is possible that the partition has split the keys
            // and the recursion proceeds with a reduced set in each region.
            //                   p0 p1               p2 p3
            // |l|--|ka|--k----k--|P|------k--|kb|----|P|----|r|
            //                 kb  |      ka
            // Search previous/next is bounded at ka/kb
            maxDepth--;
            SplittingInterval lk = k.split(p0, p1);
            // Recurse left side if required
            if (lk != null) {
                // Avoid recursive method calls
                if (k.empty()) {
                    // Entirely on left side
                    r = p0 - 1;
                    kb = lk.right();
                    k = lk;
                    continue;
                }
                introselect(part, a, l, p0 - 1, lk, maxDepth);
            }
            if (k.empty()) {
                // No middle/right side
                recursionConsumer.accept(maxDepth);
                return;
            }
            lk = k.split(p2, p3);
            // Recurse middle side if required
            if (lk != null) {
                // Avoid recursive method calls
                if (k.empty()) {
                    // Entirely in middle side
                    l = p1 + 1;
                    r = p2 - 1;
                    ka = lk.left();
                    kb = lk.right();
                    k = lk;
                    continue;
                }
                introselect(part, a, p1 + 1, p2 - 1, lk, maxDepth);
            }
            if (k.empty()) {
                // No right side
                recursionConsumer.accept(maxDepth);
                return;
            }
            // Continue right
            l = p3 + 1;
            ka = k.left();
        }
    }

    /**
     * Partition the array such that indices {@code k} correspond to their
     * correctly sorted value in the equivalent fully sorted array.
     *
     * <p>For all indices {@code k} and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     *
     * <p>This function accepts an {@link IndexIterator} of indices {@code k}; for
     * convenience the lower and upper indices of the current interval are passed as the
     * first index {@code ka} and last index {@code kb} of the closed interval of indices
     * to partition. These may be within the lower and upper indices if the interval was
     * split during recursion: {@code lower <= ka <= kb <= upper}.
     *
     * <p>The data is recursively partitioned using left-most ordering. When the current
     * interval has been partitioned the {@link IndexIterator} is used to advance to the
     * next interval to partition.
     *
     * <p>Uses an introselect variant. The dual pivot quickselect is provided as an argument;
     * the fall-back on poor convergence of the quickselect is a heapselect.
     *
     * @param part Partition function.
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param k Interval of indices to partition (ordered).
     * @param ka First key.
     * @param kb Last key.
     * @param maxDepth Maximum depth for recursion.
     */
    // package-private for benchmarking
    void introselect(DPPartition part, double[] a, int left, int right,
        IndexIterator k, int ka, int kb, int maxDepth) {
        // If partitioning splits the interval then recursion is used for left and/or
        // right sides and the middle remains within this function. If partitioning does
        // not split the interval then it remains within this function.
        int l = left;
        final int r = right;
        int lo = ka;
        int hi = kb;
        final int[] upper = {0, 0, 0};
        while (true) {
            if (maxDepth == 0) {
                // Too much recursion.
                // Advance the iterator to the end of the current range.
                // Note: heapSelectRange handles hi > right.
                // Single API method: advanceBeyond(right): return hi <= right
                while (hi < right && k.next()) {
                    hi = k.right();
                }
                heapSelectRange(a, l, right, lo, hi);
                recursionConsumer.accept(maxDepth);
                return;
            }

            // length - 1
            final int n = right - l;

            // If interval is close to one end then heapselect.
            // Only heapselect left if there are no further indices in the range.
            // |l|-----|lo|--------|hi|------|right|
            //  ---------d1----------
            //          --------------d2-----------
            if (Math.min(hi - l, right - lo) < edgeSelectConstant) {
                if (hi - l > right - lo) {
                    // Right end. Do not check above hi, just select to the end
                    edgeSelection.partition(a, l, right, lo, right);
                    recursionConsumer.accept(maxDepth);
                    return;
                } else if (k.nextAfter(right)) {
                    // Left end
                    // Only if no further indices in the range.
                    // If false this branch will continue to be triggered until
                    // a partition is made to separate the next indices.
                    edgeSelection.partition(a, l, right, lo, hi);
                    recursionConsumer.accept(maxDepth);
                    // Advance iterator
                    l = hi + 1;
                    if (!k.positionAfter(hi) || Math.max(k.left(), l) > right) {
                        // No more keys, or keys beyond the current bounds
                        return;
                    }
                    lo = Math.max(k.left(), l);
                    hi = Math.min(right, k.right());
                    // Continue right (allows a second heap select for the right side)
                    continue;
                }
            }

            // If interval is close to both ends then sort
            // |l|-----|lo|--------|hi|------|right|
            //  ---d1----
            //                       ----d2--------
            // (lo - l) + (right - hi) == (right - l) - (hi - lo)
            if (n - (hi - lo) < minQuickSelectSize) {
                // Handle small data. This is done as the JDK sort will
                // use insertion sort for small data. For double data it
                // will also pre-process the data for NaN and signed
                // zeros which is an overhead to avoid.
                if (n < minQuickSelectSize) {
                    // Must not use sortSelectRange in [lo, hi] as the iterator
                    // has not been advanced to check after hi
                    sortSelectRight(a, l, right, lo);
                } else {
                    // Note: This disregards the current level of recursion
                    // but can exploit the JDK's more advanced sort algorithm.
                    Arrays.sort(a, l, right + 1);
                }
                recursionConsumer.accept(maxDepth);
                return;
            }

            // Here: l <= lo <= hi <= right
            // Pick 2 pivots and partition
            int p0 = dualPivotingStrategy.pivotIndex(a, l, r, upper);
            p0 = part.partition(a, l, r, p0, upper[0], upper);
            final int p1 = upper[0];
            final int p2 = upper[1];
            final int p3 = upper[2];

            maxDepth--;
            // Recursion left
            if (lo < p0) {
                introselect(part, a, l, p0 - 1, k, lo, Math.min(hi, p0 - 1), maxDepth);
                // Advance iterator
                if (!k.positionAfter(p1) || k.left() > right) {
                    // No more keys, or keys beyond the current bounds
                    return;
                }
                lo = k.left();
                hi = Math.min(right, k.right());
            }
            if (hi <= p1) {
                // Advance iterator
                if (!k.positionAfter(p1) || k.left() > right) {
                    // No more keys, or keys beyond the current bounds
                    return;
                }
                lo = k.left();
                hi = Math.min(right, k.right());
            }

            // Recursion middle
            l = p1 + 1;
            lo = Math.max(lo, l);
            if (lo < p2) {
                introselect(part, a, l, p2 - 1, k, lo, Math.min(hi, p2 - 1), maxDepth);
                // Advance iterator
                if (!k.positionAfter(p3) || k.left() > right) {
                    // No more keys, or keys beyond the current bounds
                    return;
                }
                lo = k.left();
                hi = Math.min(right, k.right());
            }
            if (hi <= p3) {
                // Advance iterator
                if (!k.positionAfter(p3) || k.left() > right) {
                    // No more keys, or keys beyond the current bounds
                    return;
                }
                lo = k.left();
                hi = Math.min(right, k.right());
            }

            // Continue right
            l = p3 + 1;
            lo = Math.max(lo, l);
        }
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array. For all indices {@code k}
     * and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>Uses a Bentley-McIlroy quicksort partition method by Sedgewick.
     *
     * @param data Values.
     * @param k Indices (may be destructively modified).
     * @param n Count of indices.
     */
    void partitionSBM(double[] data, int[] k, int n) {
        // Handle NaN (this does assume n > 0)
        final int right = sortNaN(data);
        partition((SPEPartitionFunction) this::partitionSBMWithZeros, data, right, k, n);
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array. For all indices {@code k}
     * and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>The method assumes all {@code k} are valid indices into the data.
     * It handles NaN and signed zeros in the data.
     *
     * <p>Uses an introselect variant. Uses the configured single-pivot quicksort method;
     * the fall-back on poor convergence of the quickselect is controlled by
     * current configuration.
     *
     * @param data Values.
     * @param k Indices (may be destructively modified).
     * @param n Count of indices.
     */
    void partitionISP(double[] data, int[] k, int n) {
        introselect(getSPFunction(), data, k, n);
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array. For all indices {@code k}
     * and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>The method assumes all {@code k} are valid indices into the data.
     * It handles NaN and signed zeros in the data.
     *
     * <p>Uses an introselect variant. The quickselect is a dual-pivot quicksort
     * partition method by Vladimir Yaroslavskiy; the fall-back on poor convergence of
     * the quickselect is controlled by current configuration.
     *
     * @param data Values.
     * @param k Indices (may be destructively modified).
     * @param n Count of indices.
     */
    void partitionIDP(double[] data, int[] k, int n) {
        introselect((DPPartition) Partition::partitionDP, data, k, n);
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array. For all indices {@code k}
     * and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>The method assumes all {@code k} are valid indices into the data.
     * It handles NaN and signed zeros in the data.
     *
     * <p>Uses the <a href="https://en.wikipedia.org/wiki/Floyd%E2%80%93Rivest_algorithm">
     * Floyd-Rivest Algorithm (Wikipedia)</a>
     *
     * <p>WARNING: Currently this only supports a single {@code k}. For parity with other
     * select methods this accepts an array {@code k} and pre/post processes the data for
     * NaN and signed zeros.
     *
     * @param a Values.
     * @param k Indices (may be destructively modified).
     * @param count Count of indices.
     */
    void partitionFR(double[] a, int[] k, int count) {
        // Handle NaN / signed zeros
        final DoubleDataTransformer t = SORT_TRANSFORMER.get();
        // Assume this is in-place
        t.preProcess(a);
        final int end = t.length();
        int n = count;
        if (end > 1) {
            // Filter indices invalidated by NaN check
            if (end < a.length) {
                for (int i = n; --i >= 0;) {
                    final int v = k[i];
                    if (v >= end) {
                        // swap(k, i, --n)
                        k[i] = k[--n];
                        k[n] = v;
                    }
                }
            }
            // Only handles a single k
            if (n != 0) {
                selectFR(a, 0, end - 1, k[0], controlFlags);
            }
        }
        // Restore signed zeros
        t.postProcess(a, k, n);
    }

    /**
     * Select the k-th element of the array.
     *
     * <p>Uses the <a href="https://en.wikipedia.org/wiki/Floyd%E2%80%93Rivest_algorithm">
     * Floyd-Rivest Algorithm (Wikipedia)</a>.
     *
     * <p>This code has been adapted from:
     * <pre>
     * Floyd and Rivest (1975)
     * Algorithm 489: The Algorithm SELECT—for Finding the ith Smallest of n elements.
     * Comm. ACM. 18 (3): 173.
     * </pre>
     *
     * @param a Values.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param k Key of interest.
     * @param flags Control behaviour.
     */
    private void selectFR(double[] a, int left, int right, int k, int flags) {
        int l = left;
        int r = right;
        while (true) {
            // The following edgeselect modifications are additions to the
            // FR algorithm. These have been added for testing and only affect the finishing
            // selection of small lengths.

            // It is possible to use edgeselect when k is close to the end
            // |l|-----|ka|--------|kb|------|r|
            //  ---------s2----------
            //          ----------s4-----------
            if (Math.min(k - l, r - k) < edgeSelectConstant) {
                edgeSelection.partition(a, l, r, k, k);
                return;
            }

            // use SELECT recursively on a sample of size S to get an estimate for the
            // (k-l+1)-th smallest element into a[k], biased slightly so that the (k-l+1)-th
            // element is expected to lie in the smaller set after partitioning.
            int pivot = k;
            int p = l;
            int q = r;
            // length - 1
            int n = r - l;
            if (n > 600) {
                ++n;
                final int ith = k - l + 1;
                final double z = Math.log(n);
                final double s = 0.5 * Math.exp(0.6666666666666666 * z);
                final double sd = 0.5 * Math.sqrt(z * s * (n - s) / n) * Integer.signum(ith - (n >> 1));
                final int ll = Math.max(l, (int) (k - ith * s / n + sd));
                final int rr = Math.min(r, (int) (k + (n - ith) * s / n + sd));
                // Optional: sample [l, r] into [l, rs]
                if ((flags & FLAG_SUBSET_SAMPLING) != 0) {
                    // Create a random sample at the left end.
                    // This creates an unbiased random sample.
                    // This method is not as fast as sampling into [ll, rr] (see below).
                    final IntUnaryOperator rng = createRNG(n, k);
                    final int rs = l + rr - ll;
                    for (int i = l - 1; i < rs;) {
                        // r - rand [0, r - i + 1) : i is currently i-1
                        final int j = r - rng.applyAsInt(r - i);
                        final double t = a[++i];
                        a[i] = a[j];
                        a[j] = t;
                    }
                    selectFR(a, l, rs, k - ll + l, flags);
                    // Current:
                    // |l      |k-ll+l|     rs|                               r|
                    // |  < v  |   v  |  > v  |              ???               |
                    // Move partitioned data
                    // |l       |p                     |k|            q|      r|
                    // |  < v   |         ???          |v|      ???    |  > v  |
                    p = k - ll + l;
                    q = r - rs + p;
                    vectorSwap(a, p + 1, rs, r);
                    vectorSwap(a, p, p, k);
                } else {
                    // Note: Random sampling is a redundant overhead on fully random data
                    // and will part destroy sorted data. On data that is: partially partitioned;
                    // has many repeat elements; or is structured with repeat patterns, the
                    // shuffle removes side-effects of patterns and stabilises performance.
                    if ((flags & FLAG_RANDOM_SAMPLING) != 0) {
                        // This is not a random sample from [l, r] when k is not exactly
                        // in the middle. By sampling either side of k the sample
                        // will maintain the value of k if the data is already partitioned
                        // around k. However sorted data will be part scrambled by the shuffle.
                        // This sampling has the best performance overall across datasets.
                        final IntUnaryOperator rng = createRNG(n, k);
                        // Shuffle [ll, k) from [l, k)
                        if (ll > l) {
                            for (int i = k; i > ll;) {
                                // l + rand [0, i - l + 1) : i is currently i+1
                                final int j = l + rng.applyAsInt(i - l);
                                final double t = a[--i];
                                a[i] = a[j];
                                a[j] = t;
                            }
                        }
                        // Shuffle (k, rr] from (k, r]
                        if (rr < r) {
                            for (int i = k; i < rr;) {
                                // r - rand [0, r - i + 1) : i is currently i-1
                                final int j = r - rng.applyAsInt(r - i);
                                final double t = a[++i];
                                a[i] = a[j];
                                a[j] = t;
                            }
                        }
                    }
                    selectFR(a, ll, rr, k, flags);
                    // Current:
                    // |l                    |ll      |k|     rr|            r|
                    // |        ???          |  < v   |v|  > v  |      ???    |
                    // Optional: move partitioned data
                    // Unlikely to make a difference as the partitioning will skip
                    // over <v and >v.
                    // |l       |p                    |k|            q|      r|
                    // |  < v   |        ???          |v|      ???    |  > v  |
                    if ((flags & FLAG_MOVE_SAMPLE) != 0) {
                        vectorSwap(a, l, ll - 1, k - 1);
                        vectorSwap(a, k + 1, rr, r);
                        p += k - ll;
                        q -= rr - k;
                    }
                }
            } else {
                // Optional: use pivot strategy
                pivot = pivotingStrategy.pivotIndex(a, l, r, k);
            }

            // This uses the original binary partition of FR.
            // FR sub-sampling can be used in some introselect methods; this
            // allows the original FR to be compared with introselect.

            // Partition a[p : q] about t.
            // Sub-script range checking has been eliminated by appropriate placement of t
            // at the p or q end.
            final double t = a[pivot];
            // swap(left, pivot)
            a[pivot] = a[p];
            if (a[q] > t) {
                // swap(right, left)
                a[p] = a[q];
                a[q] = t;
                // Here after the first swap: a[p] = t; a[q] > t
            } else {
                a[p] = t;
                // Here after the first swap: a[p] <= t; a[q] = t
            }
            int i = p;
            int j = q;
            while (i < j) {
                // swap(i, j)
                final double temp = a[i];
                a[i] = a[j];
                a[j] = temp;
                do {
                    ++i;
                } while (a[i] < t);
                do {
                    --j;
                } while (a[j] > t);
            }
            if (a[p] == t) {
                // data[j] <= t : swap(left, j)
                a[p] = a[j];
                a[j] = t;
            } else {
                // data[j+1] > t : swap(j+1, right)
                a[q] = a[++j];
                a[j] = t;
            }
            // Continue on the correct side
            if (k < j) {
                r = j - 1;
            } else if (k > j) {
                l = j + 1;
            } else {
                return;
            }
        }
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array. For all indices {@code k}
     * and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>The method assumes all {@code k} are valid indices into the data.
     * It handles NaN and signed zeros in the data.
     *
     * <p>Uses the <a href="https://en.wikipedia.org/wiki/Floyd%E2%80%93Rivest_algorithm">
     * Floyd-Rivest Algorithm (Wikipedia)</a>, modified by Kiwiel.
     *
     * <p>WARNING: Currently this only supports a single {@code k}. For parity with other
     * select methods this accepts an array {@code k} and pre/post processes the data for
     * NaN and signed zeros.
     *
     * @param a Values.
     * @param k Indices (may be destructively modified).
     * @param count Count of indices.
     */
    void partitionKFR(double[] a, int[] k, int count) {
        // Handle NaN / signed zeros
        final DoubleDataTransformer t = SORT_TRANSFORMER.get();
        // Assume this is in-place
        t.preProcess(a);
        final int end = t.length();
        int n = count;
        if (end > 1) {
            // Filter indices invalidated by NaN check
            if (end < a.length) {
                for (int i = n; --i >= 0;) {
                    final int v = k[i];
                    if (v >= end) {
                        // swap(k, i, --n)
                        k[i] = k[--n];
                        k[n] = v;
                    }
                }
            }
            // Only handles a single k
            if (n != 0) {
                final int[] bounds = new int[5];
                selectKFR(a, 0, end - 1, k[0], bounds, null);
            }
        }
        // Restore signed zeros
        t.postProcess(a, k, n);
    }

    /**
     * Select the k-th element of the array.
     *
     * <p>Uses the <a href="https://en.wikipedia.org/wiki/Floyd%E2%80%93Rivest_algorithm">
     * Floyd-Rivest Algorithm (Wikipedia)</a>, modified by Kiwiel.
     *
     * <p>References:
     * <ul>
     * <li>Floyd and Rivest (1975)
     * Algorithm 489: The Algorithm SELECT—for Finding the ith Smallest of n elements.
     * Comm. ACM. 18 (3): 173.
     * <li>Kiwiel (2005)
     * On Floyd and Rivest's SELECT algorithm.
     * Theoretical Computer Science 347, 214-238.
     * </ul>
     *
     * @param x Values.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param k Key of interest.
     * @param bounds Inclusive bounds {@code [k-, k+]} containing {@code k}.
     * @param rng Random generator for samples in {@code [0, n)}.
     */
    private void selectKFR(double[] x, int left, int right, int k, int[] bounds,
        IntUnaryOperator rng) {
        int l = left;
        int r = right;
        while (true) {
            // The following edgeselect modifications are additions to the
            // KFR algorithm. These have been added for testing and only affect the finishing
            // selection of small lengths.

            // It is possible to use edgeselect when k is close to the end
            // |l|-----|ka|--------|kb|------|r|
            //  ---------s2----------
            //          ----------s4-----------
            if (Math.min(k - l, r - k) < edgeSelectConstant) {
                edgeSelection.partition(x, l, r, k, k);
                bounds[0] = bounds[1] = k;
                return;
            }

            // length - 1
            int n = r - l;
            if (n < 600) {
                // Switch to quickselect
                final int p0 = partitionKBM(x, l, r,
                    pivotingStrategy.pivotIndex(x, l, r, k), bounds);
                final int p1 = bounds[0];
                if (k < p0) {
                    // The element is in the left partition
                    r = p0 - 1;
                } else if (k > p1) {
                    // The element is in the right partition
                    l = p1 + 1;
                } else {
                    // The range contains the element we wanted.
                    bounds[0] = p0;
                    bounds[1] = p1;
                    return;
                }
                continue;
            }

            // Floyd-Rivest sub-sampling
            ++n;
            // Step 1: Choose sample size s <= n-1 and gap g > 0
            final double z = Math.log(n);
            // sample size = alpha * n^(2/3) * ln(n)^1/3  (4.1)
            // sample size = alpha * n^(2/3)              (4.17; original Floyd-Rivest size)
            final double s = 0.5 * Math.exp(0.6666666666666666 * z) * Math.cbrt(z);
            //final double s = 0.5 * Math.exp(0.6666666666666666 * z);
            // gap = sqrt(beta * s * ln(n))
            final double g = Math.sqrt(0.25 * s * z);
            final int rs = (int) (l + s - 1);
            // Step 2: Sample selection
            // Convenient to place the random sample in [l, rs]
            if (rng == null) {
                rng = createRNG(n, k);
            }
            for (int i = l - 1; i < rs;) {
                // r - rand [0, r - i + 1) : i is currently i-1
                final int j = r - rng.applyAsInt(r - i);
                final double t = x[++i];
                x[i] = x[j];
                x[j] = t;
            }

            // Step 3: pivot selection
            final double isn = (k - l + 1) * s / n;
            final int ku = (int) Math.max(Math.floor(l - 1 + isn - g), l);
            final int kv = (int) Math.min(Math.ceil(l - 1 + isn + g), rs);
            // Find u and v by recursion
            selectKFR(x, l, rs, ku, bounds, rng);
            final int kum = bounds[0];
            int kup = bounds[1];
            int kvm;
            int kvp;
            if (kup >= kv) {
                kvm = kv;
                kvp = kup;
                kup = kv - 1;
                // u == v will use single-pivot ternary partitioning
            } else {
                selectKFR(x, kup + 1, rs, kv, bounds, rng);
                kvm = bounds[0];
                kvp = bounds[1];
            }

            // Step 4: Partitioning
            final double u = x[kup];
            final double v = x[kvm];
            // |l      |ku- ku+|                   |kv- kv+|     rs|            r|     (6.4)
            // | x < u | x = u |     u < x < v     | x = v | x > v |      ???    |
            final int ll = kum;
            int pp = kup;
            final int rr = r - rs + kvp;
            int qq = rr - kvp + kvm;
            vectorSwap(x, kvp + 1, rs, r);
            vectorSwap(x, kvm, kvp, rr);
            // |l      |ll   pp|                   |kv-          |qq   rr|      r|     (6.5)
            // | x < u | x = u |     u < x < v     |      ???    | x = v | x > v |

            int a;
            int b;
            int c;
            int d;

            // Note: The quintary partitioning is as specified in Kiwiel.
            // Moving each branch to methods had no effect on performance.

            if (u == v) {
                // Can be optimised by omitting step A1 (moving of sentinels). Here the
                // size of ??? is large and initialisation is insignificant.
                a = partitionKBM(x, ll, rr, pp, bounds);
                d = bounds[0];
                // Make ternary and quintary partitioning compatible
                b = d + 1;
                c = a - 1;
            } else if (k < (r + l) >>> 1) {
                // Left k: u < x[k] < v --> expects x > v.
                // Quintary partitioning using the six-part array:
                // |ll   pp|              p|          |i        j|       |q    rr|     (6.6)
                // | x = u |    u < x < v  |   x < u  |   ???    | x > v | x = v |
                //
                // |ll   pp|              p|              j|i            |q    rr|     (6.7)
                // | x = u |    u < x < v  |   x < u       |       x > v | x = v |
                //
                // Swap the second and third part:
                // |ll   pp|               |b             c|i            |q    rr|     (6.8)
                // | x = u |   x < u       |    u < x < v  |       x > v | x = v |
                //
                // Swap the extreme parts with their neighbours:
                // |ll             |a      |b             c|      d|           rr|     (6.9)
                // |   x < u       | x = u |    u < x < v  | x = v |       x > v |
                int p = kvm - 1;
                int q = qq;
                int i = p;
                int j = q;
                for (;;) {
                    while (x[++i] < v) {
                        if (x[i] < u) {
                            continue;
                        }
                        // u <= xi < v
                        final double xi = x[i];
                        x[i] = x[++p];
                        if (xi > u) {
                            x[p] = xi;
                        } else {
                            x[p] = x[++pp];
                            x[pp] = xi;
                        }
                    }
                    while (x[--j] >= v) {
                        if (x[j] == v) {
                            final double xj = x[j];
                            x[j] = x[--q];
                            x[q] = xj;
                        }
                    }
                    // Here x[j] < v <= x[i]
                    if (i >= j) {
                        break;
                    }
                    //swap(x, i, j)
                    final double xi = x[j];
                    final double xj = x[i];
                    x[i] = xi;
                    x[j] = xj;
                    if (xi > u) {
                        x[i] = x[++p];
                        x[p] = xi;
                    } else if (xi == u) {
                        x[i] = x[++p];
                        x[p] = x[++pp];
                        x[pp] = xi;
                    }
                    if (xj == v) {
                        x[j] = x[--q];
                        x[q] = xj;
                    }
                }
                a = ll + i - p - 1;
                b = a + pp + 1 - ll;
                d = rr - q + 1 + j;
                c = d - rr + q - 1;
                vectorSwap(x, pp + 1, p, j);
                //vectorSwap(x, ll, pp, b - 1);
                //vectorSwap(x, i, q - 1, rr);
                vectorSwapL(x, ll, pp, b - 1, u);
                vectorSwapR(x, i, q - 1, rr, v);
            } else {
                // Right k: u < x[k] < v --> expects x < u.
                // Symmetric quintary partitioning replacing 6.6-6.8 with:
                // |ll    p|          |i        j|       |q              |qq   rr|     (6.10)
                // | x = u |   x < u  |   ???    | x > v |    u < x < v  | x = v |
                //
                // |ll    p|                j|i      |q                  |qq   rr|     (6.11)
                // | x = u |   x < u         | x > v |        u < x < v  | x = v |
                //
                // |ll    p|                j|b                 c|       |qq   rr|     (6.12)
                // | x = u |   x < u         |        u < x < v  | x > v | x = v |
                //
                // |ll               |a      |b                 c|      d|     rr|     (6.9)
                // |   x < u         | x = u |        u < x < v  | x = v | x > v |
                int p = pp;
                int q = qq - kvm + kup + 1;
                int i = p;
                int j = q;
                vectorSwap(x, pp + 1, kvm - 1, qq - 1);
                for (;;) {
                    while (x[++i] <= u) {
                        if (x[i] == u) {
                            final double xi = x[i];
                            x[i] = x[++p];
                            x[p] = xi;
                        }
                    }
                    while (x[--j] > u) {
                        if (x[j] > v) {
                            continue;
                        }
                        // u < xj <= v
                        final double xj = x[j];
                        x[j] = x[--q];
                        if (xj < v) {
                            x[q] = xj;
                        } else {
                            x[q] = x[--qq];
                            x[qq] = xj;
                        }
                    }
                    // Here x[j] < v <= x[i]
                    if (i >= j) {
                        break;
                    }
                    //swap(x, i, j)
                    final double xi = x[j];
                    final double xj = x[i];
                    x[i] = xi;
                    x[j] = xj;
                    if (xi == u) {
                        x[i] = x[++p];
                        x[p] = xi;
                    }
                    if (xj < v) {
                        x[j] = x[--q];
                        x[q] = xj;
                    } else if (xj == v) {
                        x[j] = x[--q];
                        x[q] = x[--qq];
                        x[qq] = xj;
                    }
                }
                a = ll + i - p - 1;
                b = a + p + 1 - ll;
                d = rr - q + 1 + j;
                c = d - rr + qq - 1;
                vectorSwap(x, i, q - 1, qq - 1);
                //vectorSwap(x, ll, p, j);
                //vectorSwap(x, c + 1, qq - 1, rr);
                vectorSwapL(x, ll, p, j, u);
                vectorSwapR(x, c + 1, qq - 1, rr, v);
            }

            // Step 5/6/7: Stopping test, reduction and recursion
            // |l              |a      |b             c|      d|            r|
            // |   x < u       | x = u |    u < x < v  | x = v |       x > v |
            if (a <= k) {
                l = b;
            }
            if (c < k) {
                l = d + 1;
            }
            if (k <= d) {
                r = c;
            }
            if (k < b) {
                r = a - 1;
            }
            if (l >= r) {
                if (l == r) {
                    // [b, c]
                    bounds[0] = bounds[1] = k;
                } else {
                    // l > r
                    bounds[0] = r + 1;
                    bounds[1] = l - 1;
                }
                return;
            }
        }
    }

    /**
     * Vector swap x[a:b] <-> x[b+1:c] means the first m = min(b+1-a, c-b)
     * elements of the array x[a:c] are exchanged with its last m elements.
     *
     * @param x Array.
     * @param a Index.
     * @param b Index.
     * @param c Index.
     */
    private static void vectorSwap(double[] x, int a, int b, int c) {
        for (int i = a - 1, j = c + 1, m = Math.min(b + 1 - a, c - b); --m >= 0;) {
            final double v = x[++i];
            x[i] = x[--j];
            x[j] = v;
        }
    }

    /**
     * Vector swap x[a:b] <-> x[b+1:c] means the first m = min(b+1-a, c-b)
     * elements of the array x[a:c] are exchanged with its last m elements.
     *
     * <p>This is a specialisation of {@link #vectorSwap(double[], int, int, int)}
     * where the current left-most value is a constant {@code v}.
     *
     * @param x Array.
     * @param a Index.
     * @param b Index.
     * @param c Index.
     * @param v Constant value in [a, b]
     */
    private static void vectorSwapL(double[] x, int a, int b, int c, double v) {
        for (int i = a - 1, j = c + 1, m = Math.min(b + 1 - a, c - b); --m >= 0;) {
            x[++i] = x[--j];
            x[j] = v;
        }
    }

    /**
     * Vector swap x[a:b] <-> x[b+1:c] means the first m = min(b+1-a, c-b)
     * elements of the array x[a:c] are exchanged with its last m elements.
     *
     * <p>This is a specialisation of {@link #vectorSwap(double[], int, int, int)}
     * where the current right-most value is a constant {@code v}.
     *
     * @param x Array.
     * @param a Index.
     * @param b Index.
     * @param c Index.
     * @param v Constant value in (b, c]
     */
    private static void vectorSwapR(double[] x, int a, int b, int c, double v) {
        for (int i = a - 1, j = c + 1, m = Math.min(b + 1 - a, c - b); --m >= 0;) {
            x[--j] = x[++i];
            x[i] = v;
        }
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array. For all indices {@code k}
     * and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>The method assumes all {@code k} are valid indices into the data in {@code [0, length)}.
     * It assumes no NaNs or signed zeros in the data. Data must be pre- and post-processed.
     *
     * <p>Uses the configured single-pivot quicksort method;
     * and median-of-medians algorithm for pivot selection with medians-of-5.
     *
     * <p>Note:
     * <p>This method is not configurable with the exception of the single-pivot quickselect method
     * and the size to stop quickselect recursion and finish using sort select. It has been superceded by
     * {@link #partitionLinear(double[], int[], int)} which has configurable deterministic
     * pivot selection including those using partition expansion in-place of full partitioning.
     *
     * @param data Values.
     * @param k Indices (may be destructively modified).
     * @param n Count of indices.
     */
    void partitionLSP(double[] data, int[] k, int n) {
        linearSelect(getSPFunction(), data, k, n);
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array. For all indices {@code k}
     * and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>The method assumes all {@code k} are valid indices into the data.
     * It handles NaN and signed zeros in the data.
     *
     * <p>Uses the median-of-medians algorithm for pivot selection.
     *
     * <p>WARNING: Currently this only supports a single or range of {@code k}.
     * For parity with other select methods this accepts an array {@code k} and pre/post
     * processes the data for NaN and signed zeros.
     *
     * @param part Partition function.
     * @param a Values.
     * @param k Indices (may be destructively modified).
     * @param count Count of indices.
     */
    private void linearSelect(SPEPartition part, double[] a, int[] k, int count) {
        // Handle NaN / signed zeros
        final DoubleDataTransformer t = SORT_TRANSFORMER.get();
        // Assume this is in-place
        t.preProcess(a);
        final int end = t.length();
        int n = count;
        if (end > 1) {
            // Filter indices invalidated by NaN check
            if (end < a.length) {
                for (int i = n; --i >= 0;) {
                    final int v = k[i];
                    if (v >= end) {
                        // swap(k, i, --n)
                        k[i] = k[--n];
                        k[n] = v;
                    }
                }
            }
            if (n != 0) {
                final int ka = Math.min(k[0], k[n - 1]);
                final int kb = Math.max(k[0], k[n - 1]);
                linearSelect(part, a, 0, end - 1, ka, kb, new int[2]);
            }
        }
        // Restore signed zeros
        t.postProcess(a, k, n);
    }

    /**
     * Partition the array such that indices {@code k} correspond to their
     * correctly sorted value in the equivalent fully sorted array.
     *
     * <p>For all indices {@code [ka, kb]} and any index {@code i}:
     *
     * <pre>{@code
     * data[i < ka] <= data[ka] <= data[kb] <= data[kb < i]
     * }</pre>
     *
     * <p>This function accepts indices {@code [ka, kb]} that define the
     * range of indices to partition. It is expected that the range is small.
     *
     * <p>Uses quickselect with median-of-medians pivot selection to provide Order(n)
     * performance.
     *
     * <p>Returns the bounds containing {@code [ka, kb]}. These may be lower/higher
     * than the keys if equal values are present in the data. This is to be used by
     * {@link #pivotMedianOfMedians(SPEPartition, double[], int, int, int[])} to identify
     * the equal value range of the pivot.
     *
     * @param part Partition function.
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param ka First key of interest.
     * @param kb Last key of interest.
     * @param bounds Bounds of the range containing {@code [ka, kb]} (inclusive).
     * @see <a href="https://en.wikipedia.org/wiki/Median_of_medians">Median of medians (Wikipedia)</a>
     */
    private void linearSelect(SPEPartition part, double[] a, int left, int right, int ka, int kb,
            int[] bounds) {
        int l = left;
        int r = right;
        while (true) {
            // Select when ka and kb are close to the same end
            // |l|-----|ka|kkkkkkkk|kb|------|r|
            // Optimal value for this is much higher than standard quickselect due
            // to the high cost of median-of-medians pivot computation and reuse via
            // mutual recursion so we have a different value.
            if (Math.min(kb - l, r - ka) < linearSortSelectSize) {
                sortSelectRange(a, l, r, ka, kb);
                // We could scan left/right to extend the bounds here after the sort.
                // Since the move_sample strategy is not generally useful we do not bother.
                bounds[0] = ka;
                bounds[1] = kb;
                return;
            }
            int p0 = pivotMedianOfMedians(part, a, l, r, bounds);
            if ((controlFlags & FLAG_MOVE_SAMPLE) != 0) {
                // Note: medians with 5 elements creates a sample size of 20%.
                // Avoid partitioning the sample known to be above the pivot.
                // The pivot identified the lower pivot (lp) and upper pivot (p).
                // This strategy is not faster unless there are a large number of duplicates
                // (e.g. less than 10 unique values).
                // On random data with no duplicates this is slower.
                // Note: The methods based on quickselect adaptive create the sample in
                // a region corresponding to expected k and expand the partition (faster).
                //
                // |l  |lp p0| rr|                              r|
                // | < |  == | > |        ???                    |
                //
                // Move region above P to r
                //
                // |l  |pp p0|                                  r|
                // | < |  == |           ???                 | > |
                final int lp = bounds[0];
                final int rr = bounds[1];
                vectorSwap(a, p0 + 1, rr, r);
                // 20% less to partition
                final int p = part.partition(a, p0, r - rr + p0, p0, bounds);
                // |l    |pp  |p0         |p  u|                r|
                // |  <  | == |    <      | == |        >        |
                //
                // Move additional equal pivot region to the centre:
                // |l                |p0      u|                r|
                // |        <        |   ==    |        >        |
                vectorSwapL(a, lp, p0 - 1, p - 1, a[p]);
                p0 = p - p0 + lp;
            } else {
                p0 = part.partition(a, l, r, p0, bounds);
            }
            final int p1 = bounds[0];

            // Note: Here we expect [ka, kb] to be small and splitting is unlikely.
            //                   p0 p1
            // |l|--|ka|kkkk|kb|--|P|-------------------|r|
            // |l|----------------|P|--|ka|kkk|kb|------|r|
            // |l|-----------|ka|k|P|k|kb|--------------|r|
            if (kb < p0) {
                // Entirely on left side
                r = p0 - 1;
            } else if (ka > p1) {
                // Entirely on right side
                l = p1 + 1;
            } else {
                // Pivot splits [ka, kb]. Expect ends to be close to the pivot and finish.
                // Here we set the bounds for use after median-of-medians pivot selection.
                // In the event there are many equal values this allows collecting those
                // known to be equal together when moving around the medians sample.
                bounds[0] = p0;
                bounds[1] = p1;
                if (ka < p0) {
                    sortSelectRight(a, l, p0 - 1, ka);
                    bounds[0] = ka;
                }
                if (kb > p1) {
                    sortSelectLeft(a, p1 + 1, r, kb);
                    bounds[1] = kb;
                }
                return;
            }
        }
    }

    /**
     * Compute the median-of-medians pivot. Divides the length {@code n} into groups
     * of at most 5 elements, computes the median of each group, and the median of the
     * {@code n/5} medians. Assumes {@code l <= r}.
     *
     * <p>The median-of-medians in computed in-place at the left end. The range containing
     * the medians is {@code [l, rr]} with the right bound {@code rr} returned.
     * In the event the pivot is a region of equal values, the range of the pivot values
     * is {@code [lp, p]}, with the {@code p} returned and {@code lp} set in the output bounds.
     *
     * @param part Partition function.
     * @param a Values.
     * @param l Lower bound of data (inclusive, assumed to be strictly positive).
     * @param r Upper bound of data (inclusive, assumed to be strictly positive).
     * @param bounds Bounds {@code [lp, rr]}.
     * @return the pivot index {@code p}
     */
    private int pivotMedianOfMedians(SPEPartition part, double[] a, int l, int r, int[] bounds) {
        // Process blocks of 5.
        // Moves the median of each block to the left of the array.
        int rr = l - 1;
        for (int e = l + 5;; e += 5) {
            if (e > r) {
                // Final block of size 1-5
                Sorting.sort(a, e - 5, r);
                final int m = (e - 5 + r) >>> 1;
                final double v = a[m];
                a[m] = a[++rr];
                a[rr] = v;
                break;
            }

            // Various methods for time-critical step.
            // Each must be compiled and run on the same benchmark data.
            // Decision tree is fastest.
            //final int m = Sorting.median5(a, e - 5);
            //final int m = Sorting.median5(a, e - 5, e - 4, e - 3, e - 2, e - 1);
            // Bigger decision tree (same as median5)
            //final int m = Sorting.median5b(a, e - 5);
            // Sorting network of 4 + insertion (3-4% slower)
            //final int m = Sorting.median5c(a, e - 5);
            // In-place median: Sorting of 5, or median of 5
            final int m = e - 3;
            //Sorting.sort(a, e - 5, e - 1); // insertion sort
            //Sorting.sort5(a, e - 5, e - 4, e - 3, e - 2, e - 1);
            Sorting.median5d(a, e - 5, e - 4, e - 3, e - 2, e - 1);

            final double v = a[m];
            a[m] = a[++rr];
            a[rr] = v;
        }

        int m = (l + rr + 1) >>> 1;
        // mutual recursion
        linearSelect(part, a, l, rr, m, m, bounds);
        // bounds contains the range of the pivot.
        // return the upper pivot and record the end of the range.
        m = bounds[1];
        bounds[1] = rr;
        return m;
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array. For all indices {@code k}
     * and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>The method assumes all {@code k} are valid indices into the data in {@code [0, length)}.
     * It assumes no NaNs or signed zeros in the data. Data must be pre- and post-processed.
     *
     * <p>Uses the median-of-medians algorithm to provide Order(n) performance.
     * This method has configurable deterministic pivot selection including those using
     * partition expansion in-place of full partitioning. The methods are based on the
     * QuickselectAdaptive method of Alexandrescu.
     *
     * @param data Values.
     * @param k Indices (may be destructively modified).
     * @param n Count of indices.
     * @see #setLinearStrategy(LinearStrategy)
     */
    void partitionLinear(double[] data, int[] k, int n) {
        quickSelect(linearSpFunction, data, k, n);
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array. For all indices {@code k}
     * and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>The method assumes all {@code k} are valid indices into the data.
     * It handles NaN and signed zeros in the data.
     *
     * <p>This method assumes that the partition function can compute a pivot.
     * It is used for variants of the median-of-medians algorithm which use mutual
     * recursion for pivot selection.
     *
     * <p>WARNING: Currently this only supports a single or range of {@code k}.
     * For parity with other select methods this accepts an array {@code k} and pre/post
     * processes the data for NaN and signed zeros.
     *
     * @param part Partition function.
     * @param a Values.
     * @param k Indices (may be destructively modified).
     * @param count Count of indices.
     * @see #setLinearStrategy(LinearStrategy)
     */
    private void quickSelect(SPEPartition part, double[] a, int[] k, int count) {
        // Handle NaN / signed zeros
        final DoubleDataTransformer t = SORT_TRANSFORMER.get();
        // Assume this is in-place
        t.preProcess(a);
        final int end = t.length();
        int n = count;
        if (end > 1) {
            // Filter indices invalidated by NaN check
            if (end < a.length) {
                for (int i = n; --i >= 0;) {
                    final int v = k[i];
                    if (v >= end) {
                        // swap(k, i, --n)
                        k[i] = k[--n];
                        k[n] = v;
                    }
                }
            }
            if (n != 0) {
                final int ka = Math.min(k[0], k[n - 1]);
                final int kb = Math.max(k[0], k[n - 1]);
                quickSelect(part, a, 0, end - 1, ka, kb, new int[2]);
            }
        }
        // Restore signed zeros
        t.postProcess(a, k, n);
    }

    /**
     * Partition the array such that indices {@code k} correspond to their
     * correctly sorted value in the equivalent fully sorted array.
     *
     * <p>For all indices {@code [ka, kb]} and any index {@code i}:
     *
     * <pre>{@code
     * data[i < ka] <= data[ka] <= data[kb] <= data[kb < i]
     * }</pre>
     *
     * <p>This function accepts indices {@code [ka, kb]} that define the
     * range of indices to partition. It is expected that the range is small.
     *
     * <p>This method assumes that the partition function can compute a pivot.
     * It is used for variants of the median-of-medians algorithm which use mutual
     * recursion for pivot selection. This method is based on the improvements
     * for median-of-medians algorithms in Alexandrescu (2016) (median-of-medians
     * and median-of-median-of-medians).
     *
     * <p>Returns the bounds containing {@code [ka, kb]}. These may be lower/higher
     * than the keys if equal values are present in the data.
     *
     * @param part Partition function.
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param ka First key of interest.
     * @param kb Last key of interest.
     * @param bounds Bounds of the range containing {@code [ka, kb]} (inclusive).
     * @see #setLinearStrategy(LinearStrategy)
     */
    private void quickSelect(SPEPartition part, double[] a, int left, int right, int ka, int kb,
            int[] bounds) {
        int l = left;
        int r = right;
        while (true) {
            // Select when ka and kb are close to the same end
            // |l|-----|ka|kkkkkkkk|kb|------|r|
            // Optimal value for this is much higher than standard quickselect due
            // to the high cost of median-of-medians pivot computation and reuse via
            // mutual recursion so we have a different value.
            // Note: Use of this will not break the Order(n) performance for worst
            // case data, i.e. data where all values require full insertion.
            // This will be Order(n * k) == Order(n); k becomes a multiplier as long as
            // k << n; otherwise worst case is Order(n^2 / 2) when k=n/2.
            if (Math.min(kb - l, r - ka) < linearSortSelectSize) {
                sortSelectRange(a, l, r, ka, kb);
                // We could scan left/right to extend the bounds here after the sort.
                // Attempts to do this were not measurable in benchmarking.
                bounds[0] = ka;
                bounds[1] = kb;
                return;
            }
            // Only target ka; kb is assumed to be close
            final int p0 = part.partition(a, l, r, ka, bounds);
            final int p1 = bounds[0];

            // Note: Here we expect [ka, kb] to be small and splitting is unlikely.
            //                   p0 p1
            // |l|--|ka|kkkk|kb|--|P|-------------------|r|
            // |l|----------------|P|--|ka|kkk|kb|------|r|
            // |l|-----------|ka|k|P|k|kb|--------------|r|
            if (kb < p0) {
                // Entirely on left side
                r = p0 - 1;
            } else if (ka > p1) {
                // Entirely on right side
                l = p1 + 1;
            } else {
                // Pivot splits [ka, kb]. Expect ends to be close to the pivot and finish.
                // Here we set the bounds for use after median-of-medians pivot selection.
                // In the event there are many equal values this allows collecting those
                // known to be equal together when moving around the medians sample.
                bounds[0] = p0;
                bounds[1] = p1;
                if (ka < p0) {
                    sortSelectRight(a, l, p0 - 1, ka);
                    bounds[0] = ka;
                }
                if (kb > p1) {
                    sortSelectLeft(a, p1 + 1, r, kb);
                    bounds[1] = kb;
                }
                return;
            }
        }
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array. For all indices {@code k}
     * and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>The method assumes all {@code k} are valid indices into the data in {@code [0, length)}.
     * It assumes no NaNs or signed zeros in the data. Data must be pre- and post-processed.
     *
     * <p>Uses the QuickselectAdaptive method of Alexandrescu. This is based on the
     * median-of-medians algorithm. The median sample strategy is chosen based on
     * the target index.
     *
     * @param data Values.
     * @param k Indices (may be destructively modified).
     * @param n Count of indices.
     */
    void partitionQA(double[] data, int[] k, int n) {
        quickSelectAdaptive(data, k, n);
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array. For all indices {@code k}
     * and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>The method assumes all {@code k} are valid indices into the data.
     * It handles NaN and signed zeros in the data.
     *
     * <p>WARNING: Currently this only supports a single or range of {@code k}.
     * For parity with other select methods this accepts an array {@code k} and pre/post
     * processes the data for NaN and signed zeros.
     *
     * @param a Values.
     * @param k Indices (may be destructively modified).
     * @param count Count of indices.
     */
    private void quickSelectAdaptive(double[] a, int[] k, int count) {
        // Handle NaN / signed zeros
        final DoubleDataTransformer t = SORT_TRANSFORMER.get();
        // Assume this is in-place
        t.preProcess(a);
        final int end = t.length();
        int n = count;
        if (end > 1) {
            // Filter indices invalidated by NaN check
            if (end < a.length) {
                for (int i = n; --i >= 0;) {
                    final int v = k[i];
                    if (v >= end) {
                        // swap(k, i, --n)
                        k[i] = k[--n];
                        k[n] = v;
                    }
                }
            }
            if (n != 0) {
                final int ka = Math.min(k[0], k[n - 1]);
                final int kb = Math.max(k[0], k[n - 1]);
                quickSelectAdaptive(a, 0, end - 1, ka, kb, new int[1], adaptMode);
            }
        }
        // Restore signed zeros
        t.postProcess(a, k, n);
    }

    /**
     * Partition the array such that indices {@code k} correspond to their
     * correctly sorted value in the equivalent fully sorted array.
     *
     * <p>For all indices {@code [ka, kb]} and any index {@code i}:
     *
     * <pre>{@code
     * data[i < ka] <= data[ka] <= data[kb] <= data[kb < i]
     * }</pre>
     *
     * <p>This function accepts indices {@code [ka, kb]} that define the
     * range of indices to partition. It is expected that the range is small.
     *
     * <p>Uses the QuickselectAdaptive method of Alexandrescu. This is based on the
     * median-of-medians algorithm. The median sample is strategy is chosen based on
     * the target index.
     *
     * <p>The adaption {@code mode} is used to control the sampling mode and adaption of
     * the index within the sample.
     *
     * <p>Returns the bounds containing {@code [ka, kb]}. These may be lower/higher
     * than the keys if equal values are present in the data.
     *
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param ka First key of interest.
     * @param kb Last key of interest.
     * @param bounds Upper bound of the range containing {@code [ka, kb]} (inclusive).
     * @param mode Adaption mode.
     * @return Lower bound of the range containing {@code [ka, kb]} (inclusive).
     */
    private int quickSelectAdaptive(double[] a, int left, int right, int ka, int kb,
            int[] bounds, AdaptMode mode) {
        int l = left;
        int r = right;
        AdaptMode m = mode;
        while (true) {
            // Select when ka and kb are close to the same end
            // |l|-----|ka|kkkkkkkk|kb|------|r|
            // Optimal value for this is much higher than standard quickselect due
            // to the high cost of median-of-medians pivot computation and reuse via
            // mutual recursion so we have a different value.
            if (Math.min(kb - l, r - ka) < linearSortSelectSize) {
                sortSelectRange(a, l, r, ka, kb);
                bounds[0] = kb;
                return ka;
            }

            // Only target ka; kb is assumed to be close
            int p0;
            int n = r - l;
            // f in [0, 1]
            final double f = (double) (ka - l) / n;
            // Note: Margins for fraction left/right of pivot L : R.
            // Subtract the larger margin to create the estimated size
            // after partitioning. If the new size subtracted from
            // the estimated size is negative (partition did not meet
            // the margin guarantees) then adaption mode is changed (to
            // a method more likely to achieve the margins).
            if (f <= STEP_LEFT) {
                if (m.isSampleMode() && n > subSamplingSize) {
                    // Floyd-Rivest sampling. Expect to eliminate the same as QA steps.
                    if (f <= STEP_FAR_LEFT) {
                        n -= (n >> 2) + (n >> 5) + (n >> 6);
                    } else {
                        n -= (n >> 2) + (n >> 3);
                    }
                    p0 = sampleStep(a, l, r, ka, bounds, m);
                } else if (f <= STEP_FAR_LEFT) {
                    if ((controlFlags & FLAG_QA_FAR_STEP) != 0) {
                        // 1/12 : 1/3 (use 1/4 + 1/32 + 1/64 ~ 0.328)
                        n -= (n >> 2) + (n >> 5) + (n >> 6);
                        p0 = repeatedStepFarLeft(a, l, r, ka, bounds, m);
                    } else {
                        // 1/12 : 3/8
                        n -= (n >> 2) + (n >> 3);
                        p0 = repeatedStepLeft(a, l, r, ka, bounds, m, true);
                    }
                } else {
                    // 1/6 : 1/4
                    n -= n >> 2;
                    p0 = repeatedStepLeft(a, l, r, ka, bounds, m, false);
                }
            } else if (f >= STEP_RIGHT) {
                if (m.isSampleMode() && n > subSamplingSize) {
                    // Floyd-Rivest sampling. Expect to eliminate the same as QA steps.
                    if (f >= STEP_FAR_RIGHT) {
                        n -= (n >> 2) + (n >> 5) + (n >> 6);
                    } else {
                        n -= (n >> 2) + (n >> 3);
                    }
                    p0 = sampleStep(a, l, r, ka, bounds, m);
                } else if (f >= STEP_FAR_RIGHT) {
                    if ((controlFlags & FLAG_QA_FAR_STEP) != 0) {
                        // 1/12 : 1/3 (use 1/4 + 1/32 + 1/64 ~ 0.328)
                        n -= (n >> 2) + (n >> 5) + (n >> 6);
                        p0 = repeatedStepFarRight(a, l, r, ka, bounds, m);
                    } else {
                        // 3/8 : 1/12
                        n -= (n >> 2) + (n >> 3);
                        p0 = repeatedStepRight(a, l, r, ka, bounds, m, true);
                    }
                } else {
                    // 1/4 : 1/6
                    n -= n >> 2;
                    p0 = repeatedStepRight(a, l, r, ka, bounds, m, false);
                }
            } else {
                if (m.isSampleMode() && n > subSamplingSize) {
                    // Floyd-Rivest sampling. Expect to eliminate the same as QA steps.
                    p0 = sampleStep(a, l, r, ka, bounds, m);
                } else {
                    p0 = repeatedStep(a, l, r, ka, bounds, m);
                }
                // 2/9 : 2/9 (use 1/4 - 1/32 ~ 0.219)
                n -= (n >> 2) - (n >> 5);
            }

            // Note: Here we expect [ka, kb] to be small and splitting is unlikely.
            //                   p0 p1
            // |l|--|ka|kkkk|kb|--|P|-------------------|r|
            // |l|----------------|P|--|ka|kkk|kb|------|r|
            // |l|-----------|ka|k|P|k|kb|--------------|r|
            final int p1 = bounds[0];
            if (kb < p0) {
                // Entirely on left side
                r = p0 - 1;
            } else if (ka > p1) {
                // Entirely on right side
                l = p1 + 1;
            } else {
                // Pivot splits [ka, kb]. Expect ends to be close to the pivot and finish.
                // Here we set the bounds for use after median-of-medians pivot selection.
                // In the event there are many equal values this allows collecting those
                // known to be equal together when moving around the medians sample.
                if (kb > p1) {
                    sortSelectLeft(a, p1 + 1, r, kb);
                    bounds[0] = kb;
                }
                if (ka < p0) {
                    sortSelectRight(a, l, p0 - 1, ka);
                    p0 = ka;
                }
                return p0;
            }
            // Update sampling mode
            m = m.update(n, l, r);
        }
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array. For all indices {@code k}
     * and any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>The method assumes all {@code k} are valid indices into the data in {@code [0, length)}.
     * It assumes no NaNs or signed zeros in the data. Data must be pre- and post-processed.
     *
     * <p>Uses the QuickselectAdaptive method of Alexandrescu. This is based on the
     * median-of-medians algorithm. The median sample is strategy is chosen based on
     * the target index.
     *
     * <p>Differences to QA
     *
     * <p>This function is not as configurable as {@link #partitionQA(double[], int[], int)};
     * it is composed of the best performing configuration from benchmarking.
     *
     * <p>A key difference is that this method allows starting with Floyd-Rivest sub-sampling,
     * then progression to QuickselectAdaptive sampling, before disabling of sampling. This
     * method can thus have two attempts at sampling (FR, then QA) before disabling sampling. The
     * method can also be configured to start at QA sampling, or skip QA sampling when starting
     * with FR sampling depending on the configured starting mode and increment
     * (see {@link #configureQaAdaptive(int, int)}).
     *
     * @param data Values.
     * @param k Indices (may be destructively modified).
     * @param n Count of indices.
     */
    static void partitionQA2(double[] data, int[] k, int n) {
        quickSelectAdaptive2(data, k, n, qaMode);
    }

    /**
     * Partition the array such that indices {@code k} correspond to their correctly
     * sorted value in the equivalent fully sorted array. For all indices {@code k} and
     * any index {@code i}:
     *
     * <pre>{@code
     * data[i < k] <= data[k] <= data[k < i]
     * }</pre>
     *
     * <p>The method assumes all {@code k} are valid indices into the data. It handles NaN
     * and signed zeros in the data.
     *
     * <p>WARNING: Currently this only supports a single or range of {@code k}. For parity
     * with other select methods this accepts an array {@code k} and pre/post processes
     * the data for NaN and signed zeros.
     *
     * @param a Values.
     * @param k Indices (may be destructively modified).
     * @param count Count of indices.
     * @param flags Adaption flags.
     */
    static void quickSelectAdaptive2(double[] a, int[] k, int count, int flags) {
        // Handle NaN / signed zeros
        final DoubleDataTransformer t = SORT_TRANSFORMER.get();
        // Assume this is in-place
        t.preProcess(a);
        final int end = t.length();
        int n = count;
        if (end > 1) {
            // Filter indices invalidated by NaN check
            if (end < a.length) {
                for (int i = n; --i >= 0;) {
                    final int v = k[i];
                    if (v >= end) {
                        // swap(k, i, --n)
                        k[i] = k[--n];
                        k[n] = v;
                    }
                }
            }
            if (n != 0) {
                final int ka = Math.min(k[0], k[n - 1]);
                final int kb = Math.max(k[0], k[n - 1]);
                quickSelectAdaptive2(a, 0, end - 1, ka, kb, new int[1], flags);
            }
        }
        // Restore signed zeros
        t.postProcess(a, k, n);
    }

    /**
     * Partition the array such that indices {@code k} correspond to their
     * correctly sorted value in the equivalent fully sorted array.
     *
     * <p>For all indices {@code [ka, kb]} and any index {@code i}:
     *
     * <pre>{@code
     * data[i < ka] <= data[ka] <= data[kb] <= data[kb < i]
     * }</pre>
     *
     * <p>This function accepts indices {@code [ka, kb]} that define the
     * range of indices to partition. It is expected that the range is small.
     *
     * <p>Uses the QuickselectAdaptive method of Alexandrescu. This is based on the
     * median-of-medians algorithm. The median sample is strategy is chosen based on
     * the target index.
     *
     * <p>The control {@code flags} are used to control the sampling mode and adaption of
     * the index within the sample.
     *
     * <p>Returns the bounds containing {@code [ka, kb]}. These may be lower/higher
     * than the keys if equal values are present in the data.
     *
     * @param a Values.
     * @param left Lower bound of data (inclusive, assumed to be strictly positive).
     * @param right Upper bound of data (inclusive, assumed to be strictly positive).
     * @param ka First key of interest.
     * @param kb Last key of interest.
     * @param bounds Upper bound of the range containing {@code [ka, kb]} (inclusive).
     * @param flags Adaption flags.
     * @return Lower bound of the range containing {@code [ka, kb]} (inclusive).
     */
    private static int quickSelectAdaptive2(double[] a, int left, int right, int ka, int kb,
            int[] bounds, int flags) {
        int l = left;
        int r = right;
        int m = flags;
        while (true) {
            // Select when ka and kb are close to the same end
            // |l|-----|ka|kkkkkkkk|kb|------|r|
            if (Math.min(kb - l, r - ka) < LINEAR_SORTSELECT_SIZE) {
                sortSelectRange(a, l, r, ka, kb);
                bounds[0] = kb;
                return ka;
            }

            // Only target ka; kb is assumed to be close
            int p0;
            final int n = r - l;
            // f in [0, 1]
            final double f = (double) (ka - l) / n;
            // Record the larger margin (start at 1/4) to create the estimated size.
            // step        L     R
            // far left    1/12  1/3   (use 1/4 + 1/32 + 1/64 ~ 0.328)
            // left        1/6   1/4
            // middle      2/9   2/9   (use 1/4 - 1/32 ~ 0.219)
            int margin = n >> 2;
            if (m < MODE_SAMPLING && r - l > SELECT_SUB_SAMPLING_SIZE) {
                // Floyd-Rivest sample step uses the same margins
                p0 = sampleStep(a, l, r, ka, bounds, flags);
                if (f <= STEP_FAR_LEFT || f >= STEP_FAR_RIGHT) {
                    margin += (n >> 5) + (n >> 6);
                } else if (f > STEP_LEFT && f < STEP_RIGHT) {
                    margin -= n >> 5;
                }
            } else if (f <= STEP_LEFT) {
                if (f <= STEP_FAR_LEFT) {
                    margin += (n >> 5) + (n >> 6);
                    p0 = repeatedStepFarLeft(a, l, r, ka, bounds, m);
                } else {
                    p0 = repeatedStepLeft(a, l, r, ka, bounds, m);
                }
            } else if (f >= STEP_RIGHT) {
                if (f >= STEP_FAR_RIGHT) {
                    margin += (n >> 5) + (n >> 6);
                    p0 = repeatedStepFarRight(a, l, r, ka, bounds, m);
                } else {
                    p0 = repeatedStepRight(a, l, r, ka, bounds, m);
                }
            } else {
                margin -= n >> 5;
                p0 = repeatedStep(a, l, r, ka, bounds, m);
            }

            // Note: Here we expect [ka, kb] to be small and splitting is unlikely.
            //                   p0 p1
            // |l|--|ka|kkkk|kb|--|P|-------------------|r|
            // |l|----------------|P|--|ka|kkk|kb|------|r|
            // |l|-----------|ka|k|P|k|kb|--------------|r|
            final int p1 = bounds[0];
            if (kb < p0) {
                // Entirely on left side
                r = p0 - 1;
            } else if (ka > p1) {
                // Entirely on right side
                l = p1 + 1;
            } else {
                // Pivot splits [ka, kb]. Expect ends to be close to the pivot and finish.
                // Here we set the bounds for use after median-of-medians pivot selection.
                // In the event there are many equal values this allows collecting those
                // known to be equal together when moving around the medians sample.
                if (kb > p1) {
                    sortSelectLeft(a, p1 + 1, r, kb);
                    bounds[0] = kb;
                }
                if (ka < p0) {
                    sortSelectRight(a, l, p0 - 1, ka);
                    p0 = ka;
                }
                return p0;
            }
            // Update mode based on target partition size
            m += r - l > n - margin ? qaIncrement : 0;
        }
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Implements {@link SPEPartitionFunction}. This method is not static as the
     * pivot strategy and minimum quick select size are used within the method.
     *
     * <p>Note: Handles signed zeros.
     *
     * <p>Uses a Bentley-McIlroy quicksort partition method by Sedgewick.
     *
     * @param data Data array.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param upper Upper bound (inclusive) of the pivot range.
     * @param leftInner Flag to indicate {@code left - 1} is a pivot.
     * @param rightInner Flag to indicate {@code right + 1} is a pivot.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private int partitionSBMWithZeros(double[] data, int left, int right, int[] upper,
        boolean leftInner, boolean rightInner) {
        // Single-pivot Bentley-McIlroy quicksort handling equal keys (Sedgewick's algorithm).
        //
        // Partition data using pivot P into less-than, greater-than or equal.
        // P is placed at the end to act as a sentinel.
        // k traverses the unknown region ??? and values moved if equal (l) or greater (g):
        //
        // left    p       i            j         q    right
        // |  ==P  |  <P   |     ???    |   >P    | ==P  |P|
        //
        // At the end P and additional equal values are swapped back to the centre.
        //
        // |         <P        | ==P |            >P        |
        //
        // Adapted from Sedgewick "Quicksort is optimal"
        // https://sedgewick.io/wp-content/themes/sedgewick/talks/2002QuicksortIsOptimal.pdf
        //
        // The algorithm has been changed so that:
        // - A pivot point must be provided.
        // - An edge case where the search meets in the middle is handled.
        // - Equal value data is not swapped to the end. Since the value is fixed then
        //   only the less than / greater than value must be moved from the end inwards.
        //   The end is then assumed to be the equal value. This would not work with
        //   object references. Equivalent swap calls are commented.
        // - Added a fast-forward over initial range containing the pivot.

        // Switch to insertion sort for small range
        if (right - left <= minQuickSelectSize) {
            Sorting.sort(data, left, right, leftInner);
            fixContinuousSignedZeros(data, left, right);
            upper[0] = right;
            return left;
        }

        final int l = left;
        final int r = right;

        int p = l;
        int q = r;

        // Use the pivot index to set the upper sentinel value.
        // Pass -1 as the target k (should trigger an IOOBE if strategy uses it).
        final int pivot = pivotingStrategy.pivotIndex(data, left, right, -1);
        final double v = data[pivot];
        data[pivot] = data[r];
        data[r] = v;

        // Special case: count signed zeros
        int c = 0;
        if (v == 0) {
            c = countMixedSignedZeros(data, left, right);
        }

        // Fast-forward over equal regions to reduce swaps
        while (data[p] == v) {
            if (++p == q) {
                // Edge-case: constant value
                if (c != 0) {
                    sortZero(data, left, right);
                }
                upper[0] = right;
                return left;
            }
        }
        // Cannot overrun as the prior scan using p stopped before the end
        while (data[q - 1] == v) {
            q--;
        }

        int i = p - 1;
        int j = q;

        for (;;) {
            do {
                ++i;
            } while (data[i] < v);
            while (v < data[--j]) {
                if (j == l) {
                    break;
                }
            }
            if (i >= j) {
                // Edge-case if search met on an internal pivot value
                // (not at the greater equal region, i.e. i < q).
                // Move this to the lower-equal region.
                if (i == j && v == data[i]) {
                    //swap(data, i++, p++)
                    data[i] = data[p];
                    data[p] = v;
                    i++;
                    p++;
                }
                break;
            }
            //swap(data, i, j)
            final double vi = data[j];
            final double vj = data[i];
            data[i] = vi;
            data[j] = vj;
            // Move the equal values to the ends
            if (vi == v) {
                //swap(data, i, p++)
                data[i] = data[p];
                data[p] = v;
                p++;
            }
            if (vj == v) {
                //swap(data, j, --q)
                data[j] = data[--q];
                data[q] = v;
            }
        }
        // i is at the end (exclusive) of the less-than region

        // Place pivot value in centre
        //swap(data, r, i)
        data[r] = data[i];
        data[i] = v;

        // Move equal regions to the centre.
        // Set the pivot range [j, i) and move this outward for equal values.
        j = i++;

        // less-equal:
        //   for (int k = l; k < p; k++):
        //     swap(data, k, --j)
        // greater-equal:
        //   for (int k = r; k-- > q; i++) {
        //     swap(data, k, i)

        // Move the minimum of less-equal or less-than
        int move = Math.min(p - l, j - p);
        final int lower = j - (p - l);
        for (int k = l; move-- > 0; k++) {
            data[k] = data[--j];
            data[j] = v;
        }
        // Move the minimum of greater-equal or greater-than
        move = Math.min(r - q, q - i);
        upper[0] = i + (r - q) - 1;
        for (int k = r; move-- > 0; i++) {
            data[--k] = data[i];
            data[i] = v;
        }

        // Special case: fixed signed zeros
        if (c != 0) {
            p = lower;
            while (c-- > 0) {
                data[p++] = -0.0;
            }
            while (p <= upper[0]) {
                data[p++] = 0.0;
            }
        }

        // Equal in [lower, upper]
        return lower;
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Uses a single pivot partition method. This method does not handle equal values
     * at the pivot location: {@code lower == upper}. The method conforms to the
     * {@link SPEPartition} interface to allow use with the single-pivot introselect method.
     *
     * @param data Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param pivot Pivot index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private static int partitionSP(double[] data, int l, int r, int pivot, int[] upper) {
        // Partition data using pivot P into less-than or greater-than.
        //
        // Adapted from Floyd and Rivest (1975)
        // Algorithm 489: The Algorithm SELECT—for Finding the ith Smallest of n elements.
        // Comm. ACM. 18 (3): 173.
        //
        // Sub-script range checking has been eliminated by appropriate placement
        // of values at the ends to act as sentinels.
        //
        // left           i            j               right
        // |<=P|     <P   |     ???    |   >P          |>=P|
        //
        // At the end P is swapped back to the centre.
        //
        // |         <P          |P|             >P        |
        final double v = data[pivot];
        // swap(left, pivot)
        data[pivot] = data[l];
        if (data[r] > v) {
            // swap(right, left)
            data[l] = data[r];
            data[r] = v;
            // Here after the first swap: a[l] = v; a[r] > v
        } else {
            data[l] = v;
            // Here after the first swap: a[l] <= v; a[r] = v
        }
        int i = l;
        int j = r;
        while (i < j) {
            // swap(i, j)
            final double temp = data[i];
            data[i] = data[j];
            data[j] = temp;
            do {
                ++i;
            } while (data[i] < v);
            do {
                --j;
            } while (data[j] > v);
        }
        // Move pivot back to the correct location from either l or r
        if (data[l] == v) {
            // data[j] <= v : swap(left, j)
            data[l] = data[j];
            data[j] = v;
        } else {
            // data[j+1] > v : swap(j+1, right)
            data[r] = data[++j];
            data[j] = v;
        }
        upper[0] = j;
        return j;
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Uses a Bentley-McIlroy quicksort partition method.
     *
     * @param data Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param pivot Initial index of the pivot.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private static int partitionBM(double[] data, int l, int r, int pivot, int[] upper) {
        // Single-pivot Bentley-McIlroy quicksort handling equal keys.
        //
        // Adapted from program 7 in Bentley-McIlroy (1993)
        // Engineering a sort function
        // SOFTWARE—PRACTICE AND EXPERIENCE, VOL.23(11), 1249–1265
        //
        // 3-way partition of the data using a pivot value into
        // less-than, equal or greater-than.
        //
        // First partition data into 4 reqions by scanning the unknown region from
        // left (i) and right (j) and moving equal values to the ends:
        //                  i->       <-j
        // l        p       |           |         q       r
        // |   ==   |   <   |    ???    |    >    |  ==   |
        //
        //                    <-j
        // l        p             i               q       r
        // |   ==   |      <      |       >       |  ==   |
        //
        // Then the equal values are copied from the ends to the centre:
        // | less        |        equal      |    greater |

        int i = l;
        int j = r;
        int p = l;
        int q = r;

        final double v = data[pivot];

        for (;;) {
            while (i <= j && data[i] <= v) {
                if (data[i] == v) {
                    //swap(data, i, p++)
                    data[i] = data[p];
                    data[p] = v;
                    p++;
                }
                i++;
            }
            while (j >= i && data[j] >= v) {
                if (v == data[j]) {
                    //swap(data, j, q--)
                    data[j] = data[q];
                    data[q] = v;
                    q--;
                }
                j--;
            }
            if (i > j) {
                break;
            }
            //swap(data, i++, j--)
            final double tmp = data[j];
            data[j] = data[i];
            data[i] = tmp;
        }

        // Move equal regions to the centre.
        int s = Math.min(p - l, i - p);
        for (int k = l; s > 0; k++, s--) {
            //swap(data, k, i - s)
            data[k] = data[i - s];
            data[i - s] = v;
        }
        s = Math.min(q - j, r - q);
        for (int k = i; --s >= 0; k++) {
            //swap(data, r - s, k)
            data[r - s] = data[k];
            data[k] = v;
        }

        // Set output range
        i = i - p + l;
        j = j - q + r;
        upper[0] = j;

        return i;
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Uses a Bentley-McIlroy quicksort partition method by Sedgewick.
     *
     * @param data Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param pivot Pivot index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @return Lower bound (inclusive) of the pivot range.
     */
    static int partitionSBM(double[] data, int l, int r, int pivot, int[] upper) {
        // Single-pivot Bentley-McIlroy quicksort handling equal keys (Sedgewick's algorithm).
        //
        // Partition data using pivot P into less-than, greater-than or equal.
        // P is placed at the end to act as a sentinel.
        // k traverses the unknown region ??? and values moved if equal (l) or greater (g):
        //
        // left    p       i            j         q    right
        // |  ==P  |  <P   |     ???    |   >P    | ==P  |P|
        //
        // At the end P and additional equal values are swapped back to the centre.
        //
        // |         <P        | ==P |            >P        |
        //
        // Adapted from Sedgewick "Quicksort is optimal"
        // https://sedgewick.io/wp-content/themes/sedgewick/talks/2002QuicksortIsOptimal.pdf
        //
        // Note: The difference between this and the original BM partition is the use of
        // < or > rather than <= and >=. This allows the pivot to act as a sentinel and removes
        // the requirement for checks on i; and j can be checked against an unlikely condition.
        // This method will swap runs of equal values.
        //
        // The algorithm has been changed so that:
        // - A pivot point must be provided.
        // - An edge case where the search meets in the middle is handled.
        // - Added a fast-forward over any initial range containing the pivot.
        // - Changed the final move to perform the minimum moves.

        // Use the pivot index to set the upper sentinel value
        final double v = data[pivot];
        data[pivot] = data[r];
        data[r] = v;

        int p = l;
        int q = r;

        // Fast-forward over equal regions to reduce swaps
        while (data[p] == v) {
            if (++p == q) {
                // Edge-case: constant value
                upper[0] = r;
                return l;
            }
        }
        // Cannot overrun as the prior scan using p stopped before the end
        while (data[q - 1] == v) {
            q--;
        }

        int i = p - 1;
        int j = q;

        for (;;) {
            do {
                ++i;
            } while (data[i] < v);
            while (v < data[--j]) {
                // Cannot use j == i in the event that i == q (already passed j)
                if (j == l) {
                    break;
                }
            }
            if (i >= j) {
                // Edge-case if search met on an internal pivot value
                // (not at the greater equal region, i.e. i < q).
                // Move this to the lower-equal region.
                if (i == j && v == data[i]) {
                    //swap(data, i++, p++)
                    data[i] = data[p];
                    data[p] = v;
                    i++;
                    p++;
                }
                break;
            }
            //swap(data, i, j)
            final double vi = data[j];
            final double vj = data[i];
            data[i] = vi;
            data[j] = vj;
            // Move the equal values to the ends
            if (vi == v) {
                //swap(data, i, p++)
                data[i] = data[p];
                data[p] = v;
                p++;
            }
            if (vj == v) {
                //swap(data, j, --q)
                data[j] = data[--q];
                data[q] = v;
            }
        }
        // i is at the end (exclusive) of the less-than region

        // Place pivot value in centre
        //swap(data, r, i)
        data[r] = data[i];
        data[i] = v;

        // Move equal regions to the centre.
        // Set the pivot range [j, i) and move this outward for equal values.
        j = i++;

        // less-equal:
        //   for k = l; k < p; k++
        //     swap(data, k, --j)
        // greater-equal:
        //   for k = r; k-- > q; i++
        //     swap(data, k, i)

        // Move the minimum of less-equal or less-than
        int move = Math.min(p - l, j - p);
        final int lower = j - (p - l);
        for (int k = l; --move >= 0; k++) {
            data[k] = data[--j];
            data[j] = v;
        }
        // Move the minimum of greater-equal or greater-than
        move = Math.min(r - q, q - i);
        upper[0] = i + (r - q) - 1;
        for (int k = r; --move >= 0; i++) {
            data[--k] = data[i];
            data[i] = v;
        }

        // Equal in [lower, upper]
        return lower;
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Uses a Bentley-McIlroy quicksort partition method by Kiwiel.
     *
     * @param x Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param pivot Pivot index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @return Lower bound (inclusive) of the pivot range.
     */
    static int partitionKBM(double[] x, int l, int r, int pivot, int[] upper) {
        // Single-pivot Bentley-McIlroy quicksort handling equal keys.
        //
        // Partition data using pivot v into less-than, greater-than or equal.
        // The basic idea is to work with the 5 inner parts of the array [ll, rr]
        // by positioning sentinels at l and r:
        //
        // |l |ll   p|          |i          j|         |q   rr| r|           (6.1)
        // |<v|  ==v |     <v   |     ???    |   >v    | ==v  |>v|
        //
        // until the middle part is empty or just contains an element equal to the pivot:
        //
        // |ll   p|              j|   |i          |q   rr|                   (6.2)
        // |  ==v |     <v        |==v|     >v    | ==v  |
        //
        // i.e. j = i-1 or i-2, then swap the ends into the middle:
        //
        // |ll              |a         d|              rr|                   (6.3)
        // |        <v      |     ==v   |      >v        |
        //
        // Adapted from Kiwiel (2005) "On Floyd and Rivest's SELECT algorithm"
        // Theoretical Computer Science 347, 214-238.
        // This is the safeguarded ternary partition Scheme E with modification to
        // prevent vacuous swaps of equal keys (section 5.6) in Kiwiel (2003)
        // Partitioning schemes for quicksort and quickselect,
        // Technical report, Systems Research Institute, Warsaw.
        // http://arxiv.org/abs/cs.DS/0312054
        //
        // Note: The difference between this and Sedgewick's BM is the use of sentinels
        // at either end to remove index checks at both ends and changing the behaviour
        // when i and j meet on a pivot value.
        //
        // The listing in Kiwiel (2005) has been updated:
        // - p and q mark the *inclusive* end of ==v regions.
        // - Added a fast-forward over initial range containing the pivot.
        // - Vector swap is optimised given one side of the exchange is v.

        final double v = x[pivot];
        x[pivot] = x[l];
        x[l] = v;

        int ll = l;
        int rr = r;

        // Ensure x[l] <= v <= x[r]
        if (v < x[r]) {
            --rr;
        } else if (v > x[r]) {
            x[l] = x[r];
            x[r] = v;
            ++ll;
        }

        // Position p and q for pre-in/decrement to write into edge pivot regions
        // Fast-forward over equal regions to reduce swaps
        int p = l;
        while (x[p + 1] == v) {
            if (++p == rr) {
                // Edge-case: constant value in [ll, rr]
                // Return the full range [l, r] as a single edge element
                // will also be partitioned.
                upper[0] = r;
                return l;
            }
        }
        // Cannot overrun as the prior scan using p stopped before the end
        int q = r;
        while (x[q - 1] == v) {
            --q;
        }

        // [ll, p] and [q, rr] are pivot
        // Position for pre-in/decrement
        int i = p;
        int j = q;

        for (;;) {
            do {
                ++i;
            } while (x[i] < v);
            do {
                --j;
            } while (x[j] > v);
            // Here x[j] <= v <= x[i]
            if (i >= j) {
                if (i == j) {
                    // x[i]=x[j]=v; update to leave the pivot in between (j, i)
                    ++i;
                    --j;
                }
                break;
            }
            //swap(x, i, j)
            final double vi = x[j];
            final double vj = x[i];
            x[i] = vi;
            x[j] = vj;
            // Move the equal values to the ends
            if (vi == v) {
                x[i] = x[++p];
                x[p] = v;
            }
            if (vj == v) {
                x[j] = x[--q];
                x[q] = v;
            }
        }

        // Set [a, d] (p and q are offset by 1 from Kiwiel)
        final int a = ll + j - p;
        upper[0] = rr - q + i;

        // Vector swap x[a:b] <-> x[b+1:c] means the first m = min(b+1-a, c-b)
        // elements of the array x[a:c] are exchanged with its last m elements.
        //vectorSwapL(x, ll, p, j, v);
        //vectorSwapR(x, i, q - 1, rr, v);
        // x[ll:p] <-> x[p+1:j]
        for (int m = Math.min(p + 1 - ll, j - p); --m >= 0; ++ll, --j) {
            x[ll] = x[j];
            x[j] = v;
        }
        // x[i:q-1] <-> x[q:rr]
        for (int m = Math.min(q - i, rr - q + 1); --m >= 0; ++i, --rr) {
            x[rr] = x[i];
            x[i] = v;
        }
        return a;
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Uses a Dutch-National-Flag method handling equal keys (version 1).
     *
     * @param data Data array.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param pivot Pivot index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private static int partitionDNF1(double[] data, int left, int right, int pivot, int[] upper) {
        // Dutch National Flag partitioning:
        // https://www.baeldung.com/java-sorting-arrays-with-repeated-entries
        // https://en.wikipedia.org/wiki/Dutch_national_flag_problem

        // Partition data using pivot P into less-than, greater-than or equal.
        // i traverses the unknown region ??? and values moved to the correct end.
        //
        // left    lt      i           gt       right
        // |  < P  |   P   |     ???    |     > P   |
        //
        // We can delay filling in [lt, gt) with P until the end and only
        // move values in the wrong place.

        final double value = data[pivot];

        // Fast-forward initial less-than region
        int lt = left;
        while (data[lt] < value) {
            lt++;
        }

        // Pointers positioned to use pre-increment/decrement
        lt--;
        int gt = right + 1;

        // DNF partitioning which inspects one position per loop iteration
        for (int i = lt; ++i < gt;) {
            final double v = data[i];
            if (v < value) {
                data[++lt] = v;
            } else if (v > value) {
                data[i] = data[--gt];
                data[gt] = v;
                // Ensure data[i] is inspected next time
                i--;
            }
            // else v == value and is in the central region to fill at the end
        }

        // Equal in (lt, gt) so adjust to [lt, gt]
        ++lt;
        upper[0] = --gt;

        // Fill the equal values gap
        for (int i = lt; i <= gt; i++) {
            data[i] = value;
        }

        return lt;
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Uses a Dutch-National-Flag method handling equal keys (version 2).
     *
     * @param data Data array.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param pivot Pivot index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private static int partitionDNF2(double[] data, int left, int right, int pivot, int[] upper) {
        // Dutch National Flag partitioning:
        // https://www.baeldung.com/java-sorting-arrays-with-repeated-entries
        // https://en.wikipedia.org/wiki/Dutch_national_flag_problem

        // Partition data using pivot P into less-than, greater-than or equal.
        // i traverses the unknown region ??? and values moved to the correct end.
        //
        // left    lt      i           gt       right
        // |  < P  |   P   |     ???    |     > P   |
        //
        // We can delay filling in [lt, gt) with P until the end and only
        // move values in the wrong place.

        final double value = data[pivot];

        // Fast-forward initial less-than region
        int lt = left;
        while (data[lt] < value) {
            lt++;
        }

        // Pointers positioned to use pre-increment/decrement: ++x / --x
        lt--;
        int gt = right + 1;

        // Modified DNF partitioning with fast-forward of the greater-than
        // pointer. Note the fast-forward must check bounds.
        for (int i = lt; ++i < gt;) {
            final double v = data[i];
            if (v < value) {
                data[++lt] = v;
            } else if (v > value) {
                // Fast-forward here:
                do {
                    --gt;
                } while (gt > i && data[gt] > value);
                // here data[gt] <= value
                // if data[gt] == value we can skip over it
                if (data[gt] < value) {
                    data[++lt] = data[gt];
                }
                // Move v to the >P side
                data[gt] = v;
            }
            // else v == value and is in the central region to fill at the end
        }

        // Equal in (lt, gt) so adjust to [lt, gt]
        ++lt;
        upper[0] = --gt;

        // Fill the equal values gap
        for (int i = lt; i <= gt; i++) {
            data[i] = value;
        }

        return lt;
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Uses a Dutch-National-Flag method handling equal keys (version 3).
     *
     * @param data Data array.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param pivot Pivot index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private static int partitionDNF3(double[] data, int left, int right, int pivot, int[] upper) {
        // Dutch National Flag partitioning:
        // https://www.baeldung.com/java-sorting-arrays-with-repeated-entries
        // https://en.wikipedia.org/wiki/Dutch_national_flag_problem

        // Partition data using pivot P into less-than, greater-than or equal.
        // i traverses the unknown region ??? and values moved to the correct end.
        //
        // left    lt      i           gt       right
        // |  < P  |   P   |     ???    |     > P   |
        //
        // This version writes in the value of P as it traverses. Any subsequent
        // less-than values will overwrite P values trailing behind i.

        final double value = data[pivot];

        // Fast-forward initial less-than region
        int lt = left;
        while (data[lt] < value) {
            lt++;
        }

        // Pointers positioned to use pre-increment/decrement: ++x / --x
        lt--;
        int gt = right + 1;

        // Note:
        // This benchmarks as faster than DNF1 and equal to DNF2 on random data.
        // On data with (many) repeat values it is faster than DNF2.
        // Both DNF2 & 3 have fast-forward of the gt pointer.

        // Modified DNF partitioning with fast-forward of the greater-than
        // pointer. Here we write in the pivot value at i during the sweep.
        // This acts as a sentinel when fast-forwarding greater-than.
        // It is over-written by any future <P value.
        // [begin, lt] < pivot
        // (lt, i)    == pivot
        // [i, gt)    == ???
        // [gt, end)   > pivot
        for (int i = lt; ++i < gt;) {
            final double v = data[i];
            if (v != value) {
                // Overwrite with the pivot value
                data[i] = value;
                if (v < value) {
                    // Move v to the <P side
                    data[++lt] = v;
                } else {
                    // Fast-forward here cannot pass sentinel
                    // while (data[--gt] > value)
                    do {
                        --gt;
                    } while (data[gt] > value);
                    // Now data[gt] <= value
                    // if data[gt] == value we can skip over it
                    if (data[gt] < value) {
                        data[++lt] = data[gt];
                    }
                    // Move v to the >P side
                    data[gt] = v;
                }
            }
        }

        // Equal in (lt, gt) so adjust to [lt, gt]
        ++lt;
        upper[0] = --gt;

        // In contrast to version 1 and 2 there is no requirement to fill the central
        // region with the pivot value as it was filled during the sweep

        return lt;
    }

    /**
     * Partition an array slice around 2 pivots. Partitioning exchanges array elements
     * such that all elements smaller than pivot are before it and all elements larger
     * than pivot are after it.
     *
     * <p>Uses a dual-pivot quicksort method by Vladimir Yaroslavskiy.
     *
     * <p>This method assumes {@code a[pivot1] <= a[pivot2]}.
     * If {@code pivot1 == pivot2} this triggers a switch to a single-pivot method.
     * It is assumed this indicates that choosing two pivots failed due to many equal
     * values. In this case the single-pivot method uses a Dutch National Flag algorithm
     * suitable for many equal values.
     *
     * <p>This method returns 4 points describing the pivot ranges of equal values.
     *
     * <pre>{@code
     *         |k0  k1|                |k2  k3|
     * |   <P  | ==P1 |  <P1 && <P2    | ==P2 |   >P   |
     * }</pre>
     *
     * <ul>
     * <li>k0: lower pivot1 point
     * <li>k1: upper pivot1 point (inclusive)
     * <li>k2: lower pivot2 point
     * <li>k3: upper pivot2 point (inclusive)
     * </ul>
     *
     * <p>Bounds are set so {@code i < k0},  {@code i > k3} and {@code k1 < i < k2} are
     * unsorted. When the range {@code [k0, k3]} contains fully sorted elements the result
     * is set to {@code k1 = k3; k2 == k0}. This can occur if
     * {@code P1 == P2} or there are zero or 1 value between the pivots
     * {@code P1 < v < P2}. Any sort/partition of ranges [left, k0-1], [k1+1, k2-1] and
     * [k3+1, right] must check the length is {@code > 1}.
     *
     * @param a Data array.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param bounds Points [k1, k2, k3].
     * @param pivot1 Pivot1 location.
     * @param pivot2 Pivot2 location.
     * @return Lower bound (inclusive) of the pivot range [k0].
     */
    static int partitionDP(double[] a, int left, int right, int pivot1, int pivot2, int[] bounds) {
        // Allow caller to choose a single-pivot
        if (pivot1 == pivot2) {
            // Switch to a single pivot sort. This is used when there are
            // estimated to be many equal values so use the fastest equal
            // value single pivot method.
            final int lower = partitionDNF3(a, left, right, pivot1, bounds);
            // Set dual pivot range
            bounds[2] = bounds[0];
            // No unsorted internal region (set k1 = k3; k2 = k0)
            // Note: It is extra work for the caller to detect that this region can be skipped.
            bounds[1] = lower;
            return lower;
        }

        // Dual-pivot quicksort method by Vladimir Yaroslavskiy.
        //
        // Partition data using pivots P1 and P2 into less-than, greater-than or between.
        // Pivot values P1 & P2 are placed at the end. If P1 < P2, P2 acts as a sentinel.
        // k traverses the unknown region ??? and values moved if less-than (lt) or
        // greater-than (gt):
        //
        // left        lt                k           gt        right
        // |P1|  <P1   |   P1 <= & <= P2 |    ???    |    >P2   |P2|
        //
        // <P1           (left, lt)
        // P1<= & <= P2  [lt, k)
        // >P2           (gt, right)
        //
        // At the end pivots are swapped back to behind the lt and gt pointers.
        //
        // |  <P1        |P1|     P1<= & <= P2    |P2|      >P2    |
        //
        // Adapted from Yaroslavskiy
        // http://codeblab.com/wp-content/uploads/2009/09/DualPivotQuicksort.pdf
        //
        // Modified to allow partial sorting (partitioning):
        // - Allow the caller to supply the pivot indices
        // - Ignore insertion sort for tiny array (handled by calling code)
        // - Ignore recursive calls for a full sort (handled by calling code)
        // - Change to fast-forward over initial ascending / descending runs
        // - Change to a single-pivot partition method if the pivots are equal
        // - Change to fast-forward great when v > v2 and either break the sorting
        //   loop, or move a[great] direct to the correct location.
        // - Change to remove the 'div' parameter used to control the pivot selection
        //   using the medians method (div initialises as 3 for 1/3 and 2/3 and increments
        //   when the central region is too large).
        // - Identify a large central region using ~5/8 of the length.

        final double v1 = a[pivot1];
        final double v2 = a[pivot2];

        // Swap ends to the pivot locations.
        a[pivot1] = a[left];
        a[pivot2] = a[right];
        a[left] = v1;
        a[right] = v2;

        // pointers
        int less = left;
        int great = right;

        // Fast-forward ascending / descending runs to reduce swaps.
        // Cannot overrun as end pivots (v1 <= v2) act as sentinels.
        do {
            ++less;
        } while (a[less] < v1);
        do {
            --great;
        } while (a[great] > v2);

        // a[less - 1] < P1 : a[great + 1] > P2
        // unvisited in [less, great]
        SORTING:
        for (int k = less - 1; ++k <= great;) {
            final double v = a[k];
            if (v < v1) {
                // swap(a, k, less++)
                a[k] = a[less];
                a[less] = v;
                less++;
            } else if (v > v2) {
                // while k < great and a[great] > v2:
                //   great--
                while (a[great] > v2) {
                    if (great-- == k) {
                        // Done
                        break SORTING;
                    }
                }
                // swap(a, k, great--)
                // if a[k] < v1:
                //   swap(a, k, less++)
                final double w = a[great];
                a[great] = v;
                great--;
                // delay a[k] = w
                if (w < v1) {
                    a[k] = a[less];
                    a[less] = w;
                    less++;
                } else {
                    a[k] = w;
                }
            }
        }

        // Change to inclusive ends : a[less] < P1 : a[great] > P2
        less--;
        great++;
        // Move the pivots to correct locations
        a[left] = a[less];
        a[less] = v1;
        a[right] = a[great];
        a[great] = v2;

        // Record the pivot locations
        final int lower = less;
        bounds[2] = great;

        // equal elements
        // Original paper: If middle partition is bigger than a threshold
        // then check for equal elements.

        // Note: This is extra work. When performing partitioning the region of interest
        // may be entirely above or below the central region and this could be skipped.
        // Versions that do this are not measurably faster. Skipping this may be faster
        // if this step can be skipped on the initial largest region. The 5/8 size occurs
        // approximately ~7% of the time on random data (verified using collated statistics).

        // Here we look for equal elements if the centre is more than 5/8 the length.
        // 5/8 = 1/2 + 1/8. Pivots must be different.
        if ((great - less) > ((right - left) >>> 1) + ((right - left) >>> 3) && v1 != v2) {

            // Fast-forward to reduce swaps. Changes inclusive ends to exclusive ends.
            // Since v1 != v2 these act as sentinels to prevent overrun.
            do {
                ++less;
            } while (a[less] == v1);
            do {
                --great;
            } while (a[great] == v2);

            // This copies the logic in the sorting loop using == comparisons
            EQUAL:
            for (int k = less - 1; ++k <= great;) {
                final double v = a[k];
                if (v == v1) {
                    a[k] = a[less];
                    a[less] = v;
                    less++;
                } else if (v == v2) {
                    while (a[great] == v2) {
                        if (great-- == k) {
                            // Done
                            break EQUAL;
                        }
                    }
                    final double w = a[great];
                    a[great] = v;
                    great--;
                    if (w == v1) {
                        a[k] = a[less];
                        a[less] = w;
                        less++;
                    } else {
                        a[k] = w;
                    }
                }
            }

            // Change to inclusive ends
            less--;
            great++;
        }

        // Between pivots in (less, great)
        if (v1 < v2 && less < great - 1) {
            // Record the pivot end points
            bounds[0] = less;
            bounds[1] = great;
        } else {
            // No unsorted internal region (set k1 = k3; k2 = k0)
            bounds[0] = bounds[2];
            bounds[1] = lower;
        }

        return lower;
    }

    /**
     * Expand a partition around a single pivot. Partitioning exchanges array
     * elements such that all elements smaller than pivot are before it and all
     * elements larger than pivot are after it. The central region is already
     * partitioned.
     *
     * <pre>{@code
     * |l             |s   |p0 p1|   e|                r|
     * |    ???       | <P | ==P | >P |        ???      |
     * }</pre>
     *
     * <p>This method requires that {@code left < start && end < right}. It supports
     * {@code start == end}.
     *
     * @param a Data array.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param start Start of the partition range (inclusive).
     * @param end End of the partitioned range (inclusive).
     * @param pivot0 Lower pivot location (inclusive).
     * @param pivot1 Upper pivot location (inclusive).
     * @param upper Upper bound (inclusive) of the pivot range [k1].
     * @return Lower bound (inclusive) of the pivot range [k0].
     */
    private static int expandPartitionT1(double[] a, int left, int right, int start, int end,
        int pivot0, int pivot1, int[] upper) {
        // 3-way partition of the data using a pivot value into
        // less-than, equal or greater-than.
        // Based on Sedgewick's Bentley-McIroy partitioning: always swap i<->j then
        // check for equal to the pivot and move again.
        //
        // Move sentinels from start and end to left and right. Scan towards the
        // sentinels until >=,<=. Swap then move == to the pivot region.
        //           <-i                           j->
        // |l |        |            |p0  p1|       |             | r|
        // |>=|   ???  |     <      |  ==  |   >   |     ???     |<=|
        //
        // When either i or j reach the edge perform finishing loop.
        // Finish loop for a[j] <= v replaces j with p1+1, moves value
        // to p0 for < and updates the pivot range p1 (and optionally p0):
        //                                             j->
        // |l                       |p0  p1|           |         | r|
        // |         <              |  ==  |       >   |   ???   |<=|

        // Positioned for pre-in/decrement to write to pivot region
        int p0 = pivot0;
        int p1 = pivot1;
        final double v = a[p0];
        if (a[left] < v) {
            // a[left] is not a sentinel
            final double w = a[left];
            if (a[right] > v) {
                // Most likely case: ends can be sentinels
                a[left] = a[right];
                a[right] = w;
            } else {
                // a[right] is a sentinel; use pivot for left
                a[left] = v;
                a[p0] = w;
                p0++;
            }
        } else if (a[right] > v) {
            // a[right] is not a sentinel; use pivot
            a[p1] = a[right];
            p1--;
            a[right] = v;
        }

        // Required to avoid index bound error first use of i/j
        assert left < start && end < right;
        int i = start;
        int j = end;
        while (true) {
            do {
                --i;
            } while (a[i] < v);
            do {
                ++j;
            } while (a[j] > v);
            final double vj = a[i];
            final double vi = a[j];
            a[i] = vi;
            a[j] = vj;
            // Move the equal values to pivot region
            if (vi == v) {
                a[i] = a[--p0];
                a[p0] = v;
            }
            if (vj == v) {
                a[j] = a[++p1];
                a[p1] = v;
            }
            // Termination check and finishing loops.
            // Note: this works even if pivot region is zero length (p1 == p0-1)
            // due to pivot use as a sentinel on one side because we pre-inc/decrement
            // one side and post-inc/decrement the other side.
            if (i == left) {
                while (j < right) {
                    do {
                        ++j;
                    } while (a[j] > v);
                    final double w = a[j];
                    // Move upper bound of pivot region
                    a[j] = a[++p1];
                    a[p1] = v;
                    if (w != v) {
                        // Move lower bound of pivot region
                        a[p0] = w;
                        p0++;
                    }
                }
                break;
            }
            if (j == right) {
                while (i > left) {
                    do {
                        --i;
                    } while (a[i] < v);
                    final double w = a[i];
                    // Move lower bound of pivot region
                    a[i] = a[--p0];
                    a[p0] = v;
                    if (w != v) {
                        // Move upper bound of pivot region
                        a[p1] = w;
                        p1--;
                    }
                }
                break;
            }
        }

        upper[0] = p1;
        return p0;
    }

    /**
     * Expand a partition around a single pivot. Partitioning exchanges array
     * elements such that all elements smaller than pivot are before it and all
     * elements larger than pivot are after it. The central region is already
     * partitioned.
     *
     * <pre>{@code
     * |l             |s   |p0 p1|   e|                r|
     * |    ???       | <P | ==P | >P |        ???      |
     * }</pre>
     *
     * <p>This is similar to {@link #expandPartitionT1(double[], int, int, int, int, int, int, int[])}
     * with a change to binary partitioning. It requires that {@code left < start && end < right}.
     * It supports {@code start == end}.
     *
     * @param a Data array.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param start Start of the partition range (inclusive).
     * @param end End of the partitioned range (inclusive).
     * @param pivot0 Lower pivot location (inclusive).
     * @param pivot1 Upper pivot location (inclusive).
     * @param upper Upper bound (inclusive) of the pivot range [k1].
     * @return Lower bound (inclusive) of the pivot range [k0].
     */
    private static int expandPartitionB1(double[] a, int left, int right, int start, int end,
        int pivot0, int pivot1, int[] upper) {
        // 2-way partition of the data using a pivot value into
        // less-than, or greater-than.
        //
        // Move sentinels from start and end to left and right. Scan towards the
        // sentinels until >=,<= then swap.
        //           <-i                           j->
        // |l |        |              | p|         |             | r|
        // |>=|   ???  |     <        |==|     >   |     ???     |<=|
        //
        // When either i or j reach the edge perform finishing loop.
        // Finish loop for a[j] <= v replaces j with p1+1, moves value to p
        // and moves the pivot up:
        //                                            j->
        // |l                         | p|            |         | r|
        // |         <                |==|        >   |   ???   |<=|

        // Pivot may be moved to use as a sentinel
        int p = pivot0;
        final double v = a[p];
        if (a[left] < v) {
            // a[left] is not a sentinel
            final double w = a[left];
            if (a[right] > v) {
                // Most likely case: ends can be sentinels
                a[left] = a[right];
                a[right] = w;
            } else {
                // a[right] is a sentinel; use pivot for left
                a[left] = v;
                a[p] = w;
                p++;
            }
        } else if (a[right] > v) {
            // a[right] is not a sentinel; use pivot
            a[p] = a[right];
            p--;
            a[right] = v;
        }

        // Required to avoid index bound error first use of i/j
        assert left < start && end < right;
        int i = start;
        int j = end;
        while (true) {
            do {
                --i;
            } while (a[i] < v);
            do {
                ++j;
            } while (a[j] > v);
            final double vj = a[i];
            final double vi = a[j];
            a[i] = vi;
            a[j] = vj;
            // Termination check and finishing loops.
            // These reset the pivot if it was moved then slide it as required.
            if (i == left) {
                // Reset the pivot and sentinel
                if (p < pivot0) {
                    // Pivot is in right; a[p] <= v
                    a[right] = a[p];
                    a[p] = v;
                } else if (p > pivot0) {
                    // Pivot was in left (now swapped to j); a[p] >= v
                    a[j] = a[p];
                    a[p] = v;
                }
                if (j == right) {
                    break;
                }
                while (j < right) {
                    do {
                        ++j;
                    } while (a[j] > v);
                    // Move pivot
                    a[p] = a[j];
                    a[j] = a[++p];
                    a[p] = v;
                }
                break;
            }
            if (j == right) {
                // Reset the pivot and sentinel
                if (p < pivot0) {
                    // Pivot was in right (now swapped to i); a[p] <= v
                    a[i] = a[p];
                    a[p] = v;
                } else if (p > pivot0) {
                    // Pivot is in left; a[p] >= v
                    a[left] = a[p];
                    a[p] = v;
                }
                if (i == left) {
                    break;
                }
                while (i > left) {
                    do {
                        --i;
                    } while (a[i] < v);
                    // Move pivot
                    a[p] = a[i];
                    a[i] = a[--p];
                    a[p] = v;
                }
                break;
            }
        }

        upper[0] = p;
        return p;
    }

    /**
     * Expand a partition around a single pivot. Partitioning exchanges array
     * elements such that all elements smaller than pivot are before it and all
     * elements larger than pivot are after it. The central region is already
     * partitioned.
     *
     * <pre>{@code
     * |l             |s   |p0 p1|   e|                r|
     * |    ???       | <P | ==P | >P |        ???      |
     * }</pre>
     *
     * <p>This is similar to {@link #expandPartitionT1(double[], int, int, int, int, int, int, int[])}
     * with a change to how the end-point sentinels are created. It does not use the pivot
     * but uses values at start and end. This increases the length of the lower/upper ranges
     * by 1 for the main scan. It requires that {@code start != end}. However it handles
     * {@code left == start} and/or {@code end == right}.
     *
     * @param a Data array.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param start Start of the partition range (inclusive).
     * @param end End of the partitioned range (inclusive).
     * @param pivot0 Lower pivot location (inclusive).
     * @param pivot1 Upper pivot location (inclusive).
     * @param upper Upper bound (inclusive) of the pivot range [k1].
     * @return Lower bound (inclusive) of the pivot range [k0].
     */
    private static int expandPartitionT2(double[] a, int left, int right, int start, int end,
        int pivot0, int pivot1, int[] upper) {
        // 3-way partition of the data using a pivot value into
        // less-than, equal or greater-than.
        // Based on Sedgewick's Bentley-McIroy partitioning: always swap i<->j then
        // check for equal to the pivot and move again.
        //
        // Move sentinels from start and end to left and right. Scan towards the
        // sentinels until >=,<=. Swap then move == to the pivot region.
        //           <-i                           j->
        // |l |        |            |p0  p1|       |             | r|
        // |>=|   ???  |     <      |  ==  |   >   |     ???     |<=|
        //
        // When either i or j reach the edge perform finishing loop.
        // Finish loop for a[j] <= v replaces j with p1+1, optionally moves value
        // to p0 for < and updates the pivot range p1 (and optionally p0):
        //                                             j->
        // |l                       |p0  p1|           |         | r|
        // |         <              |  ==  |       >   |   ???   |<=|

        final double v = a[pivot0];
        // Use start/end as sentinels.
        // This requires start != end
        assert start != end;
        double vi = a[start];
        double vj = a[end];
        a[start] = a[left];
        a[end] = a[right];
        a[left] = vj;
        a[right] = vi;

        int i = start + 1;
        int j = end - 1;

        // Positioned for pre-in/decrement to write to pivot region
        int p0 = pivot0 == start ? i : pivot0;
        int p1 = pivot1 == end ? j : pivot1;

        while (true) {
            do {
                --i;
            } while (a[i] < v);
            do {
                ++j;
            } while (a[j] > v);
            vj = a[i];
            vi = a[j];
            a[i] = vi;
            a[j] = vj;
            // Move the equal values to pivot region
            if (vi == v) {
                a[i] = a[--p0];
                a[p0] = v;
            }
            if (vj == v) {
                a[j] = a[++p1];
                a[p1] = v;
            }
            // Termination check and finishing loops.
            // Note: this works even if pivot region is zero length (p1 == p0-1
            // due to single length pivot region at either start/end) because we
            // pre-inc/decrement one side and post-inc/decrement the other side.
            if (i == left) {
                while (j < right) {
                    do {
                        ++j;
                    } while (a[j] > v);
                    final double w = a[j];
                    // Move upper bound of pivot region
                    a[j] = a[++p1];
                    a[p1] = v;
                    // Move lower bound of pivot region
                    //p0 += w != v ? 1 : 0;
                    if (w != v) {
                        a[p0] = w;
                        p0++;
                    }
                }
                break;
            }
            if (j == right) {
                while (i > left) {
                    do {
                        --i;
                    } while (a[i] < v);
                    final double w = a[i];
                    // Move lower bound of pivot region
                    a[i] = a[--p0];
                    a[p0] = v;
                    // Move upper bound of pivot region
                    //p1 -= w != v ? 1 : 0;
                    if (w != v) {
                        a[p1] = w;
                        p1--;
                    }
                }
                break;
            }
        }

        upper[0] = p1;
        return p0;
    }

    /**
     * Expand a partition around a single pivot. Partitioning exchanges array
     * elements such that all elements smaller than pivot are before it and all
     * elements larger than pivot are after it. The central region is already
     * partitioned.
     *
     * <pre>{@code
     * |l             |s   |p0 p1|   e|                r|
     * |    ???       | <P | ==P | >P |        ???      |
     * }</pre>
     *
     * <p>This is similar to {@link #expandPartitionT2(double[], int, int, int, int, int, int, int[])}
     * with a change to binary partitioning. It is simpler than
     * {@link #expandPartitionB1(double[], int, int, int, int, int, int, int[])} as the pivot is
     * not moved. It requires that {@code start != end}. However it handles
     * {@code left == start} and/or {@code end == right}.
     *
     * @param a Data array.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param start Start of the partition range (inclusive).
     * @param end End of the partitioned range (inclusive).
     * @param pivot0 Lower pivot location (inclusive).
     * @param pivot1 Upper pivot location (inclusive).
     * @param upper Upper bound (inclusive) of the pivot range [k1].
     * @return Lower bound (inclusive) of the pivot range [k0].
     */
    private static int expandPartitionB2(double[] a, int left, int right, int start, int end,
        int pivot0, int pivot1, int[] upper) {
        // 2-way partition of the data using a pivot value into
        // less-than, or greater-than.
        //
        // Move sentinels from start and end to left and right. Scan towards the
        // sentinels until >=,<= then swap.
        //           <-i                           j->
        // |l |        |              | p|         |             | r|
        // |>=|   ???  |     <        |==|     >   |     ???     |<=|
        //
        // When either i or j reach the edge perform finishing loop.
        // Finish loop for a[j] <= v replaces j with p1+1, moves value to p
        // and moves the pivot up:
        //                                            j->
        // |l                         | p|            |         | r|
        // |         <                |==|        >   |   ???   |<=|

        // Pivot
        int p = pivot0;
        final double v = a[p];
        // Use start/end as sentinels.
        // This requires start != end
        assert start != end;
        // Note: Must not move pivot as this invalidates the finishing loops.
        // See logic in method B1 to see added complexity of pivot location.
        // This method is not better than T2 for data with no repeat elements
        // and is slower for repeat elements when used with the improved
        // versions (e.g. linearBFPRTImproved). So for this edge case just use B1.
        if (p == start || p == end) {
            return expandPartitionB1(a, left, right, start, end, pivot0, pivot1, upper);
        }
        double vi = a[start];
        double vj = a[end];
        a[start] = a[left];
        a[end] = a[right];
        a[left] = vj;
        a[right] = vi;

        int i = start + 1;
        int j = end - 1;
        while (true) {
            do {
                --i;
            } while (a[i] < v);
            do {
                ++j;
            } while (a[j] > v);
            vj = a[i];
            vi = a[j];
            a[i] = vi;
            a[j] = vj;
            // Termination check and finishing loops
            if (i == left) {
                while (j < right) {
                    do {
                        ++j;
                    } while (a[j] > v);
                    // Move pivot
                    a[p] = a[j];
                    a[j] = a[++p];
                    a[p] = v;
                }
                break;
            }
            if (j == right) {
                while (i > left) {
                    do {
                        --i;
                    } while (a[i] < v);
                    // Move pivot
                    a[p] = a[i];
                    a[i] = a[--p];
                    a[p] = v;
                }
                break;
            }
        }

        upper[0] = p;
        return p;
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>The index {@code k} is the target element. This method ignores this value.
     * The value is included to match the method signature of the {@link SPEPartition} interface.
     * Assumes the range {@code r - l >= 4}; the caller is responsible for selection on a smaller
     * range.
     *
     * <p>Uses the Blum, Floyd, Pratt, Rivest, and Tarjan (BFPRT) median-of-medians algorithm
     * with medians of 5 with the sample medians computed in the first quintile.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private int linearBFPRTBaseline(double[] a, int l, int r, int k, int[] upper) {
        // Adapted from Alexandrescu (2016), algorithm 3.
        // Moves the responsibility for selection when r-l <= 4 to the caller.
        // Compute the median of each contiguous set of 5 to the first quintile.
        int rr = l - 1;
        for (int e = l + 4; e <= r; e += 5) {
            Sorting.median5d(a, e - 4, e - 3, e - 2, e - 1, e);
            // Median to first quintile
            final double v = a[e - 2];
            a[e - 2] = a[++rr];
            a[rr] = v;
        }
        final int m = (l + rr + 1) >>> 1;
        // mutual recursion
        quickSelect(this::linearBFPRTBaseline, a, l, rr, m, m, upper);
        // Note: repartions already partitioned data [l, rr]
        return spFunction.partition(a, l, r, m, upper);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>The index {@code k} is the target element. This method ignores this value.
     * The value is included to match the method signature of the {@link SPEPartition} interface.
     * Assumes the range {@code r - l >= 8}; the caller is responsible for selection on a smaller
     * range.
     *
     * <p>Uses the Chen and Dumitrescu repeated step median-of-medians-of-medians algorithm
     * with medians of 3 with the samples computed in the first tertile and 9th-tile.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private int linearRepeatedStepBaseline(double[] a, int l, int r, int k, int[] upper) {
        // Adapted from Alexandrescu (2016), algorithm 5.
        // Moves the responsibility for selection when r-l <= 8 to the caller.
        // Compute the median of each contiguous set of 3 to the first tertile, and repeat.
        int j = l - 1;
        for (int e = l + 2; e <= r; e += 3) {
            Sorting.sort3(a, e - 2, e - 1, e);
            // Median to first tertile
            final double v = a[e - 1];
            a[e - 1] = a[++j];
            a[j] = v;
        }
        int rr = l - 1;
        for (int e = l + 2; e <= j; e += 3) {
            Sorting.sort3(a, e - 2, e - 1, e);
            // Median to first 9th-tile
            final double v = a[e - 1];
            a[e - 1] = a[++rr];
            a[rr] = v;
        }
        final int m = (l + rr + 1) >>> 1;
        // mutual recursion
        quickSelect(this::linearRepeatedStepBaseline, a, l, rr, m, m, upper);
        // Note: repartions already partitioned data [l, rr]
        return spFunction.partition(a, l, r, m, upper);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>The index {@code k} is the target element. This method ignores this value.
     * The value is included to match the method signature of the {@link SPEPartition} interface.
     * Assumes the range {@code r - l >= 4}; the caller is responsible for selection on a smaller
     * range.
     *
     * <p>Uses the Blum, Floyd, Pratt, Rivest, and Tarjan (BFPRT) median-of-medians algorithm
     * with medians of 5 with the sample medians computed in the central quintile.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private int linearBFPRTImproved(double[] a, int l, int r, int k, int[] upper) {
        // Adapted from Alexandrescu (2016), algorithm 6.
        // Moves the responsibility for selection when r-l <= 4 to the caller.
        // Compute the median of each non-contiguous set of 5 to the middle quintile.
        final int f = (r - l + 1) / 5;
        final int f3 = 3 * f;
        // middle quintile: [2f:3f)
        final int s = l + (f << 1);
        final int e = s + f - 1;
        for (int i = l, j = s; i < s; i += 2, j++) {
            Sorting.median5d(a, i, i + 1, j, f3 + i, f3 + i + 1);
        }
        // Adaption to target kf/|A|
        //final int p = s + mapDistance(k - l, l, r, f);
        final int p = s + noSamplingAdapt.mapDistance(k - l, l, r, f);
        // mutual recursion
        quickSelect(this::linearBFPRTImproved, a, s, e, p, p, upper);
        return expandFunction.partition(a, l, r, s, e, upper[0], upper[1], upper);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>The index {@code k} is the target element. This method ignores this value.
     * The value is included to match the method signature of the {@link SPEPartition} interface.
     * Assumes the range {@code r - l >= 8}; the caller is responsible for selection on a smaller
     * range.
     *
     * <p>Uses the Chen and Dumitrescu repeated step median-of-medians-of-medians algorithm
     * with medians of 3 with the samples computed in the middle tertile and 9th-tile.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private int linearRepeatedStepImproved(double[] a, int l, int r, int k, int[] upper) {
        // Adapted from Alexandrescu (2016), algorithm 7.
        // Moves the responsibility for selection when r-l <= 8 to the caller.
        // Compute the median of each non-contiguous set of 3 to the middle tertile, and repeat.
        final int f = (r - l + 1) / 9;
        final int f3 = 3 * f;
        // i in middle tertile [3f:6f)
        for (int i = l + f3, e = l + (f3 << 1); i < e; i++) {
            Sorting.sort3(a, i - f3, i, i + f3);
        }
        // i in middle 9th-tile: [4f:5f)
        final int s = l + (f << 2);
        final int e = s + f - 1;
        for (int i = s; i <= e; i++) {
            Sorting.sort3(a, i - f, i, i + f);
        }
        // Adaption to target kf/|A|
        //final int p = s + mapDistance(k - l, l, r, f);
        final int p = s + noSamplingAdapt.mapDistance(k - l, l, r, f);
        // mutual recursion
        quickSelect(this::linearRepeatedStepImproved, a, s, e, p, p, upper);
        return expandFunction.partition(a, l, r, s, e, upper[0], upper[1], upper);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Assumes the range {@code r - l >= 8}; the caller is responsible for selection on a smaller
     * range. If using a 12th-tile for sampling then assumes {@code r - l >= 11}.
     *
     * <p>Uses the Chen and Dumitrescu repeated step median-of-medians-of-medians algorithm
     * with the median of 3 then median of 3; the final sample is placed in the
     * 5th 9th-tile; the pivot chosen from the sample is adaptive using the input {@code k}.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @param mode Adaption mode.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private int repeatedStep(double[] a, int l, int r, int k, int[] upper, AdaptMode mode) {
        // Adapted from Alexandrescu (2016), algorithm 8.
        // Moves the responsibility for selection when r-l <= 8 to the caller.
        int f;
        int s;
        int p;
        if (!mode.isSampleMode()) {
            // i in tertile [3f:6f)
            f = (r - l + 1) / 9;
            final int f3 = 3 * f;
            for (int i = l + f3, end = l + (f3 << 1); i < end; i++) {
                Sorting.sort3(a, i - f3, i, i + f3);
            }
            // 5th 9th-tile: [4f:5f)
            s = l + (f << 2);
            p = s + (mode.isAdapt() ? noSamplingAdapt.mapDistance(k - l, l, r, f) : (f >>> 1));
        } else {
            if ((controlFlags & FLAG_QA_MIDDLE_12) != 0) {
                // Switch to a 12th-tile as used in the other methods.
                f = (r - l + 1) / 12;
                // middle - f/2
                s = ((r + l) >>> 1) - (f >> 1);
            } else {
                f = (r - l + 1) / 9;
                s = l + (f << 2);
            }
            // Adaption to target kf'/|A|
            int kp = mode.isAdapt() ? samplingAdapt.mapDistance(k - l, l, r, f) : (f >>> 1);
            // Centre the sample at k
            if ((controlFlags & FLAG_QA_SAMPLE_K) != 0) {
                s = k - kp;
            }
            p = s + kp;
        }
        final int e = s + f - 1;
        for (int i = s; i <= e; i++) {
            Sorting.sort3(a, i - f, i, i + f);
        }
        p = quickSelectAdaptive(a, s, e, p, p, upper,
            (controlFlags & FLAG_QA_PROPAGATE) != 0 ? mode : adaptMode);
        return expandFunction.partition(a, l, r, s, e, p, upper[0], upper);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Assumes the range {@code r - l >= 11}; the caller is responsible for selection on a smaller
     * range.
     *
     * <p>Uses the Chen and Dumitrescu repeated step median-of-medians-of-medians algorithm
     * with the lower median of 4 then either median of 3 with the final sample placed in the
     * 5th 12th-tile, or min of 3 with the final sample in the 4th 12th-tile;
     * the pivot chosen from the sample is adaptive using the input {@code k}.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @param mode Adaption mode.
     * @param far Set to {@code true} to perform repeatedStepFarLeft.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private int repeatedStepLeft(double[] a, int l, int r, int k, int[] upper, AdaptMode mode,
        boolean far) {
        // Adapted from Alexandrescu (2016), algorithm 9 and 10.
        // Moves the responsibility for selection when r-l <= 11 to the caller.
        final int f = (r - l + 1) >> 2;
        if (!mode.isSampleMode()) {
            // i in 2nd quartile
            final int f2 = f + f;
            for (int i = l + f, e = l + f2; i < e; i++) {
                Sorting.lowerMedian4(a, i - f, i, i + f, i + f2);
            }
        }
        int fp = f / 3;
        int s;
        int e;
        int p;
        if (far) {
            // i in 4th 12th-tile
            s = l + f;
            // Variable adaption
            int kp;
            if (!mode.isSampleMode()) {
                kp = mode.isAdapt() ? noSamplingEdgeAdapt.mapDistance(k - l, l, r, fp) : fp >>> 1;
            } else {
                kp = mode.isAdapt() ? samplingEdgeAdapt.mapDistance(k - l, l, r, fp) : fp >>> 1;
                // Note: Not possible to centre the sample at k on the far step
            }
            e = s + fp - 1;
            p = s + kp;
            final int fp2 = fp << 1;
            for (int i = s; i <= e; i++) {
                // min into i
                if (a[i] > a[i + fp]) {
                    final double u = a[i];
                    a[i] = a[i + fp];
                    a[i + fp] = u;
                }
                if (a[i] > a[i + fp2]) {
                    final double v = a[i];
                    a[i] = a[i + fp2];
                    a[i + fp2] = v;
                }
            }
        } else {
            // i in 5th 12th-tile
            s = l + f + fp;
            // Variable adaption
            int kp;
            if (!mode.isSampleMode()) {
                kp = mode.isAdapt() ? noSamplingAdapt.mapDistance(k - l, l, r, fp) : fp >>> 1;
            } else {
                kp = mode.isAdapt() ? samplingAdapt.mapDistance(k - l, l, r, fp) : fp >>> 1;
                // Centre the sample at k
                if ((controlFlags & FLAG_QA_SAMPLE_K) != 0) {
                    // Avoid bounds error due to rounding as (k-l)/(r-l) -> 1/12
                    s = Math.max(k - kp, l + fp);
                }
            }
            e = s + fp - 1;
            p = s + kp;
            for (int i = s; i <= e; i++) {
                Sorting.sort3(a, i - fp, i, i + fp);
            }
        }
        p = quickSelectAdaptive(a, s, e, p, p, upper,
            (controlFlags & FLAG_QA_PROPAGATE) != 0 ? mode : adaptMode);
        return expandFunction.partition(a, l, r, s, e, p, upper[0], upper);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Assumes the range {@code r - l >= 11}; the caller is responsible for selection on a smaller
     * range.
     *
     * <p>Uses the Chen and Dumitrescu repeated step median-of-medians-of-medians algorithm
     * with the upper median of 4 then either median of 3 with the final sample placed in the
     * 8th 12th-tile, or max of 3 with the final sample in the 9th 12th-tile;
     * the pivot chosen from the sample is adaptive using the input {@code k}.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @param mode Adaption mode.
     * @param far Set to {@code true} to perform repeatedStepFarRight.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private int repeatedStepRight(double[] a, int l, int r, int k, int[] upper, AdaptMode mode,
        boolean far) {
        // Mirror image repeatedStepLeft using upper median into 3rd quartile
        final int f = (r - l + 1) >> 2;
        if (!mode.isSampleMode()) {
            // i in 3rd quartile
            final int f2 = f + f;
            for (int i = r - f, e = r - f2; i > e; i--) {
                Sorting.upperMedian4(a, i - f2, i - f, i, i + f);
            }
        }
        int fp = f / 3;
        int s;
        int e;
        int p;
        if (far) {
            // i in 9th 12th-tile
            e = r - f;
            // Variable adaption
            int kp;
            if (!mode.isSampleMode()) {
                kp = mode.isAdapt() ? noSamplingEdgeAdapt.mapDistance(r - k, l, r, fp) : fp >>> 1;
            } else {
                kp = mode.isAdapt() ? samplingEdgeAdapt.mapDistance(r - k, l, r, fp) : fp >>> 1;
                // Note: Not possible to centre the sample at k on the far step
            }
            s = e - fp + 1;
            p = e - kp;
            final int fp2 = fp << 1;
            for (int i = s; i <= e; i++) {
                // max into i
                if (a[i] < a[i - fp]) {
                    final double u = a[i];
                    a[i] = a[i - fp];
                    a[i - fp] = u;
                }
                if (a[i] < a[i - fp2]) {
                    final double v = a[i];
                    a[i] = a[i - fp2];
                    a[i - fp2] = v;
                }
            }
        } else {
            // i in 8th 12th-tile
            e = r - f - fp;
            // Variable adaption
            int kp;
            if (!mode.isSampleMode()) {
                kp = mode.isAdapt() ? noSamplingAdapt.mapDistance(r - k, l, r, fp) : fp >>> 1;
            } else {
                kp = mode.isAdapt() ? samplingAdapt.mapDistance(r - k, l, r, fp) : fp >>> 1;
                // Centre the sample at k
                if ((controlFlags & FLAG_QA_SAMPLE_K) != 0) {
                    // Avoid bounds error due to rounding as (r-k)/(r-l) -> 11/12
                    e = Math.min(k + kp, r - fp);
                }
            }
            s = e - fp + 1;
            p = e - kp;
            for (int i = s; i <= e; i++) {
                Sorting.sort3(a, i - fp, i, i + fp);
            }
        }
        p = quickSelectAdaptive(a, s, e, p, p, upper,
            (controlFlags & FLAG_QA_PROPAGATE) != 0 ? mode : adaptMode);
        return expandFunction.partition(a, l, r, s, e, p, upper[0], upper);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Assumes the range {@code r - l >= 11}; the caller is responsible for selection on a smaller
     * range.
     *
     * <p>Uses the Chen and Dumitrescu repeated step median-of-medians-of-medians algorithm
     * with the minimum of 4 then median of 3; the final sample is placed in the
     * 2nd 12th-tile; the pivot chosen from the sample is adaptive using the input {@code k}.
     *
     * <p>Given a pivot in the middle of the sample this has margins of 1/12 and 1/3.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @param mode Adaption mode.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private int repeatedStepFarLeft(double[] a, int l, int r, int k, int[] upper, AdaptMode mode) {
        // Moves the responsibility for selection when r-l <= 11 to the caller.
        final int f = (r - l + 1) >> 2;
        int fp = f / 3;
        // 2nd 12th-tile
        int s = l + fp;
        final int e = s + fp - 1;
        int p;
        if (!mode.isSampleMode()) {
            p = s + (mode.isAdapt() ? noSamplingEdgeAdapt.mapDistance(k - l, l, r, fp) : fp >>> 1);
            // i in 2nd quartile; min into i-f (1st quartile)
            final int f2 = f + f;
            for (int i = l + f, end = l + f2; i < end; i++) {
                if (a[i + f] < a[i - f]) {
                    final double u = a[i + f];
                    a[i + f] = a[i - f];
                    a[i - f] = u;
                }
                if (a[i + f2] < a[i]) {
                    final double v = a[i + f2];
                    a[i + f2] = a[i];
                    a[i] = v;
                }
                if (a[i] < a[i - f]) {
                    final double u = a[i];
                    a[i] = a[i - f];
                    a[i - f] = u;
                }
            }
        } else {
            int kp = mode.isAdapt() ? samplingEdgeAdapt.mapDistance(k - l, l, r, fp) : fp >>> 1;
            p = s + kp;
        }
        for (int i = s; i <= e; i++) {
            Sorting.sort3(a, i - fp, i, i + fp);
        }
        p = quickSelectAdaptive(a, s, e, p, p, upper,
            (controlFlags & FLAG_QA_PROPAGATE) != 0 ? mode : adaptMode);
        return expandFunction.partition(a, l, r, s, e, p, upper[0], upper);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Assumes the range {@code r - l >= 11}; the caller is responsible for selection on a smaller
     * range.
     *
     * <p>Uses the Chen and Dumitrescu repeated step median-of-medians-of-medians algorithm
     * with the maximum of 4 then median of 3; the final sample is placed in the
     * 11th 12th-tile; the pivot chosen from the sample is adaptive using the input {@code k}.
     *
     * <p>Given a pivot in the middle of the sample this has margins of 1/3 and 1/12.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @param mode Adaption mode.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private int repeatedStepFarRight(double[] a, int l, int r, int k, int[] upper, AdaptMode mode) {
        // Mirror image repeatedStepFarLeft
        final int f = (r - l + 1) >> 2;
        int fp = f / 3;
        // 11th 12th-tile
        int e = r - fp;
        final int s = e - fp + 1;
        int p;
        if (!mode.isSampleMode()) {
            p = e - (mode.isAdapt() ? noSamplingEdgeAdapt.mapDistance(r - k, l, r, fp) : fp >>> 1);
            // i in 3rd quartile; max into i+f (4th quartile)
            final int f2 = f + f;
            for (int i = r - f, end = r - f2; i > end; i--) {
                if (a[i - f] > a[i + f]) {
                    final double u = a[i - f];
                    a[i - f] = a[i + f];
                    a[i + f] = u;
                }
                if (a[i - f2] > a[i]) {
                    final double v = a[i - f2];
                    a[i - f2] = a[i];
                    a[i] = v;
                }
                if (a[i] > a[i + f]) {
                    final double u = a[i];
                    a[i] = a[i + f];
                    a[i + f] = u;
                }
            }
        } else {
            int kp = mode.isAdapt() ? samplingEdgeAdapt.mapDistance(r - k, l, r, fp) : fp >>> 1;
            p = e - kp;
        }
        for (int i = s; i <= e; i++) {
            Sorting.sort3(a, i - fp, i, i + fp);
        }
        p = quickSelectAdaptive(a, s, e, p, p, upper,
            (controlFlags & FLAG_QA_PROPAGATE) != 0 ? mode : adaptMode);
        return expandFunction.partition(a, l, r, s, e, p, upper[0], upper);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Partitions a Floyd-Rivest sample around a pivot offset so that the input {@code k} will
     * fall in the smaller partition when the entire range is partitioned.
     *
     * <p>Assumes the range {@code r - l} is large; the original Floyd-Rivest size for sampling
     * was 600.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @param mode Adaption mode.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private int sampleStep(double[] a, int l, int r, int k, int[] upper, AdaptMode mode) {
        // Floyd-Rivest: use SELECT recursively on a sample of size S to get an estimate
        // for the (k-l+1)-th smallest element into a[k], biased slightly so that the
        // (k-l+1)-th element is expected to lie in the smaller set after partitioning.
        final int n = r - l + 1;
        final int ith = k - l + 1;
        final double z = Math.log(n);
        // sample size = 0.5 * n^(2/3)
        final double s = 0.5 * Math.exp(0.6666666666666666 * z);
        final double sd = 0.5 * Math.sqrt(z * s * (n - s) / n) * Integer.signum(ith - (n >> 1));
        final int ll = Math.max(l, (int) (k - ith * s / n + sd));
        final int rr = Math.min(r, (int) (k + (n - ith) * s / n + sd));
        // Optional random sampling.
        // Support two variants.
        if ((controlFlags & FLAG_QA_RANDOM_SAMPLING) != 0) {
            final IntUnaryOperator rng = createRNG(n, k);
            if (ll == l) {
                // Shuffle [l, rr] from [l, r]
                for (int i = l - 1; i < rr;) {
                    // r - rand [0, r - i] : i is currently i-1
                    final int j = r - rng.applyAsInt(r - i);
                    final double t = a[++i];
                    a[i] = a[j];
                    a[j] = t;
                }
            } else if (rr == r) {
                // Shuffle [ll, r] from [l, r]
                for (int i = r + 1; i > ll;) {
                    // l + rand [0, i - l] : i is currently i+1
                    final int j = l + rng.applyAsInt(i - l);
                    final double t = a[--i];
                    a[i] = a[j];
                    a[j] = t;
                }
            } else {
                // Sample range [ll, rr] is internal
                // Shuffle [ll, k) from [l, k)
                for (int i = k; i > ll;) {
                    // l + rand [0, i - l + 1) : i is currently i+1
                    final int j = l + rng.applyAsInt(i - l);
                    final double t = a[--i];
                    a[i] = a[j];
                    a[j] = t;
                }
                // Shuffle (k, rr] from (k, r]
                for (int i = k; i < rr;) {
                    // r - rand [0, r - i + 1) : i is currently i-1
                    final int j = r - rng.applyAsInt(r - i);
                    final double t = a[++i];
                    a[i] = a[j];
                    a[j] = t;
                }
            }
        } else if ((controlFlags & FLAG_RANDOM_SAMPLING) != 0) {
            final IntUnaryOperator rng = createRNG(n, k);
            // Shuffle [ll, k) from [l, k)
            if (ll > l) {
                for (int i = k; i > ll;) {
                    // l + rand [0, i - l + 1) : i is currently i+1
                    final int j = l + rng.applyAsInt(i - l);
                    final double t = a[--i];
                    a[i] = a[j];
                    a[j] = t;
                }
            }
            // Shuffle (k, rr] from (k, r]
            if (rr < r) {
                for (int i = k; i < rr;) {
                    // r - rand [0, r - i + 1) : i is currently i-1
                    final int j = r - rng.applyAsInt(r - i);
                    final double t = a[++i];
                    a[i] = a[j];
                    a[j] = t;
                }
            }
        }
        // Sample recursion restarts from [ll, rr]
        final int p = quickSelectAdaptive(a, ll, rr, k, k, upper,
            (controlFlags & FLAG_QA_PROPAGATE) != 0 ? mode : adaptMode);

        // Expect a small sample and repartition the entire range...
        // Does not support a pivot range so use the centre
        //return spFunction.partition(a, l, r, (p + upper[0]) >>> 1, upper);

        return expandFunction.partition(a, l, r, ll, rr, p, upper[0], upper);
    }

    /**
     * Map the distance from the edge of {@code [l, r]} to a new distance in {@code [0, n)}.
     *
     * <p>The provides the adaption {@code kf'/|A|} from Alexandrescu (2016) where
     * {@code k == d}, {@code f' == n} and {@code |A| == r-l+1}.
     *
     * <p>For convenience this accepts the input range {@code [l, r]}.
     *
     * @param d Distance from the edge in {@code [0, r - l]}.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param n Size of the new range.
     * @return the mapped distance in [0, n)
     */
    private static int mapDistance(int d, int l, int r, int n) {
        return (int) (d * (n - 1.0) / (r - l));
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Assumes the range {@code r - l >= 8}; the caller is responsible for selection on a smaller
     * range. If using a 12th-tile for sampling then assumes {@code r - l >= 11}.
     *
     * <p>Uses the Chen and Dumitrescu repeated step median-of-medians-of-medians algorithm
     * with the median of 3 then median of 3; the final sample is placed in the
     * 5th 9th-tile; the pivot chosen from the sample is adaptive using the input {@code k}.
     *
     * <p>Given a pivot in the middle of the sample this has margins of 2/9 and 2/9.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @param flags Control flags.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private static int repeatedStep(double[] a, int l, int r, int k, int[] upper, int flags) {
        // Adapted from Alexandrescu (2016), algorithm 8.
        int fp;
        int s;
        int p;
        if (flags <= MODE_SAMPLING) {
            // Median into a 12th-tile
            fp = (r - l + 1) / 12;
            // Position the sample around the target k
            s = k - mapDistance(k - l, l, r, fp);
            p = k;
        } else {
            // i in tertile [3f':6f')
            fp = (r - l + 1) / 9;
            final int f3 = 3 * fp;
            for (int i = l + f3, end = l + (f3 << 1); i < end; i++) {
                Sorting.sort3(a, i - f3, i, i + f3);
            }
            // 5th 9th-tile: [4f':5f')
            s = l + (fp << 2);
            // No adaption uses the middle to enforce strict margins
            p = s + (flags == MODE_ADAPTION ? mapDistance(k - l, l, r, fp) : (fp >>> 1));
        }
        final int e = s + fp - 1;
        for (int i = s; i <= e; i++) {
            Sorting.sort3(a, i - fp, i, i + fp);
        }
        p = quickSelectAdaptive2(a, s, e, p, p, upper, qaMode);
        return expandPartitionT2(a, l, r, s, e, p, upper[0], upper);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Assumes the range {@code r - l >= 11}; the caller is responsible for selection on a smaller
     * range.
     *
     * <p>Uses the Chen and Dumitrescu repeated step median-of-medians-of-medians algorithm
     * with the lower median of 4 then either median of 3 with the final sample placed in the
     * 5th 12th-tile, or min of 3 with the final sample in the 4th 12th-tile;
     * the pivot chosen from the sample is adaptive using the input {@code k}.
     *
     * <p>Given a pivot in the middle of the sample this has margins of 1/6 and 1/4.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @param flags Control flags.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private static int repeatedStepLeft(double[] a, int l, int r, int k, int[] upper, int flags) {
        // Adapted from Alexandrescu (2016), algorithm 9.
        int fp;
        int s;
        int p;
        if (flags <= MODE_SAMPLING) {
            // Median into a 12th-tile
            fp = (r - l + 1) / 12;
            // Position the sample around the target k
            // Avoid bounds error due to rounding as (k-l)/(r-l) -> 1/12
            s = Math.max(k - mapDistance(k - l, l, r, fp), l + fp);
            p = k;
        } else {
            // i in 2nd quartile
            final int f = (r - l + 1) >> 2;
            final int f2 = f + f;
            for (int i = l + f, end = l + f2; i < end; i++) {
                Sorting.lowerMedian4(a, i - f, i, i + f, i + f2);
            }
            // i in 5th 12th-tile
            fp = f / 3;
            s = l + f + fp;
            // No adaption uses the middle to enforce strict margins
            p = s + (flags == MODE_ADAPTION ? mapDistance(k - l, l, r, fp) : (fp >>> 1));
        }
        final int e = s + fp - 1;
        for (int i = s; i <= e; i++) {
            Sorting.sort3(a, i - fp, i, i + fp);
        }
        p = quickSelectAdaptive2(a, s, e, p, p, upper, qaMode);
        return expandPartitionT2(a, l, r, s, e, p, upper[0], upper);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Assumes the range {@code r - l >= 11}; the caller is responsible for selection on a smaller
     * range.
     *
     * <p>Uses the Chen and Dumitrescu repeated step median-of-medians-of-medians algorithm
     * with the upper median of 4 then either median of 3 with the final sample placed in the
     * 8th 12th-tile, or max of 3 with the final sample in the 9th 12th-tile;
     * the pivot chosen from the sample is adaptive using the input {@code k}.
     *
     * <p>Given a pivot in the middle of the sample this has margins of 1/4 and 1/6.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @param flags Control flags.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private static int repeatedStepRight(double[] a, int l, int r, int k, int[] upper, int flags) {
        // Mirror image repeatedStepLeft using upper median into 3rd quartile
        int fp;
        int e;
        int p;
        if (flags <= MODE_SAMPLING) {
            // Median into a 12th-tile
            fp = (r - l + 1) / 12;
            // Position the sample around the target k
            // Avoid bounds error due to rounding as (r-k)/(r-l) -> 11/12
            e = Math.min(k + mapDistance(r - k, l, r, fp), r - fp);
            p = k;
        } else {
            // i in 3rd quartile
            final int f = (r - l + 1) >> 2;
            final int f2 = f + f;
            for (int i = r - f, end = r - f2; i > end; i--) {
                Sorting.upperMedian4(a, i - f2, i - f, i, i + f);
            }
            // i in 8th 12th-tile
            fp = f / 3;
            e = r - f - fp;
            // No adaption uses the middle to enforce strict margins
            p = e - (flags == MODE_ADAPTION ? mapDistance(r - k, l, r, fp) : (fp >>> 1));
        }
        final int s = e - fp + 1;
        for (int i = s; i <= e; i++) {
            Sorting.sort3(a, i - fp, i, i + fp);
        }
        p = quickSelectAdaptive2(a, s, e, p, p, upper, qaMode);
        return expandPartitionT2(a, l, r, s, e, p, upper[0], upper);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Assumes the range {@code r - l >= 11}; the caller is responsible for selection on a smaller
     * range.
     *
     * <p>Uses the Chen and Dumitrescu repeated step median-of-medians-of-medians algorithm
     * with the minimum of 4 then median of 3; the final sample is placed in the
     * 2nd 12th-tile; the pivot chosen from the sample is adaptive using the input {@code k}.
     *
     * <p>Given a pivot in the middle of the sample this has margins of 1/12 and 1/3.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @param flags Control flags.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private static int repeatedStepFarLeft(double[] a, int l, int r, int k, int[] upper, int flags) {
        // Far step has been changed from the Alexandrescu (2016) step of lower-median-of-4, min-of-3
        // into the 4th 12th-tile to a min-of-4, median-of-3 into the 2nd 12th-tile.
        // The differences are:
        // - The upper margin when not sampling is 8/24 vs. 9/24; the lower margin remains at 1/12.
        // - The position of the sample is closer to the expected location of k < |A| / 12.
        // - Sampling mode uses a median-of-3 with adaptive k, matching the other step methods.
        //   A min-of-3 sample can create a pivot too small if used with adaption of k leaving
        //   k in the larger partition and a wasted iteration.
        // - Adaption is adjusted to force use of the lower margin when not sampling.
        int fp;
        int s;
        int p;
        if (flags <= MODE_SAMPLING) {
            // 2nd 12th-tile
            fp = (r - l + 1) / 12;
            s = l + fp;
            // Use adaption
            p = s + mapDistance(k - l, l, r, fp);
        } else {
            // i in 2nd quartile; min into i-f (1st quartile)
            final int f = (r - l + 1) >> 2;
            final int f2 = f + f;
            for (int i = l + f, end = l + f2; i < end; i++) {
                if (a[i + f] < a[i - f]) {
                    final double u = a[i + f];
                    a[i + f] = a[i - f];
                    a[i - f] = u;
                }
                if (a[i + f2] < a[i]) {
                    final double v = a[i + f2];
                    a[i + f2] = a[i];
                    a[i] = v;
                }
                if (a[i] < a[i - f]) {
                    final double u = a[i];
                    a[i] = a[i - f];
                    a[i - f] = u;
                }
            }
            // 2nd 12th-tile
            fp = f / 3;
            s = l + fp;
            // Lower margin has 2(d+1) elements; d == (position in sample) - s
            // Force k into the lower margin
            p = s + ((k - l) >>> 1);
        }
        final int e = s + fp - 1;
        for (int i = s; i <= e; i++) {
            Sorting.sort3(a, i - fp, i, i + fp);
        }
        p = quickSelectAdaptive2(a, s, e, p, p, upper, qaMode);
        return expandPartitionT2(a, l, r, s, e, p, upper[0], upper);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Assumes the range {@code r - l >= 11}; the caller is responsible for selection on a smaller
     * range.
     *
     * <p>Uses the Chen and Dumitrescu repeated step median-of-medians-of-medians algorithm
     * with the maximum of 4 then median of 3; the final sample is placed in the
     * 11th 12th-tile; the pivot chosen from the sample is adaptive using the input {@code k}.
     *
     * <p>Given a pivot in the middle of the sample this has margins of 1/3 and 1/12.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @param flags Control flags.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private static int repeatedStepFarRight(double[] a, int l, int r, int k, int[] upper, int flags) {
        // Mirror image repeatedStepFarLeft
        int fp;
        int e;
        int p;
        if (flags <= MODE_SAMPLING) {
            // 11th 12th-tile
            fp = (r - l + 1) / 12;
            e = r - fp;
            // Use adaption
            p = e - mapDistance(r - k, l, r, fp);
        } else {
            // i in 3rd quartile; max into i+f (4th quartile)
            final int f = (r - l + 1) >> 2;
            final int f2 = f + f;
            for (int i = r - f, end = r - f2; i > end; i--) {
                if (a[i - f] > a[i + f]) {
                    final double u = a[i - f];
                    a[i - f] = a[i + f];
                    a[i + f] = u;
                }
                if (a[i - f2] > a[i]) {
                    final double v = a[i - f2];
                    a[i - f2] = a[i];
                    a[i] = v;
                }
                if (a[i] > a[i + f]) {
                    final double u = a[i];
                    a[i] = a[i + f];
                    a[i + f] = u;
                }
            }
            // 11th 12th-tile
            fp = f / 3;
            e = r - fp;
            // Upper margin has 2(d+1) elements; d == e - (position in sample)
            // Force k into the upper margin
            p = e - ((r - k) >>> 1);
        }
        final int s = e - fp + 1;
        for (int i = s; i <= e; i++) {
            Sorting.sort3(a, i - fp, i, i + fp);
        }
        p = quickSelectAdaptive2(a, s, e, p, p, upper, qaMode);
        return expandPartitionT2(a, l, r, s, e, p, upper[0], upper);
    }

    /**
     * Partition an array slice around a pivot. Partitioning exchanges array elements such
     * that all elements smaller than pivot are before it and all elements larger than
     * pivot are after it.
     *
     * <p>Partitions a Floyd-Rivest sample around a pivot offset so that the input {@code k} will
     * fall in the smaller partition when the entire range is partitioned.
     *
     * <p>Assumes the range {@code r - l} is large.
     *
     * @param a Data array.
     * @param l Lower bound (inclusive).
     * @param r Upper bound (inclusive).
     * @param k Target index.
     * @param upper Upper bound (inclusive) of the pivot range.
     * @param flags Control flags.
     * @return Lower bound (inclusive) of the pivot range.
     */
    private static int sampleStep(double[] a, int l, int r, int k, int[] upper, int flags) {
        // Floyd-Rivest: use SELECT recursively on a sample of size S to get an estimate
        // for the (k-l+1)-th smallest element into a[k], biased slightly so that the
        // (k-l+1)-th element is expected to lie in the smaller set after partitioning.
        final int n = r - l + 1;
        final int ith = k - l + 1;
        final double z = Math.log(n);
        // sample size = 0.5 * n^(2/3)
        final double s = 0.5 * Math.exp(0.6666666666666666 * z);
        final double sd = 0.5 * Math.sqrt(z * s * (n - s) / n) * Integer.signum(ith - (n >> 1));
        final int ll = Math.max(l, (int) (k - ith * s / n + sd));
        final int rr = Math.min(r, (int) (k + (n - ith) * s / n + sd));
        // Note: Random sampling is not supported.
        // Sample recursion restarts from [ll, rr]
        final int p = quickSelectAdaptive2(a, ll, rr, k, k, upper, qaMode);
        return expandPartitionT2(a, l, r, ll, rr, p, upper[0], upper);
    }

    /**
     * Move NaN values to the end of the array.
     * This allows all other values to be compared using {@code <, ==, >} operators (with
     * the exception of signed zeros).
     *
     * @param data Values.
     * @return index of last non-NaN value (or -1)
     */
    static int sortNaN(double[] data) {
        int end = data.length;
        // Find first non-NaN
        while (--end >= 0) {
            if (!Double.isNaN(data[end])) {
                break;
            }
        }
        for (int i = end; --i >= 0;) {
            final double v = data[i];
            if (Double.isNaN(v)) {
                // swap(data, i, end--)
                data[i] = data[end];
                data[end] = v;
                end--;
            }
        }
        return end;
    }

    /**
     * Move invalid indices to the end of the array.
     *
     * @param indices Values.
     * @param right Upper bound of data (inclusive).
     * @param count Count of indices.
     * @return count of valid indices
     */
    static int countIndices(int[] indices, int count, int right) {
        int end = count;
        // Find first valid index
        while (--end >= 0) {
            if (indices[end] <= right) {
                break;
            }
        }
        for (int i = end; --i >= 0;) {
            final int k = indices[i];
            if (k > right) {
                // swap(indices, i, end--)
                indices[i] = indices[end];
                indices[end] = k;
                end--;
            }
        }
        return end + 1;
    }

    /**
     * Count the number of signed zeros (-0.0) if the range contains a mix of positive and
     * negative zeros. If all positive, or all negative then this returns 0.
     *
     * <p>This method can be used when a pivot value is zero during partitioning when the
     * method uses the pivot value to replace values matched as equal using {@code ==}.
     * This may destroy a mixture of signed zeros by overwriting them as all 0.0 or -0.0
     * depending on the pivot value.
     *
     * @param data Values.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @return the count of signed zeros if some positive zeros are also present
     */
    static int countMixedSignedZeros(double[] data, int left, int right) {
        // Count negative zeros
        int c = 0;
        int cn = 0;
        for (int i = left; i <= right; i++) {
            if (data[i] == 0) {
                c++;
                if (Double.doubleToRawLongBits(data[i]) < 0) {
                    cn++;
                }
            }
        }
        return c == cn ? 0 : cn;
    }

    /**
     * Sort a range of all zero values.
     * This orders -0.0 before 0.0.
     *
     * <p>Warning: The range must contain only zeros.
     *
     * @param data Values.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     */
    static void sortZero(double[] data, int left, int right) {
        // Count negative zeros
        int c = 0;
        for (int i = left; i <= right; i++) {
            if (Double.doubleToRawLongBits(data[i]) < 0) {
                c++;
            }
        }
        // Replace
        if (c != 0) {
            int i = left;
            while (c-- > 0) {
                data[i++] = -0.0;
            }
            while (i <= right) {
                data[i++] = 0.0;
            }
        }
    }

    /**
     * Detect and fix the sort order of signed zeros. Assumes the data may have been
     * partially ordered around zero.
     *
     * <p>Searches for zeros if {@code data[left] <= 0} and {@code data[right] >= 0}.
     * If zeros are discovered in the range then they are assumed to be continuous.
     *
     * @param data Values.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     */
    private static void fixContinuousSignedZeros(double[] data, int left, int right) {
        int j;
        if (data[left] <= 0 && data[right] >= 0) {
            int i = left;
            while (data[i] < 0) {
                i++;
            }
            j = right;
            while (data[j] > 0) {
                j--;
            }
            sortZero(data, i, j);
        }
    }

    /**
     * Creates the maximum recursion depth for single-pivot quickselect recursion.
     *
     * <p>Warning: A length of zero will create a negative recursion depth.
     * In practice this does not matter as the sort / partition of a length
     * zero array should ignore the data.
     *
     * @param n Length of data (must be strictly positive).
     * @return the maximum recursion depth
     */
    private int createMaxDepthSinglePivot(int n) {
        // Ideal single pivot recursion will take log2(n) steps as data is
        // divided into length (n/2) at each iteration.
        final int maxDepth = floorLog2(n);
        // This factor should be tuned for practical performance
        return (int) Math.floor(maxDepth * recursionMultiple) + recursionConstant;
    }

    /**
     * Compute the maximum recursion depth for single pivot recursion.
     * Uses {@code 2 * floor(log2 (x))}.
     *
     * @param x Value.
     * @return {@code log3(x))}
     */
    private static int singlePivotMaxDepth(int x) {
        return (31 - Integer.numberOfLeadingZeros(x)) << 1;
    }

    /**
     * Compute {@code floor(log 2 (x))}. This is valid for all strictly positive {@code x}.
     *
     * <p>Returns -1 for {@code x = 0} in place of -infinity.
     *
     * @param x Value.
     * @return {@code floor(log 2 (x))}
     */
    static int floorLog2(int x) {
        return 31 - Integer.numberOfLeadingZeros(x);
    }

    /**
     * Convert {@code ln(n)} to the single-pivot max depth.
     *
     * @param x ln(n)
     * @return the maximum recursion depth
     */
    private int lnNtoMaxDepthSinglePivot(double x) {
        final double maxDepth = x * LOG2_E;
        return (int) Math.floor(maxDepth * recursionMultiple) + recursionConstant;
    }

    /**
     * Creates the maximum recursion depth for dual-pivot quickselect recursion.
     *
     * <p>Warning: A length of zero will create a high recursion depth.
     * In practice this does not matter as the sort / partition of a length
     * zero array should ignore the data.
     *
     * @param n Length of data (must be strictly positive).
     * @return the maximum recursion depth
     */
    private int createMaxDepthDualPivot(int n) {
        // Ideal dual pivot recursion will take log3(n) steps as data is
        // divided into length (n/3) at each iteration.
        final int maxDepth = log3(n);
        // This factor should be tuned for practical performance
        return (int) Math.floor(maxDepth * recursionMultiple) + recursionConstant;
    }

    /**
     * Compute an approximation to {@code log3 (x)}.
     *
     * <p>The result is between {@code floor(log3(x))} and {@code ceil(log3(x))}.
     * The result is correctly rounded when {@code x +/- 1} is a power of 3.
     *
     * @param x Value.
     * @return {@code log3(x))}
     */
    static int log3(int x) {
        // log3(2) ~ 1.5849625
        // log3(x) ~ log2(x) * 0.630929753... ~ log2(x) * 323 / 512 (0.630859375)
        // Use (floor(log2(x))+1) * 323 / 512
        // This result is always between floor(log3(x)) and ceil(log3(x)).
        // It is correctly rounded when x +/- 1 is a power of 3.
        return ((32 - Integer.numberOfLeadingZeros(x)) * 323) >>> 9;
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
     * <li>use of an inclusive upper bound;
     * <li>returning the closest index with a value below {@code key} if no match was not found;
     * <li>performing no range checks: it is assumed {@code left <= right} and they are valid
     * indices into the array.
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

            // Full binary search
            // Run time is up to log2(n) (fast exit on a match) but has more comparisons
            if (v < k) {
                l = m + 1;
            } else if (v > k) {
                r = m - 1;
            } else {
                // Equal
                return m;
            }

            // Modified search that does not expect a match
            // Run time is log2(n). Benchmarks as the same speed.
            //if (v > k) {
            //    r = m - 1;
            //} else {
            //    l = m + 1;
            //}
        }
        // Return largest known value below:
        // r is always moved downward when a middle index value is too high
        return r;
    }

    /**
     * Search the data for the smallest index {@code i} where {@code a[i]} is
     * greater-than-or-equal to the {@code key}; else return {@code right + 1}.
     * <pre>
     * a[i] >= k      :   left <= i <= right, or (right + 1)
     * </pre>
     *
     * <p>The data is assumed to be in ascending order, otherwise the behaviour is undefined.
     * If the range contains multiple elements with the {@code key} value, the result index
     * may be any that match.
     *
     * <p>This is similar to using {@link java.util.Arrays#binarySearch(int[], int, int, int)
     * Arrays.binarySearch}. The method differs in:
     * <ul>
     * <li>use of an inclusive upper bound;
     * <li>returning the closest index with a value above {@code key} if no match was not found;
     * <li>performing no range checks: it is assumed {@code left <= right} and they are valid
     * indices into the array.
     * </ul>
     *
     * <p>An equivalent use of binary search is:
     * <pre>{@code
     * int i = Arrays.binarySearch(a, left, right + 1, k);
     * if (i < 0) {
     *     i = ~i;
     * }
     * }</pre>
     *
     * <p>This specialisation avoids the caller checking the binary search result for the use
     * case when the presence or absence of a key is not important; only that the returned
     * index for an absence of a key is the smallest index. When used on unique keys this
     * method can be used to update a lower index so all keys are known to be above a key:
     *
     * <pre>{@code
     * int[] keys = ...
     * // [i0, i1] contains all keys
     * int i0 = 0;
     * int i1 = keys.length - 1;
     * // Update: [i0, i1] contains all keys >= k
     * i0 = searchGreaterOrEqual(keys, i0, i1, k);
     * }</pre>
     *
     * @param a Data.
     * @param left Lower bound (inclusive).
     * @param right Upper bound (inclusive).
     * @param k Key.
     * @return largest index {@code i} such that {@code a[i] >= k}, or {@code right + 1} if no
     * such index exists
     */
    static int searchGreaterOrEqual(int[] a, int left, int right, int k) {
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

            // Full binary search
            // Run time is up to log2(n) (fast exit on a match) but has more comparisons
            if (v < k) {
                l = m + 1;
            } else if (v > k) {
                r = m - 1;
            } else {
                // Equal
                return m;
            }

            // Modified search that does not expect a match
            // Run time is log2(n). Benchmarks as the same speed.
            //if (v < k) {
            //    l = m + 1;
            //} else {
            //    r = m - 1;
            //}
        }
        // Smallest known value above
        // l is always moved upward when a middle index value is too low
        return l;
    }

    /**
     * Creates the source of random numbers in {@code [0, n)}.
     * This is configurable via the control flags.
     *
     * @param n Data length.
     * @param k Target index.
     * @return the RNG
     */
    private IntUnaryOperator createRNG(int n, int k) {
        // Configurable
        if ((controlFlags & FLAG_MSWS) != 0) {
            // Middle-Square Weyl Sequence is fastest int generator
            final UniformRandomProvider rng = RandomSource.MSWS.create(n * 31L + k);
            if ((controlFlags & FLAG_BIASED_RANDOM) != 0) {
                // result = i * [0, 2^32) / 2^32
                return i -> (int) ((i * Integer.toUnsignedLong(rng.nextInt())) >>> Integer.SIZE);
            }
            return rng::nextInt;
        }
        if ((controlFlags & FLAG_SPLITTABLE_RANDOM) != 0) {
            final SplittableRandom rng = new SplittableRandom(n * 31L + k);
            if ((controlFlags & FLAG_BIASED_RANDOM) != 0) {
                // result = i * [0, 2^32) / 2^32
                return i -> (int) ((i * Integer.toUnsignedLong(rng.nextInt())) >>> Integer.SIZE);
            }
            return rng::nextInt;
        }
        return createFastRNG(n, k);
    }

    /**
     * Creates the source of random numbers in {@code [0, n)}.
     *
     * <p>This uses a RNG based on a linear congruential generator with biased numbers
     * in {@code [0, n)}, favouring speed over statitsical robustness.
     *
     * @param n Data length.
     * @param k Target index.
     * @return the RNG
     */
    static IntUnaryOperator createFastRNG(int n, int k) {
        return new Gen(n * 31L + k);
    }

    /**
     * Random generator for numbers in {@code [0, n)}.
     * The random sample should be fast in preference to statistically robust.
     * Here we implement a biased sampler for the range [0, n)
     * as n * f with f a fraction with base 2^32.
     * Source of randomness is a 64-bit LCG using the constants from MMIX by Donald Knuth.
     * https://en.wikipedia.org/wiki/Linear_congruential_generator
     */
    private static final class Gen implements IntUnaryOperator {
        /** LCG state. */
        private long s;

        /**
         * @param seed Seed.
         */
        Gen(long seed) {
            // Update state
            this.s = seed * 6364136223846793005L + 1442695040888963407L;
        }

        @Override
        public int applyAsInt(int n) {
            final long x = s;
            // Update state
            s = s * 6364136223846793005L + 1442695040888963407L;
            // Use the upper 32-bits from the state as the random 32-bit sample
            // result = n * [0, 2^32) / 2^32
            return (int) ((n * (x >>> Integer.SIZE)) >>> Integer.SIZE);
        }
    }
}
