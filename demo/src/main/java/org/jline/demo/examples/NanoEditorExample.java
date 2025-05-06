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
import java.nio.file.Paths;

import org.jline.builtins.Nano;
import org.jline.builtins.Options;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating the Nano editor in JLine builtins.
 */
public class NanoEditorExample {

    // SNIPPET_START: NanoEditorExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Configure Nano
        Options options = Options.compile(Nano.usage()).parse(new String[] {"--tabsize=4", "--tabstospaces"});

        // Launch Nano editor
        Nano nano = new Nano(terminal, Paths.get(""), options);
        nano.open("example.txt");
    }
    // SNIPPET_END: NanoEditorExample
}
