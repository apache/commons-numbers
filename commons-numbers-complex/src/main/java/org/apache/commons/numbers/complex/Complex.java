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

/**
 * Cartesian representation of a complex number. The complex number is expressed
 * in the form \( a + ib \) where \( a \) and \( b \) are real numbers and \( i \)
 * is the imaginary unit which satisfies the equation \( i^2 = -1 \). For the
 * complex number \( a + ib \), \( a \) is called the <em>real part</em> and
 * \( b \) is called the <em>imaginary part</em>.
 *
 * <p>This class is immutable. All arithmetic will create a new instance for the
 * result.</p>
 *
 * <p>Arithmetic in this class conforms to the C99 standard for complex numbers
 * defined in ISO/IEC 9899, Annex G. Methods have been named using the equivalent
 * method in ISO C99. The behavior for special cases is listed as defined in C99.</p>
 *
 * <p>For functions \( f \) which obey the conjugate equality \( conj(f(z)) = f(conj(z)) \),
 * the specifications for the upper half-plane imply the specifications for the lower
 * half-plane.</p>
 *
 * <p>For functions that are either odd, \( f(z) = -f(-z) \), or even, \( f(z) =  f(-z) \),
 * the specifications for the first quadrant imply the specifications for the other three
 * quadrants.</p>
 *
 * <p>Special cases of <a href="http://mathworld.wolfram.com/BranchCut.html">branch cuts</a>
 * for multivalued functions adopt the principle value convention from C99. Specials cases
 * from C99 that raise the "invalid" or "divide-by-zero"
 * <a href="https://en.cppreference.com/w/c/numeric/fenv/FE_exceptions">floating-point
 * exceptions</a> return the documented value without an explicit mechanism to notify
 * of the exception case, that is no exceptions are thrown during computations in-line with
 * the convention of the corresponding single-valued functions in
 * {@link java.lang.Math java.lang.Math}.
 * These cases are documented in the method special cases as "invalid" or "divide-by-zero"
 * floating-point operation.
 * Note: Invalid floating-point exception cases will result in a complex number where the
 * cardinality of NaN component parts has increased as a real or imaginary part could
 * not be computed and is set to NaN.
 *
 * @see <a href="http://www.open-std.org/JTC1/SC22/WG14/www/standards">
 *    ISO/IEC 9899 - Programming languages - C</a>
 */
public final class Complex implements Serializable  {
    /**
     * A complex number representing \( i \), the square root of \( -1 \).
     *
     * <p>\( (0 + i 1) \).
     */
    public static final Complex I = new Complex(0, 1);
    /**
     * A complex number representing one.
     *
     * <p>\( (1 + i 0) \).
     */
    public static final Complex ONE = new Complex(1, 0);
    /**
     * A complex number representing zero.
     *
     * <p>\( (0 + i 0) \).
     */
    public static final Complex ZERO = new Complex(0, 0);

    /** A complex number representing {@code NaN + i NaN}. */
    private static final Complex NAN = new Complex(Double.NaN, Double.NaN);
    /** Exponent offset in IEEE754 representation. */
    private static final int EXPONENT_OFFSET = 1023;
    /** Mask to remove the sign bit from a long. */
    private static final long UNSIGN_MASK = 0x7fff_ffff_ffff_ffffL;
    /** Mask to extract the 52-bit mantissa from a long representation of a double. */
    private static final long MANTISSA_MASK = 0x000f_ffff_ffff_ffffL;

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
     * @return {@code Complex} number.
     */
    public static Complex ofCartesian(double real, double imaginary) {
        return new Complex(real, imaginary);
    }

    /**
     * Creates a complex number from its polar representation using modulus {@code rho} (\( \rho \))
     * and phase angle {@code theta} (\( \theta \)).
     *
     * \[ \begin{aligned}
     *    x &amp;= \rho \cos(\theta) \\
     *    y &amp;= \rho \sin(\theta) \end{aligned} \]
     *
     * <p>Requires that {@code rho} is non-negative and non-NaN and {@code theta} is finite;
     * otherwise returns a complex with NaN real and imaginary parts. A {@code rho} value of
     * {@code -0.0} is considered negative and an invalid modulus.
     *
     * <p>A non-NaN complex number constructed using this method will satisfy the following
     * to within floating-point error when {@code theta} is in the range
     * \( -\pi\ \lt \theta \leq \pi \):
     *
     * <pre>
     *  Complex.ofPolar(rho, theta).abs() == rho
     *  Complex.ofPolar(rho, theta).arg() == theta</pre>
     *
     * <p>If {@code rho} is infinite then the resulting parts may be infinite or NaN
     * following the rules for double arithmetic, for example:</p>
     *
     * <ul>
     * <li>{@code ofPolar(}\( -0.0 \){@code , }\( 0 \){@code ) = }\( \text{NaN} + i \text{NaN} \)
     * <li>{@code ofPolar(}\( 0.0 \){@code , }\( 0 \){@code ) = }\( 0 + i 0 \)
     * <li>{@code ofPolar(}\( 1 \){@code , }\( 0 \){@code ) = }\( 1 + i 0 \)
     * <li>{@code ofPolar(}\( 1 \){@code , }\( \pi \){@code ) = }\( -1 + i \sin(\pi) \)
     * <li>{@code ofPolar(}\( \infty \){@code , }\( \pi \){@code ) = }\( -\infty + i \infty \)
     * <li>{@code ofPolar(}\( \infty \){@code , }\( 0 \){@code ) = }\( -\infty + i \text{NaN} \)
     * <li>{@code ofPolar(}\( \infty \){@code , }\( -\frac{\pi}{4} \){@code ) = }\( \infty - i \infty \)
     * <li>{@code ofPolar(}\( \infty \){@code , }\( 5\frac{\pi}{4} \){@code ) = }\( -\infty - i \infty \)
     * </ul>
     *
     * <p>This method is the functional equivalent of the C++ method {@code std::polar}.
     *
     * @param rho The modulus of the complex number.
     * @param theta The argument of the complex number.
     * @return {@code Complex} number.
     * @see <a href="http://mathworld.wolfram.com/PolarCoordinates.html">Polar Coordinates</a>
     */
    public static Complex ofPolar(double rho, double theta) {
        // Require finite theta and non-negative, non-nan rho
        if (!Double.isFinite(theta) || negative(rho) || Double.isNaN(rho)) {
            return NAN;
        }
        final double x = rho * Math.cos(theta);
        final double y = rho * Math.sin(theta);
        return new Complex(x, y);
    }

    /**
     * Create a complex cis number. This is also known as the complex exponential:
     *
     * \[ \text{cis}(x) = e^{ix} = \cos(x) + i \sin(x) \]
     *
     * @param x {@code double} to build the cis number.
     * @return {@code Complex} cis number.
     * @see <a href="http://mathworld.wolfram.com/Cis.html">Cis</a>
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
     * The format expects a start and end parentheses surrounding two numeric parts split
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
     * "(-1.23, 4.56)"     = Complex.ofCartesian(-1.23, 4.56)
     * "(1e300,-1.1e-2)"   = Complex.ofCartesian(1e300, -1.1e-2)</pre>
     *
     * @param s String representation.
     * @return {@code Complex} number.
     * @throws NullPointerException if the string is null.
     * @throws NumberFormatException if the string does not contain a parsable complex number.
     * @see Double#parseDouble(String)
     * @see #toString()
     */
    public static Complex parse(String s) {
        final int len = s.length();
        if (len < FORMAT_MIN_LEN) {
            throw new NumberFormatException(
                parsingExceptionMsg("Input too short, expected format",
                                    FORMAT_START + "x" + FORMAT_SEP + "y" + FORMAT_END, s));
        }

        // Confirm start: '('
        if (s.charAt(0) != FORMAT_START) {
            throw new NumberFormatException(
                parsingExceptionMsg("Expected start delimiter", FORMAT_START, s));
        }

        // Confirm end: ')'
        if (s.charAt(len - 1) != FORMAT_END) {
            throw new NumberFormatException(
                parsingExceptionMsg("Expected end delimiter", FORMAT_END, s));
        }

        // Confirm separator ',' is between at least 2 characters from
        // either end: "(x,x)"
        // Count back from the end ignoring the last 2 characters.
        final int sep = s.lastIndexOf(FORMAT_SEP, len - 3);
        if (sep < BEFORE_SEP) {
            throw new NumberFormatException(
                parsingExceptionMsg("Expected separator between two numbers", FORMAT_SEP, s));
        }

        // Should be no more separators
        if (s.indexOf(FORMAT_SEP, sep + 1) != -1) {
            throw new NumberFormatException(
                parsingExceptionMsg("Incorrect number of parts, expected only 2 using separator",
                                    FORMAT_SEP, s));
        }

        // Try to parse the parts

        final String rePart = s.substring(1, sep);
        final double re;
        try {
            re = Double.parseDouble(rePart);
        } catch (final NumberFormatException ex) {
            throw new NumberFormatException(
                parsingExceptionMsg("Could not parse real part", rePart, s));
        }

        final String imPart = s.substring(sep + 1, len - 1);
        final double im;
        try {
            im = Double.parseDouble(imPart);
        } catch (final NumberFormatException ex) {
            throw new NumberFormatException(
                parsingExceptionMsg("Could not parse imaginary part", imPart, s));
        }

        return ofCartesian(re, im);
    }

