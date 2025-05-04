---
sidebar_position: 11
---

# JLine Integration with Other Libraries

JLine works well with other Java libraries to create powerful command-line applications. This guide covers how to integrate JLine with popular command-line frameworks and libraries to enhance your terminal applications.

## Integration with Picocli

[Picocli](https://picocli.info/) is a modern framework for building command-line applications. It offers annotation-based command parsing, type conversion, validation, and more. Integrating JLine with Picocli provides rich interactive features for your command-line applications.

### Setting Up JLine with Picocli

To integrate JLine with Picocli, you'll need the following dependencies:

```xml
<dependency>
    <groupId>info.picocli</groupId>
    <artifactId>picocli</artifactId>
    <version>4.7.5</version>
</dependency>
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline</artifactId>
    <version>3.29.0</version>
</dependency>
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-builtins</artifactId>
    <version>3.29.0</version>
</dependency>
```

### Basic Integration Example

Here's a basic example of integrating JLine with Picocli:

```java title="PicocliJLineExample.java" showLineNumbers
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.util.concurrent.Callable;

public class PicocliJLineExample {
    public static void main(String[] args) {
        try {
            // Set up the terminal
            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();

            // Set up the line reader
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            // Create the command line parser
            MyCommand myCommand = new MyCommand(terminal);
            CommandLine cmd = new CommandLine(myCommand);

            // Main interactive loop
            while (true) {
                String line = reader.readLine("example> ");

                // Exit if requested
                if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                    break;
                }

                try {
                    // Parse and execute the command
                    ParsedLine pl = reader.getParser().parse(line, 0);
                    String[] arguments = pl.words().toArray(new String[0]);
                    cmd.execute(arguments);
                } catch (Exception e) {
                    terminal.writer().println("Error: " + e.getMessage());
                    terminal.flush();
                }
            }

            terminal.writer().println("Goodbye!");
            terminal.close();

        } catch (IOException e) {
            System.err.println("Error creating terminal: " + e.getMessage());
        }
    }

    // Define a command using Picocli annotations
    @Command(name = "example", mixinStandardHelpOptions = true, version = "1.0",
             description = "Example command using JLine and Picocli")
    static class MyCommand implements Callable<Integer> {
        private final Terminal terminal;

        @Option(names = {"-c", "--count"}, description = "Number of times to repeat")
        private int count = 1;

        @Parameters(index = "0", description = "The message to display")
        private String message;

        public MyCommand(Terminal terminal) {
            this.terminal = terminal;
        }

        @Override
        public Integer call() {
            if (message == null) {
                terminal.writer().println("No message provided. Use --help for usage information.");
            } else {
                for (int i = 0; i < count; i++) {
                    terminal.writer().println(message);
                }
            }
            terminal.flush();
            return 0;
        }
    }
}
```

### Advanced Integration with Tab Completion

You can enhance the integration with tab completion for Picocli commands:

```java title="PicocliCompletionExample.java" showLineNumbers
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.shell.jline3.PicocliJLineCompleter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

public class PicocliCompletionExample {
    public static void main(String[] args) {
        try {
            // Set up the terminal
            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();

            // Create the command line parser
            RootCommand rootCommand = new RootCommand(terminal);
            CommandLine commandLine = new CommandLine(rootCommand);

            // Add subcommands
            commandLine.addSubcommand("hello", new HelloCommand(terminal));
            commandLine.addSubcommand("echo", new EchoCommand(terminal));

            // Create a completer for the command line
            Completer completer = new PicocliJLineCompleter(commandLine.getCommandSpec());

            // Set up the line reader with completion
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(completer)
                    .build();

            // Main interactive loop
            while (true) {
                String line = reader.readLine("cli> ");

                // Exit if requested
                if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                    break;
                }

                try {
                    // Parse and execute the command
                    ParsedLine pl = reader.getParser().parse(line, 0);
                    String[] arguments = pl.words().toArray(new String[0]);
                    commandLine.execute(arguments);
                } catch (Exception e) {
                    terminal.writer().println("Error: " + e.getMessage());
                    terminal.flush();
                }
            }

            terminal.writer().println("Goodbye!");
            terminal.close();

        } catch (IOException e) {
            System.err.println("Error creating terminal: " + e.getMessage());
        }
    }

    // Root command
    @Command(name = "cli", mixinStandardHelpOptions = true, version = "1.0",
             description = "Interactive CLI with JLine and Picocli")
    static class RootCommand implements Callable<Integer> {
        private final Terminal terminal;

        public RootCommand(Terminal terminal) {
            this.terminal = terminal;
        }

        @Override
        public Integer call() {
            terminal.writer().println("Use one of the available commands:");
            terminal.writer().println("  hello - Say hello");
            terminal.writer().println("  echo - Echo a message");
            terminal.writer().println("  help - Show help");
            terminal.writer().println("  exit - Exit the application");
            terminal.flush();
            return 0;
        }
    }

    // Hello command
    @Command(name = "hello", description = "Say hello")
    static class HelloCommand implements Callable<Integer> {
        private final Terminal terminal;

        @Option(names = {"-n", "--name"}, description = "Name to greet")
        private String name = "World";

        public HelloCommand(Terminal terminal) {
            this.terminal = terminal;
        }

        @Override
        public Integer call() {
            terminal.writer().println("Hello, " + name + "!");
            terminal.flush();
            return 0;
        }
    }

    // Echo command
    @Command(name = "echo", description = "Echo a message")
    static class EchoCommand implements Callable<Integer> {
        private final Terminal terminal;

        @Option(names = {"-u", "--uppercase"}, description = "Convert to uppercase")
        private boolean uppercase;

        @Parameters(description = "Message to echo")
        private List<String> message;

        public EchoCommand(Terminal terminal) {
            this.terminal = terminal;
        }

        @Override
        public Integer call() {
            if (message == null || message.isEmpty()) {
                terminal.writer().println("No message provided");
            } else {
                String result = String.join(" ", message);
                if (uppercase) {
                    result = result.toUpperCase();
                }
                terminal.writer().println(result);
            }
            terminal.flush();
            return 0;
        }
    }
}
```

## Integration with Spring Shell

[Spring Shell](https://spring.io/projects/spring-shell) is a framework for building command-line applications using the Spring Framework. It provides a rich set of features for building interactive shells.

### Setting Up JLine with Spring Shell

Spring Shell already uses JLine internally, but you can customize the JLine configuration:

```xml
<dependency>
    <groupId>org.springframework.shell</groupId>
    <artifactId>spring-shell-starter</artifactId>
    <version>3.1.3</version>
</dependency>
```

### Customizing JLine in Spring Shell

You can customize the JLine configuration in Spring Shell by providing custom beans:

```java title="SpringShellJLineExample.java" showLineNumbers
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.command.CommandRegistration.Builder;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;

@SpringBootApplication
public class SpringShellJLineExample {
    public static void main(String[] args) {
        SpringApplication.run(SpringShellJLineExample.class, args);
    }

    // Customize the terminal
    @Bean
    public Terminal terminal() throws IOException {
        return TerminalBuilder.builder()
                .system(true)
                .build();
    }

    // Customize the line reader
    @Bean
    public LineReader lineReader(Terminal terminal) {
        return LineReaderBuilder.builder()
                .terminal(terminal)
                .option(LineReader.Option.AUTO_FRESH_LINE, true)
                .option(LineReader.Option.HISTORY_BEEP, false)
                .build();
    }

    // Customize the prompt
    @Bean
    public org.springframework.shell.jline.PromptProvider promptProvider() {
        return () -> new AttributedString("custom-shell:> ",
                AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
    }

    // Register a command programmatically
    @Bean
    public CommandRegistration echoCommand() {
        Builder builder = CommandRegistration.builder();
        return builder
                .command("echo")
                .description("Echo a message")
                .withOption()
                    .longNames("message")
                    .shortNames('m')
                    .description("The message to echo")
                    .required()
                    .and()
                .withOption()
                    .longNames("uppercase")
                    .shortNames('u')
                    .description("Convert to uppercase")
                    .defaultValue("false")
                    .and()
                .withTarget()
                    .function(ctx -> {
                        String message = ctx.getOptionValue("message");
                        boolean uppercase = Boolean.parseBoolean(ctx.getOptionValue("uppercase"));

                        if (uppercase) {
                            message = message.toUpperCase();
                        }

                        return message;
                    })
                    .and()
                .build();
    }

    // Define a command using annotations
    @ShellComponent
    public static class MyCommands {
        @ShellMethod(key = "hello", value = "Say hello")
        public String hello(@ShellOption(defaultValue = "World") String name) {
            return "Hello, " + name + "!";
        }

        @ShellMethod(key = "sum", value = "Sum two numbers")
        public int sum(int a, int b) {
            return a + b;
        }
    }
}
```

## Integration with Apache Commons CLI

[Apache Commons CLI](https://commons.apache.org/proper/commons-cli/) is a simple library for parsing command-line options. While it doesn't provide interactive features like Picocli or Spring Shell, you can combine it with JLine for a basic interactive shell.

### Setting Up JLine with Commons CLI

```xml
<dependency>
    <groupId>commons-cli</groupId>
    <artifactId>commons-cli</artifactId>
    <version>1.5.0</version>
</dependency>
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline</artifactId>
    <version>3.29.0</version>
</dependency>
```

### Basic Integration Example

```java title="CommonsCliJLineExample.java" showLineNumbers
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.Arrays;

public class CommonsCliJLineExample {
    public static void main(String[] args) {
        try {
            // Set up the terminal
            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();

            // Set up the line reader
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            // Define CLI options
            Options options = new Options();
            options.addOption(Option.builder("h")
                    .longOpt("help")
                    .desc("Show help")
                    .build());
            options.addOption(Option.builder("g")
                    .longOpt("greet")
                    .desc("Greet someone")
                    .hasArg()
                    .argName("name")
                    .build());
            options.addOption(Option.builder("c")
                    .longOpt("count")
                    .desc("Count to a number")
                    .hasArg()
                    .argName("number")
                    .type(Number.class)
                    .build());

            // Create the parser
            CommandLineParser parser = new DefaultParser();
            HelpFormatter formatter = new HelpFormatter();

            // Main interactive loop
            while (true) {
                String line = reader.readLine("cli> ");

                // Exit if requested
                if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                    break;
                }

                // Parse the command line
                try {
                    String[] arguments = line.split("\\s+");
                    CommandLine cmd = parser.parse(options, arguments);

                    // Process the command
                    if (cmd.hasOption("help")) {
                        formatter.printHelp(terminal.writer(), 80, "cli",
                                "CLI Example", options, 2, 2,
                                "Use 'exit' to quit", true);
                    } else if (cmd.hasOption("greet")) {
                        String name = cmd.getOptionValue("greet", "World");
                        terminal.writer().println("Hello, " + name + "!");
                    } else if (cmd.hasOption("count")) {
                        try {
                            int count = Integer.parseInt(cmd.getOptionValue("count", "10"));
                            for (int i = 1; i <= count; i++) {
                                terminal.writer().println(i);
                            }
                        } catch (NumberFormatException e) {
                            terminal.writer().println("Error: Invalid number format");
                        }
                    } else {
                        // Handle command arguments
                        String[] cmdArgs = cmd.getArgs();
                        if (cmdArgs.length > 0) {
                            terminal.writer().println("Arguments: " + Arrays.toString(cmdArgs));
                        } else {
                            terminal.writer().println("No command specified. Use --help for usage information.");
                        }
                    }
                } catch (ParseException e) {
                    terminal.writer().println("Error: " + e.getMessage());
                    formatter.printHelp(terminal.writer(), 80, "cli",
                            "CLI Example", options, 2, 2,
                            "Use 'exit' to quit", true);
                }

                terminal.flush();
            }

            terminal.writer().println("Goodbye!");
            terminal.close();

        } catch (IOException e) {
            System.err.println("Error creating terminal: " + e.getMessage());
        }
    }
}
```

## Integration with JCommander

[JCommander](https://jcommander.org/) is a command-line parsing framework that uses annotations to define parameters. Here's how to integrate it with JLine:

### Setting Up JLine with JCommander

```xml
<dependency>
    <groupId>com.beust</groupId>
    <artifactId>jcommander</artifactId>
    <version>1.82</version>
</dependency>
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline</artifactId>
    <version>3.29.0</version>
</dependency>
```

### Basic Integration Example

```java title="JCommanderJLineExample.java" showLineNumbers
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JCommanderJLineExample {
    public static void main(String[] args) {
        try {
            // Set up the terminal
            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();

            // Set up the line reader
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            // Create command objects
            MainCommand mainCommand = new MainCommand();
            GreetCommand greetCommand = new GreetCommand();
            CountCommand countCommand = new CountCommand();

            // Set up JCommander
            JCommander jc = JCommander.newBuilder()
                    .addObject(mainCommand)
                    .addCommand("greet", greetCommand)
                    .addCommand("count", countCommand)
                    .build();

            // Main interactive loop
            while (true) {
                String line = reader.readLine("jcmd> ");

                // Exit if requested
                if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                    break;
                }

                try {
                    // Parse the command line
                    String[] arguments = line.split("\\s+");

                    // Reset state
                    mainCommand.help = false;
                    jc.parse(arguments);

                    // Process the command
                    if (mainCommand.help) {
                        jc.usage();
                    } else {
                        String parsedCommand = jc.getParsedCommand();
                        if (parsedCommand == null) {
                            terminal.writer().println("No command specified. Use --help for usage information.");
                        } else if (parsedCommand.equals("greet")) {
                            terminal.writer().println("Hello, " + greetCommand.name + "!");
                        } else if (parsedCommand.equals("count")) {
                            for (int i = 1; i <= countCommand.number; i++) {
                                terminal.writer().println(i);
                            }
                        }
                    }
                } catch (Exception e) {
                    terminal.writer().println("Error: " + e.getMessage());
                    jc.usage();
                }

                terminal.flush();
            }

            terminal.writer().println("Goodbye!");
            terminal.close();

        } catch (IOException e) {
            System.err.println("Error creating terminal: " + e.getMessage());
        }
    }

    // Main command parameters
    static class MainCommand {
        @Parameter(names = {"--help", "-h"}, help = true, description = "Show help")
        boolean help;
    }

    // Greet command
    @Parameters(commandDescription = "Greet someone")
    static class GreetCommand {
        @Parameter(names = {"--name", "-n"}, description = "Name to greet")
        String name = "World";
    }

    // Count command
    @Parameters(commandDescription = "Count to a number")
    static class CountCommand {
        @Parameter(names = {"--number", "-n"}, description = "Number to count to")
        int number = 10;

        @Parameter(description = "Additional arguments")
        List<String> args = new ArrayList<>();
    }
}
```

## Integration with JLine Builtins

JLine provides its own set of built-in commands and utilities in the `jline-builtins` module. These can be used to create a rich interactive shell:

```java title="JLineBuiltinsExample.java" showLineNumbers
import org.jline.builtins.Builtins;
import org.jline.builtins.Completers.SystemCompleter;
import org.jline.builtins.Options.HelpException;
import org.jline.builtins.Widgets;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class JLineBuiltinsExample {
    public static void main(String[] args) {
        try {
            // Set up the terminal
            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();

            // Set up the parser
            Parser parser = new DefaultParser();

            // Set up the builtins
            Path currentDir = Paths.get("").toAbsolutePath();
            Builtins builtins = new Builtins(currentDir, null, null);
            SystemCompleter systemCompleter = builtins.compileCompleters();

            // Set up custom commands
            Map<String, Supplier<String>> customCommands = new HashMap<>();
            customCommands.put("hello", () -> "Hello, World!");
            customCommands.put("date", () -> new java.util.Date().toString());

            // Add completers for custom commands
            systemCompleter.add("hello", systemCompleter.compileCompleter(List.of()));
            systemCompleter.add("date", systemCompleter.compileCompleter(List.of()));

            // Set up the line reader
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(systemCompleter)
                    .parser(parser)
                    .build();

            // Set up widgets
            Widgets widgets = new Widgets(reader);
            widgets.addWidget("custom-widget", () -> {
                reader.getBuffer().write("Hello from custom widget!");
                return true;
            });

            // Bind widget to key
            reader.getKeyMaps().get(LineReader.MAIN).bind(
                    widgets.getWidget("custom-widget"),
                    "\033w");  // Alt+W

            // Create a custom prompt
            Supplier<String> prompt = () -> {
                String p = new AttributedStringBuilder()
                        .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
                        .append("shell:")
                        .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                        .append(currentDir.getFileName().toString())
                        .style(AttributedStyle.DEFAULT)
                        .append("> ")
                        .toAnsi();
                return p;
            };

            // Main interactive loop
            PrintWriter writer = terminal.writer();

            // Display welcome message
            writer.println("JLine Builtins Example");
            writer.println("---------------------");
            writer.println("Type 'help' to see available commands");
            writer.println("Press Alt+W to trigger the custom widget");
            writer.println("Type 'exit' to quit");
            writer.println();
            writer.flush();

            while (true) {
                String line = reader.readLine(prompt.get(), null, (MaskingCallback) null, null);

                // Exit if requested
                if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                    break;
                }

                // Parse the line
                ParsedLine pl = parser.parse(line, 0);
                String command = pl.words().get(0);

                try {
                    // Check for builtins
                    if (builtins.hasCommand(command)) {
                        builtins.execute(command, pl.words().subList(1, pl.words().size()).toArray(new String[0]),
                                System.in, writer, writer, terminal);
                    }
                    // Check for custom commands
                    else if (customCommands.containsKey(command)) {
                        writer.println(customCommands.get(command).get());
                    }
                    // Unknown command
                    else {
                        writer.println("Unknown command: " + command);
                        writer.println("Type 'help' to see available commands");
                    }
                } catch (HelpException e) {
                    writer.println(e.getMessage());
                } catch (Exception e) {
                    writer.println("Error: " + e.getMessage());
                }

                writer.flush();
            }

            writer.println("Goodbye!");
            terminal.close();

        } catch (IOException e) {
            System.err.println("Error creating terminal: " + e.getMessage());
        }
    }
}
```

## Best Practices for Library Integration

When integrating JLine with other libraries, follow these best practices:

1. **Separate concerns**: Keep command parsing logic separate from terminal handling.

2. **Handle exceptions gracefully**: Catch and handle exceptions from both JLine and the integrated library.

3. **Provide helpful error messages**: When command parsing fails, provide clear error messages and usage information.

4. **Add tab completion**: Enhance the user experience with tab completion for commands and arguments.

5. **Customize the prompt**: Use a custom prompt to provide context and visual cues.

6. **Support history**: Configure JLine's history feature to provide command history.

7. **Add custom widgets**: Create custom widgets for frequently used actions.

8. **Test thoroughly**: Test your integration with various commands and edge cases.

9. **Handle terminal resizing**: Ensure your application responds appropriately to terminal resize events.

10. **Provide a clean exit**: Clean up resources and restore the terminal state when exiting.

## Conclusion

Integrating JLine with other command-line libraries allows you to create powerful, interactive terminal applications with rich features. Whether you're using Picocli, Spring Shell, Commons CLI, JCommander, or JLine's built-in utilities, JLine enhances the user experience with line editing, history, tab completion, and more.

By following the examples and best practices in this guide, you can create sophisticated command-line applications that combine the strengths of JLine with your preferred command parsing library.
