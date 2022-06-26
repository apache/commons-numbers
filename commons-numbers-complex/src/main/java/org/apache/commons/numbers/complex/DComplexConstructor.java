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
 * Define a constructor for a Complex.
 * @param <R> Generic
 */
@FunctionalInterface
public interface DComplexConstructor<R> {

    /**
     * A complex result constructor that return an instance of {@link Complex}.
     */
    DComplexConstructor<DComplex> D_COMPLEX_RESULT = DComplex::of;

    /**
     * Represents a function that accepts real and imaginary part of complex number and returns an object.
     * @param r real part
     * @param i imaginary part
     * @return R
     */
    R apply(double r, double i);


}

