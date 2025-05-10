---
sidebar_position: 5
---

# Console UI Module

import CodeSnippet from '@site/src/components/CodeSnippet';

The JLine Console UI module provides interactive prompt components for console applications, inspired by [Inquirer.js](https://github.com/SBoudrias/Inquirer.js). It offers a set of UI elements for creating user-friendly command-line interfaces.

<img src="/img/ConsoleUI-Logo.png" width="200" align="right" alt="ConsoleUI logo" />

## Introduction

ConsoleUI is a library for prompting the user for different types of input. It provides simple UI elements on ANSI console-based terminals. ConsoleUI was initially implemented using JLine2 by Andreas Wegmann and was later upgraded to use JLine3 and merged into the JLine project.

## Features

Console UI currently supports:

- Text input with completion and GNU ReadLine compatible editing
- Checkboxes for multiple selections
- Lists for single item selection
- Expandable choices (multiple key-based answers for a question with help)
- Yes/No confirmation prompts

[Watch a demo of ConsoleUI on YouTube](https://youtu.be/6dB3CyOX9rU)

## Quick Start Example

Here's a simple example to get you started with ConsoleUI:

<CodeSnippet name="ConsoleUIBasicExample" />

## Basic Usage

The entry point to the builder classes is to create a new object of type `ConsolePrompt`. From the prompt object, use the `getPromptBuilder()` method to create the builder for all subsequent UI elements.

From this `PromptBuilder` you can access UI builders with the following methods:

- `createCheckboxPrompt()`: Creates a checkbox prompt that lets the user choose any number of items from a list
- `createChoicePrompt()`: Creates a choice prompt that lets the user choose one from a given number of possible answers
- `createConfirmPromp()`: Creates a confirmation prompt that lets the user answer with 'yes' or 'no'
- `createInputPrompt()`: Creates an input prompt for text entry with optional masking and completion
- `createListPrompt()`: Creates a list prompt that lets the user choose one item from a list

## Prompt Types

### Input Prompt

The InputPrompt is a classic entry line like a shell. It offers completers and optional password masking.

<CodeSnippet name="ConsoleUIInputExample" />

<img src="/img/console-ui/input_prompt.png" alt="Input prompt" />

The user can use readline compatible navigation (Emacs control codes) inside the input area, including CTRL-a to go to the beginning of input, CTRL-e to go to the end, and so on.

### List Prompt

The list prompt lets the user choose one item from a list.

<CodeSnippet name="ConsoleUIListExample" />

<img src="/img/console-ui/list_prompt.png" alt="List prompt" />

The user can navigate with arrow keys or VI-like keys ('j' for down, 'k' for up) and select an item with Enter.

### Checkbox Prompt

The checkbox prompt lets the user choose any number of items from a list.

<CodeSnippet name="ConsoleUICheckboxExample" />

<img src="/img/console-ui/checkbox_prompt.png" alt="Checkbox prompt" />

The user can navigate with arrow keys and toggle selection with the space bar. Pressing Enter completes the input.

### Expandable Choice Prompt

The choice prompt lets the user choose one from a given number of possible answers, each assigned to a single keystroke.

<CodeSnippet name="ConsoleUIChoiceExample" />

<img src="/img/console-ui/expandable_choice_prompt_1.png" alt="Expandable choice prompt" />

By default, only the message and possible keys are displayed. If the user presses 'h' (for help), all possible answers with explanations are shown as a list.

<img src="/img/console-ui/expandable_choice_prompt_3.png" alt="Expandable choice prompt with help" />

### Confirmation Prompt

The confirmation prompt lets the user answer with 'yes' or 'no' to a question. This was shown in the Quick Start Example above.

<img src="/img/console-ui/confirmation_prompt.png" alt="Confirmation prompt" />

The user can press 'y' or 'n' to select between 'yes' and 'no', and Enter to confirm.

## Advanced Features

### Page Size Control

For long lists, you can control how many items are displayed at once:

```java
// Set absolute number of items (default is 10)
.pageSize(15)

// Or set a percentage of terminal height (66 = 2/3 of terminal)
.relativePageSize(66)
```

### Disabled Items

You can disable items and display a message explaining why, as shown in the Checkbox example above with the pineapple topping:

```java
.newItem("premium").text("Premium Feature").disabledText("Requires subscription").add()
```

### Pre-checked Items

For checkbox prompts, you can pre-check items as shown in the Checkbox example above:

```java
// Method 1
.newItem("olives").text("Olives").check().add()

// Method 2
.newItem("special").text("Special").checked(true).add()
```

## Maven Dependency

To use the Console UI module in your Maven project, add the following dependency:

```xml
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-console-ui</artifactId>
    <version>${jline.version}</version>
</dependency>
```
