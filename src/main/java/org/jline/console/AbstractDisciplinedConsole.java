/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;

import org.jline.JLine.ConsoleReaderBuilder;
import org.jline.console.Attributes.ControlChar;
import org.jline.console.Attributes.InputFlag;
import org.jline.console.Attributes.LocalFlag;
import org.jline.console.Attributes.OutputFlag;
import org.jline.utils.InputStreamReader;
import org.jline.utils.NonBlockingReader;

import static org.jline.utils.Preconditions.checkNotNull;

public abstract class AbstractDisciplinedConsole extends AbstractConsole {

    protected final OutputStream output;

    protected final InputStream filterIn;
    protected final OutputStream filterInOut;
    protected final Writer filterInOutWriter;
    protected final NonBlockingReader reader;
    protected final PrintWriter writer;
    protected final Attributes attributes;
    protected final Size size;

    public AbstractDisciplinedConsole(String type, ConsoleReaderBuilder consoleReaderBuilder, String encoding) throws IOException {
        super(type, consoleReaderBuilder);
        PipedInputStream input = new PipedInputStream() {
            @Override
            public void close() throws IOException {
                super.close();
            }
        };
        // This is a hack to fix a problem in gogo where closure closes
        // streams for commands if they are PipedInputStreams.
        // So we need to get around and make sure it's not an instance of
        // that class.
        this.filterIn = new InputStream() {
            @Override
            public int read() throws IOException {
                return input.read();
            }
            @Override
            public void close() throws IOException {
                input.close();
            }
            @Override
            public int available() throws IOException {
                return input.available();
            }
        };
        this.filterInOut = new PipedOutputStream(input);
        this.filterInOutWriter = new OutputStreamWriter(filterInOut, encoding);
        this.reader = new NonBlockingReader(new InputStreamReader(filterIn, encoding));
        this.output = new FilteringOutputStream();
        this.writer = new PrintWriter(new OutputStreamWriter(output, encoding));
        this.attributes = new Attributes();
        this.size = new Size(160, 50);
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
        return filterIn;
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

   @Override
    public void raise(Signal signal) {
        checkNotNull(signal);
        if (!attributes.getLocalFlag(LocalFlag.NOFLSH)) {
            try {
                reader.clear();
            } catch (IOException e) {
                // Ignore
            }
        }
        echoSignal(signal);
        super.raise(signal);
    }

    protected void processInputByte(int c) throws IOException {
        if (attributes.getLocalFlag(LocalFlag.ISIG)) {
            if (c == attributes.getControlChar(ControlChar.VINTR)) {
                raise(Signal.INT);
                return;
            } else if (c == attributes.getControlChar(ControlChar.VQUIT)) {
                raise(Signal.QUIT);
                return;
            } else if (c == attributes.getControlChar(ControlChar.VSUSP)) {
                raise(Signal.TSTP);
                return;
            } else if (c == attributes.getControlChar(ControlChar.VSTATUS)) {
                raise(Signal.INFO);
            }
        }
        if (c == '\r') {
            if (attributes.getInputFlag(InputFlag.IGNCR)) {
                return;
            }
            if (attributes.getInputFlag(InputFlag.ICRNL)) {
                c = '\n';
            }
        } else if (c == '\n' && attributes.getInputFlag(InputFlag.INLCR)) {
            c = '\r';
        }
        if (attributes.getLocalFlag(LocalFlag.ECHO)) {
            processOutputByte(c);
            doFlush();
        }
        filterInOut.write(c);
        filterInOut.flush();
    }

    protected void processOutputByte(int c) throws IOException {
        if (attributes.getOutputFlag(OutputFlag.OPOST)) {
            if (c == '\n') {
                if (attributes.getOutputFlag(OutputFlag.ONLCR)) {
                    doWriteByte('\r');
                    doWriteByte('\n');
                    return;
                }
            }
        }
        doWriteByte(c);
    }

    protected abstract void doWriteByte(int c) throws IOException;

    protected abstract void doFlush() throws IOException;

    protected abstract void doClose() throws IOException;

    public void close() throws IOException {
        filterIn.close();
        filterInOut.close();
        reader.close();
        writer.close();
        output.close();
    }

    private class FilteringOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
            processOutputByte(b);
        }

        @Override
        public void flush() throws IOException {
            doFlush();
        }

        @Override
        public void close() throws IOException {
            doClose();
        }
    }
}
