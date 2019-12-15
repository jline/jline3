/*
 * Copyright (c) 2002-2019, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;
import org.jline.reader.Parser;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.ParsedLine;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
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

        private final Supplier<Path> currentDir;
        private final boolean forceSlash;

        public DirectoriesCompleter(File currentDir) {
            this(currentDir.toPath(), false);
        }

        public DirectoriesCompleter(File currentDir, boolean forceSlash) {
            this(currentDir.toPath(), forceSlash);
        }

        public DirectoriesCompleter(Path currentDir) {
            this(currentDir, false);
        }

        public DirectoriesCompleter(Path currentDir, boolean forceSlash) {
            this.currentDir = () -> currentDir;
            this.forceSlash = forceSlash;
        }

        public DirectoriesCompleter(Supplier<Path> currentDir) {
            this(currentDir, false);
        }

        public DirectoriesCompleter(Supplier<Path> currentDir, boolean forceSlash) {
            this.currentDir = currentDir;
            this.forceSlash = forceSlash;
        }

        @Override
        protected Path getUserDir() {
            return currentDir.get();
        }

        @Override
        protected String getSeparator(boolean useForwardSlash) {
            return forceSlash || useForwardSlash ? "/" : getUserDir().getFileSystem().getSeparator();
        }

        @Override
        protected boolean accept(Path path) {
            return Files.isDirectory(path) && super.accept(path);
        }
    }

    public static class FilesCompleter extends FileNameCompleter {

        private final Supplier<Path> currentDir;
        private final boolean forceSlash;

        public FilesCompleter(File currentDir) {
            this(currentDir.toPath(), false);
        }

        public FilesCompleter(File currentDir, boolean forceSlash) {
            this(currentDir.toPath(), forceSlash);
        }

        public FilesCompleter(Path currentDir) {
            this(currentDir, false);
        }

        public FilesCompleter(Path currentDir, boolean forceSlash) {
            this.currentDir = () -> currentDir;
            this.forceSlash = forceSlash;
        }

        public FilesCompleter(Supplier<Path> currentDir) {
            this(currentDir, false);
        }

        public FilesCompleter(Supplier<Path> currentDir, boolean forceSlash) {
            this.currentDir = currentDir;
            this.forceSlash = forceSlash;
        }

        @Override
        protected Path getUserDir() {
            return currentDir.get();
        }

        @Override
        protected String getSeparator(boolean useForwardSlash) {
            return forceSlash || useForwardSlash ? "/" : getUserDir().getFileSystem().getSeparator();
        }
    }

    /**
     * A file name completer takes the buffer and issues a list of
     * potential completions.
     * <p>
     * This completer tries to behave as similar as possible to
     * <i>bash</i>'s file name completion (using GNU readline)
     * with the following exceptions:
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
            String sep = getSeparator(reader.isSet(LineReader.Option.USE_FORWARD_SLASH));
            int lastSep = buffer.lastIndexOf(sep);
            try {
                if (lastSep >= 0) {
                    curBuf = buffer.substring(0, lastSep + 1);
                    if (curBuf.startsWith("~")) {
                        if (curBuf.startsWith("~" + sep)) {
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
                try (DirectoryStream<Path> directory = Files.newDirectoryStream(current, this::accept)) {
                    directory.forEach(p -> {
                        String value = curBuf + p.getFileName().toString();
                        if (Files.isDirectory(p)) {
                            candidates.add(
                                    new Candidate(value + (reader.isSet(LineReader.Option.AUTO_PARAM_SLASH) ? sep : ""),
                                            getDisplay(reader.getTerminal(), p), null, null,
                                            reader.isSet(LineReader.Option.AUTO_REMOVE_SLASH) ? sep : null, null, false));
                        } else {
                            candidates.add(new Candidate(value, getDisplay(reader.getTerminal(), p), null, null, null, null,
                                    true));
                        }
                    });
                } catch (IOException e) {
                    // Ignore
                }
            } catch (Exception e) {
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

        protected String getSeparator(boolean useForwardSlash) {
            return useForwardSlash ? "/" :getUserDir().getFileSystem().getSeparator();
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
            LineReader r = reader.get();
            boolean caseInsensitive = r != null && r.isSet(Option.CASE_INSENSITIVE);
            completers.apply(name).complete(r, new ArgumentLine(arg, arg.length()), candidates);
            return candidates.stream().anyMatch(c -> caseInsensitive ? c.value().equalsIgnoreCase(arg) : c.value().equals(arg));
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

    public static class SystemCompleter implements org.jline.reader.Completer {
        private Map<String,List<org.jline.reader.Completer>> completers = new HashMap<>();
        private Map<String,String> aliasCommand = new HashMap<>();
        private StringsCompleter commands;
        private boolean compiled = false;

        public SystemCompleter() {}

        @Override
        public void complete(LineReader reader, ParsedLine commandLine, List<Candidate> candidates) {
            if (!compiled) {
                throw new IllegalStateException();
            }
            assert commandLine != null;
            assert candidates != null;
            if (commandLine.words().size() > 0) {
                if (commandLine.words().size() == 1) {
                    String buffer = commandLine.words().get(0);
                    int eq = buffer.indexOf('=');
                    if (eq < 0) {
                        commands.complete(reader, commandLine, candidates);
                    } else {
                        String curBuf = buffer.substring(0, eq + 1);
                        for (String c: completers.keySet()) {
                            candidates.add(new Candidate(AttributedString.stripAnsi(curBuf+c)
                                        , c, null, null, null, null, true));
                        }
                    }
                } else {
                    String cmd = Parser.getCommand(commandLine.words().get(0));
                    if (command(cmd) != null) {
                        completers.get(command(cmd)).get(0).complete(reader, commandLine, candidates);
                    }
                }
            }
        }

        public boolean isCompiled() {
            return compiled;
        }

        private String command(String cmd) {
            String out = null;
            if (completers.containsKey(cmd)) {
                out = cmd;
            } else if (aliasCommand.containsKey(cmd)) {
                out = aliasCommand.get(cmd);
            }
            return out;
        }

        public void add(String command, List<org.jline.reader.Completer> completers) {
            for (org.jline.reader.Completer c : completers) {
                add(command, c);
            }
        }

        public void add(List<String> commands, org.jline.reader.Completer completer) {
            for (String c: commands) {
                add(c, completer);
            }
        }

        public void add(String command, org.jline.reader.Completer completer) {
            if (compiled) {
                throw new IllegalStateException();
            }
            if (!completers.containsKey(command)) {
                completers.put(command, new ArrayList<org.jline.reader.Completer>());
            }
            if (completer instanceof ArgumentCompleter) {
                ((ArgumentCompleter) completer).setStrictCommand(false);
            }
            completers.get(command).add(completer);
        }

        public void add(SystemCompleter other) {
            if (other.isCompiled()) {
                throw new IllegalStateException();
            }
            for (Map.Entry<String, List<org.jline.reader.Completer>> entry: other.getCompleters().entrySet()) {
                for (org.jline.reader.Completer c: entry.getValue()) {
                    add(entry.getKey(), c);
                }
            }
            addAliases(other.getAliases());
        }

        public void addAliases(Map<String,String> aliasCommand) {
            if (compiled) {
                throw new IllegalStateException();
            }
            this.aliasCommand.putAll(aliasCommand);
        }

        public Map<String,String> getAliases() {
            return aliasCommand;
        }

        public void compile() {
            if (compiled) {
                return;
            }
            Map<String, List<org.jline.reader.Completer>> compiledCompleters = new HashMap<>();
            for (Map.Entry<String, List<org.jline.reader.Completer>> entry: completers.entrySet()) {
                if (entry.getValue().size() == 1) {
                    compiledCompleters.put(entry.getKey(), entry.getValue());
                } else {
                    compiledCompleters.put(entry.getKey(), new ArrayList<org.jline.reader.Completer>());
                    compiledCompleters.get(entry.getKey()).add(new AggregateCompleter(entry.getValue()));
                }
            }
            completers = compiledCompleters;
            Set<String> cmds = new HashSet<>(completers.keySet());
            cmds.addAll(aliasCommand.keySet());
            commands = new StringsCompleter(cmds);
            compiled = true;
        }

        public Map<String,List<org.jline.reader.Completer>> getCompleters() {
            return completers;
        }
    }

    public static class OptDesc {
        private String shortOption;
        private String longOption;
        private String description;
        private org.jline.reader.Completer valueCompleter;

        protected static List<OptDesc> compile(Map<String,List<String>> optionValues, Collection<String> options) {
            List<OptDesc> out = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry: optionValues.entrySet()) {
                if (entry.getKey().startsWith("--")) {
                    out.add(new OptDesc(null, entry.getKey(), new StringsCompleter(entry.getValue())));
                } else if (entry.getKey().matches("-[a-zA-Z]{1}")) {
                    out.add(new OptDesc(entry.getKey(), null, new StringsCompleter(entry.getValue())));
                }
            }
            for (String o: options) {
                if (o.startsWith("--")) {
                    out.add(new OptDesc(null, o));
                } else if (o.matches("-[a-zA-Z]{1}")) {
                    out.add(new OptDesc(o, null));
                }
            }
            return out;
        }

        public OptDesc(String shortOption, String longOption, String description, org.jline.reader.Completer valueCompleter) {
            this.shortOption = shortOption;
            this.longOption = longOption;
            this.description = description;
            this.valueCompleter = valueCompleter;
        }

        public OptDesc(String shortOption, String longOption, org.jline.reader.Completer valueCompleter) {
            this(shortOption, longOption, null, valueCompleter);
        }

        public OptDesc(String shortOption, String longOption, String description) {
            this(shortOption, longOption, description, null);
        }

        public OptDesc(String shortOption, String longOption) {
            this(shortOption, longOption, null, null);
        }

        protected OptDesc() {
        }

        public void setValueCompleter(org.jline.reader.Completer valueCompleter) {
            this.valueCompleter = valueCompleter;
        }

        public String longOption() {
            return longOption;
        }

        public String shortOption() {
            return shortOption;
        }

        protected String description() {
            return description;
        }

        protected boolean hasValue() {
            return valueCompleter != null;
        }

        protected org.jline.reader.Completer valueCompleter() {
            return valueCompleter;
        }

        protected void completeOption(LineReader reader, final ParsedLine commandLine, List<Candidate> candidates, boolean longOpt) {
            if (!longOpt) {
                if (shortOption != null) {
                    candidates.add(new Candidate(shortOption, shortOption, null, description, null, null, false));
                }
            } else if (longOption != null) {
                if (hasValue()) {
                    candidates.add(new Candidate(longOption + "=", longOption, null, description, null, null, false));
                } else {
                    candidates.add(new Candidate(longOption, longOption, null, description, null, null, true));
                }
            }
        }

        protected boolean completeValue(LineReader reader, final ParsedLine commandLine, List<Candidate> candidates, String curBuf, String partialValue) {
            boolean out = false;
            List<Candidate> temp = new ArrayList<>();
            valueCompleter.complete(reader, commandLine, temp);
            for (Candidate c : temp) {
                String v = c.value();
                if (v.startsWith(partialValue)) {
                    out = true;
                }
                candidates.add(new Candidate(curBuf + v, v, null, null, null, null, true));
            }
            return out;
        }

        protected boolean match(String option) {
            return (shortOption != null && shortOption.equals(option)) || (longOption != null && longOption.equals(option));
        }

        protected boolean startsWith(String option) {
            return (shortOption != null && shortOption.startsWith(option))
                    || (longOption != null && longOption.startsWith(option));
        }
    }

    public static class OptionCompleter implements org.jline.reader.Completer {
        private Function<String,Collection<OptDesc>> commandOptions;
        private Collection<OptDesc> options;
        private List<org.jline.reader.Completer> argsCompleters = new ArrayList<>();
        private int startPos;

        public OptionCompleter(org.jline.reader.Completer completer, Function<String,Collection<OptDesc>> commandOptions, int startPos) {
            this.startPos = startPos;
            this.commandOptions = commandOptions;
            this.argsCompleters.add(completer);
        }

        public OptionCompleter(List<org.jline.reader.Completer> completers, Function<String,Collection<OptDesc>> commandOptions, int startPos) {
            this.startPos = startPos;
            this.commandOptions = commandOptions;
            this.argsCompleters = new ArrayList<>(completers);
        }

        public OptionCompleter(List<org.jline.reader.Completer> completers, Map<String,List<String>> optionValues, Collection<String> options, int startPos) {
            this(optionValues, options, startPos);
            this.argsCompleters = new ArrayList<>(completers);
        }

        public OptionCompleter(org.jline.reader.Completer completer, Map<String,List<String>> optionValues, Collection<String> options, int startPos) {
            this(optionValues, options, startPos);
            this.argsCompleters.add(completer);
        }

        public OptionCompleter(Map<String,List<String>> optionValues, Collection<String> options, int startPos) {
            this(OptDesc.compile(optionValues, options), startPos);
        }

        public OptionCompleter(org.jline.reader.Completer completer, Collection<OptDesc> options, int startPos) {
            this(options, startPos);
            this.argsCompleters.add(completer);
        }

        public OptionCompleter(List<org.jline.reader.Completer> completers, Collection<OptDesc> options, int startPos) {
            this(options, startPos);
            this.argsCompleters = new ArrayList<>(completers);
        }

        public OptionCompleter(Collection<OptDesc> options, int startPos) {
            this.options = options;
            this.startPos = startPos;
        }

        @Override
        public void complete(LineReader reader, final ParsedLine commandLine, List<Candidate> candidates) {
            assert commandLine != null;
            assert candidates != null;
            List<String> words = commandLine.words();
            String buffer = commandLine.word().substring(0, commandLine.wordCursor());
            if (startPos >= words.size()) {
                candidates.add(new Candidate(buffer, buffer, null, null, null, null, true));
                return;
            }
            String command = Parser.getCommand(words.get(0));
            if (buffer.startsWith("-")) {
                boolean addbuff = true;
                boolean valueCandidates = false;
                boolean longOption = buffer.startsWith("--");
                int eq = buffer.matches("-[a-zA-Z]{1}[a-zA-Z0-9]+") ? 2 : buffer.indexOf('=');
                if (eq < 0) {
                    List<String> usedOptions = new ArrayList<>();
                    for (int i = startPos; i < words.size(); i++) {
                        if (words.get(i).startsWith("-")) {
                            String w = words.get(i);
                            int ind = w.indexOf('=');
                            if (ind < 0) {
                                usedOptions.add(w);
                            } else {
                                usedOptions.add(w.substring(0,ind));
                            }
                        }
                    }
                    for (OptDesc o : commandOptions == null ? options : commandOptions.apply(command)) {
                        if (usedOptions.contains(o.shortOption()) || usedOptions.contains(o.longOption())) {
                            continue;
                        }
                        if (o.startsWith(buffer)) {
                            addbuff = false;
                        }
                        o.completeOption(reader, commandLine, candidates, longOption);
                    }
                } else {
                    addbuff = false;
                    int nb = buffer.contains("=") ? 1 : 0;
                    String value = buffer.substring(eq + nb);
                    String curBuf = buffer.substring(0, eq + nb);
                    String opt = buffer.substring(0, eq);
                    OptDesc option = findOptDesc(command, opt);
                    if (option.hasValue()) {
                        valueCandidates = option.completeValue(reader, commandLine, candidates, curBuf, value);
                    }
                }
                if ((buffer.contains("=") && !buffer.endsWith("=") && !valueCandidates) || addbuff) {
                    candidates.add(new Candidate(buffer, buffer, null, null, null, null, true));
                }
            } else if (words.size() > 1 && shortOptionValueCompleter(command, words.get(words.size() - 2)) != null) {
                shortOptionValueCompleter(command, words.get(words.size() - 2)).complete(reader, commandLine, candidates);
            } else if (!argsCompleters.isEmpty()) {
                int args = -1;
                for (int i = startPos; i < words.size(); i++) {
                    if (!words.get(i).startsWith("-")) {
                        if (i > 0 && shortOptionValueCompleter(command, words.get(i - 1)) == null) {
                            args++;
                        }
                    }
                }
                if (args == -1) {
                    candidates.add(new Candidate(buffer, buffer, null, null, null, null, true));
                } else if (args < argsCompleters.size()) {
                    argsCompleters.get(args).complete(reader, commandLine, candidates);
                } else {
                    argsCompleters.get(argsCompleters.size() - 1).complete(reader, commandLine, candidates);
                }
            }
        }

        private org.jline.reader.Completer shortOptionValueCompleter(String command, String opt) {
            if (!opt.matches("-[a-zA-Z]+")) {
                return null;
            }
            org.jline.reader.Completer out = null;
            Collection<OptDesc> optDescs = commandOptions == null ? options : commandOptions.apply(command);
            if (opt.length() == 2) {
                out = findOptDesc(optDescs, opt).valueCompleter();
            } else if (opt.length() > 2) {
                for (int i = 1; i < opt.length(); i++) {
                    OptDesc o = findOptDesc(optDescs, "-" + opt.charAt(i));
                    if (o.shortOption() == null) {
                        return null;
                    } else if (out == null) {
                        out = o.valueCompleter();
                    }
                }
            }
            return out;
        }

        private OptDesc findOptDesc(String command, String opt) {
            return findOptDesc(commandOptions == null ? options : commandOptions.apply(command), opt);
        }

        private OptDesc findOptDesc(Collection<OptDesc> optDescs, String opt) {
            for (OptDesc o : optDescs) {
                if (o.match(opt)) {
                    return o;
                }
            }
            return new OptDesc();
        }
    }
}
