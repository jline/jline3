/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.jansi;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.jline.jansi.io.AnsiOutputStream;
import org.jline.jansi.io.AnsiProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EncodingTest {

    @Test
    public void testEncoding8859() throws UnsupportedEncodingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final AtomicReference<String> newLabel = new AtomicReference<>();
        PrintStream ansi = new AnsiPrintStream(
                new AnsiOutputStream(
                        baos,
                        null,
                        AnsiMode.Default,
                        new AnsiProcessor(baos) {
                            @Override
                            protected void processChangeWindowTitle(String label) {
                                newLabel.set(label);
                            }
                        },
                        AnsiType.Emulation,
                        AnsiColors.TrueColor,
                        StandardCharsets.ISO_8859_1,
                        null,
                        null,
                        false),
                true,
                "ISO-8859-1");

        ansi.print("\033]0;un bon café\007");
        ansi.flush();
        assertEquals("un bon café", newLabel.get());
    }

    @Test
    public void testEncodingUtf8() throws UnsupportedEncodingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final AtomicReference<String> newLabel = new AtomicReference<>();
        PrintStream ansi = new PrintStream(
                new AnsiOutputStream(
                        baos,
                        null,
                        AnsiMode.Default,
                        new AnsiProcessor(baos) {
                            @Override
                            protected void processChangeWindowTitle(String label) {
                                newLabel.set(label);
                            }
                        },
                        AnsiType.Emulation,
                        AnsiColors.TrueColor,
                        StandardCharsets.UTF_8,
                        null,
                        null,
                        false),
                true,
                "UTF-8");

        ansi.print("\033]0;ひらがな\007");
        ansi.flush();
        assertEquals("ひらがな", newLabel.get());
    }
}
