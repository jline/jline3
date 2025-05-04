/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jline.builtins.Commands;
import org.jline.builtins.Completers.FilesCompleter;
import org.jline.builtins.Completers.OptDesc;
import org.jline.builtins.Completers.OptionCompleter;
import org.jline.builtins.ConfigurationPath;
import org.jline.builtins.SyntaxHighlighter;
import org.jline.builtins.TTop;
import org.jline.console.CommandInput;
import org.jline.console.CommandMethods;
import org.jline.console.CommandRegistry;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;
import org.jline.reader.Widget;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;

import static org.jline.builtins.SyntaxHighlighter.DEFAULT_NANORC_FILE;
import static org.jline.builtins.SyntaxHighlighter.TYPE_NANORCTHEME;

/**
 * Implementation of CommandRegistry that provides built-in commands for JLine.
 * <p>
 * The Builtins class provides a set of common commands that are useful in any
 * JLine-based console application, such as:
 * <ul>
 *   <li>File editing (nano)</li>
 *   <li>File viewing (less)</li>
 *   <li>Command history management</li>
 *   <li>Widget and keymap configuration</li>
 *   <li>Terminal and system information display</li>
 * </ul>
 * <p>
 * This class creates tab completers, executes commands, and provides descriptions
 * for these built-in commands.
 *
 */
public class Builtins extends JlineCommandRegistry implements CommandRegistry {
    /**
     * Enumeration of built-in commands provided by this class.
     */
    public enum Command {
        NANO,
        LESS,
        HISTORY,
        WIDGET,
        KEYMAP,
        SETOPT,
        SETVAR,
        UNSETOPT,
        TTOP,
        COLORS,
        HIGHLIGHTER
    }

    private final ConfigurationPath configPath;
    private final Function<String, Widget> widgetCreator;
    private final Supplier<Path> workDir;
    private LineReader reader;

    public Builtins(Path workDir, ConfigurationPath configPath, Function<String, Widget> widgetCreator) {
        this(null, () -> workDir, configPath, widgetCreator);
    }

    public Builtins(
            Set<Command> commands, Path workDir, ConfigurationPath configpath, Function<String, Widget> widgetCreator) {
        this(commands, () -> workDir, configpath, widgetCreator);
    }

    public Builtins(Supplier<Path> workDir, ConfigurationPath configPath, Function<String, Widget> widgetCreator) {
        this(null, workDir, configPath, widgetCreator);
    }

    @SuppressWarnings("this-escape")
    public Builtins(
            Set<Command> commands,
            Supplier<Path> workDir,
            ConfigurationPath configpath,
            Function<String, Widget> widgetCreator) {
        super();
        Objects.requireNonNull(configpath);
        this.configPath = configpath;
        this.widgetCreator = widgetCreator;
        this.workDir = workDir;
        Set<Command> cmds;
        Map<Command, String> commandName = new HashMap<>();
        Map<Command, CommandMethods> commandExecute = new HashMap<>();
        if (commands == null) {
            cmds = new HashSet<>(EnumSet.allOf(Command.class));
        } else {
            cmds = new HashSet<>(commands);
        }
        for (Command c : cmds) {
            commandName.put(c, c.name().toLowerCase());
        }
        commandExecute.put(Command.NANO, new CommandMethods(this::nano, this::nanoCompleter));
        commandExecute.put(Command.LESS, new CommandMethods(this::less, this::lessCompleter));
        commandExecute.put(Command.HISTORY, new CommandMethods(this::history, this::historyCompleter));
        commandExecute.put(Command.WIDGET, new CommandMethods(this::widget, this::widgetCompleter));
        commandExecute.put(Command.KEYMAP, new CommandMethods(this::keymap, this::defaultCompleter));
        commandExecute.put(Command.SETOPT, new CommandMethods(this::setopt, this::setoptCompleter));
        commandExecute.put(Command.SETVAR, new CommandMethods(this::setvar, this::setvarCompleter));
        commandExecute.put(Command.UNSETOPT, new CommandMethods(this::unsetopt, this::unsetoptCompleter));
        commandExecute.put(Command.TTOP, new CommandMethods(this::ttop, this::defaultCompleter));
        commandExecute.put(Command.COLORS, new CommandMethods(this::colors, this::defaultCompleter));
        commandExecute.put(Command.HIGHLIGHTER, new CommandMethods(this::highlighter, this::highlighterCompleter));
        registerCommands(commandName, commandExecute);
    }

    public void setLineReader(LineReader reader) {
        this.reader = reader;
    }

    private void less(CommandInput input) {
        try {
            Commands.less(
                    input.terminal(), input.in(), input.out(), input.err(), workDir.get(), input.xargs(), configPath);
        } catch (Exception e) {
            saveException(e);
        }
    }

    private void nano(CommandInput input) {
        try {
            Commands.nano(input.terminal(), input.out(), input.err(), workDir.get(), input.args(), configPath);
        } catch (Exception e) {
            saveException(e);
        }
    }

    private void history(CommandInput input) {
        try {
            Commands.history(reader, input.out(), input.err(), workDir.get(), input.args());
        } catch (Exception e) {
            saveException(e);
        }
    }

    private void widget(CommandInput input) {
        try {
            Commands.widget(reader, input.out(), input.err(), widgetCreator, input.args());
        } catch (Exception e) {
            saveException(e);
        }
    }

    private void keymap(CommandInput input) {
        try {
            Commands.keymap(reader, input.out(), input.err(), input.args());
        } catch (Exception e) {
            saveException(e);
        }
    }

    private void setopt(CommandInput input) {
        try {
            Commands.setopt(reader, input.out(), input.err(), input.args());
        } catch (Exception e) {
            saveException(e);
        }
    }

