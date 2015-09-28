/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader;

import java.io.IOError;
import java.io.IOException;
import java.util.Stack;

import org.jline.Console;
import org.jline.utils.NonBlockingReader;

public class BindingReader {

    protected final Console console;
    protected final StringBuilder opBuffer = new StringBuilder();
    protected final Stack<Integer> pushBackChar = new Stack<>();

    protected String lastBinding;
    protected boolean recording;
    protected StringBuilder macro = new StringBuilder();

    public BindingReader(Console console) {
        this.console = console;
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
        return readBinding(keys, local, true);
    }

    public Object readBinding(KeyMap keys, KeyMap local, boolean block) {
        lastBinding = null;
        Object o = null;
        int[] remaining = new int[1];
        do {
            int c = readCharacter();
            if (c == -1) {
                return null;
            }
            opBuffer.appendCodePoint(c);

            if (recording) {
                macro.appendCodePoint(c);
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
        } while (o == null && (block || peekCharacter(1l) != NonBlockingReader.READ_EXPIRED));
        if (o != null) {
            lastBinding = opBuffer.toString();
            opBuffer.setLength(0);
        }

        return o;
    }

    /**
     * Read a codepoint from the console.
     *
     * @return the character, or -1 if an EOF is received.
     */
    public int readCharacter() {
        if (!pushBackChar.isEmpty()) {
            return pushBackChar.pop();
        }
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
        if (!pushBackChar.isEmpty()) {
            return pushBackChar.peek();
        }
        try {
            return console.reader().peek(timeout);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public boolean startRecording() {
        if (recording) {
            return false;
        }
        recording = true;
        return true;
    }

    public String stopRecording() {
        if (!recording) {
            return null;
        }
        recording = false;
        macro.setLength(macro.length() - opBuffer.length());
        return macro.toString();
    }

    public void runMacro(String macro) {
        new StringBuilder(macro).reverse().codePoints().forEachOrdered(pushBackChar::push);
    }

    public String getCurrentBuffer() {
        return opBuffer.toString();
    }

    public String getLastBinding() {
        return lastBinding;
    }

}
