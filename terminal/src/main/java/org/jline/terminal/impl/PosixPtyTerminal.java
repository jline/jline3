/*
 * Copyright (c) 2002-2018, the original author or authors.
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
import org.jline.utils.NonBlocking;
import org.jline.utils.NonBlockingInputStream;
import org.jline.utils.NonBlockingReader;

public class PosixPtyTerminal extends AbstractPosixTerminal {

    private final InputStream in;
    private final OutputStream out;
    private final InputStream masterInput;
    private final OutputStream masterOutput;
    private final NonBlockingInputStream input;
    private final OutputStream output;
    private final NonBlockingReader reader;
    private final PrintWriter writer;
    private Thread inputPumpThread;
    private Thread outputPumpThread;
    private AtomicBoolean paused = new AtomicBoolean(true);

    public PosixPtyTerminal(String name, String type, Pty pty, InputStream in, OutputStream out, Charset encoding) throws IOException {
        this(name, type, pty, in, out, encoding, SignalHandler.SIG_DFL);
    }

    public PosixPtyTerminal(String name, String type, Pty pty, InputStream in, OutputStream out, Charset encoding, SignalHandler signalHandler) throws IOException {
        super(name, type, pty, encoding, signalHandler);
        this.in = Objects.requireNonNull(in);
        this.out = Objects.requireNonNull(out);
        this.masterInput = pty.getMasterInput();
        this.masterOutput = pty.getMasterOutput();
        this.input = new InputStreamWrapper(NonBlocking.nonBlocking(name, pty.getSlaveInput()));
        this.output = pty.getSlaveOutput();
        this.reader = NonBlocking.nonBlocking(name, input, encoding());
        this.writer = new PrintWriter(new OutputStreamWriter(output, encoding()));
        parseInfoCmp();
        resume();
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

    @Override
    public boolean canPauseResume() {
        return true;
    }

    @Override
    public void pause() {
        paused.compareAndSet(false, true);
    }

    @Override
    public void resume() {
        if (paused.compareAndSet(true, false)) {
            this.inputPumpThread = new PumpThread(in, masterOutput);
            this.outputPumpThread = new PumpThread(masterInput, out);
            this.inputPumpThread.start();
            this.outputPumpThread.start();
        }
    }

    @Override
    public boolean paused() {
        return paused.get();
    }

    private class InputStreamWrapper extends NonBlockingInputStream {

        private final NonBlockingInputStream in;
        private final AtomicBoolean closed = new AtomicBoolean();

        protected InputStreamWrapper(NonBlockingInputStream in) {
            this.in = in;
        }

        @Override
        public int read(long timeout, boolean isPeek) throws IOException {
            if (closed.get()) {
                throw new ClosedException();
            }
            return in.read(timeout, isPeek);
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
                while (!paused.get()) {
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
