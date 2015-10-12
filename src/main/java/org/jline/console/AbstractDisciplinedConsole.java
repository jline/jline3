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
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

import org.jline.JLine.ConsoleReaderBuilder;
import org.jline.console.Attributes.ControlChar;
import org.jline.console.Attributes.InputFlag;
import org.jline.console.Attributes.LocalFlag;
import org.jline.console.Attributes.OutputFlag;
import org.jline.utils.InputStreamReader;
import org.jline.utils.NonBlockingReader;

import static org.jline.utils.Preconditions.checkNotNull;

public abstract class AbstractDisciplinedConsole extends AbstractConsole {

    protected final PipedReader filterIn;
    protected final PipedWriter filterInOut;
    protected final NonBlockingReader reader;
    protected final PrintWriter writer;
    protected final Attributes attributes;
    protected final Size size;

    public AbstractDisciplinedConsole(String type, ConsoleReaderBuilder consoleReaderBuilder) throws IOException {
        super(type, consoleReaderBuilder);
        this.filterIn = new PipedReader();
        this.filterInOut = new PipedWriter(filterIn);
        this.reader = new NonBlockingReader(filterIn);
        this.writer = new PrintWriter(new FilteringWriter());
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
        return new InputStream() {
            @Override
            public int read() throws IOException {
                return reader.read();
            }
            public int read(byte b[], int off, int len) throws IOException {
                int c = read();
                if (c >= 0) {
                    b[off] = (byte) c;
                    return 1;
                }
                return -1;
            }
        };
    }

    @Override
    public OutputStream output() {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                writer.write(b);
            }
        };
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

    protected void processInputChar(int c) throws IOException {
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
            processOutputChar(c);
            doFlush();
        }
        filterInOut.write(c);
        filterInOut.flush();
    }

    protected void processOutputChar(int c) throws IOException {
        if (attributes.getOutputFlag(OutputFlag.OPOST)) {
            if (c == '\n') {
                if (attributes.getOutputFlag(OutputFlag.ONLCR)) {
                    doWriteChar('\r');
                    doWriteChar('\n');
                    return;
                }
            }
        }
        doWriteChar(c);
    }

    protected abstract void doWriteChar(int c) throws IOException;

    protected abstract void doFlush() throws IOException;

    protected abstract void doClose() throws IOException;

    public void close() throws IOException {
        filterIn.close();
        filterInOut.close();
        reader.close();
        writer.close();
    }

    private class FilteringWriter extends Writer {
        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            for (int i = 0; i < len; i++) {
                processOutputChar(cbuf[off + i]);
            }
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
