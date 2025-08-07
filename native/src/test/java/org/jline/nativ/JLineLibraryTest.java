/*
 * Copyright (c) 2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.nativ;

import java.io.FileDescriptor;
import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JLineLibraryTest {

    /**
     * Default constructor.
     */
    public JLineLibraryTest() {
        // Default constructor
    }

    @Test
    void testNewFileDescriptor() throws Exception {
        FileDescriptor fd = JLineLibrary.newFileDescriptor(12);
        assertNotNull(fd);
        Field field = FileDescriptor.class.getDeclaredField("fd");
        field.setAccessible(true);
        assertEquals(12, field.get(fd), fd.toString());
    }

    @Test
    void testNewRedirectPipeImpl() throws Exception {
        ProcessBuilder.Redirect redirect = JLineLibrary.newRedirectPipe(FileDescriptor.out);
        assertNotNull(redirect);
        // This requires '--add-opens java.base/java.lang=org.jline.nativ', but adding this option
        // defeats the very purpose of the method
        //
        // Method mth = redirect.getClass().getDeclaredMethod("getFd");
        // mth.setAccessible(true);
        // assertEquals(System.out, mth.invoke(redirect), redirect.toString());
    }
}
