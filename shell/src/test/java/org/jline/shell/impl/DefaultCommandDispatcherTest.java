/*
 * Copyright (c) 2026, the original author(s).
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
import java.util.Map;

import org.jline.shell.*;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link DefaultCommandDispatcher}.
 */
public class DefaultCommandDispatcherTest {

    private Terminal terminal;
    private DefaultCommandDispatcher dispatcher;

    @BeforeEach
    void setUp() throws IOException {
        terminal = TerminalBuilder.builder().dumb(true).build();
        dispatcher = new DefaultCommandDispatcher(terminal);
        dispatcher.addGroup(new SimpleCommandGroup(
                "test", new EchoCommand(), new FailCommand(), new UpperCommand(), new NoopCommand()));
    }

    // --- Fixture commands ---

    static class EchoCommand extends AbstractCommand {
        EchoCommand() {
            super("echo");
        }

        @Override
        public String description() {
            return "Echo arguments";
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            String msg = String.join(" ", args);
            session.out().println(msg);
            return msg;
        }
    }

    static class FailCommand extends AbstractCommand {
        FailCommand() {
            super("fail");
        }

        @Override
        public String description() {
            return "Always fails";
        }

        @Override
        public Object execute(CommandSession session, String[] args) throws Exception {
            throw new RuntimeException("command failed");
        }
    }

    static class UpperCommand extends AbstractCommand {
        UpperCommand() {
            super("upper");
        }

        @Override
        public String description() {
            return "Convert pipe input to upper case";
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            Object pipeInput = session.get("_pipe_input");
            String input = pipeInput != null ? pipeInput.toString().trim() : String.join(" ", args);
            String result = input.toUpperCase();
            session.out().println(result);
            return result;
        }
    }

    static class NoopCommand extends AbstractCommand {
        NoopCommand() {
            super("noop");
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            return null;
        }
    }

    // --- Tests ---

    @Test
    void simpleExecution() throws Exception {
        Object result = dispatcher.execute("echo hello world");
        assertEquals("hello world", result);
    }

    @Test
    void unknownCommand() {
        assertThrows(IllegalArgumentException.class, () -> dispatcher.execute("nonexistent"));
    }

    @Test
    void pipeExecution() throws Exception {
        Object result = dispatcher.execute("echo hello | upper");
        assertEquals("HELLO", result);
    }

    @Test
    void flipPipe() throws Exception {
        Object result = dispatcher.execute("echo hello |; echo prefix");
        // Flip appends output as argument: "echo prefix hello"
        assertEquals("prefix hello", result);
    }

    @Test
    void multiStagePipe() throws Exception {
        Object result = dispatcher.execute("echo test | upper | upper");
        assertEquals("TEST", result);
    }

    @Test
    void andSuccessChain() throws Exception {
        Object result = dispatcher.execute("echo first && echo second");
        assertEquals("second", result);
    }

    @Test
    void andFailureSkipsNext() throws Exception {
        // fail has AND operator, exception is caught, exitCode set to 1
        // next stage has prevOp=AND and exitCode=1, so it's skipped
        Object result = dispatcher.execute("fail && echo second");
        assertNull(result);
    }

    @Test
    void andChainFailurePropagates() throws Exception {
        // noop succeeds, fail throws with AND -> exitCode 1, echo skipped (prevOp=AND, exitCode=1)
        Object result = dispatcher.execute("noop && fail && echo skipped");
        assertNull(result);
    }

    @Test
    void orSuccessSkipsNext() throws Exception {
        // echo first succeeds (exitCode 0), next stage has prevOp=OR and exitCode=0 -> skip
        Object result = dispatcher.execute("echo first || echo second");
        assertEquals("first", result);
    }

    @Test
    void orFailureRunsNext() throws Exception {
        // fail throws (exitCode 1), next stage has prevOp=OR and exitCode=1 -> run
        Object result = dispatcher.execute("fail || echo recovered");
        assertEquals("recovered", result);
    }

    @Test
    void sequenceOperator() throws Exception {
        Object result = dispatcher.execute("echo first ; echo second");
        assertEquals("second", result);
    }

    @Test
    void sequenceWithFailure() throws Exception {
        // fail ; echo recovered - SEQUENCE continues unconditionally
        Object result = dispatcher.execute("fail ; echo recovered");
        assertEquals("recovered", result);
    }

    @Test
    void redirectToFile(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("out.txt");
        dispatcher.execute("echo hello > " + file);
        String content = Files.readString(file);
        assertTrue(content.trim().contains("hello"));
    }

    @Test
    void appendToFile(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("out.txt");
        dispatcher.execute("echo line1 > " + file);
        dispatcher.execute("echo line2 >> " + file);
        String content = Files.readString(file);
        assertTrue(content.contains("line1"));
        assertTrue(content.contains("line2"));
    }

    @Test
    void backgroundExecution() throws Exception {
        DefaultJobManager jobManager = new DefaultJobManager();
        DefaultCommandDispatcher bgDispatcher = new DefaultCommandDispatcher(terminal, jobManager);
        bgDispatcher.addGroup(new SimpleCommandGroup("test", new NoopCommand()));
        bgDispatcher.execute("noop &");
        // Give the background thread time to complete
        Thread.sleep(200);
        // Job should have been created
        assertFalse(jobManager.jobs().isEmpty());
    }

    @Test
    void emptyInput() throws Exception {
        assertNull(dispatcher.execute(""));
        assertNull(dispatcher.execute("   "));
        assertNull(dispatcher.execute((String) null));
    }

    @Test
    void aliasExpansion() throws Exception {
        AliasManager aliasManager = new DefaultAliasManager();
        aliasManager.setAlias("hi", "echo hello");
        DefaultCommandDispatcher aliasDispatcher = new DefaultCommandDispatcher(terminal, null, null, aliasManager);
        aliasDispatcher.addGroup(new SimpleCommandGroup("test", new EchoCommand()));
        Object result = aliasDispatcher.execute("hi");
        assertEquals("hello", result);
    }

    @Test
    void customPipelineParser() throws Exception {
        PipelineParser custom = new PipelineParser(Map.of("==>", Pipeline.Operator.PIPE));
        DefaultCommandDispatcher customDispatcher = new DefaultCommandDispatcher(terminal, null, custom, null);
        customDispatcher.addGroup(new SimpleCommandGroup("test", new EchoCommand(), new UpperCommand()));
        Object result = customDispatcher.execute("echo hello ==> upper");
        assertEquals("HELLO", result);
    }

    @Test
    void findCommand() {
        assertNotNull(dispatcher.findCommand("echo"));
        assertNotNull(dispatcher.findCommand("fail"));
        assertNull(dispatcher.findCommand("nonexistent"));
    }

    @Test
    void completer() {
        assertNotNull(dispatcher.completer());
    }

    @Test
    void executePipeline() throws Exception {
        Pipeline pipeline = Pipeline.of("echo test").pipe("upper").build();
        Object result = dispatcher.execute(pipeline);
        assertEquals("TEST", result);
    }
}
