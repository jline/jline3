/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.utils;

import org.jline.terminal.impl.ExternalTerminal;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class AttributedCharSequenceTest {

    @Test
    public void testBoldOnWindows() throws IOException {
        String HIC = "\33[36;1m";
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.appendAnsi(HIC);
        sb.append("the buffer");
        //sb.append(NOR);
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

        assertEquals("\33[34;47;1mblue on white\33[31;42;1mred on green\33[0m", AttributedString.fromAnsi("\33[34m\33[47m\33[1mblue on white\33[0m\33[31m\33[42m\33[1mred on green\33[0m").toAnsi(terminal));

        assertEquals("\33[32;1mtest\33[0m", AttributedString.fromAnsi("\33[32m\33[1mtest\33[0m").toAnsi(terminal));
        assertEquals("\33[32;1mtest\33[0m", AttributedString.fromAnsi("\33[1m\33[32mtest\33[0m").toAnsi(terminal));

    }

}
