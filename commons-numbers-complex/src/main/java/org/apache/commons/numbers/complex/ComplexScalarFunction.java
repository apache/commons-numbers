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
 * Represents a binary operation on a Cartesian form of a complex number \( a + ib \)
 * where \( a \) and \( b \) are real numbers represented as two {@code double}
 * parts and on a double scalar operand. The operation creates a complex number result; the result is supplied
 * to a terminating consumer function which may return an object representation of the complex result.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #apply(double, double, double, ComplexSink)}.
 *
 * @param <R> The type of the complex result
 * @since 1.1
 */
@FunctionalInterface
public interface ComplexScalarFunction<R> {

    /**
     * Represents a binary function that accepts a Complex number's real and imaginary parts
     * and a double operand to produce a Complex result.
     * The complex result is supplied to the provided consumer.
     *
     * @param real Real part \( a \) of the complex number \( (a +ib) \).
     * @param imaginary Imaginary part \( b \) of the complex number \( (a +ib) \).
     * @param operand Scalar operand.
     * @param action Consumer for the complex result.
     * @return the object returned by the provided consumer.
     */
    R apply(double real, double imaginary, double operand, ComplexSink<R> action);
}
