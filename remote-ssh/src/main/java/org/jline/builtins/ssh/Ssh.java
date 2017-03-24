/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.builtins.ssh;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.auth.keyboard.UserInteraction;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.channel.PtyMode;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.util.io.NoCloseInputStream;
import org.apache.sshd.common.util.io.NoCloseOutputStream;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.jline.builtins.Options;
import org.jline.reader.LineReader;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;

public class Ssh {

    public static final String[] functions = {"ssh", "sshd"};

    public static class ShellParams {
        private final Map<String, String> env;
        private final Terminal terminal;
        private final Runnable closer;
        public ShellParams(Map<String, String> env, Terminal terminal, Runnable closer) {
            this.env = env;
            this.terminal = terminal;
            this.closer = closer;
        }
        public Map<String, String> getEnv() {
            return env;
        }
        public Terminal getTerminal() {
            return terminal;
        }
        public Runnable getCloser() {
            return closer;
        }
    }

    public static class ExecuteParams {
        private final String command;
        private final Map<String, String> env;
        private final InputStream in;
        private final OutputStream out;
        private final OutputStream err;
        public ExecuteParams(String command, Map<String, String> env, InputStream in, OutputStream out, OutputStream err) {
            this.command = command;
            this.env = env;
            this.in = in;
            this.out = out;
            this.err = err;
        }
        public String getCommand() {
            return command;
        }
        public Map<String, String> getEnv() {
            return env;
        }
        public InputStream getIn() {
            return in;
        }
        public OutputStream getOut() {
            return out;
        }
        public OutputStream getErr() {
            return err;
        }
    }

    private static final int defaultPort = 2022;

    private final Consumer<ShellParams> shell;
    private final Consumer<ExecuteParams> execute;
    private final Supplier<SshServer> serverBuilder;
    private final Supplier<SshClient> clientBuilder;
    private SshServer server;
    private int port;
    private String ip;

    public Ssh(Consumer<ShellParams> shell,
               Consumer<ExecuteParams> execute,
               Supplier<SshServer> serverBuilder,
               Supplier<SshClient> clientBuilder) {
        this.shell = shell;
        this.execute = execute;
        this.serverBuilder = serverBuilder;
        this.clientBuilder = clientBuilder;
    }

