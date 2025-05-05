---
sidebar_position: 9
---

# Unicode Support

import CodeSnippet from '@site/src/components/CodeSnippet';

JLine provides robust support for Unicode characters, allowing you to create applications that work with international text, symbols, and emoji. This guide explains how to use Unicode in your JLine applications.

## Unicode Input

JLine can handle Unicode input from the user:

<CodeSnippet name="UnicodeInputExample" />

This example demonstrates how to create a terminal with UTF-8 encoding and read Unicode input from the user.

## Unicode Output

JLine can also display Unicode characters in the terminal:

<CodeSnippet name="UnicodeOutputExample" />

This example shows how to display various Unicode characters, including emoji, box drawing characters, math symbols, and international text.

## Terminal Encoding

To ensure proper Unicode support, you should specify the encoding when creating a terminal:

```java
Terminal terminal = TerminalBuilder.builder()
        .encoding("UTF-8")
        .build();
```

Most modern terminals support UTF-8, but some older terminals or environments may have limited Unicode support.

## Unicode Width

One challenge with Unicode is that some characters, particularly East Asian characters and emoji, can take up more than one column in the terminal. JLine provides utilities to handle this:

```java
import org.jline.utils.WCWidth;

// Get the display width of a character
int width = WCWidth.wcwidth('你');  // Returns 2 for most East Asian characters
int emojiWidth = WCWidth.wcwidth('😀');  // Returns 2 for most emoji
```

## Best Practices

When working with Unicode in JLine, keep these best practices in mind:

1. **Always Specify Encoding**: Always specify UTF-8 encoding when creating a terminal to ensure proper Unicode support.

2. **Character Width**: Be aware that some Unicode characters take up more than one column in the terminal.

3. **Terminal Compatibility**: Not all terminals support all Unicode characters. Test your application in different environments.

4. **Font Support**: The display of Unicode characters depends on the font used by the terminal. Some fonts may not support all characters.

5. **Fallbacks**: Provide fallbacks for environments with limited Unicode support.

6. **Input Validation**: Validate Unicode input to ensure it meets your application's requirements.

7. **Testing**: Test your application with a variety of Unicode characters to ensure it behaves as expected.

8. **Accessibility**: Consider users who may be using screen readers or other assistive technologies that may have difficulty with certain Unicode characters.

By properly handling Unicode, you can create command-line applications that work well for users around the world and provide a rich, expressive interface.
