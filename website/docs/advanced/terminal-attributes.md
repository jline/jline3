---
sidebar_position: 11
---

# Terminal Attributes

import CodeSnippet from '@site/src/components/CodeSnippet';

JLine provides access to low-level terminal attributes through the `Attributes` class. This guide explains how to use these attributes to control the behavior of the terminal.

## Basic Terminal Attributes

The `Attributes` class provides access to various terminal attributes:

<CodeSnippet name="TerminalAttributesExample" />

This example demonstrates how to get and set terminal attributes, and how to switch between canonical and raw modes.

## Input Flags

Input flags control how input is processed by the terminal:

<CodeSnippet name="InputFlagsExample" />

This example shows how to access and display the current input flags.

## Output Flags

Output flags control how output is processed by the terminal:

<CodeSnippet name="OutputFlagsExample" />

This example shows how to access and display the current output flags.

## Control Characters

Control characters define special characters that have specific meanings to the terminal:

<CodeSnippet name="ControlCharsExample" />

This example shows how to access and display the current control characters.

## Terminal Modes

Terminal modes define how the terminal processes input and output:

<CodeSnippet name="TerminalModesExample" />

This example demonstrates how to switch between canonical and raw modes.

## Custom Terminal Behavior

You can customize the terminal's behavior by setting specific attributes:

<CodeSnippet name="CustomTerminalBehaviorExample" />

This example shows how to customize the terminal's behavior by setting specific attributes.

## Common Terminal Modes

### Canonical Mode

In canonical mode, input is processed line by line, and line editing is enabled:

```java
Attributes attrs = terminal.getAttributes();
attrs.setInputFlag(InputFlag.ICANON, true);
terminal.setAttributes(attrs);
```

### Raw Mode

In raw mode, input is processed character by character, and line editing is disabled:

```java
Attributes attrs = terminal.getAttributes();
attrs.setInputFlag(InputFlag.ICANON, false);
attrs.setInputFlag(InputFlag.ECHO, false);
attrs.setInputFlag(InputFlag.ISIG, false);
attrs.setControlChar(ControlChar.VMIN, 1);
attrs.setControlChar(ControlChar.VTIME, 0);
terminal.setAttributes(attrs);
```

## Best Practices

When working with terminal attributes in JLine, keep these best practices in mind:

1. **Save and Restore**: Always save the original attributes before making changes, and restore them when you're done.

2. **Error Handling**: Handle exceptions that may occur when getting or setting attributes.

3. **Terminal Capabilities**: Be aware that not all terminals support all attributes.

4. **Documentation**: Document any changes you make to terminal attributes, as they can significantly affect the user experience.

5. **Testing**: Test your application with different terminal emulators to ensure it behaves as expected.

6. **User Experience**: Consider the user experience when changing terminal attributes, and provide clear feedback about the current mode.

7. **Graceful Degradation**: Provide fallbacks for terminals that don't support certain attributes.

8. **Performance**: Be mindful of performance when frequently changing terminal attributes.

Terminal attributes provide powerful control over the terminal's behavior, but they should be used carefully to ensure a good user experience.
