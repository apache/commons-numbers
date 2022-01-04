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
package org.apache.commons.numbers.gamma;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;

/**
 * Class to read data fields from a test resource file.
 */
class DataReader implements AutoCloseable {
    /** Comment character. */
    private static final char COMMENT = '#';
    /** Pattern to split data fields. */
    private static final Pattern FIELD_PATTERN = Pattern.compile("[, ]+");

    /** Input to read. */
    private final BufferedReader in;

    /** The current set of fields. */
    private String[] tokens;

    /**
     * @param filename Resource filename to read
     */
    DataReader(String filename) {
        final InputStream resourceAsStream = this.getClass().getResourceAsStream(filename);
        Assertions.assertNotNull(resourceAsStream, () -> "Could not find resource " + filename);
        in = new BufferedReader(new InputStreamReader(resourceAsStream));
    }

    /**
     * Read the next line of data fields.
     *
     * @return true if data is available
     * @throws IOException Signals that an I/O exception has occurred.
     */
    boolean next() throws IOException {
        tokens = null;
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            if (line.isEmpty() || line.charAt(0) == COMMENT || line.trim().isEmpty()) {
                continue;
            }
            tokens = FIELD_PATTERN.split(line);
            return true;
        }
        return false;
    }

    /**
     * Gets the double from the field.
     *
     * @param i Field index
     * @return the number
     * @see #next()
     * @throws NumberFormatException if the field cannot be parsed as a double
     * @throws NullPointerException if no field data is available
     * @throws IndexOutOfBoundsException if the field index is invalid
     */
    double getDouble(int i) {
        return Double.parseDouble(tokens[i]);
    }

    /**
     * Gets the BigDecimal from the field.
     *
     * @param i Field index
     * @return the number
     * @see #next()
     * @throws NumberFormatException if the field cannot be parsed as a BigDecimal
     * @throws NullPointerException if no field data is available
     * @throws IndexOutOfBoundsException if the field index is invalid
     */
    BigDecimal getBigDecimal(int i) {
        return new BigDecimal(tokens[i]);
    }

    /**
     * Gets the current fields. This is returned as a reference.
     *
     * <p>This is null if no fields are available for reading.
     *
     * @return the fields
     * @see #next()
     */
    String[] getFields() {
        return tokens;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
