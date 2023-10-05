/*
 * Copyright (c) 2002-2019, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.FileDescriptor;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.lang.reflect.Field;

import org.jline.nativ.JLineLibrary;
import org.jline.nativ.JLineNativeLoader;
import org.jline.terminal.Attributes;
import org.jline.terminal.spi.Pty;
import org.jline.terminal.spi.SystemStream;
import org.jline.terminal.spi.TerminalProvider;
import org.jline.utils.NonBlockingInputStream;

import static org.jline.terminal.TerminalBuilder.PROP_FILE_DESCRIPTOR_CREATION_MODE;
import static org.jline.terminal.TerminalBuilder.PROP_FILE_DESCRIPTOR_CREATION_MODE_DEFAULT;
import static org.jline.terminal.TerminalBuilder.PROP_FILE_DESCRIPTOR_CREATION_MODE_NATIVE;
import static org.jline.terminal.TerminalBuilder.PROP_FILE_DESCRIPTOR_CREATION_MODE_REFLECTION;
import static org.jline.terminal.TerminalBuilder.PROP_NON_BLOCKING_READS;

public abstract class AbstractPty implements Pty {

    protected final TerminalProvider provider;
    protected final SystemStream systemStream;
    private Attributes current;

    public AbstractPty(TerminalProvider provider, SystemStream systemStream) {
        this.provider = provider;
        this.systemStream = systemStream;
    }

    @Override
    public void setAttr(Attributes attr) throws IOException {
        current = new Attributes(attr);
        doSetAttr(attr);
    }

    @Override
    public InputStream getSlaveInput() throws IOException {
        InputStream si = doGetSlaveInput();
        if (Boolean.parseBoolean(System.getProperty(PROP_NON_BLOCKING_READS, "true"))) {
            return new PtyInputStream(si);
        } else {
            return si;
        }
    }

    protected abstract void doSetAttr(Attributes attr) throws IOException;

    protected abstract InputStream doGetSlaveInput() throws IOException;

    protected void checkInterrupted() throws InterruptedIOException {
        if (Thread.interrupted()) {
            throw new InterruptedIOException();
        }
    }

    @Override
    public TerminalProvider getProvider() {
        return provider;
    }

    @Override
    public SystemStream getSystemStream() {
        return systemStream;
    }

    class PtyInputStream extends NonBlockingInputStream {
        final InputStream in;
        int c = 0;

        PtyInputStream(InputStream in) {
            this.in = in;
        }

        @Override
        public int read(long timeout, boolean isPeek) throws IOException {
            checkInterrupted();
            if (c != 0) {
                int r = c;
                if (!isPeek) {
                    c = 0;
                }
                return r;
            } else {
                setNonBlocking();
                long start = System.currentTimeMillis();
                while (true) {
                    int r = in.read();
                    if (r >= 0) {
                        if (isPeek) {
                            c = r;
                        }
                        return r;
                    }
                    checkInterrupted();
                    long cur = System.currentTimeMillis();
                    if (timeout > 0 && cur - start > timeout) {
                        return NonBlockingInputStream.READ_EXPIRED;
                    }
                }
            }
        }

        private void setNonBlocking() {
            if (current == null
                    || current.getControlChar(Attributes.ControlChar.VMIN) != 0
                    || current.getControlChar(Attributes.ControlChar.VTIME) != 1) {
                try {
                    Attributes attr = getAttr();
                    attr.setControlChar(Attributes.ControlChar.VMIN, 0);
                    attr.setControlChar(Attributes.ControlChar.VTIME, 1);
                    setAttr(attr);
                } catch (IOException e) {
                    throw new IOError(e);
                }
            }
        }
    }

    private static FileDescriptorCreator fileDescriptorCreator;

    protected static FileDescriptor newDescriptor(int fd) {
        if (fileDescriptorCreator == null) {
            String str =
                    System.getProperty(PROP_FILE_DESCRIPTOR_CREATION_MODE, PROP_FILE_DESCRIPTOR_CREATION_MODE_DEFAULT);
            String[] modes = str.split(",");
            IllegalStateException ise = new IllegalStateException("Unable to create FileDescriptor");
            for (String mode : modes) {
                try {
                    switch (mode) {
                        case PROP_FILE_DESCRIPTOR_CREATION_MODE_NATIVE:
                            fileDescriptorCreator = new NativeFileDescriptorCreator();
                            break;
                        case PROP_FILE_DESCRIPTOR_CREATION_MODE_REFLECTION:
                            fileDescriptorCreator = new ReflectionFileDescriptorCreator();
                            break;
                    }
                } catch (Throwable t) {
                    // ignore
                    ise.addSuppressed(t);
                }
                if (fileDescriptorCreator != null) {
                    break;
                }
            }
            if (fileDescriptorCreator == null) {
                throw ise;
            }
        }
        return fileDescriptorCreator.newDescriptor(fd);
    }

    interface FileDescriptorCreator {
        FileDescriptor newDescriptor(int fd);
    }

    /*
     * Class that could be used on OpenJDK 17.  However, it requires the following JVM option
     *   --add-exports java.base/jdk.internal.access=ALL-UNNAMED
     * so the benefit does not seem important enough to warrant the problems caused
     * by access the jdk.internal.access package at compile time, which itself requires
     * custom compiler options and a different maven module, or at least a different compile
     * phase with a JDK 17 compiler.
     * So, just keep the ReflectionFileDescriptorCreator for now.
     *
    static class Jdk17FileDescriptorCreator implements FileDescriptorCreator {
        private final jdk.internal.access.JavaIOFileDescriptorAccess fdAccess;
        Jdk17FileDescriptorCreator() {
            fdAccess = jdk.internal.access.SharedSecrets.getJavaIOFileDescriptorAccess();
        }

        @Override
        public FileDescriptor newDescriptor(int fd) {
            FileDescriptor descriptor = new FileDescriptor();
            fdAccess.set(descriptor, fd);
            return descriptor;
        }
    }
     */

    /**
     * Reflection based file descriptor creator.
     * This requires the following option
     *   --add-opens java.base/java.io=ALL-UNNAMED
     */
    static class ReflectionFileDescriptorCreator implements FileDescriptorCreator {
        private final Field fileDescriptorField;

        ReflectionFileDescriptorCreator() throws Exception {
            Field field = FileDescriptor.class.getDeclaredField("fd");
            field.setAccessible(true);
            fileDescriptorField = field;
        }

        @Override
        public FileDescriptor newDescriptor(int fd) {
            FileDescriptor descriptor = new FileDescriptor();
            try {
                fileDescriptorField.set(descriptor, fd);
            } catch (IllegalAccessException e) {
                // This should not happen as the field has been set accessible
                throw new IllegalStateException(e);
            }
            return descriptor;
        }
    }

    static class NativeFileDescriptorCreator implements FileDescriptorCreator {
        NativeFileDescriptorCreator() {
            // Force load the library
            JLineNativeLoader.initialize();
        }

        @Override
        public FileDescriptor newDescriptor(int fd) {
            return JLineLibrary.newFileDescriptor(fd);
        }
    }
}
