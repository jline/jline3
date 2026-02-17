/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import org.jline.reader.LineReader.Option;
import org.jline.shell.CommandSession;
import org.jline.shell.Shell;
import org.jline.shell.impl.AbstractCommand;
import org.jline.shell.impl.DefaultJobManager;
import org.jline.shell.impl.SimpleCommandGroup;
import org.jline.widget.TailTipWidgets;
import org.jline.widget.TailTipWidgets.TipType;

/**
 * Example demonstrating job control with background execution.
 * <p>
 * Try these commands:
 * <pre>
 *   echo hello world
 *   echo hello | upper
 *   sleep 5 &amp;
 *   jobs
 *   count 10 &amp;
 *   fg 2
 * </pre>
 */
public class ShellJobExample {

    // SNIPPET_START: ShellJobExample
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
            // Check for pipe input
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
            return "Convert text to upper case";
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            // Check for pipe input first
            Object pipeInput = session.get("_pipe_input");
            String input;
            if (pipeInput != null) {
                input = pipeInput.toString().trim();
            } else {
                input = String.join(" ", args);
            }
            String result = input.toUpperCase();
            session.out().println(result);
            return result;
        }
    }

    static class SleepCommand extends AbstractCommand {
        SleepCommand() {
            super("sleep");
        }

        @Override
        public String description() {
            return "Sleep for N seconds";
        }

        @Override
        public Object execute(CommandSession session, String[] args) throws Exception {
            int seconds = args.length > 0 ? Integer.parseInt(args[0]) : 1;
            Thread.sleep(seconds * 1000L);
            session.out().println("Slept for " + seconds + " seconds.");
            return null;
        }
    }

    static class CountCommand extends AbstractCommand {
        CountCommand() {
            super("count");
        }

        @Override
        public String description() {
            return "Count from 1 to N with 1-second intervals";
        }

        @Override
        public Object execute(CommandSession session, String[] args) throws Exception {
            int max = args.length > 0 ? Integer.parseInt(args[0]) : 5;
            for (int i = 1; i <= max; i++) {
                session.out().println(i);
                if (i < max) {
                    Thread.sleep(1000L);
                }
            }
            return max;
        }
    }

    public static void main(String[] args) throws Exception {
        try (Shell shell = Shell.builder()
                .prompt("job-demo> ")
                .jobManager(new DefaultJobManager())
                .groups(new SimpleCommandGroup(
                        "demo", new EchoCommand(), new UpperCommand(), new SleepCommand(), new CountCommand()))
                .option(Option.INSERT_BRACKET, true)
                .option(Option.DISABLE_EVENT_EXPANSION, true)
                .onReaderReady(reader -> {
                    try {
                        new TailTipWidgets(reader, desc -> null, 5, TipType.COMPLETER);
                    } catch (Exception e) {
                        // ignore
                    }
                })
                .build()) {
            shell.run();
        }
    }
    // SNIPPET_END: ShellJobExample
}
