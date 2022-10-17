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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for examples contained in the user guide.
 */
class UserGuideTest {

    @Test
    void testComplex1() {
        double x = 3;
        double y = 4;
        Complex c1 = Complex.ofCartesian(x, y);
        // c1 == x + iy

        double rho = 1.23;
        double theta = Math.PI / 2;
        Complex c2 = Complex.ofPolar(rho, theta);

        // This is a compilation test. Objects must be valid.
        Assertions.assertNotNull(c1);
        Assertions.assertNotNull(c2);
    }

    @Test
    void testComplex2() {
        Complex c1 = Complex.ofCartesian(3, 4);
        Complex c2 = Complex.ofCartesian(5, 6);
        Complex c3 = c1.multiply(c2).sqrt();

        double magnitude = c3.abs();
        double argument = c3.arg();

        boolean finite = c3.isFinite();

        // This is a compilation test. Objects must be valid.
        Assertions.assertTrue(Double.isFinite(magnitude));
        Assertions.assertTrue(Double.isFinite(argument));
        Assertions.assertTrue(finite);
    }
}
