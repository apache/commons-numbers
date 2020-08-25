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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A collection of test cases that can be applied both to {@link Fraction}
 * and {@link BigFraction}, e.g. for operations and expected results that
 * involve numerators and denominators in the {@code int} range.
 */
final class CommonTestCases {

    /**
     * See {@link #numDenConstructorTestCases()}
     */
    private static final List<UnaryOperatorTestCase> numDenConstructorTestCasesList;

    /**
     * See {@link #doubleConstructorTestCases()}
     */
    private static final List<DoubleToFractionTestCase> doubleConstructorTestCasesList;

    /**
     * See {@link #doubleMaxDenomConstructorTestCases()}
     */
    private static final List<DoubleToFractionTestCase> doubleMaxDenomConstructorTestCasesList;

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

    /**
     * See {@link #addFractionTestCases()}
     */
    private static final List<BinaryOperatorTestCase> addFractionTestCasesList;

    /**
     * See {@link #addIntTestCases()}
     */
    private static final List<BinaryIntOperatorTestCase> addIntTestCasesList;

    /**
     * See {@link #divideByFractionTestCases()}
     */
    private static final List<BinaryOperatorTestCase> divideByFractionTestCasesList;

    /**
     * See {@link #divideByIntTestCases()}
     */
    private static final List<BinaryIntOperatorTestCase> divideByIntTestCasesList;

    /**
     * See {@link #multiplyByFractionTestCases()}
     */
    private static final List<BinaryOperatorTestCase> multiplyByFractionTestCasesList;

    /**
     * See {@link #multiplyByIntTestCases()}
     */
    private static final List<BinaryIntOperatorTestCase> multiplyByIntTestCasesList;

    /**
     * See {@link #subtractFractionTestCases()}
     */
    private static final List<BinaryOperatorTestCase> subtractFractionTestCasesList;

    /**
     * See {@link #subtractIntTestCases()}
     */
    private static final List<BinaryIntOperatorTestCase> subtractIntTestCasesList;

    /**
     * See {@link #powTestCases()}
     */
    private static final List<BinaryIntOperatorTestCase> powTestCasesList;

    static {
        numDenConstructorTestCasesList = collectNumDenConstructorTestCases();
        doubleConstructorTestCasesList = collectDoubleConstructorTestCases();
        doubleMaxDenomConstructorTestCasesList = collectDoubleMaxDenomConstructorTestCases();
        absTestCasesList = collectAbsTestCases();
        reciprocalTestCasesList = collectReciprocalTestCases();
        negateTestCasesList = collectNegateTestCases();
        addFractionTestCasesList = collectAddFractionTestCases();
        addIntTestCasesList = collectAddIntTestCases();
        divideByFractionTestCasesList = collectDivideByFractionTestCases();
        divideByIntTestCasesList = collectDivideByIntTestCases();
        multiplyByFractionTestCasesList = collectMultiplyByFractionTestCases();
        multiplyByIntTestCasesList = collectMultiplyByIntTestCases();
        subtractFractionTestCasesList = collectSubtractFractionTestCases();
        subtractIntTestCasesList = collectSubtractIntTestCases();
        powTestCasesList = collectPowTestCases();
    }

    private CommonTestCases() {}

    /**
     * Defines test cases as described in
     * {@link #numDenConstructorTestCases()} and collects them into a {@code
     * List}.
     * @return a list of test cases as described above
     */
    private static List<UnaryOperatorTestCase> collectNumDenConstructorTestCases() {
        final List<UnaryOperatorTestCase> testCases = new ArrayList<>();

        testCases.add(new UnaryOperatorTestCase(0, 1, 0, 1));
        testCases.add(new UnaryOperatorTestCase(0, 2, 0, 1));
        testCases.add(new UnaryOperatorTestCase(0, -1, 0, 1));
        testCases.add(new UnaryOperatorTestCase(1, 2, 1, 2));
        testCases.add(new UnaryOperatorTestCase(2, 4, 1, 2));
        testCases.add(new UnaryOperatorTestCase(-1, 2, -1, 2));
        testCases.add(new UnaryOperatorTestCase(1, -2, 1, -2));
        testCases.add(new UnaryOperatorTestCase(-2, 4, -1, 2));
        testCases.add(new UnaryOperatorTestCase(2, -4, 1, -2));
        testCases.add(new UnaryOperatorTestCase(-2, -4, -1, -2));

        testCases.add(new UnaryOperatorTestCase(2, Integer.MIN_VALUE, 1, Integer.MIN_VALUE / 2));
        testCases.add(new UnaryOperatorTestCase(Integer.MIN_VALUE, -2, -Integer.MIN_VALUE / 2, -1));

        return testCases;
    }

