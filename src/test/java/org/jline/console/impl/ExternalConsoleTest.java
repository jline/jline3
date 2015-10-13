/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.console.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.EnumSet;

import org.jline.console.Attributes;
import org.jline.console.Attributes.ControlChar;
import org.jline.console.Attributes.InputFlag;
import org.jline.console.Attributes.LocalFlag;
import org.jline.console.Attributes.OutputFlag;
import org.jline.console.Console;
import org.jline.reader.ConsoleReader;
import org.jline.reader.ConsoleReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class ExternalConsoleTest {

    @Test
    public void testInput() throws IOException, InterruptedException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExternalConsole console = new ExternalConsole("foo", "ansi", in, out, "UTF-8");

        testConsole(outIn, out, console);
    }

    /* SANDBOX JANSI
    @Test
    public void testPosix() throws IOException, InterruptedException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Console console = new PosixPtyConsole("ansi", new ConsoleReaderBuilder(), NativePty.open(null, null), in, out, "UTF-8");

        testConsole(outIn, out, console);
    }
    */

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

    @Test
    public void testInterrupt() throws Exception {
        PipedInputStream in = new PipedInputStream();
        final PipedOutputStream outIn = new PipedOutputStream(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExternalConsole console = new ExternalConsole("foo", "ansi", in, out, "UTF-8");
        Attributes attributes = console.getAttributes();
        attributes.setLocalFlag(LocalFlag.ISIG, true);
        attributes.setControlChar(ControlChar.VINTR, 3);
        console.setAttributes(attributes);
        ConsoleReader consoleReader = ConsoleReaderBuilder.builder()
                .console(console).build();
        assertNotNull(consoleReader);
        Thread th = new Thread() {
            public void run() {
                try {
                    outIn.write('a');
                    outIn.write('b');
                    outIn.flush();
                    Thread.sleep(50);
                    outIn.write(3);
                    outIn.write('c');
                    outIn.flush();
                    Thread.sleep(50);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        th.start();
        try {
            consoleReader.readLine();
            fail("Expected UserInterruptException");
        } catch (UserInterruptException e) {
            assertEquals("ab", e.getPartialLine());
        }
        th.join();
    }


}
