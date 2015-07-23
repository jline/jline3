/*
 * Copyright (c) 2002-2012, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package jline;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Terminal extension.
 *
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 * @since 2.13
 */
public interface Terminal2 extends Terminal
{
    /*
    enum ControlChar {
        VEOF(0),
        VEOL(1),
        VEOL2(2),
        VERASE(3),
        VWERASE(4),
        VKILL(5),
        VREPRINT(6),
        VINTR(8),
        VQUIT(9),
        VSUSP(10),
        VDSUSP(11),
        VSTART(12),
        VSTOP(13),
        VLNEXT(14),
        VDISCARD(15),
        VMIN(16),
        VTIME(17),
        VSTATUS(18);

        private int cc;

        ControlChar(int cc) {
            this.cc = cc;
        }
    }

    byte getControlChar(ControlChar cc);

    void setControlChar(ControlChar cc, byte value);
    */

    String getStringCapability(String capability);

    int getNumericCapability(String capability);

    boolean getBooleanCapability(String capability);

    /*
    void backspace(OutputStream out, int count);

    void crlf(OutputStream out);

    void ding(OutputStream out);

    void right(OutputStream out);

    void up(OutputStream out);
    */
}
