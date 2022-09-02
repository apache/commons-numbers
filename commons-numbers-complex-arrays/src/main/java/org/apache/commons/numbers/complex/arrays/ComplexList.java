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
import org.apache.commons.numbers.complex.ComplexBinaryOperator;
import org.apache.commons.numbers.complex.ComplexDouble;
import org.apache.commons.numbers.complex.ComplexScalarFunction;
import org.apache.commons.numbers.complex.ComplexSink;
import org.apache.commons.numbers.complex.ComplexUnaryOperator;

import java.io.File;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.UnaryOperator;

/**
 * Resizable-double array implementation of the List interface. Implements all optional list operations,
 * and permits all elements. In addition to implementing the List interface,
 * this class provides methods to manipulate the size of the array that is used internally to store the list.
 *
 * <p>Each ComplexList instance has a capacity. The capacity is half the size of the double array used to store the elements
 * in the list. As elements are added to an ComplexList, its capacity grows automatically.
 * The complex number is stored using an interleaved format and so the maximum number of elements that may be added is
 * approximately 2^30. This is half the maximum capacity of java.util.ArrayList.
 * The memory usage is more efficient than using a List of Complex objects as the underlying numbers are not stored
 * using instances of Complex.</p>
 *
 * <p>An application can increase the capacity of an ComplexList instance before adding a large number of elements
 * using the ensureCapacity operation. This may reduce the amount of incremental reallocation.</p>
 */
public class ComplexList extends AbstractList<Complex> {

    /**
     * The maximum size of array to allocate.
     * Ensuring Max capacity is even with additional space for vm array headers.
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 9;

    /**
     * Max capacity for size of complex numbers in the list.
     */
    private static final int MAX_CAPACITY = MAX_ARRAY_SIZE / 2;

    /**
     * error in case of allocation above max capacity.
     */
    private static final String OOM_ERROR_STRING = "cannot allocate capacity %s greater than max " + MAX_CAPACITY;

    /**
     * Default initial capacity.
     */
    private static final int DEFAULT_CAPACITY = 8;

    /**
     * Size label message.
     */
    private static final String SIZE_MSG = ", Size: ";
    /**
     * Index position label message.
     */
    private static final String INDEX_MSG = "Index: ";

    /**
     * The double array buffer into which the elements of the ComplexList are stored.
     */
    private double[] realAndImagParts;

    /**
     * Size of ComplexList.
     */
    private int size;

    /**
     * Constructs an empty list up to the specified capacity without a memory reallocation.
     *
     * @param capacity Capacity of list.
     * @throws IllegalArgumentException if the {@code capacity} is greater than {@code MAX_CAPACITY}.
     */
    public ComplexList(int capacity) {
        if (capacity > MAX_CAPACITY) {
            throw new IllegalArgumentException(String.format(OOM_ERROR_STRING, capacity));
        }
        final int arrayLength = Math.max(DEFAULT_CAPACITY, capacity) * 2;
        realAndImagParts = new double[arrayLength];
    }

    /**
     * Constructs an empty list.
     */
    public ComplexList() {
        realAndImagParts = new double[DEFAULT_CAPACITY * 2];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Checks if the given index is in range.
     *
     * @param index Index of the element to range check.
     * @throws IndexOutOfBoundsException if index isn't within the range.
     */
    private void rangeCheck(int index) {
        if (index >= size) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        }
    }

