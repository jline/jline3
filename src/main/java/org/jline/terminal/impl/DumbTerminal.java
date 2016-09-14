package org.jline.terminal.impl;

/*
 * Copyright (c) 2002-2016, the original author or authors.
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
import java.nio.charset.Charset;

import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.ControlChar;
import org.jline.terminal.Size;
import org.jline.terminal.impl.AbstractTerminal;
import org.jline.utils.NonBlockingReader;

public class DumbTerminal extends AbstractTerminal {

    private final InputStream input;
    private final OutputStream output;
    private final NonBlockingReader reader;
    private final PrintWriter writer;
    private final Attributes attributes;
    private final Size size;

    public DumbTerminal(InputStream in, OutputStream out) throws IOException {
        this("dumb", "ansi", in, out, Charset.defaultCharset().name());
    }

    public DumbTerminal(String name, String type, InputStream in, OutputStream out, String encoding) throws IOException {
        this(name, type, in, out, encoding, SignalHandler.SIG_DFL);
    }

    public DumbTerminal(String name, String type, InputStream in, OutputStream out, String encoding, SignalHandler signalHandler) throws IOException {
        super(name, type, signalHandler);
        this.input = in;
        this.output = out;
        this.reader = new NonBlockingReader(getName(), new InputStreamReader(in, encoding));
        this.writer = new PrintWriter(new OutputStreamWriter(out, encoding));
        this.attributes = new Attributes();
        this.attributes.setControlChar(ControlChar.VERASE,  (char) 127);
        this.attributes.setControlChar(ControlChar.VWERASE, (char) 23);
        this.attributes.setControlChar(ControlChar.VKILL,   (char) 21);
        this.attributes.setControlChar(ControlChar.VLNEXT,  (char) 22);
        this.size = new Size();
        parseInfoCmp();
    }

    public NonBlockingReader reader() {
        return reader;
    }

    public PrintWriter writer() {
        return writer;
    }

    @Override
    public InputStream input() {
        return input;
    }

    @Override
    public OutputStream output() {
        return output;
    }

    public Attributes getAttributes() {
        Attributes attr = new Attributes();
        attr.copy(attributes);
        return attr;
    }

    public void setAttributes(Attributes attr) {
        attributes.copy(attr);
    }

    public Size getSize() {
        Size sz = new Size();
        sz.copy(size);
        return sz;
    }

    public void setSize(Size sz) {
        size.copy(sz);
    }

    public void close() throws IOException {
    }
}
