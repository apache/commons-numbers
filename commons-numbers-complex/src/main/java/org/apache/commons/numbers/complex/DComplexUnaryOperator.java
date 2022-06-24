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
import java.util.function.Function;
import java.util.function.UnaryOperator;

@FunctionalInterface
public interface DComplexUnaryOperator extends UnaryOperator<DComplex> {


    DComplex apply(DComplex in, DComplexConstructor<DComplex> out);


    default DComplex apply(double r, double i, DComplexConstructor<DComplex> out) {
        return apply(Complex.ofCartesian(r, i), out);
    }
    default DComplex apply(DComplex c) {
        return apply(c, DComplexConstructor.D_COMPLEX_RESULT);
    }

    default <V extends DComplex> DComplexUnaryOperator thenApply(Function<? super DComplex, ? extends V> after) {
        Objects.requireNonNull(after);
        return (DComplex c, DComplexConstructor<DComplex> out) -> after.apply(apply(c, out));

    }

    default <V extends DComplex> DComplexUnaryOperator thenApply(DComplexUnaryOperator after) {
        Objects.requireNonNull(after);
        return (DComplex c, DComplexConstructor<DComplex> out) -> after.apply(apply(c, out), out);

    }

    default <V extends DComplex> DComplexBinaryOperator thenApplyBinaryOperator(DComplexBinaryOperator after) {
        Objects.requireNonNull(after);
        return (DComplex c1, DComplex c2, DComplexConstructor<DComplex> out) -> after.apply(apply(c1, out), c2, out);
    }

    default <V extends DComplex> DComplexScalarFunction thenApplyScalarFunction(DComplexScalarFunction after) {
        Objects.requireNonNull(after);
        return (DComplex c1, double d, DComplexConstructor<DComplex> out) -> after.apply(apply(c1, out), d, out);
    }
}
