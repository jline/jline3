---
sidebar_position: 6
---

# Terminal Providers

JLine uses a provider architecture to support different terminal implementations across various platforms. This modular approach allows JLine to work in different environments and adapt to the capabilities of the underlying system.

## Provider Architecture

JLine's terminal functionality is implemented through a set of provider modules, each using different technologies to interact with the terminal:

- **JNI (Java Native Interface)**: Direct native code integration
- **JNA (Java Native Access)**: Dynamic access to native libraries
- **Jansi**: Cross-platform ANSI support library
- **FFM (Foreign Function & Memory API)**: Modern Java API for native interoperability
- **Exec**: Fallback using external processes

Each provider has its own strengths and limitations, and JLine will automatically select the most appropriate provider based on the available dependencies and platform.

## Maven Dependencies

To use the terminal providers, add the appropriate dependencies to your project:

```xml
<!-- Core terminal module (required) -->
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-terminal</artifactId>
    <version>3.29.0</version>
</dependency>

<!-- Terminal providers (choose one or more) -->
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-terminal-jansi</artifactId>
    <version>3.29.0</version>
</dependency>

<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-terminal-jna</artifactId>
    <version>3.29.0</version>
</dependency>

<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-terminal-jni</artifactId>
    <version>3.29.0</version>
</dependency>

<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-terminal-ffm</artifactId>
    <version>3.29.0</version>
</dependency>
```

## Provider Comparison

Here's a comparison of the different terminal providers:

| Provider | Technology | Platforms | Advantages | Limitations |
|----------|------------|-----------|------------|-------------|
| JNI | Java Native Interface | All | Fast, direct access to native functions | Requires platform-specific compilation |
| JNA | Java Native Access | All | Dynamic loading of native libraries, no compilation needed | Slightly slower than JNI |
| Jansi | JNI-based library | All, focus on Windows | Good Windows support, ANSI emulation | Additional dependency |
| FFM | Foreign Function & Memory API | All | Modern API, part of Java standard | Requires Java 22+ |
| Exec | External processes | All | Works without native libraries | Limited functionality, slower |

## Provider Selection

JLine uses a discovery mechanism to find and select the appropriate terminal provider. The selection process follows this order:

1. Check for explicitly specified provider via system property or builder method
2. Try FFM provider if running on Java 22+ (recommended)
3. Try JNI provider (recommended)
4. Try JNA provider if JNA is available (deprecated)
5. Try Jansi provider if Jansi is available (deprecated)
6. Fall back to Exec provider

### Provider Selection Methods

There are two ways to influence provider selection:

1. **Using the `provider()` method**: This explicitly selects a specific provider
   ```java
   TerminalBuilder.builder().provider("jansi").build();
   ```

2. **Using boolean methods**: These methods enable or disable specific providers but don't explicitly select them
   ```java
   // Enable Jansi provider (doesn't select it, just makes it available for selection)
   TerminalBuilder.builder().jansi(true).build();

   // Disable Jansi provider
   TerminalBuilder.builder().jansi(false).build();
   ```

To explicitly select a provider, always use the `provider()` method.

You can explicitly specify which provider to use:

```java title="ProviderSelectionExample.java" showLineNumbers
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class ProviderSelectionExample {
    public static void main(String[] args) throws IOException {
        // Let JLine automatically select the best provider
        Terminal autoTerminal = TerminalBuilder.builder()
                .system(true)
                .build();
        System.out.println("Auto-selected provider: " + autoTerminal.getClass().getSimpleName());

        // highlight-start
        // Explicitly specify the JNA provider
        Terminal jnaTerminal = TerminalBuilder.builder()
                .system(true)
                .provider("jna")  // Explicitly select JNA provider
                .build();
        System.out.println("JNA provider: " + jnaTerminal.getClass().getSimpleName());

        // Explicitly specify the Jansi provider
        Terminal jansiTerminal = TerminalBuilder.builder()
                .system(true)
                .provider("jansi")  // Explicitly select Jansi provider
                .build();
        System.out.println("Jansi provider: " + jansiTerminal.getClass().getSimpleName());
        // highlight-end

        // Close the terminals
        autoTerminal.close();
        jnaTerminal.close();
        jansiTerminal.close();
    }
}
```

