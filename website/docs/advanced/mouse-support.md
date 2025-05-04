---
sidebar_position: 6
---

# Mouse Support

JLine provides support for mouse events in terminal applications, allowing you to create more interactive and user-friendly command-line interfaces. This feature is particularly useful for applications that require point-and-click interactions, such as text editors, file browsers, or interactive menus.

## Mouse Support Basics

Terminal mouse support works by capturing mouse events (clicks, movements, wheel scrolling) and translating them into escape sequences that can be processed by your application. JLine provides an abstraction layer that makes it easy to work with these events.

### Enabling Mouse Support

To enable mouse support in your JLine application:

```java title="MouseSupportBasicsExample.java" showLineNumbers
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

import java.io.IOException;

public class MouseSupportBasicsExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        try {
            // highlight-start
            // Enable mouse tracking
            terminal.puts(Capability.enter_mouse_mode);
            terminal.flush();
            // highlight-end
            
            System.out.println("Mouse tracking enabled. Click anywhere in the terminal...");
            System.out.println("Press Enter to exit.");
            
            // Simple event loop
            while (true) {
                int c = terminal.reader().read();
                if (c == '\r' || c == '\n') {
                    break;
                }
                
                // Process input (including mouse events)
                // Mouse events come as escape sequences
                // We'll see how to properly handle these in the next examples
            }
        } finally {
            // highlight-start
            // Disable mouse tracking before exiting
            terminal.puts(Capability.exit_mouse_mode);
            terminal.flush();
            // highlight-end
            
            terminal.close();
        }
    }
}
```

## Mouse Event Types

JLine can capture several types of mouse events:

- **Mouse Clicks**: Left, middle, and right button clicks
- **Mouse Movement**: Movement with buttons pressed or released
- **Mouse Wheel**: Scrolling up or down
- **Mouse Position**: Coordinates of the mouse pointer

## Handling Mouse Events

To properly handle mouse events, you need to parse the escape sequences that represent mouse actions. JLine's `Terminal.MouseEvent` class helps with this:

```java title="MouseEventHandlingExample.java" showLineNumbers
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.Terminal.MouseEvent;
import org.jline.terminal.Terminal.MouseEventType;
import org.jline.terminal.Terminal.SignalHandler;
import org.jline.terminal.Terminal.Signal;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.NonBlockingReader;

import java.io.IOException;

public class MouseEventHandlingExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        try {
            // Set up signal handler for CTRL+C
            terminal.handle(Signal.INT, SignalHandler.SIG_IGN);
            
            // Enable mouse tracking
            terminal.puts(Capability.enter_mouse_mode);
            terminal.flush();
            
            System.out.println("Mouse tracking enabled. Click or move the mouse in the terminal...");
            System.out.println("Press 'q' to exit.");
            
            NonBlockingReader reader = terminal.reader();
            StringBuilder buffer = new StringBuilder();
            boolean esc = false;
            boolean bracket = false;
            boolean mouse = false;
            
            // Event loop
            while (true) {
                int c = reader.read();
                
                // Check for 'q' to exit
                if (c == 'q') {
                    break;
                }
                
                // highlight-start
                // Parse escape sequences for mouse events
                if (c == '\033') {
                    // ESC character - start of escape sequence
                    esc = true;
                    buffer.setLength(0);
                } else if (esc && c == '[') {
                    // [ character after ESC - potential mouse event
                    bracket = true;
                } else if (esc && bracket && c == 'M') {
                    // M character after ESC[ - confirmed mouse event
                    mouse = true;
                    buffer.setLength(0);
                } else if (mouse && buffer.length() < 3) {
                    // Collect the 3 bytes that define the mouse event
                    buffer.append((char) c);
                    
                    if (buffer.length() == 3) {
                        // We have a complete mouse event
                        int b = buffer.charAt(0) - 32;
                        int x = buffer.charAt(1) - 32;
                        int y = buffer.charAt(2) - 32;
                        
                        // Decode the event type
                        boolean press = (b & 3) != 3;
                        boolean release = (b & 3) == 3;
                        boolean wheel = (b & 64) != 0;
                        
                        // Determine which button was used
                        String button = "unknown";
                        if ((b & 3) == 0) button = "left";
                        if ((b & 3) == 1) button = "middle";
                        if ((b & 3) == 2) button = "right";
                        
                        // Print the event details
                        terminal.writer().println(String.format(
                            "Mouse event: %s button %s at position (%d,%d)",
                            press ? "pressed" : (release ? "released" : (wheel ? "wheel" : "moved")),
                            button,
                            x, y
                        ));
                        terminal.flush();
                        
                        // Reset state
                        esc = false;
                        bracket = false;
                        mouse = false;
                    }
                } else {
                    // Not a mouse event or incomplete sequence
                    esc = false;
                    bracket = false;
                    mouse = false;
                }
                // highlight-end
            }
        } finally {
            // Disable mouse tracking before exiting
            terminal.puts(Capability.exit_mouse_mode);
            terminal.flush();
            
            terminal.close();
        }
    }
}
```

