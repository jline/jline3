/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.File;
import java.util.*;

import org.jline.builtins.CommandRegistry;
import org.jline.builtins.Widgets;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.utils.AttributedStringBuilder;

/**
 * Aggregate command registeries.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public class SystemRegistryImpl implements SystemRegistry {
    private final CommandRegistry[] commandRegistries;
    private Integer consoleId = null;

    public SystemRegistryImpl(CommandRegistry... commandRegistries) {
        this.commandRegistries = commandRegistries;
        for (int i = 0; i < commandRegistries.length; i++) {
            if (commandRegistries[i] instanceof ConsoleEngine) {
                if (consoleId != null) {
                    throw new IllegalArgumentException();
                } else {
                    this.consoleId = i;
                    ((ConsoleEngine) commandRegistries[i]).setSystemRegistry(this);
                }
            } else if (commandRegistries[i] instanceof SystemRegistry) {
                throw new IllegalArgumentException();
            }
        }
        SystemRegistry.add(this);
    }

    @Override
    public void initialize(File script) {
        if (consoleId != null) {
            try {
                consoleEngine().execute(script);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    @Override
    public Set<String> commandNames() {
        Set<String> out = new HashSet<>();
        for (CommandRegistry r : commandRegistries) {
            out.addAll(r.commandNames());
        }
        return out;
    }

    @Override
    public Map<String, String> commandAliases() {
        Map<String, String> out = new HashMap<>();
        for (CommandRegistry r : commandRegistries) {
            out.putAll(r.commandAliases());
        }
        return out;
    }

    @Override
    public List<String> commandInfo(String command) {
        int id = registryId(command);
        return id > -1 ? commandRegistries[id].commandInfo(command) : new ArrayList<>();
    }

    @Override
    public boolean hasCommand(String command) {
        return registryId(command) > -1;
    }

    @Override
    public Completers.SystemCompleter compileCompleters() {
        return CommandRegistry.compileCompleters(commandRegistries);
    }

    @Override
    public Widgets.CmdDesc commandDescription(String command) {
        int id = registryId(command);
        return id > -1 ? commandRegistries[id].commandDescription(command) : new Widgets.CmdDesc(false);
    }

    @Override
    public Object invoke(String command, Object... args) throws Exception {
        Object out = null;
        if (command.startsWith(":")) {
            command = command.substring(1);
        }
        int id = registryId(command);
        if (id > -1) {
            out = commandRegistries[id].invoke(command, args);
        }
        return out;
    }

    @Override
    public Object execute(ParsedLine pl) throws Exception {
        String[] argv = pl.words().subList(1, pl.words().size()).toArray(new String[0]);
        String cmd = Parser.getCommand(pl.word());
        Object out = null;
        if (cmd.startsWith(":")) {
            cmd = cmd.substring(1);
        }
        if ("help".equals(cmd) || "?".equals(cmd)) {
            help();
        } else {
            int id = registryId(cmd);
            if (id > -1) {
                out = commandRegistries[id].execute(cmd, argv);
                if (consoleId != null) {
                    out = consoleEngine().postProcess(pl.line(), out);
                }
            } else if (consoleId != null) {
                out = consoleEngine().execute(pl);
            }
        }
        return out;
    }

    private ConsoleEngine consoleEngine() {
        return consoleId != null ? (ConsoleEngine) commandRegistries[consoleId] : null;
    }

    private void help() {
        System.out.println("List of available commands:");
        for (CommandRegistry r : commandRegistries) {
            TreeSet<String> commands = new TreeSet<>(r.commandNames());
            AttributedStringBuilder asb = new AttributedStringBuilder().tabs(2);
            asb.append("\t");
            asb.append(r.getClass().getSimpleName());
            asb.append(":");
            System.out.println(asb.toString());
            for (String c : commands) {
                asb = new AttributedStringBuilder().tabs(Arrays.asList(4, 20));
                asb.append("\t");
                asb.append(c);
                asb.append("\t");
                asb.append(r.commandInfo(c).size() > 0 ? r.commandInfo(c).get(0) : " ");
                System.out.println(asb.toString());
            }
        }
        System.out.println("  Additional help:");
        System.out.println("    <command> --help");
    }

    private int registryId(String command) {
        for (int i = 0; i < commandRegistries.length; i++) {
            if (commandRegistries[i].hasCommand(command)) {
                return i;
            }
        }
        return -1;
    }
}
