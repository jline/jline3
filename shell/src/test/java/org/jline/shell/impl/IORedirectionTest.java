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
import java.nio.file.Files;
import java.nio.file.Path;

import org.jline.shell.*;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for I/O redirection operators (INPUT_REDIRECT, STDERR_REDIRECT, COMBINED_REDIRECT).
 */
public class IORedirectionTest {

    private Terminal terminal;
    private DefaultCommandDispatcher dispatcher;

    @BeforeEach
    void setUp() throws IOException {
        terminal = TerminalBuilder.builder().dumb(true).build();
        dispatcher = new DefaultCommandDispatcher(terminal);
        dispatcher.addGroup(new SimpleCommandGroup("test", new CatCommand(), new ErrCommand(), new BothCommand()));
    }

    /**
     * Reads from session input and prints to output.
     */
    static class CatCommand extends AbstractCommand {
        CatCommand() {
            super("cat");
        }

        @Override
        public Object execute(CommandSession session, String[] args) throws Exception {
            byte[] data = session.in().readAllBytes();
            String content = new String(data).trim();
            session.out().println(content);
            return content;
        }
    }

    /**
     * Writes to stderr.
     */
    static class ErrCommand extends AbstractCommand {
        ErrCommand() {
            super("errcmd");
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            String msg = args.length > 0 ? String.join(" ", args) : "error output";
            session.err().println(msg);
            return null;
        }
    }

    /**
     * Writes to both stdout and stderr.
     */
    static class BothCommand extends AbstractCommand {
        BothCommand() {
            super("bothcmd");
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            session.out().println("stdout line");
            session.err().println("stderr line");
            return "done";
        }
    }

    @Test
    void inputRedirect(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("input.txt");
        Files.writeString(inputFile, "hello from file\n");

        Object result = dispatcher.execute("cat < " + inputFile);
        assertEquals("hello from file", result);
    }

    @Test
    void stderrRedirect(@TempDir Path tempDir) throws Exception {
        Path errFile = tempDir.resolve("errors.txt");
        dispatcher.execute("errcmd some error 2> " + errFile);

        String content = Files.readString(errFile);
        assertTrue(content.contains("some error"));
    }

    @Test
    void combinedRedirect(@TempDir Path tempDir) throws Exception {
        Path allFile = tempDir.resolve("all.txt");
        dispatcher.execute("bothcmd &> " + allFile);

        String content = Files.readString(allFile);
        assertTrue(content.contains("stdout line"));
        assertTrue(content.contains("stderr line"));
    }

    @Test
    void inputRedirectParsing() {
        Pipeline pipeline = new PipelineParser().parse("cat < input.txt");
        assertFalse(pipeline.stages().isEmpty());
        Pipeline.Stage stage = pipeline.stages().get(0);
        assertNotNull(stage.inputSource());
        assertEquals("input.txt", stage.inputSource().toString());
    }

    @Test
    void stderrRedirectParsing() {
        Pipeline pipeline = new PipelineParser().parse("cmd 2> errors.txt");
        assertFalse(pipeline.stages().isEmpty());
        Pipeline.Stage stage = pipeline.stages().get(0);
        assertEquals(Pipeline.Operator.STDERR_REDIRECT, stage.operator());
        assertNotNull(stage.redirectTarget());
    }

    @Test
    void combinedRedirectParsing() {
        Pipeline pipeline = new PipelineParser().parse("cmd &> all.txt");
        assertFalse(pipeline.stages().isEmpty());
        Pipeline.Stage stage = pipeline.stages().get(0);
        assertEquals(Pipeline.Operator.COMBINED_REDIRECT, stage.operator());
        assertNotNull(stage.redirectTarget());
    }

    @Test
    void pipelineBuilderInputRedirect(@TempDir Path tempDir) {
        Path file = tempDir.resolve("in.txt");
        Pipeline pipeline = Pipeline.of("cat").inputRedirect(file).build();
        assertFalse(pipeline.stages().isEmpty());
    }

    @Test
    void pipelineBuilderStderrRedirect(@TempDir Path tempDir) {
        Path file = tempDir.resolve("err.txt");
        Pipeline pipeline = Pipeline.of("cmd").stderrRedirect(file).build();
        Pipeline.Stage stage = pipeline.stages().get(0);
        assertEquals(Pipeline.Operator.STDERR_REDIRECT, stage.operator());
    }

    @Test
    void pipelineBuilderCombinedRedirect(@TempDir Path tempDir) {
        Path file = tempDir.resolve("all.txt");
        Pipeline pipeline = Pipeline.of("cmd").combinedRedirect(file).build();
        Pipeline.Stage stage = pipeline.stages().get(0);
        assertEquals(Pipeline.Operator.COMBINED_REDIRECT, stage.operator());
    }
}
