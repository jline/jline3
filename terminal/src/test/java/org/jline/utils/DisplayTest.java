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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.jline.terminal.*;
import org.jline.terminal.impl.LineDisciplineTerminal;
import org.junit.jupiter.api.Test;

import static org.jline.utils.InfoCmp.Capability.enter_ca_mode;
import static org.jline.utils.InfoCmp.Capability.exit_ca_mode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayTest {

    @Test
    void i737() throws IOException {
        int rows = 10;
        int cols = 25;
        try (VirtualTerminal terminal = new VirtualTerminal("jline", "xterm", StandardCharsets.UTF_8, cols, rows)) {
            Attributes savedAttributes = terminal.enterRawMode();
            terminal.puts(enter_ca_mode);
            int height = terminal.getRows();

            Display display = new Display(terminal, true);
            display.resize(terminal);

            // Build Strings to displayed
            List<AttributedString> lines1 = new ArrayList<>();
            for (int i = 1; i < height + 1; i++) {
                lines1.add(new AttributedString(String.format("%03d: %s", i, "Chaine de test...")));
            }

            List<AttributedString> lines2 = new ArrayList<>();
            for (int i = 0; i < height; i++) {
                lines2.add(new AttributedString(String.format("%03d: %s", i, "Chaine de test...")));
            }

            display.update(lines1, 0);

            display.update(lines2, 0);

            long[] screen = terminal.dump();
            List<AttributedString> lines = new ArrayList<>();
            for (int r = 0; r < rows; r++) {
                AttributedStringBuilder sb = new AttributedStringBuilder();
                for (int i = 0; i < cols; i++) {
                    sb.append((char) screen[i + cols * r]);
                }
                lines.add(sb.toAttributedString());
            }
            assertEquals("009: Chaine de test...   ", lines.get(rows - 1).toString());

            terminal.setAttributes(savedAttributes);
            terminal.puts(exit_ca_mode);
        }
    }

    static class VirtualTerminal extends LineDisciplineTerminal {
        private final ScreenTerminal virtual;
        private final OutputStream masterInputOutput;

        VirtualTerminal(String name, String type, Charset encoding, int cols, int rows) throws IOException {
            super(name, type, new DelegateOutputStream(), encoding);
            setSize(Size.of(cols, rows));
            virtual = new ScreenTerminal(cols, rows);
            ((DelegateOutputStream) masterOutput).output = new MasterOutputStream();
            masterInputOutput = new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    VirtualTerminal.this.processInputByte(b);
                }
            };
        }

        long[] dump() {
            long[] screen = new long[size.getRows() * size.getColumns()];
            virtual.dump(screen, 0, 0, size.getRows(), size.getColumns(), null);
            return screen;
        }

        void resizeScreen(int cols, int rows) {
            virtual.setSize(cols, rows);
            setSize(Size.of(cols, rows));
        }

        void startCapture() {
            ((DelegateOutputStream) masterOutput).spy = new ByteArrayOutputStream();
        }

        byte[] stopCapture() {
            DelegateOutputStream dos = (DelegateOutputStream) masterOutput;
            byte[] data = dos.spy != null ? dos.spy.toByteArray() : new byte[0];
            dos.spy = null;
            return data;
        }

        private static class DelegateOutputStream extends OutputStream {
            OutputStream output;
            ByteArrayOutputStream spy;

            @Override
            public void write(int b) throws IOException {
                if (spy != null) spy.write(b);
                output.write(b);
            }

            @Override
            public void write(byte[] b) throws IOException {
                if (spy != null) spy.write(b);
                output.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                if (spy != null) spy.write(b, off, len);
                output.write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                output.flush();
            }

            @Override
            public void close() throws IOException {
                output.close();
            }
        }

        private class MasterOutputStream extends OutputStream {
            private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            private final CharsetDecoder decoder = encoding()
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE);

            @Override
            public synchronized void write(int b) {
                buffer.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                buffer.write(b, off, len);
            }

            @Override
            public synchronized void flush() throws IOException {
                int size = buffer.size();
                if (size > 0) {
                    CharBuffer out;
                    for (; ; ) {
                        out = CharBuffer.allocate(size);
                        ByteBuffer in = ByteBuffer.wrap(buffer.toByteArray());
                        CoderResult result = decoder.decode(in, out, false);
                        if (result.isOverflow()) {
                            size *= 2;
                        } else {
                            buffer.reset();
                            buffer.write(in.array(), in.arrayOffset(), in.remaining());
                            break;
                        }
                    }
                    if (out.position() > 0) {
                        out.flip();
                        virtual.write(out);
                        masterInputOutput.write(virtual.read().getBytes());
                    }
                }
            }

            @Override
            public void close() throws IOException {
                flush();
            }
        }
    }

    @Test
    void testIntraLineSkipOptimization() throws IOException {
        int rows = 3;
        int cols = 40;
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, cols, rows)) {
            terminal.enterRawMode();
            Display display = new Display(terminal, true);
            display.resize(Size.of(cols, rows));

            // Frame 1: all rows filled with 'a' in default style
            List<AttributedString> frame1 = new ArrayList<>();
            for (int r = 0; r < rows; r++) {
                AttributedStringBuilder sb = new AttributedStringBuilder();
                for (int c = 0; c < cols; c++) sb.append('a');
                sb.append('\n');
                frame1.add(sb.toAttributedString());
            }
            display.update(frame1, 0);
            terminal.flush();

            // Start capturing output for the second update
            terminal.startCapture();

            // Frame 2: row 1 has red 'X' at col 5 and col 33
            // (27 unchanged 'a' chars between them — well above the skip threshold)
            List<AttributedString> frame2 = new ArrayList<>(frame1);
            AttributedStringBuilder sb = new AttributedStringBuilder();
            for (int c = 0; c < cols; c++) {
                if (c == 5 || c == 33) {
                    sb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
                    sb.append('X');
                    sb.style(AttributedStyle.DEFAULT);
                } else {
                    sb.append('a');
                }
            }
            sb.append('\n');
            frame2.set(1, sb.toAttributedString());
            display.update(frame2, 0);
            terminal.flush();

            byte[] captured = terminal.stopCapture();
            String output = new String(captured, StandardCharsets.UTF_8);

            // Verify optimization: no long run of 'a' chars in the output
            // (the 27 unchanged chars should be skipped with cursor movement)
            int maxConsecutiveA = 0;
            int run = 0;
            for (int i = 0; i < output.length(); i++) {
                if (output.charAt(i) == 'a') {
                    run++;
                    if (run > maxConsecutiveA) maxConsecutiveA = run;
                } else {
                    run = 0;
                }
            }
            assertTrue(
                    maxConsecutiveA < 10,
                    "Expected cursor movement to skip unchanged gap, but found "
                            + maxConsecutiveA + " consecutive 'a' chars in output: "
                            + output.replace("\u001b", "\\e"));

            // Verify screen correctness
            long[] screen = terminal.dump();
            assertEquals('X', (char) screen[5 + cols * 1], "col 5 row 1 should be X");
            assertEquals('X', (char) screen[33 + cols * 1], "col 33 row 1 should be X");
            assertEquals('a', (char) screen[10 + cols * 1], "col 10 row 1 should be unchanged");
            assertEquals('a', (char) screen[0 + cols * 1], "col 0 row 1 should be unchanged");
            assertEquals('a', (char) screen[39 + cols * 1], "col 39 row 1 should be unchanged");
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {

        try (Terminal terminal = TerminalBuilder.builder().build()) {
            Attributes savedAttributes = terminal.enterRawMode();
            terminal.puts(enter_ca_mode);
            int height = terminal.getRows();

            Display display = new Display(terminal, true);
            display.resize(terminal);

            // Build Strings to displayed
            List<AttributedString> lines1 = new ArrayList<>();
            for (int i = 1; i < height + 1; i++) {
                lines1.add(new AttributedString(String.format("%03d: %s", i, "Chaine de test...")));
            }

            List<AttributedString> lines2 = new ArrayList<>();
            for (int i = 0; i < height; i++) {
                lines2.add(new AttributedString(String.format("%03d: %s", i, "Chaine de test...")));
            }

            // Display with tempo
            display.update(lines1, 0);
            Thread.sleep(3000);

            display.update(lines2, 0);
            Thread.sleep(3000);

            terminal.setAttributes(savedAttributes);
            terminal.puts(exit_ca_mode);
        }
    }
}
