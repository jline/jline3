/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import org.jline.builtins.WebTerminal;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;

/**
 * Example demonstrating WebTerminal usage.
 *
 * <p>WebTerminal is a full JLine Terminal implementation that serves a web-based
 * terminal interface over HTTP. It uses JDK's built-in HttpServer, so no external
 * dependencies are required. Users interact with the terminal through a web browser.</p>
 *
 * <p>Since WebTerminal implements the standard Terminal interface, all JLine features
 * work transparently: line editing, history, tab completion, syntax highlighting, etc.</p>
 */
public class WebTerminalExample {

    // SNIPPET_START: WebTerminalBasicExample
    /**
     * Basic example: start a WebTerminal and run a REPL accessible from a browser.
     */
    public static void basicExample() throws Exception {
        // Create a WebTerminal bound to localhost on port 8080, with 80x24 screen
        WebTerminal terminal = new WebTerminal("localhost", 8080, 80, 24);

        // Start the HTTP server
        terminal.start();
        System.out.println("WebTerminal started at " + terminal.getUrl());
        System.out.println("Open the URL in your browser to interact with the terminal.");
        System.out.println("Press Ctrl+C in this console to stop the server.");

        // Run the REPL - input comes from the browser, output goes to the browser
        try {
            runRepl(terminal);
        } finally {
            terminal.stop();
            terminal.close();
        }
    }
    // SNIPPET_END: WebTerminalBasicExample

    // SNIPPET_START: WebTerminalCustomPortExample
    /**
     * Example with a custom port and larger terminal size.
     */
    public static void customPortExample(int port) throws Exception {
        // Create a WebTerminal with custom settings
        WebTerminal terminal = new WebTerminal("0.0.0.0", port, 120, 40);

        terminal.start();
        System.out.println("WebTerminal started at " + terminal.getUrl());

        try {
            runRepl(terminal);
        } finally {
            terminal.stop();
            terminal.close();
        }
    }
    // SNIPPET_END: WebTerminalCustomPortExample

    // SNIPPET_START: WebTerminalReplExample
    /**
     * Runs a simple REPL loop using the given terminal.
     * Demonstrates that WebTerminal works with LineReader for
     * line editing, tab completion, and history.
     */
    private static void runRepl(Terminal terminal) {
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(new StringsCompleter("help", "info", "clear", "colors", "exit"))
                .build();

        terminal.writer().println("Welcome to the JLine Web Terminal!");
        terminal.writer().println("Type 'help' for available commands, 'exit' to quit.");
        terminal.writer().flush();

        while (true) {
            try {
                String line = reader.readLine("web> ");

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
                        terminal.writer().println("  help   - Show this help");
                        terminal.writer().println("  info   - Show terminal information");
                        terminal.writer().println("  clear  - Clear the screen");
                        terminal.writer().println("  colors - Show ANSI color demo");
                        terminal.writer().println("  exit   - Exit the demo");
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
                    case "colors":
                        showColors(terminal);
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
    // SNIPPET_END: WebTerminalReplExample

    /**
     * Displays ANSI color samples to demonstrate rendering in the web terminal.
     */
    private static void showColors(Terminal terminal) {
        terminal.writer().println("Standard colors:");
        for (int i = 30; i <= 37; i++) {
            terminal.writer().printf("\033[%dm %-8s \033[0m", i, colorName(i));
        }
        terminal.writer().println();

        terminal.writer().println("Bright colors:");
        for (int i = 90; i <= 97; i++) {
            terminal.writer().printf("\033[%dm %-8s \033[0m", i, colorName(i));
        }
        terminal.writer().println();

        terminal.writer().println("Text attributes:");
        terminal.writer().println("  \033[1mBold\033[0m  \033[4mUnderline\033[0m  \033[7mInverse\033[0m");
    }

    private static String colorName(int code) {
        switch (code) {
            case 30:
            case 90:
                return "Black";
            case 31:
            case 91:
                return "Red";
            case 32:
            case 92:
                return "Green";
            case 33:
            case 93:
                return "Yellow";
            case 34:
            case 94:
                return "Blue";
            case 35:
            case 95:
                return "Magenta";
            case 36:
            case 96:
                return "Cyan";
            case 37:
            case 97:
                return "White";
            default:
                return "?";
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Usage: WebTerminalExample [port]");
                System.exit(1);
            }
        }
        if (port != 8080) {
            customPortExample(port);
        } else {
            basicExample();
        }
    }
}
