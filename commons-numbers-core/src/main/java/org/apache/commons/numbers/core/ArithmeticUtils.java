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
package org.apache.commons.numbers.core;

import java.math.BigInteger;

/**
 * Some useful, arithmetics related, additions to the built-in functions in
 * {@link Math}.
 *
 */
public final class ArithmeticUtils {

    /** Negative exponent exception message part 1. */
    private static final String NEGATIVE_EXPONENT_1 = "negative exponent ({";
    /** Negative exponent exception message part 2. */
    private static final String NEGATIVE_EXPONENT_2 = "})";

    /** Private constructor. */
    private ArithmeticUtils() {
        // intentionally empty.
    }

    /**
     * Computes the greatest common divisor of the absolute value of two
     * numbers, using a modified version of the "binary gcd" method.
     * See Knuth 4.5.2 algorithm B.
     * The algorithm is due to Josef Stein (1961).
     * <br>
     * Special cases:
     * <ul>
     *  <li>The invocations
     *   {@code gcd(Integer.MIN_VALUE, Integer.MIN_VALUE)},
     *   {@code gcd(Integer.MIN_VALUE, 0)} and
     *   {@code gcd(0, Integer.MIN_VALUE)} throw an
     *   {@code ArithmeticException}, because the result would be 2^31, which
     *   is too large for an int value.</li>
     *  <li>The result of {@code gcd(x, x)}, {@code gcd(0, x)} and
     *   {@code gcd(x, 0)} is the absolute value of {@code x}, except
     *   for the special cases above.</li>
     *  <li>The invocation {@code gcd(0, 0)} is the only one which returns
     *   {@code 0}.</li>
     * </ul>
     *
     * <p>Two numbers are relatively prime, or coprime, if their gcd is 1.</p>
     *
     * @param p Number.
     * @param q Number.
     * @return the greatest common divisor (never negative).
     * @throws ArithmeticException if the result cannot be represented as
     * a non-negative {@code int} value.
     */
    public static int gcd(int p, int q) {
        // Perform the gcd algorithm on negative numbers, so that -2^31 does not
        // need to be handled separately
        int a = p > 0 ? -p : p;
        int b = q > 0 ? -q : q;

        int negatedGcd;
        if (a == 0) {
            negatedGcd = b;
        } else if (b == 0) {
            negatedGcd = a;
        } else {
            // Make "a" and "b" odd, keeping track of common power of 2.
            final int aTwos = Integer.numberOfTrailingZeros(a);
            final int bTwos = Integer.numberOfTrailingZeros(b);
            a >>= aTwos;
            b >>= bTwos;
            final int shift = Math.min(aTwos, bTwos);

            // "a" and "b" are negative and odd.
            // If a < b then "gdc(a, b)" is equal to "gcd(a - b, b)".
            // If a > b then "gcd(a, b)" is equal to "gcd(b - a, a)".
            // Hence, in the successive iterations:
            //  "a" becomes the negative absolute difference of the current values,
            //  "b" becomes that value of the two that is closer to zero.
            while (a != b) {
                final int delta = a - b;
                b = Math.max(a, b);
                a = delta > 0 ? -delta : delta;

                // Remove any power of 2 in "a" ("b" is guaranteed to be odd).
                a >>= Integer.numberOfTrailingZeros(a);
            }

            // Recover the common power of 2.
            negatedGcd = a << shift;
        }
        if (negatedGcd == Integer.MIN_VALUE) {
            throw new NumbersArithmeticException("overflow: gcd(%d, %d) is 2^31",
                                                 p, q);
        }
        return -negatedGcd;
    }

