---
sidebar_position: 10
---

# Screen Clearing and Terminal Control

Controlling the terminal display is an important aspect of creating interactive command-line applications. JLine provides several methods for clearing the screen, positioning the cursor, and controlling other terminal attributes. This guide covers the proper techniques for screen clearing and terminal control in JLine applications.

## Clearing the Screen

There are several ways to clear the screen in JLine, each with its own advantages:

### Using Terminal Capabilities

The most reliable way to clear the screen is to use terminal capabilities:

```java title="ClearScreenCapabilityExample.java" showLineNumbers
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

import java.io.IOException;

public class ClearScreenCapabilityExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Display some text
        terminal.writer().println("This text will be cleared...");
        terminal.writer().println("Wait for it...");
        terminal.flush();
        
        // Wait for 2 seconds
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // highlight-start
        // Clear the screen using terminal capabilities
        terminal.puts(Capability.clear_screen);
        terminal.flush();
        // highlight-end
        
        terminal.writer().println("Screen has been cleared!");
        terminal.writer().println("This is new content after clearing.");
        terminal.flush();
        
        terminal.close();
    }
}
```

This method uses the terminal's native capability to clear the screen, which is the most compatible approach across different terminal types.

### Using the LineReader Widget

If you're using a `LineReader`, you can use the built-in clear screen widget:

```java title="ClearScreenWidgetExample.java" showLineNumbers
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class ClearScreenWidgetExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        
        // Display some text
        terminal.writer().println("This text will be cleared...");
        terminal.writer().println("Wait for it...");
        terminal.flush();
        
        // Wait for 2 seconds
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // highlight-start
        // Clear the screen using the LineReader widget
        reader.callWidget(LineReader.CLEAR_SCREEN);
        // highlight-end
        
        terminal.writer().println("Screen has been cleared!");
        terminal.writer().println("This is new content after clearing.");
        terminal.flush();
        
        terminal.close();
    }
}
```

This method is convenient when you're already using a `LineReader` in your application.

### Using ANSI Escape Sequences

You can also use ANSI escape sequences directly, though this is less portable:

```java title="ClearScreenAnsiExample.java" showLineNumbers
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class ClearScreenAnsiExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Display some text
        terminal.writer().println("This text will be cleared...");
        terminal.writer().println("Wait for it...");
        terminal.flush();
        
        // Wait for 2 seconds
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // highlight-start
        // Clear the screen using ANSI escape sequence
        terminal.writer().print("\033[2J\033[H");
        terminal.flush();
        // highlight-end
        
        terminal.writer().println("Screen has been cleared!");
        terminal.writer().println("This is new content after clearing.");
        terminal.flush();
        
        terminal.close();
    }
}
```

This method works in most modern terminals but may not be supported in all environments.

## Cursor Positioning

Controlling the cursor position is essential for creating interactive terminal interfaces:

### Moving the Cursor

You can move the cursor to a specific position using terminal capabilities:

```java title="CursorPositioningExample.java" showLineNumbers
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

import java.io.IOException;

public class CursorPositioningExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Clear the screen first
        terminal.puts(Capability.clear_screen);
        terminal.flush();
        
        // highlight-start
        // Move cursor to position (5, 10) - row 5, column 10
        moveCursor(terminal, 5, 10);
        terminal.writer().print("Text at position (5, 10)");
        
        // Move cursor to position (8, 15)
        moveCursor(terminal, 8, 15);
        terminal.writer().print("Text at position (8, 15)");
        
        // Move cursor to position (12, 5)
        moveCursor(terminal, 12, 5);
        terminal.writer().print("Text at position (12, 5)");
        // highlight-end
        
        terminal.flush();
        
        // Move cursor to the bottom
        moveCursor(terminal, 20, 1);
        terminal.writer().println("\nPress Enter to exit.");
        terminal.flush();
        
        terminal.reader().readLine();
        terminal.close();
    }
    
    // highlight-start
    // Helper method to move the cursor to a specific position
    private static void moveCursor(Terminal terminal, int row, int column) {
        // Use the cursor_address capability if available
        String cap = terminal.getStringCapability(Capability.cursor_address);
        if (cap != null) {
            terminal.puts(Capability.cursor_address, row, column);
        } else {
            // Fall back to ANSI escape sequence
            terminal.writer().print("\033[" + row + ";" + column + "H");
        }
        terminal.flush();
    }
    // highlight-end
}
```

### Saving and Restoring Cursor Position

You can save and restore the cursor position:

