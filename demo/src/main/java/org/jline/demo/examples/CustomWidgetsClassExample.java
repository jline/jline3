/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.widget.Widgets;

import static org.jline.keymap.KeyMap.alt;
import static org.jline.keymap.KeyMap.ctrl;

/**
 * Example demonstrating how to create a custom widgets class in JLine.
 */
public class CustomWidgetsClassExample {

    // SNIPPET_START: CustomWidgetsClassExample
    /**
     * Custom widgets class that extends the base Widgets class.
     */
    public static class MyWidgets extends Widgets {

        /**
         * Factory method to create and set up a MyWidgets instance.
         *
         * @param reader The LineReader to associate with these widgets
         * @return The configured MyWidgets instance
         */
        public static MyWidgets create(LineReader reader) {
            MyWidgets widgets = new MyWidgets(reader);

            // Add a widget that executes the first word as a command
            widgets.addWidget("execute-command", widgets::executeCommand);

            // Add a widget that clears the line
            widgets.addWidget("clear-line", widgets::clearLine);

            // Bind keys to the widgets
            // Ctrl+Alt+X for execute-command
            widgets.getKeyMap().bind(new Reference("execute-command"), alt(ctrl('X')));

            // Ctrl+Alt+C for clear-line
            widgets.getKeyMap().bind(new Reference("clear-line"), alt(ctrl('C')));

            return widgets;
        }

        /**
         * Private constructor to force use of factory method.
         */
        private MyWidgets(LineReader reader) {
            super(reader);
        }

        /**
         * Widget that takes the first word of the buffer and executes it as a command.
         * In this example, it just calls the corresponding widget if it exists.
         */
        public boolean executeCommand() {
            try {
                // Get the first word from the buffer
                String buffer = buffer().toString();
                String[] words = buffer.split("\\s+");

                if (words.length > 0 && !words[0].isEmpty()) {
                    String command = words[0];
                    System.out.println("\nExecuting command: " + command);

                    // Try to call a widget with this name
                    try {
                        reader.callWidget(command);
                        return true;
                    } catch (Exception e) {
                        System.out.println("No widget named: " + command);
                        return false;
                    }
                }
            } catch (Exception e) {
                return false;
            }
            return true;
        }

        /**
         * Widget that clears the current line.
         */
        public boolean clearLine() {
            buffer().clear();
            return true;
        }
    }

    public static void main(String[] args) {
        try {
            // Create a terminal
            Terminal terminal = TerminalBuilder.builder().system(true).build();

            // Create a line reader
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

            // Create and register our custom widgets
            MyWidgets widgets = MyWidgets.create(reader);

            // Display instructions
            terminal.writer().println("Custom Widgets Class Example");
            terminal.writer().println("  Ctrl+Alt+X: Execute the first word as a command");
            terminal.writer().println("  Ctrl+Alt+C: Clear the line");
            terminal.writer().println();
            terminal.writer().println("Try typing 'clear-line' and then press Ctrl+Alt+X");
            terminal.writer().flush();

            // REPL loop
            String prompt = "widgets> ";
            while (true) {
                try {
                    String line = reader.readLine(prompt);

                    if ("exit".equalsIgnoreCase(line)) {
                        break;
                    }

                    terminal.writer().println("You entered: " + line);
                    terminal.writer().flush();
                } catch (UserInterruptException e) {
                    // Ctrl+C pressed
                    terminal.writer().println("KeyboardInterrupt (Ctrl+C)");
                    terminal.writer().flush();
                } catch (EndOfFileException e) {
                    // Ctrl+D pressed
                    terminal.writer().println("End of file (Ctrl+D)");
                    terminal.writer().flush();
                    break;
                }
            }

            terminal.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // SNIPPET_END: CustomWidgetsClassExample
}
