/*
 * Copyright (c) 2002-2021, the original author(s).
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

    /**
     * Read console option value
     * @param <T> option type
     * @param option option name
     * @param defval default value
     * @return option value
     */
    <T> T consoleOption(String option, T defval);
}
