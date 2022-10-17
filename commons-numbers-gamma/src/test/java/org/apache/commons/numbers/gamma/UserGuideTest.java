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
package org.apache.commons.numbers.gamma;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for examples contained in the user guide.
 */
class UserGuideTest {

    @Test
    void testGamma1() {
        double result = Erfc.value(1.23);
        Assertions.assertTrue(0 <= result && result <= 2);

        // Default function evaluation
        double a = 1.23;
        double x = 4.56;
        double result1 = IncompleteGamma.Lower.value(a, x);

        // Parameterize function evaluation
        double epsilon = 1e-10;
        int maxIterations = 1000;
        double result2 = IncompleteGamma.Lower.value(a, x, epsilon, maxIterations);
        Assertions.assertEquals(result1, result2, result1 * 1e-14);
    }
}
