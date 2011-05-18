/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */

package jline.console;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.ResourceBundle;

import jline.Terminal;
import jline.TerminalFactory;
import jline.console.completer.CandidateListCompletionHandler;
import jline.console.completer.Completer;
import jline.console.completer.CompletionHandler;
import jline.console.history.History;
import jline.console.history.MemoryHistory;
import jline.internal.Configuration;
import jline.internal.InputStreamReader;
import jline.internal.Log;
import org.fusesource.jansi.AnsiOutputStream;

/**
 * A reader for console applications. It supports custom tab-completion,
 * saveable command history, and command line editing. On some platforms,
 * platform-specific commands will need to be issued before the reader will
 * function properly. See {@link jline.Terminal#init} for convenience
 * methods for issuing platform-specific setup commands.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
public class ConsoleReader
{
    public static final String JLINE_NOBELL = "jline.nobell";

    public static final char BACKSPACE = '\b';

    public static final char RESET_LINE = '\r';

    public static final char KEYBOARD_BELL = '\07';

    public static final char NULL_MASK = 0;

    public static final int TAB_WIDTH = 4;

    private static final ResourceBundle
        resources = ResourceBundle.getBundle(CandidateListCompletionHandler.class.getName());

    private final Terminal terminal;

    private InputStream in;

    private final Writer out;

    private final CursorBuffer buf = new CursorBuffer();

    private String prompt;

    private boolean expandEvents = true;

    private Character mask;

    private Character echoCharacter;

    private StringBuffer searchTerm = null;

    private String previousSearchTerm = "";

    private int searchIndex = -1;

    private Reader reader;

    private String encoding;

    private boolean recording;

    private String macro = "";

    private String appName;

    private KeyMap keys;

    private boolean viEditMode;

    private Map<String, KeyMap> keyMaps;

    private URL inputrcUrl;

    private boolean skipLF = false;

    public ConsoleReader() throws IOException {
        this(null, new FileInputStream(FileDescriptor.in), System.out, null);
    }

    public ConsoleReader(final InputStream in, final OutputStream out) throws
        IOException
    {
        this(null, in, out, null);
    }

    public ConsoleReader(final InputStream in, final OutputStream out, final Terminal term) throws
        IOException
    {
        this(null, in, out, term);
    }

    public ConsoleReader(final String appName, final InputStream in, final OutputStream out, final Terminal term) throws
        IOException
    {
        this.appName = appName != null ? appName : "JLine";
        this.encoding = encoding != null ? encoding : Configuration.getEncoding();
        this.terminal = term != null ? term : TerminalFactory.get();
        this.out = new OutputStreamWriter(terminal.wrapOutIfNeeded(out), this.encoding);
        setInput( in );

        this.inputrcUrl = Configuration.getUrlFrom(Configuration.getString(Configuration.JLINE_INPUTRC,
                            Configuration.getUrlFrom(new File(Configuration.getUserHome(), Configuration.INPUT_RC)).toExternalForm()));
        keyMaps = KeyMap.keyMaps();
        loadKeys();
    }

    public void loadKeys() {
        keys = keyMaps.get("emacs");
        try {
            InputStream input = this.inputrcUrl.openStream();
            try {
                loadKeys(input);
                Log.debug("Loaded user configuration: ", this.inputrcUrl);
            }
            finally {
                try {
                    input.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        catch (IOException e) {
            if (this.inputrcUrl.getProtocol().equals("file")) {
                File file = new File(this.inputrcUrl.getPath());
                if (file.exists()) {
                    Log.warn("Unable to read user configuration: ", this.inputrcUrl, e);
                }
            } else {
                Log.warn("Unable to read user configuration: ", this.inputrcUrl, e);
            }
        }
        keys.bindArrowKeys();
        keys = viEditMode ? keyMaps.get("vi") : keyMaps.get("emacs");
    }

    private void loadKeys(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader( new java.io.InputStreamReader( input ) );
        String line;
        boolean parsing = true;
        List<Boolean> ifsStack = new ArrayList<Boolean>();
        while ( (line = reader.readLine()) != null ) {
            line = line.trim();
            if (line.length() == 0) {
                continue;
            }
            if (line.charAt(0) == '#') {
                continue;
            }
            int i = 0;
            if (line.charAt(i) == '$') {
                String cmd;
                String args;
                for (++i; i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t'); i++);
                int s = i;
                for (; i < line.length() && (line.charAt(i) != ' ' && line.charAt(i) != '\t'); i++);
                cmd = line.substring(s, i);
                for (; i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t'); i++);
                s = i;
                for (; i < line.length() && (line.charAt(i) != ' ' && line.charAt(i) != '\t'); i++);
                args = line.substring(s, i);
                if ("if".equalsIgnoreCase(cmd)) {
                    ifsStack.add( parsing );
                    if (!parsing) {
                        continue;
                    }
                    if (args.startsWith("term=")) {
                        // TODO
                    } else if (args.startsWith("mode=")) {
                        if (args.equalsIgnoreCase("mode=vi")) {
                            parsing = viEditMode;
                        } else if (args.equals("mode=emacs")) {
                            parsing = !viEditMode;
                        } else {
                            parsing = false;
                        }
                    } else {
                        parsing = args.equalsIgnoreCase(appName);
                    }
                } else if ("else".equalsIgnoreCase(cmd)) {
                    if (ifsStack.isEmpty()) {
                        throw new IllegalArgumentException("$else found without matching $if");
                    }
                    boolean invert = true;
                    for (boolean b : ifsStack) {
                        if (!b) {
                            invert = false;
                            break;
                        }
                    }
                    if (invert) {
                        parsing = !parsing;
                    }
                } else if ("endif".equalsIgnoreCase(cmd)) {
                    if (ifsStack.isEmpty()) {
                        throw new IllegalArgumentException("endif found without matching $if");
                    }
                    parsing = ifsStack.remove( ifsStack.size() - 1 );
                } else if ("include".equalsIgnoreCase(cmd)) {
                    // TODO
                }
                continue;
            }
            if (!parsing) {
                continue;
            }
            boolean equivalency;
            String keySeq = "";
            if (line.charAt(i++) == '"') {
                boolean esc = false;
                for (;; i++) {
                    if (i >= line.length()) {
                        throw new IllegalArgumentException("Missing closing quote on line '" + line + "'");
                    }
                    if (esc) {
                        esc = false;
                    } else if (line.charAt(i) == '\\') {
                        esc = true;
                    } else if (line.charAt(i) == '"') {
                        break;
                    }
                }
            }
            for (; i < line.length() && line.charAt(i) != ':'
                    && line.charAt(i) != ' ' && line.charAt(i) != '\t'
                    ; i++);
            keySeq = line.substring(0, i);
            equivalency = (i + 1 < line.length() && line.charAt(i) == ':' && line.charAt(i + 1) == '=');
            i++;
            if (equivalency) {
                i++;
            }
            if (keySeq.equalsIgnoreCase("set")) {
                String key;
                String val;
                for (; i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t'); i++);
                int s = i;
                for (; i < line.length() && (line.charAt(i) != ' ' && line.charAt(i) != '\t'); i++);
                key = line.substring( s, i );
                for (; i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t'); i++);
                s = i;
                for (; i < line.length() && (line.charAt(i) != ' ' && line.charAt(i) != '\t'); i++);
                val = line.substring( s, i );
                setVar( key, val );
            } else {
                for (; i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t'); i++);
                int start = i;
                if (i < line.length() && (line.charAt(i) == '\'' || line.charAt(i) == '\"')) {
                    char delim = line.charAt(i++);
                    boolean esc = false;
                    for (;; i++) {
                        if (i >= line.length()) {
                            break;
                        }
                        if (esc) {
                            esc = false;
                        } else if (line.charAt(i) == '\\') {
                            esc = true;
                        } else if (line.charAt(i) == delim) {
                            break;
                        }
                    }
                }
                for (; i < line.length() && line.charAt(i) != ' ' && line.charAt(i) != '\t'; i++);
                String val = line.substring(Math.min(start, line.length()), Math.min(i, line.length()));
                if (keySeq.charAt(0) == '"') {
                    keySeq = translateQuoted(keySeq);
                } else {
                    // Bind key name
                    String keyName = keySeq.lastIndexOf('-') > 0 ? keySeq.substring( keySeq.lastIndexOf('-') + 1 ) : keySeq;
                    char key = getKeyFromName(keyName);
                    keyName = keySeq.toLowerCase();
                    keySeq = "";
                    if (keyName.contains("meta-") || keyName.contains("m-")) {
                        keySeq += "\u001b";
                    }
                    if (keyName.contains("control-") || keyName.contains("c-") || keyName.contains("ctrl-")) {
                        key = (char)(Character.toUpperCase( key ) & 0x1f);
                    }
                    keySeq += key;
                }
                if (val.length() > 0 && (val.charAt(0) == '\'' || val.charAt(0) == '\"')) {
                    keys.bind( keySeq, translateQuoted(val) );
                } else {
                    val = val.replace('-', '_').toUpperCase();
                    keys.bind( keySeq, Operation.valueOf(val) );
                }
            }
        }

    }

    private String translateQuoted(String keySeq) {
        int i;
        String str = keySeq.substring( 1, keySeq.length() - 1 );
        keySeq = "";
        for (i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\\') {
                boolean ctrl = str.regionMatches(i, "\\C-", 0, 3)|| str.regionMatches(i, "\\M-\\C-", 0, 6);
                boolean meta = str.regionMatches(i, "\\M-", 0, 3)|| str.regionMatches(i, "\\C-\\M-", 0, 6);
                i += (meta ? 3 : 0) + (ctrl ? 3 : 0) + (!meta && !ctrl ? 1 : 0);
                if (i >= str.length()) {
                    break;
                }
                c = str.charAt(i);
                if (meta) {
                    keySeq += "\u001b";
                }
                if (ctrl) {
                    c = c == '?' ? 0x7f : (char)(Character.toUpperCase( c ) & 0x1f);
                }
                if (!meta && !ctrl) {
                    switch (c) {
                        case 'a': c = 0x07; break;
                        case 'b': c = '\b'; break;
                        case 'd': c = 0x7f; break;
                        case 'e': c = 0x1b; break;
                        case 'f': c = '\f'; break;
                        case 'n': c = '\n'; break;
                        case 'r': c = '\r'; break;
                        case 't': c = '\t'; break;
                        case 'v': c = 0x0b; break;
                        case '\\': c = '\\'; break;
                        case '0': case '1': case '2': case '3':
                        case '4': case '5': case '6': case '7':
                            c = 0;
                            for (int j = 0; j < 3; j++, i++) {
                                if (i >= str.length()) {
                                    break;
                                }
                                int k = Character.digit(str.charAt(i), 8);
                                if (k < 0) {
                                    break;
                                }
                                c = (char)(c * 8 + k);
                            }
                            c &= 0xFF;
                            break;
                        case 'x':
                            i++;
                            c = 0;
                            for (int j = 0; j < 2; j++, i++) {
                                if (i >= str.length()) {
                                    break;
                                }
                                int k = Character.digit(str.charAt(i), 16);
                                if (k < 0) {
                                    break;
                                }
                                c = (char)(c * 16 + k);
                            }
                            c &= 0xFF;
                            break;
                        case 'u':
                            i++;
                            c = 0;
                            for (int j = 0; j < 4; j++, i++) {
                                if (i >= str.length()) {
                                    break;
                                }
                                int k = Character.digit(str.charAt(i), 16);
                                if (k < 0) {
                                    break;
                                }
                                c = (char)(c * 16 + k);
                            }
                            break;
                    }
                }
                keySeq += c;
            } else {
                keySeq += c;
            }
        }
        return keySeq;
    }

    private static char getKeyFromName(String name) {
        if ("DEL".equalsIgnoreCase(name) || "Rubout".equalsIgnoreCase(name)) {
            return 0x7f;
        } else if ("ESC".equalsIgnoreCase(name) || "Escape".equalsIgnoreCase(name)) {
            return '\033';
        } else if ("LFD".equalsIgnoreCase(name) || "NewLine".equalsIgnoreCase(name)) {
            return '\n';
        } else if ("RET".equalsIgnoreCase(name) || "Return".equalsIgnoreCase(name)) {
            return '\r';
        } else if ("SPC".equalsIgnoreCase(name) || "Space".equalsIgnoreCase(name)) {
            return ' ';
        } else if ("Tab".equalsIgnoreCase(name)) {
            return '\t';
        } else {
            return name.charAt(0);
        }
    }

    public KeyMap getKeys() {
        return keys;
    }

    private void setVar(String key, String val) {
        if ("keymap".equalsIgnoreCase(key)) {
            if (keyMaps.containsKey(val)) {
                keys = keyMaps.get(val);
            }
        } else if ("editing-mode".equals(key)) {
            if ("vi".equalsIgnoreCase(val)) {
                keys = keyMaps.get("vi-insert");
                viEditMode = true;
            } else if ("emacs".equalsIgnoreCase(key)) {
                keys = keyMaps.get("emacs");
                viEditMode = false;
            }
        }
        // TODO
    }

    void setInput(final InputStream in) throws IOException {
        final InputStream wrapped = terminal.wrapInIfNeeded( in );
        // Wrap the input stream so that characters are only read one by one
        this.in = new FilterInputStream(wrapped) {
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (b == null) {
                    throw new NullPointerException();
                } else if (off < 0 || len < 0 || len > b.length - off) {
                    throw new IndexOutOfBoundsException();
                } else if (len == 0) {
                    return 0;
                }

                int c = read();
                if (c == -1) {
                    return -1;
                }
                b[off] = (byte)c;
                return 1;
            }
        };
        this.reader = new InputStreamReader( this.in, encoding );
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

    public void setExpandEvents(final boolean expand) {
        this.expandEvents = expand;
    }

    public boolean getExpandEvents() {
        return expandEvents;
    }

    public void setPrompt(final String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }

    /**
     * Set the echo character. For example, to have "*" entered when a password is typed:
     * <p/>
     * <pre>
     * myConsoleReader.setEchoCharacter(new Character('*'));
     * </pre>
     * <p/>
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
    protected final boolean resetLine() throws IOException {
        if (buf.cursor == 0) {
            return false;
        }

        backspaceAll();

        return true;
    }

    int getCursorPosition() {
        // FIXME: does not handle anything but a line with a prompt absolute position
        String prompt = getPrompt();
        return ((prompt == null) ? 0 : stripAnsi(lastLine(prompt)).length()) + buf.cursor;
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

    private String stripAnsi(String str) {
        if (str == null) return "";
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            AnsiOutputStream aos = new AnsiOutputStream(baos);
            aos.write(str.getBytes());
            aos.flush();
            return baos.toString();
        } catch (IOException e) {
            return str;
        }
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
            back(buf.length() - buf.cursor - 1);
        }
        // force drawBuffer to check for weird wrap (after clear screen)
        drawBuffer();
    }

    /**
     * Clear the line and redraw it.
     */
    public final void redrawLine() throws IOException {
        print(RESET_LINE);
//        flush();
        drawLine();
    }

    /**
     * Clear the buffer and add its contents to the history.
     *
     * @return the former contents of the buffer.
     */
    final String finishBuffer() throws IOException { // FIXME: Package protected because used by tests
        String str = buf.buffer.toString();

        if (expandEvents) {
            str = expandEvents(str);
        }

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
     * Expand event designator such as !!, !#, !3, etc...
     * See http://www.gnu.org/software/bash/manual/html_node/Event-Designators.html
     *
     * @param str
     * @return
     */
    protected String expandEvents(String str) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
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
                                idx = 0;
                                try {
                                    idx = Integer.parseInt(str.substring(i1, i));
                                } catch (NumberFormatException e) {
                                    throw new IllegalArgumentException((neg ? "!-" : "!") + str.substring(i1, i) + ": event not found");
                                }
                                if (neg) {
                                    if (idx < history.size()) {
                                        rep = (history.get(history.index() - idx)).toString();
                                    } else {
                                        throw new IllegalArgumentException((neg ? "!-" : "!") + str.substring(i1, i) + ": event not found");
                                    }
                                } else {
                                    if (idx >= history.index() - history.size() && idx < history.index()) {
                                        rep = (history.get(idx)).toString();
                                    } else {
                                        throw new IllegalArgumentException((neg ? "!-" : "!") + str.substring(i1, i) + ": event not found");
                                    }
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
        if (buf.cursor == buf.length() && clear == 0) {
        } else {
            char[] chars = buf.buffer.substring(buf.cursor).toCharArray();
            if (mask != null) {
                Arrays.fill(chars, mask);
            }
            if (terminal.hasWeirdWrap()) {
                // need to determine if wrapping will occur:
                int width = terminal.getWidth();
                int pos = getCursorPosition();
                for (int i = 0; i < chars.length; i++) {
                    print(chars[i]);
                    if ((pos + i + 1) % width == 0) {
                        print(32); // move cursor to next line by printing dummy space
                        print(13); // CR / not newline.
                    }
                }
            } else {
                print(chars);
            }
            clearAhead(clear, chars.length);
            if (terminal.isAnsiSupported()) {
                if (chars.length > 0) {
                    back(chars.length);
                }
            } else {
                back(chars.length);
            }
        }
        if (terminal.hasWeirdWrap()) {
            int width = terminal.getWidth();
            // best guess on whether the cursor is in that weird location...
            // Need to do this without calling ansi cursor location methods
            // otherwise it breaks paste of wrapped lines in xterm.
            if (getCursorPosition() > 0 && (getCursorPosition() % width == 0)
                    && buf.cursor == buf.length() && clear == 0) {
                // the following workaround is reverse-engineered from looking
                // at what bash sent to the terminal in the same situation
                print(32); // move cursor to next line by printing dummy space
                print(13); // CR / not newline.
            }
        }
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
     * @param delta the difference between the internal cursor and the screen
     * cursor - if > 0, assume some stuff was printed and weird wrap has to be
     * checked
     */
    private void clearAhead(final int num, int delta) throws IOException {
        if (num == 0) {
            return;
        }

        if (terminal.isAnsiSupported()) {
            int width = terminal.getWidth();
            int screenCursorCol = getCursorPosition() + delta;
            // clear current line
            printAnsiSequence("K");
            // if cursor+num wraps, then we need to clear the line(s) below too
            int curCol = screenCursorCol % width;
            int endCol = (screenCursorCol + num - 1) % width;
            int lines = num / width;
            if (endCol < curCol) lines++;
            for (int i = 0; i < lines; i++) {
                printAnsiSequence("B");
                printAnsiSequence("2K");
            }
            for (int i = 0; i < lines; i++) {
                printAnsiSequence("A");
            }
            return;
        }

        // print blank extra characters
        print(' ', num);

        // we need to flush here so a "clever" console doesn't just ignore the redundancy
        // of a space followed by a backspace.
//        flush();

        // reset the visual cursor
        back(num);

//        flush();
    }

    /**
     * Move the visual cursor backwards without modifying the buffer cursor.
     */
    protected void back(final int num) throws IOException {
        if (num == 0) return;
        if (terminal.isAnsiSupported()) {
            int width = getTerminal().getWidth();
            int cursor = getCursorPosition();
            int realCursor = cursor + num;
            int realCol  = realCursor % width;
            int newCol = cursor % width;
            int moveup = num / width;
            int delta = realCol - newCol;
            if (delta < 0) moveup++;
            if (moveup > 0) {
                printAnsiSequence(moveup + "A");
            }
            printAnsiSequence((1 + newCol) + "G");
            return;
        }
        print(BACKSPACE, num);
//        flush();
    }

    /**
     * Flush the console output stream. This is important for printout out single characters (like a backspace or
     * keyboard) that we want the console to handle immediately.
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

        int termwidth = getTerminal().getWidth();
        int lines = getCursorPosition() / termwidth;
        count = moveCursor(-1 * num) * -1;
        buf.buffer.delete(buf.cursor, buf.cursor + count);
        if (getCursorPosition() / termwidth != lines) {
            if (terminal.isAnsiSupported()) {
                // debug("doing backspace redraw: " + getCursorPosition() + " on " + termwidth + ": " + lines);
                printAnsiSequence("K");
                // if cursor+num wraps, then we need to clear the line(s) below too
                // last char printed is one pos less than cursor so we subtract
                // one
/*
                // TODO: fixme (does not work - test with reverse search with wrapping line and CTRL-E)
                int endCol = (getCursorPosition() + num - 1) % termwidth;
                int curCol = getCursorPosition() % termwidth;
                if (endCol < curCol) lines++;
                for (int i = 1; i < lines; i++) {
                    printAnsiSequence("B");
                    printAnsiSequence("2K");
                }
                for (int i = 1; i < lines; i++) {
                    printAnsiSequence("A");
                }
                return count;
*/
            }
        }
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

    protected boolean moveToEnd() throws IOException {
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

        if (terminal.isAnsiSupported()) {
            if (where < 0) {
                back(Math.abs(where));
            } else {
                int width = getTerminal().getWidth();
                int cursor = getCursorPosition();
                int oldLine = (cursor - where) / width;
                int newLine = cursor / width;
                if (newLine > oldLine) {
                    if (terminal.hasWeirdWrap()) {
                        // scroll up if at bottom
                        // note:
                        //   on rxvt cywgin terminal.getHeight() is incorrect
                        //   MacOs xterm does not seem to support scrolling
                        if (getCurrentAnsiRow() == terminal.getHeight()) {
                            printAnsiSequence((newLine - oldLine) + "S");
                        }
                    }
                    printAnsiSequence((newLine - oldLine) + "B");
                }
                printAnsiSequence(1 +(cursor % width) + "G");
            }
//            flush();
            return;
        }

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

    public final boolean replace(final int num, final String replacement) {
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
     * Read a character from the console.
     *
     * @return the character, or -1 if an EOF is received.
     */
    public final int readCharater() throws IOException {
        int c = reader.read();
        if (c >= 0) {
            Log.trace("Keystroke: ", c);
            // clear any echo characters
            clearEcho(c);
        }
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
        int num = countEchoCharacters(c);
        back(num);
        drawBuffer(num);

        return num;
    }

    private int countEchoCharacters(final int c) {
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
     * character is echoed to the screen
     *
     * Adapted from cat by Torbjorn Granlund, as repeated in stty by David MacKenzie.
     */
    private StringBuilder getPrintableCharacters(final int ch) {
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

        while (Arrays.binarySearch(allowed, c = (char) readCharater()) < 0) {
            // nothing
        }

        return c;
    }

    //
    // Key Bindings
    //

    public static final String JLINE_COMPLETION_THRESHOLD = "jline.completion.threshold";

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
     * @param prompt    The prompt to issue to the console, may be null.
     * @return          A line that is read from the terminal, or null if there was null input (e.g., <i>CTRL-D</i>
 *                      was pressed).
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
                return readLineSimple();
            }

            String originalPrompt = this.prompt;

            final int NORMAL = 1;
            final int SEARCH = 2;
            int state = NORMAL;

            boolean success = true;

            StringBuilder sb = new StringBuilder();
            List<Character> pushBackChar = new ArrayList<Character>();
            while (true) {
                int c = pushBackChar.isEmpty() ? readCharater() : pushBackChar.remove( pushBackChar.size() - 1 );
                if (c == -1) {
                    return null;
                }
                sb.append( (char) c );
                if (recording) {
                    macro += (char) c;
                }

                Object o = keys.getBound( sb );
                if (o == Operation.DO_LOWERCASE_VERSION) {
                    sb.setLength( sb.length() - 1);
                    sb.append( Character.toLowerCase( (char) c ));
                    o = keys.getBound( sb );
                }
                if ( o instanceof KeyMap ) {
                    continue;
                }
                while ( o == null && sb.length() > 0 ) {
                    c = sb.charAt( sb.length() - 1 );
                    sb.setLength( sb.length() - 1 );
                    Object o2 = keys.getBound( sb );
                    if ( o2 instanceof KeyMap ) {
                        o = ((KeyMap) o2).getAnotherKey();
                        if ( o == null ) {
                            continue;
                        } else {
                            pushBackChar.add( (char) c );
                        }
                    }
                }
                if ( o == null ) {
                    continue;
                }
                Log.trace("Binding: ", o);


                // Handle macros
                if (o instanceof String) {
                    String macro = (String) o;
                    for (int i = 0; i < macro.length(); i++) {
                        pushBackChar.add(macro.charAt(macro.length() - 1 - i));
                    }
                    sb.setLength( 0 );
                    continue;
                }

                // Handle custom callbacks
                if (o instanceof ActionListener) {
                    ((ActionListener) o).actionPerformed(null);
                    continue;
                }

                // Search mode.
                //
                // Note that we have to do this first, because if there is a command
                // not linked to a search command, we leave the search mode and fall
                // through to the normal state.
                if (state == SEARCH) {
                    int cursorDest = -1;
                    switch ( ((Operation) o )) {
                        case ABORT:
                            state = NORMAL;
                            break;

                        case REVERSE_SEARCH_HISTORY:
                            if (searchTerm.length() == 0) {
                                searchTerm.append(previousSearchTerm);
                            }

                            if (searchIndex == -1) {
                                searchIndex = searchBackwards(searchTerm.toString());
                            } else {
                                searchIndex = searchBackwards(searchTerm.toString(), searchIndex);
                            }
                            break;

                        case BACKWARD_DELETE_CHAR:
                            if (searchTerm.length() > 0) {
                                searchTerm.deleteCharAt(searchTerm.length() - 1);
                                searchIndex = searchBackwards(searchTerm.toString());
                            }
                            break;

                        case SELF_INSERT:
                            searchTerm.appendCodePoint(c);
                            searchIndex = searchBackwards(searchTerm.toString());
                            break;

                        default:
                            // Set buffer and cursor position to the found string.
                            if (searchIndex != -1) {
                                history.moveTo(searchIndex);
                                // set cursor position to the found string
                                cursorDest = history.current().toString().indexOf(searchTerm.toString());
                            }
                            state = NORMAL;
                            break;
                    }

                    // if we're still in search mode, print the search status
                    if (state == SEARCH) {
                        if (searchTerm.length() == 0) {
                            printSearchStatus("", "");
                            searchIndex = -1;
                        } else {
                            if (searchIndex == -1) {
                                beep();
                            } else {
                                printSearchStatus(searchTerm.toString(), history.get(searchIndex).toString());
                            }
                        }
                    }
                    // otherwise, restore the line
                    else {
                        restoreLine(originalPrompt, cursorDest);
                    }
                }
                if (state == NORMAL) {
                    if ( o instanceof Operation) {
                        switch ( ((Operation) o )) {
                            case COMPLETE: // tab
                                success = complete();
                                break;

                            case POSSIBLE_COMPLETIONS:
                                printCompletionCandidates();
                                success = true;
                                break;

                            case BEGINNING_OF_LINE:
                                success = setCursorPosition(0);
                                break;

                            case KILL_LINE: // CTRL-K
                                success = killLine();
                                break;

                            case KILL_WHOLE_LINE:
                                success = setCursorPosition(0) && killLine();
                                break;

                            case CLEAR_SCREEN: // CTRL-L
                                success = clearScreen();
                                break;

                            case OVERWRITE_MODE:
                                buf.setOverTyping(!buf.isOverTyping());
                                break;

                            case SELF_INSERT:
                                putString( sb );
                                success = true;
                                break;

                            case ACCEPT_LINE:
                                moveToEnd();
                                println(); // output newline
                                flush();
                                return finishBuffer();

                            case BACKWARD_WORD:
                                success = previousWord();
                                break;

                            case FORWARD_WORD:
                                success = nextWord();
                                break;

                            case PREVIOUS_HISTORY:
                                success = moveHistory(false);
                                break;

                            case NEXT_HISTORY:
                                success = moveHistory(true);
                                break;

                            case BACKWARD_DELETE_CHAR: // backspace
                                success = backspace();
                                break;

                            case DELETE_CHAR: // delete
                                success = deleteCurrentCharacter();
                                break;

                            case BACKWARD_CHAR:
                                success = moveCursor(-1) != 0;
                                break;

                            case FORWARD_CHAR:
                                success = moveCursor(1) != 0;
                                break;

                            case UNIX_LINE_DISCARD:
                                success = resetLine();
                                break;

                            case UNIX_WORD_RUBOUT:
                            case BACKWARD_KILL_WORD:
                                // in theory, those are slightly different
                                success = deletePreviousWord();
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

                            case REVERSE_SEARCH_HISTORY:
                                if (searchTerm != null) {
                                    previousSearchTerm = searchTerm.toString();
                                }
                                searchTerm = new StringBuffer(buf.buffer);
                                state = SEARCH;
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
                                success = true;
                                break;

                            case RE_READ_INIT_FILE:
                                loadKeys();
                                success = true;
                                break;

                            case START_KBD_MACRO:
                                recording = true;
                                break;

                            case END_KBD_MACRO:
                                recording = false;
                                macro = macro.substring(0, macro.length() - sb.length());
                                break;

                            case CALL_LAST_KBD_MACRO:
                                for (int i = 0; i < macro.length(); i++) {
                                    pushBackChar.add(macro.charAt(macro.length() - 1 - i));
                                }
                                sb.setLength( 0 );
                                break;

                            case VI_EDITING_MODE:
                                viEditMode = true;
                                keys = keyMaps.get("vi");
                                break;

                            case EMACS_EDITING_MODE:
                                viEditMode = false;
                                keys = keyMaps.get("emacs");
                                break;

                            default:
                                int i = 0;
                                break;
                        }
                    }
                }
                if (!success) {
                    beep();
                }
                sb.setLength( 0 );
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
    private String readLineSimple() throws IOException {
        StringBuilder buff = new StringBuilder();

        if (skipLF) {
            skipLF = false;
            
            int i = readCharacter();
            
            if (i == -1 || i == '\r') {
                return buff.toString();
            } else if (i == '\n') {
                // ignore
            } else {
                buff.append((char) i);
            }
        }        
        
        while (true) {
            int i = readCharacter();

            if (i == -1 || i == '\n') {
                return buff.toString();
            } else if (i == '\r') {
                skipLF = true;
                return buff.toString();
            } else {
                buff.append((char) i);
            }
        }
    }

    //
    // Completion
    //

    private final List<Completer> completers = new LinkedList<Completer>();

    private CompletionHandler completionHandler = new CandidateListCompletionHandler();

    /**
     * Add the specified {@link jline.console.completer.Completer} to the list of handlers for tab-completion.
     *
     * @param completer the {@link jline.console.completer.Completer} to add
     * @return true if it was successfully added
     */
    public boolean addCompleter(final Completer completer) {
        return completers.add(completer);
    }

    /**
     * Remove the specified {@link jline.console.completer.Completer} from the list of handlers for tab-completion.
     *
     * @param completer     The {@link Completer} to remove
     * @return              True if it was successfully removed
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
        assert handler != null;
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
    protected boolean complete() throws IOException {
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

        return candidates.size() != 0 && getCompletionHandler().complete(this, candidates, position);
    }

    protected void printCompletionCandidates() throws IOException {
        // debug ("tab for (" + buf + ")");
        if (completers.size() == 0) {
            return;
        }

        List<CharSequence> candidates = new LinkedList<CharSequence>();
        String bufstr = buf.buffer.toString();
        int cursor = buf.cursor;

        for (Completer comp : completers) {
            if (comp.complete(bufstr, cursor, candidates) != -1) {
                break;
            }
        }
        CandidateListCompletionHandler.printCandidates(this, candidates);
        drawLine();
    }

    /**
     * The number of tab-completion candidates above which a warning will be
     * prompted before showing all the candidates.
     */
    private int autoprintThreshold = Integer.getInteger(JLINE_COMPLETION_THRESHOLD, 100); // same default as bash

    /**
     * @param threshold the number of candidates to print without issuing a warning.
     */
    public void setAutoprintThreshold(final int threshold) {
        this.autoprintThreshold = threshold;
    }

    /**
     * @return the number of candidates to print without issuing a warning.
     */
    public int getAutoprintThreshold() {
        return autoprintThreshold;
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
//        flush();
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
        // TODO: Try to use jansi for this

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
        clearAhead(num, 0);

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
        printAnsiSequence("2J");

        // then send the ANSI code to go to position 1,1
        printAnsiSequence("1;1H");

        redrawLine();

        return true;
    }

    /**
     * Issue an audible keyboard bell.
     */
    public void beep() throws IOException {
        if (!Configuration.getBoolean(JLINE_NOBELL, true)) {
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
                    // ignore
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

    private final Map<Character, ActionListener> triggeredActions = new HashMap<Character, ActionListener>();

    /**
     * Adding a triggered Action allows to give another curse of action if a character passed the pre-processing.
     * <p/>
     * Say you want to close the application if the user enter q.
     * addTriggerAction('q', new ActionListener(){ System.exit(0); }); would do the trick.
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
        for (CharSequence item : items) {
            maxWidth = Math.max(maxWidth, item.length());
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
                    print(resources.getString("display-more"));
                    flush();
                    int c = readCharater();
                    if (c == '\r' || c == '\n') {
                        // one step forward
                        showLines = 1;
                    }
                    else if (c != 'q') {
                        // page forward
                        showLines = height - 1;
                    }

                    back(resources.getString("display-more").length());
                    if (c == 'q') {
                        // cancel
                        break;
                    }
                }
            }

            // NOTE: toString() is important here due to AnsiString being retarded
            buff.append(item.toString());
            for (int i = 0; i < (maxWidth + 3 - item.length()); i++) {
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

            maskThread = new Thread()
            {
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
        buf.cursor += this.prompt.length();
        this.prompt = "";
        backspaceAll();

        this.prompt = prompt;
        redrawLine();
        setBuffer(buffer);

        // move cursor to destination (-1 will move to end of line)
        if (cursorDest < 0) cursorDest = buffer.length();
        setCursorPosition(cursorDest);

        flush();
    }

    public void printSearchStatus(String searchTerm, String match) throws IOException {
        String prompt = "(reverse-i-search)`" + searchTerm + "': ";
        String buffer = match;
        int cursorDest = match.indexOf(searchTerm);
        resetPromptLine(prompt, buffer, cursorDest);
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

    private void printAnsiSequence(String sequence) throws IOException {
        print(27);
        print('[');
        print(sequence);
        flush(); // helps with step debugging
    }

    // return column position, reported by the terminal
    private int getCurrentPosition() {
        // check for ByteArrayInputStream to disable for unit tests
        if (terminal.isAnsiSupported() && !(in instanceof ByteArrayInputStream)) {
            try {
                printAnsiSequence("6n");
                flush();
                StringBuffer b = new StringBuffer(8);
                // position is sent as <ESC>[{ROW};{COLUMN}R
                int r;
                while((r = in.read()) > -1 && r != 'R') {
                    if (r != 27 && r != '[') {
                        b.append((char) r);
                    }
                }
                String[] pos = b.toString().split(";");
                return Integer.parseInt(pos[1]);
            } catch (Exception x) {
                // no luck
            }
        }

        return -1; // TODO: throw exception instead?
    }

    // return row position, reported by the terminal
    // needed to know whether to scroll up on cursor move in last col for weird
    // wrapping terminals - not tested for anything else
    private int getCurrentAnsiRow() {
        // check for ByteArrayInputStream to disable for unit tests
        if (terminal.isAnsiSupported() && !(in instanceof ByteArrayInputStream)) {
            try {
                printAnsiSequence("6n");
                flush();
                StringBuffer b = new StringBuffer(8);
                // position is sent as <ESC>[{ROW};{COLUMN}R
                int r;
                while((r = in.read()) > -1 && r != 'R') {
                    if (r != 27 && r != '[') {
                        b.append((char) r);
                    }
                }
                String[] pos = b.toString().split(";");
                return Integer.parseInt(pos[0]);
            } catch (Exception x) {
                // no luck
            }
        }

        return -1; // TODO: throw exception instead?
    }
}
