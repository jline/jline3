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

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating Nano and Less customization in JLine.
 */
public class NanoLessCustomizationExample {

    // SNIPPET_START: NanoLessCustomizationExample
    public static void main(String[] args) {
        try {
            // Create a terminal
            Terminal terminal = TerminalBuilder.builder().system(true).build();

            // Create a line reader
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .parser(new DefaultParser())
                    .build();

            // Create configuration files
            Path jnanorcPath = createJnanorcFile();
            Path jlessrcPath = createJlessrcFile();

            // Display information about the configuration
            terminal.writer().println("Nano and Less Customization Example");
            terminal.writer().println("----------------------------------");
            terminal.writer().println("jnanorc file: " + jnanorcPath);
            terminal.writer().println("jlessrc file: " + jlessrcPath);
            terminal.writer().println();
            terminal.writer().println("To use nano with this configuration:");
            terminal.writer().println("  nano -rcfile " + jnanorcPath + " filename.txt");
            terminal.writer().println();
            terminal.writer().println("To use less with this configuration:");
            terminal.writer().println("  less -f " + jlessrcPath + " filename.txt");
            terminal.writer().println();

            // Create a sample file to edit
            Path sampleFile = createSampleFile();
            terminal.writer().println("Sample file created: " + sampleFile);
            terminal.writer().println();

            // In a real application, you would use the Nano or Less commands here
            // But for this example, we'll just show the configuration files

            terminal.writer().println("Press Enter to exit...");
            terminal.reader().read();

            terminal.writer().println("Goodbye!");
            terminal.writer().flush();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Path createJnanorcFile() throws IOException {
        String nanorcContent = "# JLine nano configuration file\n" + "set tabstospaces\n"
                + "set autoindent\n"
                + "set tempfile\n"
                + "set historylog search.log\n"
                + "\n"
                + "# Include syntax highlighting files\n"
                + "include /usr/share/nano/*.nanorc\n"
                + "\n"
                + "# Use theme system\n"
                + "theme example-theme.nanorctheme\n";

        Path nanorcFile = Paths.get("jnanorc");
        Files.write(nanorcFile, nanorcContent.getBytes());
        return nanorcFile;
    }

    private static Path createJlessrcFile() throws IOException {
        String lessrcContent = "# JLine less configuration file\n" + "set casesearch\n"
                + "set autoindent\n"
                + "set historylog search.log\n"
                + "\n"
                + "# Include syntax highlighting files\n"
                + "include /usr/share/nano/*.nanorc\n"
                + "\n"
                + "# Use theme system\n"
                + "theme example-theme.nanorctheme\n";

        Path lessrcFile = Paths.get("jlessrc");
        Files.write(lessrcFile, lessrcContent.getBytes());
        return lessrcFile;
    }

    private static Path createSampleFile() throws IOException {
        String sampleContent = "// Sample Java file for nano and less customization\n" + "\n"
                + "/**\n"
                + " * This is a sample class to demonstrate syntax highlighting\n"
                + " */\n"
                + "public class SampleClass {\n"
                + "    // Constants\n"
                + "    private static final int MAX_COUNT = 100;\n"
                + "    \n"
                + "    // Instance variables\n"
                + "    private String name;\n"
                + "    private int count;\n"
                + "    private boolean active;\n"
                + "    \n"
                + "    /**\n"
                + "     * Constructor\n"
                + "     */\n"
                + "    public SampleClass(String name) {\n"
                + "        this.name = name;\n"
                + "        this.count = 0;\n"
                + "        this.active = true;\n"
                + "    }\n"
                + "    \n"
                + "    // TODO: Add more methods\n"
                + "    \n"
                + "    /**\n"
                + "     * Main method\n"
                + "     */\n"
                + "    public static void main(String[] args) {\n"
                + "        SampleClass sample = new SampleClass(\"Test\");\n"
                + "        System.out.println(\"Created: \" + sample.name);\n"
                + "        \n"
                + "        // Loop example\n"
                + "        for (int i = 0; i < 5; i++) {\n"
                + "            System.out.println(\"Count: \" + i);\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        Path sampleFile = Paths.get("SampleClass.java");
        Files.write(sampleFile, sampleContent.getBytes());
        return sampleFile;
    }
    // SNIPPET_END: NanoLessCustomizationExample
}
