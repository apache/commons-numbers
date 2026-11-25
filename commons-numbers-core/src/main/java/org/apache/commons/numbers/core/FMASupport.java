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
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
import java.lang.management.PlatformManagedObject;

/**
 * Allows access to a hardward-supported Fused-Multiply-Add (FMA) operator.
 *
 * <p>This class is intentionally not part of the public API.
 *
 * <p>The class allows round-off components from the exact 106-bit product of two
 * 53-bit {@code double} factors to be computed using FMA:
 *
 * <pre>{@code
 * roundoff = Math.fma(x, y, -x * y)
 * }</pre>
 *
 * <p>This method allows the roundoff to be computed in 2 FLOPs compared to
 * the 16 FLOPs required when using Dekker's split method on the arguments.
 *
 * <p>All callers of this class must check if hardware FMA has been detected
 * using the method {@link #isHardwareFMA()}. If {@code true} then callers
 * can assume that the method {@link #fma(double, double, double)} will
 * function as a FMA operator with support for infinite precision in the
 * initial product {@code a * b} before addition of {@code c}.
 *
 * <p>Best efforts are made to detect and test FMA. Detection returns
 * false if the Java HotSpot VM flag argument {@code UseFMA} is disabled
 * with {@code -XX:-UseFMA}. This disables FMA application
 * wide and is not limited to this class.
 *
 * <p>To disable FMA usage only within this class (not application wide) the
 * system property {@code org.apache.commons.numbers.core.DD.fma} can be
 * set to false before this class is loaded.
 *
 * @since 1.4
 */
final class FMASupport {
    /** Name of the Commons Numbers system property to enable/disable use of FMA in DD. */
    private static final String PROPERTY_DD_FMA_KEY = "org.apache.commons.numbers.core.DD.fma";
    /** Default for the Commons Numbers system property if it does not exist. */
    private static final boolean PROPERTY_DD_FMA_DEFAULT_VALUE = true;
    /**
     * Name of the HotSpotDiagnosticMXBean.
     * See: https://docs.oracle.com/en/java/javase/17/docs/api/jdk.management/com/sun/management/HotSpotDiagnosticMXBean.html
     */
    private static final String COM_SUN_MANAGEMENT_HOT_SPOT_DIAGNOSTIC_MX_BEAN =
        "com.sun.management.HotSpotDiagnosticMXBean";
    /**
     * Set to true if {@code java.lang.Math.fma} uses hardware FMA instructions,
     * and can be invoked to generate results that avoid intermediate overflow
     * in the product {@code a * b} before addition of {@code c}.
     *
     * <p>This flag is set once when the class is loaded. It is set to {@code true}
     * only when the system property {@code org.apache.commons.numbers.core.DD.fma}
     * is unset, or is set to true; the HotSpot VM option {@code UseFMA} is {@code true};
     * and the method {@code Math.fma} can be invoked and returns a fused-multiply-add
     * result.
     */
    private static final boolean HARDWARE_FMA;
    /**
     * Method to compute fused-multiply-add.
     * <ul>
     * <li>{@code java.lang.Math.fma} if Java 9, and use of hardware FMA is enabled;
     * <li>otherwise a default implementation that raises an exception.
     * </ul>
     */
    private static final MethodHandle FUN;

    static {
        // The checks are performed in order of speed to reduce the one-off
        // initialisation cost for start-up cost sensitive users.
        // The FMA feature can be disabled using a system property, otherwise
        // an attempt is made to find and test Math.fma.
        // Note:
        // This uses the public lookup mechanism for static methods to find methods
        // added to java.lang.Math since java 8 to make them available in java 8.
        // For simplicity the lookup is always attempted rather than checking the
        // java version from System.getProperty("java.version") which is prone
        // to implementation specific property parsing errors.
        final MethodHandle op =
            isSystemPropertyEnabled(PROPERTY_DD_FMA_KEY, PROPERTY_DD_FMA_DEFAULT_VALUE) ?
            lookupFMA(true, Math.class, "fma") :
            null;

        // Check if the JVM flag for hardware FMA is enabled; then test the function.
        // If the function is null (Math.fma cannot be found); no flag is set; or the
        // method cannot be invoked to demonstrate a functional FMA method, this flag
        // is false. If false then register a method that raises an exception.
        // This exception will only be generated if this library attempts to use
        // FMA before checking if it is available. It indicates an internal error
        // that should be reported.
        HARDWARE_FMA = op != null &&
            testFMA(op, readUseFMA(COM_SUN_MANAGEMENT_HOT_SPOT_DIAGNOSTIC_MX_BEAN));
        FUN = HARDWARE_FMA ? op : null;
    }

    /** Private constructor. */
    private FMASupport() {
        // intentionally empty.
    }

    /**
     * Test if the system property is enabled. Cases:
     * <ul>
     *  <li>If the system property cannot be read (unknown status), return false
     *  <li>If the system property is null (no explicit property), return the default value
     *  <li>If the system property is not null, return the boolean value of the property
     * </ul>
     *
     * @param name name of the system property
     * @param defaultValue the value to use when the property is not set
     * @return true if the property is enabled, or does not exist and the default value is true
     */
    // package-private for testing
    static boolean isSystemPropertyEnabled(String name, boolean defaultValue) {
        final String property;
        try {
            property = System.getProperty(name);
        } catch (RuntimeException ignored) {
            // Either: SecurityException | NullPointerException | IllegalArgumentException
            // The property cannot be read.
            // It cannot be known if the property was set to true or false.
            return false;
        }
        if (property == null) {
            // Property was not set so use the default
            return defaultValue;
        }
        return Boolean.parseBoolean(property);
    }

