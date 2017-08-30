/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.builtins.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;

public class ShellCommand implements Command, SessionAware {

    private static final Logger LOGGER = Logger.getLogger(ShellCommand.class.getName());

    private final Consumer<Ssh.ExecuteParams> execute;
    private final String command;
    private InputStream in;
    private OutputStream out;
    private OutputStream err;
    private ExitCallback callback;
    private ServerSession session;
    private Environment env;

    public ShellCommand(Consumer<Ssh.ExecuteParams> execute, String command) {
        this.execute = execute;
        this.command = command;
    }

    public void setInputStream(InputStream in) {
        this.in = in;
    }

    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    public void setErrorStream(OutputStream err) {
        this.err = err;
    }

    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;
    }

    public void setSession(ServerSession session) {
        this.session = session;
    }

    public void start(final Environment env) throws IOException {
        this.env = env;
        new Thread(this::run).start();
    }

    private void run() {
        int exitStatus = 0;
        try {
            execute.accept(new Ssh.ExecuteParams(command, env.getEnv(), in, out, err));
        } catch (RuntimeException e) {
            exitStatus = 1;
            LOGGER.log(Level.SEVERE, "Unable to start shell", e);
            try {
                Throwable t = (e.getCause() != null)  ? e.getCause() : e;
                err.write(t.toString().getBytes());
                err.flush();
            } catch (IOException e2) {
                // Ignore
            }
        } finally {
            ShellFactoryImpl.close(in, out, err);
            callback.onExit(exitStatus);
        }
    }

    public void destroy() {
    }

}
