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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.numbers.examples.jmh.core.LinearCombination.ND;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test the accuracy of each implementation of {@link LinearCombination}.
 */
class LinearCombinationAccuracyTest {
    /**
     * The dot product length.
     * This must be the same for the accuracy report as the assertions.
     * The accuracy report is used to set the approximate bounds for the pass/fail
     * condition numbers.
     */
    private static final int LENGTH = 100;

    /** The number of samples to test the dot product. */
    private static final int SAMPLES = 10;

    /**
     * Provide instances of the LinearCombination interface as arguments
     * along with the condition numbers where instance will pass and fail.
     * A pass is a mean relative error to the true dot product of {@code < 1e-3}.
     *
     * @return the stream
     */
    static Stream<Arguments> provideLinearCombination() {
        return Stream.of(
            Arguments.of(LinearCombinations.Dekker.INSTANCE, 1e20, 1e30),
            Arguments.of(LinearCombinations.Dot2s.INSTANCE, 1e20, 1e30),
            Arguments.of(LinearCombinations.DotK.DOT_3, 1e35, 1e45),
            Arguments.of(LinearCombinations.DotK.DOT_4, 1e50, 1e65),
            Arguments.of(LinearCombinations.DotK.DOT_5, 1e65, 1e85),
            Arguments.of(LinearCombinations.DotK.DOT_6, 1e80, 1e100),
            Arguments.of(LinearCombinations.DotK.DOT_7, 1e95, 1e115),
            Arguments.of(LinearCombinations.ExtendedPrecision.INSTANCE, 1e300, -1),
            Arguments.of(LinearCombinations.ExtendedPrecision.DOUBLE, 1e300, -1),
            Arguments.of(LinearCombinations.ExtendedPrecision.EXACT, 1e300, -1),
            Arguments.of(LinearCombinations.Exact.INSTANCE, 1e300, -1)
        );
    }

    /**
     * Assert the dot product function passes and fails at the specified condition numbers.
     * The average relative error must be below 1e-3 to pass.
     *
     * @param fun the function
     * @param passC the pass condition number
     * @param failC the fail condition number (set to negative to ignore)
     */
    @ParameterizedTest
    @MethodSource("provideLinearCombination")
    void testDotProduct(ND fun, double passC, double failC) {
        final double[] x = new double[LENGTH];
        final double[] y = new double[LENGTH];
        // Fixed seed to consistency
        final UniformRandomProvider rng = RandomSource.create(RandomSource.XO_RO_SHI_RO_1024_PP, 9283746);

        // Use an average as the actual condition number of the generated dot product
        // may not be the requested condition number. It will average out at the desired
        // level and the pass/fail condition bounds should be suitably broad.
        double sum = 0;
        for (int i = 0; i < SAMPLES; i++) {
            final double expected = LinearCombinationUtils.genDot(passC, rng, x, y, null);
            final double observed = fun.value(x, y);
            sum += relativeError(expected, observed);
        }
        final double error = sum / SAMPLES;
        Assertions.assertTrue(error < 1e-3, () -> "Expected to pass at C=" + passC + ". Error = " + error);
        if (failC < 0) {
            return;
        }
        sum = 0;
        for (int i = 0; i < SAMPLES; i++) {
            final double expected = LinearCombinationUtils.genDot(failC, rng, x, y, null);
            final double observed = fun.value(x, y);
            sum += relativeError(expected, observed);
        }
        final double error2 = sum / SAMPLES;
        Assertions.assertFalse(error2 < 1e-3, () -> "Expected to fail at C=" + failC + ". Error = " + error2);
    }