    /**
     * Defines test cases as described in
     * {@link #doubleConstructorTestCases()} and collects them into a {@code List}.
     * @return a list of test cases as described above
     */
    private static List<DoubleToFractionTestCase> collectDoubleConstructorTestCases() {
        final List<DoubleToFractionTestCase> testCases = new ArrayList<>();

        testCases.add(new DoubleToFractionTestCase(1d / 2d, 1, 2));
        testCases.add(new DoubleToFractionTestCase(1d / 3d, 1, 3));
        testCases.add(new DoubleToFractionTestCase(2d / 3d, 2, 3));
        testCases.add(new DoubleToFractionTestCase(1d / 4d, 1, 4));
        testCases.add(new DoubleToFractionTestCase(3d / 4d, 3, 4));
        testCases.add(new DoubleToFractionTestCase(1d / 5d, 1, 5));
        testCases.add(new DoubleToFractionTestCase(2d / 5d, 2, 5));
        testCases.add(new DoubleToFractionTestCase(3d / 5d, 3, 5));
        testCases.add(new DoubleToFractionTestCase(4d / 5d, 4, 5));
        testCases.add(new DoubleToFractionTestCase(1d / 6d, 1, 6));
        testCases.add(new DoubleToFractionTestCase(5d / 6d, 5, 6));
        testCases.add(new DoubleToFractionTestCase(1d / 7d, 1, 7));
        testCases.add(new DoubleToFractionTestCase(2d / 7d, 2, 7));
        testCases.add(new DoubleToFractionTestCase(3d / 7d, 3, 7));
        testCases.add(new DoubleToFractionTestCase(4d / 7d, 4, 7));
        testCases.add(new DoubleToFractionTestCase(5d / 7d, 5, 7));
        testCases.add(new DoubleToFractionTestCase(6d / 7d, 6, 7));
        testCases.add(new DoubleToFractionTestCase(1d / 8d, 1, 8));
        testCases.add(new DoubleToFractionTestCase(3d / 8d, 3, 8));
        testCases.add(new DoubleToFractionTestCase(5d / 8d, 5, 8));
        testCases.add(new DoubleToFractionTestCase(7d / 8d, 7, 8));
        testCases.add(new DoubleToFractionTestCase(1d / 9d, 1, 9));
        testCases.add(new DoubleToFractionTestCase(2d / 9d, 2, 9));
        testCases.add(new DoubleToFractionTestCase(4d / 9d, 4, 9));
        testCases.add(new DoubleToFractionTestCase(5d / 9d, 5, 9));
        testCases.add(new DoubleToFractionTestCase(7d / 9d, 7, 9));
        testCases.add(new DoubleToFractionTestCase(8d / 9d, 8, 9));
        testCases.add(new DoubleToFractionTestCase(1d / 10d, 1, 10));
        testCases.add(new DoubleToFractionTestCase(3d / 10d, 3, 10));
        testCases.add(new DoubleToFractionTestCase(7d / 10d, 7, 10));
        testCases.add(new DoubleToFractionTestCase(9d / 10d, 9, 10));
        testCases.add(new DoubleToFractionTestCase(1d / 11d, 1, 11));
        testCases.add(new DoubleToFractionTestCase(2d / 11d, 2, 11));
        testCases.add(new DoubleToFractionTestCase(3d / 11d, 3, 11));
        testCases.add(new DoubleToFractionTestCase(4d / 11d, 4, 11));
        testCases.add(new DoubleToFractionTestCase(5d / 11d, 5, 11));
        testCases.add(new DoubleToFractionTestCase(6d / 11d, 6, 11));
        testCases.add(new DoubleToFractionTestCase(7d / 11d, 7, 11));
        testCases.add(new DoubleToFractionTestCase(8d / 11d, 8, 11));
        testCases.add(new DoubleToFractionTestCase(9d / 11d, 9, 11));
        testCases.add(new DoubleToFractionTestCase(10d / 11d, 10, 11));

        testCases.add(new DoubleToFractionTestCase(-1d / 2d, -1, 2));
        testCases.add(new DoubleToFractionTestCase(-1d / 3d, -1, 3));

        testCases.add(new DoubleToFractionTestCase(0.00000000000001, 0, 1));
        testCases.add(new DoubleToFractionTestCase(0.40000000000001, 2, 5));
        testCases.add(new DoubleToFractionTestCase(15.0000000000001, 15, 1));
        testCases.add(new DoubleToFractionTestCase(15.0, 15, 1));
        testCases.add(new DoubleToFractionTestCase(0.0, 0, 1));
        testCases.add(new DoubleToFractionTestCase(-0.0, 0, 1));

        return testCases;
    }