    /**
     * <p>
     * Gets the greatest common divisor of the absolute value of two numbers,
     * using the "binary gcd" method which avoids division and modulo
     * operations. See Knuth 4.5.2 algorithm B. This algorithm is due to Josef
     * Stein (1961).
     * </p>
     * Special cases:
     * <ul>
     * <li>The invocations
     * {@code gcd(Long.MIN_VALUE, Long.MIN_VALUE)},
     * {@code gcd(Long.MIN_VALUE, 0L)} and
     * {@code gcd(0L, Long.MIN_VALUE)} throw an
     * {@code ArithmeticException}, because the result would be 2^63, which
     * is too large for a long value.</li>
     * <li>The result of {@code gcd(x, x)}, {@code gcd(0L, x)} and
     * {@code gcd(x, 0L)} is the absolute value of {@code x}, except
     * for the special cases above.
     * <li>The invocation {@code gcd(0L, 0L)} is the only one which returns
     * {@code 0L}.</li>
     * </ul>
     *
     * <p>Two numbers are relatively prime, or coprime, if their gcd is 1.</p>
     *
     * @param p Number.
     * @param q Number.
     * @return the greatest common divisor, never negative.
     * @throws ArithmeticException if the result cannot be represented as
     * a non-negative {@code long} value.
     */
    public static long gcd(long p, long q) {
        // Perform the gcd algorithm on negative numbers, so that -2^63 does not
        // need to be handled separately
        long a = p > 0 ? -p : p;
        long b = q > 0 ? -q : q;

        long negatedGcd;
        if (a == 0) {
            negatedGcd = b;
        } else if (b == 0) {
            negatedGcd = a;
        } else {
            // Make "a" and "b" odd, keeping track of common power of 2.
            final int aTwos = Long.numberOfTrailingZeros(a);
            final int bTwos = Long.numberOfTrailingZeros(b);
            a >>= aTwos;
            b >>= bTwos;
            final int shift = Math.min(aTwos, bTwos);

            // "a" and "b" are negative and odd.
            // If a < b then "gdc(a, b)" is equal to "gcd(a - b, b)".
            // If a > b then "gcd(a, b)" is equal to "gcd(b - a, a)".
            // Hence, in the successive iterations:
            //  "a" becomes the negative absolute difference of the current values,
            //  "b" becomes that value of the two that is closer to zero.
            while (true) {
                final long delta = a - b;

                if (delta == 0) {
                    // This way of terminating the loop is intentionally different from the int gcd implementation.
                    // Benchmarking shows that testing for long inequality (a != b) is slow compared to
                    // testing the delta against zero. The same change on the int gcd reduces performance there,
                    // hence we have two variants of this loop.
                    break;
                }

                b = Math.max(a, b);
                a = delta > 0 ? -delta : delta;

                // Remove any power of 2 in "a" ("b" is guaranteed to be odd).
                a >>= Long.numberOfTrailingZeros(a);
            }

            // Recover the common power of 2.
            negatedGcd = a << shift;
        }
        if (negatedGcd == Long.MIN_VALUE) {
            throw new NumbersArithmeticException("overflow: gcd(%d, %d) is 2^63",
                    p, q);
        }
        return -negatedGcd;
    }

    /**
     * <p>
     * Returns the least common multiple of the absolute value of two numbers,
     * using the formula {@code lcm(a,b) = (a / gcd(a,b)) * b}.
     * </p>
     * Special cases:
     * <ul>
     * <li>The invocations {@code lcm(Integer.MIN_VALUE, n)} and
     * {@code lcm(n, Integer.MIN_VALUE)}, where {@code abs(n)} is a
     * power of 2, throw an {@code ArithmeticException}, because the result
     * would be 2^31, which is too large for an int value.</li>
     * <li>The result of {@code lcm(0, x)} and {@code lcm(x, 0)} is
     * {@code 0} for any {@code x}.
     * </ul>
     *
     * @param a Number.
     * @param b Number.
     * @return the least common multiple, never negative.
     * @throws ArithmeticException if the result cannot be represented as
     * a non-negative {@code int} value.
     */
    public static int lcm(int a, int b) {
        if (a == 0 || b == 0) {
            return 0;
        }
        final int lcm = Math.abs(Math.multiplyExact(a / gcd(a, b), b));
        if (lcm == Integer.MIN_VALUE) {
            throw new NumbersArithmeticException("overflow: lcm(%d, %d) is 2^31",
                                                 a, b);
        }
        return lcm;
    }

