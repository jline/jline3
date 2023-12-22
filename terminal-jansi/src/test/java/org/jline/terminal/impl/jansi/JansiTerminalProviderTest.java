/*
 * Copyright (c) 2002-2021, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.jansi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jline.terminal.Terminal;
import org.jline.terminal.spi.SystemStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JansiTerminalProviderTest {

    static {
        System.err.println("init");
        System.err.flush();
        ensureOpenPtyLoaded();
        System.err.println("ensureOpenPtyLoaded done");
        System.err.flush();
    }

    @BeforeEach
    void setup() {
        System.err.println("setup");
        System.err.flush();
    }

    @Test
    public void testJansiVersion() {
        System.err.println("testJansiVersion");
        System.err.flush();
        assertEquals(2, JansiTerminalProvider.JANSI_MAJOR_VERSION);
        assertEquals(4, JansiTerminalProvider.JANSI_MINOR_VERSION);
    }

    @Test
    void testIsSystemStream() {
        System.err.println("testIsSystemStream");
        System.err.flush();
        assertDoesNotThrow(() -> new JansiTerminalProvider().isSystemStream(SystemStream.Output));
    }

    @Test
    // @Disabled
    void testNewTerminal() throws IOException {
        System.err.println("testNewTerminal");
        System.err.flush();
        PipedOutputStream pos = new PipedOutputStream();
        PipedInputStream pis = new PipedInputStream(pos);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Terminal terminal = new JansiTerminalProvider()
                .newTerminal(
                        "name",
                        "xterm",
                        pis,
                        baos,
                        Charset.defaultCharset(),
                        Terminal.SignalHandler.SIG_DFL,
                        true,
                        null,
                        null);
        assertNotNull(terminal);
    }

    static void ensureOpenPtyLoaded() {
        try {
            Process p = Runtime.getRuntime().exec(new String[] {"uname", "-m"});
            p.waitFor();
            try (InputStream in = p.getInputStream()) {
                String hwName = readFully(in).trim();
                Path libDir = Paths.get("/usr/lib", hwName + "-linux-gnu");
                try (Stream<Path> stream = Files.list(libDir)) {
                    List<Path> libs = stream.filter(
                                    l -> l.getFileName().toString().startsWith("libutil.so."))
                            .collect(Collectors.toList());
                    String lib = libs.iterator().next().toString();
                    System.err.println("Loading " + lib);
                    System.err.flush();
                    System.load(lib);
                }
            }

            //            int[] master = new int[1];
            //            int[] slave = new int[1];
            //            byte[] name = new byte[64];
            //            try {
            //            CLibrary.openpty(master, slave, name, null, null);
            //            new FileInputStream(AbstractPty.newDescriptor(master[0])).close();
            //            new FileInputStream(AbstractPty.newDescriptor(slave[0])).close();
            //            } catch (Throwable t) {
            //                t.printStackTrace();
            //                Class<?> cl = CLibrary.class;
            //            }
        } catch (Throwable t) {
            throw new LinkageError("Unable to load CLibrary for openpty", t);
        }
    }

    private static String readFully(InputStream in) throws IOException {
        int readLen = 0;
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[32];
        while ((readLen = in.read(buf, 0, buf.length)) >= 0) {
            b.write(buf, 0, readLen);
        }
        return b.toString();
    }
}