    /**
     * Defines test cases as described in
     * {@link #doubleMaxDenomConstructorTestCases()} and collects them into a {@code List}.
     * @return a list of test cases as described above
     */
    private static List<DoubleToFractionTestCase> collectDoubleMaxDenomConstructorTestCases() {
        final List<DoubleToFractionTestCase> testCases = new ArrayList<>();
        testCases.add(new DoubleToFractionTestCase(0.4,   9, 2, 5));
        testCases.add(new DoubleToFractionTestCase(0.4,  99, 2, 5));
        testCases.add(new DoubleToFractionTestCase(0.4, 999, 2, 5));
        testCases.add(new DoubleToFractionTestCase(-0.4,   9, -2, 5));
        testCases.add(new DoubleToFractionTestCase(-0.4,  99, -2, 5));
        testCases.add(new DoubleToFractionTestCase(-0.4, 999, -2, 5));

        testCases.add(new DoubleToFractionTestCase(0.6152,    9, 3, 5));
        testCases.add(new DoubleToFractionTestCase(0.6152,   99, 8, 13));
        testCases.add(new DoubleToFractionTestCase(0.6152,  999, 510, 829));
        testCases.add(new DoubleToFractionTestCase(0.6152, 9999, 769, 1250));
        testCases.add(new DoubleToFractionTestCase(-0.6152,    9, -3, 5));
        testCases.add(new DoubleToFractionTestCase(-0.6152,   99, -8, 13));
        testCases.add(new DoubleToFractionTestCase(-0.6152,  999, -510, 829));
        testCases.add(new DoubleToFractionTestCase(-0.6152, 9999, -769, 1250));

        // Underflow
        testCases.add(new DoubleToFractionTestCase(0x1.0p-40, Integer.MAX_VALUE, 0, 1));
        testCases.add(new DoubleToFractionTestCase(-0x1.0p-40, Integer.MAX_VALUE, 0, 1));

        // Overflow
        testCases.add(new DoubleToFractionTestCase(Math.nextUp((double) Integer.MAX_VALUE), Integer.MIN_VALUE, Integer.MAX_VALUE, 1));
        testCases.add(new DoubleToFractionTestCase(-Math.nextUp((double) Integer.MAX_VALUE), Integer.MIN_VALUE, -Integer.MAX_VALUE, 1));
        testCases.add(new DoubleToFractionTestCase(Math.nextUp((double) Integer.MAX_VALUE) / (1 << 15), Integer.MIN_VALUE, Integer.MAX_VALUE, 1 << 15));
        testCases.add(new DoubleToFractionTestCase(-Math.nextUp((double) Integer.MAX_VALUE) / (1 << 15), Integer.MIN_VALUE, -Integer.MAX_VALUE, 1 << 15));
        testCases.add(new DoubleToFractionTestCase(Math.nextUp(1.0), Integer.MIN_VALUE, 1, 1));
        testCases.add(new DoubleToFractionTestCase(-Math.nextUp(1.0), Integer.MIN_VALUE, -1, 1));

        // MATH-996
        testCases.add(new DoubleToFractionTestCase(0.5000000001, 10, 1, 2));
        testCases.add(new DoubleToFractionTestCase(-0.5000000001, 10, -1, 2));

        // NUMBERS-147
        testCases.add(new DoubleToFractionTestCase(Integer.MAX_VALUE * 1.0, 2, Integer.MAX_VALUE, 1));
        testCases.add(new DoubleToFractionTestCase(Integer.MAX_VALUE * -1.0, 2, -Integer.MAX_VALUE, 1));
        testCases.add(new DoubleToFractionTestCase(1.0 / Integer.MAX_VALUE, Integer.MAX_VALUE, 1, Integer.MAX_VALUE));
        testCases.add(new DoubleToFractionTestCase(-1.0 / Integer.MAX_VALUE, Integer.MAX_VALUE, -1, Integer.MAX_VALUE));
        testCases.add(new DoubleToFractionTestCase(Integer.MIN_VALUE * 1.0, 2, Integer.MIN_VALUE, 1));

        // Using 2^31:
        // Representations in Fraction and BigFraction are different since BigFraction
        // can use +2^31 but Fraction is limited to -2^31
        // .from(Integer.MIN_VALUE * -1.0, 2)
        // .from(Integer.MIN_VALUE / -3.0, Integer.MIN_VALUE)
        // .from(1.0 / Integer.MIN_VALUE, Integer.MIN_VALUE)
        // .from(-1.0 / Integer.MIN_VALUE, Integer.MIN_VALUE)

        return testCases;
    }

    /**
     * Defines test cases as described in {@link #absTestCases()} and
     * collects them into a {@code List}.
     * @return a list of test cases as described above
     */
    private static List<UnaryOperatorTestCase> collectAbsTestCases() {
        final List<UnaryOperatorTestCase> testCases = new ArrayList<>();

        testCases.add(new UnaryOperatorTestCase(10, 21, 10, 21));
        testCases.add(new UnaryOperatorTestCase(-11, 23, 11, 23));
        testCases.add(new UnaryOperatorTestCase(13, -24, -13, -24));
        testCases.add(new UnaryOperatorTestCase(0, 1, 0, 1));
        testCases.add(new UnaryOperatorTestCase(0, -1, 0, 1));

        return testCases;
    }

