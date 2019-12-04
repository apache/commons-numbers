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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Complex}.
 */
public class ComplexTest {

    private static final double inf = Double.POSITIVE_INFINITY;
    private static final double neginf = Double.NEGATIVE_INFINITY;
    private static final double nan = Double.NaN;
    private static final double pi = Math.PI;
    private static final Complex oneInf = Complex.ofCartesian(1, inf);
    private static final Complex oneNegInf = Complex.ofCartesian(1, neginf);
    private static final Complex infOne = Complex.ofCartesian(inf, 1);
    private static final Complex infZero = Complex.ofCartesian(inf, 0);
    private static final Complex infNan = Complex.ofCartesian(inf, nan);
    private static final Complex infNegInf = Complex.ofCartesian(inf, neginf);
    private static final Complex infInf = Complex.ofCartesian(inf, inf);
    private static final Complex negInfInf = Complex.ofCartesian(neginf, inf);
    private static final Complex negInfZero = Complex.ofCartesian(neginf, 0);
    private static final Complex negInfOne = Complex.ofCartesian(neginf, 1);
    private static final Complex negInfNan = Complex.ofCartesian(neginf, nan);
    private static final Complex negInfNegInf = Complex.ofCartesian(neginf, neginf);
    private static final Complex oneNan = Complex.ofCartesian(1, nan);
    private static final Complex zeroInf = Complex.ofCartesian(0, inf);
    private static final Complex zeroNan = Complex.ofCartesian(0, nan);
    private static final Complex nanInf = Complex.ofCartesian(nan, inf);
    private static final Complex nanNegInf = Complex.ofCartesian(nan, neginf);
    private static final Complex nanZero = Complex.ofCartesian(nan, 0);
    private static final Complex NAN = Complex.ofCartesian(nan, nan);
    private static final Complex INF = Complex.ofCartesian(inf, inf);

    /**
     * Used to test the number category of a Complex.
     */
    private enum NumberType {
        NAN,
        INFINITE,
        FINITE
    }

    @Test
    public void testCartesianConstructor() {
        final Complex z = Complex.ofCartesian(3.0, 4.0);
        Assertions.assertEquals(3.0, z.getReal());
        Assertions.assertEquals(4.0, z.getImaginary());
    }

    @Test
    public void testRealConstructor() {
        final Complex z = Complex.ofReal(3.0);
        Assertions.assertEquals(3.0, z.getReal());
        Assertions.assertEquals(0.0, z.getImaginary());
    }

    @Test
    public void testPolarConstructor() {
        final double r = 98765;
        final double theta = 0.12345;
        final Complex z = Complex.ofPolar(r, theta);
        final Complex y = Complex.ofCis(theta);
        Assertions.assertEquals(r * y.getReal(), z.getReal());
        Assertions.assertEquals(r * y.getImaginary(), z.getImaginary());

        Assertions.assertThrows(IllegalArgumentException.class, () -> Complex.ofPolar(-1, 0),
            "negative modulus should not be allowed");
    }

    @Test
    public void testCisConstructor() {
        final double x = 0.12345;
        final Complex z = Complex.ofCis(x);
        Assertions.assertEquals(Math.cos(x), z.getReal());
        Assertions.assertEquals(Math.sin(x), z.getImaginary());
    }

    @Test
    public void testNumberType() {
        assertNumberType(0, 0, NumberType.FINITE);
        assertNumberType(1, 0, NumberType.FINITE);
        assertNumberType(0, 1, NumberType.FINITE);

        assertNumberType(inf, 0, NumberType.INFINITE);
        assertNumberType(-inf, 0, NumberType.INFINITE);
        assertNumberType(0, inf, NumberType.INFINITE);
        assertNumberType(0, -inf, NumberType.INFINITE);
        // A complex or imaginary value with at least one infinite part is regarded as an infinity
        // (even if its other part is a NaN).
        assertNumberType(inf, nan, NumberType.INFINITE);
        assertNumberType(-inf, nan, NumberType.INFINITE);
        assertNumberType(nan, inf, NumberType.INFINITE);
        assertNumberType(nan, -inf, NumberType.INFINITE);

        assertNumberType(nan, 0, NumberType.NAN);
        assertNumberType(0, nan, NumberType.NAN);
        assertNumberType(nan, nan, NumberType.NAN);
    }

    /**
     * Assert the number type of the Complex created from the real and imaginary
     * components.
     *
     * @param real the real component
     * @param imaginary the imaginary component
     * @param type the type
     */
    private static void assertNumberType(double real, double imaginary, NumberType type) {
        final Complex z = Complex.ofCartesian(real, imaginary);
        final boolean isNaN = z.isNaN();
        final boolean isInfinite = z.isInfinite();
        final boolean isFinite = z.isFinite();
        // A number can be only one
        int count = isNaN ? 1 : 0;
        count += isInfinite ? 1 : 0;
        count += isFinite ? 1 : 0;
        Assertions.assertEquals(1, count,
            () -> String.format("Complex can be only one type: isNaN=%s, isInfinite=%s, isFinite=%s: %s",
                                isNaN, isInfinite, isFinite, z));
        switch (type) {
        case FINITE:
            Assertions.assertTrue(isFinite, () -> "not finite: " + z);
            break;
        case INFINITE:
            Assertions.assertTrue(isInfinite, () -> "not infinite: " + z);
            break;
        case NAN:
            Assertions.assertTrue(isNaN, () -> "not nan: " + z);
            break;
        default:
            Assertions.fail("Unknown number type");
        }
    }


    @Test
    public void testProj() {
        final Complex z = Complex.ofCartesian(3.0, 4.0);
        Assertions.assertSame(z, z.proj());
        TestUtils.assertSame(infZero, Complex.ofCartesian(inf, 4.0).proj());
        TestUtils.assertSame(infZero, Complex.ofCartesian(3.0, inf).proj());
    }

    @Test
    public void testAbs() {
        final Complex z = Complex.ofCartesian(3.0, 4.0);
        Assertions.assertEquals(5.0, z.abs(), 1.0e-5);
    }

    @Test
    public void testAbsNaN() {
        Assertions.assertTrue(Double.isNaN(NAN.abs()));
        // The result is infinite if either argument is infinite
        Assertions.assertEquals(inf, Complex.ofCartesian(inf, nan).abs());
        Assertions.assertEquals(inf, Complex.ofCartesian(nan, inf).abs());
    }

