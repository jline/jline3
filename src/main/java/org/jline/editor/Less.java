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
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.jline.Console;
import org.jline.Console.Signal;
import org.jline.Console.SignalHandler;
import org.jline.console.Attributes;
import org.jline.console.Size;
import org.jline.keymap.Binding;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.reader.Display;
import org.jline.utils.Ansi;
import org.jline.utils.Ansi.Attribute;
import org.jline.utils.AnsiHelper;
import org.jline.utils.InfoCmp.Capability;

import static org.jline.keymap.KeyMap.alt;
import static org.jline.keymap.KeyMap.ctrl;
import static org.jline.keymap.KeyMap.key;

public class Less {

    public interface Source {

        String getName();

        InputStream read() throws IOException;

    }

    public static class PathSource implements Source {
        final Path path;
        final String name;

        public PathSource(File file, String name) {
            this.path = file.toPath();
            this.name = name;
        }

        public PathSource(Path path, String name) {
            this.path = path;
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public InputStream read() throws IOException {
            return Files.newInputStream(path);
        }

    }

    public static class StdInSource implements Source {
        @Override
        public String getName() {
            return null;
        }

        @Override
        public InputStream read() throws IOException {
            return new FilterInputStream(System.in) {
                @Override
                public void close() throws IOException {
                }
            };
        }

    }

    private static final int ESCAPE = 27;

    public boolean quitAtSecondEof;
    public boolean quitAtFirstEof;
    public boolean printLineNumbers;
    public boolean quiet;
    public boolean veryQuiet;
    public boolean chopLongLines;
    public boolean ignoreCaseCond;
    public boolean ignoreCaseAlways;
    public int tabs = 4;

    protected final Console console;
    protected final Display display;
    protected final BindingReader bindingReader;

    protected List<Source> sources;
    protected int sourceIdx;
    protected BufferedReader reader;
    protected KeyMap keys;

    protected int firstLineInMemory = 0;
    protected List<String> lines = new ArrayList<>();

    protected int firstLineToDisplay = 0;
    protected int firstColumnToDisplay = 0;
    protected int offsetInLine = 0;

    protected String message;
    protected final StringBuilder buffer = new StringBuilder();
    protected Thread displayThread;
    protected final AtomicBoolean redraw = new AtomicBoolean();

    protected final Map<String, Operation> options = new TreeMap<>();

    protected int window;
    protected int halfWindow;

    protected int nbEof;

    protected String pattern;

    protected final Size size = new Size();


    public Less(Console console) {
        this.console = console;
        this.display = new Display(console, true);
        this.bindingReader = new BindingReader(console, null);
    }

    public void handle(Signal signal) {
        size.copy(console.getSize());
        redraw();
    }

    public void run(Source... sources) throws IOException, InterruptedException {
        run(Arrays.asList(sources));
    }

