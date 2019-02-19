package org.jline.terminal.impl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.jline.utils.ShutdownHooks;
import org.jline.utils.Signals;
import org.jline.utils.ShutdownHooks.Task;

public abstract class AbstractSystemTerminal extends AbstractTerminal {

    protected final Map<Signal, Object> nativeHandlers = new HashMap<>();
    protected final Task closer;
    
    public AbstractSystemTerminal(String name, String type) throws IOException {
        this(name, type, null, SignalHandler.SIG_DFL, true);
    }

    public AbstractSystemTerminal(String name, String type, Charset encoding, SignalHandler signalHandler,
            boolean nativeSignals) throws IOException {
        super(name, type, encoding, signalHandler);

        if (nativeSignals) {
            for (final Signal signal : Signal.values()) {
                if (signalHandler == SignalHandler.SIG_DFL) {
                    nativeHandlers.put(signal, Signals.registerDefault(signal.name()));
                } else {
                    nativeHandlers.put(signal, Signals.register(signal.name(), () -> raise(signal)));
                }
            }
        }

        closer = this::close;
        ShutdownHooks.add(closer);

    }

    @Override
    public void close() throws IOException {
        super.close();
        ShutdownHooks.remove(closer);
        for (Map.Entry<Signal, Object> entry : nativeHandlers.entrySet()) {
            Signals.unregister(entry.getKey().name(), entry.getValue());
        }
    }

    @Override
    public SignalHandler handle(Signal signal, SignalHandler handler) {
        SignalHandler prev = super.handle(signal, handler);
        if (prev != handler) {
            if (handler == SignalHandler.SIG_DFL) {
                Signals.registerDefault(signal.name());
            } else {
                Signals.register(signal.name(), () -> raise(signal));
            }
        }
        return prev;
    }

}
