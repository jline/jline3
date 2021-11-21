/*
 * Copyright (c) 2002-2021, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import org.jline.utils.*;

import java.io.*;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 *  Java implementation of nanorc highlighter
 *
 *  @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public class SyntaxHighlighter {
    private final List<HighlightRule> rules = new ArrayList<>();
    private boolean startEndHighlight;
    private int ruleStartId = 0;

    private SyntaxHighlighter() {}

    protected static SyntaxHighlighter build(List<Path> syntaxFiles, String file, String syntaxName) {
        return build(syntaxFiles, file, syntaxName, false);
    }

    protected static SyntaxHighlighter build(List<Path> syntaxFiles, String file, String syntaxName
            , boolean ignoreErrors) {
        SyntaxHighlighter out = new SyntaxHighlighter();
        List<HighlightRule> defaultRules = new ArrayList<>();
        Map<String, String> colorTheme = new HashMap<>();
        try {
            if (syntaxName == null || !syntaxName.equals("none")) {
                for (Path p : syntaxFiles) {
                    try {
                        if (colorTheme.isEmpty() && p.getFileName().toString().endsWith(".nanorctheme")) {
                            try (BufferedReader reader = new BufferedReader(new FileReader(p.toFile()))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    line = line.trim();
                                    if (line.length() > 0 && !line.startsWith("#")) {
                                        List<String> parts = Arrays.asList(line.split("\\s+", 2));
                                        colorTheme.put(parts.get(0), parts.get(1));
                                    }
                                }
                            }
                        } else {
                            NanorcParser parser = new NanorcParser(p, syntaxName, file, colorTheme);
                            parser.parse();
                            if (parser.matches()) {
                                out.addRules(parser.getHighlightRules());
                                return out;
                            } else if (parser.isDefault()) {
                                defaultRules.addAll(parser.getHighlightRules());
                            }
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                }
                out.addRules(defaultRules);
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
        SyntaxHighlighter out = new SyntaxHighlighter();
        List<Path> syntaxFiles = new ArrayList<>();
        try {
            try (BufferedReader reader = new BufferedReader(new FileReader(nanorc.toFile()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.length() > 0 && !line.startsWith("#")) {
                        List<String> parts = RuleSplitter.split(line);
                        if (parts.get(0).equals("include")) {
                            if (parts.get(1).contains("*") || parts.get(1).contains("?")) {
                                PathMatcher pathMatcher = FileSystems
                                        .getDefault().getPathMatcher("glob:" + parts.get(1));
                                Files.find(
                                                Paths.get(new File(parts.get(1)).getParent()),
                                                Integer.MAX_VALUE,
                                                (path, f) -> pathMatcher.matches(path))
                                        .forEach(syntaxFiles::add);
                            } else {
                                syntaxFiles.add(Paths.get(parts.get(1)));
                            }
                        } else if(parts.get(0).equals("theme")) {
                            if (parts.get(1).contains("*") || parts.get(1).contains("?")) {
                                PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + parts.get(1));
                                Optional<Path> theme = Files.find(Paths.get(new File(parts.get(1)).getParent()), Integer.MAX_VALUE, (path, f) -> pathMatcher.matches(path))
                                        .findFirst();
                                theme.ifPresent(path -> syntaxFiles.add(0, path));
                            } else {
                                syntaxFiles.add(0, Paths.get(parts.get(1)));
                            }
                        }
                    }
                }
            }
            out = build(syntaxFiles, null, syntaxName);
        } catch (Exception e) {
            // ignore
        }
        return out;
    }

    /**
     * Build SyntaxHighlighter
     *
     * @param nanorcUrl     Url of nanorc file
     * @return              SyntaxHighlighter
     */
    public static SyntaxHighlighter build(String nanorcUrl) {
        SyntaxHighlighter out = new SyntaxHighlighter();
        InputStream inputStream;
        try {
            if (nanorcUrl.startsWith("classpath:")) {
                inputStream = new Source.ResourceSource(nanorcUrl.substring(10), null).read();
            } else {
                inputStream = new Source.URLSource(new URL(nanorcUrl), null).read();
            }
            NanorcParser parser = new NanorcParser(inputStream, null, null);
            parser.parse();
            out.addRules(parser.getHighlightRules());
        } catch (IOException e) {
            // ignore
        }
        return out;
    }

    private void addRules(List<HighlightRule> rules) {
        this.rules.addAll(rules);
    }

    public SyntaxHighlighter reset() {
        ruleStartId = 0;
        startEndHighlight = false;
        return this;
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
        for (AttributedString s : attributedString.columnSplitLength(Integer.MAX_VALUE)) {
            if (!first) {
                asb.append("\n");
            }
            asb.append(_highlight(s));
            first = false;
        }
        return asb.toAttributedString();
    }

    private AttributedStringBuilder _highlight(AttributedString line) {
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
                                a.append(_highlight(asb.columnSubSequence(end.end(), asb.length()).toAttributedString()));
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
            }
        }
        return asb;
    }

    private static class HighlightRule {
        public enum RuleType {PATTERN, START_END}
        private final RuleType type;
        private Pattern pattern;
        private final AttributedStyle style;
        private Pattern start;
        private Pattern end;

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

        public static RuleType evalRuleType(List<String> colorCfg) {
            RuleType out = null;
            if (colorCfg.get(0).equals("color") || colorCfg.get(0).equals("icolor")) {
                out = RuleType.PATTERN;
                if (colorCfg.size() == 4 && colorCfg.get(2).startsWith("start=") && colorCfg.get(3).startsWith("end=")) {
                    out = RuleType.START_END;
                }
            }
            return out;
        }

    }

    private static class NanorcParser {
        private static final String DEFAULT_SYNTAX = "default";
        private final String name;
        private final String target;
        private final List<HighlightRule> highlightRules = new ArrayList<>();
        private final BufferedReader reader;
        private Map<String, String> colorTheme = new HashMap<>();
        private boolean matches = false;
        private String syntaxName = "unknown";

        public NanorcParser(Path file, String name, String target, Map<String, String> colorTheme) throws IOException {
            this(new Source.PathSource(file, null).read(), name, target);
            this.colorTheme = colorTheme;
        }

        public NanorcParser(InputStream in, String name, String target) {
            this.reader = new BufferedReader(new InputStreamReader(in));
            this.name = name;
            this.target = target;
        }

        public void parse() throws IOException {
            String line;
            int idx = 0;
            while ((line = reader.readLine()) != null) {
                idx++;
                line = line.trim();
                if (line.length() > 0 && !line.startsWith("#")) {
                    List<String> parts = RuleSplitter.split(fixRegexes(line));
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
                    } else if (!addHighlightRule(parts, idx) && parts.get(0).matches("\\+[A-Z_]+")) {
                        String key = themeKey(parts.get(0));
                        if (colorTheme.containsKey(key)) {
                            for (String l : colorTheme.get(key).split("\\\\n")) {
                                idx++;
                                addHighlightRule(RuleSplitter.split(fixRegexes(l)), idx);
                            }
                        } else {
                            Log.warn("Unknown token type: ", key);
                        }
                    }
                }
            }
            reader.close();
        }

        private String fixRegexes(String line) {
            return line.replaceAll("\\\\<", "\\\\b")
                    .replaceAll("\\\\>", "\\\\b")
                    .replaceAll("\\[:alnum:]", "\\\\p{Alnum}")
                    .replaceAll("\\[:alpha:]", "\\\\p{Alpha}")
                    .replaceAll("\\[:blank:]", "\\\\p{Blank}")
                    .replaceAll("\\[:cntrl:]", "\\\\p{Cntrl}")
                    .replaceAll("\\[:digit:]", "\\\\p{Digit}")
                    .replaceAll("\\[:graph:]", "\\\\p{Graph}")
                    .replaceAll("\\[:lower:]", "\\\\p{Lower}")
                    .replaceAll("\\[:print:]", "\\\\p{Print}")
                    .replaceAll("\\[:punct:]", "\\\\p{Punct}")
                    .replaceAll("\\[:space:]", "\\\\s")
                    .replaceAll("\\[:upper:]", "\\\\p{Upper}")
                    .replaceAll("\\[:xdigit:]", "\\\\p{XDigit}");
        }

        private boolean addHighlightRule(List<String> parts, int idx) {
            boolean out = true;
            if (parts.get(0).equals("color")) {
                addHighlightRule(syntaxName + idx, parts, false);
            } else if (parts.get(0).equals("icolor")) {
                addHighlightRule(syntaxName + idx, parts, true);
            } else if (parts.get(0).matches("[A-Z_]+[:]?")) {
                String key = themeKey(parts.get(0));
                if (colorTheme.containsKey(key)) {
                    parts.set(0, "color");
                    parts.add(1, colorTheme.get(key));
                    addHighlightRule(syntaxName + idx, parts, false);
                } else {
                    Log.warn("Unknown token type: ", key);
                }
            } else if (parts.get(0).matches("~[A-Z_]+[:]?")) {
                String key = themeKey(parts.get(0));
                if (colorTheme.containsKey(key)) {
                    parts.set(0, "icolor");
                    parts.add(1, colorTheme.get(key));
                    addHighlightRule(syntaxName + idx, parts, true);
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
                    return  key.substring(1, keyEnd);
                }
                return key.substring(0, keyEnd);
            }
        }

        public boolean matches() {
            return matches;
        }

        public List<HighlightRule> getHighlightRules() {
            return highlightRules;
        }

        public boolean isDefault() {
            return syntaxName.equals(DEFAULT_SYNTAX);
        }

        private void addHighlightRule(String reference, List<String> parts, boolean caseInsensitive) {
            Map<String,String> spec = new HashMap<>();
            spec.put(reference, parts.get(1));
            Styles.StyleCompiler sh = new Styles.StyleCompiler(spec, true);
            AttributedStyle style = new StyleResolver(sh::getStyle).resolve("." + reference);

            if (HighlightRule.evalRuleType(parts) == HighlightRule.RuleType.PATTERN) {
                for (int i = 2; i < parts.size(); i++) {
                    highlightRules.add(new HighlightRule(style, doPattern(parts.get(i), caseInsensitive)));
                }
            } else if (HighlightRule.evalRuleType(parts) == HighlightRule.RuleType.START_END) {
                String s = parts.get(2);
                String e = parts.get(3);
                highlightRules.add(new HighlightRule(style
                        , doPattern(s.substring(7, s.length() - 1), caseInsensitive)
                        , doPattern(e.substring(5, e.length() - 1), caseInsensitive)));
            }
        }

        private Pattern doPattern(String regex, boolean caseInsensitive) {
            return caseInsensitive ? Pattern.compile(regex, Pattern.CASE_INSENSITIVE)
                    : Pattern.compile(regex);
        }

    }

    protected static class RuleSplitter {
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

}

