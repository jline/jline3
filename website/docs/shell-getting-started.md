---
sidebar_position: 6
title: "Shell: Getting Started"
---

# Getting Started with jline-shell

The `jline-shell` module makes it easy to build interactive command-line applications. Here's a minimal example:

<CodeSnippet name="ShellSimpleExample" />

## Adding Commands

Define commands by extending `AbstractCommand`:

<CodeSnippet name="ShellCommandExample" />

## Using Shell.builder()

The `Shell.builder()` API provides a fluent way to configure the shell:

<CodeSnippet name="ShellBuilderExample" />

## Key Builder Options

| Method | Description |
|--------|-------------|
| `.terminal(terminal)` | Set the terminal (auto-created if omitted) |
| `.prompt("myapp> ")` | Set a static prompt |
| `.prompt(() -> dynamicPrompt)` | Set a dynamic prompt |
| `.dispatcher(dispatcher)` | Set a custom command dispatcher |
| `.groups(group1, group2)` | Add command groups |
| `.parser(parser)` | Set a custom line parser |
| `.historyFile(path)` | Set the history file location |
| `.variable(name, value)` | Set a LineReader variable |
| `.option(Option.X, true)` | Set a LineReader option |
| `.onReaderReady(reader -> { })` | Callback after LineReader creation |

## Next Steps

- [Defining Commands](./shell-commands.md) — detailed guide to the Command API
- [Picocli Integration](./shell-picocli.md) — using picocli with jline-shell
- [Migration Guide](./shell-migration.md) — migrating from the old console API
