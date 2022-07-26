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
 * parts. The operation creates a complex number result; the result is supplied
 * to a terminating consumer function which may return an object representation
 * of the complex result.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #apply(double, double, double, double, ComplexSink)}.
 *
 * @param <R> The type of the complex result
 * @since 1.1
 */
@FunctionalInterface
public interface ComplexBinaryOperator<R> {

    /**
     * Represents an operator that accepts real and imaginary parts of two complex numbers and supplies the complex result to the provided consumer.
     *
     * @param real1 Real part \( a \) of the first complex number \( (a +ib) \).
     * @param imaginary1 Imaginary part \( b \) of the first complex number \( (a +ib) \).
     * @param real2 Real part \( a \) of the second complex number \( (a +ib) \).
     * @param imaginary2 Imaginary part \( b \) of the second complex number \( (a +ib) \).
     * @param out Consumer for the complex result.
     * @return the object returned by the provided consumer.
     */
    R apply(double real1, double imaginary1, double real2, double imaginary2, ComplexSink<R> out);
}
