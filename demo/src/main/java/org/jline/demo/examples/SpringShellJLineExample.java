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

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

/**
 * Example demonstrating Spring Shell integration with JLine.
 * Note: This is a simplified example that doesn't actually use Spring Shell,
 * as that would require adding Spring Boot dependencies to the demo project.
 */
public class SpringShellJLineExample {

    // SNIPPET_START: SpringShellJLineExample
    // This is a simplified example showing how JLine components would be configured in Spring Shell

    // In a real Spring Shell application, this would be a @SpringBootApplication class
    public static void main(String[] args) throws IOException {
        // In Spring Shell, these would be @Bean methods
        Terminal terminal = createTerminal();
        LineReader lineReader = createLineReader(terminal);

        // Simulate Spring Shell prompt provider
        String prompt = createPrompt();

        // Simulate Spring Shell command execution
        System.out.println("Spring Shell with JLine Example");
        System.out.println("Type 'help' to see available commands");
        System.out.println("Type 'exit' to quit");

        while (true) {
            String line = lineReader.readLine(prompt);

            if (line.equals("exit")) {
                break;
            } else if (line.equals("help")) {
                System.out.println("Available commands:");
                System.out.println("  help    - Show this help");
                System.out.println("  echo    - Echo a message");
                System.out.println("  hello   - Say hello");
                System.out.println("  sum     - Sum two numbers");
                System.out.println("  exit    - Exit the application");
            } else if (line.startsWith("echo")) {
                String[] parts = line.split("\\s+", 2);
                if (parts.length > 1) {
                    System.out.println(parts[1]);
                } else {
                    System.out.println("Usage: echo <message>");
                }
            } else if (line.startsWith("hello")) {
                String[] parts = line.split("\\s+", 2);
                String name = parts.length > 1 ? parts[1] : "World";
                System.out.println("Hello, " + name + "!");
            } else if (line.startsWith("sum")) {
                String[] parts = line.split("\\s+");
                if (parts.length == 3) {
                    try {
                        int a = Integer.parseInt(parts[1]);
                        int b = Integer.parseInt(parts[2]);
                        System.out.println(a + " + " + b + " = " + (a + b));
                    } catch (NumberFormatException e) {
                        System.out.println("Error: Invalid numbers");
                    }
                } else {
                    System.out.println("Usage: sum <a> <b>");
                }
            } else if (!line.isEmpty()) {
                System.out.println("Unknown command: " + line);
            }
        }

        System.out.println("Goodbye!");
        terminal.close();
    }

    // In Spring Shell, this would be a @Bean method
    private static Terminal createTerminal() throws IOException {
        return TerminalBuilder.builder().system(true).build();
    }

    // In Spring Shell, this would be a @Bean method
    private static LineReader createLineReader(Terminal terminal) {
        return LineReaderBuilder.builder()
                .terminal(terminal)
                .option(LineReader.Option.AUTO_FRESH_LINE, true)
                .option(LineReader.Option.HISTORY_BEEP, false)
                .build();
    }

    // In Spring Shell, this would be a @Bean method for PromptProvider
    private static String createPrompt() {
        return new AttributedString("custom-shell:> ", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
                .toAnsi();
    }
    // SNIPPET_END: SpringShellJLineExample
}
