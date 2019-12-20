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
import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

/**
 * Edge case tests for the functions defined by the C.99 standard for complex numbers
 * defined in ISO/IEC 9899, Annex G.
 *
 * <p>The test contained here are specifically written to target edge cases of finite valued
 * input values that cause overflow/underflow during the computation.
 *
 * <p>The test data is generated from a known implementation of the standard.
 *
 * @see <a href="http://www.open-std.org/JTC1/SC22/WG14/www/standards">
 *    ISO/IEC 9899 - Programming languages - C</a>
 */
public class ComplexEdgeCaseTest {
    private static final double inf = Double.POSITIVE_INFINITY;
    private static final double nan = Double.NaN;

    /**
     * Assert the operation on the complex number is equal to the expected value.
     *
     * <p>The results are are considered equal if there are no floating-point values between them.
     *
     * @param a Real part.
     * @param b Imaginary part.
     * @param name The operation name.
     * @param operation The operation.
     * @param x Expected real part.
     * @param y Expected imaginary part.
     */
    private static void assertComplex(double a, double b,
            String name, UnaryOperator<Complex> operation,
            double x, double y) {
        assertComplex(a, b, name, operation, x, y, 1);
    }

    /**
     * Assert the operation on the complex number is equal to the expected value.
     *
     * <p>The results are considered equal within the provided units of least
     * precision. The maximum count of numbers allowed between the two values is
     * {@code maxUlps - 1}.
     *
     * @param a Real part.
     * @param b Imaginary part.
     * @param name The operation name.
     * @param operation The operation.
     * @param x Expected real part.
     * @param y Expected imaginary part.
     * @param maxUlps the maximum units of least precision between the two values
     */
    private static void assertComplex(double a, double b,
            String name, UnaryOperator<Complex> operation,
            double x, double y, long maxUlps) {
        final Complex c = Complex.ofCartesian(a, b);
        final Complex e = Complex.ofCartesian(x, y);
        CReferenceTest.assertComplex(c, name, operation, e, maxUlps);
    }

    /**
     * Assert the operation on the complex numbers is equal to the expected value.
     *
     * <p>The results are considered equal if there are no floating-point values between them.
     *
     * @param a Real part of first number.
     * @param b Imaginary part of first number.
     * @param c Real part of second number.
     * @param d Imaginary part of second number.
     * @param name The operation name.
     * @param operation The operation.
     * @param x Expected real part.
     * @param y Expected imaginary part.
     */
    // CHECKSTYLE: stop ParameterNumberCheck
    private static void assertComplex(double a, double b, double c, double d,
            String name, BiFunction<Complex, Complex, Complex> operation,
            double x, double y) {
        assertComplex(a, b, c, d, name, operation, x, y, 1);
    }

    /**
     * Assert the operation on the complex numbers is equal to the expected value.
     *
     * <p>The results are considered equal within the provided units of least
     * precision. The maximum count of numbers allowed between the two values is
     * {@code maxUlps - 1}.
     *
     * @param a Real part of first number.
     * @param b Imaginary part of first number.
     * @param c Real part of second number.
     * @param d Imaginary part of second number.
     * @param name The operation name
     * @param operation the operation
     * @param x Expected real part.
     * @param y Expected imaginary part.
     * @param maxUlps the maximum units of least precision between the two values
     */
    private static void assertComplex(double a, double b, double c, double d,
            String name, BiFunction<Complex, Complex, Complex> operation,
            double x, double y, long maxUlps) {
        final Complex c1 = Complex.ofCartesian(a, b);
        final Complex c2 = Complex.ofCartesian(c, d);
        final Complex e = Complex.ofCartesian(x, y);
        CReferenceTest.assertComplex(c1, c2, name, operation, e, maxUlps);
    }

