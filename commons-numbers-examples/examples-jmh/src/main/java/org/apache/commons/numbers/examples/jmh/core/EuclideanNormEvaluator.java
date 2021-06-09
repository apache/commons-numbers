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
package org.apache.commons.numbers.examples.jmh.core;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * Class used to evaluate the accuracy of different norm computation
 * methods.
 */
public class EuclideanNormEvaluator {

    /** Map of names to norm computation methods. */
    private final Map<String, ToDoubleFunction<double[]>> methods = new LinkedHashMap<>();

    /** Add a computation method to be evaluated.
     * @param name method name
     * @param method computation method
     * @return this instance
     */
    public EuclideanNormEvaluator addMethod(final String name, final ToDoubleFunction<double[]> method) {
        methods.put(name, method);
        return this;
    }

    /** Evaluate the configured computation methods against the given array of input vectors.
     * @param inputs array of input vectors
     * @return map of evaluation results keyed by method name
     */
    public Map<String, Stats> evaluate(final double[][] inputs) {

        final Map<String, StatsAccumulator> accumulators = new HashMap<>();
        for (final String name : methods.keySet()) {
            accumulators.put(name, new StatsAccumulator(inputs.length * 2));
        }

        for (int i = 0; i < inputs.length; ++i) {
            // compute the norm in a forward and reverse directions to include
            // summation artifacts
            final double[] vec = inputs[i];

            final double[] reverseVec = new double[vec.length];
            for (int j = 0; j < vec.length; ++j) {
                reverseVec[vec.length - 1 - j] = vec[j];
            }

            final double exact = computeExact(vec);

            for (final Map.Entry<String, ToDoubleFunction<double[]>> entry : methods.entrySet()) {
                final ToDoubleFunction<double[]> fn = entry.getValue();

                final StatsAccumulator acc = accumulators.get(entry.getKey());

                final double forwardSample = fn.applyAsDouble(vec);
                acc.report(exact, forwardSample);

                final double reverseSample = fn.applyAsDouble(reverseVec);
                acc.report(exact, reverseSample);
            }
        }

        final Map<String, Stats> stats = new LinkedHashMap<>();
        for (final String name : methods.keySet()) {
            stats.put(name, accumulators.get(name).computeStats());
        }

        return stats;
    }

    /** Compute the exact double value of the vector norm using BigDecimals
     * with a math context of {@link MathContext#DECIMAL128}.
     * @param vec input vector
     * @return euclidean norm
     */
    private static double computeExact(final double[] vec) {
        final MathContext ctx = MathContext.DECIMAL128;

        BigDecimal sum = BigDecimal.ZERO;
        for (final double v : vec) {
            sum = sum.add(new BigDecimal(v).pow(2), ctx);
        }

        return sum.sqrt(ctx).doubleValue();
    }

    /** Compute the ulp difference between two values of the same sign.
     * @param a first input
     * @param b second input
     * @return ulp difference between the arguments
     */
    private static int computeUlpDifference(final double a, final double b) {
        return (int) (Double.doubleToLongBits(a) - Double.doubleToLongBits(b));
    }

    /** Class containing evaluation statistics for a single computation method.
     */
    public static final class Stats {

        /** Mean ulp error. */
        private final double ulpErrorMean;

        /** Ulp error standard deviation. */
        private final double ulpErrorStdDev;

        /** Ulp error minimum value. */
        private final double ulpErrorMin;

        /** Ulp error maximum value. */
        private final double ulpErrorMax;

        /** Number of failed computations. */
        private final int failCount;

        /** Construct a new instance.
         * @param ulpErrorMean ulp error mean
         * @param ulpErrorStdDev ulp error standard deviation
         * @param ulpErrorMin ulp error minimum value
         * @param ulpErrorMax ulp error maximum value
         * @param failCount number of failed computations
         */
        Stats(final double ulpErrorMean, final double ulpErrorStdDev, final double ulpErrorMin,
                final double ulpErrorMax, final int failCount) {
            this.ulpErrorMean = ulpErrorMean;
            this.ulpErrorStdDev = ulpErrorStdDev;
            this.ulpErrorMin = ulpErrorMin;
            this.ulpErrorMax = ulpErrorMax;
            this.failCount = failCount;
        }

        /** Get the ulp error mean.
         * @return ulp error mean
         */
        public double getUlpErrorMean() {
            return ulpErrorMean;
        }

        /** Get the ulp error standard deviation.
         * @return ulp error standard deviation
         */
        public double getUlpErrorStdDev() {
            return ulpErrorStdDev;
        }

        /** Get the ulp error minimum value.
         * @return ulp error minimum value
         */
        public double getUlpErrorMin() {
            return ulpErrorMin;
        }

        /** Get the ulp error maximum value.
         * @return ulp error maximum value
         */
        public double getUlpErrorMax() {
            return ulpErrorMax;
        }

        /** Get the number of failed computations, meaning the number of
         * computations that overflowed or underflowed.
         * @return number of failed computations
         */
        public int getFailCount() {
            return failCount;
        }
    }

    /** Class used to accumulate statistics during a norm evaluation run.
     */
    private static final class StatsAccumulator {

        /** Sample index. */
        private int sampleIdx;

        /** Array of ulp errors for each sample. */
        private final double[] ulpErrors;

        /** Construct a new instance.
         * @param count number of samples to be accumulated
         */
        StatsAccumulator(final int count) {
            ulpErrors = new double[count];
        }

        /** Report a computation result.
         * @param expected expected result
         * @param actual actual result
         */
        public void report(final double expected, final double actual) {
            ulpErrors[sampleIdx++] = Double.isFinite(actual) && actual != 0.0 ?
                    computeUlpDifference(expected, actual) :
                    Double.NaN;
        }

        /** Compute the final statistics for the run.
         * @return statistics object
         */
        public Stats computeStats() {
            int successCount = 0;
            double sum = 0d;
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;

            for (double ulpError : ulpErrors) {
                if (Double.isFinite(ulpError)) {
                    ++successCount;
                    min = Math.min(ulpError, min);
                    max = Math.max(ulpError, max);
                    sum += ulpError;
                }
            }

            final double mean = sum / successCount;

            double diffSumSq = 0d;
            double diff;
            for (double ulpError : ulpErrors) {
                if (Double.isFinite(ulpError)) {
                    diff = ulpError - mean;
                    diffSumSq += diff * diff;
                }
            }

            final double stdDev = successCount > 1 ?
                    Math.sqrt(diffSumSq / (successCount - 1)) :
                    0d;

            return new Stats(mean, stdDev, min, max, ulpErrors.length - successCount);
        }
    }
}
