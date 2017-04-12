/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.terminal.impl.jansi;

import org.jline.terminal.Terminal;
import org.jline.terminal.impl.jansi.freebsd.FreeBsdNativePty;
import org.jline.terminal.impl.jansi.linux.LinuxNativePty;
import org.jline.terminal.impl.jansi.osx.OsXNativePty;
import org.jline.terminal.impl.jansi.solaris.SolarisNativePty;
import org.jline.terminal.impl.jansi.win.JansiWinSysTerminal;
import org.jline.terminal.spi.JansiSupport;
import org.jline.terminal.spi.Pty;

import java.io.IOException;

public class JansiSupportImpl implements JansiSupport {

    @Override
    public Pty current() throws IOException {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Linux")) {
            // This leads to java.lang.UnsatisfiedLinkError: org.fusesource.jansi.internal.CLibrary.ioctl(IJLorg/fusesource/jansi/internal/CLibrary$WinSize;)I
            // so disable it until jansi is fixed, see #112
            // return LinuxNativePty.current();
        }
        else if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
            return OsXNativePty.current();
        }
        else if (osName.startsWith("Solaris") || osName.startsWith("SunOS")) {
            // Solaris is not supported by jansi
            // return SolarisNativePty.current();
        }
        else if (osName.startsWith("FreeBSD")) {
            // FreeBSD is not supported by jansi
            // return FreeBsdNativePty.current();
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public Terminal winSysTerminal(String name, boolean nativeSignals, Terminal.SignalHandler signalHandler) throws IOException {
            return new JansiWinSysTerminal(name, nativeSignals, signalHandler);
    }

}
