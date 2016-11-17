/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.terminal;

import java.io.Closeable;
import java.io.Flushable;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.function.IntConsumer;

import org.jline.terminal.impl.NativeSignalHandler;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.NonBlockingReader;

/**
 * A terminal representing a virtual terminal on the computer.
 *
 * Terminals should be closed by calling the {@link #close()} method
 * in order to restore their original state.
 */
public interface Terminal extends Closeable, Flushable {

    /**
     * Type used for dumb terminals.
     */
    String TYPE_DUMB = "dumb";

    String getName();

    //
    // Signal support
    //

    enum Signal {
        INT,
        QUIT,
        TSTP,
        CONT,
        INFO,
        WINCH
    }

    interface SignalHandler {

        SignalHandler SIG_DFL = NativeSignalHandler.SIG_DFL;
        SignalHandler SIG_IGN = NativeSignalHandler.SIG_IGN;

        void handle(Signal signal);
    }

    SignalHandler handle(Signal signal, SignalHandler handler);

    void raise(Signal signal);

    //
    // Input / output
    //

    NonBlockingReader reader();

    PrintWriter writer();

    InputStream input();

    OutputStream output();

    //
    // Pty settings
    //

    Attributes enterRawMode();

    boolean echo();

    boolean echo(boolean echo);

    Attributes getAttributes();

    void setAttributes(Attributes attr);

    Size getSize();

    void setSize(Size size);

    default int getWidth() {
        return getSize().getColumns();
    }

    default int getHeight() {
        return getSize().getRows();
    }

    void flush();

    //
    // Infocmp capabilities
    //

    String getType();

    boolean puts(Capability capability, Object... params);

    boolean getBooleanCapability(Capability capability);

    Integer getNumericCapability(Capability capability);

    String getStringCapability(Capability capability);

    //
    // Cursor support
    //

    /**
     * Query the terminal to report the cursor position.
     *
     * As the response is read from the input stream, some
     * characters may be read before the cursor position is actually
     * read. Those characters can be given back using
     * {@link org.jline.keymap.BindingReader#runMacro(String)}.
     *
     * @param discarded a consumer receiving discarded characters
     * @return <code>null</code> if cursor position reporting
     *                  is not supported or a valid cursor position
     */
    Cursor getCursorPosition(IntConsumer discarded);

    //
    // Mouse support
    //

    enum MouseTracking {
        Off, Normal, Button, Any
    }

    boolean hasMouseSupport();

    boolean trackMouse(MouseTracking tracking);

    MouseEvent readMouseEvent();

}