    /**
     * Lookup the fused-multiply-add (FMA) operator {@code a * b + c} from the specified class.
     *
     * @param publicLookup true for a public look, else a lookup with full bytecode support for the caller.
     * @param cls class containing the method
     * @param name name of the FMA method
     * @return the method, or null
     */
    // package-private for testing
    static MethodHandle lookupFMA(boolean publicLookup, Class<?> cls, String name) {
        try {
            // The public lookup can find public methods in java.lang.Math.
            // The full bytecode support lookup can find the fall back method in this class.
            final Lookup lookup = publicLookup ?
                MethodHandles.publicLookup() :
                MethodHandles.lookup();
            return lookup.findStatic(cls,
                    name,
                    MethodType.methodType(double.class, double.class, double.class, double.class));
        } catch (NoSuchMethodException | IllegalAccessException ignored) {
            return null;
        }
    }

    /**
     * Test if the JVM uses hardware FMA instructions for {@code java.lang.Math.fma}.
     *
     * <p>Reads the HotSpot product flag {@code UseFMA} using the JDK management API
     * {@code com.sun.management.HotSpotDiagnosticMXBean}. The flag is documented in
     * the {@code java} tool reference: it enables hardware-based FMA intrinsics for
     * {@code java.lang.Math.fma} and is on by default if the hardware supports FMA.
     * Reflection is used to avoid a dependency on the {@code jdk.management} module.
     *
     * <p>Any failure (e.g. Java 8, a non-HotSpot JVM, or an unknown flag) returns false.
     *
     * @param className the name of the PlatformManagedObject
     * @return true if {@code UseFMA} is set to true
     */
    // package-private for testing
    static boolean readUseFMA(String className) {
        try {
            // Equivalent to:
            // import com.sun.management.HotSpotDiagnosticMXBean;
            //
            // HotSpotDiagnosticMXBean mxBean =
            //     ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
            // String value = mxBean.getVMOption("UseFMA").getValue();
            final Class<? extends PlatformManagedObject> c =
                Class.forName(className)
                    .asSubclass(PlatformManagedObject.class);
            final Object mxBean = ManagementFactory.getPlatformMXBean(c);
            final Object option = c.getMethod("getVMOption", String.class).invoke(mxBean, "UseFMA");
            final Object value = option.getClass().getMethod("getValue").invoke(option);
            // Check the VM option UseFMA was true or false, e.g -XX:+UseFMA or -XX:-UseFMA.
            // This allows FMA to be disabled in Commons Numbers core; it would also
            // disable it throughout the JVM for any use of java.lang.Math.fma(a, b, c).
            return Boolean.parseBoolean(String.valueOf(value));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    /**
     * Test the implementation of fused-multiply-add {@code a * b + c}.
     *
     * <p>If the method is null (e.g. Java 8), or the use FMA flag is false, this will
     * return false.
     *
     * <p>Otherwise the method is tested with arguments that would have intermediate
     * overflow if computed without infinite precision support for the result
     * {@code a * b} before addition of {@code c}.
     *
     * <p>It is assumed the invocation of the method may raise an
     * {@link IllegalStateException} if it cannot be invoked.
     *
     * @param op Method implementation (or null).
     * @param useFMA Flag to indicate hardware FMA has been detected
     * @return True if the method can be called to generate the expected result
     * @see #lookupFMA(boolean, Class, String)
     * @see #readUseFMA(String)
     */
    // package-private for testing
    static boolean testFMA(MethodHandle op, boolean useFMA) {
        if (!useFMA) {
            return false;
        }
        try {
            // Test a result that will overflow without FMA support
            // Arguments must be exact: double,double,double -> double
            return (double) op.invokeExact(Double.MAX_VALUE, 2.0, -Double.MAX_VALUE) == Double.MAX_VALUE;
        } catch (Throwable e) {
            // Method cannot be invoked
            return false;
        }
    }

    /**
     * Check if the the method {@link #fma(double, double, double)} has been detected to
     * use hardware FMA instructions for {@code java.lang.Math.fma}.
     *
     * <p><strong>Warning</strong>
     *
     * <p>If this method returns {@code false} then the method {@link #fma(double, double, double)}
     * is non-functional and will raise an exception.
     *
     * @return true if hardware FMA has been detected
     */
    static boolean isHardwareFMA() {
        return HARDWARE_FMA;
    }

    /**
     * Compute {@code a * b + c}.
     *
     * <p>This method uses a {@link MethodHandle} to call Java functions added since
     * Java 8 to the {@link Math} class:
     * <ul>
     * <li>{@code java.lang.Math.fma} if Java 9 and hardware FMA has been detected;
     * <li>otherwise raise an exception.
     * </ul>
     *
     * <p>This method should not be called if {@link #isHardwareFMA()} returns {@code false}.
     *
     * @param a Argument.
     * @param b Argument.
     * @param c Argument.
     * @return the result
     * @throws IllegalStateException if hardware FMA is not in use (internal error)
     * @see #isHardwareFMA()
     */
    static double fma(double a, double b, double c) {
        // FUN is a static final MethodHandle (or null). This should be inlined by the JIT
        // at the first successful call to the underlying method.
        try {
            return (double) FUN.invokeExact(a, b, c);
        } catch (Throwable cause) {
            throw new IllegalStateException(
                "Commons Numbers attempted to use FMA when hardware supported FMA is either " +
                "not available; has been disabled using the HotSpot VM argument -XX:-UseFMA; " +
                "or disabled before first use of DD using system property " +
                PROPERTY_DD_FMA_KEY +
                "=false. This is an internal error and should be reported as a bug.", cause
            );
        }
    }
}
