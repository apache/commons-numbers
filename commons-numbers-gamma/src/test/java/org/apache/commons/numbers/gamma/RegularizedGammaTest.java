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

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link RegularizedGamma}.
 */
public class RegularizedGammaTest {
    @Test
    public void testRegularizedGammaNanPositive() {
        testRegularizedGamma(Double.NaN, Double.NaN, 1.0);
    }

    @Test
    public void testRegularizedGammaPositiveNan() {
        testRegularizedGamma(Double.NaN, 1.0, Double.NaN);
    }

    @Test
    public void testRegularizedGammaNegativePositive() {
        testRegularizedGamma(Double.NaN, -1.5, 1.0);
    }

    @Test
    public void testRegularizedGammaPositiveNegative() {
        testRegularizedGamma(Double.NaN, 1.0, -1.0);
    }

    @Test
    public void testRegularizedGammaZeroPositive() {
        testRegularizedGamma(Double.NaN, 0.0, 1.0);
    }

    @Test
    public void testRegularizedGammaPositiveZero() {
        testRegularizedGamma(0.0, 1.0, 0.0);
    }

    @Test
    public void testRegularizedGammaPositivePositive() {
        testRegularizedGamma(0.632120558828558, 1.0, 1.0);
    }

    private void testRegularizedGamma(double expected, double a, double x) {
        double actualP = RegularizedGamma.P.value(a, x);
        double actualQ = RegularizedGamma.Q.value(a, x);
        Assert.assertEquals(expected, actualP, 1e-15);
        Assert.assertEquals(actualP, 1 - actualQ, 1e-15);
    }
}
