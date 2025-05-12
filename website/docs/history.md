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

## On Startup

The line reader will load history on the first use, but you can pre-load history if you like:

```java
History history = myLineReader.getHistory();
// Make sure the instance has access to the reader's variable set,
// and load history.
history.attach(myLineReader);
```

## On Exit

By default, JLine automatically saves history when the line reader reads a new line, thanks to the `HISTORY_INCREMENTAL` option which is enabled by default. However, if you've disabled this option or want to ensure history is saved at the end of your application, you should explicitly save the history:

```java
try {
    myLineReader.getHistory().save();
} catch (IOException e) {
    // Handle exception
}
```

This is particularly important if the reader loop exits with an exception. You might put your save call in a `finally` block, in a shutdown hook, or signal handler.

To control automatic history saving, you can set the `HISTORY_INCREMENTAL` option:

```java
// Enable automatic history saving (this is the default)
lineReader.setOption(LineReader.Option.HISTORY_INCREMENTAL, true);

// Disable automatic history saving
lineReader.setOption(LineReader.Option.HISTORY_INCREMENTAL, false);
```

## History Expansion

JLine supports history expansion similar to Bash:

<CodeSnippet name="HistoryExpansionExample" />

If you want to disable history expansion (commands like `!`, `!!`, `!n`, `!-n`, `!string`, and `^string1^string2`) and escape character interpretation, you can use the `DISABLE_EVENT_EXPANSION` option:

```java
// Disable history expansion and escape character interpretation
lineReader.setOption(LineReader.Option.DISABLE_EVENT_EXPANSION, true);

// Enable history expansion and escape character interpretation (this is the default)
lineReader.setOption(LineReader.Option.DISABLE_EVENT_EXPANSION, false);
```

Disabling event expansion is useful in applications where:
- The `!` character is commonly used for other purposes
- You need to support Windows-style paths with backslashes (e.g., `C:\Users\name`) without having the backslashes interpreted as escape characters

## History Search

You can search through history programmatically:

<CodeSnippet name="HistorySearchExample" />

## Race Conditions

Beware that if a single user has multiple instances of your program running concurrently, saving history from one could clobber content from another.

This is not usually a problem in practice since the default `History` implementation takes care to only append history entries from the current session:

```java
try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
        StandardOpenOption.WRITE, StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
    for (Entry entry : items.subList(from, items.size())) {
        if (isPersistable(entry)) {
            writer.append(format(entry));
        }
    }
}
```

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

11. **Save on Exit**: Always save history when your application exits, preferably in a finally block or shutdown hook.

12. **Race Conditions**: Be aware of potential race conditions when multiple instances of your application are running concurrently.
