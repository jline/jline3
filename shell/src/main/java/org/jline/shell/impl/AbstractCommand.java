/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jline.shell.Command;

/**
 * Base implementation of {@link Command} that stores name and aliases.
 * <p>
 * Example:
 * <pre>
 * Command echo = new AbstractCommand("echo", "e") {
 *     &#64;Override
 *     public Object execute(CommandSession session, String[] args) {
 *         session.out().println(String.join(" ", args));
 *         return null;
 *     }
 * };
 * </pre>
 *
 * @see Command
 * @since 4.0
 */
public abstract class AbstractCommand implements Command {

    private final String name;
    private final List<String> aliases;

    /**
     * Creates a new command with the given name and optional aliases.
     *
     * @param name the primary command name
     * @param aliases optional alias names
     */
    protected AbstractCommand(String name, String... aliases) {
        this.name = name;
        this.aliases =
                aliases.length > 0 ? Collections.unmodifiableList(Arrays.asList(aliases)) : Collections.emptyList();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<String> aliases() {
        return aliases;
    }
}