    /**
     * Defines test cases as described in {@link #reciprocalTestCases()} and
     * collects them into a {@code List}.
     * @return a list of test cases as described above
     */
    private static List<UnaryOperatorTestCase> collectReciprocalTestCases() {
        final List<UnaryOperatorTestCase> testCases = new ArrayList<>();

        testCases.add(new UnaryOperatorTestCase(50, 75, 3, 2));
        testCases.add(new UnaryOperatorTestCase(4, 3, 3, 4));
        testCases.add(new UnaryOperatorTestCase(-15, 47, 47, -15));
        testCases.add(new UnaryOperatorTestCase(Integer.MAX_VALUE, 1, 1, Integer.MAX_VALUE));

        return testCases;
    }

    /**
     * Defines test cases as described in {@link #negateTestCases()} and
     * collects them into a {@code List}.
     * @return a list of test cases as described above
     */
    private static List<UnaryOperatorTestCase> collectNegateTestCases() {
        final List<UnaryOperatorTestCase> testCases = new ArrayList<>();

        testCases.add(new UnaryOperatorTestCase(50, 75, -2, 3));
        testCases.add(new UnaryOperatorTestCase(-50, 75, 2, 3));
        testCases.add(new UnaryOperatorTestCase(Integer.MAX_VALUE - 1, Integer.MAX_VALUE, Integer.MIN_VALUE + 2, Integer.MAX_VALUE));
        testCases.add(new UnaryOperatorTestCase(1, Integer.MIN_VALUE, -1, Integer.MIN_VALUE));
        testCases.add(new UnaryOperatorTestCase(0, 1, 0, 1));
        testCases.add(new UnaryOperatorTestCase(0, -1, 0, 1));

        // XXX Failed by "BigFraction" (whose implementation differs from "Fraction").
        // These are tested explicitly in FractionTest.
        // testCases.add(new UnaryOperatorTestCase(Integer.MIN_VALUE, Integer.MIN_VALUE, -1, 1));
        // testCases.add(new UnaryOperatorTestCase(Integer.MIN_VALUE, 1, Integer.MIN_VALUE, -1));

        return testCases;
    }

    /**
     * Defines test cases as described in {@link #addFractionTestCases()} and
     * collects them into a {@code List}.
     * @return a list of test cases as described above
     */
    private static List<BinaryOperatorTestCase> collectAddFractionTestCases() {
        final List<BinaryOperatorTestCase> testCases = new ArrayList<>();

        testCases.add(new BinaryOperatorTestCase(1, 2, 1, 2, 1, 1));
        testCases.add(new BinaryOperatorTestCase(1, 2, 2, 3, 7, 6));
        testCases.add(new BinaryOperatorTestCase(2, 3, 1, 2, 7, 6));
        testCases.add(new BinaryOperatorTestCase(2, 3, 2, 3, 4, 3));
        testCases.add(new BinaryOperatorTestCase(2, 3, 0, 5, 2, 3));
        testCases.add(new BinaryOperatorTestCase(2, 3, 0, -5, 2, 3));
        testCases.add(new BinaryOperatorTestCase(0, 7, 2, 3, 2, 3));
        testCases.add(new BinaryOperatorTestCase(0, -7, 2, 3, 2, 3));
        testCases.add(new BinaryOperatorTestCase(2, 3, -2, 3, 0, 1));

        testCases.add(new BinaryOperatorTestCase(
                -1, 13 * 13 * 2 * 2,
                -2, 13 * 17 * 2,
                -17 - 2 * 13 * 2,
                13 * 13 * 17 * 2 * 2));

        // if this fraction is added naively, it will overflow the int range.
        // check that it doesn't.
        testCases.add(new BinaryOperatorTestCase(
                1, 32768 * 3,
                1, 59049,
                52451, 1934917632));

        testCases.add(new BinaryOperatorTestCase(
                Integer.MIN_VALUE, 3,
                1, 3,
                Integer.MIN_VALUE + 1, 3));

        testCases.add(new BinaryOperatorTestCase(
                Integer.MAX_VALUE - 1, 1,
                1, 1,
                Integer.MAX_VALUE, 1));

        //NUMBERS-129
        testCases.add(new BinaryOperatorTestCase(
                362564597, 10,
                274164323, 6,
                1229257703, 15));

        return testCases;
    }

    /**
     * Defines test cases as described in {@link #addIntTestCases()} and
     * collects them into a {@code List}.
     * @return a list of test cases as described above
     */
    private static List<BinaryIntOperatorTestCase> collectAddIntTestCases() {
        final List<BinaryIntOperatorTestCase> testCases = new ArrayList<>();

        testCases.add(new BinaryIntOperatorTestCase(1, 3, 0, 1, 3));
        testCases.add(new BinaryIntOperatorTestCase(-1, 3, 0, -1, 3));
        testCases.add(new BinaryIntOperatorTestCase(1, 3, 1, 4, 3));
        testCases.add(new BinaryIntOperatorTestCase(1, 3, -1, -2, 3));
        testCases.add(new BinaryIntOperatorTestCase(2, -1, 2, 0, 1));
        testCases.add(new BinaryIntOperatorTestCase(Integer.MAX_VALUE - 1, 1, 1, Integer.MAX_VALUE, 1));

        return testCases;
    }

