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

package org.apache.commons.numbers.complex.streams;

import org.apache.commons.numbers.complex.Complex;
import org.apache.commons.numbers.core.precision.Precision;
import org.junit.Assert;

/**
 * Test utilities.
 */
@SuppressWarnings("WeakerAccess")
class TestUtils {
    /**
     * Collection of static methods used in math unit tests.
     */
    private TestUtils() {
        super();
    }

    /**
     * Verifies that expected and actual are within delta, or are both NaN or
     * infinities of the same sign.
     */
    public static void assertEquals(double expected, double actual, double delta) {
        assertEquals(null, expected, actual, delta);
    }

    /**
     * Verifies that expected and actual are within delta, or are both NaN or
     * infinities of the same sign.
     */
    public static void assertEquals(String msg, double expected, double actual, double delta) {
        // check for NaN
        if(Double.isNaN(expected)){
            Assert.assertTrue((msg == null ? "" : msg + "\n") + actual + " is not NaN.",
                Double.isNaN(actual));
        } else {
            Assert.assertEquals(msg, expected, actual, delta);
        }
    }

    /**
     * Verifies that the two arguments are exactly the same, either
     * both NaN or infinities of same sign, or identical floating point values.
     */
    public static void assertSame(double[] expected, double[] actual) {
        assertEquals("", expected, actual, 0);
    }

    /**
     * Verifies that the two arguments are exactly the same, either
     * both NaN or infinities of same sign, or identical floating point values.
     */
    public static void assertSame(float[] expected, float[] actual) {
        assertEquals("", expected, actual, 0);
    }

    /**
     * Verifies that the two arguments are exactly the same, either
     * both NaN or infinities of same sign, or identical floating point values.
     */
    public static void assertSame(String msg, Complex[] expected, Complex[] actual) {
        assertEquals(msg, expected, actual, 0);
    }

    /**
     * Verifies that the two arguments are exactly the same, either
     * both NaN or infinities of same sign, or identical floating point values.
     */
    public static void assertSame(String msg, Complex[][] expected, Complex[][] actual) {
        assertEquals(msg, expected, actual, 0);
    }

    /**
     * Verifies that the two arguments are exactly the same, either
     * both NaN or infinities of same sign, or identical floating point values.
     */
    public static void assertSame(String msg, Complex[][][] expected, Complex[][][] actual) {
        assertEquals(msg, expected, actual, 0);
    }

    /**
     * Verifies that the two arguments are exactly the same, either
     * both NaN or infinities of same sign, or identical floating point values.
     */
    public static void assertSame(String msg, Complex[][][][] expected, Complex[][][][] actual) {
        assertEquals(msg, expected, actual, 0);
    }

    /**
     * Verifies that the two arguments are exactly the same, either
     * both NaN or infinities of same sign, or identical floating point values.
     */
    public static void assertSame(double expected, double actual) {
        Assert.assertEquals(expected, actual, 0);
    }

    /**
     * Verifies that real and imaginary parts of the two complex arguments
     * are exactly the same.  Also ensures that NaN / infinite components match.
     */
    public static void assertSame(Complex expected, Complex actual) {
        assertSame(expected.getReal(), actual.getReal());
        assertSame(expected.getImaginary(), actual.getImaginary());
    }

    /**
     * Verifies that real and imaginary parts of the two complex arguments
     * differ by at most delta.  Also ensures that NaN / infinite components match.
     */
    public static void assertEquals(Complex expected, Complex actual, double delta) {
        Assert.assertEquals("Real Values Differ", expected.getReal(), actual.getReal(), delta);
        Assert.assertEquals("Imaginary Values Differ", expected.getImaginary(), actual.getImaginary(), delta);
    }

    /** verifies that two arrays are close (sup norm) */
    public static void assertEquals(String msg, double[] expected, double[] observed, double tolerance) {
        assertArrayLengthsEqual(msg, expected.length, observed.length);
        StringBuilder out = new StringBuilder(msg);
        boolean failure = false;
        for (int i=0; i < expected.length; i++) {
            if (!equalsIncludingNaN(expected[i], observed[i], tolerance)) {
                failure = true;
                out.append("\n[").append(i).append("] ");
                out.append("Elements differ. ");
                out.append(" expected = ");
                out.append(expected[i]);
                out.append(" observed = ");
                out.append(observed[i]);
            }
        }
        if (failure) {
            Assert.fail(out.toString());
        }
    }

    /** verifies that two 2D arrays are close (sup norm) */
    public static void assertEquals(String msg, double[][] expected, double[][] observed, double tolerance) {
        assertArrayLengthsEqual(msg, expected.length, observed.length);
        for (int i=0; i < expected.length; i++) {
            assertEquals(msg + "[" + i + "]", expected[i], observed[i], tolerance);
        }
    }

    /** verifies that two 3D arrays are close (sup norm) */
    public static void assertEquals(String msg, double[][][] expected, double[][][] observed, double tolerance) {
        assertArrayLengthsEqual(msg, expected.length, observed.length);
        for (int i=0; i < expected.length; i++) {
            assertEquals(msg + "[" + i + "]", expected[i], observed[i], tolerance);
        }
    }

    /** verifies that two 4D arrays are close (sup norm) */
    public static void assertEquals(String msg, double[][][][] expected, double[][][][] observed, double tolerance) {
        assertArrayLengthsEqual(msg, expected.length, observed.length);
        for (int i=0; i < expected.length; i++) {
            assertEquals(msg + "[" + i + "]", expected[i], observed[i], tolerance);
        }
    }

