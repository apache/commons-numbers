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

/**
 * Representation of complex number. Contains real and imaginary part and creates Complex
 * number using real and imaginary part.
 */
public interface ComplexDouble {

    /**
     * Gets the real part \( a \) of complex number \( (a + i b) \).
     *
     * @return real part
     */
    double real();

    /**
     * Gets the imaginary part \( b \) of complex number \( (a + i b) \).
     *
     * @return imaginary part
     */
    double imag();

    /**
     * Create a complex number given the real and imaginary parts.
     *
     * @param r Real part.
     * @param i Imaginary part.
     * @return {@code ComplexDouble} number.
     */
    static ComplexDouble of(double r, double i) {
        return Complex.ofCartesian(r, i);
    }

    /**
     * This operator is used for all Complex operations that only deal with one Complex number.
     * @param operator ComplexUnaryOperator
     * @return ComplexDouble
     */
    default ComplexDouble applyUnaryOperator(ComplexUnaryOperator operator) {
        return operator.apply(this, Complex::ofCartesian);
    }

    /**
     * This operator is used for all Complex operations that deals with two Complex numbers.
     * @param operator ComplexBinaryOperator
     * @param input ComplexDouble
     * @return ComplexDouble
     */
    default ComplexDouble applyBinaryOperator(ComplexDouble input, ComplexBinaryOperator operator) {
        return operator.apply(this, input, Complex::ofCartesian);
    }

    /**
     * This operator is used for all Complex operations that deals with one Complex number
     * and a scalar factor.
     * @param operator ComplexScalarFunction
     * @param factor double
     * @return ComplexDouble
     */
    default ComplexDouble applyScalarFunction(double factor, ComplexScalarFunction operator) {
        return operator.apply(this, factor, Complex::ofCartesian);
    }
}