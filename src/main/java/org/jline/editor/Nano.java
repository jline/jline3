/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.editor;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jline.Console;
import org.jline.Console.Signal;
import org.jline.Console.SignalHandler;
import org.jline.console.Attributes;
import org.jline.console.Attributes.ControlChar;
import org.jline.console.Attributes.InputFlag;
import org.jline.console.Attributes.LocalFlag;
import org.jline.console.Size;
import org.jline.reader.BindingReader;
import org.jline.reader.Display;
import org.jline.reader.KeyMap;
import org.jline.utils.Ansi;
import org.jline.utils.Ansi.Attribute;
import org.jline.utils.Ansi.Color;
import org.jline.utils.Curses;
import org.jline.utils.InfoCmp.Capability;
import org.mozilla.universalchardet.UniversalDetector;

import static org.jline.reader.KeyMap.alt;
import static org.jline.reader.KeyMap.ctrl;
import static org.jline.reader.KeyMap.del;
import static org.jline.utils.Ansi.ansi;
import static org.jline.utils.AnsiHelper.length;
import static org.jline.utils.AnsiHelper.substring;

public class Nano {

    // Final fields
    protected final Console console;
    protected final Display display;
    protected final BindingReader bindingReader;
    protected final Size size;
    protected final Path root;

    // Keys
    protected KeyMap keys;

    // Configuration
    public String title = "JLine Nano 3.0.0";
    public boolean printLineNumbers = true;
    public boolean wrapping;
    public boolean smoothScrolling = true;
    public boolean oneMoreLine = true;
    public boolean constantCursor;
    public int tabs = 4;
    public String brackets = "\"’)>]}";
    public String matchBrackets = "(<[{)>]}";
    public String punct = "!.?";
    public String quoteStr = "^([ \\t]*[#:>\\|}])+";

    // Input
    protected final List<Buffer> buffers = new ArrayList<>();
    protected int bufferIndex;
    protected Buffer buffer;

    protected String message;
    protected int nbBindings = 0;

    protected LinkedHashMap<String, String> shortcuts;

    protected String editMessage;
    protected final StringBuilder editBuffer = new StringBuilder();

    protected boolean searchCaseSensitive;
    protected boolean searchRegexp;
    protected boolean searchBackwards;
    protected String searchTerm;

    protected WriteMode writeMode = WriteMode.WRITE;
    protected boolean writeBackup;

    protected boolean readNewBuffer = true;

    enum WriteMode {
        WRITE,
        APPEND,
        PREPEND
    }

    enum WriteFormat {
        UNIX,
        DOS,
        MAC
    }

    private class Buffer {
        String file;
        Charset charset;
        WriteFormat format = WriteFormat.UNIX;
        List<String> lines;

        int firstLineToDisplay;
        int firstColumnToDisplay;
        int offsetInLineToDisplay;

        int line;
        List<LinkedList<Integer>> offsets = new ArrayList<>();
        int offsetInLine;
        int column;
        int wantedColumn;

        boolean dirty;

        Buffer(String file) {
            this.file = file;
        }

        void open() throws IOException {
            if (lines != null) {
                return;
            }

            lines = new ArrayList<>();
            lines.add("");
            charset = Charset.defaultCharset();
            computeAllOffsets();

            if (file == null) {
                return;
            }

            Path path = root.resolve(file);
            if (Files.isDirectory(path)) {
                setMessage("\"" + file + "\" is a directory");
                return;
            }

            try (InputStream fis = Files.newInputStream(root.resolve(file)))
            {
                read(fis);
            } catch (IOException e) {
                setMessage("Error reading " + file + ": " + e.getMessage());
            }
        }

        void open(InputStream is) throws IOException {
            if (lines != null) {
                return;
            }

            lines = new ArrayList<>();
            lines.add("");
            charset = Charset.defaultCharset();
            computeAllOffsets();

            read(is);
        }

        void read(InputStream fis) throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int remaining;
            while ((remaining = fis.read(buffer)) > 0) {
                bos.write(buffer, 0, remaining);
            }
            byte[] bytes = bos.toByteArray();

            try {
                UniversalDetector detector = new UniversalDetector(null);
                detector.handleData(bytes, 0, bytes.length);
                detector.dataEnd();
                if (detector.getDetectedCharset() != null) {
                    charset = Charset.forName(detector.getDetectedCharset());
                }
            } catch (Throwable t) {
                // Ignore
            }

