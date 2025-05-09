# JLine - Java Console Library

[![Maven Central](https://img.shields.io/maven-central/v/org.jline/jline.svg)](https://search.maven.org/search?q=g:org.jline)
[![Build Status](https://github.com/jline/jline3/actions/workflows/master-build.yml/badge.svg)](https://github.com/jline/jline3/actions)
[![License](https://img.shields.io/badge/License-BSD-blue.svg)](https://opensource.org/licenses/BSD-3-Clause)

JLine is a Java library for handling console input. It's similar to [GNU Readline](https://tiswww.case.edu/php/chet/readline/rltop.html) but with a focus on portability, flexibility, and integration with Java applications. See https://jline.org for its documentation.

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
    <version>3.29.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'org.jline:jline:3.29.0'
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
- **jline-console-ui**: Interactive UI components (checkboxes, lists, etc.)
- **jline-style**: Styling and coloring support
- **jline-builtins**: Built-in commands and utilities
- **jline-remote-ssh**: SSH server support
- **jline-remote-telnet**: Telnet server support

## ConsoleUI

<img src="./docs/ConsoleUI-Logo.png" width="200" align="right" alt="ConsoleUI logo">

JLine includes ConsoleUI, a library for creating interactive command-line interfaces inspired by [Inquirer.js](https://github.com/SBoudrias/Inquirer.js). It provides simple UI elements for ANSI terminals:

- Text input with completion
- Checkboxes
- Lists
- Expandable choices
- Yes/No questions

[Watch a demo of ConsoleUI on YouTube](https://youtu.be/6dB3CyOX9rU)

### ConsoleUI Example

```java
import org.jline.console.ui.*;
import org.jline.console.ui.prompt.*;

public class ConsoleUIExample {
    public static void main(String[] args) {
        ConsolePrompt prompt = new ConsolePrompt();
        PromptBuilder builder = prompt.getPromptBuilder();
        
        // Create a simple yes/no prompt
        Prompt confirmPrompt = builder.createConfirmPromp()
                .name("continue")
                .message("Do you want to continue?")
                .defaultValue(ConfirmChoice.ConfirmationValue.YES)
                .build();
        
        try {
            // Display the prompt and get the result
            Map<String, Object> result = prompt.prompt(confirmPrompt);
            System.out.println("You chose: " + result.get("continue"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

For more details on ConsoleUI, see the [how-to guide](doc/howto.md).

## Documentation

- [Website](https://jline.org)
- [Wiki](https://github.com/jline/jline3/wiki)
- [Javadoc](https://www.javadoc.io/doc/org.jline/jline/latest/index.html)

## License

JLine is licensed under the [BSD License](https://opensource.org/licenses/BSD-3-Clause).

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
