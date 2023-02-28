/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jline.nativ;

import org.junit.jupiter.api.Test;

import java.io.FileDescriptor;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JLineLibraryTest {

    @Test
    void testNewFileDescriptor() throws Exception {
        FileDescriptor fd = JLineLibrary.newFileDescriptor(12);
        assertNotNull(fd);
        Field field = FileDescriptor.class.getDeclaredField("fd");
        field.setAccessible(true);
        assertEquals(12, field.get(fd), fd.toString());
    }

}