    /**
     * A version of rangeCheck used by add and addAll.
     *
     * @param index Index of the element to range check.
     * @throws IndexOutOfBoundsException if index isn't within the range of list.
     */
    private void rangeCheckForInsert(int index) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        }
    }

    /**
     * Gets the complex number \( (a + i b) \) at the indexed position of the list.
     *
     * {@inheritDoc}
     * @return the complex number.
     */
    @Override
    public Complex get(int index) {
        rangeCheck(index);
        final int i = index << 1;
        return Complex.ofCartesian(realAndImagParts[i], realAndImagParts[i + 1]);
    }

    /**
     * Replaces the element at the specified position in this list with the specified element's
     * real and imaginary parts. No range checks are done.
     *
     * @param index Index of the element to replace.
     * @param real Real part \( a \) of the complex number \( (a +ib) \).
     * @param imaginary Imaginary part \( b \) of the complex number \( (a +ib) \).
     */
    private void setNoRangeCheck(int index, double real, double imaginary) {
        final int i = index << 1;
        realAndImagParts[i] = real;
        realAndImagParts[i + 1] = imaginary;
    }

    /**
     * {@inheritDoc}
     *
     * @param element Complex element to be set.
     */
    @Override
    public Complex set(int index, Complex element) {
        rangeCheck(index);
        final int i = index << 1;
        final Complex oldValue = Complex.ofCartesian(realAndImagParts[i], realAndImagParts[i + 1]);
        setNoRangeCheck(index, element.getReal(), element.getImaginary());
        return oldValue;
    }

    /**
     * Increases the capacity of this ComplexList instance, if necessary, to ensure that it can hold at
     * least the number of elements specified by the minimum capacity argument.
     *
     * @param minCapacity Desired minimum capacity.
     * @return the backing double array.
     * @throws OutOfMemoryError if the {@code minCapacity} is greater than {@code MAX_ARRAY_SIZE}.
     */
    private double[] ensureCapacityInternal(int minCapacity) {
        modCount++;
        final long minArrayCapacity = Integer.toUnsignedLong(minCapacity) << 1;
        if (minArrayCapacity > MAX_ARRAY_SIZE) {
            throw new OutOfMemoryError(String.format(OOM_ERROR_STRING, minArrayCapacity));
        }
        final long oldArrayCapacity = realAndImagParts.length;
        if (minArrayCapacity > oldArrayCapacity) {
            long newArrayCapacity = oldArrayCapacity + (oldArrayCapacity >> 1);
            // Round-odd up to even
            newArrayCapacity += newArrayCapacity & 1;

            // Ensure minArrayCapacity <= newArrayCapacity <= MAX_ARRAY_SIZE
            // Note: At this point minArrayCapacity <= MAX_ARRAY_SIZE
            if (newArrayCapacity > MAX_ARRAY_SIZE) {
                newArrayCapacity = MAX_ARRAY_SIZE;
            } else if (newArrayCapacity < minArrayCapacity) {
                newArrayCapacity = minArrayCapacity;
            }
            realAndImagParts = Arrays.copyOf(realAndImagParts, (int) newArrayCapacity);
        }
        return realAndImagParts;
    }

    /**
     * Increases the capacity of this ComplexList instance, if necessary, to ensure that it can hold at
     * least an additional number of elements specified by the capacity argument.
     *
     * @param capacity Desired capacity.
     */
    private void expand(int capacity) {
        ensureCapacityInternal(size + capacity);
    }

    /**
     * {@inheritDoc}
     *
     * @param element Complex element to be appended to this list
     */
    @Override
    public boolean add(Complex element) {
        double[] e = realAndImagParts;
        if (size == (e.length >>> 1)) {
            e = ensureCapacityInternal(size + 1);
        }
        final int i = size << 1;
        e[i] = element.real();
        e[i + 1] = element.imag();
        size++;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(int index, Complex element) {
        rangeCheckForInsert(index);
        double[] e = realAndImagParts;
        if (size == e.length >>> 1) {
            e = ensureCapacityInternal(size + 1);
        }
        final int i = index << 1;
        final int s = size << 1;
        System.arraycopy(e, i, e, i + 2, s - i);
        e[i] = element.real();
        e[i + 1] = element.imag();
        size++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addAll(Collection<? extends Complex> c) {
        final int numNew = c.size();
        expand(numNew);
        double[] realAndImgData = new double[numNew * 2];
        int i = 0;
        for (final Complex val : c) {
            realAndImgData[i++] = val.getReal();
            realAndImgData[i++] = val.getImaginary();
        }
        final int s = size << 1;
        System.arraycopy(realAndImgData, 0, realAndImagParts, s, realAndImgData.length);
        size += numNew;
        return numNew != 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addAll(int index, Collection<? extends Complex> c) {
        rangeCheckForInsert(index);
        final int numNew = c.size();
        final int numNew2 = numNew << 1;
        expand(numNew);
        final double[] realAndImgData = new double[numNew * 2];
        int i = 0;
        for (final Complex val : c) {
            realAndImgData[i++] = val.getReal();
            realAndImgData[i++] = val.getImaginary();
        }
        final int numMoved = (size - index) * 2;
        final int index2 = index << 1;
        System.arraycopy(realAndImagParts, index2, realAndImagParts, index2 + numNew2, numMoved);
        System.arraycopy(realAndImgData, 0, realAndImagParts, index2, realAndImgData.length);
        size += numNew;
        return numNew != 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Complex remove(int index) {
        rangeCheck(index);
        modCount++;
        final int i = index << 1;
        final int s = size << 1;
        final Complex oldValue = Complex.ofCartesian(realAndImagParts[i], realAndImagParts[i + 1]);
        final int numMoved = s - i - 2;
        if (numMoved > 0) {
            System.arraycopy(realAndImagParts, i + 2, realAndImagParts, i, numMoved);
        }
        size--;
        return oldValue;
    }

    /**
     * Constructs an IndexOutOfBoundsException detail message.
     *
     * @param index Index of the element.
     * @return message detailing the exception.
     */
    private String outOfBoundsMsg(int index) {
        return INDEX_MSG + index + SIZE_MSG + size;
    }

    public static ComplexList load(File file) {
        return null;
    }

    public void save(File file) {

    }

    class ComplexCursor implements ComplexDouble, ComplexSink<Void> {

        private int index;

        @Override
        public Void apply(double r, double i) {
            ComplexList.this.realAndImagParts[index * 2] = r;
            ComplexList.this.realAndImagParts[(index * 2) + 1] = i;
            return null;
        }

        @Override
        public double real() {
            return ComplexList.this.realAndImagParts[index * 2];
        }

        @Override
        public double imag() {
            return ComplexList.this.realAndImagParts[(index * 2) + 1];
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void replaceAll(UnaryOperator<Complex> operator) {
        replaceAll((x, y, action) -> {
            final Complex c = operator.apply(Complex.ofCartesian(x, y));
            action.apply(c.real(), c.imag());
            return null;
        });
    }

    private void replaceAll(ComplexUnaryOperator<Void> operator) {
        ComplexCursor cursor = new ComplexCursor();
        while (cursor.index < size) {
            operator.apply(cursor.real(), cursor.imag(), cursor);
            cursor.index++;
        }
    }

    public void replaceAll(double realOperand, double imaginaryOperand, ComplexBinaryOperator<Void> operator) {
        ComplexCursor cursor = new ComplexCursor();
        while (cursor.index < size) {
            operator.apply(cursor.real(), cursor.imag(), realOperand, imaginaryOperand, cursor);
            cursor.index++;
        }
    }

    public void replaceAll(double input, ComplexScalarFunction<Void> operator) {
        ComplexCursor cursor = new ComplexCursor();
        while (cursor.index < size) {
            operator.apply(cursor.real(), cursor.imag(), input, cursor);
            cursor.index++;
        }
    }
}
