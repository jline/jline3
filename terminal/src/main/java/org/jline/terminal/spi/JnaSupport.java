package org.jline.terminal.spi;

import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;

import java.io.IOException;

public interface JnaSupport {

    Pty current() throws IOException;

    Pty open(Attributes attributes, Size size) throws IOException;

    Terminal winSysTerminal(String name, boolean nativeSignals, Terminal.SignalHandler signalHandler) throws IOException;

}
