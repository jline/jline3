/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */

package jline.console;

import java.awt.event.KeyEvent;

/**
 * Symbolic constants for Console operations and virtual key bindings.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 *
 * @see KeyEvent
 * 
 * @since 2.0
 */
public interface ConsoleOperations
{
    char CTRL_A = 1;

    char CTRL_B = 2;

    char CTRL_C = 3;

    char CTRL_D = 4;

    char CTRL_E = 5;

    char CTRL_F = 6;

    char CTRL_K = 11;

    char CTRL_L = 12;

    char CTRL_N = 14;

    char CTRL_P = 16;

    char CTRL_OB = 27;

    char DELETE = 127;

    char CTRL_QM = 127;
}
