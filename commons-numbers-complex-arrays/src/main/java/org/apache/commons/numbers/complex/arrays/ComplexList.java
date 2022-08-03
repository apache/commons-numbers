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

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Resizable-double array implementation of the List interface. Implements all optional list operations,
 * and permits all elements. In addition to implementing the List interface,
 * this class provides methods to manipulate the size of the array that is used internally to store the list.
 *
 * <p>Each ComplexList instance has a capacity. The capacity is half the size of the double array used to store the elements
 * in the list. It is always at least twice as large as the list size. As elements are added to an ComplexList,
 * its capacity grows automatically.</p>
 *
 * <p>An application can increase the capacity of an ComplexList instance before adding a large number of elements
 * using the ensureCapacity operation. This may reduce the amount of incremental reallocation.</p>
 */
public class ComplexList extends AbstractList<Complex> {

    /** The maximum size of array to allocate.
     * Ensuring Max capacity is even with additional space for vm array headers.
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 9;

    /** Max capacity for size of complex numbers in the list. */
    public static final int MAX_CAPACITY = MAX_ARRAY_SIZE / 2;

    /** error in case of allocation above max capacity. */
    private static final String OOM_ERROR_STRING = "cannot allocate capacity %s greater than max " + MAX_CAPACITY;

    /** Default initial capacity. */
    private static final int DEFAULT_CAPACITY = 8;

    /** Size label message. */
    private static final String SIZE_MSG = ", Size: ";
    /** Index position label message. */
    private static final String INDEX_MSG = "Index: ";

    /**
     * The double array buffer into which the elements of the ComplexList are stored.
     */
    protected double[] realAndImagParts;

    /**
     * Size of ComplexList.
     */
    private int size;

    /**
     * Constructs an empty list with the specified capacity, if it's
     * greater than the default capacity of 8.
     *
     * @param capacity - Capacity of list.
     * @throws OutOfMemoryError - if the {@code capacity} is greater than {@code MAX_CAPACITY}.
     */
    public ComplexList(int capacity) {
        if (capacity > MAX_CAPACITY) {
            throw new OutOfMemoryError(String.format(OOM_ERROR_STRING, capacity));
        }
        final int arrayLength = Math.max(DEFAULT_CAPACITY, capacity) * 2;
        realAndImagParts = new double[arrayLength];
    }

    /**
     * Constructs an empty list with the default capacity of 8.
     */
    public ComplexList() {
        realAndImagParts = new double[DEFAULT_CAPACITY * 2];
    }

