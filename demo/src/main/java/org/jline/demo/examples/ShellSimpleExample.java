/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import org.jline.shell.CommandSession;
import org.jline.shell.Shell;
import org.jline.shell.impl.AbstractCommand;
import org.jline.shell.impl.SimpleCommandGroup;

/**
 * Minimal Shell example with inline commands.
 */
public class ShellSimpleExample {

    // SNIPPET_START: ShellSimpleExample
    public static void main(String[] args) throws Exception {
        try (Shell shell = Shell.builder()
                .prompt("simple> ")
                .groups(new SimpleCommandGroup(
                        "commands",
                        new AbstractCommand("hello") {
                            @Override
                            public Object execute(CommandSession session, String[] a) {
                                session.out().println("Hello, world!");
                                return null;
                            }
                        },
                        new AbstractCommand("add") {
                            @Override
                            public String description() {
                                return "Add two numbers";
                            }

                            @Override
                            public Object execute(CommandSession session, String[] a) {
                                if (a.length < 2) {
                                    session.err().println("Usage: add <a> <b>");
                                    return null;
                                }
                                int sum = Integer.parseInt(a[0]) + Integer.parseInt(a[1]);
                                session.out().println(sum);
                                return sum;
                            }
                        }))
                .build()) {
            shell.run();
        }
    }
    // SNIPPET_END: ShellSimpleExample
}
