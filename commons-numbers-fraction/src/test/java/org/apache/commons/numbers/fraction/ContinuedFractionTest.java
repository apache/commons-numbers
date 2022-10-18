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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link ContinuedFraction}.
 */
class ContinuedFractionTest {
    /** Golden ratio constant. */
    private static final double GOLDEN_RATIO = 1.618033988749894848204586834365638117720309;

    /**
     * Evaluates the golden ratio.
     *
     * <pre>
     * 1 +        1
     *     ----------------
     *     1 +      1
     *         ------------
     *         1 +     1
     *              -------
     *              1 + ...
     * </pre>
     *
     * <p>This is used to test various conditions for the {@link ContinuedFraction}.
     *
     * @see <a href="https://mathworld.wolfram.com/GoldenRatio.html">MathWorld Golden
     * Ratio equation 17</a>
     */
    private static class GoldenRatio extends ContinuedFraction {
        private static final GoldenRatio INSTANCE = new GoldenRatio();

        /**
         * @return single instance of GoldenRatio
         */
        static GoldenRatio getInstance() {
            return INSTANCE;
        }

        /** {@inheritDoc} */
        @Override
        public double getA(int n, double x) {
            return 1;
        }

        /** {@inheritDoc} */
        @Override
        public double getB(int n, double x) {
            return 1;
        }
    }

    @Test
    void testGoldenRatio() {
        final double eps = 1e-8;
        final double gr = GoldenRatio.getInstance().evaluate(0, eps);
        Assertions.assertEquals(GOLDEN_RATIO, gr, GOLDEN_RATIO * eps);
    }

    /**
     * Test a continued fraction where the leading term is zero.
     * Evaluates the reciprocal of the golden ratio.
     */
    @Test
    void testGoldenRatioReciprocal() {
        final double eps = 1e-8;
        final ContinuedFraction cf = new ContinuedFraction() {
            @Override
            public double getA(int n, double x) {
                // Check this is not called with n=0
                Assertions.assertNotEquals(0, n, "a0 should never require evaluation");
                return 1;
            }

            @Override
            public double getB(int n, double x) {
                // b0 = 0
                return n == 0 ? 0 : 1;
            }
        };
        final double gr = cf.evaluate(0, eps);
        Assertions.assertEquals(1 / GOLDEN_RATIO, gr, eps / GOLDEN_RATIO);
    }

    /**
     * Test an invalid epsilon (zero, negative or NaN).
     * See NUMBERS-173.
     *
     * @param epsilon Epsilon
     */
    @ParameterizedTest
    @ValueSource(doubles = {0, -1, Double.NaN})
    void testGoldenRatioEpsilonZero(double epsilon) {
        // An invalid epsilon is set to the minimum epsilon.
        // It should converge to 1 ULP.
        final double tolerance = Math.ulp(GOLDEN_RATIO);
        Assertions.assertEquals(GOLDEN_RATIO, GoldenRatio.getInstance().evaluate(0, epsilon), tolerance);
    }

    @Test
    void test415Over93() {
        // https://en.wikipedia.org/wiki/Continued_fraction
        // 415             1
        // ---  = 4 + ---------
        //  93        2 +   1
        //                -----
        //                6 + 1
        //                    -
        //                    7
        //      = [4; 2, 6, 7]

        final ContinuedFraction cf = new ContinuedFraction() {
            @Override
            public double getA(int n, double x) {
                return n <= 3 ? 1 : 0;
            }

            @Override
            public double getB(int n, double x) {
                switch (n) {
                case 0:
                    return 4;
                case 1:
                    return 2;
                case 2:
                    return 6;
                case 3:
                    return 7;
                default:
                    return 1;
                }
            }
        };

        final double eps = 1e-8;
        final double gr = cf.evaluate(0, eps, 5);
        Assertions.assertEquals(415.0 / 93.0, gr, eps);
    }

    @Test
    void testMaxIterationsThrows() {
        final ContinuedFraction cf = GoldenRatio.getInstance();

        final double eps = 1e-8;
        final int maxIterations = 3;
        final Throwable t = Assertions.assertThrows(FractionException.class, () -> cf.evaluate(0, eps, maxIterations));
        assertExceptionMessageContains(t, "max");
    }

    @Test
    void testNaNThrows() {
        // Create a NaN during the iteration
        final ContinuedFraction cf = new ContinuedFraction() {
            @Override
            public double getA(int n, double x) {
                return 1;
            }

            @Override
            public double getB(int n, double x) {
                return n == 0 ? 1 : Double.NaN;
            }
        };

        final double eps = 1e-8;
        final Throwable t = Assertions.assertThrows(FractionException.class, () -> cf.evaluate(0, eps, 5));
        assertExceptionMessageContains(t, "nan");
    }

    @Test
    void testInfThrows() {
        // Create an infinity during the iteration:
        // a / cPrev => a_1 / b_0 => Double.MAX_VALUE / 0.5
        final ContinuedFraction cf = new ContinuedFraction() {
            @Override
            public double getA(int n, double x) {
                return n == 0 ? 1 : Double.MAX_VALUE;
            }

            @Override
            public double getB(int n, double x) {
                return 0.5;
            }
        };

        final double eps = 1e-8;
        final Throwable t = Assertions.assertThrows(FractionException.class, () -> cf.evaluate(0, eps, 5));
        assertExceptionMessageContains(t, "infinity");
    }

    private static void assertExceptionMessageContains(Throwable t, String text) {
        Assertions.assertTrue(t.getMessage().toLowerCase(Locale.ROOT).contains(text),
            () -> "Missing '" + text + "' from exception message: " + t.getMessage());
    }

    // NUMBERS-46
    @Test
    void testOneIteration() {
        final double eps = 0.5;
        final double gr = GoldenRatio.getInstance().evaluate(0, eps, 1);
        // Expected: 1 + 1 / 1
        Assertions.assertEquals(2.0, gr);
    }

    // NUMBERS-46
    @Test
    void testTwoIterations() {
        final double eps = 0.25;
        final double gr = GoldenRatio.getInstance().evaluate(0, eps, 2);
        // Expected: 1 + 1 / (1 + 1 / 1)
        Assertions.assertEquals(1.5, gr);
    }
}
