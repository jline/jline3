/*
 * Copyright (c) the original author(s).
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

import org.jline.nativ.JLineNativeLoader;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.PosixPtyTerminal;
import org.jline.terminal.impl.TermiosMapping;
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
     * Loading the native library via {@link JLineNativeLoader#initialize()} calls
     * {@code System.loadLibrary()}, which is a restricted operation. On JDK 24-25 this
     * produces a JVM warning when {@code --enable-native-access} is not set. On future JDKs
     * where native access is denied by default, the load will fail and
     * {@link TerminalBuilder} will fall back to other providers.
     */
    public JniTerminalProvider() {
        // Ensure the native library is loaded
        try {
            JLineNativeLoader.initialize();
        } catch (UnsupportedOperationException | IllegalCallerException e) {
            throw new UnsupportedOperationException(
                    "JNI native access is not available. Use --enable-native-access=ALL-UNNAMED or"
                            + " --enable-native-access=org.jline.terminal.jni to enable it.",
                    e);
        }
    }

    @Override
    public String name() {
        return TerminalBuilder.PROP_PROVIDER_JNI;
    }

    @Override
    public int getConsoleCodepage() {
        if (OSUtils.IS_WINDOWS) {
            return org.jline.nativ.Kernel32.GetConsoleOutputCP();
        }
        return -1;
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

    /**
     * Create a POSIX system terminal backed by a native PTY.
     *
     * The created terminal is configured with the provided character encodings and signal handling.
     * The output encoding is chosen from {@code stdoutEncoding} or {@code stderrEncoding}
     * depending on {@code systemStream}.
     *
     * @param name human-readable terminal name
     * @param type terminal type (TERM)
     * @param ansiPassThrough ignored — ANSI pass-through is only supported on Windows
     * @param encoding primary charset used by the terminal
     * @param stdinEncoding charset for standard input
     * @param stdoutEncoding charset for standard output
     * @param stderrEncoding charset for standard error
     * @param nativeSignals whether native signal handling is enabled
     * @param signalHandler handler for terminal signals
     * @param paused ignored — paused mode is only supported on Windows and PTY-backed terminals
     * @param systemStream which system stream the terminal represents (affects output encoding selection)
     * @return a POSIX system terminal
     * @throws IOException if the underlying PTY cannot be opened
     */
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
        TermiosMapping mapping = TermiosMapping.forCurrentPlatform();
        Charset outputEncoding = systemStream == SystemStream.Error ? stderrEncoding : stdoutEncoding;
        return new JniUnixSysTerminal(
                this,
                systemStream,
                mapping,
                name,
                type,
                encoding,
                stdinEncoding,
                outputEncoding,
                nativeSignals,
                signalHandler);
    }

    /**
     * Create a new terminal backed by a newly opened POSIX pseudo-terminal (PTY).
     *
     * @param name the terminal name for identification
     * @param type the terminal type (TERM)
     * @param in the input stream connected to the PTY slave
     * @param out the output stream connected to the PTY slave
     * @param encoding the primary charset for the terminal
     * @param inputEncoding the charset to decode input
     * @param outputEncoding the charset to encode output
     * @param signalHandler handler for terminal signals
     * @param paused if true, the terminal is created in a paused state
     * @param attributes initial terminal attributes for the PTY
     * @param size initial terminal size for the PTY
     * @return a Terminal instance wrapping the opened PTY and provided streams
     * @throws IOException if the PTY cannot be opened
     */
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