    @Test
    public void testAcos() {
        // acos(z) = (pi / 2) + i ln(iz + sqrt(1 - z^2))
        final String name = "acos";
        final UnaryOperator<Complex> operation = Complex::acos;

        // Edge cases are when values are big but not infinite and small but not zero.
        // Big and small are set using the limits in atanh.
        // A medium value is used to test outside the range of the CReferenceTest.
        // The results have been generated using g++ -std=c++11 acos.
        // xp1 * xm1 will overflow:
        double huge = Math.sqrt(Double.MAX_VALUE) * 2;
        double big = Math.sqrt(Double.MAX_VALUE) / 8;
        double medium = 100;
        double small = Math.sqrt(Double.MIN_NORMAL) * 4;
        assertComplex(huge, big, name, operation, 0.06241880999595735, -356.27960012801969);
        assertComplex(huge, medium, name, operation, 3.7291703656001039e-153, -356.27765080781188);
        assertComplex(huge, small, name, operation, 2.2250738585072019e-308, -356.27765080781188);
        assertComplex(big, big, name, operation, 0.78539816339744828, -353.85163567585209);
        assertComplex(big, medium, name, operation, 5.9666725849601662e-152, -353.50506208557209);
        assertComplex(big, small, name, operation, 3.560118173611523e-307, -353.50506208557209);
        assertComplex(medium, big, name, operation, 1.5707963267948966, -353.50506208557209);
        assertComplex(medium, medium, name, operation, 0.78541066339744181, -5.6448909570623842);
        assertComplex(medium, small, name, operation, 5.9669709409662999e-156, -5.298292365610485);
        assertComplex(small, big, name, operation, 1.5707963267948966, -353.50506208557209);
        assertComplex(small, medium, name, operation, 1.5707963267948966, -5.2983423656105888);
        assertComplex(small, small, name, operation, 1.5707963267948966, -5.9666725849601654e-154);
        // Additional cases to achieve full coverage
        // xm1 = 0
        assertComplex(1, small, name, operation, 2.4426773395109241e-77, -2.4426773395109241e-77);
        // https://svn.boost.org/trac10/ticket/7290
        assertComplex(1.00000002785941, 5.72464869028403e-200, name, operation, 2.4252018043912224e-196, -0.00023604834149293664);
    }

    // acosh is defined by acos so is not tested

    @Test
    public void testAsin() {
        // asin(z) = -i (ln(iz + sqrt(1 - z^2)))
        final String name = "asin";
        final UnaryOperator<Complex> operation = Complex::asin;

        // This method is essentially the same as acos and the edge cases are the same.
        // The results have been generated using g++ -std=c++11 asin.
        double huge = Math.sqrt(Double.MAX_VALUE) * 2;
        double big = Math.sqrt(Double.MAX_VALUE) / 8;
        double medium = 100;
        double small = Math.sqrt(Double.MIN_NORMAL) * 4;
        assertComplex(huge, big, name, operation, 1.5083775167989393, 356.27960012801969);
        assertComplex(huge, medium, name, operation, 1.5707963267948966, 356.27765080781188);
        assertComplex(huge, small, name, operation, 1.5707963267948966, 356.27765080781188);
        assertComplex(big, big, name, operation, 0.78539816339744828, 353.85163567585209);
        assertComplex(big, medium, name, operation, 1.5707963267948966, 353.50506208557209);
        assertComplex(big, small, name, operation, 1.5707963267948966, 353.50506208557209);
        assertComplex(medium, big, name, operation, 5.9666725849601662e-152, 353.50506208557209);
        assertComplex(medium, medium, name, operation, 0.78538566339745486, 5.6448909570623842);
        assertComplex(medium, small, name, operation, 1.5707963267948966, 5.298292365610485);
        assertComplex(small, big, name, operation, 3.560118173611523e-307, 353.50506208557209);
        assertComplex(small, medium, name, operation, 5.9663742737040751e-156, 5.2983423656105888);
        assertComplex(small, small, name, operation, 5.9666725849601654e-154, 5.9666725849601654e-154);
        // Additional cases to achieve full coverage
        // xm1 = 0
        assertComplex(1, small, name, operation, 1.5707963267948966, 2.4426773395109241e-77);
        // https://svn.boost.org/trac10/ticket/7290
        assertComplex(1.00000002785941, 5.72464869028403e-200, name, operation, 1.5707963267948966, 0.00023604834149293664);
    }

    // asinh is defined by asin so is not tested

