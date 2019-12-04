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

package org.apache.commons.numbers.complex;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.numbers.core.Precision;

/**
 * Representation of a Complex number, i.e. a number which has both a
 * real and imaginary part.
 *
 * <p>Implementations of arithmetic operations handle {@code NaN} and
 * infinite values according to the rules for {@link java.lang.Double}, i.e.
 * {@link #equals} is an equivalence relation for all instances that have
 * a {@code NaN} in either real or imaginary part, e.g. the following are
 * considered equal:</p>
 * <ul>
 *  <li>{@code 1 + NaNi}</li>
 *  <li>{@code NaN + i}</li>
 *  <li>{@code NaN + NaNi}</li>
 * </ul>
 *
 * <p>Note that this contradicts the IEEE-754 standard for floating
 * point numbers (according to which the test {@code x == x} must fail if
 * {@code x} is {@code NaN}). The method
 * {@link org.apache.commons.numbers.core.Precision#equals(double,double,int)
 * equals for primitive double} in class {@code Precision} conforms with
 * IEEE-754 while this class conforms with the standard behavior for Java
 * object types.</p>
 *
 * <p>Arithmetic in this class conforms to the C.99 standard for complex numbers
 * defined in ISO/IEC 9899, Annex G.<p>
 *
 * @see <a href="http://www.open-std.org/JTC1/SC22/WG14/www/standards">
 *    ISO/IEC 9899 - Programming languages - C</a>
 */
public final class Complex implements Serializable  {
    /** The square root of -1, a.k.a. "i". */
    public static final Complex I = new Complex(0, 1);
    /** A complex number representing one. */
    public static final Complex ONE = new Complex(1, 0);
    /** A complex number representing zero. */
    public static final Complex ZERO = new Complex(0, 0);
    /** A complex number representing "NaN + NaN i". */
    private static final Complex NAN = new Complex(Double.NaN, Double.NaN);
    /** 3*&pi;/4. */
    private static final double PI_3_OVER_4 = 0.75 * Math.PI;
    /** &pi;/2. */
    private static final double PI_OVER_2 = 0.5 * Math.PI;
    /** &pi;/4. */
    private static final double PI_OVER_4 = 0.25 * Math.PI;
    /** Expected number of elements when parsing text: 2. */
    private static final int TWO_ELEMENTS = 2;

    /** Serializable version identifier. */
    private static final long serialVersionUID = 20180201L;

    /** {@link #toString() String representation}. */
    private static final String FORMAT_START = "(";
    /** {@link #toString() String representation}. */
    private static final String FORMAT_END = ")";
    /** {@link #toString() String representation}. */
    private static final String FORMAT_SEP = ",";

    /** The imaginary part. */
    private final double imaginary;
    /** The real part. */
    private final double real;

    /**
     * Define a constructor for a Complex.
     * This is used in functions that implement trigonomic identities.
     */
    @FunctionalInterface
    private interface ComplexConstructor {
        /**
         * Create a complex number given the real and imaginary parts.
         *
         * @param real Real part.
         * @param imaginary Imaginary part.
         * @return {@code Complex} object
         */
        Complex create(double real, double imaginary);
    }

    /**
     * Private default constructor.
     *
     * @param real Real part.
     * @param imaginary Imaginary part.
     */
    private Complex(double real, double imaginary) {
        this.real = real;
        this.imaginary = imaginary;
    }

    /**
    * Create a complex number given the real and imaginary parts.
    *
    * @param real Real part.
    * @param imaginary Imaginary part.
    * @return {@code Complex} object
    */
    public static Complex ofCartesian(double real, double imaginary) {
        return new Complex(real, imaginary);
    }

    /**
    * Create a complex number given the real part.
    *
    * @param real Real part.
    * @return {@code Complex} object
    */
    public static Complex ofReal(double real) {
        return new Complex(real, 0);
    }

    /**
     * Creates a Complex from its polar representation.
     *
     * <p>If {@code r} is infinite and {@code theta} is finite, infinite or NaN
     * values may be returned in parts of the result, following the rules for
     * double arithmetic.</p>
     *
     * <pre>
     * Examples:
     * {@code
     * polar2Complex(INFINITY, \(\pi\)) = INFINITY + INFINITY i
     * polar2Complex(INFINITY, 0) = INFINITY + NaN i
     * polar2Complex(INFINITY, \(-\frac{\pi}{4}\)) = INFINITY - INFINITY i
     * polar2Complex(INFINITY, \(5\frac{\pi}{4}\)) = -INFINITY - INFINITY i }
     * </pre>
     *
     * @param r the modulus of the complex number to create
     * @param theta the argument of the complex number to create
     * @return {@code Complex}
     * @throws IllegalArgumentException if {@code r} is non-positive
     */
    public static Complex ofPolar(double r, double theta) {
        if (r <= 0) {
            throw new IllegalArgumentException("Non-positive polar modulus argument: " + r);
        }
        return new Complex(r * Math.cos(theta), r * Math.sin(theta));
    }

    /**
     * For a real constructor argument x, returns a new Complex object c
     * where {@code c = cos(x) + i sin (x)}.
     *
     * @param x {@code double} to build the cis number
     * @return {@code Complex}
     */
    public static Complex ofCis(double x) {
        return new Complex(Math.cos(x), Math.sin(x));
    }

    /**
     * Parses a string that would be produced by {@link #toString()}
     * and instantiates the corresponding object.
     *
     * @param s String representation.
     * @return an instance.
     * @throws NumberFormatException if the string does not conform
     * to the specification.
     */
    public static Complex parse(String s) {
        final int startParen = s.indexOf(FORMAT_START);
        if (startParen != 0) {
            throw new ComplexParsingException("Expected start string: " + FORMAT_START);
        }
        final int len = s.length();
        final int endParen = s.indexOf(FORMAT_END);
        if (endParen != len - 1) {
            throw new ComplexParsingException("Expected end string: " + FORMAT_END);
        }
        final String[] elements = s.substring(1, s.length() - 1).split(FORMAT_SEP);
        if (elements.length != TWO_ELEMENTS) {
            throw new ComplexParsingException("Incorrect number of parts: Expected 2 but was " +
                                              elements.length +
                                              " (separator is '" + FORMAT_SEP + "')");
        }

        final double re;
        try {
            re = Double.parseDouble(elements[0]);
        } catch (final NumberFormatException ex) {
            throw new ComplexParsingException("Could not parse real part" + elements[0], ex);
        }
        final double im;
        try {
            im = Double.parseDouble(elements[1]);
        } catch (final NumberFormatException ex) {
            throw new ComplexParsingException("Could not parse imaginary part" + elements[1], ex);
        }

        return ofCartesian(re, im);
    }

    /**
     * Returns true if either the real <em>or</em> imaginary component of the Complex is NaN
     * <em>and</em> the Complex is not infinite.
     *
     * <p>Note that in contrast to {@link Double#isNaN()}:
     * <ul>
     *   <li>There is more than one complex number that can return {@code true}.
     *   <li>Different representations of NaN can be distinguished by the
     *       {@link #equals(Object) Complex.equals(Object)} method.
     * </ul>
     *
     * @return {@code true} if this instance contains NaN and no infinite parts.
     * @see Double#isNaN(double)
     * @see #isInfinite()
     * @see #equals(Object) Complex.equals(Object)
     */
    public boolean isNaN() {
        if (Double.isNaN(real) || Double.isNaN(imaginary)) {
            return !isInfinite();
        }
        return false;
    }

