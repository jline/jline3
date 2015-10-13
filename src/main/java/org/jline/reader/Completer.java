/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader;

import java.util.List;

/**
 * A completer is the mechanism by which tab-completion candidates will be resolved.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.3
 */
public interface Completer
{
    /**
     * Populates <i>candidates</i> with a list of possible completions for the <i>buffer</i>.
     *
     * The <i>candidates</i> list will not be sorted before being displayed to the user: thus, the
     * complete method should sort the {@link List} before returning.
     *
     * @param line          The parsed command line
     * @param candidates    The {@link List} of candidates to populate
     */
    void complete(ConsoleReader reader, ParsedLine line, List<Candidate> candidates);
}