## Mouse Tracking Modes

JLine supports different mouse tracking modes, which determine what types of events are reported:

```java title="MouseTrackingModesExample.java" showLineNumbers
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

import java.io.IOException;

public class MouseTrackingModesExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        try {
            // highlight-start
            // Different mouse tracking modes
            
            // 1. Basic mouse tracking (clicks only)
            terminal.puts(Capability.enter_mouse_mode);
            
            // 2. Extended mouse tracking (clicks and movement)
            // This is terminal-dependent and may require specific escape sequences
            terminal.writer().write("\033[?1000;1002;1006;1015h");
            
            // 3. Any event tracking (clicks, movement, and position reports)
            // This is terminal-dependent and may require specific escape sequences
            terminal.writer().write("\033[?1000;1003;1006;1015h");
            // highlight-end
            
            terminal.flush();
            
            System.out.println("Enhanced mouse tracking enabled.");
            System.out.println("Try clicking, moving, and scrolling the mouse.");
            System.out.println("Press Enter to exit.");
            
            // Wait for Enter key
            while (terminal.reader().read() != '\n') {
                // Process events
            }
        } finally {
            // Disable all mouse tracking modes
            terminal.puts(Capability.exit_mouse_mode);
            terminal.writer().write("\033[?1000;1002;1003;1006;1015l");
            terminal.flush();
            
            terminal.close();
        }
    }
}
```

## Creating Interactive UI Elements

With mouse support, you can create interactive UI elements like buttons, menus, and selection lists:

```java title="MouseInteractiveUIExample.java" showLineNumbers
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.Display;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MouseInteractiveUIExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        try {
            // Enable mouse tracking
            terminal.puts(Capability.enter_mouse_mode);
            terminal.writer().write("\033[?1000;1002;1006;1015h");
            terminal.flush();
            
            // Create a display for managing the screen
            Display display = new Display(terminal, true);
            
            // Define some buttons
            List<Button> buttons = new ArrayList<>();
            buttons.add(new Button(5, 3, "Button 1", () -> {
                terminal.writer().println("Button 1 clicked!");
                terminal.flush();
            }));
            buttons.add(new Button(5, 5, "Button 2", () -> {
                terminal.writer().println("Button 2 clicked!");
                terminal.flush();
            }));
            buttons.add(new Button(5, 7, "Exit", () -> {
                // This will be used to exit the application
            }));
            
            // Initial render
            display.clear();
            terminal.writer().println("Interactive UI Example");
            terminal.writer().println("Click on the buttons below:");
            terminal.writer().println();
            
            for (Button button : buttons) {
                button.render(terminal);
            }
            
            terminal.flush();
            
            // Event loop
            boolean running = true;
            StringBuilder buffer = new StringBuilder();
            boolean esc = false;
            boolean bracket = false;
            boolean mouse = false;
            
            while (running) {
                int c = terminal.reader().read();
                
                // Parse escape sequences for mouse events
                if (c == '\033') {
                    esc = true;
                    buffer.setLength(0);
                } else if (esc && c == '[') {
                    bracket = true;
                } else if (esc && bracket && c == 'M') {
                    mouse = true;
                    buffer.setLength(0);
                } else if (mouse && buffer.length() < 3) {
                    buffer.append((char) c);
                    
                    if (buffer.length() == 3) {
                        int b = buffer.charAt(0) - 32;
                        int x = buffer.charAt(1) - 32;
                        int y = buffer.charAt(2) - 32;
                        
                        // Check if this is a mouse press event
                        if ((b & 3) != 3 && (b & 64) == 0) {
                            // Check if any button was clicked
                            for (Button button : buttons) {
                                if (button.isInside(x, y)) {
                                    button.click();
                                    
                                    // Check if Exit button was clicked
                                    if (button.getText().equals("Exit")) {
                                        running = false;
                                    }
                                    
                                    break;
                                }
                            }
                        }
                        
                        // Reset state
                        esc = false;
                        bracket = false;
                        mouse = false;
                    }
                } else {
                    // Not a mouse event or incomplete sequence
                    esc = false;
                    bracket = false;
                    mouse = false;
                }
            }
        } finally {
            // Disable mouse tracking before exiting
            terminal.puts(Capability.exit_mouse_mode);
            terminal.writer().write("\033[?1000;1002;1003;1006;1015l");
            terminal.flush();
            
            terminal.close();
        }
    }
    
    // Simple button class for demonstration
    static class Button {
        private final int x;
        private final int y;
        private final String text;
        private final Runnable action;
        
        public Button(int x, int y, String text, Runnable action) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.action = action;
        }
        
        public void render(Terminal terminal) {
            // Position cursor
            terminal.writer().write("\033[" + y + ";" + x + "H");
            
            // Draw button
            AttributedString buttonText = new AttributedString(
                "[ " + text + " ]",
                AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE).bold()
            );
            buttonText.print(terminal.writer());
        }
        
        public boolean isInside(int mouseX, int mouseY) {
            return mouseY == y && mouseX >= x && mouseX < x + text.length() + 4;
        }
        
        public void click() {
            action.run();
        }
        
        public String getText() {
            return text;
        }
    }
}
```

