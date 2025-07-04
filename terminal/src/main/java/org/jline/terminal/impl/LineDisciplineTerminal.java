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
import java.util.EnumSet;
import java.util.Objects;

import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.ControlChar;
import org.jline.terminal.Attributes.InputFlag;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Attributes.OutputFlag;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.spi.SystemStream;
import org.jline.terminal.spi.TerminalProvider;
import org.jline.utils.NonBlocking;
import org.jline.utils.NonBlockingPumpInputStream;
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
 *
 * <p>
 * The LineDisciplineTerminal class provides a terminal implementation that emulates
 * the line discipline functionality typically provided by the operating system's
 * terminal driver. Line discipline refers to the processing of input and output
 * characters according to various modes and settings, such as canonical mode,
 * echo, and special character handling.
 * </p>
 *
 * <p>
 * This terminal implementation is particularly useful in environments where:
 * </p>
 * <ul>
 *   <li>The underlying system does not provide native terminal capabilities</li>
 *   <li>The application needs precise control over terminal behavior</li>
 *   <li>The terminal is being used in a non-standard environment (e.g., embedded systems)</li>
 * </ul>
 *
 * <p>
 * Key features of this implementation include:
 * </p>
 * <ul>
 *   <li>Emulation of canonical and non-canonical input modes</li>
 *   <li>Support for character echoing</li>
 *   <li>Special character handling (e.g., interrupt, erase, kill)</li>
 *   <li>Input and output processing according to terminal attributes</li>
 * </ul>
 *
 * <p>
 * This terminal implementation works with any input and output streams, making it
 * highly flexible and adaptable to various environments.
 * </p>
 *
 * @see org.jline.terminal.Attributes
 * @see org.jline.terminal.impl.AbstractTerminal
 */
public class LineDisciplineTerminal extends AbstractTerminal {

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
    protected final NonBlockingPumpInputStream slaveInput;
    protected final NonBlockingReader slaveReader;
    protected final PrintWriter slaveWriter;
    protected final OutputStream slaveOutput;

    /**
     * Console data
     */
    protected final Attributes attributes;

    protected final Size size;

    protected boolean skipNextLf;

    public LineDisciplineTerminal(String name, String type, OutputStream masterOutput, Charset encoding)
            throws IOException {
        this(name, type, masterOutput, encoding, SignalHandler.SIG_DFL);
    }

    @SuppressWarnings("this-escape")
    public LineDisciplineTerminal(
            String name, String type, OutputStream masterOutput, Charset encoding, SignalHandler signalHandler)
            throws IOException {
        this(name, type, masterOutput, encoding, encoding, encoding, signalHandler);
    }

    @SuppressWarnings("this-escape")
    public LineDisciplineTerminal(
            String name,
            String type,
            OutputStream masterOutput,
            Charset encoding,
            Charset inputEncoding,
            Charset outputEncoding,
            SignalHandler signalHandler)
            throws IOException {
        super(name, type, encoding, inputEncoding, outputEncoding, signalHandler);
        NonBlockingPumpInputStream input = NonBlocking.nonBlockingPumpInputStream(PIPE_SIZE);
        this.slaveInputPipe = input.getOutputStream();
        this.slaveInput = input;
        this.slaveReader = NonBlocking.nonBlocking(getName(), slaveInput, inputEncoding());
        this.slaveOutput = new FilteringOutputStream();
        this.slaveWriter = new PrintWriter(new OutputStreamWriter(slaveOutput, outputEncoding()));
        this.masterOutput = masterOutput;
        this.attributes = getDefaultTerminalAttributes();
        this.size = new Size(160, 50);
        parseInfoCmp();
    }

