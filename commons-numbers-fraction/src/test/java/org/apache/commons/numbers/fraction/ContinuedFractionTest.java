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

import java.util.Locale;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


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
        Assertions.assertEquals(1.61803399, gr, eps);
    }

    @Test
    public void test415Over93() throws Exception {
        // https://en.wikipedia.org/wiki/Continued_fraction
        // 415             1
        // ---  = 4 + ---------
        //  93        2 +   1
        //                -----
        //                6 + 1
        //                    -
        //                    7
        //      = [4; 2, 6, 7]

        ContinuedFraction cf = new ContinuedFraction() {
            @Override
            public double getA(int n, double x) {
                switch (n) {
                    case 0: return 4;
                    case 1: return 2;
                    case 2: return 6;
                    case 3: return 7;
                    default: return 1;
                }
            }

            @Override
            public double getB(int n, double x) {
                return n <= 3 ? 1 : 0;
            }
        };

        final double eps = 1e-8;
        double gr = cf.evaluate(0, eps, 5);
        Assertions.assertEquals(415.0 / 93.0, gr, eps);
    }

    @Test
    public void testMaxIterationsThrows() throws Exception {
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
        final int maxIterations = 3;
        final Throwable t = Assertions.assertThrows(FractionException.class,
            () -> cf.evaluate(0, eps, maxIterations));
        assertExceptionMessageContains(t, "max");
    }

    @Test
    public void testNaNThrows() throws Exception {
        // Create a NaN during the iteration
        ContinuedFraction cf = new ContinuedFraction() {
            @Override
            public double getA(int n, double x) {
                return n == 0 ? 1 : Double.NaN;
            }

            @Override
            public double getB(int n, double x) {
                return 1;
            }
        };

        final double eps = 1e-8;
        final Throwable t = Assertions.assertThrows(FractionException.class,
            () -> cf.evaluate(0, eps, 5));
        assertExceptionMessageContains(t, "nan");
    }

    @Test
    public void testInfThrows() throws Exception {
        // Create an infinity during the iteration:
        // b / cPrev  => b_1 / a_0 => Double.MAX_VALUE / 0.5
        ContinuedFraction cf = new ContinuedFraction() {
            @Override
            public double getA(int n, double x) {
                return 0.5;
            }

            @Override
            public double getB(int n, double x) {
                return n == 0 ? 1 : Double.MAX_VALUE;
            }
        };

        final double eps = 1e-8;
        final Throwable t = Assertions.assertThrows(FractionException.class,
            () -> cf.evaluate(0, eps, 5));
        assertExceptionMessageContains(t, "infinity");
    }

    private static void assertExceptionMessageContains(Throwable t, String text) {
        Assertions.assertTrue(t.getMessage().toLowerCase(Locale.ROOT).contains(text),
            () -> "Missing '" + text + "' from exception message: " + t.getMessage());
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
        Assertions.assertEquals(1.61, gr, eps);
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
        Assertions.assertEquals(1.5, gr, 0d);
    }
}
