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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Test the accuracy of the algorithms in the {@link EuclideanNormAlgorithms} class.
 */
class EuclideanNormAccuracyTest {

    /** Length of vectors to compute norms for. */
    private static final int VECTOR_LENGTH = 100;

    /** Number of samples per evaluation. */
    private static final int SAMPLE_COUNT = 100_000;

    /** Report the relative error of various Euclidean norm computation methods and write
     * the results to a csv file. This is not a test.
     * @throws IOException if an I/O error occurs
     */
    @Test
    @Disabled("This method is used to output a report of the accuracy of implementations.")
    void reportUlpErrors() throws IOException {
        final UniformRandomProvider rng = RandomSource.create(RandomSource.XO_RO_SHI_RO_1024_PP);

        final EuclideanNormEvaluator eval = new EuclideanNormEvaluator();
        eval.addMethod("direct", new EuclideanNormAlgorithms.Direct())
            .addMethod("enorm", new EuclideanNormAlgorithms.Enorm())
            .addMethod("enormMod", new EuclideanNormAlgorithms.EnormMod())
            .addMethod("enormModKahan", new EuclideanNormAlgorithms.EnormModKahan())
            .addMethod("enormModExt", new EuclideanNormAlgorithms.EnormModExt())
            .addMethod("extLinear", new EuclideanNormAlgorithms.ExtendedPrecisionLinearCombination())
            .addMethod("extLinearMod", new EuclideanNormAlgorithms.ExtendedPrecisionLinearCombinationMod())
            .addMethod("extLinearSinglePass", new EuclideanNormAlgorithms.ExtendedPrecisionLinearCombinationSinglePass())
            .addMethod("extLinearSqrt2", new EuclideanNormAlgorithms.ExtendedPrecisionLinearCombinationSqrt2());

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("target/norms.csv"))) {
            writer.write("name, input type, error mean, error std dev, error min, error max, failed");
            writer.newLine();

            // construct a baseline array of random vectors
            final double[][] baseInput = createInputVectors(-10, +10, rng);

            evaluate("high", scaleInputVectors(baseInput, 0x1.0p+530), eval, writer);
            evaluate("high-thresh", scaleInputVectors(baseInput, 0x1.0p+500), eval, writer);
            evaluate("mid", baseInput, eval, writer);
            evaluate("low-thresh", scaleInputVectors(baseInput, 0x1.0p-500), eval, writer);
            evaluate("low", scaleInputVectors(baseInput, 0x1.0p-530), eval, writer);

            final double[][] fullInput = createInputVectors(-550, +550, rng);
            evaluate("full", fullInput, eval, writer);
        }
    }

    /** Perform a single evaluation run and write the results to {@code writer}.
     * @param inputType type of evaluation input
     * @param inputs input vectors
     * @param eval evaluator
     * @param writer output writer
     * @throws IOException if an I/O error occurs
     */
    private static void evaluate(final String inputType, final double[][] inputs, final EuclideanNormEvaluator eval,
            final BufferedWriter writer) throws IOException {
        final Map<String, EuclideanNormEvaluator.Stats> resultMap = eval.evaluate(inputs);
        writeResults(inputType, resultMap, writer);
    }

    /** Write evaluation results to the given writer instance.
     * @param inputType type of evaluation input
     * @param statsMap evaluation result map
     * @param writer writer instance
     * @throws IOException if an I/O error occurs
     */
    private static void writeResults(final String inputType, final Map<String, EuclideanNormEvaluator.Stats> resultMap,
            final BufferedWriter writer) throws IOException {
        for (Map.Entry<String, EuclideanNormEvaluator.Stats> entry : resultMap.entrySet()) {
            EuclideanNormEvaluator.Stats stats = entry.getValue();

            writer.write(String.format("%s,%s,%.3g,%.3g,%.3g,%.3g,%d",
                    entry.getKey(),
                    inputType,
                    stats.getUlpErrorMean(),
                    stats.getUlpErrorStdDev(),
                    stats.getUlpErrorMin(),
                    stats.getUlpErrorMax(),
                    stats.getFailCount()));
            writer.newLine();
        }
    }

    /** Create an array of random input vectors with exponents in the range {@code [minExp, maxExp]}.
     * @param minExp minimum exponent
     * @param maxExp maximum exponent
     * @param rng random number generator
     * @return array of random input vectors
     */
    private static double[][] createInputVectors(final int minExp, final int maxExp, final UniformRandomProvider rng) {
        final double[][] input = new double[SAMPLE_COUNT][];
        for (int i = 0; i < input.length; ++i) {
            input[i] = DoubleUtils.randomArray(VECTOR_LENGTH, minExp, maxExp, rng);
        }
        return input;
    }

    /** Return a copy of {@code inputs} with each element scaled by {@code s}.
     * @param inputs input vectors
     * @param s scale factor
     * @return copy of {@code inputs} with each element scaled by {@code s}
     */
    private static double[][] scaleInputVectors(final double[][] inputs, final double s) {
        final double[][] scaled = new double[inputs.length][];
        for (int i = 0; i < inputs.length; ++i) {
            scaled[i] = DoubleUtils.scalarMultiply(inputs[i], s);
        }
        return scaled;
    }
}
