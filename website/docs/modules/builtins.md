---
sidebar_position: 2
---

# JLine Builtins

The `jline-builtins` module provides a set of ready-to-use commands and utilities that you can incorporate into your command-line applications. These built-in components save you time and effort when implementing common command-line functionality.

## Maven Dependency

To use the builtins module, add the following dependency to your project:

```xml
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-builtins</artifactId>
    <version>3.29.0</version>
</dependency>
```

## Built-in Commands

The builtins module includes several ready-to-use commands that you can incorporate into your application:

### File Operations

```java title="FileOperationsExample.java" showLineNumbers
import org.jline.builtins.Commands;
import org.jline.builtins.Completers.FilesCompleter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileOperationsExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        PrintWriter writer = terminal.writer();

        // Create a line reader with file operations completers
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(new ArgumentCompleter(
                        new StringsCompleter("ls", "cat", "less"),
                        new FilesCompleter(Paths.get("."))))
                .build();

        // Main command loop
        while (true) {
            String line = reader.readLine("builtin> ");
            String[] arguments = line.split("\\s+");

            try {
                if (arguments.length > 0) {
                    switch (arguments[0]) {
                        // highlight-start
                        case "ls":
                            // List files in a directory
                            Path path = arguments.length > 1 ? Paths.get(arguments[1]) : Paths.get(".");
                            Commands.ls(terminal, writer, path, false, false, false, false);
                            break;
                        // highlight-end

                        case "cat":
                            // Display file contents
                            if (arguments.length > 1) {
                                Commands.cat(terminal, writer, Paths.get(arguments[1]));
                            } else {
                                writer.println("Usage: cat <file>");
                            }
                            break;

                        case "less":
                            // Display file contents with paging
                            if (arguments.length > 1) {
                                Commands.less(terminal, Paths.get(arguments[1]));
                            } else {
                                writer.println("Usage: less <file>");
                            }
                            break;

                        case "exit":
                            return;

                        default:
                            writer.println("Unknown command: " + arguments[0]);
                    }
                }
            } catch (Exception e) {
                writer.println("Error: " + e.getMessage());
            }
            writer.flush();
        }
    }
}
```

### Table Formatting

The builtins module includes utilities for formatting tabular data:

```java title="TableFormattingExample.java"
import org.jline.builtins.Styles;
import org.jline.builtins.Styles.AttributedStringBuilder;
import org.jline.builtins.Styles.AttributedStyle;
import org.jline.builtins.Tables;
import org.jline.builtins.Tables.Column;
import org.jline.builtins.Tables.ColumnType;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TableFormattingExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        PrintWriter writer = terminal.writer();

        // highlight-start
        // Define table columns
        List<Column> columns = Arrays.asList(
                new Column("ID", ColumnType.Number),
                new Column("Name", ColumnType.String),
                new Column("Role", ColumnType.String),
                new Column("Salary", ColumnType.Number)
        );
        // highlight-end

        // Create table data
        List<List<String>> data = new ArrayList<>();
        data.add(Arrays.asList("1", "John Doe", "Developer", "75000"));
        data.add(Arrays.asList("2", "Jane Smith", "Manager", "85000"));
        data.add(Arrays.asList("3", "Bob Johnson", "Designer", "65000"));

        // Print the table
        Tables.TableBuilder tableBuilder = new Tables.TableBuilder(columns);
        tableBuilder.addAll(data);

        // Format and display the table
        Tables.Table table = tableBuilder.build();
        String result = table.toStringWithColumns(
                terminal.getWidth(),
                true, // display borders
                true  // display header
        );

        writer.println(result);
        writer.flush();
    }
}
```

## Widgets

The builtins module provides several widgets that can be used with the LineReader:

```java title="WidgetsExample.java"
import org.jline.builtins.Widgets;
import org.jline.builtins.Widgets.TailTipWidgets;
import org.jline.builtins.Widgets.TailTipWidgets.TipType;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WidgetsExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        DefaultParser parser = new DefaultParser();

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(parser)
                .build();

        // highlight-start
        // Create command descriptions for TailTip widget
        Map<String, List<String>> commandDescriptions = new HashMap<>();
        commandDescriptions.put("help", Arrays.asList("Display help information"));
        commandDescriptions.put("exit", Arrays.asList("Exit the application"));
        commandDescriptions.put("ls", Arrays.asList("[path]", "List directory contents"));
        commandDescriptions.put("cat", Arrays.asList("<file>", "Display file contents"));

        // Create and install the TailTip widget
        TailTipWidgets widgets = new TailTipWidgets(reader, commandDescriptions, 5, TipType.COMPLETER);
        widgets.enable();
        // highlight-end

        // Main command loop
        while (true) {
            String line = reader.readLine("widgets> ");
            if ("exit".equals(line)) {
                break;
            }
            terminal.writer().println("You entered: " + line);
            terminal.writer().flush();
        }
    }
}
```

## SystemRegistryImpl

The builtins module includes a `SystemRegistryImpl` class that provides a registry for commands:

```java title="SystemRegistryExample.java" showLineNumbers
import org.jline.builtins.Builtins;
import org.jline.builtins.Completers.SystemCompleter;
import org.jline.builtins.Options.HelpException;
import org.jline.builtins.SystemRegistry;
import org.jline.builtins.SystemRegistryImpl;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.function.Supplier;

public class SystemRegistryExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        DefaultParser parser = new DefaultParser();

        // highlight-start
        // Create the registry
        SystemRegistry registry = new SystemRegistryImpl(parser, terminal, () -> Paths.get("."), null);

        // Create builtins
        Builtins builtins = new Builtins(registry::commandRegistry, () -> Paths.get("."), null, null);
        // highlight-end

        // Register commands
        registry.register("help", builtins::help);
        registry.register("ls", builtins::ls);
        registry.register("cat", builtins::cat);
        registry.register("less", builtins::less);

        // Set up completers
        SystemCompleter completer = builtins.compileCompleters();

        // Create line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer)
                .parser(parser)
                .build();

        registry.setLineReader(reader);

        // Main command loop
        PrintWriter writer = terminal.writer();
        while (true) {
            try {
                String line = reader.readLine("registry> ");
                if (line.trim().equalsIgnoreCase("exit")) {
                    break;
                }

                // Execute the command
                registry.execute(line);
            } catch (HelpException e) {
                writer.println(e.getMessage());
            } catch (Exception e) {
                writer.println("Error: " + e.getMessage());
                e.printStackTrace(writer);
            }
            writer.flush();
        }
    }
}
```

## Nano Text Editor

The builtins module includes a Nano-like text editor:

```java title="NanoEditorExample.java"
import org.jline.builtins.Nano;
import org.jline.builtins.Nano.NanoConfig;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.nio.file.Paths;

public class NanoEditorExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // highlight-start
        // Configure Nano
        NanoConfig config = new NanoConfig.Builder()
                .tabSize(4)
                .tabToSpaces(true)
                .build();

        // Launch Nano editor
        Nano nano = new Nano(terminal, config);
        nano.open(Paths.get("example.txt"));
        // highlight-end
    }
}
```

## Best Practices

When using the JLine builtins module, consider these best practices:

1. **Use SystemRegistry for Command Management**: The `SystemRegistryImpl` provides a clean way to register and manage commands.

2. **Leverage Built-in Commands**: Use the provided commands like `ls`, `cat`, and `less` instead of reimplementing them.

3. **Combine with Completers**: Pair built-in commands with appropriate completers for a better user experience.

4. **Use TailTipWidgets for Contextual Help**: The TailTip widgets provide inline help that improves usability.

5. **Customize Table Formatting**: Adjust table formatting to match your application's style and the terminal's capabilities.

6. **Handle Exceptions Properly**: Built-in commands may throw exceptions that should be caught and handled appropriately.

7. **Consider Terminal Capabilities**: Some built-in features may depend on terminal capabilities, so check for support before using them.
