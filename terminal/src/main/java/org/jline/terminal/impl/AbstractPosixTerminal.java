/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.IOError;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.function.IntConsumer;

import org.jline.terminal.Attributes;
import org.jline.terminal.Cursor;
import org.jline.terminal.Size;
import org.jline.terminal.spi.Pty;

public abstract class AbstractPosixTerminal extends AbstractSystemTerminal {

    protected final Pty pty;
    protected final Attributes originalAttributes;

    public AbstractPosixTerminal(String name, String type, Pty pty) throws IOException {
        this(name, type, pty, null, SignalHandler.SIG_DFL, true);
    }

    public AbstractPosixTerminal(String name, String type, Pty pty, Charset encoding, SignalHandler signalHandler, boolean nativeSignals) throws IOException {
        super(name, type, encoding, signalHandler, nativeSignals);
        Objects.requireNonNull(pty);
        this.pty = pty;
        this.originalAttributes = this.pty.getAttr();
    }

    public Pty getPty() {
        return pty;
    }

    public Attributes getAttributes() {
        try {
            return pty.getAttr();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public void setAttributes(Attributes attr) {
        try {
            pty.setAttr(attr);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public Size getSize() {
        try {
            return pty.getSize();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public void setSize(Size size) {
        try {
            pty.setSize(size);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public void close() throws IOException {
        super.close();
        pty.setAttr(originalAttributes);
        pty.close();
    }

    @Override
    public Cursor getCursorPosition(IntConsumer discarded) {
        return CursorSupport.getCursorPosition(this, discarded);
    }

}