    /**
     * Report the relative error of various implementations. This is not a test.
     * The method generates dot products with a random condition number over a suitable range.
     * The relative error of various dot product implementations are saved to a result file.
     * The file can be used to set the assertion pass/fail levels for
     * {@link #testDotProduct(ND, double, double)}.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    @Disabled("This method is used to output a report of the accuracy of implementations.")
    void reportRelativeError() throws IOException {
        // Ogita et al (2005) Figure 6.2 used length=100, samples=1000 with c in 1 to 1e120.
        // Ogita et al (2005) Figure 6.4 used length=1000, samples=2000 with c in 1 to 1e120.
        // Here the condition number is in 1e10 to 1e120 as low condition numbers are
        // computed by all methods with relative error below 1e-16. This uses the shorter
        // length for speed.
        final int samples = 2000;
        // Sample from the log-uniform distribution:
        final double logA = Math.log(1e10);
        final double logB = Math.log(1e120);

        final ArrayList<double[]> data = new ArrayList<>(samples);
        final UniformRandomProvider rng = RandomSource.create(RandomSource.XO_RO_SHI_RO_1024_PP);
        final double[] x = new double[LENGTH];
        final double[] y = new double[LENGTH];
        final double[] cc = new double[1];

        // Instances to test and their names (for the report file)
        final ArrayList<ND> methods = new ArrayList<>();
        final ArrayList<String> names = new ArrayList<>();
        addMethod(methods, names, LinearCombinations.Dekker.INSTANCE, "dekker");
        addMethod(methods, names, LinearCombinations.Dot2s.INSTANCE, "dot2s");
        addMethod(methods, names, LinearCombinations.DotK.DOT_3, "dot3");
        addMethod(methods, names, LinearCombinations.DotK.DOT_4, "dot4");
        addMethod(methods, names, LinearCombinations.DotK.DOT_5, "dot5");
        addMethod(methods, names, LinearCombinations.DotK.DOT_6, "dot6");
        addMethod(methods, names, LinearCombinations.DotK.DOT_7, "dot7");
        addMethod(methods, names, LinearCombinations.ExtendedPrecision.INSTANCE, "extended");
        addMethod(methods, names, LinearCombinations.ExtendedPrecision.DOUBLE, "extended2");
        addMethod(methods, names, LinearCombinations.ExtendedPrecision.EXACT, "extended_exact");
        addMethod(methods, names, LinearCombinations.Exact.INSTANCE, "exact");

        for (int i = 0; i < samples; i++) {
            // Random condition number.
            // This should be approximately uniform over the logarithm of the range.
            final double u = rng.nextDouble();
            final double c = Math.exp(u * logB + (1 - u) * logA);
            final double dot = LinearCombinationUtils.genDot(c, rng, x, y, cc);
            // Compute with different methods.
            // Store the condition number of the data first.
            final double[] e = new double[methods.size() + 1];
            int j = 0;
            e[j++] = cc[0];
            for (final ND method : methods) {
                e[j++] = compute(x, y, dot, method);
            }
            data.add(e);
        }

        // Sort by condition number
        Collections.sort(data, (a, b) -> Double.compare(a[0], b[0]));

        // Write to file in the Maven build directory
        try (BufferedWriter out = Files.newBufferedWriter(Paths.get("target/dot.csv"))) {
            out.append("Condition no," + names.stream().collect(Collectors.joining(",")));
            out.newLine();
            final StringBuilder sb = new StringBuilder(200);
            try (Formatter formatter = new Formatter(sb)) {
                for (final double[] e : data) {
                    sb.setLength(0);
                    formatter.format("%.3g", e[0]);
                    for (int i = 1; i < e.length; i++) {
                        formatter.format(",%.3g", e[i]);
                    }
                    out.append(sb);
                    out.newLine();
                }
            }
        }
    }

    /**
     * Adds the method to the lists
     *
     * @param methods List of methods.
     * @param names List of method names.
     * @param method Method implementation.
     * @param name Method name.
     */
    private static void addMethod(ArrayList<ND> methods, ArrayList<String> names,
                                  ND method, String name) {
        methods.add(method);
        names.add(name);
    }

    /**
     * Compute the dot product using the given function and return the relative error.
     *
     * @param x the x
     * @param y the y
     * @param expected the expected value
     * @param fun the function
     * @return the relative error
     */
    private static double compute(double[] x, double[] y, double expected, ND fun) {
        return relativeError(expected, fun.value(x, y));
    }

    /**
     * Compute the relative error:
     * <pre>
     * |expected − observed| / |expected|
     * </pre>
     *
     * <p>The value is clipped to a maximum of 2.
     *
     * @param observed the observed
     * @param expected the expected
     * @return the double
     */
    private static double relativeError(double expected, double observed) {
        return Math.min(2, Math.abs(observed - expected) / Math.abs(expected));
    }
}
