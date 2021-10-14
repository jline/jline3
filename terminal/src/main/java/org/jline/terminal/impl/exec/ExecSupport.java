/*
 * Copyright (c) 2022, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.exec;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.Charset;

import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.ExecPty;
import org.jline.terminal.spi.NativeSupport;
import org.jline.terminal.spi.Pty;
import org.jline.utils.ExecHelper;
import org.jline.utils.OSUtils;

public class ExecSupport implements NativeSupport  {

    public String name() {
        return "exec";
    }

    @Override
    public Pty current(Stream consoleStream) throws IOException {
        return ExecPty.current(consoleStream);
    }

    @Override
    public Pty open(Attributes attributes, Size size) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Terminal winSysTerminal( String name, String type, boolean ansiPassThrough, Charset encoding, int codepage,
                                    boolean nativeSignals, Terminal.SignalHandler signalHandler, boolean paused,
                                    Stream consoleStream ) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWindowsSystemStream( Stream stream )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPosixSystemStream(Stream stream)
    {
        try {
            Process p = new ProcessBuilder(OSUtils.TEST_COMMAND, "-t", Integer.toString(stream.ordinal()))
                    .inheritIO().start();
            return p.waitFor() == 0;
        } catch (Throwable t) {
            // ignore
        }
        return false;
    }

    @Override
    public String posixSystemStreamName(Stream stream)
    {
        try {
            ProcessBuilder.Redirect input = stream == Stream.Input
                                ? ProcessBuilder.Redirect.INHERIT
                                : getRedirect(stream == Stream.Output ? FileDescriptor.out : FileDescriptor.err);
            Process p = new ProcessBuilder(OSUtils.TTY_COMMAND).redirectInput(input).start();
            String result = ExecHelper.waitAndCapture(p);
            if (p.exitValue() == 0) {
                return result.trim();
            }
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }

    private ProcessBuilder.Redirect getRedirect(FileDescriptor fd) throws ReflectiveOperationException {
        // This is not really allowed, but this is the only way to redirect the output or error stream
        // to the input.  This is definitely not something you'd usually want to do, but in the case of
        // the `tty` utility, it provides a way to get
        Class<?> rpi = Class.forName("java.lang.ProcessBuilder$RedirectPipeImpl");
        Constructor<?> cns = rpi.getDeclaredConstructor();
        cns.setAccessible(true);
        ProcessBuilder.Redirect input = (ProcessBuilder.Redirect) cns.newInstance();
        Field f = rpi.getDeclaredField("fd");
        f.setAccessible(true);
        f.set(input, fd);
        return input;
    }
}
