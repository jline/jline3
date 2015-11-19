/*
 * Copyright (c) 2002-2015, the original author or authors.
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
import java.util.Objects;

import org.jline.utils.InputStreamReader;
import org.jline.utils.NonBlockingReader;

public class PosixPtyTerminal extends AbstractPosixTerminal {

    protected final InputStream input;
    protected final OutputStream output;
    private final NonBlockingReader reader;
    private final PrintWriter writer;
    private final Thread inputPumpThread;
    private final Thread outputPumpThread;

    public PosixPtyTerminal(String name, String type, Pty pty, InputStream in, OutputStream out, String encoding) throws IOException {
        super(name, type, pty);
        Objects.requireNonNull(in);
        Objects.requireNonNull(out);
        this.input = pty.getSlaveInput();
        this.output = pty.getSlaveOutput();
        this.reader = new NonBlockingReader(name, new InputStreamReader(input, encoding));
        this.writer = new PrintWriter(new OutputStreamWriter(output, encoding));
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
