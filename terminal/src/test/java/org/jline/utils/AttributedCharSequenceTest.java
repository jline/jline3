/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.jline.terminal.impl.ExternalTerminal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AttributedCharSequenceTest {

    @Test
    public void testUnderline() throws IOException {
        AttributedString as = AttributedString.fromAnsi("\33[38;5;0m\33[48;5;15mtest\33[0m");
        assertEquals(as.toAnsi(256, AttributedCharSequence.ForceMode.Force256Colors), "\33[38;5;0;48;5;15mtest\33[0m");
    }

    @Test
    public void testBoldOnWindows() throws IOException {
        String HIC = "\33[36;1m";
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.appendAnsi(HIC);
        sb.append("the buffer");
        // sb.append(NOR);
        AttributedString as = sb.toAttributedString();

        assertEquals("\33[36;1mthe buffer\33[0m", as.toAnsi(null));
    }

    @Test
    public void testBold() throws IOException {
        ExternalTerminal terminal = new ExternalTerminal(
                "my term",
                "windows",
                new ByteArrayInputStream(new byte[0]),
                new ByteArrayOutputStream(),
                StandardCharsets.UTF_8);

        assertEquals(
                "\33[34;47;1mblue on white\33[31;42;1mred on green\33[0m",
                AttributedString.fromAnsi(
                                "\33[34m\33[47m\33[1mblue on white\33[0m\33[31m\33[42m\33[1mred on green\33[0m")
                        .toAnsi(terminal));

        assertEquals(
                "\33[32;1mtest\33[0m",
                AttributedString.fromAnsi("\33[32m\33[1mtest\33[0m").toAnsi(terminal));
        assertEquals(
                "\33[32;1mtest\33[0m",
                AttributedString.fromAnsi("\33[1m\33[32mtest\33[0m").toAnsi(terminal));
    }

    @Test
    public void testRoundTrip() throws IOException {
        ExternalTerminal terminal = new ExternalTerminal(
                "my term",
                "xterm",
                new ByteArrayInputStream(new byte[0]),
                new ByteArrayOutputStream(),
                StandardCharsets.UTF_8);

        AttributedString org = new AttributedStringBuilder().append("─").toAttributedString();

        AttributedString rndTrip = AttributedString.fromAnsi(org.toAnsi(terminal), terminal);

        assertEquals(org, rndTrip);
    }
}
