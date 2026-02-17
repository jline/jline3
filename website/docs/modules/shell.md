---
sidebar_position: 3
---

# jline-shell

The `jline-shell` module provides a clean API for building interactive command-line applications with JLine. It defines the core abstractions for commands, command groups, command dispatching, pipelines, and job management.

## Architecture

The shell module sits between `jline-reader` and `jline-console` in the module dependency graph:

```
jline-terminal
  ↑
jline-reader
  ↑
jline-shell  ← this module
  ↑
jline-console (legacy bridge)
```

## Core Concepts

### Command

A `Command` encapsulates everything about a command: its name, aliases, description, execution logic, and completion support.

```java
Command echo = new AbstractCommand("echo", "e") {
    @Override
    public String description() {
        return "Echo arguments to output";
    }

    @Override
    public Object execute(CommandSession session, String[] args) {
        session.out().println(String.join(" ", args));
        return null;
    }
};
```

### CommandGroup

A `CommandGroup` organizes commands for discovery. Unlike the old `CommandRegistry`, it has no execution or completion concerns — those belong to the `CommandDispatcher`.

```java
CommandGroup group = new SimpleCommandGroup("myapp", echoCmd, greetCmd);
```

### CommandDispatcher

The `CommandDispatcher` aggregates command groups and handles command resolution, pipeline execution, and completion.

```java
CommandDispatcher dispatcher = new DefaultCommandDispatcher(terminal);
dispatcher.addGroup(group);
Object result = dispatcher.execute("echo hello | grep h");
```

### Shell

The `Shell` is a thin REPL loop. Use the builder API to configure it:

```java
Shell shell = Shell.builder()
    .terminal(terminal)
    .prompt("myapp> ")
    .groups(group1, group2)
    .historyFile(Paths.get("~/.myapp_history"))
    .build();
shell.run();
```

## Pipeline Support

The shell module includes pipeline parsing and execution with operators like `|` (pipe), `|;` (flip), `&&` (and), `||` (or), `;` (sequence), `>` (redirect), `>>` (append), and `&` (background).

The `PipelineParser` is extensible — register custom operators via its constructor, or subclass and override `matchOperator()` for full control.

## Alias System

The `AliasManager` interface and `DefaultAliasManager` implementation provide a command alias system with `$1`/`$2`/`$@` parameter substitution and optional file persistence. When configured, `alias` and `unalias` commands are automatically registered.

## Built-in Commands

The shell provides opt-in command groups via the builder:
- `historyCommands(true)` — `history` command with list/search/clear/save
- `helpCommands(true)` — `help` command listing all commands by group
- `optionCommands(true)` — `setopt`/`unsetopt`/`setvar` for runtime LineReader configuration

## Syntax Highlighting

The `CommandHighlighter` highlights known commands in bold, unknown commands in red, and pipeline operators in cyan. Enable with `commandHighlighter(true)` on the builder, or provide a custom `Highlighter`.

## Job Management

The `JobManager` and `Job` interfaces provide job control for managing foreground, background, and suspended commands.

## Maven Dependency

```xml
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-shell</artifactId>
    <version>${jline.version}</version>
</dependency>
```

## Migration from Console API

If you're using the old `CommandRegistry`/`SystemRegistry` API from `jline-console`, see the [Migration Guide](../shell-migration.md) for how to use bridge adapters.
