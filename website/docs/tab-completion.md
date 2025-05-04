---
sidebar_position: 4
---

# Tab Completion

Tab completion is one of JLine's most powerful features, allowing users to efficiently navigate and use your command-line application.

## Basic Completion

To add completion to your `LineReader`, you need to implement the `Completer` interface:

import CodeSnippet from '@site/src/components/CodeSnippet';

<CodeSnippet name="BasicCompletionExample" />

## Completer Types

JLine provides several built-in completers:

### StringsCompleter

Completes from a fixed set of strings:

```java title="StringsCompleterExample.java"
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.StringsCompleter;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

public class StringsCompleterExample {
    public void demonstrateStringsCompleter() {
        // Complete with fixed strings
        Completer stringsCompleter = new StringsCompleter("add", "remove", "list", "help");

        // highlight-start
        // Complete with dynamic strings
        Supplier<Collection<String>> dynamicStrings = this::getCurrentCommands;
        Completer dynamicCompleter = new StringsCompleter(dynamicStrings);
        // highlight-end

        System.out.println("Completers created successfully");
    }

    private Collection<String> getCurrentCommands() {
        // In a real application, this might fetch commands from a registry
        return Arrays.asList("connect", "disconnect", "status", "help");
    }
}
```

### FileNameCompleter

Completes file and directory names:

```java title="FileNameCompleterExample.java"
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.FileNameCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class FileNameCompleterExample {
    public static void main(String[] args) throws IOException {
        // highlight-next-line
        Completer fileCompleter = new FileNameCompleter();

        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(fileCompleter)
                .build();

        System.out.println("Type a file path and press Tab to complete it");
        String line = reader.readLine("file> ");
        System.out.println("You selected: " + line);
    }
}
```

### ArgumentCompleter

Handles command-line arguments with different completers for each position:

<CodeSnippet name="ArgumentCompleterExample" />

### TreeCompleter

Creates a tree of completion options:

```java title="TreeCompleterExample.java" showLineNumbers
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.TreeCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

import static org.jline.reader.impl.completer.TreeCompleter.node;

public class TreeCompleterExample {
    public static void main(String[] args) throws IOException {
        // highlight-start
        Completer treeCompleter = new TreeCompleter(
                node("help",
                        node("commands"),
                        node("syntax")
                ),
                node("set",
                        node("color",
                                node("red", "green", "blue")
                        ),
                        node("size",
                                node("small", "medium", "large")
                        )
                )
        );
        // highlight-end

        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(treeCompleter)
                .build();

        System.out.println("Type a command and press Tab to navigate the command tree");
        String line = reader.readLine("tree> ");
        System.out.println("You entered: " + line);
    }
}
```

### AggregateCompleter

Combines multiple completers:

```java title="AggregateCompleterExample.java"
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.FileNameCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class AggregateCompleterExample {
    public static void main(String[] args) throws IOException {
        // highlight-start
        Completer aggregateCompleter = new AggregateCompleter(
                new StringsCompleter("help", "exit"),
                new ArgumentCompleter(
                        new StringsCompleter("open"),
                        new FileNameCompleter()
                )
        );
        // highlight-end

        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(aggregateCompleter)
                .build();

        System.out.println("Type a command and press Tab to see completions");
        String line = reader.readLine("agg> ");
        System.out.println("You entered: " + line);
    }
}
```

## Custom Completers

You can create your own completers by implementing the `Completer` interface:

```java title="CustomCompleter.java" showLineNumbers
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;

public class CustomCompleter implements Completer {
    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        // Get the word being completed
        String word = line.word();

        // highlight-start
        // Add completion candidates based on the current word
        if ("he".startsWith(word)) {
            candidates.add(new Candidate("help", "help", null, "Show help", null, null, true));
        }
        if ("ex".startsWith(word)) {
            candidates.add(new Candidate("exit", "exit", null, "Exit application", null, null, true));
        }
        // highlight-end

        // You can add more sophisticated logic here
        if ("co".startsWith(word)) {
            candidates.add(new Candidate("connect", "connect", null, "Connect to server", null, null, true));
        }
        if ("di".startsWith(word)) {
            candidates.add(new Candidate("disconnect", "disconnect", null, "Disconnect from server", null, null, true));
        }
    }
}
```

## Completion Behavior

You can customize how completion works:

