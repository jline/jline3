/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */

package jline.console;

import java.util.List;

/**
 * A completer is the mechanism by which tab-completion candidates will be resolved.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.0
 */
public interface Completer
{
    /**
     * Populates <i>candidates</i> with a list of possible completions for the <i>buffer</i>. The <i>candidates</i>
     * list will not be sorted before being displayed to the user: thus, the complete method should sort the
     * {@link List} before returning.
     *
     * @param buffer     the buffer
     * @param cursor     ???
     * @param candidates the {@link List} of candidates to populate
     * @return the index of the <i>buffer</i> for which the completion will be relative
     */
    int complete(String buffer, int cursor, List<CharSequence> candidates);
}
