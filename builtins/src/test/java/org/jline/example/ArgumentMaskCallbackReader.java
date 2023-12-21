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

public class ArgumentMaskCallbackReader {
    /**
     * Mask a certain argument for a given command in the console. So for example 'add-user 2', 'add-user username password'
     * will be displayed as 'add-user username ********'.
     */
    public static void usage() {
        System.out.println(
                "Usage: java " + ArgumentMaskCallbackReader.class.getName() + " [command] [argument-pos-to-mask]");
    }

    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.terminal();
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

        String command = args[0];
        int pos = Integer.parseInt(args[1]);

        MaskingCallback maskingCallback = new CommandArgumentMask(command, pos);

        String line;
        do {
            line = reader.readLine("prompt> ", null, maskingCallback, null);
            System.out.println("Got line: " + line);
        } while (line != null && line.length() > 0);
    }

    private static class CommandArgumentMask implements MaskingCallback {

        private final Pattern pattern;
        private static final Character mask = '*';
        private final int pos;

        public CommandArgumentMask(String command, int pos) {
            StringBuilder regex = new StringBuilder();
            regex.append(Pattern.quote(command));
            for (int i = 0; i < pos; i++) {
                regex.append(" +([^ ]+)");
            }
            this.pattern = Pattern.compile(regex.toString());
            this.pos = pos;
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
                for (int i = m.start(pos); i < m.end(pos); i++) {
                    maskedLine.replace(i, i + 1, String.valueOf(mask));
                }
                return maskedLine.toString();
            } else {
                return line;
            }
        }
    }
}
