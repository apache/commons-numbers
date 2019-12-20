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
    /**
     * A complex number representing {@code i}, the square root of -1.
     * <pre>{@code 0 + i 1}</pre>
     */
    public static final Complex I = new Complex(0, 1);
    /**
     * A complex number representing one.
     * <pre>{@code 1 + i 0}</pre>
     */
    public static final Complex ONE = new Complex(1, 0);
    /**
     * A complex number representing zero.
     * <pre>{@code 0 + i 0}</pre>
     */
    public static final Complex ZERO = new Complex(0, 0);

    /** A complex number representing {@code NaN + i NaN}. */
    private static final Complex NAN = new Complex(Double.NaN, Double.NaN);
    /** &pi;/2. */
    private static final double PI_OVER_2 = 0.5 * Math.PI;
    /** &pi;/4. */
    private static final double PI_OVER_4 = 0.25 * Math.PI;
    /** Mask an integer number to even by discarding the lowest bit. */
    private static final int MASK_INT_TO_EVEN = ~0x1;
    /** Natural logarithm of 2 (ln(2)). */
    private static final double LN_2 = Math.log(2);
    /** Base 10 logarithm of 2 (log10(2)). */
    private static final double LOG10_2 = Math.log10(2);

    /**
     * Crossover point to switch computation for asin/acos factor A.
     * This has been updated from the 1.5 value used by Hull et al to 10
     * as used in boost::math::complex.
     * @see <a href="https://svn.boost.org/trac/boost/ticket/7290">Boost ticket 7290</a>
     */
    private static final double A_CROSSOVER = 10;
    /** Crossover point to switch computation for asin/acos factor B. */
    private static final double B_CROSSOVER = 0.6471;
    /**
     * The safe maximum double value {@code x} to avoid loss of precision in asin/acos.
     * Equal to sqrt(M) / 8 in Hull, et al (1997) with M the largest normalised floating-point value.
     */
    private static final double SAFE_MAX = Math.sqrt(Double.MAX_VALUE) / 8;
    /**
     * The safe minimum double value {@code x} to avoid loss of precision/underflow in asin/acos.
     * Equal to sqrt(u) * 4 in Hull, et al (1997) with u the smallest normalised floating-point value.
     */
    private static final double SAFE_MIN = Math.sqrt(Double.MIN_NORMAL) * 4;
    /**
     * The safe maximum double value {@code x} to avoid loss of precision in atanh.
     * Equal to sqrt(M) / 2 with M the largest normalised floating-point value.
     */
    private static final double SAFE_UPPER = Math.sqrt(Double.MAX_VALUE) / 2;
    /**
     * The safe minimum double value {@code x} to avoid loss of precision/underflow in atanh.
     * Equal to sqrt(u) * 2 with u the smallest normalised floating-point value.
     */
    private static final double SAFE_LOWER = Math.sqrt(Double.MIN_NORMAL) * 2;
    /** Exponent offset in IEEE754 representation. */
    private static final long EXPONENT_OFFSET = 1023L;
    /**
     * Largest double-precision floating-point number such that
     * {@code 1 + EPSILON} is numerically equal to 1. This value is an upper
     * bound on the relative error due to rounding real numbers to double
     * precision floating-point numbers.
     *
     * <p>In IEEE 754 arithmetic, this is 2<sup>-53</sup>.</p>
     *
     * <p>Copied from o.a.c.numbers.core.Precision
     *
     * @see <a href="http://en.wikipedia.org/wiki/Machine_epsilon">Machine epsilon</a>
     */
    public static final double EPSILON = Double.longBitsToDouble((EXPONENT_OFFSET - 53L) << 52);

    /** Serializable version identifier. */
    private static final long serialVersionUID = 20180201L;

    /**
     * The size of the buffer for {@link #toString()}.
     *
     * <p>The longest double will require a sign, a maximum of 17 digits, the decimal place
     * and the exponent, e.g. for max value this is 24 chars: -1.7976931348623157e+308.
     * Set the buffer size to twice this and round up to a power of 2 thus
     * allowing for formatting characters. The size is 64.
     */
    private static final int TO_STRING_SIZE = 64;
    /** The minimum number of characters in the format. This is 5, e.g. {@code "(0,0)"}. */
    private static final int FORMAT_MIN_LEN = 5;
    /** {@link #toString() String representation}. */
    private static final char FORMAT_START = '(';
    /** {@link #toString() String representation}. */
    private static final char FORMAT_END = ')';
    /** {@link #toString() String representation}. */
    private static final char FORMAT_SEP = ',';
    /** The minimum number of characters before the separator. This is 2, e.g. {@code "(0"}. */
    private static final int BEFORE_SEP = 2;

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
     * Returns a {@code Complex} instance representing the specified string {@code s}.
     *
     * <p>If {@code s} is {@code null}, then a {@code NullPointerException} is thrown.
     *
     * <p>The string must be in a format compatible with that produced by
     * {@link #toString() Complex.toString()}.
     * The format expects a start and end string surrounding two numeric parts split
     * by a separator. Leading and trailing spaces are allowed around each numeric part.
     * Each numeric part is parsed using {@link Double#parseDouble(String)}. The parts
     * are interpreted as the real and imaginary parts of the complex number.
     *
     * <p>Examples of valid strings and the equivalent {@code Complex} are shown below:
     *
     * <pre>
     * "(0,0)"             = Complex.ofCartesian(0, 0)
     * "(0.0,0.0)"         = Complex.ofCartesian(0, 0)
     * "(-0.0, 0.0)"       = Complex.ofCartesian(-0.0, 0)
     * "(-1.23, 4.56)"     = Complex.ofCartesian(-123, 4.56)
     * "(1e300,-1.1e-2)"   = Complex.ofCartesian(1e300, -1.1e-2)
     * </pre>
     *
     * @param s String representation.
     * @return an instance.
     * @throws NullPointerException if the string is null.
     * @throws NumberFormatException if the string does not contain a parsable complex number.
     * @see Double#parseDouble(String)
     * @see #toString()
     */
    public static Complex parse(String s) {
        final int len = s.length();
        if (len < FORMAT_MIN_LEN) {
            throw parsingException("Expected format",
                FORMAT_START + "real" + FORMAT_SEP + "imaginary" + FORMAT_END, null);
        }

        // Confirm start: '('
        if (s.charAt(0) != FORMAT_START) {
            throw parsingException("Expected start", FORMAT_START, null);
        }

        // Confirm end: ')'
        if (s.charAt(len - 1) != FORMAT_END) {
            throw parsingException("Expected end", FORMAT_END, null);
        }

        // Confirm separator ',' is between at least 2 characters from
        // either end: "(x,x)"
        // Count back from the end ignoring the last 2 characters.
        final int sep = s.lastIndexOf(FORMAT_SEP, len - 3);
        if (sep < BEFORE_SEP) {
            throw parsingException("Expected separator between two numbers", FORMAT_SEP, null);
        }

        // Should be no more separators
        if (s.indexOf(FORMAT_SEP, sep + 1) != -1) {
            throw parsingException("Incorrect number of parts, expected only 2 using separator",
                FORMAT_SEP, null);
        }

        // Try to parse the parts

        final String rePart = s.substring(1, sep);
        final double re;
        try {
            re = Double.parseDouble(rePart);
        } catch (final NumberFormatException ex) {
            throw parsingException("Could not parse real part", rePart, ex);
        }

        final String imPart = s.substring(sep + 1, len - 1);
        final double im;
        try {
            im = Double.parseDouble(imPart);
        } catch (final NumberFormatException ex) {
            throw parsingException("Could not parse imaginary part", imPart, ex);
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
     * {@link #add(Complex) add(Complex.ofCartesian(addend, 0))} since
     * {@code -0.0 + 0.0 = 0.0}.
     *
     * @param addend Value to be added to this {@code Complex}.
     * @return {@code this + addend}.
     * @see #add(Complex)
     * @see #ofCartesian(double, double)
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
     * Returns a {@code Complex} whose value is {@code (this / divisor)}.
     * Implements the formula:
     * <pre>
     * <code>
     *   a + i b     (ac + bd) + i (bc - ad)
     *   -------  =  -----------------------
     *   c + i d            c<sup>2</sup> + d<sup>2</sup>
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
     *   a + i b     (ac + bd) + i (bc - ad)
     *   -------  =  -----------------------
     *   c + i d            c<sup>2</sup> + d<sup>2</sup>
     * </code>
     * </pre>
     *
     * <p>Recalculates to recover infinities as specified in C.99
     * standard G.5.1. Method is fully in accordance with
     * C++11 standards for complex numbers.</p>
     *
     * <p>Note: In the event of divide by zero this method produces the same result
     * as dividing by a real-only zero using {@link #divide(double)}.
     *
     * @param re1 Real component of first number.
     * @param im1 Imaginary component of first number.
     * @param re2 Real component of second number.
     * @param im2 Imaginary component of second number.
     * @return (a + i b) / (c + i d).
     * @see <a href="http://mathworld.wolfram.com/ComplexDivision.html">Complex Division</a>
     * @see #divide(double)
     */
    private static Complex divide(double re1, double im1, double re2, double im2) {
        double a = re1;
        double b = im1;
        double c = re2;
        double d = im2;
        int ilogbw = 0;
        // Get the exponent to scale the divisor.
        final int exponent = getMaxExponent(c, d);
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
                // This case produces the same result as divide by a real-only zero
                // using divide(+/-0.0).
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
     *   (a + i b) / c = (a + i b) / (c + i 0)
     *                 = (a/c) + i (b/c)
     * </pre>
     *
     * <p>This method is included for compatibility with ISO C99 which defines arithmetic between
     * real-only and complex numbers.</p>
     *
     * <p>Note: This method should be preferred over using
     * {@link #divide(Complex) divide(Complex.ofCartesian(divisor, 0))}. Division
     * can generate signed zeros if {@code this} complex has zeros for the real
     * and/or imaginary component, or the divisor is infinity. The summation of signed zeros
     * in {@link #divide(Complex)} may create zeros in the result that differ in sign
     * from the equivalent call to divide by a real-only number.
     *
     * @param  divisor Value by which this {@code Complex} is to be divided.
     * @return {@code this / divisor}.
     * @see #divide(Complex)
     */
    public Complex divide(double divisor) {
        return new Complex(real / divisor, imaginary / divisor);
    }

    /**
     * Returns a {@code Complex} whose value is {@code (this / divisor)},
     * with {@code divisor} interpreted as an imaginary number.
     * Implements the formula:
     * <pre>
     *   (a + i b) / id = (a + i b) / (0 + i d)
     *                  = (b/d) + i (-a/d)
     * </pre>
     *
     * <p>This method is included for compatibility with ISO C99 which defines arithmetic between
     * imaginary-only and complex numbers.</p>
     *
     * <p>Note: This method should be preferred over using
     * {@link #divide(Complex) divide(Complex.ofCartesian(0, divisor))}. Division
     * can generate signed zeros if {@code this} complex has zeros for the real
     * and/or imaginary component, or the divisor is infinity. The summation of signed zeros
     * in {@link #divide(Complex)} may create zeros in the result that differ in sign
     * from the equivalent call to divide by an imaginary-only number.
     *
     * <p>Warning: This method will generate a different result from
     * {@link #divide(Complex) divide(Complex.ofCartesian(0, divisor))} if the divisor is zero.
     * In this case the divide method using a zero-valued Complex will produce the same result
     * as dividing by a real-only zero. The output from dividing by imaginary zero will create
     * infinite and NaN values in the same component parts as the output from
     * {@code this.divide(Complex.ZERO).multiplyImaginary(1)}, however the sign
     * of some infinity values may be negated.
     *
     * @param  divisor Value by which this {@code Complex} is to be divided.
     * @return {@code this / divisor}.
     * @see #divide(Complex)
     * @see #divide(double)
     */
    public Complex divideImaginary(double divisor) {
        return new Complex(imaginary / divisor, -real / divisor);
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
     *   (a + i b)(c + i d) = (ac - bd) + i (ad + bc)
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
     *   (a + i b)(c + i d) = (ac - bd) + i (ad + bc)
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
     *   (a + i b) c = (a + i b)(c + 0 i)
     *               = (ac) + i (bc)
     * </pre>
     *
     * <p>This method is included for compatibility with ISO C99 which defines arithmetic between
     * real-only and complex numbers.</p>
     *
     * <p>Note: This method should be preferred over using
     * {@link #multiply(Complex) multiply(Complex.ofCartesian(factor, 0))}. Multiplication
     * can generate signed zeros if either {@code this} complex has zeros for the real
     * and/or imaginary component, or if the factor is zero. The summation of signed zeros
     * in {@link #multiply(Complex)} may create zeros in the result that differ in sign
     * from the equivalent call to multiply by a real-only number.
     *
     * @param  factor value to be multiplied by this {@code Complex}.
     * @return {@code this * factor}.
     * @see #multiply(Complex)
     */
    public Complex multiply(double factor) {
        return new Complex(real * factor, imaginary * factor);
    }

    /**
     * Returns a {@code Complex} whose value is {@code this * factor}, with {@code factor}
     * interpreted as an imaginary number.
     * Implements the formula:
     * <pre>
     *   (a + i b) id = (a + i b)(0 + i d)
     *                = (-bd) + i (ad)
     * </pre>
     *
     * <p>This method can be used to compute the multiplication of this complex number {@code z}
     * by {@code i}. This should be used in preference to
     * {@link #multiply(Complex) multiply(Complex.I)} with or without {@link #negate() negation}:</p>
     *
     * <pre>
     *   iz = (-b + i a) = this.multiply(1);
     *  -iz = (b + i -a) = this.multiply(-1);
     * </pre>
     *
     * <p>This method is included for compatibility with ISO C99 which defines arithmetic between
     * imaginary-only and complex numbers.</p>
     *
     * <p>Note: This method should be preferred over using
     * {@link #multiply(Complex) multiply(Complex.ofCartesian(0, factor))}. Multiplication
     * can generate signed zeros if either {@code this} complex has zeros for the real
     * and/or imaginary component, or if the factor is zero. The summation of signed zeros
     * in {@link #multiply(Complex)} may create zeros in the result that differ in sign
     * from the equivalent call to multiply by an imaginary-only number.
     *
     * @param  factor value to be multiplied by this {@code Complex}.
     * @return {@code this * factor}.
     * @see #multiply(Complex)
     */
    public Complex multiplyImaginary(double factor) {
        return new Complex(-imaginary * factor, real * factor);
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
     * {@link #subtract(Complex) Complex.ofCartesian(minuend, 0).subtract(this))} since
     * {@code 0.0 - 0.0 = 0.0}.
     *
     * @param  minuend value this {@code Complex} is to be subtracted from.
     * @return {@code minuend - this}.
     * @see #subtract(Complex)
     * @see #ofCartesian(double, double)
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
     * <p>This is implemented using real {@code x} and imaginary {@code y} parts:</p>
     * <pre>
     * <code>
     *   acos(z) = acos(B) - i ln(A + sqrt(A<sup>2</sup>-1))
     *   A = 0.5 [ sqrt((x+1)<sup>2</sup>+y<sup>2</sup>) + sqrt((x-1)<sup>2</sup>+y<sup>2</sup>) ]
     *   B = 0.5 [ sqrt((x+1)<sup>2</sup>+y<sup>2</sup>) - sqrt((x-1)<sup>2</sup>+y<sup>2</sup>) ]
     * </code>
     * </pre>
     *
     * <p>The implementation is based on the method described in:</p>
     * <blockquote>
     * T E Hull, Thomas F Fairgrieve and Ping Tak Peter Tang (1997)
     * Implementing the complex Arcsine and Arccosine Functions using Exception Handling.
     * ACM Transactions on Mathematical Software, Vol 23, No 3, pp 299-335.
     * </blockquote>
     *
     * <p>The code has been adapted from the <a href="https://www.boost.org/">Boost</a>
     * {@code c++} implementation {@code <boost/math/complex/acos.hpp>}. The function is well
     * defined over the entire complex number range, and produces accurate values even at the
     * extremes due to special handling of overflow and underflow conditions.</p>
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
    private static Complex acos(final double real, final double imaginary,
                                final ComplexConstructor constructor) {
        // Compute with positive values and determine sign at the end
        final double x = Math.abs(real);
        final double y = Math.abs(imaginary);
        // The result (without sign correction)
        double re;
        double im;

        // Handle C99 special cases
        if (isPosInfinite(x)) {
            if (isPosInfinite(y)) {
                re = PI_OVER_4;
                im = y;
            } else if (Double.isNaN(y)) {
                // sign of the imaginary part of the result is unspecified
                return constructor.create(imaginary, real);
            } else {
                re = 0;
                im = Double.POSITIVE_INFINITY;
            }
        } else if (Double.isNaN(x)) {
            if (isPosInfinite(y)) {
                return constructor.create(x, -imaginary);
            }
            // No-use of the input constructor
            return NAN;
        } else if (isPosInfinite(y)) {
            re = PI_OVER_2;
            im = y;
        } else if (Double.isNaN(y)) {
            return constructor.create(x == 0 ? PI_OVER_2 : y, y);
        } else {
            // Special case for real numbers:
            if (y == 0 && x <= 1) {
                return constructor.create(x == 0 ? PI_OVER_2 : Math.acos(real), -imaginary);
            }

            final double xp1 = x + 1;
            final double xm1 = x - 1;

            if (inRegion(x, y, SAFE_MIN, SAFE_MAX)) {
                final double yy = y * y;
                final double r = Math.sqrt(xp1 * xp1 + yy);
                final double s = Math.sqrt(xm1 * xm1 + yy);
                final double a = 0.5 * (r + s);
                final double b = x / a;

                if (b <= B_CROSSOVER) {
                    re = Math.acos(b);
                } else {
                    final double apx = a + x;
                    if (x <= 1) {
                        re = Math.atan(Math.sqrt(0.5 * apx * (yy / (r + xp1) + (s - xm1))) / x);
                    } else {
                        re = Math.atan((y * Math.sqrt(0.5 * (apx / (r + xp1) + apx / (s + xm1)))) / x);
                    }
                }

                if (a <= A_CROSSOVER) {
                    double am1;
                    if (x < 1) {
                        am1 = 0.5 * (yy / (r + xp1) + yy / (s - xm1));
                    } else {
                        am1 = 0.5 * (yy / (r + xp1) + (s + xm1));
                    }
                    im = Math.log1p(am1 + Math.sqrt(am1 * (a + 1)));
                } else {
                    im = Math.log(a + Math.sqrt(a * a - 1));
                }
            } else {
                // Hull et al: Exception handling code from figure 6
                if (y <= (EPSILON * Math.abs(xm1))) {
                    if (x < 1) {
                        re = Math.acos(x);
                        im = y / Math.sqrt(xp1 * (1 - x));
                    } else {
                        // This deviates from Hull et al's paper as per
                        // https://svn.boost.org/trac/boost/ticket/7290
                        if ((Double.MAX_VALUE / xp1) > xm1) {
                            // xp1 * xm1 won't overflow:
                            re = y / Math.sqrt(xm1 * xp1);
                            im = Math.log1p(xm1 + Math.sqrt(xp1 * xm1));
                        } else {
                            re = y / x;
                            im = LN_2 + Math.log(x);
                        }
                    }
                } else if (y <= SAFE_MIN) {
                    // Hull et al: Assume x == 1.
                    // True if:
                    // E^2 > 8*sqrt(u)
                    //
                    // E = Machine epsilon: (1 + epsilon) = 1
                    // u = Double.MIN_NORMAL
                    re = Math.sqrt(y);
                    im = Math.sqrt(y);
                } else if (EPSILON * y - 1 >= x) {
                    re = PI_OVER_2;
                    im = LN_2 + Math.log(y);
                } else if (x > 1) {
                    re = Math.atan(y / x);
                    final double xoy = x / y;
                    im = LN_2 + Math.log(y) + 0.5 * Math.log1p(xoy * xoy);
                } else {
                    re = PI_OVER_2;
                    final double a = Math.sqrt(1 + y * y);
                    im = 0.5 * Math.log1p(2 * y * (y + a));
                }
            }
        }

        return constructor.create(negative(real) ? Math.PI - re : re,
                                  negative(imaginary) ? im : -im);
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
     * <p>This is implemented using real {@code x} and imaginary {@code y} parts:</p>
     * <pre>
     * <code>
     *   asin(z) = asin(B) + i sign(y)ln(A + sqrt(A<sup>2</sup>-1))
     *   A = 0.5 [ sqrt((x+1)<sup>2</sup>+y<sup>2</sup>) + sqrt((x-1)<sup>2</sup>+y<sup>2</sup>) ]
     *   B = 0.5 [ sqrt((x+1)<sup>2</sup>+y<sup>2</sup>) - sqrt((x-1)<sup>2</sup>+y<sup>2</sup>) ]
     *   sign(y) = {@link Math#copySign(double,double) copySign(1.0, y)}
     * </code>
     * </pre>
     *
     * <p>The implementation is based on the method described in:</p>
     * <blockquote>
     * T E Hull, Thomas F Fairgrieve and Ping Tak Peter Tang (1997)
     * Implementing the complex Arcsine and Arccosine Functions using Exception Handling.
     * ACM Transactions on Mathematical Software, Vol 23, No 3, pp 299-335.
     * </blockquote>
     *
     * <p>The code has been adapted from the <a href="https://www.boost.org/">Boost</a>
     * {@code c++} implementation {@code <boost/math/complex/asin.hpp>}. The function is well
     * defined over the entire complex number range, and produces accurate values even at the
     * extremes due to special handling of overflow and underflow conditions.</p>
     *
     * @return the inverse sine of this complex number
     */
    public Complex asin() {
        return asin(real, imaginary, Complex::ofCartesian);
    }

    /**
     * Compute the inverse sine of the complex number.
     *
     * <p>This function exists to allow implementation of the identity
     * {@code asinh(z) = -i asin(iz)}.<p>
     *
     * <p>The code has been adapted from the <a href="https://www.boost.org/">Boost</a>
     * {@code c++} implementation {@code <boost/math/complex/asin.hpp>}.</p>
     *
     * @param real Real part.
     * @param imaginary Imaginary part.
     * @param constructor Constructor.
     * @return the inverse sine of this complex number
     */
    private static Complex asin(final double real, final double imaginary,
                                final ComplexConstructor constructor) {
        // Compute with positive values and determine sign at the end
        final double x = Math.abs(real);
        final double y = Math.abs(imaginary);
        // The result (without sign correction)
        double re;
        double im;

        // Handle C99 special cases
        if (Double.isNaN(x)) {
            if (isPosInfinite(y)) {
                re = x;
                im = y;
            } else {
                // No-use of the input constructor
                return NAN;
            }
        } else if (Double.isNaN(y)) {
            if (x == 0) {
                re = 0;
                im = y;
            } else if (isPosInfinite(x)) {
                re = y;
                im = x;
            } else {
                // No-use of the input constructor
                return NAN;
            }
        } else if (isPosInfinite(x)) {
            re = isPosInfinite(y) ? PI_OVER_4 : PI_OVER_2;
            im = x;
        } else if (isPosInfinite(y)) {
            re = 0;
            im = y;
        } else {
            // Special case for real numbers:
            if (y == 0 && x <= 1) {
                return constructor.create(Math.asin(real), imaginary);
            }

            final double xp1 = x + 1;
            final double xm1 = x - 1;

            if (inRegion(x, y, SAFE_MIN, SAFE_MAX)) {
                final double yy = y * y;
                final double r = Math.sqrt(xp1 * xp1 + yy);
                final double s = Math.sqrt(xm1 * xm1 + yy);
                final double a = 0.5 * (r + s);
                final double b = x / a;

                if (b <= B_CROSSOVER) {
                    re = Math.asin(b);
                } else {
                    final double apx = a + x;
                    if (x <= 1) {
                        re = Math.atan(x / Math.sqrt(0.5 * apx * (yy / (r + xp1) + (s - xm1))));
                    } else {
                        re = Math.atan(x / (y * Math.sqrt(0.5 * (apx / (r + xp1) + apx / (s + xm1)))));
                    }
                }

                if (a <= A_CROSSOVER) {
                    double am1;
                    if (x < 1) {
                        am1 = 0.5 * (yy / (r + xp1) + yy / (s - xm1));
                    } else {
                        am1 = 0.5 * (yy / (r + xp1) + (s + xm1));
                    }
                    im = Math.log1p(am1 + Math.sqrt(am1 * (a + 1)));
                } else {
                    im = Math.log(a + Math.sqrt(a * a - 1));
                }
            } else {
                // Hull et al: Exception handling code from figure 3
                if (y <= (EPSILON * Math.abs(xm1))) {
                    if (x < 1) {
                        re = Math.asin(x);
                        im = y / Math.sqrt(-xp1 * xm1);
                    } else {
                        re = PI_OVER_2;
                        if ((Double.MAX_VALUE / xp1) > xm1) {
                            // xp1 * xm1 won't overflow:
                            im = Math.log1p(xm1 + Math.sqrt(xp1 * xm1));
                        } else {
                            im = LN_2 + Math.log(x);
                        }
                    }
                } else if (y <= SAFE_MIN) {
                    // Hull et al: Assume x == 1.
                    // True if:
                    // E^2 > 8*sqrt(u)
                    //
                    // E = Machine epsilon: (1 + epsilon) = 1
                    // u = Double.MIN_NORMAL
                    re = PI_OVER_2 - Math.sqrt(y);
                    im = Math.sqrt(y);
                } else if (EPSILON * y - 1 >= x) {
                    // Possible underflow:
                    re = x / y;
                    im = LN_2 + Math.log(y);
                } else if (x > 1) {
                    re = Math.atan(x / y);
                    final double xoy = x / y;
                    im = LN_2 + Math.log(y) + 0.5 * Math.log1p(xoy * xoy);
                } else {
                    final double a = Math.sqrt(1 + y * y);
                    // Possible underflow:
                    re = x / a;
                    im = 0.5 * Math.log1p(2 * y * (y + a));
                }
            }
        }

        return constructor.create(changeSign(re, real),
                                  changeSign(im, imaginary));
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
     * <p>This function is computed using the trigonomic identity:</p>
     * <pre>
     *   asinh(z) = -i asin(iz)
     * </pre>
     *
     * @return the inverse hyperbolic sine of this complex number
     */
    public Complex asinh() {
        // Define in terms of asin
        // asinh(z) = -i asin(iz)
        // Note: This is the opposite the the identity defined in the C.99 standard:
        // asin(z) = -i asinh(iz)
        // Multiply this number by I, compute asin, then multiply by back
        return asin(-imaginary, real, Complex::multiplyNegativeI);
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
     * <p>This is implemented using real {@code x} and imaginary {@code y} parts:</p>
     * <pre>
     * <code>
     *   atanh(z) = 0.25 ln(1 + 4x/((1-x)<sup>2</sup>+y<sup>2</sup>) + i 0.5 tan<sup>-1</sup>(2y, 1-x<sup>2</sup>-y<sup>2</sup>)
     * </code>
     * </pre>
     *
     * <p>The code has been adapted from the <a href="https://www.boost.org/">Boost</a>
     * {@code c++} implementation {@code <boost/math/complex/atanh.hpp>}. The function is well
     * defined over the entire complex number range, and produces accurate values even at the
     * extremes due to special handling of overflow and underflow conditions.</p>
     *
     * @return the inverse hyperbolic tangent of this complex number
     * @see <a href="http://functions.wolfram.com/ElementaryFunctions/ArcTanh/">ArcTanh</a>
     */
    public Complex atanh() {
        return atanh(real, imaginary, Complex::ofCartesian);
    }

    /**
     * Compute the inverse hyperbolic tangent of this complex number.
     *
     * <p>This function exists to allow implementation of the identity
     * {@code atan(z) = -i atanh(iz)}.<p>
     *
     * @param real Real part.
     * @param imaginary Imaginary part.
     * @param constructor Constructor.
     * @return the inverse hyperbolic tangent of the complex number
     */
    private static Complex atanh(final double real, final double imaginary,
                                 final ComplexConstructor constructor) {
        // Compute with positive values and determine sign at the end
        final double x = Math.abs(real);
        final double y = Math.abs(imaginary);
        // The result (without sign correction)
        double re;
        double im;

        // Handle C99 special cases
        if (Double.isNaN(x)) {
            if (isPosInfinite(y)) {
                // The sign of the real part of the result is unspecified
                return constructor.create(0, Math.copySign(PI_OVER_2, imaginary));
            }
            // No-use of the input constructor.
            // Optionally raises the ‘‘invalid’’ floating-point exception, for finite y.
            return NAN;
        } else if (Double.isNaN(y)) {
            if (isPosInfinite(x)) {
                return constructor.create(Math.copySign(0, real), Double.NaN);
            }
            if (x == 0) {
                return constructor.create(real, Double.NaN);
            }
            // No-use of the input constructor
            return NAN;
        } else {
            // x && y are finite or infinite.

            // Check the safe region.
            // The lower and upper bounds have been copied from boost::math::atanh.
            // They are different from the safe region for asin and acos.
            // x >= SAFE_UPPER: (1-x) == -x
            // x <= SAFE_LOWER: 1 - x^2 = 1

            if (inRegion(x, y, SAFE_LOWER, SAFE_UPPER)) {
                // Normal computation within a safe region.

                // minus x plus 1: (-x+1)
                final double mxp1 = 1 - x;
                final double yy = y * y;
                // The definition of real component is:
                // real = log( ((x+1)^2+y^2) / ((1-x)^2+y^2) ) / 4
                // This simplifies by adding 1 and subtracting 1 as a fraction:
                //      = log(1 + ((x+1)^2+y^2) / ((1-x)^2+y^2) - ((1-x)^2+y^2)/((1-x)^2+y^2) ) / 4
                //
                // real(atanh(z)) == log(1 + 4*x / ((1-x)^2+y^2)) / 4
                // imag(atanh(z)) == tan^-1 (2y, (1-x)(1+x) - y^2) / 2
                // The division is done at the end of the function.
                re = Math.log1p(4 * x / (mxp1 * mxp1 + yy));
                im = Math.atan2(2 * y, mxp1 * (1 + x) - yy);
            } else {
                // This section handles exception cases that would normally cause
                // underflow or overflow in the main formulas.

                // C99. G.7: Special case for imaginary only numbers
                if (x == 0) {
                    if (imaginary == 0) {
                        return constructor.create(real, imaginary);
                    }
                    // atanh(iy) = i atan(y)
                    return constructor.create(real, Math.atan(imaginary));
                }

                // Real part:
                // real = Math.log1p(4x / ((1-x)^2 + y^2))
                // real = Math.log1p(4x / (1 - 2x + x^2 + y^2))
                // real = Math.log1p(4x / (1 + x(x-2) + y^2))
                // without either overflow or underflow in the squared terms.
                if (x >= SAFE_UPPER) {
                    // (1-x) = -x to machine precision:
                    // log1p(4x / (x^2 + y^2))
                    if (isPosInfinite(x) || isPosInfinite(y)) {
                        re = 0;
                    } else if (y >= SAFE_UPPER) {
                        // Big x and y: divide by x*y
                        re = Math.log1p((4 / y) / (x / y + y / x));
                    } else if (y > 1) {
                        // Big x: divide through by x:
                        re = Math.log1p(4 / (x + y * y / x));
                    } else {
                        // Big x small y, as above but neglect y^2/x:
                        re = Math.log1p(4 / x);
                    }
                } else if (y >= SAFE_UPPER) {
                    if (x > 1) {
                        // Big y, medium x, divide through by y:
                        final double mxp1 = 1 - x;
                        re = Math.log1p((4 * x / y) / (mxp1 * mxp1 / y + y));
                    } else {
                        // Big y, small x, as above but neglect (1-x)^2/y:
                        // Note: log1p(v) == v - v^2/2 + v^3/3 ... Taylor series when v is small.
                        // Here v is so small only the first term matters.
                        re = 4 * x / y / y;
                    }
                } else if (x == 1) {
                    // x = 1, small y:
                    // Special case when x == 1 as (1-x) is invalid.
                    // Simplify the following formula:
                    // real = log( sqrt((x+1)^2+y^2) ) / 2 - log( sqrt((1-x)^2+y^2) ) / 2
                    //      = log( sqrt(4+y^2) ) / 2 - log(y) / 2
                    // if: 4+y^2 -> 4
                    //      = log( 2 ) / 2 - log(y) / 2
                    //      = (log(2) - log(y)) / 2
                    // Multiply by 2 as it will be divided by 4 at the end.
                    // C99: if y=0 raises the ‘‘divide-by-zero’’ floating-point exception.
                    re = 2 * (LN_2 - Math.log(y));
                } else {
                    // Modified from boost which checks y > SAFE_LOWER.
                    // if y*y -> 0 it will be ignored so always include it.
                    final double mxp1 = 1 - x;
                    re = Math.log1p((4 * x) / (mxp1 * mxp1 + y * y));
                }

                // Imaginary part:
                // imag = atan2(2y, (1-x)(1+x) - y^2)
                // if x or y are large, then the formula:
                //   atan2(2y, (1-x)(1+x) - y^2)
                // evaluates to +(PI - theta) where theta is negligible compared to PI.
                if ((x >= SAFE_UPPER) || (y >= SAFE_UPPER)) {
                    im = Math.PI;
                } else if (x <= SAFE_LOWER) {
                    // (1-x)^2 -> 1
                    if (y <= SAFE_LOWER) {
                        // 1 - y^2 -> 1
                        im = Math.atan2(2 * y, 1);
                    } else {
                        im = Math.atan2(2 * y, 1 - y * y);
                    }
                } else {
                    // Medium x, small y.
                    // Modified from boost which checks (y == 0) && (x == 1) and sets re = 0.
                    // This is same as the result from calling atan2(0, 0) so just do that.
                    // 1 - y^2 = 1 so ignore subtracting y^2
                    im = Math.atan2(2 * y, (1 - x) * (1 + x));
                }
            }
        }

        re /= 4;
        im /= 2;
        return constructor.create(changeSign(re, real),
                                  changeSign(im, imaginary));
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
        return acos(real, imaginary, (re, im) ->
            // Set the sign appropriately for real >= 0
            (negative(im)) ?
                // Multiply by I
                new Complex(-im, re) :
                // Multiply by -I
                new Complex(im, -re)
        );
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
        return log(Math::log, LN_2, Complex::ofCartesian);
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
        return log(Math::log10, LOG10_2, Complex::ofCartesian);
    }

    /**
     * Compute the logarithm of this complex number using the provided function.
     * Implements the formula:
     * <pre>
     *   log(a +  bi) = log(|a + b i|) + i arg(a + b i)
     * </pre>
     *
     * <p>Warning: The argument {@code logOf2} must be equal to {@code log(2)} using the
     * provided log function otherwise scaling using powers of 2 in the case of overflow
     * will be incorrect. This is provided as an internal optimisation.
     *
     * @param log Log function.
     * @param logOf2 The log function applied to 2.
     * @param constructor Constructor for the returned complex.
     * @return the logarithm of {@code this}.
     * @see #abs()
     * @see #arg()
     */
    private Complex log(UnaryOperation log, double logOf2, ComplexConstructor constructor) {
        // All ISO C99 edge cases satisfied by the Math library.
        // Make computation overflow safe.

        // Note:
        // log(|a + b i|) = log(sqrt(a^2 + b^2)) = 0.5 * log(a^2 + b^2)
        // If real and imaginary are with a safe region then omit the sqrt().
        final double x = Math.abs(real);
        final double y = Math.abs(imaginary);

        // Use the safe region defined for atanh to avoid over/underflow for x^2
        if ((x > SAFE_LOWER) && (x < SAFE_UPPER) && (y > SAFE_LOWER) && (y < SAFE_UPPER)) {
            return constructor.create(0.5 * log.apply(x * x + y * y), arg());
        }

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
            final int exponent = getMaxExponent(real, imaginary);
            // Implement scaling using 2^-exponent
            final double absOs = Math.hypot(Math.scalb(real, -exponent), Math.scalb(imaginary, -exponent));
            // log(2^exponent) = ln2(2^exponent) * log(2)
            return constructor.create(log.apply(absOs) + exponent * logOf2, arg());
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
     * <li>{@code sign(b) = }{@link Math#copySign(double,double) copySign(1.0, b)}
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
                final double absA = Math.abs(real);
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
                    // Make exponent even for fast rescaling using sqrt(2^exponent).
                    final int exponent = getMaxExponent(absA, imaginary) & MASK_INT_TO_EVEN;
                    // Implement scaling using 2^-exponent
                    final double scaleA = Math.scalb(absA, -exponent);
                    final double scaleB = Math.scalb(imaginary, -exponent);
                    absC = getAbsolute(scaleA, scaleB);
                    // t = Math.sqrt(2^exponent) * Math.sqrt((scaleA + absC) / 2)
                    // This works if exponent is even:
                    // sqrt(2^exponent) = (2^exponent)^0.5 = 2^(exponent*0.5)
                    t = Math.scalb(Math.sqrt((scaleA + absC) / 2), exponent / 2);
                } else {
                    // Over-flow safe average: absA < absC and abdC is finite.
                    t = Math.sqrt(absA + (absC - absA) / 2);
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
     * @see <a href="http://functions.wolfram.com/ElementaryFunctions/Tan/">Tangent</a>
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
     * @see <a href="http://functions.wolfram.com/ElementaryFunctions/Tanh/">Tanh</a>
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

        if (Double.isFinite(real)) {
            if (Double.isFinite(imaginary)) {
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
                final double divisor = Math.cosh(real2) + cos2(imaginary);

                // Math.sinh returns the input infinity for infinity.
                // sinh -> inf for positive x; else -inf
                final double sinhRe2 = Math.sinh(real2);

                // Avoid inf / inf
                if (Double.isInfinite(sinhRe2) && Double.isInfinite(divisor)) {
                    // Handle as if real was infinite
                    return constructor.create(Math.copySign(1, real), Math.copySign(0, sin2(imaginary)));
                }
                return constructor.create(sinhRe2 / divisor,
                                          sin2(imaginary) / divisor);
            }
            // imaginary is infinite or NaN
            return NAN;
        }
        if (Double.isInfinite(real)) {
            if (Double.isFinite(imaginary)) {
                return constructor.create(Math.copySign(1, real), Math.copySign(0, sin2(imaginary)));
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
     * Safely compute {@code cos(2*a)} when {@code a} is finite.
     * Note that {@link Math#cos(double)} returns NaN when the input is infinite.
     * If {@code 2*a} is finite use {@code Math.cos(2*a)}; otherwise use the identity:
     * <pre>
     * <code>
     *   cos(2a) = 2 cos<sup>2</sup>(a) - 1
     * </code>
     * </pre>
     *
     * @param a Angle a.
     * @return the cosine of 2a
     * @see Math#cos(double)
     */
    private static double cos2(double a) {
        final double twoA = 2 * a;
        if (Double.isFinite(twoA)) {
            return Math.cos(twoA);
        }
        final double cosA = Math.cos(a);
        return 2 * cosA * cosA - 1;
    }

    /**
     * Safely compute {@code sin(2*a)} when {@code a} is finite.
     * Note that {@link Math#sin(double)} returns NaN when the input is infinite.
     * If {@code 2*a} is finite use {@code Math.sin(2*a)}; otherwise use the identity:
     * <pre>
     * <code>
     *   sin(2a) = 2 sin(a) cos(a)
     * </code>
     * </pre>
     *
     * @param a Angle a.
     * @return the sine of 2a
     * @see Math#sin(double)
     */
    private static double sin2(double a) {
        final double twoA = 2 * a;
        if (Double.isFinite(twoA)) {
            return Math.sin(twoA);
        }
        return 2 * Math.sin(a) * Math.cos(a);
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

    /**
     * Returns a string representation of the complex number.
     *
     * <p>The string will represent the numeric values of the real and imaginary parts.
     * The values are split by a separator and surrounded by parentheses.
     * The string can be {@link #parse(String) parsed} to obtain an instance with the same value.
     *
     * <p>The format for complex number {@code (a + i b)} is {@code "(a,b)"}, with {@code a} and
     * {@code b} converted as if using {@link Double#toString(double)}.
     *
     * @return a string representation of the complex number.
     * @see #parse(String)
     * @see Double#toString(double)
     */
    @Override
    public String toString() {
        return new StringBuilder(TO_STRING_SIZE)
            .append(FORMAT_START)
            .append(real).append(FORMAT_SEP)
            .append(imaginary)
            .append(FORMAT_END)
            .toString();
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
     * Check that a value is positive infinity. Used to replace {@link Double#isInfinite()}
     * when the input value is known to be positive (i.e. in the case where it have been
     * set using {@link Math#abs(double)}).
     *
     * @param d Value.
     * @return {@code true} if {@code d} is +inf.
     */
    private static boolean isPosInfinite(double d) {
        return d == Double.POSITIVE_INFINITY;
    }

    /**
     * Create a complex number given the real and imaginary parts, then multiply by {@code -i}.
     * This is used in functions that implement trigonomic identities. It is the functional
     * equivalent of:
     *
     * <pre>
     *   z = new Complex(real, imaginary).multiplyImaginary(-1);
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
     * Returns the largest unbiased exponent used in the representation of the
     * two numbers. Special cases:
     *
     * <ul>
     * <li>If either argument is NaN or infinite, then the result is
     * {@link Double#MAX_EXPONENT} + 1.
     * <li>If both arguments are zero or subnormal, then the result is
     * {@link Double#MIN_EXPONENT} -1.
     * </ul>
     *
     * @param a the first value
     * @param b the second value
     * @return the maximum unbiased exponent of the values
     * @see Math#getExponent(double)
     */
    private static int getMaxExponent(double a, double b) {
        // This could return:
        // Math.getExponent(Math.max(Math.abs(a), Math.abs(b)))
        // A speed test is required to determine performance.

        return Math.max(Math.getExponent(a), Math.getExponent(b));
    }

    /**
     * Checks if both x and y are in the region defined by the minimum and maximum.
     *
     * @param x x value.
     * @param y y value.
     * @param min the minimum (exclusive).
     * @param max the maximum (exclusive).
     * @return true if inside the region
     */
    private static boolean inRegion(double x, double y, double min, double max) {
        return (x < max) && (x > min) && (y < max) && (y > min);
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
                                                          Object error,
                                                          Throwable cause) {
        // Not called with a null message or error
        final StringBuilder sb = new StringBuilder(100)
            .append(message)
            .append(" '").append(error).append('\'');
        if (cause != null) {
            sb.append(": ").append(cause.getMessage());
        }

        return new NumberFormatException(sb.toString());
    }
}
