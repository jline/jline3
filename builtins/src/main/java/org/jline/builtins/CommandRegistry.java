/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import org.jline.builtins.Completers;
import org.jline.builtins.Widgets;
import org.jline.reader.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CommandRegistry {
    
    /**
     * Aggregate SystemCompleters of commandRegisteries
     * @return uncompiled SystemCompleter
     */
    static Completers.SystemCompleter aggregateCompleters(CommandRegistry ... commandRegistries) {
        Completers.SystemCompleter out = new Completers.SystemCompleter();
        for (CommandRegistry r: commandRegistries) {
            out.add(r.compileCompleters());
        }
        return out;
    }

    /**
     * Aggregate and compile SystemCompleters of commandRegisteries
     * @return compiled SystemCompleter
     */
    static Completers.SystemCompleter compileCompleters(CommandRegistry ... commandRegistries) {
        Completers.SystemCompleter out = aggregateCompleters(commandRegistries);
        out.compile();
        return out;
    }

    /**
     * Returns the command names known by this registry.
     * @return the set of known command names, excluding aliases
     */
    Set<String> commandNames();

    /**
     * Returns a map of alias-to-command names known by this registry.
     * @return a map with alias keys and command name values
     */
    Map<String, String> commandAliases();
    
    /**
     * Returns a short info about command known by this registry.
     * @return a short info about command
     */
    List<String> commandInfo(String command);

    /**
     * Returns whether a command with the specified name is known to this registry.
     * @param command the command name to test
     * @return true if the specified command is registered
     */
    boolean hasCommand(String command);

    /**
     * Returns a {@code SystemCompleter} that can provide detailed completion
     * information for all registered commands.
     * 
     * @return a SystemCompleter that can provide command completion for all registered commands
     */
    Completers.SystemCompleter compileCompleters();

    /**
     * Returns a command description for use in the JLine Widgets framework.
     * @param command name of the command whose description to return 
     * @return command description for JLine TailTipWidgets to be displayed
     *         in the terminal status bar.
     */
    Widgets.CmdDesc commandDescription(String command);
    
    default Object execute(String command, String[] args) throws Exception {
        throw new IllegalArgumentException("Not implemented!");
    }

    default Object invoke(String command, Object... args) throws Exception {
        throw new IllegalArgumentException("Not implemented!");
    }
}