    @Test
    public void testAtanh() {
        // atanh(z) = (1/2) ln((1 + z) / (1 - z))
        // Odd function: negative real cases defined by positive real cases
        final String name = "atanh";
        final UnaryOperator<Complex> operation = Complex::atanh;

        // Edge cases are when values are big but not infinite and small but not zero.
        // Big and small are set using the limits in atanh.
        // A medium value is used to test outside the range of the CReferenceTest.
        // It hits an edge case when x is big and y > 1.
        // The results have been generated using g++ -std=c++11 atanh.
        double big = Math.sqrt(Double.MAX_VALUE) / 2;
        double medium = 100;
        double small = Math.sqrt(Double.MIN_NORMAL) * 2;
        assertComplex(big, big, name, operation, 7.4583407312002067e-155, 1.5707963267948966);
        assertComplex(big, medium, name, operation, 1.4916681462400417e-154, 1.5707963267948966);
        assertComplex(big, small, name, operation, 1.4916681462400417e-154, 1.5707963267948966);
        assertComplex(medium, big, name, operation, 2.225073858507202e-306, 1.5707963267948966);
        assertComplex(medium, medium, name, operation, 0.0049999166641667555, 1.5657962434640633);
        assertComplex(medium, small, name, operation, 0.010000333353334761, 1.5707963267948966);
        assertComplex(small, big, name, operation, 0, 1.5707963267948966);
        assertComplex(small, medium, name, operation, 2.9830379886812147e-158, 1.5607966601082315);
        assertComplex(small, small, name, operation, 2.9833362924800827e-154, 2.9833362924800827e-154);
        // Additional cases to achieve full coverage
        assertComplex(inf, big, name, operation, 0, 1.5707963267948966);
        assertComplex(big, inf, name, operation, 0, 1.5707963267948966);
    }

    @Test
    public void testCosh() {
        // cosh(a + b i) = cosh(a)cos(b) + i sinh(a)sin(b)
        // Even function: negative real cases defined by positive real cases
        final String name = "cosh";
        final UnaryOperator<Complex> operation = Complex::cosh;

        // Implementation defers to java.util.Math.
        // Hit edge cases for extreme values.
        double big = Double.MAX_VALUE;
        double medium = 2;
        double small = Double.MIN_NORMAL;
        assertComplex(big, big, name, operation, -inf, inf);
        assertComplex(big, medium, name, operation, -inf, inf);
        assertComplex(big, small, name, operation, inf, inf);
        assertComplex(medium, big, name, operation, -3.7621493762972804, 0.017996317370418576);
        assertComplex(medium, medium, name, operation, -1.5656258353157435, 3.297894836311237);
        assertComplex(medium, small, name, operation, 3.7621956910836314, 8.0700322819551687e-308);
        assertComplex(small, big, name, operation, -0.99998768942655991, 1.1040715888508271e-310);
        assertComplex(small, medium, name, operation, -0.41614683654714241, 2.0232539340376892e-308);
        assertComplex(small, small, name, operation, 1, 0);
    }

    @Test
    public void testSinh() {
        // sinh(a + b i) = sinh(a)cos(b)) + i cosh(a)sin(b)
        // Odd function: negative real cases defined by positive real cases
        final String name = "sinh";
        final UnaryOperator<Complex> operation = Complex::sinh;

        // Implementation defers to java.util.Math.
        // Hit edge cases for extreme values.
        double big = Double.MAX_VALUE;
        double medium = 2;
        double small = Double.MIN_NORMAL;
        assertComplex(big, big, name, operation, -inf, inf);
        assertComplex(big, medium, name, operation, -inf, inf);
        assertComplex(big, small, name, operation, inf, inf);
        assertComplex(medium, big, name, operation, -3.6268157591156114, 0.018667844927220067);
        assertComplex(medium, medium, name, operation, -1.5093064853236158, 3.4209548611170133);
        assertComplex(medium, small, name, operation, 3.626860407847019, 8.3711632828186228e-308);
        assertComplex(small, big, name, operation, -2.2250464665720564e-308, 0.004961954789184062);
        assertComplex(small, medium, name, operation, -9.2595744730151568e-309, 0.90929742682568171);
        assertComplex(small, small, name, operation, 2.2250738585072014e-308, 2.2250738585072014e-308);
    }