    private void setvar(CommandInput input) {
        try {
            Commands.setvar(reader, input.out(), input.err(), input.args());
        } catch (Exception e) {
            saveException(e);
        }
    }

    private void unsetopt(CommandInput input) {
        try {
            Commands.unsetopt(reader, input.out(), input.err(), input.args());
        } catch (Exception e) {
            saveException(e);
        }
    }

    private void ttop(CommandInput input) {
        try {
            TTop.ttop(input.terminal(), input.out(), input.err(), input.args());
        } catch (Exception e) {
            saveException(e);
        }
    }

    private void colors(CommandInput input) {
        try {
            Commands.colors(input.terminal(), input.out(), input.args());
        } catch (Exception e) {
            saveException(e);
        }
    }

    private void highlighter(CommandInput input) {
        try {
            Commands.highlighter(reader, input.terminal(), input.out(), input.err(), input.args(), configPath);
        } catch (Exception e) {
            saveException(e);
        }
    }

    private List<String> unsetOptions(boolean set) {
        List<String> out = new ArrayList<>();
        for (Option option : Option.values()) {
            if (set == (reader.isSet(option) == option.isDef())) {
                out.add((option.isDef() ? "no-" : "")
                        + option.toString().toLowerCase().replace('_', '-'));
            }
        }
        return out;
    }

    private List<Completer> highlighterCompleter(String name) {
        List<Completer> completers = new ArrayList<>();
        List<OptDesc> optDescs = commandOptions(name);
        for (OptDesc o : optDescs) {
            if (o.shortOption() != null
                    && (o.shortOption().equals("-v") || o.shortOption().equals("-s"))) {
                Path userConfig = null;
                if (o.shortOption().equals("-s")) {
                    try {
                        userConfig = configPath.getUserConfig(DEFAULT_NANORC_FILE);
                    } catch (IOException ignore) {
                    }
                }
                if (o.shortOption().equals("-v") || userConfig != null) {
                    Path ct = SyntaxHighlighter.build(configPath.getConfig(DEFAULT_NANORC_FILE), null)
                            .getCurrentTheme();
                    if (ct != null) {
                        o.setValueCompleter(new FilesCompleter(ct.getParent(), "*" + TYPE_NANORCTHEME));
                    }
                }
            }
        }
        completers.add(new ArgumentCompleter(
                NullCompleter.INSTANCE, new OptionCompleter(NullCompleter.INSTANCE, optDescs, 1)));
        return completers;
    }

    private Set<String> allWidgets() {
        Set<String> out = new HashSet<>();
        for (String s : reader.getWidgets().keySet()) {
            out.add(s);
            out.add(reader.getWidgets().get(s).toString());
        }
        return out;
    }

    private List<Completer> nanoCompleter(String name) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(
                NullCompleter.INSTANCE, new OptionCompleter(new FilesCompleter(workDir), this::commandOptions, 1)));
        return completers;
    }

    private List<Completer> lessCompleter(String name) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(
                NullCompleter.INSTANCE, new OptionCompleter(new FilesCompleter(workDir), this::commandOptions, 1)));
        return completers;
    }

    private List<Completer> historyCompleter(String name) {
        List<Completer> completers = new ArrayList<>();
        List<OptDesc> optDescs = commandOptions(name);
        for (OptDesc o : optDescs) {
            if (o.shortOption() != null
                    && (o.shortOption().equals("-A")
                            || o.shortOption().equals("-W")
                            || o.shortOption().equals("-R"))) {
                o.setValueCompleter(new FilesCompleter(workDir));
            }
        }
        completers.add(new ArgumentCompleter(
                NullCompleter.INSTANCE, new OptionCompleter(NullCompleter.INSTANCE, optDescs, 1)));
        return completers;
    }

    private List<Completer> widgetCompleter(String name) {
        List<Completer> completers = new ArrayList<>();
        List<OptDesc> optDescs = commandOptions(name);
        Candidate aliasOption = new Candidate("-A", "-A", null, null, null, null, true);
        Iterator<OptDesc> i = optDescs.iterator();
        while (i.hasNext()) {
            OptDesc o = i.next();
            if (o.shortOption() != null) {
                if (o.shortOption().equals("-D")) {
                    o.setValueCompleter(
                            new StringsCompleter(() -> reader.getWidgets().keySet()));
                } else if (o.shortOption().equals("-A")) {
                    aliasOption =
                            new Candidate(o.shortOption(), o.shortOption(), null, o.description(), null, null, true);
                    i.remove();
                }
            }
        }
        completers.add(new ArgumentCompleter(
                NullCompleter.INSTANCE, new OptionCompleter(NullCompleter.INSTANCE, optDescs, 1)));
        completers.add(new ArgumentCompleter(
                NullCompleter.INSTANCE,
                new StringsCompleter(aliasOption),
                new StringsCompleter(this::allWidgets),
                new StringsCompleter(() -> reader.getWidgets().keySet()),
                NullCompleter.INSTANCE));
        return completers;
    }

    private List<Completer> setvarCompleter(String name) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(
                NullCompleter.INSTANCE,
                new StringsCompleter(() -> reader.getVariables().keySet()),
                NullCompleter.INSTANCE));
        return completers;
    }

    private List<Completer> setoptCompleter(String name) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(NullCompleter.INSTANCE, new StringsCompleter(() -> unsetOptions(true))));
        return completers;
    }

    private List<Completer> unsetoptCompleter(String name) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(NullCompleter.INSTANCE, new StringsCompleter(() -> unsetOptions(false))));
        return completers;
    }
}
