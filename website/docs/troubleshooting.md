---
sidebar_position: 8
---

# Troubleshooting Guide

JLine is designed to work across different platforms and terminal environments, but you may encounter issues due to platform differences, terminal capabilities, or configuration problems. This guide addresses common issues and provides solutions to help you troubleshoot JLine-related problems.

## Common Issues

### Unable to Create a System Terminal

One of the most common issues is the "Unable to create a system terminal" error:

```
java.io.IOError: Unable to create a system terminal
```

This error occurs when JLine cannot initialize a terminal for the current environment.

#### Possible Causes and Solutions:

1. **Missing Terminal Provider Dependencies**

   JLine requires specific dependencies for different terminal providers.

   **Solution**: Add the appropriate dependencies to your project:

   ```xml
   <!-- For JNI support (recommended for Java < 22) -->
   <dependency>
       <groupId>org.jline</groupId>
       <artifactId>jline-terminal-jni</artifactId>
       <version>%%JLINE_VERSION%%</version>
   </dependency>

   <!-- For FFM support (recommended for Java 22+) -->
   <dependency>
       <groupId>org.jline</groupId>
       <artifactId>jline-terminal-ffm</artifactId>
       <version>%%JLINE_VERSION%%</version>
   </dependency>
   ```

2. **Running in a Non-Interactive Environment**

   JLine may fail when running in a non-interactive environment, such as when input/output is redirected.

   **Solution**: Fall back to a dumb terminal:

   ```java
   // Simply enable dumb mode if you need a fallback
   Terminal terminal = TerminalBuilder.builder()
           .system(true)
           .dumb(true)  // Falls back to dumb if system terminal can't be created
           .build();
   ```

