/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.ffm;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.charset.Charset;

import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.PosixPtyTerminal;
import org.jline.terminal.impl.PosixSysTerminal;
import org.jline.terminal.spi.Pty;
import org.jline.terminal.spi.SystemStream;
import org.jline.terminal.spi.TerminalProvider;
import org.jline.utils.OSUtils;
import org.jline.utils.Signals;

public class FfmTerminalProvider implements TerminalProvider {

    public FfmTerminalProvider() {
        if (!FfmTerminalProvider.class.getModule().isNativeAccessEnabled()) {
            throw new UnsupportedOperationException(
                    "Native access is not enabled for the current module: " + FfmTerminalProvider.class.getModule());
        }
    }

    @Override
    public String name() {
        return TerminalBuilder.PROP_PROVIDER_FFM;
    }

    @Override
    public int getConsoleCodepage() {
        if (OSUtils.IS_WINDOWS) {
            return Kernel32.GetConsoleOutputCP();
        }
        return -1;
    }

    /**
     * Creates a Terminal bound to the process system streams (stdin/stdout/stderr) or an equivalent PTY-backed terminal.
     *
     * @param name            terminal name or identifier
     * @param type            terminal type (TERM)
     * @param ansiPassThrough if true, pass ANSI/VT sequences through without filtering
     * @param encoding        overall character set for the terminal
     * @param inputEncoding   character set used for input decoding
     * @param outputEncoding  character set used for output encoding
     * @param nativeSignals   if true, attempt to use native signal handling when available
     * @param signalHandler   handler invoked for terminal signals
     * @param paused          if true, create the terminal in a paused state (input/output suspended)
     * @param systemStream    which system stream this terminal should be associated with
     * @return a Terminal instance bound to the specified system stream (or an equivalent PTY-backed terminal)
     * @throws IOException if the native PTY or system terminal cannot be created or accessed
     */
    @Override
    public Terminal sysTerminal(
            String name,
            String type,
            boolean ansiPassThrough,
            Charset encoding,
            Charset inputEncoding,
            Charset outputEncoding,
            boolean nativeSignals,
            Terminal.SignalHandler signalHandler,
            boolean paused,
            SystemStream systemStream)
            throws IOException {
        if (OSUtils.IS_WINDOWS) {
            return NativeWinSysTerminal.createTerminal(
                    this,
                    systemStream,
                    name,
                    type,
                    ansiPassThrough,
                    encoding,
                    inputEncoding,
                    outputEncoding,
                    nativeSignals,
                    signalHandler,
                    paused);
        } else {
            Pty pty = new FfmNativePty(
                    this,
                    systemStream,
                    -1,
                    null,
                    0,
                    FileDescriptor.in,
                    systemStream == SystemStream.Output ? 1 : 2,
                    systemStream == SystemStream.Output ? FileDescriptor.out : FileDescriptor.err,
                    CLibrary.ttyName(0));
            return new PosixSysTerminal(
                    this, name, type, pty, encoding, inputEncoding, outputEncoding, nativeSignals, signalHandler);
        }
    }

    @Override
    public Terminal newTerminal(
            String name,
            String type,
            InputStream in,
            OutputStream out,
            Charset encoding,
            Charset inputEncoding,
            Charset outputEncoding,
            Terminal.SignalHandler signalHandler,
            boolean paused,
            Attributes attributes,
            Size size)
            throws IOException {
        Pty pty = CLibrary.openpty(this, attributes, size);
        return new PosixPtyTerminal(
                name, type, pty, in, out, encoding, inputEncoding, outputEncoding, signalHandler, paused);
    }

    @Override
    public boolean isSystemStream(SystemStream stream) {
        if (OSUtils.IS_WINDOWS) {
            return isWindowsSystemStream(stream);
        } else {
            return isPosixSystemStream(stream);
        }
    }

    public boolean isWindowsSystemStream(SystemStream stream) {
        return NativeWinSysTerminal.isWindowsSystemStream(stream);
    }

    public boolean isPosixSystemStream(SystemStream stream) {
        return FfmNativePty.isPosixSystemStream(stream);
    }

    @Override
    public String systemStreamName(SystemStream stream) {
        return FfmNativePty.posixSystemStreamName(stream);
    }

    /**
     * Obtain the current width, in columns, of the specified system stream.
     *
     * @param stream the system stream whose width to query
     * @return the width in columns of the given system stream
     */
    @Override
    public int systemStreamWidth(SystemStream stream) {
        return FfmNativePty.systemStreamWidth(stream);
    }

    /**
     * Register a handler to be invoked when the specified signal is delivered.
     *
     * @param signal the name of the signal to handle (platform-specific string)
     * @param handler the runnable to execute when the signal is received
     * @return an opaque registration handle that can be passed to {@code unregisterSignal} to remove the handler
     */
    @Override
    public Object registerSignal(String signal, Runnable handler) {
        Object reg = FfmSignalHandler.register(signal, handler);
        if (reg != null) {
            return reg;
        }
        return Signals.register(signal, handler);
    }

    /**
     * Register the default handler for the specified signal, preferring the FFM handler if available.
     *
     * @param signal the name of the signal (for example, "INT" or "TERM")
     * @return an object representing the installed registration; the FFM registration if one was created, otherwise the fallback Signals registration
     */
    @Override
    public Object registerDefaultSignal(String signal) {
        Object reg = FfmSignalHandler.registerDefault(signal);
        if (reg != null) {
            return reg;
        }
        return Signals.registerDefault(signal);
    }

    /**
     * Unregisters a previously registered signal handler; uses FFM unregistration when the
     * provided registration is an FFM registration, otherwise uses the platform fallback.
     *
     * @param signal the name of the signal to unregister (e.g., "INT", "TERM")
     * @param registration the registration token returned by a prior register call
     */
    @Override
    public void unregisterSignal(String signal, Object registration) {
        if (registration instanceof FfmSignalHandler.Registration) {
            FfmSignalHandler.unregister(signal, registration);
        } else {
            Signals.unregister(signal, registration);
        }
    }

    /**
     * Provide a short identifying string for this terminal provider.
     *
     * @return a string of the form "TerminalProvider[<name>]" identifying the provider
     */
    @Override
    public String toString() {
        return "TerminalProvider[" + name() + "]";
    }

    static VarHandle lookupVarHandle(MemoryLayout layout, PathElement... element) {
        VarHandle h = layout.varHandle(element);

        // the last parameter of the VarHandle is additional offset, hardcode zero:
        h = MethodHandles.insertCoordinates(h, h.coordinateTypes().size() - 1, 0L);

        return h;
    }
}
