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

public class EmulatedConsole extends AbstractConsole {

    private final InputStream input;
    private final OutputStream output;
    private final PipedReader filterIn;
    private final PipedWriter filterInOut;
    private final NonBlockingReader reader;
    private final PrintWriter writer;
    private final Reader inReader;
    private final Writer outWriter;
    private final Attributes attributes;
    private final Size size;
    private final Thread pumpThread;

    public EmulatedConsole(String type, ConsoleReaderBuilder consoleReaderBuilder, InputStream in, OutputStream out, final String encoding) throws IOException {
        super(type, consoleReaderBuilder);
        checkNotNull(in);
        checkNotNull(out);
        this.input = in;
        this.output = out;
        this.inReader = new InputStreamReader(input, encoding != null ? encoding : Charset.defaultCharset().name());
        this.outWriter = new OutputStreamWriter(output, encoding != null ? encoding : Charset.defaultCharset().name());
        this.filterIn = new PipedReader();
        this.filterInOut = new PipedWriter(filterIn);
        this.pumpThread = new PumpThread();
        this.reader = new NonBlockingReader(filterIn);
        this.writer = new PrintWriter(new FilteringWriter());
        this.attributes = new Attributes();
        this.size = new Size(160, 50);
        parseInfoCmp();
        this.pumpThread.start();
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
        pumpThread.interrupt();
        filterIn.close();
        filterInOut.close();
        reader.close();
        writer.close();
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

    private void processInputChar(int c) throws IOException {
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
        }
        filterInOut.write(c);
        filterInOut.flush();
    }

    private void processOutputChar(int c) throws IOException {
        if (attributes.getOutputFlag(OutputFlag.OPOST)) {
            if (c == '\n') {
                if (attributes.getOutputFlag(OutputFlag.ONLCR)) {
                    outWriter.write('\r');
                    outWriter.write('\n');
                    outWriter.flush();
                    return;
                }
            }
        }
        outWriter.write(c);
        outWriter.flush();
    }

    private class PumpThread extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    int c = inReader.read();
                    if (c < 0) {
                        break;
                    }
                    processInputChar((char) c);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
            outWriter.flush();
        }

        @Override
        public void close() throws IOException {
            outWriter.close();
        }
    }
}
