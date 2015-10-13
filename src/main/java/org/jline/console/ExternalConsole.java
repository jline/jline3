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
import java.util.concurrent.atomic.AtomicBoolean;

import org.jline.JLine.ConsoleReaderBuilder;

/**
 * Console implementation with embedded line disciplined.
 *
 * This console is well-suited for supporting incoming external
 * connections, such as from the network (through telnet, ssh,
 * or any kind of protocol).
 * The console will start consuming the input in a separate thread
 * to generate interruption events.
 *
 * @see LineDisciplineConsole
 */
public class ExternalConsole extends LineDisciplineConsole {

    private final AtomicBoolean closed = new AtomicBoolean();
    private final Thread pumpThread;
    protected final InputStream masterInput;

    public ExternalConsole(String name, String type, ConsoleReaderBuilder consoleReaderBuilder,
                           InputStream masterInput, OutputStream masterOutput,
                           String encoding) throws IOException {
        super(name, type, consoleReaderBuilder, masterOutput, encoding);
        this.masterInput = masterInput;
        this.pumpThread = new Thread(this::pump, toString() + " input pump thread");
        this.pumpThread.start();
    }

    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            pumpThread.interrupt();
            super.close();
        }
    }

    public void pump() {
        try {
            while (true) {
                int c = masterInput.read();
                if (c < 0 || closed.get()) {
                    break;
                }
                processInputByte((char) c);
            }
        } catch (IOException e) {
            try {
                close();
            } catch (Throwable t) {
                e.addSuppressed(t);
            }
            // TODO: log
            e.printStackTrace();
        }
    }

}
