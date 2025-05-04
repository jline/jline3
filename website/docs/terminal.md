---
sidebar_position: 2
---

# Terminal Handling

JLine provides a powerful abstraction for terminal handling through its `Terminal` interface and implementations. This allows your application to interact with different terminal types in a consistent way.

The Terminal component is the foundation of JLine's architecture, providing the low-level interface to the terminal device. It's used by the [LineReader](./line-reader.md) to provide advanced line editing capabilities.

## Creating a Terminal

The `TerminalBuilder` class provides a fluent API for creating terminal instances:

```java title="TerminalCreationExample.java"
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.nio.charset.Charset;

public class TerminalCreationExample {
    public static void main(String[] args) throws IOException {
        // highlight-next-line
        // Create a system terminal (auto-detected)
        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .build();

        // Create a dumb terminal (minimal functionality)
        Terminal dumbTerminal = TerminalBuilder.builder()
                .dumb(true)
                .build();

        // Create a terminal with specific settings
        Terminal customTerminal = TerminalBuilder.builder()
                .name("CustomTerminal")
                .system(false)
                .streams(System.in, System.out)
                .encoding(Charset.forName("UTF-8"))
                .jansi(true)
                .build();
    }
}
```

## Terminal Capabilities

Once you have a terminal instance, you can query its capabilities:

```java title="TerminalCapabilitiesExample.java"
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;

public class TerminalCapabilitiesExample {
    public void checkCapabilities(Terminal terminal) {
        // Check if the terminal supports ANSI
        boolean supportsAnsi = terminal.getType().contains("ansi");

        // highlight-start
        // Get terminal size
        Size size = terminal.getSize();
        int width = size.getColumns();
        int height = size.getRows();
        // highlight-end

        // Check if the terminal is interactive
        boolean interactive = terminal.isInteractive();

        System.out.printf("Terminal: %s, Size: %dx%d, Interactive: %b%n",
                terminal.getType(), width, height, interactive);
    }
}
```

## Terminal Output

You can write directly to the terminal:

```java title="TerminalOutputExample.java"
import org.jline.terminal.Terminal;

import java.io.PrintWriter;

public class TerminalOutputExample {
    public void writeOutput(Terminal terminal) {
        // Get the terminal writer
        PrintWriter writer = terminal.writer();

        // Write text
        writer.println("Hello, JLine!");
        writer.flush();

        // highlight-start
        // Use ANSI escape sequences for formatting (if supported)
        writer.println("\u001B[1;31mThis text is bold and red\u001B[0m");
        writer.flush();
        // highlight-end
    }
}
```

## Terminal Input

For direct terminal input (without using LineReader):

```java title="TerminalInputExample.java" showLineNumbers
import org.jline.terminal.Terminal;
import org.jline.utils.NonBlockingReader;

import java.io.IOException;

public class TerminalInputExample {
    public void readInput(Terminal terminal) throws IOException {
        // Get the terminal reader
        NonBlockingReader reader = terminal.reader();

        // Read a character (blocking)
        int c = reader.read();
        System.out.println("Read character: " + (char)c);

        // Check if input is available
        boolean hasInput = reader.available() > 0;

        // Read with timeout
        int c2 = reader.read(100); // Wait up to 100ms
        if (c2 != -1) {
            System.out.println("Read character with timeout: " + (char)c2);
        }
    }
}
```

## Terminal Signals

JLine can handle terminal signals:

```java title="TerminalSignalsExample.java"
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.Terminal.Signal;

public class TerminalSignalsExample {
    public void setupSignalHandlers(Terminal terminal) {
        // highlight-start
        terminal.handle(Signal.INT, signal -> {
            // Handle Ctrl+C
            System.out.println("Received SIGINT");
        });
        // highlight-end

        terminal.handle(Signal.WINCH, signal -> {
            // Handle terminal resize
            Size size = terminal.getSize();
            System.out.println("Terminal resized to " + size.getColumns() + "x" + size.getRows());
        });
    }
}
```

## Closing the Terminal

Always close the terminal when you're done with it:

```java title="TerminalCloseExample.java"
import org.jline.terminal.Terminal;

public class TerminalCloseExample {
    public void closeTerminal(Terminal terminal) {
        try {
            // Always close the terminal when done
            terminal.close();
            System.out.println("Terminal closed successfully");
        } catch (Exception e) {
            System.err.println("Error closing terminal: " + e.getMessage());
        }
    }
}
```

## Advanced Terminal Features

JLine's terminal handling includes several advanced features:

### Raw Mode

```java title="RawModeExample.java"
import org.jline.terminal.Terminal;

import java.io.IOException;

public class RawModeExample {
    public void demonstrateRawMode(Terminal terminal) throws IOException {
        try {
            // highlight-next-line
            // Enter raw mode (disable echo, line buffering, etc.)
            terminal.enterRawMode();

            System.out.println("Terminal is now in raw mode");
            // Do some raw mode operations...

            // Exit raw mode
            terminal.setAttributes(terminal.getAttributes().copy());
            System.out.println("Terminal is back to normal mode");
        } catch (Exception e) {
            System.err.println("Error with raw mode: " + e.getMessage());
        }
    }
}
```

