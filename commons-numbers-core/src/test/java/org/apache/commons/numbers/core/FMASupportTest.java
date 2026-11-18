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

import java.lang.invoke.MethodHandle;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for the {@link FMASupport} class.
 */
class FMASupportTest {
    /** Counter incremented when MethodHandles to methods in this class are invoked. */
    private static int methodInvoked;

    /**
     * Test the FMA support can read system properties.
     * This method exercises the logic paths during FMA detection.
     */
    @Test
    void testIsSystemPropertyEnabled() {
        // Note: The SecurityManager is deprecated for removal from JDK 17.
        // An explicit test for a SecurityException raised by System.getProperty
        // due to a SecurityManager is not performed here. We test the same
        // catch block using a null or empty property name.
        // If any exception occurs reading the system properties the default value is ignored.
        Assertions.assertFalse(FMASupport.isSystemPropertyEnabled(null, false));
        Assertions.assertFalse(FMASupport.isSystemPropertyEnabled(null, true));
        Assertions.assertFalse(FMASupport.isSystemPropertyEnabled("", false));
        Assertions.assertFalse(FMASupport.isSystemPropertyEnabled("", true));

        final String key = "org.apache.commons.numbers.core.FMASupportTest";
        Assertions.assertNull(System.getProperty(key),
            () -> "System property is set for test key: " + key);

        try {
            // Any property that is missing uses the default
            Assertions.assertFalse(FMASupport.isSystemPropertyEnabled(key, false));
            Assertions.assertTrue(FMASupport.isSystemPropertyEnabled(key, true));
            // Set the property and test the boolean value
            System.setProperty(key, "something not case-insensitive true");
            Assertions.assertFalse(FMASupport.isSystemPropertyEnabled(key, false));
            Assertions.assertFalse(FMASupport.isSystemPropertyEnabled(key, true));
            // Empty
            System.setProperty(key, "");
            Assertions.assertFalse(FMASupport.isSystemPropertyEnabled(key, false));
            Assertions.assertFalse(FMASupport.isSystemPropertyEnabled(key, true));
            // Explicit false
            System.setProperty(key, "false");
            Assertions.assertFalse(FMASupport.isSystemPropertyEnabled(key, false));
            Assertions.assertFalse(FMASupport.isSystemPropertyEnabled(key, true));
            // true is case-insensitive
            System.setProperty(key, "true");
            Assertions.assertTrue(FMASupport.isSystemPropertyEnabled(key, false));
            Assertions.assertTrue(FMASupport.isSystemPropertyEnabled(key, true));
            System.setProperty(key, "TrUe");
            Assertions.assertTrue(FMASupport.isSystemPropertyEnabled(key, false));
            Assertions.assertTrue(FMASupport.isSystemPropertyEnabled(key, true));
        } finally {
            // Clean up
            System.clearProperty(key);
        }
    }

