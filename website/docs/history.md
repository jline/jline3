---
sidebar_position: 4
---

# History

import CodeSnippet from '@site/src/components/CodeSnippet';

JLine provides a powerful history mechanism that allows users to recall and reuse previously entered commands. This page explains how to configure and use the history feature in your applications.

## Basic Setup

To enable history in your LineReader, you need to configure it during creation:

<CodeSnippet name="HistorySetupExample" />

## Persistent History

You can configure JLine to save history to a file, allowing it to persist between sessions:

<CodeSnippet name="PersistentHistoryExample" />

## History Size

You can limit the number of entries stored in memory and in the history file:

<CodeSnippet name="HistorySizeExample" />

## History Filtering

JLine provides several options to control which commands are added to history:

<CodeSnippet name="HistoryFilteringExample" />

## Programmatic Access

You can access the history programmatically:

<CodeSnippet name="ProgrammaticHistoryAccessExample" />

## History Expansion

JLine supports history expansion similar to Bash:

<CodeSnippet name="HistoryExpansionExample" />

## History Search

You can search through history programmatically:

<CodeSnippet name="HistorySearchExample" />

## Best Practices

When using history in JLine, consider these best practices:

1. **Persistent History**: Save history to a file to provide a better user experience across sessions.

2. **History Size Limits**: Set reasonable limits on history size to prevent memory issues.

3. **Filter Sensitive Information**: Configure history to ignore commands containing sensitive information.

4. **Custom History**: Implement a custom history if you need special behavior, such as storing additional metadata.

5. **History Expansion**: Enable history expansion for a more bash-like experience.

6. **History Search**: Provide a way for users to search through history.

7. **History Listeners**: Use listeners to react to history changes.

8. **Documentation**: Document history features and keyboard shortcuts for your users.

9. **Testing**: Test history functionality thoroughly, especially persistence and expansion.

10. **Cleanup**: Provide a way for users to clear their history if needed.
