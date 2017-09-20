/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
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
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JansiSupportImpl implements JansiSupport {

    static final int JANSI_MAJOR_VERSION;
    static final int JANSI_MINOR_VERSION;
    static {
        int major = 0, minor = 0;
        try {
            String v = Ansi.class.getPackage().getImplementationVersion();
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

    @Override
    public Pty current() throws IOException {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Linux")) {
            if (JANSI_MAJOR_VERSION > 1 || JANSI_MAJOR_VERSION == 1 && JANSI_MINOR_VERSION >= 16) {
                return LinuxNativePty.current();
            }
        }
        else if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
            if (JANSI_MAJOR_VERSION > 1 || JANSI_MAJOR_VERSION == 1 && JANSI_MINOR_VERSION >= 12) {
                return OsXNativePty.current();
            }
        }
        else if (osName.startsWith("Solaris") || osName.startsWith("SunOS")) {
            // Solaris is not supported by jansi
            // return SolarisNativePty.current();
        }
        else if (osName.startsWith("FreeBSD")) {
            if (JANSI_MAJOR_VERSION > 1 || JANSI_MAJOR_VERSION == 1 && JANSI_MINOR_VERSION >= 16) {
                return FreeBsdNativePty.current();
            }
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public Pty open(Attributes attributes, Size size) throws IOException {
        if (JANSI_MAJOR_VERSION > 1 || JANSI_MAJOR_VERSION == 1 && JANSI_MINOR_VERSION >= 16) {
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
    public Terminal winSysTerminal(String name, Charset encoding, int codepage, boolean nativeSignals, Terminal.SignalHandler signalHandler) throws IOException {
        if (JANSI_MAJOR_VERSION > 1 || JANSI_MAJOR_VERSION == 1 && JANSI_MINOR_VERSION >= 12) {
            JansiWinSysTerminal terminal = new JansiWinSysTerminal(name, encoding, codepage, nativeSignals, signalHandler);
            if (JANSI_MAJOR_VERSION == 1 && JANSI_MINOR_VERSION < 16) {
                terminal.disableScrolling();
            }
            return terminal;
        }
        throw new UnsupportedOperationException();
    }

}
