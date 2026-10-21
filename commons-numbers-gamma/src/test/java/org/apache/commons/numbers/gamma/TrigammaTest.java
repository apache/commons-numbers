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
package org.apache.commons.numbers.gamma;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for {@link Trigamma}.
 */
class TrigammaTest {
    @ParameterizedTest
    @MethodSource
    void testTrigamma(double value, double expected) {
        // Javadoc states: 1e-8 relative error or absolute error (whichever is larger).
        // Current test data passes at 1e-9, but not 1e-10.
        final double eps = 1e-9;
        final double actual = Trigamma.value(value);
        if (Double.isFinite(expected)) {
            final double delta = Math.max(expected * eps, eps);
            Assertions.assertEquals(expected, actual, delta, () -> "trigamma: " + value);
        } else {
            Assertions.assertEquals(expected, actual, () -> "trigamma: " + value);
        }
    }

    static Stream<Arguments> testTrigamma() {
        final double inf = Double.POSITIVE_INFINITY;
        return Stream.of(
            // Negative integers are poles of the trigamma function
            Arguments.of(-1e300, inf),
            Arguments.of(-1e18, inf),
            Arguments.of(-1e16, inf),
            Arguments.of(-0x1p53, inf),
            Arguments.of(-63, inf),
            Arguments.of(-62, inf),
            Arguments.of(-2, inf),
            Arguments.of(-1, inf),
            // Computed using MATLAB R2026a:
            // digits(32)
            // vpa(psi(1, sym(x, 'f'))
            Arguments.of(-98765.4321, 10.332673372912188148264022057244),
            Arguments.of(-100.5, 9.8597034918700861519982038331294),
            Arguments.of(-63.0001, 100000003.26748120604737038124534),
            Arguments.of(-62.0001, 100000003.26722925421431309128454),
            Arguments.of(-50.5, 9.8499971860824842274155224962237),
            Arguments.of(-20.5, 9.8219943446498794820737114698026),
            Arguments.of(-10.5, 9.7787577398148123844967599803581),
            Arguments.of(-5.5, 9.703319865339400381194594333564),
            Arguments.of(-2.5, 9.5392466449891237538616899443825),
            Arguments.of(-2.0001, 100000002.89452748018853896347721),
            Arguments.of(-1.0001, 100000002.64499656752351264863645),
            Arguments.of(-0.5, 8.9348022005446793094172454999381),
            Arguments.of(-1e-1, 101.92253995947719243202249211742),
            Arguments.of(-1e-2, 10001.669304101071408182825296132),
            Arguments.of(-1e-3, 1000001.6473414317354171881001598),
            Arguments.of(-1e-4, 100000001.64517450111835638165592),
            Arguments.of(-1e-5, 10000000001.644956472249912465609),
            Arguments.of(1e-11, 10000000000000001210062.259295271),
            Arguments.of(1e-10, 99999999999999992715.20547096706),
            Arguments.of(1e-9, 999999999999999877.08175114888441),
            Arguments.of(1e-8, 10000000000000001.226482826204519),
            Arguments.of(1e-7, 100000000000001.65398420407170102),
            Arguments.of(1e-6, 1000000000001.6450221665140153103),
            Arguments.of(1e-5, 10000000001.644908389973777786412),
            Arguments.of(1e-4, 100000001.64469367834879712175657),
            Arguments.of(1e-3, 1000001.6425331958273446695041488),
            Arguments.of(1e-2, 10001.621213528312803789235020429),
            Arguments.of(1e-1, 101.43329915079274770465203965087),
            Arguments.of(1, 1.644934066848226436472415166646),
            Arguments.of(1.5, 0.93480220054467930941724549993808),
            Arguments.of(2, 0.64493406684822643647241516664603),
            Arguments.of(2.5, 0.49035775610023486497280105549363),
            Arguments.of(3, 0.39493406684822643647241516664603),
            Arguments.of(3.5, 0.33035775610023486497280105549363),
            Arguments.of(4, 0.28382295573711532536130405553491),
            Arguments.of(4.5, 0.24872510303901037517688268814669),
            Arguments.of(5, 0.22132295573711532536130405553491),
            Arguments.of(7.5, 0.14261589669670379977007418110509),
            Arguments.of(10, 0.10516633568168574612220100690806),
            Arguments.of(20, 0.051270822935203119831536294588382),
            Arguments.of(50, 0.020201333226697125805970645065733),
            Arguments.of(100, 0.010050166663333571395245668465701),
            Arguments.of(12345.6789, 0.00008100328132573320889689179458686)
        );
    }

    @Test
    void testTrigammaZero() {
        Assertions.assertEquals(Double.POSITIVE_INFINITY, Trigamma.value(0));
    }

    @Test
    void testTrigammaNonRealArgs() {
        // See NUMBERS-219
        Assertions.assertEquals(Double.NaN, Trigamma.value(Double.NaN));
        Assertions.assertEquals(0, Trigamma.value(Double.POSITIVE_INFINITY));
        Assertions.assertEquals(Double.NaN, Trigamma.value(Double.NEGATIVE_INFINITY));
    }
}

