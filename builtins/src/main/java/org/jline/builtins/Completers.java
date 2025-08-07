/*
 * Copyright (c) 2002-2025, the original author(s).
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.OSUtils;
import org.jline.utils.StyleResolver;

/**
 * Provides a collection of completion-related classes and utilities for JLine applications.
 * <p>
 * This class contains various completer implementations that can be used to provide
 * tab-completion functionality in command-line applications. These include:
 * </p>
 * <ul>
 *   <li>Command and argument completion</li>
 *   <li>File and directory name completion</li>
 *   <li>Tree-based completion</li>
 *   <li>Regular expression-based completion</li>
 *   <li>Command option completion</li>
 * </ul>
 * <p>
 * These completers can be combined and configured to provide sophisticated completion
 * behavior for command-line interfaces.
 * </p>
 */
public class Completers {

    /**
     * Creates a new Completers.
     */
    public Completers() {
        // Default constructor
    }

    /**
     * Interface defining the environment for command completion.
     * <p>
     * This interface provides methods to access command information and evaluate
     * completion expressions in a specific environment context.
     * </p>
     */
    public interface CompletionEnvironment {
        /**
         * Gets the available completions for commands.
         *
         * @return a map of command names to their completion data
         */
        Map<String, List<CompletionData>> getCompletions();

        /**
         * Gets the set of available command names.
         *
         * @return a set of command names
         */
        Set<String> getCommands();

        /**
         * Resolves a command name to its canonical form.
         *
         * @param command the command name to resolve
         * @return the resolved command name
         */
        String resolveCommand(String command);

        /**
         * Gets the display name for a command.
         *
         * @param command the command to get the name for
         * @return the display name of the command
         */
        String commandName(String command);

        /**
         * Evaluates a function in the current environment context.
         *
         * @param reader the line reader
         * @param line the parsed command line
         * @param func the function to evaluate
         * @return the result of the evaluation
         * @throws Exception if an error occurs during evaluation
         */
        Object evaluate(LineReader reader, ParsedLine line, String func) throws Exception;
    }

    /**
     * Holds data for command completion.
     * <p>
     * This class stores information about command options, descriptions, arguments,
     * and conditions used for command completion.
     * </p>
     */
    public static class CompletionData {
        /** The list of command options */
        public final List<String> options;
        /** The description of the command or option */
        public final String description;
        /** The argument specification for completion */
        public final String argument;
        /** The condition that must be satisfied for this completion to be applicable */
        public final String condition;

        /**
         * Creates a new CompletionData instance.
         *
         * @param options the list of command options
         * @param description the description of the command or option
         * @param argument the argument specification for completion
         * @param condition the condition that must be satisfied for this completion to be applicable
         */
        public CompletionData(List<String> options, String description, String argument, String condition) {
            this.options = options;
            this.description = description;
            this.argument = argument;
            this.condition = condition;
        }
    }

    /**
     * A completer implementation that provides command and argument completion.
     * <p>
     * This completer uses a CompletionEnvironment to provide context-aware completion
     * for commands and their arguments.
     * </p>
     */
    public static class Completer implements org.jline.reader.Completer {

        private final CompletionEnvironment environment;

        /**
         * Creates a new Completer with the specified environment.
         *
         * @param environment the completion environment to use
         */
        public Completer(CompletionEnvironment environment) {
            this.environment = environment;
        }

        /**
         * Completes the current input line.
         * <p>
         * If the cursor is at the first word, completes command names.
         * Otherwise, tries to complete command arguments.
         * </p>
         *
         * @param reader the line reader
         * @param line the parsed command line
         * @param candidates the list to add completion candidates to
         */
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            if (line.wordIndex() == 0) {
                completeCommand(candidates);
            } else {
                tryCompleteArguments(reader, line, candidates);
            }
        }

        /**
         * Attempts to complete command arguments.
         * <p>
         * Retrieves completion data for the command and delegates to completeCommandArguments
         * if completion data is available.
         * </p>
         *
         * @param reader the line reader
         * @param line the parsed command line
         * @param candidates the list to add completion candidates to
         */
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

