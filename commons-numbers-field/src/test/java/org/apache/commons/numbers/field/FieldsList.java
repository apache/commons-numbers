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
import java.util.Collections;

import org.apache.commons.numbers.fraction.Fraction;

/**
 * List of fields.
 */
public class FieldsList {
    /** List of all fields implemented in the library. */
    private static final List<FieldTestData[]> LIST =
        new ArrayList<FieldTestData[]>();

    static {
        try {
            // List of fields to test.
            add(new FractionField(),
                new Fraction(13, 4),
                new Fraction(5, 29),
                new Fraction(-279, 11));
            add(new FP64Field(),
                new FP64(23.45678901),
                new FP64(-543.2109876),
                new FP64(-234.5678901));

        } catch (Exception e) {
            System.err.println("Unexpected exception while creating the list of fields: " + e);
            e.printStackTrace(System.err);
            throw new RuntimeException(e);
        }
    }

    /**
     * @param field Field.
     * @param a Field element.
     * @param b Field element.
     */
    private static <T> void add(Field<T> field,
                                T a,
                                T b,
                                T c) {
        LIST.add(new FieldTestData[] { new FieldTestData(field, a, b, c) });
    }

    /**
     * Subclasses that are "parametric" tests can forward the call to
     * the "@Parameters"-annotated method to this method.
     *
     * @return the list of all fields.
     */
    public static Iterable<FieldTestData[]> list() {
        return Collections.unmodifiableList(LIST);
    }
}
