---
sidebar_position: 1
---

# JLine Modules Overview

JLine is organized into several modules, each providing specific functionality. This modular architecture allows you to include only the components you need in your application, reducing dependencies and improving maintainability.

:::info JPMS Support
JLine 4.0+ provides full support for the Java Platform Module System (JPMS). See the [JPMS Support](./jpms.md) page for detailed information about which modules are JPMS-compatible and how to use them in modular applications.
:::

## Core Modules

JLine's core functionality is provided by these essential modules:

- **jline-terminal**: Provides terminal handling capabilities
- **jline-reader**: Implements line reading with editing features

## Additional Modules

Beyond the core functionality, JLine offers several additional modules that extend its capabilities:

### jline-builtins

The `jline-builtins` module provides ready-to-use commands and utilities that you can incorporate into your command-line applications. These include:

- POSIX commands (cat, ls, grep, head, tail, wc, sort, etc.)
- File operations and utilities
- Command history management
- Completion utilities
- Table formatting
- Text editors (Nano)
- Terminal multiplexer (Tmux)
- System monitoring (TTop)

### jline-style

The `jline-style` module provides a styling API for terminal output, allowing you to:

- Define and apply styles to text
- Create color schemes
- Apply styles consistently across your application
- Parse style definitions from configuration files

### jline-console

The `jline-console` module provides a framework for building interactive console applications, including:

- Command processing infrastructure
- Command registration and discovery
- Argument parsing
- Help generation

### jline-console-ui

The `jline-console-ui` module provides interactive prompt components for console applications, inspired by [Inquirer.js](https://github.com/SBoudrias/Inquirer.js). It includes:

- Text input with completion and GNU ReadLine compatible editing
- Checkboxes for multiple selections
- Lists for single item selection
- Expandable choices (key-based answers with help)
- Yes/No confirmation prompts

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

## Maven Dependencies

To use these modules in your Maven project, add the appropriate dependencies:



```xml
<!-- Core functionality -->
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline</artifactId>
    <version>%%JLINE_VERSION%%</version>
</dependency>

<!-- Or individual modules -->
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-builtins</artifactId>
    <version>%%JLINE_VERSION%%</version>
</dependency>

<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-style</artifactId>
    <version>%%JLINE_VERSION%%</version>
</dependency>

<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-console</artifactId>
    <version>%%JLINE_VERSION%%</version>
</dependency>

<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-console-ui</artifactId>
    <version>%%JLINE_VERSION%%</version>
</dependency>
```

## JPMS Module Status

JLine 4.0+ includes full JPMS support for most modules:

- **✅ JPMS Modules**: `jline-terminal`, `jline-reader`, `jline-style`, `jline-builtins`, `jline-console`, `jline-console-ui`, `jline-native`, `jline-terminal-ffm`, `jline-terminal-jna`, `jline-terminal-jni`, `jline-jansi-core`, `jline-curses`
- **❌ Automatic Modules**: `jline-terminal-jansi`, `jline-groovy`, `jline-remote-ssh`, `jline-remote-telnet`, `jline-demo`, `jline-graal`

For detailed JPMS usage information, see the [JPMS Support](./jpms.md) documentation.

---

The following sections provide detailed documentation for each of these modules.
