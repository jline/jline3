package org.jline.terminal.impl.jansi;

import org.jline.terminal.Terminal;
import org.jline.terminal.spi.JansiSupport;

import java.io.IOException;

public class JansiSupportImpl implements JansiSupport {

    @Override
    public Terminal winSysTerminal(String name, boolean nativeSignals, Terminal.SignalHandler signalHandler) throws IOException {
            return new JansiWinSysTerminal(name, nativeSignals, signalHandler);
    }

}