    /**
     * Test the FMA support can detect a functional FMA method.
     * This method exercises the logic paths during FMA detection.
     */
    @Test
    void testTestFMA() {
        // Test lookup is safe on methods that do not exist, e.g. Math.fma on Java 8
        Assertions.assertNull(lookupFMA("noSuchMethod"));

        // Test the check for the HotSpotDiagnosticMXBean is safe when exceptions are raised
        Assertions.assertFalse(FMASupport.readUseFMA("MissingPlatformManagedObject"));

        // Null operators return false
        Assertions.assertFalse(FMASupport.testFMA(null, false));
        Assertions.assertFalse(FMASupport.testFMA(null, true));

        // Note:
        // Methods should only be invoked if the UseFMA flag is true.
        // Checking the invocations will only fail on race conditions with the static
        // methodInvoked count if multiple instances of this test method are
        // executed in the same JVM.
        int count = methodInvoked;

        // An operator that raises an exception returns false even if the UseFMA flag is true
        final MethodHandle bad = lookupFMA("badFMA");
        Assertions.assertFalse(FMASupport.testFMA(bad, false));
        Assertions.assertEquals(count, methodInvoked, "Method handle was invoked when useFMA is false");
        Assertions.assertFalse(FMASupport.testFMA(bad, true));
        Assertions.assertEquals(++count, methodInvoked, "Method handle was not invoked");

        // An operator that cannot compute FMA returns false even if the UseFMA flag is true
        final MethodHandle noFMA = lookupFMA("noFMA");
        Assertions.assertFalse(FMASupport.testFMA(noFMA, false));
        Assertions.assertEquals(count, methodInvoked, "Method handle was invoked when useFMA is false");
        Assertions.assertFalse(FMASupport.testFMA(noFMA, true));
        Assertions.assertEquals(++count, methodInvoked, "Method handle was not invoked");

        // Provide an operator that will pass with finite arguments
        final MethodHandle good = lookupFMA("goodFMA");
        Assertions.assertFalse(FMASupport.testFMA(good, false));
        Assertions.assertEquals(count, methodInvoked, "Method handle was invoked when useFMA is false");
        Assertions.assertTrue(FMASupport.testFMA(good, true));
        Assertions.assertEquals(++count, methodInvoked, "Method handle was not invoked with UseFMA");
    }

    /**
     * Lookup the fused-multiply-add (FMA) operator {@code a * b + c} from this class.
     * This allows methods with different behaviour to be passed to
     * {@link FMASupport#testFMA(MethodHandle, java.util.function.BooleanSupplier)}.
     *
     * <p>This uses the lookup performed by FMASupport but requires a lookup with
     * full bytecode support. This allows FMASupport to see the package-private methods
     * in this class as they exist in the same package.
     *
     * @param name name of the FMA method
     * @return the method, or null
     */
    private static MethodHandle lookupFMA(String name) {
        return FMASupport.lookupFMA(false, FMASupportTest.class, name);
    }

    // Methods must be package-private for the FMASupport to look-up the method

    static double badFMA(double a, double b, double c) {
        methodInvoked++;
        throw new IllegalStateException();
    }

    static double noFMA(double a, double b, double c) {
        methodInvoked++;
        return a * b + c;
    }

    static double goodFMA(double a, double b, double c) {
        methodInvoked++;
        return new BigDecimal(a).multiply(new BigDecimal(b)).add(new BigDecimal(c)).doubleValue();
    }

    /**
     * Test the detected FMA method if it is available; otherwise check an exception is raised
     * for trying to use the method when it has been marked an unavailable.
     */
    @Test
    void testHardwareFMA() {
        final boolean hardware = FMASupport.isHardwareFMA();
        if (hardware) {
            // Note:
            // This result exploits round-to-even double arithmetic for the
            // unrepresentable points between 0, MIN_VALUE, and 2 * MIN_VALUE:
            // 1.5 * MIN_VALUE = 2 * MIN_VALUE after rounding
            // 0.5 * MIN_VALUE underflows to zero; add another MIN_VALUE = 1.5 MIN_VALUE
            final double a = Double.MIN_VALUE;
            final double b = 0.5;
            final double c = Double.MIN_VALUE;
            Assertions.assertEquals(0, a * b, "Expected underflow");
            Assertions.assertEquals(2 * a, 1.5 * a, "Expected rounding");
            Assertions.assertEquals(c, a * b + c, "Expected 0 + c due to intermediate underflow");
            final double abc = FMASupport.fma(a, b, c);
            Assertions.assertNotEquals(a * b + c, abc, "FMA should not match standard multiply-add");
            Assertions.assertEquals(2 * Double.MIN_VALUE, abc, "FMA=1.5*MIN_VALUE should round to 2*MIN_VALUE");
        } else {
            // Should raise an exception if used when no hardware FMA was detected
            Assertions.assertThrows(IllegalStateException.class, () -> FMASupport.fma(1, 2, 3));
        }
    }
}
