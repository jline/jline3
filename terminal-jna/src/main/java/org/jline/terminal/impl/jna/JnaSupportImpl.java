package org.jline.terminal.impl.jna;

import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.jna.win.JnaWinSysTerminal;
import org.jline.terminal.spi.JnaSupport;
import org.jline.terminal.spi.Pty;

import java.io.IOException;

public class JnaSupportImpl implements JnaSupport {
    @Override
    public Pty current() throws IOException {
        return JnaNativePty.current();
    }

    @Override
    public Pty open(Attributes attributes, Size size) throws IOException {
        return JnaNativePty.open(attributes, size);
    }

    @Override
    public Terminal winSysTerminal(String name, boolean nativeSignals, Terminal.SignalHandler signalHandler) throws IOException {
        return new JnaWinSysTerminal(name, nativeSignals, signalHandler);
    }
}