    /**
     * Returns true if either real or imaginary component of the Complex is infinite.
     *
     * <p>Note: A complex or imaginary value with at least one infinite part is regarded
     * as an infinity (even if its other part is a NaN).</p>
     *
     * @return {@code true} if this instance contains an infinite value.
     * @see Double#isInfinite(double)
     */
    public boolean isInfinite() {
        return Double.isInfinite(real) || Double.isInfinite(imaginary);
    }

    /**
     * Returns true if both real and imaginary component of the Complex are finite.
     *
     * @return {@code true} if this instance contains finite values.
     * @see Double#isFinite(double)
     */
    public boolean isFinite() {
        return Double.isFinite(real) && Double.isFinite(imaginary);
    }

    /**
     * Returns projection of this complex number onto the Riemann sphere,
     * i.e. all infinities (including those with an NaN component)
     * project onto real infinity, as described in the
     * <a href="http://pubs.opengroup.org/onlinepubs/9699919799/functions/cproj.html">
     * IEEE and ISO C standards</a>.
     *
     * @return {@code Complex} projected onto the Riemann sphere.
     */
    public Complex proj() {
        if (Double.isInfinite(real) ||
            Double.isInfinite(imaginary)) {
            return new Complex(Double.POSITIVE_INFINITY, 0);
        }
        return this;
    }

     /**
     * Return the absolute value of this complex number.
     * This code follows the <a href="http://www.iso-9899.info/wiki/The_Standard">ISO C Standard</a>, Annex G,
     * in calculating the returned value (i.e. the hypot(x,y) method)
     * and in handling of NaNs.
     *
     * @return the absolute value.
     */
    public double abs() {
        // Delegate
        return Math.hypot(real, imaginary);
    }

    /**
     * Returns a {@code Complex} whose value is
     * {@code (this + addend)}.
     * Uses the definitional formula
     * <pre>
     *   (a + bi) + (c + di) = (a+c) + (b+d)i
     * </pre>
     *
     * @param  addend Value to be added to this {@code Complex}.
     * @return {@code this + addend}.
     */
    public Complex add(Complex addend) {
        return new Complex(real + addend.real,
                           imaginary + addend.imaginary);
    }

    /**
     * Returns a {@code Complex} whose value is {@code (this + addend)},
     * with {@code addend} interpreted as a real number.
     *
     * @param addend Value to be added to this {@code Complex}.
     * @return {@code this + addend}.
     * @see #add(Complex)
     */
    public Complex add(double addend) {
        return new Complex(real + addend, imaginary);
    }

     /**
     * Returns the conjugate of this complex number.
     * The conjugate of {@code a + bi} is {@code a - bi}.
     *
     * @return the conjugate of this complex object.
     */
    public Complex conjugate() {
        return new Complex(real, -imaginary);
    }

    /**
     * Returns the conjugate of this complex number
     * (C++11 grammar).
     *
     * @return the conjugate of this complex object.
     * @see #conjugate()
     */
    public Complex conj() {
        return conjugate();
    }

    /**
     * Returns a {@code Complex} whose value is
     * {@code (this / divisor)}.
     * Implements the definitional formula
     * <pre>
     * <code>
     *   a + bi     ac + bd + (bc - ad)i
     *   ------  =  --------------------
     *   c + di           c<sup>2</sup> + d<sup>2</sup>
     * </code>
     * </pre>
     *
     * <p>Recalculates to recover infinities as specified in C.99
     * standard G.5.1. Method is fully in accordance with
     * C++11 standards for complex numbers.</p>
     *
     * @param divisor Value by which this {@code Complex} is to be divided.
     * @return {@code this / divisor}.
     */
    public Complex divide(Complex divisor) {
        double a = real;
        double b = imaginary;
        double c = divisor.getReal();
        double d = divisor.getImaginary();
        int ilogbw = 0;
        final double logbw = Math.log(Math.max(Math.abs(c), Math.abs(d))) / Math.log(2);
        if (Double.isFinite(logbw)) {
            ilogbw = (int)logbw;
            c = Math.scalb(c, -ilogbw);
            d = Math.scalb(d, -ilogbw);
        }
        final double denom = c * c + d * d;
        double x = Math.scalb((a * c + b * d) / denom, -ilogbw);
        double y = Math.scalb((b * c - a * d) / denom, -ilogbw);
        // Recover infinities and zeros that computed as NaN+iNaN
        // the only cases are nonzero/zero, infinite/finite, and finite/infinite, ...
        // --------------
        // Modification from the listing in ISO C.99 G.5.1 (8):
        // Prevent overflow in (a * c + b * d) and (b * c - a * d).
        // It is only the sign that is important. not the magnitude.
        // --------------
        if (Double.isNaN(x) && Double.isNaN(y)) {
            if ((denom == 0.0) &&
                    (!Double.isNaN(a) || !Double.isNaN(b))) {
                // nonzero/zero
                x = Math.copySign(Double.POSITIVE_INFINITY, c) * a;
                y = Math.copySign(Double.POSITIVE_INFINITY, c) * b;
            } else if ((Double.isInfinite(a) || Double.isInfinite(b)) &&
                    Double.isFinite(c) && Double.isFinite(d)) {
                // infinite/finite
                a = boxInfinity(a);
                b = boxInfinity(b);
                x = Double.POSITIVE_INFINITY * computeACplusBD(a, b, c, d);
                y = Double.POSITIVE_INFINITY * computeBCminusAD(a, b, c, d);
            } else if (divisor.isInfinite() &&
                    Double.isFinite(a) && Double.isFinite(b)) {
                // finite/infinite
                c = boxInfinity(c);
                d = boxInfinity(d);
                x = 0.0 * computeACplusBD(a, b, c, d);
                y = 0.0 * computeBCminusAD(a, b, c, d);
            }
        }
        return new Complex(x, y);
    }

    /**
     * Compute {@code a*c + b*d} without overflow.
     * It is assumed: either {@code a} and {@code b} or {@code c} and {@code d} are
     * either zero or one (i.e. a boxed infinity); and the sign of the result is important,
     * not the value.
     *
     * @param a the a
     * @param b the b
     * @param c the c
     * @param d the d
     * @return the result
     */
    private static double computeACplusBD(double a, double b, double c, double d) {
        final double ac = a * c;
        final double bd = b * d;
        final double result = ac + bd;
        return Double.isFinite(result) ?
            result :
            // Overflow. Just divide by 2 as it is the sign of the result that matters.
            ac * 0.5 + bd * 0.5;
    }

    /**
     * Compute {@code b*c - a*d} without overflow.
     * It is assumed: either {@code a} and {@code b} or {@code c} and {@code d} are
     * either zero or one (i.e. a boxed infinity); and the sign of the result is important,
     * not the value.
     *
     * @param a the a
     * @param b the b
     * @param c the c
     * @param d the d
     * @return the result
     */
    private static double computeBCminusAD(double a, double b, double c, double d) {
        final double bc = b * c;
        final double ad = a * d;
        final double result = bc - ad;
        return Double.isFinite(result) ?
            result :
            // Overflow. Just divide by 2 as it is the sign of the result that matters.
            bc * 0.5 + ad * 0.5;
    }

    /**
     * Returns a {@code Complex} whose value is {@code (this / divisor)},
     * with {@code divisor} interpreted as a real number.
     *
     * @param  divisor Value by which this {@code Complex} is to be divided.
     * @return {@code this / divisor}.
     * @see #divide(Complex)
     */
    public Complex divide(double divisor) {
        return divide(new Complex(divisor, 0));
    }

