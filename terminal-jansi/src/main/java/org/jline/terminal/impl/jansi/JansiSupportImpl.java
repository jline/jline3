/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.jansi;

import org.fusesource.jansi.Ansi;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.jansi.freebsd.FreeBsdNativePty;
import org.jline.terminal.impl.jansi.linux.LinuxNativePty;
import org.jline.terminal.impl.jansi.osx.OsXNativePty;
import org.jline.terminal.impl.jansi.win.JansiWinSysTerminal;
import org.jline.terminal.spi.JansiSupport;
import org.jline.terminal.spi.Pty;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JansiSupportImpl implements JansiSupport {

    static final int JANSI_MAJOR_VERSION;
    static final int JANSI_MINOR_VERSION;
    static {
        int major = 0, minor = 0;
        try {
            String v = null;
            try (InputStream is = Ansi.class.getResourceAsStream("jansi.properties")) {
                if (is != null) {
                    Properties props = new Properties();
                    props.load(is);
                    v = props.getProperty("version");
                }
            } catch (IOException e) {
                // ignore
            }
            if (v == null) {
                v = Ansi.class.getPackage().getImplementationVersion();
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
    public Pty current() throws IOException {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Linux")) {
            if (isAtLeast(1, 16)) {
                return LinuxNativePty.current();
            }
        }
        else if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
            if (isAtLeast(1, 12)) {
                return OsXNativePty.current();
            }
        }
        else if (osName.startsWith("Solaris") || osName.startsWith("SunOS")) {
            // Solaris is not supported by jansi
            // return SolarisNativePty.current();
        }
        else if (osName.startsWith("FreeBSD")) {
            if (isAtLeast(1, 16)) {
                return FreeBsdNativePty.current();
            }
        }
        throw new UnsupportedOperationException();
    }

    @Override
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
    public Terminal winSysTerminal(String name, String type, boolean ansiPassThrough, Charset encoding, int codepage, boolean nativeSignals, Terminal.SignalHandler signalHandler) throws IOException {
        return winSysTerminal(name, type, ansiPassThrough, encoding, codepage, nativeSignals, signalHandler, false);
    }

    @Override
    public Terminal winSysTerminal(String name, String type, boolean ansiPassThrough, Charset encoding, int codepage, boolean nativeSignals, Terminal.SignalHandler signalHandler, boolean paused) throws IOException {
        if (isAtLeast(1, 12)) {
            JansiWinSysTerminal terminal = JansiWinSysTerminal.createTerminal(name, type, ansiPassThrough, encoding, codepage, nativeSignals, signalHandler, paused);
            if (!isAtLeast(1, 16)) {
                terminal.disableScrolling();
            }
            return terminal;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWindowsConsole() {
        return JansiWinSysTerminal.isWindowsConsole();
    }

}
