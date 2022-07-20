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
 * Cartesian representation of a complex number. The complex number is expressed
 * in the form \( a + ib \) where \( a \) and \( b \) are real numbers and \( i \)
 * is the imaginary unit which satisfies the equation \( i^2 = -1 \). For the
 * complex number \( a + ib \), \( a \) is called the <em>real part</em> and
 * \( b \) is called the <em>imaginary part</em>.
 */
class ComplexNumber {

    /** The real part. */
    private final double real;
    /** The imaginary part. */
    private final double imaginary;

    /**
     * Constructor representing a complex number by its real and imaginary parts.
     *
     * @param real Real part \( a \) of the complex number \( (a +ib \).
     * @param imaginary Imaginary part \( b \) of the complex number \( (a +ib \).
     */
    ComplexNumber(double real, double imaginary) {
        this.real = real;
        this.imaginary = imaginary;
    }

    /**
     * Gets the real part \( a \) of complex number \( (a + i b) \).
     *
     * @return real part
     */
    double getReal() {
        return real;
    }

    /**
     * Gets the imaginary part \( b \) of complex number \( (a + i b) \).
     *
     * @return imaginary part
     */
    double getImaginary() {
        return imaginary;
    }
}
