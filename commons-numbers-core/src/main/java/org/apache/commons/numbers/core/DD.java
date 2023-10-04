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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.function.DoubleUnaryOperator;

/**
 * Computes double-double floating-point operations.
 *
 * <p>A double-double is an unevaluated sum of two IEEE double precision numbers capable of
 * representing at least 106 bits of significand. A normalized double-double number {@code (x, xx)}
 *  satisfies the condition that the parts are non-overlapping in magnitude such that:
 * <pre>
 * |x| &gt; |xx|
 * x == x + xx
 * </pre>
 *
 * <p>This implementation assumes a normalized representation during operations on a {@code DD}
 * number and computes results as a normalized representation. Any double-double number
 * can be normalized by summation of the parts (see {@link #ofSum(double, double) ofSum}).
 * Note that the number {@code (x, xx)} may also be referred to using the labels high and low
 * to indicate the magnitude of the parts as
 * {@code (x}<sub>hi</sub>{@code , x}<sub>lo</sub>{@code )}, or using a numerical suffix for the
 * parts as {@code (x}<sub>0</sub>{@code , x}<sub>1</sub>{@code )}. The numerical suffix is
 * typically used when the number has an arbitrary number of parts.
 *
 * <p>The double-double class is immutable.
 *
 * <p><b>Construction</b>
 *
 * <p>Factory methods to create a {@code DD} that are exact use the prefix {@code of}. Methods
 * that create the closest possible representation use the prefix {@code from}. These methods
 * may suffer a possible loss of precision during conversion.
 *
 * <p>Primitive values of type {@code double}, {@code int} and {@code long} are
 * converted exactly to a {@code DD}.
 *
 * <p>The {@code DD} class can also be created as the result of an arithmetic operation on a pair
 * of {@code double} operands. The resulting {@code DD} has the IEEE754 {@code double} result
 * of the operation in the first part, and the second part contains the round-off lost from the
 * operation due to rounding. Construction using add ({@code +}), subtract ({@code -}) and
 * multiply ({@code *}) operators are exact. Construction using division ({@code /}) may be
 * inexact if the quotient is not representable.
 *
 * <p>Note that it is more efficient to create a {@code DD} from a {@code double} operation than
 * to create two {@code DD} values and combine them with the same operation. The result will be
 * the same for add, subtract and multiply but may lose precision for divide.
 * <pre>{@code
 * // Inefficient
 * DD a = DD.of(1.23).add(DD.of(4.56));
 * // Optimal
 * DD b = DD.ofSum(1.23, 4.56);
 *
 * // Inefficient and may lose precision
 * DD c = DD.of(1.23).divide(DD.of(4.56));
 * // Optimal
 * DD d = DD.fromQuotient(1.23, 4.56);
 * }</pre>
 *
 * <p>It is not possible to directly specify the two parts of the number.
 * The two parts must be added using {@link #ofSum(double, double) ofSum}.
 * If the two parts already represent a number {@code (x, xx)} such that {@code x == x + xx}
 * then the magnitudes of the parts will be unchanged; any signed zeros may be subject to a sign
 * change.
 *
 * <p><b>Primitive operands</b>
 *
 * <p>Operations are provided using a {@code DD} operand or a {@code double} operand.
 * Implicit type conversion allows methods with a {@code double} operand to be used
 * with other primitives such as {@code int} or {@code long}. Note that casting of a {@code long}
 * to a {@code double} may result in loss of precision.
 * To maintain the full precision of a {@code long} first convert the value to a {@code DD} using
 * {@link #of(long)} and use the same arithmetic operation using the {@code DD} operand.
 *
 * <p><b>Accuracy</b>
 *
 * <p>Add and multiply operations using two {@code double} values operands are computed to an
 * exact {@code DD} result (see {@link #ofSum(double, double) ofSum} and
 * {@link #ofProduct(double, double) ofProduct}). Operations involving a {@code DD} and another
 * operand, either {@code double} or {@code DD}, are not exact.
 *
 * <p>This class is not intended to perform exact arithmetic. Arbitrary precision arithmetic is
 * available using {@link BigDecimal}. Single operations will compute the {@code DD} result within
 * a tolerance of the 106-bit exact result. This far exceeds the accuracy of {@code double}
 * arithmetic. The reduced accuracy is a compromise to deliver increased performance.
 * The class is intended to reduce error in equivalent {@code double} arithmetic operations where
 * the {@code double} valued result is required to high accuracy. Although it
 * is possible to reduce error to 2<sup>-106</sup> for all operations, the additional computation
 * would impact performance and would require multiple chained operations to potentially
 * observe a different result when the final {@code DD} is converted to a {@code double}.
 *
 * <p><b>Canonical representation</b>
 *
 * <p>The double-double number is the sum of its parts. The canonical representation of the
 * number is the explicit value of the parts. The {@link #toString()} method is provided to
 * convert to a String representation of the parts formatted as a tuple.
 *
 * <p>The class implements {@link #equals(Object)} and {@link #hashCode()} and allows usage as
 * a key in a Set or Map. Equality requires <em>binary</em> equivalence of the parts. Note that
 * representations of zero using different combinations of +/- 0.0 are not considered equal.
 * Also note that many non-normalized double-double numbers can represent the same number.
 * Double-double numbers can be normalized before operations that involve {@link #equals(Object)}
 * by {@link #ofSum(double, double) adding} the parts; this is exact for a finite sum
 * and provides equality support for non-zero numbers. Alternatively exact numerical equality
 * and comparisons are supported by conversion to a {@link #bigDecimalValue() BigDecimal}
 * representation. Note that {@link BigDecimal} does not support non-finite values.
 *
 * <p><b>Overflow, underflow and non-finite support</b>
 *
 * <p>A double-double number is limited to the same finite range as a {@code double}
 * ({@value Double#MIN_VALUE} to {@value Double#MAX_VALUE}). This class is intended for use when
 * the ultimate result is finite and intermediate values do not approach infinity or zero.
 *
 * <p>This implementation does not support IEEE standards for handling infinite and NaN when used
 * in arithmetic operations. Computations may split a 64-bit double into two parts and/or use
 * subtraction of intermediate terms to compute round-off parts. These operations may generate
 * infinite values due to overflow which then propagate through further operations to NaN,
 * for example computing the round-off using {@code Inf - Inf = NaN}.
 *
 * <p>Operations that involve splitting a double (multiply, divide) are safe
 * when the base 2 exponent is below 996. This puts an upper limit of approximately +/-6.7e299 on
 * any values to be split; in practice the arguments to multiply and divide operations are further
 * constrained by the expected finite value of the product or quotient.
 *
 * <p>Likewise the smallest value that can be represented is {@link Double#MIN_VALUE}. The full
 * 106-bit accuracy will be lost when intermediates are within 2<sup>53</sup> of
 * {@link Double#MIN_NORMAL}.
 *
 * <p>The {@code DD} result can be verified by checking it is a {@link #isFinite() finite}
 * evaluated sum. Computations expecting to approach over or underflow must use scaling of
 * intermediate terms (see {@link #frexp(int[]) frexp} and {@link #scalb(int) scalb}) and
 * appropriate management of the current base 2 scale.
 *
 * <p>References:
 * <ol>
 * <li>
 * Dekker, T.J. (1971)
 * <a href="https://doi.org/10.1007/BF01397083">
 * A floating-point technique for extending the available precision</a>
 * Numerische Mathematik, 18:224–242.
 * <li>
 * Shewchuk, J.R. (1997)
 * <a href="https://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps">
 * Arbitrary Precision Floating-Point Arithmetic</a>.
 * <li>
 * Hide, Y, Li, X.S. and Bailey, D.H. (2008)
 * <a href="https://www.davidhbailey.com/dhbpapers/qd.pdf">
 * Library for Double-Double and Quad-Double Arithmetic</a>.
 * </ol>
 *
 * @since 1.2
 */
