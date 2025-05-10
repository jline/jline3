---
sidebar_position: 1
---

# JLine Modules Overview

JLine is organized into several modules, each providing specific functionality. This modular architecture allows you to include only the components you need in your application, reducing dependencies and improving maintainability.

## Core Modules

JLine's core functionality is provided by these essential modules:

- **jline-terminal**: Provides terminal handling capabilities
- **jline-reader**: Implements line reading with editing features

## Additional Modules

Beyond the core functionality, JLine offers several additional modules that extend its capabilities:

### jline-builtins

The `jline-builtins` module provides ready-to-use commands and utilities that you can incorporate into your command-line applications. These include:

- File operations (ls, cat, less, etc.)
- Command history management
- Completion utilities
- Table formatting

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

import VersionDisplay from '@site/src/components/VersionDisplay';

```xml
<!-- Core functionality -->
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline</artifactId>
    <version><VersionDisplay /></version>
</dependency>

<!-- Or individual modules -->
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-builtins</artifactId>
    <version><VersionDisplay /></version>
</dependency>

<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-style</artifactId>
    <version><VersionDisplay /></version>
</dependency>

<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-console</artifactId>
    <version><VersionDisplay /></version>
</dependency>

<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-console-ui</artifactId>
    <version><VersionDisplay /></version>
</dependency>
```

The following sections provide detailed documentation for each of these modules.
