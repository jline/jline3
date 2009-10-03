/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */
package jline.console.completer;

import java.util.List;

/**
 * <p>
 * A completor that does nothing. Useful as the last item in an
 * {@link ArgumentCompleter}.
 * </p>
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class NullCompleter
    implements Completer
{
    /**
     * Returns -1 always, indicating that the the buffer is never
     * handled.
     */
    public int complete(final String buffer, int cursor, List candidates) {
        return -1;
    }
}
