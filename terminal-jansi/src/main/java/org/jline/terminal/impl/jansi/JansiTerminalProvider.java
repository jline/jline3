/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.jansi;

import org.fusesource.jansi.AnsiConsole;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.PosixPtyTerminal;
import org.jline.terminal.impl.PosixSysTerminal;
import org.jline.terminal.impl.jansi.freebsd.FreeBsdNativePty;
import org.jline.terminal.impl.jansi.linux.LinuxNativePty;
import org.jline.terminal.impl.jansi.osx.OsXNativePty;
import org.jline.terminal.impl.jansi.win.JansiWinSysTerminal;
import org.jline.terminal.spi.Pty;
import org.jline.terminal.spi.TerminalProvider;
import org.jline.utils.OSUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        return "jansi";
    }

    public Pty current(Stream consoleStream) throws IOException {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Linux")) {
            return LinuxNativePty.current(consoleStream);
        }
        else if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
            return OsXNativePty.current(consoleStream);
        }
        else if (osName.startsWith("Solaris") || osName.startsWith("SunOS")) {
            // Solaris is not supported by jansi
            // return SolarisNativePty.current();
        }
        else if (osName.startsWith("FreeBSD")) {
            if (isAtLeast(1, 16)) {
                return FreeBsdNativePty.current(consoleStream);
            }
        }
        throw new UnsupportedOperationException();
    }

    public Pty open(Attributes attributes, Size size) throws IOException {
        if (isAtLeast(1, 16)) {
            String osName = System.getProperty("os.name");
            if (osName.startsWith("Linux")) {
                return LinuxNativePty.open(attributes, size);
            }
            else if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
                return OsXNativePty.open(attributes, size);
            }
            else if (osName.startsWith("Solaris") || osName.startsWith("SunOS")) {
                // Solaris is not supported by jansi
                // return SolarisNativePty.current();
            }
            else if (osName.startsWith("FreeBSD")) {
                return FreeBsdNativePty.open(attributes, size);
            }
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public Terminal sysTerminal(String name, String type, boolean ansiPassThrough, Charset encoding,
                                boolean nativeSignals, Terminal.SignalHandler signalHandler, boolean paused,
                                Stream consoleStream) throws IOException {
        if (OSUtils.IS_WINDOWS) {
            return winSysTerminal(name, type, ansiPassThrough, encoding, nativeSignals, signalHandler, paused, consoleStream );
        } else {
            return posixSysTerminal(name, type, ansiPassThrough, encoding, nativeSignals, signalHandler, paused, consoleStream );
        }
    }

    public Terminal winSysTerminal(String name, String type, boolean ansiPassThrough,
                                   Charset encoding, boolean nativeSignals,
                                   Terminal.SignalHandler signalHandler, boolean paused,
                                   Stream consoleStream) throws IOException {
        if (isAtLeast(1, 12)) {
            JansiWinSysTerminal terminal = JansiWinSysTerminal.createTerminal(name, type, ansiPassThrough, encoding,
                    nativeSignals, signalHandler, paused, consoleStream);
            if (!isAtLeast(1, 16)) {
                terminal.disableScrolling();
            }
            return terminal;
        }
        throw new UnsupportedOperationException();
    }

    public Terminal posixSysTerminal(String name, String type, boolean ansiPassThrough, Charset encoding,
                                     boolean nativeSignals, Terminal.SignalHandler signalHandler, boolean paused,
                                     Stream consoleStream) throws IOException {
        Pty pty = current(consoleStream);
        return new PosixSysTerminal(name, type, pty, encoding, nativeSignals, signalHandler);
    }

    @Override
    public Terminal newTerminal(String name, String type, InputStream in, OutputStream out,
                                Charset encoding, Terminal.SignalHandler signalHandler, boolean paused,
                                Attributes attributes, Size size) throws IOException
    {
        Pty pty = open(attributes, size);
        return new PosixPtyTerminal(name, type, pty, in, out, encoding, signalHandler, paused);
    }

    @Override
    public boolean isSystemStream(Stream stream) {
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

    public boolean isWindowsSystemStream(Stream stream) {
        return JansiWinSysTerminal.isWindowsSystemStream(stream);
    }

    public boolean isPosixSystemStream(Stream stream) {
        return JansiNativePty.isPosixSystemStream(stream);
    }

    @Override
    public String systemStreamName(Stream stream) {
        return JansiNativePty.posixSystemStreamName(stream);
    }

}
