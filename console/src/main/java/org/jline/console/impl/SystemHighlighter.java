/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;

import org.jline.builtins.Styles;
import org.jline.builtins.SyntaxHighlighter;
import org.jline.console.SystemRegistry;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.impl.DefaultHighlighter;
import org.jline.utils.*;

import static org.jline.builtins.Styles.NANORC_THEME;
import static org.jline.builtins.SyntaxHighlighter.REGEX_TOKEN_NAME;

/**
 * Highlighter implementation that provides syntax highlighting for commands and languages.
 * <p>
 * SystemHighlighter extends DefaultHighlighter to provide syntax highlighting for:
 * <ul>
 *   <li>Command syntax (command names, options, arguments)</li>
 *   <li>Programming language syntax (for various languages)</li>
 *   <li>File content based on file extensions</li>
 * </ul>
 * <p>
 * The highlighter uses nanorc syntax definitions for highlighting, making it compatible
 * with existing nanorc configuration files. It can be customized with different styles
 * and supports dynamic refreshing of highlighting rules.
 *
 */
public class SystemHighlighter extends DefaultHighlighter {
    private StyleResolver resolver = Styles.lsStyle();
    private static final String REGEX_COMMENT_LINE = "\\s*#.*";
    private static final String READER_COLORS = "READER_COLORS";
    protected final SyntaxHighlighter commandHighlighter;
    protected final SyntaxHighlighter argsHighlighter;
    protected final SyntaxHighlighter langHighlighter;
    protected final SystemRegistry systemRegistry;
    protected final Map<String, FileHighlightCommand> fileHighlight = new HashMap<>();
    protected final Map<String, SyntaxHighlighter> specificHighlighter = new HashMap<>();
    protected int commandIndex;
    private final List<Supplier<Boolean>> externalHighlightersRefresh = new ArrayList<>();

    public SystemHighlighter(
            SyntaxHighlighter commandHighlighter,
            SyntaxHighlighter argsHighlighter,
            SyntaxHighlighter langHighlighter) {
        this.commandHighlighter = commandHighlighter;
        this.argsHighlighter = argsHighlighter;
        this.langHighlighter = langHighlighter;
        this.systemRegistry = SystemRegistry.get();
    }

    public void setSpecificHighlighter(String command, SyntaxHighlighter highlighter) {
        this.specificHighlighter.put(command, highlighter);
    }

