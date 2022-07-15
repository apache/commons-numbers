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

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Complex} and {@link ComplexFunctions}.
 *
 * <p>Note: The ISO C99 math functions are not fully tested in this class. See also:
 *
 * <ul>
 * <li>{@link CStandardTest} for a test of the ISO C99 standards including special case handling.
 * <li>{@link CReferenceTest} for a test of the output using standard finite value against an
 *     ISO C99 compliant reference implementation.
 * <li>{@link ComplexEdgeCaseTest} for a test of extreme edge case finite values for real and/or
 *     imaginary parts that can create intermediate overflow or underflow.
 * </ul>
 */
class ComplexFunctionsTest {

    @Test
    void testLog10() {
        final double ln10 = Math.log(10);
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        for (int i = 0; i < 10; i++) {
            final Complex z = Complex.ofCartesian(rng.nextDouble() * 2, rng.nextDouble() * 2);
            final Complex lnz = z.log();
            final Complex log10z = z.log10();
            // real part is prone to floating-point error so use a delta
            // imaginary part should be exact
            Complex expected = Complex.ofCartesian(lnz.getReal() / ln10, lnz.getImaginary());
            TestUtils.assertComplexUnary(z, Complex::log10, ComplexFunctions::log10, expected, "log10", 1e-12, 0.0D);

        }
    }
}
