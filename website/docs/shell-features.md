---
sidebar_position: 8
title: "Shell: Features"
---

# Shell Features

The `jline-shell` module provides a rich set of features for building interactive command-line applications. This page covers the key extensibility points and built-in capabilities.

## Aliases

The alias system allows users to define short names for frequently used commands.

### Setting Up

Wire an `AliasManager` into the shell builder:

```java
Shell shell = Shell.builder()
    .aliasManager(new DefaultAliasManager())
    .build();
```

For persistence across sessions, provide a file path:

```java
Path aliasFile = Paths.get(System.getProperty("user.home"), ".myapp-aliases");
DefaultAliasManager aliasManager = new DefaultAliasManager(aliasFile);
aliasManager.load();
```

### Built-in Commands

When an `AliasManager` is configured, the shell automatically registers:

- `alias` — list all aliases, define an alias (`alias name=expansion`), or show one (`alias name`)
- `unalias` — remove an alias (`unalias name`)

### Parameter Substitution

Alias expansions support parameter markers:

- `$1`, `$2`, ... — positional arguments
- `$@` — all arguments

```
alias grp=grep $1 $2
grp pattern file.txt    → grep pattern file.txt

alias e=echo $@
e hello world           → echo hello world
```

### Demo

<CodeSnippet name="ShellAliasExample" />

---

## Built-in Commands

The shell provides several opt-in command groups that can be enabled via the builder.

### History

```java
Shell.builder().historyCommands(true)
```

Adds the `history` command:

| Usage | Description |
|-------|-------------|
| `history` | List all entries |
| `history N` | List last N entries |
| `history -c` | Clear history |
| `history -s` | Save history to file |
| `history /pattern` | Search by regex |

### Help

```java
Shell.builder().helpCommands(true)
```

Adds the `help` command (also aliased as `?`):

| Usage | Description |
|-------|-------------|
| `help` | List all commands grouped by category |
| `help <command>` | Show detailed help for a specific command |

### Options

```java
Shell.builder().optionCommands(true)
```

Adds commands for runtime LineReader configuration:

| Command | Description |
|---------|-------------|
| `setopt OPTION` | Enable a LineReader option |
| `setopt` | List enabled options |
| `unsetopt OPTION` | Disable a LineReader option |
| `setvar NAME VALUE` | Set a LineReader variable |
| `setvar` | List all variables |

---

## Syntax Highlighting

The `CommandHighlighter` provides command-aware syntax highlighting:

- **Known commands** are highlighted in bold
- **Unknown commands** are highlighted in red
- **Pipeline operators** (`|`, `&&`, `||`, `;`, etc.) are highlighted in cyan

### Enabling

```java
Shell.builder().commandHighlighter(true)
```

### Custom Highlighters

You can provide a custom `Highlighter` implementation:

```java
Shell.builder().highlighter(myHighlighter)
```

Or combine both — the command highlighter can delegate to a custom highlighter:

```java
Shell.builder()
    .highlighter(myLanguageHighlighter)
    .commandHighlighter(true)
```

---

## Job Control

The shell supports background execution and job management.

### Enabling

```java
Shell.builder().jobManager(new DefaultJobManager())
```

### Usage

| Syntax | Description |
|--------|-------------|
| `command &` | Run in background |
| `jobs` | List active jobs |
| `fg [id]` | Bring job to foreground |
| `bg [id]` | Resume job in background |

Jobs are tracked with status: `Foreground`, `Background`, `Suspended`, `Done`.

### Demo

<CodeSnippet name="ShellJobExample" />

---

## Pipeline Extensibility

The pipeline parser supports standard shell operators and can be extended with custom operators.

### Built-in Operators

| Operator | Name | Description |
|----------|------|-------------|
| `\|` | Pipe | Send output to next command |
| `\|;` | Flip | Append output as argument |
| `&&` | AND | Execute next only if previous succeeded |
| `\|\|` | OR | Execute next only if previous failed |
| `;` | Sequence | Execute next unconditionally |
| `>` | Redirect | Write output to file |
| `>>` | Append | Append output to file |
| `&` | Background | Run in background (at end of line) |

### Custom Operators

Register custom operator symbols that map to existing operator types:

```java
PipelineParser parser = new PipelineParser(Map.of(
    "==>", Pipeline.Operator.PIPE,
    "-->", Pipeline.Operator.FLIP
));
Shell.builder().pipelineParser(parser)
```

### Subclassing

For more control, subclass `PipelineParser` and override `matchOperator()`:

```java
PipelineParser parser = new PipelineParser() {
    @Override
    protected String matchOperator(String line, int pos) {
        // Custom matching logic
        if (line.startsWith("::", pos)) {
            return "::";
        }
        return super.matchOperator(line, pos);
    }
};
```

### Demo

<CodeSnippet name="ShellPipelineExample" />
