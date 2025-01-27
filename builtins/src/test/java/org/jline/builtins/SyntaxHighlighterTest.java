/*
 * Copyright (c) 2002-2017, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class SyntaxHighlighterTest {

    static String fixRegexesOld(String line) {
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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    void checkRegexFix(String posix, String expectedJava) {
        String java = SyntaxHighlighter.Parser.fixRegexes(posix);

        expectedJava = fixRegexesOld(expectedJava);

        Assertions.assertEquals(expectedJava, java);
        // should not throw
        Pattern.compile(java);
    }

    @ParameterizedTest
    @MethodSource
    public void regexFixBrackets(String posix, String expectedJava) {
        checkRegexFix(posix, expectedJava);
    }

    @ParameterizedTest
    @MethodSource
    public void regexFixBackslash(String posix, String expectedJava) {
        checkRegexFix(posix, expectedJava);
    }

    @ParameterizedTest
    @MethodSource("unescaped")
    public void regexFixUnescaped(String posix) {
        checkRegexFix(posix, posix);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @ParameterizedTest
    @MethodSource("unescaped")
    public void unescapedChecksWork(String posix) {
        // should not throw
        Pattern.compile(posix);
    }

    @ParameterizedTest
    @MethodSource
    @Disabled("There are two failing expressions, which look suspicious (unclosed groups)")
    public void failing(String posix) {
        checkRegexFix(posix, posix);
    }

    static Stream<String> failing() {
        return Stream.of(
                // java.util.regex.PatternSyntaxException: Unmatched closing ')' near index 46
                //  @(encode|end|implementation|interface)|selector)\>
                //                                                ^
                "@(encode|end|implementation|interface)|selector)\\>",
                // java.util.regex.PatternSyntaxException: Unmatched closing ')' near index 100
                // ^deb(-src)?\s+.*(mirror\+)?(ftp|https?|rsh|ssh|copy|file|in-toto|s3|spacewalk|tor):/\S+|cdrom:\[.+\]/)\s+\S+
                //                                                                                                     ^
                "^deb(-src)?\\s+.*(mirror\\+)?(ftp|https?|rsh|ssh|copy|file|in-toto|s3|spacewalk|tor):/\\S+|cdrom:\\[.+\\]/)\\s+\\S+");
    }

    // Reads in all .nanorc files on the local machine and pushes those through NanorcParser.
    // Using System.out here is a bit unorhodox, but we cannot really assert on content that's present on a
    // local machine. Putting a set of .nanorc files into the code base would pull in GPL licensed elements.
    @ParameterizedTest
    @MethodSource
    @EnabledOnOs(OS.LINUX)
    void processLocalNanorcFile(Path nanorcFile) throws Exception {
        Map<String, String> colorTheme = new HashMap<>();
        String fileName = nanorcFile.getFileName().toString().replaceAll("[.].*", "");
        String syntaxName;
        switch (fileName) {
            case "debian":
                syntaxName = "sources.list";
                break;
            case "objc":
                syntaxName = "m";
                break;
            default:
                syntaxName = fileName;
        }
        SyntaxHighlighter.NanorcParser nanorcParser =
                new SyntaxHighlighter.NanorcParser(nanorcFile, syntaxName, "syntax", colorTheme);
        nanorcParser.parse();

        Iterator<String> sourceLines = Files.readAllLines(nanorcFile).stream()
                .filter(line -> line.startsWith("color ") || line.startsWith("icolor "))
                .iterator();

        nanorcParser.getHighlightRules().forEach((s, rules) -> {
            System.out.println(s + " / " + syntaxName);
            for (SyntaxHighlighter.HighlightRule rule : rules) {
                System.out.println();
                String sourceLine = sourceLines.hasNext() ? sourceLines.next() : "<oops>";
                System.out.println("  Source line: " + sourceLine);
                int i = sourceLine.indexOf(' ', sourceLine.indexOf(' ') + 1);
                System.out.println("       source: " + sourceLine.substring(i + 1));
                switch (rule.getType()) {
                    case PATTERN:
                        System.out.println("    processed:  " + rule.getPattern());
                        break;
                    case START_END:
                        System.out.println("    processed: " + rule.getStart() + " - " + rule.getEnd());
                        break;
                    case PARSER_CONTINUE_AS:
                        System.out.println("    processed: " + rule.getContinueAs());
                        break;
                    case PARSER_START_WITH:
                        System.out.println("    processed: " + rule.getStartWith());
                        break;
                }
            }
        });
    }

    static List<Path> processLocalNanorcFile() throws Exception {
        try (Stream<Path> list = Files.list(Paths.get("/usr/share/nano"))
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".nanorc"))) {
            return list.collect(Collectors.toList());
        }
    }

    // Code to extract highlight regexes from nanorc files
    @SuppressWarnings("unused")
    void dumpNanoRegexes() throws Exception {
        try (Stream<Path> list = Files.list(Paths.get("/usr/share/nano")).filter(Files::isRegularFile)) {
            list.forEach(p -> {
                try {
                    Files.readAllLines(p).stream()
                            .filter(s -> s.startsWith("color"))
                            .map(s -> {
                                int i = s.indexOf(' ');
                                if (i == -1) {
                                    return null;
                                }
                                i = s.indexOf(' ', i + 1);
                                if (i == -1) {
                                    return null;
                                }
                                s = s.substring(i + 1).trim();
                                if (s.startsWith("\"") && s.endsWith("\"")) {
                                    return s.substring(1, s.length() - 1);
                                }
                                return null;
                            })
                            .filter(Objects::nonNull)
                            .map(s -> s.replace("\\", "\\\\").replace("\"", "\\\""))
                            .map(s -> '\"' + s + "\" ,")
                            .sorted()
                            .forEachOrdered(System.out::println);
                    ;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    static Stream<Arguments> regexFixBrackets() {
        return Stream.of(
                arguments("[][]", "[\\]\\[]"),
                arguments("[]{}[]", "[\\]{}\\[]"),
                arguments("[][][][]", "[\\]\\[][\\]\\[]"),
                arguments("\\[[][]", "\\[[\\]\\[]"),
                arguments("\\[[][]", "\\[[\\]\\[]"),
                arguments(
                        " 0(o[0-7]+|x[[:xdigit:]]+)( *[]}]|, | +#|$)", " 0(o[0-7]+|x[[:xdigit:]]+)( *[\\]}]|, | +#|$)"),
                arguments(
                        "[:,] +(Y(es)?|No?|y(es)?|no?|[Tt]rue|[Ff]alse|[Oo](n|ff))( *[]}]|, | +#|$)",
                        "[:,] +(Y(es)?|No?|y(es)?|no?|[Tt]rue|[Ff]alse|[Oo](n|ff))( *[\\]}]|, | +#|$)"),
                arguments(
                        "[:,] +[+-]?[0-9]+(\\.([0-9]+)?)?( *[]}]|, | +#|$)",
                        "[:,] +[+-]?[0-9]+(\\.([0-9]+)?)?( *[\\]}]|, | +#|$)"),
                arguments(
                        "\\^[]/4-8@A-Z\\^_`◂▸▴▾-]\"  \"[◂▸▴▾]\"  \"\\<(M|S[Hh]-[Mm])-[^\")”»“」]\"  \"\\<F([1-9]|1[0-9]|2[0-4])",
                        "\\^[\\]/4-8@A-Z\\^_`◂▸▴▾-]\"  \"[◂▸▴▾]\"  \"\\<(M|S[Hh]-[Mm])-[^\")”»“」]\"  \"\\<F([1-9]|1[0-9]|2[0-4])"));
    }

    static Stream<Arguments> regexFixBackslash() {
        return Stream.of(
                arguments(
                        "'([^'\\]|\\\\(\\[\"'\\abfnrtv]|x[[:xdigit:]]{1,2}|[0-3]?[0-7]{1,2}))'",
                        "'([^'\\\\]|\\\\(\\[\"'\\abfnrtv]|x[[:xdigit:]]{1,2}|[0-3]?[0-7]{1,2}))'"),
                arguments("'([^'\\]|\\\\.)'", "'([^'\\\\]|\\\\.)'"),
                arguments(
                        "'([^'\\]|\\\\.)*'|\"([^\"\\]|\\\\.)*\"|'''|\"\"\"",
                        "'([^'\\\\]|\\\\.)*'|\"([^\"\\\\]|\\\\.)*\"|'''|\"\"\""),
                arguments("(^|[[:blank:]])[$%@][/\\]", "(^|[[:blank:]])[$%@][/\\\\]"),
                arguments("(^|[^\\])%.*", "(^|[^\\\\])%.*"),
                arguments("<[^= 	]*>|\"([^\"\\]|\\\\.)*\"", "<[^= 	]*>|\"([^\"\\\\]|\\\\.)*\""),
                arguments("@\"([^\"\\]|\\\\.)*\"", "@\"([^\"\\\\]|\\\\.)*\""),
                arguments(
                        "[$%&@]([A-Za-z_][0-9A-Za-z_]*|\\^[][A-Z?\\^_]|[0-9]+)\\>",
                        "[$%&@]([A-Za-z_][0-9A-Za-z_]*|\\^[\\]\\[A-Z?\\^_]|[0-9]+)\\>"),
                arguments(
                        "[$%&@]([][!\"#'()*+,.:;<=>?`|~-]|\\{[][!-/:-@\\`|~]\\})|\\$[$%&@]",
                        "[$%&@]([\\]\\[!\"#'()*+,.:;<=>?`|~-]|\\{[\\]\\[!-/:-@\\`|~]\\})|\\$[$%&@]"),
                arguments(
                        "[$%&@]\\{(\\^?[A-Za-z_][0-9A-Za-z_]*|\\^[][?\\^][0-9]+)\\}",
                        "[$%&@]\\{(\\^?[A-Za-z_][0-9A-Za-z_]*|\\^[\\]\\[?\\^][0-9]+)\\}"),
                arguments("[][{}():;|`$<>!=&\\]", "[\\]\\[{}():;|`$<>!=&\\\\]"),
                arguments("[][{}():;|`$<>!=&\\]", "[\\]\\[{}():;|`$<>!=&\\\\]"),
                arguments("\"([^\"\\]|\\\\.)*\"", "\"([^\"\\\\]|\\\\.)*\""),
                arguments("\"([^\"\\]|\\\\.)*\"|'([^'\\]|\\\\.)*'", "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'"),
                arguments(
                        "\"([^\"\\]|\\\\.)*\"|'([^'\\]|\\\\.)*'|`([^`\\]|\\\\.)*`",
                        "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'|`([^`\\\\]|\\\\.)*`"),
                arguments("\"([^\"\\]|\\\\.)*\"|'([^'\\]|\\\\.)+'", "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)+'"),
                arguments("\"([^\"\\]|\\\\.)*\"|<[^=         ]*>", "\"([^\"\\\\]|\\\\.)*\"|<[^=         ]*>"),
                arguments("\"[^\"\\]*\"", "\"[^\"\\\\]*\""),
                arguments(
                        "\\\\([abcefnrtv\"\\]|x[[:xdigit:]]{2}|[0-7]{3})",
                        "\\\\([abcefnrtv\"\\\\]|x[[:xdigit:]]{2}|[0-7]{3})"),
                arguments(
                        "\\^[A-Z^\\]|\\<M-.|\\<F1?[0-9]|(\\^|M-)Space",
                        "\\^[A-Z^\\\\]|\\<M-.|\\<F1?[0-9]|(\\^|M-)Space"),
                arguments(
                        "^[[:blank:]]*bind[[:blank:]]+((\\^([A-Za-z]|[]/@\\^_`-]|Space)|([Ss][Hh]-)?[Mm]-[A-Za-z]|[Mm]-([][!\"#$%&'()*+,./0-9:;<=>?@\\^_`{|}~-]|Space))|F([1-9]|1[0-9]|2[0-4])|Ins|Del)[[:blank:]]+([a-z]+|\".*\")[[:blank:]]+(main|help|search|replace(with)?|yesno|gotoline|writeout|insert|execute|browser|whereisfile|gotodir|spell|linter|all)\\>",
                        "^[[:blank:]]*bind[[:blank:]]+((\\^([A-Za-z]|[\\]/@\\^_`-]|Space)|([Ss][Hh]-)?[Mm]-[A-Za-z]|[Mm]-([\\]\\[!\"#$%&'()*+,./0-9:;<=>?@\\^_`{|}~-]|Space))|F([1-9]|1[0-9]|2[0-4])|Ins|Del)[[:blank:]]+([a-z]+|\".*\")[[:blank:]]+(main|help|search|replace(with)?|yesno|gotoline|writeout|insert|execute|browser|whereisfile|gotodir|spell|linter|all)\\>"),
                arguments(
                        "^[[:blank:]]*unbind[[:blank:]]+((\\^([A-Za-z]|[]/@\\^_`-]|Space)|([Ss][Hh]-)?[Mm]-[A-Za-z]|[Mm]-([][!\"#$%&'()*+,./0-9:;<=>?@\\^_`{|}~-]|Space))|F([1-9]|1[0-9]|2[0-4])|Ins|Del)[[:blank:]]+(main|help|search|replace(with)?|yesno|gotoline|writeout|insert|execute|browser|whereisfile|gotodir|spell|linter|all)\\>",
                        "^[[:blank:]]*unbind[[:blank:]]+((\\^([A-Za-z]|[\\]/@\\^_`-]|Space)|([Ss][Hh]-)?[Mm]-[A-Za-z]|[Mm]-([\\]\\[!\"#$%&'()*+,./0-9:;<=>?@\\^_`{|}~-]|Space))|F([1-9]|1[0-9]|2[0-4])|Ins|Del)[[:blank:]]+(main|help|search|replace(with)?|yesno|gotoline|writeout|insert|execute|browser|whereisfile|gotodir|spell|linter|all)\\>"),
                arguments("^\\[[^][]+\\]$", "^\\[[^\\]\\[]+\\]$"),
                arguments("!?\\[[^]]+\\]", "!?\\[[^\\]]+\\]"),
                arguments(
                        "'([^']|\\\\')*'|%[qw](\\{[^}]*\\}|\\([^)]*\\)|<[^>]*>|\\[[^]]*\\]|\\$[^$]*\\$|\\^[^^]*\\^|![^!]*!)",
                        "'([^']|\\\\')*'|%[qw](\\{[^}]*\\}|\\([^)]*\\)|<[^>]*>|\\[[^\\]]*\\]|\\$[^$]*\\$|\\^[^^]*\\^|![^!]*!)"),
                arguments(
                        "\"([^\"]|\\\\\")*\"|%[QW]?(\\{[^}]*\\}|\\([^)]*\\)|<[^>]*>|\\[[^]]*\\]|\\$[^$]*\\$|\\^[^^]*\\^|![^!]*!)",
                        "\"([^\"]|\\\\\")*\"|%[QW]?(\\{[^}]*\\}|\\([^)]*\\)|<[^>]*>|\\[[^\\]]*\\]|\\$[^$]*\\$|\\^[^^]*\\^|![^!]*!)"),
                arguments("\\<https?://\\S+\\.\\S+[^])>[:space:],.]", "\\<https?://\\S+\\.\\S+[^\\])>[:space:],.]"),
                arguments("\\[[^][:blank:]]*\\]", "\\[[^\\][:blank:]]*\\]"),
                arguments("\\[[^]]+\\]\\([^)]+\\)", "\\[[^\\]]+\\]\\([^)]+\\)"));
    }

    static Stream<String> unescaped() {
        return Stream.of(
                "  $",
                " !!(binary|bool|float|int|map|null|omap|seq|set|str)( |,|$)",
                " ![^! 	][^ 	]*( |$)",
                " (:?:|\\+|\\?)?= ",
                " (no-)?[-[:alpha:]]+-format(,|$)",
                " +	+",
                " [&*](\\w|-)+( |$)",
                " [12][0-9]{3}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])",
                " [|>]([1-9]?[+-]|[+-][1-9]?)?$",
                " \\(|\\) ",
                " fuzzy(,|$)",
                "##([^{].*|$)",
                "#([^{#].*|$)",
                "#.*",
                "#?'\\<(\\w|-)+\\>",
                "#?:(\\w|[?-])+",
                "#\\\\(.|\\w+)",
                "#\\{[^}]*\\}",
                "#~.*",
                "%([1-9]\\$)?[#0 +'I-]?(\\*([1-9]\\$)?|[1-9](\\.[0-9]?)?)?[hlLjzt]?[diouxXeEfFgGaAcspnm%]",
                "&(amp|apos|gt|lt|quot);",
                "&[^; ]+;",
                "&[^;[:space:]]*;",
                "'''|\"\"\"",
                "'([^']|\\\\')*'",
                "'(\\^|M-)'",
                "'\\<(\\w|-)+\\>",
                "(#t|#f)\\>",
                "(--|//).*",
                "(/([^/]|\\\\/)*/|%r\\{([^}]|\\\\\\})*\\})[iomx]*",
                "(<\\?(php|=)?|\\?>)",
                "(BZ|bug|patch)[ ]#[0-9]+|PR [[:alnum:]]+/[0-9]+",
                "([ 	]|^):[0-9A-Za-z_]+\\>",
                "(\"([^\"]|\\\\\")+\"|'[^']+')",
                "(\\$|@)[[:alpha:]_-][[:alnum:]_.-]*",
                "(\\w|::|[/.-])+:( |$)",
                "(^\\.)?\\\\\".*",
                "(^| )#.*",
                "(^|[[:blank:]])#.*",
                "(^|[[:blank:]])//.*",
                "(^|[[:blank:]]);.*",
                "(^|[[:blank:]])\\[.*[[:blank:]].*\\]",
                "(^|[[:blank:]]+)#.*",
                "(^|[[:blank:]]+)(//|#).*",
                "(^|[[:blank:]]+)//.*",
                "(https?|ftp)://\\S+\\.\\S+[^[:space:].)]",
                "-(eq|ne|gt|lt|ge|le|ef|ot|nt)\\>",
                "--.*",
                "->|<-|=>",
                ".",
                ".*",
                "//.*",
                "//[[:blank:]]*\\+build[[:blank:]]+(([a-zA-Z_0-9]+[[:blank:]]*)+,[[:blank:]]*)*[a-zA-Z_0-9]+",
                "//[^\"]*$|(^|[[:blank:]])//.*",
                ":(\\w|[?-])+",
                ":(active|checked|focus|hover|link|visited|after|before)\\>",
                ":(close|flush|lines|read|seek|setvbuf|write)\\>",
                ":[[:alnum:]]*",
                ":[[:blank:]]*$",
                ":[[:blank:]]*\"#[[:xdigit:]]+\"",
                ":[[:blank:]]*\\-?(0|[1-9][0-9]*)(\\.[0-9]+)?([Ee]?[-+]?[0-9]+)?",
                ":|\\*|/|%|\\+|-|\\^|>|>=|<|<=|~=|=|\\.\\.|#|\\<(not|and|or)\\>",
                ";|:|\\{|\\}",
                "<-[[:blank:]]*chan\\>|\\<chan[[:blank:]]*<-",
                "</?(b|i|u|em|strong)>",
                "<[[:alnum:].%_+-]+@[[:alnum:].-]+\\.[[:alpha:]]{2,}>",
                "<[[:alpha:]/!?][^>]*>",
                "<[^> ]+|/?>",
                "<[^>]*@[^>]*>",
                "<[^>]+>",
                "<[^@]+@[^@]+>",
                "<\\?.+\\?>|<!DOCTYPE[^>]+>|\\]>",
                "==|/=|&&|\\|\\||<|>|<=|>=",
                "=|!=|&&|\\|\\|",
                "@([a-z]+|,|H|U)\\{([^}]|@\\}|@[a-z]+\\{[^}]*\\})*\\}",
                "@[!\"'&*./:=?@\\^`{}~-]",
                "@[[:alpha:]_][[:alnum:]_.]*",
                "@\\|",
                "@c(omment)?[[:space:]]+.*",
                "Copyright|\\(C\\)",
                "XXX|TODO|FIXME|\\?\\?\\?",
                "XXX|TODO|FIXME|\\?\\?\\?",
                "[!$&();<=>\\`|]",
                "[.-]tar\\>",
                "[:,]( |$)",
                "[A-Z][A-Z_0-9]+",
                "[A-Z][A-Za-z0-9]+",
                "[[:alnum:]]*:",
                "[[:blank:]]",
                "[[:blank:]](-[A-Za-z]|--\\<[A-Za-z-]+)\\>",
                "[[:blank:]](OR|AND|IS_NEWER_THAN|MATCHES|(STR|VERSION_)?(LESS|GREATER|EQUAL))[[:blank:]]",
                "[[:blank:]](start=)?\".+\"",
                "[[:blank:]]-[a-zA-Z\\$]|--[8a-z-]+",
                "[[:cntrl:]]",
                "[[:cntrl:]]| +$",
                "[[:space:]]+$",
                "[a-z_]+!",
                "[smy]/.*/",
                "[{},:]",
                "[{}]",
                "\"",
                "\"([^\"]|\\\\\")*\"",
                "\"([^\"]|\\\\\")*\"|#[[:blank:]]*include[[:blank:]]*<[^>]+>",
                "\".*\"|qq\\|.*\\|",
                "\".+\"",
                "\"[[:alpha:]_][[:alnum:]_$]*\"",
                "\"[^\"]*\"",
                "\"[^\"]*\"",
                "\"[^\"]*\"|'[^']*'",
                "\"[^\"]+\"[[:blank:]]*:",
                "\\$([-@*#?$!0-9]|[[:alpha:]_][[:alnum:]_]*)",
                "\\$+[{(][a-zA-Z0-9_-]+[})]",
                "\\$[0-9A-Za-z_!@#$*?-]+",
                "\\$[A-Za-z_][A-Za-z_0-9]*",
                "\\$\\{?[0-9A-Za-z_!@#$*?-]+\\}?",
                "\\$\\{[#!]?([-@*#?$!]|[0-9]+|[[:alpha:]_][[:alnum:]_]*)(\\[([[:blank:]]*[[:alnum:]_]+[[:blank:]]*|@)\\])?(([#%/]|:?[-=?+])[^}]*\\}|\\[|\\})",
                "\\(M-(\\)|\")\\)",
                "\\(|\\)|\\[|\\]|\\{|\\}",
                "\\*[^* 	][^*]*\\*|_[^_ 	][^_]*_",
                "\\*\\*[^*]+\\*\\*|__[^_]+__",
                "\\.(align|file|globl|global|hidden|section|size|type|weak)",
                "\\.(ascii|asciz|byte|double|float|hword|int|long|short|single|struct|word)",
                "\\.(data|subsection|text)",
                "\\.\\.\\.",
                "\\.|\\$",
                "\\<((Sh-)?Tab|Enter|Ins|(Sh-\\^?)?Del|Space|Bsp|Up|Down|Left|Right|Home|End|PgUp|PgDn)\\>",
                "\\<(([gs]et|end)(pw|gr|host|net|proto|serv)ent|getsock(name|opt)|glob|gmtime|grep|hex|import|index|int|ioctl|join)\\>",
                "\\<((pre|rc)?[0-9]+|[0-9]bit)\\>",
                "\\<(APPLE|UNIX|WIN32|CYGWIN|BORLAND|MINGW|MSVC(_IDE|60|71|80|90)?)\\>",
                "\\<(ARGC|ARGIND|ARGV|BINMODE|CONVFMT|ENVIRON|ERRNO|FIELDWIDTHS)\\>",
                "\\<(BEGIN|END|alias|and|begin|break|case|class|def|defined\\?|do|else|elsif|end|ensure|false|for|if|in|module)\\>",
                "\\<(Bounded|Data|Enum|Eq|Floating|Fractional|Functor|Integral|Monad|MonadPlus|Num|Ord|Read|Real|RealFloat|RealFrac|Show|Typeable)\\>",
                "\\<(FILENAME|FNR|FS|IGNORECASE|LINT|NF|NR|OFMT|OFS|ORS)\\>",
                "\\<(FIXME|TODO|XXX)\\>",
                "\\<(False|None|True)\\>",
                "\\<(GNU )?[Nn]ano [1-8]\\.[0-9][-.[:alnum:]]*\\>",
                "\\<(NOT|COMMAND|POLICY|TARGET|EXISTS|IS_(DIRECTORY|ABSOLUTE)|DEFINED)[[:blank:]]",
                "\\<(POT-Creation-Date|PO-Revision-Date|MIME-Version|Content-Type|Content-Transfer-Encoding)\\>",
                "\\<(PROCINFO|RS|RT|RSTART|RLENGTH|SUBSEP|TEXTDOMAIN)\\>",
                "\\<(Project-Id-Version|Report-Msgid-Bugs-To|Last-Translator|Language(-Team)?|X-Bugs|X-Generator|Plural-Forms)\\>",
                "\\<(SQL|pl(java|perlu?|pgsql|py|pythonu?|r|ruby|scheme|sh|tcl))\\>",
                "\\<(True|False|Nothing|Just|Left|Right|LT|EQ|GT)\\>",
                "\\<(UTF|ISO|Windows|Mac|IBM)-[0-9]+",
                "\\<([0-9]+|0x[[:xdigit:]]+)\\>",
                "\\<([1-9][0-9]*|0[0-7]*|0[xX][[:xdigit:]]+)\\>",
                "\\<([[:lower:]][[:lower:]_]*|(u_?)?int(8|16|32|64))_t\\>",
                "\\<(_(Alignas|Alignof|Atomic|Bool|Complex|Generic|Imaginary|Noreturn|Static_assert|Thread_local))\\>",
                "\\<(_G|_VERSION|assert|collectgarbage|dofile|error|getfenv|getmetatable|ipairs|load|loadfile|module|next|pairs|pcall|print|rawequal|rawget|rawlen|rawset|require|select|setfenv|setmetatable|tonumber|tostring|type|unpack|xpcall)[[:blank:]]*\\(",
                "\\<(__FILE__|__LINE__)\\>",
                "\\<(abbr|accept(-charset)?|accesskey|action|alink|align|alt|archive|axis|background|bgcolor|border)=",
                "\\<(abstract|as|async|await|become|box|break|const|continue|crate|do|dyn|else|enum|extern|false|final|fn|for|if|impl|in|let|loop|macro|match|mod|move|mut|override|priv|pub|ref|return|self|static|struct|super|trait|true|try|type|typeof|unsafe|unsized|use|virtual|where|while|yield)\\>",
                "\\<(abstract|as|class|clone|(end)?declare|extends|function|implements|include(_once)?|inst(ance|ead)of|interface|namespace|new|private|protected|public|require(_once)?|static|trait|use|yield)\\>",
                "\\<(abstract|class|extends|final|implements|import|instanceof|interface|native)\\>",
                "\\<(abs|accept|alarm|atan2|bin(d|mode)|bless|caller|ch(dir|mod|op|omp|own|r|root)|close(dir)?|connect|cos|crypt)\\>",
                "\\<(accept|continue|(d|s)nat|goto|jump|masquerade|return)\\>",
                "\\<(add|delete|flush|insert|remove|replace)\\>",
                "\\<(after|append|array|auto_(execok|import|load(_index)?|qualify)|binary|break)\\>",
                "\\<(alarm|auto_load_pkg|bsearch|cat(close|gets|open)|ccollate|cconcat|cequal|chgrp|chmod|chown|chroot)\\>",
                "\\<(and|as|assert|async|await|break|class|continue)\\>",
                "\\<(and|cmp|eq|ge|gt|isa|le|lt|ne|not|or|x|xor)\\>",
                "\\<(and|compl|lshift|or|rshift|xor)\\>",
                "\\<(and|or|xor)\\>",
                "\\<(append|cap|close|complex|copy|delete|imag|len|make|new|panic|print|println|real|recover)\\>",
                "\\<(arp|bridge|inet|ingress|ip6?|netdev)\\>",
                "\\<(array|bool|callable|const|float|global|int|object|string|var)\\>",
                "\\<(asort|asorti|gensub|gsub|index|length|match)\\>",
                "\\<(async|class|const|extends|function|let|this|typeof|var|void)\\>",
                "\\<(as|case|of|class|data|default|deriving|do|forall|foreign|hiding|if|then|else|import|infix(l|r)?|instance|let|in|mdo|module|newtype|qualified|type|where)\\>",
                "\\<(as|when|of)\\>",
                "\\<(atan2|cos|exp|int|log|rand|sin|sqrt|srand)\\>",
                "\\<(auto|bool|char|const|double|enum|extern|float|inline|int|long|restrict|short|signed|sizeof|static|struct|typedef|union|unsigned|void)\\>",
                "\\<(await|export|import|throw|try|catch|finally|new|delete)\\>",
                "\\<(awk|cat|cd|ch(grp|mod|own)|cp|cut|echo|env|grep|head|install|ln|make|mkdir|mv|popd|printf|pushd|rm|rmdir|sed|set|sort|tail|tar|touch|umask|unset)\\>",
                "\\<(begin|end|object|struct|sig|for|while|do|done|to|downto)\\>",
                "\\<(bindtextdomain|dcgettext|dcngettext)\\>",
                "\\<(boolean|byte|char|double|float|int|long|new|short|this|transient|void)\\>",
                "\\<(bool|u?int(8|16|32|64)?|float(32|64)|complex(64|128)|byte|rune|uintptr|string|error)\\>",
                "\\<(break|case|catch|continue|default|do|else|finally|for|if|return|switch|throw|try|while)\\>",
                "\\<(break|case|continue|do|done|elif|else|esac|exit|fi|for|function|if|in|read|return|select|shift|then|time|until|while)\\>",
                "\\<(break|continue|fallthrough|goto|return)\\>",
                "\\<(break|continue|goto|return)\\>",
                "\\<(break|continue|goto|return)\\>",
                "\\<(break|continue|return)\\>",
                "\\<(break|continue|return|yield)\\>",
                "\\<(case|catch|cd|clock|close|concat|continue|encoding|eof|error|eval|exec|exit|expr)\\>",
                "\\<(case|catch|default|do|echo|else(if)?|end(for(each)?|if|switch|while)|final(ly)?|for(each)?|if|print|switch|throw|try|while)\\>",
                "\\<(case|default|defer|else|for|go|if|range|select|switch)\\>",
                "\\<(cell(padding|spacing)|char(off|set)?|checked|cite|class(id)?|compact|code(base|tag)?|cols(pan)?)=",
                "\\<(chain|hook|policy|priority|ruleset|set|table|type|v?map)\\>",
                "\\<(chan|const|func|interface|map|struct|type|var)\\>",
                "\\<(cindex|clength|cmdtrace|commandloop|crange|csubstr|ctoken|ctype|dup|echo|execl)\\>",
                "\\<(class|explicit|friend|mutable|namespace|override|private|protected|public|register|template|this|typename|using|virtual|volatile)\\>",
                "\\<(class|namespace|template|public|protected|private|typename|this|friend|virtual|using|mutable|volatile|register|explicit)\\>",
                "\\<(close|fflush|getline|next|nextfile|print|printf|system)\\>",
                "\\<(content(editable)?|contextmenu|coords|data|datetime|declare|defer|dir|disabled|enctype)=",
                "\\<(continue|die|do|else|elsif|exit|for(each)?|fork|goto|if|last|next|return|unless|until|while)\\>",
                "\\<(dbm(close|open)|defined|delete|dump|each|eof|eval(bytes)?|exec|exists|exp|fc|fcntl|fileno|flock|fork|format|formline)\\>",
                "\\<(declare|eval|exec|export|let|local)\\>",
                "\\<(define|include)\\>",
                "\\<(def|del|elif|else|except|finally|for|from)\\>",
                "\\<(dofile|require)\\>",
                "\\<(do|end|while|repeat|until|if|elseif|then|else|for|in|function|local|return|break)\\>",
                "\\<(do|if|lambda|let(rec)?|map|unless|when)\\>",
                "\\<(do|while|if|else|switch|case|default|for|each|in|of|with)\\>",
                "\\<(drop|reject)\\>",
                "\\<(exec|print)([[:blank:]]|$)",
                "\\<(false|nil|true)\\>",
                "\\<(fblocked|fconfigure|fcopy|file(event)?|flush|for|foreach|format|gets|glob|global|history)\\>",
                "\\<(fcntl|flock|fork|fstat|ftruncate|funlock|host_info|id|infox|keyl(del|get|keys|set)|kill)\\>",
                "\\<(float|double|BOOL|bool|char|int|short|long|id|sizeof|enum|void|static|const|struct|union|typedef|extern|(un)?signed|inline)\\>",
                "\\<(for|frame(border)?|headers|height|hidden|href(lang)?|hspace|http-equiv|id|ismap)=",
                "\\<(for|if|while|do|else|case|default|switch)\\>",
                "\\<(for|if|while|do|else|in|delete|exit)\\>",
                "\\<(function|extension|BEGIN|END)\\>",
                "\\<(fun|function|functor|match|try|with)\\>",
                "\\<(get(c|login|peername|pgrp|ppid|priority|(gr|pw)nam|(host|net|proto|serv)byname|pwuid|grgid|(host|net)byaddr|protobynumber|servbyport))\\>",
                "\\<(global|if|import|in|is|lambda|nonlocal|not|or)\\>",
                "\\<(goto|continue|break|return)\\>",
                "\\<(if|else|for|while|do|switch|case|default)\\>",
                "\\<(if|incr|info|interp|join|lappend|lindex|linsert|list|llength|load|lrange|lreplace|lsearch|lset|lsort)\\>",
                "\\<(if|test|then|elif|else|fi|for|in|do|done)\\>",
                "\\<(if|then|else)\\>",
                "\\<(if|when|unless|cond|and|or|lambda|let|progn|while|dolist|dotimes)\\>",
                "\\<(include|inherit|initializer)\\>",
                "\\<(keys|kill|lc|lcfirst|length|link|listen|local(time)?|lock|log|lstat|map|mkdir|msg(ctl|get|snd|rcv)|oct)\\>",
                "\\<(label|lang|link|longdesc|margin(height|width)|maxlength|media|method|multiple)=",
                "\\<(lassign|lcontain|lempty|lgets|link|lmatch|loadlibindex|loop|lvar(cat|pop|push)|max|min|nice)\\>",
                "\\<(let|val|method|in|and|rec|private|virtual|constraint)\\>",
                "\\<(mktime|strftime|systime)\\>",
                "\\<(my|no|our|package|sub|use)\\>",
                "\\<(namespace|open|package|pid|puts|pwd|read|regexp|regsub|rename|return)\\>",
                "\\<(name|nohref|noresize|noshade|object|onclick|onfocus|onload|onmouseover|profile|readonly|rel|rev)=",
                "\\<(new|ref|mutable|lazy|assert|raise)\\>",
                "\\<(next|nil|not|or|redo|rescue|retry|return|self|super|then|true|undef|unless|until|when|while|yield)\\>",
                "\\<(open(dir)?|ord|pack|pipe|pop|pos|printf?|prototype|push|q|qq|qr|qx|qw|quotemeta|rand|read(dir|line|link|pipe)?)\\>",
                "\\<(package|import)\\>",
                "\\<(package|private|protected|public|static|strictfp|super|synchronized|throws|volatile)\\>",
                "\\<(pass|raise|return|try|while|with|yield)\\>",
                "\\<(pg_catalog|public)\\>",
                "\\<(pipe|profile|random|readdir|replicate|scan(context|file|match)|select|server_(accept|create)|signal)\\>",
                "\\<(recv|redo|ref|rename|require|reset|reverse|rewinddir|rindex|rmdir|say|scalar|seek(dir)?|select|sem(ctl|get|op))\\>",
                "\\<(require|provide)\\>",
                "\\<(rows(pan)?|rules|scheme|scope|scrolling|selected|shape|size|span|src|standby|start|style|summary)=",
                "\\<(scan|seek|set|socket|source|split|string|subst|switch|tclLog|tell|time|trace)\\>",
                "\\<(send|set(pgrp|priority|sockopt)|shift|shm(ctl|get|read|write)|shutdown|sin|sleep|socket(pair)?|sort|splice|split)\\>",
                "\\<(setq(-default|-local)?|setf|push|pop|declare(-function)?)\\>",
                "\\<(sleep|sync|system|tclx_(findinit|fork|load_tndxs|sleep|system|wait)|times|translit|try_eval|umask|wait)\\>",
                "\\<(split|sprintf|strtonum|sub|substr|tolower|toupper)\\>",
                "\\<(sprintf|sqrt|srand|state?|study|substr|symlink|sys(call|open|read|seek|tem|write)|tell(dir)?|tied?|times?|try?)\\>",
                "\\<(tabindex|target|text|title|type|usemap|valign|value(type)?|vlink|vspace|width|xmlns|xml:space)=",
                "\\<(true|false)\\>",
                "\\<(true|false|nil|iota|_)\\>",
                "\\<(true|false|null)\\>",
                "\\<(true|false|null)\\>",
                "\\<(true|false|null|undefined)\\>",
                "\\<(truncate|uc|ucfirst|umask|un(def|link|pack|shift|tie)|utime|values|vec|wait(pid)?|wantarray|warn|write)\\>",
                "\\<(try|throw|catch|operator|new|delete)\\>",
                "\\<(try|throw|catch|operator|new|delete)\\>",
                "\\<(type|open|class|module|exception|external)\\>",
                "\\<(t|nil)\\>",
                "\\<(unknown|unset|update|uplevel|upvar|variable|vwait|while)\\>",
                "\\<0x[[:xdigit:]]+(\\.[[:xdigit:]]*)?([Pp][+-]?[0-9]+)?\\>",
                "\\<[-_.0-9]+\\>",
                "\\<[0-9]+(\\.[0-9]*)?([Ee][+-]?[0-9]+)?\\>",
                "\\<[0-9]+[eE][+-]?[0-9]+i?\\>",
                "\\<[0-9]+\\.[0-9]*([eE][+-]?[0-9]+)?i?\\>",
                "\\<[0-9]+i\\>",
                "\\<[12][0-9]{3}\\.(0[1-9]|1[012])\\.(0[1-9]|[12][0-9]|3[01])\\>",
                "\\<[A-Z]+[0-9A-Z_a-z]*|(\\$|@|@@)[0-9A-Z_a-z]+",
                "\\<[A-Z][0-9a-z_]{2,}\\>",
                "\\<[A-Z_][0-9A-Z_]*\\>",
                "\\<[A-Z_]{2,}\\>",
                "\\<[A-Za-z_][A-Za-z_0-9]*\\(",
                "\\<[Nn]ano [1-8]\\.[0-9][-.[:alnum:]]* \"[^\"]+\"",
                "\\<[[:alpha:]_][[:alnum:]_]*_t\\>",
                "\\<[[:upper:]_[:digit:]]+\\>",
                "\\<array (anymore|donesearch|exists|get|names|nextelement|set|size|startsearch|statistics|unset)\\>",
                "\\<cl-def(un|macro|subst|generic|struct|type)\\>",
                "\\<coroutine\\.(create|isyieldable|resume|running|status|wrap|yield)\\>",
                "\\<debug\\.(debug|(get|set)(fenv|hook|local|metatable|(up|user)value)|getinfo|getregistry|traceback|upvalue(id|join))\\>",
                "\\<def(class|const|var(-local|alias)?)\\>",
                "\\<def(custom|face|group|theme)\\>",
                "\\<def(un|macro|subst|generic|alias)\\>",
                "\\<define(-macro|-module|-public|-syntax)?\\>",
                "\\<define-(derived|minor|generic)-mode\\>",
                "\\<eval-(and|when)-compile\\>",
                "\\<io\\.(close|flush|input|lines|output|p?open|read|tmpfile|type|write|std(in|out|err))\\>",
                "\\<math\\.((max|min)(integer)?|modf?|pi|pow|rad|random(seed)?|sinh?|sqrt|tan|tointeger|type|ult)\\>",
                "\\<math\\.(abs|acos|asin|atan2?|ceil|cosh?|deg|exp|floor|fmod|frexp|huge|ldexp|log10|log)\\>",
                "\\<os\\.(clock|date|difftime|execute|exit|getenv|remove|rename|setlocale|time|tmpname)\\>",
                "\\<package\\.(config|cpath|loaded|loadlib|path|preload|searchers|searchpath|seeall)\\>",
                "\\<proc[[:blank:]]|\\{|\\}",
                "\\<save-((window-)?excursion|restriction)\\>",
                "\\<string (compare|equal|first|index|is|last|(byte)?length|map|match|range|repeat|replace|to(lower|title|upper)?|trim(left|right)?|will|word(end|start))\\>",
                "\\<string\\.(byte|char|dump|find|format|gmatch|gsub|len|lower|match|pack|packsize|rep|reverse|sub|unpack|upper)\\>",
                "\\<table\\.(concat|insert|maxn|move|pack|remove|sort|unpack)\\>",
                "\\<utf8\\.(char|charpattern|codepoint|codes|len|offset)\\>",
                "\\B\\.[0-9]+([eE][+-]?[0-9]+)?i?\\>",
                "\\[(\\w|::|[/., -])+\\]:( |$)",
                "\\[\\[.*\\]\\]",
                "\\[|\\]|\\(|\\)",
                "\\\\($|[\\'\"abfnrtv]|[0-3]?[0-7]?[0-7]|x[[:xdigit:]]{2})",
                "\\\\(N\\{[[:alpha:]]+\\}|u[[:xdigit:]]{4}|U[[:xdigit:]]{8})",
                "\\\\([0abefnrtv\"/ \\_NLP]|$)",
                "\\\\([\"\\/bfnrt]|u[[:xdigit:]]{4})",
                "\\\\(\\\\)?\\*(.|\\(..)",
                "\\\\(\\\\)?n(.|\\(..)",
                "\\\\(x[[:xdigit:]]{2}|u[[:xdigit:]]{4}|U[[:xdigit:]]{8})",
                "\\\\.",
                "\\\\.|\\\\[A-Za-z]*",
                "\\\\[%:]",
                "\\\\\\(..",
                "\\\\\\\\\\$[1-9]",
                "\\\\f(.|\\(..)|\\\\s(\\+|\\-)?[0-9]",
                "\\\\f[BIPR]",
                "\\{(append|prepend|backup|flip(goto|replace|execute|pipe|convert|newbuffer)|browser|gotodir|(first|last)(file|line))\\}",
                "\\{(cut|copy|paste|zap|chopword(left|right)|cutrestoffile|execute|mark|speller|linter|formatter|(full)?justify)\\}",
                "\\{(help|cancel|exit|savefile|writeout|discardbuffer|insert|where(is|was)|find(previous|next|bracket)|replace)\\}",
                "\\{(left|right|up|down|home|end|(scroll|page)(up|down)|(top|bottom)row|center|cycle|(prev|next)(word|block|anchor|buf))\\}",
                "\\{(location|gotoline|(begin|end)para|comment|complete|(un)?indent|wordcount|(record|run)macro|anchor|undo|redo)\\}",
                "\\{(nohelp|constantshow|softwrap|linenumbers|whitespacedisplay|nosyntax|zero)\\}",
                "\\{(smarthome|autoindent|cutfromcursor|breaklonglines|tabstospaces|mouse|\\{)\\}",
                "\\{(tab|enter|delete|backspace|verbatim|refresh|suspend|casesens|regexp|backwards|older|newer|(dos|mac)format)\\}",
                "\\{|\\}|\\(|\\)|\\;|\\[|\\]|\\\\|<|>|!|=|&|\\+|-|\\*|%|/|\\?:|\\^|\\|",
                "\\||@|!|:|_|~|=|\\\\|;|\\(\\)|,|\\[|\\]|\\{|\\}",
                "^ *(\\?|([?:] +)?-) ",
                "^ *: ",
                "^ .*",
                "^#!.*",
                "^#.*",
                "^(    |	)* ? ? ?(\\*|\\+|-|[1-9]+\\.)( +|	)",
                "^(    |	)+ *([^*+0-9> 	-]|[*+-]\\S|[0-9][^.]).*",
                "^(%YAML +[1-9]\\.[0-9]$|%TAG |(---|\\.\\.\\.)( |$))",
                "^((override +)?(un)?define|endef|(un)?export|private|vpath)\\>",
                "^(19|20).*",
                "^(19|20)[0-9-]{8}",
                "^(=+|-+)$",
                "^(GNU )?nano[- ][0-9]\\.[0-9]\\.[^ ]+",
                "^(Index:|diff)[[:blank:]].*",
                "^(if|ifn?def|ifn?eq|else|endif|(-|s)?include)\\>",
                "^(msgid|msgid_plural|msgstr)\\>",
                "^---.*",
                "^-.*",
                "^> ?> ?>.*",
                "^> ?>.*",
                "^>.*",
                "^@@.*",
                "^@[a-z]+([[:space:]]|$)|@([a-z]+|,|H|U|AA|AE|DH|L|OE?|(La)?TeX|TH)\\{|\\}",
                "^@[a-z]+[[:space:]]+.*",
                "^M-(\\)|\")",
                "^[ 	]*>.*",
                "^[A-Za-z0-9_-]+\\(\\)",
                "^[[:blank:]]*##.*",
                "^[[:blank:]]*#.*",
                "^[[:blank:]]*#.*\"  \"\\<dnl.*",
                "^[[:blank:]]*#.*|[[:blank:]]#.{0,2}[^[:xdigit:]].*",
                "^[[:blank:]]*#[[:blank:]]*((define|else|endif|include(_next)?|line|undef)\\>|$)",
                "^[[:blank:]]*#[[:blank:]]*(define|include|import|(un|ifn?)def|endif|el(if|se)|if|warning|error)",
                "^[[:blank:]]*#[[:blank:]]*(define|undef|include|ifn?def|endif|elif|else|if|warning|error)",
                "^[[:blank:]]*((un)?(bind|set)|include|syntax|header|magic|comment|formatter|linter|tabgives|extendsyntax)\\>",
                "^[[:blank:]]*(i?color|set[[:blank:]]+((error|function|key|mini|number|prompt|scroller|selected|spotlight|status|stripe|title)color))[[:blank:]]+(bold,)?(italic,)?(((bright|light)?(white|black|red|blue|green|yellow|magenta|cyan))|normal|pink|purple|mauve|lagoon|mint|lime|peach|orange|latte|rosy|beet|plum|sea|sky|slate|teal|sage|brown|ocher|sand|tawny|brick|crimson|grey|gray|#[[:xdigit:]]{3})?(,(((light)?(white|black|red|blue|green|yellow|magenta|cyan))|normal|pink|purple|mauve|lagoon|mint|lime|peach|orange|latte|rosy|beet|plum|sea|sky|slate|teal|sage|brown|ocher|sand|tawny|brick|crimson|grey|gray|#[[:xdigit:]]{3}))?\\>",
                "^[[:blank:]]*(set|unset)[[:blank:]]+(afterends|allow_insecure_backup|atblanks|autoindent|backup|boldtext|bookstyle|breaklonglines|casesensitive|colonparsing|constantshow|cutfromcursor|emptyline|historylog|indicator|jumpyscrolling|linenumbers|locking|magic|minibar|mouse|multibuffer|noconvert|nohelp|nonewlines|positionlog|preserve|quickblank|rawsequences|rebinddelete|regexp|saveonexit|showcursor|smarthome|softwrap|stateflags|tabstospaces|trimblanks|unix|wordbounds|zap|zero)\\>",
                "^[[:blank:]]*(syntax[[:blank:]]+[^[:space:]]+|(formatter|linter)[[:blank:]]+.+)",
                "^[[:blank:]]*[.0-9A-Za-z_]*:",
                "^[[:blank:]]*[0-9A-Za-z_]+",
                "^[[:blank:]]*[A-Z_a-z][0-9A-Z_a-z]*:[[:blank:]]*$",
                "^[[:blank:]]*\\*\\*\\*.*",
                "^[[:blank:]]*extendsyntax[[:blank:]]+[[:alpha:]]+[[:blank:]]+",
                "^[[:blank:]]*extendsyntax[[:blank:]]+[[:alpha:]]+[[:blank:]]+(i?color|header|magic|comment|formatter|linter|tabgives)[[:blank:]]+.*",
                "^[[:blank:]]*i?color\\>|[[:blank:]](start=|end=)",
                "^[[:blank:]]*include[[:blank:]][^\"]*([[:blank:]]|$)",
                "^[[:blank:]]*set[[:blank:]]+(backupdir|brackets|errorcolor|functioncolor|keycolor|matchbrackets|minicolor|numbercolor|operatingdir|promptcolor|punct|quotestr|scrollercolor|selectedcolor|speller|spotlightcolor|statuscolor|stripecolor|titlecolor|whitespace|wordchars)[[:blank:]]+",
                "^[[:blank:]]*set[[:blank:]]+(fill[[:blank:]]+-?[[:digit:]]+|(guidestripe|tabsize)[[:blank:]]+[1-9][0-9]*)\\>",
                "^[^ 	]+:",
                "^\"X-Bugs:.*\"$",
                "^\\*[[:space:]]+.*::.*",
                "^\\+.*",
                "^\\+\\+\\+.*",
                "^\\.((B[IR]?|I[BR]?|R[BI]|S[BM]) |[LP]?P$)",
                "^\\.((SH|SS|TH) |[HIT]P)",
                "^\\.(B[IR]?|I[BR]?|R[BI]|S[BM]) .*",
                "^\\.(RS|RE|UR|UE|PD|DT)",
                "^\\.(SH|SS|TH) .*",
                "^\\.(ad|bp|br|ce|de|ds|el|ie|if|fi|ft|ig|in|na|ne|nf|nh|ps|so|sp|ti|tr)",
                "^\\.(ds|nr) [^[:space:]]*",
                "^\\.(hc|hla|hlm|hw|hy)",
                "^\\.[[:blank:]]*[^[:space:]]*",
                "^\\.\\\\\".*",
                "^deb(-src)?",
                "^deb(-src)?\\s+.*(mirror\\+)?(ftp|https?|rsh|ssh|copy|file|in-toto|s3|spacewalk|tor):/\\S+",
                "^deb(-src)?\\s+.*cdrom:\\[.+\\]/",
                "^deb(-src)?\\s+\\[.+\\]\\s+",
                "^deb.*",
                "__attribute__[[:blank:]]*\\(\\([^)]*\\)\\)|__(aligned|asm|builtin|hidden|inline|packed|restrict|section|typeof|weak)__",
                "__attribute__[[:blank:]]*\\(\\([^)]*\\)\\)|__(aligned|asm|builtin|hidden|inline|packed|restrict|section|typeof|weak)__",
                "`[^`]*`",
                "`[^`]*`|%x\\{[^}]*\\}",
                "`[^`]+`",
                "def [0-9A-Za-z_]+",
                "fn [a-z_0-9]+",
                "undefined",
                "~~[^~]+~~");
    }
}
