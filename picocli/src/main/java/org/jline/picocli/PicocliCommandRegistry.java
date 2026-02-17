/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.picocli;

import java.io.PrintWriter;
import java.util.*;

import org.jline.console.ArgDesc;
import org.jline.console.CmdDesc;
import org.jline.console.CommandRegistry;
import org.jline.console.impl.DescriptionAdapter;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.impl.completer.SystemCompleter;
import org.jline.shell.CommandDescription;
import org.jline.shell.CommandGroup;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;

/**
 * A {@link CommandRegistry} that bridges picocli {@link CommandLine} subcommands
 * into the JLine console framework.
 * <p>
 * This registry exposes picocli subcommands as JLine commands, providing
 * tab completion, command descriptions for TailTipWidgets, and command execution.
 * <p>
 * Example usage:
 * <pre>
 * CommandLine commandLine = new CommandLine(new MyApp());
 * PicocliCommandRegistry registry = new PicocliCommandRegistry(commandLine);
 *
 * Shell shell = Shell.builder()
 *     .commands(registry)
 *     .build();
 * shell.run();
 * </pre>
 */
@SuppressWarnings("deprecation")
public class PicocliCommandRegistry implements CommandRegistry, CommandGroup {

    private final CommandLine commandLine;
    private LineReader reader;

    @Override
    public String name() {
        return getClass().getSimpleName();
    }

    /**
     * Creates a new registry wrapping the given picocli {@link CommandLine}.
     * The subcommands of the command line become the commands of this registry.
     *
     * @param commandLine the picocli command line whose subcommands to expose
     */
    public PicocliCommandRegistry(CommandLine commandLine) {
        this.commandLine = Objects.requireNonNull(commandLine, "commandLine");
    }

    public void setLineReader(LineReader reader) {
        this.reader = reader;
    }

    @Override
    public Set<String> commandNames() {
        Set<String> names = new LinkedHashSet<>();
        for (Map.Entry<String, CommandLine> entry : commandLine.getSubcommands().entrySet()) {
            // Only include the primary name, not aliases
            String primaryName = entry.getValue().getCommandName();
            if (entry.getKey().equals(primaryName)) {
                names.add(primaryName);
            }
        }
        return names;
    }

