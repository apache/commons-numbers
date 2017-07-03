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
package org.apache.commons.numbers.fraction;

import org.junit.Assert;
import org.junit.Test;


/**
 * Tests for {@link ContinuedFraction}.
 */
public class ContinuedFractionTest {

    @Test
    public void testGoldenRatio() throws Exception {
        ContinuedFraction cf = new ContinuedFraction() {
            @Override
            public double getA(int n, double x) {
                return 1;
            }

            @Override
            public double getB(int n, double x) {
                return 1;
            }
        };

        final double eps = 1e-8;
        double gr = cf.evaluate(0, eps);
        Assert.assertEquals(1.61803399, gr, eps);
    }

    // NUMBERS-46
    @Test
    public void testOneIteration() {
        ContinuedFraction cf = new ContinuedFraction() {
            @Override
            public double getA(int n, double x) {
                return 1;
            }

            @Override
            public double getB(int n, double x) {
                return 1;
            }
        };

        final double eps = 10;
        double gr = cf.evaluate(0, eps, 1);
        Assert.assertEquals(1.61, gr, eps);
    }

    // NUMBERS-46
    @Test
    public void testTwoIterations() {
        ContinuedFraction cf = new ContinuedFraction() {
            @Override
            public double getA(int n, double x) {
                return 1;
            }

            @Override
            public double getB(int n, double x) {
                return 1;
            }
        };

        final double eps = 0.5;
        double gr = cf.evaluate(0, eps, 2);
        Assert.assertEquals(1.5, gr, 0d);
    }
}