    @Test
    public void testAdd() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final Complex y = Complex.ofCartesian(5.0, 6.0);
        final Complex z = x.add(y);
        Assertions.assertEquals(8.0, z.getReal(), 1.0e-5);
        Assertions.assertEquals(10.0, z.getImaginary(), 1.0e-5);
    }

    @Test
    public void testAddInf() {
        Complex x = Complex.ofCartesian(1, 1);
        final Complex z = Complex.ofCartesian(inf, 0);
        final Complex w = x.add(z);
        Assertions.assertEquals(1, w.getImaginary(), 0);
        Assertions.assertEquals(inf, w.getReal(), 0);

        x = Complex.ofCartesian(neginf, 0);
        Assertions.assertTrue(Double.isNaN(x.add(z).getReal()));
    }


    @Test
    public void testScalarAdd() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double yDouble = 2.0;
        final Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.add(yComplex), x.add(yDouble));
    }

    @Test
    public void testScalarAddNaN() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double yDouble = Double.NaN;
        final Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.add(yComplex), x.add(yDouble));
    }

    @Test
    public void testScalarAddInf() {
        Complex x = Complex.ofCartesian(1, 1);
        final double yDouble = Double.POSITIVE_INFINITY;

        final Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.add(yComplex), x.add(yDouble));

        x = Complex.ofCartesian(neginf, 0);
        Assertions.assertEquals(x.add(yComplex), x.add(yDouble));
    }

    @Test
    public void testConjugate() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final Complex z = x.conjugate();
        Assertions.assertEquals(3.0, z.getReal(), 1.0e-5);
        Assertions.assertEquals(-4.0, z.getImaginary(), 1.0e-5);
    }

    @Test
    public void testConjugateNaN() {
        final Complex z = NAN.conjugate();
        Assertions.assertTrue(z.isNaN());
    }

    @Test
    public void testConjugateInfiinite() {
        Complex z = Complex.ofCartesian(0, inf);
        Assertions.assertEquals(neginf, z.conjugate().getImaginary(), 0);
        z = Complex.ofCartesian(0, neginf);
        Assertions.assertEquals(inf, z.conjugate().getImaginary(), 0);
    }

    @Test
    public void testDivide() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final Complex y = Complex.ofCartesian(5.0, 6.0);
        final Complex z = x.divide(y);
        Assertions.assertEquals(39.0 / 61.0, z.getReal(), 1.0e-5);
        Assertions.assertEquals(2.0 / 61.0, z.getImaginary(), 1.0e-5);
    }

    @Test
    public void testDivideReal() {
        final Complex x = Complex.ofCartesian(2d, 3d);
        final Complex y = Complex.ofCartesian(2d, 0d);
        Assertions.assertEquals(Complex.ofCartesian(1d, 1.5), x.divide(y));

    }

    @Test
    public void testDivideImaginary() {
        final Complex x = Complex.ofCartesian(2d, 3d);
        final Complex y = Complex.ofCartesian(0d, 2d);
        Assertions.assertEquals(Complex.ofCartesian(1.5d, -1d), x.divide(y));
    }

    @Test
    public void testDivideZero() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final Complex z = x.divide(Complex.ZERO);
        Assertions.assertEquals(INF, z);
    }

    @Test
    public void testDivideZeroZero() {
        final Complex x = Complex.ofCartesian(0.0, 0.0);
        final Complex z = x.divide(Complex.ZERO);
        Assertions.assertEquals(NAN, z);
    }

    @Test
    public void testDivideNaN() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final Complex z = x.divide(NAN);
        Assertions.assertTrue(z.isNaN());
    }

    @Test
    public void testDivideNanInf() {
        Complex z = oneInf.divide(Complex.ONE);
        Assertions.assertTrue(Double.isNaN(z.getReal()));
        Assertions.assertEquals(inf, z.getImaginary(), 0);

        z = negInfNegInf.divide(oneNan);
        Assertions.assertTrue(Double.isNaN(z.getReal()));
        Assertions.assertTrue(Double.isNaN(z.getImaginary()));

        z = negInfInf.divide(Complex.ONE);
        Assertions.assertTrue(Double.isInfinite(z.getReal()));
        Assertions.assertTrue(Double.isInfinite(z.getImaginary()));
    }

    @Test
    public void testScalarDivide() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double yDouble = 2.0;
        final Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.divide(yComplex), x.divide(yDouble));
    }

    @Test
    public void testScalarDivideNaN() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double yDouble = Double.NaN;
        final Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.divide(yComplex), x.divide(yDouble));
    }

    @Test
    public void testScalarDivideZero() {
        final Complex x = Complex.ofCartesian(1, 1);
        TestUtils.assertEquals(x.divide(Complex.ZERO), x.divide(0), 0);
    }

    @Test
    public void testReciprocal() {
        final Complex z = Complex.ofCartesian(5.0, 6.0);
        final Complex act = z.reciprocal();
        final double expRe = 5.0 / 61.0;
        final double expIm = -6.0 / 61.0;
        Assertions.assertEquals(expRe, act.getReal(), Math.ulp(expRe));
        Assertions.assertEquals(expIm, act.getImaginary(), Math.ulp(expIm));
    }

    @Test
    public void testReciprocalReciprocal() {
        final Complex z = Complex.ofCartesian(5.0, 6.0);
        final Complex zRR = z.reciprocal().reciprocal();
        final double tol = 1e-14;
        Assertions.assertEquals(zRR.getReal(), z.getReal(), tol);
        Assertions.assertEquals(zRR.getImaginary(), z.getImaginary(), tol);
    }

    @Test
    public void testReciprocalReal() {
        final Complex z = Complex.ofCartesian(-2.0, 0.0);
        Assertions.assertTrue(Complex.equals(Complex.ofCartesian(-0.5, 0.0), z.reciprocal()));
    }

    @Test
    public void testReciprocalImaginary() {
        final Complex z = Complex.ofCartesian(0.0, -2.0);
        Assertions.assertEquals(Complex.ofCartesian(0.0, 0.5), z.reciprocal());
    }

    @Test
    public void testReciprocalNaN() {
        Assertions.assertTrue(NAN.reciprocal().isNaN());
    }

    @Test
    public void testReciprocalMax() {
        // This hits the edge case in reciprocal() for when q != 0 but scale == 0
        final double smaller = Math.nextDown(Double.MAX_VALUE);
        Complex z = Complex.ofCartesian(smaller, Double.MAX_VALUE);
        Assertions.assertEquals(Complex.ofCartesian(0.0, -0.0), z.reciprocal());
        z = Complex.ofCartesian(Double.MAX_VALUE, smaller);
        Assertions.assertEquals(Complex.ofCartesian(0.0, -0.0), z.reciprocal());
    }

    @Test
    public void testMultiply() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final Complex y = Complex.ofCartesian(5.0, 6.0);
        final Complex z = x.multiply(y);
        Assertions.assertEquals(-9.0, z.getReal(), 1.0e-5);
        Assertions.assertEquals(38.0, z.getImaginary(), 1.0e-5);
    }

    @Test
    public void testMultiplyInfInf() {
        // Assert.assertTrue(infInf.multiply(infInf).isNaN()); // MATH-620
        Assertions.assertTrue(infInf.multiply(infInf).isInfinite());
    }

    @Test
    public void testScalarMultiply() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double yDouble = 2.0;
        final Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.multiply(yComplex), x.multiply(yDouble));
        final int zInt = -5;
        final Complex zComplex = Complex.ofReal(zInt);
        Assertions.assertEquals(x.multiply(zComplex), x.multiply(zInt));
    }

    @Test
    public void testScalarMultiplyNaN() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double yDouble = Double.NaN;
        final Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.multiply(yComplex), x.multiply(yDouble));
    }

    @Test
    public void testScalarMultiplyInf() {
        final Complex x = Complex.ofCartesian(1, 1);
        double yDouble = Double.POSITIVE_INFINITY;
        Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.multiply(yComplex), x.multiply(yDouble));

        yDouble = Double.NEGATIVE_INFINITY;
        yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.multiply(yComplex), x.multiply(yDouble));
    }

    @Test
    public void testNegate() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final Complex z = x.negate();
        Assertions.assertEquals(-3.0, z.getReal(), 1.0e-5);
        Assertions.assertEquals(-4.0, z.getImaginary(), 1.0e-5);
    }

    @Test
    public void testNegateNaN() {
        final Complex z = NAN.negate();
        Assertions.assertTrue(z.isNaN());
    }

    @Test
    public void testSubtract() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final Complex y = Complex.ofCartesian(5.0, 6.0);
        final Complex z = x.subtract(y);
        Assertions.assertEquals(-2.0, z.getReal(), 1.0e-5);
        Assertions.assertEquals(-2.0, z.getImaginary(), 1.0e-5);
    }

    @Test
    public void testSubtractInf() {
        Complex x = Complex.ofCartesian(1, 1);
        final Complex z = Complex.ofCartesian(neginf, 0);
        final Complex w = x.subtract(z);
        Assertions.assertEquals(1, w.getImaginary(), 0);
        Assertions.assertEquals(inf, w.getReal(), 0);

        x = Complex.ofCartesian(neginf, 0);
        Assertions.assertTrue(Double.isNaN(x.subtract(z).getReal()));
    }

    @Test
    public void testScalarSubtract() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double yDouble = 2.0;
        final Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.subtract(yComplex), x.subtract(yDouble));
    }

    @Test
    public void testScalarSubtractNaN() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final double yDouble = Double.NaN;
        final Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.subtract(yComplex), x.subtract(yDouble));
    }

    @Test
    public void testScalarSubtractInf() {
        Complex x = Complex.ofCartesian(1, 1);
        final double yDouble = Double.POSITIVE_INFINITY;
        final Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.subtract(yComplex), x.subtract(yDouble));

        x = Complex.ofCartesian(neginf, 0);
        Assertions.assertEquals(x.subtract(yComplex), x.subtract(yDouble));
    }

    @Test
    public void testFloatingPointEqualsPrecondition1() {
        Assertions.assertThrows(NullPointerException.class,
            () -> Complex.equals(Complex.ofCartesian(3.0, 4.0), null, 3)
        );
    }

    @Test
    public void testFloatingPointEqualsPrecondition2() {
        Assertions.assertThrows(NullPointerException.class,
            () -> Complex.equals(null, Complex.ofCartesian(3.0, 4.0), 3)
        );
    }

    @Test
    public void testFloatingPointEquals() {
        double re = -3.21;
        double im = 456789e10;

        final Complex x = Complex.ofCartesian(re, im);
        Complex y = Complex.ofCartesian(re, im);

        Assertions.assertEquals(x, y);
        Assertions.assertTrue(Complex.equals(x, y));

        final int maxUlps = 5;
        for (int i = 0; i < maxUlps; i++) {
            re = Math.nextUp(re);
            im = Math.nextUp(im);
        }
        y = Complex.ofCartesian(re, im);
        Assertions.assertTrue(Complex.equals(x, y, maxUlps));

        re = Math.nextUp(re);
        im = Math.nextUp(im);
        y = Complex.ofCartesian(re, im);
        Assertions.assertFalse(Complex.equals(x, y, maxUlps));
    }

    @Test
    public void testFloatingPointEqualsNaN() {
        Complex c = Complex.ofCartesian(Double.NaN, 1);
        Assertions.assertFalse(Complex.equals(c, c));

        c = Complex.ofCartesian(1, Double.NaN);
        Assertions.assertFalse(Complex.equals(c, c));
    }

    @Test
    public void testFloatingPointEqualsWithAllowedDelta() {
        final double re = 153.0000;
        final double im = 152.9375;
        final double tol1 = 0.0625;
        final Complex x = Complex.ofCartesian(re, im);
        final Complex y = Complex.ofCartesian(re + tol1, im + tol1);
        Assertions.assertTrue(Complex.equals(x, y, tol1));

        final double tol2 = 0.0624;
        Assertions.assertFalse(Complex.equals(x, y, tol2));
    }

    @Test
    public void testFloatingPointEqualsWithAllowedDeltaNaN() {
        final Complex x = Complex.ofCartesian(0, Double.NaN);
        final Complex y = Complex.ofCartesian(Double.NaN, 0);
        Assertions.assertFalse(Complex.equals(x, Complex.ZERO, 0.1));
        Assertions.assertFalse(Complex.equals(x, x, 0.1));
        Assertions.assertFalse(Complex.equals(x, y, 0.1));
    }

    @Test
    public void testFloatingPointEqualsWithRelativeTolerance() {
        final double tol = 1e-4;
        final double re = 1;
        final double im = 1e10;

        final double f = 1 + tol;
        final Complex x = Complex.ofCartesian(re, im);
        final Complex y = Complex.ofCartesian(re * f, im * f);
        Assertions.assertTrue(Complex.equalsWithRelativeTolerance(x, y, tol));
    }

    @Test
    public void testFloatingPointEqualsWithRelativeToleranceNan() {
        final Complex x = Complex.ofCartesian(0, Double.NaN);
        final Complex y = Complex.ofCartesian(Double.NaN, 0);
        Assertions.assertFalse(Complex.equalsWithRelativeTolerance(x, Complex.ZERO, 0.1));
        Assertions.assertFalse(Complex.equalsWithRelativeTolerance(x, x, 0.1));
        Assertions.assertFalse(Complex.equalsWithRelativeTolerance(x, y, 0.1));
    }

    @Test
    public void testEqualsWithNull() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        Assertions.assertFalse(x.equals(null));
    }

    @Test
    public void testEqualsWithAnotherClass() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        Assertions.assertFalse(x.equals(new Object()));
    }

    @Test
    public void testEqualsWithSameObject() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        Assertions.assertEquals(x, x);
    }

    @Test
    public void testEqualsWithCopyObject() {
        final Complex x = Complex.ofCartesian(3.0, 4.0);
        final Complex y = Complex.ofCartesian(3.0, 4.0);
        Assertions.assertEquals(x, y);
    }

    @Test
    public void testEqualsWithRealDifference() {
        final Complex x = Complex.ofCartesian(0.0, 0.0);
        final Complex y = Complex.ofCartesian(0.0 + Double.MIN_VALUE, 0.0);
        Assertions.assertNotEquals(x, y);
    }

    @Test
    public void testEqualsWithImaginaryDifference() {
        final Complex x = Complex.ofCartesian(0.0, 0.0);
        final Complex y = Complex.ofCartesian(0.0, 0.0 + Double.MIN_VALUE);
        Assertions.assertNotEquals(x, y);
    }

    /**
     * Test {@link Complex#equals(Object)}. It should be consistent with
     * {@link Arrays#equals(double[], double[])} called using the components of two complex numbers.
     */
    @Test
    public void testEqualsIsConsistentWithArraysEquals() {
        // Explicit check of the cases documented in the Javadoc:
        assertEqualsIsConsistentWithArraysEquals(
                Complex.ofCartesian(Double.NaN, 0.0),
                Complex.ofCartesian(Double.NaN, 1.0), "NaN real and different non-NaN imaginary");
        assertEqualsIsConsistentWithArraysEquals(
                Complex.ofCartesian(0.0, Double.NaN),
                Complex.ofCartesian(1.0, Double.NaN), "Different non-NaN real and NaN imaginary");
        assertEqualsIsConsistentWithArraysEquals(
                Complex.ofCartesian(0.0, 0.0),
                Complex.ofCartesian(-0.0, 0.0), "Different real zeros");
        assertEqualsIsConsistentWithArraysEquals(
                Complex.ofCartesian(0.0, 0.0),
                Complex.ofCartesian(0.0, -0.0), "Different imaginary zeros");

        // Test some values of edge cases
        final double[] values = {
            Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, -1, 0, 1
        };
        final ArrayList<Complex> list = createCombinations(values);

        for (Complex c : list) {
            final double real = c.getReal();
            final double imag = c.getImaginary();

            // Check a copy is equal
            assertEqualsIsConsistentWithArraysEquals(c, Complex.ofCartesian(real, imag), "Copy complex");

            // Perform the smallest change to the two components
            final double realDelta = smallestChange(real);
            final double imagDelta = smallestChange(imag);
            Assertions.assertNotEquals(real, realDelta, "Real was not changed");
            Assertions.assertNotEquals(imag, imagDelta, "Imaginary was not changed");

            assertEqualsIsConsistentWithArraysEquals(c, Complex.ofCartesian(realDelta, imag), "Delta real");
            assertEqualsIsConsistentWithArraysEquals(c, Complex.ofCartesian(real, imagDelta), "Delta imaginary");
        }
    }

    /**
     * Specific test to target different representations that return {@code true} for
     * {@link Complex#isNaN()} are {@code false} for {@link Complex#equals(Object)}.
     */
    @Test
    public void testEqualsWithDifferentNaNs() {
        // Test some NaN combinations
        final double[] values = {
            Double.NaN, 0, 1
        };
        final ArrayList<Complex> list = createCombinations(values);

        // Is the all-vs-all comparison only the exact same values should be equal, e.g.
        // (nan,0) not equals (nan,nan)
        // (nan,0) equals (nan,0)
        // (nan,0) not equals (0,nan)
        for (int i = 0; i < list.size(); i++) {
            final Complex c1 = list.get(i);
            final Complex copy = Complex.ofCartesian(c1.getReal(), c1.getImaginary());
            assertEqualsIsConsistentWithArraysEquals(c1, copy, "Copy is not equal");
            for (int j = i + 1; j < list.size(); j++) {
                final Complex c2 = list.get(j);
                assertEqualsIsConsistentWithArraysEquals(c1, c2, "Different NaNs should not be equal");
            }
        }
    }

    /**
     * Test the two complex numbers with {@link Complex#equals(Object)} and check the result
     * is consistent with {@link Arrays#equals(double[], double[])}.
     *
     * @param c1 the first complex
     * @param c2 the second complex
     * @param msg the message to append to an assertion error
     */
    private static void assertEqualsIsConsistentWithArraysEquals(Complex c1, Complex c2, String msg) {
        final boolean expected = Arrays.equals(new double[]{c1.getReal(), c1.getImaginary()},
                                               new double[]{c2.getReal(), c2.getImaginary()});
        final boolean actual = c1.equals(c2);
        Assertions.assertEquals(expected, actual, () -> String.format(
            "equals(Object) is not consistent with Arrays.equals: %s. %s vs %s", msg, c1, c2));
    }

    /**
     * Test {@link Complex#hashCode()}. It should be consistent with
     * {@link Arrays#hashCode(double[])} called using the components of the complex number
     * and fulfil the contract of {@link Object#hashCode()},
     * i.e. objects with different hash codes are {@code false} for {@link Object#equals(Object)}.
     */
    @Test
    public void testHashCode() {
        // Test some values match Arrays.hashCode(double[])
        final double[] values = {
            Double.NaN, Double.NEGATIVE_INFINITY, -3.45, -1, -0.0, 0.0,
            Double.MIN_VALUE, 1, 3.45, Double.POSITIVE_INFINITY
        };
        final ArrayList<Complex> list = createCombinations(values);

        final String msg = "'equals' not compatible with 'hashCode'";

        for (Complex c : list) {
            final double real = c.getReal();
            final double imag = c.getImaginary();
            final int expected = Arrays.hashCode(new double[] {real, imag});
            final int hash = c.hashCode();
            Assertions.assertEquals(expected, hash, "hashCode does not match Arrays.hashCode({re, im})");

            // Test a copy has the same hash code, i.e. is not System.identityHashCode(Object)
            final Complex copy = Complex.ofCartesian(real, imag);
            Assertions.assertEquals(hash, copy.hashCode(), "Copy hash code is not equal");

            // MATH-1118
            // "equals" and "hashCode" must be compatible: if two objects have
            // different hash codes, "equals" must return false.
            // Perform the smallest change to the two components.
            // Note: The hash could actually be the same so we check it changes.
            final double realDelta = smallestChange(real);
            final double imagDelta = smallestChange(imag);
            Assertions.assertNotEquals(real, realDelta, "Real was not changed");
            Assertions.assertNotEquals(imag, imagDelta, "Imaginary was not changed");

            Complex cRealDelta = Complex.ofCartesian(realDelta, imag);
            Complex cImagDelta = Complex.ofCartesian(real, imagDelta);
            if (hash != cRealDelta.hashCode()) {
                Assertions.assertNotEquals(c, cRealDelta, () -> "real+delta: " + msg);
            }
            if (hash != cImagDelta.hashCode()) {
                Assertions.assertNotEquals(c, cImagDelta, () -> "imaginary+delta: " + msg);
            }
        }
    }

    /**
     * Specific test that different representations of zero satisfy the contract of
     * {@link Object#hashCode()}: if two objects have different hash codes, "equals" must
     * return false. This is an issue with using {@link Double#hashCode(double)} to create hash
     * codes and {@code ==} for equality when using different representations of zero:
     * Double.hashCode(-0.0) != Double.hashCode(0.0) but -0.0 == 0.0 is {@code true}.
     *
     * @see <a href="https://issues.apache.org/jira/projects/MATH/issues/MATH-1118">MATH-1118</a>
     */
    @Test
    public void testHashCodeWithDifferentZeros() {
        final double[] values = {-0.0, 0.0};
        final ArrayList<Complex> list = createCombinations(values);

        // Explicit test for issue MATH-1118
        // "equals" and "hashCode" must be compatible
        for (int i = 0; i < list.size(); i++) {
            final Complex c1 = list.get(i);
            for (int j = i + 1; j < list.size(); j++) {
                final Complex c2 = list.get(j);
                if (c1.hashCode() != c2.hashCode()) {
                    Assertions.assertNotEquals(c1, c2, "'equals' not compatible with 'hashCode'");
                }
            }
        }
    }

    /**
     * Creates a list of Complex numbers using an all-vs-all combination of the provided
     * values for both the real and imaginary parts.
     *
     * @param values the values
     * @return the list
     */
    private static ArrayList<Complex> createCombinations(final double[] values) {
        final ArrayList<Complex> list = new ArrayList<>(values.length * values.length);
        for (double re : values) {
            for (double im : values) {
                list.add(Complex.ofCartesian(re, im));
            }
        }
        return list;
    }

    /**
     * Perform the smallest change to the value. This returns the next double value adjacent to
     * d in the direction of infinity. Edge cases: if already infinity then return the next closest
     * in the direction of negative infinity; if nan then return 0.
     *
     * @param x the x
     * @return the new value
     */
    private static double smallestChange(double x) {
        if (Double.isNaN(x)) {
            return 0;
        }
        return x == Double.POSITIVE_INFINITY ?
                Math.nextDown(x) :
                Math.nextUp(x);
    }

    @Test
    @Disabled("Used to output the java environment")
    public void testJava() {
        // CHECKSTYLE: stop Regexp
        System.out.println(">>testJava()");
        // MathTest#testExpSpecialCases() checks the following:
        // Assert.assertEquals("exp of -infinity should be 0.0", 0.0, Math.exp(Double.NEGATIVE_INFINITY), Precision.EPSILON);
        // Let's check how well Math works:
        System.out.println("Math.exp=" + Math.exp(Double.NEGATIVE_INFINITY));
        final String[] props = {
            "java.version", //    Java Runtime Environment version
            "java.vendor", // Java Runtime Environment vendor
            "java.vm.specification.version", //   Java Virtual Machine specification version
            "java.vm.specification.vendor", //    Java Virtual Machine specification vendor
            "java.vm.specification.name", //  Java Virtual Machine specification name
            "java.vm.version", // Java Virtual Machine implementation version
            "java.vm.vendor", //  Java Virtual Machine implementation vendor
            "java.vm.name", //    Java Virtual Machine implementation name
            "java.specification.version", //  Java Runtime Environment specification version
            "java.specification.vendor", //   Java Runtime Environment specification vendor
            "java.specification.name", // Java Runtime Environment specification name
            "java.class.version", //  Java class format version number
        };
        for (final String t : props) {
            System.out.println(t + "=" + System.getProperty(t));
        }
        System.out.println("<<testJava()");
        // CHECKSTYLE: resume Regexp
    }

    @Test
    public void testPow() {
        final Complex x = Complex.ofCartesian(3, 4);
        final double yDouble = 5.0;
        final Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.pow(yComplex), x.pow(yDouble));
    }

    @Test
    public void testPowComplexRealZero() {
        // Hits the edge case when real == 0 but imaginary != 0
        final Complex x = Complex.ofCartesian(0, 1);
        final Complex z = Complex.ofCartesian(2, 3);
        final Complex c = x.pow(z);
        // Answer from g++
        Assertions.assertEquals(-0.008983291021129429, c.getReal());
        Assertions.assertEquals(1.1001358594835313e-18, c.getImaginary());
    }

    @Test
    public void testPowComplexZeroBase() {
        final double x = Double.MIN_VALUE;
        assertPowComplexZeroBase(0, 0, NAN);
        assertPowComplexZeroBase(0, x, NAN);
        assertPowComplexZeroBase(x, x, NAN);
        assertPowComplexZeroBase(x, 0, Complex.ZERO);
    }

    private static void assertPowComplexZeroBase(double re, double im, Complex expected) {
        final Complex z = Complex.ofCartesian(re, im);
        final Complex c = Complex.ZERO.pow(z);
        Assertions.assertEquals(expected, c);
    }

    @Test
    public void testPowScalerRealZero() {
        // Hits the edge case when real == 0 but imaginary != 0
        final Complex x = Complex.ofCartesian(0, 1);
        final Complex c = x.pow(2);
        // Answer from g++
        Assertions.assertEquals(-1, c.getReal());
        Assertions.assertEquals(1.2246467991473532e-16, c.getImaginary());
    }

    @Test
    public void testPowScalarZeroBase() {
        final double x = Double.MIN_VALUE;
        assertPowScalarZeroBase(0, NAN);
        assertPowScalarZeroBase(x, Complex.ZERO);
    }

    private static void assertPowScalarZeroBase(double exp, Complex expected) {
        final Complex c = Complex.ZERO.pow(exp);
        Assertions.assertEquals(expected, c);
    }

    @Test
    public void testPowNanBase() {
        final Complex x = NAN;
        final double yDouble = 5.0;
        final Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.pow(yComplex), x.pow(yDouble));
    }

    @Test
    public void testPowNanExponent() {
        final Complex x = Complex.ofCartesian(3, 4);
        final double yDouble = Double.NaN;
        final Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.pow(yComplex), x.pow(yDouble));
    }

    @Test
    public void testSqrtPolar() {
        final double tol = 1e-12;
        double r = 1;
        for (int i = 0; i < 5; i++) {
            r += i;
            double theta = 0;
            for (int j = 0; j < 11; j++) {
                theta += pi / 12;
                final Complex z = Complex.ofPolar(r, theta);
                final Complex sqrtz = Complex.ofPolar(Math.sqrt(r), theta / 2);
                TestUtils.assertEquals(sqrtz, z.sqrt(), tol);
            }
        }
    }

    @Test
    public void testZerothRootThrows() {
        final Complex c = Complex.ofCartesian(1, 1);
        Assertions.assertThrows(IllegalArgumentException.class, () -> c.nthRoot(0),
            "zeroth root should not be allowed");
    }

    /**
     * Test: computing <b>third roots</b> of z.
     * <pre>
     * <code>
     * <b>z = -2 + 2 * i</b>
     *   => z_0 =  1      +          i
     *   => z_1 = -1.3660 + 0.3660 * i
     *   => z_2 =  0.3660 - 1.3660 * i
     * </code>
     * </pre>
     */
    @Test
    public void testNthRootNormalThirdRoot() {
        // The complex number we want to compute all third-roots for.
        final Complex z = Complex.ofCartesian(-2, 2);
        // The List holding all third roots
        final Complex[] thirdRootsOfZ = z.nthRoot(3).toArray(new Complex[0]);
        // Returned Collection must not be empty!
        Assertions.assertEquals(3, thirdRootsOfZ.length);
        // test z_0
        Assertions.assertEquals(1.0,                  thirdRootsOfZ[0].getReal(),      1.0e-5);
        Assertions.assertEquals(1.0,                  thirdRootsOfZ[0].getImaginary(), 1.0e-5);
        // test z_1
        Assertions.assertEquals(-1.3660254037844386,  thirdRootsOfZ[1].getReal(),      1.0e-5);
        Assertions.assertEquals(0.36602540378443843,  thirdRootsOfZ[1].getImaginary(), 1.0e-5);
        // test z_2
        Assertions.assertEquals(0.366025403784439,    thirdRootsOfZ[2].getReal(),      1.0e-5);
        Assertions.assertEquals(-1.3660254037844384,  thirdRootsOfZ[2].getImaginary(), 1.0e-5);
    }

    /**
     * Test: computing <b>fourth roots</b> of z.
     * <pre>
     * <code>
     * <b>z = 5 - 2 * i</b>
     *   => z_0 =  1.5164 - 0.1446 * i
     *   => z_1 =  0.1446 + 1.5164 * i
     *   => z_2 = -1.5164 + 0.1446 * i
     *   => z_3 = -1.5164 - 0.1446 * i
     * </code>
     * </pre>
     */
    @Test
    public void testNthRootNormalFourthRoot() {
        // The complex number we want to compute all third-roots for.
        final Complex z = Complex.ofCartesian(5, -2);
        // The List holding all fourth roots
        final Complex[] fourthRootsOfZ = z.nthRoot(4).toArray(new Complex[0]);
        // Returned Collection must not be empty!
        Assertions.assertEquals(4, fourthRootsOfZ.length);
        // test z_0
        Assertions.assertEquals(1.5164629308487783,     fourthRootsOfZ[0].getReal(),      1.0e-5);
        Assertions.assertEquals(-0.14469266210702247,   fourthRootsOfZ[0].getImaginary(), 1.0e-5);
        // test z_1
        Assertions.assertEquals(0.14469266210702256,    fourthRootsOfZ[1].getReal(),      1.0e-5);
        Assertions.assertEquals(1.5164629308487783,     fourthRootsOfZ[1].getImaginary(), 1.0e-5);
        // test z_2
        Assertions.assertEquals(-1.5164629308487783,    fourthRootsOfZ[2].getReal(),      1.0e-5);
        Assertions.assertEquals(0.14469266210702267,    fourthRootsOfZ[2].getImaginary(), 1.0e-5);
        // test z_3
        Assertions.assertEquals(-0.14469266210702275,   fourthRootsOfZ[3].getReal(),      1.0e-5);
        Assertions.assertEquals(-1.5164629308487783,    fourthRootsOfZ[3].getImaginary(), 1.0e-5);
    }

    /**
     * Test: computing <b>third roots</b> of z.
     * <pre>
     * <code>
     * <b>z = 8</b>
     *   => z_0 =  2
     *   => z_1 = -1 + 1.73205 * i
     *   => z_2 = -1 - 1.73205 * i
     * </code>
     * </pre>
     */
    @Test
    public void testNthRootCornercaseThirdRootImaginaryPartEmpty() {
        // The number 8 has three third roots. One we all already know is the number 2.
        // But there are two more complex roots.
        final Complex z = Complex.ofCartesian(8, 0);
        // The List holding all third roots
        final Complex[] thirdRootsOfZ = z.nthRoot(3).toArray(new Complex[0]);
        // Returned Collection must not be empty!
        Assertions.assertEquals(3, thirdRootsOfZ.length);
        // test z_0
        Assertions.assertEquals(2.0,                thirdRootsOfZ[0].getReal(),      1.0e-5);
        Assertions.assertEquals(0.0,                thirdRootsOfZ[0].getImaginary(), 1.0e-5);
        // test z_1
        Assertions.assertEquals(-1.0,               thirdRootsOfZ[1].getReal(),      1.0e-5);
        Assertions.assertEquals(1.7320508075688774, thirdRootsOfZ[1].getImaginary(), 1.0e-5);
        // test z_2
        Assertions.assertEquals(-1.0,               thirdRootsOfZ[2].getReal(),      1.0e-5);
        Assertions.assertEquals(-1.732050807568877, thirdRootsOfZ[2].getImaginary(), 1.0e-5);
    }


    /**
     * Test: computing <b>third roots</b> of z with real part 0.
     * <pre>
     * <code>
     * <b>z = 2 * i</b>
     *   => z_0 =  1.0911 + 0.6299 * i
     *   => z_1 = -1.0911 + 0.6299 * i
     *   => z_2 = -2.3144 - 1.2599 * i
     * </code>
     * </pre>
     */
    @Test
    public void testNthRootCornercaseThirdRootRealPartZero() {
        // complex number with only imaginary part
        final Complex z = Complex.ofCartesian(0, 2);
        // The List holding all third roots
        final Complex[] thirdRootsOfZ = z.nthRoot(3).toArray(new Complex[0]);
        // Returned Collection must not be empty!
        Assertions.assertEquals(3, thirdRootsOfZ.length);
        // test z_0
        Assertions.assertEquals(1.0911236359717216,      thirdRootsOfZ[0].getReal(),      1.0e-5);
        Assertions.assertEquals(0.6299605249474365,      thirdRootsOfZ[0].getImaginary(), 1.0e-5);
        // test z_1
        Assertions.assertEquals(-1.0911236359717216,     thirdRootsOfZ[1].getReal(),      1.0e-5);
        Assertions.assertEquals(0.6299605249474365,      thirdRootsOfZ[1].getImaginary(), 1.0e-5);
        // test z_2
        Assertions.assertEquals(-2.3144374213981936E-16, thirdRootsOfZ[2].getReal(),      1.0e-5);
        Assertions.assertEquals(-1.2599210498948732,     thirdRootsOfZ[2].getImaginary(), 1.0e-5);
    }

    /**
     * Test: compute <b>third roots</b> using a negative argument
     * to go clockwise around the unit circle. Fourth roots of one
     * are taken in both directions around the circle using
     * positive and negative arguments.
     * <pre>
     * <code>
     * <b>z = 1</b>
     *   => z_0 = Positive: 1,0 ; Negative: 1,0
     *   => z_1 = Positive: 0,1 ; Negative: 0,-1
     *   => z_2 = Positive: -1,0 ; Negative: -1,0
     *   => z_3 = Positive: 0,-1 ; Negative: 0,1
     * </code>
     * </pre>
     */
    @Test
    public void testNthRootNegativeArg() {
        // The complex number we want to compute all third-roots for.
        final Complex z = Complex.ofCartesian(1, 0);
        // The List holding all fourth roots
        Complex[] fourthRootsOfZ = z.nthRoot(4).toArray(new Complex[0]);
        // test z_0
        Assertions.assertEquals(1,     fourthRootsOfZ[0].getReal(),      1.0e-5);
        Assertions.assertEquals(0,   fourthRootsOfZ[0].getImaginary(), 1.0e-5);
        // test z_1
        Assertions.assertEquals(0,    fourthRootsOfZ[1].getReal(),      1.0e-5);
        Assertions.assertEquals(1,     fourthRootsOfZ[1].getImaginary(), 1.0e-5);
        // test z_2
        Assertions.assertEquals(-1,    fourthRootsOfZ[2].getReal(),      1.0e-5);
        Assertions.assertEquals(0,    fourthRootsOfZ[2].getImaginary(), 1.0e-5);
        // test z_3
        Assertions.assertEquals(0,   fourthRootsOfZ[3].getReal(),      1.0e-5);
        Assertions.assertEquals(-1,    fourthRootsOfZ[3].getImaginary(), 1.0e-5);
        // go clockwise around the unit circle using negative argument
        fourthRootsOfZ = z.nthRoot(-4).toArray(new Complex[0]);
        // test z_0
        Assertions.assertEquals(1,     fourthRootsOfZ[0].getReal(),      1.0e-5);
        Assertions.assertEquals(0,   fourthRootsOfZ[0].getImaginary(), 1.0e-5);
        // test z_1
        Assertions.assertEquals(0,    fourthRootsOfZ[1].getReal(),      1.0e-5);
        Assertions.assertEquals(-1,     fourthRootsOfZ[1].getImaginary(), 1.0e-5);
        // test z_2
        Assertions.assertEquals(-1,    fourthRootsOfZ[2].getReal(),      1.0e-5);
        Assertions.assertEquals(0,    fourthRootsOfZ[2].getImaginary(), 1.0e-5);
        // test z_3
        Assertions.assertEquals(0,   fourthRootsOfZ[3].getReal(),      1.0e-5);
        Assertions.assertEquals(1,    fourthRootsOfZ[3].getImaginary(), 1.0e-5);
    }

    @Test
    public void testNthRootNan() {
        final int n = 3;
        final Complex z = Complex.ofReal(Double.NaN);
        final List<Complex> r = z.nthRoot(n);
        Assertions.assertEquals(n, r.size());
        for (final Complex c : r) {
            Assertions.assertTrue(Double.isNaN(c.getReal()));
            Assertions.assertTrue(Double.isNaN(c.getImaginary()));
        }
    }
    @Test
    public void testNthRootInf() {
        final int n = 3;
        final Complex z = Complex.ofReal(Double.NEGATIVE_INFINITY);
        final List<Complex> r = z.nthRoot(n);
        Assertions.assertEquals(n, r.size());
    }

    /**
     * Test standard values
     */
    @Test
    public void testGetArgument() {
        Complex z = Complex.ofCartesian(1, 0);
        assertGetArgument(0.0, z, 1.0e-12);

        z = Complex.ofCartesian(1, 1);
        assertGetArgument(Math.PI / 4, z, 1.0e-12);

        z = Complex.ofCartesian(0, 1);
        assertGetArgument(Math.PI / 2, z, 1.0e-12);

        z = Complex.ofCartesian(-1, 1);
        assertGetArgument(3 * Math.PI / 4, z, 1.0e-12);

        z = Complex.ofCartesian(-1, 0);
        assertGetArgument(Math.PI, z, 1.0e-12);

        z = Complex.ofCartesian(-1, -1);
        assertGetArgument(-3 * Math.PI / 4, z, 1.0e-12);

        z = Complex.ofCartesian(0, -1);
        assertGetArgument(-Math.PI / 2, z, 1.0e-12);

        z = Complex.ofCartesian(1, -1);
        assertGetArgument(-Math.PI / 4, z, 1.0e-12);
    }

    /**
     * Verify atan2-style handling of infinite parts
     */
    @Test
    public void testGetArgumentInf() {
        assertGetArgument(Math.PI / 4, infInf, 1.0e-12);
        assertGetArgument(Math.PI / 2, oneInf, 1.0e-12);
        assertGetArgument(0.0, infOne, 1.0e-12);
        assertGetArgument(Math.PI / 2, zeroInf, 1.0e-12);
        assertGetArgument(0.0, infZero, 1.0e-12);
        assertGetArgument(Math.PI, negInfOne, 1.0e-12);
        assertGetArgument(-3.0 * Math.PI / 4, negInfNegInf, 1.0e-12);
        assertGetArgument(-Math.PI / 2, oneNegInf, 1.0e-12);
    }

    /**
     * Verify that either part NaN results in NaN
     */
    @Test
    public void testGetArgumentNaN() {
        assertGetArgument(Double.NaN, nanZero, 0);
        assertGetArgument(Double.NaN, zeroNan, 0);
        assertGetArgument(Double.NaN, NAN, 0);
    }

    private static void assertGetArgument(double expected, Complex complex, double delta) {
        final double actual = complex.getArgument();
        Assertions.assertEquals(expected, actual, delta);
        Assertions.assertEquals(actual, complex.arg(), delta);
    }

    @Test
    public void testParse() {
        Assertions.assertEquals(Complex.ZERO, Complex.parse(Complex.ZERO.toString()));
        Assertions.assertEquals(Complex.ONE, Complex.parse(Complex.ONE.toString()));
        Assertions.assertEquals(Complex.I, Complex.parse(Complex.I.toString()));
        Assertions.assertEquals(INF, Complex.parse(INF.toString()));
        Assertions.assertEquals(NAN, Complex.parse(NAN.toString()));
        Assertions.assertEquals(oneInf, Complex.parse(oneInf.toString()));
        Assertions.assertEquals(negInfZero, Complex.parse(negInfZero.toString()));
        Assertions.assertEquals(Complex.ofReal(pi), Complex.parse(Complex.ofReal(pi).toString()));
        Assertions.assertEquals(Complex.ofPolar(2, pi), Complex.parse(Complex.ofPolar(2, pi).toString()));
        Assertions.assertEquals(Complex.ofCis(pi), Complex.parse(Complex.ofCis(pi).toString()));
    }

    @Test
    public void testParseWrongStart() {
        final String re = "1.234";
        final String im = "5.678";
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> Complex.parse(re + "," + im + ")")
        );
    }

    @Test
    public void testParseWrongEnd() {
        final String re = "1.234";
        final String im = "5.678";
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> Complex.parse("(" + re + "," + im)
        );
    }

    @Test
    public void testParseMissingSeparator() {
        final String re = "1.234";
        final String im = "5.678";
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> Complex.parse("(" + re + " " + im + ")")
        );
    }

    @Test
    public void testParseInvalidRe() {
        final String re = "I.234";
        final String im = "5.678";
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> Complex.parse("(" + re + "," + im + ")")
        );
    }

    @Test
    public void testParseInvalidIm() {
        final String re = "1.234";
        final String im = "5.G78";
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> Complex.parse("(" + re + "," + im + ")")
        );
    }

    @Test
    public void testParseSpaceAllowedAroundNumbers() {
        final double re = 1.234;
        final double im = 5.678;
        final String str = "(  " + re + "  , " + im + "     )";
        Assertions.assertEquals(Complex.ofCartesian(re, im), Complex.parse(str));
    }

    @Test
    public void testCGrammar() {
        final UniformRandomProvider rng = RandomSource.create(RandomSource.SPLIT_MIX_64);
        for (int i = 0; i < 10; i++) {
            final Complex z = Complex.ofCartesian(rng.nextDouble(), rng.nextDouble());
            Assertions.assertEquals(z.getReal(), z.real(), "real");
            Assertions.assertEquals(z.getImaginary(), z.imag(), "imag");
            Assertions.assertEquals(z.conjugate(), z.conj(), "conj");
            Assertions.assertEquals(z.getArgument(), z.arg(), "arg");
        }
    }

    @Test
    public void testLog10() {
        final double ln10 = Math.log(10);
        final UniformRandomProvider rng = RandomSource.create(RandomSource.SPLIT_MIX_64);
        for (int i = 0; i < 10; i++) {
            final Complex z = Complex.ofCartesian(rng.nextDouble(), rng.nextDouble());
            final Complex lnz = z.log();
            final Complex log10z = z.log10();
            // This is prone to floating-point error so use a delta
            Assertions.assertEquals(lnz.getReal() / ln10, log10z.getReal(), 1e-12, "real");
            // This test should be exact
            final double abs = z.abs();
            Assertions.assertEquals(Math.log10(abs), log10z.getReal(), "real");
            Assertions.assertEquals(lnz.getImaginary(), log10z.getImaginary(), "imag");
        }
    }

    @Test
    @Disabled("Required if not implemented in terms of tanh")
    public void testTan() {
        // Check the conditions on the imaginary component that create special results.
        TestUtils.assertEquals(Complex.ONE, Complex.ofCartesian(0, 25).tan(), 0);
        TestUtils.assertEquals(Complex.ofCartesian(0, -1), Complex.ofCartesian(0, -25).tan(), 0);
    }
}
