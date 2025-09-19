/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating virtual terminal creation for non-system terminals.
 */
public class VirtualTerminalCreation {

    public static void main(String[] args) throws IOException {
        // SNIPPET_START: VirtualTerminalCreation
        // Create input and output streams for the virtual terminal
        InputStream input = new ByteArrayInputStream("hello\n".getBytes());
        OutputStream output = new ByteArrayOutputStream();

        // Basic virtual terminal creation
        Terminal terminal =
                TerminalBuilder.builder().system(false).streams(input, output).build();

        // Virtual terminal with initial size and attributes
        int columns = 80;
        int rows = 24;
        Attributes attributes = new Attributes();
        // Set attributes as needed...

        Terminal terminal2 = TerminalBuilder.builder()
                .system(false)
                .streams(input, output)
                .size(new Size(columns, rows))
                .attributes(attributes)
                .build();
        // SNIPPET_END: VirtualTerminalCreation

        System.out.println("Virtual terminal created: " + terminal.getClass().getSimpleName());
        System.out.println("Virtual terminal 2 created: " + terminal2.getClass().getSimpleName());

        terminal.close();
        terminal2.close();
    }
}
