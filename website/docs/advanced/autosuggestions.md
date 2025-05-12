---
sidebar_position: 14
---

# Autosuggestions

import CodeSnippet from '@site/src/components/CodeSnippet';

JLine provides powerful autosuggestion capabilities that can enhance the user experience by suggesting commands as they type. This page explains how to use autosuggestions in your JLine applications.

## AutosuggestionWidgets

Fish-like autosuggestions for JLine. `AutosuggestionWidgets` suggests commands as you type based on command history.

<CodeSnippet name="AutosuggestionWidgetsExample" />

### Usage

As you type commands, you will see a completion offered after the cursor in a muted gray color.

If you press the → key (`forward-char` widget) or End (`end-of-line` widget) with the cursor at the end of the buffer, it will accept the suggestion, replacing the contents of the command line buffer with the suggestion.

If you invoke the `forward-word` widget, it will partially accept the suggestion up to the point that the cursor moves to.

### Key Bindings

This plugin provides `autosuggest-toggle` widget that toggles between enabled/disabled suggestions.

## TailTipWidgets

`TailTipWidgets` suggests commands as you type based on command completer data and/or command arguments and options descriptions.

<CodeSnippet name="TailTipWidgetsExample" />

### Usage

As you type commands, you will see a completion offered after the cursor in a muted gray color and argument/option description on status pane.

Command line tab completion works as normal.

### Configuration

Description pane can be disabled by setting `descriptionSize = 0`. Suggestions type can be configured using constructor parameter `tipType`:

1. `COMPLETER` - Tab completions are displayed below command line. No suggestions are displayed after cursor.
2. `TAIL_TIP` - Argument suggestions are displayed after cursor. No tab completions are displayed.
3. `COMBINED` - Argument suggestions are shown if available otherwise tab completions are displayed.

The maximum number of completer suggestions displayed can be controlled by JLine variable `list-max`.

For example, creating TailTipWidgets as:

```java
TailTipWidgets tailtipWidgets = new TailTipWidgets(reader, tailTips, 0, TipType.TAIL_TIP);
```

you will obtain Redis-like suggestions.

### Key Bindings

This plugin provides two widgets:

1. `tailtip-toggle` - Toggles between enabled/disabled suggestions.
2. `tailtip-window` - Toggles tailtip description pane.

For example, this would bind alt + w to toggle tailtip description pane:

```java
reader.getKeyMaps().get(LineReader.MAIN).bind(new Reference("tailtip-window"), KeyMap.alt('w'));
```

## Best Practices

When using autosuggestions in JLine, consider these best practices:

1. **Selective Enabling**: Enable autosuggestions only when they add value to the user experience.

2. **Performance**: Be mindful of performance, especially with large history files or complex completers.

3. **Visual Distinction**: Ensure that suggestions are visually distinct from user input.

4. **User Control**: Always provide a way for users to toggle autosuggestions on/off.

5. **Combine with History**: Autosuggestions work best when combined with a well-maintained history.

6. **Context Awareness**: Use TailTipWidgets for context-aware suggestions based on command syntax.

7. **Documentation**: Document the autosuggestion features and key bindings for your users.

8. **Testing**: Test autosuggestions with various input patterns to ensure they behave as expected.
