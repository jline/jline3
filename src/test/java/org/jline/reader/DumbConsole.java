package org.jline.reader;

/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.fusesource.jansi.Pty;
import org.fusesource.jansi.Pty.Attributes;
import org.fusesource.jansi.Pty.Size;
import org.jline.console.AbstractConsole;
import org.jline.utils.NonBlockingReader;

public class DumbConsole extends AbstractConsole {
    private final InputStream in;
    private final OutputStream out;
    private final NonBlockingReader reader;
    private final PrintWriter writer;
    private final Attributes attributes;
    private final Size size;

    public DumbConsole(InputStream in, OutputStream out) {
        super("ansi");
        this.in = in;
        this.out = out;
        this.reader = new NonBlockingReader(new InputStreamReader(in));
        this.writer = new PrintWriter(new OutputStreamWriter(out));
        this.attributes = new Attributes();
        this.attributes.setControlChar(Pty.VERASE,  (char) 127);
        this.attributes.setControlChar(Pty.VWERASE, (char) 23);
        this.attributes.setControlChar(Pty.VKILL,   (char) 21);
        this.attributes.setControlChar(Pty.VLNEXT,  (char) 22);
        this.size = new Size(160, 50);
        parseInfoCmp();
    }

    public NonBlockingReader reader() {
        return reader;
    }

    public PrintWriter writer() {
        return writer;
    }

    public Attributes getAttributes() throws IOException {
        Attributes attr = new Attributes();
        attr.copy(attributes);
        return attr;
    }

    public void setAttributes(Attributes attr) throws IOException {
        attributes.copy(attr);
    }

    public void setAttributes(Attributes attr, int actions) throws IOException {
        setAttributes(attr);
    }

    public Size getSize() throws IOException {
        Size sz = new Size();
        sz.copy(size);
        return sz;
    }

    public void setSize(Size sz) throws IOException {
        size.copy(sz);
    }

    public void close() throws IOException {
    }
}
