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
package org.apache.commons.numbers.fraction;

import java.text.MessageFormat;

/**
 * Package private exception class with constants for frequently used messages.
 */
class FractionException extends ArithmeticException {

    /** Serializable version identifier. */
    private static final long serialVersionUID = 201701191744L;

    public static final String ERROR_CONVERSION_OVERFLOW = "Overflow trying to convert {0} to fraction ({1}/{2})";
    public static final String ERROR_CONVERSION = "Unable to convert {0} to fraction after {1} iterations";
    public static final String ERROR_NEGATION_OVERFLOW = "overflow in fraction {0}/{1}, cannot negate";
    public static final String ERROR_ZERO_DENOMINATOR = "denominator must be different from 0";

    protected Object[] formatArguments;

    public FractionException() {
    }

    /**
     * Create an exception where the message is constructed by applying
     * the {@code format()} method from {@code java.text.MessageFormat}.
     *
     * @param message  the exception message with replaceable parameters
     * @param formatArguments the arguments for formatting the message
     */
    public FractionException(String message, Object... formatArguments) {
        super(message);
        this.formatArguments = formatArguments;
    }

    @Override
    public String getMessage() {
        return MessageFormat.format(super.getMessage(), formatArguments);
    }


}
