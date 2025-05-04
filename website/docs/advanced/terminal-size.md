---
sidebar_position: 7
---

# Terminal Size Changes

Terminal applications need to adapt to changes in terminal size to provide a good user experience. JLine provides mechanisms to detect and handle terminal size changes, allowing your application to respond appropriately when the user resizes their terminal window.

## Detecting Terminal Size

JLine makes it easy to get the current terminal size:

```java title="TerminalSizeExample.java" showLineNumbers
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class TerminalSizeExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        try {
            // highlight-start
            // Get the current terminal size
            Size size = terminal.getSize();
            int columns = size.getColumns();
            int rows = size.getRows();
            // highlight-end
            
            terminal.writer().println("Terminal size information:");
            terminal.writer().println("  Columns: " + columns);
            terminal.writer().println("  Rows: " + rows);
            terminal.writer().println();
            
            // Display a visual representation of the terminal size
            terminal.writer().println("Visual representation of terminal size:");
            
            // Top border
            terminal.writer().print("+");
            for (int i = 0; i < columns - 2; i++) {
                terminal.writer().print("-");
            }
            terminal.writer().println("+");
            
            // Middle section
            for (int i = 0; i < rows - 3; i++) {
                terminal.writer().print("|");
                for (int j = 0; j < columns - 2; j++) {
                    terminal.writer().print(" ");
                }
                terminal.writer().println("|");
            }
            
            // Bottom border
            terminal.writer().print("+");
            for (int i = 0; i < columns - 2; i++) {
                terminal.writer().print("-");
            }
            terminal.writer().println("+");
            
            terminal.writer().println();
            terminal.writer().println("Resize your terminal window and press Enter to see the new size...");
            terminal.writer().flush();
            
            terminal.reader().readLine();
            
            // Get the updated terminal size
            size = terminal.getSize();
            terminal.writer().println("New terminal size:");
            terminal.writer().println("  Columns: " + size.getColumns());
            terminal.writer().println("  Rows: " + size.getRows());
            terminal.writer().flush();
        } finally {
            terminal.close();
        }
    }
}
```

## Handling Size Change Events

JLine can notify your application when the terminal size changes:

```java title="SizeChangeHandlerExample.java" showLineNumbers
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.Terminal.SignalHandler;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class SizeChangeHandlerExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        try {
            // Clear screen
            terminal.puts(Capability.clear_screen);
            
            // Display instructions
            terminal.writer().println("Terminal Size Change Handler Example");
            terminal.writer().println("Resize your terminal window to see the size change events.");
            terminal.writer().println("Press Ctrl+C to exit.");
            terminal.writer().println();
            terminal.writer().flush();
            
            // Get initial size
            Size initialSize = terminal.getSize();
            terminal.writer().println("Initial size: " + initialSize.getColumns() + "x" + initialSize.getRows());
            terminal.writer().flush();
            
            // Flag to control the main loop
            AtomicBoolean running = new AtomicBoolean(true);
            
            // highlight-start
            // Register a handler for window resize signals
            terminal.handle(Signal.WINCH, new SignalHandler() {
                @Override
                public void handle(Signal signal) {
                    // Get the new terminal size
                    Size newSize = terminal.getSize();
                    
                    // Display the new size
                    terminal.writer().println("\nTerminal resized: " + newSize.getColumns() + "x" + newSize.getRows());
                    
                    // Draw a box to visualize the new size
                    drawBox(terminal, newSize);
                    
                    terminal.writer().flush();
                }
            });
            // highlight-end
            
            // Register a handler for interrupt signals (Ctrl+C)
            terminal.handle(Signal.INT, signal -> running.set(false));
            
            // Main loop
            while (running.get()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally {
            terminal.close();
        }
    }
    
    // Helper method to draw a box
    private static void drawBox(Terminal terminal, Size size) {
        int columns = size.getColumns();
        int rows = size.getRows();
        
        // Limit the box size to avoid filling the entire terminal
        int boxWidth = Math.min(columns - 2, 40);
        int boxHeight = Math.min(rows - 10, 10);
        
        // Draw the box
        terminal.writer().println();
        
        // Top border
        terminal.writer().print("+");
        for (int i = 0; i < boxWidth - 2; i++) {
            terminal.writer().print("-");
        }
        terminal.writer().println("+");
        
        // Middle section
        for (int i = 0; i < boxHeight - 2; i++) {
            terminal.writer().print("|");
            for (int j = 0; j < boxWidth - 2; j++) {
                terminal.writer().print(" ");
            }
            terminal.writer().println("|");
        }
        
        // Bottom border
        terminal.writer().print("+");
        for (int i = 0; i < boxWidth - 2; i++) {
            terminal.writer().print("-");
        }
        terminal.writer().println("+");
    }
}
```

