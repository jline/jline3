/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.util.List;
import java.util.Map;

import org.jline.shell.AliasManager;
import org.jline.shell.Command;
import org.jline.shell.CommandSession;

/**
 * Built-in alias commands: {@code alias} and {@code unalias}.
 * <p>
 * This command group is automatically added to {@link DefaultCommandDispatcher}
 * when an {@link AliasManager} is configured.
 * <ul>
 *   <li>{@code alias} — list all aliases</li>
 *   <li>{@code alias name=expansion} — define an alias</li>
 *   <li>{@code alias name} — show a single alias</li>
 *   <li>{@code unalias name} — remove an alias</li>
 * </ul>
 *
 * @since 4.0
 */
public class AliasCommands extends SimpleCommandGroup {

    /**
     * Creates alias commands backed by the given alias manager.
     *
     * @param aliasManager the alias manager
     */
    public AliasCommands(AliasManager aliasManager) {
        super("aliases", createCommands(aliasManager));
    }

    private static List<Command> createCommands(AliasManager aliasManager) {
        return List.of(new AliasCommand(aliasManager), new UnaliasCommand(aliasManager));
    }

    private static class AliasCommand extends AbstractCommand {
        private final AliasManager aliasManager;

        AliasCommand(AliasManager aliasManager) {
            super("alias");
            this.aliasManager = aliasManager;
        }

        @Override
        public String description() {
            return "Define or list aliases";
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            if (args.length == 0) {
                // List all aliases
                Map<String, String> aliases = aliasManager.aliases();
                if (aliases.isEmpty()) {
                    session.out().println("No aliases defined.");
                } else {
                    for (Map.Entry<String, String> entry : aliases.entrySet()) {
                        session.out().println("alias " + entry.getKey() + "='" + entry.getValue() + "'");
                    }
                }
                return null;
            }

            // Join all args to handle "alias name=expansion with spaces"
            String arg = String.join(" ", args);
            int eq = arg.indexOf('=');
            if (eq > 0) {
                // Define alias
                String name = arg.substring(0, eq).trim();
                String expansion = arg.substring(eq + 1).trim();
                // Remove surrounding quotes if present
                if (expansion.length() >= 2
                        && ((expansion.startsWith("'") && expansion.endsWith("'"))
                                || (expansion.startsWith("\"") && expansion.endsWith("\"")))) {
                    expansion = expansion.substring(1, expansion.length() - 1);
                }
                aliasManager.setAlias(name, expansion);
            } else {
                // Show single alias
                String name = arg.trim();
                String expansion = aliasManager.getAlias(name);
                if (expansion != null) {
                    session.out().println("alias " + name + "='" + expansion + "'");
                } else {
                    session.err().println("alias: " + name + ": not found");
                }
            }
            return null;
        }
    }

    private static class UnaliasCommand extends AbstractCommand {
        private final AliasManager aliasManager;

        UnaliasCommand(AliasManager aliasManager) {
            super("unalias");
            this.aliasManager = aliasManager;
        }

        @Override
        public String description() {
            return "Remove an alias";
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            if (args.length == 0) {
                session.err().println("unalias: usage: unalias name");
                return null;
            }
            for (String name : args) {
                if (!aliasManager.removeAlias(name)) {
                    session.err().println("unalias: " + name + ": not found");
                }
            }
            return null;
        }
    }
}
