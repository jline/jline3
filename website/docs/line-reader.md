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

```java title="LineReaderOptionsExample.java"
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;

public class LineReaderOptionsExample {
    public LineReader configureOptions(Terminal terminal) {
        // highlight-start
        // Configure options during creation
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .option(LineReader.Option.CASE_INSENSITIVE, true)
                .option(LineReader.Option.AUTO_REMOVE_SLASH, true)
                .build();
        // highlight-end

        // Or set options after creation
        reader.setOpt(LineReader.Option.HISTORY_IGNORE_DUPS);
        reader.unsetOpt(LineReader.Option.HISTORY_BEEP);

        return reader;
    }
}
```

Common options include:

- `CASE_INSENSITIVE`: Case-insensitive completion
- `AUTO_FRESH_LINE`: Automatically add a newline if the cursor isn't at the start of a line
- `HISTORY_BEEP`: Beep when navigating past the end of history
- `HISTORY_IGNORE_DUPS`: Don't add duplicate entries to history
- `HISTORY_IGNORE_SPACE`: Don't add entries starting with space to history
- `MENU_COMPLETE`: Cycle through completions on tab

## Customizing Prompts

JLine supports rich prompt customization:

```java title="CustomPromptExample.java"
import org.jline.reader.LineReader;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

public class CustomPromptExample {
    public void demonstratePrompts(LineReader reader) {
        // Simple text prompt
        String line1 = reader.readLine("simple> ");

        // highlight-start
        // Colored prompt (ANSI escape sequences)
        String line2 = reader.readLine("\u001B[1;32msimple>\u001B[0m ");
        // highlight-end

        // Dynamic prompt
        Supplier<String> timePrompt = () -> {
            LocalTime now = LocalTime.now();
            return now.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "> ";
        };
        String line3 = reader.readLine(timePrompt);

        System.out.printf("Lines entered: %s, %s, %s%n", line1, line2, line3);
    }
}
```

## Handling Special Keys

You can customize how the `LineReader` responds to key presses:

```java title="KeyBindingExample.java" showLineNumbers
import org.jline.keymap.Binding;
import org.jline.keymap.KeyMap;
import org.jline.reader.LineReader;
import org.jline.reader.Reference;

public class KeyBindingExample {
    public void customizeKeyBindings(LineReader reader) {
        // Create a custom key map
        KeyMap<Binding> keyMap = reader.getKeyMaps().get(LineReader.MAIN);

        // Bind a key to a widget
        keyMap.bind(new Reference("clear-screen"), KeyMap.ctrl('L'));

        // highlight-start
        // Bind a key to a custom action
        keyMap.bind(
            () -> {
                System.out.println("Custom action executed!");
                return true;
            },
            KeyMap.alt('X')
        );
        // highlight-end

        System.out.println("Key bindings configured");
    }
}
```

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

```java title="MultiLineInputExample.java"
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;

public class MultiLineInputExample {
    public String readMultiLineInput(Terminal terminal) {
        // highlight-start
        // Configure multi-line support
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(new DefaultParser())
                .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%M> ")
                .build();
        // highlight-end

        System.out.println("Enter a multi-line input (e.g., with unclosed quotes or brackets):");
        // Read multi-line input
        String multiLine = reader.readLine("multi> ");

        return multiLine;
    }
}
```

### Custom Validators

You can validate input before accepting it:

```java title="ValidatorExample.java"
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ValidationException;
import org.jline.reader.Validator;
import org.jline.terminal.Terminal;

public class ValidatorExample {
    public LineReader createValidatingReader(Terminal terminal) {
        // Create a validator
        Validator validator = line -> {
            // error-start
            if (line.isEmpty()) {
                throw new ValidationException("Input cannot be empty");
            }
            // error-end

            if (line.length() < 3) {
                throw new ValidationException("Input must be at least 3 characters");
            }
        };

        // Use the validator
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .validator(validator)
                .build();

        return reader;
    }
}
```

### Custom Highlighters

JLine can highlight input as it's typed:

```java title="HighlighterExample.java"
import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.List;

public class HighlighterExample {
    public LineReader createHighlightingReader(Terminal terminal) {
        // highlight-start
        // Create a highlighter
        Highlighter highlighter = (reader, buffer, list) -> {
            AttributedString highlighted = new AttributedStringBuilder()
                    .append(buffer.toString(), AttributedStyle.BOLD)
                    .toAttributedString();
            list.add(highlighted);
            return highlighted;
        };
        // highlight-end

        // Use the highlighter
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .highlighter(highlighter)
                .build();

        return reader;
    }
}
```

## Best Practices

- Always close the terminal when your application exits
- Use try-with-resources for automatic resource management
- Configure history appropriately for your application
- Consider using a parser for complex command syntax
- Provide helpful completion options for better user experience
