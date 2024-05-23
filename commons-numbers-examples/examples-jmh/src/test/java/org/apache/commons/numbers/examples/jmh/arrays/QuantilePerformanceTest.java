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

package org.apache.commons.numbers.examples.jmh.arrays;

import java.util.EnumSet;
import org.apache.commons.numbers.examples.jmh.arrays.SelectionPerformance.AbstractDataSource;
import org.apache.commons.numbers.examples.jmh.arrays.SelectionPerformance.AbstractDataSource.Distribution;
import org.apache.commons.numbers.examples.jmh.arrays.SelectionPerformance.AbstractDataSource.Modification;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Executes tests for {@link SelectionPerformance}.
 */
class SelectionPerformanceTest {
    @Test
    void testGetDistribution() {
        assertGetEnumFromParam(Distribution.class);
    }

    @Test
    void testGetModification() {
        assertGetEnumFromParam(Modification.class);
    }

    static <E extends Enum<E>> void assertGetEnumFromParam(Class<E> cls) {
        Assertions.assertEquals(EnumSet.allOf(cls),
            AbstractDataSource.getEnumFromParam(cls, "all"));
        Assertions.assertThrows(IllegalStateException.class,
            () -> AbstractDataSource.getEnumFromParam(cls, "nothing"));
        for (final E e1 : cls.getEnumConstants()) {
            final String s = e1.name().toLowerCase();
            Assertions.assertEquals(EnumSet.of(e1),
                AbstractDataSource.getEnumFromParam(cls, e1.name()));
            Assertions.assertEquals(EnumSet.of(e1),
                AbstractDataSource.getEnumFromParam(cls, s));
            for (final E e2 : cls.getEnumConstants()) {
                Assertions.assertEquals(EnumSet.of(e1, e2),
                    AbstractDataSource.getEnumFromParam(cls, s + ":" + e2.name()));
                Assertions.assertEquals(EnumSet.of(e1, e2),
                    AbstractDataSource.getEnumFromParam(cls, e2.name() + ":" + e1.name()));
            }
        }
    }
}
