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

import org.apache.commons.numbers.complex.Complex;
import org.junit.Assert;
import org.junit.Test;

public class CStandardTest {

    private static final double inf = Double.POSITIVE_INFINITY;
    private static final double negInf = Double.NEGATIVE_INFINITY;
    private static final double nan = Double.NaN;
    private static final double piOverFour = Math.PI / 4.0;
    private static final double piOverTwo = Math.PI / 2.0;
    private static final double threePiOverFour = 3.0*Math.PI/4.0;
    private static final Complex oneOne = Complex.ofCartesian(1, 1);
    private static final Complex oneZero = Complex.ofCartesian(1, 0);
    private static final Complex oneInf = Complex.ofCartesian(1, inf);
    private static final Complex oneNaN = Complex.ofCartesian(1, nan);
    private static final Complex zeroInf = Complex.ofCartesian(0, inf);
    private static final Complex zeroNegInf = Complex.ofCartesian(0,negInf);
    private static final Complex zeroNaN = Complex.ofCartesian(0, nan);
    private static final Complex zeroPiTwo = Complex.ofCartesian(0.0, piOverTwo);
    private static final Complex negZeroZero = Complex.ofCartesian(-0.0, 0);
    private static final Complex negI = Complex.ofCartesian(0.0, -1.0);
    private static final Complex infOne = Complex.ofCartesian(inf, 1);
    private static final Complex infZero = Complex.ofCartesian(inf, 0);
    private static final Complex infNaN = Complex.ofCartesian(inf, nan);
    private static final Complex infInf = Complex.ofCartesian(inf, inf);
    private static final Complex infPiTwo = Complex.ofCartesian(inf, piOverTwo);
    private static final Complex infPiFour = Complex.ofCartesian(inf, piOverFour);
    private static final Complex infPi = Complex.ofCartesian(inf, Math.PI);
    private static final Complex negInfInf = Complex.ofCartesian(negInf, inf);
    private static final Complex negInfZero = Complex.ofCartesian(negInf, 0);
    private static final Complex negInfOne = Complex.ofCartesian(negInf, 1);
    private static final Complex negInfNaN = Complex.ofCartesian(negInf, nan);
    private static final Complex negInfPosInf = Complex.ofCartesian(negInf, inf);
    private static final Complex negInfPi = Complex.ofCartesian(negInf, Math.PI);
    private static final Complex nanInf = Complex.ofCartesian(nan, inf);
    private static final Complex nanNegInf = Complex.ofCartesian(nan, negInf);
    private static final Complex nanZero = Complex.ofCartesian(nan, 0);
    private static final Complex nanOne = Complex.ofCartesian(nan, 1);
    private static final Complex piTwoNaN = Complex.ofCartesian(piOverTwo, nan);
    private static final Complex piNegInf = Complex.ofCartesian(Math.PI, negInf);
    private static final Complex piTwoNegInf = Complex.ofCartesian(piOverTwo, negInf);
    private static final Complex piTwoNegZero = Complex.ofCartesian(piOverTwo, -0.0);
    private static final Complex threePiFourNegInf = Complex.ofCartesian(threePiOverFour,negInf);
    private static final Complex piFourNegInf = Complex.ofCartesian(piOverFour, negInf);
    private static final Complex NAN = Complex.ofCartesian(nan, nan);

    public void assertComplex(Complex c1, Complex c2, double realTol, double imagTol) {
        Assert.assertEquals(c1.getReal(), c2.getReal(), realTol);
        Assert.assertEquals(c1.getImaginary(), c2.getImaginary(), imagTol);
    }

    public void assertComplex(Complex c1, Complex c2) {
        Assert.assertEquals(c1.getReal(), c2.getReal(),0.0);
        Assert.assertEquals(c1.getImaginary(), c2.getImaginary(), 0.0);
    }


    /**
     * ISO C Standard G.6.3
     */
    @Test
    public void testSqrt1() {
        Complex z1 = Complex.ofCartesian(-2.0, 0.0);
        Complex z2 = Complex.ofCartesian(0.0, Math.sqrt(2));
        assertComplex(z1.sqrt(), z2);
        z1 = Complex.ofCartesian(-2.0, -0.0);
        z2 = Complex.ofCartesian(0.0, -Math.sqrt(2));
        assertComplex(z1.sqrt(), z2);
    }