    /**
     * Creates an exception message.
     *
     * @param message Message prefix.
     * @param error Input that caused the error.
     * @param s String representation.
     * @return A message.
     */
    private static String parsingExceptionMsg(String message,
                                              Object error,
                                              String s) {
        final StringBuilder sb = new StringBuilder(100)
            .append(message)
            .append(" '").append(error)
            .append("' for input \"").append(s).append('"');
        return sb.toString();
    }

    /**
     * Gets the real part \( a \) of this complex number \( (a + i b) \).
     *
     * @return The real part.
     */
    public double getReal() {
        return real;
    }

    /**
     * Gets the real part \( a \) of this complex number \( (a + i b) \).
     *
     * <p>This method is the equivalent of the C++ method {@code std::complex::real}.
     *
     * @return The real part.
     * @see #getReal()
     */
    public double real() {
        return getReal();
    }

    /**
     * Gets the imaginary part \( b \) of this complex number \( (a + i b) \).
     *
     * @return The imaginary part.
     */
    public double getImaginary() {
        return imaginary;
    }

    /**
     * Gets the imaginary part \( b \) of this complex number \( (a + i b) \).
     *
     * <p>This method is the equivalent of the C++ method {@code std::complex::imag}.
     *
     * @return The imaginary part.
     * @see #getImaginary()
     */
    public double imag() {
        return getImaginary();
    }

    /**
     * Returns the absolute value of this complex number. This is also called complex norm, modulus,
     * or magnitude.
     *
     * <p>\[ \text{abs}(x + i y) = \sqrt{(x^2 + y^2)} \]
     *
     * <p>Special cases:
     *
     * <ul>
     * <li>{@code abs(x + iy) == abs(y + ix) == abs(x - iy)}.
     * <li>If {@code z} is ±∞ + iy for any y, returns +∞.
     * <li>If {@code z} is x + iNaN for non-infinite x, returns NaN.
     * <li>If {@code z} is x + i0, returns |x|.
     * </ul>
     *
     * <p>The cases ensure that if either component is infinite then the result is positive
     * infinity. If either component is NaN and this is not {@link #isInfinite() infinite} then
     * the result is NaN.
     *
     * <p>This method follows the
     * <a href="http://www.iso-9899.info/wiki/The_Standard">ISO C Standard</a>, Annex G,
     * in calculating the returned value without intermediate overflow or underflow.
     *
     * <p>The computed result will be within 1 ulp of the exact result.
     *
     * @return The absolute value.
     * @see #isInfinite()
     * @see #isNaN()
     * @see <a href="http://mathworld.wolfram.com/ComplexModulus.html">Complex modulus</a>
     */
    public double abs() {
        return ComplexFunctions.abs(real, imaginary);
    }

    /**
     * Returns the argument of this complex number.
     *
     * <p>The argument is the angle phi between the positive real axis and
     * the point representing this number in the complex plane.
     * The value returned is between \( -\pi \) (not inclusive)
     * and \( \pi \) (inclusive), with negative values returned for numbers with
     * negative imaginary parts.
     *
     * <p>If either real or imaginary part (or both) is NaN, then the result is NaN.
     * Infinite parts are handled as {@linkplain Math#atan2} handles them,
     * essentially treating finite parts as zero in the presence of an
     * infinite coordinate and returning a multiple of \( \frac{\pi}{4} \) depending on
     * the signs of the infinite parts.
     *
     * <p>This code follows the
     * <a href="http://www.iso-9899.info/wiki/The_Standard">ISO C Standard</a>, Annex G,
     * in calculating the returned value using the {@code atan2(y, x)} method for complex
     * \( x + iy \).
     *
     * @return The argument of this complex number.
     * @see Math#atan2(double, double)
     */
    public double arg() {
        // Delegate
        return ComplexFunctions.arg(real, imaginary);
    }

    /**
     * Returns the squared norm value of this complex number. This is also called the absolute
     * square.
     *
     * <p>\[ \text{norm}(x + i y) = x^2 + y^2 \]
     *
     * <p>If either component is infinite then the result is positive infinity. If either
     * component is NaN and this is not {@link #isInfinite() infinite} then the result is NaN.
     *
     * <p>Note: This method may not return the same value as the square of {@link #abs()} as
     * that method uses an extended precision computation.
     *
     * <p>{@code norm()} can be used as a faster alternative than {@code abs()} for ranking by
     * magnitude. If used for ranking any overflow to infinity will create an equal ranking for
     * values that may be still distinguished by {@code abs()}.
     *
     * @return The square norm value.
     * @see #isInfinite()
     * @see #isNaN()
     * @see #abs()
     * @see <a href="http://mathworld.wolfram.com/AbsoluteSquare.html">Absolute square</a>
     */
    public double norm() {
        return ComplexFunctions.norm(real, imaginary);
    }

    /**
     * Returns {@code true} if either the real <em>or</em> imaginary component of the complex number is NaN
     * <em>and</em> the complex number is not infinite.
     *
     * <p>Note that:
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
        return ComplexFunctions.isNaN(real, imaginary);
    }

    /**
     * Returns {@code true} if either real or imaginary component of the complex number is infinite.
     *
     * <p>Note: A complex number with at least one infinite part is regarded
     * as an infinity (even if its other part is a NaN).
     *
     * @return {@code true} if this instance contains an infinite value.
     * @see Double#isInfinite(double)
     */
    public boolean isInfinite() {
        return ComplexFunctions.isInfinite(real, imaginary);
    }

    /**
     * Returns {@code true} if both real and imaginary component of the complex number are finite.
     *
     * @return {@code true} if this instance contains finite values.
     * @see Double#isFinite(double)
     */
    public boolean isFinite() {
        return ComplexFunctions.isFinite(real, imaginary);
    }

    /**
     * Returns the
     * <a href="http://mathworld.wolfram.com/ComplexConjugate.html">conjugate</a>
     * \( \overline{z} \) of this complex number \( z \).
     *
     * <p>\[ \begin{aligned}
     *                z  &amp;= a + i b \\
     *      \overline{z} &amp;= a - i b \end{aligned}\]
     *
     * @return The conjugate (\( \overline{z} \)) of this complex number.
     */
    public Complex conj() {
        return new Complex(real, -imaginary);
    }

    /**
     * Returns a {@code Complex} whose value is the negation of both the real and imaginary parts
     * of complex number \( z \).
     *
     * <p>\[ \begin{aligned}
     *       z  &amp;=  a + i b \\
     *      -z  &amp;= -a - i b \end{aligned} \]
     *
     * @return \( -z \).
     */
    public Complex negate() {
        return new Complex(-real, -imaginary);
    }

    /**
     * Returns the projection of this complex number onto the Riemann sphere.
     *
     * <p>\( z \) projects to \( z \), except that all complex infinities (even those
     * with one infinite part and one NaN part) project to positive infinity on the real axis.
     *
     * <p>If \( z \) has an infinite part, then {@code z.proj()} shall be equivalent to:
     *
     * <pre>return Complex.ofCartesian(Double.POSITIVE_INFINITY, Math.copySign(0.0, z.imag());</pre>
     *
     * @return \( z \) projected onto the Riemann sphere.
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
     * Returns a {@code Complex} whose value is {@code (this + addend)}.
     * Implements the formula:
     *
     * <p>\[ (a + i b) + (c + i d) = (a + c) + i (b + d) \]
     *
     * @param  addend Value to be added to this complex number.
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
     *
     * <p>\[ (a + i b) + c = (a + c) + i b \]
     *
     * <p>This method is included for compatibility with ISO C99 which defines arithmetic between
     * real-only and complex numbers.</p>
     *
     * <p>Note: This method preserves the sign of the imaginary component \( b \) if it is {@code -0.0}.
     * The sign would be lost if adding \( (c + i 0) \) using
     * {@link #add(Complex) add(Complex.ofCartesian(addend, 0))} since
     * {@code -0.0 + 0.0 = 0.0}.
     *
     * @param addend Value to be added to this complex number.
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
     *
     * <p>\[ (a + i b) + i d = a + i (b + d) \]
     *
     * <p>This method is included for compatibility with ISO C99 which defines arithmetic between
     * imaginary-only and complex numbers.</p>
     *
     * <p>Note: This method preserves the sign of the real component \( a \) if it is {@code -0.0}.
     * The sign would be lost if adding \( (0 + i d) \) using
     * {@link #add(Complex) add(Complex.ofCartesian(0, addend))} since
     * {@code -0.0 + 0.0 = 0.0}.
     *
     * @param addend Value to be added to this complex number.
     * @return {@code this + addend}.
     * @see #add(Complex)
     * @see #ofCartesian(double, double)
     */
    public Complex addImaginary(double addend) {
        return new Complex(real, imaginary + addend);
    }