    /**
     * <p>
     * Returns the least common multiple of the absolute value of two numbers,
     * using the formula {@code lcm(a,b) = (a / gcd(a,b)) * b}.
     * </p>
     * Special cases:
     * <ul>
     * <li>The invocations {@code lcm(Long.MIN_VALUE, n)} and
     * {@code lcm(n, Long.MIN_VALUE)}, where {@code abs(n)} is a
     * power of 2, throw an {@code ArithmeticException}, because the result
     * would be 2^63, which is too large for an int value.</li>
     * <li>The result of {@code lcm(0L, x)} and {@code lcm(x, 0L)} is
     * {@code 0L} for any {@code x}.
     * </ul>
     *
     * @param a Number.
     * @param b Number.
     * @return the least common multiple, never negative.
     * @throws ArithmeticException if the result cannot be represented
     * as a non-negative {@code long} value.
     */
    public static long lcm(long a, long b) {
        if (a == 0 || b == 0) {
            return 0;
        }
        final long lcm = Math.abs(Math.multiplyExact(a / gcd(a, b), b));
        if (lcm == Long.MIN_VALUE) {
            throw new NumbersArithmeticException("overflow: lcm(%d, %d) is 2^63",
                                                 a, b);
        }
        return lcm;
    }

    /**
     * Raise an int to an int power.
     *
     * <p>Special cases:</p>
     * <ul>
     *   <li>{@code k^0} returns {@code 1} (including {@code k=0})
     *   <li>{@code k^1} returns {@code k} (including {@code k=0})
     *   <li>{@code 0^0} returns {@code 1}
     *   <li>{@code 0^e} returns {@code 0}
     *   <li>{@code 1^e} returns {@code 1}
     *   <li>{@code (-1)^e} returns {@code -1 or 1} if {@code e} is odd or even
     * </ul>
     *
     * @param k Number to raise.
     * @param e Exponent (must be positive or zero).
     * @return \( k^e \)
     * @throws IllegalArgumentException if {@code e < 0}.
     * @throws ArithmeticException if the result would overflow.
     */
    public static int pow(final int k,
                          final int e) {
        if (e < 0) {
            throw new IllegalArgumentException(NEGATIVE_EXPONENT_1 + e + NEGATIVE_EXPONENT_2);
        }

        if (k == 0) {
            return e == 0 ? 1 : 0;
        }

        if (k == 1) {
            return 1;
        }

        if (k == -1) {
            return (e & 1) == 0 ? 1 : -1;
        }

        if (e >= 31) {
            throw new ArithmeticException("integer overflow");
        }

        int exp = e;
        int result = 1;
        int k2p    = k;
        while (true) {
            if ((exp & 0x1) != 0) {
                result = Math.multiplyExact(result, k2p);
            }

            exp >>= 1;
            if (exp == 0) {
                break;
            }

            k2p = Math.multiplyExact(k2p, k2p);
        }

        return result;
    }

    /**
     * Raise a long to an int power.
     *
     * <p>Special cases:</p>
     * <ul>
     *   <li>{@code k^0} returns {@code 1} (including {@code k=0})
     *   <li>{@code k^1} returns {@code k} (including {@code k=0})
     *   <li>{@code 0^0} returns {@code 1}
     *   <li>{@code 0^e} returns {@code 0}
     *   <li>{@code 1^e} returns {@code 1}
     *   <li>{@code (-1)^e} returns {@code -1 or 1} if {@code e} is odd or even
     * </ul>
     *
     * @param k Number to raise.
     * @param e Exponent (must be positive or zero).
     * @return \( k^e \)
     * @throws IllegalArgumentException if {@code e < 0}.
     * @throws ArithmeticException if the result would overflow.
     */
    public static long pow(final long k,
                           final int e) {
        if (e < 0) {
            throw new IllegalArgumentException(NEGATIVE_EXPONENT_1 + e + NEGATIVE_EXPONENT_2);
        }

        if (k == 0L) {
            return e == 0 ? 1L : 0L;
        }

        if (k == 1L) {
            return 1L;
        }

        if (k == -1L) {
            return (e & 1) == 0 ? 1L : -1L;
        }

        if (e >= 63) {
            throw new ArithmeticException("long overflow");
        }

        int exp = e;
        long result = 1;
        long k2p    = k;
        while (true) {
            if ((exp & 0x1) != 0) {
                result = Math.multiplyExact(result, k2p);
            }

            exp >>= 1;
            if (exp == 0) {
                break;
            }

            k2p = Math.multiplyExact(k2p, k2p);
        }

        return result;
    }

