/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.builtins;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public class Completers {

    public interface CompletionEnvironment {
        Map<String, List<CompletionData>> getCompletions();
        Set<String> getCommands();
        String resolveCommand(String command);
        String commandName(String command);
        Object evaluate(LineReader reader, ParsedLine line, String func) throws Exception;
    }

    public static class CompletionData {
        public final List<String> options;
        public final String description;
        public final String argument;
        public final String condition;

        public CompletionData(List<String> options, String description, String argument, String condition) {
            this.options = options;
            this.description = description;
            this.argument = argument;
            this.condition = condition;
        }
    }

    public static class Completer implements org.jline.reader.Completer {

        private final CompletionEnvironment environment;

        public Completer(CompletionEnvironment environment) {
            this.environment = environment;
        }

        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            if (line.wordIndex() == 0) {
                completeCommand(candidates);
            } else {
                tryCompleteArguments(reader, line, candidates);
            }
        }

        @SuppressWarnings("unchecked")
        protected void tryCompleteArguments(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            String command = line.words().get(0);
            String resolved = environment.resolveCommand(command);
            Map<String, List<CompletionData>> comp = environment.getCompletions();
            if (comp != null) {
                List<CompletionData> cmd = comp.get(resolved);
                if (cmd != null) {
                    completeCommandArguments(reader, line, candidates, cmd);
                }
            }
        }

        @SuppressWarnings("unchecked")
        protected void completeCommandArguments(LineReader reader, ParsedLine line, List<Candidate> candidates, List<CompletionData> completions) {
            for (CompletionData completion : completions) {
                boolean isOption = line.word().startsWith("-");
                String prevOption = line.wordIndex() >= 2 && line.words().get(line.wordIndex() - 1).startsWith("-")
                        ? line.words().get(line.wordIndex() - 1) : null;
                String key = UUID.randomUUID().toString();
                boolean conditionValue = true;
                if (completion.condition != null) {
                    Object res = Boolean.FALSE;
                    try {
                        res = environment.evaluate(reader, line, completion.condition);
                    } catch (Throwable t) {
                        t.getCause();
                        // Ignore
                    }
                    conditionValue = isTrue(res);
                }
                if (conditionValue && isOption && completion.options != null) {
                    for (String opt : completion.options) {
                        candidates.add(new Candidate(opt, opt, "options", completion.description, null, key, true));
                    }
                } else if (!isOption && prevOption != null && completion.argument != null
                        && (completion.options != null && completion.options.contains(prevOption))) {
                    Object res = null;
                    try {
                        res = environment.evaluate(reader, line, completion.argument);
                    } catch (Throwable t) {
                        // Ignore
                    }
                    if (res instanceof Candidate) {
                        candidates.add((Candidate) res);
                    } else if (res instanceof String) {
                        candidates.add(new Candidate((String) res, (String) res, null, null, null, null, true));
                    } else if (res instanceof Collection) {
                        for (Object s : (Collection) res) {
                            if (s instanceof Candidate) {
                                candidates.add((Candidate) s);
                            } else if (s instanceof String) {
                                candidates.add(new Candidate((String) s, (String) s, null, null, null, null, true));
                            }
                        }
                    } else if (res != null && res.getClass().isArray()) {
                        for (int i = 0, l = Array.getLength(res); i < l; i++) {
                            Object s = Array.get(res, i);
                            if (s instanceof Candidate) {
                                candidates.add((Candidate) s);
                            } else if (s instanceof String) {
                                candidates.add(new Candidate((String) s, (String) s, null, null, null, null, true));
                            }
                        }
                    }
                } else if (!isOption && completion.argument != null) {
                    Object res = null;
                    try {
                        res = environment.evaluate(reader, line, completion.argument);
                    } catch (Throwable t) {
                        // Ignore
                    }
                    if (res instanceof Candidate) {
                        candidates.add((Candidate) res);
                    } else if (res instanceof String) {
                        candidates.add(new Candidate((String) res, (String) res, null, completion.description, null, null, true));
                    } else if (res instanceof Collection) {
                        for (Object s : (Collection) res) {
                            if (s instanceof Candidate) {
                                candidates.add((Candidate) s);
                            } else if (s instanceof String) {
                                candidates.add(new Candidate((String) s, (String) s, null, completion.description, null, null, true));
                            }
                        }
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        protected void completeCommand(List<Candidate> candidates) {
            Set<String> commands = environment.getCommands();
            for (String command : commands) {
                String name = environment.commandName(command);
                boolean resolved = command.equals(environment.resolveCommand(name));
                if (!name.startsWith("_")) {
                    String desc = null;
                    Map<String, List<CompletionData>> comp = environment.getCompletions();
                    if (comp != null) {
                        List<CompletionData> completions = comp.get(command);
                        if (completions != null) {
                            for (CompletionData completion : completions) {
                                if (completion.description != null
                                        && completion.options == null
                                        && completion.argument == null
                                        && completion.condition == null) {
                                    desc = completion.description;
                                }
                            }
                        }
                    }
                    String key = UUID.randomUUID().toString();
                    if (desc != null) {
                        candidates.add(new Candidate(command, command, null, desc, null, key, true));
                        if (resolved) {
                            candidates.add(new Candidate(name, name, null, desc, null, key, true));
                        }
                    } else {
                        candidates.add(new Candidate(command, command, null, null, null, key, true));
                        if (resolved) {
                            candidates.add(new Candidate(name, name, null, null, null, key, true));
                        }
                    }
                }
            }
        }

        private boolean isTrue(Object result) {
            if (result == null)
                return false;
            if (result instanceof Boolean)
                return (Boolean) result;
            if (result instanceof Number && 0 == ((Number) result).intValue()) {
                return false;
            }
            return !("".equals(result) || "0".equals(result));

        }

    }

    public static class DirectoriesCompleter extends FileNameCompleter {

        private final Path currentDir;

        public DirectoriesCompleter(File currentDir) {
            this(currentDir.toPath());
        }

        public DirectoriesCompleter(Path currentDir) {
            this.currentDir = currentDir;
        }

        @Override
        protected Path getUserDir() {
            return currentDir;
        }

        @Override
        protected boolean accept(Path path) {
            return Files.isDirectory(path) && super.accept(path);
        }
    }

    public static class FilesCompleter extends FileNameCompleter {

        private final Path currentDir;

        public FilesCompleter(File currentDir) {
            this(currentDir.toPath());
        }

        public FilesCompleter(Path currentDir) {
            this.currentDir = currentDir;
        }

        @Override
        protected Path getUserDir() {
            return currentDir;
        }
    }

    /**
     * A file name completer takes the buffer and issues a list of
     * potential completions.
     * <p/>
     * This completer tries to behave as similar as possible to
     * <i>bash</i>'s file name completion (using GNU readline)
     * with the following exceptions:
     * <p/>
     * <ul>
     * <li>Candidates that are directories will end with "/"</li>
     * <li>Wildcard regular expressions are not evaluated or replaced</li>
     * <li>The "~" character can be used to represent the user's home,
     * but it cannot complete to other users' homes, since java does
     * not provide any way of determining that easily</li>
     * </ul>
     *
     * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
     * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
     * @since 2.3
     */
    public static class FileNameCompleter implements org.jline.reader.Completer
    {

        public void complete(LineReader reader, ParsedLine commandLine, final List<Candidate> candidates) {
            assert commandLine != null;
            assert candidates != null;

            String buffer = commandLine.word().substring(0, commandLine.wordCursor());

            Path current;
            String curBuf;
            int lastSep = buffer.lastIndexOf(File.separator);
            if (lastSep >= 0) {
                curBuf = buffer.substring(0, lastSep + 1);
                if (curBuf.startsWith("~")) {
                    if (curBuf.startsWith("~/")) {
                        current = getUserHome().resolve(curBuf.substring(2));
                    } else {
                        current = getUserHome().getParent().resolve(curBuf.substring(1));
                    }
                } else {
                    current = getUserDir().resolve(curBuf);
                }
            } else {
                curBuf = "";
                current = getUserDir();
            }
            try {
                Files.newDirectoryStream(current, this::accept).forEach(p -> {
                    String value = curBuf + p.getFileName().toString();
                    if (Files.isDirectory(p)) {
                        candidates.add(new Candidate(
                                value + (reader.isSet(LineReader.Option.AUTO_PARAM_SLASH) ? "/" : ""),
                                getDisplay(reader.getTerminal(), p),
                                null, null,
                                reader.isSet(LineReader.Option.AUTO_REMOVE_SLASH) ? "/" : null,
                                null,
                                false));
                    } else {
                        candidates.add(new Candidate(value, getDisplay(reader.getTerminal(), p),
                                null, null, null, null, true));
                    }
                });
            } catch (IOException e) {
                // Ignore
            }
        }

        protected boolean accept(Path path) {
            try {
                return !Files.isHidden(path);
            } catch (IOException e) {
                return false;
            }
        }

        protected Path getUserDir() {
            return Paths.get(System.getProperty("user.dir"));
        }

        protected Path getUserHome() {
            return Paths.get(System.getProperty("user.home"));
        }

        protected String getDisplay(Terminal terminal, Path p) {
            // TODO: use $LS_COLORS for output
            String name = p.getFileName().toString();
            if (Files.isDirectory(p)) {
                AttributedStringBuilder sb = new AttributedStringBuilder();
                sb.styled(AttributedStyle.BOLD.foreground(AttributedStyle.RED), name);
                sb.append("/");
                name = sb.toAnsi(terminal);
            } else if (Files.isSymbolicLink(p)) {
                AttributedStringBuilder sb = new AttributedStringBuilder();
                sb.styled(AttributedStyle.BOLD.foreground(AttributedStyle.RED), name);
                sb.append("@");
                name = sb.toAnsi(terminal);
            }
            return name;
        }

    }

    public static class TreeCompleter implements org.jline.reader.Completer {

        final Map<String, org.jline.reader.Completer> completers = new HashMap<>();
        final RegexCompleter completer;

        public TreeCompleter(Node... nodes) {
            this(Arrays.asList(nodes));
        }

        public TreeCompleter(List<Node> nodes) {
            StringBuilder sb = new StringBuilder();
            addRoots(sb, nodes);
            completer = new RegexCompleter(sb.toString(), completers::get);
        }

        public static Node node(Object... objs) {
            org.jline.reader.Completer comp = null;
            List<Candidate> cands = new ArrayList<>();
            List<Node> nodes = new ArrayList<>();
            for (Object obj : objs) {
                if (obj instanceof String) {
                    cands.add(new Candidate((String) obj));
                } else if (obj instanceof Candidate) {
                    cands.add((Candidate) obj);
                } else if (obj instanceof Node) {
                    nodes.add((Node) obj);
                } else if (obj instanceof org.jline.reader.Completer) {
                    comp = (org.jline.reader.Completer) obj;
                } else {
                    throw new IllegalArgumentException();
                }
            }
            if (comp != null) {
                if (!cands.isEmpty()) {
                    throw new IllegalArgumentException();
                }
                return new Node(comp, nodes);
            } else if (!cands.isEmpty()) {
                return new Node((r, l, c) -> c.addAll(cands), nodes);
            } else {
                throw new IllegalArgumentException();
            }
        }

        void addRoots(StringBuilder sb, List<Node> nodes) {
            if (!nodes.isEmpty()) {
                sb.append(" ( ");
                boolean first = true;
                for (Node n : nodes) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(" | ");
                    }
                    String name = "c" + completers.size();
                    completers.put(name, n.completer);
                    sb.append(name);
                    addRoots(sb, n.nodes);
                }
                sb.append(" ) ");
            }
        }

        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            completer.complete(reader, line, candidates);
        }

        public static class Node {
            final org.jline.reader.Completer completer;
            final List<Node> nodes;

            public Node(org.jline.reader.Completer completer, List<Node> nodes) {
                this.completer = completer;
                this.nodes = nodes;
            }
        }
    }

    public static class RegexCompleter implements org.jline.reader.Completer {

        private final NfaMatcher<String> matcher;
        private final Function<String, org.jline.reader.Completer> completers;
        private final ThreadLocal<LineReader> reader = new ThreadLocal<>();

        public RegexCompleter(String syntax, Function<String, org.jline.reader.Completer> completers) {
            this.matcher = new NfaMatcher<>(syntax, this::doMatch);
            this.completers = completers;
        }

        @Override
        public synchronized void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            List<String> words = line.words().subList(0, line.wordIndex());
            this.reader.set(reader);
            Set<String> next = matcher.matchPartial(words);
            for (String n : next) {
                completers.apply(n).complete(reader, new ArgumentLine(line.word(), line.wordCursor()), candidates);
            }
            this.reader.set(null);
        }

        private boolean doMatch(String arg, String name) {
            List<Candidate> candidates = new ArrayList<>();
            completers.apply(name).complete(this.reader.get(), new ArgumentLine(arg, arg.length()), candidates);
            return candidates.stream().anyMatch(c -> c.value().equals(arg));
        }

        public static class ArgumentLine implements ParsedLine {
            private final String word;
            private final int cursor;

            public ArgumentLine(String word, int cursor) {
                this.word = word;
                this.cursor = cursor;
            }

            @Override
            public String word() {
                return word;
            }

            @Override
            public int wordCursor() {
                return cursor;
            }

            @Override
            public int wordIndex() {
                return 0;
            }

            @Override
            public List<String> words() {
                return Collections.singletonList(word);
            }

            @Override
            public String line() {
                return word;
            }

            @Override
            public int cursor() {
                return cursor;
            }
        }
    }
}
