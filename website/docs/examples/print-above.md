---
sidebar_position: 1
---

# Print Above Examples

JLine provides powerful features for printing text above the current input line. This is particularly useful for displaying asynchronous information without disrupting the user's input.

## Basic Print Above Example

The simplest way to print above the current line is to use the `printAbove` method of the `LineReader` class:

import CodeSnippet from '@site/src/components/CodeSnippet';

<CodeSnippet name="PrintAboveExample" />

In this example, notifications will appear above the input line, and the user can continue typing without interruption.

## Using PrintAboveWriter

For more control, you can use the `PrintAboveWriter` class:

<CodeSnippet name="PrintAboveWriterExample" />

The `PrintAboveWriter` class provides a standard `PrintWriter` interface, making it easy to integrate with existing code that expects a `PrintWriter`.

## Best Practices

When using these features, keep these best practices in mind:

1. **Use sparingly**: Too many messages can be distracting.
2. **Keep messages concise**: Long messages may wrap and take up too much screen space.
3. **Consider styling**: Use colors and styles to differentiate types of messages.
4. **Flush the writer**: Always call `flush()` after writing to ensure the message is displayed immediately.
5. **Thread safety**: Access to the terminal should be synchronized if multiple threads are writing to it.
