/*
 * Copyright (c) 2021-2025, the original author(s).
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

import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.LineDisciplineTerminal;
import org.junit.jupiter.api.Test;

import static org.jline.utils.InfoCmp.Capability.enter_ca_mode;
import static org.jline.utils.InfoCmp.Capability.exit_ca_mode;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DisplayTest {

    @Test
    public void i737() throws IOException {
        int rows = 10;
        int cols = 25;
        try (VirtualTerminal terminal = new VirtualTerminal("jline", "xterm", StandardCharsets.UTF_8, cols, rows)) {
            Attributes savedAttributes = terminal.enterRawMode();
            terminal.puts(enter_ca_mode);
            int height = terminal.getHeight();

            Display display = new Display(terminal, true);
            display.resize(height, terminal.getWidth());

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

        public VirtualTerminal(String name, String type, Charset encoding, int cols, int rows) throws IOException {
            super(name, type, new DelegateOutputStream(), encoding);
            setSize(new Size(cols, rows));
            virtual = new ScreenTerminal(cols, rows);
            ((DelegateOutputStream) masterOutput).output = new MasterOutputStream();
            masterInputOutput = new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    VirtualTerminal.this.processInputByte(b);
                }
            };
        }

        public long[] dump() {
            long[] screen = new long[size.getRows() * size.getColumns()];
            virtual.dump(screen, 0, 0, size.getRows(), size.getColumns(), null);
            return screen;
        }

        private static class DelegateOutputStream extends OutputStream {
            OutputStream output;

            @Override
            public void write(int b) throws IOException {
                output.write(b);
            }

            @Override
            public void write(byte[] b) throws IOException {
                output.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
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
            private final CharsetDecoder decoder = Charset.defaultCharset()
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

    public static void main(String[] args) throws InterruptedException, IOException {

        try (Terminal terminal = TerminalBuilder.builder().build()) {
            Attributes savedAttributes = terminal.enterRawMode();
            terminal.puts(enter_ca_mode);
            int height = terminal.getHeight();

            Display display = new Display(terminal, true);
            display.resize(height, terminal.getWidth());

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
