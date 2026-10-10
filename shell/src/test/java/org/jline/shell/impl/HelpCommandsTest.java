/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.io.IOException;
import java.util.List;

import org.jline.shell.ArgumentDescription;
import org.jline.shell.Command;
import org.jline.shell.CommandDescription;
import org.jline.shell.CommandSession;
import org.jline.utils.AttributedString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link HelpCommands}.
 */
class HelpCommandsTest extends AbstractCommandsTest {

    private HelpCommands commands;

    @Override
    @BeforeEach
    protected void setUp() throws IOException {
        super.setUp();
        dispatcher.addGroup(new SimpleCommandGroup("demo", new TestEchoCommand(), new TestUpperCmd()));
        commands = new HelpCommands(dispatcher);
        dispatcher.addGroup(commands);
    }

    static class TestUpperCmd extends AbstractCommand {
        TestUpperCmd() {
            super("upper");
        }

        @Override
        public String description() {
            return "Convert to upper case";
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            return String.join(" ", args).toUpperCase();
        }
    }

    @Test
    void groupName() {
        assertEquals("help", commands.name());
    }

    @Test
    void helpCommandExists() {
        Command cmd = commands.command("help");
        assertNotNull(cmd);
        assertNotNull(commands.command("?")); // alias
    }

    @Test
    void helpListAll() throws Exception {
        Command cmd = commands.command("help");
        cmd.execute(session, new String[0]);
        String output = outCapture.toString();
        assertTrue(output.contains("echo"));
        assertTrue(output.contains("upper"));
        assertTrue(output.contains("demo"));
    }

    @Test
    void helpSpecificCommand() throws Exception {
        Command cmd = commands.command("help");
        cmd.execute(session, new String[] {"echo"});
        String output = outCapture.toString();
        assertTrue(output.contains("echo"));
        assertTrue(output.contains("Echo arguments"));
    }

    @Test
    void helpUnknownCommand() throws Exception {
        Command cmd = commands.command("help");
        cmd.execute(session, new String[] {"nonexistent"});
        String err = errCapture.toString();
        assertTrue(err.contains("unknown command"));
    }

    static class WithDescribeAndSingleLineMainDescription extends AbstractCommand {
        WithDescribeAndSingleLineMainDescription() {
            super("c");
        }

        @Override
        public String description() {
            return "Does something incredible";
        }

        @Override
        public CommandDescription describe(final List<String> args) {
            return CommandDescription.builder()
                    .mainDescription(List.of(new AttributedString("Usage: c")))
                    .build();
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            return null;
        }
    }

    @Test
    void testCommandWithDescribeAndSingleLineMainDescription() throws Exception {
        dispatcher.addGroup(new SimpleCommandGroup("with-describe", new WithDescribeAndSingleLineMainDescription()));
        Command cmd = commands.command("help");
        cmd.execute(session, new String[] {"c"});
        String expected = "c\n" + "  Does something incredible\n" + "Usage: c\n";
        String output = outCapture.toString();
        assertEquals(expected, output);
    }

    static class WithDescribeAndMultiLineMainDescription extends AbstractCommand {
        WithDescribeAndMultiLineMainDescription() {
            super("c");
        }

        @Override
        public String description() {
            return "Does something incredible";
        }

        @Override
        public CommandDescription describe(final List<String> args) {
            return CommandDescription.builder()
                    .mainDescription(
                            List.of(new AttributedString("Usage: c"), new AttributedString("Want something cool?")))
                    .build();
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            return null;
        }
    }

    @Test
    void testCommandWithDescribeAndMultiLineMainDescription() throws Exception {
        dispatcher.addGroup(new SimpleCommandGroup("with-describe", new WithDescribeAndMultiLineMainDescription()));
        Command cmd = commands.command("help");
        cmd.execute(session, new String[] {"c"});
        String expected = "c\n" + "  Does something incredible\n" + "Usage: c\n" + "Want something cool?\n";
        String output = outCapture.toString();
        assertEquals(expected, output);
    }

    static class WithDescribeAndSingleLineArgumentLine extends AbstractCommand {
        WithDescribeAndSingleLineArgumentLine() {
            super("c");
        }

        @Override
        public String description() {
            return "Does something incredible";
        }

        @Override
        public CommandDescription describe(final List<String> args) {
            return CommandDescription.builder()
                    .mainDescription(List.of(new AttributedString("Usage: c <param>")))
                    .argument(new ArgumentDescription("<param>", List.of(new AttributedString("Just a param"))))
                    .build();
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            return null;
        }
    }

    @Test
    void testCommandWithDescribeAndSingleLineArgument() throws Exception {
        dispatcher.addGroup(new SimpleCommandGroup("with-describe", new WithDescribeAndSingleLineArgumentLine()));
        Command cmd = commands.command("help");
        cmd.execute(session, new String[] {"c"});
        String expected = "c\n" + "  Does something incredible\n" + "Usage: c <param>\n" + "  Arguments:\n"
                + "    <param> - Just a param\n";
        String output = outCapture.toString();
        assertEquals(expected, output);
    }

    static class WithDescribeAndMultipleLineArgumentLines extends AbstractCommand {
        WithDescribeAndMultipleLineArgumentLines() {
            super("c");
        }

        @Override
        public String description() {
            return "Does something incredible";
        }

        @Override
        public CommandDescription describe(final List<String> args) {
            return CommandDescription.builder()
                    .mainDescription(List.of(new AttributedString("Usage: c <param>")))
                    .argument(new ArgumentDescription(
                            "<param>",
                            List.of(new AttributedString("Just a param"), new AttributedString("That is very cool"))))
                    .build();
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            return null;
        }
    }

    @Test
    void testCommandWithWithDescribeAndMultipleLineArgumentLines() throws Exception {
        dispatcher.addGroup(new SimpleCommandGroup("with-describe", new WithDescribeAndMultipleLineArgumentLines()));
        Command cmd = commands.command("help");
        cmd.execute(session, new String[] {"c"});
        String expected = "c\n" + "  Does something incredible\n" + "Usage: c <param>\n" + "  Arguments:\n"
                + "    <param> - Just a param\n" + "              That is very cool\n";
        String output = outCapture.toString();
        assertEquals(expected, output);
    }
}
