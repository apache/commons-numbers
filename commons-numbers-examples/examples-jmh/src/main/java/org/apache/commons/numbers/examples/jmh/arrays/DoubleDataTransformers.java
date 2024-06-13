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
package org.apache.commons.numbers.examples.jmh.arrays;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Support for creating {@link DoubleDataTransformer} implementations.
 *
 * @since 1.2
 */
final class DoubleDataTransformers {

    /** No instances. */
    private DoubleDataTransformers() {}

    /**
     * Creates a factory to supply a {@link DoubleDataTransformer} based on the
     * {@code nanPolicy} and data {@code copy} policy.
     *
     * <p>The factory will supply instances that may be reused on the same thread.
     * Multi-threaded usage should create an instance per thread.
     *
     * @param nanPolicy NaN policy.
     * @param copy Set to {@code true} to use a copy of the data.
     * @return the factory
     */
    static Supplier<DoubleDataTransformer> createFactory(NaNPolicy nanPolicy, boolean copy) {
        if (nanPolicy == NaNPolicy.ERROR) {
            return () -> new NaNErrorTransformer(copy);
        }
        // Support including NaN / excluding NaN from the data size
        final boolean includeNaN = nanPolicy == NaNPolicy.INCLUDE;
        return () -> new SortTransformer(includeNaN, copy);
    }

    /**
     * A transformer that moves {@code NaN} to the upper end of the array.
     * Signed zeros are counted.
     */
    private abstract static class ReplaceSignedZerosTransformer implements DoubleDataTransformer {
        /** Count of negative zeros. */
        protected int negativeZeroCount;

        @Override
        public void postProcess(double[] data, int[] k, int n) {
            // Restore signed zeros
            if (negativeZeroCount != 0) {
                // Use the partitioned indices to fast-forward as much as possible.
                // Assumes partitioning has not changed indices (but
                // reordering is OK).
                int j = -1;
                for (int i = 0; i < n; i++) {
                    if (data[k[i]] < 0) {
                        j = Math.max(j, k[i]);
                    }
                }
                for (int cn = negativeZeroCount;;) {
                    if (data[++j] == 0) {
                        data[j] = -0.0;
                        if (--cn == 0) {
                            break;
                        }
                    }
                }
            }
        }

        @Override
        public void postProcess(double[] data) {
            if (negativeZeroCount != 0) {
                // Find a zero
                int j = Arrays.binarySearch(data, 0.0);
                // Scan back to before zero
                while (--j >= 0) {
                    if (data[j] != 0) {
                        break;
                    }
                }
                // Fix. Assume the zeros are all present so just overwrite
                // the required count of signed zeros.
                for (int cn = negativeZeroCount; --cn >= 0;) {
                    data[++j] = -0.0;
                }
            }
        }
    }

    /**
     * A transformer that moves {@code NaN} to the upper end of the array.
     * Signed zeros are counted.
     */
    private static final class SortTransformer extends ReplaceSignedZerosTransformer {
        /** Set to {@code true} to include NaN in the size of the data. */
        private final boolean includeNaN;
        /** Set to {@code true} to use a copy of the data. */
        private final boolean copy;
        /** Size of the data. */
        private int size;
        /** Length of data to partition. */
        private int len;

        /**
         * @param includeNaN Set to {@code true} to include NaN in the size of the data.
         * @param copy Set to {@code true} to use a copy of the data.
         */
        private SortTransformer(boolean includeNaN, boolean copy) {
            this.includeNaN = includeNaN;
            this.copy = copy;
        }

        @Override
        public double[] preProcess(double[] data) {
            final double[] a = copy ? data.clone() : data;
            // Sort NaN / count signed zeros
            int cn = 0;
            int end = a.length;
            for (int i = end; i > 0;) {
                final double v = a[--i];
                // Count negative zeros using a sign bit check.
                // This requires a performance test. If the conversion to raw bits
                // is natively supported this is faster than using the == check.
                // if (v == 0.0 && Double.doubleToRawLongBits(v) < 0) {
                if (Double.doubleToRawLongBits(v) == Long.MIN_VALUE) {
                    cn++;
                    // Change to positive zero.
                    // Data must be repaired after sort.
                    a[i] = 0.0;
                } else if (v != v) {
                    // Move NaN to end
                    a[i] = a[--end];
                    a[end] = v;
                }
            }
            negativeZeroCount = cn;
            len = end;
            size = includeNaN ? a.length : len;
            return a;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public int length() {
            return len;
        }
    }

    /**
     * A transformer that errors on {@code NaN}.
     * Signed zeros are counted and restored.
     */
    private static final class NaNErrorTransformer extends ReplaceSignedZerosTransformer {
        /** Set to {@code true} to use a copy of the data. */
        private final boolean copy;
        /** Size of the data. */
        private int size;

        /**
         * @param copy Set to {@code true} to use a copy of the data.
         */
        private NaNErrorTransformer(boolean copy) {
            this.copy = copy;
        }

        @Override
        public double[] preProcess(double[] data) {
            // Here we delay copy to not change the data if a NaN is found.
            // But we commit to a double scan for signed zeros.
            double[] a = data;
            // Error on NaN / count signed zeros
            int cn = 0;
            for (int i = a.length; i > 0;) {
                final double v = a[--i];
                // This requires a performance test
                if (Double.doubleToRawLongBits(v) == Long.MIN_VALUE) {
                    cn++;
                } else if (v != v) {
                    throw new IllegalArgumentException("NaN at: " + i);
                }
            }
            negativeZeroCount = cn;
            size = a.length;
            // No NaNs so copy the data if required
            if (copy) {
                a = a.clone();
            }
            // Re-write zeros if required
            if (cn != 0) {
                for (int i = a.length; i > 0;) {
                    if (Double.doubleToRawLongBits(a[--i]) == Long.MIN_VALUE) {
                        a[i] = 0.0;
                        if (--cn == 0) {
                            break;
                        }
                    }
                }
            }
            return a;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public int length() {
            return size;
        }
    }
}
