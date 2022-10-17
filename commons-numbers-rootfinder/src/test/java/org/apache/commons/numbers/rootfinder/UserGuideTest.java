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
package org.apache.commons.numbers.rootfinder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for examples contained in the user guide.
 */
class UserGuideTest {

    @Test
    void testBrentSolver1() {
        double relAccuracy = 1e-6;
        double absAccuracy = 1e-14;
        double functionAccuracy = 1e-15;
        BrentSolver solver = new BrentSolver(relAccuracy,
                                             absAccuracy,
                                             functionAccuracy);
        double result1 = solver.findRoot(Math::sin, 3, 4);
        double result2 = solver.findRoot(Math::sin, 3, 3.14, 4); // With initial guess
        // result1 ~ result2 ~ Math.PI
        Assertions.assertEquals(Math.PI, result1, Math.PI * relAccuracy);
        Assertions.assertEquals(Math.PI, result2, Math.PI * relAccuracy);

        // *** Throws an IllegalArgumentException ***
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> solver.findRoot(Math::sin, 2, 3));
    }
}
