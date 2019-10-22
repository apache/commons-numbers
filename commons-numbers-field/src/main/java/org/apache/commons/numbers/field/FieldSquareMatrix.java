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

/**
 * Square matrix whose elements define a {@link Field}.
 *
 * @param <T> Type of the field elements.
 */
public class FieldSquareMatrix<T> {
    /** Field. */
    private final Field<T> field;
    /** Dimension. */
    private final int dim;
    /** Data storage (in row-major order). */
    private final T[] data;

    /**
     * @param f Field.
     * @param n Dimension of the matrix.
     * @throws IllegalArgumentException if {@code n <= 0}.
     */
    private FieldSquareMatrix(Field<T> f,
                              int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("Negative size");
        }

        field = f;
        dim = n;
        data = (T[]) new Object[n * n];
    }

    /**
     * Factory method.
     *
     * @param f Field.
     * @param n Dimension of the matrix.
     * @return a new instance.
     * @throws IllegalArgumentException if {@code n <= 0}.
     */
    public static <T> FieldSquareMatrix<T> create(Field<T> f,
                                                  int n) {
        return new FieldSquareMatrix<>(f, n);
    }

    /**
     * Factory method.
     *
     * @param f Field.
     * @param n Dimension of the matrix.
     * @return a matrix with elements zet to {@link Field#zero() zero}.
     * @throws IllegalArgumentException if {@code n <= 0}.
     */
    public static <T> FieldSquareMatrix<T> zero(Field<T> f,
                                                int n) {
        return create(f, n).fill(f.zero());
    }

    /**
     * Factory method.
     *
     * @param f Field.
     * @param n Dimension of the matrix.
     * @return the identity matrix.
     * @throws IllegalArgumentException if {@code n <= 0}.
     */
    public static <T> FieldSquareMatrix<T> identity(Field<T> f,
                                                    int n) {
        final FieldSquareMatrix<T> r = zero(f, n);

        for (int i = 0; i < n; i++) {
            r.set(i, i, f.one());
        }

        return r;
    }

    /**
     * Copies this matrix.
     *
     * @return a new instance.
     */
    public FieldSquareMatrix<T> copy() {
        final FieldSquareMatrix<T> r = create(field, dim);
        System.arraycopy(data, 0, r.data, 0, data.length);
        return r;
    }

    /**
     * @return the dimension of the matrix.
     */
    public int getDimension() {
        return dim;
    }

    /**
     * Sets all elements to the given value.
     *
     * @param value Value of the elements of the matrix.
     * @return {@code this}.
     */
    public FieldSquareMatrix<T> fill(T value) {
        Arrays.fill(data, value);
        return this;
    }

    /**
     * Gets an element.
     *
     * @param i Row.
     * @param j Column.
     * @return the element at (i, j).
     */
    public T get(int i,
                 int j) {
        return data[i * dim + j];
    }

    /**
     * Sets an element.
     *
     * @param i Row.
     * @param j Column.
     * @param value Value.
     */
    public void set(int i,
                    int j,
                    T value) {
        data[i * dim + j] = value;
    }

    /**
     * Addition.
     *
     * @param other Matrix to add.
     * @return a new instance with the result of the addition.
     */
    public FieldSquareMatrix<T> add(FieldSquareMatrix<T> other) {
        checkDimension(other);
        final FieldSquareMatrix<T> r = create(field, dim);

        for (int i = 0; i < data.length; i++) {
            r.data[i] = field.add(data[i], other.data[i]);
        }

        return r;
    }

    /**
     * Subtraction.
     *
     * @param other Matrix to subtract.
     * @return a new instance with the result of the subtraction.
     */
    public FieldSquareMatrix<T> subtract(FieldSquareMatrix<T> other) {
        checkDimension(other);
        final FieldSquareMatrix<T> r = create(field, dim);

        for (int i = 0; i < data.length; i++) {
            r.data[i] = field.subtract(data[i], other.data[i]);
        }

        return r;
    }

    /**
     * Negate.
     *
     * @return a new instance with the opposite matrix.
     */
    public FieldSquareMatrix<T> negate() {
        final FieldSquareMatrix<T> r = create(field, dim);

        for (int i = 0; i < data.length; i++) {
            r.data[i] = field.negate(data[i]);
        }

        return r;
    }

    /**
     * Multiplication.
     *
     * @param factor Matrix to multiply with.
     * @return a new instance with the result of the multiplication.
     */
    public FieldSquareMatrix<T> multiply(FieldSquareMatrix<T> other) {
        checkDimension(other);
        final FieldSquareMatrix<T> r = zero(field, dim);

        for (int i = 0; i < dim; i++) {
            final int o1 = i * dim;
            for (int j = 0; j < dim; j++) {
                final int o2 = o1 + j;
                for (int k = 0; k < dim; k++) {
                    r.data[o2] = field.add(r.data[o2],
                                           field.multiply(data[o1 + k],
                                                          other.data[k * dim + j]));
                }
            }
        }

        return r;
    }

    /**
     * Multiplies the matrix with itself {@code p} times.
     *
     * @param p Exponent.
     * @return a new instance.
     * @throws IllegalArgumentException if {@code p < 0}.
     */
    public FieldSquareMatrix<T> pow(int p) {
        if (p < 0) {
            throw new IllegalArgumentException("Negative exponent: " + p);
        }

        if (p == 0) {
            return identity(field, dim);
        }

        if (p == 1) {
            return copy();
        }

        final int power = p - 1;

        // Only log_2(p) operations are necessary by doing as follows:
        //    5^214 = 5^128 * 5^64 * 5^16 * 5^4 * 5^2
        // The same approach is used for A^p.

        final char[] binary = Integer.toBinaryString(power).toCharArray();
        final ArrayList<Integer> nonZeroPositions = new ArrayList<>();

        for (int i = 0; i < binary.length; i++) {
            if (binary[i] == '1') {
                final int pos = binary.length - i - 1;
                nonZeroPositions.add(pos);
            }
        }

        final List<FieldSquareMatrix<T>> results = new ArrayList<>(binary.length);
        results.add(this);
        for (int i = 1; i < binary.length; i++) {
            final FieldSquareMatrix<T> s = results.get(i - 1);
            final FieldSquareMatrix<T> r = s.multiply(s);
            results.add(r);
        }

        FieldSquareMatrix<T> r = this;
        for (Integer i : nonZeroPositions) {
            r = r.multiply(results.get(i));
        }

        return r;
    }

    /**
     * Check that the given matrix has the same dimensions.
     *
     * @param factor Matrix to check.
     * @throws IllegalArgumentException if the dimensions do not match.
     */
    private void checkDimension(FieldSquareMatrix<T> other) {
        if (dim != other.dim) {
            throw new IllegalArgumentException("Dimension mismatch: " +
                                               dim + " != " + other.dim);
        }
    }
}
