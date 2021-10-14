/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.jna;

import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.jna.win.JnaWinSysTerminal;
import org.jline.terminal.spi.JnaSupport;
import org.jline.terminal.spi.NativeSupport;
import org.jline.terminal.spi.Pty;

import java.io.IOException;
import java.nio.charset.Charset;

public class JnaSupportImpl implements JnaSupport
{
    @Override
    public Pty current(NativeSupport.Stream console) throws IOException {
        return JnaNativePty.current(console);
    }

    @Override
    public Pty open(Attributes attributes, Size size) throws IOException {
        return JnaNativePty.open(attributes, size);
    }

    @Override
    public Terminal winSysTerminal(String name, String type, boolean ansiPassThrough,
                                   Charset encoding, int codepage, boolean nativeSignals,
                                   Terminal.SignalHandler signalHandler, boolean paused,
                                   NativeSupport.Stream console) throws IOException {
        return JnaWinSysTerminal.createTerminal(name, type, ansiPassThrough, encoding, codepage, nativeSignals, signalHandler, paused, console);
    }

    @Override
    public boolean isWindowsSystemStream(Stream stream) {
        return JnaWinSysTerminal.isWindowsSystemStream(stream);
    }

    @Override
    public boolean isPosixSystemStream(Stream stream) {
        return JnaNativePty.isPosixSystemStream(stream);
    }

    @Override
    public String posixSystemStreamName(Stream stream) {
        return JnaNativePty.posixSystemStreamName(stream);
    }
}
