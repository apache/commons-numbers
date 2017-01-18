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

package org.apache.commons.numbers.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


import org.junit.Assert;

/**
 * Test utilities.
 * TODO: Cleanup (remove unused and obsolete methods).
 */
public class TestUtils {
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
        Assert.assertEquals(null, expected, actual, delta);
    }

    /**
     * Verifies that expected and actual are within delta, or are both NaN or
     * infinities of the same sign.
     */
    public static void assertEquals(String msg, double expected, double actual, double delta) {
        // check for NaN
        if(Double.isNaN(expected)){
            Assert.assertTrue("" + actual + " is not NaN.",
                Double.isNaN(actual));
        } else {
            Assert.assertEquals(msg, expected, actual, delta);
        }
    }

    /**
     * Verifies that the two arguments are exactly the same, either
     * both NaN or infinities of same sign, or identical floating point values.
     */
    public static void assertSame(double expected, double actual) {
     Assert.assertEquals(expected, actual, 0);
    }

    /**
     * Verifies that two double arrays have equal entries, up to tolerance
     */
    public static void assertEquals(double expected[], double observed[], double tolerance) {
        assertEquals("Array comparison failure", expected, observed, tolerance);
    }

    /**
     * Serializes an object to a bytes array and then recovers the object from the bytes array.
     * Returns the deserialized object.
     *
     * @param o  object to serialize and recover
     * @return  the recovered, deserialized object
     */
    public static Object serializeAndRecover(Object o) {
        try {
            // serialize the Object
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream so = new ObjectOutputStream(bos);
            so.writeObject(o);

            // deserialize the Object
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream si = new ObjectInputStream(bis);
            return si.readObject();
        } catch (IOException ioe) {
            return null;
        } catch (ClassNotFoundException cnfe) {
            return null;
        }
    }

    /**
     * Verifies that serialization preserves equals and hashCode.
     * Serializes the object, then recovers it and checks equals and hash code.
     *
     * @param object  the object to serialize and recover
     */
    public static void checkSerializedEquality(Object object) {
        Object object2 = serializeAndRecover(object);
        Assert.assertEquals("Equals check", object, object2);
        Assert.assertEquals("HashCode check", object.hashCode(), object2.hashCode());
    }

    /**
     * Verifies that the relative error in actual vs. expected is less than or
     * equal to relativeError.  If expected is infinite or NaN, actual must be
     * the same (NaN or infinity of the same sign).
     *
     * @param expected expected value
     * @param actual  observed value
     * @param relativeError  maximum allowable relative error
     */
    public static void assertRelativelyEquals(double expected, double actual,
            double relativeError) {
        assertRelativelyEquals(null, expected, actual, relativeError);
    }

    /**
     * Verifies that the relative error in actual vs. expected is less than or
     * equal to relativeError.  If expected is infinite or NaN, actual must be
     * the same (NaN or infinity of the same sign).
     *
     * @param msg  message to return with failure
     * @param expected expected value
     * @param actual  observed value
     * @param relativeError  maximum allowable relative error
     */
    public static void assertRelativelyEquals(String msg, double expected,
            double actual, double relativeError) {
        if (Double.isNaN(expected)) {
            Assert.assertTrue(msg, Double.isNaN(actual));
        } else if (Double.isNaN(actual)) {
            Assert.assertTrue(msg, Double.isNaN(expected));
        } else if (Double.isInfinite(actual) || Double.isInfinite(expected)) {
            Assert.assertEquals(expected, actual, relativeError);
        } else if (expected == 0.0) {
            Assert.assertEquals(msg, actual, expected, relativeError);
        } else {
            double absError = Math.abs(expected) * relativeError;
            Assert.assertEquals(msg, expected, actual, absError);
        }
    }

    /**
     * Fails iff values does not contain a number within epsilon of x.
     *
     * @param msg  message to return with failure
     * @param values double array to search
     * @param x value sought
     * @param epsilon  tolerance
     */
    public static void assertContains(String msg, double[] values,
            double x, double epsilon) {
        for (double value : values) {
            if (Precision.equals(value, x, epsilon)) {
                return;
            }
        }
        Assert.fail(msg + " Unable to find " + x);
    }

    /**
     * Fails iff values does not contain a number within epsilon of x.
     *
     * @param values double array to search
     * @param x value sought
     * @param epsilon  tolerance
     */
    public static void assertContains(double[] values, double x,
            double epsilon) {
       assertContains(null, values, x, epsilon);
    }

    /** verifies that two arrays are close (sup norm) */
    public static void assertEquals(String msg, double[] expected, double[] observed, double tolerance) {
        StringBuilder out = new StringBuilder(msg);
        if (expected.length != observed.length) {
            out.append("\n Arrays not same length. \n");
            out.append("expected has length ");
            out.append(expected.length);
            out.append(" observed length = ");
            out.append(observed.length);
            Assert.fail(out.toString());
        }
        boolean failure = false;
        for (int i=0; i < expected.length; i++) {
            if (!equalsIncludingNaN(expected[i], observed[i], tolerance)) {
                failure = true;
                out.append("\n Elements at index ");
                out.append(i);
                out.append(" differ. ");
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

    /** verifies that two arrays are close (sup norm) */
    public static void assertEquals(String msg, float[] expected, float[] observed, float tolerance) {
        StringBuilder out = new StringBuilder(msg);
        if (expected.length != observed.length) {
            out.append("\n Arrays not same length. \n");
            out.append("expected has length ");
            out.append(expected.length);
            out.append(" observed length = ");
            out.append(observed.length);
            Assert.fail(out.toString());
        }
        boolean failure = false;
        for (int i=0; i < expected.length; i++) {
            if (!equalsIncludingNaN(expected[i], observed[i], tolerance)) {
                failure = true;
                out.append("\n Elements at index ");
                out.append(i);
                out.append(" differ. ");
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

    /**
     * Updates observed counts of values in quartiles.
     * counts[0] <-> 1st quartile ... counts[3] <-> top quartile
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
     * Eliminates points with zero mass from densityPoints and densityValues parallel
     * arrays.  Returns the number of positive mass points and collapses the arrays so
     * that the first <returned value> elements of the input arrays represent the positive
     * mass points.
     */
    public static int eliminateZeroMassPoints(int[] densityPoints, double[] densityValues) {
        int positiveMassCount = 0;
        for (int i = 0; i < densityValues.length; i++) {
            if (densityValues[i] > 0) {
                positiveMassCount++;
            }
        }
        if (positiveMassCount < densityValues.length) {
            int[] newPoints = new int[positiveMassCount];
            double[] newValues = new double[positiveMassCount];
            int j = 0;
            for (int i = 0; i < densityValues.length; i++) {
                if (densityValues[i] > 0) {
                    newPoints[j] = densityPoints[i];
                    newValues[j] = densityValues[i];
                    j++;
                }
            }
            System.arraycopy(newPoints,0,densityPoints,0,positiveMassCount);
            System.arraycopy(newValues,0,densityValues,0,positiveMassCount);
        }
        return positiveMassCount;
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
     * @since 2.2
     */
    private static boolean equalsIncludingNaN(double x, double y, double eps) {
        return equalsIncludingNaN(x, y) || (Math.abs(y - x) <= eps);
    }

    /**
     * Returns true if the arguments are both NaN or they are
     * equal as defined by {@link #equals(double,double) equals(x, y, 1)}.
     *
     * @param x first value
     * @param y second value
     * @return {@code true} if the values are equal or both are NaN.
     * @since 2.2
     */
    private static boolean equalsIncludingNaN(double x, double y) {
        return (x != x || y != y) ? !(x != x ^ y != y) : Precision.equals(x, y, 1);
    }


}


