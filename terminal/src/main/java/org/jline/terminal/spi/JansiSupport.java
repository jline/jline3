package org.jline.terminal.spi;

import org.jline.terminal.Terminal;

import java.io.IOException;

public interface JansiSupport {

    Pty current() throws IOException;

    Terminal winSysTerminal(String name, boolean nativeSignals, Terminal.SignalHandler signalHandler) throws IOException;

}
