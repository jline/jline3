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
 * Example showing how to define commands and command groups.
 */
public class ShellCommandExample {

    // SNIPPET_START: ShellCommandExample
    /**
     * A command that converts text to upper case.
     */
    static class UpperCommand extends AbstractCommand {
        UpperCommand() {
            super("upper", "uc");
        }

        @Override
        public String description() {
            return "Convert text to upper case";
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            String result = String.join(" ", args).toUpperCase();
            session.out().println(result);
            return result;
        }
    }

    /**
     * A command that converts text to lower case.
     */
    static class LowerCommand extends AbstractCommand {
        LowerCommand() {
            super("lower", "lc");
        }

        @Override
        public String description() {
            return "Convert text to lower case";
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            String result = String.join(" ", args).toLowerCase();
            session.out().println(result);
            return result;
        }
    }

    public static void main(String[] args) throws Exception {
        try (Shell shell = Shell.builder()
                .prompt("text> ")
                .groups(new SimpleCommandGroup("text-tools", new UpperCommand(), new LowerCommand()))
                .build()) {
            shell.run();
        }
    }
    // SNIPPET_END: ShellCommandExample
}
