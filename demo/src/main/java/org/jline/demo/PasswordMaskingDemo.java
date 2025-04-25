/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo;

import java.io.IOException;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.spi.TerminalExt;

/**
 * Demo program to test password masking in both regular and dumb terminals.
 *
 * In dumb terminals, JLine3 uses a thread-based approach to continuously refresh
 * the prompt, effectively hiding the user's input. This is similar to the approach
 * used in JLine1.
 */
public class PasswordMaskingDemo {

    public static void main(String[] args) {
        Character mask = '*';

        // Parse command line arguments
        boolean skipDash = false;
        for (String arg : args) {
            // Skip the -- separator
            if (arg.equals("--")) {
                skipDash = true;
                continue;
            }

            if (skipDash && arg.startsWith("--mask=")) {
                String maskStr = arg.substring("--mask=".length());
                if (maskStr.isEmpty()) {
                    mask = null;
                } else {
                    mask = maskStr.charAt(0);
                }
            }
        }

        try {
            // Build the terminal
            TerminalBuilder builder = TerminalBuilder.builder();
            Terminal terminal = builder.build();

            boolean isDumb = ((TerminalExt) terminal).getProvider().name().equals(TerminalBuilder.PROP_PROVIDER_DUMB);
            if (isDumb) {
                System.out.println("Using dumb terminal");
                System.out.println("With dumb terminals, JLine3 uses a thread to continuously refresh the prompt,");
                System.out.println("effectively hiding your input. You won't see mask characters, but your input");
                System.out.println("will be hidden. This is similar to how JLine1 handled password input.");
            } else {
                System.out.println("Using regular terminal");
                if (mask != null) {
                    System.out.println("You should see each character replaced with: " + mask);
                } else {
                    System.out.println("No masking will be applied - you'll see what you type");
                }
            }

            // Build the line reader
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

            // Read the password
            String password;
            if (mask != null) {
                System.out.println("\nReading password with mask: " + mask);
                password = reader.readLine("Enter password: ", mask);
            } else {
                System.out.println("\nReading password without masking");
                password = reader.readLine("Enter password: ");
            }

            System.out.println("Password entered: " + password);

            // Read another password to verify behavior
            if (mask != null) {
                System.out.println("\nTry entering another password to verify the behavior:");
                password = reader.readLine("Enter another password: ", mask);
            } else {
                System.out.println("\nTry entering another password to verify the behavior:");
                password = reader.readLine("Enter another password: ");
            }

            System.out.println("Second password entered: " + password);

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
