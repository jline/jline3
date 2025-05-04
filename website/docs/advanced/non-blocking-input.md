---
sidebar_position: 3
---

# Non-Blocking Input

JLine provides support for non-blocking input, allowing you to read user input without blocking the execution of your application. This is particularly useful for applications that need to perform background tasks while still being responsive to user input.

## NonBlockingReader

The `NonBlockingReader` class is the key component for non-blocking input in JLine:

```java title="NonBlockingReaderExample.java" showLineNumbers
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.NonBlockingReader;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class NonBlockingReaderExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // highlight-start
        // Get a non-blocking reader
        NonBlockingReader reader = terminal.reader();
        // highlight-end
        
        terminal.writer().println("Type something (program will exit after 10 seconds):");
        terminal.writer().flush();
        
        // Track start time
        long startTime = System.currentTimeMillis();
        
        // Run for 10 seconds
        while (System.currentTimeMillis() - startTime < 10000) {
            try {
                // highlight-start
                // Check if input is available
                if (reader.available() > 0) {
                    // Read a character (non-blocking)
                    int c = reader.read();
                    terminal.writer().println("Read character: " + (char)c);
                    terminal.writer().flush();
                }
                // highlight-end
                
                // Simulate background work
                terminal.writer().print(".");
                terminal.writer().flush();
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        terminal.writer().println("\nTime's up!");
        terminal.close();
    }
}
```

## Reading with Timeout

You can also read with a timeout, which will block for up to the specified time:

```java title="TimeoutReadExample.java"
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.NonBlockingReader;

import java.io.IOException;

public class TimeoutReadExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        NonBlockingReader reader = terminal.reader();
        
        terminal.writer().println("Press any key within 5 seconds:");
        terminal.writer().flush();
        
        try {
            // highlight-start
            // Read with a 5-second timeout
            int c = reader.read(5, TimeUnit.SECONDS);
            // highlight-end
            
            if (c != -1) {
                terminal.writer().println("You pressed: " + (char)c);
            } else {
                terminal.writer().println("Timeout expired!");
            }
        } catch (IOException e) {
            terminal.writer().println("Error reading input: " + e.getMessage());
        }
        
        terminal.close();
    }
}
```

## Combining with LineReader

You can combine non-blocking input with the `LineReader` for more sophisticated input handling:

```java title="NonBlockingLineReaderExample.java" showLineNumbers
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.NonBlockingReader;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class NonBlockingLineReaderExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        LineReader lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        
        // Flag to control background task
        AtomicBoolean running = new AtomicBoolean(true);
        
        // Start background task
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                while (running.get()) {
                    // Simulate background work
                    terminal.writer().print(".");
                    terminal.writer().flush();
                    TimeUnit.SECONDS.sleep(1);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                terminal.writer().println("Error in background task: " + e.getMessage());
                terminal.writer().flush();
            }
        });
        
        try {
            // Main input loop
            while (running.get()) {
                try {
                    // Read a line (this will block)
                    String line = lineReader.readLine("\nprompt> ");
                    
                    if ("exit".equalsIgnoreCase(line)) {
                        running.set(false);
                    } else {
                        terminal.writer().println("You entered: " + line);
                        terminal.writer().flush();
                    }
                } catch (UserInterruptException e) {
                    // Ctrl+C pressed
                    running.set(false);
                }
            }
        } finally {
            // Shutdown background task
            executor.shutdownNow();
            try {
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            terminal.writer().println("\nExiting...");
            terminal.close();
        }
    }
}
```

## Asynchronous Input Handling

For more complex scenarios, you can set up asynchronous input handling:

```java title="AsyncInputExample.java"
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.NonBlockingReader;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncInputExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        NonBlockingReader reader = terminal.reader();
        
        // Flag to control input handling
        AtomicBoolean running = new AtomicBoolean(true);
        
        // Start input handling thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                // highlight-start
                // Continuously read input
                while (running.get()) {
                    int c = reader.read(100);
                    if (c != -1) {
                        // Process the input
                        terminal.writer().println("\nReceived input: " + (char)c);
                        
                        if (c == 'q' || c == 'Q') {
                            running.set(false);
                        }
                        
                        terminal.writer().flush();
                    }
                }
                // highlight-end
            } catch (IOException e) {
                if (running.get()) {
                    terminal.writer().println("Error reading input: " + e.getMessage());
                    terminal.writer().flush();
                }
            }
        });
        
        // Main application loop
        try {
            terminal.writer().println("Press keys (q to quit):");
            terminal.writer().flush();
            
            int count = 0;
            while (running.get() && count < 30) {
                // Simulate application work
                terminal.writer().print(".");
                terminal.writer().flush();
                
                TimeUnit.MILLISECONDS.sleep(500);
                count++;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // Shutdown input handling
            running.set(false);
            executor.shutdownNow();
            try {
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            terminal.writer().println("\nExiting...");
            terminal.close();
        }
    }
}
```

## Raw Mode vs. Cooked Mode

Understanding terminal modes is important for non-blocking input:

- **Cooked Mode (Default)**: Input is buffered until Enter is pressed
- **Raw Mode**: Input is made available immediately, without buffering

```java title="RawModeExample.java"
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.InputFlag;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.NonBlockingReader;

import java.io.IOException;

public class RawModeExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        try {
            // Save original terminal attributes
            Attributes originalAttributes = terminal.getAttributes();
            
            // highlight-start
            // Configure raw mode
            Attributes rawAttributes = new Attributes(originalAttributes);
            rawAttributes.setInputFlag(InputFlag.ICANON, false);  // Disable canonical mode
            rawAttributes.setInputFlag(InputFlag.ECHO, false);    // Disable echo
            rawAttributes.setControlChar(Attributes.ControlChar.VMIN, 0);  // Non-blocking
            rawAttributes.setControlChar(Attributes.ControlChar.VTIME, 0); // No timeout
            
            // Enter raw mode
            terminal.setAttributes(rawAttributes);
            // highlight-end
            
            terminal.writer().println("Terminal is in raw mode. Press keys (q to quit):");
            terminal.writer().flush();
            
            NonBlockingReader reader = terminal.reader();
            
            // Read characters until 'q' is pressed
            while (true) {
                int c = reader.read(100);
                if (c != -1) {
                    terminal.writer().println("Read: " + (char)c + " (ASCII: " + c + ")");
                    terminal.writer().flush();
                    
                    if (c == 'q' || c == 'Q') {
                        break;
                    }
                }
            }
            
            // Restore original terminal attributes
            terminal.setAttributes(originalAttributes);
        } finally {
            terminal.close();
        }
    }
}
```

## Best Practices

When working with non-blocking input in JLine, consider these best practices:

1. **Handle Interruptions**: Always handle interruptions properly to ensure clean shutdown.

2. **Use Separate Threads**: Keep input handling in a separate thread from your main application logic.

3. **Set Appropriate Timeouts**: Choose timeout values that balance responsiveness with CPU usage.

4. **Restore Terminal State**: Always restore the terminal to its original state before exiting.

5. **Check for EOF**: Check for end-of-file conditions (-1 return value) when reading.

6. **Use Atomic Flags**: Use atomic boolean flags for thread coordination.

7. **Provide User Feedback**: Keep users informed about what's happening, especially during long operations.

8. **Graceful Shutdown**: Implement graceful shutdown procedures for all threads.

9. **Error Handling**: Implement robust error handling for I/O exceptions.

10. **Test on Different Platforms**: Test your non-blocking input handling on different platforms and terminal types.
