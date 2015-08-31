/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.console;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.EnumSet;

import org.jline.Console;
import org.jline.JLine.ConsoleReaderBuilder;
import org.jline.console.Attributes.InputFlag;
import org.jline.console.Attributes.LocalFlag;
import org.jline.console.Attributes.OutputFlag;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EmulatedConsoleTest {

    @Test
    public void testInput() throws IOException, InterruptedException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EmulatedConsole console = new EmulatedConsole("ansi", new ConsoleReaderBuilder(), in, out, "UTF-8");

        testConsole(outIn, out, console);
    }

    @Test
    public void testPosix() throws IOException, InterruptedException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Console console = new PosixPtyConsole("ansi", new ConsoleReaderBuilder(), NativePty.open(null, null), in, out, "UTF-8");

        testConsole(outIn, out, console);
    }

    private void testConsole(PipedOutputStream outIn, ByteArrayOutputStream out, Console console) throws IOException, InterruptedException {
        Attributes attributes = console.getAttributes();
        attributes.setLocalFlag(LocalFlag.ECHO, true);
        attributes.setInputFlag(InputFlag.IGNCR, true);
        attributes.setOutputFlags(EnumSet.of(OutputFlag.OPOST));
        console.setAttributes(attributes);

        outIn.write("a\r\nb".getBytes());
        while (out.size() < 3) {
            Thread.sleep(100);
        }

        String output = out.toString();
        assertEquals("a\nb", output);
    }

}
