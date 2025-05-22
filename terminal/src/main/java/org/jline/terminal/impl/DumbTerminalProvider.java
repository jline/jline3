/*
 * Copyright (c) 2023-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.spi.SystemStream;
import org.jline.terminal.spi.TerminalProvider;

/**
 * Terminal provider implementation for dumb terminals.
 *
 * <p>
 * The DumbTerminalProvider class provides a TerminalProvider implementation that
 * creates DumbTerminal instances. Dumb terminals have minimal capabilities and
 * are used as a fallback when more capable terminal implementations cannot be
 * created or when running in environments with limited terminal support.
 * </p>
 *
 * <p>
 * This provider supports two types of dumb terminals:
 * </p>
 * <ul>
 *   <li>Standard dumb terminal ({@link Terminal#TYPE_DUMB}) - No color support</li>
 *   <li>Color dumb terminal ({@link Terminal#TYPE_DUMB_COLOR}) - Basic color support</li>
 * </ul>
 *
 * <p>
 * The provider name is "dumb", which can be specified in the {@code org.jline.terminal.provider}
 * system property to force the use of this provider. This is useful in environments
 * where other terminal providers might not work correctly or when terminal capabilities
 * are not needed.
 * </p>
 *
 * @see org.jline.terminal.spi.TerminalProvider
 * @see org.jline.terminal.impl.DumbTerminal
 */
public class DumbTerminalProvider implements TerminalProvider {

    @Override
    public String name() {
        return TerminalBuilder.PROP_PROVIDER_DUMB;
    }

    @Override
    public Terminal sysTerminal(
            String name,
            String type,
            boolean ansiPassThrough,
            Charset encoding,
            Charset stdinEncoding,
            Charset stdoutEncoding,
            Charset stderrEncoding,
            boolean nativeSignals,
            Terminal.SignalHandler signalHandler,
            boolean paused,
            SystemStream systemStream)
            throws IOException {
        return new DumbTerminal(
                this,
                systemStream,
                name,
                type,
                new FileInputStream(FileDescriptor.in),
                new FileOutputStream(systemStream == SystemStream.Error ? FileDescriptor.err : FileDescriptor.out),
                encoding,
                stdinEncoding,
                stdoutEncoding,
                stderrEncoding,
                signalHandler);
    }

    @Override
    public Terminal newTerminal(
            String name,
            String type,
            InputStream masterInput,
            OutputStream masterOutput,
            Charset encoding,
            Charset stdinEncoding,
            Charset stdoutEncoding,
            Charset stderrEncoding,
            Terminal.SignalHandler signalHandler,
            boolean paused,
            Attributes attributes,
            Size size)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSystemStream(SystemStream stream) {
        return false;
    }

    @Override
    public String systemStreamName(SystemStream stream) {
        return null;
    }

    @Override
    public int systemStreamWidth(SystemStream stream) {
        return 0;
    }

    @Override
    public String toString() {
        return "TerminalProvider[" + name() + "]";
    }
}
