/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */

package jline.console;

import java.util.List;

/**
 * Console history.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.0
 */
public interface History
{
    int size();

    void clear();

    // TODO: Change to CharSequence?

    List<String> items();

    void add(String item);

    int index();

    String current();

    boolean previous();

    boolean next();

    boolean moveToFirst();

    boolean moveToLast();

    void moveToEnd();
}
