/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import java.io.PrintWriter;
import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import org.jline.reader.EditingTerminal;
import org.jline.terminal.Attributes;
import org.jline.terminal.Cursor;
import org.jline.terminal.MouseEvent;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.Terminal.SignalHandler;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.NonBlockingReader;

/**
 * A {@link EditingTerminal} that delegates all operations to a JLine {@link Terminal}.
 *
 * <p>This adapter is used by {@link LineReaderImpl} to maintain full backward
 * compatibility with existing code that constructs a {@code LineReader} from a
 * {@code Terminal}. Every provider method simply delegates to the corresponding
 * {@code Terminal} method.</p>
 *
 * @since 4.1
 * @see EditingTerminal
 * @see Terminal
 */
public class TerminalAdapter implements EditingTerminal {

    private final Terminal terminal;

    /**
     * Creates a new adapter wrapping the given terminal.
     *
     * @param terminal the terminal to delegate to; must not be {@code null}
     * @throws NullPointerException if {@code terminal} is {@code null}
     */
    public TerminalAdapter(Terminal terminal) {
        this.terminal = Objects.requireNonNull(terminal, "terminal must not be null");
    }

    /**
     * Returns the underlying terminal.
     *
     * @return the wrapped terminal
     */
    public Terminal getTerminal() {
        return terminal;
    }

    // --- InputProvider ---

    @Override
    public NonBlockingReader reader() {
        return terminal.reader();
    }

    @Override
    public MouseEvent readMouseEvent(IntSupplier reader, String lastBinding) {
        return terminal.readMouseEvent(reader, lastBinding);
    }

    // --- OutputProvider ---

    @Override
    public PrintWriter writer() {
        return terminal.writer();
    }

    @Override
    public void flush() {
        terminal.flush();
    }

    @Override
    public boolean puts(Capability capability, Object... params) {
        return terminal.puts(capability, params);
    }

    @Override
    public boolean trackMouse(Terminal.MouseTracking tracking) {
        return terminal.trackMouse(tracking);
    }

    // --- SizeProvider ---

    @Override
    public Size getSize() {
        return terminal.getSize();
    }

    @Override
    public Size getBufferSize() {
        return terminal.getBufferSize();
    }

    // --- CapabilityProvider ---

    @Override
    public String getStringCapability(Capability capability) {
        return terminal.getStringCapability(capability);
    }

    @Override
    public boolean getBooleanCapability(Capability capability) {
        return terminal.getBooleanCapability(capability);
    }

    @Override
    public boolean getGraphemeClusterMode() {
        return terminal.getGraphemeClusterMode();
    }

    @Override
    public Cursor getCursorPosition(IntConsumer discarded) {
        return terminal.getCursorPosition(discarded);
    }

    @Override
    public String getName() {
        return terminal.getName();
    }

    @Override
    public String getType() {
        return terminal.getType();
    }

    // --- SignalProvider ---

    @Override
    public SignalHandler handle(Signal signal, SignalHandler handler) {
        return terminal.handle(signal, handler);
    }

    @Override
    public Attributes enterRawMode() {
        return terminal.enterRawMode();
    }

    @Override
    public void setAttributes(Attributes attr) {
        terminal.setAttributes(attr);
    }

    @Override
    public Attributes getAttributes() {
        return terminal.getAttributes();
    }
}