## Adapting UI to Terminal Size

You can adapt your UI to the terminal size to provide a better user experience:

```java title="AdaptiveUIExample.java" showLineNumbers
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp.Capability;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdaptiveUIExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        try {
            // Clear screen
            terminal.puts(Capability.clear_screen);
            
            // Display instructions
            terminal.writer().println("Adaptive UI Example");
            terminal.writer().println("Resize your terminal window to see the UI adapt.");
            terminal.writer().println("Press Ctrl+C to exit.");
            terminal.writer().println();
            terminal.writer().flush();
            
            // Sample data for our UI
            List<String[]> data = new ArrayList<>();
            data.add(new String[]{"ID", "Name", "Email", "Role"});
            data.add(new String[]{"1", "John Doe", "john@example.com", "Admin"});
            data.add(new String[]{"2", "Jane Smith", "jane@example.com", "User"});
            data.add(new String[]{"3", "Bob Johnson", "bob@example.com", "Editor"});
            data.add(new String[]{"4", "Alice Brown", "alice@example.com", "User"});
            data.add(new String[]{"5", "Charlie Davis", "charlie@example.com", "Viewer"});
            
            // Flag to control the main loop
            AtomicBoolean running = new AtomicBoolean(true);
            
            // highlight-start
            // Initial render
            renderUI(terminal, data);
            
            // Register a handler for window resize signals
            terminal.handle(Signal.WINCH, signal -> {
                // Clear screen
                terminal.puts(Capability.clear_screen);
                
                // Re-render the UI with the new terminal size
                renderUI(terminal, data);
            });
            // highlight-end
            
            // Register a handler for interrupt signals (Ctrl+C)
            terminal.handle(Signal.INT, signal -> running.set(false));
            
            // Main loop
            while (running.get()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally {
            terminal.close();
        }
    }
    
    // Helper method to render the UI
    private static void renderUI(Terminal terminal, List<String[]> data) {
        Size size = terminal.getSize();
        int columns = size.getColumns();
        int rows = size.getRows();
        
        terminal.writer().println("Terminal size: " + columns + "x" + rows);
        terminal.writer().println();
        
        // Determine the layout based on terminal size
        if (columns < 50) {
            // Narrow terminal - use compact layout
            renderCompactLayout(terminal, data);
        } else {
            // Wide terminal - use table layout
            renderTableLayout(terminal, data, columns);
        }
        
        terminal.writer().flush();
    }
    
    // Render a compact layout for narrow terminals
    private static void renderCompactLayout(Terminal terminal, List<String[]> data) {
        terminal.writer().println("Using compact layout for narrow terminal");
        terminal.writer().println();
        
        // Header
        AttributedStringBuilder header = new AttributedStringBuilder()
                .style(AttributedStyle.DEFAULT.bold())
                .append("User List")
                .style(AttributedStyle.DEFAULT);
        header.toAttributedString().println(terminal);
        terminal.writer().println();
        
        // Data
        for (int i = 1; i < data.size(); i++) {
            String[] row = data[i];
            terminal.writer().println("User #" + row[0] + ":");
            terminal.writer().println("  Name: " + row[1]);
            terminal.writer().println("  Email: " + row[2]);
            terminal.writer().println("  Role: " + row[3]);
            terminal.writer().println();
        }
    }
    
    // Render a table layout for wide terminals
    private static void renderTableLayout(Terminal terminal, List<String[]> data, int terminalWidth) {
        terminal.writer().println("Using table layout for wide terminal");
        terminal.writer().println();
        
        // Calculate column widths
        int[] columnWidths = new int[data.get(0).length];
        for (String[] row : data) {
            for (int i = 0; i < row.length; i++) {
                columnWidths[i] = Math.max(columnWidths[i], row[i].length());
            }
        }
        
        // Add padding
        for (int i = 0; i < columnWidths.length; i++) {
            columnWidths[i] += 2;
        }
        
        // Check if table fits in terminal
        int totalWidth = 0;
        for (int width : columnWidths) {
            totalWidth += width;
        }
        
        // Add separators
        totalWidth += columnWidths.length - 1;
        
        if (totalWidth > terminalWidth) {
            // Table doesn't fit, adjust column widths
            int excess = totalWidth - terminalWidth;
            int columnsToAdjust = columnWidths.length;
            int adjustmentPerColumn = excess / columnsToAdjust;
            
            for (int i = 0; i < columnWidths.length; i++) {
                if (columnWidths[i] > adjustmentPerColumn + 5) {
                    columnWidths[i] -= adjustmentPerColumn;
                    excess -= adjustmentPerColumn;
                }
            }
            
            // If there's still excess, take from the widest column
            if (excess > 0) {
                int widestColumn = 0;
                for (int i = 1; i < columnWidths.length; i++) {
                    if (columnWidths[i] > columnWidths[widestColumn]) {
                        widestColumn = i;
                    }
                }
                columnWidths[widestColumn] -= excess;
            }
        }
        
        // Render header
        String[] header = data.get(0);
        for (int i = 0; i < header.length; i++) {
            AttributedStringBuilder asb = new AttributedStringBuilder()
                    .style(AttributedStyle.DEFAULT.bold())
                    .append(padOrTruncate(header[i], columnWidths[i]));
            asb.toAttributedString().print(terminal);
            
            if (i < header.length - 1) {
                terminal.writer().print("|");
            }
        }
        terminal.writer().println();
        
        // Render separator
        for (int i = 0; i < header.length; i++) {
            for (int j = 0; j < columnWidths[i]; j++) {
                terminal.writer().print("-");
            }
            
            if (i < header.length - 1) {
                terminal.writer().print("+");
            }
        }
        terminal.writer().println();
        
        // Render data
        for (int rowIndex = 1; rowIndex < data.size(); rowIndex++) {
            String[] row = data.get(rowIndex);
            for (int i = 0; i < row.length; i++) {
                terminal.writer().print(padOrTruncate(row[i], columnWidths[i]));
                
                if (i < row.length - 1) {
                    terminal.writer().print("|");
                }
            }
            terminal.writer().println();
        }
    }
    
    // Helper method to pad or truncate a string to a specific width
    private static String padOrTruncate(String str, int width) {
        if (str.length() > width - 1) {
            return str.substring(0, width - 3) + ".. ";
        } else {
            StringBuilder sb = new StringBuilder(str);
            while (sb.length() < width) {
                sb.append(" ");
            }
            return sb.toString();
        }
    }
}
```

