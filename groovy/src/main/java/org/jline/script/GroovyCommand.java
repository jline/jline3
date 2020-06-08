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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jline.console.CommandInput;
import org.jline.console.CommandMethods;
import org.jline.console.impl.AbstractCommandRegistry;
import org.jline.builtins.Completers.OptDesc;
import org.jline.builtins.Completers.OptionCompleter;
import org.jline.console.CmdDesc;
import org.jline.console.CommandRegistry;
import org.jline.console.Printer;
import org.jline.groovy.ObjectInspector;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.utils.AttributedString;

import groovy.console.ui.Console;
import groovy.console.ui.ObjectBrowser;

public class GroovyCommand extends AbstractCommandRegistry implements CommandRegistry {
    public enum Command {INSPECT, CONSOLE, GRAB}
    private GroovyEngine engine;
    private Printer printer;
    private final Map<Command,CmdDesc> commandDescs = new HashMap<>();
    private final Map<Command,List<String>> commandInfos = new HashMap<>();
    private boolean consoleUi;
    private boolean ivy;

    public GroovyCommand(GroovyEngine engine, Printer printer) {
        this(null, engine, printer);
    }

    public GroovyCommand(Set<Command> commands, GroovyEngine engine, Printer printer) {
        this.engine = engine;
        this.printer = printer;
        try {
            Class.forName("groovy.console.ui.ObjectBrowser");
            consoleUi = true;
        } catch (Exception e) {
        }
        try {
            Class.forName("org.apache.ivy.util.Message");
            System.setProperty("groovy.grape.report.downloads","true");
            ivy = true;
        } catch (Exception e) {
        }
        Set<Command> cmds = new HashSet<>();
        Map<Command,String> commandName = new HashMap<>();
        Map<Command,CommandMethods> commandExecute = new HashMap<>();
        if (commands == null) {
            cmds = new HashSet<>(EnumSet.allOf(Command.class));
        } else {
            cmds = new HashSet<>(commands);
        }
        if (!consoleUi) {
            cmds.remove(Command.CONSOLE);
        }
        if (!ivy) {
            cmds.remove(Command.GRAB);
        }
        for (Command c: cmds) {
            commandName.put(c, c.name().toLowerCase());
        }
        commandExecute.put(Command.INSPECT, new CommandMethods(this::inspect, this::inspectCompleter));
        commandExecute.put(Command.CONSOLE, new CommandMethods(this::console, this::defaultCompleter));
        commandExecute.put(Command.GRAB, new CommandMethods(this::grab, this::defaultCompleter));
        registerCommands(commandName, commandExecute);
        commandDescs.put(Command.INSPECT, inspectCmdDesc());
        commandDescs.put(Command.CONSOLE, consoleCmdDesc());
        commandDescs.put(Command.GRAB, grabCmdDesc());
    }

    @Override
    public List<String> commandInfo(String command) {
        Command cmd = (Command)registeredCommand(command);
        return commandInfos.get(cmd);
    }

    @Override
    public CmdDesc commandDescription(List<String> args) {
        String command = args != null && !args.isEmpty() ? args.get(0) : "";
        Command cmd = (Command)registeredCommand(command);
        return commandDescs.get(cmd);
    }

    @SuppressWarnings("unchecked")
    public Object grab(CommandInput input) {
        if (input.args().length != 1) {
            throw new IllegalArgumentException("Wrong number of command parameters: " + input.args().length);
        }
        try {
            String arg = input.args()[0];
            if (arg.equals("-?") || arg.equals("--help")) {
                printer.println(helpDesc(Command.GRAB));
            } else if (arg.equals("-l") || arg.equals("--list")) {
                Object resp = engine.execute("groovy.grape.Grape.getInstance().enumerateGrapes()");
                Map<String, Object> options = new HashMap<>();
                options.put(Printer.SKIP_DEFAULT_OPTIONS, true);
                options.put(Printer.MAX_DEPTH, 1);
                options.put(Printer.INDENTION, 4);
                options.put(Printer.VALUE_STYLE, "classpath:/org/jline/groovy/gron.nanorc");
                printer.println(options, resp);
            } else if (arg.startsWith("-")) {
                throw new IllegalArgumentException("Unknown command option: " + arg);
            } else {
                Map<String, String> artifact = new HashMap<>();
                Object xarg = input.xargs()[0];
                if (xarg instanceof String) {
                    String[] vals = input.args()[0].split(":");
                    if (vals.length != 3) {
                        throw new IllegalArgumentException("Invalid command parameter: " + input.args()[0]);
                    }
                    artifact.put("group", vals[0]);
                    artifact.put("module", vals[1]);
                    artifact.put("version", vals[2]);
                } else if (xarg instanceof Map) {
                    artifact = (Map<String, String>) xarg;
                } else {
                    throw new IllegalArgumentException("Unknown command parameter: " + xarg);
                }
                engine.put("_artifact", artifact);
                engine.execute("groovy.grape.Grape.grab(_artifact)");
            }
        } catch (Exception e) {
            saveException(e);
        }
        return null;
    }

