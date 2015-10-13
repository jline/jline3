package org.jline.console;

import java.io.IOError;
import java.io.IOException;

import org.jline.JLine.ConsoleReaderBuilder;

import static org.jline.utils.Preconditions.checkNotNull;

public abstract class AbstractPosixConsole extends AbstractConsole {

    protected final Pty pty;
    protected final Attributes originalAttributes;

    public AbstractPosixConsole(String name, String type, ConsoleReaderBuilder consoleReaderBuilder, Pty pty) throws IOException {
        super(name, type, consoleReaderBuilder);
        checkNotNull(pty);
        this.pty = pty;
        this.originalAttributes = this.pty.getAttr();
    }

    protected Pty getPty() {
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