    /**
     * Returns a {@code Complex} whose value is {@code (this - subtrahend)}.
     * Implements the formula:
     *
     * <p>\[ (a + i b) - (c + i d) = (a - c) + i (b - d) \]
     *
     * @param  subtrahend Value to be subtracted from this complex number.
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
     *
     * <p>\[ (a + i b) - c = (a - c) + i b \]
     *
     * <p>This method is included for compatibility with ISO C99 which defines arithmetic between
     * real-only and complex numbers.</p>
     *
     * @param  subtrahend Value to be subtracted from this complex number.
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
     *
     * <p>\[ (a + i b) - i d = a + i (b - d) \]
     *
     * <p>This method is included for compatibility with ISO C99 which defines arithmetic between
     * imaginary-only and complex numbers.</p>
     *
     * @param  subtrahend Value to be subtracted from this complex number.
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
     * \[ c - (a + i b) = (c - a) - i b \]
     *
     * <p>This method is included for compatibility with ISO C99 which defines arithmetic between
     * real-only and complex numbers.</p>
     *
     * <p>Note: This method inverts the sign of the imaginary component \( b \) if it is {@code 0.0}.
     * The sign would not be inverted if subtracting from \( c + i 0 \) using
     * {@link #subtract(Complex) Complex.ofCartesian(minuend, 0).subtract(this)} since
     * {@code 0.0 - 0.0 = 0.0}.
     *
     * @param  minuend Value this complex number is to be subtracted from.
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
     * \[ i d - (a + i b) = -a + i (d - b) \]
     *
     * <p>This method is included for compatibility with ISO C99 which defines arithmetic between
     * imaginary-only and complex numbers.</p>
     *
     * <p>Note: This method inverts the sign of the real component \( a \) if it is {@code 0.0}.
     * The sign would not be inverted if subtracting from \( 0 + i d \) using
     * {@link #subtract(Complex) Complex.ofCartesian(0, minuend).subtract(this)} since
     * {@code 0.0 - 0.0 = 0.0}.
     *
     * @param  minuend Value this complex number is to be subtracted from.
     * @return {@code this - subtrahend}.
     * @see #subtract(Complex)
     * @see #ofCartesian(double, double)
     */
    public Complex subtractFromImaginary(double minuend) {
        return new Complex(-real, minuend - imaginary);
    }

    /**
     * Returns a {@code Complex} whose value is {@code this * factor}.
     * Implements the formula:
     *
     * <p>\[ (a + i b)(c + i d) = (ac - bd) + i (ad + bc) \]
     *
     * <p>Recalculates to recover infinities as specified in C99 standard G.5.1.
     *
     * @param  factor Value to be multiplied by this complex number.
     * @return {@code this * factor}.
     * @see <a href="http://mathworld.wolfram.com/ComplexMultiplication.html">Complex Muliplication</a>
     */
    public Complex multiply(Complex factor) {
        return multiply(real, imaginary, factor.real, factor.imaginary);
    }

    /**
     * Returns a {@code Complex} whose value is:
     * <pre>
     *  (a + i b)(c + i d) = (ac - bd) + i (ad + bc)</pre>
     *
     * <p>Recalculates to recover infinities as specified in C99 standard G.5.1.
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
        // Modification from the listing in ISO C99 G.5.1 (6)
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
            if ((Double.isInfinite(c) || Double.isInfinite(d)) &&
                isNotZero(a, b)) {
                // The other complex is infinite.
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
     * @return The boxed value
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
     * @return The new value
     */
    private static double changeNaNtoZero(double value) {
        return Double.isNaN(value) ? Math.copySign(0.0, value) : value;
    }

    /**
     * Returns a {@code Complex} whose value is {@code this * factor}, with {@code factor}
     * interpreted as a real number.
     * Implements the formula:
     *
     * <p>\[ (a + i b) c =  (ac) + i (bc) \]
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
     * @param  factor Value to be multiplied by this complex number.
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
     *
     * <p>\[ (a + i b) id = (-bd) + i (ad) \]
     *
     * <p>This method can be used to compute the multiplication of this complex number \( z \)
     * by \( i \) using a factor with magnitude 1.0. This should be used in preference to
     * {@link #multiply(Complex) multiply(Complex.I)} with or without {@link #negate() negation}:</p>
     *
     * \[ \begin{aligned}
     *    iz &amp;= (-b + i a) \\
     *   -iz &amp;= (b - i a) \end{aligned} \]
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
     * @param  factor Value to be multiplied by this complex number.
     * @return {@code this * factor}.
     * @see #multiply(Complex)
     */
    public Complex multiplyImaginary(double factor) {
        return new Complex(-imaginary * factor, real * factor);
    }

    /**
     * Returns a {@code Complex} whose value is {@code (this / divisor)}.
     * Implements the formula:
     *
     * <p>\[ \frac{a + i b}{c + i d} = \frac{(ac + bd) + i (bc - ad)}{c^2+d^2} \]
     *
     * <p>Re-calculates NaN result values to recover infinities as specified in C99 standard G.5.1.
     *
     * @param divisor Value by which this complex number is to be divided.
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
     * <p>Recalculates to recover infinities as specified in C99
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
        // Get the exponent to scale the divisor parts to the range [1, 2).
        final int exponent = getScale(c, d);
        if (exponent <= Double.MAX_EXPONENT) {
            ilogbw = exponent;
            c = Math.scalb(c, -ilogbw);
            d = Math.scalb(d, -ilogbw);
        }
        final double denom = c * c + d * d;

        // Note: Modification from the listing in ISO C99 G.5.1 (8):
        // Avoid overflow if a or b are very big.
        // Since (c, d) in the range [1, 2) the sum (ac + bd) could overflow
        // when (a, b) are both above (Double.MAX_VALUE / 4). The same applies to
        // (bc - ad) with large negative values.
        // Use the maximum exponent as an approximation to the magnitude.
        if (getMaxExponent(a, b) > Double.MAX_EXPONENT - 2) {
            ilogbw -= 2;
            a /= 4;
            b /= 4;
        }

        double x = Math.scalb((a * c + b * d) / denom, -ilogbw);
        double y = Math.scalb((b * c - a * d) / denom, -ilogbw);
        // Recover infinities and zeros that computed as NaN+iNaN
        // the only cases are nonzero/zero, infinite/finite, and finite/infinite, ...
        if (Double.isNaN(x) && Double.isNaN(y)) {
            if (denom == 0.0 &&
                    (!Double.isNaN(a) || !Double.isNaN(b))) {
                // nonzero/zero
                // This case produces the same result as divide by a real-only zero
                // using Complex.divide(+/-0.0)
                x = Math.copySign(Double.POSITIVE_INFINITY, c) * a;
                y = Math.copySign(Double.POSITIVE_INFINITY, c) * b;
            } else if ((Double.isInfinite(a) || Double.isInfinite(b)) &&
                    Double.isFinite(c) && Double.isFinite(d)) {
                // infinite/finite
                a = boxInfinity(a);
                b = boxInfinity(b);
                x = Double.POSITIVE_INFINITY * (a * c + b * d);
                y = Double.POSITIVE_INFINITY * (b * c - a * d);
            } else if ((Double.isInfinite(c) || Double.isInfinite(d)) &&
                    Double.isFinite(a) && Double.isFinite(b)) {
                // finite/infinite
                c = boxInfinity(c);
                d = boxInfinity(d);
                x = 0.0 * (a * c + b * d);
                y = 0.0 * (b * c - a * d);
            }
        }
        return new Complex(x, y);
    }

    /**
     * Returns a {@code Complex} whose value is {@code (this / divisor)},
     * with {@code divisor} interpreted as a real number.
     * Implements the formula:
     *
     * <p>\[ \frac{a + i b}{c} = \frac{a}{c} + i \frac{b}{c} \]
     *
     * <p>This method is included for compatibility with ISO C99 which defines arithmetic between
     * real-only and complex numbers.</p>
     *
     * <p>Note: This method should be preferred over using
     * {@link #divide(Complex) divide(Complex.ofCartesian(divisor, 0))}. Division
     * can generate signed zeros if {@code this} complex has zeros for the real
     * and/or imaginary component, or the divisor is infinite. The summation of signed zeros
     * in {@link #divide(Complex)} may create zeros in the result that differ in sign
     * from the equivalent call to divide by a real-only number.
     *
     * @param  divisor Value by which this complex number is to be divided.
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
     *
     * <p>\[ \frac{a + i b}{id} = \frac{b}{d} - i \frac{a}{d} \]
     *
     * <p>This method is included for compatibility with ISO C99 which defines arithmetic between
     * imaginary-only and complex numbers.</p>
     *
     * <p>Note: This method should be preferred over using
     * {@link #divide(Complex) divide(Complex.ofCartesian(0, divisor))}. Division
     * can generate signed zeros if {@code this} complex has zeros for the real
     * and/or imaginary component, or the divisor is infinite. The summation of signed zeros
     * in {@link #divide(Complex)} may create zeros in the result that differ in sign
     * from the equivalent call to divide by an imaginary-only number.
     *
     * <p>Warning: This method will generate a different result from
     * {@link #divide(Complex) divide(Complex.ofCartesian(0, divisor))} if the divisor is zero.
     * In this case the divide method using a zero-valued Complex will produce the same result
     * as dividing by a real-only zero. The output from dividing by imaginary zero will create
     * infinite and NaN values in the same component parts as the output from
     * {@code this.divide(Complex.ZERO).multiplyImaginary(1)}, however the sign
     * of some infinite values may be negated.
     *
     * @param  divisor Value by which this complex number is to be divided.
     * @return {@code this / divisor}.
     * @see #divide(Complex)
     * @see #divide(double)
     */
    public Complex divideImaginary(double divisor) {
        return new Complex(imaginary / divisor, -real / divisor);
    }

