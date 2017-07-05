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
 * Tests for {@link LanczosApproximation}.
 */
public class LanczosApproximationTest {
    @Test
    public void testG() {
        Assert.assertEquals(607d / 128d, LanczosApproximation.g(), 0d);
    }

    @Test
    public void testSomeValues() {
        // Given that few values are checked, this test would only serve
        // as early warning of unexpected changes to the current code.
        Assert.assertEquals(29.020294557631818d, LanczosApproximation.value(0.1d), 0d);
        Assert.assertEquals(13.14778027539684d, LanczosApproximation.value(1.0d), 0d);
        Assert.assertEquals(7.897828855157814d, LanczosApproximation.value(2.0d), 0d);
    }
}