    /**
     * Returns the multiplicative inverse of this instance.
     *
     * @return {@code 1 / this}.
     * @see #divide(Complex)
     */
    public Complex reciprocal() {
        if (Math.abs(real) < Math.abs(imaginary)) {
            final double q = real / imaginary;
            final double scale = 1. / (real * q + imaginary);
            double scaleQ = 0;
            if (q != 0 &&
                scale != 0) {
                scaleQ = scale * q;
            }
            return new Complex(scaleQ, -scale);
        }
        final double q = imaginary / real;
        final double scale = 1. / (imaginary * q + real);
        double scaleQ = 0;
        if (q != 0 &&
            scale != 0) {
            scaleQ = scale * q;
        }
        return new Complex(scale, -scaleQ);
    }

    /**
     * Test for equality with another object. If the other object is a {@code Complex} then a
     * comparison is made of the real and imaginary parts; otherwise {@code false} is returned.
     *
     * <p>If both the real and imaginary parts of two complex numbers
     * are exactly the same the two {@code Complex} objects are considered to be equal.
     * For this purpose, two {@code double} values are considered to be
     * the same if and only if the method {@link Double #doubleToLongBits(double)}
     * returns the identical {@code long} value when applied to each.
     *
     * <p>Note that in most cases, for two instances of class
     * {@code Complex}, {@code c1} and {@code c2}, the
     * value of {@code c1.equals(c2)} is {@code true} if and only if
     *
     * <pre>
     *  {@code c1.getReal() == c2.getReal() && c1.getImaginary() == c2.getImaginary()}
     * </pre>
     *
     * <p>also has the value {@code true}. However, there are exceptions:
     *
     * <ul>
     *  <li>
     *   Instances that contain {@code NaN} values in the same part
     *   are considered to be equal for that part, even though {@code Double.NaN==Double.NaN}
     *   has the value {@code false}.
     *  </li>
     *  <li>
     *   Instances that share a {@code NaN} value in one part
     *   but have different values in the other part are <em>not</em> considered equal.
     *  </li>
     *  <li>
     *   Instances that contain different representations of zero in the same part
     *   are <em>not</em> considered to be equal for that part, even though {@code -0.0==0.0}
     *   has the value {@code true}.
     *  </li>
     * </ul>
     *
     * <p>The behavior is the same as if the components of the two complex numbers were passed
     * to {@link java.util.Arrays#equals(double[], double[]) Arrays.equals(double[], double[])}:
     *
     * <pre>
     *  <code>
     *   Arrays.equals(new double[]{c1.getReal(), c1.getImaginary()},
     *                 new double[]{c2.getReal(), c2.getImaginary()});
     *  </code>
     * </pre>
     *
     * @param other Object to test for equality with this instance.
     * @return {@code true} if the objects are equal, {@code false} if object
     * is {@code null}, not an instance of {@code Complex}, or not equal to
     * this instance.
     * @see java.lang.Double#doubleToLongBits(double)
     * @see java.util.Arrays#equals(double[], double[])
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Complex) {
            final Complex c = (Complex) other;
            return equals(real, c.real) &&
                equals(imaginary, c.imaginary);
        }
        return false;
    }

    /**
     * Test for the floating-point equality between Complex objects.
     * It returns {@code true} if both arguments are equal or within the
     * range of allowed error (inclusive).
     *
     * @param x First value (cannot be {@code null}).
     * @param y Second value (cannot be {@code null}).
     * @param maxUlps {@code (maxUlps - 1)} is the number of floating point
     * values between the real (resp. imaginary) parts of {@code x} and
     * {@code y}.
     * @return {@code true} if there are fewer than {@code maxUlps} floating
     * point values between the real (resp. imaginary) parts of {@code x}
     * and {@code y}.
     *
     * @see Precision#equals(double,double,int)
     */
    public static boolean equals(Complex x,
                                 Complex y,
                                 int maxUlps) {
        return Precision.equals(x.real, y.real, maxUlps) &&
            Precision.equals(x.imaginary, y.imaginary, maxUlps);
    }

    /**
     * Returns {@code true} iff the values are equal as defined by
     * {@link #equals(Complex,Complex,int) equals(x, y, 1)}.
     *
     * @param x First value (cannot be {@code null}).
     * @param y Second value (cannot be {@code null}).
     * @return {@code true} if the values are equal.
     */
    public static boolean equals(Complex x,
                                 Complex y) {
        return equals(x, y, 1);
    }

    /**
     * Returns {@code true} if, both for the real part and for the imaginary
     * part, there is no double value strictly between the arguments or the
     * difference between them is within the range of allowed error
     * (inclusive).  Returns {@code false} if either of the arguments is NaN.
     *
     * @param x First value (cannot be {@code null}).
     * @param y Second value (cannot be {@code null}).
     * @param eps Amount of allowed absolute error.
     * @return {@code true} if the values are two adjacent floating point
     * numbers or they are within range of each other.
     *
     * @see Precision#equals(double,double,double)
     */
    public static boolean equals(Complex x,
                                 Complex y,
                                 double eps) {
        return Precision.equals(x.real, y.real, eps) &&
            Precision.equals(x.imaginary, y.imaginary, eps);
    }

    /**
     * Returns {@code true} if, both for the real part and for the imaginary
     * part, there is no double value strictly between the arguments or the
     * relative difference between them is smaller or equal to the given
     * tolerance. Returns {@code false} if either of the arguments is NaN.
     *
     * @param x First value (cannot be {@code null}).
     * @param y Second value (cannot be {@code null}).
     * @param eps Amount of allowed relative error.
     * @return {@code true} if the values are two adjacent floating point
     * numbers or they are within range of each other.
     *
     * @see Precision#equalsWithRelativeTolerance(double,double,double)
     */
    public static boolean equalsWithRelativeTolerance(Complex x, Complex y,
                                                      double eps) {
        return Precision.equalsWithRelativeTolerance(x.real, y.real, eps) &&
            Precision.equalsWithRelativeTolerance(x.imaginary, y.imaginary, eps);
    }

    /**
     * Get a hash code for the complex number.
     *
     * <p>The behavior is the same as if the components of the complex number were passed
     * to {@link java.util.Arrays#hashCode(double[]) Arrays.hashCode(double[])}:
     * <pre>
     *  {@code Arrays.hashCode(new double[]{getReal(), getImaginary()})}
     * </pre>
     *
     * @return a hash code value for this object.
     * @see java.util.Arrays#hashCode(double[]) Arrays.hashCode(double[])
     */
    @Override
    public int hashCode() {
        return 31 * (31 + Double.hashCode(real)) + Double.hashCode(imaginary);
    }

    /**
     * Access the imaginary part.
     *
     * @return the imaginary part.
     */
    public double getImaginary() {
        return imaginary;
    }

    /**
     * Access the imaginary part (C++ grammar).
     *
     * @return the imaginary part.
     */
    public double imag() {
        return imaginary;
    }

    /**
     * Access the real part.
     *
     * @return the real part.
     */
    public double getReal() {
        return real;
    }

     /**
     * Access the real part (C++ grammar).
     *
     * @return the real part.
     */
    public double real() {
        return real;
    }

