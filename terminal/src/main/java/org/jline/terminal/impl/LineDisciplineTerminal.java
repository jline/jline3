/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.terminal.impl;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Objects;

import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.ControlChar;
import org.jline.terminal.Attributes.InputFlag;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Attributes.OutputFlag;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.utils.InputStreamReader;
import org.jline.utils.NonBlockingReader;

/**
 * Abstract terminal with support for line discipline.
 * The {@link Terminal} interface represents the slave
 * side of a PTY, but implementations derived from this class
 * will handle both the slave and master side of things.
 *
 * In order to correctly handle line discipline, the terminal
 * needs to read the input in advance in order to raise the
 * signals as fast as possible.
 * For example, when the user hits Ctrl+C, we can't wait until
 * the application consumes all the read events.
 * The same applies to echoing, when enabled, as the echoing
 * has to happen as soon as the user hit the keyboard, and not
 * only when the application running in the terminal processes
 * the input.
 */
public class LineDisciplineTerminal extends AbstractTerminal {

    private static final String DEFAULT_TERMINAL_ATTRIBUTES =
                    "speed 9600 baud; 24 rows; 80 columns;\n" +
                    "lflags: icanon isig iexten echo echoe -echok echoke -echonl echoctl\n" +
                    "\t-echoprt -altwerase -noflsh -tostop -flusho pendin -nokerninfo\n" +
                    "\t-extproc\n" +
                    "iflags: -istrip icrnl -inlcr -igncr ixon -ixoff ixany imaxbel iutf8\n" +
                    "\t-ignbrk brkint -inpck -ignpar -parmrk\n" +
                    "oflags: opost onlcr -oxtabs -onocr -onlret\n" +
                    "cflags: cread cs8 -parenb -parodd hupcl -clocal -cstopb -crtscts -dsrflow\n" +
                    "\t-dtrflow -mdmbuf\n" +
                    "cchars: discard = ^O; dsusp = ^Y; eof = ^D; eol = <undef>;\n" +
                    "\teol2 = <undef>; erase = ^?; intr = ^C; kill = ^U; lnext = ^V;\n" +
                    "\tmin = 1; quit = ^\\; reprint = ^R; start = ^Q; status = ^T;\n" +
                    "\tstop = ^S; susp = ^Z; time = 0; werase = ^W;\n";

    private static final int PIPE_SIZE = 1024;

    /*
     * Master output stream
     */
    protected final OutputStream masterOutput;

    /*
     * Slave input pipe write side
     */
    protected final OutputStream slaveInputPipe;

    /*
     * Slave streams
     */
    protected final InputStream slaveInput;
    protected final NonBlockingReader slaveReader;
    protected final PrintWriter slaveWriter;
    protected final OutputStream slaveOutput;

    /**
     * Console data
     */
    protected final Attributes attributes;
    protected final Size size;

    public LineDisciplineTerminal(String name,
                                  String type,
                                  OutputStream masterOutput,
                                  Charset encoding) throws IOException {
        this(name, type, masterOutput, encoding, SignalHandler.SIG_DFL);
    }

    public LineDisciplineTerminal(String name,
                                  String type,
                                  OutputStream masterOutput,
                                  Charset encoding,
                                  SignalHandler signalHandler) throws IOException {
        super(name, type, encoding, signalHandler);
        PipedInputStream input = new PipedInputStream(PIPE_SIZE);
        this.slaveInputPipe = new PipedOutputStream(input);
        // This is a hack to fix a problem in gogo where closure closes
        // streams for commands if they are instances of PipedInputStream.
        // So we need to get around and make sure it's not an instance of
        // that class by using a dumb FilterInputStream class to wrap it.
        this.slaveInput = new FilterInputStream(input) {};
        this.slaveReader = new NonBlockingReader(getName(), new InputStreamReader(slaveInput, encoding()));
        this.slaveOutput = new FilteringOutputStream();
        this.slaveWriter = new PrintWriter(new OutputStreamWriter(slaveOutput, encoding()));
        this.masterOutput = masterOutput;
        this.attributes = ExecPty.doGetAttr(DEFAULT_TERMINAL_ATTRIBUTES);
        this.size = new Size(160, 50);
        parseInfoCmp();
    }

