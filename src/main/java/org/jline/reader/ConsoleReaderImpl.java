/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.io.Flushable;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jline.Candidate;
import org.jline.Completer;
import org.jline.Console;
import org.jline.Console.Signal;
import org.jline.Console.SignalHandler;
import org.jline.ConsoleReader;
import org.jline.Highlighter;
import org.jline.History;
import org.jline.console.Attributes;
import org.jline.console.Attributes.ControlChar;
import org.jline.console.Size;
import org.jline.keymap.Binding;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.keymap.Macro;
import org.jline.keymap.Reference;
import org.jline.keymap.Widget;
import org.jline.reader.history.MemoryHistory;
import org.jline.utils.Ansi;
import org.jline.utils.Ansi.Attribute;
import org.jline.utils.Ansi.Color;
import org.jline.utils.AnsiHelper;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.Levenshtein;
import org.jline.utils.Log;
import org.jline.utils.Signals;
import org.jline.utils.WCWidth;

import static org.jline.keymap.KeyMap.alt;
import static org.jline.keymap.KeyMap.ctrl;
import static org.jline.keymap.KeyMap.del;
import static org.jline.keymap.KeyMap.esc;
import static org.jline.keymap.KeyMap.range;
import static org.jline.keymap.KeyMap.translate;
import static org.jline.reader.Operation.ACCEPT_LINE;
import static org.jline.reader.Operation.ARGUMENT_BASE;
import static org.jline.reader.Operation.BACKWARD_CHAR;
import static org.jline.reader.Operation.BACKWARD_DELETE_CHAR;
import static org.jline.reader.Operation.BACKWARD_KILL_LINE;
import static org.jline.reader.Operation.BACKWARD_KILL_WORD;
import static org.jline.reader.Operation.BACKWARD_WORD;
import static org.jline.reader.Operation.BEGINNING_OF_HISTORY;
import static org.jline.reader.Operation.BEGINNING_OF_LINE;
import static org.jline.reader.Operation.CALLBACK_FINISH;
import static org.jline.reader.Operation.CALLBACK_INIT;
import static org.jline.reader.Operation.CALL_LAST_KBD_MACRO;
import static org.jline.reader.Operation.CAPITALIZE_WORD;
import static org.jline.reader.Operation.CHARACTER_SEARCH;
import static org.jline.reader.Operation.CHARACTER_SEARCH_BACKWARD;
import static org.jline.reader.Operation.CLEAR_SCREEN;
import static org.jline.reader.Operation.COMPLETE_PREFIX;
import static org.jline.reader.Operation.COMPLETE_WORD;
import static org.jline.reader.Operation.DELETE_CHAR;
import static org.jline.reader.Operation.DELETE_CHAR_OR_LIST;
import static org.jline.reader.Operation.DELETE_HORIZONTAL_SPACE;
import static org.jline.reader.Operation.DIGIT_ARGUMENT;
import static org.jline.reader.Operation.DOWN_CASE_WORD;
import static org.jline.reader.Operation.DOWN_LINE_OR_HISTORY;
import static org.jline.reader.Operation.DO_LOWERCASE_VERSION;
import static org.jline.reader.Operation.EMACS_EDITING_MODE;
import static org.jline.reader.Operation.END_KBD_MACRO;
import static org.jline.reader.Operation.END_OF_HISTORY;
import static org.jline.reader.Operation.END_OF_LINE;
import static org.jline.reader.Operation.EXCHANGE_POINT_AND_MARK;
import static org.jline.reader.Operation.EXIT_OR_DELETE_CHAR;
import static org.jline.reader.Operation.FORWARD_CHAR;
import static org.jline.reader.Operation.HISTORY_INCREMENTAL_SEARCH_FORWARD;
import static org.jline.reader.Operation.FORWARD_WORD;
import static org.jline.reader.Operation.HISTORY_SEARCH_BACKWARD;
import static org.jline.reader.Operation.HISTORY_SEARCH_FORWARD;
import static org.jline.reader.Operation.INSERT_CLOSE_CURLY;
import static org.jline.reader.Operation.INSERT_CLOSE_PAREN;
import static org.jline.reader.Operation.INSERT_CLOSE_SQUARE;
import static org.jline.reader.Operation.INSERT_COMMENT;
import static org.jline.reader.Operation.INSERT_COMPLETIONS;
import static org.jline.reader.Operation.INTERRUPT;
import static org.jline.reader.Operation.KILL_LINE;
import static org.jline.reader.Operation.KILL_WHOLE_LINE;
import static org.jline.reader.Operation.KILL_WORD;
import static org.jline.reader.Operation.MENU_COMPLETE;
import static org.jline.reader.Operation.NEG_ARGUMENT;
import static org.jline.reader.Operation.DOWN_HISTORY;
import static org.jline.reader.Operation.OVERWRITE_MODE;
import static org.jline.reader.Operation.PASTE_FROM_CLIPBOARD;
import static org.jline.reader.Operation.POSSIBLE_COMPLETIONS;
import static org.jline.reader.Operation.UP_HISTORY;
import static org.jline.reader.Operation.QUIT;
import static org.jline.reader.Operation.QUOTED_INSERT;
import static org.jline.reader.Operation.REVERSE_MENU_COMPLETE;
import static org.jline.reader.Operation.HISTORY_INCREMENTAL_SEARCH_BACKWARD;
import static org.jline.reader.Operation.REVERT_LINE;
import static org.jline.reader.Operation.SELF_INSERT;
import static org.jline.reader.Operation.SELF_INSERT_UNMETA;
import static org.jline.reader.Operation.SEND_BREAK;
import static org.jline.reader.Operation.SET_MARK;
import static org.jline.reader.Operation.START_KBD_MACRO;
import static org.jline.reader.Operation.TAB_INSERT;
import static org.jline.reader.Operation.TILDE_EXPAND;
import static org.jline.reader.Operation.TRANSPOSE_CHARS;
import static org.jline.reader.Operation.TRANSPOSE_WORDS;
import static org.jline.reader.Operation.UNDEFINED_KEY;
import static org.jline.reader.Operation.UNDO;
import static org.jline.reader.Operation.UNIVERSAL_ARGUMENT;
import static org.jline.reader.Operation.UP_CASE_WORD;
import static org.jline.reader.Operation.UP_LINE_OR_HISTORY;
import static org.jline.reader.Operation.VI_ADD_EOL;
import static org.jline.reader.Operation.VI_ADD_NEXT;
import static org.jline.reader.Operation.VI_BACKWARD_WORD;
import static org.jline.reader.Operation.VI_CHANGE_CASE;
import static org.jline.reader.Operation.VI_CHANGE_CHAR;
import static org.jline.reader.Operation.VI_CHANGE_TO;
import static org.jline.reader.Operation.VI_CHANGE_TO_EOL;
import static org.jline.reader.Operation.VI_CHAR_SEARCH;
import static org.jline.reader.Operation.VI_CMD_MODE;
import static org.jline.reader.Operation.VI_COLUMN;
import static org.jline.reader.Operation.VI_COMPLETE;
import static org.jline.reader.Operation.VI_DELETE;
import static org.jline.reader.Operation.VI_DELETE_TO;
import static org.jline.reader.Operation.VI_DELETE_TO_EOL;
import static org.jline.reader.Operation.VI_DIGIT_OR_BEGINNING_OF_LINE;
import static org.jline.reader.Operation.VI_END_WORD;
import static org.jline.reader.Operation.VI_EOF_MAYBE;
import static org.jline.reader.Operation.VI_FETCH_HISTORY;
import static org.jline.reader.Operation.VI_FIRST_PRINT;
import static org.jline.reader.Operation.VI_GOTO_MARK;
import static org.jline.reader.Operation.VI_INSERT;
import static org.jline.reader.Operation.VI_INSERT_BOL;
import static org.jline.reader.Operation.VI_INSERT_COMMENT;
import static org.jline.reader.Operation.VI_KILL_WHOLE_LINE;
import static org.jline.reader.Operation.VI_MATCH_BRACKET;
import static org.jline.reader.Operation.VI_MOVE_ACCEPT_LINE;
import static org.jline.reader.Operation.VI_DOWN_LINE_OR_HISTORY;
import static org.jline.reader.Operation.VI_NEXT_WORD;
import static org.jline.reader.Operation.VI_UP_LINE_OR_HISTORY;
import static org.jline.reader.Operation.VI_PREV_WORD;
import static org.jline.reader.Operation.VI_PUT;
import static org.jline.reader.Operation.VI_REDO;
import static org.jline.reader.Operation.VI_REPLACE;
import static org.jline.reader.Operation.VI_RUBOUT;
import static org.jline.reader.Operation.VI_SEARCH;
import static org.jline.reader.Operation.VI_SEARCH_AGAIN;
import static org.jline.reader.Operation.VI_SET_MARK;
import static org.jline.reader.Operation.VI_SUBST;
import static org.jline.reader.Operation.VI_TILDE_EXPAND;
import static org.jline.reader.Operation.VI_YANK_ARG;
import static org.jline.reader.Operation.VI_YANK_TO;
import static org.jline.reader.Operation.YANK;
import static org.jline.reader.Operation.YANK_LAST_ARG;
import static org.jline.reader.Operation.YANK_NTH_ARG;
import static org.jline.reader.Operation.YANK_POP;
import static org.jline.utils.Preconditions.checkNotNull;

/**
 * A reader for console applications. It supports custom tab-completion,
 * saveable command history, and command line editing.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 */
public class ConsoleReaderImpl implements ConsoleReader, Flushable
{
    public static final char NULL_MASK = 0;

    public static final int TAB_WIDTH = 4;

    public static final long BLINK_MATCHING_PAREN_TIMEOUT = 500l;

    public static final long AMBIGUOUS_BINDING_TIMEOUT = 1000l;

    /**
     * KeyMap names
     */
    public static final String VICMD = "vicmd";
    public static final String VIINS = "viins";
    public static final String MAIN = "main";
    public static final String EMACS = "emacs";
    public static final String MENU_SELECT = "menuselect";

    /**
     * Possible states in which the current readline operation may be in.
     */
    protected enum State {
        /**
         * The user is just typing away
         */
        NORMAL,
        /**
         * readLine should exit and return the buffer content
         */
        DONE,
        /**
         * readLine should exit and throw an EOFException
         */
        EOF,
        /**
         * readLine should exit and throw an UserInterruptException
         */
        INTERRUPT
    }

    protected enum ViMoveMode {
        NORMAL,
        YANK_TO,
        DELETE_TO,
        CHANGE_TO
    }

    protected enum BellType {
        NONE,
        AUDIBLE,
        VISIBLE
    }


    //
    // Constructor variables
    //

    /** The console to use */
    protected final Console console;
    /** The application name */
    protected final String appName;
    /** The console keys mapping */
    protected final Map<String, KeyMap> keyMaps;

    //
    // Configuration
    //
    protected final Map<String, Object> variables;
    protected History history = new MemoryHistory();
    protected Completer completer = null;
    protected Highlighter highlighter = new DefaultHighlighter();
    protected Parser parser = new DefaultParser();

    //
    // State variables
    //

    protected final Map<Option, Boolean> options = new HashMap<>();

    protected final Buffer buf = new Buffer();

    protected final Size size = new Size();

    protected String prompt;
    protected String rightPrompt;

    protected Character mask;

    protected Buffer historyBuffer = null;
    protected CharSequence searchBuffer;
    protected StringBuffer searchTerm = null;
    protected int searchIndex = -1;


    // Reading buffers
    protected final BindingReader bindingReader;


    /**
     * Last character searched for with a vi character search
     */
    protected int  charSearchChar = 0;           // Character to search for
    protected int  charSearchLastInvokeChar = 0; // Most recent invocation key
    protected int  charSearchFirstInvokeChar = 0;// First character that invoked

    /**
     * The vi yank buffer
     */
    protected String yankBuffer = "";

    protected ViMoveMode viMoveMode = ViMoveMode.NORMAL;

    protected KillRing killRing = new KillRing();

    protected Deque<Buffer> undo = new ArrayDeque<>();
    protected boolean isUndo;

    protected String macro = "";

    /*
     * Current internal state of the line reader
     */
    protected State   state = State.DONE;
    protected boolean reading;

    protected Supplier<String> post;

    protected Map<String, Widget> builtinWidgets;
    protected Map<String, Widget> widgets;

    protected int count;
    protected int mult;
    protected int universal = 4;
    protected int repeatCount;
    protected boolean isArgDigit;

    protected ParsedLine parsedLine;

    protected boolean skipRedisplay;
    protected Display display;

    protected boolean overTyping = false;

    protected String keyMap = MAIN;


    public ConsoleReaderImpl(Console console) throws IOException {
        this(console, null, null);
    }

    public ConsoleReaderImpl(Console console, String appName) throws IOException {
        this(console, appName, null);
    }

    public ConsoleReaderImpl(Console console, String appName, Map<String, Object> variables) {
        checkNotNull(console);
        this.console = console;
        if (appName == null) {
            appName = "JLine";
        }
        this.appName = appName;
        if (variables != null) {
            this.variables = variables;
        } else {
            this.variables = new HashMap<>();
        }
        this.keyMaps = defaultKeyMaps();
        this.keyMap = EMACS;

        builtinWidgets = builtinWidgets();
        widgets = new HashMap<>(builtinWidgets);
        bindingReader = new BindingReader(console,
                new Reference(SELF_INSERT),
                getLong(AMBIGUOUS_BINDING, AMBIGUOUS_BINDING_TIMEOUT));
    }

    protected void setupSigCont() {
        Signals.register("CONT", () -> {
//                console.init();
            // TODO: enter raw mode
            redrawLine();
            redisplay();
        });
    }

    public Console getConsole() {
        return console;
    }

    public String getAppName() {
        return appName;
    }

    public Map<String, KeyMap> getKeyMaps() {
        return keyMaps;
    }

    public KeyMap getKeys() {
        return keyMaps.get(keyMap);
    }

    public Map<String, Widget> getWidgets() {
        return widgets;
    }

    public Map<String, Widget> getBuiltinWidgets() {
        return builtinWidgets;
    }

    public Buffer getBuffer() {
        return buf;
    }

    public void runMacro(String macro) {
        bindingReader.runMacro(macro);
    }

    /**
     * Set the completer.
     */
    public void setCompleter(Completer completer) {
        this.completer = completer;
    }

    /**
     * Returns the completer.
     */
    public Completer getCompleter() {
        return completer;
    }

    //
    // History
    //

    public void setHistory(final History history) {
        checkNotNull(history);
        this.history = history;
    }

    public History getHistory() {
        return history;
    }

    //
    // Highlighter
    //

    public void setHighlighter(Highlighter highlighter) {
        this.highlighter = highlighter;
    }

    public Highlighter getHighlighter() {
        return highlighter;
    }

    public Parser getParser() {
        return parser;
    }

    public void setParser(Parser parser) {
        this.parser = parser;
    }

    //
    // Line Reading
    //

    /**
     * Read the next line and return the contents of the buffer.
     */
    public String readLine() throws UserInterruptException, EndOfFileException {
        return readLine(null, null, null, null);
    }

    /**
     * Read the next line with the specified character mask. If null, then
     * characters will be echoed. If 0, then no characters will be echoed.
     */
    public String readLine(Character mask) throws UserInterruptException, EndOfFileException {
        return readLine(null, null, mask, null);
    }

    public String readLine(String prompt) throws UserInterruptException, EndOfFileException {
        return readLine(prompt, null, null, null);
    }

    /**
     * Read a line from the <i>in</i> {@link InputStream}, and return the line
     * (without any trailing newlines).
     *
     * @param prompt    The prompt to issue to the console, may be null.
     * @return          A line that is read from the console, or null if there was null input (e.g., <i>CTRL-D</i>
     *                  was pressed).
     */
    public String readLine(String prompt, Character mask) throws UserInterruptException, EndOfFileException {
        return readLine(prompt, null, mask, null);
    }

    /**
     * Read a line from the <i>in</i> {@link InputStream}, and return the line
     * (without any trailing newlines).
     *
     * @param prompt    The prompt to issue to the console, may be null.
     * @return          A line that is read from the console, or null if there was null input (e.g., <i>CTRL-D</i>
     *                  was pressed).
     */
    public String readLine(String prompt, Character mask, String buffer) throws UserInterruptException, EndOfFileException {
        return readLine(prompt, null, mask, buffer);
    }