    /**
     * Returns a {@code Complex} whose value is {@code this * factor}.
     * Implements the definitional formula:
     * <pre>
     *   (a + bi)(c + di) = (ac - bd) + (ad + bc)i
     * </pre>
     *
     * <p>Recalculates to recover infinities as specified in C.99
     * standard G.5.1. Method is fully in accordance with
     * C++11 standards for complex numbers.</p>
     *
     * @param  factor value to be multiplied by this {@code Complex}.
     * @return {@code this * factor}.
     */
    public Complex multiply(Complex factor) {
        double a = real;
        double b = imaginary;
        double c = factor.getReal();
        double d = factor.getImaginary();
        final double ac = a * c;
        final double bd = b * d;
        final double ad = a * d;
        final double bc = b * c;
        double x = ac - bd;
        double y = ad + bc;

        // --------------
        // NaN can occur if:
        // - any of (a,b,c,d) are NaN (for NaN or Infinite complex numbers)
        // - a multiplication of infinity by zero (ac,bd,ad,bc).
        // - a subtraction of infinity from infinity (e.g. ac - bd)
        //   Note that (ac,bd,ad,bc) can be infinite due to overflow.
        //
        // Detect a NaN result and perform correction.
        //
        // Modification from the listing in ISO C.99 G.5.1 (6)
        // Do not correct infinity multiplied by zero. This is left as NaN.
        // --------------

        if (Double.isNaN(x) && Double.isNaN(y)) {
            // Recover infinities that computed as NaN+iNaN ...
            boolean recalc = false;
            if (isInfinite() && isNotZero(c, d)) {
                // This complex is infinite.
                // "Box" the infinity and change NaNs in the other factor to 0.
                a = boxInfinity(a);
                b = boxInfinity(b);
                c = changeNaNtoZero(c);
                d = changeNaNtoZero(d);
                recalc = true;
            }
            // (c, d) may have been corrected so do not use factor.isInfinite().
            if ((Double.isInfinite(c) || Double.isInfinite(d)) &&
                isNotZero(a, b)) {
                // This other complex is infinite.
                // "Box" the infinity and change NaNs in the other factor to 0.
                c = boxInfinity(c);
                d = boxInfinity(d);
                a = changeNaNtoZero(a);
                b = changeNaNtoZero(b);
                recalc = true;
            }
            if (!recalc && (Double.isInfinite(ac) || Double.isInfinite(bd) ||
                            Double.isInfinite(ad) || Double.isInfinite(bc))) {
                // The result overflowed to infinity.
                // Recover infinities from overflow by changing NaNs to 0 ...
                a = changeNaNtoZero(a);
                b = changeNaNtoZero(b);
                c = changeNaNtoZero(c);
                d = changeNaNtoZero(d);
                recalc = true;
            }
            if (recalc) {
                x = Double.POSITIVE_INFINITY * (a * c - b * d);
                y = Double.POSITIVE_INFINITY * (a * d + b * c);
            }
        }
        return new Complex(x, y);
    }

    /**
     * Box values for the real or imaginary component of an infinite complex number.
     * Any infinite value will be returned as one. Non-infinite values will be returned as zero.
     * The sign is maintained.
     *
     * <pre>
     *  inf  =  1
     * -inf  = -1
     *  x    =  0
     * -x    = -0
     * </pre>
     *
     * @param component the component
     * @return the boxed value
     */
    private static double boxInfinity(double component) {
        return Math.copySign(Double.isInfinite(component) ? 1.0 : 0.0, component);
    }

    /**
     * Checks if the complex number is not zero.
     *
     * @param real the real component
     * @param imaginary the imaginary component
     * @return true if the complex is not zero
     */
    private static boolean isNotZero(double real, double imaginary) {
        // The use of equals is deliberate.
        // This method must distinguish NaN from zero thus ruling out:
        // (real != 0.0 || imaginary != 0.0)
        return !(real == 0.0 && imaginary == 0.0);
    }

    /**
     * Change NaN to zero preserving the sign; otherwise return the value.
     *
     * @param value the value
     * @return the new value
     */
    private static double changeNaNtoZero(double value) {
        return Double.isNaN(value) ? Math.copySign(0.0, value) : value;
    }

    /**
     * Returns a {@code Complex} whose value is {@code this * factor}, with {@code factor}
     * interpreted as a integer number.
     *
     * @param  factor value to be multiplied by this {@code Complex}.
     * @return {@code this * factor}.
     * @see #multiply(Complex)
     */
    public Complex multiply(final int factor) {
        return new Complex(real * factor, imaginary * factor);
    }

    /**
     * Returns a {@code Complex} whose value is {@code this * factor}, with {@code factor}
     * interpreted as a real number.
     *
     * @param  factor value to be multiplied by this {@code Complex}.
     * @return {@code this * factor}.
     * @see #multiply(Complex)
     */
    public Complex multiply(double factor) {
        return new Complex(real * factor, imaginary * factor);
    }

    /**
     * Returns a {@code Complex} whose value is {@code (-this)}.
     *
     * @return {@code -this}.
     */
    public Complex negate() {
        return new Complex(-real, -imaginary);
    }

    /**
     * Returns a {@code Complex} whose value is
     * {@code (this - subtrahend)}.
     * Uses the definitional formula
     * <p>
     *  {@code (a + bi) - (c + di) = (a-c) + (b-d)i}
     * </p>
     *
     * @param  subtrahend value to be subtracted from this {@code Complex}.
     * @return {@code this - subtrahend}.
     */
    public Complex subtract(Complex subtrahend) {
        return new Complex(real - subtrahend.real,
                           imaginary - subtrahend.imaginary);
    }

