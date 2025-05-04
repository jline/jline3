---
sidebar_position: 5
---

# Attributed Strings

JLine provides powerful support for styled text through its attributed string classes. These classes allow you to create text with colors, formatting, and other visual attributes for rich terminal output.

## Core Classes

JLine's attributed string functionality is built around three main classes:

1. **AttributedString**: An immutable string with style attributes
2. **AttributedStyle**: Defines style attributes like colors and formatting
3. **AttributedStringBuilder**: A builder for creating attributed strings

## AttributedStyle

The `AttributedStyle` class represents the styling attributes that can be applied to text. It includes support for:

- Foreground colors
- Background colors
- Text attributes (bold, italic, underline, etc.)

### Creating Styles

```java title="AttributedStyleExample.java" showLineNumbers
import org.jline.utils.AttributedStyle;

// Create a default style
AttributedStyle defaultStyle = AttributedStyle.DEFAULT;

// Create a style with foreground color
AttributedStyle redText = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED);

// Create a style with background color
AttributedStyle blueBackground = AttributedStyle.DEFAULT.background(AttributedStyle.BLUE);

// Create a style with text attributes
AttributedStyle boldText = AttributedStyle.DEFAULT.bold();
AttributedStyle italicText = AttributedStyle.DEFAULT.italic();
AttributedStyle underlinedText = AttributedStyle.DEFAULT.underline();

// Combine multiple attributes
AttributedStyle boldRedOnBlue = AttributedStyle.DEFAULT
        .foreground(AttributedStyle.RED)
        .background(AttributedStyle.BLUE)
        .bold();
```

### Available Colors

JLine supports 8 basic colors and their bright variants:

| Color Constant | Description |
|----------------|-------------|
| `AttributedStyle.BLACK` | Black |
| `AttributedStyle.RED` | Red |
| `AttributedStyle.GREEN` | Green |
| `AttributedStyle.YELLOW` | Yellow |
| `AttributedStyle.BLUE` | Blue |
| `AttributedStyle.MAGENTA` | Magenta |
| `AttributedStyle.CYAN` | Cyan |
| `AttributedStyle.WHITE` | White |

You can also use bright variants with the `bright()` method:

```java
AttributedStyle brightRed = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold();
```

For terminals that support it, you can also use 256-color mode:

```java
// Use a specific color from the 256-color palette
AttributedStyle color123 = AttributedStyle.DEFAULT.foreground(123);
```

### Text Attributes

JLine supports several text attributes:

| Method | Description |
|--------|-------------|
| `bold()` | Bold text |
| `faint()` | Faint text (reduced intensity) |
| `italic()` | Italic text |
| `underline()` | Underlined text |
| `blink()` | Blinking text |
| `inverse()` | Inverse colors (swap foreground and background) |
| `conceal()` | Hidden text |
| `crossedOut()` | Crossed-out text |

## AttributedString

The `AttributedString` class represents an immutable string with style attributes. It's the final product that you'll display to the terminal.

### Creating AttributedStrings

```java title="AttributedStringExample.java" showLineNumbers
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

// Create an AttributedString with a style
AttributedString errorMessage = new AttributedString(
        "Error: File not found",
        AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold()
);

// Create an AttributedString with default style
AttributedString plainText = new AttributedString("Plain text");
```

### Displaying AttributedStrings

```java title="DisplayAttributedStringExample.java" showLineNumbers
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import java.io.IOException;

public class DisplayAttributedStringExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Create an attributed string
        AttributedString message = new AttributedString(
                "This is a styled message",
                AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN).bold()
        );
        
        // Print the attributed string to the terminal
        message.println(terminal);
        
        // Or get the ANSI escape sequence string
        String ansiString = message.toAnsi(terminal);
        System.out.println("ANSI string: " + ansiString);
        
        terminal.close();
    }
}
```

## AttributedStringBuilder

The `AttributedStringBuilder` class provides a builder pattern for creating complex attributed strings with multiple styles.

### Building Complex Strings

```java title="AttributedStringBuilderExample.java" showLineNumbers
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class AttributedStringBuilderExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Create a builder
        AttributedStringBuilder builder = new AttributedStringBuilder();
        
        // Append text with different styles
        builder.append("System status: ")
               .style(AttributedStyle.DEFAULT.bold())
               .append("ONLINE")
               .style(AttributedStyle.DEFAULT)
               .append(" (")
               .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
               .append("All systems operational")
               .style(AttributedStyle.DEFAULT)
               .append(")");
        
        // Build the final AttributedString
        AttributedString result = builder.toAttributedString();
        
        // Print to terminal
        result.println(terminal);
        
        terminal.close();
    }
}
```

### Styling Specific Sections

```java title="StylingSpecificSectionsExample.java" showLineNumbers
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

// Create a builder
AttributedStringBuilder builder = new AttributedStringBuilder();

// Append text with default style
builder.append("Command: ");

// Append text with a specific style
builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW).bold())
       .append("git")
       .style(AttributedStyle.DEFAULT)
       .append(" ");

// Append more styled text
builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
       .append("commit")
       .style(AttributedStyle.DEFAULT)
       .append(" ");

// Append an option with a different style
builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
       .append("-m")
       .style(AttributedStyle.DEFAULT)
       .append(" ");

// Append a quoted string with yet another style
builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA))
       .append("\"Fix critical bug\"");
```

## Common Use Cases

### Status Messages

```java title="StatusMessageExample.java" showLineNumbers
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public class StatusMessageExample {
    public static AttributedString createStatusMessage(String status) {
        AttributedStringBuilder builder = new AttributedStringBuilder();
        builder.append("Status: ");
        
        switch (status.toLowerCase()) {
            case "success":
                builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                       .append("SUCCESS");
                break;
            case "warning":
                builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
                       .append("WARNING");
                break;
            case "error":
                builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
                       .append("ERROR");
                break;
            default:
                builder.append(status);
                break;
        }
        
        return builder.toAttributedString();
    }
}
```

