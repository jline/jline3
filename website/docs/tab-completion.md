---
sidebar_position: 5
---

# Tab Completion

import CodeSnippet from '@site/src/components/CodeSnippet';

JLine provides a powerful tab completion system that allows you to add intelligent suggestions to your command-line applications. This page explains how to configure and use the tab completion feature.

## Basic Completers

JLine provides several built-in completers for common use cases:

### StringsCompleter

The StringsCompleter completes from a fixed or dynamic set of strings:

<CodeSnippet name="StringsCompleterExample" />

### FileNameCompleter

The FileNameCompleter completes file and directory names:

<CodeSnippet name="FileNameCompleterExample" />

### TreeCompleter

The TreeCompleter allows you to define a hierarchical structure of completions:

<CodeSnippet name="TreeCompleterExample" />

### AggregateCompleter

The AggregateCompleter combines multiple completers:

<CodeSnippet name="AggregateCompleterExample" />

## Custom Completers

You can create your own completers by implementing the Completer interface:

<CodeSnippet name="CustomCompleter" />

## Completion Behavior

You can configure how completion behaves:

<CodeSnippet name="CompletionBehaviorExample" />

## Candidates with Descriptions

You can provide descriptions for completion candidates:

<CodeSnippet name="CandidatesWithDescriptionsExample" />

## Context-Aware Completion

You can create completers that are aware of the current context:

<CodeSnippet name="ContextAwareCompleter" />

## Colored Completions

You can add colors to your completions:

<CodeSnippet name="ColoredCompleter" />

## Additional Completers

JLine provides several additional completers for specific use cases:

### NullCompleter

The NullCompleter is used to terminate completion:

<CodeSnippet name="NullCompleterExample" />

### DirectoriesCompleter

The DirectoriesCompleter completes only directory names:

<CodeSnippet name="DirectoriesCompleterExample" />

### FilesCompleter

The FilesCompleter completes file names with filtering:

<CodeSnippet name="FilesCompleterExample" />

### RegexCompleter

The RegexCompleter uses regular expressions to define completion patterns:

<CodeSnippet name="RegexCompleterExample" />

### EnumCompleter

The EnumCompleter completes enum values:

<CodeSnippet name="EnumCompleterExample" />

## Builtins Completers

The JLine Builtins module provides additional completers:

### SystemCompleter

The SystemCompleter manages completers for different commands:

<CodeSnippet name="SystemCompleterExample" />

### Builtins TreeCompleter

The Builtins module provides its own TreeCompleter:

<CodeSnippet name="BuiltinsTreeCompleterExample" />

## Best Practices

When implementing tab completion in JLine, consider these best practices:

1. **Combine Completers**: Use AggregateCompleter and ArgumentCompleter to create complex completion behavior.

2. **Context-Aware Completion**: Make your completers aware of the current context for more intelligent suggestions.

3. **Descriptions**: Provide descriptions for completion candidates to help users understand their options.

4. **Colored Completions**: Use colors to distinguish between different types of completions.

5. **Dynamic Completions**: Update completion candidates based on the current state of your application.

6. **Filtering**: Filter completion candidates based on what the user has typed so far.

7. **Performance**: Be mindful of performance, especially for large sets of completion candidates.

8. **Testing**: Test your completers thoroughly with different inputs.

9. **Documentation**: Document the available completions for your users.

10. **Fallbacks**: Provide sensible fallbacks when specific completions are not available.
