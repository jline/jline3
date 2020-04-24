/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.script;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jline.console.CommandInput;
import org.jline.console.CommandMethods;
import org.jline.console.AbstractCommandRegistry;
import org.jline.console.CmdDesc;
import org.jline.console.CommandRegistry;
import org.jline.console.Printer;
import org.jline.groovy.ObjectInspector;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.utils.AttributedString;

public class GroovyCommand extends AbstractCommandRegistry implements CommandRegistry {
    public enum Command {INSPECT}
    private GroovyEngine engine;
    private Printer printer;
    private final Map<Command,CmdDesc> commandDescs = new HashMap<>();
    private final Map<Command,List<String>> commandInfos = new HashMap<>();

    public GroovyCommand(GroovyEngine engine, Printer printer) {
        this(null, engine, printer);
    }

    public GroovyCommand(Set<Command> commands, GroovyEngine engine, Printer printer) {
        this.engine = engine;
        this.printer = printer;
        Set<Command> cmds = new HashSet<>();
        Map<Command,String> commandName = new HashMap<>();
        Map<Command,CommandMethods> commandExecute = new HashMap<>();
        if (commands == null) {
            cmds = new HashSet<>(EnumSet.allOf(Command.class));
        } else {
            cmds = new HashSet<>(commands);
        }
        for (Command c: cmds) {
            commandName.put(c, c.name().toLowerCase());
        }
        commandExecute.put(Command.INSPECT, new CommandMethods(this::inspect, this::inspectCompleter));
        registerCommands(commandName, commandExecute);
        commandDescs.put(Command.INSPECT, inspectCmdDesc());
    }

    @Override
    public List<String> commandInfo(String command) {
        Command cmd = (Command)registeredCommand(command);
        return commandInfos.get(cmd);
    }

    @Override
    public CmdDesc commandDescription(String command) {
        Command cmd = (Command)registeredCommand(command);
        return commandDescs.get(cmd);
    }

    public Object inspect(CommandInput input) {
        if (input.xargs().length == 0) {
            return null;
        }
        if (input.args().length > 2) {
            throw new IllegalArgumentException("Wrong number of command parameters: " + input.args().length);
        }
        int idx = optionIdx(Command.INSPECT, input.args());
        String option = idx < 0 ? "--info" : input.args()[idx];
        if (option.equals("-?") || option.equals("--help")) {
            printer.println(commandDescs.get(Command.INSPECT));
            return null;
        }
        int id = 0;
        if (idx >= 0) {
            id = idx == 0 ? 1 : 0;
        }
        if (input.args().length < id + 1) {
            throw new IllegalArgumentException("Wrong number of command parameters: " + input.args().length);
        }
        Object obj = input.xargs()[id];
        ObjectInspector inspector = new ObjectInspector(obj);
        Object out = option;
        if (option.equals("-m") || option.equals("--methods")) {
            out = inspector.methods();
        } else if (option.equals("-n") || option.equals("--metaMethods")) {
            out = inspector.metaMethods();
        } else if (option.equals("-i") || option.equals("--info")) {
            out = inspector.properties();
        } else if (option.equals("-g") || option.equals("--gui")) {
//            groovy.inspect.swingui.ObjectBrowser.inspect(obj);
        } else {
            throw new IllegalArgumentException("Unknown option: " + option);
        }
        Map<String,Object> options = new HashMap<>();
        options.put(Printer.SKIP_DEFAULT_OPTIONS, true);
        options.put(Printer.COLUMNS, ObjectInspector.METHOD_COLUMNS);
        printer.println(options, out);
        return null;
    }

    private CmdDesc inspectCmdDesc() {
        Map<String,List<AttributedString>> optDescs = new HashMap<>();
        optDescs.put("-? --help", doDescription ("Displays command help"));
        optDescs.put("-g --gui", doDescription ("Open object browser"));
        optDescs.put("-i --info", doDescription ("Object class info"));
        optDescs.put("-m --methods", doDescription ("List object methods"));
        optDescs.put("-n --metaMethods", doDescription ("List object metaMethads"));
        CmdDesc out = new CmdDesc(new ArrayList<>(), optDescs);
        List<AttributedString> mainDesc = new ArrayList<>();
        List<String> info = new ArrayList<>();
        info.add("Display object info on terminal");
        commandInfos.put(Command.INSPECT, info);
        mainDesc.add(new AttributedString("inspect -  " + info.get(0)));
        mainDesc.add(new AttributedString("Usage: inspect [OPTION] OBJECT"));
        out.setMainDesc(mainDesc);
        return out;
    }

    private List<AttributedString> doDescription(String description) {
        List<AttributedString> out = new ArrayList<>();
        out.add(new AttributedString(description));
        return out;
    }

    private int optionIdx(Command cmd, String[] args) {
        for (String a : args) {
            int out = 0;
            if (a.startsWith("-")) {
                return out;
            }
            out++;
        }
        return -1;
    }

    private List<String> variables() {
        List<String> out = new ArrayList<>();
        for (String v : engine.find(null).keySet()) {
            out.add("$" + v);
        }
        return out;
    }

    public List<Completer> inspectCompleter(String command) {
        List<Completer> out = new ArrayList<>();
        ArgumentCompleter ac = new ArgumentCompleter(NullCompleter.INSTANCE
                                                   , new StringsCompleter("--help", "--methods", "--metaMethods", "--gui", "--info"
                                                                         ,"-?", "-n", "-m", "-i", "-g")
                                                   , new StringsCompleter(this::variables)
                                                   , NullCompleter.INSTANCE);
        out.add(ac);
        return out;
    }
}
