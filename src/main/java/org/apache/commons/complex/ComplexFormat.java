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

package org.apache.commons.complex;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;
import org.apache.commons.complex.Complex;

/**
 * Formats a Complex number in cartesian format "Re(c) + Im(c)i".  'i' can
 * be replaced with 'j' (or anything else), and the number format for both real
 * and imaginary parts can be configured.
 *
 */
public class ComplexFormat {

     /** The default imaginary character. */
    private static final String DEFAULT_IMAGINARY_CHARACTER = "i";
    /** The notation used to signify the imaginary part of the complex number. */
    private final String imaginaryCharacter;
    /** The format used for the imaginary part. */
    private final NumberFormat imaginaryFormat;
    /** The format used for the real part. */
    private final NumberFormat realFormat;

    /**
     * Create an instance with the default imaginary character, 'i', and the
     * default number format for both real and imaginary parts.
     */
    public ComplexFormat() {
        this.imaginaryCharacter = DEFAULT_IMAGINARY_CHARACTER;
        this.imaginaryFormat = getDefaultNumberFormat();
        this.realFormat = imaginaryFormat;
    }

    /**
     * Create an instance with a custom number format for both real and
     * imaginary parts.
     * @param format the custom format for both real and imaginary parts.
     * @throws NullArgumentException if {@code realFormat} is {@code null}.
     */
    public ComplexFormat(NumberFormat format) {
        if (format == null) {
            throw new RuntimeException("Null argument to ComplexFormat");
        }
        this.imaginaryCharacter = DEFAULT_IMAGINARY_CHARACTER;
        this.imaginaryFormat = format;
        this.realFormat = format;
    }

    /**
     * Create an instance with a custom number format for the real part and a
     * custom number format for the imaginary part.
     * @param realFormat the custom format for the real part.
     * @param imaginaryFormat the custom format for the imaginary part.
     * @throws NullArgumentException if {@code imaginaryFormat} is {@code null}.
     * @throws NullArgumentException if {@code realFormat} is {@code null}.
      */
    public ComplexFormat(NumberFormat realFormat, NumberFormat imaginaryFormat) {
        if (imaginaryFormat == null || realFormat == null) {
        	throw new RuntimeException("Null argument to ComplexFormat");
        }
        this.imaginaryCharacter = DEFAULT_IMAGINARY_CHARACTER;
        this.imaginaryFormat = imaginaryFormat;
        this.realFormat = realFormat;
    }

    /**
     * Create an instance with a custom imaginary character, and the default
     * number format for both real and imaginary parts.
     * @param imaginaryCharacter The custom imaginary character.
     * @throws NullArgumentException if {@code imaginaryCharacter} is
     * {@code null}.
     * @throws NoDataException if {@code imaginaryCharacter} is an
     * empty string.
     */
    public ComplexFormat(String imaginaryCharacter) {
        this(imaginaryCharacter, getDefaultNumberFormat());
    }

    /**
     * Create an instance with a custom imaginary character, and a custom number
     * format for both real and imaginary parts.
     * @param imaginaryCharacter The custom imaginary character.
     * @param format the custom format for both real and imaginary parts.
     * @throws NullArgumentException if {@code imaginaryCharacter} is
     * {@code null}.
     * @throws NoDataException if {@code imaginaryCharacter} is an
     * empty string.
     * @throws NullArgumentException if {@code format} is {@code null}.
     */
    public ComplexFormat(String imaginaryCharacter, NumberFormat format) {
        this(imaginaryCharacter, format, format);
    }

    /**
     * Create an instance with a custom imaginary character, a custom number
     * format for the real part, and a custom number format for the imaginary
     * part.
     *
     * @param imaginaryCharacter The custom imaginary character.
     * @param realFormat the custom format for the real part.
     * @param imaginaryFormat the custom format for the imaginary part.
     * @throws NullArgumentException if {@code imaginaryCharacter} is
     * {@code null}.
     * @throws NoDataException if {@code imaginaryCharacter} is an
     * empty string.
     * @throws NullArgumentException if {@code imaginaryFormat} is {@code null}.
     * @throws NullArgumentException if {@code realFormat} is {@code null}.
     */
    public ComplexFormat(String imaginaryCharacter,
                         NumberFormat realFormat,
                         NumberFormat imaginaryFormat) {
        if (imaginaryCharacter == null) {
            throw new RuntimeException("Null argument to ComplexFormat");
        }
        if (imaginaryCharacter.length() == 0) {
            throw new RuntimeException("Empty argument to ComplexFormat");
        }
        if (imaginaryFormat == null) {
            throw new RuntimeException("Null imaginary argument to ComplexFormat");
        }
        if (realFormat == null) {
            throw new RuntimeException("Null real argument to ComplexFormat");
        }

        this.imaginaryCharacter = imaginaryCharacter;
        this.imaginaryFormat = imaginaryFormat;
        this.realFormat = realFormat;
    }

