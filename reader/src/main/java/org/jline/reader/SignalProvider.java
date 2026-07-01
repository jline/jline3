/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader;

import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.Terminal.SignalHandler;

/**
 * Provides signal handling and terminal state management for the line editing engine.
 *
 * <p>This interface abstracts signal registration and terminal attribute management
 * so that the editing engine can work in any environment, not just a JLine {@code Terminal}.
 * TUI frameworks implement this interface to integrate with their own signal and
 * mode management.</p>
 *
 * @since 4.1
 * @see EditingTerminal
 */
public interface SignalProvider {

    /**
     * Registers a handler for the given signal, returning the previously registered handler.
     *
     * <p>The editing engine uses this to intercept {@code INT} (Ctrl-C),
     * {@code WINCH} (terminal resize), and {@code CONT} (resume after suspend).</p>
     *
     * @param signal  the signal to handle
     * @param handler the new handler
     * @return the previously registered handler, or {@code null}
     */
    SignalHandler handle(Signal signal, SignalHandler handler);

    /**
     * Enters raw mode, returning the previous terminal attributes for later restoration.
     *
     * <p>In raw mode, the terminal delivers each keystroke immediately without
     * line buffering or echo. The default implementation returns a new empty
     * {@code Attributes} instance (suitable for environments that are always in raw mode).</p>
     *
     * @return the previous attributes, to be passed to {@link #setAttributes(Attributes)} on cleanup
     */
    default Attributes enterRawMode() {
        return new Attributes();
    }

    /**
     * Restores terminal attributes previously saved by {@link #enterRawMode()}.
     *
     * <p>The default implementation is a no-op, suitable for environments
     * that manage their own terminal mode.</p>
     *
     * @param attr the attributes to restore
     */
    default void setAttributes(Attributes attr) {}

    /**
     * Returns the current terminal attributes.
     *
     * <p>The default implementation returns a new empty {@code Attributes} instance.</p>
     *
     * @return the current attributes
     */
    default Attributes getAttributes() {
        return new Attributes();
    }
}
