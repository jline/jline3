/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CommandLineTest {

    @Test
    void basicConstruction() {
        CommandLine cl = new CommandLine("ls -la", "ls -la", "", List.of("ls", "-la"), CommandLine.Type.COMMAND);
        assertEquals("ls -la", cl.line());
        assertEquals("ls -la", cl.head());
        assertEquals("", cl.tail());
        assertEquals(List.of("ls", "-la"), cl.args());
        assertEquals(CommandLine.Type.COMMAND, cl.type());
    }

    @Test
    void argsAreImmutable() {
        CommandLine cl = new CommandLine("test", "test", "", List.of("test"), CommandLine.Type.COMMAND);
        assertThrows(UnsupportedOperationException.class, () -> cl.args().add("extra"));
    }

    @Test
    void methodType() {
        CommandLine cl =
                new CommandLine("obj.method(", "obj.method", "(", List.of("obj.method"), CommandLine.Type.METHOD);
        assertEquals(CommandLine.Type.METHOD, cl.type());
    }

    @Test
    void syntaxType() {
        CommandLine cl = new CommandLine(
                "obj.method(x)", "obj.method", "(x)", List.of("obj.method", "x"), CommandLine.Type.SYNTAX);
        assertEquals(CommandLine.Type.SYNTAX, cl.type());
    }
}
