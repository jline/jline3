---
sidebar_position: 5
---

# Attributed Strings

import CodeSnippet from '@site/src/components/CodeSnippet';

JLine provides a powerful system for creating and manipulating styled text through its `AttributedString` and related classes. This guide explains how to use these classes to create rich, colorful text in your terminal applications.

## AttributedString Basics

The `AttributedString` class represents a string with style attributes. You can create an `AttributedString` with a specific style:

<CodeSnippet name="AttributedStringBasicsExample" />

This example demonstrates creating `AttributedString` objects with various styles and displaying them in the terminal.

## AttributedStyle

The `AttributedStyle` class represents the style attributes that can be applied to text. It includes foreground and background colors, as well as text attributes like bold, italic, and underline:

<CodeSnippet name="AttributedStyleExample" />

This example shows the various style attributes available in JLine.

## AttributedStringBuilder

For more complex styling needs, you can use the `AttributedStringBuilder` class, which allows you to build an `AttributedString` incrementally:

<CodeSnippet name="AttributedStringBuilderExample" />

This example demonstrates how to use `AttributedStringBuilder` to create a string with different styles for different parts of the text.

## Styling Specific Sections

You can apply different styles to specific sections of text:

<CodeSnippet name="StylingSpecificSectionsExample" />

This example shows how to style different parts of a command line with different colors.

## Color Support

JLine supports both 8-color and 256-color modes, depending on the capabilities of the terminal:

```java
// 8-color mode (standard ANSI colors)
AttributedStyle redFg = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED);
AttributedStyle greenFg = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
AttributedStyle blueFg = AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE);

// 256-color mode
AttributedStyle color123 = AttributedStyle.DEFAULT.foreground(123);
AttributedStyle color45 = AttributedStyle.DEFAULT.foreground(45);
```

## Best Practices

When using attributed strings in JLine, keep these best practices in mind:

1. **Terminal Capabilities**: Check the terminal's capabilities before using advanced styling features.

2. **Color Accessibility**: Choose colors that work well on both light and dark backgrounds, or adapt based on the terminal's background color.

3. **Consistent Styling**: Use consistent styling throughout your application for similar elements.

4. **Style Reuse**: Define common styles as constants to ensure consistency and reduce code duplication.

5. **Performance**: Be mindful of performance when creating many attributed strings, especially in tight loops.

6. **Fallbacks**: Provide fallbacks for terminals that don't support certain styling features.

7. **Testing**: Test your styling on different terminal emulators to ensure it looks good everywhere.

8. **Documentation**: Document your styling conventions for future maintainers.

Attributed strings are a powerful way to enhance the user experience of your command-line applications, making them more visually appealing and easier to use.
