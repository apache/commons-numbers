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
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

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

    public static void assertEquals(Complex expected, Complex actual) {
        Assertions.assertEquals(expected, actual);
        Assertions.assertEquals(expected.getReal(), actual.getReal());
        Assertions.assertEquals(expected.getImaginary(), actual.getImaginary());
    }

    /**
     * Verifies that real and imaginary parts of the two complex arguments are
     * exactly the same as defined by {@link Double#compare(double, double)}. Also
     * ensures that NaN / infinite components match.
     *
     * @param expected the expected value
     * @param actual the actual value
     */
    public static void assertSame(ComplexDouble expected, ComplexDouble actual) {
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
    public static void assertEquals(ComplexDouble expected, ComplexDouble actual, double delta) {
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
     * @param line the input
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

    /**
     * Assert the complex with a scalar operation on the complex number is equal to the expected value.
     * No deltas for real or imaginary.
     * If the imaginary part is not NaN the operation must also satisfy the conjugate equality.
     *
     * <pre>
     * op(conj(z)) = conj(op(z))
     * </pre>
     *
     * <p>The results must be binary equal. This includes the sign of zero.
     * @param c the complex
     * @param operand the scalar
     * @param operation the operation
     * @param expected the expected
     * @param actual the actual
     * @param name the operation name
     */
    static void assertComplexScalar(Complex c, double operand, ComplexScalarFunction<ComplexDouble> operation,
                                    Complex expected, Complex actual, String name) {
        assertComplexScalar(c, operand, operation, expected, actual, name, 0.0D, 0.0D);
    }

    /**
     * Assert the complex with a scalar operation on the complex number is equal to the expected value.
     * If the imaginary part is not NaN the operation must also satisfy the conjugate equality.
     *
     * <pre>
     * op(conj(z)) = conj(op(z))
     * </pre>
     *
     * <p>The results must be binary equal. This includes the sign of zero.
     * @param c the complex
     * @param operand the scalar
     * @param operation the operation
     * @param expected the expected
     * @param actual the actual
     * @param name the operation name
     * @param deltaReal real delta
     * @param deltaImaginary imaginary delta
     */
    static void assertComplexScalar(Complex c, double operand, ComplexScalarFunction<ComplexDouble> operation,
                                    Complex expected, Complex actual, String name, double deltaReal, double deltaImaginary) {

        final ComplexDouble result = operation.apply(c, operand, TestUtils.ComplexDoubleConstructor.of());

        assertEquals(() -> c + "." + name + "(): real", expected.real(), actual.real(), deltaReal);
        assertEquals(() -> c + "." + name + "(): imaginary", expected.imag(), actual.imag(), deltaImaginary);

        assertEquals(() -> "ComplexFunctions." + name + "(" + c + "): real", expected.real(), result.getReal(), deltaReal);
        assertEquals(() -> "ComplexFunctions." + name + "(" + c + "): imaginary", expected.imag(), result.getImaginary(), deltaImaginary);
    }

    /**
     * Assert the double operation on the complex number is equal to the expected value.
     * No delta.
     * If the imaginary part is not NaN the operation must also satisfy the conjugate equality.
     *
     * <pre>
     * op(conj(z)) = conj(op(z))
     * </pre>
     *
     * <p>The results must be binary equal. This includes the sign of zero.
     * @param c the complex
     * @param operation the operation
     * @param expected the expected
     * @param name the operation name
     */
    static void assertDouble(Complex c, DoubleBinaryOperator operation,
                             double expected, String name) {
        assertDouble(c.getReal(), c.getImaginary(), operation, expected, name, 0.0D);
    }

    /**
     * Assert the double operation on the complex number (real and imag parts) is equal to the expected value.
     * If the imaginary part is not NaN the operation must also satisfy the conjugate equality.
     *
     * <pre>
     * op(conj(z)) = conj(op(z))
     * </pre>
     *
     * <p>The results must be binary equal. This includes the sign of zero.
     * @param r real
     * @param i imaginary
     * @param operation the operation
     * @param expected the expected
     * @param name the operation name
     * @param delta delta
     */
    static void assertDouble(double r, double i, DoubleBinaryOperator operation,
                             double expected, String name, double delta) {

        final double result = operation.applyAsDouble(r, i);

        assertEquals(() -> "ComplexFunctions." + name + "(" + expected + "): result", expected, result, delta);
    }

    /**
     * Assert the unary complex operation on the complex number is equal to the expected value.
     * No delta.
     * If the imaginary part is not NaN the operation must also satisfy the conjugate equality.
     *
     * <pre>
     * op(conj(z)) = conj(op(z))
     * </pre>
     *
     * <p>The results must be binary equal. This includes the sign of zero.
     * @param c the complex
     * @param operation1 the operation
     * @param operation2 the second operation
     * @param expected the expected
     * @param name the operation name
     */
    static void assertComplexUnary(Complex c,
                                   UnaryOperator<Complex> operation1, ComplexUnaryOperator<ComplexDouble> operation2,
                                   Complex expected, String name) {
        assertComplexUnary(c, operation1, operation2, expected, name, 0.0D, 0.0D);
    }

    /**
     * Assert the unary complex operation on the complex number is equal to the expected value.
     * If the imaginary part is not NaN the operation must also satisfy the conjugate equality.
     *
     * <pre>
     * op(conj(z)) = conj(op(z))
     * </pre>
     *
     * <p>The results must be binary equal. This includes the sign of zero.
     * @param c the complex
     * @param operation1 the operation
     * @param operation2 the second operation
     * @param expected the expected
     * @param name the operation name
     * @param delta delta
     */
    static void assertComplexUnary(Complex c,
                                   UnaryOperator<Complex> operation1, ComplexUnaryOperator<ComplexDouble> operation2,
                                   Complex expected, String name, double delta) {
        assertComplexUnary(c, operation1, operation2, expected, name, delta, delta);
    }

    /**
     * Assert the unary complex operation on the complex number is equal to the expected value.
     * If the imaginary part is not NaN the operation must also satisfy the conjugate equality.
     *
     * <pre>
     * op(conj(z)) = conj(op(z))
     * </pre>
     *
     * <p>The results must be binary equal. This includes the sign of zero.
     * @param c the complex
     * @param operation1 the operation
     * @param operation2 the second operation
     * @param expected the expected
     * @param name the operation name
     * @param deltaReal real delta
     * @param deltaImaginary imaginary delta
     */
    static void assertComplexUnary(Complex c,
                                   UnaryOperator<Complex> operation1, ComplexUnaryOperator<ComplexDouble> operation2,
                                   Complex expected, String name, double deltaReal, double deltaImaginary) {
        final Complex result1 = operation1.apply(c);
        final ComplexDouble result2 = operation2.apply(c,  TestUtils.ComplexDoubleConstructor.of());

        assertEquals(() -> c + "." + name + "(): real", expected.real(), result1.real(), deltaReal);
        assertEquals(() -> c + "." + name + "(): imaginary", expected.imag(), result1.imag(), deltaImaginary);

        assertEquals(() -> "ComplexFunctions." + name + "(" + c + "): real", expected.real(), result2.getReal(), deltaReal);
        assertEquals(() -> "ComplexFunctions." + name + "(" + c + "): imaginary", expected.imag(), result2.getImaginary(), deltaImaginary);
    }

    /**
     * Assert the binary complex operation on the complex number is equal to the expected value.
     * If the imaginary part is not NaN the operation must also satisfy the conjugate equality.
     *
     * <pre>
     * op(conj(z)) = conj(op(z))
     * </pre>
     *
     * <p>The results must be binary equal. This includes the sign of zero.
     * @param c1 the first complex
     * @param c2 the second complex
     * @param operation1 the operation
     * @param operation2 the second operation
     * @param expected the expected
     * @param name the operation name
     */
    static void assertComplexBinary(Complex c1, Complex c2,
                                    BinaryOperator<Complex> operation1, ComplexBinaryOperator<ComplexDouble> operation2,
                                    Complex expected, String name) {
        assertComplexBinary(c1, c2, operation1, operation2, expected, name, 0.0D, 0.0D);
    }

    /**
     * Assert the binary complex operation on the complex number is equal to the expected value.
     * If the imaginary part is not NaN the operation must also satisfy the conjugate equality.
     *
     * <pre>
     * op(conj(z)) = conj(op(z))
     * </pre>
     *
     * <p>The results must be binary equal. This includes the sign of zero.
     * @param c1 the first complex
     * @param c2 the second complex
     * @param operation1 the complex operation
     * @param operation2 the complexFunctions operation
     * @param expected the expected result
     * @param name the operation name
     * @param deltaReal real delta
     * @param deltaImaginary imaginary delta
     */
    static void assertComplexBinary(Complex c1, Complex c2,
                                    BinaryOperator<Complex> operation1, ComplexBinaryOperator<ComplexDouble> operation2,
                                    Complex expected, String name, double deltaReal, double deltaImaginary) {
        final Complex result1 = operation1.apply(c1, c2);
        final ComplexDouble result2 = operation2.apply(c1, c2, TestUtils.ComplexDoubleConstructor.of());

        assertEquals(() -> c1 + "." + name + "(" + c2 + "): real", expected.real(), result1.real(), deltaReal);
        assertEquals(() -> c1 + "." + name + "(" + c2 + "): imaginary", expected.imag(), result1.imag(), deltaImaginary);

        assertEquals(() -> "ComplexFunctions." + name + "(" + c1 + ", " + c2 + "): real", expected.real(), result2.getReal(), deltaReal);
        assertEquals(() -> "ComplexFunctions." + name + "(" + c1 + ", " + c2 + "): imaginary", expected.imag(), result2.getImaginary(), deltaImaginary);
    }

    /**
     * Assert the binary complex operation on the complex number is equal to the expected value.
     * If the imaginary part is not NaN the operation must also satisfy the conjugate equality.
     *
     * <pre>
     * op(conj(z)) = conj(op(z))
     * </pre>
     *
     * <p>The results must be binary equal. This includes the sign of zero.
     * @param c1 the first complex
     * @param c2 the second complex
     * @param operation1 the complex operation
     * @param operation2 the complexFunctions operation
     * @param resultChecker function to assert expected result
     * @param name the operation name
     */
    static void assertComplexBinary(Complex c1, Complex c2,
                                    BinaryOperator<Complex> operation1, ComplexBinaryOperator<ComplexDouble> operation2,
                                    ComplexConstructor<Boolean> resultChecker, String name) {
        final Complex result1 = operation1.apply(c1, c2);
        final ComplexDouble result2 = operation2.apply(c1, c2, TestUtils.ComplexDoubleConstructor.of());

        Assertions.assertTrue(resultChecker.apply(result1.getReal(), result1.getImaginary()), () -> c1 + "." + name + "(" + c2 + ")");
        Assertions.assertTrue(resultChecker.apply(result2.getReal(), result2.getImaginary()), () ->  "ComplexFunctions." + c1 + "." + name + "(" + c2 + ")");
    }

    /**
     * Assert the two numbers are equal within the provided units of least precision.
     * The maximum count of numbers allowed between the two values is {@code maxUlps - 1}.
     *
     * <p>Numbers must have the same sign. Thus -0.0 and 0.0 are never equal.
     *
     * @param msg the failure message
     * @param expected the expected
     * @param actual the actual
     * @param delta delta
     */
    static void assertEquals(Supplier<String> msg, double expected, double actual, double delta) {
        Assertions.assertEquals(expected, actual, delta, msg);
    }

    static class ComplexDoubleConstructor implements ComplexConstructor<ComplexDouble>, ComplexDouble {

        private double realPart;
        private double imaginaryPart;

        static ComplexDoubleConstructor of() {
            return new ComplexDoubleConstructor();
        }

        @Override
        public ComplexDouble apply(double real, double imaginary) {
            this.realPart = real;
            this.imaginaryPart = imaginary;
            return this;
        }

        @Override
        public double getReal() {
            return realPart;
        }

        @Override
        public double getImaginary() {
            return imaginaryPart;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ComplexDoubleConstructor that = (ComplexDoubleConstructor) o;
            return Double.compare(that.realPart, realPart) == 0 &&
                Double.compare(that.imaginaryPart, imaginaryPart) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(realPart, imaginaryPart);
        }

    }
}