    private static Attributes getDefaultTerminalAttributes() {
        // speed 9600 baud; 24 rows; 80 columns;
        // lflags: icanon isig iexten echo echoe -echok echoke -echonl echoctl
        //     -echoprt -altwerase -noflsh -tostop -flusho pendin -nokerninfo
        //     -extproc
        // iflags: -istrip icrnl -inlcr -igncr ixon -ixoff ixany imaxbel iutf8
        //     -ignbrk brkint -inpck -ignpar -parmrk
        // oflags: opost onlcr -oxtabs -onocr -onlret
        // cflags: cread cs8 -parenb -parodd hupcl -clocal -cstopb -crtscts -dsrflow
        //     -dtrflow -mdmbuf
        // cchars: discard = ^O; dsusp = ^Y; eof = ^D; eol = <undef>;
        //     eol2 = <undef>; erase = ^?; intr = ^C; kill = ^U; lnext = ^V;
        //     min = 1; quit = ^\\; reprint = ^R; start = ^Q; status = ^T;
        //     stop = ^S; susp = ^Z; time = 0; werase = ^W;
        Attributes attr = new Attributes();
        attr.setLocalFlags(EnumSet.of(
                LocalFlag.ICANON,
                LocalFlag.ISIG,
                LocalFlag.IEXTEN,
                LocalFlag.ECHO,
                LocalFlag.ECHOE,
                LocalFlag.ECHOKE,
                LocalFlag.ECHOCTL,
                LocalFlag.PENDIN));
        attr.setInputFlags(EnumSet.of(
                InputFlag.ICRNL,
                InputFlag.IXON,
                InputFlag.IXANY,
                InputFlag.IMAXBEL,
                InputFlag.IUTF8,
                InputFlag.BRKINT));
        attr.setOutputFlags(EnumSet.of(OutputFlag.OPOST, OutputFlag.ONLCR));
        attr.setControlChar(ControlChar.VDISCARD, ctrl('O'));
        attr.setControlChar(ControlChar.VDSUSP, ctrl('Y'));
        attr.setControlChar(ControlChar.VEOF, ctrl('D'));
        attr.setControlChar(ControlChar.VERASE, ctrl('?'));
        attr.setControlChar(ControlChar.VINTR, ctrl('C'));
        attr.setControlChar(ControlChar.VKILL, ctrl('U'));
        attr.setControlChar(ControlChar.VLNEXT, ctrl('V'));
        attr.setControlChar(ControlChar.VMIN, 1);
        attr.setControlChar(ControlChar.VQUIT, ctrl('\\'));
        attr.setControlChar(ControlChar.VREPRINT, ctrl('R'));
        attr.setControlChar(ControlChar.VSTART, ctrl('Q'));
        attr.setControlChar(ControlChar.VSTATUS, ctrl('T'));
        attr.setControlChar(ControlChar.VSTOP, ctrl('S'));
        attr.setControlChar(ControlChar.VSUSP, ctrl('Z'));
        attr.setControlChar(ControlChar.VTIME, 0);
        attr.setControlChar(ControlChar.VWERASE, ctrl('W'));
        return attr;
    }

    private static int ctrl(char c) {
        return c == '?' ? 177 : c - 64;
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
        return new Attributes(attributes);
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
     * @throws IOException if anything wrong happens
     */
    public void processInputByte(int c) throws IOException {
        boolean flushOut = doProcessInputByte(c);
        slaveInputPipe.flush();
        if (flushOut) {
            masterOutput.flush();
        }
    }

    public void processInputBytes(byte[] input) throws IOException {
        processInputBytes(input, 0, input.length);
    }

    public void processInputBytes(byte[] input, int offset, int length) throws IOException {
        boolean flushOut = false;
        for (int i = 0; i < length; i++) {
            flushOut |= doProcessInputByte(input[offset + i]);
        }
        slaveInputPipe.flush();
        if (flushOut) {
            masterOutput.flush();
        }
    }

    protected boolean doProcessInputByte(int c) throws IOException {
        if (attributes.getLocalFlag(LocalFlag.ISIG)) {
            if (c == attributes.getControlChar(ControlChar.VINTR)) {
                raise(Signal.INT);
                return false;
            } else if (c == attributes.getControlChar(ControlChar.VQUIT)) {
                raise(Signal.QUIT);
                return false;
            } else if (c == attributes.getControlChar(ControlChar.VSUSP)) {
                raise(Signal.TSTP);
                return false;
            } else if (c == attributes.getControlChar(ControlChar.VSTATUS)) {
                raise(Signal.INFO);
            }
        }
        if (attributes.getInputFlag(InputFlag.INORMEOL)) {
            if (c == '\r') {
                skipNextLf = true;
                c = '\n';
            } else if (c == '\n') {
                if (skipNextLf) {
                    skipNextLf = false;
                    return false;
                }
            } else {
                skipNextLf = false;
            }
        } else if (c == '\r') {
            if (attributes.getInputFlag(InputFlag.IGNCR)) {
                return false;
            }
            if (attributes.getInputFlag(InputFlag.ICRNL)) {
                c = '\n';
            }
        } else if (c == '\n' && attributes.getInputFlag(InputFlag.INLCR)) {
            c = '\r';
        }
        boolean flushOut = false;
        if (attributes.getLocalFlag(LocalFlag.ECHO)) {
            processOutputByte(c);
            flushOut = true;
        }
        slaveInputPipe.write(c);
        return flushOut;
    }

    /**
     * Master output processing.
     * All data going to the master should be provided by this method.
     *
     * @param c the output byte
     * @throws IOException if anything wrong happens
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

    protected void processIOException(IOException ioException) {
        this.slaveInput.setIoException(ioException);
    }

    protected void doClose() throws IOException {
        super.doClose();
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

    @Override
    public TerminalProvider getProvider() {
        return null;
    }

    @Override
    public SystemStream getSystemStream() {
        return null;
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
            } else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return;
            }
            for (int i = 0; i < len; i++) {
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
