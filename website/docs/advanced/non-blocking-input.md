---
sidebar_position: 3
---

# Non-Blocking Input

import CodeSnippet from '@site/src/components/CodeSnippet';

JLine provides support for non-blocking input, which is essential for applications that need to perform other tasks while waiting for user input. This guide explains how to use JLine's non-blocking input features.

## NonBlockingReader

The most direct way to handle non-blocking input is to use JLine's `NonBlockingReader` class, which you can obtain from a Terminal:

<CodeSnippet name="NonBlockingReaderExample" />

In this example, the application continues to perform work (printing dots) while checking for user input. When input is available, it's read and processed immediately.

## Reading with Timeout

You can also read with a timeout, which will block for up to the specified time:

<CodeSnippet name="TimeoutReadExample" />

This approach is useful when you want to wait for input for a limited time before continuing with other tasks.

## Non-Blocking LineReader

While the `LineReader` class is primarily designed for blocking line input, you can use it in a non-blocking way by running it in a separate thread:

<CodeSnippet name="NonBlockingLineReaderExample" />

In this example, a background thread continuously updates the display while the main thread handles line input. This approach allows you to maintain the rich line editing capabilities of `LineReader` while still performing background tasks.

## Asynchronous Input Handling

For more complex applications, you might want to handle input asynchronously:

<CodeSnippet name="AsyncInputExample" />

This approach uses a dedicated thread to continuously read and process input, allowing the main application thread to focus on other tasks.

## Using BindingReader After readLine()

When an application alternates between `LineReader.readLine()` and its own input loop using `BindingReader` (or raw `NonBlockingReader`), arrow keys and other special keys may stop working in the custom input loop.

### Why This Happens

`LineReader.readLine()` puts the terminal into **application keypad mode** by sending the `keypad_xmit` (smkx) capability on entry. In this mode, arrow keys send application-mode escape sequences (e.g., `\eOA` for Up). When `readLine()` returns, it sends `keypad_local` (rmkx) to restore normal keypad mode. In normal mode, the same arrow keys send different escape sequences (e.g., `\e[A` for Up).

If your `BindingReader` key map was built from the terminal's application-mode key definitions (which is what `InfoCmp` reports), the sequences won't match after `readLine()` has switched back to normal mode.

### Solution

Send `keypad_xmit` before entering your own input loop, and `keypad_local` when you're done:

```java
// After readLine() returns, before your own input loop:
terminal.puts(Capability.keypad_xmit);
terminal.flush();

BindingReader bindingReader = new BindingReader(terminal.reader());
KeyMap<String> keyMap = new KeyMap<>();
// ... populate key map with bindings ...

try {
    while (true) {
        String binding = bindingReader.readBinding(keyMap);
        if (binding == null) break;
        // process binding...
    }
} finally {
    terminal.puts(Capability.keypad_local);
    terminal.flush();
}
```

This ensures that arrow keys and function keys produce the escape sequences that match your key map definitions.

### When This Matters

This only affects applications that:
1. Use `LineReader.readLine()` for command input, **and**
2. Run their own `BindingReader`-based input loop for a different purpose (e.g., a game, a pager, a custom menu)

If you only use `readLine()`, this is handled automatically.

## Best Practices

When working with non-blocking input in JLine, keep these best practices in mind:

1. **Thread Safety**: Be careful when accessing shared resources from multiple threads. Use synchronization or thread-safe data structures when necessary.

2. **Resource Management**: Always close the terminal when you're done with it, especially when using multiple threads.

3. **Error Handling**: Handle exceptions appropriately, especially `IOException` which can occur during reading.

4. **Graceful Shutdown**: Implement a clean shutdown mechanism to ensure all threads are properly terminated.

5. **User Experience**: Provide clear feedback to the user about what's happening, especially when the application is busy with background tasks.

6. **Performance**: Be mindful of how often you check for input. Checking too frequently can waste CPU resources, while checking too infrequently can make the application feel unresponsive.

7. **Buffering**: Remember that terminal input is often buffered at the OS level, which can affect the responsiveness of your application.

8. **Signal Handling**: Consider how your application will handle signals like SIGINT (Ctrl+C) when using non-blocking input.

Non-blocking input is a powerful feature that can significantly enhance the user experience of your command-line applications, allowing them to remain responsive even while performing complex tasks.
