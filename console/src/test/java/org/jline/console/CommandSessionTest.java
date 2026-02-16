/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.jline.console.impl.DefaultJob;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the enriched {@link CommandRegistry.CommandSession}.
 */
public class CommandSessionTest {

    @Test
    void defaultSessionHasNullTerminal() {
        CommandRegistry.CommandSession session = new CommandRegistry.CommandSession();
        assertNull(session.terminal());
        assertNotNull(session.in());
        assertNotNull(session.out());
        assertNotNull(session.err());
    }

    @Test
    void sessionWithTerminal() throws Exception {
        Terminal terminal = TerminalBuilder.builder().dumb(true).build();
        CommandRegistry.CommandSession session = new CommandRegistry.CommandSession(terminal);
        assertSame(terminal, session.terminal());
    }

    @Test
    void sessionVariables() {
        CommandRegistry.CommandSession session = new CommandRegistry.CommandSession();
        assertNull(session.get("key"));
        session.put("key", "value");
        assertEquals("value", session.get("key"));
        assertEquals(1, session.variables().size());
    }

    @Test
    void variablesMapIsUnmodifiable() {
        CommandRegistry.CommandSession session = new CommandRegistry.CommandSession();
        session.put("key", "value");
        assertThrows(
                UnsupportedOperationException.class, () -> session.variables().put("x", "y"));
    }

    @Test
    void workingDirectory() {
        CommandRegistry.CommandSession session = new CommandRegistry.CommandSession();
        assertNull(session.workingDirectory());
        Path dir = Paths.get("/tmp");
        session.setWorkingDirectory(dir);
        assertEquals(dir, session.workingDirectory());
    }

    @Test
    void lastExitCode() {
        CommandRegistry.CommandSession session = new CommandRegistry.CommandSession();
        assertEquals(0, session.lastExitCode());
        session.setLastExitCode(42);
        assertEquals(42, session.lastExitCode());
    }

    @Test
    void foregroundJob() {
        CommandRegistry.CommandSession session = new CommandRegistry.CommandSession();
        assertNull(session.foregroundJob());
        DefaultJob job = new DefaultJob(1, "test", Thread.currentThread());
        session.setForegroundJob(job);
        assertSame(job, session.foregroundJob());
    }

    @Test
    void systemRegistry() {
        CommandRegistry.CommandSession session = new CommandRegistry.CommandSession();
        assertNull(session.systemRegistry());
    }
}
