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
package org.apache.commons.numbers.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for default methods of {@link Multiplication}.
 */
class MultiplicationTest {
    private static class MultiplicationTester implements Multiplication<MultiplicationTester> {
        private final int value;

        MultiplicationTester(int value) {
            this.value = value;
        }

        @Override
        public MultiplicationTester multiply(MultiplicationTester a) {
            throw new UnsupportedOperationException("not needed for testing");
        }

        @Override
        public MultiplicationTester one() {
            return new MultiplicationTester(1);
        }

        @Override
        public MultiplicationTester reciprocal() {
            throw new UnsupportedOperationException("not needed for testing");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MultiplicationTester that = (MultiplicationTester) o;
            return value == that.value;
        }

        @Override
        public int hashCode() {
            return value;
        }
    }

    @Test
    void testIsZero() {
        Assertions.assertTrue(new MultiplicationTester(1).isOne());
        Assertions.assertFalse(new MultiplicationTester(0).isOne());
    }
}
