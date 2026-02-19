/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.jline.reader.Completer;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.shell.Command;
import org.jline.shell.CommandSession;

/**
 * Built-in history command group.
 * <p>
 * Provides the {@code history} command with the following usage:
 * <ul>
 *   <li>{@code history} — list all history entries</li>
 *   <li>{@code history N} — list last N entries</li>
 *   <li>{@code history -c} or {@code --clear} — clear history</li>
 *   <li>{@code history -s} or {@code --save} — save history to file</li>
 *   <li>{@code history /pattern} — search history by regex</li>
 * </ul>
 *
 * @since 4.0
 */
public class HistoryCommands extends SimpleCommandGroup {

    /**
     * Creates history commands using the given line reader's history.
     *
     * @param reader the line reader
     */
    public HistoryCommands(LineReader reader) {
        super("history", createCommands(reader));
    }

    private static List<Command> createCommands(LineReader reader) {
        return List.of(new HistoryCommand(reader));
    }

    private static class HistoryCommand extends AbstractCommand {
        private final LineReader reader;

        HistoryCommand(LineReader reader) {
            super("history");
            this.reader = reader;
        }

        @Override
        public String description() {
            return "Display or manage command history";
        }

        @Override
        public List<Completer> completers() {
            return List.of(new StringsCompleter("-c", "--clear", "-s", "--save"));
        }

        @Override
        public Object execute(CommandSession session, String[] args) throws Exception {
            History history = reader.getHistory();

            if (args.length == 0) {
                // List all entries
                printHistory(session, history, Integer.MAX_VALUE, null);
                return null;
            }

            String arg = args[0];

            if ("-c".equals(arg) || "--clear".equals(arg)) {
                history.purge();
                session.out().println("History cleared.");
                return null;
            }

            if ("-s".equals(arg) || "--save".equals(arg)) {
                try {
                    history.save();
                    session.out().println("History saved.");
                } catch (IOException e) {
                    session.err().println("history: failed to save: " + e.getMessage());
                }
                return null;
            }

            if (arg.startsWith("/")) {
                // Regex search
                String patternStr = arg.substring(1);
                Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
                printHistory(session, history, Integer.MAX_VALUE, pattern);
                return null;
            }

            try {
                int n = Integer.parseInt(arg);
                printHistory(session, history, n, null);
            } catch (NumberFormatException e) {
                session.err().println("history: invalid argument: " + arg);
            }

            return null;
        }

        private void printHistory(CommandSession session, History history, int limit, Pattern filter) {
            int size = history.size();
            int start = Math.max(0, size - limit);
            for (int i = start; i < size; i++) {
                String entry = history.get(i);
                if (filter != null && !filter.matcher(entry).find()) {
                    continue;
                }
                session.out().printf("%5d  %s%n", i + 1, entry);
            }
        }
    }
}