    /**
     * Defines test cases as described in
     * {@link #divideByFractionTestCases()} and collects them into a {@code List}.
     * @return a list of test cases as described above
     */
    private static List<BinaryOperatorTestCase> collectDivideByFractionTestCases() {
        final List<BinaryOperatorTestCase> testCases = new ArrayList<>();

        testCases.add(new BinaryOperatorTestCase(1, 2, 1, 2, 1, 1));
        testCases.add(new BinaryOperatorTestCase(1, 2, 2, 3, 3, 4));
        testCases.add(new BinaryOperatorTestCase(2, 3, 1, 2, 4, 3));
        testCases.add(new BinaryOperatorTestCase(2, 3, 2, 3, 1, 1));
        testCases.add(new BinaryOperatorTestCase(0, 3, 2, 3, 0, 1));
        // Return the original zero representation
        testCases.add(new BinaryOperatorTestCase(0, -3, 2, 3, 0, 1));

        testCases.add(new BinaryOperatorTestCase(
                2, 7,
                1, 1,
                2, 7));
        testCases.add(new BinaryOperatorTestCase(
                1, Integer.MAX_VALUE,
                1, Integer.MAX_VALUE,
                1, 1));
        testCases.add(new BinaryOperatorTestCase(
                Integer.MIN_VALUE, Integer.MAX_VALUE,
                1, Integer.MAX_VALUE,
                Integer.MIN_VALUE, 1));

        return testCases;
    }

    /**
     * Defines test cases as described in
     * {@link #divideByIntTestCases()} and collects them into a {@code List}.
     * @return a list of test cases as described above
     */
    private static List<BinaryIntOperatorTestCase> collectDivideByIntTestCases() {
        final List<BinaryIntOperatorTestCase> testCases = new ArrayList<>();

        testCases.add(new BinaryIntOperatorTestCase(1, 3, 1, 1, 3));
        testCases.add(new BinaryIntOperatorTestCase(0, 5, 11, 0, 1));
        testCases.add(new BinaryIntOperatorTestCase(6, 35, 15, 2, 175));

        // This captures an implementation detail
        testCases.add(new BinaryIntOperatorTestCase(1, 3, -1, 1, -3));

        return testCases;
    }

    /**
     * Defines test cases as described in
     * {@link #multiplyByFractionTestCases()} and collects them into a {@code List}.
     * @return a list of test cases as described above
     */
    private static List<BinaryOperatorTestCase> collectMultiplyByFractionTestCases() {
        final List<BinaryOperatorTestCase> testCases = new ArrayList<>();

        testCases.add(new BinaryOperatorTestCase(1, 2, 1, 2, 1, 4));
        testCases.add(new BinaryOperatorTestCase(1, 2, 2, 3, 1, 3));
        testCases.add(new BinaryOperatorTestCase(2, 3, 1, 2, 1, 3));
        testCases.add(new BinaryOperatorTestCase(2, 3, 2, 3, 4, 9));
        testCases.add(new BinaryOperatorTestCase(0, 3, 2, 3, 0, 1));
        testCases.add(new BinaryOperatorTestCase(0, -3, 2, 3, 0, 1));
        testCases.add(new BinaryOperatorTestCase(2, 3, 0, 3, 0, 1));
        testCases.add(new BinaryOperatorTestCase(2, 3, 0, -3, 0, 1));

        testCases.add(new BinaryOperatorTestCase(
                Integer.MAX_VALUE, 1,
                Integer.MIN_VALUE, Integer.MAX_VALUE,
                Integer.MIN_VALUE, 1));

        return testCases;
    }

    /**
     * Defines test cases as described in
     * {@link #multiplyByIntTestCases()} and collects them into a {@code List}.
     * @return a list of test cases as described above
     */
    private static List<BinaryIntOperatorTestCase> collectMultiplyByIntTestCases() {
        final List<BinaryIntOperatorTestCase> testCases = new ArrayList<>();

        testCases.add(new BinaryIntOperatorTestCase(1, 3, 1, 1, 3));
        testCases.add(new BinaryIntOperatorTestCase(6, 35, 15, 18, 7));
        testCases.add(new BinaryIntOperatorTestCase(Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, 1));
        // Test zero with multiply by integer
        testCases.add(new BinaryIntOperatorTestCase(0, 1, 42, 0, 1));
        testCases.add(new BinaryIntOperatorTestCase(1, 1, 0, 0, 1));

        // This captures an implementation detail
        testCases.add(new BinaryIntOperatorTestCase(1, 3, -1, -1, 3));

        return testCases;
    }

