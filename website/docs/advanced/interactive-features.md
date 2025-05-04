---
sidebar_position: 2
---

# Interactive Features

JLine provides several advanced features that enhance the interactive experience of command-line applications. This guide covers some of the most powerful interactive features: PrintAboveWriter, LineReader#printAbove, Status, and Tailtips.

## PrintAboveWriter and LineReader#printAbove

One of JLine's most powerful features is the ability to print text above the current input line. This is particularly useful for displaying asynchronous information (like notifications or progress updates) without disrupting the user's input.

### Using LineReader#printAbove

The simplest way to print above the current line is to use the `printAbove` method of the `LineReader` class:

import CodeSnippet from '@site/src/components/CodeSnippet';

<CodeSnippet name="PrintAboveExample" />

In this example, notifications will appear above the input line, and the user can continue typing without interruption.

### Using PrintAboveWriter

For more control, you can use the `PrintAboveWriter` class:

<CodeSnippet name="PrintAboveWriterExample" />

The `PrintAboveWriter` class provides a standard `PrintWriter` interface, making it easy to integrate with existing code that expects a `PrintWriter`.

### Best Practices

When using these features, keep these best practices in mind:

1. **Use sparingly**: Too many messages can be distracting.
2. **Keep messages concise**: Long messages may wrap and take up too much screen space.
3. **Consider styling**: Use colors and styles to differentiate types of messages.
4. **Flush the writer**: Always call `flush()` after writing to ensure the message is displayed immediately.
5. **Thread safety**: Access to the terminal should be synchronized if multiple threads are writing to it.

## Status Line

JLine's Status feature allows you to display persistent status information at the bottom of the terminal. This is useful for showing application state, connection status, or other contextual information.

### Basic Status Usage

```java title="StatusExample.java" showLineNumbers
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Status;

public class StatusExample {
    public static void main(String[] args) throws Exception {
        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

        // Create a Status instance
        Status status = Status.getStatus(terminal);
        if (status != null) {
            // highlight-start
            // Update the status line
            status.update(new AttributedStringBuilder()
                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
                    .append("Connected to server | ")
                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                    .append("3 tasks running")
                    .toAttributedString());
            // highlight-end
        }

        // Read input normally
        while (true) {
            String line = reader.readLine("prompt> ");
            System.out.println("You entered: " + line);
        }
    }
}
```

### Dynamic Status Updates

You can update the status line dynamically to reflect changes in your application's state:

```java
// Start a background thread to update the status
new Thread(() -> {
    try {
        int taskCount = 0;
        while (true) {
            Thread.sleep(2000);
            taskCount = (taskCount + 1) % 10;

            if (status != null) {
                status.update(new AttributedStringBuilder()
                        .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
                        .append("Connected to server | ")
                        .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                        .append(taskCount + " tasks running")
                        .toAttributedString());
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}).start();
```

### Status with Multiple Segments

You can create a more complex status line with multiple segments:

```java
// Create a multi-segment status line
AttributedStringBuilder asb = new AttributedStringBuilder();

// Left-aligned segment
asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
   .append("Server: Connected");

// Center segment (with padding)
int width = terminal.getWidth();
int leftLen = "Server: Connected".length();
int rightLen = "Users: 42".length();
int padding = (width - leftLen - rightLen) / 2;
for (int i = 0; i < padding; i++) {
    asb.append(" ");
}

// Right-aligned segment
asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
   .append("Users: 42");

status.update(asb.toAttributedString());
```

## Tailtips

Tailtips provide contextual hints or suggestions that appear after the cursor. They're useful for showing completion possibilities, command syntax, or other helpful information.

### Basic Tailtips Usage

```java title="TailtipExample.java" showLineNumbers
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public class TailtipExample {
    public static void main(String[] args) throws Exception {
        Terminal terminal = TerminalBuilder.builder().build();
        DefaultParser parser = new DefaultParser();

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(parser)
                // highlight-next-line
                .variable(LineReader.TAILTIP_ENABLED, true)
                .build();

        // Read input with tailtips
        while (true) {
            // error-start
            String line = reader.readLine("prompt> ", null,
                    (String) null, null,
                    s -> {
                        // This function provides the tailtip based on current input
                        if (s.startsWith("help")) {
                            return new AttributedStringBuilder()
                                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
                                    .append(" [command] - Display help for command")
                                    .toAttributedString();
                        } else if (s.startsWith("connect")) {
                            return new AttributedStringBuilder()
                                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
                                    .append(" <host> <port> - Connect to server")
                                    .toAttributedString();
                        }
                        return null;
                    });
            // error-end

            System.out.println("You entered: " + line);
        }
    }
}
```

### Command-Specific Tailtips

You can provide different tailtips based on the command being typed:

```java
// Map of commands to their syntax help
Map<String, String> commandHelp = new HashMap<>();
commandHelp.put("help", "[command] - Display help for command");
commandHelp.put("connect", "<host> <port> - Connect to server");
commandHelp.put("disconnect", "- Disconnect from server");
commandHelp.put("list", "[pattern] - List available resources");

// Read input with command-specific tailtips
while (true) {
    String line = reader.readLine("prompt> ", null,
            (String) null, null,
            s -> {
                // Extract the command part
                String[] parts = s.split("\\s+", 2);
                String cmd = parts[0];

                // Look up help for this command
                String help = commandHelp.get(cmd);
                if (help != null) {
                    return new AttributedStringBuilder()
                            .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
                            .append(" " + help)
                            .toAttributedString();
                }
                return null;
            });

    System.out.println("You entered: " + line);
}
```

### Dynamic Tailtips Based on Context

You can provide more sophisticated tailtips based on the current parsing context:

```java
// Read input with context-aware tailtips
while (true) {
    String line = reader.readLine("prompt> ", null,
            (String) null, null,
            s -> {
                try {
                    // Parse the current line
                    ParsedLine pl = parser.parse(s, s.length());
                    String word = pl.word();
                    List<String> words = pl.words();

                    // Command-specific help based on context
                    if (words.size() >= 1) {
                        String cmd = words.get(0);

                        if (cmd.equals("connect")) {
                            if (words.size() == 1) {
                                // Just the command
                                return new AttributedStringBuilder()
                                        .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
                                        .append(" <host> <port> - Connect to server")
                                        .toAttributedString();
                            } else if (words.size() == 2) {
                                // Command and host
                                return new AttributedStringBuilder()
                                        .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
                                        .append(" <port> - Port number to connect to")
                                        .toAttributedString();
                            }
                        }
                    }
                } catch (Exception e) {
                    // Ignore parsing errors
                }
                return null;
            });

    System.out.println("You entered: " + line);
}
```

## Combining Features

These features can be combined to create a highly interactive and informative command-line interface:

```java
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.PrintAboveWriter;
import org.jline.utils.Status;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class InteractiveExample {
    public static void main(String[] args) throws Exception {
        Terminal terminal = TerminalBuilder.builder().build();
        DefaultParser parser = new DefaultParser();

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(parser)
                .variable(LineReader.TAILTIP_ENABLED, true)
                .build();

        // Set up PrintAboveWriter
        PrintWriter writer = new PrintAboveWriter(terminal, reader::printAbove);

        // Set up Status
        Status status = Status.getStatus(terminal);
        if (status != null) {
            status.update(new AttributedStringBuilder()
                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
                    .append("Ready")
                    .toAttributedString());
        }

        // Command help for tailtips
        Map<String, String> commandHelp = new HashMap<>();
        commandHelp.put("help", "[command] - Display help for command");
        commandHelp.put("connect", "<host> <port> - Connect to server");
        commandHelp.put("disconnect", "- Disconnect from server");
        commandHelp.put("list", "[pattern] - List available resources");

        // Start a background thread for notifications
        new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    Thread.sleep(3000);

                    // Print notification above
                    AttributedStringBuilder asb = new AttributedStringBuilder();
                    asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                       .append("System notification #")
                       .append(String.valueOf(i));

                    writer.println(asb.toAnsi(terminal));
                    writer.flush();

                    // Update status
                    if (status != null) {
                        status.update(new AttributedStringBuilder()
                                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
                                .append("Notification received: ")
                                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                                .append(String.valueOf(i))
                                .toAttributedString());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // Main input loop with tailtips
        while (true) {
            String line = reader.readLine("prompt> ", null,
                    (String) null, null,
                    s -> {
                        // Extract the command part
                        String[] parts = s.split("\\s+", 2);
                        String cmd = parts[0];

                        // Look up help for this command
                        String help = commandHelp.get(cmd);
                        if (help != null) {
                            return new AttributedStringBuilder()
                                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
                                    .append(" " + help)
                                    .toAttributedString();
                        }
                        return null;
                    });

            System.out.println("You entered: " + line);

            // Update status based on command
            if (status != null) {
                status.update(new AttributedStringBuilder()
                        .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
                        .append("Last command: ")
                        .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
                        .append(line)
                        .toAttributedString());
            }
        }
    }
}
```

## Best Practices

When using these interactive features, keep these guidelines in mind:

1. **Consistency**: Use consistent styling and positioning for similar types of information.
2. **Clarity**: Make sure the information is clear and concise.
3. **Performance**: Update the status and tailtips only when necessary to avoid performance issues.
4. **Accessibility**: Don't rely solely on colors for conveying information.
5. **Thread safety**: Synchronize access to shared resources when updating from multiple threads.

These interactive features can significantly enhance the user experience of your command-line application, making it more informative and responsive.
