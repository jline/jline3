---
sidebar_position: 4
---

# Screen Clearing and Cursor Control

import CodeSnippet from '@site/src/components/CodeSnippet';

JLine provides powerful capabilities for controlling the terminal display, including clearing the screen, moving the cursor, and updating specific portions of the display. This guide explains how to use these features to create more dynamic and interactive terminal applications.

## Clearing the Entire Screen

The most basic screen operation is clearing the entire screen:

<CodeSnippet name="ScreenClearingExample" />

This example uses the `clear_screen` capability to erase all content from the terminal and position the cursor at the top-left corner.

## Partial Screen Clearing

You can also clear only a portion of the screen:

<CodeSnippet name="PartialScreenClearingExample" />

In this example, we move the cursor to a specific position and then clear everything from that position to the end of the screen.

## Clearing a Single Line

For more precise control, you can clear a single line:

<CodeSnippet name="LineClearingExample" />

This example moves the cursor to the beginning of a specific line and then clears that line before writing new content.

## Cursor Movement

JLine provides several capabilities for moving the cursor:

<CodeSnippet name="CursorMovementExample" />

This example demonstrates moving the cursor to absolute positions, as well as saving and restoring the cursor position.

## Raw Mode

For applications that need complete control over the terminal, JLine supports raw mode:

<CodeSnippet name="RawModeExample" />

In raw mode, input is not processed by the terminal driver, giving your application direct access to each keystroke.

## Using the Display Class

For more complex screen management, JLine provides the `Display` class:

<CodeSnippet name="DisplayExample" />

The `Display` class manages a virtual screen buffer and efficiently updates only the parts of the screen that have changed, which can significantly improve performance for complex displays.

## Terminal Capabilities

JLine uses the `InfoCmp.Capability` enum to access terminal capabilities. Here are some commonly used capabilities for screen and cursor control:

| Capability | Description |
|------------|-------------|
| `clear_screen` | Clear the entire screen |
| `clr_eol` | Clear from cursor to end of line |
| `clr_eos` | Clear from cursor to end of screen |
| `cursor_address` | Move cursor to absolute position |
| `cursor_up` | Move cursor up one line |
| `cursor_down` | Move cursor down one line |
| `cursor_right` | Move cursor right one column |
| `cursor_left` | Move cursor left one column |
| `save_cursor` | Save current cursor position |
| `restore_cursor` | Restore previously saved cursor position |
| `enter_bold_mode` | Start bold text |
| `exit_attribute_mode` | Turn off all attributes |

## Best Practices

When working with screen clearing and cursor control in JLine, keep these best practices in mind:

1. **Terminal Compatibility**: Not all terminals support all capabilities. Check for capability support before using it.

2. **Efficient Updates**: Update only the parts of the screen that have changed to improve performance.

3. **Cursor Position**: Always be aware of the current cursor position, especially after clearing portions of the screen.

4. **Terminal Size**: Be mindful of the terminal size when positioning content to avoid unexpected wrapping or scrolling.

5. **Restore State**: When your application exits, restore the terminal to a usable state.

6. **User Experience**: Use screen clearing and cursor control to create a more intuitive and responsive user interface.

7. **Accessibility**: Consider users who may be using screen readers or other assistive technologies.

8. **Performance**: Minimize the number of terminal operations to improve performance, especially over slow connections.

Screen clearing and cursor control are powerful tools for creating dynamic terminal applications. By using these features effectively, you can create more engaging and interactive command-line interfaces.
