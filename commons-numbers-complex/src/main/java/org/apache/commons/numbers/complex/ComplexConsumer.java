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
 * Represents an operation that accepts two double input arguments and returns no result.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #apply(double, double)}.
 *
 * @since 1.1
 */
@FunctionalInterface
public interface ComplexConsumer {

    /**
     * Represents a function that accepts real and imaginary part of complex number and returns no result.
     * @param real Real part \( a \) of the complex number \( (a + ib) \).
     * @param imaginary Imaginary part \( b \) of the complex number \( (a + ib) \).
     */
    void apply(double real, double imaginary);
}
