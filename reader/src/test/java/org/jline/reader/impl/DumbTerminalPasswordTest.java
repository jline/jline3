/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DumbTerminalPasswordTest {

    @Test
    public void testPasswordMaskingWithDumbTerminal() throws IOException {
        // Setup a dumb terminal with input and output streams
        ByteArrayInputStream in = new ByteArrayInputStream("password\n".getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (Terminal terminal = new DumbTerminal(in, out)) {
            LineReader reader = new LineReaderImpl(terminal);

            // Read a line with a password mask
            String password = reader.readLine("Password: ", '*');

            // Verify the password was read correctly
            assertEquals("password", password);

            // Verify the output contains the prompt
            String output = out.toString();
            assertTrue(output.contains("Password: "));

            // The output should contain multiple instances of the prompt due to the masking thread
            int promptCount = countOccurrences(output, "Password: ");
            assertTrue(promptCount > 1, "Expected multiple instances of the prompt due to masking thread");
        }
    }

    private int countOccurrences(String str, String substr) {
        int count = 0;
        int index = 0;
        while ((index = str.indexOf(substr, index)) != -1) {
            count++;
            index += substr.length();
        }
        return count;
    }
}
