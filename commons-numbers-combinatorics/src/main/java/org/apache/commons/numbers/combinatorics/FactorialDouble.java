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

package org.apache.commons.numbers.combinatorics;

/**
 * @deprecated Since 1.1 this functionality has been replaced with {@link Factorial#doubleValue(int)}.
 *
 * <a href="http://mathworld.wolfram.com/Factorial.html">Factorial of a number</a>.
 */
@Deprecated
public final class FactorialDouble {
    /** Single instance. */
    private static final FactorialDouble INSTANCE = new FactorialDouble();

    /** No public instances. */
    private FactorialDouble() {}

    /**
     * @deprecated Since 1.1 this functionality has been replaced with {@link Factorial#doubleValue(int)}.
     *
     * <p>This class no longer supports a cache. This method returns a reference to a single instance.
     *
     * @return instance
     */
    @Deprecated
    public static FactorialDouble create() {
        return INSTANCE;
    }

    /**
     * @deprecated Since 1.1 this functionality has been replaced with {@link Factorial#doubleValue(int)}.
     *
     * <p>This class no longer supports a cache. This method returns a reference to the same object.
     *
     * @param cacheSize Ignored.
     * @return instance
     */
    @Deprecated
    public FactorialDouble withCache(final int cacheSize) {
        return this;
    }

    /**
     * @deprecated Since 1.1 this functionality has been replaced with {@link Factorial#doubleValue(int)}.
     *
     * <p>The result of calling this method is the same as calling the {@link Factorial#doubleValue(int)}.
     *
     * @param n Argument.
     * @return {@code n!}
     * @throws IllegalArgumentException if {@code n < 0}.
     */
    @Deprecated
    public double value(int n) {
        return Factorial.doubleValue(n);
    }
}
