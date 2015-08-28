/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.console;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.fusesource.jansi.Pty;
import org.fusesource.jansi.Pty.Attributes;
import org.fusesource.jansi.Pty.Size;
import org.jline.utils.InputStreamReader;
import org.jline.utils.NonBlockingReader;

import static org.jline.utils.Preconditions.checkNotNull;

public class PosixPtyConsole extends AbstractPosixConsole {

    private final NonBlockingReader reader;
    private final PrintWriter writer;
    private final Thread inputPumpThread;
    private final Thread outputPumpThread;

    public PosixPtyConsole(String type, InputStream in, OutputStream out, String encoding, Attributes attributes, Size size) throws IOException {
        super(type, Pty.open(attributes, size));
        checkNotNull(in);
        checkNotNull(out);
        FileInputStream sin = new FileInputStream(getPty().getSlaveFD());
        FileOutputStream sout = new FileOutputStream(getPty().getSlaveFD());
        this.reader = new NonBlockingReader(new InputStreamReader(sin, encoding));
        this.writer = new PrintWriter(new OutputStreamWriter(sout, encoding));
        this.inputPumpThread = new PumpThread(in, new FileOutputStream(getPty().getMasterFD()));
        this.outputPumpThread = new PumpThread(new FileInputStream(getPty().getMasterFD()), out);
        parseInfoCmp();
        this.inputPumpThread.start();
        this.outputPumpThread.start();
    }

    public NonBlockingReader reader() {
        return reader;
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
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
