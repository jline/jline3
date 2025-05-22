/*
 * Copyright (c) 2002-2017, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins.ssh;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.sshd.common.channel.PtyMode;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.Signal;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.shell.ShellFactory;
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.ControlChar;
import org.jline.terminal.Attributes.InputFlag;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Attributes.OutputFlag;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SSHD {@link org.apache.sshd.server.command.Command} factory which provides access to
 * Shell.
 */
public class ShellFactoryImpl implements ShellFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShellFactoryImpl.class);

    private final Consumer<Ssh.ShellParams> shell;

    public ShellFactoryImpl(Consumer<Ssh.ShellParams> shell) {
        this.shell = shell;
    }

    public Command createShell(ChannelSession session) {
        return new ShellImpl();
    }

    public class ShellImpl implements Command {
        private InputStream in;

        private OutputStream out;

        private OutputStream err;

        private ExitCallback callback;

        private boolean closed;

        public void setInputStream(final InputStream in) {
            this.in = in;
        }

        public void setOutputStream(final OutputStream out) {
            this.out = out;
        }

        public void setErrorStream(final OutputStream err) {
            this.err = err;
        }

        public void setExitCallback(ExitCallback callback) {
            this.callback = callback;
        }

        public void start(final ChannelSession session, final Environment env) throws IOException {
            try {
                new Thread(() -> ShellImpl.this.run(session, env)).start();
            } catch (Exception e) {
                throw new IOException("Unable to start shell", e);
            }
        }

        public void run(ChannelSession session, Environment env) {
            try {
                Attributes attributes = new Attributes();
                for (Map.Entry<PtyMode, Integer> e : env.getPtyModes().entrySet()) {
                    switch (e.getKey()) {
                        case VINTR:
                            attributes.setControlChar(ControlChar.VINTR, e.getValue());
                            break;
                        case VQUIT:
                            attributes.setControlChar(ControlChar.VQUIT, e.getValue());
                            break;
                        case VERASE:
                            attributes.setControlChar(ControlChar.VERASE, e.getValue());
                            break;
                        case VKILL:
                            attributes.setControlChar(ControlChar.VKILL, e.getValue());
                            break;
                        case VEOF:
                            attributes.setControlChar(ControlChar.VEOF, e.getValue());
                            break;
                        case VEOL:
                            attributes.setControlChar(ControlChar.VEOL, e.getValue());
                            break;
                        case VEOL2:
                            attributes.setControlChar(ControlChar.VEOL2, e.getValue());
                            break;
                        case VSTART:
                            attributes.setControlChar(ControlChar.VSTART, e.getValue());
                            break;
                        case VSTOP:
                            attributes.setControlChar(ControlChar.VSTOP, e.getValue());
                            break;
                        case VSUSP:
                            attributes.setControlChar(ControlChar.VSUSP, e.getValue());
                            break;
                        case VDSUSP:
                            attributes.setControlChar(ControlChar.VDSUSP, e.getValue());
                            break;
                        case VREPRINT:
                            attributes.setControlChar(ControlChar.VREPRINT, e.getValue());
                            break;
                        case VWERASE:
                            attributes.setControlChar(ControlChar.VWERASE, e.getValue());
                            break;
                        case VLNEXT:
                            attributes.setControlChar(ControlChar.VLNEXT, e.getValue());
                            break;
                        /*
                        case VFLUSH:
                            attr.setControlChar(ControlChar.VMIN, e.getValue());
                            break;
                        case VSWTCH:
                            attr.setControlChar(ControlChar.VTIME, e.getValue());
                            break;
                        */
                        case VSTATUS:
                            attributes.setControlChar(ControlChar.VSTATUS, e.getValue());
                            break;
                        case VDISCARD:
                            attributes.setControlChar(ControlChar.VDISCARD, e.getValue());
                            break;
                        case ECHO:
                            attributes.setLocalFlag(LocalFlag.ECHO, e.getValue() != 0);
                            break;
                        case ICANON:
                            attributes.setLocalFlag(LocalFlag.ICANON, e.getValue() != 0);
                            break;
                        case ISIG:
                            attributes.setLocalFlag(LocalFlag.ISIG, e.getValue() != 0);
                            break;
                        case ICRNL:
                            attributes.setInputFlag(InputFlag.ICRNL, e.getValue() != 0);
                            break;
                        case INLCR:
                            attributes.setInputFlag(InputFlag.INLCR, e.getValue() != 0);
                            break;
                        case IGNCR:
                            attributes.setInputFlag(InputFlag.IGNCR, e.getValue() != 0);
                            break;
                        case OCRNL:
                            attributes.setOutputFlag(OutputFlag.OCRNL, e.getValue() != 0);
                            break;
                        case ONLCR:
                            attributes.setOutputFlag(OutputFlag.ONLCR, e.getValue() != 0);
                            break;
                        case ONLRET:
                            attributes.setOutputFlag(OutputFlag.ONLRET, e.getValue() != 0);
                            break;
                        case OPOST:
                            attributes.setOutputFlag(OutputFlag.OPOST, e.getValue() != 0);
                            break;
                    }
                }
                Terminal terminal = TerminalBuilder.builder()
                        .name("JLine SSH")
                        .type(env.getEnv().get("TERM"))
                        .system(false)
                        .streams(in, out)
                        .attributes(attributes)
                        .size(new Size(
                                Integer.parseInt(env.getEnv().get("COLUMNS")),
                                Integer.parseInt(env.getEnv().get("LINES"))))
                        .build();
                env.addSignalListener(
                        (channel, signals) -> {
                            terminal.setSize(new Size(
                                    Integer.parseInt(env.getEnv().get("COLUMNS")),
                                    Integer.parseInt(env.getEnv().get("LINES"))));
                            terminal.raise(Terminal.Signal.WINCH);
                        },
                        Signal.WINCH);

                shell.accept(new Ssh.ShellParams(env.getEnv(), session.getSession(), terminal, () -> destroy(session)));
            } catch (Throwable t) {
                if (!closed) {
                    LOGGER.error("Error occured while executing shell", t);
                }
            }
        }

        public void destroy(ChannelSession session) {
            if (!closed) {
                closed = true;
                flush(out, err);
                close(in, out, err);
                callback.onExit(0);
            }
        }
    }

    static void flush(OutputStream... streams) {
        for (OutputStream s : streams) {
            try {
                s.flush();
            } catch (IOException e) {
                LOGGER.debug("Error flushing " + s, e);
            }
        }
    }

    static void close(Closeable... closeables) {
        for (Closeable c : closeables) {
            try {
                c.close();
            } catch (IOException e) {
                LOGGER.debug("Error closing " + c, e);
            }
        }
    }
}