### Cursor Manipulation

```java title="CursorManipulationExample.java"
import org.jline.terminal.Cursor;
import org.jline.terminal.Terminal;

import java.io.IOException;
import java.io.PrintWriter;

public class CursorManipulationExample {
    public void manipulateCursor(Terminal terminal) throws IOException {
        // Get cursor position
        Cursor position = terminal.getCursorPosition(null);
        if (position != null) {
            System.out.printf("Current cursor position: %d,%d%n",
                    position.getX(), position.getY());
        }

        // Get the terminal writer
        PrintWriter writer = terminal.writer();

        // highlight-start
        // Move cursor to row 5, column 10
        writer.write("\u001B[5;10H");
        writer.flush();
        // highlight-end
    }
}
```

### Screen Clearing

JLine provides a more portable way to clear the screen using terminal capabilities:

```java title="ScreenClearingExample.java"
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

public class ScreenClearingExample {
    public void clearScreen(Terminal terminal) {
        // highlight-start
        // Clear screen using terminal capabilities
        terminal.puts(Capability.clear_screen);
        // highlight-end

        // Clear line using terminal capabilities
        terminal.puts(Capability.clr_eol);

        // Move cursor to home position
        terminal.puts(Capability.cursor_home);

        terminal.writer().println("Screen and line cleared");
        terminal.flush();
    }
}
```

This approach is preferred over using raw ANSI escape sequences because:

1. It's more portable across different terminal types
2. It automatically adapts to the terminal's capabilities
3. It works even on terminals that don't support ANSI sequences

### Mouse Support

JLine supports mouse events in terminals that provide mouse capabilities:

```java title="MouseSupportExample.java" showLineNumbers
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.MouseEvent;
import org.jline.terminal.MouseEvent.Type;
import org.jline.utils.InfoCmp.Capability;

import java.io.IOException;
import java.util.function.Consumer;

public class MouseSupportExample {
    public static void main(String[] args) throws IOException, InterruptedException {
        Terminal terminal = TerminalBuilder.builder().build();

        try {
            // Clear screen
            terminal.puts(Capability.clear_screen);
            terminal.flush();

            // highlight-start
            // Enable mouse tracking
            terminal.trackMouse(Terminal.MouseTracking.Normal);
            // highlight-end

            terminal.writer().println("Mouse tracking enabled. Click anywhere or press q to quit.");
            terminal.writer().println();
            terminal.flush();

            // Set up mouse event handler
            Consumer<MouseEvent> mouseHandler = event -> {
                terminal.writer().println(String.format("Mouse event: type=%s, button=%s, x=%d, y=%d",
                        event.getType(), event.getButton(), event.getX(), event.getY()));
                terminal.flush();
            };

            // Register the mouse handler
            terminal.handle(Terminal.Signal.MOUSE, signal -> {
                MouseEvent event = terminal.readMouseEvent();
                mouseHandler.accept(event);
            });

            // Wait for 'q' key to exit
            while (true) {
                int c = terminal.reader().read(1000);
                if (c == 'q' || c == 'Q') {
                    break;
                }
            }
        } finally {
            // Disable mouse tracking before exiting
            terminal.trackMouse(Terminal.MouseTracking.Off);
            terminal.close();
        }
    }
}
```

Mouse tracking modes:

- `Terminal.MouseTracking.Off`: Disables mouse tracking
- `Terminal.MouseTracking.Normal`: Reports button press and release events
- `Terminal.MouseTracking.Button`: Reports button press, release, and motion events while buttons are pressed
- `Terminal.MouseTracking.Any`: Reports all mouse events, including motion events

Not all terminals support mouse tracking. You can check if mouse tracking is supported:

```java
boolean supportsMouseTracking = terminal.getStringCapability(Capability.key_mouse) != null;
```

## Platform Compatibility

JLine's terminal handling works across different platforms:

- Windows (using JNA or Jansi)
- Unix/Linux (using native PTY)
- macOS (using native PTY)
- Dumb terminals (minimal functionality)

This cross-platform compatibility makes JLine ideal for applications that need to run in various environments.

## Related Topics

- [Terminal Providers](./modules/terminal-providers.md): Learn about the different terminal provider implementations
- [Terminal Attributes](./advanced/terminal-attributes.md): Configure terminal attributes like echo and signals
- [Terminal Size](./advanced/terminal-size.md): Handle terminal size changes
- [Screen Clearing](./advanced/screen-clearing.md): Clear the screen and control cursor positioning
- [Line Reader](./line-reader.md): Build on top of the terminal for line editing capabilities
