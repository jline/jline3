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

The shell module includes pipeline parsing and execution with operators like `|` (pipe), `&&` (and), `||` (or), `>` (redirect), and `>>` (append).

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
