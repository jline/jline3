/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.jline.utils.Status;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LineReaderResizeTest {

    @Test
    void winchRedisplayDoesNotDuplicateMultiRowPromptBuffer() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (Terminal terminal = new DumbTerminal(
                "terminal", "ansi", new ByteArrayInputStream(new byte[0]), output, StandardCharsets.UTF_8)) {
            terminal.setSize(Size.of(20, 8));
            ExposedLineReader reader = new ExposedLineReader(terminal);
            output.reset();

            reader.setPrompt("-- user --\n");
            reader.getBuffer().write("abc def ghi jkl mno pqr");

            MiniScreen screen = new MiniScreen(20, 8);
            String previousOutput = "previous output";
            terminal.writer().print(previousOutput + "\r\n");
            terminal.flush();
            screen.write(output.toString(StandardCharsets.UTF_8));
            output.reset();

            reader.redisplay();
            terminal.flush();
            screen.write(output.toString(StandardCharsets.UTF_8));
            output.reset();

            assertEquals(1, screen.count("-- user --"), screen::dump);
            assertEquals(1, screen.count(previousOutput), screen::dump);

            terminal.setSize(Size.of(18, 9));
            screen.resize(18, 9);
            reader.handleWinch();
            terminal.flush();
            assertEquals("", output.toString(StandardCharsets.UTF_8), "WINCH should not repaint without status rows");
            output.reset();

            reader.redisplay();
            terminal.flush();
            assertEquals(
                    "", output.toString(StandardCharsets.UTF_8), "resized display model should already be current");

            assertEquals(1, screen.count("-- user --"), screen::dump);
            assertEquals(1, screen.count(previousOutput), screen::dump);
        }
    }

    @Test
    void winchWithEmptyStatusDoesNotRedrawPromptBuffer() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (Terminal terminal = new DumbTerminal(
                "terminal", "ansi", new ByteArrayInputStream(new byte[0]), output, StandardCharsets.UTF_8)) {
            terminal.setSize(Size.of(20, 8));
            Status status = Status.getStatus(terminal);
            ExposedLineReader reader = new ExposedLineReader(terminal);
            output.reset();

            assertEquals(0, status.size());

            reader.setPrompt("-- user --\n");
            reader.getBuffer().write("abc def ghi jkl mno pqr");
            reader.redisplay();
            terminal.flush();
            output.reset();

            terminal.setSize(Size.of(18, 9));
            reader.handleWinch();
            terminal.flush();

            assertEquals("", output.toString(StandardCharsets.UTF_8));
        }
    }

    private static final class ExposedLineReader extends LineReaderImpl {
        private ExposedLineReader(Terminal terminal) throws IOException {
            super(terminal);
        }

        private void handleWinch() {
            handleSignal(Terminal.Signal.WINCH);
        }
    }

    private static final class MiniScreen {
        private int columns;
        private int rows;
        private char[][] cells;
        private int row;
        private int column;

        private MiniScreen(int columns, int rows) {
            this.columns = columns;
            this.rows = rows;
            this.cells = blankCells(rows, columns);
        }

        private void resize(int newColumns, int newRows) {
            char[][] resized = blankCells(newRows, newColumns);
            int rowsToCopy = Math.min(rows, newRows);
            int columnsToCopy = Math.min(columns, newColumns);
            for (int r = 0; r < rowsToCopy; r++) {
                System.arraycopy(cells[r], 0, resized[r], 0, columnsToCopy);
            }
            cells = resized;
            rows = newRows;
            columns = newColumns;
            row = Math.min(row, rows - 1);
            column = Math.min(column, columns - 1);
        }

        private void write(String data) {
            int index = 0;
            while (index < data.length()) {
                char ch = data.charAt(index++);
                if (ch == '\u001B') {
                    index = consumeEscape(data, index);
                } else if (ch == '\r') {
                    column = 0;
                } else if (ch == '\n') {
                    newline();
                } else if (ch >= ' ') {
                    put(ch);
                }
            }
        }

        private int count(String needle) {
            int total = 0;
            for (String line : lines()) {
                int from = 0;
                int found;
                while ((found = line.indexOf(needle, from)) >= 0) {
                    total++;
                    from = found + needle.length();
                }
            }
            return total;
        }

        private String dump() {
            return String.join("\n", lines());
        }

        private String[] lines() {
            String[] lines = new String[rows];
            for (int r = 0; r < rows; r++) {
                lines[r] = new String(cells[r]);
            }
            return lines;
        }

        private int consumeEscape(String data, int index) {
            if (index >= data.length()) {
                return index;
            }
            char ch = data.charAt(index++);
            if (ch != '[') {
                return index;
            }

            int paramsStart = index;
            while (index < data.length()) {
                ch = data.charAt(index++);
                if (ch >= '@' && ch <= '~') {
                    handleCsi(data.substring(paramsStart, index - 1), ch);
                    return index;
                }
            }
            return index;
        }

        private void handleCsi(String params, char command) {
            if (params.startsWith("?")) {
                return;
            }
            int value = firstParam(params, 1);
            switch (command) {
                case 'A':
                    row = Math.max(0, row - value);
                    break;
                case 'B':
                    row = Math.min(rows - 1, row + value);
                    break;
                case 'C':
                    column = Math.min(columns - 1, column + value);
                    break;
                case 'D':
                    column = Math.max(0, column - value);
                    break;
                case 'H':
                case 'f':
                    moveTo(params);
                    break;
                case 'J':
                    if (value == 0) {
                        clearToEndOfScreen();
                    } else if (value == 2) {
                        cells = blankCells(rows, columns);
                        row = 0;
                        column = 0;
                    }
                    break;
                case 'K':
                    clearToEndOfLine();
                    break;
                default:
                    break;
            }
        }

        private int firstParam(String params, int defaultValue) {
            if (params.isEmpty()) {
                return defaultValue;
            }
            String first = params.split(";", -1)[0];
            if (first.isEmpty()) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(first);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        private void moveTo(String params) {
            String[] parts = params.split(";", -1);
            int targetRow = parseParam(parts, 0, 1) - 1;
            int targetColumn = parseParam(parts, 1, 1) - 1;
            row = Math.max(0, Math.min(rows - 1, targetRow));
            column = Math.max(0, Math.min(columns - 1, targetColumn));
        }

        private int parseParam(String[] parts, int index, int defaultValue) {
            if (index >= parts.length || parts[index].isEmpty()) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(parts[index]);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        private void put(char ch) {
            cells[row][column] = ch;
            column++;
            if (column >= columns) {
                column = 0;
                newline();
            }
        }

        private void newline() {
            if (row < rows - 1) {
                row++;
            } else {
                for (int r = 1; r < rows; r++) {
                    System.arraycopy(cells[r], 0, cells[r - 1], 0, columns);
                }
                Arrays.fill(cells[rows - 1], ' ');
            }
        }

        private void clearToEndOfScreen() {
            clearToEndOfLine();
            for (int r = row + 1; r < rows; r++) {
                Arrays.fill(cells[r], ' ');
            }
        }

        private void clearToEndOfLine() {
            Arrays.fill(cells[row], column, columns, ' ');
        }

        private static char[][] blankCells(int rows, int columns) {
            char[][] blank = new char[rows][columns];
            for (char[] row : blank) {
                Arrays.fill(row, ' ');
            }
            return blank;
        }
    }
}
