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
package org.apache.commons.numbers.angle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for examples contained in the user guide.
 */
class UserGuideTest {

    @Test
    void testAngle1() {
        Angle.Deg a = Angle.Turn.of(0.5).toDeg();
        Angle.Rad b = a.toRad();
        Angle.Turn c = b.toTurn();
        Assertions.assertEquals(0.5, c.getAsDouble());
    }

    @Test
    void testAngleNormalizer1() {
        double angle = Angle.Deg.normalizer(-180).applyAsDouble(270);
        Assertions.assertEquals(-90.0, angle);
    }

    @Test
    void testReduce1() {
        Reduce reduce = new Reduce(0, 24);
        double hours1 = reduce.applyAsDouble(173.5);
        double hours2 = reduce.applyAsDouble(23.5);
        double hours3 = reduce.applyAsDouble(-10);
        Assertions.assertEquals(5.5, hours1);
        Assertions.assertEquals(23.5, hours2);
        Assertions.assertEquals(14.0, hours3);
    }

    @Test
    void testCosAngle1() {
        double[] v1 = {1, 0};
        double[] v2 = {0, 1};
        double[] v3 = {7, 7};
        double cosAngle1 = CosAngle.value(v1, v1);
        double cosAngle2 = CosAngle.value(v1, v2);
        double cosAngle3 = CosAngle.value(v1, v3);
        Assertions.assertEquals(1, cosAngle1);
        Assertions.assertEquals(0, cosAngle2);
        Assertions.assertEquals(0.7071067811865476, cosAngle3);
        Assertions.assertEquals(45.0, Math.toDegrees(Math.acos(cosAngle3)));
    }
}