public final class DD
    extends Number
    implements NativeOperators<DD>,
               Serializable {
    // Caveat:
    //
    // The code below uses many additions/subtractions that may
    // appear redundant. However, they should NOT be simplified, as they
    // do use IEEE754 floating point arithmetic rounding properties.
    //
    // Algorithms are based on computing the product or sum of two values x and y in
    // extended precision. The standard result is stored using a double (high part z) and
    // the round-off error (or low part zz) is stored in a second double, e.g:
    // x * y = (z, zz); z + zz = x * y
    // x + y = (z, zz); z + zz = x + y
    //
    // The building blocks for double-double arithmetic are:
    //
    // Fast-Two-Sum: Addition of two doubles (ordered |x| > |y|) to a double-double
    // Two-Sum: Addition of two doubles (unordered) to a double-double
    // Two-Prod: Multiplication of two doubles to a double-double
    //
    // These are used to create functions operating on double and double-double numbers.
    //
    // To sum multiple (z, zz) results ideally the parts are sorted in order of
    // non-decreasing magnitude and summed. This is exact if each number's most significant
    // bit is below the least significant bit of the next (i.e. does not
    // overlap). Creating non-overlapping parts requires a rebalancing
    // of adjacent pairs using a summation z + zz = (z1, zz1) iteratively through the parts
    // (see Shewchuk (1997) Grow-Expansion and Expansion-Sum [2]).
    //
    // Accurate summation of an expansion (more than one double value) to a double-double
    // performs a two-sum through the expansion e (length m).
    // The single pass with two-sum ensures that the final term e_m is a good approximation
    // for e: |e - e_m| < ulp(e_m); and the sum of the parts to
    // e_(m-1) is within 1 ULP of the round-off ulp(|e - e_m|).
    // These final two terms create the double-double result using two-sum.
    //
    // Maintenance of 1 ULP precision in the round-off component for all double-double
    // operations is a performance burden. This class avoids this requirement to provide
    // a compromise between accuracy and performance.

    /**
     * A double-double number representing one.
     */
    public static final DD ONE = new DD(1, 0);
    /**
     * A double-double number representing zero.
     */
    public static final DD ZERO = new DD(0, 0);

    /**
     * The multiplier used to split the double value into high and low parts. From
     * Dekker (1971): "The constant should be chosen equal to 2^(p - p/2) + 1,
     * where p is the number of binary digits in the mantissa". Here p is 53
     * and the multiplier is {@code 2^27 + 1}.
     */
    private static final double MULTIPLIER = 1.0 + 0x1.0p27;
    /** The mask to extract the raw 11-bit exponent.
     * The value must be shifted 52-bits to remove the mantissa bits. */
    private static final int EXP_MASK = 0x7ff;
    /** The value 2046 converted for use if using {@link Integer#compareUnsigned(int, int)}.
     * This requires adding {@link Integer#MIN_VALUE} to 2046. */
    private static final int CMP_UNSIGNED_2046 = Integer.MIN_VALUE + 2046;
    /** The value -1 converted for use if using {@link Integer#compareUnsigned(int, int)}.
     * This requires adding {@link Integer#MIN_VALUE} to -1. */
    private static final int CMP_UNSIGNED_MINUS_1 = Integer.MIN_VALUE - 1;
    /** The value 1022 converted for use if using {@link Integer#compareUnsigned(int, int)}.
     * This requires adding {@link Integer#MIN_VALUE} to 1022. */
    private static final int CMP_UNSIGNED_1022 = Integer.MIN_VALUE + 1022;
    /** 2^512. */
    private static final double TWO_POW_512 = 0x1.0p512;
    /** 2^-512. */
    private static final double TWO_POW_M512 = 0x1.0p-512;
    /** 2^53. Any double with a magnitude above this is an even integer. */
    private static final double TWO_POW_53 = 0x1.0p53;
    /** Mask to extract the high 32-bits from a long. */
    private static final long HIGH32_MASK = 0xffff_ffff_0000_0000L;
    /** Mask to remove the sign bit from a long. */
    private static final long UNSIGN_MASK = 0x7fff_ffff_ffff_ffffL;
    /** Mask to extract the 52-bit mantissa from a long representation of a double. */
    private static final long MANTISSA_MASK = 0x000f_ffff_ffff_ffffL;
    /** Exponent offset in IEEE754 representation. */
    private static final int EXPONENT_OFFSET = 1023;
    /** 0.5. */
    private static final double HALF = 0.5;
    /** The limit for safe multiplication of {@code x*y}, assuming values above 1.
     * Used to maintain positive values during the power computation. */
    private static final double SAFE_MULTIPLY = 0x1.0p500;

    /**
     * The size of the buffer for {@link #toString()}.
     *
     * <p>The longest double will require a sign, a maximum of 17 digits, the decimal place
     * and the exponent, e.g. for max value this is 24 chars: -1.7976931348623157e+308.
     * Set the buffer size to twice this and round up to a power of 2 thus
     * allowing for formatting characters. The size is 64.
     */
    private static final int TO_STRING_SIZE = 64;
    /** {@link #toString() String representation}. */
    private static final char FORMAT_START = '(';
    /** {@link #toString() String representation}. */
    private static final char FORMAT_END = ')';
    /** {@link #toString() String representation}. */
    private static final char FORMAT_SEP = ',';

    /** Serializable version identifier. */
    private static final long serialVersionUID = 20230701L;

    /** The high part of the double-double number. */
    private final double x;
    /** The low part of the double-double number. */
    private final double xx;

    /**
     * Create a double-double number {@code (x, xx)}.
     *
     * @param x High part.
     * @param xx Low part.
     */
    private DD(double x, double xx) {
        this.x = x;
        this.xx = xx;
    }

    // Conversion constructors

    /**
     * Creates the double-double number as the value {@code (x, 0)}.
     *
     * @param x Value.
     * @return the double-double
     */
    public static DD of(double x) {
        return new DD(x, 0);
    }

    /**
     * Creates the double-double number as the value {@code (x, xx)}.
     *
     * <p><strong>Warning</strong>
     *
     * <p>The arguments are used directly. No checks are made that they represent
     * a normalized double-double number: {@code x == x + xx}.
     *
     * <p>This method is exposed for testing.
     *
     * @param x High part.
     * @param xx Low part.
     * @return the double-double
     * @see #twoSum(double, double)
     */
    static DD of(double x, double xx) {
        return new DD(x, xx);
    }

    /**
     * Creates the double-double number as the value {@code (x, 0)}.
     *
     * <p>Note this method exists to avoid using {@link #of(long)} for {@code integer}
     * arguments; the {@code long} variation is slower as it preserves all 64-bits
     * of information.
     *
     * @param x Value.
     * @return the double-double
     * @see #of(long)
     */
    public static DD of(int x) {
        return new DD(x, 0);
    }

    /**
     * Creates the double-double number with the high part equal to {@code (double) x}
     * and the low part equal to any remaining bits.
     *
     * <p>Note this method preserves all 64-bits of precision. Faster construction can be
     * achieved using up to 53-bits of precision using {@code of((double) x)}.
     *
     * @param x Value.
     * @return the double-double
     * @see #of(double)
     */
    public static DD of(long x) {
        // Note: Casting the long to a double can lose bits due to rounding.
        // These are not recoverable using lo = x - (long)((double) x)
        // if the double is rounded outside the range of a long (i.e. 2^53).
        // Split the long into two 32-bit numbers that are exactly representable
        // and add them.
        final long a = x & HIGH32_MASK;
        final long b = x - a;
        // When x is positive: a > b or a == 0
        // When x is negative: |a| > |b|
        return fastTwoSum(a, b);
    }

    /**
     * Creates the double-double number {@code (z, zz)} using the {@code double} representation
     * of the argument {@code x}; the low part is the {@code double} representation of the
     * round-off error.
     * <pre>
     * double z = x.doubleValue();
     * double zz = x.subtract(new BigDecimal(z)).doubleValue();
     * </pre>
     * <p>If the value cannot be represented as a finite value the result will have an
     * infinite high part and the low part is undefined.
     *
     * <p>Note: This conversion can lose information about the precision of the BigDecimal value.
     * The result is the closest double-double representation to the value.
     *
     * @param x Value.
     * @return the double-double
     */
    public static DD from(BigDecimal x) {
        final double z = x.doubleValue();
        // Guard against an infinite throwing a exception
        final double zz = Double.isInfinite(z) ? 0 : x.subtract(new BigDecimal(z)).doubleValue();
        // No normalisation here
        return new DD(z, zz);
    }

    // Arithmetic constructors:

    /**
     * Returns a {@code DD} whose value is {@code (x + y)}.
     * The values are not required to be ordered by magnitude,
     * i.e. the result is commutative: {@code x + y == y + x}.
     *
     * <p>This method ignores special handling of non-normal numbers and
     * overflow within the extended precision computation.
     * This creates the following special cases:
     *
     * <ul>
     *  <li>If {@code x + y} is infinite then the low part is NaN.
     *  <li>If {@code x} or {@code y} is infinite or NaN then the low part is NaN.
     *  <li>If {@code x + y} is sub-normal or zero then the low part is +/-0.0.
     * </ul>
     *
     * <p>An invalid result can be identified using {@link #isFinite()}.
     *
     * <p>The result is the exact double-double representation of the sum.
     *
     * @param x Addend.
     * @param y Addend.
     * @return the sum {@code x + y}.
     * @see #ofDifference(double, double)
     */
    public static DD ofSum(double x, double y) {
        return twoSum(x, y);
    }

    /**
     * Returns a {@code DD} whose value is {@code (x - y)}.
     * The values are not required to be ordered by magnitude,
     * i.e. the result matches a negation and addition: {@code x - y == -y + x}.
     *
     * <p>Computes the same results as {@link #ofSum(double, double) ofSum(a, -b)}.
     * See that method for details of special cases.
     *
     * <p>An invalid result can be identified using {@link #isFinite()}.
     *
     * <p>The result is the exact double-double representation of the difference.
     *
     * @param x Minuend.
     * @param y Subtrahend.
     * @return {@code x - y}.
     * @see #ofSum(double, double)
     */
    public static DD ofDifference(double x, double y) {
        return twoDiff(x, y);
    }

    /**
     * Returns a {@code DD} whose value is {@code (x * y)}.
     *
     * <p>This method ignores special handling of non-normal numbers and intermediate
     * overflow within the extended precision computation.
     * This creates the following special cases:
     *
     * <ul>
     *  <li>If either {@code |x|} or {@code |y|} multiplied by {@code 1 + 2^27}
     *      is infinite (intermediate overflow) then the low part is NaN.
     *  <li>If {@code x * y} is infinite then the low part is NaN.
     *  <li>If {@code x} or {@code y} is infinite or NaN then the low part is NaN.
     *  <li>If {@code x * y} is sub-normal or zero then the low part is +/-0.0.
     * </ul>
     *
     * <p>An invalid result can be identified using {@link #isFinite()}.
     *
     * <p>Note: Ignoring special cases is a design choice for performance. The
     * method is therefore not a drop-in replacement for
     * {@code roundOff = Math.fma(x, y, -x * y)}.
     *
     * <p>The result is the exact double-double representation of the product.
     *
     * @param x Factor.
     * @param y Factor.
     * @return the product {@code x * y}.
     */
    public static DD ofProduct(double x, double y) {
        return twoProd(x, y);
    }

    /**
     * Returns a {@code DD} whose value is {@code (x * x)}.
     *
     * <p>This method is an optimisation of {@link #ofProduct(double, double) multiply(x, x)}.
     * See that method for details of special cases.
     *
     * <p>An invalid result can be identified using {@link #isFinite()}.
     *
     * <p>The result is the exact double-double representation of the square.
     *
     * @param x Factor.
     * @return the square {@code x * x}.
     * @see #ofProduct(double, double)
     */
    public static DD ofSquare(double x) {
        return twoSquare(x);
    }

    /**
     * Returns a {@code DD} whose value is {@code (x / y)}.
     * If {@code y = 0} the result is undefined.
     *
     * <p>This method ignores special handling of non-normal numbers and intermediate
     * overflow within the extended precision computation.
     * This creates the following special cases:
     *
     * <ul>
     *  <li>If either {@code |x / y|} or {@code |y|} multiplied by {@code 1 + 2^27}
     *      is infinite (intermediate overflow) then the low part is NaN.
     *  <li>If {@code x / y} is infinite then the low part is NaN.
     *  <li>If {@code x} or {@code y} is infinite or NaN then the low part is NaN.
     *  <li>If {@code x / y} is sub-normal or zero, excluding the previous cases,
     *      then the low part is +/-0.0.
     * </ul>
     *
     * <p>An invalid result can be identified using {@link #isFinite()}.
     *
     * <p>The result is the closest double-double representation to the quotient.
     *
     * @param x Dividend.
     * @param y Divisor.
     * @return the quotient {@code x / y}.
     */
    public static DD fromQuotient(double x, double y) {
        // Long division
        // quotient q0 = x / y
        final double q0 = x / y;
        // remainder r = x - q0 * y
        final double p0 = q0 * y;
        final double p1 = twoProductLow(q0, y, p0);
        final double r0 = x - p0;
        final double r1 = twoDiffLow(x, p0, r0) - p1;
        // correction term q1 = r0 / y
        final double q1 = (r0 + r1) / y;
        return new DD(q0, q1);
    }

    // Properties

    /**
     * Gets the first part {@code x} of the double-double number {@code (x, xx)}.
     * In a normalized double-double number this part will have the greatest magnitude.
     *
     * <p>This is equivalent to returning the high-part {@code x}<sub>hi</sub> for the number
     * {@code (x}<sub>hi</sub>{@code , x}<sub>lo</sub>{@code )}.
     *
     * @return the first part
     */
    public double hi() {
        return x;
    }

    /**
     * Gets the second part {@code xx} of the double-double number {@code (x, xx)}.
     * In a normalized double-double number this part will have the smallest magnitude.
     *
     * <p>This is equivalent to returning the low part {@code x}<sub>lo</sub> for the number
     * {@code (x}<sub>hi</sub>{@code , x}<sub>lo</sub>{@code )}.
     *
     * @return the second part
     */
    public double lo() {
        return xx;
    }

    /**
     * Returns {@code true} if the evaluated sum of the parts is finite.
     *
     * <p>This method is provided as a utility to check the result of a {@code DD} computation.
     * Note that for performance the {@code DD} class does not follow IEEE754 arithmetic
     * for infinite and NaN, and does not protect from overflow of intermediate values in
     * multiply and divide operations. If this method returns {@code false} following
     * {@code DD} arithmetic then the computation is not supported to extended precision.
     *
     * <p>Note: Any number that returns {@code true} may be converted to the exact
     * {@link BigDecimal} value.
     *
     * @return {@code true} if this instance represents a finite {@code double} value.
     * @see Double#isFinite(double)
     * @see #bigDecimalValue()
     */
    public boolean isFinite() {
        return Double.isFinite(x + xx);
    }

    // Number conversions

    /**
     * Get the value as a {@code double}. This is the evaluated sum of the parts.
     *
     * <p>Note that even when the return value is finite, this conversion can lose
     * information about the precision of the {@code DD} value.
     *
     * <p>Conversion of a finite {@code DD} can also be performed using the
     * {@link #bigDecimalValue() BigDecimal} representation.
     *
     * @return the value converted to a {@code double}
     * @see #bigDecimalValue()
     */
    @Override
    public double doubleValue() {
        return x + xx;
    }

    /**
     * Get the value as a {@code float}. This is the narrowing primitive conversion of the
     * {@link #doubleValue()}. This conversion can lose range, resulting in a
     * {@code float} zero from a nonzero {@code double} and a {@code float} infinity from
     * a finite {@code double}. A {@code double} NaN is converted to a {@code float} NaN
     * and a {@code double} infinity is converted to the same-signed {@code float}
     * infinity.
     *
     * <p>Note that even when the return value is finite, this conversion can lose
     * information about the precision of the {@code DD} value.
     *
     * <p>Conversion of a finite {@code DD} can also be performed using the
     * {@link #bigDecimalValue() BigDecimal} representation.
     *
     * @return the value converted to a {@code float}
     * @see #bigDecimalValue()
     */
    @Override
    public float floatValue() {
        return (float) doubleValue();
    }

    /**
     * Get the value as an {@code int}. This conversion discards the fractional part of the
     * number and effectively rounds the value to the closest whole number in the direction
     * of zero. This is the equivalent of a cast of a floating-point number to an integer, for
     * example {@code (int) -2.75 => -2}.
     *
     * <p>Note that this conversion can lose information about the precision of the
     * {@code DD} value.
     *
     * <p>Special cases:
     * <ul>
     *  <li>If the {@code DD} value is infinite the result is {@link Integer#MAX_VALUE}.
     *  <li>If the {@code DD} value is -infinite the result is {@link Integer#MIN_VALUE}.
     *  <li>If the {@code DD} value is NaN the result is 0.
     * </ul>
     *
     * <p>Conversion of a finite {@code DD} can also be performed using the
     * {@link #bigDecimalValue() BigDecimal} representation. Note that {@link BigDecimal}
     * conversion rounds to the {@link java.math.BigInteger BigInteger} whole number
     * representation and returns the low-order 32-bits. Numbers too large for an {@code int}
     * may change sign. This method ensures the sign is correct by directly rounding to
     * an {@code int} and returning the respective upper or lower limit for numbers too
     * large for an {@code int}.
     *
     * @return the value converted to an {@code int}
     * @see #bigDecimalValue()
     */
    @Override
    public int intValue() {
        // Clip the long value
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, longValue()));
    }

    /**
     * Get the value as a {@code long}. This conversion discards the fractional part of the
     * number and effectively rounds the value to the closest whole number in the direction
     * of zero. This is the equivalent of a cast of a floating-point number to an integer, for
     * example {@code (long) -2.75 => -2}.
     *
     * <p>Note that this conversion can lose information about the precision of the
     * {@code DD} value.
     *
     * <p>Special cases:
     * <ul>
     *  <li>If the {@code DD} value is infinite the result is {@link Long#MAX_VALUE}.
     *  <li>If the {@code DD} value is -infinite the result is {@link Long#MIN_VALUE}.
     *  <li>If the {@code DD} value is NaN the result is 0.
     * </ul>
     *
     * <p>Conversion of a finite {@code DD} can also be performed using the
     * {@link #bigDecimalValue() BigDecimal} representation. Note that {@link BigDecimal}
     * conversion rounds to the {@link java.math.BigInteger BigInteger} whole number
     * representation and returns the low-order 64-bits. Numbers too large for a {@code long}
     * may change sign. This method ensures the sign is correct by directly rounding to
     * a {@code long} and returning the respective upper or lower limit for numbers too
     * large for a {@code long}.
     *
     * @return the value converted to an {@code int}
     * @see #bigDecimalValue()
     */
    @Override
    public long longValue() {
        // Assume |hi| > |lo|, i.e. the low part is the round-off
        final long a = (long) x;
        // The cast will truncate the value to the range [Long.MIN_VALUE, Long.MAX_VALUE].
        // If the long converted back to a double is the same value then the high part
        // was a representable integer and we must use the low part.
        // Note: The floating-point comparison is intentional.
        if (a == x) {
            // Edge case: Any double value above 2^53 is even. To workaround representation
            // of 2^63 as Long.MAX_VALUE (which is 2^63-1) we can split a into two parts.
            long a1;
            long a2;
            if (Math.abs(x) > TWO_POW_53) {
                a1 = (long) (x * 0.5);
                a2 = a1;
            } else {
                a1 = a;
                a2 = 0;
            }

            // To truncate the fractional part of the double-double towards zero we
            // convert the low part to a whole number. This must be rounded towards zero
            // with respect to the sign of the high part.
            final long b = (long) (a < 0 ? Math.ceil(xx) : Math.floor(xx));

            final long sum = a1 + b + a2;
            // Avoid overflow. If the sum has changed sign then an overflow occurred.
            // This happens when high == 2^63 and the low part is additional magnitude.
            // The xor operation creates a negative if the signs are different.
            if ((sum ^ a) >= 0) {
                return sum;
            }
        }
        // Here the high part had a fractional part, was non-finite or was 2^63.
        // Ignore the low part.
        return a;
    }

    /**
     * Get the value as a {@code BigDecimal}. This is the evaluated sum of the parts;
     * the conversion is exact.
     *
     * <p>The conversion will raise a {@link NumberFormatException} if the number
     * is non-finite.
     *
     * @return the double-double as a {@code BigDecimal}.
     * @throws NumberFormatException if any part of the number is {@code infinite} or {@code NaN}
     * @see BigDecimal
     */
    public BigDecimal bigDecimalValue() {
        return new BigDecimal(x).add(new BigDecimal(xx));
    }

    // Static extended precision methods for computing the round-off component
    // for double addition and multiplication

    /**
     * Compute the sum of two numbers {@code a} and {@code b} using
     * Dekker's two-sum algorithm. The values are required to be ordered by magnitude:
     * {@code |a| >= |b|}.
     *
     * <p>If {@code a} is zero and {@code b} is non-zero the returned value is {@code (b, 0)}.
     *
     * @param a First part of sum.
     * @param b Second part of sum.
     * @return the sum
     * @see #fastTwoDiff(double, double)
     * @see <a href="http://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps">
     * Shewchuk (1997) Theorum 6</a>
     */
    static DD fastTwoSum(double a, double b) {
        final double x = a + b;
        return new DD(x, fastTwoSumLow(a, b, x));
    }

    /**
     * Compute the round-off of the sum of two numbers {@code a} and {@code b} using
     * Dekker's two-sum algorithm. The values are required to be ordered by magnitude:
     * {@code |a| >= |b|}.
     *
     * <p>If {@code a} is zero and {@code b} is non-zero the returned value is zero.
     *
     * @param a First part of sum.
     * @param b Second part of sum.
     * @param x Sum.
     * @return the sum round-off
     * @see #fastTwoSum(double, double)
     */
    static double fastTwoSumLow(double a, double b, double x) {
        // (x, xx) = a + b
        // bVirtual = x - a
        // xx = b - bVirtual
        return b - (x - a);
    }

    /**
     * Compute the difference of two numbers {@code a} and {@code b} using
     * Dekker's two-sum algorithm. The values are required to be ordered by magnitude:
     * {@code |a| >= |b|}.
     *
     * <p>Computes the same results as {@link #fastTwoSum(double, double) fastTwoSum(a, -b)}.
     *
     * @param a Minuend.
     * @param b Subtrahend.
     * @return the difference
     * @see #fastTwoSum(double, double)
     * @see <a href="http://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps">
     * Shewchuk (1997) Theorum 6</a>
     */
    static DD fastTwoDiff(double a, double b) {
        final double x = a - b;
        return new DD(x, fastTwoDiffLow(a, b, x));
    }

    /**
     * Compute the round-off of the difference of two numbers {@code a} and {@code b} using
     * Dekker's two-sum algorithm. The values are required to be ordered by magnitude:
     * {@code |a| >= |b|}.
     *
     * @param a Minuend.
     * @param b Subtrahend.
     * @param x Difference.
     * @return the difference round-off
     * @see #fastTwoDiff(double, double)
     */
    private static double fastTwoDiffLow(double a, double b, double x) {
        // (x, xx) = a - b
        // bVirtual = a - x
        // xx = bVirtual - b
        return (a - x) - b;
    }

    /**
     * Compute the sum of two numbers {@code a} and {@code b} using
     * Knuth's two-sum algorithm. The values are not required to be ordered by magnitude,
     * i.e. the result is commutative {@code s = a + b == b + a}.
     *
     * @param a First part of sum.
     * @param b Second part of sum.
     * @return the sum
     * @see #twoDiff(double, double)
     * @see <a href="http://www-2.cs.cmu.edu/afs/cs/project/quake/public/papers/robust-arithmetic.ps">
     * Shewchuk (1997) Theorum 7</a>
     */
    static DD twoSum(double a, double b) {
        final double x = a + b;
        return new DD(x, twoSumLow(a, b, x));
    }

    /**
     * Compute the round-off of the sum of two numbers {@code a} and {@code b} using
     * Knuth two-sum algorithm. The values are not required to be ordered by magnitude,
     * i.e. the result is commutative {@code s = a + b == b + a}.
     *
     * @param a First part of sum.
     * @param b Second part of sum.
     * @param x Sum.
     * @return the sum round-off
     * @see #twoSum(double, double)
     */
    static double twoSumLow(double a, double b, double x) {
        // (x, xx) = a + b
        // bVirtual = x - a
        // aVirtual = x - bVirtual
        // bRoundoff = b - bVirtual
        // aRoundoff = a - aVirtual
        // xx = aRoundoff + bRoundoff
        final double bVirtual = x - a;
        return (a - (x - bVirtual)) + (b - bVirtual);
    }

    /**
     * Compute the difference of two numbers {@code a} and {@code b} using
     * Knuth's two-sum algorithm. The values are not required to be ordered by magnitude.
     *
     * <p>Computes the same results as {@link #twoSum(double, double) twoSum(a, -b)}.
     *
     * @param a Minuend.
     * @param b Subtrahend.
     * @return the difference
     * @see #twoSum(double, double)
     */
    static DD twoDiff(double a, double b) {
        final double x = a - b;
        return new DD(x, twoDiffLow(a, b, x));
    }

    /**
     * Compute the round-off of the difference of two numbers {@code a} and {@code b} using
     * Knuth two-sum algorithm. The values are not required to be ordered by magnitude,
     *
     * @param a Minuend.
     * @param b Subtrahend.
     * @param x Difference.
     * @return the difference round-off
     * @see #twoDiff(double, double)
     */
    private static double twoDiffLow(double a, double b, double x) {
        // (x, xx) = a - b
        // bVirtual = a - x
        // aVirtual = x + bVirtual
        // bRoundoff = b - bVirtual
        // aRoundoff = a - aVirtual
        // xx = aRoundoff - bRoundoff
        final double bVirtual = a - x;
        return (a - (x + bVirtual)) - (b - bVirtual);
    }

    /**
     * Compute the double-double number {@code (z,zz)} for the exact
     * product of {@code x} and {@code y}.
     *
     * <p>The high part of the number is equal to the product {@code z = x * y}.
     * The low part is set to the round-off of the {@code double} product.
     *
     * <p>This method ignores special handling of non-normal numbers and intermediate
     * overflow within the extended precision computation.
     * This creates the following special cases:
     *
     * <ul>
     *  <li>If {@code x * y} is sub-normal or zero then the low part is +/-0.0.
     *  <li>If {@code x * y} is infinite then the low part is NaN.
     *  <li>If {@code x} or {@code y} is infinite or NaN then the low part is NaN.
     *  <li>If either {@code |x|} or {@code |y|} multiplied by {@code 1 + 2^27}
     *      is infinite (intermediate overflow) then the low part is NaN.
     * </ul>
     *
     * <p>Note: Ignoring special cases is a design choice for performance. The
     * method is therefore not a drop-in replacement for
     * {@code round_off = Math.fma(x, y, -x * y)}.
     *
     * @param x First factor.
     * @param y Second factor.
     * @return the product
     */
    static DD twoProd(double x, double y) {
        final double xy = x * y;
        // No checks for non-normal xy, or overflow during the split of the arguments
        return new DD(xy, twoProductLow(x, y, xy));
    }

    /**
     * Compute the low part of the double length number {@code (z,zz)} for the exact
     * product of {@code x} and {@code y} using Dekker's mult12 algorithm. The standard
     * precision product {@code x*y} must be provided. The numbers {@code x} and {@code y}
     * are split into high and low parts using Dekker's algorithm.
     *
     * <p>Warning: This method does not perform scaling in Dekker's split and large
     * finite numbers can create NaN results.
     *
     * @param x First factor.
     * @param y Second factor.
     * @param xy Product of the factors (x * y).
     * @return the low part of the product double length number
     * @see #highPart(double)
     */
    static double twoProductLow(double x, double y, double xy) {
        // Split the numbers using Dekker's algorithm without scaling
        final double hx = highPart(x);
        final double lx = x - hx;
        final double hy = highPart(y);
        final double ly = y - hy;
        return twoProductLow(hx, lx, hy, ly, xy);
    }

    /**
     * Compute the low part of the double length number {@code (z,zz)} for the exact
     * product of {@code x} and {@code y} using Dekker's mult12 algorithm. The standard
     * precision product {@code x*y}, and the high and low parts of the factors must be
     * provided.
     *
     * @param hx High-part of first factor.
     * @param lx Low-part of first factor.
     * @param hy High-part of second factor.
     * @param ly Low-part of second factor.
     * @param xy Product of the factors (x * y).
     * @return the low part of the product double length number
     */
    static double twoProductLow(double hx, double lx, double hy, double ly, double xy) {
        // Compute the multiply low part:
        // err1 = xy - hx * hy
        // err2 = err1 - lx * hy
        // err3 = err2 - hx * ly
        // low = lx * ly - err3
        return lx * ly - (((xy - hx * hy) - lx * hy) - hx * ly);
    }

    /**
     * Compute the double-double number {@code (z,zz)} for the exact
     * square of {@code x}.
     *
     * <p>The high part of the number is equal to the square {@code z = x * x}.
     * The low part is set to the round-off of the {@code double} square.
     *
     * <p>This method is an optimisation of {@link #twoProd(double, double) twoProd(x, x)}.
     * See that method for details of special cases.
     *
     * @param x Factor.
     * @return the square
     * @see #twoProd(double, double)
     */
    static DD twoSquare(double x) {
        final double xx = x * x;
        // No checks for non-normal xy, or overflow during the split of the arguments
        return new DD(xx, twoSquareLow(x, xx));
    }

    /**
     * Compute the low part of the double length number {@code (z,zz)} for the exact
     * square of {@code x} using Dekker's mult12 algorithm. The standard
     * precision square {@code x*x} must be provided. The number {@code x}
     * is split into high and low parts using Dekker's algorithm.
     *
     * <p>Warning: This method does not perform scaling in Dekker's split and large
     * finite numbers can create NaN results.
     *
     * @param x Factor.
     * @param x2 Square of the factor (x * x).
     * @return the low part of the square double length number
     * @see #highPart(double)
     * @see #twoProductLow(double, double, double)
     */
    static double twoSquareLow(double x, double x2) {
        // See productLowUnscaled
        final double hx = highPart(x);
        final double lx = x - hx;
        return twoSquareLow(hx, lx, x2);
    }

    /**
     * Compute the low part of the double length number {@code (z,zz)} for the exact
     * square of {@code x} using Dekker's mult12 algorithm. The standard
     * precision square {@code x*x}, and the high and low parts of the factors must be
     * provided.
     *
     * @param hx High-part of factor.
     * @param lx Low-part of factor.
     * @param x2 Square of the factor (x * x).
     * @return the low part of the square double length number
     */
    static double twoSquareLow(double hx, double lx, double x2) {
        return lx * lx - ((x2 - hx * hx) - 2 * lx * hx);
    }

    /**
     * Implement Dekker's method to split a value into two parts. Multiplying by (2^s + 1) creates
     * a big value from which to derive the two split parts.
     * <pre>
     * c = (2^s + 1) * a
     * a_big = c - a
     * a_hi = c - a_big
     * a_lo = a - a_hi
     * a = a_hi + a_lo
     * </pre>
     *
     * <p>The multiplicand allows a p-bit value to be split into
     * (p-s)-bit value {@code a_hi} and a non-overlapping (s-1)-bit value {@code a_lo}.
     * Combined they have (p-1) bits of significand but the sign bit of {@code a_lo}
     * contains a bit of information. The constant is chosen so that s is ceil(p/2) where
     * the precision p for a double is 53-bits (1-bit of the mantissa is assumed to be
     * 1 for a non sub-normal number) and s is 27.
     *
     * <p>This conversion does not use scaling and the result of overflow is NaN. Overflow
     * may occur when the exponent of the input value is above 996.
     *
     * <p>Splitting a NaN or infinite value will return NaN.
     *
     * @param value Value.
     * @return the high part of the value.
     * @see Math#getExponent(double)
     */
    static double highPart(double value) {
        final double c = MULTIPLIER * value;
        return c - (c - value);
    }

    // Public API operations

    /**
     * Returns a {@code DD} whose value is the negation of both parts of double-double number.
     *
     * @return the negation
     */
    @Override
    public DD negate() {
        return new DD(-x, -xx);
    }

    /**
     * Returns a {@code DD} whose value is the absolute value of the number {@code (x, xx)}
     * This method assumes that the low part {@code xx} is the smaller magnitude.
     *
     * <p>Cases:
     * <ul>
     *  <li>If the {@code x} value is negative the result is {@code (-x, -xx)}.
     *  <li>If the {@code x} value is +/- 0.0 the result is {@code (0.0, 0.0)}; this
     *      will remove sign information from the round-off component assumed to be zero.
     *  <li>Otherwise the result is {@code this}.
     * </ul>
     *
     * @return the absolute value
     * @see #negate()
     * @see #ZERO
     */
    public DD abs() {
        // Assume |hi| > |lo|, i.e. the low part is the round-off
        if (x < 0) {
            return negate();
        }
        // NaN, positive or zero
        // return a canonical absolute of zero
        return x == 0 ? ZERO : this;
    }

    /**
     * Returns the largest (closest to positive infinity) {@code DD} value that is less
     * than or equal to {@code this} number {@code (x, xx)} and is equal to a mathematical integer.
     *
     * <p>This method may change the representation of zero and non-finite values; the
     * result is equivalent to {@code Math.floor(x)} and the {@code xx} part is ignored.
     *
     * <p>Cases:
     * <ul>
     *  <li>If {@code x} is NaN, then the result is {@code (NaN, 0)}.
     *  <li>If {@code x} is infinite, then the result is {@code (x, 0)}.
     *  <li>If {@code x} is +/-0.0, then the result is {@code (x, 0)}.
     *  <li>If {@code x != Math.floor(x)}, then the result is {@code (Math.floor(x), 0)}.
     *  <li>Otherwise the result is the {@code DD} value equal to the sum
     *      {@code Math.floor(x) + Math.floor(xx)}.
     * </ul>
     *
     * <p>The result may generate a high part smaller (closer to negative infinity) than
     * {@code Math.floor(x)} if {@code x} is a representable integer and the {@code xx} value
     * is negative.
     *
     * @return the largest (closest to positive infinity) value that is less than or equal
     * to {@code this} and is equal to a mathematical integer
     * @see Math#floor(double)
     * @see #isFinite()
     */
    public DD floor() {
        return floorOrCeil(x, xx, Math::floor);
    }

    /**
     * Returns the smallest (closest to negative infinity) {@code DD} value that is greater
     * than or equal to {@code this} number {@code (x, xx)} and is equal to a mathematical integer.
     *
     * <p>This method may change the representation of zero and non-finite values; the
     * result is equivalent to {@code Math.ceil(x)} and the {@code xx} part is ignored.
     *
     * <p>Cases:
     * <ul>
     *  <li>If {@code x} is NaN, then the result is {@code (NaN, 0)}.
     *  <li>If {@code x} is infinite, then the result is {@code (x, 0)}.
     *  <li>If {@code x} is +/-0.0, then the result is {@code (x, 0)}.
     *  <li>If {@code x != Math.ceil(x)}, then the result is {@code (Math.ceil(x), 0)}.
     *  <li>Otherwise the result is the {@code DD} value equal to the sum
     *      {@code Math.ceil(x) + Math.ceil(xx)}.
     * </ul>
     *
     * <p>The result may generate a high part larger (closer to positive infinity) than
     * {@code Math.ceil(x)} if {@code x} is a representable integer and the {@code xx} value
     * is positive.
     *
     * @return the smallest (closest to negative infinity) value that is greater than or equal
     * to {@code this} and is equal to a mathematical integer
     * @see Math#ceil(double)
     * @see #isFinite()
     */
    public DD ceil() {
        return floorOrCeil(x, xx, Math::ceil);
    }

    /**
     * Implementation of the floor and ceiling functions.
     *
     * <p>Cases:
     * <ul>
     *  <li>If {@code x} is non-finite or zero, then the result is {@code (x, 0)}.
     *  <li>If {@code x} is rounded by the operator to a new value {@code y}, then the
     *      result is {@code (y, 0)}.
     *  <li>Otherwise the result is the {@code DD} value equal to the sum {@code op(x) + op(xx)}.
     * </ul>
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param op Floor or ceiling operator.
     * @return the result
     */
    private static DD floorOrCeil(double x, double xx, DoubleUnaryOperator op) {
        // Assume |hi| > |lo|, i.e. the low part is the round-off
        final double y = op.applyAsDouble(x);
        // Note: The floating-point comparison is intentional
        if (y == x) {
            // Handle non-finite and zero by ignoring the low part
            if (isNotNormal(y)) {
                return new DD(y, 0);
            }
            // High part is an integer, use the low part.
            // Any rounding must propagate to the high part.
            // Note: add 0.0 to convert -0.0 to 0.0. This is required to ensure
            // the round-off component of the fastTwoSum result is always 0.0
            // when yy == 0. This only applies in the ceiling operator when
            // xx is in (-1, 0] and will be converted to -0.0.
            final double yy = op.applyAsDouble(xx) + 0;
            return fastTwoSum(y, yy);
        }
        // NaN or already rounded.
        // xx has no effect on the rounding.
        return new DD(y, 0);
    }

    /**
     * Returns a {@code DD} whose value is {@code (this + y)}.
     *
     * <p>This computes the same result as
     * {@link #add(DD) add(DD.of(y))}.
     *
     * <p>The computed result is within 2 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * @param y Value to be added to this number.
     * @return {@code this + y}.
     * @see #add(DD)
     */
    public DD add(double y) {
        // (s0, s1) = x + y
        final double s0 = x + y;
        final double s1 = twoSumLow(x, y, s0);
        // Note: if x + y cancel to a non-zero result then s.x is >= 1 ulp of x.
        // This is larger than xx so fast-two-sum can be used.
        return fastTwoSum(s0, s1 + xx);
    }

    /**
     * Returns a {@code DD} whose value is {@code (this + y)}.
     *
     * <p>The computed result is within 4 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * @param y Value to be added to this number.
     * @return {@code this + y}.
     */
    @Override
    public DD add(DD y) {
        return add(x, xx, y.x, y.xx);
    }

    /**
     * Compute the sum of {@code (x, xx)} and {@code (y, yy)}.
     *
     * <p>The computed result is within 4 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param y High part of y.
     * @param yy Low part of y.
     * @return the sum
     * @see #accurateAdd(double, double, double, double)
     */
    static DD add(double x, double xx, double y, double yy) {
        // Sum parts and save
        // (s0, s1) = x + y
        final double s0 = x + y;
        final double s1 = twoSumLow(x, y, s0);
        // (t0, t1) = xx + yy
        final double t0 = xx + yy;
        final double t1 = twoSumLow(xx, yy, t0);
        // result = s + t
        // |s1| is >= 1 ulp of max(|x|, |y|)
        // |t0| is >= 1 ulp of max(|xx|, |yy|)
        final DD zz = fastTwoSum(s0, s1 + t0);
        return fastTwoSum(zz.x, zz.xx + t1);
    }

    /**
     * Compute the sum of {@code (x, xx)} and {@code y}.
     *
     * <p>This computes the same result as
     * {@link #accurateAdd(double, double, double, double) accurateAdd(x, xx, y, 0)}.
     *
     * <p>Note: This is an internal helper method used when accuracy is required.
     * The computed result is within 1 eps of the exact result where eps is 2<sup>-106</sup>.
     * The performance is approximately 1.5-fold slower than {@link #add(double)}.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param y y.
     * @return the sum
     */
    static DD accurateAdd(double x, double xx, double y) {
        // Grow expansion (Schewchuk): (x, xx) + y -> (s0, s1, s2)
        DD s = twoSum(xx, y);
        double s2 = s.xx;
        s = twoSum(x, s.x);
        final double s0 = s.x;
        final double s1 = s.xx;
        // Compress (Schewchuk Fig. 15): (s0, s1, s2) -> (s0, s1)
        s = fastTwoSum(s1, s2);
        s2 = s.xx;
        s = fastTwoSum(s0, s.x);
        // Here (s0, s1) = s
        // e = exact 159-bit result
        // |e - s0| <= ulp(s0)
        // |s1 + s2| <= ulp(e - s0)
        return fastTwoSum(s.x, s2 + s.xx);
    }

    /**
     * Compute the sum of {@code (x, xx)} and {@code (y, yy)}.
     *
     * <p>The high-part of the result is within 1 ulp of the true sum {@code e}.
     * The low-part of the result is within 1 ulp of the result of the high-part
     * subtracted from the true sum {@code e - hi}.
     *
     * <p>Note: This is an internal helper method used when accuracy is required.
     * The computed result is within 1 eps of the exact result where eps is 2<sup>-106</sup>.
     * The performance is approximately 2-fold slower than {@link #add(DD)}.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param y High part of y.
     * @param yy Low part of y.
     * @return the sum
     */
    static DD accurateAdd(double x, double xx, double y, double yy) {
        // Expansion sum (Schewchuk Fig 7): (x, xx) + (x, yy) -> (s0, s1, s2, s3)
        DD s = twoSum(xx, yy);
        double s3 = s.xx;
        s = twoSum(x, s.x);
        // (s0, s1, s2) == (s.x, s.xx, s3)
        double s0 = s.x;
        s = twoSum(s.xx, y);
        double s2 = s.xx;
        s = twoSum(s0, s.x);
        // s1 = s.xx
        s0 = s.x;
        // Compress (Schewchuk Fig. 15) (s0, s1, s2, s3) -> (s0, s1)
        s = fastTwoSum(s.xx, s2);
        final double s1 = s.x;
        s = fastTwoSum(s.xx, s3);
        // s2 = s.x
        s3 = s.xx;
        s = fastTwoSum(s1, s.x);
        s2 = s.xx;
        s = fastTwoSum(s0, s.x);
        // Here (s0, s1) = s
        // e = exact 212-bit result
        // |e - s0| <= ulp(s0)
        // |s1 + s2 + s3| <= ulp(e - s0)   (Sum magnitudes small to high)
        return fastTwoSum(s.x, s3 + s2 + s.xx);
    }

    /**
     * Returns a {@code DD} whose value is {@code (this - y)}.
     *
     * <p>This computes the same result as {@link #add(DD) add(-y)}.
     *
     * <p>The computed result is within 2 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * @param y Value to be subtracted from this number.
     * @return {@code this - y}.
     * @see #subtract(DD)
     */
    public DD subtract(double y) {
        return add(-y);
    }

    /**
     * Returns a {@code DD} whose value is {@code (this - y)}.
     *
     * <p>This computes the same result as {@link #add(DD) add(y.negate())}.
     *
     * <p>The computed result is within 4 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * @param y Value to be subtracted from this number.
     * @return {@code this - y}.
     */
    @Override
    public DD subtract(DD y) {
        return add(x, xx, -y.x, -y.xx);
    }

    /**
     * Returns a {@code DD} whose value is {@code this * y}.
     *
     * <p>This computes the same result as
     * {@link #multiply(DD) multiply(DD.of(y))}.
     *
     * <p>The computed result is within 4 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * @param y Factor.
     * @return {@code this * y}.
     * @see #multiply(DD)
     */
    public DD multiply(double y) {
        return multiply(x, xx, y);
    }

    /**
     * Compute the multiplication product of {@code (x, xx)} and {@code y}.
     *
     * <p>This computes the same result as
     * {@link #multiply(double, double, double, double) multiply(x, xx, y, 0)}.
     *
     * <p>The computed result is within 4 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param y High part of y.
     * @return the product
     * @see #multiply(double, double, double, double)
     */
    private static DD multiply(double x, double xx, double y) {
        // Dekker mul2 with yy=0
        // (Alternative: Scale expansion (Schewchuk Fig 13))
        final double hi = x * y;
        final double lo = twoProductLow(x, y, hi);
        // Save 2 FLOPS compared to multiply(x, xx, y, 0).
        // This is reused in divide to save more FLOPS so worth the optimisation.
        return fastTwoSum(hi, lo + xx * y);
    }

    /**
     * Returns a {@code DD} whose value is {@code this * y}.
     *
     * <p>The computed result is within 4 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * @param y Factor.
     * @return {@code this * y}.
     */
    @Override
    public DD multiply(DD y) {
        return multiply(x, xx, y.x, y.xx);
    }

    /**
     * Compute the multiplication product of {@code (x, xx)} and {@code (y, yy)}.
     *
     * <p>The computed result is within 4 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param y High part of y.
     * @param yy Low part of y.
     * @return the product
     */
    private static DD multiply(double x, double xx, double y, double yy) {
        // Dekker mul2
        // (Alternative: Scale expansion (Schewchuk Fig 13))
        final double hi = x * y;
        final double lo = twoProductLow(x, y, hi);
        return fastTwoSum(hi, lo + (x * yy + xx * y));
    }

    /**
     * Returns a {@code DD} whose value is {@code this * this}.
     *
     * <p>This method is an optimisation of {@link #multiply(DD) multiply(this)}.
     *
     * <p>The computed result is within 4 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * @return {@code this}<sup>2</sup>
     * @see #multiply(DD)
     */
    public DD square() {
        return square(x, xx);
    }

    /**
     * Compute the square of {@code (x, xx)}.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @return the square
     */
    private static DD square(double x, double xx) {
        // Dekker mul2
        final double hi = x * x;
        final double lo = twoSquareLow(x, hi);
        return fastTwoSum(hi, lo + (2 * x * xx));
    }

    /**
     * Returns a {@code DD} whose value is {@code (this / y)}.
     * If {@code y = 0} the result is undefined.
     *
     * <p>The computed result is within 1 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * @param y Divisor.
     * @return {@code this / y}.
     */
    public DD divide(double y) {
        return divide(x, xx, y);
    }

    /**
     * Compute the division of {@code (x, xx)} by {@code y}.
     * If {@code y = 0} the result is undefined.
     *
     * <p>The computed result is within 1 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param y High part of y.
     * @return the quotient
     */
    private static DD divide(double x, double xx, double y) {
        // Long division
        // quotient q0 = x / y
        final double q0 = x / y;
        // remainder r0 = x - q0 * y
        DD p = twoProd(y, q0);
        // High accuracy add required
        DD r = accurateAdd(x, xx, -p.x, -p.xx);
        // next quotient q1 = r0 / y
        final double q1 = r.x / y;
        // remainder r1 = r0 - q1 * y
        p = twoProd(y, q1);
        // accurateAdd not used as we do not need r1.xx
        r = add(r.x, r.xx, -p.x, -p.xx);
        // next quotient q2 = r1 / y
        final double q2 = r.x / y;
        // Collect (q0, q1, q2)
        final DD q = fastTwoSum(q0, q1);
        return twoSum(q.x, q.xx + q2);
    }

    /**
     * Returns a {@code DD} whose value is {@code (this / y)}.
     * If {@code y = 0} the result is undefined.
     *
     * <p>The computed result is within 4 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * @param y Divisor.
     * @return {@code this / y}.
     */
    @Override
    public DD divide(DD y) {
        return divide(x, xx, y.x, y.xx);
    }

    /**
     * Compute the division of {@code (x, xx)} by {@code (y, yy)}.
     * If {@code y = 0} the result is undefined.
     *
     * <p>The computed result is within 4 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * @param x High part of x.
     * @param xx Low part of x.
     * @param y High part of y.
     * @param yy Low part of y.
     * @return the quotient
     */
    private static DD divide(double x, double xx, double y, double yy) {
        // Long division
        // quotient q0 = x / y
        final double q0 = x / y;
        // remainder r0 = x - q0 * y
        DD p = multiply(y, yy, q0);
        // High accuracy add required
        DD r = accurateAdd(x, xx, -p.x, -p.xx);
        // next quotient q1 = r0 / y
        final double q1 = r.x / y;
        // remainder r1 = r0 - q1 * y
        p = multiply(y, yy, q1);
        // accurateAdd not used as we do not need r1.xx
        r = add(r.x, r.xx, -p.x, -p.xx);
        // next quotient q2 = r1 / y
        final double q2 = r.x / y;
        // Collect (q0, q1, q2)
        final DD q = fastTwoSum(q0, q1);
        return twoSum(q.x, q.xx + q2);
    }

    /**
     * Compute the reciprocal of {@code this}.
     * If {@code this} value is zero the result is undefined.
     *
     * <p>The computed result is within 4 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * @return {@code this}<sup>-1</sup>
     */
    @Override
    public DD reciprocal() {
        return reciprocal(x, xx);
    }

    /**
     * Compute the inverse of {@code (y, yy)}.
     * If {@code y = 0} the result is undefined.
     *
     * <p>The computed result is within 4 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * @param y High part of y.
     * @param yy Low part of y.
     * @return the inverse
     */
    private static DD reciprocal(double y, double yy) {
        // As per divide using (x, xx) = (1, 0)
        // quotient q0 = x / y
        final double q0 = 1 / y;
        // remainder r0 = x - q0 * y
        DD p = multiply(y, yy, q0);
        // High accuracy add required
        // This add saves 2 twoSum and 3 fastTwoSum (24 FLOPS) by ignoring the zero low part
        DD r = accurateAdd(-p.x, -p.xx, 1);
        // next quotient q1 = r0 / y
        final double q1 = r.x / y;
        // remainder r1 = r0 - q1 * y
        p = multiply(y, yy, q1);
        // accurateAdd not used as we do not need r1.xx
        r = add(r.x, r.xx, -p.x, -p.xx);
        // next quotient q2 = r1 / y
        final double q2 = r.x / y;
        // Collect (q0, q1, q2)
        final DD q = fastTwoSum(q0, q1);
        return twoSum(q.x, q.xx + q2);
    }

    /**
     * Compute the square root of {@code this} number {@code (x, xx)}.
     *
     * <p>Uses the result {@code Math.sqrt(x)}
     * if that result is not a finite normalized {@code double}.
     *
     * <p>Special cases:
     * <ul>
     *  <li>If {@code x} is NaN or less than zero, then the result is {@code (NaN, 0)}.
     *  <li>If {@code x} is positive infinity, then the result is {@code (+infinity, 0)}.
     *  <li>If {@code x} is positive zero or negative zero, then the result is {@code (x, 0)}.
     * </ul>
     *
     * <p>The computed result is within 4 eps of the exact result where eps is 2<sup>-106</sup>.
     *
     * @return {@code sqrt(this)}
     * @see Math#sqrt(double)
     * @see Double#MIN_NORMAL
     */
    public DD sqrt() {
        // Standard sqrt
        final double c = Math.sqrt(x);

        // Here we support {negative, +infinity, nan and zero} edge cases.
        // This is required to avoid a divide by zero in the following
        // computation, otherwise (0, 0).sqrt() = (NaN, NaN).
        if (isNotNormal(c)) {
            return new DD(c, 0);
        }

        // Here hi is positive, non-zero and finite; assume lo is also finite

        // Dekker's double precision sqrt2 algorithm.
        // See Dekker, 1971, pp 242.
        final double hc = highPart(c);
        final double lc = c - hc;
        final double u = c * c;
        final double uu = twoSquareLow(hc, lc, u);
        final double cc = (x - u - uu + xx) * 0.5 / c;

        // Extended precision result:
        // y = c + cc
        // yy = c - y + cc
        return fastTwoSum(c, cc);
    }

    /**
     * Checks if the number is not normal. This is functionally equivalent to:
     * <pre>{@code
     * final double abs = Math.abs(a);
     * return (abs <= Double.MIN_NORMAL || !(abs <= Double.MAX_VALUE));
     * }</pre>
     *
     * @param a The value.
     * @return true if the value is not normal
     */
    static boolean isNotNormal(double a) {
        // Sub-normal numbers have a biased exponent of 0.
        // Inf/NaN numbers have a biased exponent of 2047.
        // Catch both cases by extracting the raw exponent, subtracting 1
        // and compare unsigned (so 0 underflows to a unsigned large value).
        final int baisedExponent = ((int) (Double.doubleToRawLongBits(a) >>> 52)) & EXP_MASK;
        // Pre-compute the additions used by Integer.compareUnsigned
        return baisedExponent + CMP_UNSIGNED_MINUS_1 >= CMP_UNSIGNED_2046;
    }

    /**
     * Multiply {@code this} number {@code (x, xx)} by an integral power of two.
     * <pre>
     * (y, yy) = (x, xx) * 2^exp
     * </pre>
     *
     * <p>The result is rounded as if performed by a single correctly rounded floating-point
     * multiply. This performs the same result as:
     * <pre>
     * y = Math.scalb(x, exp);
     * yy = Math.scalb(xx, exp);
     * </pre>
     *
     * <p>The implementation computes using a single multiplication if {@code exp}
     * is in {@code [-1022, 1023]}. Otherwise the parts {@code (x, xx)} are scaled by
     * repeated multiplication by power-of-two factors. The result is exact unless the scaling
     * generates sub-normal parts; in this case precision may be lost by a single rounding.
     *
     * @param exp Power of two scale factor.
     * @return the result
     * @see Math#scalb(double, int)
     * @see #frexp(int[])
     */
    public DD scalb(int exp) {
        // Handle scaling when 2^n can be represented with a single normal number
        // n >= -1022 && n <= 1023
        // Using unsigned compare => n + 1022 <= 1023 + 1022
        if (exp + CMP_UNSIGNED_1022 < CMP_UNSIGNED_2046) {
            final double s = twoPow(exp);
            return new DD(x * s, xx * s);
        }

        // Scale by multiples of 2^512 (largest representable power of 2).
        // Scaling requires max 5 multiplications to under/overflow any normal value.
        // Break this down into e.g.: 2^512^(exp / 512) * 2^(exp % 512)
        // Number of multiples n = exp / 512   : exp >>> 9
        // Remainder           m = exp % 512   : exp & 511  (exp must be positive)
        int n;
        int m;
        double p;
        if (exp < 0) {
            // Downscaling
            // (Note: Using an unsigned shift handles negation of min value: -2^31)
            n = -exp >>> 9;
            // m = exp % 512
            m = -(-exp & 511);
            p = TWO_POW_M512;
        } else {
            // Upscaling
            n = exp >>> 9;
            m = exp & 511;
            p = TWO_POW_512;
        }

        // Multiply by the remainder scaling factor first. The remaining multiplications
        // are either 2^512 or 2^-512.
        // Down-scaling to sub-normal will use the final multiplication into a sub-normal result.
        // Note here that n >= 1 as the n in [-1022, 1023] case has been handled.

        double z0;
        double z1;

        // Handle n : 1, 2, 3, 4, 5
        if (n >= 5) {
            // n >= 5 will be over/underflow. Use an extreme scale factor.
            // Do not use +/- infinity as this creates NaN if x = 0.
            // p -> 2^1023 or 2^-1025
            p *= p * 0.5;
            z0 = x * p * p * p;
            z1 = xx * p * p * p;
            return new DD(z0, z1);
        }

        final double s = twoPow(m);
        if (n == 4) {
            z0 = x * s * p * p * p * p;
            z1 = xx * s * p * p * p * p;
        } else if (n == 3) {
            z0 = x * s * p * p * p;
            z1 = xx * s * p * p * p;
        } else if (n == 2) {
            z0 = x * s * p * p;
            z1 = xx * s * p * p;
        } else {
            // n = 1. Occurs only if exp = -1023.
            z0 = x * s * p;
            z1 = xx * s * p;
        }
        return new DD(z0, z1);
    }

    /**
     * Create a normalized double with the value {@code 2^n}.
     *
     * <p>Warning: Do not call with {@code n = -1023}. This will create zero.
     *
     * @param n Exponent (in the range [-1022, 1023]).
     * @return the double
     */
    static double twoPow(int n) {
        return Double.longBitsToDouble(((long) (n + 1023)) << 52);
    }

    /**
     * Convert {@code this} number {@code x} to fractional {@code f} and integral
     * {@code 2^exp} components.
     * <pre>
     * x = f * 2^exp
     * </pre>
     *
     * <p>The combined fractional part (f, ff) is in the range {@code [0.5, 1)}.
     *
     * <p>Special cases:
     * <ul>
     *  <li>If {@code x} is zero, then the normalized fraction is zero and the exponent is zero.
     *  <li>If {@code x} is NaN, then the normalized fraction is NaN and the exponent is unspecified.
     *  <li>If {@code x} is infinite, then the normalized fraction is infinite and the exponent is unspecified.
     *  <li>If high-part {@code x} is an exact power of 2 and the low-part {@code xx} has an opposite
     *      signed non-zero magnitude then fraction high-part {@code f} will be {@code +/-1} such that
     *      the double-double number is in the range {@code [0.5, 1)}.
     * </ul>
     *
     * <p>This is named using the equivalent function in the standard C math.h library.
     *
     * @param exp Power of two scale factor (integral exponent).
     * @return Fraction part.
     * @see Math#getExponent(double)
     * @see #scalb(int)
     * @see <a href="https://www.cplusplus.com/reference/cmath/frexp/">C math.h frexp</a>
     */
    public DD frexp(int[] exp) {
        exp[0] = getScale(x);
        // Handle non-scalable numbers
        if (exp[0] == Double.MAX_EXPONENT + 1) {
            // Returns +/-0.0, inf or nan
            // Maintain the fractional part unchanged.
            // Do not change the fractional part of inf/nan, and assume
            // |xx| < |x| thus if x == 0 then xx == 0 (otherwise the double-double is invalid)
            // Unspecified for NaN/inf so just return zero exponent.
            exp[0] = 0;
            return this;
        }
        // The scale will create the fraction in [1, 2) so increase by 1 for [0.5, 1)
        exp[0] += 1;
        DD f = scalb(-exp[0]);
        // Return |(hi, lo)| = (1, -eps) if required.
        // f.x * f.xx < 0 detects sign change unless the product underflows.
        // Handle extreme case of |f.xx| being min value by doubling f.x to 1.
        if (Math.abs(f.x) == HALF && 2 * f.x * f.xx < 0) {
            f = new DD(f.x * 2, f.xx * 2);
            exp[0] -= 1;
        }
        return f;
    }

    /**
     * Returns a scale suitable for use with {@link Math#scalb(double, int)} to normalise
     * the number to the interval {@code [1, 2)}.
     *
     * <p>In contrast to {@link Math#getExponent(double)} this handles
     * sub-normal numbers by computing the number of leading zeros in the mantissa
     * and shifting the unbiased exponent. The result is that for all finite, non-zero,
     * numbers, the magnitude of {@code scalb(x, -getScale(x))} is
     * always in the range {@code [1, 2)}.
     *
     * <p>This method is a functional equivalent of the c function ilogb(double).
     *
     * <p>The result is to be used to scale a number using {@link Math#scalb(double, int)}.
     * Hence the special case of a zero argument is handled using the return value for NaN
     * as zero cannot be scaled. This is different from {@link Math#getExponent(double)}.
     *
     * <p>Special cases:
     * <ul>
     *  <li>If the argument is NaN or infinite, then the result is {@link Double#MAX_EXPONENT} + 1.
     *  <li>If the argument is zero, then the result is {@link Double#MAX_EXPONENT} + 1.
     * </ul>
     *
     * @param a Value.
     * @return The unbiased exponent of the value to be used for scaling, or 1024 for 0, NaN or Inf
     * @see Math#getExponent(double)
     * @see Math#scalb(double, int)
     * @see <a href="https://www.cplusplus.com/reference/cmath/ilogb/">ilogb</a>
     */
    private static int getScale(double a) {
        // Only interested in the exponent and mantissa so remove the sign bit
        final long bits = Double.doubleToRawLongBits(a) & UNSIGN_MASK;
        // Get the unbiased exponent
        int exp = ((int) (bits >>> 52)) - EXPONENT_OFFSET;

        // No case to distinguish nan/inf (exp == 1024).
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
     * Compute {@code this} number {@code (x, xx)} raised to the power {@code n}.
     *
     * <p>Special cases:
     * <ul>
     *  <li>If {@code x} is not a finite normalized {@code double}, the low part {@code xx}
     *      is ignored and the result is {@link Math#pow(double, double) Math.pow(x, n)}.
     *  <li>If {@code n = 0} the result is {@code (1, 0)}.
     *  <li>If {@code n = 1} the result is {@code (x, xx)}.
     *  <li>If {@code n = -1} the result is the {@link #reciprocal() reciprocal}.
     *  <li>If the computation overflows the result is undefined.
     * </ul>
     *
     * <p>Computation uses multiplication by factors generated by repeat squaring of the value.
     * These multiplications have no special case handling for overflow; in the event of overflow
     * the result is undefined. The {@link #pow(int, long[])} method can be used to
     * generate a scaled fraction result for any finite {@code DD} number and exponent.
     *
     * <p>The computed result is approximately {@code 16 * (n - 1) * eps} of the exact result
     * where eps is 2<sup>-106</sup>.
     *
     * @param n Exponent.
     * @return {@code this}<sup>n</sup>
     * @see Math#pow(double, double)
     * @see #pow(int, long[])
     * @see #isFinite()
     */
    @Override
    public DD pow(int n) {
        // Edge cases.
        if (n == 1) {
            return this;
        }
        if (n == 0) {
            return ONE;
        }

        // Handles {infinity, nan and zero} cases
        if (isNotNormal(x)) {
            // Assume the high part has the greatest magnitude
            // so here the low part is irrelevant
            return new DD(Math.pow(x, n), 0);
        }

        // Here hi is finite; assume lo is also finite
        if (n == -1) {
            return reciprocal();
        }

        // Extended precision computation is required.
        // No checks for overflow.
        if (n < 0) {
            // Note: Correctly handles negating -2^31
            return computePow(x, xx, -n).reciprocal();
        }
        return computePow(x, xx, n);
    }

    /**
     * Compute the number {@code x} (non-zero finite) raised to the power {@code n}.
     *
     * <p>The input power is treated as an unsigned integer. Thus the negative value
     * {@link Integer#MIN_VALUE} is 2^31.
     *
     * @param x Fractional high part of x.
     * @param xx Fractional low part of x.
     * @param n Power (in [2, 2^31]).
     * @return x^n.
     */
    private static DD computePow(double x, double xx, int n) {
        // Compute the power by multiplication (keeping track of the scale):
        // 13 = 1101
        // x^13 = x^8 * x^4 * x^1
        //      = ((x^2 * x)^2)^2 * x
        // 21 = 10101
        // x^21 = x^16 * x^4 * x^1
        //      = (((x^2)^2 * x)^2)^2 * x
        // 1. Find highest set bit in n (assume n != 0)
        // 2. Initialise result as x
        // 3. For remaining bits (0 or 1) below the highest set bit:
        //    - square the current result
        //    - if the current bit is 1 then multiply by x
        // In this scheme the factors to multiply by x can be pre-computed.

        // Split b
        final double xh = highPart(x);
        final double xl = x - xh;

        // Initialise the result as x^1
        double f0 = x;
        double f1 = xx;

        double u;
        double v;
        double w;

        // Shift the highest set bit off the top.
        // Any remaining bits are detected in the sign bit.
        final int shift = Integer.numberOfLeadingZeros(n) + 1;
        int bits = n << shift;

        // Multiplication is done without object allocation of DD intermediates.
        // The square can be optimised.
        // Process remaining bits below highest set bit.
        for (int i = 32 - shift; i != 0; i--, bits <<= 1) {
            // Square the result
            // Inline multiply(f0, f1, f0, f1), adapted for squaring
            u = f0 * f0;
            v = twoSquareLow(f0, u);
            // Inline (f0, f1) = fastTwoSum(hi, lo + (2 * f0 * f1))
            w = v + (2 * f0 * f1);
            f0 = u + w;
            f1 = fastTwoSumLow(u, w, f0);
            if (bits < 0) {
                // Inline multiply(f0, f1, x, xx)
                u = highPart(f0);
                v = f0 - u;
                w = f0 * x;
                v = twoProductLow(u, v, xh, xl, w);
                // Inline (f0, f1) = fastTwoSum(w, v + (f0 * xx + f1 * x))
                u = v + (f0 * xx + f1 * x);
                f0 = w + u;
                f1 = fastTwoSumLow(w, u, f0);
            }
        }

        return new DD(f0, f1);
    }

    /**
     * Compute {@code this} number {@code x} raised to the power {@code n}.
     *
     * <p>The value is returned as fractional {@code f} and integral
     * {@code 2^exp} components.
     * <pre>
     * (x+xx)^n = (f+ff) * 2^exp
     * </pre>
     *
     * <p>The combined fractional part (f, ff) is in the range {@code [0.5, 1)}.
     *
     * <p>Special cases:
     * <ul>
     *  <li>If {@code (x, xx)} is zero the high part of the fractional part is
     *      computed using {@link Math#pow(double, double) Math.pow(x, n)} and the exponent is 0.
     *  <li>If {@code n = 0} the fractional part is 0.5 and the exponent is 1.
     *  <li>If {@code (x, xx)} is an exact power of 2 the fractional part is 0.5 and the exponent
     *      is the power of 2 minus 1.
     *  <li>If the result high-part is an exact power of 2 and the low-part has an opposite
     *      signed non-zero magnitude then the fraction high-part {@code f} will be {@code +/-1} such that
     *      the double-double number is in the range {@code [0.5, 1)}.
     *  <li>If the argument is not finite then a fractional representation is not possible.
     *      In this case the fraction and the scale factor is undefined.
     * </ul>
     *
     * <p>The computed result is approximately {@code 16 * (n - 1) * eps} of the exact result
     * where eps is 2<sup>-106</sup>.
     *
     * @param n Power.
     * @param exp Result power of two scale factor (integral exponent).
     * @return Fraction part.
     * @see #frexp(int[])
     */
    public DD pow(int n, long[] exp) {
        // Edge cases.
        if (n == 0) {
            exp[0] = 1;
            return new DD(0.5, 0);
        }
        // IEEE result for non-finite or zero
        if (!Double.isFinite(x) || x == 0) {
            exp[0] = 0;
            return new DD(Math.pow(x, n), 0);
        }
        // Here the number is non-zero finite
        final int[] ie = {0};
        DD f = frexp(ie);
        final long b = ie[0];
        // Handle exact powers of 2
        if (Math.abs(f.x) == HALF && f.xx == 0) {
            // (f * 2^b)^n = (2f)^n * 2^(b-1)^n
            // Use Math.pow to create the sign.
            // Note the result must be scaled to the fractional representation
            // by multiplication by 0.5 and addition of 1 to the exponent.
            final double y0 = 0.5 * Math.pow(2 * f.x, n);
            // Propagate sign change (y0*f.x) to the original zero (this.xx)
            final double y1 = Math.copySign(0.0, y0 * f.x * this.xx);
            exp[0] = 1 + (b - 1) * n;
            return new DD(y0, y1);
        }
        if (n < 0) {
            f = computePowScaled(b, f.x, f.xx, -n, exp);
            // Result is a non-zero fraction part so inversion is safe
            f = reciprocal(f.x, f.xx);
            // Rescale to [0.5, 1.0)
            f = f.frexp(ie);
            exp[0] = ie[0] - exp[0];
            return f;
        }
        return computePowScaled(b, f.x, f.xx, n, exp);
    }

    /**
     * Compute the number {@code x} (non-zero finite) raised to the power {@code n}.
     *
     * <p>The input power is treated as an unsigned integer. Thus the negative value
     * {@link Integer#MIN_VALUE} is 2^31.
     *
     * @param b Integral component 2^exp of x.
     * @param x Fractional high part of x.
     * @param xx Fractional low part of x.
     * @param n Power (in [2, 2^31]).
     * @param exp Result power of two scale factor (integral exponent).
     * @return Fraction part.
     */
    private static DD computePowScaled(long b, double x, double xx, int n, long[] exp) {
        // Compute the power by multiplication (keeping track of the scale):
        // 13 = 1101
        // x^13 = x^8 * x^4 * x^1
        //      = ((x^2 * x)^2)^2 * x
        // 21 = 10101
        // x^21 = x^16 * x^4 * x^1
        //      = (((x^2)^2 * x)^2)^2 * x
        // 1. Find highest set bit in n (assume n != 0)
        // 2. Initialise result as x
        // 3. For remaining bits (0 or 1) below the highest set bit:
        //    - square the current result
        //    - if the current bit is 1 then multiply by x
        // In this scheme the factors to multiply by x can be pre-computed.

        // Scale the input in [0.5, 1) to be above 1. Represented as 2^be * b.
        final long be = b - 1;
        final double b0 = x * 2;
        final double b1 = xx * 2;
        // Split b
        final double b0h = highPart(b0);
        final double b0l = b0 - b0h;

        // Initialise the result as x^1. Represented as 2^fe * f.
        long fe = be;
        double f0 = b0;
        double f1 = b1;

        double u;
        double v;
        double w;

        // Shift the highest set bit off the top.
        // Any remaining bits are detected in the sign bit.
        final int shift = Integer.numberOfLeadingZeros(n) + 1;
        int bits = n << shift;

        // Multiplication is done without using DD.multiply as the arguments
        // are always finite and the product will not overflow. The square can be optimised.
        // Process remaining bits below highest set bit.
        for (int i = 32 - shift; i != 0; i--, bits <<= 1) {
            // Square the result
            // Inline multiply(f0, f1, f0, f1, f), adapted for squaring
            fe <<= 1;
            u = f0 * f0;
            v = twoSquareLow(f0, u);
            // Inline fastTwoSum(hi, lo + (2 * f0 * f1), f)
            w = v + (2 * f0 * f1);
            f0 = u + w;
            f1 = fastTwoSumLow(u, w, f0);
            // Rescale
            if (Math.abs(f0) > SAFE_MULTIPLY) {
                // Scale back to the [1, 2) range. As safe multiply is 2^500
                // the exponent should be < 1001 so the twoPow scaling factor is supported.
                final int e = Math.getExponent(f0);
                final double s = twoPow(-e);
                fe += e;
                f0 *= s;
                f1 *= s;
            }
            if (bits < 0) {
                // Multiply by b
                fe += be;
                // Inline multiply(f0, f1, b0, b1, f)
                u = highPart(f0);
                v = f0 - u;
                w = f0 * b0;
                v = twoProductLow(u, v, b0h, b0l, w);
                // Inline fastTwoSum(w, v + (f0 * b1 + f1 * b0), f)
                u = v + (f0 * b1 + f1 * b0);
                f0 = w + u;
                f1 = fastTwoSumLow(w, u, f0);
                // Avoid rescale as x2 is in [1, 2)
            }
        }

        final int[] e = {0};
        final DD f = new DD(f0, f1).frexp(e);
        exp[0] = fe + e[0];
        return f;
    }

    /**
     * Test for equality with another object. If the other object is a {@code DD} then a
     * comparison is made of the parts; otherwise {@code false} is returned.
     *
     * <p>If both parts of two double-double numbers
     * are numerically equivalent the two {@code DD} objects are considered to be equal.
     * For this purpose, two {@code double} values are considered to be
     * the same if and only if the method call
     * {@link Double#doubleToLongBits(double) Double.doubleToLongBits(value + 0.0)}
     * returns the identical {@code long} when applied to each value. This provides
     * numeric equality of different representations of zero as per {@code -0.0 == 0.0},
     * and equality of {@code NaN} values.
     *
     * <p>Note that in most cases, for two instances of class
     * {@code DD}, {@code x} and {@code y}, the
     * value of {@code x.equals(y)} is {@code true} if and only if
     *
     * <pre>
     *  {@code x.hi() == y.hi() && x.lo() == y.lo()}</pre>
     *
     * <p>also has the value {@code true}. However, there are exceptions:
     *
     * <ul>
     *  <li>Instances that contain {@code NaN} values in the same part
     *      are considered to be equal for that part, even though {@code Double.NaN == Double.NaN}
     *      has the value {@code false}.
     *  <li>Instances that share a {@code NaN} value in one part
     *      but have different values in the other part are <em>not</em> considered equal.
     * </ul>
     *
     * <p>The behavior is the same as if the components of the two double-double numbers were passed
     * to {@link java.util.Arrays#equals(double[], double[]) Arrays.equals(double[], double[])}:
     *
     * <pre>
     *  Arrays.equals(new double[]{x.hi() + 0.0, x.lo() + 0.0},
     *                new double[]{y.hi() + 0.0, y.lo() + 0.0}); </pre>
     *
     * <p>Note: Addition of {@code 0.0} converts signed representations of zero values
     * {@code -0.0} and {@code 0.0} to a canonical {@code 0.0}.
     *
     * @param other Object to test for equality with this instance.
     * @return {@code true} if the objects are equal, {@code false} if object
     * is {@code null}, not an instance of {@code DD}, or not equal to
     * this instance.
     * @see Double#doubleToLongBits(double)
     * @see java.util.Arrays#equals(double[], double[])
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof DD) {
            final DD c = (DD) other;
            return equals(x, c.x) && equals(xx, c.xx);
        }
        return false;
    }

    /**
     * Gets a hash code for the double-double number.
     *
     * <p>The behavior is the same as if the parts of the double-double number were passed
     * to {@link java.util.Arrays#hashCode(double[]) Arrays.hashCode(double[])}:
     *
     * <pre>
     *  {@code Arrays.hashCode(new double[] {hi() + 0.0, lo() + 0.0})}</pre>
     *
     * <p>Note: Addition of {@code 0.0} provides the same hash code for different
     * signed representations of zero values {@code -0.0} and {@code 0.0}.
     *
     * @return A hash code value for this object.
     * @see java.util.Arrays#hashCode(double[]) Arrays.hashCode(double[])
     */
    @Override
    public int hashCode() {
        return 31 * (31 + Double.hashCode(x + 0.0)) + Double.hashCode(xx + 0.0);
    }

    /**
     * Returns {@code true} if the values are numerically equal.
     *
     * <p>Two {@code double} values are considered to be
     * the same if and only if the method call
     * {@link Double#doubleToLongBits(double) Double.doubleToLongBits(value + 0.0)}
     * returns the identical {@code long} when applied to each value. This provides
     * numeric equality of different representations of zero as per {@code -0.0 == 0.0},
     * and equality of {@code NaN} values.
     *
     * @param x Value
     * @param y Value
     * @return {@code true} if the values are numerically equal
     */
    private static boolean equals(double x, double y) {
        return Double.doubleToLongBits(x + 0.0) == Double.doubleToLongBits(y + 0.0);
    }

    /**
     * Returns a string representation of the double-double number.
     *
     * <p>The string will represent the numeric values of the parts.
     * The values are split by a separator and surrounded by parentheses.
     *
     * <p>The format for a double-double number is {@code "(x,xx)"}, with {@code x} and
     * {@code xx} converted as if using {@link Double#toString(double)}.
     *
     * <p>Note: A numerical string representation of a finite double-double number can be
     * generated by conversion to a {@link BigDecimal} before formatting.
     *
     * @return A string representation of the double-double number.
     * @see Double#toString(double)
     * @see #bigDecimalValue()
     */
    @Override
    public String toString() {
        return new StringBuilder(TO_STRING_SIZE)
            .append(FORMAT_START)
            .append(x).append(FORMAT_SEP)
            .append(xx)
            .append(FORMAT_END)
            .toString();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Note: Addition of this value with any element {@code a} may not create an
     * element equal to {@code a} if the element contains sign zeros. In this case the
     * magnitude of the result will be identical.
     */
    @Override
    public DD zero() {
        return ZERO;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isZero() {
        // we keep |x| > |xx| and Java provides 0.0 == -0.0
        return x == 0.0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Note: Multiplication of this value with any element {@code a} may not create an
     * element equal to {@code a} if the element contains sign zeros. In this case the
     * magnitude of the result will be identical.
     */
    @Override
    public DD one() {
        return ONE;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isOne() {
        return x == 1.0 && xx == 0.0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This computes the same result as {@link #multiply(double) multiply((double) y)}.
     *
     * @see #multiply(double)
     */
    @Override
    public DD multiply(int n) {
        // Note: This method exists to support the NativeOperators interface
        return multiply(x, xx, n);
    }
}
