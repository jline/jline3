/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.builtins.telnet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import org.jline.builtins.Options;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.TerminalBuilder;

/*
 * a very simple Telnet server.
 * real remote access should be via ssh.
 */
public class Telnet {

    public static final String[] functions = {"telnetd"};

    public interface ShellProvider {

        void shell(Terminal terminal, Map<String, String> environment);

    }

    private static final int defaultPort = 2019;

    private final Terminal terminal;
    private final ShellProvider provider;
    private PortListener portListener;
    private int port;
    private String ip;

    public Telnet(Terminal terminal, ShellProvider provider) {
        this.terminal = terminal;
        this.provider = provider;
    }

    public void telnetd(String[] argv) throws IOException {
        final String[] usage = {"telnetd - start simple telnet server",
                "Usage: telnetd [-i ip] [-p port] start | stop | status",
                "  -i --ip=INTERFACE        listen interface (default=127.0.0.1)",
                "  -p --port=PORT           listen port (default=" + defaultPort + ")",
                "  -? --help                show help"};

        Options opt = Options.compile(usage).parse(argv, true);
        List<String> args = opt.args();

        if (opt.isSet("help") || args.isEmpty()) {
            opt.usage(System.err);
            return;
        }

        String command = args.get(0);

        if ("start".equals(command)) {
            if (portListener != null) {
                throw new IllegalStateException("telnetd is already running on port " + port);
            }
            ip = opt.get("ip");
            port = opt.getNumber("port");
            start();
            status();
        } else if ("stop".equals(command)) {
            if (portListener == null) {
                throw new IllegalStateException("telnetd is not running.");
            }
            stop();
        } else if ("status".equals(command)) {
            status();
        } else {
            throw opt.usageError("bad command: " + command);
        }
    }

    private void status() {
        if (portListener != null) {
            System.out.println("telnetd is running on " + ip + ":" + port);
        } else {
            System.out.println("telnetd is not running.");
        }
    }

    private void start() throws IOException {
        ConnectionManager connectionManager = new ConnectionManager(1000, 5 * 60 * 1000, 5 * 60 * 1000, 60 * 1000, null, null, false) {
            @Override
            protected Connection createConnection(ThreadGroup threadGroup, ConnectionData newCD) {
                return new Connection(threadGroup, newCD) {
                    TelnetIO telnetIO;

                    @Override
                    protected void doRun() throws Exception {
                        telnetIO = new TelnetIO();
                        telnetIO.setConnection(this);
                        telnetIO.initIO();

                        InputStream in = new InputStream() {
                            @Override
                            public int read() throws IOException {
                                return telnetIO.read();
                            }
                            @Override
                            public int read(byte[] b, int off, int len) throws IOException {
                                int r = read();
                                if (r >= 0) {
                                    b[off] = (byte) r;
                                    return 1;
                                } else {
                                    return -1;
                                }
                            }
                        };
                        PrintStream out = new PrintStream(new OutputStream() {
                            @Override
                            public void write(int b) throws IOException {
                                telnetIO.write(b);
                            }
                            @Override
                            public void flush() throws IOException {
                                telnetIO.flush();
                            }
                        });
                        Terminal terminal = TerminalBuilder.builder()
                                .type(getConnectionData().getNegotiatedTerminalType().toLowerCase())
                                .streams(in, out)
                                .system(false)
                                .name("telnet")
                                .build();
                        terminal.setSize(new Size(getConnectionData().getTerminalColumns(), getConnectionData().getTerminalRows()));
                        terminal.setAttributes(Telnet.this.terminal.getAttributes());
                        addConnectionListener(new ConnectionListener() {
                            @Override
                            public void connectionTerminalGeometryChanged(ConnectionEvent ce) {
                                terminal.setSize(new Size(getConnectionData().getTerminalColumns(), getConnectionData().getTerminalRows()));
                                terminal.raise(Signal.WINCH);
                            }
                        });
                        try {
                            provider.shell(terminal, getConnectionData().getEnvironment());
                        } finally {
                            close();
                        }
                    }

                    @Override
                    protected void doClose() throws Exception {
                        telnetIO.closeOutput();
                        telnetIO.closeInput();
                    }
                };
            }
        };
        portListener = new PortListener("gogo", port, 10);
        portListener.setConnectionManager(connectionManager);
        portListener.start();
    }

    private void stop() throws IOException {
        portListener.stop();
        portListener = null;
    }

}
