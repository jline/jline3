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
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Stack;

import org.jline.Completer;
import org.jline.Console;
import org.jline.Console.Signal;
import org.jline.Console.SignalHandler;
import org.jline.ConsoleReader;
import org.jline.History;
import org.jline.console.Attributes;
import org.jline.console.Attributes.ControlChar;
import org.jline.console.Size;
import org.jline.reader.history.MemoryHistory;
import org.jline.utils.Ansi;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.Log;
import org.jline.utils.NonBlockingReader;
import org.jline.utils.Signals;
import org.jline.utils.WCWidth;

import static org.jline.reader.KeyMap.ESCAPE;
import static org.jline.utils.NonBlockingReader.READ_EXPIRED;
import static org.jline.utils.Preconditions.checkNotNull;

/**
 * A reader for console applications. It supports custom tab-completion,
 * saveable command history, and command line editing.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 */
public class ConsoleReaderImpl implements ConsoleReader
{
    public static final char NULL_MASK = 0;

    public static final int TAB_WIDTH = 8;

    public static final long COPY_PASTE_DETECTION_TIMEOUT = 50l;
    public static final long BLINK_MATCHING_PAREN_TIMEOUT = 500l;

    private enum Messages
    {
        DISPLAY_CANDIDATES,
        DISPLAY_CANDIDATES_YES,
        DISPLAY_CANDIDATES_NO,
        DISPLAY_MORE;

        private static final
        ResourceBundle
                bundle =
                ResourceBundle.getBundle(CandidateListCompletionHandler.class.getName(), Locale.getDefault());

        public String format(final Object... args) {
            if (bundle == null)
                return "";
            else
                return String.format(bundle.getString(name()), args);
        }
    }

    private static final int NO_BELL = 0;
    private static final int AUDIBLE_BELL = 1;
    private static final int VISIBLE_BELL = 2;


    //
    // Constructor variables
    //

    /** The console to use */
    private final Console console;
    /** The inputrc url */
    private final URL inputrc;
    /** The application name, used when parsing the inputrc */
    private final String appName;
    /** The console keys mapping */
    private final ConsoleKeys consoleKeys;



    //
    // Configuration
    //
    private final Map<String, String> variables = new HashMap<>();

    //
    // State variables
    //

    private final CursorBuffer buf = new CursorBuffer();
    private boolean cursorOk;

    private Size size;

    private String prompt;
    private int    promptLen;

    private Character mask;

    private CursorBuffer originalBuffer = null;

    private StringBuffer searchTerm = null;

    private String previousSearchTerm = "";

    private int searchIndex = -1;


    // Reading buffers
    private final StringBuilder opBuffer = new StringBuilder();
    private final Stack<Character> pushBackChar = new Stack<Character>();


    /**
     * Last character searched for with a vi character search
     */
    private char  charSearchChar = 0;           // Character to search for
    private char  charSearchLastInvokeChar = 0; // Most recent invocation key
    private char  charSearchFirstInvokeChar = 0;// First character that invoked

    /**
     * The vi yank buffer
     */
    private String yankBuffer = "";

    private KillRing killRing = new KillRing();

    private boolean quotedInsert;

    private boolean recording;

    private String macro = "";

    /*
     * Current internal state of the line reader
     */
    private State   state = State.NORMAL;


    private History history = new MemoryHistory();

    private final List<Completer> completers = new LinkedList<Completer>();

    private CompletionHandler completionHandler = new CandidateListCompletionHandler();

    private Thread readLineThread;

    /**
     * Possible states in which the current readline operation may be in.
     */
    private enum State {
        /**
         * The user is just typing away
         */
        NORMAL,
        /**
         * In the middle of a emacs seach
         */
        SEARCH,
        FORWARD_SEARCH,
        /**
         * VI "yank-to" operation ("y" during move mode)
         */
        VI_YANK_TO,
        /**
         * VI "delete-to" operation ("d" during move mode)
         */
        VI_DELETE_TO,
        /**
         * VI "change-to" operation ("c" during move mode)
         */
        VI_CHANGE_TO
    }

    public ConsoleReaderImpl(Console console) throws IOException {
        this(console, null, null);
    }

    public ConsoleReaderImpl(Console console, String appName, URL inputrc) throws IOException {
        this(console, appName, inputrc, null);
    }

    public ConsoleReaderImpl(Console console, String appName, URL inputrc, Map<String, String> variables) {
        checkNotNull(console);
        this.console = console;
        if (appName == null) {
            appName = "JLine";
        }
        if (inputrc == null) {
            File f = new File(System.getProperty("user.home"), ".inputrc");
            if (!f.exists()) {
                f = new File("/etc/inputrc");
            }
            try {
                inputrc = f.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException();
            }
        }
        this.appName = appName;
        this.inputrc = inputrc;
        if (variables != null) {
            this.variables.putAll(variables);
        }
        this.consoleKeys = new ConsoleKeys(appName, inputrc);

        if (getBoolean(BIND_TTY_SPECIAL_CHARS, true)) {
            Attributes attr = console.getAttributes();
            bindConsoleChars(consoleKeys.getKeyMaps().get(KeyMap.EMACS), attr);
            bindConsoleChars(consoleKeys.getKeyMaps().get(KeyMap.VI_INSERT), attr);
        }
    }

    /**
     * Bind special chars defined by the console instead of
     * the default bindings
     */
    private static void bindConsoleChars(KeyMap keyMap, Attributes attr) {
        if (attr != null) {
            rebind(keyMap, Operation.BACKWARD_DELETE_CHAR,
                           /* C-? */ (char) 127, (char) attr.getControlChar(ControlChar.VERASE));
            rebind(keyMap, Operation.UNIX_WORD_RUBOUT,
                           /* C-W */ (char) 23,  (char) attr.getControlChar(ControlChar.VWERASE));
            rebind(keyMap, Operation.UNIX_LINE_DISCARD,
                           /* C-U */ (char) 21,  (char) attr.getControlChar(ControlChar.VKILL));
            rebind(keyMap, Operation.QUOTED_INSERT,
                           /* C-V */ (char) 22,  (char) attr.getControlChar(ControlChar.VLNEXT));
        }
    }

    private static void rebind(KeyMap keyMap, Operation operation, char prevBinding, char newBinding) {
        if (prevBinding > 0 && prevBinding < 255) {
            if (keyMap.getBound("" + prevBinding) == operation) {
                keyMap.bind("" + prevBinding, Operation.SELF_INSERT);
                if (newBinding > 0 && newBinding < 255) {
                    keyMap.bind("" + newBinding, operation);
                }
            }
        }
    }

