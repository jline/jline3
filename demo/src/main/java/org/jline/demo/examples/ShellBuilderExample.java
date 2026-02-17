/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.nio.file.Paths;

import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;
import org.jline.shell.CommandSession;
import org.jline.shell.Shell;
import org.jline.shell.impl.AbstractCommand;
import org.jline.shell.impl.SimpleCommandGroup;

/**
 * Example demonstrating how Shell.builder() eliminates REPL boilerplate.
 * <p>
 * Compare this with {@link ReplConsoleExample} to see the reduction in setup code.
 */
public class ShellBuilderExample {

    // SNIPPET_START: ShellBuilderExample
    public static void main(String[] args) {
        try (Shell shell = Shell.builder()
                .prompt("demo> ")
                .groups(new SimpleCommandGroup(
                        "demo",
                        new AbstractCommand("echo") {
                            @Override
                            public String description() {
                                return "Echo arguments to output";
                            }

                            @Override
                            public Object execute(CommandSession session, String[] a) {
                                String msg = String.join(" ", a);
                                session.out().println(msg);
                                return msg;
                            }
                        },
                        new AbstractCommand("greet") {
                            @Override
                            public String description() {
                                return "Greet someone";
                            }

                            @Override
                            public Object execute(CommandSession session, String[] a) {
                                String name = a.length > 0 ? a[0] : "World";
                                session.out().println("Hello, " + name + "!");
                                return null;
                            }
                        }))
                .variable(LineReader.HISTORY_FILE, Paths.get(System.getProperty("user.home"), ".demo_history"))
                .option(Option.INSERT_BRACKET, true)
                .option(Option.DISABLE_EVENT_EXPANSION, true)
                .build()) {
            shell.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // SNIPPET_END: ShellBuilderExample
}
