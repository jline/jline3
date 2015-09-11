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
import java.io.BufferedReader;
import java.io.File;
import java.io.Flushable;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
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
import org.jline.reader.history.MemoryHistory;
import org.jline.utils.Ansi;
import org.jline.utils.Ansi.Attribute;
import org.jline.utils.Ansi.Color;
import org.jline.utils.AnsiHelper;
import org.jline.utils.DiffHelper;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.Levenshtein;
import org.jline.utils.Log;
import org.jline.utils.NonBlockingReader;
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

    public static final int TAB_WIDTH = 8;

    public static final long COPY_PASTE_DETECTION_TIMEOUT = 50l;
    public static final long BLINK_MATCHING_PAREN_TIMEOUT = 500l;

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

    protected enum Messages
    {
        DISPLAY_CANDIDATES,
        DISPLAY_CANDIDATES_YES,
        DISPLAY_CANDIDATES_NO,
        DISPLAY_MORE;

        protected static final ResourceBundle bundle =
                ResourceBundle.getBundle(ConsoleReaderImpl.class.getName(), Locale.getDefault());

        public String format(final Object... args) {
            if (bundle == null)
                return "";
            else
                return String.format(bundle.getString(name()), args);
        }
    }

    protected static final int NO_BELL = 0;
    protected static final int AUDIBLE_BELL = 1;
    protected static final int VISIBLE_BELL = 2;


    //
    // Constructor variables
    //

    /** The console to use */
    protected final Console console;
    /** The inputrc url */
    protected final URL inputrc;
    /** The application name, used when parsing the inputrc */
    protected final String appName;
    /** The console keys mapping */
    protected final ConsoleKeys consoleKeys;



    //
    // Configuration
    //
    protected final Map<String, String> variables = new HashMap<>();
    protected History history = new MemoryHistory();
    protected final List<Completer> completers = new LinkedList<>();
    protected Highlighter highlighter = new DefaultHighlighter();
    protected Parser parser = new DefaultParser();

    //
    // State variables
    //

    protected final Buffer buf = new Buffer();

    protected final Size size = new Size();

    protected String prompt;
    protected int    promptLen;
    protected String rightPrompt;

    protected Character mask;

    protected StringBuffer searchTerm = null;
    protected int searchIndex = -1;


    // Reading buffers
    protected final StringBuilder opBuffer = new StringBuilder();
    protected final Stack<Integer> pushBackChar = new Stack<>();


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

    protected boolean recording;

    protected String macro = "";

    /*
     * Current internal state of the line reader
     */
    protected State   state = State.NORMAL;

    protected String oldBuf;
    protected int oldColumns;
    protected String oldPrompt;
    protected String[][] oldPost;
    protected String oldRightPrompt;

    protected String[][] post;

    protected int cursorPos;
    protected boolean cursorOk;


    protected Map<Operation, Widget> dispatcher;

    protected int count;
    protected int repeatCount;
    protected boolean isArgDigit;

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
        dispatcher = createDispatcher();
    }

    /**
     * Bind special chars defined by the console instead of
     * the default bindings
     */
    protected static void bindConsoleChars(KeyMap keyMap, Attributes attr) {
        if (attr != null) {
            rebind(keyMap, Operation.BACKWARD_DELETE_CHAR,
                           /* C-? */ (char) 127, (char) attr.getControlChar(ControlChar.VERASE));
            rebind(keyMap, Operation.UNIX_WORD_RUBOUT,
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

    public URL getInputrc() {
        return inputrc;
    }

    public KeyMap getKeys() {
        return consoleKeys.getKeys();
    }

    public Buffer getCursorBuffer() {
        return buf;
    }

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
            previousWinchHandler = console.handle(Signal.WINCH, signal -> {
                // TODO: fix possible threading issue
                size.copy(console.getSize());
                redisplay();
            });
            originalAttributes = console.enterRawMode();

            this.mask = mask;

            /*
             * This is the accumulator for VI-mode repeat count. That is, while in
             * move mode, if you type 30x it will delete 30 characters. This is
             * where the "30" is accumulated until the command is struck.
             */
            repeatCount = 0;

            state = State.NORMAL;

            pushBackChar.clear();

            size.copy(console.getSize());
            cursorPos = 0;

            setPrompt(prompt);
            setRightPrompt(rightPrompt);
            buf.clear();
            if (buffer != null) {
                buf.write(buffer);
            }

            // Draw initial prompt
            redrawLine();
            redisplay();

            while (true) {

                Object o = readBinding(getKeys());
                if (o == null) {
                    return null;
                }
                Log.trace("Binding: ", o);

                // Handle macros
                if (o instanceof Macro) {
                    String macro = ((Macro) o).getSequence();
                    new StringBuilder(macro).reverse().codePoints().forEachOrdered(pushBackChar::push);
                    continue;
                }

                // Cache console size for the duration of the binding processing
                size.copy(console.getSize());
                // If this is still false after handling the binding, then
                // we reset our repeatCount to 0.
                isArgDigit = false;
                // Every command that can be repeated a specified number
                // of times, needs to know how many times to repeat, so
                // we figure that out here.
                count = (repeatCount == 0) ? 1 : repeatCount;

                if (o instanceof Operation) {
                    o = dispatcher.get(o);
                }

                // Handle custom callbacks
                // TODO: merge that with the usual dispatch
                if (o instanceof Widget) {
                    if (!((Widget) o).apply(this)) {
                        beep();
                    }
                } else {
                    // TODO: what should we do there ?
                    beep();
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

    //
    // Helper methods
    //

    protected void setPrompt(final String prompt) {
        this.prompt = prompt != null ? prompt : "";
        this.promptLen = getLastLineWidth(this.prompt);
    }

    private int getLastLineWidth(String str) {
        str = AnsiHelper.strip(str);
        int last = str.lastIndexOf("\n");
        if (last >= 0) {
            str = str.substring(last + 1, str.length());
        }
        return wcwidth(str, 0);
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
        return promptLen + wcwidth(buf.upToCursor(), promptLen);
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
        buf.write(buffer.substring(sameIndex)); // append the differences
    }

    protected void setBufferKeepPos(final String buffer) {
        int pos = buf.cursor();
        setBuffer(buffer);
        buf.cursor(pos);
    }

    /**
     * Clear the line and redraw it.
     */
    public void redrawLine() {
        oldBuf = "";
        oldPrompt = "";
        oldPost = null;
        oldColumns = size.getColumns();
        oldRightPrompt = "";
    }

    /**
     * Clear the buffer and add its contents to the history.
     *
     * @return the former contents of the buffer.
     */
    protected String finishBuffer() {
        String str = buf.toString();
        String historyLine = str;

        if (!isSet(DISABLE_EVENT_EXPANSION)) {
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
        buf.write(str);
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
        String comment = getVariable(COMMENT_BEGIN);
        if (comment == null) {
            comment = "#";
        }
        beginningOfLine();
        putString(comment);
        if (isViMode) {
            setKeyMap(KeyMap.VI_INSERT);
        }
        return acceptLine();
    }

    /**
     * Implements vi search ("/" or "?").
     */
    @SuppressWarnings("fallthrough")
    protected boolean viSearch() {
        int searchChar = opBuffer.codePointAt(0);
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
        pushBackChar.push(ch);

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

    protected int moveVisualCursorTo(int i1) {
        int i0 = cursorPos;
        if (i0 == i1) return i1;
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
        cursorPos = i1;
        cursorOk = true;
        return i1;
    }

    /**
     * Read a character from the console.
     *
     * @return the character, or -1 if an EOF is received.
     */
    public int readCharacter() {
        try {
            int c = NonBlockingReader.READ_EXPIRED;
            int s = 0;
            while (c == NonBlockingReader.READ_EXPIRED) {
                c = console.reader().read(100l);
                if (c >= 0 && Character.isHighSurrogate((char) c)) {
                    s = c;
                    c = NonBlockingReader.READ_EXPIRED;
                }
            }
            return s != 0 ? Character.toCodePoint((char) s, (char) c) : c;
        } catch (IOException e) {
            throw new IOError(e);
        }
    }
    
    public int peekCharacter(long timeout) {
        try {
            return console.reader().peek(timeout);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public int readCharacter(final int... allowed) {
        // if we restrict to a limited set and the current character is not in the set, then try again.
        int c;

        Arrays.sort(allowed); // always need to sort before binarySearch

        //noinspection StatementWithEmptyBody
        while (Arrays.binarySearch(allowed, c = readCharacter()) < 0) {
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
    public Object readBinding(KeyMap keys) {
        return readBinding(keys, null);
    }

    public Object readBinding(KeyMap keys, KeyMap local) {
        Object o = null;
        int[] remaining = new int[1];
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

            if (local != null) {
                o = local.getBound(opBuffer, remaining);
            }
            if (o == null && (local == null || remaining[0] >= 0)) {
                o = keys.getBound(opBuffer, remaining);
            }
            if (remaining[0] > 0) {
                int[] cps = opBuffer.codePoints().toArray();
                if (o != null) {
                    opBuffer.setLength(0);
                    opBuffer.append(new String(cps, 0, cps.length - remaining[0]));
                    for (int i = cps.length - 1; i >= cps.length - remaining[0]; i--) {
                        pushBackChar.push(cps[i]);
                    }
                } else {
                    opBuffer.setLength(0);
                }
            }

            /*
             * The kill ring keeps record of whether or not the
             * previous command was a yank or a kill. We reset
             * that state here if needed.
             */
            if (!recording && o != null) {
                if (o != Operation.YANK_POP && o != Operation.YANK) {
                    killRing.resetLastYank();
                }
                if (o != Operation.KILL_LINE && o != Operation.KILL_WHOLE_LINE
                        && o != Operation.BACKWARD_KILL_WORD && o != Operation.KILL_WORD
                        && o != Operation.UNIX_LINE_DISCARD && o != Operation.UNIX_WORD_RUBOUT) {
                    killRing.resetLastKill();
                }
            }

        } while (o == null);

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

    protected boolean viBeginningOfLineOrArgDigit() {
        if (repeatCount > 0) {
            return viArgDigit();
        } else {
            return beginningOfLine();
        }
    }

    protected boolean viArgDigit() {
        repeatCount = (repeatCount * 10) + opBuffer.charAt(0) - '0';
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
                Widget widget = dispatcher.get(op);
                if (widget != null && !widget.apply(this)) {
                    return false;
                }
                viMoveMode = ViMoveMode.NORMAL;
            }
            return viDeleteTo(cursorStart, buf.cursor());
        } else {
            opBuffer.reverse().codePoints().forEachOrdered(pushBackChar::push);
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
                Widget widget = dispatcher.get(op);
                if (widget != null && !widget.apply(this)) {
                    return false;
                }
                viMoveMode = ViMoveMode.NORMAL;
            }
            return viYankTo(cursorStart, buf.cursor());
        } else {
            opBuffer.reverse().codePoints().forEachOrdered(pushBackChar::push);
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
                Widget widget = dispatcher.get(op);
                if (widget != null && !widget.apply(this)) {
                    return false;
                }
                viMoveMode = ViMoveMode.NORMAL;
            }
            boolean res = viChangeTo(cursorStart, buf.cursor());
            consoleKeys.setKeyMap(KeyMap.VI_INSERT);
            return res;
        } else {
            opBuffer.reverse().codePoints().forEachOrdered(pushBackChar::push);
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

        KeyMap terminators = new KeyMap("terminators");
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
                            searchTerm.append(opBuffer);
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
                            opBuffer.reverse().codePoints().forEachOrdered(pushBackChar::push);
                            return true;
                    }
                } else {
                    opBuffer.reverse().codePoints().forEachOrdered(pushBackChar::push);
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

    protected boolean historySearchForward() {
        try {
            searchTerm = new StringBuffer(buf.upToCursor());
            int index = history.index() + 1;

            if (index == history.size()) {
                history.moveToEnd();
                setBufferKeepPos(searchTerm.toString());
            } else if (index < history.size()) {
                searchIndex = searchForwards(searchTerm.toString(), index, true);
                if (searchIndex == -1) {
                    return false;
                } else {
                    // Maintain cursor position while searching.
                    if (history.moveTo(searchIndex)) {
                        setBufferKeepPos(history.current());
                    } else {
                        return false;
                    }
                }
            }
            return true;
        } finally {
            searchIndex = -1;
            searchTerm = null;
        }
    }

    protected boolean historySearchBackward() {
        try {
            searchTerm = new StringBuffer(buf.upToCursor());
            searchIndex = searchBackwards(searchTerm.toString(), history.index(), true);

            if (searchIndex == -1) {
                return false;
            } else {
                // Maintain cursor position while searching.
                if (history.moveTo(searchIndex)) {
                    setBufferKeepPos(history.current());
                } else {
                    return false;
                }
            }
            return true;
        } finally {
            searchIndex = -1;
            searchTerm = null;
        }
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
        post = new String[][] { new String[] { searchLabel + ": " + searchTerm + "_" } };
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
        setKeyMap(KeyMap.VI_INSERT);
        return acceptLine();
    }

    protected boolean acceptLine() {
        String str = buf.toString();
        ParsedLine line = parser.parse(str, buf.cursor());
        if (line.complete()) {
            state = State.DONE;
            if (!isSet(DISABLE_EVENT_EXPANSION)) {
                try {
                    String exp = expandEvents(str);
                    if (!exp.equals(str)) {
                        buf.clear();
                        buf.write(exp);
                        if (isSet("history-verify")) {
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
        } else {
            buf.write("\n");
        }
        return true;
    }

    protected boolean selfInsert() {
        putString(opBuffer);
        return true;
    }

    protected boolean selfInsertUnmeta() {
        if (opBuffer.charAt(0) == KeyMap.ESCAPE) {
            String s = opBuffer.substring(1);
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
        buf.overTyping(!buf.overTyping());
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
        return setKeyMap(KeyMap.VI_MOVE);
    }

    protected boolean viInsertionMode() {
        return setKeyMap(KeyMap.VI_INSERT);
    }

    protected boolean  viAppendMode() {
        buf.move(1);
        return setKeyMap(KeyMap.VI_INSERT);
    }

    protected boolean viAppendEol() {
        return endOfLine() && setKeyMap(KeyMap.VI_INSERT);
    }

    protected boolean emacsEditingMode() {
        return setKeyMap(KeyMap.EMACS);
    }

    protected boolean viChangeToEol() {
        return viChangeTo(buf.cursor(), buf.length())
                && setKeyMap(KeyMap.VI_INSERT);
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
        int c = opBuffer.codePointAt(0);
        int searchChar = (c != ';' && c != ',')
                ? (pushBackChar.isEmpty()
                ? readCharacter()
                : pushBackChar.pop())
                : 0;

        return doViCharSearch(count, c, searchChar);
    }

    protected boolean viKillWholeLine() {
        return killWholeLine() && setKeyMap(KeyMap.VI_INSERT);
    }

    protected boolean viInsertBeg() {
        return beginningOfLine() && setKeyMap(KeyMap.VI_INSERT);
    }

    protected boolean backwardDeleteChar() {
        return buf.backspace();
    }

    protected boolean viEditingMode() {
        return setKeyMap(KeyMap.VI_INSERT);
    }

    protected boolean callLastKbdMacro() {
        new StringBuilder(macro).reverse().codePoints().forEachOrdered(pushBackChar::push);
        return true;
    }

    protected boolean endKbdMacro() {
        recording = false;
        macro = macro.substring(0, macro.length() - opBuffer.length());
        return true;
    }

    protected boolean startKbdMacro() {
        recording = true;
        return true;
    }

    protected boolean reReadInitFile() {
        consoleKeys.loadKeys(appName, inputrc);
        return true;
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
        buf.cursor(0);
        return true;
    }

    protected boolean endOfLine() {
        buf.cursor(buf.length());
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
        int c = pushBackChar.isEmpty() ? readCharacter() : pushBackChar.pop();
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
        int old = opBuffer.codePointBefore(opBuffer.length());
        opBuffer.setLength(opBuffer.length() - Character.charCount(old));
        opBuffer.appendCodePoint(Character.toLowerCase(old));
        int[] codepoints = opBuffer.toString().codePoints().toArray();
        for (int i = codepoints.length - 1; i >= 0; i--) {
            pushBackChar.add(codepoints[i]);
        }
        return true;
    }

    protected Map<Operation, Widget> createDispatcher() {
        Map<Operation, Widget> dispatcher = new HashMap<>();
        dispatcher.put(Operation.PASTE_FROM_CLIPBOARD, ConsoleReaderImpl::pasteFromClipboard);
        dispatcher.put(Operation.BACKWARD_KILL_WORD, ConsoleReaderImpl::backwardKillWord);
        dispatcher.put(Operation.KILL_WORD, ConsoleReaderImpl::killWord);
        dispatcher.put(Operation.TRANSPOSE_CHARS, ConsoleReaderImpl::transposeChars);
        dispatcher.put(Operation.INSERT_CLOSE_CURLY, ConsoleReaderImpl::insertCloseCurly);
        dispatcher.put(Operation.INSERT_CLOSE_PAREN, ConsoleReaderImpl::insertCloseParen);
        dispatcher.put(Operation.INSERT_CLOSE_SQUARE, ConsoleReaderImpl::insertCloseSquare);
        dispatcher.put(Operation.CLEAR_SCREEN, ConsoleReaderImpl::clearScreen);
        dispatcher.put(Operation.VI_MATCH, ConsoleReaderImpl::viMatch);
        dispatcher.put(Operation.VI_FIRST_PRINT, ConsoleReaderImpl::viFirstPrint);
        dispatcher.put(Operation.VI_PREV_WORD, ConsoleReaderImpl::viPreviousWord);
        dispatcher.put(Operation.VI_NEXT_WORD, ConsoleReaderImpl::viNextWord);
        dispatcher.put(Operation.VI_END_WORD, ConsoleReaderImpl::viEndWord);
        dispatcher.put(Operation.VI_RUBOUT, ConsoleReaderImpl::viRubout);
        dispatcher.put(Operation.VI_DELETE, ConsoleReaderImpl::viDelete);
        dispatcher.put(Operation.VI_PUT, ConsoleReaderImpl::viPut);
        dispatcher.put(Operation.VI_CHANGE_CASE, ConsoleReaderImpl::viChangeCase);
        dispatcher.put(Operation.CAPITALIZE_WORD, ConsoleReaderImpl::capitalizeWord);
        dispatcher.put(Operation.UPCASE_WORD, ConsoleReaderImpl::upCaseWord);
        dispatcher.put(Operation.DOWNCASE_WORD, ConsoleReaderImpl::downCaseWord);
        dispatcher.put(Operation.END_OF_LINE, ConsoleReaderImpl::endOfLine);
        dispatcher.put(Operation.DELETE_CHAR, ConsoleReaderImpl::deleteChar);
        dispatcher.put(Operation.BACKWARD_CHAR, ConsoleReaderImpl::backwardChar);
        dispatcher.put(Operation.FORWARD_CHAR, ConsoleReaderImpl::forwardChar);
        dispatcher.put(Operation.UNIX_LINE_DISCARD, ConsoleReaderImpl::unixLineDiscard);
        dispatcher.put(Operation.UNIX_WORD_RUBOUT, ConsoleReaderImpl::unixWordRubout);
        dispatcher.put(Operation.BEGINNING_OF_LINE, ConsoleReaderImpl::beginningOfLine);
        dispatcher.put(Operation.YANK, ConsoleReaderImpl::yank);
        dispatcher.put(Operation.YANK_POP, ConsoleReaderImpl::yankPop);
        dispatcher.put(Operation.KILL_LINE, ConsoleReaderImpl::killLine);
        dispatcher.put(Operation.KILL_WHOLE_LINE, ConsoleReaderImpl::killWholeLine);
        dispatcher.put(Operation.BACKWARD_WORD, ConsoleReaderImpl::backwardWord);
        dispatcher.put(Operation.FORWARD_WORD, ConsoleReaderImpl::forwardWord);
        dispatcher.put(Operation.PREVIOUS_HISTORY, ConsoleReaderImpl::previousHistory);
        dispatcher.put(Operation.VI_PREVIOUS_HISTORY, ConsoleReaderImpl::viPreviousHistory);
        dispatcher.put(Operation.NEXT_HISTORY, ConsoleReaderImpl::nextHistory);
        dispatcher.put(Operation.VI_NEXT_HISTORY, ConsoleReaderImpl::viNextHistory);
        dispatcher.put(Operation.BACKWARD_DELETE_CHAR, ConsoleReaderImpl::backwardDeleteChar);
        dispatcher.put(Operation.BEGINNING_OF_HISTORY, ConsoleReaderImpl::beginningOfHistory);
        dispatcher.put(Operation.END_OF_HISTORY, ConsoleReaderImpl::endOfHistory);
        dispatcher.put(Operation.OVERWRITE_MODE, ConsoleReaderImpl::overwriteMode);
        dispatcher.put(Operation.SELF_INSERT, ConsoleReaderImpl::selfInsert);
        dispatcher.put(Operation.SELF_INSERT_UNMETA, ConsoleReaderImpl::selfInsertUnmeta);
        dispatcher.put(Operation.TAB_INSERT, ConsoleReaderImpl::tabInsert);
        dispatcher.put(Operation.RE_READ_INIT_FILE, ConsoleReaderImpl::reReadInitFile);
        dispatcher.put(Operation.START_KBD_MACRO, ConsoleReaderImpl::startKbdMacro);
        dispatcher.put(Operation.END_KBD_MACRO, ConsoleReaderImpl::endKbdMacro);
        dispatcher.put(Operation.CALL_LAST_KBD_MACRO, ConsoleReaderImpl::callLastKbdMacro);
        dispatcher.put(Operation.VI_EDITING_MODE, ConsoleReaderImpl::viEditingMode);
        dispatcher.put(Operation.VI_MOVEMENT_MODE, ConsoleReaderImpl::viMovementMode);
        dispatcher.put(Operation.VI_INSERTION_MODE, ConsoleReaderImpl::viInsertionMode);
        dispatcher.put(Operation.VI_APPEND_MODE, ConsoleReaderImpl::viAppendMode);
        dispatcher.put(Operation.VI_APPEND_EOL, ConsoleReaderImpl::viAppendEol);
        dispatcher.put(Operation.VI_SEARCH, ConsoleReaderImpl::viSearch);
        dispatcher.put(Operation.VI_INSERT_BEG, ConsoleReaderImpl::viInsertBeg);
        dispatcher.put(Operation.VI_KILL_WHOLE_LINE, ConsoleReaderImpl::viKillWholeLine);
        dispatcher.put(Operation.VI_CHAR_SEARCH, ConsoleReaderImpl::viCharSearch);
        dispatcher.put(Operation.VI_CHANGE_CHAR, ConsoleReaderImpl::viChangeChar);
        dispatcher.put(Operation.QUOTED_INSERT, ConsoleReaderImpl::quotedInsert);
        dispatcher.put(Operation.VI_DELETE_TO_EOL, ConsoleReaderImpl::viDeleteToEol);
        dispatcher.put(Operation.VI_CHANGE_TO_EOL, ConsoleReaderImpl::viChangeToEol);
        dispatcher.put(Operation.EMACS_EDITING_MODE, ConsoleReaderImpl::emacsEditingMode);
        dispatcher.put(Operation.ACCEPT_LINE, ConsoleReaderImpl::acceptLine);
        dispatcher.put(Operation.INSERT_COMMENT, ConsoleReaderImpl::insertComment);
        dispatcher.put(Operation.VI_INSERT_COMMENT, ConsoleReaderImpl::viInsertComment);
        dispatcher.put(Operation.VI_MOVE_ACCEPT_LINE, ConsoleReaderImpl::viMoveAcceptLine);
        dispatcher.put(Operation.QUIT, ConsoleReaderImpl::quit);
        dispatcher.put(Operation.ABORT, ConsoleReaderImpl::abort);
        dispatcher.put(Operation.INTERRUPT, ConsoleReaderImpl::interrupt);
        dispatcher.put(Operation.EXIT_OR_DELETE_CHAR, ConsoleReaderImpl::exitOrDeleteChar);
        dispatcher.put(Operation.HISTORY_SEARCH_BACKWARD, ConsoleReaderImpl::historySearchBackward);
        dispatcher.put(Operation.HISTORY_SEARCH_FORWARD, ConsoleReaderImpl::historySearchForward);
        dispatcher.put(Operation.REVERSE_SEARCH_HISTORY, ConsoleReaderImpl::reverseSearchHistory);
        dispatcher.put(Operation.FORWARD_SEARCH_HISTORY, ConsoleReaderImpl::forwardSearchHistory);
        dispatcher.put(Operation.VI_EOF_MAYBE, ConsoleReaderImpl::viEofMaybe);
        dispatcher.put(Operation.VI_DELETE_TO, ConsoleReaderImpl::viDeleteTo);
        dispatcher.put(Operation.VI_YANK_TO, ConsoleReaderImpl::viYankTo);
        dispatcher.put(Operation.VI_CHANGE_TO, ConsoleReaderImpl::viChangeTo);
        dispatcher.put(Operation.VI_ARG_DIGIT, ConsoleReaderImpl::viArgDigit);
        dispatcher.put(Operation.VI_BEGINNING_OF_LINE_OR_ARG_DIGIT, ConsoleReaderImpl::viBeginningOfLineOrArgDigit);
        dispatcher.put(Operation.COMPLETE_WORD, ConsoleReaderImpl::completeWord);
        dispatcher.put(Operation.POSSIBLE_COMPLETIONS, ConsoleReaderImpl::listChoices);
        dispatcher.put(Operation.DO_LOWERCASE_VERSION, ConsoleReaderImpl::doLowercaseVersion);
        dispatcher.put(Operation.UP_LINE_OR_HISTORY, ConsoleReaderImpl::upLineOrHistory);
        dispatcher.put(Operation.DOWN_LINE_OR_HISTORY, ConsoleReaderImpl::downLineOrHistory);
        return dispatcher;
    }

    protected void redisplay() {
        redisplay(true);
    }

    protected void redisplay(boolean flush) {
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

        String oldPostStr = "";
        String newPostStr = "";
        if (oldPost != null) {
            oldPostStr = "\n" + toColumns(oldPost, oldColumns);
        }
        if (post != null) {
            newPostStr = "\n" + toColumns(post, size.getColumns());
        }
        String tOldBuf = insertSecondaryPrompts(oldBuf, new ArrayList<>());
        List<String> secondaryPrompts = new ArrayList<>();
        String tNewBuf = insertSecondaryPrompts(buffer, secondaryPrompts);

        List<String> oldLines = AnsiHelper.splitLines(oldPrompt + tOldBuf + oldPostStr, oldColumns, TAB_WIDTH);
        List<String> newLines = AnsiHelper.splitLines(prompt + tNewBuf + newPostStr, size.getColumns(), TAB_WIDTH);
        List<String> oldRightPromptLines = AnsiHelper.splitLines(oldRightPrompt, oldColumns, TAB_WIDTH);
        List<String> rightPromptLines = AnsiHelper.splitLines(rightPrompt, size.getColumns(), TAB_WIDTH);

        while (oldLines.size() < rightPromptLines.size()) {
            oldLines.add("");
        }
        while (newLines.size() < rightPromptLines.size()) {
            newLines.add("");
        }
        for (int i = 0; i < oldRightPromptLines.size(); i++) {
            String line = oldRightPromptLines.get(i);
            oldLines.set(i, addRightPrompt(line, oldLines.get(i)));
        }
        for (int i = 0; i < rightPromptLines.size(); i++) {
            String line = rightPromptLines.get(i);
            newLines.set(i, addRightPrompt(line, newLines.get(i)));
        }

        int lineIndex = 0;
        int currentPos = 0;
        while (lineIndex < Math.min(oldLines.size(), newLines.size())) {
            String oldLine = oldLines.get(lineIndex);
            String newLine = newLines.get(lineIndex);
            lineIndex++;

            List<DiffHelper.Diff> diffs = DiffHelper.diff(oldLine, newLine);
            boolean ident = true;
            boolean cleared = false;
            int curCol = currentPos;
            for (int i = 0; i < diffs.size(); i++) {
                DiffHelper.Diff diff = diffs.get(i);
                int width = wcwidth(AnsiHelper.strip(diff.text), currentPos);
                switch (diff.operation) {
                    case EQUAL:
                        if (!ident) {
                            cursorPos = moveVisualCursorTo(currentPos);
                            rawPrint(diff.text);
                            cursorPos += width;
                            cursorOk = false;
                            currentPos = cursorPos;
                        } else {
                            currentPos += width;
                        }
                        break;
                    case INSERT:
                        if (i <= diffs.size() - 2
                                && diffs.get(i+1).operation == DiffHelper.Operation.EQUAL) {
                            cursorPos = moveVisualCursorTo(currentPos);
                            boolean hasIch = console.getStringCapability(Capability.parm_ich) != null;
                            boolean hasIch1 = console.getStringCapability(Capability.insert_character) != null;
                            if (hasIch) {
                                console.puts(Capability.parm_ich, width);
                                rawPrint(diff.text);
                                cursorPos += width;
                                cursorOk = false;
                                currentPos = cursorPos;
                                break;
                            } else if (hasIch1) {
                                for (int j = 0; j < width; j++) {
                                    console.puts(Capability.insert_character);
                                }
                                rawPrint(diff.text);
                                cursorPos += width;
                                cursorOk = false;
                                currentPos = cursorPos;
                                break;
                            }
                        } else if (i <= diffs.size() - 2
                                && diffs.get(i+1).operation == DiffHelper.Operation.DELETE
                                && width == wcwidth(AnsiHelper.strip(diffs.get(i + 1).text), currentPos)) {
                            moveVisualCursorTo(currentPos);
                            rawPrint(diff.text);
                            cursorPos += width;
                            cursorOk = false;
                            currentPos = cursorPos;
                            i++; // skip delete
                            break;
                        }
                        moveVisualCursorTo(currentPos);
                        rawPrint(diff.text);
                        cursorPos += width;
                        cursorOk = false;
                        currentPos = cursorPos;
                        ident = false;
                        break;
                    case DELETE:
                        if (cleared) {
                            continue;
                        }
                        if (currentPos - curCol >= size.getColumns()) {
                            continue;
                        }
                        if (i <= diffs.size() - 2
                                && diffs.get(i+1).operation == DiffHelper.Operation.EQUAL) {
                            if (currentPos + wcwidth(diffs.get(i+1).text, cursorPos) < size.getColumns()) {
                                moveVisualCursorTo(currentPos);
                                boolean hasDch = console.getStringCapability(Capability.parm_dch) != null;
                                boolean hasDch1 = console.getStringCapability(Capability.delete_character) != null;
                                if (hasDch) {
                                    console.puts(Capability.parm_dch, width);
                                    break;
                                } else if (hasDch1) {
                                    for (int j = 0; j < width; j++) {
                                        console.puts(Capability.delete_character);
                                    }
                                    break;
                                }
                            }
                        }
                        int oldLen = wcwidth(oldLine, 0);
                        int newLen = wcwidth(newLine, 0);
                        int nb = Math.max(oldLen, newLen) - currentPos;
                        moveVisualCursorTo(currentPos);
                        if (!console.puts(Capability.clr_eol)) {
                            rawPrint(' ', nb);
                            cursorPos += nb;
                            cursorOk = false;
                        }
                        cleared = true;
                        ident = false;
                        break;
                }
            }
            if (!cursorOk
                    && console.getBooleanCapability(Capability.auto_right_margin)
                    && console.getBooleanCapability(Capability.eat_newline_glitch)
                    && lineIndex == Math.max(oldLines.size(), newLines.size()) - 1
                    && cursorPos > curCol && cursorPos % size.getColumns() == 0) {
                rawPrint(' '); // move cursor to next line by printing dummy space
                console.puts(Capability.carriage_return); // CR / not newline.
            }
            currentPos = curCol + size.getColumns();
        }
        while (lineIndex < Math.max(oldLines.size(), newLines.size())) {
            moveVisualCursorTo(currentPos);
            if (lineIndex < oldLines.size()) {
                if (console.getStringCapability(Capability.clr_eol) != null) {
                    console.puts(Capability.clr_eol);
                } else {
                    int nb = wcwidth(AnsiHelper.strip(newLines.get(lineIndex)), cursorPos);
                    rawPrint(' ', nb);
                    cursorPos += nb;
                }
            } else {
                rawPrint(newLines.get(lineIndex));
                cursorPos += wcwidth(AnsiHelper.strip(newLines.get(lineIndex)), cursorPos);
            }
            lineIndex++;
            currentPos = currentPos + size.getColumns();
        }
        List<String> promptLines = AnsiHelper.splitLines(prompt + insertSecondaryPrompts(buf.upToCursor(), secondaryPrompts, false), size.getColumns(), TAB_WIDTH);
        if (!promptLines.isEmpty()) {
            moveVisualCursorTo((promptLines.size() - 1) * size.getColumns()
                    + wcwidth(AnsiHelper.strip(promptLines.get(promptLines.size() - 1)), 0));
        }
        if (flush) {
            flush();
        }
        oldBuf = buffer;
        oldPrompt = prompt;
        oldPost = post;
        oldColumns = size.getColumns();
        oldRightPrompt = rightPrompt;
    }

    private static String SECONDARY_PROMPT = "> ";

    private String insertSecondaryPrompts(String str, List<String> prompts) {
        return insertSecondaryPrompts(str, prompts, true);
    }

    private String insertSecondaryPrompts(String str, List<String> prompts, boolean computePrompts) {
        checkNotNull(prompts);
        StringBuilder sb = new StringBuilder();
        int line = 0;
        if (computePrompts || !isSet("pad-prompts") || prompts.size() < 2) {
            for (int i = 0; i < str.length(); i++) {
                char ch = str.charAt(i);
                sb.append(ch);
                if (ch == '\n') {
                    String prompt;
                    if (computePrompts) {
                        ParsedLine pl = parser.parse(sb.toString(), 0);
                        prompt = (pl.complete() ? "" : pl.missingPrompt()) + SECONDARY_PROMPT;
                    } else {
                        prompt = prompts.get(line++);
                    }
                    prompts.add(prompt);
                    sb.append(prompt);
                }
            }
        }
        if (isSet("pad-prompts") && prompts.size() >= 2) {
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
        int width = wcwidth(AnsiHelper.strip(prompt), 0);
        int nb = size.getColumns() - width - wcwidth(AnsiHelper.strip(line), 0) - 3;
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

    protected boolean useTab() {
        if (!buf.toString().matches("[\r\n\t ]*")) {
            return false;
        }
        return true;
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

    private int menucmp;
    private int lastambig;
    private int zmult;
    private boolean bashListFirst;
    private int useMenu;
    private boolean useGlob;

    protected boolean completeWord() {
        useMenu = getBoolean("menucomplete", false) ? 1 : 0;
        useGlob = getBoolean("globcomplete", false);
        if (opBuffer.toString().equals("\t") && useTab()) {
            return selfInsert();
        } else {
            boolean ret;
            if (lastambig == 1 && getBoolean("bashautolist", false) && useMenu != 0 && menucmp == 0) {
                bashListFirst = true;
                ret = doComplete(CompletionType.List);
                bashListFirst = false;
                lastambig = 2;
            } else {
                ret = doComplete(CompletionType.Complete);
            }
            return ret;
        }
    }

    protected boolean menuComplete() {
        useMenu = 1;
        useGlob = getBoolean("globcomplete", false);
        if (opBuffer.toString().equals("\t") && useTab()) {
            return selfInsert();
        } else {
            return doComplete(CompletionType.Complete);
        }
    }

    protected boolean listChoices() {
        useMenu = getBoolean("menucomplete", false) ? 1 : 0;
        useGlob = getBoolean("globcomplete", false);
        return doComplete(CompletionType.List);
    }

    protected boolean deleteCharOrList() {
        useMenu = getBoolean("menucomplete", false) ? 1 : 0;
        useGlob = getBoolean("globcomplete", false);
        if (buf.cursor() != buf.length()) {
            return deleteChar();
        } else {
            return doComplete(CompletionType.List);
        }
    }

    protected boolean reverseMenuComplete() {
        zmult = -zmult;
        return menuComplete();
    }

    protected boolean acceptMenuComplete() {
        if (menucmp == 0) {
            return false;
        } else {
            // TODO: doCompletionAccept();
            return menuComplete();
        }
    }

    protected boolean doComplete(CompletionType lst) {
        try {
            if (doExpandHist()) {
                return true;
            }
        } catch (IllegalArgumentException e) {
            return false;
        }

        List<Candidate> candidates = new ArrayList<>();
        ParsedLine line = parser.parse(buf.toString(), buf.cursor());
        for (Completer completer : completers) {
            completer.complete(line, candidates);
        }

        boolean caseInsensitive = isSet("case-insensitive");
        int errors = getInt("errors", 2);

        NavigableMap<String, List<Candidate>> sortedCandidates =
                new TreeMap<>(caseInsensitive ? String.CASE_INSENSITIVE_ORDER : null);
        for (Candidate cand : candidates) {
            sortedCandidates
                    .computeIfAbsent(AnsiHelper.strip(cand.value()), s -> new ArrayList<>())
                    .add(cand);
        }

        String word = line.word();
        String w = line.word().substring(0, line.wordCursor());


        boolean doList;
        boolean doMenu = false;
        int selection = 0;

        List<Candidate> possible;
        if (lst == CompletionType.List) {
            doList = true;
            possible = sortedCandidates.entrySet().stream()
                    .filter(e -> e.getKey().startsWith(w))
                    .flatMap(e -> e.getValue().stream())
                    .collect(Collectors.toList());
        } else {
            boolean exact = false;
            String completion = null;
            Map<String, List<Candidate>> matching = sortedCandidates.subMap(w, getHigherBound(w));
            // Found an exact match of the whole word
            if (sortedCandidates.containsKey(word)
                    && (matching.size() == 1
                        || isSet("recognize-exact"))) {
                exact = true;
                completion = line.word();
                possible = Collections.emptyList();
                doList = false;
            } else {
                for (String key : matching.keySet()) {
                    completion = completion == null ? key : getCommonStart(completion, key, caseInsensitive);
                }
                possible = matching.entrySet().stream()
                        .flatMap(e -> e.getValue().stream())
                        .collect(Collectors.toList());
                // No match
                if (completion == null) {
                    // Add any fixable typos
                    possible = sortedCandidates.entrySet().stream()
                            .filter(e -> !e.getKey().startsWith(w) && Levenshtein.distance(w, e.getKey().substring(0, w.length())) < errors)
                            .flatMap(e -> e.getValue().stream())
                            .collect(Collectors.toList());
                    if (!possible.isEmpty()) {
                        possible.add(new Candidate(w, "original", null));
                        doList = isSet("auto-list");
                    } else {
                        doList = false;
                    }
                }
                else if (possible.size() == 1) {
                    exact = true;
                    doList = false;
                } else {
                    if (completion.length() == w.length()) {
                        completion = null;
                    }
                    doList = !possible.isEmpty() && isSet("auto-list");
                }

                if (doList && isSet("auto-menu") && useMenu != 0) {
                    doMenu = true;
                    if (completion == null) {
                        completion = AnsiHelper.strip(possible.get(0).value());
                    }
                }
            }

            if (completion != null) {
                if (isSet("complete-overwrite-word")) {
                    buf.move(word.length() - w.length());
                    buf.backspace(word.length());
                    buf.write(completion);
                    if (exact && buf.currChar() != ' ') {
                        buf.write(" ");
                    }
                } else {
                    buf.backspace(w.length());
                    buf.write(completion);
                    if (exact && buf.currChar() == 0) {
                        buf.write(" ");
                    }
                }
            }
        }

        if (doList) {
            computePost(possible, possible.get(selection));
        }

        if (doMenu) {
            redisplay();

            KeyMap keyMap = new KeyMap("menuselect");
            keyMap.bind("\t", "complete");
            keyMap.bind("\r", "accept");
            keyMap.bind("\n", "accept");
            keyMap.bind(console.getStringCapability(Capability.cursor_up), "up");
            keyMap.bind(console.getStringCapability(Capability.cursor_down), "down");
            keyMap.bind(console.getStringCapability(Capability.cursor_left), "left");
            keyMap.bind(console.getStringCapability(Capability.cursor_right), "right");

            Object operation;
            while ((operation = readBinding(getKeys(), keyMap)) != null) {
                if ("complete".equals(operation)) {
                    selection = (selection + 1) % possible.size();
                    computePost(possible, possible.get(selection));
                } else if ("accept".equals(operation)) {
                    return true;
                }
                redisplay();
            }
        }

        return !candidates.isEmpty();
    }

    private void computePost(List<Candidate> possible, Candidate selection) {
        boolean displayDesc = false;
        boolean groupName = isSet("group");
        if (groupName) {
            LinkedHashMap<String, TreeMap<String, Candidate>> sorted = new LinkedHashMap<>();
            for (Candidate cand : possible) {
                String group = cand.group();
                sorted.computeIfAbsent(group != null ? group : "", s -> new TreeMap<>())
                        .put(cand.value(), cand);
            }
            List<String[]> strings = new ArrayList<>();
            for (Map.Entry<String, TreeMap<String, Candidate>> entry : sorted.entrySet()) {
                String group = entry.getKey();
                if (group.isEmpty() && sorted.size() > 1) {
                    group = "others";
                }
                if (!group.isEmpty()) {
                    strings.add(new String[] { Ansi.ansi().fg(Color.CYAN).a(group).reset().toString() });
                }
                if (displayDesc) {
                    for (Candidate cand : entry.getValue().values()) {
                        strings.add(new String[] {
                           cand.value(), cand.descr()
                        });
                    }
                } else {
                    List<String> strs = new ArrayList<>();
                    for (Candidate cand : entry.getValue().values()) {
                        if (cand == selection) {
                            strs.add(Ansi.ansi().a(Attribute.NEGATIVE_ON).a(cand.value()).a(Attribute.NEGATIVE_OFF).toString());
                        } else {
                            strs.add(cand.value());
                        }
                    }
                    strings.add(strs.toArray(new String[strs.size()]));
                }
            }
            post = strings.toArray(new String[strings.size()][]);
        } else {
            Set<String> groups = new LinkedHashSet<>();
            TreeMap<String, Candidate> sorted = new TreeMap<>();
            for (Candidate cand : possible) {
                String group = cand.group();
                if (group != null) {
                    groups.add(group);
                }
                sorted.put(AnsiHelper.strip(cand.value()), cand);
            }
            List<String[]> strings = new ArrayList<>();
            for (String group : groups) {
                strings.add(new String[] { Ansi.ansi().fg(Color.CYAN).a(group).reset().toString() });
            }
            List<String> strs = new ArrayList<>();
            for (Candidate cand : sorted.values()) {
                if (cand == selection) {
                    strs.add(Ansi.ansi().a(Attribute.NEGATIVE_ON).a(cand.value()).a(Attribute.NEGATIVE_OFF).toString());
                } else {
                    strs.add(cand.value());
                }
            }
            strings.add(strs.toArray(new String[strs.size()]));
            post = strings.toArray(new String[strings.size()][]);
        }
    }

    private String getHigherBound(String str) {
        int[] s = str.codePoints().toArray();
        s[s.length - 1]++;
        return new String(s, 0, s.length);
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
     * Output the specified string to the output stream (but not the buffer).
     */
    public void print(String buff) {
        checkNotNull(buff);
        int cursorPos = getCursorPosition();
        for (int i = 0, end = buff.length(); i < end; i++) {
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
    }

    public void println(String s) {
        print(s);
        println();
    }

    /**
     * Output a platform-dependant newline.
     */
    public void println() {
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
    public boolean pasteFromClipboard() {
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
        catch (UnsupportedFlavorException | IOException e) {
            Log.error("Paste failed: ", e);

            return false;
        }
    }

    /**
     * Adding a triggered Action allows to give another curse of action if a character passed the pre-processing.
     * <p/>
     * Say you want to close the application if the user enter q.
     * addTriggerAction('q', new ActionListener(){ System.exit(0); }); would do the trick.
     *
     * TODO: deprecate
     */
    public void addTriggeredAction(final char c, final Widget widget) {
        getKeys().bind(Character.toString(c), widget);
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
    public void printCandidates(Collection<Candidate> candidates)
    {
        /*
        candidates = new LinkedHashSet<>(candidates);

        int max = getInt(COMPLETION_QUERY_ITEMS, 100);
        if (max > 0 && candidates.size() >= max) {
            println();
            rawPrint(Messages.DISPLAY_CANDIDATES.format(candidates.size()));
            flush();

            int c;

            String noOpt = Messages.DISPLAY_CANDIDATES_NO.format();
            String yesOpt = Messages.DISPLAY_CANDIDATES_YES.format();
            int[] allowed = {yesOpt.charAt(0), noOpt.charAt(0)};

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
            printColumns(candidates);
            println();
        }
        else {
            post = candidates.toArray(new Candidate[candidates.size()]);
        }
        */
    }

    protected String toColumns(String[][] items, int width) {
        StringBuilder buff = new StringBuilder();
        for (String[] part : items) {
            toColumns(part, width, buff);
        }
        return buff.toString();
    }

    protected void toColumns(String[] items, int width, StringBuilder buff) {
        if (items == null || items.length == 0) {
            return;
        }
        int maxWidth = 0;
        for (String item : items) {
            // we use 0 here, as we don't really support tabulations inside candidates
            int len = wcwidth(AnsiHelper.strip(item), 0);
            maxWidth = Math.max(maxWidth, len);
        }
        maxWidth = maxWidth + 3;

        int realLength = 0;
        for (String item : items) {
            if ((realLength + maxWidth) > width) {
                buff.append('\n');
                realLength = 0;
            }

            buff.append(item);
            int strippedItemLength = wcwidth(AnsiHelper.strip(item), 0);
            for (int i = 0; i < (maxWidth - strippedItemLength); i++) {
                buff.append(' ');
            }
            realLength += maxWidth;
        }
        buff.append('\n');
    }

    /**
     * Output the specified {@link Collection} in proper columns.
     */
    public void printColumns(final Collection<? extends CharSequence> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        int width = size.getColumns();
        int height = size.getRows();

        int maxWidth = 0;
        for (CharSequence item : items) {
            // we use 0 here, as we don't really support tabulations inside candidates
            int len = wcwidth(AnsiHelper.strip(item.toString()), 0);
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

                    console.puts(Capability.carriage_return);
                    if (c == 'q') {
                        // cancel
                        break;
                    }
                }
            }

            // NOTE: toString() is important here due to AnsiString being retarded
            buff.append(item.toString());
            int strippedItemLength = wcwidth(AnsiHelper.strip(item.toString()), 0);
            for (int i = 0; i < (maxWidth - strippedItemLength); i++) {
                buff.append(' ');
            }
            realLength += maxWidth;
        }

        if (buff.length() > 0) {
            rawPrintln(buff.toString());
        }
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

    public String getVariable(String name) {
        String v = variables.get(name);
        return v != null ? v : consoleKeys.getVariable(name);
    }

    public void setVariable(String name, String value) {
        variables.put(name, value);
    }

    private boolean isSet(String name) {
        return getBoolean(name, false);
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

    String getString(String name, String def) {
        String v = getVariable(name);
        return (v != null) ? v : def;
    }

}
