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

package org.apache.commons.numbers.complex;

import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Function;

@FunctionalInterface
public interface DComplexBinaryOperator extends BinaryOperator<DComplex> {

    DComplex apply(DComplex c1, DComplex c2, DComplexConstructor<DComplex> result);

    default DComplex apply(double r1, double i1, double r2, double i2, DComplexConstructor<DComplex> out) {
        return apply(Complex.ofCartesian(r1, i1), Complex.ofCartesian(r1, i1), out);
    }

    default DComplex apply(DComplex c1, DComplex c2) {
        return apply(c1, c2, DComplexConstructor.D_COMPLEX_RESULT);
    }

    default <V extends DComplex> DComplexBinaryOperator thenApply(Function<? super DComplex, ? extends V> after) {
        Objects.requireNonNull(after);
        return (DComplex c1, DComplex c2, DComplexConstructor<DComplex> out) -> after.apply(apply(c1, c2, out));
    }

    default <V extends DComplex> DComplexBinaryOperator thenApply(DComplexUnaryOperator after) {
        Objects.requireNonNull(after);
        return (DComplex c1, DComplex c2, DComplexConstructor<DComplex> out) -> after.apply(apply(c1, c2, out), out);

    }
}
