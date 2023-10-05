/*
 * Copyright (c) 2009-2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.jansi.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.jline.jansi.AnsiColors;
import org.jline.jansi.AnsiMode;
import org.jline.jansi.AnsiType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnsiOutputStreamTest {

    @Test
    void canHandleSgrsWithMultipleOptions() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final AnsiOutputStream ansiOutput = new AnsiOutputStream(
                baos,
                null,
                AnsiMode.Strip,
                null,
                AnsiType.Emulation,
                AnsiColors.TrueColor,
                StandardCharsets.UTF_8,
                null,
                null,
                false);
        ansiOutput.write(
                ("\u001B[33mbanana_1  |\u001B[0m 19:59:14.353\u001B[0;38m [debug] A message\u001B[0m\n").getBytes());
        assertEquals("banana_1  | 19:59:14.353 [debug] A message\n", baos.toString());
    }
}