## Handling Size Changes with LineReader

When using `LineReader`, you need to handle size changes to ensure proper line editing:

```java title="LineReaderSizeChangeExample.java"
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

import java.io.IOException;

public class LineReaderSizeChangeExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        try {
            // Clear screen
            terminal.puts(Capability.clear_screen);
            
            // Create a line reader
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();
            
            // Display instructions
            terminal.writer().println("LineReader Size Change Example");
            terminal.writer().println("Resize your terminal window while typing to see how LineReader adapts.");
            terminal.writer().println();
            
            // highlight-start
            // Register a handler for window resize signals
            terminal.handle(Signal.WINCH, signal -> {
                Size newSize = terminal.getSize();
                terminal.writer().println("\nTerminal resized: " + newSize.getColumns() + "x" + newSize.getRows());
                terminal.writer().flush();
                
                // Redraw the line
                reader.callWidget(LineReader.REDRAW_LINE);
                reader.callWidget(LineReader.REDISPLAY);
            });
            // highlight-end
            
            // Read lines until "exit" is entered
            String line;
            while (!(line = reader.readLine("prompt> ")).equalsIgnoreCase("exit")) {
                terminal.writer().println("You entered: " + line);
                terminal.writer().println("Current terminal size: " + 
                        terminal.getSize().getColumns() + "x" + 
                        terminal.getSize().getRows());
            }
        } finally {
            terminal.close();
        }
    }
}
```

