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
 * Represents a data sink for a complex number \( (a + i b) \).
 * Operations return a result of type {@code R}.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #apply(double, double)}.
 *
 * @param <R> The type of the result
 * @since 1.1
 */
@FunctionalInterface
public interface ComplexSink<R> {

    /**
     * Represents a function that accepts real and imaginary part of complex number and returns an object.
     * @param real Real part \( a \) of the complex number \( (a +ib) \).
     * @param imaginary Imaginary part \( b \) of the complex number \( (a +ib) \).
     * @return R the object encapsulating the complex result
     */
    R apply(double real, double imaginary);
}
