/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.picocli;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.jline.builtins.Completers.OptDesc;
import org.jline.console.CmdDesc;
import org.jline.console.CommandContext;
import org.jline.console.CommandRegistry;
import org.jline.console.CommandSession;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.SystemCompleter;
import org.jline.utils.Log;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.shell.jline3.PicocliJLineCompleter;

/**
 * A CommandRegistry implementation that integrates Picocli commands with JLine.
 * <p>
 * This registry provides seamless integration between Picocli's command-line parsing
 * and JLine's interactive features, including:
 * <ul>
 *   <li>Automatic command registration from Picocli @Command annotated classes</li>
 *   <li>Context injection into commands that need access to terminal and environment</li>
 *   <li>Smart completion using Picocli's built-in completion capabilities</li>
 *   <li>Rich help and description generation from Picocli annotations</li>
 * </ul>
 * <p>
 * Usage example:
 * <pre>{@code
 * // Create context
 * CommandContext context = CommandContext.builder()
 *     .terminal(terminal)
 *     .currentDir(Paths.get("."))
 *     .build();
 * 
 * // Create registry and register commands
 * PicocliCommandRegistry registry = new PicocliCommandRegistry(context)
 *     .register(new MyCommand())
 *     .register(AnotherCommand.class);
 * 
 * // Use with SystemRegistry
 * SystemRegistry systemRegistry = new SystemRegistryImpl(parser, terminal, workDir, configPath);
 * systemRegistry.setCommandRegistries(registry);
 * }</pre>
 */
public class PicocliCommandRegistry implements CommandRegistry {

    private final CommandContext context;
    private final Map<String, CommandLine> commands = new ConcurrentHashMap<>();
    private final Map<String, String> aliases = new ConcurrentHashMap<>();
    private final PicocliJLineCompleter completer;
    private CommandLine rootCommandLine;

    /**
     * Creates a new PicocliCommandRegistry with the given context.
     * @param context the command execution context
     * @throws IllegalStateException if Picocli is not available on the classpath
     */
    public PicocliCommandRegistry(CommandContext context) {
        checkPicocliAvailable();
        this.context = context;
        // Create a root command line for completion
        this.rootCommandLine = new CommandLine(new RootCommand());
        this.completer = new PicocliJLineCompleter(rootCommandLine.getCommandSpec());
    }

