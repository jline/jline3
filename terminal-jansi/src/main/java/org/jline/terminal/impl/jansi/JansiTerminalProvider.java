/*
 * Copyright (c) 2002-2020, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.jansi;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.internal.CLibrary;
import org.fusesource.jansi.internal.Kernel32;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.AbstractPty;
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

    public static void verifyAtLeast(int major, int minor) {
        if (!isAtLeast(major, minor)) {
            throw new UnsupportedOperationException("An old version of Jansi is loaded from "
                    + Kernel32.class
                            .getClassLoader()
                            .getResource(Kernel32.class.getName().replace('.', '/') + ".class"));
        }
    }

    public JansiTerminalProvider() {
        verifyAtLeast(1, 17);
        checkIsSystemStream(SystemStream.Output);
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
            throw new UnsupportedOperationException("Unsupported platform " + osName);
        } else if (osName.startsWith("FreeBSD")) {
            return FreeBsdNativePty.current(this, systemStream);
        } else {
            throw new UnsupportedOperationException("Unsupported platform " + osName);
        }
    }

    public Pty open(Attributes attributes, Size size) throws IOException {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Linux")) {
            ensureOpenPtyLoaded();
            return LinuxNativePty.open(this, attributes, size);
        } else if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
            ensureOpenPtyLoaded();
            return OsXNativePty.open(this, attributes, size);
        } else if (osName.startsWith("Solaris") || osName.startsWith("SunOS")) {
            // Solaris is not supported by jansi
            // return SolarisNativePty.current();
            throw new UnsupportedOperationException("Unsupported platform " + osName);
        } else if (osName.startsWith("FreeBSD")) {
            ensureOpenPtyLoaded();
            return FreeBsdNativePty.open(this, attributes, size);
        } else {
            throw new UnsupportedOperationException("Unsupported platform " + osName);
        }
    }

    static void ensureOpenPtyLoaded() {
        try {
            Process p = Runtime.getRuntime().exec(new String[] {"uname", "-m"});
            p.waitFor();
            try (InputStream in = p.getInputStream()) {
                String hwName = readFully(in).trim();
                Path libDir = Paths.get("/usr/lib", hwName + "-linux-gnu");
                try (Stream<Path> stream = Files.list(libDir)) {
                    List<Path> libs = stream.filter(
                                    l -> l.getFileName().toString().startsWith("libutil.so."))
                            .collect(Collectors.toList());
                    String lib = libs.iterator().next().toString();
                    System.err.println("Loading " + lib);
                    System.load(lib);
                }
            }

            int[] master = new int[1];
            int[] slave = new int[1];
            byte[] name = new byte[64];
            try {
                CLibrary.openpty(master, slave, name, null, null);
                new FileInputStream(AbstractPty.newDescriptor(master[0])).close();
                new FileInputStream(AbstractPty.newDescriptor(slave[0])).close();
            } catch (Throwable t) {
                t.printStackTrace();
                Class<?> cl = CLibrary.class;
            }
        } catch (Throwable t) {
            throw new LinkageError("Unable to load CLibrary for openpty", t);
        }
    }

    private static String readFully(InputStream in) throws IOException {
        int readLen = 0;
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[32];
        while ((readLen = in.read(buf, 0, buf.length)) >= 0) {
            b.write(buf, 0, readLen);
        }
        return b.toString();
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
        JansiWinSysTerminal terminal = JansiWinSysTerminal.createTerminal(
                this, systemStream, name, type, ansiPassThrough, encoding, nativeSignals, signalHandler, paused);
        terminal.disableScrolling();
        return terminal;
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
            return checkIsSystemStream(stream);
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean checkIsSystemStream(SystemStream stream) {
        if (OSUtils.IS_WINDOWS) {
            return JansiWinSysTerminal.isWindowsSystemStream(stream);
        } else {
            return JansiNativePty.isPosixSystemStream(stream);
        }
    }

    @Override
    public String systemStreamName(SystemStream stream) {
        return JansiNativePty.posixSystemStreamName(stream);
    }

    @Override
    public int systemStreamWidth(SystemStream stream) {
        return JansiNativePty.systemStreamWidth(stream);
    }

    @Override
    public String toString() {
        return "TerminalProvider[" + name() + "]";
    }
}
