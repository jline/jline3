/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */

package jline.console;

import jline.Terminal;
import jline.TerminalFactory;
import jline.console.history.MemoryHistory;
import jline.internal.Log;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
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
    public static final String JLINE_NOBELL = "jline.nobell";

    public static final char BACKSPACE = '\b';

    public static final char RESET_LINE = '\r';

    public static final char KEYBOARD_BELL = '\07';

    public static final char NULL_MASK = 0;

    public static final int TAB_WIDTH = 4;

    private static final ResourceBundle loc = ResourceBundle.getBundle(CandidateListCompletionHandler.class.getName());

    private final Terminal terminal;

    private InputStream in;

    private final Writer out;

    private final CursorBuffer buf = new CursorBuffer();

    private String prompt;

    private boolean bellEnabled = true;

    private Character mask;

    private Character echoCharacter;

    public ConsoleReader(final InputStream in, final Writer out, InputStream bindings, final Terminal term) throws IOException {
        this.in = in;
        this.out = out;
        this.terminal = term != null ? term : TerminalFactory.get();
        this.keyBindings = loadKeyBindings(bindings);
        
        if (Boolean.getBoolean(JLINE_NOBELL)) {
            setBellEnabled(false);
        }
    }

    public ConsoleReader(final InputStream in, final Writer out, final Terminal term) throws IOException {
        this(in, out, null, term);
    }

    public ConsoleReader(final InputStream in, final Writer out) throws IOException {
        this(in, out, null, null);
    }

    /**
     * Create a new reader using {@link FileDescriptor#in} for input and
     * {@link System#out} for output.
     *
     * {@link FileDescriptor#in} is used because it has a better chance of not being buffered.
     */
    public ConsoleReader() throws IOException {
        this(new FileInputStream(FileDescriptor.in), new PrintWriter(new OutputStreamWriter(System.out)), null, null);
    }

    // FIXME: Only used for tests
    void setInput(final InputStream in) {
        this.in = in;
    }

    public InputStream getInput() {
        return in;
    }

    public Writer getOutput() {
        return out;
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public CursorBuffer getCursorBuffer() {
        return buf;
    }

    public void setBellEnabled(final boolean enabled) {
        this.bellEnabled = enabled;
    }

    public boolean isBellEnabled() {
        return bellEnabled;
    }

    public void setPrompt(final String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }

    /**
     * Set the echo character. For example, to have "*" entered when a password is typed:
     *
     * <pre>
     * myConsoleReader.setEchoCharacter(new Character('*'));
     * </pre>
     *
     * Setting the character to
     *
     * <pre>
     * null
     * </pre>
     *
     * will restore normal character echoing. Setting the character to
     *
     * <pre>
     * new Character(0)
     * </pre>
     *
     * will cause nothing to be echoed.
     *
     * @param c the character to echo to the console in place of the typed character.
     */
    public void setEchoCharacter(final Character c) {
        this.echoCharacter = c;
    }

    /**
     * Returns the echo character.
     */
    public Character getEchoCharacter() {
        return echoCharacter;
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

    int getCursorPosition() {
        // FIXME: does not handle anything but a line with a prompt absolute position
        String prompt = getPrompt();
        return (prompt == null ? 0 : prompt.length()) + buf.cursor;
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
     * Output put the prompt + the current buffer
     */
    public final void drawLine() throws IOException {
        String prompt = getPrompt();
        if (prompt != null) {
            print(prompt);
        }

        print(buf.buffer.toString());

        if (buf.length() != buf.cursor) { // not at end of line
            back(buf.length() - buf.cursor);
        }
    }

    /**
     * Clear the line and redraw it.
     */
    public final void redrawLine() throws IOException {
        print(RESET_LINE);
        flush();
        drawLine();
    }

    /**
     * Clear the buffer and add its contents to the history.
     *
     * @return the former contents of the buffer.
     */
    final String finishBuffer() { // FIXME: Package protected because used by tests
        String str = buf.buffer.toString();

        // we only add it to the history if the buffer is not empty
        // and if mask is null, since having a mask typically means
        // the string was a password. We clear the mask after this call
        if (str.length() > 0) {
            if (mask == null && isHistoryEnabled()) {
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
    public final void putString(final CharSequence str) throws IOException {
        buf.write(str);
        print(str);
        drawBuffer();
    }

    /**
     * Output the specified character, both to the buffer and the output stream.
     */
    private void putChar(final int c, final boolean print) throws IOException {
        buf.write((char) c);

        if (print) {
            if (mask == null) {
                // no masking
                print(c);
            }
            else if (mask == NULL_MASK) {
                // Don't print anything
            }
            else {
                print(mask);
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

        print(chars);

        clearAhead(clear);
        back(chars.length);
        flush();
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
        print(' ', num);

        // we need to flush here so a "clever" console
        // doesn't just ignore the redundancy of a space followed by
        // a backspace.
        flush();

        // reset the visual cursor
        back(num);

        flush();
    }

    /**
     * Move the visual cursor backwards without modifying the buffer cursor.
     */
    private void back(final int num) throws IOException {
        print(BACKSPACE, num);
        flush();
    }

    /**
     * Flush the console output stream. This is important for printout out
     * single characters (like a backspace or keyboard) that we want the console
     * to handle immediately.
     */
    public void flush() throws IOException {
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
     * Delete the character at the current position and redraw the remainder of the buffer.
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
     * Move the cursor <i>where</i> characters, withough checking the current buffer.
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

            char chars[] = new char[len];
            Arrays.fill(chars, BACKSPACE);
            out.write(chars);

            return;
        }
        else if (buf.cursor == 0) {
            return;
        }
        else if (mask != null) {
            c = mask;
        }
        else {
            print(buf.buffer.substring(buf.cursor - where, buf.cursor).toCharArray());
            return;
        }

        // null character mask: don't output anything
        if (mask == NULL_MASK) {
            return;
        }

        print(c, Math.abs(where));
    }

    // FIXME: replace() is not used
    
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

    //
    // Key reading
    //

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

    /**
     * Clear the echoed characters for the specified character code.
     */
    private int clearEcho(final int c) throws IOException {
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

    private int countEchoCharacters(final char c) {
        // tabs as special: we need to determine the number of spaces
        // to cancel based on what out current cursor position is
        if (c == 9) {
            int tabStop = 8; // will this ever be different?
            int position = getCursorPosition();

            return tabStop - (position % tabStop);
        }

        return getPrintableCharacters(c).length();
    }

    /**
     * Return the number of characters that will be printed when the specified
     * character is echoed to the screen. Adapted from cat by Torbjorn Granlund,
     * as repeated in stty by David MacKenzie.
     */
    private StringBuilder getPrintableCharacters(final char ch) {
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

    public final int readCharacter(final char... allowed) throws IOException {
        // if we restrict to a limited set and the current character is not in the set, then try again.
        char c;

        Arrays.sort(allowed); // always need to sort before binarySearch

        while (Arrays.binarySearch(allowed, c = (char) readVirtualKey()) < 0) {
            // nothing
        }

        return c;
    }

    //
    // Key Bindings
    //

    public static final String JLINE_COMPLETION_THRESHOLD = "jline.completion.threshold";

    public static final String JLINE_KEYBINDINGS = "jline.keybindings";

    public static final String JLINEBINDINGS_PROPERTIES = ".jlinebindings.properties";

    /** The map for logical operations. */
    private final short[] keyBindings;

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
            input = getTerminal().getDefaultBindings();
        }

        short[] keyBindings = new short[Character.MAX_VALUE * 2];

        Arrays.fill(keyBindings, Operation.UNKNOWN.code);

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
                    keyBindings[code] = op.code;
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

        return keyBindings;
    }

    int getKeyForAction(final short logicalAction) {
        for (int i = 0; i < keyBindings.length; i++) {
            if (keyBindings[i] == logicalAction) {
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
     * Reads the console input and returns an array of the form [raw, key binding].
     */
    private int[] readBinding() throws IOException {
        int c = readVirtualKey();

        if (c == -1) {
            return null;
        }

        // extract the appropriate key binding
        short code = keyBindings[c];

        Log.trace("Translated: ", c, " -> ", code);

        return new int[]{ c, code };
    }

    //
    // Line Reading
    //

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
    public String readLine(String prompt, final Character mask) throws IOException {
        // prompt may be null
        // mask may be null

        // FIXME: This blows, each call to readLine will reset the console's state which doesn't seem very nice.
        this.mask = mask;
        if (prompt != null) {
            setPrompt(prompt);
        }
        else {
            prompt = getPrompt();
        }

        try {
            if (!terminal.isSupported()) {
                beforeReadLine(prompt, mask);
            }

            if (prompt != null && prompt.length() > 0) {
                out.write(prompt);
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
                        println(); // output newline
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

                flush();
            }
        }
        finally {
            if (!terminal.isSupported()) {
                afterReadLine();
            }
        }
    }

    /**
     * Read a line for unsupported terminals.
     */
    private String readLine(final InputStream in) throws IOException {
        StringBuilder buff = new StringBuilder();

        while (true) {
            int i = in.read();

            if (i == -1 || i == '\n' || i == '\r') {
                return buff.toString();
            }

            buff.append((char) i);
        }

        // return new BufferedReader (new InputStreamReader (in)).readLine ();
    }

    //
    // Completion
    //

    private final List<Completer> completers = new LinkedList<Completer>();

    private CompletionHandler completionHandler = new CandidateListCompletionHandler();

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
     * @param completer the {@link Completer} to remove
     * @return true if it was successfully removed
     */
    public boolean removeCompleter(final Completer completer) {
        return completers.remove(completer);
    }

    /**
     * Returns an unmodifiable list of all the completers.
     */
    public Collection<Completer> getCompleters() {
        return Collections.unmodifiableList(completers);
    }

    public void setCompletionHandler(final CompletionHandler handler) {
        this.completionHandler = handler;
    }

    public CompletionHandler getCompletionHandler() {
        return this.completionHandler;
    }

    /**
     * Use the completers to modify the buffer with the appropriate completions.
     *
     * @return true if successful
     */
    private boolean complete() throws IOException {
        // debug ("tab for (" + buf + ")");
        if (completers.size() == 0) {
            return false;
        }

        List<CharSequence> candidates = new LinkedList<CharSequence>();
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

        return getCompletionHandler().complete(this, candidates, position);
    }

    /**
     * The number of tab-completion candidates above which a warning will be
     * prompted before showing all the candidates.
     */
    private int autoprintThreshhold = Integer.getInteger(JLINE_COMPLETION_THRESHOLD, 100); // same default as bash

    /**
     * @param threshhold the number of candidates to print without issuing a warning.
     */
    public void setAutoprintThreshhold(final int threshhold) {
        this.autoprintThreshhold = threshhold;
    }

    /**
     * @return the number of candidates to print without issuing a warning.
     */
    public int getAutoprintThreshhold() {
        return autoprintThreshhold;
    }

    private boolean paginationEnabled;

    /**
     * Whether to use pagination when the number of rows of candidates exceeds the height of the terminal.
     */
    public void setPaginationEnabled(final boolean enabled) {
        this.paginationEnabled = enabled;
    }

    /**
     * Whether to use pagination when the number of rows of candidates exceeds the height of the terminal.
     */
    public boolean isPaginationEnabled() {
        return paginationEnabled;
    }

    //
    // History
    //

    private History history = new MemoryHistory();

    public void setHistory(final History history) {
        this.history = history;
    }

    public History getHistory() {
        return history;
    }

    private boolean historyEnabled = true;

    /**
     * Whether or not to add new commands to the history buffer.
     */
    public void setHistoryEnabled(final boolean enabled) {
        this.historyEnabled = enabled;
    }

    /**
     * Whether or not to add new commands to the history buffer.
     */
    public boolean isHistoryEnabled() {
        return historyEnabled;
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

    public static final String CR = System.getProperty("line.separator");

    /**
     * Output the specified character to the output stream without manipulating the current buffer.
     */
    private void print(final int c) throws IOException {
        if (c == '\t') {
            char chars[] = new char[TAB_WIDTH];
            Arrays.fill(chars, ' ');
            out.write(chars);
            return;
        }

        out.write(c);
    }

    /**
     * Output the specified characters to the output stream without manipulating the current buffer.
     */
    private void print(final char... buff) throws IOException {
        int len = 0;
        for (char c : buff) {
            if (c == '\t') {
                len += TAB_WIDTH;
            }
            else {
                len++;
            }
        }

        char chars[];
        if (len == buff.length) {
            chars = buff;
        }
        else {
            chars = new char[len];
            int pos = 0;
            for (char c : buff) {
                if (c == '\t') {
                    Arrays.fill(chars, pos, pos + TAB_WIDTH, ' ');
                    pos += TAB_WIDTH;
                }
                else {
                    chars[pos] = c;
                    pos++;
                }
            }
        }

        out.write(chars);
    }

    private void print(final char c, final int num) throws IOException {
        if (num == 1) {
            print(c);
        }
        else {
            char[] chars = new char[num];
            Arrays.fill(chars, c);
            print(chars);
        }
    }

    /**
     * Output the specified string to the output stream (but not the buffer).
     */
    public final void print(final CharSequence s) throws IOException {
        assert s != null;
        print(s.toString().toCharArray());
    }

    public final void println(final CharSequence s) throws IOException {
        assert s != null;
        print(s.toString().toCharArray());
        println();
    }

    /**
     * Output a platform-dependant newline.
     */
    public final void println() throws IOException {
        print(CR);
        flush();
    }

    //
    // Actions
    //

    /**
     * Issue a delete.
     *
     * @return true if successful
     */
    public final boolean delete() throws IOException {
        return delete(1) == 1;
    }

    // FIXME: delete(int) only used by above + the return is always 1 and num is ignored

    /**
     * Issue <em>num</em> deletes.
     *
     * @return the number of characters backed up
     */
    private int delete(final int num) throws IOException {
        /* Commented out because of DWA-2949:
        if (buf.cursor == 0) {
            return 0;
        }
        */

        buf.buffer.delete(buf.cursor, buf.cursor + 1);
        drawBuffer(1);

        return 1;
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
        print(((char) 27) + "[2J");
        flush();

        // then send the ANSI code to go to position 1,1
        print(((char) 27) + "[1;1H");
        flush();

        redrawLine();

        return true;
    }

    /**
     * Issue an audible keyboard bell, if {@link #isBellEnabled} return true.
     */
    public void beep() throws IOException {
        if (isBellEnabled()) {
            print(KEYBOARD_BELL);
            // need to flush so the console actually beeps
            flush();
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

            // This fix was suggested in bug #1060649 at
            // http://sourceforge.net/tracker/index.php?func=detail&aid=1060649&group_id=64033&atid=506056
            // to get around the deprecated DataFlavor.plainTextFlavor, but it
            // raises a UnsupportedFlavorException on Mac OS X
            
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

    //
    // Triggered Actions
    //

    private final Map<Character,ActionListener> triggeredActions = new HashMap<Character,ActionListener>();

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

    //
    // Formatted Output
    //

    /**
     * Output the specified {@link Collection} in proper columns.
     */
    public void printColumns(final Collection<? extends CharSequence> items) throws IOException {
        if (items == null || items.isEmpty()) {
            return;
        }

        int width = getTerminal().getWidth();
        int height = getTerminal().getHeight();

        int maxWidth = 0;
        Iterator<? extends CharSequence> iter = items.iterator();
        while (iter.hasNext()) {
            maxWidth = Math.max(maxWidth, iter.next().length());
        }
        Log.debug("Max width: ", maxWidth);

        int showLines;
        if (isPaginationEnabled()) {
            showLines = height - 1; // page limit
        }
        else {
            showLines = Integer.MAX_VALUE;
        }

        StringBuilder buff = new StringBuilder();
        for (CharSequence item : items) {
            if ((buff.length() + maxWidth) > width) {
                println(buff);
                buff.setLength(0);

                if (--showLines == 0) {
                    // Overflow
                    print(loc.getString("display-more"));
                    flush();
                    int c = readVirtualKey();
                    if (c == '\r' || c == '\n') {
                        // one step forward
                        showLines = 1;
                    }
                    else if (c != 'q') {
                        // page forward
                        showLines = height - 1;
                    }

                    back(loc.getString("display-more").length());
                    if (c == 'q') {
                        // cancel
                        break;
                    }
                }
            }

            // NOTE: toString() is important here due to AnsiString being retarded
            buff.append(item.toString());
            for (int i=0; i < (maxWidth + 3 - item.length()); i++) {
                buff.append(' ');
            }
        }

        if (buff.length() > 0) {
            println(buff);
        }
    }

    //
    // Non-supported Terminal Support
    //

    private Thread maskThread;

    private void beforeReadLine(final String prompt, final Character mask) {
        if (mask != null && maskThread == null) {
            final String fullPrompt = "\r" + prompt
                + "                 "
                + "                 "
                + "                 "
                + "\r" + prompt;

            maskThread = new Thread() {
                public void run() {
                    while (!interrupted()) {
                        try {
                            Writer out = getOutput();
                            out.write(fullPrompt);
                            out.flush();
                            sleep(3);
                        }
                        catch (IOException e) {
                            return;
                        }
                        catch (InterruptedException e) {
                            return;
                        }
                    }
                }
            };

            maskThread.setPriority(Thread.MAX_PRIORITY);
            maskThread.setDaemon(true);
            maskThread.start();
        }
    }

    private void afterReadLine() {
        if (maskThread != null && maskThread.isAlive()) {
            maskThread.interrupt();
        }

        maskThread = null;
    }

    //
    // Helpers
    //

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
