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

### jline-shell

The `jline-shell` module provides a clean, modern API for building interactive command-line applications. It defines:

- `Command` and `AbstractCommand` — the core command abstraction
- `CommandGroup` and `SimpleCommandGroup` — command organization and discovery
- `CommandDispatcher` — command resolution, pipeline execution, and completion
- `Shell` and `Shell.builder()` — a thin REPL loop with a fluent builder API
- `Pipeline` and `PipelineParser` — extensible pipeline parsing and execution (`|`, `&&`, `||`, `;`, `>`, `>>`)
- `JobManager` and `Job` — job control for foreground, background, and suspended commands
- `AliasManager` — command alias system with parameter substitution and optional persistence
- Built-in commands — `history`, `help`, `setopt`/`unsetopt`/`setvar` (opt-in via builder)
- `CommandHighlighter` — command-aware syntax highlighting

See the [jline-shell module page](./shell.md) for full details.

### jline-console

The `jline-console` module provides the legacy framework for building interactive console applications. New applications should prefer `jline-shell`. It includes:

- Command processing infrastructure
- Command registration and discovery
- Argument parsing
- Help generation
- Bridge adapters for interoperability with `jline-shell`

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
   ↑ ↑
   |  jline-shell → jline-console (legacy bridge)
   |
jline-style
    ↑
jline-builtins
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
    <version>4.0.0</version>
</dependency>

<!-- Or individual modules -->
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-builtins</artifactId>
    <version>4.0.0</version>
</dependency>

<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-style</artifactId>
    <version>4.0.0</version>
</dependency>

<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-shell</artifactId>
    <version>%%JLINE_VERSION%%</version>
</dependency>

<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-console</artifactId>
    <version>4.0.0</version>
</dependency>

<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-console-ui</artifactId>
    <version>4.0.0</version>
</dependency>
```

## JPMS Module Status

JLine 4.0+ includes full JPMS support for most modules:

- **✅ JPMS Modules**: `jline-terminal`, `jline-reader`, `jline-style`, `jline-builtins`, `jline-shell`, `jline-console`, `jline-console-ui`, `jline-native`, `jline-terminal-ffm`, `jline-terminal-jni`, `jline-jansi-core`
- **❌ Automatic Modules**: `jline-terminal-jansi`, `jline-groovy`, `jline-remote-ssh`, `jline-remote-telnet`, `jline-demo`, `jline-graal`

For detailed JPMS usage information, see the [JPMS Support](./jpms.md) documentation.

---

The following sections provide detailed documentation for each of these modules.