    /**
     * Raise a BigInteger to an int power.
     *
     * @param k Number to raise.
     * @param e Exponent (must be positive or zero).
     * @return k<sup>e</sup>
     * @throws IllegalArgumentException if {@code e < 0}.
     */
    public static BigInteger pow(final BigInteger k, int e) {
        if (e < 0) {
            throw new IllegalArgumentException(NEGATIVE_EXPONENT_1 + e + NEGATIVE_EXPONENT_2);
        }

        return k.pow(e);
    }

    /**
     * Raise a BigInteger to a long power.
     *
     * @param k Number to raise.
     * @param e Exponent (must be positive or zero).
     * @return k<sup>e</sup>
     * @throws IllegalArgumentException if {@code e < 0}.
     */
    public static BigInteger pow(final BigInteger k, final long e) {
        if (e < 0) {
            throw new IllegalArgumentException(NEGATIVE_EXPONENT_1 + e + NEGATIVE_EXPONENT_2);
        }

        long exp = e;
        BigInteger result = BigInteger.ONE;
        BigInteger k2p    = k;
        while (exp != 0) {
            if ((exp & 0x1) != 0) {
                result = result.multiply(k2p);
            }
            k2p = k2p.multiply(k2p);
            exp >>= 1;
        }

        return result;
    }

    /**
     * Raise a BigInteger to a BigInteger power.
     *
     * @param k Number to raise.
     * @param e Exponent (must be positive or zero).
     * @return k<sup>e</sup>
     * @throws IllegalArgumentException if {@code e < 0}.
     */
    public static BigInteger pow(final BigInteger k, final BigInteger e) {
        if (e.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException(NEGATIVE_EXPONENT_1 + e + NEGATIVE_EXPONENT_2);
        }

        BigInteger exp = e;
        BigInteger result = BigInteger.ONE;
        BigInteger k2p    = k;
        while (!BigInteger.ZERO.equals(exp)) {
            if (exp.testBit(0)) {
                result = result.multiply(k2p);
            }
            k2p = k2p.multiply(k2p);
            exp = exp.shiftRight(1);
        }

        return result;
    }

    /**
     * Returns true if the argument is a power of two.
     *
     * @param n the number to test
     * @return true if the argument is a power of two
     */
    public static boolean isPowerOfTwo(long n) {
        return n > 0 && (n & (n - 1)) == 0;
    }

    /**
     * Returns the unsigned remainder from dividing the first argument
     * by the second where each argument and the result is interpreted
     * as an unsigned value.
     *
     * <p>Implementation note
     *
     * <p>In v1.0 this method did not use the {@code long} datatype.
     * Modern 64-bit processors make use of the {@code long} datatype
     * faster than an algorithm using the {@code int} datatype. This method
     * now delegates to {@link Integer#remainderUnsigned(int, int)}
     * which uses {@code long} arithmetic; or from JDK 19 an intrinsic method.
     *
     * @param dividend the value to be divided
     * @param divisor the value doing the dividing
     * @return the unsigned remainder of the first argument divided by
     * the second argument.
     * @see Integer#remainderUnsigned(int, int)
     */
    public static int remainderUnsigned(int dividend, int divisor) {
        return Integer.remainderUnsigned(dividend, divisor);
    }

    /**
     * Returns the unsigned remainder from dividing the first argument
     * by the second where each argument and the result is interpreted
     * as an unsigned value.
     *
     * <p>Implementation note
     *
     * <p>This method does not use the {@code BigInteger} datatype.
     * The JDK implementation of {@link Long#remainderUnsigned(long, long)}
     * uses {@code BigInteger} prior to JDK 17 and this method is 15-25x faster.
     * From JDK 17 onwards the JDK implementation is as fast; or from JDK 19
     * even faster due to use of an intrinsic method.
     *
     * @param dividend the value to be divided
     * @param divisor the value doing the dividing
     * @return the unsigned remainder of the first argument divided by
     * the second argument.
     * @see Long#remainderUnsigned(long, long)
     */
    public static long remainderUnsigned(long dividend, long divisor) {
        // Adapts the divideUnsigned method to compute the remainder.
        if (divisor < 0) {
            // Using unsigned compare:
            // if dividend < divisor: return dividend
            // else: return dividend - divisor

            // Subtracting divisor using masking is more complex in this case
            // and we use a condition
            return dividend >= 0 || dividend < divisor ? dividend : dividend - divisor;
        }
        // From Hacker's Delight 2.0, section 9.3
        final long q = ((dividend >>> 1) / divisor) << 1;
        final long r = dividend - q * divisor;
        // unsigned r: 0 <= r < 2 * divisor
        // if (r < divisor): r
        // else: r - divisor

        // The compare of unsigned r can be done using:
        // return (r + Long.MIN_VALUE) < (divisor | Long.MIN_VALUE) ? r : r - divisor

        // Here we subtract divisor if (r - divisor) is positive, else the result is r.
        // This can be done by flipping the sign bit and
        // creating a mask as -1 or 0 by signed shift.
        return r - (divisor & (~(r - divisor) >> 63));
    }

