/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import javax.swing.*;

import org.jline.builtins.SwingTerminal;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;

/**
 * Example demonstrating SwingTerminal usage.
 *
 * <p>SwingTerminal is a full JLine Terminal implementation rendered in a Swing window.
 * It can be used to embed a terminal in any Swing or desktop Java application.
 * Since it implements the standard Terminal interface, all JLine features work
 * out of the box: line editing, history, completion, syntax highlighting, etc.</p>
 */
public class SwingTerminalExample {

    // SNIPPET_START: SwingTerminalBasicExample
    /**
     * Basic example: create a SwingTerminal and run a simple REPL inside it.
     */
    public static void basicExample() throws Exception {
        // Create a SwingTerminal (it is a full JLine Terminal)
        SwingTerminal terminal = new SwingTerminal("demo", 80, 24);

        // Create a JFrame to display the terminal
        JFrame frame = terminal.createFrame("JLine Swing Terminal Demo");

        // Add a window listener to clean up on close
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                terminal.dispose();
            }
        });

        // Run the REPL in a separate thread so the Swing UI stays responsive
        Thread replThread = new Thread(
                () -> {
                    try {
                        runRepl(terminal);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        terminal.dispose();
                        frame.dispose();
                    }
                },
                "SwingTerminal-REPL");
        replThread.setDaemon(true);
        replThread.start();
    }
    // SNIPPET_END: SwingTerminalBasicExample

    // SNIPPET_START: SwingTerminalEmbeddedExample
    /**
     * Embedded example: embed a SwingTerminal inside an existing Swing application.
     */
    public static void embeddedExample() throws Exception {
        // Create the terminal
        SwingTerminal terminal = new SwingTerminal("embedded", 80, 24);

        // Build a custom UI around the terminal component
        JFrame frame = new JFrame("Application with Embedded Terminal");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new java.awt.BorderLayout());

        // Add a label at the top
        JLabel header = new JLabel("My Application - Terminal Panel", SwingConstants.CENTER);
        header.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        frame.add(header, java.awt.BorderLayout.NORTH);

        // Add the terminal component in the center
        frame.add(terminal.getComponent(), java.awt.BorderLayout.CENTER);

        frame.setSize(700, 500);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        terminal.getComponent().requestFocusInWindow();

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                terminal.dispose();
            }
        });

        // Run the REPL
        Thread replThread = new Thread(
                () -> {
                    try {
                        runRepl(terminal);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        terminal.dispose();
                        frame.dispose();
                    }
                },
                "SwingTerminal-Embedded-REPL");
        replThread.setDaemon(true);
        replThread.start();
    }
    // SNIPPET_END: SwingTerminalEmbeddedExample

    // SNIPPET_START: SwingTerminalReplExample
    /**
     * Runs a simple REPL loop using the given terminal.
     * Demonstrates that SwingTerminal works with LineReader for
     * line editing, tab completion, and history.
     */
    private static void runRepl(Terminal terminal) {
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(new StringsCompleter("help", "info", "clear", "exit"))
                .build();

        terminal.writer().println("Welcome to the JLine Swing Terminal!");
        terminal.writer().println("Type 'help' for available commands, 'exit' to quit.");
        terminal.writer().flush();

        while (true) {
            try {
                String line = reader.readLine("swing> ");

                if (line.trim().isEmpty()) {
                    continue;
                }

                switch (line.trim()) {
                    case "exit":
                        terminal.writer().println("Goodbye!");
                        terminal.writer().flush();
                        return;
                    case "help":
                        terminal.writer().println("Available commands:");
                        terminal.writer().println("  help  - Show this help");
                        terminal.writer().println("  info  - Show terminal information");
                        terminal.writer().println("  clear - Clear the screen");
                        terminal.writer().println("  exit  - Exit the demo");
                        break;
                    case "info":
                        terminal.writer().println("Terminal: " + terminal.getName());
                        terminal.writer().println("Type:     " + terminal.getType());
                        terminal.writer().println("Size:     " + terminal.getWidth() + "x" + terminal.getHeight());
                        terminal.writer().println("Encoding: " + terminal.encoding());
                        break;
                    case "clear":
                        terminal.writer().print("\033[2J\033[H");
                        break;
                    default:
                        terminal.writer().println("Echo: " + line);
                        break;
                }
                terminal.writer().flush();

            } catch (UserInterruptException e) {
                // Ctrl+C: ignore
            } catch (EndOfFileException e) {
                // Ctrl+D: exit
                return;
            }
        }
    }
    // SNIPPET_END: SwingTerminalReplExample

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && "embedded".equals(args[0])) {
            embeddedExample();
        } else {
            basicExample();
        }
    }
}
