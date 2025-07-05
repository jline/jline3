/*
 * Copyright (c) 2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.apache.felix.gogo.jline;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.felix.gogo.jline.Shell.Context;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Process;
import org.jline.builtins.Commands;
import org.jline.builtins.Options;
import org.jline.builtins.Options.HelpException;
import org.jline.builtins.PosixCommands;
import org.jline.builtins.TTop;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;

/**
 * Posix-like utilities.
 *
 * @see <a href="http://www.opengroup.org/onlinepubs/009695399/utilities/contents.html">
 * http://www.opengroup.org/onlinepubs/009695399/utilities/contents.html</a>
 */
public class Posix {

    static final String[] functions;

    static {
        // TTop function is new in JLine 3.2
        String[] func;
        try {
            @SuppressWarnings("unused")
            Class<?> cl = TTop.class;
            func = new String[] {
                "cat", "echo", "grep", "sort", "sleep", "cd", "pwd", "ls", "less", "watch", "nano", "tmux", "head",
                "tail", "clear", "wc", "date", "ttop",
            };
        } catch (Throwable t) {
            func = new String[] {
                "cat", "echo", "grep", "sort", "sleep", "cd", "pwd", "ls", "less", "watch", "nano", "tmux", "head",
                "tail", "clear", "wc", "date"
            };
        }
        functions = func;
    }

    public static final String DEFAULT_GREP_COLORS = "mt=1;31:fn=35:ln=32:se=36";

    private static final List<String> WINDOWS_EXECUTABLE_EXTENSIONS =
            Collections.unmodifiableList(Arrays.asList(".bat", ".exe", ".cmd"));

    private final CommandProcessor processor;

    public Posix(CommandProcessor processor) {
        this.processor = processor;
    }

    public void _main(CommandSession session, String[] argv) {
        if (argv == null || argv.length < 1) {
            throw new IllegalArgumentException();
        }
        Process process = Process.Utils.current();
        try {
            run(session, process, argv);
        } catch (IllegalArgumentException e) {
            process.err().println(e.getMessage());
            process.error(2);
        } catch (HelpException e) {
            HelpException.highlight(e.getMessage(), HelpException.defaultStyle())
                    .print(Shell.getTerminal(session));
            process.error(0);
        } catch (Exception e) {
            process.err().println(argv[0] + ": " + e.toString());
            process.error(1);
        }
    }

    protected Options parseOptions(CommandSession session, String[] usage, Object[] argv) throws Exception {
        Options opt = Options.compile(usage, s -> get(session, s)).parse(argv, true);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        return opt;
    }

    protected String get(CommandSession session, String name) {
        Object o = session.get(name);
        return o != null ? o.toString() : null;
    }

    /**
     * Create a PosixCommands.Context from the gogo CommandSession and Process.
     */
    protected PosixCommands.Context createPosixContext(CommandSession session, Process process) {
        return new PosixCommands.Context(
                process.in(),
                process.out(),
                process.err(),
                session.currentDir(),
                Shell.getTerminal(session),
                session::get);
    }

    protected Object run(CommandSession session, Process process, String[] argv) throws Exception {
        switch (argv[0]) {
            case "cat":
                cat(session, process, argv);
                break;
            case "echo":
                echo(session, process, argv);
                break;
            case "grep":
                grep(session, process, argv);
                break;
            case "sort":
                sort(session, process, argv);
                break;
            case "sleep":
                sleep(session, process, argv);
                break;
            case "cd":
                cd(session, process, argv);
                break;
            case "pwd":
                pwd(session, process, argv);
                break;
            case "ls":
                ls(session, process, argv);
                break;
            case "less":
                less(session, process, argv);
                break;
            case "watch":
                watch(session, process, argv);
                break;
            case "nano":
                nano(session, process, argv);
                break;
            case "tmux":
                tmux(session, process, argv);
                break;
            case "ttop":
                ttop(session, process, argv);
                break;
            case "clear":
                clear(session, process, argv);
                break;
            case "head":
                head(session, process, argv);
                break;
            case "tail":
                tail(session, process, argv);
                break;
            case "wc":
                wc(session, process, argv);
                break;
            case "date":
                date(session, process, argv);
                break;
        }
        return null;
    }

    protected void date(CommandSession session, Process process, String[] argv) throws Exception {
        PosixCommands.date(createPosixContext(session, process), argv);
    }

    protected void wc(CommandSession session, Process process, String[] argv) throws Exception {
        PosixCommands.wc(createPosixContext(session, process), argv);
    }

    protected void head(CommandSession session, Process process, String[] argv) throws Exception {
        PosixCommands.head(createPosixContext(session, process), argv);
    }

    protected void tail(CommandSession session, Process process, String[] argv) throws Exception {
        PosixCommands.tail(createPosixContext(session, process), argv);
    }

    protected void clear(CommandSession session, Process process, String[] argv) throws Exception {
        PosixCommands.clear(createPosixContext(session, process), argv);
    }

    protected void tmux(final CommandSession session, Process process, String[] argv) throws Exception {
        Commands.tmux(
                Shell.getTerminal(session),
                process.out(),
                System.err,
                () -> session.get(".tmux"),
                t -> session.put(".tmux", t),
                c -> startShell(session, c),
                Arrays.copyOfRange(argv, 1, argv.length));
    }

    private void startShell(CommandSession session, Terminal terminal) {
        new Thread(() -> runShell(session, terminal), terminal.getName() + " shell").start();
    }

