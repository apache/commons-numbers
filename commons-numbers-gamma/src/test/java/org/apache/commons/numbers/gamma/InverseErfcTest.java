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

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link InverseErfc}.
 */
public class InverseErfcTest {
    @Test
    public void testErfcInvNaN() {
        Assert.assertTrue(Double.isNaN(InverseErfc.value(-0.001)));
        Assert.assertTrue(Double.isNaN(InverseErfc.value(+2.001)));
    }

    @Test
    public void testErfcInvInfinite() {
        Assert.assertTrue(Double.isInfinite(InverseErfc.value(-0)));
        Assert.assertTrue(InverseErfc.value( 0) > 0);
        Assert.assertTrue(Double.isInfinite(InverseErfc.value(+2)));
        Assert.assertTrue(InverseErfc.value(+2) < 0);
    }

    @Test
    public void testErfcInv() {
        for (double x = -5.85; x < 5.9; x += 0.01) {
            final double y = Erfc.value(x);
            final double dydxAbs = 2 * Math.exp(-x * x) / Math.sqrt(Math.PI);
            Assert.assertEquals(x, InverseErfc.value(y), 1.0e-15 / dydxAbs);
        }
    }
}
