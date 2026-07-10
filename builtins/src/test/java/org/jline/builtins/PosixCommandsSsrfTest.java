/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the file-reading POSIX commands only accept local (file-backed)
 * {@code jar:} sources and never fetch a nested network URL such as
 * {@code jar:http://host/x.jar!/entry}.
 */
class PosixCommandsSsrfTest {

    @TempDir
    Path tempDir;

    private Terminal terminal;
    private PosixCommands.Context context;

    @BeforeEach
    void setUp() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        Map<String, Object> vars = new HashMap<>();
        terminal = new DumbTerminal(in, out);
        context = new PosixCommands.Context(
                in, new PrintStream(out), new PrintStream(new ByteArrayOutputStream()), tempDir, terminal, vars::get);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (terminal != null) {
            terminal.close();
        }
    }

    @Test
    void catDoesNotFetchNestedHttpJarUrl() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            server.setSoTimeout(3000);
            Thread accepter = new Thread(() -> {
                try (Socket s = server.accept()) {
                    connected.countDown();
                    // Reply with an empty 404 and close so the client never blocks.
                    s.getOutputStream()
                            .write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                                    .getBytes());
                    s.getOutputStream().flush();
                } catch (Exception ignore) {
                    // timeout: no connection was made
                }
            });
            accepter.setDaemon(true);
            accepter.start();

            String url = "jar:http://127.0.0.1:" + server.getLocalPort() + "/x.jar!/entry";
            Thread run = new Thread(() -> {
                try {
                    PosixCommands.cat(context, new String[] {"cat", url});
                } catch (Exception ignore) {
                    // a rejected jar: URL is read as a bogus local path and fails; that is fine
                }
            });
            run.setDaemon(true);
            run.start();
            run.join(5000);

            assertFalse(run.isAlive(), "cat must not block on a jar:http URL argument");
            assertFalse(
                    connected.await(500, TimeUnit.MILLISECONDS),
                    "cat must not open a network connection for a jar:http URL argument");
        }
    }

    @Test
    void onlyFileBackedJarsAreReadAsUrlSources() throws Exception {
        // An empty or local authority is what the JDK's file handler reads from disk.
        assertTrue(PosixCommands.isLocalJarFile(new URL("file:/tmp/x.jar")));
        assertTrue(PosixCommands.isLocalJarFile(new URL("file:///tmp/x.jar")));
        assertTrue(PosixCommands.isLocalJarFile(new URL("file://localhost/tmp/x.jar")));

        // A file: URL with a remote authority is fetched over FTP by the file handler,
        // and resolves to a UNC path on Windows, so it must not be read as a URL source.
        assertFalse(PosixCommands.isLocalJarFile(new URL("file://169.254.169.254/x.jar")));
        assertFalse(PosixCommands.isLocalJarFile(new URL("file://attacker.example.com/share/x.jar")));

        // Nested network schemes stay rejected.
        assertFalse(PosixCommands.isLocalJarFile(new URL("http://169.254.169.254/x.jar")));
        assertFalse(PosixCommands.isLocalJarFile(new URL("https://169.254.169.254/x.jar")));
        assertFalse(PosixCommands.isLocalJarFile(new URL("ftp://169.254.169.254/x.jar")));
    }

    @Test
    void catStillReadsLocalFileJar() throws Exception {
        Path jar = tempDir.resolve("local.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar))) {
            jos.putNextEntry(new JarEntry("entry.txt"));
            jos.write("hello-from-jar".getBytes());
            jos.closeEntry();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PosixCommands.Context ctx = new PosixCommands.Context(
                new ByteArrayInputStream(new byte[0]),
                new PrintStream(out),
                new PrintStream(new ByteArrayOutputStream()),
                tempDir,
                terminal,
                new HashMap<String, Object>()::get);

        String url = "jar:" + jar.toUri() + "!/entry.txt";
        PosixCommands.cat(ctx, new String[] {"cat", url});

        assertTrue(out.toString().contains("hello-from-jar"), "cat should still read entries from a local file jar");
    }
}