        /**
         * Completes command arguments based on completion data.
         * <p>
         * Processes each completion data entry and adds appropriate candidates based on
         * the current command line state and completion conditions.
         * </p>
         *
         * @param reader the line reader
         * @param line the parsed command line
         * @param candidates the list to add completion candidates to
         * @param completions the list of completion data to process
         */
        protected void completeCommandArguments(
                LineReader reader, ParsedLine line, List<Candidate> candidates, List<CompletionData> completions) {
            for (CompletionData completion : completions) {
                boolean isOption = line.word().startsWith("-");
                String prevOption = line.wordIndex() >= 2
                                && line.words().get(line.wordIndex() - 1).startsWith("-")
                        ? line.words().get(line.wordIndex() - 1)
                        : null;
                String key = UUID.randomUUID().toString();
                boolean conditionValue = true;
                if (completion.condition != null) {
                    Object res = Boolean.FALSE;
                    try {
                        res = environment.evaluate(reader, line, completion.condition);
                    } catch (Throwable t) {
                        // Ignore
                    }
                    conditionValue = isTrue(res);
                }
                if (conditionValue && isOption && completion.options != null) {
                    for (String opt : completion.options) {
                        candidates.add(new Candidate(opt, opt, "options", completion.description, null, key, true));
                    }
                } else if (!isOption
                        && prevOption != null
                        && completion.argument != null
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
                        for (Object s : (Collection<?>) res) {
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
                        candidates.add(new Candidate(
                                (String) res, (String) res, null, completion.description, null, null, true));
                    } else if (res instanceof Collection) {
                        for (Object s : (Collection<?>) res) {
                            if (s instanceof Candidate) {
                                candidates.add((Candidate) s);
                            } else if (s instanceof String) {
                                candidates.add(new Candidate(
                                        (String) s, (String) s, null, completion.description, null, null, true));
                            }
                        }
                    }
                }
            }
        }

        /**
         * Completes command names.
         * <p>
         * Adds completion candidates for all available commands, including their
         * descriptions if available.
         * </p>
         *
         * @param candidates the list to add completion candidates to
         */
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

