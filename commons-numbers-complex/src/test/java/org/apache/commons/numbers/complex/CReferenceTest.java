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
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Tests the functions defined by the C.99 standard for complex numbers
 * defined in ISO/IEC 9899, Annex G.
 *
 * <p>The test data is generated from a known implementation of the standard: GNU g++.
 *
 * @see <a href="http://www.open-std.org/JTC1/SC22/WG14/www/standards">
 *    ISO/IEC 9899 - Programming languages - C</a>
 */
public class CReferenceTest {
    private static final double inf = Double.POSITIVE_INFINITY;
    private static final double nan = Double.NaN;

    /**
     * Assert the two numbers are equal within the provided units of least precision.
     * The maximum count of numbers allowed between the two values is {@code maxUlps - 1}.
     *
     * @param msg the failure message
     * @param expected the expected
     * @param actual the actual
     * @param maxUlps the maximum units of least precision between the two values
     */
    private static void assertEquals(Supplier<String> msg, double expected, double actual, long maxUlps) {
        final long e = Double.doubleToLongBits(expected);
        final long a = Double.doubleToLongBits(actual);
        final long delta = Math.abs(e - a);
        if (delta > maxUlps) {
            // DEBUG:
            if (maxUlps < 0) {
                // CHECKSTYLE: stop Regex
                System.out.printf("%s: %s != %s (ulps=%d)%n", msg.get(), expected, actual, delta);
                // CHECKSTYLE: resume Regex
            } else {
                Assertions.fail(String.format("%s: %s != %s (ulps=%d)", msg.get(), expected, actual, delta));
            }
        }
    }

