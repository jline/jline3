/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.ffm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.impl.TermiosData;
import org.jline.terminal.impl.TermiosMapping;
import org.jline.terminal.spi.Pty;
import org.jline.terminal.spi.TerminalProvider;
import org.jline.utils.OSUtils;

@SuppressWarnings("restricted")
class CLibrary {

    private static final Logger logger = Logger.getLogger("org.jline");

    // Window sizes.
    // @see <a href="http://man7.org/linux/man-pages/man4/tty_ioctl.4.html">IOCTL_TTY(2) man-page</a>
    static class winsize {
        static final GroupLayout LAYOUT;
        private static final VarHandle ws_col;
        private static final VarHandle ws_row;

        static {
            LAYOUT = MemoryLayout.structLayout(
                    ValueLayout.JAVA_SHORT.withName("ws_row"),
                    ValueLayout.JAVA_SHORT.withName("ws_col"),
                    ValueLayout.JAVA_SHORT,
                    ValueLayout.JAVA_SHORT);
            ws_row = FfmTerminalProvider.lookupVarHandle(LAYOUT, MemoryLayout.PathElement.groupElement("ws_row"));
            ws_col = FfmTerminalProvider.lookupVarHandle(LAYOUT, MemoryLayout.PathElement.groupElement("ws_col"));
        }

        private final MemorySegment seg;

        winsize() {
            seg = Arena.ofAuto().allocate(LAYOUT);
        }

        winsize(short ws_col, short ws_row) {
            this();
            ws_col(ws_col);
            ws_row(ws_row);
        }

        MemorySegment segment() {
            return seg;
        }

        short ws_col() {
            return (short) ws_col.get(seg);
        }

        void ws_col(short col) {
            ws_col.set(seg, col);
        }

        short ws_row() {
            return (short) ws_row.get(seg);
        }

        void ws_row(short row) {
            ws_row.set(seg, row);
        }
    }

    // termios structure for termios functions, describing a general terminal interface that is
    // provided to control asynchronous communications ports
    // @see <a href="http://man7.org/linux/man-pages/man3/termios.3.html">TERMIOS(3) man-page</a>
    static class termios {
        static final GroupLayout LAYOUT;
        private static final VarHandle c_iflag;
        private static final VarHandle c_oflag;
        private static final VarHandle c_cflag;
        private static final VarHandle c_lflag;
        private static final long c_cc_offset;
        private static final VarHandle c_ispeed;
        private static final VarHandle c_ospeed;

        static {
            if (OSUtils.IS_OSX) {
                LAYOUT = MemoryLayout.structLayout(
                        ValueLayout.JAVA_LONG.withName("c_iflag"),
                        ValueLayout.JAVA_LONG.withName("c_oflag"),
                        ValueLayout.JAVA_LONG.withName("c_cflag"),
                        ValueLayout.JAVA_LONG.withName("c_lflag"),
                        MemoryLayout.sequenceLayout(32, ValueLayout.JAVA_BYTE).withName("c_cc"),
                        ValueLayout.JAVA_LONG.withName("c_ispeed"),
                        ValueLayout.JAVA_LONG.withName("c_ospeed"));
            } else if (OSUtils.IS_LINUX) {
                LAYOUT = MemoryLayout.structLayout(
                        ValueLayout.JAVA_INT.withName("c_iflag"),
                        ValueLayout.JAVA_INT.withName("c_oflag"),
                        ValueLayout.JAVA_INT.withName("c_cflag"),
                        ValueLayout.JAVA_INT.withName("c_lflag"),
                        ValueLayout.JAVA_BYTE.withName("c_line"),
                        MemoryLayout.sequenceLayout(32, ValueLayout.JAVA_BYTE).withName("c_cc"),
                        MemoryLayout.paddingLayout(3),
                        ValueLayout.JAVA_INT.withName("c_ispeed"),
                        ValueLayout.JAVA_INT.withName("c_ospeed"));
            } else {
                throw new IllegalStateException("Unsupported system!");
            }
            c_iflag = adjust2LinuxHandle(
                    FfmTerminalProvider.lookupVarHandle(LAYOUT, MemoryLayout.PathElement.groupElement("c_iflag")));
            c_oflag = adjust2LinuxHandle(
                    FfmTerminalProvider.lookupVarHandle(LAYOUT, MemoryLayout.PathElement.groupElement("c_oflag")));
            c_cflag = adjust2LinuxHandle(
                    FfmTerminalProvider.lookupVarHandle(LAYOUT, MemoryLayout.PathElement.groupElement("c_cflag")));
            c_lflag = adjust2LinuxHandle(
                    FfmTerminalProvider.lookupVarHandle(LAYOUT, MemoryLayout.PathElement.groupElement("c_lflag")));
            c_cc_offset = LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("c_cc"));
            c_ispeed = adjust2LinuxHandle(
                    FfmTerminalProvider.lookupVarHandle(LAYOUT, MemoryLayout.PathElement.groupElement("c_ispeed")));
            c_ospeed = adjust2LinuxHandle(
                    FfmTerminalProvider.lookupVarHandle(LAYOUT, MemoryLayout.PathElement.groupElement("c_ospeed")));
        }