    /**
     * Returns the
     * <a href="http://mathworld.wolfram.com/ExponentialFunction.html">
     * exponential function</a> of this complex number.
     *
     * <p>\[ \exp(z) = e^z \]
     *
     * <p>The exponential function of \( z \) is an entire function in the complex plane.
     * Special cases:
     *
     * <ul>
     * <li>{@code z.conj().exp() == z.exp().conj()}.
     * <li>If {@code z} is ±0 + i0, returns 1 + i0.
     * <li>If {@code z} is x + i∞ for finite x, returns NaN + iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is x + iNaN for finite x, returns NaN + iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is +∞ + i0, returns +∞ + i0.
     * <li>If {@code z} is −∞ + iy for finite y, returns +0 cis(y) (see {@link #ofCis(double)}).
     * <li>If {@code z} is +∞ + iy for finite nonzero y, returns +∞ cis(y).
     * <li>If {@code z} is −∞ + i∞, returns ±0 ± i0 (where the signs of the real and imaginary parts of the result are unspecified).
     * <li>If {@code z} is +∞ + i∞, returns ±∞ + iNaN (where the sign of the real part of the result is unspecified; "invalid" floating-point operation).
     * <li>If {@code z} is −∞ + iNaN, returns ±0 ± i0 (where the signs of the real and imaginary parts of the result are unspecified).
     * <li>If {@code z} is +∞ + iNaN, returns ±∞ + iNaN (where the sign of the real part of the result is unspecified).
     * <li>If {@code z} is NaN + i0, returns NaN + i0.
     * <li>If {@code z} is NaN + iy for all nonzero numbers y, returns NaN + iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is NaN + iNaN, returns NaN + iNaN.
     * </ul>
     *
     * <p>Implements the formula:
     *
     * <p>\[ \exp(x + iy) = e^x (\cos(y) + i \sin(y)) \]
     *
     * @return The exponential of this complex number.
     * @see <a href="http://functions.wolfram.com/ElementaryFunctions/Exp/">Exp</a>
     */
    public Complex exp() {
        return ComplexFunctions.exp(real, imaginary, Complex::ofCartesian);
    }

    /**
     * Returns the
     * <a href="http://mathworld.wolfram.com/NaturalLogarithm.html">
     * natural logarithm</a> of this complex number.
     *
     * <p>The natural logarithm of \( z \) is unbounded along the real axis and
     * in the range \( [-\pi, \pi] \) along the imaginary axis. The imaginary part of the
     * natural logarithm has a branch cut along the negative real axis \( (-infty,0] \).
     * Special cases:
     *
     * <ul>
     * <li>{@code z.conj().log() == z.log().conj()}.
     * <li>If {@code z} is −0 + i0, returns −∞ + iπ ("divide-by-zero" floating-point operation).
     * <li>If {@code z} is +0 + i0, returns −∞ + i0 ("divide-by-zero" floating-point operation).
     * <li>If {@code z} is x + i∞ for finite x, returns +∞ + iπ/2.
     * <li>If {@code z} is x + iNaN for finite x, returns NaN + iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is −∞ + iy for finite positive-signed y, returns +∞ + iπ.
     * <li>If {@code z} is +∞ + iy for finite positive-signed y, returns +∞ + i0.
     * <li>If {@code z} is −∞ + i∞, returns +∞ + i3π/4.
     * <li>If {@code z} is +∞ + i∞, returns +∞ + iπ/4.
     * <li>If {@code z} is ±∞ + iNaN, returns +∞ + iNaN.
     * <li>If {@code z} is NaN + iy for finite y, returns NaN + iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is NaN + i∞, returns +∞ + iNaN.
     * <li>If {@code z} is NaN + iNaN, returns NaN + iNaN.
     * </ul>
     *
     * <p>Implements the formula:
     *
     * <p>\[ \ln(z) = \ln |z| + i \arg(z) \]
     *
     * <p>where \( |z| \) is the absolute and \( \arg(z) \) is the argument.
     *
     * <p>The implementation is based on the method described in:</p>
     * <blockquote>
     * T E Hull, Thomas F Fairgrieve and Ping Tak Peter Tang (1994)
     * Implementing complex elementary functions using exception handling.
     * ACM Transactions on Mathematical Software, Vol 20, No 2, pp 215-244.
     * </blockquote>
     *
     * @return The natural logarithm of this complex number.
     * @see Math#log(double)
     * @see #abs()
     * @see #arg()
     * @see <a href="http://functions.wolfram.com/ElementaryFunctions/Log/">Log</a>
     */
    public Complex log() {
        return ComplexFunctions.log(real, imaginary, Complex::ofCartesian);
    }

    /**
     * Returns the base 10
     * <a href="http://mathworld.wolfram.com/CommonLogarithm.html">
     * common logarithm</a> of this complex number.
     *
     * <p>The common logarithm of \( z \) is unbounded along the real axis and
     * in the range \( [-\pi, \pi] \) along the imaginary axis. The imaginary part of the
     * common logarithm has a branch cut along the negative real axis \( (-infty,0] \).
     * Special cases are as defined in the {@link #log() natural logarithm}:
     *
     * <p>Implements the formula:
     *
     * <p>\[ \log_{10}(z) = \log_{10} |z| + i \arg(z) \]
     *
     * <p>where \( |z| \) is the absolute and \( \arg(z) \) is the argument.
     *
     * @return The base 10 logarithm of this complex number.
     * @see Math#log10(double)
     * @see #abs()
     * @see #arg()
     */
    public Complex log10() {
        return ComplexFunctions.log10(real, imaginary, Complex::ofCartesian);
    }

