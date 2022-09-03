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

package org.apache.commons.numbers.complex.arrays;

import org.apache.commons.numbers.complex.Complex;
import org.apache.commons.numbers.complex.ComplexFunctions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class ComplexListFunctionsTest {

    @Test
    void testComplexUnaryOperator() {
        List<Complex> objectList = generateList(10);
        List<Complex> expectedList = objectList.stream().map(Complex::conj).collect(Collectors.toList());
        ComplexList actualList = new ComplexList();
        actualList.addAll(objectList);
        actualList.replaceAll(ComplexFunctions::conj);
        Assertions.assertEquals(expectedList, actualList);
    }

    @Test
    void testComplexBinaryOperator() {
        List<Complex> objectList = generateList(10);
        double r = 2;
        double i = 3;
        Complex multiplier = Complex.ofCartesian(r, i);
        List<Complex> expectedList = objectList.stream().map(c -> c.multiply(multiplier)).collect(Collectors.toList());
        ComplexList actualList = new ComplexList();
        actualList.addAll(objectList);
        actualList.replaceAll((x, y, action) -> ComplexFunctions.multiply(x, y, r, i, action));
        Assertions.assertEquals(expectedList, actualList);
    }

    @Test
    void testComplexScalarFunction() {
        List<Complex> objectList = generateList(10);
        double factor = 2;
        List<Complex> expectedList = objectList.stream().map(c -> c.pow(factor)).collect(Collectors.toList());
        ComplexList actualList = new ComplexList();
        actualList.addAll(objectList);
        actualList.replaceAll((x, y, action) -> ComplexFunctions.pow(x, y, factor, action));
        Assertions.assertEquals(expectedList, actualList);
    }

    private List<Complex> generateList(int size) {
        return ThreadLocalRandom.current().doubles(size, -Math.PI, Math.PI)
           .mapToObj(Complex::ofCis).collect(Collectors.toList());
    }
}
