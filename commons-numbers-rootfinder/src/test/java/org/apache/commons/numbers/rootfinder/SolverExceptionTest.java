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
 * Test cases for the {@link SolverException} class.
 */
class SolverExceptionTest {
    @Test
    void testOutOfRangeFormatting() {
        final double x = Double.MIN_NORMAL;
        final double y = Double.MIN_VALUE;
        final double z = 1.0;
        final String msg = new SolverException(SolverException.OUT_OF_RANGE,
                x, y, z).getMessage();
        Assertions.assertTrue(msg.contains(String.valueOf(x)), () -> "Missing: " + x);
        Assertions.assertTrue(msg.contains(String.valueOf(y)), () -> "Missing: " + y);
        Assertions.assertTrue(msg.contains(String.valueOf(z)), () -> "Missing: " + z);
    }

    @Test
    void testBracketingFormatting() {
        final double w = Double.MAX_VALUE;
        final double x = Double.MIN_NORMAL;
        final double y = Double.MIN_VALUE;
        final double z = 1.0;
        final String msg = new SolverException(SolverException.BRACKETING,
                w, x, y, z).getMessage();
        Assertions.assertTrue(msg.contains(String.valueOf(w)), () -> "Missing: " + w);
        Assertions.assertTrue(msg.contains(String.valueOf(x)), () -> "Missing: " + x);
        Assertions.assertTrue(msg.contains(String.valueOf(y)), () -> "Missing: " + y);
        Assertions.assertTrue(msg.contains(String.valueOf(z)), () -> "Missing: " + z);
    }

    @Test
    void testTooLargeFormatting() {
        final double x = Double.MIN_NORMAL;
        final double y = Double.MIN_VALUE;
        final String msg = new SolverException(SolverException.TOO_LARGE,
                x, y).getMessage();
        Assertions.assertTrue(msg.contains(String.valueOf(x)), () -> "Missing: " + x);
        Assertions.assertTrue(msg.contains(String.valueOf(y)), () -> "Missing: " + y);
    }
}
