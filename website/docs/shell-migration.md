---
sidebar_position: 10
title: "Shell: Migration Guide"
---

# Migrating from Console API

The `jline-shell` module replaces the old `jline-console` command infrastructure. The old API is deprecated but still functional.

## What Changed

| Old (jline-console) | New (jline-shell) |
|---------------------|-------------------|
| `CommandRegistry` | `CommandGroup` |
| `SystemRegistry` | `CommandDispatcher` |
| `CommandMethods` + `CommandInput` | `Command.execute()` |
| `CmdDesc` | `CommandDescription` |
| `CmdLine` | `CommandLine` |
| `ArgDesc` | `ArgumentDescription` |
| `CommandRegistry.CommandSession` | `CommandSession` |

## Bridge Adapters

If you have existing `CommandRegistry` implementations, use `CommandGroupAdapter` to wrap them for the new API:

```java
// Wrap old registry as new CommandGroup
CommandGroup group = new CommandGroupAdapter(myOldRegistry);

Shell shell = Shell.builder()
    .groups(group)
    .build();
```

Going the other direction, use `CommandRegistryAdapter`:

```java
// Wrap new CommandGroup as old CommandRegistry
CommandRegistry registry = new CommandRegistryAdapter(myNewGroup);
```

## Using ConsoleDispatcherBuilder

For applications that need the full classic experience (scripting, SystemRegistry, Builtins), use `ConsoleDispatcherBuilder`:

```java
CommandDispatcher dispatcher = ConsoleDispatcherBuilder.builder()
    .terminal(terminal)
    .parser(parser)
    .builtins(builtins)
    .commands(myOldRegistry)
    .build();

Shell shell = Shell.builder()
    .dispatcher(dispatcher)
    .build();
shell.run();
```

## Description Conversion

Use `DescriptionAdapter` for converting between old and new description types:

```java
// Old to new
CommandDescription desc = DescriptionAdapter.toCommandDescription(cmdDesc);

// New to old
CmdDesc cmdDesc = DescriptionAdapter.toCmdDesc(commandDescription);
```
