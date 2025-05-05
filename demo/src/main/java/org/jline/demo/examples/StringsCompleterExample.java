/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

import org.jline.reader.Completer;
import org.jline.reader.impl.completer.StringsCompleter;

/**
 * Example demonstrating StringsCompleter in JLine.
 */
public class StringsCompleterExample {

    // SNIPPET_START: StringsCompleterExample
    public void demonstrateStringsCompleter() {
        // Complete with fixed strings
        Completer stringsCompleter = new StringsCompleter("add", "remove", "list", "help");

        // Complete with dynamic strings
        Supplier<Collection<String>> dynamicStrings = this::getCurrentCommands;
        Completer dynamicCompleter = new StringsCompleter(dynamicStrings);

        System.out.println("Completers created successfully");
    }

    private Collection<String> getCurrentCommands() {
        // In a real application, this might fetch commands from a registry
        return Arrays.asList("connect", "disconnect", "status", "help");
    }
    // SNIPPET_END: StringsCompleterExample
}
