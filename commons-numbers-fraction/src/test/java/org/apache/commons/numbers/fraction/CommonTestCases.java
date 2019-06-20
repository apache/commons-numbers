package org.apache.commons.numbers.fraction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A collection of test cases that can be applied both
 * to {@link Fraction} and {@link BigFraction}, e.g.
 * for operations and expected results that involve
 * numerators and denominators in the {@code int} range.
 */
public class CommonTestCases {
    /**
     * See {@link #numDenConstructorTestCases()}
     */
    private static final List<UnaryOperatorTestCase> numDenConstructorTestCasesList;

    /**
     * See {@link #doubleConstructorTestCases()}
     */
    private static final List<DoubleToFractionTestCase> doubleConstructorTestCasesList;

    /**
     * See {@link #absTestCases()}
     */
    private static final List<UnaryOperatorTestCase> absTestCasesList;

    /**
     * See {@link #reciprocalTestCases()}
     */
    private static final List<UnaryOperatorTestCase> reciprocalTestCasesList;

    /**
     * See {@link #negateTestCases()}
     */
    private static final List<UnaryOperatorTestCase> negateTestCasesList;

    static {
        numDenConstructorTestCasesList = collectNumDenConstructorTestCases();
        doubleConstructorTestCasesList = collectDoubleConstructorTestCases();
        absTestCasesList = collectAbsTestCases();
        reciprocalTestCasesList = collectReciprocalTestCases();
        negateTestCasesList = collectNegateTestCases();
    }

    /**
     * Defines test cases as described in {@link #numDenConstructorTestCases()} and collects
     * them into a {@code List}.
     * @return a list of test cases as described above
     */
    private static List<UnaryOperatorTestCase> collectNumDenConstructorTestCases() {
        List<UnaryOperatorTestCase> testCases = new ArrayList<>();

        testCases.add(new UnaryOperatorTestCase(0, 1, 0, 1));
        testCases.add(new UnaryOperatorTestCase(0, 2, 0, 1));
        testCases.add(new UnaryOperatorTestCase(0, -1, 0, 1));
        testCases.add(new UnaryOperatorTestCase(1, 2, 1, 2));
        testCases.add(new UnaryOperatorTestCase(2, 4, 1, 2));
        testCases.add(new UnaryOperatorTestCase(-1, 2, -1, 2));
        testCases.add(new UnaryOperatorTestCase(1, -2, -1, 2));
        testCases.add(new UnaryOperatorTestCase(-2, 4, -1, 2));
        testCases.add(new UnaryOperatorTestCase(2, -4, -1, 2));

        return testCases;
    }

