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
import java.io.StringWriter;
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
import org.jline.keymap.Widget;
import org.jline.keymap.WidgetRef;
import org.jline.reader.history.MemoryHistory;
import org.jline.utils.Ansi;
import org.jline.utils.Ansi.Attribute;
import org.jline.utils.Ansi.Color;
import org.jline.utils.AnsiHelper;
import org.jline.utils.Curses;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.Levenshtein;
import org.jline.utils.Log;
import org.jline.utils.Signals;
import org.jline.utils.WCWidth;

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

    protected static final int NO_BELL = 0;
    protected static final int AUDIBLE_BELL = 1;
    protected static final int VISIBLE_BELL = 2;


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
    protected State   state = State.NORMAL;

    protected Supplier<String> post;

    protected Map<String, Widget<ConsoleReaderImpl>> widgets;

    protected int count;
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

        widgets = builtinWidgets();
        bindingReader = new BindingReader(console,
                Operation.SELF_INSERT,
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

    public Map<String, Widget<ConsoleReaderImpl>> getWidgets() {
        return widgets;
    }

    public Buffer getCursorBuffer() {
        return buf;
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

            state = State.NORMAL;

            // Cache console size for the duration of the call to readLine()
            // It will eventually be updated with WINCH signals
            size.copy(console.getSize());

            display = new Display(console, false);
            display.setColumns(size.getColumns());
            display.setTabWidth(TAB_WIDTH);

            // Make sure we position the cursor on column 0
            rawPrint(Ansi.ansi().bg(Color.DEFAULT).fgBright(Color.BLACK).a("~").fg(Color.DEFAULT).toString());
            rawPrint(' ', size.getColumns() - 1);
            console.puts(Capability.carriage_return);
            rawPrint(' ');
            console.puts(Capability.carriage_return);

            setPrompt(prompt);
            setRightPrompt(rightPrompt);
            buf.clear();
            if (buffer != null) {
                buf.write(buffer);
            }
            undo.clear();

            // Draw initial prompt
            redrawLine();
            redisplay();

            while (true) {

                Object o = readBinding(getKeys());
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
                count = (repeatCount == 0) ? 1 : repeatCount;
                // Reset undo/redo flag
                isUndo = false;

                // Get executable widget
                Buffer copy = buf.copy();
                Widget<ConsoleReaderImpl> w = getWidget(o);
                if (w == null || !w.apply(this)) {
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

    protected void handleSignal(Signal signal) {
        if (signal == Signal.WINCH) {
            size.copy(console.getSize());
            display.setColumns(size.getColumns());
            redisplay();
        }
    }

    @SuppressWarnings("unchecked")
    protected Widget<ConsoleReaderImpl> getWidget(Object binding) {
        Widget<ConsoleReaderImpl> w = null;
        if (binding instanceof Widget) {
            w = (Widget<ConsoleReaderImpl>) binding;
        } else if (binding instanceof Macro) {
            String macro = ((Macro) binding).getSequence();
            w = r -> {
                bindingReader.runMacro(macro);
                return true;
            };
        } else if (binding instanceof Operation) {
            w = widgets.get(((Operation) binding).func());
        } else if (binding instanceof WidgetRef) {
            w = widgets.get(((WidgetRef) binding).name());
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

    /**
     * Erase the current line.
     */
    protected boolean unixLineDiscard() {
        if (buf.cursor() == 0) {
            return false;
        } else {
            StringBuilder killed = new StringBuilder();
            while (buf.cursor() > 0) {
                int c = buf.prevChar();
                if (c == 0) {
                    break;
                }
                killed.appendCodePoint(c);
                buf.backspace();
            }
            String copy = killed.reverse().toString();
            killRing.addBackwards(copy);
            return true;
        }
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
    protected Operation viDeleteChangeYankToRemap (Operation op) {
        switch (op) {
            case VI_EOF_MAYBE:
            case ABORT:
            case BACKWARD_CHAR:
            case FORWARD_CHAR:
            case END_OF_LINE:
            case VI_MATCH:
            case VI_BEGINNING_OF_LINE_OR_ARG_DIGIT:
            case VI_ARG_DIGIT:
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
                return Operation.VI_MOVEMENT_MODE;
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
        while (isDelimiter(buf.prevChar()) && (buf.move(-1) != 0));
        while (!isDelimiter(buf.prevChar()) && (buf.move(-1) != 0));
        return true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    protected boolean forwardWord() {
        while (isDelimiter(buf.currChar()) && (buf.move(1) != 0));
        while (!isDelimiter(buf.currChar()) && (buf.move(1) != 0));
        return true;
    }

    /**
     * Deletes to the beginning of the word that the cursor is sitting on.
     * If the cursor is on white-space, it deletes that and to the beginning
     * of the word before it.  If the user is not on a word or whitespace
     * it deletes up to the end of the previous word.
     */
    protected boolean unixWordRubout() {
        StringBuilder killed = new StringBuilder();

        for (int count = this.count; count > 0; --count) {
            if (buf.cursor() == 0) {
                return false;
            }

            while (isWhitespace(buf.prevChar())) {
                int c = buf.prevChar();
                if (c == 0) {
                    break;
                }

                killed.appendCodePoint(c);
                buf.backspace();
            }

            while (!isWhitespace(buf.prevChar())) {
                int c = buf.prevChar();
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
        doViMatch();
        flush();

        peekCharacter(BLINK_MATCHING_PAREN_TIMEOUT);

        buf.cursor(closePosition);
        return true;
    }

    protected boolean viMatch() {
        return doViMatch();
    }

    /**
     * Implements vi style bracket matching ("%" command). The matching
     * bracket for the current bracket type that you are sitting on is matched.
     * The logic works like so:
     * @return true if it worked, false if the cursor was not on a bracket
     *   character or if there was no matching bracket.
     */
    protected boolean doViMatch() {
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
        StringBuilder killed = new StringBuilder();
        int c;

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

        String copy = killed.reverse().toString();
        killRing.addBackwards(copy);
        return true;
    }

    protected boolean killWord() {
        StringBuilder killed = new StringBuilder();
        int c;

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

        String copy = killed.toString();
        killRing.add(copy);
        return true;
    }

    protected boolean capitalizeWord() {
        boolean first = true;
        int c;
        while (buf.cursor() < buf.length() && !isDelimiter(c = buf.currChar())) {
            buf.currChar(first ? Character.toUpperCase(c) : Character.toLowerCase(c));
            buf.move(1);
            first = false;
        }
        return true;
    }

    protected boolean upCaseWord() {
        int c;
        while (buf.cursor() < buf.length() && !isDelimiter(c = buf.currChar())) {
            buf.currChar(Character.toUpperCase(c));
            buf.move(1);
        }
        return true;
    }

    protected boolean downCaseWord() {
        int c;
        while (buf.cursor() < buf.length() && !isDelimiter(c = buf.currChar())) {
            buf.currChar(Character.toLowerCase(c));
            buf.move(1);
        }
        return true;
    }

    /**
     * Performs character transpose. The character prior to the cursor and the
     * character under the cursor are swapped and the cursor is advanced one
     * character unless you are already at the end of the line.
     */
    protected boolean transposeChars() {
        for (int count = this.count; count > 0; --count) {
            if (!buf.transpose()) {
                return false;
            }
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

    protected boolean abort() {
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
        if (o != null) {
            if (o != Operation.YANK_POP && o != Operation.YANK) {
                killRing.resetLastYank();
            }
            if (o != Operation.KILL_LINE && o != Operation.KILL_WHOLE_LINE
                    && o != Operation.BACKWARD_KILL_WORD && o != Operation.KILL_WORD
                    && o != Operation.UNIX_LINE_DISCARD && o != Operation.UNIX_WORD_RUBOUT) {
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

    protected boolean viBeginningOfLineOrArgDigit() {
        if (repeatCount > 0) {
            return viArgDigit();
        } else {
            return beginningOfLine();
        }
    }

    protected boolean viArgDigit() {
        repeatCount = (repeatCount * 10) + getLastBinding().charAt(0) - '0';
        isArgDigit = true;
        return true;
    }

    protected boolean viDeleteTo() {
        int cursorStart = buf.cursor();
        Object o = readBinding(getKeys());
        if (o instanceof Operation) {
            Operation op = viDeleteChangeYankToRemap((Operation) o);
            // This is a weird special case. In vi
            // "dd" deletes the current line. So if we
            // get a delete-to, followed by a delete-to,
            // we delete the line.
            if (op == Operation.VI_DELETE_TO) {
                killWholeLine();
            } else {
                viMoveMode = ViMoveMode.DELETE_TO;
                Widget<ConsoleReaderImpl> widget = widgets.get(op.func());
                if (widget != null && !widget.apply(this)) {
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
        Object o = readBinding(getKeys());
        if (o instanceof Operation) {
            Operation op = viDeleteChangeYankToRemap((Operation) o);
            // Similar to delete-to, a "yy" yanks the whole line.
            if (op == Operation.VI_YANK_TO) {
                yankBuffer = buf.toString();
                return true;
            } else {
                viMoveMode = ViMoveMode.YANK_TO;
                Widget<ConsoleReaderImpl> widget = widgets.get(op.func());
                if (widget != null && !widget.apply(this)) {
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
        Object o = readBinding(getKeys());
        if (o instanceof Operation) {
            Operation op = viDeleteChangeYankToRemap((Operation) o);
            // change whole line
            if (op == Operation.VI_CHANGE_TO) {
                killWholeLine();
            } else {
                viMoveMode = ViMoveMode.CHANGE_TO;
                Widget<ConsoleReaderImpl> widget = widgets.get(op.func());
                if (widget != null && !widget.apply(this)) {
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
        redisplay(false);
        println();
        flush();
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

    protected boolean forwardSearchHistory() {
        return doSearchHistory(false);
    }

    protected boolean reverseSearchHistory() {
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
                .codePoints().forEach(c -> terminators.bind(new String(Character.toChars(c)), Operation.ACCEPT_LINE));

        try {
            while (true) {
                Object o = readBinding(getKeys(), terminators);
                if (o instanceof Operation) {
                    switch (((Operation) o)) {
                        case ABORT:
                            buf.setBuffer(originalBuffer);
                            return true;

                        case REVERSE_SEARCH_HISTORY:
                            backward = true;
                            if (searchTerm.length() == 0) {
                                searchTerm.append(previousSearchTerm);
                            }
                            if (searchIndex > 0) {
                                searchIndex = searchBackwards(searchTerm.toString(), searchIndex, false);
                            }
                            break;

                        case FORWARD_SEARCH_HISTORY:
                            backward = false;
                            if (searchTerm.length() == 0) {
                                searchTerm.append(previousSearchTerm);
                            }
                            if (searchIndex > -1 && searchIndex < history.size() - 1) {
                                searchIndex = searchForwards(searchTerm.toString(), searchIndex, false);
                            }
                            break;

                        case BACKWARD_DELETE_CHAR:
                            if (searchTerm.length() > 0) {
                                searchTerm.deleteCharAt(searchTerm.length() - 1);
                                if (backward) {
                                    searchIndex = searchBackwards(searchTerm.toString(), history.index(), false);
                                } else {
                                    searchIndex = searchForwards(searchTerm.toString(), history.index(), false);
                                }
                            }
                            break;

                        case SELF_INSERT:
                            searchTerm.append(getLastBinding());
                            if (backward) {
                                searchIndex = searchBackwards(searchTerm.toString(), history.index(), false);
                            } else {
                                searchIndex = searchForwards(searchTerm.toString(), history.index(), false);
                            }
                            break;

                        default:
                            // Set buffer and cursor position to the found string.
                            if (searchIndex != -1) {
                                history.moveTo(searchIndex);
                            }
                            pushBackBinding();
                            return true;
                    }
                } else {
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
        getCursorBuffer().clear();
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
                rawPrintln(e.getMessage());
                flush();
            }
        }
        return true;
    }

    protected boolean selfInsert() {
        putString(getLastBinding());
        return true;
    }

    protected boolean selfInsertUnmeta() {
        if (getLastBinding().charAt(0) == ESCAPE) {
            String s = getLastBinding().substring(1);
            if ("\r".equals(s)) {
                s = "\n";
            }
            putString(s);
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

    protected boolean viPreviousHistory() {
        /*
         * According to bash/readline move through history
         * in "vi" mode will move the cursor to the
         * start of the line. If there is no previous
         * history, then the cursor doesn't move.
         */
        return moveHistory(false, count) && beginningOfLine();
    }

    protected boolean nextHistory() {
        return moveHistory(true);

    }

    protected boolean viNextHistory() {
        /*
         * According to bash/readline move through history
         * in "vi" mode will move the cursor to the
         * start of the line. If there is no next history,
         * then the cursor doesn't move.
         */
        return moveHistory(true, count) && beginningOfLine();
    }

    protected boolean upLineOrHistory() {
        String str = buf.toString();
        if (str.contains("\n")) {
            return buf.up();
        } else {
            return historySearchBackward();
        }
    }

    protected boolean downLineOrHistory() {
        String str = buf.toString();
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

    protected boolean viMovementMode() {
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

    protected boolean viInsertionMode() {
        return setKeyMap(VIINS);
    }

    protected boolean  viAppendMode() {
        buf.move(1);
        return setKeyMap(VIINS);
    }

    protected boolean viAppendEol() {
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
        putString(new String(Character.toChars(c)));
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

    protected boolean viInsertBeg() {
        return beginningOfLine() && setKeyMap(VIINS);
    }

    protected boolean backwardDeleteChar() {
        return buf.backspace();
    }

    protected boolean viEditingMode() {
        return setKeyMap(VIINS);
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
        while (buf.prevChar() != '\n' && buf.move(-1) == -1);
        return true;
    }

    protected boolean endOfLine() {
        while (buf.currChar() != '\n' && buf.move(1) == 1);
        return true;
    }

    protected boolean deleteChar() {
        return buf.delete();
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

    protected Map<String, Widget<ConsoleReaderImpl>> builtinWidgets() {
        Map<String, Widget<ConsoleReaderImpl>> widgets = new HashMap<>();
        widgets.put(Operation.ABORT.func(), ConsoleReaderImpl::abort);
        widgets.put(Operation.ACCEPT_LINE.func(), ConsoleReaderImpl::acceptLine);
        widgets.put(Operation.BACKWARD_CHAR.func(), ConsoleReaderImpl::backwardChar);
        widgets.put(Operation.BACKWARD_DELETE_CHAR.func(), ConsoleReaderImpl::backwardDeleteChar);
        widgets.put(Operation.BACKWARD_KILL_WORD.func(), ConsoleReaderImpl::backwardKillWord);
        widgets.put(Operation.BACKWARD_WORD.func(), ConsoleReaderImpl::backwardWord);
        widgets.put(Operation.BEGINNING_OF_HISTORY.func(), ConsoleReaderImpl::beginningOfHistory);
        widgets.put(Operation.BEGINNING_OF_LINE.func(), ConsoleReaderImpl::beginningOfLine);
        widgets.put(Operation.CALL_LAST_KBD_MACRO.func(), ConsoleReaderImpl::callLastKbdMacro);
        widgets.put(Operation.CAPITALIZE_WORD.func(), ConsoleReaderImpl::capitalizeWord);
        widgets.put(Operation.CLEAR_SCREEN.func(), ConsoleReaderImpl::clearScreen);
        widgets.put(Operation.COMPLETE_PREFIX.func(), ConsoleReaderImpl::completePrefix);
        widgets.put(Operation.COMPLETE_WORD.func(), ConsoleReaderImpl::completeWord);
        widgets.put(Operation.DELETE_CHAR.func(), ConsoleReaderImpl::deleteChar);
        widgets.put(Operation.DELETE_CHAR_OR_LIST.func(), ConsoleReaderImpl::deleteCharOrList);
        widgets.put(Operation.DO_LOWERCASE_VERSION.func(), ConsoleReaderImpl::doLowercaseVersion);
        widgets.put(Operation.DOWN_LINE_OR_HISTORY.func(), ConsoleReaderImpl::downLineOrHistory);
        widgets.put(Operation.DOWNCASE_WORD.func(), ConsoleReaderImpl::downCaseWord);
        widgets.put(Operation.EMACS_EDITING_MODE.func(), ConsoleReaderImpl::emacsEditingMode);
        widgets.put(Operation.END_KBD_MACRO.func(), ConsoleReaderImpl::endKbdMacro);
        widgets.put(Operation.END_OF_HISTORY.func(), ConsoleReaderImpl::endOfHistory);
        widgets.put(Operation.END_OF_LINE.func(), ConsoleReaderImpl::endOfLine);
        widgets.put(Operation.EXIT_OR_DELETE_CHAR.func(), ConsoleReaderImpl::exitOrDeleteChar);
        widgets.put(Operation.FORWARD_CHAR.func(), ConsoleReaderImpl::forwardChar);
        widgets.put(Operation.FORWARD_SEARCH_HISTORY.func(), ConsoleReaderImpl::forwardSearchHistory);
        widgets.put(Operation.FORWARD_WORD.func(), ConsoleReaderImpl::forwardWord);
        widgets.put(Operation.HISTORY_SEARCH_BACKWARD.func(), ConsoleReaderImpl::historySearchBackward);
        widgets.put(Operation.HISTORY_SEARCH_FORWARD.func(), ConsoleReaderImpl::historySearchForward);
        widgets.put(Operation.INSERT_CLOSE_CURLY.func(), ConsoleReaderImpl::insertCloseCurly);
        widgets.put(Operation.INSERT_CLOSE_PAREN.func(), ConsoleReaderImpl::insertCloseParen);
        widgets.put(Operation.INSERT_CLOSE_SQUARE.func(), ConsoleReaderImpl::insertCloseSquare);
        widgets.put(Operation.INSERT_COMMENT.func(), ConsoleReaderImpl::insertComment);
        widgets.put(Operation.INTERRUPT.func(), ConsoleReaderImpl::interrupt);
        widgets.put(Operation.KILL_LINE.func(), ConsoleReaderImpl::killLine);
        widgets.put(Operation.KILL_WHOLE_LINE.func(), ConsoleReaderImpl::killWholeLine);
        widgets.put(Operation.KILL_WORD.func(), ConsoleReaderImpl::killWord);
        widgets.put(Operation.MENU_COMPLETE.func(), ConsoleReaderImpl::menuComplete);
        widgets.put(Operation.NEXT_HISTORY.func(), ConsoleReaderImpl::nextHistory);
        widgets.put(Operation.OVERWRITE_MODE.func(), ConsoleReaderImpl::overwriteMode);
        widgets.put(Operation.PASTE_FROM_CLIPBOARD.func(), ConsoleReaderImpl::pasteFromClipboard);
        widgets.put(Operation.POSSIBLE_COMPLETIONS.func(), ConsoleReaderImpl::listChoices);
        widgets.put(Operation.PREVIOUS_HISTORY.func(), ConsoleReaderImpl::previousHistory);
        widgets.put(Operation.QUIT.func(), ConsoleReaderImpl::quit);
        widgets.put(Operation.QUOTED_INSERT.func(), ConsoleReaderImpl::quotedInsert);
        widgets.put(Operation.REVERSE_SEARCH_HISTORY.func(), ConsoleReaderImpl::reverseSearchHistory);
        widgets.put(Operation.SELF_INSERT.func(), ConsoleReaderImpl::selfInsert);
        widgets.put(Operation.SELF_INSERT_UNMETA.func(), ConsoleReaderImpl::selfInsertUnmeta);
        widgets.put(Operation.START_KBD_MACRO.func(), ConsoleReaderImpl::startKbdMacro);
        widgets.put(Operation.TAB_INSERT.func(), ConsoleReaderImpl::tabInsert);
        widgets.put(Operation.TRANSPOSE_CHARS.func(), ConsoleReaderImpl::transposeChars);
        widgets.put(Operation.UNIX_LINE_DISCARD.func(), ConsoleReaderImpl::unixLineDiscard);
        widgets.put(Operation.UNIX_WORD_RUBOUT.func(), ConsoleReaderImpl::unixWordRubout);
        widgets.put(Operation.UPCASE_WORD.func(), ConsoleReaderImpl::upCaseWord);
        widgets.put(Operation.UP_LINE_OR_HISTORY.func(), ConsoleReaderImpl::upLineOrHistory);
        widgets.put(Operation.VI_ARG_DIGIT.func(), ConsoleReaderImpl::viArgDigit);
        widgets.put(Operation.VI_APPEND_EOL.func(), ConsoleReaderImpl::viAppendEol);
        widgets.put(Operation.VI_APPEND_MODE.func(), ConsoleReaderImpl::viAppendMode);
        widgets.put(Operation.VI_BEGINNING_OF_LINE_OR_ARG_DIGIT.func(), ConsoleReaderImpl::viBeginningOfLineOrArgDigit);
        widgets.put(Operation.VI_CHANGE_CASE.func(), ConsoleReaderImpl::viChangeCase);
        widgets.put(Operation.VI_CHANGE_CHAR.func(), ConsoleReaderImpl::viChangeChar);
        widgets.put(Operation.VI_CHANGE_TO.func(), ConsoleReaderImpl::viChangeTo);
        widgets.put(Operation.VI_CHANGE_TO_EOL.func(), ConsoleReaderImpl::viChangeToEol);
        widgets.put(Operation.VI_CHAR_SEARCH.func(), ConsoleReaderImpl::viCharSearch);
        widgets.put(Operation.VI_DELETE.func(), ConsoleReaderImpl::viDelete);
        widgets.put(Operation.VI_DELETE_TO.func(), ConsoleReaderImpl::viDeleteTo);
        widgets.put(Operation.VI_DELETE_TO_EOL.func(), ConsoleReaderImpl::viDeleteToEol);
        widgets.put(Operation.VI_EDITING_MODE.func(), ConsoleReaderImpl::viEditingMode);
        widgets.put(Operation.VI_END_WORD.func(), ConsoleReaderImpl::viEndWord);
        widgets.put(Operation.VI_EOF_MAYBE.func(), ConsoleReaderImpl::viEofMaybe);
        widgets.put(Operation.VI_FIRST_PRINT.func(), ConsoleReaderImpl::viFirstPrint);
        widgets.put(Operation.VI_INSERT_BEG.func(), ConsoleReaderImpl::viInsertBeg);
        widgets.put(Operation.VI_INSERT_COMMENT.func(), ConsoleReaderImpl::viInsertComment);
        widgets.put(Operation.VI_INSERTION_MODE.func(), ConsoleReaderImpl::viInsertionMode);
        widgets.put(Operation.VI_KILL_WHOLE_LINE.func(), ConsoleReaderImpl::viKillWholeLine);
        widgets.put(Operation.VI_MATCH.func(), ConsoleReaderImpl::viMatch);
        widgets.put(Operation.VI_MOVE_ACCEPT_LINE.func(), ConsoleReaderImpl::viMoveAcceptLine);
        widgets.put(Operation.VI_MOVEMENT_MODE.func(), ConsoleReaderImpl::viMovementMode);
        widgets.put(Operation.VI_NEXT_HISTORY.func(), ConsoleReaderImpl::viNextHistory);
        widgets.put(Operation.VI_NEXT_WORD.func(), ConsoleReaderImpl::viNextWord);
        widgets.put(Operation.VI_PREV_WORD.func(), ConsoleReaderImpl::viPreviousWord);
        widgets.put(Operation.VI_PREVIOUS_HISTORY.func(), ConsoleReaderImpl::viPreviousHistory);
        widgets.put(Operation.VI_PUT.func(), ConsoleReaderImpl::viPut);
        widgets.put(Operation.VI_RUBOUT.func(), ConsoleReaderImpl::viRubout);
        widgets.put(Operation.VI_SEARCH.func(), ConsoleReaderImpl::viSearch);
        widgets.put(Operation.VI_YANK_TO.func(), ConsoleReaderImpl::viYankTo);
        widgets.put(Operation.YANK.func(), ConsoleReaderImpl::yank);
        widgets.put(Operation.YANK_POP.func(), ConsoleReaderImpl::yankPop);
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
                Object op = readBinding(getKeys());
                if (op != null) {
                    String chars = getString("REMOVE_SUFFIX_CHARS", " \t\n;&|");
                    if (op == Operation.SELF_INSERT && chars.indexOf(getLastBinding().charAt(0)) >= 0
                            || op == Operation.ACCEPT_LINE) {
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
        Object operation = readBinding(getKeys(), keyMap);
        if (operation == Operation.MENU_COMPLETE) {
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
        Object operation;
        while ((operation = readBinding(getKeys(), keyMap)) != null) {
            if (operation == Operation.MENU_COMPLETE) {
                menuSupport.next();
            } else if (operation == Operation.REVERSE_MENU_COMPLETE) {
                menuSupport.previous();
            } else if (operation == Operation.UP_LINE_OR_HISTORY) {
                menuSupport.up();
            } else if (operation == Operation.DOWN_LINE_OR_HISTORY) {
                menuSupport.down();
            } else if (operation == Operation.FORWARD_CHAR) {
                menuSupport.right();
            } else if (operation == Operation.BACKWARD_CHAR) {
                menuSupport.left();
            } else if (operation == Operation.CLEAR_SCREEN) {
                clearScreen();
            } else {
                Candidate completion = menuSupport.completion();
                if (completion.suffix() != null) {
                    String chars = getString("REMOVE_SUFFIX_CHARS", " \t\n;&|");
                    if (operation == Operation.SELF_INSERT && chars.indexOf(getLastBinding().charAt(0)) >= 0
                            || operation == Operation.ACCEPT_LINE
                            || operation == Operation.BACKWARD_DELETE_CHAR) {
                        buf.backspace(completion.suffix().length());
                    }
                }
                if (completion.complete()
                        && getLastBinding().charAt(0) != ' '
                        && (operation != Operation.SELF_INSERT || getLastBinding().charAt(0) != ' ')) {
                    buf.write(' ');
                }
                if (operation != Operation.ACCEPT_LINE && operation != Operation.BACKWARD_DELETE_CHAR) {
                    pushBackBinding(true);
                }
                post = null;
                console.puts(Capability.keypad_local);
                return true;
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
            rawPrint(getAppName() + ": do you wish to see to see all " + possible.size()
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
                    rawPrintln(postResult.post);
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
     * Output a platform-dependant newline.
     */
    void println() {
        console.puts(Capability.carriage_return);
        rawPrint('\n');
        redrawLine();
    }

    /**
     * Raw output printing
     */
    void rawPrint(int c) {
        console.writer().write(c);
    }

    void rawPrint(String str) {
        console.writer().write(str);
    }

    void rawPrint(char c, int num) {
        for (int i = 0; i < num; i++) {
            rawPrint(c);
        }
    }

    void rawPrintln(String s) {
        rawPrint(s);
        println();
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
        return beginningOfLine() && killLine();
    }

    /**
     * Kill the buffer ahead of the current cursor position.
     *
     * @return true if successful
     */
    public boolean killLine() {
        int cp = buf.cursor();
        int len = buf.length();
        int num = len - cp;
        String killed = buf.substring(cp, cp + num);
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
        int bell_preference = AUDIBLE_BELL;
        switch (getString(BELL_STYLE, "")) {
            case "none":
            case "off":
                bell_preference = NO_BELL;
                break;
            case "audible":
                bell_preference = AUDIBLE_BELL;
                break;
            case "visible":
                bell_preference = VISIBLE_BELL;
                break;
            case "on":
                bell_preference = getBoolean(PREFER_VISIBLE_BELL, false) ? VISIBLE_BELL : AUDIBLE_BELL;
                break;
        }
        if (bell_preference == VISIBLE_BELL) {
            if (console.puts(Capability.flash_screen)
                    || console.puts(Capability.bell)) {
                flush();
            }
        } else if (bell_preference == AUDIBLE_BELL) {
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

    /**
     * Adding a triggered Action allows to give another curse of action if a character passed the pre-processing.
     * <p/>
     * Say you want to close the application if the user enter q.
     * addTriggerAction('q', new ActionListener(){ System.exit(0); }); would do the trick.
     *
     * TODO: deprecate
     */
    public void addTriggeredAction(final char c, final Widget<ConsoleReaderImpl> widget) {
        getKeys().bind(Character.toString(c), widget);
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

    public static final String VICMD = "vicmd";
    public static final String VIINS = "viins";
    public static final String MAIN = "main";
    public static final String EMACS = "emacs";
    public static final String MENU_SELECT = "menuselect";

    /**
     * Bind special chars defined by the console instead of
     * the default bindings
     */
    protected static void bindConsoleChars(KeyMap keyMap, Attributes attr) {
        if (attr != null) {
            rebind(keyMap, Operation.BACKWARD_DELETE_CHAR,
                           /* C-? */ (char) 127, (char) attr.getControlChar(ControlChar.VERASE));
            rebind(keyMap, Operation.BACKWARD_KILL_WORD,
                           /* C-W */ (char) 23,  (char) attr.getControlChar(ControlChar.VWERASE));
            rebind(keyMap, Operation.UNIX_LINE_DISCARD,
                           /* C-U */ (char) 21, (char) attr.getControlChar(ControlChar.VKILL));
            rebind(keyMap, Operation.QUOTED_INSERT,
                           /* C-V */ (char) 22, (char) attr.getControlChar(ControlChar.VLNEXT));
        }
    }

    protected static void rebind(KeyMap keyMap, Operation operation, char prevBinding, char newBinding) {
        if (prevBinding > 0 && prevBinding < 255) {
            if (keyMap.getBound("" + prevBinding) == operation) {
                keyMap.bind("" + prevBinding, Operation.SELF_INSERT);
                if (newBinding > 0 && newBinding < 255) {
                    keyMap.bind("" + newBinding, operation);
                }
            }
        }
    }

    protected void bindCapability(KeyMap keyMap, Capability cap, Operation operation) {
        String seq = getSequence(cap);
        if (seq != null) {
            keyMap.bindIfNotBound(seq, operation);
        }
    }

    private String getSequence(Capability cap) {
        String str = console.getStringCapability(cap);
        if (str != null) {
            StringWriter sw = new StringWriter();
            try {
                Curses.tputs(sw, str);
            } catch (IOException e) {
                throw new IOError(e);
            }
            return sw.toString();
        }
        return null;
    }


    public void bindArrowKeys(KeyMap map) {

        // MS-DOS
        map.bind( "\033[0A", Operation.PREVIOUS_HISTORY );
        map.bind( "\033[0B", Operation.BACKWARD_CHAR );
        map.bind( "\033[0C", Operation.FORWARD_CHAR );
        map.bind( "\033[0D", Operation.NEXT_HISTORY );

        // Windows
        map.bind( "\340\000", Operation.KILL_WHOLE_LINE );
        map.bind( "\340\107", Operation.BEGINNING_OF_LINE );
        map.bind( "\340\110", Operation.PREVIOUS_HISTORY );
        map.bind( "\340\111", Operation.BEGINNING_OF_HISTORY );
        map.bind( "\340\113", Operation.BACKWARD_CHAR );
        map.bind( "\340\115", Operation.FORWARD_CHAR );
        map.bind( "\340\117", Operation.END_OF_LINE );
        map.bind( "\340\120", Operation.NEXT_HISTORY );
        map.bind( "\340\121", Operation.END_OF_HISTORY );
        map.bind( "\340\122", Operation.OVERWRITE_MODE );
        map.bind( "\340\123", Operation.DELETE_CHAR );

        map.bind( "\000\107", Operation.BEGINNING_OF_LINE );
        map.bind( "\000\110", Operation.PREVIOUS_HISTORY );
        map.bind( "\000\111", Operation.BEGINNING_OF_HISTORY );
        map.bind( "\000\110", Operation.PREVIOUS_HISTORY );
        map.bind( "\000\113", Operation.BACKWARD_CHAR );
        map.bind( "\000\115", Operation.FORWARD_CHAR );
        map.bind( "\000\117", Operation.END_OF_LINE );
        map.bind( "\000\120", Operation.NEXT_HISTORY );
        map.bind( "\000\121", Operation.END_OF_HISTORY );
        map.bind( "\000\122", Operation.OVERWRITE_MODE );
        map.bind( "\000\123", Operation.DELETE_CHAR );

        map.bind( "\033[A", Operation.PREVIOUS_HISTORY );
        map.bind( "\033[B", Operation.NEXT_HISTORY );
        map.bind( "\033[C", Operation.FORWARD_CHAR );
        map.bind( "\033[D", Operation.BACKWARD_CHAR );
        map.bind( "\033[H", Operation.BEGINNING_OF_LINE );
        map.bind( "\033[F", Operation.END_OF_LINE );

        map.bind( "\033OA", Operation.PREVIOUS_HISTORY );
        map.bind( "\033OB", Operation.NEXT_HISTORY );
        map.bind( "\033OC", Operation.FORWARD_CHAR );
        map.bind( "\033OD", Operation.BACKWARD_CHAR );
        map.bind( "\033OH", Operation.BEGINNING_OF_LINE );
        map.bind( "\033OF", Operation.END_OF_LINE );

        map.bind( "\033[1~", Operation.BEGINNING_OF_LINE);
        map.bind( "\033[4~", Operation.END_OF_LINE);
        map.bind( "\033[3~", Operation.DELETE_CHAR);

        // MINGW32
        map.bind( "\0340H", Operation.PREVIOUS_HISTORY );
        map.bind( "\0340P", Operation.NEXT_HISTORY );
        map.bind( "\0340M", Operation.FORWARD_CHAR );
        map.bind( "\0340K", Operation.BACKWARD_CHAR );
    }

    public Map<String, KeyMap> defaultKeyMaps() {
        Map<String, KeyMap> keyMaps = new HashMap<>();

        KeyMap emacs = emacs();
        bindArrowKeys(emacs);
        emacs.bind("\033\033[C", Operation.FORWARD_WORD);
        emacs.bind("\033\033[D", Operation.BACKWARD_WORD);
        keyMaps.put(EMACS, emacs);

        KeyMap viCmd = viMovement();
        bindArrowKeys(viCmd);
        keyMaps.put(VICMD, viCmd);

        KeyMap viIns = viInsertion();
        bindArrowKeys(viIns);
        keyMaps.put(VIINS, viIns);

        KeyMap menuSelect = menuSelect();
        bindArrowKeys(menuSelect);
        keyMaps.put(MENU_SELECT, menuSelect);

        keyMaps.put(MAIN, emacs);

        if (getBoolean(BIND_TTY_SPECIAL_CHARS, true)) {
            Attributes attr = console.getAttributes();
            bindConsoleChars(emacs, attr);
            bindConsoleChars(viIns, attr);
        }
        return keyMaps;
    }

    public static KeyMap emacs() {
        Binding[] map = new Binding[KeyMap.KEYMAP_LENGTH];
        Binding[] ctrl = new Binding[]{
                // Control keys.
                Operation.SET_MARK,                 /* Control-@ */
                Operation.BEGINNING_OF_LINE,        /* Control-A */
                Operation.BACKWARD_CHAR,            /* Control-B */
                Operation.INTERRUPT,                /* Control-C */
                Operation.DELETE_CHAR_OR_LIST,      /* Control-D */
                Operation.END_OF_LINE,              /* Control-E */
                Operation.FORWARD_CHAR,             /* Control-F */
                Operation.ABORT,                    /* Control-G */
                Operation.BACKWARD_DELETE_CHAR,     /* Control-H */
                Operation.COMPLETE_WORD,            /* Control-I */
                Operation.ACCEPT_LINE,              /* Control-J */
                Operation.KILL_LINE,                /* Control-K */
                Operation.CLEAR_SCREEN,             /* Control-L */
                Operation.ACCEPT_LINE,              /* Control-M */
                Operation.NEXT_HISTORY,             /* Control-N */
                null,                               /* Control-O */
                Operation.PREVIOUS_HISTORY,         /* Control-P */
                Operation.QUOTED_INSERT,            /* Control-Q */
                Operation.REVERSE_SEARCH_HISTORY,   /* Control-R */
                Operation.FORWARD_SEARCH_HISTORY,   /* Control-S */
                Operation.TRANSPOSE_CHARS,          /* Control-T */
                Operation.UNIX_LINE_DISCARD,        /* Control-U */
                Operation.QUOTED_INSERT,            /* Control-V */
                Operation.UNIX_WORD_RUBOUT,         /* Control-W */
                emacsCtrlX(),                       /* Control-X */
                Operation.YANK,                     /* Control-Y */
                null,                               /* Control-Z */
                emacsMeta(),                        /* Control-[ */
                null,                               /* Control-\ */
                Operation.CHARACTER_SEARCH,         /* Control-] */
                null,                               /* Control-^ */
                Operation.UNDO,                     /* Control-_ */
        };
        System.arraycopy(ctrl, 0, map, 0, ctrl.length);
        for (int i = 32; i < 256; i++) {
            map[i] = Operation.SELF_INSERT;
        }
        map[DELETE] = Operation.BACKWARD_DELETE_CHAR;
        return new KeyMap(map);
    }

    public static final char CTRL_D = (char) 4;
    public static final char CTRL_G = (char) 7;
    public static final char CTRL_H = (char) 8;
    public static final char CTRL_I = (char) 9;
    public static final char CTRL_J = (char) 10;
    public static final char CTRL_M = (char) 13;
    public static final char CTRL_R = (char) 18;
    public static final char CTRL_S = (char) 19;
    public static final char CTRL_U = (char) 21;
    public static final char CTRL_X = (char) 24;
    public static final char CTRL_Y = (char) 25;
    public static final char ESCAPE = (char) 27; /* Ctrl-[ */
    public static final char CTRL_OB = (char) 27; /* Ctrl-[ */
    public static final char CTRL_CB = (char) 29; /* Ctrl-] */

    public static final int DELETE = (char) 127;

    public static KeyMap emacsCtrlX() {
        Binding[] map = new Binding[KeyMap.KEYMAP_LENGTH];
        map[CTRL_G] = Operation.ABORT;
        map[CTRL_U] = Operation.UNDO;
        map[CTRL_X] = Operation.EXCHANGE_POINT_AND_MARK;
        map['('] = Operation.START_KBD_MACRO;
        map[')'] = Operation.END_KBD_MACRO;
        for (int i = 'A'; i <= 'Z'; i++) {
            map[i] = Operation.DO_LOWERCASE_VERSION;
        }
        map['e'] = Operation.CALL_LAST_KBD_MACRO;
        map[DELETE] = Operation.KILL_LINE;
        return new KeyMap(map);
    }

    public static KeyMap emacsMeta() {
        Binding[] map = new Binding[KeyMap.KEYMAP_LENGTH];
        map[CTRL_G] = Operation.ABORT;
        map[CTRL_H] = Operation.BACKWARD_KILL_WORD;
        map[CTRL_I] = Operation.TAB_INSERT;
        map[CTRL_J] = Operation.VI_EDITING_MODE;
        map[CTRL_M] = Operation.SELF_INSERT_UNMETA;
        map[CTRL_R] = Operation.REVERT_LINE;
        map[CTRL_Y] = Operation.YANK_NTH_ARG;
        map[CTRL_OB] = Operation.COMPLETE_WORD;
        map[CTRL_CB] = Operation.CHARACTER_SEARCH_BACKWARD;
        map[' '] = Operation.SET_MARK;
        map['#'] = Operation.INSERT_COMMENT;
        map['&'] = Operation.TILDE_EXPAND;
        map['*'] = Operation.INSERT_COMPLETIONS;
        map['-'] = Operation.DIGIT_ARGUMENT;
        map['.'] = Operation.YANK_LAST_ARG;
        map['<'] = Operation.BEGINNING_OF_HISTORY;
        map['='] = Operation.POSSIBLE_COMPLETIONS;
        map['>'] = Operation.END_OF_HISTORY;
        map['?'] = Operation.POSSIBLE_COMPLETIONS;
        for (int i = 'A'; i <= 'Z'; i++) {
            map[i] = Operation.DO_LOWERCASE_VERSION;
        }
        map['\\'] = Operation.DELETE_HORIZONTAL_SPACE;
        map['_'] = Operation.YANK_LAST_ARG;
        map['b'] = Operation.BACKWARD_WORD;
        map['c'] = Operation.CAPITALIZE_WORD;
        map['d'] = Operation.KILL_WORD;
        map['f'] = Operation.FORWARD_WORD;
        map['l'] = Operation.DOWNCASE_WORD;
        map['p'] = Operation.NON_INCREMENTAL_REVERSE_SEARCH_HISTORY;
        map['r'] = Operation.REVERT_LINE;
        map['t'] = Operation.TRANSPOSE_WORDS;
        map['u'] = Operation.UPCASE_WORD;
        map['y'] = Operation.YANK_POP;
        map['~'] = Operation.TILDE_EXPAND;
        map[DELETE] = Operation.BACKWARD_KILL_WORD;
        return new KeyMap(map);
    }

    public static KeyMap viInsertion() {
        Binding[] map = new Binding[KeyMap.KEYMAP_LENGTH];
        Binding[] ctrl = new Binding[]{
                // Control keys.
                null,                               /* Control-@ */
                Operation.SELF_INSERT,              /* Control-A */
                Operation.SELF_INSERT,              /* Control-B */
                Operation.SELF_INSERT,              /* Control-C */
                Operation.VI_EOF_MAYBE,             /* Control-D */
                Operation.SELF_INSERT,              /* Control-E */
                Operation.SELF_INSERT,              /* Control-F */
                Operation.SELF_INSERT,              /* Control-G */
                Operation.BACKWARD_DELETE_CHAR,     /* Control-H */
                Operation.COMPLETE_WORD,            /* Control-I */
                Operation.ACCEPT_LINE,              /* Control-J */
                Operation.SELF_INSERT,              /* Control-K */
                Operation.SELF_INSERT,              /* Control-L */
                Operation.ACCEPT_LINE,              /* Control-M */
                Operation.MENU_COMPLETE,            /* Control-N */
                Operation.SELF_INSERT,              /* Control-O */
                Operation.REVERSE_MENU_COMPLETE,    /* Control-P */
                Operation.SELF_INSERT,              /* Control-Q */
                Operation.REVERSE_SEARCH_HISTORY,   /* Control-R */
                Operation.FORWARD_SEARCH_HISTORY,   /* Control-S */
                Operation.TRANSPOSE_CHARS,          /* Control-T */
                Operation.UNIX_LINE_DISCARD,        /* Control-U */
                Operation.QUOTED_INSERT,            /* Control-V */
                Operation.UNIX_WORD_RUBOUT,         /* Control-W */
                Operation.SELF_INSERT,              /* Control-X */
                Operation.YANK,                     /* Control-Y */
                Operation.SELF_INSERT,              /* Control-Z */
                Operation.VI_MOVEMENT_MODE,         /* Control-[ */
                Operation.SELF_INSERT,              /* Control-\ */
                Operation.SELF_INSERT,              /* Control-] */
                Operation.SELF_INSERT,              /* Control-^ */
                Operation.UNDO,                     /* Control-_ */
        };
        System.arraycopy(ctrl, 0, map, 0, ctrl.length);
        for (int i = 32; i < 256; i++) {
            map[i] = Operation.SELF_INSERT;
        }
        map[DELETE] = Operation.BACKWARD_DELETE_CHAR;
        return new KeyMap(map);
    }

    public static KeyMap viMovement() {
        Binding[] map = new Binding[KeyMap.KEYMAP_LENGTH];
        Binding[] low = new Binding[]{
                // Control keys.
                null,                               /* Control-@ */
                null,                               /* Control-A */
                null,                               /* Control-B */
                Operation.INTERRUPT,                /* Control-C */
                        /* 
                         * ^D is supposed to move down half a screen. In bash
                         * appears to be ignored.
                         */
                Operation.VI_EOF_MAYBE,             /* Control-D */
                Operation.EMACS_EDITING_MODE,       /* Control-E */
                null,                               /* Control-F */
                Operation.ABORT,                    /* Control-G */
                Operation.BACKWARD_CHAR,            /* Control-H */
                null,                               /* Control-I */
                Operation.VI_MOVE_ACCEPT_LINE,      /* Control-J */
                Operation.KILL_LINE,                /* Control-K */
                Operation.CLEAR_SCREEN,             /* Control-L */
                Operation.VI_MOVE_ACCEPT_LINE,      /* Control-M */
                Operation.VI_NEXT_HISTORY,          /* Control-N */
                null,                               /* Control-O */
                Operation.VI_PREVIOUS_HISTORY,      /* Control-P */
                        /*
                         * My testing with readline is the ^Q is ignored. 
                         * Maybe this should be null?
                         */
                Operation.QUOTED_INSERT,            /* Control-Q */
                        
                        /*
                         * TODO - Very broken.  While in forward/reverse
                         * history search the VI keyset should go out the
                         * window and we need to enter a very simple keymap.
                         */
                Operation.REVERSE_SEARCH_HISTORY,   /* Control-R */
                        /* TODO */
                Operation.FORWARD_SEARCH_HISTORY,   /* Control-S */
                Operation.TRANSPOSE_CHARS,          /* Control-T */
                Operation.UNIX_LINE_DISCARD,        /* Control-U */
                        /* TODO */
                Operation.QUOTED_INSERT,            /* Control-V */
                Operation.UNIX_WORD_RUBOUT,         /* Control-W */
                null,                               /* Control-X */
                        /* TODO */
                Operation.YANK,                     /* Control-Y */
                null,                               /* Control-Z */
                emacsMeta(),                        /* Control-[ */
                null,                               /* Control-\ */
                        /* TODO */
                Operation.CHARACTER_SEARCH,         /* Control-] */
                null,                               /* Control-^ */
                        /* TODO */
                Operation.UNDO,                     /* Control-_ */
                Operation.FORWARD_CHAR,             /* SPACE */
                null,                               /* ! */
                null,                               /* " */
                Operation.VI_INSERT_COMMENT,        /* # */
                Operation.END_OF_LINE,              /* $ */
                Operation.VI_MATCH,                 /* % */
                Operation.VI_TILDE_EXPAND,          /* & */
                null,                               /* ' */
                null,                               /* ( */
                null,                               /* ) */
                        /* TODO */
                Operation.VI_COMPLETE,              /* * */
                Operation.VI_NEXT_HISTORY,          /* + */
                Operation.VI_CHAR_SEARCH,           /* , */
                Operation.VI_PREVIOUS_HISTORY,      /* - */
                        /* TODO */
                Operation.VI_REDO,                  /* . */
                Operation.VI_SEARCH,                /* / */
                Operation.VI_BEGINNING_OF_LINE_OR_ARG_DIGIT, /* 0 */
                Operation.VI_ARG_DIGIT,             /* 1 */
                Operation.VI_ARG_DIGIT,             /* 2 */
                Operation.VI_ARG_DIGIT,             /* 3 */
                Operation.VI_ARG_DIGIT,             /* 4 */
                Operation.VI_ARG_DIGIT,             /* 5 */
                Operation.VI_ARG_DIGIT,             /* 6 */
                Operation.VI_ARG_DIGIT,             /* 7 */
                Operation.VI_ARG_DIGIT,             /* 8 */
                Operation.VI_ARG_DIGIT,             /* 9 */
                null,                               /* : */
                Operation.VI_CHAR_SEARCH,           /* ; */
                null,                               /* < */
                Operation.VI_COMPLETE,              /* = */
                null,                               /* > */
                Operation.VI_SEARCH,                /* ? */
                null,                               /* @ */
                Operation.VI_APPEND_EOL,            /* A */
                Operation.VI_BACKWARD_WORD,         /* B */
                Operation.VI_CHANGE_TO_EOL,         /* C */
                Operation.VI_DELETE_TO_EOL,         /* D */
                Operation.VI_END_WORD,              /* E */
                Operation.VI_CHAR_SEARCH,           /* F */
                        /* I need to read up on what this does */
                Operation.VI_FETCH_HISTORY,         /* G */
                null,                               /* H */
                Operation.VI_INSERT_BEG,            /* I */
                null,                               /* J */
                null,                               /* K */
                null,                               /* L */
                null,                               /* M */
                Operation.VI_SEARCH_AGAIN,          /* N */
                null,                               /* O */
                Operation.VI_PUT,                   /* P */
                null,                               /* Q */
                        /* TODO */
                Operation.VI_REPLACE,               /* R */
                Operation.VI_KILL_WHOLE_LINE,       /* S */
                Operation.VI_CHAR_SEARCH,           /* T */
                        /* TODO */
                Operation.REVERT_LINE,              /* U */
                null,                               /* V */
                Operation.VI_NEXT_WORD,             /* W */
                Operation.VI_RUBOUT,                /* X */
                Operation.VI_YANK_TO,               /* Y */
                null,                               /* Z */
                null,                               /* [ */
                Operation.VI_COMPLETE,              /* \ */
                null,                               /* ] */
                Operation.VI_FIRST_PRINT,           /* ^ */
                Operation.VI_YANK_ARG,              /* _ */
                Operation.VI_GOTO_MARK,             /* ` */
                Operation.VI_APPEND_MODE,           /* a */
                Operation.VI_PREV_WORD,             /* b */
                Operation.VI_CHANGE_TO,             /* c */
                Operation.VI_DELETE_TO,             /* d */
                Operation.VI_END_WORD,              /* e */
                Operation.VI_CHAR_SEARCH,           /* f */
                null,                               /* g */
                Operation.BACKWARD_CHAR,            /* h */
                Operation.VI_INSERTION_MODE,        /* i */
                Operation.NEXT_HISTORY,             /* j */
                Operation.PREVIOUS_HISTORY,         /* k */
                Operation.FORWARD_CHAR,             /* l */
                Operation.VI_SET_MARK,              /* m */
                Operation.VI_SEARCH_AGAIN,          /* n */
                null,                               /* o */
                Operation.VI_PUT,                   /* p */
                null,                               /* q */
                Operation.VI_CHANGE_CHAR,           /* r */
                Operation.VI_SUBST,                 /* s */
                Operation.VI_CHAR_SEARCH,           /* t */
                Operation.UNDO,                     /* u */
                null,                               /* v */
                Operation.VI_NEXT_WORD,             /* w */
                Operation.VI_DELETE,                /* x */
                Operation.VI_YANK_TO,               /* y */
                null,                               /* z */
                null,                               /* { */
                Operation.VI_COLUMN,                /* | */
                null,                               /* } */
                Operation.VI_CHANGE_CASE,           /* ~ */
                Operation.VI_DELETE                 /* DEL */
        };
        System.arraycopy(low, 0, map, 0, low.length);
        for (int i = 128; i < 256; i++) {
            map[i] = null;
        }
        return new KeyMap(map);
    }

    public static KeyMap menuSelect() {
        KeyMap keyMap = new KeyMap();
        keyMap.bind("\t", Operation.MENU_COMPLETE);
        keyMap.bind("\033[Z", Operation.REVERSE_MENU_COMPLETE);
        keyMap.bind("\r", Operation.ACCEPT_LINE);
        keyMap.bind("\n", Operation.ACCEPT_LINE);
        return keyMap;
    }
}