    @Override
    public Map<String, String> commandAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        for (Map.Entry<String, CommandLine> entry : commandLine.getSubcommands().entrySet()) {
            String primaryName = entry.getValue().getCommandName();
            String key = entry.getKey();
            if (!key.equals(primaryName)) {
                // This entry is an alias
                aliases.put(key, primaryName);
            }
        }
        return aliases;
    }

    @Override
    public List<String> commandInfo(String command) {
        CommandLine sub = commandLine.getSubcommands().get(command);
        if (sub == null) {
            return Collections.emptyList();
        }
        String[] desc = sub.getCommandSpec().usageMessage().description();
        if (desc.length > 0) {
            return Collections.singletonList(desc[0]);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean hasCommand(String command) {
        return commandLine.getSubcommands().containsKey(command);
    }

    @Override
    public SystemCompleter compileCompleters() {
        SystemCompleter completer = new SystemCompleter();
        for (Map.Entry<String, CommandLine> entry : commandLine.getSubcommands().entrySet()) {
            String name = entry.getKey();
            CommandSpec spec = entry.getValue().getCommandSpec();
            List<String> completionStrings = new ArrayList<>();

            // Add option completions
            for (OptionSpec option : spec.options()) {
                for (String optName : option.names()) {
                    completionStrings.add(optName);
                }
            }

            // Add subcommand completions
            for (String subName : entry.getValue().getSubcommands().keySet()) {
                completionStrings.add(subName);
            }

            Completer argCompleter =
                    new ArgumentCompleter(new StringsCompleter(completionStrings), NullCompleter.INSTANCE);
            completer.add(name, argCompleter);
        }
        return completer;
    }

    @Override
    public CmdDesc commandDescription(List<String> args) {
        if (args == null || args.isEmpty()) {
            return null;
        }
        String command = args.get(0);
        CommandLine sub = commandLine.getSubcommands().get(command);
        if (sub == null) {
            return null;
        }
        CommandSpec spec = sub.getCommandSpec();

        // Main description
        List<AttributedString> mainDesc = new ArrayList<>();
        for (String line : spec.usageMessage().description()) {
            mainDesc.add(new AttributedString(line));
        }

        // Argument descriptions
        List<ArgDesc> argsDesc = new ArrayList<>();
        for (PositionalParamSpec param : spec.positionalParameters()) {
            String paramLabel = param.paramLabel();
            argsDesc.add(new ArgDesc(paramLabel));
        }

        // Option descriptions
        Map<String, List<AttributedString>> optsDesc = new LinkedHashMap<>();
        for (OptionSpec option : spec.options()) {
            String key = String.join(" ", option.names());
            if (option.arity().max() > 0) {
                key += "=" + option.paramLabel();
            }
            List<AttributedString> optDesc = new ArrayList<>();
            for (String desc : option.description()) {
                optDesc.add(new AttributedString(desc));
            }
            optsDesc.put(key, optDesc);
        }

        CmdDesc cmdDesc = new CmdDesc(mainDesc, argsDesc, optsDesc);
        return cmdDesc;
    }

    @Override
    public Object invoke(CommandSession session, String command, Object... args) throws Exception {
        CommandLine sub = commandLine.getSubcommands().get(command);
        if (sub == null) {
            throw new IllegalArgumentException("Unknown command: " + command);
        }

        // Set output streams from session
        Terminal terminal = session.terminal();
        if (terminal != null) {
            sub.setOut(new PrintWriter(terminal.output(), true));
            sub.setErr(new PrintWriter(terminal.output(), true));
        } else {
            sub.setOut(new PrintWriter(session.out(), true));
            sub.setErr(new PrintWriter(session.err(), true));
        }

        String[] argv;
        if (args.length == 1 && args[0] instanceof String[]) {
            argv = (String[]) args[0];
        } else {
            argv = new String[args.length];
            for (int i = 0; i < args.length; i++) {
                argv[i] = args[i] != null ? args[i].toString() : "";
            }
        }
        int exitCode = sub.execute(argv);
        return exitCode;
    }

    // --- CommandGroup implementation ---

    @Override
    public Collection<org.jline.shell.Command> commands() {
        List<org.jline.shell.Command> commands = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map.Entry<String, CommandLine> entry : commandLine.getSubcommands().entrySet()) {
            String primaryName = entry.getValue().getCommandName();
            if (seen.add(primaryName)) {
                CommandLine sub = entry.getValue();
                commands.add(new PicocliCommand(primaryName, sub));
            }
        }
        return commands;
    }

    /**
     * A {@link org.jline.shell.Command} wrapper around a picocli subcommand.
     */
    private class PicocliCommand implements org.jline.shell.Command {

        private final String name;
        private final CommandLine sub;

        PicocliCommand(String name, CommandLine sub) {
            this.name = name;
            this.sub = sub;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public List<String> aliases() {
            List<String> aliases = new ArrayList<>();
            for (Map.Entry<String, CommandLine> entry :
                    commandLine.getSubcommands().entrySet()) {
                if (entry.getValue() == sub && !entry.getKey().equals(name)) {
                    aliases.add(entry.getKey());
                }
            }
            return aliases;
        }

        @Override
        public String description() {
            String[] desc = sub.getCommandSpec().usageMessage().description();
            return desc.length > 0 ? desc[0] : "";
        }

        @Override
        public CommandDescription describe(List<String> args) {
            CmdDesc cmdDesc = commandDescription(args);
            return DescriptionAdapter.toCommandDescription(cmdDesc);
        }

        @Override
        public Object execute(org.jline.shell.CommandSession session, String[] args) throws Exception {
            // Set output streams from session
            Terminal terminal = session.terminal();
            if (terminal != null) {
                sub.setOut(new PrintWriter(terminal.output(), true));
                sub.setErr(new PrintWriter(terminal.output(), true));
            } else {
                sub.setOut(new PrintWriter(session.out(), true));
                sub.setErr(new PrintWriter(session.err(), true));
            }
            int exitCode = sub.execute(args);
            return exitCode;
        }

        @Override
        public List<Completer> completers() {
            CommandSpec spec = sub.getCommandSpec();
            List<String> completionStrings = new ArrayList<>();
            for (OptionSpec option : spec.options()) {
                for (String optName : option.names()) {
                    completionStrings.add(optName);
                }
            }
            for (String subName : sub.getSubcommands().keySet()) {
                completionStrings.add(subName);
            }
            return List.of(new ArgumentCompleter(new StringsCompleter(completionStrings), NullCompleter.INSTANCE));
        }
    }

    /**
     * Returns the underlying picocli {@link CommandLine}.
     *
     * @return the command line
     */
    public CommandLine getCommandLine() {
        return commandLine;
    }
}
