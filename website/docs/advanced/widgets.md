---
sidebar_position: 7
---

# Custom Widgets

JLine's widget system allows you to extend the functionality of the line reader with custom actions and behaviors. Widgets are reusable components that can be bound to key combinations or called programmatically, making them a powerful tool for customizing the command-line experience.

## Widget Basics

A widget in JLine is a function that performs an action on the line buffer, such as inserting text, moving the cursor, or manipulating the content. Widgets are implemented as instances of the `Widget` interface, which is a functional interface with a single method:

```java
@FunctionalInterface
public interface Widget {
    boolean apply();
}
```

The `apply()` method should return `true` if the widget was successfully applied, or `false` otherwise.

## Built-in Widgets

JLine comes with many built-in widgets that provide common line editing functionality. These widgets are identified by string constants in the `LineReader` class:

```java title="BuiltinWidgetsExample.java" showLineNumbers
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.Map;

public class BuiltinWidgetsExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        
        // highlight-start
        // Get all registered widgets
        Map<String, Widget> widgets = reader.getWidgets();
        
        // Print some common built-in widgets
        terminal.writer().println("Common built-in widgets:");
        terminal.writer().println("  " + LineReader.ACCEPT_LINE + ": Accept the current line");
        terminal.writer().println("  " + LineReader.BACKWARD_CHAR + ": Move cursor backward one character");
        terminal.writer().println("  " + LineReader.FORWARD_CHAR + ": Move cursor forward one character");
        terminal.writer().println("  " + LineReader.BACKWARD_WORD + ": Move cursor backward one word");
        terminal.writer().println("  " + LineReader.FORWARD_WORD + ": Move cursor forward one word");
        terminal.writer().println("  " + LineReader.BEGINNING_OF_LINE + ": Move cursor to beginning of line");
        terminal.writer().println("  " + LineReader.END_OF_LINE + ": Move cursor to end of line");
        terminal.writer().println("  " + LineReader.KILL_LINE + ": Kill text from cursor to end of line");
        terminal.writer().println("  " + LineReader.BACKWARD_KILL_WORD + ": Kill text from cursor to start of word");
        terminal.writer().println("  " + LineReader.CLEAR_SCREEN + ": Clear the screen");
        terminal.writer().println("  " + LineReader.HISTORY_SEARCH_BACKWARD + ": Search history backward");
        terminal.writer().println("  " + LineReader.HISTORY_SEARCH_FORWARD + ": Search history forward");
        terminal.writer().println("  " + LineReader.COMPLETE_WORD + ": Complete the current word");
        terminal.writer().println("  " + LineReader.YANK + ": Yank (paste) previously killed text");
        terminal.writer().flush();
        // highlight-end
        
        // Read a line to demonstrate widgets
        terminal.writer().println("\nType some text (try using key bindings to activate widgets):");
        String line = reader.readLine("prompt> ");
        terminal.writer().println("You entered: " + line);
        
        terminal.close();
    }
    
    // Widget interface for reference
    @FunctionalInterface
    public interface Widget {
        boolean apply();
    }
}
```

## Creating Custom Widgets

You can create custom widgets to add new functionality to the line reader:

```java title="CustomWidgetExample.java" showLineNumbers
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Widget;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CustomWidgetExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        
        // highlight-start
        // Create a custom widget to insert the current timestamp
        Widget insertTimestampWidget = () -> {
            // Get the current timestamp
            String timestamp = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            // Insert the timestamp at the current cursor position
            reader.getBuffer().write(timestamp);
            
            // Return true to indicate success
            return true;
        };
        
        // Register the widget with the line reader
        reader.getWidgets().put("insert-timestamp", insertTimestampWidget);
        
        // Bind the widget to a key combination (Alt+T)
        reader.getKeyMaps().get(LineReader.MAIN).bind(
                insertTimestampWidget, 
                "\033t");  // Escape followed by 't' represents Alt+T
        // highlight-end
        
        // Display instructions
        terminal.writer().println("Custom widget example:");
        terminal.writer().println("  Press Alt+T to insert the current timestamp");
        terminal.writer().println("\nType some text and try the custom widget:");
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

## Widget Categories

Widgets can be categorized based on their functionality:

### Text Insertion Widgets

These widgets insert text at the current cursor position:

```java title="TextInsertionWidgetExample.java" showLineNumbers
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Widget;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TextInsertionWidgetExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        
        // highlight-start
        // Create a map of text snippets
        Map<String, String> snippets = new HashMap<>();
        snippets.put("greeting", "Hello, world!");
        snippets.put("signature", "Best regards,\nYour Name");
        snippets.put("date", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        
        // Create widgets for each snippet
        for (Map.Entry<String, String> entry : snippets.entrySet()) {
            String name = entry.getKey();
            String text = entry.getValue();
            
            // Create a widget that inserts the snippet
            Widget widget = () -> {
                reader.getBuffer().write(text);
                return true;
            };
            
            // Register the widget
            reader.getWidgets().put("insert-" + name, widget);
        }
        
        // Bind widgets to key combinations
        reader.getKeyMaps().get(LineReader.MAIN).bind(
                reader.getWidgets().get("insert-greeting"), 
                "\033g");  // Alt+G
        
        reader.getKeyMaps().get(LineReader.MAIN).bind(
                reader.getWidgets().get("insert-signature"), 
                "\033s");  // Alt+S
        
        reader.getKeyMaps().get(LineReader.MAIN).bind(
                reader.getWidgets().get("insert-date"), 
                "\033d");  // Alt+D
        // highlight-end
        
        // Display instructions
        terminal.writer().println("Text insertion widgets:");
        terminal.writer().println("  Alt+G: Insert greeting");
        terminal.writer().println("  Alt+S: Insert signature");
        terminal.writer().println("  Alt+D: Insert date");
        terminal.writer().println("\nType some text and try the widgets:");
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

### Cursor Movement Widgets

These widgets move the cursor within the line buffer:

```java title="CursorMovementWidgetExample.java" showLineNumbers
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Widget;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class CursorMovementWidgetExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        
        // highlight-start
        // Create a widget to move cursor to the middle of the line
        Widget moveToMiddleWidget = () -> {
            int length = reader.getBuffer().length();
            reader.getBuffer().cursor(length / 2);
            return true;
        };
        
        // Create a widget to move cursor to a specific column
        Widget moveToColumn10Widget = () -> {
            int column = 10;
            if (reader.getBuffer().length() >= column) {
                reader.getBuffer().cursor(column);
                return true;
            }
            return false;
        };
        
        // Create a widget to move cursor forward by 5 characters
        Widget moveForward5Widget = () -> {
            int cursor = reader.getBuffer().cursor();
            int newCursor = Math.min(cursor + 5, reader.getBuffer().length());
            reader.getBuffer().cursor(newCursor);
            return true;
        };
        
        // Register the widgets
        reader.getWidgets().put("move-to-middle", moveToMiddleWidget);
        reader.getWidgets().put("move-to-column-10", moveToColumn10Widget);
        reader.getWidgets().put("move-forward-5", moveForward5Widget);
        
        // Bind widgets to key combinations
        reader.getKeyMaps().get(LineReader.MAIN).bind(
                moveToMiddleWidget, 
                "\033m");  // Alt+M
        
        reader.getKeyMaps().get(LineReader.MAIN).bind(
                moveToColumn10Widget, 
                "\0330");  // Alt+0
        
        reader.getKeyMaps().get(LineReader.MAIN).bind(
                moveForward5Widget, 
                "\033f");  // Alt+F
        // highlight-end
        
        // Display instructions
        terminal.writer().println("Cursor movement widgets:");
        terminal.writer().println("  Alt+M: Move cursor to middle of line");
        terminal.writer().println("  Alt+0: Move cursor to column 10");
        terminal.writer().println("  Alt+F: Move cursor forward 5 characters");
        terminal.writer().println("\nType some text and try the widgets:");
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

### Text Manipulation Widgets

These widgets modify the text in the line buffer:

```java title="TextManipulationWidgetExample.java" showLineNumbers
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Widget;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class TextManipulationWidgetExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        
        // highlight-start
        // Create a widget to convert the current word to uppercase
        Widget uppercaseWordWidget = () -> {
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
        };
        
        // Create a widget to reverse the entire line
        Widget reverseLineWidget = () -> {
            String line = reader.getBuffer().toString();
            String reversed = new StringBuilder(line).reverse().toString();
            
            reader.getBuffer().clear();
            reader.getBuffer().write(reversed);
            
            return true;
        };
        
        // Create a widget to remove duplicate spaces
        Widget removeDuplicateSpacesWidget = () -> {
            String line = reader.getBuffer().toString();
            String cleaned = line.replaceAll("\\s+", " ");
            
            reader.getBuffer().clear();
            reader.getBuffer().write(cleaned);
            
            return true;
        };
        
        // Register the widgets
        reader.getWidgets().put("uppercase-word", uppercaseWordWidget);
        reader.getWidgets().put("reverse-line", reverseLineWidget);
        reader.getWidgets().put("remove-duplicate-spaces", removeDuplicateSpacesWidget);
        
        // Bind widgets to key combinations
        reader.getKeyMaps().get(LineReader.MAIN).bind(
                uppercaseWordWidget, 
                "\033u");  // Alt+U
        
        reader.getKeyMaps().get(LineReader.MAIN).bind(
                reverseLineWidget, 
                "\033r");  // Alt+R
        
        reader.getKeyMaps().get(LineReader.MAIN).bind(
                removeDuplicateSpacesWidget, 
                "\033s");  // Alt+S
        // highlight-end
        
        // Display instructions
        terminal.writer().println("Text manipulation widgets:");
        terminal.writer().println("  Alt+U: Convert current word to uppercase");
        terminal.writer().println("  Alt+R: Reverse the entire line");
        terminal.writer().println("  Alt+S: Remove duplicate spaces");
        terminal.writer().println("\nType some text and try the widgets:");
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

### Command Execution Widgets

These widgets execute commands or perform actions beyond simple text manipulation:

```java title="CommandExecutionWidgetExample.java" showLineNumbers
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Widget;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class CommandExecutionWidgetExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        
        // highlight-start
        // Create a widget to list files in the current directory
        Widget listFilesWidget = () -> {
            try {
                terminal.writer().println();
                Files.list(Paths.get("."))
                        .forEach(path -> terminal.writer().println(path.getFileName()));
                terminal.writer().println();
                terminal.flush();
                
                // Redraw the prompt and current buffer
                reader.callWidget(LineReader.REDRAW_LINE);
                reader.callWidget(LineReader.REDISPLAY);
                
                return true;
            } catch (IOException e) {
                return false;
            }
        };
        
        // Create a widget to show system information
        Widget systemInfoWidget = () -> {
            terminal.writer().println();
            terminal.writer().println("System Information:");
            terminal.writer().println("  OS: " + System.getProperty("os.name"));
            terminal.writer().println("  Java: " + System.getProperty("java.version"));
            terminal.writer().println("  User: " + System.getProperty("user.name"));
            terminal.writer().println("  Terminal: " + terminal.getType());
            terminal.writer().println();
            terminal.flush();
            
            // Redraw the prompt and current buffer
            reader.callWidget(LineReader.REDRAW_LINE);
            reader.callWidget(LineReader.REDISPLAY);
            
            return true;
        };
        
        // Create a widget to show the last 5 history entries
        Widget showHistoryWidget = () -> {
            terminal.writer().println();
            terminal.writer().println("Last 5 history entries:");
            
            List<String> history = reader.getHistory().entries();
            int start = Math.max(0, history.size() - 5);
            for (int i = start; i < history.size(); i++) {
                terminal.writer().println("  " + (i + 1) + ": " + history.get(i));
            }
            
            terminal.writer().println();
            terminal.flush();
            
            // Redraw the prompt and current buffer
            reader.callWidget(LineReader.REDRAW_LINE);
            reader.callWidget(LineReader.REDISPLAY);
            
            return true;
        };
        
        // Register the widgets
        reader.getWidgets().put("list-files", listFilesWidget);
        reader.getWidgets().put("system-info", systemInfoWidget);
        reader.getWidgets().put("show-history", showHistoryWidget);
        
        // Bind widgets to key combinations
        reader.getKeyMaps().get(LineReader.MAIN).bind(
                listFilesWidget, 
                "\033l");  // Alt+L
        
        reader.getKeyMaps().get(LineReader.MAIN).bind(
                systemInfoWidget, 
                "\033i");  // Alt+I
        
        reader.getKeyMaps().get(LineReader.MAIN).bind(
                showHistoryWidget, 
                "\033h");  // Alt+H
        // highlight-end
        
        // Display instructions
        terminal.writer().println("Command execution widgets:");
        terminal.writer().println("  Alt+L: List files in current directory");
        terminal.writer().println("  Alt+I: Show system information");
        terminal.writer().println("  Alt+H: Show last 5 history entries");
        terminal.writer().println("\nType some text and try the widgets:");
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

```java title="CallWidgetProgrammaticallyExample.java" showLineNumbers
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class CallWidgetProgrammaticallyExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        
        // Display instructions
        terminal.writer().println("This example demonstrates calling widgets programmatically.");
        terminal.writer().println("The line will be pre-filled and manipulated using widgets.");
        terminal.writer().println("\nPress Enter to continue...");
        terminal.writer().flush();
        terminal.reader().readLine();
        
        // highlight-start
        // Set up a callback to be called before reading a line
        reader.setVariable(LineReader.PRE_READ_LINE, (Function<LineReader, Boolean>) r -> {
            // Pre-fill the line buffer
            r.getBuffer().write("This is a pre-filled line with some extra   spaces");
            
            // Call widgets programmatically
            r.callWidget(LineReader.BEGINNING_OF_LINE);  // Move to beginning of line
            r.callWidget(LineReader.FORWARD_WORD);       // Move forward one word
            r.callWidget(LineReader.FORWARD_WORD);       // Move forward another word
            r.callWidget(LineReader.UPCASE_WORD);        // Uppercase the current word
            
            // Call a custom widget to remove duplicate spaces
            r.callWidget("remove-duplicate-spaces");
            
            return true;
        });
        
        // Create and register a custom widget to remove duplicate spaces
        reader.getWidgets().put("remove-duplicate-spaces", () -> {
            String line = reader.getBuffer().toString();
            String cleaned = line.replaceAll("\\s+", " ");
            
            reader.getBuffer().clear();
            reader.getBuffer().write(cleaned);
            
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

## Widget Composition

You can compose widgets to create more complex functionality:

```java title="WidgetCompositionExample.java" showLineNumbers
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Widget;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.function.Function;

public class WidgetCompositionExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        
        // highlight-start
        // Create a utility method to compose widgets
        Function<Widget[], Widget> compose = widgets -> () -> {
            boolean result = true;
            for (Widget widget : widgets) {
                result = result && widget.apply();
            }
            return result;
        };
        
        // Create individual widgets
        Widget moveToBeginning = () -> {
            reader.callWidget(LineReader.BEGINNING_OF_LINE);
            return true;
        };
        
        Widget deleteToEnd = () -> {
            reader.callWidget(LineReader.KILL_LINE);
            return true;
        };
        
        Widget insertTimestamp = () -> {
            String timestamp = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            reader.getBuffer().write(timestamp);
            return true;
        };
        
        // Compose widgets to create a complex action
        Widget replaceLineWithTimestamp = compose.apply(new Widget[]{
            moveToBeginning,
            deleteToEnd,
            insertTimestamp
        });
        
        // Register the composed widget
        reader.getWidgets().put("replace-line-with-timestamp", replaceLineWithTimestamp);
        
        // Bind the widget to a key combination
        reader.getKeyMaps().get(LineReader.MAIN).bind(
                replaceLineWithTimestamp, 
                "\033t");  // Alt+T
        // highlight-end
        
        // Display instructions
        terminal.writer().println("Widget composition example:");
        terminal.writer().println("  Alt+T: Replace the entire line with the current timestamp");
        terminal.writer().println("\nType some text and try the composed widget:");
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

## Creating a Widget Library

For more complex applications, you might want to create a reusable widget library:

```java title="WidgetLibraryExample.java" showLineNumbers
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Widget;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class WidgetLibraryExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        
        // highlight-start
        // Create a widget library
        WidgetLibrary library = new WidgetLibrary(reader);
        
        // Register widgets from the library
        library.registerAll();
        
        // Bind some widgets to key combinations
        reader.getKeyMaps().get(LineReader.MAIN).bind(
                reader.getWidgets().get("insert-timestamp"), 
                "\033t");  // Alt+T
        
        reader.getKeyMaps().get(LineReader.MAIN).bind(
                reader.getWidgets().get("uppercase-word"), 
                "\033u");  // Alt+U
        
        reader.getKeyMaps().get(LineReader.MAIN).bind(
                reader.getWidgets().get("duplicate-line"), 
                "\033d");  // Alt+D
        // highlight-end
        
        // Display instructions
        terminal.writer().println("Widget library example:");
        terminal.writer().println("  Alt+T: Insert timestamp");
        terminal.writer().println("  Alt+U: Uppercase current word");
        terminal.writer().println("  Alt+D: Duplicate current line");
        terminal.writer().println("\nType some text and try the widgets:");
        terminal.writer().flush();
        
        // Read lines until "exit" is entered
        String line;
        while (!(line = reader.readLine("prompt> ")).equalsIgnoreCase("exit")) {
            terminal.writer().println("You entered: " + line);
        }
        
        terminal.close();
    }
    
    // highlight-start
    // Widget library class
    static class WidgetLibrary {
        private final LineReader reader;
        private final Map<String, Widget> widgets = new HashMap<>();
        
        public WidgetLibrary(LineReader reader) {
            this.reader = reader;
            initializeWidgets();
        }
        
        private void initializeWidgets() {
            // Text insertion widgets
            widgets.put("insert-timestamp", () -> {
                String timestamp = LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                reader.getBuffer().write(timestamp);
                return true;
            });
            
            // Text manipulation widgets
            widgets.put("uppercase-word", () -> {
                String buffer = reader.getBuffer().toString();
                int cursor = reader.getBuffer().cursor();
                
                int start = buffer.lastIndexOf(' ', cursor - 1) + 1;
                int end = buffer.indexOf(' ', cursor);
                if (end == -1) end = buffer.length();
                
                String word = buffer.substring(start, end);
                
                reader.getBuffer().cursor(start);
                reader.getBuffer().delete(end - start);
                reader.getBuffer().write(word.toUpperCase());
                
                return true;
            });
            
            widgets.put("duplicate-line", () -> {
                String line = reader.getBuffer().toString();
                reader.getBuffer().write("\n" + line);
                return true;
            });
            
            // Add more widgets as needed...
        }
        
        public void registerAll() {
            // Register all widgets with the line reader
            for (Map.Entry<String, Widget> entry : widgets.entrySet()) {
                reader.getWidgets().put(entry.getKey(), entry.getValue());
            }
        }
        
        public Widget getWidget(String name) {
            return widgets.get(name);
        }
    }
    // highlight-end
}
```

## Best Practices for Widget Development

When creating custom widgets, follow these best practices:

1. **Keep widgets focused**: Each widget should do one thing well.

2. **Handle edge cases**: Consider what happens when the buffer is empty, the cursor is at the beginning or end, etc.

3. **Return appropriate values**: Return `true` if the widget was successfully applied, or `false` otherwise.

4. **Document your widgets**: Provide clear documentation for what each widget does and how to use it.

5. **Use meaningful names**: Choose descriptive names for your widgets that indicate their purpose.

6. **Compose widgets**: Use composition to create complex behaviors from simple widgets.

7. **Test thoroughly**: Test your widgets with various input scenarios to ensure they work correctly.

8. **Consider performance**: For widgets that might be called frequently, optimize for performance.

9. **Provide visual feedback**: For complex operations, consider providing visual feedback to the user.

10. **Clean up after yourself**: If your widget makes changes to the terminal state, make sure to restore it properly.

## Conclusion

JLine's widget system provides a powerful way to extend and customize the line reader functionality. By creating custom widgets, you can add new features, modify existing behaviors, and create a more tailored command-line experience for your users.

Whether you're adding simple text insertion capabilities or complex command execution functionality, widgets offer a clean, modular approach to extending JLine's capabilities.
