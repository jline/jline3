package org.jline.console;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.fusesource.jansi.Pty;
import org.fusesource.jansi.Pty.Attributes;
import org.jline.Console;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EmulatedConsoleTest {

    @Test
    public void testInput() throws IOException, InterruptedException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EmulatedConsole console = new EmulatedConsole("ansi", in, out, "UTF-8");

        testConsole(outIn, out, console);
    }

    @Test
    public void testPosix() throws IOException, InterruptedException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Console console = new PosixPtyConsole("ansi", in, out, "UTF-8", null, null);

        testConsole(outIn, out, console);
    }

    private void testConsole(PipedOutputStream outIn, ByteArrayOutputStream out, Console console) throws IOException, InterruptedException {
        Attributes attributes = console.getAttributes();
        attributes.setLocalFlag(Pty.ECHO, true);
        attributes.setInputFlag(Pty.IGNCR, true);
        attributes.setOutputFlags(Pty.OPOST);
        console.setAttributes(attributes);

        outIn.write("a\r\nb".getBytes());
        while (out.size() < 3) {
            Thread.sleep(100);
        }

        String output = out.toString();
        assertEquals("a\nb", output);
    }

}
