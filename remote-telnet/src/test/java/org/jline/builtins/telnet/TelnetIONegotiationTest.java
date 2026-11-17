/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins.telnet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A peer that sends data without answering the telnet option negotiation is not a telnet client.
 * A web page can make the browser POST to the telnetd port, and the shell would otherwise read the
 * request headers and body as command lines.
 */
public class TelnetIONegotiationTest {

    private static final int IAC = 255;
    private static final int WONT = 252;
    private static final int TTYPE = 24;
    private static final int NAWS = 31;

    @Test
    void httpRequestIsRejected() throws Exception {
        String request = "POST / HTTP/1.1\r\n"
                + "Host: 127.0.0.1:2019\r\n"
                + "Content-Type: text/plain;charset=UTF-8\r\n"
                + "Content-Length: 18\r\n"
                + "\r\n"
                + "\r\ninjected-command\r\n";
        TelnetIO io = newTelnetIO(request.getBytes(StandardCharsets.US_ASCII));
        assertThrows(IOException.class, () -> initTelnetCommunication(io));
    }

    @Test
    void negotiatingClientIsAccepted() throws Exception {
        ByteArrayOutputStream in = new ByteArrayOutputStream();
        in.write(new byte[] {(byte) IAC, (byte) WONT, (byte) NAWS, (byte) IAC, (byte) WONT, (byte) TTYPE});
        in.write("ls\r\n".getBytes(StandardCharsets.US_ASCII));
        TelnetIO io = newTelnetIO(in.toByteArray());
        assertDoesNotThrow(() -> initTelnetCommunication(io));
    }

    @Test
    void silentClientIsAccepted() throws Exception {
        TelnetIO io = newTelnetIO(new byte[0]);
        assertDoesNotThrow(() -> initTelnetCommunication(io));
    }

    private static void initTelnetCommunication(TelnetIO io) throws Exception {
        Method m = TelnetIO.class.getDeclaredMethod("initTelnetCommunication");
        m.setAccessible(true);
        try {
            m.invoke(io);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }

    private static TelnetIO newTelnetIO(byte[] input) throws Exception {
        Socket socket = new Socket() {
            @Override
            public InetAddress getInetAddress() {
                return InetAddress.getLoopbackAddress();
            }

            @Override
            public int getPort() {
                return 0;
            }

            @Override
            public synchronized void setSoTimeout(int timeout) {
                // no real socket
            }

            @Override
            public synchronized void close() {
                // no real socket
            }
        };
        TelnetIO io = new TelnetIO();
        setField(io, "connectionData", new ConnectionData(socket, null));
        setField(io, "in", new DataInputStream(new ByteArrayInputStream(input)));
        setField(io, "out", new DataOutputStream(new ByteArrayOutputStream()));
        Class<?> iacClass = Class.forName("org.jline.builtins.telnet.TelnetIO$IACHandler");
        Constructor<?> ctor = iacClass.getDeclaredConstructor(TelnetIO.class);
        ctor.setAccessible(true);
        setField(io, "iacHandler", ctor.newInstance(io));
        return io;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = TelnetIO.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
