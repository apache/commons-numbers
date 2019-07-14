package org.apache.commons.numbers.fraction;

import java.math.BigInteger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SimpleContinuedFractionTest {

    private static void assertConvergent(long expectedNumerator, long expectedDenominator, BigInteger[] actual) {
        Assertions.assertArrayEquals(
                toBigIntegerArray(expectedNumerator, expectedDenominator),
                actual
        );
    }

    private static BigInteger[] toBigIntegerArray(long... arr) {
        BigInteger[] result = new BigInteger[arr.length];
        for (int i = 0; i < arr.length; i++) {
            result[i] = BigInteger.valueOf(arr[i]);
        }
        return result;
    }

    @Test
    void testAddCoefficient() {
        final SimpleContinuedFraction testSubject = new SimpleContinuedFraction();

        testSubject.addCoefficient(BigInteger.ONE);
        assertConvergent(1, 1, testSubject.getCurrentConvergent());
        assertConvergent(1, 0, testSubject.getPreviousConvergent());

        testSubject.addCoefficient(BigInteger.valueOf(2));
        assertConvergent(3, 2, testSubject.getCurrentConvergent());
        assertConvergent(1, 1, testSubject.getPreviousConvergent());

        testSubject.addCoefficient(BigInteger.valueOf(3));
        assertConvergent(10, 7, testSubject.getCurrentConvergent());
        assertConvergent(3, 2, testSubject.getPreviousConvergent());

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> testSubject.addCoefficient(BigInteger.valueOf(-4))
        );

        Assertions.assertEquals(
                Arrays.asList(BigInteger.ONE, BigInteger.valueOf(2), BigInteger.valueOf(3)),
                testSubject.viewCoefficients()
        );
    }

    @Test
    void testSetLastCoefficient() {
        final SimpleContinuedFraction testSubject = new SimpleContinuedFraction();

        Assertions.assertThrows(IllegalStateException.class,
                () -> testSubject.setLastCoefficient(BigInteger.ONE)
        );

        testSubject.addCoefficient(BigInteger.ONE);
        BigInteger oldCoefficient = testSubject.setLastCoefficient(BigInteger.valueOf(-1));
        Assertions.assertEquals(BigInteger.ONE, oldCoefficient);
        assertConvergent(-1, 1, testSubject.getCurrentConvergent());
        assertConvergent(1, 0, testSubject.getPreviousConvergent());

        testSubject.addCoefficient(BigInteger.valueOf(2));
        oldCoefficient = testSubject.setLastCoefficient(BigInteger.valueOf(3));
        Assertions.assertEquals(BigInteger.valueOf(2), oldCoefficient);
        assertConvergent(-2, 3, testSubject.getCurrentConvergent());
        assertConvergent(-1, 1, testSubject.getPreviousConvergent());

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> testSubject.setLastCoefficient(BigInteger.valueOf(-3))
        );

        Assertions.assertEquals(
                Arrays.asList(BigInteger.valueOf(-1), BigInteger.valueOf(3)),
                testSubject.viewCoefficients()
        );
    }

    @Test
    void testRemoveLastCoefficient() {
        final SimpleContinuedFraction testSubject = new SimpleContinuedFraction();
        testSubject.addCoefficient(BigInteger.ZERO);
        testSubject.addCoefficient(BigInteger.valueOf(4));
        testSubject.addCoefficient(BigInteger.valueOf(5));
        testSubject.addCoefficient(BigInteger.valueOf(2));

        BigInteger removedCoefficient = testSubject.removeLastCoefficient();
        Assertions.assertEquals(BigInteger.valueOf(2), removedCoefficient);
        assertConvergent(5, 21, testSubject.getCurrentConvergent());
        assertConvergent(1, 4, testSubject.getPreviousConvergent());

        removedCoefficient = testSubject.removeLastCoefficient();
        Assertions.assertEquals(BigInteger.valueOf(5), removedCoefficient);
        assertConvergent(1, 4, testSubject.getCurrentConvergent());
        assertConvergent(0, 1, testSubject.getPreviousConvergent());

        removedCoefficient = testSubject.removeLastCoefficient();
        Assertions.assertEquals(BigInteger.valueOf(4), removedCoefficient);
        assertConvergent(0, 1, testSubject.getCurrentConvergent());
        assertConvergent(1, 0, testSubject.getPreviousConvergent());

        removedCoefficient = testSubject.removeLastCoefficient();
        Assertions.assertEquals(BigInteger.ZERO, removedCoefficient);

        Assertions.assertThrows(IllegalStateException.class,
                testSubject::removeLastCoefficient
        );

        Assertions.assertTrue(testSubject.viewCoefficients().isEmpty());
    }

    @Test
    void testConvergentAccessorsWithEmptyInstance() {
        final SimpleContinuedFraction testSubject = new SimpleContinuedFraction();

        assertConvergent(1, 0, testSubject.getCurrentConvergent());
        assertConvergent(0, 1, testSubject.getPreviousConvergent());
    }

    @Test
    void testToBigFraction() {
        final SimpleContinuedFraction testSubject = new SimpleContinuedFraction();

        Assertions.assertThrows(IllegalStateException.class,
                testSubject::toBigFraction
        );

        testSubject.addCoefficient(BigInteger.valueOf(3));
        Assertions.assertEquals(BigFraction.of(3, 1), testSubject.toBigFraction());

        testSubject.addCoefficient(BigInteger.valueOf(2));
        Assertions.assertEquals(BigFraction.of(7, 2), testSubject.toBigFraction());
    }

    @Test
    void testCoefficientsOf() {
        final Iterator<BigInteger[]> coefficientsIterator = SimpleContinuedFraction.coefficientsOf(BigFraction.of(-415, 93));
        BigInteger[] expectedCoefficients = toBigIntegerArray(-5, 1, 1, 6, 7);
        BigFraction[] expectedFractions = new BigFraction[]{
                BigFraction.of(-415, 93),
                BigFraction.of(93, 50),
                BigFraction.of(50, 43),
                BigFraction.of(43, 7),
                BigFraction.of(7, 1)
        };

        for (int i = 0; i < expectedCoefficients.length; i++) {
            Assertions.assertTrue(coefficientsIterator.hasNext());
            BigInteger[] next = coefficientsIterator.next();
            Assertions.assertEquals(3, next.length);
            Assertions.assertEquals(expectedCoefficients[i], next[0]);
            Assertions.assertEquals(1, next[2].signum());
            Assertions.assertEquals(expectedFractions[i], BigFraction.of(next[1], next[2]));
        }
        Assertions.assertFalse(coefficientsIterator.hasNext());
        Assertions.assertThrows(NoSuchElementException.class,
                coefficientsIterator::next
        );
    }
}
