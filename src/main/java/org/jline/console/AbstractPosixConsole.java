package org.jline.console;

import java.io.IOException;

import org.fusesource.jansi.Pty;
import org.fusesource.jansi.Pty.Attributes;
import org.fusesource.jansi.Pty.Size;

import static org.jline.utils.Preconditions.checkNotNull;

public abstract class AbstractPosixConsole extends AbstractConsole {

    private final Pty pty;
    private final Attributes originalAttributes;

    public AbstractPosixConsole(String type, Pty pty) throws IOException {
        super(type);
        checkNotNull(pty);
        this.pty = pty;
        this.originalAttributes = this.pty.getAttr();
    }

    protected Pty getPty() {
        return pty;
    }

    public String getPtyName() {
        return pty.getName();
    }

    public Attributes getAttributes() throws IOException {
        return pty.getAttr();
    }

    public void setAttributes(Attributes attr) throws IOException {
        pty.setAttr(attr);
    }

    public void setAttributes(Attributes attr, int actions) throws IOException {
        pty.setAttr(attr, actions);
    }

    public Size getSize() throws IOException {
        return pty.getSize();
    }

    public void setSize(Size size) throws IOException {
        pty.setSize(size);
    }

    public void close() throws IOException {
        pty.setAttr(originalAttributes);
    }
}
