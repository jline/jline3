---
sidebar_position: 2
---

# JLine Architecture

This page provides a high-level overview of JLine's architecture and how its components interact with each other.

## Component Overview

JLine is organized into several core components that work together to provide a complete terminal handling and line editing solution:

```
┌─────────────────────────────────────────────────────────────────┐
│                        JLine Architecture                       │
└─────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                            Terminal                             │
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────┐ │
│  │    Jansi    │  │     JNA     │  │     FFM     │  │   Exec  │ │
│  │   Provider  │  │   Provider  │  │   Provider  │  │ Provider│ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                           LineReader                            │
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────┐ │
│  │   Parser    │  │  Completer  │  │   History   │  │ Widgets │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Higher-Level APIs                        │
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌────────────┐  ┌──────────┐ │
│  │    Style    │  │   Builtins  │  │  Console   │  │Console UI│ │
│  └─────────────┘  └─────────────┘  └────────────┘  └──────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## Core Components

### Terminal

The `Terminal` component is the foundation of JLine. It provides:

- Access to the underlying terminal device
- Raw mode for character-by-character input
- ANSI escape sequence handling
- Terminal size information
- Signal handling (e.g., window resize, Ctrl+C)

The Terminal layer uses different providers (Jansi, JNA, FFM, etc.) to interact with the native terminal capabilities on different platforms.

### LineReader

The `LineReader` builds on top of the Terminal to provide:

- Line editing capabilities
- History management
- Tab completion
- Key binding
- Widget system for custom functionality
- Multi-line editing
- Syntax highlighting

### Higher-Level APIs

JLine includes several higher-level modules that provide additional functionality:

- **Style**: Styling API for terminal output
- **Builtins**: Ready-to-use commands and utilities
- **Console**: Framework for building interactive console applications
- **Console UI**: UI components like progress bars, tables, and forms

## Data Flow

Here's how data flows through the JLine system:

1. The `Terminal` captures raw input from the user
2. Input is processed through key bindings and widgets
3. The `LineReader` applies editing operations based on the input
4. When the user presses Enter, the `LineReader` returns the completed line
5. The application processes the line and may use higher-level APIs for output

## Module Dependencies

The modules have the following dependency relationships:

```
jline-terminal
    ↑
jline-reader
    ↑
jline-style
    ↑
jline-builtins
    ↑
jline-console
    ↑
jline-console-ui
```

## Key Interfaces

JLine defines several key interfaces that you'll work with:

- `Terminal`: Represents the terminal device
- `LineReader`: Reads lines of input with editing capabilities
- `Completer`: Provides tab completion suggestions
- `History`: Manages command history
- `Parser`: Parses input lines into tokens
- `Widget`: Implements custom functionality for the line reader

## Customization Points

JLine is highly customizable through several extension points:

- **Terminal Providers**: Choose or implement different terminal backends
- **Completers**: Create custom completion logic
- **Widgets**: Add new editing functions
- **Key Bindings**: Map keys to specific actions
- **Highlighters**: Implement syntax highlighting
- **History**: Customize history storage and retrieval

## Common Usage Patterns

### Basic Terminal and LineReader

```java
// Create a terminal
Terminal terminal = TerminalBuilder.builder()
        .system(true)
        .build();

// Create a line reader
LineReader reader = LineReaderBuilder.builder()
        .terminal(terminal)
        .build();

// Read input
String line = reader.readLine("prompt> ");
```

### Adding Tab Completion

```java
// Create a completer
Completer completer = new StringsCompleter("command1", "command2", "help", "quit");

// Create a line reader with completion
LineReader reader = LineReaderBuilder.builder()
        .terminal(terminal)
        .completer(completer)
        .build();
```

### Using History

```java
// Create a history file
Path historyFile = Paths.get(System.getProperty("user.home"), ".myapp_history");

// Create a line reader with history
LineReader reader = LineReaderBuilder.builder()
        .terminal(terminal)
        .variable(LineReader.HISTORY_FILE, historyFile)
        .build();
```

## Conclusion

JLine's architecture provides a flexible and powerful foundation for building command-line applications. By understanding how the components interact, you can leverage JLine's capabilities to create sophisticated terminal interfaces.

For more detailed information about each component, refer to the specific documentation pages.