        private static VarHandle adjust2LinuxHandle(VarHandle v) {
            if (OSUtils.IS_LINUX) {
                MethodHandle id = MethodHandles.identity(int.class);
                v = MethodHandles.filterValue(
                        v,
                        MethodHandles.explicitCastArguments(id, MethodType.methodType(int.class, long.class)),
                        MethodHandles.explicitCastArguments(id, MethodType.methodType(long.class, int.class)));
            }

            return v;
        }

        private final MemorySegment seg;

        termios() {
            seg = Arena.ofAuto().allocate(LAYOUT);
        }

        termios(Attributes t) {
            this();
            TermiosData data = TermiosMapping.forCurrentPlatform().toTermios(t);
            c_iflag(data.iflag());
            c_oflag(data.oflag());
            c_cflag(data.cflag());
            c_lflag(data.lflag());
            c_cc().copyFrom(MemorySegment.ofArray(data.cc()).asSlice(0, 20));
        }

        MemorySegment segment() {
            return seg;
        }

        long c_iflag() {
            return (long) c_iflag.get(seg);
        }

        void c_iflag(long f) {
            c_iflag.set(seg, f);
        }

        long c_oflag() {
            return (long) c_oflag.get(seg);
        }

        void c_oflag(long f) {
            c_oflag.set(seg, f);
        }

        long c_cflag() {
            return (long) c_cflag.get(seg);
        }

        void c_cflag(long f) {
            c_cflag.set(seg, f);
        }

        long c_lflag() {
            return (long) c_lflag.get(seg);
        }

        void c_lflag(long f) {
            c_lflag.set(seg, f);
        }

        MemorySegment c_cc() {
            return seg.asSlice(c_cc_offset, 20);
        }

        long c_ispeed() {
            return (long) c_ispeed.get(seg);
        }

        void c_ispeed(long f) {
            c_ispeed.set(seg, f);
        }

        long c_ospeed() {
            return (long) c_ospeed.get(seg);
        }

        void c_ospeed(long f) {
            c_ospeed.set(seg, f);
        }