    @Override
    public void refresh(LineReader lineReader) {
        Path currentTheme = null;
        if (commandHighlighter != null) {
            commandHighlighter.refresh();
            currentTheme = compareThemes(commandHighlighter, currentTheme);
        }
        if (argsHighlighter != null) {
            argsHighlighter.refresh();
            currentTheme = compareThemes(argsHighlighter, currentTheme);
        }
        if (langHighlighter != null) {
            langHighlighter.refresh();
            currentTheme = compareThemes(langHighlighter, currentTheme);
        }
        for (SyntaxHighlighter sh : specificHighlighter.values()) {
            sh.refresh();
            currentTheme = compareThemes(sh, currentTheme);
        }
        if (currentTheme != null) {
            try (BufferedReader reader = Files.newBufferedReader(currentTheme)) {
                String line;
                Map<String, String> tokens = new HashMap<>();
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.trim().split("\\s+", 2);
                    if (parts[0].matches(REGEX_TOKEN_NAME) && parts.length == 2) {
                        tokens.put(parts[0], parts[1]);
                    }
                }
                SystemRegistry registry = SystemRegistry.get();
                registry.setConsoleOption(NANORC_THEME, tokens);
                Map<String, String> readerColors = registry.consoleOption(READER_COLORS, new HashMap<>());
                Styles.StyleCompiler styleCompiler = new Styles.StyleCompiler(readerColors);
                for (String key : readerColors.keySet()) {
                    lineReader.setVariable(key, styleCompiler.getStyle(key));
                }
                for (Supplier<Boolean> refresh : externalHighlightersRefresh) {
                    refresh.get();
                }
                resolver = Styles.lsStyle();
            } catch (IOException e) {
                Log.warn(e.getMessage());
            }
        }
    }

    public void addExternalHighlighterRefresh(Supplier<Boolean> refresh) {
        externalHighlightersRefresh.add(refresh);
    }

    private Path compareThemes(SyntaxHighlighter highlighter, Path currentTheme) {
        Path out;
        if (currentTheme != null) {
            Path theme = highlighter.getCurrentTheme();
            try {
                if (theme != null && !Files.isSameFile(theme, currentTheme)) {
                    Log.warn("Multiple nanorc themes are in use!");
                }
            } catch (Exception e) {
                Log.warn(e.getMessage());
            }
            out = currentTheme;
        } else {
            out = highlighter.getCurrentTheme();
        }
        return out;
    }

    @Override
    public AttributedString highlight(LineReader reader, String buffer) {
        return doDefaultHighlight(reader) ? super.highlight(reader, buffer) : systemHighlight(reader, buffer);
    }

    public void addFileHighlight(String... commands) {
        for (String c : commands) {
            fileHighlight.put(c, new FileHighlightCommand());
        }
    }

    public void addFileHighlight(String command, String subcommand, Collection<String> fileOptions) {
        fileHighlight.put(command, new FileHighlightCommand(subcommand, fileOptions));
    }

    private boolean doDefaultHighlight(LineReader reader) {
        String search = reader.getSearchTerm();
        return ((search != null && !search.isEmpty())
                || reader.getRegionActive() != LineReader.RegionType.NONE
                || errorIndex > -1
                || errorPattern != null);
    }

    protected AttributedString systemHighlight(LineReader reader, String buffer) {
        AttributedString out;
        Parser parser = reader.getParser();
        ParsedLine pl = parser.parse(buffer, 0, Parser.ParseContext.SPLIT_LINE);
        String command = !pl.words().isEmpty() ? parser.getCommand(pl.words().get(0)) : "";
        command = command.startsWith("!") ? "!" : command;
        commandIndex = buffer.indexOf(command) + command.length();
        if (buffer.trim().isEmpty()) {
            out = new AttributedStringBuilder().append(buffer).toAttributedString();
        } else if (specificHighlighter.containsKey(command)) {
            AttributedStringBuilder asb = new AttributedStringBuilder();
            if (commandHighlighter == null) {
                asb.append(specificHighlighter.get(command).reset().highlight(buffer));
            } else {
                highlightCommand(buffer.substring(0, commandIndex), asb);
                asb.append(specificHighlighter.get(command).reset().highlight(buffer.substring(commandIndex)));
            }
            out = asb.toAttributedString();
        } else if (fileHighlight.containsKey(command)) {
            FileHighlightCommand fhc = fileHighlight.get(command);
            if (!fhc.hasFileOptions()) {
                out = doFileArgsHighlight(reader, buffer, pl.words(), fhc);
            } else {
                out = doFileOptsHighlight(reader, buffer, pl.words(), fhc);
            }
        } else if (systemRegistry.isCommandOrScript(command)
                || systemRegistry.isCommandAlias(command)
                || command.isEmpty()
                || buffer.matches(REGEX_COMMENT_LINE)) {
            out = doCommandHighlight(buffer);
        } else if (langHighlighter != null) {
            out = langHighlighter.reset().highlight(buffer);
        } else {
            out = new AttributedStringBuilder().append(buffer).toAttributedString();
        }
        return out;
    }

    protected AttributedString doFileOptsHighlight(
            LineReader reader, String buffer, List<String> words, FileHighlightCommand fhc) {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        if (commandIndex < 0) {
            highlightCommand(buffer, asb);
        } else {
            highlightCommand(buffer.substring(0, commandIndex), asb);
            if (!fhc.isSubcommand() || (words.size() > 2 && fhc.getSubcommand().equals(words.get(1)))) {
                boolean subCommand = fhc.isSubcommand();
                int idx = buffer.indexOf(words.get(0)) + words.get(0).length();
                boolean fileOption = false;
                for (int i = 1; i < words.size(); i++) {
                    int nextIdx = buffer.substring(idx).indexOf(words.get(i)) + idx;
                    for (int j = idx; j < nextIdx; j++) {
                        asb.append(buffer.charAt(j));
                    }
                    String word = words.get(i);
                    if (subCommand) {
                        subCommand = false;
                        highlightArgs(word, asb);
                    } else if (word.contains("=")
                            && fhc.getFileOptions().contains(word.substring(0, word.indexOf("=")))) {
                        highlightArgs(word.substring(0, word.indexOf("=") + 1), asb);
                        highlightFileArg(reader, word.substring(word.indexOf("=") + 1), asb);
                    } else if (fhc.getFileOptions().contains(word)) {
                        highlightArgs(word, asb);
                        fileOption = true;
                    } else if (fileOption) {
                        highlightFileArg(reader, word, asb);
                    } else {
                        highlightArgs(word, asb);
                        fileOption = false;
                    }
                    idx = nextIdx + word.length();
                }
            } else {
                highlightArgs(buffer.substring(commandIndex), asb);
            }
        }
        return asb.toAttributedString();
    }

    protected AttributedString doFileArgsHighlight(
            LineReader reader, String buffer, List<String> words, FileHighlightCommand fhc) {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        if (commandIndex < 0) {
            highlightCommand(buffer, asb);
        } else {
            highlightCommand(buffer.substring(0, commandIndex), asb);
            if (!fhc.isSubcommand() || (words.size() > 2 && fhc.getSubcommand().equals(words.get(1)))) {
                boolean subCommand = fhc.isSubcommand();
                int idx = buffer.indexOf(words.get(0)) + words.get(0).length();
                for (int i = 1; i < words.size(); i++) {
                    int nextIdx = buffer.substring(idx).indexOf(words.get(i)) + idx;
                    for (int j = idx; j < nextIdx; j++) {
                        asb.append(buffer.charAt(j));
                    }
                    if (subCommand) {
                        subCommand = false;
                        highlightArgs(words.get(i), asb);
                    } else {
                        highlightFileArg(reader, words.get(i), asb);
                        idx = nextIdx + words.get(i).length();
                    }
                }
            } else {
                highlightArgs(buffer.substring(commandIndex), asb);
            }
        }
        return asb.toAttributedString();
    }

    protected AttributedString doCommandHighlight(String buffer) {
        AttributedString out;
        if (commandHighlighter != null || argsHighlighter != null) {
            AttributedStringBuilder asb = new AttributedStringBuilder();
            if (commandIndex < 0 || buffer.matches(REGEX_COMMENT_LINE)) {
                highlightCommand(buffer, asb);
            } else {
                highlightCommand(buffer.substring(0, commandIndex), asb);
                highlightArgs(buffer.substring(commandIndex), asb);
            }
            out = asb.toAttributedString();
        } else {
            out = new AttributedStringBuilder().append(buffer).toAttributedString();
        }
        return out;
    }

    private void highlightFileArg(LineReader reader, String arg, AttributedStringBuilder asb) {
        if (arg.startsWith("-")) {
            highlightArgs(arg, asb);
        } else {
            String separator = reader.isSet(LineReader.Option.USE_FORWARD_SLASH)
                    ? "/"
                    : Paths.get(System.getProperty("user.dir")).getFileSystem().getSeparator();
            StringBuilder sb = new StringBuilder();
            try {
                Path path = new File(arg).toPath();
                Iterator<Path> iterator = path.iterator();
                if (OSUtils.IS_WINDOWS && arg.matches("^[A-Za-z]:.*$")) {
                    if (arg.length() == 2) {
                        sb.append(arg);
                        asb.append(arg);
                    } else if (arg.charAt(2) == separator.charAt(0)) {
                        sb.append(arg.substring(0, 3));
                        asb.append(arg.substring(0, 3));
                    }
                }
                if (arg.startsWith(separator)) {
                    sb.append(separator);
                    asb.append(separator);
                }
                while (iterator.hasNext()) {
                    sb.append(iterator.next());
                    highlightFile(new File(sb.toString()).toPath(), asb);
                    if (iterator.hasNext()) {
                        sb.append(separator);
                        asb.append(separator);
                    }
                }
                if (arg.length() > 2 && !arg.matches("^[A-Za-z]:" + separator) && arg.endsWith(separator)) {
                    asb.append(separator);
                }
            } catch (Exception e) {
                asb.append(arg);
            }
        }
    }

    private void highlightFile(Path path, AttributedStringBuilder asb) {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        String name = path.getFileName().toString();
        int idx = name.lastIndexOf(".");
        String type = idx != -1 ? ".*" + name.substring(idx) : null;
        if (Files.isSymbolicLink(path)) {
            sb.styled(resolver.resolve(".ln"), name);
        } else if (Files.isDirectory(path)) {
            sb.styled(resolver.resolve(".di"), name);
        } else if (Files.isExecutable(path) && !OSUtils.IS_WINDOWS) {
            sb.styled(resolver.resolve(".ex"), name);
        } else if (type != null && resolver.resolve(type).getStyle() != 0) {
            sb.styled(resolver.resolve(type), name);
        } else if (Files.isRegularFile(path)) {
            sb.styled(resolver.resolve(".fi"), name);
        } else {
            sb.append(name);
        }
        asb.append(sb);
    }

    private void highlightArgs(String args, AttributedStringBuilder asb) {
        if (argsHighlighter != null) {
            asb.append(argsHighlighter.reset().highlight(args));
        } else {
            asb.append(args);
        }
    }

    private void highlightCommand(String command, AttributedStringBuilder asb) {
        if (commandHighlighter != null) {
            asb.append(commandHighlighter.reset().highlight(command));
        } else {
            asb.append(command);
        }
    }

    protected static class FileHighlightCommand {
        private final String subcommand;
        private final List<String> fileOptions = new ArrayList<>();

        public FileHighlightCommand() {
            this(null, new ArrayList<>());
        }

        public FileHighlightCommand(String subcommand, Collection<String> fileOptions) {
            this.subcommand = subcommand;
            this.fileOptions.addAll(fileOptions);
        }

        public boolean isSubcommand() {
            return subcommand != null;
        }

        public boolean hasFileOptions() {
            return !fileOptions.isEmpty();
        }

        public String getSubcommand() {
            return subcommand;
        }

        public List<String> getFileOptions() {
            return fileOptions;
        }
    }
}
