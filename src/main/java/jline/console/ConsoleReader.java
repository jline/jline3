/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */

package jline.console;

import jline.Terminal;
import jline.TerminalFactory;
import jline.WindowsTerminal;
import jline.internal.Log;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.io.BufferedInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * A reader for console applications. It supports custom tab-completion,
 * saveable command history, and command line editing. On some platforms,
 * platform-specific commands will need to be issued before the reader will
 * function properly. See {@link jline.Terminal#init} for convenience
 * methods for issuing platform-specific setup commands.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class ConsoleReader
{
    public static final String JLINE_COMPLETION_THRESHOLD = "jline.completion.threshold";

    public static final String JLINE_KEYBINDINGS = "jline.keybindings";

    public static final String JLINEBINDINGS_PROPERTIES = ".jlinebindings.properties";

    public static final String JLINE_NOBELL = "jline.nobell";

    public static final String CR = System.getProperty("line.separator");

    public static final char BACKSPACE = '\b';

    public static final char RESET_LINE = '\r';

    public static final char KEYBOARD_BELL = '\07';

    public static final Character NULL_MASK = (char) 0;

    public static final int TAB_WIDTH = 4;

    private static final ResourceBundle loc = ResourceBundle.getBundle(CandidateListCompletionHandler.class.getName());

    private final Terminal terminal;

    private InputStream in;

    private final Writer out;

    private final CursorBuffer buf = new CursorBuffer();

    private History history = new FileHistory();

    private final List<Completer> completers = new LinkedList<Completer>();

    private String prompt;

    private boolean useHistory = true;

    private boolean usePagination = false;

    /**
     * The map for logical operations.
     */
    private final short[] keybindings;

    /**
     * If true, issue an audible keyboard bell when appropriate.
     */
    private boolean bellEnabled = true;

    /**
     * The current character mask.
     */
    private Character mask;

    /**
     * The number of tab-completion candidates above which a warning will be
     * prompted before showing all the candidates.
     */
    private int autoprintThreshhold = Integer.getInteger(JLINE_COMPLETION_THRESHOLD, 100); // same default as bash

    private CompletionHandler completionHandler = new CandidateListCompletionHandler();

    private Character echoCharacter = null;

    private Map<Character,ActionListener> triggeredActions = new HashMap<Character,ActionListener>();

    /**
     * Create a new reader.
     *
     * @param in       the input
     * @param out      the output
     * @param bindings the key bindings to use
     * @param term     the terminal to use
     */
    public ConsoleReader(final InputStream in, final Writer out, InputStream bindings, final Terminal term) throws IOException {
        this.terminal = term;
        setInput(in);
        this.out = out;
        this.keybindings = loadKeyBindings(bindings);

        if (Boolean.getBoolean(JLINE_NOBELL)) {
            setBellEnabled(false);
        }
    }

    private short[] loadKeyBindings(InputStream input) throws IOException {
        if (input == null) {
            try {
                File file = new File(System.getProperty("user.home", JLINEBINDINGS_PROPERTIES));

                String path = System.getProperty(JLINE_KEYBINDINGS);
                if (path != null) {
                    file = new File(path);
                }

                if (file.isFile()) {
                    Log.debug("Loading user bindings from: ", file);
                    input = new FileInputStream(file);
                }
            }
            catch (Exception e) {
                Log.error("Failed to load user bindings", e);
            }
        }

        if (input == null) {
            Log.debug("Using default bindings");
            input = terminal.getDefaultBindings();
        }

        short[] keybindings = new short[Character.MAX_VALUE * 2];

        Arrays.fill(keybindings, Operation.UNKNOWN.code);

        // Loads the key bindings. Bindings file is in the format:
        //
        // keycode: operation name

        if (input != null) {
            input = new BufferedInputStream(input);
            Properties p = new Properties();
            p.load(input);
            input.close();

            for (Object key : p.keySet()) {
                String val = (String) key;

                try {
                    short code = Short.parseShort(val);
                    String name = p.getProperty(val);
                    Operation op = Operation.valueOf(name);
                    keybindings[code] = op.code;
                }
                catch (NumberFormatException e) {
                    Log.error("Failed to convert binding code: ", val, e);
                }
            }

            // hardwired arrow key bindings
            // keybindings[VK_UP] = PREV_HISTORY;
            // keybindings[VK_DOWN] = NEXT_HISTORY;
            // keybindings[VK_LEFT] = PREV_CHAR;
            // keybindings[VK_RIGHT] = NEXT_CHAR;
        }

        return keybindings;
    }

    /**
     * Create a new reader using {@link FileDescriptor#in} for input and
     * {@link System#out} for output. {@link FileDescriptor#in} is used because
     * it has a better chance of being unbuffered.
     */
    public ConsoleReader() throws IOException {
        this(new FileInputStream(FileDescriptor.in),
            new PrintWriter(new OutputStreamWriter(System.out,
                    // FIXME: Why windows stuff here?
                    System.getProperty(WindowsTerminal.JLINE_WINDOWS_TERMINAL_OUTPUT_ENCODING,
                            System.getProperty("file.encoding")))));
    }

    /**
     * Create a new reader using the specified {@link InputStream} for input and
     * the specific writer for output, using the default keybindings resource.
     */
    public ConsoleReader(final InputStream in, final Writer out) throws IOException {
        this(in, out, null);
    }

    public ConsoleReader(final InputStream in, final Writer out, final InputStream bindings) throws IOException {
        this(in, out, bindings, TerminalFactory.get());
    }

    /**
     * Set the stream to be used for console input.
     */
    public void setInput(final InputStream in) {
        this.in = in;
    }

    /**
     * Returns the stream used for console input.
     */
    public InputStream getInput() {
        return in;
    }

    public Writer getOutput() {
        return out;
    }

    public Terminal getTerminal() {
        return terminal;
    }

    /**
     * @param flag if true, enable audible keyboard bells if an alert is required.
     */
    public void setBellEnabled(final boolean flag) {
        this.bellEnabled = flag;
    }

    /**
     * @return true is audible keyboard bell is enabled.
     */
    public boolean getBellEnabled() {
        return bellEnabled;
    }

    /**
     * Add the specified {@link Completer} to the list of handlers for
     * tab-completion.
     *
     * @param completer the {@link Completer} to add
     * @return true if it was successfully added
     */
    public boolean addCompletor(final Completer completer) {
        return completers.add(completer);
    }

    /**
     * Remove the specified {@link Completer} from the list of handlers for
     * tab-completion.
     *
     * @param completer the {@link Completer} to remove
     * @return true if it was successfully removed
     */
    public boolean removeCompletor(final Completer completer) {
        return completers.remove(completer);
    }

    /**
     * Returns an unmodifiable list of all the completors.
     */
    public Collection<Completer> getCompleters() {
        return Collections.unmodifiableList(completers);
    }

    /**
     * @param threshhold the number of candidates to print without issuing a warning.
     */
    public void setAutoprintThreshhold(final int threshhold) {
        this.autoprintThreshhold = threshhold;
    }

    /**
     * @return the number of candidates to print without issing a warning.
     */
    public int getAutoprintThreshhold() {
        return autoprintThreshhold;
    }

    /**
     * The default prompt that will be issued.
     */
    public void setDefaultPrompt(final String prompt) {
        this.prompt = prompt;
    }

    /**
     * The default prompt that will be issued.
     */
    public String getDefaultPrompt() {
        return prompt;
    }

    public void setHistory(final History history) {
        this.history = history;
    }

    public History getHistory() {
        return history;
    }

    public void setCompletionHandler(final CompletionHandler handler) {
        this.completionHandler = handler;
    }

    public CompletionHandler getCompletionHandler() {
        return this.completionHandler;
    }

    /**
     * <p>
     * Set the echo character. For example, to have "*" entered when a password
     * is typed:
     * </p>
     * <p/>
     * <pre>
     * myConsoleReader.setEchoCharacter(new Character('*'));
     * </pre>
     * <p/>
     * <p>
     * Setting the character to
     * <p/>
     * <pre>
     * null
     * </pre>
     * <p/>
     * will restore normal character echoing. Setting the character to
     * <p/>
     * <pre>
     * new Character(0)
     * </pre>
     * <p/>
     * will cause nothing to be echoed.
     * </p>
     *
     * @param c the character to echo to the console in place of the typed
     *                      character.
     */
    public void setEchoCharacter(final Character c) {
        this.echoCharacter = c;
    }

    /**
     * Returns the echo character.
     */
    public Character getEchoCharacter() {
        return this.echoCharacter;
    }

    /**
     * Whether or not to add new commands to the history buffer.
     */
    public void setUseHistory(final boolean flag) {
        this.useHistory = flag;
    }

    /**
     * Whether or not to add new commands to the history buffer.
     */
    public boolean getUseHistory() {
        return useHistory;
    }

    /**
     * Whether to use pagination when the number of rows of candidates exceeds
     * the height of the temrinal.
     */
    public void setUsePagination(final boolean flag) {
        this.usePagination = flag;
    }

    /**
     * Whether to use pagination when the number of rows of candidates exceeds
     * the height of the temrinal.
     */
    public boolean getUsePagination() {
        return usePagination;
    }

    /**
     * Adding a triggered Action allows to give another curse of action
     * if a character passed the preprocessing.
     * 
     * Say you want to close the application if the user enter q.
     * addTriggerAction('q', new ActionListener(){ System.exit(0); });
     * would do the trick.
     */
    public void addTriggeredAction(final char c, final ActionListener listener) {
        triggeredActions.put(c, listener);
    }

    int getKeyForAction(final short logicalAction) {
        for (int i = 0; i < keybindings.length; i++) {
            if (keybindings[i] == logicalAction) {
                return i;
            }
        }

        return -1;
    }

    int getKeyForAction(final Operation op) {
        assert op != null;
        return getKeyForAction(op.code);
    }
    
    /**
     * Clear the echoed characters for the specified character code.
     */
    int clearEcho(final int c) throws IOException {
        // if the terminal is not echoing, then ignore
        if (!terminal.isEchoEnabled()) {
            return 0;
        }

        // otherwise, clear
        int num = countEchoCharacters((char) c);
        back(num);
        drawBuffer(num);

        return num;
    }

    int countEchoCharacters(final char c) {
        // tabs as special: we need to determine the number of spaces
        // to cancel based on what out current cursor position is
        if (c == 9) {
            int tabstop = 8; // will this ever be different?
            int position = getCursorPosition();

            return tabstop - (position % tabstop);
        }

        return getPrintableCharacters(c).length();
    }

    /**
     * Return the number of characters that will be printed when the specified
     * character is echoed to the screen. Adapted from cat by Torbjorn Granlund,
     * as repeated in stty by David MacKenzie.
     */
    StringBuilder getPrintableCharacters(final char ch) {
        StringBuilder sbuff = new StringBuilder();

        if (ch >= 32) {
            if (ch < 127) {
                sbuff.append(ch);
            }
            else if (ch == 127) {
                sbuff.append('^');
                sbuff.append('?');
            }
            else {
                sbuff.append('M');
                sbuff.append('-');

                if (ch >= (128 + 32)) {
                    if (ch < (128 + 127)) {
                        sbuff.append((char) (ch - 128));
                    }
                    else {
                        sbuff.append('^');
                        sbuff.append('?');
                    }
                }
                else {
                    sbuff.append('^');
                    sbuff.append((char) (ch - 128 + 64));
                }
            }
        }
        else {
            sbuff.append('^');
            sbuff.append((char) (ch + 64));
        }

        return sbuff;
    }

    int getCursorPosition() {
        // FIXME: does not handle anything but a line with a prompt absolute position
        return ((prompt == null) ? 0 : prompt.length()) + buf.cursor;
    }

    /**
     * Read the next line and return the contents of the buffer.
     */
    public String readLine() throws IOException {
        return readLine((String) null);
    }

    /**
     * Read the next line with the specified character mask. If null, then
     * characters will be echoed. If 0, then no characters will be echoed.
     */
    public String readLine(final Character mask) throws IOException {
        return readLine(null, mask);
    }

    public String readLine(final String prompt) throws IOException {
        return readLine(prompt, null);
    }

    /**
     * Read a line from the <i>in</i> {@link InputStream}, and return the line
     * (without any trailing newlines).
     *
     * @param prompt the prompt to issue to the console, may be null.
     * @return a line that is read from the terminal, or null if there was null
     *         input (e.g., <i>CTRL-D</i> was pressed).
     */
    public String readLine(final String prompt, final Character mask) throws IOException {
        this.mask = mask;
        if (prompt != null) {
            this.prompt = prompt;
        }

        try {
            terminal.beforeReadLine(this, this.prompt, mask);

            if ((this.prompt != null) && (this.prompt.length() > 0)) {
                out.write(this.prompt);
                out.flush();
            }

            // if the terminal is unsupported, just use plain-java reading
            if (!terminal.isSupported()) {
                return readLine(in);
            }

            while (true) {
                int[] next = readBinding();

                if (next == null) {
                    return null;
                }

                int c = next[0];
                // int code = next[1];
                Operation code = Operation.valueOf(next[1]);

                if (c == -1) {
                    return null;
                }

                boolean success = true;

                switch (code) {
                    case EXIT: // ctrl-d
                        if (buf.buffer.length() == 0) {
                            return null;
                        }
                        break;

                    case COMPLETE: // tab
                        success = complete();
                        break;

                    case MOVE_TO_BEG:
                        success = setCursorPosition(0);
                        break;

                    case KILL_LINE: // CTRL-K
                        success = killLine();
                        break;

                    case CLEAR_SCREEN: // CTRL-L
                        success = clearScreen();
                        break;

                    case KILL_LINE_PREV: // CTRL-U
                        success = resetLine();
                        break;

                    case NEWLINE: // enter
                        moveToEnd();
                        printNewline(); // output newline
                        return finishBuffer();

                    case DELETE_PREV_CHAR: // backspace
                        success = backspace();
                        break;

                    case DELETE_NEXT_CHAR: // delete
                        success = deleteCurrentCharacter();
                        break;

                    case MOVE_TO_END:
                        success = moveToEnd();
                        break;

                    case PREV_CHAR:
                        success = moveCursor(-1) != 0;
                        break;

                    case NEXT_CHAR:
                        success = moveCursor(1) != 0;
                        break;

                    case NEXT_HISTORY:
                        success = moveHistory(true);
                        break;

                    case PREV_HISTORY:
                        success = moveHistory(false);
                        break;

                    case REDISPLAY:
                        break;

                    case PASTE:
                        success = paste();
                        break;

                    case DELETE_PREV_WORD:
                        success = deletePreviousWord();
                        break;

                    case PREV_WORD:
                        success = previousWord();
                        break;

                    case NEXT_WORD:
                        success = nextWord();
                        break;

                    case START_OF_HISTORY:
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

                    case CLEAR_LINE:
                        moveInternal(-(buf.buffer.length()));
                        killLine();
                        break;

                    case INSERT:
                        buf.setOverTyping(!buf.isOverTyping());
                        break;

                    case UNKNOWN:
                    default:
                        if (c != 0) { // ignore null chars
                            ActionListener action = triggeredActions.get((char) c);
                            if (action != null) {
                                action.actionPerformed(null);
                            }
                            else {
                                putChar(c, true);
                            }
                        }
                        else {
                            success = false;
                        }
                }

                if (!success) {
                    beep();
                }

                flushConsole();
            }
        }
        finally {
            terminal.afterReadLine(this, this.prompt, mask);
        }
    }

    private String readLine(final InputStream in) throws IOException {
        StringBuilder buf = new StringBuilder();

        while (true) {
            int i = in.read();

            if ((i == -1) || (i == '\n') || (i == '\r')) {
                return buf.toString();
            }

            buf.append((char) i);
        }

        // return new BufferedReader (new InputStreamReader (in)).readLine ();
    }

    /**
     * Reads the console input and returns an array of the form [raw, key binding].
     */
    private int[] readBinding() throws IOException {
        int c = readVirtualKey();

        if (c == -1) {
            return null;
        }

        // extract the appropriate key binding
        short code = keybindings[c];

        Log.trace("Translated: ", c, " -> ", code);

        return new int[]{ c, code };
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

    /**
     * Paste the contents of the clipboard into the console buffer
     *
     * @return true if clipboard contents pasted
     */
    public boolean paste() throws IOException {
        Clipboard clipboard;
        try { // May throw ugly exception on system without X
            clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
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
            Object content = transferable.getTransferData(DataFlavor.plainTextFlavor);

            /*
             * This fix was suggested in bug #1060649 at
             * http://sourceforge.net/tracker/index.php?func=detail&aid=1060649&group_id=64033&atid=506056
             * to get around the deprecated DataFlavor.plainTextFlavor, but it
             * raises a UnsupportedFlavorException on Mac OS X
             */
            if (content == null) {
                try {
                    content = new DataFlavor().getReaderForText(transferable);
                }
                catch (Exception e) {
                }
            }

            if (content == null) {
                return false;
            }

            String value;

            if (content instanceof Reader) {
                // TODO: we might want instead connect to the input stream
                // so we can interpret individual lines
                value = "";
                String line;

                BufferedReader read = new BufferedReader((Reader) content);
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

        int num = buf.buffer.length() - cp;
        clearAhead(num);

        for (int i = 0; i < num; i++) {
            buf.buffer.deleteCharAt(len - i - 1);
        }

        return true;
    }

    /**
     * Clear the screen by issuing the ANSI "clear screen" code.
     */
    public boolean clearScreen() throws IOException {
        if (!terminal.isAnsiSupported()) {
            return false;
        }

        // send the ANSI code to clear the screen
        printString(((char) 27) + "[2J");
        flushConsole();

        // then send the ANSI code to go to position 1,1
        printString(((char) 27) + "[1;1H");
        flushConsole();

        redrawLine();

        return true;
    }

    /**
     * Use the completors to modify the buffer with the appropriate completions.
     *
     * @return true if successful
     */
    private boolean complete() throws IOException {
        // debug ("tab for (" + buf + ")");
        if (completers.size() == 0) {
            return false;
        }

        List<String> candidates = new LinkedList<String>();
        String bufstr = buf.buffer.toString();
        int cursor = buf.cursor;

        int position = -1;

        for (Completer comp : completers) {
            if ((position = comp.complete(bufstr, cursor, candidates)) != -1) {
                break;
            }
        }

        // no candidates? Fail.
        if (candidates.size() == 0) {
            return false;
        }

        return completionHandler.complete(this, candidates, position);
    }

    public CursorBuffer getCursorBuffer() {
        return buf;
    }

    /**
     * Output the specified {@link Collection} in proper columns.
     *
     * @param stuff the stuff to print
     */
    public void printColumns(final Collection<String> stuff) throws IOException {
        if ((stuff == null) || (stuff.size() == 0)) {
            return;
        }

        int width = getTerminal().getWidth();
        int maxwidth = 0;

        for (Iterator i = stuff.iterator(); i.hasNext(); maxwidth = Math.max(maxwidth, i.next().toString().length())) {
            // empty
        }

        StringBuilder line = new StringBuilder();

        int showLines;

        if (usePagination) {
            showLines = getTerminal().getHeight() - 1; // page limit
        }
        else {
            showLines = Integer.MAX_VALUE;
        }

        for (String cur : stuff) {
            if ((line.length() + maxwidth) > width) {
                printString(line.toString().trim());
                printNewline();
                line.setLength(0);
                if (--showLines == 0) { // Overflow
                    printString(loc.getString("display-more"));
                    flushConsole();
                    int c = readVirtualKey();
                    if (c == '\r' || c == '\n') {
                        showLines = 1; // one step forward
                    }
                    else if (c != 'q') {
                        showLines = getTerminal().getHeight() - 1;
                    } // page forward

                    back(loc.getString("display-more").length());
                    if (c == 'q') {
                        break;
                    } // cancel
                }
            }

            pad(cur, maxwidth + 3, line);
        }

        if (line.length() > 0) {
            printString(line.toString().trim());
            printNewline();
            line.setLength(0);
        }
    }

    /**
     * Append <i>toPad</i> to the specified <i>appendTo</i>, as well as (<i>toPad.length () -
     * len</i>) spaces.
     *
     * @param toPad    the {@link String} to pad
     * @param len      the target length
     * @param appendTo the {@link StringBuilder} to which to append the padded
     *                 {@link String}.
     */
    private void pad(final String toPad, final int len, final StringBuilder appendTo) {
        appendTo.append(toPad);

        for (int i = 0; i < (len - toPad.length()); i++, appendTo.append(' ')) {
            // empty
        }
    }

    /**
     * Erase the current line.
     *
     * @return false if we failed (e.g., the buffer was empty)
     */
    final boolean resetLine() throws IOException {
        if (buf.cursor == 0) {
            return false;
        }

        backspaceAll();

        return true;
    }

    /**
     * Move the cursor position to the specified absolute index.
     */
    public final boolean setCursorPosition(final int position) throws IOException {
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

        int diff = buf.buffer.length() - sameIndex;

        backspace(diff); // go back for the differences
        killLine(); // clear to the end of the line
        buf.buffer.setLength(sameIndex); // the new length
        putString(buffer.substring(sameIndex)); // append the differences
    }

    /**
     * Clear the line and redraw it.
     */
    public final void redrawLine() throws IOException {
        printCharacter(RESET_LINE);
        flushConsole();
        drawLine();
    }

    /**
     * Output put the prompt + the current buffer
     */
    public final void drawLine() throws IOException {
        if (prompt != null) {
            printString(prompt);
        }

        printString(buf.buffer.toString());

        if (buf.length() != buf.cursor) // not at end of line
        {
            back(buf.length() - buf.cursor);
        } // sync
    }

    /**
     * Output a platform-dependant newline.
     */
    public final void printNewline() throws IOException {
        printString(CR);
        flushConsole();
    }

    /**
     * Clear the buffer and add its contents to the history.
     *
     * @return the former contents of the buffer.
     */
    final String finishBuffer() {
        String str = buf.buffer.toString();

        // we only add it to the history if the buffer is not empty
        // and if mask is null, since having a mask typically means
        // the string was a password. We clear the mask after this call
        if (str.length() > 0) {
            if (mask == null && useHistory) {
                history.add(str);
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
     * Write out the specified string to the buffer and the output stream.
     */
    public final void putString(final String str) throws IOException {
        buf.write(str);
        printString(str);
        drawBuffer();
    }

    /**
     * Output the specified string to the output stream (but not the buffer).
     */
    public final void printString(final String str) throws IOException {
        printCharacters(str.toCharArray());
    }

    /**
     * Output the specified character, both to the buffer and the output stream.
     */
    private void putChar(final int c, final boolean print) throws IOException {
        buf.write((char) c);

        if (print) {
            // no masking...
            if (mask == null) {
                printCharacter(c);
            }
            // null mask: don't print anything...
            else if (mask == 0) {
                ;
            }
            // otherwise print the mask...
            else {
                printCharacter(mask);
            }

            drawBuffer();
        }
    }

    /**
     * Redraw the rest of the buffer from the cursor onwards. This is necessary
     * for inserting text into the buffer.
     *
     * @param clear the number of characters to clear after the end of the buffer
     */
    private void drawBuffer(final int clear) throws IOException {
        // debug ("drawBuffer: " + clear);
        char[] chars = buf.buffer.substring(buf.cursor).toCharArray();
        if (mask != null) {
            Arrays.fill(chars, mask);
        }

        printCharacters(chars);

        clearAhead(clear);
        back(chars.length);
        flushConsole();
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
     */
    private void clearAhead(final int num) throws IOException {
        if (num == 0) {
            return;
        }

        // debug ("clearAhead: " + num);

        // print blank extra characters
        printCharacters(' ', num);

        // we need to flush here so a "clever" console
        // doesn't just ignore the redundancy of a space followed by
        // a backspace.
        flushConsole();

        // reset the visual cursor
        back(num);

        flushConsole();
    }

    /**
     * Move the visual cursor backwards without modifying the buffer cursor.
     */
    private void back(final int num) throws IOException {
        printCharacters(BACKSPACE, num);
        flushConsole();
    }

    /**
     * Issue an audible keyboard bell, if {@link #getBellEnabled} return true.
     */
    public void beep() throws IOException {
        if (getBellEnabled()) {
            printCharacter(KEYBOARD_BELL);
            // need to flush so the console actually beeps
            flushConsole();
        }
    }

    /**
     * Output the specified character to the output stream without manipulating
     * the current buffer.
     */
    private void printCharacter(final int c) throws IOException {
        if (c == '\t') {
            char cbuf[] = new char[TAB_WIDTH];
            Arrays.fill(cbuf, ' ');
            out.write(cbuf);
            return;
        }

        out.write(c);
    }

    /**
     * Output the specified characters to the output stream without manipulating
     * the current buffer.
     */
    private void printCharacters(final char[] chars) throws IOException {
        int len = 0;
        for (char c : chars) {
            if (c == '\t') {
                len += TAB_WIDTH;
            }
            else {
                len++;
            }
        }

        char cbuf[];
        if (len == chars.length) {
            cbuf = chars;
        }
        else {
            cbuf = new char[len];
            int pos = 0;
            for (char c : chars) {
                if (c == '\t') {
                    Arrays.fill(cbuf, pos, pos + TAB_WIDTH, ' ');
                    pos += TAB_WIDTH;
                }
                else {
                    cbuf[pos] = c;
                    pos++;
                }
            }
        }

        out.write(cbuf);
    }

    private void printCharacters(final char c, final int num) throws IOException {
        if (num == 1) {
            printCharacter(c);
        }
        else {
            char[] chars = new char[num];
            Arrays.fill(chars, c);
            printCharacters(chars);
        }
    }

    /**
     * Flush the console output stream. This is important for printout out
     * single characters (like a backspace or keyboard) that we want the console
     * to handle immedately.
     */
    public void flushConsole() throws IOException {
        out.flush();
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

        int count = 0;

        count = moveCursor(-1 * num) * -1;
        // debug ("Deleting from " + buf.cursor + " for " + count);
        buf.buffer.delete(buf.cursor, buf.cursor + count);
        drawBuffer(count);

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

    private boolean moveToEnd() throws IOException {
        if (moveCursor(1) == 0) {
            return false;
        }

        while (moveCursor(1) != 0) {
            // nothing
        }

        return true;
    }

    /**
     * Delete the character at the current position and redraw the remainder of
     * the buffer.
     */
    private boolean deleteCurrentCharacter() throws IOException {
        boolean success = buf.buffer.length() > 0;
        if (!success) {
            return false;
        }

        if (buf.cursor == buf.buffer.length()) {
            return false;
        }

        buf.buffer.deleteCharAt(buf.cursor);
        drawBuffer(1);
        return true;
    }

    private boolean previousWord() throws IOException {
        while (isDelimiter(buf.current()) && (moveCursor(-1) != 0)) {
            // nothing
        }

        while (!isDelimiter(buf.current()) && (moveCursor(-1) != 0)) {
            // nothing
        }

        return true;
    }

    private boolean nextWord() throws IOException {
        while (isDelimiter(buf.current()) && (moveCursor(1) != 0)) {
            // nothing
        }

        while (!isDelimiter(buf.current()) && (moveCursor(1) != 0)) {
            // nothing
        }

        return true;
    }

    private boolean deletePreviousWord() throws IOException {
        while (isDelimiter(buf.current()) && backspace()) {
            // nothing
        }

        while (!isDelimiter(buf.current()) && backspace()) {
            // nothing
        }

        return true;
    }

    /**
     * Move the cursor <i>where</i> characters.
     *
     * @param num if less than 0, move abs(<i>where</i>) to the left,
     *              otherwise move <i>where</i> to the right.
     * @return the number of spaces we moved
     */
    public int moveCursor(final int num) throws IOException {
        int where = num;

        if ((buf.cursor == 0) && (where < 0)) {
            return 0;
        }

        if ((buf.cursor == buf.buffer.length()) && (where > 0)) {
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
     * Move the cursor <i>where</i> characters, withough checking the current
     * buffer.
     *
     * @param where the number of characters to move to the right or left.
     */
    private void moveInternal(final int where) throws IOException {
        // debug ("move cursor " + where + " ("
        // + buf.cursor + " => " + (buf.cursor + where) + ")");
        buf.cursor += where;

        char c;

        if (where < 0) {
            int len = 0;
            for (int i = buf.cursor; i < buf.cursor - where; i++) {
                if (buf.buffer.charAt(i) == '\t') {
                    len += TAB_WIDTH;
                }
                else {
                    len++;
                }
            }

            char cbuf[] = new char[len];
            Arrays.fill(cbuf, BACKSPACE);
            out.write(cbuf);

            return;
        }
        else if (buf.cursor == 0) {
            return;
        }
        else if (mask != null) {
            c = mask;
        }
        else {
            printCharacters(buf.buffer.substring(buf.cursor - where, buf.cursor).toCharArray());
            return;
        }

        // null character mask: don't output anything
        if (NULL_MASK.equals(mask)) {
            return;
        }

        printCharacters(c, Math.abs(where));
    }

    /**
     * Read a character from the console.
     *
     * @return the character, or -1 if an EOF is received.
     */
    public final int readVirtualKey() throws IOException {
        int c = terminal.readVirtualKey(in);

        Log.trace("Keystroke: ", c);

        // clear any echo characters
        clearEcho(c);

        return c;
    }

    public final int readCharacter(final char[] allowed) throws IOException {
        // if we restrict to a limited set and the current character
        // is not in the set, then try again.
        char c;

        Arrays.sort(allowed); // always need to sort before binarySearch

        while (Arrays.binarySearch(allowed, c = (char) readVirtualKey()) < 0) {
            // nothing
        }

        return c;
    }


    /**
     * Issue <em>num</em> deletes.
     *
     * @return the number of characters backed up
     */
    private int delete(final int num) throws IOException {
        /* Commented out beacuse of DWA-2949:
        if (buf.cursor == 0) {
            return 0;
        }
        */

        buf.buffer.delete(buf.cursor, buf.cursor + 1);
        drawBuffer(1);

        return 1;
    }

    public final boolean replace(final int num, final  String replacement) {
        buf.buffer.replace(buf.cursor - num, buf.cursor, replacement);
        try {
            moveCursor(-num);
            drawBuffer(Math.max(0, num - replacement.length()));
            moveCursor(replacement.length());
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Issue a delete.
     *
     * @return true if successful
     */
    public final boolean delete() throws IOException {
        return delete(1) == 1;
    }
    
    /**
     * Checks to see if the specified character is a delimiter. We consider a
     * character a delimiter if it is anything but a letter or digit.
     *
     * @param c the character to test
     * @return true if it is a delimiter
     */
    private boolean isDelimiter(final char c) {
        return !Character.isLetterOrDigit(c);
    }
}
