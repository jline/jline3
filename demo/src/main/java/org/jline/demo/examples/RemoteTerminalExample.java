/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.util.Map;
import java.util.function.Consumer;

import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.jline.builtins.ssh.Ssh;
import org.jline.builtins.telnet.Telnet;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating remote terminal functionality with Telnet and SSH.
 */
public class RemoteTerminalExample {

    // SNIPPET_START: TelnetServerExample
    public static void startTelnetServer() throws Exception {
        // Create a local terminal for the server
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Create a shell provider that will be used for each client connection
        Telnet.ShellProvider shellProvider = (clientTerminal, environment) -> {
            // This code runs for each client that connects
            try {
                // Create a line reader for the client
                LineReader reader =
                        LineReaderBuilder.builder().terminal(clientTerminal).build();

                String line;
                while ((line = reader.readLine("telnet> ")) != null) {
                    if ("exit".equals(line)) {
                        break;
                    }
                    clientTerminal.writer().println("You typed: " + line);
                    clientTerminal.flush();
                }
            } catch (Exception e) {
                // Handle exceptions
            }
        };

        // Create and start the Telnet server
        Telnet telnet = new Telnet(terminal, shellProvider);
        telnet.telnetd(new String[] {
            "--port=2023", // Default is 2019
            "--ip=127.0.0.1", // Default is 127.0.0.1 (localhost only)
            "start"
        });
    }
    // SNIPPET_END: TelnetServerExample

    // SNIPPET_START: SSHServerExample
    public static void startSshServer() throws Exception {
        // Create a local terminal for the server
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Create a shell consumer
        Consumer<Ssh.ShellParams> shellConsumer = (params) -> {
            // This code runs for each client that connects
            try {
                Terminal clientTerminal = params.getTerminal();
                // Create a line reader for the client
                LineReader reader =
                        LineReaderBuilder.builder().terminal(clientTerminal).build();

                String line;
                while ((line = reader.readLine("ssh> ")) != null) {
                    if ("exit".equals(line)) {
                        break;
                    }
                    clientTerminal.writer().println("You typed: " + line);
                    clientTerminal.flush();
                }
            } catch (Exception e) {
                // Handle exceptions
            }
        };

        // Create and start the SSH server
        Ssh ssh = new Ssh(
                shellConsumer,
                null,
                () -> {
                    SshServer server = SshServer.setUpDefaultServer();
                    server.setPasswordAuthenticator(
                            (username, password, session) -> "admin".equals(username) && "password".equals(password));
                    server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
                    return server;
                },
                null);

        ssh.sshd(System.out, System.err, new String[] {
            "--port=2222", // Default is 8022
            "--ip=127.0.0.1", // Default is 127.0.0.1 (localhost only)
            "start"
        });
    }
    // SNIPPET_END: SSHServerExample

    // SNIPPET_START: RemoteShellExample
    public static class RemoteShell implements Telnet.ShellProvider {
        @Override
        public void shell(Terminal terminal, Map<String, String> environment) {
            try {
                // Create a line reader for the client
                LineReader reader =
                        LineReaderBuilder.builder().terminal(terminal).build();

                // Print welcome message
                terminal.writer().println("Welcome to JLine Remote Shell!");
                terminal.writer().println("Type 'help' for available commands.");
                terminal.writer().println("Type 'exit' to disconnect.");
                terminal.flush();

                // Main command loop
                String line;
                while ((line = reader.readLine("shell> ")) != null) {
                    try {
                        if (line.trim().isEmpty()) {
                            continue;
                        } else if ("exit".equals(line)) {
                            break;
                        } else if ("help".equals(line)) {
                            terminal.writer().println("Available commands:");
                            terminal.writer().println("  help - Display this help");
                            terminal.writer().println("  info - Display terminal info");
                            terminal.writer().println("  exit - Exit the shell");
                        } else if ("info".equals(line)) {
                            terminal.writer().println("Terminal type: " + terminal.getType());
                            terminal.writer().println("Size: " + terminal.getWidth() + "x" + terminal.getHeight());
                            terminal.writer().println("Encoding: " + terminal.encoding());
                        } else {
                            terminal.writer().println("Unknown command: " + line);
                            terminal.writer().println("Type 'help' for available commands.");
                        }
                    } catch (Exception e) {
                        terminal.writer().println("Error: " + e.getMessage());
                    }
                    terminal.flush();
                }

                terminal.writer().println("Goodbye!");
                terminal.flush();
            } catch (Exception e) {
                // Handle exceptions
            }
        }
    }
    // SNIPPET_END: RemoteShellExample

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && "telnet".equals(args[0])) {
            startTelnetServer();
            System.out.println("Telnet server started on port 2023");
            System.out.println("Connect with: telnet localhost 2023");
            System.out.println("Press Ctrl+C to exit");
            Thread.sleep(Long.MAX_VALUE);
        } else if (args.length > 0 && "ssh".equals(args[0])) {
            startSshServer();
            System.out.println("SSH server started on port 2222");
            System.out.println("Connect with: ssh -p 2222 admin@localhost");
            System.out.println("Password: password");
            System.out.println("Press Ctrl+C to exit");
            Thread.sleep(Long.MAX_VALUE);
        } else {
            System.out.println("Usage: RemoteTerminalExample [telnet|ssh]");
        }
    }
}