    public void ssh(Terminal terminal,
                    LineReader reader,
                    String user,
                    InputStream stdin,
                    PrintStream stdout,
                    PrintStream stderr,
                    String[] argv) throws Exception {
        final String[] usage = {"ssh - connect to a server using ssh",
                "Usage: ssh [user@]hostname [command]",
                "  -? --help                show help"};


        Options opt = Options.compile(usage).parse(argv, true);
        List<String> args = opt.args();

        if (opt.isSet("help") || args.isEmpty()) {
            opt.usage(stderr);
            return;
        }

        String username = user;
        String hostname = args.remove(0);
        int port = this.port;
        String command = null;
        int idx = hostname.indexOf('@');
        if (idx >= 0) {
            username = hostname.substring(0, idx);
            hostname = hostname.substring(idx + 1);
        }
        idx = hostname.indexOf(':');
        if (idx >= 0) {
            port = Integer.parseInt(hostname.substring(idx + 1));
            hostname = hostname.substring(0, idx);
        }
        if (!args.isEmpty()) {
            command = String.join(" ", args);
        }

        try (SshClient client = clientBuilder.get()) {
            JLineUserInteraction ui = new JLineUserInteraction(terminal, reader, stderr);
            client.setFilePasswordProvider(ui);
            client.setUserInteraction(ui);
            client.start();

            try (ClientSession sshSession = connectWithRetries(terminal.writer(), client, username, hostname, port, 3)) {
                sshSession.auth().verify();
                if (command != null) {
                    ClientChannel channel = sshSession.createChannel("exec", command + "\n");
                    channel.setIn(new ByteArrayInputStream(new byte[0]));
                    channel.setOut(new NoCloseOutputStream(stdout));
                    channel.setErr(new NoCloseOutputStream(stderr));
                    channel.open().verify();
                    channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 0);
                } else {
                    final ChannelShell channel = sshSession.createShellChannel();
                    Attributes attributes = terminal.enterRawMode();
                    try {
                        Map<PtyMode, Integer> modes = new HashMap<>();
                        // Control chars
                        modes.put(PtyMode.VINTR, attributes.getControlChar(Attributes.ControlChar.VINTR));
                        modes.put(PtyMode.VQUIT, attributes.getControlChar(Attributes.ControlChar.VQUIT));
                        modes.put(PtyMode.VERASE, attributes.getControlChar(Attributes.ControlChar.VERASE));
                        modes.put(PtyMode.VKILL, attributes.getControlChar(Attributes.ControlChar.VKILL));
                        modes.put(PtyMode.VEOF, attributes.getControlChar(Attributes.ControlChar.VEOF));
                        modes.put(PtyMode.VEOL, attributes.getControlChar(Attributes.ControlChar.VEOL));
                        modes.put(PtyMode.VEOL2, attributes.getControlChar(Attributes.ControlChar.VEOL2));
                        modes.put(PtyMode.VSTART, attributes.getControlChar(Attributes.ControlChar.VSTART));
                        modes.put(PtyMode.VSTOP, attributes.getControlChar(Attributes.ControlChar.VSTOP));
                        modes.put(PtyMode.VSUSP, attributes.getControlChar(Attributes.ControlChar.VSUSP));
                        modes.put(PtyMode.VDSUSP, attributes.getControlChar(Attributes.ControlChar.VDSUSP));
                        modes.put(PtyMode.VREPRINT, attributes.getControlChar(Attributes.ControlChar.VREPRINT));
                        modes.put(PtyMode.VWERASE, attributes.getControlChar(Attributes.ControlChar.VWERASE));
                        modes.put(PtyMode.VLNEXT, attributes.getControlChar(Attributes.ControlChar.VLNEXT));
                        modes.put(PtyMode.VSTATUS, attributes.getControlChar(Attributes.ControlChar.VSTATUS));
                        modes.put(PtyMode.VDISCARD, attributes.getControlChar(Attributes.ControlChar.VDISCARD));
                        // Input flags
                        modes.put(PtyMode.IGNPAR, getFlag(attributes, Attributes.InputFlag.IGNPAR));
                        modes.put(PtyMode.PARMRK, getFlag(attributes, Attributes.InputFlag.PARMRK));
                        modes.put(PtyMode.INPCK, getFlag(attributes, Attributes.InputFlag.INPCK));
                        modes.put(PtyMode.ISTRIP, getFlag(attributes, Attributes.InputFlag.ISTRIP));
                        modes.put(PtyMode.INLCR, getFlag(attributes, Attributes.InputFlag.INLCR));
                        modes.put(PtyMode.IGNCR, getFlag(attributes, Attributes.InputFlag.IGNCR));
                        modes.put(PtyMode.ICRNL, getFlag(attributes, Attributes.InputFlag.ICRNL));
                        modes.put(PtyMode.IXON, getFlag(attributes, Attributes.InputFlag.IXON));
                        modes.put(PtyMode.IXANY, getFlag(attributes, Attributes.InputFlag.IXANY));
                        modes.put(PtyMode.IXOFF, getFlag(attributes, Attributes.InputFlag.IXOFF));
                        // Local flags
                        modes.put(PtyMode.ISIG, getFlag(attributes, Attributes.LocalFlag.ISIG));
                        modes.put(PtyMode.ICANON, getFlag(attributes, Attributes.LocalFlag.ICANON));
                        modes.put(PtyMode.ECHO, getFlag(attributes, Attributes.LocalFlag.ECHO));
                        modes.put(PtyMode.ECHOE, getFlag(attributes, Attributes.LocalFlag.ECHOE));
                        modes.put(PtyMode.ECHOK, getFlag(attributes, Attributes.LocalFlag.ECHOK));
                        modes.put(PtyMode.ECHONL, getFlag(attributes, Attributes.LocalFlag.ECHONL));
                        modes.put(PtyMode.NOFLSH, getFlag(attributes, Attributes.LocalFlag.NOFLSH));
                        modes.put(PtyMode.TOSTOP, getFlag(attributes, Attributes.LocalFlag.TOSTOP));
                        modes.put(PtyMode.IEXTEN, getFlag(attributes, Attributes.LocalFlag.IEXTEN));
                        // Output flags
                        modes.put(PtyMode.OPOST, getFlag(attributes, Attributes.OutputFlag.OPOST));
                        modes.put(PtyMode.ONLCR, getFlag(attributes, Attributes.OutputFlag.ONLCR));
                        modes.put(PtyMode.OCRNL, getFlag(attributes, Attributes.OutputFlag.OCRNL));
                        modes.put(PtyMode.ONOCR, getFlag(attributes, Attributes.OutputFlag.ONOCR));
                        modes.put(PtyMode.ONLRET, getFlag(attributes, Attributes.OutputFlag.ONLRET));
                        channel.setPtyModes(modes);
                        channel.setPtyColumns(terminal.getWidth());
                        channel.setPtyLines(terminal.getHeight());
                        channel.setAgentForwarding(true);
                        channel.setEnv("TERM", terminal.getType());
                        // TODO: channel.setEnv("LC_CTYPE", terminal.encoding().toString());
                        channel.setIn(new NoCloseInputStream(stdin));
                        channel.setOut(new NoCloseOutputStream(stdout));
                        channel.setErr(new NoCloseOutputStream(stderr));
                        channel.open().verify();
                        Terminal.SignalHandler prevWinchHandler = terminal.handle(Terminal.Signal.WINCH, signal -> {
                            try {
                                Size size = terminal.getSize();
                                channel.sendWindowChange(size.getColumns(), size.getRows());
                            } catch (IOException e) {
                                // Ignore
                            }
                        });
                        Terminal.SignalHandler prevQuitHandler = terminal.handle(Terminal.Signal.QUIT, signal -> {
                            try {
                                channel.getInvertedIn().write(attributes.getControlChar(Attributes.ControlChar.VQUIT));
                                channel.getInvertedIn().flush();
                            } catch (IOException e) {
                                // Ignore
                            }
                        });
                        Terminal.SignalHandler prevIntHandler = terminal.handle(Terminal.Signal.INT, signal -> {
                            try {
                                channel.getInvertedIn().write(attributes.getControlChar(Attributes.ControlChar.VINTR));
                                channel.getInvertedIn().flush();
                            } catch (IOException e) {
                                // Ignore
                            }
                        });
                        Terminal.SignalHandler prevStopHandler = terminal.handle(Terminal.Signal.TSTP, signal -> {
                            try {
                                channel.getInvertedIn().write(attributes.getControlChar(Attributes.ControlChar.VDSUSP));
                                channel.getInvertedIn().flush();
                            } catch (IOException e) {
                                // Ignore
                            }
                        });
                        try {
                            channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 0);
                        } finally {
                            terminal.handle(Terminal.Signal.WINCH, prevWinchHandler);
                            terminal.handle(Terminal.Signal.INT, prevIntHandler);
                            terminal.handle(Terminal.Signal.TSTP, prevStopHandler);
                            terminal.handle(Terminal.Signal.QUIT, prevQuitHandler);
                        }
                    } finally {
                        terminal.setAttributes(attributes);
                    }
                }
            }
        }
    }

    private static int getFlag(Attributes attributes, Attributes.InputFlag flag) {
        return attributes.getInputFlag(flag) ? 1 : 0;
    }

    private static int getFlag(Attributes attributes, Attributes.OutputFlag flag) {
        return attributes.getOutputFlag(flag) ? 1 : 0;
    }

    private static int getFlag(Attributes attributes, Attributes.LocalFlag flag) {
        return attributes.getLocalFlag(flag) ? 1 : 0;
    }

    private ClientSession connectWithRetries(PrintWriter stdout, SshClient client, String username, String host, int port, int maxAttempts) throws Exception {
        ClientSession session = null;
        int retries = 0;
        do {
            ConnectFuture future = client.connect(username, host, port);
            future.await();
            try {
                session = future.getSession();
            } catch (Exception ex) {
                if (retries++ < maxAttempts) {
                    Thread.sleep(2 * 1000);
                    stdout.println("retrying (attempt " + retries + ") ...");
                } else {
                    throw ex;
                }
            }
        } while (session == null);
        return session;
    }

    public void sshd(PrintStream stdout, PrintStream stderr, String[] argv) throws IOException {
        final String[] usage = {"sshd - start an ssh server",
                "Usage: sshd [-i ip] [-p port] start | stop | status",
                "  -i --ip=INTERFACE        listen interface (default=127.0.0.1)",
                "  -p --port=PORT           listen port (default=" + defaultPort + ")",
                "  -? --help                show help"};

        Options opt = Options.compile(usage).parse(argv, true);
        List<String> args = opt.args();

        if (opt.isSet("help") || args.isEmpty()) {
            opt.usage(stderr);
            return;
        }

        String command = args.get(0);

        if ("start".equals(command)) {
            if (server != null) {
                throw new IllegalStateException("sshd is already running on port " + port);
            }
            ip = opt.get("ip");
            port = opt.getNumber("port");
            start();
            status(stdout);
        } else if ("stop".equals(command)) {
            if (server == null) {
                throw new IllegalStateException("sshd is not running.");
            }
            stop();
        } else if ("status".equals(command)) {
            status(stdout);
        } else {
            throw opt.usageError("bad command: " + command);
        }

    }

    private void status(PrintStream stdout) {
        if (server != null) {
            stdout.println("sshd is running on " + ip + ":" + port);
        } else {
            stdout.println("sshd is not running.");
        }
    }

    private void start() throws IOException {
        server = serverBuilder.get();
        server.setPort(port);
        server.setHost(ip);
        server.setShellFactory(new ShellFactoryImpl(shell));
        server.setCommandFactory(new ScpCommandFactory.Builder()
                .withDelegate(command -> new ShellCommand(execute, command)).build());
        server.setSubsystemFactories(Collections.singletonList(
                new SftpSubsystemFactory.Builder().build()
        ));
        server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        server.start();
    }

    private void stop() throws IOException {
        try {
            server.stop();
        } finally {
            server = null;
        }
    }

    private static class JLineUserInteraction implements UserInteraction, FilePasswordProvider {
        private final Terminal terminal;
        private final LineReader reader;
        private final PrintStream stderr;

        public JLineUserInteraction(Terminal terminal, LineReader reader, PrintStream stderr) {
            this.terminal = terminal;
            this.reader = reader;
            this.stderr = stderr;
        }

        @Override
        public String getPassword(String resourceKey) throws IOException {
            return readLine("Enter password for " + resourceKey + ":", false);
        }

        @Override
        public void welcome(ClientSession session, String banner, String lang) {
            terminal.writer().println(banner);
        }

        @Override
        public String[] interactive(ClientSession s, String name, String instruction, String lang, String[] prompt, boolean[] echo) {
            String[] answers = new String[prompt.length];
            try {
                for (int i = 0; i < prompt.length; i++) {
                    answers[i] = readLine(prompt[i], echo[i]);
                }
            } catch (Exception e) {
                stderr.append(e.getClass().getSimpleName()).append(" while read prompts: ").println(e.getMessage());
            }
            return answers;
        }

        @Override
        public boolean isInteractionAllowed(ClientSession session) {
            return true;
        }

        @Override
        public void serverVersionInfo(ClientSession session, List<String> lines) {
            for (String l : lines) {
                terminal.writer().append('\t').println(l);
            }
        }

        @Override
        public String getUpdatedPassword(ClientSession session, String prompt, String lang) {
            try {
                return readLine(prompt, false);
            } catch (Exception e) {
                stderr.append(e.getClass().getSimpleName()).append(" while reading password: ").println(e.getMessage());
            }
            return null;
        }

        private String readLine(String prompt, boolean echo) {
            return reader.readLine(prompt + " ", echo ? null : '\0');
        }
    }
}
