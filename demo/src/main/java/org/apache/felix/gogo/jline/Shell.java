/*
 * Copyright (c) the original author(s).
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
 *
 * NOTE: This class has been modified to fix an issue with interruption handling in nested shells.
 * The fix in Posix.java's runShell method clears the current pipe before creating a child shell
 * and restores it afterward. This ensures that when Ctrl+C is pressed in a child shell
 * (e.g., when running 'sh' followed by 'ttop'), the interruption signal is properly propagated
 * to the child process.
 *
 * This is a temporary workaround for a Felix Gogo issue. The proper fix has been submitted to
 * the Felix project in two PRs:
 * 1. https://github.com/apache/felix-dev/pull/411 - Make Pipe.setCurrentPipe public
 * 2. https://github.com/apache/felix-dev/pull/412 - Update Posix.runShell to handle nested shells
 *
 * Once these PRs are merged and released, this class and the modified Posix.java should be removed
 * from JLine and the official Felix Gogo JLine implementation should be used instead.
 *
 * See: https://github.com/jline/jline3/issues/1143
 */

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.CharBuffer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.gogo.runtime.Closure;
import org.apache.felix.gogo.runtime.CommandProxy;
import org.apache.felix.gogo.runtime.CommandSessionImpl;
import org.apache.felix.gogo.runtime.Pipe;
import org.apache.felix.gogo.runtime.Reflective;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Function;
import org.apache.felix.service.command.Job;
import org.apache.felix.service.command.Job.Status;
import org.apache.felix.service.command.Parameter;
import org.apache.felix.service.threadio.ThreadIO;
import org.jline.builtins.Completers.CompletionData;
import org.jline.builtins.Completers.CompletionEnvironment;
import org.jline.builtins.Options;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.Terminal.SignalHandler;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public class Shell {

    public static final String VAR_COMPLETIONS = ".completions";
    public static final String VAR_COMMAND_LINE = ".commandLine";
    public static final String VAR_READER = ".reader";
    public static final String VAR_SESSION = ".session";
    public static final String VAR_PROCESSOR = ".processor";
    public static final String VAR_TERMINAL = ".terminal";
    public static final String VAR_EXCEPTION = "exception";
    public static final String VAR_RESULT = "_";
    public static final String VAR_LOCATION = ".location";
    public static final String VAR_PROMPT = "prompt";
    public static final String VAR_RPROMPT = "rprompt";
    public static final String VAR_SCOPE = "SCOPE";
    public static final String VAR_CONTEXT = org.apache.felix.gogo.runtime.activator.Activator.CONTEXT;

    static final String[] functions = {"gosh", "sh", "source", "help"};

    private final URI baseURI;
    private final String profile;
    private final Context context;
    private final CommandProcessor processor;
    private final ThreadIO tio;

    private AtomicBoolean stopping = new AtomicBoolean();

    public Shell(Context context, CommandProcessor processor) {
        this(context, processor, null);
    }

    public Shell(Context context, CommandProcessor processor, String profile) {
        this(context, processor, null, profile);
    }

    public Shell(Context context, CommandProcessor processor, ThreadIO tio, String profile) {
        this.context = context;
        this.processor = processor;
        this.tio = tio != null ? tio : getThreadIO(processor);
        String baseDir = context.getProperty("gosh.home");
        baseDir = (baseDir == null) ? context.getProperty("user.dir") : baseDir;
        this.baseURI = new File(baseDir).toURI();
        this.profile = profile != null ? profile : "gosh_profile";
    }

    private ThreadIO getThreadIO(CommandProcessor processor) {
        try {
            Field field = processor.getClass().getDeclaredField("threadIO");
            field.setAccessible(true);
            return (ThreadIO) field.get(processor);
        } catch (Throwable t) {
            return null;
        }
    }

    public Context getContext() {
        return context;
    }

    public static Terminal getTerminal(CommandSession session) {
        return (Terminal) session.get(VAR_TERMINAL);
    }

    public static LineReader getReader(CommandSession session) {
        return (LineReader) session.get(VAR_READER);
    }

    public static CommandProcessor getProcessor(CommandSession session) {
        return (CommandProcessor) session.get(VAR_PROCESSOR);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, List<CompletionData>> getCompletions(CommandSession session) {
        return (Map<String, List<CompletionData>>) session.get(VAR_COMPLETIONS);
    }

    @SuppressWarnings("unchecked")
    public static Set<String> getCommands(CommandSession session) {
        return (Set<String>) session.get(CommandSessionImpl.COMMANDS);
    }

    public static ParsedLine getParsedLine(CommandSession session) {
        return (ParsedLine) session.get(VAR_COMMAND_LINE);
    }

    public static String getPrompt(CommandSession session) {
        return expand(session, VAR_PROMPT, "gl! ");
    }

    public static String getRPrompt(CommandSession session) {
        return expand(session, VAR_RPROMPT, null);
    }

    public static String expand(CommandSession session, String name, String def) {
        Object prompt = session.get(name);
        if (prompt != null) {
            try {
                Object o = org.apache.felix.gogo.runtime.Expander.expand(
                        prompt.toString(), new Closure((CommandSessionImpl) session, null, null));
                if (o != null) {
                    return o.toString();
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return def;
    }

    public static String resolve(CommandSession session, String command) {
        String resolved = command;
        if (command.indexOf(':') < 0) {
            Set<String> commands = getCommands(session);
            Object path = session.get(VAR_SCOPE);
            String scopePath = (null == path ? "*" : path.toString());
            for (String scope : scopePath.split(":")) {
                for (String entry : commands) {
                    if ("*".equals(scope) && entry.endsWith(":" + command) || entry.equals(scope + ":" + command)) {
                        resolved = entry;
                        break;
                    }
                }
            }
        }
        return resolved;
    }

    public static CharSequence readScript(URI script) throws Exception {
        CharBuffer buf = CharBuffer.allocate(4096);
        StringBuilder sb = new StringBuilder();

        URLConnection conn = script.toURL().openConnection();

        try (InputStreamReader in = new InputStreamReader(conn.getInputStream())) {
            while (in.read(buf) > 0) {
                buf.flip();
                sb.append(buf);
                buf.clear();
            }
        } finally {
            if (conn instanceof HttpURLConnection) {
                ((HttpURLConnection) conn).disconnect();
            }
        }

        return sb;
    }

    @SuppressWarnings("unchecked")
    static Set<String> getVariables(CommandSession session) {
        return (Set<String>) session.get(".variables");
    }

    private static <T extends Annotation> T findAnnotation(Annotation[] anns, Class<T> clazz) {
        for (int i = 0; (anns != null) && (i < anns.length); i++) {
            if (clazz.isInstance(anns[i])) {
                return clazz.cast(anns[i]);
            }
        }
        return null;
    }

    public void stop() {
        stopping.set(true);
    }

    public Object gosh(CommandSession currentSession, String[] argv) throws Exception {
        final String[] usage = {
            "gosh - execute script with arguments in a new session",
            "  args are available as session variables $1..$9 and $args.",
            "Usage: gosh [OPTIONS] [script-file [args..]]",
            "  -c --command             pass all remaining args to sub-shell",
            "     --nointeractive       don't start interactive session",
            "     --nohistory           don't save the command history",
            "     --login               login shell (same session, reads etc/gosh_profile)",
            "  -s --noshutdown          don't shutdown framework when script completes",
            "  -x --xtrace              echo commands before execution",
            "  -? --help                show help",
            "If no script-file, an interactive shell is started, type $D to exit."
        };

        Options opt = Options.compile(usage).setOptionsFirst(true).parse(argv);
        List<String> args = opt.args();

        boolean login = opt.isSet("login");
        boolean interactive = !opt.isSet("nointeractive");

        if (opt.isSet("help")) {
            opt.usage(System.err);
            if (login && !opt.isSet("noshutdown")) {
                shutdown();
            }
            return null;
        }

        if (opt.isSet("command") && args.isEmpty()) {
            throw opt.usageError("option --command requires argument(s)");
        }

        CommandSession session;
        if (login) {
            session = currentSession;
        } else {
            session = createChildSession(currentSession);
        }

        if (opt.isSet("xtrace")) {
            session.put("echo", true);
        }

        Terminal terminal = getTerminal(session);
        session.put(Shell.VAR_CONTEXT, context);
        session.put(Shell.VAR_PROCESSOR, processor);
        session.put(Shell.VAR_SESSION, session);
        session.put("#TERM", (Function) (s, arguments) -> terminal.getType());
        session.put("#COLUMNS", (Function) (s, arguments) -> terminal.getWidth());
        session.put("#LINES", (Function) (s, arguments) -> terminal.getHeight());
        session.put("#PWD", (Function) (s, arguments) -> s.currentDir().toString());
        if (!opt.isSet("nohistory")) {
            session.put(LineReader.HISTORY_FILE, Paths.get(System.getProperty("user.home"), ".gogo.history"));
        }

        if (tio != null) {
            PrintWriter writer = terminal.writer();
            PrintStream out = new PrintStream(new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    write(new byte[] {(byte) b}, 0, 1);
                }

                public void write(byte b[], int off, int len) {
                    writer.write(new String(b, off, len));
                }

                public void flush() {
                    writer.flush();
                }

                public void close() {
                    writer.close();
                }
            });
            tio.setStreams(terminal.input(), out, out);
        }

        try {
            LineReader reader;
            if (args.isEmpty() && interactive) {
                CompletionEnvironment completionEnvironment = new CompletionEnvironment() {
                    @Override
                    public Map<String, List<CompletionData>> getCompletions() {
                        return Shell.getCompletions(session);
                    }

                    @Override
                    public Set<String> getCommands() {
                        return Shell.getCommands(session);
                    }

                    @Override
                    public String resolveCommand(String command) {
                        return Shell.resolve(session, command);
                    }

                    @Override
                    public String commandName(String command) {
                        int idx = command.indexOf(':');
                        return idx >= 0 ? command.substring(idx + 1) : command;
                    }

                    @Override
                    public Object evaluate(LineReader reader, ParsedLine line, String func) throws Exception {
                        session.put(Shell.VAR_COMMAND_LINE, line);
                        return session.execute(func);
                    }
                };
                reader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .variables(((CommandSessionImpl) session).getVariables())
                        .completer(new org.jline.builtins.Completers.Completer(completionEnvironment))
                        .highlighter(new Highlighter(session))
                        .parser(new Parser())
                        .expander(new Expander(session))
                        .build();
                reader.setOpt(LineReader.Option.AUTO_FRESH_LINE);
                session.put(Shell.VAR_READER, reader);
                session.put(Shell.VAR_COMPLETIONS, new HashMap<>());
            } else {
                reader = null;
            }

            if (login || interactive) {
                URI uri = baseURI.resolve("etc/" + profile);
                if (!new File(uri).exists()) {
                    URL url = getClass().getResource("/ext/" + profile);
                    if (url == null) {
                        url = getClass().getResource("/" + profile);
                    }
                    uri = (url == null) ? null : url.toURI();
                }
                if (uri != null) {
                    source(session, uri.toString());
                }
            }

            Object result = null;

            if (args.isEmpty()) {
                if (interactive) {
                    result = runShell(session, terminal, reader);
                }
            } else {
                CharSequence program;

                if (opt.isSet("command")) {
                    StringBuilder buf = new StringBuilder();
                    for (String arg : args) {
                        if (buf.length() > 0) {
                            buf.append(' ');
                        }
                        buf.append(arg);
                    }
                    program = buf;
                } else {
                    URI script = session.currentDir().toUri().resolve(args.remove(0));

                    // set script arguments
                    session.put("0", script);
                    session.put("args", args);

                    for (int i = 0; i < args.size(); ++i) {
                        session.put(String.valueOf(i + 1), args.get(i));
                    }

                    program = readScript(script);
                }

                result = session.execute(program);
            }

            if (login && interactive && !opt.isSet("noshutdown")) {
                if (terminal != null) {
                    terminal.writer().println("gosh: stopping framework");
                    terminal.flush();
                }
                shutdown();
            }

            return result;
        } finally {
            if (tio != null) {
                tio.close();
            }
        }
    }

    private CommandSession createChildSession(CommandSession parent) {
        CommandSession session = processor.createSession(parent);
        getVariables(parent).stream()
                .filter(key -> key.matches("[.]?[A-Z].*"))
                .forEach(key -> session.put(key, parent.get(key)));
        session.put(Shell.VAR_TERMINAL, getTerminal(parent));
        return session;
    }

    private static final Method PIPE_SET_CURRENT_PIPE;

    static {
        try {
            PIPE_SET_CURRENT_PIPE = Pipe.class.getDeclaredMethod("setCurrentPipe", Pipe.class);
            PIPE_SET_CURRENT_PIPE.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private Object runShell(CommandSession session, Terminal terminal, LineReader reader) throws InterruptedException {
        Object previous;
        try {
            previous = PIPE_SET_CURRENT_PIPE.invoke(null, (Object) null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            return doRunShell(session, terminal, reader);
        } finally {
            try {
                PIPE_SET_CURRENT_PIPE.invoke(null, previous);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Object doRunShell(final CommandSession session, Terminal terminal, LineReader reader)
            throws InterruptedException {
        AtomicBoolean reading = new AtomicBoolean();
        session.setJobListener((job, previous, current) -> {
            if (previous == Status.Background
                    || current == Status.Background
                    || previous == Status.Suspended
                    || current == Status.Suspended) {
                int width = terminal.getWidth();
                String status = current.name().toLowerCase();
                terminal.writer().write(getStatusLine(job, width, status));
                terminal.flush();
                if (reading.get() && !stopping.get()) {
                    reader.callWidget(LineReader.REDRAW_LINE);
                    reader.callWidget(LineReader.REDISPLAY);
                }
            }
        });
        SignalHandler intHandler = terminal.handle(Signal.INT, s -> {
            Job current = session.foregroundJob();
            if (current != null) {
                current.interrupt();
            }
        });
        SignalHandler suspHandler = terminal.handle(Signal.TSTP, s -> {
            Job current = session.foregroundJob();
            if (current != null) {
                current.suspend();
            }
        });
        Object result = null;
        try {
            while (!stopping.get()) {
                try {
                    reading.set(true);
                    try {
                        String prompt = Shell.getPrompt(session);
                        String rprompt = Shell.getRPrompt(session);
                        if (stopping.get()) {
                            break;
                        }
                        reader.readLine(prompt, rprompt, (Character) null, null);
                    } finally {
                        reading.set(false);
                    }
                    ParsedLine parsedLine = reader.getParsedLine();
                    if (parsedLine == null) {
                        throw new EndOfFileException();
                    }
                    try {
                        result = session.execute(((ParsedLineImpl) parsedLine).program());
                        session.put(Shell.VAR_RESULT, result); // set $_ to last result

                        if (result != null && !Boolean.FALSE.equals(session.get(".Gogo.format"))) {
                            System.out.println(session.format(result, Converter.INSPECT));
                        }
                    } catch (Exception e) {
                        AttributedStringBuilder sb = new AttributedStringBuilder();
                        sb.style(sb.style().foreground(AttributedStyle.RED));
                        sb.append(e.toString());
                        sb.style(sb.style().foregroundDefault());
                        terminal.writer().println(sb.toAnsi(terminal));
                        terminal.flush();
                        session.put(Shell.VAR_EXCEPTION, e);
                    }

                    waitJobCompletion(session);

                } catch (IOError e) {
                    if (e.getCause() instanceof IOException) {
                        // Terminal I/O broken (e.g. Ctrl+C on some platforms), exit the loop
                        break;
                    }
                    throw e;
                } catch (UserInterruptException e) {
                    // continue;
                } catch (EndOfFileException e) {
                    try {
                        reader.getHistory().save();
                    } catch (IOException e1) {
                        // ignore
                    }
                    break;
                }
            }
        } finally {
            terminal.handle(Signal.INT, intHandler);
            terminal.handle(Signal.TSTP, suspHandler);
        }
        return result;
    }

    private void waitJobCompletion(final CommandSession session) throws InterruptedException {
        while (true) {
            Job job = session.foregroundJob();
            if (job != null) {
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (job) {
                    if (job.status() == Status.Foreground) {
                        job.wait();
                    }
                }
            } else {
                break;
            }
        }
    }

    private String getStatusLine(Job job, int width, String status) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < width - 1; i++) {
            sb.append(' ');
        }
        sb.append('\r');
        sb.append("[").append(job.id()).append("]  ");
        sb.append(status);
        for (int i = status.length(); i < "background".length(); i++) {
            sb.append(' ');
        }
        sb.append("  ").append(job.command()).append("\n");
        return sb.toString();
    }

    @Descriptor("start a new shell")
    public Object sh(final CommandSession session, String[] argv) throws Exception {
        return gosh(session, argv);
    }

    private void shutdown() throws Exception {
        context.exit();
    }

    @Descriptor("Evaluates contents of file")
    public Object source(CommandSession session, String script) throws Exception {
        URI uri = session.currentDir().toUri().resolve(script);
        session.put("0", uri);
        try {
            return session.execute(readScript(uri));
        } finally {
            session.put("0", null); // API doesn't support remove
        }
    }

    private Map<String, List<Method>> getReflectionCommands(CommandSession session) {
        Map<String, List<Method>> commands = new TreeMap<>();
        Set<String> names = getCommands(session);
        for (String name : names) {
            Function function = (Function) session.get(name);
            if (function instanceof CommandProxy) {
                Object target = ((CommandProxy) function).getTarget();
                List<Method> methods = new ArrayList<>();
                String func = name.substring(name.indexOf(':') + 1).toLowerCase();
                List<String> funcs = new ArrayList<>();
                funcs.add("is" + func);
                funcs.add("get" + func);
                funcs.add("set" + func);
                if (Reflective.KEYWORDS.contains(func)) {
                    funcs.add("_" + func);
                } else {
                    funcs.add(func);
                }
                for (Method method : target.getClass().getMethods()) {
                    if (funcs.contains(method.getName().toLowerCase())) {
                        methods.add(method);
                    }
                }
                commands.put(name, methods);
                ((CommandProxy) function).ungetTarget();
            }
        }
        return commands;
    }

    @Descriptor("displays available commands")
    public void help(CommandSession session) {
        Map<String, List<Method>> commands = getReflectionCommands(session);
        commands.keySet().forEach(System.out::println);
    }

    @Descriptor("displays information about a specific command")
    public void help(CommandSession session, @Descriptor("target command") String name) {
        Map<String, List<Method>> commands = getReflectionCommands(session);

        List<Method> methods = null;

        // If the specified command doesn't have a scope, then
        // search for matching methods by ignoring the scope.
        int scopeIdx = name.indexOf(':');
        if (scopeIdx < 0) {
            for (Entry<String, List<Method>> entry : commands.entrySet()) {
                String k = entry.getKey().substring(entry.getKey().indexOf(':') + 1);
                if (name.equals(k)) {
                    name = entry.getKey();
                    methods = entry.getValue();
                    break;
                }
            }
        }
        // Otherwise directly look up matching methods.
        else {
            methods = commands.get(name);
        }

        if ((methods != null) && (methods.size() > 0)) {
            for (Method m : methods) {
                Descriptor d = m.getAnnotation(Descriptor.class);
                if (d == null) {
                    System.out.println("\n" + m.getName());
                } else {
                    System.out.println("\n" + m.getName() + " - " + d.value());
                }

                System.out.println("   scope: " + name.substring(0, name.indexOf(':')));

                // Get flags and options.
                Class<?>[] paramTypes = m.getParameterTypes();
                Map<String, Parameter> flags = new TreeMap<>();
                Map<String, String> flagDescs = new TreeMap<>();
                Map<String, Parameter> options = new TreeMap<>();
                Map<String, String> optionDescs = new TreeMap<>();
                List<String> params = new ArrayList<>();
                Annotation[][] anns = m.getParameterAnnotations();
                for (int paramIdx = 0; paramIdx < anns.length; paramIdx++) {
                    Class<?> paramType = m.getParameterTypes()[paramIdx];
                    if (paramType == CommandSession.class) {
                        /* Do not bother the user with a CommandSession. */
                        continue;
                    }
                    Parameter p = findAnnotation(anns[paramIdx], Parameter.class);
                    d = findAnnotation(anns[paramIdx], Descriptor.class);
                    if (p != null) {
                        if (p.presentValue().equals(Parameter.UNSPECIFIED)) {
                            options.put(p.names()[0], p);
                            if (d != null) {
                                optionDescs.put(p.names()[0], d.value());
                            }
                        } else {
                            flags.put(p.names()[0], p);
                            if (d != null) {
                                flagDescs.put(p.names()[0], d.value());
                            }
                        }
                    } else if (d != null) {
                        params.add(paramTypes[paramIdx].getSimpleName());
                        params.add(d.value());
                    } else {
                        params.add(paramTypes[paramIdx].getSimpleName());
                        params.add("");
                    }
                }

                // Print flags and options.
                if (flags.size() > 0) {
                    System.out.println("   flags:");
                    for (Entry<String, Parameter> entry : flags.entrySet()) {
                        // Print all aliases.
                        String[] names = entry.getValue().names();
                        System.out.print("      " + names[0]);
                        for (int aliasIdx = 1; aliasIdx < names.length; aliasIdx++) {
                            System.out.print(", " + names[aliasIdx]);
                        }
                        System.out.println("   " + flagDescs.get(entry.getKey()));
                    }
                }
                if (options.size() > 0) {
                    System.out.println("   options:");
                    for (Entry<String, Parameter> entry : options.entrySet()) {
                        // Print all aliases.
                        String[] names = entry.getValue().names();
                        System.out.print("      " + names[0]);
                        for (int aliasIdx = 1; aliasIdx < names.length; aliasIdx++) {
                            System.out.print(", " + names[aliasIdx]);
                        }
                        System.out.println("   "
                                + optionDescs.get(entry.getKey())
                                + ((entry.getValue().absentValue() == null) ? "" : " [optional]"));
                    }
                }
                if (params.size() > 0) {
                    System.out.println("   parameters:");
                    for (Iterator<String> it = params.iterator(); it.hasNext(); ) {
                        System.out.println("      " + it.next() + "   " + it.next());
                    }
                }
            }
        }
    }

    public interface Context {
        String getProperty(String name);

        void exit() throws Exception;
    }
}
