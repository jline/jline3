/*
 * Copyright (c) 2026, the original author(s).
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

import org.apache.sshd.client.SshClient;
import org.apache.sshd.core.CoreModuleProperties;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.UserAuthNoneFactory;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.ShellFactory;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.LineDisciplineTerminal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(30)
class SshServerTextTest {

    /** Window title change, screen wipe and a fake password prompt, all inside the banner. */
    private static final String HOSTILE_BANNER = "Authorized access only\n\033]0;pwned\007\033[2J\033[1;1HPassword:";

    @TempDir
    Path tempDir;

    @Test
    void welcomeBannerCannotDriveTheTerminal() throws Exception {
        String out = runWithBanner(HOSTILE_BANNER);
        assertTrue(out.contains("Authorized access only"), "banner text must still be shown: " + out);
        assertFalse(out.contains("\033"), "banner must not put an escape character on the terminal: " + out);
        assertFalse(out.contains("\007"), "banner must not put a BEL on the terminal: " + out);
    }

    /**
     * Drives {@link Ssh#ssh} against an in-process server that sends {@code banner} as its
     * pre-authentication welcome banner, and returns what the client wrote to its terminal.
     */
    private String runWithBanner(String banner) throws Exception {
        SshServer sshd = SshServer.setUpDefaultServer();
        sshd.setPort(0);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(tempDir.resolve("banner-hostkey.ser")));
        sshd.setUserAuthFactories(Collections.singletonList(UserAuthNoneFactory.INSTANCE));
        CoreModuleProperties.WELCOME_BANNER.set(sshd, banner);
        sshd.setShellFactory(new ImmediateExitShellFactory());
        sshd.start();

        ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
        Terminal terminal = new LineDisciplineTerminal("banner-test", "xterm", terminalOut, StandardCharsets.UTF_8);
        try {
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
            Ssh ssh = new Ssh(null, null, null, SshClient::setUpDefaultClient);
            String[] argv = new String[] {"ssh", "localhost:" + sshd.getPort()};
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
            terminal.writer().flush();
            return terminalOut.toString(StandardCharsets.UTF_8);
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
                public void setInputStream(InputStream in) {
                    // not needed: test shell exits immediately without reading
                }

                @Override
                public void setOutputStream(OutputStream out) {
                    // not needed: test shell exits immediately without writing
                }

                @Override
                public void setErrorStream(OutputStream err) {
                    // not needed: test shell exits immediately without writing
                }

                @Override
                public void setExitCallback(ExitCallback callback) {
                    this.callback = callback;
                }

                @Override
                public void start(ChannelSession channel, Environment env) throws IOException {
                    callback.onExit(0);
                }

                @Override
                public void destroy(ChannelSession channel) {
                    // nothing to clean up in the test stub
                }
            };
        }
    }
}
