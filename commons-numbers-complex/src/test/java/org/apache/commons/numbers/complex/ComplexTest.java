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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


/**
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
    private static final Complex infNaN = Complex.ofCartesian(inf, nan);
    private static final Complex infNegInf = Complex.ofCartesian(inf, neginf);
    private static final Complex infInf = Complex.ofCartesian(inf, inf);
    private static final Complex negInfInf = Complex.ofCartesian(neginf, inf);
    private static final Complex negInfZero = Complex.ofCartesian(neginf, 0);
    private static final Complex negInfOne = Complex.ofCartesian(neginf, 1);
    private static final Complex negInfNaN = Complex.ofCartesian(neginf, nan);
    private static final Complex negInfNegInf = Complex.ofCartesian(neginf, neginf);
    private static final Complex oneNaN = Complex.ofCartesian(1, nan);
    private static final Complex zeroInf = Complex.ofCartesian(0, inf);
    private static final Complex zeroNaN = Complex.ofCartesian(0, nan);
    private static final Complex nanInf = Complex.ofCartesian(nan, inf);
    private static final Complex nanNegInf = Complex.ofCartesian(nan, neginf);
    private static final Complex nanZero = Complex.ofCartesian(nan, 0);
    private static final Complex NAN = Complex.ofCartesian(nan, nan);
    private static final Complex INF = Complex.ofCartesian(inf, inf);

    @Test
    public void testConstructor() {
        Complex z = Complex.ofCartesian(3.0, 4.0);
        Assertions.assertEquals(3.0, z.getReal(), 1.0e-5);
        Assertions.assertEquals(4.0, z.getImaginary(), 1.0e-5);
    }

    @Test
    public void testConstructorNaN() {
        Complex z = Complex.ofCartesian(3.0, Double.NaN);
        Assertions.assertTrue(z.isNaN());

        z = Complex.ofCartesian(nan, 4.0);
        Assertions.assertTrue(z.isNaN());

        z = Complex.ofCartesian(3.0, 4.0);
        Assertions.assertFalse(z.isNaN());
    }

    @Test
    public void testAbs() {
        Complex z = Complex.ofCartesian(3.0, 4.0);
        Assertions.assertEquals(5.0, z.abs(), 1.0e-5);
    }

    @Test
    public void testAbsNaN() {
        Assertions.assertTrue(Double.isNaN(NAN.abs()));
        Complex z = Complex.ofCartesian(inf, nan);
        Assertions.assertTrue(Double.isNaN(z.abs()));
    }

    @Test
    public void testAdd() {
        Complex x = Complex.ofCartesian(3.0, 4.0);
        Complex y = Complex.ofCartesian(5.0, 6.0);
        Complex z = x.add(y);
        Assertions.assertEquals(8.0, z.getReal(), 1.0e-5);
        Assertions.assertEquals(10.0, z.getImaginary(), 1.0e-5);
    }

    @Test
    public void testAddInf() {
        Complex x = Complex.ofCartesian(1, 1);
        Complex z = Complex.ofCartesian(inf, 0);
        Complex w = x.add(z);
        Assertions.assertEquals(1, w.getImaginary(), 0);
        Assertions.assertEquals(inf, w.getReal(), 0);

        x = Complex.ofCartesian(neginf, 0);
        Assertions.assertTrue(Double.isNaN(x.add(z).getReal()));
    }


    @Test
    public void testScalarAdd() {
        Complex x = Complex.ofCartesian(3.0, 4.0);
        double yDouble = 2.0;
        Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.add(yComplex), x.add(yDouble));
    }

    @Test
    public void testScalarAddNaN() {
        Complex x = Complex.ofCartesian(3.0, 4.0);
        double yDouble = Double.NaN;
        Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.add(yComplex), x.add(yDouble));
    }

    @Test
    public void testScalarAddInf() {
        Complex x = Complex.ofCartesian(1, 1);
        double yDouble = Double.POSITIVE_INFINITY;

        Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.add(yComplex), x.add(yDouble));

        x = Complex.ofCartesian(neginf, 0);
        Assertions.assertEquals(x.add(yComplex), x.add(yDouble));
    }

    @Test
    public void testConjugate() {
        Complex x = Complex.ofCartesian(3.0, 4.0);
        Complex z = x.conjugate();
        Assertions.assertEquals(3.0, z.getReal(), 1.0e-5);
        Assertions.assertEquals(-4.0, z.getImaginary(), 1.0e-5);
    }

    @Test
    public void testConjugateNaN() {
        Complex z = NAN.conjugate();
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
        Complex x = Complex.ofCartesian(3.0, 4.0);
        Complex y = Complex.ofCartesian(5.0, 6.0);
        Complex z = x.divide(y);
        Assertions.assertEquals(39.0 / 61.0, z.getReal(), 1.0e-5);
        Assertions.assertEquals(2.0 / 61.0, z.getImaginary(), 1.0e-5);
    }

    @Test
    public void testDivideReal() {
        Complex x = Complex.ofCartesian(2d, 3d);
        Complex y = Complex.ofCartesian(2d, 0d);
        Assertions.assertEquals(Complex.ofCartesian(1d, 1.5), x.divide(y));

    }

    @Test
    public void testDivideImaginary() {
        Complex x = Complex.ofCartesian(2d, 3d);
        Complex y = Complex.ofCartesian(0d, 2d);
        Assertions.assertEquals(Complex.ofCartesian(1.5d, -1d), x.divide(y));
    }

    @Test
    public void testDivideZero() {
        Complex x = Complex.ofCartesian(3.0, 4.0);
        Complex z = x.divide(Complex.ZERO);
        Assertions.assertEquals(INF, z);
    }

    @Test
    public void testDivideZeroZero() {
        Complex x = Complex.ofCartesian(0.0, 0.0);
        Complex z = x.divide(Complex.ZERO);
        Assertions.assertEquals(NAN, z);
    }

    @Test
    public void testDivideNaN() {
        Complex x = Complex.ofCartesian(3.0, 4.0);
        Complex z = x.divide(NAN);
        Assertions.assertTrue(z.isNaN());
    }

    @Test
    public void testDivideNaNInf() {
       Complex z = oneInf.divide(Complex.ONE);
       Assertions.assertTrue(Double.isNaN(z.getReal()));
       Assertions.assertEquals(inf, z.getImaginary(), 0);

       z = negInfNegInf.divide(oneNaN);
       Assertions.assertTrue(Double.isNaN(z.getReal()));
       Assertions.assertTrue(Double.isNaN(z.getImaginary()));

       z = negInfInf.divide(Complex.ONE);
       Assertions.assertTrue(Double.isInfinite(z.getReal()));
       Assertions.assertTrue(Double.isInfinite(z.getImaginary()));
    }

    @Test
    public void testScalarDivide() {
        Complex x = Complex.ofCartesian(3.0, 4.0);
        double yDouble = 2.0;
        Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.divide(yComplex), x.divide(yDouble));
    }

    @Test
    public void testScalarDivideNaN() {
        Complex x = Complex.ofCartesian(3.0, 4.0);
        double yDouble = Double.NaN;
        Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.divide(yComplex), x.divide(yDouble));
    }

    @Test
    public void testScalarDivideZero() {
        Complex x = Complex.ofCartesian(1,1);
        TestUtils.assertEquals(x.divide(Complex.ZERO), x.divide(0), 0);
    }

    @Test
    public void testReciprocal() {
        Complex z = Complex.ofCartesian(5.0, 6.0);
        Complex act = z.reciprocal();
        double expRe = 5.0 / 61.0;
        double expIm = -6.0 / 61.0;
        Assertions.assertEquals(expRe, act.getReal(), Math.ulp(expRe));
        Assertions.assertEquals(expIm, act.getImaginary(), Math.ulp(expIm));
    }

    @Test
    public void testReciprocalReciprocal() {
        Complex z = Complex.ofCartesian(5.0, 6.0);
        Complex zRR = z.reciprocal().reciprocal();
        final double tol = 1e-14;
        Assertions.assertEquals(zRR.getReal(), z.getReal(), tol);
        Assertions.assertEquals(zRR.getImaginary(), z.getImaginary(), tol);
    }

    @Test
    public void testReciprocalReal() {
        Complex z = Complex.ofCartesian(-2.0, 0.0);
        Assertions.assertTrue(Complex.equals(Complex.ofCartesian(-0.5, 0.0), z.reciprocal()));
    }

    @Test
    public void testReciprocalImaginary() {
        Complex z = Complex.ofCartesian(0.0, -2.0);
        Assertions.assertEquals(Complex.ofCartesian(0.0, 0.5), z.reciprocal());
    }

    @Test
    public void testReciprocalNaN() {
        Assertions.assertTrue(NAN.reciprocal().isNaN());
    }

    @Test
    public void testMultiply() {
        Complex x = Complex.ofCartesian(3.0, 4.0);
        Complex y = Complex.ofCartesian(5.0, 6.0);
        Complex z = x.multiply(y);
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
        Complex x = Complex.ofCartesian(3.0, 4.0);
        double yDouble = 2.0;
        Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.multiply(yComplex), x.multiply(yDouble));
        int zInt = -5;
        Complex zComplex = Complex.ofReal(zInt);
        Assertions.assertEquals(x.multiply(zComplex), x.multiply(zInt));
    }

    @Test
    public void testScalarMultiplyNaN() {
        Complex x = Complex.ofCartesian(3.0, 4.0);
        double yDouble = Double.NaN;
        Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.multiply(yComplex), x.multiply(yDouble));
    }

    @Test
    public void testScalarMultiplyInf() {
        Complex x = Complex.ofCartesian(1, 1);
        double yDouble = Double.POSITIVE_INFINITY;
        Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.multiply(yComplex), x.multiply(yDouble));

        yDouble = Double.NEGATIVE_INFINITY;
        yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.multiply(yComplex), x.multiply(yDouble));
    }

    @Test
    public void testNegate() {
        Complex x = Complex.ofCartesian(3.0, 4.0);
        Complex z = x.negate();
        Assertions.assertEquals(-3.0, z.getReal(), 1.0e-5);
        Assertions.assertEquals(-4.0, z.getImaginary(), 1.0e-5);
    }

    @Test
    public void testNegateNaN() {
        Complex z = NAN.negate();
        Assertions.assertTrue(z.isNaN());
    }

    @Test
    public void testSubtract() {
        Complex x = Complex.ofCartesian(3.0, 4.0);
        Complex y = Complex.ofCartesian(5.0, 6.0);
        Complex z = x.subtract(y);
        Assertions.assertEquals(-2.0, z.getReal(), 1.0e-5);
        Assertions.assertEquals(-2.0, z.getImaginary(), 1.0e-5);
    }

    @Test
    public void testSubtractInf() {
        Complex x = Complex.ofCartesian(1, 1);
        Complex z = Complex.ofCartesian(neginf, 0);
        Complex w = x.subtract(z);
        Assertions.assertEquals(1, w.getImaginary(), 0);
        Assertions.assertEquals(inf, w.getReal(), 0);

        x = Complex.ofCartesian(neginf, 0);
        Assertions.assertTrue(Double.isNaN(x.subtract(z).getReal()));
    }

    @Test
    public void testScalarSubtract() {
        Complex x = Complex.ofCartesian(3.0, 4.0);
        double yDouble = 2.0;
        Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.subtract(yComplex), x.subtract(yDouble));
    }

    @Test
    public void testScalarSubtractNaN() {
        Complex x = Complex.ofCartesian(3.0, 4.0);
        double yDouble = Double.NaN;
        Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.subtract(yComplex), x.subtract(yDouble));
    }

    @Test
    public void testScalarSubtractInf() {
        Complex x = Complex.ofCartesian(1, 1);
        double yDouble = Double.POSITIVE_INFINITY;
        Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.subtract(yComplex), x.subtract(yDouble));

        x = Complex.ofCartesian(neginf, 0);
        Assertions.assertEquals(x.subtract(yComplex), x.subtract(yDouble));
    }


    @Test
    public void testEqualsNull() {
        Complex x = Complex.ofCartesian(3.0, 4.0);
        Assertions.assertNotEquals(null, x);
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
    public void testEqualsClass() {
        Complex x = Complex.ofCartesian(3.0, 4.0);
        Assertions.assertFalse(x.equals(this));
    }

    @Test
    public void testEqualsSame() {
        Complex x = Complex.ofCartesian(3.0, 4.0);
        Assertions.assertEquals(x, x);
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
    public void testFloatingPointEqualsWithRelativeToleranceNaN() {
        final Complex x = Complex.ofCartesian(0, Double.NaN);
        final Complex y = Complex.ofCartesian(Double.NaN, 0);
        Assertions.assertFalse(Complex.equalsWithRelativeTolerance(x, Complex.ZERO, 0.1));
        Assertions.assertFalse(Complex.equalsWithRelativeTolerance(x, x, 0.1));
        Assertions.assertFalse(Complex.equalsWithRelativeTolerance(x, y, 0.1));
    }

    @Test
    public void testEqualsTrue() {
        Complex x = Complex.ofCartesian(3.0, 4.0);
        Complex y = Complex.ofCartesian(3.0, 4.0);
        Assertions.assertEquals(x, y);
    }

    @Test
    public void testEqualsRealDifference() {
        Complex x = Complex.ofCartesian(0.0, 0.0);
        Complex y = Complex.ofCartesian(0.0 + Double.MIN_VALUE, 0.0);
        Assertions.assertNotEquals(x, y);
    }

    @Test
    public void testEqualsImaginaryDifference() {
        Complex x = Complex.ofCartesian(0.0, 0.0);
        Complex y = Complex.ofCartesian(0.0, 0.0 + Double.MIN_VALUE);
        Assertions.assertNotEquals(x, y);
    }

    @Test
    public void testHashCode() {
        Complex x = Complex.ofCartesian(0.0, 0.0);
        Complex y = Complex.ofCartesian(0.0, 0.0 + Double.MIN_VALUE);
        Assertions.assertNotEquals(x.hashCode(), y.hashCode());
        y = Complex.ofCartesian(0.0 + Double.MIN_VALUE, 0.0);
        Assertions.assertNotEquals(x.hashCode(), y.hashCode());
        Complex realNaN = Complex.ofCartesian(Double.NaN, 0.0);
        Complex imaginaryNaN = Complex.ofCartesian(0.0, Double.NaN);
        Assertions.assertEquals(realNaN.hashCode(), imaginaryNaN.hashCode());
        Assertions.assertEquals(imaginaryNaN.hashCode(), NAN.hashCode());

        // MATH-1118
        // "equals" and "hashCode" must be compatible: if two objects have
        // different hash codes, "equals" must return false.
        final String msg = "'equals' not compatible with 'hashCode'";

        x = Complex.ofCartesian(0.0, 0.0);
        y = Complex.ofCartesian(0.0, -0.0);
        Assertions.assertTrue(x.hashCode() != y.hashCode());
        Assertions.assertNotEquals(x, y, msg);

        x = Complex.ofCartesian(0.0, 0.0);
        y = Complex.ofCartesian(-0.0, 0.0);
        Assertions.assertTrue(x.hashCode() != y.hashCode());
        Assertions.assertNotEquals(x, y, msg);
    }

    @Test
    @Disabled
    public void testJava() {// TODO more debug
        System.out.println(">>testJava()");
        // MathTest#testExpSpecialCases() checks the following:
        // Assert.assertEquals("exp of -infinity should be 0.0", 0.0, Math.exp(Double.NEGATIVE_INFINITY), Precision.EPSILON);
        // Let's check how well Math works:
        System.out.println("Math.exp="+Math.exp(Double.NEGATIVE_INFINITY));
        String props[] = {
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
        for(String t : props) {
            System.out.println(t + "=" + System.getProperty(t));
        }
        System.out.println("<<testJava()");
    }


    @Test
    public void testScalarPow() {
        Complex x = Complex.ofCartesian(3, 4);
        double yDouble = 5.0;
        Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.pow(yComplex), x.pow(yDouble));
    }

    @Test
    public void testScalarPowNaNBase() {
        Complex x = NAN;
        double yDouble = 5.0;
        Complex yComplex = Complex.ofReal(yDouble);
        Assertions.assertEquals(x.pow(yComplex), x.pow(yDouble));
    }

    @Test
    public void testScalarPowNaNExponent() {
        Complex x = Complex.ofCartesian(3, 4);
        double yDouble = Double.NaN;
        Complex yComplex = Complex.ofReal(yDouble);
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
                Complex z = Complex.ofPolar(r, theta);
                Complex sqrtz = Complex.ofPolar(Math.sqrt(r), theta / 2);
                TestUtils.assertEquals(sqrtz, z.sqrt(), tol);
            }
        }
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
        Complex z = Complex.ofCartesian(-2,2);
        // The List holding all third roots
        Complex[] thirdRootsOfZ = z.nthRoot(3).toArray(new Complex[0]);
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
        Complex z = Complex.ofCartesian(5,-2);
        // The List holding all fourth roots
        Complex[] fourthRootsOfZ = z.nthRoot(4).toArray(new Complex[0]);
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
        Complex z = Complex.ofCartesian(8,0);
        // The List holding all third roots
        Complex[] thirdRootsOfZ = z.nthRoot(3).toArray(new Complex[0]);
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
        Complex z = Complex.ofCartesian(0,2);
        // The List holding all third roots
        Complex[] thirdRootsOfZ = z.nthRoot(3).toArray(new Complex[0]);
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
        Complex z = Complex.ofCartesian(1, 0);
        // The List holding all fourth roots
        Complex[] fourthRootsOfZ = z.nthRoot(4).toArray(new Complex[0]);
        // test z_0
        Assertions.assertEquals(1,     fourthRootsOfZ[0].getReal(),      1.0e-5);
        Assertions.assertEquals(0,   fourthRootsOfZ[0].getImaginary(), 1.0e-5);
//         test z_1
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
//         test z_1
        Assertions.assertEquals(0,    fourthRootsOfZ[1].getReal(),      1.0e-5);
        Assertions.assertEquals(-1,     fourthRootsOfZ[1].getImaginary(), 1.0e-5);
        // test z_2
        Assertions.assertEquals(-1,    fourthRootsOfZ[2].getReal(),      1.0e-5);
        Assertions.assertEquals(0,    fourthRootsOfZ[2].getImaginary(), 1.0e-5);
        // test z_3
        Assertions.assertEquals(0,   fourthRootsOfZ[3].getReal(),      1.0e-5);
        Assertions.assertEquals(1,    fourthRootsOfZ[3].getImaginary(), 1.0e-5);
    }
    /**
     * Test standard values
     */
    @Test
    public void testGetArgument() {
        Complex z = Complex.ofCartesian(1, 0);
        Assertions.assertEquals(0.0, z.getArgument(), 1.0e-12);

        z = Complex.ofCartesian(1, 1);
        Assertions.assertEquals(Math.PI/4, z.getArgument(), 1.0e-12);

        z = Complex.ofCartesian(0, 1);
        Assertions.assertEquals(Math.PI/2, z.getArgument(), 1.0e-12);

        z = Complex.ofCartesian(-1, 1);
        Assertions.assertEquals(3 * Math.PI/4, z.getArgument(), 1.0e-12);

        z = Complex.ofCartesian(-1, 0);
        Assertions.assertEquals(Math.PI, z.getArgument(), 1.0e-12);

        z = Complex.ofCartesian(-1, -1);
        Assertions.assertEquals(-3 * Math.PI/4, z.getArgument(), 1.0e-12);

        z = Complex.ofCartesian(0, -1);
        Assertions.assertEquals(-Math.PI/2, z.getArgument(), 1.0e-12);

        z = Complex.ofCartesian(1, -1);
        Assertions.assertEquals(-Math.PI/4, z.getArgument(), 1.0e-12);

    }

    /**
     * Verify atan2-style handling of infinite parts
     */
    @Test
    public void testGetArgumentInf() {
        Assertions.assertEquals(Math.PI/4, infInf.getArgument(), 1.0e-12);
        Assertions.assertEquals(Math.PI/2, oneInf.getArgument(), 1.0e-12);
        Assertions.assertEquals(0.0, infOne.getArgument(), 1.0e-12);
        Assertions.assertEquals(Math.PI/2, zeroInf.getArgument(), 1.0e-12);
        Assertions.assertEquals(0.0, infZero.getArgument(), 1.0e-12);
        Assertions.assertEquals(Math.PI, negInfOne.getArgument(), 1.0e-12);
        Assertions.assertEquals(-3.0*Math.PI/4, negInfNegInf.getArgument(), 1.0e-12);
        Assertions.assertEquals(-Math.PI/2, oneNegInf.getArgument(), 1.0e-12);
    }

    /**
     * Verify that either part NaN results in NaN
     */
    @Test
    public void testGetArgumentNaN() {
        Assertions.assertTrue(Double.isNaN(nanZero.getArgument()));
        Assertions.assertTrue(Double.isNaN(zeroNaN.getArgument()));
        Assertions.assertTrue(Double.isNaN(NAN.getArgument()));
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
                () -> Complex.parse( re + "," + im + ")")
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
}
