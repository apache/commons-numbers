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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.numbers.core.Precision;

import org.junit.jupiter.api.Assertions;

/**
 * Test utilities. TODO: Cleanup (remove unused and obsolete methods).
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
     * Serializes an object to a bytes array and then recovers the object from the
     * bytes array. Returns the deserialized object.
     *
     * @param o object to serialize and recover
     * @return the recovered, deserialized object
     */
    public static Object serializeAndRecover(Object o) {
        try {
            // serialize the Object
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final ObjectOutputStream so = new ObjectOutputStream(bos);
            so.writeObject(o);

            // deserialize the Object
            final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            final ObjectInputStream si = new ObjectInputStream(bis);
            return si.readObject();
        } catch (final IOException ioe) {
            return null;
        } catch (final ClassNotFoundException cnfe) {
            return null;
        }
    }

    /**
     * Verifies that serialization preserves equals and hashCode. Serializes the
     * object, then recovers it and checks equals and hash code.
     *
     * @param object the object to serialize and recover
     */
    public static void checkSerializedEquality(Object object) {
        final Object object2 = serializeAndRecover(object);
        Assertions.assertEquals(object, object2, "Equals check");
        Assertions.assertEquals(object.hashCode(), object2.hashCode(), "HashCode check");
    }

    /**
     * Verifies that the relative error in actual vs. expected is less than or equal
     * to relativeError. If expected is infinite or NaN, actual must be the same
     * (NaN or infinity of the same sign).
     *
     * @param expected expected value
     * @param actual observed value
     * @param relativeError maximum allowable relative error
     */
    public static void assertRelativelyEquals(double expected, double actual, double relativeError) {
        assertRelativelyEquals(null, expected, actual, relativeError);
    }

    /**
     * Verifies that the relative error in actual vs. expected is less than or equal
     * to relativeError. If expected is infinite or NaN, actual must be the same
     * (NaN or infinity of the same sign).
     *
     * @param msg message to return with failure
     * @param expected expected value
     * @param actual observed value
     * @param relativeError maximum allowable relative error
     */
    public static void assertRelativelyEquals(String msg, double expected, double actual, double relativeError) {
        if (Double.isNaN(expected)) {
            Assertions.assertTrue(Double.isNaN(actual), msg);
        } else if (Double.isNaN(actual)) {
            Assertions.assertTrue(Double.isNaN(expected), msg);
        } else if (Double.isInfinite(actual) || Double.isInfinite(expected)) {
            Assertions.assertEquals(expected, actual, relativeError);
        } else if (expected == 0.0) {
            Assertions.assertEquals(actual, expected, relativeError, msg);
        } else {
            final double absError = Math.abs(expected) * relativeError;
            Assertions.assertEquals(expected, actual, absError, msg);
        }
    }

    /**
     * Fails iff values does not contain a number within epsilon of z.
     *
     * @param msg message to return with failure
     * @param values complex array to search
     * @param z value sought
     * @param epsilon tolerance
     */
    public static void assertContains(String msg, Complex[] values, Complex z, double epsilon) {
        for (final Complex value : values) {
            if (Precision.equals(value.getReal(), z.getReal(), epsilon) &&
                    Precision.equals(value.getImaginary(), z.getImaginary(), epsilon)) {
                return;
            }
        }
        Assertions.fail(msg + " Unable to find " + z);
    }

    /**
     * Fails iff values does not contain a number within epsilon of z.
     *
     * @param values complex array to search
     * @param z value sought
     * @param epsilon tolerance
     */
    public static void assertContains(Complex[] values, Complex z, double epsilon) {
        assertContains(null, values, z, epsilon);
    }

    /**
     * Fails iff values does not contain a number within epsilon of x.
     *
     * @param msg message to return with failure
     * @param values double array to search
     * @param x value sought
     * @param epsilon tolerance
     */
    public static void assertContains(String msg, double[] values, double x, double epsilon) {
        for (final double value : values) {
            if (Precision.equals(value, x, epsilon)) {
                return;
            }
        }
        Assertions.fail(msg + " Unable to find " + x);
    }

    /**
     * Fails iff values does not contain a number within epsilon of x.
     *
     * @param values double array to search
     * @param x value sought
     * @param epsilon tolerance
     */
    public static void assertContains(double[] values, double x, double epsilon) {
        assertContains(null, values, x, epsilon);
    }

    /** verifies that two arrays are close (sup norm) */
    public static void assertEquals(String msg, Complex[] expected, Complex[] observed, double tolerance) {
        final StringBuilder out = new StringBuilder(msg);
        if (expected.length != observed.length) {
            out.append("\n Arrays not same length. \n");
            out.append("expected has length ");
            out.append(expected.length);
            out.append(" observed length = ");
            out.append(observed.length);
            Assertions.fail(out.toString());
        }
        boolean failure = false;
        for (int i = 0; i < expected.length; i++) {
            if (!Precision.equalsIncludingNaN(expected[i].getReal(), observed[i].getReal(), tolerance)) {
                failure = true;
                out.append("\n Real elements at index ");
                out.append(i);
                out.append(" differ. ");
                out.append(" expected = ");
                out.append(expected[i].getReal());
                out.append(" observed = ");
                out.append(observed[i].getReal());
            }
            if (!Precision.equalsIncludingNaN(expected[i].getImaginary(), observed[i].getImaginary(), tolerance)) {
                failure = true;
                out.append("\n Imaginary elements at index ");
                out.append(i);
                out.append(" differ. ");
                out.append(" expected = ");
                out.append(expected[i].getImaginary());
                out.append(" observed = ");
                out.append(observed[i].getImaginary());
            }
        }
        if (failure) {
            Assertions.fail(out.toString());
        }
    }

    /**
     * Updates observed counts of values in quartiles. counts[0] <-> 1st quartile
     * ... counts[3] <-> top quartile
     */
    public static void updateCounts(double value, long[] counts, double[] quartiles) {
        if (value < quartiles[0]) {
            counts[0]++;
        } else if (value > quartiles[2]) {
            counts[3]++;
        } else if (value > quartiles[1]) {
            counts[2]++;
        } else {
            counts[1]++;
        }
    }

    /**
     * Eliminates points with zero mass from densityPoints and densityValues
     * parallel arrays. Returns the number of positive mass points and collapses the
     * arrays so that the first <returned value> elements of the input arrays
     * represent the positive mass points.
     */
    public static int eliminateZeroMassPoints(int[] densityPoints, double[] densityValues) {
        int positiveMassCount = 0;
        for (int i = 0; i < densityValues.length; i++) {
            if (densityValues[i] > 0) {
                positiveMassCount++;
            }
        }
        if (positiveMassCount < densityValues.length) {
            final int[] newPoints = new int[positiveMassCount];
            final double[] newValues = new double[positiveMassCount];
            int j = 0;
            for (int i = 0; i < densityValues.length; i++) {
                if (densityValues[i] > 0) {
                    newPoints[j] = densityPoints[i];
                    newValues[j] = densityValues[i];
                    j++;
                }
            }
            System.arraycopy(newPoints, 0, densityPoints, 0, positiveMassCount);
            System.arraycopy(newValues, 0, densityValues, 0, positiveMassCount);
        }
        return positiveMassCount;
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
