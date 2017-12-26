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

/**
 * Interface representing a <a href="http://mathworld.wolfram.com/Field.html">field</a>.
 *
 * @param <T> Type of the field elements.
 */
public interface Field<T> {
    /**
     * @param a Field element.
     * @param b Field element.
     * @return {@code a + b}.
     */
    T add(T a, T b);

    /**
     * @param a Field element.
     * @param b Field element.
     * @return {@code a - b}.
     */
    T subtract(T a, T b);

    /**
     * @param a Field element.
     * @return {@code -a}.
     */
    T negate(T a);

    /**
     * @param a Field element.
     * @param n Number of times {@code a} must be added to itself.
     * @return {@code n a}.
     */
    T multiply(int n, T a);

    /**
     * @param a Field element.
     * @param b Field element.
     * @return {@code a * b}.
     */
    T multiply(T a, T b);

    /**
     * @param a Field element.
     * @param b Field element.
     * @return <code>a * b<sup>-1</sup></code>.
     */
    T divide(T a, T b);

    /**
     * @param a Field element.
     * @return <code>a<sup>-1</sup></code>.
     */
    T reciprocal(T a);

    /**
     * @return the field element {@code 1} such that for all {@code a},
     * {@code 1 * a == a}.
     */
    T one();

    /**
     * @return the field element {@code 0} such that for all {@code a},
     * {@code 0 + a == a}.
     */
    T zero();
}
