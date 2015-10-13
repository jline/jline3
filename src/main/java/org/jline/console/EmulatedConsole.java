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

import org.jline.JLine.ConsoleReaderBuilder;

public class EmulatedConsole extends AbstractDisciplinedConsole {

    private final Thread pumpThread;
    protected final InputStream inStream;
    protected final OutputStream outStream;

    public EmulatedConsole(String type, ConsoleReaderBuilder consoleReaderBuilder,
                           InputStream input, OutputStream output,
                           String encoding) throws IOException {
        super(type, consoleReaderBuilder, encoding);
        this.inStream = input;
        this.outStream = output;
        this.pumpThread = new PumpThread();
        this.pumpThread.start();
    }

    protected void doWriteByte(int c) throws IOException {
        outStream.write(c);
    }

    protected void doFlush() throws IOException {
        outStream.flush();
    }

    protected void doClose() throws IOException {
        outStream.close();
    }

    public void close() throws IOException {
        pumpThread.interrupt();
        super.close();
    }

    private class PumpThread extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    int c = inStream.read();
                    if (c < 0) {
                        break;
                    }
                    processInputByte((char) c);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
