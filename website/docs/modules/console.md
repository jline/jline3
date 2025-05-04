---
sidebar_position: 4
---

# JLine Console

The `jline-console` module provides a framework for building interactive console applications. It includes infrastructure for command processing, argument parsing, and help generation, making it easier to create sophisticated command-line interfaces.

## Maven Dependency

To use the console module, add the following dependency to your project:

```xml
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-console</artifactId>
    <version>3.29.0</version>
</dependency>
```

## Command Framework

The console module provides a command framework that makes it easy to define and execute commands:

```java title="CommandFrameworkExample.java" showLineNumbers
import org.jline.console.CommandInput;
import org.jline.console.CommandRegistry;
import org.jline.console.impl.AbstractCommandRegistry;
import org.jline.console.impl.DefaultCommandRegistry;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class CommandFrameworkExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        DefaultParser parser = new DefaultParser();

        // highlight-start
        // Create a command registry
        CommandRegistry registry = new DefaultCommandRegistry();

        // Register commands
        registry.registerCommand("echo", args -> {
            terminal.writer().println(String.join(" ", args));
            return 0;
        });

        registry.registerCommand("add", args -> {
            try {
                int sum = Arrays.stream(args)
                        .mapToInt(Integer::parseInt)
                        .sum();
                terminal.writer().println("Sum: " + sum);
                return 0;
            } catch (NumberFormatException e) {
                terminal.writer().println("Error: Invalid number format");
                return 1;
            }
        });
        // highlight-end

        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(parser)
                .completer(registry.completer())
                .build();

        // Main command loop
        PrintWriter writer = terminal.writer();
        while (true) {
            String line = reader.readLine("console> ");
            if (line.trim().equalsIgnoreCase("exit")) {
                break;
            }

            try {
                // Parse and execute the command
                CommandInput input = new CommandInput(line, parser.parse(line, 0).words());
                registry.execute(input);
            } catch (Exception e) {
                writer.println("Error: " + e.getMessage());
            }
            writer.flush();
        }
    }
}
```

## Creating Custom Commands

You can create custom commands by implementing the `Command` interface or extending `AbstractCommand`:

```java title="CustomCommandExample.java" showLineNumbers
import org.jline.console.CommandInput;
import org.jline.console.CommandRegistry;
import org.jline.console.impl.AbstractCommand;
import org.jline.console.impl.DefaultCommandRegistry;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class CustomCommandExample {
    // highlight-start
    // Custom command implementation
    static class GreetCommand extends AbstractCommand {
        public GreetCommand() {
            super("greet", "Greet a person", "greet [name]");
        }

        @Override
        public Object execute(CommandInput input) {
            List<String> args = input.args();
            String name = args.isEmpty() ? "World" : args.get(0);
            return "Hello, " + name + "!";
        }
    }

    static class CalculateCommand extends AbstractCommand {
        public CalculateCommand() {
            super("calc", "Perform calculations", "calc <operation> <num1> <num2>");
        }

        @Override
        public Object execute(CommandInput input) {
            List<String> args = input.args();
            if (args.size() < 3) {
                throw new IllegalArgumentException("Not enough arguments");
            }

            String operation = args.get(0);
            double num1 = Double.parseDouble(args.get(1));
            double num2 = Double.parseDouble(args.get(2));

            switch (operation) {
                case "add": return num1 + num2;
                case "subtract": return num1 - num2;
                case "multiply": return num1 * num2;
                case "divide":
                    if (num2 == 0) {
                        throw new IllegalArgumentException("Cannot divide by zero");
                    }
                    return num1 / num2;
                default:
                    throw new IllegalArgumentException("Unknown operation: " + operation);
            }
        }
    }
    // highlight-end

    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        DefaultParser parser = new DefaultParser();

        // Create a command registry
        CommandRegistry registry = new DefaultCommandRegistry();

        // Register custom commands
        registry.registerCommand(new GreetCommand());
        registry.registerCommand(new CalculateCommand());

        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(parser)
                .completer(registry.completer())
                .build();

        // Main command loop
        PrintWriter writer = terminal.writer();
        while (true) {
            String line = reader.readLine("custom> ");
            if (line.trim().equalsIgnoreCase("exit")) {
                break;
            }

            try {
                // Parse and execute the command
                CommandInput input = new CommandInput(line, parser.parse(line, 0).words());
                Object result = registry.execute(input);
                if (result != null) {
                    writer.println(result);
                }
            } catch (Exception e) {
                writer.println("Error: " + e.getMessage());
            }
            writer.flush();
        }
    }
}
```

## Command Completion

The console module provides support for command completion:

```java title="CommandCompletionExample.java"
import org.jline.console.CommandRegistry;
import org.jline.console.impl.DefaultCommandRegistry;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.Arrays;

public class CommandCompletionExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        DefaultParser parser = new DefaultParser();

        // Create a command registry
        CommandRegistry registry = new DefaultCommandRegistry();

        // Register commands
        registry.registerCommand("help", args -> {
            terminal.writer().println("Available commands: help, echo, exit");
            return 0;
        });

        registry.registerCommand("echo", args -> {
            terminal.writer().println(String.join(" ", args));
            return 0;
        });

        // highlight-start
        // Create completers for commands
        Completer helpCompleter = new ArgumentCompleter(
                new StringsCompleter("help"),
                NullCompleter.INSTANCE
        );

        Completer echoCompleter = new ArgumentCompleter(
                new StringsCompleter("echo"),
                new StringsCompleter("hello", "world", "test")
        );

        // Register completers
        registry.registerCompleter("help", helpCompleter);
        registry.registerCompleter("echo", echoCompleter);
        // highlight-end

        // Create a line reader with the registry's completer
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(parser)
                .completer(registry.completer())
                .build();

        // Main command loop
        while (true) {
            String line = reader.readLine("completion> ");
            if (line.trim().equalsIgnoreCase("exit")) {
                break;
            }

            try {
                registry.execute(line);
            } catch (Exception e) {
                terminal.writer().println("Error: " + e.getMessage());
            }
            terminal.writer().flush();
        }
    }
}
```

## Command Groups

You can organize commands into groups:

```java title="CommandGroupsExample.java" showLineNumbers
import org.jline.console.CommandInput;
import org.jline.console.CommandRegistry;
import org.jline.console.impl.AbstractCommand;
import org.jline.console.impl.DefaultCommandRegistry;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class CommandGroupsExample {
    // File commands
    // highlight-start
    static class ListCommand extends AbstractCommand {
        public ListCommand() {
            super("file:list", "List files", "file:list [directory]");
        }

        @Override
        public Object execute(CommandInput input) {
            List<String> args = input.args();
            String dir = args.isEmpty() ? "." : args.get(0);
            return "Listing files in " + dir;
        }
    }

    static class CatCommand extends AbstractCommand {
        public CatCommand() {
            super("file:cat", "Display file contents", "file:cat <file>");
        }

        @Override
        public Object execute(CommandInput input) {
            List<String> args = input.args();
            if (args.isEmpty()) {
                throw new IllegalArgumentException("File name required");
            }
            return "Contents of " + args.get(0);
        }
    }

    // Network commands
    static class PingCommand extends AbstractCommand {
        public PingCommand() {
            super("net:ping", "Ping a host", "net:ping <host>");
        }

        @Override
        public Object execute(CommandInput input) {
            List<String> args = input.args();
            if (args.isEmpty()) {
                throw new IllegalArgumentException("Host required");
            }
            return "Pinging " + args.get(0);
        }
    }

    static class HttpCommand extends AbstractCommand {
        public HttpCommand() {
            super("net:http", "Make HTTP request", "net:http <url>");
        }

        @Override
        public Object execute(CommandInput input) {
            List<String> args = input.args();
            if (args.isEmpty()) {
                throw new IllegalArgumentException("URL required");
            }
            return "Fetching " + args.get(0);
        }
    }
    // highlight-end

    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        DefaultParser parser = new DefaultParser();

        // Create a command registry
        CommandRegistry registry = new DefaultCommandRegistry();

        // Register commands by group
        registry.registerCommand(new ListCommand());
        registry.registerCommand(new CatCommand());
        registry.registerCommand(new PingCommand());
        registry.registerCommand(new HttpCommand());

        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(parser)
                .completer(registry.completer())
                .build();

        // Main command loop
        PrintWriter writer = terminal.writer();
        while (true) {
            String line = reader.readLine("groups> ");
            if (line.trim().equalsIgnoreCase("exit")) {
                break;
            }

            try {
                // Parse and execute the command
                CommandInput input = new CommandInput(line, parser.parse(line, 0).words());
                Object result = registry.execute(input);
                if (result != null) {
                    writer.println(result);
                }
            } catch (Exception e) {
                writer.println("Error: " + e.getMessage());
            }
            writer.flush();
        }
    }
}
```

## Script Execution

The console module supports executing scripts:

```java title="ScriptExecutionExample.java"
import org.jline.console.CommandRegistry;
import org.jline.console.ScriptEngine;
import org.jline.console.impl.DefaultCommandRegistry;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ScriptExecutionExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        DefaultParser parser = new DefaultParser();
        PrintWriter writer = terminal.writer();

        // Create a command registry
        CommandRegistry registry = new DefaultCommandRegistry();

        // Register basic commands
        registry.registerCommand("echo", args -> {
            writer.println(String.join(" ", args));
            return 0;
        });

        registry.registerCommand("add", args -> {
            int sum = 0;
            for (String arg : args) {
                sum += Integer.parseInt(arg);
            }
            writer.println("Sum: " + sum);
            return sum;
        });

        // highlight-start
        // Create a script engine
        ScriptEngine engine = new ScriptEngine() {
            @Override
            public Object execute(CommandRegistry commandRegistry, String script) {
                String[] lines = script.split("\n");
                Object result = null;

                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue; // Skip empty lines and comments
                    }

                    try {
                        result = commandRegistry.execute(line);
                    } catch (Exception e) {
                        writer.println("Error executing script line: " + line);
                        writer.println("  " + e.getMessage());
                        return 1; // Error code
                    }
                }

                return result;
            }

            @Override
            public boolean hasVariable(String name) {
                return false;
            }

            @Override
            public Object getVariable(String name) {
                return null;
            }

            @Override
            public void putVariable(String name, Object value) {
                // Not implemented
            }
        };
        // highlight-end

        // Register script execution command
        registry.registerCommand("source", args -> {
            if (args.isEmpty()) {
                writer.println("Usage: source <script-file>");
                return 1;
            }

            Path scriptPath = Paths.get(args.get(0));
            if (!Files.exists(scriptPath)) {
                writer.println("Script file not found: " + scriptPath);
                return 1;
            }

            try {
                List<String> scriptLines = Files.readAllLines(scriptPath);
                String script = String.join("\n", scriptLines);
                return engine.execute(registry, script);
            } catch (IOException e) {
                writer.println("Error reading script file: " + e.getMessage());
                return 1;
            }
        });

        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(parser)
                .completer(registry.completer())
                .build();

        // Main command loop
        while (true) {
            String line = reader.readLine("script> ");
            if (line.trim().equalsIgnoreCase("exit")) {
                break;
            }

            try {
                registry.execute(line);
            } catch (Exception e) {
                writer.println("Error: " + e.getMessage());
            }
            writer.flush();
        }
    }
}
```

## Variable Support

The console module provides support for variables:

```java title="VariableSupportExample.java" showLineNumbers
import org.jline.console.CommandRegistry;
import org.jline.console.impl.DefaultCommandRegistry;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VariableSupportExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        DefaultParser parser = new DefaultParser();
        PrintWriter writer = terminal.writer();

        // highlight-start
        // Create a variable store
        Map<String, Object> variables = new HashMap<>();

        // Pattern to match variable references
        Pattern varPattern = Pattern.compile("\\$([a-zA-Z0-9_]+)");
        // highlight-end

        // Create a command registry
        CommandRegistry registry = new DefaultCommandRegistry();

        // Register variable commands
        registry.registerCommand("set", args -> {
            if (args.size() < 2) {
                writer.println("Usage: set <name> <value>");
                return 1;
            }

            String name = args.get(0);
            String value = args.get(1);
            variables.put(name, value);
            writer.println(name + " = " + value);
            return 0;
        });

        registry.registerCommand("get", args -> {
            if (args.isEmpty()) {
                // List all variables
                variables.forEach((name, value) -> writer.println(name + " = " + value));
            } else {
                // Get specific variable
                String name = args.get(0);
                if (variables.containsKey(name)) {
                    writer.println(name + " = " + variables.get(name));
                } else {
                    writer.println("Variable not found: " + name);
                    return 1;
                }
            }
            return 0;
        });

        registry.registerCommand("echo", args -> {
            StringBuilder result = new StringBuilder();

            for (String arg : args) {
                // Replace variable references
                Matcher matcher = varPattern.matcher(arg);
                StringBuffer sb = new StringBuffer();

                while (matcher.find()) {
                    String varName = matcher.group(1);
                    String varValue = variables.containsKey(varName)
                            ? variables.get(varName).toString()
                            : "$" + varName;
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(varValue));
                }
                matcher.appendTail(sb);

                result.append(sb).append(" ");
            }

            writer.println(result.toString().trim());
            return 0;
        });

        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(parser)
                .completer(registry.completer())
                .build();

        // Main command loop
        while (true) {
            String line = reader.readLine("vars> ");
            if (line.trim().equalsIgnoreCase("exit")) {
                break;
            }

            try {
                registry.execute(line);
            } catch (Exception e) {
                writer.println("Error: " + e.getMessage());
            }
            writer.flush();
        }
    }
}
```

## Best Practices

When using the JLine console module, consider these best practices:

1. **Organize Commands Logically**: Group related commands together to make them easier to discover and use.

2. **Provide Helpful Command Descriptions**: Include clear descriptions and usage information for each command.

3. **Implement Tab Completion**: Add completers for commands to improve usability.

4. **Handle Errors Gracefully**: Catch and handle exceptions appropriately, providing helpful error messages.

5. **Use Command Groups**: Organize commands into groups for better organization in larger applications.

6. **Support Script Execution**: Allow users to automate tasks by executing scripts.

7. **Implement Variable Support**: Variables make scripts more flexible and powerful.

8. **Provide Help Commands**: Include commands that display help information for other commands.

9. **Follow Consistent Command Syntax**: Use a consistent syntax for all commands to make them easier to learn and use.

10. **Support Command History**: Leverage JLine's history capabilities to allow users to recall and edit previous commands.
