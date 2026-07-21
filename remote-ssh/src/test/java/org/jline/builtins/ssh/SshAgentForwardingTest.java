/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins.ssh;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.sshd.agent.local.ProxyAgentFactory;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.UserAuthNoneFactory;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.forward.StaticDecisionForwardingFilter;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.ShellFactory;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.LineDisciplineTerminal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(30)
class SshAgentForwardingTest {

    @Test
    void agentForwardingOffByDefault() throws Exception {
        assertFalse(
                opensShellRequestingAgentForwarding(false),
                "interactive ssh must not request ssh-agent forwarding unless -A is given");
    }

    @Test
    void agentForwardingEnabledWithFlag() throws Exception {
        assertTrue(opensShellRequestingAgentForwarding(true), "ssh -A must request ssh-agent forwarding");
    }

    /**
     * Drives {@link Ssh#ssh} against an in-process server and reports whether the interactive shell
     * channel asked the server for ssh-agent forwarding (the server records the
     * {@code auth-agent-req@openssh.com} request through its forwarding filter).
     */
    private boolean opensShellRequestingAgentForwarding(boolean forwardAgent) throws Exception {
        AtomicBoolean agentRequested = new AtomicBoolean(false);

        SshServer sshd = SshServer.setUpDefaultServer();
        sshd.setPort(0);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Path.of("target/agentfwd-hostkey.ser")));
        sshd.setUserAuthFactories(Collections.singletonList(UserAuthNoneFactory.INSTANCE));
        // The server only consults the forwarding filter when it also has an agent factory, so
        // install one; the filter rejects, so no agent channel is actually established.
        sshd.setAgentFactory(new ProxyAgentFactory());
        sshd.setForwardingFilter(new StaticDecisionForwardingFilter(false) {
            @Override
            public boolean canForwardAgent(Session session, String requestType) {
                agentRequested.set(true);
                return false;
            }
        });
        sshd.setShellFactory(new ImmediateExitShellFactory());
        sshd.start();

        Terminal terminal = new LineDisciplineTerminal(
                "agent-forwarding-test", "xterm", new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        try {
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
            Ssh ssh = new Ssh(null, null, null, SshClient::setUpDefaultClient);
            String target = "localhost:" + sshd.getPort();
            String[] argv = forwardAgent ? new String[] {"ssh", "-A", target} : new String[] {"ssh", target};
            PrintStream out = new PrintStream(new ByteArrayOutputStream());

            Thread runner = new Thread(() -> {
                try {
                    ssh.ssh(terminal, reader, "test", new ByteArrayInputStream(new byte[0]), out, out, argv);
                } catch (Exception e) {
                    // connection/shell teardown races are irrelevant to what we assert
                }
            });
            runner.setDaemon(true);
            runner.start();
            runner.join(20000);
            if (runner.isAlive()) {
                runner.interrupt();
                throw new IllegalStateException("ssh() did not return within the timeout");
            }
            return agentRequested.get();
        } finally {
            terminal.close();
            sshd.stop(true);
        }
    }

    /** Server shell that closes the channel as soon as it starts, so the client's shell loop returns. */
    private static class ImmediateExitShellFactory implements ShellFactory {
        @Override
        public Command createShell(ChannelSession channel) {
            return new Command() {
                private ExitCallback callback;

                @Override
                public void setInputStream(InputStream in) {}

                @Override
                public void setOutputStream(OutputStream out) {}

                @Override
                public void setErrorStream(OutputStream err) {}

                @Override
                public void setExitCallback(ExitCallback callback) {
                    this.callback = callback;
                }

                @Override
                public void start(ChannelSession channel, Environment env) throws IOException {
                    callback.onExit(0);
                }

                @Override
                public void destroy(ChannelSession channel) {}
            };
        }
    }
}
