---
sidebar_position: 5
---

# Key Bindings and Widgets

JLine provides a powerful system for customizing key bindings and creating widgets. This allows you to tailor the behavior of your command-line interface to meet your specific needs.

## Understanding Key Bindings

Key bindings map keyboard input to specific actions or functions (called widgets). JLine's key binding system is inspired by GNU Readline and provides similar functionality.

```java title="KeyBindingBasicsExample.java" showLineNumbers
import org.jline.keymap.Binding;
import org.jline.keymap.KeyMap;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Reference;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class KeyBindingBasicsExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        
        // highlight-start
        // Get the main key map
        KeyMap<Binding> keyMap = reader.getKeyMaps().get(LineReader.MAIN);
        
        // Display some default key bindings
        terminal.writer().println("Default key bindings:");
        terminal.writer().println("  Ctrl+A: " + keyMap.getBound(KeyMap.ctrl('A')));
        terminal.writer().println("  Ctrl+E: " + keyMap.getBound(KeyMap.ctrl('E')));
        terminal.writer().println("  Ctrl+L: " + keyMap.getBound(KeyMap.ctrl('L')));
        terminal.writer().println("  Ctrl+R: " + keyMap.getBound(KeyMap.ctrl('R')));
        terminal.writer().println("  Ctrl+U: " + keyMap.getBound(KeyMap.ctrl('U')));
        terminal.writer().flush();
        // highlight-end
        
        // Read a line to demonstrate the key bindings
        terminal.writer().println("\nType some text (try using the key bindings above):");
        String line = reader.readLine("prompt> ");
        terminal.writer().println("You entered: " + line);
        
        terminal.close();
    }
}
```

## Key Maps

JLine organizes key bindings into key maps, which are collections of bindings for different modes:

```java title="KeyMapsExample.java"
import org.jline.keymap.Binding;
import org.jline.keymap.KeyMap;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.Map;

public class KeyMapsExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        
        // highlight-start
        // Get all key maps
        Map<String, KeyMap<Binding>> keyMaps = reader.getKeyMaps();
        
        // Display available key maps
        terminal.writer().println("Available key maps:");
        for (String name : keyMaps.keySet()) {
            terminal.writer().println("  " + name);
        }
        terminal.writer().flush();
        // highlight-end
        
        // Explain key maps
        terminal.writer().println("\nKey map descriptions:");
        terminal.writer().println("  " + LineReader.MAIN + ": Main key map for normal input mode");
        terminal.writer().println("  " + LineReader.VIINS + ": Vi input mode");
        terminal.writer().println("  " + LineReader.VICMD + ": Vi command mode");
        terminal.writer().println("  " + LineReader.EMACS + ": Emacs mode");
        terminal.writer().println("  " + LineReader.SEARCH + ": Search mode (Ctrl+R)");
        terminal.writer().println("  " + LineReader.MENU + ": Menu selection mode");
        terminal.writer().flush();
        
        terminal.close();
    }
}
```

## Creating Custom Key Bindings

You can create custom key bindings to add new functionality or modify existing behavior:

```java title="CustomKeyBindingsExample.java" showLineNumbers
import org.jline.keymap.Binding;
import org.jline.keymap.KeyMap;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Reference;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CustomKeyBindingsExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        
        // Get the main key map
        KeyMap<Binding> keyMap = reader.getKeyMaps().get(LineReader.MAIN);
        
        // highlight-start
        // Bind Ctrl+T to insert the current time
        keyMap.bind(() -> {
            // Get the current time
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            
            // Insert the time at the current cursor position
            reader.getBuffer().write(time);
            return true;
        }, KeyMap.ctrl('T'));
        
        // Bind Alt+C to clear the screen
        keyMap.bind(() -> {
            terminal.puts(Capability.clear_screen);
            reader.callWidget(LineReader.REDRAW_LINE);
            reader.callWidget(LineReader.REDISPLAY);
            return true;
        }, KeyMap.alt('C'));
        
        // Bind Alt+U to convert the current word to uppercase
        keyMap.bind(() -> {
            // Get the current buffer
            String buffer = reader.getBuffer().toString();
            int cursor = reader.getBuffer().cursor();
            
            // Find the start and end of the current word
            int start = buffer.lastIndexOf(' ', cursor - 1) + 1;
            int end = buffer.indexOf(' ', cursor);
            if (end == -1) end = buffer.length();
            
            // Extract the current word
            String word = buffer.substring(start, end);
            
            // Replace with uppercase version
            reader.getBuffer().cursor(start);
            reader.getBuffer().delete(end - start);
            reader.getBuffer().write(word.toUpperCase());
            
            return true;
        }, KeyMap.alt('U'));
        // highlight-end
        
        // Display instructions
        terminal.writer().println("Custom key bindings:");
        terminal.writer().println("  Ctrl+T: Insert current time");
        terminal.writer().println("  Alt+C: Clear screen");
        terminal.writer().println("  Alt+U: Convert current word to uppercase");
        terminal.writer().println("\nType some text and try the custom key bindings:");
        terminal.writer().flush();
        
        // Read lines until "exit" is entered
        String line;
        while (!(line = reader.readLine("prompt> ")).equalsIgnoreCase("exit")) {
            terminal.writer().println("You entered: " + line);
        }
        
        terminal.close();
    }
}
```

