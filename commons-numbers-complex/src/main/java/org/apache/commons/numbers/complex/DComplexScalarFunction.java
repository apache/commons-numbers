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
 * This is a functional interface whose functional method is apply(DComplex, double).
 */
@FunctionalInterface
public interface DComplexScalarFunction {

    /**
     * Represents a binary function that accepts a Complex and a double operand to produce a Complex result.
     * @param c Complex number
     * @param f factor
     * @param result Constructor
     * @return DComplex
     */
    DComplex apply(DComplex c, double f, DComplexConstructor<DComplex> result);

    /**
     * Returns a composed scalar function that first applies this function to its input, and then applies the after function to the result.
     * If evaluation of either function throws an exception, it is relayed to the caller of the composed function.
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then applies the after function
     */
    default DComplexScalarFunction thenApply(DComplexUnaryOperator after) {
        Objects.requireNonNull(after);
        return (DComplex c, double f, DComplexConstructor<DComplex> out) -> after.apply(apply(c, f, out), out);

    }
}