    private static void checkPicocliAvailable() {
        try {
            Class.forName("picocli.CommandLine");
            Class.forName("picocli.shell.jline3.PicocliJLineCompleter");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                "Picocli integration requires picocli and picocli-shell-jline3 on the classpath", e);
        }
    }

    /**
     * Registers a command instance.
     * The command will be enhanced with context injection if it supports it.
     * @param command the command instance to register
     * @return this registry for method chaining
     */
    public PicocliCommandRegistry register(Object command) {
        CommandLine commandLine = new CommandLine(command);
        injectContext(command, commandLine);
        registerCommandLine(commandLine);
        return this;
    }

    /**
     * Registers a command class.
     * The class will be instantiated with context injection if possible.
     * @param commandClass the command class to register
     * @return this registry for method chaining
     */
    public PicocliCommandRegistry register(Class<?> commandClass) {
        try {
            Object command = createCommandInstance(commandClass);
            return register(command);
        } catch (Exception e) {
            Log.warn("Failed to register command class: " + commandClass.getName(), e);
            return this;
        }
    }

    /**
     * Registers multiple commands at once.
     * @param commands the commands to register (can be instances or classes)
     * @return this registry for method chaining
     */
    public PicocliCommandRegistry registerAll(Object... commands) {
        for (Object command : commands) {
            if (command instanceof Class) {
                register((Class<?>) command);
            } else {
                register(command);
            }
        }
        return this;
    }

    /**
     * Scans packages for @Command annotated classes and registers them.
     * @param packageNames the package names to scan
     * @return this registry for method chaining
     */
    public PicocliCommandRegistry scanPackages(String... packageNames) {
        for (String packageName : packageNames) {
            try {
                scanPackage(packageName);
            } catch (Exception e) {
                Log.warn("Failed to scan package: " + packageName, e);
            }
        }
        return this;
    }

    private void scanPackage(String packageName) {
        // This is a simplified implementation - in a real scenario you'd use
        // a proper classpath scanner like Reflections or Spring's ClassPathScanningCandidateComponentProvider
        try {
            String packagePath = packageName.replace('.', '/');
            java.net.URL packageUrl = Thread.currentThread().getContextClassLoader().getResource(packagePath);
            if (packageUrl != null) {
                java.io.File packageDir = new java.io.File(packageUrl.toURI());
                if (packageDir.exists() && packageDir.isDirectory()) {
                    scanDirectory(packageDir, packageName);
                }
            }
        } catch (Exception e) {
            Log.debug("Could not scan package: " + packageName, e);
        }
    }

    private void scanDirectory(java.io.File dir, String packageName) {
        java.io.File[] files = dir.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
                if (file.isFile() && file.getName().endsWith(".class")) {
                    String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                    try {
                        Class<?> clazz = Class.forName(className);
                        if (clazz.isAnnotationPresent(Command.class)) {
                            register(clazz);
                        }
                    } catch (Exception e) {
                        Log.debug("Could not load class: " + className, e);
                    }
                }
            }
        }
    }

    private Object createCommandInstance(Class<?> commandClass) throws Exception {
        // Try constructor injection first
        Constructor<?>[] constructors = commandClass.getConstructors();
        for (Constructor<?> constructor : constructors) {
            Parameter[] params = constructor.getParameters();
            if (params.length == 1 && CommandContext.class.isAssignableFrom(params[0].getType())) {
                return constructor.newInstance(context);
            }
        }
        
        // Fall back to default constructor
        Object instance = commandClass.getDeclaredConstructor().newInstance();
        return instance;
    }

    private void injectContext(Object command, CommandLine commandLine) {
        // Try field injection
        injectContextIntoFields(command);
        
        // Configure custom execution strategy for method parameter injection
        commandLine.setExecutionStrategy(new ContextInjectingExecutionStrategy(context));
    }

    private void injectContextIntoFields(Object command) {
        Class<?> clazz = command.getClass();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (CommandContext.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        field.set(command, context);
                    } catch (Exception e) {
                        Log.debug("Failed to inject context into field: " + field.getName(), e);
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    private void registerCommandLine(CommandLine commandLine) {
        CommandSpec spec = commandLine.getCommandSpec();
        String name = spec.name();
        
        commands.put(name, commandLine);
        
        // Register aliases
        for (String alias : spec.aliases()) {
            aliases.put(alias, name);
        }
        
        // Add to root command for completion
        rootCommandLine.addSubcommand(name, commandLine);
        
        // Update completer
        this.completer.setCommandSpec(rootCommandLine.getCommandSpec());
    }

    @Override
    public Set<String> commandNames() {
        return new HashSet<>(commands.keySet());
    }

    @Override
    public Map<String, String> commandAliases() {
        return new HashMap<>(aliases);
    }

    @Override
    public boolean hasCommand(String command) {
        return commands.containsKey(command) || aliases.containsKey(command);
    }

    @Override
    public List<String> commandInfo(String command) {
        CommandLine commandLine = getCommandLine(command);
        if (commandLine == null) {
            return Collections.emptyList();
        }
        
        CommandSpec spec = commandLine.getCommandSpec();
        List<String> info = new ArrayList<>();
        
        if (spec.usageMessage().description().length > 0) {
            info.addAll(Arrays.asList(spec.usageMessage().description()));
        } else {
            info.add(spec.name() + " - " + (spec.usageMessage().header().length > 0 
                    ? spec.usageMessage().header()[0] : "Picocli command"));
        }
        
        return info;
    }

    @Override
    public CmdDesc commandDescription(List<String> args) {
        if (args.isEmpty()) {
            return null;
        }
        
        String command = args.get(0);
        CommandLine commandLine = getCommandLine(command);
        if (commandLine == null) {
            return null;
        }
        
        return PicocliCmdDescGenerator.generate(commandLine.getCommandSpec());
    }

    @Override
    public List<OptDesc> commandOptions(String command) {
        CommandLine commandLine = getCommandLine(command);
        if (commandLine == null) {
            return Collections.emptyList();
        }
        
        return PicocliOptDescGenerator.generate(commandLine.getCommandSpec());
    }

    @Override
    public SystemCompleter compileCompleters() {
        SystemCompleter systemCompleter = new SystemCompleter();
        
        for (String command : commands.keySet()) {
            systemCompleter.add(command, completer);
        }
        
        return systemCompleter;
    }

    @Override
    public Object invoke(CommandSession session, String command, Object... args) throws Exception {
        CommandLine commandLine = getCommandLine(command);
        if (commandLine == null) {
            throw new IllegalArgumentException("Unknown command: " + command);
        }
        
        // Convert args to string array
        String[] stringArgs = Arrays.stream(args)
                .map(arg -> arg != null ? arg.toString() : "")
                .toArray(String[]::new);
        
        // Execute the command
        return commandLine.execute(stringArgs);
    }

    private CommandLine getCommandLine(String command) {
        CommandLine commandLine = commands.get(command);
        if (commandLine == null) {
            String realCommand = aliases.get(command);
            if (realCommand != null) {
                commandLine = commands.get(realCommand);
            }
        }
        return commandLine;
    }

    /**
     * Creates a builder for PicocliCommandRegistry.
     * @param context the command execution context
     * @return a new builder instance
     */
    public static Builder builder(CommandContext context) {
        return new Builder(context);
    }

    /**
     * Builder for PicocliCommandRegistry with fluent configuration.
     */
    public static class Builder {
        private final CommandContext context;
        private final List<Object> commands = new ArrayList<>();
        private final List<Class<?>> commandClasses = new ArrayList<>();
        private final List<String> packages = new ArrayList<>();

        private Builder(CommandContext context) {
            this.context = context;
        }

        public Builder register(Object command) {
            commands.add(command);
            return this;
        }

        public Builder register(Class<?> commandClass) {
            commandClasses.add(commandClass);
            return this;
        }

        public Builder scanPackages(String... packageNames) {
            packages.addAll(Arrays.asList(packageNames));
            return this;
        }

        public PicocliCommandRegistry build() {
            PicocliCommandRegistry registry = new PicocliCommandRegistry(context);

            // Register individual commands
            for (Object command : commands) {
                registry.register(command);
            }

            // Register command classes
            for (Class<?> commandClass : commandClasses) {
                registry.register(commandClass);
            }

            // Scan packages
            if (!packages.isEmpty()) {
                registry.scanPackages(packages.toArray(new String[0]));
            }

            return registry;
        }
    }

    /**
     * Root command for organizing subcommands and completion.
     */
    @Command(name = "root", hidden = true)
    private static class RootCommand {
        // Empty root command for organizing subcommands
    }
}