    public void console(CommandInput input) {
        if (input.args().length > 1) {
            throw new IllegalArgumentException("Wrong number of command parameters: " + input.args().length);
        }
        if (input.args().length == 1) {
            String arg = input.args()[0];
            if (arg.equals("-?") || arg.equals("--help")) {
                printer.println(helpDesc(Command.CONSOLE));
                return;
            } else {
                throw new IllegalArgumentException("Unknown command parameter: " + input.args()[0]);
            }
        }
        Console c = new Console(engine.sharedData);
        c.run();
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
            printer.println(helpDesc(Command.INSPECT));
            return null;
        }
        int id = 0;
        if (idx >= 0) {
            id = idx == 0 ? 1 : 0;
        }
        if (input.args().length < id + 1) {
            throw new IllegalArgumentException("Wrong number of command parameters: " + input.args().length);
        }
        try {
            Object obj = input.xargs()[id];
            ObjectInspector inspector = new ObjectInspector(obj);
            Object out = null;
            Map<String,Object> options = new HashMap<>();
            if (option.equals("-m") || option.equals("--methods")) {
                out = inspector.methods();
            } else if (option.equals("-n") || option.equals("--metaMethods")) {
                out = inspector.metaMethods();
            } else if (option.equals("-i") || option.equals("--info")) {
                out = inspector.properties();
                options.put(Printer.VALUE_STYLE, "classpath:/org/jline/groovy/gron.nanorc");
            } else if (consoleUi && (option.equals("-g") || option.equals("--gui"))) {
                ObjectBrowser.inspect(obj);
            } else {
                throw new IllegalArgumentException("Unknown option: " + option);
            }
            options.put(Printer.SKIP_DEFAULT_OPTIONS, true);
            options.put(Printer.COLUMNS, ObjectInspector.METHOD_COLUMNS);
            options.put(Printer.MAX_DEPTH, 1);
            options.put(Printer.INDENTION, 4);
            printer.println(options, out);
        } catch (Exception e) {
            saveException(e);
        }
        return null;
    }

    private CmdDesc helpDesc(Command command) {
        return doHelpDesc(command.toString().toLowerCase(), commandInfos.get(command), commandDescs.get(command));
    }

    private CmdDesc grabCmdDesc() {
        Map<String,List<AttributedString>> optDescs = new HashMap<>();
        optDescs.put("-? --help", doDescription ("Displays command help"));
        optDescs.put("-l --list", doDescription ("List the modules in the cache"));
        CmdDesc out = new CmdDesc(new ArrayList<>(), optDescs);
        List<AttributedString> mainDesc = new ArrayList<>();
        List<String> info = new ArrayList<>();
        info.add("Add maven repository dependencies to classpath");
        commandInfos.put(Command.GRAB, info);
        mainDesc.add(new AttributedString("grab <group>:<artifact>:<version>"));
        mainDesc.add(new AttributedString("grab --list"));
        out.setMainDesc(mainDesc);
        out.setHighlighted(false);
        return out;
    }

    private CmdDesc consoleCmdDesc() {
        Map<String,List<AttributedString>> optDescs = new HashMap<>();
        optDescs.put("-? --help", doDescription ("Displays command help"));
        CmdDesc out = new CmdDesc(new ArrayList<>(), optDescs);
        List<AttributedString> mainDesc = new ArrayList<>();
        List<String> info = new ArrayList<>();
        info.add("Launch Groovy console");
        commandInfos.put(Command.CONSOLE, info);
        mainDesc.add(new AttributedString("console"));
        out.setMainDesc(mainDesc);
        out.setHighlighted(false);
        return out;
    }

    private CmdDesc inspectCmdDesc() {
        Map<String,List<AttributedString>> optDescs = new HashMap<>();
        optDescs.put("-? --help", doDescription ("Displays command help"));
        if (consoleUi) {
            optDescs.put("-g --gui", doDescription ("Launch object browser"));
        }
        optDescs.put("-i --info", doDescription ("Object class info"));
        optDescs.put("-m --methods", doDescription ("List object methods"));
        optDescs.put("-n --metaMethods", doDescription ("List object metaMethods"));
        CmdDesc out = new CmdDesc(new ArrayList<>(), optDescs);
        List<AttributedString> mainDesc = new ArrayList<>();
        List<String> info = new ArrayList<>();
        info.add("Display object info on terminal");
        commandInfos.put(Command.INSPECT, info);
        mainDesc.add(new AttributedString("inspect [OPTION] OBJECT"));
        out.setMainDesc(mainDesc);
        out.setHighlighted(false);
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

    private List<OptDesc> compileOptDescs(String command) {
        List<OptDesc> out = new ArrayList<>();
        Command cmd = Command.valueOf(command.toUpperCase());
        for (Map.Entry<String,List<AttributedString>> entry : commandDescs.get(cmd).getOptsDesc().entrySet()) {
            String[] option = entry.getKey().split("\\s+");
            String desc = entry.getValue().get(0).toString();
            if (option.length == 2) {
                out.add(new OptDesc(option[0], option[1], desc));
            } else if (option[0].charAt(1) == '-') {
                out.add(new OptDesc(null, option[0], desc));
            } else {
                out.add(new OptDesc(option[0], null, desc));
            }
        }
        return out;
    }

    private List<Completer> inspectCompleter(String command) {
        List<Completer> out = new ArrayList<>();
        ArgumentCompleter ac = new ArgumentCompleter(NullCompleter.INSTANCE
                                   , new OptionCompleter(Arrays.asList(new StringsCompleter(this::variables), NullCompleter.INSTANCE)
                                                       , this::compileOptDescs, 1)
                                   );
        out.add(ac);
        return out;
    }

    private List<Completer> defaultCompleter(String command) {
        List<Completer> out = new ArrayList<>();
        ArgumentCompleter ac = new ArgumentCompleter(NullCompleter.INSTANCE
                                    , new OptionCompleter(NullCompleter.INSTANCE
                                                        , this::compileOptDescs, 1)
                                    );
        out.add(ac);
        return out;
    }
}