    /**
     * Defines test cases as described in {@link #doubleConstructorTestCases()} and collects
     * them into a {@code List}.
     * @return a list of test cases as described above
     */
    private static List<DoubleToFractionTestCase> collectDoubleConstructorTestCases() {
        List<DoubleToFractionTestCase> testCases = new ArrayList<>();

        testCases.add(new DoubleToFractionTestCase(1d/2d, 1, 2));
        testCases.add(new DoubleToFractionTestCase(1d/3d, 1, 3));
        testCases.add(new DoubleToFractionTestCase(2d/3d, 2, 3));
        testCases.add(new DoubleToFractionTestCase(1d/4d, 1, 4));
        testCases.add(new DoubleToFractionTestCase(3d/4d, 3, 4));
        testCases.add(new DoubleToFractionTestCase(1d/5d, 1, 5));
        testCases.add(new DoubleToFractionTestCase(2d/5d, 2, 5));
        testCases.add(new DoubleToFractionTestCase(3d/5d, 3, 5));
        testCases.add(new DoubleToFractionTestCase(4d/5d, 4, 5));
        testCases.add(new DoubleToFractionTestCase(1d/6d, 1, 6));
        testCases.add(new DoubleToFractionTestCase(5d/6d, 5, 6));
        testCases.add(new DoubleToFractionTestCase(1d/7d, 1, 7));
        testCases.add(new DoubleToFractionTestCase(2d/7d, 2, 7));
        testCases.add(new DoubleToFractionTestCase(3d/7d, 3, 7));
        testCases.add(new DoubleToFractionTestCase(4d/7d, 4, 7));
        testCases.add(new DoubleToFractionTestCase(5d/7d, 5, 7));
        testCases.add(new DoubleToFractionTestCase(6d/7d, 6, 7));
        testCases.add(new DoubleToFractionTestCase(1d/8d, 1, 8));
        testCases.add(new DoubleToFractionTestCase(3d/8d, 3, 8));
        testCases.add(new DoubleToFractionTestCase(5d/8d, 5, 8));
        testCases.add(new DoubleToFractionTestCase(7d/8d, 7, 8));
        testCases.add(new DoubleToFractionTestCase(1d/9d, 1, 9));
        testCases.add(new DoubleToFractionTestCase(2d/9d, 2, 9));
        testCases.add(new DoubleToFractionTestCase(4d/9d, 4, 9));
        testCases.add(new DoubleToFractionTestCase(5d/9d, 5, 9));
        testCases.add(new DoubleToFractionTestCase(7d/9d, 7, 9));
        testCases.add(new DoubleToFractionTestCase(8d/9d, 8, 9));
        testCases.add(new DoubleToFractionTestCase(1d/10d, 1, 10));
        testCases.add(new DoubleToFractionTestCase(3d/10d, 3, 10));
        testCases.add(new DoubleToFractionTestCase(7d/10d, 7, 10));
        testCases.add(new DoubleToFractionTestCase(9d/10d, 9, 10));
        testCases.add(new DoubleToFractionTestCase(1d/11d, 1, 11));
        testCases.add(new DoubleToFractionTestCase(2d/11d, 2, 11));
        testCases.add(new DoubleToFractionTestCase(3d/11d, 3, 11));
        testCases.add(new DoubleToFractionTestCase(4d/11d, 4, 11));
        testCases.add(new DoubleToFractionTestCase(5d/11d, 5, 11));
        testCases.add(new DoubleToFractionTestCase(6d/11d, 6, 11));
        testCases.add(new DoubleToFractionTestCase(7d/11d, 7, 11));
        testCases.add(new DoubleToFractionTestCase(8d/11d, 8, 11));
        testCases.add(new DoubleToFractionTestCase(9d/11d, 9, 11));
        testCases.add(new DoubleToFractionTestCase(10d/11d, 10, 11));

        testCases.add(new DoubleToFractionTestCase(0.00000000000001, 0, 1));
        testCases.add(new DoubleToFractionTestCase(0.40000000000001, 2, 5));
        testCases.add(new DoubleToFractionTestCase(15.0000000000001, 15, 1));

        return testCases;
    }

    /**
     * Defines test cases as described in {@link #collectAbsTestCases()} and collects
     * them into a {@code List}.
     * @return a list of test cases as described above
     */
    private static List<UnaryOperatorTestCase> collectAbsTestCases() {
        List<UnaryOperatorTestCase> testCases = new ArrayList<>();

        testCases.add(new UnaryOperatorTestCase(10, 21, 10, 21));
        testCases.add(new UnaryOperatorTestCase(-10, 21, 10, 21));
        testCases.add(new UnaryOperatorTestCase(10, -21, 10, 21));

        return testCases;
    }

    /**
     * Defines test cases as described in {@link #reciprocalTestCases()} and collects
     * them into a {@code List}.
     * @return a list of test cases as described above
     */
    private static List<UnaryOperatorTestCase> collectReciprocalTestCases() {
        List<UnaryOperatorTestCase> testCases = new ArrayList<>();

        testCases.add(new UnaryOperatorTestCase(50, 75, 3, 2));
        testCases.add(new UnaryOperatorTestCase(4, 3, 3, 4));
        testCases.add(new UnaryOperatorTestCase(-15, 47, -47, 15));
        testCases.add(new UnaryOperatorTestCase(Integer.MAX_VALUE, 1, 1, Integer.MAX_VALUE));

        return testCases;
    }

