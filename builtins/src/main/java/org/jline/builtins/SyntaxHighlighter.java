/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.*;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import org.jline.utils.*;

/**
 * Java implementation of a syntax highlighter based on nanorc format.
 * <p>
 * This class provides syntax highlighting capabilities for JLine applications,
 * using configuration files in a format similar to GNU nano's nanorc files.
 * It supports:
 * </p>
 * <ul>
 *   <li>Regular expression based highlighting rules</li>
 *   <li>Theme-based styling</li>
 *   <li>File type detection</li>
 *   <li>Inclusion of other configuration files</li>
 * </ul>
 * <p>
 * The highlighter can be used to add syntax coloring to various text displays
 * in terminal applications, such as file viewers, editors, and REPLs.
 * </p>
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public class SyntaxHighlighter {
    public static final String REGEX_TOKEN_NAME = "[A-Z_]+";
    public static final String TYPE_NANORCTHEME = ".nanorctheme";
    public static final String DEFAULT_NANORC_FILE = "jnanorc";
    protected static final String DEFAULT_LESSRC_FILE = "jlessrc";
    protected static final String COMMAND_INCLUDE = "include";
    protected static final String COMMAND_THEME = "theme";
    private static final String TOKEN_NANORC = "NANORC";
    private final Path nanorc;
    private final String syntaxName;
    private final String nanorcUrl;
    private final Map<String, List<HighlightRule>> rules = new HashMap<>();
    private Path currentTheme;
    private boolean startEndHighlight;
    private int ruleStartId = 0;

    private Parser parser;

    private SyntaxHighlighter() {
        this(null, null, null);
    }

    private SyntaxHighlighter(String nanorcUrl) {
        this(null, null, nanorcUrl);
    }

    private SyntaxHighlighter(Path nanorc, String syntaxName) {
        this(nanorc, syntaxName, null);
    }

    private SyntaxHighlighter(Path nanorc, String syntaxName, String nanorcUrl) {
        this.nanorc = nanorc;
        this.syntaxName = syntaxName;
        this.nanorcUrl = nanorcUrl;
        Map<String, List<HighlightRule>> defaultRules = new HashMap<>();
        defaultRules.put(TOKEN_NANORC, new ArrayList<>());
        rules.putAll(defaultRules);
    }

    protected static SyntaxHighlighter build(List<Path> syntaxFiles, String file, String syntaxName) {
        return build(syntaxFiles, file, syntaxName, false);
    }

    protected static SyntaxHighlighter build(
            List<Path> syntaxFiles, String file, String syntaxName, boolean ignoreErrors) {
        SyntaxHighlighter out = new SyntaxHighlighter();
        Map<String, String> colorTheme = new HashMap<>();
        try {
            if (syntaxName == null || !syntaxName.equals("none")) {
                for (Path p : syntaxFiles) {
                    try {
                        if (colorTheme.isEmpty() && p.getFileName().toString().endsWith(TYPE_NANORCTHEME)) {
                            out.setCurrentTheme(p);
                            try (BufferedReader reader = Files.newBufferedReader(p)) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    line = line.trim();
                                    if (!line.isEmpty() && !line.startsWith("#")) {
                                        List<String> parts = Arrays.asList(line.split("\\s+", 2));
                                        colorTheme.put(parts.get(0), parts.get(1));
                                    }
                                }
                            }
                        } else {
                            NanorcParser nanorcParser = new NanorcParser(p, syntaxName, file, colorTheme);
                            nanorcParser.parse();
                            if (nanorcParser.matches()) {
                                out.addRules(nanorcParser.getHighlightRules());
                                out.setParser(nanorcParser.getParser());
                                return out;
                            } else if (nanorcParser.isDefault()) {
                                out.addRules(nanorcParser.getHighlightRules());
                            }
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        } catch (PatternSyntaxException e) {
            if (!ignoreErrors) {
                throw e;
            }
        }
        return out;
    }

    /**
     * Build SyntaxHighlighter
     *
     * @param nanorc        Path of nano config file jnanorc
     * @param syntaxName    syntax name e.g 'Java'
     * @return              SyntaxHighlighter
     */
    public static SyntaxHighlighter build(Path nanorc, String syntaxName) {
        SyntaxHighlighter out = new SyntaxHighlighter(nanorc, syntaxName);
        List<Path> syntaxFiles = new ArrayList<>();
        try {
            try (BufferedReader reader = Files.newBufferedReader(nanorc)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        List<String> parts = RuleSplitter.split(line);
                        if (parts.get(0).equals(COMMAND_INCLUDE)) {
                            nanorcInclude(nanorc, parts.get(1), syntaxFiles);
                        } else if (parts.get(0).equals(COMMAND_THEME)) {
                            nanorcTheme(nanorc, parts.get(1), syntaxFiles);
                        }
                    }
                }
            }
            SyntaxHighlighter sh = build(syntaxFiles, null, syntaxName);
            out.addRules(sh.rules);
            out.setParser(sh.parser);
            out.setCurrentTheme(sh.currentTheme);
        } catch (Exception e) {
            // ignore
        }
        return out;
    }

    protected static void nanorcInclude(Path nanorc, String parameter, List<Path> syntaxFiles) throws IOException {
        addFiles(nanorc, parameter, s -> s.forEach(syntaxFiles::add));
    }

    protected static void nanorcTheme(Path nanorc, String parameter, List<Path> syntaxFiles) throws IOException {
        addFiles(nanorc, parameter, s -> s.findFirst().ifPresent(p -> syntaxFiles.add(0, p)));
    }

    protected static void addFiles(Path nanorc, String parameter, Consumer<Stream<Path>> consumer) throws IOException {
        // Extract the static prefix and glob pattern parts
        PathParts parts = extractPathParts(parameter);

        Path searchRoot = nanorc.resolveSibling(parts.staticPrefix);
        if (Files.exists(searchRoot)) {
            if (parts.globPattern.isEmpty()) {
                // No wildcards - treat as literal path
                consumer.accept(Stream.of(searchRoot));
            } else {
                // Has wildcards - use glob matching
                PathMatcher pathMatcher = searchRoot.getFileSystem().getPathMatcher("glob:" + parts.globPattern);
                try (Stream<Path> pathStream = Files.walk(searchRoot)) {
                    consumer.accept(pathStream.filter(p -> pathMatcher.matches(searchRoot.relativize(p))));
                }
            }
        }
    }

    /**
     * Represents the static and glob parts of a path pattern.
     */
    private static class PathParts {
        final String staticPrefix;
        final String globPattern;

        PathParts(String staticPrefix, String globPattern) {
            this.staticPrefix = staticPrefix;
            this.globPattern = globPattern;
        }
    }

    /**
     * Extracts the static (non-wildcard) path prefix and the glob pattern from a path.
     * For example: <ul>
     * <li>{@code foo/bar&#47;*.nanorc} returns {@code PathParts("foo/bar", "*.nanorc")}</li>
     * <li>{@code foo/bar/**&#47;*.nanorc} returns {@code PathParts("foo/bar", "**&#47;*.nanorc")}</li>
     * <li>{@code *.nanorc} returns {@code PathParts("", "*.nanorc")}</li>
     * <li>{@code /usr/share/nano/*.nanorc} returns {@code PathParts("/usr/share/nano", "*.nanorc")}</li>
     * </ul>
     */
    private static PathParts extractPathParts(String pattern) {
        // Find the first occurrence of wildcards
        int firstWildcard = Math.min(
                pattern.indexOf('*') == -1 ? Integer.MAX_VALUE : pattern.indexOf('*'),
                pattern.indexOf('?') == -1 ? Integer.MAX_VALUE : pattern.indexOf('?'));

        if (firstWildcard == Integer.MAX_VALUE) {
            // No wildcards found, the entire pattern is static
            return new PathParts(pattern, "");
        }

        // Find the last directory separator before the first wildcard (handle both / and \)
        int lastSlashBeforeWildcard = -1;
        for (int i = firstWildcard - 1; i >= 0; i--) {
            char c = pattern.charAt(i);
            if (c == '/' || c == '\\') {
                lastSlashBeforeWildcard = i;
                break;
            }
        }

        if (lastSlashBeforeWildcard == -1) {
            // No directory separator before wildcard (e.g., "*.nanorc")
            return new PathParts("", pattern);
        }

        String staticPrefix = pattern.substring(0, lastSlashBeforeWildcard);
        String globPattern = pattern.substring(lastSlashBeforeWildcard + 1);
        return new PathParts(staticPrefix, globPattern);
    }

    /**
     * Build SyntaxHighlighter
     * <p>
     * This method builds a SyntaxHighlighter from a URL or classpath resource.
     * The URL can be a file URL, an HTTP URL, or a classpath resource URL.
     * </p>
     * <p>
     * For classpath resources, use the "classpath:" prefix followed by the resource path.
     * For example: "classpath:/nano/jnanorc"
     * </p>
     *
     * @param nanorcUrl     URL or classpath resource path of nanorc file
     * @return              SyntaxHighlighter
     */
    public static SyntaxHighlighter build(String nanorcUrl) {
        SyntaxHighlighter out = new SyntaxHighlighter(nanorcUrl);
        InputStream inputStream;
        try {
            if (nanorcUrl.startsWith("classpath:")) {
                String resourcePath = nanorcUrl.substring(10);
                try {
                    // Try to get the resource as a Path first
                    Path resourceAsPath = ClasspathResourceUtil.getResourcePath(resourcePath);
                    inputStream = Files.newInputStream(resourceAsPath);
                } catch (Exception e) {
                    // Fall back to direct resource loading if Path conversion fails
                    inputStream = new Source.ResourceSource(resourcePath, null).read();
                }
            } else {
                inputStream = new Source.URLSource(new URI(nanorcUrl).toURL(), null).read();
            }
            NanorcParser parser = new NanorcParser(inputStream, null, null);
            parser.parse();
            out.addRules(parser.getHighlightRules());
        } catch (IOException | URISyntaxException e) {
            // ignore
        }
        return out;
    }

    private void addRules(Map<String, List<HighlightRule>> rules) {
        this.rules.putAll(rules);
    }

    public void setCurrentTheme(Path currentTheme) {
        this.currentTheme = currentTheme;
    }

    public Path getCurrentTheme() {
        return currentTheme;
    }

    void setParser(Parser parser) {
        this.parser = parser;
    }

    public SyntaxHighlighter reset() {
        ruleStartId = 0;
        startEndHighlight = false;
        if (parser != null) {
            parser.reset();
        }
        return this;
    }

    public void refresh() {
        SyntaxHighlighter sh;
        if (nanorc != null && syntaxName != null) {
            sh = SyntaxHighlighter.build(nanorc, syntaxName);
        } else if (nanorcUrl != null) {
            sh = SyntaxHighlighter.build(nanorcUrl);
        } else {
            throw new IllegalStateException("Not possible to refresh highlighter!");
        }
        rules.clear();
        addRules(sh.rules);
        parser = sh.parser;
        currentTheme = sh.currentTheme;
    }

    public AttributedString highlight(String string) {
        return splitAndHighlight(new AttributedString(string));
    }

    public AttributedString highlight(AttributedStringBuilder asb) {
        return splitAndHighlight(asb.toAttributedString());
    }

    public AttributedString highlight(AttributedString attributedString) {
        return splitAndHighlight(attributedString);
    }

    private AttributedString splitAndHighlight(AttributedString attributedString) {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        boolean first = true;
        for (AttributedString line : attributedString.columnSplitLength(Integer.MAX_VALUE)) {
            if (!first) {
                asb.append("\n");
            }
            List<ParsedToken> tokens = new ArrayList<>();
            if (parser != null) {
                parser.parse(line);
                tokens = parser.getTokens();
            }
            if (tokens.isEmpty()) {
                asb.append(_highlight(line, rules.get(TOKEN_NANORC)));
            } else {
                int pos = 0;
                for (ParsedToken t : tokens) {
                    if (t.getStart() > pos) {
                        AttributedStringBuilder head =
                                _highlight(line.columnSubSequence(pos, t.getStart() + 1), rules.get(TOKEN_NANORC));
                        asb.append(head.columnSubSequence(0, head.length() - 1));
                    }
                    asb.append(_highlight(
                            line.columnSubSequence(t.getStart(), t.getEnd()),
                            rules.get(t.getName()),
                            t.getStartWith(),
                            line.columnSubSequence(t.getEnd(), line.length())));
                    pos = t.getEnd();
                }
                if (pos < line.length()) {
                    asb.append(_highlight(line.columnSubSequence(pos, line.length()), rules.get(TOKEN_NANORC)));
                }
            }
            first = false;
        }
        return asb.toAttributedString();
    }

    private AttributedStringBuilder _highlight(AttributedString line, List<HighlightRule> rules) {
        return _highlight(line, rules, null, null);
    }

    private AttributedStringBuilder _highlight(
            AttributedString line, List<HighlightRule> rules, CharSequence startWith, CharSequence continueAs) {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.append(line);
        if (rules.isEmpty()) {
            return asb;
        }
        int startId = ruleStartId;
        boolean endHighlight = startEndHighlight;
        for (int i = startId; i < (endHighlight ? startId + 1 : rules.size()); i++) {
            HighlightRule rule = rules.get(i);
            switch (rule.getType()) {
                case PATTERN:
                    asb.styleMatches(rule.getPattern(), rule.getStyle());
                    break;
                case START_END:
                    boolean done = false;
                    Matcher start = rule.getStart().matcher(asb.toAttributedString());
                    Matcher end = rule.getEnd().matcher(asb.toAttributedString());
                    while (!done) {
                        AttributedStringBuilder a = new AttributedStringBuilder();
                        if (startEndHighlight && ruleStartId == i) {
                            if (end.find()) {
                                ruleStartId = 0;
                                startEndHighlight = false;
                                a.append(asb.columnSubSequence(0, end.end()), rule.getStyle());
                                a.append(_highlight(
                                        asb.columnSubSequence(end.end(), asb.length())
                                                .toAttributedString(),
                                        rules));
                            } else {
                                a.append(asb, rule.getStyle());
                                done = true;
                            }
                            asb = a;
                        } else {
                            if (start.find()) {
                                a.append(asb.columnSubSequence(0, start.start()));
                                if (end.find()) {
                                    a.append(asb.columnSubSequence(start.start(), end.end()), rule.getStyle());
                                    a.append(asb.columnSubSequence(end.end(), asb.length()));
                                } else {
                                    ruleStartId = i;
                                    startEndHighlight = true;
                                    a.append(asb.columnSubSequence(start.start(), asb.length()), rule.getStyle());
                                    done = true;
                                }
                                asb = a;
                            } else {
                                done = true;
                            }
                        }
                    }
                    break;
                case PARSER_START_WITH:
                    if (startWith != null && startWith.toString().startsWith(rule.getStartWith())) {
                        asb.styleMatches(rule.getPattern(), rule.getStyle());
                    }
                    break;
                case PARSER_CONTINUE_AS:
                    if (continueAs != null && continueAs.toString().matches(rule.getContinueAs() + ".*")) {
                        asb.styleMatches(rule.getPattern(), rule.getStyle());
                    }
                    break;
            }
        }
        return asb;
    }

    static class HighlightRule {
        public enum RuleType {
            PATTERN,
            START_END,
            PARSER_START_WITH,
            PARSER_CONTINUE_AS
        }

        private final RuleType type;
        private Pattern pattern;
        private final AttributedStyle style;
        private Pattern start;
        private Pattern end;
        private String startWith;
        private String continueAs;

        public HighlightRule(AttributedStyle style, Pattern pattern) {
            this.type = RuleType.PATTERN;
            this.pattern = pattern;
            this.style = style;
        }

        public HighlightRule(AttributedStyle style, Pattern start, Pattern end) {
            this.type = RuleType.START_END;
            this.style = style;
            this.start = start;
            this.end = end;
        }

        public HighlightRule(RuleType parserRuleType, AttributedStyle style, String value) {
            this.type = parserRuleType;
            this.style = style;
            this.pattern = Pattern.compile(".*");
            if (parserRuleType == RuleType.PARSER_START_WITH) {
                this.startWith = value;
            } else if (parserRuleType == RuleType.PARSER_CONTINUE_AS) {
                this.continueAs = value;
            } else {
                throw new IllegalArgumentException("Bad RuleType: " + parserRuleType);
            }
        }

        public RuleType getType() {
            return type;
        }

        public AttributedStyle getStyle() {
            return style;
        }

        public Pattern getPattern() {
            if (type == RuleType.START_END) {
                throw new IllegalAccessError();
            }
            return pattern;
        }

        public Pattern getStart() {
            if (type == RuleType.PATTERN) {
                throw new IllegalAccessError();
            }
            return start;
        }

        public Pattern getEnd() {
            if (type == RuleType.PATTERN) {
                throw new IllegalAccessError();
            }
            return end;
        }

        public String getStartWith() {
            return startWith;
        }

        public String getContinueAs() {
            return continueAs;
        }

        public static RuleType evalRuleType(List<String> colorCfg) {
            RuleType out = null;
            if (colorCfg.get(0).equals("color") || colorCfg.get(0).equals("icolor")) {
                out = RuleType.PATTERN;
                if (colorCfg.size() == 3) {
                    if (colorCfg.get(2).startsWith("startWith=")) {
                        out = RuleType.PARSER_START_WITH;
                    } else if (colorCfg.get(2).startsWith("continueAs=")) {
                        out = RuleType.PARSER_CONTINUE_AS;
                    }
                } else if (colorCfg.size() == 4) {
                    if (colorCfg.get(2).startsWith("start=") && colorCfg.get(3).startsWith("end=")) {
                        out = RuleType.START_END;
                    }
                }
            }
            return out;
        }

        public String toString() {
            return "{type:" + type
                    + ", startWith: " + startWith
                    + ", continueAs: " + continueAs
                    + ", start: " + start
                    + ", end: " + end
                    + ", pattern: " + pattern
                    + "}";
        }
    }

    static class NanorcParser {
        private static final String DEFAULT_SYNTAX = "default";
        private final String name;
        private final String target;
        private final Map<String, List<HighlightRule>> highlightRules = new HashMap<>();
        private final BufferedReader reader;
        private Map<String, String> colorTheme = new HashMap<>();
        private boolean matches = false;
        private String syntaxName = "unknown";

        private Parser parser;

        public NanorcParser(Path file, String name, String target, Map<String, String> colorTheme) throws IOException {
            this(new Source.PathSource(file, null).read(), name, target);
            this.colorTheme = colorTheme;
        }

        public NanorcParser(InputStream in, String name, String target) {
            this.reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            this.name = name;
            this.target = target;
            highlightRules.put(TOKEN_NANORC, new ArrayList<>());
        }

        public void parse() throws IOException {
            String line;
            int idx = 0;
            try {
                while ((line = reader.readLine()) != null) {
                    idx++;
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        List<String> parts = RuleSplitter.split(line);
                        if (parts.get(0).equals("syntax")) {
                            syntaxName = parts.get(1);
                            List<Pattern> filePatterns = new ArrayList<>();
                            if (name != null) {
                                if (name.equals(syntaxName)) {
                                    matches = true;
                                } else {
                                    break;
                                }
                            } else if (target != null) {
                                for (int i = 2; i < parts.size(); i++) {
                                    filePatterns.add(Pattern.compile(parts.get(i)));
                                }
                                for (Pattern p : filePatterns) {
                                    if (p.matcher(target).find()) {
                                        matches = true;
                                        break;
                                    }
                                }
                                if (!matches && !syntaxName.equals(DEFAULT_SYNTAX)) {
                                    break;
                                }
                            } else {
                                matches = true;
                            }
                        } else if (parts.get(0).startsWith("$")) {
                            String key = themeKey(parts.get(0));
                            if (colorTheme.containsKey(key)) {
                                if (parser == null) {
                                    parser = new Parser();
                                }
                                String[] args = parts.get(1).split(",\\s*");
                                boolean validKey = true;
                                if (key.startsWith("$BLOCK_COMMENT")) {
                                    parser.setBlockCommentDelimiters(key, args);
                                } else if (key.startsWith("$LINE_COMMENT")) {
                                    parser.setLineCommentDelimiters(key, args);
                                } else if (key.startsWith("$BALANCED_DELIMITERS")) {
                                    parser.setBalancedDelimiters(key, args);
                                } else {
                                    Log.warn("Unknown token type: ", key);
                                    validKey = false;
                                }
                                if (validKey) {
                                    if (!highlightRules.containsKey(key)) {
                                        highlightRules.put(key, new ArrayList<>());
                                    }
                                    for (String l : colorTheme.get(key).split("\\\\n")) {
                                        idx++;
                                        addHighlightRule(RuleSplitter.split(l), idx, key);
                                    }
                                }
                            } else {
                                Log.warn("Unknown token type: ", key);
                            }
                        } else if (!addHighlightRule(parts, idx, TOKEN_NANORC)
                                && parts.get(0).matches("\\+" + REGEX_TOKEN_NAME)) {
                            String key = themeKey(parts.get(0));
                            String theme = colorTheme.get(key);
                            if (theme != null) {
                                for (String l : theme.split("\\\\n")) {
                                    idx++;
                                    addHighlightRule(RuleSplitter.split(l), idx, TOKEN_NANORC);
                                }
                            } else {
                                Log.warn("Unknown token type: ", key);
                            }
                        }
                    }
                }
            } finally {
                reader.close();
            }
        }

        private boolean addHighlightRule(List<String> parts, int idx, String tokenName) {
            boolean out = true;
            if (parts.get(0).equals("color")) {
                addHighlightRule(syntaxName + idx, parts, false, tokenName);
            } else if (parts.get(0).equals("icolor")) {
                addHighlightRule(syntaxName + idx, parts, true, tokenName);
            } else if (parts.get(0).matches(REGEX_TOKEN_NAME + "[:]?")) {
                String key = themeKey(parts.get(0));
                String theme = colorTheme.get(key);
                if (theme != null) {
                    parts.set(0, "color");
                    parts.add(1, theme);
                    addHighlightRule(syntaxName + idx, parts, false, tokenName);
                } else {
                    Log.warn("Unknown token type: ", key);
                }
            } else if (parts.get(0).matches("~" + REGEX_TOKEN_NAME + "[:]?")) {
                String key = themeKey(parts.get(0));
                String theme = colorTheme.get(key);
                if (theme != null) {
                    parts.set(0, "icolor");
                    parts.add(1, theme);
                    addHighlightRule(syntaxName + idx, parts, true, tokenName);
                } else {
                    Log.warn("Unknown token type: ", key);
                }
            } else {
                out = false;
            }
            return out;
        }

        private String themeKey(String key) {
            if (key.startsWith("+")) {
                return key;
            } else {
                int keyEnd = key.endsWith(":") ? key.length() - 1 : key.length();
                if (key.startsWith("~")) {
                    return key.substring(1, keyEnd);
                }
                return key.substring(0, keyEnd);
            }
        }

        public boolean matches() {
            return matches;
        }

        public Parser getParser() {
            return parser;
        }

        public Map<String, List<HighlightRule>> getHighlightRules() {
            return highlightRules;
        }

        public boolean isDefault() {
            return syntaxName.equals(DEFAULT_SYNTAX);
        }

        private void addHighlightRule(String reference, List<String> parts, boolean caseInsensitive, String tokenName) {
            Map<String, String> spec = new HashMap<>();
            spec.put(reference, parts.get(1));
            Styles.StyleCompiler sh = new Styles.StyleCompiler(spec, true);
            AttributedStyle style = new StyleResolver(sh::getStyle).resolve("." + reference);

            try {
                if (HighlightRule.evalRuleType(parts) == HighlightRule.RuleType.PATTERN) {
                    if (parts.size() == 2) {
                        highlightRules.get(tokenName).add(new HighlightRule(style, doPattern(".*", caseInsensitive)));
                    } else {
                        for (int i = 2; i < parts.size(); i++) {
                            highlightRules
                                    .get(tokenName)
                                    .add(new HighlightRule(style, doPattern(parts.get(i), caseInsensitive)));
                        }
                    }
                } else if (HighlightRule.evalRuleType(parts) == HighlightRule.RuleType.START_END) {
                    String s = parts.get(2);
                    String e = parts.get(3);
                    highlightRules
                            .get(tokenName)
                            .add(new HighlightRule(
                                    style,
                                    doPattern(s.substring(7, s.length() - 1), caseInsensitive),
                                    doPattern(e.substring(5, e.length() - 1), caseInsensitive)));
                } else if (HighlightRule.evalRuleType(parts) == HighlightRule.RuleType.PARSER_START_WITH) {
                    highlightRules
                            .get(tokenName)
                            .add(new HighlightRule(
                                    HighlightRule.RuleType.PARSER_START_WITH,
                                    style,
                                    parts.get(2).substring(10)));
                } else if (HighlightRule.evalRuleType(parts) == HighlightRule.RuleType.PARSER_CONTINUE_AS) {
                    highlightRules
                            .get(tokenName)
                            .add(new HighlightRule(
                                    HighlightRule.RuleType.PARSER_CONTINUE_AS,
                                    style,
                                    parts.get(2).substring(11)));
                }
            } catch (PatternSyntaxException e) {
                Log.warn("Invalid highlight regex", reference, parts, e);
            } catch (Exception e) {
                Log.warn("Failure while handling highlight regex", reference, parts, e);
            }
        }

        private Pattern doPattern(String regex, boolean caseInsensitive) {
            regex = Parser.fixRegexes(regex);
            return caseInsensitive ? Pattern.compile(regex, Pattern.CASE_INSENSITIVE) : Pattern.compile(regex);
        }
    }

    protected static class RuleSplitter {
        protected RuleSplitter() {}

        protected static List<String> split(String s) {
            List<String> out = new ArrayList<>();
            if (s.length() == 0) {
                return out;
            }
            int depth = 0;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '"') {
                    if (depth == 0) {
                        depth = 1;
                    } else {
                        char nextChar = i < s.length() - 1 ? s.charAt(i + 1) : ' ';
                        if (nextChar == ' ') {
                            depth = 0;
                        }
                    }
                } else if (c == ' ' && depth == 0 && sb.length() > 0) {
                    out.add(stripQuotes(sb.toString()));
                    sb = new StringBuilder();
                    continue;
                }
                if (sb.length() > 0 || (c != ' ' && c != '\t')) {
                    sb.append(c);
                }
            }
            if (sb.length() > 0) {
                out.add(stripQuotes(sb.toString()));
            }
            return out;
        }

        private static String stripQuotes(String s) {
            String out = s.trim();
            if (s.startsWith("\"") && s.endsWith("\"")) {
                out = s.substring(1, s.length() - 1);
            }
            return out;
        }
    }

    private static class BlockCommentDelimiters {
        private final String start;
        private final String end;

        public BlockCommentDelimiters(String[] args) {
            if (args.length != 2
                    || args[0] == null
                    || args[1] == null
                    || args[0].isEmpty()
                    || args[1].isEmpty()
                    || args[0].equals(args[1])) {
                throw new IllegalArgumentException("Bad block comment delimiters!");
            }
            start = args[0];
            end = args[1];
        }

        public String getStart() {
            return start;
        }

        public String getEnd() {
            return end;
        }
    }

    private static class ParsedToken {
        private final String name;
        private final CharSequence startWith;
        private final int start;
        private final int end;

        public ParsedToken(String name, CharSequence startWith, int start, int end) {
            this.name = name;
            this.startWith = startWith;
            this.start = start;
            this.end = end;
        }

        public String getName() {
            return name;
        }

        public CharSequence getStartWith() {
            return startWith;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }
    }

    static class Parser {
        private static final char escapeChar = '\\';
        private String blockCommentTokenName;
        private BlockCommentDelimiters blockCommentDelimiters;
        private String lineCommentTokenName;
        private String[] lineCommentDelimiters;
        private String balancedDelimiterTokenName;
        private String[] balancedDelimiters;
        private String balancedDelimiter;
        private List<ParsedToken> tokens;
        private CharSequence startWith;
        private int tokenStart = 0;
        private boolean blockComment;
        private boolean lineComment;
        private boolean balancedQuoted;

        public Parser() {}

        public void setBlockCommentDelimiters(String tokenName, String[] args) {
            try {
                blockCommentTokenName = tokenName;
                blockCommentDelimiters = new BlockCommentDelimiters(args);
            } catch (Exception e) {
                Log.warn(e.getMessage());
            }
        }

        public void setLineCommentDelimiters(String tokenName, String[] args) {
            lineCommentTokenName = tokenName;
            lineCommentDelimiters = args;
        }

        public void setBalancedDelimiters(String tokenName, String[] args) {
            balancedDelimiterTokenName = tokenName;
            balancedDelimiters = args;
        }

        public void reset() {
            startWith = null;
            blockComment = false;
            lineComment = false;
            balancedQuoted = false;
            tokenStart = 0;
        }

        public void parse(final CharSequence line) {
            if (line == null) {
                return;
            }
            tokens = new ArrayList<>();
            if (blockComment || balancedQuoted) {
                tokenStart = 0;
            }
            for (int i = 0; i < line.length(); i++) {
                if (isEscapeChar(line, i) || isEscaped(line, i)) {
                    continue;
                }
                if (!blockComment && !lineComment && !balancedQuoted) {
                    if (blockCommentDelimiters != null && isDelimiter(line, i, blockCommentDelimiters.getStart())) {
                        blockComment = true;
                        tokenStart = i;
                        startWith = startWithSubstring(line, i);
                        i = i + blockCommentDelimiters.getStart().length() - 1;
                    } else if (isLineCommentDelimiter(line, i)) {
                        lineComment = true;
                        tokenStart = i;
                        startWith = startWithSubstring(line, i);
                        break;
                    } else if ((balancedDelimiter = balancedDelimiter(line, i)) != null) {
                        balancedQuoted = true;
                        tokenStart = i;
                        startWith = startWithSubstring(line, i);
                        i = i + balancedDelimiter.length() - 1;
                    }
                } else if (blockComment) {
                    if (isDelimiter(line, i, blockCommentDelimiters.getEnd())) {
                        blockComment = false;
                        i = i + blockCommentDelimiters.getEnd().length() - 1;
                        tokens.add(new ParsedToken(blockCommentTokenName, startWith, tokenStart, i + 1));
                    }
                } else if (balancedQuoted) {
                    if (isDelimiter(line, i, balancedDelimiter)) {
                        balancedQuoted = false;
                        i = i + balancedDelimiter.length() - 1;
                        if (i - tokenStart + 1 > 2 * balancedDelimiter.length()) {
                            tokens.add(new ParsedToken(balancedDelimiterTokenName, startWith, tokenStart, i + 1));
                        }
                    }
                }
            }
            if (blockComment) {
                tokens.add(new ParsedToken(blockCommentTokenName, startWith, tokenStart, line.length()));
            } else if (lineComment) {
                lineComment = false;
                tokens.add(new ParsedToken(lineCommentTokenName, startWith, tokenStart, line.length()));
            } else if (balancedQuoted) {
                tokens.add(new ParsedToken(balancedDelimiterTokenName, startWith, tokenStart, line.length()));
            }
        }

        private CharSequence startWithSubstring(CharSequence line, int pos) {
            return line.subSequence(pos, Math.min(pos + 5, line.length()));
        }

        public List<ParsedToken> getTokens() {
            return tokens;
        }

        private String balancedDelimiter(final CharSequence buffer, final int pos) {
            if (balancedDelimiters != null) {
                for (String delimiter : balancedDelimiters) {
                    if (isDelimiter(buffer, pos, delimiter)) {
                        return delimiter;
                    }
                }
            }
            return null;
        }

        private boolean isDelimiter(final CharSequence buffer, final int pos, final String delimiter) {
            if (pos < 0 || delimiter == null) {
                return false;
            }
            final int length = delimiter.length();
            if (length <= buffer.length() - pos) {
                for (int i = 0; i < length; i++) {
                    if (delimiter.charAt(i) != buffer.charAt(pos + i)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        private boolean isLineCommentDelimiter(final CharSequence buffer, final int pos) {
            if (lineCommentDelimiters != null) {
                for (String delimiter : lineCommentDelimiters) {
                    if (isDelimiter(buffer, pos, delimiter)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean isEscapeChar(char ch) {
            return escapeChar == ch;
        }

        /**
         * Check if this character is a valid escape char (i.e. one that has not been escaped)
         *
         * @param buffer
         *          the buffer to check in
         * @param pos
         *          the position of the character to check
         * @return true if the character at the specified position in the given buffer is an escape
         *         character and the character immediately preceding it is not an escape character.
         */
        private boolean isEscapeChar(final CharSequence buffer, final int pos) {
            if (pos < 0) {
                return false;
            }
            char ch = buffer.charAt(pos);
            return isEscapeChar(ch) && !isEscaped(buffer, pos);
        }

        /**
         * Check if a character is escaped (i.e. if the previous character is an escape)
         *
         * @param buffer
         *          the buffer to check in
         * @param pos
         *          the position of the character to check
         * @return true if the character at the specified position in the given buffer is an escape
         *         character and the character immediately preceding it is an escape character.
         */
        private boolean isEscaped(final CharSequence buffer, final int pos) {
            if (pos <= 0) {
                return false;
            }
            return isEscapeChar(buffer, pos - 1);
        }

        /**
         * Perform Posix/Java regex fixups. This function parses the given regex and escapes according to these rules:
         *
         * <p>The first {@code ]} in a bracket expression does not need to be escaped in Posix,translate to {@code \]}.
         *
         * <p>Same as above for a negating bracket expression like {@code [^][]}, translate to {@code [^\]\[]}.
         *
         * <p>Any {@code [} in a bracket expression does not need to be escaped in Posix, translate to {@code \[}.
         *
         * <p>Any {@code ]} not in a bracket expression is valid in both Posix and Java, no translation.
         *
         * <p>A backslash before the closing bracket like {@code [.f\]} is not an escape of the closing bracket,
         * the backslash needs to be escaped for Java, translate to {@code [.f\\]}.
         *
         * <p>Do not perform the above translations within an escape via {@code \}, except for {@code \<} and {@code \>} to {@code \b}.
         *
         * <p>Replace the Posix classes like {@code [:space:]} or {@code [:digit:]} to Java classes, inside and outside a bracket expression.
         *
         * @param posix Posix regex
         * @return Java regex
         */
        static String fixRegexes(String posix) {
            int len = posix.length();
            StringBuilder java = new StringBuilder();

            boolean inBracketExpression = false;

            int i = 0;
            char next;
            try {
                for (; i < len; i++) {
                    char c = posix.charAt(i);

                    switch (c) {
                        case escapeChar:
                            next = posix.charAt(++i);
                            // Don't translate anything after the \ character escape
                            if (inBracketExpression && next == ']') {
                                inBracketExpression = false;
                                java.append("\\\\").append(next);
                            } else {
                                // Translate '\<' and '\>' to '\b'
                                if (next == '<' || next == '>') {
                                    next = 'b';
                                }
                                java.append(c).append(next);
                            }
                            break;
                        case '[':
                            if (i == len - 1) {
                                throw new IllegalArgumentException("Lone [ at the end of (index " + i + "): " + posix);
                            }
                            // Handle "single bracket" Posix "classes" like [:space:] or [:digit:]
                            if (posix.charAt(i + 1) == ':') {
                                int afterClass = nextAfterClass(posix, i + 2);
                                if (!posix.regionMatches(afterClass, ":]", 0, 2)) {
                                    java.append("[:");
                                    i++;
                                    inBracketExpression = true;
                                } else {
                                    String className = posix.substring(i + 2, afterClass);
                                    java.append(replaceClass(className));
                                    i = afterClass + 1;
                                }
                                break;
                            }
                            if (inBracketExpression) {
                                // Translate lone [ to \[
                                java.append('\\').append(c);
                            } else {
                                inBracketExpression = true;
                                java.append(c);
                                next = posix.charAt(i + 1);
                                if (next == ']') {
                                    i++;
                                    java.append("\\]");
                                } else if (next == '^' && posix.charAt(i + 2) == ']') {
                                    i += 2;
                                    java.append("^\\]");
                                }
                            }
                            break;
                        case ']':
                            inBracketExpression = false;
                            java.append(c);
                            break;
                        default:
                            java.append(c);
                            break;
                    }
                }
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Posix-to-Java regex translation failed around index " + i + " of: " + posix, e);
            }

            return java.toString();
        }

        private static String replaceClass(String className) {
            switch (className) {
                case "alnum":
                    return "\\p{Alnum}";
                case "alpha":
                    return "\\p{Alpha}";
                case "blank":
                    return "\\p{Blank}";
                case "cntrl":
                    return "\\p{Cntrl}";
                case "digit":
                    return "\\p{Digit}";
                case "graph":
                    return "\\p{Graph}";
                case "lower":
                    return "\\p{Lower}";
                case "print":
                    return "\\p{Print}";
                case "punct":
                    return "\\p{Punct}";
                case "space":
                    return "\\s";
                case "upper":
                    return "\\p{Upper}";
                case "xdigit":
                    return "\\p{XDigit}";
            }
            throw new IllegalArgumentException("Unknown class '" + className + "'");
        }

        private static int nextAfterClass(String s, int idx) {
            if (s.charAt(idx) == ':') {
                idx++;
            }
            while (true) {
                char c = s.charAt(idx);
                if (!Character.isLetterOrDigit(c)) {
                    break;
                }
                idx++;
            }
            return idx;
        }
    }
}