    /**
     * Read a line from the <i>in</i> {@link InputStream}, and return the line
     * (without any trailing newlines).
     *
     * @param prompt    The prompt to issue to the console, may be null.
     * @return          A line that is read from the console, or null if there was null input (e.g., <i>CTRL-D</i>
     *                  was pressed).
     */
    public String readLine(String prompt, String rightPrompt, Character mask, String buffer) throws UserInterruptException, EndOfFileException {
        // prompt may be null
        // mask may be null
        // buffer may be null

        Thread readLineThread = Thread.currentThread();
        SignalHandler previousIntrHandler = null;
        SignalHandler previousWinchHandler = null;
        Attributes originalAttributes = null;
        try {
            reading = true;

            previousIntrHandler = console.handle(Signal.INT, signal -> readLineThread.interrupt());
            previousWinchHandler = console.handle(Signal.WINCH, this::handleSignal);
            originalAttributes = console.enterRawMode();

            this.mask = mask;

            /*
             * This is the accumulator for VI-mode repeat count. That is, while in
             * move mode, if you type 30x it will delete 30 characters. This is
             * where the "30" is accumulated until the command is struck.
             */
            repeatCount = 0;
            mult = 1;

            state = State.NORMAL;

            // Cache console size for the duration of the call to readLine()
            // It will eventually be updated with WINCH signals
            size.copy(console.getSize());
            if (size.getColumns() == 0 || size.getRows() == 0) {
                throw new IllegalStateException("Invalid terminal size: " + size);
            }

            display = new Display(console, false);
            display.setColumns(size.getColumns());
            display.setTabWidth(TAB_WIDTH);

            // Move into application mode
            console.puts(Capability.keypad_xmit);
            // Make sure we position the cursor on column 0
            print(Ansi.ansi().bg(Color.DEFAULT).fgBright(Color.BLACK).a("~").fg(Color.DEFAULT).toString());
            for (int i = 0; i < size.getColumns() - 1; i++) {
                print(" ");
            }
            console.puts(Capability.carriage_return);
            print(" ");
            console.puts(Capability.carriage_return);

            setPrompt(prompt);
            setRightPrompt(rightPrompt);
            buf.clear();
            if (buffer != null) {
                buf.write(buffer);
            }
            undo.clear();
            parsedLine = null;

            callWidget(CALLBACK_INIT);

            // Draw initial prompt
            redrawLine();
            redisplay();

            while (true) {

                Binding o = readBinding(getKeys());
                if (o == null) {
                    return null;
                }
                Log.trace("Binding: ", o);

                // If this is still false after handling the binding, then
                // we reset our repeatCount to 0.
                isArgDigit = false;
                // Every command that can be repeated a specified number
                // of times, needs to know how many times to repeat, so
                // we figure that out here.
                count = ((repeatCount == 0) ? 1 : repeatCount) * mult;
                // Reset undo/redo flag
                isUndo = false;

                // Get executable widget
                Buffer copy = buf.copy();
                Widget w = getWidget(o);
                if (w == null || !w.apply()) {
                    beep();
                }
                if (!isUndo && !copy.toString().equals(buf.toString())) {
                    undo.push(copy);
                }

                switch (state) {
                    case DONE:
                        return finishBuffer();
                    case EOF:
                        throw new EndOfFileException();
                    case INTERRUPT:
                        throw new UserInterruptException(buf.toString());
                }

                if (!isArgDigit) {
                    /*
                     * If the operation performed wasn't a vi argument
                     * digit, then clear out the current repeatCount;
                     */
                    repeatCount = 0;
                    mult = 1;
                }

                redisplay();
            }
        } catch (IOError e) {
            if (e.getCause() instanceof InterruptedIOException) {
                throw new UserInterruptException(buf.toString());
            } else {
                throw e;
            }
        }
        finally {
            cleanup();
            reading = false;
            if (originalAttributes != null) {
                console.setAttributes(originalAttributes);
            }
            if (previousIntrHandler != null) {
                console.handle(Signal.INT, previousIntrHandler);
            }
            if (previousWinchHandler != null) {
                console.handle(Signal.WINCH, previousWinchHandler);
            }
        }
    }

    public void callWidget(String name) {
        if (!reading) {
            throw new IllegalStateException();
        }
        try {
            Widget w;
            if (name.startsWith(".")) {
                w = builtinWidgets.get(name.substring(1));
            } else {
                w = widgets.get(name);
            }
            if (w != null) {
                w.apply();
            }
        } catch (Throwable t) {
            // TODO: log
        }
    }

    protected void handleSignal(Signal signal) {
        if (signal == Signal.WINCH) {
            size.copy(console.getSize());
            display.setColumns(size.getColumns());
            redisplay();
        }
    }

    @SuppressWarnings("unchecked")
    protected Widget getWidget(Binding binding) {
        Widget w = null;
        if (binding instanceof Widget) {
            w = (Widget) binding;
        } else if (binding instanceof Macro) {
            String macro = ((Macro) binding).getSequence();
            w = () -> {
                bindingReader.runMacro(macro);
                return true;
            };
        } else if (binding instanceof Reference) {
            String name = ((Reference) binding).name();
            w = widgets.get(name);
            if (w == null) {
                post = () -> "No such widget `" + name + "'";
            }
        }
        return w;
    }

    //
    // Helper methods
    //

    protected void setPrompt(final String prompt) {
        this.prompt = prompt != null ? prompt : "";
    }

    protected void setRightPrompt(final String rightPrompt) {
        this.rightPrompt = rightPrompt != null ? rightPrompt : "";
    }

    protected void setBuffer(Buffer buffer) {
        setBuffer(buffer.toString());
        buf.cursor(buffer.cursor());
    }

    /**
     * Set the current buffer's content to the specified {@link String}. The
     * visual console will be modified to show the current buffer.
     *
     * @param buffer the new contents of the buffer.
     */
    protected void setBuffer(final String buffer) {
        // don't bother modifying it if it is unchanged
        if (buffer.equals(buf.toString())) {
            return;
        }

        // obtain the difference between the current buffer and the new one
        int sameIndex = 0;

        for (int i = 0, l1 = buffer.length(), l2 = buf.length(); (i < l1)
            && (i < l2); i++) {
            if (buffer.charAt(i) == buf.atChar(i)) {
                sameIndex++;
            }
            else {
                break;
            }
        }

        int diff = buf.cursor() - sameIndex;
        if (diff < 0) { // we can't buf.backspace here so try from the end of the buffer
            endOfLine();
            diff = buf.length() - sameIndex;
        }

        buf.backspace(diff); // go back for the differences
        killLine(); // clear to the end of the line
        buf.write(buffer.substring(sameIndex), overTyping); // append the differences
    }

    /**
     * Clear the line and redraw it.
     */
    public void redrawLine() {
        display.reset();
    }

    /**
     * Clear the buffer and add its contents to the history.
     *
     * @return the former contents of the buffer.
     */
    protected String finishBuffer() {
        String str = buf.toString();
        String historyLine = str;

        if (!isSet(Option.DISABLE_EVENT_EXPANSION)) {
            StringBuilder sb = new StringBuilder();
            boolean escaped = false;
            for (int i = 0; i < str.length(); i++) {
                char ch = str.charAt(i);
                if (escaped) {
                    escaped = false;
                    sb.append(ch);
                } else if (ch == '\\') {
                    escaped = true;
                } else {
                    sb.append(ch);
                }
            }
            str = sb.toString();
        }

        // we only add it to the history if the buffer is not empty
        // and if mask is null, since having a mask typically means
        // the string was a password. We clear the mask after this call
        if (str.length() > 0) {
            if (mask == null && !getBoolean(DISABLE_HISTORY, false)) {
                history.add(historyLine);
            }
        }
        return str;
    }

