---
sidebar_position: 2
---

# JLine Architecture

import CodeSnippet from '@site/src/components/CodeSnippet';

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

## Remote Terminals

JLine provides support for remote terminal connections through its `remote-telnet` and `remote-ssh` modules, allowing you to create networked terminal applications accessible via Telnet or SSH protocols.

### Telnet Support

The `remote-telnet` module provides a simple Telnet server implementation that can be used to expose JLine-based applications over the network:

```
┌─────────────────────────────────────────────────────────────────┐
│                      Telnet Architecture                        │
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Telnet Server                            │
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │ Connection  │  │ Connection  │  │ Connection  │             │
│  │  Manager    │  │    Data     │  │             │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Remote Terminal                            │
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐                               │
│  │  Terminal   │  │ Shell       │                               │
│  │  Builder    │  │ Provider    │                               │
│  └─────────────┘  └─────────────┘                               │
└─────────────────────────────────────────────────────────────────┘
```

Key components:

- **Telnet**: Main class that sets up the Telnet server
- **ConnectionManager**: Manages client connections and sessions
- **Connection**: Represents a single client connection
- **ShellProvider**: Interface for providing a shell to connected clients

The Telnet server creates a new `Terminal` instance for each client connection, allowing remote users to interact with your application as if they were using a local terminal.

### SSH Support

The `remote-ssh` module provides SSH server and client capabilities, offering a more secure alternative to Telnet:

- **Server**: Allows remote users to connect to your application via SSH
- **Client**: Enables your application to connect to remote SSH servers
- **SCP/SFTP**: Support for secure file transfer protocols

SSH support is built on Apache MINA SSHD and provides a more secure option for production environments.

### Common Use Cases

- **Remote Administration**: Allow administrators to manage applications remotely
- **Multi-User Applications**: Create applications that can be accessed by multiple users simultaneously
- **Network Services**: Implement network services with interactive terminal interfaces
- **Embedded Systems**: Provide terminal access to devices with limited local I/O capabilities

## Common Usage Patterns

### Basic Terminal and LineReader

<CodeSnippet name="BasicTerminalAndLineReader" />

### Adding Tab Completion

<CodeSnippet name="AddingTabCompletion" />

### Using History

<CodeSnippet name="UsingHistory" />

## Conclusion

JLine's architecture provides a flexible and powerful foundation for building command-line applications. By understanding how the components interact, you can leverage JLine's capabilities to create sophisticated terminal interfaces.

For more detailed information about each component, refer to the specific documentation pages.