### Progress Bars

```java title="ProgressBarExample.java" showLineNumbers
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.IOException;

public class ProgressBarExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        for (int i = 0; i <= 100; i += 5) {
            // Create a progress bar
            AttributedString progressBar = createProgressBar(i, 50);
            
            // Clear the line and print the progress bar
            terminal.writer().print("\r");
            progressBar.print(terminal);
            
            // Sleep to simulate work
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        terminal.writer().println();
        terminal.close();
    }
    
    private static AttributedString createProgressBar(int percentage, int width) {
        AttributedStringBuilder builder = new AttributedStringBuilder();
        
        // Calculate the number of completed and remaining segments
        int completed = width * percentage / 100;
        int remaining = width - completed;
        
        // Add the percentage
        builder.style(AttributedStyle.DEFAULT.bold())
               .append(String.format("%3d%% ", percentage));
        
        // Add the progress bar
        builder.append("[");
        
        // Add completed segments
        builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
               .append("=".repeat(completed));
        
        // Add remaining segments
        builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLACK))
               .append(" ".repeat(remaining));
        
        // Close the progress bar
        builder.style(AttributedStyle.DEFAULT)
               .append("]");
        
        return builder.toAttributedString();
    }
}
```

### Syntax Highlighting

```java title="SyntaxHighlightingExample.java" showLineNumbers
import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SyntaxHighlightingExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Define keywords for highlighting
        Set<String> commands = new HashSet<>(Arrays.asList("select", "from", "where", "insert", "update", "delete"));
        Set<String> operators = new HashSet<>(Arrays.asList("and", "or", "not", "in", "like", "between"));
        
        // Create a custom highlighter
        Highlighter highlighter = (reader, buffer, candidates) -> {
            AttributedStringBuilder builder = new AttributedStringBuilder();
            
            // Simple SQL syntax highlighting
            String[] tokens = buffer.toString().split("\\s+");
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i];
                
                if (i > 0) {
                    builder.append(" ");
                }
                
                if (commands.contains(token.toLowerCase())) {
                    // Highlight commands
                    builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE).bold())
                           .append(token);
                } else if (operators.contains(token.toLowerCase())) {
                    // Highlight operators
                    builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
                           .append(token);
                } else if (token.matches("\\d+")) {
                    // Highlight numbers
                    builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA))
                           .append(token);
                } else if (token.startsWith("'") && token.endsWith("'")) {
                    // Highlight strings
                    builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                           .append(token);
                } else {
                    // Default style
                    builder.style(AttributedStyle.DEFAULT)
                           .append(token);
                }
            }
            
            AttributedString result = builder.toAttributedString();
            candidates.add(result);
            return result;
        };
        
        // Create a line reader with the highlighter
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .highlighter(highlighter)
                .build();
        
        // Read input with syntax highlighting
        String line = reader.readLine("SQL> ");
        System.out.println("You entered: " + line);
        
        terminal.close();
    }
}
```

## Integration with Other JLine Features

### Status Bar

```java title="StatusBarExample.java" showLineNumbers
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Status;

import java.io.IOException;

public class StatusBarExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        
        // Get the status instance
        Status status = Status.getStatus(terminal);
        if (status != null) {
            // Create a styled status message
            AttributedStringBuilder builder = new AttributedStringBuilder();
            builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
                   .append("Server: ")
                   .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                   .append("Connected")
                   .style(AttributedStyle.DEFAULT)
                   .append(" | ")
                   .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
                   .append("Users: ")
                   .style(AttributedStyle.DEFAULT)
                   .append("42");
            
            // Update the status bar
            status.update(builder.toAttributedString());
        }
        
        // Read input normally
        String line = reader.readLine("prompt> ");
        System.out.println("You entered: " + line);
        
        terminal.close();
    }
}
```

### PrintAbove

```java title="PrintAboveExample.java" showLineNumbers
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.IOException;

public class PrintAboveExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        
        // Start a background thread to print styled messages
        new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    Thread.sleep(2000);
                    
                    // Create a styled notification
                    AttributedStringBuilder builder = new AttributedStringBuilder();
                    builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE).bold())
                           .append("[INFO] ")
                           .style(AttributedStyle.DEFAULT)
                           .append("Notification #")
                           .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
                           .append(String.valueOf(i));
                    
                    // Print above the current line
                    reader.printAbove(builder.toAttributedString());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        
        // Read input normally
        while (true) {
            String line = reader.readLine("prompt> ");
            System.out.println("You entered: " + line);
        }
    }
}
```

## Best Practices

1. **Reuse Styles**: Create and reuse common styles for consistency.
   ```java
   // Define common styles
   private static final AttributedStyle ERROR_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold();
   private static final AttributedStyle WARNING_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);
   private static final AttributedStyle SUCCESS_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
   ```

2. **Consider Terminal Capabilities**: Not all terminals support all styling features. Use `terminal.getType()` to check terminal capabilities.

3. **Reset Styles**: Always reset styles after use to avoid unintended styling.
   ```java
   builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
          .append("Error")
          .style(AttributedStyle.DEFAULT)  // Reset to default style
          .append(": Something went wrong");
   ```

4. **Use Builder Pattern**: For complex strings with multiple styles, use `AttributedStringBuilder`.

5. **Test on Different Terminals**: Test your styled output on different terminals to ensure compatibility.

## Conclusion

JLine's attributed string classes provide a powerful way to create rich, styled terminal output. By combining `AttributedStyle`, `AttributedString`, and `AttributedStringBuilder`, you can create visually appealing and informative terminal interfaces that enhance the user experience.