    private void runShell(CommandSession session, Terminal terminal) {
        InputStream in = terminal.input();
        OutputStream out = terminal.output();
        CommandSession newSession = processor.createSession(in, out, out);
        newSession.put(Shell.VAR_TERMINAL, terminal);
        newSession.put(".tmux", session.get(".tmux"));
        Context context = new Context() {
            public String getProperty(String name) {
                return System.getProperty(name);
            }

            public void exit() throws Exception {
                terminal.close();
            }
        };
        try {
            new Shell(context, processor).gosh(newSession, new String[] {"--login"});
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                terminal.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected void ttop(final CommandSession session, Process process, String[] argv) throws Exception {
        PosixCommands.ttop(createPosixContext(session, process), argv);
    }

    protected void nano(final CommandSession session, Process process, String[] argv) throws Exception {
        PosixCommands.nano(createPosixContext(session, process), argv);
    }

    protected void watch(final CommandSession session, Process process, String[] argv) throws Exception {
        // Use enhanced PosixCommands.watch with command executor
        PosixCommands.CommandExecutor executor = command -> {
            try {
                String cmdLine = String.join(" ", command);
                Object result = session.execute(cmdLine);
                return result != null ? result.toString() : "";
            } catch (Exception e) {
                throw new RuntimeException("Command execution failed: " + e.getMessage(), e);
            }
        };
        PosixCommands.watch(createPosixContext(session, process), argv, executor);
    }

    // watchFull method removed - functionality moved to PosixCommands.watch
    /*
    protected void watchFull(final CommandSession session, Process process, String[] argv) throws Exception {
        final String[] usage = {
            "watch - watches & refreshes the output of a command",
            "Usage: watch [OPTIONS] COMMAND",
            "  -? --help                    Show help",
            "  -n --interval=SECONDS        Interval between executions of the command in seconds",
            "  -a --append                  The output should be appended but not clear the console"
        };

        Options opt = parseOptions(session, usage, argv);

        List<String> args = opt.args();
        if (args.isEmpty()) {
            throw new IllegalArgumentException("usage: watch COMMAND");
        }
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        final Terminal terminal = Shell.getTerminal(session);
        final CommandProcessor processor = Shell.getProcessor(session);
        try {
            int interval = 1;
            if (opt.isSet("interval")) {
                interval = opt.getNumber("interval");
                if (interval < 1) {
                    interval = 1;
                }
            }
            final String cmd = String.join(" ", args);
            Runnable task = () -> {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream os = new PrintStream(baos);
                InputStream is = new ByteArrayInputStream(new byte[0]);
                if (opt.isSet("append") || !terminal.puts(Capability.clear_screen)) {
                    terminal.writer().println();
                }
                try {
                    CommandSession ns = processor.createSession(is, os, os);
                    Set<String> vars = Shell.getCommands(session);
                    for (String n : vars) {
                        ns.put(n, session.get(n));
                    }
                    ns.execute(cmd);
                } catch (Throwable t) {
                    t.printStackTrace(os);
                }
                os.flush();
                terminal.writer().print(baos.toString());
                terminal.writer().flush();
            };
            executorService.scheduleAtFixedRate(task, 0, interval, TimeUnit.SECONDS);
            Attributes attr = terminal.enterRawMode();
            terminal.reader().read();
            terminal.setAttributes(attr);
        } finally {
            executorService.shutdownNow();
        }
    }
    */

    protected void less(CommandSession session, Process process, String[] argv) throws Exception {
        PosixCommands.less(createPosixContext(session, process), argv);
    }

    protected void sort(CommandSession session, Process process, String[] argv) throws Exception {
        PosixCommands.sort(createPosixContext(session, process), argv);
    }

    protected void pwd(CommandSession session, Process process, String[] argv) throws Exception {
        PosixCommands.pwd(createPosixContext(session, process), argv);
    }

    protected void cd(CommandSession session, Process process, String[] argv) throws Exception {
        Consumer<Path> directoryChanger = path -> session.currentDir(path);
        PosixCommands.cd(createPosixContext(session, process), argv, directoryChanger);
    }

    protected void ls(CommandSession session, Process process, String[] argv) throws Exception {
        PosixCommands.ls(createPosixContext(session, process), argv);
    }

    protected void cat(CommandSession session, Process process, String[] argv) throws Exception {
        PosixCommands.cat(createPosixContext(session, process), argv);
    }

    protected void echo(CommandSession session, Process process, Object[] argv) throws Exception {
        PosixCommands.echo(createPosixContext(session, process), argv);
    }

    protected void grep(CommandSession session, Process process, String[] argv) throws Exception {
        PosixCommands.grep(createPosixContext(session, process), argv);
    }

    protected void sleep(CommandSession session, Process process, String[] argv) throws Exception {
        PosixCommands.sleep(createPosixContext(session, process), argv);
    }

    public static Map<String, String> getColorMap(CommandSession session, String name, String def) {
        return PosixCommands.getColorMap(session::get, name, def);
    }

    /**
     * Apply style to text using color map.
     */
    public static String applyStyle(String text, Map<String, String> colors, String... types) {
        return PosixCommands.applyStyle(text, colors, types);
    }

    /**
     * Apply style to AttributedStringBuilder using color map.
     */
    public static void applyStyle(AttributedStringBuilder sb, Map<String, String> colors, String... types) {
        PosixCommands.applyStyle(sb, colors, types);
    }
}
