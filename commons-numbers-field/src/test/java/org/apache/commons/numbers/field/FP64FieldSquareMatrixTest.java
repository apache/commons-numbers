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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.util.Pair;

/**
 * Tests for {@link FieldSquareMatrix} (using {@link FP64} as field elements).
 */
public class FP64FieldSquareMatrixTest {
    @Test
    public void testGetDimension() {
        final int dim = 6;
        final FieldSquareMatrix<FP64> a = FieldSquareMatrix.create(FP64Field.get(), dim);
        Assertions.assertEquals(dim, a.getDimension());
    }

    @Test
    public void testSetGet() {
        final int dim = 20;
        final FieldSquareMatrix<FP64> a = FieldSquareMatrix.create(FP64Field.get(), dim);

        int count = 0;
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                a.set(i, j, FP64.of(count++));
            }
        }
        Assertions.assertEquals(dim * dim, count);

        count = 0;
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                Assertions.assertEquals((double) count++,
                                        a.get(i, j).doubleValue(),
                                        0d);
            }
        }
    }

    @Test
    public void testAdd() {
        final int dim = 6;
        final double scale = 1e3;
        final Pair<FieldSquareMatrix<FP64>, RealMatrix> p1 = createRandom(dim, scale);
        final Pair<FieldSquareMatrix<FP64>, RealMatrix> p2 = createRandom(dim, scale);

        assertEquals(p1.getFirst().add(p2.getFirst()),
                     p1.getSecond().add(p2.getSecond()),
                     0d);
    }

    @Test
    public void testSubtract() {
        final int dim = 6;
        final double scale = 1e3;
        final Pair<FieldSquareMatrix<FP64>, RealMatrix> p1 = createRandom(dim, scale);
        final Pair<FieldSquareMatrix<FP64>, RealMatrix> p2 = createRandom(dim, scale);

        assertEquals(p1.getFirst().subtract(p2.getFirst()),
                     p1.getSecond().subtract(p2.getSecond()),
                     0d);
    }

    @Test
    public void testMultiply() {
        final int dim = 7;
        final double scale = 1e2;
        final Pair<FieldSquareMatrix<FP64>, RealMatrix> p1 = createRandom(dim, scale);
        final Pair<FieldSquareMatrix<FP64>, RealMatrix> p2 = createRandom(dim, scale);

        assertEquals(p1.getFirst().multiply(p2.getFirst()),
                     p1.getSecond().multiply(p2.getSecond()),
                     0d);
    }

    @Test
    public void testNegate() {
        final int dim = 13;
        final double scale = 1;
        final Pair<FieldSquareMatrix<FP64>, RealMatrix> p = createRandom(dim, scale);

        assertEquals(p.getFirst().negate(),
                     p.getSecond().scalarMultiply(-1),
                     0d);
    }

    @Test
    public void testPowZero() {
        final int dim = 5;
        final double scale = 1e100;
        final Pair<FieldSquareMatrix<FP64>, RealMatrix> p = createRandom(dim, scale);

        final int exp = 0;
        assertEquals(p.getFirst().pow(exp),
                     p.getSecond().power(exp),
                     0d);
    }

    @Test
    public void testPowOne() {
        final int dim = 5;
        final double scale = 1e100;
        final Pair<FieldSquareMatrix<FP64>, RealMatrix> p = createRandom(dim, scale);

        final int exp = 1;
        assertEquals(p.getFirst().pow(exp),
                     p.getSecond().power(exp),
                     0d);
    }

    @Test
    public void testPow() {
        final int dim = 5;
        final double scale = 1e2;
        final Pair<FieldSquareMatrix<FP64>, RealMatrix> p = createRandom(dim, scale);

        final int exp = 4;
        assertEquals(p.getFirst().pow(exp),
                     p.getSecond().power(exp),
                     0d);
    }

    /**
     * Compares with result obtained from "Commons Math".
     *
     * @param a "Commons Numbers" result.
     * @param b "Commons Math" result.
     * @param tol Tolerance.
     */
    private void assertEquals(FieldSquareMatrix<FP64> a,
                              RealMatrix b,
                              double tol) {
        final int dim = a.getDimension();
        if (dim != b.getRowDimension() ||
            dim != b.getColumnDimension()) {
            Assertions.fail("Dimension mismatch"); 
        }

        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                Assertions.assertEquals(a.get(i, j).doubleValue(),
                                        b.getEntry(i, j),
                                        tol,
                                        "(" + i + ", " + j + ")");
            }
        }
    }

    /**
     * Creates test matrices with random entries.
     *
     * @param dim Dimension.
     * @param scale Range of the entries.
     * @return a pair of matrices whose entries are in the interval
     * {@code [-scale, scale]}.
     */
    private Pair<FieldSquareMatrix<FP64>, RealMatrix> createRandom(int dim,
                                                                   double scale) {
        final FieldSquareMatrix<FP64> a = FieldSquareMatrix.create(FP64Field.get(), dim);
        final RealMatrix b = new Array2DRowRealMatrix(dim, dim);
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                final double v = scale * (2 * Math.random() - 1);
                a.set(i, j, FP64.of(v));
                b.setEntry(i, j, v);
            }
        }

        return new Pair<>(a, b);
    }
}
