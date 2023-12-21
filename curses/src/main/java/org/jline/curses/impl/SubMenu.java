/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses.impl;

import java.util.List;

public class SubMenu {

    private String name;
    private String key;
    private List<MenuItem> contents;

    public SubMenu(String name, String key, List<MenuItem> contents) {
        this.name = name;
        this.key = key;
        this.contents = contents;
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    public List<MenuItem> getContents() {
        return contents;
    }
}
