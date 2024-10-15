/*
 * Copyright (c) 2002-2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins.ssh;

import java.nio.file.Paths;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.junit.jupiter.api.Test;

public class SshdTest {

    @Test
    void test() throws Exception {
        SshServer sshd = SshServer.setUpDefaultServer();
        sshd.setPort(0);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Paths.get("target/hostkey.ser")));
        sshd.setPasswordAuthenticator((username, password, session) -> true);
        sshd.setShellFactory(new ShellFactoryImpl(shellParams -> {
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(shellParams.getTerminal())
                    .build();

            try {
                String line;
                reader.printAbove("Welcome to SSH Server");
                while ((line = reader.readLine("sshTest > ")) != null) {
                    System.out.println(line);
                }
            } catch (UserInterruptException e) {
                // Ignore
            } catch (EndOfFileException e) {
                // Ignore
            } catch (Exception e) {
                // ignore OTHER EXCEPTIONS
            }
        }));

        // Start the server
        sshd.start();
        System.out.println("SSH Server started on port 2222");

        SshClient client = SshClient.setUpDefaultClient();
        client.start();
        ClientSession session =
                client.connect("test", "localhost", sshd.getPort()).verify().getClientSession();
        session.addPasswordIdentity("foo");
        session.auth().verify();
        ChannelShell shell = session.createShellChannel();
        shell.open().verify();
        shell.getInvertedIn().write("echo foo\n".getBytes());
        shell.close();
        client.close();

        Thread.sleep(1000);
    }
}
