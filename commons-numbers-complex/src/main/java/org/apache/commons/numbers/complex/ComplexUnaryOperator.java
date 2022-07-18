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
 * Represents a unary operation on a Cartesian form of a complex number \( a + ib \)
 * where \( a \) and \( b \) are real numbers represented as two {@code double}
 * parts. The operation creates a complex number result; the result is supplied
 * to a terminating consumer function which may return an object representation
 * of the complex result.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #apply(double, double, ComplexResult)}.
 *
 * @param <R> The type of the complex result
 * @since 1.1
 */
@FunctionalInterface
public interface ComplexUnaryOperator<R> {

    /**
     * Represents an operator that accepts real and imaginary parts of a complex number and a complex constructor to produce and return the result.
     * @param r Real part \( a \) of the complex number \(a +ib \).
     * @param i Imaginary part \( b \) of the complex number \(a +ib \).
     * @param out Terminating consumer for the complex result, used to construct a result of type {@code R}.
     * @return the object created by the supplied constructor.
     */
    R apply(double r, double i, ComplexResult<R> out);
}
