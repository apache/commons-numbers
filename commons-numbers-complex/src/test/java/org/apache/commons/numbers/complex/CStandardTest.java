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
import org.apache.commons.numbers.complex.ComplexUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class CStandardTest {

    private double inf = Double.POSITIVE_INFINITY;
    private double neginf = Double.NEGATIVE_INFINITY;
    private double nan = Double.NaN;
    private double pi = Math.PI;
    private double piOverFour = Math.PI / 4.0;
    private double piOverTwo = Math.PI / 2.0;
    private double threePiOverFour = 3.0*Math.PI/4.0
    private Complex oneInf = new Complex(1, inf);
    private Complex oneNegInf = new Complex(1, neginf);
    private Complex infOne = new Complex(inf, 1);
    private Complex infZero = new Complex(inf, 0);
    private Complex infNaN = new Complex(inf, nan);
    private Complex infNegInf = new Complex(inf, neginf);
    private Complex infInf = new Complex(inf, inf);
    private Complex negInfInf = new Complex(neginf, inf);
    private Complex negInfZero = new Complex(neginf, 0);
    private Complex negInfOne = new Complex(neginf, 1);
    private Complex negInfNaN = new Complex(neginf, nan);
    private Complex negInfNegInf = new Complex(neginf, neginf);
    private Complex oneNaN = new Complex(1, nan);
    private Complex zeroInf = new Complex(0, inf);
    private Complex zeroNaN = new Complex(0, nan);
    private Complex nanInf = new Complex(nan, inf);
    private Complex nanNegInf = new Complex(nan, neginf);
    private Complex nanZero = new Complex(nan, 0);
    private Complex negZeroZero = new Complex(-0.0, 0);
    private Complex negZeroNan = new Complex(-0.0, nan);
    private Complex negI = new Complex(0.0, -1.0);
    private Complex zeroPiTwo = new Complex(0.0, piOverTwo);
    private Complex piTwoNaN = new Complex(piOverTwo, nan);
    private Complex piNegInf = new Complex(Math.PI, negInf);
    private Complex piTwoNegInf = new Complex(piOverTwo, negInf);
    private Complex negInfPosInf = new Complex(negInf, inf);
    private Complex piTwoNegZero = new Complex(piOverTwo, -0.0);
    private Complex threePiFourNegInf = new Complex(threePiOverFour,negInf);
    private Complex piFourNegInf = new Complex(piOverFour, negInf);
    private Complex infPiTwo = new Complex(inf, piOverTwo);
    private Complex infPiFour = new Complex(inf, piOverFour);
    private Complex negInfPi = new Complex(negInf, Math.PI);
    /**
     * ISO C Standard G.6.3
     */
    @Test
    public void testSqrt() {
        Complex z1 = new Complex(-2.0, 0.0);
        Complex z2 = new Complex(0.0, Math.sqrt(2));
        Assert.assertEquals(z1.sqrt(), z2);
        z1 = new Complex(-2.0, -0.0);
        z2 = new Complex(0.0, -Math.sqrt(2));
        Assert.assertEquals(z1.sqrt(), z2);
    }

    @Test
    public void testImplicitTrig() {
        Complex z1 = new Complex(3.0);
        Complex z2 = new Complex(0.0, 3.0); 
        Assert.assertEquals(z1.asin(), negI.multiply(z2.asinh()));
        Assert.assertEquals(z1.atan(), negI.multiply(z2.atanh()));
        Assert.assertEquals(z1.cos(), z2.cosh());
        Assert.assertEquals(z1.sin(), negI.multiply(z2.sinh()));
        Assert.assertEquals(z1.tan(), negI.multiply(z1.tanh()));
    }

    /**
     * ISO C Standard G.6.1.1
     */
    @Test
    public void testAcos() {
        Assert.assertEquals(oneOne.acos().conj(), oneOne.conj().acos());
        Assert.assertEquals(Complex.ZERO.acos(), piTwoNegZero);
        Assert.assertEquals(negZeroZero.acos(), piTwoNegZero);
        Assert.assertEquals(zeroNaN.acos(), piTwoNaN);
        Assert.assertEquals(oneInf.acos(), piTwoNegInf);
        Assert.assertEquals(oneNaN.acos(), Complex.NaN);
        Assert.assertEquals(negInfOne.acos(), piNegInf);
        Assert.assertEquals(infOne.acos(), zeroInf);
        Assert.assertEquals(negInfPosInf.acos(), threePiFourNegInf);
        Assert.assertEquals(infInf.acos(), piFourNegInf);
        Assert.assertEquals(infNaN.acos(), naNInf);
        Assert.assertEquals(negInfNan.acos(), nanNegInf);
        Assert.assertEquals(nanOne.acos(), Complex.NaN);
        Assert.assertEquals(nanInf.acos(), nanNegInf);
        Assert.assertEquals(Complex.NaN.acos(), Complex.NaN);
    }

    /**
     * ISO C Standard G.6.2.2
     */
    @Test
    public void testAsinh() {
        // TODO: test for which Asinh is odd
        Assert.assertEquals(oneOne.conj().asinh(), oneOne.asinh().conj());
        Assert.assertEquals(Complex.ZERO.asinh(), Complex.ZERO);
        Assert.assertEquals(oneInf.asinh(), infPiTwo);
        Assert.assertEquals(oneNaN.asinh(), Complex.NaN);
        Assert.assertEquals(infOne.asinh(), infZero);
        Assert.assertEquals(infInf.asinh(), infPiFour);
        Assert.assertEquals(infNaN.asinh(), z1);
        Assert.assertEquals(nanZero.asinh(), nanZero);
        Assert.assertEquals(nanOne.asinh(), Complex.NaN);
        Assert.assertEquals(nanInf.asinh(), infNan);
        Assert.assertEquals(Complex.NaN, Complex.NaN);
    }

    /**
     * ISO C Standard G.6.2.3
     */
    @Test
    public void testAtanh() {
        Assert.assertEquals(oneOne.conj().atanh(), oneOne.atanh().conj());
        Assert.assertEquals(Complex.ZERO.atanh(), Complex.ZERO);
        Assert.assertEquals(zeroNaN.atanh(), zeroNaN);
        Assert.assertEquals(oneZero.atanh(), infZero);
        Assert.assertEquals(oneInf.atanh(),zeroPiTwo);
        Assert.assertEquals(oneNaN.atanh(), Complex.NaN);
        Assert.assertEquals(infOne.atanh(), zeroPiTwo);
        Assert.assertEquals(infInf.atanh(), zeroPiTwo);
        Assert.assertEquals(infNaN.atanh(), zeroNaN);
        Assert.assertEquals(nanOne.atanh(), Complex.NaN);
        Assert.assertEquals(nanInf.atanh(), zeroPiTwo);
        Assert.assertEquals(Complex.NaN.atanh(), Complex.NaN);
    }

    /**
     * ISO C Standard G.6.2.4
     */
    @Test
    public void testCosh() {
        Assert.assertEquals(oneOne.cosh().conj(), oneOne.conj().cosh());
        Assert.assertEquals(Complex.ZERO.cosh(), Complex.ONE);
        Assert.assertEquals(zeroInf.cosh(), nanZero);
        Assert.assertEquals(zeroNan.cosh(), nanZero);
        Assert.assertEquals(oneInf.cosh(), Complex.NaN);
        Assert.assertEquals(oneNan.cosh(), Complex.NaN);
        Assert.assertEquals(infZero.cosh(), infZero);
        // the next test does not appear to make sense:
        // (inf + iy) = inf + cis(y)
        // skipped
        Assert.assertEquals(infInf.cosh(), infNaN);
        Assert.assertEquals(infNaN.cosh(), infNaN);
        Assert.assertEquals(nanZero.cosh(), nanZero);
        Assert.assertEquals(nanOne.cosh(), Complex.NaN);
        Assert.assertEquals(Complex.NaN.cosh(), Complex.NaN);
    }

    /**
     * ISO C Standard G.6.2.5
     */
    @Test
    public void testSinh() {
        Assert.assertEquals(oneOne.sinh().conj(), oneOne.conj().sinh()); // AND CSINH IS ODD
        Assert.assertEquals(Complex.ZERO.sinh(), Complex.ZERO);
        Assert.assertEquals(zeroInf.sinh(), zeroNaN);
        Assert.assertEquals(zeroNaN.sinh(), zeroNaN);
        Assert.assertEquals(oneInf.sinh(), Complex.NaN);
        Assert.assertEquals(oneNaN.sinh(), Complex.NaN);
        Assert.assertEquals(infZero.sinh(), infZero);
        // skipped test similar to previous section
        Assert.assertEquals(infInf.sinh(), infNaN);
        Assert.assertEquals(infNaN.sinh(), infNaN);
        Assert.assertEquals(nanZero.sinh(), nanZero);
        Assert.assertEquals(nanOne.sinh(), Complex.NaN);
        Assert.assertEquals(Complex.NaN.sinh(), Complex.NaN);
    }

    /**
     * ISO C Standard G.6.2.6
     */
    @Test
    public void testTanh() {
        Assert.assertEquals(oneOne.tanh().conj(), oneOne.conj().tanh()); // AND CSINH IS ODD
        Assert.assertEquals(Complex.ZERO.tanh(), Complex.ZERO);
        Assert.assertEquals(oneInf.tanh(), Complex.NaN);
        Assert.assertEquals(oneNaN.tanh(), Complex.NaN);
        //Do Not Understand the Next Test
        Assert.assertEquals(infInf.tanh(), oneZero);
        Assert.assertEquals(infNaN.tanh(), oneZero);
        Assert.assertEquals(nanZero.tanh(), nanZero);
        Assert.assertEquals(nanOne.tanh(), Complex.NaN);
        Assert.assertEquals(Complex.NaN.tanh(), Complex.NaN);
    }

    /**
     * ISO C Standard G.6.3.1
     */
    @Test
    public void testExp() {
        Assert.assertEquals(oneOne.conj().exp(), oneOne.exp().conj());
        Assert.assertEquals(Complex.ZERO.exp(), oneZero);
        Assert.assertEquals(negZero.exp(), oneZero);
        Assert.assertEquals(oneInf.exp(), Complex.NaN);
        Assert.assertEquals(oneNaN.exp(), Complex.NaN);
        Assert.assertEquals(infZero.exp(), infZero);
        // Do not understand next test
        Assert.assertEquals(negInfInf.exp(), Complex.ZERO);
        Assert.assertEquals(infInf.exp(), infNaN);
        Assert.assertEquals(negInfNaN.exp(), Complex.ZERO);
        Assert.assertEquals(infNaN.exp(), infNaN);
        Assert.assertEquals(nanZero.exp(), nanZero);
        Assert.assertEquals(nanOne.exp(), Complex.NaN);
        Assert.assertEquals(Complex.NaN.exp(), Complex.NaN);
    }

    /**
     * ISO C Standard G.6.3.2
     */
    @Test
    public void testLog() {
        Assert.assertEquals(oneOne.log().conj(), oneOne.conj().log());
        Assert.assertEquals(negZeroZero.log(), negInfPi); 
        Assert.assertEquals(Complex.ZERO.log(), negInfZero);
        Assert.assertEquals(oneInf.log(), infPiTwo);
        Assert.assertEquals(oneNaN.log(), Complex.NaN);
        Assert.assertEquals(negInfOne.log(), infPi);
        Assert.assertEquals(infOne.log(), infZero);
        Assert.assertEquals(infInf.log(), infPiFour);
        Assert.assertEquals(infNaN.log(), infNaN);
        Assert.assertEquals(nanOne.log(), Complex.NaN);
        Assert.assertEquals(nanInf.log(), infNaN);
        Assert.assertEquals(Complex.NaN.log(), Complex.NaN);
    }

    /**
     * ISO C Standard G.6.4.2
     */
    @Test
    public void testSqrt() {
        Assert.assertEquals(oneOne.sqrt().conj(), oneOne.conj(), sqrt());
        Assert.assertEquals(Complex.ZERO.sqrt(), Complex.ZERO);
        Assert.assertEquals(oneInf.sqrt(), infInf);
        Assert.assertEquals(negInfOne.sqrt(), zeroNaN);
        Assert.assertEquals(infOne.sqrt(), infZero);
        Assert.assertEquals(negInfNaN.sqrt(), nanInf);
        Assert.assertEquals(infNaN.sqrt(), infNaN);
        Assert.assertEquals(nanOne.sqrt(), Complex.NaN);
        Assert.assertEquals(Complex.NaN.sqrt(), Complex.NaN);
    }
}
