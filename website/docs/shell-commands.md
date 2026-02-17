---
sidebar_position: 7
title: "Shell: Commands"
---

# Defining Commands

## The Command Interface

The `Command` interface is the core abstraction. Each command provides:

- **`name()`** — the primary command name
- **`aliases()`** — alternative names
- **`description()`** — one-line description for help listings
- **`describe(args)`** — detailed description for widgets
- **`execute(session, args)`** — the execution logic
- **`completers()`** — tab completion support

## Using AbstractCommand

`AbstractCommand` is a convenient base class:

```java
public class MyCommand extends AbstractCommand {
    public MyCommand() {
        super("mycommand", "mc", "my");  // name + aliases
    }

    @Override
    public String description() {
        return "Does something useful";
    }

    @Override
    public Object execute(CommandSession session, String[] args) {
        // Command logic here
        return null;
    }
}
```

## Command Groups

`SimpleCommandGroup` organizes commands:

```java
CommandGroup tools = new SimpleCommandGroup("tools",
    new UpperCommand(),
    new LowerCommand(),
    new CountCommand()
);
```

## CommandSession

The `CommandSession` provides the execution context:

- `session.terminal()` — the terminal
- `session.in()` — input stream
- `session.out()` — output print stream
- `session.err()` — error print stream
- `session.get(name)` / `session.put(name, value)` — session variables
- `session.workingDirectory()` — current working directory
- `session.lastExitCode()` — last command's exit code
