---
sidebar_position: 19
---

# Web and Swing Terminal Implementations

import CodeSnippet from '@site/src/components/CodeSnippet';

JLine provides two additional terminal implementations built on top of the `ScreenTerminal` class that allow you to create terminal interfaces for web applications and desktop GUI applications.

## Overview

Both implementations extend the `ScreenTerminal` class and provide full ANSI escape sequence support:

1. **WebTerminal** - An HTTP-based terminal that serves a web interface
2. **SwingTerminal** - A Swing JComponent-based terminal for desktop applications

## WebTerminal

The `WebTerminal` class provides a web-based terminal interface using the JDK's built-in HTTP server.

### Features

- HTTP server using `com.sun.net.httpserver.HttpServer`
- Real-time terminal updates via AJAX polling
- ANSI escape sequence rendering in HTML/CSS
- Keyboard input handling via JavaScript
- GZIP compression support for better performance
- Session management for multiple users

### Basic Usage

```java
// Create a web terminal
WebTerminal webTerminal = new WebTerminal("localhost", 8080, 80, 24);

// Add initial content
webTerminal.write("Welcome to JLine WebTerminal!\n");
webTerminal.write("$ ");

// Start the HTTP server
webTerminal.start();

// The terminal is now accessible at http://localhost:8080

// Process input/output in a loop
while (webTerminal.isRunning()) {
    // Handle terminal I/O
    String output = webTerminal.read();
    if (output != null && !output.isEmpty()) {
        // Process output
    }
    Thread.sleep(100);
}

// Stop the server
webTerminal.stop();
```

### Constructor Options

```java
// Default constructor (localhost:8080, 80x24)
WebTerminal terminal = new WebTerminal();

// Custom host and port
WebTerminal terminal = new WebTerminal("0.0.0.0", 9090);

// Full customization
WebTerminal terminal = new WebTerminal("localhost", 8080, 120, 40);
```

### Web Interface Features

The web interface provides:
- Full keyboard support including special keys (arrows, function keys, etc.)
- ANSI color rendering with CSS
- Automatic scrolling
- Responsive design
- Copy/paste support (browser dependent)

## SwingTerminal

The `SwingTerminal` class provides a Swing JComponent that can be embedded in desktop applications.

### Features

- Custom painting for terminal characters and attributes
- ANSI color support with configurable color palette
- Font configuration with monospace font support
- Keyboard input handling with special key support
- Mouse support for focus management
- Cursor blinking
- Thread-safe input/output handling

### Basic Usage

```java
// Create a Swing terminal
SwingTerminal swingTerminal = new SwingTerminal(80, 24);

// Get the component for embedding
SwingTerminal.TerminalComponent component = swingTerminal.getComponent();

// Add to a Swing container
JFrame frame = new JFrame("Terminal");
frame.add(component);
frame.pack();
frame.setVisible(true);

// Or use the convenience method
JFrame frame = swingTerminal.createFrame("My Terminal");
frame.setVisible(true);

// Add initial content
swingTerminal.write("Welcome to JLine SwingTerminal!\n");
swingTerminal.write("$ ");

// Handle input in a separate thread
new Thread(() -> {
    try {
        while (true) {
            String input = swingTerminal.takeInput(); // Blocking
            // Or use: String input = swingTerminal.pollInput(); // Non-blocking
            
            if (input != null) {
                swingTerminal.processInput(input);
                
                // Handle specific inputs
                if ("\r".equals(input)) {
                    swingTerminal.write("\n$ ");
                }
            }
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}).start();
```

### Customization

```java
SwingTerminal.TerminalComponent component = swingTerminal.getComponent();

// Change font
Font newFont = new Font("Consolas", Font.PLAIN, 16);
component.setTerminalFont(newFont);

// The component automatically handles:
// - ANSI colors (16 standard colors)
// - Text attributes (bold, underline, inverse)
// - Cursor positioning and blinking
// - Keyboard input translation
```

## Demo Applications

Several demo applications are provided in the `TerminalDemo` class:

### Running the Demos

```bash
# Compile the project
mvn compile

# Run web terminal demo only
mvn exec:java -Dexec.mainClass="org.jline.builtins.TerminalDemo" -Dexec.args="web"

# Run Swing terminal demo only
mvn exec:java -Dexec.mainClass="org.jline.builtins.TerminalDemo" -Dexec.args="swing"

# Run advanced Swing terminal with menu
mvn exec:java -Dexec.mainClass="org.jline.builtins.TerminalDemo" -Dexec.args="advanced"

# Run both demos
mvn exec:java -Dexec.mainClass="org.jline.builtins.TerminalDemo" -Dexec.args="both"
```

### Demo Features

1. **Basic Web Demo**: Simple web terminal with echo functionality
2. **Basic Swing Demo**: Simple Swing terminal window
3. **Advanced Swing Demo**: Swing terminal with menu bar, font selection, and status bar
4. **Both Demos**: Runs web and Swing terminals simultaneously

## Integration with Shell Processes

Both terminals can be integrated with shell processes or command interpreters:

```java
// Example integration with a process
ProcessBuilder pb = new ProcessBuilder("/bin/bash");
Process process = pb.start();

// Connect terminal to process streams
InputStream processOutput = process.getInputStream();
OutputStream processInput = process.getOutputStream();

// Handle process output
new Thread(() -> {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(processOutput))) {
        String line;
        while ((line = reader.readLine()) != null) {
            terminal.write(line + "\n");
        }
    } catch (IOException e) {
        // Handle error
    }
}).start();

// Handle terminal input
new Thread(() -> {
    try (PrintWriter writer = new PrintWriter(processInput, true)) {
        while (true) {
            String input = terminal.takeInput();
            if (input != null) {
                String processed = terminal.pipe(input);
                writer.print(processed);
                writer.flush();
            }
        }
    } catch (Exception e) {
        // Handle error
    }
}).start();
```

## ANSI Escape Sequence Support

Both terminals support the full range of ANSI escape sequences provided by `ScreenTerminal`:

- Cursor movement and positioning
- Text attributes (bold, underline, inverse, etc.)
- Foreground and background colors (16 standard colors)
- Screen clearing and scrolling
- Alternate screen buffer
- Character set selection
- Tab stops
- Scroll regions

## Performance Considerations

### WebTerminal

- Uses GZIP compression for responses > 100 bytes
- AJAX polling interval is 100ms (configurable in JavaScript)
- Supports multiple concurrent sessions
- Memory usage scales with number of active sessions

### SwingTerminal

- Efficient repainting using dirty regions
- Font metrics cached for performance
- Input handling in separate thread to avoid blocking UI
- Cursor blinking timer runs at 500ms intervals

## Thread Safety

Both implementations are designed to be thread-safe:

- `ScreenTerminal` base class uses synchronized methods
- `WebTerminal` handles concurrent HTTP requests safely
- `SwingTerminal` uses proper Swing threading (EDT for UI updates)

## Browser Compatibility

The WebTerminal web interface is compatible with:

- Chrome/Chromium 60+
- Firefox 55+
- Safari 12+
- Edge 79+

Features used:
- ES6 classes and arrow functions
- Fetch API
- FormData
- CSS Grid (for layout)

## Known Limitations

1. **WebTerminal**:
   - No WebSocket support (uses HTTP polling)
   - Limited to single session per browser tab
   - Clipboard access depends on browser security policies

2. **SwingTerminal**:
   - Font must be monospace for proper character alignment
   - Limited to 16 ANSI colors (no 256-color support yet)
   - No built-in scrollback buffer (can be added)

## Future Enhancements

Potential improvements for future versions:

1. **WebTerminal**:
   - WebSocket support for real-time communication
   - Multiple session support with session management
   - File upload/download capabilities
   - Terminal recording and playback

2. **SwingTerminal**:
   - 256-color support
   - Scrollback buffer with search
   - Copy/paste with formatting
   - Terminal tabs support
   - Configurable color themes