    /**
     * Get the set of locales for which complex formats are available.
     * <p>This is the same set as the {@link NumberFormat} set.</p>
     * @return available complex format locales.
     */
    public static Locale[] getAvailableLocales() {
        return NumberFormat.getAvailableLocales();
    }

    /**
     * This method calls {@link #format(Object,StringBuffer,FieldPosition)}.
     *
     * @param c Complex object to format.
     * @return A formatted number in the form "Re(c) + Im(c)i".
     */
    public String format(Complex c) {
        return format(c, new StringBuffer(), new FieldPosition(0)).toString();
    }

    /**
     * This method calls {@link #format(Object,StringBuffer,FieldPosition)}.
     *
     * @param c Double object to format.
     * @return A formatted number.
     */
    public String format(Double c) {
        return format(new Complex(c, 0), new StringBuffer(), new FieldPosition(0)).toString();
    }

    /**
     * Formats a {@link Complex} object to produce a string.
     *
     * @param complex the object to format.
     * @param toAppendTo where the text is to be appended
     * @param pos On input: an alignment field, if desired. On output: the
     *            offsets of the alignment field
     * @return the value passed in as toAppendTo.
     */
    public StringBuffer format(Complex complex, StringBuffer toAppendTo,
                               FieldPosition pos) {
        pos.setBeginIndex(0);
        pos.setEndIndex(0);

        // format real
        double re = complex.getReal();
        formatDouble(re, getRealFormat(), toAppendTo, pos);

        // format sign and imaginary
        double im = complex.getImaginary();
        StringBuffer imAppendTo;
        if (im < 0.0) {
            toAppendTo.append(" - ");
            imAppendTo = formatImaginary(-im, new StringBuffer(), pos);
            toAppendTo.append(imAppendTo);
            toAppendTo.append(getImaginaryCharacter());
        } else if (im > 0.0 || Double.isNaN(im)) {
            toAppendTo.append(" + ");
            imAppendTo = formatImaginary(im, new StringBuffer(), pos);
            toAppendTo.append(imAppendTo);
            toAppendTo.append(getImaginaryCharacter());
        }

        return toAppendTo;
    }

    /**
     * Format the absolute value of the imaginary part.
     *
     * @param absIm Absolute value of the imaginary part of a complex number.
     * @param toAppendTo where the text is to be appended.
     * @param pos On input: an alignment field, if desired. On output: the
     * offsets of the alignment field.
     * @return the value passed in as toAppendTo.
     */
    private StringBuffer formatImaginary(double absIm,
                                         StringBuffer toAppendTo,
                                         FieldPosition pos) {
        pos.setBeginIndex(0);
        pos.setEndIndex(0);

        formatDouble(absIm, getImaginaryFormat(), toAppendTo, pos);
        if (toAppendTo.toString().equals("1")) {
            // Remove the character "1" if it is the only one.
            toAppendTo.setLength(0);
        }

        return toAppendTo;
    }

    /**
     * Formats a object to produce a string.  {@code obj} must be either a
     * {@link Complex} object or a {@link Number} object.  Any other type of
     * object will result in an {@link IllegalArgumentException} being thrown.
     *
     * @param obj the object to format.
     * @param toAppendTo where the text is to be appended
     * @param pos On input: an alignment field, if desired. On output: the
     *            offsets of the alignment field
     * @return the value passed in as toAppendTo.
     * @see java.text.Format#format(java.lang.Object, java.lang.StringBuffer, java.text.FieldPosition)
     * @throws MathIllegalArgumentException is {@code obj} is not a valid type.
     */
    public StringBuffer format(Object obj, StringBuffer toAppendTo,
                               FieldPosition pos) {

        StringBuffer ret = null;

        if (obj instanceof Complex) {
            ret = format( (Complex)obj, toAppendTo, pos);
        } else if (obj instanceof Number) {
            ret = format(new Complex(((Number)obj).doubleValue(), 0.0),
                         toAppendTo, pos);
        } else {
            throw new RuntimeException("Cannot format instance as complex");
        }

        return ret;
    }

    /**
     * Access the imaginaryCharacter.
     * @return the imaginaryCharacter.
     */
    public String getImaginaryCharacter() {
        return imaginaryCharacter;
    }

