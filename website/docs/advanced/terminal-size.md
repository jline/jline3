---
sidebar_position: 8
---

# Terminal Size

import CodeSnippet from '@site/src/components/CodeSnippet';

JLine provides facilities for detecting and responding to changes in the terminal size. This is essential for creating responsive terminal applications that adapt to different terminal dimensions.

## Getting Terminal Size

You can get the current terminal size using the `getSize()` method of the `Terminal` class:

<CodeSnippet name="TerminalSizeHandlingExample" />

This example demonstrates how to get the initial terminal size and how to respond to terminal resize events by redrawing a box that adapts to the new dimensions.

## Creating Responsive UIs

For more complex applications, you can create responsive UIs that adapt to different terminal sizes:

<CodeSnippet name="ResponsiveUIExample" />

This example shows how to create a UI that changes its layout based on the available width, similar to responsive web design.

## Best Practices

When handling terminal size in JLine, keep these best practices in mind:

1. **Always Check Size**: Always check the terminal size before drawing content to avoid unexpected wrapping or scrolling.

2. **Handle Resize Events**: Register a handler for the `WINCH` signal to detect terminal resize events.

3. **Responsive Design**: Design your UI to adapt to different terminal sizes, not just a fixed size.

4. **Minimum Size**: Define a minimum terminal size for your application and provide a graceful fallback for smaller terminals.

5. **Efficient Redrawing**: When redrawing after a resize, only update what's necessary to avoid flickering.

6. **User Feedback**: Provide feedback if the terminal is too small for your application to function properly.

7. **Testing**: Test your application with different terminal sizes to ensure it behaves as expected.

8. **Accessibility**: Consider users who may be using terminals with unusual dimensions or constraints.

By properly handling terminal size, you can create command-line applications that provide a good user experience across a wide range of terminal emulators and window sizes.
