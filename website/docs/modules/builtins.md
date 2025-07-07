---
sidebar_position: 3
---

# Builtins Module

import CodeSnippet from '@site/src/components/CodeSnippet';

The JLine Builtins module provides a set of ready-to-use components and utilities that can be integrated into your command-line applications. These components include POSIX commands, file operations, table formatting, widgets, and more.

## POSIX Commands

The Builtins module includes implementations of common POSIX commands that can be used in your applications:

<CodeSnippet name="FileOperationsExample" />

### Available Commands

The `PosixCommands` class provides the following commands:

#### Core File Operations
- **cat** - concatenate and print files with line numbering support
- **head** - display first lines of files (configurable line/byte count)
- **tail** - display last lines of files with follow mode support
- **less** - pager for viewing files with navigation and search
- **nano** - text editor with nano-like interface

#### Text Processing
- **echo** - display text with newline control
- **grep** - search text patterns with regular expressions, case-insensitive matching, and color highlighting
- **sort** - sort lines of text with field-based sorting, numeric sorting, and reverse ordering
- **wc** - word, line, character, and byte count

#### Directory Operations
- **ls** - list directory contents with color support, long format, sorting options, and file type indicators
- **pwd** - print working directory
- **cd** - change directory with POSIX-compliant options (-P, -L)

#### System Utilities
- **date** - display or format date and time with ISO 8601, RFC 2822, and RFC 3339 support
- **sleep** - suspend execution for a specified time
- **clear** - clear terminal screen
- **watch** - execute commands repeatedly and display output
- **ttop** - thread monitoring with real-time updates

#### Advanced Features

Each command includes:
- **POSIX compliance** - Standard options and behaviors
- **Help system** - Built-in `--help` for all commands
- **Error handling** - Comprehensive error messages
- **Color support** - Configurable color schemes where applicable
- **Streaming support** - Efficient handling of large files

### Using POSIX Commands

You can use POSIX commands in several ways:

1. **Direct method calls** - Call individual command methods directly
2. **PosixCommandsRegistry** - Use the registry for easier command management
3. **Integration with command frameworks** - Integrate with existing command systems

Each command supports standard POSIX options and provides help via the `--help` flag.

### Command Examples

Here are some examples of using the POSIX commands:

```java
// Create a context for command execution
PosixCommands.Context context = new PosixCommands.Context(
    System.in, System.out, System.err,
    Paths.get("."), terminal, key -> System.getProperty(key)
);

// List files with long format and colors
PosixCommands.ls(context, new String[]{"ls", "-l", "--color=always"});

// Search for patterns in files
PosixCommands.grep(context, new String[]{"grep", "-n", "-i", "pattern", "file.txt"});

// Sort file contents numerically
PosixCommands.sort(context, new String[]{"sort", "-n", "numbers.txt"});

// Display date in ISO format
PosixCommands.date(context, new String[]{"date", "-I"});

// Watch command output every 2 seconds
CommandExecutor executor = command -> {
    // Your command execution logic
    return "command output";
};
PosixCommands.watch(context, new String[]{"watch", "-n", "2", "command"}, executor);
```

### Integration with Shell Environments

The POSIX commands are designed to integrate seamlessly with shell environments:

```java
// Example integration with Apache Felix Gogo
protected void ls(CommandSession session, Process process, String[] argv) {
    Map<String, String> colorMap = getColorMap(session);
    PosixCommands.ls(createPosixContext(session, process), argv, colorMap);
}

// Example cd command with directory changing
protected void cd(CommandSession session, Process process, String[] argv) {
    Consumer<Path> directoryChanger = path -> session.currentDir(path);
    PosixCommands.cd(createPosixContext(session, process), argv, directoryChanger);
}
```

### POSIX Commands Registry

For easier integration, you can use the `PosixCommandsRegistry` class:

<CodeSnippet name="PosixCommandsRegistryExample" />

## System Registry

The SystemRegistry provides a framework for registering and executing commands:

<CodeSnippet name="SystemRegistryExample" />

## Nano Editor

The Builtins module includes a Nano-like text editor:

<CodeSnippet name="NanoEditorExample" />

JLine provides [support for customizing `nano`](advanced/nano-less-customization.md).

## InputRC

The `InputRC` class can configure a `LineReader` from an `.inputrc` style file.

## Best Practices

When using the JLine Builtins module, consider these best practices:

1. **Use Built-in Commands**: Leverage the built-in commands for common operations instead of implementing them yourself.

2. **Customize Tables**: Customize table formatting to match your application's style.

3. **Leverage Widgets**: Use widgets like TailTipWidgets to enhance the user experience.

4. **Extend SystemRegistry**: Extend the SystemRegistry to create a comprehensive command framework.

5. **Provide Help**: Include help information for all commands to make your application more user-friendly.

6. **Consistent Command Syntax**: Maintain a consistent command syntax across your application.

7. **Error Handling**: Implement proper error handling for all commands.

8. **Tab Completion**: Configure tab completion for all commands to improve usability.

9. **Documentation**: Document all commands and their options.

10. **Testing**: Test all commands thoroughly to ensure they work as expected.