You can also specify the provider using system properties:

```bash
# Use the JNA provider
java -Dorg.jline.terminal.provider=jna -jar myapp.jar

# Use the Jansi provider
java -Dorg.jline.terminal.provider=jansi -jar myapp.jar

# Use the FFM provider
java -Dorg.jline.terminal.provider=ffm -jar myapp.jar

# Use the JNI provider
java -Dorg.jline.terminal.provider=jni -jar myapp.jar

# Use the Exec provider
java -Dorg.jline.terminal.provider=exec -jar myapp.jar
```

## JLine Terminal-JNA

The `jline-terminal-jna` module provides terminal implementations using the Java Native Access (JNA) library. JNA allows Java code to access native shared libraries without writing JNI code.

**Note: The JNA provider is deprecated. It is recommended to use JNI or FFM providers instead.**

### Features

- Dynamic loading of native libraries
- No need for platform-specific compilation
- Works on Windows, Linux, macOS, and other Unix-like systems
- Supports most terminal features

### Usage

```java title="JnaTerminalExample.java"
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class JnaTerminalExample {
    public static void main(String[] args) throws IOException {
        // highlight-start
        // Create a JNA-based terminal
        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .provider("jna")  // Explicitly select JNA provider
                .build();
        // highlight-end

        System.out.println("Terminal type: " + terminal.getType());
        System.out.println("Terminal size: " + terminal.getWidth() + "x" + terminal.getHeight());

        terminal.writer().println("Hello from JNA terminal!");
        terminal.writer().flush();

        terminal.close();
    }
}
```

## JLine Terminal-Jansi

The `jline-terminal-jansi` module provides terminal implementations using the Jansi library. Jansi is particularly useful for Windows systems, where it provides ANSI escape sequence support.

**Note: The Jansi provider is deprecated. It is recommended to use JNI or FFM providers instead.**

### Features

- Cross-platform ANSI support
- Enhanced Windows terminal support
- Color and cursor positioning on Windows command prompt
- Automatic detection of Windows console vs. Cygwin/MSYS/MinGW

### Usage

```java title="JansiTerminalExample.java"
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class JansiTerminalExample {
    public static void main(String[] args) throws IOException {
        // highlight-start
        // Create a Jansi-based terminal
        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .provider("jansi")  // Explicitly select Jansi provider
                .build();
        // highlight-end

        System.out.println("Terminal type: " + terminal.getType());

        // Use ANSI escape sequences for colors
        terminal.writer().println("\u001B[1;31mRed text\u001B[0m");
        terminal.writer().println("\u001B[1;32mGreen text\u001B[0m");
        terminal.writer().println("\u001B[1;34mBlue text\u001B[0m");
        terminal.writer().flush();

        terminal.close();
    }
}
```

## JLine Terminal-FFM

The `jline-terminal-ffm` module provides terminal implementations leveraging the Foreign Function & Memory API introduced in Java 22. This is the most modern approach and is recommended for applications running on Java 22 or later.

### Features

- Uses standard Java API for native interoperability
- No additional native libraries required
- Good performance
- Clean integration with modern Java

### Usage

```java title="FfmTerminalExample.java"
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class FfmTerminalExample {
    public static void main(String[] args) throws IOException {
        // highlight-start
        // Create an FFM-based terminal (requires Java 22+)
        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .provider("ffm")  // Explicitly select FFM provider
                .build();
        // highlight-end

        System.out.println("Terminal type: " + terminal.getType());

        terminal.writer().println("Hello from FFM terminal!");
        terminal.writer().flush();

        terminal.close();
    }
}
```

## JLine Terminal-JNI

The `jline-terminal-jni` module provides terminal implementations using the Java Native Interface (JNI). This approach requires platform-specific compilation but offers the best performance.

### Features

- Direct access to native functions
- Best performance
- Works on all platforms with appropriate native libraries

### Usage

```java title="JniTerminalExample.java"
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class JniTerminalExample {
    public static void main(String[] args) throws IOException {
        // highlight-start
        // Create a JNI-based terminal
        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .provider("jni")  // Explicitly select JNI provider
                .build();
        // highlight-end

        System.out.println("Terminal type: " + terminal.getType());

        terminal.writer().println("Hello from JNI terminal!");
        terminal.writer().flush();

        terminal.close();
    }
}
```

