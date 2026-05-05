/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.ffm;

import java.io.IOException;
import java.nio.charset.Charset;

import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Sized;
import org.jline.terminal.impl.AbstractUnixSysTerminal;
import org.jline.terminal.spi.SystemStream;
import org.jline.terminal.spi.TerminalProvider;

/**
 * FFM-based POSIX system terminal that calls {@link CLibrary} directly.
 *
 * <p>This implementation bypasses the PTY abstraction layer, reducing the call
 * chain from 7 layers to 4:</p>
 * <pre>
 *   Terminal → AbstractTerminal → AbstractUnixSysTerminal → FfmUnixSysTerminal → CLibrary → syscall
 * </pre>
 */
public class FfmUnixSysTerminal extends AbstractUnixSysTerminal {

    private final int outputFd;

    @SuppressWarnings({"this-escape", "squid:S107"})
    FfmUnixSysTerminal(
            TerminalProvider provider,
            SystemStream systemStream,
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
                CLibrary.getAttributes(STDIN_FD));
        this.outputFd = (systemStream == SystemStream.Output) ? STDOUT_FD : STDERR_FD;
    }

    @Override
    protected Attributes doGetAttributes() {
        return CLibrary.getAttributes(STDIN_FD);
    }

    @Override
    protected void doSetAttributes(Attributes attr) {
        CLibrary.setAttributes(STDIN_FD, attr);
    }

    @Override
    protected Size doGetSize() {
        return CLibrary.getTerminalSize(outputFd);
    }

    @Override
    protected void doSetSize(Sized size) {
        CLibrary.setTerminalSize(outputFd, size);
    }
}