    public void run(List<Source> sources) throws IOException, InterruptedException {
        if (sources == null || sources.isEmpty()) {
            throw new IllegalArgumentException("No sources");
        }
        this.sources = sources;

        sourceIdx = 0;
        openSource();

        try {
            size.copy(console.getSize());
            SignalHandler prevHandler = console.handle(Signal.WINCH, this::handle);
            Attributes attr = console.enterRawMode();
            try {
                window = size.getRows() - 1;
                halfWindow = window / 2;
                keys = new KeyMap();
                bindKeys(keys);

                // Use alternate buffer
                console.puts(Capability.enter_ca_mode);
                console.puts(Capability.keypad_xmit);
                console.writer().flush();

                displayThread = new Thread() {
                    @Override
                    public void run() {
                        redrawLoop();
                    }
                };
                displayThread.start();
                redraw();
                checkInterrupted();

                options.put("-e", Operation.OPT_QUIT_AT_SECOND_EOF);
                options.put("--quit-at-eof", Operation.OPT_QUIT_AT_SECOND_EOF);
                options.put("-E", Operation.OPT_QUIT_AT_FIRST_EOF);
                options.put("-QUIT-AT-EOF", Operation.OPT_QUIT_AT_FIRST_EOF);
                options.put("-N", Operation.OPT_PRINT_LINES);
                options.put("--LINE-NUMBERS", Operation.OPT_PRINT_LINES);
                options.put("-q", Operation.OPT_QUIET);
                options.put("--quiet", Operation.OPT_QUIET);
                options.put("--silent", Operation.OPT_QUIET);
                options.put("-Q", Operation.OPT_VERY_QUIET);
                options.put("--QUIET", Operation.OPT_VERY_QUIET);
                options.put("--SILENT", Operation.OPT_VERY_QUIET);
                options.put("-S", Operation.OPT_CHOP_LONG_LINES);
                options.put("--chop-long-lines", Operation.OPT_CHOP_LONG_LINES);
                options.put("-i", Operation.OPT_IGNORE_CASE_COND);
                options.put("--ignore-case", Operation.OPT_IGNORE_CASE_COND);
                options.put("-I", Operation.OPT_IGNORE_CASE_ALWAYS);
                options.put("--IGNORE-CASE", Operation.OPT_IGNORE_CASE_ALWAYS);

                Operation op;
                do {
                    checkInterrupted();

                    op = null;
                    //
                    // Option edition
                    //
                    if (buffer.length() > 0 && buffer.charAt(0) == '-') {
                        int c = console.reader().read();
                        synchronized (redraw) {
                            message = null;
                            if (buffer.length() == 1) {
                                buffer.append((char) c);
                                if (c != '-') {
                                    op = options.get(buffer.toString());
                                    if (op == null) {
                                        message = "There is no " + printable(buffer.toString()) + " option";
                                        buffer.setLength(0);
                                    }
                                }
                            } else if (c == '\r') {
                                op = options.get(buffer.toString());
                                if (op == null) {
                                    message = "There is no " + printable(buffer.toString()) + " option";
                                    buffer.setLength(0);
                                }
                            } else {
                                buffer.append((char) c);
                                Map<String, Operation> matching = new HashMap<>();
                                for (Map.Entry<String, Operation> entry : options.entrySet()) {
                                    if (entry.getKey().startsWith(buffer.toString())) {
                                        matching.put(entry.getKey(), entry.getValue());
                                    }
                                }
                                switch (matching.size()) {
                                    case 0:
                                        buffer.setLength(0);
                                        break;
                                    case 1:
                                        buffer.setLength(0);
                                        buffer.append(matching.keySet().iterator().next());
                                        break;
                                }
                            }
                        }
                    }
                    //
                    // Pattern edition
                    //
                    else if (buffer.length() > 0 && (buffer.charAt(0) == '/' || buffer.charAt(0) == '?')) {
                        int c = console.reader().read();
                        synchronized (redraw) {
                            message = null;
                            if (c == '\r') {
                                pattern = buffer.toString().substring(1);
                                if (buffer.charAt(0) == '/') {
                                    moveToNextMatch();
                                } else {
                                    moveToPreviousMatch();
                                }
                                buffer.setLength(0);
                            } else {
                                buffer.append((char) c);
                            }
                        }
                    }
                    //
                    // Command reading
                    //
                    else {
                        Object obj = bindingReader.readBinding(keys, null, false);
                        if (obj == Operation.CHAR) {
                            synchronized (redraw) {
                                char c = bindingReader.getLastBinding().charAt(0);
                                // Enter option mode or pattern edit mode
                                if (c == '-' || c == '/' || c == '?') {
                                    buffer.setLength(0);
                                }
                                buffer.append(c);
                            }
                        } else if (obj instanceof Operation) {
                            op = (Operation) obj;
                        }
                    }
                    if (op != null) {
                        synchronized (redraw) {
                            message = null;
                            switch (op) {
                                case FORWARD_ONE_LINE:
                                    moveForward(getStrictPositiveNumberInBuffer(1));
                                    break;
                                case BACKWARD_ONE_LINE:
                                    moveBackward(getStrictPositiveNumberInBuffer(1));
                                    break;
                                case FORWARD_ONE_WINDOW_OR_LINES:
                                    moveForward(getStrictPositiveNumberInBuffer(window));
                                    break;
                                case FORWARD_ONE_WINDOW_AND_SET:
                                    window = getStrictPositiveNumberInBuffer(window);
                                    moveForward(window);
                                    break;
                                case FORWARD_ONE_WINDOW_NO_STOP:
                                    moveForward(window);
                                    // TODO: handle no stop
                                    break;
                                case FORWARD_HALF_WINDOW_AND_SET:
                                    halfWindow = getStrictPositiveNumberInBuffer(halfWindow);
                                    moveForward(halfWindow);
                                    break;
                                case BACKWARD_ONE_WINDOW_AND_SET:
                                    window = getStrictPositiveNumberInBuffer(window);
                                    moveBackward(window);
                                    break;
                                case BACKWARD_ONE_WINDOW_OR_LINES:
                                    moveBackward(getStrictPositiveNumberInBuffer(window));
                                    break;
                                case BACKWARD_HALF_WINDOW_AND_SET:
                                    halfWindow = getStrictPositiveNumberInBuffer(halfWindow);
                                    moveBackward(halfWindow);
                                    break;
                                case GO_TO_FIRST_LINE_OR_N:
                                    // TODO: handle number
                                    firstLineToDisplay = firstLineInMemory;
                                    offsetInLine = 0;
                                    break;
                                case GO_TO_LAST_LINE_OR_N:
                                    // TODO: handle number
                                    moveForward(Integer.MAX_VALUE);
                                    break;
                                case LEFT_ONE_HALF_SCREEN:
                                    firstColumnToDisplay = Math.max(0, firstColumnToDisplay - size.getColumns() / 2);
                                    break;
                                case RIGHT_ONE_HALF_SCREEN:
                                    firstColumnToDisplay += size.getColumns() / 2;
                                    break;
                                case REPEAT_SEARCH_BACKWARD:
                                case REPEAT_SEARCH_BACKWARD_SPAN_FILES:
                                    moveToPreviousMatch();
                                    break;
                                case REPEAT_SEARCH_FORWARD:
                                case REPEAT_SEARCH_FORWARD_SPAN_FILES:
                                    moveToNextMatch();
                                    break;
                                case UNDO_SEARCH:
                                    pattern = null;
                                    break;
                                case OPT_PRINT_LINES:
                                    buffer.setLength(0);
                                    printLineNumbers = !printLineNumbers;
                                    message = printLineNumbers ? "Constantly display line numbers" : "Don't use line numbers";
                                    break;
                                case OPT_QUIET:
                                    buffer.setLength(0);
                                    quiet = !quiet;
                                    veryQuiet = false;
                                    message = quiet ? "Ring the bell for errors but not at eof/bof" : "Ring the bell for errors AND at eof/bof";
                                    break;
                                case OPT_VERY_QUIET:
                                    buffer.setLength(0);
                                    veryQuiet = !veryQuiet;
                                    quiet = false;
                                    message = veryQuiet ? "Never ring the bell" : "Ring the bell for errors AND at eof/bof";
                                    break;
                                case OPT_CHOP_LONG_LINES:
                                    buffer.setLength(0);
                                    offsetInLine = 0;
                                    chopLongLines = !chopLongLines;
                                    message = chopLongLines ? "Chop long lines" : "Fold long lines";
                                    break;
                                case OPT_IGNORE_CASE_COND:
                                    ignoreCaseCond = !ignoreCaseCond;
                                    ignoreCaseAlways = false;
                                    message = ignoreCaseCond ? "Ignore case in searches" : "Case is significant in searches";
                                    break;
                                case OPT_IGNORE_CASE_ALWAYS:
                                    ignoreCaseAlways = !ignoreCaseAlways;
                                    ignoreCaseCond = false;
                                    message = ignoreCaseAlways ? "Ignore case in searches and in patterns" : "Case is significant in searches";
                                    break;
                                case NEXT_FILE:
                                    if (sourceIdx < sources.size() - 1) {
                                        sourceIdx++;
                                        openSource();
                                    } else {
                                        message = "No next file";
                                    }
                                    break;
                                case PREV_FILE:
                                    if (sourceIdx > 0) {
                                        sourceIdx--;
                                        openSource();
                                    } else {
                                        message = "No previous file";
                                    }
                                    break;
                            }
                            buffer.setLength(0);
                        }
                    }
                    if (quitAtFirstEof && nbEof > 0 || quitAtSecondEof && nbEof > 1) {
                        if (sourceIdx < sources.size() - 1) {
                            sourceIdx++;
                            openSource();
                        } else {
                            op = Operation.EXIT;
                        }
                    }
                    redraw();
                } while (op != Operation.EXIT);
            } catch (InterruptedException ie) {
                // Do nothing
            } finally {
                console.setAttributes(attr);
                if (prevHandler != null) {
                    console.handle(Console.Signal.WINCH, prevHandler);
                }
                displayThread.interrupt();
                displayThread.join();
                // Use main buffer
                console.puts(Capability.exit_ca_mode);
                console.puts(Capability.keypad_local);
                console.writer().flush();
                // Clear line
                console.writer().println();
                console.writer().flush();
            }
        } finally {
            reader.close();
        }
    }