## Mouse Support in LineReader

JLine's `LineReader` can also be configured to handle mouse events:

```java title="LineReaderMouseExample.java" showLineNumbers
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

import java.io.IOException;

public class LineReaderMouseExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        try {
            // Enable mouse support
            terminal.puts(Capability.enter_mouse_mode);
            terminal.flush();
            
            // highlight-start
            // Create a LineReader with mouse support
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .variable(LineReader.MOUSE, true)  // Enable mouse support
                    .build();
            // highlight-end
            
            System.out.println("Mouse support enabled in LineReader.");
            System.out.println("Try clicking in the input line or using the mouse wheel.");
            System.out.println("Type 'exit' to quit.");
            
            // Read lines with mouse support
            String line;
            while (!(line = reader.readLine("prompt> ")).equalsIgnoreCase("exit")) {
                System.out.println("You entered: " + line);
            }
        } finally {
            // Disable mouse tracking before exiting
            terminal.puts(Capability.exit_mouse_mode);
            terminal.flush();
            
            terminal.close();
        }
    }
}
```

## Terminal Compatibility

Mouse support depends on the capabilities of the terminal emulator being used. Not all terminals support mouse events, and those that do may support different features or use different escape sequences.

Here's a compatibility table for common terminal emulators:

| Terminal Emulator | Basic Clicks | Mouse Movement | Mouse Wheel | Extended Reporting |
|-------------------|--------------|----------------|-------------|-------------------|
| xterm             | Yes          | Yes            | Yes         | Yes               |
| iTerm2            | Yes          | Yes            | Yes         | Yes               |
| GNOME Terminal    | Yes          | Yes            | Yes         | Yes               |
| Konsole           | Yes          | Yes            | Yes         | Yes               |
| PuTTY             | Yes          | Yes            | Yes         | Partial           |
| Windows Terminal  | Yes          | Yes            | Yes         | Yes               |
| CMD.exe           | No           | No             | No          | No                |
| MinTTY (Git Bash) | Yes          | Yes            | Yes         | Yes               |

To check if a terminal supports mouse events:

```java
boolean supportsMouseTracking = terminal.getStringCapability(Capability.enter_mouse_mode) != null;
```

## Best Practices

When implementing mouse support in your JLine applications:

1. **Always disable mouse tracking before exiting**: Failing to do so can leave the terminal in an inconsistent state.

2. **Provide keyboard alternatives**: Not all users can or want to use a mouse, so always provide keyboard shortcuts for mouse actions.

3. **Check for terminal capabilities**: Before enabling mouse support, check if the terminal supports it.

4. **Handle terminal resizing**: Mouse coordinates can change when the terminal is resized, so be prepared to update your UI accordingly.

5. **Use clear visual indicators**: Make it obvious which elements are clickable and provide visual feedback on mouse hover and click.

6. **Test in different terminals**: Mouse support varies across terminal emulators, so test your application in multiple environments.

7. **Consider accessibility**: Some users rely on screen readers or other assistive technologies that may not work well with mouse-based interfaces.

## Conclusion

Mouse support in JLine allows you to create more interactive and user-friendly terminal applications. By capturing and processing mouse events, you can implement clickable buttons, menus, selection lists, and other UI elements that would be cumbersome to navigate using only the keyboard.

Remember that mouse support is an enhancement, not a replacement for keyboard navigation. Always ensure your application is fully usable without a mouse for maximum accessibility and compatibility.
