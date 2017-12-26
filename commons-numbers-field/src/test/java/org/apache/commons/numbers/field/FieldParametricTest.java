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
package org.apache.commons.numbers.field;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for fields.
 */
@RunWith(value=Parameterized.class)
public class FieldParametricTest {
    /** Field under test. */
    private final Field field;
    private final Object a;
    private final Object b;
    private final Object c;

    /**
     * Initializes data instance.
     *
     * @param data Field data to be tested.
     */
    public FieldParametricTest(FieldTestData data) {
        this.field = data.getField();
        this.a = data.getA();
        this.b = data.getB();
        this.c = data.getC();
    }

    @Parameters(name = "{index}: data={0}")
    public static Iterable<FieldTestData[]> getList() {
        return FieldsList.list();
    }

    @Test
    public void testAdditionAssociativity() {
        final Object r1 = field.add(field.add(a, b), c);
        final Object r2 = field.add(a, field.add(b, c));
        Assert.assertTrue(r1.equals(r2));
    }
    @Test
    public void testAdditionCommutativity() {
        final Object r1 = field.add(a, b);
        final Object r2 = field.add(b, a);
        Assert.assertTrue(r1.equals(r2));
    }
    @Test
    public void testAdditiveIdentity() {
        final Object r1 = field.add(a, field.zero());
        final Object r2 = a;
        Assert.assertTrue(r1.equals(r2));
    }
    @Test
    public void testAdditiveInverse() {
        final Object r1 = field.add(a, field.negate(a));
        final Object r2 = field.zero();
        Assert.assertTrue(r1.equals(r2));
    }

    @Test
    public void testMultiplicationAssociativity() {
        final Object r1 = field.multiply(field.multiply(a, b), c);
        final Object r2 = field.multiply(a, field.multiply(b, c));
        Assert.assertTrue(r1.equals(r2));
    }
    @Test
    public void testMultiplicationCommutativity() {
        final Object r1 = field.multiply(a, b);
        final Object r2 = field.multiply(b, a);
        Assert.assertTrue(r1.equals(r2));
    }
    @Test
    public void testMultiplicativeIdentity() {
        final Object r1 = field.multiply(a, field.one());
        final Object r2 = a;
        Assert.assertTrue(r1.equals(r2));
    }
    @Test
    public void testMultiplicativeInverse() {
        final Object r1 = field.multiply(a, field.reciprocal(a));
        final Object r2 = field.one();
        Assert.assertTrue(r1.equals(r2));
    }

    @Test
    public void testDistributivity() {
        final Object r1 = field.multiply(a, field.add(b, c));
        final Object r2 = field.add(field.multiply(a, b), field.multiply(a, c));
        Assert.assertTrue(r1.equals(r2));        
    }
}