    protected void openSource() throws IOException {
        if (reader != null) {
            reader.close();
        }
        Source source = sources.get(sourceIdx);
        InputStream in = source.read();
        if (sources.size() == 1) {
            message = source.getName();
        } else {
            message = source.getName() + " (file " + (sourceIdx + 1) + " of " + sources.size() + ")";
        }
        reader = new BufferedReader(new InputStreamReader(new InterruptibleInputStream(in)));
        firstLineInMemory = 0;
        lines = new ArrayList<>();
        firstLineToDisplay = 0;
        firstColumnToDisplay = 0;
        offsetInLine = 0;
    }

    private void moveToNextMatch() throws IOException {
        Pattern compiled = getPattern();
        if (compiled != null) {
            for (int lineNumber = firstLineToDisplay + 1; ; lineNumber++) {
                String line = getLine(lineNumber);
                if (line == null) {
                    break;
                } else if (compiled.matcher(line).find()) {
                    firstLineToDisplay = lineNumber;
                    offsetInLine = 0;
                    return;
                }
            }
        }
        message = "Pattern not found";
    }

    private void moveToPreviousMatch() throws IOException {
        Pattern compiled = getPattern();
        if (compiled != null) {
            for (int lineNumber = firstLineToDisplay - 1; lineNumber >= firstLineInMemory; lineNumber--) {
                String line = getLine(lineNumber);
                if (line == null) {
                    break;
                } else if (compiled.matcher(line).find()) {
                    firstLineToDisplay = lineNumber;
                    offsetInLine = 0;
                    return;
                }
            }
        }
        message = "Pattern not found";
    }

