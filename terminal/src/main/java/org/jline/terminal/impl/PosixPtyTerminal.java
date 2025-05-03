/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Objects;

import org.jline.terminal.spi.Pty;
import org.jline.utils.ClosedException;
import org.jline.utils.NonBlocking;
import org.jline.utils.NonBlockingInputStream;
import org.jline.utils.NonBlockingReader;

/**
 * Terminal implementation for POSIX systems using a pseudoterminal (PTY).
 *
 * <p>
 * The PosixPtyTerminal class provides a terminal implementation for POSIX systems
 * (Linux, macOS, etc.) that uses a pseudoterminal (PTY) for terminal operations.
 * It extends the AbstractPosixTerminal class and adds functionality specific to
 * PTY-based terminals.
 * </p>
 *
 * <p>
 * This implementation is used when a full terminal emulation is needed, such as
 * when creating a terminal for an external process or when connecting to a remote
 * terminal. It provides access to the master and slave sides of the PTY, allowing
 * for bidirectional communication with the terminal.
 * </p>
 *
 * <p>
 * Key features of this implementation include:
 * </p>
 * <ul>
 *   <li>Full terminal emulation using a pseudoterminal</li>
 *   <li>Support for terminal attributes and size changes</li>
 *   <li>Access to both master and slave sides of the PTY</li>
 *   <li>Support for non-blocking I/O</li>
 * </ul>
 *
 * @see org.jline.terminal.impl.AbstractPosixTerminal
 * @see org.jline.terminal.spi.Pty
 */
public class PosixPtyTerminal extends AbstractPosixTerminal {

    private final InputStream in;
    private final OutputStream out;
    private final InputStream masterInput;
    private final OutputStream masterOutput;
    private final NonBlockingInputStream input;
    private final OutputStream output;
    private final NonBlockingReader reader;
    private final PrintWriter writer;

    private final Object lock = new Object();
    private Thread inputPumpThread;
    private Thread outputPumpThread;
    private boolean paused = true;

    public PosixPtyTerminal(String name, String type, Pty pty, InputStream in, OutputStream out, Charset encoding)
            throws IOException {
        this(name, type, pty, in, out, encoding, SignalHandler.SIG_DFL);
    }

    public PosixPtyTerminal(
            String name,
            String type,
            Pty pty,
            InputStream in,
            OutputStream out,
            Charset encoding,
            SignalHandler signalHandler)
            throws IOException {
        this(name, type, pty, in, out, encoding, signalHandler, false);
    }

    @SuppressWarnings("this-escape")
    public PosixPtyTerminal(
            String name,
            String type,
            Pty pty,
            InputStream in,
            OutputStream out,
            Charset encoding,
            SignalHandler signalHandler,
            boolean paused)
            throws IOException {
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
        if (!paused) {
            resume();
        }
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
    protected void doClose() throws IOException {
        super.doClose();
        reader.close();
    }

    @Override
    public boolean canPauseResume() {
        return true;
    }

    @Override
    public void pause() {
        try {
            pause(false);
        } catch (InterruptedException e) {
            // nah
        }
    }

    @Override
    public void pause(boolean wait) throws InterruptedException {
        Thread p1, p2;
        synchronized (lock) {
            paused = true;
            p1 = inputPumpThread;
            p2 = outputPumpThread;
        }
        if (p1 != null) {
            p1.interrupt();
        }
        if (p2 != null) {
            p2.interrupt();
        }
        if (wait) {
            if (p1 != null) {
                p1.join();
            }
            if (p2 != null) {
                p2.join();
            }
        }
    }

    @Override
    public void resume() {
        synchronized (lock) {
            paused = false;
            if (inputPumpThread == null) {
                inputPumpThread = new Thread(this::pumpIn, toString() + " input pump thread");
                inputPumpThread.setDaemon(true);
                inputPumpThread.start();
            }
            if (outputPumpThread == null) {
                outputPumpThread = new Thread(this::pumpOut, toString() + " output pump thread");
                outputPumpThread.setDaemon(true);
                outputPumpThread.start();
            }
        }
    }

    @Override
    public boolean paused() {
        synchronized (lock) {
            return paused;
        }
    }

    private static class InputStreamWrapper extends NonBlockingInputStream {

        private final NonBlockingInputStream in;
        private volatile boolean closed;

        protected InputStreamWrapper(NonBlockingInputStream in) {
            this.in = in;
        }

        @Override
        public int read(long timeout, boolean isPeek) throws IOException {
            if (closed) {
                throw new ClosedException();
            }
            return in.read(timeout, isPeek);
        }

        @Override
        public void close() throws IOException {
            closed = true;
        }
    }

    private void pumpIn() {
        try {
            for (; ; ) {
                synchronized (lock) {
                    if (paused) {
                        inputPumpThread = null;
                        return;
                    }
                }
                int b = in.read();
                if (b < 0) {
                    input.close();
                    break;
                }
                masterOutput.write(b);
                masterOutput.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            synchronized (lock) {
                inputPumpThread = null;
            }
        }
    }

    private void pumpOut() {
        try {
            for (; ; ) {
                synchronized (lock) {
                    if (paused) {
                        outputPumpThread = null;
                        return;
                    }
                }
                int b = masterInput.read();
                if (b < 0) {
                    input.close();
                    break;
                }
                out.write(b);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            synchronized (lock) {
                outputPumpThread = null;
            }
        }
        try {
            close();
        } catch (Throwable t) {
            // Ignore
        }
    }
}
