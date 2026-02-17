/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell;

import java.io.IOException;
import java.util.Map;

/**
 * Manages command aliases for the shell.
 * <p>
 * An alias maps a short name to a longer command expansion. When the shell
 * encounters an alias name as the first word of a command line, it substitutes
 * the alias expansion before executing.
 * <p>
 * Implementations may optionally support persistence via {@link #load()} and
 * {@link #save()}.
 *
 * @see org.jline.shell.impl.DefaultAliasManager
 * @since 4.0
 */
public interface AliasManager {

    /**
     * Defines or redefines an alias.
     *
     * @param name the alias name
     * @param expansion the expansion text
     */
    void setAlias(String name, String expansion);

    /**
     * Removes an alias.
     *
     * @param name the alias name
     * @return true if the alias existed and was removed
     */
    boolean removeAlias(String name);

    /**
     * Returns the expansion for an alias.
     *
     * @param name the alias name
     * @return the expansion, or null if no alias with that name exists
     */
    String getAlias(String name);

    /**
     * Returns all defined aliases.
     *
     * @return an unmodifiable map of alias name to expansion
     */
    Map<String, String> aliases();

    /**
     * Expands aliases in a command line string.
     * <p>
     * The first word of the line is checked against defined aliases. If it matches,
     * the alias expansion is substituted. Supports parameter markers ({@code $1},
     * {@code $2}, {@code $@}) in expansions. Includes a recursion guard to prevent
     * infinite expansion.
     *
     * @param line the command line to expand
     * @return the expanded command line
     */
    String expand(String line);

    /**
     * Loads aliases from persistent storage.
     * <p>
     * The default implementation does nothing.
     *
     * @throws IOException if loading fails
     */
    default void load() throws IOException {}

    /**
     * Saves aliases to persistent storage.
     * <p>
     * The default implementation does nothing.
     *
     * @throws IOException if saving fails
     */
    default void save() throws IOException {}
}