    /**
     * Defines test cases as described in {@link #negateTestCases()} and collects
     * them into a {@code List}.
     * @return a list of test cases as described above
     */
    private static List<UnaryOperatorTestCase> collectNegateTestCases() {
        List<UnaryOperatorTestCase> testCases = new ArrayList<>();

        testCases.add(new UnaryOperatorTestCase(50, 75, -2, 3));
        testCases.add(new UnaryOperatorTestCase(-50, 75, 2, 3));
        testCases.add(new UnaryOperatorTestCase(Integer.MAX_VALUE - 1, Integer.MAX_VALUE, Integer.MIN_VALUE + 2, Integer.MAX_VALUE));

        return testCases;
    }

    /**
     * Provides a list of test cases where a fraction should be created from a specified
     * numerator and denominator, both in the {@code int} range, and the expected
     * numerator and denominator of the created fraction are also in the {@code int} range.
     * @return a list of test cases as described above
     */
    public static List<UnaryOperatorTestCase> numDenConstructorTestCases() {
        return Collections.unmodifiableList(numDenConstructorTestCasesList);
    }

    /**
     * Provides a list of test cases where a {@code double} value should be converted
     * to a fraction with a certain amount of absolute error allowed, and the expected
     * numerator and denominator of the resulting fraction are in the {@code int} range.
     * @return a list of test cases as described above
     */
    public static List<DoubleToFractionTestCase> doubleConstructorTestCases() {
        return Collections.unmodifiableList(doubleConstructorTestCasesList);
    }

    /**
     * Provides a list of test cases where the absolute value of a fraction created from a specified
     * numerator and denominator, both in the {@code int} range, should be
     * calculated, and the expected
     * numerator and denominator of the resulting fraction are also in the {@code int} range.
     * @return a list of test cases as described above
     */
    public static List<UnaryOperatorTestCase> absTestCases() {
        return Collections.unmodifiableList(absTestCasesList);
    }

    /**
     * Provides a list of test cases where the multiplicative inverse of a fraction created from a specified
     * numerator and denominator, both in the {@code int} range, should be
     * calculated, and the expected
     * numerator and denominator of the resulting fraction are also in the {@code int} range.
     * @return a list of test cases as described above
     */
    public static List<UnaryOperatorTestCase> reciprocalTestCases() {
        return Collections.unmodifiableList(reciprocalTestCasesList);
    }

    /**
     * Provides a list of test cases where the additive inverse of a fraction created from a specified
     * numerator and denominator, both in the {@code int} range, should be
     * calculated, and the expected
     * numerator and denominator of the resulting fraction are also in the {@code int} range.
     * @return a list of test cases as described above
     */
    public static List<UnaryOperatorTestCase> negateTestCases() {
        return Collections.unmodifiableList(negateTestCasesList);
    }

    /**
     * Represents a test case where a unary operation should be performed on a specified combination
     * of numerator and denominator, both in the {@code int} range, and the numerator and
     * denominator of the expected result are also in the {@code int} range.
     */
    public static class UnaryOperatorTestCase {
        public final int operandNumerator;
        public final int operandDenominator;
        public final int expectedNumerator;
        public final int expectedDenominator;

        public UnaryOperatorTestCase(
                int operandNumerator,
                int operandDenominator,
                int expectedNumerator,
                int expectedDenominator) {
            this.operandNumerator = operandNumerator;
            this.operandDenominator = operandDenominator;
            this.expectedNumerator = expectedNumerator;
            this.expectedDenominator = expectedDenominator;
        }
    }

    /**
     * Represents a test case where an operation that yields a fraction should be performed
     * on a {@code double} value and the numerator and denominator of the expected result
     * are in the {@code int} range.
     */
    public static class DoubleToFractionTestCase {
        public final double operand;
        public final int expectedNumerator;
        public final int expectedDenominator;

        public DoubleToFractionTestCase(
                double operand,
                int expectedNumerator,
                int expectedDenominator) {
            this.operand = operand;
            this.expectedNumerator = expectedNumerator;
            this.expectedDenominator = expectedDenominator;
        }
    }
}
