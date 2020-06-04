/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

public interface ConsoleOptionGetter {

    /**
     * Return console option value
     * @param name the option name
     * @return option value
     */
    Object consoleOption(String name);
}