    /**
     * Defines test cases as described in
     * {@link #subtractFractionTestCases()} and collects them into a {@code List}.
     * @return a list of test cases as described above
     */
    private static List<BinaryOperatorTestCase> collectSubtractFractionTestCases() {
        final List<BinaryOperatorTestCase> testCases = new ArrayList<>();

        testCases.add(new BinaryOperatorTestCase(1, 2, 1, 2, 0, 1));
        testCases.add(new BinaryOperatorTestCase(1, 2, 2, 3, -1, 6));
        testCases.add(new BinaryOperatorTestCase(2, 3, 1, 2, 1, 6));
        testCases.add(new BinaryOperatorTestCase(2, 3, 2, 3, 0, 1));
        testCases.add(new BinaryOperatorTestCase(0, 3, 1, 5, -1, 5));
        testCases.add(new BinaryOperatorTestCase(0, -3, 1, 5, -1, 5));
        testCases.add(new BinaryOperatorTestCase(2, 3, 0, 5, 2, 3));
        testCases.add(new BinaryOperatorTestCase(2, 3, 0, -5, 2, 3));

        // if this fraction is subtracted naively, it will overflow the int range.
        // check that it doesn't.
        testCases.add(new BinaryOperatorTestCase(
                1, 32768 * 3,
                1, 59049,
                -13085, 1934917632));

        testCases.add(new BinaryOperatorTestCase(
                Integer.MIN_VALUE, 3,
                -1, 3,
                Integer.MIN_VALUE + 1, 3
        ));

        testCases.add(new BinaryOperatorTestCase(
                Integer.MAX_VALUE, 1,
                1, 1,
                Integer.MAX_VALUE - 1, 1
        ));

        return testCases;
    }

    /**
     * Defines test cases as described in
     * {@link #subtractIntTestCases()} and collects them into a {@code List}.
     * @return a list of test cases as described above
     */
    private static List<BinaryIntOperatorTestCase> collectSubtractIntTestCases() {
        final List<BinaryIntOperatorTestCase> testCases = new ArrayList<>();

        testCases.add(new BinaryIntOperatorTestCase(1, 3, 1, -2, 3));
        testCases.add(new BinaryIntOperatorTestCase(0, 1, 3, -3, 1));
        testCases.add(new BinaryIntOperatorTestCase(2, 3, 3, -7, 3));
        testCases.add(new BinaryIntOperatorTestCase(2, 3, 0, 2, 3));
        testCases.add(new BinaryIntOperatorTestCase(2, -1, -2, 0, 1));

        return testCases;
    }

    /**
     * Defines test cases as described in
     * {@link #powTestCases()} and collects them into a {@code List}.
     * @return a list of test cases as described above
     */
    private static List<BinaryIntOperatorTestCase> collectPowTestCases() {
        final List<BinaryIntOperatorTestCase> testCases = new ArrayList<>();

        testCases.add(new BinaryIntOperatorTestCase(3, 7, 0, 1, 1));
        testCases.add(new BinaryIntOperatorTestCase(3, 7, 1, 3, 7));
        testCases.add(new BinaryIntOperatorTestCase(3, 7, -1, 7, 3));
        testCases.add(new BinaryIntOperatorTestCase(3, 7, 2, 9, 49));
        testCases.add(new BinaryIntOperatorTestCase(3, 7, -2, 49, 9));

        testCases.add(new BinaryIntOperatorTestCase(3, -7, 0, 1, 1));
        testCases.add(new BinaryIntOperatorTestCase(3, -7, 1, 3, -7));
        testCases.add(new BinaryIntOperatorTestCase(3, -7, -1, -7, 3));
        testCases.add(new BinaryIntOperatorTestCase(3, -7, 2, 9, 49));
        testCases.add(new BinaryIntOperatorTestCase(3, -7, -2, 49, 9));

        testCases.add(new BinaryIntOperatorTestCase(2, 3, 13, 8192, 1594323));
        testCases.add(new BinaryIntOperatorTestCase(2, 3, -13, 1594323, 8192));

        testCases.add(new BinaryIntOperatorTestCase(0, 1, Integer.MAX_VALUE, 0, 1));
        testCases.add(new BinaryIntOperatorTestCase(0, -1, Integer.MAX_VALUE, 0, 1));

        testCases.add(new BinaryIntOperatorTestCase(1, 1, Integer.MIN_VALUE, 1, 1));
        testCases.add(new BinaryIntOperatorTestCase(1, -1, Integer.MIN_VALUE, 1, 1));
        testCases.add(new BinaryIntOperatorTestCase(-1, 1, Integer.MIN_VALUE, 1, 1));
        testCases.add(new BinaryIntOperatorTestCase(-1, -1, Integer.MIN_VALUE, 1, 1));
        return testCases;
    }

    /**
     * Provides a list of test cases where a fraction should be created from
     * a specified numerator and denominator, both in the {@code int} range,
     * and the expected numerator and denominator of the created fraction are
     * also in the {@code int} range.
     *
     * @return a list of test cases as described above
     */
    static List<UnaryOperatorTestCase> numDenConstructorTestCases() {
        return Collections.unmodifiableList(numDenConstructorTestCasesList);
    }

    /**
     * Provides a list of test cases where a {@code double} value should be
     * converted to a fraction with a certain amount of absolute error
     * allowed, and the expected numerator and denominator of the resulting
     * fraction are in the {@code int} range.
     *
     * <p>The maximum denominator in the test cases will be zero and should be ignored.
     *
     * @return a list of test cases as described above
     */
    static List<DoubleToFractionTestCase> doubleConstructorTestCases() {
        return Collections.unmodifiableList(doubleConstructorTestCasesList);
    }