    /**
     * Returns a {@code Complex} whose value is
     * {@code (this - subtrahend)}.
     *
     * @param  subtrahend value to be subtracted from this {@code Complex}.
     * @return {@code this - subtrahend}.
     * @see #subtract(Complex)
     */
    public Complex subtract(double subtrahend) {
        return new Complex(real - subtrahend, imaginary);
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/InverseCosine.html">
     * inverse cosine</a> of this complex number.
     * Implements the formula:
     * <pre>
     * <code>
     *   acos(z) = -i (log(z + i (sqrt(1 - z<sup>2</sup>))))
     * </code>
     * </pre>
     *
     * @return the inverse cosine of this complex number.
     */
    public Complex acos() {
        if (Double.isFinite(real)) {
            if (Double.isFinite(imaginary)) {
                // Special case for zero
                if (real == 0 && imaginary == 0) {
                    return new Complex(PI_OVER_2, Math.copySign(0, -imaginary));
                }
                // ISO C99: Preserve the equality
                // acos(conj(z)) = conj(acos(z))
                Complex z;
                ComplexConstructor constructor;
                if (negative(imaginary)) {
                    z = conj();
                    constructor = Complex::ofCartesianConjugate;
                } else {
                    z = this;
                    constructor = Complex::ofCartesian;
                }
                return z.add(z.sqrt1z().multiplyByI()).log().multiplyByNegI(constructor);
            }
            if (Double.isInfinite(imaginary)) {
                return new Complex(PI_OVER_2, Math.copySign(Double.POSITIVE_INFINITY, -imaginary));
            }
            // imaginary is NaN
            // Special case for real == 0
            return real == 0 ? new Complex(PI_OVER_2, Double.NaN) : NAN;
        }
        if (Double.isInfinite(real)) {
            if (Double.isFinite(imaginary)) {
                final double re = real == Double.NEGATIVE_INFINITY ? Math.PI : 0;
                return new Complex(re, Math.copySign(Double.POSITIVE_INFINITY, -imaginary));
            }
            if (Double.isInfinite(imaginary)) {
                final double re = real == Double.NEGATIVE_INFINITY ? PI_3_OVER_4 : PI_OVER_4;
                return new Complex(re, Math.copySign(Double.POSITIVE_INFINITY, -imaginary));
            }
            // imaginary is NaN
            // Swap real and imaginary
            return new Complex(Double.NaN, real);
        }
        // real is NaN
        if (Double.isInfinite(imaginary)) {
            return new Complex(Double.NaN, -imaginary);
        }
        // optionally raises the ‘‘invalid’’ floating-point exception, for finite y.
        return NAN;
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/InverseSine.html">
     * inverse sine</a> of this complex number.
     * <pre>
     * <code>
     *   asin(z) = -i (log(sqrt(1 - z<sup>2</sup>) + iz))
     * </code>
     * </pre>
     *
     * <p>As per the C.99 standard this function is computed using the trigonomic identity:</p>
     * <pre>
     *   asin(z) = -i asinh(iz)
     * </pre>
     *
     * @return the inverse sine of this complex number
     */
    public Complex asin() {
        // Define in terms of asinh
        // asin(z) = -i asinh(iz)
        return multiplyByI().asinh().multiplyByNegI();
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/InverseTangent.html">
     * inverse tangent</a> of this complex number.
     * Implements the formula:
     * <pre>
     *   atan(z) = (i/2) log((i + z)/(i - z))
     * </pre>
     *
     * <p>As per the C.99 standard this function is computed using the trigonomic identity:</p>
     * <pre>
     *   atan(z) = -i atanh(iz)
     * </pre>
     *
     * @return the inverse tangent of this complex number
     */
    public Complex atan() {
        // Define in terms of atanh
        // atan(z) = -i atanh(iz)
        return multiplyByI().atanh().multiplyByNegI();
    }

    /**
     * Multiply the Complex by I.
     *
     * @return the result (iz)
     */
    private Complex multiplyByI() {
        return new Complex(-imaginary, real);
    }

    /**
     * Multiply the Complex by -I.
     *
     * @return the result (-iz)
     */
    private Complex multiplyByNegI() {
        return new Complex(imaginary, -real);
    }

    /**
     * Multiply the Complex by -I and create the result using the constructor.
     *
     * @param constructor Constructor
     * @return the result (-iz)
     */
    private Complex multiplyByNegI(ComplexConstructor constructor) {
        return constructor.create(imaginary, -real);
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/InverseHyperbolicSine.html">
     * inverse hyperbolic sine</a> of this complex number.
     * Implements the formula:
     * <pre>
     *   asinh(z) = log(z+sqrt(z^2+1))
     * </pre>
     *
     * @return the inverse hyperbolic sine of this complex number
     */
    public Complex asinh() {
        if (Double.isFinite(real)) {
            if (Double.isFinite(imaginary)) {
                // Special case for zero
                if (real == 0 && imaginary == 0) {
                    return this;
                }
                // ISO C99: Preserve the equality
                // asinh(conj(z)) = conj(asinh(z))
                final Complex z = negative(imaginary) ? conjugate() : this;
                final Complex result = z.square().add(ONE).sqrt().add(z).log();
                return z == this ? result : result.conjugate();
            }
            if (Double.isInfinite(imaginary)) {
                return new Complex(Double.POSITIVE_INFINITY, Math.copySign(PI_OVER_2, imaginary));
            }
            // imaginary is NaN
            return NAN;
        }
        if (Double.isInfinite(real)) {
            if (Double.isFinite(imaginary)) {
                return new Complex(real, Math.copySign(0, imaginary));
            }
            if (Double.isInfinite(imaginary)) {
                return new Complex(real, Math.copySign(PI_OVER_4, imaginary));
            }
            // imaginary is NaN
            return new Complex(real, Double.NaN);
        }
        // real is NaN
        if (imaginary == 0) {
            return new Complex(Double.NaN, Math.copySign(0, imaginary));
        }
        if (Double.isInfinite(imaginary)) {
            return new Complex(Double.POSITIVE_INFINITY, Double.NaN);
        }
        // optionally raises the ‘‘invalid’’ floating-point exception, for finite y.
        return NAN;
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/InverseHyperbolicTangent.html">
     * inverse hyperbolic tangent</a> of this complex number.
     * Implements the formula:
     * <pre>
     *   atanh(z) = log((1+z)/(1-z))/2
     * </pre>
     *
     * @return the inverse hyperbolic tangent of this complex number
     */
    public Complex atanh() {
        if (Double.isFinite(real)) {
            if (Double.isFinite(imaginary)) {
                // Special case for zero
                if (imaginary == 0) {
                    if (real == 0) {
                        return this;
                    }
                    if (Math.abs(real) == 1) {
                        // raises the ‘‘divide-by-zero’’ floating-point exception.
                        return new Complex(Math.copySign(Double.POSITIVE_INFINITY, real), imaginary);
                    }
                }
                // ISO C99: Preserve the equality
                // atanh(conj(z)) = conj(atanh(z))
                final Complex z = negative(imaginary) ? conjugate() : this;
                final Complex result = z.add(ONE).divide(ONE.subtract(z)).log().multiply(0.5);
                return z == this ? result : result.conjugate();
            }
            if (Double.isInfinite(imaginary)) {
                return new Complex(Math.copySign(0, real), Math.copySign(PI_OVER_2, imaginary));
            }
            // imaginary is NaN
            // Special case for real == 0
            return real == 0 ? new Complex(real, Double.NaN) : NAN;
        }
        if (Double.isInfinite(real)) {
            if (Double.isNaN(imaginary)) {
                return new Complex(Math.copySign(0, real), Double.NaN);
            }
            // imaginary is finite or infinite
            return new Complex(Math.copySign(0, real), Math.copySign(PI_OVER_2, imaginary));
        }
        // real is NaN
        if (Double.isInfinite(imaginary)) {
            return new Complex(0, Math.copySign(PI_OVER_2, imaginary));
        }
        // optionally raises the ‘‘invalid’’ floating-point exception, for finite y.
        return NAN;
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/InverseHyperbolicCosine.html">
     * inverse hyperbolic cosine</a> of this complex number.
     * Implements the formula:
     * <pre>
     *   acosh(z) = log(z+sqrt(z^2-1))
     * </pre>
     *
     * @return the inverse hyperbolic cosine of this complex number
     */
    public Complex acosh() {
        if (Double.isFinite(real)) {
            if (Double.isFinite(imaginary)) {
                // Special case for zero
                if (real == 0 && imaginary == 0) {
                    return new Complex(real, Math.copySign(PI_OVER_2, imaginary));
                }
                // ISO C99: Preserve the equality
                // acosh(conj(z)) = conj(acosh(z))
                final Complex z = negative(imaginary) ? conjugate() : this;
                final Complex result = z.square().subtract(ONE).sqrt().add(z).log();
                return z == this ? result : result.conjugate();
            }
            if (Double.isInfinite(imaginary)) {
                return new Complex(Double.POSITIVE_INFINITY, Math.copySign(PI_OVER_2, imaginary));
            }
            // imaginary is NaN
            return NAN;
        }
        if (Double.isInfinite(real)) {
            if (Double.isFinite(imaginary)) {
                final double im = real == Double.NEGATIVE_INFINITY ? Math.PI : 0;
                return new Complex(Double.POSITIVE_INFINITY, Math.copySign(im, imaginary));
            }
            if (Double.isInfinite(imaginary)) {
                final double im = real == Double.NEGATIVE_INFINITY ? PI_3_OVER_4 : PI_OVER_4;
                return new Complex(Double.POSITIVE_INFINITY, Math.copySign(im, imaginary));
            }
            // imaginary is NaN
            return new Complex(Double.POSITIVE_INFINITY, Double.NaN);
        }
        // real is NaN
        if (Double.isInfinite(imaginary)) {
            return new Complex(Double.POSITIVE_INFINITY, Double.NaN);
        }
        // optionally raises the ‘‘invalid’’ floating-point exception, for finite y.
        return NAN;
    }

    /**
     * Compute the square of this complex number.
     *
     * @return square of this complex number
     */
    public Complex square() {
        return multiply(this);
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/Cosine.html">
     * cosine</a> of this complex number.
     * Implements the formula:
     * <pre>
     *   cos(a + bi) = cos(a)cosh(b) - sin(a)sinh(b)i}
     * </pre>
     * where the (real) functions on the right-hand side are
     * {@link Math#sin}, {@link Math#cos},
     * {@link Math#cosh} and {@link Math#sinh}.
     *
     * <p>As per the C.99 standard this function is computed using the trigonomic identity:</p>
     * <pre>
     *   cos(z) = cosh(iz)
     * </pre>
     *
     * @return the cosine of this complex number.
     */
    public Complex cos() {
        // Define in terms of cosh
        // cos(z) = cosh(iz)
        // Multiply this number by I and compute cosh.
        return cosh(-imaginary, real, Complex::ofCartesian);
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/HyperbolicCosine.html">
     * hyperbolic cosine</a> of this complex number.
     * Implements the formula:
     * <pre>
     *   cosh(a + bi) = cosh(a)cos(b) + sinh(a)sin(b)i
     * </pre>
     * where the (real) functions on the right-hand side are
     * {@link Math#sin}, {@link Math#cos},
     * {@link Math#cosh} and {@link Math#sinh}.
     *
     * @return the hyperbolic cosine of this complex number.
     */
    public Complex cosh() {
        return cosh(real, imaginary, Complex::ofCartesian);
    }

    /**
     * Compute the hyperbolic cosine of the complex number.
     *
     * <p>This function exists to allow implementation of the identity
     * {@code cos(z) = cosh(iz)}.<p>
     *
     * @param real Real part.
     * @param imaginary Imaginary part.
     * @param constructor Constructor.
     * @return the hyperbolic cosine of this complex number
     */
    private static Complex cosh(double real, double imaginary, ComplexConstructor constructor) {
        if (Double.isFinite(real)) {
            if (Double.isFinite(imaginary)) {
                return constructor.create(Math.cosh(real) * Math.cos(imaginary),
                                          Math.sinh(real) * Math.sin(imaginary));
            }
            // Special case for real == 0
            final double im = real == 0 ? Math.copySign(0, imaginary) : Double.NaN;
            return constructor.create(Double.NaN, im);
        }
        if (Double.isInfinite(real)) {
            if (Double.isFinite(imaginary)) {
                if (imaginary == 0) {
                    // Determine sign
                    final double im = real > 0 ? imaginary : -imaginary;
                    return constructor.create(Double.POSITIVE_INFINITY, im);
                }
                // inf * cis(y)
                final double re = real * Math.cos(imaginary);
                final double im = real * Math.sin(imaginary);
                return constructor.create(re, im);
            }
            // imaginary is infinite or NaN
            return constructor.create(Double.POSITIVE_INFINITY, Double.NaN);
        }
        // real is NaN
        if (imaginary == 0) {
            return constructor.create(Double.NaN, Math.copySign(0, imaginary));
        }
        // optionally raises the ‘‘invalid’’ floating-point exception, for nonzero y.
        return NAN;
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/ExponentialFunction.html">
     * exponential function</a> of this complex number.
     * Implements the formula:
     * <pre>
     *   exp(a + bi) = exp(a)cos(b) + exp(a)sin(b)i
     * </pre>
     * where the (real) functions on the right-hand side are
     * {@link Math#exp}, {@link Math#cos}, and
     * {@link Math#sin}.
     *
     * @return <code><i>e</i><sup>this</sup></code>.
     */
    public Complex exp() {
        if (Double.isFinite(real)) {
            if (Double.isFinite(imaginary)) {
                final double expReal = Math.exp(real);
                if (imaginary == 0) {
                    // Preserve sign for conjugate equality
                    return new Complex(expReal, imaginary);
                }
                return new Complex(expReal * Math.cos(imaginary),
                                   expReal * Math.sin(imaginary));
            }
            // Imaginary is infinite or nan
            return NAN;
        }
        if (Double.isInfinite(real)) {
            if (Double.isFinite(imaginary)) {
                if (real == Double.POSITIVE_INFINITY) {
                    if (imaginary == 0) {
                        return this;
                    }
                    // inf * cis(y)
                    final double re = Double.POSITIVE_INFINITY * Math.cos(imaginary);
                    final double im = Double.POSITIVE_INFINITY * Math.sin(imaginary);
                    return new Complex(re, im);
                }
                // +0 * cis(y)
                final double re = 0.0 * Math.cos(imaginary);
                final double im = 0.0 * Math.sin(imaginary);
                return new Complex(re, im);
            }
            // imaginary is infinite or NaN
            if (real == Double.POSITIVE_INFINITY) {
                return new Complex(Double.POSITIVE_INFINITY, Double.NaN);
            }
            // Preserve sign for conjugate equality
            return new Complex(0, Math.copySign(0, imaginary));
        }
        // real is NaN
        if (imaginary == 0) {
            return new Complex(Double.NaN, Math.copySign(0, imaginary));
        }
        // optionally raises the ‘‘invalid’’ floating-point exception, for finite y.
        return NAN;
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/NaturalLogarithm.html">
     * natural logarithm</a> of this complex number.
     * Implements the formula:
     * <pre>
     *   log(a + bi) = ln(|a + bi|) + arg(a + bi)i
     * </pre>
     * where ln on the right hand side is {@link Math#log},
     * {@code |a + bi|} is the modulus, {@link Complex#abs}, and
     * {@code arg(a + bi) = }{@link Math#atan2}(b, a).
     *
     * @return the natural logarithm of {@code this}.
     */
    public Complex log() {
        // All edge cases satisfied by the Math library
        return new Complex(Math.log(abs()),
                           Math.atan2(imaginary, real));
    }

    /**
     * Compute the base 10 or
     * <a href="http://mathworld.wolfram.com/CommonLogarithm.html">
     * common logarithm</a> of this complex number.
     * Implements the formula:
     * <pre>
     *   log10(a + bi) = log(|a + bi|) + arg(a + bi)i
     * </pre>
     * where log on the right hand side is {@link Math#log10},
     * {@code |a + bi|} is the modulus, {@link Complex#abs}, and
     * {@code arg(a + bi) = }{@link Math#atan2}(b, a).
     *
     * @return the base 10 logarithm of {@code this}.
     */
    public Complex log10() {
        // All edge cases satisfied by the Math library
        return new Complex(Math.log10(abs()),
                           Math.atan2(imaginary, real));
    }

    /**
     * Returns of value of this complex number raised to the power of {@code x}.
     * Implements the formula:
     * <pre>
     * <code>
     *   y<sup>x</sup> = exp(x&middot;log(y))
     * </code>
     * </pre>
     * where {@code exp} and {@code log} are {@link #exp} and
     * {@link #log}, respectively.
     *
     * @param  x exponent to which this {@code Complex} is to be raised.
     * @return <code>this<sup>x</sup></code>.
     */
    public Complex pow(Complex x) {
        if (real == 0 &&
            imaginary == 0) {
            // This value is zero. Test the other.
            if (x.real > 0 &&
                x.imaginary == 0) {
                // 0 raised to positive number is 0
                return ZERO;
            }
            // 0 raised to anything else is NaN
            return NAN;
        }
        return log().multiply(x).exp();
    }

    /**
     * Returns of value of this complex number raised to the power of {@code x}.
     *
     * @param  x exponent to which this {@code Complex} is to be raised.
     * @return <code>this<sup>x</sup></code>.
     * @see #pow(Complex)
     */
    public Complex pow(double x) {
        if (real == 0 &&
            imaginary == 0) {
            // This value is zero. Test the other.
            if (x > 0) {
                // 0 raised to positive number is 0
                return ZERO;
            }
            // 0 raised to anything else is NaN
            return NAN;
        }
        return log().multiply(x).exp();
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/Sine.html">
     * sine</a>
     * of this complex number.
     * Implements the formula:
     * <pre>
     *   sin(a + bi) = sin(a)cosh(b) - cos(a)sinh(b)i
     * </pre>
     * where the (real) functions on the right-hand side are
     * {@link Math#sin}, {@link Math#cos},
     * {@link Math#cosh} and {@link Math#sinh}.
     *
     * <p>As per the C.99 standard this function is computed using the trigonomic identity:</p>
     * <pre>
     *   sin(z) = -i sinh(iz)
     * </pre>
     *
     * @return the sine of this complex number.
     */
    public Complex sin() {
        // Define in terms of sinh
        // sin(z) = -i sinh(iz)
        // Multiply this number by I, compute cosh, then multiply by back
        return sinh(-imaginary, real, Complex::multiplyNegativeI);
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/HyperbolicSine.html">
     * hyperbolic sine</a> of this complex number.
     * Implements the formula:
     * <pre>
     *   sinh(a + bi) = sinh(a)cos(b)) + cosh(a)sin(b)i
     * </pre>
     * where the (real) functions on the right-hand side are
     * {@link Math#sin}, {@link Math#cos},
     * {@link Math#cosh} and {@link Math#sinh}.
     *
     * @return the hyperbolic sine of {@code this}.
     */
    public Complex sinh() {
        return sinh(real, imaginary, Complex::ofCartesian);
    }

    /**
     * Compute the hyperbolic sine of the complex number.
     *
     * <p>This function exists to allow implementation of the identity
     * {@code sin(z) = -i sinh(iz)}.<p>
     *
     * @param real Real part.
     * @param imaginary Imaginary part.
     * @param constructor Constructor.
     * @return the hyperbolic sine of this complex number
     */
    private static Complex sinh(double real, double imaginary, ComplexConstructor constructor) {
        if (Double.isFinite(real)) {
            if (Double.isFinite(imaginary)) {
                return constructor.create(Math.sinh(real) * Math.cos(imaginary),
                                          Math.cosh(real) * Math.sin(imaginary));
            }
            // Special case for real == 0
            final double re = real == 0 ? real : Double.NaN;
            return constructor.create(re, Double.NaN);
        }
        if (Double.isInfinite(real)) {
            if (Double.isFinite(imaginary)) {
                if (imaginary == 0) {
                    return constructor.create(real, imaginary);
                }
                // inf * cis(y)
                final double re = real * Math.cos(imaginary);
                final double im = real * Math.sin(imaginary);
                return constructor.create(re, im);
            }
            // imaginary is infinite or NaN
            return constructor.create(Double.POSITIVE_INFINITY, Double.NaN);
        }
        // real is NaN
        if (imaginary == 0) {
            return constructor.create(Double.NaN, Math.copySign(0, imaginary));
        }
        // optionally raises the ‘‘invalid’’ floating-point exception, for nonzero y.
        return NAN;
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/SquareRoot.html">
     * square root</a> of this complex number.
     * Implements the following algorithm to compute {@code sqrt(a + bi)}:
     * <ol><li>Let {@code t = sqrt((|a| + |a + bi|) / 2)}</li>
     * <li><pre>if {@code  a &#8805; 0} return {@code t + (b/2t)i}
     *  else return {@code |b|/2t + sign(b)t i }</pre></li>
     * </ol>
     * where <ul>
     * <li>{@code |a| = }{@link Math#abs}(a)</li>
     * <li>{@code |a + bi| = }{@link Complex#abs}(a + bi)</li>
     * <li>{@code sign(b) =  }{@link Math#copySign(double,double) copySign(1d, b)}
     * </ul>
     *
     * @return the square root of {@code this}.
     */
    public Complex sqrt() {
        // Special case for infinite imaginary for all real including nan
        if (Double.isInfinite(imaginary)) {
            return new Complex(Double.POSITIVE_INFINITY, imaginary);
        }
        if (Double.isFinite(real)) {
            if (Double.isFinite(imaginary)) {
                // Handle zero
                if (real == 0 && imaginary == 0) {
                    return new Complex(0, imaginary);
                }
                final double t = Math.sqrt((Math.abs(real) + abs()) / 2);
                if (real >= 0) {
                    return new Complex(t, imaginary / (2 * t));
                }
                return new Complex(Math.abs(imaginary) / (2 * t),
                                   Math.copySign(1d, imaginary) * t);
            }
            // Imaginary is nan
            return NAN;
        }
        if (Double.isInfinite(real)) {
            // imaginary is finite or NaN
            final double part = Double.isNaN(imaginary) ? Double.NaN : 0;
            if (real == Double.NEGATIVE_INFINITY) {
                return new Complex(part, Math.copySign(Double.POSITIVE_INFINITY, imaginary));
            }
            return new Complex(Double.POSITIVE_INFINITY, Math.copySign(part, imaginary));
        }
        // real is NaN
        // optionally raises the ‘‘invalid’’ floating-point exception, for finite y.
        return NAN;
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/SquareRoot.html">
     * square root</a> of <code>1 - this<sup>2</sup></code> for this complex
     * number.
     *
     * <p>Computes the result directly as
     * {@code sqrt(ONE.subtract(z.multiply(z)))}.</p>
     *
     * @return the square root of <code>1 - this<sup>2</sup></code>.
     */
    private Complex sqrt1z() {
        return ONE.subtract(square()).sqrt();
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/Tangent.html">
     * tangent</a> of this complex number.
     * Implements the formula:
     * <pre>
     *   tan(a + bi) = sin(2a)/(cos(2a)+cosh(2b)) + [sinh(2b)/(cos(2a)+cosh(2b))]i
     * </pre>
     * where the (real) functions on the right-hand side are
     * {@link Math#sin}, {@link Math#cos}, {@link Math#cosh} and
     * {@link Math#sinh}.
     *
     * <p>As per the C.99 standard this function is computed using the trigonomic identity:</p>
     * <pre>
     *   tan(z) = -i tanh(iz)
     * </pre>
     *
     * @return the tangent of {@code this}.
     */
    public Complex tan() {
        // Define in terms of tanh
        // tan(z) = -i tanh(iz)
        // Multiply this number by I, compute cosh, then multiply by back
        return tanh(-imaginary, real, Complex::multiplyNegativeI);
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/HyperbolicTangent.html">
     * hyperbolic tangent</a> of this complex number.
     * Implements the formula:
     * <pre>
     *   tan(a + bi) = sinh(2a)/(cosh(2a)+cos(2b)) + [sin(2b)/(cosh(2a)+cos(2b))]i
     * </pre>
     * where the (real) functions on the right-hand side are
     * {@link Math#sin}, {@link Math#cos}, {@link Math#cosh} and
     * {@link Math#sinh}.
     *
     * @return the hyperbolic tangent of {@code this}.
     */
    public Complex tanh() {
        return tanh(real, imaginary, Complex::ofCartesian);
    }

    /**
     * Compute the hyperbolic tangent of this complex number.
     *
     * <p>This function exists to allow implementation of the identity
     * {@code tan(z) = -i tanh(iz)}.<p>
     *
     * @param real Real part.
     * @param imaginary Imaginary part.
     * @param constructor Constructor.
     * @return the hyperbolic tangent of this complex number
     */
    private static Complex tanh(double real, double imaginary, ComplexConstructor constructor) {
        // TODO: Should these checks be made on real2 and imaginary2?
        // Compare to other library implementations.
        //
        // Math.cos and Math.sin return NaN for infinity.
        // Math.cosh returns positive infinity for infinity.
        // Math.sinh returns the input infinity for infinity.

        if (Double.isFinite(real)) {
            if (Double.isFinite(imaginary)) {
                final double real2 = 2 * real;
                final double imaginary2 = 2 * imaginary;
                final double d = Math.cosh(real2) + Math.cos(imaginary2);

                return constructor.create(Math.sinh(real2) / d,
                                          Math.sin(imaginary2) / d);
            }
            return NAN;
        }
        if (Double.isInfinite(real)) {
            if (Double.isFinite(imaginary)) {
                return constructor.create(Math.copySign(1, real), Math.copySign(0, Math.sin(2 * imaginary)));
            }
            // imaginary is infinite or NaN
            return constructor.create(Math.copySign(1, real), Math.copySign(0, imaginary));
        }
        // real is NaN
        if (imaginary == 0) {
            return constructor.create(Double.NaN, Math.copySign(0, imaginary));
        }
        // optionally raises the ‘‘invalid’’ floating-point exception, for nonzero y.
        return NAN;
    }

   /**
     * Compute the argument of this complex number.
     * The argument is the angle phi between the positive real axis and
     * the point representing this number in the complex plane.
     * The value returned is between -PI (not inclusive)
     * and PI (inclusive), with negative values returned for numbers with
     * negative imaginary parts.
     *
     * <p>If either real or imaginary part (or both) is NaN, NaN is returned.
     * Infinite parts are handled as {@code Math.atan2} handles them,
     * essentially treating finite parts as zero in the presence of an
     * infinite coordinate and returning a multiple of pi/4 depending on
     * the signs of the infinite parts.
     * See the javadoc for {@code Math.atan2} for full details.</p>
     *
     * @return the argument of {@code this}.
     */
    public double getArgument() {
        // Delegate
        return Math.atan2(imaginary, real);
    }

    /**
     * Compute the argument of this complex number
     * (C++11 grammar).
     *
     * @return the argument of {@code this}.
     * @see #getArgument()
     */
    public double arg() {
        return getArgument();
    }

    /**
     * Computes the n-th roots of this complex number.
     * The nth roots are defined by the formula:
     * <pre>
     * <code>
     *   z<sub>k</sub> = abs<sup>1/n</sup> (cos(phi + 2&pi;k/n) + i (sin(phi + 2&pi;k/n))
     * </code>
     * </pre>
     * for <i>{@code k=0, 1, ..., n-1}</i>, where {@code abs} and {@code phi}
     * are respectively the {@link #abs() modulus} and
     * {@link #getArgument() argument} of this complex number.
     *
     * <p>If one or both parts of this complex number is NaN, a list with all
     * all elements set to {@code NaN + NaN i} is returned.</p>
     *
     * @param n Degree of root.
     * @return a List of all {@code n}-th roots of {@code this}.
     * @throws IllegalArgumentException if {@code n} is zero.
     */
    public List<Complex> nthRoot(int n) {
        if (n == 0) {
            throw new IllegalArgumentException("cannot compute zeroth root");
        }

        final List<Complex> result = new ArrayList<>();

        // nth root of abs -- faster / more accurate to use a solver here?
        final double nthRootOfAbs = Math.pow(abs(), 1d / n);

        // Compute nth roots of complex number with k = 0, 1, ... n-1
        final double nthPhi = getArgument() / n;
        final double slice = 2 * Math.PI / n;
        double innerPart = nthPhi;
        for (int k = 0; k < Math.abs(n); k++) {
            // inner part
            final double realPart = nthRootOfAbs *  Math.cos(innerPart);
            final double imaginaryPart = nthRootOfAbs *  Math.sin(innerPart);
            result.add(new Complex(realPart, imaginaryPart));
            innerPart += slice;
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder();
        s.append(FORMAT_START)
            .append(real).append(FORMAT_SEP)
            .append(imaginary)
            .append(FORMAT_END);

        return s.toString();
    }

    /**
     * Returns {@code true} if the values are equal according to semantics of
     * {@link Double#equals(Object)}.
     *
     * @param x Value
     * @param y Value
     * @return {@code Double.valueof(x).equals(Double.valueOf(y))}
     */
    private static boolean equals(double x, double y) {
        return Double.doubleToLongBits(x) == Double.doubleToLongBits(y);
    }

    /**
     * Check that a value is negative. It must meet all the following conditions:
     * <ul>
     *  <li>it is not {@code NaN},</li>
     *  <li>it is negative signed,</li>
     * </ul>
     *
     * <p>Note: This is true for negative zero.</p>
     *
     * @param d Value.
     * @return {@code true} if {@code d} is negative.
     */
    private static boolean negative(double d) {
        return d < 0 || equals(d, -0.0);
    }

    /**
     * Create a complex number given the real and imaginary parts, then multiply by {@code -i}.
     * This is used in functions that implement trigonomic identities. It is the functional
     * equivalent of:
     *
     * <pre>
     *   z = new Complex(real, imaginary).multiply(new Complex(0, -1));
     * </pre>
     *
     * @param real Real part.
     * @param imaginary Imaginary part.
     * @return {@code Complex} object
     */
    private static Complex multiplyNegativeI(double real, double imaginary) {
        return new Complex(imaginary, -real);
    }

    /**
     * Create the conjugate of a complex number given the real and imaginary parts.
     * This is used in functions that implement conjugate identities. It is the functional
     * equivalent of:
     *
     * <pre>
     *   z = new Complex(real, imaginary).conjugate();
     * </pre>
     *
     * @param real Real part.
     * @param imaginary Imaginary part.
     * @return {@code Complex} object
     */
    private static Complex ofCartesianConjugate(double real, double imaginary) {
        return new Complex(real, -imaginary);
    }

     /** See {@link #parse(String)}. */
    private static class ComplexParsingException extends NumberFormatException {
        /** Serializable version identifier. */
        private static final long serialVersionUID = 20180430L;

        /**
         * @param msg Error message.
         */
        ComplexParsingException(String msg) {
            super(msg);
        }

        /**
         * @param msg Error message.
         * @param cause Cause.
         */
        ComplexParsingException(String msg, Throwable cause) {
            super(msg);
            initCause(cause);
        }
    }
}