    /**
     * Returns the complex power of this complex number raised to the power of {@code x}.
     * Implements the formula:
     *
     * <p>\[ z^x = e^{x \ln(z)} \]
     *
     * <p>If this complex number is zero then this method returns zero if {@code x} is positive
     * in the real component and zero in the imaginary component;
     * otherwise it returns NaN + iNaN.
     *
     * @param  x The exponent to which this complex number is to be raised.
     * @return This complex number raised to the power of {@code x}.
     * @see #log()
     * @see #multiply(Complex)
     * @see #exp()
     * @see <a href="http://mathworld.wolfram.com/ComplexExponentiation.html">Complex exponentiation</a>
     * @see <a href="http://functions.wolfram.com/ElementaryFunctions/Power/">Power</a>
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
     * Returns the complex power of this complex number raised to the power of {@code x},
     * with {@code x} interpreted as a real number.
     * Implements the formula:
     *
     * <p>\[ z^x = e^{x \ln(z)} \]
     *
     * <p>If this complex number is zero then this method returns zero if {@code x} is positive;
     * otherwise it returns NaN + iNaN.
     *
     * @param  x The exponent to which this complex number is to be raised.
     * @return This complex number raised to the power of {@code x}.
     * @see #log()
     * @see #multiply(double)
     * @see #exp()
     * @see #pow(Complex)
     * @see <a href="http://functions.wolfram.com/ElementaryFunctions/Power/">Power</a>
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
     * Returns the
     * <a href="http://mathworld.wolfram.com/SquareRoot.html">
     * square root</a> of this complex number.
     *
     * <p>\[ \sqrt{x + iy} = \frac{1}{2} \sqrt{2} \left( \sqrt{ \sqrt{x^2 + y^2} + x } + i\ \text{sgn}(y) \sqrt{ \sqrt{x^2 + y^2} - x } \right) \]
     *
     * <p>The square root of \( z \) is in the range \( [0, +\infty) \) along the real axis and
     * is unbounded along the imaginary axis. The imaginary part of the square root has a
     * branch cut along the negative real axis \( (-infty,0) \). Special cases:
     *
     * <ul>
     * <li>{@code z.conj().sqrt() == z.sqrt().conj()}.
     * <li>If {@code z} is ±0 + i0, returns +0 + i0.
     * <li>If {@code z} is x + i∞ for all x (including NaN), returns +∞ + i∞.
     * <li>If {@code z} is x + iNaN for finite x, returns NaN + iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is −∞ + iy for finite positive-signed y, returns +0 + i∞.
     * <li>If {@code z} is +∞ + iy for finite positive-signed y, returns +∞ + i0.
     * <li>If {@code z} is −∞ + iNaN, returns NaN ± i∞ (where the sign of the imaginary part of the result is unspecified).
     * <li>If {@code z} is +∞ + iNaN, returns +∞ + iNaN.
     * <li>If {@code z} is NaN + iy for finite y, returns NaN + iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is NaN + iNaN, returns NaN + iNaN.
     * </ul>
     *
     * <p>Implements the following algorithm to compute \( \sqrt{x + iy} \):
     * <ol>
     * <li>Let \( t = \sqrt{2 (|x| + |x + iy|)} \)
     * <li>if \( x \geq 0 \) return \( \frac{t}{2} + i \frac{y}{t} \)
     * <li>else return \( \frac{|y|}{t} + i\ \text{sgn}(y) \frac{t}{2} \)
     * </ol>
     * where:
     * <ul>
     * <li>\( |x| =\ \){@link Math#abs(double) abs}(x)
     * <li>\( |x + y i| =\ \){@link Complex#abs}
     * <li>\( \text{sgn}(y) =\ \){@link Math#copySign(double,double) copySign}(1.0, y)
     * </ul>
     *
     * <p>The implementation is overflow and underflow safe based on the method described in:</p>
     * <blockquote>
     * T E Hull, Thomas F Fairgrieve and Ping Tak Peter Tang (1994)
     * Implementing complex elementary functions using exception handling.
     * ACM Transactions on Mathematical Software, Vol 20, No 2, pp 215-244.
     * </blockquote>
     *
     * @return The square root of this complex number.
     * @see <a href="http://functions.wolfram.com/ElementaryFunctions/Sqrt/">Sqrt</a>
     */
    public Complex sqrt() {
        return ComplexFunctions.sqrt(real, imaginary, Complex::ofCartesian);
    }

    /**
     * Returns the
     * <a href="http://mathworld.wolfram.com/Sine.html">
     * sine</a> of this complex number.
     *
     * <p>\[ \sin(z) = \frac{1}{2} i \left( e^{-iz} - e^{iz} \right) \]
     *
     * <p>This is an odd function: \( \sin(z) = -\sin(-z) \).
     * The sine is an entire function and requires no branch cuts.
     *
     * <p>This is implemented using real \( x \) and imaginary \( y \) parts:
     *
     * <p>\[ \sin(x + iy) = \sin(x)\cosh(y) + i \cos(x)\sinh(y) \]
     *
     * <p>As per the C99 standard this function is computed using the trigonomic identity:
     *
     * <p>\[ \sin(z) = -i \sinh(iz) \]
     *
     * @return The sine of this complex number.
     * @see <a href="http://functions.wolfram.com/ElementaryFunctions/Sin/">Sin</a>
     */
    public Complex sin() {
        return ComplexFunctions.sin(real, imaginary, Complex::ofCartesian);
    }

    /**
     * Returns the
     * <a href="http://mathworld.wolfram.com/Cosine.html">
     * cosine</a> of this complex number.
     *
     * <p>\[ \cos(z) = \frac{1}{2} \left( e^{iz} + e^{-iz} \right) \]
     *
     * <p>This is an even function: \( \cos(z) = \cos(-z) \).
     * The cosine is an entire function and requires no branch cuts.
     *
     * <p>This is implemented using real \( x \) and imaginary \( y \) parts:
     *
     * <p>\[ \cos(x + iy) = \cos(x)\cosh(y) - i \sin(x)\sinh(y) \]
     *
     * <p>As per the C99 standard this function is computed using the trigonomic identity:
     *
     * <p>\[ cos(z) = cosh(iz) \]
     *
     * @return The cosine of this complex number.
     * @see <a href="http://functions.wolfram.com/ElementaryFunctions/Cos/">Cos</a>
     */
    public Complex cos() {
        return ComplexFunctions.cos(real, imaginary, Complex::ofCartesian);
    }

    /**
     * Returns the
     * <a href="http://mathworld.wolfram.com/Tangent.html">
     * tangent</a> of this complex number.
     *
     * <p>\[ \tan(z) = \frac{i(e^{-iz} - e^{iz})}{e^{-iz} + e^{iz}} \]
     *
     * <p>This is an odd function: \( \tan(z) = -\tan(-z) \).
     * The tangent is an entire function and requires no branch cuts.
     *
     * <p>This is implemented using real \( x \) and imaginary \( y \) parts:</p>
     * \[ \tan(x + iy) = \frac{\sin(2x)}{\cos(2x)+\cosh(2y)} + i \frac{\sinh(2y)}{\cos(2x)+\cosh(2y)} \]
     *
     * <p>As per the C99 standard this function is computed using the trigonomic identity:</p>
     * \[ \tan(z) = -i \tanh(iz) \]
     *
     * @return The tangent of this complex number.
     * @see <a href="http://functions.wolfram.com/ElementaryFunctions/Tan/">Tangent</a>
     */
    public Complex tan() {
        return ComplexFunctions.tan(real, imaginary, Complex::ofCartesian);
    }

    /**
     * Returns the
     * <a href="http://mathworld.wolfram.com/InverseSine.html">
     * inverse sine</a> of this complex number.
     *
     * <p>\[ \sin^{-1}(z) = - i \left(\ln{iz + \sqrt{1 - z^2}}\right) \]
     *
     * <p>The inverse sine of \( z \) is unbounded along the imaginary axis and
     * in the range \( [-\pi, \pi] \) along the real axis. Special cases are handled
     * as if the operation is implemented using \( \sin^{-1}(z) = -i \sinh^{-1}(iz) \).
     *
     * <p>The inverse sine is a multivalued function and requires a branch cut in
     * the complex plane; the cut is conventionally placed at the line segments
     * \( (\infty,-1) \) and \( (1,\infty) \) of the real axis.
     *
     * <p>This is implemented using real \( x \) and imaginary \( y \) parts:
     *
     * <p>\[ \begin{aligned}
     *   \sin^{-1}(z) &amp;= \sin^{-1}(B) + i\ \text{sgn}(y)\ln \left(A + \sqrt{A^2-1} \right) \\
     *   A &amp;= \frac{1}{2} \left[ \sqrt{(x+1)^2+y^2} + \sqrt{(x-1)^2+y^2} \right] \\
     *   B &amp;= \frac{1}{2} \left[ \sqrt{(x+1)^2+y^2} - \sqrt{(x-1)^2+y^2} \right] \end{aligned} \]
     *
     * <p>where \( \text{sgn}(y) \) is the sign function implemented using
     * {@link Math#copySign(double,double) copySign(1.0, y)}.
     *
     * <p>The implementation is based on the method described in:</p>
     * <blockquote>
     * T E Hull, Thomas F Fairgrieve and Ping Tak Peter Tang (1997)
     * Implementing the complex Arcsine and Arccosine Functions using Exception Handling.
     * ACM Transactions on Mathematical Software, Vol 23, No 3, pp 299-335.
     * </blockquote>
     *
     * <p>The code has been adapted from the <a href="https://www.boost.org/">Boost</a>
     * {@code c++} implementation {@code <boost/math/complex/asin.hpp>}.
     *
     * @return The inverse sine of this complex number.
     * @see <a href="http://functions.wolfram.com/ElementaryFunctions/ArcSin/">ArcSin</a>
     */
    public Complex asin() {
        return ComplexFunctions.asin(real, imaginary, Complex::ofCartesian);
    }

