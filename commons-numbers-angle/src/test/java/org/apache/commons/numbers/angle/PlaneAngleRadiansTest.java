/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.commons.numbers.angle;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for the {@link PlaneAngleRadians} class.
 */
public class PlaneAngleRadiansTest {
    @Test
    public void testNormalize() {
        for (double a = -15.0; a <= 15.0; a += 0.1) {
            for (double b = -15.0; b <= 15.0; b += 0.2) {
                final double c = PlaneAngleRadians.normalize(a, b);
                Assert.assertTrue((b - Math.PI) <= c);
                Assert.assertTrue(c <= (b + Math.PI));
                double twoK = Math.rint((a - c) / Math.PI);
                Assert.assertEquals(c, a - twoK * Math.PI, 1e-14);
            }
        }
    }
}
