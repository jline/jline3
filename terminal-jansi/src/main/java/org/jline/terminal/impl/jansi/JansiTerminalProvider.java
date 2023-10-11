/*
 * Copyright (c) 2002-2020, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.jansi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fusesource.jansi.AnsiConsole;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.PosixPtyTerminal;
import org.jline.terminal.impl.PosixSysTerminal;
import org.jline.terminal.impl.jansi.freebsd.FreeBsdNativePty;
import org.jline.terminal.impl.jansi.linux.LinuxNativePty;
import org.jline.terminal.impl.jansi.osx.OsXNativePty;
import org.jline.terminal.impl.jansi.win.JansiWinSysTerminal;
import org.jline.terminal.spi.Pty;
import org.jline.terminal.spi.SystemStream;
import org.jline.terminal.spi.TerminalProvider;
import org.jline.utils.OSUtils;

public class JansiTerminalProvider implements TerminalProvider {

    static final int JANSI_MAJOR_VERSION;
    static final int JANSI_MINOR_VERSION;

    static {
        int major = 0, minor = 0;
        try {
            String v = null;
            try (InputStream is = AnsiConsole.class.getResourceAsStream("jansi.properties")) {
                if (is != null) {
                    Properties props = new Properties();
                    props.load(is);
                    v = props.getProperty("version");
                }
            } catch (IOException e) {
                // ignore
            }
            if (v == null) {
                v = AnsiConsole.class.getPackage().getImplementationVersion();
            }
            if (v != null) {
                Matcher m = Pattern.compile("([0-9]+)\\.([0-9]+)([\\.-]\\S+)?").matcher(v);
                if (m.matches()) {
                    major = Integer.parseInt(m.group(1));
                    minor = Integer.parseInt(m.group(2));
                }
            }
        } catch (Throwable t) {
            // Ignore
        }
        JANSI_MAJOR_VERSION = major;
        JANSI_MINOR_VERSION = minor;
    }

    public static int getJansiMajorVersion() {
        return JANSI_MAJOR_VERSION;
    }

    public static int getJansiMinorVersion() {
        return JANSI_MINOR_VERSION;
    }

    public static boolean isAtLeast(int major, int minor) {
        return JANSI_MAJOR_VERSION > major || JANSI_MAJOR_VERSION == major && JANSI_MINOR_VERSION >= minor;
    }

    @Override
    public String name() {
        return TerminalBuilder.PROP_PROVIDER_JANSI;
    }

    public Pty current(SystemStream systemStream) throws IOException {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Linux")) {
            return LinuxNativePty.current(this, systemStream);
        } else if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
            return OsXNativePty.current(this, systemStream);
        } else if (osName.startsWith("Solaris") || osName.startsWith("SunOS")) {
            // Solaris is not supported by jansi
            // return SolarisNativePty.current();
        } else if (osName.startsWith("FreeBSD")) {
            if (isAtLeast(1, 16)) {
                return FreeBsdNativePty.current(this, systemStream);
            }
        }
        throw new UnsupportedOperationException();
    }

    public Pty open(Attributes attributes, Size size) throws IOException {
        if (isAtLeast(1, 16)) {
            String osName = System.getProperty("os.name");
            if (osName.startsWith("Linux")) {
                return LinuxNativePty.open(this, attributes, size);
            } else if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
                return OsXNativePty.open(this, attributes, size);
            } else if (osName.startsWith("Solaris") || osName.startsWith("SunOS")) {
                // Solaris is not supported by jansi
                // return SolarisNativePty.current();
            } else if (osName.startsWith("FreeBSD")) {
                return FreeBsdNativePty.open(this, attributes, size);
            }
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public Terminal sysTerminal(
            String name,
            String type,
            boolean ansiPassThrough,
            Charset encoding,
            boolean nativeSignals,
            Terminal.SignalHandler signalHandler,
            boolean paused,
            SystemStream systemStream)
            throws IOException {
        if (OSUtils.IS_WINDOWS) {
            return winSysTerminal(
                    name, type, ansiPassThrough, encoding, nativeSignals, signalHandler, paused, systemStream);
        } else {
            return posixSysTerminal(
                    name, type, ansiPassThrough, encoding, nativeSignals, signalHandler, paused, systemStream);
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
        if (isAtLeast(1, 12)) {
            JansiWinSysTerminal terminal = JansiWinSysTerminal.createTerminal(
                    this, systemStream, name, type, ansiPassThrough, encoding, nativeSignals, signalHandler, paused);
            if (!isAtLeast(1, 16)) {
                terminal.disableScrolling();
            }
            return terminal;
        }
        throw new UnsupportedOperationException();
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
        Pty pty = current(systemStream);
        return new PosixSysTerminal(name, type, pty, encoding, nativeSignals, signalHandler);
    }

    @Override
    public Terminal newTerminal(
            String name,
            String type,
            InputStream in,
            OutputStream out,
            Charset encoding,
            Terminal.SignalHandler signalHandler,
            boolean paused,
            Attributes attributes,
            Size size)
            throws IOException {
        Pty pty = open(attributes, size);
        return new PosixPtyTerminal(name, type, pty, in, out, encoding, signalHandler, paused);
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
            return false;
        }
    }

    public boolean isWindowsSystemStream(SystemStream stream) {
        return JansiWinSysTerminal.isWindowsSystemStream(stream);
    }

    public boolean isPosixSystemStream(SystemStream stream) {
        return JansiNativePty.isPosixSystemStream(stream);
    }

    @Override
    public String systemStreamName(SystemStream stream) {
        return JansiNativePty.posixSystemStreamName(stream);
    }

    @Override
    public int systemStreamWidth(SystemStream stream) {
        return JansiNativePty.systemStreamWidth(stream);
    }
}
