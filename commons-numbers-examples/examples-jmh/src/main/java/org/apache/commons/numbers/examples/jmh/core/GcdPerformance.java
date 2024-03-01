package org.apache.commons.numbers.examples.jmh.core;

import org.apache.commons.numbers.core.ArithmeticUtils;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx512M"})
public class GcdPerformance {
    private static final int NUM_PAIRS = 1000;
    private static final long SEED = 42;

    @State(Scope.Benchmark)
    public static class Ints {
        final int[] values;


        public Ints() {
            values = getRandomProvider().ints().filter(i -> i != Integer.MIN_VALUE).limit(NUM_PAIRS * 2).toArray();
        }
    }

    @State(Scope.Benchmark)
    public static class Longs {
        final long[] values;


        public Longs() {
            values = getRandomProvider().longs().filter(i -> i != Long.MIN_VALUE).limit(NUM_PAIRS * 2).toArray();
        }
    }

    private static RestorableUniformRandomProvider getRandomProvider() {
        return RandomSource.XO_RO_SHI_RO_128_PP.create(SEED);
    }

    @Benchmark
    public void gcdInt(Ints ints, Blackhole blackhole) {
        for (int i = 0; i < ints.values.length; i += 2) {
            blackhole.consume(ArithmeticUtils.gcd(ints.values[i], ints.values[i + 1]));
        }
    }

    @Benchmark
    public void oldGcdInt(Ints ints, Blackhole blackhole) {
        for (int i = 0; i < ints.values.length; i += 2) {
            blackhole.consume(oldGcdInt(ints.values[i], ints.values[i + 1]));
        }
    }

    @Benchmark
    public void gcdLong(Longs longs, Blackhole blackhole) {
        for (int i = 0; i < longs.values.length; i += 2) {
            blackhole.consume(ArithmeticUtils.gcd(longs.values[i], longs.values[i + 1]));
        }
    }

    @Benchmark
    public void oldGcdLong(Longs longs, Blackhole blackhole) {
        for (int i = 0; i < longs.values.length; i += 2) {
            blackhole.consume(oldGcdLong(longs.values[i], longs.values[i + 1]));
        }
    }

    @Benchmark
    public void oldGcdIntAdaptedForLong(Longs longs, Blackhole blackhole) {
        for (int i = 0; i < longs.values.length; i += 2) {
            blackhole.consume(oldGcdIntAdaptedForLong(longs.values[i], longs.values[i + 1]));
        }
    }

    @Benchmark
    public void gcdBigInteger(Longs longs, Blackhole blackhole) {
        for (int i = 0; i < longs.values.length; i += 2) {
            blackhole.consume(BigInteger.valueOf(longs.values[i]).gcd(BigInteger.valueOf(longs.values[i + 1])).longValue());
        }
    }

    private static long oldGcdIntAdaptedForLong(long p, long q) {
        // Perform the gcd algorithm on negative numbers, so that -2^31 does not
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
            while (a != b) {
                final long delta = a - b;
                b = Math.max(a, b);
                a = delta > 0 ? -delta : delta;

                // Remove any power of 2 in "a" ("b" is guaranteed to be odd).
                a >>= Long.numberOfTrailingZeros(a);
            }

            // Recover the common power of 2.
            negatedGcd = a << shift;
        }
        if (negatedGcd == Long.MIN_VALUE) {
            throw new ArithmeticException();
        }

        return -negatedGcd;
    }

    public static long oldGcdLong(final long p, final long q) {
        long u = p;
        long v = q;
        if (u == 0 || v == 0) {
            if (u == Long.MIN_VALUE || v == Long.MIN_VALUE) {
                throw new ArithmeticException();
            }
            return Math.abs(u) + Math.abs(v);
        }
        // keep u and v negative, as negative integers range down to
        // -2^63, while positive numbers can only be as large as 2^63-1
        // (i.e. we can't necessarily negate a negative number without
        // overflow)
        /* assert u!=0 && v!=0; */
        if (u > 0) {
            u = -u;
        } // make u negative
        if (v > 0) {
            v = -v;
        } // make v negative
        // B1. [Find power of 2]
        int k = 0;
        while ((u & 1) == 0 && (v & 1) == 0 && k < 63) { // while u and v are
            // both even...
            u /= 2;
            v /= 2;
            k++; // cast out twos.
        }
        if (k == 63) {
            throw new ArithmeticException();
        }
        // B2. Initialize: u and v have been divided by 2^k and at least
        // one is odd.
        long t = ((u & 1) == 1) ? v : -(u / 2)/* B3 */;
        // t negative: u was odd, v may be even (t replaces v)
        // t positive: u was even, v is odd (t replaces u)
        do {
            /* assert u<0 && v<0; */
            // B4/B3: cast out twos from t.
            while ((t & 1) == 0) { // while t is even..
                t /= 2; // cast out twos
            }
            // B5 [reset max(u,v)]
            if (t > 0) {
                u = -t;
            } else {
                v = t;
            }
            // B6/B3. at this point both u and v should be odd.
            t = (v - u) / 2;
            // |u| larger: t positive (replace u)
            // |v| larger: t negative (replace v)
        } while (t != 0);
        return -u * (1L << k); // gcd is u*2^k
    }

    public static int oldGcdInt(int p, int q) {
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
            throw new ArithmeticException();
        }
        return -negatedGcd;
    }
}