    /**
     * Returns the unsigned quotient of dividing the first argument by
     * the second where each argument and the result is interpreted as
     * an unsigned value.
     * <p>Note that in two's complement arithmetic, the three other
     * basic arithmetic operations of add, subtract, and multiply are
     * bit-wise identical if the two operands are regarded as both
     * being signed or both being unsigned. Therefore separate {@code
     * addUnsigned}, etc. methods are not provided.</p>
     *
     * <p>Implementation note
     *
     * <p>In v1.0 this method did not use the {@code long} datatype.
     * Modern 64-bit processors make use of the {@code long} datatype
     * faster than an algorithm using the {@code int} datatype. This method
     * now delegates to {@link Integer#divideUnsigned(int, int)}
     * which uses {@code long} arithmetic; or from JDK 19 an intrinsic method.
     *
     * @param dividend the value to be divided
     * @param divisor the value doing the dividing
     * @return the unsigned quotient of the first argument divided by
     * the second argument
     * @see Integer#divideUnsigned(int, int)
     */
    public static int divideUnsigned(int dividend, int divisor) {
        return Integer.divideUnsigned(dividend, divisor);
    }

    /**
     * Returns the unsigned quotient of dividing the first argument by
     * the second where each argument and the result is interpreted as
     * an unsigned value.
     * <p>Note that in two's complement arithmetic, the three other
     * basic arithmetic operations of add, subtract, and multiply are
     * bit-wise identical if the two operands are regarded as both
     * being signed or both being unsigned. Therefore separate {@code
     * addUnsigned}, etc. methods are not provided.</p>
     *
     * <p>Implementation note
     *
     * <p>This method does not use the {@code BigInteger} datatype.
     * The JDK implementation of {@link Long#divideUnsigned(long, long)}
     * uses {@code BigInteger} prior to JDK 17 and this method is 15-25x faster.
     * From JDK 17 onwards the JDK implementation is as fast; or from JDK 19
     * even faster due to use of an intrinsic method.
     *
     * @param dividend the value to be divided
     * @param divisor the value doing the dividing
     * @return the unsigned quotient of the first argument divided by
     * the second argument.
     * @see Long#divideUnsigned(long, long)
     */
    public static long divideUnsigned(long dividend, long divisor) {
        // The implementation is a Java port of algorithm described in the book
        // "Hacker's Delight 2.0" (section 9.3 "Unsigned short division from signed division").
        // Adapts 6-line predicate expressions program with (u >=) an unsigned compare
        // using the provided branchless variants.
        if (divisor < 0) {
            // line 1 branchless:
            // q <- (dividend (u >=) divisor)
            return (dividend & ~(dividend - divisor)) >>> 63;
        }
        final long q = ((dividend >>> 1) / divisor) << 1;
        final long r = dividend - q * divisor;
        // line 5 branchless:
        // q <- q + (r (u >=) divisor)
        return q + ((r | ~(r - divisor)) >>> 63);
    }

    /**
     * Exception.
     */
    private static class NumbersArithmeticException extends ArithmeticException {
        /** Serializable version Id. */
        private static final long serialVersionUID = 20180130L;

        /**
         * Create an exception where the message is constructed by applying
         * {@link String#format(String, Object...)}.
         *
         * @param message Exception message format string
         * @param args Arguments for formatting the message
         */
        NumbersArithmeticException(String message, Object... args) {
            super(String.format(message, args));
        }
    }
}
