/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.util.Map;

import org.jline.shell.CommandSession;
import org.jline.shell.Pipeline;
import org.jline.shell.Shell;
import org.jline.shell.impl.AbstractCommand;
import org.jline.shell.impl.PipelineParser;
import org.jline.shell.impl.SimpleCommandGroup;

/**
 * Example demonstrating pipeline extensibility with custom operators.
 * <p>
 * This example defines a custom operator {@code ==>} that maps to PIPE,
 * and shows the sequence operator {@code ;}.
 * <p>
 * Try these commands:
 * <pre>
 *   echo hello ==> upper
 *   echo first ; echo second
 *   echo hello | upper
 *   echo hello && echo world
 *   fail || echo recovered
 * </pre>
 */
public class ShellPipelineExample {

    // SNIPPET_START: ShellPipelineExample
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
            Object pipeInput = session.get("_pipe_input");
            String msg;
            if (args.length > 0) {
                msg = String.join(" ", args);
            } else if (pipeInput != null) {
                msg = pipeInput.toString();
            } else {
                msg = "";
            }
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

    static class FailCommand extends AbstractCommand {
        FailCommand() {
            super("fail");
        }

        @Override
        public String description() {
            return "Always fails (for testing conditionals)";
        }

        @Override
        public Object execute(CommandSession session, String[] args) throws Exception {
            throw new RuntimeException("command failed");
        }
    }

    public static void main(String[] args) throws Exception {
        // Custom pipeline parser with an additional "==>" operator mapped to PIPE
        PipelineParser customParser = new PipelineParser(Map.of("==>", Pipeline.Operator.PIPE)); // HIGHLIGHT

        try (Shell shell = Shell.builder()
                .prompt("pipeline-demo> ")
                .pipelineParser(customParser) // HIGHLIGHT
                .helpCommands(true)
                .groups(new SimpleCommandGroup("demo", new EchoCommand(), new UpperCommand(), new FailCommand()))
                .build()) {
            shell.run();
        }
    }
    // SNIPPET_END: ShellPipelineExample
}