    /** verifies that two 4D arrays are close (sup norm) */
    public static void assertEquals(String msg, float[][][][] expected, float[][][][] observed, float tolerance) {
        assertArrayLengthsEqual(msg, expected.length, observed.length);
        for (int i=0; i < expected.length; i++) {
            assertEquals(msg + "[" + i + "]", expected[i], observed[i], tolerance);
        }
    }

    /** verifies that two arrays are close (sup norm) */
    public static void assertEquals(String msg, float[] expected, float[] observed, float tolerance) {
        assertArrayLengthsEqual(msg, expected.length, observed.length);
        StringBuilder out = new StringBuilder(msg);
        boolean failure = false;
        for (int i=0; i < expected.length; i++) {
            if (!equalsIncludingNaN(expected[i], observed[i], tolerance)) {
                failure = true;
                out.append("\n[").append(i).append("] ");
                out.append("Elements differ. ");
                out.append(" expected = ");
                out.append(expected[i]);
                out.append(" observed = ");
                out.append(observed[i]);
            }
        }
        if (failure) {
            Assert.fail(out.toString());
        }
    }

    /** verifies that two 2D arrays are close (sup norm) */
    public static void assertEquals(String msg, float[][] expected, float[][] observed, float tolerance) {
        assertArrayLengthsEqual(msg, expected.length, observed.length);
        for (int i=0; i < expected.length; i++) {
            assertEquals(msg + "[" + i + "]", expected[i], observed[i], tolerance);
        }
    }

    /** verifies that two 3D arrays are close (sup norm) */
    public static void assertEquals(String msg, float[][][] expected, float[][][] observed, float tolerance) {
        assertArrayLengthsEqual(msg, expected.length, observed.length);
        for (int i=0; i < expected.length; i++) {
            assertEquals(msg + "[" + i + "]", expected[i], observed[i], tolerance);
        }
    }

    /** verifies that two arrays are close (sup norm) */
    public static void assertEquals(String msg, Complex[] expected, Complex[] observed, double tolerance) {
        assertArrayLengthsEqual(msg, expected.length, observed.length);
        StringBuilder out = new StringBuilder(msg);
        boolean failure = false;
        for (int i=0; i < expected.length; i++) {
            if (!equalsIncludingNaN(expected[i].getReal(), observed[i].getReal(), tolerance)) {
                failure = true;
                out.append("\n[").append(i).append("] ");
                out.append("Real elements differ. ");
                out.append(" expected = ");
                out.append(expected[i].getReal());
                out.append(" observed = ");
                out.append(observed[i].getReal());
            }
            if (!equalsIncludingNaN(expected[i].getImaginary(), observed[i].getImaginary(), tolerance)) {
                failure = true;
                out.append("\n[").append(i).append("] ");
                out.append("Imaginary elements differ. ");
                out.append(" expected = ");
                out.append(expected[i].getImaginary());
                out.append(" observed = ");
                out.append(observed[i].getImaginary());
            }
        }
        if (failure) {
            Assert.fail(out.toString());
        }
    }

    /** verifies that two 2D arrays are close (sup norm) */
    public static void assertEquals(String msg, Complex[][] expected, Complex[][] observed, double tolerance) {
        assertArrayLengthsEqual(msg, expected.length, observed.length);
        for (int i=0; i < expected.length; i++) {
            assertEquals(msg + "[" + i + "]", expected[i], observed[i], tolerance);
        }
    }

    /** verifies that two 3D arrays are close (sup norm) */
    public static void assertEquals(String msg, Complex[][][] expected, Complex[][][] observed, double tolerance) {
        assertArrayLengthsEqual(msg, expected.length, observed.length);
        for (int i=0; i < expected.length; i++) {
            assertEquals(msg + "[" + i + "]", expected[i], observed[i], tolerance);
        }
    }

    /** verifies that two 4D arrays are close (sup norm) */
    public static void assertEquals(String msg, Complex[][][][] expected, Complex[][][][] observed, double tolerance) {
        assertArrayLengthsEqual(msg, expected.length, observed.length);
        for (int i=0; i < expected.length; i++) {
            assertEquals(msg + "[" + i + "]", expected[i], observed[i], tolerance);
        }
    }

    /**
     * Returns true if the arguments are both NaN, are equal or are within the range
     * of allowed error (inclusive).
     *
     * @param x first value
     * @param y second value
     * @param eps the amount of absolute error to allow.
     * @return {@code true} if the values are equal or within range of each other,
     * or both are NaN.
     * 
     */
    private static boolean equalsIncludingNaN(double x, double y, double eps) {
        return equalsIncludingNaN(x, y) || (Math.abs(y - x) <= eps);
    }

    /**
     * Returns true if the arguments are both NaN or they are
     * equal as defined by {@link Precision#equals(double,double) equals(x, y, 1)}.
     *
     * @param x first value
     * @param y second value
     * @return {@code true} if the values are equal or both are NaN.
     * 
     */
    private static boolean equalsIncludingNaN(double x, double y) {
        return (x != x || y != y) ? !(x != x ^ y != y) : Precision.equals(x, y, 1);
    }

    /**
     * Assert that the given array lengths are the same
     * @param msg Initial message
     * @param expectedLength expected array length
     * @param observedLength observed array length
     */
    private static void assertArrayLengthsEqual(String msg, int expectedLength, int observedLength) {
        if (expectedLength != observedLength) {
            StringBuilder out = new StringBuilder(msg);
            if (msg != null && msg.length()>0) out.append("\n");
            out.append("Arrays not same length. \n");
            out.append("expected has length ").append(expectedLength);
            out.append(" observed has length = ").append(observedLength);
            Assert.fail(out.toString());
        }

    }
}