            // TODO: detect format, do not eat last newline
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new ByteArrayInputStream(bytes), charset))) {
                String line;
                lines.clear();
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            if (lines.isEmpty()) {
                lines.add("");
            }
            computeAllOffsets();
            moveToChar(0);
        }

        void insert(String insert) {
            String text = lines.get(line);
            int pos = offsetInLine + column;
            insert = insert.replaceAll("\r\n", "\n");
            insert = insert.replaceAll("\r", "\n");
            String mod;
            if (pos == text.length()) {
                mod = text + insert;
            } else {
                mod = text.substring(0, pos) + insert + text.substring(pos);
            }
            List<String> ins = new ArrayList<>();
            int last = 0;
            int idx = mod.indexOf('\n', last);
            while (idx >= 0) {
                ins.add(mod.substring(last, idx));
                last = idx + 1;
                idx = mod.indexOf('\n', last);
            }
            ins.add(mod.substring(last));
            lines.set(line, ins.get(0));
            offsets.set(line, computeOffsets(ins.get(0)));
            for (int i = 1; i < ins.size(); i++) {
                ++line;
                lines.add(line, ins.get(i));
                offsets.add(line, computeOffsets(ins.get(i)));
            }
            moveToChar(ins.get(ins.size() - 1).length() - (text.length() - pos));
            dirty = true;
        }

        void computeAllOffsets() {
            offsets.clear();
            for (String text : lines) {
                offsets.add(computeOffsets(text));
            }
        }

        LinkedList<Integer> computeOffsets(String text) {
            int width = size.getColumns() - (printLineNumbers ? 8 : 0);
            LinkedList<Integer> offsets = new LinkedList<>();
            offsets.add(0);
            int last = 0;
            int prevword = 0;
            boolean inspace = false;
            for (int i = 0; i < text.length(); i++) {
                if (isBreakable(text.charAt(i))) {
                    inspace = true;
                } else if (inspace) {
                    prevword = i;
                    inspace = false;
                }
                if (i == last + width - 1) {
                    if (prevword == last) {
                        prevword = i;
                    }
                    offsets.add(prevword);
                    last = prevword;
                }
            }
            return offsets;
        }

        boolean isBreakable(char ch) {
            return ch == ' ';
        }

        void moveToChar(int pos) {
            offsetInLine = prevLineOffset(line, pos + 1).get();
            column = pos - offsetInLine;
        }

        void delete(int count) {
            while (--count >= 0 && moveRight(1) && backspace(1));
        }

        boolean backspace(int count) {
            while (count > 0) {
                String text = lines.get(line);
                int pos = offsetInLine + column;
                if (pos == 0) {
                    if (line == 0) {
                        bof();
                        return false;
                    }
                    String prev = lines.get(--line);
                    lines.set(line, prev + text);
                    offsets.set(line, computeOffsets(prev + text));
                    moveToChar(length(prev, tabs));
                    lines.remove(line + 1);
                    offsets.remove(line + 1);
                    count--;
                    dirty = true;
                } else {
                    int nb = Math.min(pos, count);
                    text = text.substring(0, pos - nb) + text.substring(pos);
                    lines.set(line, text);
                    offsets.set(line, computeOffsets(text));
                    moveToChar(offsetInLine + column - nb);
                    count -= nb;
                    dirty = true;
                }
            }
            return true;
        }

        boolean moveLeft(int chars) {
            boolean ret = true;
            while (--chars >= 0) {
                if (offsetInLine + column > 0) {
                    moveToChar(offsetInLine + column - 1);
                } else if (line > 0) {
                    line--;
                    moveToChar(length(getLine(line), tabs));
                } else {
                    bof();
                    ret = false;
                    break;
                }
            }
            wantedColumn = column;
            ensureCursorVisible();
            return ret;
        }

        boolean moveRight(int chars) {
            boolean ret = true;
            while (--chars >= 0) {
                int len = length(getLine(line), tabs);
                if (offsetInLine + column + 1 <= len) {
                    moveToChar(offsetInLine + column + 1);
                } else if (getLine(line + 1) != null) {
                    line++;
                    offsetInLine = 0;
                    column = 0;
                } else {
                    eof();
                    ret = false;
                    break;
                }
            }
            wantedColumn = column;
            ensureCursorVisible();
            return ret;
        }

        void moveDown(int lines) throws IOException {
            cursorDown(lines);
            ensureCursorVisible();
        }

        void moveUp(int lines) {
            cursorUp(lines);
            ensureCursorVisible();
        }

        private Optional<Integer> prevLineOffset(int line, int offsetInLine) {
            if (line >= offsets.size()) {
                return Optional.empty();
            }
            Iterator<Integer> it = offsets.get(line).descendingIterator();
            while (it.hasNext()) {
                int off = it.next();
                if (off < offsetInLine) {
                    return Optional.of(off);
                }
            }
            return Optional.empty();
        }

        private Optional<Integer> nextLineOffset(int line, int offsetInLine) {
            if (line >= offsets.size()) {
                return Optional.empty();
            }
            return offsets.get(line).stream()
                    .filter(o -> o > offsetInLine)
                    .findFirst();
        }

        void moveDisplayDown(int lines) {
            int height = size.getRows() - computeHeader().size() - computeFooter().size();
            // Adjust cursor
            while (--lines >= 0) {
                int lastLineToDisplay = firstLineToDisplay;
                if (firstColumnToDisplay > 0 || !wrapping) {
                    lastLineToDisplay += height - 1;
                } else {
                    int off = offsetInLineToDisplay;
                    for (int l = 0; l < height - 1; l++) {
                        Optional<Integer> next = nextLineOffset(lastLineToDisplay, off);
                        if (next.isPresent()) {
                            off = next.get();
                        } else {
                            off = 0;
                            lastLineToDisplay++;
                        }
                    }
                }
                if (getLine(lastLineToDisplay) == null) {
                    eof();
                    return;
                }
                Optional<Integer> next = nextLineOffset(firstLineToDisplay, offsetInLineToDisplay);
                if (next.isPresent()) {
                    offsetInLineToDisplay = next.get();
                } else {
                    offsetInLineToDisplay = 0;
                    firstLineToDisplay++;
                }
            }
        }

        void moveDisplayUp(int lines) {
            int width = size.getColumns() - (printLineNumbers ? 8 : 0);
            while (--lines >= 0) {
                if (offsetInLineToDisplay > 0) {
                    offsetInLineToDisplay = Math.max(0, offsetInLineToDisplay - (width - 1));
                } else if (firstLineToDisplay > 0) {
                    firstLineToDisplay--;
                    offsetInLineToDisplay = prevLineOffset(firstLineToDisplay, Integer.MAX_VALUE).get();
                } else {
                    bof();
                    return;
                }
            }
        }

        private void cursorDown(int lines) {
            // Adjust cursor
            while (--lines >= 0) {
                if (firstColumnToDisplay > 0 || !wrapping) {
                    if (getLine(line + 1) != null) {
                        line++;
                        offsetInLine = 0;
                        column = Math.min(getLine(line).length(), wantedColumn);
                    } else {
                        bof();
                        break;
                    }
                } else {
                    String txt = getLine(line);
                    Optional<Integer> off = nextLineOffset(line, offsetInLine);
                    if (off.isPresent()) {
                        offsetInLine = off.get();
                    } else if (getLine(line + 1) == null) {
                        eof();
                        break;
                    } else {
                        line++;
                        offsetInLine = 0;
                    }
                    int next = nextLineOffset(line, offsetInLine).orElseGet(txt::length);
                    column = Math.min(wantedColumn, next - offsetInLine);
                }
            }
        }

        private void cursorUp(int lines) {
            while (--lines >= 0) {
                if (firstColumnToDisplay > 0 || !wrapping) {
                    if (line > 0) {
                        line--;
                        column = Math.min(length(getLine(line), tabs) - offsetInLine, wantedColumn);
                    } else {
                        bof();
                        break;
                    }
                } else {
                    Optional<Integer> prev = prevLineOffset(line, offsetInLine);
                    if (prev.isPresent()) {
                        offsetInLine = prev.get();
                    } else if (line > 0) {
                        line--;
                        offsetInLine = prevLineOffset(line, Integer.MAX_VALUE).get();
                        int next = nextLineOffset(line, offsetInLine).orElse(getLine(line).length());
                        column = Math.min(wantedColumn, next - offsetInLine);
                    } else {
                        bof();
                        break;
                    }
                }
            }
        }

        void ensureCursorVisible() {
            List<String> header = computeHeader();
            int rwidth = size.getColumns();
            int height = size.getRows() - header.size() - computeFooter().size();

            while (line < firstLineToDisplay
                    || line == firstLineToDisplay && offsetInLine < offsetInLineToDisplay) {
                moveDisplayUp(smoothScrolling ? 1 : height / 2);
            }

            while (true) {
                int cursor = header.size() * size.getColumns() + (printLineNumbers ? 8 : 0);
                int cur = firstLineToDisplay;
                int off = offsetInLineToDisplay;
                while (true) {
                    if (cur < line || off < offsetInLine) {
                        if (firstColumnToDisplay > 0 || !wrapping) {
                            cursor += rwidth;
                            cur++;
                        } else {
                            cursor += rwidth;
                            Optional<Integer> next = nextLineOffset(cur, off);
                            if (next.isPresent()) {
                                off = next.get();
                            } else {
                                cur++;
                                off = 0;
                            }
                        }
                    } else if (cur == line) {
                        cursor += column;
                        break;
                    } else {
                        throw new IllegalStateException();
                    }
                }
                if (cursor >= (height + header.size()) * rwidth) {
                    moveDisplayDown(smoothScrolling ? 1 : height / 2);
                } else {
                    break;
                }
            }
        }

        void eof() {
        }

        void bof() {
        }

        void resetDisplay() {
            int width = size.getColumns() - (printLineNumbers ? 8 : 0);
            column = offsetInLine + column;
            offsetInLine = (column / width) * (width - 1);
            column = column - offsetInLine;
        }

        String getLine(int line) {
            return line < lines.size() ? lines.get(line) : null;
        }

        String getTitle() {
            return file != null ? "File: " + file : "New Buffer";
        }

        List<String> computeHeader() {
            String left = Nano.this.getTitle();
            String middle = null;
            String right = dirty ? "Modified" : "        ";

            int width = size.getColumns();
            int mstart = 2 + left.length() + 1;
            int mend = width - 2 - 8;

            if (file == null) {
                middle = "New Buffer";
            } else {
                int max = mend - mstart;
                String src = file;
                if ("File: ".length() + src.length() > max) {
                    int lastSep = src.lastIndexOf('/');
                    if (lastSep > 0) {
                        String p1 = src.substring(lastSep);
                        String p0 = src.substring(0, lastSep);
                        while (p0.startsWith(".")) {
                            p0 = p0.substring(1);
                        }
                        int nb = max - p1.length() - "File: ...".length();
                        int cut;
                        cut = Math.max(0, Math.min(p0.length(), p0.length() - nb));
                        middle = "File: ..." + p0.substring(cut, p0.length()) + p1;
                    }
                    if (middle == null || middle.length() > max) {
                        left = null;
                        max = mend - 2;
                        int nb = max - "File: ...".length();
                        int cut = Math.max(0, Math.min(src.length(), src.length() - nb));
                        middle = "File: ..." + src.substring(cut, src.length());
                        if (middle.length() > max) {
                            middle = middle.substring(0, max);
                        }
                    }
                } else {
                    middle = "File: " + src;
                }
            }

            int pos = 0;
            StringBuilder sb = new StringBuilder();
            Ansi ansi = ansi(sb);
            ansi.a(Attribute.NEGATIVE_ON);
            ansi.a("  ");
            pos += 2;

            if (left != null) {
                ansi.a(left);
                pos += left.length();
                ansi.a(" ");
                pos += 1;
                for (int i = 1; i < (size.getColumns() - middle.length()) / 2 - left.length() - 1 - 2; i++) {
                    ansi.a(" ");
                    pos++;
                }
            }
            ansi.a(middle);
            pos += middle.length();
            while (pos < width - 8 - 2) {
                ansi.a(" ");
                pos++;
            }
            ansi.a(right);
            ansi.a("  ");
            ansi.a(Attribute.NEGATIVE_OFF);
            if (oneMoreLine) {
                return Collections.singletonList(ansi.toString());
            } else {
                return Arrays.asList(ansi.toString(), "");
            }
        }

        List<String> getDisplayedLines(int nbLines) {
            String cut = Ansi.ansi().fgBright(Color.BLACK).a("…").fg(Color.DEFAULT).toString();
            String ret = Ansi.ansi().fgBright(Color.BLACK).a("↩").fg(Color.DEFAULT).toString();

            List<String> newLines = new ArrayList<>();
            int rwidth = size.getColumns();
            int width = rwidth - (printLineNumbers ? 8 : 0);
            int curLine = firstLineToDisplay;
            int curOffset = offsetInLineToDisplay;
            int prevLine = -1;
            for (int terminalLine = 0; terminalLine < nbLines; terminalLine++) {
                String prefix;
                if (printLineNumbers && curLine < lines.size()) {
                    Ansi pfx = ansi().fgBright(Color.BLACK);
                    if (curLine != prevLine) {
                        pfx.format("%7d ", curLine + 1);
                    } else {
                        pfx.a("      ‧ ");
                    }
                    pfx.fg(Color.DEFAULT);
                    prefix = pfx.toString();
                    prevLine = curLine;
                } else {
                    prefix = "";
                }
                String toDisplay;
                if (curLine >= lines.size()) {
                    toDisplay = "";
                } else if (firstColumnToDisplay > 0 || !wrapping) {
                    String line = getLine(curLine);
                    String rem = substring(line, firstColumnToDisplay, Integer.MAX_VALUE, tabs);
                    if (length(rem, tabs) >= width) {
                        toDisplay = substring(rem, 0, width - 1, tabs) + cut;
                    } else {
                        toDisplay = rem;
                    }
                    curLine++;
                } else {
                    Optional<Integer> nextOffset = nextLineOffset(curLine, curOffset);
                    if (nextOffset.isPresent()) {
                        toDisplay = getLine(curLine).substring(curOffset, nextOffset.get()) + ret;
                        curOffset = nextOffset.get();
                    } else {
                        toDisplay = getLine(curLine).substring(curOffset);
                        curLine++;
                        curOffset = 0;
                    }
                    toDisplay = substring(toDisplay, 0, Integer.MAX_VALUE, tabs);
                }
                newLines.add(prefix + toDisplay);
            }
            return newLines;
        }

        public int getDisplayedCursor() {
            int rwidth = size.getColumns();
            int cursor = (printLineNumbers ? 8 : 0);
            int cur = firstLineToDisplay;
            int off = offsetInLineToDisplay;
            while (true) {
                if (cur < line || off < offsetInLine) {
                    if (firstColumnToDisplay > 0 || !wrapping) {
                        cursor += rwidth;
                        cur++;
                    } else {
                        cursor += rwidth;
                        Optional<Integer> next = nextLineOffset(cur, off);
                        if (next.isPresent()) {
                            off = next.get();
                        } else {
                            cur++;
                            off = 0;
                        }
                    }
                } else if (cur == line) {
                    cursor += column;
                    break;
                } else {
                    throw new IllegalStateException();
                }
            }
            return cursor;
        }

        char getCurrentChar() {
            String str = lines.get(line);
            if (column + offsetInLine < str.length()) {
                return str.charAt(column + offsetInLine);
            } else if (line < lines.size() - 1) {
                return '\n';
            } else {
                return 0;
            }
        }

        @SuppressWarnings("StatementWithEmptyBody")
        public void prevWord() {
            while (Character.isAlphabetic(getCurrentChar())
                    && moveLeft(1));
            while (!Character.isAlphabetic(getCurrentChar())
                    && moveLeft(1));
            while (Character.isAlphabetic(getCurrentChar())
                    && moveLeft(1));
            moveRight(1);
        }

        @SuppressWarnings("StatementWithEmptyBody")
        public void nextWord() {
            while (Character.isAlphabetic(getCurrentChar())
                    && moveRight(1));
            while (!Character.isAlphabetic(getCurrentChar())
                    && moveRight(1));
        }

        public void beginningOfLine() {
            column = offsetInLine = 0;
            wantedColumn = 0;
        }

        public void endOfLine() {
            column = length(lines.get(line), tabs);
            int width = size.getColumns() - (printLineNumbers ? 8 : 0);
            offsetInLine = (column / width) * (width - 1);
            column = column - offsetInLine;
            wantedColumn = column;
        }

        public void prevPage() {
            int height = size.getRows() - computeHeader().size() - computeFooter().size();
            scrollUp(height - 2);
        }

        public void nextPage() {
            int height = size.getRows() - computeHeader().size() - computeFooter().size();
            scrollDown(height - 2);
        }

        public void scrollUp(int lines) {
            cursorUp(lines);
            moveDisplayUp(lines);
        }

        public void scrollDown(int lines) {
            cursorDown(lines);
            moveDisplayDown(lines);
        }

        public void firstLine() {
            line = 0;
            offsetInLine = column = 0;
            ensureCursorVisible();
        }

        public void lastLine() {
            line = lines.size() - 1;
            offsetInLine = column = 0;
            ensureCursorVisible();
        }

        void nextSearch() {
            if (searchTerm == null) {
                setMessage("No current search pattern");
                return;
            }
            setMessage(null);
            int cur = line;
            int dir = searchBackwards ? -1 : +1;
            int newPos = -1;
            int newLine = -1;
            // Search on current line
            List<Integer> curRes = doSearch(lines.get(line));
            if (searchBackwards) {
                Collections.reverse(curRes);
            }
            for (int r : curRes) {
                if (searchBackwards ? r < offsetInLine + column : r > offsetInLine + column) {
                    newPos = r;
                    newLine = line;
                    break;
                }
            }
            // Check other lines
            if (newPos < 0) {
                while (true) {
                    cur = (cur + dir + lines.size()) % lines.size();
                    if (cur == line) {
                        break;
                    }
                    List<Integer> res = doSearch(lines.get(cur));
                    if (!res.isEmpty()) {
                        newPos = searchBackwards ? res.get(res.size() - 1) : res.get(0);
                        newLine = cur;
                        break;
                    }
                }
            }
            if (newPos < 0) {
                if (!curRes.isEmpty()) {
                    newPos = curRes.get(0);
                    newLine = line;
                }
            }
            if (newPos >= 0) {
                if (newLine == line && newPos == offsetInLine + column) {
                    setMessage("This is the only occurence");
                    return;
                }
                if ((searchBackwards && (newLine > line || (newLine == line && newPos > offsetInLine + column)))
                    || (!searchBackwards && (newLine < line || (newLine == line && newPos < offsetInLine + column)))) {
                    setMessage("Search Wrapped");
                }
                int width = size.getColumns() - (printLineNumbers ? 8 : 0);
                line = newLine;
                column = newPos;
                offsetInLine = (column / width) * (width - 1);
                ensureCursorVisible();
            } else {
                setMessage("\"" + searchTerm + "\" not found");
            }
        }

        private List<Integer> doSearch(String text) {
            Pattern pat = Pattern.compile(searchTerm,
                    (searchCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                            | (searchRegexp ? 0 : Pattern.LITERAL));
            Matcher m = pat.matcher(text);
            List<Integer> res = new ArrayList<>();
            while (m.find()) {
                res.add(m.start());
            }
            return res;
        }

        public void matching() {
            int opening = getCurrentChar();
            int idx = matchBrackets.indexOf(opening);
            if (idx >= 0) {
                int dir = (idx >= matchBrackets.length() / 2) ? -1 : +1;
                int closing = matchBrackets.charAt((idx + matchBrackets.length() / 2) % matchBrackets.length());

                int lvl = 1;
                int cur = line;
                int pos = offsetInLine + column;
                while (true) {
                    if ((pos + dir >= 0) && (pos + dir < getLine(cur).length())) {
                        pos += dir;
                    } else if ((cur + dir >= 0) && (cur + dir < lines.size())) {
                        cur += dir;
                        pos = dir > 0 ? 0 : lines.get(cur).length() - 1;
                        // Skip empty lines
                        if (pos < 0 || pos >= lines.get(cur).length()) {
                            continue;
                        }
                    } else {
                        setMessage("No matching bracket");
                        return;
                    }
                    int c = lines.get(cur).charAt(pos);
                    if (c == opening) {
                        lvl++;
                    } else if (c == closing) {
                        if (--lvl == 0) {
                            line = cur;
                            moveToChar(pos);
                            ensureCursorVisible();
                            return;
                        }
                    }
                }
            } else {
                setMessage("Not a bracket");
            }
        }
    }

    public Nano(Console console, File root) {
        this(console, root.toPath());
    }

    public Nano(Console console, Path root) {
        this.console = console;
        this.root = root;
        this.display = new Display(console, true);
        this.bindingReader = new BindingReader(console);
        this.size = new Size();
        bindKeys();
    }

    public void open(String... files) throws IOException {
        open(Arrays.asList(files));
    }

    public void open(List<String> files) throws IOException {
        for (String file : files) {
            buffers.add(new Buffer(file));
        }
    }

    public void run() throws IOException {
        if (buffers.isEmpty()) {
            buffers.add(new Buffer(null));
        }
        buffer = buffers.get(bufferIndex);

        Attributes attributes = console.getAttributes();
        Attributes newAttr = new Attributes(attributes);
        newAttr.setLocalFlags(EnumSet.of(LocalFlag.ICANON, LocalFlag.ECHO, LocalFlag.IEXTEN), false);
        newAttr.setInputFlags(EnumSet.of(InputFlag.IXON, InputFlag.ICRNL, InputFlag.INLCR), false);
        newAttr.setControlChar(ControlChar.VMIN, 1);
        newAttr.setControlChar(ControlChar.VTIME, 0);
        newAttr.setControlChar(ControlChar.VINTR, 0);
        console.setAttributes(newAttr);
        SignalHandler prevHandler = console.handle(Signal.WINCH, this::handle);
        console.puts(Capability.enter_ca_mode);
        size.copy(console.getSize());
        display.clear();
        display.reset();
        display.setColumns(size.getColumns());

        this.shortcuts = standardShortcuts();

        try {
            buffer.open();
            if (buffer.file != null) {
                setMessage("Read " + buffer.lines.size() + " lines");
            }

            display();

            while (true) {
                Operation op;
                switch (op = readOperation(keys)) {
                    case QUIT:
                        if (quit()) {
                            return;
                        }
                        break;
                    case WRITE:
                        write();
                        break;
                    case READ:
                        read();
                        break;
                    case UP:
                        buffer.moveUp(1);
                        break;
                    case DOWN:
                        buffer.moveDown(1);
                        break;
                    case LEFT:
                        buffer.moveLeft(1);
                        break;
                    case RIGHT:
                        buffer.moveRight(1);
                        break;
                    case INSERT:
                        buffer.insert(bindingReader.getLastBinding());
                        break;
                    case BACKSPACE:
                        buffer.backspace(1);
                        break;
                    case DELETE:
                        buffer.delete(1);
                        break;
                    case WRAP:
                        wrap();
                        break;
                    case NUMBERS:
                        numbers();
                        break;
                    case SMOOTH_SCROLLING:
                        smoothScrolling();
                        break;
                    case ONE_MORE_LINE:
                        oneMoreLine();
                        break;
                    case CLEAR_SCREEN:
                        clearScreen();
                        break;
                    case PREV_BUFFER:
                        prevBuffer();
                        break;
                    case NEXT_BUFFER:
                        nextBuffer();
                        break;
                    case CUR_POS:
                        curPos();
                        break;
                    case PREV_WORD:
                        buffer.prevWord();
                        break;
                    case NEXT_WORD:
                        buffer.nextWord();
                        break;
                    case BEGINNING_OF_LINE:
                        buffer.beginningOfLine();
                        break;
                    case END_OF_LINE:
                        buffer.endOfLine();
                        break;
                    case FIRST_LINE:
                        buffer.firstLine();
                        break;
                    case LAST_LINE:
                        buffer.lastLine();
                        break;
                    case PREV_PAGE:
                        buffer.prevPage();
                        break;
                    case NEXT_PAGE:
                        buffer.nextPage();
                        break;
                    case SCROLL_UP:
                        buffer.scrollUp(1);
                        break;
                    case SCROLL_DOWN:
                        buffer.scrollDown(1);
                        break;
                    case SEARCH:
                        search();
                        break;
                    case NEXT_SEARCH:
                        buffer.nextSearch();
                        break;
                    case HELP:
                        help("nano-main-help.txt");
                        break;
                    case CONSTANT_CURSOR:
                        constantCursor();
                        break;
                    case VERBATIM:
                        buffer.insert(new String(Character.toChars(bindingReader.readCharacter())));
                        break;
                    case MATCHING:
                        buffer.matching();
                        break;
                    default:
                        setMessage("Unsupported " + op.name().toLowerCase().replace('_', '-'));
                        break;
                }
                display();
            }
        } finally {
            console.puts(Capability.exit_ca_mode);
            console.flush();
            console.setAttributes(attributes);
            console.handle(Signal.WINCH, prevHandler);
        }
    }

    boolean write() throws IOException {
        KeyMap writeKeyMap = new KeyMap("write", Operation.INSERT);
        for (char i = 32; i < 256; i++) {
            writeKeyMap.bind(Character.toString(i), Operation.INSERT);
        }
        for (char i = 'A'; i <= 'Z'; i++) {
            writeKeyMap.bind(alt(i), Operation.DO_LOWER_CASE);
        }
        writeKeyMap.bind(del(), Operation.BACKSPACE);
        writeKeyMap.bind(alt('m'), Operation.MAC_FORMAT);
        writeKeyMap.bind(alt('d'), Operation.DOS_FORMAT);
        writeKeyMap.bind(alt('a'), Operation.APPEND_MODE);
        writeKeyMap.bind(alt('p'), Operation.PREPEND_MODE);
        writeKeyMap.bind(alt('b'), Operation.BACKUP);
        writeKeyMap.bind(ctrl('T'), Operation.TO_FILES);
        writeKeyMap.bind("\r", Operation.ACCEPT);
        writeKeyMap.bind(ctrl('C'), Operation.CANCEL);
        writeKeyMap.bind(ctrl('G'), Operation.HELP);
        bindCapability(writeKeyMap, Capability.key_f1, Operation.HELP);

        editMessage = getWriteMessage();
        editBuffer.setLength(0);
        editBuffer.append(buffer.file == null ? "" : buffer.file);
        this.shortcuts = writeShortcuts();
        display();
        while (true) {
            switch (readOperation(writeKeyMap)) {
                case INSERT:
                    editBuffer.append(bindingReader.getLastBinding());
                    break;
                case BACKSPACE:
                    if (editBuffer.length() > 0) {
                        editBuffer.setLength(editBuffer.length() - 1);
                    }
                    break;
                case CANCEL:
                    editMessage = null;
                    this.shortcuts = standardShortcuts();
                    return false;
                case ACCEPT:
                    editMessage = null;
                    if (save(editBuffer.toString())) {
                        this.shortcuts = standardShortcuts();
                        return true;
                    }
                    return false;
                case HELP:
                    help("nano-write-help.txt");
                    break;
                case MAC_FORMAT:
                    buffer.format = (buffer.format == WriteFormat.MAC) ? WriteFormat.UNIX : WriteFormat.MAC;
                    break;
                case DOS_FORMAT:
                    buffer.format = (buffer.format == WriteFormat.DOS) ? WriteFormat.UNIX : WriteFormat.DOS;
                    break;
                case APPEND_MODE:
                    writeMode = (writeMode == WriteMode.APPEND) ? WriteMode.WRITE : WriteMode.APPEND;
                    break;
                case PREPEND_MODE:
                    writeMode = (writeMode == WriteMode.PREPEND) ? WriteMode.WRITE : WriteMode.PREPEND;
                    break;
                case BACKUP:
                    writeBackup = !writeBackup;
                    break;
            }
            editMessage = getWriteMessage();
            display();
        }
    }

    private Operation readOperation(KeyMap keymap) {
        while (true) {
            Object op = bindingReader.readBinding(keymap);
            if (op == Operation.DO_LOWER_CASE) {
                bindingReader.runMacro(bindingReader.getLastBinding().toLowerCase());
            } else if (op instanceof Operation) {
                return (Operation) op;
            }
        }
    }

    private boolean save(String name) throws IOException {
        Path orgPath = buffer.file != null ? root.resolve(buffer.file) : null;
        Path newPath = root.resolve(name);
        boolean isSame = orgPath != null && Files.isSameFile(orgPath, newPath);
        if (!isSame && Files.exists(Paths.get(name))) {
            Operation op = getYNC("File exists, OVERWRITE ? ");
            if (op != Operation.YES) {
                return false;
            }
        }
        // TODO: support backup / prepend / append
        Path t = Files.createTempFile(newPath.getParent(), "jline-", ".temp");
        try (OutputStream os = Files.newOutputStream(t, StandardOpenOption.WRITE,
                                                        StandardOpenOption.TRUNCATE_EXISTING,
                                                        StandardOpenOption.CREATE)) {
            if (writeMode == WriteMode.APPEND) {
                if (Files.isReadable(newPath)) {
                    Files.copy(newPath, os);
                }
            }
            Writer w = new OutputStreamWriter(os, buffer.charset);
            for (int i = 0; i < buffer.lines.size(); i++) {
                if (i > 0) {
                    switch (buffer.format) {
                        case UNIX:
                            w.write("\n");
                            break;
                        case DOS:
                            w.write("\r\n");
                            break;
                        case MAC:
                            w.write("\r");
                            break;
                    }
                }
                w.write(buffer.lines.get(i));
            }
            w.flush();
            if (writeMode == WriteMode.PREPEND) {
                if (Files.isReadable(newPath)) {
                    Files.copy(newPath, os);
                }
            }
            if (writeBackup) {
                Files.move(newPath, newPath.resolveSibling(newPath.getFileName().toString() + "~"), StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(t, newPath, StandardCopyOption.REPLACE_EXISTING);
            buffer.file = name;
            setMessage("Wrote " + buffer.lines.size() + " lines");
            return true;
        } catch (IOException e) {
            setMessage("Error writing " + name + ": " + e.toString());
            return false;
        } finally {
            Files.deleteIfExists(t);
        }
    }

    private Operation getYNC(String message) {
        String oldEditMessage = editMessage;
        String oldEditBuffer = editBuffer.toString();
        LinkedHashMap<String, String> oldShortcuts = shortcuts;
        try {
            editMessage = message;
            editBuffer.setLength(0);
            KeyMap yncKeyMap = new KeyMap("ync");
            yncKeyMap.bind("y", Operation.YES);
            yncKeyMap.bind("Y", Operation.YES);
            yncKeyMap.bind("n", Operation.NO);
            yncKeyMap.bind("N", Operation.NO);
            yncKeyMap.bind(ctrl('C'), Operation.CANCEL);
            shortcuts = new LinkedHashMap<>();
            shortcuts.put(" Y", "Yes");
            shortcuts.put(" N", "No");
            shortcuts.put("^C", "Cancel");
            display();
            return readOperation(yncKeyMap);
        } finally {
            editMessage = oldEditMessage;
            editBuffer.append(oldEditBuffer);
            shortcuts = oldShortcuts;
        }
    }

    private String getWriteMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("File Name to ");
        switch (writeMode) {
            case WRITE:
                sb.append("Write");
                break;
            case APPEND:
                sb.append("Append");
                break;
            case PREPEND:
                sb.append("Prepend");
                break;
        }
        switch (buffer.format) {
            case UNIX:
                break;
            case DOS:
                sb.append(" [DOS Format]");
                break;
            case MAC:
                sb.append(" [Mac Format]");
                break;
        }
        if (writeBackup) {
            sb.append(" [Backup]");
        }
        sb.append(": ");
        return sb.toString();
    }

    void read() {
        KeyMap readKeyMap = new KeyMap("read", Operation.INSERT);
        for (char i = 32; i < 256; i++) {
            readKeyMap.bind(Character.toString(i), Operation.INSERT);
        }
        readKeyMap.bind(del(), Operation.BACKSPACE);
        readKeyMap.bind(alt('f'), Operation.NEW_BUFFER);
        readKeyMap.bind(ctrl('T'), Operation.TO_FILES);
        readKeyMap.bind(ctrl('X'), Operation.EXECUTE);
        readKeyMap.bind("\r", Operation.ACCEPT);
        readKeyMap.bind(ctrl('C'), Operation.CANCEL);
        readKeyMap.bind(ctrl('G'), Operation.HELP);
        bindCapability(readKeyMap, Capability.key_f1, Operation.HELP);

        editMessage = getReadMessage();
        editBuffer.setLength(0);
        this.shortcuts = readShortcuts();
        display();
        while (true) {
            switch (readOperation(readKeyMap)) {
                case INSERT:
                    editBuffer.append(bindingReader.getLastBinding());
                    break;
                case BACKSPACE:
                    if (editBuffer.length() > 0) {
                        editBuffer.setLength(editBuffer.length() - 1);
                    }
                    break;
                case CANCEL:
                    editMessage = null;
                    this.shortcuts = standardShortcuts();
                    return;
                case ACCEPT:
                    editMessage = null;
                    String file = editBuffer.toString();
                    boolean empty = file.isEmpty();
                    Path p = empty ? null : root.resolve(file);
                    if (!readNewBuffer && !empty && !Files.exists(p)) {
                        setMessage("\"" + file + "\" not found");
                    } else if (!empty && Files.isDirectory(p)) {
                        setMessage("\"" + file + "\" is a directory");
                    } else if (!empty && !Files.isRegularFile(p)) {
                        setMessage("\"" + file + "\" is not a regular file");
                    } else {
                        Buffer buf = new Buffer(empty ? null : file);
                        try {
                            buf.open();
                            if (readNewBuffer) {
                                buffers.add(++bufferIndex, buf);
                                buffer = buf;
                            } else {
                                buffer.insert(String.join("\n", buf.lines));
                            }
                            setMessage(null);
                        } catch (IOException e) {
                            setMessage("Error reading " + file + ": " + e.getMessage());
                        }
                    }
                    this.shortcuts = standardShortcuts();
                    return;
                case HELP:
                    help("nano-read-help.txt");
                    break;
                case NEW_BUFFER:
                    readNewBuffer = !readNewBuffer;
                    break;
            }
            editMessage = getReadMessage();
            display();
        }
    }

    private String getReadMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("File to insert");
        if (readNewBuffer) {
            sb.append(" into new buffer");
        }
        sb.append(" [from ./]: ");
        return sb.toString();
    }

    private LinkedHashMap<String, String> readShortcuts() {
        LinkedHashMap<String, String> shortcuts = new LinkedHashMap<>();
        shortcuts.put("^G", "Get Help");
        shortcuts.put("^T", "To Files");
        shortcuts.put("M-F", "New Buffer");
        shortcuts.put("^C", "Cancel");
        shortcuts.put("^X", "Execute Command");
        return shortcuts;
    }

    private LinkedHashMap<String, String> writeShortcuts() {
        LinkedHashMap<String, String> s = new LinkedHashMap<>();
        s.put("^G", "Get Help");
        s.put("^T", "To Files");
        s.put("M-M", "Mac Format");
        s.put("M-P", "Prepend");
        s.put("^C", "Cancel");
        s.put("M-D", "DOS Format");
        s.put("M-A", "Append");
        s.put("M-B", "Backup File");
        return s;
    }

    private LinkedHashMap<String, String> helpShortcuts() {
        LinkedHashMap<String, String> s = new LinkedHashMap<>();
        s.put("^L", "Refresh");
        s.put("^Y", "Prev Page");
        s.put("^P", "Prev Line");
        s.put("M-\\", "First Line");
        s.put("^X", "Exit");
        s.put("^V", "Next Page");
        s.put("^N", "Next Line");
        s.put("M-/", "Last Line");
        return s;
    }

    private LinkedHashMap<String, String> searchShortcuts() {
        LinkedHashMap<String, String> s = new LinkedHashMap<>();
        s.put("^G", "Get Help");
        s.put("^Y", "First Line");
        s.put("^R", "Replace");
        s.put("^W", "Beg of Par");
        s.put("M-C", "Case Sens");
        s.put("M-R", "Regexp");
        s.put("^C", "Cancel");
        s.put("^V", "Last Line");
        s.put("^T", "Go To Line");
        s.put("^O", "End of Par");
        s.put("M-B", "Backwards");
        s.put("^P", "PrevHstory");
        return s;
    }

    private LinkedHashMap<String, String> standardShortcuts() {
        LinkedHashMap<String, String> s = new LinkedHashMap<>();
        s.put("^G", "Get Help");
        s.put("^O", "WriteOut");
        s.put("^R", "Read File");
        s.put("^O", "WriteOut");
        s.put("^Y", "Prev Page");
        s.put("^K", "Cut Text");
        s.put("^C", "Cur Pos");
        s.put("^X", "Exit");
        s.put("^J", "Justify");
        s.put("^W", "Where Is");
        s.put("^V", "Next Page");
        s.put("^U", "UnCut Text");
        s.put("^T", "To Spell");
        return s;
    }

    void help(String help) {
        Buffer org = this.buffer;
        Buffer newBuf = new Buffer(null);
        try (InputStream is = getClass().getResourceAsStream(help)) {
            newBuf.open(is);
        } catch (IOException e) {
            setMessage("Unable to read help");
            return;
        }
        LinkedHashMap<String, String> oldShortcuts = this.shortcuts;
        this.shortcuts = helpShortcuts();
        boolean oldWrapping = this.wrapping;
        boolean oldPrintLineNumbers = this.printLineNumbers;
        boolean oldConstantCursor = this.constantCursor;
        this.wrapping = true;
        this.printLineNumbers = false;
        this.constantCursor = false;
        this.buffer = newBuf;
        try {
            this.message = null;
            console.puts(Capability.cursor_invisible);
            display();
            while (true) {
                switch (readOperation(keys)) {
                    case QUIT:
                        return;
                    case FIRST_LINE:
                        buffer.firstLine();
                        break;
                    case LAST_LINE:
                        buffer.lastLine();
                        break;
                    case PREV_PAGE:
                        buffer.prevPage();
                        break;
                    case NEXT_PAGE:
                        buffer.nextPage();
                        break;
                    case UP:
                        buffer.scrollUp(1);
                        break;
                    case DOWN:
                        buffer.scrollDown(1);
                        break;
                    case CLEAR_SCREEN:
                        clearScreen();
                        break;
                }
                display();
            }
        } finally {
            this.buffer = org;
            this.wrapping = oldWrapping;
            this.printLineNumbers = oldPrintLineNumbers;
            this.constantCursor = oldConstantCursor;
            this.shortcuts = oldShortcuts;
            console.puts(Capability.cursor_visible);
        }
    }

    void search() throws IOException {
        KeyMap searchKeyMap = new KeyMap("search", Operation.INSERT);
        searchKeyMap.bind(alt('c'), Operation.CASE_SENSITIVE);
        searchKeyMap.bind(alt('b'), Operation.BACKWARDS);
        searchKeyMap.bind(alt('r'), Operation.REGEXP);
        searchKeyMap.bind("\r", Operation.ACCEPT);
        searchKeyMap.bind(ctrl('C'), Operation.CANCEL);
        searchKeyMap.bind(ctrl('Y'), Operation.FIRST_LINE);
        searchKeyMap.bind(ctrl('V'), Operation.LAST_LINE);

        editMessage = getSearchMessage();
        editBuffer.setLength(0);
        this.shortcuts = searchShortcuts();
        display();
        try {
            while (true) {
                switch (readOperation(searchKeyMap)) {
                    case INSERT:
                        editBuffer.append(bindingReader.getLastBinding());
                        break;
                    case CASE_SENSITIVE:
                        searchCaseSensitive = !searchCaseSensitive;
                        break;
                    case BACKWARDS:
                        searchBackwards = !searchBackwards;
                        break;
                    case REGEXP:
                        searchRegexp = !searchRegexp;
                        break;
                    case CANCEL:
                        return;
                    case BACKSPACE:
                        if (editBuffer.length() > 0) {
                            editBuffer.setLength(editBuffer.length() - 1);
                        }
                        break;
                    case ACCEPT:
                        if (editBuffer.length() > 0) {
                            searchTerm = editBuffer.toString();
                        }
                        if (searchTerm == null || searchTerm.isEmpty()) {
                            setMessage("Cancelled");
                        } else {
                            buffer.nextSearch();
                        }
                        return;
                    case HELP:
                        help("nano-search-help.txt");
                        break;
                    case FIRST_LINE:
                        buffer.firstLine();
                        return;
                    case LAST_LINE:
                        buffer.lastLine();
                        return;
                }
                editMessage = getSearchMessage();
                display();
            }
        } finally {
            this.shortcuts = standardShortcuts();
            editMessage = null;
        }
    }

    private String getSearchMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Search");
        if (searchCaseSensitive) {
            sb.append(" [Case Sensitive]");
        }
        if (searchRegexp) {
            sb.append(" [Regexp]");
        }
        if (searchBackwards) {
            sb.append(" [Backwards]");
        }
        if (searchTerm != null) {
            sb.append(" [");
            sb.append(searchTerm);
            sb.append("]");
        }
        sb.append(": ");
        return sb.toString();
    }

    String computeCurPos() {
        int chari = 0;
        int chart = 0;
        for (int i = 0; i < buffer.lines.size(); i++) {
            int l = buffer.lines.get(i).length() + 1;
            if (i < buffer.line) {
                chari += l;
            } else if (i == buffer.line) {
                chari += buffer.offsetInLine + buffer.column;
            }
            chart += l;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("line ");
        sb.append(buffer.line + 1);
        sb.append("/");
        sb.append(buffer.lines.size());
        sb.append(" (");
        sb.append(Math.round((100.0 * buffer.line) / buffer.lines.size()));
        sb.append("%), ");
        sb.append("col ");
        sb.append(buffer.offsetInLine + buffer.column + 1);
        sb.append("/");
        sb.append(buffer.lines.get(buffer.line).length() + 1);
        sb.append(" (");
        if (buffer.lines.get(buffer.line).length() > 0) {
            sb.append(Math.round((100.0 * (buffer.offsetInLine + buffer.column))
                    / (buffer.lines.get(buffer.line).length())));
        } else {
            sb.append("100");
        }
        sb.append("%), ");
        sb.append("char ");
        sb.append(chari + 1);
        sb.append("/");
        sb.append(chart);
        sb.append(" (");
        sb.append(Math.round((100.0 * chari) / chart));
        sb.append("%)");
        return sb.toString();
    }

    void curPos() {
        setMessage(computeCurPos());
    }

    void prevBuffer() throws IOException {
        if (buffers.size() > 1) {
            bufferIndex = (bufferIndex + buffers.size() - 1) % buffers.size();
            buffer = buffers.get(bufferIndex);
            setMessage("Switched to " + buffer.getTitle());
            buffer.open();
            display.clear();
        } else {
            setMessage("No more open file buffers");
        }
    }

    void nextBuffer() throws IOException {
        if (buffers.size() > 1) {
            bufferIndex = (bufferIndex + 1) % buffers.size();
            buffer = buffers.get(bufferIndex);
            setMessage("Switched to " + buffer.getTitle());
            buffer.open();
            display.clear();
        } else {
            setMessage("No more open file buffers");
        }
    }

    void setMessage(String message) {
        this.message = message;
        this.nbBindings = 25;
    }

    boolean quit() throws IOException {
        if (buffer.dirty) {
            Operation op = getYNC("Save modified buffer (ANSWERING \"No\" WILL DESTROY CHANGES) ? ");
            switch (op) {
                case CANCEL:
                    return false;
                case NO:
                    break;
                case YES:
                    if (!write()) {
                        return false;
                    }
            }
        }
        buffers.remove(bufferIndex);
        if (bufferIndex == buffers.size() && bufferIndex > 0) {
            bufferIndex = buffers.size() - 1;
        }
        if (buffers.isEmpty()) {
            buffer = null;
            return true;
        } else {
            buffer = buffers.get(bufferIndex);
            buffer.open();
            display.clear();
            setMessage("Switched to " + buffer.getTitle());
            return false;
        }
    }

    void numbers() {
        printLineNumbers = !printLineNumbers;
        resetDisplay();
        setMessage("Lines numbering " + (printLineNumbers ? "enabled" : "disabled"));
    }

    void smoothScrolling() {
        smoothScrolling = !smoothScrolling;
        setMessage("Smooth scrolling " + (smoothScrolling ? "enabled" : "disabled"));
    }

    void constantCursor() {
        constantCursor = !constantCursor;
        setMessage("Constant cursor position display " + (constantCursor ? "enabled" : "disabled"));
    }

    void oneMoreLine() {
        oneMoreLine = !oneMoreLine;
        setMessage("Use of one more line for editing " + (oneMoreLine ? "enabled" : "disabled"));
    }

    void wrap() {
        wrapping = !wrapping;
        resetDisplay();
        setMessage("Lines wrapping " + (wrapping ? "enabled" : "disabled"));
    }

    void clearScreen() {
        resetDisplay();
    }

    public String getTitle() {
        return title;
    }


    void resetDisplay() {
        display.clear();
        display.setColumns(size.getColumns());
        for (Buffer buffer : buffers) {
            buffer.resetDisplay();
        }
    }

    synchronized void display() {
        if (nbBindings > 0) {
            if (--nbBindings == 0) {
                message = null;
            }
        }

        List<String> header = buffer.computeHeader();
        List<String> footer = computeFooter();

        int nbLines = size.getRows() - header.size() - footer.size();
        List<String> newLines = buffer.getDisplayedLines(nbLines);
        newLines.addAll(0, header);
        newLines.addAll(footer);

        // Compute cursor position
        int cursor;
        if (editMessage != null) {
            cursor = editMessage.length() + editBuffer.length();
            cursor += (size.getRows() - footer.size()) * size.getColumns();
        } else {
            cursor = buffer.getDisplayedCursor();
            cursor += header.size() * size.getColumns();
        }
        display.update(newLines, cursor);
        flush();
    }

    protected void flush() {
        try {
            console.flush();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    protected List<String> computeFooter() {
        List<String> footer = new ArrayList<>();

        if (editMessage != null) {
            Ansi ansi = ansi();
            ansi.a(Attribute.NEGATIVE_ON);
            ansi.a(editMessage);
            ansi.a(editBuffer);
            for (int i = editMessage.length() + editBuffer.length(); i < size.getColumns(); i++) {
                ansi.a(' ');
            }
            ansi.a(Attribute.NEGATIVE_OFF);
            footer.add(ansi.toString());
        } else if (message != null || constantCursor) {
            int rwidth = size.getColumns();
            String text = "[ " + (message == null ? computeCurPos() : message) + " ]";
            int len = text.length();
            Ansi ansi = ansi();
            for (int i = 0; i < (rwidth - len) / 2; i++) {
                ansi.a(' ');
            }
            ansi.a(Attribute.NEGATIVE_ON);
            ansi.a(text);
            ansi.a(Attribute.NEGATIVE_OFF);
            footer.add(ansi.toString());
        } else {
            footer.add("");
        }

        Iterator<Entry<String, String>> sit = shortcuts.entrySet().iterator();
        int cols = (shortcuts.size() + 1) / 2;
        int cw = size.getColumns() / cols;
        int rem = size.getColumns() % cols;
        for (int l = 0; l < 2; l++) {
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < cols; c++) {
                Ansi ansi = ansi();
                Map.Entry<String, String> entry = sit.hasNext() ? sit.next() : null;
                String key = entry != null ? entry.getKey() : "";
                String val = entry != null ? entry.getValue() : "";
                ansi.a(Attribute.NEGATIVE_ON);
                ansi.a(key);
                ansi.a(Attribute.NEGATIVE_OFF);
                ansi.a(" ");
                int nb = cw - key.length() - 1 + (c < rem ? 1 : 0);
                if (val.length() > nb) {
                    ansi.a(val.substring(0, nb));
                } else {
                    ansi.a(val);
                    if (c < cols - 1) {
                        for (int i = 0; i < nb - val.length(); i++) {
                            ansi.a(" ");
                        }
                    }
                }
                sb.append(ansi.toString());
            }
            footer.add(sb.toString());
        }

        return footer;
    }

    protected void handle(Signal signal) {
        size.copy(console.getSize());
        buffer.computeAllOffsets();
        buffer.moveToChar(buffer.offsetInLine + buffer.column);
        resetDisplay();
        display();
    }

    protected void bindKeys() {
        keys = new KeyMap("default", Operation.INSERT);

        for (char i = 32; i < 256; i++) {
            keys.bind(Character.toString(i), Operation.INSERT);
        }
        keys.bind(del(), Operation.BACKSPACE);
        for (char i = 'A'; i <= 'Z'; i++) {
            keys.bind(alt(i), Operation.DO_LOWER_CASE);
        }

        keys.bind(ctrl('G'), Operation.HELP);
        keys.bind(ctrl('X'), Operation.QUIT);
        keys.bind(ctrl('O'), Operation.WRITE);
        keys.bind(ctrl('J'), Operation.JUSTIFY_PARAGRAPH);
        bindCapability(keys, Capability.key_f1, Operation.HELP);
        bindCapability(keys, Capability.key_f2, Operation.QUIT);
        bindCapability(keys, Capability.key_f3, Operation.WRITE);
        bindCapability(keys, Capability.key_f4, Operation.JUSTIFY_PARAGRAPH);

        keys.bind(ctrl('R'), Operation.READ);
        keys.bind(ctrl('W'), Operation.SEARCH);
        keys.bind(ctrl('Y'), Operation.PREV_PAGE);
        keys.bind(ctrl('V'), Operation.NEXT_PAGE);
        bindCapability(keys, Capability.key_f5, Operation.READ);
        bindCapability(keys, Capability.key_f6, Operation.SEARCH);
        bindCapability(keys, Capability.key_f7, Operation.PREV_PAGE);
        bindCapability(keys, Capability.key_f8, Operation.NEXT_PAGE);

        keys.bind(ctrl('K'), Operation.CUT);
        keys.bind(ctrl('U'), Operation.UNCUT);
        keys.bind(ctrl('C'), Operation.CUR_POS);
        keys.bind(ctrl('T'), Operation.TO_SPELL);
        bindCapability(keys, Capability.key_f9, Operation.CUT);
        bindCapability(keys, Capability.key_f10, Operation.UNCUT);
        bindCapability(keys, Capability.key_f11, Operation.CUR_POS);
        bindCapability(keys, Capability.key_f12, Operation.TO_SPELL);

        keys.bind(ctrl('_'), Operation.GOTO);
        keys.bind(ctrl('\\'), Operation.REPLACE);
        keys.bind(ctrl('^'), Operation.MARK);
        bindCapability(keys, Capability.key_f13, Operation.GOTO);
        bindCapability(keys, Capability.key_f14, Operation.REPLACE);
        bindCapability(keys, Capability.key_f15, Operation.MARK);
        bindCapability(keys, Capability.key_f16, Operation.NEXT_SEARCH);
        keys.bind(alt('g'), Operation.GOTO);
        keys.bind(alt('r'), Operation.REPLACE);
        keys.bind(alt('a'), Operation.MARK);
        keys.bind(alt('w'), Operation.NEXT_SEARCH);

        keys.bind(alt('^'), Operation.COPY);
        keys.bind(alt('}'), Operation.INDENT);
        keys.bind(alt('{'), Operation.UNINDENT);

        keys.bind(ctrl('F'), Operation.RIGHT);
        keys.bind(ctrl('B'), Operation.LEFT);
        keys.bind(ctrl(' '), Operation.NEXT_WORD);
        keys.bind(alt(' '), Operation.PREV_WORD);
        keys.bind(ctrl('P'), Operation.UP);
        keys.bind(ctrl('N'), Operation.DOWN);

        keys.bind(ctrl('A'), Operation.BEGINNING_OF_LINE);
        keys.bind(ctrl('E'), Operation.END_OF_LINE);
        keys.bind(alt('('), Operation.BEGINNING_OF_PARAGRAPH);
        keys.bind(alt(')'), Operation.END_OF_PARAGRAPH);
        keys.bind(alt('\\'), Operation.FIRST_LINE);
        keys.bind(alt('/'), Operation.LAST_LINE);
        keys.bind(alt('9'), Operation.BEGINNING_OF_PARAGRAPH);
        keys.bind(alt('0'), Operation.END_OF_PARAGRAPH);
        keys.bind(alt('|'), Operation.FIRST_LINE);
        keys.bind(alt('?'), Operation.LAST_LINE);

        keys.bind(alt(']'), Operation.MATCHING);
        keys.bind(alt('-'), Operation.SCROLL_UP);
        keys.bind(alt('+'), Operation.SCROLL_DOWN);
        keys.bind(alt('_'), Operation.SCROLL_UP);
        keys.bind(alt('='), Operation.SCROLL_DOWN);

        keys.bind(alt('<'), Operation.PREV_BUFFER);
        keys.bind(alt('>'), Operation.NEXT_BUFFER);
        keys.bind(alt(','), Operation.PREV_BUFFER);
        keys.bind(alt('.'), Operation.NEXT_BUFFER);

        keys.bind(alt('v'), Operation.VERBATIM);
        keys.bind(ctrl('I'), Operation.INSERT);
        keys.bind(ctrl('M'), Operation.INSERT);
        keys.bind(ctrl('D'), Operation.DELETE);
        keys.bind(ctrl('H'), Operation.BACKSPACE);
        keys.bind(alt('t'), Operation.CUT_TO_END);

        keys.bind(alt('j'), Operation.JUSTIFY_FILE);
        keys.bind(alt('d'), Operation.COUNT);
        keys.bind(ctrl('L'), Operation.CLEAR_SCREEN);

        keys.bind(alt('x'), Operation.HELP);
        keys.bind(alt('c'), Operation.CONSTANT_CURSOR);
        keys.bind(alt('o'), Operation.ONE_MORE_LINE);
        keys.bind(alt('s'), Operation.SMOOTH_SCROLLING);
        keys.bind(alt('p'), Operation.WHITESPACE);
        keys.bind(alt('y'), Operation.HIGHLIGHT);

        keys.bind(alt('h'), Operation.SMART_HOME_KEY);
        keys.bind(alt('i'), Operation.AUTO_INDENT);
        keys.bind(alt('k'), Operation.CUT_TO_END_TOGGLE);
        keys.bind(alt('l'), Operation.WRAP);
        keys.bind(alt('q'), Operation.TABS_TO_SPACE);

        keys.bind(alt('b'), Operation.BACKUP);

        keys.bind(alt('n'), Operation.NUMBERS);

        // TODO: map other keys
        keys.bind("\033[A", Operation.UP);
        keys.bind("\033[B", Operation.DOWN);
        keys.bind("\033[C", Operation.RIGHT);
        keys.bind("\033[D", Operation.LEFT);
    }

    private void bindCapability(KeyMap keyMap, Capability capability, Operation operation) {
        try {
            String str = console.getStringCapability(capability);
            if (str != null) {
                StringWriter sw = new StringWriter();
                Curses.tputs(sw, str);
                keyMap.bind(sw.toString(), operation);
            }
        } catch (IOException e) {
            // Ignore
        }
    }

    enum Operation {
        DO_LOWER_CASE,

        QUIT,
        WRITE,
        READ,
        GOTO,
        FIND,

        WRAP,
        NUMBERS,
        SMOOTH_SCROLLING,
        ONE_MORE_LINE,
        CLEAR_SCREEN,

        UP,
        DOWN,
        LEFT,
        RIGHT,

        INSERT,
        BACKSPACE,

        NEXT_BUFFER,
        PREV_BUFFER,

        HELP,
        NEXT_PAGE,
        PREV_PAGE,
        SCROLL_UP,
        SCROLL_DOWN,
        NEXT_WORD,
        PREV_WORD,
        BEGINNING_OF_LINE,
        END_OF_LINE,
        FIRST_LINE,
        LAST_LINE,

        CUR_POS,

        CASE_SENSITIVE,
        BACKWARDS,
        REGEXP,
        ACCEPT,
        CANCEL,
        SEARCH,
        MAC_FORMAT,
        DOS_FORMAT,
        APPEND_MODE,
        PREPEND_MODE,
        BACKUP,
        TO_FILES,
        YES,
        NO,
        NEW_BUFFER,
        EXECUTE,
        NEXT_SEARCH,
        MATCHING,
        VERBATIM,
        DELETE,

        JUSTIFY_PARAGRAPH,
        TO_SPELL,
        CUT,
        REPLACE,
        MARK,
        COPY,
        INDENT,
        UNINDENT,
        BEGINNING_OF_PARAGRAPH,
        END_OF_PARAGRAPH,
        CUT_TO_END,
        JUSTIFY_FILE,
        COUNT,
        CONSTANT_CURSOR,
        WHITESPACE,
        HIGHLIGHT,
        SMART_HOME_KEY,
        AUTO_INDENT,
        CUT_TO_END_TOGGLE,
        TABS_TO_SPACE,
        UNCUT
    }

}
