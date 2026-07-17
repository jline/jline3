/*
 * Copyright (c) 2026, the original author(s).
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
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Verifies that the file-reading POSIX commands (cat, sort) do not fetch
 * remote URLs when given an absolute URI argument such as
 * {@code http://host/path} or {@code jar:http://host/x.jar!/entry}.
 *
 * <p>On 3.x the vulnerable pattern was {@code cwd.toUri().resolve(arg)},
 * which passes through any absolute URI unchanged (including {@code http:},
 * {@code jar:http:}, etc.). The fix switches to {@code cwd.resolve(arg)}
 * ({@link Path#resolve}), which always treats the argument as a local
 * filesystem path.
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
    void catDoesNotFetchRemoteUrl() throws Exception {
        assertNoNetworkFetch(url -> {
            try {
                PosixCommands.cat(context, new String[] {"cat", url});
            } catch (Exception ignore) {
                // rejected URL treated as a bogus local path and fails; that is fine
            }
        });
    }

    @Test
    void catDoesNotFetchJarHttpUrl() throws Exception {
        assertNoNetworkFetchJar(url -> {
            try {
                PosixCommands.cat(context, new String[] {"cat", url});
            } catch (Exception ignore) {
                // rejected URL treated as a bogus local path and fails; that is fine
            }
        });
    }

    @Test
    void sortDoesNotFetchRemoteUrl() throws Exception {
        assertNoNetworkFetch(url -> {
            try {
                PosixCommands.sort(context, new String[] {"sort", url});
            } catch (Exception ignore) {
                // rejected URL treated as a bogus local path and fails; that is fine
            }
        });
    }

    @Test
    void sortDoesNotFetchJarHttpUrl() throws Exception {
        assertNoNetworkFetchJar(url -> {
            try {
                PosixCommands.sort(context, new String[] {"sort", url});
            } catch (Exception ignore) {
                // rejected URL treated as a bogus local path and fails; that is fine
            }
        });
    }

    /**
     * Starts a local HTTP server, passes {@code http://127.0.0.1:<port>/path}
     * to the command under test, and asserts no connection is made.
     */
    private void assertNoNetworkFetch(java.util.function.Consumer<String> command) throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            server.setSoTimeout(3000);
            Thread accepter = new Thread(() -> {
                try (Socket s = server.accept()) {
                    connected.countDown();
                    s.getOutputStream()
                            .write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                                    .getBytes());
                    s.getOutputStream().flush();
                } catch (Exception ignore) {
                    // accept() timeout: no connection was made — expected outcome
                }
            });
            accepter.setDaemon(true);
            accepter.start();

            String url = "http://127.0.0.1:" + server.getLocalPort() + "/secret";
            Thread run = new Thread(() -> command.accept(url));
            run.setDaemon(true);
            run.start();
            run.join(5000);

            assertFalse(run.isAlive(), "command must not block on an http URL argument");
            assertFalse(
                    connected.await(500, TimeUnit.MILLISECONDS),
                    "command must not open a network connection for an http URL argument");
        }
    }

    /**
     * Same as above but with a {@code jar:http://...} wrapper URL.
     */
    private void assertNoNetworkFetchJar(java.util.function.Consumer<String> command) throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            server.setSoTimeout(3000);
            Thread accepter = new Thread(() -> {
                try (Socket s = server.accept()) {
                    connected.countDown();
                    s.getOutputStream()
                            .write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                                    .getBytes());
                    s.getOutputStream().flush();
                } catch (Exception ignore) {
                    // accept() timeout: no connection was made — expected outcome
                }
            });
            accepter.setDaemon(true);
            accepter.start();

            String url = "jar:http://127.0.0.1:" + server.getLocalPort() + "/x.jar!/entry";
            Thread run = new Thread(() -> command.accept(url));
            run.setDaemon(true);
            run.start();
            run.join(5000);

            assertFalse(run.isAlive(), "command must not block on a jar:http URL argument");
            assertFalse(
                    connected.await(500, TimeUnit.MILLISECONDS),
                    "command must not open a network connection for a jar:http URL argument");
        }
    }
}
