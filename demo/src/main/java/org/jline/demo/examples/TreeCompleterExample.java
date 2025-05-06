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
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import static org.jline.builtins.Completers.TreeCompleter.node;

/**
 * Example demonstrating TreeCompleter in JLine.
 */
public class TreeCompleterExample {

    // SNIPPET_START: TreeCompleterExample
    public static void main(String[] args) throws IOException {
        Completer treeCompleter = new TreeCompleter(
                node("help", node("commands"), node("syntax")),
                node(
                        "set",
                        node("color", node("red", "green", "blue")),
                        node("size", node("small", "medium", "large"))));

        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(treeCompleter)
                .build();

        System.out.println("Type a command and press Tab to navigate the command tree");
        String line = reader.readLine("tree> ");
        System.out.println("You entered: " + line);
    }
    // SNIPPET_END: TreeCompleterExample
}
