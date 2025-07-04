/*
 * Copyright (c) 2002-2020, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.jni;

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
import org.jline.terminal.impl.jni.freebsd.FreeBsdNativePty;
import org.jline.terminal.impl.jni.linux.LinuxNativePty;
import org.jline.terminal.impl.jni.osx.OsXNativePty;
import org.jline.terminal.impl.jni.solaris.SolarisNativePty;
import org.jline.terminal.impl.jni.win.NativeWinSysTerminal;
import org.jline.terminal.spi.Pty;
import org.jline.terminal.spi.SystemStream;
import org.jline.terminal.spi.TerminalProvider;
import org.jline.utils.Log;
import org.jline.utils.OSUtils;

/**
 * Terminal provider implementation that uses JNI (Java Native Interface) to access
 * native terminal functionality.
 * <p>
 * This provider requires the JLine native library to be loaded, which is handled by
 * {@link org.jline.nativ.JLineNativeLoader}. The native library provides access to
 * low-level terminal operations that are not available through standard Java APIs.
 * <p>
 * The native library is automatically loaded when this provider is used. If the library
 * cannot be loaded, the provider will not be available and JLine will fall back to other
 * available providers.
 * <p>
 * The native library loading can be configured using system properties as documented in
 * {@link org.jline.nativ.JLineNativeLoader}.
 *
 * @see org.jline.nativ.JLineNativeLoader
 * @see org.jline.terminal.TerminalBuilder
 */
public class JniTerminalProvider implements TerminalProvider {

    /**
     * Creates a new JNI terminal provider instance and ensures the native library is loaded.
     * <p>
     * The constructor initializes the JLine native library using {@link org.jline.nativ.JLineNativeLoader#initialize()}.
     * If the native library cannot be loaded, methods in this provider may throw exceptions when used.
     */
    public JniTerminalProvider() {
        try {
            // Ensure the native library is loaded
            org.jline.nativ.JLineNativeLoader.initialize();
        } catch (Exception e) {
            // Log the error but don't throw - this allows the provider to be instantiated
            // even if the native library can't be loaded. TerminalBuilder will handle this
            // by trying other providers.
            Log.debug("Failed to load JLine native library: " + e.getMessage(), e);
        }
    }

    @Override
    public String name() {
        return TerminalBuilder.PROP_PROVIDER_JNI;
    }

    public Pty current(SystemStream systemStream) throws IOException {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Linux")) {
            return LinuxNativePty.current(this, systemStream);
        } else if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
            return OsXNativePty.current(this, systemStream);
        } else if (osName.startsWith("Solaris") || osName.startsWith("SunOS")) {
            return SolarisNativePty.current(this, systemStream);
        } else if (osName.startsWith("FreeBSD")) {
            return FreeBsdNativePty.current(this, systemStream);
        }
        throw new UnsupportedOperationException();
    }

    public Pty open(Attributes attributes, Size size) throws IOException {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Linux")) {
            return LinuxNativePty.open(this, attributes, size);
        } else if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
            return OsXNativePty.open(this, attributes, size);
        } else if (osName.startsWith("Solaris") || osName.startsWith("SunOS")) {
            return SolarisNativePty.open(this, attributes, size);
        } else if (osName.startsWith("FreeBSD")) {
            return FreeBsdNativePty.open(this, attributes, size);
        }
        throw new UnsupportedOperationException();
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
        return NativeWinSysTerminal.createTerminal(
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
            if (OSUtils.IS_WINDOWS) {
                return isWindowsSystemStream(stream);
            } else {
                return isPosixSystemStream(stream);
            }
        } catch (Throwable t) {
            Log.debug("Exception while checking system stream (this may disable the JNI provider)", t);
            return false;
        }
    }

    public boolean isWindowsSystemStream(SystemStream stream) {
        return NativeWinSysTerminal.isWindowsSystemStream(stream);
    }

    public boolean isPosixSystemStream(SystemStream stream) {
        return JniNativePty.isPosixSystemStream(stream);
    }

    @Override
    public String systemStreamName(SystemStream stream) {
        return JniNativePty.posixSystemStreamName(stream);
    }

    @Override
    public int systemStreamWidth(SystemStream stream) {
        return JniNativePty.systemStreamWidth(stream);
    }

    @Override
    public String toString() {
        return "TerminalProvider[" + name() + "]";
    }
}
