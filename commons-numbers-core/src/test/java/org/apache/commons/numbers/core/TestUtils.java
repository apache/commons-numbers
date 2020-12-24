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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Test utilities.
 */
public final class TestUtils {
    /**
     * Collection of static methods used in math unit tests.
     */
    private TestUtils() {
    }

    /**
     * Serializes an object to a bytes array and then recovers the object from the bytes array.
     * Returns the deserialized object.
     *
     * @param o  object to serialize and recover
     * @return  the recovered, deserialized object
     */
    public static Object serializeAndRecover(final Object o) {
        try {
            // serialize the Object
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final ObjectOutputStream so = new ObjectOutputStream(bos);
            so.writeObject(o);

            // deserialize the Object
            final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            final ObjectInputStream si = new ObjectInputStream(bis);
            return si.readObject();
        } catch (final IOException ioe) {
            return null;
        } catch (final ClassNotFoundException cnfe) {
            return null;
        }
    }
}


