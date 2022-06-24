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

import java.util.Random;


class ComplexComposeTest {


    private static final DComplexUnaryOperator neg = ComplexFunctions::negate;
    private static final DComplexUnaryOperator multiplyImag = ComplexFunctions::multiplyImaginary;
    private static final DComplexUnaryOperator conj = ComplexFunctions::conjComplex;
    private static final DComplexUnaryOperator multiplyImagConj = multiplyImag.thenApply(conj);
    private static final DComplexUnaryOperator conjMultiplyImag = conj.thenApply(multiplyImag);
    private static final DComplexUnaryOperator identity1 = multiplyImagConj.thenApply(multiplyImagConj);
    private static final DComplexUnaryOperator identity2 = conjMultiplyImag.thenApply(conjMultiplyImag);



    @Test
    void testComposing() {
        Random random = new Random();
        double real = random.nextInt();
        double imag = random.nextInt();
        Complex c = Complex.ofCartesian(real, imag);


        Complex c1 = (Complex) multiplyImagConj.thenApply(neg).apply(c, Complex::ofCartesian);
        Complex c2 = (Complex) conjMultiplyImag.apply(c, Complex::ofCartesian);

        Assertions.assertEquals(c1, c2);
        c1 = (Complex) identity1.apply(c, Complex::ofCartesian);
        c2 = (Complex) identity2.apply(c, Complex::ofCartesian);

        Assertions.assertEquals(c, c1);
        Assertions.assertEquals(c, c2);


    }
}
