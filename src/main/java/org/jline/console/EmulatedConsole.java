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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

import org.fusesource.jansi.Pty;
import org.fusesource.jansi.Pty.Attributes;
import org.fusesource.jansi.Pty.Size;
import org.jline.utils.NonBlockingReader;

import static org.jline.utils.Preconditions.checkNotNull;

public class EmulatedConsole extends AbstractConsole {

    private final PipedInputStream filterIn;
    private final PipedOutputStream filterInOut;
    private final OutputStream filterOut;
    private final Charset charset;
    private final NonBlockingReader reader;
    private final PrintWriter writer;
    private final Reader inReader;
    private final Writer outWriter;
    private final Writer filterInOutWriter;
    private final Attributes attributes;
    private final Size size;
    private final Thread pumpThread;

    public EmulatedConsole(String type, InputStream in, OutputStream out, final String encoding) throws IOException {
        super(type);
        checkNotNull(in);
        checkNotNull(out);
        this.charset = encoding != null ? Charset.forName(encoding) : Charset.defaultCharset();
        this.filterIn = new PipedInputStream();
        this.filterInOut = new PipedOutputStream(filterIn);
        this.filterOut = new FilteringOutputStream();
        this.pumpThread = new PumpThread();
        this.reader = new NonBlockingReader(new InputStreamReader(filterIn, charset));
        this.inReader = new InputStreamReader(in, charset);
        this.outWriter = new OutputStreamWriter(out, charset);
        this.writer = new PrintWriter(new FilteringWriter());
        this.filterInOutWriter = new OutputStreamWriter(filterInOut, charset);
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
        pumpThread.interrupt();
        filterIn.close();
        filterInOut.close();
        filterOut.close();
        reader.close();
        writer.close();
    }

    @Override
    public void raise(Signal signal) {
        checkNotNull(signal);
        if (!attributes.getLocalFlag(Pty.NOFLSH)) {
            try {
                while (reader.ready()) {
                    reader.read();
                }
            } catch (IOException e) {
                // Ignore
            }
        }
        echoSignal(signal);
        super.raise(signal);
    }

    private void processInputChar(int c) throws IOException {
        if (attributes.getInputFlag(Pty.ISIG)) {
            if (c == attributes.getControlChar(Pty.VINTR)) {
                raise(Signal.INT);
                return;
            } else if (c == attributes.getControlChar(Pty.VQUIT)) {
                raise(Signal.QUIT);
                return;
            } else if (c == attributes.getControlChar(Pty.VSUSP)) {
                raise(Signal.TSTP);
                return;
            } else if (c == attributes.getControlChar(Pty.VSTATUS)) {
                raise(Signal.INFO);
            }
        }
        if (c == '\r') {
            if (attributes.getInputFlag(Pty.IGNCR)) {
                return;
            }
            if (attributes.getInputFlag(Pty.ICRNL)) {
                c = '\n';
            }
        } else if (c == '\n' && attributes.getInputFlag(Pty.INLCR)) {
            c = '\r';
        }
        if (attributes.getLocalFlag(Pty.ECHO)) {
            processOutputChar(c);
        }
        filterInOutWriter.write(c);
        filterInOutWriter.flush();
    }

    private void processOutputChar(int c) throws IOException {
        if (attributes.getOutputFlag(Pty.OPOST)) {
            if (c == '\n') {
                if (attributes.getOutputFlag(Pty.ONLCR)) {
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

    private class FilteringOutputStream extends OutputStream {

        private ByteBuffer outBuffer = ByteBuffer.allocate(4);

        private CharsetDecoder decoder = charset.newDecoder().onMalformedInput(
                CodingErrorAction.REPLACE).onUnmappableCharacter(
                CodingErrorAction.REPLACE);

        @Override
        public void write(int b) throws IOException {
            CharBuffer inChars = CharBuffer.allocate(1);
            outBuffer.put((byte) b);
            decoder.decode(outBuffer, inChars, false);
            while (inChars.hasRemaining()) {
                int c = inChars.get();
                processOutputChar(c);
            }
        }
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