```java title="SaveRestoreCursorExample.java" showLineNumbers
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

import java.io.IOException;

public class SaveRestoreCursorExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Clear the screen
        terminal.puts(Capability.clear_screen);
        terminal.flush();
        
        // Write some initial text
        terminal.writer().println("Initial text line 1");
        terminal.writer().println("Initial text line 2");
        terminal.writer().println("Initial text line 3");
        terminal.flush();
        
        // highlight-start
        // Save cursor position
        terminal.puts(Capability.save_cursor);
        terminal.flush();
        
        // Move cursor to a different position
        terminal.puts(Capability.cursor_address, 10, 5);
        terminal.writer().print("This text is at position (10, 5)");
        terminal.flush();
        
        // Wait for 2 seconds
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Restore cursor position
        terminal.puts(Capability.restore_cursor);
        terminal.flush();
        // highlight-end
        
        terminal.writer().println("Cursor has been restored to its original position");
        terminal.writer().println("This text continues from where we left off");
        terminal.flush();
        
        terminal.close();
    }
}
```

## Erasing Parts of the Screen

Sometimes you need to erase only part of the screen:

```java title="ErasePartsExample.java" showLineNumbers
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

import java.io.IOException;

public class ErasePartsExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Clear the screen
        terminal.puts(Capability.clear_screen);
        terminal.flush();
        
        // Write some text
        terminal.writer().println("Line 1: This line will remain");
        terminal.writer().println("Line 2: This line will be erased");
        terminal.writer().println("Line 3: This line will be erased");
        terminal.writer().println("Line 4: This line will remain");
        terminal.flush();
        
        // Wait for 2 seconds
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // highlight-start
        // Move cursor to the beginning of line 2
        terminal.puts(Capability.cursor_address, 2, 1);
        
        // Erase from cursor to end of line
        terminal.puts(Capability.clr_eol);
        
        // Move cursor to the beginning of line 3
        terminal.puts(Capability.cursor_address, 3, 1);
        
        // Erase from cursor to end of line
        terminal.puts(Capability.clr_eol);
        // highlight-end
        
        terminal.flush();
        
        // Move cursor to the bottom
        terminal.puts(Capability.cursor_address, 5, 1);
        terminal.writer().println("Lines 2 and 3 have been erased");
        terminal.flush();
        
        terminal.close();
    }
}
```

## Terminal Control Sequences

JLine provides access to various terminal control sequences through the `Capability` enum:

```java title="TerminalControlSequencesExample.java" showLineNumbers
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

import java.io.IOException;

public class TerminalControlSequencesExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Clear the screen
        terminal.puts(Capability.clear_screen);
        terminal.flush();
        
        terminal.writer().println("Terminal Control Sequences Example");
        terminal.writer().println("----------------------------------");
        terminal.writer().println();
        terminal.flush();
        
        // highlight-start
        // Turn on bold mode
        terminal.puts(Capability.enter_bold_mode);
        terminal.writer().println("This text is bold");
        
        // Turn off attributes
        terminal.puts(Capability.exit_attribute_mode);
        terminal.writer().println("This text is normal");
        
        // Turn on underline mode
        terminal.puts(Capability.enter_underline_mode);
        terminal.writer().println("This text is underlined");
        
        // Turn off attributes
        terminal.puts(Capability.exit_attribute_mode);
        terminal.writer().println("This text is normal");
        
        // Turn on standout mode (usually reverse video)
        terminal.puts(Capability.enter_standout_mode);
        terminal.writer().println("This text is in standout mode");
        
        // Turn off attributes
        terminal.puts(Capability.exit_attribute_mode);
        terminal.writer().println("This text is normal");
        
        // Turn on blinking mode
        terminal.puts(Capability.enter_blink_mode);
        terminal.writer().println("This text is blinking (if supported)");
        
        // Turn off attributes
        terminal.puts(Capability.exit_attribute_mode);
        // highlight-end
        
        terminal.writer().println();
        terminal.writer().println("Press Enter to exit.");
        terminal.flush();
        
        terminal.reader().readLine();
        terminal.close();
    }
}
```

## Creating a Status Line

You can use terminal control to create a persistent status line:

```java title="StatusLineExample.java" showLineNumbers
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StatusLineExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Clear the screen
        terminal.puts(Capability.clear_screen);
        terminal.flush();
        
        // Get terminal dimensions
        int height = terminal.getHeight();
        int width = terminal.getWidth();
        
        // Main content
        terminal.writer().println("Status Line Example");
        terminal.writer().println("------------------");
        terminal.writer().println();
        terminal.writer().println("This example demonstrates a persistent status line");
        terminal.writer().println("at the bottom of the terminal.");
        terminal.writer().println();
        terminal.writer().println("The status line will update every second.");
        terminal.writer().println();
        terminal.writer().println("Press Enter to exit.");
        terminal.flush();
        
        // Create a thread to update the status line
        Thread statusThread = new Thread(() -> {
            try {
                while (!Thread.interrupted()) {
                    updateStatusLine(terminal, height, width);
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        statusThread.setDaemon(true);
        statusThread.start();
        
        // Wait for Enter key
        terminal.reader().readLine();
        
        // Clean up
        statusThread.interrupt();
        terminal.close();
    }
    
    // highlight-start
    private static void updateStatusLine(Terminal terminal, int height, int width) throws IOException {
        // Save cursor position
        terminal.puts(Capability.save_cursor);
        
        // Move to the bottom line
        terminal.puts(Capability.cursor_address, height - 1, 1);
        
        // Clear the line
        terminal.puts(Capability.clr_eol);
        
        // Create status line content
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String status = "Status: Running";
        String memory = "Memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024) + "MB";
        
        // Format the status line
        StringBuilder statusLine = new StringBuilder();
        
        // Add reverse video for the entire line
        terminal.puts(Capability.enter_standout_mode);
        
        // Left-aligned time
        statusLine.append(time);
        
        // Center-aligned status
        int leftPadding = (width - status.length()) / 2 - time.length();
        if (leftPadding > 0) {
            statusLine.append(" ".repeat(leftPadding));
        }
        statusLine.append(status);
        
        // Right-aligned memory usage
        int rightPadding = width - statusLine.length() - memory.length();
        if (rightPadding > 0) {
            statusLine.append(" ".repeat(rightPadding));
        }
        statusLine.append(memory);
        
        // Ensure the line fills the width
        if (statusLine.length() < width) {
            statusLine.append(" ".repeat(width - statusLine.length()));
        }
        
        // Write the status line
        terminal.writer().print(statusLine.toString());
        
        // Turn off reverse video
        terminal.puts(Capability.exit_standout_mode);
        
        // Restore cursor position
        terminal.puts(Capability.restore_cursor);
        
        terminal.flush();
    }
    // highlight-end
}
```

## Using the Display Class

For more complex screen management, JLine provides the `Display` class:

```java title="DisplayClassExample.java" showLineNumbers
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Display;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DisplayClassExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // highlight-start
        // Create a display for managing the screen
        Display display = new Display(terminal, true);
        
        // Create a list of lines to display
        List<AttributedString> lines = new ArrayList<>();
        lines.add(new AttributedString("Display Class Example", 
                AttributedStyle.DEFAULT.bold()));
        lines.add(new AttributedString("---------------------"));
        lines.add(new AttributedString(""));
        lines.add(new AttributedString("The Display class provides efficient screen management."));
        lines.add(new AttributedString("It only updates the parts of the screen that have changed."));
        lines.add(new AttributedString(""));
        lines.add(new AttributedString("This example will update a counter every second."));
        lines.add(new AttributedString(""));
        lines.add(new AttributedString("Counter: 0", 
                AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE)));
        lines.add(new AttributedString(""));
        lines.add(new AttributedString("Press Enter to exit."));
        
        // Update the display
        display.update(lines, 0);
        // highlight-end
        
        // Create a thread to update the counter
        Thread counterThread = new Thread(() -> {
            try {
                for (int i = 1; i <= 10 && !Thread.interrupted(); i++) {
                    Thread.sleep(1000);
                    
                    // Update only the counter line
                    lines.set(8, new AttributedString("Counter: " + i, 
                            AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE)));
                    
                    // Redisplay
                    display.update(lines, 0);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        counterThread.setDaemon(true);
        counterThread.start();
        
        // Wait for Enter key
        terminal.reader().readLine();
        
        // Clean up
        counterThread.interrupt();
        terminal.close();
    }
}
```

## Best Practices

When working with screen clearing and terminal control:

1. **Use terminal capabilities**: Prefer using terminal capabilities over hardcoded ANSI escape sequences for better compatibility.

2. **Check capability support**: Not all terminals support all capabilities. Check if a capability is supported before using it:

   ```java
   if (terminal.getStringCapability(Capability.clear_screen) != null) {
       terminal.puts(Capability.clear_screen);
   } else {
       // Fall back to a different method
   }
   ```

3. **Flush after terminal operations**: Always call `terminal.flush()` after using `terminal.puts()` to ensure the changes are sent to the terminal.

4. **Save and restore cursor position**: When temporarily moving the cursor, save its position first and restore it afterward.

5. **Handle terminal resize**: Update your display when the terminal is resized to ensure proper layout.

6. **Clean up on exit**: Restore the terminal to a clean state before exiting your application.

7. **Use the Display class for complex UIs**: For complex screen management, use the `Display` class to efficiently update only the changed parts of the screen.

8. **Provide keyboard navigation**: Don't rely solely on cursor positioning; provide keyboard navigation for accessibility.

9. **Test in different terminals**: Test your application in various terminal emulators to ensure compatibility.

10. **Gracefully handle unsupported features**: Provide fallbacks for terminals that don't support certain capabilities.

## Conclusion

JLine provides powerful tools for screen clearing and terminal control, allowing you to create sophisticated terminal user interfaces. By using terminal capabilities and the `Display` class, you can create responsive, efficient, and compatible terminal applications that provide a great user experience across different terminal environments.
