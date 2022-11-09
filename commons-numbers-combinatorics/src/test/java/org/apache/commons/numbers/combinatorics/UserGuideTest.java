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
package org.apache.commons.numbers.combinatorics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for examples contained in the user guide.
 */
class UserGuideTest {

    @Test
    void testBinomialCoefficient1() {
        long   bc1 = BinomialCoefficient.value(66, 33);
        double bc2 = BinomialCoefficientDouble.value(1029, 514);
        double bc3 = LogBinomialCoefficient.value(152635712, 125636);
        Assertions.assertEquals(7219428434016265740L, bc1);
        // Test with relative tolerance.
        // Expected values taken from the test suite.
        Assertions.assertEquals(1.429820686498904e308, bc2, bc2 * 1e-15);
        Assertions.assertEquals(1017897.199659759, bc3, bc3 * 1e-15);
    }

    @Test
    void testFactorial1() {
        long   f1 = Factorial.value(15);
        double f2 = Factorial.doubleValue(170);
        double f3 = LogFactorial.create().value(Integer.MAX_VALUE);
        Assertions.assertEquals(1307674368000L, f1);
        Assertions.assertEquals(7.257415615307999e306, f2);
        // This value is taken using LogFactorial.
        // It should be verified independently.
        // Currently this asserts the value in the user guide is correct.
        Assertions.assertEquals(4.3996705655378525e10, f3);
    }

    @Test
    void testLogFactorial1() {
        LogFactorial lf = LogFactorial.create().withCache(50);
        // This is a compilation test. Just verify the object is created.
        Assertions.assertNotNull(lf);
    }

    @Test
    void testCombinations1() {
        List<String> actual = new ArrayList<>();
        Combinations.of(4, 2).iterator().forEachRemaining(c -> actual.add(Arrays.toString(c)));
        Assertions.assertEquals(Arrays.asList(
                "[0, 1]",
                "[0, 2]",
                "[1, 2]",
                "[0, 3]",
                "[1, 3]",
                "[2, 3]"
            ), actual);
    }

    @Test
    void testCombinationsComparator1() {
        List<int[]> list = Arrays.asList(new int[][] {
            {3, 4, 5},
            {3, 1, 5},
            {3, 2, 5},
            {4, 2, 4},
        });
        list.sort(Combinations.of(6, 3).comparator());
        List<String> actual = new ArrayList<>();
        list.forEach(c -> actual.add(Arrays.toString(c)));
        Assertions.assertEquals(Arrays.asList(
                "[4, 2, 4]",
                "[3, 1, 5]",
                "[3, 2, 5]",
                "[3, 4, 5]"
            ), actual);
    }

    @Test
    void testStirlingS2() {
        Assertions.assertEquals(1, Stirling.stirlingS2(3, 1));
        Assertions.assertEquals(3, Stirling.stirlingS2(3, 2));
        Assertions.assertEquals(1, Stirling.stirlingS2(3, 3));
    }
}
