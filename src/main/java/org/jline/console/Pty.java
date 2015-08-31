/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.console;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Constructor;

import org.fusesource.jansi.internal.CLibrary;

public abstract class Pty {

    private final int master;
    private final int slave;
    private final String name;

    Pty(int master, int slave, String name) {
        this.master = master;
        this.slave = slave;
        this.name = name;
    }

    public int getMaster() {
        return master;
    }

    public int getSlave() {
        return slave;
    }

    public FileDescriptor getMasterFD() {
        return newDescriptor(master);
    }

    public FileDescriptor getSlaveFD() {
        return newDescriptor(slave);
    }

    private FileDescriptor newDescriptor(int fd) {
        try {
            Constructor<FileDescriptor> cns = FileDescriptor.class.getDeclaredConstructor(int.class);
            cns.setAccessible(true);
            return cns.newInstance(fd);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create FileDescriptor", e);
        }
    }

    public String getName() {
        return name;
    }

    public abstract Attributes getAttr() throws IOException;

    public abstract void setAttr(Attributes attr) throws IOException;

    public abstract Size getSize() throws IOException;

    public abstract void setSize(Size size) throws IOException;

}