## Exec Terminal Provider

The Exec terminal provider is a fallback option that uses external processes to interact with the terminal. It's used when no other provider is available or when explicitly requested.

### Features

- Works without native libraries
- Available on all platforms
- Minimal dependencies

### Limitations

- Limited functionality
- Slower than native providers
- May not support all terminal features

### Usage

```java title="ExecTerminalExample.java"
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class ExecTerminalExample {
    public static void main(String[] args) throws IOException {
        // highlight-start
        // Create an Exec-based terminal
        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .provider("exec")  // Explicitly select Exec provider
                .build();
        // highlight-end

        System.out.println("Terminal type: " + terminal.getType());

        terminal.writer().println("Hello from Exec terminal!");
        terminal.writer().flush();

        terminal.close();
    }
}
```

## Dumb Terminal

JLine also provides a "dumb" terminal implementation that doesn't rely on any native functionality. This is useful for environments where terminal capabilities are limited or unavailable.

### Features

- Works in any environment
- No dependencies
- Simple implementation

### Limitations

- No advanced terminal features
- No color support
- No cursor positioning

### Usage

```java title="DumbTerminalExample.java"
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class DumbTerminalExample {
    public static void main(String[] args) throws IOException {
        // highlight-start
        // Create a dumb terminal
        Terminal terminal = TerminalBuilder.builder()
                .dumb(true)  // Request a dumb terminal
                .build();
        // highlight-end

        System.out.println("Terminal type: " + terminal.getType());

        terminal.writer().println("Hello from dumb terminal!");
        terminal.writer().flush();

        terminal.close();
    }
}
```

## Best Practices

When working with JLine terminal providers, consider these best practices:

1. **Let JLine Choose**: In most cases, let JLine automatically select the best provider for the current environment.

2. **Include Multiple Providers**: Include dependencies for multiple providers to ensure JLine can find a suitable implementation.

3. **Fallback Gracefully**: Handle cases where advanced terminal features might not be available.

4. **Test on Different Platforms**: Test your application on different platforms to ensure it works with different terminal providers.

5. **Consider Java Version**: Use the FFM provider for Java 22+ applications for the best integration with modern Java. JNI is recommended for Java versions below 22.

6. **Check Terminal Capabilities**: Use the terminal's capabilities to determine what features are available.

7. **Close Terminals**: Always close terminals when you're done with them to release resources.

8. **Handle Exceptions**: Be prepared to handle exceptions that might occur when creating or using terminals.

## Troubleshooting

### Common Issues

#### Unable to create a system terminal

This error occurs when JLine cannot find a suitable terminal provider. To resolve:

1. Check that you have included the appropriate terminal provider dependencies
2. Try explicitly specifying a provider using `TerminalBuilder`
3. Fall back to a dumb terminal if necessary

```java
// Simply enable dumb mode if you need a fallback
Terminal terminal = TerminalBuilder.builder()
        .system(true)
        .dumb(true)  // Falls back to dumb if system terminal can't be created
        .build();
```

#### Recommended provider dependencies

It's recommended to use the JNI or FFM providers:

```xml
<!-- For JNI support (recommended for Java < 22) -->
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-terminal-jni</artifactId>
    <version>3.29.0</version>
</dependency>

<!-- For FFM support (recommended for Java 22+) -->
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-terminal-ffm</artifactId>
    <version>3.29.0</version>
</dependency>
```

#### Illegal reflective access warnings

When using JNA or Jansi on newer Java versions, you might see warnings about illegal reflective access. These are generally harmless but can be addressed by:

1. Using the FFM provider on Java 22+
2. Using the JNI provider on Java versions below 22
3. Adding appropriate `--add-opens` JVM arguments if you must use JNA/Jansi
4. Suppressing the warnings if they don't affect functionality

#### Terminal size issues

If your application isn't correctly detecting the terminal size:

1. Check if you're running in a real terminal (not redirected)
2. Try different terminal providers
3. Provide a default size for non-interactive terminals

```java
Terminal terminal = TerminalBuilder.builder()
        .system(true)
        .build();

if (!terminal.getSize().getColumns() > 0) {
    // Set a default size for non-interactive terminals
    terminal.setSize(new Size(80, 24));
}
```