## Binding to Built-in Widgets

JLine provides many built-in widgets that you can bind to keys:

```java title="BuiltinWidgetsExample.java"
import org.jline.keymap.Binding;
import org.jline.keymap.KeyMap;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Reference;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class BuiltinWidgetsExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        
        // Get the main key map
        KeyMap<Binding> keyMap = reader.getKeyMaps().get(LineReader.MAIN);
        
        // highlight-start
        // Bind keys to built-in widgets
        keyMap.bind(new Reference(LineReader.CLEAR_SCREEN), KeyMap.ctrl('L'));     // Clear screen
        keyMap.bind(new Reference(LineReader.BACKWARD_KILL_WORD), KeyMap.alt('h')); // Delete word backward
        keyMap.bind(new Reference(LineReader.KILL_WORD), KeyMap.alt('d'));         // Delete word forward
        keyMap.bind(new Reference(LineReader.BEGINNING_OF_LINE), KeyMap.ctrl('A')); // Move to beginning of line
        keyMap.bind(new Reference(LineReader.END_OF_LINE), KeyMap.ctrl('E'));      // Move to end of line
        keyMap.bind(new Reference(LineReader.UP_HISTORY), KeyMap.ctrl('P'));       // Previous history entry
        keyMap.bind(new Reference(LineReader.DOWN_HISTORY), KeyMap.ctrl('N'));     // Next history entry
        keyMap.bind(new Reference(LineReader.BACKWARD_WORD), KeyMap.alt('b'));     // Move backward one word
        keyMap.bind(new Reference(LineReader.FORWARD_WORD), KeyMap.alt('f'));      // Move forward one word
        keyMap.bind(new Reference(LineReader.CAPITALIZE_WORD), KeyMap.alt('c'));   // Capitalize word
        keyMap.bind(new Reference(LineReader.TRANSPOSE_CHARS), KeyMap.ctrl('T'));  // Transpose characters
        // highlight-end
        
        // Display instructions
        terminal.writer().println("Built-in widgets bound to keys:");
        terminal.writer().println("  Ctrl+L: Clear screen");
        terminal.writer().println("  Alt+H: Delete word backward");
        terminal.writer().println("  Alt+D: Delete word forward");
        terminal.writer().println("  Ctrl+A: Move to beginning of line");
        terminal.writer().println("  Ctrl+E: Move to end of line");
        terminal.writer().println("  Ctrl+P: Previous history entry");
        terminal.writer().println("  Ctrl+N: Next history entry");
        terminal.writer().println("  Alt+B: Move backward one word");
        terminal.writer().println("  Alt+F: Move forward one word");
        terminal.writer().println("  Alt+C: Capitalize word");
        terminal.writer().println("  Ctrl+T: Transpose characters");
        terminal.writer().println("\nType some text and try the key bindings:");
        terminal.writer().flush();
        
        // Read lines until "exit" is entered
        String line;
        while (!(line = reader.readLine("prompt> ")).equalsIgnoreCase("exit")) {
            terminal.writer().println("You entered: " + line);
        }
        
        terminal.close();
    }
}
```

## Creating Custom Widgets

You can create custom widgets for more complex functionality:

```java title="CustomWidgetsExample.java" showLineNumbers
import org.jline.keymap.Binding;
import org.jline.keymap.KeyMap;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Widget;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

public class CustomWidgetsExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        
        // highlight-start
        // Create custom widgets
        
        // Widget to insert the current date
        Widget insertDateWidget = () -> {
            String date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            reader.getBuffer().write(date);
            return true;
        };
        
        // Widget to duplicate the current line
        Widget duplicateLineWidget = () -> {
            String currentLine = reader.getBuffer().toString();
            reader.getBuffer().write("\n" + currentLine);
            return true;
        };
        
        // Widget to reverse the current word
        Widget reverseWordWidget = () -> {
            // Get the current buffer
            String buffer = reader.getBuffer().toString();
            int cursor = reader.getBuffer().cursor();
            
            // Find the start and end of the current word
            int start = buffer.lastIndexOf(' ', cursor - 1) + 1;
            int end = buffer.indexOf(' ', cursor);
            if (end == -1) end = buffer.length();
            
            // Extract the current word
            String word = buffer.substring(start, end);
            
            // Replace with reversed version
            reader.getBuffer().cursor(start);
            reader.getBuffer().delete(end - start);
            reader.getBuffer().write(new StringBuilder(word).reverse().toString());
            
            return true;
        };
        
        // Register the widgets
        reader.getWidgets().put("insert-date", insertDateWidget);
        reader.getWidgets().put("duplicate-line", duplicateLineWidget);
        reader.getWidgets().put("reverse-word", reverseWordWidget);
        
        // Bind keys to the widgets
        KeyMap<Binding> keyMap = reader.getKeyMaps().get(LineReader.MAIN);
        keyMap.bind(insertDateWidget, KeyMap.alt('d'));
        keyMap.bind(duplicateLineWidget, KeyMap.alt('l'));
        keyMap.bind(reverseWordWidget, KeyMap.alt('r'));
        // highlight-end
        
        // Display instructions
        terminal.writer().println("Custom widgets:");
        terminal.writer().println("  Alt+D: Insert current date");
        terminal.writer().println("  Alt+L: Duplicate current line");
        terminal.writer().println("  Alt+R: Reverse current word");
        terminal.writer().println("\nType some text and try the custom widgets:");
        terminal.writer().flush();
        
        // Read lines until "exit" is entered
        String line;
        while (!(line = reader.readLine("prompt> ")).equalsIgnoreCase("exit")) {
            terminal.writer().println("You entered: " + line);
        }
        
        terminal.close();
    }
}
```