    /**
     * Gives the size of this ComplexList.
     *
     * @return the number of elements it contains.
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Checks if the given index is in range.
     *
     * @param index - Index of the element to range check.
     * @throws IndexOutOfBoundsException - if index isn't within the range.
     */
    private void rangeCheck(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        }
    }

    /**
     * A version of rangeCheck used by add and addAll.
     *
     * @param index - Index of the element to range check.
     * @throws IndexOutOfBoundsException - if index isn't within the range of list.
     */
    private void rangeCheckForInsert(int index) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        }
    }

    /**
     * Gets the complex number \( (a + i b) \) at the indexed position of the list.
     *
     * @param index - Index of the element to get.
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
     * @param index - Index of the element to replace.
     * @param real - Real part \( a \) of the complex number \( (a +ib) \).
     * @param imaginary - Imaginary part \( b \) of the complex number \( (a +ib) \).
     */
    private void setNoRangeCheck(int index, double real, double imaginary) {
        final int i = index << 1;
        realAndImagParts[i] = real;
        realAndImagParts[i + 1] = imaginary;
    }

    /**
     * Replaces the element at the specified position in this list with the specified element.

     * @param index - Index of the element to replace.
     * @param element - Element to be stored at the specified position.
     * @return the element previously at the specified position.
     * @throws IndexOutOfBoundsException - if index isn't within the range of list.
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
     * @param minCapacity – Desired minimum capacity.
     * @throws OutOfMemoryError - if the {@code minCapacity} is greater than {@code MAX_ARRAY_SIZE}.
     */
    public void ensureCapacity(int minCapacity) {
        ensureCapacityInternal(minCapacity);
    }

    /**
     * Increases the capacity of this ComplexList instance, if necessary, to ensure that it can hold at
     * least the number of elements specified by the minimum capacity argument.
     *
     * @param minCapacity – Desired minimum capacity.
     * @return the backing double array.
     * @throws OutOfMemoryError - if the {@code minCapacity} is greater than {@code MAX_ARRAY_SIZE}.
     */
    private double[] ensureCapacityInternal(int minCapacity) {
        modCount++;
        final long minArrayCapacity = minCapacity * 2L;
        if (minArrayCapacity > MAX_ARRAY_SIZE) {
            throw new OutOfMemoryError(String.format(OOM_ERROR_STRING, minArrayCapacity));
        }
        final long oldArrayCapacity = realAndImagParts.length;
        if (minArrayCapacity > oldArrayCapacity) {
            long newArrayCapacity = oldArrayCapacity + (oldArrayCapacity >> 1);
            if (newArrayCapacity % 2 != 0) {
                ++newArrayCapacity;
            }
            if (newArrayCapacity > MAX_ARRAY_SIZE) {
                newArrayCapacity = MAX_ARRAY_SIZE;
            } else if (newArrayCapacity < minArrayCapacity) {
                newArrayCapacity = minArrayCapacity;
            }
            realAndImagParts = Arrays.copyOf(realAndImagParts, (int)newArrayCapacity);
        }
        return realAndImagParts;
    }

    /**
     * Increases the capacity of this ComplexList instance, if necessary, to ensure that it can hold at
     * least an additional number of elements specified by the capacity argument.
     *
     * @param capacity - Desired capacity.
     */
    private void expand(int capacity) {
        ensureCapacityInternal(size + capacity);
    }

    /**
     * Appends the specified complex element to the end of this list.
     *
     * @param element - Complex element to be appended to this list.
     * @return true after element has been added and size has been updated.
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
     * Inserts the specified element at the specified position in this list. Shifts the element currently at that
     * position (if any) and any subsequent elements to the right (adds one to their indices).
     *
     * @param index – Index at which the specified element is to be inserted.
     * @param element – Complex element to be inserted.
     * @throws IndexOutOfBoundsException – if index isn't within the range of list.
     */
    @Override
    public void add(int index, Complex element) {
        rangeCheckForInsert(index);
        double[] e = realAndImagParts;
        if (size == e.length >>> 1) {
            e = ensureCapacityInternal(size + 1);
        }
        final int i = index << 1;
        System.arraycopy(e, 2 * index, e, i + 2, (size * 2) - i);
        e[i] = element.real();
        e[i + 1] = element.imag();
        size++;
    }

    /**
     * Appends all the elements in the specified collection to the end of this list, in the order that they are
     * returned by the specified collection's Iterator. The behavior of this operation is undefined if the
     * specified collection is modified while the operation is in progress.
     * (This implies that the behavior of this call is undefined if the specified collection is this list,
     * and this list is nonempty.)
     *
     * @param c – Collection containing elements to be added to this list.
     * @return true if this list changed as a result of the call.
     * @throws NullPointerException – if the specified collection is null.
     */
    @Override
    public boolean addAll(Collection<? extends Complex> c) {
        final int numNew = c.size();
        expand(numNew * 2);
        double[] realAndImgData = new double[c.size() * 2];
        int i = 0;
        for (final Complex val : c) {
            final int i2 = i << 1;
            realAndImgData[i2] = val.getReal();
            realAndImgData[i2 + 1] = val.getImaginary();
            i++;
        }
        System.arraycopy(realAndImgData, 0, realAndImagParts, size * 2, realAndImgData.length);
        size += numNew;
        return numNew != 0;
    }

    /**
     * Inserts all the elements in the specified collection into this list, starting at the specified position.
     * Shifts the element currently at that position (if any) and any subsequent elements to the right (increases their indices).
     * The new elements will appear in the list in the order that they are returned by the specified collection's iterator.
     *
     * @param index – Index at which to insert the first element from the specified collection.
     * @param c – Collection containing elements to be added to this list.
     * @return true if this list changed as a result of the call.
     * @throws IndexOutOfBoundsException – if index isn't within the range of list.
     * @throws NullPointerException – if the specified collection is null.
     */
    @Override
    public boolean addAll(int index, Collection<? extends Complex> c) {
        rangeCheckForInsert(index);
        final int numNew = c.size();
        final int numNew2 = numNew << 1;
        expand(numNew * 2);
        final double[] realAndImgData = new double[c.size() * 2];
        int i = 0;
        for (final Complex val : c) {
            final int i2 = i << 1;
            realAndImgData[i2] = val.getReal();
            realAndImgData[i2 + 1] = val.getImaginary();
            i++;
        }
        final int numMoved = (size - index) * 2;
        final int index2 = index << 1;
        System.arraycopy(realAndImagParts, index2, realAndImagParts, index2 + numNew2, numMoved);
        System.arraycopy(realAndImgData, 0, realAndImagParts, index2, realAndImgData.length);
        size += numNew;
        return numNew != 0;
    }

    /**
     * Removes the element at the specified position in this list.
     * Shifts any subsequent elements to the left (subtracts one from their indices).
     *
     * @param index – Index of the element to be removed.
     * @return the element that was removed from the list.
     * @throws IndexOutOfBoundsException – if index isn't within the range of list.
     */
    @Override
    public Complex remove(int index) {
        rangeCheck(index);
        final int i = index << 1;
        final int s = size << 1;
        final Complex oldValue = Complex.ofCartesian(realAndImagParts[i], realAndImagParts[i + 1]);
        final int numMoved = s - i - 2;
        if (numMoved > 0) {
            System.arraycopy(realAndImagParts, i + 2, realAndImagParts, i, numMoved);
        }
        size--;
        realAndImagParts[s] = 0;
        realAndImagParts[s + 1] = 0;

        return oldValue;
    }

    /**
     * Constructs an IndexOutOfBoundsException detail message.
     *
     * @param index – Index of the element.
     * @return message detailing the exception.
     */
    private String outOfBoundsMsg(int index) {
        return INDEX_MSG + index + SIZE_MSG + size;
    }

}
