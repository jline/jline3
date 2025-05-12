---
sidebar_position: 3
---

# Line Reading

The `LineReader` is one of JLine's core components, providing sophisticated line editing capabilities for your command-line applications. It allows reading lines from a terminal, with full input/line editing.

## Creating a LineReader

Use the `LineReaderBuilder` to create a `LineReader` instance:

import CodeSnippet from '@site/src/components/CodeSnippet';

```java
Terminal terminal = ...;
LineReader lineReader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .build();
```

<CodeSnippet name="LineReaderCreationExample" />

## LineReader Building Options

The builder can be customized to create more suitable line readers:

```java
LineReader lineReader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .completer(new MyCompleter())
                        .highlighter(new MyHighlighter())
                        .parser(new MyParser())
                        .build();
```

Here are the main options available:

| Option | Description |
|--------|-------------|
| terminal | The `Terminal` to use |
| appName | The application name |
| variables | A `Map<String, Object>` containing variables |
| completer | The `Completer` component to use |
| history | The `History` component to use |
| highlighter | The `Highlighter` component to use |
| parser | The `Parser` component to use |
| expander | The `Expander` component to use |

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

## Simple Line Reading

For REPL style programs, you can use a simple loop:

```java
LineReader reader = LineReaderBuilder.builder().build();
String prompt = ...;
while (true) {
    String line = null;
    try {
        line = reader.readLine(prompt);
    } catch (UserInterruptException e) {
        // Ignore
    } catch (EndOfFileException e) {
        return;
    }
    // Process the line
    ...
}
```

## Various ReadLine Methods

There are a few overridden `readLine` methods that take various parameters, depending on the use cases. They all delegate to the most generic one which is:

```java
String readLine(String prompt, String rightPrompt, Character mask, String buffer) throws UserInterruptException, EndOfFileException;
```

- `prompt` is the unexpanded prompt pattern that will be displayed to the user on the left of the input line
- `rightPrompt` is the unexpanded prompt pattern that will be displayed on the right of the first input line
- `mask` is the character used to hide user input (when reading a password for example)
- `buffer` is the initial content of the input line

## Widgets and Key Mapping

JLine has a couple of built-in widgets: `AutoPairWidgets`, `AutosuggestionWidgets` and `TailTipWidgets`.

You can create custom widgets and bind them to keystrokes:

```java
import static org.jline.keymap.KeyMap.ctrl;
import static org.jline.keymap.KeyMap.alt;
import org.jline.reader.LineReader;
import org.jline.reader.Reference;

public static class MyWidgets extends org.jline.widget.Widgets {

    public MyWidgets(LineReader reader) {
        super(reader);
        addWidget("test-widget", this::testWidget);
        getKeyMap().bind(new Reference("test-widget"), alt(ctrl('X')));
    }

    public boolean testWidget() {
        try {
            String name = buffer().toString().split("\\s+")[0];
            reader.callWidget(name);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
```

## Simple Line Reading

For REPL style programs, you can use a simple loop:

<CodeSnippet name="SimpleLineReadingExample" />

## Various ReadLine Methods

There are a few overridden `readLine` methods that take various parameters, depending on the use cases. They all delegate to the most generic one which is:

```java
String readLine(String prompt, String rightPrompt, Character mask, String buffer) throws UserInterruptException, EndOfFileException;
```

- `prompt` is the unexpanded prompt pattern that will be displayed to the user on the left of the input line
- `rightPrompt` is the unexpanded prompt pattern that will be displayed on the right of the first input line
- `mask` is the character used to hide user input (when reading a password for example)
- `buffer` is the initial content of the input line

## Widgets and Key Mapping

JLine has a couple of built-in widgets: `AutoPairWidgets`, `AutosuggestionWidgets` and `TailTipWidgets`.

You can create custom widgets and bind them to keystrokes:

<CodeSnippet name="CustomWidgetExample" />

You can also create a custom widgets class that extends the base `Widgets` class:

<CodeSnippet name="CustomWidgetsClassExample" />

## Best Practices

- Always close the terminal when your application exits
- Use try-with-resources for automatic resource management
- Configure history appropriately for your application
- Consider using a parser for complex command syntax
- Provide helpful completion options for better user experience
- Handle exceptions like `UserInterruptException` and `EndOfFileException` appropriately
- Save history on application exit
- Use widgets to extend functionality
