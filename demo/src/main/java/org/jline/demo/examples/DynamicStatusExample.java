/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.io.IOError;
import java.util.Collections;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Status;

/**
 * Example demonstrating dynamic status updates in JLine.
 */
public class DynamicStatusExample {

    // SNIPPET_START: DynamicStatusExample
    public static void main(String[] args) throws Exception {
        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

        // Create a Status instance
        Status status = Status.getStatus(terminal);

        // Start a daemon thread to update the status
        Thread statusThread = new Thread(() -> {
            try {
                int taskCount = 0;
                while (!Thread.interrupted()) {
                    Thread.sleep(2000);
                    taskCount = (taskCount + 1) % 10;

                    if (status != null) {
                        status.update(Collections.singletonList(new AttributedStringBuilder()
                                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
                                .append("Connected to server | ")
                                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                                .append(Integer.toString(taskCount))
                                .append(" tasks running")
                                .toAttributedString()));
                    }
                }
            } catch (InterruptedException e) {
                // thread stopping
            }
        });
        statusThread.setDaemon(true);
        statusThread.start();

        // Read input normally
        while (true) {
            try {
                String line = reader.readLine("prompt> ");
                System.out.println("You entered: " + line);

                if (line.equals("exit")) {
                    break;
                }
            } catch (UserInterruptException e) {
                // Ctrl+C pressed, continue to next prompt
            } catch (EndOfFileException e) {
                // Ctrl+D pressed, exit
                break;
            } catch (IOError e) {
                // Terminal I/O error (e.g., Ctrl+C on macOS can cause
                // the PTY read to fail with EIO at the native level)
                break;
            }
        }
        statusThread.interrupt();
        terminal.close();
    }
    // SNIPPET_END: DynamicStatusExample
}
