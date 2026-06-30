/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal.Signal;

/**
 * Wraps an input stream to intercept signal control characters when ISIG is cleared
 * (raw mode). The byte is passed through after raising the signal so that the
 * LineReader's INTERRUPT widget can also see it via its keymap binding.
 *
 * <p>Used by both {@link AbstractUnixSysTerminal} (direct-fd terminals) and
 * {@link PosixSysTerminal} (PTY-based fallback terminals).</p>
 */
class SignalInterceptingInputStream extends FilterInputStream {

    private final Supplier<Attributes> attributesSupplier;
    private final Consumer<Signal> signalRaiser;

    SignalInterceptingInputStream(
            InputStream in, Supplier<Attributes> attributesSupplier, Consumer<Signal> signalRaiser) {
        super(in);
        this.attributesSupplier = attributesSupplier;
        this.signalRaiser = signalRaiser;
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b >= 0) {
            checkSignalByte(b);
        }
        return b;
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        int n = super.read(buf, off, len);
        if (n > 0) {
            checkSignalBytes(buf, off, n);
        }
        return n;
    }

    private void checkSignalByte(int b) {
        Attributes attr = attributesSupplier.get();
        if (attr != null && !attr.getLocalFlag(Attributes.LocalFlag.ISIG)) {
            raiseIfSignal(b, attr);
        }
    }

    private void checkSignalBytes(byte[] buf, int off, int count) {
        Attributes attr = attributesSupplier.get();
        if (attr != null && !attr.getLocalFlag(Attributes.LocalFlag.ISIG)) {
            for (int i = 0; i < count; i++) {
                raiseIfSignal(buf[off + i] & 0xFF, attr);
            }
        }
    }

    private void raiseIfSignal(int b, Attributes attr) {
        if (b == attr.getControlChar(Attributes.ControlChar.VINTR)) {
            signalRaiser.accept(Signal.INT);
        } else if (b == attr.getControlChar(Attributes.ControlChar.VQUIT)) {
            signalRaiser.accept(Signal.QUIT);
        } else if (b == attr.getControlChar(Attributes.ControlChar.VSUSP)) {
            signalRaiser.accept(Signal.TSTP);
        } else if (b == attr.getControlChar(Attributes.ControlChar.VSTATUS)) {
            signalRaiser.accept(Signal.INFO);
        }
    }
}
