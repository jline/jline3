# Migration from jline-console-ui to jline-prompt

This document describes the migration from the deprecated `jline-console-ui` module to the new `jline-prompt` module.

## Overview

The `jline-console-ui` module has been deprecated in favor of the new `jline-prompt` module, which provides a cleaner, interface-based API with better separation of concerns.

## Key Improvements

The new `jline-prompt` module offers:

- **Clean interface-based design**: All components are defined as interfaces with implementations in the `impl` package
- **Better separation of concerns**: Clear distinction between API and implementation
- **Record-style accessor methods**: Methods like `indicator()` instead of `getIndicator()`
- **Improved type safety**: Better type definitions and generics usage
- **Better documentation**: Comprehensive JavaDoc for all interfaces and methods
- **Platform-specific defaults**: Automatic selection of appropriate UI characters based on the platform

## Migration Steps

### 1. Update Dependencies

Replace the dependency in your `pom.xml`:

```xml
<!-- Old dependency -->
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-console-ui</artifactId>
    <version>3.30.3</version>
</dependency>

<!-- New dependency -->
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-prompt</artifactId>
    <version>3.30.3</version>
</dependency>
```

### 2. Update Imports

Replace imports from the old package with the new one:

```java
// Old imports
import org.jline.consoleui.prompt.ConsolePrompt;
import org.jline.consoleui.prompt.builder.PromptBuilder;

// New imports
import org.jline.prompt.Prompter;
import org.jline.prompt.PrompterFactory;
import org.jline.prompt.PromptBuilder;
```

### 3. Update Code

#### Old API Usage:
```java
ConsolePrompt prompt = new ConsolePrompt(terminal);
PromptBuilder promptBuilder = prompt.getPromptBuilder();

promptBuilder.createListPrompt()
    .name("choice")
    .message("Choose an option:")
    .newItem("option1").text("Option 1").add()
    .newItem("option2").text("Option 2").add()
    .addPrompt();

Map<String, PromptResultItemIF> result = prompt.prompt(promptBuilder.build());
```

#### New API Usage:
```java
Prompter prompter = PrompterFactory.create(terminal);
PromptBuilder promptBuilder = prompter.getPromptBuilder();

promptBuilder.createListPrompt()
    .name("choice")
    .message("Choose an option:")
    .newItem("option1").text("Option 1").add()
    .newItem("option2").text("Option 2").add()
    .addPrompt();

Map<String, PromptResult> result = prompter.prompt(header, promptBuilder.build());
```

## API Mapping

| Old API | New API |
|---------|---------|
| `ConsolePrompt` | `ConsoleUI` |
| `ConsolePrompt.UiConfig` | `ConsoleUI.Config` |
| `PromptResultItemIF` | `PromptResult` |
| `ListResult` | `ListPromptResult` |
| `CheckboxResult` | `CheckboxPromptResult` |
| `InputResult` | `InputPromptResult` |
| `ExpandableChoiceResult` | `ChoicePromptResult` |
| `ConfirmResult` | `ConfirmPromptResult` |

## Configuration Changes

### Old Configuration:
```java
ConsolePrompt.UiConfig config;
if (OSUtils.IS_WINDOWS) {
    config = new ConsolePrompt.UiConfig(">", "( )", "(x)", "( )");
} else {
    config = new ConsolePrompt.UiConfig("❯", "◯ ", "◉ ", "◯ ");
}
ConsolePrompt prompt = new ConsolePrompt(terminal, config);
```

### New Configuration:
```java
// Platform-specific defaults are now automatic
Prompter prompter = PrompterFactory.create(terminal);

// Or with custom configuration
Prompter.Config config = new DefaultPrompter.DefaultConfig("->", "[ ]", "[x]", "[-]");
Prompter prompter = PrompterFactory.create(terminal, config);
```

## Backward Compatibility

The old `jline-console-ui` module is still available but marked as deprecated. It will be removed in a future version. The new `jline-prompt` module delegates to the existing implementation, ensuring full backward compatibility during the transition period.

## Benefits of Migration

1. **Future-proof**: The new API is designed for long-term stability
2. **Better IDE support**: Interface-based design provides better auto-completion and type checking
3. **Easier testing**: Clean interfaces make it easier to create mocks and test implementations
4. **Extensibility**: The new design makes it easier to create custom implementations
5. **Platform defaults**: No need to manually handle platform-specific UI characters

## Support

For questions about migration or issues with the new API, please refer to the JLine documentation or create an issue in the JLine repository.
