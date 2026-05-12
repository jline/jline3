/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import org.jline.shell.CommandSession;

/**
 * Echoes arguments to output.
 */
public class TestEchoCommand extends AbstractCommand {
    public TestEchoCommand() {
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
