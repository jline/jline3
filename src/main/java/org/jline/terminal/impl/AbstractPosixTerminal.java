package org.jline.terminal.impl;

import java.io.IOError;
import java.io.IOException;
import java.util.Objects;

import org.jline.terminal.Attributes;
import org.jline.terminal.Size;

public abstract class AbstractPosixTerminal extends AbstractTerminal {

    protected final Pty pty;
    protected final Attributes originalAttributes;

    public AbstractPosixTerminal(String name, String type, Pty pty) throws IOException {
        super(name, type);
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
        pty.setAttr(originalAttributes);
        pty.close();
    }
}
