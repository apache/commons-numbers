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
 * Cartesian representation of a Complex number, i.e. a number which has both a
 * real and imaginary part.
 *
 * <p>This class is immutable. All arithmetic will create a new instance for the
 * result.</p>
 *
 * <p>Arithmetic in this class conforms to the C.99 standard for complex numbers
 * defined in ISO/IEC 9899, Annex G. All methods have been named using the equivalent
 * method in ISO C.99.</p>
 *
 * <p>Operations ({@code op}) with no arguments obey the conjuagte equality:</p>
 * <pre>z.op().conjugate() == z.conjugate().op()</pre>
 *
 * <p>Operations that are odd or even obey the equality:</p>
 * <pre>
 * Odd:  f(z) = -f(-z)
 * Even: f(z) =  f(-z)
 * </pre>
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
    /** Mask an integer number to even by discarding the lowest bit. */
    private static final int MASK_INT_TO_EVEN = ~0x1;

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
     * Define a unary operation on a double.
     * This is used in the log() and log10() functions.
     */
    @FunctionalInterface
    private interface UnaryOperation {
        /**
         * Apply an operation to a value.
         *
         * @param value The value.
         * @return The result.
         */
        double apply(double value);
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
     * ofPolar(INFINITY, \(\pi\)) = INFINITY + INFINITY i
     * ofPolar(INFINITY, 0) = INFINITY + NaN i
     * ofPolar(INFINITY, \(-\frac{\pi}{4}\)) = INFINITY - INFINITY i
     * ofPolar(INFINITY, \(5\frac{\pi}{4}\)) = -INFINITY - INFINITY i }
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
            throw parsingException("Expected start string", FORMAT_START, null);
        }
        final int len = s.length();

        final int endParen = s.indexOf(FORMAT_END);
        if (endParen != len - 1) {
            throw parsingException("Expected end string", FORMAT_END, null);
        }

        final String[] elements = s.substring(1, s.length() - 1).split(FORMAT_SEP);
        if (elements.length != TWO_ELEMENTS) {
            throw parsingException("Incorrect number of parts: Expected 2 but was " + elements.length,
                                   "separator is '" + FORMAT_SEP + "'", null);
        }

        final double re;
        try {
            re = Double.parseDouble(elements[0]);
        } catch (final NumberFormatException ex) {
            throw parsingException("Could not parse real part", elements[0], ex);
        }
        final double im;
        try {
            im = Double.parseDouble(elements[1]);
        } catch (final NumberFormatException ex) {
            throw parsingException("Could not parse imaginary part", elements[1], ex);
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
     * Returns projection of this complex number onto the Riemann sphere.
     *
     * <p>{@code z} projects to {@code z}, except that all complex infinities (even those
     * with one infinite part and one NaN part) project to positive infinity on the real axis.
     *
     * If {@code z} has an infinite part, then {@code z.proj()} shall be equivalent to:</p>
     * <pre>
     *   return Complex.ofCartesian(Double.POSITIVE_INFINITY, Math.copySign(0.0, imag());
     * </pre>
     *
     * @return {@code z} projected onto the Riemann sphere.
     * @see #isInfinite()
     * @see <a href="http://pubs.opengroup.org/onlinepubs/9699919799/functions/cproj.html">
     * IEEE and ISO C standards: cproj</a>
     */
    public Complex proj() {
        if (isInfinite()) {
            return new Complex(Double.POSITIVE_INFINITY, Math.copySign(0.0, imaginary));
        }
        return this;
    }

    /**
     * Return the absolute value of this complex number. This is also called norm, modulus,
     * or magnitude.
     * <pre>abs(a + b i) = sqrt(a^2 + b^2)</pre>
     *
     * <p>If either component is infinite then the result is positive infinity. If either
     * component is NaN and this is not {@link #isInfinite() infinite} then the result is NaN.
     *
     * <p>This code follows the
     * <a href="http://www.iso-9899.info/wiki/The_Standard">ISO C Standard</a>, Annex G,
     * in calculating the returned value using the {@code hypot(a, b)} method for complex
     * {@code a + b i}.
     *
     * @return the absolute value.
     * @see #isInfinite()
     * @see #isNaN()
     * @see Math#hypot(double, double)
     */
    public double abs() {
        // Delegate
        return Math.hypot(real, imaginary);
    }

    /**
     * Compute the absolute of the complex number.
     *
     * <p>This function exists for use in trigonomic functions.
     *
     * @param real Real part.
     * @param imaginary Imaginary part.
     * @return the absolute value.
     * @see Math#hypot(double, double)
     */
    private static double getAbsolute(double real, double imaginary) {
        // Delegate
        return Math.hypot(real, imaginary);
    }

    /**
     * Returns a {@code Complex} whose value is {@code (this + addend)}.
     * Implements the formula:
     * <pre>
     *   (a + i b) + (c + i d) = (a + c) + i (b + d)
     * </pre>
     *
     * @param  addend Value to be added to this {@code Complex}.
     * @return {@code this + addend}.
     * @see <a href="http://mathworld.wolfram.com/ComplexAddition.html">Complex Addition</a>
     */
    public Complex add(Complex addend) {
        return new Complex(real + addend.real,
                           imaginary + addend.imaginary);
    }

    /**
     * Returns a {@code Complex} whose value is {@code (this + addend)},
     * with {@code addend} interpreted as a real number.
     * Implements the formula:
     * <pre>
     *  (a + i b) + c = (a + c) + i b
     * </pre>
     *
     * <p>This method is included for compatibility with ISO C99 which defines arithmetic between
     * real-only and complex numbers.</p>
     *
     * <p>Note: This method preserves the sign of the imaginary component {@code b} if it is {@code -0.0}.
     * The sign would be lost if adding {@code (c + i 0)} using
     * {@link #add(Complex) add(Complex.ofReal(addend))} since
     * {@code -0.0 + 0.0 = 0.0}.
     *
     * @param addend Value to be added to this {@code Complex}.
     * @return {@code this + addend}.
     * @see #add(Complex)
     * @see #ofReal(double)
     */
    public Complex add(double addend) {
        return new Complex(real + addend, imaginary);
    }

    /**
     * Returns a {@code Complex} whose value is {@code (this + addend)},
     * with {@code addend} interpreted as an imaginary number.
     * Implements the formula:
     * <pre>
     *  (a + i b) + i d = a + i (b + d)
     * </pre>
     *
     * <p>This method is included for compatibility with ISO C99 which defines arithmetic between
     * imaginary-only and complex numbers.</p>
     *
     * <p>Note: This method preserves the sign of the real component {@code a} if it is {@code -0.0}.
     * The sign would be lost if adding {@code (0 + i d)} using
     * {@link #add(Complex) add(Complex.ofCartesian(0, addend))} since
     * {@code -0.0 + 0.0 = 0.0}.
     *
     * @param addend Value to be added to this {@code Complex}.
     * @return {@code this + addend}.
     * @see #add(Complex)
     * @see #ofCartesian(double, double)
     */
    public Complex addImaginary(double addend) {
        return new Complex(real, imaginary + addend);
    }

    /**
     * Returns the
     * <a href="http://mathworld.wolfram.com/ComplexConjugate.html">conjugate</a>
     * z&#773; of this complex number z.
     * <pre>
     *  z = a + b i
     *
     *  z&#773; = a - b i
     * </pre>
     *
     * @return the conjugate (z&#773;) of this complex object.
     */
    public Complex conj() {
        return new Complex(real, -imaginary);
    }

    /**
     * Returns a {@code Complex} whose value is
     * {@code (this / divisor)}.
     * Implements the formula:
     * <pre>
     * <code>
     *   a + b i     ac + bd + i (bc - ad)
     *   -------  =  ---------------------
     *   c + d i           c<sup>2</sup> + d<sup>2</sup>
     * </code>
     * </pre>
     *
     * <p>Recalculates to recover infinities as specified in C.99
     * standard G.5.1. Method is fully in accordance with
     * C++11 standards for complex numbers.</p>
     *
     * @param divisor Value by which this {@code Complex} is to be divided.
     * @return {@code this / divisor}.
     * @see <a href="http://mathworld.wolfram.com/ComplexDivision.html">Complex Division</a>
     */
    public Complex divide(Complex divisor) {
        return divide(real, imaginary, divisor.real, divisor.imaginary);
    }

    /**
     * Returns a {@code Complex} whose value is:
     * <pre>
     * <code>
     *   a + b i     ac + bd + i (bc - ad)
     *   -------  =  ---------------------
     *   c + d i           c<sup>2</sup> + d<sup>2</sup>
     * </code>
     * </pre>
     *
     * <p>Recalculates to recover infinities as specified in C.99
     * standard G.5.1. Method is fully in accordance with
     * C++11 standards for complex numbers.</p>
     *
     * @param re1 Real component of first number.
     * @param im1 Imaginary component of first number.
     * @param re2 Real component of second number.
     * @param im2 Imaginary component of second number.
     * @return (a + b i) / (c + d i).
     * @see <a href="http://mathworld.wolfram.com/ComplexDivision.html">Complex Division</a>
     */
    private static Complex divide(double re1, double im1, double re2, double im2) {
        double a = re1;
        double b = im1;
        double c = re2;
        double d = im2;
        int ilogbw = 0;
        // Get the exponent to scale the divisor.
        // This is equivalent to (int) Math.log2(double).
        final int exponent = Math.getExponent(Math.max(Math.abs(c), Math.abs(d)));
        if (exponent <= Double.MAX_EXPONENT) {
            ilogbw = exponent;
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
            } else if ((Double.isInfinite(c) || Double.isInfinite(d)) &&
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
            bc * 0.5 - ad * 0.5;
    }

    /**
     * Returns a {@code Complex} whose value is {@code (this / divisor)},
     * with {@code divisor} interpreted as a real number.
     * Implements the formula:
     * <pre>
     *   (a + b i) / c = (a + b i) / (c + 0 i)
     * </pre>
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
            final double scale = 1.0 / (real * q + imaginary);
            double scaleQ = 0;
            if (q != 0 &&
                scale != 0) {
                scaleQ = scale * q;
            }
            return new Complex(scaleQ, -scale);
        }
        final double q = imaginary / real;
        final double scale = 1.0 / (imaginary * q + real);
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
     *  {@code Arrays.hashCode(new double[] {getReal(), getImaginary()})}
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
     * @see #getImaginary()
     */
    public double imag() {
        return getImaginary();
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
     * @see #getReal()
     */
    public double real() {
        return getReal();
    }

    /**
     * Returns a {@code Complex} whose value is {@code this * factor}.
     * Implements the formula:
     * <pre>
     *   (a + b i)(c + d i) = (ac - bd) + i (ad + bc)
     * </pre>
     *
     * <p>Recalculates to recover infinities as specified in C.99
     * standard G.5.1. Method is fully in accordance with
     * C++11 standards for complex numbers.</p>
     *
     * @param  factor value to be multiplied by this {@code Complex}.
     * @return {@code this * factor}.
     * @see <a href="http://mathworld.wolfram.com/ComplexMultiplication.html">Complex Muliplication</a>
     */
    public Complex multiply(Complex factor) {
        return multiply(real, imaginary, factor.real, factor.imaginary);
    }

    /**
     * Returns a {@code Complex} whose value is:
     * <pre>
     *   (a + b i)(c + d i) = (ac - bd) + i (ad + bc)
     * </pre>
     *
     * <p>Recalculates to recover infinities as specified in C.99
     * standard G.5.1. Method is fully in accordance with
     * C++11 standards for complex numbers.</p>
     *
     * @param re1 Real component of first number.
     * @param im1 Imaginary component of first number.
     * @param re2 Real component of second number.
     * @param im2 Imaginary component of second number.
     * @return (a + b i)(c + d i).
     */
    private static Complex multiply(double re1, double im1, double re2, double im2) {
        double a = re1;
        double b = im1;
        double c = re2;
        double d = im2;
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
            if ((Double.isInfinite(a) || Double.isInfinite(b)) &&
                isNotZero(c, d)) {
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
     * interpreted as a real number.
     * Implements the formula:
     * <pre>
     *   (a + b i) c = (a + b i)(c + 0 i)
     *               = ac + bc i
     * </pre>
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
     * Returns a {@code Complex} whose value is {@code (this - subtrahend)}.
     * Implements the formula:
     * <pre>
     *  (a + i b) - (c + i d) = (a - c) + i (b - d)
     * </pre>
     *
     * @param  subtrahend value to be subtracted from this {@code Complex}.
     * @return {@code this - subtrahend}.
     * @see <a href="http://mathworld.wolfram.com/ComplexSubtraction.html">Complex Subtraction</a>
     */
    public Complex subtract(Complex subtrahend) {
        return new Complex(real - subtrahend.real,
                           imaginary - subtrahend.imaginary);
    }

    /**
     * Returns a {@code Complex} whose value is {@code (this - subtrahend)},
     * with {@code subtrahend} interpreted as a real number.
     * Implements the formula:
     * <pre>
     *  (a + i b) - c = (a - c) + i b
     * </pre>
     *
     * <p>This method is included for compatibility with ISO C99 which defines arithmetic between
     * real-only and complex numbers.</p>
     *
     * @param  subtrahend value to be subtracted from this {@code Complex}.
     * @return {@code this - subtrahend}.
     * @see #subtract(Complex)
     */
    public Complex subtract(double subtrahend) {
        return new Complex(real - subtrahend, imaginary);
    }

    /**
     * Returns a {@code Complex} whose value is {@code (this - subtrahend)},
     * with {@code subtrahend} interpreted as an imaginary number.
     * Implements the formula:
     * <pre>
     *  (a + i b) - i d = a + i (b - d)
     * </pre>
     *
     * <p>This method is included for compatibility with ISO C99 which defines arithmetic between
     * imaginary-only and complex numbers.</p>
     *
     * @param  subtrahend value to be subtracted from this {@code Complex}.
     * @return {@code this - subtrahend}.
     * @see #subtract(Complex)
     */
    public Complex subtractImaginary(double subtrahend) {
        return new Complex(real, imaginary - subtrahend);
    }

    /**
     * Returns a {@code Complex} whose value is {@code (minuend - this)},
     * with {@code minuend} interpreted as a real number.
     * Implements the formula:
     * <pre>
     *  c - (a + i b) = (c - a) - i b
     * </pre>
     *
     * <p>This method is included for compatibility with ISO C99 which defines arithmetic between
     * real-only and complex numbers.</p>
     *
     * <p>Note: This method inverts the sign of the imaginary component {@code b} if it is {@code 0.0}.
     * The sign would not be inverted if subtracting from {@code (c + i 0)} using
     * {@link #subtract(Complex) Complex.ofReal(minuend).subtract(this))} since
     * {@code 0.0 - 0.0 = 0.0}.
     *
     * @param  minuend value this {@code Complex} is to be subtracted from.
     * @return {@code minuend - this}.
     * @see #subtract(Complex)
     * @see #ofReal(double)
     */
    public Complex subtractFrom(double minuend) {
        return new Complex(minuend - real, -imaginary);
    }

    /**
     * Returns a {@code Complex} whose value is {@code (this - subtrahend)},
     * with {@code minuend} interpreted as an imaginary number.
     * Implements the formula:
     * <pre>
     *  i d - (a + i b) = -a + i (d - b)
     * </pre>
     *
     * <p>This method is included for compatibility with ISO C99 which defines arithmetic between
     * imaginary-only and complex numbers.</p>
     *
     * <p>Note: This method inverts the sign of the real component {@code a} if it is {@code 0.0}.
     * The sign would not be inverted if subtracting from {@code (0 + i d)} using
     * {@link #subtract(Complex) Complex.ofCartesian(0, minuend).subtract(this))} since
     * {@code 0.0 - 0.0 = 0.0}.
     *
     * @param  minuend value this {@code Complex} is to be subtracted from.
     * @return {@code this - subtrahend}.
     * @see #subtract(Complex)
     * @see #ofCartesian(double, double)
     */
    public Complex subtractFromImaginary(double minuend) {
        return new Complex(-real, minuend - imaginary);
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/InverseCosine.html">
     * inverse cosine</a> of this complex number.
     * Implements the formula:
     * <pre>
     * <code>
     *   acos(z) = (pi / 2) + i ln(iz + sqrt(1 - z<sup>2</sup>))
     * </code>
     * </pre>
     *
     * @return the inverse cosine of this complex number.
     */
    public Complex acos() {
        return acos(real, imaginary, Complex::ofCartesian);
    }

    /**
     * Compute the inverse cosine of the complex number.
     *
     * <p>This function exists to allow implementation of the identity
     * {@code acosh(z) = +-i acos(z)}.<p>
     *
     * @param real Real part.
     * @param imaginary Imaginary part.
     * @param constructor Constructor.
     * @return the inverse cosine of the complex number.
     */
    private static Complex acos(double real, double imaginary, ComplexConstructor constructor) {
        if (Double.isFinite(real)) {
            if (Double.isFinite(imaginary)) {
                // Special case for real numbers
                if (imaginary == 0 && Math.abs(real) <= 1) {
                    return constructor.create(real == 0.0 ? PI_OVER_2 : Math.acos(real),
                                              Math.copySign(0, -imaginary));
                }
                // ISO C99: Preserve the equality
                // acos(conj(z)) = conj(acos(z))
                // by always computing on a positive imaginary Complex number.
                final double a = real;
                final double b = Math.abs(imaginary);
                final Complex z2 = multiply(a, b, a, b);
                // sqrt(1 - z^2)
                final Complex sqrt1mz2 = sqrt(1 - z2.real, -z2.imaginary);
                // Compute the rest inline to avoid Complex object creation.
                // (x + y i) = iz + sqrt(1 - z^2)
                final double x = -b + sqrt1mz2.real;
                final double y = a + sqrt1mz2.imaginary;
                // (re + im i) = (pi / 2) + i ln(iz + sqrt(1 - z^2))
                final double re = PI_OVER_2 - getArgument(x, y);
                final double im = Math.log(getAbsolute(x, y));
                // Map back to the correct sign
                return constructor.create(re, changeSign(im, imaginary));
            }
            if (Double.isInfinite(imaginary)) {
                return constructor.create(PI_OVER_2, Math.copySign(Double.POSITIVE_INFINITY, -imaginary));
            }
            // imaginary is NaN
            // Special case for real == 0
            return real == 0 ? constructor.create(PI_OVER_2, Double.NaN) : NAN;
        }
        if (Double.isInfinite(real)) {
            if (Double.isFinite(imaginary)) {
                final double re = real == Double.NEGATIVE_INFINITY ? Math.PI : 0;
                return constructor.create(re, Math.copySign(Double.POSITIVE_INFINITY, -imaginary));
            }
            if (Double.isInfinite(imaginary)) {
                final double re = real == Double.NEGATIVE_INFINITY ? PI_3_OVER_4 : PI_OVER_4;
                return constructor.create(re, Math.copySign(Double.POSITIVE_INFINITY, -imaginary));
            }
            // imaginary is NaN
            // Swap real and imaginary
            return constructor.create(Double.NaN, real);
        }
        // real is NaN
        if (Double.isInfinite(imaginary)) {
            return constructor.create(Double.NaN, -imaginary);
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
     *   asin(z) = -i (ln(iz + sqrt(1 - z<sup>2</sup>)))
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
        // Multiply this number by I, compute asinh, then multiply by back
        return asinh(-imaginary, real, Complex::multiplyNegativeI);
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/InverseTangent.html">
     * inverse tangent</a> of this complex number.
     * <pre>
     *   atan(z) = (i / 2) ln((i + z) / (i - z))
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
        // Multiply this number by I, compute atanh, then multiply by back
        return atanh(-imaginary, real, Complex::multiplyNegativeI);
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/InverseHyperbolicSine.html">
     * inverse hyperbolic sine</a> of this complex number.
     * Implements the formula:
     * <pre>
     * <code>
     *   asinh(z) = ln(z + sqrt(1 + z<sup>2</sup>))
     * </code>
     * </pre>
     *
     * <p>This is an odd function: {@code f(z) = -f(-z)}.
     *
     * @return the inverse hyperbolic sine of this complex number
     */
    public Complex asinh() {
        return asinh(real, imaginary, Complex::ofCartesian);
    }

    /**
     * Compute the inverse hyperbolic sine of the complex number.
     *
     * <p>This function exists to allow implementation of the identity
     * {@code sin(z) = -i sinh(iz)}.<p>
     *
     * @param real Real part.
     * @param imaginary Imaginary part.
     * @param constructor Constructor.
     * @return the inverse hyperbolic sine of the complex number
     */
    private static Complex asinh(double real, double imaginary, ComplexConstructor constructor) {
        if (Double.isFinite(real)) {
            if (Double.isFinite(imaginary)) {
                // ISO C99: Preserve the equality
                // asinh(conj(z)) = conj(asinh(z))
                // and the odd function: f(z) = -f(-z)
                // by always computing on a positive valued Complex number.
                final double a = Math.abs(real);
                final double b = Math.abs(imaginary);
                // C99. G.7: Special case for imaginary only numbers
                if (a == 0 && b <= 1.0) {
                    if (imaginary == 0) {
                        return constructor.create(real, imaginary);
                    }
                    // asinh(iy) = i asin(y)
                    final double im = Math.asin(imaginary);
                    return constructor.create(real, im);
                }
                // square() is implemented using multiply
                final Complex z2 = multiply(a, b, a, b);
                // sqrt(1 + z^2)
                final Complex sqrt1pz2 = sqrt(1 + z2.real, z2.imaginary);
                // Compute the rest inline to avoid Complex object creation.
                // (x + y i) = z + sqrt(1 + z^2)
                final double x = a + sqrt1pz2.real;
                final double y = b + sqrt1pz2.imaginary;
                // (re + im i) = ln(z + sqrt(1 + z^2))
                final double re = Math.log(getAbsolute(x, y));
                final double im = getArgument(x, y);
                // Map back to the correct sign
                return constructor.create(changeSign(re, real),
                                          changeSign(im, imaginary));
            }
            if (Double.isInfinite(imaginary)) {
                return constructor.create(Math.copySign(Double.POSITIVE_INFINITY, real),
                                          Math.copySign(PI_OVER_2, imaginary));
            }
            // imaginary is NaN
            return NAN;
        }
        if (Double.isInfinite(real)) {
            if (Double.isFinite(imaginary)) {
                return constructor.create(real, Math.copySign(0, imaginary));
            }
            if (Double.isInfinite(imaginary)) {
                return constructor.create(real, Math.copySign(PI_OVER_4, imaginary));
            }
            // imaginary is NaN
            return constructor.create(real, Double.NaN);
        }
        // real is NaN
        if (imaginary == 0) {
            return constructor.create(Double.NaN, Math.copySign(0, imaginary));
        }
        if (Double.isInfinite(imaginary)) {
            return constructor.create(Double.POSITIVE_INFINITY, Double.NaN);
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
     *   atanh(z) = (1/2) ln((1 + z) / (1 - z))
     * </pre>
     *
     * <p>This is an odd function: {@code f(z) = -f(-z)}.
     *
     * @return the inverse hyperbolic tangent of this complex number
     */
    public Complex atanh() {
        return atanh(real, imaginary, Complex::ofCartesian);
    }

    /**
     * Compute the inverse hyperbolic tangent of this complex number.
     *
     * <p>This function exists to allow implementation of the identity
     * {@code sin(z) = -i sinh(iz)}.<p>
     *
     * @param real Real part.
     * @param imaginary Imaginary part.
     * @param constructor Constructor.
     * @return the inverse hyperbolic tangent of the complex number
     */
    private static Complex atanh(double real, double imaginary, ComplexConstructor constructor) {
        if (Double.isFinite(real)) {
            if (Double.isFinite(imaginary)) {
                // ISO C99: Preserve the equality
                // atanh(conj(z)) = conj(atanh(z))
                // and the odd function: f(z) = -f(-z)
                // by always computing on a positive valued Complex number.
                final double a = Math.abs(real);
                final double b = Math.abs(imaginary);
                // Special case for divide-by-zero
                if (a == 1 && b == 0) {
                    // raises the ‘‘divide-by-zero’’ floating-point exception.
                    return constructor.create(Math.copySign(Double.POSITIVE_INFINITY, real), imaginary);
                }
                // C99. G.7: Special case for imaginary only numbers
                if (a == 0) {
                    if (imaginary == 0) {
                        return constructor.create(real, imaginary);
                    }
                    // atanh(iy) = i atan(y)
                    final double im = Math.atan(imaginary);
                    return constructor.create(real, im);
                }
                // (1 + (a + b i)) / (1 - (a + b i))
                final Complex result = divide(1 + a, b, 1 - a, -b);
                // Compute the log:
                // (re + im i) = (1/2) * ln((1 + z) / (1 - z))
                return result.log(Math::log, (re, im) ->
                   // Divide log() by 2 and map back to the correct sign
                   constructor.create(0.5 * changeSign(re, real),
                                      0.5 * changeSign(im, imaginary))
                );
            }
            if (Double.isInfinite(imaginary)) {
                return constructor.create(Math.copySign(0, real), Math.copySign(PI_OVER_2, imaginary));
            }
            // imaginary is NaN
            // Special case for real == 0
            return real == 0 ? constructor.create(real, Double.NaN) : NAN;
        }
        if (Double.isInfinite(real)) {
            if (Double.isNaN(imaginary)) {
                return constructor.create(Math.copySign(0, real), Double.NaN);
            }
            // imaginary is finite or infinite
            return constructor.create(Math.copySign(0, real), Math.copySign(PI_OVER_2, imaginary));
        }
        // real is NaN
        if (Double.isInfinite(imaginary)) {
            return constructor.create(0, Math.copySign(PI_OVER_2, imaginary));
        }
        // optionally raises the ‘‘invalid’’ floating-point exception, for finite y.
        return NAN;
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/InverseHyperbolicCosine.html">
     * inverse hyperbolic cosine</a> of this complex number.
     * <pre>
     *   acosh(z) = ln(z + sqrt(z + 1) sqrt(z - 1))
     * </pre>
     *
     * <p>This function is computed using the trigonomic identity:</p>
     * <pre>
     *   acosh(z) = +-i acos(z)
     * </pre>
     *
     * <p>The sign of the multiplier is chosen to give {@code z.acosh().real() >= 0}
     * and compatibility with the C.99 standard.</p>
     *
     * @return the inverse hyperbolic cosine of this complex number
     */
    public Complex acosh() {
        // Define in terms of acos
        // acosh(z) = +-i acos(z)
        // Handle special case:
        // acos(+-0 + iNaN) = π/2 + iNaN
        // acosh(x + iNaN) = NaN + iNaN for all finite x (including zero)
        if (Double.isNaN(imaginary) && Double.isFinite(real)) {
            return NAN;
        }
        // ISO C99: Preserve the equality
        // acos(conj(z)) = conj(acos(z))
        // by always computing on a positive imaginary Complex number.
        return acos(real, Math.abs(imaginary), (re, im) ->
            // Set the sign appropriately for C99 equalities.
            (im > 0) ?
                // Multiply by -I and map back to the correct sign
                new Complex(im, changeSign(-re, imaginary)) :
                // Multiply by I
                new Complex(-im, changeSign(re, imaginary))
        );
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
     *   cos(a + b i) = cos(a)*cosh(b) - i sin(a)*sinh(b)
     * </pre>
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
     *   cosh(a + b i) = cosh(a)cos(b) + i sinh(a)sin(b)
     * </pre>
     *
     * <p>This is an even function: {@code f(z) = f(-z)}.
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
     * @return the hyperbolic cosine of the complex number
     */
    private static Complex cosh(double real, double imaginary, ComplexConstructor constructor) {
        if (Double.isFinite(real)) {
            if (Double.isFinite(imaginary)) {
                return constructor.create(Math.cosh(real) * Math.cos(imaginary),
                                          Math.sinh(real) * Math.sin(imaginary));
            }
            // ISO C99: Preserve the even function by mapping to positive
            // f(z) = f(-z)
            double re;
            double im;
            if (negative(real)) {
                re = -real;
                im = -imaginary;
            } else {
                re = real;
                im = imaginary;
            }
            // Special case for real == 0
            return constructor.create(Double.NaN,
                                      re == 0 ? Math.copySign(0, im) : Double.NaN);
        }
        if (Double.isInfinite(real)) {
            if (Double.isFinite(imaginary)) {
                if (imaginary == 0) {
                    // Determine sign
                    final double im = real > 0 ? imaginary : -imaginary;
                    return constructor.create(Double.POSITIVE_INFINITY, im);
                }
                // inf * cis(y)
                // ISO C99: Preserve the even function
                // f(z) = f(-z)
                double re;
                double im;
                if (real < 0) {
                    re = -real;
                    im = -imaginary;
                } else {
                    re = real;
                    im = imaginary;
                }
                return constructor.create(re * Math.cos(im), re * Math.sin(im));
            }
            // imaginary is infinite or NaN
            return constructor.create(Double.POSITIVE_INFINITY, Double.NaN);
        }
        // real is NaN
        if (imaginary == 0) {
            return constructor.create(Double.NaN, imaginary);
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
     *   exp(a + b i) = exp(a) (cos(b) + i sin(b))
     * </pre>
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
     *   ln(a + b i) = ln(|a + b i|) + i arg(a + b i)
     * </pre>
     *
     * @return the natural logarithm of {@code this}.
     * @see Math#log(double)
     * @see #abs()
     * @see #arg()
     */
    public Complex log() {
        return log(Math::log, Complex::ofCartesian);
    }

    /**
     * Compute the base 10
     * <a href="http://mathworld.wolfram.com/CommonLogarithm.html">
     * common logarithm</a> of this complex number.
     * Implements the formula:
     * <pre>
     *   log10(a +  bi) = log10(|a + b i|) + i arg(a + b i)
     * </pre>
     *
     * @return the base 10 logarithm of {@code this}.
     * @see Math#log10(double)
     * @see #abs()
     * @see #arg()
     */
    public Complex log10() {
        return log(Math::log10, Complex::ofCartesian);
    }

    /**
     * Compute the logarithm of this complex number using the provided function.
     * Implements the formula:
     * <pre>
     *   log(a +  bi) = log(|a + b i|) + i arg(a + b i)
     * </pre>
     *
     * @param log Log function.
     * @param constructor Constructor for the returned complex.
     * @return the logarithm of {@code this}.
     * @see #abs()
     * @see #arg()
     */
    private Complex log(UnaryOperation log, ComplexConstructor constructor) {
        // All ISO C99 edge cases satisfied by the Math library.
        // Make computation overflow safe.
        final double abs = abs();
        if (abs == Double.POSITIVE_INFINITY && isFinite()) {
            // Edge-case where the |a + b i| overflows.
            // |a + b i| = sqrt(a^2 + b^2)
            // This can be scaled linearly.
            // Scale the absolute and exploit:
            // ln(abs / scale) = ln(abs) - ln(scale)
            // ln(abs) = ln(abs / scale) + ln(scale)
            // Use precise scaling with:
            // scale ~ 2^exponent
            final double scale = Math.max(Math.abs(real), Math.abs(imaginary));
            final int exponent = Math.getExponent(scale);
            // Implement scaling using 2^-exponent
            final double absOs = Math.hypot(Math.scalb(real, -exponent), Math.scalb(imaginary, -exponent));
            // log(2^exponent) = ln2(2^exponent) * log(2)
            return constructor.create(log.apply(absOs) + exponent * log.apply(2), arg());
        }
        return constructor.create(log.apply(abs), arg());
    }

    /**
     * Returns of value of this complex number raised to the power of {@code x}.
     * Implements the formula:
     * <pre>
     * <code>
     *   y<sup>x</sup> = exp(x&middot;log(y))
     * </code>
     * </pre>
     *
     * <p>If this Complex is zero then this method returns zero if {@code x} is positive
     * in the real component and zero in the imaginary component;
     * otherwise it returns (NaN + i NaN).
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
     * Implements the formula:
     * <pre>
     * <code>
     *   y<sup>x</sup> = exp(x&middot;log(y))
     * </code>
     * </pre>
     *
     * <p>If this Complex is zero then this method returns zero if {@code x} is positive;
     * otherwise it returns (NaN + i NaN).
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
     * sine</a> of this complex number.
     * Implements the formula:
     * <pre>
     *   sin(a + b i) = sin(a)cosh(b) - i cos(a)sinh(b)
     * </pre>
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
        // Multiply this number by I, compute sinh, then multiply by back
        return sinh(-imaginary, real, Complex::multiplyNegativeI);
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/HyperbolicSine.html">
     * hyperbolic sine</a> of this complex number.
     * Implements the formula:
     * <pre>
     *   sinh(a + b i) = sinh(a)cos(b)) + i cosh(a)sin(b)
     * </pre>
     *
     * <p>This is an odd function: {@code f(z) = -f(-z)}.
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
     * @return the hyperbolic sine of the complex number
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
                // ISO C99: Preserve the equality
                // sinh(conj(z)) = conj(sinh(z))
                // and the odd function: f(z) = -f(-z)
                // by always computing on a positive valued Complex number.
                // Math.cos(-x) == Math.cos(x) so ignore sign transform.
                final double signIm = imaginary < 0 ? -1 : 1;
                final double re = Double.POSITIVE_INFINITY * Math.cos(imaginary);
                final double im = Double.POSITIVE_INFINITY * Math.sin(imaginary * signIm);
                // Transform back
                return constructor.create(real < 0 ? -re : re, im * signIm);
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
     * Implements the following algorithm to compute {@code sqrt(a + b i)}:
     * <ol>
     * <li>Let {@code t = sqrt((|a| + |a + b i|) / 2)}
     * <li>if {@code (a >= 0)} return {@code t + (b / 2t) i}
     * <li>else return {@code |b| / 2t + sign(b)t i }
     * </ol>
     * where:
     * <ul>
     * <li>{@code |a| = }{@link Math#abs}(a)
     * <li>{@code |a + b i| = }{@link Complex#abs}(a + b i)
     * <li>{@code sign(b) =  }{@link Math#copySign(double,double) copySign(1.0, b)}
     * </ul>
     *
     * @return the square root of {@code this}.
     */
    public Complex sqrt() {
        return sqrt(real, imaginary);
    }

    /**
     * Compute the square root of the complex number.
     * Implements the following algorithm to compute {@code sqrt(a + b i)}:
     * <ol>
     * <li>Let {@code t = sqrt((|a| + |a + b i|) / 2)}
     * <li>if {@code (a >= 0)} return {@code t + (b / 2t) i}
     * <li>else return {@code |b| / 2t + sign(b)t i }
     * </ol>
     * where:
     * <ul>
     * <li>{@code |a| = }{@link Math#abs}(a)
     * <li>{@code |a + b i| = }{@link Complex#abs}(a + b i)
     * <li>{@code sign(b) = }{@link Math#copySign(double,double) copySign}(1.0, b)
     * </ul>
     *
     * @param real Real component.
     * @param imaginary Imaginary component.
     * @return the square root of the complex number.
     */
    private static Complex sqrt(double real, double imaginary) {
        // Special case for infinite imaginary for all real including nan
        if (Double.isInfinite(imaginary)) {
            return new Complex(Double.POSITIVE_INFINITY, imaginary);
        }
        if (Double.isFinite(real)) {
            if (Double.isFinite(imaginary)) {
                // Edge case for real numbers
                if (imaginary == 0) {
                    final double sqrtAbs = Math.sqrt(Math.abs(real));
                    if (real < 0) {
                        return new Complex(0, Math.copySign(sqrtAbs, imaginary));
                    }
                    return new Complex(sqrtAbs, imaginary);
                }
                // Get the absolute of the real
                double absA = Math.abs(real);
                // Compute |a + b i|
                double absC = getAbsolute(real, imaginary);

                // t = sqrt((|a| + |a + b i|) / 2)
                // This is always representable as this complex is finite.
                double t;

                // Overflow safe
                if (absC == Double.POSITIVE_INFINITY) {
                    // Complex is too large.
                    // Divide by the largest absolute component,
                    // compute the required sqrt and then scale back.
                    // Use the equality: sqrt(n) = sqrt(scale) * sqrt(n/scale)
                    // t = sqrt(max) * sqrt((|a|/max + |a + b i|/max) / 2)
                    // Note: The function may be non-monotonic at the junction.
                    // The alternative of returning infinity for a finite input is worse.
                    // Use precise scaling with:
                    // scale ~ 2^exponent
                    final double scale = Math.max(absA, Math.abs(imaginary));
                    // Make this even for fast rescaling using sqrt(2^exponent).
                    final int exponent = Math.getExponent(scale) & MASK_INT_TO_EVEN;
                    // Implement scaling using 2^-exponent
                    final double scaleA = Math.scalb(absA, -exponent);
                    final double scaleB = Math.scalb(imaginary, -exponent);
                    absC = getAbsolute(scaleA, scaleB);
                    // t = Math.sqrt(2^exponent) * Math.sqrt((scaleA + absC) / 2)
                    // This works if exponent is even:
                    // sqrt(2^exponent) = (2^exponent)^0.5 = 2^(exponent*0.5)
                    t = Math.scalb(Math.sqrt((scaleA + absC) / 2), exponent / 2);
                } else {
                    // Over-flow safe average
                    t = Math.sqrt(average(absA, absC));
                }

                if (real >= 0) {
                    return new Complex(t, imaginary / (2 * t));
                }
                return new Complex(Math.abs(imaginary) / (2 * t),
                                   Math.copySign(1.0, imaginary) * t);
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
     * Compute the average of two positive finite values in an overflow safe manner.
     *
     * @param a the first value
     * @param b the second value
     * @return the average
     */
    private static double average(double a, double b) {
        return (a < b) ?
            a + (b - a) / 2 :
            b + (a - b) / 2;
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/Tangent.html">
     * tangent</a> of this complex number.
     * Implements the formula:
     * <pre>
     *   tan(a + b i) = sin(2a)/(cos(2a)+cosh(2b)) + i [sinh(2b)/(cos(2a)+cosh(2b))]
     * </pre>
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
        // Multiply this number by I, compute tanh, then multiply by back
        return tanh(-imaginary, real, Complex::multiplyNegativeI);
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/HyperbolicTangent.html">
     * hyperbolic tangent</a> of this complex number.
     * Implements the formula:
     * <pre>
     *   tan(a + b i) = sinh(2a)/(cosh(2a)+cos(2b)) + i [sin(2b)/(cosh(2a)+cos(2b))]
     * </pre>
     *
     * <p>This is an odd function: {@code f(z) = -f(-z)}.
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
     * @return the hyperbolic tangent of the complex number
     */
    private static Complex tanh(double real, double imaginary, ComplexConstructor constructor) {
        // Math.cos and Math.sin return NaN for infinity.
        // Perform edge-condition checks on twice the imaginary value.
        // This handles very big imaginary numbers as infinite.

        final double imaginary2 = 2 * imaginary;

        if (Double.isFinite(real)) {
            if (Double.isFinite(imaginary2)) {
                if (real == 0) {
                    // Identity: sin x / (1 + cos x) = tan(x/2)
                    return constructor.create(real, Math.tan(imaginary));
                }
                if (imaginary == 0) {
                    // Identity: sinh x / (1 + cosh x) = tanh(x/2)
                    return constructor.create(Math.tanh(real), imaginary);
                }

                final double real2 = 2 * real;

                // Math.cosh returns positive infinity for infinity.
                // cosh -> inf
                final double d = Math.cosh(real2) + Math.cos(imaginary2);

                // Math.sinh returns the input infinity for infinity.
                // sinh -> inf for positive x; else -inf
                final double sinhRe2 = Math.sinh(real2);

                // Avoid inf / inf
                if (Double.isInfinite(sinhRe2) && Double.isInfinite(d)) {
                    // Fall-through to the result if infinite
                    return constructor.create(Math.copySign(1, real), Math.copySign(0, Math.sin(imaginary2)));
                }
                return constructor.create(sinhRe2 / d,
                                          Math.sin(imaginary2) / d);
            }
            // imaginary is infinite or NaN
            return NAN;
        }
        if (Double.isInfinite(real)) {
            if (Double.isFinite(imaginary2)) {
                return constructor.create(Math.copySign(1, real), Math.copySign(0, Math.sin(imaginary2)));
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
     *
     * <p>The argument is the angle phi between the positive real axis and
     * the point representing this number in the complex plane.
     * The value returned is between -PI (not inclusive)
     * and PI (inclusive), with negative values returned for numbers with
     * negative imaginary parts.
     *
     * <p>If either real or imaginary part (or both) is NaN, NaN is returned.
     * Infinite parts are handled as {@linkplain Math#atan2} handles them,
     * essentially treating finite parts as zero in the presence of an
     * infinite coordinate and returning a multiple of pi/4 depending on
     * the signs of the infinite parts.
     *
     * <p>This code follows the
     * <a href="http://www.iso-9899.info/wiki/The_Standard">ISO C Standard</a>, Annex G,
     * in calculating the returned value using the {@code atan2(b, a)} method for complex
     * {@code a + b i}.
     *
     * @return the argument of {@code this}.
     * @see Math#atan2(double, double)
     */
    public double arg() {
        // Delegate
        return Math.atan2(imaginary, real);
    }

    /**
     * Compute the argument of the complex number.
     *
     * <p>This function exists for use in trigonomic functions.
     *
     * @param real Real part.
     * @param imaginary Imaginary part.
     * @return the argument.
     * @see Math#atan2(double, double)
     */
    private static double getArgument(double real, double imaginary) {
        // Delegate
        return Math.atan2(imaginary, real);
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
     * {@link #arg() argument} of this complex number.
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
        final double nthRootOfAbs = Math.pow(abs(), 1.0 / n);

        // Compute nth roots of complex number with k = 0, 1, ... n-1
        final double nthPhi = arg() / n;
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
     * Change the sign of the magnitude based on the signed value.
     *
     * <p>If the signed value is negative then the result is {@code -magnitude}; otherwise
     * return {@code magnitude}.
     *
     * <p>A signed value of {@code -0.0} is treated as negative. A signed value of {@code NaN}
     * is treated as positive.
     *
     * <p>This is not the same as {@link Math#copySign(double, double)} as this method
     * will change the sign based on the signed value rather than copy the sign.
     *
     * @param magnitude the magnitude
     * @param signedValue the signed value
     * @return magnitude or -magnitude
     * @see #negative(double)
     */
    private static double changeSign(double magnitude, double signedValue) {
        return negative(signedValue) ? -magnitude : magnitude;
    }

    /**
     * Creates an exception.
     *
     * @param message Message prefix.
     * @param error Input that caused the error.
     * @param cause Underlying exception (if any).
     * @return a new instance.
     */
    private static NumberFormatException parsingException(String message,
                                                          String error,
                                                          Throwable cause) {
        // Not called with a null message or error
        final StringBuilder sb = new StringBuilder(100)
            .append(message)
            .append(" (").append(error).append(" )");
        if (cause != null) {
            sb.append(": ").append(cause.getMessage());
        }

        return new NumberFormatException(sb.toString());
    }
}
