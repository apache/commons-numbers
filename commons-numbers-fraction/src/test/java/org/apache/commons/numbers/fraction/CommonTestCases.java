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
    private static final List<UnaryOperatorTestCase> numDenConstructorTestCasesList;

    private static final List<DoubleToFractionTestCase> doubleConstructorTestCasesList;

    static {
        numDenConstructorTestCasesList = new ArrayList<>();
        numDenConstructorTestCasesList.add(new UnaryOperatorTestCase(0, 1, 0, 1));
        numDenConstructorTestCasesList.add(new UnaryOperatorTestCase(0, 2, 0, 1));
        numDenConstructorTestCasesList.add(new UnaryOperatorTestCase(0, -1, 0, 1));
        numDenConstructorTestCasesList.add(new UnaryOperatorTestCase(1, 2, 1, 2));
        numDenConstructorTestCasesList.add(new UnaryOperatorTestCase(2, 4, 1, 2));
        numDenConstructorTestCasesList.add(new UnaryOperatorTestCase(-1, 2, -1, 2));
        numDenConstructorTestCasesList.add(new UnaryOperatorTestCase(1, -2, -1, 2));
        numDenConstructorTestCasesList.add(new UnaryOperatorTestCase(-2, 4, -1, 2));
        numDenConstructorTestCasesList.add(new UnaryOperatorTestCase(2, -4, -1, 2));

        doubleConstructorTestCasesList = new ArrayList<>();
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(1d/2d, 1, 2));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(1d/3d, 1, 3));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(2d/3d, 2, 3));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(1d/4d, 1, 4));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(3d/4d, 3, 4));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(1d/5d, 1, 5));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(2d/5d, 2, 5));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(3d/5d, 3, 5));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(4d/5d, 4, 5));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(1d/6d, 1, 6));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(5d/6d, 5, 6));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(1d/7d, 1, 7));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(2d/7d, 2, 7));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(3d/7d, 3, 7));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(4d/7d, 4, 7));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(5d/7d, 5, 7));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(6d/7d, 6, 7));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(1d/8d, 1, 8));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(3d/8d, 3, 8));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(5d/8d, 5, 8));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(7d/8d, 7, 8));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(1d/9d, 1, 9));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(2d/9d, 2, 9));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(4d/9d, 4, 9));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(5d/9d, 5, 9));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(7d/9d, 7, 9));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(8d/9d, 8, 9));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(1d/10d, 1, 10));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(3d/10d, 3, 10));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(7d/10d, 7, 10));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(9d/10d, 9, 10));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(1d/11d, 1, 11));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(2d/11d, 2, 11));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(3d/11d, 3, 11));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(4d/11d, 4, 11));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(5d/11d, 5, 11));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(6d/11d, 6, 11));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(7d/11d, 7, 11));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(8d/11d, 8, 11));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(9d/11d, 9, 11));
        doubleConstructorTestCasesList.add(new DoubleToFractionTestCase(10d/11d, 10, 11));
    }

    public static List<UnaryOperatorTestCase> numDenConstructorTestCases() {
        return Collections.unmodifiableList(numDenConstructorTestCasesList);
    }

    public static List<DoubleToFractionTestCase> doubleConstructorTestCases() {
        return Collections.unmodifiableList(doubleConstructorTestCasesList);
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