    public NonBlockingReader reader() {
        return slaveReader;
    }

    public PrintWriter writer() {
        return slaveWriter;
    }

    @Override
    public InputStream input() {
        return slaveInput;
    }

    @Override
    public OutputStream output() {
        return slaveOutput;
    }

    public Attributes getAttributes() {
        Attributes attr = new Attributes();
        attr.copy(attributes);
        return attr;
    }

    public void setAttributes(Attributes attr) {
        attributes.copy(attr);
    }

    public Size getSize() {
        Size sz = new Size();
        sz.copy(size);
        return sz;
    }

    public void setSize(Size sz) {
        size.copy(sz);
    }

   @Override
    public void raise(Signal signal) {
       Objects.requireNonNull(signal);
        // Do not call clear() atm as this can cause
        // deadlock between reading / writing threads
        // TODO: any way to fix that ?
        /*
        if (!attributes.getLocalFlag(LocalFlag.NOFLSH)) {
            try {
                slaveReader.clear();
            } catch (IOException e) {
                // Ignore
            }
        }
        */
        echoSignal(signal);
        super.raise(signal);
    }

    /**
     * Master input processing.
     * All data coming to the terminal should be provided
     * using this method.
     *
     * @param c the input byte
     * @throws IOException
     */
    public void processInputByte(int c) throws IOException {
        if (attributes.getLocalFlag(LocalFlag.ISIG)) {
            if (c == attributes.getControlChar(ControlChar.VINTR)) {
                raise(Signal.INT);
                return;
            } else if (c == attributes.getControlChar(ControlChar.VQUIT)) {
                raise(Signal.QUIT);
                return;
            } else if (c == attributes.getControlChar(ControlChar.VSUSP)) {
                raise(Signal.TSTP);
                return;
            } else if (c == attributes.getControlChar(ControlChar.VSTATUS)) {
                raise(Signal.INFO);
            }
        }
        if (c == '\r') {
            if (attributes.getInputFlag(InputFlag.IGNCR)) {
                return;
            }
            if (attributes.getInputFlag(InputFlag.ICRNL)) {
                c = '\n';
            }
        } else if (c == '\n' && attributes.getInputFlag(InputFlag.INLCR)) {
            c = '\r';
        }
        if (attributes.getLocalFlag(LocalFlag.ECHO)) {
            processOutputByte(c);
            masterOutput.flush();
        }
        slaveInputPipe.write(c);
        slaveInputPipe.flush();
    }

    /**
     * Master output processing.
     * All data going to the master should be provided by this method.
     *
     * @param c the output byte
     * @throws IOException
     */
    protected void processOutputByte(int c) throws IOException {
        if (attributes.getOutputFlag(OutputFlag.OPOST)) {
            if (c == '\n') {
                if (attributes.getOutputFlag(OutputFlag.ONLCR)) {
                    masterOutput.write('\r');
                    masterOutput.write('\n');
                    return;
                }
            }
        }
        masterOutput.write(c);
    }

    public void close() throws IOException {
        try {
            slaveReader.close();
        } finally {
            try {
                slaveInputPipe.close();
            } finally {
                try {
                } finally {
                    slaveWriter.close();
                }
            }
        }
    }

    private class FilteringOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
            processOutputByte(b);
            flush();
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            } else if ((off < 0) || (off > b.length) || (len < 0) ||
                    ((off + len) > b.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return;
            }
            for (int i = 0 ; i < len ; i++) {
                processOutputByte(b[off + i]);
            }
            flush();
        }

        @Override
        public void flush() throws IOException {
            masterOutput.flush();
        }

        @Override
        public void close() throws IOException {
            masterOutput.close();
        }
    }
}
