/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader.completer;

import java.util.List;

import org.jline.Candidate;
import org.jline.Completer;
import org.jline.reader.ParsedLine;

/**
 * Null completer.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.3
 */
public final class NullCompleter
    implements Completer
{
    public static final NullCompleter INSTANCE = new NullCompleter();

    public int complete(final ParsedLine line, final List<Candidate> candidates) {
        return -1;
    }
}