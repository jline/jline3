/*
 * Copyright (c) 2009-2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.nativ;

/**
 * Interface to access some low level POSIX functions,.
 *
 * @see JLineNativeLoader
 */
@SuppressWarnings("unused")
public class CLibrary {

    /**
     * Private constructor to prevent instantiation.
     */
    private CLibrary() {
        // Utility class
    }

    //
    // Initialization
    //

    static {
        if (JLineNativeLoader.initialize()) {
            init();
        }
    }

    private static native void init();

    //
    // Constants
    //

    public static int TCSANOW;
    public static int TCSADRAIN;
    public static int TCSAFLUSH;
    public static long TIOCGWINSZ;
    public static long TIOCSWINSZ;

    /**
     * test whether a file descriptor refers to a terminal
     *
     * @param fd file descriptor
     * @return isatty() returns 1 if fd is an open file descriptor referring to a
     * terminal; otherwise 0 is returned, and errno is set to indicate the
     * error
     * @see <a href="http://man7.org/linux/man-pages/man3/isatty.3.html">ISATTY(3) man-page</a>
     * @see <a href="http://man7.org/linux/man-pages/man3/isatty.3p.html">ISATTY(3P) man-page</a>
     */
    public static native int isatty(int fd);

    public static native String ttyname(int filedes);

    /**
     * The openpty() function finds an available pseudoterminal and returns
     * file descriptors for the master and slave in amaster and aslave.
     *
     * @param amaster master return value
     * @param aslave  slave return value
     * @param name    filename return value
     * @param termios optional pty attributes
     * @param winsize optional size
     * @return 0 on success
     * @see <a href="http://man7.org/linux/man-pages/man3/openpty.3.html">OPENPTY(3) man-page</a>
     */
    public static native int openpty(int[] amaster, int[] aslave, byte[] name, Termios termios, WinSize winsize);

    public static native int tcgetattr(int filedes, Termios termios);

    public static native int tcsetattr(int filedes, int optional_actions, Termios termios);

    /**
     * Control a STREAMS device.
     *
     * @see <a href="http://man7.org/linux/man-pages/man3/ioctl.3p.html">IOCTL(3P) man-page</a>
     */
    public static native int ioctl(int filedes, long request, int[] params);

    public static native int ioctl(int filedes, long request, WinSize params);

    public static short getTerminalWidth(int fd) {
        WinSize sz = new WinSize();
        ioctl(fd, TIOCGWINSZ, sz);
        return sz.ws_col;
    }

    /**
     * Window sizes.
     *
     * @see <a href="http://man7.org/linux/man-pages/man4/tty_ioctl.4.html">IOCTL_TTY(2) man-page</a>
     */
    public static class WinSize {

        static {
            if (JLineNativeLoader.initialize()) {
                init();
            }
        }

        private static native void init();

        public static int SIZEOF;

        public short ws_row;
        public short ws_col;
        public short ws_xpixel;
        public short ws_ypixel;

        public WinSize() {}

        public WinSize(short ws_row, short ws_col) {
            this.ws_row = ws_row;
            this.ws_col = ws_col;
        }
    }

    /**
     * termios structure for termios functions, describing a general terminal interface that is
     * provided to control asynchronous communications ports
     *
     * @see <a href="http://man7.org/linux/man-pages/man3/termios.3.html">TERMIOS(3) man-page</a>
     */
    public static class Termios {

        /**
         * Default constructor.
         */
        public Termios() {
            // Default constructor
        }

        static {
            if (JLineNativeLoader.initialize()) {
                init();
            }
        }

        private static native void init();

        public static int SIZEOF;

        public long c_iflag;
        public long c_oflag;
        public long c_cflag;
        public long c_lflag;
        public byte[] c_cc = new byte[32];
        public long c_ispeed;
        public long c_ospeed;
    }
}
