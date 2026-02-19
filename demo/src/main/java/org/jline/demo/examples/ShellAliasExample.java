/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.nio.file.Files;
import java.nio.file.Path;

import org.jline.shell.CommandSession;
import org.jline.shell.Shell;
import org.jline.shell.impl.AbstractCommand;
import org.jline.shell.impl.DefaultAliasManager;
import org.jline.shell.impl.SimpleCommandGroup;

/**
 * Example demonstrating the alias system with persistence.
 * <p>
 * Try these commands:
 * <pre>
 *   echo hello world
 *   alias ll=echo listing all files
 *   ll
 *   alias hi=echo hello $@
 *   hi world
 *   alias
 *   unalias ll
 *   alias
 *   upper some text
 *   alias u=upper
 *   u lowercase text
 * </pre>
 */
public class ShellAliasExample {

    // SNIPPET_START: ShellAliasExample
    static class EchoCommand extends AbstractCommand {
        EchoCommand() {
            super("echo");
        }

        @Override
        public String description() {
            return "Echo arguments to output";
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            String msg = String.join(" ", args);
            session.out().println(msg);
            return msg;
        }
    }

    static class UpperCommand extends AbstractCommand {
        UpperCommand() {
            super("upper");
        }

        @Override
        public String description() {
            return "Convert text to upper case";
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            String input = String.join(" ", args);
            String result = input.toUpperCase();
            session.out().println(result);
            return result;
        }
    }

    static class LowerCommand extends AbstractCommand {
        LowerCommand() {
            super("lower");
        }

        @Override
        public String description() {
            return "Convert text to lower case";
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            String input = String.join(" ", args);
            String result = input.toLowerCase();
            session.out().println(result);
            return result;
        }
    }

    public static void main(String[] args) throws Exception {
        // Create alias manager with persistence to a temp file
        Path aliasFile = Files.createTempFile("jline-aliases-", ".txt");
        DefaultAliasManager aliasManager = new DefaultAliasManager(aliasFile);

        try (Shell shell = Shell.builder()
                .prompt("alias-demo> ")
                .aliasManager(aliasManager) // HIGHLIGHT
                .helpCommands(true)
                .groups(new SimpleCommandGroup("demo", new EchoCommand(), new UpperCommand(), new LowerCommand()))
                .build()) {
            shell.run();
        } finally {
            // Save aliases on exit
            aliasManager.save();
            Files.deleteIfExists(aliasFile);
        }
    }
    // SNIPPET_END: ShellAliasExample
}
