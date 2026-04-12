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
import org.jline.shell.impl.AbstractCommand;

/**
 * Shared demo commands reused across shell examples.
 */
public class DemoCommands {

    private DemoCommands() {}

    /**
     * Echoes arguments to output.
     */
    public static class EchoCommand extends AbstractCommand {
        public EchoCommand() {
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

    /**
     * Converts text to upper case, reading from arguments or stdin.
     */
    public static class UpperCommand extends AbstractCommand {
        public UpperCommand() {
            super("upper");
        }

        @Override
        public String description() {
            return "Convert text to upper case";
        }

        @Override
        public Object execute(CommandSession session, String[] args) throws Exception {
            String input;
            if (args.length > 0) {
                input = String.join(" ", args);
            } else {
                input = new String(session.in().readAllBytes()).trim();
            }
            String result = input.toUpperCase();
            session.out().println(result);
            return result;
        }
    }
}
