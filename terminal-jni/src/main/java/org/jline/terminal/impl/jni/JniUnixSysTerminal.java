/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.jni;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.jline.nativ.CLibrary;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Sized;
import org.jline.terminal.impl.AbstractUnixSysTerminal;
import org.jline.terminal.impl.TermiosMapping;
import org.jline.terminal.spi.SystemStream;
import org.jline.terminal.spi.TerminalProvider;
import org.jline.utils.NonBlockingInputStream;
import org.jline.utils.NonBlockingInputStream.TimedReader;

import static org.jline.terminal.impl.TermiosData.TCSANOW;

/**
 * JNI-based POSIX system terminal that calls {@link CLibrary} directly.
 *
 * <p>This implementation bypasses the PTY abstraction layer, reducing the call
 * chain from 7 layers to 4:</p>
 * <pre>
 *   Terminal → AbstractTerminal → AbstractUnixSysTerminal → JniUnixSysTerminal → CLibrary → syscall
 * </pre>
 *
 * <p>A {@link TermiosMapping} instance handles platform-specific
 * {@code Termios} ↔ {@code Attributes} conversions.</p>
 */
public class JniUnixSysTerminal extends AbstractUnixSysTerminal {

    private final TermiosMapping mapping;
    private final int outputFd;

    @SuppressWarnings({"this-escape", "squid:S107"})
    JniUnixSysTerminal(
            TerminalProvider provider,
            SystemStream systemStream,
            TermiosMapping mapping,
            String name,
            String type,
            Charset encoding,
            Charset inputEncoding,
            Charset outputEncoding,
            boolean nativeSignals,
            SignalHandler signalHandler)
            throws IOException {
        super(
                provider,
                systemStream,
                name,
                type,
                encoding,
                inputEncoding,
                outputEncoding,
                nativeSignals,
                signalHandler,
                readAttributes(mapping));
        this.mapping = mapping;
        this.outputFd = (systemStream == SystemStream.Output) ? STDOUT_FD : STDERR_FD;
    }

    private static Attributes readAttributes(TermiosMapping mapping) {
        CLibrary.Termios tios = new CLibrary.Termios();
        CLibrary.tcgetattr(STDIN_FD, tios);
        return mapping.toAttributes(JniNativePty.fromNativeTermios(tios));
    }

    @Override
    protected TimedReader createTimedStdinReader(InputStream stdin) {
        return (timeoutMs) -> {
            int ret = CLibrary.pollIn(STDIN_FD, timeoutMs);
            if (ret > 0) {
                return stdin.read();
            } else if (ret == 0) {
                return NonBlockingInputStream.READ_EXPIRED;
            } else {
                throw new IOException("poll() failed on stdin");
            }
        };
    }

    @Override
    protected Attributes doGetAttributes() {
        return readAttributes(mapping);
    }

    @Override
    protected void doSetAttributes(Attributes attr) {
        CLibrary.Termios tios = new CLibrary.Termios();
        CLibrary.tcgetattr(STDIN_FD, tios);
        JniNativePty.applyAttributes(tios, attr);
        CLibrary.tcsetattr(STDIN_FD, TCSANOW, tios);
    }

    @Override
    protected Size doGetSize() {
        CLibrary.WinSize sz = new CLibrary.WinSize();
        CLibrary.ioctl(outputFd, CLibrary.TIOCGWINSZ, sz);
        return Size.of(sz.ws_col, sz.ws_row);
    }

    @Override
    protected void doSetSize(Sized size) {
        CLibrary.WinSize sz = new CLibrary.WinSize((short) size.getRows(), (short) size.getColumns());
        CLibrary.ioctl(outputFd, CLibrary.TIOCSWINSZ, sz);
    }
}
