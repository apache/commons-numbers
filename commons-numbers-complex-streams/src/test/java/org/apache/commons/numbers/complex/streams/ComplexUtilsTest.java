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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComplexUtils}.
 */
class ComplexUtilsTest {

    private static final double inf = Double.POSITIVE_INFINITY;
    private static final double negInf = Double.NEGATIVE_INFINITY;
    private static final double nan = Double.NaN;
    private static final double pi = Math.PI;

    private static final Complex negInfInf = Complex.ofCartesian(negInf, inf);
    private static final Complex infNegInf = Complex.ofCartesian(inf, negInf);
    private static final Complex infInf = Complex.ofCartesian(inf, inf);
    private static final Complex negInfNegInf = Complex.ofCartesian(negInf, negInf);
    private static final Complex infNaN = Complex.ofCartesian(inf, nan);
    private static final Complex NAN = Complex.ofCartesian(nan, nan);

    private static Complex[] c; // complex array with real values even and imag
    // values odd
    private static Complex[] cr; // complex array with real values consecutive
    private static Complex[] ci; // complex array with imag values consecutive
    private static double[] d; // real array with consecutive vals
    private static double[] di; // interleaved real array with consecutive vals,
    // 'interleaved' length
    private static float[] f; // real array with consecutive vals
    private static float[] fi; // interleaved real array with consecutive vals, interleaved
    // length
    private static double[] sr; // real component of split array, evens
    private static double[] si; // imag component of split array, odds
    private static float[] sfr; // real component of split array, float, evens
    private static float[] sfi; // imag component of split array, float, odds
    private static String msg; // error message for AssertEquals
    // CHECKSTYLE: stop MultipleVariableDeclarations
    private static Complex[][] c2d, cr2d, ci2d; // for 2d methods
    private static Complex[][][] c3d, cr3d, ci3d; // for 3d methods
    private static Complex[][][][] c4d, cr4d, ci4d; // for 3d methods
    private static double[][] di2d0, di2d1, sr2d, si2d;
    private static double[][][] di3d0, di3d1, di3d2, sr3d, si3d;
    private static double[][][][] di4d0, di4d1, di4d2, di4d3, sr4d, si4d;
    private static float[][] fi2d0, fi2d1, sfr2d, sfi2d;
    private static float[][][] fi3d0, fi3d1, fi3d2, sfr3d, sfi3d;
    private static float[][][][] sfr4d, sfi4d;
    // CHECKSTYLE: resume MultipleVariableDeclarations

