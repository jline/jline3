/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins.ssh;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.keyverifier.ServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.UserAuthNoneFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.LineDisciplineTerminal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the host key verification installed by {@link Ssh#setupServerKeyVerifier}: an unknown
 * key needs the user's confirmation and is then recorded in the known-hosts file, a recorded key is
 * accepted without a prompt, and a changed key is refused.
 */
@Timeout(60)
class SshHostKeyVerificationTest {

    @TempDir
    Path tempDir;

    @Test
    void unknownKeyIsRecordedAfterConfirmationAndReusedSilently() throws Exception {
        Path knownHosts = tempDir.resolve("known_hosts");
        try (SshServer sshd = newServer(tempDir.resolve("keyA.ser"), 0)) {
            sshd.start();
            assertTrue(
                    authenticates(sshd.getPort(), knownHosts, "yes\n", new ByteArrayOutputStream()),
                    "connection must succeed once the user confirms the key");
            assertTrue(Files.exists(knownHosts), "confirmed key must be recorded");
            assertTrue(Files.size(knownHosts) > 0, "known-hosts file must not be empty");
            // second connection: the key is known, so no confirmation is needed
            assertTrue(
                    authenticates(sshd.getPort(), knownHosts, "", new ByteArrayOutputStream()),
                    "a recorded key must be accepted without prompting");
        }
    }

    @Test
    void unknownKeyIsRefusedWithoutConfirmation() throws Exception {
        Path knownHosts = tempDir.resolve("known_hosts");
        try (SshServer sshd = newServer(tempDir.resolve("keyA.ser"), 0)) {
            sshd.start();
            assertFalse(
                    authenticates(sshd.getPort(), knownHosts, "no\n", new ByteArrayOutputStream()),
                    "connection must fail when the user does not confirm the key");
            assertFalse(Files.exists(knownHosts), "a refused key must not be recorded");
        }
    }

    @Test
    void changedKeyIsRefused() throws Exception {
        Path knownHosts = tempDir.resolve("known_hosts");
        int port;
        try (SshServer sshd = newServer(tempDir.resolve("keyA.ser"), 0)) {
            sshd.start();
            port = sshd.getPort();
            assertTrue(authenticates(port, knownHosts, "yes\n", new ByteArrayOutputStream()));
            sshd.stop(true);
        }
        // same endpoint, different host key: must be refused even if the user would confirm
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        try (SshServer sshd = newServer(tempDir.resolve("keyB.ser"), port)) {
            sshd.start();
            assertFalse(
                    authenticates(port, knownHosts, "yes\n", stderr),
                    "a host key that differs from the recorded one must be refused");
        }
        String warning = stderr.toString(StandardCharsets.UTF_8);
        assertTrue(
                warning.contains("REMOTE HOST IDENTIFICATION HAS CHANGED"),
                "the user must be warned about the changed key: " + warning);
    }

    @Test
    void callerConfiguredVerifierIsLeftInPlace() throws Exception {
        ServerKeyVerifier custom = (session, address, key) -> true;
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.setServerKeyVerifier(custom);
            try (Terminal terminal = newTerminal("", new ByteArrayOutputStream())) {
                LineReader reader =
                        LineReaderBuilder.builder().terminal(terminal).build();
                Ssh.setupServerKeyVerifier(
                        client, reader, new PrintStream(new ByteArrayOutputStream()), tempDir.resolve("known_hosts"));
            }
            assertSame(custom, client.getServerKeyVerifier(), "an explicitly configured verifier must be kept");
        }
        // AcceptAllServerKeyVerifier.INSTANCE set explicitly after a previous
        // setupServerKeyVerifier call must also be left in place
        try (SshClient client = SshClient.setUpDefaultClient()) {
            try (Terminal terminal = newTerminal("", new ByteArrayOutputStream())) {
                LineReader reader =
                        LineReaderBuilder.builder().terminal(terminal).build();
                PrintStream ps = new PrintStream(new ByteArrayOutputStream());
                // first call installs the KnownHostsServerKeyVerifier
                Ssh.setupServerKeyVerifier(client, reader, ps, tempDir.resolve("known_hosts"));
                assertFalse(
                        client.getServerKeyVerifier() instanceof AcceptAllServerKeyVerifier,
                        "first call must install KnownHostsServerKeyVerifier");
                // caller explicitly reverts to AcceptAll
                client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
                // second call must respect the explicit choice
                Ssh.setupServerKeyVerifier(client, reader, ps, tempDir.resolve("known_hosts"));
                assertSame(
                        AcceptAllServerKeyVerifier.INSTANCE,
                        client.getServerKeyVerifier(),
                        "AcceptAllServerKeyVerifier.INSTANCE set after our install must be kept");
            }
        }
    }

    @Test
    void missingParentDirectoryIsCreatedForKnownHosts() throws Exception {
        Path knownHosts = tempDir.resolve("missing/.ssh/known_hosts");
        assertFalse(Files.isDirectory(knownHosts.getParent()), "parent must not exist yet");
        try (SshServer sshd = newServer(tempDir.resolve("keyMissing.ser"), 0)) {
            sshd.start();
            assertTrue(
                    authenticates(sshd.getPort(), knownHosts, "yes\n", new ByteArrayOutputStream()),
                    "connection must succeed even when the known_hosts parent is missing");
            assertTrue(Files.isDirectory(knownHosts.getParent()), "parent directory must have been created");
            assertTrue(Files.exists(knownHosts), "confirmed key must be recorded");
            assertTrue(Files.size(knownHosts) > 0, "known-hosts file must not be empty");
        }
    }

    private static SshServer newServer(Path hostKey, int port) {
        SshServer sshd = SshServer.setUpDefaultServer();
        sshd.setPort(port);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(hostKey));
        sshd.setUserAuthFactories(Collections.singletonList(UserAuthNoneFactory.INSTANCE));
        return sshd;
    }

    /**
     * Connects and authenticates with a client whose verifier was installed by
     * {@link Ssh#setupServerKeyVerifier}, answering a possible confirmation prompt with
     * {@code terminalInput}.
     */
    private static boolean authenticates(int port, Path knownHosts, String terminalInput, ByteArrayOutputStream stderr)
            throws Exception {
        try (Terminal terminal = newTerminal(terminalInput, new ByteArrayOutputStream());
                SshClient client = SshClient.setUpDefaultClient()) {
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
            Ssh.setupServerKeyVerifier(client, reader, new PrintStream(stderr), knownHosts);
            client.start();
            try (ClientSession session =
                    client.connect("test", "localhost", port).verify(10000).getSession()) {
                session.auth().verify(10000);
                return session.isAuthenticated();
            } catch (Exception e) {
                return false;
            }
        }
    }

    private static Terminal newTerminal(String input, ByteArrayOutputStream output) throws Exception {
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("host-key-test", "xterm", output, StandardCharsets.UTF_8);
        if (!input.isEmpty()) {
            terminal.processInputBytes(input.getBytes(StandardCharsets.UTF_8));
        }
        return terminal;
    }
}
