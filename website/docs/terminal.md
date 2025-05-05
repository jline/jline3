---
sidebar_position: 2
---

# Terminal

import CodeSnippet from '@site/src/components/CodeSnippet';

The Terminal is the foundation of JLine, providing access to the underlying terminal capabilities. This page explains how to create and use Terminal objects in your applications.

## Creating a Terminal

You can create a Terminal using the TerminalBuilder:

<CodeSnippet name="TerminalCreationExample" />

## Terminal Capabilities

You can check the capabilities of a Terminal:

<CodeSnippet name="TerminalCapabilitiesExample" />

## Terminal Output

You can write to the Terminal:

<CodeSnippet name="TerminalOutputExample" />

## Terminal Input

You can read from the Terminal:

<CodeSnippet name="TerminalInputExample" />

## Terminal Signals

You can handle terminal signals:

<CodeSnippet name="TerminalSignalsExample" />

## Terminal Attributes

You can get and set terminal attributes:

<CodeSnippet name="TerminalAttributesExample" />

## Terminal Size

You can get and handle changes to the terminal size:

<CodeSnippet name="TerminalSizeExample" />

## Cursor Control

You can control the cursor position:

<CodeSnippet name="TerminalCursorExample" />

## Terminal Colors

You can use colors in the terminal:

<CodeSnippet name="TerminalColorsExample" />

## Best Practices

When working with Terminal objects in JLine, consider these best practices:

1. **Resource Management**: Always close the Terminal when you're done with it.

2. **Signal Handling**: Handle signals like SIGINT (Ctrl+C) appropriately.

3. **Terminal Attributes**: Save and restore terminal attributes when making changes.

4. **Terminal Size**: Handle terminal resize events to update your UI.

5. **Terminal Capabilities**: Check terminal capabilities before using advanced features.

6. **Error Handling**: Handle exceptions that may occur when creating or using a Terminal.

7. **Terminal Type**: Be aware of the terminal type and adjust your behavior accordingly.

8. **Terminal Input**: Use non-blocking input when appropriate.

9. **Terminal Output**: Flush the output after writing to ensure it's displayed.

10. **Terminal Colors**: Use colors consistently and provide fallbacks for terminals that don't support them.