## Automatic Wrapping and Scrolling

JLine handles automatic wrapping and scrolling based on the terminal size:

```java title="WrappingAndScrollingExample.java" showLineNumbers
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp.Capability;

import java.io.IOException;

public class WrappingAndScrollingExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        try {
            // Clear screen
            terminal.puts(Capability.clear_screen);
            
            // Create a line reader
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();
            
            // Display instructions
            terminal.writer().println("Wrapping and Scrolling Example");
            terminal.writer().println("Resize your terminal window to see how text wraps and scrolls.");
            terminal.writer().println("Press Enter to continue...");
            terminal.writer().flush();
            
            terminal.reader().readLine();
            terminal.puts(Capability.clear_screen);
            
            // highlight-start
            // Get terminal size
            Size size = terminal.getSize();
            int columns = size.getColumns();
            
            // Generate a long line that will wrap
            StringBuilder longLine = new StringBuilder();
            for (int i = 1; i <= 200; i++) {
                longLine.append(i).append(" ");
            }
            
            // Display the long line
            terminal.writer().println("Long line that will wrap based on terminal width:");
            terminal.writer().println(longLine.toString());
            terminal.writer().println();
            
            // Generate text that will cause scrolling
            terminal.writer().println("Text that will cause scrolling:");
            for (int i = 1; i <= 50; i++) {
                terminal.writer().println("Line " + i + " of 50");
            }
            // highlight-end
            
            terminal.writer().println();
            terminal.writer().println("End of scrolling text. Press Enter to continue...");
            terminal.writer().flush();
            
            terminal.reader().readLine();
            terminal.puts(Capability.clear_screen);
            
            // Demonstrate line editing with wrapping
            terminal.writer().println("Line editing with wrapping:");
            terminal.writer().println("Type a long line to see how it wraps during editing.");
            terminal.writer().println();
            
            String line = reader.readLine("prompt> ");
            
            terminal.writer().println();
            terminal.writer().println("You entered a line of length " + line.length() + ":");
            terminal.writer().println(line);
        } finally {
            terminal.close();
        }
    }
}
```

## Minimum Size Requirements

You can specify minimum size requirements for your application:

```java title="MinimumSizeExample.java"
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

import java.io.IOException;

public class MinimumSizeExample {
    // highlight-start
    // Define minimum size requirements
    private static final int MIN_COLUMNS = 80;
    private static final int MIN_ROWS = 24;
    // highlight-end
    
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        try {
            // Clear screen
            terminal.puts(Capability.clear_screen);
            
            // Display instructions
            terminal.writer().println("Minimum Size Requirements Example");
            terminal.writer().println("This application requires a terminal of at least " + 
                    MIN_COLUMNS + "x" + MIN_ROWS + ".");
            terminal.writer().println();
            
            // highlight-start
            // Check if terminal meets minimum size requirements
            Size size = terminal.getSize();
            int columns = size.getColumns();
            int rows = size.getRows();
            
            if (columns < MIN_COLUMNS || rows < MIN_ROWS) {
                terminal.writer().println("WARNING: Terminal size is too small!");
                terminal.writer().println("Current size: " + columns + "x" + rows);
                terminal.writer().println("Required size: " + MIN_COLUMNS + "x" + MIN_ROWS);
                terminal.writer().println();
                terminal.writer().println("Please resize your terminal and press Enter to continue...");
                terminal.writer().flush();
                
                terminal.reader().readLine();
                
                // Check again after user resized
                size = terminal.getSize();
                columns = size.getColumns();
                rows = size.getRows();
                
                if (columns < MIN_COLUMNS || rows < MIN_ROWS) {
                    terminal.writer().println("Terminal is still too small. Some features may not work correctly.");
                } else {
                    terminal.writer().println("Thank you! Terminal now meets minimum size requirements.");
                }
            } else {
                terminal.writer().println("Terminal meets minimum size requirements.");
            }
            // highlight-end
            
            terminal.writer().println();
            terminal.writer().println("Press Enter to continue...");
            terminal.writer().flush();
            
            terminal.reader().readLine();
            
            // Display a simple UI that requires the minimum size
            displayUI(terminal);
        } finally {
            terminal.close();
        }
    }
    
    // Helper method to display a UI that requires the minimum size
    private static void displayUI(Terminal terminal) {
        Size size = terminal.getSize();
        int columns = size.getColumns();
        int rows = size.getRows();
        
        terminal.puts(Capability.clear_screen);
        
        // Draw a box that requires the minimum size
        terminal.writer().println("UI Example (requires " + MIN_COLUMNS + "x" + MIN_ROWS + "):");
        terminal.writer().println();
        
        // Top border
        terminal.writer().print("+");
        for (int i = 0; i < MIN_COLUMNS - 2; i++) {
            terminal.writer().print("-");
        }
        terminal.writer().println("+");
        
        // Middle section
        for (int i = 0; i < MIN_ROWS - 4; i++) {
            terminal.writer().print("|");
            
            if (i == (MIN_ROWS - 4) / 2) {
                // Center text
                String text = "This UI requires a " + MIN_COLUMNS + "x" + MIN_ROWS + " terminal";
                int padding = (MIN_COLUMNS - 2 - text.length()) / 2;
                
                for (int j = 0; j < padding; j++) {
                    terminal.writer().print(" ");
                }
                
                terminal.writer().print(text);
                
                for (int j = 0; j < padding; j++) {
                    terminal.writer().print(" ");
                }
                
                // Adjust for odd lengths
                if ((MIN_COLUMNS - 2 - text.length()) % 2 != 0) {
                    terminal.writer().print(" ");
                }
            } else {
                for (int j = 0; j < MIN_COLUMNS - 2; j++) {
                    terminal.writer().print(" ");
                }
            }
            
            terminal.writer().println("|");
        }
        
        // Bottom border
        terminal.writer().print("+");
        for (int i = 0; i < MIN_COLUMNS - 2; i++) {
            terminal.writer().print("-");
        }
        terminal.writer().println("+");
        
        terminal.writer().println();
        terminal.writer().println("Current terminal size: " + columns + "x" + rows);
        terminal.writer().flush();
    }
}
```

## Best Practices

When handling terminal size changes, consider these best practices:

1. **Always Check Terminal Size**: Always check the terminal size before rendering UI elements.

2. **Listen for Size Change Events**: Register a handler for the `WINCH` signal to detect terminal size changes.

3. **Adapt UI to Available Space**: Design your UI to adapt to different terminal sizes.

4. **Provide Minimum Size Requirements**: Specify minimum size requirements for your application and inform users if they're not met.

5. **Redraw After Size Changes**: Redraw your UI after terminal size changes to ensure proper display.

6. **Use Relative Sizing**: Use relative sizing (percentages) rather than absolute sizing (fixed columns/rows) when possible.

7. **Handle Wrapping Gracefully**: Design your UI to handle text wrapping gracefully.

8. **Test with Different Sizes**: Test your application with different terminal sizes to ensure it adapts correctly.

9. **Consider Mobile Terminals**: Remember that users might be using mobile terminals with very limited screen space.

10. **Provide Fallbacks**: Provide fallback layouts for terminals that are too small for your preferred layout.
