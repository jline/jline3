/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.terminal.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jline.terminal.spi.Pty;
import org.jline.utils.ClosedException;
import org.jline.utils.InputStreamReader;
import org.jline.utils.NonBlockingReader;

public class PosixPtyTerminal extends AbstractPosixTerminal {

    private final InputStreamWrapper input;
    private final OutputStream output;
    private final InputStreamReader innerReader;
    private final NonBlockingReader reader;
    private final PrintWriter writer;
    private final Thread inputPumpThread;
    private final Thread outputPumpThread;

    public PosixPtyTerminal(String name, String type, Pty pty, InputStream in, OutputStream out, Charset encoding) throws IOException {
        this(name, type, pty, in, out, encoding, SignalHandler.SIG_DFL);
    }

    public PosixPtyTerminal(String name, String type, Pty pty, InputStream in, OutputStream out, Charset encoding, SignalHandler signalHandler) throws IOException {
        super(name, type, pty, encoding, signalHandler);
        Objects.requireNonNull(in);
        Objects.requireNonNull(out);
        this.input = new InputStreamWrapper(pty.getSlaveInput());
        this.output = pty.getSlaveOutput();
        this.innerReader = new InputStreamReader(input, encoding());
        this.reader = new NonBlockingReader(name, innerReader);
        this.writer = new PrintWriter(new OutputStreamWriter(output, encoding()));
        this.inputPumpThread = new PumpThread(in, getPty().getMasterOutput());
        this.outputPumpThread = new PumpThread(getPty().getMasterInput(), out);
        parseInfoCmp();
        this.inputPumpThread.start();
        this.outputPumpThread.start();
    }

    public InputStream input() {
        return input;
    }

    public NonBlockingReader reader() {
        return reader;
    }

    public OutputStream output() {
        return output;
    }

    public PrintWriter writer() {
        return writer;
    }

    @Override
    public void close() throws IOException {
        super.close();
        reader.close();
    }

    private class InputStreamWrapper extends InputStream {

        private final InputStream in;
        private final AtomicBoolean closed = new AtomicBoolean();

        protected InputStreamWrapper(InputStream in) {
            this.in = in;
        }

        @Override
        public int read() throws IOException {
            if (closed.get()) {
                throw new ClosedException();
            }
            return in.read();
        }

        @Override
        public void close() throws IOException {
            closed.set(true);
        }
    }

    private class PumpThread extends Thread {
        private final InputStream in;
        private final OutputStream out;

        public PumpThread(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    int b = in.read();
                    if (b < 0) {
                        input.close();
                        break;
                    }
                    out.write(b);
                    out.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
