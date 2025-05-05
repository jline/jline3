---
sidebar_position: 3
---

# Line Reading

The `LineReader` is one of JLine's core components, providing sophisticated line editing capabilities for your command-line applications.

## Creating a LineReader

Use the `LineReaderBuilder` to create a `LineReader` instance:

import CodeSnippet from '@site/src/components/CodeSnippet';

<CodeSnippet name="LineReaderCreationExample" />

## Reading Input

The basic method for reading input is `readLine()`:

<CodeSnippet name="LineReaderInputExample" />

## Line Reader Options

JLine's `LineReader` supports numerous options to customize behavior:

<CodeSnippet name="LineReaderOptionsExample" />

Common options include:

- `CASE_INSENSITIVE`: Case-insensitive completion
- `AUTO_FRESH_LINE`: Automatically add a newline if the cursor isn't at the start of a line
- `HISTORY_BEEP`: Beep when navigating past the end of history
- `HISTORY_IGNORE_DUPS`: Don't add duplicate entries to history
- `HISTORY_IGNORE_SPACE`: Don't add entries starting with space to history
- `MENU_COMPLETE`: Cycle through completions on tab

## Customizing Prompts

JLine supports rich prompt customization:

<CodeSnippet name="CustomPromptExample" />

## Handling Special Keys

You can customize how the `LineReader` responds to key presses:

<CodeSnippet name="KeyBindingExample" />

## Line Editing Features

JLine's `LineReader` provides numerous line editing features:

- **Navigation**: Move by character, word, or line
- **Editing**: Insert, delete, cut, paste, transpose
- **History**: Navigate, search, and filter command history
- **Completion**: Tab completion with customizable behavior
- **Search**: Incremental search through current line or history

## Advanced Usage

### Multi-line Input

JLine supports multi-line input with proper continuation:

<CodeSnippet name="MultiLineInputExample" />

### Custom Highlighters

JLine can highlight input as it's typed:

<CodeSnippet name="HighlighterExample" />

## Best Practices

- Always close the terminal when your application exits
- Use try-with-resources for automatic resource management
- Configure history appropriately for your application
- Consider using a parser for complex command syntax
- Provide helpful completion options for better user experience
