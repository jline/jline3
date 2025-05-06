---
sidebar_position: 10
---

# Widgets

import CodeSnippet from '@site/src/components/CodeSnippet';

JLine provides a powerful widget system that allows you to extend the functionality of the line reader. Widgets are reusable components that can be bound to key combinations and called programmatically. This guide explains how to create and use widgets in your JLine applications.

## Custom Widgets

You can create custom widgets by implementing the `Widget` interface:

<CodeSnippet name="CustomWidgetExample" />

In this example, we create two custom widgets: one that inserts the current date and time, and another that converts the current word to uppercase. We then bind these widgets to key combinations (Alt+T and Alt+U).

## Widget Chains

Widgets can call other widgets, allowing you to create complex operations by chaining simpler ones:

<CodeSnippet name="WidgetChainExample" />

This example demonstrates how to create widgets that chain multiple operations together. The `formatTextWidget` calls several built-in widgets and adds its own functionality, while the `quoteTextWidget` manipulates the buffer directly.

## Built-in Widgets

JLine provides many built-in widgets for common operations. You can access these widgets using constants defined in the `LineReader` class:

```java
// Call a built-in widget
reader.callWidget(LineReader.BEGINNING_OF_LINE);
reader.callWidget(LineReader.END_OF_LINE);
reader.callWidget(LineReader.KILL_LINE);
reader.callWidget(LineReader.BACKWARD_KILL_WORD);
reader.callWidget(LineReader.YANK);
reader.callWidget(LineReader.YANK_POP);
reader.callWidget(LineReader.COMPLETE);
reader.callWidget(LineReader.HISTORY_SEARCH_BACKWARD);
```

## Widget Registry

All widgets are stored in a registry that you can access through the `LineReader`:

```java
// Get the widget registry
Map<String, Widget> widgets = reader.getWidgets();

// Register a custom widget
widgets.put("my-widget", myWidget);

// Get a widget by name
Widget widget = widgets.get("my-widget");

// Call a widget by name
reader.callWidget("my-widget");
```

## Key Bindings

To make widgets accessible to users, you can bind them to key combinations:

```java
// Get the main key map
KeyMap<Binding> keyMap = reader.getKeyMaps().get(LineReader.MAIN);

// Bind a widget to a key combination
keyMap.bind(myWidget, KeyMap.alt('m'));
keyMap.bind(myWidget, KeyMap.ctrl('X'));
keyMap.bind(myWidget, "\\e[24~");  // F12 key
```

## Best Practices

When working with widgets in JLine, keep these best practices in mind:

1. **Reusability**: Design widgets to be reusable and composable.

2. **Error Handling**: Handle errors gracefully within your widgets to avoid crashing the application.

3. **Buffer Manipulation**: Be careful when manipulating the buffer directly, as it can lead to unexpected behavior.

4. **Key Bindings**: Choose key bindings that are intuitive and don't conflict with existing bindings.

5. **Documentation**: Document your widgets and their key bindings for users.

6. **Testing**: Test your widgets thoroughly to ensure they behave as expected.

7. **Performance**: Keep your widgets efficient, especially if they're called frequently.

8. **Accessibility**: Consider users who may be using screen readers or other assistive technologies.

Widgets are a powerful way to extend the functionality of JLine and provide a more customized experience for your users. By creating and combining widgets, you can build sophisticated command-line interfaces that are both powerful and user-friendly.
