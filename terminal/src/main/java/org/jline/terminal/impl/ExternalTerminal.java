/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.terminal.impl;

import org.jline.terminal.Cursor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntConsumer;

/**
 * Console implementation with embedded line disciplined.
 *
 * This terminal is well-suited for supporting incoming external
 * connections, such as from the network (through telnet, ssh,
 * or any kind of protocol).
 * The terminal will start consuming the input in a separate thread
 * to generate interruption events.
 *
 * @see LineDisciplineTerminal
 */
public class ExternalTerminal extends LineDisciplineTerminal {

    protected final AtomicBoolean closed = new AtomicBoolean();
    protected final InputStream masterInput;
    protected final AtomicBoolean paused = new AtomicBoolean(true);
    protected Thread pumpThread;

    public ExternalTerminal(String name, String type,
                            InputStream masterInput,
                            OutputStream masterOutput,
                            Charset encoding) throws IOException {
        this(name, type, masterInput, masterOutput, encoding, SignalHandler.SIG_DFL);
    }

    public ExternalTerminal(String name, String type,
                            InputStream masterInput,
                            OutputStream masterOutput,
                            Charset encoding,
                            SignalHandler signalHandler) throws IOException {
        super(name, type, masterOutput, encoding, signalHandler);
        this.masterInput = masterInput;
        resume();
    }

    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            pause();
            super.close();
        }
    }

    @Override
    public boolean canPauseResume() {
        return true;
    }

    @Override
    public void pause() {
        if (paused.compareAndSet(false, true)) {
            this.pumpThread.interrupt();
        }
    }

    @Override
    public void resume() {
        if (paused.compareAndSet(true, false)) {
            this.pumpThread = new Thread(this::pump, toString() + " input pump thread");
            this.pumpThread.start();
        }
    }

    @Override
    public boolean paused() {
        return paused.get();
    }

    public void pump() {
        try {
            while (true) {
                int c = masterInput.read();
                if (c >= 0) {
                    processInputByte((char) c);
                }
                if (c < 0 || closed.get() || paused.get()) {
                    break;
                }
            }
        } catch (IOException e) {
            // Ignore
        }
        try {
            close();
        } catch (Throwable t) {
            // Ignore
        }
    }

    @Override
    public Cursor getCursorPosition(IntConsumer discarded) {
        return CursorSupport.getCursorPosition(this, discarded);
    }

}