    @Test
    public void testImplicitTrig() {
        Complex z1 = Complex.ofReal(3.0);
        Complex z2 = Complex.ofCartesian(0.0, 3.0);
        assertComplex(z1.asin(), negI.multiply(z2.asinh()));
        assertComplex(z1.atan(), negI.multiply(z2.atanh()), Math.ulp(1), Math.ulp(1));
        assertComplex(z1.cos(), z2.cosh());
        assertComplex(z1.sin(), negI.multiply(z2.sinh()));
        assertComplex(z1.tan(), negI.multiply(z2.tanh()));
    }

    /**
     * ISO C Standard G.6.1.1
     */
    @Test
    public void testAcos() {
        assertComplex(oneOne.acos().conj(), oneOne.conj().acos(), Math.ulp(1), Math.ulp(1));
        assertComplex(Complex.ZERO.acos(), piTwoNegZero);
        assertComplex(negZeroZero.acos(), piTwoNegZero);
        assertComplex(zeroNaN.acos(), piTwoNaN);
        assertComplex(oneInf.acos(), piTwoNegInf);
        assertComplex(oneNaN.acos(), NAN);
        assertComplex(negInfOne.acos(), piNegInf);
        assertComplex(infOne.acos(), zeroNegInf);
        assertComplex(negInfPosInf.acos(), threePiFourNegInf);
        assertComplex(infInf.acos(), piFourNegInf);
        assertComplex(infNaN.acos(), nanInf);
        assertComplex(negInfNaN.acos(), nanNegInf);
        assertComplex(nanOne.acos(), NAN);
        assertComplex(nanInf.acos(), nanNegInf);
        assertComplex(NAN.acos(), NAN);
    }

    /**
     * ISO C Standard G.6.2.2
     */
    @Test
    public void testAsinh() {
        // TODO: test for which Asinh is odd
        assertComplex(oneOne.conj().asinh(), oneOne.asinh().conj());
        assertComplex(Complex.ZERO.asinh(), Complex.ZERO);
        assertComplex(oneInf.asinh(), infPiTwo);
        assertComplex(oneNaN.asinh(), NAN);
        assertComplex(infOne.asinh(), infZero);
        assertComplex(infInf.asinh(), infPiFour);
        assertComplex(infNaN.asinh(), infNaN);
        assertComplex(nanZero.asinh(), nanZero);
        assertComplex(nanOne.asinh(), NAN);
        assertComplex(nanInf.asinh(), infNaN);
        assertComplex(NAN, NAN);
    }

    /**
     * ISO C Standard G.6.2.3
     */
    @Test
    public void testAtanh() {
        assertComplex(oneOne.conj().atanh(), oneOne.atanh().conj());
        assertComplex(Complex.ZERO.atanh(), Complex.ZERO);
        assertComplex(zeroNaN.atanh(), zeroNaN);
        assertComplex(oneZero.atanh(), infZero);
        assertComplex(oneInf.atanh(),zeroPiTwo);
        assertComplex(oneNaN.atanh(), NAN);
        assertComplex(infOne.atanh(), zeroPiTwo);
        assertComplex(infInf.atanh(), zeroPiTwo);
        assertComplex(infNaN.atanh(), zeroNaN);
        assertComplex(nanOne.atanh(), NAN);
        assertComplex(nanInf.atanh(), zeroPiTwo);
        assertComplex(NAN.atanh(), NAN);
    }

    /**
     * ISO C Standard G.6.2.4
     */
    @Test
    public void testCosh() {
        assertComplex(oneOne.cosh().conj(), oneOne.conj().cosh());
        assertComplex(Complex.ZERO.cosh(), Complex.ONE);
        assertComplex(zeroInf.cosh(), nanZero);
        assertComplex(zeroNaN.cosh(), nanZero);
        assertComplex(oneInf.cosh(), NAN);
        assertComplex(oneNaN.cosh(), NAN);
        assertComplex(infZero.cosh(), infZero);
        // the next test does not appear to make sense:
        // (inf + iy) = inf + cis(y)
        // skipped
        assertComplex(infInf.cosh(), infNaN);
        assertComplex(infNaN.cosh(), infNaN);
        assertComplex(nanZero.cosh(), nanZero);
        assertComplex(nanOne.cosh(), NAN);
        assertComplex(NAN.cosh(), NAN);
    }

