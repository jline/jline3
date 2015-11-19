package org.jline.terminal.impl;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Platform;
import org.fusesource.jansi.internal.CLibrary;
import org.fusesource.jansi.internal.CLibrary.Termios;
import org.fusesource.jansi.internal.CLibrary.WinSize;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.impl.jna.JnaNativePty;
import org.jline.terminal.impl.jna.osx.CLibrary.winsize;
import org.junit.Test;

import static org.jline.terminal.impl.jna.osx.CLibrary.TIOCSWINSZ;

/**
 * Created by gnodet on 18/11/15.
 */
public class JnaNativePtyTest {

    @Test
    public void testNative() throws Exception {
//        int[] master = new int[1];
//        int[] slave = new int[1];
//        byte[] name = new byte[64];
//        int ret = CLibrary.openpty(master, slave, name, null, null);
//        int i = 0;
//        while (name[i++] != 0) ;
//        String strName = new String(name, 0, i - 1);
//        ret = CLibrary.ioctl(slave[0], CLibrary.TIOCSWINSZ, new WinSize((short) 10, (short) 10));


        JnaNativePty pty = JnaNativePty.open(null, null);
        pty.getSlaveInput();
        pty.getSlaveOutput();

        int ret = CLibrary.ioctl(pty.getSlave(), CLibrary.TIOCSWINSZ, new WinSize((short) 10, (short) 10));

        org.jline.terminal.impl.jna.osx.CLibrary C_LIBRARY = (org.jline.terminal.impl.jna.osx.CLibrary) Native.loadLibrary(Platform.C_LIBRARY_NAME, org.jline.terminal.impl.jna.osx.CLibrary.class);
        C_LIBRARY.ioctl(pty.getSlave(), new NativeLong(TIOCSWINSZ), new winsize(new Size(80, 40)));

//        pty.setSize(new Size(80, 40));
//        Attributes attr = pty.getAttr();
    }

}
