/*
 * Copyright (c) 2022, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.ServiceLoader;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;

import org.jline.terminal.Attributes;
import org.jline.terminal.impl.exec.ExecSupport;
import org.jline.terminal.spi.JansiSupport;
import org.jline.terminal.spi.JnaSupport;
import org.jline.terminal.spi.NativeSupport;
import org.jline.terminal.spi.Pty;
import org.jline.utils.NonBlocking;
import org.jline.utils.NonBlockingInputStream;
import org.jline.utils.NonBlockingReader;
import org.jline.utils.OSUtils;

public class Diag {

    public static void main(String[] args) {
        diag(System.out);
    }

    static void diag(PrintStream out) {
        out.println("System properties");
        out.println("=================");
        out.println("os.name =         " + System.getProperty("os.name"));
        out.println("OSTYPE =          " + System.getenv("OSTYPE"));
        out.println("MSYSTEM =         " + System.getenv("MSYSTEM"));
        out.println("PWD =             " + System.getenv("PWD"));
        out.println("ConEmuPID =       " + System.getenv("ConEmuPID"));
        out.println("WSL_DISTRO_NAME = " + System.getenv("WSL_DISTRO_NAME"));
        out.println("WSL_INTEROP =     " + System.getenv("WSL_INTEROP"));
        out.println();

        out.println("OSUtils");
        out.println("=================");
        out.println("IS_WINDOWS = " + OSUtils.IS_WINDOWS);
        out.println("IS_CYGWIN =  " + OSUtils.IS_CYGWIN);
        out.println("IS_MSYSTEM = " + OSUtils.IS_MSYSTEM);
        out.println("IS_WSL =     " + OSUtils.IS_WSL);
        out.println("IS_WSL1 =    " + OSUtils.IS_WSL1);
        out.println("IS_WSL2 =    " + OSUtils.IS_WSL2);
        out.println("IS_CONEMU =  " + OSUtils.IS_CONEMU);
        out.println("IS_OSX =     " + OSUtils.IS_OSX);
        out.println();

        out.println("JnaSupport");
        out.println("=================");
        try {
            NativeSupport jnaSupport = load(JnaSupport.class);
            try {
                out.println("StdIn posix stream =    " + jnaSupport.isPosixSystemStream(JnaSupport.Stream.Input));
                out.println("StdOut posix stream =   " + jnaSupport.isPosixSystemStream(JnaSupport.Stream.Output));
                out.println("StdErr posix stream =   " + jnaSupport.isPosixSystemStream(JnaSupport.Stream.Error));
            } catch (Throwable t2) {
                out.println("Unable to check posix streams: " + t2);
            }
            try {
                out.println("StdIn windows stream =  " + jnaSupport.isWindowsSystemStream(JnaSupport.Stream.Input));
                out.println("StdOut windows stream = " + jnaSupport.isWindowsSystemStream(JnaSupport.Stream.Output));
                out.println("StdErr windows stream = " + jnaSupport.isWindowsSystemStream(JnaSupport.Stream.Error));
            } catch (Throwable t2) {
                out.println("Unable to check windows streams: " + t2);
            }
            try {
                out.println("StdIn stream name =     " + jnaSupport.posixSystemStreamName(JansiSupport.Stream.Input));
                out.println("StdOut stream name =    " + jnaSupport.posixSystemStreamName(JansiSupport.Stream.Output));
                out.println("StdErr stream name =    " + jnaSupport.posixSystemStreamName(JansiSupport.Stream.Error));
            } catch (Throwable t2) {
                out.println("Unable to check stream names: " + t2);
            }
            testPty( out, () -> jnaSupport.current(NativeSupport.Stream.Output), "posix pty" );
        } catch (Throwable t) {
            out.println("JNA support not available: " + t);
        }
        out.println();

        out.println("JansiSupport");
        out.println("=================");
        try {
            NativeSupport jansiSupport = load(JansiSupport.class);
            try {
                out.println("StdIn posix stream =    " + jansiSupport.isPosixSystemStream(JansiSupport.Stream.Input));
                out.println("StdOut posix stream =   " + jansiSupport.isPosixSystemStream(JansiSupport.Stream.Output));
                out.println("StdErr posix stream =   " + jansiSupport.isPosixSystemStream(JansiSupport.Stream.Error));
            } catch (Throwable t2) {
                out.println("Unable to check posix streams: " + t2);
            }
            try {
                out.println("StdIn windows stream =  " + jansiSupport.isWindowsSystemStream(JansiSupport.Stream.Input));
                out.println("StdOut windows stream = " + jansiSupport.isWindowsSystemStream(JansiSupport.Stream.Output));
                out.println("StdErr windows stream = " + jansiSupport.isWindowsSystemStream(JansiSupport.Stream.Error));
            } catch (Throwable t2) {
                out.println("Unable to check windows streams: " + t2);
            }
            try {
                out.println("StdIn stream name =     " + jansiSupport.posixSystemStreamName(JansiSupport.Stream.Input));
                out.println("StdOut stream name =    " + jansiSupport.posixSystemStreamName(JansiSupport.Stream.Output));
                out.println("StdErr stream name =    " + jansiSupport.posixSystemStreamName(JansiSupport.Stream.Error));
            } catch (Throwable t2) {
                out.println("Unable to check stream names: " + t2);
            }
            testPty( out, () -> jansiSupport.current(NativeSupport.Stream.Output), "posix pty" );
//            try {
//                try (Terminal terminal = jansiSupport.winSysTerminal("foo", "xterm", false,
//                        StandardCharsets.UTF_8, 0, false, Terminal.SignalHandler.SIG_DFL, false)) {
//                    ForkJoinTask<Integer> t = new ForkJoinPool(1).submit(
//                            () -> ((NonBlockingInputStream) terminal.input()).read(1));
//                    int r = t.get(5, TimeUnit.MILLISECONDS);
//                    System.out.println("Windows terminal seems to work: " + terminal);
//                } catch (Throwable t2) {
//                    System.out.println("Unable to open windows terminal: " + t2);
//                    t2.printStackTrace();
//                }
//            } finally {
//                System.setProperty("os.name", osName);
//            }
//            System.out.println();
        } catch (Throwable t) {
            out.println("Jansi support not available: " + t);
        }
        out.println();

        // Exec
        out.println("Exec Support");
        out.println("=================");
        try {
            NativeSupport execSupport = new ExecSupport();
            try {
                out.println("StdIn posix stream =    " + execSupport.isPosixSystemStream(JansiSupport.Stream.Input));
                out.println("StdOut posix stream =   " + execSupport.isPosixSystemStream(JansiSupport.Stream.Output));
                out.println("StdErr posix stream =   " + execSupport.isPosixSystemStream(JansiSupport.Stream.Error));
            } catch (Throwable t2) {
                out.println("Unable to check posix streams: " + t2);
            }
            try {
                out.println("StdIn stream name =     " + execSupport.posixSystemStreamName(JansiSupport.Stream.Input));
                out.println("StdOut stream name =    " + execSupport.posixSystemStreamName(JansiSupport.Stream.Output));
                out.println("StdErr stream name =    " + execSupport.posixSystemStreamName(JansiSupport.Stream.Error));
            } catch (Throwable t2) {
                out.println("Unable to check stream names: " + t2);
            }
        } catch (Throwable t) {
            out.println("Exec support not available: " + t);
        }
    }

    private static void testPty(PrintStream out, ThrowingSupplier<Pty, IOException> ptySupplier, String name) {
        try ( Pty pty = ptySupplier.get()) {
            Attributes attr = enterRawMode(pty);
            try {
                NonBlockingInputStream is = NonBlocking.nonBlocking( name, pty.getSlaveInput());
                NonBlockingReader reader = NonBlocking.nonBlocking( name, is, StandardCharsets.UTF_8);
                ForkJoinTask<Integer> t = new ForkJoinPool(1).submit(() -> is.read(1) );
                int r = t.get(1000, TimeUnit.MILLISECONDS);
                out.println( "The " + name + " seems to work: " + pty );
            } catch (Throwable t3) {
                out.println("Unable to read from " + name + ": " + t3);
                t3.printStackTrace();
            } finally {
                pty.setAttr(attr);
            }
        } catch (Throwable t2) {
            out.println("Unable to open " + name + ": " + t2);
            t2.printStackTrace();
        }
    }

    interface ThrowingSupplier<T, E extends Throwable> {
        T get() throws E;
    }

    static <S> S load(Class<S> clazz) {
        return ServiceLoader.load(clazz, clazz.getClassLoader()).iterator().next();
    }

    static Attributes enterRawMode(Pty pty) throws IOException {
        Attributes prvAttr = pty.getAttr();
        Attributes newAttr = new Attributes(prvAttr);
        newAttr.setLocalFlags( EnumSet.of( Attributes.LocalFlag.ICANON, Attributes.LocalFlag.ECHO, Attributes.LocalFlag.IEXTEN), false);
        newAttr.setInputFlags(EnumSet.of( Attributes.InputFlag.IXON, Attributes.InputFlag.ICRNL, Attributes.InputFlag.INLCR), false);
        newAttr.setControlChar( Attributes.ControlChar.VMIN, 0);
        newAttr.setControlChar( Attributes.ControlChar.VTIME, 1);
        pty.setAttr(newAttr);
        return prvAttr;
    }
}
