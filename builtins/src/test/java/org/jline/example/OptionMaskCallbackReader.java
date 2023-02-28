/*
 * Copyright (c) 2002-2017, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.example;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class OptionMaskCallbackReader {
    /**
     * Mask a certain option value in the command line. So for example for the option '--password', '--password mypassword'
     * will be displayed as '--password **********'.
     */
    public static void usage() {
        System.out.println("Usage: java " + OptionMaskCallbackReader.class.getName() + " [option-name-to-mask]");
    }

    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.terminal();
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

        String optionToMask = args[0];

        MaskingCallback maskingCallback = new OptionValueMask(optionToMask);

        String line;
        do {
            line = reader.readLine("prompt> ", null, maskingCallback, null);
            System.out.println("Got line: " + line);
        } while (line != null && line.length() > 0);
    }

    private static class OptionValueMask implements MaskingCallback {

        private final Pattern pattern;
        private static final Character mask = '*';

        public OptionValueMask(String option) {
            String patternString = ".*?" + Pattern.quote(option) + " ??([^ ]+)";
            this.pattern = Pattern.compile(patternString);
        }

        @Override
        public String display(String line) {
            return filter(line);
        }

        @Override
        public String history(String line) {
            final String filter = filter(line);
            System.out.println();
            System.out.print("Adding to history: " + filter);
            return filter;
        }

        public String filter(String line) {
            Matcher m = pattern.matcher(line);

            if (m.find()) {
                StringBuilder maskedLine = new StringBuilder(line);
                for (int i = m.start(1); i < m.end(1); i++) {
                    maskedLine.replace(i, i + 1, String.valueOf(mask));
                }
                return maskedLine.toString();
            } else {
                return line;
            }
        }
    }
}
