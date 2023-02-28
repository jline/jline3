/*
 * Copyright (c) 2002-2022, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jline.builtins.Nano.PatternHistory;
import org.jline.builtins.Source.ResourceSource;
import org.jline.builtins.Source.URLSource;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.Terminal.SignalHandler;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Display;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.NonBlockingReader;
import org.jline.utils.Status;

import static org.jline.builtins.SyntaxHighlighter.*;
import static org.jline.keymap.KeyMap.alt;
import static org.jline.keymap.KeyMap.ctrl;
import static org.jline.keymap.KeyMap.del;
import static org.jline.keymap.KeyMap.key;

public class Less {

    private static final int ESCAPE = 27;
    private static final String MESSAGE_FILE_INFO = "FILE_INFO";

    public boolean quitAtSecondEof;
    public boolean quitAtFirstEof;
    public boolean quitIfOneScreen;
    public boolean printLineNumbers;
    public boolean quiet;
    public boolean veryQuiet;
    public boolean chopLongLines;
    public boolean ignoreCaseCond;
    public boolean ignoreCaseAlways;
    public boolean noKeypad;
    public boolean noInit;
    protected List<Integer> tabs = Collections.singletonList(4);
    protected String syntaxName;
    private String historyLog = null;

    protected final Terminal terminal;
    protected final Display display;
    protected final BindingReader bindingReader;
    protected final Path currentDir;

    protected List<Source> sources;
    protected int sourceIdx;
    protected BufferedReader reader;
    protected KeyMap<Operation> keys;

    protected int firstLineInMemory = 0;
    protected List<AttributedString> lines = new ArrayList<>();

    protected int firstLineToDisplay = 0;
    protected int firstColumnToDisplay = 0;
    protected int offsetInLine = 0;

    protected String message;
    protected String errorMessage;
    protected final StringBuilder buffer = new StringBuilder();

    protected final Map<String, Operation> options = new TreeMap<>();

    protected int window;
    protected int halfWindow;

    protected int nbEof;

    protected PatternHistory patternHistory = new PatternHistory(null);
    protected String pattern;
    protected String displayPattern;

    protected final Size size = new Size();

    SyntaxHighlighter syntaxHighlighter;
    private final List<Path> syntaxFiles = new ArrayList<>();
    private boolean highlight = true;
    private boolean nanorcIgnoreErrors;

    public static String[] usage() {
        return new String[] {
            "less -  file pager",
            "Usage: less [OPTIONS] [FILES]",
            "  -? --help                    Show help",
            "  -e --quit-at-eof             Exit on second EOF",
            "  -E --QUIT-AT-EOF             Exit on EOF",
            "  -F --quit-if-one-screen      Exit if entire file fits on first screen",
            "  -q --quiet --silent          Silent mode",
            "  -Q --QUIET --SILENT          Completely silent",
            "  -S --chop-long-lines         Do not fold long lines",
            "  -i --ignore-case             Search ignores lowercase case",
            "  -I --IGNORE-CASE             Search ignores all case",
            "  -x --tabs=N[,...]            Set tab stops",
            "  -N --LINE-NUMBERS            Display line number for each line",
            "  -Y --syntax=name             The name of the syntax highlighting to use.",
            "     --no-init                 Disable terminal initialization",
            "     --no-keypad               Disable keypad handling",
            "     --ignorercfiles           Don't look at the system's lessrc nor at the user's lessrc.",
            "  -H --historylog=name         Log search strings to file, so they can be retrieved in later sessions"
        };
    }

    public Less(Terminal terminal, Path currentDir) {
        this(terminal, currentDir, null);
    }

    public Less(Terminal terminal, Path currentDir, Options opts) {
        this(terminal, currentDir, opts, null);
    }

    public Less(Terminal terminal, Path currentDir, Options opts, ConfigurationPath configPath) {
        this.terminal = terminal;
        this.display = new Display(terminal, true);
        this.bindingReader = new BindingReader(terminal.reader());
        this.currentDir = currentDir;
        Path lessrc = configPath != null ? configPath.getConfig(DEFAULT_LESSRC_FILE) : null;
        boolean ignorercfiles = opts != null && opts.isSet("ignorercfiles");
        if (lessrc != null && !ignorercfiles) {
            try {
                parseConfig(lessrc);
            } catch (IOException e) {
                errorMessage = "Encountered error while reading config file: " + lessrc;
            }
        } else if (new File("/usr/share/nano").exists() && !ignorercfiles) {
            PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:/usr/share/nano/*.nanorc");
            try {
                Files.find(Paths.get("/usr/share/nano"), Integer.MAX_VALUE, (path, f) -> pathMatcher.matches(path))
                        .forEach(syntaxFiles::add);
                nanorcIgnoreErrors = true;
            } catch (IOException e) {
                errorMessage = "Encountered error while reading nanorc files";
            }
        }
        if (opts != null) {
            if (opts.isSet("QUIT-AT-EOF")) {
                quitAtFirstEof = true;
            }
            if (opts.isSet("quit-at-eof")) {
                quitAtSecondEof = true;
            }
            if (opts.isSet("quit-if-one-screen")) {
                quitIfOneScreen = true;
            }
            if (opts.isSet("quiet")) {
                quiet = true;
            }
            if (opts.isSet("QUIET")) {
                veryQuiet = true;
            }
            if (opts.isSet("chop-long-lines")) {
                chopLongLines = true;
            }
            if (opts.isSet("IGNORE-CASE")) {
                ignoreCaseAlways = true;
            }
            if (opts.isSet("ignore-case")) {
                ignoreCaseCond = true;
            }
            if (opts.isSet("LINE-NUMBERS")) {
                printLineNumbers = true;
            }
            if (opts.isSet("tabs")) {
                doTabs(opts.get("tabs"));
            }
            if (opts.isSet("syntax")) {
                syntaxName = opts.get("syntax");
                nanorcIgnoreErrors = false;
            }
            if (opts.isSet("no-init")) {
                noInit = true;
            }
            if (opts.isSet("no-keypad")) {
                noKeypad = true;
            }
            if (opts.isSet("historylog")) {
                historyLog = opts.get("historylog");
            }
        }
        if (configPath != null && historyLog != null) {
            try {
                patternHistory = new PatternHistory(configPath.getUserConfig(historyLog, true));
            } catch (IOException e) {
                errorMessage = "Encountered error while reading pattern-history file: " + historyLog;
            }
        }
    }

    private void parseConfig(Path file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {
            String line = reader.readLine();
            while (line != null) {
                line = line.trim();
                if (line.length() > 0 && !line.startsWith("#")) {
                    List<String> parts = SyntaxHighlighter.RuleSplitter.split(line);
                    if (parts.get(0).equals(COMMAND_INCLUDE)) {
                        SyntaxHighlighter.nanorcInclude(parts.get(1), syntaxFiles);
                    } else if (parts.get(0).equals(COMMAND_THEME)) {
                        SyntaxHighlighter.nanorcTheme(parts.get(1), syntaxFiles);
                    } else if (parts.size() == 2
                            && (parts.get(0).equals("set") || parts.get(0).equals("unset"))) {
                        String option = parts.get(1);
                        boolean val = parts.get(0).equals("set");
                        if (option.equals("QUIT-AT-EOF")) {
                            quitAtFirstEof = val;
                        } else if (option.equals("quit-at-eof")) {
                            quitAtSecondEof = val;
                        } else if (option.equals("quit-if-one-screen")) {
                            quitIfOneScreen = val;
                        } else if (option.equals("quiet") || option.equals("silent")) {
                            quiet = val;
                        } else if (option.equals("QUIET") || option.equals("SILENT")) {
                            veryQuiet = val;
                        } else if (option.equals("chop-long-lines")) {
                            chopLongLines = val;
                        } else if (option.equals("IGNORE-CASE")) {
                            ignoreCaseAlways = val;
                        } else if (option.equals("ignore-case")) {
                            ignoreCaseCond = val;
                        } else if (option.equals("LINE-NUMBERS")) {
                            printLineNumbers = val;
                        } else {
                            errorMessage = "Less config: Unknown or unsupported configuration option " + option;
                        }
                    } else if (parts.size() == 3 && parts.get(0).equals("set")) {
                        String option = parts.get(1);
                        String val = parts.get(2);
                        if (option.equals("tabs")) {
                            doTabs(val);
                        } else if (option.equals("historylog")) {
                            historyLog = val;
                        } else {
                            errorMessage = "Less config: Unknown or unsupported configuration option " + option;
                        }
                    } else if (parts.get(0).equals("bind") || parts.get(0).equals("unbind")) {
                        errorMessage = "Less config: Key bindings can not be changed!";
                    } else {
                        errorMessage = "Less config: Bad configuration '" + line + "'";
                    }
                }
                line = reader.readLine();
            }
        }
    }

    private void doTabs(String val) {
        tabs = new ArrayList<>();
        for (String s : val.split(",")) {
            try {
                tabs.add(Integer.parseInt(s));
            } catch (Exception ex) {
                errorMessage = "Less config: tabs option error parsing number: " + s;
            }
        }
    }

    // to be removed
    public Less tabs(List<Integer> tabs) {
        this.tabs = tabs;
        return this;
    }

    public void handle(Signal signal) {
        size.copy(terminal.getSize());
        try {
            display.clear();
            display(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run(Source... sources) throws IOException, InterruptedException {
        run(new ArrayList<>(Arrays.asList(sources)));
    }

    public void run(List<Source> sources) throws IOException, InterruptedException {
        if (sources == null || sources.isEmpty()) {
            throw new IllegalArgumentException("No sources");
        }
        sources.add(0, new ResourceSource("less-help.txt", "HELP -- Press SPACE for more, or q when done"));
        this.sources = sources;

        sourceIdx = 1;
        openSource();
        if (errorMessage != null) {
            message = errorMessage;
            errorMessage = null;
        }
        Status status = Status.getStatus(terminal, false);

        try {
            if (status != null) {
                status.suspend();
            }
            size.copy(terminal.getSize());

            if (quitIfOneScreen && sources.size() == 2) {
                if (display(true)) {
                    return;
                }
            }

            SignalHandler prevHandler = terminal.handle(Signal.WINCH, this::handle);
            Attributes attr = terminal.enterRawMode();
            try {
                window = size.getRows() - 1;
                halfWindow = window / 2;
                keys = new KeyMap<>();
                bindKeys(keys);

                // Use alternate buffer
                if (!noInit) {
                    terminal.puts(Capability.enter_ca_mode);
                }
                if (!noKeypad) {
                    terminal.puts(Capability.keypad_xmit);
                }
                terminal.writer().flush();

                display.clear();
                display(false);
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
                options.put("-Y", Operation.OPT_SYNTAX_HIGHLIGHT);
                options.put("--syntax", Operation.OPT_SYNTAX_HIGHLIGHT);

                Operation op;
                boolean forward = true;
                do {
                    checkInterrupted();

                    op = null;
                    //
                    // Option edition
                    //
                    if (buffer.length() > 0 && buffer.charAt(0) == '-') {
                        int c = terminal.reader().read();
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
                    //
                    // Pattern edition
                    //
                    else if (buffer.length() > 0
                            && (buffer.charAt(0) == '/' || buffer.charAt(0) == '?' || buffer.charAt(0) == '&')) {
                        forward = search();
                    }
                    //
                    // Command reading
                    //
                    else {
                        Operation obj = bindingReader.readBinding(keys, null, false);
                        if (obj == Operation.CHAR) {
                            char c = bindingReader.getLastBinding().charAt(0);
                            // Enter option mode or pattern edit mode
                            if (c == '-' || c == '/' || c == '?' || c == '&') {
                                buffer.setLength(0);
                            }
                            buffer.append(c);
                        } else if (obj == Operation.BACKSPACE) {
                            if (buffer.length() > 0) {
                                buffer.deleteCharAt(buffer.length() - 1);
                            }
                        } else {
                            op = obj;
                        }
                    }
                    if (op != null) {
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
                                moveTo(getStrictPositiveNumberInBuffer(1) - 1);
                                break;
                            case GO_TO_LAST_LINE_OR_N:
                                int lineNum = getStrictPositiveNumberInBuffer(0) - 1;
                                if (lineNum < 0) {
                                    moveForward(Integer.MAX_VALUE);
                                } else {
                                    moveTo(lineNum);
                                }
                                break;
                            case HOME:
                                moveTo(0);
                                break;
                            case END:
                                moveForward(Integer.MAX_VALUE);
                                break;
                            case LEFT_ONE_HALF_SCREEN:
                                firstColumnToDisplay = Math.max(0, firstColumnToDisplay - size.getColumns() / 2);
                                break;
                            case RIGHT_ONE_HALF_SCREEN:
                                firstColumnToDisplay += size.getColumns() / 2;
                                break;
                            case REPEAT_SEARCH_BACKWARD_SPAN_FILES:
                                moveToMatch(!forward, true);
                                break;
                            case REPEAT_SEARCH_BACKWARD:
                                moveToMatch(!forward, false);
                                break;
                            case REPEAT_SEARCH_FORWARD_SPAN_FILES:
                                moveToMatch(forward, true);
                                break;
                            case REPEAT_SEARCH_FORWARD:
                                moveToMatch(forward, false);
                                break;
                            case UNDO_SEARCH:
                                pattern = null;
                                break;
                            case OPT_PRINT_LINES:
                                buffer.setLength(0);
                                printLineNumbers = !printLineNumbers;
                                message =
                                        printLineNumbers ? "Constantly display line numbers" : "Don't use line numbers";
                                break;
                            case OPT_QUIET:
                                buffer.setLength(0);
                                quiet = !quiet;
                                veryQuiet = false;
                                message = quiet
                                        ? "Ring the bell for errors but not at eof/bof"
                                        : "Ring the bell for errors AND at eof/bof";
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
                                display.clear();
                                break;
                            case OPT_IGNORE_CASE_COND:
                                ignoreCaseCond = !ignoreCaseCond;
                                ignoreCaseAlways = false;
                                message =
                                        ignoreCaseCond ? "Ignore case in searches" : "Case is significant in searches";
                                break;
                            case OPT_IGNORE_CASE_ALWAYS:
                                ignoreCaseAlways = !ignoreCaseAlways;
                                ignoreCaseCond = false;
                                message = ignoreCaseAlways
                                        ? "Ignore case in searches and in patterns"
                                        : "Case is significant in searches";
                                break;
                            case OPT_SYNTAX_HIGHLIGHT:
                                highlight = !highlight;
                                message = "Highlight " + (highlight ? "enabled" : "disabled");
                                break;
                            case ADD_FILE:
                                addFile();
                                break;
                            case NEXT_FILE:
                                int next = getStrictPositiveNumberInBuffer(1);
                                if (sourceIdx < sources.size() - next) {
                                    SavedSourcePositions ssp = new SavedSourcePositions();
                                    sourceIdx += next;
                                    String newSource = sources.get(sourceIdx).getName();
                                    try {
                                        openSource();
                                    } catch (FileNotFoundException exp) {
                                        ssp.restore(newSource);
                                    }
                                } else {
                                    message = "No next file";
                                }
                                break;
                            case PREV_FILE:
                                int prev = getStrictPositiveNumberInBuffer(1);
                                if (sourceIdx > prev) {
                                    SavedSourcePositions ssp = new SavedSourcePositions(-1);
                                    sourceIdx -= prev;
                                    String newSource = sources.get(sourceIdx).getName();
                                    try {
                                        openSource();
                                    } catch (FileNotFoundException exp) {
                                        ssp.restore(newSource);
                                    }
                                } else {
                                    message = "No previous file";
                                }
                                break;
                            case GOTO_FILE:
                                int tofile = getStrictPositiveNumberInBuffer(1);
                                if (tofile < sources.size()) {
                                    SavedSourcePositions ssp = new SavedSourcePositions(tofile < sourceIdx ? -1 : 0);
                                    sourceIdx = tofile;
                                    String newSource = sources.get(sourceIdx).getName();
                                    try {
                                        openSource();
                                    } catch (FileNotFoundException exp) {
                                        ssp.restore(newSource);
                                    }
                                } else {
                                    message = "No such file";
                                }
                                break;
                            case INFO_FILE:
                                message = MESSAGE_FILE_INFO;
                                break;
                            case DELETE_FILE:
                                if (sources.size() > 2) {
                                    sources.remove(sourceIdx);
                                    if (sourceIdx >= sources.size()) {
                                        sourceIdx = sources.size() - 1;
                                    }
                                    openSource();
                                }
                                break;
                            case REPAINT:
                                size.copy(terminal.getSize());
                                display.clear();
                                break;
                            case REPAINT_AND_DISCARD:
                                message = null;
                                size.copy(terminal.getSize());
                                display.clear();
                                break;
                            case HELP:
                                help();
                                break;
                        }
                        buffer.setLength(0);
                    }
                    if (quitAtFirstEof && nbEof > 0 || quitAtSecondEof && nbEof > 1) {
                        if (sourceIdx < sources.size() - 1) {
                            sourceIdx++;
                            openSource();
                        } else {
                            op = Operation.EXIT;
                        }
                    }
                    display(false);
                } while (op != Operation.EXIT);
            } catch (InterruptedException ie) {
                // Do nothing
            } finally {
                terminal.setAttributes(attr);
                if (prevHandler != null) {
                    terminal.handle(Terminal.Signal.WINCH, prevHandler);
                }
                // Use main buffer
                if (!noInit) {
                    terminal.puts(Capability.exit_ca_mode);
                }
                if (!noKeypad) {
                    terminal.puts(Capability.keypad_local);
                }
                terminal.writer().flush();
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (status != null) {
                status.restore();
            }
            patternHistory.persist();
        }
    }

    private void moveToMatch(boolean forward, boolean spanFiles) throws IOException {
        if (forward) {
            moveToNextMatch(spanFiles);
        } else {
            moveToPreviousMatch(spanFiles);
        }
    }

    private class LineEditor {
        private final int begPos;

        public LineEditor(int begPos) {
            this.begPos = begPos;
        }

        public int editBuffer(Operation op, int curPos) {
            switch (op) {
                case INSERT:
                    buffer.insert(curPos++, bindingReader.getLastBinding());
                    break;
                case BACKSPACE:
                    if (curPos > begPos - 1) {
                        buffer.deleteCharAt(--curPos);
                    }
                    break;
                case NEXT_WORD:
                    int newPos = buffer.length();
                    for (int i = curPos; i < buffer.length(); i++) {
                        if (buffer.charAt(i) == ' ') {
                            newPos = i + 1;
                            break;
                        }
                    }
                    curPos = newPos;
                    break;
                case PREV_WORD:
                    newPos = begPos;
                    for (int i = curPos - 2; i > begPos; i--) {
                        if (buffer.charAt(i) == ' ') {
                            newPos = i + 1;
                            break;
                        }
                    }
                    curPos = newPos;
                    break;
                case HOME:
                    curPos = begPos;
                    break;
                case END:
                    curPos = buffer.length();
                    break;
                case DELETE:
                    if (curPos >= begPos && curPos < buffer.length()) {
                        buffer.deleteCharAt(curPos);
                    }
                    break;
                case DELETE_WORD:
                    while (true) {
                        if (curPos < buffer.length() && buffer.charAt(curPos) != ' ') {
                            buffer.deleteCharAt(curPos);
                        } else {
                            break;
                        }
                    }
                    while (true) {
                        if (curPos - 1 >= begPos) {
                            if (buffer.charAt(curPos - 1) != ' ') {
                                buffer.deleteCharAt(--curPos);
                            } else {
                                buffer.deleteCharAt(--curPos);
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    break;
                case DELETE_LINE:
                    buffer.setLength(begPos);
                    curPos = 1;
                    break;
                case LEFT:
                    if (curPos > begPos) {
                        curPos--;
                    }
                    break;
                case RIGHT:
                    if (curPos < buffer.length()) {
                        curPos++;
                    }
                    break;
            }
            return curPos;
        }
    }

    private class SavedSourcePositions {
        int saveSourceIdx;
        int saveFirstLineToDisplay;
        int saveFirstColumnToDisplay;
        int saveOffsetInLine;
        boolean savePrintLineNumbers;

        public SavedSourcePositions() {
            this(0);
        }

        public SavedSourcePositions(int dec) {
            saveSourceIdx = sourceIdx + dec;
            saveFirstLineToDisplay = firstLineToDisplay;
            saveFirstColumnToDisplay = firstColumnToDisplay;
            saveOffsetInLine = offsetInLine;
            savePrintLineNumbers = printLineNumbers;
        }

        public void restore(String failingSource) throws IOException {
            sourceIdx = saveSourceIdx;
            openSource();
            firstLineToDisplay = saveFirstLineToDisplay;
            firstColumnToDisplay = saveFirstColumnToDisplay;
            offsetInLine = saveOffsetInLine;
            printLineNumbers = savePrintLineNumbers;
            if (failingSource != null) {
                message = failingSource + " not found!";
            }
        }
    }

    private void addSource(String file) throws IOException {
        if (file.contains("*") || file.contains("?")) {
            for (Path p : Commands.findFiles(currentDir, file)) {
                sources.add(new URLSource(p.toUri().toURL(), p.toString()));
            }
        } else {
            sources.add(new URLSource(currentDir.resolve(file).toUri().toURL(), file));
        }
        sourceIdx = sources.size() - 1;
    }

    private void addFile() throws IOException, InterruptedException {
        KeyMap<Operation> fileKeyMap = new KeyMap<>();
        fileKeyMap.setUnicode(Operation.INSERT);
        for (char i = 32; i < 256; i++) {
            fileKeyMap.bind(Operation.INSERT, Character.toString(i));
        }
        fileKeyMap.bind(Operation.RIGHT, key(terminal, Capability.key_right), alt('l'));
        fileKeyMap.bind(Operation.LEFT, key(terminal, Capability.key_left), alt('h'));
        fileKeyMap.bind(Operation.HOME, key(terminal, Capability.key_home), alt('0'));
        fileKeyMap.bind(Operation.END, key(terminal, Capability.key_end), alt('$'));
        fileKeyMap.bind(Operation.BACKSPACE, del());
        fileKeyMap.bind(Operation.DELETE, alt('x'));
        fileKeyMap.bind(Operation.DELETE_WORD, alt('X'));
        fileKeyMap.bind(Operation.DELETE_LINE, ctrl('U'));
        fileKeyMap.bind(Operation.ACCEPT, "\r");

        SavedSourcePositions ssp = new SavedSourcePositions();
        message = null;
        buffer.append("Examine: ");
        int curPos = buffer.length();
        final int begPos = curPos;
        display(false, curPos);
        LineEditor lineEditor = new LineEditor(begPos);
        while (true) {
            checkInterrupted();
            Operation op;
            switch (op = bindingReader.readBinding(fileKeyMap)) {
                case ACCEPT:
                    String name = buffer.substring(begPos);
                    addSource(name);
                    try {
                        openSource();
                    } catch (Exception exp) {
                        ssp.restore(name);
                    }
                    return;
                default:
                    curPos = lineEditor.editBuffer(op, curPos);
                    break;
            }
            if (curPos > begPos) {
                display(false, curPos);
            } else {
                buffer.setLength(0);
                return;
            }
        }
    }

    private boolean search() throws IOException, InterruptedException {
        KeyMap<Operation> searchKeyMap = new KeyMap<>();
        searchKeyMap.setUnicode(Operation.INSERT);
        for (char i = 32; i < 256; i++) {
            searchKeyMap.bind(Operation.INSERT, Character.toString(i));
        }
        searchKeyMap.bind(Operation.RIGHT, key(terminal, Capability.key_right), alt('l'));
        searchKeyMap.bind(Operation.LEFT, key(terminal, Capability.key_left), alt('h'));
        searchKeyMap.bind(Operation.NEXT_WORD, alt('w'));
        searchKeyMap.bind(Operation.PREV_WORD, alt('b'));
        searchKeyMap.bind(Operation.HOME, key(terminal, Capability.key_home), alt('0'));
        searchKeyMap.bind(Operation.END, key(terminal, Capability.key_end), alt('$'));
        searchKeyMap.bind(Operation.BACKSPACE, del());
        searchKeyMap.bind(Operation.DELETE, alt('x'));
        searchKeyMap.bind(Operation.DELETE_WORD, alt('X'));
        searchKeyMap.bind(Operation.DELETE_LINE, ctrl('U'));
        searchKeyMap.bind(Operation.UP, key(terminal, Capability.key_up), alt('k'));
        searchKeyMap.bind(Operation.DOWN, key(terminal, Capability.key_down), alt('j'));
        searchKeyMap.bind(Operation.ACCEPT, "\r");

        boolean forward = true;
        message = null;
        int curPos = buffer.length();
        final int begPos = curPos;
        final char type = buffer.charAt(0);
        String currentBuffer = buffer.toString();
        LineEditor lineEditor = new LineEditor(begPos);
        while (true) {
            checkInterrupted();
            Operation op;
            switch (op = bindingReader.readBinding(searchKeyMap)) {
                case UP:
                    buffer.setLength(0);
                    buffer.append(type);
                    buffer.append(patternHistory.up(currentBuffer.substring(1)));
                    curPos = buffer.length();
                    break;
                case DOWN:
                    buffer.setLength(0);
                    buffer.append(type);
                    buffer.append(patternHistory.down(currentBuffer.substring(1)));
                    curPos = buffer.length();
                    break;
                case ACCEPT:
                    try {
                        String _pattern = buffer.substring(1);
                        if (type == '&') {
                            displayPattern = _pattern.length() > 0 ? _pattern : null;
                            getPattern(true);
                        } else {
                            pattern = _pattern;
                            getPattern();
                            if (type == '/') {
                                moveToNextMatch();
                            } else {
                                if (lines.size() - firstLineToDisplay <= size.getRows()) {
                                    firstLineToDisplay = lines.size();
                                } else {
                                    moveForward(size.getRows() - 1);
                                }
                                moveToPreviousMatch();
                                forward = false;
                            }
                        }
                        patternHistory.add(_pattern);
                        buffer.setLength(0);
                    } catch (PatternSyntaxException e) {
                        String str = e.getMessage();
                        if (str.indexOf('\n') > 0) {
                            str = str.substring(0, str.indexOf('\n'));
                        }
                        if (type == '&') {
                            displayPattern = null;
                        } else {
                            pattern = null;
                        }
                        buffer.setLength(0);
                        message = "Invalid pattern: " + str + " (Press a key)";
                        display(false);
                        terminal.reader().read();
                        message = null;
                    }
                    return forward;
                default:
                    curPos = lineEditor.editBuffer(op, curPos);
                    currentBuffer = buffer.toString();
                    break;
            }
            if (curPos < begPos) {
                buffer.setLength(0);
                return forward;
            } else {
                display(false, curPos);
            }
        }
    }

    private void help() throws IOException {
        SavedSourcePositions ssp = new SavedSourcePositions();
        printLineNumbers = false;
        sourceIdx = 0;
        try {
            openSource();
            display(false);
            Operation op;
            do {
                checkInterrupted();
                op = bindingReader.readBinding(keys, null, false);
                if (op != null) {
                    switch (op) {
                        case FORWARD_ONE_WINDOW_OR_LINES:
                            moveForward(getStrictPositiveNumberInBuffer(window));
                            break;
                        case BACKWARD_ONE_WINDOW_OR_LINES:
                            moveBackward(getStrictPositiveNumberInBuffer(window));
                            break;
                    }
                }
                display(false);
            } while (op != Operation.EXIT);
        } catch (IOException | InterruptedException exp) {
            // Do nothing
        } finally {
            ssp.restore(null);
        }
    }

    protected void openSource() throws IOException {
        boolean wasOpen = false;
        if (reader != null) {
            reader.close();
            wasOpen = true;
        }
        boolean open;
        boolean displayMessage = false;
        do {
            Source source = sources.get(sourceIdx);
            try {
                InputStream in = source.read();
                if (sources.size() == 2 || sourceIdx == 0) {
                    message = source.getName();
                } else {
                    message = source.getName() + " (file " + sourceIdx + " of " + (sources.size() - 1) + ")";
                }
                reader = new BufferedReader(new InputStreamReader(new InterruptibleInputStream(in)));
                firstLineInMemory = 0;
                lines = new ArrayList<>();
                firstLineToDisplay = 0;
                firstColumnToDisplay = 0;
                offsetInLine = 0;
                display.clear();
                if (sourceIdx == 0) {
                    syntaxHighlighter = SyntaxHighlighter.build(syntaxFiles, null, "none");
                } else {
                    syntaxHighlighter =
                            SyntaxHighlighter.build(syntaxFiles, source.getName(), syntaxName, nanorcIgnoreErrors);
                }
                open = true;
                if (displayMessage) {
                    AttributedStringBuilder asb = new AttributedStringBuilder();
                    asb.style(AttributedStyle.INVERSE);
                    asb.append(source.getName() + " (press RETURN)");
                    asb.toAttributedString().println(terminal);
                    terminal.writer().flush();
                    terminal.reader().read();
                }
            } catch (FileNotFoundException exp) {
                sources.remove(sourceIdx);
                if (sourceIdx > sources.size() - 1) {
                    sourceIdx = sources.size() - 1;
                }
                if (wasOpen) {
                    throw exp;
                } else {
                    AttributedStringBuilder asb = new AttributedStringBuilder();
                    asb.append(source.getName() + " not found!");
                    asb.toAttributedString().println(terminal);
                    terminal.writer().flush();
                    open = false;
                    displayMessage = true;
                }
            }
        } while (!open && sourceIdx > 0);
        if (!open) {
            throw new FileNotFoundException();
        }
    }

    void moveTo(int lineNum) throws IOException {
        AttributedString line = getLine(lineNum);
        if (line != null) {
            display.clear();
            if (firstLineInMemory > lineNum) {
                openSource();
            }
            firstLineToDisplay = lineNum;
            offsetInLine = 0;
        } else {
            message = "Cannot seek to line number " + (lineNum + 1);
        }
    }

    private void moveToNextMatch() throws IOException {
        moveToNextMatch(false);
    }

    private void moveToNextMatch(boolean spanFiles) throws IOException {
        Pattern compiled = getPattern();
        Pattern dpCompiled = getPattern(true);
        if (compiled != null) {
            for (int lineNumber = firstLineToDisplay + 1; ; lineNumber++) {
                AttributedString line = getLine(lineNumber);
                if (line == null) {
                    break;
                } else if (!toBeDisplayed(line, dpCompiled)) {
                    continue;
                } else if (compiled.matcher(line).find()) {
                    display.clear();
                    firstLineToDisplay = lineNumber;
                    offsetInLine = 0;
                    return;
                }
            }
        }
        if (spanFiles) {
            if (sourceIdx < sources.size() - 1) {
                SavedSourcePositions ssp = new SavedSourcePositions();
                String newSource = sources.get(++sourceIdx).getName();
                try {
                    openSource();
                    moveToNextMatch(true);
                } catch (FileNotFoundException exp) {
                    ssp.restore(newSource);
                }
            } else {
                message = "Pattern not found";
            }
        } else {
            message = "Pattern not found";
        }
    }

    private void moveToPreviousMatch() throws IOException {
        moveToPreviousMatch(false);
    }

    private void moveToPreviousMatch(boolean spanFiles) throws IOException {
        Pattern compiled = getPattern();
        Pattern dpCompiled = getPattern(true);
        if (compiled != null) {
            for (int lineNumber = firstLineToDisplay - 1; lineNumber >= firstLineInMemory; lineNumber--) {
                AttributedString line = getLine(lineNumber);
                if (line == null) {
                    break;
                } else if (!toBeDisplayed(line, dpCompiled)) {
                    continue;
                } else if (compiled.matcher(line).find()) {
                    display.clear();
                    firstLineToDisplay = lineNumber;
                    offsetInLine = 0;
                    return;
                }
            }
        }
        if (spanFiles) {
            if (sourceIdx > 1) {
                SavedSourcePositions ssp = new SavedSourcePositions(-1);
                String newSource = sources.get(--sourceIdx).getName();
                try {
                    openSource();
                    moveTo(Integer.MAX_VALUE);
                    moveToPreviousMatch(true);
                } catch (FileNotFoundException exp) {
                    ssp.restore(newSource);
                }
            } else {
                message = "Pattern not found";
            }
        } else {
            message = "Pattern not found";
        }
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
        Pattern dpCompiled = getPattern(true);
        int width = size.getColumns() - (printLineNumbers ? 8 : 0);
        int height = size.getRows();
        boolean doOffsets = firstColumnToDisplay == 0 && !chopLongLines;
        if (lines >= size.getRows() - 1) {
            display.clear();
        }
        if (lines == Integer.MAX_VALUE) {
            moveTo(Integer.MAX_VALUE);
            for (int l = 0; l < height - 1; l++) {
                firstLineToDisplay =
                        prevLine2display(firstLineToDisplay, dpCompiled).getU();
            }
        }
        while (--lines >= 0) {
            int lastLineToDisplay = firstLineToDisplay;
            if (!doOffsets) {
                for (int l = 0; l < height - 1; l++) {
                    lastLineToDisplay =
                            nextLine2display(lastLineToDisplay, dpCompiled).getU();
                }
            } else {
                int off = offsetInLine;
                for (int l = 0; l < height - 1; l++) {
                    Pair<Integer, AttributedString> nextLine = nextLine2display(lastLineToDisplay, dpCompiled);
                    AttributedString line = nextLine.getV();
                    if (line == null) {
                        lastLineToDisplay = nextLine.getU();
                        break;
                    }
                    if (line.columnLength() > off + width) {
                        off += width;
                    } else {
                        off = 0;
                        lastLineToDisplay = nextLine.getU();
                    }
                }
            }
            if (getLine(lastLineToDisplay) == null) {
                eof();
                return;
            }
            Pair<Integer, AttributedString> nextLine = nextLine2display(firstLineToDisplay, dpCompiled);
            AttributedString line = nextLine.getV();
            if (doOffsets && line.columnLength() > width + offsetInLine) {
                offsetInLine += width;
            } else {
                offsetInLine = 0;
                firstLineToDisplay = nextLine.getU();
            }
        }
    }

    void moveBackward(int lines) throws IOException {
        Pattern dpCompiled = getPattern(true);
        int width = size.getColumns() - (printLineNumbers ? 8 : 0);
        if (lines >= size.getRows() - 1) {
            display.clear();
        }
        while (--lines >= 0) {
            if (offsetInLine > 0) {
                offsetInLine = Math.max(0, offsetInLine - width);
            } else if (firstLineInMemory < firstLineToDisplay) {
                Pair<Integer, AttributedString> prevLine = prevLine2display(firstLineToDisplay, dpCompiled);
                firstLineToDisplay = prevLine.getU();
                AttributedString line = prevLine.getV();
                if (line != null && firstColumnToDisplay == 0 && !chopLongLines) {
                    int length = line.columnLength();
                    offsetInLine = length - length % width;
                }
            } else {
                bof();
                return;
            }
        }
    }

    private void eof() {
        nbEof++;
        if (sourceIdx > 0 && sourceIdx < sources.size() - 1) {
            message = "(END) - Next: " + sources.get(sourceIdx + 1).getName();
        } else {
            message = "(END)";
        }
        if (!quiet && !veryQuiet && !quitAtFirstEof && !quitAtSecondEof) {
            terminal.puts(Capability.bell);
            terminal.writer().flush();
        }
    }

    private void bof() {
        if (!quiet && !veryQuiet) {
            terminal.puts(Capability.bell);
            terminal.writer().flush();
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

    private Pair<Integer, AttributedString> nextLine2display(int line, Pattern dpCompiled) throws IOException {
        AttributedString curLine;
        do {
            curLine = getLine(line++);
        } while (!toBeDisplayed(curLine, dpCompiled));
        return new Pair<>(line, curLine);
    }

    private Pair<Integer, AttributedString> prevLine2display(int line, Pattern dpCompiled) throws IOException {
        AttributedString curLine;
        do {
            curLine = getLine(line--);
        } while (line > 0 && !toBeDisplayed(curLine, dpCompiled));
        if (line == 0 && !toBeDisplayed(curLine, dpCompiled)) {
            curLine = null;
        }
        return new Pair<>(line, curLine);
    }

    private boolean toBeDisplayed(AttributedString curLine, Pattern dpCompiled) {
        return curLine == null
                || dpCompiled == null
                || sourceIdx == 0
                || dpCompiled.matcher(curLine).find();
    }

    synchronized boolean display(boolean oneScreen) throws IOException {
        return display(oneScreen, null);
    }

    synchronized boolean display(boolean oneScreen, Integer curPos) throws IOException {
        List<AttributedString> newLines = new ArrayList<>();
        int width = size.getColumns() - (printLineNumbers ? 8 : 0);
        int height = size.getRows();
        int inputLine = firstLineToDisplay;
        AttributedString curLine = null;
        Pattern compiled = getPattern();
        Pattern dpCompiled = getPattern(true);
        boolean fitOnOneScreen = false;
        boolean eof = false;
        if (highlight) {
            syntaxHighlighter.reset();
            for (int i = Math.max(0, inputLine - height); i < inputLine; i++) {
                syntaxHighlighter.highlight(getLine(i));
            }
        }
        for (int terminalLine = 0; terminalLine < height - 1; terminalLine++) {
            if (curLine == null) {
                Pair<Integer, AttributedString> nextLine = nextLine2display(inputLine, dpCompiled);
                inputLine = nextLine.getU();
                curLine = nextLine.getV();
                if (curLine == null) {
                    if (oneScreen) {
                        fitOnOneScreen = true;
                        break;
                    }
                    eof = true;
                    curLine = new AttributedString("~");
                } else if (highlight) {
                    curLine = syntaxHighlighter.highlight(curLine);
                }
                if (compiled != null) {
                    curLine = curLine.styleMatches(compiled, AttributedStyle.DEFAULT.inverse());
                }
            }
            AttributedString toDisplay;
            if (firstColumnToDisplay > 0 || chopLongLines) {
                int off = firstColumnToDisplay;
                if (terminalLine == 0 && offsetInLine > 0) {
                    off = Math.max(offsetInLine, off);
                }
                toDisplay = curLine.columnSubSequence(off, off + width);
                curLine = null;
            } else {
                if (terminalLine == 0 && offsetInLine > 0) {
                    curLine = curLine.columnSubSequence(offsetInLine, Integer.MAX_VALUE);
                }
                toDisplay = curLine.columnSubSequence(0, width);
                curLine = curLine.columnSubSequence(width, Integer.MAX_VALUE);
                if (curLine.length() == 0) {
                    curLine = null;
                }
            }
            if (printLineNumbers && !eof) {
                AttributedStringBuilder sb = new AttributedStringBuilder();
                sb.append(String.format("%7d ", inputLine));
                sb.append(toDisplay);
                newLines.add(sb.toAttributedString());
            } else {
                newLines.add(toDisplay);
            }
        }
        if (oneScreen) {
            if (fitOnOneScreen) {
                newLines.forEach(l -> l.println(terminal));
            }
            return fitOnOneScreen;
        }
        AttributedStringBuilder msg = new AttributedStringBuilder();
        if (MESSAGE_FILE_INFO.equals(message)) {
            Source source = sources.get(sourceIdx);
            Long allLines = source.lines();
            message = source.getName()
                    + (sources.size() > 2 ? " (file " + sourceIdx + " of " + (sources.size() - 1) + ")" : "")
                    + " lines " + (firstLineToDisplay + 1) + "-" + inputLine + "/"
                    + (allLines != null ? allLines : lines.size())
                    + (eof ? " (END)" : "");
        }
        if (buffer.length() > 0) {
            msg.append(" ").append(buffer);
        } else if (bindingReader.getCurrentBuffer().length() > 0
                && terminal.reader().peek(1) == NonBlockingReader.READ_EXPIRED) {
            msg.append(" ").append(printable(bindingReader.getCurrentBuffer()));
        } else if (message != null) {
            msg.style(AttributedStyle.INVERSE);
            msg.append(message);
            msg.style(AttributedStyle.INVERSE.inverseOff());
        } else if (displayPattern != null) {
            msg.append("&");
        } else {
            msg.append(":");
        }
        newLines.add(msg.toAttributedString());

        display.resize(size.getRows(), size.getColumns());
        if (curPos == null) {
            display.update(newLines, -1);
        } else {
            display.update(newLines, size.cursorPos(size.getRows() - 1, curPos + 1));
        }
        return false;
    }

    private Pattern getPattern() {
        return getPattern(false);
    }

    private Pattern getPattern(boolean doDisplayPattern) {
        Pattern compiled = null;
        String _pattern = doDisplayPattern ? displayPattern : pattern;
        if (_pattern != null) {
            boolean insensitive =
                    ignoreCaseAlways || ignoreCaseCond && _pattern.toLowerCase().equals(_pattern);
            compiled = Pattern.compile(
                    "(" + _pattern + ")", insensitive ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0);
        }
        return compiled;
    }

    AttributedString getLine(int line) throws IOException {
        while (line >= lines.size()) {
            String str = reader.readLine();
            if (str != null) {
                lines.add(AttributedString.fromAnsi(str, tabs));
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
     * @throws InterruptedException if the thread has been interruped
     */
    public static void checkInterrupted() throws InterruptedException {
        Thread.yield();
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }

    private void bindKeys(KeyMap<Operation> map) {
        map.bind(Operation.HELP, "h", "H");
        map.bind(Operation.EXIT, "q", ":q", "Q", ":Q", "ZZ");
        map.bind(Operation.FORWARD_ONE_LINE, "e", ctrl('E'), "j", ctrl('N'), "\r", key(terminal, Capability.key_down));
        map.bind(
                Operation.BACKWARD_ONE_LINE,
                "y",
                ctrl('Y'),
                "k",
                ctrl('K'),
                ctrl('P'),
                key(terminal, Capability.key_up));
        map.bind(
                Operation.FORWARD_ONE_WINDOW_OR_LINES,
                "f",
                ctrl('F'),
                ctrl('V'),
                " ",
                key(terminal, Capability.key_npage));
        map.bind(Operation.BACKWARD_ONE_WINDOW_OR_LINES, "b", ctrl('B'), alt('v'), key(terminal, Capability.key_ppage));
        map.bind(Operation.FORWARD_ONE_WINDOW_AND_SET, "z");
        map.bind(Operation.BACKWARD_ONE_WINDOW_AND_SET, "w");
        map.bind(Operation.FORWARD_ONE_WINDOW_NO_STOP, alt(' '));
        map.bind(Operation.FORWARD_HALF_WINDOW_AND_SET, "d", ctrl('D'));
        map.bind(Operation.BACKWARD_HALF_WINDOW_AND_SET, "u", ctrl('U'));
        map.bind(Operation.RIGHT_ONE_HALF_SCREEN, alt(')'), key(terminal, Capability.key_right));
        map.bind(Operation.LEFT_ONE_HALF_SCREEN, alt('('), key(terminal, Capability.key_left));
        map.bind(Operation.FORWARD_FOREVER, "F");
        map.bind(Operation.REPAINT, "r", ctrl('R'), ctrl('L'));
        map.bind(Operation.REPAINT_AND_DISCARD, "R");
        map.bind(Operation.REPEAT_SEARCH_FORWARD, "n");
        map.bind(Operation.REPEAT_SEARCH_BACKWARD, "N");
        map.bind(Operation.REPEAT_SEARCH_FORWARD_SPAN_FILES, alt('n'));
        map.bind(Operation.REPEAT_SEARCH_BACKWARD_SPAN_FILES, alt('N'));
        map.bind(Operation.UNDO_SEARCH, alt('u'));
        map.bind(Operation.GO_TO_FIRST_LINE_OR_N, "g", "<", alt('<'));
        map.bind(Operation.GO_TO_LAST_LINE_OR_N, "G", ">", alt('>'));
        map.bind(Operation.HOME, key(terminal, Capability.key_home));
        map.bind(Operation.END, key(terminal, Capability.key_end));
        map.bind(Operation.ADD_FILE, ":e", ctrl('X') + ctrl('V'));
        map.bind(Operation.NEXT_FILE, ":n");
        map.bind(Operation.PREV_FILE, ":p");
        map.bind(Operation.GOTO_FILE, ":x");
        map.bind(Operation.INFO_FILE, "=", ":f", ctrl('G'));
        map.bind(Operation.DELETE_FILE, ":d");
        map.bind(Operation.BACKSPACE, del());
        "-/0123456789?&".chars().forEach(c -> map.bind(Operation.CHAR, Character.toString((char) c)));
    }

    protected enum Operation {

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
        OPT_SYNTAX_HIGHLIGHT,

        // Files
        ADD_FILE,
        NEXT_FILE,
        PREV_FILE,
        GOTO_FILE,
        INFO_FILE,
        DELETE_FILE,

        //
        CHAR,

        // Edit pattern
        INSERT,
        RIGHT,
        LEFT,
        NEXT_WORD,
        PREV_WORD,
        HOME,
        END,
        BACKSPACE,
        DELETE,
        DELETE_WORD,
        DELETE_LINE,
        ACCEPT,
        UP,
        DOWN
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

    static class Pair<U, V> {
        final U u;
        final V v;

        public Pair(U u, V v) {
            this.u = u;
            this.v = v;
        }

        public U getU() {
            return u;
        }

        public V getV() {
            return v;
        }
    }
}