    private String printable(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ESCAPE) {
                sb.append("ESC");
            } else if (c < 32) {
                sb.append('^').append((char) (c + '@'));
            } else if (c < 128) {
                sb.append(c);
            } else {
                sb.append('\\').append(String.format("%03o", (int) c));
            }
        }
        return sb.toString();
    }

    void moveForward(int lines) throws IOException {
        int width = size.getColumns() - (printLineNumbers ? 8 : 0);
        int height = size.getRows();
        while (--lines >= 0) {

            int lastLineToDisplay = firstLineToDisplay;
            if (firstColumnToDisplay > 0 || chopLongLines) {
                lastLineToDisplay += height - 1;
            } else {
                int off = offsetInLine;
                for (int l = 0; l < height - 1; l++) {
                    String line = getLine(lastLineToDisplay);
                    if (line == null) {
                        break;
                    }
                    if (ansiLength(line) > off + width) {
                        off += width;
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

            String line = getLine(firstLineToDisplay);
            if (ansiLength(line) > width + offsetInLine) {
                offsetInLine += width;
            } else {
                offsetInLine = 0;
                firstLineToDisplay++;
            }
        }
    }

    void moveBackward(int lines) throws IOException {
        int width = size.getColumns() - (printLineNumbers ? 8 : 0);
        while (--lines >= 0) {
            if (offsetInLine > 0) {
                offsetInLine = Math.max(0, offsetInLine - width);
            } else if (firstLineInMemory < firstLineToDisplay) {
                firstLineToDisplay--;
                String line = getLine(firstLineToDisplay);
                int length = ansiLength(line);
                offsetInLine = length - length % width;
            } else {
                bof();
                return;
            }
        }
    }

    private void eof() {
        nbEof++;
        if (sourceIdx < sources.size() - 1) {
            message = "(END) - Next: " + sources.get(sourceIdx + 1).getName();
        } else {
            message = "(END)";
        }
        if (!quiet && !veryQuiet && !quitAtFirstEof && !quitAtSecondEof) {
            console.puts(Capability.bell);
            console.writer().flush();
        }
    }

    private void bof() {
        if (!quiet && !veryQuiet) {
            console.puts(Capability.bell);
            console.writer().flush();
        }
    }

    int getStrictPositiveNumberInBuffer(int def) {
        try {
            int n = Integer.parseInt(buffer.toString());
            return (n > 0) ? n : def;
        } catch (NumberFormatException e) {
            return def;
        } finally {
            buffer.setLength(0);
        }
    }

    void redraw() {
        synchronized (redraw) {
            redraw.set(true);
            redraw.notifyAll();
        }
    }

    void redrawLoop() {
        synchronized (redraw) {
            for (; ; ) {
                try {
                    if (redraw.compareAndSet(true, false)) {
                        display();
                    } else {
                        redraw.wait();
                    }
                } catch (Exception e) {
                    return;
                }
            }
        }
    }

    void display() throws IOException {
        List<String> newLines = new ArrayList<>();
        int width = size.getColumns() - (printLineNumbers ? 8 : 0);
        int height = size.getRows();
        int inputLine = firstLineToDisplay;
        String curLine = null;
        Pattern compiled = getPattern();
        for (int terminalLine = 0; terminalLine < height - 1; terminalLine++) {
            if (curLine == null) {
                curLine = getLine(inputLine++);
                if (curLine == null) {
                    curLine = "";
                }
                if (compiled != null) {
                    String repl = Ansi.ansi().a(Attribute.NEGATIVE_ON).a("$1").a(Attribute.NEGATIVE_OFF).toString();
                    curLine = compiled.matcher(curLine).replaceAll(repl);
                }
            }
            String toDisplay;
            if (firstColumnToDisplay > 0 || chopLongLines) {
                int off = firstColumnToDisplay;
                if (terminalLine == 0 && offsetInLine > 0) {
                    off = Math.max(offsetInLine, off);
                }
                toDisplay = ansiSubstring(curLine, off, off + width);
                curLine = null;
            } else {
                if (terminalLine == 0 && offsetInLine > 0) {
                    curLine = ansiSubstring(curLine, offsetInLine, Integer.MAX_VALUE);
                }
                toDisplay = ansiSubstring(curLine, 0, width);
                curLine = ansiSubstring(curLine, width, Integer.MAX_VALUE);
                if (curLine.length() == 0) {
                    curLine = null;
                }
            }
            if (printLineNumbers) {
                newLines.add(String.format("%7d ", inputLine) + toDisplay);
            } else {
                newLines.add(toDisplay);
            }
        }
        String msg;
        if (buffer.length() > 0) {
            msg = " " + buffer;
        } else if (bindingReader.getCurrentBuffer().length() > 0) {
            msg = " " + printable(bindingReader.getCurrentBuffer());
        } else if (message != null) {
            msg = Ansi.ansi().a(Attribute.NEGATIVE_ON).a(message).a(Attribute.NEGATIVE_OFF).toString();
        } else {
            msg = ":";
        }
        newLines.add(msg);

        display.resize(size.getRows(), size.getColumns());
        display.update(newLines, -1);
        console.flush();
    }

    private Pattern getPattern() {
        Pattern compiled = null;
        if (pattern != null) {
            boolean insensitive = ignoreCaseAlways || ignoreCaseCond && pattern.toLowerCase().equals(pattern);
            compiled = Pattern.compile("(" + pattern + ")", insensitive ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0);
        }
        return compiled;
    }

    private int ansiLength(String curLine) throws IOException {
        return AnsiHelper.length(curLine, tabs);
    }

    private String ansiSubstring(String curLine, int begin, int end) throws IOException {
        return AnsiHelper.substring(curLine, begin, end, tabs);
    }

    String getLine(int line) throws IOException {
        while (line >= lines.size()) {
            String str = reader.readLine();
            if (str != null) {
                lines.add(str);
            } else {
                break;
            }
        }
        if (line < lines.size()) {
            return lines.get(line);
        }
        return null;
    }

    /**
     * This is for long running commands to be interrupted by ctrl-c
     *
     * @throws InterruptedException
     */
    public static void checkInterrupted() throws InterruptedException {
        Thread.yield();
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }

    private void bindKeys(KeyMap map) {
        map.bind(Operation.HELP, "h", "H");
        map.bind(Operation.EXIT, "q", ":q", "Q", ":Q", "ZZ");
        map.bind(Operation.FORWARD_ONE_LINE, "e", ctrl('E'), "j", ctrl('N'), "\r", key(console, Capability.key_down));
        map.bind(Operation.BACKWARD_ONE_LINE, "y", ctrl('Y'), "k", ctrl('K'), ctrl('P'), key(console, Capability.key_up));
        map.bind(Operation.FORWARD_ONE_WINDOW_OR_LINES, "f", ctrl('F'), ctrl('V'), " ");
        map.bind(Operation.BACKWARD_ONE_WINDOW_OR_LINES, "b", ctrl('B'), alt('v'));
        map.bind(Operation.FORWARD_ONE_WINDOW_AND_SET, "z");
        map.bind(Operation.BACKWARD_ONE_WINDOW_AND_SET, "w");
        map.bind(Operation.FORWARD_ONE_WINDOW_NO_STOP, alt(' '));
        map.bind(Operation.FORWARD_HALF_WINDOW_AND_SET, "d", ctrl('D'));
        map.bind(Operation.BACKWARD_HALF_WINDOW_AND_SET, "u", ctrl('U'));
        map.bind(Operation.RIGHT_ONE_HALF_SCREEN, alt(')'), key(console, Capability.key_right));
        map.bind(Operation.LEFT_ONE_HALF_SCREEN, alt('('), key(console, Capability.key_left));
        map.bind(Operation.FORWARD_FOREVER, "F");
        map.bind(Operation.REPEAT_SEARCH_FORWARD, "n", "N");
        map.bind(Operation.REPEAT_SEARCH_FORWARD_SPAN_FILES, alt('n'), alt('N'));
        map.bind(Operation.UNDO_SEARCH, alt('u'));
        map.bind(Operation.GO_TO_FIRST_LINE_OR_N, "g", "<", alt('<'));
        map.bind(Operation.GO_TO_LAST_LINE_OR_N, "G", ">", alt('>'));
        map.bind(Operation.NEXT_FILE, ":n");
        map.bind(Operation.PREV_FILE, ":p");
        "-/0123456789?".chars().forEach(c -> map.bind(Operation.CHAR, Character.toString((char) c)));
    }

    enum Operation implements Binding {

        // General
        HELP,
        EXIT,

        // Moving
        FORWARD_ONE_LINE,
        BACKWARD_ONE_LINE,
        FORWARD_ONE_WINDOW_OR_LINES,
        BACKWARD_ONE_WINDOW_OR_LINES,
        FORWARD_ONE_WINDOW_AND_SET,
        BACKWARD_ONE_WINDOW_AND_SET,
        FORWARD_ONE_WINDOW_NO_STOP,
        FORWARD_HALF_WINDOW_AND_SET,
        BACKWARD_HALF_WINDOW_AND_SET,
        LEFT_ONE_HALF_SCREEN,
        RIGHT_ONE_HALF_SCREEN,
        FORWARD_FOREVER,
        REPAINT,
        REPAINT_AND_DISCARD,

        // Searching
        REPEAT_SEARCH_FORWARD,
        REPEAT_SEARCH_BACKWARD,
        REPEAT_SEARCH_FORWARD_SPAN_FILES,
        REPEAT_SEARCH_BACKWARD_SPAN_FILES,
        UNDO_SEARCH,

        // Jumping
        GO_TO_FIRST_LINE_OR_N,
        GO_TO_LAST_LINE_OR_N,
        GO_TO_PERCENT_OR_N,
        GO_TO_NEXT_TAG,
        GO_TO_PREVIOUS_TAG,
        FIND_CLOSE_BRACKET,
        FIND_OPEN_BRACKET,

        // Options
        OPT_PRINT_LINES,
        OPT_CHOP_LONG_LINES,
        OPT_QUIT_AT_FIRST_EOF,
        OPT_QUIT_AT_SECOND_EOF,
        OPT_QUIET,
        OPT_VERY_QUIET,
        OPT_IGNORE_CASE_COND,
        OPT_IGNORE_CASE_ALWAYS,

        // Files
        NEXT_FILE,
        PREV_FILE,
        
        // 
        CHAR

    }

    static class InterruptibleInputStream extends FilterInputStream {
        InterruptibleInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedIOException();
            }
            return super.read(b, off, len);
        }
    }

}
