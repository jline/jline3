/*
 * Copyright (c) 2009-2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.jansi;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.jline.jansi.io.AnsiOutputStream;

/**
 * Simple PrintStream holding an AnsiOutputStream.
 * This allows changing the mode in which the underlying AnsiOutputStream operates.
 */
public class AnsiPrintStream extends PrintStream {

    public AnsiPrintStream(AnsiOutputStream out, boolean autoFlush) {
        super(out, autoFlush);
    }

    public AnsiPrintStream(AnsiOutputStream out, boolean autoFlush, String encoding)
            throws UnsupportedEncodingException {
        super(out, autoFlush, encoding);
    }

    protected AnsiOutputStream getOut() {
        return (AnsiOutputStream) out;
    }

    public AnsiType getType() {
        return getOut().getType();
    }

    public AnsiColors getColors() {
        return getOut().getColors();
    }

    public AnsiMode getMode() {
        return getOut().getMode();
    }

    public void setMode(AnsiMode ansiMode) {
        getOut().setMode(ansiMode);
    }

    public boolean isResetAtUninstall() {
        return getOut().isResetAtUninstall();
    }

    public void setResetAtUninstall(boolean resetAtClose) {
        getOut().setResetAtUninstall(resetAtClose);
    }

    /**
     * Returns the width of the terminal associated with this stream or 0.
     * @since 2.2
     */
    public int getTerminalWidth() {
        return getOut().getTerminalWidth();
    }

    public void install() throws IOException {
        getOut().install();
    }

    public void uninstall() throws IOException {
        // If the system output stream has been closed, out should be null, so avoid a NPE
        AnsiOutputStream out = getOut();
        if (out != null) {
            out.uninstall();
        }
    }

    @Override
    public String toString() {
        return "AnsiPrintStream{"
                + "type=" + getType()
                + ", colors=" + getColors()
                + ", mode=" + getMode()
                + ", resetAtUninstall=" + isResetAtUninstall()
                + "}";
    }
}
