/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.jline.terminal.Size;
import org.jline.terminal.impl.LineDisciplineTerminal;

public class VirtualTerminal extends LineDisciplineTerminal {
    private final ScreenTerminal virtual;

    public VirtualTerminal(int cols, int rows) throws IOException {
        this("jline", "xterm", StandardCharsets.UTF_8, cols, rows);
    }

    @SuppressWarnings("this-escape")
    public VirtualTerminal(String name, String type, Charset encoding, int cols, int rows) throws IOException {
        super(name, type, new SpyDelegateOutputStream(), encoding);
        setSize(Size.of(cols, rows));
        boolean xenl = getBooleanCapability(InfoCmp.Capability.eat_newline_glitch);
        virtual = new ScreenTerminal(cols, rows, xenl);
        OutputStream feedbackOutput = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                VirtualTerminal.this.processInputByte(b);
            }
        };
        ((SpyDelegateOutputStream) masterOutput)
                .setDelegate(new ScreenTerminalOutputStream(virtual, encoding, feedbackOutput));
    }

    public String screenContent() {
        return virtual.toString();
    }

    public long[] dump() {
        long[] screen = new long[size.getRows() * size.getColumns()];
        virtual.dump(screen, 0, 0, size.getRows(), size.getColumns(), null);
        return screen;
    }

    public void resizeScreen(int cols, int rows) {
        virtual.setSize(Size.of(cols, rows));
        setSize(Size.of(cols, rows));
    }

    public void startCapture() {
        ((SpyDelegateOutputStream) masterOutput).spy = new ByteArrayOutputStream();
    }

    public byte[] stopCapture() {
        SpyDelegateOutputStream dos = (SpyDelegateOutputStream) masterOutput;
        byte[] data = dos.spy != null ? dos.spy.toByteArray() : new byte[0];
        dos.spy = null;
        return data;
    }

    private static class SpyDelegateOutputStream extends ScreenTerminalOutputStream.DelegateOutputStream {
        ByteArrayOutputStream spy;

        @Override
        public void write(int b) throws IOException {
            if (spy != null) spy.write(b);
            super.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (spy != null) spy.write(b, off, len);
            super.write(b, off, len);
        }
    }
}
