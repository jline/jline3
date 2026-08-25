/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins.telnet;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class TelnetIOEscapeTest {

    private static final int IAC = 255;
    private static final int DO = 253;
    private static final int LOGOUT = 18;

    private static ByteArrayOutputStream wire(TelnetIO io) throws Exception {
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        Field out = TelnetIO.class.getDeclaredField("out");
        out.setAccessible(true);
        out.set(io, new DataOutputStream(sink));
        return sink;
    }

    @Test
    public void dataByteIacIsDoubled() throws Exception {
        TelnetIO io = new TelnetIO();
        ByteArrayOutputStream sink = wire(io);

        io.write(IAC); // a 0xFF byte emitted by the hosted application
        io.flush();

        // The peer un-escapes 255 255 back to a single 255; a lone 255 would be
        // read as the IAC command introducer instead.
        assertArrayEquals(new byte[] {(byte) 255, (byte) 255}, sink.toByteArray());
    }

    @Test
    public void iacInsideDataStreamIsEscapedInPlace() throws Exception {
        TelnetIO io = new TelnetIO();
        ByteArrayOutputStream sink = wire(io);

        io.write(new byte[] {(byte) 'a', (byte) 0xFF, (byte) 'b'});
        io.flush();

        assertArrayEquals(new byte[] {(byte) 'a', (byte) 255, (byte) 255, (byte) 'b'}, sink.toByteArray());
    }

    @Test
    public void consecutiveIacBytesAreEachDoubled() throws Exception {
        TelnetIO io = new TelnetIO();
        ByteArrayOutputStream sink = wire(io);

        io.write(new byte[] {(byte) 0xFF, (byte) 0xFF});
        io.flush();

        // Two 0xFF data bytes are escaped independently, so four reach the wire.
        assertArrayEquals(new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255}, sink.toByteArray());
    }

    @Test
    public void iacFollowedByLfKeepsCrLfConversion() throws Exception {
        TelnetIO io = new TelnetIO();
        ByteArrayOutputStream sink = wire(io);

        io.write(new byte[] {(byte) 0xFF, (byte) '\n'});
        io.flush();

        // IAC doubling and the LF -> CRLF conversion do not interfere.
        assertArrayEquals(new byte[] {(byte) 255, (byte) 255, (byte) 13, (byte) 10}, sink.toByteArray());
    }

    @Test
    public void ordinaryBytesPassThroughUnchanged() throws Exception {
        TelnetIO io = new TelnetIO();
        ByteArrayOutputStream sink = wire(io);

        io.write((byte) 'A');
        io.flush();

        assertArrayEquals(new byte[] {(byte) 'A'}, sink.toByteArray());
    }

    @Test
    public void logoutCommandIsNotDoubled() throws Exception {
        TelnetIO io = new TelnetIO();
        ByteArrayOutputStream sink = wire(io);

        io.closeOutput();

        // IAC DO LOGOUT is a command sequence, so its leading 255 stays single.
        assertArrayEquals(new byte[] {(byte) IAC, (byte) DO, (byte) LOGOUT}, sink.toByteArray());
    }
}
