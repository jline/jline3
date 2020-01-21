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
import org.jline.builtins.Options.HelpException;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;

/**
 * Aggregate command registeries.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public class SystemRegistryImpl implements SystemRegistry {
    private static final Class<?>[] BUILTIN_REGISTERIES = {Builtins.class, ConsoleEngineImpl.class};
    private final CommandRegistry[] commandRegistries;
    private Integer consoleId = null;
    private Terminal terminal;

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

    public void setTerminal(Terminal terminal) {
        this.terminal = terminal;
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
        } else if (consoleId != null) {
            out = consoleEngine().invoke(command, args);
        }
        return out;
    }

    @Override
    public Object execute(ParsedLine pl) throws Exception {
        if (pl.line().isEmpty() || pl.line().startsWith("#")) {
            return null;
        }
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
                if (consoleId != null) {
                    out = commandRegistries[id].invoke(cmd, consoleEngine().expandParameters(argv));
                    out = consoleEngine().postProcess(pl.line(), out);
                } else {
                    out = commandRegistries[id].execute(cmd, argv);
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

    private boolean isBuiltinRegistry(CommandRegistry registry) {
        for (Class<?> c : BUILTIN_REGISTERIES) {
            if (c == registry.getClass()) {
                return true;
            }
        }
        return false;
    }

    private void printHeader(String header) {
        AttributedStringBuilder asb = new AttributedStringBuilder().tabs(2);
        asb.append("\t");
        asb.append(header, HelpException.defaultStyle().resolve(".ti"));
        asb.append(":");
        asb.toAttributedString().println(terminal);
    }

    private void printCommandInfo(String command, String info, int max) {
        AttributedStringBuilder asb = new AttributedStringBuilder().tabs(Arrays.asList(4, max + 4));
        asb.append("\t");
        asb.append(command, HelpException.defaultStyle().resolve(".co"));
        asb.append("\t");
        asb.append(info);
        asb.setLength(terminal.getWidth());
        asb.toAttributedString().println(terminal);
    }

    private void printCommands(Collection<String> commands, int max) {
        AttributedStringBuilder asb = new AttributedStringBuilder().tabs(Arrays.asList(4, max + 4));
        int col = 0;
        asb.append("\t");
        col += 4;
        boolean done = false;
        for (String c : commands) {
            asb.append(c, HelpException.defaultStyle().resolve(".co"));
            asb.append("\t");
            col += max;
            if (col + max > terminal.getWidth()) {
                asb.toAttributedString().println(terminal);
                asb = new AttributedStringBuilder().tabs(Arrays.asList(4, max + 4));
                col = 0;
                asb.append("\t");
                col += 4;
                done = true;
            } else {
                done = false;
            }
        }
        if (!done) {
            asb.toAttributedString().println(terminal);
        }
    }

    private String doCommandInfo(List<String> info) {
        return info.size() > 0 ? info.get(0) : " ";
    }

    private void help() {
        Set<String> commands = commandNames();
        boolean withInfo = commands.size() < terminal.getHeight() ? true : false;
        int max = Collections.max(commands, Comparator.comparing(String::length)).length() + 1;
        TreeMap<String,String> builtinCommands = new TreeMap<>();
        for (CommandRegistry r : commandRegistries) {
            if (isBuiltinRegistry(r)) {
                for (String c: r.commandNames()) {
                    builtinCommands.put(c, doCommandInfo(r.commandInfo(c)));
                }
            }
        }
        printHeader("Builtins");
        if (withInfo) {
            for (Map.Entry<String, String> entry: builtinCommands.entrySet()) {
                printCommandInfo(entry.getKey(), entry.getValue(), max);
            }
        } else {
            printCommands(builtinCommands.keySet(), max);
        }
        for (CommandRegistry r : commandRegistries) {
            if (isBuiltinRegistry(r)) {
                continue;
            }
            TreeSet<String> cmds = new TreeSet<>(r.commandNames());
            printHeader(r.name());
            if (withInfo) {
                for (String c : cmds) {
                    printCommandInfo(c, doCommandInfo(r.commandInfo(c)), max);
                }
            } else {
                printCommands(cmds, max);
            }
        }
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
