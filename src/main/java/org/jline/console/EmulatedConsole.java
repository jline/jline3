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
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

import org.jline.JLine.ConsoleReaderBuilder;
import org.jline.utils.InputStreamReader;

public class EmulatedConsole extends AbstractDisciplinedConsole {

    private final Thread pumpThread;
    protected final Reader inReader;
    protected final Writer outWriter;

    public EmulatedConsole(String type, ConsoleReaderBuilder consoleReaderBuilder,
                           InputStream input, OutputStream output,
                           String encoding) throws IOException {
        super(type, consoleReaderBuilder);
        this.inReader = new InputStreamReader(input, encoding != null ? encoding : Charset.defaultCharset().name());
        this.outWriter = new OutputStreamWriter(output, encoding != null ? encoding : Charset.defaultCharset().name());
        this.pumpThread = new PumpThread();
        this.pumpThread.start();
    }

    protected void doWriteChar(int c) throws IOException {
        outWriter.write(c);
    }

    protected void doFlush() throws IOException {
        outWriter.flush();
    }

    protected void doClose() throws IOException {
        outWriter.close();
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

}