    /**
     * Returns the
     * <a href="http://mathworld.wolfram.com/InverseCosine.html">
     * inverse cosine</a> of this complex number.
     *
     * <p>\[ \cos^{-1}(z) = \frac{\pi}{2} + i \left(\ln{iz + \sqrt{1 - z^2}}\right) \]
     *
     * <p>The inverse cosine of \( z \) is in the range \( [0, \pi) \) along the real axis and
     * unbounded along the imaginary axis. Special cases:
     *
     * <ul>
     * <li>{@code z.conj().acos() == z.acos().conj()}.
     * <li>If {@code z} is ±0 + i0, returns π/2 − i0.
     * <li>If {@code z} is ±0 + iNaN, returns π/2 + iNaN.
     * <li>If {@code z} is x + i∞ for finite x, returns π/2 − i∞.
     * <li>If {@code z} is x + iNaN, returns NaN + iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is −∞ + iy for positive-signed finite y, returns π − i∞.
     * <li>If {@code z} is +∞ + iy for positive-signed finite y, returns +0 − i∞.
     * <li>If {@code z} is −∞ + i∞, returns 3π/4 − i∞.
     * <li>If {@code z} is +∞ + i∞, returns π/4 − i∞.
     * <li>If {@code z} is ±∞ + iNaN, returns NaN ± i∞ where the sign of the imaginary part of the result is unspecified.
     * <li>If {@code z} is NaN + iy for finite y, returns NaN + iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is NaN + i∞, returns NaN − i∞.
     * <li>If {@code z} is NaN + iNaN, returns NaN + iNaN.
     * </ul>
     *
     * <p>The inverse cosine is a multivalued function and requires a branch cut in
     * the complex plane; the cut is conventionally placed at the line segments
     * \( (-\infty,-1) \) and \( (1,\infty) \) of the real axis.
     *
     * <p>This function is implemented using real \( x \) and imaginary \( y \) parts:
     *
     * <p>\[ \begin{aligned}
     *   \cos^{-1}(z) &amp;= \cos^{-1}(B) - i\ \text{sgn}(y) \ln\left(A + \sqrt{A^2-1}\right) \\
     *   A &amp;= \frac{1}{2} \left[ \sqrt{(x+1)^2+y^2} + \sqrt{(x-1)^2+y^2} \right] \\
     *   B &amp;= \frac{1}{2} \left[ \sqrt{(x+1)^2+y^2} - \sqrt{(x-1)^2+y^2} \right] \end{aligned} \]
     *
     * <p>where \( \text{sgn}(y) \) is the sign function implemented using
     * {@link Math#copySign(double,double) copySign(1.0, y)}.
     *
     * <p>The implementation is based on the method described in:</p>
     * <blockquote>
     * T E Hull, Thomas F Fairgrieve and Ping Tak Peter Tang (1997)
     * Implementing the complex Arcsine and Arccosine Functions using Exception Handling.
     * ACM Transactions on Mathematical Software, Vol 23, No 3, pp 299-335.
     * </blockquote>
     *
     * <p>The code has been adapted from the <a href="https://www.boost.org/">Boost</a>
     * {@code c++} implementation {@code <boost/math/complex/acos.hpp>}.
     *
     * @return The inverse cosine of this complex number.
     * @see <a href="http://functions.wolfram.com/ElementaryFunctions/ArcCos/">ArcCos</a>
     */
    public Complex acos() {
        return ComplexFunctions.acos(real, imaginary, Complex::ofCartesian);
    }

    /**
     * Returns the
     * <a href="http://mathworld.wolfram.com/InverseTangent.html">
     * inverse tangent</a> of this complex number.
     *
     * <p>\[ \tan^{-1}(z) = \frac{i}{2} \ln \left( \frac{i + z}{i - z} \right) \]
     *
     * <p>The inverse hyperbolic tangent of \( z \) is unbounded along the imaginary axis and
     * in the range \( [-\pi/2, \pi/2] \) along the real axis.
     *
     * <p>The inverse tangent is a multivalued function and requires a branch cut in
     * the complex plane; the cut is conventionally placed at the line segments
     * \( (i \infty,-i] \) and \( [i,i \infty) \) of the imaginary axis.
     *
     * <p>As per the C99 standard this function is computed using the trigonomic identity:
     * \[ \tan^{-1}(z) = -i \tanh^{-1}(iz) \]
     *
     * @return The inverse tangent of this complex number.
     * @see <a href="http://functions.wolfram.com/ElementaryFunctions/ArcTan/">ArcTan</a>
     */
    public Complex atan() {
        return ComplexFunctions.atan(real, imaginary, Complex::ofCartesian);
    }

    /**
     * Returns the
     * <a href="http://mathworld.wolfram.com/HyperbolicSine.html">
     * hyperbolic sine</a> of this complex number.
     *
     * <p>\[ \sinh(z) = \frac{1}{2} \left( e^{z} - e^{-z} \right) \]
     *
     * <p>The hyperbolic sine of \( z \) is an entire function in the complex plane
     * and is periodic with respect to the imaginary component with period \( 2\pi i \).
     * Special cases:
     *
     * <ul>
     * <li>{@code z.conj().sinh() == z.sinh().conj()}.
     * <li>This is an odd function: \( \sinh(z) = -\sinh(-z) \).
     * <li>If {@code z} is +0 + i0, returns +0 + i0.
     * <li>If {@code z} is +0 + i∞, returns ±0 + iNaN (where the sign of the real part of the result is unspecified; "invalid" floating-point operation).
     * <li>If {@code z} is +0 + iNaN, returns ±0 + iNaN (where the sign of the real part of the result is unspecified).
     * <li>If {@code z} is x + i∞ for positive finite x, returns NaN + iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is x + iNaN for finite nonzero x, returns NaN + iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is +∞ + i0, returns +∞ + i0.
     * <li>If {@code z} is +∞ + iy for positive finite y, returns +∞ cis(y) (see {@link #ofCis(double)}.
     * <li>If {@code z} is +∞ + i∞, returns ±∞ + iNaN (where the sign of the real part of the result is unspecified; "invalid" floating-point operation).
     * <li>If {@code z} is +∞ + iNaN, returns ±∞ + iNaN (where the sign of the real part of the result is unspecified).
     * <li>If {@code z} is NaN + i0, returns NaN + i0.
     * <li>If {@code z} is NaN + iy for all nonzero numbers y, returns NaN + iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is NaN + iNaN, returns NaN + iNaN.
     * </ul>
     *
     * <p>This is implemented using real \( x \) and imaginary \( y \) parts:
     *
     * <p>\[ \sinh(x + iy) = \sinh(x)\cos(y) + i \cosh(x)\sin(y) \]
     *
     * @return The hyperbolic sine of this complex number.
     * @see <a href="http://functions.wolfram.com/ElementaryFunctions/Sinh/">Sinh</a>
     */
    public Complex sinh() {
        return ComplexFunctions.sinh(real, imaginary, Complex::ofCartesian);
    }

    /**
     * Returns the
     * <a href="http://mathworld.wolfram.com/HyperbolicCosine.html">
     * hyperbolic cosine</a> of this complex number.
     *
     * <p>\[ \cosh(z) = \frac{1}{2} \left( e^{z} + e^{-z} \right) \]
     *
     * <p>The hyperbolic cosine of \( z \) is an entire function in the complex plane
     * and is periodic with respect to the imaginary component with period \( 2\pi i \).
     * Special cases:
     *
     * <ul>
     * <li>{@code z.conj().cosh() == z.cosh().conj()}.
     * <li>This is an even function: \( \cosh(z) = \cosh(-z) \).
     * <li>If {@code z} is +0 + i0, returns 1 + i0.
     * <li>If {@code z} is +0 + i∞, returns NaN ± i0 (where the sign of the imaginary part of the result is unspecified; "invalid" floating-point operation).
     * <li>If {@code z} is +0 + iNaN, returns NaN ± i0 (where the sign of the imaginary part of the result is unspecified).
     * <li>If {@code z} is x + i∞ for finite nonzero x, returns NaN + iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is x + iNaN for finite nonzero x, returns NaN + iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is +∞ + i0, returns +∞ + i0.
     * <li>If {@code z} is +∞ + iy for finite nonzero y, returns +∞ cis(y) (see {@link #ofCis(double)}).
     * <li>If {@code z} is +∞ + i∞, returns ±∞ + iNaN (where the sign of the real part of the result is unspecified).
     * <li>If {@code z} is +∞ + iNaN, returns +∞ + iNaN.
     * <li>If {@code z} is NaN + i0, returns NaN ± i0 (where the sign of the imaginary part of the result is unspecified).
     * <li>If {@code z} is NaN + iy for all nonzero numbers y, returns NaN + iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is NaN + iNaN, returns NaN + iNaN.
     * </ul>
     *
     * <p>This is implemented using real \( x \) and imaginary \( y \) parts:
     *
     * <p>\[ \cosh(x + iy) = \cosh(x)\cos(y) + i \sinh(x)\sin(y) \]
     *
     * @return The hyperbolic cosine of this complex number.
     * @see <a href="http://functions.wolfram.com/ElementaryFunctions/Cosh/">Cosh</a>
     */
    public Complex cosh() {
        return ComplexFunctions.cosh(real, imaginary, Complex::ofCartesian);
    }