3. **IDE Console Limitations**

   Some IDE consoles (like IntelliJ's) don't fully support all terminal features.

   **Solution**: Run your application in a real terminal outside the IDE, or configure your IDE to use an external terminal.

4. **Windows-Specific Issues**

   On Windows, you might encounter issues with the console or with Cygwin/MinGW environments.

   **Solution**: Explicitly specify the JNI or FFM provider:

   ```java
   Terminal terminal = TerminalBuilder.builder()
           .system(true)
           .provider("jni")  // or "ffm" for Java 22+
           .build();
   ```

### ANSI Color Codes Not Working

If ANSI color codes or other escape sequences aren't working properly:

#### Possible Causes and Solutions:

1. **Terminal Doesn't Support ANSI**

   Some terminals don't support ANSI escape sequences.

   **Solution**: Check terminal capabilities before using ANSI codes:

   ```java
   boolean supportsAnsi = terminal.getType().contains("ansi");
   if (supportsAnsi) {
       // Use ANSI escape sequences
   } else {
       // Use plain text alternative
   }
   ```

2. **Windows Command Prompt**

   The standard Windows Command Prompt has limited ANSI support.

   **Solution**: Use the JNI or FFM provider, which provides ANSI support on Windows:

   ```java
   Terminal terminal = TerminalBuilder.builder()
           .system(true)
           .provider("jni")  // or "ffm" for Java 22+
           .build();
   ```

3. **Output Redirection**

   ANSI escape sequences might not work when output is redirected to a file.

   **Solution**: Detect if output is being redirected and disable ANSI codes accordingly:

   ```java
   boolean interactive = terminal.isInteractive();
   if (interactive) {
       // Use ANSI escape sequences
   } else {
       // Use plain text alternative
   }
   ```

### Line Editing Features Not Working

If line editing features like history navigation, tab completion, or key bindings aren't working:

#### Possible Causes and Solutions:

1. **Terminal in Non-Raw Mode**

   Line editing features require the terminal to be in raw mode.

   **Solution**: Ensure the terminal is properly initialized:

   ```java
   Terminal terminal = TerminalBuilder.builder()
           .system(true)
           .build();

   LineReader reader = LineReaderBuilder.builder()
           .terminal(terminal)
           .build();
   ```

2. **Key Binding Conflicts**

   Custom key bindings might conflict with default ones.

   **Solution**: Check for conflicts and use different key combinations:

   ```java
   // Get the current binding for Ctrl+A
   KeyMap<Binding> keyMap = reader.getKeyMaps().get(LineReader.MAIN);
   Binding currentBinding = keyMap.getBound(KeyMap.ctrl('A'));

   // Only bind if not already bound to something important
   if (currentBinding == null) {
       keyMap.bind(myWidget, KeyMap.ctrl('A'));
   }
   ```

3. **Incorrect Terminal Type**

   If the terminal type is incorrectly detected, some features might not work.

   **Solution**: Explicitly specify the terminal type:

   ```java
   Terminal terminal = TerminalBuilder.builder()
           .system(true)
           .type("xterm-256color")
           .build();
   ```

### Terminal Size Issues

If the terminal size is incorrectly detected or not updated when the window is resized:

#### Possible Causes and Solutions:

1. **Non-Interactive Terminal**

   Size detection might not work in non-interactive terminals.

   **Solution**: Provide default dimensions:

   ```java
   Terminal terminal = TerminalBuilder.builder()
           .system(true)
           .build();

   if (terminal.getWidth() <= 0 || terminal.getHeight() <= 0) {
       terminal.setSize(new Size(80, 24));
   }
   ```

2. **Resize Events Not Handled**

   Terminal resize events need to be handled explicitly.

   **Solution**: Add a signal handler for window resize events:

   ```java
   terminal.handle(Signal.WINCH, signal -> {
       // Terminal has been resized
       Size size = terminal.getSize();
       System.out.println("Terminal resized to " + size.getColumns() + "x" + size.getRows());

       // Update your UI accordingly
   });
   ```

### Performance Issues

If you experience performance issues with JLine:

#### Possible Causes and Solutions:

1. **Too Many Terminal Operations**

   Excessive terminal operations can slow down your application.

   **Solution**: Batch updates and minimize terminal operations:

   ```java
   // Instead of updating line by line
   for (String line : lines) {
       terminal.writer().println(line);
       terminal.flush();  // Avoid flushing after each line
   }

   // Batch updates
   for (String line : lines) {
       terminal.writer().println(line);
   }
   terminal.flush();  // Flush once at the end
   ```

2. **Inefficient Redrawing**

   Redrawing the entire screen too frequently can cause flickering and performance issues.

   **Solution**: Use the `Display` class for efficient screen updates:

   ```java
   Display display = new Display(terminal, true);
   List<AttributedString> lines = new ArrayList<>();

   // Update only what changed
   lines.add(new AttributedString("Line 1"));
   lines.add(new AttributedString("Line 2"));
   display.update(lines, 0);
   ```

3. **Complex Completers**

   Complex tab completion logic can slow down input handling.

   **Solution**: Optimize your completers and consider caching results:

   ```java
   // Cache completion results
   private Map<String, List<Candidate>> completionCache = new HashMap<>();

   @Override
   public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
       String buffer = line.line();

       // Check cache first
       if (completionCache.containsKey(buffer)) {
           candidates.addAll(completionCache.get(buffer));
           return;
       }

       // Compute completions
       List<Candidate> results = computeCompletions(line);

       // Cache results
       completionCache.put(buffer, new ArrayList<>(results));

       // Add to candidates
       candidates.addAll(results);
   }
   ```

## Platform-Specific Issues

### Windows Issues

Windows has some specific challenges for terminal applications:

#### Common Windows Issues and Solutions:

1. **Console API Limitations**

   The Windows Console API has limitations compared to Unix terminals.

   **Solution**: Use the JNI or FFM provider for better Windows support:

   ```java
   Terminal terminal = TerminalBuilder.builder()
           .system(true)
           .provider("jni")  // or "ffm" for Java 22+
           .build();
   ```

2. **Ctrl+C Handling**

   By default, Ctrl+C terminates the application on Windows.

   **Solution**: Install a custom signal handler:

   ```java
   terminal.handle(Signal.INT, signal -> {
       // Handle Ctrl+C gracefully
       terminal.writer().println("Ctrl+C pressed, type 'exit' to quit");
       terminal.flush();
   });
   ```

3. **Unicode Support**

   Windows Command Prompt has limited Unicode support.

   **Solution**: Use Windows Terminal or ConEmu for better Unicode support, or set an appropriate code page:

   ```java
   // Set UTF-8 code page (requires JNA)
   if (System.getProperty("os.name").toLowerCase().contains("win")) {
       try {
           new ProcessBuilder("cmd", "/c", "chcp", "65001").inheritIO().start().waitFor();
       } catch (Exception e) {
           // Handle exception
       }
   }
   ```

4. **Line Ending Issues**

   Windows uses CRLF line endings, which can cause issues.

   **Solution**: Normalize line endings:

   ```java
   String normalized = input.replace("\r\n", "\n");
   ```

### Unix/Linux Issues

Unix and Linux systems also have their own challenges:

#### Common Unix/Linux Issues and Solutions:

1. **Terminal Reset on Exit**

   The terminal might not be properly reset when the application exits.

   **Solution**: Ensure proper cleanup:

   ```java
   try {
       // Your application code
   } finally {
       terminal.close();
   }
   ```

2. **Signal Handling**

   Unix signals need proper handling for a good user experience.

   **Solution**: Install signal handlers for common signals:

   ```java
   terminal.handle(Signal.INT, signal -> {
       // Handle Ctrl+C
   });

   terminal.handle(Signal.WINCH, signal -> {
       // Handle terminal resize
   });

   terminal.handle(Signal.TSTP, signal -> {
       // Handle Ctrl+Z (suspend)
       terminal.pause();
   });

   terminal.handle(Signal.CONT, signal -> {
       // Handle resume from suspension
       terminal.resume();
   });
   ```

3. **SSH Session Issues**

   JLine might behave differently in SSH sessions.

   **Solution**: Check if running in an SSH session and adjust accordingly:

   ```java
   boolean isSsh = System.getenv("SSH_CLIENT") != null || System.getenv("SSH_TTY") != null;
   if (isSsh) {
       // Adjust settings for SSH session
   }
   ```

## Remote Terminal Issues

When using JLine's remote terminal capabilities (Telnet and SSH), you might encounter specific issues:

### Telnet Connection Issues

#### Possible Causes and Solutions:

1. **Port Already in Use**

   The default Telnet port might already be in use by another application.

   **Solution**: Specify a different port when starting the Telnet server:

   ```java
   Telnet telnet = new Telnet(terminal, shellProvider);
   telnet.telnetd(System.out, System.err, new String[]{"--port=2023", "start"});
   ```

2. **Connection Refused**

   The server might be binding to a specific interface that's not accessible.

   **Solution**: Specify the interface to bind to:

   ```java
   Telnet telnet = new Telnet(terminal, shellProvider);
   telnet.telnetd(System.out, System.err, new String[]{"--ip=0.0.0.0", "start"});
   ```

3. **Terminal Type Negotiation**

   Some Telnet clients might not properly negotiate terminal types.

   **Solution**: Handle different terminal types in your ShellProvider implementation:

   ```java
   public void shell(Terminal terminal, Map<String, String> environment) {
       String termType = terminal.getType();
       if (termType == null || termType.isEmpty() || "dumb".equals(termType)) {
           // Use basic terminal features only
       }
       // Continue with shell implementation
   }
   ```

### SSH Connection Issues

#### Possible Causes and Solutions:

1. **Authentication Failures**

   SSH requires proper authentication configuration.

   **Solution**: Configure authentication properly:

   ```java
   // Set up password authentication
   server.setPasswordAuthenticator((username, password, session) ->
       "admin".equals(username) && "password".equals(password));

   // Or set up public key authentication
   server.setPublickeyAuthenticator((username, key, session) -> {
       // Verify the key
       return true; // if valid
   });
   ```

2. **Host Key Verification**

   Clients might reject connections due to unknown host keys.

   **Solution**: Use a persistent key provider:

   ```java
   // Instead of SimpleGeneratorHostKeyProvider which generates temporary keys
   File keyFile = new File("hostkey.ser");
   server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(keyFile.toPath()));
   ```

3. **Terminal Size Issues**

   Remote terminals might not properly report or update their size.

   **Solution**: Handle window change requests and set default size if needed:

   ```java
   // In your shell implementation
   if (terminal.getWidth() <= 0 || terminal.getHeight() <= 0) {
       terminal.setSize(new Size(80, 24));
   }

   // Register for window change events
   terminal.handle(Signal.WINCH, signal -> {
       // Terminal size has changed
       Size size = terminal.getSize();
       // Update your UI accordingly
   });
   ```

### General Remote Terminal Troubleshooting

1. **Connection Timeouts**

   Remote connections might time out if idle for too long.

   **Solution**: Configure timeout settings:

   ```java
   // For Telnet
   ConnectionManager manager = new ConnectionManager(
       maxConnections,
       warningTimeout,    // e.g., 5 * 60 * 1000 (5 minutes)
       disconnectTimeout, // e.g., 10 * 60 * 1000 (10 minutes)
       housekeepingInterval, // e.g., 60 * 1000 (1 minute)
       filter, loginShell, lineMode);

   // For SSH
   server.getProperties().put(SshServer.IDLE_TIMEOUT, "600000"); // 10 minutes
   ```

2. **Character Encoding Issues**

   Remote clients might use different character encodings.

   **Solution**: Explicitly set the encoding:

   ```java
   Terminal terminal = TerminalBuilder.builder()
       .streams(inputStream, outputStream)
       .encoding(StandardCharsets.UTF_8)
       .name("remote")
       .build();
   ```

3. **Performance with Multiple Connections**

   Handling many concurrent connections can impact performance.

   **Solution**: Limit the number of concurrent connections and optimize your shell implementation:

   ```java
   // Limit to 10 concurrent connections
   ConnectionManager manager = new ConnectionManager(
       10, // maxConnections
       warningTimeout,
       disconnectTimeout,
       housekeepingInterval,
       filter, loginShell, lineMode);
   ```

## Advanced Troubleshooting

For more complex issues, try these advanced troubleshooting techniques:

### Enable Debug Logging

JLine uses Java Util Logging (JUL). Enable debug logging to see what's happening:

```java
// Configure Java Util Logging
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.ConsoleHandler;

public void configureLogging() {
    ConsoleHandler handler = new ConsoleHandler();
    handler.setLevel(Level.FINE);

    Logger logger = Logger.getLogger("org.jline");
    logger.setLevel(Level.FINE);
    logger.addHandler(handler);
}
```

Or using a logging.properties file:

```properties
# logging.properties
handlers=java.util.logging.ConsoleHandler
.level=INFO
java.util.logging.ConsoleHandler.level=FINE
org.jline.level=FINE
```

### Inspect Terminal Capabilities

Check what capabilities are available in your terminal:

```java
Terminal terminal = TerminalBuilder.builder().build();

System.out.println("Terminal type: " + terminal.getType());
System.out.println("Interactive: " + terminal.isInteractive());
System.out.println("Ansi supported: " + terminal.getType().contains("ansi"));
System.out.println("Width: " + terminal.getWidth());
System.out.println("Height: " + terminal.getHeight());

// Check specific capabilities
String enterAm = terminal.getStringCapability(Capability.enter_am_mode);
String exitAm = terminal.getStringCapability(Capability.exit_am_mode);
System.out.println("Auto-margin mode supported: " + (enterAm != null && exitAm != null));
```

### Test with Different Terminal Providers

Try different terminal providers to isolate the issue:

```java
// Try with JNI provider (recommended for Java < 22)
try {
    Terminal jniTerminal = TerminalBuilder.builder()
            .provider("jni")
            .build();
    System.out.println("JNI terminal created successfully: " + jniTerminal.getType());
    jniTerminal.close();
} catch (Exception e) {
    System.err.println("JNI terminal failed: " + e.getMessage());
}

// Try with FFM provider (recommended for Java 22+)
try {
    Terminal ffmTerminal = TerminalBuilder.builder()
            .provider("ffm")
            .build();
    System.out.println("FFM terminal created successfully: " + ffmTerminal.getType());
    ffmTerminal.close();
} catch (Exception e) {
    System.err.println("FFM terminal failed: " + e.getMessage());
}

// Try with dumb terminal
try {
    Terminal dumbTerminal = TerminalBuilder.builder()
            .dumb(true)
            .build();
    System.out.println("Dumb terminal created successfully: " + dumbTerminal.getType());
    dumbTerminal.close();
} catch (Exception e) {
    System.err.println("Dumb terminal failed: " + e.getMessage());
}
```

### Check for Native Library Issues

JLine uses native libraries through JNI or FFM. Check for native library issues:

```java
try {
    // Check if JNI native library is available
    Terminal jniTerminal = TerminalBuilder.builder()
            .provider("jni")
            .build();
    System.out.println("JNI terminal created successfully");
    jniTerminal.close();
} catch (Throwable t) {
    System.err.println("Error with JNI terminal: " + t.getMessage());
}

// For Java 22+
try {
    // Check if FFM is available
    Terminal ffmTerminal = TerminalBuilder.builder()
            .provider("ffm")
            .build();
    System.out.println("FFM terminal created successfully");
    ffmTerminal.close();
} catch (Throwable t) {
    System.err.println("Error with FFM terminal: " + t.getMessage());
}
```

## Getting Help

If you're still experiencing issues after trying the solutions in this guide:

1. **Check the JLine GitHub Issues**: Search the [JLine GitHub issues](https://github.com/jline/jline3/issues) to see if your problem has been reported and if there's a solution.

2. **JLine Mailing Lists**: Post your question to the [JLine Users mailing list](https://groups.google.com/g/jline-users).

3. **Stack Overflow**: Ask a question on Stack Overflow with the `jline` tag.

4. **Create a Minimal Reproducible Example**: If you're reporting a bug, create a minimal example that demonstrates the issue.

5. **Include Environment Information**: When seeking help, include details about your environment:
   - JLine version
   - Java version
   - Operating system
   - Terminal emulator
   - Relevant code snippets
   - Error messages and stack traces

## Conclusion

JLine is a powerful library for creating interactive command-line applications, but it can sometimes be challenging to get it working perfectly across all environments. By understanding common issues and their solutions, you can troubleshoot problems more effectively and create robust terminal applications that work well on all platforms.
