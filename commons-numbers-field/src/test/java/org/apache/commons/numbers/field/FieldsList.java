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
package org.apache.commons.numbers.field;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import java.util.ArrayList;

import org.apache.commons.numbers.fraction.Fraction;
import org.apache.commons.numbers.core.DD;
import org.apache.commons.numbers.core.Precision;
import org.apache.commons.numbers.fraction.BigFraction;

import org.junit.jupiter.api.Assertions;

/**
 * List of fields.
 */
final class FieldsList {
    /** List of all fields implemented in the library. */
    private static final List<FieldTestData<?>> LIST =
            new ArrayList<>();

    static {
        try {
            // List of fields to test.
            add(FractionField.get(),
                Fraction.of(13, 4),
                Fraction.of(5, 29),
                Fraction.of(-279, 11));
            add(BigFractionField.get(),
                BigFraction.of(13256093L, 43951044L),
                BigFraction.of(543016315L, 29L),
                BigFraction.of(-27930919051L, 11L));
            add(FP64Field.get(),
                FP64.of(23.45678901),
                FP64.of(-543.2109876),
                FP64.of(-234.5678901),
                // double operations are subject to rounding so allow a tolerance
                (x, y) -> Precision.equals(x.doubleValue(), y.doubleValue(), 1));
            add(DDField.get(),
                createDD(23.45678901, 4.5671892973),
                createDD(-543.2109876, 5.237848286),
                createDD(-234.5678901, -4.561268179),
                // double-double operations are subject to rounding so allow a tolerance.
                FieldsList::areEqual);

        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new RuntimeException(e);
        }
    }

    private FieldsList() {}

    /**
     * @param field Field.
     * @param a Field element.
     * @param b Field element.
     * @param c Field element.
     */
    private static <T> void add(Field<T> field,
                                T a,
                                T b,
                                T c) {
        LIST.add(new FieldTestData<>(field, a, b, c));
    }

    /**
     * @param field Field.
     * @param a Field element.
     * @param b Field element.
     * @param c Field element.
     * @param equals Field equality predicate.
     */
    private static <T> void add(Field<T> field,
                                T a,
                                T b,
                                T c,
                                BiPredicate<T, T> equals) {
        LIST.add(new FieldTestData<>(field, a, b, c, equals));
    }

    /**
     * Creates the double-double number from two random values.
     * The second value is scaled so that it does not overlap the first.
     *
     * @param a Value.
     * @param b Value.
     * @return the number
     */
    private static DD createDD(double a, double b) {
        final int ea = Math.getExponent(a);
        final int eb = Math.getExponent(b);
        // Scale to have a non-overlapping 53-bit mantissa
        double bb = Math.scalb(b, ea - eb - 53);
        // If b has a larger mantissa than a (ignoring the exponent) then an overlap may occur
        if (a != a + bb) {
            bb *= 0.5;
        }
        Assertions.assertEquals(a, a + bb);
        return DD.ofSum(a, bb);
    }

    /**
     * Test if the two numbers are equal.
     *
     * @param x Value.
     * @param y Value.
     * @return true if equal
     */
    private static boolean areEqual(DD x, DD y) {
        // A simple binary equality is fine for most cases.
        if (x.equals(y)) {
            return true;
        }
        // If the high part is the same we can test a ULP tolerance on the low part.
        if (x.hi() == y.hi()) {
            return Precision.equals(x.lo(), y.lo(), 1);
        }
        // Note that the high part could be different by 1 ulp and then the low part
        // can be significantly different (opposite signed values) to create numbers that
        // are almost the same: x+xx ~ y-yy.
        // Here we obtain the difference and use a relative error of 2^-105:
        //  | x - y |
        // ------------   <  relative error
        // max(|x|, |y|)
        return Math.abs(x.subtract(y).doubleValue()) /
            Math.max(Math.abs(x.doubleValue()), Math.abs(y.doubleValue())) < Math.pow(2, -105);
    }

    /**
     * Subclasses that are "parametric" tests can forward the call to
     * the "@Parameters"-annotated method to this method.
     *
     * @return the stream of all fields.
     */
    static Stream<FieldTestData<?>> stream() {
        return LIST.stream();
    }
}