    /**
     * Access the imaginaryFormat.
     * @return the imaginaryFormat.
     */
    public NumberFormat getImaginaryFormat() {
        return imaginaryFormat;
    }

    /**
     * Returns the default complex format for the current locale.
     * @return the default complex format.
     */
    public static ComplexFormat getInstance() {
        return getInstance(Locale.getDefault());
    }

    /**
     * Returns the default complex format for the given locale.
     * @param locale the specific locale used by the format.
     * @return the complex format specific to the given locale.
     */
    public static ComplexFormat getInstance(Locale locale) {
        NumberFormat f = getDefaultNumberFormat(locale);
        return new ComplexFormat(f);
    }

    /**
     * Returns the default complex format for the given locale.
     * @param locale the specific locale used by the format.
     * @param imaginaryCharacter Imaginary character.
     * @return the complex format specific to the given locale.
     * @throws NullArgumentException if {@code imaginaryCharacter} is
     * {@code null}.
     * @throws NoDataException if {@code imaginaryCharacter} is an
     * empty string.
     */
    public static ComplexFormat getInstance(String imaginaryCharacter, Locale locale) {
        NumberFormat f = getDefaultNumberFormat(locale);
        return new ComplexFormat(imaginaryCharacter, f);
    }

    /**
     * Access the realFormat.
     * @return the realFormat.
     */
    public NumberFormat getRealFormat() {
        return realFormat;
    }

    /**
     * Parses a string to produce a {@link Complex} object.
     *
     * @param source the string to parse.
     * @return the parsed {@link Complex} object.
     * @throws MathParseException if the beginning of the specified string
     * cannot be parsed.
     */
    public Complex parse(String source) {
        ParsePosition parsePosition = new ParsePosition(0);
        Complex result = parse(source, parsePosition);
        if (parsePosition.getIndex() == 0) {
            throw new RuntimeException("ComplexFormat parse error at index "+ parsePosition.getErrorIndex());
        }
        return result;
    }

    /**
     * Parses a string to produce a {@link Complex} object.
     *
     * @param source the string to parse
     * @param pos input/ouput parsing parameter.
     * @return the parsed {@link Complex} object.
     */
    public Complex parse(String source, ParsePosition pos) {
        int initialIndex = pos.getIndex();

        // parse whitespace
        parseAndIgnoreWhitespace(source, pos);

        // parse real
        Number re = parseNumber(source, getRealFormat(), pos);
        if (re == null) {
            // invalid real number
            // set index back to initial, error index should already be set
            pos.setIndex(initialIndex);
            return null;
        }

        // parse sign
        int startIndex = pos.getIndex();
        char c = parseNextCharacter(source, pos);
        int sign = 0;
        switch (c) {
        case 0 :
            // no sign
            // return real only complex number
            return new Complex(re.doubleValue(), 0.0);
        case '-' :
            sign = -1;
            break;
        case '+' :
            sign = 1;
            break;
        default :
            // invalid sign
            // set index back to initial, error index should be the last
            // character examined.
            pos.setIndex(initialIndex);
            pos.setErrorIndex(startIndex);
            return null;
        }

        // parse whitespace
        parseAndIgnoreWhitespace(source, pos);

        // parse imaginary
        Number im = parseNumber(source, getRealFormat(), pos);
        if (im == null) {
            // invalid imaginary number
            // set index back to initial, error index should already be set
            pos.setIndex(initialIndex);
            return null;
        }

        // parse imaginary character
        if (!parseFixedstring(source, getImaginaryCharacter(), pos)) {
            return null;
        }

        return new Complex(re.doubleValue(), im.doubleValue() * sign);

    }


    /**
     * Create a default number format.  The default number format is based on
     * {@link NumberFormat#getInstance()} with the only customizing that the
     * maximum number of fraction digits is set to 10.
     * @return the default number format.
     */
    public static NumberFormat getDefaultNumberFormat() {
        return getDefaultNumberFormat(Locale.getDefault());
    }

    /**
     * Create a default number format.  The default number format is based on
     * {@link NumberFormat#getInstance(java.util.Locale)} with the only
     * customizing that the maximum number of fraction digits is set to 10.
     * @param locale the specific locale used by the format.
     * @return the default number format specific to the given locale.
     */
    public static NumberFormat getDefaultNumberFormat(final Locale locale) {
        final NumberFormat nf = NumberFormat.getInstance(locale);
        nf.setMaximumFractionDigits(10);
        return nf;
    }

