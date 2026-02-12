# JLine Prompt

JLine Prompt provides a simple console interface for querying information from the user.
It is inspired by Inquirer.js which is written in JavaScript.

## Features

JLine Prompt currently supports:

- Text input with completion and GNU ReadLine compatible editing
- Checkboxes for multiple selections
- Lists for single item selection
- Expandable choices (multiple key-based answers for a question with help)
- Yes/No confirmation prompts

## Usage

The JLine Prompt API provides a clean, interface-based approach for creating interactive console prompts.

### Basic Example

```java
// Create a Prompter instance
Terminal terminal = TerminalBuilder.builder().build();
Prompter prompter = PrompterFactory.create(terminal);

// Create a prompt builder
PromptBuilder promptBuilder = prompter.newBuilder();

// Add prompts
promptBuilder.createListPrompt()
    .name("choice")
    .message("Choose an option:")
    .newItem("option1")
    .text("Option 1")
    .add()
    .newItem("option2")
    .text("Option 2")
    .add()
    .addPrompt();

// Prompt the user
Map<String, PromptResult> result = prompter.prompt(header, promptBuilder.build());
```

### Supported Prompt Types

- **Input Prompt**: Text input with optional completion and validation
- **Password Prompt**: Masked text input for sensitive data
- **Number Prompt**: Numeric input with range validation and decimal control
- **List Prompt**: Single selection from a list of options
- **Checkbox Prompt**: Multiple selection from a list of options
- **Choice Prompt**: Single selection with keyboard shortcuts
- **Confirm Prompt**: Yes/No confirmation
- **Text Prompt**: Display text without requiring input
- **Search Prompt**: Interactive search with dynamic filtering
- **Editor Prompt**: Full-screen text editor using JLine's built-in Nano editor

### Configuration

The API supports platform-specific defaults:
- On Windows: `>`, `( )`, `(x)`, `( )`
- On other platforms: `❯`, `◯ `, `◉ `, `◯ `

You can customize these by providing your own configuration:

```java
// Use platform defaults
PrompterConfig config = PrompterConfig.defaults();

// Or create a custom configuration
PrompterConfig customConfig = PrompterConfig.custom(
    "->",   // indicator
    "[ ]",  // unchecked box
    "[x]",  // checked box
    "[-]",  // unavailable item
    null,   // style resolver (optional)
    false   // cancellable first prompt
);

Prompter prompter = PrompterFactory.create(terminal, customConfig);
```

## Examples

You can find examples in the `org.jline.prompt.examples` package.

## Migration from jline-console-ui

This module replaces the deprecated `jline-console-ui` module. The new API provides:

- Clean interface-based design
- Better separation of concerns
- Record-style accessor methods
- Improved type safety
- Better documentation

To migrate from the old API, replace imports from `org.jline.consoleui` with `org.jline.prompt`.