## Calling Widgets Programmatically

You can call widgets programmatically using the `callWidget` method:

```java title="CallWidgetExample.java"
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class CallWidgetExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        
        // Display instructions
        terminal.writer().println("This example demonstrates calling widgets programmatically.");
        terminal.writer().println("The line will be pre-filled and the cursor positioned.");
        terminal.writer().println("\nPress Enter to continue...");
        terminal.writer().flush();
        terminal.reader().readLine();
        
        // highlight-start
        // Set up a callback to be called before reading a line
        reader.setVariable(LineReader.BELL_STYLE, "none");
        reader.setVariable(LineReader.PRE_READ_LINE, (Function<LineReader, Boolean>) r -> {
            // Pre-fill the line buffer
            r.getBuffer().write("This is a pre-filled line");
            
            // Move cursor to the beginning of the line
            r.callWidget(LineReader.BEGINNING_OF_LINE);
            
            // Move forward by 5 characters
            for (int i = 0; i < 5; i++) {
                r.callWidget(LineReader.FORWARD_CHAR);
            }
            
            return true;
        });
        // highlight-end
        
        // Read a line
        String line = reader.readLine("prompt> ");
        terminal.writer().println("You entered: " + line);
        
        terminal.close();
    }
}
```

## Emacs vs. Vi Mode

JLine supports both Emacs and Vi editing modes:

```java title="EditingModesExample.java"
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class EditingModesExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // highlight-start
        // Create a line reader with Emacs editing mode (default)
        LineReader emacsReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .variable(LineReader.EDITING_MODE, LineReader.EMACS)
                .build();
        
        // Create a line reader with Vi editing mode
        LineReader viReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .variable(LineReader.EDITING_MODE, LineReader.VI)
                .build();
        // highlight-end
        
        // Demonstrate Emacs mode
        terminal.writer().println("Emacs editing mode:");
        terminal.writer().println("  Ctrl+A: Beginning of line");
        terminal.writer().println("  Ctrl+E: End of line");
        terminal.writer().println("  Ctrl+F: Forward character");
        terminal.writer().println("  Ctrl+B: Backward character");
        terminal.writer().println("  Alt+F: Forward word");
        terminal.writer().println("  Alt+B: Backward word");
        terminal.writer().println("  Ctrl+K: Kill to end of line");
        terminal.writer().println("  Ctrl+Y: Yank (paste)");
        terminal.writer().println("\nType some text in Emacs mode:");
        terminal.writer().flush();
        
        String line = emacsReader.readLine("emacs> ");
        terminal.writer().println("You entered: " + line);
        
        // Demonstrate Vi mode
        terminal.writer().println("\nVi editing mode:");
        terminal.writer().println("  ESC: Enter command mode");
        terminal.writer().println("  i: Enter insert mode");
        terminal.writer().println("  h, j, k, l: Move cursor");
        terminal.writer().println("  w: Forward word");
        terminal.writer().println("  b: Backward word");
        terminal.writer().println("  d: Delete");
        terminal.writer().println("  y: Yank (copy)");
        terminal.writer().println("  p: Put (paste)");
        terminal.writer().println("\nType some text in Vi mode:");
        terminal.writer().flush();
        
        line = viReader.readLine("vi> ");
        terminal.writer().println("You entered: " + line);
        
        terminal.close();
    }
}
```

## Key Binding Best Practices

When working with key bindings and widgets, consider these best practices:

1. **Respect Standard Bindings**: Try to respect standard key bindings that users are familiar with.

2. **Document Custom Bindings**: Clearly document any custom key bindings you add.

3. **Avoid Conflicts**: Be careful not to override important key bindings unless you have a good reason.

4. **Consider Different Terminals**: Some key combinations may not work in all terminals.

5. **Group Related Bindings**: Use a consistent prefix for related bindings (e.g., Alt+1, Alt+2, etc.).

6. **Test Thoroughly**: Test your key bindings in different environments and terminal emulators.

7. **Provide Feedback**: Give users feedback when they use key bindings, especially for complex operations.

8. **Make Bindings Discoverable**: Provide a way for users to discover available key bindings.

9. **Consider Accessibility**: Ensure your key bindings are accessible to users with disabilities.

10. **Allow Customization**: If possible, allow users to customize key bindings to their preferences.
