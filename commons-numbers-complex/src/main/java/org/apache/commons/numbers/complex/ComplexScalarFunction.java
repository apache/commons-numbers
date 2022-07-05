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

/**
 * Represents an operation upon two operands of the different type, producing a result.
 * This is a functional interface whose functional method is {@link #apply(double, double, double, ComplexConstructor)}.
 * @param <R> Generic
 */
@FunctionalInterface
public interface ComplexScalarFunction<R> {

    /**
     * Represents a binary function that accepts a Complex and a double operand to produce a Complex result.
     * The result is accepted by the ComplexConstructor.
     * @param real part the first complex argument
     * @param imaginary part of the first function argument
     * @param operand the second function argument
     * @param result Constructor
     * @return ComplexDouble
     */
    R apply(double real, double imaginary, double operand, ComplexConstructor<R> result);

    /**
     * Returns a composed scalar function that first applies this function to its input, and then applies the after function to the result.
     * If evaluation of either function throws an exception, it is relayed to the caller of the composed function.
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then applies the after function
     */
    default ComplexScalarFunction<R> andThen(ComplexUnaryOperator<R> after) {
        Objects.requireNonNull(after);
        return (real, imaginary, operand, out) ->
             apply(real, imaginary, operand, out.compose(after));
    }
}
