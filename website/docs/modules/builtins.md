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

- **cat** - concatenate and print files
- **echo** - display text
- **grep** - search text patterns with regular expressions
- **ls** - list directory contents with various formatting options
- **pwd** - print working directory
- **head** - display first lines of files
- **tail** - display last lines of files
- **wc** - word, line, character, and byte count
- **date** - display or format date and time
- **sleep** - suspend execution for a specified time
- **sort** - sort lines of text with various options
- **clear** - clear terminal screen

### Using POSIX Commands

You can use POSIX commands in several ways:

1. **Direct method calls** - Call individual command methods directly
2. **PosixCommandsRegistry** - Use the registry for easier command management
3. **Integration with command frameworks** - Integrate with existing command systems

Each command supports standard POSIX options and provides help via the `--help` flag.

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
