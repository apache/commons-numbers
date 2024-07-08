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

package org.apache.commons.numbers.complex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;

/**
 * Test utilities.
 */
public final class TestUtils {

    /**
     * The option for how to process test data lines flagged (prefixed)
     * with the {@code ;} character
     */
    public enum TestDataFlagOption {
        /** Ignore the line. */
        IGNORE,
        /** Load the data by stripping the {@code ;} character. */
        LOAD
    }

    /**
     * Collection of static methods used in math unit tests.
     */
    private TestUtils() {
    }

    /**
     * Verifies that real and imaginary parts of the two complex arguments are
     * exactly the same as defined by {@link Double#compare(double, double)}. Also
     * ensures that NaN / infinite components match.
     *
     * @param expected the expected value
     * @param actual the actual value
     */
    public static void assertSame(Complex expected, Complex actual) {
        Assertions.assertEquals(expected.getReal(), actual.getReal());
        Assertions.assertEquals(expected.getImaginary(), actual.getImaginary());
    }

    /**
     * Verifies that real and imaginary parts of the two complex arguments differ by
     * at most delta. Also ensures that NaN / infinite components match.
     *
     * @param expected the expected value
     * @param actual the actual value
     * @param delta the delta
     */
    public static void assertEquals(Complex expected, Complex actual, double delta) {
        Assertions.assertEquals(expected.getReal(), actual.getReal(), delta);
        Assertions.assertEquals(expected.getImaginary(), actual.getImaginary(), delta);
    }

    /**
     * Load test data from resources.
     *
     * <p>This method can be used to load input complex numbers and the expected result
     * after applying a function.
     *
     * <p>Data is assumed to be a resource available to the class loader. The data should
     * be space delimited doubles. Each pair of doubles on a line is converted to a
     * Complex. For example the following represents the numbers (0.5 - 0 i) and (1.5 + 2
     * i):
     *
     * <pre>
     * 0.5 -0.0 1.5 2
     * </pre>
     *
     * <p>An unmatched double not part of a pair on a line will raise an AssertionError.
     *
     * <p>Lines starting with the {@code #} character are ignored.
     *
     * <p>Lines starting with the {@code ;} character are processed using the provided
     * flag option. This character can be used to disable tests in the data file.
     *
     * <p>The flagged data will be passed to the consumer.
     *
     * @param name the resource name
     * @param option the option controlling processing of flagged data
     * @param flaggedDataConsumer the flagged data consumer (can be null)
     * @return the list
     */
    public static List<Complex[]> loadTestData(String name, TestDataFlagOption option,
            Consumer<String> flaggedDataConsumer) {
        final List<Complex[]> data = new ArrayList<>();
        try (BufferedReader input = new BufferedReader(
                new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream(name)))) {
            for  (String line = input.readLine(); line != null; line = input.readLine()) {
                line = preprocessTestData(line, option, flaggedDataConsumer);
                if (line == null) {
                    continue;
                }
                final String[] parts = line.split(" ");
                if ((parts.length & 0x1) == 1) {
                    Assertions.fail("Odd count of numbers on the line: " + line);
                }
                final Complex[] numbers = new Complex[parts.length / 2];
                for (int i = 0; i < parts.length; i += 2) {
                    final double a = Double.parseDouble(parts[i]);
                    final double b = Double.parseDouble(parts[i + 1]);
                    numbers[i / 2] = Complex.ofCartesian(a, b);
                }
                data.add(numbers);
            }
        } catch (NumberFormatException | IOException e) {
            Assertions.fail("Failed to load test data: " + name, e);
        }
        return data;
    }

    /**
     * Pre-process the next line of data from the input.
     * Returns null when the line should be ignored.
     *
     * @param input the input
     * @param option the option controlling processing of flagged data
     * @param flaggedDataConsumer the flagged data consumer (can be null)
     * @return the line of data (or null)
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static String preprocessTestData(String line, TestDataFlagOption option,
            Consumer<String> flaggedDataConsumer) {
        // Skip comments and empty lines
        if (line.isEmpty() || line.charAt(0) == '#') {
            return null;
        }
        if (line.charAt(0) == ';') {
            switch (option) {
            case LOAD:
                // Strip the leading character
                line = line.substring(1);
                break;
            case IGNORE:
            default:
                if (flaggedDataConsumer != null) {
                    flaggedDataConsumer.accept(line.substring(1));
                }
                // Ignore the line
                line = null;
            }
        }
        return line;
    }
}