        /**
         * Determines if an object represents a true value.
         * <p>
         * Used for evaluating condition results in completion data.
         * </p>
         *
         * @param result the object to evaluate
         * @return true if the object represents a true value, false otherwise
         */
        private boolean isTrue(Object result) {
            if (result == null) return false;
            if (result instanceof Boolean) return (Boolean) result;
            if (result instanceof Number && 0 == ((Number) result).intValue()) {
                return false;
            }
            return !("".equals(result) || "0".equals(result));
        }
    }

    /**
     * A completer for directory names.
     * <p>
     * This completer provides completion for directory paths, filtering out non-directory entries.
     * It extends FileNameCompleter and overrides the accept method to only accept directories.
     * </p>
     */
    public static class DirectoriesCompleter extends FileNameCompleter {

        /** The supplier for the current directory path */
        private final Supplier<Path> currentDir;

        /**
         * Creates a new DirectoriesCompleter with the specified current directory.
         *
         * @param currentDir the current directory as a File
         */
        public DirectoriesCompleter(File currentDir) {
            this(currentDir.toPath());
        }

        /**
         * Creates a new DirectoriesCompleter with the specified current directory.
         *
         * @param currentDir the current directory as a Path
         */
        public DirectoriesCompleter(Path currentDir) {
            this.currentDir = () -> currentDir;
        }

        /**
         * Creates a new DirectoriesCompleter with a supplier for the current directory.
         *
         * @param currentDir a supplier that provides the current directory path
         */
        public DirectoriesCompleter(Supplier<Path> currentDir) {
            this.currentDir = currentDir;
        }

        /**
         * Gets the user's current directory.
         *
         * @return the current directory path
         */
        @Override
        protected Path getUserDir() {
            return currentDir.get();
        }

        /**
         * Determines if a path should be accepted for completion.
         * <p>
         * Only accepts directories that also pass the parent class's accept method.
         * </p>
         *
         * @param path the path to check
         * @return true if the path should be accepted, false otherwise
         */
        @Override
        protected boolean accept(Path path) {
            return Files.isDirectory(path) && super.accept(path);
        }
    }

    /**
     * A completer for file names.
     * <p>
     * This completer provides completion for file paths, with optional filtering by name pattern.
     * It extends FileNameCompleter and overrides the accept method to filter files by pattern.
     * </p>
     */
    public static class FilesCompleter extends FileNameCompleter {

        /** The supplier for the current directory path */
        private final Supplier<Path> currentDir;
        /** The compiled pattern for filtering file names */
        private final String namePattern;

        /**
         * Creates a new FilesCompleter with the specified current directory.
         *
         * @param currentDir the current directory as a File
         */
        public FilesCompleter(File currentDir) {
            this(currentDir.toPath(), null);
        }

        /**
         * Creates a new FilesCompleter with the specified current directory and name pattern.
         *
         * @param currentDir the current directory as a File
         * @param namePattern the pattern to filter file names
         */
        public FilesCompleter(File currentDir, String namePattern) {
            this(currentDir.toPath(), namePattern);
        }

        /**
         * Creates a new FilesCompleter with the specified current directory.
         *
         * @param currentDir the current directory as a Path
         */
        public FilesCompleter(Path currentDir) {
            this(currentDir, null);
        }

        /**
         * Creates a new FilesCompleter with the specified current directory and name pattern.
         *
         * @param currentDir the current directory as a Path
         * @param namePattern the pattern to filter file names
         */
        public FilesCompleter(Path currentDir, String namePattern) {
            this.currentDir = () -> currentDir;
            this.namePattern = compilePattern(namePattern);
        }

        /**
         * Creates a new FilesCompleter with a supplier for the current directory.
         *
         * @param currentDir a supplier that provides the current directory path
         */
        public FilesCompleter(Supplier<Path> currentDir) {
            this(currentDir, null);
        }

        /**
         * Creates a new FilesCompleter with a supplier for the current directory and name pattern.
         *
         * @param currentDir a supplier that provides the current directory path
         * @param namePattern the pattern to filter file names
         */
        public FilesCompleter(Supplier<Path> currentDir, String namePattern) {
            this.currentDir = currentDir;
            this.namePattern = compilePattern(namePattern);
        }

        /**
         * Compiles a file name pattern into a regular expression.
         * <p>
         * Converts glob-like patterns to regular expressions by escaping dots,
         * converting asterisks to .*, and handling backslash escapes.
         * </p>
         *
         * @param pattern the pattern to compile
         * @return the compiled pattern as a regular expression, or null if the input pattern is null
         */
        private String compilePattern(String pattern) {
            if (pattern == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < pattern.length(); i++) {
                char ch = pattern.charAt(i);
                if (ch == '\\') {
                    ch = pattern.charAt(++i);
                    sb.append(ch);
                } else if (ch == '.') {
                    sb.append('\\').append('.');
                } else if (ch == '*') {
                    sb.append('.').append('*');
                } else {
                    sb.append(ch);
                }
            }
            return sb.toString();
        }

        /**
         * Gets the user's current directory.
         *
         * @return the current directory path
         */
        @Override
        protected Path getUserDir() {
            return currentDir.get();
        }

        /**
         * Determines if a path should be accepted for completion.
         * <p>
         * Accepts directories unconditionally, and files that match the name pattern if one is specified.
         * </p>
         *
         * @param path the path to check
         * @return true if the path should be accepted, false otherwise
         */
        @Override
        protected boolean accept(Path path) {
            if (namePattern == null || Files.isDirectory(path)) {
                return super.accept(path);
            }
            return path.getFileName().toString().matches(namePattern) && super.accept(path);
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
    public static class FileNameCompleter implements org.jline.reader.Completer {

        public FileNameCompleter() {}

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
                StyleResolver resolver = Styles.lsStyle();
                try (DirectoryStream<Path> directory = Files.newDirectoryStream(current, this::accept)) {
                    directory.forEach(p -> {
                        String value = curBuf + p.getFileName().toString();
                        if (Files.isDirectory(p)) {
                            candidates.add(new Candidate(
                                    value + (reader.isSet(LineReader.Option.AUTO_PARAM_SLASH) ? sep : ""),
                                    getDisplay(reader.getTerminal(), p, resolver, sep),
                                    null,
                                    null,
                                    reader.isSet(LineReader.Option.AUTO_REMOVE_SLASH) ? sep : null,
                                    null,
                                    false));
                        } else {
                            candidates.add(new Candidate(
                                    value,
                                    getDisplay(reader.getTerminal(), p, resolver, sep),
                                    null,
                                    null,
                                    null,
                                    null,
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
            return useForwardSlash ? "/" : getUserDir().getFileSystem().getSeparator();
        }

        protected String getDisplay(Terminal terminal, Path p, StyleResolver resolver, String separator) {
            AttributedStringBuilder sb = new AttributedStringBuilder();
            String name = p.getFileName().toString();
            int idx = name.lastIndexOf(".");
            String type = idx != -1 ? ".*" + name.substring(idx) : null;
            if (Files.isSymbolicLink(p)) {
                sb.styled(resolver.resolve(".ln"), name).append("@");
            } else if (Files.isDirectory(p)) {
                sb.styled(resolver.resolve(".di"), name).append(separator);
            } else if (Files.isExecutable(p) && !OSUtils.IS_WINDOWS) {
                sb.styled(resolver.resolve(".ex"), name).append("*");
            } else if (type != null && resolver.resolve(type).getStyle() != 0) {
                sb.styled(resolver.resolve(type), name);
            } else if (Files.isRegularFile(p)) {
                sb.styled(resolver.resolve(".fi"), name);
            } else {
                sb.append(name);
            }
            return sb.toAnsi(terminal);
        }
    }

    /**
     * A completer that supports hierarchical command structures.
     * <p>
     * This completer allows defining a tree of command nodes, where each node can have
     * its own completer and child nodes. It's useful for implementing command hierarchies
     * where different completions are available at different levels of the command structure.
     * </p>
     */
    public static class TreeCompleter implements org.jline.reader.Completer {

        /** Map of completer names to completer instances */
        final Map<String, org.jline.reader.Completer> completers = new HashMap<>();
        /** The regex completer that handles the tree structure */
        final RegexCompleter completer;

        /**
         * Creates a new TreeCompleter with the specified nodes.
         *
         * @param nodes the root nodes of the completion tree
         */
        public TreeCompleter(Node... nodes) {
            this(Arrays.asList(nodes));
        }

        /**
         * Creates a new TreeCompleter with the specified list of nodes.
         *
         * @param nodes the list of root nodes of the completion tree
         */
        @SuppressWarnings("this-escape")
        public TreeCompleter(List<Node> nodes) {
            StringBuilder sb = new StringBuilder();
            addRoots(sb, nodes);
            completer = new RegexCompleter(sb.toString(), completers::get);
        }

        /**
         * Creates a new node for the completion tree.
         * <p>
         * This method accepts various types of objects and constructs a node based on their types:
         * <ul>
         *   <li>String objects are converted to Candidate objects</li>
         *   <li>Candidate objects are used directly</li>
         *   <li>Node objects are added as child nodes</li>
         *   <li>Completer objects are used as the node's completer</li>
         * </ul>
         *
         * @param objs the objects to include in the node
         * @return a new Node instance
         * @throws IllegalArgumentException if the objects cannot form a valid node
         */
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

        /**
         * Adds root nodes to the regex pattern for the completer.
         * <p>
         * This method recursively builds a regex pattern that represents the tree structure
         * of completers, adding each node's completer to the completers map with a unique name.
         * </p>
         *
         * @param sb the StringBuilder to append the pattern to
         * @param nodes the list of nodes to add
         */
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

        /**
         * Completes the current input line using the tree structure.
         * <p>
         * Delegates to the RegexCompleter which handles the tree structure.
         * </p>
         *
         * @param reader the line reader
         * @param line the parsed command line
         * @param candidates the list to add completion candidates to
         */
        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            completer.complete(reader, line, candidates);
        }

        /**
         * Represents a node in the completion tree.
         * <p>
         * Each node has a completer for the current level and a list of child nodes
         * for the next level in the command hierarchy.
         * </p>
         */
        public static class Node {
            /** The completer for this node */
            final org.jline.reader.Completer completer;
            /** The list of child nodes */
            final List<Node> nodes;

            /**
             * Creates a new Node with the specified completer and child nodes.
             *
             * @param completer the completer for this node
             * @param nodes the list of child nodes
             */
            public Node(org.jline.reader.Completer completer, List<Node> nodes) {
                this.completer = completer;
                this.nodes = nodes;
            }
        }
    }

    /**
     * A completer that uses regular expressions to match command patterns.
     * <p>
     * This completer uses a non-deterministic finite automaton (NFA) to match
     * command patterns and provide appropriate completions based on the current
     * state of the command line.
     * </p>
     */
    public static class RegexCompleter implements org.jline.reader.Completer {

        /** The NFA matcher for command patterns */
        private final NfaMatcher<String> matcher;
        /** Function to get completers by name */
        private final Function<String, org.jline.reader.Completer> completers;
        /** Thread-local storage for the current line reader */
        private final ThreadLocal<LineReader> reader = new ThreadLocal<>();

        /**
         * Creates a new RegexCompleter with the specified syntax and completers.
         *
         * @param syntax the regular expression syntax for command patterns
         * @param completers a function that provides completers by name
         */
        public RegexCompleter(String syntax, Function<String, org.jline.reader.Completer> completers) {
            this.matcher = new NfaMatcher<>(syntax, this::doMatch);
            this.completers = completers;
        }

        /**
         * Completes the current input line using the regex pattern.
         * <p>
         * Finds all possible next states in the NFA and applies the corresponding
         * completers to generate completion candidates.
         * </p>
         *
         * @param reader the line reader
         * @param line the parsed command line
         * @param candidates the list to add completion candidates to
         */
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

        /**
         * Determines if an argument matches a completer name.
         * <p>
         * Used by the NFA matcher to check if a word matches a specific completer.
         * </p>
         *
         * @param arg the argument to check
         * @param name the name of the completer to use
         * @return true if the argument matches a completion from the named completer
         */
        private boolean doMatch(String arg, String name) {
            List<Candidate> candidates = new ArrayList<>();
            LineReader r = reader.get();
            boolean caseInsensitive = r != null && r.isSet(Option.CASE_INSENSITIVE);
            completers.apply(name).complete(r, new ArgumentLine(arg, arg.length()), candidates);
            return candidates.stream()
                    .anyMatch(c -> caseInsensitive
                            ? c.value().equalsIgnoreCase(arg)
                            : c.value().equals(arg));
        }

        /**
         * A simple implementation of ParsedLine for argument completion.
         * <p>
         * This class represents a single word with a cursor position, used for
         * completing arguments in the RegexCompleter.
         * </p>
         */
        public static class ArgumentLine implements ParsedLine {
            /** The current word being completed */
            private final String word;
            /** The cursor position within the word */
            private final int cursor;

            /**
             * Creates a new ArgumentLine with the specified word and cursor position.
             *
             * @param word the word being completed
             * @param cursor the cursor position within the word
             */
            public ArgumentLine(String word, int cursor) {
                this.word = word;
                this.cursor = cursor;
            }

            /**
             * Gets the current word.
             *
             * @return the current word
             */
            @Override
            public String word() {
                return word;
            }

            /**
             * Gets the cursor position within the current word.
             *
             * @return the cursor position
             */
            @Override
            public int wordCursor() {
                return cursor;
            }

            /**
             * Gets the index of the current word, which is always 0 for ArgumentLine.
             *
             * @return always returns 0
             */
            @Override
            public int wordIndex() {
                return 0;
            }

            /**
             * Gets the list of words, which contains only the current word.
             *
             * @return a singleton list containing the current word
             */
            @Override
            public List<String> words() {
                return Collections.singletonList(word);
            }

            /**
             * Gets the full line, which is the same as the current word.
             *
             * @return the current word
             */
            @Override
            public String line() {
                return word;
            }

            /**
             * Gets the cursor position within the line.
             *
             * @return the cursor position
             */
            @Override
            public int cursor() {
                return cursor;
            }
        }
    }

    /**
     * Describes a command-line option for completion.
     * <p>
     * This class holds information about command options, including short and long forms,
     * descriptions, and value completers for options that take arguments.
     * </p>
     */
    public static class OptDesc {
        /** The short form of the option (e.g., "-a") */
        private String shortOption;
        /** The long form of the option (e.g., "--all") */
        private String longOption;
        /** The description of the option */
        private String description;
        /** The completer for option values, or null if the option doesn't take a value */
        private org.jline.reader.Completer valueCompleter;

        /**
         * Compiles a list of OptDesc objects from option values and options.
         * <p>
         * Creates OptDesc objects for both options with values and options without values.
         * </p>
         *
         * @param optionValues a map of option names to their possible values
         * @param options a collection of option names that don't take values
         * @return a list of OptDesc objects
         */
        protected static List<OptDesc> compile(Map<String, List<String>> optionValues, Collection<String> options) {
            List<OptDesc> out = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : optionValues.entrySet()) {
                if (entry.getKey().startsWith("--")) {
                    out.add(new OptDesc(null, entry.getKey(), new StringsCompleter(entry.getValue())));
                } else if (entry.getKey().matches("-[a-zA-Z]")) {
                    out.add(new OptDesc(entry.getKey(), null, new StringsCompleter(entry.getValue())));
                }
            }
            for (String o : options) {
                if (o.startsWith("--")) {
                    out.add(new OptDesc(null, o));
                } else if (o.matches("-[a-zA-Z]")) {
                    out.add(new OptDesc(o, null));
                }
            }
            return out;
        }

        /**
         * Command option description. If option does not have short/long option assign to it null value.
         * If option does not have value set valueCompleter = NullCompleter.INSTANCE
         * @param shortOption short option
         * @param longOption  long option
         * @param description short option description
         * @param valueCompleter option value completer
         */
        public OptDesc(
                String shortOption, String longOption, String description, org.jline.reader.Completer valueCompleter) {
            this.shortOption = shortOption;
            this.longOption = longOption;
            this.description = description;
            this.valueCompleter = valueCompleter;
        }

        /**
         * Command option description. If option does not have short/long option assign to it null value.
         * If option does not have value set valueCompleter = NullCompleter.INSTANCE
         * @param shortOption short option
         * @param longOption long option
         * @param valueCompleter option value completer
         */
        public OptDesc(String shortOption, String longOption, org.jline.reader.Completer valueCompleter) {
            this(shortOption, longOption, null, valueCompleter);
        }

        /**
         * Command option description. If option does not have short/long option assign to it null value.
         * @param shortOption short option
         * @param longOption long option
         * @param description short option description
         */
        public OptDesc(String shortOption, String longOption, String description) {
            this(shortOption, longOption, description, null);
        }

        /**
         * Command option description. If option does not have short/long option assign to it null value.
         * @param shortOption short option
         * @param longOption long option
         */
        public OptDesc(String shortOption, String longOption) {
            this(shortOption, longOption, null, null);
        }

        /**
         * Protected default constructor for subclasses.
         */
        protected OptDesc() {}

        /**
         * Sets the value completer for this option.
         *
         * @param valueCompleter the completer for option values
         */
        public void setValueCompleter(org.jline.reader.Completer valueCompleter) {
            this.valueCompleter = valueCompleter;
        }

        /**
         * Gets the long form of the option.
         *
         * @return the long option string, or null if not set
         */
        public String longOption() {
            return longOption;
        }

        /**
         * Gets the short form of the option.
         *
         * @return the short option string, or null if not set
         */
        public String shortOption() {
            return shortOption;
        }

        /**
         * Gets the description of the option.
         *
         * @return the option description, or null if not set
         */
        public String description() {
            return description;
        }

        /**
         * Determines if this option takes a value.
         *
         * @return true if the option takes a value, false otherwise
         */
        protected boolean hasValue() {
            return valueCompleter != null && valueCompleter != NullCompleter.INSTANCE;
        }

        /**
         * Gets the value completer for this option.
         *
         * @return the value completer, or null if not set
         */
        protected org.jline.reader.Completer valueCompleter() {
            return valueCompleter;
        }

        /**
         * Completes an option based on whether it's a short or long option.
         * <p>
         * Adds appropriate candidates for the option based on its type and whether it takes a value.
         * </p>
         *
         * @param reader the line reader
         * @param commandLine the parsed command line
         * @param candidates the list to add completion candidates to
         * @param longOpt true if completing long options, false for short options
         */
        protected void completeOption(
                LineReader reader, final ParsedLine commandLine, List<Candidate> candidates, boolean longOpt) {
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

        /**
         * Completes the value for an option.
         * <p>
         * Uses the option's value completer to generate completion candidates for the option value.
         * </p>
         *
         * @param reader the line reader
         * @param commandLine the parsed command line
         * @param candidates the list to add completion candidates to
         * @param curBuf the current buffer up to the cursor
         * @param partialValue the partial value to complete
         * @return true if any candidates were added, false otherwise
         */
        protected boolean completeValue(
                LineReader reader,
                final ParsedLine commandLine,
                List<Candidate> candidates,
                String curBuf,
                String partialValue) {
            boolean out = false;
            List<Candidate> temp = new ArrayList<>();
            ParsedLine pl = reader.getParser().parse(partialValue, partialValue.length());
            valueCompleter.complete(reader, pl, temp);
            for (Candidate c : temp) {
                String v = c.value();
                if (v.startsWith(partialValue)) {
                    out = true;
                    String val = c.value();
                    if (valueCompleter instanceof FileNameCompleter) {
                        FileNameCompleter cc = (FileNameCompleter) valueCompleter;
                        String sep = cc.getSeparator(reader.isSet(LineReader.Option.USE_FORWARD_SLASH));
                        val = cc.getDisplay(reader.getTerminal(), Paths.get(c.value()), Styles.lsStyle(), sep);
                    }
                    candidates.add(new Candidate(curBuf + v, val, null, null, null, null, c.complete()));
                }
            }
            return out;
        }

        /**
         * Determines if this option matches the specified option string.
         *
         * @param option the option string to check
         * @return true if this option matches the specified string, false otherwise
         */
        protected boolean match(String option) {
            return (shortOption != null && shortOption.equals(option))
                    || (longOption != null && longOption.equals(option));
        }

        /**
         * Determines if this option starts with the specified prefix.
         *
         * @param option the prefix to check
         * @return true if this option starts with the specified prefix, false otherwise
         */
        protected boolean startsWith(String option) {
            return (shortOption != null && shortOption.startsWith(option))
                    || (longOption != null && longOption.startsWith(option));
        }
    }

    /**
     * A completer for command options and arguments.
     * <p>
     * This completer handles completion for command options (both short and long forms)
     * and their values, as well as command arguments. It's designed to be used as part
     * of an ArgumentCompleter for a complete command-line interface.
     * </p>
     */
    public static class OptionCompleter implements org.jline.reader.Completer {
        /** Function to get option descriptions for a command */
        private Function<String, Collection<OptDesc>> commandOptions;
        /** Collection of option descriptions */
        private Collection<OptDesc> options;
        /** List of completers for command arguments */
        private List<org.jline.reader.Completer> argsCompleters = new ArrayList<>();
        /** The position of this completer in the argument list */
        private int startPos;

        /**
         * OptionCompleter completes command options and parameters. OptionCompleter should be used as an argument of ArgumentCompleter
         * @param completer command parameter completer
         * @param commandOptions command options descriptions
         * @param startPos OptionCompleter position in ArgumentCompleter parameters
         */
        public OptionCompleter(
                org.jline.reader.Completer completer,
                Function<String, Collection<OptDesc>> commandOptions,
                int startPos) {
            this.startPos = startPos;
            this.commandOptions = commandOptions;
            this.argsCompleters.add(completer);
        }

        /**
         * OptionCompleter completes command options and parameters. OptionCompleter should be used as an argument of ArgumentCompleter
         * @param completers command parameters completers
         * @param commandOptions command options descriptions
         * @param startPos OptionCompleter position in ArgumentCompleter parameters
         */
        public OptionCompleter(
                List<org.jline.reader.Completer> completers,
                Function<String, Collection<OptDesc>> commandOptions,
                int startPos) {
            this.startPos = startPos;
            this.commandOptions = commandOptions;
            this.argsCompleters = new ArrayList<>(completers);
        }

        /**
         * OptionCompleter completes command options and parameters. OptionCompleter should be used as an argument of ArgumentCompleter
         * @param completers command parameters completers
         * @param optionValues command value options as map key and its possible values as map value
         * @param options command options that do not have value
         * @param startPos OptionCompleter position in ArgumentCompleter parameters
         */
        public OptionCompleter(
                List<org.jline.reader.Completer> completers,
                Map<String, List<String>> optionValues,
                Collection<String> options,
                int startPos) {
            this(optionValues, options, startPos);
            this.argsCompleters = new ArrayList<>(completers);
        }

        /**
         * OptionCompleter completes command options and parameters. OptionCompleter should be used as an argument of ArgumentCompleter
         * @param completer command parameter completer
         * @param optionValues command value options as map key and its possible values as map value
         * @param options command options that do not have value
         * @param startPos OptionCompleter position in ArgumentCompleter parameters
         */
        public OptionCompleter(
                org.jline.reader.Completer completer,
                Map<String, List<String>> optionValues,
                Collection<String> options,
                int startPos) {
            this(optionValues, options, startPos);
            this.argsCompleters.add(completer);
        }

        /**
         * OptionCompleter completes command options and parameters. OptionCompleter should be used as an argument of ArgumentCompleter
         * @param optionValues command value options as map key and its possible values as map value
         * @param options command options that do not have value
         * @param startPos OptionCompleter position in ArgumentCompleter parameters
         */
        public OptionCompleter(Map<String, List<String>> optionValues, Collection<String> options, int startPos) {
            this(OptDesc.compile(optionValues, options), startPos);
        }

        /**
         * OptionCompleter completes command options and parameters. OptionCompleter should be used as an argument of ArgumentCompleter
         * @param completer command parameter completer
         * @param options command options that do not have value
         * @param startPos OptionCompleter position in ArgumentCompleter parameters
         */
        public OptionCompleter(org.jline.reader.Completer completer, Collection<OptDesc> options, int startPos) {
            this(options, startPos);
            this.argsCompleters.add(completer);
        }

        /**
         * OptionCompleter completes command options and parameters. OptionCompleter should be used as an argument of ArgumentCompleter
         * @param completers command parameters completers
         * @param options command options that do not have value
         * @param startPos OptionCompleter position in ArgumentCompleter parameters
         */
        public OptionCompleter(List<org.jline.reader.Completer> completers, Collection<OptDesc> options, int startPos) {
            this(options, startPos);
            this.argsCompleters = new ArrayList<>(completers);
        }

        /**
         * OptionCompleter completes command options and parameters. OptionCompleter should be used as an argument of ArgumentCompleter
         * @param options command options that do not have value
         * @param startPos OptionCompleter position in ArgumentCompleter parameters
         */
        public OptionCompleter(Collection<OptDesc> options, int startPos) {
            this.options = options;
            this.startPos = startPos;
        }

        /**
         * Sets the start position of this completer in the argument list.
         *
         * @param startPos the position of this completer
         */
        public void setStartPos(int startPos) {
            this.startPos = startPos;
        }

        /**
         * Completes command options and arguments.
         * <p>
         * This method handles completion for:
         * <ul>
         *   <li>Command options (both short and long forms)</li>
         *   <li>Option values (for options that take values)</li>
         *   <li>Command arguments (using the appropriate argument completer)</li>
         * </ul>
         *
         * @param reader the line reader
         * @param commandLine the parsed command line
         * @param candidates the list to add completion candidates to
         */
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
            String command = reader.getParser().getCommand(words.get(startPos - 1));
            if (buffer.startsWith("-")) {
                boolean addbuff = true;
                boolean valueCandidates = false;
                boolean longOption = buffer.startsWith("--");
                int eq = buffer.matches("-[a-zA-Z][a-zA-Z0-9]+") ? 2 : buffer.indexOf('=');
                if (eq < 0) {
                    List<String> usedOptions = new ArrayList<>();
                    for (int i = startPos; i < words.size(); i++) {
                        if (words.get(i).startsWith("-")) {
                            String w = words.get(i);
                            int ind = w.indexOf('=');
                            if (ind < 0) {
                                usedOptions.add(w);
                            } else {
                                usedOptions.add(w.substring(0, ind));
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
                shortOptionValueCompleter(command, words.get(words.size() - 2))
                        .complete(reader, commandLine, candidates);
            } else if (words.size() > 1 && longOptionValueCompleter(command, words.get(words.size() - 2)) != null) {
                longOptionValueCompleter(command, words.get(words.size() - 2))
                        .complete(reader, commandLine, candidates);
            } else if (!argsCompleters.isEmpty()) {
                int args = -1;
                for (int i = startPos; i < words.size(); i++) {
                    if (!words.get(i).startsWith("-")) {
                        if (i > 0
                                && shortOptionValueCompleter(command, words.get(i - 1)) == null
                                && longOptionValueCompleter(command, words.get(i - 1)) == null) {
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

        /**
         * Gets the value completer for a long option.
         *
         * @param command the command name
         * @param opt the option string
         * @return the value completer for the option, or null if the option doesn't take a value
         */
        private org.jline.reader.Completer longOptionValueCompleter(String command, String opt) {
            if (!opt.matches("--[a-zA-Z]+")) {
                return null;
            }
            Collection<OptDesc> optDescs = commandOptions == null ? options : commandOptions.apply(command);
            OptDesc option = findOptDesc(optDescs, opt);
            return option.hasValue() ? option.valueCompleter() : null;
        }

        /**
         * Gets the value completer for a short option.
         * <p>
         * Handles both single short options (-a) and combined short options (-abc).
         * </p>
         *
         * @param command the command name
         * @param opt the option string
         * @return the value completer for the option, or null if the option doesn't take a value
         */
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

        /**
         * Finds an option description for a command and option string.
         *
         * @param command the command name
         * @param opt the option string
         * @return the option description
         */
        private OptDesc findOptDesc(String command, String opt) {
            return findOptDesc(commandOptions == null ? options : commandOptions.apply(command), opt);
        }

        /**
         * Finds an option description in a collection of option descriptions.
         *
         * @param optDescs the collection of option descriptions
         * @param opt the option string
         * @return the matching option description, or a new empty OptDesc if not found
         */
        private OptDesc findOptDesc(Collection<OptDesc> optDescs, String opt) {
            for (OptDesc o : optDescs) {
                if (o.match(opt)) {
                    return o;
                }
            }
            return new OptDesc();
        }
    }

    /**
     * A completer that accepts any input.
     * <p>
     * This completer simply returns the current word as a candidate, effectively
     * accepting any input without providing additional completions.
     * </p>
     */
    public static class AnyCompleter implements org.jline.reader.Completer {
        /** Singleton instance of AnyCompleter */
        public static final AnyCompleter INSTANCE = new AnyCompleter();

        private AnyCompleter() {}

        /**
         * Completes the current word by returning it as a candidate.
         *
         * @param reader the line reader
         * @param commandLine the parsed command line
         * @param candidates the list to add completion candidates to
         */
        @Override
        public void complete(LineReader reader, ParsedLine commandLine, List<Candidate> candidates) {
            assert commandLine != null;
            assert candidates != null;
            String buffer = commandLine.word().substring(0, commandLine.wordCursor());
            candidates.add(new Candidate(AttributedString.stripAnsi(buffer), buffer, null, null, null, null, true));
        }
    }
}
