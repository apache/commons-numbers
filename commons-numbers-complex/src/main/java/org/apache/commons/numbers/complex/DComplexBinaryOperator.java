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

import java.util.Objects;
import java.util.function.BinaryOperator;


/**
 * Represents an operation upon two operands of the same type, producing a result of the same type as the operands.
 * This is a specialization of BinaryOperator for the case where the operands and the result are all of the same type.
 * This is a functional interface whose functional method is apply(DComplex, DComplex).
*/
@FunctionalInterface
public interface DComplexBinaryOperator extends BinaryOperator<DComplex> {

    /**
     * Represents an operator that accepts two complex operands and a complex constructor to produce and return the result.
     * @param c1 Complex number 1
     * @param c2 Complex number 2
     * @param result Constructor
     * @return DComplex
     */
    DComplex apply(DComplex c1, DComplex c2, DComplexConstructor<DComplex> result);

    /**
     * Represents an operator that accepts real and imaginary parts of two complex operands and a complex constructor to produce and return the result.
     * @param r1 real 1
     * @param i1 imaginary 1
     * @param r2 real 2
     * @param i2 imaginary 2
     * @param out constructor
     * @return DComplex
     */
    default DComplex apply(double r1, double i1, double r2, double i2, DComplexConstructor<DComplex> out) {
        return apply(Complex.ofCartesian(r1, i1), Complex.ofCartesian(r2, i2), out);
    }

    /**
     * Represents an operator that accepts 2 complex operands and produces a result.
     * @param c1 Complex number 1
     * @param c2 Complex number 2
     * @return DComplex
     */
    @Override
    default DComplex apply(DComplex c1, DComplex c2) {
        return apply(c1, c2, DComplexConstructor.D_COMPLEX_RESULT);
    }

    /**
     * Returns a composed function that first applies this function to its input, and then applies the after function to the result.
     * If evaluation of either function throws an exception, it is relayed to the caller of the composed function.
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then applies the after function
     */
    default DComplexBinaryOperator thenApply(DComplexUnaryOperator after) {
        Objects.requireNonNull(after);
        return (DComplex c1, DComplex c2, DComplexConstructor<DComplex> out) -> after.apply(apply(c1, c2, out), out);

    }
}