    /**
     * Assert the operation on the complex number is equal to the expected value.
     *
     * <p>The results are are considered equal if there are no floating-point values between them.
     *
     * @param a Real part.
     * @param b Imaginary part.
     * @param operation the operation
     * @param x Expected real part.
     * @param y Expected imaginary part.
     */
    private static void assertComplex(double a, double b,
            UnaryOperator<Complex> operation,
            double x, double y) {
        assertComplex(a, b, operation, x, y, 1);
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
     * @param operation the operation
     * @param x Expected real part.
     * @param y Expected imaginary part.
     * @param maxUlps the maximum units of least precision between the two values
     */
    private static void assertComplex(double a, double b,
            UnaryOperator<Complex> operation,
            double x, double y, long maxUlps) {
        final Complex c = Complex.ofCartesian(a, b);
        final Complex z = operation.apply(c);
        assertEquals(() -> c + ": real", x, z.getReal(), maxUlps);
        assertEquals(() -> c + ": imaginary", y, z.getImaginary(), maxUlps);
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
     * @param operation the operation
     * @param x Expected real part.
     * @param y Expected imaginary part.
     */
    private static void assertComplex(double a, double b, double c, double d,
            BiFunction<Complex, Complex, Complex> operation,
            double x, double y) {
        assertComplex(a, b, c, d, operation, x, y, 1);
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
     * @param operation the operation
     * @param x Expected real part.
     * @param y Expected imaginary part.
     * @param maxUlps the maximum units of least precision between the two values
     */
    // CHECKSTYLE: stop ParameterNumberCheck
    private static void assertComplex(double a, double b, double c, double d,
            BiFunction<Complex, Complex, Complex> operation,
            double x, double y, long maxUlps) {
        final Complex c1 = Complex.ofCartesian(a, b);
        final Complex c2 = Complex.ofCartesian(c, d);
        final Complex z = operation.apply(c1, c2);
        assertEquals(() -> c1 + " op " + c2 + ": real", x, z.getReal(), maxUlps);
        assertEquals(() -> c1 + " op " + c2 + ": imaginary", y, z.getImaginary(), maxUlps);
    }

    @Test
    public void testAcos() {
        assertComplex(-2, 0.0, Complex::acos, 3.1415926535897931, -1.3169578969248164);
        assertComplex(-2, 0.5, Complex::acos, 2.8638383970320791, -1.3618009008578467, 3);
        assertComplex(-2, 1, Complex::acos, 2.6342363503726487, -1.4693517443681854);
        assertComplex(-2, 2, Complex::acos, 2.3250454714929427, -1.7343245214879679);
        assertComplex(-1, 0.0, Complex::acos, 3.1415926535897931, -0.0);
        assertComplex(-1, 0.5, Complex::acos, 2.4667038080037869, -0.73285767597364526);
        assertComplex(-1, 1, Complex::acos, 2.2370357592874122, -1.0612750619050364, 4);
        assertComplex(-1, 2, Complex::acos, 1.997874913187373, -1.5285709194809995, 9);
        assertComplex(-0.5, 0.0, Complex::acos, 2.0943951023931953, -1.1102230246251565e-16);
        assertComplex(-0.5, 0.5, Complex::acos, 2.023074773946087, -0.53063753095251776);
        assertComplex(-0.5, 1, Complex::acos, 1.9202353896521094, -0.92613303135018232, 2);
        assertComplex(-0.5, 2, Complex::acos, 1.7918149624177808, -1.4657153519472903, 2);
        assertComplex(-0.0, 0.0, Complex::acos, 1.5707963267948966, -0.0);
        assertComplex(-0.0, 0.5, Complex::acos, 1.5707963267948966, -0.48121182505960336);
        assertComplex(-0.0, 1, Complex::acos, 1.5707963267948963, -0.88137358701954283);
        assertComplex(-0.0, 2, Complex::acos, 1.5707963267948959, -1.4436354751788099, 3);
        assertComplex(0.0, 0.0, Complex::acos, 1.5707963267948966, -0.0);
        assertComplex(0.0, 0.5, Complex::acos, 1.5707963267948966, -0.48121182505960347, 2);
        assertComplex(0.0, 1, Complex::acos, 1.5707963267948966, -0.88137358701954294);
        assertComplex(0.0, 2, Complex::acos, 1.5707963267948966, -1.4436354751788103, 2);
        assertComplex(0.5, 0.0, Complex::acos, 1.0471975511965976, -1.1102230246251565e-16);
        assertComplex(0.5, 0.5, Complex::acos, 1.1185178796437059, -0.53063753095251787);
        assertComplex(0.5, 1, Complex::acos, 1.2213572639376833, -0.92613303135018255, 2);
        assertComplex(0.5, 2, Complex::acos, 1.3497776911720127, -1.4657153519472905);
        assertComplex(1, 0.0, Complex::acos, 0.0, -0.0);
        assertComplex(1, 0.5, Complex::acos, 0.67488884558600637, -0.73285767597364526);
        assertComplex(1, 1, Complex::acos, 0.90455689430238129, -1.0612750619050355);
        assertComplex(1, 2, Complex::acos, 1.1437177404024204, -1.528570919480998, 2);
        assertComplex(2, 0.0, Complex::acos, 0.0, -1.3169578969248166);
        assertComplex(2, 0.5, Complex::acos, 0.27775425655771396, -1.3618009008578458);
        assertComplex(2, 1, Complex::acos, 0.50735630321714453, -1.4693517443681852);
        assertComplex(2, 2, Complex::acos, 0.8165471820968504, -1.7343245214879663, 7);
    }

    @Test
    public void testAcosh() {
        assertComplex(-2, 0.0, Complex::acosh, 1.3169578969248164, 3.1415926535897931);
        assertComplex(-2, 0.5, Complex::acosh, 1.3618009008578467, 2.8638383970320791, 3);
        assertComplex(-2, 1, Complex::acosh, 1.4693517443681854, 2.6342363503726487);
        assertComplex(-2, 2, Complex::acosh, 1.7343245214879679, 2.3250454714929427);
        assertComplex(-1, 0.0, Complex::acosh, 0.0, 3.1415926535897931);
        assertComplex(-1, 0.5, Complex::acosh, 0.73285767597364526, 2.4667038080037869);
        assertComplex(-1, 1, Complex::acosh, 1.0612750619050364, 2.2370357592874122, 4);
        assertComplex(-1, 2, Complex::acosh, 1.5285709194809995, 1.997874913187373, 9);
        assertComplex(-0.5, 0.0, Complex::acosh, 1.1102230246251565e-16, 2.0943951023931953);
        assertComplex(-0.5, 0.5, Complex::acosh, 0.53063753095251776, 2.023074773946087);
        assertComplex(-0.5, 1, Complex::acosh, 0.92613303135018232, 1.9202353896521094, 2);
        assertComplex(-0.5, 2, Complex::acosh, 1.4657153519472903, 1.7918149624177808, 2);
        assertComplex(-0.0, 0.0, Complex::acosh, 0.0, 1.5707963267948966);
        assertComplex(-0.0, 0.5, Complex::acosh, 0.48121182505960336, 1.5707963267948966);
        assertComplex(-0.0, 1, Complex::acosh, 0.88137358701954283, 1.5707963267948963);
        assertComplex(-0.0, 2, Complex::acosh, 1.4436354751788099, 1.5707963267948959, 3);
        assertComplex(0.0, 0.0, Complex::acosh, 0.0, 1.5707963267948966);
        assertComplex(0.0, 0.5, Complex::acosh, 0.48121182505960347, 1.5707963267948966, 2);
        assertComplex(0.0, 1, Complex::acosh, 0.88137358701954294, 1.5707963267948966);
        assertComplex(0.0, 2, Complex::acosh, 1.4436354751788103, 1.5707963267948966, 2);
        assertComplex(0.5, 0.0, Complex::acosh, 1.1102230246251565e-16, 1.0471975511965976);
        assertComplex(0.5, 0.5, Complex::acosh, 0.53063753095251787, 1.1185178796437059);
        assertComplex(0.5, 1, Complex::acosh, 0.92613303135018255, 1.2213572639376833, 2);
        assertComplex(0.5, 2, Complex::acosh, 1.4657153519472905, 1.3497776911720127);
        assertComplex(1, 0.0, Complex::acosh, 0.0, 0.0);
        assertComplex(1, 0.5, Complex::acosh, 0.73285767597364526, 0.67488884558600637);
        assertComplex(1, 1, Complex::acosh, 1.0612750619050355, 0.90455689430238129);
        assertComplex(1, 2, Complex::acosh, 1.528570919480998, 1.1437177404024204, 2);
        assertComplex(2, 0.0, Complex::acosh, 1.3169578969248166, 0.0);
        assertComplex(2, 0.5, Complex::acosh, 1.3618009008578458, 0.27775425655771396);
        assertComplex(2, 1, Complex::acosh, 1.4693517443681852, 0.50735630321714453, 7);
        assertComplex(2, 2, Complex::acosh, 1.7343245214879663, 0.8165471820968504, 7);
    }

    @Test
    public void testAsinh() {
        // Odd function: negative real cases defined by positive real cases
        assertComplex(0.0, 0.0, Complex::asinh, 0.0, 0.0);
        assertComplex(0.0, 0.5, Complex::asinh, 1.1102230246251565e-16, 0.52359877559829893);
        assertComplex(0.0, 1, Complex::asinh, 0.0, 1.5707963267948966);
        assertComplex(0.0, 2, Complex::asinh, 1.3169578969248166, 1.5707963267948966);
        assertComplex(0.5, 0.0, Complex::asinh, 0.48121182505960347, 0.0);
        assertComplex(0.5, 0.5, Complex::asinh, 0.53063753095251787, 0.45227844715119064);
        assertComplex(0.5, 1, Complex::asinh, 0.73285767597364526, 0.8959074812088903);
        assertComplex(0.5, 2, Complex::asinh, 1.3618009008578458, 1.2930420702371828);
        assertComplex(1, 0.0, Complex::asinh, 0.88137358701954294, 0.0);
        assertComplex(1, 0.5, Complex::asinh, 0.92613303135018255, 0.34943906285721327);
        assertComplex(1, 1, Complex::asinh, 1.0612750619050357, 0.66623943249251527);
        assertComplex(1, 2, Complex::asinh, 1.4693517443681852, 1.0634400235777519);
        assertComplex(2, 0.0, Complex::asinh, 1.4436354751788103, 0.0);
        assertComplex(2, 0.5, Complex::asinh, 1.4657153519472905, 0.22101863562288387);
        assertComplex(2, 1, Complex::asinh, 1.528570919480998, 0.42707858639247614);
        assertComplex(2, 2, Complex::asinh, 1.7343245214879663, 0.75424914469804605);
    }

    @Test
    public void testAtanh() {
        // Odd function: negative real cases defined by positive real cases
        assertComplex(0.0, 0.0, Complex::atanh, 0.0, 0.0);
        assertComplex(0.0, 0.5, Complex::atanh, 0.0, 0.46364760900080615);
        assertComplex(0.0, 1, Complex::atanh, 0.0, 0.78539816339744828);
        assertComplex(0.0, 2, Complex::atanh, 0.0, 1.1071487177940904);
        assertComplex(0.5, 0.0, Complex::atanh, 0.54930614433405489, 0.0);
        assertComplex(0.5, 0.5, Complex::atanh, 0.40235947810852513, 0.5535743588970452);
        assertComplex(0.5, 1, Complex::atanh, 0.23887786125685911, 0.84757566067082901);
        assertComplex(0.5, 2, Complex::atanh, 0.096415620202996211, 1.1265564408348223, 7);
        assertComplex(1, 0.0, Complex::atanh, inf, 0.0);
        assertComplex(1, 0.5, Complex::atanh, 0.70830333601405404, 0.90788749496088039);
        assertComplex(1, 1, Complex::atanh, 0.40235947810852513, 1.0172219678978514);
        assertComplex(1, 2, Complex::atanh, 0.17328679513998635, 1.1780972450961724);
        assertComplex(2, 0.0, Complex::atanh, 0.54930614433405489, 1.5707963267948966);
        assertComplex(2, 0.5, Complex::atanh, 0.50037000005253096, 1.4215468610018069);
        assertComplex(2, 1, Complex::atanh, 0.40235947810852513, 1.3389725222944935);
        assertComplex(2, 2, Complex::atanh, 0.23887786125685906, 1.311223269671635);
    }

    @Test
    public void testCosh() {
        // Even function: negative real cases defined by positive real cases
        assertComplex(0.0, 0.0, Complex::cosh, 1, 0.0);
        assertComplex(0.0, 0.5, Complex::cosh, 0.87758256189037276, 0.0);
        assertComplex(0.0, 1, Complex::cosh, 0.54030230586813977, 0.0);
        assertComplex(0.0, 2, Complex::cosh, -0.41614683654714241, 0.0);
        assertComplex(0.5, 0.0, Complex::cosh, 1.1276259652063807, 0.0);
        assertComplex(0.5, 0.5, Complex::cosh, 0.9895848833999199, 0.24982639750046154);
        assertComplex(0.5, 1, Complex::cosh, 0.60925890915779424, 0.43848657989259532);
        assertComplex(0.5, 2, Complex::cosh, -0.46925797822905341, 0.473830620416407);
        assertComplex(1, 0.0, Complex::cosh, 1.5430806348152437, 0.0);
        assertComplex(1, 0.5, Complex::cosh, 1.3541806567045842, 0.5634214652309818);
        assertComplex(1, 1, Complex::cosh, 0.83373002513114913, 0.98889770576286506);
        assertComplex(1, 2, Complex::cosh, -0.64214812471551996, 1.0686074213827783);
        assertComplex(2, 0.0, Complex::cosh, 3.7621956910836314, 0.0);
        assertComplex(2, 0.5, Complex::cosh, 3.3016373329140944, 1.7388095044743164);
        assertComplex(2, 1, Complex::cosh, 2.0327230070196656, 3.0518977991517997);
        assertComplex(2, 2, Complex::cosh, -1.5656258353157435, 3.2978948363112366);
    }

    @Test
    public void testSinh() {
        // Odd function: negative real cases defined by positive real cases
        assertComplex(0.0, 0.0, Complex::sinh, 0.0, 0.0);
        assertComplex(0.0, 0.5, Complex::sinh, 0.0, 0.47942553860420301);
        assertComplex(0.0, 1, Complex::sinh, 0.0, 0.8414709848078965);
        assertComplex(0.0, 2, Complex::sinh, -0.0, 0.90929742682568171);
        assertComplex(0.5, 0.0, Complex::sinh, 0.52109530549374738, 0.0);
        assertComplex(0.5, 0.5, Complex::sinh, 0.45730415318424927, 0.54061268571315335);
        assertComplex(0.5, 1, Complex::sinh, 0.28154899513533443, 0.94886453143716798);
        assertComplex(0.5, 2, Complex::sinh, -0.21685216292078974, 1.0253473885839877);
        assertComplex(1, 0.0, Complex::sinh, 1.1752011936438014, 0.0);
        assertComplex(1, 0.5, Complex::sinh, 1.0313360742545512, 0.73979226445601376);
        assertComplex(1, 1, Complex::sinh, 0.63496391478473613, 1.2984575814159773);
        assertComplex(1, 2, Complex::sinh, -0.48905625904129368, 1.4031192506220405);
        assertComplex(2, 0.0, Complex::sinh, 3.6268604078470186, 0.0);
        assertComplex(2, 0.5, Complex::sinh, 3.1828694483371489, 1.8036926955321817);
        assertComplex(2, 1, Complex::sinh, 1.9596010414216061, 3.1657785132161682);
        assertComplex(2, 2, Complex::sinh, -1.5093064853236156, 3.4209548611170133);
    }

    @Test
    public void testTanh() {
        // Odd function: negative real cases defined by positive real cases
        assertComplex(0.0, 0.0, Complex::tanh, 0.0, 0.0);
        assertComplex(0.0, 0.5, Complex::tanh, 0.0, 0.54630248984379048);
        assertComplex(0.0, 1, Complex::tanh, 0.0, 1.5574077246549021);
        assertComplex(0.0, 2, Complex::tanh, 0.0, -2.1850398632615189);
        assertComplex(0.5, 0.0, Complex::tanh, 0.46211715726000974, 0.0);
        assertComplex(0.5, 0.5, Complex::tanh, 0.56408314126749848, 0.40389645531602575, 2);
        assertComplex(0.5, 1, Complex::tanh, 1.042830728344361, 0.80687741216308495);
        assertComplex(0.5, 2, Complex::tanh, 1.3212865837711918, -0.85087812114493777, 2);
        assertComplex(1, 0.0, Complex::tanh, 0.76159415595576485, 0.0);
        assertComplex(1, 0.5, Complex::tanh, 0.84296620484578311, 0.19557731006593398);
        assertComplex(1, 1, Complex::tanh, 1.0839233273386946, 0.27175258531951174);
        assertComplex(1, 2, Complex::tanh, 1.1667362572409199, -0.24345820118572523);
        assertComplex(2, 0.0, Complex::tanh, 0.9640275800758169, 0.0);
        assertComplex(2, 0.5, Complex::tanh, 0.97994084996173814, 0.030215987322877575);
        assertComplex(2, 1, Complex::tanh, 1.0147936161466335, 0.033812826079896691);
        assertComplex(2, 2, Complex::tanh, 1.0238355945704727, -0.028392952868232287);
    }

    @Test
    public void testExp() {
        assertComplex(-2, 0.0, Complex::exp, 0.1353352832366127, 0.0);
        assertComplex(-2, 0.5, Complex::exp, 0.11876788457694579, 0.064883191057865414);
        assertComplex(-2, 1, Complex::exp, 0.073121965598059641, 0.1138807140643681);
        assertComplex(-2, 2, Complex::exp, -0.056319349992127891, 0.12306002480577674);
        assertComplex(-1, 0.0, Complex::exp, 0.36787944117144233, 0.0);
        assertComplex(-1, 0.5, Complex::exp, 0.32284458245003306, 0.17637079922503196);
        assertComplex(-1, 1, Complex::exp, 0.19876611034641298, 0.30955987565311222);
        assertComplex(-1, 2, Complex::exp, -0.15309186567422631, 0.33451182923926226);
        assertComplex(-0.5, 0.0, Complex::exp, 0.60653065971263342, 0.0);
        assertComplex(-0.5, 0.5, Complex::exp, 0.53228073021567079, 0.29078628821269187);
        assertComplex(-0.5, 1, Complex::exp, 0.32770991402245986, 0.51037795154457277);
        assertComplex(-0.5, 2, Complex::exp, -0.25240581530826373, 0.55151676816758077);
        assertComplex(-0.0, 0.0, Complex::exp, 1, 0.0);
        assertComplex(-0.0, 0.5, Complex::exp, 0.87758256189037276, 0.47942553860420301);
        assertComplex(-0.0, 1, Complex::exp, 0.54030230586813977, 0.8414709848078965);
        assertComplex(-0.0, 2, Complex::exp, -0.41614683654714241, 0.90929742682568171);
        assertComplex(0.0, 0.0, Complex::exp, 1, 0.0);
        assertComplex(0.0, 0.5, Complex::exp, 0.87758256189037276, 0.47942553860420301);
        assertComplex(0.0, 1, Complex::exp, 0.54030230586813977, 0.8414709848078965);
        assertComplex(0.0, 2, Complex::exp, -0.41614683654714241, 0.90929742682568171);
        assertComplex(0.5, 0.0, Complex::exp, 1.6487212707001282, 0.0);
        assertComplex(0.5, 0.5, Complex::exp, 1.4468890365841693, 0.79043908321361489);
        assertComplex(0.5, 1, Complex::exp, 0.89080790429312873, 1.3873511113297634);
        assertComplex(0.5, 2, Complex::exp, -0.68611014114984314, 1.4991780090003948);
        assertComplex(1, 0.0, Complex::exp, 2.7182818284590451, 0.0);
        assertComplex(1, 0.5, Complex::exp, 2.3855167309591354, 1.3032137296869954);
        assertComplex(1, 1, Complex::exp, 1.4686939399158851, 2.2873552871788423);
        assertComplex(1, 2, Complex::exp, -1.1312043837568135, 2.4717266720048188);
        assertComplex(2, 0.0, Complex::exp, 7.3890560989306504, 0.0);
        assertComplex(2, 0.5, Complex::exp, 6.4845067812512438, 3.5425022000064983);
        assertComplex(2, 1, Complex::exp, 3.9923240484412719, 6.2176763123679679);
        assertComplex(2, 2, Complex::exp, -3.0749323206393591, 6.7188496974282499);
    }

    @Test
    public void testLog() {
        assertComplex(-2, 0.0, Complex::log, 0.69314718055994529, 3.1415926535897931);
        assertComplex(-2, 0.5, Complex::log, 0.72345949146816269, 2.8966139904629289);
        assertComplex(-2, 1, Complex::log, 0.80471895621705025, 2.677945044588987);
        assertComplex(-2, 2, Complex::log, 1.0397207708399181, 2.3561944901923448);
        assertComplex(-1, 0.0, Complex::log, 0.0, 3.1415926535897931);
        assertComplex(-1, 0.5, Complex::log, 0.11157177565710492, 2.677945044588987);
        assertComplex(-1, 1, Complex::log, 0.3465735902799727, 2.3561944901923448);
        assertComplex(-1, 2, Complex::log, 0.80471895621705025, 2.0344439357957027);
        assertComplex(-0.5, 0.0, Complex::log, -0.69314718055994529, 3.1415926535897931);
        assertComplex(-0.5, 0.5, Complex::log, -0.34657359027997259, 2.3561944901923448);
        assertComplex(-0.5, 1, Complex::log, 0.11157177565710492, 2.0344439357957027);
        assertComplex(-0.5, 2, Complex::log, 0.72345949146816269, 1.8157749899217608);
        assertComplex(-0.0, 0.0, Complex::log, -inf, 3.1415926535897931);
        assertComplex(-0.0, 0.5, Complex::log, -0.69314718055994529, 1.5707963267948966);
        assertComplex(-0.0, 1, Complex::log, 0.0, 1.5707963267948966);
        assertComplex(-0.0, 2, Complex::log, 0.69314718055994529, 1.5707963267948966);
        assertComplex(0.0, 0.0, Complex::log, -inf, 0.0);
        assertComplex(0.0, 0.5, Complex::log, -0.69314718055994529, 1.5707963267948966);
        assertComplex(0.0, 1, Complex::log, 0.0, 1.5707963267948966);
        assertComplex(0.0, 2, Complex::log, 0.69314718055994529, 1.5707963267948966);
        assertComplex(0.5, 0.0, Complex::log, -0.69314718055994529, 0.0);
        assertComplex(0.5, 0.5, Complex::log, -0.34657359027997259, 0.78539816339744828);
        assertComplex(0.5, 1, Complex::log, 0.11157177565710492, 1.1071487177940904);
        assertComplex(0.5, 2, Complex::log, 0.72345949146816269, 1.3258176636680326);
        assertComplex(1, 0.0, Complex::log, 0.0, 0.0);
        assertComplex(1, 0.5, Complex::log, 0.11157177565710492, 0.46364760900080609);
        assertComplex(1, 1, Complex::log, 0.3465735902799727, 0.78539816339744828);
        assertComplex(1, 2, Complex::log, 0.80471895621705025, 1.1071487177940904);
        assertComplex(2, 0.0, Complex::log, 0.69314718055994529, 0.0);
        assertComplex(2, 0.5, Complex::log, 0.72345949146816269, 0.24497866312686414);
        assertComplex(2, 1, Complex::log, 0.80471895621705025, 0.46364760900080609);
        assertComplex(2, 2, Complex::log, 1.0397207708399181, 0.78539816339744828);
    }

    @Test
    public void testSqrt() {
        // Note: When computed in polar coordinates:
        //   real = (x^2 + y^2)^0.25 * cos(0.5 * atan2(y, x))
        // If x is positive and y is +/-0.0 atan2 returns +/-0.
        // If x is negative and y is +/-0.0 atan2 returns +/-PI.
        // This causes problems as
        //   cos(0.5 * PI) = 6.123233995736766e-17
        // assert: Math.cos(Math.acos(0)) != 0.0
        // Thus polar computation will produce incorrect output when
        // there is no imaginary component and real is negative.
        // The computation should be done for real only numbers separately.

        assertComplex(-2, 0.0, Complex::sqrt, 0, 1.4142135623730951);
        assertComplex(-2, 0.5, Complex::sqrt, 0.17543205637629397, 1.425053124063947, 5);
        assertComplex(-2, 1, Complex::sqrt, 0.3435607497225126, 1.4553466902253549, 3);
        assertComplex(-2, 2, Complex::sqrt, 0.64359425290558281, 1.5537739740300374, 2);
        assertComplex(-1, 0.0, Complex::sqrt, 0, 1);
        assertComplex(-1, 0.5, Complex::sqrt, 0.24293413587832291, 1.0290855136357462, 3);
        assertComplex(-1, 1, Complex::sqrt, 0.45508986056222739, 1.0986841134678098);
        assertComplex(-1, 2, Complex::sqrt, 0.78615137775742339, 1.2720196495140688);
        assertComplex(-0.5, 0.0, Complex::sqrt, 0, 0.70710678118654757);
        assertComplex(-0.5, 0.5, Complex::sqrt, 0.3217971264527914, 0.77688698701501868, 2);
        assertComplex(-0.5, 1, Complex::sqrt, 0.55589297025142126, 0.89945371997393353);
        assertComplex(-0.5, 2, Complex::sqrt, 0.88361553087551337, 1.1317139242778693, 2);
        assertComplex(-0.0, 0.0, Complex::sqrt, 0.0, 0.0);
        assertComplex(-0.0, 0.5, Complex::sqrt, 0.50000000000000011, 0.5);
        assertComplex(-0.0, 1, Complex::sqrt, 0.70710678118654757, 0.70710678118654746);
        assertComplex(-0.0, 2, Complex::sqrt, 1.0000000000000002, 1);
        assertComplex(0.0, 0.0, Complex::sqrt, 0.0, 0.0);
        assertComplex(0.0, 0.5, Complex::sqrt, 0.50000000000000011, 0.5);
        assertComplex(0.0, 1, Complex::sqrt, 0.70710678118654757, 0.70710678118654746);
        assertComplex(0.0, 2, Complex::sqrt, 1.0000000000000002, 1);
        assertComplex(0.5, 0.0, Complex::sqrt, 0.70710678118654757, 0.0);
        assertComplex(0.5, 0.5, Complex::sqrt, 0.77688698701501868, 0.32179712645279135);
        assertComplex(0.5, 1, Complex::sqrt, 0.89945371997393364, 0.55589297025142115);
        assertComplex(0.5, 2, Complex::sqrt, 1.1317139242778693, 0.88361553087551337);
        assertComplex(1, 0.0, Complex::sqrt, 1, 0.0);
        assertComplex(1, 0.5, Complex::sqrt, 1.0290855136357462, 0.24293413587832283);
        assertComplex(1, 1, Complex::sqrt, 1.0986841134678098, 0.45508986056222733);
        assertComplex(1, 2, Complex::sqrt, 1.272019649514069, 0.78615137775742328);
        assertComplex(2, 0.0, Complex::sqrt, 1.4142135623730951, 0.0);
        assertComplex(2, 0.5, Complex::sqrt, 1.425053124063947, 0.17543205637629383);
        assertComplex(2, 1, Complex::sqrt, 1.4553466902253549, 0.34356074972251244);
        assertComplex(2, 2, Complex::sqrt, 1.5537739740300374, 0.6435942529055827);
    }

    @Test
    public void testMultiply() {
        assertComplex(2, 3, 5, 4, Complex::multiply, -2, 23);
        assertComplex(5, 4, 2, 3, Complex::multiply, -2, 23);
        assertComplex(2, 3, -5, 4, Complex::multiply, -22, -7);
        assertComplex(-5, 4, 2, 3, Complex::multiply, -22, -7);
        assertComplex(2, 3, 5, -4, Complex::multiply, 22, 7);
        assertComplex(5, -4, 2, 3, Complex::multiply, 22, 7);
        assertComplex(2, 3, -5, -4, Complex::multiply, 2, -23);
        assertComplex(-5, -4, 2, 3, Complex::multiply, 2, -23);
    }

    @Test
    public void testDivide() {
        assertComplex(2, 3, 5, 4, Complex::divide, 0.53658536585365857, 0.17073170731707318);
        assertComplex(5, 4, 2, 3, Complex::divide, 1.6923076923076923, -0.53846153846153844);
        assertComplex(2, 3, -5, 4, Complex::divide, 0.04878048780487805, -0.56097560975609762);
        assertComplex(-5, 4, 2, 3, Complex::divide, 0.15384615384615385, 1.7692307692307692);
        assertComplex(2, 3, 5, -4, Complex::divide, -0.04878048780487805, 0.56097560975609762);
        assertComplex(5, -4, 2, 3, Complex::divide, -0.15384615384615385, -1.7692307692307692);
        assertComplex(2, 3, -5, -4, Complex::divide, -0.53658536585365857, -0.17073170731707318);
        assertComplex(-5, -4, 2, 3, Complex::divide, -1.6923076923076923, 0.53846153846153844);
    }

    @Test
    public void testPowComplex() {
        assertComplex(2, 3, 5, 4, Complex::pow, -9.7367145095888414, -6.9377513609299886, 2);
        assertComplex(5, 4, 2, 3, Complex::pow, 4.354910316631539, 3.2198331430252156, 8);
        assertComplex(2, 3, -5, 4, Complex::pow, 3.1452105198427317e-05, 6.8990150088148226e-06);
        assertComplex(-5, 4, 2, 3, Complex::pow, -0.011821399482548253, -0.022082334539521097);
        assertComplex(2, 3, 5, -4, Complex::pow, 30334.832969842264, -6653.9414970320349);
        assertComplex(5, -4, 2, 3, Complex::pow, -146.48661898442663, -273.63651239033993, 9);
        assertComplex(2, 3, -5, -4, Complex::pow, -0.068119398044204305, 0.048537465694561743, 2);
        assertComplex(-5, -4, 2, 3, Complex::pow, 53964.514878760994, 39899.038308625939);
    }
}