```java title="CompletionBehaviorExample.java"
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class CompletionBehaviorExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        Completer completer = new StringsCompleter("help", "exit", "list", "connect", "disconnect");

        // highlight-start
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer)
                .option(LineReader.Option.AUTO_LIST, true)  // Automatically list options
                .option(LineReader.Option.LIST_PACKED, true)  // Display completions in a compact form
                .option(LineReader.Option.AUTO_MENU, true)  // Show menu automatically
                .option(LineReader.Option.MENU_COMPLETE, true)  // Cycle through completions
                .build();
        // highlight-end

        System.out.println("Type a command and press Tab to see enhanced completion behavior");
        String line = reader.readLine("cmd> ");
        System.out.println("You entered: " + line);
    }
}
```

## Advanced Completion Features

### Completion with Descriptions

You can provide descriptions for completion candidates:

```java title="CandidatesWithDescriptionsExample.java"
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CandidatesWithDescriptionsExample {
    public static void main(String[] args) throws IOException {
        // highlight-start
        List<Candidate> candidates = new ArrayList<>();
        candidates.add(new Candidate("help", "help", null, "Display help information", null, null, true));
        candidates.add(new Candidate("exit", "exit", null, "Exit the application", null, null, true));

        Completer completer = (reader, line, completions) -> {
            completions.addAll(candidates);
        };
        // highlight-end

        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer)
                .option(LineReader.Option.AUTO_LIST, true)
                .build();

        System.out.println("Type a command and press Tab to see completions with descriptions");
        String line = reader.readLine("desc> ");
        System.out.println("You entered: " + line);
    }
}
```

### Context-Aware Completion

Create completers that are aware of the current context:

```java
public class ContextAwareCompleter implements Completer {
    private final Map<String, Completer> contextCompleters = new HashMap<>();

    public ContextAwareCompleter() {
        contextCompleters.put("default", new StringsCompleter("help", "context", "exit"));
        contextCompleters.put("file", new FileNameCompleter());
        contextCompleters.put("user", new StringsCompleter("admin", "guest", "user1", "user2"));
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        // Get current context from reader variables
        String context = (String) reader.getVariable("CONTEXT");
        if (context == null) {
            context = "default";
        }

        // Use the appropriate completer for this context
        Completer contextCompleter = contextCompleters.getOrDefault(context,
                                                                   contextCompleters.get("default"));
        contextCompleter.complete(reader, line, candidates);
    }
}
```

### Completion with Colors

You can colorize completion candidates:

```java
public class ColoredCompleter implements Completer {
    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        // Command in bold red
        candidates.add(new Candidate("help", "help", null, "Show help", null,
                                    AttributedStyle.BOLD.foreground(AttributedStyle.RED), true));

        // File in blue
        candidates.add(new Candidate("file.txt", "file.txt", null, "A text file", null,
                                    AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE), true));
    }
}
```

## Additional Completers in org.jline.reader.impl.completer

Besides the completers already covered, JLine provides several other completers in the `org.jline.reader.impl.completer` package:

### NullCompleter

A completer that always returns no completions. Useful as a terminal completer in an `ArgumentCompleter`:

```java title="NullCompleterExample.java"
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class NullCompleterExample {
    public static void main(String[] args) throws IOException {
        // highlight-start
        // Create an argument completer with a null completer at the end
        Completer completer = new ArgumentCompleter(
                new StringsCompleter("command"),
                new StringsCompleter("subcommand1", "subcommand2"),
                NullCompleter.INSTANCE
        );
        // highlight-end

        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer)
                .build();

        String line = reader.readLine("null> ");
        System.out.println("You entered: " + line);
    }
}
```

### DirectoriesCompleter

Completes directory names only:

```java title="DirectoriesCompleterExample.java"
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.DirectoriesCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.nio.file.Paths;

public class DirectoriesCompleterExample {
    public static void main(String[] args) throws IOException {
        // highlight-start
        // Create a completer that only completes directory names
        Completer dirCompleter = new DirectoriesCompleter(Paths.get("."));
        // highlight-end

        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(dirCompleter)
                .build();

        String line = reader.readLine("dir> ");
        System.out.println("You selected directory: " + line);
    }
}
```

### FilesCompleter

Completes file names with optional filtering:

```java title="FilesCompleterExample.java"
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.FilesCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.function.Predicate;

public class FilesCompleterExample {
    public static void main(String[] args) throws IOException {
        // highlight-start
        // Create a completer that only completes .txt files
        Predicate<java.nio.file.Path> filter = path ->
            path.toString().endsWith(".txt") || path.toFile().isDirectory();
        Completer filesCompleter = new FilesCompleter(Paths.get("."), filter);
        // highlight-end

        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(filesCompleter)
                .build();

        String line = reader.readLine("file> ");
        System.out.println("You selected file: " + line);
    }
}
```

### RegexCompleter

Completes based on regular expression patterns:

```java title="RegexCompleterExample.java"
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.RegexCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RegexCompleterExample {
    public static void main(String[] args) throws IOException {
        // highlight-start
        // Define a regex pattern and completers for each group
        Map<String, Completer> completers = new HashMap<>();
        completers.put("COMMAND", new org.jline.reader.impl.completer.StringsCompleter("help", "exit", "connect"));
        completers.put("TARGET", new org.jline.reader.impl.completer.StringsCompleter("server1", "server2", "server3"));
        completers.put("OPTION", new org.jline.reader.impl.completer.StringsCompleter("-v", "-f", "-h"));

        // Create a regex completer with the pattern
        Completer regexCompleter = new RegexCompleter("(COMMAND) (TARGET) (OPTION)", completers);
        // highlight-end

        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(regexCompleter)
                .build();

        String line = reader.readLine("regex> ");
        System.out.println("You entered: " + line);
    }
}
```

### EnumCompleter

Completes enum values:

```java title="EnumCompleterExample.java"
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.EnumCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class EnumCompleterExample {
    public static void main(String[] args) throws IOException {
        // highlight-start
        // Create a completer that completes enum values
        Completer enumCompleter = new EnumCompleter(TimeUnit.class);
        // highlight-end

        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(enumCompleter)
                .build();

        String line = reader.readLine("timeunit> ");
        System.out.println("You selected: " + line);
    }
}
```

## Completers in org.jline.builtins.Completers

The `org.jline.builtins.Completers` class provides additional completers that are useful for building command-line applications:

### SystemCompleter

A completer that manages completions for multiple commands:

```java title="SystemCompleterExample.java"
import org.jline.builtins.Completers.SystemCompleter;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.Arrays;

public class SystemCompleterExample {
    public static void main(String[] args) throws IOException {
        // highlight-start
        // Create a system completer
        SystemCompleter systemCompleter = new SystemCompleter();

        // Add completers for different commands
        systemCompleter.add("help", new StringsCompleter("commands", "usage", "options"));

        // Add a more complex completer for the "connect" command
        systemCompleter.add("connect", new ArgumentCompleter(
                new StringsCompleter("connect"),
                new StringsCompleter("server1", "server2", "server3"),
                NullCompleter.INSTANCE
        ));

        // Compile the completers
        systemCompleter.compile();
        // highlight-end

        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(systemCompleter)
                .build();

        String line = reader.readLine("system> ");
        System.out.println("You entered: " + line);
    }
}
```

### TreeCompleter (from builtins package)

The `org.jline.builtins.Completers.TreeCompleter` provides similar functionality to the `TreeCompleter` in the `impl.completer` package but with a different API:

```java title="BuiltinsTreeCompleterExample.java"
import org.jline.builtins.Completers.TreeCompleter;
import org.jline.builtins.Completers.TreeCompleter.Node;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

import static org.jline.builtins.Completers.TreeCompleter.node;

public class BuiltinsTreeCompleterExample {
    public static void main(String[] args) throws IOException {
        // highlight-start
        // Create a tree completer
        TreeCompleter treeCompleter = new TreeCompleter(
                node("help",
                        node("commands"),
                        node("usage")
                ),
                node("connect",
                        node("server1"),
                        node("server2")
                ),
                node("exit")
        );
        // highlight-end

        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(treeCompleter)
                .build();

        String line = reader.readLine("tree> ");
        System.out.println("You entered: " + line);
    }
}
```

## Best Practices

- Provide meaningful completions that help users discover functionality
- Include descriptions for non-obvious options
- Group related completions logically
- Consider the context when providing completions
- Use appropriate styling to differentiate types of completions
- Test completion with various input scenarios
- Keep completion fast, especially for large option sets
