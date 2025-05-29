/*
 * Copyright (c) 2002-2020, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.jna;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.PosixPtyTerminal;
import org.jline.terminal.impl.PosixSysTerminal;
import org.jline.terminal.impl.jna.win.JnaWinSysTerminal;
import org.jline.terminal.spi.Pty;
import org.jline.terminal.spi.SystemStream;
import org.jline.terminal.spi.TerminalProvider;
import org.jline.utils.OSUtils;

public class JnaTerminalProvider implements TerminalProvider {

    public JnaTerminalProvider() {
        checkSystemStream(SystemStream.Output);
    }

    @Override
    public String name() {
        return TerminalBuilder.PROP_PROVIDER_JNA;
    }

    public Pty current(SystemStream systemStream) throws IOException {
        return JnaNativePty.current(this, systemStream);
    }

    public Pty open(Attributes attributes, Size size) throws IOException {
        return JnaNativePty.open(this, attributes, size);
    }

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
            return winSysTerminal(
                    name,
                    type,
                    ansiPassThrough,
                    encoding,
                    inputEncoding,
                    outputEncoding,
                    outputEncoding,
                    nativeSignals,
                    signalHandler,
                    paused,
                    systemStream);
        } else {
            return posixSysTerminal(
                    name,
                    type,
                    ansiPassThrough,
                    encoding,
                    inputEncoding,
                    outputEncoding,
                    outputEncoding,
                    nativeSignals,
                    signalHandler,
                    paused,
                    systemStream);
        }
    }

    @SuppressWarnings("deprecation")
    @Deprecated
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
        if (OSUtils.IS_WINDOWS) {
            return winSysTerminal(
                    name,
                    type,
                    ansiPassThrough,
                    encoding,
                    stdinEncoding,
                    stdoutEncoding,
                    stderrEncoding,
                    nativeSignals,
                    signalHandler,
                    paused,
                    systemStream);
        } else {
            return posixSysTerminal(
                    name,
                    type,
                    ansiPassThrough,
                    encoding,
                    stdinEncoding,
                    stdoutEncoding,
                    stderrEncoding,
                    nativeSignals,
                    signalHandler,
                    paused,
                    systemStream);
        }
    }

    public Terminal winSysTerminal(
            String name,
            String type,
            boolean ansiPassThrough,
            Charset encoding,
            boolean nativeSignals,
            Terminal.SignalHandler signalHandler,
            boolean paused,
            SystemStream systemStream)
            throws IOException {
        return winSysTerminal(
                name,
                type,
                ansiPassThrough,
                encoding,
                encoding,
                encoding,
                encoding,
                nativeSignals,
                signalHandler,
                paused,
                systemStream);
    }

    public Terminal winSysTerminal(
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
        return JnaWinSysTerminal.createTerminal(
                this,
                systemStream,
                name,
                type,
                ansiPassThrough,
                encoding,
                stdinEncoding,
                stdoutEncoding,
                stderrEncoding,
                nativeSignals,
                signalHandler,
                paused);
    }

    public Terminal posixSysTerminal(
            String name,
            String type,
            boolean ansiPassThrough,
            Charset encoding,
            boolean nativeSignals,
            Terminal.SignalHandler signalHandler,
            boolean paused,
            SystemStream systemStream)
            throws IOException {
        return posixSysTerminal(
                name,
                type,
                ansiPassThrough,
                encoding,
                encoding,
                encoding,
                encoding,
                nativeSignals,
                signalHandler,
                paused,
                systemStream);
    }

    public Terminal posixSysTerminal(
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
        Pty pty = current(systemStream);
        // Use the appropriate output encoding based on the system stream
        Charset outputEncoding = systemStream == SystemStream.Error ? stderrEncoding : stdoutEncoding;
        return new PosixSysTerminal(
                name, type, pty, encoding, stdinEncoding, outputEncoding, nativeSignals, signalHandler);
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
        Pty pty = open(attributes, size);
        return new PosixPtyTerminal(
                name, type, pty, in, out, encoding, inputEncoding, outputEncoding, signalHandler, paused);
    }

    @SuppressWarnings("deprecation")
    @Deprecated
    @Override
    public Terminal newTerminal(
            String name,
            String type,
            InputStream in,
            OutputStream out,
            Charset encoding,
            Charset stdinEncoding,
            Charset stdoutEncoding,
            Charset stderrEncoding,
            Terminal.SignalHandler signalHandler,
            boolean paused,
            Attributes attributes,
            Size size)
            throws IOException {
        Pty pty = open(attributes, size);
        return new PosixPtyTerminal(
                name, type, pty, in, out, encoding, stdinEncoding, stdoutEncoding, signalHandler, paused);
    }

    @Override
    public boolean isSystemStream(SystemStream stream) {
        try {
            return checkSystemStream(stream);
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean checkSystemStream(SystemStream stream) {
        if (OSUtils.IS_WINDOWS) {
            return JnaWinSysTerminal.isWindowsSystemStream(stream);
        } else {
            return JnaNativePty.isPosixSystemStream(stream);
        }
    }

    @Override
    public String systemStreamName(SystemStream stream) {
        if (OSUtils.IS_WINDOWS) {
            return null;
        } else {
            return JnaNativePty.posixSystemStreamName(stream);
        }
    }

    @Override
    public int systemStreamWidth(SystemStream stream) {
        try (Pty pty = current(stream)) {
            return pty.getSize().getColumns();
        } catch (Throwable t) {
            return -1;
        }
    }

    @Override
    public String toString() {
        return "TerminalProvider[" + name() + "]";
    }
}