    // CHECKSTYLE: stop MethodLength
    private static void setArrays() { // initial setup method
        c = new Complex[10];
        cr = new Complex[10];
        ci = new Complex[10];
        d = new double[10];
        f = new float[10];
        di = new double[20];
        fi = new float[20];
        sr = new double[10];
        si = new double[10];
        sfr = new float[10];
        sfi = new float[10];
        c2d = new Complex[10][10];
        cr2d = new Complex[10][10];
        ci2d = new Complex[10][10];
        c3d = new Complex[10][10][10];
        cr3d = new Complex[10][10][10];
        ci3d = new Complex[10][10][10];
        c4d = new Complex[10][10][10][10];
        cr4d = new Complex[10][10][10][10];
        ci4d = new Complex[10][10][10][10];
        sr2d = new double[10][10];
        sr3d = new double[10][10][10];
        sr4d = new double[10][10][10][10];
        si2d = new double[10][10];
        si3d = new double[10][10][10];
        si4d = new double[10][10][10][10];
        sfr2d = new float[10][10];
        sfr3d = new float[10][10][10];
        sfr4d = new float[10][10][10][10];
        sfi2d = new float[10][10];
        sfi3d = new float[10][10][10];
        sfi4d = new float[10][10][10][10];
        di2d0 = new double[20][10];
        di2d1 = new double[10][20];
        di3d0 = new double[20][10][10];
        di3d1 = new double[10][20][10];
        di3d2 = new double[10][10][20];
        di4d0 = new double[20][10][10][10];
        di4d1 = new double[10][20][10][10];
        di4d2 = new double[10][10][20][10];
        di4d3 = new double[10][10][10][20];
        fi2d0 = new float[20][10];
        fi2d1 = new float[10][20];
        fi3d0 = new float[20][10][10];
        fi3d1 = new float[10][20][10];
        fi3d2 = new float[10][10][20];
        for (int i = 0; i < 20; i += 2) {
            int halfI = i / 2;

            // Complex arrays
            c[halfI] = Complex.ofCartesian(i, i + 1);
            cr[halfI] = Complex.ofCartesian(halfI, 0);
            ci[halfI] = Complex.ofCartesian(0, halfI);

            // standalone - split equivalent to c
            sr[halfI] = i;
            si[halfI] = i + 1;
            sfr[halfI] = i;
            sfi[halfI] = i + 1;

            // depending on method used equivalents to cr or ci
            d[halfI] = halfI;
            f[halfI] = halfI;

            // interleaved - all equivalent to c2d
            di[i] = i;
            di[i + 1] = i + 1;
            fi[i] = i;
            fi[i + 1] = i + 1;
        }
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 20; j += 2) {
                int halfJ = j / 2;
                int real = 10 * i + j;
                int imaginary = 10 * i + j + 1;

                // Complex arrays
                c2d[i][halfJ] = Complex.ofCartesian(real, imaginary);
                cr2d[i][halfJ] = Complex.ofCartesian(real, 0);
                ci2d[i][halfJ] = Complex.ofCartesian(0, imaginary);

                // standalone - split equivalent to c2d, standalone equivalent to cr2d or ci2d
                sr2d[i][halfJ] = real;
                si2d[i][halfJ] = imaginary;
                sfr2d[i][halfJ] = real;
                sfi2d[i][halfJ] = imaginary;

                // interleaved - all equivalent to c2d
                di2d0[j][i] = 10 * halfJ + 2 * i;
                di2d0[j + 1][i] = di2d0[j][i] + 1;
                di2d1[i][j] = real;
                di2d1[i][j + 1] = imaginary;
                fi2d0[j][i] = 10 * halfJ + 2 * i;
                fi2d0[j + 1][i] = fi2d0[j][i] + 1;
                fi2d1[i][j] = real;
                fi2d1[i][j + 1] = imaginary;
            }
        }
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                for (int k = 0; k < 20; k += 2) {
                    int halfK = k / 2;
                    int real = 100 * i + 10 * j + k;
                    int imaginary = 100 * i + 10 * j + k + 1;

                    // Complex arrays
                    c3d[i][j][halfK] = Complex.ofCartesian(real, imaginary);
                    cr3d[i][j][halfK] = Complex.ofCartesian(real, 0);
                    ci3d[i][j][halfK] = Complex.ofCartesian(0, imaginary);

                    // standalone - split equivalent to c3d, standalone equivalent to cr3d or ci3d
                    sr3d[i][j][halfK] = real;
                    si3d[i][j][halfK] = imaginary;
                    sfr3d[i][j][halfK] = real;
                    sfi3d[i][j][halfK] = imaginary;

                    // interleaved - all equivalent to c3d
                    di3d0[k][i][j] = 100 * halfK + 10 * i + 2 * j;
                    di3d0[k + 1][i][j] = di3d0[k][i][j] + 1;
                    di3d1[j][k][i] = 100 * j + 10 * halfK + 2 * i;
                    di3d1[j][k + 1][i] = di3d1[j][k][i] + 1;
                    di3d2[i][j][k] = real;
                    di3d2[i][j][k + 1] = imaginary;
                    fi3d0[k][i][j] = 100 * halfK + 10 * i + 2 * j;
                    fi3d0[k + 1][i][j] = fi3d0[k][i][j] + 1;
                    fi3d1[j][k][i] = 100 * j + 10 * halfK + 2 * i;
                    fi3d1[j][k + 1][i] = fi3d1[j][k][i] + 1;
                    fi3d2[i][j][k] = real;
                    fi3d2[i][j][k + 1] = imaginary;
                }
            }
        }
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                for (int k = 0; k < 10; k++) {
                    for (int l = 0; l < 20; l += 2) {
                        int halfL = l / 2;
                        int real = 1000 * i + 100 * j + 10 * k + l;
                        int imaginary = 1000 * i + 100 * j + 10 * k + l + 1;

                        // Complex arrays
                        c4d[i][j][k][halfL] = Complex.ofCartesian(real, imaginary);
                        cr4d[i][j][k][halfL] = Complex.ofCartesian(real, 0);
                        ci4d[i][j][k][halfL] = Complex.ofCartesian(0, imaginary);

                        // standalone - split equivalent to c4d, standalone equivalent to cr4d or ci4d
                        sr4d[i][j][k][halfL] = real;
                        si4d[i][j][k][halfL] = imaginary;
                        sfr4d[i][j][k][halfL] = real;
                        sfi4d[i][j][k][halfL] = imaginary;

                        // interleaved - all equivalent to c4d
                        di4d0[l][i][j][k] = 1000 * halfL + 100 * i + 10 * j + 2 * k;
                        di4d0[l + 1][i][j][k] = di4d0[l][i][j][k] + 1;
                        di4d1[k][l][i][j] = 1000 * k + 100 * halfL + 10 * i + 2 * j;
                        di4d1[k][l + 1][i][j] = di4d1[k][l][i][j] + 1;
                        di4d2[j][k][l][i] = 1000 * j + 100 * k + 10 * halfL + 2 * i;
                        di4d2[j][k][l + 1][i] = di4d2[j][k][l][i] + 1;
                        di4d3[i][j][k][l] = real;
                        di4d3[i][j][k][l + 1] = imaginary;
                    }
                }
            }
        }
        msg = "";
    }
    // CHECKSTYLE: resume MethodLength

    @Test
    void testPolar2Complex() {
        TestUtils.assertEquals(Complex.ONE, ComplexUtils.polar2Complex(1, 0), 10e-12);
        TestUtils.assertEquals(Complex.ZERO, ComplexUtils.polar2Complex(0, 1), 10e-12);
        TestUtils.assertEquals(Complex.ZERO, ComplexUtils.polar2Complex(0, -1), 10e-12);
        TestUtils.assertEquals(Complex.I, ComplexUtils.polar2Complex(1, pi / 2), 10e-12);
        TestUtils.assertEquals(Complex.I.negate(), ComplexUtils.polar2Complex(1, -pi / 2), 10e-12);
        double r = 0;
        for (int i = 0; i < 5; i++) {
            r += i;
            double theta = 0;
            for (int j = 0; j < 20; j++) {
                theta += pi / 6;
                TestUtils.assertEquals(altPolar(r, theta), ComplexUtils.polar2Complex(r, theta), 10e-12);
            }
            theta = -2 * pi;
            for (int j = 0; j < 20; j++) {
                theta -= pi / 6;
                TestUtils.assertEquals(altPolar(r, theta), ComplexUtils.polar2Complex(r, theta), 10e-12);
            }
        }
        // 1D
        double[] r1D = new double[11];
        double[] theta1D = new double[11];
        for (int i = 0; i < 11; i++) {
            r1D[i] = i;
        }
        theta1D[5] = 0;
        for (int i = 1; i < 5; i++) {
            theta1D[5 + i] = theta1D[5 + i - 1] + pi / 6;
            theta1D[5 - i] = theta1D[5 + i + 1] - pi / 6;
        }
        Complex[] observed1D = ComplexUtils.polar2Complex(r1D, theta1D);
        Assertions.assertEquals(r1D.length, observed1D.length);
        for (int i = 0; i < r1D.length; i++) {
            Assertions.assertEquals(ComplexUtils.polar2Complex(r1D[i], theta1D[i]), observed1D[i]);
        }

        // 2D
        double[][] theta2D = new double[3][4];
        double[][] r2D = new double[3][4];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                r2D[i][j] = i + j;
                theta2D[i][j] = i * j;
            }
        }
        Complex[][] observed2D = ComplexUtils.polar2Complex(r2D, theta2D);
        Assertions.assertEquals(r2D.length, observed2D.length);
        for (int i = 0; i < r2D.length; i++) {
            TestUtils.assertSame(msg, ComplexUtils.polar2Complex(r2D[i], theta2D[i]), observed2D[i]);
        }

        // 3D
        double[][][] theta3D = new double[3][4][3];
        double[][][] r3D = new double[3][4][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 3; k++) {
                    r3D[i][j][k] = i + j + k;
                    theta3D[i][j][k] = i * j * k;
                }
            }
        }
        Complex[][][] observed3D = ComplexUtils.polar2Complex(r3D, theta3D);
        Assertions.assertEquals(r3D.length, observed3D.length);
        for (int i = 0; i < r3D.length; i++) {
            TestUtils.assertSame(msg, ComplexUtils.polar2Complex(r3D[i], theta3D[i]), observed3D[i]);
        }
    }

    private Complex altPolar(double r, double theta) {
        return Complex.I.multiply(Complex.ofCartesian(theta, 0)).exp().multiply(Complex.ofCartesian(r, 0));
    }

    @Test
    void testPolar2ComplexIllegalModulus() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ComplexUtils.polar2Complex(-1, 0)
        );
    }

    @Test
    void testPolar2ComplexIllegalModulus1D() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ComplexUtils.polar2Complex(new double[]{0, -1, 2}, new double[]{0, 1, 2})
        );
    }

    @Test
    void testPolar2ComplexIllegalModulus2D() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ComplexUtils.polar2Complex(new double[][]{{0, 2, 2}, {0, -1, 2}}, new double[][]{{0, 1, 2}, {0, 1, 2}})
        );
    }

    @Test
    void testPolar2ComplexIllegalModulus3D() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ComplexUtils.polar2Complex(new double[][][]{{{0, 2, 2}}, {{0, -1, 2}}}, new double[][][]{{{0, 1, 2}}, {{0, 1, 2}}})
        );
    }

    @Test
    void testPolar2ComplexNaN() {
        TestUtils.assertSame(NAN, ComplexUtils.polar2Complex(nan, 1));
        TestUtils.assertSame(NAN, ComplexUtils.polar2Complex(1, nan));
        TestUtils.assertSame(NAN, ComplexUtils.polar2Complex(nan, nan));
    }

    @Test
    void testPolar2ComplexInf() {
        TestUtils.assertSame(NAN, ComplexUtils.polar2Complex(1, inf));
        TestUtils.assertSame(NAN, ComplexUtils.polar2Complex(1, negInf));
        TestUtils.assertSame(NAN, ComplexUtils.polar2Complex(inf, inf));
        TestUtils.assertSame(NAN, ComplexUtils.polar2Complex(inf, negInf));
        TestUtils.assertSame(infInf, ComplexUtils.polar2Complex(inf, pi / 4));
        TestUtils.assertSame(infNaN, ComplexUtils.polar2Complex(inf, 0));
        TestUtils.assertSame(infNegInf, ComplexUtils.polar2Complex(inf, -pi / 4));
        TestUtils.assertSame(negInfInf, ComplexUtils.polar2Complex(inf, 3 * pi / 4));
        TestUtils.assertSame(negInfNegInf, ComplexUtils.polar2Complex(inf, 5 * pi / 4));
    }

    @Test
    void testCExtract() {
        final double[] real = new double[] {negInf, -123.45, 0, 1, 234.56, pi, inf};
        final Complex[] complex = ComplexUtils.real2Complex(real);

        for (int i = 0; i < real.length; i++) {
            Assertions.assertEquals(real[i], complex[i].getReal());
        }
    }

    // EXTRACTION METHODS

    @Test
    void testExtractionMethods() {
        setArrays();
        // Extract complex from real double array, index 3
        TestUtils.assertSame(Complex.ofCartesian(3, 0), ComplexUtils.extractComplexFromRealArray(d, 3));
        // Extract complex from real float array, index 3
        TestUtils.assertSame(Complex.ofCartesian(3, 0), ComplexUtils.extractComplexFromRealArray(f, 3));
        // Extract complex from real double array, index 3
        TestUtils.assertSame(Complex.ofCartesian(0, 3), ComplexUtils.extractComplexFromImaginaryArray(d, 3));
        // Extract complex from real float array, index 3
        TestUtils.assertSame(Complex.ofCartesian(0, 3), ComplexUtils.extractComplexFromImaginaryArray(f, 3));
        // Extract real double from complex array, index 3
        Assertions.assertEquals(6, ComplexUtils.extractRealFromComplexArray(c, 3));
        // Extract real float from complex array, index 3
        Assertions.assertEquals(6, ComplexUtils.extractRealFloatFromComplexArray(c, 3));
        // Extract real double from complex array, index 3
        Assertions.assertEquals(7, ComplexUtils.extractImaginaryFromComplexArray(c, 3));
        // Extract real float from complex array, index 3
        Assertions.assertEquals(7, ComplexUtils.extractImaginaryFloatFromComplexArray(c, 3));
        // Extract complex from interleaved double array, index 3
        TestUtils.assertSame(Complex.ofCartesian(6, 7), ComplexUtils.extractComplexFromInterleavedArray(d, 3));
        // Extract interleaved double array from complex array, index 3
        Assertions.assertArrayEquals(new double[]{6d, 7d}, ComplexUtils.extractInterleavedFromComplexArray(c, 3));
        // Extract interleaved float array from complex array, index 3
        Assertions.assertArrayEquals(new float[]{6f, 7f}, ComplexUtils.extractInterleavedFloatFromComplexArray(c, 3));
        // Extract complex from interleaved float array, index 3
        TestUtils.assertSame(Complex.ofCartesian(6, 7), ComplexUtils.extractComplexFromInterleavedArray(f, 3));
        // Extract interleaved double from complex array, index 3
        Assertions.assertArrayEquals(new double[] {6, 7}, ComplexUtils.extractInterleavedFromComplexArray(c, 3),
                Math.ulp(1), msg);
        // Extract interleaved float from complex array, index 3
        Assertions.assertArrayEquals(new double[] {6, 7}, ComplexUtils.extractInterleavedFromComplexArray(c, 3),
                Math.ulp(1), msg);
    }
    // REAL <-> COMPLEX

    @Test
    void testRealToComplex() {
        setArrays();
        // Real double to complex, whole array
        TestUtils.assertEquals(msg, cr, ComplexUtils.real2Complex(d), Math.ulp(1.0));
        // Real float to complex, whole array
        TestUtils.assertEquals(msg, cr, ComplexUtils.real2Complex(f), Math.ulp(1.0));

        // 2d
        TestUtils.assertEquals(msg, cr2d, ComplexUtils.real2Complex(sr2d), 0);
        TestUtils.assertEquals(msg, cr2d, ComplexUtils.real2Complex(sfr2d), 0);

        // 3d
        TestUtils.assertEquals(msg, cr3d, ComplexUtils.real2Complex(sr3d), 0);
        TestUtils.assertEquals(msg, cr3d, ComplexUtils.real2Complex(sfr3d), 0);

        // 4d
        TestUtils.assertEquals(msg, cr4d, ComplexUtils.real2Complex(sr4d), 0);
    }

    @Test
    void testComplexToReal() {
        setArrays();
        // Real complex to double, whole array
        Assertions.assertArrayEquals(sr, ComplexUtils.complex2Real(c), Math.ulp(1.0), msg);
        // Real complex to float, whole array
        Assertions.assertArrayEquals(sfr, ComplexUtils.complex2RealFloat(c), Math.ulp(1.0f), msg);

        // 2d
        TestUtils.assertEquals(msg, sr2d, ComplexUtils.complex2Real(cr2d), 0);
        TestUtils.assertEquals(msg, sfr2d, ComplexUtils.complex2RealFloat(cr2d), 0);

        // 3d
        TestUtils.assertEquals(msg, sr3d, ComplexUtils.complex2Real(cr3d), 0);
        TestUtils.assertEquals(msg, sfr3d, ComplexUtils.complex2RealFloat(cr3d), 0);

        // 4d
        TestUtils.assertEquals(msg, sr4d, ComplexUtils.complex2Real(cr4d), 0);
        TestUtils.assertEquals(msg, sfr4d, ComplexUtils.complex2RealFloat(cr4d), 0);
    }

    // IMAGINARY <-> COMPLEX

    @Test
    void testImaginaryToComplex() {
        setArrays();
        // Imaginary double to complex, whole array
        TestUtils.assertEquals(msg, ci, ComplexUtils.imaginary2Complex(d), Math.ulp(1.0));
        // Imaginary float to complex, whole array
        TestUtils.assertEquals(msg, ci, ComplexUtils.imaginary2Complex(f), Math.ulp(1.0));

        // 2d
        TestUtils.assertEquals(msg, ci2d, ComplexUtils.imaginary2Complex(si2d), 0);

        // 3d
        TestUtils.assertEquals(msg, ci3d, ComplexUtils.imaginary2Complex(si3d), 0);

        // 4d
        TestUtils.assertEquals(msg, ci4d, ComplexUtils.imaginary2Complex(si4d), 0);
    }

    @Test
    void testComplexToImaginary() {
        setArrays();
        // Imaginary complex to double, whole array
        Assertions.assertArrayEquals(si, ComplexUtils.complex2Imaginary(c), Math.ulp(1.0), msg);
        // Imaginary complex to float, whole array
        Assertions.assertArrayEquals(sfi, ComplexUtils.complex2ImaginaryFloat(c), Math.ulp(1.0f), msg);

        // 2d
        TestUtils.assertEquals(msg, si2d, ComplexUtils.complex2Imaginary(ci2d), 0);
        TestUtils.assertEquals(msg, sfi2d, ComplexUtils.complex2ImaginaryFloat(ci2d), 0);

        // 3d
        TestUtils.assertEquals(msg, si3d, ComplexUtils.complex2Imaginary(ci3d), 0);
        TestUtils.assertEquals(msg, sfi3d, ComplexUtils.complex2ImaginaryFloat(ci3d), 0);

        // 4d
        TestUtils.assertEquals(msg, si4d, ComplexUtils.complex2Imaginary(ci4d), 0);
        TestUtils.assertEquals(msg, sfi4d, ComplexUtils.complex2ImaginaryFloat(ci4d), 0);
    }

    // INTERLEAVED <-> COMPLEX

    @Test
    void testComplex2InterleavedIllegalIndex2Dmin() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ComplexUtils.complex2Interleaved(c2d, -1)
        );
    }

    @Test
    void testComplex2InterleavedIllegalIndex2Dmax() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ComplexUtils.complex2Interleaved(c2d, 2)
        );
    }

    @Test
    void testComplex2InterleavedIllegalIndex3Dmin() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ComplexUtils.complex2Interleaved(c3d, -1)
        );
    }

    @Test
    void testComplex2InterleavedIllegalIndex3Dmax() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ComplexUtils.complex2Interleaved(c3d, 3)
        );
    }

    @Test
    void testComplex2InterleavedIllegalIndex4Dmin() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ComplexUtils.complex2Interleaved(c4d, -1)
        );
    }

    @Test
    void testComplex2InterleavedIllegalIndex4Dmax() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ComplexUtils.complex2Interleaved(c4d, 4)
        );
    }

    @Test
    void testComplex2InterleavedFloatIllegalIndex2Dmin() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ComplexUtils.complex2InterleavedFloat(c2d, -1)
        );
    }

    @Test
    void testComplex2InterleavedFloatIllegalIndex2Dmax() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ComplexUtils.complex2InterleavedFloat(c2d, 2)
        );
    }

    @Test
    void testComplex2InterleavedFloatIllegalIndex3Dmin() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ComplexUtils.complex2InterleavedFloat(c3d, -1)
        );
    }

    @Test
    void testComplex2InterleavedFloatIllegalIndex3Dmax() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ComplexUtils.complex2InterleavedFloat(c3d, 3)
        );
    }

    @Test
    void testInterleaved2ComplexIllegalIndex2Dmin() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ComplexUtils.interleaved2Complex(di2d0, -1)
        );
    }

    @Test
    void testInterleaved2ComplexIllegalIndex2Dmax() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ComplexUtils.interleaved2Complex(di2d0, 2)
        );
    }

    @Test
    void testInterleaved2ComplexIllegalIndex3Dmin() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ComplexUtils.interleaved2Complex(di3d0, -1)
        );
    }

    @Test
    void testInterleaved2ComplexIllegalIndex3Dmax() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ComplexUtils.interleaved2Complex(di3d0, 3)
        );
    }

    @Test
    void testInterleaved2ComplexIllegalIndex4Dmin() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ComplexUtils.interleaved2Complex(di4d0, -1)
        );
    }

    @Test
    void testInterleaved2ComplexIllegalIndex4Dmax() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ComplexUtils.interleaved2Complex(di4d0, 4)
        );
    }

    @Test
    void testInterleaved2ComplexFloatIllegalIndex2Dmin() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ComplexUtils.interleaved2Complex(fi2d0, -1)
        );
    }

    @Test
    void testInterleaved2ComplexFloatIllegalIndex2Dmax() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ComplexUtils.interleaved2Complex(fi2d0, 2)
        );
    }

    @Test
    void testInterleaved2ComplexFloatIllegalIndex3Dmin() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ComplexUtils.interleaved2Complex(fi3d0, -1)
        );
    }

    @Test
    void testInterleaved2ComplexFloatIllegalIndex3Dmax() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ComplexUtils.interleaved2Complex(fi3d0, 3)
        );
    }

    @Test
    void testInterleavedToComplex() {
        setArrays();
        // Interleaved double to complex, whole array
        TestUtils.assertEquals(msg, c, ComplexUtils.interleaved2Complex(di), Math.ulp(1.0));
        // Interleaved float to complex, whole array
        TestUtils.assertEquals(msg, c, ComplexUtils.interleaved2Complex(fi), Math.ulp(1.0));

        // 2d
        TestUtils.assertSame(msg, c2d, ComplexUtils.interleaved2Complex(di2d0, 0));
        TestUtils.assertSame(msg, c2d, ComplexUtils.interleaved2Complex(di2d1, 1));
        TestUtils.assertSame(msg, c2d, ComplexUtils.interleaved2Complex(di2d1));

        TestUtils.assertSame(msg, c2d, ComplexUtils.interleaved2Complex(fi2d0, 0));
        TestUtils.assertSame(msg, c2d, ComplexUtils.interleaved2Complex(fi2d1, 1));
        TestUtils.assertSame(msg, c2d, ComplexUtils.interleaved2Complex(fi2d1));

        // 3d
        TestUtils.assertSame(msg, c3d, ComplexUtils.interleaved2Complex(di3d0, 0));
        TestUtils.assertSame(msg, c3d, ComplexUtils.interleaved2Complex(di3d1, 1));
        TestUtils.assertSame(msg, c3d, ComplexUtils.interleaved2Complex(di3d2, 2));
        TestUtils.assertSame(msg, c3d, ComplexUtils.interleaved2Complex(di3d2));

        TestUtils.assertSame(msg, c3d, ComplexUtils.interleaved2Complex(fi3d0, 0));
        TestUtils.assertSame(msg, c3d, ComplexUtils.interleaved2Complex(fi3d1, 1));
        TestUtils.assertSame(msg, c3d, ComplexUtils.interleaved2Complex(fi3d2, 2));
        TestUtils.assertSame(msg, c3d, ComplexUtils.interleaved2Complex(fi3d2));

        // 4d
        TestUtils.assertSame(msg, c4d, ComplexUtils.interleaved2Complex(di4d0, 0));
        TestUtils.assertSame(msg, c4d, ComplexUtils.interleaved2Complex(di4d1, 1));
        TestUtils.assertSame(msg, c4d, ComplexUtils.interleaved2Complex(di4d2, 2));
        TestUtils.assertSame(msg, c4d, ComplexUtils.interleaved2Complex(di4d3, 3));
    }

    @Test
    void testComplexToInterleaved() {
        setArrays();
        Assertions.assertArrayEquals(di, ComplexUtils.complex2Interleaved(c), Math.ulp(1.0), msg);
        // Interleaved complex to float, whole array
        Assertions.assertArrayEquals(fi, ComplexUtils.complex2InterleavedFloat(c), Math.ulp(1.0f), msg);

        // 2d
        TestUtils.assertEquals(msg, di2d0, ComplexUtils.complex2Interleaved(c2d, 0), 0);
        TestUtils.assertEquals(msg, di2d1, ComplexUtils.complex2Interleaved(c2d, 1), 0);
        TestUtils.assertEquals(msg, di2d1, ComplexUtils.complex2Interleaved(c2d), 0);

        TestUtils.assertEquals(msg, fi2d0, ComplexUtils.complex2InterleavedFloat(c2d, 0), 0);
        TestUtils.assertEquals(msg, fi2d1, ComplexUtils.complex2InterleavedFloat(c2d, 1), 0);
        TestUtils.assertEquals(msg, fi2d1, ComplexUtils.complex2InterleavedFloat(c2d), 0);

        // 3d
        TestUtils.assertEquals(msg, di3d0, ComplexUtils.complex2Interleaved(c3d, 0), 0);
        TestUtils.assertEquals(msg, di3d1, ComplexUtils.complex2Interleaved(c3d, 1), 0);
        TestUtils.assertEquals(msg, di3d2, ComplexUtils.complex2Interleaved(c3d, 2), 0);
        TestUtils.assertEquals(msg, di3d2, ComplexUtils.complex2Interleaved(c3d), 0);

        TestUtils.assertEquals(msg, fi3d0, ComplexUtils.complex2InterleavedFloat(c3d, 0), 0);
        TestUtils.assertEquals(msg, fi3d1, ComplexUtils.complex2InterleavedFloat(c3d, 1), 0);
        TestUtils.assertEquals(msg, fi3d2, ComplexUtils.complex2InterleavedFloat(c3d, 2), 0);
        TestUtils.assertEquals(msg, fi3d2, ComplexUtils.complex2InterleavedFloat(c3d), 0);

        // 4d
        TestUtils.assertEquals(msg, di4d0, ComplexUtils.complex2Interleaved(c4d, 0), 0);
        TestUtils.assertEquals(msg, di4d1, ComplexUtils.complex2Interleaved(c4d, 1), 0);
        TestUtils.assertEquals(msg, di4d2, ComplexUtils.complex2Interleaved(c4d, 2), 0);
        TestUtils.assertEquals(msg, di4d3, ComplexUtils.complex2Interleaved(c4d, 3), 0);
        TestUtils.assertEquals(msg, di4d3, ComplexUtils.complex2Interleaved(c4d), 0);
    }

    // SPLIT TO COMPLEX
    @Test
    void testSplit2Complex() {
        setArrays();
        // Split double to complex, whole array
        TestUtils.assertEquals(msg, c, ComplexUtils.split2Complex(sr, si), Math.ulp(1.0));

        TestUtils.assertSame(msg, c2d, ComplexUtils.split2Complex(sr2d, si2d));
        TestUtils.assertSame(msg, c3d, ComplexUtils.split2Complex(sr3d, si3d));
        TestUtils.assertSame(msg, c4d, ComplexUtils.split2Complex(sr4d, si4d));
        TestUtils.assertSame(msg, c2d, ComplexUtils.split2Complex(sfr2d, sfi2d));
        TestUtils.assertSame(msg, c3d, ComplexUtils.split2Complex(sfr3d, sfi3d));
    }

    // INITIALIZATION METHODS

    @Test
    void testInitialize() {
        Complex[] complexes = new Complex[10];
        ComplexUtils.initialize(complexes);
        for (Complex cc : complexes) {
            TestUtils.assertEquals(Complex.ofCartesian(0, 0), cc, Math.ulp(0));
        }
    }

    @Test
    void testInitialize2d() {
        Complex[][] complexes = new Complex[10][10];
        ComplexUtils.initialize(complexes);
        for (Complex[] c1 : complexes) {
            for (Complex c0 : c1) {
                TestUtils.assertEquals(Complex.ofCartesian(0, 0), c0, Math.ulp(0));
            }
        }
    }

    @Test
    void testInitialize3d() {
        Complex[][][] complexes = new Complex[10][10][10];
        ComplexUtils.initialize(complexes);
        for (Complex[][] c2 : complexes) {
            for (Complex[] c1 : c2) {
                for (Complex c0 : c1) {
                    TestUtils.assertEquals(Complex.ofCartesian(0, 0), c0, Math.ulp(0));
                }
            }
        }
    }

    @Test
    void testAbs() {
        setArrays();
        double[] observed = ComplexUtils.abs(c);
        Assertions.assertEquals(c.length, observed.length);
        for (int i = 0; i < c.length; i++) {
            Assertions.assertEquals(c[i].abs(), observed[i]);
        }
    }

    @Test
    void testArg() {
        setArrays();
        double[] observed = ComplexUtils.arg(c);
        Assertions.assertEquals(c.length, observed.length);
        for (int i = 0; i < c.length; i++) {
            Assertions.assertEquals(c[i].arg(), observed[i]);
        }
    }
}
