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
import java.util.function.UnaryOperator;

/**
 * Represents a complex operation that accepts a complex number of type DComplex and produces a DComplex result.
 */
@FunctionalInterface
public interface ComplexUnaryOperator extends UnaryOperator<ComplexDouble> {


    /**
     * Represents an operator that accepts a complex number and a complex constructor to produce and return the result.
     * @param in Complex number
     * @param out Constructor
     * @return DComplex
     */
    ComplexDouble apply(ComplexDouble in, ComplexConstructor<ComplexDouble> out);

    /**
     * Represents an operator that accepts real and imaginary parts of a complex number and a complex constructor to produce and return the result.
     * @param r real
     * @param i imaginary
     * @param out Constructor
     * @return DComplex
     */
    default ComplexDouble apply(double r, double i, ComplexConstructor<ComplexDouble> out) {
        return apply(Complex.ofCartesian(r, i), out);
    }

    /**
     * Represents an operator that accepts a complex number and produces a result.
     * @param c Complex number
     * @return DComplex
     */
    @Override
    default ComplexDouble apply(ComplexDouble c) {
        return apply(c, ComplexConstructor.D_COMPLEX_RESULT);
    }

    /**
     * Returns a composed unary operator that first applies this function to its input, and then applies the after UnaryOperator function to the result.
     * If evaluation of either function throws an exception, it is relayed to the caller of the composed function.
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then applies the after function
     */
    default ComplexUnaryOperator thenApply(ComplexUnaryOperator after) {
        Objects.requireNonNull(after);
        return (ComplexDouble c, ComplexConstructor<ComplexDouble> out) -> after.apply(apply(c, out), out);

    }

    /**
     * Returns a composed BinaryOperator that first applies this function to its first binary input,
     * and then applies the result and the second binary input to the after BinaryOperator to produce the result.
     * If evaluation of either function throws an exception, it is relayed to the caller of the composed function.
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then applies the after function
     */
    default ComplexBinaryOperator thenApplyBinaryOperator(ComplexBinaryOperator after) {
        Objects.requireNonNull(after);
        return (ComplexDouble c1, ComplexDouble c2, ComplexConstructor<ComplexDouble> out) ->
            after.apply(apply(c1, out), c2, out);
    }

    /**
     * Returns a composed scalar function that first applies this function to its input, and then applies the after ScalarFunctiom function to the result.
     * If evaluation of either function throws an exception, it is relayed to the caller of the composed function.
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then applies the after function
     */
    default ComplexScalarFunction thenApplyScalarFunction(ComplexScalarFunction after) {
        Objects.requireNonNull(after);
        return (ComplexDouble c1, double d, ComplexConstructor<ComplexDouble> out) ->
            after.apply(apply(c1, out), d, out);
    }


}
