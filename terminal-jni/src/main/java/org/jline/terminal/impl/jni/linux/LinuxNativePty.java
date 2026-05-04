/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.jni.linux;

import java.io.FileDescriptor;
import java.io.IOException;

import org.jline.nativ.CLibrary;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.impl.jni.JniNativePty;
import org.jline.terminal.spi.SystemStream;
import org.jline.terminal.spi.TerminalProvider;

public class LinuxNativePty extends JniNativePty {

    public static LinuxNativePty current(TerminalProvider provider, SystemStream systemStream) throws IOException {
        try {
            switch (systemStream) {
                case Output:
                    return new LinuxNativePty(
                            provider, systemStream, -1, null, 0, FileDescriptor.in, 1, FileDescriptor.out, ttyname(1));
                case Error:
                    return new LinuxNativePty(
                            provider, systemStream, -1, null, 0, FileDescriptor.in, 2, FileDescriptor.err, ttyname(2));
                default:
                    throw new IllegalArgumentException("Unsupported stream for console: " + systemStream);
            }
        } catch (IOException e) {
            throw new IOException("Not a tty", e);
        }
    }

    public static LinuxNativePty open(TerminalProvider provider, Attributes attr, Size size) throws IOException {
        int[] master = new int[1];
        int[] slave = new int[1];
        byte[] buf = new byte[64];
        CLibrary.openpty(
                master,
                slave,
                buf,
                attr != null ? toNativeTermios(attr) : null,
                size != null ? new CLibrary.WinSize((short) size.getRows(), (short) size.getColumns()) : null);
        int len = 0;
        while (buf[len] != 0) {
            len++;
        }
        String name = new String(buf, 0, len);
        return new LinuxNativePty(
                provider, null, master[0], newDescriptor(master[0]), slave[0], newDescriptor(slave[0]), name);
    }

    public LinuxNativePty(
            TerminalProvider provider,
            SystemStream systemStream,
            int master,
            FileDescriptor masterFD,
            int slave,
            FileDescriptor slaveFD,
            String name) {
        super(provider, systemStream, master, masterFD, slave, slaveFD, name);
    }

    public LinuxNativePty(
            TerminalProvider provider,
            SystemStream systemStream,
            int master,
            FileDescriptor masterFD,
            int slave,
            FileDescriptor slaveFD,
            int slaveOut,
            FileDescriptor slaveOutFD,
            String name) {
        super(provider, systemStream, master, masterFD, slave, slaveFD, slaveOut, slaveOutFD, name);
    }
}
