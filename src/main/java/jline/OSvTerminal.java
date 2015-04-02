/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package jline;

import jline.internal.Log;

/**
 * Terminal that is used for OSv. This is seperate to unix terminal
 * implementation because exec cannot be used as currently used by UnixTerminal.
 *
 * @author <a href="mailto:arun.neelicattu@gmail.com">Arun Neelicattu</a>
 * @since 2.13
 */
public class OSvTerminal
    extends TerminalSupport
{

    public OSvTerminal() throws Exception {
        super(true);
        setAnsiSupported(true);
        setEchoEnabled(false);
    }
}