    /**
     * Expand event designator such as !!, !#, !3, etc...
     * See http://www.gnu.org/software/bash/manual/html_node/Event-Designators.html
     */
    @SuppressWarnings("fallthrough")
    protected String expandEvents(String str) {
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (escaped) {
                escaped = false;
                sb.append(c);
                continue;
            }
            switch (c) {
                case '\\':
                    // any '\!' should be considered an expansion escape, so skip expansion and strip the escape character
                    // a leading '\^' should be considered an expansion escape, so skip expansion and strip the escape character
                    // otherwise, add the escape
                    escaped = true;
                    sb.append(c);
                    break;
                case '!':
                    if (i + 1 < str.length()) {
                        c = str.charAt(++i);
                        boolean neg = false;
                        String rep = null;
                        int i1, idx;
                        switch (c) {
                            case '!':
                                if (history.size() == 0) {
                                    throw new IllegalArgumentException("!!: event not found");
                                }
                                rep = history.get(history.index() - 1);
                                break;
                            case '#':
                                sb.append(sb.toString());
                                break;
                            case '?':
                                i1 = str.indexOf('?', i + 1);
                                if (i1 < 0) {
                                    i1 = str.length();
                                }
                                String sc = str.substring(i + 1, i1);
                                i = i1;
                                idx = searchBackwards(sc);
                                if (idx < 0) {
                                    throw new IllegalArgumentException("!?" + sc + ": event not found");
                                } else {
                                    rep = history.get(idx);
                                }
                                break;
                            case '$':
                                if (history.size() == 0) {
                                    throw new IllegalArgumentException("!$: event not found");
                                }
                                String previous = history.get(history.index() - 1).trim();
                                int lastSpace = previous.lastIndexOf(' ');
                                if(lastSpace != -1) {
                                    rep = previous.substring(lastSpace+1);
                                } else {
                                    rep = previous;
                                }
                                break;
                            case ' ':
                            case '\t':
                                sb.append('!');
                                sb.append(c);
                                break;
                            case '-':
                                neg = true;
                                i++;
                                // fall through
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                i1 = i;
                                for (; i < str.length(); i++) {
                                    c = str.charAt(i);
                                    if (c < '0' || c > '9') {
                                        break;
                                    }
                                }
                                try {
                                    idx = Integer.parseInt(str.substring(i1, i));
                                } catch (NumberFormatException e) {
                                    throw new IllegalArgumentException((neg ? "!-" : "!") + str.substring(i1, i) + ": event not found");
                                }
                                if (neg && idx > 0 && idx <= history.size()) {
                                    rep = history.get(history.index() - idx);
                                } else if (!neg && idx > history.index() - history.size() && idx <= history.index()) {
                                    rep = history.get(idx - 1);
                                } else {
                                    throw new IllegalArgumentException((neg ? "!-" : "!") + str.substring(i1, i) + ": event not found");
                                }
                                break;
                            default:
                                String ss = str.substring(i);
                                i = str.length();
                                idx = searchBackwards(ss, history.index(), true);
                                if (idx < 0) {
                                    throw new IllegalArgumentException("!" + ss + ": event not found");
                                } else {
                                    rep = history.get(idx);
                                }
                                break;
                        }
                        if (rep != null) {
                            sb.append(rep);
                        }
                    } else {
                        sb.append(c);
                    }
                    break;
                case '^':
                    if (i == 0) {
                        int i1 = str.indexOf('^', i + 1);
                        int i2 = str.indexOf('^', i1 + 1);
                        if (i2 < 0) {
                            i2 = str.length();
                        }
                        if (i1 > 0 && i2 > 0) {
                            String s1 = str.substring(i + 1, i1);
                            String s2 = str.substring(i1 + 1, i2);
                            String s = history.get(history.index() - 1).replace(s1, s2);
                            sb.append(s);
                            i = i2 + 1;
                            break;
                        }
                    }
                    sb.append(c);
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * Write out the specified string to the buffer and the output stream.
     */
    public void putString(final CharSequence str) {
        buf.write(str, overTyping);
    }

    /**
     * Flush the console output stream. This is important for printout out single characters (like a buf.backspace or
     * keyboard) that we want the console to handle immediately.
     */
    public void flush() {
        console.writer().flush();
    }

    /**
     * This method is calling while doing a delete-to ("d"), change-to ("c"),
     * or yank-to ("y") and it filters out only those movement operations
     * that are allowable during those operations. Any operation that isn't
     * allow drops you back into movement mode.
     *
     * @param op The incoming operation to remap
     * @return The remaped operation
     */
    protected String viDeleteChangeYankToRemap (String op) {
        switch (op) {
            case VI_EOF_MAYBE:
            case SEND_BREAK:
            case BACKWARD_CHAR:
            case FORWARD_CHAR:
            case END_OF_LINE:
            case VI_MATCH_BRACKET:
            case VI_DIGIT_OR_BEGINNING_OF_LINE:
            case NEG_ARGUMENT:
            case DIGIT_ARGUMENT:
            case VI_PREV_WORD:
            case VI_END_WORD:
            case VI_CHAR_SEARCH:
            case VI_NEXT_WORD:
            case VI_FIRST_PRINT:
            case VI_GOTO_MARK:
            case VI_COLUMN:
            case VI_DELETE_TO:
            case VI_YANK_TO:
            case VI_CHANGE_TO:
                return op;

            default:
                return VI_CMD_MODE;
        }
    }

    /**
     * Searches forward of the current position for a character and moves
     * the cursor onto it.
     * @param count Number of times to repeat the process.
     * @param ch The character to search for
     * @return true if the char was found, false otherwise
     */
    protected boolean doViCharSearch(int count, int invokeChar, int ch) {
        if (ch < 0 || invokeChar < 0) {
            return false;
        }

        int     searchChar = ch;
        boolean isForward;
        boolean stopBefore;

        /*
         * The character stuff turns out to be hairy. Here is how it works:
         *   f - search forward for ch
         *   F - search backward for ch
         *   t - search forward for ch, but stop just before the match
         *   T - search backward for ch, but stop just after the match
         *   ; - After [fFtT;], repeat the last search, after ',' reverse it
         *   , - After [fFtT;], reverse the last search, after ',' repeat it
         */
        if (invokeChar == ';' || invokeChar == ',') {
            // No recent search done? Then bail
            if (charSearchChar == 0) {
                return false;
            }

            // Reverse direction if switching between ',' and ';'
            if (charSearchLastInvokeChar == ';' || charSearchLastInvokeChar == ',') {
                if (charSearchLastInvokeChar != invokeChar) {
                    charSearchFirstInvokeChar = switchCase(charSearchFirstInvokeChar);
                }
            }
            else {
                if (invokeChar == ',') {
                    charSearchFirstInvokeChar = switchCase(charSearchFirstInvokeChar);
                }
            }

            searchChar = charSearchChar;
        }
        else {
            charSearchChar            = searchChar;
            charSearchFirstInvokeChar = invokeChar;
        }

        charSearchLastInvokeChar = invokeChar;

        isForward = Character.isLowerCase(charSearchFirstInvokeChar);
        stopBefore = (Character.toLowerCase(charSearchFirstInvokeChar) == 't');

        boolean ok = false;

        if (isForward) {
            while (count-- > 0) {
                int pos = buf.cursor() + 1;
                while (pos < buf.length()) {
                    if (buf.atChar(pos) == searchChar) {
                        buf.cursor(pos);
                        ok = true;
                        break;
                    }
                    ++pos;
                }
            }

            if (ok) {
                if (stopBefore)
                    buf.move(-1);

                /*
                 * When in yank-to, move-to, del-to state we actually want to
                 * go to the character after the one we landed on to make sure
                 * that the character we ended up on is included in the
                 * operation
                 */
                if (isInViMoveOperationState()) {
                    buf.move(1);
                }
            }
        }
        else {
            while (count-- > 0) {
                int pos = buf.cursor() - 1;
                while (pos >= 0) {
                    if (buf.atChar(pos) == searchChar) {
                        buf.cursor(pos);
                        ok = true;
                        break;
                    }
                    --pos;
                }
            }

            if (ok && stopBefore)
                buf.move(1);
        }

        return ok;
    }

    protected int switchCase(int ch) {
        if (Character.isUpperCase(ch)) {
            return Character.toLowerCase(ch);
        } else if (Character.isLowerCase(ch)) {
            return Character.toUpperCase(ch);
        } else {
            return ch;
        }
    }

    /**
     * @return true if line reader is in the middle of doing a change-to
     *   delete-to or yank-to.
     */
    protected boolean isInViMoveOperationState() {
        return viMoveMode != ViMoveMode.NORMAL;
    }

    protected boolean viNextWord() {
        return doViNextWord(count);
    }

    /**
     * This is a close facsimile of the actual vi next word logic.
     * As with viPreviousWord() this probably needs to be improved
     * at some point.
     *
     * @param count number of iterations
     * @return true if the move was successful, false otherwise
     */
    protected boolean doViNextWord(int count) {
        int pos = buf.cursor();
        int end = buf.length();

        if (pos == end) {
            return false;
        }

        for (int i = 0; pos < end && i < count; i++) {
            // Skip over letter/digits
            while (pos < end && !isDelimiter(buf.atChar(pos))) {
                ++pos;
            }

            /*
             * Don't you love special cases? During delete-to and yank-to
             * operations the word movement is normal. However, during a
             * change-to, the trailing spaces behind the last word are
             * left in tact.
             */
            if (i < (count-1) || !(viMoveMode == ViMoveMode.CHANGE_TO)) {
                while (pos < end && isDelimiter(buf.atChar(pos))) {
                    ++pos;
                }
            }
        }

        buf.cursor(pos);
        return true;
    }

    /**
     * Implements a close facsimile of the vi end-of-word movement.
     * If the character is on white space, it takes you to the end
     * of the next word.  If it is on the last character of a word
     * it takes you to the next of the next word.  Any other character
     * of a word, takes you to the end of the current word.
     */
    protected boolean viEndWord() {
        int pos = buf.cursor();
        int end = buf.length();

        // TODO: refactor to use buf.current() / buf.moveBufferCursor
        for (int i = 0; pos < end && i < count; i++) {
            if (pos < (end-1)
                    && !isDelimiter(buf.atChar(pos))
                    && isDelimiter(buf.atChar(pos+1))) {
                ++pos;
            }

            // If we are on white space, then move back.
            while (pos < end && isDelimiter(buf.atChar(pos))) {
                ++pos;
            }

            while (pos < (end-1) && !isDelimiter(buf.atChar(pos+1))) {
                ++pos;
            }
        }
        buf.cursor(pos);
        return true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    protected boolean backwardWord() {
        if (count < 0) {
            count = - count;
            return forwardWord();
        }
        for (int count = this.count; count > 0; --count) {
            while (isDelimiter(buf.prevChar()) && (buf.move(-1) != 0)) ;
            while (!isDelimiter(buf.prevChar()) && (buf.move(-1) != 0)) ;
        }
        return true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    protected boolean forwardWord() {
        if (count < 0) {
            count = - count;
            return backwardWord();
        }
        for (int count = this.count; count > 0; --count) {
            while (isDelimiter(buf.currChar()) && (buf.move(1) != 0)) ;
            while (!isDelimiter(buf.currChar()) && (buf.move(1) != 0)) ;
        }
        return true;
    }

    protected boolean insertComment() {
        return doInsertComment(false);
    }

    protected boolean viInsertComment() {
        return doInsertComment(true);
    }

    protected boolean doInsertComment(boolean isViMode) {
        String comment = getString(COMMENT_BEGIN, "#");
        beginningOfLine();
        putString(comment);
        if (isViMode) {
            setKeyMap(VIINS);
        }
        return acceptLine();
    }

    /**
     * Implements vi search ("/" or "?").
     */
    @SuppressWarnings("fallthrough")
    protected boolean viSearch() {
        int searchChar = getLastBinding().codePointAt(0);
        boolean isForward = (searchChar == '/');

        /*
         * This is a little gross, I'm sure there is a more appropriate way
         * of saving and restoring state.
         */
        Buffer origBuffer = buf.copy();

        // Clear the contents of the current line and
        killWholeLine();

        // Our new "prompt" is the character that got us into search mode.
        putString(new String(Character.toChars(searchChar)));
        flush();

        boolean isAborted = false;
        boolean isComplete = false;

        /*
         * Readline doesn't seem to do any special character map handling
         * here, so I think we are safe.
         */
        int ch = -1;
        while (!isAborted && !isComplete && (ch = readCharacter()) != -1) {
            switch (ch) {
                case '\033':  // ESC
                    /*
                     * The ESC behavior doesn't appear to be readline behavior,
                     * but it is a little tweak of my own. I like it.
                     */
                    isAborted = true;
                    break;
                case '\010':  // backspace
                case '\177':  // Delete
                    buf.backspace();
                    /*
                     * Backspacing through the "prompt" aborts the search.
                     */
                    if (buf.cursor() == 0) {
                        isAborted = true;
                    }
                    break;
                case '\012': // NL
                case '\015': // CR
                    isComplete = true;
                    break;
                default:
                    putString(Character.toString((char) ch));
            }

            flush();
        }

        // If we aborted, then put ourself at the end of the original buffer.
        if (ch == -1 || isAborted) {
            killWholeLine();
            setBuffer(origBuffer);
            return true;
        }

        /*
         * The first character of the buffer was the search character itself
         * so we discard it.
         */
        String searchTerm = buf.substring(1);
        int idx = -1;

        /*
         * The semantics of the history thing is gross when you want to
         * explicitly iterate over entries (without an iterator) as size()
         * returns the actual number of entries in the list but get()
         * doesn't work the way you think.
         */
        int end   = history.index();
        int start = (end <= history.size()) ? 0 : end - history.size();

        if (isForward) {
            for (int i = start; i < end; i++) {
                if (history.get(i).contains(searchTerm)) {
                    idx = i;
                    break;
                }
            }
        }
        else {
            for (int i = end-1; i >= start; i--) {
                if (history.get(i).contains(searchTerm)) {
                    idx = i;
                    break;
                }
            }
        }

        /*
         * No match? Then restore what we were working on, but make sure
         * the cursor is at the beginning of the line.
         */
        if (idx == -1) {
            killWholeLine();
            putString(origBuffer.toString());
            beginningOfLine();
            return true;
        }

        /*
         * Show the match.
         */
        killWholeLine();
        putString(history.get(idx));
        beginningOfLine();
        flush();

        /*
         * While searching really only the "n" and "N" keys are interpreted
         * as movement, any other key is treated as if you are editing the
         * line with it, so we return it back up to the caller for interpretation.
         */
        isComplete = false;
        while (!isComplete && (ch = readCharacter()) != -1) {
            boolean forward = isForward;
            switch (ch) {
                case 'p': case 'P':
                    forward = !isForward;
                    // Fallthru
                case 'n': case 'N':
                    boolean isMatch = false;
                    if (forward) {
                        for (int i = idx+1; !isMatch && i < end; i++) {
                            if (history.get(i).contains(searchTerm)) {
                                idx = i;
                                isMatch = true;
                            }
                        }
                    }
                    else {
                        for (int i = idx - 1; !isMatch && i >= start; i--) {
                            if (history.get(i).contains(searchTerm)) {
                                idx = i;
                                isMatch = true;
                            }
                        }
                    }
                    if (isMatch) {
                        killWholeLine();
                        putString(history.get(idx));
                        beginningOfLine();
                    }
                    break;
                default:
                    isComplete = true;
            }
            flush();
        }

        /*
         * Complete?
         */
        bindingReader.runMacro(new String(Character.toChars(ch)));

        return true;
    }

    protected boolean insertCloseCurly() {
        return insertClose("}");
    }

    protected boolean insertCloseParen() {
        return insertClose(")");
    }

    protected boolean insertCloseSquare() {
        return insertClose("]");
    }

    protected boolean insertClose(String s) {
        putString(s);

        int closePosition = buf.cursor();

        buf.move(-1);
        doViMatchBracket();
        redisplay();

        peekCharacter(BLINK_MATCHING_PAREN_TIMEOUT);

        buf.cursor(closePosition);
        return true;
    }

    protected boolean viMatchBracket() {
        return doViMatchBracket();
    }

    protected boolean undefinedKey() {
        return false;
    }

    /**
     * Implements vi style bracket matching ("%" command). The matching
     * bracket for the current bracket type that you are sitting on is matched.
     * The logic works like so:
     * @return true if it worked, false if the cursor was not on a bracket
     *   character or if there was no matching bracket.
     */
    protected boolean doViMatchBracket() {
        int pos        = buf.cursor();

        if (pos == buf.length()) {
            return false;
        }

        int type       = getBracketType(buf.atChar(pos));
        int move       = (type < 0) ? -1 : 1;
        int count      = 1;

        if (type == 0)
            return false;

        while (count > 0) {
            pos += move;

            // Fell off the start or end.
            if (pos < 0 || pos >= buf.length()) {
                return false;
            }

            int curType = getBracketType(buf.atChar(pos));
            if (curType == type) {
                ++count;
            }
            else if (curType == -type) {
                --count;
            }
        }

        /*
         * Slight adjustment for delete-to, yank-to, change-to to ensure
         * that the matching paren is consumed
         */
        if (move > 0 && isInViMoveOperationState())
            ++pos;

        buf.cursor(pos);
        return true;
    }

    /**
     * Given a character determines what type of bracket it is (paren,
     * square, curly, or none).
     * @param ch The character to check
     * @return 1 is square, 2 curly, 3 parent, or zero for none.  The value
     *   will be negated if it is the closing form of the bracket.
     */
    protected int getBracketType (int ch) {
        switch (ch) {
            case '[': return  1;
            case ']': return -1;
            case '{': return  2;
            case '}': return -2;
            case '(': return  3;
            case ')': return -3;
            default:
                return 0;
        }
    }

    protected boolean backwardKillWord() {
        if (count < 0) {
            count = - count;
            killWord();
        }
        StringBuilder killed = new StringBuilder();
        int c;
        for (int count = this.count; count > 0; --count) {
            while (isDelimiter((c = buf.prevChar()))) {
                if (c == 0) {
                    break;
                }
                killed.appendCodePoint(c);
                buf.backspace();
            }
            while (!isDelimiter((c = buf.prevChar()))) {
                if (c == 0) {
                    break;
                }
                killed.appendCodePoint(c);
                buf.backspace();
            }
        }
        String copy = killed.reverse().toString();
        killRing.addBackwards(copy);
        return true;
    }

    protected boolean killWord() {
        if (count < 0) {
            count = - count;
            backwardKillWord();
        }
        StringBuilder killed = new StringBuilder();
        int c;
        for (int count = this.count; count > 0; --count) {
            while (isDelimiter((c = buf.currChar()))) {
                if (c == 0) {
                    break;
                }
                killed.appendCodePoint(c);
                delete();
            }
            while (!isDelimiter((c = buf.currChar()))) {
                if (c == 0) {
                    break;
                }
                killed.appendCodePoint(c);
                delete();
            }
        }
        String copy = killed.toString();
        killRing.add(copy);
        return true;
    }

    protected boolean capitalizeWord() {
        int c;
        for (int count = this.count; count > 0; --count) {
            while (isDelimiter(buf.currChar()) && (buf.move(1) != 0)) ;
            boolean first = true;
            while (buf.cursor() < buf.length() && !isDelimiter(c = buf.currChar())) {
                buf.currChar(first ? Character.toUpperCase(c) : Character.toLowerCase(c));
                buf.move(1);
                first = false;
            }
        }
        return true;
    }

    protected boolean upCaseWord() {
        int c;
        for (int count = this.count; count > 0; --count) {
            while (isDelimiter(buf.currChar()) && (buf.move(1) != 0)) ;
            while (buf.cursor() < buf.length() && !isDelimiter(c = buf.currChar())) {
                buf.currChar(Character.toUpperCase(c));
                buf.move(1);
            }
        }
        return true;
    }

    protected boolean downCaseWord() {
        int c;
        for (int count = this.count; count > 0; --count) {
            while (isDelimiter(buf.currChar()) && (buf.move(1) != 0)) ;
            while (buf.cursor() < buf.length() && !isDelimiter(c = buf.currChar())) {
                buf.currChar(Character.toLowerCase(c));
                buf.move(1);
            }
        }
        return true;
    }

    /**
     * Performs character transpose. The character prior to the cursor and the
     * character under the cursor are swapped and the cursor is advanced one.
     * Do not cross line breaks.
     */
    protected boolean transposeChars() {
        int lstart = buf.cursor() - 1;
        int lend = buf.cursor();
        while (buf.atChar(lstart) != 0 && buf.atChar(lstart) != '\n') {
            lstart--;
        }
        lstart++;
        while (buf.atChar(lend) != 0 && buf.atChar(lend) != '\n') {
            lend++;
        }
        if (lend - lstart < 2) {
            return false;
        }
        boolean neg = this.count < 0;
        for (int count = Math.max(this.count, -this.count); count > 0; --count) {
            while (buf.cursor() <= lstart) {
                buf.move(1);
            }
            while (buf.cursor() >= lend) {
                buf.move(-1);
            }
            int c = buf.currChar();
            buf.currChar(buf.prevChar());
            buf.move(-1);
            buf.currChar(c);
            buf.move(neg ? 0 : 2);
        }
        return true;
    }

    protected boolean transposeWords() {
        int lstart = buf.cursor() - 1;
        int lend = buf.cursor();
        while (buf.atChar(lstart) != 0 && buf.atChar(lstart) != '\n') {
            lstart--;
        }
        lstart++;
        while (buf.atChar(lend) != 0 && buf.atChar(lend) != '\n') {
            lend++;
        }
        if (lend - lstart < 2) {
            return false;
        }
        int words = 0;
        boolean inWord = false;
        if (!isDelimiter(buf.atChar(lstart))) {
            words++;
            inWord = true;
        }
        for (int i = lstart; i < lend; i++) {
            if (isDelimiter(buf.atChar(i))) {
                inWord = false;
            } else {
                if (!inWord) {
                    words++;
                }
                inWord = true;
            }
        }
        if (words < 2) {
            return false;
        }
        boolean neg = this.count < 0;
        for (int count = Math.max(this.count, -this.count); count > 0; --count) {
            int sta1, end1, sta2, end2;
            // Compute current word boundaries
            sta1 = buf.cursor();
            while (sta1 > lstart && !isDelimiter(buf.atChar(sta1 - 1))) {
                sta1--;
            }
            end1 = sta1;
            while (end1 < lend && !isDelimiter(buf.atChar(++end1)));
            if (neg) {
                end2 = sta1 - 1;
                while (end2 > lstart && isDelimiter(buf.atChar(end2 - 1))) {
                    end2--;
                }
                if (end2 < lstart) {
                    // No word before, use the word after
                    sta2 = end1;
                    while (isDelimiter(buf.atChar(++sta2)));
                    end2 = sta2;
                    while (end2 < lend && !isDelimiter(buf.atChar(++end2)));
                } else {
                    sta2 = end2;
                    while (sta2 > lstart && !isDelimiter(buf.atChar(sta2 - 1))) {
                        sta2--;
                    }
                }
            } else {
                sta2 = end1;
                while (sta2 < lend && isDelimiter(buf.atChar(++sta2)));
                if (sta2 == lend) {
                    // No word after, use the word before
                    end2 = sta1;
                    while (isDelimiter(buf.atChar(end2 - 1))) {
                        end2--;
                    }
                    sta2 = end2;
                    while (sta2 > lstart && !isDelimiter(buf.atChar(sta2 - 1))) {
                        sta2--;
                    }
                } else {
                    end2 = sta2;
                    while (end2 < lend && !isDelimiter(buf.atChar(++end2))) ;
                }
            }
            if (sta1 < sta2) {
                String res = buf.substring(0, sta1) + buf.substring(sta2, end2)
                        + buf.substring(end1, sta2) + buf.substring(sta1, end1)
                        + buf.substring(end2);
                buf.clear();
                buf.write(res);
                buf.cursor(neg ? end1 : end2);
            } else {
                String res = buf.substring(0, sta2) + buf.substring(sta1, end1)
                        + buf.substring(end2, sta1) + buf.substring(sta2, end2)
                        + buf.substring(end1);
                buf.clear();
                buf.write(res);
                buf.cursor(neg ? end2 : end1);
            }
//            int sta1 = buf.cursor();
//            while (!isDelimiter(buf.currChar()) && buf.move(1) != 0);
//            int end1 = buf.cursor();
//            while (isDelimiter(buf.currChar()) && buf.move(1) != 0);
//            int sta2 = buf.cursor();
//            while (!isDelimiter(buf.currChar()) && buf.move(1) != 0);
//            int end2 = buf.cursor();
//            String res = buf.substring(0, sta1) + buf.substring(sta2, end2)
//                    + buf.substring(end1, sta2) + buf.substring(sta1, end1)
//                    + buf.substring(end2);
//            buf.clear();
//            buf.write(res);
//            buf.cursor(end2);
        }
        return true;
    }

    public boolean isKeyMap(String name) {
        return keyMap.equals(name);
    }

    protected boolean undo() {
        isUndo = true;
        if (undo.size() > 0) {
            Buffer b = undo.pop();
            buf.setBuffer(b);
            return true;
        }
        return false;
    }

    protected boolean sendBreak() {
        if (searchTerm == null) {
            buf.clear();
            println();
            redrawLine();
            return false;
        }
        return true;
    }

    protected boolean backwardChar() {
        return buf.move(-count) != 0;
    }

    protected boolean forwardChar() {
        return buf.move(count) != 0;
    }

    /**
     * Read a character from the console.
     *
     * @return the character, or -1 if an EOF is received.
     */
    public int readCharacter() {
        return bindingReader.readCharacter();
    }

    public int peekCharacter(long timeout) {
        return bindingReader.peekCharacter(timeout);
    }

    /**
     * Read from the input stream and decode an operation from the key map.
     *
     * The input stream will be read character by character until a matching
     * binding can be found.  Characters that can't possibly be matched to
     * any binding will be discarded.
     *
     * @param keys the KeyMap to use for decoding the input stream
     * @return the decoded binding or <code>null</code> if the end of
     *         stream has been reached
     */
    public Binding readBinding(KeyMap keys) {
        return readBinding(keys, null);
    }

    public Binding readBinding(KeyMap keys, KeyMap local) {
        Binding o = bindingReader.readBinding(keys, local);
        /*
         * The kill ring keeps record of whether or not the
         * previous command was a yank or a kill. We reset
         * that state here if needed.
         */
        if (o instanceof Reference) {
            String ref = ((Reference) o).name();
            if (!YANK_POP.equals(ref) && !YANK.equals(ref)) {
                killRing.resetLastYank();
            }
            if (!KILL_LINE.equals(ref) && !KILL_WHOLE_LINE.equals(ref)
                    && !BACKWARD_KILL_WORD.equals(ref) && !KILL_WORD.equals(ref)) {
                killRing.resetLastKill();
            }
        }
        return o;
    }

    public ParsedLine getParsedLine() {
        return parsedLine;
    }

    public String getLastBinding() {
        return bindingReader.getLastBinding();
    }

    public String getSearchTerm() {
        return searchTerm != null ? searchTerm.toString() : null;
    }

    //
    // Key Bindings
    //

    /**
     * Sets the current keymap by name. Supported keymaps are "emacs",
     * "viins", "vicmd".
     * @param name The name of the keymap to switch to
     * @return true if the keymap was set, or false if the keymap is
     *    not recognized.
     */
    public boolean setKeyMap(String name) {
        KeyMap map = keyMaps.get(name);
        if (map == null) {
            return false;
        }
        this.keyMap = name;
        return true;
    }

    /**
     * Returns the name of the current key mapping.
     * @return the name of the key mapping. This will be the canonical name
     *   of the current mode of the key map and may not reflect the name that
     *   was used with {@link #setKeyMap(String)}.
     */
    public String getKeyMap() {
        return keyMap;
    }

    protected boolean viDigitOrBeginningOfLine() {
        if (repeatCount > 0) {
            return digitArgument();
        } else {
            return beginningOfLine();
        }
    }

    protected boolean universalArgument() {
        mult *= universal;
        return true;
    }

    protected boolean argumentBase() {
        if (repeatCount > 0 && repeatCount < 32) {
            universal = repeatCount;
            return true;
        } else {
            return false;
        }
    }

    protected boolean negArgument() {
        mult *= -1;
        isArgDigit = true;
        return true;
    }

    protected boolean digitArgument() {
        String s = getLastBinding();
        repeatCount = (repeatCount * 10) + s.charAt(s.length() - 1) - '0';
        isArgDigit = true;
        return true;
    }

    protected boolean viDeleteTo() {
        int cursorStart = buf.cursor();
        Binding o = readBinding(getKeys());
        if (o instanceof Reference) {
            String op = viDeleteChangeYankToRemap(((Reference) o).name());
            // This is a weird special case. In vi
            // "dd" deletes the current line. So if we
            // get a delete-to, followed by a delete-to,
            // we delete the line.
            if (VI_DELETE_TO.equals(op)) {
                killWholeLine();
            } else {
                viMoveMode = ViMoveMode.DELETE_TO;
                Widget widget = widgets.get(op);
                if (widget != null && !widget.apply()) {
                    return false;
                }
                viMoveMode = ViMoveMode.NORMAL;
            }
            return viDeleteTo(cursorStart, buf.cursor());
        } else {
            pushBackBinding();
            return false;
        }
    }

    protected boolean viYankTo() {
        int cursorStart = buf.cursor();
        Binding o = readBinding(getKeys());
        if (o instanceof Reference) {
            String op = viDeleteChangeYankToRemap(((Reference) o).name());
            // Similar to delete-to, a "yy" yanks the whole line.
            if (VI_YANK_TO.equals(op)) {
                yankBuffer = buf.toString();
                return true;
            } else {
                viMoveMode = ViMoveMode.YANK_TO;
                Widget widget = widgets.get(op);
                if (widget != null && !widget.apply()) {
                    return false;
                }
                viMoveMode = ViMoveMode.NORMAL;
            }
            return viYankTo(cursorStart, buf.cursor());
        } else {
            pushBackBinding();
            return false;
        }
    }

    protected boolean viChangeTo() {
        int cursorStart = buf.cursor();
        Binding o = readBinding(getKeys());
        if (o instanceof Reference) {
            String op = viDeleteChangeYankToRemap(((Reference) o).name());
            // change whole line
            if (VI_CHANGE_TO.equals(op)) {
                killWholeLine();
            } else {
                viMoveMode = ViMoveMode.CHANGE_TO;
                Widget widget = widgets.get(op);
                if (widget != null && !widget.apply()) {
                    return false;
                }
                viMoveMode = ViMoveMode.NORMAL;
            }
            boolean res = viChangeTo(cursorStart, buf.cursor());
            setKeyMap(VIINS);
            return res;
        } else {
            pushBackBinding();
            return false;
        }
    }

    protected void cleanup() {
        endOfLine();
        post = null;
        if (size.getColumns() > 0 || size.getRows() > 0) {
            redisplay(false);
            println();
            console.puts(Capability.keypad_local);
            flush();
        }
        history.moveToEnd();
    }

    protected boolean viEofMaybe() {
        /*
         * Handler for CTRL-D. Attempts to follow readline
         * behavior. If the line is empty, then it is an EOF
         * otherwise it is as if the user hit enter.
         */
        if (buf.length() == 0) {
            state = State.EOF;
            return true;
        } else {
            return acceptLine();
        }
    }

    protected boolean historyIncrementalSearchForward() {
        return doSearchHistory(false);
    }

    protected boolean historyIncrementalSearchBackward() {
        return doSearchHistory(true);
    }

    protected boolean doSearchHistory(boolean backward) {
        Buffer originalBuffer = buf.copy();
        String previousSearchTerm = (searchTerm != null) ? searchTerm.toString() : "";
        searchTerm = new StringBuffer(buf.toString());
        if (searchTerm.length() > 0) {
            searchIndex = backward
                    ? searchBackwards(searchTerm.toString(), history.index(), false)
                    : searchForwards(searchTerm.toString(), history.index(), false);
            if (searchIndex == -1) {
                beep();
            }
            printSearchStatus(searchTerm.toString(),
                    searchIndex > -1 ? history.get(searchIndex) : "", backward);
        } else {
            searchIndex = -1;
            printSearchStatus("", "", backward);
        }

        redisplay();

        KeyMap terminators = new KeyMap();
        getString("search-terminators", "\033\012")
                .codePoints().forEach(c -> bind(terminators, ACCEPT_LINE, new String(Character.toChars(c))));

        try {
            while (true) {
                Binding o = readBinding(getKeys(), terminators);
                if (new Reference(SEND_BREAK).equals(o)) {
                    buf.setBuffer(originalBuffer);
                    return true;
                } else if (new Reference(HISTORY_INCREMENTAL_SEARCH_BACKWARD).equals(o)) {
                    backward = true;
                    if (searchTerm.length() == 0) {
                        searchTerm.append(previousSearchTerm);
                    }
                    if (searchIndex > 0) {
                        searchIndex = searchBackwards(searchTerm.toString(), searchIndex, false);
                    }
                } else if (new Reference(HISTORY_INCREMENTAL_SEARCH_FORWARD).equals(o)) {
                    backward = false;
                    if (searchTerm.length() == 0) {
                        searchTerm.append(previousSearchTerm);
                    }
                    if (searchIndex > -1 && searchIndex < history.size() - 1) {
                        searchIndex = searchForwards(searchTerm.toString(), searchIndex, false);
                    }
                } else if (new Reference(BACKWARD_DELETE_CHAR).equals(o)) {
                    if (searchTerm.length() > 0) {
                        searchTerm.deleteCharAt(searchTerm.length() - 1);
                        if (backward) {
                            searchIndex = searchBackwards(searchTerm.toString(), history.index(), false);
                        } else {
                            searchIndex = searchForwards(searchTerm.toString(), history.index(), false);
                        }
                    }
                } else if (new Reference(SELF_INSERT).equals(o)) {
                    searchTerm.append(getLastBinding());
                    if (backward) {
                        searchIndex = searchBackwards(searchTerm.toString(), history.index(), false);
                    } else {
                        searchIndex = searchForwards(searchTerm.toString(), history.index(), false);
                    }
                } else {
                    // Set buffer and cursor position to the found string.
                    if (searchIndex != -1) {
                        history.moveTo(searchIndex);
                    }
                    pushBackBinding();
                    return true;
                }

                // print the search status
                if (searchTerm.length() == 0) {
                    printSearchStatus("", "", backward);
                    searchIndex = -1;
                } else {
                    if (searchIndex == -1) {
                        beep();
                        printSearchStatus(searchTerm.toString(), "", backward);
                    } else {
                        printSearchStatus(searchTerm.toString(), history.get(searchIndex), backward);
                    }
                }
                redisplay();
            }
        } finally {
            searchTerm = null;
            searchIndex = -1;
            post = null;
        }
    }

    private void pushBackBinding() {
        pushBackBinding(false);
    }

    private void pushBackBinding(boolean skip) {
        String s = getLastBinding();
        if (s != null) {
            bindingReader.runMacro(s);
            skipRedisplay = skip;
        }
    }

    protected boolean historySearchForward() {
        if (historyBuffer == null || !buf.toString().equals(history.current())) {
            historyBuffer = buf.copy();
            searchBuffer = getFirstWord();
        }
        int index = history.index() + 1;

        if (index < history.size()) {
            int searchIndex = searchForwards(searchBuffer.toString(), index, true);
            if (searchIndex == -1) {
                history.moveToEnd();
                if (!buf.toString().equals(historyBuffer.toString())) {
                    setBuffer(historyBuffer.toString());
                    historyBuffer = null;
                } else {
                    return false;
                }
            } else {
                // Maintain cursor position while searching.
                if (history.moveTo(searchIndex)) {
                    setBuffer(history.current());
                } else {
                    history.moveToEnd();
                    setBuffer(historyBuffer.toString());
                    return false;
                }
            }
        } else {
            history.moveToEnd();
            if (!buf.toString().equals(historyBuffer.toString())) {
                setBuffer(historyBuffer.toString());
                historyBuffer = null;
            } else {
                return false;
            }
        }
        return true;
    }

    private CharSequence getFirstWord() {
        String s = buf.toString();
        int i = 0;
        while (i < s.length() && !Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return s.substring(0, i);
    }

    protected boolean historySearchBackward() {
        if (historyBuffer == null || !buf.toString().equals(history.current())) {
            historyBuffer = buf.copy();
            searchBuffer = getFirstWord();
        }
        int searchIndex = searchBackwards(searchBuffer.toString(), history.index(), true);

        if (searchIndex == -1) {
            return false;
        } else {
            // Maintain cursor position while searching.
            if (history.moveTo(searchIndex)) {
                setBuffer(history.current());
            } else {
                return false;
            }
        }
        return true;
    }

    //
    // History search
    //
    /**
     * Search backward in history from a given position.
     *
     * @param searchTerm substring to search for.
     * @param startIndex the index from which on to search
     * @return index where this substring has been found, or -1 else.
     */
    public int searchBackwards(String searchTerm, int startIndex) {
        return searchBackwards(searchTerm, startIndex, false);
    }

    /**
     * Search backwards in history from the current position.
     *
     * @param searchTerm substring to search for.
     * @return index where the substring has been found, or -1 else.
     */
    public int searchBackwards(String searchTerm) {
        return searchBackwards(searchTerm, history.index(), false);
    }


    public int searchBackwards(String searchTerm, int startIndex, boolean startsWith) {
        ListIterator<History.Entry> it = history.entries(startIndex);
        while (it.hasPrevious()) {
            History.Entry e = it.previous();
            if (startsWith) {
                if (e.value().startsWith(searchTerm)) {
                    return e.index();
                }
            } else {
                if (e.value().contains(searchTerm)) {
                    return e.index();
                }
            }
        }
        return -1;
    }

    public int searchForwards(String searchTerm, int startIndex, boolean startsWith) {
        if (startIndex >= history.size()) {
            startIndex = history.size() - 1;
        }
        ListIterator<History.Entry> it = history.entries(startIndex);
        if (searchIndex != -1 && it.hasNext()) {
            it.next();
        }
        while (it.hasNext()) {
            History.Entry e = it.next();
            if (startsWith) {
                if (e.value().startsWith(searchTerm)) {
                    return e.index();
                }
            } else {
                if (e.value().contains(searchTerm)) {
                    return e.index();
                }
            }
        }
        return -1;
    }

    /**
     * Search forward in history from a given position.
     *
     * @param searchTerm substring to search for.
     * @param startIndex the index from which on to search
     * @return index where this substring has been found, or -1 else.
     */
    public int searchForwards(String searchTerm, int startIndex) {
        return searchForwards(searchTerm, startIndex, false);
    }
    /**
     * Search forwards in history from the current position.
     *
     * @param searchTerm substring to search for.
     * @return index where the substring has been found, or -1 else.
     */
    public int searchForwards(String searchTerm) {
        return searchForwards(searchTerm, history.index());
    }

    public void printSearchStatus(String searchTerm, String match, boolean backward) {
        String searchLabel = backward ? "bck-i-search" : "i-search";
        post = () -> searchLabel + ": " + searchTerm + "_";
        setBuffer(match);
        buf.move(match.indexOf(searchTerm) - buf.cursor());
    }

    protected boolean interrupt() {
        state = State.INTERRUPT;
        return true;
    }

    protected boolean exitOrDeleteChar() {
        if (buf.length() == 0) {
            state = State.EOF;
            return true;
        } else {
            return deleteChar();
        }
    }

    protected boolean quit() {
        getBuffer().clear();
        return acceptLine();
    }

    protected boolean viMoveAcceptLine() {
        /*
         * VI_MOVE_ACCEPT_LINE is the result of an ENTER
         * while in move mode. This is the same as a normal
         * ACCEPT_LINE, except that we need to enter
         * insert mode as well.
         */
        setKeyMap(VIINS);
        return acceptLine();
    }

    protected boolean acceptLine() {
        String str = buf.toString();
        try {
            parsedLine = parser.parse(str, buf.cursor());
        } catch (EOFError e) {
            buf.write("\n");
            return true;
        } catch (SyntaxError e) {
            // do nothing
        }
        callWidget(CALLBACK_FINISH);
        state = State.DONE;
        if (!isSet(Option.DISABLE_EVENT_EXPANSION)) {
            try {
                String exp = expandEvents(str);
                if (!exp.equals(str)) {
                    buf.clear();
                    buf.write(exp);
                    if (isSet(Option.HISTORY_VERIFY)) {
                        state = State.NORMAL;
                    }
                }
            } catch (IllegalArgumentException e) {
                Log.error("Could not expand event", e);
                beep();
                buf.clear();
                println();
                println(e.getMessage());
                flush();
            }
        }
        return true;
    }

    protected boolean selfInsert() {
        for (int count = this.count; count > 0; count--) {
            putString(getLastBinding());
        }
        return true;
    }

    protected boolean selfInsertUnmeta() {
        if (getLastBinding().charAt(0) == '\u001b') {
            String s = getLastBinding().substring(1);
            if ("\r".equals(s)) {
                s = "\n";
            }
            for (int count = this.count; count > 0; count--) {
                putString(s);
            }
            return true;
        } else {
            return false;
        }
    }

    protected boolean overwriteMode() {
        overTyping = !overTyping;
        return true;
    }

    protected boolean previousHistory() {
        return moveHistory(false);
    }

    protected boolean viUpLineOrHistory() {
        if (moveHistory(false, count)) {
            int c = 0;
            while (buf.atChar(c) != 0 && isDelimiter(buf.atChar(c))) {
                c++;
            }
            buf.cursor(c);
            return true;
        }
        return false;
    }

    protected boolean nextHistory() {
        return moveHistory(true);

    }

    protected boolean viDownLineOrHistory() {
        if (moveHistory(true, count)) {
            int c = 0;
            while (buf.atChar(c) != 0 && isDelimiter(buf.atChar(c))) {
                c++;
            }
            buf.cursor(c);
            return true;
        }
        return false;
    }

    protected boolean upLineOrHistory() {
        String str = buf.substring(0, buf.cursor());
        if (str.contains("\n")) {
            return buf.up();
        } else {
            return historySearchBackward();
        }
    }

    protected boolean downLineOrHistory() {
        String str = buf.substring(buf.cursor());
        if (str.contains("\n")) {
            return buf.down();
        } else {
            return historySearchForward();
        }
    }

    protected boolean beginningOfHistory() {
        if (history.moveToFirst()) {
            setBuffer(history.current());
            return true;
        } else {
            return false;
        }
    }

    protected boolean endOfHistory() {
        if (history.moveToLast()) {
            setBuffer(history.current());
            return true;
        } else {
            return false;
        }
    }

    protected boolean viCmdMode() {
        // If we are re-entering move mode from an
        // aborted yank-to, delete-to, change-to then
        // don't move the cursor back. The cursor is
        // only move on an explicit entry to movement
        // mode.
        if (state == State.NORMAL) {
            buf.move(-1);
        }
        return setKeyMap(VICMD);
    }

    protected boolean viInsert() {
        return setKeyMap(VIINS);
    }

    protected boolean viAddNext() {
        buf.move(1);
        return setKeyMap(VIINS);
    }

    protected boolean viAddEol() {
        return endOfLine() && setKeyMap(VIINS);
    }

    protected boolean emacsEditingMode() {
        return setKeyMap(EMACS);
    }

    protected boolean viChangeToEol() {
        return viChangeTo(buf.cursor(), buf.length())
                && setKeyMap(VIINS);
    }

    protected boolean viDeleteToEol() {
        return viDeleteTo(buf.cursor(), buf.length());
    }

    protected boolean quotedInsert() {
        int c = readCharacter();
        for (int count = this.count; count > 0; --count) {
            putString(new String(Character.toChars(c)));
        }
        return true;
    }

    protected boolean viCharSearch() {
        int c = getLastBinding().codePointAt(0);
        int searchChar = (c != ';' && c != ',')
                ? readCharacter()
                : 0;

        return doViCharSearch(count, c, searchChar);
    }

    protected boolean viKillWholeLine() {
        return killWholeLine() && setKeyMap(VIINS);
    }

    protected boolean viInsertBol() {
        return beginningOfLine() && setKeyMap(VIINS);
    }

    protected boolean backwardDeleteChar() {
        if (count < 0) {
            count = -count;
            return deleteChar();
        }
        if (buf.cursor() == 0) {
            return false;
        }
        buf.backspace(count);
        return true;
    }

    protected boolean callLastKbdMacro() {
        bindingReader.runMacro(macro);
        return true;
    }

    protected boolean endKbdMacro() {
        String s = bindingReader.stopRecording();
        if (s == null) {
            return false;
        }
        macro = s;
        return true;
    }

    protected boolean startKbdMacro() {
        return bindingReader.startRecording();
    }

    protected boolean tabInsert() {
        putString("\t");
        return true;
    }

    protected boolean viFirstPrint() {
        beginningOfLine();
        return doViNextWord(1);
    }

    protected boolean beginningOfLine() {
        for (int count = this.count; count > 0; --count) {
            while (buf.move(-1) == -1 && buf.prevChar() != '\n') ;
        }
        return true;
    }

    protected boolean endOfLine() {
        for (int count = this.count; count > 0; --count) {
            while (buf.move(1) == 1 && buf.currChar() != '\n') ;
        }
        return true;
    }

    protected boolean deleteChar() {
        if (count < 0) {
            count = -count;
            return backwardDeleteChar();
        }
        if (buf.cursor() == buf.length()) {
            return false;
        }
        buf.delete(count);
        return true;
    }

    /**
     * Deletes the previous character from the cursor position
     */
    protected boolean viRubout() {
        for (int i = 0; i < count; i++) {
            if (!buf.backspace()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Deletes the character you are sitting on and sucks the rest of
     * the line in from the right.
     */
    protected boolean viDelete() {
        for (int i = 0; i < count; i++) {
            if (!buf.delete()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Switches the case of the current character from upper to lower
     * or lower to upper as necessary and advances the cursor one
     * position to the right.
     */
    protected boolean viChangeCase() {
        for (int i = 0; i < count; i++) {
            if (buf.cursor() < buf.length()) {
                int ch = buf.atChar(buf.cursor());
                ch = switchCase(ch);
                buf.currChar(ch);
                buf.move(1);
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Implements the vi change character command (in move-mode "r"
     * followed by the character to change to).
     */
    protected boolean viChangeChar() {
        int c = readCharacter();
        // EOF, ESC, or CTRL-C aborts.
        if (c < 0 || c == '\033' || c == '\003') {
            return true;
        }

        for (int i = 0; i < count; i++) {
            if (buf.currChar((char) c)) {
                if (i < count - 1) {
                    buf.move(1);
                }
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * This is a close facsimile of the actual vi previous word logic. In
     * actual vi words are determined by boundaries of identity characterse.
     * This logic is a bit more simple and simply looks at white space or
     * digits or characters.  It should be revised at some point.
     */
    protected boolean viPreviousWord() {
        if (buf.cursor() == 0) {
            return false;
        }

        int pos = buf.cursor() - 1;
        for (int i = 0; pos > 0 && i < count; i++) {
            // If we are on white space, then move back.
            while (pos > 0 && isWhitespace(buf.atChar(pos))) {
                --pos;
            }

            while (pos > 0 && !isDelimiter(buf.atChar(pos-1))) {
                --pos;
            }

            if (pos > 0 && i < (count-1)) {
                --pos;
            }
        }
        buf.cursor(pos);
        return true;
    }

    protected boolean viChangeTo(int startPos, int endPos) {
        return doViDeleteOrChange(startPos, endPos, true);
    }

    protected boolean viDeleteTo(int startPos, int endPos) {
        return doViDeleteOrChange(startPos, endPos, false);
    }

    /**
     * Performs the vi "delete-to" action, deleting characters between a given
     * span of the input line.
     * @param startPos The start position
     * @param endPos The end position.
     * @param isChange If true, then the delete is part of a change operationg
     *    (e.g. "c$" is change-to-end-of line, so we first must delete to end
     *    of line to start the change
     * @return true if it succeeded, false otherwise
     */
    protected boolean doViDeleteOrChange(int startPos, int endPos, boolean isChange) {
        if (startPos == endPos) {
            return true;
        }

        if (endPos < startPos) {
            int tmp = endPos;
            endPos = startPos;
            startPos = tmp;
        }

        buf.cursor(startPos);
        buf.delete(endPos - startPos);

        // If we are doing a delete operation (e.g. "d$") then don't leave the
        // cursor dangling off the end. In reality the "isChange" flag is silly
        // what is really happening is that if we are in "move-mode" then the
        // cursor can't be moved off the end of the line, but in "edit-mode" it
        // is ok, but I have no easy way of knowing which mode we are in.
        if (! isChange && startPos > 0 && startPos == buf.length()) {
            buf.move(-1);
        }
        return true;
    }

    /**
     * Implement the "vi" yank-to operation.  This operation allows you
     * to yank the contents of the current line based upon a move operation,
     * for example "yw" yanks the current word, "3yw" yanks 3 words, etc.
     *
     * @param startPos The starting position from which to yank
     * @param endPos The ending position to which to yank
     * @return true if the yank succeeded
     */
    protected boolean viYankTo(int startPos, int endPos) {
        int cursorPos = startPos;

        if (endPos < startPos) {
            int tmp = endPos;
            endPos = startPos;
            startPos = tmp;
        }

        if (startPos == endPos) {
            yankBuffer = "";
            return true;
        }

        yankBuffer = buf.substring(startPos, endPos);

        /*
         * It was a movement command that moved the cursor to find the
         * end position, so put the cursor back where it started.
         */
        buf.cursor(cursorPos);
        return true;
    }

    /**
     * Pasts the yank buffer to the right of the current cursor position
     * and moves the cursor to the end of the pasted region.
     */
    protected boolean viPut() {
        if (yankBuffer.length () != 0) {
            if (buf.cursor() < buf.length()) {
                buf.move(1);
            }
            for (int i = 0; i < count; i++) {
                putString(yankBuffer);
            }
            buf.move(-1);
        }
        return true;
    }

    protected boolean doLowercaseVersion() {
        bindingReader.runMacro(getLastBinding().toLowerCase());
        return true;
    }

    protected Map<String, Widget> builtinWidgets() {
        Map<String, Widget> widgets = new HashMap<>();
        widgets.put(ACCEPT_LINE, this::acceptLine);
        widgets.put(ARGUMENT_BASE, this::argumentBase);
        widgets.put(BACKWARD_CHAR, this::backwardChar);
        widgets.put(BACKWARD_DELETE_CHAR, this::backwardDeleteChar);
        widgets.put(BACKWARD_KILL_LINE, this::backwardKillLine);
        widgets.put(BACKWARD_KILL_WORD, this::backwardKillWord);
        widgets.put(BACKWARD_WORD, this::backwardWord);
        widgets.put(BEGINNING_OF_HISTORY, this::beginningOfHistory);
        widgets.put(BEGINNING_OF_LINE, this::beginningOfLine);
        widgets.put(CALL_LAST_KBD_MACRO, this::callLastKbdMacro);
        widgets.put(CAPITALIZE_WORD, this::capitalizeWord);
        widgets.put(CLEAR_SCREEN, this::clearScreen);
        widgets.put(COMPLETE_PREFIX, this::completePrefix);
        widgets.put(COMPLETE_WORD, this::completeWord);
        widgets.put(DELETE_CHAR, this::deleteChar);
        widgets.put(DELETE_CHAR_OR_LIST, this::deleteCharOrList);
        widgets.put(DIGIT_ARGUMENT, this::digitArgument);
        widgets.put(DO_LOWERCASE_VERSION, this::doLowercaseVersion);
        widgets.put(DOWN_CASE_WORD, this::downCaseWord);
        widgets.put(DOWN_LINE_OR_HISTORY, this::downLineOrHistory);
        widgets.put(EMACS_EDITING_MODE, this::emacsEditingMode);
        widgets.put(END_KBD_MACRO, this::endKbdMacro);
        widgets.put(END_OF_HISTORY, this::endOfHistory);
        widgets.put(END_OF_LINE, this::endOfLine);
        widgets.put(EXIT_OR_DELETE_CHAR, this::exitOrDeleteChar);
        widgets.put(FORWARD_CHAR, this::forwardChar);
        widgets.put(FORWARD_WORD, this::forwardWord);
        widgets.put(HISTORY_INCREMENTAL_SEARCH_BACKWARD, this::historyIncrementalSearchBackward);
        widgets.put(HISTORY_INCREMENTAL_SEARCH_FORWARD, this::historyIncrementalSearchForward);
        widgets.put(HISTORY_SEARCH_BACKWARD, this::historySearchBackward);
        widgets.put(HISTORY_SEARCH_FORWARD, this::historySearchForward);
        widgets.put(INSERT_CLOSE_CURLY, this::insertCloseCurly);
        widgets.put(INSERT_CLOSE_PAREN, this::insertCloseParen);
        widgets.put(INSERT_CLOSE_SQUARE, this::insertCloseSquare);
        widgets.put(INSERT_COMMENT, this::insertComment);
        widgets.put(INTERRUPT, this::interrupt);
        widgets.put(KILL_LINE, this::killLine);
        widgets.put(KILL_WHOLE_LINE, this::killWholeLine);
        widgets.put(KILL_WORD, this::killWord);
        widgets.put(MENU_COMPLETE, this::menuComplete);
        widgets.put(NEG_ARGUMENT, this::negArgument);
        widgets.put(DOWN_HISTORY, this::nextHistory);
        widgets.put(OVERWRITE_MODE, this::overwriteMode);
        widgets.put(PASTE_FROM_CLIPBOARD, this::pasteFromClipboard);
        widgets.put(POSSIBLE_COMPLETIONS, this::listChoices);
        widgets.put(UP_HISTORY, this::previousHistory);
        widgets.put(QUIT, this::quit);
        widgets.put(QUOTED_INSERT, this::quotedInsert);
        widgets.put(SELF_INSERT, this::selfInsert);
        widgets.put(SELF_INSERT_UNMETA, this::selfInsertUnmeta);
        widgets.put(SEND_BREAK, this::sendBreak);
        widgets.put(START_KBD_MACRO, this::startKbdMacro);
        widgets.put(TAB_INSERT, this::tabInsert);
        widgets.put(TRANSPOSE_CHARS, this::transposeChars);
        widgets.put(TRANSPOSE_WORDS, this::transposeWords);
        widgets.put(UNDEFINED_KEY, this::undefinedKey);
        widgets.put(UNIVERSAL_ARGUMENT, this::universalArgument);
        widgets.put(UP_CASE_WORD, this::upCaseWord);
        widgets.put(UNDO, this::undo);
        widgets.put(UP_LINE_OR_HISTORY, this::upLineOrHistory);
        widgets.put(VI_ADD_EOL, this::viAddEol);
        widgets.put(VI_ADD_NEXT, this::viAddNext);
        widgets.put(VI_DIGIT_OR_BEGINNING_OF_LINE, this::viDigitOrBeginningOfLine);
        widgets.put(VI_CHANGE_CASE, this::viChangeCase);
        widgets.put(VI_CHANGE_CHAR, this::viChangeChar);
        widgets.put(VI_CHANGE_TO, this::viChangeTo);
        widgets.put(VI_CHANGE_TO_EOL, this::viChangeToEol);
        widgets.put(VI_CHAR_SEARCH, this::viCharSearch);
        widgets.put(VI_DELETE, this::viDelete);
        widgets.put(VI_DELETE_TO, this::viDeleteTo);
        widgets.put(VI_DELETE_TO_EOL, this::viDeleteToEol);
        widgets.put(VI_END_WORD, this::viEndWord);
        widgets.put(VI_EOF_MAYBE, this::viEofMaybe);
        widgets.put(VI_FIRST_PRINT, this::viFirstPrint);
        widgets.put(VI_INSERT_BOL, this::viInsertBol);
        widgets.put(VI_INSERT_COMMENT, this::viInsertComment);
        widgets.put(VI_INSERT, this::viInsert);
        widgets.put(VI_KILL_WHOLE_LINE, this::viKillWholeLine);
        widgets.put(VI_MATCH_BRACKET, this::viMatchBracket);
        widgets.put(VI_MOVE_ACCEPT_LINE, this::viMoveAcceptLine);
        widgets.put(VI_CMD_MODE, this::viCmdMode);
        widgets.put(VI_DOWN_LINE_OR_HISTORY, this::viDownLineOrHistory);
        widgets.put(VI_NEXT_WORD, this::viNextWord);
        widgets.put(VI_PREV_WORD, this::viPreviousWord);
        widgets.put(VI_UP_LINE_OR_HISTORY, this::viUpLineOrHistory);
        widgets.put(VI_PUT, this::viPut);
        widgets.put(VI_RUBOUT, this::viRubout);
        widgets.put(VI_SEARCH, this::viSearch);
        widgets.put(VI_YANK_TO, this::viYankTo);
        widgets.put(YANK, this::yank);
        widgets.put(YANK_POP, this::yankPop);
        return widgets;
    }

    protected void redisplay() {
        redisplay(true);
    }

    protected void redisplay(boolean flush) {
        if (skipRedisplay) {
            skipRedisplay = false;
            return;
        }
        // TODO: support TERM_SHORT, terminal lines < 3
        String buffer = buf.toString();
        if (mask != null) {
            if (mask == NULL_MASK) {
                buffer = "";
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = buffer.length(); i-- > 0;) {
                    sb.append((char) mask);
                }
                buffer = sb.toString();
            }
        } else if (highlighter != null) {
            buffer = highlighter.highlight(this, buffer);
        }

        List<String> secondaryPrompts = new ArrayList<>();
        String tNewBuf = insertSecondaryPrompts(buffer, secondaryPrompts);

        List<String> newLines = AnsiHelper.splitLines(prompt + tNewBuf + (post != null ? "\n" + post.get() : ""), size.getColumns(), TAB_WIDTH);
        List<String> rightPromptLines = rightPrompt.isEmpty() ? Collections.emptyList() : AnsiHelper.splitLines(rightPrompt, size.getColumns(), TAB_WIDTH);

        while (newLines.size() < rightPromptLines.size()) {
            newLines.add("");
        }
        for (int i = 0; i < rightPromptLines.size(); i++) {
            String line = rightPromptLines.get(i);
            newLines.set(i, addRightPrompt(line, newLines.get(i)));
        }

        int cursorPos = -1;
        // TODO: buf.upToCursor() does not take into account the mask which could modify the display length
        // TODO: in case of wide chars
        List<String> promptLines = AnsiHelper.splitLines(prompt + insertSecondaryPrompts(buf.upToCursor(), secondaryPrompts, false), size.getColumns(), TAB_WIDTH);
        if (!promptLines.isEmpty()) {
            cursorPos = (promptLines.size() - 1) * size.getColumns()
                            + display.wcwidth(promptLines.get(promptLines.size() - 1));
        }

        display.update(newLines, cursorPos);

        if (flush) {
            flush();
        }
    }

    private static String SECONDARY_PROMPT = "> ";

    private String insertSecondaryPrompts(String str, List<String> prompts) {
        return insertSecondaryPrompts(str, prompts, true);
    }

    private String insertSecondaryPrompts(String str, List<String> prompts, boolean computePrompts) {
        checkNotNull(prompts);
        StringBuilder sb = new StringBuilder();
        int line = 0;
        if (computePrompts || !isSet(Option.PAD_PROMPTS) || prompts.size() < 2) {
            List<String> strippedLines = AnsiHelper.splitLines(AnsiHelper.strip(str), Integer.MAX_VALUE, TAB_WIDTH);
            List<String> ansiLines = AnsiHelper.splitLines(str, Integer.MAX_VALUE, TAB_WIDTH);
            StringBuilder buf = new StringBuilder();
            while (line < strippedLines.size() - 1) {
                sb.append(ansiLines.get(line)).append("\n");
                buf.append(strippedLines.get(line)).append("\n");
                String prompt;
                if (computePrompts) {
                    prompt = SECONDARY_PROMPT;
                    try {
                        parser.parse(buf.toString(), buf.length());
                    } catch (EOFError e) {
                        prompt = e.getMissing() + SECONDARY_PROMPT;
                    } catch (SyntaxError e) {
                        // Ignore
                    }
                } else {
                    prompt = prompts.get(line);
                }
                prompts.add(prompt);
                sb.append(prompt);
                line++;
            }
            sb.append(ansiLines.get(line));
            buf.append(strippedLines.get(line));
        }
        if (isSet(Option.PAD_PROMPTS) && prompts.size() >= 2) {
            if (computePrompts) {
                int max = prompts.stream().map(String::length).max(Comparator.<Integer>naturalOrder()).get();
                for (ListIterator<String> it = prompts.listIterator(); it.hasNext(); ) {
                    String prompt = it.next();
                    if (prompt.length() < max) {
                        StringBuilder pb = new StringBuilder(max);
                        pb.append(prompt, 0, prompt.length() - SECONDARY_PROMPT.length());
                        while (pb.length() < max - SECONDARY_PROMPT.length()) {
                            pb.append(' ');
                        }
                        pb.append(SECONDARY_PROMPT);
                        it.set(pb.toString());
                    }
                }
            }
            sb.setLength(0);
            line = 0;
            for (int i = 0; i < str.length(); i++) {
                char ch = str.charAt(i);
                sb.append(ch);
                if (ch == '\n') {
                    sb.append(prompts.get(line++));
                }
            }
        }
        return sb.toString();
    }

    private String addRightPrompt(String prompt, String line) {
        int width = display.wcwidth(prompt);
        int nb = size.getColumns() - width - display.wcwidth(line) - 3;
        if (nb >= 0) {
            StringBuilder sb = new StringBuilder(size.getColumns());
            sb.append(line);
            for (int j = 0; j < nb + 2; j++) {
                sb.append(' ');
            }
            sb.append(prompt);
            line = sb.toString();
        }
        return line;
    }

    //
    // Completion
    //

    protected boolean insertTab() {
        return getLastBinding().equals("\t") && buf.toString().matches("(^|[\\s\\S]*\n)[\r\n\t ]*");
    }

    protected boolean doExpandHist() {
        String str = buf.toString();
        String exp = expandEvents(str);
        if (!exp.equals(str)) {
            buf.clear();
            buf.write(exp);
            return true;
        } else {
            return false;
        }
    }

    enum CompletionType {
        Complete,
        List,
    }

    protected boolean completeWord() {
        if (insertTab()) {
            return selfInsert();
        } else {
            return doComplete(CompletionType.Complete, isSet(Option.MENU_COMPLETE), false);
        }
    }

    protected boolean menuComplete() {
        if (insertTab()) {
            return selfInsert();
        } else {
            return doComplete(CompletionType.Complete, true, false);
        }
    }

    protected boolean completePrefix() {
        if (insertTab()) {
            return selfInsert();
        } else {
            return doComplete(CompletionType.Complete, isSet(Option.MENU_COMPLETE), true);
        }
    }

    protected boolean listChoices() {
        return doComplete(CompletionType.List, isSet(Option.MENU_COMPLETE), false);
    }

    protected boolean deleteCharOrList() {
        if (buf.cursor() != buf.length() || buf.length() == 0) {
            return exitOrDeleteChar();
        } else {
            return doComplete(CompletionType.List, isSet(Option.MENU_COMPLETE), false);
        }
    }

    protected boolean doComplete(CompletionType lst, boolean useMenu, boolean prefix) {
        // Try to expand history first
        // If there is actually an expansion, bail out now
        try {
            if (doExpandHist()) {
                return true;
            }
        } catch (IllegalArgumentException e) {
            return false;
        }

        // Parse the command line and find completion candidates
        List<Candidate> candidates = new ArrayList<>();
        ParsedLine line;
        try {
            line = parser.parse(buf.toString(), buf.cursor());
            if (completer != null) {
                completer.complete(this, line, candidates);
            }
        } catch (Exception e) {
            return false;
        }

        boolean caseInsensitive = isSet(Option.CASE_INSENSITIVE);
        int errors = getInt("errors", 2);

        // Build a list of sorted candidates
        NavigableMap<String, List<Candidate>> sortedCandidates =
                new TreeMap<>(caseInsensitive ? String.CASE_INSENSITIVE_ORDER : null);
        for (Candidate cand : candidates) {
            sortedCandidates
                    .computeIfAbsent(AnsiHelper.strip(cand.value()), s -> new ArrayList<>())
                    .add(cand);
        }

        // Find matchers
        // TODO: glob completion
        List<Function<Map<String, List<Candidate>>,
                      Map<String, List<Candidate>>>> matchers;
        Predicate<String> exact;
        if (prefix) {
            String wp = line.word().substring(0, line.wordCursor());
            matchers = Arrays.asList(
                    simpleMatcher(s -> s.startsWith(wp)),
                    simpleMatcher(s -> s.contains(wp)),
                    typoMatcher(wp, errors)
            );
            exact = s -> s.equals(wp);
        } else if (isSet(Option.COMPLETE_IN_WORD)) {
            String wd = line.word();
            String wp = wd.substring(0, line.wordCursor());
            String ws = wd.substring(line.wordCursor());
            Pattern p1 = Pattern.compile(Pattern.quote(wp) + ".*" + Pattern.quote(ws) + ".*");
            Pattern p2 = Pattern.compile(".*" + Pattern.quote(wp) + ".*" + Pattern.quote(ws) + ".*");
            matchers = Arrays.asList(
                    simpleMatcher(s -> p1.matcher(s).matches()),
                    simpleMatcher(s -> p2.matcher(s).matches()),
                    typoMatcher(wd, errors)
            );
            exact = s -> s.equals(wd);
        } else {
            String wd = line.word();
            matchers = Arrays.asList(
                    simpleMatcher(s -> s.startsWith(wd)),
                    simpleMatcher(s -> s.contains(wd)),
                    typoMatcher(wd, errors)
            );
            exact = s -> s.equals(wd);
        }
        // Find matching candidates
        Map<String, List<Candidate>> matching = Collections.emptyMap();
        for (Function<Map<String, List<Candidate>>,
                      Map<String, List<Candidate>>> matcher : matchers) {
            matching = matcher.apply(sortedCandidates);
            if (!matching.isEmpty()) {
                break;
            }
        }

        // If we have no matches, bail out
        if (matching.isEmpty()) {
            return false;
        }

        // If we only need to display the list, do it now
        if (lst == CompletionType.List) {
            List<Candidate> possible = matching.entrySet().stream()
                    .flatMap(e -> e.getValue().stream())
                    .collect(Collectors.toList());
            doList(possible);
            return !possible.isEmpty();
        }

        // Check if there's a single possible match
        Candidate completion = null;
        // If there's a single possible completion
        if (matching.size() == 1) {
            completion = matching.values().stream().<Candidate>flatMap(Collection::stream)
                    .findFirst().orElse(null);
        }
        // Or if RECOGNIZE_EXACT is set, try to find an exact match
        else if (isSet(Option.RECOGNIZE_EXACT)) {
            completion = matching.values().stream().<Candidate>flatMap(Collection::stream)
                    .filter(Candidate::complete)
                    .filter(c -> exact.test(c.value()))
                    .findFirst().orElse(null);
        }
        // Complete and exit
        if (completion != null) {
            if (prefix) {
                buf.backspace(line.wordCursor());
            } else {
                buf.move(line.word().length() - line.wordCursor());
                buf.backspace(line.word().length());
            }
            buf.write(completion.value());
            if (completion.complete() && buf.currChar() != ' ') {
                buf.write(" ");
            }
            if (completion.suffix() != null) {
                redisplay();
                Binding op = readBinding(getKeys());
                if (op != null) {
                    String chars = getString("REMOVE_SUFFIX_CHARS", " \t\n;&|");
                    String ref = op instanceof Reference ? ((Reference) op).name() : null;
                    if (SELF_INSERT.equals(ref) && chars.indexOf(getLastBinding().charAt(0)) >= 0
                            || ACCEPT_LINE.equals(ref)) {
                        buf.backspace(completion.suffix().length());
                        if (getLastBinding().charAt(0) != ' ') {
                            buf.write(' ');
                        }
                    }
                    pushBackBinding(true);
                }
            }
            return true;
        }

        List<Candidate> possible = matching.entrySet().stream()
                .flatMap(e -> e.getValue().stream())
                .collect(Collectors.toList());

        if (useMenu) {
            buf.move(line.word().length() - line.wordCursor());
            buf.backspace(line.word().length());
            doMenu(possible);
            return true;
        }

        // Find current word and move to end
        String current;
        if (prefix) {
            current = line.word().substring(0, line.wordCursor());
        } else {
            current = line.word();
            buf.move(current.length() - line.wordCursor());
        }
        // Now, we need to find the unambiguous completion
        // TODO: need to find common suffix
        String commonPrefix = null;
        for (String key : matching.keySet()) {
            commonPrefix = commonPrefix == null ? key : getCommonStart(commonPrefix, key, caseInsensitive);
        }
        boolean hasUnambiguous = commonPrefix.startsWith(current) && !commonPrefix.equals(current);

        if (hasUnambiguous) {
            buf.backspace(current.length());
            buf.write(commonPrefix);
            current = commonPrefix;
            if ((!isSet(Option.AUTO_LIST) && isSet(Option.AUTO_MENU))
                    || (isSet(Option.AUTO_LIST) && isSet(Option.LIST_AMBIGUOUS))) {
                if (!nextBindingIsComplete()) {
                    return true;
                }
            }
        }
        if (isSet(Option.AUTO_LIST)) {
            doList(possible);
            if (isSet(Option.AUTO_MENU)) {
                if (!nextBindingIsComplete()) {
                    return true;
                }
            }
        }
        if (isSet(Option.AUTO_MENU)) {
            buf.backspace(current.length());
            doMenu(possible);
        }
        return true;
    }

    private void mergeCandidates(List<Candidate> possible) {
        // Merge candidates if the have the same key
        Map<String, List<Candidate>> keyedCandidates = new HashMap<>();
        for (Candidate candidate : possible) {
            if (candidate.key() != null) {
                List<Candidate> cands = keyedCandidates.computeIfAbsent(candidate.key(), s -> new ArrayList<>());
                cands.add(candidate);
            }
        }
        if (!keyedCandidates.isEmpty()) {
            for (List<Candidate> candidates : keyedCandidates.values()) {
                if (candidates.size() >= 1) {
                    possible.removeAll(candidates);
                    // Candidates with the same key are supposed to have
                    // the same description
                    candidates.sort(Comparator.comparing(Candidate::value));
                    Candidate first = candidates.get(0);
                    String disp = candidates.stream()
                            .map(Candidate::displ)
                            .collect(Collectors.joining(" "));
                    possible.add(new Candidate(first.value(), disp, first.group(),
                            first.descr(), first.suffix(), null, first.complete()));
                }
            }
        }
    }

    private Function<Map<String, List<Candidate>>,
                     Map<String, List<Candidate>>> simpleMatcher(Predicate<String> pred) {
        return m -> m.entrySet().stream()
                .filter(e -> pred.test(e.getKey()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    private Function<Map<String, List<Candidate>>,
                     Map<String, List<Candidate>>> typoMatcher(String word, int errors) {
        return m -> {
            Map<String, List<Candidate>> map = m.entrySet().stream()
                    .filter(e -> Levenshtein.distance(
                                    word, e.getKey().substring(0, Math.min(e.getKey().length(), word.length()))) < errors)
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            if (map.size() > 1) {
                map.computeIfAbsent(word, w -> new ArrayList<>())
                        .add(new Candidate(word, word, "original", null, null, null, false));
            }
            return map;
        };
    }


    protected boolean nextBindingIsComplete() {
        redisplay();
        KeyMap keyMap = keyMaps.get(MENU_SELECT);
        Binding operation = readBinding(getKeys(), keyMap);
        if (operation instanceof Reference && MENU_COMPLETE.equals(((Reference) operation).name())) {
            return true;
        } else {
            pushBackBinding();
            return false;
        }
    }

    private class MenuSupport implements Supplier<String> {
        final List<Candidate> possible;
        int selection;
        int topLine;
        String word;
        String computed;
        int lines;
        int columns;

        public MenuSupport(List<Candidate> original) {
            this.possible = new ArrayList<>();
            this.selection = -1;
            this.topLine = 0;
            this.word = "";
            computePost(original, null, possible);
            next();
        }

        public Candidate completion() {
            return possible.get(selection);
        }

        public void next() {
            selection = (selection + 1) % possible.size();
            update();
        }

        public void previous() {
            selection = (selection + possible.size() - 1) % possible.size();
            update();
        }

        public void down() {
            if (isSet(Option.LIST_ROWS_FIRST)) {
                int r = selection / columns;
                int c = selection % columns;
                if ((r + 1) * columns + c < possible.size()) {
                    r++;
                } else if (c + 1 < columns) {
                    c++;
                    r = 0;
                } else {
                    r = 0;
                    c = 0;
                }
                selection = r * columns + c;
                update();
            } else {
                next();
            }
        }
        public void left() {
            if (isSet(Option.LIST_ROWS_FIRST)) {
                previous();
            } else {
                int c = selection / lines;
                int r = selection % lines;
                if (c - 1 >= 0) {
                    c--;
                } else {
                    c = columns - 1;
                    r--;
                }
                selection = c * lines + r;
                if (selection < 0) {
                    selection = possible.size() - 1;
                }
                update();
            }
        }
        public void right() {
            if (isSet(Option.LIST_ROWS_FIRST)) {
                next();
            } else {
                int c = selection / lines;
                int r = selection % lines;
                if (c + 1 < columns) {
                    c++;
                } else {
                    c = 0;
                    r++;
                }
                selection = c * lines + r;
                if (selection >= possible.size()) {
                    selection = 0;
                }
                update();
            }
        }
        public void up() {
            if (isSet(Option.LIST_ROWS_FIRST)) {
                int r = selection / columns;
                int c = selection % columns;
                if (r > 0) {
                    r--;
                } else {
                    c = (c + columns - 1) % columns;
                    r = lines - 1;
                    if (r * columns + c >= possible.size()) {
                        r--;
                    }
                }
                selection = r * columns + c;
                update();
            } else {
                previous();
            }
        }

        private void update() {
            buf.backspace(word.length());
            word = completion().value();
            buf.write(word);

            // Compute displayed prompt
            PostResult pr = computePost(possible, completion(), null);
            String text = insertSecondaryPrompts(prompt + buf.toString(), new ArrayList<>());
            int promptLines = AnsiHelper.splitLines(text, size.getColumns(), TAB_WIDTH).size();
            if (pr.lines >= size.getRows() - promptLines) {
                int displayed = size.getRows() - promptLines - 1;
                if (pr.selectedLine >= 0) {
                    if (pr.selectedLine < topLine) {
                        topLine = pr.selectedLine;
                    } else if (pr.selectedLine >= topLine + displayed) {
                        topLine = pr.selectedLine - displayed + 1;
                    }
                }
                List<String> lines = AnsiHelper.splitLines(pr.post, size.getColumns(), TAB_WIDTH);
                List<String> sub = new ArrayList<>(lines.subList(topLine, topLine + displayed));
                sub.add(Ansi.ansi().fg(Color.CYAN).a("rows ")
                        .a(topLine + 1).a(" to ").a(topLine + displayed)
                        .a(" of ").a(lines.size()).fg(Color.DEFAULT).toString());
                computed = String.join("\n", sub);
            } else {
                computed = pr.post;
            }
            lines = pr.lines;
            columns = (possible.size() + lines - 1) / lines;
        }

        @Override
        public String get() {
            return computed;
        }

    }

    protected boolean doMenu(List<Candidate> original) {
        // Reorder candidates according to display order
        final List<Candidate> possible = new ArrayList<>();
        mergeCandidates(original);
        computePost(original, null, possible);

        // Build menu support
        MenuSupport menuSupport = new MenuSupport(original);
        post = menuSupport;
        redisplay();

        // Loop
        console.puts(Capability.keypad_xmit);
        KeyMap keyMap = keyMaps.get(MENU_SELECT);
        Binding operation;
        while ((operation = readBinding(getKeys(), keyMap)) != null) {
            String ref = (operation instanceof Reference) ? ((Reference) operation).name() : "";
            switch (ref) {
                case MENU_COMPLETE:
                    menuSupport.next();
                    break;
                case REVERSE_MENU_COMPLETE:
                    menuSupport.previous();
                    break;
                case UP_LINE_OR_HISTORY:
                    menuSupport.up();
                    break;
                case DOWN_LINE_OR_HISTORY:
                    menuSupport.up();
                    break;
                case FORWARD_CHAR:
                    menuSupport.right();
                    break;
                case BACKWARD_CHAR:
                    menuSupport.left();
                    break;
                case CLEAR_SCREEN:
                    clearScreen();
                    break;
                default: {
                    Candidate completion = menuSupport.completion();
                    if (completion.suffix() != null) {
                        String chars = getString("REMOVE_SUFFIX_CHARS", " \t\n;&|");
                        if (SELF_INSERT.equals(ref) && chars.indexOf(getLastBinding().charAt(0)) >= 0
                                || ACCEPT_LINE.equals(ref)
                                || BACKWARD_DELETE_CHAR.equals(ref)) {
                            buf.backspace(completion.suffix().length());
                        }
                    }
                    if (completion.complete()
                            && getLastBinding().charAt(0) != ' '
                            && (SELF_INSERT.equals(ref) || getLastBinding().charAt(0) != ' ')) {
                        buf.write(' ');
                    }
                    if (!ACCEPT_LINE.equals(ref)
                            && !BACKWARD_DELETE_CHAR.equals(ref)) {
                        pushBackBinding(true);
                    }
                    post = null;
                    console.puts(Capability.keypad_local);
                    return true;
                }
            }
            redisplay();
        }
        console.puts(Capability.keypad_local);
        return false;
    }

    protected void doList(List<Candidate> possible) {
        // If we list only and if there's a big
        // number of items, we should ask the user
        // for confirmation, display the list
        // and redraw the line at the bottom
        mergeCandidates(possible);
        String text = insertSecondaryPrompts(prompt + buf.toString(), new ArrayList<>());
        int promptLines = AnsiHelper.splitLines(text, size.getColumns(), TAB_WIDTH).size();
        PostResult postResult = computePost(possible, null, null);
        int lines = postResult.lines;
        int listMax = getInt("list-max", 100);
        if (listMax > 0 && possible.size() >= listMax
                || lines >= size.getRows() - promptLines) {
            // prompt
            post = null;
            int oldCursor = buf.cursor();
            buf.cursor(buf.length());
            redisplay(true);
            buf.cursor(oldCursor);
            println();
            print(getAppName() + ": do you wish to see to see all " + possible.size()
                    + " possibilities (" + lines + " lines)?");
            flush();
            int c = readCharacter();
            if (c != 'y' && c != 'Y' && c != '\t') {
                return;
            }
        }
        /*
        if (lines >= size.getRows() - promptLines) {
            post = null;
            int oldCursor = buf.cursor();
            buf.cursor(buf.length());
            redisplay(false);
            buf.cursor(oldCursor);
            println();
            rawPrintln(postResult.post);
            redrawLine();
        } else*/ {
            post = () -> {
                String t = insertSecondaryPrompts(prompt + buf.toString(), new ArrayList<>());
                int pl = AnsiHelper.splitLines(t, size.getColumns(), TAB_WIDTH).size();
                PostResult pr = computePost(possible, null, null);
                if (pr.lines >= size.getRows() - pl) {
                    post = null;
                    int oldCursor = buf.cursor();
                    buf.cursor(buf.length());
                    redisplay(false);
                    buf.cursor(oldCursor);
                    println();
                    println(postResult.post);
                    redrawLine();
                    return "";
                }
                return pr.post;
            };
        }
    }

    private static class PostResult {
        final String post;
        final int lines;
        final int selectedLine;

        public PostResult(String post, int lines, int selectedLine) {
            this.post = post;
            this.lines = lines;
            this.selectedLine = selectedLine;
        }
    }

    protected PostResult computePost(List<Candidate> possible, Candidate selection, List<Candidate> ordered) {
        List<Object> strings = new ArrayList<>();
        boolean groupName = isSet(Option.GROUP);
        if (groupName) {
            LinkedHashMap<String, TreeMap<String, Candidate>> sorted = new LinkedHashMap<>();
            for (Candidate cand : possible) {
                String group = cand.group();
                sorted.computeIfAbsent(group != null ? group : "", s -> new TreeMap<>())
                        .put(cand.value(), cand);
            }
            for (Map.Entry<String, TreeMap<String, Candidate>> entry : sorted.entrySet()) {
                String group = entry.getKey();
                if (group.isEmpty() && sorted.size() > 1) {
                    group = "others";
                }
                if (!group.isEmpty()) {
                    strings.add(group);
                }
                strings.add(new ArrayList<>(entry.getValue().values()));
                if (ordered != null) {
                    ordered.addAll(entry.getValue().values());
                }
            }
        } else {
            Set<String> groups = new LinkedHashSet<>();
            TreeMap<String, Candidate> sorted = new TreeMap<>();
            for (Candidate cand : possible) {
                String group = cand.group();
                if (group != null) {
                    groups.add(group);
                }
                sorted.put(cand.value(), cand);
            }
            for (String group : groups) {
                strings.add(group);
            }
            strings.add(new ArrayList<>(sorted.values()));
            if (ordered != null) {
                ordered.addAll(sorted.values());
            }
        }
        return toColumns(strings, selection);
    }

    private static final String DESC_PREFIX = "(";
    private static final String DESC_SUFFIX = ")";
    private static final int MARGIN_BETWEEN_DISPLAY_AND_DESC = 1;
    private static final int MARGIN_BETWEEN_COLUMNS = 3;

    @SuppressWarnings("unchecked")
    protected PostResult toColumns(List<Object> items, Candidate selection) {
        int[] out = new int[2];
        int width = size.getColumns();
        // TODO: support Option.LIST_PACKED
        // Compute column width
        int maxWidth = 0;
        for (Object item : items) {
            if (item instanceof String) {
                int len = display.wcwidth((String) item);
                maxWidth = Math.max(maxWidth, len);
            }
            else if (item instanceof List) {
                for (Candidate cand : (List<Candidate>) item) {
                    int len = display.wcwidth(cand.displ());
                    if (cand.descr() != null) {
                        len += MARGIN_BETWEEN_DISPLAY_AND_DESC;
                        len += DESC_PREFIX.length();
                        len += display.wcwidth(cand.descr());
                        len += DESC_SUFFIX.length();
                    }
                    maxWidth = Math.max(maxWidth, len);
                }
            }
        }
        // Build columns
        Ansi ansi = Ansi.ansi();
        for (Object list : items) {
            toColumns(list, width, maxWidth, ansi, selection, out);
        }
        String str = ansi.toString();
        if (str.endsWith("\n")) {
            str = str.substring(0, str.length() - 1);
        }
        return new PostResult(str, out[0], out[1]);
    }

    @SuppressWarnings("unchecked")
    protected void toColumns(Object items, int width, int maxWidth, Ansi ansi, Candidate selection, int[] out) {
        // This is a group
        if (items instanceof String) {
            ansi.fg(Color.CYAN)
                    .a((String) items)
                    .fg(Color.DEFAULT)
                    .a("\n");
            out[0]++;
        }
        // This is a Candidate list
        else if (items instanceof List) {
            List<Candidate> candidates = (List<Candidate>) items;
            maxWidth = Math.min(width, maxWidth);
            int c = width / maxWidth;
            while (c > 1 && c * maxWidth + (c - 1) * MARGIN_BETWEEN_COLUMNS >= width) {
                c--;
            }
            int columns = c;
            int lines = (candidates.size() + columns - 1) / columns;
            IntBinaryOperator index;
            if (isSet(Option.LIST_ROWS_FIRST)) {
                index = (i, j) -> i * columns + j;
            } else {
                index = (i, j) -> j * lines + i;
            }
            for (int i = 0; i < lines; i++) {
                for (int j = 0; j < columns; j++) {
                    int idx = index.applyAsInt(i, j);
                    if (idx < candidates.size()) {
                        Candidate cand = candidates.get(idx);
                        boolean hasRightItem = j < columns - 1 && index.applyAsInt(i, j + 1) < candidates.size();
                        String left = cand.displ();
                        String right = cand.descr();
                        int lw = display.wcwidth(left);
                        int rw = 0;
                        if (right != null) {
                            int rem = maxWidth - (lw + MARGIN_BETWEEN_DISPLAY_AND_DESC
                                    + DESC_PREFIX.length() + DESC_SUFFIX.length());
                            rw = display.wcwidth(right);
                            if (rw > rem) {
                                right = AnsiHelper.cut(right, rem - WCWidth.wcwidth('…'), 1) + "…";
                                rw = display.wcwidth(right);
                            }
                            right = DESC_PREFIX + right + DESC_SUFFIX;
                            rw += DESC_PREFIX.length() + DESC_SUFFIX.length();
                        }
                        if (cand == selection) {
                            out[1] = i;
                            ansi.a(Attribute.NEGATIVE_ON);
                            ansi.a(AnsiHelper.strip(left));
                            for (int k = 0; k < maxWidth - lw - rw; k++) {
                                ansi.a(' ');
                            }
                            if (right != null) {
                                ansi.a(AnsiHelper.strip(right));
                            }
                            ansi.a(Attribute.NEGATIVE_OFF);
                        } else {
                            ansi.a(left);
                            if (right != null || hasRightItem) {
                                for (int k = 0; k < maxWidth - lw - rw; k++) {
                                    ansi.a(' ');
                                }
                            }
                            if (right != null) {
                                ansi.fgBright(Color.BLACK);
                                ansi.a(right);
                                ansi.fg(Color.DEFAULT);
                            }
                        }
                        if (hasRightItem) {
                            for (int k = 0; k < MARGIN_BETWEEN_COLUMNS; k++) {
                                ansi.a(' ');
                            }
                        }
                    }
                }
                ansi.a('\n');
            }
            out[0] += lines;
        }
    }

    private String getCommonStart(String str1, String str2, boolean caseInsensitive) {
        int[] s1 = str1.codePoints().toArray();
        int[] s2 = str2.codePoints().toArray();
        int len = 0;
        while (len < Math.min(s1.length, s2.length)) {
            int ch1 = s1[len];
            int ch2 = s2[len];
            if (ch1 != ch2 && caseInsensitive) {
                ch1 = Character.toUpperCase(ch1);
                ch2 = Character.toUpperCase(ch2);
                if (ch1 != ch2) {
                    ch1 = Character.toLowerCase(ch1);
                    ch2 = Character.toLowerCase(ch2);
                }
            }
            if (ch1 != ch2) {
                break;
            }
            len++;
        }
        return new String(s1, 0, len);
    }

    /**
     * Used in "vi" mode for argumented history move, to move a specific
     * number of history entries forward or back.
     *
     * @param next If true, move forward
     * @param count The number of entries to move
     * @return true if the move was successful
     */
    protected boolean moveHistory(final boolean next, int count) {
        boolean ok = true;
        for (int i = 0; i < count && (ok = moveHistory(next)); i++) {
            /* empty */
        }
        return ok;
    }

    /**
     * Move up or down the history tree.
     */
    protected boolean moveHistory(final boolean next) {
        if (next && !history.next()) {
            return false;
        }
        else if (!next && !history.previous()) {
            return false;
        }

        setBuffer(history.current());

        return true;
    }

    //
    // Printing
    //

    /**
     * Raw output printing
     */
    void print(String str) {
        console.writer().write(str);
    }

    void println(String s) {
        print(s);
        println();
    }

    /**
     * Output a platform-dependant newline.
     */
    void println() {
        console.puts(Capability.carriage_return);
        print("\n");
        redrawLine();
    }


    //
    // Actions
    //

    /**
     * Issue a delete.
     *
     * @return true if successful
     */
    public boolean delete() {
        return buf.delete();
    }

    protected boolean killWholeLine() {
        if (buf.length() == 0) {
            return false;
        }
        int start;
        int end;
        if (count < 0) {
            end = buf.cursor();
            while (buf.atChar(end) != 0 && buf.atChar(end) != '\n') {
                end++;
            }
            start = end;
            for (int count = -this.count; count > 0; --count) {
                while (start > 0 && buf.atChar(start - 1) != '\n') {
                    start--;
                }
                start--;
            }
        } else {
            start = buf.cursor();
            while (start > 0 && buf.atChar(start - 1) != '\n') {
                start--;
            }
            end = start;
            for (int count = this.count; count > 0; --count) {
                while (end < buf.length() && buf.atChar(end) != '\n') {
                    end++;
                }
                end++;
            }
        }
        String killed = buf.substring(start, end);
        buf.cursor(start);
        buf.delete(end - start);
        killRing.add(killed);
        return true;
    }

    /**
     * Kill the buffer ahead of the current cursor position.
     *
     * @return true if successful
     */
    public boolean killLine() {
        if (count < 0) {
            count = -count;
            return backwardKillLine();
        }
        if (buf.cursor() == buf.length()) {
            return false;
        }
        int cp = buf.cursor();
        int len = cp;
        for (int count = this.count; count > 0; --count) {
            if (buf.atChar(len) == '\n') {
                len++;
            } else {
                while (buf.atChar(len) != 0 && buf.atChar(len) != '\n') {
                    len++;
                }
            }
        }
        int num = len - cp;
        String killed = buf.substring(cp, cp + num);
        buf.delete(num);
        killRing.add(killed);
        return true;
    }

    public boolean backwardKillLine() {
        if (count < 0) {
            count = -count;
            return killLine();
        }
        if (buf.cursor() == 0) {
            return false;
        }
        int cp = buf.cursor();
        int beg = cp;
        for (int count = this.count; count > 0; --count) {
            if (beg == 0) {
                break;
            }
            if (buf.atChar(beg - 1) == '\n') {
                beg--;
            } else {
                while (beg > 0 && buf.atChar(beg - 1) != 0 && buf.atChar(beg - 1) != '\n') {
                    beg--;
                }
            }
        }
        int num = cp - beg;
        String killed = buf.substring(cp - beg, cp);
        buf.cursor(beg);
        buf.delete(num);
        killRing.add(killed);
        return true;
    }

    public boolean yank() {
        String yanked = killRing.yank();
        if (yanked == null) {
            return false;
        } else {
            putString(yanked);
            return true;
        }
    }

    public boolean yankPop() {
        if (!killRing.lastYank()) {
            return false;
        }
        String current = killRing.yank();
        if (current == null) {
            // This shouldn't happen.
            return false;
        }
        buf.backspace(current.length());
        String yanked = killRing.yankPop();
        if (yanked == null) {
            // This shouldn't happen.
            return false;
        }

        putString(yanked);
        return true;
    }

    /**
     * Clear the screen by issuing the ANSI "clear screen" code.
     */
    public boolean clearScreen() {
        if (console.puts(Capability.clear_screen)) {
            redrawLine();
        } else {
            println();
        }
        return true;
    }

    /**
     * Issue an audible keyboard bell.
     */
    public void beep() {
        BellType bell_preference = BellType.AUDIBLE;
        switch (getString(BELL_STYLE, "")) {
            case "none":
            case "off":
                bell_preference = BellType.NONE;
                break;
            case "audible":
                bell_preference = BellType.AUDIBLE;
                break;
            case "visible":
                bell_preference = BellType.VISIBLE;
                break;
            case "on":
                bell_preference = getBoolean(PREFER_VISIBLE_BELL, false)
                        ? BellType.VISIBLE : BellType.AUDIBLE;
                break;
        }
        if (bell_preference == BellType.VISIBLE) {
            if (console.puts(Capability.flash_screen)
                    || console.puts(Capability.bell)) {
                flush();
            }
        } else if (bell_preference == BellType.AUDIBLE) {
            if (console.puts(Capability.bell)) {
                flush();
            }
        }
    }

    /**
     * Paste the contents of the clipboard into the console buffer
     *
     * @return true if clipboard contents pasted
     */
    public boolean pasteFromClipboard() {
        try {
            Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            String result = (String) clipboard.getData(DataFlavor.stringFlavor);
            if (result != null) {
                putString(result);
                return true;
            }
        }
        catch (Exception e) {
            Log.error("Paste failed: ", e);
        }
        return false;
    }

    //
    // Helpers
    //

    /**
     * Checks to see if the specified character is a delimiter. We consider a
     * character a delimiter if it is anything but a letter or digit.
     *
     * @param c     The character to test
     * @return      True if it is a delimiter
     */
    protected boolean isDelimiter(int c) {
        return !Character.isLetterOrDigit(c);
    }

    /**
     * Checks to see if a character is a whitespace character. Currently
     * this delegates to {@link Character#isWhitespace(char)}, however
     * eventually it should be hooked up so that the definition of whitespace
     * can be configured, as readline does.
     *
     * @param c The character to check
     * @return true if the character is a whitespace
     */
    protected boolean isWhitespace(int c) {
        return Character.isWhitespace(c);
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public Object getVariable(String name) {
        return variables.get(name);
    }

    public void setVariable(String name, Object value) {
        variables.put(name, value);
    }

    public boolean isSet(Option option) {
        Boolean b = options.get(option);
        return b != null ? b : option.isDef();
    }

    public void setOpt(Option option) {
        options.put(option, Boolean.TRUE);
    }

    public void unsetOpt(Option option) {
        options.put(option, Boolean.FALSE);
    }

    String getString(String name, String def) {
        Object v = getVariable(name);
        return v != null ? v.toString() : def;
    }

    boolean getBoolean(String name, boolean def) {
        Object v = getVariable(name);
        if (v instanceof Boolean) {
            return (Boolean) v;
        } else if (v != null) {
            String s = v.toString();
            return s.isEmpty() || s.equalsIgnoreCase("on")
                    || s.equalsIgnoreCase("1") || s.equalsIgnoreCase("true");
        }
        return def;
    }

    int getInt(String name, int def) {
        int nb = def;
        Object v = getVariable(name);
        if (v instanceof Number) {
            return ((Number) v).intValue();
        } else if (v != null) {
            nb = 0;
            try {
                nb = Integer.parseInt(v.toString());
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return nb;
    }

    long getLong(String name, long def) {
        long nb = def;
        Object v = getVariable(name);
        if (v instanceof Number) {
            return ((Number) v).longValue();
        } else if (v != null) {
            nb = 0;
            try {
                nb = Long.parseLong(v.toString());
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return nb;
    }

    public Map<String, KeyMap> defaultKeyMaps() {
        Map<String, KeyMap> keyMaps = new HashMap<>();
        keyMaps.put(EMACS, emacs());
        keyMaps.put(VICMD, viCmd());
        keyMaps.put(VIINS, viInsertion());
        keyMaps.put(MENU_SELECT, menuSelect());
        if (getBoolean(BIND_TTY_SPECIAL_CHARS, true)) {
            Attributes attr = console.getAttributes();
            bindConsoleChars(keyMaps.get(EMACS), attr);
            bindConsoleChars(keyMaps.get(VIINS), attr);
        }
        // By default, link main to emacs
        keyMaps.put(MAIN, keyMaps.get(EMACS));
        return keyMaps;
    }

    public KeyMap emacs() {
        KeyMap emacs = new KeyMap();
        bind(emacs, SET_MARK,                   ctrl('@'));
        bind(emacs, BEGINNING_OF_LINE,          ctrl('A'));
        bind(emacs, BACKWARD_CHAR,              ctrl('B'));
        bind(emacs, INTERRUPT,                  ctrl('C'));
        bind(emacs, DELETE_CHAR_OR_LIST,        ctrl('D'));
        bind(emacs, END_OF_LINE,                ctrl('E'));
        bind(emacs, FORWARD_CHAR,               ctrl('F'));
        bind(emacs, SEND_BREAK,                 ctrl('G'));
        bind(emacs, BACKWARD_DELETE_CHAR,       ctrl('H'));
        bind(emacs, COMPLETE_WORD,              ctrl('I'));
        bind(emacs, ACCEPT_LINE,                ctrl('J'));
        bind(emacs, KILL_LINE,                  ctrl('K'));
        bind(emacs, CLEAR_SCREEN,               ctrl('L'));
        bind(emacs, ACCEPT_LINE,                ctrl('M'));
        bind(emacs, DOWN_LINE_OR_HISTORY,       ctrl('N'));
        bind(emacs, UP_LINE_OR_HISTORY,         ctrl('P'));
        bind(emacs, HISTORY_INCREMENTAL_SEARCH_BACKWARD,    ctrl('R'));
        bind(emacs, HISTORY_INCREMENTAL_SEARCH_FORWARD,     ctrl('S'));
        bind(emacs, TRANSPOSE_CHARS,            ctrl('T'));
        bind(emacs, KILL_WHOLE_LINE,            ctrl('U'));
        bind(emacs, QUOTED_INSERT,              ctrl('V'));
        bind(emacs, BACKWARD_KILL_WORD,         ctrl('W'));
        bind(emacs, YANK,                       ctrl('Y'));
        bind(emacs, CHARACTER_SEARCH,           ctrl(']'));
        bind(emacs, UNDO,                       ctrl('_'));
        bind(emacs, SELF_INSERT,                range(" -~"));
        bind(emacs, INSERT_CLOSE_PAREN,         ")");
        bind(emacs, INSERT_CLOSE_SQUARE,        "]");
        bind(emacs, INSERT_CLOSE_CURLY,         "}");
        bind(emacs, BACKWARD_DELETE_CHAR,       del());
        bind(emacs, VI_MATCH_BRACKET,           translate("^X^B"));
        bind(emacs, SEND_BREAK,                 translate("^X^G"));
        bind(emacs, OVERWRITE_MODE,             translate("^X^O"));
        bind(emacs, UNDO,                       translate("^X^U"));
        bind(emacs, VI_CMD_MODE,                translate("^X^V"));
        bind(emacs, EXCHANGE_POINT_AND_MARK,    translate("^X^X"));
        bind(emacs, START_KBD_MACRO,            translate("^X("));
        bind(emacs, END_KBD_MACRO,              translate("^X)"));
        bind(emacs, DO_LOWERCASE_VERSION,       translate("^XA-^XZ"));
        bind(emacs, CALL_LAST_KBD_MACRO,        translate("^Xe"));
        bind(emacs, KILL_LINE,                  translate("^X^?"));
        bind(emacs, SEND_BREAK,                 alt(ctrl('G')));
        bind(emacs, BACKWARD_KILL_WORD,         alt(ctrl('H')));
        bind(emacs, TAB_INSERT,                 alt(ctrl('I')));
        bind(emacs, SELF_INSERT_UNMETA,         alt(ctrl('M')));
        bind(emacs, REVERT_LINE,                alt(ctrl('R')));
        bind(emacs, YANK_NTH_ARG,               alt(ctrl('Y')));
        bind(emacs, COMPLETE_WORD,              alt(esc()));
        bind(emacs, CHARACTER_SEARCH_BACKWARD,  alt(ctrl(']')));
        bind(emacs, SET_MARK,                   alt(' '));
        bind(emacs, INSERT_COMMENT,             alt('#'));
        bind(emacs, TILDE_EXPAND,               alt('&'));
        bind(emacs, INSERT_COMPLETIONS,         alt('*'));
        bind(emacs, NEG_ARGUMENT,               alt('-'));
        bind(emacs, YANK_LAST_ARG,              alt('.'));
        bind(emacs, DIGIT_ARGUMENT,             range("\\E0-\\E9"));
        bind(emacs, BEGINNING_OF_HISTORY,       alt('<'));
        bind(emacs, POSSIBLE_COMPLETIONS,       alt('='));
        bind(emacs, END_OF_HISTORY,             alt('>'));
        bind(emacs, POSSIBLE_COMPLETIONS,       alt('?'));
        bind(emacs, DO_LOWERCASE_VERSION,       range("^[A-^[Z"));
        bind(emacs, DELETE_HORIZONTAL_SPACE,    alt('\\'));
        bind(emacs, YANK_LAST_ARG,              alt('_'));
        bind(emacs, BACKWARD_WORD,              alt('b'));
        bind(emacs, CAPITALIZE_WORD,            alt('c'));
        bind(emacs, KILL_WORD,                  alt('d'));
        bind(emacs, FORWARD_WORD,               alt('f'));
        bind(emacs, DOWN_CASE_WORD,             alt('l'));
        bind(emacs, HISTORY_SEARCH_FORWARD,     alt('n'));
        bind(emacs, HISTORY_SEARCH_BACKWARD,    alt('p'));
        bind(emacs, REVERT_LINE,                alt('r'));
        bind(emacs, TRANSPOSE_WORDS,            alt('t'));
        bind(emacs, UP_CASE_WORD,               alt('u'));
        bind(emacs, YANK_POP,                   alt('y'));
        bind(emacs, TILDE_EXPAND,               alt('~'));
        bind(emacs, BACKWARD_KILL_WORD,         alt(del()));
        bindArrowKeys(emacs);
        bind(emacs, FORWARD_WORD,               alt(key(Capability.key_right)));
        bind(emacs, BACKWARD_WORD,              alt(key(Capability.key_left)));
        return emacs;
    }

    public KeyMap viInsertion() {
        KeyMap viins = new KeyMap();
        bind(viins, SELF_INSERT,            range("^@-^_"));
        bind(viins, VI_EOF_MAYBE,           ctrl('D'));
        bind(viins, BACKWARD_DELETE_CHAR,   ctrl('H'));
        bind(viins, COMPLETE_WORD,          ctrl('I'));
        bind(viins, ACCEPT_LINE,            ctrl('J'));
        bind(viins, CLEAR_SCREEN,           ctrl('L'));
        bind(viins, ACCEPT_LINE,            ctrl('M'));
        bind(viins, MENU_COMPLETE,          ctrl('N'));
        bind(viins, REVERSE_MENU_COMPLETE,  ctrl('P'));
        bind(viins, HISTORY_INCREMENTAL_SEARCH_BACKWARD,    ctrl('R'));
        bind(viins, HISTORY_INCREMENTAL_SEARCH_FORWARD,     ctrl('S'));
        bind(viins, TRANSPOSE_CHARS,        ctrl('T'));
        bind(viins, KILL_WHOLE_LINE,        ctrl('U'));
        bind(viins, QUOTED_INSERT,          ctrl('V'));
        bind(viins, BACKWARD_KILL_WORD,     ctrl('W'));
        bind(viins, YANK,                   ctrl('Y'));
        bind(viins, VI_CMD_MODE,            ctrl('['));
        bind(viins, UNDO,                   ctrl('_'));
        bind(viins, HISTORY_INCREMENTAL_SEARCH_BACKWARD,    ctrl('X') + "r");
        bind(viins, HISTORY_INCREMENTAL_SEARCH_FORWARD,     ctrl('X') + "s");
        bind(viins, SELF_INSERT,            range(" -~"));
        bind(viins, INSERT_CLOSE_PAREN,     ")");
        bind(viins, INSERT_CLOSE_SQUARE,    "]");
        bind(viins, INSERT_CLOSE_CURLY,     "}");
        bind(viins, BACKWARD_DELETE_CHAR,   del());
        bindArrowKeys(viins);
        return viins;
    }

    public KeyMap viCmd() {
        KeyMap vicmd = new KeyMap();
        bind(vicmd, VI_EOF_MAYBE,               ctrl('D'));
        bind(vicmd, EMACS_EDITING_MODE,         ctrl('E'));
        bind(vicmd, SEND_BREAK,                 ctrl('G'));
        bind(vicmd, BACKWARD_CHAR,              ctrl('H'));
        bind(vicmd, VI_MOVE_ACCEPT_LINE,        ctrl('J'));
        bind(vicmd, KILL_LINE,                  ctrl('K'));
        bind(vicmd, CLEAR_SCREEN,               ctrl('L'));
        bind(vicmd, VI_MOVE_ACCEPT_LINE,        ctrl('M'));
        bind(vicmd, VI_DOWN_LINE_OR_HISTORY,    ctrl('N'));
        bind(vicmd, VI_UP_LINE_OR_HISTORY,      ctrl('P'));
        bind(vicmd, QUOTED_INSERT,              ctrl('Q'));
        bind(vicmd, HISTORY_INCREMENTAL_SEARCH_BACKWARD,    ctrl('R'));
        bind(vicmd, HISTORY_INCREMENTAL_SEARCH_FORWARD,     ctrl('S'));
        bind(vicmd, TRANSPOSE_CHARS,            ctrl('T'));
        bind(vicmd, KILL_WHOLE_LINE,            ctrl('U'));
        bind(vicmd, QUOTED_INSERT,              ctrl('V'));
        bind(vicmd, BACKWARD_KILL_WORD,         ctrl('W'));
        bind(vicmd, YANK,                       ctrl('Y'));
        bind(vicmd, HISTORY_INCREMENTAL_SEARCH_BACKWARD,    ctrl('X') + "r");
        bind(vicmd, HISTORY_INCREMENTAL_SEARCH_FORWARD,     ctrl('X') + "s");
        bind(vicmd, SEND_BREAK,                 alt(ctrl('G')));
        bind(vicmd, BACKWARD_KILL_WORD,         alt(ctrl('H')));
        bind(vicmd, TAB_INSERT,                 alt(ctrl('I')));
        bind(vicmd, SELF_INSERT_UNMETA,         alt(ctrl('M')));
        bind(vicmd, REVERT_LINE,                alt(ctrl('R')));
        bind(vicmd, YANK_NTH_ARG,               alt(ctrl('Y')));
        bind(vicmd, COMPLETE_WORD,              alt(esc()));
        bind(vicmd, CHARACTER_SEARCH_BACKWARD,  alt(ctrl(']')));
        bind(vicmd, SET_MARK,                   alt(' '));
        bind(vicmd, INSERT_COMMENT,             alt('#'));
        bind(vicmd, TILDE_EXPAND,               alt('&'));
        bind(vicmd, INSERT_COMPLETIONS,         alt('*'));
        bind(vicmd, DIGIT_ARGUMENT,             alt('-'));
        bind(vicmd, YANK_LAST_ARG,              alt('.'));
        bind(vicmd, BEGINNING_OF_HISTORY,       alt('<'));
        bind(vicmd, POSSIBLE_COMPLETIONS,       alt('='));
        bind(vicmd, END_OF_HISTORY,             alt('>'));
        bind(vicmd, POSSIBLE_COMPLETIONS,       alt('?'));
        bind(vicmd, DO_LOWERCASE_VERSION,       range("^[A-^[Z"));
        bind(vicmd, DELETE_HORIZONTAL_SPACE,    alt('\\'));
        bind(vicmd, YANK_LAST_ARG,              alt('_'));
        bind(vicmd, BACKWARD_WORD,              alt('b'));
        bind(vicmd, CAPITALIZE_WORD,            alt('c'));
        bind(vicmd, KILL_WORD,                  alt('d'));
        bind(vicmd, FORWARD_WORD,               alt('f'));
        bind(vicmd, DOWN_CASE_WORD,             alt('l'));
        bind(vicmd, HISTORY_SEARCH_FORWARD,     alt('n'));
        bind(vicmd, HISTORY_SEARCH_BACKWARD,    alt('p'));
        bind(vicmd, REVERT_LINE,                alt('r'));
        bind(vicmd, TRANSPOSE_WORDS,            alt('t'));
        bind(vicmd, UP_CASE_WORD,               alt('u'));
        bind(vicmd, YANK_POP,                   alt('y'));
        bind(vicmd, TILDE_EXPAND,               alt('~'));
        bind(vicmd, BACKWARD_KILL_WORD,         alt(del()));

        bind(vicmd, FORWARD_CHAR,           " ");
        bind(vicmd, VI_INSERT_COMMENT,      "#");
        bind(vicmd, END_OF_LINE,            "$");
        bind(vicmd, VI_MATCH_BRACKET,        "%");
        bind(vicmd, VI_TILDE_EXPAND,        "&");
        bind(vicmd, VI_COMPLETE,            "*");
        bind(vicmd, VI_DOWN_LINE_OR_HISTORY,"+");
        bind(vicmd, VI_CHAR_SEARCH,         ",");
        bind(vicmd, VI_UP_LINE_OR_HISTORY,  "-");
        bind(vicmd, VI_REDO,                ".");
        bind(vicmd, VI_SEARCH,              "/");
        bind(vicmd, VI_DIGIT_OR_BEGINNING_OF_LINE, "0");
        bind(vicmd, DIGIT_ARGUMENT,         range("1-9"));
        bind(vicmd, VI_CHAR_SEARCH,         ";");
        bind(vicmd, VI_COMPLETE,            "=");
        bind(vicmd, VI_SEARCH,              "?");
        bind(vicmd, VI_ADD_EOL,             "A");
        bind(vicmd, VI_BACKWARD_WORD,       "B");
        bind(vicmd, VI_CHANGE_TO_EOL,       "C");
        bind(vicmd, VI_DELETE_TO_EOL,       "D");
        bind(vicmd, VI_END_WORD,            "E");
        bind(vicmd, VI_CHAR_SEARCH,         "F");
        bind(vicmd, VI_FETCH_HISTORY,       "G");
        bind(vicmd, VI_INSERT_BOL,          "I");
        bind(vicmd, VI_SEARCH_AGAIN,        "N");
        bind(vicmd, VI_PUT,                 "P");
        bind(vicmd, VI_REPLACE,             "R");
        bind(vicmd, VI_KILL_WHOLE_LINE,     "S");
        bind(vicmd, VI_CHAR_SEARCH,         "T");
        bind(vicmd, REVERT_LINE,            "U");
        bind(vicmd, VI_NEXT_WORD,           "W");
        bind(vicmd, VI_RUBOUT,              "X");
        bind(vicmd, VI_YANK_TO,             "Y");
        bind(vicmd, VI_COMPLETE,            "\\");
        bind(vicmd, VI_FIRST_PRINT,         "^");
        bind(vicmd, VI_YANK_ARG,            "_");
        bind(vicmd, VI_GOTO_MARK,           "`");
        bind(vicmd, VI_ADD_NEXT,            "a");
        bind(vicmd, VI_PREV_WORD,           "b");
        bind(vicmd, VI_CHANGE_TO,           "c");
        bind(vicmd, VI_DELETE_TO,           "d");
        bind(vicmd, VI_END_WORD,            "e");
        bind(vicmd, VI_CHAR_SEARCH,         "f");
        bind(vicmd, BACKWARD_CHAR,          "h");
        bind(vicmd, VI_INSERT,              "i");
        bind(vicmd, DOWN_HISTORY,           "j");
        bind(vicmd, UP_HISTORY,             "k");
        bind(vicmd, FORWARD_CHAR,           "l");
        bind(vicmd, VI_SET_MARK,            "m");
        bind(vicmd, VI_SEARCH_AGAIN,        "n");
        bind(vicmd, VI_PUT,                 "p");
        bind(vicmd, VI_CHANGE_CHAR,         "r");
        bind(vicmd, VI_SUBST,               "s");
        bind(vicmd, VI_CHAR_SEARCH,         "t");
        bind(vicmd, UNDO,                   "u");
        bind(vicmd, VI_NEXT_WORD,           "w");
        bind(vicmd, VI_DELETE,              "x");
        bind(vicmd, VI_YANK_TO,             "y");
        bind(vicmd, VI_COLUMN,              "|");
        bind(vicmd, VI_CHANGE_CASE,         "~");
        bind(vicmd, VI_DELETE,              del());

        bindArrowKeys(vicmd);
        return vicmd;
    }

    public KeyMap menuSelect() {
        KeyMap menuselect = new KeyMap();
        bind(menuselect, MENU_COMPLETE,         "\t");
        bind(menuselect, REVERSE_MENU_COMPLETE, key(Capability.back_tab));
        bind(menuselect, ACCEPT_LINE,           "\r", "\n");
        bindArrowKeys(menuselect);
        return menuselect;
    }

    private void bind(KeyMap map, String widget, Iterable<? extends CharSequence> keySeqs) {
        map.bind(new Reference(widget), keySeqs);
    }

    private void bind(KeyMap map, String widget, CharSequence... keySeqs) {
        map.bind(new Reference(widget), keySeqs);
    }

    private String key(Capability capability) {
        return KeyMap.key(console, capability);
    }

    private void bindArrowKeys(KeyMap map) {
        bind(map, UP_LINE_OR_HISTORY,   key(Capability.key_up));
        bind(map, DOWN_LINE_OR_HISTORY, key(Capability.key_down));
        bind(map, BACKWARD_CHAR,        key(Capability.key_left));
        bind(map, FORWARD_CHAR,         key(Capability.key_right));
        bind(map, BEGINNING_OF_LINE,    key(Capability.key_home));
        bind(map, END_OF_LINE,          key(Capability.key_end));
        bind(map, DELETE_CHAR,          key(Capability.key_dc));
        bind(map, KILL_WHOLE_LINE,      key(Capability.key_dl));
        bind(map, OVERWRITE_MODE,       key(Capability.key_ic));
    }

    /**
     * Bind special chars defined by the console instead of
     * the default bindings
     */
    private void bindConsoleChars(KeyMap keyMap, Attributes attr) {
        if (attr != null) {
            rebind(keyMap, BACKWARD_DELETE_CHAR,
                    del(), (char) attr.getControlChar(ControlChar.VERASE));
            rebind(keyMap, BACKWARD_KILL_WORD,
                    ctrl('W'),  (char) attr.getControlChar(ControlChar.VWERASE));
            rebind(keyMap, KILL_WHOLE_LINE,
                    ctrl('U'), (char) attr.getControlChar(ControlChar.VKILL));
            rebind(keyMap, QUOTED_INSERT,
                    ctrl('V'), (char) attr.getControlChar(ControlChar.VLNEXT));
        }
    }

    private void rebind(KeyMap keyMap, String operation, String prevBinding, char newBinding) {
        if (newBinding > 0 && newBinding < 128) {
            Reference ref = new Reference(operation);
            bind(keyMap, SELF_INSERT, prevBinding);
            keyMap.bind(ref, Character.toString(newBinding));
        }
    }


}