    @Test
    public void testTanh() {
        // tan(a + b i) = sinh(2a)/(cosh(2a)+cos(2b)) + i [sin(2b)/(cosh(2a)+cos(2b))]
        // Odd function: negative real cases defined by positive real cases
        final String name = "tanh";
        final UnaryOperator<Complex> operation = Complex::tanh;

        // Overflow on 2b:
        // cos(2b) = cos(inf) = NaN
        // sin(2b) = sin(inf) = NaN
        assertComplex(1, Double.MAX_VALUE, name, operation, 0.76160203106265523, -0.0020838895895863505);

        // Underflow on 2b:
        // cos(2b) -> 1
        // sin(2b) -> 0
        assertComplex(1, Double.MIN_NORMAL, name, operation, 0.76159415595576485, 9.344739287691424e-309);
        assertComplex(1, Double.MIN_VALUE, name, operation, 0.76159415595576485, 0);

        // Overflow on 2a:
        // sinh(2a) = sinh(inf) = inf
        // cosh(2a) = cosh(inf) = inf
        // Test all sign variants as this execution path to treat real as infinite
        // is not tested else where.
        assertComplex(Double.MAX_VALUE, 1, name, operation, 1, 0.0);
        assertComplex(Double.MAX_VALUE, -1, name, operation, 1, -0.0);
        assertComplex(-Double.MAX_VALUE, 1, name, operation, -1, 0.0);
        assertComplex(-Double.MAX_VALUE, -1, name, operation, -1, -0.0);

        // Underflow on 2a:
        // sinh(2a) -> 0
        // cosh(2a) -> 0
        assertComplex(Double.MIN_NORMAL, 1, name, operation, 7.6220323800193346e-308, 1.5574077246549021);
        assertComplex(Double.MIN_VALUE, 1, name, operation, 1.4821969375237396e-323, 1.5574077246549021);
    }

    @Test
    public void testExp() {
        final String name = "exp";
        final UnaryOperator<Complex> operation = Complex::exp;

        // exp(a + b i) = exp(a) (cos(b) + i sin(b))

        // Overflow if exp(a) == inf
        assertComplex(1000, 0, name, operation, inf, 0.0);
        assertComplex(1000, 1, name, operation, inf, inf);
        assertComplex(1000, 2, name, operation, -inf, inf);
        assertComplex(1000, 3, name, operation, -inf, inf);
        assertComplex(1000, 4, name, operation, -inf, -inf);

        // Underflow if exp(a) == 0
        assertComplex(-1000, 0, name, operation, 0.0, 0.0);
        assertComplex(-1000, 1, name, operation, 0.0, 0.0);
        assertComplex(-1000, 2, name, operation, -0.0, 0.0);
        assertComplex(-1000, 3, name, operation, -0.0, 0.0);
        assertComplex(-1000, 4, name, operation, -0.0, -0.0);
    }

    @Test
    public void testLog() {
        final String name = "log";
        final UnaryOperator<Complex> operation = Complex::log;

        // ln(a + b i) = ln(|a + b i|) + i arg(a + b i)
        // |a + b i| = sqrt(a^2 + b^2)
        // arg(a + b i) = Math.atan2(imaginary, real)

        // Overflow if sqrt(a^2 + b^2) == inf.
        // Matlab computes this.
        assertComplex(-Double.MAX_VALUE, Double.MAX_VALUE, name, operation, 7.101292864836639e2, Math.PI * 3 / 4);
        assertComplex(Double.MAX_VALUE, Double.MAX_VALUE, name, operation, 7.101292864836639e2, Math.PI / 4);
        assertComplex(-Double.MAX_VALUE, Double.MAX_VALUE / 4, name, operation, 7.098130252042921e2, 2.896613990462929);
        assertComplex(Double.MAX_VALUE, Double.MAX_VALUE / 4, name, operation, 7.098130252042921e2, 2.449786631268641e-1, 2);

        // Underflow if sqrt(a^2 + b^2) == 0
        assertComplex(-Double.MIN_VALUE, Double.MIN_VALUE, name, operation, -744.44007192138122, 2.3561944901923448);
        assertComplex(Double.MIN_VALUE, Double.MIN_VALUE, name, operation, -744.44007192138122, 0.78539816339744828);
    }

