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
package org.apache.commons.numbers.primes;

import java.text.MessageFormat;
import java.util.List;
import org.apache.commons.numbers.core.ArithmeticUtils;

/**
 * Methods related to prime numbers in the range of <code>int</code>.
 * <ul>
 * <li>primality test</li>
 * <li>prime number generation</li>
 * <li>factorization</li>
 * </ul>
 */
public final class Primes {
    /** Exception message format when an argument is too small. */
    static final String NUMBER_TOO_SMALL = "{0} is smaller than the minimum ({1})";

    /**
     * Utility class.
     */
    private Primes() {}

    /**
     * Primality test: tells if the argument is a (provable) prime or not.
     * <p>
     * It uses the Miller-Rabin probabilistic test in such a way that a result is guaranteed:
     * it uses the firsts prime numbers as successive base (see Handbook of applied cryptography
     * by Menezes, table 4.1).
     *
     * @param n Number to test.
     * @return true if {@code n} is prime. All numbers &lt; 2 return false.
     */
    public static boolean isPrime(int n) {
        if (n < 2) {
            return false;
        }

        for (final int p : SmallPrimes.PRIMES) {
            if (0 == (n % p)) {
                return n == p;
            }
        }
        return SmallPrimes.millerRabinPrimeTest(n);
    }

    /**
     * Return the smallest prime greater than or equal to n.
     *
     * @param n Positive number.
     * @return the smallest prime greater than or equal to {@code n}.
     * @throws IllegalArgumentException if n &lt; 0.
     */
    public static int nextPrime(int n) {
        if (n < 0) {
            throw new IllegalArgumentException(MessageFormat.format(NUMBER_TOO_SMALL, n, 0));
        }
        if (n <= 2) {
            return 2;
        }
        n |= 1; // make sure n is odd

        if (isPrime(n)) {
            return n;
        }

        // prepare entry in the +2, +4 loop:
        // n should not be a multiple of 3
        final int rem = n % 3;
        if (0 == rem) { // if n % 3 == 0
            n += 2; // n % 3 == 2
        } else if (1 == rem) { // if n % 3 == 1
            n += 4; // n % 3 == 2
        }
        while (true) { // this loop skips all multiple of 3
            if (isPrime(n)) {
                return n;
            }
            n += 2; // n % 3 == 1
            if (isPrime(n)) {
                return n;
            }
            n += 4; // n % 3 == 2
        }
    }

    /**
     * Prime factors decomposition.
     *
     * @param n Number to factorize: must be &ge; 2.
     * @return the list of prime factors of {@code n}.
     * @throws IllegalArgumentException if n &lt; 2.
     */
    public static List<Integer> primeFactors(int n) {
        if (n < 2) {
            throw new IllegalArgumentException(MessageFormat.format(NUMBER_TOO_SMALL, n, 2));
        }
        return SmallPrimes.trialDivision(n);
    }

    /**
     * Checks if the absolute value of the 2 given integers are relative primes.
     * @param x first integer to check.
     * @param y second integer to check.
     * @return true/false i.e. if the arguments are relative primes.
     */
    public static boolean areRelativePrimes(final int x, final int y) {
        return SmallPrimes.areRelativePrimes(x, y);
    }

    /**
     * Checks if the absolute value of the 2 given integers are relative primes.
     * @param x first integer to check.
     * @param y second integer to check.
     * @return true/false i.e. if the arguments are relative primes.
     */
    public static boolean areRelativePrimes(final int x, final long y) {
        return SmallPrimes.areRelativePrimes(x, y);
    }

    /**
     * Checks if the absolute value of the 2 given integers are relative primes.
     * @param x first integer to check.
     * @param y second integer to check.
     * @return true/false i.e. if the arguments are relative primes.
     */
    public static boolean areRelativePrimes(final long x, final int y) {
        return SmallPrimes.areRelativePrimes(x, y);
    }

    /**
     * Checks if the absolute value of the 2 given integers are relative primes.
     * @param x first integer to check.
     * @param y second integer to check.
     * @return true/false i.e. if the arguments are relative primes.
     */
    public static boolean areRelativePrimes(final long x, final long y) {
        return SmallPrimes.areRelativePrimes(x, y);
    }
}
