/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

/**
 * Platform-independent holder for POSIX termios fields.
 *
 * <p>Mirrors the native {@code struct termios} layout: four flag words and a control-character
 * array. Used as an intermediate representation between JLine {@link org.jline.terminal.Attributes}
 * and the actual native structure provided by JNI or FFM.</p>
 */
public class TermiosData {

    /** Creates a new instance with all flags zeroed and all control characters set to 0. */
    public TermiosData() {}

    /** Maximum {@code c_cc} array size across supported platforms (Linux NCCS=32, macOS/FreeBSD NCCS=20). */
    public static final int NCCS = 32;

    /** Apply attribute changes immediately ({@code tcsetattr} action). Identical (0) on all supported platforms. */
    public static final int TCSANOW = 0;

    private long iflag;
    private long oflag;
    private long cflag;
    private long lflag;
    private final byte[] cc = new byte[NCCS];

    /** Returns the input mode flags. */
    public long iflag() {
        return iflag;
    }

    /** Sets the input mode flags. */
    public void iflag(long value) {
        this.iflag = value;
    }

    /** Returns the output mode flags. */
    public long oflag() {
        return oflag;
    }

    /** Sets the output mode flags. */
    public void oflag(long value) {
        this.oflag = value;
    }

    /** Returns the control mode flags. */
    public long cflag() {
        return cflag;
    }

    /** Sets the control mode flags. */
    public void cflag(long value) {
        this.cflag = value;
    }

    /** Returns the local mode flags. */
    public long lflag() {
        return lflag;
    }

    /** Sets the local mode flags. */
    public void lflag(long value) {
        this.lflag = value;
    }

    /** Returns the control characters array. */
    public byte[] cc() {
        return cc;
    }
}
