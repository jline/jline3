/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.io.IOException;

import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.OutputFlag;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating output flags in JLine.
 */
public class OutputFlagsExample {

    // SNIPPET_START: OutputFlagsExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        try {
            // Get current attributes
            Attributes attributes = terminal.getAttributes();

            // Display current output flags
            terminal.writer().println("Current output flags:");

            // Common output flags
            for (OutputFlag flag : new OutputFlag[] {
                OutputFlag.OPOST, // Post-process output
                OutputFlag.ONLCR, // Map NL to CR-NL
                OutputFlag.OCRNL, // Map CR to NL
                OutputFlag.ONOCR, // Don't output CR at column 0
                OutputFlag.ONLRET, // NL performs CR function
                OutputFlag.OFILL, // Use fill characters for delay
                OutputFlag.OFDEL, // Fill is DEL
                OutputFlag.NLDLY, // NL delay
                OutputFlag.CRDLY, // CR delay
                OutputFlag.TABDLY, // Tab delay
                OutputFlag.BSDLY, // Backspace delay
                OutputFlag.VTDLY, // Vertical tab delay
                OutputFlag.FFDLY, // Form feed delay
            }) {
                try {
                    boolean value = attributes.getOutputFlag(flag);
                    terminal.writer().println("  " + flag + ": " + value);
                } catch (Exception e) {
                    terminal.writer().println("  " + flag + ": unsupported");
                }
            }

            terminal.writer().flush();
        } finally {
            terminal.close();
        }
    }
    // SNIPPET_END: OutputFlagsExample
}