    /**
     * Provides a list of test cases where a {@code double} value should be
     * converted to a fraction with a specified maximum denominator, and the
     * expected numerator and denominator of the resulting fraction are in the
     * {@code int} range.
     *
     * @return a list of test cases as described above
     */
    static List<DoubleToFractionTestCase> doubleMaxDenomConstructorTestCases() {
        return Collections.unmodifiableList(doubleMaxDenomConstructorTestCasesList);
    }

    /**
     * Provides a list of test cases where the absolute value of a fraction
     * created from a specified numerator and denominator, both in the {@code
     * int} range, should be calculated, and the expected numerator and
     * denominator of the resulting fraction are also in the {@code int} range.
     * @return a list of test cases as described above
     */
    static List<UnaryOperatorTestCase> absTestCases() {
        return Collections.unmodifiableList(absTestCasesList);
    }

    /**
     * Provides a list of test cases where the multiplicative inverse of a
     * fraction created from a specified numerator and denominator, both in
     * the {@code int} range, should be calculated, and the expected
     * numerator and denominator of the resulting fraction are also in the
     * {@code int} range.
     *
     * @return a list of test cases as described above
     */
    static List<UnaryOperatorTestCase> reciprocalTestCases() {
        return Collections.unmodifiableList(reciprocalTestCasesList);
    }

    /**
     * Provides a list of test cases where the additive inverse of a fraction
     * created from a specified numerator and denominator, both in the {@code
     * int} range, should be calculated, and the expected numerator and
     * denominator of the resulting fraction are also in the {@code int} range.
     *
     * @return a list of test cases as described above
     */
    static List<UnaryOperatorTestCase> negateTestCases() {
        return Collections.unmodifiableList(negateTestCasesList);
    }

    /**
     * Provides a list of test cases where two fractions, each created from a
     * specified numerator and denominator in the {@code int} range, should
     * be added, and the expected numerator and denominator of the resulting
     * fraction are also in the {@code int} range.
     *
     * @return a list of test cases as described above
     */
    static List<BinaryOperatorTestCase> addFractionTestCases() {
        return Collections.unmodifiableList(addFractionTestCasesList);
    }

    /**
     * Provides a list of test cases where a fraction, created from a
     * specified numerator and denominator in the {@code int} range, should
     * be added to a specified value in the {@code int} range,
     * and the expected numerator and denominator of the resulting fraction
     * are in the {@code int} range as well.
     *
     * @return a list of test cases as described above
     */
    static List<BinaryIntOperatorTestCase> addIntTestCases() {
        return Collections.unmodifiableList(addIntTestCasesList);
    }

    /**
     * Provides a list of test cases where a fraction, created from a
     * specified numerator and denominator in the {@code int} range, should
     * be divided by another fraction, also created from a specified
     * numerator and denominator in the {@code int} range, and the expected
     * numerator and denominator of the resulting fraction are in the {@code
     * int} range as well.
     *
     * <p>The first operand in each test case is the dividend and the second
     * operand is the divisor.</p>
     *
     * @return a list of test cases as described above
     */
    static List<BinaryOperatorTestCase> divideByFractionTestCases() {
        return Collections.unmodifiableList(divideByFractionTestCasesList);
    }

    /**
     * Provides a list of test cases where a fraction, created from a
     * specified numerator and denominator in the {@code int} range, should
     * be divided by a specified value in the {@code int} range,
     * and the expected numerator and denominator of the resulting fraction
     * are in the {@code int} range as well.
     *
     * <p>The first operand in each test case is the dividend and the second
     * operand is the divisor.</p>
     *
     * @return a list of test cases as described above
     */
    static List<BinaryIntOperatorTestCase> divideByIntTestCases() {
        return Collections.unmodifiableList(divideByIntTestCasesList);
    }

    /**
     * Provides a list of test cases where a fraction, created from a
     * specified numerator and denominator in the {@code int} range, should
     * be multiplied by another fraction, also created from a specified
     * numerator and denominator in the {@code int} range, and the expected
     * numerator and denominator of the resulting fraction are in the {@code
     * int} range as well.
     *
     * @return a list of test cases as described above
     */
    static List<BinaryOperatorTestCase> multiplyByFractionTestCases() {
        return Collections.unmodifiableList(multiplyByFractionTestCasesList);
    }

    /**
     * Provides a list of test cases where a fraction, created from a
     * specified numerator and denominator in the {@code int} range, should
     * be multiplied by a specified value in the {@code int} range,
     * and the expected numerator and denominator of the resulting fraction
     * are in the {@code int} range as well.
     *
     * @return a list of test cases as described above
     */
    static List<BinaryIntOperatorTestCase> multiplyByIntTestCases() {
        return Collections.unmodifiableList(multiplyByIntTestCasesList);
    }