    @Test
    public void testSqrt() {
        final String name = "sqrt";
        final UnaryOperator<Complex> operation = Complex::sqrt;

        // Computed in polar coordinates:
        //   real = (x^2 + y^2)^0.25 * cos(0.5 * atan2(y, x))
        //   imag = (x^2 + y^2)^0.25 * sin(0.5 * atan2(y, x))
        // ---
        // Note:
        // If x is positive and y is +/-0.0 atan2 returns +/-0.
        // If x is negative and y is +/-0.0 atan2 returns +/-PI.
        // This causes problems as
        //   cos(0.5 * PI) = 6.123233995736766e-17
        // assert: Math.cos(Math.acos(0)) != 0.0
        // Thus an unchecked polar computation will produce incorrect output when
        // there is no imaginary component and real is negative.
        // The computation should be done for real only numbers separately.
        // This condition is tested in the reference test against known data
        // so not repeated here.
        // ---

        // Check overflow safe.
        double a = Double.MAX_VALUE;
        final double b = a / 4;
        Assertions.assertEquals(inf, Complex.ofCartesian(a, b).abs(), "Expected overflow");
        // Compute the expected new magnitude by expressing b as a scale factor of a:
        // (x^2 + y^2)^0.25
        // = sqrt(sqrt(a^2 + (b/a)^2 * a^2))
        // = sqrt(sqrt((1+(b/a)^2) * a^2))
        // = sqrt(sqrt((1+(b/a)^2))) * sqrt(a)
        final double newAbs = Math.sqrt(Math.sqrt(1 + (b / a) * (b / a))) * Math.sqrt(a);
        assertComplex(a, b, name, operation, newAbs * Math.cos(0.5 * Math.atan2(b, a)),
                                             newAbs * Math.sin(0.5 * Math.atan2(b, a)), 3);
        assertComplex(b, a, name, operation, newAbs * Math.cos(0.5 * Math.atan2(a, b)),
                                             newAbs * Math.sin(0.5 * Math.atan2(a, b)), 2);

        // In polar coords:
        // real = sqrt(abs()) * Math.cos(arg() / 2)
        // imag = sqrt(abs()) * Math.sin(arg() / 2)
        // This is possible if abs() does not overflow.
        a = Double.MAX_VALUE / 2;
        assertComplex(-a, a, name, operation, 4.3145940638864765e+153, 1.0416351505169177e+154, 2);
        assertComplex(a, a, name, operation, 1.0416351505169177e+154, 4.3145940638864758e+153);
        assertComplex(-a, -a, name, operation, 4.3145940638864765e+153,  -1.0416351505169177e+154, 2);
        assertComplex(a, -a, name, operation, 1.0416351505169177e+154, -4.3145940638864758e+153);

        // Check minimum normal value conditions
        // Computing in Polar coords produces a very different result with
        // MIN_VALUE so use MIN_NORMAL
        a = Double.MIN_NORMAL;
        assertComplex(-a, a, name, operation, 6.7884304867749663e-155, 1.6388720948399111e-154);
        assertComplex(a, a, name, operation, 1.6388720948399111e-154, 6.7884304867749655e-155);
        assertComplex(-a, -a, name, operation, 6.7884304867749663e-155, -1.6388720948399111e-154);
        assertComplex(a, -a, name, operation, 1.6388720948399111e-154, -6.7884304867749655e-155);
    }

    // Note:
    // multiply is tested in CStandardTest
    // divide is tested in CStandardTest

    @Test
    public void testPow() {
        final String name = "pow";
        final BiFunction<Complex, Complex, Complex> operation = Complex::pow;

        // pow(Complex) is log().multiply(Complex).exp()
        // All are overflow safe and handle infinities as defined in the C99 standard.
        // TODO: Test edge cases with:
        // Double.MAX_VALUE, Double.MIN_NORMAL, Inf
        // using other library implementations.

        // Test NaN
        assertComplex(1, 1, nan, nan, name, operation, nan, nan);
        assertComplex(nan, nan, 1, 1, name, operation, nan, nan);
        assertComplex(nan, 1, 1, 1, name, operation, nan, nan);
        assertComplex(1, nan, 1, 1, name, operation, nan, nan);
        assertComplex(1, 1, nan, 1, name, operation, nan, nan);
        assertComplex(1, 1, 1, nan, name, operation, nan, nan);
    }
}
