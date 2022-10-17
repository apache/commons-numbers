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

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Supplier;
import org.apache.commons.numbers.fraction.GeneralizedContinuedFraction.Coefficient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link GeneralizedContinuedFraction}.
 */
class GeneralizedContinuedFractionTest {
    /** Golden ratio constant. */
    private static final double GOLDEN_RATIO = 1.618033988749894848204586834365638117720309;
    /** Reciprocal of the golden ratio constant. */
    private static final double INV_GOLDEN_RATIO = 0.618033988749894848204586834365638117720309;

    @Test
    void testRelativeErrorAssumptions() {
        Assertions.assertEquals(0x1.0p-52, 1 / (1.0 - 0x1.0p-53) - 1, "Default absolute threshold");
        for (double eps = 0x1.0p-53; eps <= 0.5; eps *= 2) {
            final double low = 1 - eps;
            final double high = 1 / low;
            Assertions.assertTrue(high - 1.0 >= eps);
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {1e-6, 1e-10})
    void testGoldenRatioA(double eps) {
        final double actual = GeneralizedContinuedFraction.value(GoldenRatio.getInstance(), eps);
        Assertions.assertEquals(GOLDEN_RATIO, actual, GOLDEN_RATIO * eps);
    }

    /**
     * Test a continued fraction where the leading term is added after evaluation.
     * Evaluates the golden ratio.
     */
    @ParameterizedTest
    @ValueSource(doubles = {1e-6, 1e-10})
    void testGoldenRatioB(double eps) {
        final double actual = GeneralizedContinuedFraction.value(1, GoldenRatio.getInstance(), eps);
        Assertions.assertEquals(GOLDEN_RATIO, actual, GOLDEN_RATIO * eps);
    }

    /**
     * Test a continued fraction where the leading term is zero.
     * Evaluates the reciprocal of the golden ratio.
     */
    @ParameterizedTest
    @ValueSource(doubles = {1e-6, 1e-10})
    void testGoldenRatioC(double eps) {
        final double actual = GeneralizedContinuedFraction.value(0, GoldenRatio.getInstance(), eps);
        Assertions.assertEquals(INV_GOLDEN_RATIO, actual, INV_GOLDEN_RATIO * eps);
    }

    /**
     * Test a continued fraction where the leading term is not part of a series.
     * Evaluates the base of the natural logarithm.
     */
    @ParameterizedTest
    @ValueSource(doubles = {1e-6, 1e-10})
    void testBaseE(double eps) {
        final double actual = GeneralizedContinuedFraction.value(2, new E(), eps);
        Assertions.assertEquals(Math.E, actual, Math.E * eps);
    }

    /**
     * Test a continued fraction where the leading term is zero.
     * Evaluates tan(x) which approaches zero when {@code x -> 0}.
     */
    @ParameterizedTest
    @CsvSource(value = {
        "0.5, 1e-6",
        "1.0, 1e-6",
        "2.0, 1e-6",
        "0.5, 1e-10",
        "1.0, 1e-10",
        "2.0, 1e-10",
        // tan(x) -> 0 as x -> 0
        "1e-5, 1e-10",
        "1e-10, 1e-10",
        "1e-100, 1e-10",
        "1e-200, 1e-10",
    })
    void testTanX(double x, double eps) {
        final double expected = Math.tan(x);
        final double actual = GeneralizedContinuedFraction.value(0, new Tan(x), eps);
        Assertions.assertEquals(expected, actual, Math.abs(expected) * eps);
    }

    /**
     * Test a continued fraction where the leading term is zero.
     * Evaluates tan(x) which approaches zero when {@code x -> 0}.
     * This uses a different formulation to {@link #testTanX(double, double)} which
     * is more accurate when {@code x < 1}. This test demonstrates that the optimum
     * evaluation requires testing of different implementations of the continued fraction.
     */
    @ParameterizedTest
    @CsvSource(value = {
        // Cases known to be more accurate when the fraction is advanced 1 term.
        // Answers are provided to avoid JDK variations in Math.tan(x).
        // https://keisan.casio.com/calculator
        "0.5, 0.5463024898437905132552",
        "0.125, 0.1256551365751309677927",
        "0.0078125, 0.007812658949600007638479",
    })
    void testTanX2(double x, double expected) {
        final double actual1 = GeneralizedContinuedFraction.value(0, new Tan(x));
        Assertions.assertNotEquals(expected, actual1, "Standard formulation");
        Assertions.assertEquals(expected, actual1, 2 * Math.ulp(expected));
        // Advance 1 term
        final Tan t = new Tan(x);
        final Coefficient c = t.get();
        final double actual2 = c.getA() / GeneralizedContinuedFraction.value(c.getB(), t);
        Assertions.assertEquals(expected, actual2, "Revised formulation");
    }

    /**
     * Test an invalid epsilon (big, very small, zero, negative or NaN).
     * This test ensures the behaviour when passed an invalid epsilon is to attempt to
     * converge rather than throw an exception.
     * See NUMBERS-173.
     */
    @ParameterizedTest
    @ValueSource(doubles = {1.0, 0x1.0p-100, 0, -1, Double.NaN})
    void testGoldenRatioAInvalidEpsilon(double eps) {
        final double actual = GeneralizedContinuedFraction.value(GoldenRatio.getInstance(), eps, 10000);
        // An invalid epsilon is set to the default epsilon.
        // It should converge to 1 ULP.
        Assertions.assertEquals(GOLDEN_RATIO, actual, Math.ulp(GOLDEN_RATIO));
    }

    /**
     * Test an invalid epsilon (big, very small, zero, negative or NaN).
     * This test ensures the behaviour when passed an invalid epsilon is to attempt to
     * converge rather than throw an exception.
     * See NUMBERS-173.
     */
    @ParameterizedTest
    @ValueSource(doubles = {1.0, 0x1.0p-100, 0, -1, Double.NaN})
    void testGoldenRatioBInvalidEpsilon(double eps) {
        final double actual = GeneralizedContinuedFraction.value(1, GoldenRatio.getInstance(), eps, 10000);
        // An invalid epsilon is set to the default epsilon.
        // It should converge to 1 ULP.
        Assertions.assertEquals(GOLDEN_RATIO, actual, Math.ulp(GOLDEN_RATIO));
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

        final Supplier<Coefficient> gen = simpleContinuedFraction(4, 2, 6, 7);

        final double actual = GeneralizedContinuedFraction.value(gen);
        final double expected = 415.0 / 93;
        Assertions.assertEquals(expected, actual, Math.ulp(expected));
    }

    @Test
    void test43Over93() {
        // As per 415 / 93 but modified to remove leading 4 term.
        // expected = (415 - 4 * 93) / 93 = 43 / 93

        final Supplier<Coefficient> gen = simpleContinuedFraction(2, 6, 7);

        // Evaluate with b0 = 0
        final double actual = GeneralizedContinuedFraction.value(0, gen);
        final double expected = 43.0 / 93;
        Assertions.assertEquals(expected, actual, Math.ulp(expected));
    }

    @Test
    void test0Over93() {
        // As per 415 / 93 but modified to change the leading 4 term to be equal
        // to the fraction component so the result is zero. This is a badly
        // constructed simple continued fraction as the initial convergent (-43/93)
        // is not an approximation for the result of 0. This tests the algorithm
        // can converge close to zero.

        final Supplier<Coefficient> gen = simpleContinuedFraction(-43.0 / 93, 2, 6, 7);

        final double actual = GeneralizedContinuedFraction.value(gen);
        Assertions.assertEquals(0, actual, Math.ulp(0.5));
    }

    @Test
    void testMaxIterationsThrowsA() {
        final double eps = Math.ulp(1.0);

        // This works when enough terms are used
        Assertions.assertEquals(GOLDEN_RATIO,
            GeneralizedContinuedFraction.value(GoldenRatio.getInstance(), eps, 100), GOLDEN_RATIO * eps);

        final GoldenRatio gr = GoldenRatio.getInstance();
        final Throwable t = Assertions.assertThrows(ArithmeticException.class,
            () -> GeneralizedContinuedFraction.value(gr, eps, 3));
        assertExceptionMessageContains(t, "max");
    }

    @Test
    void testMaxIterationsThrowsB() {
        final double eps = Math.ulp(1.0);

        // This works when enough terms are used
        Assertions.assertEquals(INV_GOLDEN_RATIO,
            GeneralizedContinuedFraction.value(0, GoldenRatio.getInstance(), eps, 100), INV_GOLDEN_RATIO * eps);

        final GoldenRatio gr = GoldenRatio.getInstance();
        final Throwable t = Assertions.assertThrows(ArithmeticException.class,
            () -> GeneralizedContinuedFraction.value(0, gr, eps, 3));
        assertExceptionMessageContains(t, "max");
    }

    @Test
    void testNaNThrowsA() {
        // Create a NaN during the iteration
        final Supplier<Coefficient> gen = simpleContinuedFraction(1, 1, Double.NaN);

        final Throwable t = Assertions.assertThrows(FractionException.class,
            () -> GeneralizedContinuedFraction.value(gen));
        assertExceptionMessageContains(t, "nan");
    }

    @Test
    void testNaNThrowsB() {
        // Create a NaN during the iteration
        final Supplier<Coefficient> gen = simpleContinuedFraction(1, 1, Double.NaN);

        final Throwable t = Assertions.assertThrows(FractionException.class,
            () -> GeneralizedContinuedFraction.value(0, gen));
        assertExceptionMessageContains(t, "nan");
    }

    /**
     * Evaluate a fraction with very large numerator coefficient that creates infinity
     * for the update step.
     * <pre>
     *                 max
     *    0.5 + ---------------------
     *          0.5 +      max
     *                ---------------
     *                0.5 +    max
     *                      ---------
     *                      0.5 + ...
     * </pre>
     */
    @Test
    void testInfThrowsA() {
        // Create an infinity during the iteration:
        // cN = b + a / cPrev => b_1 + a_1 / b_0 => 0.5 + Double.MAX_VALUE / 0.5
        // deltaN = cN * dN -> infinity for the update
        final Supplier<Coefficient> gen = () -> Coefficient.of(Double.MAX_VALUE, 0.5);
        final Throwable t = Assertions.assertThrows(FractionException.class,
            () -> GeneralizedContinuedFraction.value(gen));
        assertExceptionMessageContains(t, "infinity");
    }

    /**
     * Evaluate a fraction with very large numerator coefficient that creates infinity
     * for the update step.
     * <pre>
     *                 max
     *    0.0 + ---------------------
     *          0.5 +      max
     *                ---------------
     *                0.5 +    max
     *                      ---------
     *                      0.5 + ...
     * </pre>
     */
    @Test
    void testInfThrowsB() {
        // Create an infinity during the iteration.
        // This is created as per the method above as the continued fraction
        // is evaluated with the same terms and divided into a0.

        final Supplier<Coefficient> gen = () -> Coefficient.of(Double.MAX_VALUE, 0.5);
        final Throwable t = Assertions.assertThrows(FractionException.class,
            () -> GeneralizedContinuedFraction.value(0, gen));
        assertExceptionMessageContains(t, "infinity");
    }

    /**
     * Evaluate a fraction with very large numerator coefficient that creates zero for
     * the update step.
     * <pre>
     *                   1
     *    0.5 + ---------------------
     *          0.5 +       max
     *                ---------------
     *                0.5 +    max
     *                      ---------
     *                      0.5 + ...
     * </pre>
     */
    @Test
    void testZeroThrowsA() {
        // Create an infinity during the iteration:
        // dN = b + a * dPrev => b_2 + a_2 * 2.0 => 0.5 + Double.MAX_VALUE * 2.0
        // deltaN = cN * 1/dN -> 0 for the update.

        final Supplier<Coefficient> gen = new Supplier<Coefficient>() {
            private int n;

            @Override
            public Coefficient get() {
                final double a = n++ < 2 ? 1 : Double.MAX_VALUE;
                return Coefficient.of(a, 0.5);
            }
        };
        final Throwable t = Assertions.assertThrows(FractionException.class,
            () -> GeneralizedContinuedFraction.value(gen, 1e-10, 100));

        // If the zero update step is not detected with the current coefficients
        // of (max, 0.5) the algorithm continues up to the maximum iterations.
        assertExceptionMessageContains(t, "zero");
    }

    /**
     * Evaluate a fraction with very large numerator coefficient that creates zero for
     * the update step.
     * <pre>
     *                   1
     *    0.0 + ---------------------
     *          0.5 +       1
     *                ---------------
     *                0.5 +    max
     *                      ---------
     *                      0.5 + ...
     * </pre>
     */
    @Test
    void testZeroThrowsB() {
        // Create an infinity during the iteration.
        // This is created as per the method above as the continued fraction
        // is evaluated with the same terms and divided into a0.

        final Supplier<Coefficient> gen = new Supplier<Coefficient>() {
            private int n;

            @Override
            public Coefficient get() {
                final double a = n++ < 2 ? 1 : Double.MAX_VALUE;
                return Coefficient.of(a, 0.5);
            }
        };
        final Throwable t = Assertions.assertThrows(FractionException.class,
            () -> GeneralizedContinuedFraction.value(0, gen, 1e-10, 100));

        // If the zero update step is not detected with the current coefficients
        // of (max, 0.5) the algorithm continues up to the maximum iterations.
        assertExceptionMessageContains(t, "zero");
    }

    private static void assertExceptionMessageContains(Throwable t, String text) {
        Assertions.assertTrue(t.getMessage().toLowerCase(Locale.ROOT).contains(text),
            () -> "Missing '" + text + "' from exception message: " + t.getMessage());
    }

    /**
     * Evaluate a fraction where the initial term is negative and close to zero.
     * The algorithm should be set the initial term to a small value
     * of the same sign. The current close to zero threshold is 1e-50.
     * Ensure the magnitude of the fraction component is larger. If smaller then the result
     * will always be around the small threshold of 1e-50.
     * <pre>
     *              1
     *    -1e-51 - ----
     *             1e49
     * </pre>
     * <p>If the sign is not preserved the result is incorrect.
     */
    @Test
    void testCloseToZeroB0() {
        final double b0 = -1e-51;
        final double b1 = -1e49;

        final double expected = b0 + 1 / b1;
        // Recommended when b0 is small to pass this separately
        Assertions.assertEquals(expected, GeneralizedContinuedFraction.value(b0, simpleContinuedFraction(b1)));
        // This should be close. The sign should not be different.
        Assertions.assertEquals(expected,
            GeneralizedContinuedFraction.value(simpleContinuedFraction(b0, b1)), Math.abs(expected) * 0.1);

        // Check the negation
        Assertions.assertEquals(-expected, GeneralizedContinuedFraction.value(-b0, simpleContinuedFraction(-b1)));
        Assertions.assertEquals(-expected,
            GeneralizedContinuedFraction.value(simpleContinuedFraction(-b0, -b1)), Math.abs(expected) * 0.1);
    }

    /**
     * Evaluate simple continued fractions.
     */
    @ParameterizedTest
    @CsvSource(value = {
        // Generated using matlab, e.g. rat(34.567, 1e-15)
        "34.567, 35:-2:-3:-4:-3:-10",
        "-4.788, -5:5:-4:2:7",
        "67.492, 67:2:31:-4",
        "0.6782348, 1:-3:-9:-4:3:7:-19:3:-2:-11",
    })
    void testSimpleContinuedFraction(double x, String scf) {
        final double[] b = Arrays.stream(scf.split(":")).mapToDouble(Double::parseDouble).toArray();
        Assertions.assertEquals(x,
            GeneralizedContinuedFraction.value(simpleContinuedFraction(b)), Math.abs(x) * 1e-15);
    }

    /**
     * Test using an epsilon that converges after one iteration.
     * This should not raise an exception.
     * See NUMBERS-46.
     */
    @Test
    void testOneIteration() {
        final double eps = 0.5;
        final double gr = GeneralizedContinuedFraction.value(GoldenRatio.getInstance(), eps, 1);
        // Expected: 1 + 1 / 1
        Assertions.assertEquals(2.0, gr);
    }

    /**
     * Test using an epsilon that converges after two iterations.
     * This should not raise an exception.
     * See NUMBERS-46.
     */
    @Test
    void testTwoIterations() {
        final double eps = 0.25;
        final double gr = GeneralizedContinuedFraction.value(GoldenRatio.getInstance(), eps, 2);
        // Expected: 1 + 1 / (1 + 1 / 1)
        Assertions.assertEquals(1.5, gr);
    }

    /**
     * Series to compute the golden ratio:<br>
     * https://mathworld.wolfram.com/GoldenRatio.html<br>
     * Continued fraction eq. 17.
     * <pre>
     *                1
     * GR = 1 + ---------------
     *          1 +      1
     *              -----------
     *              1 +    1
     *                  -------
     *                  1 + ...
     * </pre>
     */
    private static class GoldenRatio implements Supplier<Coefficient> {
        private static final GoldenRatio INSTANCE = new GoldenRatio();
        private static final Coefficient RESULT = Coefficient.of(1, 1);

        /**
         * @return single instance of GoldenRatio
         */
        static GoldenRatio getInstance() {
            return INSTANCE;
        }

        @Override
        public Coefficient get() {
            return RESULT;
        }
    }

    /**
     * Series to compute the base of the natural logarithm e:<br>
     * https://en.wikipedia.org/wiki/Continued_fraction#Regular_patterns_in_continued_fractions<br>
     * <pre>
     * e = [2; 1, 2, 1, 1, 4, 1, 1, 6, 1, ...]
     * </pre>
     * <p>The term 2 must be added to the generated series.
     */
    private static class E implements Supplier<Coefficient> {
        private int n;

        @Override
        public Coefficient get() {
            n++;
            // Repeating series of 3:
            // [1, t, 1] where t = 2 + 2 n/3
            final double t = n % 3 == 2 ? 2 + 2 * (n / 3) : 1;
            return Coefficient.of(1, t);
        }
    }

    /**
     * Series to compute tan(x):<br>
     * https://www.boost.org/doc/libs/1_77_0/libs/math/doc/html/math_toolkit/internals/cf.html<br/>
     * <pre>
     *                z
     * tan(z) = ---------------
     *          1 -     z^2
     *              -----------
     *              3 -   z^2
     *                  -------
     *                  5 - ...
     * </pre>
     */
    static class Tan implements Supplier<Coefficient> {
        private double a;
        private double b = -1;

        /**
         * @param z Argument z
         */
        Tan(double z) {
            a = z;
        }

        @Override
        public Coefficient get() {
            b += 2;
            // Special first case
            if (b == 1) {
                final double z = a;
                // Update the term for the rest of the fraction.
                // The continuant is subtracted from the b terms, thus all the
                // remaining a terms are negative.
                a *= -z;
                return Coefficient.of(z, b);
            }
            return Coefficient.of(a, b);
        }
    }

    /**
     * Create a generator for a simple continued fraction.
     * <pre>
     *              1
     * b0 + ------------------
     *      b1 +      1
     *           -------------
     *           b2 +    1
     *                --------
     *                b3 + ...
     * </pre>
     *
     * @param coefficients b coefficients
     * @return the generator
     */
    static Supplier<Coefficient> simpleContinuedFraction(double... coefficients) {
        return new Supplier<Coefficient>() {
            /* iteration. */
            private int n;
            /* denominator terms. */
            private final double[] b = coefficients.clone();

            @Override
            public Coefficient get() {
                if (n != b.length) {
                    // Return the next term
                    return Coefficient.of(1, b[n++]);
                }
                return Coefficient.of(0, 1);
            }
        };
    }
}
