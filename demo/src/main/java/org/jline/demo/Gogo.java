/*
 * Copyright (c) 2002-2017, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo;

import java.util.Map;
import java.util.function.Consumer;

import org.apache.felix.gogo.jline.Shell;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.jline.builtins.ssh.Ssh;
import org.jline.builtins.telnet.Telnet;
import org.jline.terminal.Terminal;

public class Gogo {

    private final CommandProcessor processor;

    public Gogo(CommandProcessor processor) {
        this.processor = processor;
    }

    public Consumer<Ssh.ShellParams> shell() {
        return this::shell;
    }

    public Consumer<Ssh.ExecuteParams> command() {
        return this::command;
    }

    public Telnet.ShellProvider telnet() {
        return this::telnet;
    }

    private void shell(Ssh.ShellParams shell) {
        Terminal terminal = shell.getTerminal();
        CommandSession session = processor.createSession(terminal.input(), terminal.output(), terminal.output());
        session.put(Shell.VAR_TERMINAL, terminal);
        shell.getEnv().forEach(session::put);
        try {
            new Shell(context(shell.getCloser()::run), processor).gosh(session, new String[] {"--login"});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void command(Ssh.ExecuteParams exec) {
        CommandSession session = processor.createSession(exec.getIn(), exec.getOut(), exec.getErr());
        exec.getEnv().forEach(session::put);
        try {
            new Shell(context(null), processor).gosh(session, new String[] {
                "--login", "--nointeractive", "--noshutdown", "--command", exec.getCommand()
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void telnet(Terminal terminal, Map<String, String> environment) {
        CommandSession session = processor.createSession(terminal.input(), terminal.output(), terminal.output());
        session.put(Shell.VAR_TERMINAL, terminal);
        environment.forEach(session::put);
        try {
            new Shell(context(terminal::close), processor).gosh(session, new String[] {"--login"});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    interface Closer {
        void close() throws Exception;
    }

    private Shell.Context context(Closer closer) {
        return new Shell.Context() {
            @Override
            public String getProperty(String name) {
                return System.getProperty(name);
            }

            @Override
            public void exit() throws Exception {
                if (closer != null) {
                    closer.close();
                }
            }
        };
    }
}
