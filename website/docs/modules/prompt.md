---
sidebar_position: 6
---

# Prompt Module

import CodeSnippet from '@site/src/components/CodeSnippet';

The JLine Prompt module provides a modern, native JLine3 implementation for interactive console prompts. It offers a complete replacement for the console-ui module with enhanced features including multi-column layouts, advanced navigation, and script integration capabilities.

## Introduction

The Prompt module is a comprehensive prompt system built natively on JLine3, providing all the functionality of console-ui while adding significant enhancements. It features a fluent builder API, multi-column layouts, grid-based navigation, and command-line integration for scripts and automation.

## Key Features

The Prompt module offers:

- **Multi-column layouts** with automatic terminal width adaptation
- **Grid-based navigation** using arrow keys (left/right for columns, up/down for rows)
- **Advanced item support** including disabled items and pre-checked checkboxes
- **Choice prompts** with key-based selection (e.g., 'r' for Red, 'g' for Green)
- **Specialized prompts** for passwords, numbers, toggles, search, editor, and key press
- **Professional display management** with efficient screen updates
- **Pagination and scrolling** for large item lists
- **Script integration** via PromptCommands for shell scripts and automation
- **Terminal capability detection** with graceful fallbacks
- **Native JLine3 implementation** with no external dependencies

## Quick Start Example

Here's a simple example demonstrating the fluent builder API:

<CodeSnippet name="PromptBasicExample" />

## Architecture

The Prompt module uses a factory pattern with fluent builders:

```java
// Create a prompter
Terminal terminal = TerminalBuilder.builder().build();
Prompter prompter = PrompterFactory.create(terminal);

// Build prompts using fluent API
PromptBuilder builder = prompter.newBuilder();
```

## Prompt Types

### List Prompt

Single-selection lists with multi-column layout support:

<CodeSnippet name="PromptListExample" />

### Checkbox Prompt

Multiple-selection checkboxes with pre-checked item support:

<CodeSnippet name="PromptCheckboxExample" />

### Choice Prompt

Key-based selection with character shortcuts:

<CodeSnippet name="PromptChoiceExample" />

### Input Prompt

Text input with default values and masking:

<CodeSnippet name="PromptInputExample" />

### Confirm Prompt

Yes/No confirmation with default values:

<CodeSnippet name="PromptConfirmExample" />

### Password Prompt

Secure password input with configurable masking:

<CodeSnippet name="PromptPasswordExample" />

### Number Prompt

Numeric input with range validation and decimal control:

<CodeSnippet name="PromptNumberExample" />

### Toggle Prompt

Binary choice between two labeled options, navigated with arrow keys:

<CodeSnippet name="PromptToggleExample" />

### Editor Prompt

Opens an inline editor (Nano) for multi-line text editing:

<CodeSnippet name="PromptEditorExample" />

### Search Prompt

Dynamic search with type-ahead filtering:

<CodeSnippet name="PromptSearchExample" />

### KeyPress Prompt

Captures a single key press without requiring Enter:

<CodeSnippet name="PromptKeyPressExample" />

## Multi-Column Layouts

The Prompt module automatically calculates optimal column layouts based on terminal width and item content:

<CodeSnippet name="PromptMultiColumnExample" />

### Navigation

- **Up/Down arrows**: Navigate between rows
- **Left/Right arrows**: Navigate between columns, switch toggle state
- **Enter**: Select/confirm
- **Space**: Toggle checkbox items
- **Character keys**: Quick selection in choice prompts
- **Escape**: Cancel / go back to previous prompt
- **Any key**: Advances key press prompts

## Advanced Features

### Disabled Items

Items can be disabled with custom disabled text:

<CodeSnippet name="PromptDisabledItemsExample" />

### Pre-checked Checkboxes

Checkbox items can be initially checked:

<CodeSnippet name="PromptPreCheckedExample" />

### Mixed Prompt Types

Combine multiple prompt types in a single session:

<CodeSnippet name="PromptMixedTypesExample" />

## Script Integration

The Prompt module includes PromptCommands for command-line usage:

### Command Syntax

```bash
prompt [OPTIONS] TYPE [ITEMS...]

Options:
  -m --message=MESSAGE     prompt message
  -t --title=TITLE         prompt title/header
  -d --default=VALUE       default value
  -k --key=KEYS           choice keys (for choice type)
```

### Examples

```bash
# List selection
prompt list "Choose environment" "Development" "Staging" "Production"

# Multiple selection
prompt checkbox "Select features" "Feature A" "Feature B" "Feature C"

# Quick choice with keys
prompt choice "Pick color" "Red" "Green" "Blue" -k "rgb"

# Text input with default
prompt input "Enter name" -d "John Doe"

# Confirmation
prompt confirm "Deploy to production?" -d "y"
```

## Configuration

The prompt system can be configured via PrompterConfig:

<CodeSnippet name="PromptConfigExample" />

## Migration from Console-UI

The Prompt module provides full backward compatibility with console-ui:

### Before (Console-UI)
```java
ConsolePrompt prompt = new ConsolePrompt();
PromptBuilder promptBuilder = prompt.getPromptBuilder();
```

### After (Prompt Module)
```java
Prompter prompter = PrompterFactory.create(terminal);
PromptBuilder promptBuilder = prompter.newBuilder();
```

The API is identical, but the Prompt module offers enhanced features and better performance.

## Best Practices

### Builder Pattern Usage

Always use the fluent builder API for consistency:

<CodeSnippet name="PromptBestPracticesExample" />

### Error Handling

Handle user interruptions gracefully:

<CodeSnippet name="PromptErrorHandlingExample" />

### Terminal Adaptation

The prompt system automatically adapts to different terminal sizes and capabilities. For optimal experience:

- Use descriptive but concise item text
- Consider terminal width when designing prompts
- Test with different terminal sizes
- Provide meaningful default values

## Performance

The Prompt module is optimized for performance:

- **Efficient rendering**: Only updates changed screen areas
- **Lazy evaluation**: Calculates layouts only when needed
- **Memory efficient**: Minimal object allocation during navigation
- **Terminal capability caching**: Avoids repeated capability detection

## Troubleshooting

### Common Issues

**Multi-column layout not working**: Ensure terminal width is sufficient and item text is not too long.

**Navigation keys not responding**: Check terminal capability detection and ensure proper key binding setup.

**Display corruption**: Verify terminal supports required capabilities and consider using fallback mode.

### Debug Mode

Enable debug logging to troubleshoot issues:

```java
System.setProperty("jline.prompt.debug", "true");
```

## API Reference

### Core Interfaces

- `Prompter`: Main interface for creating prompts
- `PromptBuilder`: Builder for creating multiple prompts
- `BaseBuilder`: Common base for all prompt builders (name, message, transformer, filter)
- `PromptResult`: Base interface for prompt results

### Prompt Builders

- `ListBuilder`: Single-selection list prompt
- `CheckboxBuilder`: Multiple-selection checkbox prompt
- `ChoiceBuilder`: Key-based choice prompt
- `InputBuilder`: Text input prompt
- `ConfirmBuilder`: Yes/No confirmation prompt
- `PasswordBuilder`: Masked password input prompt
- `NumberBuilder`: Numeric input with validation
- `ToggleBuilder`: Binary toggle between two options
- `EditorBuilder`: Multi-line editor prompt
- `SearchBuilder`: Type-ahead search prompt
- `KeyPressBuilder`: Single key capture prompt
- `TextBuilder`: Static text display

### Factory Classes

- `PrompterFactory`: Creates Prompter instances
- `DefaultPrompter`: Main implementation

### Configuration

- `PrompterConfig`: Configuration interface
- `DefaultPrompterConfig`: Default configuration implementation

For complete API documentation, see the [JavaDoc reference](/api/prompt).