    /**
     * Provides a list of test cases where a fraction, created from a
     * specified numerator and denominator in the {@code int} range, should
     * be subtracted from another fraction, also created from a specified
     * numerator and denominator in the {@code int} range, and the expected
     * numerator and denominator of the resulting fraction are in the {@code
     * int} range as well.
     *
     * <p>The first operand in each test case is the minuend and the second
     * operand is the subtrahend.</p>
     *
     * @return a list of test cases as described above
     */
    static List<BinaryOperatorTestCase> subtractFractionTestCases() {
        return Collections.unmodifiableList(subtractFractionTestCasesList);
    }

    /**
     * Provides a list of test cases where a fraction, created from a
     * specified numerator and denominator in the {@code int} range, should
     * have a specified value in the {@code int} range subtracted,
     * and the expected numerator and denominator of the resulting fraction
     * are in the {@code int} range as well.
     *
     * <p>The first operand in each test case is the minuend and the second
     * operand is the subtrahend.</p>
     *
     * @return a list of test cases as described above
     */
    static List<BinaryIntOperatorTestCase> subtractIntTestCases() {
        return Collections.unmodifiableList(subtractIntTestCasesList);
    }

    /**
     * Provides a list of test cases where a fraction, created from a
     * specified numerator and denominator in the {@code int} range, should
     * be raised to a specified power specified in the {@code int} range,
     * and the expected numerator and denominator of the resulting fraction
     * are in the {@code int} range as well.
     *
     * @return a list of test cases as described above
     */
    static List<BinaryIntOperatorTestCase> powTestCases() {
        return Collections.unmodifiableList(powTestCasesList);
    }

    // CHECKSTYLE: stop VisibilityModifier

    /**
     * Represents a test case where a unary operation should be performed on
     * a specified combination of numerator and denominator, both in the
     * {@code int} range, and the numerator and denominator of the expected
     * result are also in the {@code int} range.
     */
    static class UnaryOperatorTestCase {
        final int operandNumerator;
        final int operandDenominator;
        final int expectedNumerator;
        final int expectedDenominator;

        UnaryOperatorTestCase(
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
     * Represents a test case where a binary operation should be performed on
     * two specified combinations of numerator and denominator, with the
     * numerator and denominator of each combination in the {@code int}
     * range, and the numerator and denominator of the expected result are
     * also in the {@code int} range.
     */
    static class BinaryOperatorTestCase {
        final int firstOperandNumerator;
        final int firstOperandDenominator;
        final int secondOperandNumerator;
        final int secondOperandDenominator;
        final int expectedNumerator;
        final int expectedDenominator;

        BinaryOperatorTestCase(
                int firstOperandNumerator,
                int firstOperandDenominator,
                int secondOperandNumerator,
                int secondOperandDenominator,
                int expectedNumerator,
                int expectedDenominator) {
            this.firstOperandNumerator = firstOperandNumerator;
            this.firstOperandDenominator = firstOperandDenominator;
            this.secondOperandNumerator = secondOperandNumerator;
            this.secondOperandDenominator = secondOperandDenominator;
            this.expectedNumerator = expectedNumerator;
            this.expectedDenominator = expectedDenominator;
        }
    }

    /**
     * Represents a test case where a binary operation should be performed on
     * a specified combination of numerator and denominator and a integer argument,
     * with the numerator and denominator in the {@code int}
     * range, and the numerator and denominator of the expected result are
     * also in the {@code int} range.
     */
    static class BinaryIntOperatorTestCase {
        final int firstOperandNumerator;
        final int firstOperandDenominator;
        final int secondOperand;
        final int expectedNumerator;
        final int expectedDenominator;

        BinaryIntOperatorTestCase(
                int firstOperandNumerator,
                int firstOperandDenominator,
                int secondOperand,
                int expectedNumerator,
                int expectedDenominator) {
            this.firstOperandNumerator = firstOperandNumerator;
            this.firstOperandDenominator = firstOperandDenominator;
            this.secondOperand = secondOperand;
            this.expectedNumerator = expectedNumerator;
            this.expectedDenominator = expectedDenominator;
        }
    }

    /**
     * Represents a test case where an operation that yields a fraction
     * should be performed on a {@code double} value and the numerator and
     * denominator of the expected result
     * are in the {@code int} range.
     *
     * <p>Optionally captures a maximum denominator. This will be zero if
     * not required in the test case.
     */
    static class DoubleToFractionTestCase {
        final double operand;
        final int maxDenominator;
        final int expectedNumerator;
        final int expectedDenominator;

        DoubleToFractionTestCase(
                double operand,
                int maxDenominator,
                int expectedNumerator,
                int expectedDenominator) {
            this.operand = operand;
            this.maxDenominator = maxDenominator;
            this.expectedNumerator = expectedNumerator;
            this.expectedDenominator = expectedDenominator;
        }

        DoubleToFractionTestCase(
                double operand,
                int expectedNumerator,
                int expectedDenominator) {
            this(operand, 0, expectedNumerator, expectedDenominator);
        }
    }
}