    private void setupSigCont() {
        Signals.register("CONT", new Runnable() {
            public void run() {
//                console.init();
                // TODO: enter raw mode
                try {
                    drawLine();
                    flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public Console getConsole() {
        return console;
    }

    public String getAppName() {
        return appName;
    }

    public URL getInputrc() {
        return inputrc;
    }

    public KeyMap getKeys() {
        return consoleKeys.getKeys();
    }

    public CursorBuffer getCursorBuffer() {
        return buf;
    }

    private void setPrompt(final String prompt) {
        this.prompt = prompt;
        this.promptLen = ((prompt == null) ? 0 : wcwidth(Ansi.stripAnsi(lastLine(prompt)), 0));
    }

    /**
     * Erase the current line.
     *
     * @return false if we failed (e.g., the buffer was empty)
     */
    protected final boolean resetLine() throws IOException {
        if (buf.cursor == 0) {
            return false;
        }

        StringBuilder killed = new StringBuilder();

        while (buf.cursor > 0) {
            char c = buf.current();
            if (c == 0) {
                break;
            }

            killed.append(c);
            backspace();
        }

        String copy = killed.reverse().toString();
        killRing.addBackwards(copy);

        return true;
    }

    int wcwidth(CharSequence str, int pos) {
        return wcwidth(str, 0, str.length(), pos);
    }

    int wcwidth(CharSequence str, int start, int end, int pos) {
        int cur = pos;
        for (int i = start; i < end;) {
            int ucs;
            char c1 = str.charAt(i++);
            if (!Character.isHighSurrogate(c1) || i >= end) {
                ucs = c1;
            } else {
                char c2 = str.charAt(i);
                if (Character.isLowSurrogate(c2)) {
                    i++;
                    ucs = Character.toCodePoint(c1, c2);
                } else {
                    ucs = c1;
                }
            }
            cur += wcwidth(ucs, cur);
        }
        return cur - pos;
    }

    int wcwidth(int ucs, int pos) {
        if (ucs == '\t') {
            return nextTabStop(pos);
        } else if (ucs < 32) {
            return 2;
        } else  {
            int w = WCWidth.wcwidth(ucs);
            return w > 0 ? w : 0;
        }
    }

    int nextTabStop(int pos) {
        int tabWidth = TAB_WIDTH;
        int width = size.getColumns();
        int mod = (pos + tabWidth - 1) % tabWidth;
        int npos = pos + tabWidth - mod;
        return npos < width ? npos - pos : width - pos;
    }

    int getCursorPosition() {
        return promptLen + wcwidth(buf.buffer, 0, buf.cursor, promptLen);
    }

    /**
     * Returns the text after the last '\n'.
     * prompt is returned if no '\n' characters are present.
     * null is returned if prompt is null.
     */
    private String lastLine(String str) {
        if (str == null) return "";
        int last = str.lastIndexOf("\n");

        if (last >= 0) {
            return str.substring(last + 1, str.length());
        }

        return str;
    }

    /**
     * Move the cursor position to the specified absolute index.
     */
    public boolean setCursorPosition(final int position) throws IOException {
        if (position == buf.cursor) {
            return true;
        }

        return moveCursor(position - buf.cursor) != 0;
    }

    /**
     * Set the current buffer's content to the specified {@link String}. The
     * visual console will be modified to show the current buffer.
     *
     * @param buffer the new contents of the buffer.
     */
    private void setBuffer(final String buffer) throws IOException {
        // don't bother modifying it if it is unchanged
        if (buffer.equals(buf.buffer.toString())) {
            return;
        }

        // obtain the difference between the current buffer and the new one
        int sameIndex = 0;

        for (int i = 0, l1 = buffer.length(), l2 = buf.buffer.length(); (i < l1)
            && (i < l2); i++) {
            if (buffer.charAt(i) == buf.buffer.charAt(i)) {
                sameIndex++;
            }
            else {
                break;
            }
        }

        int diff = buf.cursor - sameIndex;
        if (diff < 0) { // we can't backspace here so try from the end of the buffer
            moveToEnd();
            diff = buf.buffer.length() - sameIndex;
        }

        backspace(diff); // go back for the differences
        killLine(); // clear to the end of the line
        buf.buffer.setLength(sameIndex); // the new length
        putString(buffer.substring(sameIndex)); // append the differences
    }

    private void setBuffer(final CharSequence buffer) throws IOException {
        setBuffer(String.valueOf(buffer));
    }

    private void setBufferKeepPos(final String buffer) throws IOException {
        int pos = buf.cursor;
        setBuffer(buffer);
        setCursorPosition(pos);
    }

    private void setBufferKeepPos(final CharSequence buffer) throws IOException {
        setBufferKeepPos(String.valueOf(buffer));
    }

    /**
     * Output put the prompt + the current buffer
     */
    public void drawLine() throws IOException {
        if (prompt != null) {
            rawPrint(prompt);
        }

        print(buf.buffer, 0, buf.length(), promptLen);

        if (buf.length() != buf.cursor) { // not at end of line
            back(buf.length() - buf.cursor - 1);
        }
        // force drawBuffer to check for weird wrap (after clear screen)
        drawBuffer();
    }

    /**
     * Clear the line and redraw it.
     */
    public void redrawLine() throws IOException {
        console.puts(Capability.carriage_return);
        drawLine();
    }

    /**
     * Clear the buffer and add its contents to the history.
     *
     * @return the former contents of the buffer.
     */
    final String finishBuffer() throws IOException { // FIXME: Package protected because used by tests
        String str = buf.buffer.toString();
        String historyLine = str;

        if (!getBoolean(DISABLE_EVENT_EXPANSION, false)) {
            try {
                str = expandEvents(str);
                // all post-expansion occurrences of '!' must have been escaped, so re-add escape to each
                historyLine = str.replace("!", "\\!");
                // only leading '^' results in expansion, so only re-add escape for that case
                historyLine = historyLine.replaceAll("^\\^", "\\\\^");
            } catch(IllegalArgumentException e) {
                Log.error("Could not expand event", e);
                beep();
                buf.clear();
                str = "";
            }
        }

        // we only add it to the history if the buffer is not empty
        // and if mask is null, since having a mask typically means
        // the string was a password. We clear the mask after this call
        if (str.length() > 0) {
            if (mask == null && !getBoolean(DISABLE_HISTORY, false)) {
                history.add(historyLine);
            }
            else {
                mask = null;
            }
        }

        history.moveToEnd();

        buf.buffer.setLength(0);
        buf.cursor = 0;

        return str;
    }

    /**
     * Expand event designator such as !!, !#, !3, etc...
     * See http://www.gnu.org/software/bash/manual/html_node/Event-Designators.html
     */
    @SuppressWarnings("fallthrough")
    protected String expandEvents(String str) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '\\':
                    // any '\!' should be considered an expansion escape, so skip expansion and strip the escape character
                    // a leading '\^' should be considered an expansion escape, so skip expansion and strip the escape character
                    // otherwise, add the escape
                    if (i + 1 < str.length()) {
                        char nextChar = str.charAt(i+1);
                        if (nextChar == '!' || (nextChar == '^' && i == 0)) {
                            c = nextChar;
                            i++;
                        }
                    }
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
                                rep = history.get(history.index() - 1).toString();
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
                                    rep = history.get(idx).toString();
                                }
                                break;
                            case '$':
                                if (history.size() == 0) {
                                    throw new IllegalArgumentException("!$: event not found");
                                }
                                String previous = history.get(history.index() - 1).toString().trim();
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
                                    rep = (history.get(history.index() - idx)).toString();
                                } else if (!neg && idx > history.index() - history.size() && idx <= history.index()) {
                                    rep = (history.get(idx - 1)).toString();
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
                                    rep = history.get(idx).toString();
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
                            String s = history.get(history.index() - 1).toString().replace(s1, s2);
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
        String result = sb.toString();
        if (!str.equals(result)) {
            print(result);
            println();
            flush();
        }
        return result;

    }

    /**
     * Write out the specified string to the buffer and the output stream.
     */
    @SuppressWarnings("StatementWithEmptyBody")
    public void putString(final CharSequence str) throws IOException {
        int pos = getCursorPosition();
        buf.write(str);
        if (mask == null) {
            // no masking
            print(str, pos);
        } else if (mask == NULL_MASK) {
            // don't print anything
        } else {
            rawPrint(mask, str.length());
        }
        drawBuffer();
    }

    /**
     * Redraw the rest of the buffer from the cursor onwards. This is necessary
     * for inserting text into the buffer.
     *
     * @param clear the number of characters to clear after the end of the buffer
     */
    private void drawBuffer(final int clear) throws IOException {
        // debug ("drawBuffer: " + clear);
        int nbChars = buf.length() - buf.cursor;
        if (buf.cursor != buf.length() || clear != 0) {
            if (mask != null) {
                if (mask != NULL_MASK) {
                    rawPrint(mask, nbChars);
                } else {
                    nbChars = 0;
                }
            } else {
                print(buf.buffer, buf.cursor, buf.length());
            }
        }
        int cursorPos = promptLen + wcwidth(buf.buffer, 0, buf.length(), promptLen);

        if (console.getBooleanCapability(Capability.auto_right_margin)
                && console.getBooleanCapability(Capability.eat_newline_glitch)
                && !cursorOk) {
            int width = size.getColumns();
            // best guess on whether the cursor is in that weird location...
            // Need to do this without calling ansi cursor location methods
            // otherwise it breaks paste of wrapped lines in xterm.
            if (cursorPos > 0 && (cursorPos % width == 0)) {
                // the following workaround is reverse-engineered from looking
                // at what bash sent to the console in the same situation
                rawPrint(' '); // move cursor to next line by printing dummy space
                console.puts(Capability.carriage_return); // CR / not newline.
            }
            cursorOk = true;
        }
        clearAhead(clear, cursorPos);
        back(nbChars);
    }

    /**
     * Redraw the rest of the buffer from the cursor onwards. This is necessary
     * for inserting text into the buffer.
     */
    private void drawBuffer() throws IOException {
        drawBuffer(0);
    }

    /**
     * Clear ahead the specified number of characters without moving the cursor.
     *
     * @param num the number of characters to clear
     * @param pos the current screen cursor position
     */
    private void clearAhead(int num, final int pos) throws IOException {
        if (num == 0) return;

        int width = size.getColumns();
        // Use kill line
        if (console.getStringCapability(Capability.clr_eol) != null) {
            int cur = pos;
            int c0 = cur % width;
            // Erase end of current line
            int nb = Math.min(num, width - c0);
            console.puts(Capability.clr_eol);
            num -= nb;
            // Loop
            while (num > 0) {
                // Move to beginning of next line
                int prev = cur;
                cur = cur - cur % width + width;
                moveCursorFromTo(prev, cur);
                // Erase
                nb = Math.min(num, width);
                console.puts(Capability.clr_eol);
                num -= nb;
            }
            moveCursorFromTo(cur, pos);
        }
        // Terminal does not wrap on the right margin
        else if (!console.getBooleanCapability(Capability.auto_right_margin)) {
            int cur = pos;
            int c0 = cur % width;
            // Erase end of current line
            int nb = Math.min(num, width - c0);
            rawPrint(' ', nb);
            num -= nb;
            cur += nb;
            // Loop
            while (num > 0) {
                // Move to beginning of next line
                moveCursorFromTo(cur, ++cur);
                // Erase
                nb = Math.min(num, width);
                rawPrint(' ', nb);
                num -= nb;
                cur += nb;
            }
            moveCursorFromTo(cur, pos);
        }
        // Simple erasure
        else {
            rawPrint(' ', num);
            moveCursorFromTo(pos + num, pos);
        }
    }

    /**
     * Move the visual cursor backward without modifying the buffer cursor.
     */
    protected void back(final int num) throws IOException {
        if (num == 0) return;
        int i0 = promptLen + wcwidth(buf.buffer, 0, buf.cursor, promptLen);
        int i1 = i0 + ((mask != null) ? num : wcwidth(buf.buffer, buf.cursor, buf.cursor + num, i0));
        moveCursorFromTo(i1, i0);
    }

    /**
     * Flush the console output stream. This is important for printout out single characters (like a backspace or
     * keyboard) that we want the console to handle immediately.
     */
    public void flush() throws IOException {
        console.writer().flush();
    }

    private int backspaceAll() throws IOException {
        return backspace(Integer.MAX_VALUE);
    }

    /**
     * Issue <em>num</em> backspaces.
     *
     * @return the number of characters backed up
     */
    private int backspace(final int num) throws IOException {
        if (buf.cursor == 0) {
            return 0;
        }

        int count = - moveCursor(-num);
        int clear = wcwidth(buf.buffer, buf.cursor, buf.cursor + count, getCursorPosition());
        buf.buffer.delete(buf.cursor, buf.cursor + count);

        drawBuffer(clear);
        return count;
    }

    /**
     * Issue a backspace.
     *
     * @return true if successful
     */
    public boolean backspace() throws IOException {
        return backspace(1) == 1;
    }

    protected boolean moveToEnd() throws IOException {
        if (buf.cursor == buf.length()) {
            return true;
        }
        return moveCursor(buf.length() - buf.cursor) > 0;
    }

    /**
     * Delete the character at the current position and redraw the remainder of the buffer.
     */
    private boolean deleteCurrentCharacter() throws IOException {
        if (buf.length() == 0 || buf.cursor == buf.length()) {
            return false;
        }

        buf.buffer.deleteCharAt(buf.cursor);
        drawBuffer(1);
        return true;
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
    private Operation viDeleteChangeYankToRemap (Operation op) {
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
     * Deletes the previous character from the cursor position
     * @param count number of times to do it.
     * @return true if it was done.
     */
    private boolean viRubout(int count) throws IOException {
        boolean ok = true;
        for (int i = 0; ok && i < count; i++) {
            ok = backspace();
        }
        return ok;
    }

    /**
     * Deletes the character you are sitting on and sucks the rest of
     * the line in from the right.
     * @param count Number of times to perform the operation.
     * @return true if its works, false if it didn't
     */
    private boolean viDelete(int count) throws IOException {
        boolean ok = true;
        for (int i = 0; ok && i < count; i++) {
            ok = deleteCurrentCharacter();
        }
        return ok;
    }

    /**
     * Switches the case of the current character from upper to lower
     * or lower to upper as necessary and advances the cursor one
     * position to the right.
     * @param count The number of times to repeat
     * @return true if it completed successfully, false if not all
     *   case changes could be completed.
     */
    private boolean viChangeCase(int count) throws IOException {
        boolean ok = true;
        for (int i = 0; ok && i < count; i++) {

            ok = buf.cursor < buf.buffer.length ();
            if (ok) {
                char ch = buf.buffer.charAt(buf.cursor);
                if (Character.isUpperCase(ch)) {
                    ch = Character.toLowerCase(ch);
                }
                else if (Character.isLowerCase(ch)) {
                    ch = Character.toUpperCase(ch);
                }
                buf.buffer.setCharAt(buf.cursor, ch);
                drawBuffer(1);
                moveCursor(1);
            }
        }
        return ok;
    }

    /**
     * Implements the vi change character command (in move-mode "r"
     * followed by the character to change to).
     * @param count Number of times to perform the action
     * @param c The character to change to
     * @return Whether or not there were problems encountered
     */
    private boolean viChangeChar(int count, int c) throws IOException {
        // EOF, ESC, or CTRL-C aborts.
        if (c < 0 || c == '\033' || c == '\003') {
            return true;
        }

        boolean ok = true;
        for (int i = 0; ok && i < count; i++) {
            ok = buf.cursor < buf.buffer.length ();
            if (ok) {
                buf.buffer.setCharAt(buf.cursor, (char) c);
                drawBuffer(1);
                if (i < (count-1)) {
                    moveCursor(1);
                }
            }
        }
        return ok;
    }

    /**
     * This is a close facsimile of the actual vi previous word logic. In
     * actual vi words are determined by boundaries of identity characterse.
     * This logic is a bit more simple and simply looks at white space or
     * digits or characters.  It should be revised at some point.
     *
     * @param count number of iterations
     * @return true if the move was successful, false otherwise
     */
    private boolean viPreviousWord(int count) throws IOException {
        if (buf.cursor == 0) {
            return false;
        }

        int pos = buf.cursor - 1;
        for (int i = 0; pos > 0 && i < count; i++) {
            // If we are on white space, then move back.
            while (pos > 0 && isWhitespace(buf.buffer.charAt(pos))) {
                --pos;
            }

            while (pos > 0 && !isDelimiter(buf.buffer.charAt(pos-1))) {
                --pos;
            }

            if (pos > 0 && i < (count-1)) {
                --pos;
            }
        }
        setCursorPosition(pos);
        return true;
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
    private boolean viDeleteTo(int startPos, int endPos, boolean isChange) throws IOException {
        if (startPos == endPos) {
            return true;
        }

        if (endPos < startPos) {
            int tmp = endPos;
            endPos = startPos;
            startPos = tmp;
        }

        setCursorPosition(startPos);
        buf.cursor = startPos;
        buf.buffer.delete(startPos, endPos);
        drawBuffer(endPos - startPos);

        // If we are doing a delete operation (e.g. "d$") then don't leave the
        // cursor dangling off the end. In reality the "isChange" flag is silly
        // what is really happening is that if we are in "move-mode" then the
        // cursor can't be moved off the end of the line, but in "edit-mode" it
        // is ok, but I have no easy way of knowing which mode we are in.
        if (! isChange && startPos > 0 && startPos == buf.length()) {
            moveCursor(-1);
        }
        return true;
    }

    /**
     * Implement the "vi" yank-to operation.  This operation allows you
     * to yank the contents of the current line based upon a move operation,
     * for exaple "yw" yanks the current word, "3yw" yanks 3 words, etc.
     *
     * @param startPos The starting position from which to yank
     * @param endPos The ending position to which to yank
     * @return true if the yank succeeded
     */
    private boolean viYankTo(int startPos, int endPos) throws IOException {
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

        yankBuffer = buf.buffer.substring(startPos, endPos);

        /*
         * It was a movement command that moved the cursor to find the
         * end position, so put the cursor back where it started.
         */
        setCursorPosition(cursorPos);
        return true;
    }

    /**
     * Pasts the yank buffer to the right of the current cursor position
     * and moves the cursor to the end of the pasted region.
     *
     * @param count Number of times to perform the operation.
     * @return true if it worked, false otherwise
     */
    private boolean viPut(int count) throws IOException {
        if (yankBuffer.length () == 0) {
            return true;
        }
        if (buf.cursor < buf.buffer.length ()) {
            moveCursor(1);
        }
        for (int i = 0; i < count; i++) {
            putString(yankBuffer);
        }
        moveCursor(-1);
        return true;
    }

    /**
     * Searches forward of the current position for a character and moves
     * the cursor onto it.
     * @param count Number of times to repeat the process.
     * @param ch The character to search for
     * @return true if the char was found, false otherwise
     */
    private boolean viCharSearch(int count, int invokeChar, int ch) throws IOException {
        if (ch < 0 || invokeChar < 0) {
            return false;
        }

        char    searchChar = (char)ch;
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
            charSearchFirstInvokeChar = (char) invokeChar;
        }

        charSearchLastInvokeChar = (char)invokeChar;

        isForward = Character.isLowerCase(charSearchFirstInvokeChar);
        stopBefore = (Character.toLowerCase(charSearchFirstInvokeChar) == 't');

        boolean ok = false;

        if (isForward) {
            while (count-- > 0) {
                int pos = buf.cursor + 1;
                while (pos < buf.buffer.length()) {
                    if (buf.buffer.charAt(pos) == searchChar) {
                        setCursorPosition(pos);
                        ok = true;
                        break;
                    }
                    ++pos;
                }
            }

            if (ok) {
                if (stopBefore)
                    moveCursor(-1);

                /*
                 * When in yank-to, move-to, del-to state we actually want to
                 * go to the character after the one we landed on to make sure
                 * that the character we ended up on is included in the
                 * operation
                 */
                if (isInViMoveOperationState()) {
                    moveCursor(1);
                }
            }
        }
        else {
            while (count-- > 0) {
                int pos = buf.cursor - 1;
                while (pos >= 0) {
                    if (buf.buffer.charAt(pos) == searchChar) {
                        setCursorPosition(pos);
                        ok = true;
                        break;
                    }
                    --pos;
                }
            }

            if (ok && stopBefore)
                moveCursor(1);
        }

        return ok;
    }

    private char switchCase(char ch) {
        if (Character.isUpperCase(ch)) {
            return Character.toLowerCase(ch);
        }
        return Character.toUpperCase(ch);
    }

    /**
     * @return true if line reader is in the middle of doing a change-to
     *   delete-to or yank-to.
     */
    private boolean isInViMoveOperationState() {
        return state == State.VI_CHANGE_TO
            || state == State.VI_DELETE_TO
            || state == State.VI_YANK_TO;
    }

    /**
     * This is a close facsimile of the actual vi next word logic.
     * As with viPreviousWord() this probably needs to be improved
     * at some point.
     *
     * @param count number of iterations
     * @return true if the move was successful, false otherwise
     */
    private boolean viNextWord(int count) throws IOException {
        int pos = buf.cursor;
        int end = buf.buffer.length();

        for (int i = 0; pos < end && i < count; i++) {
            // Skip over letter/digits
            while (pos < end && !isDelimiter(buf.buffer.charAt(pos))) {
                ++pos;
            }

            /*
             * Don't you love special cases? During delete-to and yank-to
             * operations the word movement is normal. However, during a
             * change-to, the trailing spaces behind the last word are
             * left in tact.
             */
            if (i < (count-1) || !(state == State.VI_CHANGE_TO)) {
                while (pos < end && isDelimiter(buf.buffer.charAt(pos))) {
                    ++pos;
                }
            }
        }

        setCursorPosition(pos);
        return true;
    }

    /**
     * Implements a close facsimile of the vi end-of-word movement.
     * If the character is on white space, it takes you to the end
     * of the next word.  If it is on the last character of a word
     * it takes you to the next of the next word.  Any other character
     * of a word, takes you to the end of the current word.
     *
     * @param count Number of times to repeat the action
     * @return true if it worked.
     */
    private boolean viEndWord(int count) throws IOException {
        int pos = buf.cursor;
        int end = buf.buffer.length();

        for (int i = 0; pos < end && i < count; i++) {
            if (pos < (end-1)
                    && !isDelimiter(buf.buffer.charAt(pos))
                    && isDelimiter(buf.buffer.charAt (pos+1))) {
                ++pos;
            }

            // If we are on white space, then move back.
            while (pos < end && isDelimiter(buf.buffer.charAt(pos))) {
                ++pos;
            }

            while (pos < (end-1) && !isDelimiter(buf.buffer.charAt(pos+1))) {
                ++pos;
            }
        }
        setCursorPosition(pos);
        return true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private boolean previousWord() throws IOException {
        while (isDelimiter(buf.current()) && (moveCursor(-1) != 0)) {
            // nothing
        }

        while (!isDelimiter(buf.current()) && (moveCursor(-1) != 0)) {
            // nothing
        }

        return true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private boolean nextWord() throws IOException {
        while (isDelimiter(buf.nextChar()) && (moveCursor(1) != 0)) {
            // nothing
        }

        while (!isDelimiter(buf.nextChar()) && (moveCursor(1) != 0)) {
            // nothing
        }

        return true;
    }

    /**
     * Deletes to the beginning of the word that the cursor is sitting on.
     * If the cursor is on white-space, it deletes that and to the beginning
     * of the word before it.  If the user is not on a word or whitespace
     * it deletes up to the end of the previous word.
     *
     * @param count Number of times to perform the operation
     * @return true if it worked, false if you tried to delete too many words
     */
    private boolean unixWordRubout(int count) throws IOException {
        boolean success = true;
        StringBuilder killed = new StringBuilder();

        for (; count > 0; --count) {
            if (buf.cursor == 0) {
                success = false;
                break;
            }

            while (isWhitespace(buf.current())) {
                char c = buf.current();
                if (c == 0) {
                    break;
                }

                killed.append(c);
                backspace();
            }

            while (!isWhitespace(buf.current())) {
                char c = buf.current();
                if (c == 0) {
                    break;
                }

                killed.append(c);
                backspace();
            }
        }

        String copy = killed.reverse().toString();
        killRing.addBackwards(copy);

        return success;
    }

    private String insertComment(boolean isViMode) throws IOException {
        String comment = getVariable(COMMENT_BEGIN);
        if (comment == null) {
            comment = "#";
        }
        setCursorPosition(0);
        putString(comment);
        if (isViMode) {
            consoleKeys.setKeyMap(KeyMap.VI_INSERT);
        }
        return accept();
    }

    /**
     * Implements vi search ("/" or "?").
     */
    @SuppressWarnings("fallthrough")
    private int viSearch(char searchChar) throws IOException {
        boolean isForward = (searchChar == '/');

        /*
         * This is a little gross, I'm sure there is a more appropriate way
         * of saving and restoring state.
         */
        CursorBuffer origBuffer = buf.copy();

        // Clear the contents of the current line and
        setCursorPosition (0);
        killLine();

        // Our new "prompt" is the character that got us into search mode.
        putString(Character.toString(searchChar));
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
                case '\010':  // Backspace
                case '\177':  // Delete
                    backspace();
                    /*
                     * Backspacing through the "prompt" aborts the search.
                     */
                    if (buf.cursor == 0) {
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
            setCursorPosition(0);
            killLine();
            putString(origBuffer.buffer);
            setCursorPosition(origBuffer.cursor);
            return -1;
        }

        /*
         * The first character of the buffer was the search character itself
         * so we discard it.
         */
        String searchTerm = buf.buffer.substring(1);
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
                if (history.get(i).toString().contains(searchTerm)) {
                    idx = i;
                    break;
                }
            }
        }
        else {
            for (int i = end-1; i >= start; i--) {
                if (history.get(i).toString().contains(searchTerm)) {
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
            setCursorPosition(0);
            killLine();
            putString(origBuffer.buffer);
            setCursorPosition(0);
            return -1;
        }

        /*
         * Show the match.
         */
        setCursorPosition(0);
        killLine();
        putString(history.get(idx));
        setCursorPosition(0);
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
                            if (history.get(i).toString().contains(searchTerm)) {
                                idx = i;
                                isMatch = true;
                            }
                        }
                    }
                    else {
                        for (int i = idx - 1; !isMatch && i >= start; i--) {
                            if (history.get(i).toString().contains(searchTerm)) {
                                idx = i;
                                isMatch = true;
                            }
                        }
                    }
                    if (isMatch) {
                        setCursorPosition(0);
                        killLine();
                        putString(history.get(idx));
                        setCursorPosition(0);
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
        return ch;
    }

    private void insertClose(String s) throws IOException {
        putString(s);

        int closePosition = buf.cursor;

        moveCursor(-1);
        viMatch();

        console.reader().peek(BLINK_MATCHING_PAREN_TIMEOUT);

        setCursorPosition(closePosition);
        flush();
    }

    /**
     * Implements vi style bracket matching ("%" command). The matching
     * bracket for the current bracket type that you are sitting on is matched.
     * The logic works like so:
     * @return true if it worked, false if the cursor was not on a bracket
     *   character or if there was no matching bracket.
     */
    private boolean viMatch() throws IOException {
        int pos        = buf.cursor;

        if (pos == buf.length()) {
            return false;
        }

        int type       = getBracketType(buf.buffer.charAt (pos));
        int move       = (type < 0) ? -1 : 1;
        int count      = 1;

        if (type == 0)
            return false;

        while (count > 0) {
            pos += move;

            // Fell off the start or end.
            if (pos < 0 || pos >= buf.buffer.length ()) {
                return false;
            }

            int curType = getBracketType(buf.buffer.charAt (pos));
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

        setCursorPosition(pos);
        flush();
        return true;
    }

    /**
     * Given a character determines what type of bracket it is (paren,
     * square, curly, or none).
     * @param ch The character to check
     * @return 1 is square, 2 curly, 3 parent, or zero for none.  The value
     *   will be negated if it is the closing form of the bracket.
     */
    private int getBracketType (char ch) {
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

    private boolean deletePreviousWord() throws IOException {
        StringBuilder killed = new StringBuilder();
        char c;

        while (isDelimiter((c = buf.current()))) {
            if (c == 0) {
                break;
            }

            killed.append(c);
            backspace();
        }

        while (!isDelimiter((c = buf.current()))) {
            if (c == 0) {
                break;
            }

            killed.append(c);
            backspace();
        }

        String copy = killed.reverse().toString();
        killRing.addBackwards(copy);
        return true;
    }

    private boolean deleteNextWord() throws IOException {
        StringBuilder killed = new StringBuilder();
        char c;

        while (isDelimiter((c = buf.nextChar()))) {
            if (c == 0) {
                break;
            }
            killed.append(c);
            delete();
        }

        while (!isDelimiter((c = buf.nextChar()))) {
            if (c == 0) {
                break;
            }
            killed.append(c);
            delete();
        }

        String copy = killed.toString();
        killRing.add(copy);

        return true;
    }

    private boolean capitalizeWord() throws IOException {
        boolean first = true;
        int i = 1;
        char c;
        while (buf.cursor + i  - 1< buf.length() && !isDelimiter((c = buf.buffer.charAt(buf.cursor + i - 1)))) {
            buf.buffer.setCharAt(buf.cursor + i - 1, first ? Character.toUpperCase(c) : Character.toLowerCase(c));
            first = false;
            i++;
        }
        drawBuffer();
        moveCursor(i - 1);
        return true;
    }

    private boolean upCaseWord() throws IOException {
        int i = 1;
        char c;
        while (buf.cursor + i - 1 < buf.length() && !isDelimiter((c = buf.buffer.charAt(buf.cursor + i - 1)))) {
            buf.buffer.setCharAt(buf.cursor + i - 1, Character.toUpperCase(c));
            i++;
        }
        drawBuffer();
        moveCursor(i - 1);
        return true;
    }

    private boolean downCaseWord() throws IOException {
        int i = 1;
        char c;
        while (buf.cursor + i - 1 < buf.length() && !isDelimiter((c = buf.buffer.charAt(buf.cursor + i - 1)))) {
            buf.buffer.setCharAt(buf.cursor + i - 1, Character.toLowerCase(c));
            i++;
        }
        drawBuffer();
        moveCursor(i - 1);
        return true;
    }

    /**
     * Performs character transpose. The character prior to the cursor and the
     * character under the cursor are swapped and the cursor is advanced one
     * character unless you are already at the end of the line.
     *
     * @param count The number of times to perform the transpose
     * @return true if the operation succeeded, false otherwise (e.g. transpose
     *   cannot happen at the beginning of the line).
     */
    private boolean transposeChars(int count) throws IOException {
        for (; count > 0; --count) {
            if (buf.cursor == 0 || buf.cursor == buf.buffer.length()) {
                return false;
            }

            int first  = buf.cursor-1;
            int second = buf.cursor;

            char tmp = buf.buffer.charAt (first);
            buf.buffer.setCharAt(first, buf.buffer.charAt(second));
            buf.buffer.setCharAt(second, tmp);

            // This could be done more efficiently by only re-drawing at the end.
            moveInternal(-1);
            drawBuffer();
            moveInternal(2);
        }

        return true;
    }

    public boolean isKeyMap(String name) {
        // Current keymap.
        KeyMap map = consoleKeys.getKeys();
        KeyMap mapByName = consoleKeys.getKeyMaps().get(name);

        if (mapByName == null)
            return false;

        /*
         * This may not be safe to do, but there doesn't appear to be a
         * clean way to find this information out.
         */
        return map == mapByName;
    }


    /**
     * The equivalent of hitting &lt;RET&gt;.  The line is considered
     * complete and is returned.
     *
     * @return The completed line of text.
     */
    public String accept() throws IOException {
        moveToEnd();
        println(); // output newline
        flush();
        return finishBuffer();
    }

    private void abort() throws IOException {
        beep();
        buf.clear();
        println();
        redrawLine();
    }

    /**
     * Move the cursor <i>where</i> characters.
     *
     * @param num   If less than 0, move abs(<i>where</i>) to the left, otherwise move <i>where</i> to the right.
     * @return      The number of spaces we moved
     */
    public int moveCursor(final int num) throws IOException {
        int where = num;

        if ((buf.cursor == 0) && (where <= 0)) {
            return 0;
        }

        if ((buf.cursor == buf.buffer.length()) && (where >= 0)) {
            return 0;
        }

        if ((buf.cursor + where) < 0) {
            where = -buf.cursor;
        }
        else if ((buf.cursor + where) > buf.buffer.length()) {
            where = buf.buffer.length() - buf.cursor;
        }

        moveInternal(where);

        return where;
    }

    /**
     * Move the cursor <i>where</i> characters, without checking the current buffer.
     *
     * @param where the number of characters to move to the right or left.
     */
    private void moveInternal(final int where) throws IOException {
        // debug ("move cursor " + where + " ("
        // + buf.cursor + " => " + (buf.cursor + where) + ")");
        buf.cursor += where;

        int i0;
        int i1;
        if (mask == null) {
            if (where < 0) {
                i1 = promptLen + wcwidth(buf.buffer, 0, buf.cursor, promptLen);
                i0 = i1 + wcwidth(buf.buffer, buf.cursor, buf.cursor - where, i1);
            } else {
                i0 = promptLen + wcwidth(buf.buffer, 0, buf.cursor - where, promptLen);
                i1 = i0 + wcwidth(buf.buffer, buf.cursor - where, buf.cursor, i0);
            }
        } else if (mask != NULL_MASK) {
            i1 = promptLen + buf.cursor;
            i0 = i1 - where;
        } else {
            return;
        }
        moveCursorFromTo(i0, i1);
    }

    private void moveCursorFromTo(int i0, int i1) throws IOException {
        if (i0 == i1) return;
        int width = size.getColumns();
        int l0 = i0 / width;
        int c0 = i0 % width;
        int l1 = i1 / width;
        int c1 = i1 % width;
        if (l0 == l1 + 1) {
            if (!console.puts(Capability.cursor_up)) {
                console.puts(Capability.parm_up_cursor, 1);
            }
        } else if (l0 > l1) {
            if (!console.puts(Capability.parm_up_cursor, l0 - l1)) {
                for (int i = l1; i < l0; i++) {
                    console.puts(Capability.cursor_up);
                }
            }
        } else if (l0 < l1) {
            console.puts(Capability.carriage_return);
            rawPrint('\n', l1 - l0);
            c0 = 0;
        }
        if (c0 == c1 - 1) {
            console.puts(Capability.cursor_right);
        } else if (c0 == c1 + 1) {
            console.puts(Capability.cursor_left);
        } else if (c0 < c1) {
            if (!console.puts(Capability.parm_right_cursor, c1 - c0)) {
                for (int i = c0; i < c1; i++) {
                    console.puts(Capability.cursor_right);
                }
            }
        } else if (c0 > c1) {
            if (!console.puts(Capability.parm_left_cursor, c0 - c1)) {
                for (int i = c1; i < c0; i++) {
                    console.puts(Capability.cursor_left);
                }
            }
        }
        cursorOk = true;
    }

    /**
     * Read a character from the console.
     *
     * @return the character, or -1 if an EOF is received.
     */
    public int readCharacter() throws IOException {
        int c = NonBlockingReader.READ_EXPIRED;
        while (c == NonBlockingReader.READ_EXPIRED) {
            c = console.reader().read(100);
        }
        return c;
    }

    public int readCharacter(final char... allowed) throws IOException {
        // if we restrict to a limited set and the current character is not in the set, then try again.
        char c;

        Arrays.sort(allowed); // always need to sort before binarySearch

        //noinspection StatementWithEmptyBody
        while (Arrays.binarySearch(allowed, c = (char) readCharacter()) < 0) {
            // nothing
        }

        return c;
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
    public Object readBinding(KeyMap keys) throws IOException {
        Object o;
        opBuffer.setLength(0);
        do {
            int c = pushBackChar.isEmpty() ? readCharacter() : pushBackChar.pop();
            if (c == -1) {
                return null;
            }
            opBuffer.appendCodePoint(c);

            if (recording) {
                macro += new String(Character.toChars(c));
            }

            if (quotedInsert) {
                o = Operation.SELF_INSERT;
                quotedInsert = false;
            } else {
                o = keys.getBound(opBuffer);
            }

            /*
             * The kill ring keeps record of whether or not the
             * previous command was a yank or a kill. We reset
             * that state here if needed.
             */
            if (!recording && !(o instanceof KeyMap)) {
                if (o != Operation.YANK_POP && o != Operation.YANK) {
                    killRing.resetLastYank();
                }
                if (o != Operation.KILL_LINE && o != Operation.KILL_WHOLE_LINE
                        && o != Operation.BACKWARD_KILL_WORD && o != Operation.KILL_WORD
                        && o != Operation.UNIX_LINE_DISCARD && o != Operation.UNIX_WORD_RUBOUT) {
                    killRing.resetLastKill();
                }
            }

            if (o == Operation.DO_LOWERCASE_VERSION) {
                opBuffer.setLength(opBuffer.length() - 1);
                opBuffer.append(Character.toLowerCase((char) c));
                o = keys.getBound(opBuffer);
            }

            /*
             * The ESC key (#27) is special in that it is ambiguous until
             * you know what is coming next.  The ESC could be a literal
             * escape, like the user entering vi-move mode, or it could
             * be part of a console control sequence.  The following
             * logic attempts to disambiguate things in the same
             * fashion as regular vi or readline.
             *
             * When ESC is encountered and there is no other pending
             * character in the pushback queue, then attempt to peek
             * into the input stream (if the feature is enabled) for
             * 150ms. If nothing else is coming, then assume it is
             * not a console control sequence, but a raw escape.
             */
            if (o instanceof KeyMap && opBuffer.length() == 1 && c == ESCAPE && pushBackChar.isEmpty()) {
                long t = getLong(ESCAPE_TIMEOUT, 0l);
                if (t > 0 && console.reader().peek(t) == READ_EXPIRED) {
                    Object otherKey = ((KeyMap) o).getAnotherKey();
                    if (otherKey == null) {
                        // The next line is in case a binding was set up inside this secondary
                        // KeyMap (like EMACS_META).  For example, a binding could be put
                        // there for an ActionListener for the ESC key.  This way, the var 'o' won't
                        // be null and the code can proceed to let the ActionListener be
                        // handled, below.
                        otherKey = ((KeyMap) o).getBound(Character.toString((char) c));
                    }
                    if (otherKey != null && !(otherKey instanceof KeyMap)) {
                        return otherKey;
                    }
                }
            }

            /*
             * If we didn't find a binding for the key and there is
             * more than one character accumulated then start checking
             * the largest span of characters from the beginning to
             * see if there is a binding for them.
             *
             * For example if our buffer has ESC,CTRL-M,C the getBound()
             * called previously indicated that there is no binding for
             * this sequence, so this then checks ESC,CTRL-M, and failing
             * that, just ESC. Each keystroke that is pealed off the end
             * during these tests is stuffed onto the pushback buffer so
             * they won't be lost.
             *
             * If there is no binding found, then we go back to waiting for
             * input.
             */
            while (o == null && opBuffer.length() > 0) {
                c = opBuffer.charAt(opBuffer.length() - 1);
                opBuffer.setLength(opBuffer.length() - 1);
                Object o2 = keys.getBound(opBuffer);
                if (o2 instanceof KeyMap) {
                    o = ((KeyMap) o2).getAnotherKey();
                    if (o != null) {
                        pushBackChar.push((char) c);
                    }
                }
            }

        } while (o == null || o instanceof KeyMap);

        return o;
    }

    public String getLastBinding() {
        return opBuffer.toString();
    }

    //
    // Key Bindings
    //

    /**
     * Sets the current keymap by name. Supported keymaps are "emacs",
     * "vi-insert", "vi-move".
     * @param name The name of the keymap to switch to
     * @return true if the keymap was set, or false if the keymap is
     *    not recognized.
     */
    public boolean setKeyMap(String name) {
        return consoleKeys.setKeyMap(name);
    }

    /**
     * Returns the name of the current key mapping.
     * @return the name of the key mapping. This will be the canonical name
     *   of the current mode of the key map and may not reflect the name that
     *   was used with {@link #setKeyMap(String)}.
     */
    public String getKeyMap() {
        return consoleKeys.getKeys().getName();
    }

    //
    // Line Reading
    //

    /**
     * Read the next line and return the contents of the buffer.
     */
    public String readLine() throws UserInterruptException, EOFException {
        return readLine(null, null, null);
    }

    /**
     * Read the next line with the specified character mask. If null, then
     * characters will be echoed. If 0, then no characters will be echoed.
     */
    public String readLine(Character mask) throws UserInterruptException, EOFException {
        return readLine(null, mask, null);
    }

    public String readLine(String prompt) throws UserInterruptException, EOFException {
        return readLine(prompt, null, null);
    }

    /**
     * Read a line from the <i>in</i> {@link InputStream}, and return the line
     * (without any trailing newlines).
     *
     * @param prompt    The prompt to issue to the console, may be null.
     * @return          A line that is read from the console, or null if there was null input (e.g., <i>CTRL-D</i>
     *                  was pressed).
     */
    public String readLine(String prompt, Character mask) throws UserInterruptException, EOFException {
        return readLine(prompt, mask, null);
    }

    /**
     * Read a line from the <i>in</i> {@link InputStream}, and return the line
     * (without any trailing newlines).
     *
     * @param prompt    The prompt to issue to the console, may be null.
     * @return          A line that is read from the console, or null if there was null input (e.g., <i>CTRL-D</i>
     *                  was pressed).
     */
    public String readLine(String prompt, Character mask, String buffer) throws UserInterruptException, EOFException {
        // prompt may be null
        // mask may be null
        // buffer may be null

        readLineThread = Thread.currentThread();
        SignalHandler previousIntrHandler = null;
        Attributes originalAttributes = null;
        try {
            previousIntrHandler = console.handle(Signal.INT, new SignalHandler() {
                @Override
                public void handle(Signal signal) {
                    if (signal == Signal.INT) {
                        readLineThread.interrupt();
                    }
                }
            });
            originalAttributes = console.enterRawMode();

            /*
             * This is the accumulator for VI-mode repeat count. That is, while in
             * move mode, if you type 30x it will delete 30 characters. This is
             * where the "30" is accumulated until the command is struck.
             */
            int repeatCount = 0;

            this.mask = mask;
            setPrompt(prompt);

            String originalPrompt = this.prompt;

            state = State.NORMAL;

            pushBackChar.clear();

            if (buffer != null) {
                buf.write(buffer);
            }

            if (prompt != null && prompt.length() > 0) {
                console.writer().write(prompt);
                console.writer().flush();
            }

            while (true) {

                Object o = readBinding(getKeys());
                if (o == null) {
                    return null;
                }
                int c = 0;
                if (opBuffer.length() > 0) {
                    c = opBuffer.codePointBefore(opBuffer.length());
                }
                Log.trace("Binding: ", o);


                // Handle macros
                if (o instanceof String) {
                    String macro = (String) o;
                    for (int i = 0; i < macro.length(); i++) {
                        pushBackChar.push(macro.charAt(macro.length() - 1 - i));
                    }
                    opBuffer.setLength(0);
                    continue;
                }

                // Handle custom callbacks
                if (o instanceof ActionListener) {
                    ((ActionListener) o).actionPerformed(null);
                    opBuffer.setLength(0);
                    continue;
                }

                boolean success = true;

                // Cache console size for the duration of the binding processing
                this.size = console.getSize();

                CursorBuffer oldBuf = buf.copy();

                // Search mode.
                //
                // Note that we have to do this first, because if there is a command
                // not linked to a search command, we leave the search mode and fall
                // through to the normal state.
                if (state == State.SEARCH || state == State.FORWARD_SEARCH) {
                    int cursorDest = -1;
                    // TODO: check the isearch-terminators variable terminating the search
                    switch ( ((Operation) o )) {
                        case ABORT:
                            state = State.NORMAL;
                            buf.clear();
                            buf.write(originalBuffer.buffer);
                            buf.cursor = originalBuffer.cursor;
                            break;

                        case REVERSE_SEARCH_HISTORY:
                            state = State.SEARCH;
                            if (searchTerm.length() == 0) {
                                searchTerm.append(previousSearchTerm);
                            }

                            if (searchIndex > 0) {
                                searchIndex = searchBackwards(searchTerm.toString(), searchIndex);
                            }
                            break;

                        case FORWARD_SEARCH_HISTORY:
                            state = State.FORWARD_SEARCH;
                            if (searchTerm.length() == 0) {
                                searchTerm.append(previousSearchTerm);
                            }

                            if (searchIndex > -1 && searchIndex < history.size() - 1) {
                                searchIndex = searchForwards(searchTerm.toString(), searchIndex);
                            }
                            break;

                        case BACKWARD_DELETE_CHAR:
                            if (searchTerm.length() > 0) {
                                searchTerm.deleteCharAt(searchTerm.length() - 1);
                                if (state == State.SEARCH) {
                                    searchIndex = searchBackwards(searchTerm.toString());
                                } else {
                                    searchIndex = searchForwards(searchTerm.toString());
                                }
                            }
                            break;

                        case SELF_INSERT:
                            searchTerm.appendCodePoint(c);
                            if (state == State.SEARCH) {
                                searchIndex = searchBackwards(searchTerm.toString());
                            } else {
                                searchIndex = searchForwards(searchTerm.toString());
                            }
                            break;

                        default:
                            // Set buffer and cursor position to the found string.
                            if (searchIndex != -1) {
                                history.moveTo(searchIndex);
                                // set cursor position to the found string
                                cursorDest = history.current().toString().indexOf(searchTerm.toString());
                            }
                            if (o != Operation.ACCEPT_LINE) {
                                o = null;
                            }
                            state = State.NORMAL;
                            break;
                    }

                    // if we're still in search mode, print the search status
                    if (state == State.SEARCH || state == State.FORWARD_SEARCH) {
                        if (searchTerm.length() == 0) {
                            if (state == State.SEARCH) {
                                printSearchStatus("", "");
                            } else {
                                printForwardSearchStatus("", "");
                            }
                            searchIndex = -1;
                        } else {
                            if (searchIndex == -1) {
                                beep();
                                printSearchStatus(searchTerm.toString(), "");
                            } else if (state == State.SEARCH) {
                                printSearchStatus(searchTerm.toString(), history.get(searchIndex).toString());
                            } else {
                                printForwardSearchStatus(searchTerm.toString(), history.get(searchIndex).toString());
                            }
                        }
                    }
                    // otherwise, restore the line
                    else {
                        restoreLine(originalPrompt, cursorDest);
                    }
                }
                if (state != State.SEARCH && state != State.FORWARD_SEARCH) {
                    /*
                     * If this is still false at the end of the switch, then
                     * we reset our repeatCount to 0.
                     */
                    boolean isArgDigit = false;

                    /*
                     * Every command that can be repeated a specified number
                     * of times, needs to know how many times to repeat, so
                     * we figure that out here.
                     */
                    int count = (repeatCount == 0) ? 1 : repeatCount;

                    /*
                     * Default success to true. You only need to explicitly
                     * set it if something goes wrong.
                     */
                    success = true;

                    if (o instanceof Operation) {
                        Operation op = (Operation)o;
                        /*
                         * Current location of the cursor (prior to the operation).
                         * These are used by vi *-to operation (e.g. delete-to)
                         * so we know where we came from.
                         */
                        int     cursorStart = buf.cursor;
                        State   origState   = state;

                        /*
                         * If we are on a "vi" movement based operation, then we
                         * need to restrict the sets of inputs pretty heavily.
                         */
                        if (state == State.VI_CHANGE_TO
                            || state == State.VI_YANK_TO
                            || state == State.VI_DELETE_TO) {

                            op = viDeleteChangeYankToRemap(op);
                        }

                        switch ( op ) {
                            case COMPLETE: // tab
                                // There is an annoyance with tab completion in that
                                // sometimes the user is actually pasting input in that
                                // has physical tabs in it.  This attempts to look at how
                                // quickly a character follows the tab, if the character
                                // follows *immediately*, we assume it is a tab literal.
                                boolean isTabLiteral = false;
                                if (getBoolean(COPY_PASTE_DETECTION, false)
                                    && c == '\t'
                                    && (!pushBackChar.isEmpty()
                                        || console.reader().peek(COPY_PASTE_DETECTION_TIMEOUT) != READ_EXPIRED)) {
                                    isTabLiteral = true;
                                } else if (getBoolean(DISABLE_COMPLETION, false)) {
                                    isTabLiteral = true;
                                }

                                if (! isTabLiteral) {
                                    success = complete();
                                }
                                else {
                                    putString(opBuffer);
                                }
                                break;

                            case POSSIBLE_COMPLETIONS:
                                printCompletionCandidates();
                                break;

                            case BEGINNING_OF_LINE:
                                success = setCursorPosition(0);
                                break;

                            case YANK:
                                success = yank();
                                break;

                            case YANK_POP:
                                success = yankPop();
                                break;

                            case KILL_LINE: // CTRL-K
                                success = killLine();
                                break;

                            case KILL_WHOLE_LINE:
                                success = setCursorPosition(0) && killLine();
                                break;

                            case CLEAR_SCREEN: // CTRL-L
                                success = clearScreen();
                                redrawLine();
                                break;

                            case OVERWRITE_MODE:
                                buf.setOverTyping(!buf.isOverTyping());
                                break;

                            case SELF_INSERT:
                                putString(opBuffer);
                                break;

                            case ACCEPT_LINE:
                                return accept();

                            case ABORT:
                                if (searchTerm == null) {
                                    abort();
                                }
                                break;

                            case INTERRUPT:
                                println();
                                flush();
                                String partialLine = buf.buffer.toString();
                                buf.clear();
                                history.moveToEnd();
                                throw new UserInterruptException(partialLine);

                            /*
                             * VI_MOVE_ACCEPT_LINE is the result of an ENTER
                             * while in move mode. This is the same as a normal
                             * ACCEPT_LINE, except that we need to enter
                             * insert mode as well.
                             */
                            case VI_MOVE_ACCEPT_LINE:
                                consoleKeys.setKeyMap(KeyMap.VI_INSERT);
                                return accept();

                            case BACKWARD_WORD:
                                success = previousWord();
                                break;

                            case FORWARD_WORD:
                                success = nextWord();
                                break;

                            case PREVIOUS_HISTORY:
                                success = moveHistory(false);
                                break;

                            /*
                             * According to bash/readline move through history
                             * in "vi" mode will move the cursor to the
                             * start of the line. If there is no previous
                             * history, then the cursor doesn't move.
                             */
                            case VI_PREVIOUS_HISTORY:
                                success = moveHistory(false, count)
                                    && setCursorPosition(0);
                                break;

                            case NEXT_HISTORY:
                                success = moveHistory(true);
                                break;

                            /*
                             * According to bash/readline move through history
                             * in "vi" mode will move the cursor to the
                             * start of the line. If there is no next history,
                             * then the cursor doesn't move.
                             */
                            case VI_NEXT_HISTORY:
                                success = moveHistory(true, count)
                                    && setCursorPosition(0);
                                break;

                            case BACKWARD_DELETE_CHAR: // backspace
                                success = backspace();
                                break;

                            case EXIT_OR_DELETE_CHAR:
                                if (buf.buffer.length() == 0) {
                                    println();
                                    flush();
                                    throw new EOFException();
                                }
                                success = deleteCurrentCharacter();
                                break;

                            case DELETE_CHAR: // delete
                                success = deleteCurrentCharacter();
                                break;

                            case BACKWARD_CHAR:
                                success = moveCursor(-(count)) != 0;
                                break;

                            case FORWARD_CHAR:
                                success = moveCursor(count) != 0;
                                break;

                            case UNIX_LINE_DISCARD:
                                success = resetLine();
                                break;

                            case UNIX_WORD_RUBOUT:
                                success = unixWordRubout(count);
                                break;

                            case BACKWARD_KILL_WORD:
                                success = deletePreviousWord();
                                break;

                            case KILL_WORD:
                                success = deleteNextWord();
                                break;

                            case BEGINNING_OF_HISTORY:
                                success = history.moveToFirst();
                                if (success) {
                                    setBuffer(history.current());
                                }
                                break;

                            case END_OF_HISTORY:
                                success = history.moveToLast();
                                if (success) {
                                    setBuffer(history.current());
                                }
                                break;

                            case HISTORY_SEARCH_BACKWARD:
                                searchTerm = new StringBuffer(buf.upToCursor());
                                searchIndex = searchBackwards(searchTerm.toString(), history.index(), true);

                                if (searchIndex == -1) {
                                    beep();
                                } else {
                                    // Maintain cursor position while searching.
                                    success = history.moveTo(searchIndex);
                                    if (success) {
                                        setBufferKeepPos(history.current());
                                    }
                                }
                                break;

                            case HISTORY_SEARCH_FORWARD:
                                searchTerm = new StringBuffer(buf.upToCursor());
                                int index = history.index() + 1;

                                if (index == history.size()) {
                                    history.moveToEnd();
                                    setBufferKeepPos(searchTerm.toString());
                                } else if (index < history.size()) {
                                    searchIndex = searchForwards(searchTerm.toString(), index, true);
                                    if (searchIndex == -1) {
                                        beep();
                                    } else {
                                        // Maintain cursor position while searching.
                                        success = history.moveTo(searchIndex);
                                        if (success) {
                                            setBufferKeepPos(history.current());
                                        }
                                    }
                                }
                                break;

                            case REVERSE_SEARCH_HISTORY:
                                originalBuffer = new CursorBuffer();
                                originalBuffer.write(buf.buffer);
                                originalBuffer.cursor = buf.cursor;
                                if (searchTerm != null) {
                                    previousSearchTerm = searchTerm.toString();
                                }
                                searchTerm = new StringBuffer(buf.buffer);
                                state = State.SEARCH;
                                if (searchTerm.length() > 0) {
                                    searchIndex = searchBackwards(searchTerm.toString());
                                    if (searchIndex == -1) {
                                        beep();
                                    }
                                    printSearchStatus(searchTerm.toString(),
                                            searchIndex > -1 ? history.get(searchIndex).toString() : "");
                                } else {
                                    searchIndex = -1;
                                    printSearchStatus("", "");
                                }
                                break;

                            case FORWARD_SEARCH_HISTORY:
                                originalBuffer = new CursorBuffer();
                                originalBuffer.write(buf.buffer);
                                originalBuffer.cursor = buf.cursor;
                                if (searchTerm != null) {
                                    previousSearchTerm = searchTerm.toString();
                                }
                                searchTerm = new StringBuffer(buf.buffer);
                                state = State.FORWARD_SEARCH;
                                if (searchTerm.length() > 0) {
                                    searchIndex = searchForwards(searchTerm.toString());
                                    if (searchIndex == -1) {
                                        beep();
                                    }
                                    printForwardSearchStatus(searchTerm.toString(),
                                            searchIndex > -1 ? history.get(searchIndex).toString() : "");
                                } else {
                                    searchIndex = -1;
                                    printForwardSearchStatus("", "");
                                }
                                break;

                            case CAPITALIZE_WORD:
                                success = capitalizeWord();
                                break;

                            case UPCASE_WORD:
                                success = upCaseWord();
                                break;

                            case DOWNCASE_WORD:
                                success = downCaseWord();
                                break;

                            case END_OF_LINE:
                                success = moveToEnd();
                                break;

                            case TAB_INSERT:
                                putString( "\t" );
                                break;

                            case RE_READ_INIT_FILE:
                                consoleKeys.loadKeys(appName, inputrc);
                                break;

                            case START_KBD_MACRO:
                                recording = true;
                                break;

                            case END_KBD_MACRO:
                                recording = false;
                                macro = macro.substring(0, macro.length() - opBuffer.length());
                                break;

                            case CALL_LAST_KBD_MACRO:
                                for (int i = 0; i < macro.length(); i++) {
                                    pushBackChar.push(macro.charAt(macro.length() - 1 - i));
                                }
                                opBuffer.setLength(0);
                                break;

                            case VI_EDITING_MODE:
                                consoleKeys.setKeyMap(KeyMap.VI_INSERT);
                                break;

                            case VI_MOVEMENT_MODE:
                                /*
                                 * If we are re-entering move mode from an
                                 * aborted yank-to, delete-to, change-to then
                                 * don't move the cursor back. The cursor is
                                 * only move on an expclit entry to movement
                                 * mode.
                                 */
                                if (state == State.NORMAL) {
                                    moveCursor(-1);
                                }
                                consoleKeys.setKeyMap(KeyMap.VI_MOVE);
                                break;

                            case VI_INSERTION_MODE:
                                consoleKeys.setKeyMap(KeyMap.VI_INSERT);
                                break;

                            case VI_APPEND_MODE:
                                moveCursor(1);
                                consoleKeys.setKeyMap(KeyMap.VI_INSERT);
                                break;

                            case VI_APPEND_EOL:
                                success = moveToEnd();
                                consoleKeys.setKeyMap(KeyMap.VI_INSERT);
                                break;

                            /*
                             * Handler for CTRL-D. Attempts to follow readline
                             * behavior. If the line is empty, then it is an EOF
                             * otherwise it is as if the user hit enter.
                             */
                            case VI_EOF_MAYBE:
                                if (buf.buffer.length() == 0) {
                                    println();
                                    flush();
                                    throw new EOFException();
                                }
                                return accept();

                            case TRANSPOSE_CHARS:
                                success = transposeChars(count);
                                break;

                            case INSERT_COMMENT:
                                return insertComment (false);

                            case INSERT_CLOSE_CURLY:
                                insertClose("}");
                                break;

                            case INSERT_CLOSE_PAREN:
                                insertClose(")");
                                break;

                            case INSERT_CLOSE_SQUARE:
                                insertClose("]");
                                break;

                            case VI_INSERT_COMMENT:
                                return insertComment (true);

                            case VI_MATCH:
                                success = viMatch ();
                                break;

                            case VI_SEARCH:
                                int lastChar = viSearch(opBuffer.charAt(0));
                                if (lastChar != -1) {
                                    pushBackChar.push((char)lastChar);
                                }
                                break;

                            case VI_ARG_DIGIT:
                                repeatCount = (repeatCount * 10) + opBuffer.charAt(0) - '0';
                                isArgDigit = true;
                                break;

                            case VI_BEGINNING_OF_LINE_OR_ARG_DIGIT:
                                if (repeatCount > 0) {
                                    repeatCount = (repeatCount * 10) + opBuffer.charAt(0) - '0';
                                    isArgDigit = true;
                                }
                                else {
                                    success = setCursorPosition(0);
                                }
                                break;

                            case VI_FIRST_PRINT:
                                success = setCursorPosition(0) && viNextWord(1);
                                break;

                            case VI_PREV_WORD:
                                success = viPreviousWord(count);
                                break;

                            case VI_NEXT_WORD:
                                success = viNextWord(count);
                                break;

                            case VI_END_WORD:
                                success = viEndWord(count);
                                break;

                            case VI_INSERT_BEG:
                                success = setCursorPosition(0);
                                consoleKeys.setKeyMap(KeyMap.VI_INSERT);
                                break;

                            case VI_RUBOUT:
                                success = viRubout(count);
                                break;

                            case VI_DELETE:
                                success = viDelete(count);
                                break;

                            case VI_DELETE_TO:
                                /*
                                 * This is a weird special case. In vi
                                 * "dd" deletes the current line. So if we
                                 * get a delete-to, followed by a delete-to,
                                 * we delete the line.
                                 */
                                if (state == State.VI_DELETE_TO) {
                                    success = setCursorPosition(0) && killLine();
                                    state = origState = State.NORMAL;
                                }
                                else {
                                    state = State.VI_DELETE_TO;
                                }
                                break;

                            case VI_YANK_TO:
                                // Similar to delete-to, a "yy" yanks the whole line.
                                if (state == State.VI_YANK_TO) {
                                    yankBuffer = buf.buffer.toString();
                                    state = origState = State.NORMAL;
                                }
                                else {
                                    state = State.VI_YANK_TO;
                                }
                                break;

                            case VI_CHANGE_TO:
                                if (state == State.VI_CHANGE_TO) {
                                    success = setCursorPosition(0) && killLine();
                                    state = origState = State.NORMAL;
                                    consoleKeys.setKeyMap(KeyMap.VI_INSERT);
                                }
                                else {
                                    state = State.VI_CHANGE_TO;
                                }
                                break;

                            case VI_KILL_WHOLE_LINE:
                                success = setCursorPosition(0) && killLine();
                                consoleKeys.setKeyMap(KeyMap.VI_INSERT);
                                break;

                            case VI_PUT:
                                success = viPut(count);
                                break;

                            case VI_CHAR_SEARCH: {
                                 // ';' and ',' don't need another character. They indicate repeat next or repeat prev.
                                int searchChar = (c != ';' && c != ',')
                                    ? (pushBackChar.isEmpty()
                                        ? readCharacter()
                                        : pushBackChar.pop ())
                                    : 0;

                                    success = viCharSearch(count, c, searchChar);
                                }
                                break;

                            case VI_CHANGE_CASE:
                                success = viChangeCase(count);
                                break;

                            case VI_CHANGE_CHAR:
                                success = viChangeChar(count,
                                    pushBackChar.isEmpty()
                                        ? readCharacter()
                                        : pushBackChar.pop());
                                break;

                            case VI_DELETE_TO_EOL:
                                success = viDeleteTo(buf.cursor, buf.buffer.length(), false);
                                break;

                            case VI_CHANGE_TO_EOL:
                                success = viDeleteTo(buf.cursor, buf.buffer.length(), true);
                                consoleKeys.setKeyMap(KeyMap.VI_INSERT);
                                break;

                            case EMACS_EDITING_MODE:
                                consoleKeys.setKeyMap(KeyMap.EMACS);
                                break;

                            case QUIT:
                                getCursorBuffer().clear();
                                return accept();

                            case QUOTED_INSERT:
                                quotedInsert = true;
                                break;

                            case PASTE_FROM_CLIPBOARD:
                                paste();
                                break;

                            default:
                                break;
                        }

                        /*
                         * If we were in a yank-to, delete-to, move-to
                         * when this operation started, then fall back to
                         */
                        if (origState != State.NORMAL) {
                            if (origState == State.VI_DELETE_TO) {
                                success = viDeleteTo(cursorStart, buf.cursor, false);
                            }
                            else if (origState == State.VI_CHANGE_TO) {
                                success = viDeleteTo(cursorStart, buf.cursor, true);
                                consoleKeys.setKeyMap(KeyMap.VI_INSERT);
                            }
                            else if (origState == State.VI_YANK_TO) {
                                success = viYankTo(cursorStart, buf.cursor);
                            }
                            state = State.NORMAL;
                        }

                        /*
                         * Another subtly. The check for the NORMAL state is
                         * to ensure that we do not clear out the repeat
                         * count when in delete-to, yank-to, or move-to modes.
                         */
                        if (state == State.NORMAL && !isArgDigit) {
                            /*
                             * If the operation performed wasn't a vi argument
                             * digit, then clear out the current repeatCount;
                             */
                            repeatCount = 0;
                        }

                        if (state != State.SEARCH && state != State.FORWARD_SEARCH) {
                            originalBuffer = null;
                            previousSearchTerm = "";
                            searchTerm = null;
                            searchIndex = -1;
                        }
                    }
                }
                if (!success) {
                    beep();
                }
                opBuffer.setLength(0);

                flush();
            }
        } catch (InterruptedIOException e) {
            try {
                println();
                flush();
            } catch (Exception e2) {
                // Ignore
            }
            String partialLine = buf.buffer.toString();
            buf.clear();
            history.moveToEnd();
            throw new UserInterruptException(partialLine);
        } catch (UserInterruptException | EOFException e) {
            throw e;
        } catch (IOException e) {
            throw new IOError(e);
        }
        finally {
            if (originalAttributes != null) {
                console.setAttributes(originalAttributes);
            }
            if (previousIntrHandler != null) {
                console.handle(Signal.INT, previousIntrHandler);
            }
        }
    }

    //
    // Completion
    //

    /**
     * Add the specified {@link Completer} to the list of handlers for tab-completion.
     *
     * @param completer the {@link Completer} to add
     * @return true if it was successfully added
     */
    public boolean addCompleter(final Completer completer) {
        return completers.add(completer);
    }

    /**
     * Remove the specified {@link Completer} from the list of handlers for tab-completion.
     *
     * @param completer     The {@link Completer} to remove
     * @return              True if it was successfully removed
     */
    public boolean removeCompleter(final Completer completer) {
        return completers.remove(completer);
    }

    public void setCompleters(Collection<Completer> completers) {
        checkNotNull(completers);
        this.completers.clear();
        this.completers.addAll(completers);
    }

    /**
     * Returns an unmodifiable list of all the completers.
     */
    public Collection<Completer> getCompleters() {
        return Collections.unmodifiableList(completers);
    }

    public void setCompletionHandler(final CompletionHandler handler) {
        this.completionHandler = checkNotNull(handler);
    }

    public CompletionHandler getCompletionHandler() {
        return this.completionHandler;
    }

    /**
     * Use the completers to modify the buffer with the appropriate completions.
     *
     * @return true if successful
     */
    protected boolean complete() throws IOException {
        // debug ("tab for (" + buf + ")");
        if (completers.size() == 0) {
            return false;
        }

        List<CharSequence> candidates = new LinkedList<>();
        String bufstr = buf.buffer.toString();
        int cursor = buf.cursor;

        int position = -1;

        for (Completer comp : completers) {
            if ((position = comp.complete(bufstr, cursor, candidates)) != -1) {
                break;
            }
        }

        return candidates.size() != 0 && getCompletionHandler().complete(this, candidates, position);
    }

    protected void printCompletionCandidates() throws IOException {
        // debug ("tab for (" + buf + ")");
        if (completers.size() == 0) {
            return;
        }

        List<CharSequence> candidates = new LinkedList<>();
        String bufstr = buf.buffer.toString();
        int cursor = buf.cursor;

        for (Completer comp : completers) {
            if (comp.complete(bufstr, cursor, candidates) != -1) {
                break;
            }
        }
        printCandidates(candidates);
        drawLine();
    }

    //
    // History
    //

    public void setHistory(final History history) {
        this.history = history;
    }

    public History getHistory() {
        return history;
    }

    /**
     * Used in "vi" mode for argumented history move, to move a specific
     * number of history entries forward or back.
     *
     * @param next If true, move forward
     * @param count The number of entries to move
     * @return true if the move was successful
     */
    private boolean moveHistory(final boolean next, int count) throws IOException {
        boolean ok = true;
        for (int i = 0; i < count && (ok = moveHistory(next)); i++) {
            /* empty */
        }
        return ok;
    }

    /**
     * Move up or down the history tree.
     */
    private boolean moveHistory(final boolean next) throws IOException {
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
     * Output the specified characters to the output stream without manipulating the current buffer.
     */
    private int print(final CharSequence buff, int cursorPos) throws IOException {
        return print(buff, 0, buff.length(), cursorPos);
    }

    private int print(final CharSequence buff, int start, int end) throws IOException {
        return print(buff, start, end, getCursorPosition());
    }

    private int print(final CharSequence buff, int start, int end, int cursorPos) throws IOException {
        checkNotNull(buff);
        for (int i = start; i < end; i++) {
            char c = buff.charAt(i);
            if (c == '\t') {
                int nb = nextTabStop(cursorPos);
                cursorPos += nb;
                while (nb-- > 0) {
                    console.writer().write(' ');
                }
            } else if (c < 32) {
                console.writer().write('^');
                console.writer().write((char) (c + '@'));
                cursorPos += 2;
            } else {
                int w = WCWidth.wcwidth(c);
                if (w > 0) {
                    console.writer().write(c);
                    cursorPos += w;
                }
            }
        }
        cursorOk = false;
        return cursorPos;
    }

    /**
     * Output the specified string to the output stream (but not the buffer).
     */
    public void print(final CharSequence s) throws IOException {
        print(s, getCursorPosition());
    }

    public void println(final CharSequence s) throws IOException {
        print(s);
        println();
    }

    /**
     * Output a platform-dependant newline.
     */
    public void println() throws IOException {
        console.puts(Capability.carriage_return);
        rawPrint('\n');
    }

    /**
     * Raw output printing
     */
    final void rawPrint(final int c) throws IOException {
        console.writer().write(c);
        cursorOk = false;
    }

    final void rawPrint(final String str) throws IOException {
        console.writer().write(str);
        cursorOk = false;
    }

    private void rawPrint(final char c, final int num) throws IOException {
        for (int i = 0; i < num; i++) {
            rawPrint(c);
        }
    }

    private void rawPrintln(final String s) throws IOException {
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
    public boolean delete() throws IOException {
        if (buf.cursor == buf.buffer.length()) {
          return false;
        }

        buf.buffer.delete(buf.cursor, buf.cursor + 1);
        drawBuffer(1);

        return true;
    }

    /**
     * Kill the buffer ahead of the current cursor position.
     *
     * @return true if successful
     */
    public boolean killLine() throws IOException {
        int cp = buf.cursor;
        int len = buf.buffer.length();

        if (cp >= len) {
            return false;
        }

        int num = len - cp;
        int pos = getCursorPosition();
        int width = wcwidth(buf.buffer, cp, len, pos);
        clearAhead(width, pos);

        char[] killed = new char[num];
        buf.buffer.getChars(cp, (cp + num), killed, 0);
        buf.buffer.delete(cp, (cp + num));

        String copy = new String(killed);
        killRing.add(copy);

        return true;
    }

    public boolean yank() throws IOException {
        String yanked = killRing.yank();

        if (yanked == null) {
            return false;
        }
        putString(yanked);
        return true;
    }

    public boolean yankPop() throws IOException {
        if (!killRing.lastYank()) {
            return false;
        }
        String current = killRing.yank();
        if (current == null) {
            // This shouldn't happen.
            return false;
        }
        backspace(current.length());
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
    public boolean clearScreen() throws IOException {
        if (!console.puts(Capability.clear_screen)) {
            println();
        }
        return true;
    }

    /**
     * Issue an audible keyboard bell.
     */
    public void beep() throws IOException {
        int bell_preference = AUDIBLE_BELL;
        String bellStyle = getVariable(BELL_STYLE);
        if ("none".equals(bellStyle) || "off".equals(bellStyle)) {
            bell_preference = NO_BELL;
        } else if ("audible".equals(bellStyle)) {
            bell_preference = AUDIBLE_BELL;
        } else if ("visible".equals(bellStyle)) {
            bell_preference = VISIBLE_BELL;
        } else if ("on".equals(bellStyle)) {
            String preferVisibleBellStr = getVariable(PREFER_VISIBLE_BELL);
            if ("off".equals(preferVisibleBellStr)) {
                bell_preference = AUDIBLE_BELL;
            } else {
                bell_preference = VISIBLE_BELL;
            }
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
    public boolean paste() throws IOException {
        Clipboard clipboard;
        try { // May throw ugly exception on system without X
            clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
        }
        catch (Exception e) {
            return false;
        }

        if (clipboard == null) {
            return false;
        }

        Transferable transferable = clipboard.getContents(null);

        if (transferable == null) {
            return false;
        }

        try {
            @SuppressWarnings("deprecation")
            Object content = transferable.getTransferData(DataFlavor.plainTextFlavor);

            // This fix was suggested in bug #1060649 at
            // http://sourceforge.net/tracker/index.php?func=detail&aid=1060649&group_id=64033&atid=506056
            // to get around the deprecated DataFlavor.plainTextFlavor, but it
            // raises a UnsupportedFlavorException on Mac OS X

            if (content == null) {
                try {
                    content = new DataFlavor().getReaderForText(transferable);
                }
                catch (Exception e) {
                    // ignore
                }
            }

            if (content == null) {
                return false;
            }

            String value;

            if (content instanceof java.io.Reader) {
                // TODO: we might want instead connect to the input stream
                // so we can interpret individual lines
                value = "";
                String line;

                BufferedReader read = new BufferedReader((java.io.Reader) content);
                while ((line = read.readLine()) != null) {
                    if (value.length() > 0) {
                        value += "\n";
                    }

                    value += line;
                }
            }
            else {
                value = content.toString();
            }

            if (value == null) {
                return true;
            }

            putString(value);

            return true;
        }
        catch (UnsupportedFlavorException e) {
            Log.error("Paste failed: ", e);

            return false;
        }
    }

    /**
     * Adding a triggered Action allows to give another curse of action if a character passed the pre-processing.
     * <p/>
     * Say you want to close the application if the user enter q.
     * addTriggerAction('q', new ActionListener(){ System.exit(0); }); would do the trick.
     */
    public void addTriggeredAction(final char c, final ActionListener listener) {
        getKeys().bind(Character.toString(c), listener);
    }

    //
    // Formatted Output
    //

    /**
     * Print out the candidates. If the size of the candidates is greater than the
     * {@link ConsoleReader#COMPLETION_QUERY_ITEMS}, they prompt with a warning.
     *
     * @param candidates the list of candidates to print
     */
    public void printCandidates(Collection<CharSequence> candidates) throws
            IOException
    {
        Set<CharSequence> distinct = new HashSet<>(candidates);

        int max = getInt(COMPLETION_QUERY_ITEMS, 100);
        if (max > 0 && distinct.size() >= max) {
            println();
            print(Messages.DISPLAY_CANDIDATES.format(candidates.size()));
            flush();

            int c;

            String noOpt = Messages.DISPLAY_CANDIDATES_NO.format();
            String yesOpt = Messages.DISPLAY_CANDIDATES_YES.format();
            char[] allowed = {yesOpt.charAt(0), noOpt.charAt(0)};

            while ((c = readCharacter(allowed)) != -1) {
                String tmp = new String(new char[]{(char) c});

                if (noOpt.startsWith(tmp)) {
                    println();
                    return;
                }
                else if (yesOpt.startsWith(tmp)) {
                    break;
                }
                else {
                    beep();
                }
            }
        }

        // copy the values and make them distinct, without otherwise affecting the ordering. Only do it if the sizes differ.
        if (distinct.size() != candidates.size()) {
            Collection<CharSequence> copy = new ArrayList<CharSequence>();

            for (CharSequence next : candidates) {
                if (!copy.contains(next)) {
                    copy.add(next);
                }
            }

            candidates = copy;
        }

        println();
        printColumns(candidates);
    }

    /**
     * Output the specified {@link Collection} in proper columns.
     */
    public void printColumns(final Collection<? extends CharSequence> items) throws IOException {
        if (items == null || items.isEmpty()) {
            return;
        }

        int width = size.getColumns();
        int height = size.getRows();

        int maxWidth = 0;
        for (CharSequence item : items) {
            // we use 0 here, as we don't really support tabulations inside candidates
            int len = wcwidth(Ansi.stripAnsi(item.toString()), 0);
            maxWidth = Math.max(maxWidth, len);
        }
        maxWidth = maxWidth + 3;
        Log.debug("Max width: ", maxWidth);

        int showLines;
        if (getBoolean(PAGE_COMPLETIONS, true)) {
            showLines = height - 1; // page limit
        }
        else {
            showLines = Integer.MAX_VALUE;
        }

        StringBuilder buff = new StringBuilder();
        int realLength = 0;
        for (CharSequence item : items) {
            if ((realLength + maxWidth) > width) {
                rawPrintln(buff.toString());
                buff.setLength(0);
                realLength = 0;

                if (--showLines == 0) {
                    // Overflow
                    String more = Messages.DISPLAY_MORE.format();
                    print(more);
                    flush();
                    int c = readCharacter();
                    if (c == '\r' || c == '\n') {
                        // one step forward
                        showLines = 1;
                    }
                    else if (c != 'q') {
                        // page forward
                        showLines = height - 1;
                    }

                    back(more.length());
                    if (c == 'q') {
                        // cancel
                        break;
                    }
                }
            }

            // NOTE: toString() is important here due to AnsiString being retarded
            buff.append(item.toString());
            int strippedItemLength = wcwidth(Ansi.stripAnsi(item.toString()), 0);
            for (int i = 0; i < (maxWidth - strippedItemLength); i++) {
                buff.append(' ');
            }
            realLength += maxWidth;
        }

        if (buff.length() > 0) {
            rawPrintln(buff.toString());
        }
    }

    /**
     * Erases the current line with the existing prompt, then redraws the line
     * with the provided prompt and buffer
     * @param prompt
     *            the new prompt
     * @param buffer
     *            the buffer to be drawn
     * @param cursorDest
     *            where you want the cursor set when the line has been drawn.
     *            -1 for end of line.
     * */
    public void resetPromptLine(String prompt, String buffer, int cursorDest) throws IOException {
        // move cursor to end of line
        moveToEnd();

        // backspace all text, including prompt
        buf.buffer.append(this.prompt);
        int promptLength = 0;
        if (this.prompt != null) {
            promptLength = this.prompt.length();
        }

        buf.cursor += promptLength;
        setPrompt("");
        backspaceAll();

        setPrompt(prompt);
        redrawLine();
        setBuffer(buffer);

        // move cursor to destination (-1 will move to end of line)
        if (cursorDest < 0) cursorDest = buffer.length();
        setCursorPosition(cursorDest);

        flush();
    }

    public void printSearchStatus(String searchTerm, String match) throws IOException {
        printSearchStatus(searchTerm, match, "(reverse-i-search)`");
    }

    public void printForwardSearchStatus(String searchTerm, String match) throws IOException {
        printSearchStatus(searchTerm, match, "(i-search)`");
    }

    private void printSearchStatus(String searchTerm, String match, String searchLabel) throws IOException {
        String prompt = searchLabel + searchTerm + "': ";
        int cursorDest = match.indexOf(searchTerm);
        resetPromptLine(prompt, match, cursorDest);
    }

    public void restoreLine(String originalPrompt, int cursorDest) throws IOException {
        // TODO move cursor to matched string
        String prompt = lastLine(originalPrompt);
        String buffer = buf.buffer.toString();
        resetPromptLine(prompt, buffer, cursorDest);
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
        return searchBackwards(searchTerm, history.index());
    }


    public int searchBackwards(String searchTerm, int startIndex, boolean startsWith) {
        ListIterator<History.Entry> it = history.entries(startIndex);
        while (it.hasPrevious()) {
            History.Entry e = it.previous();
            if (startsWith) {
                if (e.value().toString().startsWith(searchTerm)) {
                    return e.index();
                }
            } else {
                if (e.value().toString().contains(searchTerm)) {
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
                if (e.value().toString().startsWith(searchTerm)) {
                    return e.index();
                }
            } else {
                if (e.value().toString().contains(searchTerm)) {
                    return e.index();
                }
            }
        }
        return -1;
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
    private boolean isDelimiter(final char c) {
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
    private boolean isWhitespace(final char c) {
        return Character.isWhitespace(c);
    }

    public String getVariable(String name) {
        String v = variables.get(name);
        return v != null ? v : consoleKeys.getVariable(name);
    }

    public void setVariable(String name, String value) {
        variables.put(name, value);
    }

    boolean getBoolean(String name, boolean def) {
        String v = getVariable(name);
        return v != null ? v.isEmpty() || v.equalsIgnoreCase("on") || v.equalsIgnoreCase("1") : def;
    }

    int getInt(String name, int def) {
        int nb = def;
        String v = getVariable(name);
        if (v != null) {
            nb = 0;
            try {
                nb = Integer.parseInt(v);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return nb;
    }

    long getLong(String name, long def) {
        long nb = def;
        String v = getVariable(name);
        if (v != null) {
            nb = 0;
            try {
                nb = Long.parseLong(v);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return nb;
    }

}