    /**
     * Returns the
     * <a href="http://mathworld.wolfram.com/HyperbolicTangent.html">
     * hyperbolic tangent</a> of this complex number.
     *
     * <p>\[ \tanh(z) = \frac{e^z - e^{-z}}{e^z + e^{-z}} \]
     *
     * <p>The hyperbolic tangent of \( z \) is an entire function in the complex plane
     * and is periodic with respect to the imaginary component with period \( \pi i \)
     * and has poles of the first order along the imaginary line, at coordinates
     * \( (0, \pi(\frac{1}{2} + n)) \).
     * Note that the {@code double} floating-point representation is unable to exactly represent
     * \( \pi/2 \) and there is no value for which a pole error occurs. Special cases:
     *
     * <ul>
     * <li>{@code z.conj().tanh() == z.tanh().conj()}.
     * <li>This is an odd function: \( \tanh(z) = -\tanh(-z) \).
     * <li>If {@code z} is +0 + i0, returns +0 + i0.
     * <li>If {@code z} is 0 + i∞, returns 0 + iNaN.
     * <li>If {@code z} is x + i∞ for finite non-zero x, returns NaN + iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is 0 + iNaN, returns 0 + iNAN.
     * <li>If {@code z} is x + iNaN for finite non-zero x, returns NaN + iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is +∞ + iy for positive-signed finite y, returns 1 + i0 sin(2y).
     * <li>If {@code z} is +∞ + i∞, returns 1 ± i0 (where the sign of the imaginary part of the result is unspecified).
     * <li>If {@code z} is +∞ + iNaN, returns 1 ± i0 (where the sign of the imaginary part of the result is unspecified).
     * <li>If {@code z} is NaN + i0, returns NaN + i0.
     * <li>If {@code z} is NaN + iy for all nonzero numbers y, returns NaN + iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is NaN + iNaN, returns NaN + iNaN.
     * </ul>
     *
     * <p>Special cases include the technical corrigendum
     * <a href="http://www.open-std.org/jtc1/sc22/wg14/www/docs/n1892.htm#dr_471">
     * DR 471: Complex math functions cacosh and ctanh</a>.
     *
     * <p>This is defined using real \( x \) and imaginary \( y \) parts:
     *
     * <p>\[ \tan(x + iy) = \frac{\sinh(2x)}{\cosh(2x)+\cos(2y)} + i \frac{\sin(2y)}{\cosh(2x)+\cos(2y)} \]
     *
     * <p>The implementation uses double-angle identities to avoid overflow of {@code 2x}
     * and {@code 2y}.
     *
     * @return The hyperbolic tangent of this complex number.
     * @see <a href="http://functions.wolfram.com/ElementaryFunctions/Tanh/">Tanh</a>
     */
    public Complex tanh() {
        return ComplexFunctions.tanh(real, imaginary, Complex::ofCartesian);
    }

    /**
     * Returns the
     * <a href="http://mathworld.wolfram.com/InverseHyperbolicSine.html">
     * inverse hyperbolic sine</a> of this complex number.
     *
     * <p>\[ \sinh^{-1}(z) = \ln \left(z + \sqrt{1 + z^2} \right) \]
     *
     * <p>The inverse hyperbolic sine of \( z \) is unbounded along the real axis and
     * in the range \( [-\pi, \pi] \) along the imaginary axis. Special cases:
     *
     * <ul>
     * <li>{@code z.conj().asinh() == z.asinh().conj()}.
     * <li>This is an odd function: \( \sinh^{-1}(z) = -\sinh^{-1}(-z) \).
     * <li>If {@code z} is +0 + i0, returns 0 + i0.
     * <li>If {@code z} is x + i∞ for positive-signed finite x, returns +∞ + iπ/2.
     * <li>If {@code z} is x + iNaN for finite x, returns NaN + iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is +∞ + iy for positive-signed finite y, returns +∞ + i0.
     * <li>If {@code z} is +∞ + i∞, returns +∞ + iπ/4.
     * <li>If {@code z} is +∞ + iNaN, returns +∞ + iNaN.
     * <li>If {@code z} is NaN + i0, returns NaN + i0.
     * <li>If {@code z} is NaN + iy for finite nonzero y, returns NaN + iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is NaN + i∞, returns ±∞ + iNaN (where the sign of the real part of the result is unspecified).
     * <li>If {@code z} is NaN + iNaN, returns NaN + iNaN.
     * </ul>
     *
     * <p>The inverse hyperbolic sine is a multivalued function and requires a branch cut in
     * the complex plane; the cut is conventionally placed at the line segments
     * \( (-i \infty,-i) \) and \( (i,i \infty) \) of the imaginary axis.
     *
     * <p>This function is computed using the trigonomic identity:
     *
     * <p>\[ \sinh^{-1}(z) = -i \sin^{-1}(iz) \]
     *
     * @return The inverse hyperbolic sine of this complex number.
     * @see <a href="http://functions.wolfram.com/ElementaryFunctions/ArcSinh/">ArcSinh</a>
     */
    public Complex asinh() {
        return ComplexFunctions.asinh(real, imaginary, Complex::ofCartesian);
    }

    /**
     * Returns the
     * <a href="http://mathworld.wolfram.com/InverseHyperbolicCosine.html">
     * inverse hyperbolic cosine</a> of this complex number.
     *
     * <p>\[ \cosh^{-1}(z) = \ln \left(z + \sqrt{z + 1} \sqrt{z - 1} \right) \]
     *
     * <p>The inverse hyperbolic cosine of \( z \) is in the range \( [0, \infty) \) along the
     * real axis and in the range \( [-\pi, \pi] \) along the imaginary axis. Special cases:
     *
     * <ul>
     * <li>{@code z.conj().acosh() == z.acosh().conj()}.
     * <li>If {@code z} is ±0 + i0, returns +0 + iπ/2.
     * <li>If {@code z} is x + i∞ for finite x, returns +∞ + iπ/2.
     * <li>If {@code z} is 0 + iNaN, returns NaN + iπ/2 <sup>[1]</sup>.
     * <li>If {@code z} is x + iNaN for finite non-zero x, returns NaN + iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is −∞ + iy for positive-signed finite y, returns +∞ + iπ.
     * <li>If {@code z} is +∞ + iy for positive-signed finite y, returns +∞ + i0.
     * <li>If {@code z} is −∞ + i∞, returns +∞ + i3π/4.
     * <li>If {@code z} is +∞ + i∞, returns +∞ + iπ/4.
     * <li>If {@code z} is ±∞ + iNaN, returns +∞ + iNaN.
     * <li>If {@code z} is NaN + iy for finite y, returns NaN + iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is NaN + i∞, returns +∞ + iNaN.
     * <li>If {@code z} is NaN + iNaN, returns NaN + iNaN.
     * </ul>
     *
     * <p>Special cases include the technical corrigendum
     * <a href="http://www.open-std.org/jtc1/sc22/wg14/www/docs/n1892.htm#dr_471">
     * DR 471: Complex math functions cacosh and ctanh</a>.
     *
     * <p>The inverse hyperbolic cosine is a multivalued function and requires a branch cut in
     * the complex plane; the cut is conventionally placed at the line segment
     * \( (-\infty,-1) \) of the real axis.
     *
     * <p>This function is computed using the trigonomic identity:
     *
     * <p>\[ \cosh^{-1}(z) = \pm i \cos^{-1}(z) \]
     *
     * <p>The sign of the multiplier is chosen to give {@code z.acosh().real() >= 0}
     * and compatibility with the C99 standard.
     *
     * @return The inverse hyperbolic cosine of this complex number.
     * @see <a href="http://functions.wolfram.com/ElementaryFunctions/ArcCosh/">ArcCosh</a>
     */
    public Complex acosh() {
        return ComplexFunctions.acosh(real, imaginary, Complex::ofCartesian);
    }

