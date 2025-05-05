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

import org.jline.builtins.Completers.TreeCompleter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import static org.jline.builtins.Completers.TreeCompleter.node;

/**
 * Example demonstrating TreeCompleter from builtins package in JLine.
 */
public class BuiltinsTreeCompleterExample {

    // SNIPPET_START: BuiltinsTreeCompleterExample
    public static void main(String[] args) throws IOException {
        // Create a tree completer
        TreeCompleter treeCompleter = new TreeCompleter(
                node("help", node("commands"), node("usage")),
                node("connect", node("server1"), node("server2")),
                node("exit"));

        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(treeCompleter)
                .build();

        String line = reader.readLine("tree> ");
        System.out.println("You entered: " + line);
    }
    // SNIPPET_END: BuiltinsTreeCompleterExample
}
