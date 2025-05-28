---
sidebar_position: 3
---

# Builtins Module

import CodeSnippet from '@site/src/components/CodeSnippet';

The JLine Builtins module provides a set of ready-to-use components and utilities that can be integrated into your command-line applications. These components include file operations, table formatting, widgets, and more.

## File Operations

The Builtins module includes commands for common file operations:

<CodeSnippet name="FileOperationsExample" />

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