    /**
     * Returns the
     * <a href="http://mathworld.wolfram.com/InverseHyperbolicTangent.html">
     * inverse hyperbolic tangent</a> of this complex number.
     *
     * <p>\[ \tanh^{-1}(z) = \frac{1}{2} \ln \left( \frac{1 + z}{1 - z} \right) \]
     *
     * <p>The inverse hyperbolic tangent of \( z \) is unbounded along the real axis and
     * in the range \( [-\pi/2, \pi/2] \) along the imaginary axis. Special cases:
     *
     * <ul>
     * <li>{@code z.conj().atanh() == z.atanh().conj()}.
     * <li>This is an odd function: \( \tanh^{-1}(z) = -\tanh^{-1}(-z) \).
     * <li>If {@code z} is +0 + i0, returns +0 + i0.
     * <li>If {@code z} is +0 + iNaN, returns +0 + iNaN.
     * <li>If {@code z} is +1 + i0, returns +∞ + i0 ("divide-by-zero" floating-point operation).
     * <li>If {@code z} is x + i∞ for finite positive-signed x, returns +0 + iπ/2.
     * <li>If {@code z} is x+iNaN for nonzero finite x, returns NaN+iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is +∞ + iy for finite positive-signed y, returns +0 + iπ/2.
     * <li>If {@code z} is +∞ + i∞, returns +0 + iπ/2.
     * <li>If {@code z} is +∞ + iNaN, returns +0 + iNaN.
     * <li>If {@code z} is NaN+iy for finite y, returns NaN+iNaN ("invalid" floating-point operation).
     * <li>If {@code z} is NaN + i∞, returns ±0 + iπ/2 (where the sign of the real part of the result is unspecified).
     * <li>If {@code z} is NaN + iNaN, returns NaN + iNaN.
     * </ul>
     *
     * <p>The inverse hyperbolic tangent is a multivalued function and requires a branch cut in
     * the complex plane; the cut is conventionally placed at the line segments
     * \( (\infty,-1] \) and \( [1,\infty) \) of the real axis.
     *
     * <p>This is implemented using real \( x \) and imaginary \( y \) parts:
     *
     * <p>\[ \tanh^{-1}(z) = \frac{1}{4} \ln \left(1 + \frac{4x}{(1-x)^2+y^2} \right) + \\
     *                     i \frac{1}{2} \left( \tan^{-1} \left(\frac{2y}{1-x^2-y^2} \right) + \frac{\pi}{2} \left(\text{sgn}(x^2+y^2-1)+1 \right) \text{sgn}(y) \right) \]
     *
     * <p>The imaginary part is computed using {@link Math#atan2(double, double)} to ensure the
     * correct quadrant is returned from \( \tan^{-1} \left(\frac{2y}{1-x^2-y^2} \right) \).
     *
     * <p>The code has been adapted from the <a href="https://www.boost.org/">Boost</a>
     * {@code c++} implementation {@code <boost/math/complex/atanh.hpp>}.
     *
     * @return The inverse hyperbolic tangent of this complex number.
     * @see <a href="http://functions.wolfram.com/ElementaryFunctions/ArcTanh/">ArcTanh</a>
     */
    public Complex atanh() {
        return ComplexFunctions.atanh(real, imaginary, Complex::ofCartesian);
    }

    /**
     * Returns the n-th roots of this complex number.
     * The nth roots are defined by the formula:
     *
     * <p>\[ z_k = |z|^{\frac{1}{n}} \left( \cos \left(\phi + \frac{2\pi k}{n} \right) + i \sin \left(\phi + \frac{2\pi k}{n} \right) \right) \]
     *
     * <p>for \( k=0, 1, \ldots, n-1 \), where \( |z| \) and \( \phi \)
     * are respectively the {@link #abs() modulus} and
     * {@link #arg() argument} of this complex number.
     *
     * <p>If one or both parts of this complex number is NaN, a list with all
     * all elements set to {@code NaN + i NaN} is returned.</p>
     *
     * @param n Degree of root.
     * @return A list of all {@code n}-th roots of this complex number.
     * @throws IllegalArgumentException if {@code n} is zero.
     * @see <a href="http://functions.wolfram.com/ElementaryFunctions/Root/">Root</a>
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
            result.add(ofCartesian(realPart, imaginaryPart));
            innerPart += slice;
        }

        return result;
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
     *  {@code c1.getReal() == c2.getReal() && c1.getImaginary() == c2.getImaginary()}</pre>
     *
     * <p>also has the value {@code true}. However, there are exceptions:
     *
     * <ul>
     *  <li>
     *   Instances that contain {@code NaN} values in the same part
     *   are considered to be equal for that part, even though {@code Double.NaN == Double.NaN}
     *   has the value {@code false}.
     *  </li>
     *  <li>
     *   Instances that share a {@code NaN} value in one part
     *   but have different values in the other part are <em>not</em> considered equal.
     *  </li>
     *  <li>
     *   Instances that contain different representations of zero in the same part
     *   are <em>not</em> considered to be equal for that part, even though {@code -0.0 == 0.0}
     *   has the value {@code true}.
     *  </li>
     * </ul>
     *
     * <p>The behavior is the same as if the components of the two complex numbers were passed
     * to {@link java.util.Arrays#equals(double[], double[]) Arrays.equals(double[], double[])}:
     *
     * <pre>
     *  Arrays.equals(new double[]{c1.getReal(), c1.getImaginary()},
     *                new double[]{c2.getReal(), c2.getImaginary()}); </pre>
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
     * Gets a hash code for the complex number.
     *
     * <p>The behavior is the same as if the components of the complex number were passed
     * to {@link java.util.Arrays#hashCode(double[]) Arrays.hashCode(double[])}:
     *
     * <pre>
     *  {@code Arrays.hashCode(new double[] {getReal(), getImaginary()})}</pre>
     *
     * @return A hash code value for this object.
     * @see java.util.Arrays#hashCode(double[]) Arrays.hashCode(double[])
     */
    @Override
    public int hashCode() {
        return 31 * (31 + Double.hashCode(real)) + Double.hashCode(imaginary);
    }

    /**
     * Returns a string representation of the complex number.
     *
     * <p>The string will represent the numeric values of the real and imaginary parts.
     * The values are split by a separator and surrounded by parentheses.
     * The string can be {@link #parse(String) parsed} to obtain an instance with the same value.
     *
     * <p>The format for complex number \( x + i y \) is {@code "(x,y)"}, with \( x \) and
     * \( y \) converted as if using {@link Double#toString(double)}.
     *
     * @return A string representation of the complex number.
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
     * @return {@code Double.valueof(x).equals(Double.valueOf(y))}.
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
    //TODO - make private in ComplexFunctions in future
    private static boolean negative(double d) {
        return ComplexFunctions.negative(d);
    }

    /**
     * Returns a scale suitable for use with {@link Math#scalb(double, int)} to normalise
     * the number to the interval {@code [1, 2)}.
     *
     * <p>The scale is typically the largest unbiased exponent used in the representation of the
     * two numbers. In contrast to {@link Math#getExponent(double)} this handles
     * sub-normal numbers by computing the number of leading zeros in the mantissa
     * and shifting the unbiased exponent. The result is that for all finite, non-zero,
     * numbers {@code a, b}, the magnitude of {@code scalb(x, -getScale(a, b))} is
     * always in the range {@code [1, 2)}, where {@code x = max(|a|, |b|)}.
     *
     * <p>This method is a functional equivalent of the c function ilogb(double) adapted for
     * two input arguments.
     *
     * <p>The result is to be used to scale a complex number using {@link Math#scalb(double, int)}.
     * Hence the special case of both zero arguments is handled using the return value for NaN
     * as zero cannot be scaled. This is different from {@link Math#getExponent(double)}
     * or {@link #getMaxExponent(double, double)}.
     *
     * <p>Special cases:
     *
     * <ul>
     * <li>If either argument is NaN or infinite, then the result is
     * {@link Double#MAX_EXPONENT} + 1.
     * <li>If both arguments are zero, then the result is
     * {@link Double#MAX_EXPONENT} + 1.
     * </ul>
     *
     * @param a the first value
     * @param b the second value
     * @return The maximum unbiased exponent of the values to be used for scaling
     * @see Math#getExponent(double)
     * @see Math#scalb(double, int)
     * @see <a href="http://www.cplusplus.com/reference/cmath/ilogb/">ilogb</a>
     */
    private static int getScale(double a, double b) {
        // Only interested in the exponent and mantissa so remove the sign bit
        final long x = Double.doubleToRawLongBits(a) & UNSIGN_MASK;
        final long y = Double.doubleToRawLongBits(b) & UNSIGN_MASK;
        // Only interested in the maximum
        final long bits = Math.max(x, y);
        // Get the unbiased exponent
        int exp = ((int) (bits >>> 52)) - EXPONENT_OFFSET;

        // No case to distinguish nan/inf
        // Handle sub-normal numbers
        if (exp == Double.MIN_EXPONENT - 1) {
            // Special case for zero, return as nan/inf to indicate scaling is not possible
            if (bits == 0) {
                return Double.MAX_EXPONENT + 1;
            }
            // A sub-normal number has an exponent below -1022. The amount below
            // is defined by the number of shifts of the most significant bit in
            // the mantissa that is required to get a 1 at position 53 (i.e. as
            // if it were a normal number with assumed leading bit)
            final long mantissa = bits & MANTISSA_MASK;
            exp -= Long.numberOfLeadingZeros(mantissa << 12);
        }
        return exp;
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
     * <p>This is used by {@link #divide(double, double, double, double)} as
     * a simple detection that a number may overflow if multiplied
     * by a value in the interval [1, 2).
     *
     * @param a the first value
     * @param b the second value
     * @return The maximum unbiased exponent of the values.
     * @see Math#getExponent(double)
     * @see #divide(double, double, double, double)
     */
    private static int getMaxExponent(double a, double b) {
        // This could return:
        // Math.getExponent(Math.max(Math.abs(a), Math.abs(b)))
        // A speed test is required to determine performance.
        return Math.max(Math.getExponent(a), Math.getExponent(b));
    }
}
