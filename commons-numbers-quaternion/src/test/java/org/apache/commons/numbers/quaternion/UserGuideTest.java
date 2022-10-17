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
package org.apache.commons.numbers.quaternion;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for examples contained in the user guide.
 */
class UserGuideTest {

    @Test
    void testQuaternion1() {
        double w = 2;
        double x = 3;
        double y = 4;
        double z = 5;
        Quaternion h1 = Quaternion.of(w, x, y, z);
        Quaternion h2 = Quaternion.of(w, new double[] {x, y, z});
        Assertions.assertEquals(w, h1.getW());
        Assertions.assertEquals(w, h1.getScalarPart());
        Assertions.assertArrayEquals(new double[] {x, y, z}, h1.getVectorPart());
        Assertions.assertEquals(h1, h2);
    }

    @Test
    void testQuaternion2() {
        Quaternion h = Quaternion.of(1, 2, 3, 4);
        Quaternion h1 = h.add(Quaternion.ONE);
        Quaternion hi = h.add(Quaternion.I);
        Quaternion hj = h.add(Quaternion.J);
        Quaternion hk = h.add(Quaternion.K);
        Assertions.assertEquals(Quaternion.of(2, 2, 3, 4), h1);
        Assertions.assertEquals(Quaternion.of(1, 3, 3, 4), hi);
        Assertions.assertEquals(Quaternion.of(1, 2, 4, 4), hj);
        Assertions.assertEquals(Quaternion.of(1, 2, 3, 5), hk);
    }

    @Test
    void testQuaternion3() {
        Quaternion h1 = Quaternion.of(1, 2, 3, 4);
        Quaternion h2 = Quaternion.of(5, 6, 7, 8);
        Quaternion result1 = h1.multiply(h2);
        Quaternion result2 = Quaternion.multiply(h1, h2);
        Assertions.assertEquals(result1, result2);
    }

    @Test
    void testQuaternion4() {
        Quaternion q1 = Quaternion.of(1, 2, 3, 4);
        Quaternion q2 = Quaternion.of(1, 2, 3, 4 + 1e-10);
        Assertions.assertFalse(q1.equals(q2));
        Assertions.assertTrue(q1.equals(q2, 1e-9));
    }

    @Test
    void testSlerp1() {
        Quaternion q1 = SlerpTest.createZRotation(0.75 * Math.PI);
        Quaternion q2 = SlerpTest.createZRotation(0.76 * Math.PI);

        Slerp slerp = new Slerp(q1, q2);

        Assertions.assertTrue(slerp.apply(0.0).equals(q1, 1e-7));
        Assertions.assertTrue(slerp.apply(0.25).equals(SlerpTest.createZRotation(0.7525 * Math.PI), 1e-7));
        Assertions.assertTrue(slerp.apply(0.5).equals(SlerpTest.createZRotation(0.755 * Math.PI), 1e-7));
        Assertions.assertTrue(slerp.apply(0.75).equals(SlerpTest.createZRotation(0.7575 * Math.PI), 1e-7));
        Assertions.assertTrue(slerp.apply(0.0).equals(q1, 1e-7));
    }
}