    /**
     * ISO C Standard G.6.2.5
     */
    @Test
    public void testSinh() {
        assertComplex(oneOne.sinh().conj(), oneOne.conj().sinh()); // AND CSINH IS ODD
        assertComplex(Complex.ZERO.sinh(), Complex.ZERO);
        assertComplex(zeroInf.sinh(), zeroNaN);
        assertComplex(zeroNaN.sinh(), zeroNaN);
        assertComplex(oneInf.sinh(), NAN);
        assertComplex(oneNaN.sinh(), NAN);
        assertComplex(infZero.sinh(), infZero);
        // skipped test similar to previous section
        assertComplex(infInf.sinh(), infNaN);
        assertComplex(infNaN.sinh(), infNaN);
        assertComplex(nanZero.sinh(), nanZero);
        assertComplex(nanOne.sinh(), NAN);
        assertComplex(NAN.sinh(), NAN);
    }

    /**
     * ISO C Standard G.6.2.6
     */
    @Test
    public void testTanh() {
        assertComplex(oneOne.tanh().conj(), oneOne.conj().tanh()); // AND CSINH IS ODD
        assertComplex(Complex.ZERO.tanh(), Complex.ZERO);
        assertComplex(oneInf.tanh(), NAN);
        assertComplex(oneNaN.tanh(), NAN);
        //Do Not Understand the Next Test
        assertComplex(infInf.tanh(), oneZero);
        assertComplex(infNaN.tanh(), oneZero);
        assertComplex(nanZero.tanh(), nanZero);
        assertComplex(nanOne.tanh(), NAN);
        assertComplex(NAN.tanh(), NAN);
    }

    /**
     * ISO C Standard G.6.3.1
     */
    @Test
    public void testExp() {
        assertComplex(oneOne.conj().exp(), oneOne.exp().conj());
        assertComplex(Complex.ZERO.exp(), oneZero);
        assertComplex(negZeroZero.exp(), oneZero);
        assertComplex(oneInf.exp(), NAN);
        assertComplex(oneNaN.exp(), NAN);
        assertComplex(infZero.exp(), infZero);
        // Do not understand next test
        assertComplex(negInfInf.exp(), Complex.ZERO);
        assertComplex(infInf.exp(), infNaN);
        assertComplex(negInfNaN.exp(), Complex.ZERO);
        assertComplex(infNaN.exp(), infNaN);
        assertComplex(nanZero.exp(), nanZero);
        assertComplex(nanOne.exp(), NAN);
        assertComplex(NAN.exp(), NAN);
    }

    /**
     * ISO C Standard G.6.3.2
     */
    @Test
    public void testLog() {
        assertComplex(oneOne.log().conj(), oneOne.conj().log());
        assertComplex(negZeroZero.log(), negInfPi);
        assertComplex(Complex.ZERO.log(), negInfZero);
        assertComplex(oneInf.log(), infPiTwo);
        assertComplex(oneNaN.log(), NAN);
        assertComplex(negInfOne.log(), infPi);
        assertComplex(infOne.log(), infZero);
        assertComplex(infInf.log(), infPiFour);
        assertComplex(infNaN.log(), infNaN);
        assertComplex(nanOne.log(), NAN);
        assertComplex(nanInf.log(), infNaN);
        assertComplex(NAN.log(), NAN);
    }

    /**
     * ISO C Standard G.6.4.2
     */
    @Test
    public void testSqrt2() {
        assertComplex(oneOne.sqrt().conj(), oneOne.conj().sqrt());
        assertComplex(Complex.ZERO.sqrt(), Complex.ZERO);
        assertComplex(oneInf.sqrt(), infInf);
        assertComplex(negInfOne.sqrt(), zeroNaN);
        assertComplex(infOne.sqrt(), infZero);
        assertComplex(negInfNaN.sqrt(), nanInf);
        assertComplex(infNaN.sqrt(), infNaN);
        assertComplex(nanOne.sqrt(), NAN);
        assertComplex(NAN.sqrt(), NAN);
    }
}
