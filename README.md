# JLine - Java Console Library

[![Maven Central](https://img.shields.io/maven-central/v/org.jline/jline.svg)](https://search.maven.org/search?q=g:org.jline)
[![Build Status](https://github.com/jline/jline3/actions/workflows/master-build.yml/badge.svg)](https://github.com/jline/jline3/actions)
[![License](https://img.shields.io/badge/License-BSD-blue.svg)](https://opensource.org/licenses/BSD-3-Clause)

JLine is a Java library for handling console input. It's similar to [GNU Readline](https://tiswww.case.edu/php/chet/readline/rltop.html) but with a focus on portability, flexibility, and integration with Java applications. See https://jline.org for its documentation.

## Requirements

- **Java 11 or higher**: JLine 4.x requires Java 11 as the minimum runtime version
- **Maven 4.0 or higher**: JLine 4.x requires Maven 4.0+ for building from source
- **Note**: JLine 3.x supports Java 8+ and Maven 3.x+, but JLine 4.x requires Java 11+ and Maven 4.0+

## Features

- **Cross-platform support**: Works on Windows, macOS, Linux, and other Unix-like systems
- **Line editing**: Emacs and Vi editing modes with customizable key bindings
- **History management**: Persistent command history with search capabilities
- **Tab completion**: Customizable completion for commands, arguments, and file paths
- **Syntax highlighting**: Colorize input based on custom rules
- **Password masking**: Secure input for sensitive information
- **ANSI terminal support**: Full support for ANSI escape sequences and colors
- **Unicode support**: Proper handling of wide characters and combining marks
- **ConsoleUI components**: Interactive UI elements like menus, checkboxes, and prompts

## Installation

### Maven

```xml
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline</artifactId>
    <version>4.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'org.jline:jline:4.0.0'
```

## Quick Start

Here's a simple example to get you started:

```java
import org.jline.reader.*;
import org.jline.reader.impl.*;
import org.jline.terminal.*;
import org.jline.terminal.impl.*;

public class HelloJLine {
    public static void main(String[] args) {
        try {
            // Create a terminal
            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();

            // Create line reader
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            // Prompt and read input
            String line = reader.readLine("JLine > ");

            // Print the result
            System.out.println("You entered: " + line);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Modules

JLine is organized into several modules:

- **jline-terminal**: Core terminal functionality
- **jline-reader**: Line editing and reading
- **jline-console**: Higher-level console abstractions
- **jline-console-ui**: Interactive UI components (checkboxes, lists, etc.) - **Deprecated**
- **jline-prompt**: Modern prompt API for interactive console applications
- **jline-style**: Styling and coloring support
- **jline-builtins**: Built-in commands and utilities
- **jline-remote-ssh**: SSH server support
- **jline-remote-telnet**: Telnet server support

## JPMS (Java Platform Module System) Support

JLine provides full support for JPMS starting with version 4.0. The following modules are proper JPMS modules with `module-info.java`:

### ✅ JPMS Modules (with module-info.java)

| Module | Artifact ID | Module Name | Description |
|--------|-------------|-------------|-------------|
| **Core Modules** | | | |
| Native | `jline-native` | `org.jline.nativ` | Native library loading |
| Terminal | `jline-terminal` | `org.jline.terminal` | Core terminal functionality |
| Terminal FFM | `jline-terminal-ffm` | `org.jline.terminal.ffm` | FFM-based terminal (JDK 22+) |
| Reader | `jline-reader` | `org.jline.reader` | Line editing and reading |
| Style | `jline-style` | `org.jline.style` | Styling and coloring |
| **Extended Modules** | | | |
| Builtins | `jline-builtins` | `org.jline.builtins` | Built-in commands |
| Console UI | `jline-console-ui` | `org.jline.console.ui` | Interactive UI components (deprecated) |
| Prompt | `jline-prompt` | `org.jline.prompt` | Modern prompt API for interactive applications |
| Console | `jline-console` | `org.jline.console` | Console framework |
| Jansi Core | `jline-jansi-core` | `org.jline.jansi.core` | ANSI support |
| Curses | `jline-curses` | `org.jline.curses` | Curses-like UI components |

### ❌ Non-JPMS Modules (automatic modules)

These modules remain as automatic modules for compatibility:

| Module | Artifact ID | Reason |
|--------|-------------|---------|
| Terminal Jansi | `jline-terminal-jansi` | Legacy compatibility |
| Groovy | `jline-groovy` | Groovy integration |
| Remote SSH | `jline-remote-ssh` | SSH server support |
| Remote Telnet | `jline-remote-telnet` | Telnet server support |
| Demo | `jline-demo` | Example applications |
| Graal | `jline-graal` | GraalVM native image support |

### 🎯 Usage with JPMS

When using JLine in a modular application, add the required modules to your `module-info.java`:

```java
module your.application {
    requires org.jline.terminal;
    requires org.jline.reader;
    requires org.jline.style;        // Optional: for styling
    requires org.jline.builtins;     // Optional: for built-in commands
    requires org.jline.console;      // Optional: for console framework
    requires org.jline.prompt;       // Optional: for modern prompt API
    requires org.jline.console.ui;   // Optional: for legacy UI components (deprecated)
}
```

**Note**: The FFM terminal provider (`jline-terminal-ffm`) requires JDK 22+ and native access permissions:
```bash
java --enable-native-access=org.jline.terminal.ffm your.application
```

## Documentation

- [Website](https://jline.org)
- [Wiki](https://github.com/jline/jline3/wiki)
- [Javadoc](https://www.javadoc.io/doc/org.jline/jline/latest/index.html)

## License

JLine is licensed under the [BSD License](https://opensource.org/licenses/BSD-3-Clause).

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
