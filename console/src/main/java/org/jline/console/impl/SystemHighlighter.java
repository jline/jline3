/*
 * Copyright (c) 2002-2021, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.impl;

import org.jline.builtins.SyntaxHighlighter;
import org.jline.builtins.Styles;
import org.jline.console.SystemRegistry;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.impl.DefaultHighlighter;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.OSUtils;
import org.jline.utils.StyleResolver;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Highlight command and language syntax using nanorc highlighter.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public class SystemHighlighter extends DefaultHighlighter {
    private final static StyleResolver resolver = Styles.lsStyle();
    private final static String REGEX_COMMENT_LINE =  "\\s*#.*";
    protected final SyntaxHighlighter commandHighlighter;
    protected final SyntaxHighlighter argsHighlighter;
    protected final SyntaxHighlighter langHighlighter;
    protected final SystemRegistry systemRegistry;
    protected final Map<String, FileHighlightCommand> fileHighlight = new HashMap<>();
    protected final Map<String,SyntaxHighlighter> specificHighlighter = new HashMap<>();
    protected int  commandIndex;

    public SystemHighlighter(SyntaxHighlighter commandHighlighter, SyntaxHighlighter argsHighlighter
            , SyntaxHighlighter langHighlighter) {
        this.commandHighlighter = commandHighlighter;
        this.argsHighlighter = argsHighlighter;
        this.langHighlighter = langHighlighter;
        this.systemRegistry = SystemRegistry.get();
    }

    public void setSpecificHighlighter(String command, SyntaxHighlighter highlighter) {
        this.specificHighlighter.put(command, highlighter);
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
        return ((search != null && search.length() > 0) || reader.getRegionActive() != LineReader.RegionType.NONE
                || errorIndex > -1 || errorPattern != null);
    }

    protected AttributedString systemHighlight(LineReader reader, String buffer) {
        AttributedString out;
        Parser parser = reader.getParser();
        ParsedLine pl = parser.parse(buffer, 0, Parser.ParseContext.COMPLETE);
        String command = pl.words().size() > 0 ? parser.getCommand(pl.words().get(0)) : "";
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
                out = doFileArgsHighlight(reader, buffer, fhc);
            } else {
                out = doFileOptsHighlight(reader, buffer, fhc);
            }
        } else if (systemRegistry.isCommandOrScript(command) || systemRegistry.isCommandAlias(command) || command.isEmpty()
                || buffer.matches(REGEX_COMMENT_LINE)) {
            out = doCommandHighlight(buffer);
        } else if (langHighlighter != null) {
            out = langHighlighter.reset().highlight(buffer);
        } else {
            out = new AttributedStringBuilder().append(buffer).toAttributedString();
        }
        return out;
    }

    protected AttributedString doFileOptsHighlight(LineReader reader, String buffer, FileHighlightCommand fhc) {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        if (commandIndex < 0) {
            highlightCommand(buffer, asb);
        } else {
            highlightCommand(buffer.substring(0, commandIndex), asb);
            ParsedLine parsedLine = reader.getParser().parse(buffer, buffer.length() + 1, Parser.ParseContext.COMPLETE);
            List<String> words = parsedLine.words();
            if (!fhc.isSubcommand() || (words.size() > 2 && fhc.getSubcommand().equals(words.get(1)))) {
                int firstArg = fhc.isSubcommand() ? 1 : 0;
                int idx = buffer.indexOf(words.get(firstArg)) + words.get(firstArg).length() + 1;
                highlightArgs(buffer.substring(commandIndex, idx), asb);
                boolean fileOption = false;
                for (int i = firstArg + 1; i < words.size(); i++) {
                    int nextIdx = buffer.substring(idx).indexOf(words.get(i)) + idx;
                    for (int j = idx; j < nextIdx; j++) {
                        asb.append(buffer.charAt(j));
                    }
                    String word = words.get(i);
                    if (word.contains("=") && fhc.getFileOptions().contains(word.substring(0, word.indexOf("=")))) {
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

    protected AttributedString doFileArgsHighlight(LineReader reader, String buffer, FileHighlightCommand fhc) {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        if (commandIndex < 0) {
            highlightCommand(buffer, asb);
        } else {
            highlightCommand(buffer.substring(0, commandIndex), asb);
            ParsedLine parsedLine = reader.getParser().parse(buffer, buffer.length() + 1, Parser.ParseContext.COMPLETE);
            List<String> words = parsedLine.words();
            if (!fhc.isSubcommand() || (words.size() > 2 && fhc.getSubcommand().equals(words.get(1)))) {
                int firstArg = fhc.isSubcommand() ? 1 : 0;
                int idx = buffer.indexOf(words.get(firstArg)) + words.get(firstArg).length();
                highlightArgs(buffer.substring(commandIndex, idx), asb);
                for (int i = firstArg + 1; i < words.size(); i++) {
                    int nextIdx = buffer.substring(idx).indexOf(words.get(i)) + idx;
                    for (int j = idx; j < nextIdx; j++) {
                        asb.append(buffer.charAt(j));
                    }
                    highlightFileArg(reader, words.get(i), asb);
                    idx = nextIdx + words.get(i).length();
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
                    ? "/" : Paths.get(System.getProperty("user.dir")).getFileSystem().getSeparator();
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
        String type = idx != -1 ? ".*" + name.substring(idx): null;
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
