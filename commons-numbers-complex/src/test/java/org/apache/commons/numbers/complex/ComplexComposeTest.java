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

package org.apache.commons.numbers.complex;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ComplexComposeTest {

    private static final ComplexUnaryOperator neg = ComplexFunctions::negate;
    private static final ComplexUnaryOperator multiplyImag = ComplexFunctions::multiplyImaginary;
    private static final ComplexUnaryOperator conj = ComplexFunctions::conj;
    private static final ComplexUnaryOperator multiplyImagConj = multiplyImag.andThen(conj);
    private static final ComplexUnaryOperator conjMultiplyImag = conj.andThen(multiplyImag);
    private static final ComplexUnaryOperator identity1 = multiplyImagConj.andThen(multiplyImagConj);
    private static final ComplexUnaryOperator identity2 = conjMultiplyImag.andThen(conjMultiplyImag);
    private static final ComplexBinaryOperator divide = ComplexFunctions::divide;

    @Test
    void testConjugateIdentities() {
        testConjugateIdentity(Complex.ofCartesian(3, 4));
        testConjugateIdentity(Complex.ofCartesian(Double.NaN, Double.NaN));
        testConjugateIdentity(Complex.ofCartesian(Double.NaN, 0));
        testConjugateIdentity(Complex.ofCartesian(0, Double.NaN));
        testConjugateIdentity(Complex.ofCartesian(Double.POSITIVE_INFINITY, 0));
        testConjugateIdentity(Complex.ofCartesian(Double.NEGATIVE_INFINITY, 0));
        testConjugateIdentity(Complex.ofCartesian(0, Double.POSITIVE_INFINITY));
        testConjugateIdentity(Complex.ofCartesian(0, Double.NEGATIVE_INFINITY));
        testConjugateIdentity(Complex.ofCartesian(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        testConjugateIdentity(Complex.ofCartesian(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
    }

    private void testConjugateIdentity(ComplexDouble c) {
        ComplexDouble c1 = multiplyImagConj.andThen(neg).apply(c, Complex::ofCartesian);
        ComplexDouble r1 = multiplyImagConj.andThen(neg).apply(c.getReal(), c.getImaginary(), Complex::ofCartesian);
        ComplexDouble c2 = conjMultiplyImag.apply(c);

        Assertions.assertEquals(r1, c2);
        Assertions.assertEquals(c1, c2);
        c1 = identity1.apply(c, Complex::ofCartesian);
        c2 = identity2.apply(c, Complex::ofCartesian);

        Assertions.assertEquals(c, c1);
        Assertions.assertEquals(c, c2);
    }

    @Test
    void testMultiplyImaginaryAndThenRoot() {
        testUnary(multiplyImag, ComplexFunctions::sqrt, 3, -4);
        testUnary(multiplyImag, ComplexFunctions::sqrt, Double.NaN, Double.NaN);
        testUnary(multiplyImag, ComplexFunctions::sqrt, Double.NaN, 0);
        testUnary(multiplyImag, ComplexFunctions::sqrt, 0, Double.NaN);
        testUnary(multiplyImag, ComplexFunctions::sqrt, Double.POSITIVE_INFINITY, 0);
        testUnary(multiplyImag, ComplexFunctions::sqrt, Double.NEGATIVE_INFINITY, 0);
        testUnary(multiplyImag, ComplexFunctions::sqrt, 0, Double.POSITIVE_INFINITY);
        testUnary(multiplyImag, ComplexFunctions::sqrt, 0, Double.NEGATIVE_INFINITY);
        testUnary(multiplyImag, ComplexFunctions::sqrt, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        testUnary(multiplyImag, ComplexFunctions::sqrt, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
    }

    private static void testUnary(ComplexUnaryOperator before, ComplexUnaryOperator after,
                                   double real, double imaginary) {

        ComplexUnaryOperator composed = before.andThen(after);
        Complex z = Complex.ofCartesian(real, imaginary);
        ComplexDouble expected = after.apply(before.apply(z));

        Assertions.assertEquals(expected, composed.apply(z));
        Assertions.assertEquals(expected, composed.apply(z.getReal(), z.getImaginary(), Complex::ofCartesian));
    }

    @Test
    void testSumAndExp() {
        testBinaryCompose(ComplexFunctions::multiply, ComplexFunctions::exp,
            Complex.ofCartesian(3, -4),  Complex.ofCartesian(3, 4));
        testBinaryCompose(ComplexFunctions::multiply, ComplexFunctions::exp,
            Complex.ofCartesian(0, 0),  Complex.ofCartesian(3, 4));
        testBinaryCompose(ComplexFunctions::multiply, ComplexFunctions::exp,
            Complex.ofCartesian(0, 0),  Complex.ofCartesian(Double.NEGATIVE_INFINITY, 4));
        testBinaryCompose(ComplexFunctions::multiply, ComplexFunctions::exp,
            Complex.ofCartesian(0, 0),  Complex.ofCartesian(Double.NaN, Double.NaN));
        testBinaryCompose(ComplexFunctions::multiply, ComplexFunctions::exp,
            Complex.ofCartesian(Double.NaN, Double.NaN),  Complex.ofCartesian(Double.NaN, Double.NaN));
    }

    private static void testBinaryCompose(ComplexBinaryOperator before, ComplexUnaryOperator after,
                                  ComplexDouble z1, ComplexDouble z2) {

        ComplexBinaryOperator composed = before.andThen(after);
        ComplexDouble expected = after.apply(before.apply(z1, z2));

        Assertions.assertEquals(expected, composed.apply(z1, z2));
        Assertions.assertEquals(expected, composed.apply(z1.getReal(), z1.getImaginary(),
            z2.getReal(), z2.getImaginary(), Complex::ofCartesian));
    }
}
