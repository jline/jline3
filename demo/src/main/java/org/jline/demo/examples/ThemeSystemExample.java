/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Example demonstrating the JLine theme system.
 */
public class ThemeSystemExample {

    // SNIPPET_START: ThemeSystemExample
    public static void main(String[] args) {
        try {
            // Create a terminal
            Terminal terminal = TerminalBuilder.builder().system(true).build();

            // Create a line reader
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

            // Create a theme system file
            Path themeFile = createThemeFile();

            // Create a syntax file that uses the theme
            Path syntaxFile = createSyntaxFile();

            // Create a map for theme styles
            Map<String, AttributedStyle> themeMap = new HashMap<>();

            // Add some example styles
            themeMap.put("BOOLEAN", AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE));
            themeMap.put("NUMBER", AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE));
            themeMap.put("CONSTANT", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
            themeMap.put("COMMENT", AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT | AttributedStyle.BLACK));
            themeMap.put("STRING", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));

            // Display information about the theme system
            terminal.writer().println("Theme System Example");
            terminal.writer().println("-------------------");
            terminal.writer().println("Theme file: " + themeFile);
            terminal.writer().println("Syntax file: " + syntaxFile);
            terminal.writer().println();

            // Display the theme styles
            terminal.writer().println("Theme Styles:");
            for (Map.Entry<String, AttributedStyle> entry : themeMap.entrySet()) {
                AttributedStringBuilder asb = new AttributedStringBuilder();
                asb.append("  ");
                asb.append(entry.getKey(), entry.getValue());
                asb.append(": ");
                asb.styled(entry.getValue(), "This is styled text");
                terminal.writer().println(asb.toAttributedString());
            }
            terminal.writer().println();

            // Display a sample of styled text
            terminal.writer().println("Sample Styled Text:");
            String sampleCode = "// This is a comment\n" + "public class Example {\n"
                    + "    public static void main(String[] args) {\n"
                    + "        boolean flag = true;\n"
                    + "        int number = 42;\n"
                    + "        System.out.println(\"Hello, World!\");\n"
                    + "    }\n"
                    + "}\n";

            for (String line : sampleCode.split("\n")) {
                AttributedStringBuilder asb = new AttributedStringBuilder();

                // Apply simple styling based on content
                if (line.trim().startsWith("//")) {
                    asb.styled(themeMap.get("COMMENT"), line);
                } else if (line.contains("true")) {
                    String before = line.substring(0, line.indexOf("true"));
                    String keyword = "true";
                    String after = line.substring(line.indexOf("true") + 4);
                    asb.append(before);
                    asb.styled(themeMap.get("BOOLEAN"), keyword);
                    asb.append(after);
                } else if (line.contains("42")) {
                    String before = line.substring(0, line.indexOf("42"));
                    String number = "42";
                    String after = line.substring(line.indexOf("42") + 2);
                    asb.append(before);
                    asb.styled(themeMap.get("NUMBER"), number);
                    asb.append(after);
                } else if (line.contains("\"Hello, World!\"")) {
                    String before = line.substring(0, line.indexOf("\"Hello, World!\""));
                    String string = "\"Hello, World!\"";
                    String after = line.substring(line.indexOf("\"Hello, World!\"") + 15);
                    asb.append(before);
                    asb.styled(themeMap.get("STRING"), string);
                    asb.append(after);
                } else {
                    asb.append(line);
                }

                terminal.writer().println(asb.toAttributedString());
            }

            terminal.writer().flush();
            terminal.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Path createThemeFile() throws IOException {
        String themeContent = "# Theme system configuration file\n" + "BOOLEAN     brightwhite\n"
                + "NUMBER      blue\n"
                + "CONSTANT    yellow\n"
                + "COMMENT     brightblack\n"
                + "DOC_COMMENT white\n"
                + "TODO        brightwhite,yellow\n"
                + "WHITESPACE  ,green\n"
                + "STRING      green\n"
                + "KEYWORD     brightblue\n"
                + "TYPE        cyan\n"
                + "IDENTIFIER  white\n"
                + "OPERATOR    red\n"
                + "#\n"
                + "# mixin\n"
                + "#\n"
                + "+LINT   WHITESPACE: \"[[:space:]]+$\" \\n WHITESPACE: \"\\t*\"\n"
                + "#\n"
                + "# parser\n"
                + "#\n"
                + "$LINE_COMMENT   COMMENT \\n TODO: \"(FIXME|TODO|XXX)\"\n"
                + "$BLOCK_COMMENT  COMMENT \\n DOC_COMMENT: startWith=/** \\n TODO: \"(FIXME|TODO|XXX)\"\n";

        Path themeFile = Paths.get("example-theme.nanorctheme");
        Files.write(themeFile, themeContent.getBytes());
        return themeFile;
    }

    private static Path createSyntaxFile() throws IOException {
        String syntaxContent = "# Syntax file for Java\n" + "syntax \"java\" \"\\.java$\"\n"
                + "\n"
                + "# Keywords\n"
                + "KEYWORD: \"\\b(abstract|assert|break|case|catch|class|const|continue|default|do|else|enum|extends|final|finally|for|goto|if|implements|import|instanceof|interface|native|new|package|private|protected|public|return|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|volatile|while)\\b\"\n"
                + "\n"
                + "# Types\n"
                + "TYPE: \"\\b(boolean|byte|char|double|float|int|long|short|void)\\b\"\n"
                + "\n"
                + "# Boolean and null literals\n"
                + "BOOLEAN: \"\\b(true|false|null)\\b\"\n"
                + "\n"
                + "# Numbers\n"
                + "~NUMBER: \"\\b([0-9]+)\\b\" \"\\b0x[0-9a-fA-F]+\\b\"\n"
                + "\n"
                + "# String literals\n"
                + "STRING: \"\\\".*?\\\"\" \"'.*?'\"\n"
                + "\n"
                + "# Constants\n"
                + "CONSTANT: \"\\b[A-Z]+([_]{1}[A-Z]+){0,}\\b\"\n"
                + "\n"
                + "# Comments\n"
                + "$LINE_COMMENT: \"//\"\n"
                + "$BLOCK_COMMENT: \"/*, */\"\n"
                + "\n"
                + "# Include the LINT mixin\n"
                + "+LINT\n";

        Path syntaxFile = Paths.get("java.nanorc");
        Files.write(syntaxFile, syntaxContent.getBytes());
        return syntaxFile;
    }
    // SNIPPET_END: ThemeSystemExample
}