    /**
     * Parses <code>source</code> until a non-whitespace character is found.
     *
     * @param source the string to parse
     * @param pos input/output parsing parameter.  On output, <code>pos</code>
     *        holds the index of the next non-whitespace character.
     */
    public static void parseAndIgnoreWhitespace(final String source,
                                                final ParsePosition pos) {
        parseNextCharacter(source, pos);
        pos.setIndex(pos.getIndex() - 1);
    }

    /**
     * Parses <code>source</code> until a non-whitespace character is found.
     *
     * @param source the string to parse
     * @param pos input/output parsing parameter.
     * @return the first non-whitespace character.
     */
    public static char parseNextCharacter(final String source,
                                          final ParsePosition pos) {
         int index = pos.getIndex();
         final int n = source.length();
         char ret = 0;

         if (index < n) {
             char c;
             do {
                 c = source.charAt(index++);
             } while (Character.isWhitespace(c) && index < n);
             pos.setIndex(index);

             if (index < n) {
                 ret = c;
             }
         }

         return ret;
    }

    /**
     * Parses <code>source</code> for special double values.  These values
     * include Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY.
     *
     * @param source the string to parse
     * @param value the special value to parse.
     * @param pos input/output parsing parameter.
     * @return the special number.
     */
    private static Number parseNumber(final String source, final double value,
                                      final ParsePosition pos) {
        Number ret = null;

        StringBuilder sb = new StringBuilder();
        sb.append('(');
        sb.append(value);
        sb.append(')');

        final int n = sb.length();
        final int startIndex = pos.getIndex();
        final int endIndex = startIndex + n;
        if (endIndex < source.length() &&
            source.substring(startIndex, endIndex).compareTo(sb.toString()) == 0) {
            ret = Double.valueOf(value);
            pos.setIndex(endIndex);
        }

        return ret;
    }

    /**
     * Parses <code>source</code> for a number.  This method can parse normal,
     * numeric values as well as special values.  These special values include
     * Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY.
     *
     * @param source the string to parse
     * @param format the number format used to parse normal, numeric values.
     * @param pos input/output parsing parameter.
     * @return the parsed number.
     */
    public static Number parseNumber(final String source, final NumberFormat format,
                                     final ParsePosition pos) {
        final int startIndex = pos.getIndex();
        Number number = format.parse(source, pos);
        final int endIndex = pos.getIndex();

        // check for error parsing number
        if (startIndex == endIndex) {
            // try parsing special numbers
            final double[] special = {
                Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
            };
            for (int i = 0; i < special.length; ++i) {
                number = parseNumber(source, special[i], pos);
                if (number != null) {
                    break;
                }
            }
        }

        return number;
    }

    /**
     * Parse <code>source</code> for an expected fixed string.
     * @param source the string to parse
     * @param expected expected string
     * @param pos input/output parsing parameter.
     * @return true if the expected string was there
     */
    public static boolean parseFixedstring(final String source,
                                           final String expected,
                                           final ParsePosition pos) {

        final int startIndex = pos.getIndex();
        final int endIndex = startIndex + expected.length();
        if ((startIndex >= source.length()) ||
            (endIndex > source.length()) ||
            (source.substring(startIndex, endIndex).compareTo(expected) != 0)) {
            // set index back to start, error index should be the start index
            pos.setIndex(startIndex);
            pos.setErrorIndex(startIndex);
            return false;
        }

        // the string was here
        pos.setIndex(endIndex);
        return true;
    }

    /**
     * Formats a double value to produce a string.  In general, the value is
     * formatted using the formatting rules of <code>format</code>.  There are
     * three exceptions to this:
     * <ol>
     * <li>NaN is formatted as '(NaN)'</li>
     * <li>Positive infinity is formatted as '(Infinity)'</li>
     * <li>Negative infinity is formatted as '(-Infinity)'</li>
     * </ol>
     *
     * @param value the double to format.
     * @param format the format used.
     * @param toAppendTo where the text is to be appended
     * @param pos On input: an alignment field, if desired. On output: the
     *            offsets of the alignment field
     * @return the value passed in as toAppendTo.
     */
    public static StringBuffer formatDouble(final double value, final NumberFormat format,
            final StringBuffer toAppendTo,
            final FieldPosition pos) {
		if( Double.isNaN(value) || Double.isInfinite(value) ) {
		toAppendTo.append('(');
		toAppendTo.append(value);
		toAppendTo.append(')');
		} else {
		format.format(value, toAppendTo, pos);
		}
		return toAppendTo;
	}


}
