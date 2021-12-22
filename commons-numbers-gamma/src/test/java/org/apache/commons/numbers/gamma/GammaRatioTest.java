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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link GammaRatio}.
 *
 * <p>The class directly calls the methods in {@link BoostGamma}. This test ensures
 * the arguments are passed through correctly. Accuracy of the function is tested
 * in {@link BoostGammaTest}.
 */
class GammaRatioTest {
    /**
     * Reference data for the {@link GammaRatio#value(double, double)} function. This
     * data was generated with the following <a
     * href="http://maxima.sourceforge.net/">Maxima</a> script.
     * Note: This data is different from the data used for {@link BoostGammaTest}.
     *
     * <pre>
     * kill(all);
     * fpprec : 256;
     *
     * ratio(a, b) := gamma(bfloat(a)) / gamma(bfloat(b));
     * str(x) := ssubst("e","b",string(x));
     *
     * z : [
     *   [3.5, 4.5],
     *   [13.5, 23.75],
     *   [27, 13],
     *   [56, 59],
     *   [101.5, 81.25],
     *   [199.5, 175],
     *   [400, 410]
     * ];
     *
     * for i : 1 while i <= length(z) do
     *     printf(true, "\"~f,~f,~a\",~%", z[i][1], z[i][2], str(ratio(z[i][1], z[i][2]))), fpprintprec:30;
     * </pre>
     */
    @ParameterizedTest
    @CsvSource(value = {
        "3.5,4.5,2.85714285714285714285714285714e-1",
        "13.5,23.75,1.45490620351017217797152181526e-13",
        "27.0,13.0,8.4194178292224e17",
        "56.0,59.0,5.40143462103534698816005531069e-6",
        "101.5,81.25,4.36798404948858205602778130195e39",
        "199.5,175.0,4.34767833253393186075428596899e55",
        "400.0,410.0,8.52951298328691416355607053229e-27",
    })
    void testGammaRatio(double a, double b, double expected) {
        final double actual = GammaRatio.value(a, b);
        final double tol = 25 * Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol, () -> a + " " + b);
    }

    /**
     * Reference data for the {@link GammaRatio#delta(double, double)} function. This
     * data was generated with the following <a
     * href="http://maxima.sourceforge.net/">Maxima</a> script.
     * Note: This data is different from the data used for {@link BoostGammaTest}.
     *
     * <pre>
     * kill(all);
     * fpprec : 256;
     *
     * delta_ratio(a, b) := gamma(bfloat(a)) / gamma(bfloat(a)+b);
     * str(x) := ssubst("e","b",string(x));
     *
     * z : [
     *   [3.5, 0.125],
     *   [13.5, -0.0625],
     *   [27, 5],
     *   [56, -3],
     *   [101.5, 1.25],
     *   [199.5, 0.25],
     *   [400, -0.75]
     * ];
     *
     * for i : 1 while i <= length(z) do
     *     printf(true, "\"~f,~f,~a\",~%", z[i][1], z[i][2], str(delta_ratio(z[i][1], z[i][2]))), fpprintprec:30;
     * </pre>
     */
    @ParameterizedTest
    @CsvSource(value = {
        "3.5,0.125,8.68974954038878070833378190907e-1",
        "13.5,-0.0625,1.17371545612608976781951134174e0",
        "27.0,5.0,4.9045284492077224743149845115e-8",
        "56.0,-3.0,1.5741e5",
        "101.5,1.25,3.09920663912863057124944000292e-3",
        "199.5,0.25,2.6620637148638139042172285053e-1",
        "400.0,-0.75,8.92959446413503605759310244867e1",
    })
    void testGammaDeltaRatio(double a, double b, double expected) {
        final double actual = GammaRatio.delta(a, b);
        final double tol = 20 * Math.ulp(expected);
        Assertions.assertEquals(expected, actual, tol, () -> a + " " + b);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "0, 1",
        "-1, 1",
        "Infinity, 1",
        "NaN, 1",
        "1, 0",
        "1, -1",
        "1, Infinity",
        "1, NaN",
    })
    void testGammaRatioIllegalArguments(double a, double b) {
        Assertions.assertEquals(Double.NaN, GammaRatio.value(a, b));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "NaN, 1",
        "1, NaN",
    })
    void testGammaDeltaRatioIllegalArguments(double a, double delta) {
        Assertions.assertEquals(Double.NaN, GammaRatio.delta(a, delta));
    }
}