        /**
         * Converts this native termios structure to JLine {@link Attributes}
         * using the platform-specific {@link TermiosMapping}.
         *
         * @return a new {@link Attributes} instance reflecting the current terminal settings
         */
        public Attributes asAttributes() {
            TermiosData data = new TermiosData();
            data.iflag(c_iflag());
            data.oflag(c_oflag());
            data.cflag(c_cflag());
            data.lflag(c_lflag());
            byte[] cc = c_cc().toArray(ValueLayout.JAVA_BYTE);
            System.arraycopy(cc, 0, data.cc(), 0, cc.length);
            return TermiosMapping.forCurrentPlatform().toAttributes(data);
        }
    }

    static final MethodHandle ioctl;
    static final MethodHandle isatty;
    static final MethodHandle openptyHandle;
    static final MethodHandle tcsetattr;
    static final MethodHandle tcgetattr;
    static final MethodHandle ttyname_r;
    static LinkageError openptyError;

    static {
        // methods
        Linker linker = Linker.nativeLinker();
        SymbolLookup lookup = SymbolLookup.loaderLookup().or(linker.defaultLookup());
        // https://man7.org/linux/man-pages/man2/ioctl.2.html
        ioctl = linker.downcallHandle(
                lookup.find("ioctl").get(),
                FunctionDescriptor.of(
                        ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                Linker.Option.firstVariadicArg(2));
        // https://www.man7.org/linux/man-pages/man3/isatty.3.html
        isatty = linker.downcallHandle(
                lookup.find("isatty").get(), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        // https://man7.org/linux/man-pages/man3/tcsetattr.3p.html
        tcsetattr = linker.downcallHandle(
                lookup.find("tcsetattr").get(),
                FunctionDescriptor.of(
                        ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        // https://man7.org/linux/man-pages/man3/tcgetattr.3p.html
        tcgetattr = linker.downcallHandle(
                lookup.find("tcgetattr").get(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        // https://man7.org/linux/man-pages/man3/ttyname.3.html
        ttyname_r = linker.downcallHandle(
                lookup.find("ttyname_r").get(),
                FunctionDescriptor.of(
                        ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        // https://man7.org/linux/man-pages/man3/openpty.3.html
        LinkageError error = null;
        Optional<MemorySegment> openPtyAddr = lookup.find("openpty");
        if (openPtyAddr.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Unable to find openpty native method in static libraries and unable to load the util library.");
            List<Throwable> suppressed = new ArrayList<>();
            try {
                System.loadLibrary("util");
                openPtyAddr = lookup.find("openpty");
            } catch (Throwable t) {
                suppressed.add(t);
            }
            if (openPtyAddr.isEmpty()) {
                String libUtilPath = System.getProperty("org.jline.ffm.libutil");
                if (libUtilPath != null && !libUtilPath.isEmpty()) {
                    try {
                        System.load(libUtilPath);
                        openPtyAddr = lookup.find("openpty");
                    } catch (Throwable t) {
                        suppressed.add(t);
                    }
                }
            }
            if (openPtyAddr.isEmpty() && OSUtils.IS_LINUX) {
                String hwName;
                try {
                    Process p = Runtime.getRuntime().exec(new String[] {"uname", "-m"});
                    p.waitFor();
                    try (InputStream in = p.getInputStream()) {
                        hwName = readFully(in).trim();
                        Path libDir = Path.of("/usr/lib", hwName + "-linux-gnu");
                        try (Stream<Path> stream = Files.list(libDir)) {
                            List<Path> libs = stream.filter(
                                            l -> l.getFileName().toString().startsWith("libutil.so."))
                                    .collect(Collectors.toList());
                            for (Path lib : libs) {
                                try {
                                    System.load(lib.toString());
                                    openPtyAddr = lookup.find("openpty");
                                    if (openPtyAddr.isPresent()) {
                                        break;
                                    }
                                } catch (Throwable t) {
                                    suppressed.add(t);
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    suppressed.add(t);
                }
            }
            if (openPtyAddr.isEmpty()) {
                for (Throwable t : suppressed) {
                    sb.append("\n\t- ").append(t.toString());
                }
                error = new LinkageError(sb.toString());
                suppressed.forEach(error::addSuppressed);
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.WARNING, error.getMessage(), error);
                } else {
                    logger.log(Level.WARNING, error.getMessage());
                }
            }
        }
        if (openPtyAddr.isPresent()) {
            openptyHandle = linker.downcallHandle(
                    openPtyAddr.get(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS));
            openptyError = null;
        } else {
            openptyHandle = null;
            openptyError = error;
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

    static Size getTerminalSize(int fd) {
        try {
            winsize ws = new winsize();
            int res = (int) ioctl.invoke(fd, (long) TIOCGWINSZ, ws.segment());
            return new Size(ws.ws_col(), ws.ws_row());
        } catch (Throwable e) {
            throw new RuntimeException("Unable to call ioctl(TIOCGWINSZ)", e);
        }
    }

    static void setTerminalSize(int fd, Size size) {
        try {
            winsize ws = new winsize();
            ws.ws_row((short) size.getRows());
            ws.ws_col((short) size.getColumns());
            int res = (int) ioctl.invoke(fd, TIOCSWINSZ, ws.segment());
        } catch (Throwable e) {
            throw new RuntimeException("Unable to call ioctl(TIOCSWINSZ)", e);
        }
    }

    static Attributes getAttributes(int fd) {
        try {
            termios t = new termios();
            int res = (int) tcgetattr.invoke(fd, t.segment());
            return t.asAttributes();
        } catch (Throwable e) {
            throw new RuntimeException("Unable to call tcgetattr()", e);
        }
    }

    static void setAttributes(int fd, Attributes attr) {
        try {
            termios t = new termios(attr);
            int res = (int) tcsetattr.invoke(fd, TermiosData.TCSANOW, t.segment());
        } catch (Throwable e) {
            throw new RuntimeException("Unable to call tcsetattr()", e);
        }
    }

    static boolean isTty(int fd) {
        try {
            return (int) isatty.invoke(fd) == 1;
        } catch (Throwable e) {
            throw new RuntimeException("Unable to call isatty()", e);
        }
    }

    static String ttyName(int fd) {
        try {
            MemorySegment buf = Arena.ofAuto().allocate(64);
            int res = (int) ttyname_r.invoke(fd, buf, buf.byteSize());
            byte[] data = buf.toArray(ValueLayout.JAVA_BYTE);
            int len = 0;
            while (data[len] != 0) {
                len++;
            }
            return new String(data, 0, len);
        } catch (Throwable e) {
            throw new RuntimeException("Unable to call ttyname_r()", e);
        }
    }

    static Pty openpty(TerminalProvider provider, Attributes attr, Size size) {
        if (openptyError != null) {
            throw openptyError;
        }
        try {
            MemorySegment buf = Arena.ofAuto().allocate(64);
            MemorySegment master = Arena.ofAuto().allocate(ValueLayout.JAVA_INT);
            MemorySegment slave = Arena.ofAuto().allocate(ValueLayout.JAVA_INT);
            int res = (int) openptyHandle.invoke(
                    master,
                    slave,
                    buf,
                    attr != null ? new termios(attr).segment() : MemorySegment.NULL,
                    size != null
                            ? new winsize((short) size.getRows(), (short) size.getColumns()).segment()
                            : MemorySegment.NULL);
            if (res != 0) {
                throw new UncheckedIOException(new IOException("Unable to call openpty(): return code " + res));
            }
            byte[] str = buf.toArray(ValueLayout.JAVA_BYTE);
            int len = 0;
            while (str[len] != 0) {
                len++;
            }
            String device = new String(str, 0, len);
            return new FfmNativePty(
                    provider, null, master.get(ValueLayout.JAVA_INT, 0), slave.get(ValueLayout.JAVA_INT, 0), device);
        } catch (UncheckedIOException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException("Unable to call openpty()", e);
        }
    }

    // CONSTANTS

    private static final int TIOCGWINSZ;
    private static final int TIOCSWINSZ;

    static {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Linux")) {
            String arch = System.getProperty("os.arch");
            boolean isMipsPpcOrSparc = arch.equals("mips")
                    || arch.equals("mips64")
                    || arch.equals("mipsel")
                    || arch.equals("mips64el")
                    || arch.startsWith("ppc")
                    || arch.startsWith("sparc");
            TIOCGWINSZ = isMipsPpcOrSparc ? 0x40087468 : 0x00005413;
            TIOCSWINSZ = isMipsPpcOrSparc ? 0x80087467 : 0x00005414;
        } else if (osName.startsWith("Solaris") || osName.startsWith("SunOS")) {
            int _TIOC = ('T' << 8);
            TIOCGWINSZ = (_TIOC | 104);
            TIOCSWINSZ = (_TIOC | 103);
        } else if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
            TIOCGWINSZ = 0x40087468;
            TIOCSWINSZ = 0x80087467;
        } else if (osName.startsWith("FreeBSD")) {
            TIOCGWINSZ = 0x40087468;
            TIOCSWINSZ = 0x80087467;
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
