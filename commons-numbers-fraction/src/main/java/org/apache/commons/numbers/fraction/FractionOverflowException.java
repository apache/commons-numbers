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

/**
 * Error thrown when evaluating a fraction causes an overflow.
 */
public class FractionOverflowException extends FractionException {

    /** Serializable version identifier. */
    private static final long serialVersionUID = 201701181869L;

    /**
     * Constructs an exception the default formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param p current numerator
     * @param q current denominator
     */
    public FractionOverflowException(long p, long q) {
        super("overflow in fraction {0}/{1}, cannot negate", p, q);
    }

    /**
     * Constructs an exception with specified formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param message  the custom message
     * @param formatArguments  arguments when formatting the message
     */
    public FractionOverflowException(String message, Object... formatArguments) {
        super(message, formatArguments);
    }
}
